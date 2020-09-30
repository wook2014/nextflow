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
import org.pf4j.PluginStateEvent
import org.pf4j.PluginStateListener

/**
 *  Plugin manager specialized for Nextflow build environment
 *
 *  @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class NextflowPluginManager extends DefaultPluginManager implements PluginStateListener {

    NextflowPluginManager() {
        addPluginStateListener(this)
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

    @Override
    void pluginStateChanged(PluginStateEvent ev) {
        final err = ev.plugin.failedException
        final dsc = ev.plugin.descriptor
        if( err ) {
            throw new IllegalStateException("Unable to start plugin id=${dsc.pluginId} version=${dsc.version} -- cause: ${err.message ?: err}", err)
        }
    }
}
