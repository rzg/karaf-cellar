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
package org.apache.karaf.cellar.features.shell;

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.features.RemoteFeaturesEvent;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.features.FeatureEvent;

/**
 * Uninstall feature command.
 */
@Command(scope = "cluster", name = "features-uninstall", description = "Uninstall a feature from the cluster group")
public class UninstallFeatureCommand extends FeatureCommandSupport {

    @Argument(index = 0, name = "group", description = "The name of the group", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "feature", description = "The name of the feature", required = true, multiValued = false)
    String feature;

    @Argument(index = 2, name = "version", description = "The version of the feature", required = false, multiValued = false)
    String version;

    @Override
    protected Object doExecute() throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        EventProducer producer = eventTransportFactory.getEventProducer(groupName,true);
        RemoteFeaturesEvent event = new RemoteFeaturesEvent(feature, version, FeatureEvent.EventType.FeatureUninstalled);
        event.setForce(true);
        event.setSourceGroup(group);
        producer.produce(event);

        updateFeatureStatus(groupName, feature, version, true);
        return null;
    }

}
