package com.learning.ai.domain.response;

import java.util.List;

public record AICustomerSupportResponse(String name, int age, List<String> records, List<String> trophiesWon) {}
