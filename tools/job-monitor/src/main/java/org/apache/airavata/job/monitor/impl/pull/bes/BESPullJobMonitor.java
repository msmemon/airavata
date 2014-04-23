package org.apache.airavata.job.monitor.impl.pull.bes;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.airavata.common.utils.ServerSettings;
import org.apache.airavata.gsi.ssh.api.SSHApiException;
import org.apache.airavata.job.monitor.HostMonitorData;
import org.apache.airavata.job.monitor.MonitorID;
import org.apache.airavata.job.monitor.UserMonitorData;
import org.apache.airavata.job.monitor.core.PullMonitor;
import org.apache.airavata.job.monitor.event.MonitorPublisher;
import org.apache.airavata.job.monitor.exception.AiravataMonitorException;
import org.apache.airavata.job.monitor.state.JobStatus;
import org.apache.airavata.job.monitor.util.CommonUtils;
import org.apache.airavata.model.workspace.experiment.JobState;
import org.apache.airavata.schemas.gfac.UnicoreHostType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* This monitor can be used to monitor a job which runs locally,
* Since its a local job job doesn't have states, once it get executed
* then the job starts running
*/
public class BESPullJobMonitor extends PullMonitor {
	
   private final static Logger logger = LoggerFactory.getLogger(BESPullJobMonitor.class);

   private BlockingQueue<UserMonitorData> userMonitorQueue;

   private boolean startPulling = false;

   private MonitorPublisher publisher;
   
   private Map<String, BESStatusChecker> connections;

   
   public BESPullJobMonitor() {
	   connections = new HashMap<String, BESStatusChecker>();
   }
   
   public BESPullJobMonitor(BlockingQueue<UserMonitorData> queue, MonitorPublisher publisher) {
	   this();
       this.userMonitorQueue = queue;
       this.publisher = publisher;
   }
   
   public void run() {
	   this.startPulling = true;
       while (this.startPulling && !ServerSettings.isStopAllThreads()) {
           try {
               startPulling();
               // After finishing one iteration of the full queue this thread sleeps 1 second
               Thread.sleep(3000);
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
    UserMonitorData take = null;
    JobStatus jobStatus = new JobStatus();
    MonitorID currentMonitorID = null;
    try {
        take = userMonitorQueue.take();
        List<MonitorID> completedJobs = new ArrayList<MonitorID>();
        List<HostMonitorData> hostMonitorData = take.getHostMonitorData();
        for (HostMonitorData iHostMonitorData : hostMonitorData) {
            if (iHostMonitorData.getHost().getType() instanceof UnicoreHostType) {
                UnicoreHostType unicoreHostType = (UnicoreHostType) iHostMonitorData.getHost().getType();
                String hostName = unicoreHostType.getUnicoreBESEndPointArray()[0];
//                ResourceConnection connection = null;
                BESStatusChecker connection = null;
                if (connections.containsKey(hostName)) {
                    logger.debug("We already have the BES connection so not going to create one");
                    connection = connections.get(hostName);
                } else {
//                    connection = new ResourceConnection(take.getUserName(), iHostMonitorData, null);
                	connection = new BESStatusChecker(iHostMonitorData);
                    connections.put(hostName, connection);
                }
                
                List<MonitorID> monitorID = iHostMonitorData.getMonitorIDs();
                Map<String, JobState> jobStatuses = connection.getJobStatuses(take.getUserName(), monitorID);
                System.out.println("printing job statuses");
                for (MonitorID iMonitorID : monitorID) {
                    currentMonitorID = iMonitorID;
                    iMonitorID.setStatus(jobStatuses.get(iMonitorID.getJobID()));
                    jobStatus.setMonitorID(iMonitorID);
                    jobStatus.setState(iMonitorID.getStatus());
                    // we have this JobStatus class to handle amqp monitoring

                    publisher.publish(jobStatus);
                    // if the job is completed we do not have to put the job to the queue again
                    iMonitorID.setLastMonitored(new Timestamp((new Date()).getTime()));

                    // After successful monitoring perform following actions to cleanup the queue, if necessary
                    if (jobStatus.getState().equals(JobState.COMPLETE)) {
                        completedJobs.add(iMonitorID);
                    } else if (iMonitorID.getFailedCount() > 2 && iMonitorID.getStatus().equals(JobState.UNKNOWN)) {
                        logger.error("Tried to monitor the job with ID " + iMonitorID.getJobID() + " But failed 3 times, so skip this Job from Monitor");
                        iMonitorID.setLastMonitored(new Timestamp((new Date()).getTime()));
                        completedJobs.add(iMonitorID);
                    } else {
                        // Evey
                        iMonitorID.setLastMonitored(new Timestamp((new Date()).getTime()));
                        // if the job is complete we remove it from the Map, if any of these maps
                        // get empty this userMonitorData will get delete from the queue
                    }
                }
            } else {
                logger.debug("BES Monitor doesn't support non-UNICORE hosts");
            }
        }
        // We have finished all the HostMonitorData object in userMonitorData, now we need to put it back
        // now the userMonitorData goes back to the tail of the queue
        userMonitorQueue.put(take);
        // cleaning up the completed jobs, this method will remove some of the userMonitorData from the queue if
        // they become empty
        for(MonitorID completedJob:completedJobs){
            CommonUtils.removeMonitorFromQueue(userMonitorQueue,completedJob);
        }
    } catch (InterruptedException e) {
        if (!this.userMonitorQueue.contains(take)) {
            try {
                this.userMonitorQueue.put(take);
            } catch (InterruptedException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        logger.error("Error handling the job with Job ID:" + currentMonitorID.getJobID());
        throw new AiravataMonitorException(e);
    } catch (Exception e) {
        if (currentMonitorID.getFailedCount() < 3) {
            try {
                currentMonitorID.setFailedCount(currentMonitorID.getFailedCount() + 1);
                this.userMonitorQueue.put(take);
                // if we get a wrong status we wait for a while and request again
                Thread.sleep(10000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        } else {
            logger.error(e.getMessage());
            logger.error("Tryied to monitor the job 3 times, so dropping of the the Job with ID: " + currentMonitorID.getJobID());
        }
        throw new AiravataMonitorException("Error retrieving the job status", e);
    }
    return true;
}


@Override
public boolean stopPulling() throws AiravataMonitorException {
	return false;
}




public BlockingQueue<UserMonitorData> getQueue() {
	return userMonitorQueue;
}


public void setQueue(BlockingQueue<UserMonitorData> queue) {
	this.userMonitorQueue = queue;
}
}
