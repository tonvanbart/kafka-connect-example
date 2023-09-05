package org.tonvanbart.wikipedia.eventstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditEventMeta {

    private String uri;

    private String domain;

    private String topic;

    private int partition;

    private long offset;

}
