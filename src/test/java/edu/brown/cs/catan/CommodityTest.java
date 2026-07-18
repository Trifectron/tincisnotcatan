package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommodityTest {

  private static final double EPS = 1e-9;

  @Test
  public void stringRoundTrip() {
    for (Commodity c : Commodity.values()) {
      assertEquals(c, Commodity.stringToCommodity(c.toString()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void unknownStringThrows() {
    Commodity.stringToCommodity("gold");
  }

  @Test
  public void cityProductionMapping() {
    // Cities & Knights: ore->coin, sheep->cloth, wood->paper; others none.
    assertEquals(Commodity.COIN, Commodity.fromResource(Resource.ORE));
    assertEquals(Commodity.CLOTH, Commodity.fromResource(Resource.SHEEP));
    assertEquals(Commodity.PAPER, Commodity.fromResource(Resource.WOOD));
    assertNull(Commodity.fromResource(Resource.BRICK));
    assertNull(Commodity.fromResource(Resource.WHEAT));
  }

  @Test
  public void playerStartsWithNoCommodities() {
    Player p = new HumanPlayer(0, "test", "#000000");
    for (Commodity c : Commodity.values()) {
      assertEquals(0.0, p.getCommodities().get(c), EPS);
      assertFalse(p.hasCommodity(c, 1));
    }
  }

  @Test
  public void addAndRemoveCommodity() {
    Player p = new HumanPlayer(0, "test", "#000000");
    p.addCommodity(Commodity.COIN, 3);
    assertEquals(3.0, p.getCommodities().get(Commodity.COIN), EPS);
    assertTrue(p.hasCommodity(Commodity.COIN, 3));
    assertFalse(p.hasCommodity(Commodity.COIN, 4));

    p.removeCommodity(Commodity.COIN, 2);
    assertEquals(1.0, p.getCommodities().get(Commodity.COIN), EPS);
    // Other commodities untouched.
    assertEquals(0.0, p.getCommodities().get(Commodity.PAPER), EPS);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void readOnlyPlayerCannotMutateCommodities() {
    Player p = new HumanPlayer(0, "test", "#000000").getImmutableCopy();
    p.addCommodity(Commodity.COIN, 1);
  }
}
