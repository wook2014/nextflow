package nextflow.plugin

import java.nio.file.Files
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.pf4j.DefaultPluginRepository

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class LocalPluginRepository extends DefaultPluginRepository {

    LocalPluginRepository(Path pluginsRoot) {
        super(pluginsRoot)
    }

    /**
     * Override {@link DefaultPluginRepository#deletePluginPath(java.nio.file.Path)}
     * to prevent deleting the real plugin directory
     *
     *  @param pluginPath
     * @return
     */
    @Override
    boolean deletePluginPath(Path pluginPath) {
        if(Files.isSymbolicLink(pluginPath)) {
            try {
                Files.delete(pluginPath)
                return true
            }
            catch (Exception e) {
                log.debug "Unable to delete plugin path: $pluginPath"
            }
        }

        return super.deletePluginPath(pluginPath)
    }
}
