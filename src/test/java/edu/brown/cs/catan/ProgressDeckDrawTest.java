package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

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
    // The Science deck holds Printer, Irrigation, and Engineer.
    Set<ProgressCardType> drawnScience = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      drawnScience.add(ref.drawProgressCard(CityImprovement.SCIENCE));
    }
    assertEquals(EnumSet.of(ProgressCardType.PRINTER,
        ProgressCardType.IRRIGATION, ProgressCardType.ENGINEER), drawnScience);
    assertNull(ref.drawProgressCard(CityImprovement.SCIENCE));
    // The Politics deck holds Constitution, Intrigue, and Wedding.
    Set<ProgressCardType> drawnPolitics = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      drawnPolitics.add(ref.drawProgressCard(CityImprovement.POLITICS));
    }
    assertEquals(EnumSet.of(ProgressCardType.CONSTITUTION,
        ProgressCardType.INTRIGUE, ProgressCardType.WEDDING), drawnPolitics);
    assertNull(ref.drawProgressCard(CityImprovement.POLITICS));
    // Trade deck holds Master Merchant and Resource Monopoly.
    Set<ProgressCardType> drawnTrade = new HashSet<>();
    for (int i = 0; i < 2; i++) {
      drawnTrade.add(ref.drawProgressCard(CityImprovement.TRADE));
    }
    assertEquals(EnumSet.of(ProgressCardType.MASTER_MERCHANT,
        ProgressCardType.RESOURCE_MONOPOLY), drawnTrade);
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
