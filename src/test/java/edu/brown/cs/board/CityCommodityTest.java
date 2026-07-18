package edu.brown.cs.board;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.HumanPlayer;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

public class CityCommodityTest {

  private static final int PID = 0;
  private final Player player = new HumanPlayer(PID, "test", "#000000");

  @Test
  public void cityBaseProductionUnchanged() {
    City city = new City(player);
    // No-arg and non-C&K both yield the base 2 resources.
    assertEquals(Integer.valueOf(2),
        city.collectResource(Resource.ORE).get(PID).get(Resource.ORE));
    assertEquals(Integer.valueOf(2),
        city.collectResource(Resource.ORE, false).get(PID).get(Resource.ORE));
  }

  @Test
  public void cityCommodityHexYieldsOneResourcePlusCommodity() {
    City city = new City(player);
    // Ore hex under C&K: 1 ore + 1 coin.
    assertEquals(Integer.valueOf(1),
        city.collectResource(Resource.ORE, true).get(PID).get(Resource.ORE));
    assertEquals(Integer.valueOf(1),
        city.collectCommodity(Resource.ORE).get(PID).get(Commodity.COIN));
  }

  @Test
  public void cityNonCommodityHexStillYieldsTwo() {
    City city = new City(player);
    // Brick hex under C&K: still 2 brick, no commodity.
    assertEquals(Integer.valueOf(2),
        city.collectResource(Resource.BRICK, true).get(PID).get(Resource.BRICK));
    assertTrue(city.collectCommodity(Resource.BRICK).isEmpty());
  }

  @Test
  public void settlementsProduceNoCommodities() {
    Settlement settlement = new Settlement(player);
    // Settlement yields 1 resource even under C&K, and never a commodity.
    assertEquals(Integer.valueOf(1),
        settlement.collectResource(Resource.ORE, true).get(PID)
            .get(Resource.ORE));
    assertTrue(settlement.collectCommodity(Resource.ORE).isEmpty());
  }
}
