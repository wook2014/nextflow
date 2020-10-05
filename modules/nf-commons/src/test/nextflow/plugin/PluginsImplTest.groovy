package nextflow.plugin

import java.nio.file.Files

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class PluginsImplTest extends Specification {

    def 'should setup plugins' () {
        given:
        def folder = Files.createTempDirectory('test')
        def plugins = new PluginsImpl(folder)

        when:
        plugins.setup([plugins: [ [id: 'nf-console', version: '0.1.0'] ]])
        then:
        folder.resolve('nf-console-0.1.0').exists()

        cleanup:
        folder?.deleteDir()
    }
}
