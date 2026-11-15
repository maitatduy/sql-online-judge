package com.sqljudge.learning.repository;

import java.util.UUID;

public record QuizOptionRow(
        UUID questionKey,
        String prompt,
        boolean multiple,
        UUID optionKey,
        String label,
        boolean correct) {}
