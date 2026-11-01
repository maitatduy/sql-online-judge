package com.sqljudge.judge.sandbox;

import java.util.List;

public record DatasetSnapshot(List<SnapshotTable> tables) {

    public DatasetSnapshot {
        tables = List.copyOf(tables);
    }
}
