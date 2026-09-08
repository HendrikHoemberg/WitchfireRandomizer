package dev.hendrikhoemberg.witchfirerandomizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MysteriumTier(
    int level,
    String effect,
    List<String> charismata,
    List<String> requirements
) {}
