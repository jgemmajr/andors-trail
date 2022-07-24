package com.gpl.rpg.AndorsTrail_beta2.controller.listeners;

import com.gpl.rpg.AndorsTrail_beta2.model.actor.Monster;
import com.gpl.rpg.AndorsTrail_beta2.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail_beta2.util.Coord;
import com.gpl.rpg.AndorsTrail_beta2.util.CoordRect;

public interface MonsterSpawnListener {
	void onMonsterSpawned(PredefinedMap map, Monster m);
	void onMonsterRemoved(PredefinedMap map, Monster m, CoordRect previousPosition);
	void onSplatterAdded(PredefinedMap map, Coord p);
	void onSplatterChanged(PredefinedMap map, Coord p);
	void onSplatterRemoved(PredefinedMap map, Coord p);
}
