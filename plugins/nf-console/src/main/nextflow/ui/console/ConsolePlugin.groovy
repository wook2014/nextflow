package nextflow.ui.console

import groovy.transform.CompileStatic
import org.pf4j.Plugin
import org.pf4j.PluginWrapper

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ConsolePlugin extends Plugin {

    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     */
    ConsolePlugin(PluginWrapper wrapper) {
        super(wrapper)
    }


    @Override
    void start() {
        log.debug("Starting nextflow console plugin");
    }

    @Override
    void stop() {
        log.debug("Stopping nextflow console plugin");
    }
}
