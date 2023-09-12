package org.tonvanbart.wikipedia.eventstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

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

    private Map<String, Integer> length;

    public Integer oldLength() {
        return length == null || length.get("old") == null ? 0 : length.get("old");
    }

    public Integer newLength() {
        return length == null || length.get("new") == null ? 0 : length.get("new");
    }

}
