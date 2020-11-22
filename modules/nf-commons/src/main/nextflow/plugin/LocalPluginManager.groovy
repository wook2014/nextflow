package nextflow.plugin

import java.nio.file.Files
import java.nio.file.Path

import groovy.transform.CompileStatic
import org.pf4j.DefaultPluginLoader
import org.pf4j.DefaultPluginManager
import org.pf4j.ManifestPluginDescriptorFinder
import org.pf4j.PluginDescriptorFinder
import org.pf4j.PluginLoader
import org.pf4j.PluginRepository
/**
 * Custom plugin manager creating tracking plugins with symlinks to
 * the parent repository
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class LocalPluginManager extends DefaultPluginManager {

    LocalPluginManager(Path root) {
        super(root)
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new ManifestPluginDescriptorFinder()
    }

    @Override
    protected PluginLoader createPluginLoader() {
        return new DefaultPluginLoader(this)
    }

    @Override
    protected PluginRepository createPluginRepository() {
        return new LocalPluginRepository(getPluginsRoot())
    }

    /**
     * Override parent {@link DefaultPluginManager#loadPlugin(java.nio.file.Path)} method
     * creating a symlink the plugin path directory in the current plugin root
     *
     * @param pluginPath
     *      The file path where the plugin is stored unzipped) in the
     *      central plugins directory
     */
    @Override
    String loadPlugin(Path pluginPath) {
        if( !pluginPath )
            throw new IllegalArgumentException("Plugin path cannot be null")
        if( !Files.isDirectory(pluginPath) )
            throw new IllegalArgumentException("Plugin path must be a directory: $pluginPath")

        // create a symlink relative to the current root
        final symlink = getPluginsRoot().resolve(pluginPath.getFileName())
        Files.createSymbolicLink(symlink, pluginPath)
        super.loadPlugin(symlink)
    }
}
