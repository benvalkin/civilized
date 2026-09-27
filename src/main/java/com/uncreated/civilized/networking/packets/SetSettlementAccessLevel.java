package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.permission.AccessLevel;
import com.uncreated.civilized.core.settlement.permission.PlayerPermission;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSettlementAccessLevel(UUID settlementId, UUID playerId,
      Optional<AccessLevel> accessLevel) implements CustomPacketPayload {

   public static final Type<SetSettlementAccessLevel> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "set_settlement_access_level"));

   public static final StreamCodec<FriendlyByteBuf, SetSettlementAccessLevel> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               SetSettlementAccessLevel::settlementId,
               UUIDUtil.STREAM_CODEC,
               SetSettlementAccessLevel::playerId,
               ByteBufCodecs.optional(NeoForgeStreamCodecs.enumCodec(AccessLevel.class)),
               SetSettlementAccessLevel::accessLevel,
               SetSettlementAccessLevel::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveSetSettlementAccessLevel(SetSettlementAccessLevel packet, IPayloadContext context) {
      if (!(context.player() instanceof ServerPlayer sender))
         return;

      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(packet.settlementId);
      if (settlement.isEmpty())
         return;

      ServerSettlementPermissionStore store = ServerSettlementPermissionStore.INSTANCE;
      SettlementPermissions permissions = store.getOrCreate(packet.settlementId);

      if (!permissions.hasManagePermissionsPermission(sender.getUUID())) {
         deny(sender, "message.settlement.permission.denied.manage_permissions", settlement.get());
         return;
      }

      // you cannot change your own role otherwise you could accidentally fully revoke your access to your own
      // settlement (including your ability to fix the mistake)
      if (packet.playerId.equals(sender.getUUID())) {
         deny(sender, "message.settlement.permission.denied.own_access_level", settlement.get());
         return;
      }

      // lockout protection - there always has to be someone left who can manage the settlement's permissions
      AccessLevel newAccessLevel = packet.accessLevel.orElse(null);
      if (permissions.wouldLeaveNoGovernors(packet.playerId, newAccessLevel)) {
         deny(sender, "message.settlement.permission.denied.no_governors_left", settlement.get());
         return;
      }

      if (newAccessLevel == null) {
         store.removeAccess(packet.settlementId, packet.playerId);
      } else {
         // the name comes from the player while they're online, or else from the access they already have
         Optional<String> name =
               Optional.ofNullable(sender.server.getPlayerList().getPlayer(packet.playerId))
                     .map(Player::getScoreboardName)
                     .or(() -> permissions.find(packet.playerId).map(PlayerPermission::scoreboardName));
         if (name.isEmpty()) {
            deny(sender, "message.settlement.permission.denied.player_not_found", settlement.get());
            return;
         }

         store.setAccessLevel(packet.settlementId, packet.playerId, name.get(), newAccessLevel);
      }
   }

   private static void deny(ServerPlayer sender, String translationKey, Settlement settlement) {
      PacketDistributor.sendToPlayer(
            sender,
            new SettlementAccessLevelDenied(
                  Component.translatable(translationKey, settlement.displayNameTranslation())));
   }
}
