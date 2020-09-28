package nextflow.cloud.alibaba.batch

import java.nio.file.Path

import com.aliyuncs.batchcompute.main.v20151111.BatchComputeClient
import com.aliyuncs.batchcompute.pojo.v20151111.Command
import com.aliyuncs.batchcompute.pojo.v20151111.DAG
import com.aliyuncs.batchcompute.pojo.v20151111.JobDescription
import com.aliyuncs.batchcompute.pojo.v20151111.Parameters
import com.aliyuncs.batchcompute.pojo.v20151111.TaskDescription
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.executor.BashWrapperBuilder
import nextflow.processor.TaskBean
import nextflow.processor.TaskHandler
import nextflow.processor.TaskRun
import nextflow.processor.TaskStatus
import nextflow.util.Escape

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class AliBatchTaskHandler extends TaskHandler {

    private Path exitFile
    private Path wrapperFile
    private Path outputFile
    private Path errorFile
    private Path logFile
    private Path scriptFile
    private Path inputFile
    private Path traceFile

    private TaskBean bean
    private BatchComputeClient client
    private AliBatchExecutor executor
    private Map<String,String> environment

    private volatile String jobId

    AliBatchTaskHandler(TaskRun task, AliBatchExecutor executor) {
        super(task)
        this.bean = new TaskBean(task)
        this.executor = executor
        this.client = executor.client
        this.environment = System.getenv()

        this.logFile = task.workDir.resolve(TaskRun.CMD_LOG)
        this.scriptFile = task.workDir.resolve(TaskRun.CMD_SCRIPT)
        this.inputFile =  task.workDir.resolve(TaskRun.CMD_INFILE)
        this.outputFile = task.workDir.resolve(TaskRun.CMD_OUTFILE)
        this.errorFile = task.workDir.resolve(TaskRun.CMD_ERRFILE)
        this.exitFile = task.workDir.resolve(TaskRun.CMD_EXIT)
        this.wrapperFile = task.workDir.resolve(TaskRun.CMD_RUN)
        this.traceFile = task.workDir.resolve(TaskRun.CMD_TRACE)
    }


    @Override
    boolean checkIfRunning() {
        // https://www.alibabacloud.com/help/doc-detail/43042.htm?spm=a2c63.p38356.879954.12.4f467529iysOBu
        final getJobResponse = client.getJob(jobId)
        final job = getJobResponse.getJob()
        log.debug "[ALI BATCH] Job $jobId; state=$job.state"
        if( job.startTime || job.endTime ) {
            this.status = TaskStatus.RUNNING
            return true
        }
        else
            return false
    }

    @Override
    boolean checkIfCompleted() {
        assert jobId
        if( !isRunning() )
            return false
        final getJobResponse = client.getJob(jobId)
        final job = getJobResponse.getJob()
        final done = job?.state in ['Failed', 'Finished']
        log.debug "[ALI BATCH] Job $jobId; state=$job.state"
        if( done ) {
            // finalize the task
            task.exitStatus = readExitFile()
            task.stdout = outputFile
            task.stderr = errorFile
            status = TaskStatus.COMPLETED
            return true
        }
        else
            return false
    }

    private int readExitFile() {
        try {
            exitFile.text as Integer
        }
        catch( Exception e ) {
            log.debug "[AWS BATCH] Cannot read exitstatus for task: `$task.name` | ${e.message}"
            return Integer.MAX_VALUE
        }
    }

    @Override
    void kill() {
        assert jobId
        log.trace "[ALI BATCH] killing job=$jobId"
        try {
            client.stopJob(jobId)
        }
        catch (Exception e) {
            log.warn "Unable to stop job id: $jobId -- Cause: ${e.message ?: e}"
        }
    }

    @Override
    void submit() {
        buildTaskWrapper()

        final req = makeJobReq()
        log.trace "[ALIB BATCH] new job request > $req"

        final resp = client.createJob(req)
        this.jobId = resp.jobId
        this.status = TaskStatus.SUBMITTED
        log.debug "[ALI BATCH] submitted > job=$jobId; work-dir=${task.getWorkDirStr()}"

    }

    protected void buildTaskWrapper() {
        final builder = new BashWrapperBuilder(bean)
        // this is needed to change the work directory into
        builder.headerScript = "NXF_CHDIR=${Escape.path(task.workDir)}"
        //  build launcher scripts
        builder.build()
    }


    protected String getSubmitCommand() {
        "bash ${wrapperFile}"
    }

    protected Map<String,String> getEnvVars() {
        if( environment.containsKey('NXF_DEBUG')) {
            final result = new HashMap(5)
            result.put( 'NXF_DEBUG', this.environment['NXF_DEBUG'] )
            return result
        }
        else
            return Collections.emptyMap()
    }

    protected JobDescription makeJobReq() {
        final result = new JobDescription();
        result.setName( task.name )
        result.setPriority(0)
        result.setJobFailOnInstanceFail(true)
        result.setType("DAG")

        final dag = new DAG()
        result.setDag(dag)

        final cmd = new Command()
            cmd.setCommandLine(getSubmitCommand())
            cmd.setEnvVars(getEnvVars())
        final params = new Parameters()
            params.setCommand(cmd)

        final operation =  new TaskDescription();
            operation.setTaskName( task.name )
            operation.setInstanceCount(1)
            operation.setParameters(params)
            operation.addInputMapping(remoteWorkDir(), localWorkDir());
            operation.addOutputMapping(localWorkDir(), remoteWorkDir());

        dag.addTask(operation)
        return result
    }

    protected String remoteWorkDir() {
        def path = task.workDirStr
        // since NF uses S3 library, replace s3
        assert path.startsWith('s3://')
        path = path.replace('s3://', 'oss://')
        // directory must end with `/`
        if( !path.endsWith('/') )
            path += '/'
        return path
    }

    protected String localWorkDir() {
        def path = task.workDir.toString()
        // directory must end with `/`
        if( !path.endsWith('/') )
            path += '/'
        return path
    }


}
