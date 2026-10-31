package com.sqljudge.problem.service;

import com.sqljudge.problem.entity.DatasetColumn;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVersion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class DatasetChecksum {

    private static final String ALGORITHM = "SHA-256";

    private DatasetChecksum() {}

    public static String of(DatasetVersion version) {
        StringBuilder canonical = new StringBuilder();
        for (DatasetTable table : version.getTables()) {
            canonical.append("table|").append(table.getName()).append('\n');
            for (DatasetColumn column : table.getColumns()) {
                canonical
                        .append("column|")
                        .append(column.getName())
                        .append('|')
                        .append(column.getLogicalType())
                        .append('|')
                        .append(column.isPrimaryKey())
                        .append('|')
                        .append(column.isNullable())
                        .append('|')
                        .append(column.getReferencesTable())
                        .append('|')
                        .append(column.getReferencesColumn())
                        .append('\n');
            }
        }
        for (DatasetVariant variant : version.getVariants()) {
            canonical
                    .append("variant|")
                    .append(variant.getName())
                    .append('|')
                    .append(variant.isPublic())
                    .append('\n');
            for (DatasetTable table : version.getTables()) {
                String csv = variant.findTable(table.getName()).orElseThrow().getCsv();
                canonical
                        .append("data|")
                        .append(table.getName())
                        .append('|')
                        .append(csv.length())
                        .append('\n')
                        .append(csv)
                        .append('\n');
            }
        }
        return sha256(canonical.toString());
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance(ALGORITHM)
                                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM + " is not available", exception);
        }
    }
}
