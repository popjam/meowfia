#!/usr/bin/env python3
"""
╔══════════════════════════════════════════════════════════════════════╗
║              MEOWFIA v5 — Scoring System Test Harness               ║
║                                                                      ║
║  Extensive Monte Carlo simulation of the Meowfia v5 scoring rules.   ║
║  Run this to validate game balance across thousands of simulated     ║
║  games with different player strategies and configurations.          ║
║                                                                      ║
║  Usage:                                                              ║
║    python meowfia_test_harness.py                    (default run)   ║
║    python meowfia_test_harness.py --games 5000       (more games)    ║
║    python meowfia_test_harness.py --players 8        (8 players)     ║
║    python meowfia_test_harness.py --rounds 3         (3 rounds)      ║
║    python meowfia_test_harness.py --seed 42          (reproducible)  ║
║    python meowfia_test_harness.py --verbose          (sample games)  ║
║    python meowfia_test_harness.py --all              (run everything)║
║                                                                      ║
║  No dependencies required — pure Python 3.8+.                        ║
╚══════════════════════════════════════════════════════════════════════╝

MEOWFIA v5 SCORING RULES (as implemented):
───────────────────────────────────────────
  Winning team: thrown cards → score pile (face value), then suit bonus.
  Losing team:  thrown cards → discard pile, then suit penalty.
                EXCEPT: if you targeted the opposite team, bank your
                single best thrown card (no suit effects) first.

  ♥ Hearts    Win: no bonus              Loss: no penalty
  ♦ Diamonds  Win: lock in best hand     Loss: move best score pile
              card → score pile          card → hand
  ♣ Clubs     Win: no bonus (2 votes)    Loss: card → target's score pile
  ♠ Spades    Win: steal 1 from          Loss: give 1 from your hand
              target's hand              to target

  Dead players throw (resolved for scoring) but don't count as votes.
  Kept cards = 1 point each at game end.
  Final score = score pile face values + 1 per hand card.
"""

import random
import math
import argparse
import sys
import time
from dataclasses import dataclass, field
from typing import List, Dict, Tuple, Optional
from collections import defaultdict, Counter
from enum import Enum

# ═══════════════════════════════════════════════════════════════
#  CARD & DECK
# ═══════════════════════════════════════════════════════════════

SUITS = ['hearts', 'diamonds', 'clubs', 'spades']
SUIT_SYM = {'hearts': '♥', 'diamonds': '♦', 'clubs': '♣', 'spades': '♠', 'wild': '★'}


@dataclass
class Card:
    suit: str
    value: int

    @property
    def display(self):
        if self.suit == 'wild':
            return 'Wild'
        return f"{self.value}{SUIT_SYM[self.suit]}"

    def __repr__(self):
        return self.display


def make_deck() -> List[Card]:
    """Standard 64-card Meowfia deck: 14 per suit + 8 wilds."""
    deck = []
    for suit in SUITS:
        for v in range(1, 14):
            deck.append(Card(suit, v))
        deck.append(Card(suit, random.randint(1, 13)))  # 1 duplicate
    for _ in range(8):
        deck.append(Card('wild', 0))
    return deck


# ═══════════════════════════════════════════════════════════════
#  PLAYER STRATEGIES
# ═══════════════════════════════════════════════════════════════

class Strategy:
    """
    Defines how a simulated player makes decisions.

    aggression: 0.0 (throw as little as possible) to 1.0 (throw everything)
    skill:      0.0 (random deduction) to 1.0 (perfect target selection)
    """

    def __init__(self, name: str, aggression: float, skill: float):
        self.name = name
        self.aggression = aggression
        self.skill = skill

    def choose_target(self, my_idx: int, alignments: List[str],
                      meowfia_indices: List[int], n_players: int) -> int:
        """Pick a player to throw at. Skill determines accuracy."""
        others = [i for i in range(n_players) if i != my_idx]
        is_meowfia = alignments[my_idx] == 'meowfia'

        if is_meowfia:
            # Meowfia wants to eliminate Farm
            farm = [i for i in others if alignments[i] == 'farm']
            if farm and random.random() < self.skill * 0.7:
                return random.choice(farm)
            return random.choice(others)
        else:
            # Farm wants to eliminate Meowfia
            if meowfia_indices and random.random() < self.skill:
                valid = [i for i in meowfia_indices if i != my_idx]
                if valid:
                    return random.choice(valid)
            return random.choice(others)

    def get_confidence(self, is_correct: bool) -> float:
        """How confident this player feels (affects throw count and suit choice)."""
        if is_correct:
            return max(0.1, min(0.95, 0.3 + self.skill * 0.6 + random.gauss(0, 0.1)))
        return max(0.1, min(0.95, 0.3 + (1 - self.skill) * 0.3 + random.gauss(0, 0.15)))

    def choose_throw(self, hand: List[Card], confidence: float) -> Tuple[List[Card], List[Card]]:
        """Decide which cards to throw and which to keep. Returns (thrown, kept)."""
        if not hand:
            return [], []

        willingness = self.aggression * 0.5 + confidence * 0.5
        sorted_hand = sorted(hand, key=lambda c: self._throw_priority(c, confidence))
        n_throw = max(1, min(len(hand), int(willingness * len(hand) + random.gauss(0, 0.5))))
        return sorted_hand[:n_throw], sorted_hand[n_throw:]

    def _throw_priority(self, card: Card, confidence: float) -> float:
        """Lower value = throw first. Each suit has different risk profile."""
        b = card.value
        if card.suit == 'wild':
            return -100  # Always throw wilds first

        if card.suit == 'hearts':
            # Safe — throw readily
            return b * 0.4

        elif card.suit == 'diamonds':
            # Riches & rags — high confidence needed for high diamonds
            if confidence > 0.65:
                return b * 0.55
            elif confidence > 0.4:
                return b * 1.0
            else:
                return b * 1.6

        elif card.suit == 'clubs':
            # Terrifying — club goes to target's score on loss
            if confidence > 0.65:
                return b * 0.5 - 5  # 2-vote bonus makes them attractive when confident
            elif confidence > 0.4:
                return b * 1.3
            else:
                return b * 2.5  # High clubs at low confidence are radioactive

        elif card.suit == 'spades':
            # Political — steal/give with target
            if confidence > 0.5:
                return b * 0.75
            else:
                return b * 1.2

        return b


# Six standard strategy archetypes
ARCHETYPES = [
    Strategy("Aggressive-Skilled",    aggression=0.85, skill=0.80),
    Strategy("Aggressive-Unskilled",  aggression=0.85, skill=0.20),
    Strategy("Conservative-Skilled",  aggression=0.20, skill=0.80),
    Strategy("Conservative-Unskilled",aggression=0.20, skill=0.20),
    Strategy("Balanced-Skilled",      aggression=0.50, skill=0.70),
    Strategy("Balanced-Random",       aggression=0.50, skill=0.35),
]


# ═══════════════════════════════════════════════════════════════
#  PLAYER STATE
# ═══════════════════════════════════════════════════════════════

@dataclass
class PlayerState:
    idx: int
    strategy: Strategy
    hand: List[Card] = field(default_factory=list)
    score_pile: List[Card] = field(default_factory=list)
    alignment: str = 'farm'
    is_dead: bool = False

    def total_score(self) -> int:
        return sum(c.value for c in self.score_pile) + len(self.hand)

    def score_pile_value(self) -> int:
        return sum(c.value for c in self.score_pile)

    def move_best_score_to_hand(self):
        """Diamond loss: move highest-value score pile card back to hand."""
        if not self.score_pile:
            return
        self.score_pile.sort(key=lambda c: c.value, reverse=True)
        self.hand.append(self.score_pile.pop(0))

    def lock_best_hand_to_score(self):
        """Diamond win: move highest-value hand card into score pile."""
        if not self.hand:
            return
        self.hand.sort(key=lambda c: c.value, reverse=True)
        self.score_pile.append(self.hand.pop(0))


# ═══════════════════════════════════════════════════════════════
#  GAME SIMULATION ENGINE
# ═══════════════════════════════════════════════════════════════

@dataclass
class RoundLog:
    """Detailed log of a single round for verbose output."""
    round_num: int = 0
    alignments: List[str] = field(default_factory=list)
    roles: List[str] = field(default_factory=list)
    targets: Dict[int, int] = field(default_factory=dict)
    thrown: Dict[int, List[Card]] = field(default_factory=dict)
    kept: Dict[int, List[Card]] = field(default_factory=dict)
    votes: Dict[int, int] = field(default_factory=dict)
    eliminated: int = -1
    winning_team: str = ''
    pre_scores: List[int] = field(default_factory=list)
    post_scores: List[int] = field(default_factory=list)
    events: List[str] = field(default_factory=list)


@dataclass
class GameResult:
    final_scores: List[int]
    strategies: List[str]
    round_deltas: List[List[int]]
    correct_throws: List[int]
    total_throws: List[int]
    correct_targets: List[int]
    total_losses: List[int]
    rounds: List[RoundLog]
    suit_throws: Dict[str, int]
    suit_wins: Dict[str, int]
    suit_losses: Dict[str, int]
    club_gifted_values: List[int]
    spade_steals: int
    spade_gives: int
    diamond_locks: int
    diamond_demotes: int


def simulate_game(n_players: int, n_rounds: int, strategies: List[Strategy],
                  verbose: bool = False) -> GameResult:
    """Simulate one complete Meowfia game. Returns detailed results."""

    deck = make_deck()
    random.shuffle(deck)
    discard: List[Card] = []

    players = [PlayerState(idx=i, strategy=strategies[i]) for i in range(n_players)]
    for p in players:
        for _ in range(3):
            if deck:
                p.hand.append(deck.pop())

    round_deltas = [[] for _ in range(n_players)]
    correct_throws = [0] * n_players
    total_throws = [0] * n_players
    correct_targets = [0] * n_players
    total_losses = [0] * n_players
    rounds_log: List[RoundLog] = []

    # Suit tracking
    suit_throws = defaultdict(int)
    suit_wins = defaultdict(int)
    suit_losses = defaultdict(int)
    club_gifted_values: List[int] = []
    spade_steals = 0
    spade_gives = 0
    diamond_locks = 0
    diamond_demotes = 0

    for rnd in range(1, n_rounds + 1):
        log = RoundLog(round_num=rnd)

        # Pool phase: remove 3 cards
        for _ in range(3):
            if deck:
                deck.pop()

        # Setup: draw 2, assign alignments
        for p in players:
            for _ in range(2):
                if deck:
                    p.hand.append(deck.pop())
            p.is_dead = random.random() < 0.08
            p.alignment = 'meowfia' if random.random() < 1 / 3 else 'farm'

        alignments = [p.alignment for p in players]
        meowfia_idx = [i for i, a in enumerate(alignments) if a == 'meowfia']
        log.alignments = list(alignments)

        # Night: nest eggs + card draw
        for p in players:
            eggs = random.randint(1, 3) if p.alignment == 'meowfia' else random.randint(0, 3)
            for _ in range(min(eggs, 5)):
                if deck:
                    p.hand.append(deck.pop())

        log.pre_scores = [p.total_score() for p in players]

        # ── Voting ──
        targets: Dict[int, int] = {}
        thrown: Dict[int, List[Card]] = {}
        kept: Dict[int, List[Card]] = {}

        for p in players:
            if not p.hand:
                continue

            t = p.strategy.choose_target(p.idx, alignments, meowfia_idx, n_players)
            targets[p.idx] = t

            helps = (p.alignment == 'farm' and alignments[t] == 'meowfia') or \
                    (p.alignment == 'meowfia' and alignments[t] == 'farm')
            conf = p.strategy.get_confidence(helps)

            th, ke = p.strategy.choose_throw(p.hand, conf)
            thrown[p.idx] = th
            kept[p.idx] = ke
            p.hand = ke

            # Track suits thrown
            for c in th:
                suit_throws[c.suit] += 1

        log.targets = dict(targets)
        log.thrown = {k: list(v) for k, v in thrown.items()}
        log.kept = {k: list(v) for k, v in kept.items()}

        # Count votes (dead players don't count)
        votes: Dict[int, int] = defaultdict(int)
        for pi, ti in targets.items():
            if players[pi].is_dead:
                continue
            for c in thrown.get(pi, []):
                if c.suit == 'clubs':
                    votes[ti] += 2
                else:
                    votes[ti] += 1

        log.votes = dict(votes)

        if not votes:
            for p in players:
                round_deltas[p.idx].append(0)
            rounds_log.append(log)
            continue

        # Determine eliminated player
        mx = max(votes.values())
        tied = [i for i, v in votes.items() if v == mx]
        if len(tied) > 1:
            farm_tied = [i for i in tied if alignments[i] == 'farm']
            eliminated = random.choice(farm_tied) if farm_tied else random.choice(tied)
        else:
            eliminated = tied[0]

        winning_team = 'farm' if alignments[eliminated] == 'meowfia' else 'meowfia'
        log.eliminated = eliminated
        log.winning_team = winning_team

        # Track stats
        for p in players:
            if p.idx in thrown:
                total_throws[p.idx] += 1
                if p.alignment == winning_team:
                    correct_throws[p.idx] += 1

        # ── Resolve thrown cards ──
        for p in players:
            if p.idx not in thrown:
                continue

            cards = thrown[p.idx]
            is_winner = p.alignment == winning_team
            ti = targets.get(p.idx)
            target_p = players[ti] if ti is not None else None

            if is_winner:
                for c in cards:
                    # Bank the card
                    p.score_pile.append(c)
                    suit_wins[c.suit] += 1

                    # Suit win effects
                    if c.suit == 'diamonds':
                        p.lock_best_hand_to_score()
                        diamond_locks += 1
                        log.events.append(f"  {p.strategy.name} (seat {p.idx}) won ♦{c.value} → locked in extra card")

                    elif c.suit == 'spades':
                        if target_p and target_p.hand:
                            stolen = target_p.hand.pop(random.randint(0, len(target_p.hand) - 1))
                            p.hand.append(stolen)
                            spade_steals += 1
                            log.events.append(f"  {p.strategy.name} (seat {p.idx}) won ♠{c.value} → stole {stolen.display} from seat {ti}")

            else:
                # LOSING TEAM
                targeted_opp = (ti is not None and p.alignment != alignments[ti])

                # Correct-target consolation: bank best 1
                remaining = list(cards)
                if targeted_opp and remaining:
                    remaining.sort(key=lambda c: c.value, reverse=True)
                    banked = remaining.pop(0)
                    p.score_pile.append(banked)
                    correct_targets[p.idx] += 1
                    log.events.append(f"  {p.strategy.name} (seat {p.idx}) lost but targeted opposite → banked {banked.display}")

                total_losses[p.idx] += 1

                # Discard rest with suit penalties
                for c in remaining:
                    suit_losses[c.suit] += 1

                    if c.suit == 'diamonds':
                        # Move best score pile card → hand
                        discard.append(c)
                        p.move_best_score_to_hand()
                        diamond_demotes += 1
                        log.events.append(f"  {p.strategy.name} (seat {p.idx}) lost ♦{c.value} → demoted score card to hand")

                    elif c.suit == 'clubs':
                        # Card goes to TARGET's score pile
                        if target_p:
                            target_p.score_pile.append(c)
                            club_gifted_values.append(c.value)
                            log.events.append(f"  {p.strategy.name} (seat {p.idx}) lost ♣{c.value} → gifted to seat {ti}!")
                        else:
                            discard.append(c)

                    elif c.suit == 'spades':
                        # Give 1 from hand to target
                        discard.append(c)
                        if p.hand and target_p:
                            given = p.hand.pop(random.randint(0, len(p.hand) - 1))
                            target_p.hand.append(given)
                            spade_gives += 1
                            log.events.append(f"  {p.strategy.name} (seat {p.idx}) lost ♠{c.value} → gave {given.display} to seat {ti}")

                    else:
                        # Hearts / wilds — just discard
                        discard.append(c)

        # Record deltas
        log.post_scores = [p.total_score() for p in players]
        for i in range(n_players):
            round_deltas[i].append(log.post_scores[i] - log.pre_scores[i])

        rounds_log.append(log)

        # Reshuffle if low
        if len(deck) < n_players * 3:
            random.shuffle(discard)
            deck.extend(discard)
            discard.clear()

    return GameResult(
        final_scores=[p.total_score() for p in players],
        strategies=[s.name for s in strategies],
        round_deltas=round_deltas,
        correct_throws=correct_throws,
        total_throws=total_throws,
        correct_targets=correct_targets,
        total_losses=total_losses,
        rounds=rounds_log,
        suit_throws=dict(suit_throws),
        suit_wins=dict(suit_wins),
        suit_losses=dict(suit_losses),
        club_gifted_values=club_gifted_values,
        spade_steals=spade_steals,
        spade_gives=spade_gives,
        diamond_locks=diamond_locks,
        diamond_demotes=diamond_demotes,
    )


# ═══════════════════════════════════════════════════════════════
#  ANALYSIS & METRICS
# ═══════════════════════════════════════════════════════════════

def gini(values: List[float]) -> float:
    """Gini coefficient: 0 = perfect equality, 1 = max inequality."""
    values = sorted(max(0, v) for v in values)
    n = len(values)
    total = sum(values)
    if total == 0:
        return 0
    return sum((2 * (i + 1) - n - 1) * v for i, v in enumerate(values)) / (n * total)


def pearson_correlation(xs: List[float], ys: List[float]) -> float:
    """Pearson correlation coefficient between two lists."""
    n = len(xs)
    if n < 2:
        return 0
    mx, my = sum(xs) / n, sum(ys) / n
    cov = sum((x - mx) * (y - my) for x, y in zip(xs, ys))
    vx = sum((x - mx) ** 2 for x in xs)
    vy = sum((y - my) ** 2 for y in ys)
    if vx == 0 or vy == 0:
        return 0
    return cov / math.sqrt(vx * vy)


def run_batch(n_games: int, n_players: int, n_rounds: int,
              verbose_samples: int = 0) -> Dict:
    """Run a batch of games and compute aggregate statistics."""

    all_scores = []
    all_ginis = []
    skill_premiums = []
    aggro_cons_gaps = []
    catch_up_rates = []
    deduction_pairs = []
    strategy_scores = defaultdict(list)
    correct_target_rates = []

    # Suit-level tracking
    total_suit_throws = defaultdict(int)
    total_suit_wins = defaultdict(int)
    total_suit_losses = defaultdict(int)
    all_club_gifts = []
    total_spade_steals = 0
    total_spade_gives = 0
    total_diamond_locks = 0
    total_diamond_demotes = 0

    sample_logs = []

    for game_num in range(n_games):
        strats = [ARCHETYPES[i % len(ARCHETYPES)] for i in range(n_players)]
        random.shuffle(strats)

        do_verbose = game_num < verbose_samples
        result = simulate_game(n_players, n_rounds, strats, verbose=do_verbose)

        if do_verbose:
            sample_logs.append(result)

        scores = result.final_scores
        all_scores.append(scores)
        all_ginis.append(gini(scores))

        for i, sn in enumerate(result.strategies):
            strategy_scores[sn].append(scores[i])

        # Skill premium
        skilled = [scores[i] for i, st in enumerate(strats) if st.skill >= 0.7]
        unskilled = [scores[i] for i, st in enumerate(strats) if st.skill <= 0.3]
        if skilled and unskilled:
            skill_premiums.append(sum(skilled) / len(skilled) - sum(unskilled) / len(unskilled))

        # Aggro vs conservative
        aggro = [scores[i] for i, st in enumerate(strats) if st.aggression >= 0.7]
        cons = [scores[i] for i, st in enumerate(strats) if st.aggression <= 0.3]
        if aggro and cons:
            aggro_cons_gaps.append(sum(aggro) / len(aggro) - sum(cons) / len(cons))

        # Catch-up rate
        rd = result.round_deltas
        if n_rounds >= 3:
            mid = [sum(d[:n_rounds // 2]) for d in rd]
            mr = sorted(range(n_players), key=lambda i: mid[i], reverse=True)
            fr = sorted(range(n_players), key=lambda i: scores[i], reverse=True)
            rank_diff = sum(abs(mr.index(i) - fr.index(i)) for i in range(n_players))
            catch_up_rates.append(rank_diff / max(1, n_players * (n_players - 1) / 2))

        # Deduction correlation
        for i in range(n_players):
            if result.total_throws[i] > 0:
                acc = result.correct_throws[i] / result.total_throws[i]
                deduction_pairs.append((acc, scores[i]))

        # Correct target rate
        for i in range(n_players):
            if result.total_losses[i] > 0:
                correct_target_rates.append(
                    result.correct_targets[i] / result.total_losses[i]
                )

        # Suit tracking
        for suit, count in result.suit_throws.items():
            total_suit_throws[suit] += count
        for suit, count in result.suit_wins.items():
            total_suit_wins[suit] += count
        for suit, count in result.suit_losses.items():
            total_suit_losses[suit] += count
        all_club_gifts.extend(result.club_gifted_values)
        total_spade_steals += result.spade_steals
        total_spade_gives += result.spade_gives
        total_diamond_locks += result.diamond_locks
        total_diamond_demotes += result.diamond_demotes

    # ── Aggregate metrics ──
    flat_scores = [s for game in all_scores for s in game]

    # Deduction correlation
    if deduction_pairs:
        deduction_corr = pearson_correlation(
            [d[0] for d in deduction_pairs],
            [d[1] for d in deduction_pairs]
        )
    else:
        deduction_corr = 0

    # Strategy stats
    strat_means = {k: sum(v) / len(v) for k, v in strategy_scores.items() if v}
    strat_stds = {}
    for k, v in strategy_scores.items():
        if len(v) > 1:
            m = sum(v) / len(v)
            strat_stds[k] = math.sqrt(sum((x - m) ** 2 for x in v) / (len(v) - 1))

    if strat_means:
        best = max(strat_means.values())
        threshold = best * 0.8 if best > 0 else 0
        viable = sum(1 for v in strat_means.values() if v >= threshold)
        viability = viable / len(strat_means)
        spread = max(strat_means.values()) - min(strat_means.values())
    else:
        viability, spread = 0, 0

    neg_count = sum(1 for s in flat_scores if s < 0)

    return {
        'n_games': n_games,
        'n_players': n_players,
        'n_rounds': n_rounds,
        'avg_score': sum(flat_scores) / len(flat_scores) if flat_scores else 0,
        'median_score': sorted(flat_scores)[len(flat_scores) // 2] if flat_scores else 0,
        'min_score': min(flat_scores) if flat_scores else 0,
        'max_score': max(flat_scores) if flat_scores else 0,
        'score_std': math.sqrt(sum((s - sum(flat_scores)/len(flat_scores))**2 for s in flat_scores) / len(flat_scores)) if flat_scores else 0,
        'neg_pct': neg_count / len(flat_scores) * 100 if flat_scores else 0,
        'avg_gini': sum(all_ginis) / len(all_ginis) if all_ginis else 0,
        'skill_premium': sum(skill_premiums) / len(skill_premiums) if skill_premiums else 0,
        'aggro_cons_gap': sum(aggro_cons_gaps) / len(aggro_cons_gaps) if aggro_cons_gaps else 0,
        'catch_up_rate': sum(catch_up_rates) / len(catch_up_rates) if catch_up_rates else 0,
        'deduction_corr': deduction_corr,
        'viability': viability,
        'spread': spread,
        'strat_means': strat_means,
        'strat_stds': strat_stds,
        'avg_correct_target_rate': sum(correct_target_rates) / len(correct_target_rates) if correct_target_rates else 0,
        'suit_throws': dict(total_suit_throws),
        'suit_wins': dict(total_suit_wins),
        'suit_losses': dict(total_suit_losses),
        'club_gifts_total': len(all_club_gifts),
        'club_gifts_avg_value': sum(all_club_gifts) / len(all_club_gifts) if all_club_gifts else 0,
        'spade_steals': total_spade_steals,
        'spade_gives': total_spade_gives,
        'diamond_locks': total_diamond_locks,
        'diamond_demotes': total_diamond_demotes,
        'sample_logs': sample_logs,
    }


# ═══════════════════════════════════════════════════════════════
#  REPORTING
# ═══════════════════════════════════════════════════════════════

def print_header(title: str, width: int = 90):
    print("═" * width)
    print(f"  {title}")
    print("═" * width)


def print_section(title: str, width: int = 90):
    print()
    print(f"  ── {title} ──")
    print()


def print_results(r: Dict):
    """Print comprehensive analysis of batch results."""

    print()
    print_header(f"MEOWFIA v5 SCORING ANALYSIS — {r['n_games']} Games")
    print(f"  Config: {r['n_players']} players × {r['n_rounds']} rounds per game")
    print()

    # ── Score Distribution ──
    print_section("SCORE DISTRIBUTION")
    print(f"    Average:   {r['avg_score']:.1f}")
    print(f"    Median:    {r['median_score']:.0f}")
    print(f"    Std Dev:   {r['score_std']:.1f}")
    print(f"    Range:     {r['min_score']:.0f} to {r['max_score']:.0f}")
    print(f"    Negative:  {r['neg_pct']:.1f}% of player-games")

    # ── Balance Metrics ──
    print_section("BALANCE METRICS")
    metrics = [
        ("Score Equality (Gini)",        f"{r['avg_gini']:.3f}",       "0=equal, 1=unequal. Target: <0.35"),
        ("Skill Premium",                f"+{r['skill_premium']:.1f}", "Skilled vs unskilled avg gap. Target: +15 to +25"),
        ("Aggro-Conservative Gap",       f"+{r['aggro_cons_gap']:.1f}","Aggressive vs conservative. Target: <+12"),
        ("Catch-up Rate",                f"{r['catch_up_rate']:.3f}",  "Rank mobility mid→end. Target: >0.35"),
        ("Deduction Correlation",        f"{r['deduction_corr']:.3f}", "Correct throws → score. Target: >0.55"),
        ("Strategy Viability",           f"{r['viability']:.0%}",      "Archetypes within 80% of best. Target: ≥50%"),
        ("Strategy Spread",              f"{r['spread']:.1f}",         "Best minus worst archetype. Target: <35"),
        ("Correct-Target Rate",          f"{r['avg_correct_target_rate']:.1%}", "How often losers targeted opposite team"),
    ]
    for name, value, desc in metrics:
        print(f"    {name:<30} {value:>8}   ({desc})")

    # ── Strategy Breakdown ──
    print_section("STRATEGY PERFORMANCE")
    strat_order = [
        "Aggressive-Skilled", "Aggressive-Unskilled",
        "Balanced-Skilled", "Balanced-Random",
        "Conservative-Skilled", "Conservative-Unskilled",
    ]
    print(f"    {'Archetype':<28} {'Avg Score':>9} {'Std Dev':>8} {'vs Best':>8}")
    print(f"    {'─' * 56}")
    best_val = max(r['strat_means'].values()) if r['strat_means'] else 0
    for sn in strat_order:
        avg = r['strat_means'].get(sn, 0)
        std = r['strat_stds'].get(sn, 0)
        diff = avg - best_val
        marker = " ★" if avg == best_val else ""
        print(f"    {sn:<28} {avg:>9.1f} {std:>8.1f} {diff:>+8.1f}{marker}")

    # ── Suit Economics ──
    print_section("SUIT ECONOMICS")
    total_thrown = sum(r['suit_throws'].values())
    for suit in ['hearts', 'diamonds', 'clubs', 'spades', 'wild']:
        thrown = r['suit_throws'].get(suit, 0)
        wins = r['suit_wins'].get(suit, 0)
        losses = r['suit_losses'].get(suit, 0)
        pct = thrown / total_thrown * 100 if total_thrown else 0
        win_rate = wins / (wins + losses) * 100 if (wins + losses) > 0 else 0
        print(f"    {SUIT_SYM.get(suit, '?')} {suit:<10}  Thrown: {thrown:>7} ({pct:>5.1f}%)  Win rate: {win_rate:>5.1f}%")

    print()
    games = r['n_games']
    print(f"    ♦ Diamond locks (win):    {r['diamond_locks']:>6}  ({r['diamond_locks']/games:.1f}/game)")
    print(f"    ♦ Diamond demotes (loss): {r['diamond_demotes']:>6}  ({r['diamond_demotes']/games:.1f}/game)")
    print(f"    ♣ Clubs gifted to target: {r['club_gifts_total']:>6}  ({r['club_gifts_total']/games:.1f}/game)  avg value: {r['club_gifts_avg_value']:.1f}")
    print(f"    ♠ Spade steals (win):     {r['spade_steals']:>6}  ({r['spade_steals']/games:.1f}/game)")
    print(f"    ♠ Spade gives (loss):     {r['spade_gives']:>6}  ({r['spade_gives']/games:.1f}/game)")

    # ── Verdict ──
    print_section("HEALTH CHECK")
    issues = []
    passes = []

    if r['avg_gini'] > 0.35:
        issues.append("⚠ High Gini — runaway leader risk")
    else:
        passes.append("✓ Score equality healthy")

    if r['skill_premium'] < 10:
        issues.append("⚠ Low skill premium — skill doesn't matter enough")
    elif r['skill_premium'] > 30:
        issues.append("⚠ Very high skill premium — new players may struggle")
    else:
        passes.append("✓ Skill premium in healthy range")

    if abs(r['aggro_cons_gap']) > 15:
        issues.append("⚠ Large aggro-conservative gap — one playstyle dominates")
    else:
        passes.append("✓ Aggro-conservative balance healthy")

    if r['catch_up_rate'] < 0.3:
        issues.append("⚠ Low catch-up rate — hard to recover from early losses")
    else:
        passes.append("✓ Catch-up potential adequate")

    if r['deduction_corr'] < 0.5:
        issues.append("⚠ Low deduction reward — correct reads don't pay enough")
    else:
        passes.append("✓ Deduction rewarded")

    if r['viability'] < 0.4:
        issues.append("⚠ Low strategy viability — too few playstyles competitive")
    else:
        passes.append("✓ Multiple playstyles viable")

    if r['neg_pct'] > 5:
        issues.append(f"⚠ {r['neg_pct']:.1f}% negative scores — too harsh")
    else:
        passes.append("✓ Negative scores rare/absent")

    for p in passes:
        print(f"    {p}")
    for i in issues:
        print(f"    {i}")

    if not issues:
        print()
        print("    ★ All metrics in healthy range — scoring system is well-balanced.")

    print()


def print_verbose_game(result: GameResult, game_num: int, names: List[str]):
    """Print a detailed play-by-play of a single game."""
    n = len(result.final_scores)

    print()
    print_header(f"SAMPLE GAME #{game_num + 1}", 80)
    print(f"  Players: {', '.join(f'{names[i]} ({result.strategies[i]})' for i in range(n))}")
    print()

    for rnd_log in result.rounds:
        r = rnd_log
        print(f"  ──── Round {r.round_num} ────")

        # Alignments
        align_str = "  ".join(
            f"{names[i]}={'M' if r.alignments[i] == 'meowfia' else 'F'}"
            for i in range(n)
        )
        print(f"  Teams: {align_str}")

        # Throws
        for i in range(n):
            if i in r.thrown:
                ti = r.targets.get(i, -1)
                thrown_str = ', '.join(c.display for c in r.thrown[i])
                kept_str = ', '.join(c.display for c in r.kept.get(i, []))
                print(f"  {names[i]:>10} → {names[ti]}: threw [{thrown_str}]  kept [{kept_str}]")

        # Votes & elimination
        if r.eliminated >= 0:
            vote_str = ', '.join(f"{names[k]}:{v}" for k, v in sorted(r.votes.items(), key=lambda x: -x[1]))
            print(f"  Votes: {vote_str}")
            team = r.alignments[r.eliminated]
            print(f"  Eliminated: {names[r.eliminated]} ({'MEOWFIA' if team == 'meowfia' else 'FARM'}) → {r.winning_team.upper()} wins")

        # Events
        for e in r.events:
            print(e)

        # Scores
        score_str = "  ".join(f"{names[i]}:{r.post_scores[i]}" for i in range(n))
        print(f"  Scores: {score_str}")
        print()

    print(f"  FINAL: {', '.join(f'{names[i]}: {result.final_scores[i]}' for i in range(n))}")
    winner = max(range(n), key=lambda i: result.final_scores[i])
    print(f"  WINNER: {names[winner]} ({result.strategies[winner]}) with {result.final_scores[winner]} points")
    print()


# ═══════════════════════════════════════════════════════════════
#  PLAYER COUNT SWEEP
# ═══════════════════════════════════════════════════════════════

def run_player_sweep(n_games: int, n_rounds: int):
    """Test across different player counts."""
    print_header("PLAYER COUNT SWEEP")
    print(f"  {n_games} games per configuration, {n_rounds} rounds each")
    print()

    print(f"  {'Players':>7} {'Avg':>6} {'Gini':>6} {'Skl+':>6} {'A-C':>6} {'Cat':>5} {'Ded':>5} {'Via':>4}")
    print(f"  {'─' * 52}")

    for np in [4, 5, 6, 7, 8]:
        r = run_batch(n_games, np, n_rounds)
        print(f"  {np:>7} {r['avg_score']:>6.1f} {r['avg_gini']:>6.3f} "
              f"{r['skill_premium']:>+6.1f} {r['aggro_cons_gap']:>+6.1f} "
              f"{r['catch_up_rate']:>5.3f} {r['deduction_corr']:>5.3f} "
              f"{r['viability']:>3.0%}")

    print()


# ═══════════════════════════════════════════════════════════════
#  ROUND COUNT SWEEP
# ═══════════════════════════════════════════════════════════════

def run_round_sweep(n_games: int, n_players: int):
    """Test across different round counts."""
    print_header("ROUND COUNT SWEEP")
    print(f"  {n_games} games per configuration, {n_players} players each")
    print()

    print(f"  {'Rounds':>6} {'Avg':>6} {'Gini':>6} {'Skl+':>6} {'A-C':>6} {'Cat':>5} {'Ded':>5} {'Via':>4}")
    print(f"  {'─' * 50}")

    for nr in [3, 4, 5, 6, 7, 8]:
        r = run_batch(n_games, n_players, nr)
        print(f"  {nr:>6} {r['avg_score']:>6.1f} {r['avg_gini']:>6.3f} "
              f"{r['skill_premium']:>+6.1f} {r['aggro_cons_gap']:>+6.1f} "
              f"{r['catch_up_rate']:>5.3f} {r['deduction_corr']:>5.3f} "
              f"{r['viability']:>3.0%}")

    print()


# ═══════════════════════════════════════════════════════════════
#  MAIN
# ═══════════════════════════════════════════════════════════════

PLAYER_NAMES = ["Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Hank"]


def main():
    parser = argparse.ArgumentParser(
        description="MEOWFIA v5 Scoring System Test Harness",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python meowfia_test_harness.py                   # Standard analysis
  python meowfia_test_harness.py --games 5000      # More games for precision
  python meowfia_test_harness.py --verbose          # Show 3 sample games
  python meowfia_test_harness.py --all              # Full suite (slow)
  python meowfia_test_harness.py --seed 42          # Reproducible run
        """
    )
    parser.add_argument('--games', type=int, default=2000, help='Number of games to simulate (default: 2000)')
    parser.add_argument('--players', type=int, default=6, help='Number of players (default: 6, range: 4-8)')
    parser.add_argument('--rounds', type=int, default=5, help='Rounds per game (default: 5)')
    parser.add_argument('--seed', type=int, default=None, help='Random seed for reproducibility')
    parser.add_argument('--verbose', action='store_true', help='Print 3 detailed sample games')
    parser.add_argument('--samples', type=int, default=3, help='Number of verbose sample games (default: 3)')
    parser.add_argument('--sweep-players', action='store_true', help='Run player count sweep (4-8)')
    parser.add_argument('--sweep-rounds', action='store_true', help='Run round count sweep (3-8)')
    parser.add_argument('--all', action='store_true', help='Run everything (main + sweeps + verbose)')

    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)
        print(f"Random seed: {args.seed}")
    else:
        seed = int(time.time())
        random.seed(seed)
        print(f"Random seed: {seed}")

    if args.all:
        args.verbose = True
        args.sweep_players = True
        args.sweep_rounds = True

    n_verbose = args.samples if args.verbose else 0

    print()
    print("Running main simulation...")
    start = time.time()
    results = run_batch(args.games, args.players, args.rounds, verbose_samples=n_verbose)
    elapsed = time.time() - start
    print(f"Completed in {elapsed:.1f}s")

    print_results(results)

    # Verbose sample games
    if args.verbose and results['sample_logs']:
        for i, game_result in enumerate(results['sample_logs'][:args.samples]):
            names = PLAYER_NAMES[:args.players]
            print_verbose_game(game_result, i, names)

    # Player count sweep
    if args.sweep_players:
        print()
        sweep_games = max(500, args.games // 3)
        print(f"Running player count sweep ({sweep_games} games each)...")
        run_player_sweep(sweep_games, args.rounds)

    # Round count sweep
    if args.sweep_rounds:
        print()
        sweep_games = max(500, args.games // 3)
        print(f"Running round count sweep ({sweep_games} games each)...")
        run_round_sweep(sweep_games, args.players)

    print()
    print("Done.")


if __name__ == '__main__':
    main()
