package com.uncreated.civilized.core.building.state.artisan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.production.RecipeProductionMachine;
import com.uncreated.civilized.core.building.production.bills.ItemFilters;
import com.uncreated.civilized.core.building.production.bills.NbtHelper;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionRecipe;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;
import com.uncreated.civilized.core.building.production.bills.RecipeAllowed;
import com.uncreated.civilized.core.building.production.bills.strategy.ProductionStrategyType;
import com.uncreated.civilized.core.building.production.orders.ProductionOrder;
import com.uncreated.civilized.core.building.state.BuildingState;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;

public abstract class ArtisanHouseState extends BuildingState {

   private final Map<ProductionType, List<ProductionBill>> productionLines;

   public ArtisanHouseState(Building building) {
      super(building);

      productionLines = new HashMap<>();
      for (ProductionType productionType : getSupportedProductionTypes())
         productionLines.put(productionType, new ArrayList<>());

      assert !getSupportedProductionTypes().isEmpty() : "House must support at least one ProductionType.";
   }

   public <Order extends ProductionOrder> void createProductionOrders(
         RecipeProductionMachine<Order> machine,
         ServerLevel serverLevel) {

      RecipeManager recipeManager = serverLevel.getServer().getRecipeManager();

      List<ProductionBill> productionBills =
            this.productionLines.getOrDefault(machine.getProductionType(), new ArrayList<>());

      for (int i = 0; i < productionBills.size(); i++) {

         ProductionBill bill = productionBills.get(i);

         Optional<RecipeHolder<?>> recipe = bill.resolveRecipe(recipeManager);

         if (recipe.isEmpty())
            continue;

         String key = "productionBill_" + i;
         Order order = machine.createOrderFromBill(key, bill, serverLevel);
         machine.registerOrder(order);
      }
   }

   public void applyNbt(CompoundTag compoundTag, HolderLookup.Provider registryAccess) {
      for (ProductionType productionType : getSupportedProductionTypes()) {
         readProductionLine(compoundTag, registryAccess, productionType);
      }
   }

   private void readProductionLine(
         CompoundTag compoundTag,
         HolderLookup.Provider registryAccess,
         ProductionType productionType) {

      if (!getSupportedProductionTypes().contains(productionType))
         return;

      String key = "production_bills_" + productionType.name().toLowerCase();
      if (!compoundTag.contains(key))
         return;

      CompoundTag billsTag = compoundTag.getCompound(key);
      List<ProductionBill> productionBills = new ArrayList<>();
      ListTag list = billsTag.getList("production_bills", ListTag.TAG_COMPOUND);
      for (Tag tag : list) {
         if (!(tag instanceof CompoundTag bt))
            continue;

         ProductionBill bill =
               new ProductionBill(
                     bt.getString("recipe_name"),
                     ProductionTypes.getFromResourceLocation(ResourceLocation.parse(bt.getString("production_type"))),
                     ProductionStrategyType.valueOf(bt.getString("production_strategy_type")),
                     bt.getInt("bill_amount"),
                     bt.getBoolean("enabled"),
                     NbtHelper.readItemList(bt.getList("input_items", Tag.TAG_COMPOUND), registryAccess),
                     ItemFilters.fromNbt(bt.getList("ingredient_filters", Tag.TAG_COMPOUND), registryAccess),
                     ItemStack.parse(registryAccess, bt.getCompound("display_item")).orElse(ItemStack.EMPTY));
         productionBills.add(bill);

         productionLines.put(productionType, productionBills);
      }
   }

   public CompoundTag toNbt(HolderLookup.Provider registryAccess) {
      CompoundTag tag = new CompoundTag();

      for (ProductionType productionType : productionLines.keySet()) {
         writeProductionBills(registryAccess, tag, productionType);
      }

      return tag;
   }

   private void writeProductionBills(
         HolderLookup.Provider registryAccess,
         CompoundTag rootTag,
         ProductionType productionType) {
      CompoundTag tag = new CompoundTag();
      ListTag list = new ListTag();

      List<ProductionBill> productionBills = this.productionLines.get(productionType);
      for (ProductionBill bill : productionBills) {
         CompoundTag billTag = new CompoundTag();
         billTag.putString("recipe_name", bill.getMinecraftRecipeName());
         billTag.putString("production_type", bill.getProductionType().resourceLocation().toString());
         billTag.putString("production_strategy_type", bill.getProductionStrategy().getType().name());
         billTag.putInt("bill_amount", bill.getBillAmount());
         billTag.putBoolean("enabled", bill.isEnabled());
         billTag.put("input_items", NbtHelper.writeItemList(bill.getInputItems(), registryAccess));
         billTag.put("ingredient_filters", bill.getIngredientFilters().toNbt(registryAccess));
         billTag.put("display_item", bill.getDisplayItem().save(registryAccess, new CompoundTag()));
         list.add(billTag);
      }

      tag.put("production_bills", list);
      rootTag.put("production_bills_" + productionType.name().toLowerCase(), tag);
   }

   public Optional<List<ProductionBill>> tryGetProductionBills(ProductionType productionType) {
      return Optional.ofNullable(productionLines.get(productionType));
   }

   public List<ProductionBill> getProductionBills(ProductionType productionType) {
      return tryGetProductionBills(productionType).orElseThrow(
            () -> new UnsupportedOperationException(
                  "Artisan building does not support production type: " + productionType));
   }

   public void addBill(ProductionBill productionBill) {
      productionLines.computeIfPresent(productionBill.getProductionType(), (type, bills) -> {
         bills.add(productionBill);
         return bills;
      });
   }

   public void removeBill(ProductionType productionType, int index) {
      productionLines.computeIfPresent(productionType, (type, bills) -> {
         bills.remove(index);
         return bills;
      });
   }

   public void replaceBill(int index, ProductionBill productionBill) {
      productionLines.computeIfPresent(productionBill.getProductionType(), (type, bills) -> {
         bills.set(index, productionBill);
         return bills;
      });

   }

   protected abstract List<ProductionType> getSupportedProductionTypes();

   protected abstract List<ProductionBill> getDefaultProductionBills(ProductionType productionType);

   public boolean tryLoadDefaultProductionBills() {

      boolean result = false;
      for (Map.Entry<ProductionType, List<ProductionBill>> line : productionLines.entrySet()) {
         if (line.getValue().isEmpty()) {
            productionLines.put(line.getKey(), getDefaultProductionBills(line.getKey()));
            result = true;
         }
      }

      return result;
   }

   public RecipeEvaluation serverEvaluateRecipe(
         ProductionType productionType,
         List<ItemStack> inputs,
         ServerLevel level) {
      Optional<ProductionRecipe> recipe = productionType.recipeLookup().find(inputs, level);
      if (recipe.isEmpty())
         return new RecipeEvaluation(Optional.empty(), RecipeAllowed.INVALID_RECIPE);

      boolean allowed = recipeAllowed(productionType, recipe.get().input(), recipe.get().result(), level);
      return new RecipeEvaluation(recipe, allowed ? RecipeAllowed.ALLOWED : RecipeAllowed.NOT_ALLOWED);
   }

   public record RecipeEvaluation(Optional<ProductionRecipe> recipe, RecipeAllowed recipeAllowed) {

      public ItemStack resultItem() {
         return recipe.map(ProductionRecipe::result).orElse(ItemStack.EMPTY);
      }
   }

   public abstract boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level);

   public abstract Tooltip getAllowedRecipeHelpTooltip();

   protected static boolean recipeHasAtLeastOneIngredientWithMatchingTag(
         RecipeInput recipeInput,
         List<TagKey<Item>> allowedTags) {
      for (int i = 0; i < recipeInput.size(); ++i) {
         ItemStack item = recipeInput.getItem(i);

         if (itemHasMatchingTag(item, allowedTags))
            return true;
      }

      return false;
   }

   protected static boolean recipeHasNoIngredientsWithMatchingTag(
         RecipeInput recipeInput,
         List<TagKey<Item>> allowedTags) {
      for (int i = 0; i < recipeInput.size(); ++i) {
         ItemStack item = recipeInput.getItem(i);

         if (itemHasMatchingTag(item, allowedTags))
            return false;
      }

      return true;
   }

   protected static boolean itemHasMatchingTag(ItemStack item, List<TagKey<Item>> allowedTags) {
      return allowedTags.stream().anyMatch(item::is);
   }

   protected static boolean itemDoesNotHaveMatchingTag(ItemStack item, List<TagKey<Item>> allowedTags) {
      return allowedTags.stream().noneMatch(item::is);
   }

   protected static boolean itemIsOneOf(ItemStack item, List<Item> allowedItems) {
      return allowedItems.stream().anyMatch(item::is);
   }

   protected static boolean recipeHasAtLeastOneIngredientThatIsOneOf(RecipeInput recipeInput, List<Item> allowedItems) {
      for (int i = 0; i < recipeInput.size(); ++i) {
         ItemStack item = recipeInput.getItem(i);

         if (itemIsOneOf(item, allowedItems))
            return true;
      }

      return false;
   }

   protected static ProductionBill createDefaultBill(
         String recipe,
         ProductionType productionType,
         List<ItemStack> inputItems,
         ItemStack displayItem) {
      return new ProductionBill(
            recipe,
            productionType,
            ProductionStrategyType.PRODUCE_UP_TO,
            64,
            true,
            inputItems,
            productionType.createEmptyIngredientFilters().get(),
            displayItem);
   }
}
