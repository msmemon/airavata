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

package org.apache.airavata.persistance.registry.jpa.model;

import javax.persistence.*;

@Entity
@Table(name ="")
@IdClass(Experiment_Output_PK.class)
public class Experiment_Output {
    @Id
    @Column(name = "EXPERIMENT_ID")
    private String experiment_id;

    @Id
    @Column(name = "OUTPUT_KEY")
    private String ex_key;
    @Column(name = "VALUE")
    private String value;
    @Column(name = "METADATA")
    private String metadata;
    @Column(name = "OUTPUT_KEY_TYPE")
    private String outputKeyType;

    @ManyToOne
    @JoinColumn(name = "EXPERIMENT_ID")
    private Experiment experiment;

    public String getExperiment_id() {
        return experiment_id;
    }

    public void setExperiment_id(String experiment_id) {
        this.experiment_id = experiment_id;
    }

    public String getEx_key() {
        return ex_key;
    }

    public void setEx_key(String ex_key) {
        this.ex_key = ex_key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getOutputKeyType() {
        return outputKeyType;
    }

    public void setOutputKeyType(String outputKeyType) {
        this.outputKeyType = outputKeyType;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }
}
