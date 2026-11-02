package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.workshift.WorkShift;
import com.uncreated.civilized.entity.behaviour.InvalidateImportantLocations;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sets a villager's workshift. Villager-specific workshifts take precedence over their worksite's default work shift.
 */
public record AssignWorkShiftRequest(UUID villagerId, Optional<WorkShift> workShift) implements CustomPacketPayload {

   public static final Type<AssignWorkShiftRequest> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "assign_work_shift_request"));

   public static final StreamCodec<FriendlyByteBuf, AssignWorkShiftRequest> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               AssignWorkShiftRequest::villagerId,
               ByteBufCodecs.optional(WorkShift.STREAM_CODEC),
               AssignWorkShiftRequest::workShift,
               AssignWorkShiftRequest::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequest(AssignWorkShiftRequest packet, IPayloadContext context) {
      Optional<VillagerInfo> villager = ServerVillagerStore.INSTANCE.find(packet.villagerId());
      if (villager.isEmpty()) {
         failure(context, "Could not assign villager work shift - villager info does not exist.");
         return;
      }

      if (villager.get().getSettlementId() == null) {
         failure(context, "Could not assign villager work shift -  villager does not belong to a settlement.");
         return;
      }

      UUID settlementId = villager.get().getSettlementId();
      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlementId)
            .hasEditBuildingPermission(context.player().getUUID())) {
         failure(
               context,
               "Could not assign villager work shift - you do not have permission to manage villagers in this settlement.");
         return;
      }

      if (!villager.get().getOccupation().supportsWorkshiftOverrides()) {
         failure(context, "Could not assign villager work shift - this villager's occupation has fixed work hours.");
         return;
      }

      Optional<WorkShift> workShift = packet.workShift();
      if (workShift.isPresent()
            && (workShift.get().presetId() == null || workShift.get().equals(WorkShift.UNEMPLOYED))) {
         failure(context, "Could not assign villager work shift - invalid work shift.");
         return;
      }

      @Nullable
      WorkShift newWorkShift = packet.workShift().orElse(null);
      villager.get().setWorkshift(newWorkShift);
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.get(), StoreOperation.UPDATE);

      // otherwise the new shift only takes effect when the villager next revalidates, which can take a while
      LoadedVillagers.find(packet.villagerId())
            .ifPresent(loaded -> InvalidateImportantLocations.revalidate((ServerLevel) loaded.level(), loaded));
   }

   private static void failure(IPayloadContext context, String errorMessage) {
      context.player().displayClientMessage(Component.literal(errorMessage).withColor(Colors.VALIDATION_ERROR), false);
   }
}
