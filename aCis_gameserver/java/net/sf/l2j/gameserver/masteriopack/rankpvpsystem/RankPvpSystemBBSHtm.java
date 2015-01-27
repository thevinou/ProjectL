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

import java.util.Map;
import java.util.Map.Entry;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Masterio
 */
public final class RankPvpSystemBBSHtm 
{

	public static final String getPage(L2PcInstance activeChar, int page)
	{
		String file = null; 
		
		file = getBody();
		
		if (file == null)
		{
			file = "<html><body><br><br><center>404 :File Not found!<br> (check file: data/html/CommunityBoard/rankpvpsystem/body.htm) </center></body></html>";
		}
		else
		{
			file = prepareHeaderName(page, file);
			
			if(RankPvpSystemConfig.RANKS_ENABLED)
			{
				file = file.replace("%button_1%", getNextButton(page));
				file = file.replace("%button_2%", getPreviousButton(page));
			}
			else
			{
				file = file.replace("%button_1%", "&nbsp;");
				file = file.replace("%button_2%", "&nbsp;");
			}
			
			file = prepareTopList(activeChar, page, file);
			
			file = file.replace("%refresh_time%", RankPvpSystemUtil.timeToString(TopTable.getNextUpdateTime()));
		}
		
		file = file.replace("%version%", RankPvpSystemConfig.RANK_PVP_SYSTEM_VERSION);
			
		return file;
	}
	
	private static final String prepareHeaderName(int page, String file)
	{
		
		if(!TopTable.getInstance().isUpdating())
		{
			if(page == 1)
			{
				return file.replace("%header%", "TOP 10 Rank Point Gatherers");
			}
				
			return file.replace("%header%", "TOP 10 Killers");
		}
		
		return file.replace("%header%", "TOP 10");
	}
	
	private static final String prepareTopList(L2PcInstance activeChar, int page, String file)
	{
		
		String list = "";
		
		if(!TopTable.getInstance().isUpdating())
		{

			boolean playerInfo = false;

			int pos = 0;
		
			Map<Integer, TopField> topTable = null;
			
			if(page == 1)
			{
				topTable = TopTable.getInstance().getTopGatherersTable();
			}
			else
			{
				topTable = TopTable.getInstance().getTopKillsTable();
			}
			
			if(topTable == null)
			{
				return file;
			}

			for (Entry<Integer, TopField> e : topTable.entrySet()) 
			{
				pos++;
				
				TopField tf = e.getValue();

				if(activeChar.getObjectId() == tf.getCharacterId())
				{
					if(pos <= 10)
					{
						// add row to the top 10 list for current player who is watching the list:
						list += prepareListItem(pos, tf.getCharacterName(), tf.getCharacterLevel(), RankPvpSystemUtil.getClassName(tf.getCharacterBaseClassId()), tf.getCharacterPoints(), "2080D0");
					}
					else
					{
						// add row under the top 10 list for current player who is watching the list:
						list += "<br>"+prepareListItem(pos, tf.getCharacterName(), tf.getCharacterLevel(), RankPvpSystemUtil.getClassName(tf.getCharacterBaseClassId()), tf.getCharacterPoints(), "2080D0");
					}
					
					playerInfo = true;

				}
				else if(pos <= 10)
				{
					// add row to list with player data:
					list += prepareListItem(pos, tf.getCharacterName(), tf.getCharacterLevel(), RankPvpSystemUtil.getClassName(tf.getCharacterBaseClassId()), tf.getCharacterPoints(), null);
				}
					
				
				
				if(pos > 10 && playerInfo)
				{
					// if list complete:
					break;
				}
			}
			

			if(!playerInfo)
			{
		     	if(RankPvpSystemConfig.TOP_LIST_IGNORE_TIME_LIMIT > 0)
		     	{
		     		file = file.replace("%message%", "You're out of "+TopTable.TOP_LIMIT+", or you did not kill anyone or even killed more than "+Math.round((double)RankPvpSystemConfig.TOP_LIST_IGNORE_TIME_LIMIT / (double)86400000)+" days ago.");
		     	}
		     	else
		     	{
		     		file = file.replace("%message%", "You're out of "+TopTable.TOP_LIMIT+", or you did not kill anyone.");
		     	}

			}
			else
			{
				file = file.replace("%message%", "&nbsp;");
			}
	
			if(list.equals(""))
			{
		     	list += "No one on TOP 10 list yet";
		    }
			
			// add list header before item list:
			list = prepareListHead(page) + list;
			
		}
		else
		{ // if is updating:
	
	     	list = "<font color=FF8000>Updating... try again for few seconds</font><br><br>";
	     	
	     	if(page == 1)
	     	{
	     		list += "<button value=\"Refresh\" action=\"bypass _bbsrps:1\" width="+RankPvpSystemConfig.BUTTON_W+" height="+RankPvpSystemConfig.BUTTON_H+" back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\">";	
	     	}
	     	else
	     	{
	     		list += "<button value=\"Refresh\" action=\"bypass _bbsrps:0\" width="+RankPvpSystemConfig.BUTTON_W+" height="+RankPvpSystemConfig.BUTTON_H+" back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\">";
	     	}

		}

			
		return file.replace("%list%", list);


	}
	
	private static final String prepareListHead(int page)
	{
		String item = getListHead();

		if(page == 1)
		{
			item = item.replace("%col5_name%", "Rank Point's");
		}
		else
		{
			item = item.replace("%col5_name%", "PvP Kill's");
		}

		return item;
	}
	
	/**
	 * If fontColor == null then function will not use any colors.
	 * @param position
	 * @param playerName
	 * @param playerLevel
	 * @param playerClass
	 * @param col5Value
	 * @param fontColor - Example: "FF0000", "red", "FFFFFF", ...
	 * @return
	 */
	private static final String prepareListItem(int position, String playerName, int playerLevel, String playerClass, long col5Value, String fontColor)
	{

		String item = "";
		
		
		if(fontColor != null) item += "<font color="+fontColor+">";
		
		item += getListItem();
		
		item = item.replace("%position%", position+"");
		item = item.replace("%player_name%", playerName);
		item = item.replace("%player_level%", playerLevel+"");
		item = item.replace("%player_class%", playerClass);
		item = item.replace("%col5_value%", RankPvpSystemUtil.preparePrice(col5Value)+"");
		
		if(fontColor != null) item += "</font>";
		
		return item;
		
	}
	
	private static final String getNextButton(int page)
	{
		if(!TopTable.getInstance().isUpdating())
		{
			if(page == 0)
			{
				return "<button value=\">>\" action=\"bypass _bbsrps:1\" width="+RankPvpSystemConfig.BUTTON_W+" height="+RankPvpSystemConfig.BUTTON_H+" back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\">";
			}
			
			return "&nbsp;";
		}
		
		return "&nbsp;";
	}
	
	private static final String getPreviousButton(int page)
	{
		if(!TopTable.getInstance().isUpdating())
		{
			if(page == 1)
			{
				return "<button value=\"<<\" action=\"bypass _bbsrps:0\" width="+RankPvpSystemConfig.BUTTON_W+" height="+RankPvpSystemConfig.BUTTON_H+" back=\""+RankPvpSystemConfig.BUTTON_DOWN+"\" fore=\""+RankPvpSystemConfig.BUTTON_UP+"\">";
			}
			
			return "&nbsp;";
		}
		
		return "&nbsp;";
	}

	private static final String getBody()
	{
		return HtmCache.getInstance().getHtm("data/html/CommunityBoard/rankpvpsystem/body.htm");
	}
	
	private static final String getListHead()
	{
		return HtmCache.getInstance().getHtm("data/html/CommunityBoard/rankpvpsystem/list_head.htm");
	}
	
	private static final String getListItem()
	{
		return HtmCache.getInstance().getHtm("data/html/CommunityBoard/rankpvpsystem/list_item.htm");
	}
	
}
