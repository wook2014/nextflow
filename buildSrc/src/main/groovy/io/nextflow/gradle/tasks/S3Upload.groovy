package io.nextflow.gradle.tasks

import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.PutObjectRequest
import groovy.transform.CompileStatic
import io.nextflow.gradle.tasks.AbstractS3Task
import io.nextflow.gradle.util.BucketTokenizer
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
/**
 * Upload files to a S3 bucket
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class S3Upload extends AbstractS3Task {

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

    @Input boolean overwrite = false

    @Input boolean publicRead = false

    @Input boolean dryRun = false

    @Input boolean skipExisting

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

        if (s3Client.doesObjectExist(bucket, targetKey)) {
            if (overwrite) {
                copy(sourceFile, bucket, targetKey)
            }
            else if( skipExisting ) {
                logger.quiet("s3://${bucket}/${targetKey} exists! -- Skipping it.")
            }
            else if( !isSameContent(sourceFile, bucket, targetKey)) {
                throw new GradleException("s3://${bucket}/${targetKey} exists! -- Refuse to owerwrite it.")
            }
        }
        else {
            copy(sourceFile, bucket, targetKey)
        }
    }

    boolean isSameContent(File sourceFile, String bucket, String targetKey) {
        final d1= DigestUtils.sha512(sourceFile.bytes)
        final d2= DigestUtils.sha512(s3Client.getObject(bucket, targetKey).getObjectContent())
        return d1==d2
    }

    void copy(File sourceFile, String bucket, String targetKey) {
        if( dryRun ) {
            logger.quiet("S3 Would upload ${sourceFile} → s3://${bucket}/${targetKey} with overwrite")
        }
        else {
            final req = new PutObjectRequest(bucket, targetKey, sourceFile)
            if( publicRead )
                req.withCannedAcl(CannedAccessControlList.PublicRead)

            logger.quiet("S3 Upload ${sourceFile} → s3://${bucket}/${targetKey} with overwrite")
            s3Client.putObject(req)
        }
    }
}
