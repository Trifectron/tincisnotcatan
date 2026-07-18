package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.board.City;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;

public class ProgressCardTest {

  @Test
  public void cardMetadata() {
    assertEquals(CityImprovement.SCIENCE, ProgressCardType.PRINTER.getDeck());
    assertEquals(CityImprovement.POLITICS,
        ProgressCardType.CONSTITUTION.getDeck());
    assertEquals(ProgressCardType.PRINTER,
        ProgressCardType.fromString("printer"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void unknownCardNameThrows() {
    ProgressCardType.fromString("Sorcery");
  }

  @Test
  public void deckHoldsOnlyItsTrackAndDrainsToEmpty() {
    ProgressCardDeck science = new ProgressCardDeck(CityImprovement.SCIENCE);
    assertEquals(3, science.size());
    Set<ProgressCardType> drawn = new HashSet<>();
    while (!science.isEmpty()) {
      drawn.add(science.draw());
    }
    assertEquals(EnumSet.of(ProgressCardType.PRINTER,
        ProgressCardType.IRRIGATION, ProgressCardType.ENGINEER), drawn);
    assertNull(science.draw());

    // A track with no wired cards yields an empty deck, not an error.
    assertTrue(new ProgressCardDeck(CityImprovement.TRADE).isEmpty());
  }

  @Test
  public void playerHoldsAndPlaysCards() {
    Player p = new HumanPlayer(0, "test", "#000000");
    assertTrue(p.getProgressCards().isEmpty());

    p.addProgressCard(ProgressCardType.PRINTER);
    assertEquals(1, p.getProgressCards().size());
    assertTrue(p.getProgressCards().contains(ProgressCardType.PRINTER));

    int before = p.numVictoryPoints();
    // The wired effects do not use the referee, so null is safe here.
    ProgressCardType.PRINTER.play(null, p);
    assertEquals(before + 1, p.numVictoryPoints());

    assertTrue(p.removeProgressCard(ProgressCardType.PRINTER));
    assertTrue(p.getProgressCards().isEmpty());
    assertFalse(p.removeProgressCard(ProgressCardType.PRINTER));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void readOnlyPlayerCannotAddCards() {
    Player p = new HumanPlayer(0, "test", "#000000").getImmutableCopy();
    p.addProgressCard(ProgressCardType.PRINTER);
  }

  private static MasterReferee cnkReferee() {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", 2);
    return new MasterReferee(new GameSettings(json));
  }

  @Test
  public void irrigationPaysWheatPerAdjacentBuilding() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player p = ref.getPlayerByID(p0);

    Tile wheatTile = null;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getType() == TileType.WHEAT) {
        wheatTile = tile;
        break;
      }
    }
    assertTrue("Standard board should have a wheat hex", wheatTile != null);
    Intersection settlementInt = wheatTile.getIntersections().iterator().next();
    settlementInt.placeSettlement(p);

    // A settlement pays 2 wheat per adjacent wheat hex; on the generated
    // board it may border more than just the one hex picked above.
    int expectedWheat = 0;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getType() == TileType.WHEAT
          && tile.getIntersections().contains(settlementInt)) {
        expectedWheat += 2;
      }
    }

    double before = p.getResources().get(Resource.WHEAT);
    String msg = ProgressCardType.IRRIGATION.play(ref, p);
    assertEquals(before + expectedWheat, p.getResources().get(Resource.WHEAT),
        0.0001);
    assertTrue(msg.contains("wheat"));
  }

  @Test
  public void irrigationNoOpWithoutAdjacentBuilding() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player p = ref.getPlayerByID(p0);

    double before = p.getResources().get(Resource.WHEAT);
    String msg = ProgressCardType.IRRIGATION.play(ref, p);
    assertEquals(before, p.getResources().get(Resource.WHEAT), 0.0001);
    assertTrue(msg.contains("no settlements"));
  }

  @Test
  public void engineerBuildsFreeWallOnFirstUnwalledCity() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player p = ref.getPlayerByID(p0);

    Intersection cityInt = ref.getBoard().getIntersections().values()
        .iterator().next();
    cityInt.placeSettlement(p);
    cityInt.placeCity(p);
    assertFalse(((City) cityInt.getBuilding()).hasWall());

    String msg = ProgressCardType.ENGINEER.play(ref, p);
    assertTrue(((City) cityInt.getBuilding()).hasWall());
    assertTrue(msg.contains("free city wall"));
  }

  @Test
  public void engineerNoOpWithoutUnwalledCity() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player p = ref.getPlayerByID(p0);

    String msg = ProgressCardType.ENGINEER.play(ref, p);
    assertTrue(msg.contains("no unwalled city"));
  }
}
