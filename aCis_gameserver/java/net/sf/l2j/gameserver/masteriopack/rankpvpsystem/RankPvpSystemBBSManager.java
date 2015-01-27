/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

/**
 * @author Masterio
 */
public class RankPvpSystemBBSManager extends BaseBBSManager
{	
	public static final Logger log = Logger.getLogger(RankPvpSystemBBSManager.class.getName());
	
	@Override
	public void parseCmd(String command, L2PcInstance activeChar)
	{
		if(RankPvpSystemConfig.RANK_PVP_SYSTEM_ENABLED && RankPvpSystemConfig.TOP_LIST_ENABLED)
		{
			if(command.startsWith("_bbsrps:"))
			{
				int page = 0;
				try
				{
					page = Integer.parseInt(command.split(":", 2)[1].trim());
				}
				catch(Exception e)
				{
					log.info(e.getMessage());
					page = 0;
				}
				
				separateAndSend(RankPvpSystemBBSHtm.getPage(activeChar, page), activeChar);
			}
		}
		else if(!RankPvpSystemConfig.TOP_LIST_ENABLED)
		{
			ShowBoard sb = null;

			sb = new ShowBoard("<html><body><br><br><center>Community Board Top List is disabled in config file</center><br><br></body></html>", "101");

			activeChar.sendPacket(sb);
			activeChar.sendPacket(new ShowBoard(null, "102"));
			activeChar.sendPacket(new ShowBoard(null, "103"));
			
			sb = null;
		}
	}

	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		// 
	}

	private static RankPvpSystemBBSManager _instance = new RankPvpSystemBBSManager();

	public static RankPvpSystemBBSManager getInstance()
	{
		return _instance;
	}
}
