package edu.brown.cs.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Static board data for the Seafarers "Heading for New Shores" scenario.
 *
 * <p>
 * The layout is a faithful adaptation of the introductory Seafarers scenario: a
 * large central <b>home island</b> (where every player places their initial
 * settlements), surrounded by open sea, with three smaller <b>outer
 * islands</b> that can only be reached by ship. Two of the outer hexes are
 * <b>gold</b> hexes, which let their owner choose the resource they produce.
 *
 * <p>
 * Tiles are described in an axial (row, column) grid and converted to the
 * engine's redundant cube {@link HexCoordinate} via {@code coord(row, col) = new
 * HexCoordinate(-row, col, 0)}. In that mapping a hex at (row, col) renders at
 * cartesian ({@code col + 0.5*row}, {@code (sqrt3/2)*row}); its six neighbours
 * are (row, col&plusmn;1), (row+1, col), (row+1, col-1), (row-1, col) and
 * (row-1, col+1). Every land island is separated from the others by at least
 * one full water hex, so no roads can bridge between islands.
 *
 * <p>
 * Water hexes are generated automatically as a one-hex ring around every land
 * hex, which both surrounds each island with a coastline and fills the gaps
 * between islands with a connected sea that ships can traverse.
 */
public final class SeafarersScenario {

  private SeafarersScenario() {
  }

  /**
   * A single tile definition: its coordinate, terrain type and dice number
   * (0 for tiles that never produce, i.e. water and desert).
   */
  public static final class TileData {
    public final HexCoordinate coord;
    public final TileType type;
    public final int number;

    public TileData(HexCoordinate coord, TileType type, int number) {
      this.coord = coord;
      this.type = type;
      this.number = number;
    }
  }

  /** All tiles in the scenario, land + gold + generated water. */
  public static final List<TileData> NEW_SHORES;
  /** The hex coordinates that make up the home island (initial settlements). */
  public static final Set<HexCoordinate> HOME_ISLAND;

  // Converts an axial (row, col) into the engine's cube coordinate.
  private static HexCoordinate coord(int row, int col) {
    return new HexCoordinate(-row, col, 0);
  }

  // The six neighbours of a cube coordinate.
  private static List<HexCoordinate> neighbors(HexCoordinate c) {
    int x = c.getX();
    int y = c.getY();
    int z = c.getZ();
    List<HexCoordinate> n = new ArrayList<>(6);
    n.add(new HexCoordinate(x, y + 1, z));
    n.add(new HexCoordinate(x, y - 1, z));
    n.add(new HexCoordinate(x - 1, y, z));
    n.add(new HexCoordinate(x + 1, y, z));
    n.add(new HexCoordinate(x, y, z + 1));
    n.add(new HexCoordinate(x, y, z - 1));
    return n;
  }

  static {
    List<TileData> land = new ArrayList<>();
    Set<HexCoordinate> home = new HashSet<>();

    // ---- Home island (13 land hexes, incl. one desert with the robber) ----
    addHome(land, home, 1, -1, TileType.WOOD, 6);
    addHome(land, home, 1, 0, TileType.WHEAT, 3);
    addHome(land, home, 1, 1, TileType.BRICK, 8);
    addHome(land, home, 1, 2, TileType.SHEEP, 4);
    addHome(land, home, 0, -2, TileType.ORE, 11);
    addHome(land, home, 0, -1, TileType.WHEAT, 5);
    addHome(land, home, 0, 0, TileType.DESERT, 0);
    addHome(land, home, 0, 1, TileType.WOOD, 9);
    addHome(land, home, 0, 2, TileType.BRICK, 10);
    addHome(land, home, -1, -2, TileType.SHEEP, 2);
    addHome(land, home, -1, -1, TileType.ORE, 12);
    addHome(land, home, -1, 0, TileType.WHEAT, 6);
    addHome(land, home, -1, 1, TileType.WOOD, 5);

    // ---- North outer island (with a gold hex) ----
    land.add(new TileData(coord(3, 0), TileType.SHEEP, 9));
    land.add(new TileData(coord(3, 1), TileType.WHEAT, 4));
    land.add(new TileData(coord(4, 0), TileType.GOLD, 5));

    // ---- East outer island (with a gold hex) ----
    land.add(new TileData(coord(0, 4), TileType.ORE, 8));
    land.add(new TileData(coord(0, 5), TileType.GOLD, 9));
    land.add(new TileData(coord(1, 4), TileType.WOOD, 10));

    // ---- South outer island ----
    land.add(new TileData(coord(-3, -1), TileType.BRICK, 3));
    land.add(new TileData(coord(-3, 0), TileType.SHEEP, 11));
    land.add(new TileData(coord(-3, 1), TileType.WHEAT, 8));

    // ---- Generate a one-hex water ring around all land ----
    Set<HexCoordinate> landCoords = new HashSet<>();
    for (TileData td : land) {
      landCoords.add(td.coord);
    }
    List<TileData> all = new ArrayList<>(land);
    Set<HexCoordinate> water = new HashSet<>();
    for (TileData td : land) {
      for (HexCoordinate n : neighbors(td.coord)) {
        if (!landCoords.contains(n) && !water.contains(n)) {
          water.add(n);
          all.add(new TileData(n, TileType.WATER, 0));
        }
      }
    }

    NEW_SHORES = Collections.unmodifiableList(all);
    HOME_ISLAND = Collections.unmodifiableSet(home);
  }

  private static void addHome(List<TileData> land, Set<HexCoordinate> home,
      int row, int col, TileType type, int number) {
    HexCoordinate c = coord(row, col);
    land.add(new TileData(c, type, number));
    home.add(c);
  }
}
