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

import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventType;

/**
 * OBR URL Event.
 */
public class ObrUrlEvent extends Event {

    private String url;
    private int type;

    public ObrUrlEvent(String url, int type) {
        super(url);
        this.url = url;
        this.type = type;
    }

    public String getUrl() {
        return this.url;
    }

    public int getType() {
        return this.type;
    }

}