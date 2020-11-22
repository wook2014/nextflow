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

    public static final String DEFAULT_PLUGINS_REPO = 'https://raw.githubusercontent.com/nextflow-io/plugins/main/plugins.json'

    private final static PluginsFacade INSTANCE = new PluginsFacade()

    static PluginManager getManager() { INSTANCE.manager }

    static synchronized void setup(Map config = Collections.emptyMap()) {
        INSTANCE.setup(config)
    }

    static void start(String pluginId) {
        INSTANCE.start(pluginId)
    }

    static synchronized void stop() {
        INSTANCE.stop()
    }

    static <T> List<T> getExtensions(Class<T> type) {
        INSTANCE.getExtensions(type)
    }

    static <T> T getExtension(Class<T> type) {
        final allExtensions = INSTANCE.getExtensions(type)
        return allExtensions ? allExtensions.first() : null
    }
}
