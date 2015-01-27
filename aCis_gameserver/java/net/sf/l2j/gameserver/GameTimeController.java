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
package net.sf.l2j.gameserver;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles :
 * <ul>
 * <li>ingame time (10 real seconds equal 1 ingame minute) ;</li>
 * <li>character movement ;</li>
 * <li>ShadowSense messages.</li>
 * </ul>
 */
public class GameTimeController
{
	protected static final Logger _log = Logger.getLogger(GameTimeController.class.getName());
	
	public static final int TICKS_PER_SECOND = 10;
	public static final int MILLIS_IN_TICK = 100;
	
	protected static int _gameTicks;
	protected static long _gameStartTime;
	protected static boolean _isNight = false;
	protected static boolean _interruptRequest = false;
	
	private static final Map<Integer, L2Character> _movingObjects = new ConcurrentHashMap<>();
	protected static TimerThread _timer;
	
	public static GameTimeController getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected GameTimeController()
	{
		_gameStartTime = System.currentTimeMillis() - 3600000;
		_gameTicks = 36000;
		
		_timer = new TimerThread();
		_timer.start();
		
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new BroadcastSunState(), 0, 600000);
	}
	
	public boolean isNowNight()
	{
		return _isNight;
	}
	
	public int getGameTime()
	{
		return _gameTicks / MILLIS_IN_TICK;
	}
	
	public static int getGameTicks()
	{
		return _gameTicks;
	}
	
	/**
	 * Add a L2Character to movingObjects of GameTimeController.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController.<BR>
	 * <BR>
	 * @param cha The L2Character to add to movingObjects of GameTimeController
	 */
	public void registerMovingObject(L2Character cha)
	{
		if (cha == null)
			return;
		
		_movingObjects.put(cha.getObjectId(), cha);
	}
	
	/**
	 * Move all L2Characters contained in movingObjects of GameTimeController.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Update the position of each L2Character</li> <li>If movement is finished, the L2Character is removed from movingObjects</li> <li>Create a task to update the _knownObject and _knowPlayers of each L2Character that finished its movement and of their already known L2Object then notify AI with
	 * EVT_ARRIVED</li>
	 */
	protected void moveObjects()
	{
		// Go throw the table containing L2Character in movement
		Iterator<Map.Entry<Integer, L2Character>> it = _movingObjects.entrySet().iterator();
		while (it.hasNext())
		{
			// If movement is finished, the L2Character is removed from
			// movingObjects and added to the ArrayList ended
			L2Character ch = it.next().getValue();
			if (ch.updatePosition(_gameTicks))
			{
				it.remove();
				ThreadPoolManager.getInstance().executeTask(new MovingObjectArrived(ch));
			}
		}
	}
	
	public void stopTimer()
	{
		_interruptRequest = true;
		_timer.interrupt();
	}
	
	class TimerThread extends Thread
	{
		public TimerThread()
		{
			super("GameTimeController");
			setDaemon(true);
			setPriority(MAX_PRIORITY);
		}
		
		@Override
		public void run()
		{
			int oldTicks;
			long runtime;
			int sleepTime;
			
			for (;;)
			{
				try
				{
					oldTicks = _gameTicks; // save old ticks value to avoid moving objects 2x in same tick
					runtime = System.currentTimeMillis() - _gameStartTime; // from server boot to now
					
					_gameTicks = (int) (runtime / MILLIS_IN_TICK); // new ticks value (ticks now)
					
					if (oldTicks != _gameTicks)
						moveObjects();
					
					runtime = (System.currentTimeMillis() - _gameStartTime) - runtime;
					
					// calculate sleep time... time needed to next tick minus time it takes to call moveObjects()
					sleepTime = 1 + MILLIS_IN_TICK - ((int) runtime) % MILLIS_IN_TICK;
					
					if (sleepTime > 0)
						Thread.sleep(sleepTime);
				}
				catch (InterruptedException ie)
				{
					if (_interruptRequest)
						return;
				}
			}
		}
	}
	
	/**
	 * Update the _knownObject and _knowPlayers of each L2Character that finished its movement and of their already known L2Object then notify AI with EVT_ARRIVED.
	 */
	private static class MovingObjectArrived implements Runnable
	{
		private final L2Character _ended;
		
		MovingObjectArrived(L2Character ended)
		{
			_ended = ended;
		}
		
		@Override
		public void run()
		{
			if (_ended.hasAI()) // AI could be just disabled due to region turn off
				_ended.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
		}
	}
	
	class BroadcastSunState implements Runnable
	{
		int h;
		boolean tempIsNight;
		
		@Override
		public void run()
		{
			h = ((getGameTime() + 29) / 60) % 24; // Time in hour
			tempIsNight = (h < 6);
			
			if (tempIsNight != _isNight)
			{
				_isNight = tempIsNight; // Set current day/night variable to value of temp variable
				DayNightSpawnManager.getInstance().notifyChangeMode();
			}
			
			// "Activate" shadow sense at 00h00 (night) and 06h00 (sunrise)
			if (h == 0 || h == 6)
				activateShadowSense();
		}
	}
	
	protected void activateShadowSense()
	{
		for (L2PcInstance onlinePlayer : L2World.getInstance().getAllPlayers().values())
		{
			// if a player is a DE, verify if he got the skill
			if (onlinePlayer != null && onlinePlayer.getRace() == Race.DarkElf)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
				
				// If player got the skill (exemple : low level DEs haven't it)
				if (skill != null && onlinePlayer.getSkillLevel(294) == 1)
				{
					if (isNowNight())
						onlinePlayer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NIGHT_S1_EFFECT_APPLIES).addSkillName(294));
					else
						onlinePlayer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DAY_S1_EFFECT_DISAPPEARS).addSkillName(294));
					
					// You saw nothing and that pack doesn't even exist w_w.
					onlinePlayer.removeSkill(skill, false);
					onlinePlayer.addSkill(skill, false);
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final GameTimeController _instance = new GameTimeController();
	}
}