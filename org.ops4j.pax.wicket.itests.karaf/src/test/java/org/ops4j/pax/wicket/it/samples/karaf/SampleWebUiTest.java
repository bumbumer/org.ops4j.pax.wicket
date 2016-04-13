/**
 * Copyright OPS4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.wicket.it.samples.karaf;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import java.io.File;
import javax.inject.Inject;
import org.apache.wicket.protocol.http.WebApplication;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.wicket.api.WebApplicationFactory;
import org.ops4j.pax.wicket.samples.plain.simple.service.EchoService;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public class SampleWebUiTest {

    /**
     * WebApplicationFactory of the some of the applications we started. We
     * don't use these members, except for synchronizing the test. Injecting
     * them guarantees that the services are available before our test runs. The
     * timeouts are rather high for the benefit of our CI server.
     */
    protected static final String WEBUI_PORT = "8181";
    protected static final String LOG_LEVEL = "WARN";
    protected static final String SYMBOLIC_NAME_PAX_WICKET_SERVICE = "org.ops4j.pax.wicket.service";

    private static final int TIMEOUT = 120 * 1000;

    @Inject
    private BundleContext bundleContext;

    @Inject
    @Filter(value = "(pax.wicket.applicationname=edge.inheritinjection)", timeout = TIMEOUT)
    private WebApplicationFactory<WebApplication> factoryEdgeInheritInjection;

    @Inject
    @Filter(value = "(pax.wicket.applicationname=springdm.simple.default)", timeout = TIMEOUT)
    private WebApplicationFactory<WebApplication> factorySpringDmSimpleDefault;

    /**
     * see module: /samples/ds/webapplication
     */
    @Inject
    @Filter(value = "(pax.wicket.applicationname=sample.ds.factory)", timeout = TIMEOUT)
    private WebApplicationFactory<WebApplication> factorySampleDS;

    @Configuration
    public final Option[] configureAdditionalProvision() {

        MavenUrlReference wicketFeatureRepo = maven()
                .groupId("org.ops4j.pax.wicket").artifactId("paxwicket")
                .version("3.0.3-SNAPSHOT").classifier("features").type("xml");

        MavenUrlReference paxwicketFeatureRepo = maven()
                .groupId("org.ops4j.pax.wicket").artifactId("features")
                .version("3.0.3-SNAPSHOT").classifier("features").type("xml");
        MavenUrlReference karafSampleFeatureRepo = maven()
                .groupId("org.ops4j.pax.wicket.samples").artifactId("features")
                .version("3.0.3-SNAPSHOT").classifier("features").type("xml");
        MavenUrlReference karafStandardRepo = maven()
                .groupId("org.apache.karaf.features").artifactId("standard").versionAsInProject().classifier("features").type("xml");

        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.apache.karaf").artifactId("apache-karaf")
                .version("4.0.4").type("zip");

        return new Option[]{
            karafDistributionConfiguration()
            .frameworkUrl(karafUrl)
            .unpackDirectory(new File("target", "exam"))
            .useDeployFolder(false),
            keepRuntimeFolder(),
            configureConsole().ignoreLocalConsole(), //logLevel(LogLevel.TRACE),

            features(karafStandardRepo, "scr"),
            features(karafStandardRepo, "webconsole"),
            features(wicketFeatureRepo, "wicket"),
            features(paxwicketFeatureRepo, "pax-wicket"),
            features(paxwicketFeatureRepo, "pax-wicket-spring"),
            features(paxwicketFeatureRepo, "pax-wicket-blueprint"),
            features(karafSampleFeatureRepo, "wicket-samples-base"),
            features(karafSampleFeatureRepo, "wicket-samples-plain-simple"),
            features(karafSampleFeatureRepo, "wicket-samples-plain-pagefactory"),
            features(karafSampleFeatureRepo, "wicket-samples-blueprint-simple"),
            features(karafSampleFeatureRepo, "wicket-samples-blueprint-wicketproperties"),
            features(karafSampleFeatureRepo, "wicket-samples-blueprint-mount"),
            features(karafSampleFeatureRepo, "wicket-samples-blueprint-filter"),
            features(karafSampleFeatureRepo, "wicket-samples-blueprint-injection-simple"),
            features(karafSampleFeatureRepo, "wicket-samples-spring-injection-simple"),
            features(karafSampleFeatureRepo, "wicket-samples-spring-simple"),
            features(karafSampleFeatureRepo, "wicket-samples-edge-mixed"),
            features(karafSampleFeatureRepo, "wicket-samples-ds"),
            features(karafSampleFeatureRepo, "wicket-samples-edge-inheritinjection"),

            provision(mavenBundle().groupId("org.openengsb.wrapped").artifactId("net.sourceforge.htmlunit-all")
            .versionAsInProject())
        };

    }

    @Test
    public void testIfAllExamplesWhereLoaded_shouldBeAbleToAccessThemAll() throws Exception {
        assertNotNull(factoryEdgeInheritInjection);
        assertNotNull(factorySpringDmSimpleDefault);
        assertNotNull(factorySampleDS);
//        //Register a service here for later injection
        bundleContext.registerService(EchoService.class, new EchoServiceImplementation(), null);
        System.in.read();
//        // testNavigationApplication_shouldRender
//        WebClient webclient = new WebClient();
//        HtmlPage page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/navigation/");
//        assertTrue(page.asText().contains("Homepage linking all OPS4J samples"));
//        webclient.closeAllWindows();
//        // testSamplePlainSimple_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/plain/simple/");
//        assertTrue(page.asText().contains("Welcome to the most simple pax-wicket application"));
//        webclient.closeAllWindows();
//        // testSamplePlainPageFactoryShouldAllowLink
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/plain/pagefactory/");
//        assertTrue(page.asText().contains("Welcome to the most simple pax-wicket application"));
//        webclient.closeAllWindows();
//        //Check injected page
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/plain/inject/");
//        assertTrue("/plain/inject/ failed to start properly", page.asText().contains("Echo: Welcome to the most simple pax-wicket application"));
//        webclient.closeAllWindows();
//        // testSampleBlueprintSimpleDefault_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/blueprint/simple/default");
//        assertTrue(page.asText().contains("Welcome to the most simple pax-wicket application based on blueprint"));
//        webclient.closeAllWindows();
//        // testSampleBlueprintSimplePaxwicket_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/blueprint/simple/paxwicket");
//        assertTrue(page.asText().contains("Welcome to the most simple pax-wicket application based on blueprint"));
//        webclient.closeAllWindows();
//        // testSampleBlueprintMountPoint_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/blueprint/mount/manuallymounted");
//        assertTrue(page.asText().contains("This page is mounted manually."));
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/blueprint/mount/automounted");
//        assertTrue(page.asText().contains("This page is automatically mounted."));
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/blueprint/mount/initiallymounted");
//        assertTrue(page.asText().contains("This page is mounted initially."));
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/blueprint/mount");
//        assertTrue(page.asText().contains("Mountpoint blueprint based sample."));
//        webclient.closeAllWindows();
//        // testSampleBlueprintMountPoint_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/blueprint/applicationfactory/first");
//        assertTrue(page.asText().contains("This is the 'The first' application home page."));
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/blueprint/applicationfactory/second");
//        assertTrue(page.asText().contains("This is the 'The second' application home page."));
//        webclient.closeAllWindows();
//        // testSampleSpringdmSimpleDefault_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/springdm/simple/default");
//        assertTrue(page.asText().contains("Welcome to the most simple pax-wicket application based on springdm"));
//        webclient.closeAllWindows();
//        // testSampleSpringdmInjectionSimple_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/springdm/injection/simple");
//        assertTrue(page.asText().contains(
//                "Welcome to the most simple pax-wicket injection application based on springdm."));
//        webclient.closeAllWindows();
//        // testSampleSpringdmSimplePaxwicket_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/springdm/simple/paxwicket");
//        assertTrue(page.asText().contains("Welcome to the most simple pax-wicket application based on springdm"));
//        webclient.closeAllWindows();
//        // testSampleMixed_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/mixed");
//        assertTrue(page.asText().contains(
//                "Welcome to the mixed component and technology example. Enjoy the full power of pax wicket!."));
//        assertTrue(page.asText().contains("This is a link"));
//        assertTrue(page.asText().contains("This is a panel from a separate component"));
//        webclient.closeAllWindows();
//        // testSampleEdgeInheritInjection_shouldRenderPage
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/edge/inheritinjection");
//        assertTrue(page.asText().contains("Back to parent"));
//        assertTrue(page.asText().contains("This is a link"));
//        webclient.closeAllWindows();
//
//        // declarative services
//        webclient = new WebClient();
//        page = webclient.getPage("http://localhost:" + WEBUI_PORT + "/example/ds");
//        assertTrue(page.asText().contains("Declarative Services"));
//        webclient.closeAllWindows();
    }

    /**
     * Simple Echo Implementation for itest...
     */
    private final class EchoServiceImplementation implements EchoService {

        private static final long serialVersionUID = 6447679249771482700L;

        public String someEchoMethod(String toEcho) {
            return "Echo: " + toEcho;
        }
    }
}
