package com.uncreated.civilized.neoforge.registration.attachments;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.UUID;
import java.util.function.Supplier;

import com.uncreated.civilized.core.quest.attachments.PlayerDialogueCooldowns;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;

import lombok.Getter;
import lombok.Setter;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class DataAttachments {

   public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
         DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CIVILIZED_MOD_ID);

   public static final Supplier<AttachmentType<LinkedBuilding>> LINKED_BUILDING =
         ATTACHMENTS.register(
               "civilized_building_id",
               () -> AttachmentType.builder(() -> new LinkedBuilding())
                     .build());

   public static final Supplier<AttachmentType<PlayerQuests>> QUESTS =
           ATTACHMENTS.register(
                   "civilized_player_quests",
                   () -> AttachmentType.serializable(PlayerQuests::new)
                           .copyOnDeath()
                           .build());

   public static final Supplier<AttachmentType<PlayerDialogueCooldowns>> DIALOGUE_COOLDOWNS =
           ATTACHMENTS.register(
                   "civilized_player_dialogue_cooldowns",
                   () -> AttachmentType.builder(PlayerDialogueCooldowns::new)
                           .build());

   public static final Supplier<AttachmentType<Integer>> COIN_VALUE = ATTACHMENTS.register(
           "coin_value", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

   @Getter
   @Setter
   public static class LinkedBuilding {

      public static final String FIELD_BUILDING_IS_BUILDING = "civilized_building_id";

      public LinkedBuilding() {
      }

      public LinkedBuilding(@Nullable UUID buildingId) {
         this.buildingId = buildingId;
      }

      private @Nullable UUID buildingId;
   }
}
