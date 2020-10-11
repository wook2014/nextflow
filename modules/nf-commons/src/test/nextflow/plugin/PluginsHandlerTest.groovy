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
        plugins.setup([plugins: [ [id: 'nf-console', version: '0.1.0'] ]])
        then:
        folder.resolve('nf-console-0.1.0').exists()

        cleanup:
        folder?.deleteDir()
    }


    def 'should parse plugins config' () {
        given:
        def CONFIG = '''
        plugins = [ 
            [id: 'foo', version: '1.2.3'],
            [id: 'bar', version: '3.2.1'],
         ]
        '''

        when:
        def cfg = new ConfigSlurper().parse(CONFIG)
        then:
        cfg.plugins.size() == 2
        cfg.plugins[0].id == 'foo'
        cfg.plugins[0].version == '1.2.3'

        when:
        final plugins = PluginsHandler.parseConf(cfg)
        then:
        plugins.size() == 2
        and:
        plugins[0].id == 'foo'
        plugins[0].version == '1.2.3'
        and:
        plugins[1].id == 'bar'
        plugins[1].version == '3.2.1'
    }
}
