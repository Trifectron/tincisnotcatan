package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
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

  // Places n fresh basic knights for p on distinct empty intersections.
  private static List<Intersection> placeKnights(MasterReferee ref, Player p,
      int n) {
    List<Intersection> result = new ArrayList<>();
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() == null && i.getKnight() == null) {
        i.placeKnight(p);
        result.add(i);
        if (result.size() == n) {
          break;
        }
      }
    }
    return result;
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
  public void gatesMightyPromotionOnPoliticsLevelFour() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa);

    // Stock enough ore+sheep for the basic -> strong upgrade (1 ore + 1
    // wool) AND stock two extra ore so the strong -> mighty step would be
    // affordable IF the Politics gate were satisfied. Without the metropolis
    // level on Politics, the second upgrade must be rejected before any
    // resources change hands.
    stockKnightCosts(pa, 3);
    pa.addResource(Resource.ORE, 1);
    // Politics level 3 is not enough — the metropolis level (4) is required
    // per the official Cities & Knights rulebook.
    advancePoliticsTo(pa, 3);

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
  public void allowsMightyPromotionAtPoliticsLevelFour() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa);
    // 3 ore + 2 sheep total: 1 ore + 1 wool for the basic -> strong upgrade,
    // then 2 ore + 1 wool for the strong -> mighty upgrade.
    pa.addResource(Resource.ORE, 3);
    pa.addResource(Resource.SHEEP, 2);
    advancePoliticsTo(pa, 4);

    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    assertEquals(Settings.MAX_KNIGHT_TIER, knightAt.getKnight().getTier());
  }

  // Cities & Knights limits each player to 2 physical pieces per tier (2
  // basic, 2 strong, 2 mighty). BuildKnightTest covers the basic-tier half;
  // these two cover the strong/mighty half.
  @Test
  public void rejectsAThirdKnightUpgradeToStrongTier() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    List<Intersection> knights = placeKnights(ref, pa, 3);
    stockKnightCosts(pa, 3);

    new UpgradeKnight(ref, p0, knights.get(0).getPosition()).execute();
    new UpgradeKnight(ref, p0, knights.get(1).getPosition()).execute();
    assertEquals(2, knights.get(0).getKnight().getTier());
    assertEquals(2, knights.get(1).getKnight().getTier());

    Map<Integer, ActionResponse> third = new UpgradeKnight(ref, p0,
        knights.get(2).getPosition()).execute();

    assertFalse(third.get(p0).getSuccess());
    assertEquals(1, knights.get(2).getKnight().getTier());
  }

  @Test
  public void rejectsAThirdKnightUpgradeToMightyTier() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    List<Intersection> knights = placeKnights(ref, pa, 3);
    // Force all three to tier 2 directly, bypassing the gated action, so
    // the test isolates the tier-3 cap from the tier-2 cap.
    for (Intersection i : knights) {
      i.getKnight().upgrade();
    }
    advancePoliticsTo(pa, 4);
    // Mighty promotion costs 2 ore + 1 wool; one knight upgrade is one of
    // each. We need three such "increments" — stock properly.
    pa.addResource(Resource.ORE, 6);
    pa.addResource(Resource.SHEEP, 3);

    new UpgradeKnight(ref, p0, knights.get(0).getPosition()).execute();
    new UpgradeKnight(ref, p0, knights.get(1).getPosition()).execute();
    assertEquals(3, knights.get(0).getKnight().getTier());
    assertEquals(3, knights.get(1).getKnight().getTier());

    Map<Integer, ActionResponse> third = new UpgradeKnight(ref, p0,
        knights.get(2).getPosition()).execute();

    assertFalse(third.get(p0).getSuccess());
    assertEquals(2, knights.get(2).getKnight().getTier());
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

  // Strong -> mighty costs 2 ore + 1 wool; basic -> strong costs 1 ore + 1
  // wool per the official rulebook.
  @Test
  public void mightyPromotionRequiresAnExtraOreOverBasicUpgrade() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection knightAt = placeUpgradableKnight(ref, pa);

    // First upgrade: stock exactly 1 ore + 1 wool.
    pa.addResource(Resource.ORE, 1);
    pa.addResource(Resource.SHEEP, 1);
    advancePoliticsTo(pa, 4);
    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    assertEquals(2, knightAt.getKnight().getTier());

    // Second upgrade to mighty: needs 2 ore + 1 wool. The remaining ore (0)
    // and wool (0) means the player cannot afford the mighty cost as-is.
    Map<Integer, ActionResponse> rejected = new UpgradeKnight(ref, p0,
        knightAt.getPosition()).execute();
    assertFalse(rejected.get(p0).getSuccess());
    assertEquals(2, knightAt.getKnight().getTier());

    // Add 2 ore + 1 wool so the mighty cost is now paid in full.
    pa.addResource(Resource.ORE, 2);
    pa.addResource(Resource.SHEEP, 1);
    new UpgradeKnight(ref, p0, knightAt.getPosition()).execute();
    assertEquals(Settings.MAX_KNIGHT_TIER, knightAt.getKnight().getTier());
  }
}
