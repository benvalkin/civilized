package com.uncreated.civilized.core.building;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.behaviour.BuildingBehaviour;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.state.BuildingState;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupation;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.ResidenceBuildingScreen;
import com.uncreated.civilized.ui.style.Colors;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

@Getter
@Accessors(fluent = true)
@Builder(builderMethodName = "internalBuilder")
public class BuildingType {
   private final ResourceLocation resourceLocation;
   private final boolean isResidence;
   private final boolean isWorksite;
   private final boolean isAnimalFarm;
   @Builder.Default
   private final Function<Building, BuildingState> createState = BuildingState::new;
   @Builder.Default
   private final Function<LoadedBuilding, BuildingBehaviour> createBehaviour = BuildingBehaviour::new;
   @Builder.Default
   private final List<ProductionType> supportedProductionTypes = List.of();
   @Getter(AccessLevel.NONE)
   @Builder.Default
   private final Supplier<VillagerOccupation> occupation = () -> VillagerOccupations.UNEMPLOYED;
   @Builder.Default
   private final BiFunction<BuildingScreenContext, Component, ABuildingScreen> buildingScreenSupplier =
         ResidenceBuildingScreen::new;

   /**
    * Whether this building's screen is backed by a {@code BuildingMenu}, i.e. whether it has been moved off the older
    * screen-only approach. {@code buildingScreenSupplier} is ignored once this is set.
    */
   @Builder.Default
   private final boolean usesBuildingMenu = false;

   public static BuildingTypeBuilder builder(ResourceLocation key) {
      return internalBuilder().resourceLocation(key);
   }

   public VillagerOccupation occupation() {
      return occupation.get();
   }

   public String name() {
      return resourceLocation.getPath();
   }

   public boolean is(BuildingType other) {
      return this.equals(other);
   }

   public boolean isArtisanHouse() {
      return !supportedProductionTypes.isEmpty();
   }

   public String translationKey() {
      return resourceLocation.getNamespace() + ".building." + resourceLocation.getPath();
   }

   public MutableComponent translation() {
      return Component.translatableWithFallback(translationKey(), resourceLocation.getPath().replace("_", " "))
            .withColor(Colors.BUILDING_LIGHT);
   }

   public MutableComponent translationDark() {
      return Component.translatableWithFallback(translationKey(), resourceLocation.getPath().replace("_", " "))
            .withColor(Colors.BUILDING_DARK);
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass())
         return false;
      BuildingType that = (BuildingType) o;
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
