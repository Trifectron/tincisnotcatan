package edu.brown.cs.board;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.catan.GameSettings;

public class SeafarersBoardTest {

  private static GameSettings seafarersSettings() {
    JsonObject json = new JsonObject();
    json.addProperty("isSeafarers", true);
    return new GameSettings(json);
  }

  @Test
  public void settingsFlagParses() {
    assertTrue(seafarersSettings().isSeafarers);
    assertTrue(!new GameSettings().isSeafarers);
  }

  @Test
  public void islandLayoutHasWaterAndGold() {
    Board b = new Board(seafarersSettings());
    boolean hasWater = false;
    boolean hasGold = false;
    for (Tile t : b.getTiles()) {
      if (t.getType() == TileType.WATER) {
        hasWater = true;
        // In-play water hexes produce nothing and never hold the robber.
        assertTrue(t.getRollNumber() == 0);
        assertTrue(!t.hasRobber());
      }
      if (t.getType() == TileType.GOLD) {
        hasGold = true;
      }
    }
    assertTrue(hasWater);
    assertTrue(hasGold);
  }

  @Test
  public void robberStillHasAHome() {
    // The desert in the island interior hosts the robber at game start.
    assertNotNull(new Board(seafarersSettings()).findRobber());
  }
}
