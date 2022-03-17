package com.veracode.bamboo.uploadscan;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import org.jetbrains.annotations.NotNull;

import org.apache.log4j.Logger;

public class VeracodeTask implements TaskType
{
	private static final Logger log = Logger.getLogger(VeracodeTask.class);
	
    @NotNull
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
    	// Veracode class to handle all the work
        VeracodeUploadScan veracodeUploadScan = new VeracodeUploadScan(taskContext);
        
        // setup the log snooper for echoing build logs to the Bamboo log
        final VeracodeLogInterceptor logInterceptor = new VeracodeLogInterceptor(taskContext);
        
        log.info("Starting Veracode Task");
               
        final String appName = taskContext.getConfigurationMap().get("appName");
        final String createApp = taskContext.getConfigurationMap().get("createApp");
        final String busCrit = taskContext.getConfigurationMap().get("busCrit");
        final String sandboxName = taskContext.getConfigurationMap().get("sandboxName");
        final String createSandbox = taskContext.getConfigurationMap().get("createSandbox");
        final String scanName = taskContext.getConfigurationMap().get("scanName");
        final String uploadInclude = taskContext.getConfigurationMap().get("uploadInclude");
        final String uploadExclude = taskContext.getConfigurationMap().get("uploadExclude");
        final String scanInclude = taskContext.getConfigurationMap().get("scanInclude");
        final String scanExclude = taskContext.getConfigurationMap().get("scanExclude");
        final String saveasFilename = taskContext.getConfigurationMap().get("saveasFilename");
        final String saveasReplacement = taskContext.getConfigurationMap().get("saveasReplacement");
        final String waitComplete = taskContext.getConfigurationMap().get("waitComplete");
        
        log.debug("execute with: appName = " + appName 
        					+ ", createApp = " + createApp
        					+ ", busCrit = " + busCrit
        					+ ", sandboxName = " + sandboxName
        					+ ", createSandbox = " + createSandbox
        					+ ", scanName = " + scanName
        					+ ", uploadInclude = " + uploadInclude
        					+ ", uploadExclude = " + uploadExclude
        					+ ", scanInclude = " + scanInclude
        					+ ", scanExclude = " + scanExclude
        					+ ", saveasFilename = " + saveasFilename
        					+ ", saveasReplacement = " + saveasReplacement
        					+ ", waitComplete = " + waitComplete);
        
        // do it
        TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
       
        BuildLogger bl = taskContext.getBuildLogger();
        bl.getInterceptorStack().add(logInterceptor);
        
        if(veracodeUploadScan.uploadForScan() < 0)
        	return taskResultBuilder.failed().build();
        else
        	return taskResultBuilder.build();
    }
}
