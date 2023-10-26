package org.tonvanbart.wikipedia.connect;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WikiUpdate {
    private Boolean bot;
    private Integer sizeOld;
    private Integer sizeNew;
    private String timestamp;
    private String user;
    private String title;
    private String comment;
}
