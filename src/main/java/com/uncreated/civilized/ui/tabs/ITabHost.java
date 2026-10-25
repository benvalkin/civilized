package com.uncreated.civilized.ui.tabs;

import org.jetbrains.annotations.Nullable;

/**
 * A screen (plain screen or menu screen) that shows tabs.
 **/
public interface ITabHost {

   TabCoords getTabCoords();

   ATab changeTab(ATab newTab);

   @Nullable
   ATab getCurrentTab();

   int getFirstTabButtonX();

   int getFirstTabButtonY();
}
