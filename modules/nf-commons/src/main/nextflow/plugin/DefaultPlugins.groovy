package nextflow.plugin

import groovy.transform.CompileStatic

/**
 * Model the collection of default plugins used if no plugins are provided by the user
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class DefaultPlugins {

    private Map<String,PluginSpec> plugins = new HashMap<>(20)

    DefaultPlugins() {
        final meta = this.class.getResourceAsStream('/META-INF/plugins-info.txt')?.text
        plugins = parseMeta(meta)
    }

    protected Map<String,PluginSpec> parseMeta(String meta) {
        if( !meta )
            return Collections.emptyMap()

        final result = new HashMap(20)
        for( String line : meta.readLines() ) {
            final spec = PluginSpec.parse(line)
            result[spec.id] = spec
        }
        return result
    }

    PluginSpec getPlugin(String pluginId) throws IllegalArgumentException {
        if( !pluginId )
            throw new IllegalArgumentException("Missing pluginId argument")
        final result = plugins.get(pluginId)
        if( !result )
            throw new IllegalArgumentException("Unknown Nextflow plugin '$pluginId'")
        return result
    }


    List getPlugins() {
        new ArrayList(plugins.values())
    }

}
