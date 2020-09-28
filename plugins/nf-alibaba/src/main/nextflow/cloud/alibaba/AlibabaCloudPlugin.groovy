package nextflow.cloud.alibaba

import groovy.transform.CompileStatic;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alibaba cloud plugin entry point
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
public class AlibabaCloudPlugin extends Plugin {

    private static final Logger log = LoggerFactory.getLogger(AlibabaCloudPlugin.class);

    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     */
    public AlibabaCloudPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        log.debug("Starting Alibaba Cloud plugin");
    }

    @Override
    public void stop() {
        log.debug("Stopping Alibaba Cloud plugin");
    }
}
