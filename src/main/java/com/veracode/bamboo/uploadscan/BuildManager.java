package com.veracode.bamboo.uploadscan;

import com.veracode.apiwrapper.wrappers.*;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

public class BuildManager 
{
	private TaskContext m_taskContext;
	private BuildLogger m_buildLogger;
	private String m_appName;
	private String m_appID;
	private String m_sandboxID;
	private String m_scanName;
	private String m_buildID;
	private UploadAPIWrapper m_uploadAPIWrapper;
	private CredentialsHelper m_credentialsHelper;
	private FileUtils m_fileUtil;
	private final int m_sleepSecs = 120;
	
	private static final Logger log = Logger.getLogger(BuildManager.class);
	
	
	public BuildManager(final TaskContext taskContext, final UploadAPIWrapper uploadAPIWrapper, final String appID, final String sandboxID)
	{
		m_taskContext = taskContext;
		m_buildLogger = m_taskContext.getBuildLogger();
		m_appID = appID;
		m_sandboxID = sandboxID;
		
		m_uploadAPIWrapper = uploadAPIWrapper;
		
		m_fileUtil = new FileUtils(m_taskContext);
		m_credentialsHelper = new CredentialsHelper(m_taskContext);
		
		m_appName = m_taskContext.getConfigurationMap().get("appName");
		m_scanName = m_taskContext.getConfigurationMap().get("scanName");
	}
	
	public int doScan()
	{
		int result;
		
		m_buildLogger.addBuildLogEntry("Starting scan");
	
		// Also use as a check for an existing build?
		result = createBuild();
		if(result != 0)
			return result;
		
		// add file(s) to build
		// this will create a build if none exists
		result = uploadFiles();
		if(result != 0)
			return result;
		
		// start scan
		result = runScan();
		if(result != 0)
			return result;
		
		// optional: wait for scan to finish
		if(m_taskContext.getConfigurationMap().get("waitComplete").equalsIgnoreCase("true"))
			result = waitForScan();
		
		return result;
	}
	
	// create a new scan/build for our appID
	private int createBuild()
	{
		log.info("Creating build");
		
		try
		{
			String buildInfoXML = m_uploadAPIWrapper.createBuild(m_appID, 
																	m_scanName,
																	null,	// platform
																	null,	// platform_id
																	null,	// lifecycle_stage
																	null, 	// lifecycle_stage_id
																	null,	// launch_date
																	m_sandboxID);
			
			log.debug("create Build XML: " + buildInfoXML);
			
			// get the Build ID
			ParseAppDataXML parser = new ParseAppDataXML(buildInfoXML);
			
			try
			{
				m_buildID = parser.getBuildIDFromInfo(m_scanName);
				m_buildLogger.addBuildLogEntry("New Scan created: " + m_scanName + " with buildID = " + m_buildID);
				return 0;
			}
			catch (ParseException pp)
			{
				m_buildLogger.addErrorLogEntry("Unable to get BuildID from newly created build: " + m_scanName +
						"result of createBuild: " + buildInfoXML + " Exception: " + pp.toString());
				return -1;
			}
			catch (XMLStreamException x)
			{
				m_buildLogger.addErrorLogEntry("Unable to get BuildID from newly created build: " + m_scanName +
						" Exception: " + x.toString());
				return -1;	
			}
		}
		catch(IOException e)
		{
			m_buildLogger.addErrorLogEntry("Unable to create new scan for app: " + m_appName +
					" Exception: " + e.toString());
			return -1;
		}
	}
	
	
	private int uploadFiles()
	{
		log.info("Uploading File(s)");
		
		// get list of files to upload from user params
		Map<String, String> fileList = m_fileUtil.getUploadList();

		// if we couldn't match any files, fail
		if(fileList.size() == 0)
		{
			m_buildLogger.addErrorLogEntry("No files found for upload - aborting job.");
			return -1;
		}
		
		// walk the list of files and upload them
		for(Map.Entry<String, String> entry : fileList.entrySet())
		{
			m_buildLogger.addBuildLogEntry("Uploading " + entry.getKey());
			if(entry.getValue() != null)
				m_buildLogger.addBuildLogEntry("\tand renamed to " + entry.getValue());

			try
			{
				String filelistXML = m_uploadAPIWrapper.uploadFile(m_appID, 
																	entry.getKey(),
																	m_sandboxID,
																	entry.getValue());
				
				log.debug("Upload File XML: " + filelistXML);
				
			}
			catch(IOException e)
			{
				m_buildLogger.addErrorLogEntry("Unable to upload file " + entry.getKey() + " Exception: " + e.toString());
				return -1;
			}
		}
		return 0;
	}
	
	
	private int runScan()
	{
		log.info("Running scan");
		
		
		/* handle the top-level module list
		 * 
		 * if no top-level modules spec'd, run pre-scan with auto-scan = true
		 * 		- accept the default top-level module list
		 * 
		 * if the user has spec'd top-level modules, run pre-scan, then:
		 * 		- get the module list
		 * 		- match user selection to get module ID's
		 */
		
		if(m_taskContext.getConfigurationMap().get("scanInclude").isEmpty() 
    			&& m_taskContext.getConfigurationMap().get("scanExclude").isEmpty())
		{
			m_buildLogger.addBuildLogEntry("No top-level modules specified, using defaults");
			
			try
			{
				String buildInfoXML = m_uploadAPIWrapper.beginPreScan(m_appID, m_sandboxID, "true");
				
				log.debug("Prescan XML: " + buildInfoXML);
			}
			catch(IOException e)
			{
				m_buildLogger.addErrorLogEntry("Unable to begin pre-scan for app: " + m_appName +
						" Exception: " + e.toString());
				return -1;
			}
			
			m_buildLogger.addBuildLogEntry("Pre-scan started, scan will automatically start after pre-scan");
			return 0;
		}
		
		/* 
		 * if we got here, we need to handle module selection
		 * 
		 * Wait for the pre-scan to finish (with status = 'Pre-Scan Success'),
		 * then pull the file list and check for matches.
		 */
		
		// run pre-scan - needed to get the module list
		m_buildLogger.addBuildLogEntry("Top-level modules specified for inclusion/exclusion - need to wait for pre-scan to finish.");
		
		try
		{
			String buildInfoXML = m_uploadAPIWrapper.beginPreScan(m_appID, m_sandboxID, "false");
			
			log.debug("Prescan XML: " + buildInfoXML);
		}
		catch(IOException e)
		{
			m_buildLogger.addErrorLogEntry("Unable to begin pre-scan for app: " + m_appName +
					" Exception: " + e.toString());
			return -1;
		}
		
		// now, wait for pre-scan to finish to get the module list
		String buildInfoXML;
		String buildStatus;
		
		while(true)
		{	
			try
			{
				buildInfoXML = m_uploadAPIWrapper.getBuildInfo(m_appID, m_buildID, m_sandboxID);	
				
				// get the status of the build
				ParseAppDataXML parser = new ParseAppDataXML(buildInfoXML);
				
				try
				{
					buildStatus = parser.getBuildStatusFromInfo(m_scanName);
					
					m_buildLogger.addBuildLogEntry("Scan status = " + buildStatus);
					
					// if pre-scan finished successfully, we're good to go
					if(buildStatus.equalsIgnoreCase("Pre-Scan Success") )
						break;
					
					// pre-scan still thinking, sleep and check again
					if(buildStatus.equalsIgnoreCase("Pre-Scan Submitted") )
					{
						m_buildLogger.addBuildLogEntry("Pre-scan not done, waiting " 
								+ String.valueOf(m_sleepSecs/60) + " minutes and will check again.");
								
						try
						{
							Thread.sleep(m_sleepSecs * 1000);
						}
						catch (InterruptedException ie)
						{
							// TODO: valid??
							m_buildLogger.addBuildLogEntry("Sleep interrupted, trying again " +
									" Exception: " + ie.toString());	
						}
					}
					else
					{
						m_buildLogger.addErrorLogEntry("Pre-scan failure, aborting job.  Pre-scan state is: " + buildStatus);
						return -1;	
					}
				}
				catch (ParseException pp)
				{
					m_buildLogger.addErrorLogEntry("Unable to get Build status: " + m_scanName +
							" Exception: " + pp.toString());
					return -1;
				}
				catch (XMLStreamException x)
				{
					m_buildLogger.addErrorLogEntry("Unable to get Build status: " + m_scanName +
							" Exception: " + x.toString());
					return -1;	
				}
			}
			catch(IOException e)
			{
				m_buildLogger.addErrorLogEntry("Unable to get build info for app: " + m_appName + ", build: " + m_buildID +
						" Exception: " + e.toString());
				return -1;
			}
		}
		
		m_buildLogger.addBuildLogEntry("Pre-Scan finished successfully.  Setting top-level modules.");
		
		// get the pre-scan results, which will have the module list
		Map<String, String> moduleList;
		try
		{
			String prescanResultsXML = m_uploadAPIWrapper.getPreScanResults(m_appID, m_buildID, m_sandboxID);
			
			log.debug("prescan results: " + prescanResultsXML);
			
			ParseAppDataXML parser = new ParseAppDataXML(prescanResultsXML);
			
			try
			{
				moduleList = parser.getModuleListFromPrescan();
				
				m_buildLogger.addBuildLogEntry("Module List:");
				for(Map.Entry<String, String> entry : moduleList.entrySet())
					m_buildLogger.addBuildLogEntry("\t[" + entry.getKey() + ":" + entry.getValue() + "]");
				
				ArrayList<String> topLevelModules = m_fileUtil.getTopLevelModules(moduleList);	
				
				// got here - we have a top-level module list
				m_buildLogger.addBuildLogEntry("Top Level Module list: " + topLevelModules);
				
				// topLevelModules is an ArraryList of the moduleIDs - turn into a string
				String moduleIDs = new String("");
				
				for(String tmp : topLevelModules)
					moduleIDs += tmp + ",";

				// strip the trailing comma, if we added at least 1
				if(moduleIDs.length() > 0)
					moduleIDs = moduleIDs.substring(0, moduleIDs.length() - 1);
				
				log.debug("Top Level Module IDs: " + moduleIDs);
				
				try
				{
					String buildInfoXML3 = m_uploadAPIWrapper.beginScan(m_appID, moduleIDs, "false", m_sandboxID);
					
					log.debug("Scan XML: " + buildInfoXML3);
				}
				catch(IOException e)
				{
					m_buildLogger.addErrorLogEntry("Unable to begin scan for app: " + m_appName +
							" Exception: " + e.toString());
					return -1;
				}
				
				m_buildLogger.addBuildLogEntry("Scan started");
				return 0;
			}
			catch (ParseException pp)
			{
				m_buildLogger.addErrorLogEntry("Unable to get Build status: " + m_scanName +
						" Exception: " + pp.toString());
				return -1;
			}
			catch (XMLStreamException x)
			{
				m_buildLogger.addErrorLogEntry("Unable to get Build status: " + m_scanName +
						" Exception: " + x.toString());
				return -1;	
			}
		}
		catch(IOException e)
		{
			m_buildLogger.addErrorLogEntry("Unable to get prescan results for app: " + m_appName + ", build: " + m_buildID +
					" Exception: " + e.toString());
			return -1;
		}	
	}

	private int waitForScan()
	{
		log.info("Waiting for Scan to complete");
		
		final String waitTimeStr = m_taskContext.getConfigurationMap().get("waitTime");
		
		// sanity checking
		int waitTime;
		try
		{
			waitTime = Integer.parseInt(waitTimeStr);
			
			if(waitTime <= 1)
			{
				m_buildLogger.addErrorLogEntry("Illegal value in 'Wait for Scan' time");
				return -1;
			}
		}
		catch (NumberFormatException e)
		{
			m_buildLogger.addErrorLogEntry("Illegal value in 'Wait for Scan' time");
			return -1;
		}

		// and now we wait...
		m_buildLogger.addBuildLogEntry("Waiting for Scan to Complete (timeout = " + Integer.toString(waitTime) + " minutes)");
		m_buildLogger.addBuildLogEntry("\tWill poll every 2 minutes");
		
		int sleepTime = 2 * 60 * 1000;	// 2 minutes
		int timeCounter = 0;
		String buildInfoXML;
		String buildStatus;
		
		while(true)
		{
			// get build info (status)
			try
			{
				buildInfoXML = m_uploadAPIWrapper.getBuildInfo(m_appID, m_buildID, m_sandboxID);	
				
				// get the status of the build
				ParseAppDataXML parser = new ParseAppDataXML(buildInfoXML);
				
				try
				{
					buildStatus = parser.getBuildStatusFromInfo(m_scanName);
					
					m_buildLogger.addBuildLogEntry("Scan status = " + buildStatus);
				}
				catch (ParseException pp)
				{
					m_buildLogger.addErrorLogEntry("Unable to get Build status: " + m_scanName +
							" Exception: " + pp.toString());
					return -1;
				}
				catch (XMLStreamException x)
				{
					m_buildLogger.addErrorLogEntry("Unable to get Build status: " + m_scanName +
							" Exception: " + x.toString());
					return -1;	
				}
			}
			catch(IOException e)
			{
				m_buildLogger.addErrorLogEntry("Unable to get build info for app: " + m_appName + ", build: " + m_buildID +
						" Exception: " + e.toString());
				return -1;
			}
			
			// handle the status
			if(buildStatus.equalsIgnoreCase("Results Ready"))
				break;
			
			// not ready yet, check for timeout
			if(timeCounter >= waitTime)
			{
				m_buildLogger.addErrorLogEntry("Scan timed out - aborting");
				return -1;	
			}
			
			//sleep and try again
			try
			{
				Thread.sleep(sleepTime);
			}
			catch (InterruptedException ie)
			{
				// TODO: valid??
				m_buildLogger.addBuildLogEntry("Sleep interrupted, trying again " +
						" Exception: " + ie.toString());	
			}
			
			timeCounter += 2;
		}
		
		// check the results - pass/fail - using the results wrapper summary report
		
		ResultsAPIWrapper resultsAPIWrapper = new ResultsAPIWrapper();
		
		// setup creds for Results API
		if(!m_credentialsHelper.setUpCredentials(resultsAPIWrapper))
			return -1;
		
		String passFail;
		try
		{
			String summaryReportXML = resultsAPIWrapper.summaryReport(m_buildID);	
			
			log.debug("Summary Report XML: " + summaryReportXML);
			
			// get the pass/fail status
			ParseAppDataXML parser = new ParseAppDataXML(summaryReportXML);
			
			try
			{
				passFail = parser.getPassFailFromSummaryReport(m_scanName);
				
				m_buildLogger.addBuildLogEntry("pass/fail status = " + passFail);
				
				if(passFail.equalsIgnoreCase("Pass") || passFail.equalsIgnoreCase("Conditional Pass"))
					return 0;
				else
					return -1;
			}
			catch (ParseException pp)
			{
				m_buildLogger.addErrorLogEntry("Unable to get pass/fail status: " + m_scanName +
						" Exception: " + pp.toString());
				return -1;
			}
			catch (XMLStreamException x)
			{
				m_buildLogger.addErrorLogEntry("Unable to get pass/fail status: " + m_scanName +
						" Exception: " + x.toString());
				return -1;	
			}
		}
		catch(IOException e)
		{
			m_buildLogger.addErrorLogEntry("Unable to get pass/fail for app: " + m_appName + ", build: " + m_buildID +
					" Exception: " + e.toString());
			return -1;
		}
	}
}
