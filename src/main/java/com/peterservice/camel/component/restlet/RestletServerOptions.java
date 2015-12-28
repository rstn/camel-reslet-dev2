package com.peterservice.camel.component.restlet;

import java.util.HashMap;
import java.util.Map;


public class RestletServerOptions {
    
    // options that can be set on the restlet server
    private Boolean controllerDaemon;
    private Integer controllerSleepTimeMs;
    private Integer inboundBufferSize;
    private Integer minThreads;
    private Integer maxThreads;
    private Integer workerThreads;
    private Integer maxConnectionsPerHost;
    private Integer maxTotalConnections;
    private Integer outboundBufferSize;
    private Boolean persistingConnections;
    private Boolean pipeliningConnections;
    private Integer threadMaxIdleTimeMs;
    private Boolean useForwardedForHeader;
    private Boolean reuseAddress;
    
    public Boolean getControllerDaemon() {
        return controllerDaemon; 
    }

    public void setControllerDaemon(Boolean controllerDaemon) {
        this.controllerDaemon = controllerDaemon; 
    }

    public Integer getControllerSleepTimeMs() {
        return controllerSleepTimeMs;
    }

    public void setControllerSleepTimeMs(Integer controllerSleepTimeMs) {
        this.controllerSleepTimeMs = controllerSleepTimeMs;
    }

    public Integer getInboundBufferSize() {
        return inboundBufferSize;
    }

    public void setInboundBufferSize(Integer inboundBufferSize) {
        this.inboundBufferSize = inboundBufferSize;
    }

    public Integer getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(Integer maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public Integer getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(Integer maxThreads) {
        this.maxThreads = maxThreads;
    }

    public Integer getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(Integer maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public Integer getMinThreads() {
        return minThreads;
    }

    public void setMinThreads(Integer minThreads) {
        this.minThreads = minThreads;
    }

    public Integer getOutboundBufferSize() {
        return outboundBufferSize;
    }

    public void setOutboundBufferSize(Integer outboundBufferSize) {
        this.outboundBufferSize = outboundBufferSize;
    }

    public Boolean getPersistingConnections() {
        return persistingConnections;
    }

    public void setPersistingConnections(Boolean persistingConnections) {
        this.persistingConnections = persistingConnections;
    }

    public Boolean getPipeliningConnections() {
        return pipeliningConnections;
    }

    public void setPipeliningConnections(Boolean pipeliningConnections) {
        this.pipeliningConnections = pipeliningConnections;
    }

    public Integer getThreadMaxIdleTimeMs() {
        return threadMaxIdleTimeMs;
    }

    public void setThreadMaxIdleTimeMs(Integer threadMaxIdleTimeMs) {
        this.threadMaxIdleTimeMs = threadMaxIdleTimeMs;
    }

    public Boolean getUseForwardedForHeader() {
        return useForwardedForHeader;
    }

    public void setUseForwardedForHeader(Boolean useForwardedForHeader) {
        this.useForwardedForHeader = useForwardedForHeader;
    }

    public Boolean getReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(Boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }
        
    public Integer getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(Integer workerThreads) {
        this.workerThreads = workerThreads;
    }
}
