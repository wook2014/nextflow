package io.nextflow.gradle

import java.text.DecimalFormat

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.SystemPropertiesCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressListener
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * S3 uploader/downloader
 *
 * Based on https://github.com/mgk/s3-plugin/blob/master/src/main/groovy/com/github/mgk/gradle/S3Plugin.groovy
 *
 */

@CompileStatic
class NextflowPlugin implements Plugin<Project> {

    void apply(Project target) {
        target.extensions.create('s3', S3Extension)
    }

}


class S3Extension {
    String profile
    String region
}

abstract class S3Task extends DefaultTask {

    @Internal
    AmazonS3Client getS3Client() {
        def profileCreds
        if (project.s3.profile) {
            logger.quiet("Using AWS credentials profile: ${project.s3.profile}")
            profileCreds = new ProfileCredentialsProvider(project.s3.profile)
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
        String region = project.s3.region
        if (region) {
            s3Client.region = Region.getRegion(Regions.fromName(region))
        }
        return s3Client
    }
}

@CompileStatic
class S3Upload extends S3Task {

    /**
     * The S3 target path
     *
     * the provider mess is needed to lazy evaluate the `project.version` property
     *   https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_properties
     *   https://stackoverflow.com/questions/13198358/how-to-get-project-version-in-custom-gradle-plugin/13198744
     *
     */
    @Input
    final Property<String> target = project.objects.property(String)

    /**
     * The source file to upload
     */
    @Input
    final Property<String> source = project.objects.property(String)

    @Input
    boolean overwrite = false

    @Input
    boolean publicRead = false

    @TaskAction
    def task() {
        final sourceFile = new File(source.get())
        final targetUrl = target.get()
        final urlTokens = BucketTokenizer.from(targetUrl)
        if( urlTokens.scheme != 's3' )
            throw new GradleException("S3 upload failed -- Invalid target s3 path: $targetUrl")
        final bucket = urlTokens.bucket
        final targetKey = urlTokens.key

        if( !sourceFile.exists() )
          throw new GradleException("S3 upload failed -- Source file does not exists: $sourceFile")

        final req = new PutObjectRequest(bucket, targetKey, sourceFile)
        if( publicRead )
            req.withCannedAcl(CannedAccessControlList.PublicRead)

        if (s3Client.doesObjectExist(bucket, targetKey)) {
            if (overwrite) {
                logger.quiet("S3 Upload ${sourceFile} → s3://${bucket}/${targetKey} with overwrite")
                s3Client.putObject(req)
            }
            else {
                throw new GradleException("s3://${bucket}/${targetKey} exists! -- Refuse to owerwrite it.")
            }
        }
        else {
            logger.quiet("S3 Upload ${sourceFile} → s3://${bucket}/${targetKey}")
            s3Client.putObject(req)
        }
    }
}

@CompileStatic
class S3Download extends S3Task {

    @Input
    String bucket

    @Input
    String key

    @Input
    String file

    @Optional
    @Input
    String keyPrefix

    @Optional
    @Input
    String destDir

    @TaskAction
    def task() {
        TransferManager tm = new TransferManager(getS3Client())
        Transfer transfer

        // directory download
        if (keyPrefix != null) {
            logger.quiet("S3 Download recursive s3://${bucket}/${keyPrefix} → ${project.file(destDir)}/")
            transfer = (Transfer) tm.downloadDirectory(bucket, keyPrefix, project.file(destDir))
        }

        // single file download
        else {
            logger.quiet("S3 Download s3://${bucket}/${key} → ${file}")
            File f = new File(file)
            f.parentFile.mkdirs()
            transfer = (Transfer) tm.download(bucket, key, f)
        }

        S3Listener listener = new S3Listener()
        listener.transfer = transfer
        transfer.addProgressListener(listener)
        transfer.waitForCompletion()
    }

    class S3Listener implements ProgressListener {
        Transfer transfer

        DecimalFormat df = new DecimalFormat("#0.0")
        public void progressChanged(ProgressEvent e) {
            logger.info("${df.format(transfer.progress.percentTransferred)}%")
        }
    }
}

