package com.veracode.bamboo.uploadscan;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.CustomVariableContextImpl;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.bamboo.variable.substitutor.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.veracode.parser.util.*;	// for the Veracode StringUtility

import org.apache.log4j.Logger;


public class FileUtils {
	
	private TaskContext m_taskContext;
	private CustomVariableContext m_variableContext;
	private BuildLogger m_buildLogger;
	private Map<String, String> m_fileList = new HashMap<String, String>();	// Map of uploaded-name, renamed-name
	
	private static final Logger log = Logger.getLogger(FileUtils.class);
	
	public FileUtils(final TaskContext taskContext)
	{
		m_taskContext = taskContext;
		m_buildLogger = m_taskContext.getBuildLogger();
		
		VariableSubstitutorFactory vsf =  new VariableSubstitutorFactoryImpl();
		m_variableContext = new CustomVariableContextImpl(vsf);
	}
	
	
	// get the list of files to upload
	public Map<String, String> getUploadList()
	{
		log.info("getting file upload list");
		
		/* 
		 * walk the working dir and match all files against the list, add to the Map if found
		 *		we'll add renaming data later, if required
		 */
		
		// start with the Bamboo working dir	
		String workingDir = getVariable("build.working.directory");
		log.info("Bamboo build.working.directory = " + workingDir);
		
		// Files to include: parse a comma-separated list of files/wildcards
		String include = m_taskContext.getConfigurationMap().get("uploadInclude");
		String[] includePatterns;		// list of file patterns to match, might not have wildcards
		if(include.isEmpty())
		{
			includePatterns = null;
			m_buildLogger.addBuildLogEntry("No files/patterns specified for inclusion, so use all files in the root directory");
		}
		else
		{
			// trim() to remove leading and trailing whitespace
			// \s*,\s* includes any whitespace in the split string (e.g.: removes whitespace after a comma)
			includePatterns = include.trim().split("\\s*,\\s*");
			m_buildLogger.addBuildLogEntry("File (patterns) to upload: " + Arrays.toString(includePatterns) );
		}
		
		// Files to exclude: parse a comma-separated list of files/wildcards
		String exclude = m_taskContext.getConfigurationMap().get("uploadExclude");
		String[] excludePatterns;		// list of file patterns to match, might not have wildcards
		if(exclude.isEmpty())
		{
			excludePatterns = null;
			m_buildLogger.addBuildLogEntry("No files/patterns specified for exclusion");
		}
		else
		{
			// trim() to remove leading and trailing whitespace
			// \s*,\s* includes any whitespace in the split string (e.g.: removes whitespace after a comma)
			excludePatterns = exclude.trim().split("\\s*,\\s*");
			m_buildLogger.addBuildLogEntry("File (patterns) to exclude: " + Arrays.toString(excludePatterns) );
		}
				
		// all files in the root dir are uploaded if none are specified
		File root = new File(workingDir);
		if(includePatterns == null)
		{
			for(File f : root.listFiles())
			{
				if(f.isFile())
					if( !fileMatchesPatterns(f, excludePatterns))
						m_fileList.put(f.getAbsolutePath(), null);
			}
		}
		else
		{
			// recursive walk, find and check every file
			getRecursiveFiles(root, includePatterns, excludePatterns);
		}
		
		handleFileRenaming();
		
		// print out the list of files
		m_buildLogger.addBuildLogEntry("Files found matching criteria [file:renamed file]:");
		for(Map.Entry<String, String> entry : m_fileList.entrySet())
			m_buildLogger.addBuildLogEntry("\t" + entry.getKey() +":" + entry.getValue());
		
		return m_fileList;
	}
	
	
	private String getVariable(String var)
	{
		Map<String, VariableDefinitionContext> vc = m_variableContext.getVariableContexts();
		log.debug("custom variables: map size = " + vc.size());
		for(Map.Entry<String, VariableDefinitionContext> m:vc.entrySet())
			log.debug("custom var map: " + m.getKey() + " = [" + m.getValue() + "]");
		
		/*
		2017-09-25 14:56:54,597 DEBUG [16-BAM::Default Agent::Agent:pool-31-thread-1] 
				[FileUtils] custom var map: build.working.directory = 
				[VariableDefinitionContextImpl{variableType=CUSTOM, key=build.working.directory, 
				value=/Users/krise/my-repositories/bamboo-veracode/target/bamboo/home/xml-data/build-dir/PLAN-TEST-JOB1}]
		*/
		
		VariableDefinitionContext vdc = vc.get(var);
		if(vdc != null)
			return vdc.getValue();
		else
			return null;
	}
	
	/* 
	 * walk a directory tree and get all the files in it
	 */
	private void getRecursiveFiles(File node, String[] includePatterns, String[] excludePatterns)
	{
		for(File f : node.listFiles())
		{
			if(f.isDirectory())
			{
				// keep digging
				getRecursiveFiles(f, includePatterns, excludePatterns);
			}
			else
			{	
				// add to list
				log.debug("checking file: " + f.getAbsolutePath());
				
				if(fileMatchesPatterns(f, includePatterns)  && !fileMatchesPatterns(f, excludePatterns))
					m_fileList.put(f.getAbsolutePath(), null);
			}
		}
	}
	
	/*
	 * match a file's absolute path against an array of patterns
	 */
	private boolean fileMatchesPatterns(File f, String[] matchPatterns)
	{
		// handle special case to make this more generic
		if(matchPatterns == null)
			return false;
		
		Path path = Paths.get(f.getAbsolutePath());
		FileSystem fs = FileSystems.getDefault();
		
		// try each entry in the array of matches
		for(String match : matchPatterns)
		{
			// stupid windows - need to escape the windows backslash
			if(System.getProperty("os.name").startsWith("Windows"))
			{
				match = match.replace("\\", "\\\\");
			}

			String mm = "glob:" + match;
			
			log.debug("checking against match: " + mm);
			PathMatcher matcher = fs.getPathMatcher(mm);
			if(matcher.matches(path))
			{
				log.debug("matched:  path: " + path.toString() + "  match: " + mm);
				return true;
			}
		}

		return false;
	}
	

	/*
	 * handle file renaming - stolen directly from the Jenkins plugin
	 * 	com.veracode.apiwrapper.cli.VeracodeCommand.class
	 * 	support for only 1 filename pattern and 1 replacement pattern (that's just how it works)
	 */
	private void handleFileRenaming()
	{
		// File match rule
		String origPattern = m_taskContext.getConfigurationMap().get("saveasFilename");
		if(origPattern.isEmpty())
		{
			m_buildLogger.addBuildLogEntry("No 'Save As Filename' pattern - skipping renaming");
			return;
		}
		
		// Renaming rule
		String renamedPattern = m_taskContext.getConfigurationMap().get("saveasReplacement");
		if(renamedPattern.isEmpty())
		{
			m_buildLogger.addBuildLogEntry("No 'Save As Replacement' pattern - skipping renaming");
			return;
		}
		
		/* 
		 * walk down the file list, and check for renaming matches
		 */
		// TODO: cleanup, refactor
		for(Map.Entry<String, String> entry : m_fileList.entrySet())
		{
			String tmp;
		
			File f = new File(entry.getKey());
			tmp = replaceFileName(f.getName(), origPattern, renamedPattern);
			
			log.debug("renamed = " + tmp);
			
			if(!tmp.equals(f.getName()) )
				entry.setValue(tmp);
		}
	}
	
	private String replaceFileName(String fileName, String pattern, String replacement)
    {
		log.debug("replaceFileName: " + fileName + ", " + pattern + ", " + replacement);
		
		if ((!StringUtility.isNullOrEmpty(fileName)) && (!StringUtility.isNullOrEmpty(pattern)) 
				&& (!StringUtility.isNullOrEmpty(replacement))) {
			try
			{
				Matcher matcher = Pattern.compile(escapeFileNamePattern(pattern)).matcher(fileName);
				if (matcher.matches()) 
				{
					log.debug("match found");
					return matcher.replaceAll(escapeReplacementPattern(replacement, matcher.groupCount()));
				}
				
			}
			catch (Throwable localThrowable) {}
		}
		
		return fileName;
    }
    
    private String escapeFileNamePattern(String pattern)
    {
    	return "^" + Pattern.quote(pattern).replace("?", "\\E(.)\\Q").replace("*", "\\E(.*?)\\Q") + "$";
    }
    
    private String escapeReplacementPattern(String replacement, int groupCount)
    {    	
    	String[] tokens = replacement.split("\\\\", -1);
    	
    	for (int x = 0; x < tokens.length; x++) 
    		tokens[x] = escapeReplacementToken(tokens[x], groupCount);
      
    	return StringUtility.join("\\\\", tokens);
    }
    
    private String escapeReplacementToken(String replacementToken, int groupCount)
    {
    	Matcher m = Pattern.compile("(?<!\\\\)\\$((\\{|\\$|\\d+|.)(([^\\$\\{\\}]+)\\}([^\\$])?)?)?").matcher(replacementToken);
      
    	StringBuffer sb = new StringBuffer();
    	
    	while (m.find())
    	{
    		String firstCapture = m.group(1);
    		if (!StringUtility.isNullOrEmpty(firstCapture))
    		{
    			String secondCapture = m.group(2);
    			String thirdCapture = m.group(3);
    			
    			if ("{".equals(secondCapture))
    			{
    				if (!StringUtility.isNullOrEmpty(thirdCapture))
    				{
    					String indexString = m.group(4);
    					String nextCharString = m.group(5);
              
    					boolean validIndex = false;
    					try
    					{
    						int index = Integer.parseInt(indexString);
    						validIndex = (index <= groupCount) && (indexString.charAt(0) != '-');
    					}
    					catch (NumberFormatException localNumberFormatException) {}
    					
    					if (validIndex)
    					{
    						if ((!StringUtility.isNullOrEmpty(nextCharString)) && (Character.isDigit(nextCharString.charAt(0)))) {
    							m.appendReplacement(sb, Matcher.quoteReplacement("$" + indexString + "\\" + nextCharString));
    						} else {
    							m.appendReplacement(sb, Matcher.quoteReplacement("$" + indexString + StringUtility.getEmptyIfNull(nextCharString)));
    						}
    					}
    					else {
    						m.appendReplacement(sb, Matcher.quoteReplacement("\\${" + indexString + "}" + StringUtility.getEmptyIfNull(nextCharString)));
    					}
    				}
    				else
    				{
    					m.appendReplacement(sb, Matcher.quoteReplacement("\\${"));
    				}
    			}
    			else if ("$".equals(secondCapture))
    			{
    				m.appendReplacement(sb, Matcher.quoteReplacement("\\$" + StringUtility.getEmptyIfNull(thirdCapture)));
    			}
    			else
    			{
    				boolean validIndex = false;
    				try
    				{
    					int index = Integer.parseInt(secondCapture);
    					validIndex = (index <= groupCount) && (secondCapture.charAt(0) != '-');
    				}
    				catch (NumberFormatException localNumberFormatException1) {}
    				
    				if (validIndex) {
    					m.appendReplacement(sb, Matcher.quoteReplacement("$" + StringUtility.getEmptyIfNull(secondCapture) + StringUtility.getEmptyIfNull(thirdCapture)));
    				} else {
    					m.appendReplacement(sb, Matcher.quoteReplacement("\\$" + StringUtility.getEmptyIfNull(secondCapture) + StringUtility.getEmptyIfNull(thirdCapture)));
    				}
    			}
    		}
    		else
    		{
    			m.appendReplacement(sb, Matcher.quoteReplacement("\\$"));
    		}
    	}
    	m.appendTail(sb);
      
    	return sb.toString();
    }
    
    public ArrayList<String> getTopLevelModules(Map<String, String>moduleList)
    {  
    	ArrayList<String> topLevelList = new ArrayList<String>();
    	
    	// Modules to include: parse a comma-separated list of files/wildcards
		String include = m_taskContext.getConfigurationMap().get("scanInclude");
		String[] includePatterns;		// list of file patterns to match, might not have wildcards
		if(include.isEmpty())
		{
			includePatterns = null;
			m_buildLogger.addBuildLogEntry("Module selection: No top-level modules specified - using defaults");
		}
		else
		{
			// trim() to remove leading and trailing whitespace
			// \s*,\s* includes any whitespace in the split string (e.g.: removes whitespace after a comma)
			includePatterns = include.trim().split("\\s*,\\s*");
			m_buildLogger.addBuildLogEntry("Modules (patterns) for top-level modules: " + Arrays.toString(includePatterns) );
		}
		
		// Modules to exclude: parse a comma-separated list of files/wildcards
		String exclude = m_taskContext.getConfigurationMap().get("scanExclude");
		String[] excludePatterns;		// list of file patterns to match, might not have wildcards
		if(exclude.isEmpty())
		{
			excludePatterns = null;
			m_buildLogger.addBuildLogEntry("Module selection: No files/patterns specified for exclusion - using defaults");
		}
		else
		{
			// trim() to remove leading and trailing whitespace
			// \s*,\s* includes any whitespace in the split string (e.g.: removes whitespace after a comma)
			excludePatterns = exclude.trim().split("\\s*,\\s*");
			m_buildLogger.addBuildLogEntry("Modules (patterns) to exclude from top-level: " + Arrays.toString(excludePatterns) );
		}

		if(include.isEmpty() && exclude.isEmpty())
		{
			m_buildLogger.addBuildLogEntry("Module selection: No files/patterns specified - using defaults");
			return topLevelList;
		}
		
		/* 
		 * walk down the module list, and check for includes and excludes
		 */
		
		for(Map.Entry<String, String> entry : moduleList.entrySet())
		{
			if(isTopLevel(entry.getValue(), includePatterns, excludePatterns))
			{
				topLevelList.add(entry.getKey());		
			}
		}
    	
    	return topLevelList;	
    }
    
    private boolean isTopLevel(String name, String[] includePatterns, String[] excludePatterns)
    {
    	log.debug("isTopLevel: " + name + ", " + includePatterns + ", " + excludePatterns);
		
    	boolean maybe = false;
    	
		if (!StringUtility.isNullOrEmpty(name)) 
		{
			// check the include patterns
			if(includePatterns != null)
			{
				for(String pattern: includePatterns)
				{
					try
					{
						Matcher matcher = Pattern.compile(escapeFileNamePattern(pattern)).matcher(name);
						if (matcher.matches()) 
						{
							log.debug("include match found");
							maybe = true;
							break;
						}
					}
					catch (Throwable localThrowable) {}
				}
			}
			
			// check the exclude patterns
			if(excludePatterns != null)
			{
				for(String pattern: excludePatterns)
				{
					try
					{
						Matcher matcher = Pattern.compile(escapeFileNamePattern(pattern)).matcher(name);
						if (matcher.matches()) 
						{
							log.debug("exclude match found");
							maybe = false;
							break;
						}
					}
					catch (Throwable localThrowable) {}	
				}
			}
		}
		
    	return maybe;
    }
}

