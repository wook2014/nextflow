package nextflow.cloud.alibaba.batch

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.exception.AbortOperationException

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class AliOptions {

    private String region
    private String accessKey
    private String secretKey

    String getRegion() { region }
    String getAccessKey() { accessKey }
    String getSecretKey() { secretKey }

    /** only for testing - do not use */
    protected AliOptions() {}

    AliOptions(AliBatchExecutor executor) {
        this(executor.session)
    }

    AliOptions(Session session) {
        region = session.config.navigate('alibaba.region')
        accessKey = session.config.navigate('alibaba.accessKey')
        secretKey = session.config.navigate('alibaba.secretKey')

        if( !region ) throw new AbortOperationException("Missing Alibaba cloud region attribute")
        if( !accessKey ) throw new AbortOperationException("Missing Alibaba cloud accessKey attribute")
        if( !secretKey ) throw new AbortOperationException("Missing Alibaba cloud secretKey attribute")
    }
}
