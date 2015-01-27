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

import java.util.Collection;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.StringUtil;

/**
 * Handles visibility over target's knownlist, offering details about current target's vicinity.
 * @author Tryskell
 */
public class AdminKnownlist implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_knownlist"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_knownlist"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			L2Object target = null;
			
			// Try to parse the parameter as an int, then try to retrieve an objectId ; if it's a string, search for any player name.
			if (st.hasMoreTokens())
			{
				final String parameter = st.nextToken();
				
				try
				{
					final int objectId = Integer.parseInt(parameter);
					target = L2World.getInstance().findObject(objectId);
				}
				catch (NumberFormatException nfe)
				{
					target = L2World.getInstance().getPlayer(parameter);
				}
			}
			
			// If no one is found, pick potential activeChar's target or the activeChar himself.
			if (target == null)
			{
				target = activeChar.getTarget();
				if (target == null)
					target = activeChar;
			}
			
			final Collection<L2Object> knownlist = target.getKnownList().getKnownObjects();
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
			adminReply.setFile("data/html/admin/knownlist.htm");
			
			// Generate data.
			final StringBuilder replyMSG = new StringBuilder(knownlist.size() * 200);
			
			for (L2Object object : knownlist)
			{
				StringUtil.append(replyMSG, "<tr><td>" + object.getName() + " [" + object.getClass().getSimpleName() + "]</td></tr>");
			}
			adminReply.replace("%target%", target.getName());
			adminReply.replace("%size%", knownlist.size());
			adminReply.replace("%knownlist%", replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}