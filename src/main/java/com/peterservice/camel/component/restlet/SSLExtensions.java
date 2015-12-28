package com.peterservice.camel.component.restlet;


public class SSLExtensions {
    /* Peter-Service defined extensions for SSL */
    private String sslContextFactory;
    private String keystorePath;
    private String keystorePassword;
    private String trustStorePassword;
    private String trustStorePath;
    private String keyPassword;
    private String keystoreType;
    private String certAlgorithm;
    private String sslProtocol;
    private boolean needClientAuthentication;
    private boolean wantClientAuthentication;

    public boolean isWantClientAuthentication() {
        return wantClientAuthentication;
    }

    public void setWantClientAuthentication(boolean wantClientAuthentication) {
        this.wantClientAuthentication = wantClientAuthentication;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public String getSslContextFactory() {
        return sslContextFactory;
    }

    public void setSslContextFactory(String sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
    }

    public boolean isNeedClientAuthentication() {
        return needClientAuthentication;
    }

    public void setNeedClientAuthentication(boolean needClientAuthentication) {
        this.needClientAuthentication = needClientAuthentication;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getCertAlgorithm() {
        return certAlgorithm;
    }

    public void setCertAlgorithm(String certAlgorithm) {
        this.certAlgorithm = certAlgorithm;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }
    
    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }
}
