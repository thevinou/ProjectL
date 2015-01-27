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
package net.sf.l2j.gameserver.model.actor.knownlist;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class GuardKnownList extends AttackableKnownList
{
	private static final Logger _log = Logger.getLogger(GuardKnownList.class.getName());
	
	public GuardKnownList(L2GuardInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
			return false;
		
		if (object instanceof L2PcInstance)
		{
			// Check if the object added is a L2PcInstance that owns Karma
			if (((L2PcInstance) object).getKarma() > 0)
			{
				if (Config.DEBUG)
					_log.fine(getActiveChar().getObjectId() + ": PK " + object.getObjectId() + " entered on guard range.");
				
				// Set the L2GuardInstance Intention to ACTIVE
				if (getActiveChar().getAI().getIntention() == CtrlIntention.IDLE)
					getActiveChar().getAI().setIntention(CtrlIntention.ACTIVE, null);
			}
		}
		else if ((Config.GUARD_ATTACK_AGGRO_MOB && getActiveChar().isInActiveRegion()) && object instanceof L2MonsterInstance)
		{
			// Check if the object added is an aggressive L2MonsterInstance
			if (((L2MonsterInstance) object).isAggressive())
			{
				if (Config.DEBUG)
					_log.fine(getActiveChar().getObjectId() + ": Aggressive mob " + object.getObjectId() + " entered on guard range.");
				
				// Set the L2GuardInstance Intention to ACTIVE
				if (getActiveChar().getAI().getIntention() == CtrlIntention.IDLE)
					getActiveChar().getAI().setIntention(CtrlIntention.ACTIVE, null);
			}
		}
		return true;
	}
	
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
			return false;
		
		// If the _aggroList of the L2GuardInstance is empty, set to IDLE
		if (getActiveChar().gotNoTarget())
		{
			if (getActiveChar().hasAI())
				getActiveChar().getAI().setIntention(CtrlIntention.IDLE, null);
		}
		return true;
	}
	
	@Override
	public final L2GuardInstance getActiveChar()
	{
		return (L2GuardInstance) super.getActiveChar();
	}
}