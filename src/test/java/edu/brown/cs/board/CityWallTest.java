package edu.brown.cs.board;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.brown.cs.catan.HumanPlayer;
import edu.brown.cs.catan.Player;

public class CityWallTest {

  private final Player player = new HumanPlayer(0, "test", "#000000");

  @Test
  public void cityStartsWithoutWallAndCanGainOne() {
    City city = new City(player);
    assertFalse(city.hasWall());
    city.buildWall();
    assertTrue(city.hasWall());
  }

  @Test
  public void cityWallDoesNotProtectAgainstADowngrade() {
    IntersectionCoordinate coord = new IntersectionCoordinate(
        new HexCoordinate(0, 0, 0), new HexCoordinate(1, 0, 0),
        new HexCoordinate(1, 1, 0));
    Intersection i = new Intersection(coord);
    i.placeSettlement(player);
    i.placeCity(player);
    ((City) i.getBuilding()).buildWall();
    assertTrue(((City) i.getBuilding()).hasWall());

    // Official rule: a city wall does not protect against a barbarian
    // attack -- the wall is simply destroyed along with the city.
    boolean downgraded = i.downgradeCity();
    assertTrue(downgraded);
    assertTrue(i.getBuilding() instanceof Settlement);
  }

  @Test
  public void downgradeOnUnwalledCityReturnsTrue() {
    IntersectionCoordinate coord = new IntersectionCoordinate(
        new HexCoordinate(0, 0, 0), new HexCoordinate(1, 0, 0),
        new HexCoordinate(1, 1, 0));
    Intersection i = new Intersection(coord);
    i.placeSettlement(player);
    i.placeCity(player);

    boolean downgraded = i.downgradeCity();
    assertTrue(downgraded);
    assertTrue(i.getBuilding() instanceof Settlement);
  }
}
