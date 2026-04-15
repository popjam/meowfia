# MEOWFIA

**A social deduction card game where nobody knows who the cats are, not even the cats.**

Meowfia is a 4-8 player tabletop game of bluffing, deduction, and risk management, paired with a companion Android app that acts as game master. Each round, every player is independently assigned Farm or Meowfia, roles are drawn from the current pool, night actions resolve secretly through the app, and players then bet real cards from their hand on who they think should be eggsecuted.

The game is not about survival. It is about scoring across multiple rounds.

## Core Pitch

Two things define Meowfia:

- Alignments are rerolled every round. A table can have zero Meowfia, one Meowfia, several Meowfia, or even all Meowfia.
- Voting is gambling. You throw hidden-value cards from your hand at another player. If your team wins, those cards bank at full value. If your team loses, they are discarded unless you at least targeted the opposite team and reclaim your best thrown card.

The result is a social deduction game where reading the room and pricing risk matter as much as identifying liars.

## Current Rules Model

The current design center is the `v8` role model:

- Your hand is your **nest**.
- "Lay an egg in a player's nest" means they draw a card.
- "Steal or lose an egg" means cards are discarded from hand.
- The app tracks hidden state, night visits, role changes, status effects, and dawn reports.
- Physical cards still matter for scoring and voting; the app does not replace the deck.

The formal rules document in the repo is still [docs/meowfia_rules_v6.md](/Users/james/stuff-large/meowfia/docs/meowfia_rules_v6.md:1), while the current role set is [docs/meowfia_roles_v8.md](/Users/james/stuff-large/meowfia/docs/meowfia_roles_v8.md:1). When those differ, treat the `v8` roles doc as the more current mechanical reference.

## Round Flow

1. **Pool Phase**: reveal 3 cards into the pool. Animals become available roles; flowers trigger round modifiers.
2. **Setup Phase**: players draw up to their round hand, then the app assigns alignment and role from the pool.
3. **Night Phase**: each player secretly performs their role through the app.
4. **Dawn Phase**: each player privately learns what changed and draws or discards accordingly.
5. **Day Phase**: open discussion, bluffing, accusations, and flower-driven chaos.
6. **Eggsecution**: players secretly choose a target and throw one or more cards from hand.
7. **Resolution and scoring**: the eliminated player's alignment determines which team won the round.

## Scoring

Final score is:

`score pile card values + 1 point per card left in hand`

That creates the central tension:

- keep cards for a guaranteed low-value floor
- throw cards for a chance to bank them at full value

Wilds are worth `0` in score piles but still count as votes.

## Roles and Flowers

The current design docs define:

- `40` adapted roles in [docs/meowfia_roles_v8.md](/Users/james/stuff-large/meowfia/docs/meowfia_roles_v8.md:1)
- `36` flowers in [docs/meowfia_flowers.md](/Users/james/stuff-large/meowfia/docs/meowfia_flowers.md:1)

Farm roles are mostly information, tracking, protection, status, and egg economy. Meowfia roles are mostly disruption, manipulation, alignment pressure, and selective intel. Flowers can alter communication, timing, voting, scoring, physical behavior, or even round structure.

## App Status

This repository contains the Android companion app in Kotlin with Jetpack Compose.

Implemented app foundations include:

- core game engine pieces such as `GameCoordinator`, `NightResolver`, `RoleAssigner`, `DawnReportGenerator`, and `PostRoundAnalyzer`
- modular role handlers via a role registry
- modular flower handlers via a flower registry
- QR scanning with CameraX and ML Kit
- pass-and-play UI building blocks for handoff, timers, player picking, bot actions, and role display
- simulation and reporting utilities under `app/src/test`

Current registered content in code:

- `24` role handlers, including both buffer roles
- `18` flower handlers

So the repo already contains a working engine/test bed for part of the design space, but it does not yet implement every role and flower described in the docs.

## Tech Stack

- Android app
- Kotlin
- Jetpack Compose
- CameraX
- ML Kit barcode scanning
- JUnit + Truth for testing

Project settings currently target:

- `minSdk 26`
- `targetSdk 35`
- Java/Kotlin `17`

## Repo Guide

- [docs/meowfia_rules_v6.md](/Users/james/stuff-large/meowfia/docs/meowfia_rules_v6.md:1): current full rules writeup
- [docs/meowfia_roles_v8.md](/Users/james/stuff-large/meowfia/docs/meowfia_roles_v8.md:1): current adapted role set
- [docs/meowfia_flowers.md](/Users/james/stuff-large/meowfia/docs/meowfia_flowers.md:1): flower event reference
- [app/src/main/java/com/meowfia/app/engine/GameCoordinator.kt](/Users/james/stuff-large/meowfia/app/src/main/java/com/meowfia/app/engine/GameCoordinator.kt:1): phase orchestration entry point
- [app/src/main/java/com/meowfia/app/data/registry/RoleRegistry.kt](/Users/james/stuff-large/meowfia/app/src/main/java/com/meowfia/app/data/registry/RoleRegistry.kt:1): registered role handlers
- [app/src/main/java/com/meowfia/app/flowers/FlowerRegistry.kt](/Users/james/stuff-large/meowfia/app/src/main/java/com/meowfia/app/flowers/FlowerRegistry.kt:1): registered flower handlers
- [app/src/test/java/com/meowfia/app/testing/BatchSimTest.kt](/Users/james/stuff-large/meowfia/app/src/test/java/com/meowfia/app/testing/BatchSimTest.kt:1): simulation entry points

## Current Gaps

The repo still has some intentional drift between design docs and implementation:

- the rules doc is older than the roles doc
- many documented roles and flowers are not yet implemented in the Android app
- the physical card production assets under `physical/` are present locally but are not part of the tracked app/docs flow yet

If you are changing mechanics, update the docs first, then bring the registries and handlers into alignment.
