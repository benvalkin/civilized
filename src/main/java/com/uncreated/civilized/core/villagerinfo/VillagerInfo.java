package com.uncreated.civilized.core.villagerinfo;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

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
                  .visitingSettlementId(buffer.readNullable((b -> b.readUUID())))
                  .workBuildingId(buffer.readNullable((b -> b.readUUID())))
                  .npcRole(VillagerNpcRoles.getFromResourceLocation(buffer.readResourceLocation()))
                  .gender(buffer.readEnum(Gender.class))
                  .partnerId(buffer.readNullable((b -> b.readUUID())))
                  .socialClass(buffer.readVarInt())
                  .loadoutId(buffer.readNullable((b -> b.readUUID())))
                  .workAreaId(buffer.readNullable((b -> b.readUUID())))
                  .motherId(buffer.readNullable((b -> b.readUUID())))
                  .fatherId(buffer.readNullable((b -> b.readUUID())))
                  .birthDay(buffer.readNullable((FriendlyByteBuf::readVarLong)))
                  .lifeStage(buffer.readEnum(LifeStage.class))
                  .skills(VillagerSkills.decode(buffer));

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
      buffer.writeNullable(visitingSettlementId, (b, v) -> b.writeUUID(v));
      buffer.writeNullable(workBuildingId, (b, v) -> b.writeUUID(v));
      buffer.writeResourceLocation(npcRole.resourceLocation());
      buffer.writeEnum(gender);
      buffer.writeNullable(partnerId, (b, v) -> b.writeUUID(v));
      buffer.writeVarInt(socialClass);
      buffer.writeNullable(loadoutId, (b, v) -> b.writeUUID(v));
      buffer.writeNullable(workAreaId, (b, v) -> b.writeUUID(v));
      buffer.writeNullable(motherId, (b, v) -> b.writeUUID(v));
      buffer.writeNullable(fatherId, (b, v) -> b.writeUUID(v));
      buffer.writeNullable(birthDay, FriendlyByteBuf::writeVarLong);
      buffer.writeEnum(lifeStage);
      skills.encode(buffer);
   }

   public static @NotNull VillagerInfo fromNbt(CompoundTag itemTag, HolderLookup.Provider registryAccess) {
      VillagerInfo.VillagerInfoBuilder builder =
            VillagerInfo.builder()
                  .villagerId(itemTag.getUUID(VillagerInfo.FIELD_VILLAGER_ID))
                  .isDeceased(itemTag.getBoolean(VillagerInfo.FIELD_IS_DECEASED))
                  .firstName(itemTag.getString(VillagerInfo.FIELD_FIRST_NAME))
                  .lastName(itemTag.getString(VillagerInfo.FIELD_LAST_NAME))
                  .occupation(
                        VillagerOccupations.getFromResourceLocation(
                              ResourceLocation.parse(itemTag.getString(VillagerInfo.FIELD_VILLAGER_OCCUPATION))))
                  .npcRole(
                        VillagerNpcRoles.getFromResourceLocation(
                              ResourceLocation.parse(itemTag.getString(VillagerInfo.FIELD_VILLAGER_NPC_ROLE))))
                  .gender(Gender.valueOf(itemTag.getString(VillagerInfo.FIELD_VILLAGER_GENDER)))
                  .skills(VillagerSkills.fromNbt(itemTag.getCompound(VillagerInfo.FIELD_SKILLS)));

      if (itemTag.hasUUID(VillagerInfo.FIELD_SETTLEMENT_ID))
         builder.settlementId(itemTag.getUUID(VillagerInfo.FIELD_SETTLEMENT_ID));
      if (itemTag.hasUUID(VillagerInfo.FIELD_HOME_BUILDING_ID))
         builder.homeBuildingId(itemTag.getUUID(VillagerInfo.FIELD_HOME_BUILDING_ID));
      if (itemTag.hasUUID(VillagerInfo.FIELD_VISITING_SETTLEMENT_ID))
         builder.visitingSettlementId(itemTag.getUUID(VillagerInfo.FIELD_VISITING_SETTLEMENT_ID));
      if (itemTag.hasUUID(VillagerInfo.FIELD_WORK_BUILDING_ID))
         builder.workBuildingId(itemTag.getUUID(VillagerInfo.FIELD_WORK_BUILDING_ID));
      if (itemTag.hasUUID(VillagerInfo.FIELD_MOTHER_ID))
         builder.motherId(itemTag.getUUID(VillagerInfo.FIELD_MOTHER_ID));
      if (itemTag.hasUUID(VillagerInfo.FIELD_FATHER_ID))
         builder.fatherId(itemTag.getUUID(VillagerInfo.FIELD_FATHER_ID));
      if (itemTag.contains(VillagerInfo.FIELD_BIRTH_DAY))
         builder.birthDay(itemTag.getLong(VillagerInfo.FIELD_BIRTH_DAY));
      if (itemTag.contains(VillagerInfo.FIELD_LIFE_STAGE))
         builder.lifeStage(LifeStage.valueOf(itemTag.getString(VillagerInfo.FIELD_LIFE_STAGE)));
      if (itemTag.hasUUID(VillagerInfo.FIELD_WORK_AREA_ID))
         builder.workAreaId(itemTag.getUUID(VillagerInfo.FIELD_WORK_AREA_ID));
      if (itemTag.hasUUID(VillagerInfo.FIELD_PARTNER_ID))
         builder.partnerId(itemTag.getUUID(VillagerInfo.FIELD_PARTNER_ID));
      if (itemTag.contains(VillagerInfo.FIELD_SOCIAL_CLASS))
         builder.socialClass(itemTag.getInt(VillagerInfo.FIELD_SOCIAL_CLASS));
      if (itemTag.hasUUID(VillagerInfo.FIELD_LOADOUT_ID))
         builder.loadoutId(itemTag.getUUID(VillagerInfo.FIELD_LOADOUT_ID));

      return builder.build();
   }

   public @NotNull CompoundTag toNbt(HolderLookup.Provider registryAccess) {
      CompoundTag item = new CompoundTag();
      item.putUUID(VillagerInfo.FIELD_VILLAGER_ID, getVillagerId());
      item.putBoolean(FIELD_IS_DECEASED, isDeceased());
      if (getSettlementId() != null)
         item.putUUID(FIELD_SETTLEMENT_ID, getSettlementId());
      if (getHomeBuildingId() != null)
         item.putUUID(FIELD_HOME_BUILDING_ID, getHomeBuildingId());
      if (getVisitingSettlementId() != null)
         item.putUUID(FIELD_VISITING_SETTLEMENT_ID, getVisitingSettlementId());
      if (getWorkBuildingId() != null)
         item.putUUID(FIELD_WORK_BUILDING_ID, getWorkBuildingId());
      if (getWorkAreaId() != null)
         item.putUUID(FIELD_WORK_AREA_ID, getWorkAreaId());
      item.putString(FIELD_FIRST_NAME, getFirstName());
      item.putString(FIELD_LAST_NAME, getLastName());
      item.putString(FIELD_VILLAGER_OCCUPATION, getOccupation().resourceLocation().toString());
      item.putString(FIELD_VILLAGER_GENDER, getGender().name());
      if (getPartnerId() != null)
         item.putUUID(FIELD_PARTNER_ID, getPartnerId());

      item.putString(FIELD_VILLAGER_NPC_ROLE, getNpcRole().resourceLocation().toString());
      item.putInt(FIELD_SOCIAL_CLASS, getSocialClass());
      if (getLoadoutId() != null)
         item.putUUID(FIELD_LOADOUT_ID, getLoadoutId());
      item.put(FIELD_SKILLS, getSkills().toNbt());
      if (getMotherId() != null)
         item.putUUID(FIELD_MOTHER_ID, getMotherId());
      if (getFatherId() != null)
         item.putUUID(FIELD_FATHER_ID, getFatherId());
      if (getBirthDay() != null)
         item.putLong(FIELD_BIRTH_DAY, getBirthDay());
      item.putString(FIELD_LIFE_STAGE, getLifeStage().name());
      return item;
   }

   public static final String FIELD_VILLAGER_ID = "villager_id";
   public static final String FIELD_IS_DECEASED = "is_deceased";
   public static final String FIELD_SETTLEMENT_ID = "settlement_id";
   public static final String FIELD_HOME_BUILDING_ID = "home_building_id";
   public static final String FIELD_VISITING_SETTLEMENT_ID = "visiting_settlement_id";
   public static final String FIELD_WORK_BUILDING_ID = "work_building_id";
   public static final String FIELD_FIRST_NAME = "first_name";
   public static final String FIELD_LAST_NAME = "last_name";
   public static final String FIELD_VILLAGER_OCCUPATION = "field_villager_occupation";
   public static final String FIELD_VILLAGER_NPC_ROLE = "npc_role";
   public static final String FIELD_VILLAGER_GENDER = "field_villager_gender";
   public static final String FIELD_PARTNER_ID = "field_spouse_id";
   public static final String FIELD_SOCIAL_CLASS = "social_class";
   public static final String FIELD_LOADOUT_ID = "loadout_id";
   public static final String FIELD_WORK_AREA_ID = "work_area_id";
   public static final String FIELD_SKILLS = "skills";
   public static final String FIELD_MOTHER_ID = "mother_id";
   public static final String FIELD_FATHER_ID = "father_id";
   public static final String FIELD_BIRTH_DAY = "birth_day";
   public static final String FIELD_LIFE_STAGE = "life_stage";
   public static final int GROW_UP_DAYS = 30;

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
   private @Nullable UUID visitingSettlementId;
   @Setter
   private @Nullable UUID workBuildingId;
   private Gender gender;
   @Setter
   private @Nullable UUID partnerId;
   @Builder.Default
   private int socialClass = 1;
   @Setter
   private @Nullable UUID loadoutId;
   @Setter
   private @Nullable UUID workAreaId;
   @Setter
   @Builder.Default
   private VillagerSkills skills = VillagerSkills.allSkillsZeroXp();
   private @Nullable UUID motherId;
   private @Nullable UUID fatherId;
   @Setter
   private @Nullable Long birthDay;
   @Setter
   @Builder.Default
   private LifeStage lifeStage = LifeStage.ADULT;

   public long lifetimeSeed() {
      return villagerId.getMostSignificantBits();
   }

   public RandomSource perLifetimeRandom() {
      return RandomSource.create(lifetimeSeed());
   }

   public RandomSource perLifetimeRandom(int consume) {
      RandomSource random = perLifetimeRandom();
      random.consumeCount(consume);
      return random;
   }

   public boolean hasName() {
      return !firstName.isEmpty() && !lastName.isEmpty();
   }

   public void setSocialClass(int socialClass) {
      this.socialClass = Math.clamp(socialClass, 1, MAX_SOCIAL_CLASS_LEVEL);
   }

   public boolean isTaken() {
      return partnerId != null;
   }

   public boolean isHomemaker() {
      return isTaken() && occupation.is(VillagerOccupations.UNEMPLOYED);
   }

   public boolean isPartnerOf(VillagerInfo other) {
      if (partnerId == null)
         return false;

      return partnerId.equals(other.villagerId) && villagerId.equals(other.partnerId);
   }

   /** Only for newborns: a villager's parents never change once they're born. */
   public void setParents(UUID motherId, UUID fatherId) {
      this.motherId = motherId;
      this.fatherId = fatherId;
   }

   public List<UUID> getParentIds() {
      List<UUID> parentIds = new ArrayList<>(2);
      if (motherId != null)
         parentIds.add(motherId);
      if (fatherId != null)
         parentIds.add(fatherId);
      return parentIds;
   }

   public boolean isChildOf(VillagerInfo other) {
      return other.villagerId.equals(motherId) || other.villagerId.equals(fatherId);
   }

   public boolean isSiblingOf(VillagerInfo other) {
      if (villagerId.equals(other.villagerId))
         return false;

      for (UUID parentId : getParentIds()) {
         if (other.getParentIds().contains(parentId))
            return true;
      }
      return false;
   }

   /**
    * Whether they're close family who'd live together: partners, parents and children, siblings, and step-parents and
    * step-children.
    */
   public boolean isCloseFamilyOf(VillagerInfo other) {
      if (isPartnerOf(other) || isChildOf(other) || other.isChildOf(this) || isSiblingOf(other))
         return true;

      // a parent's new partner, or a partner's children
      boolean isStepChild = other.partnerId != null && getParentIds().contains(other.partnerId);
      boolean isStepParent = partnerId != null && other.getParentIds().contains(partnerId);
      return isStepChild || isStepParent;
   }

   public OptionalLong getAgeInDays(long dayTime) {
      if (birthDay == null)
         return OptionalLong.empty();

      return OptionalLong.of(dayTime / 24000 - birthDay);
   }

   public boolean isChild() {
      return lifeStage == LifeStage.CHILD;
   }

   public boolean isOldEnoughToGrowUp(long dayTime) {
      OptionalLong age = getAgeInDays(dayTime);
      return isChild() && age.isPresent() && age.getAsLong() >= GROW_UP_DAYS;
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

   public Component getTitleTranslation() {
      if (getNpcRole().is(VillagerNpcRoles.CITIZEN)) {
         if (isChild())
            return getChildTranslation();
         if (isHomemaker())
            return getHomeMakerTranslation();

         return getOccupation().translation();
      }

      return getNpcRole().translation(gender);
   }

   public Component getChildTranslation() {
      return Component.translatable("menu.building.occupants.job.child." + gender.toString().toLowerCase());
   }

   public Component getHomeMakerTranslation() {
      return Component.translatable("menu.building.occupants.job.homemaker." + gender.toString().toLowerCase());
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
      visitingSettlementId = other.visitingSettlementId;
      workBuildingId = other.workBuildingId;
      npcRole = other.npcRole;
      gender = other.gender;
      partnerId = other.partnerId;
      socialClass = other.socialClass;
      loadoutId = other.loadoutId;
      workAreaId = other.workAreaId;
      skills = other.skills;
      motherId = other.motherId;
      fatherId = other.fatherId;
      birthDay = other.birthDay;
      lifeStage = other.lifeStage;
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
      return building.getBuildingId().equals(workBuildingId) || building.getBuildingId().equals(workAreaId);
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
