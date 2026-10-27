package com.sqljudge.judge.compare;

import com.sqljudge.judge.compare.Cell.BooleanCell;
import com.sqljudge.judge.compare.Cell.BytesCell;
import com.sqljudge.judge.compare.Cell.NonFiniteNumberCell;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TemporalCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import java.util.Comparator;

final class RowOrdering implements Comparator<Row> {

    static final RowOrdering INSTANCE = new RowOrdering();

    private RowOrdering() {}

    @Override
    public int compare(Row left, Row right) {
        for (int i = 0; i < left.cells().size(); i++) {
            int result = compareCells(left.cells().get(i), right.cells().get(i));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private int compareCells(Cell left, Cell right) {
        int byRank = Integer.compare(left.rank(), right.rank());
        return byRank != 0 ? byRank : compareSameKind(left, right);
    }

    private int compareSameKind(Cell left, Cell right) {
        if (left instanceof NumberCell number) {
            return number.value().compareTo(((NumberCell) right).value());
        }
        if (left instanceof NonFiniteNumberCell number) {
            return number.value().compareTo(((NonFiniteNumberCell) right).value());
        }
        if (left instanceof TextCell text) {
            return text.value().compareTo(((TextCell) right).value());
        }
        if (left instanceof BooleanCell flag) {
            return Boolean.compare(flag.value(), ((BooleanCell) right).value());
        }
        if (left instanceof TemporalCell temporal) {
            return compareTemporal(temporal, (TemporalCell) right);
        }
        if (left instanceof BytesCell bytes) {
            return bytes.hex().compareTo(((BytesCell) right).hex());
        }
        return 0;
    }

    private int compareTemporal(TemporalCell left, TemporalCell right) {
        int byKind = left.kind().compareTo(right.kind());
        return byKind != 0 ? byKind : left.iso().compareTo(right.iso());
    }
}
