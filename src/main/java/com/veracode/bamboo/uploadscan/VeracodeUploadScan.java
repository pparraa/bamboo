package com.veracode.bamboo.uploadscan;

import com.veracode.apiwrapper.wrappers.*;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.build.logger.BuildLogger;
import java.io.IOException;
import com.atlassian.bamboo.utils.i18n.*;
import com.atlassian.spring.container.LazyComponentReference;

import java.text.ParseException;
import javax.xml.stream.XMLStreamException;
import org.apache.log4j.Logger;

public class VeracodeUploadScan 
{
	private TaskContext m_taskContext;
	private BuildLogger m_buildLogger;
	private UploadAPIWrapper m_uploadAPIWrapper;
	private SandboxAPIWrapper m_sandboxAPIWrapper;
	private BuildManager m_buildManager;
	private String m_appName;
	private String m_appID;
	private String m_sandboxID = null;
	private CredentialsHelper m_credentialsHelper;
	
	// import the i18nBean so I can access the text in the i18n file
	// TODO: refactor all the i18n stuff out into a separate class?
    LazyComponentReference<I18nBeanFactory> i18nBeanFactoryReference =
            new LazyComponentReference<I18nBeanFactory>("i18nBeanFactory");
    I18nBeanFactory i18nBeanFactory = i18nBeanFactoryReference.get();
    I18nBean m_i18nBean = i18nBeanFactory.getI18nBean();
    
	private static final Logger log = Logger.getLogger(VeracodeUploadScan.class);
	
	public VeracodeUploadScan(final TaskContext taskContext)
	{
		// setup Bamboo stuff
		m_taskContext = taskContext;
		m_buildLogger = m_taskContext.getBuildLogger();
		m_appName = m_taskContext.getConfigurationMap().get("appName");
		
		// setup Veracode stuff - Creds, etc.
		m_uploadAPIWrapper = new UploadAPIWrapper();
		m_credentialsHelper = new CredentialsHelper(m_taskContext);
	}
	
	int uploadForScan()
	{
		m_buildLogger.addBuildLogEntry("Starting Veracode UploadScan");
		
		if(!m_credentialsHelper.setUpCredentials(m_uploadAPIWrapper))
			return -1;
		
		m_buildLogger.addBuildLogEntry("Searching for existing app: " + m_appName);
		
		try
		{
			String appListXML = m_uploadAPIWrapper.getAppList();
			log.debug("App List XML: " + appListXML);
		
			try
			{
				// get the App ID
				ParseAppDataXML parser = new ParseAppDataXML(appListXML);
				
				m_appID = parser.getAppIDFromList(m_appName);
				m_buildLogger.addBuildLogEntry("Found existing app: " + m_appName + " with appID = " + m_appID);
			}
			catch (ParseException p)
			{
				// failed to find app name
				if( m_taskContext.getConfigurationMap().get("createApp").equalsIgnoreCase("true") )
				{
					// didn't find app, but asked to create a new one, so try
					try
					{
						String bc = m_taskContext.getConfigurationMap().get("busCrit");
						String cl = m_i18nBean.getText("uploadscan.busCrit." + bc);
						
						m_buildLogger.addBuildLogEntry("Creating new app " + m_appName +
								" with Crit Level " + cl);
						
						String appIDXML = m_uploadAPIWrapper.createApp(m_appName, cl);
						
						log.debug("Created App ID XML: " + appIDXML);
						
						// get the appID
						ParseAppDataXML m_parser = new ParseAppDataXML(appIDXML);
						
						try
						{
							m_appID = m_parser.getAppIDFromInfo(m_appName);
							m_buildLogger.addBuildLogEntry("New app: " + m_appName + " with appID = " + m_appID);
						}
						catch (ParseException pp)
						{
							m_buildLogger.addErrorLogEntry("Unable to get AppID from newly created app: " + m_appName +
									" Exception: " + pp.toString());
							return -1;
						}
						catch (XMLStreamException x)
						{
							m_buildLogger.addErrorLogEntry("Unable to get AppID from newly created app: " + m_appName +
									" Exception: " + x.toString());
							return -1;	
						}
					}
					catch(IOException e)
					{
						m_buildLogger.addErrorLogEntry("Unable to create app: " + m_appName +
								" Exception: " + e.toString());
						return -1;
					}	
				}
				else	// not found, and not asked to create
				{
					m_buildLogger.addErrorLogEntry("Unable to find existing app: " + m_appName +
							" and 'Create App' not set.");
					return -1;
				}
			}
			catch (XMLStreamException e)
			{
				m_buildLogger.addErrorLogEntry(e.toString());
				return -1;
			}
		}
		catch(IOException e)
		{
			m_buildLogger.addErrorLogEntry("Error getting app list: " + m_appName +
					" Exception: " + e.toString());
			return -1;
		}
		
		// success finding/creating the app, move on
			
		// if we have a sandbox name use it (and create if necessary)
		if( !m_taskContext.getConfigurationMap().get("sandboxName").isEmpty())
		{
			String sandboxName = m_taskContext.getConfigurationMap().get("sandboxName");
			m_buildLogger.addBuildLogEntry("Using Sandbox " + sandboxName);
			
			m_sandboxAPIWrapper = new SandboxAPIWrapper();
			
			// setup creds for Sandbox API
			if(!m_credentialsHelper.setUpCredentials(m_sandboxAPIWrapper))
				return -1;
			
			// Find (or create) the sandbox
			try
			{
				String sandboxListXML = m_sandboxAPIWrapper.getSandboxList(m_appID);
				log.debug("Sandbox List XML: " + sandboxListXML);
			
				try
				{
					// get the Sandbox ID
					ParseAppDataXML parser = new ParseAppDataXML(sandboxListXML);
					
					m_sandboxID = parser.getSandboxIDFromList(m_appID, sandboxName);
					m_buildLogger.addBuildLogEntry("Found existing sandbox: " + sandboxName + " with sandboxID = " + m_sandboxID);
				}
				catch (ParseException p)
				{
					// failed to find sandbox name
					if( m_taskContext.getConfigurationMap().get("createSandbox").equalsIgnoreCase("true") )
					{
						// didn't find sandbox, but asked to create a new one, so try
						try
						{							
							m_buildLogger.addBuildLogEntry("Creating new sandbox " + sandboxName);
							
							String sandboxIDXML = m_sandboxAPIWrapper.createSandbox(m_appID, sandboxName);
							
							log.debug("Created Sandbox ID XML: " + sandboxIDXML);
							
							// get the appID
							ParseAppDataXML m_parser = new ParseAppDataXML(sandboxIDXML);
							
							try
							{
								m_sandboxID = m_parser.getSandboxIDFromInfo(sandboxName);
								m_buildLogger.addBuildLogEntry("New Sandbox: " + sandboxName + " with sandboxID = " + m_sandboxID);
							}
							catch (ParseException pp)
							{
								m_buildLogger.addErrorLogEntry("Unable to get SandboxID from newly created sandbox: " + sandboxName +
										" Exception: " + pp.toString());
								return -1;
							}
							catch (XMLStreamException x)
							{
								m_buildLogger.addErrorLogEntry("Unable to get SandboxID from newly created sandbox: " + sandboxName +
										" Exception: " + x.toString());
								return -1;	
							}
						}
						catch(IOException e)
						{
							m_buildLogger.addErrorLogEntry("Unable to create sandbox: " + sandboxName +
									" Exception: " + e.toString());
							return -1;
						}	
					}
					else	// not found, and not asked to create
					{
						m_buildLogger.addErrorLogEntry("Unable to find existing app: " + m_appName +
								" and 'Create App' not set.");
						return -1;
					}
				}
				catch (XMLStreamException e)
				{
					m_buildLogger.addErrorLogEntry(e.toString());
					return -1;
				}
			}
			catch(IOException e)
			{
				m_buildLogger.addErrorLogEntry("Error getting sandbox list: " + m_appName +
						" Exception: " + e.toString());
				return -1;
			}
		}
		
		// create build and do the scan
		m_buildManager = new BuildManager(m_taskContext, m_uploadAPIWrapper, m_appID, m_sandboxID);
		int result = m_buildManager.doScan();
		
		return result;
	}
}
