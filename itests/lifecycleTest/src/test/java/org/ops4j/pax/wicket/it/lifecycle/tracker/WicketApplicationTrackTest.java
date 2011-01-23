/*  Copyright 2008 Edward Yakop.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.wicket.it.lifecycle.tracker;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.wicket.it.bundles.simpleApp.SimpleAppConstants.SYMBOLIC_NAME_SIMPLE_APP;

import org.junit.Test;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.wicket.it.PaxWicketIntegrationTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author edward.yakop@gmail.com
 * @since 0.5.4
 */
public final class WicketApplicationTrackTest extends PaxWicketIntegrationTest {

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public final Option[] configureAdditionalProvision() {
        return options(provision("mvn:org.ops4j.pax.wicket.itests.bundles/pax-wicket-itests-bundles-simpleApp"));
    }

    @Test
    public final void testApplicationTracker()
        throws Exception {
        sleep(1000);
        Bundle simpleAppBundle = getBundleBySymbolicName(bundleContext, SYMBOLIC_NAME_SIMPLE_APP);
        assertNotNull(simpleAppBundle);
        assertEquals(simpleAppBundle.getState(), Bundle.ACTIVE);
        ServiceReference[] beforeStopServices = simpleAppBundle.getRegisteredServices();
        assertEquals(12, beforeStopServices.length);

        Bundle bundle = getPaxWicketServiceBundle(bundleContext);
        bundle.stop();

        ServiceReference[] services = simpleAppBundle.getRegisteredServices();
        assertNotNull(services);
        assertEquals(1, services.length);
    }
}