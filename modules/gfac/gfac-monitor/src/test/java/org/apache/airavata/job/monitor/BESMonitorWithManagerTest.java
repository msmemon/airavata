package org.apache.airavata.job.monitor;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.airavata.commons.gfac.type.HostDescription;
import org.apache.airavata.gsi.ssh.impl.authentication.MyProxyAuthenticationInfo;
import org.apache.airavata.job.monitor.exception.AiravataMonitorException;
import org.apache.airavata.job.monitor.impl.pull.bes.BESPullJobMonitor;
import org.apache.airavata.job.monitor.state.JobStatus;
import org.apache.airavata.schemas.gfac.JobDirectoryModeDocument.JobDirectoryMode;
import org.apache.airavata.schemas.gfac.UnicoreHostType;
import org.ggf.schemas.bes.x2006.x08.besFactory.CreateActivityDocument;
import org.ggf.schemas.bes.x2006.x08.besFactory.CreateActivityResponseDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDescriptionType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.FileNameType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationDocument;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.junit.Before;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.bes.client.FactoryClient;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.DirectoryCertChainValidator;
import eu.emi.security.authn.x509.impl.PEMCredential;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

public class BESMonitorWithManagerTest {
	private MonitorManager monitorManager;
    private String myProxyUserName;
    private String myProxyPassword;
    private String certificateLocation;
    private String pbsFilePath;
    private String workingDirectory;
    private HostDescription hostDescription;
    
    protected FactoryClient factory;
    
    private DefaultClientConfiguration secProperties;
    protected static final String factoryUrl = "https://zam1161v01.zam.kfa-juelich.de:8002/INTEROP1/services/BESFactory?res=default_bes_factory";
    

    @Before
    public void setUp() throws Exception {
    	hostDescription = new HostDescription(UnicoreHostType.type);
        hostDescription.getType().setHostAddress("zam1161v01.kfa-juelich.de");
        hostDescription.getType().setHostName("interop-server");
  		((UnicoreHostType) hostDescription.getType())
  				.setUnicoreBESEndPointArray(new String[] { "https://zam1161v01.zam.kfa-juelich.de:8002/INTEROP1/services/BESFactory?res=default_bes_factory" });
        ((UnicoreHostType) hostDescription.getType()).setJobDirectoryMode(JobDirectoryMode.SMS_BYTE_IO);
        myProxyUserName = System.getProperty("myproxy.username");
        myProxyPassword = System.getProperty("myproxy.password");
        workingDirectory = System.getProperty("gsi.working.directory");
        certificateLocation = System.getProperty("trusted.cert.location");
        if (myProxyUserName == null || myProxyPassword == null) {
            System.out.println(">>>>>> Please run tests with my proxy user name and password. " +
                    "E.g :- mvn clean install -Dmyproxy.user=xxx -Dmyproxy.password=xxx -Dgsi.working.directory=/path<<<<<<<");
            throw new Exception("Need my proxy user name password to run tests.");
        }
        
        MyProxyAuthenticationInfo myProxy = new MyProxyAuthenticationInfo(myProxyUserName, myProxyPassword, "myproxy.teragrid.org", 7512, 17280000, certificateLocation);
    	  secProperties = getDefaultClientConfig((GlobusGSSCredentialImpl)myProxy.getCredentials());
    	  EndpointReferenceType eprt = WSUtilities.makeServiceEPR(factoryUrl);
    	  factory = new FactoryClient(eprt,secProperties);
    	  
          monitorManager = new MonitorManager();
          BESPullJobMonitor qstatMonitor = new
                  BESPullJobMonitor(monitorManager.getPullQueue(), monitorManager.getMonitorPublisher());
          try {
              monitorManager.addPullMonitor(qstatMonitor);
              monitorManager.launchMonitor();
          } catch (AiravataMonitorException e) {
              e.printStackTrace();
          }
    }
    
    private DefaultClientConfiguration getDefaultClientConfig(GlobusGSSCredentialImpl gss) throws Exception{
    	DefaultClientConfiguration secProperties = null;
		List<String> trustedCert = new ArrayList<String>();
		trustedCert.add(certificateLocation + "/*.0");
		trustedCert.add(certificateLocation + "/*.pem");
		DirectoryCertChainValidator dcValidator = new DirectoryCertChainValidator(trustedCert, Encoding.PEM, -1, 60000, null);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BufferedOutputStream bufos = new BufferedOutputStream(bos);
		ByteArrayInputStream bis = null;
		BufferedInputStream bufis = null;
		try{
			gss.getX509Credential().save(bufos);
			bufos.flush();
			char[] c = null;
			bis = new ByteArrayInputStream(bos.toByteArray());
			bufis = new BufferedInputStream(bis);
			PEMCredential cred = new PEMCredential(bufis, c);
			secProperties = new DefaultClientConfiguration(dcValidator, cred);
		}
		finally{
			if(bos!=null)bos.close();
			if(bufos!=null)bufos.close();
			if(bis!=null)bis.close();
			if(bufis!=null)bufis.close();
		}
		secProperties.getETDSettings().setExtendTrustDelegation(true);
    	return secProperties;
    }

    @Test
    public void testBESMonitor() throws Exception {
    	/* now have to submit a job to some machine and add that job to the queue */
    	//sme changes
        //Create authentication
    	MyProxyAuthenticationInfo authenticationInfo
        = new MyProxyAuthenticationInfo(myProxyUserName, myProxyPassword, "myproxy.teragrid.org",
        7512, 17280000, certificateLocation);
    	JobDefinitionDocument jsdlDefDoc = JobDefinitionDocument.Factory.newInstance();
    	JobDescriptionType jsdlDesc = jsdlDefDoc.addNewJobDefinition().addNewJobDescription();;
    	jsdlDesc.addNewApplication();
    	POSIXApplicationDocument posixDoc = POSIXApplicationDocument.Factory.newInstance();
    	FileNameType exec = FileNameType.Factory.newInstance();
    	exec.setStringValue("/bin/date");
    	posixDoc.addNewPOSIXApplication().setExecutable(exec);
    	WSUtilities.insertAny(posixDoc, jsdlDesc.getApplication());
    	CreateActivityDocument req = CreateActivityDocument.Factory.newInstance();
    	req.addNewCreateActivity().addNewActivityDocument().setJobDefinition(jsdlDefDoc.getJobDefinition());
        for (int i = 0; i < 1; i++) {
        	CreateActivityResponseDocument res = factory.createActivity(req);
        	assertTrue(res.getCreateActivityResponse().getActivityIdentifier() != null);
            String jobID = res.getCreateActivityResponse().getActivityIdentifier().toString();
            MonitorID monitorID = new MonitorID(hostDescription, jobID,null,null, "ogce");
            System.out.println("Job submitted successfully, Job ID: " +  jobID);
            monitorID = new MonitorID(hostDescription, jobID,null,null, "ogce");
            monitorID.setAuthenticationInfo(authenticationInfo);
            try {
                monitorManager.addAJobToMonitor(monitorID);
            } catch (AiravataMonitorException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        try {
            Thread.sleep(5000);
            BlockingQueue<UserMonitorData> pullQueue = monitorManager.getPullQueue();
            Iterator<UserMonitorData> iterator = pullQueue.iterator();
            UserMonitorData next = iterator.next();
            MonitorID monitorID = next.getHostMonitorData().get(0).getMonitorIDs().get(0);
            org.junit.Assert.assertNotNull(monitorID.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
    }









}
