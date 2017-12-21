
package com.applitools.eyes.metadata;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "matchLevel",
    "ignore",
    "strict",
    "content",
    "layout",
    "floating",
    "splitTopHeight",
    "splitBottomHeight",
    "ignoreCaret",
    "scale",
    "remainder"
})
public class DefaultMatchSettings {

    @JsonProperty("matchLevel")
    private String matchLevel;
    @JsonProperty("ignore")
    private List<Object> ignore = null;
    @JsonProperty("strict")
    private List<Object> strict = null;
    @JsonProperty("content")
    private List<Object> content = null;
    @JsonProperty("layout")
    private List<Object> layout = null;
    @JsonProperty("floating")
    private List<Object> floating = null;
    @JsonProperty("splitTopHeight")
    private Integer splitTopHeight;
    @JsonProperty("splitBottomHeight")
    private Integer splitBottomHeight;
    @JsonProperty("ignoreCaret")
    private Boolean ignoreCaret;
    @JsonProperty("scale")
    private Integer scale;
    @JsonProperty("remainder")
    private Integer remainder;

    @JsonProperty("matchLevel")
    public String getMatchLevel() {
        return matchLevel;
    }

    @JsonProperty("matchLevel")
    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
    }

    @JsonProperty("ignore")
    public List<Object> getIgnore() {
        return ignore;
    }

    @JsonProperty("ignore")
    public void setIgnore(List<Object> ignore) {
        this.ignore = ignore;
    }

    @JsonProperty("strict")
    public List<Object> getStrict() {
        return strict;
    }

    @JsonProperty("strict")
    public void setStrict(List<Object> strict) {
        this.strict = strict;
    }

    @JsonProperty("content")
    public List<Object> getContent() {
        return content;
    }

    @JsonProperty("content")
    public void setContent(List<Object> content) {
        this.content = content;
    }

    @JsonProperty("layout")
    public List<Object> getLayout() {
        return layout;
    }

    @JsonProperty("layout")
    public void setLayout(List<Object> layout) {
        this.layout = layout;
    }

    @JsonProperty("floating")
    public List<Object> getFloating() {
        return floating;
    }

    @JsonProperty("floating")
    public void setFloating(List<Object> floating) {
        this.floating = floating;
    }

    @JsonProperty("splitTopHeight")
    public Integer getSplitTopHeight() {
        return splitTopHeight;
    }

    @JsonProperty("splitTopHeight")
    public void setSplitTopHeight(Integer splitTopHeight) {
        this.splitTopHeight = splitTopHeight;
    }

    @JsonProperty("splitBottomHeight")
    public Integer getSplitBottomHeight() {
        return splitBottomHeight;
    }

    @JsonProperty("splitBottomHeight")
    public void setSplitBottomHeight(Integer splitBottomHeight) {
        this.splitBottomHeight = splitBottomHeight;
    }

    @JsonProperty("ignoreCaret")
    public Boolean getIgnoreCaret() {
        return ignoreCaret;
    }

    @JsonProperty("ignoreCaret")
    public void setIgnoreCaret(Boolean ignoreCaret) {
        this.ignoreCaret = ignoreCaret;
    }

    @JsonProperty("scale")
    public Integer getScale() {
        return scale;
    }

    @JsonProperty("scale")
    public void setScale(Integer scale) {
        this.scale = scale;
    }

    @JsonProperty("remainder")
    public Integer getRemainder() {
        return remainder;
    }

    @JsonProperty("remainder")
    public void setRemainder(Integer remainder) {
        this.remainder = remainder;
    }

}
