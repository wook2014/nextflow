package nextflow.plugin

import java.util.function.BooleanSupplier

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.pf4j.CompoundPluginLoader
import org.pf4j.DefaultPluginLoader
import org.pf4j.DefaultPluginManager
import org.pf4j.JarPluginLoader
import org.pf4j.ManifestPluginDescriptorFinder
import org.pf4j.PluginDescriptorFinder
import org.pf4j.PluginLoader

/**
 * Plugin manager specialized for Nextflow build environment
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@Singleton
@CompileStatic
class NextflowPlugins extends DefaultPluginManager {

    {
        final mode = System.getProperty('pf4j.mode')
        final dir = System.getProperty('pf4j.pluginsDir')
        log.debug "Creating plugin manager > pf4j.mode=${mode}; pf4j.pluginsDir=$dir"
    }

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
