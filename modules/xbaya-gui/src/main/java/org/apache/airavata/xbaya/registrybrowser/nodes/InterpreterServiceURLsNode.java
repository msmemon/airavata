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

package org.apache.airavata.xbaya.registrybrowser.nodes;

import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;

import org.apache.airavata.client.api.exception.AiravataAPIInvocationException;
import org.apache.airavata.xbaya.model.registrybrowser.InterpreterServiceURLs;
import org.apache.airavata.xbaya.ui.actions.AbstractBrowserActionItem;
import org.apache.airavata.xbaya.ui.actions.registry.browser.RefreshAction;

public class InterpreterServiceURLsNode extends AbstractAiravataTreeNode {
    private InterpreterServiceURLs interpreterServiceURLs;

    public InterpreterServiceURLsNode(InterpreterServiceURLs interpreterServiceURLs, TreeNode parent) {
        super(parent);
        setInterpreterServiceURLs(interpreterServiceURLs);
    }

    @Override
    protected List<TreeNode> getChildren() {
        try {
            return getTreeNodeList(getInterpreterServiceURLs().getURLS().toArray(), this);
        } catch (AiravataAPIInvocationException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getCaption(boolean selected, boolean expanded, boolean leaf, boolean hasFocus) {
        return "Interpreter Servers";
    }

    @Override
    public Icon getIcon(boolean selected, boolean expanded, boolean leaf, boolean hasFocus) {
        return JCRBrowserIcons.GFAC_URLS_ICON;
    }

    @Override
    public List<String> getSupportedActions() {
        return Arrays.asList(RefreshAction.ID);
    }

    @Override
    public String getActionCaption(AbstractBrowserActionItem action) {
        return action.getDefaultCaption();
    }

    @Override
    public Icon getActionIcon(AbstractBrowserActionItem action) {
        return null;
    }

    @Override
    public String getActionDescription(AbstractBrowserActionItem action) {
        return null;
    }

	public InterpreterServiceURLs getInterpreterServiceURLs() {
		return interpreterServiceURLs;
	}

	public void setInterpreterServiceURLs(InterpreterServiceURLs interpreterServiceURLs) {
		this.interpreterServiceURLs = interpreterServiceURLs;
	}
}
