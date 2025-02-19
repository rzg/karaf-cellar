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
package org.apache.karaf.cellar.features;

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.FeatureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Features event handler.
 */
public class FeaturesEventHandler extends FeaturesSupport implements EventHandler<RemoteFeaturesEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSynchronizer.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.features.handler";

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
     * Features Event.
     *
     * @param event
     */
    public void handle(RemoteFeaturesEvent event) {
        String name = event.getName();
        String version = event.getVersion();
        if (isAllowed(event.getSourceGroup(), Constants.FEATURES_CATEGORY, name, EventType.INBOUND) || event.getForce()) {
            FeatureEvent.EventType type = event.getType();
            Boolean isInstalled = isInstalled(name, version);
            try {
                if (FeatureEvent.EventType.FeatureInstalled.equals(type) && !isInstalled) {
                    if (version != null) {
                        LOGGER.debug("CELLAR FEATURES: installing feature {}/{}", name, version);
                        featuresService.installFeature(name, version);
                    } else {
                        LOGGER.debug("CELLAR FEATURES: installing feature {}", name);
                        featuresService.installFeature(name);
                    }
                } else if (FeatureEvent.EventType.FeatureUninstalled.equals(type) && isInstalled) {
                    if (version != null) {
                        LOGGER.debug("CELLAR FEATURES: un-installing feature {}/{}", name, version);
                        featuresService.uninstallFeature(name, version);
                    } else {
                        LOGGER.debug("CELLAR FEATURES: un-installing feature {}", name);
                        featuresService.uninstallFeature(name);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("CELLAR FEATURES: failed to handle event", e);
            }
        } else LOGGER.warn("CELLAR FEATURES: feature {} is marked as BLOCKED INBOUND", name);
    }

    public Class<RemoteFeaturesEvent> getType() {
        return RemoteFeaturesEvent.class;
    }

    public Switch getSwitch() {
        return eventSwitch;
    }

}
