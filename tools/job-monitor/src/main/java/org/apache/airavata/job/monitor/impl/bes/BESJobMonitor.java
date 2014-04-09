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
package org.apache.airavata.job.monitor.impl.bes;

import java.util.concurrent.BlockingQueue;

import org.apache.airavata.common.utils.ServerSettings;
import org.apache.airavata.job.monitor.UserMonitorData;
import org.apache.airavata.job.monitor.core.PullMonitor;
import org.apache.airavata.job.monitor.event.MonitorPublisher;
import org.apache.airavata.job.monitor.exception.AiravataMonitorException;
import org.apache.airavata.job.monitor.impl.pull.qstat.QstatMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* This monitor can be used to monitor a job which runs locally,
* Since its a local job job doesn't have states, once it get executed
* then the job starts running
*/
public class BESJobMonitor extends PullMonitor {
	
   private final static Logger logger = LoggerFactory.getLogger(QstatMonitor.class);

   private BlockingQueue<UserMonitorData> queue;

   private boolean startPulling = false;

   private MonitorPublisher publisher;


   public void run() {

	   this.startPulling = true;
       while (this.startPulling && !ServerSettings.isStopAllThreads()) {
           try {
               startPulling();
               // After finishing one iteration of the full queue this thread sleeps 1 second
               Thread.sleep(10000);
           } catch (Exception e){
               // we catch all the exceptions here because no matter what happens we do not stop running this
               // thread, but ideally we should report proper error messages, but this is handled in startPulling
               // method, incase something happen in Thread.sleep we handle it with this catch block.
               e.printStackTrace();
               logger.error(e.getMessage());
           }
       }
       // While Loop
   }


@Override
public boolean startPulling() throws AiravataMonitorException {
	// TODO Auto-generated method stub
	return false;
}


@Override
public boolean stopPulling() throws AiravataMonitorException {
	// TODO Auto-generated method stub
	return false;
}


public BlockingQueue<UserMonitorData> getQueue() {
	return queue;
}


public void setQueue(BlockingQueue<UserMonitorData> queue) {
	this.queue = queue;
}
}
