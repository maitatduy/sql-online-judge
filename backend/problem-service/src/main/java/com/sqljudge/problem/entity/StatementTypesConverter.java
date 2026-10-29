package com.sqljudge.problem.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class StatementTypesConverter implements AttributeConverter<Set<StatementType>, String> {

    private static final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(Set<StatementType> statementTypes) {
        return EnumSet.copyOf(statementTypes).stream()
                .map(StatementType::name)
                .collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public Set<StatementType> convertToEntityAttribute(String column) {
        return Arrays.stream(column.split(SEPARATOR))
                .map(StatementType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(StatementType.class)));
    }
}
