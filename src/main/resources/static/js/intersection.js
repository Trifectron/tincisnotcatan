var SETTLEMENT_SCALE = 0.2;
var CITY_SCALE = 0.25;
var SELECTABLE_AREA_SCALE = 0.25;
var SETTLEMENT_SVG_WIDTH = 16;
var CITY_SVG_WIDTH = 100;

var SETTLEMENT_SVG = "<svg class='settlement-icon'><path "
	+ "d='M16 9.5l-3-3v-4.5h-2v2.5l-3-3-8 8v0.5h2v5h5v-3h2v3h5v-5h2z'></path></svg>";
var CITY_SVG = '<svg class="city-icon">'
	+ '<path d="M95.359,55.907H100l-5.063-8.122H54.429V34.388h7'
	+ '.173L30.801,12.026l-10.548,7.658v-4.638H13.22v9.744L0,34.388h7.173 v47.996H2.'
	+ '532v5.591h45.147h3.797h47.258v-5.591h-3.375V55.907z"/></svg>';

var BUILDING = {
	NONE: 1,
	SETTLEMENT: 2,
	CITY: 3
}

/*
 * Construct a new intersection.
 * @param coord1 - the coordinate of the first adjacent tile
 * @param coord2 - the coordinate of the second adjacent tile
 * @param coord3 - the coordinate of the third adjacent tile
 */
function Intersection(coord1, coord2, coord3) {
	this.intersectCoordinates = {coord1: coord1, coord2: coord2, coord3: coord3};
	this.coordinates = findCenter(coord1, coord2, coord3);
	this.id = ("intersection-x-" + this.coordinates.x + "y-" 
			+ this.coordinates.y + "z-" + this.coordinates.z).replace(/[.]/g, "_");

	this.building = BUILDING.NONE;
	this.player;

	// Cities & Knights: whether this city has a wall.
	this.hasWall = false;

	// Cities & Knights: whether this player may place a new knight here.
	this.canBuildKnight = false;

	// Cities & Knights knight occupying this intersection: {player, tier, active}
	this.knight = null;

	this.port = PORT.NONE;

	this.highlighted = false;
	this.canBuildSettlement;

	$("#board-viewport").append("<div class='intersection-wall' id='" + this.id + "-wall'></div>");
	$("#board-viewport").append("<div class='intersection-select circle' id='" + this.id + "-select'></div>");
	$("#board-viewport").append("<div class='intersection' id='" + this.id  + "'></div>");
}

/*
 * Redraws this intersection.
 * @param transX - the x translation of the board
 * @param transY - the y translation of the board
 * @param scale - the scale to draw at
 */
Intersection.prototype.draw = function(transX, transY, scale) {
	var displacement = hexToCartesian(this.coordinates);
		
	var element = $("#" + this.id);
	element.empty();
		
	switch(this.building) {
	case BUILDING.SETTLEMENT:
		var size = scale * SETTLEMENT_SCALE;
		var x = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - size / 4;
		var y = transY + displacement.y * scale + scale / 4 - size / 2;

		// Move intersection to correct location and set size
		element.append(SETTLEMENT_SVG);
		element.css("transform", "translate(" + x + "px, " + y + "px)");
		element.attr("height", size);
		element.attr("width", size);
		
		// Scale svg element
		var svg = element[0].getElementsByTagName("svg")[0];
		svg.setAttribute("viewBox", "0 0 " + SETTLEMENT_SVG_WIDTH + " " + SETTLEMENT_SVG_WIDTH);
		svg.setAttribute("height", size);
		svg.setAttribute("width", size);
		
		// Set fill of svg element
		var svg2 = $("#" + this.id).children().first();
		svg2.css("fill", this.player.color);
		break;
	case BUILDING.CITY:
		var size = scale * CITY_SCALE;
		var x = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - size / 4;
		var y = transY + displacement.y * scale + scale / 4 - size / 2;
		
		element.append(CITY_SVG);
		element.css("transform", "translate(" + x + "px, " + y + "px)");
		element.attr("height", size);
		element.attr("width", size);
		
		var svg = element[0].getElementsByTagName("svg")[0];
		svg.setAttribute("viewBox", "0 0 " + CITY_SVG_WIDTH + " " + CITY_SVG_WIDTH);
		svg.setAttribute("height", size);
		svg.setAttribute("width", size);
		
		var svg2 = $("#" + this.id).children().first();
		svg2.css("fill", this.player.color);
		
		break;
	default:
		break;
	}
	
	// Render a Cities & Knights city wall behind the city, if present.
	var wall = $("#" + this.id + "-wall");
	wall.empty();
	if (this.building === BUILDING.CITY && this.hasWall) {
		var wSize = scale * CITY_SCALE * 1.9;
		var wx = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - wSize / 4;
		var wy = transY + displacement.y * scale + scale / 4 - wSize / 2;
		wall.append("<img src='images/city-wall.png' style='width:100%;height:100%;'>");
		wall.css("transform", "translate(" + wx + "px, " + wy + "px)");
		wall.css("height", wSize);
		wall.css("width", wSize);
	}

	// Render a Cities & Knights knight, if one occupies this intersection.
	// Knights and buildings never share an intersection, so we reuse the same
	// element the building would use.
	if (this.knight) {
		var kSize = scale * SETTLEMENT_SCALE * 1.2;
		var kx = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - kSize / 4;
		var ky = transY + displacement.y * scale + scale / 4 - kSize / 2;
		var tier = this.knight.tier === 3 ? "mighty"
				: (this.knight.tier === 2 ? "strong" : "basic");
		var owner = playersById[this.knight.player];
		var kColor = owner ? owner.color : "#000000";

		element.append("<img class='knight-icon' src='images/knight-" + tier + ".png'>");
		element.css("transform", "translate(" + kx + "px, " + ky + "px)");
		element.attr("height", kSize);
		element.attr("width", kSize);

		var kImg = element.children("img").last();
		kImg.css("width", kSize);
		kImg.css("height", kSize);
		kImg.css("box-sizing", "border-box");
		kImg.css("border", Math.max(1, scale * 0.015) + "px solid " + kColor);
		kImg.css("border-radius", "50%");
		kImg.css("background-color", kColor);
		// Inactive knights are dimmed.
		kImg.css("opacity", this.knight.active ? "1" : "0.45");
	}

	// Calculate size and displacement of selectable area
	var size = scale * SELECTABLE_AREA_SCALE;
	var x = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - size / 4 - 0.020 * scale;
	var y = transY + displacement.y * scale + scale / 4 - size / 2;
	
	if (this.building === BUILDING.SETTLEMENT) {
		x = x + 0.0075 * scale;
		y = y + 0.015 * scale;
	}

	// Add selectable area to intersection
	var select = $("#" + this.id + "-select");
	select.css("transform", "translate(" + x + "px, " + y + "px)");
	select.css("height", size);
	select.css("width", size);
}

/*
 * Adds a settlement to this intersection.
 * @param player - the player who owns the settlement
 */
Intersection.prototype.addSettlement = function(player) {
	this.building = BUILDING.SETTLEMENT;
	this.player = player;
}

/*
 * Adds a city to this intersection.
 * @param player - the player who owns the city
 */
Intersection.prototype.addCity = function(player) {
	this.building = BUILDING.CITY;
	this.player = player;
}

/*
 * Creates an intersection click handler for building cities and settlements.
 */
Intersection.prototype.createIntersectionClickHandler = function() {
	var that = this;
	return function(event) {
		if (currentMode === BUILD_MODE.CITY_WALL) {
			if (that.building === BUILDING.CITY && that.player
					&& that.player.id === playerId && !that.hasWall) {
				sendBuildCityWallAction(that.intersectCoordinates);
				exitBuildMode();
			}
			return;
		}
		if (currentMode === BUILD_MODE.KNIGHT_BUILD) {
			if (that.canBuildKnight) {
				sendBuildKnightAction(that.intersectCoordinates);
				exitBuildMode();
			}
			return;
		}
		if (currentMode === BUILD_MODE.KNIGHT_ACTIVATE) {
			if (that.knight && that.knight.player === playerId
					&& !that.knight.active) {
				sendActivateKnightAction(that.intersectCoordinates);
				exitBuildMode();
			}
			return;
		}
		if (currentMode === BUILD_MODE.KNIGHT_UPGRADE) {
			if (that.knight && that.knight.player === playerId
					&& that.knight.tier < 3) {
				sendUpgradeKnightAction(that.intersectCoordinates);
				exitBuildMode();
			}
			return;
		}
		if (currentMode === BUILD_MODE.KNIGHT_MOVE) {
			if (!knightMoveFrom) {
				// Step 1: pick one of your active knights as the source.
				if (that.knight && that.knight.player === playerId
						&& that.knight.active) {
					selectKnightMoveSource(that);
				}
			} else if (that !== knightMoveFrom && that.highlighted) {
				// Step 2: pick a highlighted destination.
				sendMoveKnightAction(knightMoveFrom.intersectCoordinates,
						that.intersectCoordinates);
				exitBuildMode();
			}
			return;
		}
		if (that.building === BUILDING.NONE) {
			if (inPlaceSettlementMode) {
				sendPlaceSettlementAction(that.intersectCoordinates);
				exitPlaceSettlementMode();
			} else {
				sendBuildSettlementAction(that.intersectCoordinates);
				exitBuildMode();
			}
		} else if (that.building === BUILDING.SETTLEMENT) {
			sendBuildCityAction(that.intersectCoordinates);
			exitBuildMode();
		}
	};
}

/*
 * Highlights this intersection.
 */
Intersection.prototype.highlight = function() {
	if (!(this.highlighted)) {
		this.highlighted = true;
		
		var select = $("#" + this.id + "-select");
		select.addClass("highlighted");
	
		select.click(this.createIntersectionClickHandler());
	}
}

/*
 * Unhighlights this intersection.
 */
Intersection.prototype.unHighlight = function() {
	if (this.highlighted) {
		this.highlighted = false;
		
		var select = $("#" + this.id + "-select");
		select.removeClass("highlighted");
	
		var that = this;
		select.off("click");
	}
}

/*
 * Creates a new intersection from the given path data.
 * @param data - the intersection data
 */
function parseIntersection(data) {
	var position = data.coordinate;
	var intersect = new Intersection(parseHexCoordinates(position.coord1),
			parseHexCoordinates(position.coord2),
			parseHexCoordinates(position.coord3))

	if (data.hasOwnProperty("building")) {
		var player = playersById[data.building.player];
		if (data.building.type === "settlement") {
			intersect.addSettlement(player);
		} else if (data.building.type === "city") {
			intersect.addCity(player);
			intersect.hasWall = !!data.building.hasWall;
		}
	}

	if (data.hasOwnProperty("knight") && data.knight) {
		intersect.knight = data.knight;
	}

	if (data.hasOwnProperty("port")) {
		switch (data.port._resource) {
			case "BRICK":
				this.port = PORT.BRICK;
				break;
			case "WOOD":
				this.port = PORT.WOOD;
				break;
			case "ORE":
				this.port = PORT.ORE;
				break;
			case "WHEAT":
				this.port = PORT.WHEAT;
				break;
			case "SHEEP":
				this.port = PORT.SHEEP;
				break;
			case "WILDCARD":
				this.port = PORT.WILDCARD;
				break;
			default:
				break;
		}
	}

	intersect.canBuildSettlement = data.canBuildSettlement;
	intersect.canBuildKnight = !!data.canBuildKnight;

	return intersect;
}
