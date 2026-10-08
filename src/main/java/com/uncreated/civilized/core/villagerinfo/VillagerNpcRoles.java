package com.uncreated.civilized.core.villagerinfo;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.dialogue.questline.advisor.controller.AdvisorDialogueController;
import com.uncreated.civilized.core.dialogue.questline.migrant.controller.MigrantDialogueController;
import com.uncreated.civilized.core.dialogue.questline.worker.controller.WorkerDialogueController;
import com.uncreated.civilized.entity.MerchantVisitorBehaviour;
import com.uncreated.civilized.entity.VisitorBehaviour;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class VillagerNpcRoles {

   private static final ResourceKey<Registry<VillagerNpcRole>> NPC_ROLES_KEY =
         ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "villager_npc_roles"));
   private static final Registry<VillagerNpcRole> NPC_ROLES_INTERNAL = new RegistryBuilder<>(NPC_ROLES_KEY).create();

   public static final DeferredRegister<VillagerNpcRole> NPC_ROLES =
         DeferredRegister.create(NPC_ROLES_INTERNAL, CIVILIZED_MOD_ID);

   private static final List<VillagerNpcRole> ALL = new ArrayList<>();

   private static VillagerNpcRole declare(VillagerNpcRole role) {
      ALL.add(role);
      return role;
   }

   public static List<VillagerNpcRole> all() {
      return Collections.unmodifiableList(ALL);
   }

   public static ResourceLocation createResourceKey(String roleName) {
      return ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, roleName);
   }

   /** Falls back to {@link #NONE} for roles that no longer exist, e.g. ones a removed addon registered. */
   public static VillagerNpcRole getFromResourceLocation(ResourceLocation resourceLocation) {
      return Objects.requireNonNullElse(NPC_ROLES.getRegistry().get().getValue(resourceLocation), NONE);
   }

   @SubscribeEvent
   private static void registerRegistry(NewRegistryEvent event) {
      event.register(NPC_ROLES_INTERNAL);
   }

   @SubscribeEvent
   private static void registerTypes(RegisterEvent event) {
      event.register(NPC_ROLES_KEY, registry -> ALL.forEach(role -> registry.register(role.resourceLocation(), role)));
   }

   public static final VillagerNpcRole NONE = declare(VillagerNpcRole.builder(createResourceKey("none")).build());
   // settlement members
   public static final VillagerNpcRole WORKER =
         declare(
               VillagerNpcRole.builder(createResourceKey("worker"))
                     .dialogueController(WorkerDialogueController::new)
                     .build());
   public static final VillagerNpcRole SPOUSE =
         declare(VillagerNpcRole.builder(createResourceKey("spouse")).genderedTitle(true).build());
   public static final VillagerNpcRole ADVISOR =
         declare(
               VillagerNpcRole.builder(createResourceKey("advisor"))
                     .clothingSets(List.of("patrician"))
                     .dialogueController(AdvisorDialogueController::new)
                     .build());
   // visitors
   public static final VillagerNpcRole TRAVELLER =
         declare(
               VillagerNpcRole.builder(createResourceKey("traveller"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .build());
   public static final VillagerNpcRole MIGRANT =
         declare(
               VillagerNpcRole.builder(createResourceKey("migrant"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .dialogueController(MigrantDialogueController::new)
                     .build());
   public static final VillagerNpcRole SUITOR =
         declare(
               VillagerNpcRole.builder(createResourceKey("suitor"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .genderedTitle(true)
                     .build());
   public static final VillagerNpcRole SKILLED_PROFESSIONAL =
         declare(
               VillagerNpcRole.builder(createResourceKey("skilled_professional"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .build());
   public static final VillagerNpcRole MERCENARY =
         declare(
               VillagerNpcRole.builder(createResourceKey("mercenary"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .build());
   public static final VillagerNpcRole BEGGAR =
         declare(
               VillagerNpcRole.builder(createResourceKey("beggar"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .build());
   public static final VillagerNpcRole SCOUNDREL =
         declare(
               VillagerNpcRole.builder(createResourceKey("scoundrel"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .build());
   public static final VillagerNpcRole THIEF =
         declare(
               VillagerNpcRole.builder(createResourceKey("thief"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .build());
   public static final VillagerNpcRole MERCHANT =
         declare(
               VillagerNpcRole.builder(createResourceKey("merchant"))
                     .clothingSets(List.of("patrician"))
                     .createRoleBehaviour(MerchantVisitorBehaviour::new)
                     .build());
   public static final VillagerNpcRole BANDIT =
         declare(
               VillagerNpcRole.builder(createResourceKey("bandit"))
                     .createRoleBehaviour(VisitorBehaviour::new)
                     .build());
}
