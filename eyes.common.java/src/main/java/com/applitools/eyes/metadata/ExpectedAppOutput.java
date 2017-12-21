
package com.applitools.eyes.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "tag",
    "image",
    "thumbprint",
    "occurredAt",
    "annotations"
})
public class ExpectedAppOutput {

    @JsonProperty("tag")
    private String tag;
    @JsonProperty("image")
    private Image image;
    @JsonProperty("thumbprint")
    private Image thumbprint;
    @JsonProperty("occurredAt")
    private String occurredAt;
    @JsonProperty("annotations")
    private Annotations annotations;

    @JsonProperty("tag")
    public String getTag() {
        return tag;
    }

    @JsonProperty("tag")
    public void setTag(String tag) {
        this.tag = tag;
    }

    @JsonProperty("image")
    public Image getImage() {
        return image;
    }

    @JsonProperty("image")
    public void setImage(Image image) {
        this.image = image;
    }

    @JsonProperty("thumbprint")
    public Image getThumbprint() {
        return thumbprint;
    }

    @JsonProperty("thumbprint")
    public void setThumbprint(Image thumbprint) {
        this.thumbprint = thumbprint;
    }

    @JsonProperty("occurredAt")
    public String getOccurredAt() {
        return occurredAt;
    }

    @JsonProperty("occurredAt")
    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    @JsonProperty("annotations")
    public Annotations getAnnotations() {
        return annotations;
    }

    @JsonProperty("annotations")
    public void setAnnotations(Annotations annotations) {
        this.annotations = annotations;
    }

}
