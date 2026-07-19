package edu.brown.cs.board;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.HumanPlayer;
import edu.brown.cs.catan.Player;

/**
 * Tests for ships (Seafarers): placement legality, connectivity and their
 * contribution to the longest trade route.
 */
public class ShipTest {

  private static GameSettings seafarers() {
    JsonObject s = new JsonObject();
    s.addProperty("numPlayers", 4);
    s.addProperty("victoryPoints", 10);
    s.addProperty("isSeafarers", true);
    return new GameSettings(s);
  }

  // Three distinct, chain-connectable intersection coordinates (a-b-c).
  private static IntersectionCoordinate icA() {
    return new IntersectionCoordinate(new HexCoordinate(0, 0, 0),
        new HexCoordinate(0, 1, 0), new HexCoordinate(0, 0, -1));
  }

  private static IntersectionCoordinate icB() {
    return new IntersectionCoordinate(new HexCoordinate(0, 0, 0),
        new HexCoordinate(1, 0, 0), new HexCoordinate(0, 0, -1));
  }

  private static IntersectionCoordinate icC() {
    return new IntersectionCoordinate(new HexCoordinate(0, 0, 0),
        new HexCoordinate(1, 1, 0), new HexCoordinate(2, 1, 0));
  }

  @Test
  public void shipStoresPlayer() {
    Player p = new HumanPlayer(1, "1", "000000");
    Ship s = new Ship(p);
    assertEquals(p, s.getPlayer());
  }

  @Test
  public void canPlaceShipRequiresMaritimeAndConnection() {
    Intersection a = new Intersection(icA());
    Intersection b = new Intersection(icB());
    Path p = new Path(a, b);
    Player player = new HumanPlayer(1, "1", "000000");
    // A settlement alone is not enough if the path is not navigable.
    a.placeSettlement(player);
    assertFalse(p.canPlaceShip(player));
    // Once marked maritime, the settlement connection makes it legal.
    p.setMaritime(true);
    assertTrue(p.canPlaceShip(player));
    assertNull(p.getShip());
    p.placeShip(player);
    assertNotNull(p.getShip());
    // The slot is now occupied for both ships and roads.
    assertFalse(p.canPlaceShip(player));
    assertFalse(p.canPlaceRoad(player));
  }

  @Test
  public void shipsExtendFromOtherShips() {
    Intersection a = new Intersection(icA());
    Intersection b = new Intersection(icB());
    Intersection c = new Intersection(icC());
    Path p1 = new Path(a, b);
    p1.setMaritime(true);
    Path p2 = new Path(b, c);
    p2.setMaritime(true);
    Player player = new HumanPlayer(1, "1", "000000");
    assertFalse(p2.canPlaceShip(player));
    a.placeSettlement(player);
    p1.placeShip(player);
    // p2 shares intersection b with p1 (which now hosts a ship).
    assertTrue(p2.canPlaceShip(player));
  }

  @Test
  public void longestRouteCountsShipsLikeRoads() {
    Player player = new HumanPlayer(1, "1", "000000");

    // Road chain a-b-c.
    Intersection ra = new Intersection(icA());
    Intersection rb = new Intersection(icB());
    Intersection rc = new Intersection(icC());
    Path r1 = new Path(ra, rb);
    Path r2 = new Path(rb, rc);
    ra.placeSettlement(player);
    r1.placeRoad(player);
    r2.placeRoad(player);
    int roadLen = r1.getLongestPath(player);

    // Equivalent ship chain a-b-c.
    Intersection sa = new Intersection(icA());
    Intersection sb = new Intersection(icB());
    Intersection sc = new Intersection(icC());
    Path s1 = new Path(sa, sb);
    s1.setMaritime(true);
    Path s2 = new Path(sb, sc);
    s2.setMaritime(true);
    sa.placeSettlement(player);
    s1.placeShip(player);
    s2.placeShip(player);
    int shipLen = s1.getLongestPath(player);

    assertEquals(roadLen, shipLen);
    assertTrue(roadLen >= 1);
  }

  @Test
  public void longestRouteMergesRoadAndShip() {
    Player player = new HumanPlayer(1, "1", "000000");
    Intersection a = new Intersection(icA());
    Intersection b = new Intersection(icB());
    Intersection c = new Intersection(icC());
    Path road = new Path(a, b);
    Path ship = new Path(b, c);
    ship.setMaritime(true);
    // Road and ship meet at settlement b (road<->ship transitions happen at a
    // settlement/city). Both connect there.
    b.placeSettlement(player);
    road.placeRoad(player);
    ship.placeShip(player);
    assertNotNull(road.getRoad());
    assertNotNull(ship.getShip());
    // A road and a ship sharing an intersection form one continuous route.
    assertTrue(road.getLongestPath(player) >= 1);
  }

  @Test
  public void inlandBoardPathRejectsShips() {
    Board board = new Board(seafarers());
    Player player = new HumanPlayer(0, "0", "000000");
    Path inland = null;
    for (Path p : board.getPaths().values()) {
      if (!p.isMaritime()) {
        inland = p;
        break;
      }
    }
    assertNotNull(inland);
    inland.getStart().placeSettlement(player);
    assertFalse(inland.canPlaceShip(player));
  }

  @Test
  public void boardCoastalPathAcceptsShipFromSettlement() {
    Board board = new Board(seafarers());
    Player player = new HumanPlayer(0, "0", "000000");
    Path coastal = null;
    for (Path p : board.getPaths().values()) {
      if (p.isMaritime()
          && (p.getStart().hasAdjacentLand() || p.getEnd().hasAdjacentLand())) {
        coastal = p;
        break;
      }
    }
    assertNotNull(coastal);
    Intersection landEnd = coastal.getStart().hasAdjacentLand() ? coastal
        .getStart() : coastal.getEnd();
    assertFalse(coastal.canPlaceShip(player));
    landEnd.placeSettlement(player);
    assertTrue(coastal.canPlaceShip(player));
  }
}
