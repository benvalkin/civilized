package com.uncreated.civilized.commands;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.trading.MerchantType;
import com.uncreated.civilized.core.trading.MerchantTypes;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.MerchantVisitorBehaviour;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

public class SummonMerchant {

   private static final SimpleCommandExceptionType ERROR_NO_MERCHANT_TYPES =
         new SimpleCommandExceptionType(Component.literal("No merchant types are loaded."));
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_MERCHANT_TYPE =
         new DynamicCommandExceptionType(type -> Component.literal("Unknown merchant type: " + type));
   private static final SimpleCommandExceptionType ERROR_ENTITY_SPAWN_ERROR =
         new SimpleCommandExceptionType(Component.literal("Something went wrong while spawning the merchant."));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            Commands.literal("summonmerchant")
                  .requires(source -> source.hasPermission(2))
                  .executes(context -> summon(context.getSource(), Optional.empty()))
                  .then(
                        Commands.argument("type", ResourceLocationArgument.id())
                              // merchant types aren't synced to clients, so the server has to suggest them
                              .suggests(
                                    (context, builder) -> SharedSuggestionProvider.suggestResource(
                                          context.getSource()
                                                .registryAccess()
                                                .lookupOrThrow(MerchantTypes.REGISTRY_KEY)
                                                .keySet(),
                                          builder))
                              .executes(
                                    context -> summon(
                                          context.getSource(),
                                          Optional.of(ResourceLocationArgument.getId(context, "type"))))));
   }

   private static int summon(CommandSourceStack source, Optional<ResourceLocation> typeId)
         throws CommandSyntaxException {

      ServerLevel level = source.getLevel();
      Holder.Reference<MerchantType> type = findMerchantType(source, typeId);

      BlockPos pos = BlockPos.containing(source.getPosition());
      CivilizedVillager villager = EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, pos, EntitySpawnReason.EVENT);
      if (villager == null)
         throw ERROR_ENTITY_SPAWN_ERROR.create();

      villager.changeNpcRole(VillagerNpcRoles.MERCHANT);
      if (villager.getRoleBehaviour() instanceof MerchantVisitorBehaviour merchant)
         merchant.stockWithNewTrades(type);

      Optional<Building> building = ServerBuildingsStore.INSTANCE.findEnclosingBuilding(pos, level);
      if (building.isPresent() && building.get().getBuildingType().is(BuildingTypes.TOWN_SQUARE))
         villager.getInfo().setHomeBuildingId(building.get().getBuildingId());

      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);
      // the villager's activities may have changed with its role
      villager.refreshBrain(level);

      source.sendSuccess(
            () -> Component
                  .literal("Summoned a " + type.key().location() + " merchant: " + villager.getInfo().getFullName()),
            true);
      return 1;
   }

   private static Holder.Reference<MerchantType> findMerchantType(
         CommandSourceStack source,
         Optional<ResourceLocation> typeId)
         throws CommandSyntaxException {

      if (typeId.isEmpty())
         return MerchantTypes.pickRandom(source.registryAccess(), source.getLevel().getRandom())
               .orElseThrow(ERROR_NO_MERCHANT_TYPES::create);

      return source.registryAccess()
            .lookupOrThrow(MerchantTypes.REGISTRY_KEY)
            .get(ResourceKey.create(MerchantTypes.REGISTRY_KEY, typeId.get()))
            .orElseThrow(() -> ERROR_UNKNOWN_MERCHANT_TYPE.create(typeId.get()));
   }
}
