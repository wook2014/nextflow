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

    private static PluginManager manager

    static {
        // check mode -- either `dev` or `prod`
        final mode = System.getenv('NXF_PLUGINS_MODE') ?: 'prod'
        if( mode )
            System.setProperty('pf4j.mode', mode)
        // check directory
        final dir = System.getenv('NXF_PLUGINS_DIR') ?: 'plugins'
        if( dir )
            System.setProperty('pf4j.pluginsDir', dir)
        // log
        log.debug "Setting up plugin manager > NXF_PLUGINS_MODE=${mode}; NXF_PLUGINS_DIR=$dir"
    }

    static PluginManager getManager() { manager}

    static synchronized void loadPlugins() {
        if( !manager )
            manager = new NextflowPluginManager()
        manager.loadPlugins()
    }

    static void startPlugins() {
        assert manager, "Plugins not loaded"
        manager.startPlugins()
    }

    static void stopPlugins() {
        if( manager )
            manager.stopPlugins()
    }

    static <T> List<T> getExtensions(Class<T> type) {
        assert manager, "Plugins not loaded"
        manager.getExtensions(type)
    }
}
