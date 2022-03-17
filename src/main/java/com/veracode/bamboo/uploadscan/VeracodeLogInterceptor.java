package com.veracode.bamboo.uploadscan;

import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.build.logger.LogInterceptor;
import org.apache.log4j.Logger;
import com.atlassian.bamboo.build.LogEntry;
import com.atlassian.bamboo.v2.build.BuildContext;


/* class to snoop the build log and echo it to the Bamboo system log
 * (only when DEBUG is enabled) - makes for easier debugging
 */
@SuppressWarnings("serial")
public class VeracodeLogInterceptor implements LogInterceptor {

	private BuildContext m_buildContext;
	private static final Logger log = Logger.getLogger(VeracodeLogInterceptor.class);
	
	public VeracodeLogInterceptor(final TaskContext taskContext)
	{
		this.m_buildContext = taskContext.getBuildContext();
		log.debug("Log Interceptor implemented");
		 
	}
	
	public void intercept(LogEntry logEntry)
	{		
		if(log.isDebugEnabled())
			log.debug(logEntry);
	}
	
	public void interceptError(LogEntry logEntry)
	{		
		if(log.isDebugEnabled())
			log.debug(logEntry);
	}

}
