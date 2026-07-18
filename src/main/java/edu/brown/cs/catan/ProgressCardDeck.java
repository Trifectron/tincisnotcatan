package edu.brown.cs.catan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * A shuffled draw pile of the progress cards for one improvement track. A game
 * keeps one deck per track (Trade, Politics, Science); a player draws from the
 * deck matching the discipline rolled.
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
        cards.add(card);
      }
    }
    Collections.shuffle(cards);
    _cards = new ArrayDeque<>(cards);
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
