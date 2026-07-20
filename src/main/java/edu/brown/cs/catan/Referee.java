package edu.brown.cs.catan;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.board.Board;
import edu.brown.cs.gamestats.GameStats;

public interface Referee {

  void startNextTurn();

  Turn getTurn();

  Player currentPlayer();

  void playDevCard();

  int addPlayer(String name);

  int addPlayer(String name, String color);

  DevelopmentCard getDevCard();

  boolean devCardDeckIsEmpty();

  /**
   * Draws the top progress card from the given improvement track's deck (Cities
   * &amp; Knights).
   *
   * @param track
   *          The improvement track whose deck to draw from.
   * @return The drawn card, or null if the game has no such deck or it is empty.
   */
  ProgressCardType drawProgressCard(CityImprovement track);

  /**
   * @return The barbarian fleet track (Cities &amp; Knights), or null in base
   *         games.
   */
  BarbarianTrack getBarbarianTrack();

  /**
   * Resolves a barbarian attack: compares total active knight strength against
   * the barbarian fleet (the number of cities), applies the consequences
   * (reward the strongest defender, or downgrade the weakest defenders' cities),
   * deactivates all knights, and resets the track.
   *
   * @return A message describing the outcome.
   */
  String resolveBarbarianAttack();

  /**
   * Re-evaluates ownership of the metropolis for the given improvement track
   * (Cities &amp; Knights) and transfers it, with its victory points, if a player
   * now out-levels the current holder. No-op in base games.
   *
   * @param track
   *          The improvement track to re-evaluate.
   */
  void awardMetropolis(CityImprovement track);

  /**
   * @param track
   *          An improvement track.
   * @return The player id holding that track's metropolis, or -1 if none.
   */
  int getMetropolisOwner(CityImprovement track);

  GameSettings getGameSettings();

  Referee getReadOnlyReferee();

  Map<Resource, Double> getBankRates(int playerID);

  /**
   * Grants the current player a 2:1 bank rate on the given resource for the
   * rest of this turn (Cities & Knights Merchant Fleet progress card).
   *
   * @param res
   *          The resource to grant a 2:1 rate on.
   */
  void setMerchantFleetResource(Resource res);

  /**
   * Records the dice value the current player's Alchemist card will force.
   * The matching RollDice.execute() then consults this value instead of the
   * RNG (Cities & Knights rulebook: Alchemist is "played before rolling
   * dice; pick a value from 2-12"). The referee stores this on its *real*
   * turn rather than a defensive copy so the value survives until that
   * roll happens.
   *
   * @param roll the forced roll (2-12)
   */
  void setAlchemisedRoll(int roll);

  /**
   * @return the dice value the current player's Alchemist card has forced
   *         for the upcoming roll, or null if no Alchemist was played or it
   *         has already been consumed. Reflects the value on the real
   *         internal turn (not a defensive copy), so callers and tests can
   *         observe what RollDice.execute() will see.
   */
  Integer getAlchemisedRoll();

  /**
   * Consumes the current player's Alchemist-forced roll so
   * {@link #getAlchemisedRoll()} reports null for the remainder of the
   * turn. Called by RollDice.execute() once it has read the forced value.
   */
  void clearAlchemisedRoll();

  Board getBoard();

  Player getPlayerByID(int id);

  Collection<Player> getPlayers();

  boolean hasLongestRoad(int playerID);

  boolean hasLargestArmy(int playerID);

  int getNumPublicPoints(int playerID);

  int getNumTotalPoints(int playerID);

  Bank getBank();

  GameStatus getGameStatus();

  void setGameStatus(GameStatus state);

  FollowUpAction getNextFollowUp(int playerID);

  void addFollowUp(Collection<FollowUpAction> actions);

  public void removeFollowUp(FollowUpAction action);

  List<Integer> getTurnOrder();

  Setup getSetup();

  Player getWinner();

  GameStats getGameStats();

  boolean removePlayer(int id);

  public enum GameStatus {
    WAITING, // Waiting for players (pre-game)
    SETUP, // Placement of settlements
    PROGRESS; // Regular game in progress
  }

}
