package nextflow.ga4gh

import groovy.transform.CompileStatic
import org.pf4j.Plugin
import org.pf4j.PluginWrapper

/**
 * Nextflow plugin for GA4GH extensions
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class Ga4ghPlugin extends Plugin {
    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     */
    Ga4ghPlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    @Override
    void start() {
        log.debug("Starting GA4GH plugin");
    }

    @Override
    void stop() {
        log.debug("Stopping GA4GH plugin");
    }
}
