package com.veracode.bamboo.uploadscan;

import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.spring.ComponentAccessor;
import com.veracode.parser.util.*;	// for the Veracode StringUtility

import org.apache.log4j.Logger;

public class SecurityUtils {

	private EncryptionService encryptionService = ComponentAccessor.ENCRYPTION_SERVICE.get();
	
	private static final Logger log = Logger.getLogger(SecurityUtils.class);
	
	public SecurityUtils()
	{
		;
	}
	
	public String encrypt(final String plaintext)
	{
		log.debug("encrypting");
		//log.debug("    [" + plaintext + "]");			// for debugging ONLY
		if(StringUtility.isNullOrEmpty(plaintext))
			return "";
		else
			return encryptionService.encrypt(plaintext);
	}
	
	public String decrypt(final String crypttext)
	{
		log.debug("decrypting");
		if(StringUtility.isNullOrEmpty(crypttext))
			return "";
		else
		{
			return encryptionService.decrypt(crypttext);
			
			// for debugging ONLY
			/*
			String tmp;
		
			tmp = encryptionService.decrypt(crypttext);
			log.debug("   [" + tmp + "]");
			return tmp;
			*/
		}
	}
}
