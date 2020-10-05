package nextflow.plugin

import java.nio.file.Path
import java.nio.file.Paths

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.pf4j.PluginManager

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class PluginsImpl {

    private NextflowPluginManager manager
    private String mode
    private String root

    PluginsImpl() {
        init(
            Paths.get(System.getenv('NXF_PLUGINS_DIR') ?: 'plugins'),
            System.getenv('NXF_PLUGINS_MODE') ?: 'prod' )
    }

    PluginsImpl(Path root, String mode='prod') {
        init(root, mode)
    }

    private void init(Path root, String mode) {
        System.setProperty('pf4j.mode', mode)
        this.mode = mode
        this.root = root
    }


    PluginManager getManager() { manager }

    synchronized void setup(Map config = Collections.emptyMap()) {
        if( manager )
            throw new IllegalArgumentException("Plugin system was already setup")
        else {
            final pluginsConf = (List<Map>) config.plugins
            log.debug "Setting up plugin manager > NXF_PLUGINS_MODE=${mode}; NXF_PLUGINS_DIR=$root"
            manager = new NextflowPluginManager(Paths.get(root))
            manager.loadPlugins()
            manager.start(pluginsConf)
        }
    }

    synchronized void stop() {
        if( manager ) {
            manager.stopPlugins()
            manager = null
        }
    }

    def <T> List<T> getExtensions(Class<T> type) {
        if( !manager )
            setup()
        manager.getExtensions(type)
    }

}
