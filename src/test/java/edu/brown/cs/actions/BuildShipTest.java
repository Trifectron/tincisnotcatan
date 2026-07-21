package edu.brown.cs.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.board.Board;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Path;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

public class BuildShipTest {

  private static Referee seafarersRef() {
    JsonObject s = new JsonObject();
    s.addProperty("numPlayers", 4);
    s.addProperty("victoryPoints", 10);
    s.addProperty("isSeafarers", true);
    return new MasterReferee(new GameSettings(s));
  }

  private static Path coastalPath(Board board) {
    for (Path p : board.getPaths().values()) {
      if (p.isMaritime()
          && (p.getStart().hasAdjacentLand() || p.getEnd().hasAdjacentLand())) {
        return p;
      }
    }
    return null;
  }

  private static Path inlandPath(Board board) {
    for (Path p : board.getPaths().values()) {
      if (!p.isMaritime()) {
        return p;
      }
    }
    return null;
  }

  private static Intersection landEndpoint(Path p) {
    return p.getStart().hasAdjacentLand() ? p.getStart() : p.getEnd();
  }

  @Test
  public void testBuildBasicShip() {
    Referee ref = seafarersRef();
    ref.addPlayer("Sean", "Red");
    Player p = ref.getPlayerByID(0);
    Board board = ref.getBoard();
    Path coastal = coastalPath(board);
    assertNotNull(coastal);
    landEndpoint(coastal).placeSettlement(p);
    p.addResource(Resource.WOOD);
    p.addResource(Resource.SHEEP);
    IntersectionCoordinate start = coastal.getStart().getPosition();
    IntersectionCoordinate end = coastal.getEnd().getPosition();
    Map<Integer, ActionResponse> resp = new BuildShip(ref, 0, start, end, true)
        .execute();
    assertTrue(resp.get(0).getSuccess());
    assertNotNull(coastal.getShip());
    assertTrue(p.getResources().get(Resource.WOOD) == 0);
    assertTrue(p.getResources().get(Resource.SHEEP) == 0);
  }

  @Test
  public void testCannotAffordShip() {
    Referee ref = seafarersRef();
    ref.addPlayer("Sean", "Red");
    Player p = ref.getPlayerByID(0);
    Board board = ref.getBoard();
    Path coastal = coastalPath(board);
    landEndpoint(coastal).placeSettlement(p);
    // No resources given.
    IntersectionCoordinate start = coastal.getStart().getPosition();
    IntersectionCoordinate end = coastal.getEnd().getPosition();
    Map<Integer, ActionResponse> resp = new BuildShip(ref, 0, start, end, true)
        .execute();
    assertFalse(resp.get(0).getSuccess());
    assertNull(coastal.getShip());
  }

  @Test
  public void testCannotBuildShipOnInlandPath() {
    Referee ref = seafarersRef();
    ref.addPlayer("Sean", "Red");
    Player p = ref.getPlayerByID(0);
    Board board = ref.getBoard();
    Path inland = inlandPath(board);
    assertNotNull(inland);
    inland.getStart().placeSettlement(p);
    p.addResource(Resource.WOOD);
    p.addResource(Resource.SHEEP);
    IntersectionCoordinate start = inland.getStart().getPosition();
    IntersectionCoordinate end = inland.getEnd().getPosition();
    Map<Integer, ActionResponse> resp = new BuildShip(ref, 0, start, end, true)
        .execute();
    assertFalse(resp.get(0).getSuccess());
    assertNull(inland.getShip());
  }

  @Test
  public void testCannotBuildDisconnectedShip() {
    Referee ref = seafarersRef();
    ref.addPlayer("Sean", "Red");
    Player p = ref.getPlayerByID(0);
    Board board = ref.getBoard();
    Path coastal = coastalPath(board);
    // No settlement or ship connected to this path.
    p.addResource(Resource.WOOD);
    p.addResource(Resource.SHEEP);
    IntersectionCoordinate start = coastal.getStart().getPosition();
    IntersectionCoordinate end = coastal.getEnd().getPosition();
    Map<Integer, ActionResponse> resp = new BuildShip(ref, 0, start, end, true)
        .execute();
    assertFalse(resp.get(0).getSuccess());
    assertNull(coastal.getShip());
  }

  @Test
  public void testNoShipsLeft() {
    Referee ref = seafarersRef();
    ref.addPlayer("Sean", "Red");
    Player p = ref.getPlayerByID(0);
    Board board = ref.getBoard();
    Path coastal = coastalPath(board);
    landEndpoint(coastal).placeSettlement(p);
    p.addResource(Resource.WOOD);
    p.addResource(Resource.SHEEP);
    for (int i = 0; i < edu.brown.cs.catan.Settings.INITIAL_SHIPS; i++) {
      p.useShip();
    }
    IntersectionCoordinate start = coastal.getStart().getPosition();
    IntersectionCoordinate end = coastal.getEnd().getPosition();
    Map<Integer, ActionResponse> resp = new BuildShip(ref, 0, start, end, true)
        .execute();
    assertFalse(resp.get(0).getSuccess());
  }

  @Test
  public void testBadPath() {
    Referee ref = seafarersRef();
    ref.addPlayer("Sean", "Red");
    IntersectionCoordinate bad = new IntersectionCoordinate(new HexCoordinate(
        50, 50, 50), new HexCoordinate(51, 50, 50), new HexCoordinate(50, 51,
        50));
    try {
      new BuildShip(ref, 0, bad, bad, true);
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
  }
}
