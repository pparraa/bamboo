package com.veracode.bamboo.uploadscan;

import com.veracode.apiwrapper.AbstractAPIWrapper;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.build.logger.BuildLogger;

import org.apache.log4j.Logger;

public class CredentialsHelper 
{	
	private TaskContext m_taskContext;
	private BuildLogger m_buildLogger;
	private SecurityUtils m_securityUtils;
	
	private static final Logger log = Logger.getLogger(CredentialsHelper.class);
	
	public CredentialsHelper(final TaskContext taskContext)
	{
		m_taskContext = taskContext;
		m_buildLogger = m_taskContext.getBuildLogger();
		m_securityUtils = new SecurityUtils();
	}
	
	public boolean setUpCredentials(final AbstractAPIWrapper wrapper)
	{
		String credType = m_taskContext.getConfigurationMap().get("credType");
		if(credType.equals("UP"))
		{
			log.debug("setting up Username/password creds");
			
			wrapper.setUpCredentials(m_taskContext.getConfigurationMap().get("credUsername"),
					m_securityUtils.decrypt(m_taskContext.getConfigurationMap().get("credPassword")) );
		}
		else // assume API ID/Key
		{
			log.debug("setting up API creds");
			
			try
			{
				wrapper.setUpApiCredentials(m_taskContext.getConfigurationMap().get("credApiID"),
						m_securityUtils.decrypt(m_taskContext.getConfigurationMap().get("credApiKey")) );
			}
			catch(IllegalArgumentException e)
			{
				// this can get thrown if the API Key is bogus
				m_buildLogger.addErrorLogEntry("Problem setting up API creds" +
						" Exception: " + e.toString());
				
				return false;				
			}
		}
		
		return true;
	}
}
