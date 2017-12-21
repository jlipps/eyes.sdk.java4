
package com.applitools.eyes.metadata;

import java.util.List;

import com.applitools.eyes.FloatingMatchSettings;
import com.applitools.eyes.Region;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "floating",
    "ignore",
    "strict",
    "content",
    "layout",
    "remarks",
    "mismatching"
})
public class Annotations {

    @JsonProperty("floating")
    private List<FloatingMatchSettings> floating = null;
    @JsonProperty("ignore")
    private List<Region> ignore = null;
    @JsonProperty("strict")
    private List<Region> strict = null;
    @JsonProperty("content")
    private List<Region> content = null;
    @JsonProperty("layout")
    private List<Region> layout = null;
    @JsonProperty("remarks")
    private List<Region> remarks = null;
    @JsonProperty("mismatching")
    private List<Region> mismatching = null;

    @JsonProperty("floating")
    public List<FloatingMatchSettings> getFloating() {
        return floating;
    }

    @JsonProperty("floating")
    public void setFloating(List<FloatingMatchSettings> floating) {
        this.floating = floating;
    }

    @JsonProperty("ignore")
    public List<Region> getIgnore() {
        return ignore;
    }

    @JsonProperty("ignore")
    public void setIgnore(List<Region> ignore) {
        this.ignore = ignore;
    }

    @JsonProperty("strict")
    public List<Region> getStrict() {
        return strict;
    }

    @JsonProperty("strict")
    public void setStrict(List<Region> strict) {
        this.strict = strict;
    }

    @JsonProperty("content")
    public List<Region> getContent() {
        return content;
    }

    @JsonProperty("content")
    public void setContent(List<Region> content) {
        this.content = content;
    }

    @JsonProperty("layout")
    public List<Region> getLayout() {
        return layout;
    }

    @JsonProperty("layout")
    public void setLayout(List<Region> layout) {
        this.layout = layout;
    }

    @JsonProperty("remarks")
    public List<Region> getRemarks() {
        return remarks;
    }

    @JsonProperty("remarks")
    public void setRemarks(List<Region> remarks) {
        this.remarks = remarks;
    }

    @JsonProperty("mismatching")
    public List<Region> getMismatching() {
        return mismatching;
    }

    @JsonProperty("mismatching")
    public void setMismatching(List<Region> mismatching) {
        this.mismatching = mismatching;
    }

}
