package nextflow.plugin


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.pf4j.PluginManager
/**
 * Plugin manager specialized for Nextflow build environment
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class Plugins {

    private final static PluginsImpl INSTANCE = new PluginsImpl()

    static PluginManager getManager() { INSTANCE.manager }

    static synchronized void setup(Map config = Collections.emptyMap()) {
        INSTANCE.setup(config)
    }

    static synchronized void stop() {
        INSTANCE.stop()
    }

    static <T> List<T> getExtensions(Class<T> type) {
        INSTANCE.getExtensions(type)
    }
}
