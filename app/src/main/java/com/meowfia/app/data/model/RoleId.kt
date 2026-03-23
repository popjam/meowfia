package com.meowfia.app.data.model

/**
 * Every known role and flower in Meowfia.
 * The [implemented] flag controls whether the role appears in the manual pool selector.
 */
enum class RoleId(
    val displayName: String,
    val cardType: CardType,
    val description: String,
    val implemented: Boolean = true
) {
    // === BUFFERS (always in pool) ===
    PIGEON("Pigeon", CardType.FARM_ANIMAL,
        "Visit a player and lay an egg in their nest."),
    HOUSE_CAT("House Cat", CardType.MEOWFIA_ANIMAL,
        "Visit a player and steal an egg. Learn their role and who they visited."),

    // === V1 FARM ANIMALS ===
    HAWK("Hawk", CardType.FARM_ANIMAL,
        "Visit a player. If Meowfia, lay an egg in your nest."),
    OWL("Owl", CardType.FARM_ANIMAL,
        "Visit a player. Learn animals that visited them. If none, lay an egg."),
    EAGLE("Eagle", CardType.FARM_ANIMAL,
        "Visit a player. Gain eggs equal to their visitor count."),
    TURKEY("Turkey", CardType.FARM_ANIMAL,
        "Lay an egg for every player who visited you."),
    FALCON("Falcon", CardType.FARM_ANIMAL,
        "Visit a player. Lay an egg in the nest of who they visited."),
    MOSQUITO("Mosquito", CardType.FARM_ANIMAL,
        "Visit a random player and lay an egg in their nest."),
    CHICKEN("Chicken", CardType.FARM_ANIMAL,
        "Visit a player and lay 2 eggs. Lose if anyone throws a single egg at you."),
    TIT("Tit", CardType.FARM_ANIMAL,
        "Visit a random Meowfia player and lay an egg in their nest."),
    BLACK_SWAN("Black Swan", CardType.FARM_ANIMAL,
        "Visit yourself. If still Black Swan, lay an egg in your nest."),

    // === V1 FLOWERS ===
    SUNFLOWER("Sunflower", CardType.FLOWER,
        "Day phase: one player may publicly reveal their role to gain 2 eggs."),
    DANDELION("Dandelion", CardType.FLOWER,
        "Night actions resolve in seat order instead of tier order this round."),
    WOLFSBANE("Wolfsbane", CardType.FLOWER,
        "On reveal: all Meowfia players gain 1 extra egg this round."),

    // === FUTURE ROLES (not implemented) ===
    BLIND_HAWK("Blind Hawk", CardType.FARM_ANIMAL,
        "Investigate, but learn only alignment — not role.", implemented = false),
    PERSIAN("Persian", CardType.MEOWFIA_ANIMAL,
        "Steal and create a fake nest egg.", implemented = false),
    MOUSER("Mouser", CardType.MEOWFIA_ANIMAL,
        "Steal and gain a Wink.", implemented = false),
    FLOOFER("Floofer", CardType.MEOWFIA_ANIMAL,
        "Steal and hug your target.", implemented = false);

    val isBuffer: Boolean get() = this == PIGEON || this == HOUSE_CAT
    val isFlower: Boolean get() = cardType == CardType.FLOWER
    val isFarmAnimal: Boolean get() = cardType == CardType.FARM_ANIMAL
    val isMeowfiaAnimal: Boolean get() = cardType == CardType.MEOWFIA_ANIMAL
}
