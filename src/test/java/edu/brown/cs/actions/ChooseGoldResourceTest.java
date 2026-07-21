package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.board.Board;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

public class ChooseGoldResourceTest {

  private static Referee seafarersRef() {
    JsonObject s = new JsonObject();
    s.addProperty("numPlayers", 4);
    s.addProperty("victoryPoints", 10);
    s.addProperty("isSeafarers", true);
    return new MasterReferee(new GameSettings(s));
  }

  private static Tile goldTile(Board board) {
    for (Tile t : board.getTiles()) {
      if (t.getType() == TileType.GOLD) {
        return t;
      }
    }
    return null;
  }

  private static JsonObject resources(double wood, double brick, double sheep,
      double ore, double wheat) {
    JsonObject r = new JsonObject();
    r.addProperty("wood", wood);
    r.addProperty("brick", brick);
    r.addProperty("sheep", sheep);
    r.addProperty("ore", ore);
    r.addProperty("wheat", wheat);
    JsonObject set = new JsonObject();
    set.add("resources", r);
    return set;
  }

  @Test
  public void settlementGoldProducesOnePip() {
    Board board = new Board(new GameSettings(seafarersSettings()));
    Tile gold = goldTile(board);
    assertNotNull(gold);
    Player p = new edu.brown.cs.catan.HumanPlayer(0, "0", "000000");
    Intersection corner = gold.getIntersections().iterator().next();
    corner.placeSettlement(p);
    Map<Integer, Integer> production = gold.goldProduction();
    assertEquals(Integer.valueOf(1), production.get(0));
  }

  @Test
  public void cityGoldProducesTwoPips() {
    Board board = new Board(new GameSettings(seafarersSettings()));
    Tile gold = goldTile(board);
    Player p = new edu.brown.cs.catan.HumanPlayer(0, "0", "000000");
    Intersection corner = gold.getIntersections().iterator().next();
    corner.placeSettlement(p);
    corner.placeCity(p);
    Map<Integer, Integer> production = gold.goldProduction();
    assertEquals(Integer.valueOf(2), production.get(0));
  }

  @Test
  public void testNotSetUp() {
    try {
      FollowUpAction gold = new ChooseGoldResource(0, 1.0);
      gold.execute();
      assertTrue(false);
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testWrongCountRejected() {
    Referee ref = seafarersRef();
    ref.addPlayer("Sean", "Red");
    // numToChoose is 1 but the player tries to take 2.
    FollowUpAction gold = new ChooseGoldResource(0, 1.0);
    gold.setupAction(ref, 0, resources(0, 0, 0, 2, 0));
    Map<Integer, ActionResponse> resp = gold.execute();
    assertTrue(resp.get(0).getSuccess() == false);
  }

  @Test
  public void testChoosingResourceAddsFromBank() {
    Referee ref = seafarersRef();
    Player p = ref.getPlayerByID(ref.addPlayer("Sean", "Red"));
    FollowUpAction gold = new ChooseGoldResource(0, 1.0);
    Collection<FollowUpAction> follow = new ArrayList<>();
    follow.add(gold);
    ref.addFollowUp(follow);
    gold.setupAction(ref, 0, resources(0, 0, 0, 1, 0));
    Map<Integer, ActionResponse> resp = gold.execute();
    assertTrue(resp.get(0).getSuccess());
    assertTrue(p.getResources().get(Resource.ORE) == 1.0);
  }

  private static JsonObject seafarersSettings() {
    JsonObject s = new JsonObject();
    s.addProperty("numPlayers", 4);
    s.addProperty("victoryPoints", 10);
    s.addProperty("isSeafarers", true);
    return s;
  }
}
