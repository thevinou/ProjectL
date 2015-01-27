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
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * @author Tryskell
 */
public class TakeBreakTaskManager
{
	protected Map<L2PcInstance, Long> _takeBreakTask = new ConcurrentHashMap<>();
	
	public TakeBreakTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TakeBreakScheduler(), 0, 1000);
	}
	
	public static TakeBreakTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void add(L2PcInstance actor)
	{
		_takeBreakTask.put(actor, System.currentTimeMillis() + 7200000);
	}
	
	public void remove(L2Character actor)
	{
		_takeBreakTask.remove(actor);
	}
	
	private class TakeBreakScheduler implements Runnable
	{
		protected TakeBreakScheduler()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			if (!_takeBreakTask.isEmpty())
			{
				Long current = System.currentTimeMillis();
				synchronized (this)
				{
					for (L2PcInstance actor : _takeBreakTask.keySet())
					{
						if (current > _takeBreakTask.get(actor))
						{
							if (actor.isOnline())
							{
								actor.sendPacket(SystemMessageId.PLAYING_FOR_LONG_TIME);
								_takeBreakTask.put(actor, System.currentTimeMillis() + 7200000);
							}
						}
					}
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final TakeBreakTaskManager _instance = new TakeBreakTaskManager();
	}
}