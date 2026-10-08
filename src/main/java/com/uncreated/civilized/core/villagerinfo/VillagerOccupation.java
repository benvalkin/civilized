package com.uncreated.civilized.core.villagerinfo;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;

@Getter
@Accessors(fluent = true)
@Builder(builderMethodName = "internalBuilder")
public class VillagerOccupation {

   private final ResourceLocation resourceLocation;
   @Nullable
   private final BuildingType homeType;
   @Nullable
   private final Predicate<BuildingType> validWorksite;
   @Nullable
   private final Supplier<ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>>> workBehaviourPackage;
   /** The clothing sets villagers with this occupation wear, instead of their role's. Empty to use the role's. */
   @Builder.Default
   private final List<String> clothingSets = List.of();

   public static VillagerOccupation.VillagerOccupationBuilder builder(ResourceLocation key) {
      return internalBuilder().resourceLocation(key);
   }

   public String name() {
      return resourceLocation.getPath();
   }

   public boolean is(VillagerOccupation other) {
      return this.equals(other);
   }

   public String translationKey() {
      return resourceLocation.getNamespace() + ".villager.occupation." + resourceLocation.getPath();
   }

   public MutableComponent translation() {
      return Component.translatableWithFallback(translationKey(), resourceLocation.getPath().replace("_", " "))
            .withColor(Colors.BUILDING_LIGHT);
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass())
         return false;
      VillagerOccupation that = (VillagerOccupation) o;
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
