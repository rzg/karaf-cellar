/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.Result;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Produces {@code Event}s into the distributed {@code ITopic}.
 */
public class QueueProducer<E extends Event> implements EventProducer<E> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(QueueProducer.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.queue.producer";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    private HazelcastInstance instance;
    private IQueue<E> queue;
    private Node node;

    /**
     * Initialization method.
     */
    public void init() {
        if (queue == null) {
            queue = instance.getQueue(Constants.QUEUE);
        }
    }

    /**
     * Destruction method.
     */
    public void destroy() {
    }

    /**
     * Propagates an event into the distributed {@code ITopic}.
     *
     * @param event
     */
    public void produce(E event) {
        if (eventSwitch.getStatus().equals(SwitchStatus.ON) || event.getForce() || event instanceof Result) {
            event.setSourceNode(node);
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                LOGGER.error("CELLAR HAZELCAST: queue producer interrupted", e);
            }
        }
    }

    public Switch getSwitch() {
        return eventSwitch;
    }

    public IQueue<E> getQueue() {
        return queue;
    }

    public void setQueue(IQueue<E> queue) {
        this.queue = queue;
    }

    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }


    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

}
