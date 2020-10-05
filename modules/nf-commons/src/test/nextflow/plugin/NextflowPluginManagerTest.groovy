package nextflow.plugin

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class NextflowPluginManagerTest extends Specification {


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
        final plugins = NextflowPluginManager.parseConf(cfg.plugins)
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
