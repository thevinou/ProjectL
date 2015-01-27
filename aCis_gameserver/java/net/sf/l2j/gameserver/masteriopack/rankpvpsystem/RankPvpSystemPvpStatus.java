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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.masteriopack.imageconverter.ServerSideImage;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Masterio
 */
public class RankPvpSystemPvpStatus
{
	private static final Logger log = Logger.getLogger(RankPvpSystemPvpStatus.class.getName());
	
	/**
	 * Show HTM page for player.
	 * @param player
	 * @param playerTarget
	 */
	public static void sendPage(L2PcInstance player, L2PcInstance playerTarget)
	{
		NpcHtmlMessage n = new NpcHtmlMessage(0);
		
		n.setHtml(preparePage(player, playerTarget));

		player.sendPacket(n);
	}
	
	private static String preparePage(L2PcInstance player, L2PcInstance playerTarget)
	{
		String tb = "";

		// get PvP object with target. (for get how many times he killed player):
		Pvp pvp1 = new Pvp();
		Pvp pvp2 = new Pvp();
		
		if(player.getObjectId() != playerTarget.getObjectId())
		{
			pvp1 = PvpTable.getInstance().getPvp(playerTarget.getObjectId(), player.getObjectId(), true, false); // pvp: target -> player
			pvp2 = PvpTable.getInstance().getPvp(player.getObjectId(), playerTarget.getObjectId(), true, false); // pvp: player -> target
		}

		// get target PvpStats:
		KillerPvpStats targetPvpStats = PvpTable.getInstance().getKillerPvpStats(playerTarget.getObjectId(), true, false);

		tb += "<html><title>"+playerTarget.getName()+" PvP Status</title><body>";

		tb += rankImgTableHtml(player, targetPvpStats);
		
		if(player.equals(playerTarget))
		{
			tb += expBelt(player, targetPvpStats);
		}
		else
		{
			//span
			tb += "<br>";
		}
		
		
		// about player target:
		tb += "<center><table border=0 cellspacing=0 cellpadding=0>";

		//name [level]
		if(RankPvpSystemConfig.SHOW_PLAYER_LEVEL_IN_PVPINFO_ENABLED)
		{
			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Name [lvl]</font></td><td width=135 height=22 align=left><font color=ffa000>"+playerTarget.getName()+" ["+playerTarget.getLevel()+"]</font></td></tr>";
		}
		else
		{
			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Name</font></td><td width=135 height=22 align=left><font color=ffa000>"+playerTarget.getName()+"</font></td></tr>";
		}
		
		//current class
		tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Current class</font></td><td width=135 height=22 align=left><font color=ffa000>"+RankPvpSystemUtil.getClassName(playerTarget.getClassId().getId())+"</font></td></tr>";

		//main class
		if(playerTarget.getBaseClass() != playerTarget.getClassId().getId())
		{
			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Main class</font></td><td width=135 height=22 align=left><font color=ffa000>"+RankPvpSystemUtil.getClassName(playerTarget.getBaseClass())+"</font></td></tr>";
		}
		
		//nobles / hero
		tb += "<tr><td width=135 height=22 align=rigth><font color=ae9977>Nobles/Hero</font></td><td width=135 height=22 align=left><font color=808080>";
		
		if(playerTarget.isNoble())
		{
			tb += "Yes / ";
		}
		else
		{
			tb += "No / ";
		}
		
		if(playerTarget.isHero())
		{
			tb += "Yes";
		}
		else
		{
			tb += "No";
		}
		
		tb += "</font></td></tr>";
		
		
		//clan
		if(player.isDead() && !player.equals(playerTarget))
		{
			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Clan</font></td><td width=135 height=22 align=left>";
			
			if(playerTarget.getClan() != null)
			{
				tb += "<font color=ffa000>"+playerTarget.getClan().getName()+"</font>";
			}
			else
			{
				tb += "<font color=808080>No clan</font>";
			}
			
			tb += "</td></tr>";
		}
		
		//span
		tb += "<tr><td width=135 HEIGHT=1><img src=\"L2UI.Squaregray\" width=\"135\" height=\"1\"></img></td><td width=135 HEIGHT=1><img src=\"L2UI.Squaregray\" width=\"135\" height=\"1\"></img></td></tr><tr><td width=135 height=12></td><td width=135 height=12></td></tr>";
		
		
		if(RankPvpSystemConfig.RANKS_ENABLED)
		{
			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Rank</font></td><td width=135 height=22 align=left><font color=ffff00>"+targetPvpStats.getRank().getName()+"</font></td></tr>";

			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Rank Points</font></td><td width=135 height=22 align=left><font color=ffa000>"+RankPvpSystemUtil.preparePrice(targetPvpStats.getTotalRankPoints())+"</font></td></tr>";
		}	
		
		if(RankPvpSystemConfig.TOTAL_KILLS_IN_PVPINFO_ENABLED)
		{
			// legal/total kills:
			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Legal/Total Kills</font></td><td width=135 height=22 align=left><font color=ffa000>"+RankPvpSystemUtil.preparePrice(targetPvpStats.getTotalKillsLegal())+" / "+RankPvpSystemUtil.preparePrice(targetPvpStats.getTotalKills())+"</font></td></tr>";
		}
		else
		{
			// legal kills:
			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Legal Kills</font></td><td width=135 height=22 align=left><font color=ffa000>"+RankPvpSystemUtil.preparePrice(targetPvpStats.getTotalKillsLegal())+"</font></td></tr>";
		}
		
		// war kills
		if(RankPvpSystemConfig.WAR_KILLS_ENABLED)
		{
			
			if(RankPvpSystemConfig.TOTAL_KILLS_IN_PVPINFO_ENABLED)
			{
				// war legal/total kills:
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Legal/Total War Kills</font></td><td width=135 height=22 align=left><font color=2080D0>"+RankPvpSystemUtil.preparePrice(targetPvpStats.getTotalWarKillsLegal())+" / "+RankPvpSystemUtil.preparePrice(targetPvpStats.getTotalWarKills())+"</font></td></tr>";
			}
			else
			{
				// war legal kills:
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Legal War Kills</font></td><td width=135 height=22 align=left><font color=2080D0>"+RankPvpSystemUtil.preparePrice(targetPvpStats.getTotalWarKillsLegal())+"</font></td></tr>";
			}
		}
		
		//span
		tb += "<tr><td width=135 HEIGHT=1><img src=\"L2UI.Squaregray\" width=\"135\" height=\"1\"></img></td><td width=135 HEIGHT=1><img src=\"L2UI.Squaregray\" width=\"135\" height=\"1\"></img></td></tr><tr><td width=135 height=12></td><td width=135 height=12></td></tr>";

		// Rank Points for Rank
		if(RankPvpSystemConfig.RANKS_ENABLED)
		{
			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>RP for kill</font></td><td width=135 height=22 align=left><font color=ffa000>"+targetPvpStats.getRank().getPointsForKill()+"</font>";

			if(RankPvpSystemConfig.RANK_POINTS_CUT_ENABLED)
			{
				// get player PvpStats:
				KillerPvpStats playerPvpStats = PvpTable.getInstance().getKillerPvpStats(player.getObjectId(), true, false);
				
				if(playerPvpStats.getRank().getPointsForKill() < targetPvpStats.getRank().getPointsForKill())
				{
					tb += "<font color=ff0000> ["+playerPvpStats.getRank().getPointsForKill()+"]</font>";
				}
			}
				
			tb += "</td></tr>";
		}
		
		// PvP + Rank RPC Reward
		// (PvP + Rank || PvP || Rank) RPC Reward
		if(RankPvpSystemConfig.RANK_RPC_ENABLED || RankPvpSystemConfig.RPC_REWARD_ENABLED)
		{
			if(RankPvpSystemConfig.RANK_RPC_ENABLED && RankPvpSystemConfig.RPC_REWARD_ENABLED)
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>PvP + Rank RPC Reward</font></td><td width=135 height=22 align=left><font color=ffa000>("+RankPvpSystemConfig.RPC_REWARD_AMOUNT+" + "+targetPvpStats.getRank().getRpc()+")</font><font color=FFFF00> RPC</font></td></tr>";
    		
			else if(!RankPvpSystemConfig.RANK_RPC_ENABLED && RankPvpSystemConfig.RPC_REWARD_ENABLED)
    			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>PvP RPC Reward</font></td><td width=135 height=22 align=left><font color=ffa000>"+RankPvpSystemConfig.RPC_REWARD_AMOUNT+" </font><font color=FFFF00> RPC</font></td></tr>";
    		
			else if(RankPvpSystemConfig.RANK_RPC_ENABLED)
    			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Rank RPC Reward</font></td><td width=135 height=22 align=left><font color=ffa000>"+targetPvpStats.getRank().getRpc()+" </font><font color=FFFF00> RPC</font></td></tr>";
		}
		
		// PvP + Rank Reward (no RPC)
		// (PvP + Rank || PvP || Rank) Reward
		if(RankPvpSystemConfig.RANK_PVP_REWARD_ENABLED || RankPvpSystemConfig.PVP_REWARD_ENABLED)
		{
			String list_button = "<a action=\"bypass RPS.RewardList:"+targetPvpStats.getRankId()+",1\">Show</a>";
			
			if(RankPvpSystemConfig.RANK_PVP_REWARD_ENABLED && RankPvpSystemConfig.PVP_REWARD_ENABLED)
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>PvP + Rank Reward List</font></td><td width=135 height=22 align=left>"+list_button+"</td></tr>";
    		
			else if(!RankPvpSystemConfig.RANK_PVP_REWARD_ENABLED && RankPvpSystemConfig.PVP_REWARD_ENABLED)
    		{
    			String item_name = ItemTable.getInstance().getTemplate(RankPvpSystemConfig.PVP_REWARD_ID).getName();
    			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>PvP Reward</font></td><td width=135 height=22 align=left><font color=ffa000>"+RankPvpSystemConfig.PVP_REWARD_AMOUNT+" </font>x<font color=FFFF00> "+item_name+"</font></td></tr>";
    		}
			else if(RankPvpSystemConfig.RANK_PVP_REWARD_ENABLED)
    			tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Rank Reward List</font></td><td width=135 height=22 align=left>"+list_button+"</td></tr>";
		}
		
		// protection
		if(!player.equals(playerTarget) && RankPvpSystemConfig.PROTECTION_TIME_RESET > 0)
		{
			if((RankPvpSystemConfig.PVP_REWARD_ENABLED || RankPvpSystemConfig.RPC_REWARD_ENABLED || RankPvpSystemConfig.RANK_RPC_ENABLED) && RankPvpSystemConfig.RANKS_ENABLED)
			{
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>RP/Reward Protection</font></td>";
			}
			else if(RankPvpSystemConfig.RANKS_ENABLED)
			{
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Rank Points Protection</font></td>";
			}
			else if((RankPvpSystemConfig.PVP_REWARD_ENABLED || RankPvpSystemConfig.RPC_REWARD_ENABLED || RankPvpSystemConfig.RANK_RPC_ENABLED))
			{
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Reward Protection</font></td>";
			}
			
			long sys_time = System.currentTimeMillis();

			if(RankPvpSystemConfig.PROTECTION_TIME_RESET > 0 && (sys_time - (60000 * RankPvpSystemConfig.PROTECTION_TIME_RESET) < pvp2.getKillTime()))
			{
				tb += "<td width=135 height=22 align=left><font color=FFFF00>"+RankPvpSystem.calculateTimeToString(sys_time, pvp2.getKillTime())+"</font></td></tr>";
			}
			else
			{
				tb += "<td width=135 height=22 align=left><font color=00FF00>OFF</font></td></tr>";
			}
		}

		tb += "</table>";
		
		if(!player.equals(playerTarget))
		{
			//span
			tb += "<table border=0 cellspacing=0 cellpadding=0><tr><td width=135 HEIGHT=1><img src=\"L2UI.Squaregray\" width=\"135\" height=\"1\"></img></td><td width=135 HEIGHT=1><img src=\"L2UI.Squaregray\" width=\"135\" height=\"1\"></img></td></tr><tr><td width=135 height=12></td><td width=135 height=12></td></tr>";

			if(RankPvpSystemConfig.TOTAL_KILLS_ON_ME_IN_PVPINFO_ENABLED)
			{
				// legal/total kills on me:
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Legal/Total Kills on Me</font></td><td width=135 height=22 align=left><font color=FF00FF>"+pvp1.getKillsLegal()+" / "+pvp1.getKills()+"</font></td></tr>";
			}
			else
			{
				// legal kills on me:
				tb += "<tr><td width=135 height=22 align=left><font color=ae9977>Legal Kills on Me</font></td><td width=135 height=22 align=left><font color=FF00FF>"+pvp1.getKillsLegal()+"</font></td></tr>";
			}
			
			tb += "</table>";
		}

		if(RankPvpSystemConfig.RPC_EXCHANGE_ENABLED && player.equals(playerTarget) && (RankPvpSystemConfig.RANK_RPC_ENABLED || RankPvpSystemConfig.RPC_REWARD_ENABLED))
		{
			// button RPC Exchanger:
			tb += "<table border=0 cellspacing=0 cellpadding=0><tr><td width=270 HEIGHT=1><img src=\"L2UI.Squaregray\" width=\"270\" height=\"1\"></img></td></tr><tr><td width=270 height=12></td></tr><tr><td width=270 align=center><button value=\"RPC Exchange\" action=\"bypass RPS.RPC:1\" width="+RankPvpSystemConfig.BUTTON_BIG_W+" height="+RankPvpSystemConfig.BUTTON_BIG_H+" back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\"></td></tr></table>";
		}
		
		if(RankPvpSystemConfig.DEATH_MANAGER_DETAILS_ENABLED && player.getRankPvpSystemPc().isDeathStatusActive() && player.isDead() && playerTarget.getObjectId() == player.getRankPvpSystemPc().getDeathStatus().getKillerObjectId())
		{ //playerTarget is not real target its handler to current killer. //getTarget() store last killer.
			// button Death Status:
			tb += "<table border=0 cellspacing=0 cellpadding=0><tr><td width=270 HEIGHT=1><img src=\"L2UI.Squaregray\" width=\"270\" height=\"1\"></img></td></tr><tr><td width=270 height=12></td></tr><tr><td width=270 align=center><button value=\"Death Status\" action=\"bypass RPS.DS\" width="+RankPvpSystemConfig.BUTTON_BIG_W+" height="+RankPvpSystemConfig.BUTTON_BIG_H+" back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\"></td></tr></table>";
		}
		
		tb += "</center></body></html>";

		return tb;
	}

	/**
	 * Generate HTML table for images.
	 * @param player 
	 * @param targetPvpStats 
	 * @return
	 */
	private static String rankImgTableHtml(L2PcInstance player, KillerPvpStats targetPvpStats)
	{
		String tb = "";

		//rank image
		tb += "<table cellpadding=0 cellspacing=0 border=0 width=292 height=60 width=292><tr><td width=60 height=60>";
			tb += ServerSideImage.getInstance().getRankIconImageHtmlTag(player, targetPvpStats.getRankId(), 60, 60);
		//rank label
		tb += "</td><td width=232 height=60 align=left>";
			tb += ServerSideImage.getInstance().getRankNameImageHtmlTag(player, targetPvpStats.getRankId(), 232, 60);
		tb += "</td></tr></table>";

		return tb;
	}
	
	private static String expBelt(L2PcInstance player, KillerPvpStats targetPvpStats)
	{
		int percent = calculatePercent(targetPvpStats);

		String tb = "";
		
		// percent belt
		tb += "<table border=0 cellspacing=0 cellpadding=0><tr><td width=292 height=20 align=left>";

		tb += ServerSideImage.getInstance().getExpImageHtmlTag(player, percent, 292, 20);

		tb += "</td></tr><tr><td width=292 height=18></td></tr></table>";

		return tb;
	}
	
	private static int calculatePercent(KillerPvpStats targetPvpStats)
	{
		Rank rank = targetPvpStats.getRank();
		
		if(rank == null)
		{
			log.info("[ERROR] calculatePercent(): no rank found!");
			return 0;
		}
		
		int rankId = rank.getId();
		long minRP = rank.getMinPoints();
		long nextRP = 0;
		long currentRP = targetPvpStats.getTotalRankPoints();
		int percent = 0;

		// check if next rank exists
		if(RankTable.getInstance().getRankList().containsKey(rankId + 1))
		{ 
			// get minRP from next rank:
			Rank nextRank = RankTable.getInstance().getRankList().get(rankId + 1);
			
			if(nextRank != null)
				nextRP = nextRank.getMinPoints();
		}

		if(nextRP > minRP)
		{
			double a = currentRP - minRP;
			double b = nextRP - minRP;
			double calc = (a / b) * 100;
			percent = (int) Math.floor(calc); 
		}
		else
		{
			percent = 100;
		}
		
		if(percent > 100 || percent < 0)
		{
			log.info("[ERROR] calculatePercent(): percent == "+percent+", no exp image can be fit! ");
			log.info("[DEBUG] rankId: "+rankId+", nextRP: "+nextRP+", minRP: "+minRP+", currentRP: "+currentRP);
			return 0;
		}

		return percent;
	}
	
}
