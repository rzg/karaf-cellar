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
package org.apache.karaf.cellar.obr;

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OBR URL Event handler.
 */
public class ObrUrlEventHandler extends ObrSupport implements EventHandler<ObrUrlEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.urls.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Process an OBR URL event.
     *
     * @param obrUrlEvent the OBR URL Event.
     */
    @Override
    public void handle(ObrUrlEvent obrUrlEvent) {
        String url = obrUrlEvent.getUrl();
        String groupName = obrUrlEvent.getSourceGroup().getName();
        try {
            if (isAllowed(obrUrlEvent.getSourceGroup(), Constants.URLS_CONFIG_CATEGORY, url, EventType.INBOUND) || obrUrlEvent.getForce()) {
                LOGGER.debug("CELLAR OBR: received OBR URL {}", url);
                if (obrUrlEvent.getType() == Constants.URL_ADD_EVENT_TYPE) {
                    LOGGER.debug("CELLAR OBR: add OBR URL {}", url);
                    obrService.addRepository(url);
                }
                if (obrUrlEvent.getType() == Constants.URL_REMOVE_EVENT_TYPE) {
                    LOGGER.debug("CELLAR OBR: remove OBR URL {}", url);
                    boolean removed = obrService.removeRepository(url);
                    if (!removed) {
                        LOGGER.warn("CELLAR OBR: the repository URL hasn't been removed from the OBR service");
                    }
                }
            } else LOGGER.warn("CELLAR OBR: repository URL {} is marked as BLOCKED INBOUND", url);
        } catch (Exception e) {
            LOGGER.error("CELLAR OBR: failed to register URL {}", url, e);
        }
    }

    public Class<ObrUrlEvent> getType() {
        return ObrUrlEvent.class;
    }

    public Switch getSwitch() {
        return this.eventSwitch;
    }

}
