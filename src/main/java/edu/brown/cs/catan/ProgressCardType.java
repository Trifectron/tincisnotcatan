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
 * Cards that need a player-chosen target take it as a string in
 * {@link #play(Referee, Player, String)} (e.g. Resource Monopoly's resource
 * name); cards that don't need one ignore the parameter. Remaining cards
 * (Inventor's tile swap, Alchemist's re-rolled dice, Road Building's
 * placement, Diplomat's road pick, Bishop's robber target, Merchant/Merchant
 * Fleet's trade hex, Trade Monopoly) are not wired yet.
 *
 */
public enum ProgressCardType {

  // Science deck.
  PRINTER(CityImprovement.SCIENCE, "Printer", (ref, player, target) -> {
    player.addVictoryPoints(1);
    return "You played Printer and gained a victory point.";
  }),

  IRRIGATION(CityImprovement.SCIENCE, "Irrigation", (ref, player, target) -> {
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

  ENGINEER(CityImprovement.SCIENCE, "Engineer", (ref, player, target) -> {
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
  CONSTITUTION(CityImprovement.POLITICS, "Constitution", (ref, player, target) -> {
    player.addVictoryPoints(1);
    return "You played Constitution and gained a victory point.";
  }),

  INTRIGUE(CityImprovement.POLITICS, "Intrigue", (ref, player, target) -> {
    Knight strongest = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      Knight k = i.getKnight();
      if (k != null && k.isActive() && !k.getPlayer().equals(player)
          && (strongest == null || k.getTier() > strongest.getTier())) {
        strongest = k;
      }
    }
    if (strongest == null) {
      return "You played Intrigue but no opposing knight is active.";
    }
    strongest.deactivate();
    return String.format(
        "You played Intrigue and deactivated %s's tier-%d knight.",
        strongest.getPlayer().getName(), strongest.getTier());
  }),

  WEDDING(CityImprovement.POLITICS, "Wedding", (ref, player, target) -> {
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
  }),

  // Trade deck.
  MASTER_MERCHANT(CityImprovement.TRADE, "Master Merchant", (ref, player, target) -> {
    Player richest = null;
    double richestCount = 0;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)) {
        continue;
      }
      double count = 0;
      for (double n : other.getResources().values()) {
        count += n;
      }
      // ponytail: ties keep the first player found; the real rule lets the
      // card's player break ties, which needs a target parameter to support.
      if (count > richestCount) {
        richestCount = count;
        richest = other;
      }
    }
    if (richest == null) {
      return "You played Master Merchant but no other player holds any cards.";
    }
    int received = 0;
    for (int i = 0; i < 2; i++) {
      Resource biggest = null;
      double max = 0;
      for (Map.Entry<Resource, Double> entry : richest.getResources()
          .entrySet()) {
        if (entry.getValue() > max) {
          max = entry.getValue();
          biggest = entry.getKey();
        }
      }
      if (biggest == null) {
        break;
      }
      richest.removeResource(biggest, 1, ref.getBank());
      player.addResource(biggest, 1, ref.getBank());
      received++;
    }
    return String.format(
        "You played Master Merchant and took %d resource card(s) from %s.",
        received, richest.getName());
  }),

  RESOURCE_MONOPOLY(CityImprovement.TRADE, "Resource Monopoly", (ref, player,
      target) -> {
    Resource res = Resource.stringToResource(target);
    int received = 0;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)) {
        continue;
      }
      double has = other.getResources().get(res);
      if (has <= 0) {
        continue;
      }
      other.removeResource(res, 1, ref.getBank());
      player.addResource(res, 1, ref.getBank());
      received++;
    }
    return received > 0
        ? String.format(
            "You played Resource Monopoly and took %d %s card(s).", received,
            res.toString())
        : String.format(
            "You played Resource Monopoly but no other player has any %s.",
            res.toString());
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
   * Plays this card with no target, applying its effect. Only valid for
   * cards that don't need a player-chosen target.
   *
   * @param ref
   *          The referee for the game.
   * @param player
   *          The player playing the card.
   * @return A message describing what happened.
   */
  public String play(Referee ref, Player player) {
    return play(ref, player, null);
  }

  /**
   * Plays this card, applying its effect.
   *
   * @param ref
   *          The referee for the game.
   * @param player
   *          The player playing the card.
   * @param target
   *          A player-chosen target (e.g. a resource name for Resource
   *          Monopoly); ignored by cards that don't need one.
   * @return A message describing what happened.
   */
  public String play(Referee ref, Player player, String target) {
    return _effect.apply(ref, player, target);
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
