package org.apache.airavata.job.monitor;

import static org.testng.AssertJUnit.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.airavata.commons.gfac.type.HostDescription;
import org.apache.airavata.gsi.ssh.api.Cluster;
import org.apache.airavata.gsi.ssh.api.SSHApiException;
import org.apache.airavata.gsi.ssh.api.ServerInfo;
import org.apache.airavata.gsi.ssh.api.authentication.GSIAuthenticationInfo;
import org.apache.airavata.gsi.ssh.api.job.JobDescriptor;
import org.apache.airavata.gsi.ssh.impl.PBSCluster;
import org.apache.airavata.gsi.ssh.impl.authentication.MyProxyAuthenticationInfo;
import org.apache.airavata.gsi.ssh.util.CommonUtils;
import org.apache.airavata.job.monitor.event.MonitorPublisher;
import org.apache.airavata.job.monitor.impl.pull.bes.BESPullJobMonitor;
import org.apache.airavata.job.monitor.impl.pull.qstat.QstatMonitor;
import org.apache.airavata.job.monitor.state.JobStatus;
import org.apache.airavata.model.workspace.experiment.JobState;
import org.apache.airavata.schemas.gfac.GsisshHostType;
import org.apache.airavata.schemas.gfac.JobDirectoryModeDocument.JobDirectoryMode;
import org.apache.airavata.schemas.gfac.UnicoreHostType;
import org.ggf.schemas.bes.x2006.x08.besFactory.CreateActivityDocument;
import org.ggf.schemas.bes.x2006.x08.besFactory.CreateActivityResponseDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDescriptionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDescriptionType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.FileNameType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationDocument;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import de.fzj.unicore.bes.client.FactoryClient;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.emi.security.authn.x509.impl.DirectoryCertChainValidator;
import eu.emi.security.authn.x509.impl.PEMCredential;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.UnicoreSecurityFactory;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

public class BESPullMonitorTest {
	
    private String myProxyUserName;
    private String myProxyPassword;
    private String certificateLocation;
    private String workingDirectory;
    private HostDescription hostDescription;
    private MonitorPublisher monitorPublisher;
    private BlockingQueue<UserMonitorData> pullQueue;
    private Thread monitorThread;
    protected FactoryClient factory;
    
    private DefaultClientConfiguration secProperties;
    protected static final String factoryUrl = "https://zam1161v01.zam.kfa-juelich.de:8002/INTEROP1/services/BESFactory?res=default_bes_factory";
    
    @org.testng.annotations.BeforeClass
    public void setUp() throws Exception {
    	
        hostDescription = new HostDescription(UnicoreHostType.type);
        hostDescription.getType().setHostAddress("zam1161v01.kfa-juelich.de");
        hostDescription.getType().setHostName("interop-server");
  		((UnicoreHostType) hostDescription.getType())
  				.setUnicoreBESEndPointArray(new String[] { "https://zam1161v01.zam.kfa-juelich.de:8002/INTEROP1/services/BESFactory?res=default_bes_factory" });
        ((UnicoreHostType) hostDescription.getType()).setJobDirectoryMode(JobDirectoryMode.SMS_BYTE_IO);

    	
//      System.setProperty("myproxy.username", "ogce");
//      System.setProperty("myproxy.password", "");
//      System.setProperty("basedir", "/Users/lahirugunathilake/work/airavata/sandbox/gsissh");
//      System.setProperty("gsi.working.directory", "/home/ogce");
//      System.setProperty("trusted.cert.location", "/Users/lahirugunathilake/Downloads/certificates");

    	
      myProxyUserName = System.getProperty("myproxy.username");
      myProxyPassword = System.getProperty("myproxy.password");
      workingDirectory = System.getProperty("gsi.working.directory");
      certificateLocation = System.getProperty("trusted.cert.location");
      if (myProxyUserName == null || myProxyPassword == null) {
          System.out.println(">>>>>> Please run tests with my proxy user name and password. " +
                  "E.g :- mvn clean install -Dmyproxy.username=xxx -Dmyproxy.password=xxx -Dgsi.working.directory=/path<<<<<<<");
          throw new Exception("Need my proxy user name password to run tests.");
      }
      
      MyProxyAuthenticationInfo myProxy = new MyProxyAuthenticationInfo(myProxyUserName, myProxyPassword, "myproxy.teragrid.org", 7512, 17280000, certificateLocation);
  	  secProperties = getDefaultClientConfig((GlobusGSSCredentialImpl)myProxy.getCredentials());
  	  EndpointReferenceType eprt = WSUtilities.makeServiceEPR(factoryUrl);
  	  factory = new FactoryClient(eprt,secProperties);
      monitorPublisher =  new MonitorPublisher(new EventBus());
      pullQueue = new LinkedBlockingQueue<UserMonitorData>();
      BESPullJobMonitor besMonitor = new BESPullJobMonitor(pullQueue, monitorPublisher);
      try {
          monitorThread = (new Thread(besMonitor));
          monitorThread.start();
      } catch (Exception e) {
          e.printStackTrace();
      }
      
      monitorPublisher.registerListener(this);
      
    }

    
    public void testQstatMonitor() throws SSHApiException {
        /* now have to submit a job to some machine and add that job to the queue */
        //Create authentication
    	
    	MyProxyAuthenticationInfo authenticationInfo
        = new MyProxyAuthenticationInfo(myProxyUserName, myProxyPassword, "myproxy.teragrid.org",
        7512, 17280000, certificateLocation);
    	
        // Server info
        ServerInfo serverInfo = new ServerInfo("ogce", hostDescription.getType().getHostAddress());


        Cluster pbsCluster = new PBSCluster(serverInfo, null, CommonUtils.getPBSJobManager("/opt/torque/bin/"));


        // Execute command
        System.out.println("Target PBS file path: " + workingDirectory);
        // constructing the job object
        JobDescriptor jobDescriptor = new JobDescriptor();
        jobDescriptor.setWorkingDirectory(workingDirectory);
        jobDescriptor.setShellName("/bin/bash");
        jobDescriptor.setJobName("GSI_SSH_SLEEP_JOB");
        jobDescriptor.setExecutablePath("/bin/echo");
        jobDescriptor.setAllEnvExport(true);
        jobDescriptor.setMailOptions("n");
        jobDescriptor.setStandardOutFile(workingDirectory + File.separator + "application.out");
        jobDescriptor.setStandardErrorFile(workingDirectory + File.separator + "application.err");
        jobDescriptor.setNodes(1);
        jobDescriptor.setProcessesPerNode(1);
        jobDescriptor.setQueueName("normal");
        jobDescriptor.setMaxWallTime("60");
        jobDescriptor.setAcountString("sds128");
        List<String> inputs = new ArrayList<String>();
        jobDescriptor.setOwner("ogce");
        inputs.add("Hello World");
        jobDescriptor.setInputValues(inputs);
        //finished construction of job object
        System.out.println(jobDescriptor.toXML());
        
        String jobID = pbsCluster.submitBatchJob(jobDescriptor);
        System.out.println("Job submitted successfully, Job ID: " +  jobID);
        MonitorID monitorID = new MonitorID(hostDescription, jobID,null,null, "ogce");
        monitorID.setAuthenticationInfo(authenticationInfo);
        try {
            org.apache.airavata.job.monitor.util.CommonUtils.addMonitortoQueue(pullQueue, monitorID);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        try {
            monitorThread.join(5000);
            Iterator<UserMonitorData> iterator = pullQueue.iterator();
            UserMonitorData next = iterator.next();
            monitorID = next.getHostMonitorData().get(0).getMonitorIDs().get(0);
            org.junit.Assert.assertNotNull(monitorID.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testBESMonitor() throws Exception {
        /* now have to submit a job to some machine and add that job to the queue */
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
    	CreateActivityResponseDocument res = factory.createActivity(req);
    	
    	assertTrue(res.getCreateActivityResponse().getActivityIdentifier() != null);

        String jobID = res.getCreateActivityResponse().getActivityIdentifier().toString();

        MonitorID monitorID = new MonitorID(hostDescription, jobID,null,null, "ogce");
        
        System.out.println("Job submitted successfully, Job ID: " +  jobID);
        monitorID = new MonitorID(hostDescription, jobID,null,null, "ogce");
        monitorID.setAuthenticationInfo(authenticationInfo);
        try {
            org.apache.airavata.job.monitor.util.CommonUtils.addMonitortoQueue(pullQueue, monitorID);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        testCaseShutDown(monitorID.getStatus());

//        try {
//        	monitorThread.join(1000);
//            Iterator<UserMonitorData> iterator = pullQueue.iterator();
//            UserMonitorData next = iterator.next();
//            monitorID = next.getHostMonitorData().get(0).getMonitorIDs().get(0);
//            System.out.println("Job Status: "+ monitorID.getStatus());
//            assertNotNull(monitorID.getStatus());
//            testCaseShutDown(monitorID.getStatus());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
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
    
    @Subscribe
    public void testCaseShutDown(JobState status) {
        System.out.println("Hello");
        assertNotNull(status);
        monitorThread.stop();
    }
}
