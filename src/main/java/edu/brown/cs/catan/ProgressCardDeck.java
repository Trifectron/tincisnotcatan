package edu.brown.cs.catan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * A shuffled draw pile of the progress cards for one improvement track. A game
 * keeps one deck per track (Trade, Politics, Science); a player draws from the
 * deck matching the discipline rolled.
 *
 * Each card is held in 2 copies by default, matching the rough multiplicity
 * rule of the official Cities & Knights decks (most cards appear twice).
 * Cards that intentionally appear a different number of times in the real
 * game override {@link #deckMultiplicity(ProgressCardType)}.
 *
 */
public class ProgressCardDeck {

  private final Deque<ProgressCardType> _cards;

  /**
   * Builds and shuffles the deck of progress cards for the given track.
   *
   * @param track
   *          The improvement track this deck belongs to.
   */
  public ProgressCardDeck(CityImprovement track) {
    List<ProgressCardType> cards = new ArrayList<>();
    for (ProgressCardType card : ProgressCardType.values()) {
      if (card.getDeck() == track) {
        int copies = deckMultiplicity(card);
        for (int i = 0; i < copies; i++) {
          cards.add(card);
        }
      }
    }
    Collections.shuffle(cards);
    _cards = new ArrayDeque<>(cards);
  }

  /**
   * How many copies of this card the deck should hold, per the official
   * Cities & Knights multiplicity rule. Defaults to 2 for every card; cards
   * that the printed rules ship with a different count override here. The
   * real deck has 3 copies of Spy and 2 of every other card.
   */
  private static int deckMultiplicity(ProgressCardType card) {
    Map<ProgressCardType, Integer> overrides = ImmutableMap.of(
        ProgressCardType.SPY, 3);
    Integer special = overrides.get(card);
    return special != null ? special : 2;
  }

  /**
   * @return Whether the deck has no cards left to draw.
   */
  public boolean isEmpty() {
    return _cards.isEmpty();
  }

  /**
   * @return The number of cards remaining in the deck.
   */
  public int size() {
    return _cards.size();
  }

  /**
   * Draws the top card.
   *
   * @return The drawn card, or null if the deck is empty.
   */
  public ProgressCardType draw() {
    return _cards.poll();
  }
}
