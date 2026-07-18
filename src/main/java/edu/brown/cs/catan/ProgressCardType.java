package edu.brown.cs.catan;

import edu.brown.cs.board.City;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;

/**
 * The catalog of Cities &amp; Knights progress cards. Each card belongs to one
 * improvement deck (Trade, Politics, or Science) and carries its own effect.
 *
 * Cards whose real effect requires the player to name a target (Inventor's
 * tile swap, Alchemist's re-rolled dice, Road Building's placement, Diplomat's
 * road pick, Bishop's robber target, Merchant/Merchant Fleet's trade hex) are
 * not wired yet: PlayProgressCard only carries a card name, no extra choice
 * parameter. ponytail: extend PlayProgressCard with a target field (like
 * PlayMonopoly's resource param) when those cards are added.
 *
 */
public enum ProgressCardType {

  // Science deck.
  PRINTER(CityImprovement.SCIENCE, "Printer", (ref, player) -> {
    player.addVictoryPoints(1);
    return "You played Printer and gained a victory point.";
  }),

  IRRIGATION(CityImprovement.SCIENCE, "Irrigation", (ref, player) -> {
    int wheat = 0;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getType() != TileType.WHEAT) {
        continue;
      }
      for (Intersection i : tile.getIntersections()) {
        if (i.getBuilding() == null || !i.getBuilding().getPlayer()
            .equals(player)) {
          continue;
        }
        wheat += i.getBuilding() instanceof City ? 4 : 2;
      }
    }
    if (wheat > 0) {
      player.addResource(Resource.WHEAT, wheat, ref.getBank());
    }
    return wheat > 0
        ? String.format("You played Irrigation and received %d wheat.", wheat)
        : "You played Irrigation but have no settlements or cities on a wheat hex.";
  }),

  ENGINEER(CityImprovement.SCIENCE, "Engineer", (ref, player) -> {
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() instanceof City && i.getBuilding().getPlayer()
          .equals(player) && !((City) i.getBuilding()).hasWall()) {
        ((City) i.getBuilding()).buildWall();
        return "You played Engineer and built a free city wall.";
      }
    }
    return "You played Engineer but have no unwalled city to fortify.";
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
