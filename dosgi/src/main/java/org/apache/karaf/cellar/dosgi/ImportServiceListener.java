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
package org.apache.karaf.cellar.dosgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Producer;
import org.apache.karaf.cellar.core.command.ClusteredExecutionContext;
import org.apache.karaf.cellar.core.command.CommandStore;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Listener on the import service.
 */
public class ImportServiceListener implements ListenerHook {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ImportServiceListener.class);

    private BundleContext bundleContext;
    private ClusterManager clusterManager;
    private CommandStore commandStore;
    private EventTransportFactory eventTransportFactory;
    private Map<String,EndpointDescription> remoteEndpoints;

    private final Map<EndpointDescription, ServiceRegistration> registrations = new HashMap<EndpointDescription, ServiceRegistration>();

    private final Map<String, EventProducer> producers = new HashMap<String, EventProducer>();
    private final Map<String, EventConsumer> consumers = new HashMap<String, EventConsumer>();

    public void init() {
        remoteEndpoints = clusterManager.getMap(Constants.REMOTE_ENDPOINTS);
    }

    public void destroy() {
        for (Map.Entry<EndpointDescription, ServiceRegistration> entry : registrations.entrySet()) {
            ServiceRegistration registration = entry.getValue();
            registration.unregister();
        }
        for (Map.Entry<String, EventConsumer> consumerEntry : consumers.entrySet()) {
            EventConsumer consumer = consumerEntry.getValue();
            consumer.stop();
        }
        consumers.clear();
        producers.clear();

    }

    @Override
    public void added(Collection listeners) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listeners) {

                if (listenerInfo.getBundleContext() == bundleContext || listenerInfo.getFilter() == null) {
                    continue;
                }

                // Make sure we only import remote services

                // Iterate through known services and import them if needed
                Set<EndpointDescription> matches = new LinkedHashSet<EndpointDescription>();
                for (Map.Entry<String,EndpointDescription> entry : remoteEndpoints.entrySet()) {
                    EndpointDescription endpointDescription = entry.getValue();
                    if (endpointDescription.matches(listenerInfo.getFilter()) && !endpointDescription.getNodes().contains(clusterManager.getNode().getId())) {
                        matches.add(endpointDescription);
                    }
                }

                for (EndpointDescription endpoint : matches) {
                    importService(endpoint, listenerInfo);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void removed(Collection listeners) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listeners) {
                if (listenerInfo.getBundleContext() == bundleContext || listenerInfo.getFilter() == null) {
                    continue;
                }

                // Make sure we only import remote services
                String filter = "(&" + listenerInfo.getFilter() + "(!(" + Constants.ENDPOINT_FRAMEWORK_UUID + "=" + clusterManager.getNode().getId() + ")))";
                // Iterate through known services and import them if needed
                Set<EndpointDescription> matches = new LinkedHashSet<EndpointDescription>();
                for (Map.Entry<String,EndpointDescription> entry : remoteEndpoints.entrySet()) {
                    EndpointDescription endpointDescription = entry.getValue();
                    if (endpointDescription.matches(filter)) {
                        matches.add(endpointDescription);
                    }
                }

                for (EndpointDescription endpoint : matches) {
                    unimportService(endpoint);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Imports a remote service to the service registry.
     *
     * @param endpoint
     * @param listenerInfo
     */
    private void importService(EndpointDescription endpoint, ListenerInfo listenerInfo) {
        LOGGER.debug("CELLAR DOSGI: importing remote service");

        EventProducer requestProducer = producers.get(endpoint.getId());
        if(requestProducer == null) {
            requestProducer = eventTransportFactory.getEventProducer(Constants.INTERFACE_PREFIX + Constants.SEPARATOR + endpoint.getId(),Boolean.FALSE);
            producers.put(endpoint.getId(),requestProducer);
        }

        EventConsumer resultConsumer = consumers.get(endpoint.getId());
        if(resultConsumer == null) {
            resultConsumer = eventTransportFactory.getEventConsumer(Constants.RESULT_PREFIX + Constants.SEPARATOR + clusterManager.getNode().getId() + endpoint.getId(), Boolean.FALSE);
            consumers.put(endpoint.getId(),resultConsumer);
        } else if(!resultConsumer.isConsuming()) {
            resultConsumer.start();
        }

        producers.put(endpoint.getId(),requestProducer);
        consumers.put(endpoint.getId(),resultConsumer);

        ExecutionContext executionContext = new ClusteredExecutionContext(requestProducer,commandStore);

        RemoteServiceFactory remoteServiceFactory = new RemoteServiceFactory(endpoint, clusterManager, executionContext);
        ServiceRegistration registration = listenerInfo.getBundleContext().registerService(endpoint.getServiceClass(),
                remoteServiceFactory,
                new Hashtable<String, Object>(endpoint.getProperties()));
        registrations.put(endpoint, registration);
    }

    /**
     * Unregisters an imported service
     *
     * @param endpoint
     */
    private void unimportService(EndpointDescription endpoint) {
        ServiceRegistration registration = registrations.get(endpoint);
        registration.unregister();

        producers.remove(endpoint.getId());
        EventConsumer consumer = consumers.remove(endpoint.getId());
        if(consumer != null) {
            consumer.stop();
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public CommandStore getCommandStore() {
        return commandStore;
    }

    public void setCommandStore(CommandStore commandStore) {
        this.commandStore = commandStore;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

}
