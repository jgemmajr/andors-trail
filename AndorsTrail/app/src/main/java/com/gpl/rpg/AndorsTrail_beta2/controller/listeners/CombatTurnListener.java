package com.gpl.rpg.AndorsTrail_beta2.controller.listeners;

import com.gpl.rpg.AndorsTrail_beta2.model.actor.Monster;

public interface CombatTurnListener {
	void onCombatStarted();
	void onCombatEnded();
	void onNewPlayerTurn();
	void onMonsterIsAttacking(Monster m);
}
