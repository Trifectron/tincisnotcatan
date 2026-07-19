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
  public void cityWallAbsorbsADowngradeHit() {
    IntersectionCoordinate coord = new IntersectionCoordinate(
        new HexCoordinate(0, 0, 0), new HexCoordinate(1, 0, 0),
        new HexCoordinate(1, 1, 0));
    Intersection i = new Intersection(coord);
    i.placeSettlement(player);
    i.placeCity(player);
    ((City) i.getBuilding()).buildWall();
    assertTrue(((City) i.getBuilding()).hasWall());

    // Barbarian downgrade: a walled city absorbs the hit by losing only its
    // wall; the city itself remains.
    boolean downgraded = i.downgradeCity();
    assertFalse(downgraded);
    assertTrue(i.getBuilding() instanceof City);
    assertFalse(((City) i.getBuilding()).hasWall());
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
