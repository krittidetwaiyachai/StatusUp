package xyz.kaijiieow.statusup.core;

import java.util.List;

public record UpgradeDetails(
        String currentGroup,
        String nextGroup,
        String currentGroupDisplay,
        String nextGroupDisplay,
        List<String> costs,
        List<String> requirements,
        boolean canAfford,
        boolean meetsStats,
        boolean isMaxLevel
) {
}