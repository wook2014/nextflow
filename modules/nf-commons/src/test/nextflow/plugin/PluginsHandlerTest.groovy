package nextflow.plugin

import java.nio.file.Files

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class PluginsHandlerTest extends Specification {

    def 'should setup plugins' () {
        given:
        def folder = Files.createTempDirectory('test')
        def plugins = new PluginsHandler(folder)

        when:
        plugins.setup([plugins: [ 'nf-console@0.1.0' ]])
        then:
        folder.resolve('nf-console-0.1.0').exists()

        cleanup:
        folder?.deleteDir()
    }


    def 'should parse plugins config' () {
        given:
        def handler = new PluginsHandler()
        and:
        def cfg = [plugins: [ 'foo@1.2.3', 'bar@3.2.1' ]]

        when:
        final plugins = handler.parseConf(cfg)
        then:
        plugins.size() == 2
        and:
        plugins[0].id == 'foo'
        plugins[0].version == '1.2.3'
        and:
        plugins[1].id == 'bar'
        plugins[1].version == '3.2.1'
    }

    def 'should return plugin requirements' () {
        given:
        def defaults = new DefaultPlugins(plugins: [
                'nf-amazon': new PluginSpec('nf-amazon', '0.1.0'),
                'nf-google': new PluginSpec('nf-google', '0.1.0'),
                'nf-ignite': new PluginSpec('nf-ignite', '0.1.0'),
                'nf-tower': new PluginSpec('nf-tower', '0.1.0')
        ])
        and:
        def handler = new PluginsHandler(defaultPlugins: defaults, env: [:])

        when:
        def result = handler.pluginsRequirement([:])
        then:
        result == []

        when:
        handler = new PluginsHandler(defaultPlugins: defaults, env: [NXF_PLUGINS_DEFAULT:'true'])
        result = handler.pluginsRequirement([:])
        then:
        result == [ new PluginSpec('nf-amazon', '0.1.0')]

        when:
        handler = new PluginsHandler(defaultPlugins: defaults, env: [NXF_PLUGINS_DEFAULT:'true'])
        result = handler.pluginsRequirement([tower:[enabled:true]])
        then:
        result == [
                new PluginSpec('nf-amazon', '0.1.0'),
                new PluginSpec('nf-tower', '0.1.0') ]
    }

    def 'should return default plugins given config' () {
        given:
        def defaults = new DefaultPlugins(plugins: [
                'nf-amazon': new PluginSpec('nf-amazon', '0.1.0'),
                'nf-google': new PluginSpec('nf-google', '0.1.0'),
                'nf-ignite': new PluginSpec('nf-ignite', '0.1.0'),
                'nf-tower': new PluginSpec('nf-tower', '0.1.0')
        ])
        and:
        def handler = new PluginsHandler(defaultPlugins: defaults)

        when:
        def plugins = handler.defaultPluginsConf([process:[executor: 'awsbatch']])
        then:
        plugins.find { it.id == 'nf-amazon' }
        !plugins.find { it.id == 'nf-google' }
        !plugins.find { it.id == 'nf-ignite' }
        
        when:
        plugins = handler.defaultPluginsConf([process:[executor: 'google-lifesciences']])
        then:
        plugins.find { it.id == 'nf-google' }
        !plugins.find { it.id == 'nf-amazon' }
        !plugins.find { it.id == 'nf-ignite' }

        when:
        plugins = handler.defaultPluginsConf([process:[executor: 'ignite']])
        then:
        plugins.find { it.id == 'nf-ignite' }
        plugins.find { it.id == 'nf-amazon' }
        !plugins.find { it.id == 'nf-google' }

        when:
        plugins = handler.defaultPluginsConf([:])
        then:
        plugins.find { it.id == 'nf-amazon' }
        !plugins.find { it.id == 'nf-ignite' }
        !plugins.find { it.id == 'nf-google' }

    }
}
