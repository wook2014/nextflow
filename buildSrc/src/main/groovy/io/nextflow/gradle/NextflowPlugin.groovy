package io.nextflow.gradle


import groovy.transform.CompileStatic
import io.nextflow.gradle.model.S3Extension
import org.gradle.api.Plugin
import org.gradle.api.Project
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

