package org.apache.airavata.gfac.handler;

import java.util.Map;

import org.apache.airavata.gfac.GFacException;
import org.apache.airavata.gfac.context.JobExecutionContext;
import org.apache.airavata.gfac.context.security.UNICORESecurityContext;
import org.apache.airavata.gfac.provider.impl.BESConstants;
import org.apache.airavata.gfac.provider.utils.DataTransferrer;
import org.apache.airavata.gfac.provider.utils.StorageCreator;
import org.apache.airavata.schemas.gfac.JobDirectoryModeDocument.JobDirectoryMode;
import org.apache.airavata.schemas.gfac.UnicoreHostType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.StorageClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

public abstract class AbstractSMSHandler implements BESConstants, GFacHandler{
    
	// TODO: later use AbstractHandler, which cannot be used due to error in RegistryFactory
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
    protected DefaultClientConfiguration secProperties;
	
	protected StorageClient storageClient; 
	
	protected DataTransferrer dataTransferrer;
	
	@Override
	public void initProperties(Map<String, String> properties)
			throws GFacHandlerException, GFacException {
		
	}

	@Override
	public void invoke(JobExecutionContext jobExecutionContext)
			throws GFacHandlerException, GFacException {
		
		// if not SMS then not to pass further
		if(!isSMSEnabled(jobExecutionContext)) return;
		
		initSecurityProperties(jobExecutionContext);
		

		
		UnicoreHostType host = (UnicoreHostType) jobExecutionContext.getApplicationContext().getHostDescription()
                .getType();
        String factoryUrl = host.getUnicoreBESEndPointArray()[0];
        
        storageClient = null;
        
        if(!isSMSInstanceExisting(jobExecutionContext)) {
            EndpointReferenceType eprt = EndpointReferenceType.Factory.newInstance();
            eprt.addNewAddress().setStringValue(factoryUrl);
            StorageCreator storageCreator = new StorageCreator(secProperties, factoryUrl, 5, null);
            try {
                storageClient = storageCreator.createStorage();
            } catch (Exception e2) {
                log.error("Cannot create storage..");
                throw new GFacException("Cannot create storage..", e2);
            }
            jobExecutionContext.setProperty(PROP_SMS_EPR, storageClient.getEPR());
        }
        else {
        	EndpointReferenceType eprt = (EndpointReferenceType)jobExecutionContext.getProperty(PROP_SMS_EPR);
        		try {
					storageClient = new StorageClient(eprt, secProperties);
				} catch (Exception e) {
					throw new GFacException("Cannot create storage..", e);
				}
        }
        dataTransferrer = new DataTransferrer(jobExecutionContext, storageClient);
	}
	
	protected void initSecurityProperties(JobExecutionContext jobExecutionContext) throws GFacException{
		log.debug("Initializing SMSInHandler security properties ..");
        if (secProperties != null) {
            secProperties = secProperties.clone();
            return;
        }
        UNICORESecurityContext unicoreContext = (UNICORESecurityContext) jobExecutionContext.getSecurityContext(UNICORESecurityContext.UNICORE_SECURITY_CONTEXT);
        if(log.isDebugEnabled()) {
        	log.debug("Generating client's default security configuration..");
        }
        //TODO: check what kind of credential (server signed or myproxy) should be used
        secProperties = unicoreContext.getDefaultConfiguration();
        if(log.isDebugEnabled()) {
        	log.debug("Security properties are initialized.");
        }
        jobExecutionContext.setProperty(PROP_CLIENT_CONF, secProperties);
	}
	
	protected boolean isSMSInstanceExisting(JobExecutionContext jec){
		boolean hasSMS = true;
        if((null == jec.getProperty(PROP_SMS_EPR))) {
        	hasSMS = false;
        }
        return hasSMS;
	}

	/**
	 * It checks whether the SMSByteIO protocol is used during the creation 
	 * of the job execution context.
	 * */
	protected boolean isSMSEnabled(JobExecutionContext jobExecutionContext){
		if(((UnicoreHostType)jobExecutionContext.getApplicationContext().getHostDescription().getType()).getJobDirectoryMode() == JobDirectoryMode.SMS_BYTE_IO) {
			return true;
		}
		return false;
	}
	

}
