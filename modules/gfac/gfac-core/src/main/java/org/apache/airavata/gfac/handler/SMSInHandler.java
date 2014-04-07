package org.apache.airavata.gfac.handler;

import java.util.Map;

import org.apache.airavata.gfac.GFacException;
import org.apache.airavata.gfac.context.JobExecutionContext;
import org.apache.airavata.gfac.provider.GFacProviderException;

/**
 * Download upload job's input files to the temporary SMS directory.
 * 
 * */
public class SMSInHandler extends AbstractSMSHandler implements GFacHandler {

	@Override
	public void initProperties(Map<String, String> properties)
			throws GFacHandlerException, GFacException {
	}

	@Override
	public void invoke(JobExecutionContext jobExecutionContext)
			throws GFacHandlerException, GFacException {
		super.invoke(jobExecutionContext);
        try {
        	if(jobExecutionContext.getInMessageContext().getParameters().size() < 1) return;
        	dataTransferrer.uploadLocalFiles();
		} catch (GFacProviderException e) {
			throw new GFacHandlerException("Cannot upload local data",e);
		}
 	}
	
	
}
