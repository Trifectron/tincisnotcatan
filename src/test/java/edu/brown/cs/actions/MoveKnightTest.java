package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
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

/**
 * Cities &amp; Knights knight-displacement rules: a knight may move onto an
 * intersection held by a strictly weaker opposing knight, bumping it to an
 * open intersection along its own owner's roads (or off the board entirely
 * if no such intersection exists). A knight may never displace another
 * knight belonging to the same player, or one of equal or greater tier.
 * Moving a knight -- whether to an empty intersection or by displacing an
 * enemy -- turns the mover to its inactive side.
 */
public class MoveKnightTest {

  private static MasterReferee cnk(int numPlayers) {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", numPlayers);
    return new MasterReferee(new GameSettings(json));
  }

  // Finds an intersection with three distinct road connections, so tests can
  // build a small road network (x)-(y)-(z) plus a spare (y)-(w) edge for
  // displacement relocation targets.
  private static Intersection threeWayJunction(MasterReferee ref) {
    for (Intersection y : ref.getBoard().getIntersections().values()) {
      if (y.getPaths().size() >= 3) {
        return y;
      }
    }
    return null;
  }

  @Test
  public void movingToAnEmptyIntersectionDeactivatesTheKnight() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Intersection y = threeWayJunction(ref);
    assertNotNull("Board should have a 3-way intersection", y);
    List<Path> paths = y.getPaths();
    Path pathToX = paths.get(0);
    Path pathToZ = paths.get(1);
    Intersection x = pathToX.getOtherEnd(y);
    Intersection z = pathToZ.getOtherEnd(y);

    x.placeSettlement(pa);
    pathToX.placeRoad(pa);
    pathToZ.placeRoad(pa);
    z.placeKnight(pa);
    z.getKnight().activate();

    Map<Integer, ActionResponse> response = new MoveKnight(ref, p0,
        z.getPosition(), y.getPosition()).execute();

    assertTrue(response.get(p0).getSuccess());
    assertFalse(z.hasKnight());
    assertNotNull(y.getKnight());
    assertEquals(pa, y.getKnight().getPlayer());
    assertFalse("A moved knight turns inactive", y.getKnight().isActive());
  }

  @Test
  public void displacesAWeakerEnemyKnightToAnOpenRelocationSpot() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    Intersection y = threeWayJunction(ref);
    assertNotNull("Board should have a 3-way intersection", y);
    List<Path> paths = y.getPaths();
    Path pathToX = paths.get(0);
    Path pathToZ = paths.get(1);
    Path pathToW = paths.get(2);
    Intersection x = pathToX.getOtherEnd(y);
    Intersection z = pathToZ.getOtherEnd(y);
    Intersection w = pathToW.getOtherEnd(y);

    // pa's road network: x (settlement) - y - z, active tier-2 knight at z.
    x.placeSettlement(pa);
    pathToX.placeRoad(pa);
    pathToZ.placeRoad(pa);
    z.placeKnight(pa);
    z.getKnight().upgrade();
    z.getKnight().activate();

    // pb's road network reaches y via w, but w itself stays empty (no
    // building) so it's a legal relocation spot: bootstrap the w-y road
    // from a settlement one hop further out, at v.
    Path pathWtoV = null;
    for (Path p : w.getPaths()) {
      if (!p.getOtherEnd(w).equals(y)) {
        pathWtoV = p;
        break;
      }
    }
    assertNotNull("w should have a neighbor besides y", pathWtoV);
    Intersection v = pathWtoV.getOtherEnd(w);
    v.placeSettlement(pb);
    pathWtoV.placeRoad(pb);
    pathToW.placeRoad(pb);
    y.placeKnight(pb);

    Map<Integer, ActionResponse> response = new MoveKnight(ref, p0,
        z.getPosition(), y.getPosition()).execute();

    assertTrue(response.get(p0).getSuccess());
    assertFalse(z.hasKnight());
    assertNotNull(y.getKnight());
    assertEquals(pa, y.getKnight().getPlayer());
    assertEquals(2, y.getKnight().getTier());
    assertFalse(y.getKnight().isActive());

    assertNotNull("Displaced knight should relocate to w", w.getKnight());
    assertEquals(pb, w.getKnight().getPlayer());
    assertEquals(1, w.getKnight().getTier());
  }

  @Test
  public void displacedKnightNeverRelocatesOntoABuilding() {
    MasterReferee ref = cnk(3);
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    int p2 = ref.addPlayer("C", "#222222");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    Player pc = ref.getPlayerByID(p2);

    Intersection y = threeWayJunction(ref);
    assertNotNull("Board should have a 3-way intersection", y);
    List<Path> paths = y.getPaths();
    Path pathToX = paths.get(0);
    Path pathToZ = paths.get(1);
    Path pathToW = paths.get(2);
    Intersection x = pathToX.getOtherEnd(y);
    Intersection z = pathToZ.getOtherEnd(y);
    Intersection w = pathToW.getOtherEnd(y);

    x.placeSettlement(pa);
    pathToX.placeRoad(pa);
    pathToZ.placeRoad(pa);
    z.placeKnight(pa);
    z.getKnight().upgrade();
    z.getKnight().activate();

    // pb's road reaches y via w (bootstrapped from a settlement at v), but
    // w gets a (third player's) building after the road goes in, so it must
    // be skipped as a relocation target even though the road is legal.
    Path pathWtoV = null;
    for (Path p : w.getPaths()) {
      if (!p.getOtherEnd(w).equals(y)) {
        pathWtoV = p;
        break;
      }
    }
    assertNotNull("w should have a neighbor besides y", pathWtoV);
    Intersection v = pathWtoV.getOtherEnd(w);
    v.placeSettlement(pb);
    pathWtoV.placeRoad(pb);
    pathToW.placeRoad(pb);
    w.placeSettlement(pc);
    y.placeKnight(pb);

    new MoveKnight(ref, p0, z.getPosition(), y.getPosition()).execute();

    assertNotNull(y.getKnight());
    assertEquals(pa, y.getKnight().getPlayer());
    assertEquals(pc, w.getBuilding().getPlayer());
    assertFalse("w has a building, so it can't also hold the displaced "
        + "knight", w.hasKnight());
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.hasKnight()) {
        assertFalse("pb's displaced knight should be gone entirely",
            i.getKnight().getPlayer().equals(pb));
      }
    }
  }

  @Test
  public void displacedKnightIsRemovedWhenNoRelocationSpotExists() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    Intersection y = threeWayJunction(ref);
    assertNotNull("Board should have a 3-way intersection", y);
    List<Path> paths = y.getPaths();
    Path pathToX = paths.get(0);
    Path pathToZ = paths.get(1);
    Intersection x = pathToX.getOtherEnd(y);
    Intersection z = pathToZ.getOtherEnd(y);

    x.placeSettlement(pa);
    pathToX.placeRoad(pa);
    pathToZ.placeRoad(pa);
    z.placeKnight(pa);
    z.getKnight().upgrade();
    z.getKnight().activate();

    // pb's weaker knight sits at y with no road network of its own, so it
    // has nowhere to relocate to.
    y.placeKnight(pb);

    new MoveKnight(ref, p0, z.getPosition(), y.getPosition()).execute();

    assertNotNull(y.getKnight());
    assertEquals(pa, y.getKnight().getPlayer());
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.hasKnight()) {
        assertFalse("pb's displaced knight should be gone entirely",
            i.getKnight().getPlayer().equals(pb));
      }
    }
  }

  @Test
  public void rejectsDisplacingYourOwnKnight() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Intersection y = threeWayJunction(ref);
    assertNotNull("Board should have a 3-way intersection", y);
    List<Path> paths = y.getPaths();
    Path pathToX = paths.get(0);
    Path pathToZ = paths.get(1);
    Intersection x = pathToX.getOtherEnd(y);
    Intersection z = pathToZ.getOtherEnd(y);

    x.placeSettlement(pa);
    pathToX.placeRoad(pa);
    pathToZ.placeRoad(pa);
    z.placeKnight(pa);
    z.getKnight().upgrade();
    z.getKnight().activate();
    y.placeKnight(pa);

    Map<Integer, ActionResponse> response = new MoveKnight(ref, p0,
        z.getPosition(), y.getPosition()).execute();

    assertFalse(response.get(p0).getSuccess());
    assertEquals(1, y.getKnight().getTier());
    assertTrue(z.hasKnight());
  }

  @Test
  public void rejectsDisplacingAnEqualOrStrongerKnight() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    Intersection y = threeWayJunction(ref);
    assertNotNull("Board should have a 3-way intersection", y);
    List<Path> paths = y.getPaths();
    Path pathToX = paths.get(0);
    Path pathToZ = paths.get(1);
    Intersection x = pathToX.getOtherEnd(y);
    Intersection z = pathToZ.getOtherEnd(y);

    x.placeSettlement(pa);
    pathToX.placeRoad(pa);
    pathToZ.placeRoad(pa);
    z.placeKnight(pa);
    z.getKnight().activate();
    y.placeKnight(pb);

    Map<Integer, ActionResponse> response = new MoveKnight(ref, p0,
        z.getPosition(), y.getPosition()).execute();

    assertFalse(response.get(p0).getSuccess());
    assertEquals(pb, y.getKnight().getPlayer());
    assertTrue(z.hasKnight());
  }
}
