package io.seqera.tower.plugin

import groovy.transform.CompileStatic
import org.pf4j.Plugin
import org.pf4j.PluginWrapper

/**
 * Nextflow Tower plugin
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class TowerPlugin extends Plugin {
    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     */
    TowerPlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    @Override
    void start() {
        log.debug("Starting Tower plugin");
    }

    @Override
    void stop() {
        log.debug("Stopping Tower  plugin");
    }
}
