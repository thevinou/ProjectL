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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.datatables.BuyListTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.buylist.NpcBuyList;
import net.sf.l2j.gameserver.model.buylist.Product;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestEventType;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.util.StringUtil;

/**
 * @author terry
 */
public class AdminEditNpc implements IAdminCommandHandler
{
	private final static int PAGE_LIMIT = 20;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_droplist",
		"admin_show_scripts",
		"admin_show_shop",
		"admin_show_shoplist",
		"admin_show_skilllist"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		if (command.startsWith("admin_show_shoplist"))
		{
			try
			{
				showShopList(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_shoplist <list_id>");
			}
		}
		else if (command.startsWith("admin_show_shop"))
		{
			try
			{
				showShop(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_shop <npc_id>");
			}
		}
		else if (command.startsWith("admin_show_droplist"))
		{
			try
			{
				int npcId = Integer.parseInt(st.nextToken());
				int page = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 1;
				
				showNpcDropList(activeChar, npcId, page);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_droplist <npc_id> [<page>]");
			}
		}
		else if (command.startsWith("admin_show_skilllist"))
		{
			try
			{
				showNpcSkillList(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_skilllist <npc_id>");
			}
		}
		else if (command.startsWith("admin_show_scripts"))
		{
			try
			{
				showScriptsList(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_scripts <npc_id>");
			}
		}
		
		return true;
	}
	
	private static void showShopList(L2PcInstance activeChar, int listId)
	{
		final NpcBuyList buyList = BuyListTable.getInstance().getBuyList(listId);
		if (buyList == null)
		{
			activeChar.sendMessage("BuyList template is unknown for id: " + listId + ".");
			return;
		}
		
		final StringBuilder replyMSG = new StringBuilder();
		replyMSG.append("<html><body><center><font color=\"LEVEL\">");
		replyMSG.append(NpcTable.getInstance().getTemplate(buyList.getNpcId()).getName());
		replyMSG.append(" (");
		replyMSG.append(buyList.getNpcId());
		replyMSG.append(") buylist id: ");
		replyMSG.append(buyList.getListId());
		replyMSG.append("</font></center><br><table width=\"100%\"><tr><td width=200>Item</td><td width=80>Price</td></tr>");
		
		for (Product product : buyList.getProducts())
		{
			replyMSG.append("<tr><td>");
			replyMSG.append(product.getItem().getName());
			replyMSG.append("</td><td>");
			replyMSG.append(product.getPrice());
			replyMSG.append("</td></tr>");
		}
		replyMSG.append("</table></body></html>");
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private static void showShop(L2PcInstance activeChar, int npcId)
	{
		final List<NpcBuyList> buyLists = BuyListTable.getInstance().getBuyListsByNpcId(npcId);
		if (buyLists.isEmpty())
		{
			activeChar.sendMessage("No buyLists found for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder replyMSG = new StringBuilder();
		StringUtil.append(replyMSG, "<html><title>Merchant Shop Lists</title><body>");
		
		if (activeChar.getTarget() instanceof L2MerchantInstance)
		{
			L2Npc merchant = (L2Npc) activeChar.getTarget();
			int taxRate = merchant.getCastle().getTaxPercent();
			
			StringUtil.append(replyMSG, "<center><font color=\"LEVEL\">", merchant.getName(), " (", Integer.toString(npcId), ")</font></center><br>Tax rate: ", Integer.toString(taxRate), "%");
		}
		
		StringUtil.append(replyMSG, "<table width=\"100%\">");
		
		for (NpcBuyList buyList : buyLists)
			StringUtil.append(replyMSG, "<tr><td><a action=\"bypass -h admin_show_shoplist ", String.valueOf(buyList.getListId()), " 1\">Buylist id: ", String.valueOf(buyList.getListId()), "</a></td></tr>");
		
		StringUtil.append(replyMSG, "</table></body></html>");
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private static void showNpcDropList(L2PcInstance activeChar, int npcId, int page)
	{
		final NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Npc template is unknown for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder replyMSG = new StringBuilder(2000);
		replyMSG.append("<html><title>Show droplist page ");
		replyMSG.append(page);
		replyMSG.append("</title><body><center><font color=\"LEVEL\">");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcId);
		replyMSG.append(")</font></center><br>");
		
		if (!npcData.getDropData().isEmpty())
		{
			replyMSG.append("Drop type legend: <font color=\"3BB9FF\">Drop</font> | <font color=\"00ff00\">Sweep</font><br><table><tr><td width=25>cat.</td><td width=255>item</td></tr>");
			
			int myPage = 1;
			int i = 0;
			int shown = 0;
			boolean hasMore = false;
			
			for (DropCategory cat : npcData.getDropData())
			{
				if (shown == PAGE_LIMIT)
				{
					hasMore = true;
					break;
				}
				
				for (DropData drop : cat.getAllDrops())
				{
					final String color = ((cat.isSweep()) ? "00FF00" : "3BB9FF");
					
					if (myPage != page)
					{
						i++;
						if (i == PAGE_LIMIT)
						{
							myPage++;
							i = 0;
						}
						continue;
					}
					
					if (shown == PAGE_LIMIT)
					{
						hasMore = true;
						break;
					}
					
					replyMSG.append("<tr><td><font color=\"");
					replyMSG.append(color);
					replyMSG.append("\">");
					replyMSG.append(cat.getCategoryType());
					replyMSG.append("</td><td>");
					replyMSG.append(ItemTable.getInstance().getTemplate(drop.getItemId()).getName());
					replyMSG.append(" (");
					replyMSG.append(drop.getItemId());
					replyMSG.append(")</td></tr>");
					shown++;
				}
			}
			
			replyMSG.append("</table><table width=\"100%\" bgcolor=666666><tr>");
			
			if (page > 1)
			{
				replyMSG.append("<td width=120><a action=\"bypass -h admin_show_droplist ");
				replyMSG.append(npcId);
				replyMSG.append(" ");
				replyMSG.append(page - 1);
				replyMSG.append("\">Prev Page</a></td>");
				if (!hasMore)
				{
					replyMSG.append("<td width=100>Page ");
					replyMSG.append(page);
					replyMSG.append("</td><td width=70></td></tr>");
				}
			}
			
			if (hasMore)
			{
				if (page <= 1)
					replyMSG.append("<td width=120></td>");
				replyMSG.append("<td width=100>Page ");
				replyMSG.append(page);
				replyMSG.append("</td><td width=70><a action=\"bypass -h admin_show_droplist ");
				replyMSG.append(npcId);
				replyMSG.append(" ");
				replyMSG.append(page + 1);
				replyMSG.append("\">Next Page</a></td></tr>");
			}
			replyMSG.append("</table>");
		}
		else
			replyMSG.append("This NPC has no drops.");
		
		replyMSG.append("</body></html>");
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private static void showNpcSkillList(L2PcInstance activeChar, int npcId)
	{
		final NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Npc template is unknown for id: " + npcId + ".");
			return;
		}
		
		final L2Skill[] skills = npcData.getSkillsArray();
		
		final StringBuilder replyMSG = new StringBuilder();
		replyMSG.append("<html><body><center><font color=\"LEVEL\">");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcId);
		replyMSG.append("): ");
		replyMSG.append(skills.length);
		replyMSG.append(" skills</font></center><table width=\"100%\">");
		
		for (L2Skill skill : skills)
		{
			replyMSG.append("<tr><td>");
			replyMSG.append((skill.getSkillType() == L2SkillType.NOTDONE) ? ("<font color=\"777777\">" + skill.getName() + "</font>") : skill.getName());
			replyMSG.append(" [");
			replyMSG.append(skill.getId());
			replyMSG.append("-");
			replyMSG.append(skill.getLevel());
			replyMSG.append("]</td></tr>");
		}
		replyMSG.append("</table></body></html>");
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private static void showScriptsList(L2PcInstance activeChar, int npcId)
	{
		final NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Npc template is unknown for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder replyMSG = new StringBuilder(2000);
		replyMSG.append("<html><body><center><font color=\"LEVEL\">");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcId);
		replyMSG.append(")</font></center><br>");
		
		if (!npcData.getEventQuests().isEmpty())
		{
			QuestEventType type = null; // Used to see if we moved of type.
			
			// For any type of QuestEventType
			for (Map.Entry<QuestEventType, List<Quest>> entry : npcData.getEventQuests().entrySet())
			{
				if (type != entry.getKey())
				{
					type = entry.getKey();
					replyMSG.append("<br><font color=\"LEVEL\">" + type.name() + "</font><br1>");
				}
				
				for (Quest quest : entry.getValue())
					replyMSG.append(quest.getName() + "<br1>");
			}
		}
		else
			replyMSG.append("This NPC isn't affected by scripts.");
		
		replyMSG.append("</body></html>");
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}