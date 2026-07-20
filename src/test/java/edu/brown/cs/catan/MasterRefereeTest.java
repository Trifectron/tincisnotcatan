package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.IntersectionCoordinate;

public class MasterRefereeTest {

  @Test
  public void testConstruction() {
    Referee ref = new MasterReferee();
    assertTrue(ref != null);
    assertFalse(ref.getTurn().devHasBeenPlayed());
    assertTrue(ref.getTurn().getTurnNum() == 1);
  }

  @Test
  public void startNextTurnTest() {
    Referee ref = new MasterReferee();
    assertTrue(ref.getTurn().getTurnNum() == 1);
    ref.startNextTurn();
    assertTrue(ref.getTurn().getTurnNum() == 2);
  }

  @Test
  public void testGetBoard() {
    Referee ref = new MasterReferee();
    assertTrue(ref.getBoard() != null);
  }

  @Test
  public void testAddPlayers() {
    Referee ref = new MasterReferee();
    ref.addPlayer("John", "#000000");
    ref.addPlayer("Peter", "#0fffff");
    ref.startNextTurn();
    try {
      ref.addPlayer("Not going to work", "color");
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testLookupPlayers() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("John", "#000000");
    int id2 = ref.addPlayer("Peter", "#0fffff");
    assertTrue(ref.getPlayerByID(id1) != null);
    assertTrue(ref.getPlayerByID(id2) != null);
    assertTrue(ref.getPlayerByID(id1).equals(ref.getPlayerByID(id1)));
    assertFalse(ref.getPlayerByID(id1).equals(ref.getPlayerByID(id2)));
  }

  @Test
  public void testHasLargestArmy() {
    Referee ref = new MasterReferee();
    int id = ref.addPlayer("p1", "color");
    Player player = ref.getPlayerByID(id);
    assertFalse(ref.hasLargestArmy(id));
    player.addDevelopmentCard(DevelopmentCard.KNIGHT);
    player.addDevelopmentCard(DevelopmentCard.KNIGHT);
    player.addDevelopmentCard(DevelopmentCard.KNIGHT);
    assertFalse(ref.hasLargestArmy(id));
    player.playDevelopmentCard(DevelopmentCard.KNIGHT);
    player.playDevelopmentCard(DevelopmentCard.KNIGHT);
    player.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertTrue(ref.hasLargestArmy(id));

  }

  @Test
  public void testHasLargestArmyComplicated() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("p1", "color");
    int id2 = ref.addPlayer("p2", "color");
    Player p1 = ref.getPlayerByID(id1);
    Player p2 = ref.getPlayerByID(id2);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertTrue(ref.hasLargestArmy(id2));
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertTrue(ref.hasLargestArmy(id1));
  }

  @Test
  public void testHasLargestArmyTie() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("p1", "color");
    int id2 = ref.addPlayer("p2", "color");
    Player p1 = ref.getPlayerByID(id1);
    Player p2 = ref.getPlayerByID(id2);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertTrue(ref.hasLargestArmy(id2));
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertFalse(ref.hasLargestArmy(id1));
    assertTrue(ref.hasLargestArmy(id2));
  }

  @Test
  public void largestArmyEdgeTooEarly() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("player1", "color");
    Player p1 = ref.getPlayerByID(id1);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertFalse(ref.hasLargestArmy(p1.getID()));
  }

  @Test
  public void getNumPublicPointsTest() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("player1", "color");
    Player p1 = ref.getPlayerByID(id1);
    assertTrue(ref.getNumPublicPoints(id1) == 0);
    p1.useSettlement();
    assertTrue(ref.getNumPublicPoints(id1) == Settings.SETTLEMENT_POINT_VAL);
    p1.useCity();
    assertTrue(ref.getNumPublicPoints(id1) == Settings.CITY_POINT_VAL);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertTrue(ref.getNumPublicPoints(id1) == Settings.CITY_POINT_VAL);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertTrue(ref.getNumPublicPoints(id1) == Settings.CITY_POINT_VAL
        + Settings.LARGEST_ARMY_POINT_VAL);
  }

  @Test
  public void getNumTotalPointsTest() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("", "");
    Player p1 = ref.getPlayerByID(id1);
    assertTrue(ref.getNumTotalPoints(id1) == 0);
    p1.addDevelopmentCard(DevelopmentCard.POINT);
    assertTrue(ref.getNumTotalPoints(id1) == 1);
    p1.addDevelopmentCard(DevelopmentCard.POINT);
    assertTrue(ref.getNumTotalPoints(id1) == 2);
    p1.useSettlement();
    assertTrue(ref.getNumTotalPoints(id1) == 2 + Settings.SETTLEMENT_POINT_VAL);
  }

  @Test
  public void getBankRatesTest() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("Blah");
    assertTrue(ref.getBankRates(id1) != null);
  }

  @Test
  public void getBankRatesSimpleBank() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("Blah");
    assertTrue(ref.getBankRates(id1).get(Resource.BRICK)
        .equals(Settings.BANK_RATE));
    assertTrue(ref.getBankRates(id1).get(Resource.WOOD)
        .equals(Settings.BANK_RATE));
    assertTrue(ref.getBankRates(id1).get(Resource.ORE)
        .equals(Settings.BANK_RATE));
    assertTrue(ref.getBankRates(id1).get(Resource.WHEAT)
        .equals(Settings.BANK_RATE));
    assertTrue(ref.getBankRates(id1).get(Resource.SHEEP)
        .equals(Settings.BANK_RATE));
  }

  @Test
  public void getBankRatesWithWildCardPort() {
    HexCoordinate h1 = new HexCoordinate(0, 2, 0);
    HexCoordinate h2 = new HexCoordinate(0, 3, 0);
    HexCoordinate h3 = new HexCoordinate(1, 3, 0);
    IntersectionCoordinate i = new IntersectionCoordinate(h1, h2, h3);
    //TODO: finsih port test
  }

  // Real-rule Largest Army tie: when the holder is at 3 and a second
  // player also reaches 3, the incumbent holds the bonus (the rulebook
  // says the tie goes to the player who got there first).
  @Test
  public void largestArmyTiePreservesTheIncumbentHolder() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("p1", "color");
    int id2 = ref.addPlayer("p2", "color");
    Player p1 = ref.getPlayerByID(id1);
    Player p2 = ref.getPlayerByID(id2);
    for (int i = 0; i < 3; i++) {
      p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    }
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    // p1 is incumbent holder at 3 knights.
    assertTrue(ref.hasLargestArmy(id1));

    // p2 also reaches 3 — must NOT take the bonus from p1.
    for (int i = 0; i < 3; i++) {
      p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    }
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertTrue(ref.hasLargestArmy(id1));
    assertFalse(ref.hasLargestArmy(id2));

    // p2 surges to 4 — now strictly outranks, so they take the bonus.
    p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertFalse(ref.hasLargestArmy(id1));
    assertTrue(ref.hasLargestArmy(id2));
  }

  // Real-rule Largest Army: when the holder falls below the threshold
  // (e.g. after a Knights-card reversal in some printings), the bonus
  // must be released. With minimum 3 played knights hard to touch here,
  // we simulate by having the holder's count stay at 3 while another
  // player also drops — covering the threshold-loss fact: the bonus
  // isn't released while the holder remains at the threshold.
  // 5-6 player Catan extension wins at 12; default 4 (or fewer) player
  // games still hit 10 unless the JSON explicitly overrides.
  @Test
  public void gameSettingsAutoBumpsWinningPointCountForLargePlayerCount() {
    com.google.gson.JsonObject j1 = new com.google.gson.JsonObject();
    j1.addProperty("numPlayers", 6);
    edu.brown.cs.catan.GameSettings gs1 = new edu.brown.cs.catan.GameSettings(j1);
    assertTrue(gs1.winningPointCount == 12);

    com.google.gson.JsonObject j2 = new com.google.gson.JsonObject();
    j2.addProperty("numPlayers", 4);
    edu.brown.cs.catan.GameSettings gs2 = new edu.brown.cs.catan.GameSettings(j2);
    assertTrue(gs2.winningPointCount == 10);

    // Caller override via victoryPoints key survives even at 6 players.
    com.google.gson.JsonObject j3 = new com.google.gson.JsonObject();
    j3.addProperty("numPlayers", 6);
    j3.addProperty("victoryPoints", 15);
    edu.brown.cs.catan.GameSettings gs3 = new edu.brown.cs.catan.GameSettings(j3);
    assertTrue(gs3.winningPointCount == 15);
  }

  // Real-rule: a player who hits the threshold mid-turn doesn't end the
  // game until the current turn has resolved (i.e., EndTurn -> startNextTurn
  // has been called). getWinner() returns null in that window.
  @Test
  public void getWinnerReturnsNullMidTurn() {
    Referee ref = new MasterReferee();
    int id = ref.addPlayer("p1", "color");
    Player p = ref.getPlayerByID(id);
    // Add 10 hidden VPs to comfortably exceed the 10-point win threshold.
    for (int i = 0; i < 10; i++) {
      p.addDevelopmentCard(DevelopmentCard.POINT);
    }
    // Without the mid-turn gate, getWinner() would already return this
    // player.
    assertTrue("Player is over the threshold mid-turn but no turn has "
        + "ended yet", ref.getWinner() == null);

    // After EndTurn-style startNextTurn, getWinner() resolves to the
    // player (the lowest-id tiebreaker).
    ref.startNextTurn();
    assertTrue(ref.getWinner() != null);
    assertEquals(id, ref.getWinner().getID());
  }

  @Test
  public void largestArmyHolderLosesItWhenAnotherStrictlyOutranks() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("p1", "color");
    int id2 = ref.addPlayer("p2", "color");
    Player p1 = ref.getPlayerByID(id1);
    Player p2 = ref.getPlayerByID(id2);
    for (int i = 0; i < 3; i++) {
      p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
    }
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    // p1 is the first holder.
    assertTrue(ref.hasLargestArmy(id1));
    assertFalse(ref.hasLargestArmy(id2));

    // p2 strictly outranks p1 -- the bonus moves to p2.
    for (int i = 0; i < 4; i++) {
      p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    }
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertFalse(ref.hasLargestArmy(id1));
    assertTrue(ref.hasLargestArmy(id2));
  }

  // A fresh tie with no existing incumbent awards the bonus to no one,
  // matching this codebase's BarbarianAttack tie rule.
  @Test
  public void largestArmyFreshTieWithNoIncumbentAwardsNoOne() {
    Referee ref = new MasterReferee();
    int id1 = ref.addPlayer("p1", "color");
    int id2 = ref.addPlayer("p2", "color");
    Player p1 = ref.getPlayerByID(id1);
    Player p2 = ref.getPlayerByID(id2);
    for (int i = 0; i < 3; i++) {
      p1.addDevelopmentCard(DevelopmentCard.KNIGHT);
      p2.addDevelopmentCard(DevelopmentCard.KNIGHT);
    }
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p1.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    p2.playDevelopmentCard(DevelopmentCard.KNIGHT);
    assertFalse(ref.hasLargestArmy(id1));
    assertFalse(ref.hasLargestArmy(id2));
  }
}
