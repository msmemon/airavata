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

package org.apache.airavata.gfac.provider.utils;

import org.apache.airavata.gfac.context.JobExecutionContext;
import org.apache.airavata.gfac.provider.GFacProviderException;
import org.apache.airavata.model.workspace.experiment.ComputationalResourceScheduling;
import org.apache.airavata.model.workspace.experiment.TaskDetails;
import org.apache.airavata.schemas.gfac.HpcApplicationDeploymentType;
import org.apache.airavata.schemas.gfac.QueueType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionType;
import org.ogf.schemas.jsdl.x2007.x02.jsdlSpmd.NumberOfProcessesType;

public class ResourceProcessor {

	
	public static void generateResourceElements(JobDefinitionType value, JobExecutionContext context) throws Exception{
		
		HpcApplicationDeploymentType appDepType = (HpcApplicationDeploymentType) context
				.getApplicationContext().getApplicationDeploymentDescription()
				.getType();
		
		createMemory(value, appDepType);
		TaskDetails taskData = context.getTaskData();
	    if(taskData != null && taskData.isSetTaskScheduling()){
	    	ComputationalResourceScheduling computionResource= taskData.getTaskScheduling();
                try {
                    int cpuCount = computionResource.getTotalCPUCount();
                    if(cpuCount>0){
//                    	appDepType.setCpuCount(cpuCount);
                		NumberOfProcessesType num = NumberOfProcessesType.Factory.newInstance();
    					String processers = Integer.toString(cpuCount);
						num.setStringValue(processers);
    					JSDLUtils.getOrCreateSPMDApplication(value).setNumberOfProcesses(num);
                    }
                } catch (NullPointerException e) {
                    new GFacProviderException("No Value sent in WorkflowContextHeader for Node Count, value in the Deployment Descriptor will be used",e);
                }
                try {
                    int nodeCount = computionResource.getNodeCount();
                    if(nodeCount>0){
                    	appDepType.setNodeCount(nodeCount);
                    }
                } catch (NullPointerException e) {
                     new GFacProviderException("No Value sent in WorkflowContextHeader for Node Count, value in the Deployment Descriptor will be used",e);
                }
                try {
                    String queueName = computionResource.getQueueName();
                    if (queueName != null) {
                        if(appDepType.getQueue() == null){
                            QueueType queueType = appDepType.addNewQueue();
                            queueType.setQueueName(queueName);
                        }else{
                        	appDepType.getQueue().setQueueName(queueName);
                        }
                    }
                } catch (NullPointerException e) {
                     new GFacProviderException("No Value sent in WorkflowContextHeader for Node Count, value in the Deployment Descriptor will be used",e);
                }
                try {
                    int maxwallTime = computionResource.getWallTimeLimit();
                    if(maxwallTime>0){
                    	appDepType.setMaxWallTime(maxwallTime);
                    }
                } catch (NullPointerException e) {
                     new GFacProviderException("No Value sent in WorkflowContextHeader for Node Count, value in the Deployment Descriptor will be used",e);
                }
        }
		
		if (appDepType.getCpuCount() > 0) {
			RangeValueType rangeType = new RangeValueType();
			rangeType.setLowerBound(Double.NaN);
			rangeType.setUpperBound(Double.NaN);
			rangeType.setExact(appDepType.getCpuCount());
			JSDLUtils.setTotalCPUCountRequirements(value, rangeType);
		}

		if (appDepType.getProcessorsPerNode() > 0) {
			RangeValueType rangeType = new RangeValueType();
			rangeType.setLowerBound(Double.NaN);
			rangeType.setUpperBound(Double.NaN);
			rangeType.setExact(appDepType.getProcessorsPerNode());
			JSDLUtils.setIndividualCPUCountRequirements(value, rangeType);
		}
		
		if (appDepType.getNodeCount() > 0) {
			RangeValueType rangeType = new RangeValueType();
			rangeType.setLowerBound(Double.NaN);
			rangeType.setUpperBound(Double.NaN);
			rangeType.setExact(appDepType.getNodeCount());
			JSDLUtils.setTotalResourceCountRequirements(value, rangeType);
		}
		
		if(appDepType.getMaxWallTime() > 0) {
			RangeValueType cpuTime = new RangeValueType();
			cpuTime.setLowerBound(Double.NaN);
			cpuTime.setUpperBound(Double.NaN);
			long wallTime = appDepType.getMaxWallTime() * 60;
			cpuTime.setExact(wallTime);
			JSDLUtils.setIndividualCPUTimeRequirements(value, cpuTime);
		}
	}
	
	
	private static void createMemory(JobDefinitionType value, HpcApplicationDeploymentType appDepType){
		if (appDepType.getMinMemory() > 0 && appDepType.getMaxMemory() > 0) {
			RangeValueType rangeType = new RangeValueType();
			rangeType.setLowerBound(appDepType.getMinMemory());
			rangeType.setUpperBound(appDepType.getMaxMemory());
			JSDLUtils.setIndividualPhysicalMemoryRequirements(value, rangeType);
		}

		else if (appDepType.getMinMemory() > 0 && appDepType.getMaxMemory() <= 0) {
			// TODO set Wall time
			RangeValueType rangeType = new RangeValueType();
			rangeType.setLowerBound(appDepType.getMinMemory());
			JSDLUtils.setIndividualPhysicalMemoryRequirements(value, rangeType);
		}
		
		else if (appDepType.getMinMemory() <= 0 && appDepType.getMaxMemory() > 0) {
			// TODO set Wall time
			RangeValueType rangeType = new RangeValueType();
			rangeType.setUpperBound(appDepType.getMinMemory());
			JSDLUtils.setIndividualPhysicalMemoryRequirements(value, rangeType);
		}
		
	}

	
	

	
}
