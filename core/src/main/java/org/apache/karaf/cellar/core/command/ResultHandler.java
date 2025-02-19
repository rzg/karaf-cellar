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
package org.apache.karaf.cellar.core.command;

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;

/**
 * An event handler class the handles result event.
 */
public class ResultHandler<R extends Result> implements EventHandler<R> {

    public static final String SWITCH_ID = " org.apache.karaf.cellar.command.result.handler";

    private Switch handlerSwitch = new BasicSwitch(SWITCH_ID);
    private CommandStore commandStore;

    /**
     * Retrieves the correlated command from the store and sets the result on the command object.
     *
     * @param result
     */
    public void handle(R result) {
        if (commandStore != null && commandStore.getPending() != null) {
            String id = result.getId();
            Command command = commandStore.getPending().get(id);

            if (command != null && handlerSwitch.getStatus().equals(SwitchStatus.ON)) {
                command.addResults(result);
            }
        }
    }

    public Class<R> getType() {
        return null;
    }

    public CommandStore getCommandStore() {
        return commandStore;
    }

    public void setCommandStore(CommandStore commandStore) {
        this.commandStore = commandStore;
    }

    public Switch getSwitch() {
        return handlerSwitch;
    }

}
