package nextflow.plugin


import org.pf4j.PluginManager
import org.pf4j.update.UpdateManager
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class DevPluginUpdater extends UpdateManager {

    DevPluginUpdater(PluginManager pluginManager) {
        super(pluginManager)
    }

    @Override
    boolean installPlugin(String id, String version) {
        throw new UnsupportedOperationException("Install is not supported on dev mode - Missing plugin $id $version")
    }

    @Override
    boolean updatePlugin(String id, String version) {
        throw new UnsupportedOperationException("Update is not supported on dev mode - Missing plugin $id $version")
    }
}
