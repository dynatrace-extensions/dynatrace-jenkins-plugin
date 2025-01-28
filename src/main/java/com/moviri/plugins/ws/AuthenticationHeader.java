package com.moviri.plugins.ws;

import com.moviri.plugins.config.DynatraceConfiguration;
import org.apache.hc.core5.http.Header;

// Authentication header that includes the Dynatrace API Token
public class AuthenticationHeader implements Header {
    @Override
    public boolean isSensitive() {
        return true;
    }

    @Override
    public String getName() {
        return "Authorization";
    }

    @Override
    public String getValue() {
        return "Api-Token " + getDynatraceConfiguration().getApiToken();
    }

    protected DynatraceConfiguration getDynatraceConfiguration() {
        return DynatraceConfiguration.get();
    }
}