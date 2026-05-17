package com.metalexplorer.domain.tag;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagCooccurrence {

    private String tagA;
    private String tagB;
    private int count;
}
