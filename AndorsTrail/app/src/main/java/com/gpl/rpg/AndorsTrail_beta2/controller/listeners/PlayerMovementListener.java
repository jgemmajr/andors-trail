package com.gpl.rpg.AndorsTrail_beta2.controller.listeners;

import com.gpl.rpg.AndorsTrail_beta2.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail_beta2.util.Coord;

public interface PlayerMovementListener {
	void onPlayerMoved(PredefinedMap map, Coord newPosition, Coord previousPosition);
	void onPlayerEnteredNewMap(PredefinedMap map, Coord p);
}
