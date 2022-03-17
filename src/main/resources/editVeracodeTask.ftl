<div class="veracode-plugin">
	[#-- this is a comment --]
	
	[@ui.bambooSection titleKey="uploadscan.appInfo" cssClass="titled-group"]
		[@ww.textfield 
			labelKey='uploadscan.appName' 
			name='appName' 
			cssClass='long-field' 
			required='true' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.appName.help' /]
			
		[@ww.checkbox 
			labelKey='uploadscan.createApp' 
			name='createApp'
			toggle='true' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.createApp.help' /]

		[@ui.bambooSection dependsOn='createApp' showOn='true']
			[@ww.select 
				labelKey='uploadscan.busCrit' 
				name='busCrit' 
				list=critLevels 
				listKey='first'
				listValue='second'
				helpIconCssClass='aui-iconfont-info'
				helpDialogKey='uploadscan.busCrit.help' /]
		[/@ui.bambooSection]
		
		[@ww.textfield 
			labelKey='uploadscan.sandboxName' 
			name='sandboxName' 
			cssClass='long-field' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.sandboxName.help' /]
			
		[@ww.checkbox 
			labelKey='uploadscan.createSandbox'
			name='createSandbox'
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.createSandbox.help' /]
	[/@ui.bambooSection]
	
	[@ui.bambooSection titleKey="uploadscan.scanInfo" cssClass="titled-group"]
		[@ww.textfield 
			labelKey='uploadscan.scanName'
			name='scanName'
			cssClass='long-field' 
			required='true' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.scanName.help' /]
		
		[@ww.textfield 
			labelKey='uploadscan.uploadInclude'
			name='uploadInclude'
			cssClass='long-field' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.uploadInclude.help' /]

		[@ww.textfield 
			labelKey='uploadscan.uploadExclude'
			name='uploadExclude' 
			cssClass='long-field' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.uploadExclude.help' /]
		
		[@ww.textfield 
			labelKey='uploadscan.scanInclude'
			name='scanInclude'
			cssClass='long-field' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.scanInclude.help' /]

		[@ww.textfield 
			labelKey='uploadscan.scanExclude'
			name='scanExclude' 
			cssClass='long-field' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.scanExclude.help' /]
			
		[@ww.textfield 
			labelKey='uploadscan.saveasFilename'
			name='saveasFilename'
			cssClass='long-field' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.saveasFilename.help' /]

		[@ww.textfield 
			labelKey='uploadscan.saveasReplacement'
			name='saveasReplacement' 
			cssClass='long-field' 
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.saveasReplacement.help' /]
		
		[@ww.hidden name='hideWait' value='false' /]
		[@ui.bambooSection dependsOn='hideWait' showOn='false']	
			[@ww.checkbox 
				labelKey='uploadscan.waitComplete' 
				name='waitComplete' 
				toggle='true'
				helpIconCssClass='aui-iconfont-info'
				helpDialogKey='uploadscan.waitComplete.help' /]		
				
			[@ui.bambooSection dependsOn='waitComplete' showOn='true']
	    		[@ww.textfield 
					labelKey='uploadscan.waitTime'
					name='waitTime' 
					helpIconCssClass='aui-iconfont-info' /]	
			[/@ui.bambooSection]
		[/@ui.bambooSection]
	[/@ui.bambooSection]
	
	[@ui.bambooSection titleKey="uploadscan.credentials" cssClass="titled-group"]		
		[@ww.select 
			labelKey='uploadscan.credType' 
			name='credType'
			toggle='true'
			list=credTypes 
			listKey='first'
			listValue='second'
			helpIconCssClass='aui-iconfont-info'
			helpDialogKey='uploadscan.credType.help' /]	
			
		[@ui.bambooSection dependsOn='credType' showOn='UP']
    		[@ww.textfield 
				labelKey='uploadscan.credType.username'
				name='credUsername'
				cssClass='long-field' 
				required='true' /]
				
			[@ww.password 
				labelKey='uploadscan.credType.password'
				name='credPassword'
				showPassword='true'
				cssClass='long-field' 
				required='true' /]
		[/@ui.bambooSection]
		[@ui.bambooSection dependsOn='credType' showOn='API']
    		[@ww.textfield 
				labelKey='uploadscan.credType.ApiID'
				name='credApiID'
				cssClass='long-field' 
				required='true' /]
				
			[@ww.password 
				labelKey='uploadscan.credType.ApiKey'
				name='credApiKey'
				showPassword='true'
				cssClass='long-field' 
				required='true' /]
		[/@ui.bambooSection]
	[/@ui.bambooSection]
</div>