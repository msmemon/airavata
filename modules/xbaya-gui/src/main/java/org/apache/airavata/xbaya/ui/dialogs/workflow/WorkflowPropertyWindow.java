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

package org.apache.airavata.xbaya.ui.dialogs.workflow;

import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.airavata.common.utils.StringUtil;
import org.apache.airavata.common.utils.WSConstants;
import org.apache.airavata.common.utils.XMLUtil;
import org.apache.airavata.workflow.model.wf.Workflow;
import org.apache.airavata.xbaya.ui.XBayaGUI;
import org.apache.airavata.xbaya.ui.dialogs.XBayaDialog;
import org.apache.airavata.xbaya.ui.graph.GraphCanvas;
import org.apache.airavata.xbaya.ui.widgets.GridPanel;
import org.apache.airavata.xbaya.ui.widgets.XBayaLabel;
import org.apache.airavata.xbaya.ui.widgets.XBayaTextArea;
import org.apache.airavata.xbaya.ui.widgets.XBayaTextField;
import org.xmlpull.infoset.XmlElement;

public class WorkflowPropertyWindow {

    private XBayaGUI xbayaGUI;

    private XBayaDialog dialog;

    private JButton okButton;

    private Workflow workflow;

    private XBayaTextField nameTextField;

    private XBayaTextField templateIDField;

    private XBayaTextField instanceIDField;

    private XBayaTextArea descriptionTextArea;

    private XBayaTextArea metadataTextArea;

    
    /**
     * @param engine
     */
    public WorkflowPropertyWindow(XBayaGUI xbayaGUI) {
        this.xbayaGUI = xbayaGUI;
        initGui();
    }

    /**
     * Shows the dialog.
     */
    public void show() {
        this.workflow = this.xbayaGUI.getWorkflow();

        String name = this.workflow.getName();
        this.nameTextField.setText(name);

        String description = this.workflow.getDescription();
        this.descriptionTextArea.setText(description);

        URI templateID = this.workflow.getUniqueWorkflowName();
        if (templateID == null) {
            this.templateIDField.setText("");
        } else {
            this.templateIDField.setText(templateID.toString());
        }

        URI instanceID = this.workflow.getGPELInstanceID();
        if (instanceID == null) {
            this.instanceIDField.setText("");
        } else {
            this.instanceIDField.setText(instanceID.toString());
        }

        XmlElement metadata = this.workflow.getMetadata();
        String metadataText;
        if (metadata == null) {
            metadataText = WSConstants.EMPTY_APPINFO;
        } else {
            metadataText = XMLUtil.xmlElementToString(metadata);
        }
        this.metadataTextArea.setText(metadataText);

        this.dialog.show();
    }

    /**
     * Hides the dialog.
     */
    private void hide() {
        this.dialog.hide();
    }

    private boolean isWorkflowNameAlreadyPresent(String name){
    	List<GraphCanvas> graphCanvases = xbayaGUI.getGraphCanvases();
    	for (GraphCanvas graphCanvas : graphCanvases) {
    		if (graphCanvas!=xbayaGUI.getGraphCanvas()){
				String existingName = graphCanvas.getWorkflow().getGraph().getName();
				if (name.equals(existingName)){
					return true;
				}
    		}
		}
    	return false;
    }
    
    private void setToWorkflow() {
        String name = this.nameTextField.getText();
        if (name != null && name.equals(StringUtil.convertToJavaIdentifier(name)) && (!isWorkflowNameAlreadyPresent(name))) {
            String description = this.descriptionTextArea.getText();
            String metadataText = this.metadataTextArea.getText();

            XmlElement metadata;
            if (metadataText.length() == 0) {
                metadata = null;
            } else {
                try {
                    metadata = XMLUtil.stringToXmlElement(metadataText);
                } catch (RuntimeException e) {
                    String warning = "The metadata is ill-formed.";
                    this.xbayaGUI.getErrorWindow().error(warning, e);
                    return;
                }
            }

            GraphCanvas graphCanvas = this.xbayaGUI.getGraphCanvas();
            graphCanvas.setNameAndDescription(name, description);
            graphCanvas.getWorkflow().setMetadata(metadata);
            hide();
        } else {
            this.nameTextField.setText(StringUtil.convertToJavaIdentifier(name));
            JOptionPane.showMessageDialog(this.xbayaGUI.getFrame(),
                    "Invalid Name or a Workflow under the same name already exists. Please consider the Name suggsted", "Invalid Name", JOptionPane.OK_OPTION);
        }
    }

    private void initGui() {
        this.nameTextField = new XBayaTextField();
        XBayaLabel nameLabel = new XBayaLabel("Name", this.nameTextField);

        this.templateIDField = new XBayaTextField();
        this.templateIDField.setEditable(false);
        XBayaLabel templateIDLabel = new XBayaLabel("Template ID", this.templateIDField);

        this.instanceIDField = new XBayaTextField();
        this.instanceIDField.setEditable(false);
        XBayaLabel instanceIDLabel = new XBayaLabel("Instance ID", this.instanceIDField);

        this.descriptionTextArea = new XBayaTextArea();
        XBayaLabel descriptionLabel = new XBayaLabel("Description", this.descriptionTextArea);

        this.metadataTextArea = new XBayaTextArea();
        XBayaLabel metadataLabel = new XBayaLabel("Metadata", this.metadataTextArea);

        GridPanel mainPanel = new GridPanel();
        mainPanel.add(nameLabel);
        mainPanel.add(this.nameTextField);
        mainPanel.add(templateIDLabel);
        mainPanel.add(this.templateIDField);
        mainPanel.add(instanceIDLabel);
        mainPanel.add(this.instanceIDField);
        mainPanel.add(descriptionLabel);
        mainPanel.add(this.descriptionTextArea);
        mainPanel.add(metadataLabel);
        mainPanel.add(this.metadataTextArea);
        mainPanel.layout(new double[] { 0, 0, 0, 0.5, 0.5 }, new double[] { 0, 1 });

        this.okButton = new JButton("OK");
        this.okButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setToWorkflow();

            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(this.okButton);
        buttonPanel.add(cancelButton);

        this.dialog = new XBayaDialog(this.xbayaGUI, "Workflow Properties", mainPanel, buttonPanel);
        this.dialog.setDefaultButton(this.okButton);
    }
}