package com.uncreated.civilized.core.building.production.bills;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.production.lines.crafting.CraftingMachine;
import com.uncreated.civilized.core.building.production.lines.singleitem.cooking.BlastingMachine;
import com.uncreated.civilized.core.building.production.lines.singleitem.cooking.SmeltingMachine;
import com.uncreated.civilized.core.building.production.lines.singleitem.cooking.SmokingMachine;

import net.minecraft.core.Registry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class ProductionTypes {
   private static final ResourceKey<Registry<ProductionType>> PRODUCTION_TYPES_KEY =
         ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "production_types"));
   private static final Registry<ProductionType> PRODUCTION_TYPES_INTERNAL =
         new RegistryBuilder<>(PRODUCTION_TYPES_KEY).create();

   public static final DeferredRegister<ProductionType> PRODUCTION_TYPES =
         DeferredRegister.create(PRODUCTION_TYPES_INTERNAL, CIVILIZED_MOD_ID);

   public static ResourceLocation createResourceKey(String villagerOccupationName) {
      return ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, villagerOccupationName);
   }

   private static void registerProductionType(
         RegisterEvent.RegisterHelper<ProductionType> registry,
         ProductionType productionType) {
      registry.register(productionType.resourceLocation(), productionType);
   }

   public static ProductionType getFromResourceLocation(ResourceLocation resourceLocation) {
      return PRODUCTION_TYPES.getRegistry().get().getValue(resourceLocation);
   }

   @SubscribeEvent
   private static void registerRegistry(NewRegistryEvent event) {
      event.register(PRODUCTION_TYPES_INTERNAL);
   }

   @SubscribeEvent
   private static void registerTypes(RegisterEvent event) {
      event.register(PRODUCTION_TYPES_KEY, registry -> {
         registerProductionType(registry, CRAFTING);
         registerProductionType(registry, SMELTING);
         registerProductionType(registry, BLASTING);
         registerProductionType(registry, SMOKING);
      });
   }

   /**
    * Production Types can be sent over the network by registry name. Decodes to {@code null} for a name that is not
    * registered.
    */
   public static final StreamCodec<ByteBuf, ProductionType> STREAM_CODEC =
         ResourceLocation.STREAM_CODEC.map(ProductionTypes::getFromResourceLocation, ProductionType::resourceLocation);

   public static final ProductionType CRAFTING =
         new ProductionType(
               createResourceKey("crafting_production"),
               RecipeType.CRAFTING,
               CraftingMachine::new,
               () -> ItemFilters.emptyItemFilters(1),
               List.of(RecipeSlotType.INGREDIENTS),
               3,
               3,
               ProductionTypes::findCraftingRecipe);
   public static final ProductionType SMELTING =
         new ProductionType(
               createResourceKey("smelting_production"),
               RecipeType.SMELTING,
               SmeltingMachine::new,
               () -> new ItemFilters(ItemFilter.allowAllItems(), ItemFilter.defaultBurnableFuel()),
               List.of(RecipeSlotType.INGREDIENTS, RecipeSlotType.fuel(RecipeType.SMELTING)),
               1,
               1,
               cookingRecipeLookup(RecipeType.SMELTING));
   public static final ProductionType BLASTING =
         new ProductionType(
               createResourceKey("blasting_production"),
               RecipeType.BLASTING,
               BlastingMachine::new,
               () -> new ItemFilters(ItemFilter.allowAllItems(), ItemFilter.defaultBurnableFuel()),
               List.of(RecipeSlotType.INGREDIENTS, RecipeSlotType.fuel(RecipeType.BLASTING)),
               1,
               1,
               cookingRecipeLookup(RecipeType.BLASTING));
   public static final ProductionType SMOKING =
         new ProductionType(
               createResourceKey("smoking_production"),
               RecipeType.SMOKING,
               SmokingMachine::new,
               () -> new ItemFilters(ItemFilter.allowAllItems(), ItemFilter.defaultBurnableFuel()),
               List.of(RecipeSlotType.INGREDIENTS, RecipeSlotType.fuel(RecipeType.SMOKING)),
               1,
               1,
               cookingRecipeLookup(RecipeType.SMOKING));

   private static Optional<ProductionRecipe> findCraftingRecipe(List<ItemStack> inputs, ServerLevel level) {
      CraftingInput input = CraftingInput.of(3, 3, inputs);

      return level.recipeAccess()
            .getRecipeFor(RecipeType.CRAFTING, input, level)
            .map(recipe -> new ProductionRecipe(recipe, input, recipe.value().assemble(input, level.registryAccess())));
   }

   private static <T extends AbstractCookingRecipe> IProductionRecipeLookup cookingRecipeLookup(
         RecipeType<T> recipeType) {
      return (inputs, level) -> {
         SingleRecipeInput input = new SingleRecipeInput(inputs.getFirst());

         return level.recipeAccess()
               .getRecipeFor(recipeType, input, level)
               .map(
                     recipe -> new ProductionRecipe(
                           recipe,
                           input,
                           recipe.value().assemble(input, level.registryAccess())));
      };
   }
}
