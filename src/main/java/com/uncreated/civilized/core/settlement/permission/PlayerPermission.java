package com.uncreated.civilized.core.settlement.permission;

import java.util.UUID;

public record PlayerPermission(UUID playerId, String scoreboardName, AccessLevel accessLevel) {
}
