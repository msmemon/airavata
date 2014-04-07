package org.apache.airavata.gfac.handler;

import java.util.Map;

import org.apache.airavata.gfac.GFacException;
import org.apache.airavata.gfac.context.JobExecutionContext;
import org.apache.airavata.gfac.provider.GFacProviderException;
import org.apache.airavata.gfac.provider.utils.ActivityInfo;
import org.ggf.schemas.bes.x2006.x08.besFactory.ActivityStateEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMSOutHandler extends AbstractSMSHandler implements GFacHandler{

	// TODO: later use AbstractHandler, which cannot be used due to error in RegistryFactory
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void initProperties(Map<String, String> properties)
			throws GFacHandlerException, GFacException {
	}

	@Override
	public void invoke(JobExecutionContext jobExecutionContext)
			throws GFacHandlerException, GFacException {
		super.invoke(jobExecutionContext);
		
		ActivityInfo activityInfo = (ActivityInfo)jobExecutionContext.getProperty(PROP_ACTIVITY_INFO);
		try {
		if(activityInfo == null) {
			log.error("No ActivityInfo instance found. The activity execution is ended due to an exception, see provider logs");
			return;
		}
		
		if ((activityInfo.getActivityStatus().getState() == ActivityStateEnumeration.FAILED)) {
            try {Thread.sleep(5000);}catch (InterruptedException e){}
            
            try {
				dataTransferrer.downloadStdOuts();
			} catch (GFacProviderException e) {
				throw new GFacHandlerException("Cannot download stdout data",e);
			}
		}
        else if (activityInfo.getActivityStatus().getState() == ActivityStateEnumeration.FINISHED) {
        	try {Thread.sleep(5000);}catch (InterruptedException e){}
        	
        	try {
					if (activityInfo.getActivityStatus().getExitCode() == 0) {
						dataTransferrer.downloadRemoteFiles();
					} else {
						dataTransferrer.downloadStdOuts();
					}
				} catch (GFacProviderException e) {
					throw new GFacHandlerException(
							"Cannot download stdout data", e);
				}
			}
		} finally {
			try {
				if (storageClient != null) {
					storageClient.destroy();
				}
			} catch (Exception e) {
				log.warn("Cannot destroy temporary SMS instance", e);
			}

		}		

 	}

}
