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
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.springframework.osgi.context.BundleContextAware;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Hazelcast group manager.
 */
public class HazelcastGroupManager implements GroupManager, BundleContextAware {

    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HazelcastClusterManager.class);

    private static final String GROUPS = "org.apache.karaf.cellar.groups";
    private static final String DEFAULT_GROUP = "default";

    private Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();

    private Map<String, EventProducer> groupProducers = new HashMap<String, EventProducer>();
    private Map<String, EventConsumer> groupConsumer = new HashMap<String, EventConsumer>();

    private BundleContext bundleContext;

    private HazelcastInstance instance;
    private ConfigurationAdmin configurationAdmin;

    private EventTransportFactory eventTransportFactory;
    private CombinedClassLoader combinedClassLoader;

    public void init() {
        //Add group to configuration
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Dictionary<String, String> properties = configuration.getProperties();
                if (properties != null) {
                    String groups = properties.get(Configurations.GROUPS_KEY);
                    Set<String> groupNames = convertStringToSet(groups);
                    if (groupNames != null && !groupNames.isEmpty()) {
                        for (String groupName : groupNames) {
                            registerGroup(groupName);
                        }
                        return;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: error reading group configuration", e);
        }
        registerGroup(DEFAULT_GROUP);
    }

    public void destroy() {
        for(Map.Entry<String,EventConsumer> consumerEntry:groupConsumer.entrySet()) {
            EventConsumer consumer = consumerEntry.getValue();
            consumer.stop();
        }
        groupConsumer.clear();
        groupProducers.clear();
    }

    @Override
    public Node getNode() {
        Node node = null;
        Cluster cluster = instance.getCluster();
        if (cluster != null) {
            Member member = cluster.getLocalMember();
            node = new HazelcastNode(member.getInetSocketAddress().getHostName(), member.getInetSocketAddress().getPort());
        }
        return node;
    }

    public Group createGroup(String groupName) {
        Group group = listGroups().get(groupName);
        if (group == null) {
            group = new Group(groupName);
        }
        if (!listGroups().containsKey(groupName)) {
            copyGroupConfiguration(Configurations.DEFAULT_GROUP_NAME, groupName);
            listGroups().put(groupName, group);
        }
        return group;
    }

    public void deleteGroup(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            if (!groupName.equals(Configurations.DEFAULT_GROUP_NAME)) {
                listGroups().remove(groupName);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Set<Group> listLocalGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroups(getNode());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Set<Group> listAllGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return new HashSet<Group>(listGroups().values());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Group findGroupByName(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroups().get(groupName);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Map<String, Group> listGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return instance.getMap(GROUPS);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Set<Group> listGroups(Node node) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Set<Group> result = new HashSet<Group>();

            Map<String, Group> groupMap = instance.getMap(GROUPS);
            Collection<Group> groupCollection = groupMap.values();
            if (groupCollection != null && !groupCollection.isEmpty()) {
                for (Group group : groupCollection) {
                    if (group.getMembers().contains(node)) {
                        result.add(group);
                    }
                }
            }
            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Set<String> listGroupNames() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroupNames(getNode());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Set<String> listGroupNames(Node node) {
        Set<String> names = new HashSet<String>();
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Map<String, Group> groups = listGroups();

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups.values()) {
                    if (group.getMembers().contains(node)) {
                        names.add(group.getName());
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return names;
    }

    public void registerGroup(Group group) {
        String groupName = group.getName();
        createGroup(groupName);

        Properties serviceProperties = new Properties();
        serviceProperties.put("type", "group");
        serviceProperties.put("name", groupName);

        if (!producerRegistrations.containsKey(groupName)) {
            EventProducer producer = groupProducers.get(groupName);
            if(producer == null) {
                producer = eventTransportFactory.getEventProducer(groupName,Boolean.TRUE);
                groupProducers.put(groupName,producer);
            }

            ServiceRegistration producerRegistration = bundleContext.registerService(EventProducer.class.getCanonicalName(), producer, serviceProperties);
            producerRegistrations.put(groupName, producerRegistration);
        }

        if (!consumerRegistrations.containsKey(groupName)) {
            EventConsumer consumer = groupConsumer.get(groupName);
            if(consumer == null) {
                consumer = eventTransportFactory.getEventConsumer(groupName,true);
                groupConsumer.put(groupName,consumer);
            } else if(!consumer.isConsuming()) {
                consumer.start();
            }
            ServiceRegistration consumerRegistration = bundleContext.registerService(EventConsumer.class.getCanonicalName(), consumer, serviceProperties);
            consumerRegistrations.put(groupName, consumerRegistration);
        }

        group.getMembers().add(getNode());
        listGroups().put(groupName, group);

        //Add group to configuration
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            Dictionary<String, String> properties = configuration.getProperties();
            String groups = properties.get(Configurations.GROUPS_KEY);
            if (groups == null || (groups.trim().length() < 1)) {
                groups = groupName;
            } else {

                        Set<String> groupNamesSet = convertStringToSet(groups);
                        groupNamesSet.add(groupName);
                        groups = convertSetToString(groupNamesSet);
                    }

            if (groups == null || (groups.trim().length() < 1)) {
                groups = groupName;
            }
            properties.put(Configurations.GROUPS_KEY, groups);
            configuration.update(properties);
        } catch (IOException e) {
            LOGGER.error("Error reading group configuration {}", group);
        }

        //Sync
        try {
            ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences("org.apache.karaf.cellar.core.Synchronizer", null);
            if (serviceReferences != null && serviceReferences.length > 0) {
                for (ServiceReference ref : serviceReferences) {
                    Synchronizer synchronizer = (Synchronizer) bundleContext.getService(ref);
                    if (synchronizer.isSyncEnabled(group)) {
                        synchronizer.pull(group);
                        synchronizer.push(group);
                    }
                    bundleContext.ungetService(ref);
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Error looking up for synchronizers.", e);
        }
    }

    public void registerGroup(String groupName) {
        Group group = listGroups().get(groupName);
        if (group == null) {
            group = new Group(groupName);
        }
        registerGroup(group);
    }

    public void unRegisterGroup(String groupName) {
        unRegisterGroup(listGroups().get(groupName));
    }

    public void unRegisterGroup(Group group) {
        String groupName = group.getName();
        //1. Remove local node from group.
        group.getMembers().remove(getNode());
        listGroups().put(groupName, group);

        //2. Unregister group consumers
        if (consumerRegistrations != null && !consumerRegistrations.isEmpty()) {
            ServiceRegistration consumerRegistration = consumerRegistrations.get(groupName);
            if (consumerRegistration != null) {
                consumerRegistration.unregister();
                consumerRegistrations.remove(groupName);
            }

        }

        //3. Unregister group producers
        if (producerRegistrations != null && !producerRegistrations.isEmpty()) {
            ServiceRegistration producerRegistration = producerRegistrations.get(groupName);
            if (producerRegistration != null) {
                producerRegistration.unregister();
                producerRegistrations.remove(groupName);
            }
        }

        //4. Remove Consumers & Producers
        groupProducers.remove(groupName);
        EventConsumer consumer = groupConsumer.remove(groupName);
        if(consumer != null) {
            consumer.stop();
        }



        //Remove group from configuration
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            Dictionary<String, String> properties = configuration.getProperties();
            String groups = properties.get(Configurations.GROUPS_KEY);
            if (groups == null || (groups.trim().length() < 1)) {
                groups = "";
            } else if (groups.contains(groupName)) {

                Set<String> groupNamesSet = convertStringToSet(groups);
                groupNamesSet.remove(groupName);
                groups = convertSetToString(groupNamesSet);

            }
            properties.put(Configurations.GROUPS_KEY, groups);
            configuration.update(properties);
        } catch (IOException e) {
            LOGGER.error("Error reading group configuration {}", group);
        }
    }

    /**
     * Copies the setup of a {@link Group}.
     *
     * @param sourceGroupName
     * @param targetGroupName
     */
    public void copyGroupConfiguration(String sourceGroupName, String targetGroupName) {
        try {
            Configuration conf = configurationAdmin.getConfiguration(Configurations.GROUP);
            if (conf != null) {
                Dictionary dictionary = conf.getProperties();
                if (dictionary != null) {
                    Dictionary updatedProperties = new Properties();
                    Enumeration keyEnumeration = dictionary.keys();
                    while (keyEnumeration.hasMoreElements()) {
                        String key = (String) keyEnumeration.nextElement();
                        String value = (String) dictionary.get(key);

                        if (key.startsWith(sourceGroupName)) {
                            String newKey = key.replace(sourceGroupName, targetGroupName);
                            updatedProperties.put(newKey, value);
                        }
                        updatedProperties.put(key, value);
                    }


                    conf.update(updatedProperties);
                }
            }

        } catch (IOException e) {
            LOGGER.error("Error reading group configuration.", e);
        }
    }

    /**
     * Utility method which converts a set to a String.
     *
     * @param set
     * @return
     */
    protected String convertSetToString(Set<String> set) {
        StringBuffer result = new StringBuffer();
        Iterator<String> groupIterator = set.iterator();
        while (groupIterator.hasNext()) {
            String name = groupIterator.next();
            result.append(name);
            if (groupIterator.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    }

    /**
     * Utility method which converts String to Set.
     *
     * @param string
     * @return
     */
    protected Set<String> convertStringToSet(String string) {
        Set<String> result = new HashSet<String>();
        String[] groupNames = string.split(",");

        if (groupNames != null && groupNames.length > 0) {
            for (String name : groupNames) {
                result.add(name);
            }
        } else {
            result.add(string);
        }
        return result;
    }


    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

    public CombinedClassLoader getCombinedClassLoader() {
        return combinedClassLoader;
    }

    public void setCombinedClassLoader(CombinedClassLoader combinedClassLoader) {
        this.combinedClassLoader = combinedClassLoader;
    }
}
