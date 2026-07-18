package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.brown.cs.actions.RollDice;
import edu.brown.cs.board.City;
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
    assertEquals(5, science.size());
    Set<ProgressCardType> drawn = new HashSet<>();
    while (!science.isEmpty()) {
      drawn.add(science.draw());
    }
    assertEquals(EnumSet.of(ProgressCardType.PRINTER,
        ProgressCardType.IRRIGATION, ProgressCardType.ENGINEER,
        ProgressCardType.INVENTOR, ProgressCardType.ALCHEMIST), drawn);
    assertNull(science.draw());

    ProgressCardDeck politics = new ProgressCardDeck(CityImprovement.POLITICS);
    assertEquals(5, politics.size());
    Set<ProgressCardType> drawnPolitics = new HashSet<>();
    while (!politics.isEmpty()) {
      drawnPolitics.add(politics.draw());
    }
    assertEquals(EnumSet.of(ProgressCardType.CONSTITUTION,
        ProgressCardType.INTRIGUE, ProgressCardType.WEDDING,
        ProgressCardType.BISHOP, ProgressCardType.DIPLOMAT), drawnPolitics);

    ProgressCardDeck trade = new ProgressCardDeck(CityImprovement.TRADE);
    assertEquals(5, trade.size());
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
  public void inventorSwapsTwoTilesNumbers() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    Player pa = ref.getPlayerByID(p0);

    Tile first = ref.getBoard().getTiles().iterator().next();
    Tile second = null;
    for (Tile t : ref.getBoard().getTiles()) {
      if (!t.equals(first) && t.getRollNumber() != first.getRollNumber()) {
        second = t;
        break;
      }
    }
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
}
