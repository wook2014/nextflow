package nextflow.plugin

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Primary {

}
