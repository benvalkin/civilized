package com.uncreated.civilized.core.settlement;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

@Getter
@Builder
public class Settlement {

   // The stream decoder reference
   public static Settlement decode(FriendlyByteBuf buffer) {
      return Settlement.builder()
            .settlementId(buffer.readUUID())
            .ownerId(buffer.readUUID())
            .displayName(buffer.readUtf())
            .citizenIds(buffer.readCollection(ArrayList::new, b -> b.readUUID()))
            .settlementLevel(SettlementLevel.valueOf(buffer.readInt()))
            .bounds(SettlementBounds.decode(buffer))
            .build();
   }

   // The stream encoder reference
   public void encode(FriendlyByteBuf buffer) {
      buffer.writeUUID(settlementId);
      buffer.writeUUID(ownerId);
      buffer.writeUtf(displayName);
      buffer.writeCollection(citizenIds, (b, i) -> b.writeUUID(i));
      buffer.writeInt(settlementLevel.getLevel());
      bounds.encode(buffer);
   }

   public static final String FIELD_SETTLEMENT_ID = "settlement_id";
   public static final String FIELD_OWNER_ID = "owner_id";
   public static final String FIELD_DISPLAY_NAME = "display_name";
   public static final String FIELD_LIST_CITIZENS = "list_citizens";
   public static final String FIELD_LIST_ITEM_CITIZEN_ID = "list_item_citizen_id";
   public static final String FIELD_SETTLEMENT_LEVEL = "settlement_level";
   public static final String FIELD_ORIGIN_POS = "origin_pos";
   public static final String FIELD_LOWER_CORNER_POS = "lower_corner_pos";
   public static final String FIELD_UPPER_CORNER_POS = "upper_corner_pos";

   private UUID settlementId;
   private UUID ownerId;
   private String displayName;
   @Builder.Default
   private List<UUID> citizenIds = Lists.newArrayList();
   @Builder.Default
   public SettlementLevel settlementLevel = SettlementLevel.OUTPOST;
   private SettlementBounds bounds;

   public Settlement.Packet toPacket() {
      return new Settlement.Packet(this, StoreOperation.UPDATE);
   }

   public Settlement.Packet toPacket(StoreOperation operation) {
      return new Settlement.Packet(this, operation);
   }

   public void copyFrom(Settlement other) {
      settlementId = other.settlementId;
      ownerId = other.ownerId;
      displayName = other.displayName;
      citizenIds = other.citizenIds; // TECHDEBT: this is sus if we are saving the list reference anywhere
      settlementLevel = other.settlementLevel;
      bounds = other.bounds;
   }

   public String toStringLite() {
      return String.format("{settlementId: %s displayName: %s, pop: %s}", settlementId, displayName, citizenIds.size());
   }

   public MutableComponent displayNameTranslation() {
      return Component.literal(displayName).withColor(Colors.SETTLEMENT_NAME);
   }

   public MutableComponent displayNameTranslationExtended() {
      return Component.translatable("settlement.display_name_extended", displayName, settlementLevel.translation());
   }

   public record Packet(Settlement settlement, StoreOperation storeOperation) implements CustomPacketPayload {

      public static final CustomPacketPayload.Type<Settlement.Packet> SYNC_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "sync_settlement"));

      public static StreamCodec<FriendlyByteBuf, Settlement.Packet> STREAM_CODEC =
            StreamCodec.ofMember(Settlement.Packet::encode, Settlement.Packet::decode);

      public static Settlement.Packet decode(FriendlyByteBuf buffer) {
         return new Settlement.Packet(Settlement.decode(buffer), buffer.readEnum(StoreOperation.class));
      }

      public void encode(FriendlyByteBuf buffer) {
         settlement.encode(buffer);
         buffer.writeEnum(storeOperation);
      }

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return SYNC_TYPE;
      }
   }

   public static String generateRandomName() {
      return switch (new Random().nextInt(12)) {
      case 0 -> "Blizzhollow";
      case 1 -> "Chalbay";
      case 2 -> "Dustcross";
      case 3 -> "Relminster";
      case 4 -> "Geldarch";
      case 5 -> "Chimerket";
      case 6 -> "Faergamble";
      case 7 -> "Misthollow";
      case 8 -> "Faunacre";
      case 9 -> "Knightbridge";
      case 10 -> "Emberham";
      case 11 -> "Millmeadow";
      default -> "Newhaven";
      };
   }

   public void recalculateSettlementBounds(BlockPos settlementOrigin, Set<Building> buildings) {

      if (buildings.isEmpty()) {
         bounds = new SettlementBounds(settlementOrigin);
         return;
      }

      BlockPos minBuildingCorner = null;
      BlockPos maxBuildingCorner = null;
      for (Building other : buildings) {
         BlockPos lowerCorner = other.getBounds().getLowerCorner();
         BlockPos upperCorner = other.getBounds().getUpperCorner();
         if (minBuildingCorner == null || lowerCorner.getX() < minBuildingCorner.getX()
               || lowerCorner.getZ() < minBuildingCorner.getZ())
            minBuildingCorner = lowerCorner;
         if (maxBuildingCorner == null || upperCorner.getX() > maxBuildingCorner.getX()
               || upperCorner.getZ() > maxBuildingCorner.getZ())
            maxBuildingCorner = upperCorner;
      }

      bounds = SettlementBounds.fromBuildingCorners(settlementOrigin, minBuildingCorner, maxBuildingCorner);
   }
}
