package com.moviri.plugins.config;

import hudson.Extension;
import jenkins.YesNoMaybe;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.HashMap;
import java.util.Map;

@Extension(dynamicLoadable = YesNoMaybe.NO)
public class ValueStore extends GlobalConfiguration {

    private final Map<String, Integer> pipelineToLastBuildId = new HashMap<>();

    public ValueStore() {
        load();
    }

    /**
     * This method must only be called from testing classes, as it avoids to attach the ValueStore
     * to the Jenkins instance.
     *
     * @return Instance of ValueStore detached from the Jenkins engine
     */
    protected static ValueStore createForTesting() {
        return new ValueStore(true);
    }
    private ValueStore(boolean isTesting) {}

    public static ValueStore get() {
        return (ValueStore) Jenkins.get().getDescriptor(ValueStore.class);
    }

    public int getLastBuildId(String pipeline) {
        if (this.pipelineToLastBuildId.containsKey(pipeline)) {
            return this.pipelineToLastBuildId.get(pipeline);
        }
        return 0;
    }

    public void setLastBuildId(String pipeline, int lastBuildId) {
        this.pipelineToLastBuildId.put(pipeline, lastBuildId);
        save();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        // Not editable by a user
        return true;
    }
}
