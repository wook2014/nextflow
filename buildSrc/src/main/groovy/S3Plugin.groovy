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
import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class S3Extension {
    String profile
    String region
    String bucket
}


abstract class S3Task extends DefaultTask {
    @Input
    String bucket

    String getBucket() { bucket ?: project.s3.bucket }

    @Internal
    def getS3Client() {
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
        s3Client
    }
}


class S3Upload extends S3Task {
    @Input
    final Property<String> key = project.objects.property(String)

    @Input
    final Property<String> file = project.objects.property(String)

    @Input
    boolean overwrite = false

    @TaskAction
    def task() {
        final sourceFile = new File(file.get())
        final targetKey = key.get()

        if( !sourceFile.exists() )
          throw new GradleException("S3 upload failed -- Source file does not exists: $sourceFile")

        if (s3Client.doesObjectExist(bucket, targetKey)) {
            if (overwrite) {
                logger.quiet("S3 Upload ${sourceFile} → s3://${bucket}/${targetKey} with overwrite")
                s3Client.putObject(bucket, targetKey, sourceFile)
            }
            else {
                throw new GradleException("s3://${bucket}/${targetKey} exists! -- Refuse to owerwrite it.")
            }
        }
        else {
            logger.quiet("S3 Upload ${sourceFile} → s3://${bucket}/${targetKey}")
            s3Client.putObject(bucket, targetKey, sourceFile)
        }
    }
}


class S3Download extends S3Task {
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
            transfer = tm.downloadDirectory(bucket, keyPrefix, project.file(destDir))
        }

        // single file download
        else {
            logger.quiet("S3 Download s3://${bucket}/${key} → ${file}")
            File f = new File(file)
            f.parentFile.mkdirs()
            transfer = tm.download(bucket, key, f)
        }

        def listener = new S3Listener()
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


class S3Plugin implements Plugin<Project> {
    void apply(Project target) {
        target.extensions.create('s3', S3Extension)
    }
}
