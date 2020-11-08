package nextflow.ignite


import groovy.transform.CompileStatic
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Implements Apache Ignite plugin
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class IgnitePlugin extends Plugin {

    private static final Logger log = LoggerFactory.getLogger(IgnitePlugin)

    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     */
    IgnitePlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    @Override
    void start() {
        log.debug("Starting Ignite plugin")
    }

    @Override
    void stop() {
        log.debug("Stopping Ignite plugin")
    }
}
