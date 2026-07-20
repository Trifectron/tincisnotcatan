package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCardType;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Referee.GameStatus;

public class EndTurnTest {

  private static MasterReferee cnkRef() {
    JsonObject j = new JsonObject();
    j.addProperty("numPlayers", 2);
    j.addProperty("isCitiesAndKnights", true);
    return new MasterReferee(new GameSettings(j));
  }

  private static MasterReferee baseRef() {
    JsonObject j = new JsonObject();
    j.addProperty("numPlayers", 2);
    j.addProperty("isCitiesAndKnights", false);
    return new MasterReferee(new GameSettings(j));
  }

  // End-turn must queue a DropCards follow-up for the player whose hand
  // exceeds the 7-card limit (base game).
  @Test
  public void queuesDropCardsWhenPlayerExceedsHandLimit() {
    MasterReferee ref = baseRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();
    current.addResource(Resource.WOOD, 8, ref.getBank());
    assertTrue(current.getNumResourceCards() > 7.0);

    new EndTurn(ref, currentId).execute();

    // After EndTurn, the player who just ended their turn must have a
    // DropCards follow-up queued for them.
    FollowUpAction fu = ref.getNextFollowUp(currentId);
    assertNotNull("Expected a DropCards follow-up to be queued", fu);
    assertEquals("dropCards", fu.getID());
    assertEquals(1.0, fu.getData().get("numToDrop").getAsDouble(), 0.0001);
  }

  // No discard is queued when the acting player is at or below the 7-card
  // limit.
  @Test
  public void noDropFollowUpWhenPlayerIsAtHandLimit() {
    MasterReferee ref = baseRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();
    current.addResource(Resource.WOOD, 7, ref.getBank());

    new EndTurn(ref, currentId).execute();

    // No drop is queued for the current player. Other follow-ups (RollDice
    // for the next player) may exist.
    assertTrue(ref.getNextFollowUp(currentId) == null
        || !"dropCards".equals(ref.getNextFollowUp(currentId).getID()));
  }

  // Cities & Knights city walls raise the threshold: a player with one
  // walled city holding 9 cards is at the threshold and triggers no drop.
  @Test
  public void wallsRaiseTheHandLimitThreshold() {
    MasterReferee ref = cnkRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();

    edu.brown.cs.board.Intersection cityInt = null;
    for (edu.brown.cs.board.Intersection i : ref.getBoard()
        .getIntersections().values()) {
      cityInt = i;
      break;
    }
    cityInt.placeSettlement(current);
    cityInt.placeCity(current);
    ((edu.brown.cs.board.City) cityInt.getBuilding()).buildWall();

    current.addResource(Resource.WOOD, 9, ref.getBank());
    new EndTurn(ref, currentId).execute();

    // Threshold = 7 + 2 = 9; player has 9; queue should have NO drop for
    // this player.
    FollowUpAction fu = ref.getNextFollowUp(currentId);
    assertTrue(fu == null || !"dropCards".equals(fu.getID()));
  }

  // Player with 10 cards and one wall (threshold 9) must drop exactly 1.
  @Test
  public void wallsLimitDropsPartialExcess() {
    MasterReferee ref = cnkRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();

    edu.brown.cs.board.Intersection cityInt = null;
    for (edu.brown.cs.board.Intersection i : ref.getBoard()
        .getIntersections().values()) {
      cityInt = i;
      break;
    }
    cityInt.placeSettlement(current);
    cityInt.placeCity(current);
    ((edu.brown.cs.board.City) cityInt.getBuilding()).buildWall();

    current.addResource(Resource.WOOD, 10, ref.getBank());
    new EndTurn(ref, currentId).execute();

    FollowUpAction fu = ref.getNextFollowUp(currentId);
    assertNotNull("Expected DropCards follow-up to be queued", fu);
    assertEquals("dropCards", fu.getID());
    assertEquals(1.0, fu.getData().get("numToDrop").getAsDouble(), 0.0001);
  }

  // Cities & Knights: a player must discard down to the 4-progress-card
  // hand limit at the end of their own turn.
  @Test
  public void queuesDropProgressCardsWhenPlayerExceedsHandLimit() {
    MasterReferee ref = cnkRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();
    current.addProgressCard(ProgressCardType.SPY);
    current.addProgressCard(ProgressCardType.SPY);
    current.addProgressCard(ProgressCardType.INTRIGUE);
    current.addProgressCard(ProgressCardType.WARLORD);
    current.addProgressCard(ProgressCardType.BISHOP);
    assertEquals(5, current.getProgressCards().size());

    new EndTurn(ref, currentId).execute();

    FollowUpAction fu = ref.getNextFollowUp(currentId);
    assertNotNull("Expected a DropProgressCards follow-up to be queued", fu);
    assertEquals("dropProgressCards", fu.getID());
    assertEquals(1, fu.getData().get("numToDrop").getAsInt());
  }

  // No discard is queued at exactly the 4-card progress-card limit.
  @Test
  public void noDropProgressCardsFollowUpAtHandLimit() {
    MasterReferee ref = cnkRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();
    current.addProgressCard(ProgressCardType.SPY);
    current.addProgressCard(ProgressCardType.INTRIGUE);
    current.addProgressCard(ProgressCardType.WARLORD);
    current.addProgressCard(ProgressCardType.BISHOP);

    new EndTurn(ref, currentId).execute();

    FollowUpAction fu = ref.getNextFollowUp(currentId);
    assertTrue(fu == null || !"dropProgressCards".equals(fu.getID()));
  }

  // Base-game (non-C&K) games have no progress-card hand limit at all.
  @Test
  public void baseGameNeverQueuesDropProgressCards() {
    MasterReferee ref = baseRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();

    new EndTurn(ref, currentId).execute();

    FollowUpAction fu = ref.getNextFollowUp(currentId);
    assertTrue(fu == null || !"dropProgressCards".equals(fu.getID()));
  }
}
