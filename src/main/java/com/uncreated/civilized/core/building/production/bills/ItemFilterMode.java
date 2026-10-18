package com.uncreated.civilized.core.building.production.bills;

public enum ItemFilterMode {
    WHITELIST,
    BLACKLIST,
    /**Same as whitelist, but does not allow any item if there are no items in the filter.*/
    WHITELIST_STRICT
}
