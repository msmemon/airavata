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

package org.apache.airavata.xbaya.invoker;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import org.apache.airavata.common.utils.XMLUtil;
import org.apache.airavata.workflow.model.exceptions.WorkflowException;
import org.apache.airavata.workflow.model.exceptions.WorkflowRuntimeException;
import org.apache.airavata.xbaya.jython.lib.ServiceNotifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.builder.XmlElement;

import xsul.lead.LeadContextHeader;
import xsul.wsif.WSIFMessage;

public class WorkflowInvokerWrapperForGFacInvoker extends GFacInvoker {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowInvokerWrapperForGFacInvoker.class);

    private ServiceNotifiable notifier;

    private String serviceInformation;

    private Future<Boolean> result;

    protected boolean failerSent = false;

    public WorkflowInvokerWrapperForGFacInvoker(QName portTypeQName, String gfacURL, String messageBoxURL,
            LeadContextHeader leadcontext, ServiceNotifiable serviceNotificationSender) {
        super(portTypeQName, gfacURL, messageBoxURL, leadcontext);
        this.notifier = serviceNotificationSender;
        this.serviceInformation = portTypeQName.toString();
    }

    /**
     * @see org.apache.airavata.xbaya.invoker.WorkflowInvoker#invoke()
     */
    public synchronized boolean invoke() throws WorkflowException {

        try {
            WSIFMessage inputMessage = super.getInputs();
            logger.debug("inputMessage: " + XMLUtil.xmlElementToString((XmlElement) inputMessage));
            this.notifier.invokingService(inputMessage);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            this.result = executor.submit(new Callable<Boolean>() {
                @SuppressWarnings("boxing")
                public Boolean call() {
                    try {
                        boolean success = WorkflowInvokerWrapperForGFacInvoker.super.invoke();
                        if (success) {
                            // Send notification
                            WSIFMessage outputMessage = WorkflowInvokerWrapperForGFacInvoker.super.getOutputs();
                            // An implementation of WSIFMessage,
                            // WSIFMessageElement, implements toString(), which
                            // serialize the message XML.
                            logger.debug("outputMessage: " + outputMessage);
                            WorkflowInvokerWrapperForGFacInvoker.this.notifier.serviceFinished(outputMessage);
                        } else {
                            WSIFMessage faultMessage = WorkflowInvokerWrapperForGFacInvoker.super.getFault();
                            // An implementation of WSIFMessage,
                            // WSIFMessageElement, implements toString(), which
                            // serialize the message XML.
                            logger.debug("received fault: " + faultMessage);
                            WorkflowInvokerWrapperForGFacInvoker.this.notifier.receivedFault(faultMessage);
                            WorkflowInvokerWrapperForGFacInvoker.this.failerSent = true;
                        }
                        return success;
                    } catch (WorkflowException e) {
                        logger.error(e.getMessage(), e);
                        // An appropriate message has been set in the exception.
                        WorkflowInvokerWrapperForGFacInvoker.this.notifier.invocationFailed(e.getMessage(), e);
                        WorkflowInvokerWrapperForGFacInvoker.this.failerSent = true;
                        throw new WorkflowRuntimeException(e);
                    } catch (RuntimeException e) {
                        logger.error(e.getMessage(), e);
                        String message = "Error in invoking a service: "
                                + WorkflowInvokerWrapperForGFacInvoker.this.serviceInformation;
                        WorkflowInvokerWrapperForGFacInvoker.this.notifier.invocationFailed(message, e);
                        WorkflowInvokerWrapperForGFacInvoker.this.failerSent = true;
                        throw e;
                    }
                }
            });

            // Kill the thread inside of executor. This is necessary for Jython
            // script to finish.
            executor.shutdown();

            // Let other threads know that job has been submitted.
            notifyAll();

            // Check if the invocation itself fails. This happens immediately.
            try {
                this.result.get(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            } catch (TimeoutException e) {
                // The job is probably running fine.
                // The normal case.
                return true;
            } catch (ExecutionException e) {
                // The service-failed notification should have been sent
                // already.
                logger.error(e.getMessage(), e);
                String message = "Error in invoking a service: " + this.serviceInformation;
                throw new WorkflowException(message, e);
            }
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            String message = "Error in invoking a service: " + this.serviceInformation;
            this.notifier.invocationFailed(message, e);
            throw new WorkflowException(message, e);
        } catch (Error e) {
            logger.error(e.getMessage(), e);
            String message = "Unexpected error: " + this.serviceInformation;
            this.notifier.invocationFailed(message, e);
            throw new WorkflowException(message, e);
        }

        boolean success = super.invoke();
        if (!success) {
            try {
                throw new Exception("Failed invoking GFac");
            } catch (Exception e) {
                notifier.invocationFailed(super.getFault().toString(), e);
            }

        } else {
            notifier.serviceFinished(super.getOutputs());
        }
        return success;
    }

    public synchronized void waitToFinish() throws WorkflowException {
        try {
            while (this.result == null) {
                // The job is not submitted yet.
                try {
                    wait();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            // Wait for the job to finish.
            Boolean success = this.result.get();
            if (success == false) {
                WSIFMessage faultMessage = super.getFault();
                String message = "Error in a service: ";
                // An implementation of WSIFMessage,
                // WSIFMessageElement, implements toString(), which
                // serialize the message XML.
                message += faultMessage.toString();
                throw new WorkflowException(message);
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            // The service-failed notification should have been sent already.
            logger.error(e.getMessage(), e);
            String message = "Error in invoking a service: " + this.serviceInformation;
            throw new WorkflowException(message, e);
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            String message = "Error while waiting for a service to finish: " + this.serviceInformation;
            this.notifier.invocationFailed(message, e);
            throw new WorkflowException(message, e);
        } catch (Error e) {
            logger.error(e.getMessage(), e);
            String message = "Unexpected error: " + this.serviceInformation;
            this.notifier.invocationFailed(message, e);
            throw new WorkflowException(message, e);
        }
    }

    public Object getOutput(String name) throws WorkflowException {
        try {
            waitToFinish();
            Object output = super.getOutput(name);
            if (output instanceof XmlElement) {
                logger.debug("output: " + XMLUtil.xmlElementToString((XmlElement) output));
            }
            return output;
        } catch (WorkflowException e) {
            logger.error(e.getMessage(), e);
            // An appropriate message has been set in the exception.
            if (!this.failerSent) {
                this.notifier.invocationFailed(e.getMessage(), e);
            }
            throw e;
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            String message = "Error while waiting for a output: " + name;
            this.notifier.invocationFailed(message, e);
            throw new WorkflowException(message, e);
        } catch (Error e) {
            logger.error(e.getMessage(), e);
            String message = "Unexpected error: " + this.serviceInformation;
            this.notifier.invocationFailed(message, e);
            throw new WorkflowException(message, e);
        }
    }

}