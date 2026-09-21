package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.state.IGhostSlotState;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetGhostSlotItem(UUID buildingId, int slot, ItemStack item) implements CustomPacketPayload {

   public static final Type<SetGhostSlotItem> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "set_ghost_slot_item"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SetGhostSlotItem> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               SetGhostSlotItem::buildingId,
               ByteBufCodecs.VAR_INT,
               SetGhostSlotItem::slot,
               ItemStack.OPTIONAL_STREAM_CODEC,
               SetGhostSlotItem::item,
               SetGhostSlotItem::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveSetGhostSlotItem(SetGhostSlotItem packet, IPayloadContext context) {

      Optional<Building> building = ServerBuildingsStore.INSTANCE.find(packet.buildingId);
      if (building.isEmpty())
         return;

      // only accepted from a player who actually has this building's screen open
      if (!(context.player().containerMenu instanceof BuildingMenu buildingMenu)
            || !buildingMenu.getBuilding().getBuildingId().equals(packet.buildingId))
         return;

      if (!(building.get().getState() instanceof IGhostSlotState ghostSlotState))
         return;

      if (packet.slot < 0 || packet.slot >= ghostSlotState.getGhostSlotCount())
         return;

      ItemStack chosenItem = packet.item.copyWithCount(1);
      if (!ghostSlotState.isValidGhostSlotItem(packet.slot, chosenItem))
         return;

      ghostSlotState.setGhostSlotItem(packet.slot, chosenItem);

      ServerBuildingsStore.INSTANCE.replicateChange(building.get(), StoreOperation.UPDATE);
      ServerBuildingsStore.INSTANCE.setDirty();
   }
}
