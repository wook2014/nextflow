package nextflow.cloud.aws

import static nextflow.util.SpuriousDeps.shutdownS3Uploader

import com.upplication.s3fs.S3FileSystemProvider
import groovy.transform.CompileStatic
import nextflow.file.FileHelper
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Nextflow plugin for Amazon extensions
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class AmazonPlugin extends Plugin {

    private static Logger log = LoggerFactory.getLogger(AmazonPlugin)
    
    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     */
    AmazonPlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    @Override
    void start() {
        log.debug("Starting AWS plugin")
        FileHelper.getOrInstallProvider(S3FileSystemProvider)
    }

    @Override
    void stop() {
        log.debug("Stopping AWS plugin")
        shutdownS3Uploader()
    }
}
