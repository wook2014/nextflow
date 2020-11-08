package nextflow.plugin

import groovy.transform.Canonical

/**
 * Model a plugin Id and version
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Canonical
class PluginSpec {

    /**
     * Plugin unique ID
     */
    String id

    /**
     * The plugin version
     */
    String version

    /**
     * Parse a plugin fully-qualified ID eg. nf-amazon@1.2.0
     *
     * @param fqid The fully qualified plugin id
     * @return A {@link PluginSpec} representing the plugin
     */
    static PluginSpec parse(String fqid) {
        final tokens = fqid.tokenize('@')
        final id = tokens[0]
        return new PluginSpec(id, tokens[1])
    }
}
