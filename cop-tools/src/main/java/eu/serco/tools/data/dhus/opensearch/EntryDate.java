package eu.serco.tools.data.dhus.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntryDate {

    @JsonProperty("name")
    public String name;

    @JsonProperty("content")
    public String content;
}

