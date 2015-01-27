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
package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Tryskell
 */
public class PvpFlagTaskManager
{
	protected Map<L2PcInstance, Long> _pvpFlagTask = new ConcurrentHashMap<>();
	
	public PvpFlagTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PvpFlagScheduler(), 0, 1000);
	}
	
	public static PvpFlagTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void add(L2PcInstance actor, long time)
	{
		_pvpFlagTask.put(actor, time);
	}
	
	public void remove(L2PcInstance actor)
	{
		_pvpFlagTask.remove(actor);
	}
	
	private class PvpFlagScheduler implements Runnable
	{
		protected PvpFlagScheduler()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			if (!_pvpFlagTask.isEmpty())
			{
				Long current = System.currentTimeMillis();
				synchronized (this)
				{
					for (L2PcInstance actor : _pvpFlagTask.keySet())
					{
						if (current > _pvpFlagTask.get(actor))
						{
							actor.updatePvPFlag(0);
							_pvpFlagTask.remove(actor);
						}
						else if (current > (_pvpFlagTask.get(actor) - 5000))
							actor.updatePvPFlag(2);
						else
							actor.updatePvPFlag(1);
					}
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final PvpFlagTaskManager _instance = new PvpFlagTaskManager();
	}
}