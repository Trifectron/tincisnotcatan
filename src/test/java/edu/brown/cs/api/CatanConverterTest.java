package edu.brown.cs.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCardType;
import edu.brown.cs.catan.Resource;

public class CatanConverterTest {

  private static MasterReferee cnk(int numPlayers) {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", numPlayers);
    return new MasterReferee(new GameSettings(json));
  }

  @Test
  public void tileRawExposesMerchantOwner() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");

    Tile target = null;
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.getType().getType() != null) {
        target = t;
        break;
      }
    }
    assertNotNull("Standard board should have a resource tile", target);
    assertEquals(-1, target.getMerchantOwner());

    target.setMerchantOwner(p0);

    CatanConverter converter = new CatanConverter();
    JsonObject state = converter.getGameState(ref, p0);
    JsonArray tiles = state.getAsJsonObject("board")
        .getAsJsonArray("tiles");
    boolean found = false;
    int unsetCount = 0;
    for (int i = 0; i < tiles.size(); i++) {
      JsonElement el = tiles.get(i);
      int owner = ((Number) el.getAsJsonObject().get("merchantOwner")
          .getAsLong()).intValue();
      if (owner == -1) {
        unsetCount++;
      }
      if (owner == p0) {
        found = true;
      }
    }
    assertTrue("Expected a tile JSON with merchantOwner == " + p0, found);
    assertTrue("Expected every other tile to still report merchantOwner == -1",
        unsetCount >= 17);
  }

  @Test
  public void gameStateExposesMerchantFleetResourceWhenSet() {
    MasterReferee ref = cnk(2);
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player current = ref.currentPlayer();
    assertNull(ref.getTurn().getMerchantFleetResource());

    JsonObject before = new CatanConverter().getGameState(ref, current.getID());
    // When unplayed the resource is null in Java, which Gson serializes as
    // JSON null. Accept either: the key absent or explicitly null.
    if (before.has("merchantFleetResource")) {
      assertTrue("Expected merchantFleetResource to be JSON-null when unplayed",
          before.get("merchantFleetResource").isJsonNull());
    }

    ProgressCardType.MERCHANT_FLEET.play(ref, current, "ore");
    assertEquals(Resource.ORE, ref.getTurn().getMerchantFleetResource());

    JsonObject after = new CatanConverter().getGameState(ref, current.getID());
    // Gson serializes Resource enums by their name() (uppercase), which is
    // the convention already used elsewhere in the game-state JSON (e.g.
    // the rates map's keys).
    assertEquals("ORE",
        after.get("merchantFleetResource").getAsString());
  }
}
