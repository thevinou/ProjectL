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
package net.sf.l2j.gameserver.model.zone.type;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;

/**
 * @author DaRkRaGe
 */
public class L2BossZone extends L2ZoneType
{
	private int _timeInvade;
	private boolean _enabled = true;
	
	// Track the times that players got disconnected. Players are allowed to log back into the zone as long as their log-out was within _timeInvade time...
	private final Map<Integer, Long> _playerAllowedReEntryTimes = new ConcurrentHashMap<>();
	
	// Track players admitted to the zone who should be allowed back in after reboot/server downtime, within 30min of server restart
	private final List<Integer> _playersAllowed = new CopyOnWriteArrayList<>();
	
	private final int[] _oustLoc = new int[3];
	
	public L2BossZone(int id)
	{
		super(id);
		
		GrandBossManager.getInstance().addZone(this);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("InvadeTime"))
			_timeInvade = Integer.parseInt(value);
		else if (name.equals("EnabledByDefault"))
			_enabled = Boolean.parseBoolean(value);
		else if (name.equals("oustX"))
			_oustLoc[0] = Integer.parseInt(value);
		else if (name.equals("oustY"))
			_oustLoc[1] = Integer.parseInt(value);
		else if (name.equals("oustZ"))
			_oustLoc[2] = Integer.parseInt(value);
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (_enabled)
		{
			if (character instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance) character;
				player.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
				if (player.isGM())
					return;
				
				// if player has been (previously) cleared by npc/ai for entry and the zone is
				// set to receive players (aka not waiting for boss to respawn)
				if (_playersAllowed.contains(player.getObjectId()))
				{
					// Get the information about this player's last logout-exit from this zone.
					final Long expirationTime = _playerAllowedReEntryTimes.get(player.getObjectId());
					
					// with legal entries, do nothing.
					if (expirationTime == null) // legal null expirationTime entries
					{
						long serverStartTime = GameServer.dateTimeServerStarted.getTimeInMillis();
						if (serverStartTime > (System.currentTimeMillis() - _timeInvade))
							return;
					}
					else
					{
						// legal non-null logoutTime entries
						_playerAllowedReEntryTimes.remove(player.getObjectId());
						if (expirationTime.longValue() > System.currentTimeMillis())
							return;
					}
					_playersAllowed.remove(_playersAllowed.indexOf(player.getObjectId()));
				}
				
				// teleport out all players who attempt "illegal" (re-)entry
				if (_oustLoc[0] != 0 && _oustLoc[1] != 0 && _oustLoc[2] != 0)
					player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], 0);
				else
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			else if (character instanceof L2Summon)
			{
				final L2PcInstance player = ((L2Summon) character).getOwner();
				if (player != null)
				{
					if (_playersAllowed.contains(player.getObjectId()) || player.isGM())
						return;
					
					// remove summon and teleport out owner who attempt "illegal" (re-)entry
					if (_oustLoc[0] != 0 && _oustLoc[1] != 0 && _oustLoc[2] != 0)
						player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], 0);
					else
						player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				((L2Summon) character).unSummon(player);
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (character instanceof L2Playable && _enabled)
		{
			if (character instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance) character;
				player.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
				if (player.isGM())
					return;
				
				// if the player just got disconnected/logged out, store the dc time so that
				// decisions can be made later about allowing or not the player to log into the zone
				if (!player.isOnline() && _playersAllowed.contains(player.getObjectId()))
				{
					// mark the time that the player left the zone
					_playerAllowedReEntryTimes.put(player.getObjectId(), System.currentTimeMillis() + _timeInvade);
				}
				else
				{
					if (_playersAllowed.contains(player.getObjectId()))
						_playersAllowed.remove(_playersAllowed.indexOf(player.getObjectId()));
					
					_playerAllowedReEntryTimes.remove(player.getObjectId());
				}
			}
			
			// If playables aren't found, force all bosses to return to spawnpoint.
			if (!_characterList.isEmpty())
			{
				if (getKnownTypeInside(L2Playable.class).size() > 0)
					return;
				
				for (L2Attackable raid : getKnownTypeInside(L2Attackable.class))
				{
					if (raid.isRaid())
					{
						if (raid.getSpawn() == null || raid.isDead())
							continue;
						
						if (!raid.isInsideRadius(raid.getSpawn().getLocx(), raid.getSpawn().getLocy(), 150, false))
							raid.returnHome();
					}
				}
			}
		}
		else if (character instanceof L2Attackable && character.isRaid() && !character.isDead())
			((L2Attackable) character).returnHome();
	}
	
	public void addAllowedPlayer(int objectId)
	{
		if (!_playersAllowed.contains(objectId))
			_playersAllowed.add(objectId);
	}
	
	public List<Integer> getAllowedPlayers()
	{
		return _playersAllowed;
	}
	
	public boolean isPlayerAllowed(L2PcInstance player)
	{
		if (player.isGM())
			return true;
		
		if (_playersAllowed.contains(player.getObjectId()))
			return true;
		
		if (_oustLoc[0] != 0 && _oustLoc[1] != 0 && _oustLoc[2] != 0)
			player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], 0);
		else
			player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
		
		return false;
	}
	
	/**
	 * Some GrandBosses send all players in zone to a specific part of the zone, rather than just removing them all. If this is the case, this command should be used. If this is no the case, then use oustAllPlayers().
	 * @param x
	 * @param y
	 * @param z
	 */
	public void movePlayersTo(int x, int y, int z)
	{
		if (_characterList.isEmpty())
			return;
		
		for (L2PcInstance player : getKnownTypeInside(L2PcInstance.class))
		{
			if (player.isOnline())
				player.teleToLocation(x, y, z, 0);
		}
	}
	
	/**
	 * Occasionally, all players need to be sent out of the zone (for example, if the players are just running around without fighting for too long, or if all players die, etc). This call sends all online players to town and marks offline players to be teleported (by clearing their relog expiration
	 * times) when they log back in (no real need for off-line teleport).
	 */
	public void oustAllPlayers()
	{
		if (_characterList.isEmpty())
			return;
		
		for (L2PcInstance player : getKnownTypeInside(L2PcInstance.class))
		{
			if (player.isOnline())
			{
				if (_oustLoc[0] != 0 && _oustLoc[1] != 0 && _oustLoc[2] != 0)
					player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], 0);
				else
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
		}
		_playerAllowedReEntryTimes.clear();
		_playersAllowed.clear();
	}
	
	/**
	 * This function is to be used by external sources, such as quests and AI in order to allow a player for entry into the zone for some time.<BR>
	 * <BR>
	 * Naturally if the player does not enter within the allowed time, he/she will be teleported out again...
	 * @param player Reference to the player we wish to allow.
	 * @param durationInSec Amount of time in seconds during which entry is valid.
	 */
	public void allowPlayerEntry(L2PcInstance player, int durationInSec)
	{
		if (!_playersAllowed.contains(player.getObjectId()))
			_playersAllowed.add(player.getObjectId());
		
		_playerAllowedReEntryTimes.put(player.getObjectId(), System.currentTimeMillis() + durationInSec * 1000);
	}
	
	public void removePlayer(L2PcInstance player)
	{
		_playersAllowed.remove(Integer.valueOf(player.getObjectId()));
		_playerAllowedReEntryTimes.remove(player.getObjectId());
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
}