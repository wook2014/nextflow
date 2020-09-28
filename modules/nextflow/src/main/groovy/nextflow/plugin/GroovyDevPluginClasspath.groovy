package nextflow.plugin

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j;
import org.pf4j.PluginClasspath;

/**
 * Customise classpath loader for Groovy based
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class GroovyDevPluginClasspath extends PluginClasspath {

    GroovyDevPluginClasspath() {
        // the path where classes are resources should be found in the dev environment
        // for each plugin project directory
        addClassesDirectories("build/classes/groovy/main", "build/resources/main")

        // the path where the plugin dependencies jar files are expected to be found
        // note: this path is not created automatically by Gradle, it should be created by a custom task
        // see `targetLibs` task in the base plugins `build.gradle`
        addJarsDirectories('build/target/libs')

        log.debug "Groovy DEV plugin classpath: classes-dirs=${getClassesDirectories()}; jars-dirs=${getJarsDirectories()}"
    }

}
