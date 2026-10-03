package com.uncreated.civilized.commands.events;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.commands.OpenTradingMenu;
import com.uncreated.civilized.commands.SetSettlementAccess;
import com.uncreated.civilized.commands.SummonMerchant;
import com.uncreated.civilized.commands.SummonResident;
import com.uncreated.civilized.core.building.events.model.BuildingUpdatedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class CommandRegistry {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SummonResident.register(event.getDispatcher());
        SetSettlementAccess.register(event.getDispatcher());
        OpenTradingMenu.register(event.getDispatcher());
        SummonMerchant.register(event.getDispatcher());
    }
}