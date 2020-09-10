package org.kascoder.clean4m2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class Configuration {
    private final Set<String> paths;

    @JsonCreator
    public Configuration(@JsonProperty("paths") Set<String> paths) {
        this.paths = paths;
    }

    public Set<String> getPaths() {
        return paths;
    }
}
