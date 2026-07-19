package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

public class PlaceInitialSettlementTest {

  private static MasterReferee baseRef(boolean ck) {
    JsonObject j = new JsonObject();
    j.addProperty("numPlayers", 1);
    j.addProperty("isCitiesAndKnights", ck);
    return new MasterReferee(new GameSettings(j));
  }

  private static PlaceInitialSettlement enqueue(MasterReferee ref, int p0,
      int settlementNum) {
    PlaceInitialSettlement settle =
        new PlaceInitialSettlement(p0, settlementNum);
    ref.addFollowUp(java.util.Collections.singletonList(settle));
    return settle;
  }

  private static Tile findTile(MasterReferee ref, TileType type) {
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.getType() == type) {
        return t;
      }
    }
    return null;
  }

  // Building second-initial-settlement in a base game: only resources are
  // credited; no commodities when the hex also produces one.
  @Test
  public void secondSettlementHasNoCommodityYieldInBaseGame() {
    MasterReferee ref = baseRef(false);
    int p0 = ref.addPlayer("Tester", "Red");
    Player p = ref.getPlayerByID(p0);

    Tile ore = findTile(ref, TileType.ORE);
    assertNotNull(ore);
    Intersection hexIntersection = ore.getIntersections().iterator().next();

    PlaceInitialSettlement settle = enqueue(ref, p0, 2);
    settle.setupAction(ref, p0, serialize(hexIntersection));

    Map<Integer, ActionResponse> resp = settle.execute();
    assertTrue(resp.get(p0).getSuccess());

    // Base game: ore resource was added, no commodity.
    assertEquals(1.0,
        p.getResources().getOrDefault(Resource.ORE, 0.0), 0.0001);
    assertEquals(0.0,
        p.getCommodities().getOrDefault(Commodity.COIN, 0.0), 0.0001);
  }

  // Cities & Knights: second-initial-settlement on a hex that produces
  // both a resource AND a commodity must credit both, per the rulebook.
  @Test
  public void secondSettlementYieldsCommodityInCitiesAndKnights() {
    MasterReferee ref = baseRef(true);
    int p0 = ref.addPlayer("Tester", "Red");
    Player p = ref.getPlayerByID(p0);

    Tile ore = findTile(ref, TileType.ORE);
    assertNotNull(ore);
    Intersection hexIntersection = ore.getIntersections().iterator().next();

    PlaceInitialSettlement settle = enqueue(ref, p0, 2);
    settle.setupAction(ref, p0, serialize(hexIntersection));

    Map<Integer, ActionResponse> resp = settle.execute();
    assertTrue(resp.get(p0).getSuccess());

    assertTrue(p.getResources().getOrDefault(Resource.ORE, 0.0) >= 1.0);
    assertTrue(p.getCommodities().getOrDefault(Commodity.COIN, 0.0) >= 1.0);
  }

  private static JsonObject serialize(Intersection i) {
    HexCoordinate c1 = i.getPosition().getCoord1();
    HexCoordinate c2 = i.getPosition().getCoord2();
    HexCoordinate c3 = i.getPosition().getCoord3();
    JsonObject params = new JsonObject();
    params.add("coordinate", coord(c1, c2, c3));
    return params;
  }

  private static JsonObject coord(HexCoordinate c1, HexCoordinate c2,
      HexCoordinate c3) {
    JsonObject o = new JsonObject();
    o.add("coord1", hex(c1));
    o.add("coord2", hex(c2));
    o.add("coord3", hex(c3));
    return o;
  }

  private static JsonObject hex(HexCoordinate h) {
    JsonObject o = new JsonObject();
    o.addProperty("x", h.getX());
    o.addProperty("y", h.getY());
    o.addProperty("z", h.getZ());
    return o;
  }
}
