package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonObject;

import org.junit.Test;

public class ProgressDeckDrawTest {

  private static MasterReferee citiesAndKnightsReferee() {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    return new MasterReferee(new GameSettings(json));
  }

  @Test
  public void citiesAndKnightsGameDrawsFromTrackDecks() {
    Referee ref = citiesAndKnightsReferee();
    // The Science deck holds only Printer so far; it draws once then empties.
    assertEquals(ProgressCardType.PRINTER,
        ref.drawProgressCard(CityImprovement.SCIENCE));
    assertNull(ref.drawProgressCard(CityImprovement.SCIENCE));
    // Politics deck holds Constitution.
    assertEquals(ProgressCardType.CONSTITUTION,
        ref.drawProgressCard(CityImprovement.POLITICS));
    // Trade has no wired cards yet: an empty deck, not an error.
    assertNull(ref.drawProgressCard(CityImprovement.TRADE));
  }

  @Test
  public void baseGameHasNoProgressDecks() {
    Referee ref = new MasterReferee(new GameSettings());
    for (CityImprovement track : CityImprovement.values()) {
      assertNull(ref.drawProgressCard(track));
    }
  }
}
