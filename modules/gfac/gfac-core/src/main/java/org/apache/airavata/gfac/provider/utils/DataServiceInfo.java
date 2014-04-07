package org.apache.airavata.gfac.provider.utils;

import java.io.Serializable;

import org.apache.airavata.gfac.context.JobExecutionContext;
import org.apache.airavata.gfac.provider.impl.BESConstants;
import org.apache.airavata.schemas.gfac.JobDirectoryModeDocument;
import org.apache.airavata.schemas.gfac.JobDirectoryModeDocument.JobDirectoryMode;
import org.apache.airavata.schemas.gfac.UnicoreHostType;
import org.apache.xmlbeans.XmlException;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;

/**
 * A value object carrying information about data service access mode.
 * */
public class DataServiceInfo implements BESConstants, Serializable {

	private static final long serialVersionUID = 1L;
	
	public enum DirectoryAccessMode {
		GridFTP, SMSBYTEIO, RNSBYTEIO
	}

	/*
	 * basically only uses information to hold gridftp address or an optional
	 * pointer to a remote StorageManagementService instance.
	 */
	private String dataServiceUrl;

	private DirectoryAccessMode directoryAccesMode = DirectoryAccessMode.SMSBYTEIO;
	
	public DataServiceInfo(JobExecutionContext c) {
		JobDirectoryMode.Enum directoryAccess = ((UnicoreHostType)c.getApplicationContext().getHostDescription().getType()).getJobDirectoryMode();
		
		switch(directoryAccess.intValue()) {
			case JobDirectoryMode.INT_SMS_BYTE_IO:
				directoryAccesMode =  DirectoryAccessMode.SMSBYTEIO;
				EndpointReferenceType s = (EndpointReferenceType) c
						.getProperty(PROP_SMS_EPR);
				dataServiceUrl = s.getAddress().getStringValue();
				
				break;
			case JobDirectoryMode.INT_GRID_FTP:
			case JobDirectoryMode.INT_RNS_BYTE_IO:
			default:
				directoryAccesMode =  DirectoryAccessMode.GridFTP;
				break;
		}
		
	}

	public String getDataServiceUrl() {
		return dataServiceUrl;
	}
	
	public void setDataServiceUrl(String dataServiceUrl) {
		this.dataServiceUrl = dataServiceUrl;
	}
	
	public DirectoryAccessMode getDirectoryAccesMode() {
		return directoryAccesMode;
	}


}
