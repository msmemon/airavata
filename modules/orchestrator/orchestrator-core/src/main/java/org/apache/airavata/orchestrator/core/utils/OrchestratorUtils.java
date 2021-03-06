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
package org.apache.airavata.orchestrator.core.utils;

import org.apache.airavata.common.utils.ServerSettings;
import org.apache.airavata.commons.gfac.type.HostDescription;
import org.apache.airavata.gfac.utils.GFacUtils;
import org.apache.airavata.model.workspace.experiment.ComputationalResourceScheduling;
import org.apache.airavata.model.workspace.experiment.TaskDetails;
import org.apache.airavata.orchestrator.core.OrchestratorConfiguration;
import org.apache.airavata.orchestrator.core.exception.OrchestratorException;
import org.apache.airavata.orchestrator.core.impl.EmbeddedGFACJobSubmitter;
import org.apache.airavata.orchestrator.core.job.JobSubmitter;
import org.apache.airavata.orchestrator.cpi.Orchestrator;
import org.apache.airavata.orchestrator.cpi.impl.SimpleOrchestratorImpl;
import org.apache.airavata.registry.api.AiravataRegistry2;
import org.apache.airavata.registry.api.exception.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * This contains orchestrator specific utilities
 */
public class OrchestratorUtils {
    private final static Logger logger = LoggerFactory.getLogger(OrchestratorUtils.class);

    public static OrchestratorConfiguration loadOrchestratorConfiguration() throws OrchestratorException, IOException {
        OrchestratorConfiguration orchestratorConfiguration = new OrchestratorConfiguration();
        Properties orchestratorProps = ServerSettings.getProperties();
        orchestratorConfiguration.setNewJobSubmitterClass((String) orchestratorProps.get(OrchestratorConstants.JOB_SUBMITTER));
        orchestratorConfiguration.setSubmitterInterval(Integer.parseInt((String) orchestratorProps.get(OrchestratorConstants.SUBMIT_INTERVAL)));
        orchestratorConfiguration.setThreadPoolSize(Integer.parseInt((String) orchestratorProps.get(OrchestratorConstants.THREAD_POOL_SIZE)));
        orchestratorConfiguration.setStartSubmitter(Boolean.valueOf(orchestratorProps.getProperty(OrchestratorConstants.START_SUBMITTER)));
        orchestratorConfiguration.setEmbeddedMode(Boolean.valueOf(orchestratorProps.getProperty(OrchestratorConstants.EMBEDDED_MODE)));
        orchestratorConfiguration.setEnableValidation(Boolean.valueOf(orchestratorProps.getProperty(OrchestratorConstants.ENABLE_VALIDATION)));
        if (orchestratorConfiguration.isEnableValidation()) {
            orchestratorConfiguration.setValidatorClass((String) orchestratorProps.get(OrchestratorConstants.JOB_VALIDATOR));
        }
        return orchestratorConfiguration;
    }

    public static HostDescription getHostDescription(Orchestrator orchestrator, TaskDetails taskDetails)throws OrchestratorException {
        JobSubmitter jobSubmitter = ((SimpleOrchestratorImpl) orchestrator).getJobSubmitter();
        AiravataRegistry2 registry = ((EmbeddedGFACJobSubmitter) jobSubmitter).getOrchestratorContext().getRegistry();
        ComputationalResourceScheduling taskScheduling = taskDetails.getTaskScheduling();
        String resourceHostId = taskScheduling.getResourceHostId();
        try {
            return registry.getHostDescriptor(resourceHostId);
        } catch (RegistryException e) {
            throw new OrchestratorException(e);
        }
    }

}
