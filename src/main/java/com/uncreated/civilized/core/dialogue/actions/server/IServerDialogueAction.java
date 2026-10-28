package com.uncreated.civilized.core.dialogue.actions.server;

import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.server.level.ServerPlayer;


public interface IServerDialogueAction {

   boolean run(ServerPlayer player, CivilizedVillager villager);
}
