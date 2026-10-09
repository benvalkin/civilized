package com.uncreated.civilized.neoforge.registration.entity;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.entity.renderer.CivilizedModelLayers;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderer;
import com.uncreated.civilized.entity.stats.ClothingTextureRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


public class EntitySetupEventsClient {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.CIVILIZED_VILLAGER.get(), CivilizedVillagerRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        CivilizedModelLayers.registerLayerDefinitions(event);
    }

    @SubscribeEvent
    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        // finds the villager clothing textures whenever resources are loaded, e.g. at startup or with F3+T
        event.addListener(
              ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "villager_clothing"),
              (ResourceManagerReloadListener) ClothingTextureRegistry::reload);
    }
}
