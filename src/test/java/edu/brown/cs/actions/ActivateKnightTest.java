package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Knight;
import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

public class ActivateKnightTest {

  private static MasterReferee cnk(int numPlayers) {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", numPlayers);
    return new MasterReferee(new GameSettings(json));
  }

  private static Intersection placeUpgradableKnight(MasterReferee ref,
      Player p, Commodity chosen) {
    Intersection target = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() == null && i.getKnight() == null) {
        target = i;
        break;
      }
    }
    target.placeKnight(p);
    return target;
  }

  // Official Cities & Knights: activating a knight costs 1 grain + 1 commodity
  // (the player picks any color).
  @Test
  public void activatesKnightSpendingWheatAndACommodity() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa, Commodity.CLOTH);
    pa.addResource(Resource.WHEAT, 1);
    pa.addCommodity(Commodity.CLOTH, 1);

    double clothBefore = pa.getCommodities().getOrDefault(Commodity.CLOTH, 0.0);
    double wheatBefore = pa.getResources().getOrDefault(Resource.WHEAT, 0.0);

    Map<Integer, ActionResponse> resp = new ActivateKnight(ref, p0,
        knightAt.getPosition(), Commodity.CLOTH).execute();

    assertTrue(resp.get(p0).getSuccess());
    assertTrue(knightAt.getKnight().isActive());
    assertEquals(clothBefore - 1,
        pa.getCommodities().getOrDefault(Commodity.CLOTH, 0.0), 0.0001);
    assertEquals(wheatBefore - 1,
        pa.getResources().getOrDefault(Resource.WHEAT, 0.0), 0.0001);
  }

  @Test
  public void rejectsWhenPlayerHasWheatButNoCommodity() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa, Commodity.CLOTH);
    pa.addResource(Resource.WHEAT, 1);

    Map<Integer, ActionResponse> resp = new ActivateKnight(ref, p0,
        knightAt.getPosition(), Commodity.CLOTH).execute();

    assertFalse(resp.get(p0).getSuccess());
    Knight k = knightAt.getKnight();
    assertFalse(k.isActive());
  }
}
