/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

package org.apache.airavata.gfac.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.airavata.gfac.GFacConfiguration;
import org.apache.airavata.gfac.GFacException;
import org.apache.airavata.gfac.SecurityContext;
import org.apache.airavata.gfac.notification.GFacNotifier;
import org.apache.airavata.gfac.provider.GFacProvider;
import org.apache.airavata.model.workspace.experiment.JobDetails;
import org.apache.airavata.model.workspace.experiment.TaskDetails;
import org.apache.airavata.registry.cpi.Registry;

public class JobExecutionContext extends AbstractContext{

    private GFacConfiguration gfacConfiguration;

    private ApplicationContext applicationContext;

    private MessageContext inMessageContext;

    private MessageContext outMessageContext;

    private GFacNotifier notifier;
    
    private TaskDetails taskData;

    private JobDetails jobDetails;

//    private ContextHeaderDocument.ContextHeader contextHeader;

    // Keep track of the current path of the message. Before hitting provider its in-path.
    // After provider its out-path.
    private boolean inPath = true;

    // Keep list of full qualified class names of GFac handlers which should invoked before
    // the provider. This is specific to current job being executed.
    private List<String> inHandlers = new ArrayList<String>();

    // Keep list of full qualified class names of GFac handlers which should invoked after
    // the provider. This is specific to current job being executed.
    private List<String> outHandlers = new ArrayList<String>();

    // During the execution of in-flow one of the handlers(Scheduling handler) will
    // set this and GFac API will get it from the JobExecutionContext and execute the provider.
    private GFacProvider provider;

    // Service description is used by GFac to mainly specify input/output parameters for a job
    // and to expose a job as a service to the outside world. This service concept abstract out
    // a scientific application(or algorithm) as a service. Service name is there to identify to
    // which service description we should refer during the execution of the current job represented
    // by this context instance.
    private String serviceName;

    private String experimentID;
    
    private String status;

    private List<String> outputFileList;
    
     private Registry registry;
    /**
     *  Security context is used to handle authentication for input handlers and providers.
     *  There can be multiple security requirement for a single job so this allows you to add multiple security types
     *
     */
    private Map<String, SecurityContext> securityContext = new HashMap<String, SecurityContext>();

    public JobExecutionContext(GFacConfiguration gFacConfiguration,String serviceName){
        this.gfacConfiguration = gFacConfiguration;
        notifier = new GFacNotifier();
        setServiceName(serviceName);
        outputFileList = new ArrayList<String>();
    }

    public String getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(String experimentID) {
        this.experimentID = experimentID;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public MessageContext getInMessageContext() {
        return inMessageContext;
    }

    public void setInMessageContext(MessageContext inMessageContext) {
        this.inMessageContext = inMessageContext;
    }

    public MessageContext getOutMessageContext() {
        return outMessageContext;
    }

    public void setOutMessageContext(MessageContext outMessageContext) {
        this.outMessageContext = outMessageContext;
    }

    public GFacConfiguration getGFacConfiguration() {
        return gfacConfiguration;
    }

    public GFacNotifier getNotificationService(){
        return notifier;
    }

    public GFacProvider getProvider() {
        return provider;
    }

    public void setProvider(GFacProvider provider) {
        this.gfacConfiguration.setInHandlers(provider.getClass().getName(),
                this.getServiceName());
        this.gfacConfiguration.setOutHandlers(provider.getClass().getName(),
        		 this.getServiceName());
        this.provider = provider;
    }

    public List<String> getInHandlers() {
        return inHandlers;
    }

    public void setInHandlers(List<String> inHandlers) {
        this.inHandlers = inHandlers;
    }

    public List<String> getOutHandlers() {
        return outHandlers;
    }

    public void setOutHandlers(List<String> outHandlers) {
        this.outHandlers = outHandlers;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public GFacNotifier getNotifier() {
        return notifier;
    }

    public boolean isInPath() {
        return inPath;
    }

    public TaskDetails getTaskData() {
		return taskData;
	}

	public void setTaskData(TaskDetails taskData) {
		this.taskData = taskData;
	}

	public boolean isOutPath(){
        return !inPath;
    }

    public void setInPath() {
        this.inPath = true;
    }

    public void setOutPath(){
        this.inPath = false;
    }

//    public ContextHeaderDocument.ContextHeader getContextHeader() {
//        return contextHeader;
//    }
//
//    public void setContextHeader(ContextHeaderDocument.ContextHeader contextHeader) {
//        this.contextHeader = contextHeader;
//    }

	
	public SecurityContext getSecurityContext(String name) throws GFacException{
		SecurityContext secContext = securityContext.get(name);
		return secContext;
	}

	public void addSecurityContext(String name, SecurityContext value){
		securityContext.put(name, value);
    }

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

    public JobDetails getJobDetails() {
        return jobDetails;
    }

    public void setJobDetails(JobDetails jobDetails) {
        this.jobDetails = jobDetails;
    }

    public void addOutputFile(String file) {
        outputFileList.add(file);
    }
    public List<String> getOutputFiles(){
        return outputFileList;
    }
    
    
        public Registry getRegistry() {
            return registry;
        }
    
        public void setRegistry(Registry registry) {
            this.registry = registry;
        }
}
