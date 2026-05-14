package com.metalexplorer.domain.tag;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tag_cooccurrence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagCooccurrence {

    @EmbeddedId
    private TagCooccurrenceId id;

    @Column(nullable = false)
    private int count;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TagCooccurrenceId implements java.io.Serializable {
        @Column(name = "tag_a", length = 100)
        private String tagA;

        @Column(name = "tag_b", length = 100)
        private String tagB;
    }
}
