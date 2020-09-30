package nextflow.cloud.aws

import groovy.transform.CompileStatic
import org.pf4j.Plugin
import org.pf4j.PluginWrapper

/**
 * Nextflow plugin for Amazon extensions
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class AmazonPlugin extends Plugin {
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
        log.debug("Starting AWS plugin");
    }

    @Override
    void stop() {
        log.debug("Stopping AWS plugin");
    }
}
