package com.gpl.rpg.AndorsTrail_beta2.controller.listeners;

import com.gpl.rpg.AndorsTrail_beta2.model.map.LayeredTileMap;
import com.gpl.rpg.AndorsTrail_beta2.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail_beta2.util.Coord;

public interface MapLayoutListener {
	void onLootBagCreated(PredefinedMap map, Coord p);
	void onLootBagRemoved(PredefinedMap map, Coord p);
	void onMapTilesChanged(PredefinedMap map, LayeredTileMap tileMap);
}
