package nextflow.plugin

import java.nio.file.Files

import org.pf4j.DefaultPluginManager
import org.pf4j.update.DefaultUpdateRepository
import org.pf4j.update.UpdateManager
import spock.lang.Ignore
import spock.lang.Specification
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Ignore
class PluginInstallerTest extends Specification {



    def 'should list plugins' () {
        given:
        def folder = Files.createTempDirectory('test')
        println folder
        and:
        def manager = new DefaultPluginManager(folder)
        and:
        def repo = new DefaultUpdateRepository('nextflow', new URL('http://www.nextflow.io.s3-website-eu-west-1.amazonaws.com/plugins/plugins.json'))
        def updater = new UpdateManager(manager, [repo])
        when:
        def list = updater.getAvailablePlugins()
        println list
        then:
        list.size() ==6
        list.find { it.id == 'nf-amazon' }

        when:
        def updates = updater.getUpdates()
        println updates
        then:
        true


        when:
        def plugins = updater.getPlugins()
        println plugins
        then:
        plugins.size() == 6

        when:
        def aws = updater.getLastPluginRelease('nf-amazon')
        println aws
        then:
        aws.version == '0.1.0'

        when:
        def map = updater.getPluginsMap()
        then:
        map.size() == 6


        when:
        updater.installPlugin('nf-amazon','0.1.0')
        then:
        folder.resolve('nf-amazon-0.1.0').exists()

        cleanup:
        folder?.deleteDir()

    }

}
