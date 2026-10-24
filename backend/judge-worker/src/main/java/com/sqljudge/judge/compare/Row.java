package com.sqljudge.judge.compare;

import java.util.List;

public record Row(List<Cell> cells) {

    public Row {
        cells = List.copyOf(cells);
    }
}
