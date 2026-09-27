package com.uncreated.civilized.core.settlement.permission;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

@Accessors(fluent = true)
public class SettlementPermissions {

   @Getter
   private final UUID settlementId;
   private HashMap<UUID, PlayerPermission> permissions;

   public SettlementPermissions(UUID settlementId, List<PlayerPermission> permissions) {
      this.settlementId = settlementId;
      this.permissions = new HashMap<>();
      for (PlayerPermission permission : permissions)
         this.permissions.put(permission.playerId(), permission);
   }

   public Collection<PlayerPermission> entries() {
      return Collections.unmodifiableCollection(permissions.values());
   }

   private @NotNull Optional<PlayerPermission> getPermissionForPlayer(UUID playerId) {
      return Optional.ofNullable(permissions.get(playerId));
   }

   public void setAccessLevel(Player player, AccessLevel accessLevel) {
      PlayerPermission playerPermission = new PlayerPermission(player.getUUID(), player.getScoreboardName(), accessLevel);
      permissions.put(player.getUUID(), playerPermission);
   }

   public void removeAccess(UUID playerId) {
      permissions.remove(playerId);
   }

   public boolean hasItemPickUpPermission(UUID playerId) {
      return getPermissionForPlayer(playerId).map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.FRIEND))
              .orElse(false);
   }

   public boolean hasOpenDoorsPermission(UUID playerId) {
      return getPermissionForPlayer(playerId).map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.FRIEND))
            .orElse(false);
   }

   public boolean hasGeneralBlockPlacingPermission(UUID playerId) {
      return getPermissionForPlayer(playerId).map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.CITIZEN))
            .orElse(false);
   }

   public boolean hasOpenSelectChestsPermission(UUID playerId) {
      return getPermissionForPlayer(playerId).map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.CITIZEN))
            .orElse(false);
   }

   public boolean hasOpenAllChestsPermission(UUID playerId) {
      return getPermissionForPlayer(playerId)
            .map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.ADMINISTRATOR))
            .orElse(false);
   }

   public boolean hasEditBuildingPermission(UUID playerId) {
      return getPermissionForPlayer(playerId)
            .map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.ADMINISTRATOR))
            .orElse(false);
   }

   public boolean hasCreateBuildingsPermission(UUID playerId) {
      return getPermissionForPlayer(playerId).map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.GOVERNOR))
            .orElse(false);
   }
}
