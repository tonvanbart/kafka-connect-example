package org.tonvanbart.wikipedia.eventstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditEvent {

    private EditEventMeta meta;

    private String user;

    private String comment;

    private String title;

    private Boolean bot;

}
