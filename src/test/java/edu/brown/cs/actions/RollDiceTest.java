package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Cities &amp; Knights: the robber cannot be moved (via any means, including a
 * rolled 7) until the barbarian fleet has reached the island for the first
 * time. Rolling a 7 before that must never queue an unresolvable MoveRobber
 * follow-up -- MoveRobber unconditionally rejects while the gate is closed,
 * so queuing it anyway would soft-lock the game on any pre-barbarian 7.
 */
public class RollDiceTest {

  private static MasterReferee cnkRef() {
    JsonObject j = new JsonObject();
    j.addProperty("numPlayers", 2);
    j.addProperty("isCitiesAndKnights", true);
    return new MasterReferee(new GameSettings(j));
  }

  @Test
  public void sevenBeforeFirstBarbarianLandingQueuesNoMoveRobber() {
    MasterReferee ref = cnkRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();
    ref.addFollowUp(ImmutableList.of(new RollDice(currentId)));

    RollDice roll = (RollDice) ref.getNextFollowUp(currentId);
    roll.forceRoll(7);
    roll.setupAction(ref, currentId, new JsonObject());
    roll.execute();

    FollowUpAction fu = ref.getNextFollowUp(currentId);
    assertTrue("No MoveRobber should be queued while the barbarian gate "
        + "is closed -- it can never succeed and would soft-lock the game",
        fu == null || !MoveRobber.ID.equals(fu.getID()));
  }

  @Test
  public void sevenAfterFirstBarbarianLandingQueuesMoveRobber() {
    MasterReferee ref = cnkRef();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    ref.setGameStatus(GameStatus.PROGRESS);
    Player current = ref.currentPlayer();
    int currentId = current.getID();
    ref.addFollowUp(ImmutableList.of(new RollDice(currentId)));

    for (int i = 0; i < 7; i++) {
      ref.getBarbarianTrack().advance();
    }
    assertEquals(true, ref.getBarbarianTrack().hasEverReachedIsland());

    RollDice roll = (RollDice) ref.getNextFollowUp(currentId);
    roll.forceRoll(7);
    roll.setupAction(ref, currentId, new JsonObject());
    roll.execute();

    FollowUpAction fu = ref.getNextFollowUp(currentId);
    assertNotNull("MoveRobber should be queued once the barbarians have "
        + "reached the island", fu);
    assertEquals(MoveRobber.ID, fu.getID());
  }
}
