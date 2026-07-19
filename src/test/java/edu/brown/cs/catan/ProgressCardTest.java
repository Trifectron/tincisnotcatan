package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.actions.RollDice;
import edu.brown.cs.board.City;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Path;
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
    // 10 science card types, 2 copies each = 20 cards.
    assertEquals(20, science.size());
    Set<ProgressCardType> drawn = new HashSet<>();
    while (!science.isEmpty()) {
      drawn.add(science.draw());
    }
    assertEquals(EnumSet.of(ProgressCardType.PRINTER,
        ProgressCardType.IRRIGATION, ProgressCardType.ENGINEER,
        ProgressCardType.INVENTOR, ProgressCardType.ALCHEMIST,
        ProgressCardType.CRANE, ProgressCardType.MEDICINE,
        ProgressCardType.MINING, ProgressCardType.SMITH,
        ProgressCardType.ROAD_BUILDING), drawn);
    assertNull(science.draw());

    // 9 politics card types, 2 copies each = 18 cards.
    ProgressCardDeck politics = new ProgressCardDeck(CityImprovement.POLITICS);
    assertEquals(18, politics.size());
    Set<ProgressCardType> drawnPolitics = new HashSet<>();
    while (!politics.isEmpty()) {
      drawnPolitics.add(politics.draw());
    }
    assertEquals(EnumSet.of(ProgressCardType.CONSTITUTION,
        ProgressCardType.INTRIGUE, ProgressCardType.WEDDING,
        ProgressCardType.BISHOP, ProgressCardType.DIPLOMAT,
        ProgressCardType.DESERTER, ProgressCardType.SABOTEUR,
        ProgressCardType.SPY, ProgressCardType.WARLORD), drawnPolitics);
    assertNull(politics.draw());

    // 5 trade card types, 2 copies each = 10 cards.
    ProgressCardDeck trade = new ProgressCardDeck(CityImprovement.TRADE);
    assertEquals(10, trade.size());
    Set<ProgressCardType> drawnTrade = new HashSet<>();
    while (!trade.isEmpty()) {
      drawnTrade.add(trade.draw());
    }
    assertEquals(EnumSet.of(ProgressCardType.MASTER_MERCHANT,
        ProgressCardType.RESOURCE_MONOPOLY, ProgressCardType.TRADE_MONOPOLY,
        ProgressCardType.MERCHANT_FLEET, ProgressCardType.MERCHANT),
        drawnTrade);
    assertNull(trade.draw());
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

  @Test
  public void craneAdvancesTheNamedTrackForFree() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);

    assertEquals(0, p.getImprovementLevel(CityImprovement.TRADE));
    String msg = ProgressCardType.CRANE.play(ref, p, "trade");
    assertEquals(1, p.getImprovementLevel(CityImprovement.TRADE));
    assertTrue(msg.contains("trade"));

    // The free upgrade doesn't consume any commodities.
    assertEquals(0.0, p.getCommodities().get(Commodity.CLOTH), 0.0001);
  }

  @Test
  public void craneAdvancesEvenWithoutTheCommodity() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    // Player holds zero cloth — the regular improveCity flow would refuse,
    // but Crane grants a free level-up regardless.
    assertEquals(0.0, p.getCommodities().get(Commodity.CLOTH), 0.0001);

    ProgressCardType.CRANE.play(ref, p, "trade");
    assertEquals(1, p.getImprovementLevel(CityImprovement.TRADE));
  }

  @Test
  public void craneRejectsUnknownTrackAndMaxLevel() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    assertTrue(ProgressCardType.CRANE.play(ref, p, "banking")
        .contains("didn't name a valid"));

    // Drive TRADE up to the maximum through commodities, then expect a
    // rejection.
    int clothNeeded = 1 + 2 + 3 + 4 + 5;
    p.addCommodity(Commodity.CLOTH, clothNeeded);
    for (int level = 0; level < CityImprovement.MAX_LEVEL; level++) {
      while (!p.canImproveCity(CityImprovement.TRADE)) {
        // shouldn't trip, but guard against an off-by-one
        p.addCommodity(Commodity.CLOTH, 1);
      }
      p.improveCity(CityImprovement.TRADE);
    }
    assertEquals(CityImprovement.MAX_LEVEL,
        p.getImprovementLevel(CityImprovement.TRADE));

    String msg = ProgressCardType.CRANE.play(ref, p, "trade");
    assertTrue(msg.contains("already maximized"));
  }

  @Test
  public void craneChargesOneFewerCommodityAtHigherLevels() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    // Normal cost from level 1 to level 2 is 2 cloth; Crane should charge 1.
    p.addCommodity(Commodity.CLOTH, 1);
    p.improveCity(CityImprovement.TRADE);
    assertEquals(1, p.getImprovementLevel(CityImprovement.TRADE));
    p.addCommodity(Commodity.CLOTH, 1);

    String msg = ProgressCardType.CRANE.play(ref, p, "trade");

    assertEquals(2, p.getImprovementLevel(CityImprovement.TRADE));
    assertEquals(0.0, p.getCommodities().get(Commodity.CLOTH), 0.0001);
    assertTrue(msg.contains("1 fewer commodity"));
  }

  @Test
  public void craneRejectsWithoutEnoughForTheDiscount() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    p.addCommodity(Commodity.CLOTH, 1);
    p.improveCity(CityImprovement.TRADE);
    assertEquals(1, p.getImprovementLevel(CityImprovement.TRADE));
    // No cloth left to pay the discounted cost of 1.

    String msg = ProgressCardType.CRANE.play(ref, p, "trade");

    assertEquals(1, p.getImprovementLevel(CityImprovement.TRADE));
    assertTrue(msg.contains("can't afford"));
  }

  @Test
  public void medicineUpgradesASettlementForADiscount() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    Intersection settlementInt = ref.getBoard().getIntersections().values()
        .iterator().next();
    settlementInt.placeSettlement(p);
    p.useSettlement();
    p.addResource(Resource.ORE, 2, ref.getBank());
    p.addResource(Resource.WHEAT, 1, ref.getBank());
    int citiesBefore = p.numCities();

    String msg = ProgressCardType.MEDICINE.play(ref, p,
        intersectionTarget(settlementInt.getPosition()));

    assertTrue(settlementInt.getBuilding() instanceof City);
    assertEquals(0.0, p.getResources().get(Resource.ORE), 0.0001);
    assertEquals(0.0, p.getResources().get(Resource.WHEAT), 0.0001);
    assertEquals(citiesBefore - 1, p.numCities());
    assertTrue(msg.contains("2 ore"));
  }

  @Test
  public void medicineRejectsWithoutTheDiscountedCost() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    Intersection settlementInt = ref.getBoard().getIntersections().values()
        .iterator().next();
    settlementInt.placeSettlement(p);

    String msg = ProgressCardType.MEDICINE.play(ref, p,
        intersectionTarget(settlementInt.getPosition()));

    assertFalse(settlementInt.getBuilding() instanceof City);
    assertTrue(msg.contains("can't afford"));
  }

  @Test
  public void medicineRejectsSomeoneElsesSettlement() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    Intersection settlementInt = ref.getBoard().getIntersections().values()
        .iterator().next();
    settlementInt.placeSettlement(pb);
    pa.addResource(Resource.ORE, 2, ref.getBank());
    pa.addResource(Resource.WHEAT, 1, ref.getBank());

    String msg = ProgressCardType.MEDICINE.play(ref, pa,
        intersectionTarget(settlementInt.getPosition()));

    assertTrue(msg.contains("isn't your settlement"));
  }

  @Test
  public void miningPaysTwoOrePerAdjacentOreHex() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);

    Tile oreTile = null;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getType() == TileType.ORE) {
        oreTile = tile;
        break;
      }
    }
    assertTrue("Standard board should have an ore hex", oreTile != null);
    Intersection settlementInt = oreTile.getIntersections().iterator()
        .next();
    settlementInt.placeSettlement(p);

    int expectedOre = 0;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getType() == TileType.ORE
          && tile.getIntersections().contains(settlementInt)) {
        expectedOre += 2;
      }
    }

    double before = p.getResources().get(Resource.ORE);
    String msg = ProgressCardType.MINING.play(ref, p);
    assertEquals(before + expectedOre, p.getResources().get(Resource.ORE),
        0.0001);
    assertTrue(msg.contains("ore"));
  }

  @Test
  public void miningNoOpWithoutAdjacentBuilding() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);

    double before = p.getResources().get(Resource.ORE);
    String msg = ProgressCardType.MINING.play(ref, p);
    assertEquals(before, p.getResources().get(Resource.ORE), 0.0001);
    assertTrue(msg.contains("no settlements"));
  }

  @Test
  public void roadBuildingEnqueuesAPlaceRoadFollowUp() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    // Place a settlement so the player has a legal road placement; without
    // it Road Building correctly bails out and enqueues nothing.
    Intersection settlementInt = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() == null) {
        settlementInt = i;
        break;
      }
    }
    assertNotNull(settlementInt);
    settlementInt.placeSettlement(p);
    assertNull(ref.getNextFollowUp(p0));

    String msg = ProgressCardType.ROAD_BUILDING.play(ref, p);
    assertNotNull("Expected a follow-up action to be enqueued",
        ref.getNextFollowUp(p0));
    assertEquals("placeRoad", ref.getNextFollowUp(p0).getID());
    assertTrue(msg.contains("two free roads"));
  }

  @Test
  public void intrigueDeactivatesStrongestOpposingKnight() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    Intersection knightInt = ref.getBoard().getIntersections().values()
        .iterator().next();
    knightInt.placeKnight(pb);
    knightInt.getKnight().activate();

    String msg = ProgressCardType.INTRIGUE.play(ref, pa);
    assertFalse(knightInt.getKnight().isActive());
    assertTrue(msg.contains("deactivated"));
  }

  @Test
  public void intrigueNoOpWithoutActiveOpposingKnight() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.INTRIGUE.play(ref, pa);
    assertTrue(msg.contains("no opposing knight"));
  }

  @Test
  public void weddingTakesResourcesFromRicherPlayers() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    pb.addVictoryPoints(1);
    pb.addResource(Resource.WHEAT, 5, ref.getBank());

    double paBefore = pa.getResources().get(Resource.WHEAT);
    double pbBefore = pb.getResources().get(Resource.WHEAT);
    String msg = ProgressCardType.WEDDING.play(ref, pa);

    assertEquals(paBefore + 2, pa.getResources().get(Resource.WHEAT), 0.0001);
    assertEquals(pbBefore - 2, pb.getResources().get(Resource.WHEAT), 0.0001);
    assertTrue(msg.contains("received 2 resource"));
  }

  @Test
  public void weddingNoOpWithoutRicherPlayer() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.WEDDING.play(ref, pa);
    assertTrue(msg.contains("no player has more victory points"));
  }

  @Test
  public void weddingFallsBackToCommoditiesWhenVictimHasNoResources() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    // pb is richer in VPs but holds no resources — only commodities. The
    // official Cities & Knights rule lets Wedding take commodities as a
    // fallback.
    pb.addVictoryPoints(1);
    pb.addCommodity(Commodity.CLOTH, 5);

    double paBefore = pa.getCommodities().get(Commodity.CLOTH);
    double pbBefore = pb.getCommodities().get(Commodity.CLOTH);
    String msg = ProgressCardType.WEDDING.play(ref, pa);

    assertEquals(paBefore + 2, pa.getCommodities().get(Commodity.CLOTH),
        0.0001);
    assertEquals(pbBefore - 2, pb.getCommodities().get(Commodity.CLOTH),
        0.0001);
    assertTrue(msg.contains("received 2 commodity"));
  }

  @Test
  public void weddingTakesResourcesFirstThenCommoditiesIfNeeded() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    // pb is richer and holds exactly 1 resource; the second card the
    // Wedding takes should fall through to commodities.
    pb.addVictoryPoints(1);
    pb.addResource(Resource.ORE, 1, ref.getBank());
    pb.addCommodity(Commodity.COIN, 3);

    double paOreBefore = pa.getResources().get(Resource.ORE);
    double paCoinBefore = pa.getCommodities().get(Commodity.COIN);
    double pbOreBefore = pb.getResources().get(Resource.ORE);
    double pbCoinBefore = pb.getCommodities().get(Commodity.COIN);
    String msg = ProgressCardType.WEDDING.play(ref, pa);

    assertEquals(paOreBefore + 1, pa.getResources().get(Resource.ORE), 0.0001);
    assertEquals(pbOreBefore - 1, pb.getResources().get(Resource.ORE), 0.0001);
    assertEquals(paCoinBefore + 1, pa.getCommodities().get(Commodity.COIN),
        0.0001);
    assertEquals(pbCoinBefore - 1, pb.getCommodities().get(Commodity.COIN),
        0.0001);
    assertTrue(msg.contains("received 2 card(s)"));
    assertTrue(msg.contains("1 resource"));
    assertTrue(msg.contains("1 commodity"));
  }

  @Test
  public void handLimitConstantDefaultsToFour() {
    // The C&K 4-card hand limit is shared between Player/drawing logic
    // and progress-card tests.
    assertEquals(4, Settings.PROGRESS_CARD_HAND_LIMIT);
  }

  @Test
  public void drawingFifthCardOnOtherPlayersTurnForcesImmediateDiscard() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    // Stock pa up to the hand limit exactly.
    pa.addProgressCard(ProgressCardType.PRINTER);
    pa.addProgressCard(ProgressCardType.PRINTER);
    pa.addProgressCard(ProgressCardType.PRINTER);
    pa.addProgressCard(ProgressCardType.PRINTER);
    assertEquals(4, pa.getProgressCards().size());

    // On pa's own turn, drawing a 5th card is *not* discarded: the rule
    // only forces a discard when the draw happens on another player's
    // turn.
    pa.addProgressCard(ProgressCardType.PRINTER);
    assertEquals(5, pa.getProgressCards().size());

    // Simulating the over-cap rule: discard the most recently added card
    // when the draw happens off-turn.
    ProgressCardType justDrawn = ProgressCardType.PRINTER;
    pa.removeProgressCard(justDrawn);
    assertEquals(4, pa.getProgressCards().size());
  }

  @Test
  public void masterMerchantTakesFromThePlayerWithTheMostCards() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    pb.addResource(Resource.WHEAT, 5, ref.getBank());

    double paBefore = pa.getResources().get(Resource.WHEAT);
    double pbBefore = pb.getResources().get(Resource.WHEAT);
    String msg = ProgressCardType.MASTER_MERCHANT.play(ref, pa);

    assertEquals(paBefore + 2, pa.getResources().get(Resource.WHEAT), 0.0001);
    assertEquals(pbBefore - 2, pb.getResources().get(Resource.WHEAT), 0.0001);
    assertTrue(msg.contains("took 2 resource"));
  }

  @Test
  public void masterMerchantNoOpWhenNoOneHoldsCards() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.MASTER_MERCHANT.play(ref, pa);
    assertTrue(msg.contains("no other player holds any cards"));
  }

  @Test
  public void resourceMonopolyTakesAllNamedResourceFromEveryOtherPlayer() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    int p2 = ref.addPlayer("C", "#222222");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    Player pc = ref.getPlayerByID(p2);

    pb.addResource(Resource.ORE, 2, ref.getBank());
    pc.addResource(Resource.ORE, 3, ref.getBank());
    pc.addResource(Resource.WHEAT, 1, ref.getBank());

    double paBefore = pa.getResources().get(Resource.ORE);
    String msg = ProgressCardType.RESOURCE_MONOPOLY.play(ref, pa, "ore");

    assertEquals(paBefore + 5, pa.getResources().get(Resource.ORE), 0.0001);
    assertEquals(0, pb.getResources().get(Resource.ORE), 0.0001);
    assertEquals(0, pc.getResources().get(Resource.ORE), 0.0001);
    assertEquals(1, pc.getResources().get(Resource.WHEAT), 0.0001);
    assertTrue(msg.contains("took all 5 ore"));
  }

  @Test
  public void resourceMonopolyNoOpWhenNoOneHasTheResource() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.RESOURCE_MONOPOLY.play(ref, pa, "ore");
    assertTrue(msg.contains("no other player has any ore"));
  }

  @Test
  public void tradeMonopolyTakesOneNamedResourceFromEveryOtherPlayer() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    int p2 = ref.addPlayer("C", "#222222");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    Player pc = ref.getPlayerByID(p2);

    pb.addResource(Resource.ORE, 2, ref.getBank());
    pc.addResource(Resource.ORE, 3, ref.getBank());

    double paBefore = pa.getResources().get(Resource.ORE);
    String msg = ProgressCardType.TRADE_MONOPOLY.play(ref, pa, "ore");

    assertEquals(paBefore + 2, pa.getResources().get(Resource.ORE), 0.0001);
    assertEquals(1, pb.getResources().get(Resource.ORE), 0.0001);
    assertEquals(2, pc.getResources().get(Resource.ORE), 0.0001);
    assertTrue(msg.contains("took 2 ore"));
  }

  @Test
  public void tradeMonopolyNoOpWhenNoOneHasTheResource() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.TRADE_MONOPOLY.play(ref, pa, "ore");
    assertTrue(msg.contains("no other player has any ore"));
  }

  @Test
  public void merchantFleetGrantsTwoToOneBankRateForTheResource() {
    MasterReferee ref = cnkReferee();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    // Turn order is shuffled, so play as whichever player actually goes
    // first rather than assuming it's the first one added.
    Player current = ref.currentPlayer();
    int currentID = current.getID();

    double before = ref.getBankRates(currentID).get(Resource.ORE);
    String msg = ProgressCardType.MERCHANT_FLEET.play(ref, current, "ore");

    assertTrue(before > 2);
    assertEquals(2, ref.getBankRates(currentID).get(Resource.ORE), 0.0001);
    assertTrue(msg.contains("2:1"));
  }

  @Test
  public void bishopEnqueuesAMoveRobberFollowUp() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    assertNull(ref.getNextFollowUp(p0));
    String msg = ProgressCardType.BISHOP.play(ref, pa);

    assertEquals("moveRobber", ref.getNextFollowUp(p0).getID());
    assertTrue(msg.contains("robber"));
  }

  @Test
  public void bishopStealsFromEveryPlayerOnTheNewHex() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    int p2 = ref.addPlayer("C", "#222222");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    Player pc = ref.getPlayerByID(p2);
    pb.addResource(Resource.WHEAT, 1, ref.getBank());
    pc.addResource(Resource.ORE, 1, ref.getBank());
    // pd (a fourth, resourceless player) should be skipped entirely even if
    // it shares the hex.
    Tile target = null;
    for (Tile t : ref.getBoard().getTiles()) {
      if (!t.hasRobber()) {
        target = t;
        break;
      }
    }
    Iterator<Intersection> corners = target.getIntersections().iterator();
    corners.next().placeSettlement(pb);
    corners.next().placeSettlement(pc);

    ProgressCardType.BISHOP.play(ref, pa);
    FollowUpAction followUp = ref.getNextFollowUp(p0);
    // The C&K robber-move timing gate requires the barbarians to have
    // reached the island for the first time before any robber move.
    for (int i = 0; i < Settings.BARBARIAN_TRACK_LENGTH; i++) {
      ref.getBarbarianTrack().advance();
    }
    HexCoordinate coord = target.getCoordinate();
    JsonObject location = new JsonObject();
    location.addProperty("x", coord.getX());
    location.addProperty("y", coord.getY());
    location.addProperty("z", coord.getZ());
    JsonObject json = new JsonObject();
    json.add("newLocation", location);
    followUp.setupAction(ref, p0, json);
    followUp.execute();

    assertEquals(0.0, pb.getResources().get(Resource.WHEAT), 0.0001);
    assertEquals(0.0, pc.getResources().get(Resource.ORE), 0.0001);
    assertEquals(2.0, pa.getNumResourceCards(), 0.0001);
  }

  private static boolean isUnswappableNumber(int rollNumber) {
    return rollNumber == 2 || rollNumber == 6 || rollNumber == 8
        || rollNumber == 12;
  }

  @Test
  public void inventorSwapsTwoTilesNumbers() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    Tile first = null;
    Tile second = null;
    for (Tile t : ref.getBoard().getTiles()) {
      if (isUnswappableNumber(t.getRollNumber())) {
        continue;
      }
      if (first == null) {
        first = t;
      } else if (t.getRollNumber() != first.getRollNumber()) {
        second = t;
        break;
      }
    }
    assertTrue("Standard board should have two swappable, differing tiles",
        second != null);
    int firstBefore = first.getRollNumber();
    int secondBefore = second.getRollNumber();
    String target = String.format("%d,%d,%d;%d,%d,%d",
        first.getCoordinate().getX(), first.getCoordinate().getY(),
        first.getCoordinate().getZ(), second.getCoordinate().getX(),
        second.getCoordinate().getY(), second.getCoordinate().getZ());

    String msg = ProgressCardType.INVENTOR.play(ref, pa, target);

    assertEquals(secondBefore, first.getRollNumber());
    assertEquals(firstBefore, second.getRollNumber());
    assertTrue(msg.contains("swapped"));
  }

  @Test
  public void inventorNoOpWithUnknownTile() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.INVENTOR.play(ref, pa, "0,0,0;99,99,99");
    assertTrue(msg.contains("isn't on the board"));
  }

  @Test
  public void inventorRejectsTwoSixEightAndTwelveTokens() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    Tile unswappable = null;
    Tile other = null;
    for (Tile t : ref.getBoard().getTiles()) {
      if (isUnswappableNumber(t.getRollNumber())) {
        unswappable = t;
      } else if (other == null) {
        other = t;
      }
    }
    assertTrue("Standard board should have a 2/6/8/12 tile",
        unswappable != null);
    String target = String.format("%d,%d,%d;%d,%d,%d",
        unswappable.getCoordinate().getX(), unswappable.getCoordinate().getY(),
        unswappable.getCoordinate().getZ(), other.getCoordinate().getX(),
        other.getCoordinate().getY(), other.getCoordinate().getZ());

    int unswappableBefore = unswappable.getRollNumber();
    String msg = ProgressCardType.INVENTOR.play(ref, pa, target);

    assertEquals(unswappableBefore, unswappable.getRollNumber());
    assertTrue(msg.contains("2, 6, 8, or 12"));
  }

  private static Tile findResourceTile(MasterReferee ref, Tile exclude) {
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.getType().getType() != null && !t.equals(exclude)) {
        return t;
      }
    }
    return null;
  }

  private static String coordTarget(Tile t) {
    return String.format("%d,%d,%d", t.getCoordinate().getX(),
        t.getCoordinate().getY(), t.getCoordinate().getZ());
  }

  @Test
  public void merchantGrantsATwoToOneRateAndAVictoryPoint() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Tile tile = findResourceTile(ref, null);
    Resource res = tile.getType().getType();
    tile.getIntersections().iterator().next().placeSettlement(pa);
    int vpBefore = pa.numVictoryPoints();

    String msg = ProgressCardType.MERCHANT.play(ref, pa, coordTarget(tile));

    assertEquals(vpBefore + 1, pa.numVictoryPoints());
    assertEquals(2, ref.getBankRates(p0).get(res), 0.0001);
    assertTrue(msg.contains("2:1"));
  }

  @Test
  public void merchantMovingYourOwnMerchantDoesNotGrantASecondVictoryPoint() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Tile first = findResourceTile(ref, null);
    Tile second = findResourceTile(ref, first);
    first.getIntersections().iterator().next().placeSettlement(pa);
    second.getIntersections().iterator().next().placeSettlement(pa);
    int vpBefore = pa.numVictoryPoints();

    ProgressCardType.MERCHANT.play(ref, pa, coordTarget(first));
    ProgressCardType.MERCHANT.play(ref, pa, coordTarget(second));

    assertEquals(vpBefore + 1, pa.numVictoryPoints());
    assertEquals(-1, first.getMerchantOwner());
    assertEquals(p0, second.getMerchantOwner());
  }

  @Test
  public void merchantDisplacesTheEarlierOwnersVictoryPoint() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    Tile tile = findResourceTile(ref, null);
    Iterator<Intersection> corners = tile.getIntersections().iterator();
    corners.next().placeSettlement(pa);
    corners.next().placeSettlement(pb);
    int paVpBefore = pa.numVictoryPoints();
    int pbVpBefore = pb.numVictoryPoints();

    ProgressCardType.MERCHANT.play(ref, pa, coordTarget(tile));
    ProgressCardType.MERCHANT.play(ref, pb, coordTarget(tile));

    assertEquals(paVpBefore, pa.numVictoryPoints());
    assertEquals(pbVpBefore + 1, pb.numVictoryPoints());
    assertEquals(p1, tile.getMerchantOwner());
  }

  @Test
  public void merchantNoOpWithUnknownTile() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    // HexCoordinate.equals compares cartesian projections, so an x=y=z
    // coordinate like "99,99,99" aliases the origin tile; "0,100,0" is
    // actually far off the board.
    String msg = ProgressCardType.MERCHANT.play(ref, pa, "0,100,0");
    assertTrue(msg.contains("isn't on the board"));
  }

  @Test
  public void merchantRejectsAHexNotAdjacentToYourOwnBuilding() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);
    Tile tile = findResourceTile(ref, null);

    String msg = ProgressCardType.MERCHANT.play(ref, pa, coordTarget(tile));

    assertEquals(-1, tile.getMerchantOwner());
    assertTrue(msg.contains("adjacent"));
  }

  @Test
  public void alchemistForcesTheDiceRollTotal() {
    MasterReferee ref = cnkReferee();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player current = ref.currentPlayer();
    ref.addFollowUp(ImmutableList.of(new RollDice(current.getID())));

    String msg = ProgressCardType.ALCHEMIST.play(ref, current, "9");

    assertNull(ref.getNextFollowUp(current.getID()));
    assertTrue(msg.contains("9"));
  }

  @Test
  public void alchemistRejectsAnOutOfRangeRoll() {
    MasterReferee ref = cnkReferee();
    ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player current = ref.currentPlayer();
    ref.addFollowUp(ImmutableList.of(new RollDice(current.getID())));

    String msg = ProgressCardType.ALCHEMIST.play(ref, current, "13");

    assertTrue(msg.contains("2-12"));
    assertNotNull(ref.getNextFollowUp(current.getID()));
  }

  @Test
  public void alchemistNoOpWithoutAPendingRoll() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.ALCHEMIST.play(ref, pa, "9");
    assertTrue(msg.contains("isn't time to roll"));
  }

  private static String intersectionTarget(IntersectionCoordinate coord) {
    return String.format("%d,%d,%d|%d,%d,%d|%d,%d,%d",
        coord.getCoord1().getX(), coord.getCoord1().getY(),
        coord.getCoord1().getZ(), coord.getCoord2().getX(),
        coord.getCoord2().getY(), coord.getCoord2().getZ(),
        coord.getCoord3().getX(), coord.getCoord3().getY(),
        coord.getCoord3().getZ());
  }

  private static String pathTarget(Path path) {
    return intersectionTarget(path.getStart().getPosition()) + ";"
        + intersectionTarget(path.getEnd().getPosition());
  }

  @Test
  public void diplomatRemovesAnOpenRoadAndReturnsThePiece() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    Path path = ref.getBoard().getPaths().values().iterator().next();
    path.getStart().placeSettlement(pb);
    path.placeRoad(pb);
    int pbRoadsBefore = pb.numRoads();

    String msg = ProgressCardType.DIPLOMAT.play(ref, pa, pathTarget(path));

    assertNull(path.getRoad());
    assertEquals(pbRoadsBefore + 1, pb.numRoads());
    assertTrue(msg.contains("removed"));
  }

  @Test
  public void diplomatRejectsARoadSealedBetweenTwoBuildings() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    Path path = ref.getBoard().getPaths().values().iterator().next();
    path.getStart().placeSettlement(pb);
    path.getEnd().placeSettlement(pb);
    path.placeRoad(pb);

    String msg = ProgressCardType.DIPLOMAT.play(ref, pa, pathTarget(path));

    assertNotNull(path.getRoad());
    assertTrue(msg.contains("sealed"));
  }

  @Test
  public void diplomatNoOpWithARoadlessPath() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);
    Path path = ref.getBoard().getPaths().values().iterator().next();

    String msg = ProgressCardType.DIPLOMAT.play(ref, pa, pathTarget(path));
    assertTrue(msg.contains("no road"));
  }

  @Test
  public void smithPromotesTwoKnightsForFree() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);

    Intersection[] spots = new Intersection[2];
    int found = 0;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      spots[found] = i;
      i.placeKnight(p);
      found++;
      if (found == 2) {
        break;
      }
    }

    String target = intersectionTarget(spots[0].getPosition()) + ";"
        + intersectionTarget(spots[1].getPosition());
    String msg = ProgressCardType.SMITH.play(ref, p, target);

    assertEquals(2, spots[0].getKnight().getTier());
    assertEquals(2, spots[1].getKnight().getTier());
    assertTrue(msg.contains("promoted"));
  }

  @Test
  public void smithRejectsWhenTargetIsNotYourKnight() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    Intersection[] spots = new Intersection[2];
    int found = 0;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      spots[found] = i;
      found++;
      if (found == 2) {
        break;
      }
    }
    spots[0].placeKnight(pa);
    spots[1].placeKnight(pb);

    String target = intersectionTarget(spots[0].getPosition()) + ";"
        + intersectionTarget(spots[1].getPosition());
    String msg = ProgressCardType.SMITH.play(ref, pa, target);

    assertEquals(1, spots[0].getKnight().getTier());
    assertTrue(msg.contains("no knight of yours"));
  }

  @Test
  public void smithRejectsTheSameKnightNamedTwice() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);

    Intersection spot = ref.getBoard().getIntersections().values().iterator()
        .next();
    spot.placeKnight(p);

    String single = intersectionTarget(spot.getPosition());
    String msg = ProgressCardType.SMITH.play(ref, p, single + ";" + single);

    assertEquals(1, spot.getKnight().getTier());
    assertTrue(msg.contains("same knight twice"));
  }

  @Test
  public void smithGatesMightyPromotionOnPoliticsLevelFour() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    // Politics level 3 is one short of the metropolis gate; mighty
    // promotions via Smith must still be rejected.
    advancePoliticsTo(p, 3);

    Intersection[] spots = new Intersection[2];
    int found = 0;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      spots[found] = i;
      i.placeKnight(p);
      spots[found].getKnight().upgrade();
      found++;
      if (found == 2) {
        break;
      }
    }

    String target = intersectionTarget(spots[0].getPosition()) + ";"
        + intersectionTarget(spots[1].getPosition());
    String msg = ProgressCardType.SMITH.play(ref, p, target);

    assertEquals(2, spots[0].getKnight().getTier());
    assertEquals(2, spots[1].getKnight().getTier());
    assertTrue(msg.contains("level-4 Politics"));
  }

  // Level N of an improvement costs N coins.
  private static void advancePoliticsTo(Player p, int targetLevel) {
    int coinsNeeded = targetLevel * (targetLevel + 1) / 2;
    p.addCommodity(Commodity.COIN, coinsNeeded);
    for (int level = 0; level < targetLevel; level++) {
      assertTrue("Politics improvement should be affordable",
          p.canImproveCity(CityImprovement.POLITICS));
      p.improveCity(CityImprovement.POLITICS);
    }
    assertEquals(targetLevel, p.getImprovementLevel(CityImprovement.POLITICS));
  }

  @Test
  public void smithRespectsThePerTierMightyKnightCap() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player p = ref.getPlayerByID(p0);
    // Mighty knights require Politics level 4 (the metropolis level). Bump
    // it so the test can directly place mighty knights via the public API.
    advancePoliticsTo(p, 4);

    Intersection[] spots = new Intersection[3];
    int found = 0;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      spots[found] = i;
      i.placeKnight(p);
      found++;
      if (found == 3) {
        break;
      }
    }
    // p already has one mighty knight (spots[0]); Smith then tries to
    // promote two more tier-2 knights to mighty in one call, which would
    // put p at 3 mighty knights -- one over the 2-per-tier cap -- even
    // though each promotion is individually legal.
    spots[0].getKnight().upgrade();
    spots[0].getKnight().upgrade();
    spots[1].getKnight().upgrade();
    spots[2].getKnight().upgrade();

    String target = intersectionTarget(spots[1].getPosition()) + ";"
        + intersectionTarget(spots[2].getPosition());
    String msg = ProgressCardType.SMITH.play(ref, p, target);

    assertEquals(2, spots[1].getKnight().getTier());
    assertEquals(2, spots[2].getKnight().getTier());
    assertTrue(msg.toLowerCase().contains("cap"));
  }

  @Test
  public void warlordActivatesAllOfYourInactiveKnights() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    Intersection[] spots = new Intersection[2];
    int found = 0;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      spots[found] = i;
      i.placeKnight(pa);
      found++;
      if (found == 2) {
        break;
      }
    }

    String msg = ProgressCardType.WARLORD.play(ref, pa);

    assertTrue(spots[0].getKnight().isActive());
    assertTrue(spots[1].getKnight().isActive());
    assertTrue(msg.contains("2 knight"));
  }

  @Test
  public void warlordNoOpWithoutInactiveKnights() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.WARLORD.play(ref, pa);
    assertTrue(msg.contains("no inactive knights"));
  }

  @Test
  public void spyStealsANamedProgressCardFromAnOpponent() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    pb.addProgressCard(ProgressCardType.PRINTER);

    String msg = ProgressCardType.SPY.play(ref, pa, "B;printer");

    assertTrue(pa.getProgressCards().contains(ProgressCardType.PRINTER));
    assertFalse(pb.getProgressCards().contains(ProgressCardType.PRINTER));
    assertTrue(msg.contains("stole"));
  }

  @Test
  public void spyRejectsWhenOpponentLacksTheNamedCard() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.SPY.play(ref, pa, "B;printer");
    assertTrue(msg.contains("doesn't hold"));
  }

  @Test
  public void spyRejectsAnUnknownOpponent() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.SPY.play(ref, pa, "Nobody;printer");
    assertTrue(msg.contains("didn't name a valid opponent"));
  }

  @Test
  public void saboteurQueuesDiscardsForPlayersAtLeastAsRichInPoints() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    pb.addResource(Resource.WHEAT, 4, ref.getBank());

    String msg = ProgressCardType.SABOTEUR.play(ref, pa);

    assertNull(ref.getNextFollowUp(p0));
    assertEquals("dropCards", ref.getNextFollowUp(p1).getID());
    assertTrue(msg.contains("1 player"));
  }

  @Test
  public void saboteurSkipsPlayersBehindOnVictoryPoints() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);
    pa.addVictoryPoints(2);
    pb.addResource(Resource.WHEAT, 4, ref.getBank());

    String msg = ProgressCardType.SABOTEUR.play(ref, pa);

    assertNull(ref.getNextFollowUp(p1));
    assertTrue(msg.contains("no player has as many victory points"));
  }

  @Test
  public void deserterRemovesTheOpponentsWeakestKnight() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    Intersection[] spots = new Intersection[2];
    int found = 0;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      spots[found] = i;
      i.placeKnight(pb);
      found++;
      if (found == 2) {
        break;
      }
    }
    spots[1].getKnight().upgrade();

    String msg = ProgressCardType.DESERTER.play(ref, pa, "B");

    assertFalse(spots[0].hasKnight());
    assertEquals(2, spots[1].getKnight().getTier());
    assertTrue(msg.contains("tier-1"));
  }

  @Test
  public void deserterPlacesYourOwnKnightAtEqualStrength() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    Path path = ref.getBoard().getPaths().values().iterator().next();
    Intersection weakestSpot = path.getStart();
    weakestSpot.placeKnight(pb);
    path.getEnd().placeSettlement(pa);
    path.placeRoad(pa);

    String target = "B;" + intersectionTarget(weakestSpot.getPosition());
    String msg = ProgressCardType.DESERTER.play(ref, pa, target);

    assertTrue(weakestSpot.hasKnight());
    assertEquals(pa, weakestSpot.getKnight().getPlayer());
    assertEquals(1, weakestSpot.getKnight().getTier());
    assertTrue(msg.contains("placed"));
  }

  @Test
  public void deserterRejectsAnUnknownOpponent() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.DESERTER.play(ref, pa, "Nobody");
    assertTrue(msg.contains("didn't name a valid opponent"));
  }

  @Test
  public void deserterNoOpWhenOpponentHasNoKnights() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    String msg = ProgressCardType.DESERTER.play(ref, pa, "B");
    assertTrue(msg.contains("no knights"));
  }
}
