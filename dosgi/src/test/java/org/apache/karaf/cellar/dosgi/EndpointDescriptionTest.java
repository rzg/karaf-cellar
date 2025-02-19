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
package org.apache.karaf.cellar.dosgi;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test on a endpoint description.
 */
public class EndpointDescriptionTest {

    String objectClass = "org.apache.karaf.cellar.dosgi.Test";
    String frameworkUUID1 = "192.168.1.101:5701";
    String frameworkUUID2 = "192.168.1.102:5702";
    String filterPattern = "(&(objectClass=%s))";

    @Test
    public void testMatches() throws Exception {
        //This is a dummy test for testing the behaviour of matches method.
        String testEndpointFilter = String.format(filterPattern,objectClass);
        String endpointId = objectClass+Constants.SEPARATOR+"1.0.0";
        EndpointDescription endpointDescription1 = new EndpointDescription(endpointId,frameworkUUID1);
        EndpointDescription endpointDescription2 = new EndpointDescription(endpointId,frameworkUUID2);
        Assert.assertTrue(endpointDescription1.matches(testEndpointFilter));
        Assert.assertTrue(endpointDescription2.matches(testEndpointFilter));

    }

}
