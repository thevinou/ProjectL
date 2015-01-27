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
package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.util.Util;

public class RegionBBSManager extends BaseBBSManager
{
	private static final String REGION_LIST = "<table><tr><td width=5></td><td width=160><a action=\"bypass _bbsloc;%castleId%\">%castleName%</a></td><td width=160>%ownerName%</td><td width=160>%allyName%</td><td width=120>%actualTax%</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=605 height=1><br1>";
	private static final String CLAN_HALL_BAR = "<br><br><table width=610 bgcolor=A7A19A><tr><td width=5></td><td width=200>Clan Hall Name</td><td width=200>Owning Clan</td><td width=200>Clan Leader Name</td><td width=5></td></tr></table><br1>";
	private static final String CLAN_HALL_LIST = "<table><tr><td width=5></td><td width=200>%chName%</td><td width=200>%clanName%</td><td width=200>%leaderName%</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=605 height=1><br1>";
	
	protected RegionBBSManager()
	{
	}
	
	public static RegionBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@Override
	public void parseCmd(String command, L2PcInstance activeChar)
	{
		if (command.equals("_bbsloc"))
			showRegionsList(activeChar);
		else if (command.startsWith("_bbsloc"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			
			showRegion(activeChar, Integer.parseInt(st.nextToken()));
		}
		else
			super.parseCmd(command, activeChar);
	}
	
	@Override
	protected String getFolder()
	{
		return "region/";
	}
	
	private static void showRegionsList(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtm(CB_PATH + "region/castlelist.htm");
		String list = "";
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			final L2Clan owner = ClanTable.getInstance().getClan(castle.getOwnerId());
			
			list += REGION_LIST;
			list = list.replace("%castleId%", Integer.toString(castle.getCastleId()));
			list = list.replace("%castleName%", castle.getName());
			list = list.replace("%ownerName%", ((owner != null) ? "<a action=\"bypass _bbsclan;home;" + owner.getClanId() + "\">" + owner.getName() + "</a>" : "None"));
			list = list.replace("%allyName%", ((owner != null && owner.getAllyId() > 0) ? owner.getAllyName() : "None"));
			list = list.replace("%actualTax%", ((owner != null) ? Integer.toString(castle.getTaxPercent()) : "0"));
		}
		content = content.replace("%castleList%", list);
		separateAndSend(content, activeChar);
	}
	
	private static void showRegion(L2PcInstance activeChar, int castleId)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		final L2Clan owner = ClanTable.getInstance().getClan(castle.getOwnerId());
		
		String content = HtmCache.getInstance().getHtm(CB_PATH + "region/castle.htm");
		
		content = content.replace("%castleName%", castle.getName());
		content = content.replace("%tax%", Integer.toString(castle.getTaxPercent()));
		content = content.replace("%lord%", ((owner != null) ? owner.getLeaderName() : "None"));
		content = content.replace("%clanName%", ((owner != null) ? "<a action=\"bypass _bbsclan;home;" + owner.getClanId() + "\">" + owner.getName() + "</a>" : "None"));
		content = content.replace("%allyName%", ((owner != null && owner.getAllyId() > 0) ? owner.getAllyName() : "None"));
		content = content.replace("%siegeDate%", Util.formatDate(castle.getSiegeDate().getTimeInMillis(), "yyyy/MM/dd HH:mm"));
		
		String list = "";
		
		final List<ClanHall> clanHalls = ClanHallManager.getInstance().getClanHallsByLocation(castle.getName());
		if (clanHalls != null && !clanHalls.isEmpty())
		{
			list = CLAN_HALL_BAR;
			
			for (ClanHall ch : clanHalls)
			{
				final L2Clan chOwner = ClanTable.getInstance().getClan(ch.getOwnerId());
				
				list += CLAN_HALL_LIST;
				list = list.replace("%chName%", ch.getName());
				list = list.replace("%clanName%", ((chOwner != null) ? "<a action=\"bypass _bbsclan;home;" + chOwner.getClanId() + "\">" + chOwner.getName() + "</a>" : "None"));
				list = list.replace("%leaderName%", ((chOwner != null) ? chOwner.getLeaderName() : "None"));
			}
		}
		content = content.replaceAll("%hallsList%", list);
		separateAndSend(content, activeChar);
	}
	
	private static class SingletonHolder
	{
		protected static final RegionBBSManager _instance = new RegionBBSManager();
	}
}