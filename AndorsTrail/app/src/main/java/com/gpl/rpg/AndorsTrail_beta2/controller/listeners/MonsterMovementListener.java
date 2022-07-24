package com.gpl.rpg.AndorsTrail_beta2.controller.listeners;

import com.gpl.rpg.AndorsTrail_beta2.model.actor.Monster;
import com.gpl.rpg.AndorsTrail_beta2.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail_beta2.util.CoordRect;

public interface MonsterMovementListener {
	void onMonsterSteppedOnPlayer(Monster m);
	void onMonsterMoved(PredefinedMap map, Monster m, CoordRect previousPosition);
}
