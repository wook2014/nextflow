package nextflow.plugin

import groovy.transform.CompileStatic

/**
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
            final tokens = line.tokenize('@')
            final id = tokens[0]
            result[id] = new PluginSpec(id, tokens[1])
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
