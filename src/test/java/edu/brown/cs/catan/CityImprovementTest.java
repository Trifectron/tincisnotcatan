package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CityImprovementTest {

  private static final double EPS = 1e-9;

  @Test
  public void trackCommodityMapping() {
    assertEquals(Commodity.CLOTH, CityImprovement.TRADE.getCommodity());
    assertEquals(Commodity.COIN, CityImprovement.POLITICS.getCommodity());
    assertEquals(Commodity.PAPER, CityImprovement.SCIENCE.getCommodity());
    assertEquals(CityImprovement.TRADE,
        CityImprovement.fromCommodity(Commodity.CLOTH));
    assertEquals(CityImprovement.SCIENCE, CityImprovement.fromString("science"));
  }

  @Test
  public void playerStartsAtLevelZero() {
    Player p = new HumanPlayer(0, "test", "#000000");
    for (CityImprovement imp : CityImprovement.values()) {
      assertEquals(0, p.getImprovementLevel(imp));
      assertFalse(p.canImproveCity(imp));
    }
  }

  @Test
  public void improvingSpendsCommoditiesAndCostsScale() {
    Player p = new HumanPlayer(0, "test", "#000000");
    // Level 1 costs 1, level 2 costs 2 -> 3 coin buys two Politics levels.
    p.addCommodity(Commodity.COIN, 3);

    assertTrue(p.canImproveCity(CityImprovement.POLITICS));
    p.improveCity(CityImprovement.POLITICS);
    assertEquals(1, p.getImprovementLevel(CityImprovement.POLITICS));
    assertEquals(2.0, p.getCommodities().get(Commodity.COIN), EPS);

    p.improveCity(CityImprovement.POLITICS);
    assertEquals(2, p.getImprovementLevel(CityImprovement.POLITICS));
    assertEquals(0.0, p.getCommodities().get(Commodity.COIN), EPS);

    // Broke now: next level would cost 3.
    assertFalse(p.canImproveCity(CityImprovement.POLITICS));
    // Other tracks untouched.
    assertEquals(0, p.getImprovementLevel(CityImprovement.TRADE));
  }

  @Test
  public void cannotExceedMaxLevel() {
    Player p = new HumanPlayer(0, "test", "#000000");
    // 1+2+3+4+5 = 15 cloth reaches the max level exactly.
    p.addCommodity(Commodity.CLOTH, 15);
    for (int i = 0; i < CityImprovement.MAX_LEVEL; i++) {
      p.improveCity(CityImprovement.TRADE);
    }
    assertEquals(CityImprovement.MAX_LEVEL,
        p.getImprovementLevel(CityImprovement.TRADE));
    assertFalse(p.canImproveCity(CityImprovement.TRADE));
    // Extra cloth cannot push past the cap.
    p.addCommodity(Commodity.CLOTH, 6);
    assertFalse(p.canImproveCity(CityImprovement.TRADE));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void readOnlyPlayerCannotImprove() {
    Player p = new HumanPlayer(0, "test", "#000000").getImmutableCopy();
    p.improveCity(CityImprovement.TRADE);
  }
}
