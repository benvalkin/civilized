package com.uncreated.civilized.ui.tabs;

import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs.TabCoords;

/**
 * A screen (plain screen or menu screen) that shows tabs.
 **/
public interface ILazyLoadTabHost {

   TabCoords getTabCoords();

   ATab changeTab(ATab newTab);

   int getFirstTabButtonX();

   int getFirstTabButtonY();
}
