package org.apache.airavata.core.gfac.services.impl;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import org.apache.airavata.commons.gfac.type.ActualParameter;
import org.apache.airavata.commons.gfac.type.ApplicationDescription;
import org.apache.airavata.gfac.context.ApplicationContext;
import org.apache.airavata.gfac.context.MessageContext;
import org.apache.airavata.gfac.cpi.GFacImpl;
import org.apache.airavata.schemas.gfac.ApplicationDeploymentDescriptionType;
import org.apache.airavata.schemas.gfac.ExtendedKeyValueType;
import org.apache.airavata.schemas.gfac.HpcApplicationDeploymentType;
import org.apache.airavata.schemas.gfac.JobDirectoryModeDocument.JobDirectoryMode;
import org.apache.airavata.schemas.gfac.JobTypeType;
import org.apache.airavata.schemas.gfac.URIParameterType;
import org.apache.airavata.schemas.gfac.UnicoreHostType;
import org.junit.Before;
import org.junit.Test;

public class SimpleBESWIthSMS extends AbstractBESTest{
	@Before
	public void initJobContext() throws Exception {
		initTest();
	}

	@Test
	public void submitJob() throws Exception {
		JobTypeType jobType = JobTypeType.Factory.newInstance();
		jobType.set(JobTypeType.SERIAL);
		ApplicationContext appContext = getApplicationContext();
		appContext.setApplicationDeploymentDescription(getApplicationDesc(jobType));
		jobExecutionContext.setApplicationContext(appContext);
		((UnicoreHostType) jobExecutionContext.getApplicationContext()
				.getHostDescription().getType())
				.setJobDirectoryMode(JobDirectoryMode.SMS_BYTE_IO);
		GFacImpl gFacAPI = new GFacImpl();
		gFacAPI.submitJob(jobExecutionContext);
		gFacAPI.invokeOutFlowHandlers(null, null);
	}
	
	
	protected ApplicationDescription getApplicationDesc(JobTypeType jobType) {
		ApplicationDescription appDesc = new ApplicationDescription(
				HpcApplicationDeploymentType.type);
		HpcApplicationDeploymentType appDepType = (HpcApplicationDeploymentType) appDesc
				.getType();
		ApplicationDeploymentDescriptionType.ApplicationName name = ApplicationDeploymentDescriptionType.ApplicationName.Factory
				.newInstance();
		name.setStringValue("Date-Job");
		appDepType.setApplicationName(name);

//		ProjectAccountType projectAccountType = appDepType.addNewProjectAccount();
//		projectAccountType.setProjectAccountNumber("TG-AST110064");

//		QueueType queueType = appDepType.addNewQueue();
//		queueType.setQueueName("development");

		// TODO: also handle parallel jobs
		if((jobType.enumValue() == JobTypeType.SERIAL) || (jobType.enumValue() == JobTypeType.SINGLE)) {
			appDepType.setJobType(JobTypeType.SERIAL);
		}
		else if (jobType.enumValue() == JobTypeType.MPI) {
			appDepType.setJobType(JobTypeType.MPI);
		}
		else {
			appDepType.setJobType(JobTypeType.OPEN_MP);
		}
		
		appDepType.setNodeCount(1);
		appDepType.setProcessorsPerNode(1);
		
		appDepType.setMaxWallTime(5);
		
		appDepType.setExecutableLocation("/bin/date");
		
		ExtendedKeyValueType extKV = appDepType.addNewKeyValuePairs();
		// using jsdl spmd standard
		extKV.setName("NumberOfProcesses");
		// this will be transformed into mpiexec -n 4
		extKV.setStringValue("3"); 
		
		/*
		 * Default tmp location
		 */
		String date = (new Date()).toString();
		date = date.replaceAll(" ", "_");
		date = date.replaceAll(":", "_");

		String remoteTempDir = scratchDir + File.separator + "US3" + "_" + date + "_"
				+ UUID.randomUUID();

		System.out.println(remoteTempDir);
		
		// no need of these parameters, as unicore manages by itself
		appDepType.setScratchWorkingDirectory(remoteTempDir);
		appDepType.setStaticWorkingDirectory(remoteTempDir);
		appDepType.setInputDataDirectory(remoteTempDir + File.separator + "inputData");
		appDepType.setOutputDataDirectory(remoteTempDir + File.separator + "outputData");
		
		appDepType.setStandardOutput(appDepType.getOutputDataDirectory()+"/stdout");
		
		appDepType.setStandardError(appDepType.getOutputDataDirectory()+"/stderr");

		return appDesc;
	}
	protected MessageContext getInMessageContext() {
		MessageContext inMessage = new MessageContext();
		
//	    ActualParameter a1 = new ActualParameter();
//	    a1.getType().changeType(StringParameterType.type);
//	    ((StringParameterType)a1.getType()).setValue("hpcinput-uslims3.uthscsa.edu-uslims3_cauma3-01594.tar");
//	    inMessage.addParameter("arg1", a1);
	        
//        ActualParameter i1 = new ActualParameter();
//        i1.getType().changeType(URIParameterType.type);
//        ((URIParameterType)i1.getType()).setValue("file:///"+System.getProperty("user.home")+"/hpcinput.tar");
//        inMessage.addParameter("i1", i1);

        return inMessage;
	}

	protected MessageContext getOutMessageContext() {
		MessageContext outMessage = new MessageContext();
		
//		ActualParameter a1 = new ActualParameter();
//		a1.getType().changeType(StringParameterType.type);
//		((StringParameterType)a1.getType()).setValue("output/analysis-results.tar");
//		outMessage.addParameter("o1", a1);

		return outMessage;
	}

}
