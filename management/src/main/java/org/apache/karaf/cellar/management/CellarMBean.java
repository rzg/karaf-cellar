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
package org.apache.karaf.cellar.management;

import javax.management.openmbean.TabularData;

/**
 *  Cellar MBean describing the core attributes and operation that you can do on Cellar.
 */
public interface CellarMBean {

    TabularData consumerStatus() throws Exception;
    void consumerStart(String nodeId) throws Exception;
    void consumerStop(String nodeId) throws Exception;

    TabularData handlerStatus() throws Exception;
    void handlerStart(String handlerId, String nodeId) throws Exception;
    void handlerStop(String handlerId, String nodeId) throws Exception;

    TabularData producerStatus() throws Exception;
    void producerStart(String nodeId) throws Exception;
    void producerStop(String nodeId) throws Exception;

}
