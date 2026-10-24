package com.sqljudge.judge.compare;

import java.math.BigDecimal;

public sealed interface Cell {

    String display();

    record NullCell() implements Cell {
        @Override
        public String display() {
            return null;
        }
    }

    record NumberCell(BigDecimal value) implements Cell {
        @Override
        public String display() {
            return value.toPlainString();
        }
    }

    record NonFiniteNumberCell(String value) implements Cell {
        @Override
        public String display() {
            return value;
        }
    }

    record TextCell(String value) implements Cell {
        @Override
        public String display() {
            return value;
        }
    }

    record BooleanCell(boolean value) implements Cell {
        @Override
        public String display() {
            return Boolean.toString(value);
        }
    }

    record TemporalCell(TemporalKind kind, String iso) implements Cell {
        @Override
        public String display() {
            return iso;
        }
    }

    record BytesCell(String hex) implements Cell {
        @Override
        public String display() {
            return "\\x" + hex;
        }
    }
}
