package com.anleanja.wardrobe.navigation

import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.WardrobeItem

sealed class NavigationEvent {
    data object NavigateToAddItem : NavigationEvent()
    data class NavigateToItemDetail(val item: WardrobeItem) : NavigationEvent()
    data object NavigateToAddOutfit : NavigationEvent()
    data class NavigateToOutfitDetail(val outfit: Outfit) : NavigationEvent()
    data class NavigateToScheduleOutfit(val outfit: Outfit) : NavigationEvent()
    data object NavigateBack : NavigationEvent()
}
