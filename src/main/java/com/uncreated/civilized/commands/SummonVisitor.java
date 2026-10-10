package com.uncreated.civilized.commands;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.behaviour.TownSquareBehaviour;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.trading.MerchantType;
import com.uncreated.civilized.core.trading.MerchantTypes;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRole;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.Util;
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

/**
 * Dev command which spawns a visitor at the town square of the settlement the command is run in, the same way the town
 * square spawns them by itself. {@code /summonvisitor [role] [merchant_type]}
 */
public class SummonVisitor {

   private static final SimpleCommandExceptionType ERROR_NOT_IN_SETTLEMENT =
         new SimpleCommandExceptionType(Component.literal("You must be inside a settlement to use this command."));
   private static final SimpleCommandExceptionType ERROR_NO_TOWN_SQUARE =
         new SimpleCommandExceptionType(Component.literal("This settlement doesn't have a town square."));
   private static final DynamicCommandExceptionType ERROR_NOT_A_VISITOR_ROLE =
         new DynamicCommandExceptionType(role -> Component.literal("Not a visitor role: " + role));
   private static final DynamicCommandExceptionType ERROR_NOT_A_MERCHANT =
         new DynamicCommandExceptionType(role -> Component.literal("Only merchants have a merchant type, not " + role));
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_MERCHANT_TYPE =
         new DynamicCommandExceptionType(type -> Component.literal("Unknown merchant type: " + type));
   private static final SimpleCommandExceptionType ERROR_ENTITY_SPAWN_ERROR =
         new SimpleCommandExceptionType(
               Component.literal(
                     "Something went wrong while spawning the visitor. There may be nowhere to stand in the town square."));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            Commands.literal("summonvisitor")
                  .requires(source -> source.hasPermission(2))
                  .executes(context -> summon(context.getSource(), null, null))
                  .then(
                        Commands.argument("role", ResourceLocationArgument.id())
                              .suggests(
                                    (context, builder) -> SharedSuggestionProvider.suggestResource(
                                          TownSquareBehaviour.visitorRoles()
                                                .stream()
                                                .map(VillagerNpcRole::resourceLocation),
                                          builder))
                              .executes(context -> summon(context.getSource(), getRole(context), null))
                              .then(
                                    Commands.argument("merchant_type", ResourceLocationArgument.id())
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
                                                      getRole(context),
                                                      ResourceLocationArgument.getId(context, "merchant_type"))))));
   }

   private static VillagerNpcRole getRole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      ResourceLocation roleId = ResourceLocationArgument.getId(context, "role");
      return TownSquareBehaviour.visitorRoles()
            .stream()
            .filter(role -> role.resourceLocation().equals(roleId))
            .findFirst()
            .orElseThrow(() -> ERROR_NOT_A_VISITOR_ROLE.create(roleId));
   }

   private static int summon(
         CommandSourceStack source,
         @Nullable VillagerNpcRole role,
         @Nullable ResourceLocation merchantTypeId)
         throws CommandSyntaxException {

      ServerLevel level = source.getLevel();
      Building townSquare = findTownSquare(source);

      if (role == null) {
         List<VillagerNpcRole> roles = TownSquareBehaviour.visitorRoles();
         role = Util.getRandom(roles, level.getRandom());
      }

      Holder.Reference<MerchantType> merchantType = null;
      if (merchantTypeId != null) {
         if (!role.is(VillagerNpcRoles.MERCHANT))
            throw ERROR_NOT_A_MERCHANT.create(role);
         merchantType = findMerchantType(source, merchantTypeId);
      }

      CivilizedVillager villager =
            TownSquareBehaviour.spawnVisitor(level, townSquare, role, merchantType)
                  .orElseThrow(ERROR_ENTITY_SPAWN_ERROR::create);

      VillagerNpcRole spawnedRole = role;
      source.sendSuccess(
            () -> Component.literal("Summoned a " + spawnedRole + " visitor: " + villager.getInfo().getFullName()),
            true);
      return 1;
   }

   private static Building findTownSquare(CommandSourceStack source) throws CommandSyntaxException {
      Settlement settlement =
            ServerSettlementsStore.INSTANCE.findEnclosing(BlockPos.containing(source.getPosition()), source.getLevel())
                  .orElseThrow(ERROR_NOT_IN_SETTLEMENT::create);

      return ServerBuildingsStore.INSTANCE.findTownSquare(settlement.getSettlementId())
            .orElseThrow(ERROR_NO_TOWN_SQUARE::create);
   }

   private static Holder.Reference<MerchantType> findMerchantType(CommandSourceStack source, ResourceLocation typeId)
         throws CommandSyntaxException {

      return source.registryAccess()
            .lookupOrThrow(MerchantTypes.REGISTRY_KEY)
            .get(ResourceKey.create(MerchantTypes.REGISTRY_KEY, typeId))
            .orElseThrow(() -> ERROR_UNKNOWN_MERCHANT_TYPE.create(typeId));
   }
}
