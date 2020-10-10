package io.nextflow.gradle.tasks

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.SystemPropertiesCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import io.nextflow.gradle.model.PluginMeta
import io.nextflow.gradle.model.PluginRelease
import io.nextflow.gradle.util.BucketTokenizer
import io.nextflow.gradle.util.GithubClient
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
/**
 * This task traverse a S3 plugins repo and creates
 * and updates plugins repository index. Finally push
 * the updated index to the Github repository.
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class PluginsRepositoryPublisher extends DefaultTask {

    /**
     * The source repository URL. It must a S3 bucket URL
     */
    @Input String repositoryUrl

    /**
     * The target plugins repository index HTTP URL
     */
    @Input String indexUrl

    /**
     * The auth access token to post to access Github plugins repo
     */
    @Input String githubToken

    /**
     * The Github user name posting the changes
     */
    @Input String githubUser

    /**
     * The Github email signing the index update
     */
    @Input String githubEmail

    /**
     * The AWS creds profile
     */
    @Input @Optional String profile

    /**
     * The AWS plugins repo region
     */
    @Input @Optional String region

    private String bucket
    private String prefix


    @Memoized
    private AmazonS3Client getS3Client() {
        def profileCreds
        if (profile) {
            logger.info("Using AWS credentials profile: ${profile}")
            profileCreds = new ProfileCredentialsProvider(profile)
        }
        else {
            profileCreds = new ProfileCredentialsProvider()
        }
        def creds = new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                profileCreds,
                new EC2ContainerCredentialsProviderWrapper()
        )

        AmazonS3Client s3Client = new AmazonS3Client(creds)
        if (region) {
            s3Client.region = Region.getRegion(Regions.fromName(region))
        }
        return s3Client
    }


    String mergeIndex(List<PluginMeta> mainIndex, Map<String,List<PluginRelease>> pluginsToPublish) {

        for( Map.Entry<String,List<PluginRelease>> item : pluginsToPublish ) {
            final pluginId = item.key
            final pluginReleases = item.value
            final PluginMeta indexEntry = mainIndex.find { PluginMeta meta -> meta.id == pluginId }

            if (!indexEntry) {
                mainIndex.add(new PluginMeta(id: pluginId, releases: pluginReleases))
            }
            else {
                for (PluginRelease rel : pluginReleases) {
                    // check if this version already exist in the index
                    final indexRel = indexEntry.releases.find { PluginRelease it -> it.version = rel.version }
                    // if not exists, add to the index
                    if( !indexRel ) {
                        indexEntry.releases << rel
                    }
                    // otherwise verify the checksum matches
                    else if( indexRel.sha512sum != rel.sha512sum ) {
                        def msg = "Plugin $pluginId@${rel.version} invalid checksum:\n"
                        msg += "- index sha512sum: $indexRel.sha512sum\n"
                        msg += "- repo sha512sum : $rel.sha512sum\n"
                        msg += "- repo url       : $rel.url"
                        throw new GradleException(msg)
                    }

                }
            }
        }

        new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(mainIndex)
    }

    List<PluginMeta> parseMainIndex() {
        // get main repo index
        final indexJson = new URL(indexUrl).text
        final type = new TypeToken<ArrayList<PluginMeta>>(){}.getType()
        return new Gson().fromJson(indexJson, type)
    }


    /*
     * Traverse a S3 bucket and return a map given all releases for each
     * plugin id
     *
     * @return The map holding the plugin releases for each plugin id
     */
    Map<String,List<PluginRelease>> listPlugins() {

        final gson = new Gson()
        final result = new HashMap<String,List<PluginRelease>>()

        final req = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(prefix + '/');

        def objects = s3Client.listObjects(req)

        while(true) {
            List<S3ObjectSummary> summaries = objects.getObjectSummaries();
            if (summaries.size() < 1)
                break

            for(int i=0;i<summaries.size();i++){
                final key = summaries.get(i).getKey()
                if( key.endsWith('.json') ) {
                    def items = key.tokenize('/')
                    if( items.size()==3 ) {
                        logger.quiet("Found plugin s3://$bucket/$key")
                        final pluginId = items[1]
                        final releases = result.getOrDefault(pluginId, [])
                        final rel = gson.fromJson(s3Client.getObject(bucket, key).getObjectContent().text, PluginRelease)
                        releases << rel
                        result.put(pluginId, releases)
                    }
                }
            }

            objects = s3Client.listNextBatchOfObjects(objects)
        }

        return result
    }


    @TaskAction
    def apply() {
        final urlTokens = BucketTokenizer.from(repositoryUrl)
        if( urlTokens.scheme != 's3' )
            throw new GradleException("Invalid publishUrl: $repositoryUrl -- It must start with s3:// prefix")
        this.bucket = urlTokens.bucket
        this.prefix = urlTokens.key

        // list plugins in the nextflow s3 releases
        logger.quiet("Fetching plugins from $repositoryUrl")
        final pluginsToPublish = listPlugins()

        // fetch the plugins public index
        logger.quiet("Parsing current index $indexUrl")
        def mainIndex = parseMainIndex()

        // merge indexes
        logger.quiet("Merging index")
        final result = mergeIndex(mainIndex, pluginsToPublish)

        final gitUrl = new URL(indexUrl)
        final tokns = gitUrl.path.tokenize('/')
        final githubOrg = tokns[0]
        final githubRepo = tokns[1]
        final githubBranch = tokns[2]
        final targetFile = tokns[3]

        // push to github
        logger.quiet("Publish merged index to $indexUrl")
        final github = new GithubClient()
        github.userName = githubUser
        github.authToken = githubToken
        github.branch = githubBranch ?: 'master'
        github.repo = githubRepo
        github.owner = githubOrg
        github.email = githubEmail

        github.pushChange(targetFile, result.toString() + '\n', "Nextflow plugins update")
    }

}
