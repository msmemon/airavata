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
package org.apache.airavata.gfac.scheduler.impl;

import org.apache.airavata.commons.gfac.type.HostDescription;
import org.apache.airavata.gfac.scheduler.HostScheduler;

import java.util.List;

public class SimpleHostScheduler implements HostScheduler{
    public HostDescription schedule(List<HostDescription> registeredHosts) {
         //todo implement an algorithm to pick a host among different hosts, ideally this could be configurable in an xml
        return registeredHosts.get(0);
    }
}
