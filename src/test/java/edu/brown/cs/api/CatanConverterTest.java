package edu.brown.cs.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.actions.Action;
    import java.util.Map;
import edu.brown.cs.actions.ActionResponse;

import edu.brown.cs.actions.PlayProgressCard;
import edu.brown.cs.board.City;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Knight;
import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.Commodity;
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
    if (before.has("merchantFleetResource")) {
      assertTrue("Expected merchantFleetResource to be JSON-null when unplayed",
          before.get("merchantFleetResource").isJsonNull());
    }

    ProgressCardType.MERCHANT_FLEET.play(ref, current, "ore");
    assertEquals(Resource.ORE, ref.getTurn().getMerchantFleetResource());

    JsonObject after = new CatanConverter().getGameState(ref, current.getID());
    // Gson serializes Resource enums by their name() (uppercase), matching
    // the convention used elsewhere in the game-state JSON (e.g. rates).
    assertEquals("ORE",
        after.get("merchantFleetResource").getAsString());
  }

  @Test
  public void handExposesCommodities() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    pa.addCommodity(Commodity.PAPER, 3);

    JsonObject state = new CatanConverter().getGameState(ref, p0);
    JsonObject commodities = state.getAsJsonObject("hand")
        .getAsJsonObject("commodities");
    boolean foundPaper = false;
    double total = 0;
    for (java.util.Map.Entry<String, JsonElement> entry : commodities
        .entrySet()) {
      total += entry.getValue().getAsDouble();
      if (entry.getKey().equalsIgnoreCase("paper")) {
        foundPaper = true;
      }
    }
    assertEquals(3.0, total, 0.0001);
    assertTrue("Expected PAPER commodity to appear in handshake", foundPaper);
  }

  @Test
  public void handExposesCityImprovements() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    // Advancing Trade costs 1 cloth at level 0; stock it so improveCity
    // actually runs.
    pa.addCommodity(Commodity.CLOTH, 1);
    pa.improveCity(CityImprovement.TRADE);
    assertEquals(1, pa.getImprovementLevel(CityImprovement.TRADE));

    JsonObject state = new CatanConverter().getGameState(ref, p0);
    JsonObject improvements = state.getAsJsonObject("hand")
        .getAsJsonObject("improvements");
    boolean foundTrade = false;
    int total = 0;
    for (java.util.Map.Entry<String, JsonElement> entry : improvements
        .entrySet()) {
      total += entry.getValue().getAsInt();
      if (entry.getKey().equalsIgnoreCase("trade")) {
        foundTrade = true;
      }
    }
    assertEquals(1, total);
    assertTrue("Expected TRADE key in improvements", foundTrade);
  }

  @Test
  public void handExposesProgressCards() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    pa.addProgressCard(ProgressCardType.PRINTER);

    JsonObject state = new CatanConverter().getGameState(ref, p0);
    JsonArray progressCards = state.getAsJsonObject("hand")
        .getAsJsonArray("progressCards");
    assertEquals(1, progressCards.size());
    assertEquals("PRINTER", progressCards.get(0).getAsString());
  }

  @Test
  public void intersectionExposesKnightWithTierAndActiveState() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    // placeKnight(T) silently no-ops if the intersection already has a
    // building/knight, so pick one of the empty ones directly.
    Intersection knightAt = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() == null && i.getKnight() == null) {
        knightAt = i;
        break;
      }
    }
    assertNotNull(knightAt);
    knightAt.placeKnight(ref.getPlayerByID(p0));
    Knight knight = knightAt.getKnight();
    assertNotNull(knight);
    knight.activate();
    assertTrue(knight.isActive());
    assertEquals(1, knight.getTier());

    JsonObject state = new CatanConverter().getGameState(ref, p0);
    boolean found = false;
    JsonArray intersections = state.getAsJsonObject("board")
        .getAsJsonArray("intersections");
    for (int i = 0; i < intersections.size(); i++) {
      JsonElement el = intersections.get(i);
      if (el.getAsJsonObject().has("knight")
          && !el.getAsJsonObject().get("knight").isJsonNull()) {
        JsonObject k = el.getAsJsonObject().getAsJsonObject("knight");
        assertEquals(p0, k.get("player").getAsInt());
        assertEquals(1, k.get("tier").getAsInt());
        assertTrue(k.get("active").getAsBoolean());
        found = true;
      }
    }
    assertTrue("Expected at least one intersection with a non-null knight",
        found);

    knight.deactivate();
    JsonObject state2 = new CatanConverter().getGameState(ref, p0);
    for (int i = 0; i < intersections.size(); i++) {
      JsonObject obj = state2.getAsJsonObject("board")
          .getAsJsonArray("intersections").get(i).getAsJsonObject();
      if (obj.has("knight") && !obj.get("knight").isJsonNull()) {
        assertFalse(obj.getAsJsonObject("knight")
            .get("active").getAsBoolean());
      }
    }
  }

  @Test
  public void buildingExposesCityWall() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Intersection cityInt = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.canPlaceSettlement(ref, p0)) {
        cityInt = i;
        break;
      }
    }
    assertNotNull(cityInt);
    cityInt.placeSettlement(pa);
    cityInt.placeCity(pa);
    assertFalse(((City) cityInt.getBuilding()).hasWall());

    JsonObject state = new CatanConverter().getGameState(ref, p0);
    JsonObject building = state.getAsJsonObject("board")
        .getAsJsonArray("intersections").get(0).getAsJsonObject()
        .getAsJsonObject("building");
    assertEquals("city", building.get("type").getAsString());
    assertFalse(building.get("hasWall").getAsBoolean());

    ((City) cityInt.getBuilding()).buildWall();
    JsonObject state2 = new CatanConverter().getGameState(ref, p0);
    boolean anyTrue = false;
    JsonArray intersections = state2.getAsJsonObject("board")
        .getAsJsonArray("intersections");
    for (int i = 0; i < intersections.size(); i++) {
      JsonObject b = intersections.get(i).getAsJsonObject()
          .getAsJsonObject("building");
      if (b != null && b.has("hasWall")
          && b.get("hasWall").getAsBoolean()) {
        anyTrue = true;
      }
    }
    assertTrue(anyTrue);
  }

  @Test
  public void gameStateExposesBarbarianPosition() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");

    JsonObject state = new CatanConverter().getGameState(ref, p0);
    assertEquals(0, state.get("barbarianPosition").getAsInt());

    ref.getBarbarianTrack().advance();
    JsonObject state2 = new CatanConverter().getGameState(ref, p0);
    assertEquals(1, state2.get("barbarianPosition").getAsInt());
  }

  @Test
  public void gameStateExposesMetropolisOwnersUnderCnK() {
    MasterReferee ref = cnk(2);
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    JsonObject state = new CatanConverter().getGameState(ref, p0);
    JsonObject owners = state.getAsJsonObject("metropolisOwners");
    for (java.util.Map.Entry<String, JsonElement> entry : owners.entrySet()) {
      assertEquals(-1, entry.getValue().getAsInt());
    }

    // Reaching level 4 on the TRADE track requires 1+2+3+4 = 10 cloth, then
    // awardMetropolis grants the metropolis to the lone highest player.
    pa.addCommodity(Commodity.CLOTH, 10);
    for (int level = 0; level < 4; level++) {
      assertTrue(pa.canImproveCity(CityImprovement.TRADE));
      pa.improveCity(CityImprovement.TRADE);
    }
    assertEquals(4, pa.getImprovementLevel(CityImprovement.TRADE));
    ref.awardMetropolis(CityImprovement.TRADE);
    assertEquals(p0, ref.getMetropolisOwner(CityImprovement.TRADE));

    JsonObject state2 = new CatanConverter().getGameState(ref, p0);
    assertEquals(p0,
        state2.getAsJsonObject("metropolisOwners").get("TRADE").getAsInt());
  }

  @Test
  public void actionFactoryBuildsPlayProgressCardWithTarget()
      throws WaitingOnActionException {
    MasterReferee ref = cnk(2);
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player current = ref.currentPlayer();
    current.addProgressCard(ProgressCardType.RESOURCE_MONOPOLY);

    ActionFactory factory = new ActionFactory(ref);
    // ProgressCardType.fromString matches the card's display name
    // case-insensitively, so the wire value uses the canonical name.
    Action action = factory.createAction(
        "{action: playProgressCard, player: " + current.getID()
            + ", card: \"Resource Monopoly\", target: ore}");
    assertNotNull(action);
    assertTrue(action instanceof PlayProgressCard);

    Map<Integer, ActionResponse> responses = action.execute();
    assertNotNull(responses.get(current.getID()));
  }
}
