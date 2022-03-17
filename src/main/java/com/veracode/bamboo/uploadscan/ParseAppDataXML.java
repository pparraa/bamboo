package com.veracode.bamboo.uploadscan;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

// StAX XML parser
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
//import javax.xml.stream.events.Characters;
//import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import java.text.ParseException;
import org.apache.log4j.Logger;


public class ParseAppDataXML {
	
	// class variables
	XMLInputFactory m_factory;
	XMLEventReader m_eventReader;
	ByteArrayInputStream m_inputStream;
	
	private static final Logger log = Logger.getLogger(ParseAppDataXML.class);
	
	public ParseAppDataXML(final String xmldata)
	{
		// StAX is designed to handle reading from a stream, so a little fudging here
		byte[] byteArray = xmldata.getBytes(StandardCharsets.UTF_8);
		m_inputStream = new ByteArrayInputStream(byteArray);
		
		m_factory = XMLInputFactory.newInstance();	
	}
	
	// get the app ID from applist.xsd doc (from getAppList() )
	public String getAppIDFromList(final String appName)
	throws ParseException, XMLStreamException
	{
		log.info("getting AppID for " + appName);
		
		try
		{
			m_eventReader = m_factory.createXMLEventReader(m_inputStream);
						
			while(m_eventReader.hasNext())
			{
				XMLEvent event = m_eventReader.nextEvent();
				
				switch(event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					
					StartElement startElem = event.asStartElement();
					String eName = startElem.getName().getLocalPart();
					
					// find an 'app' element with the matching name
					if(eName.equalsIgnoreCase("app"))
					{
						Attribute attribName = startElem.getAttributeByName(new QName("app_name"));
						String name = attribName.getValue();
						
						Attribute attribID = startElem.getAttributeByName(new QName("app_id"));
						String id = attribID.getValue();
						
						log.debug("attribName = " + name + ",  attribID = " + id);
						
						if(name.equals(appName) )
							return id;
					}
					
					break;
					
				case XMLStreamConstants.CHARACTERS:
					break;
					
					
				case XMLStreamConstants.END_ELEMENT:
					break;
				}
			}
			
			log.error("No app found matching name " + appName);
			throw new ParseException("No app found matching name " + appName, 0);			
		}
		catch(XMLStreamException e)
		{
			log.error("Error reading from xml string " + e.toString());
			throw new XMLStreamException("Error reading from xml string " + e.toString());
		}
	}
	
	// get the app ID from appinfo.xsd doc (e.g., from createApp() )
	public String getAppIDFromInfo(final String appName)
	throws ParseException, XMLStreamException
	{
		log.info("getting AppID for " + appName);
		
		try
		{
			m_eventReader = m_factory.createXMLEventReader(m_inputStream);
						
			while(m_eventReader.hasNext())
			{
				XMLEvent event = m_eventReader.nextEvent();
				
				switch(event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					
					StartElement startElem = event.asStartElement();
					String eName = startElem.getName().getLocalPart();
					
					// find the 'application' element
					if(eName.equalsIgnoreCase("application"))
					{
						Attribute attribName = startElem.getAttributeByName(new QName("app_name"));
						String name = attribName.getValue();
						
						Attribute attribID = startElem.getAttributeByName(new QName("app_id"));
						String id = attribID.getValue();
						
						log.debug("attribName = " + name + ",  attribID = " + id);
						
						return id;
					}
					
					break;
					
				case XMLStreamConstants.CHARACTERS:
					break;
					
					
				case XMLStreamConstants.END_ELEMENT:
					break;
				}
			}
			
			log.error("No app found matching name " + appName);
			throw new ParseException("No app found matching name " + appName, 0);
		}
		catch(XMLStreamException e)
		{
			log.error("Error reading from xml string " + e.toString());
			throw new XMLStreamException("Error reading from xml string " + e.toString());
		}
	}
	
	
	// get the sandbox ID from sandboxlist.xsd doc (e.g., from getSandboxList() )
	public String getSandboxIDFromList(final String appID, final String sandboxName)
	throws ParseException, XMLStreamException
	{
		log.info("getting SandboxID for " + sandboxName);
		
		try
		{
			m_eventReader = m_factory.createXMLEventReader(m_inputStream);
						
			while(m_eventReader.hasNext())
			{
				XMLEvent event = m_eventReader.nextEvent();
				
				switch(event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					
					StartElement startElem = event.asStartElement();
					String eName = startElem.getName().getLocalPart();
					
					// find the 'sandbox' element
					if(eName.equalsIgnoreCase("sandbox"))
					{
						Attribute attribName = startElem.getAttributeByName(new QName("sandbox_name"));
						String name = attribName.getValue();
						
						Attribute attribID = startElem.getAttributeByName(new QName("sandbox_id"));
						String id = attribID.getValue();
						
						log.debug("attribName = " + name + ",  attribID = " + id);
												
						if(name.equals(sandboxName) )
							return id;						
					}
					
					break;
					
				case XMLStreamConstants.CHARACTERS:
					break;
					
					
				case XMLStreamConstants.END_ELEMENT:
					break;
				}
			}
			
			log.error("No sandbox found matching name " + sandboxName);
			throw new ParseException("No sandbox found matching name " + sandboxName, 0);
		}
		catch(XMLStreamException e)
		{
			log.error("Error reading from xml string " + e.toString());
			throw new XMLStreamException("Error reading from xml string " + e.toString());
		}
	}
	
	// get the sandbox ID from sandboxinfo.xsd doc (e.g., from createSandbox() )
	public String getSandboxIDFromInfo(final String sandboxName)
	throws ParseException, XMLStreamException
	{
		log.info("getting SandboxID for " + sandboxName);
		
		try
		{
			m_eventReader = m_factory.createXMLEventReader(m_inputStream);
						
			while(m_eventReader.hasNext())
			{
				XMLEvent event = m_eventReader.nextEvent();
				
				switch(event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					
					StartElement startElem = event.asStartElement();
					String eName = startElem.getName().getLocalPart();
					
					// find the 'sandbox' element
					if(eName.equalsIgnoreCase("sandbox"))
					{
						Attribute attribName = startElem.getAttributeByName(new QName("sandbox_name"));
						String name = attribName.getValue();
						
						Attribute attribID = startElem.getAttributeByName(new QName("sandbox_id"));
						String id = attribID.getValue();
						
						log.debug("attribName = " + name + ",  attribID = " + id);
						
						return id;
					}
					
					break;
					
				case XMLStreamConstants.CHARACTERS:
					break;
					
					
				case XMLStreamConstants.END_ELEMENT:
					break;
				}
			}
			
			log.error("No sandbox found matching name " + sandboxName);
			throw new ParseException("No sandbox found matching name " + sandboxName, 0);
		}
		catch(XMLStreamException e)
		{
			log.error("Error reading from xml string " + e.toString());
			throw new XMLStreamException("Error reading from xml string " + e.toString());
		}
	}

	
	// get the build ID from buildinfo.xsd doc (e.g., from createBuild() )
	public String getBuildIDFromInfo(final String buildName)
	throws ParseException, XMLStreamException
	{
		log.info("getting BuildID for " + buildName);
		
		try
		{
			m_eventReader = m_factory.createXMLEventReader(m_inputStream);
						
			while(m_eventReader.hasNext())
			{
				XMLEvent event = m_eventReader.nextEvent();
				
				switch(event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					
					StartElement startElem = event.asStartElement();
					String eName = startElem.getName().getLocalPart();
					
					// find the 'build' element
					if(eName.equalsIgnoreCase("build"))
					{
						Attribute attribID = startElem.getAttributeByName(new QName("build_id"));
						String id = attribID.getValue();
						
						log.debug("attribID = " + id);
						
						return id;
					}
					
					break;
					
				case XMLStreamConstants.CHARACTERS:
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					break;
				}
			}
			
			log.error("No build element in build with name " + buildName);
			throw new ParseException("No build element in build with name " + buildName, 0);
		}
		catch(XMLStreamException e)
		{
			log.error("Error reading from xml string " + e.toString());
			throw new XMLStreamException("Error reading from xml string " + e.toString());
		}
	}
	
	
	// get the build status from buildinfo.xsd doc (e.g., from getBuildStatus() )
	public String getBuildStatusFromInfo(final String buildName)
	throws ParseException, XMLStreamException
	{
		log.info("getting Build status for " + buildName);
		
		try
		{
			m_eventReader = m_factory.createXMLEventReader(m_inputStream);
						
			while(m_eventReader.hasNext())
			{
				XMLEvent event = m_eventReader.nextEvent();
				
				switch(event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					
					StartElement startElem = event.asStartElement();
					String eName = startElem.getName().getLocalPart();
					
					// find the 'analysis_unit' element
					if(eName.equalsIgnoreCase("analysis_unit"))
					{
						Attribute attribStatus = startElem.getAttributeByName(new QName("status"));
						String status = attribStatus.getValue();
						
						log.debug("attribStatus = " + status);
						
						return status;
					}
					
					break;
					
				case XMLStreamConstants.CHARACTERS:
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					break;
				}
			}
			
			log.error("No analysis_unit in build found with name " + buildName);
			throw new ParseException("No analysis_unit in build found with name " + buildName, 0);
		}
		catch(XMLStreamException e)
		{
			log.error("Error reading from xml string " + e.toString());
			throw new XMLStreamException("Error reading from xml string " + e.toString());
		}
	}

	// return a Map of <moduleID, moduleName> from prescanresults.xsd doc (e.g., from getPreScanResults() )
	public Map<String, String> getModuleListFromPrescan()
	throws ParseException, XMLStreamException
	{
		Map<String, String> moduleList = new HashMap<String, String>();	// Map of moduleID, moduleName;
		
		log.debug("Extracting module info from PreScan results");
		
		try
		{
			m_eventReader = m_factory.createXMLEventReader(m_inputStream);
						
			while(m_eventReader.hasNext())
			{
				XMLEvent event = m_eventReader.nextEvent();
				
				switch(event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					
					StartElement startElem = event.asStartElement();
					String eName = startElem.getName().getLocalPart();
					
					// find a 'module' element
					if(eName.equalsIgnoreCase("module"))
					{
						Attribute attribID = startElem.getAttributeByName(new QName("id"));
						String id = attribID.getValue();
						
						Attribute attribName = startElem.getAttributeByName(new QName("name"));
						String name = attribName.getValue();
						
						Attribute attribFatal = startElem.getAttributeByName(new QName("has_fatal_errors"));
						String fatal = attribFatal.getValue();
						
						log.debug("attribName = " + name + " id=" + id + " fatal errors=" + fatal);
						
						// if the module has a fatal error, we can't scan it
						if(fatal.equalsIgnoreCase("false"))
							moduleList.put(id, name);
					}
					
					break;
					
				case XMLStreamConstants.CHARACTERS:
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					break;
				}
			}
			
			// error check
			if(moduleList.size() == 0)
			{
				log.error("No modules found w/o fatal errors ");
				throw new ParseException("No modules found w/o fatal errors", 0);
			}
			
			for(Map.Entry<String, String> entry : moduleList.entrySet())
				log.debug("Module: ID=" + entry.getKey() + " Name=" + entry.getValue());
				
			return moduleList;
		}
		catch(XMLStreamException e)
		{
			log.error("Error reading from xml string " + e.toString());
			throw new XMLStreamException("Error reading from xml string " + e.toString());
		}
	}
	
	// get the pass/fail from the summaryreport.xsd doc (e.g., from getSummaryReport() )
	public String getPassFailFromSummaryReport(final String buildName)
	throws ParseException, XMLStreamException
	{
		log.info("getting pass/fail status for " + buildName);
		
		try
		{
			m_eventReader = m_factory.createXMLEventReader(m_inputStream);
						
			while(m_eventReader.hasNext())
			{
				XMLEvent event = m_eventReader.nextEvent();
				
				switch(event.getEventType())
				{
				case XMLStreamConstants.START_ELEMENT:
					
					StartElement startElem = event.asStartElement();
					String eName = startElem.getName().getLocalPart();
					
					// find the 'xxxxxx' element
					if(eName.equalsIgnoreCase("summaryreport"))
					{
						Attribute attribStatus = startElem.getAttributeByName(new QName("policy_compliance_status"));
						String status = attribStatus.getValue();
						
						log.debug("attribStatus = " + status);
						
						return status;
					}
					
					break;
					
				case XMLStreamConstants.CHARACTERS:
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					break;
				}
			}
		}
		catch(XMLStreamException e)
		{
			log.error("Error reading from xml string " + e.toString());
			throw new XMLStreamException("Error reading from xml string " + e.toString());
		}
		
		// really, we should never get here, but just in case...
		return "fail";
	}
}
