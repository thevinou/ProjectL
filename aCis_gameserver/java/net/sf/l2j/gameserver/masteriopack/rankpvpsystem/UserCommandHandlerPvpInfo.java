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

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


/**
 * @author Masterio
 */
public class UserCommandHandlerPvpInfo implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS = { RankPvpSystemConfig.PVP_INFO_USER_COMMAND_ID };

	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if(id != COMMAND_IDS[0])
		{
			return false;
		}
		
		if(activeChar == null)
			return false;
		
		if(activeChar.getRankPvpSystemPc() == null)
			return false;
		
		RankPvpSystemPc pc = activeChar.getRankPvpSystemPc();
		
		// reset death status:
		if(!activeChar.isDead())
		{
			pc.setDeathStatus(null);
		}
		
		// save target of active player when command executed:
		if(activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
		{
			pc.setTarget((L2PcInstance) activeChar.getTarget());
		}
		else
		{
			pc.setTarget(activeChar);
			activeChar.sendMessage("PvP Status executed on self!");
		}

		RankPvpSystemPvpStatus.sendPage(activeChar, pc.getTarget());
		
		return true;
	}

	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}

}