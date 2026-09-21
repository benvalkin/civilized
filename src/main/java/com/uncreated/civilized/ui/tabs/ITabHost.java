package com.uncreated.civilized.ui.tabs;


/**
 * A screen (plain screen or menu screen) that shows tabs.
 **/
public interface ITabHost {

   TabCoords getTabCoords();

   ATab changeTab(ATab newTab);

   int getFirstTabButtonX();

   int getFirstTabButtonY();
}
