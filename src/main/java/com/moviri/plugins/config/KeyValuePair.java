package com.moviri.plugins.config;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class KeyValuePair extends AbstractDescribableImpl<KeyValuePair> {

    private final String key;
    private final String value;

    @DataBoundConstructor
    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<KeyValuePair> {
        @Override
        public String getDisplayName() {
            return "Key Value Pair";
        }
    }
}
