package eu.serco.tools.data.dhus.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Feed {

    @JsonProperty("opensearch:totalResults")
    public String totalresults;

    @JsonProperty("entry")
    public Entry[] entries;

}
