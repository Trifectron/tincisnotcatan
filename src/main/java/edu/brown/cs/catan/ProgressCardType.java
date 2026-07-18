package edu.brown.cs.catan;

import java.util.Map;

import edu.brown.cs.board.City;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Knight;
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
  }),

  INTRIGUE(CityImprovement.POLITICS, "Intrigue", (ref, player) -> {
    Knight target = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      Knight k = i.getKnight();
      if (k != null && k.isActive() && !k.getPlayer().equals(player)
          && (target == null || k.getTier() > target.getTier())) {
        target = k;
      }
    }
    if (target == null) {
      return "You played Intrigue but no opposing knight is active.";
    }
    target.deactivate();
    return String.format(
        "You played Intrigue and deactivated %s's tier-%d knight.",
        target.getPlayer().getName(), target.getTier());
  }),

  WEDDING(CityImprovement.POLITICS, "Wedding", (ref, player) -> {
    int received = 0;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)
          || other.numVictoryPoints() <= player.numVictoryPoints()) {
        continue;
      }
      for (int i = 0; i < 2; i++) {
        Resource biggest = null;
        double max = 0;
        for (Map.Entry<Resource, Double> entry : other.getResources()
            .entrySet()) {
          if (entry.getValue() > max) {
            max = entry.getValue();
            biggest = entry.getKey();
          }
        }
        if (biggest == null) {
          break;
        }
        other.removeResource(biggest, 1, ref.getBank());
        player.addResource(biggest, 1, ref.getBank());
        received++;
      }
    }
    return received > 0
        ? String.format("You played Wedding and received %d resource card(s).",
            received)
        : "You played Wedding but no player has more victory points than you.";
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
