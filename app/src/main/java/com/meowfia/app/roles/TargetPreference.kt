package com.meowfia.app.roles

/**
 * Hints for bot targeting logic. Each role declares what kind of
 * target a smart bot should prefer. BotBrain uses this with the
 * bot's nightSkill to choose between smart and random targeting.
 */
enum class TargetPreference {
    /** No preference — pick randomly. */
    RANDOM,
    /** Target the opposite alignment (e.g. Hawk looking for Meowfia). */
    OPPOSITE_TEAM,
    /** Target the same alignment (e.g. Pigeon laying eggs for allies). */
    SAME_TEAM,
    /** Target non-buffer roles (e.g. Eagle wanting popular targets). */
    INTERESTING_ROLES,
    /** Target players who are likely visiting someone (e.g. Falcon, Switcheroo). */
    ACTIVE_VISITORS
}
