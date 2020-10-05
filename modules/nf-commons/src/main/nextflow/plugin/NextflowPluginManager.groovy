package nextflow.plugin

import java.nio.file.Path
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
import org.pf4j.update.DefaultUpdateRepository
import org.pf4j.update.UpdateManager
import org.pf4j.update.UpdateRepository
/**
 *  Plugin manager specialized for Nextflow build environment
 *
 *  @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class NextflowPluginManager extends DefaultPluginManager implements PluginStateListener {


    private UpdateManager updater

    NextflowPluginManager(Path pluginsRoot) {
        super(pluginsRoot)
        addPluginStateListener(this)
        createUpdater()
    }

    protected void createUpdater() {
        final repos = new ArrayList<UpdateRepository>()
        repos << new DefaultUpdateRepository('nextflow.io', new URL('http://www.nextflow.io.s3-website-eu-west-1.amazonaws.com/plugins/plugins.json'))
        this.updater = new UpdateManager(this, repos)
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


    void start(PluginSpec plugin) {
        final result = getPlugin(plugin.id)
        if( !result ) {
            // install & start the plugin
            updater.installPlugin(plugin.id, plugin.version)
        }
        else if( result.descriptor.version != plugin.version ) {
            // update & start the plugin
            updater.updatePlugin(plugin.id, plugin.version)
        }
        else {
            startPlugin(plugin.id)
        }
    }

    void start(List<Map> config) {
        final specs = parseConf(config)
        for( PluginSpec it : specs ) {
            start(it)
        }
    }


    static protected List<PluginSpec> parseConf(List<Map> pluginsConf) {
        final result = new ArrayList( pluginsConf?.size() ?: 0 )
        if(pluginsConf) for( Map entry : pluginsConf ) {
            result.add( new PluginSpec(entry.id as String, entry.version as String) )
        }
        return result
    }
}
