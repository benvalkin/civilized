package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.uncreated.civilized.core.settlement.permission.PlayerPermission;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.townhall.tabs.ManagePermissionsTab;

import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to sync a Settlement's Permissions to a client, usually when the client's Building Menu is open.
 */
public record SettlementPermissionsSync(UUID settlementId,
      List<PlayerPermission> permissions) implements CustomPacketPayload {

   public static final Type<SettlementPermissionsSync> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "settlement_permissions_sync"));

   public static final StreamCodec<FriendlyByteBuf, SettlementPermissionsSync> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               SettlementPermissionsSync::settlementId,
               PlayerPermission.STREAM_CODEC.apply(ByteBufCodecs.list()),
               SettlementPermissionsSync::permissions,
               SettlementPermissionsSync::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void sendToViewers(
         MinecraftServer server,
         UUID settlementId,
         Collection<PlayerPermission> permissions) {
      SettlementPermissionsSync packet = new SettlementPermissionsSync(settlementId, List.copyOf(permissions));
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         if (player.containerMenu instanceof BuildingMenu menu
               && menu.getSettlement().getSettlementId().equals(settlementId))
            PacketDistributor.sendToPlayer(player, packet);
      }
   }

   public static void clientReceiveSettlementPermissionsSync(
         SettlementPermissionsSync packet,
         IPayloadContext context) {
      if (!(Minecraft.getInstance().screen instanceof ABuildingMenuScreen screen)
            || !screen.getContext().settlement().getSettlementId().equals(packet.settlementId))
         return;

      // kept on the screen so that tabs opened later see the update too
      screen.getContext().setSettlementPermissions(packet.permissions);
      if (screen.getCurrentTab() instanceof ManagePermissionsTab permissionsTab)
         permissionsTab.refresh();
   }
}
