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
package org.apache.airavata.job.monitor;

import com.google.common.eventbus.EventBus;
import org.apache.airavata.commons.gfac.type.HostDescription;
import org.apache.airavata.gsi.ssh.api.Cluster;
import org.apache.airavata.gsi.ssh.api.SSHApiException;
import org.apache.airavata.gsi.ssh.api.ServerInfo;
import org.apache.airavata.gsi.ssh.api.authentication.GSIAuthenticationInfo;
import org.apache.airavata.gsi.ssh.api.job.JobDescriptor;
import org.apache.airavata.gsi.ssh.impl.PBSCluster;
import org.apache.airavata.gsi.ssh.impl.authentication.MyProxyAuthenticationInfo;
import org.apache.airavata.job.monitor.event.MonitorPublisher;
import org.apache.airavata.job.monitor.exception.AiravataMonitorException;
import org.apache.airavata.job.monitor.impl.push.amqp.AMQPMonitor;
import org.apache.airavata.schemas.gfac.GsisshHostType;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AMQPMonitorTest {
    private String myProxyUserName;
    private String myProxyPassword;
    private String certificateLocation;
    private String workingDirectory;
    private HostDescription hostDescription;
    private BlockingQueue<MonitorID> amqpQueue;
    private BlockingQueue<MonitorID> finishQueue;
    private AMQPMonitor amqpMonitor;
    @Before
    public void setUp() throws Exception {
        System.setProperty("myproxy.user", "ogce");
        System.setProperty("myproxy.password", "");
        System.setProperty("basedir", "/Users/lahirugunathilake/work/airavata/sandbox/gsissh");
        System.setProperty("gsi.working.directory", "/home/ogce");
        myProxyUserName = System.getProperty("myproxy.user");
        myProxyPassword = System.getProperty("myproxy.password");
        workingDirectory = System.getProperty("gsi.working.directory");
        String pomDirectory = System.getProperty("basedir");
        certificateLocation = "/Users/lahirugunathilake/Downloads/certificates";
        if (myProxyUserName == null || myProxyPassword == null || workingDirectory == null) {
            System.out.println(">>>>>> Please run tests with my proxy user name and password. " +
                    "E.g :- mvn clean install -Dmyproxy.user=xxx -Dmyproxy.password=xxx -Dgsi.working.directory=/path<<<<<<<");
            throw new Exception("Need my proxy user name password to run tests.");
        }
        amqpQueue = new LinkedBlockingQueue<MonitorID>();
        finishQueue = new LinkedBlockingQueue<MonitorID>();
        amqpMonitor = new
                AMQPMonitor(new MonitorPublisher(new EventBus()),
                amqpQueue,finishQueue,"/Users/lahirugunathilake/Downloads/x509up_u503876","xsede_private",
                Arrays.asList("info1.dyn.teragrid.org,info2.dyn.teragrid.org".split(",")));
        hostDescription = new HostDescription(GsisshHostType.type);
        hostDescription.getType().setHostAddress("gordon.sdsc.xsede.org");
        hostDescription.getType().setHostName("gsissh-gordon");
    }

    @Test
    public void testAMQPMonitor() throws SSHApiException {
        /* now have to submit a job to some machine and add that job to the queue */
        //Create authentication
        GSIAuthenticationInfo authenticationInfo
                = new MyProxyAuthenticationInfo(myProxyUserName, myProxyPassword, "myproxy.teragrid.org",
                7512, 17280000, certificateLocation);

        // Server info
        ServerInfo serverInfo = new ServerInfo("ogce", "trestles.sdsc.edu");


        Cluster pbsCluster = new
                PBSCluster(serverInfo, authenticationInfo, org.apache.airavata.gsi.ssh.util.CommonUtils.getPBSJobManager("/opt/torque/bin/"));


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
        System.out.println(jobID);
        try {
             amqpMonitor.registerListener(new MonitorID(hostDescription,jobID,null,null,myProxyUserName));
        } catch (AiravataMonitorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
