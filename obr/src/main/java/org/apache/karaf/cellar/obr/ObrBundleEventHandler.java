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

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OBR bundles event handler.
 */
public class ObrBundleEventHandler extends ObrSupport implements EventHandler<ObrBundleEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrBundleEventHandler.class);

    protected static final char VERSION_DELIM = ',';

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.bundles.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    private String[] getTarget(String bundle) {
        String[] target;
        int idx = bundle.indexOf(VERSION_DELIM);
        if (idx > 0) {
            target = new String[]{ bundle.substring(0, idx), bundle.substring(idx + 1) };
        } else {
            target = new String[]{ bundle, null };
        }
        return target;
    }

    private Resource selectNewestVersion(Resource[] resources) {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++) {
            if (i == 0) {
                idx = 0;
                v = resources[i].getVersion();
            } else {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0) {
                    idx = i;
                    v = vtmp;
                }
            }
        }
        return (idx < 0) ? null : resources[idx];
    }

    private Resource[] searchRepository(String targetId, String targetVersion) throws InvalidSyntaxException {
        try {
            Bundle bundle = getBundleContext().getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        } catch (NumberFormatException e) {
            // it was not a number, so ignore.
        }

        // the target ID may be a bundle name or bundle symbolic name, so create
        StringBuffer sb = new StringBuffer("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null) {
            sb.insert(0, "&(");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return obrService.discoverResources(sb.toString());
    }

    /**
     * Handle an OBR bundle event.
     *
     * @param event the OBR bundle event.
     */
    @Override
    public void handle(ObrBundleEvent event) {
        String bundleId = event.getBundleId();
        try {
            if (isAllowed(event.getSourceGroup(), Constants.BUNDLES_CONFIG_CATEGORY, bundleId, EventType.INBOUND)) {
                Resolver resolver = obrService.resolver();
                String[] target = getTarget(bundleId);
                Resource resource = selectNewestVersion(searchRepository(target[0], target[1]));
                if (resource != null) {
                    resolver.add(resource);
                } else {
                    LOGGER.warn("CELLAR OBR: bundle {} unknown", target[0]);
                }
                if ((resolver.getAddedResources() != null) &&
                        (resolver.getAddedResources().length > 0)) {
                    if (resolver.resolve()) {
                        if (event.getType() == Constants.BUNDLE_START_EVENT_TYPE)
                            resolver.deploy(Resolver.START);
                        else resolver.deploy(0);
                    }
                } else {
                    Reason[] reqs = resolver.getUnsatisfiedRequirements();
                    if (reqs != null && reqs.length > 0) {
                        LOGGER.warn("CELLAR OBR: unsatisfied requirement(s): ");
                        for (Reason reason : reqs) {
                            LOGGER.warn("CELLAR OBR:    {}", reason.getRequirement().getFilter());
                            LOGGER.warn("CELLAR OBR:      {}", reason.getResource().getPresentationName());
                        }
                    } else LOGGER.warn("CELLAR OBR: could not resolve targets");
                }
            } else LOGGER.warn("CELLAR OBR: bundle {} is marked as BLOCKED INBOUND", bundleId);
        } catch (Exception e) {
            LOGGER.error("CELLAR OBR: failed to handle bundle event {}", bundleId, e);
        }
    }

    public Class<ObrBundleEvent> getType() {
        return ObrBundleEvent.class;
    }

    public Switch getSwitch() {
        return this.eventSwitch;
    }

}
