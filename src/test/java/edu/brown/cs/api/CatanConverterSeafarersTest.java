package edu.brown.cs.api;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Referee;

/**
 * Verifies the JSON contract the frontend relies on for a Seafarers game: water
 * and gold tiles serialize by type, and paths expose ship state and
 * ship-buildability / maritime flags.
 */
public class CatanConverterSeafarersTest {

  private static Referee seafarersRef() {
    JsonObject s = new JsonObject();
    s.addProperty("numPlayers", 2);
    s.addProperty("victoryPoints", 10);
    s.addProperty("isSeafarers", true);
    MasterReferee ref = new MasterReferee(new GameSettings(s));
    ref.addPlayer("P1", "Red");
    ref.addPlayer("P2", "Blue");
    return ref;
  }

  @Test
  public void gameStateContainsWaterAndGoldTiles() {
    Referee ref = seafarersRef();
    JsonObject state = new CatanConverter().getGameState(ref, 0);
    JsonArray tiles = state.getAsJsonObject("board").getAsJsonArray("tiles");

    boolean hasWater = false;
    boolean hasGold = false;
    for (int i = 0; i < tiles.size(); i++) {
      String type = tiles.get(i).getAsJsonObject().get("type").getAsString();
      if ("WATER".equals(type)) {
        hasWater = true;
      }
      if ("GOLD".equals(type)) {
        hasGold = true;
      }
    }
    assertTrue("game state should serialize water tiles", hasWater);
    assertTrue("game state should serialize gold tiles", hasGold);
  }

  @Test
  public void pathsExposeShipContract() {
    Referee ref = seafarersRef();
    JsonObject state = new CatanConverter().getGameState(ref, 0);
    JsonArray paths = state.getAsJsonObject("board").getAsJsonArray("paths");

    boolean anyMaritime = false;
    for (int i = 0; i < paths.size(); i++) {
      JsonObject p = paths.get(i).getAsJsonObject();
      // Every path must expose the ship-building fields the client reads.
      assertTrue("path missing canBuildShip", p.has("canBuildShip"));
      assertTrue("path missing isMaritime", p.has("isMaritime"));
      if (p.get("isMaritime").getAsBoolean()) {
        anyMaritime = true;
      }
    }
    assertTrue("expected at least one navigable path in the JSON", anyMaritime);
  }

  @Test
  public void baseGameJsonHasNoMaritimePaths() {
    // Regression: a base-game state must not report any maritime paths.
    MasterReferee ref = new MasterReferee(new GameSettings());
    ref.addPlayer("P1", "Red");
    JsonObject state = new CatanConverter().getGameState(ref, 0);
    JsonArray paths = state.getAsJsonObject("board").getAsJsonArray("paths");
    for (int i = 0; i < paths.size(); i++) {
      JsonObject p = paths.get(i).getAsJsonObject();
      assertTrue(p.has("isMaritime"));
      assertTrue("base game paths must not be maritime",
          !p.get("isMaritime").getAsBoolean());
    }
  }
}
