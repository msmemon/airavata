/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of4 the License at
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
package org.apache.airavata.workflow.model.graph.system;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.airavata.common.utils.WSConstants;
import org.apache.airavata.workflow.model.component.Component;
import org.apache.airavata.workflow.model.component.system.DifferedInputComponent;
import org.apache.airavata.workflow.model.component.system.InputComponent;
import org.apache.airavata.workflow.model.component.ws.WSComponentPort;
import org.apache.airavata.workflow.model.exceptions.WorkflowRuntimeException;
import org.apache.airavata.workflow.model.graph.DataEdge;
import org.apache.airavata.workflow.model.graph.DataPort;
import org.apache.airavata.workflow.model.graph.Edge;
import org.apache.airavata.workflow.model.graph.Graph;
import org.apache.airavata.workflow.model.graph.GraphException;
import org.apache.airavata.workflow.model.graph.GraphSchema;
import org.apache.airavata.workflow.model.graph.Port;
import org.apache.airavata.workflow.model.graph.ws.WSPort;
import org.xmlpull.infoset.XmlElement;

import xsul5.MLogger;

public class DifferedInputNode extends ParameterNode {

	private static final String VALUE_TAG_NAME = "value";

    private static final MLogger logger = MLogger.getLogger();

    private Object defaultValue;

    /**
     * Creates an InputNode.
     * 
     * @param graph
     */
    public DifferedInputNode(Graph graph) {
        super(graph);
    }

    /**
     * Constructs an InputNode.
     * 
     * @param nodeElement
     * @throws GraphException
     */
    public DifferedInputNode(XmlElement nodeElement) throws GraphException {
        super(nodeElement);
    }

    /**
     * @see edu.indiana.extreme.xbaya.graph.impl.NodeImpl#getComponent()
     */
    @Override
    public Component getComponent() {
        Component component = super.getComponent();
        if (component == null) {
            // The component is null when read from the graph XML.
            component = new DifferedInputComponent();
            setComponent(component);
        }
        return component;
    }

    /**
     * Returns the type of the parameter
     * 
     * @return The type of the parameter (e.g. string, int)
     */
    @Override
    public QName getParameterType() {
        List<DataEdge> edges = getEdges();
        QName parameterType = super.getParameterType();
        if (parameterType == null && getEdges().size() > 0) {
            // This happens when the graph XML doesn't have parameterType.
            DataEdge edge = edges.get(0);
            DataPort toPort = edge.getToPort();
            parameterType = toPort.getType();
        }
        return parameterType;
    }

    /**
     * Returns the default value.
     * 
     * @return The defaultValue.
     */
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * Sets the default value.
     * 
     * @param defaultValue
     *            The default value to set.
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the port of this InputNode.
     * 
     * Note that an InputNode always has only one output port.
     * 
     * @return The port
     */
    @Override
    public SystemDataPort getPort() {
        return (SystemDataPort) getOutputPorts().get(0);
    }

    /**
     * Returns the first port that this input node is connected to.
     * 
     * @return The first port that this input node is connected to
     */
    @Override
    public Port getConnectedPort() {
        return getPort().getEdge(0).getToPort();
    }

    /**
     * Checks if the user input is valid.
     * 
     * @param input
     *            The user input
     * @return true if the user input is valid against the parameter type; false
     *         otherwise
     */
    public boolean isInputValid(String input) {
        logger.entering(new Object[] { input });
        // TODO type checks
        return true;
    }

    /**
     * Called whan an Edge was added to the parameter port. Change the name of
     * this node.
     * 
     * @throws GraphException
     * 
     * @see edu.indiana.extreme.xbaya.graph.impl.NodeImpl#edgeWasAdded(edu.indiana.extreme.xbaya.graph.impl.EdgeImpl)
     */
    @Override
    protected void edgeWasAdded(Edge edge) throws GraphException {
        super.edgeWasAdded(edge);

        // TODO organize this.
        if (edge instanceof DataEdge) {
            DataEdge dataEdge = (DataEdge) edge;
            DataPort toPort = dataEdge.getToPort();
            QName toType = toPort.getType();

            List<DataEdge> edges = getEdges();
            if (edges.size() == 1) {
                // The first edge.
                setParameterType(toType);

                if (!isConfigured() && toPort instanceof WSPort) {
                    // Copy
                    copyDefaultConfiguration((WSPort) toPort);
                }
            } else if (edges.size() > 1) {
                // Not the first edge.
                QName parameterType = getParameterType();
                if (!toType.equals(WSConstants.XSD_ANY_TYPE)
                        && !parameterType.equals(toType)) {
                    throw new GraphException(
                            "Cannot connect ports with different types.");
                }

            } else {
                // Should not happen.
                throw new WorkflowRuntimeException("edges.size(): " + edges.size());
            }
        }
    }

    /**
     * Called whan an Edge was removed from the parameter port. Change the name
     * of the node.
     * 
     * @see edu.indiana.extreme.xbaya.graph.impl.NodeImpl#edgeWasRemoved(edu.indiana.extreme.xbaya.graph.impl.EdgeImpl)
     */
    @Override
    protected void edgeWasRemoved(Edge removedEdge) {
        super.edgeWasRemoved(removedEdge);
        // TODO organize this.
        List<DataEdge> edges = getEdges();
        if (edges.size() == 0) {
            setParameterType(null);

            if (!isConfigured()) {
                // Reset
                setName(getComponent().getName());
                setDescription("");
                setDefaultValue(null);
                setMetadata(null);
            }

        } else {
            Edge edge = edges.get(0);
            Port toPort = edge.getToPort();
            WSPort toWsPort = (WSPort) toPort;
            QName toType = toWsPort.getType();
            setParameterType(toType);

            if (!isConfigured()) {
                // Copy
                copyDefaultConfiguration(toWsPort);
            }
        }
    }

    /**
     * @see edu.indiana.extreme.xbaya.graph.system.SystemNode#portTypeChanged(edu.indiana.extreme.xbaya.graph.system.SystemDataPort)
     */
    @Override
    protected void portTypeChanged(SystemDataPort port) throws GraphException {
        super.portTypeChanged(port);
        setParameterType(port.getType());
    }

    @Override
    protected void parseComponent(XmlElement componentElement) {
        // No need to parse the XML.
        setComponent(new InputComponent());
    }

    @Override
    protected void parseConfiguration(XmlElement configElement) {
        super.parseConfiguration(configElement);
        XmlElement element = configElement.element(null, VALUE_TAG_NAME);
        if (element != null) {
            // It might be a String or XmlElement
            for (Object child : element.children()) {
                if (child instanceof String) {
                    if (((String) child).trim().length() == 0) {
                        // Skip white space before xml element.
                        continue;
                    }
                }
                this.defaultValue = child;
                break;
            }
            // this.defaultValue = element.requiredText();
        }
    }

    @Override
    public XmlElement toXML() {
        XmlElement nodeElement = super.toXML();
        nodeElement.setAttributeValue(GraphSchema.NS,
                GraphSchema.NODE_TYPE_ATTRIBUTE, GraphSchema.NODE_TYPE_DIFFERED_INPUT);
        return nodeElement;
    }

    @Override
    protected XmlElement addConfigurationElement(XmlElement nodeElement) {
        XmlElement configElement = super.addConfigurationElement(nodeElement);
        if (this.defaultValue != null) {
            XmlElement element = configElement.addElement(GraphSchema.NS,
                    VALUE_TAG_NAME);
            element.addChild(this.defaultValue);
        }
        return configElement;
    }

    /**
     * @param toWSPort
     */
    private void copyDefaultConfiguration(WSPort toWSPort) {
        // TODO support recursive search for WSPort in case the input is
        // connected to special nodes.
        setName(toWSPort.getName());
        WSComponentPort componentPort = toWSPort.getComponentPort();
        setDescription(componentPort.getDescription());
        setDefaultValue(componentPort.getDefaultValue());
        setMetadata(componentPort.getAppinfo());
    }
}


/*
 * Indiana University Extreme! Lab Software License, Version 1.2
 * 
 * Copyright (c) 2012 The Trustees of Indiana University. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) All redistributions of source code must retain the above copyright notice,
 * the list of authors in the original source code, this list of conditions and
 * the disclaimer listed in this license;
 * 
 * 2) All redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the disclaimer listed in this license in
 * the documentation and/or other materials provided with the distribution;
 * 
 * 3) Any documentation included with all redistributions must include the
 * following acknowledgement:
 * 
 * "This product includes software developed by the Indiana University Extreme!
 * Lab. For further information please visit http://www.extreme.indiana.edu/"
 * 
 * Alternatively, this acknowledgment may appear in the software itself, and
 * wherever such third-party acknowledgments normally appear.
 * 
 * 4) The name "Indiana University" or "Indiana University Extreme! Lab" shall
 * not be used to endorse or promote products derived from this software without
 * prior written permission from Indiana University. For written permission,
 * please contact http://www.extreme.indiana.edu/.
 * 
 * 5) Products derived from this software may not use "Indiana University" name
 * nor may "Indiana University" appear in their name, without prior written
 * permission of the Indiana University.
 * 
 * Indiana University provides no reassurances that the source code provided
 * does not infringe the patent or any other intellectual property rights of any
 * other entity. Indiana University disclaims any liability to any recipient for
 * claims brought by any other entity based on infringement of intellectual
 * property rights or otherwise.
 * 
 * LICENSEE UNDERSTANDS THAT SOFTWARE IS PROVIDED "AS IS" FOR WHICH NO
 * WARRANTIES AS TO CAPABILITIES OR ACCURACY ARE MADE. INDIANA UNIVERSITY GIVES
 * NO WARRANTIES AND MAKES NO REPRESENTATION THAT SOFTWARE IS FREE OF
 * INFRINGEMENT OF THIRD PARTY PATENT, COPYRIGHT, OR OTHER PROPRIETARY RIGHTS.
 * INDIANA UNIVERSITY MAKES NO WARRANTIES THAT SOFTWARE IS FREE FROM "BUGS",
 * "VIRUSES", "TROJAN HORSES", "TRAP DOORS", "WORMS", OR OTHER HARMFUL CODE.
 * LICENSEE ASSUMES THE ENTIRE RISK AS TO THE PERFORMANCE OF SOFTWARE AND/OR
 * ASSOCIATED MATERIALS, AND TO THE PERFORMANCE AND VALIDITY OF INFORMATION
 * GENERATED USING SOFTWARE.
 */