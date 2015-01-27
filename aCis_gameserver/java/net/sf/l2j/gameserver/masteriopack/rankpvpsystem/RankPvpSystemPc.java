package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Class used in L2PcInstance. Contains some system variables used in game.
 * From Killer side.
 * @author Masterio
 */
public class RankPvpSystemPc 
{
	private RankPvpSystemDeathStatus _deathStatus = null;
	private RankPvpSystemComboKill _comboKill = null;
	
	private L2PcInstance _target = null;
	
	public void runPvpTask(L2PcInstance player, L2Character target)
	{
		if(RankPvpSystemConfig.RANK_PVP_SYSTEM_ENABLED)
		{
			if(player != null && target != null && target instanceof L2PcInstance)
			{
				// set Victim handler for Killer
				//setTarget((L2PcInstance)target);	// [not required]
				
				// set Killer handler for Victim
				((L2PcInstance)target).getRankPvpSystemPc().setTarget(player);
				
				ThreadPoolManager.getInstance().executeTask(new RankPvpSystemPvpTask(player, (L2PcInstance)target));
			}
		}
	}

	public class RankPvpSystemPvpTask implements Runnable
	{
		private L2PcInstance _killer = null;
		private L2PcInstance _victim = null;
		
		public RankPvpSystemPvpTask(L2PcInstance killer, L2PcInstance victim)
		{
			_killer = killer;
			_victim = victim;
		}
		
		@Override
		public void run() 
		{
			RankPvpSystem rps = new RankPvpSystem(_killer, _victim);
				
			rps.doPvp();
		}
	}

	public RankPvpSystemDeathStatus getDeathStatus()
	{
		return _deathStatus;
	}
	
	public boolean isDeathStatusActive()
	{
		if(_deathStatus == null)
			return false;
		
		return true;
	}

	public void setDeathStatus(RankPvpSystemDeathStatus deathStatus)
	{
		_deathStatus = deathStatus;
	}

	public RankPvpSystemComboKill getComboKill()
	{
		return _comboKill;
	}
	
	public boolean isComboKillActive()
	{
		if(_comboKill == null)
			return false;
		
		return true;
	}

	public void setComboKill(RankPvpSystemComboKill comboKill) 
	{
		_comboKill = comboKill;
	}

	/**
	 * The player's Target.
	 * @return
	 */
	public L2PcInstance getTarget() 
	{
		return _target;
	}

	/** 
	 * The player's Target.
	 * @param target
	 */
	public void setTarget(L2PcInstance target) 
	{
		_target = target;
	}
}
