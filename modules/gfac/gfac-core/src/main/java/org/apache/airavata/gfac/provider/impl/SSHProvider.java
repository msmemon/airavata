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

package org.apache.airavata.gfac.provider.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.airavata.commons.gfac.type.ActualParameter;
import org.apache.airavata.commons.gfac.type.MappingFactory;
import org.apache.airavata.gfac.Constants;
import org.apache.airavata.gfac.GFacException;
import org.apache.airavata.gfac.context.JobExecutionContext;
import org.apache.airavata.gfac.context.MessageContext;
import org.apache.airavata.gfac.context.security.SSHSecurityContext;
import org.apache.airavata.gfac.provider.GFacProvider;
import org.apache.airavata.gfac.provider.GFacProviderException;
import org.apache.airavata.gfac.utils.GFacUtils;
import org.apache.airavata.gsi.ssh.api.Cluster;
import org.apache.airavata.gsi.ssh.api.CommandExecutor;
import org.apache.airavata.gsi.ssh.api.SSHApiException;
import org.apache.airavata.gsi.ssh.impl.RawCommandInfo;
import org.apache.airavata.gsi.ssh.impl.StandardOutReader;
import org.apache.airavata.model.workspace.experiment.JobState;
import org.apache.airavata.schemas.gfac.ApplicationDeploymentDescriptionType;
import org.apache.airavata.schemas.gfac.NameValuePairType;
import org.apache.airavata.schemas.gfac.SSHHostType;
import org.apache.airavata.schemas.gfac.URIArrayType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Execute application using remote SSH
 */
public class SSHProvider extends AbstractProvider implements GFacProvider{
    private static final Logger log = LoggerFactory.getLogger(SSHProvider.class);
    private Cluster cluster;
    private String jobID = null;
    private String taskID = null;
    // we keep gsisshprovider to support qsub submission incase of hpc scenario with ssh
    private GSISSHProvider gsiSshProvider = null;

    public void initialize(JobExecutionContext jobExecutionContext) throws GFacProviderException, GFacException {
    	super.initialize(jobExecutionContext);
    	taskID = jobExecutionContext.getTaskData().getTaskID();
		if (!((SSHHostType) jobExecutionContext.getApplicationContext().getHostDescription().getType()).getHpcResource()) {
            jobID = "SSH_" + jobExecutionContext.getApplicationContext().getHostDescription().getType().getHostAddress() + "_" + Calendar.getInstance().getTimeInMillis();
            cluster = ((SSHSecurityContext) jobExecutionContext.getSecurityContext(SSHSecurityContext.SSH_SECURITY_CONTEXT)).getPbsCluster();
            
            ApplicationDeploymentDescriptionType app = jobExecutionContext.getApplicationContext().getApplicationDeploymentDescription().getType();
            String remoteFile = app.getStaticWorkingDirectory() + File.separatorChar + Constants.EXECUTABLE_NAME;
            details.setJobDescription(remoteFile);
            GFacUtils.saveJobStatus(details, JobState.SETUP, taskID);
            log.info(remoteFile);
            try {
            	File runscript = createShellScript(jobExecutionContext);
                cluster.scpTo(remoteFile, runscript.getAbsolutePath());
            } catch (Exception e) {
                throw new GFacProviderException(e.getLocalizedMessage(), e);
            }
        }else{
           gsiSshProvider = new GSISSHProvider();
        }
    }


    public void execute(JobExecutionContext jobExecutionContext) throws GFacProviderException {
        if (gsiSshProvider == null) {
            ApplicationDeploymentDescriptionType app = jobExecutionContext.getApplicationContext().getApplicationDeploymentDescription().getType();
            try {
                /*
                 * Execute
                 */
                String execuable = app.getStaticWorkingDirectory() + File.separatorChar + Constants.EXECUTABLE_NAME;
                details.setJobDescription(execuable);
                GFacUtils.updateJobStatus(details, JobState.SUBMITTED);
                RawCommandInfo rawCommandInfo = new RawCommandInfo("/bin/chmod 755 " + execuable + "; " + execuable);

                StandardOutReader jobIDReaderCommandOutput = new StandardOutReader();
                
                CommandExecutor.executeCommand(rawCommandInfo, cluster.getSession(), jobIDReaderCommandOutput);
                String stdOutputString = getOutputifAvailable(jobIDReaderCommandOutput, "Error submitting job to resource");

                log.info("stdout=" + stdOutputString);
             
                GFacUtils.updateJobStatus(details, JobState.COMPLETE);
            } catch (Exception e) {
                throw new GFacProviderException(e.getMessage(), e);
            } finally {
                if (cluster != null) {
                	try {
						cluster.disconnect();
					} catch (SSHApiException e) {
						throw new GFacProviderException(e.getMessage(), e);
					}
                }
            }
        } else {
            try {
                gsiSshProvider.execute(jobExecutionContext);
            } catch (GFacException e) {
                throw new GFacProviderException(e.getMessage(), e);
            }
        }
    }

    public void dispose(JobExecutionContext jobExecutionContext) throws GFacProviderException {
        if (gsiSshProvider != null){
            try {
                gsiSshProvider.dispose(jobExecutionContext);
            } catch (GFacException e) {
                throw new GFacProviderException(e.getMessage(),e);
            }
        }
    }


    public void cancelJob(String jobId, JobExecutionContext jobExecutionContext) throws GFacException {
        throw new NotImplementedException();
    }


    private File createShellScript(JobExecutionContext context) throws IOException {
        ApplicationDeploymentDescriptionType app = context.getApplicationContext()
                .getApplicationDeploymentDescription().getType();
        String uniqueDir = app.getApplicationName().getStringValue() + System.currentTimeMillis()
                + new Random().nextLong();

        File shellScript = File.createTempFile(uniqueDir, "sh");
        OutputStream out = new FileOutputStream(shellScript);

        out.write("#!/bin/bash\n".getBytes());
        out.write(("cd " + app.getStaticWorkingDirectory() + "\n").getBytes());
        out.write(("export " + Constants.INPUT_DATA_DIR_VAR_NAME + "=" + app.getInputDataDirectory() + "\n").getBytes());
        out.write(("export " + Constants.OUTPUT_DATA_DIR_VAR_NAME + "=" + app.getOutputDataDirectory() + "\n")
                .getBytes());
        // get the env of the host and the application
        NameValuePairType[] env = app.getApplicationEnvironmentArray();

        Map<String, String> nv = new HashMap<String, String>();
        if (env != null) {
            for (int i = 0; i < env.length; i++) {
                String key = env[i].getName();
                String value = env[i].getValue();
                nv.put(key, value);
            }
        }
        for (Entry<String, String> entry : nv.entrySet()) {
            log.debug("Env[" + entry.getKey() + "] = " + entry.getValue());
            out.write(("export " + entry.getKey() + "=" + entry.getValue() + "\n").getBytes());

        }

        // prepare the command
        final String SPACE = " ";
        StringBuffer cmd = new StringBuffer();
        cmd.append(app.getExecutableLocation());
        cmd.append(SPACE);

        MessageContext input = context.getInMessageContext();
        ;
        Map<String, Object> inputs = input.getParameters();
        Set<String> keys = inputs.keySet();
        for (String paramName : keys) {
            ActualParameter actualParameter = (ActualParameter) inputs.get(paramName);
            if ("URIArray".equals(actualParameter.getType().getType().toString())) {
                String[] values = ((URIArrayType) actualParameter.getType()).getValueArray();
                for (String value : values) {
                    cmd.append(value);
                    cmd.append(SPACE);
                }
            } else {
                String paramValue = MappingFactory.toString(actualParameter);
                cmd.append(paramValue);
                cmd.append(SPACE);
            }
        }
        // We redirect the error and stdout to remote files, they will be read
        // in later
        cmd.append(SPACE);
        cmd.append("1>");
        cmd.append(SPACE);
        cmd.append(app.getStandardOutput());
        cmd.append(SPACE);
        cmd.append("2>");
        cmd.append(SPACE);
        cmd.append(app.getStandardError());

        String cmdStr = cmd.toString();
        log.info("Command = " + cmdStr);
        out.write((cmdStr + "\n").getBytes());
        String message = "\"execuationSuceeded\"";
        out.write(("echo " + message + "\n").getBytes());
        out.close();

        return shellScript;
    }

    public void initProperties(Map<String, String> properties) throws GFacProviderException, GFacException {
         if (gsiSshProvider != null){
            try {
               initProperties(properties);
            } catch (GFacException e) {
                throw new GFacProviderException(e.getMessage(),e);
            }
        }
    }
    /**
     * This method will read standard output and if there's any it will be parsed
     * @param jobIDReaderCommandOutput
     * @param errorMsg
     * @return
     * @throws SSHApiException
     */
    private String getOutputifAvailable(StandardOutReader jobIDReaderCommandOutput, String errorMsg) throws SSHApiException {
        String stdOutputString = jobIDReaderCommandOutput.getStdOutputString();
        String stdErrorString = jobIDReaderCommandOutput.getStdErrorString();

        if(stdOutputString == null || stdOutputString.isEmpty() || (stdErrorString != null && !stdErrorString.isEmpty())){
            log.error("Standard Error output : " + stdErrorString);
            throw new SSHApiException(errorMsg + stdErrorString);
        }
        return stdOutputString;
    }


}
