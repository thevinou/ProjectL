/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

import java.util.Calendar;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

/**
 * @author Masterio
 */
public class RankPvpSystem 
{
	private L2PcInstance _killer = null;
	private L2PcInstance _victim = null; 
	
	private final long _protectionTime = 60000 * RankPvpSystemConfig.PROTECTION_TIME_RESET;
	private boolean _protectionTimeEnabled = false;

	public RankPvpSystem(L2PcInstance killer, L2PcInstance victim)
	{
		_victim = victim;
		_killer = killer;
	}
	
	/** 
	 * Executed when kill player (from killer side)
	 */
	public synchronized void doPvp()
	{
		if(checkBasicConditions(_killer, _victim))
		{
			// set pvp times:
			Calendar c = Calendar.getInstance();	
			long systemTime = c.getTimeInMillis(); // date & time
			
			c.set(Calendar.MILLISECOND, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.HOUR, 0);
			long systemDay = c.getTimeInMillis(); // date

			// get killer - victim pvp:
			Pvp pvp = PvpTable.getInstance().getPvp(_killer.getObjectId(), _victim.getObjectId(), systemDay, false);

			// check time protection:
			_protectionTimeEnabled = checkProtectionTime(pvp, systemTime);
			
			String nextRewardTime = "";
			if(_protectionTimeEnabled)
			{
				nextRewardTime = calculateTimeToString(systemTime, pvp.getKillTime());
			}

			// get Killer and Victim pvp stats:
			KillerPvpStats killerPvpStats = PvpTable.getInstance().getKillerPvpStats(_killer.getObjectId(), systemDay, false, false);
			KillerPvpStats victimPvpStats = PvpTable.getInstance().getKillerPvpStats(_victim.getObjectId(), systemDay, true, false);

			// update pvp:
			increasePvp(pvp, killerPvpStats, systemTime, systemDay);

			// update killer Alt+T info.
			if(RankPvpSystemConfig.LEGAL_COUNTER_ALTT_ENABLED)
			{
				_killer.setPvpKills(killerPvpStats.getTotalKillsLegal());
				_killer.sendPacket(new UserInfo(_killer));
				_killer.broadcastUserInfo(); // 
			}
			
			// start message separator:
			_killer.sendMessage("----------------------------------------------------------------");	
			_victim.sendMessage("----------------------------------------------------------------");

			// PvP Reward || Rank PvP Reward || RPC || RP
			if(checkRewardProtections(pvp))
			{
    			// give PvP Reward:
    			if(RankPvpSystemConfig.PVP_REWARD_ENABLED && checkPvpRewardConditions(pvp))
    			{
    				RewardTable.getInstance().giveReward(_killer);
    			}
    			
    			// give Rank PvP Reward:
    			if(RankPvpSystemConfig.RANK_PVP_REWARD_ENABLED && checkRankPvpRewardConditions(pvp))
    			{
    				RewardTable.getInstance().giveRankPvpRewards(_killer, victimPvpStats.getRankId());
    			}
    
    			// add RPC:
    			if(RankPvpSystemConfig.RPC_REWARD_ENABLED && checkRpcConditions(pvp))
    			{
    				RPCTable.getInstance().addRpcForPlayer(_killer.getObjectId(), RankPvpSystemConfig.RPC_REWARD_AMOUNT);
    			}
    
    			// add RP:
    			if(checkRankPointsConditions(pvp))
    			{
    				addRankPointsForKiller(pvp, killerPvpStats, victimPvpStats);
    			}
			}
			
			if(_protectionTimeEnabled)
			{
				_killer.sendMessage("Protection Time is activated for: "+ nextRewardTime);
			}
			
			// update nick and title colors:
			updateNickAndTitleColor(_killer, killerPvpStats);
	
			// show message:
			shoutPvpMessage(pvp);
			
			// end message separator:
			_killer.sendMessage("----------------------------------------------------------------");
			_victim.sendMessage("----------------------------------------------------------------");

			if(RankPvpSystemConfig.DEATH_MANAGER_DETAILS_ENABLED)
			{
				_victim.getRankPvpSystemPc().setDeathStatus(new RankPvpSystemDeathStatus(_killer));
			}
			
			if(RankPvpSystemConfig.PVP_INFO_COMMAND_ON_DEATH_ENABLED)
			{
				if(!RankPvpSystemProtection.isInDMRestrictedZone(_killer))
				{
					RankPvpSystemPvpStatus.sendPage(_victim, _killer);
				}
			}
			
			// shout defeat message, if victim have combo level > 0
			if(_victim.getRankPvpSystemPc().isComboKillActive())
			{ 
				_victim.getRankPvpSystemPc().getComboKill().shoutDefeatMessage(_victim);
				_victim.getRankPvpSystemPc().setComboKill(null); // reset current combo for victim.
			}
		}	
	}

	/** Check all conditions and increase or not PvP <br>(rank points are increased in addRankPointsForKiller() method) 
	 * @param pvp 
	 * @param killerPvpStats 
	 * @param systemTime 
	 * @param systemDay 
	 */
	private void increasePvp(Pvp pvp, KillerPvpStats killerPvpStats, long systemTime, long systemDay)
	{
		// killerPvpStats today fields are updated on the end.
		
		// add normal kills, checking is outside this method:
		pvp.increaseKills();
		killerPvpStats.addTotalKills(1);
		
		if(pvp.getKillDay() == systemDay) // daily
		{
			pvp.increaseKillsToday();
		}
		else
		{
			pvp.setKillsToday(1);
		}
		
		if(RankPvpSystemProtection.checkWar(_killer, _victim))
		{
			pvp.increaseWarKills();
			killerPvpStats.addTotalWarKills(1);
		}
		
		// shout combo kill, if legal kill protection is disabled:
		if(RankPvpSystemConfig.COMBO_KILL_ENABLED && !RankPvpSystemConfig.COMBO_KILL_PROTECTION_WITH_LEGAL_KILL_ENABLED)
		{
			shoutComboKill(systemTime);
		}

		if(checkLegalKillConditions(_killer, _victim, pvp))
		{

			if(!_protectionTimeEnabled)
			{
				
				pvp.increaseKillsLegal();
				killerPvpStats.addTotalKillsLegal(1);
				
				if(RankPvpSystemProtection.checkWar(_killer, _victim))
				{
					pvp.increaseWarKillsLegal();
					killerPvpStats.addTotalWarKillsLegal(1);
				}
				
				if(pvp.getKillDay() == systemDay)
				{
					pvp.increaseKillsLegalToday(); // daily
				}
				else
				{
					pvp.setKillsLegalToday(1); // daily
				}
				
				// shout combo kill, if legal kill protection is enabled:
				if(RankPvpSystemConfig.COMBO_KILL_ENABLED && RankPvpSystemConfig.COMBO_KILL_PROTECTION_WITH_LEGAL_KILL_ENABLED)
				{
					shoutComboKill(systemTime);
				}
				
				// if protection is OFF set the current kill time. 
				pvp.setKillTime(systemTime);
			}
	
		}
		
		if(pvp.getKillTime() == 0) // set last kill time if it is initial kill.
			pvp.setKillTime(systemTime);
		
		pvp.setKillDay(systemDay);
		
		// update daily fields for killerPvpStats:
		killerPvpStats.updateDailyStats(systemDay);
		
		// used for check not active killers on top list filter:
		killerPvpStats.setLastKillTime(systemTime);
		
	}
	
	/**
	 * Add rank points for kill, and update killer rank. Gives RPC and Rank Rewards for killer.
	 * @param pvp
	 * @param killerPvpStats
	 * @param victimPvpStats
	 */
	private void addRankPointsForKiller(Pvp pvp, KillerPvpStats killerPvpStats, KillerPvpStats victimPvpStats)
	{
		
		int[] points_table = getPointsForKill(pvp, killerPvpStats, victimPvpStats, getKiller(), getVictim());
		
		// old rank id:
		int oldRankId = killerPvpStats.getRankId();
		
		// increase rank points:
		pvp.increaseRankPointsBy(points_table[0]);
		pvp.increaseRankPointsTodayBy(points_table[0]);
		
		// required update this object for increasePvp() (only in this method):
		killerPvpStats.addTotalRankPoints(points_table[0]); // rank info updated inside.
		killerPvpStats.addTotalRankPointsToday(points_table[0]); // required for show in chat below.
		
		// add rank RPC for killer:
		if(RankPvpSystemConfig.RANK_RPC_ENABLED)
		{
			RPCTable.getInstance().addRpcForPlayer(_killer.getObjectId(), victimPvpStats.getRank().getRpc());
		}
		
		// new rank shout:
		if(oldRankId < killerPvpStats.getRankId())
		{
			_killer.sendMessage("You have reached a new rank: "+RankTable.getInstance().getRankById(killerPvpStats.getRankId()).getName());
			
			// give rank rewards for new ranks:
			if(RankPvpSystemConfig.RANK_LEVEL_REWARD_ENABLED)
			{
				// if player reached 1 or more new ranks.
    			for(int i = oldRankId+1; i<killerPvpStats.getRankId(); i++)
        		{
        			RewardTable.getInstance().giveRankLvlRewards(_killer, i);
        		}
			}
		}
		
		// shout rank informations:
		if(RankPvpSystemConfig.RANK_SHOUT_INFO_ON_KILL_ENABLED)
		{
			_killer.sendMessage("You have obtained "+points_table[0]+" Rank Points for kill "+_victim.getName()/*+" ("+victimPvpStats.getRank().getName()+")"*/);
			if(RankPvpSystemConfig.RANK_SHOUT_BONUS_INFO_ON_KILL_ENABLED && RankPvpSystemConfig.RANK_SHOUT_INFO_ON_KILL_ENABLED)
			{
				showBonusDataPointsForKiller(points_table);
			}
			_killer.sendMessage("Your Rank Points: "+killerPvpStats.getTotalRankPoints()+" ("+killerPvpStats.getTotalRankPointsToday()+" today)"/*+", current Rank: "+killerRankName*/);
			_victim.sendMessage("You have been killed by "+_killer.getName()+" ("+killerPvpStats.getRank().getName()+")");
		}
	}
	
	/**
	 * Shout current kills, kills_today, etc.
	 * @param pvp
	 */
	private void shoutPvpMessage(Pvp pvp)
	{
		
		if(RankPvpSystemConfig.TOTAL_KILLS_IN_SHOUT_ENABLED)
		{
			if(pvp.getKills() > 1)
			{
				String timeStr1 = " times";
				if(pvp.getKillsToday() == 1){ timeStr1 = "st time"; }
				String msgVictim1 = _killer.getName() + " killed you " + pvp.getKills() + " times";
				String msgVictim2 = _killer.getName() + " killed you " + pvp.getKills() + " times ("+ pvp.getKillsToday() +""+timeStr1+" today)";
				String msgKiller1 = "You have killed " + _victim.getName() + " " + pvp.getKills() + " times";
				String msgKiller2 = "You have killed " + _victim.getName() + " " + pvp.getKills() + " times ("+ pvp.getKillsToday() +""+timeStr1+" today)";
				
				if(RankPvpSystemConfig.PROTECTION_TIME_RESET == 0)
				{
					_victim.sendMessage(msgVictim1);
					_killer.sendMessage(msgKiller1);
				}
				else
				{
					_victim.sendMessage(msgVictim2);
					_killer.sendMessage(msgKiller2);
				}
			}
			else
			{
				_victim.sendMessage("This is the first time you have been killed by " + _killer.getName());
				_killer.sendMessage("You have killed " + _victim.getName() + " for the first time"); 
			}
		}
		else
		{
			if(pvp.getKillsLegal() > 1)
			{
				String timeStr1 = " times";
				if(pvp.getKillsLegalToday() == 1){ timeStr1 = "st time"; }
				String msgVictim1 = _killer.getName() + " killed you " + pvp.getKillsLegal() + " times legally";
				String msgVictim2 = _killer.getName() + " killed you " + pvp.getKillsLegal() + " times ("+ pvp.getKillsLegalToday() +""+timeStr1+" today) legally";
				String msgKiller1 = "You have killed " + _victim.getName() + " " + pvp.getKillsLegal() + " times legally";
				String msgKiller2 = "You have killed " + _victim.getName() + " " + pvp.getKillsLegal() + " times ("+ pvp.getKillsLegalToday() +""+timeStr1+" today) legally";
				
				if(RankPvpSystemConfig.PROTECTION_TIME_RESET == 0)
				{
					_victim.sendMessage(msgVictim1);
					_killer.sendMessage(msgKiller1);
				}
				else
				{
					_victim.sendMessage(msgVictim2);
					_killer.sendMessage(msgKiller2);
				}
			}
			else
			{
				_victim.sendMessage("This is the first time you have been killed by " + _killer.getName() + " legally.");
				_killer.sendMessage("You have killed " + _victim.getName() + " for the first time legally."); 
			}
		}

	}
	
	private void showBonusDataPointsForKiller(int[] points_table)
	{
		// show bonus points data for killer:
		String war = "";
		String area = "";
		String combo = "";
		
		if(points_table[1] > 0)
		{
			war = "war: "+points_table[1]+", ";
		}
		
		if(points_table[2] > 0)
		{
			area = "area: "+points_table[2]+", ";
		}
		
		if(points_table[3] > 0)
		{
			combo = "combo: "+points_table[3]+", ";
		}
		
		if(points_table[1] > 0 || points_table[2] > 0 || points_table[3] > 0)
		{			
			String msg = war+area+combo;
			msg = msg.substring(0, msg.length()-2);
			
			_killer.sendMessage("Bonus RP ("+msg+")");
		}
		
	}
	
	/**
	 * Update nick and title color for character with specified rank.
	 * @param killer - can not be null
	 * @param killerPvpStats - if null then pvpStats will be found.
	 */
	public static void updateNickAndTitleColor(L2PcInstance killer, KillerPvpStats killerPvpStats)
	{
		if(killer == null)
			return;
		
		if(!RankPvpSystemConfig.GM_IGNORE_ENABLED && killer.isGM())
			return;
		
		KillerPvpStats pvpStats = killerPvpStats;
		
		if(pvpStats == null)
		{
			pvpStats = PvpTable.getInstance().getKillerPvpStats(killer.getObjectId(), true, false);
			if(pvpStats == null)
				return;
		}

		Rank rank = pvpStats.getRank();
		
		if(rank == null)
			return;
		
		if(RankPvpSystemConfig.NICK_COLOR_ENABLED && killer.getAppearance().getNameColor() != rank.getNickColor() && rank.getNickColor() > -1)
		{
			killer.getAppearance().setNameColor(rank.getNickColor());
			killer.sendPacket(new UserInfo(killer));
			killer.broadcastUserInfo();
		}
		
		if(RankPvpSystemConfig.TITLE_COLOR_ENABLED && killer.getAppearance().getTitleColor() != rank.getTitleColor() && rank.getTitleColor() > -1)
		{
			killer.getAppearance().setTitleColor(rank.getTitleColor());
			killer.broadcastUserInfo();
		}
	}
	
	public static final String calculateTimeToString(long sys_time, long kill_time)
	{
		long TimeToRewardInMilli = ((kill_time + (60000 * RankPvpSystemConfig.PROTECTION_TIME_RESET)) - sys_time);
        long TimeToRewardHours = TimeToRewardInMilli / 3600000;
        long TimeToRewardMinutes = (TimeToRewardInMilli % 3600000) / 60000;
        long TimeToRewardSeconds = (TimeToRewardInMilli % 60000) / 1000;
        
        String H = Long.toString(TimeToRewardHours);
        String M = Long.toString(TimeToRewardMinutes);
        String S = Long.toString(TimeToRewardSeconds);
        
        if(TimeToRewardHours <= 9){ H = "0" + H; }
        if(TimeToRewardMinutes <= 9){ M = "0" + M; }
        if(TimeToRewardSeconds <= 9){ S = "0" + S; }
        
        return H+":"+M+":"+S;
	}
	
	/**
	 * Calculate Rank Points awarded for Killer<br> [0] - Sum of Rank Points and Bonus Points. <br> [1] - Bonus points for War.<br>[2] - Bonus points for Area.<br>[3] - Bonus points for Combo.
	 * @param pvp 
	 * @param killerPvpStats 
	 * @param victimPvpStats 
	 * @param killer 
	 * @param victim 
	 * @return Returns table of Rank Points awarded for Killer<br> [0] - Sum of Rank Points and Bonus Points. <br> [1] - Bonus points for War.<br>[2] - Bonus points for Area.<br>[3] - Bonus points for Combo.
	 */
	private int[] getPointsForKill(Pvp pvp, KillerPvpStats killerPvpStats, KillerPvpStats victimPvpStats, L2PcInstance killer, L2PcInstance victim)
	{
		
		int points = 0;
		int points_war = 0;
		int points_bonus_zone = 0;
		int points_combo = 0;

		// add basic points:
		if(RankPvpSystemConfig.RANK_POINTS_DOWN_COUNT_ENABLED)
		{
			int i = 1;
			
			for (Integer value : RankPvpSystemConfig.RANK_POINTS_DOWN_AMOUNTS) 
			{
				if(pvp.getKillsLegalToday() == i)
				{
					points = value;
					break;
				}
				i++;
		    }
		}
		else
		{
			points = victimPvpStats.getRank().getPointsForKill();
		}
		
		// cut points if enabled:
		if(RankPvpSystemConfig.RANK_POINTS_CUT_ENABLED && killerPvpStats.getRank().getPointsForKill() < points)
		{
			points = killerPvpStats.getRank().getPointsForKill();
		}
		
		// add war points, if Killer's clan and Victim's clan at war:
		if(RankPvpSystemConfig.WAR_KILLS_ENABLED && points > 0 && RankPvpSystemConfig.WAR_RANK_POINTS_RATIO > 1 && RankPvpSystemProtection.checkWar(killer, victim))
		{
			points_war = (int) Math.floor((points * RankPvpSystemConfig.WAR_RANK_POINTS_RATIO) - points);
		}
		
		// add bonus zone points, if Killer is inside bonus zone:
		if(points > 0)
		{
			double zone_ratio_killer = RankPvpSystemProtection.getZoneBonusRatio(killer);
			if(zone_ratio_killer > 1)
			{
				points_bonus_zone = (int) Math.floor((points * zone_ratio_killer) - points);
			}
		}
		
		// add combo points:
		if(RankPvpSystemConfig.COMBO_KILL_RANK_POINTS_RATIO_ENABLED && killer.getRankPvpSystemPc().getComboKill() != null)
		{
			double combo_ratio = killer.getRankPvpSystemPc().getComboKill().getComboKillRankPointsRatio();
			if(combo_ratio > 1)
			{
				points_combo = (int) Math.floor((points * combo_ratio) - points);
			}
		}

		points = points + points_war + points_bonus_zone + points_combo;

		int[] points_table = new int[4];
		points_table[0] = points;
		points_table[1] = points_war;
		points_table[2] = points_bonus_zone;
		points_table[3] = points_combo;
		
		return points_table;
	}
	
	/**
	 * Method used for Combo Kill System.
	 * @param killTime 
	 */
	private void shoutComboKill(long killTime)
	{

		// create new combo instance if not exists:
		if(_killer.getRankPvpSystemPc().getComboKill() == null)
		{
			_killer.getRankPvpSystemPc().setComboKill(new RankPvpSystemComboKill());
		}
		// reset old combo if kill reseter enabled
		else if(RankPvpSystemConfig.COMBO_KILL_RESETER > 0 && (killTime - _killer.getRankPvpSystemPc().getComboKill().getLastKillTime()) > RankPvpSystemConfig.COMBO_KILL_RESETER * 1000)
		{
			_killer.getRankPvpSystemPc().setComboKill(new RankPvpSystemComboKill());
		}
		
		// rise combo level and shout message:
		if(_killer.getRankPvpSystemPc().getComboKill().addVictim(_victim.getObjectId(), killTime))
		{
			_killer.getRankPvpSystemPc().getComboKill().shoutComboKill(_killer, _victim);
		}

	}

	
	/**
	 * Returns TRUE if protection time is activated.
	 * @param pvp
	 * @param systemTime
	 * @return
	 */
	private boolean checkProtectionTime(Pvp pvp, long systemTime)
	{
		if(RankPvpSystemConfig.PROTECTION_TIME_RESET > 0)
		{
			if(pvp.getKillTime() + _protectionTime > systemTime)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check Basic conditions for RPS, it's mean check if can I add +1 into kills and kills_today.<br>
	 * Basic mean: if killer is: in olympiad, in event, in restricted zone, etc.
	 * @param killer 
	 * @param victim 
	 * @return TRUE if conditions are correct.
	 */
	private boolean checkBasicConditions(L2PcInstance killer, L2PcInstance victim)
	{

		if(killer == null || victim == null)
		{
			return false;
		}
		
		if(killer.isDead() || killer.isAlikeDead())
		{
			return false;
		}
		
		if(RankPvpSystemProtection.checkEvent(killer))
		{
			return false;
		}
		
		if(RankPvpSystemConfig.GM_IGNORE_ENABLED && (killer.isGM() || victim.isGM()))
		{
			killer.sendMessage("Rank PvP System ignore GM characters!");
			return false;
		}
		
		// check if killer is in allowed zone & not in restricted zone:
		if(!RankPvpSystemProtection.isInPvpAllowedZone(killer) || RankPvpSystemProtection.isInPvpRestrictedZone(killer))
		{
			if((RankPvpSystemConfig.RPC_REWARD_ENABLED || RankPvpSystemConfig.PVP_REWARD_ENABLED || RankPvpSystemConfig.RANK_PVP_REWARD_ENABLED) && RankPvpSystemConfig.RANKS_ENABLED)
			{
				killer.sendMessage("You can't earn Reward or Rank Points in restricted zone");
				return false;
			}
			
			return false;
		}
		
		if(!RankPvpSystemProtection.antiFarmCheck(killer, victim))
		{
			return false;
		}

		return true;
	}
	
	private boolean checkLegalKillProtection(Pvp pvp)
	{
		// 1: check total legal kills:
		if(RankPvpSystemConfig.LEGAL_KILL_PROTECTION > 0 && pvp.getKillsLegal() > RankPvpSystemConfig.LEGAL_KILL_PROTECTION)
		{
			return false;
		}
		
		// 2: check total legal kills today:
		if(RankPvpSystemConfig.DAILY_LEGAL_KILL_PROTECTION > 0 && pvp.getKillsLegalToday() > RankPvpSystemConfig.DAILY_LEGAL_KILL_PROTECTION)
		{
			return false;
		}
		
		// 3. check protectionTimeEnabled
		if(_protectionTimeEnabled)
		{
			return false;
		}

		return true;
	}
	
	/**
	 * Returns TRUE if okay.<br>
	 * Check the reward protections like:<br>
	 *  REWARD_FOR_INNOCENT_KILL, <br>REWARD_FOR_PK_KILLER, <br>REWARD_LEGAL_KILL.
	 * @param pvp
	 * @return
	 */
	private boolean checkRewardProtections(Pvp pvp)
	{
		// if PK mode is disabled:
		if(!RankPvpSystemConfig.REWARD_FOR_INNOCENT_KILL_ENABLED && _victim.getPvpFlag() == 0 && _victim.getKarma() == 0)
		{
			_killer.sendMessage("You can't earn reward on innocent players!");
			return false;
		}
		
		// if reward for PK kill is disabled:
		if(!RankPvpSystemConfig.REWARD_FOR_PK_KILLER_ENABLED && _victim.getKarma() > 0)
		{
			_killer.sendMessage("No reward for kill player with Karma!");
			return false;
		}
		
		if(RankPvpSystemConfig.REWARD_LEGAL_KILL_ENABLED && !checkLegalKillProtection(pvp))
		{
			return false;
		}
		
		return true;
	}

	/**
	 * Return True if it's Legal Kill (without farm check).
	 * @param killer
	 * @param victim
	 * @param pvp 
	 * @return
	 */
	private boolean checkLegalKillConditions(L2PcInstance killer, L2PcInstance victim, Pvp pvp)
	{
		if((RankPvpSystemConfig.LEGAL_KILL_MIN_LVL > victim.getLevel()) || (RankPvpSystemConfig.LEGAL_KILL_MIN_LVL > killer.getLevel()))
		{
			return false;
		}
		
		if(!RankPvpSystemConfig.LEGAL_KILL_FOR_INNOCENT_KILL_ENABLED && victim.getKarma() == 0 && victim.getPvpFlag() == 0)
		{
			return false;
		}
		
		if(!RankPvpSystemConfig.LEGAL_KILL_FOR_PK_KILLER_ENABLED && victim.getKarma() > 0)
		{
			return false;
		}
		
		if(!checkLegalKillProtection(pvp))
		{
			return false;
		}
		
		return true;
	}
	
	private boolean checkRpcConditions(Pvp pvp)
	{
		
		if(!RankPvpSystemConfig.RPC_REWARD_ENABLED)
		{
			return false;
		}
		
		if((RankPvpSystemConfig.RPC_REWARD_MIN_LVL > _victim.getLevel()) || (RankPvpSystemConfig.RPC_REWARD_MIN_LVL > _killer.getLevel()))
		{
			_killer.sendMessage("You or your target have not required level!");
			return false;
		}

		return true;
	}
	
	private boolean checkPvpRewardConditions(Pvp pvp)
	{
		
		if(!RankPvpSystemConfig.PVP_REWARD_ENABLED)
		{
			return false;
		}
		
		if((RankPvpSystemConfig.PVP_REWARD_MIN_LVL > _victim.getLevel()) || (RankPvpSystemConfig.PVP_REWARD_MIN_LVL > _killer.getLevel()))
		{
			_killer.sendMessage("You or your target have not required level!");
			return false;
		}

		return true;
	}
	
	private boolean checkRankPvpRewardConditions(Pvp pvp)
	{
		
		if(!RankPvpSystemConfig.RANK_PVP_REWARD_ENABLED)
		{
			return false;
		}
		
		if((RankPvpSystemConfig.RANK_PVP_REWARD_MIN_LVL > _victim.getLevel()) || (RankPvpSystemConfig.RANK_PVP_REWARD_MIN_LVL > _killer.getLevel()))
		{
			_killer.sendMessage("You or your target have not required level!");
			return false;
		}

		return true;
	}
	
	private boolean checkRankPointsConditions(Pvp pvp)
	{
		
		if(!RankPvpSystemConfig.RANKS_ENABLED)
			return false;
		
		if((RankPvpSystemConfig.RANK_POINTS_MIN_LVL > _victim.getLevel()) || (RankPvpSystemConfig.RANK_POINTS_MIN_LVL > _killer.getLevel()))
		{
			_killer.sendMessage("You or your target have not required level!");
			return false;
		}
		
		return true;
	}

	/**
	 * @return the _killer
	 */
	public L2PcInstance getKiller()
	{
		return _killer;
	}

	/**
	 * @param killer the _killer to set
	 */
	public void setKiller(L2PcInstance killer)
	{
		_killer = killer;
	}

	/**
	 * @return the _victim
	 */
	public L2PcInstance getVictim()
	{
		return _victim;
	}

	/**
	 * @param victim the _victim to set
	 */
	public void setVictim(L2PcInstance victim)
	{
		_victim = victim;
	}
}