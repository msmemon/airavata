/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

/*
 * Application Programming Interface definition for Apache Airavata Services.
 *   this parent thrift file is contains all service interfaces. The data models are 
 *   described in respective thrift files.
*/

include "airavataErrors.thrift"
include "airavataDataModel.thrift"
include "experimentModel.thrift"
include "workspaceModel.thrift"

namespace java org.apache.airavata.api
namespace php Airavata.API
namespace cpp airavata.api
namespace perl AiravataAPI
namespace py airavata.api
namespace js AiravataAPI

/*
 * Airavata Interface Versions depend upon this Thrift Interface File. When Making changes, please edit the
 *  Version Constants according to Semantic Versioning Specification (SemVer) http://semver.org.
 *
 * Note: The Airavata API version may be different from the Airavata software release versions.
 *
 * The Airavata API version is composed as a dot delimited string with major, minor, and patch level components.
 *
 *  - Major: Incremented for backward incompatible changes. An example would be changes to interfaces.
 *  - Minor: Incremented for backward compatible changes. An example would be the addition of a new optional methods.
 *  - Patch: Incremented for bug fixes. The patch level should be increased for every edit that doesn't result
 *              in a change to major/minor version numbers.
*/
const string AIRAVATA_API_VERSION = "0.12.0"

service Airavata {

/*
 * Apache Airavata API Service Methods. For data structures associated in the signatures, please see included thrift files
*/

  /** Query Airavata to fetch the API version */
  string GetAPIVersion(),
  
  /**
   * Create a Project
   *
  */
  string createProject (1: required workspaceModel.Project project,
                        2: required string userName)
      throws (1: airavataErrors.InvalidRequestException ire,
              2: airavataErrors.AiravataClientException ace,
              3: airavataErrors.AiravataSystemException ase)

/**
   * Update a Project
   *
  */
  void updateProject (1: required workspaceModel.Project project)
      throws (1: airavataErrors.InvalidRequestException ire,
              2: airavataErrors.AiravataClientException ace,
              3: airavataErrors.AiravataSystemException ase)

/**
   * Get a Project by ID
   *
  */
  workspaceModel.Project getProject (1: required string projectId)
        throws (1: airavataErrors.InvalidRequestException ire,
                2: airavataErrors.AiravataClientException ace,
                3: airavataErrors.AiravataSystemException ase)

/**
   * Get all Project by user
   *
  */
  list<workspaceModel.Project> getAllUserProjects (1: required string userName)
        throws (1: airavataErrors.InvalidRequestException ire,
                2: airavataErrors.AiravataClientException ace,
                3: airavataErrors.AiravataSystemException ase)

  /**
     * Get all Experiments within a Project
     *
  */
  list<experimentModel.Experiment> getAllExperimentsInProject(1: required string projectId)
          throws (1: airavataErrors.InvalidRequestException ire,
                  2: airavataErrors.AiravataClientException ace,
                  3: airavataErrors.AiravataSystemException ase)

  /**
     * Get all Experiments by user
     *
  */
  list<experimentModel.Experiment> getAllUserExperiments(1: required string userName)
            throws (1: airavataErrors.InvalidRequestException ire,
                    2: airavataErrors.AiravataClientException ace,
                    3: airavataErrors.AiravataSystemException ase)

  /**
     * Create an experiment for the specified user belonging to the gateway. The gateway identity is not explicitly passed
     *   but inferred from the authentication header. This experiment is just a persistent place holder. The client
     *   has to subsequently configure and launch the created experiment. No action is taken on Airavata Server except
     *   registering the experiment in a persistent store.
     *
     * @param basicExperimentMetadata
     *    The create experiment will require the basic experiment metadata like the name and description, intended user,
     *      the gateway identifer and if the experiment should be shared public by defualt. During the creation of an experiment
     *      the ExperimentMetadata is a required field.
     *
     * @return
     *   The server-side generated airavata experiment globally unique identifier.
     *
     * @throws org.apache.airavata.api.error.InvalidRequestException
     *    For any incorrect forming of the request itself.
     *
     * @throws org.apache.airavata.api.error.AiravataClientException
     *    The following list of exceptions are thrown which Airavata Client can take corrective actions to resolve:
     *
     *      UNKNOWN_GATEWAY_ID - If a Gateway is not registered with Airavata as a one time administrative
     *         step, then Airavata Registry will not have a provenance area setup. The client has to follow
     *         gateway registration steps and retry this request.
     *
     *      AUTHENTICATION_FAILURE - How Authentication will be implemented is yet to be determined.
     *         For now this is a place holder.
     *
     *      INVALID_AUTHORIZATION - This will throw an authorization exception. When a more robust security hand-shake
     *         is implemented, the authorization will be more substantial.
     *
     * @throws org.apache.airavata.api.error.AiravataSystemException
     *    This exception will be thrown for any Airavata Server side issues and if the problem cannot be corrected by the client
     *       rather an Airavata Administrator will be notified to take corrective action.
     *
    */

  string createExperiment(1: required experimentModel.Experiment experiment)
    throws (1: airavataErrors.InvalidRequestException ire,
            2: airavataErrors.AiravataClientException ace,
            3: airavataErrors.AiravataSystemException ase)

  /**
   * Fetch previously created experiment metadata.
   *
   * @param airavataExperimentId
   *    The identifier for the requested experiment. This is returned during the create experiment step.
   *
   * @return experimentMetada
   *   This method will return the previously stored experiment metadata.
   *
   * @throws org.apache.airavata.api.error.InvalidRequestException
   *    For any incorrect forming of the request itself.
   * 
   * @throws org.apache.airavata.api.error.ExperimentNotFoundException
   *    If the specified experiment is not previously created, then an Experiment Not Found Exception is thrown.
   * 
   * @throws org.apache.airavata.api.error.AiravataClientException
   *    The following list of exceptions are thrown which Airavata Client can take corrective actions to resolve:
   *      
   *      UNKNOWN_GATEWAY_ID - If a Gateway is not registered with Airavata as a one time administrative
   *         step, then Airavata Registry will not have a provenance area setup. The client has to follow
   *         gateway registration steps and retry this request.
   *
   *      AUTHENTICATION_FAILURE - How Authentication will be implemented is yet to be determined.
   *         For now this is a place holder.
   *
   *      INVALID_AUTHORIZATION - This will throw an authorization exception. When a more robust security hand-shake
   *         is implemented, the authorization will be more substantial.
   *
   * @throws org.apache.airavata.api.error.AiravataSystemException
   *    This exception will be thrown for any Airavata Server side issues and if the problem cannot be corrected by the client
   *       rather an Airavata Administrator will be notified to take corrective action.
   *
  */
  experimentModel.Experiment getExperiment(1: required string airavataExperimentId)
    throws (1: airavataErrors.InvalidRequestException ire,
            2: airavataErrors.ExperimentNotFoundException enf,
            3: airavataErrors.AiravataClientException ace,
            4: airavataErrors.AiravataSystemException ase)

  /**
   * Configure a previously created experiment with required inputs, scheduling and other quality of service
   *   parameters. This method only updates the experiment object within the registry. The experiment has to be launched
   *   to make it actionable by the server.
   *
   * @param airavataExperimentId
   *    The identifier for the requested experiment. This is returned during the create experiment step.
   *
   * @param experimentConfigurationData
   *    The configuration information of the experiment with application input parameters, computational resource scheduling
   *      information, special input output handling and additional quality of service parameters.
   *
   * @return
   *   This method call does not have a return value.
   *
   * @throws org.apache.airavata.api.error.InvalidRequestException
   *    For any incorrect forming of the request itself.
   * 
   * @throws org.apache.airavata.api.error.ExperimentNotFoundException
   *    If the specified experiment is not previously created, then an Experiment Not Found Exception is thrown.
   * 
   * @throws org.apache.airavata.api.error.AiravataClientException
   *    The following list of exceptions are thrown which Airavata Client can take corrective actions to resolve:
   *      
   *      UNKNOWN_GATEWAY_ID - If a Gateway is not registered with Airavata as a one time administrative
   *         step, then Airavata Registry will not have a provenance area setup. The client has to follow
   *         gateway registration steps and retry this request.
   *
   *      AUTHENTICATION_FAILURE - How Authentication will be implemented is yet to be determined.
   *         For now this is a place holder.
   *
   *      INVALID_AUTHORIZATION - This will throw an authorization exception. When a more robust security hand-shake
   *         is implemented, the authorization will be more substantial.
   *
   * @throws org.apache.airavata.api.error.AiravataSystemException
   *    This exception will be thrown for any Airavata Server side issues and if the problem cannot be corrected by the client
   *       rather an Airavata Administrator will be notified to take corrective action.
   *
  */
  void updateExperiment(1: required string airavataExperimentId,
                           2: required experimentModel.Experiment experiment)
    throws (1: airavataErrors.InvalidRequestException ire,
            2: airavataErrors.ExperimentNotFoundException enf,
            3: airavataErrors.AiravataClientException ace,
            4: airavataErrors.AiravataSystemException ase)

  void updateExperimentConfiguration(1: required string airavataExperimentId,
                                       2: required experimentModel.UserConfigurationData userConfiguration)

  void updateResourceScheduleing(1: required string airavataExperimentId,
                                 2: required experimentModel.ComputationalResourceScheduling resourceScheduling)

  /**
   * Launch a previously created and configured experiment. Airavata Server will then start processing the request and appropriate
   *   notifications and intermediate and output data will be subsequently available for this experiment.
   *
   * @param airavataExperimentId
   *    The identifier for the requested experiment. This is returned during the create experiment step.
   *
   * @param airavataCredStoreToken:
   *   A requirement to execute experiments within Airavata is to first register the targeted remote computational account
   *     credentials with Airavata Credential Store. The administrative API (related to credential store) will return a
   *     generated token associated with the registered credentials. The client has to security posses this token id and is
   *     required to pass it to Airavata Server for all execution requests.
   *   Note: At this point only the credential store token is required so the string is directly passed here. In future if 
   *     if more security credentials are enables, then the structure ExecutionSecurityParameters should be used.
   *   Note: This parameter is not persisted within Airavata Registry for security reasons.
   *
   * @return
   *   This method call does not have a return value.
   *
   * @throws org.apache.airavata.api.error.InvalidRequestException
   *    For any incorrect forming of the request itself.
   * 
   * @throws org.apache.airavata.api.error.ExperimentNotFoundException
   *    If the specified experiment is not previously created, then an Experiment Not Found Exception is thrown.
   * 
   * @throws org.apache.airavata.api.error.AiravataClientException
   *    The following list of exceptions are thrown which Airavata Client can take corrective actions to resolve:
   *      
   *      UNKNOWN_GATEWAY_ID - If a Gateway is not registered with Airavata as a one time administrative
   *         step, then Airavata Registry will not have a provenance area setup. The client has to follow
   *         gateway registration steps and retry this request.
   *
   *      AUTHENTICATION_FAILURE - How Authentication will be implemented is yet to be determined.
   *         For now this is a place holder.
   *
   *      INVALID_AUTHORIZATION - This will throw an authorization exception. When a more robust security hand-shake
   *         is implemented, the authorization will be more substantial.
   *
   * @throws org.apache.airavata.api.error.AiravataSystemException
   *    This exception will be thrown for any Airavata Server side issues and if the problem cannot be corrected by the client
   *       rather an Airavata Administrator will be notified to take corrective action.
   *
  */
  void launchExperiment(1: required string airavataExperimentId
                                  2: required string airavataCredStoreToken)
    throws (1: airavataErrors.InvalidRequestException ire,
            2: airavataErrors.ExperimentNotFoundException enf,
            3: airavataErrors.AiravataClientException ace,
            4: airavataErrors.AiravataSystemException ase)


    experimentModel.ExperimentStatus getExperimentStatus(1: required string airavataExperimentId)
      throws (1: airavataErrors.InvalidRequestException ire,
              2: airavataErrors.ExperimentNotFoundException enf,
              3: airavataErrors.AiravataClientException ace,
              4: airavataErrors.AiravataSystemException ase)

  list<experimentModel.DataObjectType> getExperimentOutputs (1: required string airavataExperimentId)

  map<string, experimentModel.JobStatus> getJobStatuses(1: required string airavataExperimentId)


  /**
   * Configure and Launch a previously created experiment with required inputs, scheduling, security and other quality of service
   *   parameters. This method also launches the experiment after it is configured. If you would like to configure only 
   *   and launch at a later time or partially configure then ConfigureExperiment should be used.
   *
   * @param airavataExperimentId
   *    The identifier for the requested experiment. This is returned during the create experiment step.
   * 
   * @param experimentConfigurationData
   *    The configuration information of the experiment with application input parameters, computational resource scheduling
   *      information, special input output handling and additional quality of service parameters.
   *
   * @param airavataCredStoreToken:
   *   A requirement to execute experiments within Airavata is to first register the targeted remote computational account
   *     credentials with Airavata Credential Store. The administrative API (related to credential store) will return a
   *     generated token associated with the registered credentials. The client has to security posses this token id and is
   *     required to pass it to Airavata Server for all execution requests.
   *   Note: At this point only the credential store token is required so the string is directly passed here. In future if 
   *     if more security credentials are enables, then the structure ExecutionSecurityParameters should be used.
   *
   * @return
   *   The server-side generated experiment GUID.
   *
   * @throws org.apache.airavata.api.error.InvalidRequestException
   *    For any incorrect forming of the request itself.
   * 
   * @throws org.apache.airavata.api.error.AiravataClientException
   *    The following list of exceptions are thrown which Airavata Client can take corrective actions to resolve:
   *      
   *      UNKNOWN_GATEWAY_ID - If a Gateway is not registered with Airavata as a one time administrative
   *         step, then Airavata Registry will not have a provenance area setup. The client has to follow
   *         gateway registration steps and retry this request.
   *
   *      AUTHENTICATION_FAILURE - How Authentication will be implemented is yet to be determined.
   *         For now this is a place holder.
   *
   *      INVALID_AUTHORIZATION - This will throw an authorization exception. When a more robust security hand-shake
   *         is implemented, the authorization will be more substantial.
   *
   * @throws org.apache.airavata.api.error.AiravataSystemException
   *    This exception will be thrown for any Airavata Server side issues and if the problem cannot be corrected by the client
   *       rather an Airavata Administrator will be notified to take corrective action.
   *
  */
//  string updateAndLaunchExperiment (1: string airavataExperimentId
//                                       2: experimentModel.Experiment experiment,
//                                       3: string airavataCredStoreToken)
//    throws (1: airavataErrors.InvalidRequestException ire,
//            2: airavataErrors.ExperimentNotFoundException enf,
//            3: airavataErrors.AiravataClientException ace,
//            4: airavataErrors.AiravataSystemException ase)

  /**
   * Clone an specified experiment with a new name. A copy of the experiment configuration is made and is persisted with new metadata.
   *   The client has to subsequently update this configuration if needed and launch the cloned experiment. 
   *
   * @param airavataExperimentIdToBeCloned
   *    This is the experiment identifier that is to be cloned.
   *
   * @param basicExperimentMetadata
   *    Once an experiment is cloned, to disambiguate, the users are suggested to provide new metadata. This will again require
   *      the basic experiment metadata like the name and description, intended user, the gateway identifier and if the experiment
   *      should be shared public by default.
   *
   * @return
   *   The server-side generated airavata experiment globally unique identifier for the newly cloned experiment.
   *
   * @throws org.apache.airavata.api.error.InvalidRequestException
   *    For any incorrect forming of the request itself.
   * 
   * @throws org.apache.airavata.api.error.ExperimentNotFoundException
   *    If the specified experiment is not previously created, then an Experiment Not Found Exception is thrown.
   * 
   * @throws org.apache.airavata.api.error.AiravataClientException
   *    The following list of exceptions are thrown which Airavata Client can take corrective actions to resolve:
   *      
   *      UNKNOWN_GATEWAY_ID - If a Gateway is not registered with Airavata as a one time administrative
   *         step, then Airavata Registry will not have a provenance area setup. The client has to follow
   *         gateway registration steps and retry this request.
   *
   *      AUTHENTICATION_FAILURE - How Authentication will be implemented is yet to be determined.
   *         For now this is a place holder.
   *
   *      INVALID_AUTHORIZATION - This will throw an authorization exception. When a more robust security hand-shake
   *         is implemented, the authorization will be more substantial.
   *
   * @throws org.apache.airavata.api.error.AiravataSystemException
   *    This exception will be thrown for any Airavata Server side issues and if the problem cannot be corrected by the client
   *       rather an Airavata Administrator will be notified to take corrective action.
   *
  */
  string cloneExperiment(1: string airavataExperimentIdToBeCloned,
                                      2: experimentModel.Experiment updatedExperiment)
    throws (1: airavataErrors.InvalidRequestException ire,
            2: airavataErrors.ExperimentNotFoundException enf,
            3: airavataErrors.AiravataClientException ace,
            4: airavataErrors.AiravataSystemException ase)

  /**
   * Terminate a running experiment.
   *
   * @param airavataExperimentId
   *    The identifier for the requested experiment. This is returned during the create experiment step.
   *
   * @return
   *   This method call does not have a return value.
   *
   * @throws org.apache.airavata.api.error.InvalidRequestException
   *    For any incorrect forming of the request itself.
   * 
   * @throws org.apache.airavata.api.error.ExperimentNotFoundException
   *    If the specified experiment is not previously created, then an Experiment Not Found Exception is thrown.
   * 
   * @throws org.apache.airavata.api.error.AiravataClientException
   *    The following list of exceptions are thrown which Airavata Client can take corrective actions to resolve:
   *      
   *      UNKNOWN_GATEWAY_ID - If a Gateway is not registered with Airavata as a one time administrative
   *         step, then Airavata Registry will not have a provenance area setup. The client has to follow
   *         gateway registration steps and retry this request.
   *
   *      AUTHENTICATION_FAILURE - How Authentication will be implemented is yet to be determined.
   *         For now this is a place holder.
   *
   *      INVALID_AUTHORIZATION - This will throw an authorization exception. When a more robust security hand-shake
   *         is implemented, the authorization will be more substantial.
   *
   * @throws org.apache.airavata.api.error.AiravataSystemException
   *    This exception will be thrown for any Airavata Server side issues and if the problem cannot be corrected by the client
   *       rather an Airavata Administrator will be notified to take corrective action.
   *
  */
  void terminateExperiment(1: string airavataExperimentId)
    throws (1: airavataErrors.InvalidRequestException ire,
            2: airavataErrors.ExperimentNotFoundException enf,
            3: airavataErrors.AiravataClientException ace,
            4: airavataErrors.AiravataSystemException ase)
}
