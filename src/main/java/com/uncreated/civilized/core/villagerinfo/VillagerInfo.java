package com.uncreated.civilized.core.villagerinfo;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

@Getter
@Builder
public class VillagerInfo {

   private static final int MAX_SOCIAL_CLASS_LEVEL = 3;

   public static StreamCodec<FriendlyByteBuf, VillagerInfo> CODEC =
         StreamCodec.ofMember(VillagerInfo::encode, VillagerInfo::decode);

   // The stream decoder reference
   public static VillagerInfo decode(FriendlyByteBuf buffer) {
      VillagerInfoBuilder builder =
            VillagerInfo.builder()
                  .villagerId(buffer.readUUID())
                  .isDeceased(buffer.readBoolean())
                  .firstName(buffer.readUtf())
                  .lastName(buffer.readUtf())
                  .occupation(VillagerOccupations.getFromResourceLocation(buffer.readResourceLocation()))
                  .settlementId(buffer.readNullable((b -> b.readUUID())))
                  .homeBuildingId(buffer.readNullable((b -> b.readUUID())))
                  .primaryWorksiteId(buffer.readNullable((b -> b.readUUID())))
                  .npcRole(VillagerNpcRoles.getFromResourceLocation(buffer.readResourceLocation()))
                  .gender(buffer.readEnum(Gender.class))
                  .partnerId(buffer.readNullable((b -> b.readUUID())))
                  .socialClass(buffer.readVarInt());

      return builder.build();
   }

   // The stream encoder reference
   public void encode(FriendlyByteBuf buffer) {
      buffer.writeUUID(villagerId);
      buffer.writeBoolean(isDeceased);
      buffer.writeUtf(firstName);
      buffer.writeUtf(lastName);
      buffer.writeResourceLocation(occupation.resourceLocation());
      buffer.writeNullable(settlementId, (b, v) -> b.writeUUID(v));
      buffer.writeNullable(homeBuildingId, (b, v) -> b.writeUUID(v));
      buffer.writeNullable(primaryWorksiteId, (b, v) -> b.writeUUID(v));
      buffer.writeResourceLocation(npcRole.resourceLocation());
      buffer.writeEnum(gender);
      buffer.writeNullable(partnerId, (b, v) -> b.writeUUID(v));
      buffer.writeVarInt(socialClass);
   }

   public static final String FIELD_VILLAGER_ID = "villager_id";
   public static final String FIELD_IS_DECEASED = "is_deceased";
   public static final String FIELD_SETTLEMENT_ID = "settlement_id";
   public static final String FIELD_HOME_BUILDING_ID = "home_building_id";
   public static final String FIELD_FIRST_NAME = "first_name";
   public static final String FIELD_LAST_NAME = "last_name";
   public static final String FIELD_VILLAGER_OCCUPATION = "field_villager_occupation";
   public static final String FIELD_VILLAGER_NPC_ROLE = "npc_role";
   public static final String FIELD_VILLAGER_GENDER = "field_villager_gender";
   public static final String FIELD_PARTNER_ID = "field_spouse_id";
   public static final String FIELD_SOCIAL_CLASS = "social_class";

   private UUID villagerId;
   @Setter
   private boolean isDeceased;
   @Setter
   @Builder.Default
   private String firstName = "";
   @Setter
   @Builder.Default
   private String lastName = "";
   @Setter
   @Builder.Default
   private VillagerOccupation occupation = VillagerOccupations.UNEMPLOYED;
   @Setter
   @Builder.Default
   private VillagerNpcRole npcRole = VillagerNpcRoles.NONE;
   @Setter
   private @Nullable UUID settlementId;
   @Setter
   private @Nullable UUID homeBuildingId;
   @Setter
   private @Nullable UUID primaryWorksiteId;
   private Gender gender;
   @Setter
   private @Nullable UUID partnerId;
   @Builder.Default
   private int socialClass = 1;

   public boolean hasName() {
      return !firstName.isEmpty() && !lastName.isEmpty();
   }

   public void setSocialClass(int socialClass) {
      this.socialClass = Math.clamp(socialClass, 1, MAX_SOCIAL_CLASS_LEVEL);
   }

   public boolean isTaken() {
      return partnerId != null;
   }

   public boolean isPartnerOf(VillagerInfo other) {
      if (partnerId == null)
         return false;

      if (other.partnerId == null)
         return false;

      return partnerId.equals(other.partnerId);
   }

   public String getFullName() {

      if (!hasName()) {
         return "Unnamed Villager";
      }

      return firstName + " " + lastName;
   }

   public Component getFullNameComponent() {
      if (!hasName())
         return Component.empty();

      return Component.literal(getFullName());
   }

   public Component getSocialClassTranslation() {
      return Component
            .translatable("villager.social_class.default." + socialClass + "." + gender.toString().toLowerCase());
   }

   public static List<String> PLACEHOLDER_FIRST_NAMES_MALE =
         List.of(
               "James",
               "William",
               "Thomas",
               "John",
               "Henry",
               "Edward",
               "Richard",
               "Robert",
               "Walter",
               "Geoffrey",
               "Hugh",
               "Roger",
               "Edmund",
               "Nicholas",
               "Arthur",
               "Samuel");
   public static List<String> PLACEHOLDER_FIRST_NAMES_FEMALE =
         List.of(
               "Anne",
               "Mary",
               "Elizabeth",
               "Margaret",
               "Alice",
               "Agnes",
               "Joan",
               "Matilda",
               "Eleanor",
               "Catherine",
               "Isabel",
               "Edith",
               "Beatrice",
               "Martha",
               "Emma",
               "Sarah");
   public static List<String> PLACEHOLDER_LAST_NAMES =
         List.of(
               "Smith",
               "Cooper",
               "Fletcher",
               "Turner",
               "Hawthorne",
               "Ashdown",
               "Whitaker",
               "Radcliffe",
               "Holloway",
               "Pembroke",
               "Ellsworth",
               "Blackwood",
               "Harrington",
               "Caldwell",
               "Thornbury",
               "Pritchard");

   public static Pair<String, String> generateRandomName(Gender gender) {
      return switch (gender) {
      case MALE -> Pair.of(
            PLACEHOLDER_FIRST_NAMES_MALE.get(new Random().nextInt(PLACEHOLDER_FIRST_NAMES_MALE.size())),
            PLACEHOLDER_LAST_NAMES.get(new Random().nextInt(PLACEHOLDER_LAST_NAMES.size())));
      case FEMALE -> Pair.of(
            PLACEHOLDER_FIRST_NAMES_FEMALE.get(new Random().nextInt(PLACEHOLDER_FIRST_NAMES_FEMALE.size())),
            PLACEHOLDER_LAST_NAMES.get(new Random().nextInt(PLACEHOLDER_LAST_NAMES.size())));
      };
   }

   public Packet toPacket() {
      return new Packet(this, StoreOperation.UPDATE);
   }

   public Packet toPacket(StoreOperation operation) {
      return new Packet(this, operation);
   }

   public void copyFrom(VillagerInfo other) {
      isDeceased = other.isDeceased;
      firstName = other.firstName;
      lastName = other.lastName;
      occupation = other.occupation;
      settlementId = other.settlementId;
      homeBuildingId = other.homeBuildingId;
      primaryWorksiteId = other.primaryWorksiteId;
      npcRole = other.npcRole;
      gender = other.gender;
      partnerId = other.partnerId;
      socialClass = other.socialClass;
   }

   public String toStringLite() {
      return String.format(
            "{name: %s %s - occupation: %s, villagerId: %s settlementId: %s}",
            firstName,
            lastName,
            villagerId,
            settlementId,
            occupation);
   }

   public boolean isOccupantOf(Building building) {
      return building.getBuildingId().equals(homeBuildingId);
   }

   public boolean isAssignedWorkerOf(Building building) {
      return building.getBuildingId().equals(primaryWorksiteId);
   }

   public record Packet(VillagerInfo villager, StoreOperation storeOperation) implements CustomPacketPayload {

      public static final Type<Packet> SYNC_TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "sync_villagerinfo"));

      public static StreamCodec<FriendlyByteBuf, Packet> STREAM_CODEC =
            StreamCodec.ofMember(Packet::encode, Packet::decode);

      public static Packet decode(FriendlyByteBuf buffer) {
         return new Packet(VillagerInfo.decode(buffer), buffer.readEnum(StoreOperation.class));
      }

      public void encode(FriendlyByteBuf buffer) {
         villager.encode(buffer);
         buffer.writeEnum(storeOperation);
      }

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return SYNC_TYPE;
      }
   }
}
