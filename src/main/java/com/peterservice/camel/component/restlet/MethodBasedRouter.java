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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple router that routes requests to target Restlets based on method. 
 * 
 * @version 
 */
class MethodBasedRouter extends Restlet {
    private static final Logger LOG = LoggerFactory.getLogger(MethodBasedRouter.class);    
    private final String uriPattern;    
    private final Map<Method, Restlet> routes = new ConcurrentHashMap<Method, Restlet>();    
    private final AtomicBoolean isFlagAttached = new AtomicBoolean(false);

    MethodBasedRouter(final String uriPattern) {
    	super();
        this.uriPattern = uriPattern;
    }
    
    @Override
    /**
     * handling routes
     */
    public void handle(final Request request, final Response response) {
        final Method method = request.getMethod();        
        LOG.debug("MethodRouter ({}) received request method: {}", uriPattern, method);
        
        final Restlet target = routes.get(method);
        if (target == null) {
            LOG.debug("No route for request method: {}", method);
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } else {
            target.handle(request, response);
        }
    }

    /**
     * adds route
     * @param method Method to execute when handling a call
     * @param target Context and lifecycle support
     */
    public void addRoute(final Method method, final Restlet target) {
        routes.put(method, target);
    }
  
    /**
     * removes route
     * @param method Method to execute when handling a call
     */
    public void removeRoute(final Method method) {
        routes.remove(method);
    }

    /**
     * Checks whether routes are present
     * @return routes are empty or not
     */
    public boolean hasRoutes() {
        return !routes.isEmpty();
    }
    
    /**
     * This method does "test-and-set" on the underlying flag that indicates
     * whether this router restlet has been attached to a server or not.  It 
     * is the caller's responsibility to perform the "attach" when this method 
     * returns false. 
     * 
     * @return true only this method is called the first time.
     */
    public boolean isFlagBeingAttached() {
        return isFlagAttached.getAndSet(true);
    }
    
    /**
     * 
     * @return uriPattern
     */
    public String getUriPattern() {
        return uriPattern;
    }

    /**
     * 
     * @return routes Map
     */
	public Map<Method, Restlet> getRoutes() {
		return routes;
	}

	/**
	 * 
	 * @return isFlagAttached value
	 */
	public AtomicBoolean getIsFlagAttached() {
		return isFlagAttached;
	}
}
