package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.settlement.loadouts.ServerLoadoutsStore;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssignLoadoutRequest(UUID villagerId, Optional<UUID> loadoutId) implements CustomPacketPayload {

   public static final Type<AssignLoadoutRequest> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "assign_loadout_request"));

   public static final StreamCodec<FriendlyByteBuf, AssignLoadoutRequest> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               AssignLoadoutRequest::villagerId,
               ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
               AssignLoadoutRequest::loadoutId,
               AssignLoadoutRequest::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequest(AssignLoadoutRequest packet, IPayloadContext context) {
      Optional<VillagerInfo> soldier = ServerVillagerStore.INSTANCE.find(packet.villagerId());
      if (soldier.isEmpty() || soldier.get().getSettlementId() == null) {
         failure(
               context,
               "Loadout could not be assigned - soldier info does not exist, or soldier does not belong to a settlement.");
         return;
      }

      UUID settlementId = soldier.get().getSettlementId();
      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlementId)
            .hasEditBuildingPermission(context.player().getUUID())) {
         failure(
               context,
               "Loadout could not be assigned - you do not have permission to manage soldiers in this settlement.");
         return;
      }

      if (!soldier.get().getOccupation().is(VillagerOccupations.SOLDIER)) {
         failure(context, "Loadout could not be assigned - specified villager is not a soldier.");
         return;
      }

      if (packet.loadoutId().isPresent()) {
         Optional<Loadout> loadout = ServerLoadoutsStore.INSTANCE.find(packet.loadoutId().get());
         if (loadout.isEmpty() || !settlementId.equals(loadout.get().settlementId())) {
            failure(context, "Loadout could not be assigned - the loadout no longer exists in this settlement.");
            return;
         }
      }

      @Nullable
      UUID newLoadoutId = packet.loadoutId().orElse(null);
      soldier.get().setLoadoutId(newLoadoutId);
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(soldier.get(), StoreOperation.UPDATE);
   }

   private static void failure(IPayloadContext context, String errorMessage) {
      context.player().displayClientMessage(Component.literal(errorMessage).withColor(Colors.VALIDATION_ERROR), false);
   }
}
