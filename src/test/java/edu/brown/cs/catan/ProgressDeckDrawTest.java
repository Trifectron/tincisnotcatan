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
    // The Science deck holds 2 copies each of Printer, Irrigation,
    // Engineer, Inventor, Alchemist, Crane, Medicine, Mining, Smith, and
    // Road Building (20 cards total).
    Set<ProgressCardType> drawnScience = new HashSet<>();
    for (int i = 0; i < 20; i++) {
      drawnScience.add(ref.drawProgressCard(CityImprovement.SCIENCE));
    }
    assertEquals(EnumSet.of(ProgressCardType.PRINTER,
        ProgressCardType.IRRIGATION, ProgressCardType.ENGINEER,
        ProgressCardType.INVENTOR, ProgressCardType.ALCHEMIST,
        ProgressCardType.CRANE, ProgressCardType.MEDICINE,
        ProgressCardType.MINING, ProgressCardType.SMITH,
        ProgressCardType.ROAD_BUILDING),
        drawnScience);
    assertNull(ref.drawProgressCard(CityImprovement.SCIENCE));
    // The Politics deck holds 3 copies of Spy and 2 copies each of
    // Constitution, Intrigue, Wedding, Bishop, Diplomat, Deserter,
    // Saboteur, and Warlord (19 cards total), per the official rulebook.
    Set<ProgressCardType> drawnPolitics = new HashSet<>();
    for (int i = 0; i < 19; i++) {
      drawnPolitics.add(ref.drawProgressCard(CityImprovement.POLITICS));
    }
    assertEquals(EnumSet.of(ProgressCardType.CONSTITUTION,
        ProgressCardType.INTRIGUE, ProgressCardType.WEDDING,
        ProgressCardType.BISHOP, ProgressCardType.DIPLOMAT,
        ProgressCardType.DESERTER, ProgressCardType.SABOTEUR,
        ProgressCardType.SPY, ProgressCardType.WARLORD), drawnPolitics);
    assertNull(ref.drawProgressCard(CityImprovement.POLITICS));
    // The Trade deck holds 2 copies each of Master Merchant, Resource
    // Monopoly, Trade Monopoly, Merchant Fleet, and Merchant (10 cards
    // total).
    Set<ProgressCardType> drawnTrade = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      drawnTrade.add(ref.drawProgressCard(CityImprovement.TRADE));
    }
    assertEquals(EnumSet.of(ProgressCardType.MASTER_MERCHANT,
        ProgressCardType.RESOURCE_MONOPOLY, ProgressCardType.TRADE_MONOPOLY,
        ProgressCardType.MERCHANT_FLEET, ProgressCardType.MERCHANT),
        drawnTrade);
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
