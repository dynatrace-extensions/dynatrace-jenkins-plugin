package com.moviri.plugins.config;

import hudson.Extension;
import jenkins.YesNoMaybe;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import lombok.Getter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

@Getter
@Extension(dynamicLoadable = YesNoMaybe.NO)
public class DynatraceConfiguration extends GlobalConfiguration {

    private String tenant;
    private String apiToken;

    public DynatraceConfiguration() {
        load();
    }

    /**
     * This method must only be called from testing classes, as it avoids to attach the DynatraceConfiguration
     * to the Jenkins instance.
     *
     * @return Instance of DynatraceConfiguration detached from the Jenkins engine
     */
    protected static DynatraceConfiguration createForTesting() {
        return new DynatraceConfiguration(true);
    }
    private DynatraceConfiguration(boolean isTesting) {}

    public static DynatraceConfiguration get() {
        return (DynatraceConfiguration) Jenkins.get().getDescriptor(DynatraceConfiguration.class);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        req.bindJSON(this, json);
        save();
        return true;
    }

    @DataBoundSetter
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @DataBoundSetter
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
}
