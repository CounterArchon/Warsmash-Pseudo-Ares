package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttackMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver.TargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public class CAbilityAttack extends AbstractCAbility {

	public CAbilityAttack(final int handleId) {
		super(handleId);
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (unit.getCurrentAttacks().isEmpty()) {
			receiver.disabled();
		}
		else {
			receiver.useOk();
		}
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target == unit) {
			receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_SELF);
			return; // no attacking self ever
		}
		if (orderId == OrderIds.smart) {
			if (target instanceof CUnit) {
				if (game.getPlayer(unit.getPlayerIndex()).hasAlliance(((CUnit) target).getPlayerIndex(),
						CAllianceType.PASSIVE)) {
					receiver.orderIdNotAccepted();
					return;
				}
			}
			else if (target instanceof CDestructable) {
				// fall thru to below
			}
			else {
				receiver.orderIdNotAccepted();
				return;
			}
		}
		if ((orderId == OrderIds.smart) || (orderId == OrderIds.attack)) {
			boolean canTarget = false;
			CUnitAttack lastUnavailableAttack = null;
			for (final CUnitAttack attack : unit.getCurrentAttacks()) {
				if (target.canBeTargetedBy(game, unit, attack.getTargetsAllowed(), BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
					canTarget = true;
					break;
				} else {
					lastUnavailableAttack = attack;
				}
			}
			if (canTarget) {
				receiver.targetOk(target);
			}
			else {
				if(lastUnavailableAttack != null) {
					// a check known to fail, so it will populate our receiver
					target.canBeTargetedBy(game, unit, lastUnavailableAttack.getTargetsAllowed(), receiver);
				} else {
					receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
				}
			}
		}
		else {
			receiver.orderIdNotAccepted();
		}
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTarget target) {
		return true;
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		switch (orderId) {
		case OrderIds.attack:
			receiver.targetOk(target);
			break;
		case OrderIds.attackground:
			boolean allowAttackGround = false;
			for (final CUnitAttack attack : unit.getCurrentAttacks()) {
				if (attack.getWeaponType().isAttackGroundSupported()) {
					allowAttackGround = true;
					break;
				}
			}
			if (allowAttackGround) {
				receiver.targetOk(target);
			}
			else {
				receiver.orderIdNotAccepted();
			}
			break;
		default:
			receiver.orderIdNotAccepted();
			break;
		}
	}

	@Override
	public void checkCanTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		unit.setAttackBehavior(new CBehaviorAttack(unit));
		if (!unit.isMovementDisabled()) {
			unit.setAttackMoveBehavior(new CBehaviorAttackMove(unit));
		}
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId, final CWidget target) {
		CBehavior behavior = null;
		for (final CUnitAttack attack : caster.getCurrentAttacks()) {
			if (target.canBeTargetedBy(game, caster, attack.getTargetsAllowed(), BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				behavior = caster.getAttackBehavior().reset(OrderIds.attack, attack, target, false,
						CBehaviorAttackListener.DO_NOTHING);
				break;
			}
		}
		if (behavior == null) {
			behavior = caster.getMoveBehavior().reset(OrderIds.attack, target);
		}
		return behavior;
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget point) {
		switch (orderId) {
		case OrderIds.attack:
			if (caster.getAttackMoveBehavior() == null) {
				return caster.pollNextOrderBehavior(game);
			}
			caster.setDefaultBehavior(caster.getAttackMoveBehavior());
			return caster.getAttackMoveBehavior().reset(point);
		case OrderIds.attackground:
			CBehavior behavior = null;
			for (final CUnitAttack attack : caster.getCurrentAttacks()) {
				if (attack.getWeaponType().isAttackGroundSupported()) {
					behavior = caster.getAttackBehavior().reset(OrderIds.attackground, attack, point, false,
							CBehaviorAttackListener.DO_NOTHING);
					break;
				}
			}
			if (behavior == null) {
				behavior = caster.getMoveBehavior().reset(OrderIds.attackground, point);
			}
			return behavior;
		default:
			return caster.pollNextOrderBehavior(game);
		}
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int orderId) {
		return caster.pollNextOrderBehavior(game);
	}

	@Override
	public <T> T visit(final CAbilityVisitor<T> visitor) {
		return visitor.accept(this);
	}

	@Override
	public void onCancelFromQueue(final CSimulation game, final CUnit unit, final int orderId) {
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit cUnit) {
	}

}
