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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.obr.Constants;

import java.util.Set;

/**
 * cluster:obr-list command
 */
@Command(scope = "cluster", name = "obr-listurl", description = "List repository URLs defined in the distributed OBR service for the given group")
public class ObrListUrlCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "Cluster group name", required = true, multiValued = false)
    String groupName;

    public Object doExecute() throws Exception {
        // get the URLs from the distribution set
        Set<String> urls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
        if (urls != null) {
            for (String url : urls) {
                System.out.println(url);
            }
        }
        return null;
    }

}
