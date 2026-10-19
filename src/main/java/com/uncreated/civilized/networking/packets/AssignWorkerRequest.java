package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.CivilizedVillager;
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
 * Asks the server to make a villager work at a building, which gives them that building's occupation, or to take their
 * job away when the worksite is empty. Assigning them somewhere new also takes them off their old worksite.
 */
public record AssignWorkerRequest(UUID villagerId, Optional<UUID> worksiteId) implements CustomPacketPayload {

   public static final Type<AssignWorkerRequest> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "assign_worker"));

   public static final StreamCodec<FriendlyByteBuf, AssignWorkerRequest> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               AssignWorkerRequest::villagerId,
               ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
               AssignWorkerRequest::worksiteId,
               AssignWorkerRequest::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequest(AssignWorkerRequest packet, IPayloadContext context) {
      Optional<VillagerInfo> worker = ServerVillagerStore.INSTANCE.find(packet.villagerId());
      if (worker.isEmpty() || worker.get().getSettlementId() == null) {
         failure(context, "Worker could not be assigned - this villager does not belong to a settlement.");
         return;
      }

      UUID settlementId = worker.get().getSettlementId();
      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlementId)
            .hasEditBuildingPermission(context.player().getUUID())) {
         failure(
               context,
               "Worker could not be assigned - you do not have permission to manage workers in this settlement.");
         return;
      }

      // todo: let any adult work, once spouses are folded into the homemaker occupation
      if (!worker.get().getNpcRole().is(VillagerNpcRoles.WORKER)) {
         failure(context, "Worker could not be assigned - this villager is not able to work.");
         return;
      }

      Optional<Building> worksite = Optional.empty();
      if (packet.worksiteId().isPresent()) {
         worksite = ServerBuildingsStore.INSTANCE.find(packet.worksiteId().get());
         if (worksite.isEmpty() || !settlementId.equals(worksite.get().getSettlementId())
               || !worksite.get().getBuildingType().offersWork()) {
            failure(context, "Worker could not be assigned - this building is not a worksite in their settlement.");
            return;
         }

         boolean alreadyWorksHere = worker.get().isAssignedWorkerOf(worksite.get());
         boolean worksiteIsFull =
               !BuildingUtil.worksiteHasFreeWorkerSlot(worksite.get(), ServerVillagerStore.INSTANCE);

         if (!alreadyWorksHere && worksiteIsFull) {
            failure(context, "Worker could not be assigned - this worksite has no free worker slots.");
            return;
         }
      }

      UUID oldWorksiteId = worker.get().getPrimaryWorksiteId();
      worker.get().setPrimaryWorksiteId(worksite.map(Building::getBuildingId).orElse(null));

      // a loaded villager starts their new job and moves into its housing straight away
      Optional<CivilizedVillager> villager = LoadedVillagers.find(packet.villagerId());
      if (villager.isPresent()) {
         InvalidateImportantLocations.revalidate((ServerLevel) villager.get().level(), villager.get());
      } else {
         // an unloaded one catches up on the rest when they're next loaded
         worker.get()
               .setOccupation(
                     worksite.map(b -> b.getBuildingType().occupation()).orElse(VillagerOccupations.UNEMPLOYED));
         ServerVillagerStore.INSTANCE.setDirty();
         ServerVillagerStore.INSTANCE.replicateChange(worker.get(), StoreOperation.UPDATE);
      }

      BuildingUtil.serverRefreshAndReplicate(oldWorksiteId, worker.get().getPrimaryWorksiteId());
   }

   private static void failure(IPayloadContext context, String errorMessage) {
      context.player().displayClientMessage(Component.literal(errorMessage).withColor(Colors.VALIDATION_ERROR), false);
   }
}
