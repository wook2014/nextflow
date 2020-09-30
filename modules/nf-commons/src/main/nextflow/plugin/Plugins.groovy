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
    private static String mode
    private static String dir

    static {
        // check mode -- either `dev` or `prod`
        mode = System.getenv('NXF_PLUGINS_MODE') ?: 'prod'
        if( mode )
            System.setProperty('pf4j.mode', mode)
        // check directory
        dir = System.getenv('NXF_PLUGINS_DIR') ?: 'plugins'
        if( dir )
            System.setProperty('pf4j.pluginsDir', dir)
    }

    static PluginManager getManager() { manager }

    static synchronized void setup() {
        if( manager )
            throw new IllegalArgumentException("Plugin system was already setup")
        else {
            log.debug "Setting up plugin manager > NXF_PLUGINS_MODE=${mode}; NXF_PLUGINS_DIR=$dir"
            manager = new NextflowPluginManager()
            manager.loadPlugins()
            manager.startPlugins()
        }
    }

    static synchronized  void stop() {
        if( manager ) {
            manager.stopPlugins()
            manager = null
        }
    }

    static <T> List<T> getExtensions(Class<T> type) {
        if( !manager )
            setup()
        manager.getExtensions(type)
    }
}
