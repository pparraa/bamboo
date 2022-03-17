package com.veracode.bamboo.uploadscan;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.Pair;
//import com.atlassian.sal.api.message.I18nResolver;

import com.google.common.collect.ImmutableList;
import com.atlassian.bamboo.utils.i18n.*;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.CustomVariableContextImpl;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.bamboo.variable.substitutor.VariableSubstitutorFactory;
import com.atlassian.bamboo.variable.substitutor.VariableSubstitutorFactoryImpl;

//import com.atlassian.bamboo.variable.VariableDefinitionContext;
//import com.atlassian.bamboo.variable.substitutor.*;



import com.atlassian.spring.container.LazyComponentReference;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;

public class VeracodeTaskConfigurator extends AbstractTaskConfigurator
{	
	private SecurityUtils m_securityUtils = new SecurityUtils();
	private CustomVariableContext m_variableContext;
	
	/*@ComponentImport*/ //private I18nResolver i18nResolver;
	
	// import the i18nBean so I can access the text in the i18n file
	// TODO: refactor all the i18n stuff out into a separate class?
    LazyComponentReference<I18nBeanFactory> i18nBeanFactoryReference =
            new LazyComponentReference<I18nBeanFactory>("i18nBeanFactory");
    I18nBeanFactory i18nBeanFactory = i18nBeanFactoryReference.get();
    I18nBean i18nBean = i18nBeanFactory.getI18nBean();
	
    private static final Logger log = Logger.getLogger(VeracodeTask.class);
    
    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        log.info("generateTaskConfigMap");
        
        // common data
        config.put("appName", params.getString("appName"));
        config.put("createApp", String.valueOf(params.getBoolean("createApp")));
        config.put("busCrit", params.getString("busCrit"));
        config.put("sandboxName", params.getString("sandboxName"));
        config.put("createSandbox", String.valueOf(params.getBoolean("createSandbox")));
        config.put("scanName", params.getString("scanName"));
        config.put("uploadInclude", params.getString("uploadInclude"));
        config.put("uploadExclude", params.getString("uploadExclude"));
        config.put("scanInclude", params.getString("scanInclude"));
        config.put("scanExclude", params.getString("scanExclude"));
        config.put("saveasFilename", params.getString("saveasFilename"));
        config.put("saveasReplacement", params.getString("saveasReplacement"));
        config.put("waitComplete", String.valueOf(params.getBoolean("waitComplete")));
        config.put("waitTime", params.getString("waitTime"));

        // creds
        config.put("credType", params.getString("credType"));
        config.put("credUsername", params.getString("credUsername"));
        config.put("credPassword", m_securityUtils.encrypt(params.getString("credPassword")));
        config.put("credApiID", params.getString("credApiID"));
        config.put("credApiKey", m_securityUtils.encrypt(params.getString("credApiKey")));
        
        return config;
    }

    @Override
    // populate default values on Task creation
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
    	log.info("populateContextForCreate");
    	
        super.populateContextForCreate(context);
    	
        checkToHideFields(context);
        
        // setup pull-down lists
        context.put("critLevels", getVeracodeCritLevels());
        context.put("busCrit", "VH");
        
        context.put("credTypes", getVeracodeCredTypes());
        context.put("credType", "UP");
    }

    @Override
    // get the exiting config and display it (edit mode)
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
    	log.info("populateContextForEdit");
    	
        super.populateContextForEdit(context, taskDefinition);
        
        checkToHideFields(context);
        context.putAll(taskDefinition.getConfiguration());
        
    	context.replace("credPassword", m_securityUtils.decrypt((String) context.get("credPassword")));
    	context.replace("credApiKey", m_securityUtils.decrypt((String) context.get("credApiKey")));
    	
        // populate pull-down list
        context.put("critLevels", getVeracodeCritLevels());
        context.put("credTypes", getVeracodeCredTypes());
    }
    
    @Override
    // validate user input
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
    	log.info("Validate Params");
    	
        super.validate(params, errorCollection);
        
        // check the things that need to get set
        final String appName = params.getString("appName");
        if(appName.isEmpty())
            errorCollection.addError("appName", i18nBean.getText("uploadscan.appName.error"));
        
        final String scanName = params.getString("scanName");
        if(scanName.isEmpty())
            errorCollection.addError("scanName", i18nBean.getText("uploadscan.scanName.error"));
        
        final String waitComplete = params.getString("waitComplete");
        if(waitComplete != null)		// could be null first time through
        {
        	if(waitComplete.equalsIgnoreCase("true"))
        	{
        		final String waitTime = params.getString("waitTime");
        		if(waitTime.isEmpty())
        			errorCollection.addError("waitTime", i18nBean.getText("uploadscan.waitTime.error"));
	        	else
	        	{
	        		try
	        		{
	        			int value = Integer.parseInt(waitTime);
	        			
	        			if(value <= 1)
	        				errorCollection.addError("waitTime", i18nBean.getText("uploadscan.waitTime.error"));
	        		}
	        		catch (NumberFormatException e)
	        		{
	        			errorCollection.addError("waitTime", i18nBean.getText("uploadscan.waitTime.error"));	
	        		}
	        	}
        	}
        }
        
        final String credType = params.getString("credType");
        if(credType.equals("UP"))
        {        	
        	final String uname = params.getString("credUsername");
        	if(uname.isEmpty())
        		errorCollection.addError("credUsername", i18nBean.getText("uploadscan.credTypeUP.error"));
        	
        	final String pw = params.getString("credPassword");
        	if(pw.isEmpty())
        		errorCollection.addError("credPassword", i18nBean.getText("uploadscan.credTypeUP.error"));
        }
        else
        {        	
        	final String id = params.getString("credApiID");
        	if(id.isEmpty())
        		errorCollection.addError("credType", i18nBean.getText("uploadscan.credTypeAPI.error"));	
        	
        	final String key = params.getString("credApiKey");
        	if(key.isEmpty())
        		errorCollection.addError("credType", i18nBean.getText("uploadscan.credTypeAPI.error"));	
        } 
    }
    
    // might make more sense to use <Int, String>, but everything is String-based in the UI
    private List<Pair<String, String>> getVeracodeCritLevels()
    {
    	return ImmutableList.of( 
    			Pair.make("VH", i18nBean.getText("uploadscan.busCrit.VH")),
    			Pair.make("H", i18nBean.getText("uploadscan.busCrit.H")),
    			Pair.make("M", i18nBean.getText("uploadscan.busCrit.M")),
    			Pair.make("L", i18nBean.getText("uploadscan.busCrit.L")),
    			Pair.make("VL", i18nBean.getText("uploadscan.busCrit.VL")) );
    }
    
    private List<Pair<String, String>> getVeracodeCredTypes()
    {
    	return ImmutableList.of( 
    			Pair.make("UP", i18nBean.getText("uploadscan.credType.UP")),
    			Pair.make("API", i18nBean.getText("uploadscan.credType.API")) );
    }
    
    private void checkToHideFields(@NotNull final Map<String, Object> context)
    {
    	// TODO: this is also used in the FileUtils - refactor to common code
    	
        VariableSubstitutorFactory vsf = new VariableSubstitutorFactoryImpl();
    	m_variableContext = new CustomVariableContextImpl(vsf);	
    	
    	Map<String, VariableDefinitionContext> vc = m_variableContext.getVariableContexts();
		log.debug("custom variables: map size = " + vc.size());
		for(Map.Entry<String, VariableDefinitionContext> m:vc.entrySet())
			log.debug("custom var map: " + m.getKey() + " = [" + m.getValue() + "]");
		
		/*
		2017-12-01 13:21:55,043 DEBUG [http-nio-6990-exec-10] 
			[VeracodeTask] custom var map: com.veracode.hideWait = 
			[VariableDefinitionContextImpl{variableType=GLOBAL, key=com.veracode.hideWait, value=false}]
		*/
		
		VariableDefinitionContext vdc = vc.get("com.veracode.hideWait");
		if((vdc != null) && vdc.getValue().equals("true"))
			context.put("hideWait", "true");
		else
			context.put("hideWait", "false");
    }
    
    
    /*
    //@Autowired
    public void setI18nResolver(I18nResolver i18nResolver)
    {
    	log.info("Veracode: setting i18nResolver");
    	
    	this.i18nResolver = i18nResolver;
    }
    */
}
