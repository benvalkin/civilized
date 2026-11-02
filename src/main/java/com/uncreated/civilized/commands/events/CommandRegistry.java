package com.uncreated.civilized.commands.events;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.commands.DetectRooms;
import com.uncreated.civilized.commands.OpenTradingMenu;
import com.uncreated.civilized.commands.ShowPathfindingMetrics;
import com.uncreated.civilized.commands.SetItemQuality;
import com.uncreated.civilized.commands.SetSettlementAccess;
import com.uncreated.civilized.commands.SetVillagerAge;
import com.uncreated.civilized.commands.SetVillagerSkill;
import com.uncreated.civilized.commands.SummonChild;
import com.uncreated.civilized.commands.SummonWorker;
import com.uncreated.civilized.commands.SummonVisitor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class CommandRegistry {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SummonWorker.register(event.getDispatcher());
        SetSettlementAccess.register(event.getDispatcher());
        OpenTradingMenu.register(event.getDispatcher());
        SummonVisitor.register(event.getDispatcher());
        DetectRooms.register(event.getDispatcher());
        SummonChild.register(event.getDispatcher());
        SetVillagerAge.register(event.getDispatcher());
        SetVillagerSkill.register(event.getDispatcher());
        SetItemQuality.register(event.getDispatcher());
        ShowPathfindingMetrics.register(event.getDispatcher());
    }
}
