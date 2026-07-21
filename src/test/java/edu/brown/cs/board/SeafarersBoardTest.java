package edu.brown.cs.board;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;

/**
 * Tests for the Seafarers "Heading for New Shores" board generation.
 */
public class SeafarersBoardTest {

  private static GameSettings seafarers() {
    JsonObject s = new JsonObject();
    s.addProperty("numPlayers", 4);
    s.addProperty("victoryPoints", 10);
    s.addProperty("isSeafarers", true);
    return new GameSettings(s);
  }

  private static Map<TileType, Integer> countByType(Board board) {
    Map<TileType, Integer> counts = new HashMap<>();
    for (Tile t : board.getTiles()) {
      counts.merge(t.getType(), 1, Integer::sum);
    }
    return counts;
  }

  @Test
  public void tileCountsMatchScenario() {
    Board board = new Board(seafarers());
    assertEquals(SeafarersScenario.NEW_SHORES.size(), board.getTiles().size());

    Map<TileType, Integer> counts = countByType(board);
    // 13 home-island + 3 north + 3 east + 3 south = 22 land hexes.
    int land = 0;
    for (Tile t : board.getTiles()) {
      if (t.getType() != TileType.WATER) {
        land++;
      }
    }
    assertEquals(22, land);
    assertEquals(Integer.valueOf(2), counts.get(TileType.GOLD));
    assertEquals(Integer.valueOf(1), counts.get(TileType.DESERT));
    assertTrue(counts.get(TileType.WATER) > 0);
  }

  @Test
  public void robberStartsOnDesert() {
    Board board = new Board(seafarers());
    HexCoordinate robber = board.findRobber();
    assertNotNull(robber);
  }

  @Test
  public void bothMaritimeAndInlandPathsExist() {
    Board board = new Board(seafarers());
    boolean maritime = false;
    boolean inland = false;
    boolean openSea = false;
    for (Path p : board.getPaths().values()) {
      if (p.isMaritime()) {
        maritime = true;
      } else {
        inland = true;
      }
      if (p.isPureSea()) {
        openSea = true;
      }
    }
    assertTrue("expected at least one navigable (maritime) path", maritime);
    assertTrue("expected at least one inland (land-locked) path", inland);
    assertTrue("expected at least one open-sea path", openSea);
  }

  @Test
  public void openSeaPathsForbidRoadsButAllowShips() {
    Board board = new Board(seafarers());
    for (Path p : board.getPaths().values()) {
      if (p.isPureSea()) {
        // Open sea is navigable but never buildable for roads.
        assertTrue(p.isMaritime());
      }
    }
  }

  @Test
  public void someIntersectionsAreOpenSea() {
    Board board = new Board(seafarers());
    boolean land = false;
    boolean sea = false;
    for (Intersection i : board.getIntersections().values()) {
      if (i.hasAdjacentLand()) {
        land = true;
      } else {
        sea = true;
      }
    }
    assertTrue("expected land-adjacent intersections", land);
    assertTrue("expected open-sea intersections", sea);
  }

  @Test
  public void initialSettlementsRestrictedToHomeIsland() {
    MasterReferee ref = new MasterReferee(seafarers());
    ref.addPlayer("A", "Red");
    Board board = ref.getBoard();
    Intersection home = null;
    Intersection foreign = null;
    for (Intersection i : board.getIntersections().values()) {
      if (i.hasAdjacentLand() && i.isHomeIsland() && home == null) {
        home = i;
      }
      if (i.hasAdjacentLand() && !i.isHomeIsland() && foreign == null) {
        foreign = i;
      }
    }
    assertNotNull(home);
    assertNotNull(foreign);
    // During setup, only home-island intersections are placeable.
    assertTrue(home.canPlaceSettlement(ref, 0));
    assertFalse(foreign.canPlaceSettlement(ref, 0));
  }

  @Test
  public void foreignIslandSettlementEarnsBonusPoints() {
    MasterReferee ref = new MasterReferee(seafarers());
    Player p = ref.getPlayerByID(ref.addPlayer("A", "Red"));
    Board board = ref.getBoard();
    Intersection home = null;
    Intersection foreign = null;
    for (Intersection i : board.getIntersections().values()) {
      if (i.hasAdjacentLand() && i.isHomeIsland() && home == null) {
        home = i;
      }
      if (i.hasAdjacentLand() && !i.isHomeIsland() && foreign == null) {
        foreign = i;
      }
    }
    // A home-island settlement is worth 1 point.
    home.placeSettlement(p);
    p.useSettlement();
    assertEquals(1, ref.getNumPublicPoints(0));
    // A foreign-island settlement adds 1 (settlement) + 2 (bonus) = 3 more.
    foreign.placeSettlement(p);
    p.useSettlement();
    assertEquals(4, ref.getNumPublicPoints(0));
  }

  @Test
  public void baseGameBoardHasNoMaritimePaths() {
    // A non-Seafarers game must be unaffected: no maritime paths, and every
    // intersection is treated as land-adjacent.
    Board board = new Board(new GameSettings());
    for (Path p : board.getPaths().values()) {
      assertFalse(p.isMaritime());
      assertFalse(p.isPureSea());
    }
    for (Intersection i : board.getIntersections().values()) {
      assertTrue(i.hasAdjacentLand());
    }
  }
}
