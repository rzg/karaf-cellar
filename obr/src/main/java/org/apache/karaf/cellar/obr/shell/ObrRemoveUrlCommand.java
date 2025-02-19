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
package org.apache.karaf.cellar.obr.shell;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.obr.Constants;
import org.apache.karaf.cellar.obr.ObrBundleInfo;
import org.apache.karaf.cellar.obr.ObrUrlEvent;

import java.util.Set;

/**
 * cluster:obr-removeurl command
 */
@Command(scope = "cluster", name = "obr-removeurl", description = "Remove a repository URL from the distributed OBR service")
public class ObrRemoveUrlCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "url", description = "The repository URL to add in the OBR service", required = true, multiValued = false)
    String url;

    public Object doExecute() throws Exception {
        // find the group for the given name
        Group group = groupManager.findGroupByName(groupName);
        // create an event and produce it
        EventProducer producer = eventTransportFactory.getEventProducer(groupName, true);
        ObrUrlEvent event = new ObrUrlEvent(url, Constants.URL_REMOVE_EVENT_TYPE);
        event.setForce(true);
        event.setSourceGroup(group);
        producer.produce(event);
        // remove URL from the distributed map
        Set<String> urls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
        urls.remove(url);
        // remove bundles from the distributed map
        Set<ObrBundleInfo> bundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
        synchronized(obrService) {
            Repository repository = obrService.addRepository(url);
            Resource[] resources = repository.getResources();
            for (Resource resource : resources) {
                ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(),resource.getSymbolicName(), resource.getVersion().toString());
                bundles.remove(info);
            }
            obrService.removeRepository(url);
        }
        return null;
    }

}
