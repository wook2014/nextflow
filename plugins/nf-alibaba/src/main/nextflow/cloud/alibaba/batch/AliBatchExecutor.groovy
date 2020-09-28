package nextflow.cloud.alibaba.batch

import java.nio.file.Path

import com.aliyuncs.batchcompute.main.v20151111.BatchComputeClient
import com.upplication.s3fs.S3Path
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import nextflow.exception.AbortOperationException
import nextflow.executor.Executor
import nextflow.extension.FilesEx
import nextflow.processor.TaskHandler
import nextflow.processor.TaskMonitor
import nextflow.processor.TaskPollingMonitor
import nextflow.processor.TaskRun
import nextflow.util.Duration
import nextflow.util.ServiceName

/**
 * Alibaba Batch executor
 *
 * https://www.alibabacloud.com/help/doc-detail/27994.htm?spm=a2c63.p38356.b99.2.4f467529YvOZI7
 * https://www.alibabacloud.com/help/doc-detail/42410.htm?spm=a2c63.p38356.b99.17.55102bceHVOTfO
 * https://www.alibabacloud.com/help/doc-detail/42391.htm?spm=a2c63.p38356.879954.11.64d8576eEE728E
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@ServiceName('alibatch')
@CompileStatic
class AliBatchExecutor extends Executor {

    private BatchComputeClient client
    private AliOptions opts
    private Path remoteBinDir

    @PackageScope
    BatchComputeClient getClient() { client }

    @Override
    protected void register() {
        super.register()
        setupOpts()
        validateWorkDir()
        validatePathDir()
        uploadBinDir()
        createAliClient()
    }

    protected void validateWorkDir() {
        /*
         * make sure the work dir is a S3 bucket
         */
        if( !(workDir instanceof S3Path) ) {
            session.abort()
            throw new AbortOperationException("When using `$name` executor a S3 bucket must be provided as working directory either using -bucket-dir or -work-dir command line option")
        }
    }

    protected void validatePathDir() {
        def path = session.config.navigate('env.PATH')
        if( path ) {
            log.warn "Environment PATH defined in config file is ignored by AWS Batch executor"
        }
    }

    /*
     * upload local binaries
     */
    protected void uploadBinDir() {
        def disableBinDir = session.getExecConfigProp(name, 'disableRemoteBinDir', false)
        if( session.binDir && !session.binDir.empty() && !disableBinDir ) {
            def s3 = getTempDir()
            log.info "Uploading local `bin` scripts folder to ${s3.toUriString()}/bin"
            remoteBinDir = FilesEx.copyTo(session.binDir, s3)
        }
    }

    protected void setupOpts() {
        opts = new AliOptions(this)
    }
    protected void createAliClient() {
        client = new BatchComputeClient(opts.region, opts.accessKey, opts.secretKey);
    }

    @Override
    protected TaskMonitor createTaskMonitor() {
        TaskPollingMonitor.create(session, name, 1000, Duration.of('10 sec'))
    }

    @Override
    TaskHandler createTaskHandler(TaskRun task) {
        log.trace "[ALI BATCH] Launching process > ${task.name} -- work folder: ${task.workDirStr}"
        new AliBatchTaskHandler(task, this)
    }
}
