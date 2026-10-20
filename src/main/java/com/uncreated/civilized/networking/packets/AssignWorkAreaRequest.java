package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.WorkAreaUtil;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssignWorkAreaRequest(UUID villagerId, UUID workAreaId) implements CustomPacketPayload {

   public static final Type<AssignWorkAreaRequest> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "assign_work_area"));

   public static final StreamCodec<FriendlyByteBuf, AssignWorkAreaRequest> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               AssignWorkAreaRequest::villagerId,
               UUIDUtil.STREAM_CODEC,
               AssignWorkAreaRequest::workAreaId,
               AssignWorkAreaRequest::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequest(AssignWorkAreaRequest packet, IPayloadContext context) {
      Optional<VillagerInfo> worker = ServerVillagerStore.INSTANCE.find(packet.villagerId());
      Optional<Building> workArea = ServerBuildingsStore.INSTANCE.find(packet.workAreaId());

      boolean invalidWorkArea = worker.isEmpty() || workArea.isEmpty()
              || !WorkAreaUtil.canWork(worker.get(), workArea.get());

      if (invalidWorkArea) {
         failure(context, "Worker could not be assigned - they cannot work here from their worksite.");
         return;
      }

      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(workArea.get().getSettlementId())
            .hasEditBuildingPermission(context.player().getUUID())) {
         failure(
               context,
               "Worker could not be assigned - you do not have permission to manage workers in this settlement.");
         return;
      }

      for (VillagerInfo changed : WorkAreaUtil.assign(
            worker.get(),
            workArea.get(),
            ServerBuildingsStore.INSTANCE,
            ServerVillagerStore.INSTANCE)) {
         ServerVillagerStore.INSTANCE.replicateChange(changed, StoreOperation.UPDATE);
      }
      ServerVillagerStore.INSTANCE.setDirty();
   }

   private static void failure(IPayloadContext context, String errorMessage) {
      context.player().displayClientMessage(Component.literal(errorMessage).withColor(Colors.VALIDATION_ERROR), false);
   }
}
