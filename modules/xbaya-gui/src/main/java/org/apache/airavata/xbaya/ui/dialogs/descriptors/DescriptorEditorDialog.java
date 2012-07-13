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

package org.apache.airavata.xbaya.ui.dialogs.descriptors;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.airavata.common.registry.api.exception.RegistryException;
import org.apache.airavata.common.utils.SwingUtil;
import org.apache.airavata.commons.gfac.type.ApplicationDeploymentDescription;
import org.apache.airavata.commons.gfac.type.HostDescription;
import org.apache.airavata.commons.gfac.type.ServiceDescription;
import org.apache.airavata.registry.api.AiravataRegistry;
import org.apache.airavata.xbaya.XBayaEngine;
import org.apache.airavata.xbaya.registrybrowser.nodes.JCRBrowserIcons;
import org.apache.airavata.xbaya.ui.dialogs.XBayaDialog;
import org.apache.airavata.xbaya.ui.widgets.GridPanel;

public class DescriptorEditorDialog extends JDialog {

	private static final long serialVersionUID = 478151437279682576L;

	private XBayaEngine engine;

    private XBayaDialog dialog;

    private AiravataRegistry registry;

	private JList descriptorList;

	private Map<ApplicationDeploymentDescription,String> dlist;

	private JButton editButton;

	private AbstractButton removeButton;
	
	public enum DescriptorType{
		HOST,
		SERVICE,
		APPLICATION
	};

	public DescriptorType descriptorType;
	
    /**
     * @param engine XBaya workflow engine
     */
    public DescriptorEditorDialog(XBayaEngine engine,DescriptorType descriptorType) {
        this.engine = engine;
        setRegistry(engine.getConfiguration().getJcrComponentRegistry().getRegistry());
        this.descriptorType=descriptorType;
        initGUI();
        
    }

    /**
     * Displays the dialog.
     */
    public void show() {
        this.dialog.show();
    }

    public void hide() {
        this.dialog.hide();
    }

    /**
     * Initializes the GUI.
     */
    private void initGUI() {
    	descriptorList= new JList(new DefaultListModel());
    	descriptorList.setCellRenderer(new DescriptorListCellRenderer(descriptorType));
    	JScrollPane pane = new JScrollPane(descriptorList);
    	
    	descriptorList.addMouseListener(new MouseAdapter(){
    		@Override
    		public void mouseClicked(MouseEvent e) {
    			if (e.getClickCount()==2){
    				editDescriptor();
    			}
    		}
    	});
    	GridPanel infoPanel=new GridPanel();
        infoPanel.add(pane);
        infoPanel.getSwingComponent().setBorder(BorderFactory.createEtchedBorder());
        SwingUtil.layoutToGrid(infoPanel.getSwingComponent(), 1, 1, 0, 0);

        JButton newButton = new JButton("New...");
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	newDescriptor();
            }
        });
        descriptorList.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e) {
				boolean isSelected=descriptorList.getSelectedIndex()!=-1;
				editButton.setEnabled(isSelected);
				removeButton.setEnabled(isSelected);
			}
        	
        });
        editButton = new JButton("Edit...");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	editDescriptor();
            }

        });
        removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	deleteDescriptor();
            }
        });
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        });
        

        GridPanel buttonPanel = new GridPanel();
        buttonPanel.add(newButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(closeButton);
        buttonPanel.getSwingComponent().setBorder(BorderFactory.createEtchedBorder());
        String title=null; 
        switch (descriptorType){
        	case HOST:
        		title="Host Descriptions";
        		break;
        	case SERVICE:
        		title="Applications";
        		break;
        	case APPLICATION:
        		title="Application Descriptions";
        		break;
        }
		this.dialog = new XBayaDialog(this.engine.getGUI(), title, infoPanel, buttonPanel);
        this.dialog.setDefaultButton(editButton);
        editButton.setEnabled(false);
        removeButton.setEnabled(false);
        loadDescriptors();
    }
    
    private void editDescriptor() {
    	switch (descriptorType){
	    	case HOST:
	    		HostDescription h = (HostDescription) getSelected();
	    		HostDescriptionDialog hostDescriptionDialog = new HostDescriptionDialog(engine.getConfiguration().getJcrComponentRegistry().getRegistry(),false,h);
	    		hostDescriptionDialog.setLocationRelativeTo(this.engine.getGUI().getFrame());
	    		hostDescriptionDialog.open();
	    		if (hostDescriptionDialog.isHostCreated()) {
					loadDescriptors();
				}
	    		break;
	    	case SERVICE:
	    		ServiceDescription d = (ServiceDescription) getSelected();
	    		DeploymentDescriptionDialog serviceDescriptionDialog = new DeploymentDescriptionDialog(getRegistry(),false,d);
	        	serviceDescriptionDialog.open();
//	    		ServiceDescriptionDialog serviceDescriptionDialog = new ServiceDescriptionDialog(getRegistry(),false,d);
//	    		serviceDescriptionDialog.open();
	    		if (serviceDescriptionDialog.isServiceCreated()) {
					loadDescriptors();
				}
	    		break;
	    	case APPLICATION:
	    		ApplicationDeploymentDescription a = (ApplicationDeploymentDescription) getSelected();
	    		String[] s = dlist.get(a).split("\\$");
	    		ApplicationDescriptionDialog aDescriptionDialog = new ApplicationDescriptionDialog(engine,false,a,s[1],s[0]);
	    		aDescriptionDialog.setLocationRelativeTo(this.engine.getGUI().getFrame());
	    		aDescriptionDialog.open();
			if (aDescriptionDialog.isApplicationDescCreated()) {
				loadDescriptors();
			}
			break;
    	}
	}

    private void newDescriptor() {
    	switch (descriptorType){
	    	case HOST:
	    		HostDescriptionDialog hostDescriptionDialog = new HostDescriptionDialog(engine.getConfiguration().getJcrComponentRegistry().getRegistry());
	    		hostDescriptionDialog.open();
	    		if (hostDescriptionDialog.isHostCreated()){
	    			loadDescriptors();
	    		}
	    		break;
	    	case SERVICE:
	    		DeploymentDescriptionDialog serviceDescriptionDialog = new DeploymentDescriptionDialog(getRegistry());
	        	serviceDescriptionDialog.open();
//	    		ServiceDescriptionDialog serviceDescriptionDialog = new ServiceDescriptionDialog(getRegistry());
//	    		serviceDescriptionDialog.open();
	    		if (serviceDescriptionDialog.isServiceCreated()){
	    			loadDescriptors();
	    		}
	    		break;
	    	case APPLICATION:
	    		ApplicationDescriptionDialog applicationDescriptionDialog = new ApplicationDescriptionDialog(engine);
	    		applicationDescriptionDialog.setLocationRelativeTo(this.engine.getGUI().getFrame());
	    		applicationDescriptionDialog.open();
	    		if (applicationDescriptionDialog.isApplicationDescCreated()){
	    			loadDescriptors();
	    		}
	    		break;
    	}
		
	}
    
	private Object getSelected() {
		return descriptorList.getModel().getElementAt(descriptorList.getSelectedIndex());
	}
	protected boolean askQuestion(String title, String question) {
        return JOptionPane.showConfirmDialog(null, question, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    private boolean deleteDescriptor(){
    	String title=null;
    	String question=null;
    	switch (descriptorType){
	    	case HOST:
	    		HostDescription h = (HostDescription) getSelected();
	    		title = "Host description";
	    		question = "Are you sure that you want to remove the service description \""
	                    + h.getType().getHostName() + "\"?";
	    		break;
	    	case SERVICE:
	        	ServiceDescription d = (ServiceDescription) getSelected();
	    		title = "Service description";
	    		question = "Are you sure that you want to remove the applications associated with \""
	                    + d.getType().getName() + "\"?";
	    		break;
	    	case APPLICATION:
	    		ApplicationDeploymentDescription a = (ApplicationDeploymentDescription) getSelected();
	    		title = "Service description";
	    		question = "Are you sure that you want to remove the service description \""
	                    + a.getType().getApplicationName().getStringValue() + "\"?";
	    		break;
    	}
    	
        
		if (askQuestion(title, question)) {
            try {
            	switch (descriptorType){
	    	    	case HOST:
	    	    		HostDescription h = (HostDescription) getSelected();
	    	        	getRegistry().deleteHostDescription(h.getType().getHostName());
	    	    		break;
	    	    	case SERVICE:
	    	        	ServiceDescription d = (ServiceDescription) getSelected();
	    	        	getRegistry().deleteServiceDescription(d.getType().getName());
	    	    		break;
	    	    	case APPLICATION:
	    	    		ApplicationDeploymentDescription a = (ApplicationDeploymentDescription) getSelected();
	    	    		String[] s = dlist.get(a).split("\\$");
	    	        	getRegistry().deleteDeploymentDescription(s[0], s[1], a.getType().getApplicationName().getStringValue());
	    	    		break;
            	}
				loadDescriptors();
			} catch (RegistryException e) {
				this.engine.getGUI().getErrorWindow().error(e);
			}
        }
        return true;
    }
    
    private void loadDescriptors() {
    	try {
    		//allow the registry cache to update
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
    	((DefaultListModel)descriptorList.getModel()).removeAllElements();
    	try {
    		List<?> descriptors=null;
			switch (descriptorType){
	    	case HOST:
	    		descriptors = getRegistry().searchHostDescription(".*");
	    		break;
	    	case SERVICE:
	    		descriptors = getRegistry().searchServiceDescription(".*");
	    		break;
	    	case APPLICATION:
	    		dlist=getRegistry().searchDeploymentDescription();
	    		descriptors =Arrays.asList(dlist.keySet().toArray(new ApplicationDeploymentDescription[]{})); 
	    		break;
    		}
    		for (Object d : descriptors) {
				((DefaultListModel)descriptorList.getModel()).addElement(d);
			}
		} catch (RegistryException e) {
			engine.getGUI().getErrorWindow().error(e);
		}
	}
    
    private static class DescriptorListCellRenderer extends DefaultListCellRenderer{
		private static final long serialVersionUID = -1019715929291926180L;
		private DescriptorType descriptorType;
		public DescriptorListCellRenderer(DescriptorType descriptorType) {
			this.descriptorType=descriptorType;
		}
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (c instanceof JLabel){
				switch (descriptorType){
		    	case HOST:
		    		((JLabel) c).setText(((HostDescription)value).getType().getHostName());
					((JLabel) c).setIcon(JCRBrowserIcons.HOST_ICON);
		    		break;
		    	case SERVICE:
		    		((JLabel) c).setText(((ServiceDescription)value).getType().getName());
					((JLabel) c).setIcon(JCRBrowserIcons.SERVICE_ICON);
		    		break;
		    	case APPLICATION:
		    		((JLabel) c).setText(((ApplicationDeploymentDescription)value).getType().getApplicationName().getStringValue());
					((JLabel) c).setIcon(JCRBrowserIcons.APPLICATION_ICON);
		    		break;
				}
				
			}
			return c;
		}
    	
    }
    public AiravataRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(AiravataRegistry registry) {
        this.registry = registry;
    }
}