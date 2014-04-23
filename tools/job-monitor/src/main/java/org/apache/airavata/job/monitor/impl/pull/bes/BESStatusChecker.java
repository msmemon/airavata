package org.apache.airavata.job.monitor.impl.pull.bes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.airavata.gsi.ssh.api.authentication.AuthenticationInfo;
import org.apache.airavata.gsi.ssh.impl.authentication.MyProxyAuthenticationInfo;
import org.apache.airavata.job.monitor.HostMonitorData;
import org.apache.airavata.job.monitor.MonitorID;
import org.apache.airavata.model.workspace.experiment.JobState;
import org.apache.airavata.schemas.gfac.UnicoreHostType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.ggf.schemas.bes.x2006.x08.besFactory.ActivityStateEnumeration;
import org.ggf.schemas.bes.x2006.x08.besFactory.ActivityStatusType;
import org.ggf.schemas.bes.x2006.x08.besFactory.ActivityStateEnumeration.Enum;
import org.ggf.schemas.bes.x2006.x08.besFactory.GetActivityStatusesDocument;
import org.ggf.schemas.bes.x2006.x08.besFactory.GetActivityStatusesResponseDocument;
import org.ggf.schemas.bes.x2006.x08.besFactory.GetActivityStatusesType;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.x2005.x08.addressing.EndpointReferenceType;
import de.fzj.unicore.bes.client.FactoryClient;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.emi.security.authn.x509.impl.DirectoryCertChainValidator;
import eu.emi.security.authn.x509.impl.PEMCredential;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.util.httpclient.DefaultClientConfiguration;


// not tested 

public class BESStatusChecker {
	
	private static final Logger log = LoggerFactory.getLogger(BESStatusChecker.class);
	
	protected FactoryClient factory;
	
	protected DefaultClientConfiguration secProperties;
	
	private AuthenticationInfo authenticationInfo;
	
	public BESStatusChecker(HostMonitorData host) throws Exception {
		// it is assumed that all the child monitors have same security configuration,
		// therefore takes the first authentication info
        authenticationInfo = host.getMonitorIDs().get(0).getAuthenticationInfo();
        buildClientConfiguration();
        String factoryUrl = ((UnicoreHostType)host.getHost().getType()).getUnicoreBESEndPointArray()[0];
        EndpointReferenceType eprt = WSUtilities.makeServiceEPR(factoryUrl);
        factory = new FactoryClient(eprt, secProperties);
	}
	
	public void buildClientConfiguration() throws Exception{
		if(authenticationInfo instanceof MyProxyAuthenticationInfo) {
			MyProxyAuthenticationInfo myproxy =  (MyProxyAuthenticationInfo)authenticationInfo;
			GlobusGSSCredentialImpl gss = (GlobusGSSCredentialImpl) myproxy.getCredentials();
			String trustedRootDir = (String)myproxy.getProperties().getProperty(MyProxyAuthenticationInfo.X509_CERT_DIR);
			ByteArrayOutputStream bos = null;
			BufferedOutputStream bufos = null;
			ByteArrayInputStream bis = null;
			BufferedInputStream bufis = null;
			try{
				List<String> trustedCert = new ArrayList<String>();
				trustedCert.add(trustedRootDir + "/*.0");
				trustedCert.add(trustedRootDir + "/*.pem");
				DirectoryCertChainValidator dcValidator;
				dcValidator = new DirectoryCertChainValidator(trustedCert, Encoding.PEM, -1, 60000, null);
				bos = new ByteArrayOutputStream();
				bufos = new BufferedOutputStream(bos);
				bis = null;
				bufis = null;
				gss.getX509Credential().save(bufos);
				bufos.flush();
				char[] c = null;
				bis = new ByteArrayInputStream(bos.toByteArray());
				bufis = new BufferedInputStream(bis);
				PEMCredential cred = new PEMCredential(bufis, c);
				secProperties = new DefaultClientConfiguration(dcValidator, cred);
	//			setExtraSettings();
			}
			catch (GeneralSecurityException e) {
				throw new Exception("Cannot create UNICORE client's security configuration.", e); 
			}		
			finally{
				try {
					if(bos!=null)bos.close();
					if(bufos!=null)bufos.close();
					if(bis!=null)bis.close();
					if(bufis!=null)bufis.close();
				} catch (IOException e) {
					log.error("Error closing IO streams.", e);
				}
			}
			
			secProperties.getETDSettings().setExtendTrustDelegation(true);
		}
	}
	
    public Map<String,JobState> getJobStatuses(String userName,List<MonitorID> monitorIDs) throws Exception {
        Map<String,JobState> statusMap = new TreeMap<String,JobState>();
        
        List<EndpointReferenceType> eprt = new ArrayList<EndpointReferenceType>(monitorIDs.size());
        
        for (MonitorID monitorID : monitorIDs) {
        	System.out.println(monitorID.getJobID());
            eprt.add(EndpointReferenceType.Factory.parse(monitorID.getJobID()));
        }

        GetActivityStatusesDocument getStatsDoc = GetActivityStatusesDocument.Factory.newInstance(); 
        GetActivityStatusesType getStatsType = getStatsDoc.addNewGetActivityStatuses();
        getStatsType.setActivityIdentifierArray(eprt.toArray(new EndpointReferenceType[eprt.size()]));
        GetActivityStatusesResponseDocument getStatsResDoc;
        
        if(monitorIDs.size()>0) {
        	 getStatsResDoc = factory.getActivityStatuses(getStatsDoc);
        	 int i=0;
        	 for (MonitorID monitorID : monitorIDs){
	        	 ActivityStatusType asType = getStatsResDoc.getGetActivityStatusesResponse().getResponseArray()[i++].getActivityStatus();
	             statusMap.put(monitorID.getJobID(),getApplicationJobStatus(asType));
	         }
        }
        return statusMap;
    }
    
	private JobState getApplicationJobStatus(ActivityStatusType activityStatus) throws XmlException{
		
        if (activityStatus == null) {
            return JobState.UNKNOWN;
        }
        Enum state = activityStatus.getState();
        String status = null;
        XmlCursor acursor = activityStatus.newCursor();
        try {
            if (acursor.toFirstChild()) {
                if (acursor.getName().getNamespaceURI().equals("http://schemas.ogf.org/hpcp/2007/01/fs")) {
                    status = acursor.getName().getLocalPart();
                }
            }
            if (status != null) {
                if (status.equalsIgnoreCase("Queued") || status.equalsIgnoreCase("Starting")
                        || status.equalsIgnoreCase("Ready")) {
                    return JobState.QUEUED;
                } else if (status.equalsIgnoreCase("Staging-In")) {
                    return JobState.SUBMITTED;
                } else if (status.equalsIgnoreCase("Staging-Out") || status.equalsIgnoreCase("FINISHED")) {
                    return JobState.COMPLETE;
                } else if (status.equalsIgnoreCase("Executing")) {
                    return JobState.ACTIVE;
                } else if (status.equalsIgnoreCase("FAILED")) {
                    return JobState.FAILED;
                } else if (status.equalsIgnoreCase("CANCELLED")) {
                    return JobState.CANCELED;
                }
            } else {
                if (ActivityStateEnumeration.CANCELLED.equals(state)) {
                    return JobState.CANCELED;
                } else if (ActivityStateEnumeration.FAILED.equals(state)) {
                    return JobState.FAILED;
                } else if (ActivityStateEnumeration.FINISHED.equals(state)) {
                    return JobState.COMPLETE;
                } else if (ActivityStateEnumeration.RUNNING.equals(state)) {
                    return JobState.ACTIVE;
                }
            }
        } finally {
            if (acursor != null)
                acursor.dispose();
        }
        return JobState.UNKNOWN;
    }

}
