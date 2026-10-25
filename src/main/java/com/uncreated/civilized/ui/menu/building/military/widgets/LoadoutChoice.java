package com.uncreated.civilized.ui.menu.building.military.widgets;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class LoadoutChoice {
    @Nullable
    private final Loadout loadout;

    private LoadoutChoice(@Nullable Loadout loadout) {
        this.loadout = loadout;
    }

    public Optional<Loadout> getLoadout() {
        return Optional.ofNullable(loadout);
    }

    public static LoadoutChoice of(@NotNull Loadout loadout) {
        return new LoadoutChoice(loadout);
    }


    public static LoadoutChoice noLoadout() {
        return new LoadoutChoice(null);
    }
}
