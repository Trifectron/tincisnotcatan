package edu.brown.cs.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Path;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

/**
 * Cities &amp; Knights limits each player to 2 physical knight pieces per
 * tier (2 basic, 2 strong, 2 mighty), per the official rulebook's piece
 * count. This covers the basic-tier (build-time) half of that cap; the
 * strong/mighty half is covered by UpgradeKnightTest.
 */
public class BuildKnightTest {

  private static MasterReferee cnk(int numPlayers) {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", numPlayers);
    return new MasterReferee(new GameSettings(json));
  }

  // Finds a 3-way junction y and builds a settlement + road network so that
  // y, and its two neighbors z and w, are each legal knight-build spots
  // (empty, touching one of the player's own roads).
  private static Intersection[] threeBuildSpots(MasterReferee ref, Player p) {
    for (Intersection y : ref.getBoard().getIntersections().values()) {
      List<Path> paths = y.getPaths();
      if (paths.size() < 3) {
        continue;
      }
      Path pathToX = paths.get(0);
      Path pathToZ = paths.get(1);
      Path pathToW = paths.get(2);
      Intersection x = pathToX.getOtherEnd(y);
      Intersection z = pathToZ.getOtherEnd(y);
      Intersection w = pathToW.getOtherEnd(y);
      x.placeSettlement(p);
      pathToX.placeRoad(p);
      pathToZ.placeRoad(p);
      pathToW.placeRoad(p);
      return new Intersection[] {y, z, w};
    }
    return null;
  }

  private static void stockKnightCosts(Player p, int count) {
    p.addResource(Resource.ORE, count);
    p.addResource(Resource.SHEEP, count);
    p.addResource(Resource.WHEAT, count);
  }

  @Test
  public void rejectsBuildingAThirdBasicKnight() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Intersection[] spots = threeBuildSpots(ref, pa);
    assertNotNull("Board should have a 3-way intersection", spots);
    stockKnightCosts(pa, 3);

    Map<Integer, ActionResponse> first = new BuildKnight(ref, p0,
        spots[0].getPosition()).execute();
    Map<Integer, ActionResponse> second = new BuildKnight(ref, p0,
        spots[1].getPosition()).execute();
    assertTrue(first.get(p0).getSuccess());
    assertTrue(second.get(p0).getSuccess());

    Map<Integer, ActionResponse> third = new BuildKnight(ref, p0,
        spots[2].getPosition()).execute();

    assertFalse(third.get(p0).getSuccess());
    assertFalse(spots[2].hasKnight());
  }
}
