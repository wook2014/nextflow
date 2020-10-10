package io.nextflow.gradle.tasks

import java.util.regex.Pattern

import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import io.nextflow.gradle.tasks.AbstractS3Task
import io.nextflow.gradle.util.BucketTokenizer
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Create a plugin repository index traversing S3 directory structure
 * for PF4J update manager: https://github.com/pf4j/pf4j-update#repository-structure
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class PluginsRepositoryIndexer extends AbstractS3Task {

    static private Pattern PATTERN_RIGHT_TRIM = ~/\s+$/

    @Input
    String publishUrl

    @Canonical
    static class Release {
        String pluginId
        String metaFile
    }

    private String bucket

    @TaskAction
    def task() {
        final urlTokens = BucketTokenizer.from(publishUrl)
        if( urlTokens.scheme != 's3' )
            throw new GradleException("Invalid publishUrl: $publishUrl -- It must start with s3:// prefix")

        this.bucket = urlTokens.bucket
        final prefix = urlTokens.key

        final req = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(urlTokens.key + '/');

        Map allPlugins = new HashMap<String,List<Release>>()
        ObjectListing objects = s3Client.listObjects(req)
        while(true) {
            List<S3ObjectSummary> summaries = objects.getObjectSummaries();
            if (summaries.size() < 1)
                break

            for(int i=0;i<summaries.size();i++){
                final key = summaries.get(i).getKey()
                if( key.endsWith('.json') ) {
                    def items = key.tokenize('/')
                    if( items.size()==3 ) {
                        final pluginId = items[1]
                        final releases = allPlugins.getOrDefault(pluginId, [])
                        releases.add( new Release(pluginId, key) )
                        allPlugins.put(pluginId, releases)
                    }
                }
            }

            objects = s3Client.listNextBatchOfObjects(objects)
        }


        final index = renderRepoIndex(allPlugins)
        final meta = new ObjectMetadata(); meta.setContentType('text/json')
        final uploadReq = new PutObjectRequest(bucket,"$prefix/plugins.json", new ByteArrayInputStream(index.bytes), meta)
                .withCannedAcl(CannedAccessControlList.PublicRead)
        s3Client.putObject(uploadReq)

    }

    String renderRepoIndex(Map<String,List<Release>> entries) {
        def result = new StringBuilder()
        result.append("[\n")
        result.append( entries.collect { String k, List<Release> v -> renderPlugin(k,v) }.join(',\n') + '\n' )
        result.append("]")

    }

    String renderPlugin(String id, List<Release> releases) {
        def result = new StringBuilder()
        result << "{\n"
        result << "  \"id\": \"${id}\",\n"
        result << "  \"name\": null,\n"
        result << "  \"description\": null,\n"
        result << "  \"provider\": null,\n"
        result << "  \"releases\": [\n"
        result << "${indent(renderReleases(releases), '      ')} ]\n"
        result << "}"

        return indent(result.toString(), '  ')
    }

    String renderReleases(List<Release> releases) {
        releases
                .collect{ Release it -> s3Client.getObject(bucket, it.metaFile).getObjectContent().text?.trim() }
                .join(',\n')
    }


    String indent( String text, String prefix ) {
        text
                .tokenize('\n')
                .collect { prefix + it }
                .join('\n')
    }

    String rightTrim(String self) {
        self.replaceAll(PATTERN_RIGHT_TRIM,'')
    }
}
