package com.example.wardrobe

object CategoryHierarchy {
    val hierarchy: Map<String, List<String>> = mapOf(
        "Tops" to listOf(
            "T-Shirts", "T-Shirts Cropped", "Longsleeves", "Longsleeves Cropped",
            "Tanks", "Tanks Cropped", "Blouses", "Sweatshirts", "Sweatshirts Cropped",
            "Hoodies", "Sweaters", "Sweaters Cropped", "Bodies", "Sports Bras"
        ),
        "Pants" to listOf(
            "Jeans", "Trousers", "Joggers", "Leggings", "Shorts"
        ),
        "Skirts" to listOf(
            "Skirts Mini", "Skirts Midi", "Skirts Maxi"
        ),
        "Dresses" to listOf(
            "Dresses Mini", "Dresses Midi", "Dresses Maxi"
        ),
        "Outerwear" to listOf(
            "Coats", "Trench Coats", "Blazers", "Jackets", "Biker Jackets",
            "Cardigans", "Zippers", "Fleece Jackets", "Parkas", "Overshirts"
        ),
        "Shoes" to listOf(
            "Sneakers", "Sports Shoes", "Hiking Shoes", "Boots", "Flats",
            "Heels", "Sandals", "Ankles"
        ),
        "Bags" to listOf(
            "Tote", "Shoulder", "Crossbody", "Backpacks", "Clutch"
        ),
        "Headwear" to listOf(
            "Cap", "Hat", "Beanie",
        ),
        "Jewelry" to listOf(
            "Earrings", "Necklaces", "Bracelets", "Rings"
        ),
        "Accessories" to listOf(
            "Scarves", "Watches",
            "Gloves", "Belts", "Tights"
        ),
        "Homewear" to listOf("Pajama", "Nightgown", "Robe"),
        "Beachwear" to listOf("Bikini", "Swimsuit"),
        "Hair" to listOf("Hair Styles", "Hair Ties", "Hair Bands", "Hair Clips"),
        "Eyewear" to listOf("Glasses", "Sunglasses")
    )

    val superCategories: List<String> = hierarchy.keys.sorted()
}