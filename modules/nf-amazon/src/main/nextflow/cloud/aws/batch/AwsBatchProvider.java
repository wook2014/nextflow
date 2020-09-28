package nextflow.cloud.aws.batch;

import nextflow.executor.ExecutorProvider;
import org.pf4j.Extension;

/**
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Extension
public class AwsBatchProvider implements ExecutorProvider {

    @Override
    public Class<AwsBatchExecutor> executorType() {
        return AwsBatchExecutor.class;
    }
}
