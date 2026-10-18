package com.uncreated.civilized.core.settlement.loadouts;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.building.production.bills.ItemFilterMode;
import com.uncreated.civilized.core.building.production.bills.ItemFilters;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

@Accessors(fluent = true)
@Getter
public class Loadout {

   public static final List<CarriedEquipmentSlot> SUPPORTED_SLOTS =
         List.of(
               CarriedEquipmentSlot.PRIMARY_WEAPON,
               CarriedEquipmentSlot.SECONDARY_WEAPON,
               CarriedEquipmentSlot.OFFHAND_WEAPON,
               CarriedEquipmentSlot.HEAD,
               CarriedEquipmentSlot.CHEST,
               CarriedEquipmentSlot.LEGS,
               CarriedEquipmentSlot.FEET);

   private final UUID loadoutId;
   private final UUID settlementId;
   private String displayName;
   private ItemFilters loadoutItems;

   public static final StreamCodec<RegistryFriendlyByteBuf, Loadout> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               Loadout::loadoutId,
               UUIDUtil.STREAM_CODEC,
               Loadout::settlementId,
               ByteBufCodecs.STRING_UTF8,
               Loadout::displayName,
               ItemFilters.STREAM_CODEC,
               Loadout::loadoutItems,
               Loadout::new);

   public Loadout(UUID loadoutId, UUID settlementId, String displayName, ItemFilters loadoutItems) {
      this.loadoutId = loadoutId;
      this.settlementId = settlementId;
      this.displayName = displayName;
      this.loadoutItems = loadoutItems;
   }

   public void secureCopyFrom(Loadout other) {
      this.displayName = other.displayName;
      this.loadoutItems = other.loadoutItems.copy();
   }

   public static Loadout create(
         UUID settlementId,
         String displayName,
         Map<CarriedEquipmentSlot, List<ItemStack>> slots) {

      ItemFilters itemFilters = defaultLoadoutFilters();
      for (var slot : slots.entrySet()) {
         int i = SUPPORTED_SLOTS.indexOf(slot.getKey());
         if (i == -1)
            continue;

         itemFilters.setFilter(i, new ItemFilter(ItemFilterMode.WHITELIST_STRICT, slot.getValue()));
      }

      return new Loadout(UUID.randomUUID(), settlementId, displayName, itemFilters);
   }

   public static Loadout create(
           UUID loadoutId,
           UUID settlementId,
           String displayName,
           ItemStack[][] slots) {

      ItemFilters itemFilters = defaultLoadoutFilters();
       for (int i = 0; i < slots.length; i++) {
           List<ItemStack> items = Arrays.stream(slots[i]).toList();
           itemFilters.setFilter(i, new ItemFilter(ItemFilterMode.WHITELIST_STRICT, items));
       }

      return new Loadout(loadoutId, settlementId, displayName, itemFilters);
   }

   void copyFrom(Loadout other) {
      displayName = other.displayName;
      loadoutItems = other.loadoutItems;
   }

   public Packet toPacket(StoreOperation operation) {
      return new Packet(this, operation);
   }

   private static ItemFilters defaultLoadoutFilters() {
      List<ItemFilter> filters = new LinkedList<>();
      for (int i = 0; i < SUPPORTED_SLOTS.size(); i++) {
         filters.add(ItemFilter.allowNothing());
      }
      return new ItemFilters(filters);
   }

   public Optional<ItemFilter> getAllowedItems(CarriedEquipmentSlot slot) {
      int index = SUPPORTED_SLOTS.indexOf(slot);
      if (index == -1)
         return Optional.empty();

      ItemFilter allowed = loadoutItems.getFilter(index);
      if (allowed.allowsNothing())
         return Optional.empty();

      return Optional.of(allowed);
   }

   public Loadout sanitized() {
       List<ItemFilter> sanitized = new ArrayList<>(loadoutItems.size());
       for (int i = 0; i < loadoutItems.size(); i++) {
           ItemFilter filter = loadoutItems.getFilter(i);
          sanitized.add(filter);
       }
       return new Loadout(loadoutId, settlementId, displayName, new ItemFilters(sanitized));
   }

   public record Packet(Loadout loadout, StoreOperation storeOperation) implements CustomPacketPayload {

      public static final Type<Packet> SYNC_TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "loadout_sync"));

      public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC =
            StreamCodec.composite(
                  Loadout.STREAM_CODEC,
                  Packet::loadout,
                  NeoForgeStreamCodecs.enumCodec(StoreOperation.class),
                  Packet::storeOperation,
                  Packet::new);

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return SYNC_TYPE;
      }
   }
}
