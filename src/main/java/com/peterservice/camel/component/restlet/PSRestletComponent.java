/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.peterservice.camel.component.restlet;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.HeaderFilterStrategyComponent;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.UnsafeUriCharactersEncoder;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;
import org.restlet.util.Series;
import org.restlet.util.ServerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Camel component embedded Restlet that produces and consumes exchanges.
 * 
 * @version
 */
public class PSRestletComponent extends HeaderFilterStrategyComponent {
    private static final Logger LOG = LoggerFactory.getLogger(PSRestletComponent.class);

    private final Map<String, Server> servers = new HashMap<String, Server>();
    private final Map<String, MethodBasedRouter> routers = new HashMap<String, MethodBasedRouter>();
    private final Component component;
    private final RestletServerOptions serverOptions = new RestletServerOptions();
    private final SSLExtensions sslExtensions = new SSLExtensions();
        
    public PSRestletComponent() {
        this(new Component());
    }

    public PSRestletComponent(Component component) {
        // Allow the Component to be injected, so that the RestletServlet may be
        // configured within a webapp
        VirtualHostWithPSMatching vHost = new VirtualHostWithPSMatching(component.getDefaultHost());
        component.setDefaultHost(vHost);
        this.component = component;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        RestletEndpoint result = new RestletEndpoint(this, remaining);
        setEndpointHeaderFilterStrategy(result);
        setProperties(result, parameters);
        // set the endpoint uri according to the parameter
        result.updateEndpointUri();

        // construct URI so we can use it to get the splitted information
        URI u = new URI(remaining);
        String protocol = u.getScheme();

        String uriPattern = u.getPath();
        if (parameters.size() > 0) {
            uriPattern = uriPattern + "?" + URISupport.createQueryString(parameters);
        }

        int port = 0;
        String host = u.getHost();
        if (u.getPort() > 0) {
            port = u.getPort();
        }

        result.setProtocol(protocol);
        result.setUriPattern(uriPattern);
        result.setHost(host);
        if (port > 0) {
            result.setPort(port);
        }

        return result;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        component.start();
    }

    @Override
    protected void doStop() throws Exception {
        component.stop();
        // component stop will stop servers so we should clear our list as well
        servers.clear();
        // routers map entries are removed as consumer stops and servers map
        // is not touch so to keep in sync with component's servers
        super.doStop();
    }

    @Override
    protected boolean useIntrospectionOnEndpoint() {
        // we invoke setProperties ourselves so we can construct "user" uri on
        // on the remaining parameters
        return false;
    }

    public void connect(RestletConsumer consumer) throws Exception {
        RestletEndpoint endpoint = consumer.getEndpoint();
        addServerIfNecessary(endpoint);

        // if restlet servlet server is created, the offsetPath is set in component context
        // see http://restlet.tigris.org/issues/show_bug.cgi?id=988
        String offsetPath = (String) this.component.getContext()
                .getAttributes().get("org.restlet.ext.servlet.offsetPath");

       this.component.getContext().getAttributes().put("org.restlet.autoWire","false");
        
        if (endpoint.getUriPattern() != null && endpoint.getUriPattern().length() > 0) {
            attachUriPatternToRestlet(offsetPath, endpoint.getUriPattern(), endpoint, consumer.getRestlet());
        }

        if (endpoint.getRestletUriPatterns() != null) {
            for (String uriPattern : endpoint.getRestletUriPatterns()) {
                attachUriPatternToRestlet(offsetPath, uriPattern, endpoint, consumer.getRestlet());
            }
        }
    }

    public void disconnect(RestletConsumer consumer) throws Exception {
        RestletEndpoint endpoint = consumer.getEndpoint();

        List<MethodBasedRouter> routesToRemove = new ArrayList<MethodBasedRouter>();

        String pattern = decodePattern(endpoint.getUriPattern());
        if (pattern != null && !pattern.isEmpty()) {
            routesToRemove.add(getMethodRouter(pattern, false));
        }

        if (endpoint.getRestletUriPatterns() != null) {
            for (String uriPattern : endpoint.getRestletUriPatterns()) {
                routesToRemove.add(getMethodRouter(uriPattern, false));
            }
        }

        for (MethodBasedRouter router : routesToRemove) {
            if (endpoint.getRestletMethods() != null) {
                Method[] methods = endpoint.getRestletMethods();
                for (Method method : methods) {
                    if (router!=null && method!=null) {
                        router.removeRoute(method);
                    }
                }
            } else {
                router.removeRoute(endpoint.getRestletMethod());
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Detached restlet uriPattern: {} method: {}", router.getUriPattern(),endpoint.getRestletMethod());
            }

            // remove router if its no longer in use
            if (!router.hasRoutes()) {
                deattachUriPatternFrimRestlet(router.getUriPattern(), router);
                if (!router.isStopped()) {
                    router.stop();
                }
                routers.remove(router.getUriPattern());
            }
        }
    }

    private MethodBasedRouter getMethodRouter(String uriPattern, boolean addIfEmpty) {
        synchronized (routers) {
            MethodBasedRouter result = routers.get(uriPattern);
            if (result == null && addIfEmpty) {
                result = new MethodBasedRouter(uriPattern);
                LOG.debug("Added method based router: {}", result);
                routers.put(uriPattern, result);
            }
            return result;
        }
    }
    
    protected Server createServer(RestletEndpoint endpoint) {
        return new Server(component.getContext().createChildContext(), Protocol.valueOf(endpoint.getProtocol()), endpoint.getPort());
    }

    protected void addServerIfNecessary(RestletEndpoint endpoint) throws Exception {
        String key = buildKey(endpoint);
        Server server;
        synchronized (servers) {
            server = servers.get(key);
            if (server == null) {
                server = createServer(endpoint);
                component.getServers().add(server);

                // Add any Restlet server parameters that were included
                Series<Parameter> params = server.getContext().getParameters();

                
                // Here it is better to use reflection to call appropriate methods
                // of RestletServerOptions class in runtime. Method names can be
                // generated from HashMap keys. HashMap collections are private fields
                // of RestletServerOptions class
                // a code will be similar to below
                /*
                   Class c = <method which returns RestletServerOptions class>; 
                   Object obj = c.newInstance();
                   
                   Class noparams[] = {};
                   // here organize cycle for each of two collections
                   {
                       Method method = c.getDeclaredMethod(<method name>, noparams);                   
                       Boolean (or Integer) value = (Boolean)(or Integer)method.invoke(obj,null)
                       if (value != null) {
                       ..
                       ..
                   }
                 */
                
                if (serverOptions.getControllerDaemon() != null) {
                    params.add("controllerDaemon", serverOptions.getControllerDaemon().toString());
                }
                if (serverOptions.getControllerSleepTimeMs() != null) {
                    params.add("controllerSleepTimeMs", serverOptions.getControllerSleepTimeMs().toString());
                }
                if (serverOptions.getInboundBufferSize() != null) {
                    params.add("inboundBufferSize", serverOptions.getInboundBufferSize().toString());
                }
                if (serverOptions.getMinThreads() != null) {
                    params.add("minThreads", serverOptions.getMinThreads().toString());
                }
                if (serverOptions.getMaxThreads() != null) {
                    params.add("maxThreads", serverOptions.getMaxThreads().toString());
                }
                if (serverOptions.getWorkerThreads() != null) {
                    params.add("workerThreads", serverOptions.getWorkerThreads().toString());
                }
                if (serverOptions.getMaxConnectionsPerHost() != null) {
                    params.add("maxConnectionsPerHost", serverOptions.getMaxConnectionsPerHost().toString());
                }
                if (serverOptions.getMaxTotalConnections() != null) {
                    params.add("maxTotalConnections", serverOptions.getMaxTotalConnections().toString());
                }
                if (serverOptions.getOutboundBufferSize() != null) {
                    params.add("outboundBufferSize", serverOptions.getOutboundBufferSize().toString());
                }
                if (serverOptions.getPersistingConnections() != null) {
                    params.add("persistingConnections", serverOptions.getPersistingConnections().toString());
                }
                if (serverOptions.getPipeliningConnections() != null) {
                    params.add("pipeliningConnections", serverOptions.getPipeliningConnections().toString());
                }
                if (serverOptions.getThreadMaxIdleTimeMs() != null) {
                    params.add("threadMaxIdleTimeMs", serverOptions.getThreadMaxIdleTimeMs().toString());
                }
                if (serverOptions.getUseForwardedForHeader() != null) {
                    params.add("useForwardedForHeader", serverOptions.getUseForwardedForHeader().toString());
                }
                if (serverOptions.getReuseAddress() != null) {
                    params.add("reuseAddress", serverOptions.getReuseAddress().toString());
                }

                if (sslExtensions.isWantClientAuthentication()) {
                    params.add("wantClientAuthentication", String.valueOf(sslExtensions.isWantClientAuthentication()));
                }
                if (sslExtensions.isNeedClientAuthentication()) {
                    params.add("needClientAuthentication", String.valueOf(sslExtensions.isNeedClientAuthentication()));
                }
                if (sslExtensions.getKeyPassword() != null) {
                    params.add("keyPassword", sslExtensions.getKeyPassword());
                }
                if (sslExtensions.getKeystorePassword() != null) {
                    params.add("keystorePassword", sslExtensions.getKeystorePassword());
                }
                if (sslExtensions.getKeystorePath() != null) {
                    params.add("keystorePath", sslExtensions.getKeystorePath());
                }
                if (sslExtensions.getKeystoreType() != null) {
                    params.add("keystoreType", sslExtensions.getKeystoreType());
                }
                if (sslExtensions.getSslContextFactory() != null) {
                    params.add("sslContextFactory", sslExtensions.getSslContextFactory());
                }
                if (sslExtensions.getCertAlgorithm() != null) {
                    params.add("certAlgorithm", sslExtensions.getCertAlgorithm());
                }
                if (sslExtensions.getSslProtocol() != null) {
                    params.add("sslProtocol", sslExtensions.getSslProtocol());
                }
                if (sslExtensions.getTrustStorePassword() != null) {
                    params.add("truststorePassword", sslExtensions.getTrustStorePassword());
                }
                if (sslExtensions.getTrustStorePath() != null) {
                    params.add("truststorePath", sslExtensions.getTrustStorePath());
                }

                LOG.debug("Setting parameters: {} to server: {}", params, server);
                server.getContext().setParameters(params);

                servers.put(key, server);
                LOG.debug("Added server: {}", key);
                server.start();
            }
        }
    }

    private static String buildKey(RestletEndpoint endpoint) {
        return endpoint.getHost() + ":" + endpoint.getPort();
    }

    private void attachUriPatternToRestlet(String offsetPath, String uriPattern, RestletEndpoint endpoint, Restlet target) throws Exception {
        Restlet neededTarget = target;
        String neededUriPattern = decodePattern(uriPattern);
        MethodBasedRouter router = getMethodRouter(neededUriPattern, true);

        Map<String, String> realm = endpoint.getRestletRealm();
        if (realm != null && realm.size() > 0) {
            ChallengeAuthenticator guard = new ChallengeAuthenticator(component.getContext()
                .createChildContext(), ChallengeScheme.HTTP_BASIC, "Camel-Restlet Endpoint Realm");
            MapVerifier verifier = new MapVerifier();
            for (Map.Entry<String, String> entry : realm.entrySet()) {
                verifier.getLocalSecrets().put(entry.getKey(), entry.getValue().toCharArray());
            }
            guard.setVerifier(verifier);
            guard.setNext(target);
            neededTarget = guard;
            LOG.debug("Target has been set to guard: {}", guard);
        }

        if (endpoint.getRestletMethods() != null) {
            Method[] methods = endpoint.getRestletMethods();
            for (Method method : methods) {
                router.addRoute(method, neededTarget);
                LOG.debug("Attached restlet uriPattern: {} method: {}", neededUriPattern, method);
            }
        } else {
            Method method = endpoint.getRestletMethod();
            router.addRoute(method, neededTarget);
            LOG.debug("Attached restlet uriPattern: {} method: {}", neededUriPattern, method);
        }

        if (!router.isFlagBeingAttached()) {
            String attachmentString;
            if (offsetPath == null) {
                attachmentString = neededUriPattern;
            } else {
                attachmentString = offsetPath + neededUriPattern;
            }
            component.getDefaultHost().attach(attachmentString, router);
            LOG.debug("Attached methodRouter uriPattern: {}", neededUriPattern);
        }

        if (!router.isStarted()) {
            router.start();
            LOG.debug("Started methodRouter uriPattern: {}", neededUriPattern);
        }
    }

    private void deattachUriPatternFrimRestlet(String uriPattern, Restlet target) throws Exception {
        component.getDefaultHost().detach(target);
        LOG.debug("Deattached methodRouter uriPattern: {}", uriPattern);
    }

    @Deprecated
    @Override
    protected String preProcessUri(String uri) {
        // If the URI was not valid (i.e. contains '{' and '}'
        // it was most likely encoded by normalizeEndpointUri in DefaultCamelContext.getEndpoint(String)
        return UnsafeUriCharactersEncoder.encode(uri.replaceAll("%7B", "(").replaceAll("%7D", ")"));
    }
    
    private static String decodePattern(String pattern) {
        if (pattern == null) {
            return null;
        } else {
            return pattern.replaceAll("\\(", "{").replaceAll("\\)", "}");
        }
    }
}
