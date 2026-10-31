package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "datasets",
        indexes = {
            @Index(name = "uk_datasets_slug", columnList = "slug", unique = true),
            @Index(name = "idx_datasets_owner_id_updated_at", columnList = "owner_id, updated_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class Dataset extends BaseEntity {

    @Column(name = "owner_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerId;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number")
    private List<DatasetVersion> versions = new ArrayList<>();

    public DatasetVersion latestVersion() {
        return versions.get(versions.size() - 1);
    }

    public DatasetVersion addDraftFromLatest() {
        DatasetVersion latest = latestVersion();
        DatasetVersion draft = addVersion(latest.getNumber() + 1);
        draft.copyFrom(latest);
        return draft;
    }

    public DatasetVersion addVersion(int versionNumber) {
        DatasetVersion datasetVersion = new DatasetVersion(this, versionNumber);
        versions.add(datasetVersion);
        return datasetVersion;
    }
}
