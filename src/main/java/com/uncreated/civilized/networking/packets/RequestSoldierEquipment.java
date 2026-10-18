package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.entity.equipment.VillagerEquipment;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestSoldierEquipment(int requestId, UUID villagerId) implements CustomPacketPayload {

   public static final Type<RequestSoldierEquipment> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "request_soldier_equipment"));

   public static final StreamCodec<FriendlyByteBuf, RequestSoldierEquipment> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.VAR_INT,
               RequestSoldierEquipment::requestId,
               UUIDUtil.STREAM_CODEC,
               RequestSoldierEquipment::villagerId,
               RequestSoldierEquipment::new);

   private static int lastRequestId = 0;

   public static int nextRequestId() {
      return ++lastRequestId;
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequest(RequestSoldierEquipment packet, IPayloadContext context) {

      // a villager's equipment is saved with its entity, so we can only read the equipment of loaded villagers.
      // All villagers in the settlement should hopefully be loaded.
      Optional<CivilizedVillager> villager = LoadedVillagers.find(packet.villagerId());
      if (villager.isEmpty()) {
         emptyEquipment(packet, context);
         return;
      }

      VillagerEquipment equipment = villager.get().getEquipment();

      List<ItemStack> equippedItems = new LinkedList<>();
      for (CarriedEquipmentSlot slot : Loadout.SUPPORTED_SLOTS) {
         ItemStack equippedItem = equipment.getEquippedOrCarried(slot).copy();
         equippedItems.add(equippedItem);
      }

      success(packet, context, equippedItems);
   }

   private static void success(RequestSoldierEquipment packet, IPayloadContext context, List<ItemStack> items) {
      context.reply(new ListSoldierEquipment(packet.requestId(), Optional.of(items)));
   }

   private static void emptyEquipment(RequestSoldierEquipment packet, IPayloadContext context) {
      context.reply(new ListSoldierEquipment(packet.requestId(), Optional.empty()));
   }
}
