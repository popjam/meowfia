# MEOWFIA

**A social deduction card game where nobody knows who the cats are — not even the cats.**

Meowfia is a 4–8 player tabletop game of bluffing, deduction, and calculated gambling, paired with a companion app that serves as the game master. Players are secretly assigned as either **Farm** (innocent) or **Meowfia** (cat) each round, gather information through secret night actions, then stake their cards on who they think the cats are. Every card in the game is an egg, and every egg is a bet.

---

## The Game

### What Makes Meowfia Different

Most social deduction games split the table into two teams that stay fixed for the whole game. Meowfia doesn't. Alignments are re-rolled every round — each player has an independent 1-in-3 chance of being Meowfia. That means there might be zero cats, one cat, five cats, or a table full of cats. Nobody knows the count, and Meowfia players don't know who else is Meowfia. The paranoia is real.

The other twist is the scoring. Meowfia isn't about surviving — it's about accumulating points across multiple rounds. When you vote to eliminate someone, you're literally throwing cards from your hand at them. Thrown cards are placed **face-down** — nobody sees what you risked. Higher-value cards are worth more in your score pile but cost more if you lose. The winning team banks all their thrown cards; the losing team discards theirs. But if you're on the losing team and you targeted someone on the opposite team, you get a consolation — your best thrown card comes back to your hand.

The winner isn't the best liar or the best detective — it's the player who read the room most accurately and gambled accordingly.

### How a Round Works

1. **Pool Phase** — The dealer reveals 3 cards from the deck. Their animal or flower artwork determines the available roles and special events for the round.
2. **Setup Phase** — Players draw cards and the app secretly assigns each player an alignment and a role from the pool.
3. **Night Phase** — Eyes closed, phone passed around. Each player reads their role's night action (visiting another player, laying eggs, stealing eggs, gathering information) and makes their choice.
4. **Dawn Phase** — Phone passed again. Each player privately learns how many eggs they gained or lost and any role-specific intel. Players draw cards (if gained) or discard cards (if lost) accordingly.
5. **Day Phase** — Five minutes of open discussion. Argue, accuse, bluff, deflect. Three "CAW CAW" calls trigger an early vote.
6. **Voting (Eggsecution)** — No talking. Everyone secretly picks a target and cards to throw. Reveal simultaneously — thrown cards are shown **face-down**. Most votes gets eliminated. Winner is determined by the eliminated player's alignment.
7. **Scoring** — Winning team banks thrown cards face-down to score pile. Losing team discards thrown cards. Correct-target consolation returns best card to hand. Kept cards are worth 1 point each at game end.

After the agreed number of rounds, the player with the highest score wins.

### Components

- A 64-card deck (14 per suit + 8 wilds), where every card is both an egg (value) and an animal/flower (role description + QR code)
- Player colour tokens (one per player)
- The Meowfia companion app on a single phone

---

## The App

### What It Does

The Meowfia app is the game master — it handles everything that needs to be secret or calculated, so no human moderator is required. It runs on a single phone passed between players.

The app tracks:

- Player names and seating order
- Secret alignment and role assignments (re-rolled each round)
- Night visit targets and action resolution
- Egg gain/loss deltas per round
- Dawn reports with role-specific information
- Day phase timer and early-vote tracking

The app does **not** track the physical cards in players' hands, the deck composition, individual egg colours, or scoring. Those stay physical and private.

### Pass-and-Play Design

All secret information is shown one player at a time behind a tap-to-reveal gate. The phone is passed clockwise during the night and dawn phases. Each screen has a handoff confirmation to prevent accidental leaks — you tap to see your info, confirm when done, and the screen locks before passing.

### Role System

Every role in the game is a self-contained module in the app. The night resolution engine processes all actions in a defined priority order (lays → steals → investigations → tracking → passive → self-visit), with same-tier conflicts resolved by seat order clockwise from the dealer.

The v1 app ships with 11 roles:

| Role | Alignment | Night Action |
|------|-----------|--------------|
| Pigeon | Farm (buffer) | Visit a player, lay an egg in their nest |
| House Cat | Meowfia (buffer) | Visit a player, steal an egg, learn their role and who they visited |
| Hawk | Farm | Investigate a player — if Meowfia, gain an egg |
| Owl | Farm | Learn which animals visited your target; if none visited, lay an egg |
| Eagle | Farm | Gain eggs equal to the number of visitors your target received |
| Turkey | Farm | Stay home — lay an egg for each player who visits you |
| Falcon | Farm | Lay an egg in the nest of whoever your target visited |
| Mosquito | Farm | Visit a random player, lay an egg |
| Chicken | Farm | Lay 2 eggs, but lose if anyone throws a single egg at you |
| Tit | Farm | Visit a random Meowfia player and lay an egg in their nest |
| Black Swan | Farm | Visit yourself — if you're still a Black Swan, gain an egg |

Pigeon and House Cat are always in the pool as buffer roles. If more players share an alignment than there are unique roles available, the extras become Pigeons or House Cats. Future updates will add more Meowfia cat roles, complex farm animals, a flower event system, and cross-cutting mechanics like winks and death.

### Pool Input

At the start of each round, the dealer scans the revealed cards' QR codes with the app's camera (CameraX + ML Kit). A manual card selector is available as a fallback if scanning isn't practical.

### Architecture

The app targets Android (Kotlin, Jetpack Compose, min SDK 26). Key architectural decisions:

- **Modular roles** — each role implements a common `RoleHandler` interface. Adding a role means registering a new class, never modifying the engine.
- **Resolution order as data** — night action priority lives in a standalone config file with its own version number. The engine reads it; roles don't need to know about each other.
- **Abstracted randomness** — a seeded `RandomProvider` makes every game reproducible for testing.
- **Single coordinator** — a `GameCoordinator` orchestrates all phase transitions and serves as the single source of truth for game state.

---

## Test Harness

The project includes a Python-based Monte Carlo simulation (`meowfia_test_harness.py`) that validates the scoring system's balance across thousands of simulated games. It models six player strategy archetypes (varying aggression and skill) and measures:

- Score distribution and equality (Gini coefficient)
- Skill premium (does better deduction lead to higher scores?)
- Aggression vs. conservative balance
- Catch-up rate (can losing players recover?)
- Strategy viability (are multiple playstyles competitive?)

Run it with:

```bash
python meowfia_test_harness.py                  # default: 2000 games, 6 players, 5 rounds
python meowfia_test_harness.py --all            # full suite with sweeps and sample games
python meowfia_test_harness.py --seed 42        # reproducible run
```

The app also includes an in-app test dashboard that can run simulated games using the actual game engine (same `NightResolver`, `RoleAssigner`, and `RoleHandler` implementations) and produce structured text logs.

---

## Project Files

| File | Description |
|------|-------------|
| `meowfia_rules_v6.md` | Complete official rules (v6) — setup, round flow, value-based scoring, strategy tips |
| `meowfia_rules_v5-1.md` | Historical rules (v5) — includes suit-based scoring mechanics |
| `meowfia_app_outline-1.md` | Full app architecture — data models, role system, night resolution engine, UI screens, state machine, testing infrastructure |
| `meowfia_test_harness.py` | Python scoring simulation — Monte Carlo balance testing with strategy archetypes and statistical analysis |

---

## Status

The game rules (v6) are stable and playtested. The app architecture is designed and documented. Implementation is in progress, with the core engine (role resolution, assignment, dawn reports) as the current priority. The scoring test harness is functional and produces balance reports.

### Roadmap

- **Now:** Core engine implementation (night resolver, role handlers, game coordinator)
- **Next:** Android UI (pass-and-play screens, handoff gates, QR scanning)
- **Then:** Additional Meowfia cat roles, flower event system, expanded farm animals
- **Later:** Wink/death mechanics, advanced roles (role-swapping, copying, confusion)
