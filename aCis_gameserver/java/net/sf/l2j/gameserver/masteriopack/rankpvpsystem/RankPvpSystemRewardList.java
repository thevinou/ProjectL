package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

import java.util.Map;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Masterio
 */
public class RankPvpSystemRewardList
{
	public static final int LIST_LENGTH = 8;
	
	/**
	 * Show HTM page for player.
	 * @param player
	 * @param pageNo
	 * @param rankId
	 */
	public static void sendPage(L2PcInstance player, int pageNo, int rankId)
	{
		NpcHtmlMessage n = new NpcHtmlMessage(0);
		
		n.setHtml(preparePage(player, pageNo, rankId));

		player.sendPacket(n);
	}
	
	private static String preparePage(L2PcInstance player, int pageNo, int rankId)
	{
		// define container table:
		String[] text = getPvpRewardListAndPageChanger(pageNo, rankId);
		return "<html><title>Rank Reward List</title><body><center><table border=0 cellspacing=0 cellpadding=0><tr><td><table><tr><td width=100 align=left> <font color=ae9977></font> </td><td align=center><font color=2080D0></font></td></tr><tr><td width=100 align=left> <font color=ae9977></font> </td><td width=170 align=center><font color=2080D0></font></td></tr></table></td></tr><tr><td height=8>&nbsp;</td></tr><tr><td FIXWIDTH=270 HEIGHT=4><img src=\"L2UI.Squaregray\" width=\"270\" height=\"1\"></img></td></tr><tr><td height=8></td></tr><tr><td>"+text[0]+"</td></tr><tr><td>&nbsp;</td></tr><tr><td HEIGHT=4></td></tr><tr><td>"+text[1]+"</td></tr><tr><td FIXWIDTH=270 HEIGHT=4><img src=\"L2UI.Squaregray\" width=\"270\" height=\"1\"></img></td></tr><tr><td align=center><button value=\"Back\" action=\"bypass RPS.PS\" width="+RankPvpSystemConfig.BUTTON_W+" height="+RankPvpSystemConfig.BUTTON_H+" back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\"></td></tr></table></center></body></html>";
	}

	/**
	 * List is gathered from RewardTable.pvpRewardList
	 * @param pageNo 
	 * @param rankId 
	 * @return [0] getPvpRewardList, [1] getPageChanger
	 */
	private static String[] getPvpRewardListAndPageChanger(final int pageNo, int rankId)
	{
		String[] result = new String[2];
		
		int i = 0;
		boolean inList = false;
		
		String list = "";
		
		if(RankPvpSystemConfig.PVP_REWARD_ENABLED && pageNo == 1)
		{
			String item_name = ItemTable.getInstance().getTemplate(RankPvpSystemConfig.PVP_REWARD_ID).getName();
			list += getPvpRewardListItem(RankPvpSystemConfig.PVP_REWARD_ID, item_name, RankPvpSystemConfig.PVP_REWARD_AMOUNT, "PvP", pageNo);
			i++;
		}

		Map<Integer, Reward> rpcRewardList = RewardTable.getInstance().getRankPvpRewardList(rankId);
		
		for (Map.Entry<Integer, Reward> e : rpcRewardList.entrySet()) 
		{
			Reward r = e.getValue();
			
			if(r == null)
				break;

			i++;
	
			if(i > (LIST_LENGTH * (pageNo - 1)) && i <= (LIST_LENGTH * (pageNo)))
			{
				
				
				String itemName = ItemTable.getInstance().getTemplate(r.getItemId()).getName();

				list += getPvpRewardListItem(r.getId(), itemName, r.getItemAmount(), "Rank", pageNo);

				inList = true;
			}
			else
			{
				if(inList)
					break;
			}
		}

		if(list.equals(""))
		{
			result[0] = "<table cellspacing=0 cellpadding=0><tr><td height=270>No rewards defined yet.</td></tr></table>";
		}
		
		result[0] = "<table cellspacing=0 cellpadding=0><tr><td height=270><table>" + list + "</table></td></tr></table>";
	
		result[1] = getPageChanger(pageNo, rankId, getPageCount(rpcRewardList.size()));
		
		return result;
	}
	
	private static String getPvpRewardListItem(int itemId, String itemName, long itemCount, String type, int pageNo)
	{
		return "<tr><td height=32><table cellspacing=0 cellpadding=0><tr><td width=270 height=16><font color=FF8000>"+itemName+"</font></td></tr><tr><td><table width=270 cellspacing=0 cellpadding=0><tr><td width=150 height=16><font color=ae9977>Count:</font> <font color=808080>"+RankPvpSystemUtil.preparePrice(itemCount)+"</font></td><td align=right><font color=ae9977>Type:</font> <font color=808080>"+type+"</font></td></tr></table></td></tr></table></td></tr><tr><td FIXWIDTH=270 HEIGHT=4><img src=\"L2UI.Squaregray\" width=\"270\" height=\"1\"></img></td></tr>";
	}
	
	private static String getPageChanger(int pageNo, int rankId, int pageCount)
	{
		String backButton = "&nbsp;";
		String nextButton = "&nbsp;";
		
		if(pageNo > 1)
		{
			backButton = "<button value=\"<<\" action=\"bypass RPS.RewardList:"+rankId+","+(pageNo-1)+"\" width=40 height=16 back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\">";
		}
		
		if(pageNo < pageCount)
		{
			nextButton = "<button value=\">>\" action=\"bypass RPS.RewardList:"+rankId+","+(pageNo+1)+"\" width=40 height=16 back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\">";
		}
		
		return "<table><tr><td width=90 align=right>"+backButton+"</td><td width=90 align=center>"+pageNo+" / "+pageCount+"</td><td width=90 align=left>"+nextButton+"</td></tr></table>";
	}

	/**
	 * Used for calculate all pages with reward list in RankPvpSystemRewardList window.<br>
	 * @param rewardListSize 
	 * @return 
	 */
	public static int getPageCount(int rewardListSize)
	{
		if(rewardListSize % RankPvpSystemRewardList.LIST_LENGTH == 0)
		{
			return (int)Math.floor(rewardListSize / RankPvpSystemRewardList.LIST_LENGTH);
		}

		return (int)Math.floor(rewardListSize / RankPvpSystemRewardList.LIST_LENGTH) + 1;
	}
}
















