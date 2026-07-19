package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Knight;
import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

public class UpgradeKnightTest {

  private static MasterReferee cnk(int numPlayers) {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", numPlayers);
    return new MasterReferee(new GameSettings(json));
  }

  // Reaches a basic, then strong knight on the first empty intersection
  // owned by the player. Returns the intersection. Pre-stocks the player
  // with the ore+sheep needed per upgrade (two upgrades' worth) but does
  // NOT stock any commodities.
  private static Intersection placeUpgradableKnight(MasterReferee ref,
      Player p) {
    Intersection target = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() == null && i.getKnight() == null) {
        target = i;
        break;
      }
    }
    assertNotNull(target);
    target.placeKnight(p);
    return target;
  }

  private static void stockKnightCosts(Player p, int stacks) {
    p.addResource(Resource.ORE, stacks);
    p.addResource(Resource.SHEEP, stacks);
  }

  private static void advancePoliticsTo(Player p, int targetLevel) {
    // Level N costs N coins.
    int coinsNeeded = targetLevel * (targetLevel + 1) / 2;
    p.addCommodity(Commodity.COIN, coinsNeeded);
    for (int level = 0; level < targetLevel; level++) {
      assertTrue("Politics improvement should be affordable",
          p.canImproveCity(CityImprovement.POLITICS));
      p.improveCity(CityImprovement.POLITICS);
    }
    assertEquals(targetLevel, p.getImprovementLevel(CityImprovement.POLITICS));
  }

  @Test
  public void upgradesFromTier1ToTier2WithoutAnyImprovement() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa);
    stockKnightCosts(pa, 1);

    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();

    assertEquals(2, knightAt.getKnight().getTier());
  }

  @Test
  public void gatesMightyPromotionOnPoliticsLevelThree() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa);

    // Two upgrades' worth of ore+sheep so both steps are affordable.
    stockKnightCosts(pa, 2);
    // Politics level 2 is not enough — must be level 3.
    advancePoliticsTo(pa, 2);

    // First upgrade: tier 1 to tier 2 — always allowed.
    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    assertEquals(2, knightAt.getKnight().getTier());

    // Second upgrade: tier 2 to tier 3 — gated.
    double oreAfterSecondStep = pa.getResources().get(Resource.ORE);
    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    assertEquals(2, knightAt.getKnight().getTier());
    assertEquals("No ore should be spent on a rejected upgrade",
        oreAfterSecondStep, pa.getResources().get(Resource.ORE), 0.0001);
  }

  @Test
  public void allowsMightyPromotionAtPoliticsLevelThree() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa);
    stockKnightCosts(pa, 2);
    advancePoliticsTo(pa, 3);

    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    assertEquals(Settings.MAX_KNIGHT_TIER, knightAt.getKnight().getTier());
  }

  @Test
  public void rejectsWhenNoKnightAtIntersection() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Intersection empty = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() == null && i.getKnight() == null) {
        empty = i;
        break;
      }
    }
    assertNotNull(empty);
    stockKnightCosts(pa, 1);

    Map<Integer, ActionResponse> response =
        new UpgradeKnight(ref, p0, empty.getPosition()).execute();
    assertFalse(response.get(p0).getSuccess());
  }

  @Test
  public void rejectsWhenTierAlreadyMaxed() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa);
    Knight knight = knightAt.getKnight();
    stockKnightCosts(pa, 2);
    advancePoliticsTo(pa, 3);

    // Force-max the knight directly to bypass the gated upgrade steps.
    while (knight.canUpgrade()) {
      knight.upgrade();
    }
    assertEquals(Settings.MAX_KNIGHT_TIER, knight.getTier());

    Map<Integer, ActionResponse> response =
        new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    assertFalse(response.get(p0).getSuccess());
  }

  @Test
  public void rejectsWhenNotCitiesAndKnights() {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", false);
    json.addProperty("numPlayers", 2);
    MasterReferee ref = new MasterReferee(new GameSettings(json));
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa);
    stockKnightCosts(pa, 1);

    Map<Integer, ActionResponse> response =
        new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    assertFalse(response.get(p0).getSuccess());
  }
}
