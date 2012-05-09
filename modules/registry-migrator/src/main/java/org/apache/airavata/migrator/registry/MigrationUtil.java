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

package org.apache.airavata.migrator.registry;

import org.apache.airavata.commons.gfac.type.ApplicationDeploymentDescription;
import org.apache.airavata.commons.gfac.type.HostDescription;
import org.apache.airavata.commons.gfac.type.ServiceDescription;
import org.apache.airavata.schemas.gfac.*;
import org.ogce.schemas.gfac.beans.ApplicationBean;
import org.ogce.schemas.gfac.beans.HostBean;
import org.ogce.schemas.gfac.beans.ServiceBean;
import org.ogce.schemas.gfac.beans.utils.ParamObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MigrationUtil {
    private static Logger log = LoggerFactory.getLogger(MigrationUtil.class);

    /**
     * Creates a HostDescription from HostBean
     *
     * @param hostBean HostBean
     * @return HostDescription
     */
    public static HostDescription createHostDescription(HostBean hostBean) {
        HostDescription host = new HostDescription();
        if(hostBean.getGateKeeperendPointReference()!=null ||
                hostBean.getGridFtpendPointReference()!=null) {
            host.getType().changeType(GlobusHostType.type);
            host.getType().setHostName(hostBean.getHostName());
            host.getType().setHostAddress(hostBean.getHostName());
            ((GlobusHostType) host.getType()).
                    setGridFTPEndPointArray(new String[]{hostBean.getGridFtpendPointReference()});
            ((GlobusHostType) host.getType()).
                    setGlobusGateKeeperEndPointArray(new String[]{
                            hostBean.getGateKeeperendPointReference()});
        } else {
            host.getType().setHostName(hostBean.getHostName());
            host.getType().setHostAddress(hostBean.getHostName());
        }
        return host;
    }

    /**
     * Creates ServiceDescription from ServiceBean
     *
     * @param serviceBean ServiceBean
     * @return ServiceDescription
     */
    public static ServiceDescription createServiceDescription(ServiceBean serviceBean) {
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.getType().setName(serviceBean.getServiceName());

        ArrayList<ParamObject> inputParameterTypes = serviceBean.getMethodBean().getInputParms();
        List<InputParameterType> inputList = new ArrayList<InputParameterType>();
        if (inputParameterTypes != null) {
            for (ParamObject inputParameterType : inputParameterTypes) {
                InputParameterType input = InputParameterType.Factory.newInstance();
                input.setParameterName(inputParameterType.getName());
                input.setParameterDescription(inputParameterType.getDesc());

                ParameterType parameterType = input.addNewParameterType();
                parameterType.setType(DataType.Enum.forString(inputParameterType.getType()));
                parameterType.setName(inputParameterType.getType());

                if(log.isDebugEnabled()) {
                    log.debug("Input param name : ." + inputParameterType.getName() + ".");
                    log.debug("            type : ." + inputParameterType.getType() + ".");
                    log.debug("        set type : ." +
                            DataType.Enum.forString(inputParameterType.getType()) + ".");
                }

                inputList.add(input);
            }
            InputParameterType[] inputParamList =
                    inputList.toArray(new InputParameterType[inputList.size()]);
            serviceDescription.getType().setInputParametersArray(inputParamList);
        }

        ArrayList<ParamObject> outputParameterTypes = serviceBean.getMethodBean().getOutputParms();
        List<OutputParameterType> outputList = new ArrayList<OutputParameterType>();
        if (outputParameterTypes != null){
            for (ParamObject outputParameterType : outputParameterTypes) {
                OutputParameterType output = OutputParameterType.Factory.newInstance();
                output.setParameterName(outputParameterType.getName());
                output.setParameterDescription(outputParameterType.getDesc());

                ParameterType parameterType = output.addNewParameterType();
                parameterType.setType(DataType.Enum.forString(outputParameterType.getType()));
                parameterType.setName(outputParameterType.getType());

                if(log.isDebugEnabled()) {
                    log.debug("Output param name : ." + outputParameterType.getName() + ".");
                    log.debug("             type : ." + outputParameterType.getType() + ".");
                    log.debug("         set type : ." +
                            DataType.Enum.forString(outputParameterType.getType()) + ".");
                }

                outputList.add(output);
            }
            OutputParameterType[] outputParamList =
                    outputList.toArray(new OutputParameterType[outputList.size()]);
            serviceDescription.getType().setOutputParametersArray(outputParamList);
        }

        return serviceDescription;
    }

    /**
     * Creates ServiceDescription with the given serviceName from ServiceBean
     *
     * @param serviceName Service name
     * @param serviceBean ServiceBean
     * @return ServiceDescription
     */
    public static ServiceDescription createServiceDescription(String serviceName,
                                                              ServiceBean serviceBean) {
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.getType().setName(serviceName);
        log.debug("\nSERVICE : " + serviceName);

        ArrayList<ParamObject> inputParameterTypes = serviceBean.getMethodBean().getInputParms();
        List<InputParameterType> inputList = new ArrayList<InputParameterType>();
        if (inputParameterTypes != null) {
            for (ParamObject inputParameterType : inputParameterTypes) {
                InputParameterType input = InputParameterType.Factory.newInstance();
                input.setParameterName(inputParameterType.getName());
                input.setParameterDescription(inputParameterType.getDesc());

                ParameterType parameterType = input.addNewParameterType();
                parameterType.setType(DataType.Enum.forString(inputParameterType.getType()));
                parameterType.setName(inputParameterType.getType());

                if(log.isDebugEnabled()) {
                    log.debug("Input param name : ." + inputParameterType.getName() + ".");
                    log.debug("            type : ." + inputParameterType.getType() + ".");
                    log.debug("        set type : ." +
                            DataType.Enum.forString(inputParameterType.getType()) + ".");
                }

                inputList.add(input);
            }
            InputParameterType[] inputParamList =
                    inputList.toArray(new InputParameterType[inputList.size()]);
            serviceDescription.getType().setInputParametersArray(inputParamList);
        }

        ArrayList<ParamObject> outputParameterTypes = serviceBean.getMethodBean().getOutputParms();
        List<OutputParameterType> outputList = new ArrayList<OutputParameterType>();
        if (outputParameterTypes != null){
            for (ParamObject outputParameterType : outputParameterTypes) {
                OutputParameterType output = OutputParameterType.Factory.newInstance();
                output.setParameterName(outputParameterType.getName());
                output.setParameterDescription(outputParameterType.getDesc());

                ParameterType parameterType = output.addNewParameterType();
                parameterType.setType(DataType.Enum.forString(outputParameterType.getType()));
                parameterType.setName(outputParameterType.getType());

                if(log.isDebugEnabled()) {
                    log.debug("Output param name : ." + outputParameterType.getName() + ".");
                    log.debug("             type : ." + outputParameterType.getType() + ".");
                    log.debug("         set type : ." +
                            DataType.Enum.forString(outputParameterType.getType()) + ".");
                }

                outputList.add(output);
            }
            OutputParameterType[] outputParamList =
                    outputList.toArray(new OutputParameterType[outputList.size()]);
            serviceDescription.getType().setOutputParametersArray(outputParamList);
        }

        return serviceDescription;
    }

    /**
     * Creates ApplicationDeploymentDescription from ApplicationBean
     *
     * @param appBean ApplicationBean
     * @return ApplicationDeploymentDescription
     */
    public static ApplicationDeploymentDescription createAppDeploymentDescription(
            ApplicationBean appBean) {
        ApplicationDeploymentDescription appDesc = new ApplicationDeploymentDescription();

        if(appBean.getJobType() != null) {
            appDesc.getType().changeType(GramApplicationDeploymentType.type);
            GramApplicationDeploymentType gram = (GramApplicationDeploymentType) appDesc.getType();
            ApplicationDeploymentDescriptionType.ApplicationName name =
                    ApplicationDeploymentDescriptionType.ApplicationName.Factory.newInstance();
            name.setStringValue(appBean.getApplicationName());

            gram.setApplicationName(name);
            gram.setExecutableLocation(appBean.getExecutable());
            gram.setScratchWorkingDirectory(appBean.getTmpDir());
            gram.setMaxWallTime(appBean.getMaxWallTime());
            if (appBean.getPcount() != null) {
                gram.setCpuCount(appBean.getPcount());
            }
            if (appBean.getMinMemory() != null) {
                gram.setMinMemory(appBean.getMinMemory());
            }

            gram.setJobType(getJobTypeEnum(appBean.getJobType()));
            ProjectAccountType projectAccount;
            projectAccount = getProjectAccountType(gram);
            projectAccount.setProjectAccountNumber(appBean.getProjectName());
            projectAccount.setProjectAccountDescription("");

            QueueType queueName;
            queueName = getQueueType(gram);
            queueName.setQueueName(appBean.getQueue());

        } else {
            ApplicationDeploymentDescriptionType app = appDesc.getType();
            ApplicationDeploymentDescriptionType.ApplicationName name =
                    ApplicationDeploymentDescriptionType.ApplicationName.Factory.newInstance();
            name.setStringValue(appBean.getApplicationName());

            app.setApplicationName(name);
            app.setExecutableLocation(appBean.getExecutable());
            app.setScratchWorkingDirectory(appBean.getTmpDir());
        }
        return appDesc;

    }

    private static ProjectAccountType getProjectAccountType(GramApplicationDeploymentType gram) {
        ProjectAccountType projectAccount;
        if(gram.getProjectAccount() != null) {
            projectAccount = gram.getProjectAccount();
        } else {
            projectAccount = gram.addNewProjectAccount();
        }
        return projectAccount;
    }

    /**
     * Creates ApplicationDeploymentDescription from ApplicationBean with a the provided
     * applicationName
     *
     * @param applicationName Application name
     * @param appBean ApplicationBean
     * @return ApplicationDeploymentDescription
     */
    public static ApplicationDeploymentDescription createAppDeploymentDescription(
            String applicationName, ApplicationBean appBean) {
        ApplicationDeploymentDescription appDesc = new ApplicationDeploymentDescription();

        if(appBean.getJobType() != null) {
            appDesc.getType().changeType(GramApplicationDeploymentType.type);
            GramApplicationDeploymentType gram = (GramApplicationDeploymentType) appDesc.getType();
            ApplicationDeploymentDescriptionType.ApplicationName name =
                    ApplicationDeploymentDescriptionType.ApplicationName.Factory.newInstance();
            name.setStringValue(applicationName);

            gram.setApplicationName(name);
            gram.setExecutableLocation(appBean.getExecutable());
            gram.setScratchWorkingDirectory(appBean.getTmpDir());
            gram.setMaxWallTime(appBean.getMaxWallTime());
            if (appBean.getPcount() != null) {
                gram.setCpuCount(appBean.getPcount());
            }
            if (appBean.getMinMemory() != null) {
                gram.setMinMemory(appBean.getMinMemory());
            }

            gram.setJobType(getJobTypeEnum(appBean.getJobType()));
            ProjectAccountType projectAccount;
            projectAccount = getProjectAccountType(gram);
            projectAccount.setProjectAccountNumber(appBean.getProjectName());
            projectAccount.setProjectAccountDescription("");

            QueueType queueName;
            queueName = getQueueType(gram);
            queueName.setQueueName(appBean.getQueue());

        } else {
            ApplicationDeploymentDescriptionType app = appDesc.getType();
            ApplicationDeploymentDescriptionType.ApplicationName name =
                    ApplicationDeploymentDescriptionType.ApplicationName.Factory.newInstance();
            name.setStringValue(applicationName);

            app.setApplicationName(name);
            app.setExecutableLocation(appBean.getExecutable());
            app.setScratchWorkingDirectory(appBean.getTmpDir());
        }
        return appDesc;
    }

    private static QueueType getQueueType(GramApplicationDeploymentType gram) {
        QueueType queueName;
        if(gram.getQueue() != null) {
            queueName = gram.getQueue();
        } else {
            queueName = gram.addNewQueue();
        }
        return queueName;
    }

    public static JobTypeType.Enum getJobTypeEnum(String jobTypeString){
        for (JobTypeType.Enum jobTypeEnum : getJobTypes()) {
            if (jobTypeEnum.toString().equalsIgnoreCase(jobTypeString)){
                return jobTypeEnum;
            }
        }
        return null;
    }

    private static List<JobTypeType.Enum> getJobTypes() {
        List<JobTypeType.Enum> jobTypes;
        jobTypes = new ArrayList<JobTypeType.Enum>();
        jobTypes.add(JobTypeType.OPEN_MP);
        jobTypes.add(JobTypeType.MPI);
        jobTypes.add(JobTypeType.SERIAL);
        jobTypes.add(JobTypeType.SINGLE);
        return jobTypes;
    }
}