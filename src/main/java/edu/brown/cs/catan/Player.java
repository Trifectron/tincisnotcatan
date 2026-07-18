package edu.brown.cs.catan;

import java.util.List;
import java.util.Map;

/**
 * Represents a Catan Player. Responsible for keeping track of a Player's data
 * including their Hand, Name, Points..etc.
 *
 */
public interface Player {

  /**
   * Returns the number of roads a player has remaining.
   *
   * @return Number of roads remaining.
   */
  int numRoads();

  /**
   * Returns the number of settlements a player has remaining.
   *
   * @return Number of settlements remaining.
   */
  int numSettlements();

  /**
   * Returns the number of cities a player has remaining.
   *
   * @return Number of cities remaining.
   */
  int numCities();

  /**
   * Removes the resource costs of building a road.
   */
  void buildRoad();

  /**
   * Removes the resource costs of building a settlement.
   */
  void buildSettlement();

  /**
   * Removes the resource costs of building a city.
   */
  void buildCity();

  /**
   * Removes the resource costs of buying a development card.
   */
  void buyDevelopmentCard();

  /**
   * Uses a Road piece.
   */
  void useRoad();

  /**
   * Uses a City piece.
   */
  void useCity();

  /**
   * Reverses a city placement: returns a city to the player's supply and uses a
   * settlement piece (e.g. a city downgraded by a barbarian attack).
   */
  void downgradeCity();

  void useSettlement();

  void playDevelopmentCard(DevelopmentCard card);

  Map<Resource, Double> getResources();

  Map<DevelopmentCard, Integer> getDevCards();

  void addResource(Resource resource);

  void addResource(Resource resource, double count);

  void addResource(Resource resource, double count, Bank bank);

  void removeResource(Resource resource);

  void removeResource(Resource resource, double count);

  void removeResource(Resource resource, double count, Bank bank);

  double getNumResourceCards();

  int getNumDevelopmentCards();

  void addDevelopmentCard(DevelopmentCard card);

  Player getImmutableCopy();

  String getName();

  int getID();

  int numPlayedKnights();

  int numVictoryPoints();

  String getColor();

  boolean canBuyDevelopmentCard();

  boolean canBuildRoad();

  boolean canBuildCity();

  boolean canBuildSettlement();

  boolean hasResource(Resource res, double count);

  /**
   * The player's commodity hand (Cities &amp; Knights). Base-game players hold
   * zero of every commodity.
   *
   * @return An unmodifiable map of commodity to count.
   */
  Map<Commodity, Double> getCommodities();

  void addCommodity(Commodity commodity, double count);

  void removeCommodity(Commodity commodity, double count);

  boolean hasCommodity(Commodity commodity, double count);

  /**
   * The player's Cities &amp; Knights city-improvement levels. Base-game players
   * sit at level zero on every track.
   *
   * @return An unmodifiable map of improvement track to level.
   */
  Map<CityImprovement, Integer> getCityImprovements();

  int getImprovementLevel(CityImprovement improvement);

  /**
   * Whether the player can advance the given track: below the max level and
   * holding enough of the track's commodity to pay for the next level.
   */
  boolean canImproveCity(CityImprovement improvement);

  void improveCity(CityImprovement improvement);

  /**
   * The player's held Cities &amp; Knights progress cards (may contain
   * duplicates). Empty for base-game players.
   *
   * @return An unmodifiable list of the player's progress cards.
   */
  List<ProgressCardType> getProgressCards();

  void addProgressCard(ProgressCardType card);

  /**
   * Removes one copy of the given progress card from the player's hand.
   *
   * @return Whether a copy was held and removed.
   */
  boolean removeProgressCard(ProgressCardType card);

  /**
   * Adds victory points directly (e.g. from a victory-point progress card).
   */
  void addVictoryPoints(int points);

}
