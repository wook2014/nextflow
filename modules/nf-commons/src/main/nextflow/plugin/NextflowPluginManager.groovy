package nextflow.plugin

import java.util.function.BooleanSupplier

import org.pf4j.CompoundPluginLoader
import org.pf4j.DefaultPluginLoader
import org.pf4j.DefaultPluginManager
import org.pf4j.JarPluginLoader
import org.pf4j.ManifestPluginDescriptorFinder
import org.pf4j.PluginDescriptorFinder
import org.pf4j.PluginLoader

/**
 *  Plugin manager specialized for Nextflow build environment
 *
 *  @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class NextflowPluginManager extends DefaultPluginManager {

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new ManifestPluginDescriptorFinder()
    }

    @Override
    protected PluginLoader createPluginLoader() {
        return new CompoundPluginLoader()
                .add(new GroovyDevPluginLoader(this), this::isDevelopment as BooleanSupplier)
                .add(new JarPluginLoader(this), this::isNotDevelopment as BooleanSupplier)
                .add(new DefaultPluginLoader(this), this::isNotDevelopment as BooleanSupplier);
    }
}
