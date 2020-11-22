package nextflow.plugin


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.pf4j.BasePluginLoader
import org.pf4j.PluginManager

/**
 * Plugin loader for Groovy based development environment
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class DevPluginLoader extends BasePluginLoader {

    DevPluginLoader(PluginManager pluginManager) {
        super(pluginManager, new DevPluginClasspath())
    }

}
