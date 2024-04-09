package com.learning.openai;

import java.time.LocalDate;
import java.util.List;

public record Person(
        String name,
        LocalDate dateOfBirth,
        int experienceInYears,
        LocalDate dateOfJoining,
        List<String> captaincy) {}
