package nextflow.plugin

import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.BooleanSupplier

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.extension.Bolts
import nextflow.extension.FilesEx
import org.pf4j.CompoundPluginLoader
import org.pf4j.DefaultPluginLoader
import org.pf4j.DefaultPluginManager
import org.pf4j.JarPluginLoader
import org.pf4j.ManifestPluginDescriptorFinder
import org.pf4j.PluginDescriptorFinder
import org.pf4j.PluginLoader
import org.pf4j.PluginManager
import org.pf4j.PluginStateEvent
import org.pf4j.PluginStateListener
import org.pf4j.update.DefaultUpdateRepository
import org.pf4j.update.UpdateManager
import org.pf4j.update.UpdateRepository
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class PluginsHandler implements PluginStateListener {

    private static final String DEFAULT_PLUGINS_REPO = 'https://raw.githubusercontent.com/nextflow-io/plugins/main/plugins.json'

    private String mode
    private Path root
    private UpdateManager updater
    private PluginManager manager
    private DefaultPlugins defaultPlugins

    PluginsHandler() {
        mode = System.getenv('NXF_PLUGINS_MODE') ?: 'prod'
        root = Paths.get(System.getenv('NXF_PLUGINS_DIR') ?: ( mode=='dev' ? 'plugins' : '.plugins' ) )
        System.setProperty('pf4j.mode', mode)
        defaultPlugins = new DefaultPlugins()
    }

    PluginsHandler(Path root, String mode='prod') {
        this.mode = mode
        this.root = root
        System.setProperty('pf4j.mode', mode)
        defaultPlugins = new DefaultPlugins()
    }

    protected void init(Path root) {
        this.manager = createManager(root)
        this.updater = createUpdater(manager)
    }

    protected PluginManager createManager(Path root) {
        final result = new DefaultPluginManager(root) {
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

        result.addPluginStateListener(this)
        return result
    }


    protected UpdateManager createUpdater(PluginManager manager) {
        final repos = new ArrayList<UpdateRepository>()
        repos << new DefaultUpdateRepository('nextflow.io', new URL(DEFAULT_PLUGINS_REPO))
        new UpdateManager(manager, repos)
    }


    @Override
    void pluginStateChanged(PluginStateEvent ev) {
        final err = ev.plugin.failedException
        final dsc = ev.plugin.descriptor
        if( err ) {
            throw new IllegalStateException("Unable to start plugin id=${dsc.pluginId} version=${dsc.version} -- cause: ${err.message ?: err}", err)
        }
    }

    PluginManager getManager() { manager }

    synchronized void setup(Map config = Collections.emptyMap()) {
        if( manager )
            throw new IllegalArgumentException("Plugin system was already setup")
        else {
            log.debug "Setting up plugin manager > NXF_PLUGINS_MODE=${mode}; NXF_PLUGINS_DIR=$root"
            // make sure plugins dir exists
            if( !FilesEx.mkdirs(root) )
                throw new IOException("Unable to create plugins dir: $root")
            // create the plugin manager
            init(root)
            // load the plugins
            manager.loadPlugins()
            start(config)
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


    void start( String pluginId ) {
         start( defaultPlugins.getPlugin(pluginId) )
    }

    void start(PluginSpec plugin) {
        final result = manager.getPlugin(plugin.id)
        if( !result ) {
            // install & start the plugin
            updater.installPlugin(plugin.id, plugin.version)
        }
        else if( result.descriptor.version != plugin.version ) {
            // update & start the plugin
            updater.updatePlugin(plugin.id, plugin.version)
        }
        else {
            manager.startPlugin(plugin.id)
        }
    }

    void start(Map config) {
        def specs = parseConf(config)
        if( specs ) {
            log.debug "Plugins declared=$specs"
        }
        else {
            specs = defaultPluginsFor(config)
            log.debug "Plugins inferred=$specs"
        }
        for( PluginSpec it : specs ) {
            start(it)
        }
    }

    List<PluginSpec> defaultPluginsFor(Map config) {
        final plugins = new ArrayList<PluginSpec>()
        final executor = Bolts.navigate(config, 'process.executor')

        if( executor == 'awsbatch' )
            plugins << defaultPlugins.getPlugin('nf-amazon')

        if( executor == 'google-lifesciences' )
            plugins << defaultPlugins.getPlugin('nf-google')

        if( executor == 'ignite' )
            plugins << defaultPlugins.getPlugin('nf-ignite')

        return plugins
    }

    /**
     * Lookup for plugins declared in the nextflow.config using the `plugins` scope
     *
     * @param config The nextflow config as a Map object
     * @return The list of declared plugins
     */
    static protected List<PluginSpec> parseConf(Map config) {
        final pluginsConf = config.plugins as List<Map>
        final result = new ArrayList( pluginsConf?.size() ?: 0 )
        if(pluginsConf) for( Map entry : pluginsConf ) {
            result.add( new PluginSpec(entry.id as String, entry.version as String) )
        }
        return result
    }


}
