package com.sqljudge.judge.compare;

import java.math.BigDecimal;

public sealed interface Cell {

    int rank();

    record NullCell() implements Cell {
        @Override
        public int rank() {
            return 0;
        }
    }

    record NumberCell(BigDecimal value) implements Cell {
        @Override
        public int rank() {
            return 1;
        }
    }

    record NonFiniteNumberCell(String value) implements Cell {
        @Override
        public int rank() {
            return 2;
        }
    }

    record TextCell(String value) implements Cell {
        @Override
        public int rank() {
            return 3;
        }
    }

    record BooleanCell(boolean value) implements Cell {
        @Override
        public int rank() {
            return 4;
        }
    }

    record TemporalCell(TemporalKind kind, String iso) implements Cell {
        @Override
        public int rank() {
            return 5;
        }
    }

    record BytesCell(String hex) implements Cell {
        @Override
        public int rank() {
            return 6;
        }
    }
}
