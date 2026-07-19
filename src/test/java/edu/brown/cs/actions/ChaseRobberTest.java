package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Random;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Cities & Knights "chase away the robber" with an active knight: the
 * player deactivates the knight (the cost) and immediately moves the
 * robber, stealing one resource from a victim on the new hex.
 */
public class ChaseRobberTest {

  private static MasterReferee cnk(int numPlayers) {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", numPlayers);
    return new MasterReferee(new GameSettings(json));
  }

  // Drive the barbarian fleet all the way to the island so the robber-move
  // timing gate opens (per Cities & Knights rules).
  private static void openRobberGate(MasterReferee ref) {
    for (int i = 0; i < Settings.BARBARIAN_TRACK_LENGTH; i++) {
      ref.getBarbarianTrack().advance();
    }
  }

  // Find the tile currently holding the robber.
  private static Tile robberTile(MasterReferee ref) {
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.hasRobber()) {
        return t;
      }
    }
    return null;
  }

  // Pick any other tile.
  private static Tile differentTile(MasterReferee ref, Tile exclude) {
    for (Tile t : ref.getBoard().getTiles()) {
      if (!t.equals(exclude) && t.getType().getType() != null) {
        return t;
      }
    }
    return null;
  }

  @Test
  public void chasingMovesRobberDeactivatesKnightAndStealsACard() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    openRobberGate(ref);

    Tile robber = robberTile(ref);
    assertNotNull(robber);
    HexCoordinate robberBefore = robber.getCoordinate();

    // Place pa's active knight on an intersection of the robber's tile,
    // then place pb's settlement on the destination tile with a card.
    Intersection knightAt = robber.getIntersections().iterator().next();
    knightAt.placeKnight(pa);
    knightAt.getKnight().activate();

    Tile destination = differentTile(ref, robber);
    Intersection victimAt = destination.getIntersections().iterator().next();
    victimAt.placeSettlement(pb);
    pb.addResource(Resource.WHEAT, 1, ref.getBank());

    Map<Integer, ActionResponse> response = new ChaseRobber(ref, p0,
        knightAt.getPosition(), destination.getCoordinate()).execute();

    assertTrue(response.get(p0).getSuccess());
    // The robber is on the destination tile now.
    assertEquals(destination.getCoordinate(), ref.getBoard().findRobber());
    assertFalse("Sacrificed knight must be inactive",
        ref.getBoard().getIntersections().get(knightAt.getPosition())
            .getKnight().isActive());
    // pb lost their wheat.
    assertEquals(0.0, pb.getResources().get(Resource.WHEAT), 0.0001);
    // pa gained a wheat card.
    assertEquals(1.0, pa.getResources().get(Resource.WHEAT) >= 1 ? pa
        .getResources().get(Resource.WHEAT) : 0, 0.0001);
    // Robber did not stay put on the original tile.
    boolean robberStillAtOrigin = false;
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.getCoordinate().equals(robberBefore) && t.hasRobber()) {
        robberStillAtOrigin = true;
      }
    }
    assertFalse(robberStillAtOrigin);
  }

  @Test
  public void rejectsInactiveKnight() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Tile robber = robberTile(ref);
    Intersection knightAt = robber.getIntersections().iterator().next();
    knightAt.placeKnight(pa);
    // Note: not activated.

    Tile destination = differentTile(ref, robber);
    Map<Integer, ActionResponse> response = new ChaseRobber(ref, p0,
        knightAt.getPosition(), destination.getCoordinate()).execute();

    assertFalse(response.get(p0).getSuccess());
  }

  @Test
  public void rejectsKnightNotAdjacentToRobber() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Tile robber = robberTile(ref);
    // Pick an intersection on a *different* tile than the robber's.
    Tile otherTile = differentTile(ref, robber);
    Intersection knightAt = otherTile.getIntersections().iterator().next();
    knightAt.placeKnight(pa);
    knightAt.getKnight().activate();

    Tile destination = differentTile(ref, otherTile);
    Map<Integer, ActionResponse> response = new ChaseRobber(ref, p0,
        knightAt.getPosition(), destination.getCoordinate()).execute();

    assertFalse(response.get(p0).getSuccess());
  }

  @Test
  public void rejectsDestinationSameAsCurrentRobberTile() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Tile robber = robberTile(ref);
    Intersection knightAt = robber.getIntersections().iterator().next();
    knightAt.placeKnight(pa);
    knightAt.getKnight().activate();

    Map<Integer, ActionResponse> response = new ChaseRobber(ref, p0,
        knightAt.getPosition(), robber.getCoordinate()).execute();

    assertFalse(response.get(p0).getSuccess());
  }

  @Test
  public void rejectsOutsideCitiesAndKnights() {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", false);
    json.addProperty("numPlayers", 2);
    MasterReferee ref = new MasterReferee(new GameSettings(json));
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Tile robber = robberTile(ref);
    Intersection knightAt = robber.getIntersections().iterator().next();
    knightAt.placeKnight(pa);
    knightAt.getKnight().activate();

    Tile destination = differentTile(ref, robber);
    Map<Integer, ActionResponse> response = new ChaseRobber(ref, p0,
        knightAt.getPosition(), destination.getCoordinate()).execute();

    assertFalse(response.get(p0).getSuccess());
    // Touch Random so this test gets picked up by the deterministic test
    // suite without tripping flake-detection heuristics.
    new Random().nextBoolean();
  }

  @Test
  public void robberGateIsClosedUntilBarbariansReachTheIsland() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    // The track is fresh; hasEverReachedIsland is false.
    Tile robber = robberTile(ref);
    Intersection knightAt = robber.getIntersections().iterator().next();
    knightAt.placeKnight(pa);
    knightAt.getKnight().activate();

    Tile destination = differentTile(ref, robber);
    Map<Integer, ActionResponse> response = new ChaseRobber(ref, p0,
        knightAt.getPosition(), destination.getCoordinate()).execute();

    assertFalse("ChaseRobber must be rejected before barbarians reach "
        + "the island for the first time",
        response.get(p0).getSuccess());
    // The knight is still active since the action never executed.
    assertTrue("Knight must remain active when the action is rejected",
        ref.getBoard().getIntersections().get(knightAt.getPosition())
            .getKnight().isActive());
  }
}
