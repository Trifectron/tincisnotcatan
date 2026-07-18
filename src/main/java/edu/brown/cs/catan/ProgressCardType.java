package edu.brown.cs.catan;

/**
 * The catalog of Cities &amp; Knights progress cards. Each card belongs to one
 * improvement deck (Trade, Politics, or Science) and carries its own effect.
 *
 * Only a starter set of cards is wired so far; the remaining cards are added
 * one effect at a time. ponytail: grow the catalog per card, not all at once.
 *
 */
public enum ProgressCardType {

  // Science deck.
  PRINTER(CityImprovement.SCIENCE, "Printer", (ref, player) -> {
    player.addVictoryPoints(1);
    return "You played Printer and gained a victory point.";
  }),

  // Politics deck.
  CONSTITUTION(CityImprovement.POLITICS, "Constitution", (ref, player) -> {
    player.addVictoryPoints(1);
    return "You played Constitution and gained a victory point.";
  });

  private final CityImprovement _deck;
  private final String _name;
  private final ProgressCardEffect _effect;

  private ProgressCardType(CityImprovement deck, String name,
      ProgressCardEffect effect) {
    _deck = deck;
    _name = name;
    _effect = effect;
  }

  /**
   * @return The improvement deck this card is drawn from.
   */
  public CityImprovement getDeck() {
    return _deck;
  }

  /**
   * @return The card's display name.
   */
  public String getName() {
    return _name;
  }

  /**
   * Plays this card, applying its effect.
   *
   * @param ref
   *          The referee for the game.
   * @param player
   *          The player playing the card.
   * @return A message describing what happened.
   */
  public String play(Referee ref, Player player) {
    return _effect.apply(ref, player);
  }

  /**
   * @param name
   *          A card display name (case-insensitive).
   * @return The matching card.
   */
  public static ProgressCardType fromString(String name) {
    for (ProgressCardType card : values()) {
      if (card._name.equalsIgnoreCase(name)) {
        return card;
      }
    }
    throw new IllegalArgumentException(String.format(
        "No progress card named %s.", name));
  }
}
