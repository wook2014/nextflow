package nextflow.executor;

import org.pf4j.ExtensionPoint;

/**
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
interface ExecutorProvider extends ExtensionPoint {

    Class<? extends Executor> executorType()

}
