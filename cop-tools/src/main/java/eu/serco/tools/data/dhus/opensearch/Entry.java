package eu.serco.tools.data.dhus.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Entry {
    @JsonProperty("title")
    public String title;

    @JsonProperty("id")
    public String id;

    @JsonProperty("date")
    public EntryDate[] dates;
 }
