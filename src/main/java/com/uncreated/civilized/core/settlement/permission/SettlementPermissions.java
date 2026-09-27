package com.uncreated.civilized.core.settlement.permission;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.StoreOperation;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

@Accessors(fluent = true)
public class SettlementPermissions {

   public static final StreamCodec<FriendlyByteBuf, SettlementPermissions> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               SettlementPermissions::settlementId,
               PlayerPermission.STREAM_CODEC.apply(ByteBufCodecs.list()),
               p -> List.copyOf(p.entries()),
               SettlementPermissions::new);

   @Getter
   private final UUID settlementId;
   private final HashMap<UUID, PlayerPermission> permissions;

   public SettlementPermissions(UUID settlementId, List<PlayerPermission> permissions) {
      this.settlementId = settlementId;
      this.permissions = new HashMap<>();
      for (PlayerPermission permission : permissions)
         this.permissions.put(permission.playerId(), permission);
   }

  void copyFrom(SettlementPermissions other) {
      permissions.clear();
      permissions.putAll(other.permissions);
   }

   public Packet toPacket(StoreOperation operation) {
      return new Packet(this, operation);
   }

   public Collection<PlayerPermission> entries() {
      return Collections.unmodifiableCollection(permissions.values());
   }

   private @NotNull Optional<PlayerPermission> getPermissionForPlayer(UUID playerId) {
      return Optional.ofNullable(permissions.get(playerId));
   }

   void setAccessLevel(Player player, AccessLevel accessLevel) {
      setAccessLevel(player.getUUID(), player.getScoreboardName(), accessLevel);
   }

   /** For players who may be offline, the scoreboard name has to specified. */
   void setAccessLevel(UUID playerId, String scoreboardName, AccessLevel accessLevel) {
      permissions.put(playerId, new PlayerPermission(playerId, scoreboardName, accessLevel));
   }

   public Optional<PlayerPermission> find(UUID playerId) {
      return getPermissionForPlayer(playerId);
   }

   public boolean wouldLeaveNoGovernors(UUID playerId, @Nullable AccessLevel newAccessLevel) {
      if (newAccessLevel == AccessLevel.GOVERNOR)
         return false;

      return permissions.values()
            .stream()
            .noneMatch(p -> !p.playerId().equals(playerId) && p.accessLevel() == AccessLevel.GOVERNOR);
   }

   public boolean hasManagePermissionsPermission(UUID playerId) {
      return getPermissionForPlayer(playerId).map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.GOVERNOR))
            .orElse(false);
   }

   void removeAccess(UUID playerId) {
      permissions.remove(playerId);
   }

//   public boolean hasItemPickUpPermission(UUID playerId) {
//      return getPermissionForPlayer(playerId).map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.FRIEND))
//              .orElse(false);
//   }
//
//   public boolean hasOpenDoorsPermission(UUID playerId) {
//      return getPermissionForPlayer(playerId).map(p -> p.accessLevel().isAboveOrEqualTo(AccessLevel.FRIEND))
//            .orElse(false);
//   }

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

   /** Only ever sent from the server. Clients are not allowed to tell the server to change permissions. */
   public record Packet(SettlementPermissions permissions, StoreOperation storeOperation)
         implements CustomPacketPayload {

      public static final Type<Packet> SYNC_TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "settlement_permissions_store_sync"));

      public static final StreamCodec<FriendlyByteBuf, Packet> STREAM_CODEC =
            StreamCodec.composite(
                  SettlementPermissions.STREAM_CODEC,
                  Packet::permissions,
                  NeoForgeStreamCodecs.enumCodec(StoreOperation.class),
                  Packet::storeOperation,
                  Packet::new);

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return SYNC_TYPE;
      }
   }
}
