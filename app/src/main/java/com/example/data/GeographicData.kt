package com.example.data

data class GeographicHierarchy(
    val districts: List<String> = listOf("Kampala", "Wakiso", "Mukono", "Jinja", "Gulu", "Mbarara"),
    val subcounties: Map<String, List<String>> = mapOf(
        "Kampala" to listOf("Kampala Central", "Nakawa Division", "Makindye Division", "Kawempe Division", "Rubaga Division"),
        "Wakiso" to listOf("Kyengera TC", "Nansana Municipality", "Entebbe Municipality", "Kira Municipality", "Makindye Ssabagabo"),
        "Mukono" to listOf("Mukono Central", "Goma Division", "Central Division"),
        "Jinja" to listOf("Jinja South", "Jinja North", "Kakira"),
        "Gulu" to listOf("Laroo-Pece", "Bardege-Layibi"),
        "Mbarara" to listOf("Kakoba", "Kamukuzi", "Nyamitanga")
    ),
    val villages: Map<String, List<String>> = mapOf(
        "Kampala Central" to listOf("Kololo", "Nakasero", "Kamwokya", "Kisenyi"),
        "Nakawa Division" to listOf("Bugolobi", "Bukoto", "Ntinda", "Naguru", "Luzira"),
        "Makindye Division" to listOf("Muyenga", "Kabalagala", "Kansanga", "Kibuli"),
        "Kawempe Division" to listOf("Kalerwe", "Bwaise", "Mulago"),
        "Rubaga Division" to listOf("Mengo", "Rubaga", "Lubya"),
        "Kyengera TC" to listOf("Seguku", "Bulenga", "Kyengera Central"),
        "Nansana Municipality" to listOf("Nansana", "Ggaba", "Namungoona"),
        "Entebbe Municipality" to listOf("Kisubi", "Kajjansi", "Entebbe Town"),
        "Kira Municipality" to listOf("Namugongo", "Kyaliwajjala", "Kira Town"),
        "Makindye Ssabagabo" to listOf("Lubowa", "Mutundwe", "Bunamwaya"),
        "Mukono Central" to listOf("Wantoni", "Kauga", "Nabuti"),
        "Goma Division" to listOf("Seeta Town", "Misindye"),
        "Central Division" to listOf("Nassuuti", "Kigombya"),
        "Jinja South" to listOf("Rippon", "Walukuba", "Mpumudde"),
        "Jinja North" to listOf("Bugembe", "Mafubira"),
        "Kakira" to listOf("Kakira Estate", "Mwiri"),
        "Laroo-Pece" to listOf("Pece", "Senior Quarters"),
        "Bardege-Layibi" to listOf("Layibi", "Bardege"),
        "Kakoba" to listOf("Kakoba", "Ruharo"),
        "Kamukuzi" to listOf("Kamukuzi", "Rwebikoona"),
        "Nyamitanga" to listOf("Nyamitanga", "Katete")
    )
)
