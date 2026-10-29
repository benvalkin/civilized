package com.uncreated.civilized.core.villagerinfo;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.uncreated.civilized.core.dialogue.controller.DialogueController;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.DefaultRoleBehaviour;
import com.uncreated.civilized.entity.VillagerRoleBehaviour;

import com.uncreated.civilized.entity.stats.ClothingTextureRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

@Getter
@Accessors(fluent = true)
@Builder(builderMethodName = "internalBuilder")
public class VillagerNpcRole {

   private final ResourceLocation resourceLocation;
   private final boolean genderedTitle;
   @Builder.Default
   private final Function<CivilizedVillager, VillagerRoleBehaviour> createRoleBehaviour = DefaultRoleBehaviour::new;
   @Builder.Default
   private final DialogueController dialogue = DialogueController.noDialogue();
   @Builder.Default
   private final List<String> clothingSets = List.of(ClothingTextureRegistry.DEFAULT_CLOTHING_SET);

   public static VillagerNpcRoleBuilder builder(ResourceLocation key) {
      return internalBuilder().resourceLocation(key);
   }

   public String name() {
      return resourceLocation.getPath();
   }

   public boolean is(VillagerNpcRole other) {
      return this.equals(other);
   }

   public String translationKey() {
      return resourceLocation.getNamespace() + ".villager.role." + resourceLocation.getPath();
   }

   public MutableComponent translation(Gender gender) {
      if (genderedTitle)
         return Component.translatable(translationKey() + "." + gender.name().toLowerCase());
      return Component.translatableWithFallback(translationKey(), resourceLocation.getPath().replace("_", " "));
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass())
         return false;
      VillagerNpcRole that = (VillagerNpcRole) o;
      return Objects.equals(resourceLocation, that.resourceLocation);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(resourceLocation);
   }

   @Override
   public String toString() {
      return resourceLocation.toString();
   }
}
