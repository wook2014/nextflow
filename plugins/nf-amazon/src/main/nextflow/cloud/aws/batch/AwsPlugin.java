package nextflow.cloud.aws.batch;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public class AwsPlugin extends Plugin {
    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     *
     * @param wrapper
     */
    public AwsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("Starting AWS plugin");
    }

    @Override
    public void stop() {
        System.out.println("Stopping AWS plugin");
    }
}
