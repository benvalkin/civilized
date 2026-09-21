package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.state.IEyeDropperSlotState;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetEyeDropperSlotItem(UUID buildingId, int slot, ItemStack item) implements CustomPacketPayload {

   public static final Type<SetEyeDropperSlotItem> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "set_eye_dropper_slot_item"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SetEyeDropperSlotItem> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               SetEyeDropperSlotItem::buildingId,
               ByteBufCodecs.VAR_INT,
               SetEyeDropperSlotItem::slot,
               ItemStack.OPTIONAL_STREAM_CODEC,
               SetEyeDropperSlotItem::item,
               SetEyeDropperSlotItem::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveSetEyeDropperSlotItem(SetEyeDropperSlotItem packet, IPayloadContext context) {

      Optional<Building> building = ServerBuildingsStore.INSTANCE.find(packet.buildingId);
      if (building.isEmpty())
         return;

      // only accepted from a player who actually has this building's screen open
      if (!(context.player().containerMenu instanceof BuildingMenu buildingMenu)
            || !buildingMenu.getBuilding().getBuildingId().equals(packet.buildingId))
         return;

      if (!(building.get().getState() instanceof IEyeDropperSlotState eyeDropperSlotState))
         return;

      if (packet.slot < 0 || packet.slot >= eyeDropperSlotState.getEyeDropperSlotCount())
         return;

      ItemStack chosenItem = packet.item.copyWithCount(1);
      if (!eyeDropperSlotState.isValidEyeDropperSlotItem(packet.slot, chosenItem))
         return;

      eyeDropperSlotState.setEyeDropperSlotItem(packet.slot, chosenItem);

      ServerBuildingsStore.INSTANCE.replicateChange(building.get(), StoreOperation.UPDATE);
      ServerBuildingsStore.INSTANCE.setDirty();
   }
}
