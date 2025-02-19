/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.management.internal;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.control.ManageGroupAction;
import org.apache.karaf.cellar.core.control.ManageGroupCommand;
import org.apache.karaf.cellar.management.CellarGroupMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *  Implementation of the Cellar group MBean.
 */
public class CellarGroupMBeanImpl extends StandardMBean implements CellarGroupMBean {

    private ClusterManager clusterManager;
    private ExecutionContext executionContext;
    private GroupManager groupManager;

    public CellarGroupMBeanImpl() throws NotCompliantMBeanException {
        super(CellarGroupMBean.class);
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public GroupManager getGroupManager() {
        return this.groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void create(String name) throws Exception {
        groupManager.createGroup(name);
    }

    public void delete(String name) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Group g = groupManager.findGroupByName(name);
            List<String> nodes = new ArrayList<String>();

            if (g.getMembers() != null && !g.getMembers().isEmpty()) {
                for (Node n : g.getMembers()) {
                    nodes.add(n.getId());
                }
                ManageGroupCommand command = new ManageGroupCommand(clusterManager.generateId());
                command.setAction(ManageGroupAction.QUIT);
                command.setGroupName(name);
                Set<Node> recipientList = clusterManager.listNodes(nodes);
                command.setDestination(recipientList);
                executionContext.execute(command);
            }

            groupManager.deleteGroup(name);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public void join(String group, String nodeId) throws Exception {
        List<String> nodes = new ArrayList<String>();
        nodes.add(nodeId);
        ManageGroupCommand command = new ManageGroupCommand(clusterManager.generateId());
        command.setAction(ManageGroupAction.JOIN);
        command.setGroupName(group);
        Set<Node> recipientList = clusterManager.listNodes(nodes);
        command.setDestination(recipientList);
        executionContext.execute(command);
    }

    public void quit(String group, String nodeId) throws Exception {
        List<String> nodes = new ArrayList<String>();
        nodes.add(nodeId);
        ManageGroupCommand command = new ManageGroupCommand(clusterManager.generateId());
        command.setAction(ManageGroupAction.QUIT);
        command.setGroupName(group);
        Set<Node> recipientList = clusterManager.listNodes(nodes);
        command.setDestination(recipientList);
        executionContext.execute(command);
    }

    public TabularData getGroups() throws Exception {
        Set<Group> allGroups = groupManager.listAllGroups();

        CompositeType groupType = new CompositeType("Group", "Karaf Cellar cluster group",
                new String[]{ "name", "members"},
                new String[]{ "Name of the cluster group", "Members of the cluster group" },
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING });

        TabularType tableType = new TabularType("Groups", "Table of all Karaf Cellar groups", groupType,
                new String[]{ "name" });

        TabularData table = new TabularDataSupport(tableType);

        for (Group group : allGroups) {
            StringBuffer members = new StringBuffer();
            for (Node node : group.getMembers()) {
                members.append(node.getId());
                members.append(" ");
            }
            CompositeData data = new CompositeDataSupport(groupType,
                    new String[]{ "name", "members" },
                    new Object[]{ group.getName(), members.toString() });
            table.put(data);
        }

        return table;
    }

}
