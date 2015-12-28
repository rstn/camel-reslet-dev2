package com.peterservice.camel.component.restlet;

import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Route;
import org.restlet.routing.VirtualHost;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static org.junit.Assert.*;

public class VirtualHostWithPSMatchingTest {

    private static VirtualHostWithPSMatching vHost;

    @BeforeClass
    public static void initVH() throws Exception {
        VirtualHost virtualHost = new VirtualHost(new Context());
        vHost = new VirtualHostWithPSMatching(virtualHost);
        File xmlConfigFile = new File("src/test/resources/service_config.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlConfigFile);
        NodeList nodeList = doc.getElementsByTagName("function");
        for (int i = 0; i < nodeList.getLength(); i++) {
            String str = nodeList.item(i).getAttributes().getNamedItem("url").getTextContent();
            if (str.isEmpty() || (str.charAt(0) == '/'))
                str = "subscribers" + str;
            else
                str = "subscribers/" + str;
            vHost.attach(str, ServerResource.class);
        }
    }

    @Test
    public void testGetCustomBasic() throws Exception {
        Request request = new Request(Method.GET, "subscribers/12345/calls/search");
        Route route = null;
        route = vHost.getCustom(request, null);
        String s = route.getTemplate().getPattern();
        assertNotNull(route);
        assertEquals("subscribers/{subscriberId}/calls/search", s);
    }

    @Test
    public void testGetCustomSlashURL() throws Exception {
        Request request = new Request(Method.GET, "/subscribers/12345/calls/search");
        Route route = null;
        route = vHost.getCustom(request, null);
        String s = route.getTemplate().getPattern();
        assertNotNull(route);
        assertEquals("subscribers/{subscriberId}/calls/search", s);
    }

    @Test
    public void testGetCustomScope7B() throws Exception {
        Request request = new Request(Method.GET, "subscribers/{12345/calls/search");
        Route route = null;
        route = vHost.getCustom(request, null);
        assertNull(route);
    }

    @Test
    public void testGetCustomScope7D() throws Exception {
        Request request = new Request(Method.GET, "subscribers/12345}/calls/search");
        Route route = null;
        route = vHost.getCustom(request, null);
        assertNull(route);
    }

    @Test
    public void testGetCustom1() throws Exception {
        Request request = new Request(Method.GET, "subscribers/5/calls/search");
        Route route = null;
        route = vHost.getCustom(request, null);
        String s = route.getTemplate().getPattern();
        assertNotNull(route);
        assertEquals("subscribers/{subscriberId}/calls/search", s);
    }

    @Test
    public void testMultiple() throws Exception {
        Request request = new Request(Method.GET, "subscribers/5/phoneBaseInfo/5");
        assertEquals("subscribers/{subscriberId}/phoneBaseInfo/{phoneNumberId}", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/5/phoneBaseInfo");
        assertEquals("subscribers/{subscriberId}/phoneBaseInfo", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/5/services/allowedActions");
        assertEquals("subscribers/{subscriberId}/services/allowedActions", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/5/services/456");
        assertEquals("subscribers/{subscriberId}/services/{serviceId}", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/5/services/456/bans");
        assertEquals("subscribers/{subscriberId}/services/{serviceId}/bans", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/5/services");
        assertEquals("subscribers/{subscriberId}/services", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/5");
        assertEquals("subscribers/{subscriberId}", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/test/ratingOptions/567");
        assertEquals("subscribers/test/ratingOptions/{id}", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/test/rating/567");
        assertEquals("subscribers/test/{someId}/567", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/test/rating/5");
        assertEquals("subscribers/test/{someId}/{id}", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/2346/ratingOptionsInternal");
        assertEquals("subscribers/{subscriberId}/ratingOptionsInternal", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/2346");
        assertEquals("subscribers/{subscriberId}", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/serviceVersion");
        assertEquals("subscribers/serviceVersion", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/12345/services/idsrv5757/history/search");
        assertEquals("subscribers/{subscriberId}/services/{serviceId}/history/search", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/qe45rt4/SIMCards/link");
        assertEquals("subscribers/{subscriberId}/SIMCards/link", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/PSTN/ratePlans/availableForAssign");
        assertEquals("subscribers/PSTN/ratePlans/availableForAssign", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/w35rew5/periodicAccumulators");
        assertEquals("subscribers/{subscriberId}/periodicAccumulators", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/12345/packs/allowedActions/search");
        assertEquals("subscribers/{subscriberId}/packs/allowedActions/search", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/8349578/services/255/serviceSpecificAttributes");
        assertEquals("subscribers/{subscriberId}/services/{serviceId}/serviceSpecificAttributes", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/346/discounts/id224");
        assertEquals("subscribers/{subscriberId}/discounts/{discountId}", vHost.getCustom(request, null).getTemplate().getPattern());

        request = new Request(Method.GET, "subscribers/dt54y/discounts/activate");
        assertEquals("subscribers/{subscriberId}/discounts/activate", vHost.getCustom(request, null).getTemplate().getPattern());
    }

    @Test
    public void testGetCustomNull() throws Exception {
        Request request = new Request(Method.GET, "");
        Route route = null;
        route = vHost.getCustom(request, null);
        assertNull(route);
    }

    @Test
    public void testGetCustomSub() throws Exception {
        Request request = new Request(Method.GET, "subscribers/48yh/charges/runBilling");
        Route route = null;
        route = vHost.getCustom(request, null);
        String s = route.getTemplate().getPattern();
        assertNotNull(route);
        assertEquals("subscribers/{subscriberId}/charges/runBilling", s);
    }

    @Test
    public void testGetCustomDoubleSlash() throws Exception {
        Request request = new Request(Method.GET, "subscribers//services/allowedActions");
        assertEquals("subscribers/{subscriberId}/services/allowedActions", vHost.getCustom(request, null).getTemplate().getPattern());
    }

//    @Test
//    @BenchmarkOptions(benchmarkRounds = 200000, warmupRounds = 100)
//    public void testGetCustomLinkedList() {
//        Request request = new Request(Method.GET, "subscribers/48yh/charges/runBilling");
//        assertEquals("subscribers/{subscriberId}/charges/runBilling", vHost.getCustom(request, null).getTemplate().getPattern());
//    }
//
//    @Test
//    @BenchmarkOptions(benchmarkRounds = 200000, warmupRounds = 100)
//    public void testGetCustomBasicFun() {
//        Request request = new Request(Method.GET, "subscribers/48yh/charges/runBilling");
//        assertEquals("subscribers/{subscriberId}/charges/runBilling", vHost.getOriginalCustom(request, null).getTemplate().getPattern());
//    }
    /*@Test
    @Ignore
    public void testGetCustomComparison() throws Exception {
        Request request = new Request(Method.GET, "subscribers/123qwp/calls/search");
        Route route = null;
        long start = System.currentTimeMillis();
        for (int i = 0; i < 200000; i++) {
            route = vHost.getCustom(request, null);
        }
        long finish = System.currentTimeMillis();
        long result = finish - start;
        Logger logger = Logger.getLogger(VirtualHostWithPSMatchingTest.class.getName());
        logger.info("Execution time " + result + " ms");
        String s = route.getTemplate().getPattern();
        assertNotNull(route);
        assertEquals("subscribers/{subscriberId}/calls/search", s);
        start = System.currentTimeMillis();

        for (int i = 0; i < 200000; i++) {
            route = vHost.getOriginalCustom(request, null);
        }

        finish = System.currentTimeMillis();
        result = finish - start;
        logger.info("Execution time " + result + " ms");
        s = route.getTemplate().getPattern();
        assertNotNull(route);
        assertEquals("subscribers/{subscriberId}/calls/search", s);
        //route = vHost.getCustom(request, null);
        //String s = route.getTemplate().getPattern();
        //assertNotNull(route);
        //assertEquals("subscribers/{subscriberId}/services/{serviceId}/allowedActionsBis",s);
        //Template t = route.getTemplate();

        //String s = route.getTemplate().getPattern();
    }*/
}