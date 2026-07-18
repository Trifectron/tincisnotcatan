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

  GameSettings getGameSettings();

  Referee getReadOnlyReferee();

  Map<Resource, Double> getBankRates(int playerID);

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
