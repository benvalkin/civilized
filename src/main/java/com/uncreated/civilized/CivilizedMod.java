package com.uncreated.civilized;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirements;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.entity.events.EntityEvents;
import com.uncreated.civilized.core.settlement.events.SettlementStoreEvents;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.events.VillagerStoreEvents;
import com.uncreated.civilized.neoforge.registration.BlockRegistry;
import com.uncreated.civilized.neoforge.registration.ItemRegistry;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;
import com.uncreated.civilized.neoforge.registration.attachments.DataAttachments;
import com.uncreated.civilized.neoforge.registration.attachments.MyDataComponents;
import com.uncreated.civilized.neoforge.registration.creativetab.CreativeTab;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;
import com.uncreated.civilized.neoforge.registration.entity.EntitySetupEventsClient;
import com.uncreated.civilized.neoforge.registration.entity.EntitySetupEventsCommon;
import com.uncreated.civilized.neoforge.registration.gui.GuiRegistry;
import com.uncreated.civilized.neoforge.registration.gui.GuiSetupEvents;
import com.uncreated.civilized.networking.PacketRegistry;
import com.uncreated.civilized.ui.events.UIUpdateEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CivilizedMod.CIVILIZED_MOD_ID)
public class CivilizedMod {
   // Define mod id in a common place for everything to reference
   public static final String CIVILIZED_MOD_ID = "civilized";
   // Directly reference a slf4j logger
   private static final Logger LOGGER = LogUtils.getLogger();

   // The constructor for the mod class is the first code that is run when your mod is loaded.
   // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
   public CivilizedMod(IEventBus modEventBus, ModContainer modContainer) {
      // Register the commonSetup method for modloading
      modEventBus.addListener(this::commonSetup);

      // Register the Deferred Register to the mod event bus so blocks get registered
      BlockRegistry.BLOCKS.register(modEventBus);
      BlockRegistry.BLOCK_ENTITIES.register(modEventBus);
      ItemRegistry.ITEMS.register(modEventBus);
      EntityRegistry.ENTITIES.register(modEventBus);
      GuiRegistry.MENUS.register(modEventBus);
      AIRegistry.ACTIVITIES.register(modEventBus);
      AIRegistry.MEMORY_MODULES.register(modEventBus);
      AIRegistry.SENSORS.register(modEventBus);
      AIRegistry.SCHEDULES.register(modEventBus);
      DataAttachments.ATTACHMENTS.register(modEventBus);
      MyDataComponents.COMPONENTS.register(modEventBus);

      BuildingRequirements.BUILDING_REQUIREMENTS.register(modEventBus);
      BuildingTypes.BUILDING_TYPES.register(modEventBus);

      CreativeTab.CREATIVE_MODE_TABS.register(modEventBus);

      // Register ourselves for server and other game events we are interested in.
      // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
      // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting()
      // below.
      NeoForge.EVENT_BUS.register(this);

      NeoForge.EVENT_BUS.register(SettlementStoreEvents.class);
      NeoForge.EVENT_BUS.register(EntityEvents.class);
      NeoForge.EVENT_BUS.register(VillagerStoreEvents.class);
      NeoForge.EVENT_BUS.register(UIUpdateEvents.class);

      // Register the item to a creative tab
      modEventBus.addListener(this::addCreative);

      modEventBus.register(EntitySetupEventsClient.class);
      modEventBus.register(EntitySetupEventsCommon.class);
      modEventBus.register(GuiSetupEvents.class);
      modEventBus.register(PacketRegistry.class);

      // Register our mod's ModConfigSpec so that FML can create and load the config file for us
      modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
   }

   private void commonSetup(final FMLCommonSetupEvent event) {
      // Some common setup code
      LOGGER.info("HELLO FROM COMMON SETUP");

      if (Config.logDirtBlock)
         LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

      LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

      Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
   }

   // Add the example block item to the building blocks tab
   private void addCreative(BuildCreativeModeTabContentsEvent event) {
      // if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
      // event.accept(BlockRegistry.EXAMPLE_BLOCK_ITEM);
   }

   // You can use SubscribeEvent and let the Event Bus discover methods to call
   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
      // Do something when the server starts
      LOGGER.info("HELLO from server starting");
   }

   @SubscribeEvent
   public void onLevelLoad(LevelEvent.Load event) {
      // Do something when the server starts
      if (event.getLevel().isClientSide() || event.getLevel().getServer() == null)
         return;

      ServerSettlementsStore.loadServer(event.getLevel().getServer());
      ServerBuildingsStore.loadServer(event.getLevel().getServer());
      ServerVillagerStore.loadServer(event.getLevel().getServer());
      LOGGER.info("Loaded settlements");
   }

   // You can use EventBusSubscriber to automatically register all static methods in the class annotated with
   // @SubscribeEvent
   @EventBusSubscriber(modid = CIVILIZED_MOD_ID, value = Dist.CLIENT)
   public static class ClientModEvents {
      @SubscribeEvent
      public static void onClientSetup(FMLClientSetupEvent event) {
         // Some client setup code
         LOGGER.info("HELLO FROM CLIENT SETUP");
         LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
      }
   }
}
