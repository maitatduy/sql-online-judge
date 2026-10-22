package com.sqljudge.judge.compare;

import com.sqljudge.judge.compare.Cell.BooleanCell;
import com.sqljudge.judge.compare.Cell.BytesCell;
import com.sqljudge.judge.compare.Cell.NonFiniteNumberCell;
import com.sqljudge.judge.compare.Cell.NullCell;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TemporalCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import java.util.Comparator;
import java.util.List;

final class RowOrdering implements Comparator<Row> {

    static final RowOrdering INSTANCE = new RowOrdering();

    private static final List<Class<? extends Cell>> KIND_ORDER =
            List.of(
                    NullCell.class,
                    NumberCell.class,
                    NonFiniteNumberCell.class,
                    TextCell.class,
                    BooleanCell.class,
                    TemporalCell.class,
                    BytesCell.class);

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
        int byKind = Integer.compare(kindRank(left), kindRank(right));
        if (byKind != 0) {
            return byKind;
        }
        return compareSameKind(left, right);
    }

    private int compareSameKind(Cell left, Cell right) {
        if (left instanceof NumberCell number) {
            return number.value().compareTo(((NumberCell) right).value());
        }
        if (left instanceof BooleanCell flag) {
            return Boolean.compare(flag.value(), ((BooleanCell) right).value());
        }
        if (left instanceof TemporalCell temporal) {
            TemporalCell other = (TemporalCell) right;
            int byKind = temporal.kind().compareTo(other.kind());
            return byKind != 0 ? byKind : temporal.iso().compareTo(other.iso());
        }
        if (left instanceof NullCell) {
            return 0;
        }
        return left.display().compareTo(right.display());
    }

    private int kindRank(Cell cell) {
        return KIND_ORDER.indexOf(cell.getClass());
    }
}
