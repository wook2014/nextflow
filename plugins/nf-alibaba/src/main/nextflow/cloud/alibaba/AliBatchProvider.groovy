package nextflow.cloud.alibaba;

import nextflow.cloud.alibaba.batch.AliBatchExecutor;
import nextflow.executor.Executor;
import nextflow.executor.ExecutorProvider;
import org.pf4j.Extension;

/**
 * Extension point to plugin-in Alibaba Batch executor
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Extension
public class AliBatchProvider implements ExecutorProvider {

    @Override
    public Class<? extends Executor> executorType() {
        return AliBatchExecutor.class;
    }
}
