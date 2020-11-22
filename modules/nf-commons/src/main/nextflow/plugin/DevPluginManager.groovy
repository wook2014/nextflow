package nextflow.plugin

import java.nio.file.Path

import groovy.transform.CompileStatic
import org.pf4j.DefaultPluginManager
import org.pf4j.DevelopmentPluginRepository
import org.pf4j.ManifestPluginDescriptorFinder
import org.pf4j.PluginDescriptorFinder
import org.pf4j.PluginLoader
import org.pf4j.PluginRepository

/**
 * Custom plugin manager that allow loading plugins from Groovy/Gradle/Intellij build environment
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class DevPluginManager extends DefaultPluginManager {

    DevPluginManager(Path root) {
        super(root)
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new ManifestPluginDescriptorFinder()
    }

    @Override
    protected PluginLoader createPluginLoader() {
        return new DevPluginLoader(this)
    }

    @Override
    protected PluginRepository createPluginRepository() {
        return new DevelopmentPluginRepository(getPluginsRoot())
    }

}
