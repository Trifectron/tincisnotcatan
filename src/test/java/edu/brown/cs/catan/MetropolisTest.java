package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;

import org.junit.Test;

public class MetropolisTest {

  private static MasterReferee cnkReferee() {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", 2);
    return new MasterReferee(new GameSettings(json));
  }

  private static void raiseTrack(Player p, CityImprovement track, int toLevel) {
    // Reaching level N costs 1+2+...+N of the track's commodity.
    int cost = toLevel * (toLevel + 1) / 2;
    p.addCommodity(track.getCommodity(), cost);
    for (int i = 0; i < toLevel; i++) {
      p.improveCity(track);
    }
    assertEquals(toLevel, p.getImprovementLevel(track));
  }

  @Test
  public void firstToLevelFourWinsTheMetropolis() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    raiseTrack(pa, CityImprovement.POLITICS, Settings.METROPOLIS_LEVEL);
    ref.awardMetropolis(CityImprovement.POLITICS);

    assertEquals(p0, ref.getMetropolisOwner(CityImprovement.POLITICS));
    assertEquals(Settings.METROPOLIS_POINT_VAL, pa.numVictoryPoints());
  }

  @Test
  public void outLevelingStealsTheMetropolisAndItsPoints() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    raiseTrack(pa, CityImprovement.POLITICS, Settings.METROPOLIS_LEVEL);
    ref.awardMetropolis(CityImprovement.POLITICS);
    assertEquals(p0, ref.getMetropolisOwner(CityImprovement.POLITICS));

    // p1 reaches level 5, strictly out-leveling the holder at 4.
    raiseTrack(pb, CityImprovement.POLITICS, Settings.METROPOLIS_LEVEL + 1);
    ref.awardMetropolis(CityImprovement.POLITICS);

    assertEquals(p1, ref.getMetropolisOwner(CityImprovement.POLITICS));
    assertEquals(Settings.METROPOLIS_POINT_VAL, pb.numVictoryPoints());
    // The former holder loses the metropolis points.
    assertEquals(0, pa.numVictoryPoints());
  }

  @Test
  public void aTieDoesNotAwardTheMetropolis() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    raiseTrack(pa, CityImprovement.SCIENCE, Settings.METROPOLIS_LEVEL);
    raiseTrack(pb, CityImprovement.SCIENCE, Settings.METROPOLIS_LEVEL);
    ref.awardMetropolis(CityImprovement.SCIENCE);

    assertEquals(-1, ref.getMetropolisOwner(CityImprovement.SCIENCE));
    assertEquals(0, pa.numVictoryPoints());
    assertEquals(0, pb.numVictoryPoints());
  }

  @Test
  public void baseGameHasNoMetropolis() {
    Referee ref = new MasterReferee(new GameSettings());
    ref.awardMetropolis(CityImprovement.TRADE);
    assertEquals(-1, ref.getMetropolisOwner(CityImprovement.TRADE));
  }
}
