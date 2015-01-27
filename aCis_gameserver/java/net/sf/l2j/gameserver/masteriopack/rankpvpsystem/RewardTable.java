package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class contains informations about Rewards for reach a Rank.<br>
 * Each reward is defined in database and it is static table in game.
 * @author Masterio
 */
public class RewardTable
{
	public static final Logger log = Logger.getLogger(RewardTable.class.getName());
	
	private static RewardTable _instance = null;
	
	/** [Reward ID, Reward object] */
	private Map<Integer, Reward> _pvpRewardList = new HashMap<>();
	
	/** [Reward ID, Reward object] */
	private Map<Integer, Reward> _lvlRewardList = new HashMap<>();
	
	private RewardTable()
	{
		long startTime = Calendar.getInstance().getTimeInMillis();
		
		load();

		long endTime = Calendar.getInstance().getTimeInMillis();
		
		log.info(" - RewardTable: Data loaded. PvP: " + _pvpRewardList.size() +", Level: "+ _lvlRewardList.size() + " objects in " + (endTime - startTime) + " ms.");
	}
	
	public static RewardTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new RewardTable();
		}
		
		return _instance;
	}
	
	/**
	 * [Reward ID, Reward object]
	 * @return
	 */
	public Map<Integer, Reward> getPvpRewardList()
	{
		return _pvpRewardList;
	}
	
	/**
	 * [Reward ID, Reward object]
	 * @param pvpRewardList
	 */
	public void setPvpRewardList(Map<Integer, Reward> pvpRewardList)
	{
		_pvpRewardList = pvpRewardList;
	}
	
	/**
	 * [Reward ID, Reward object]
	 * @return
	 */
	public Map<Integer, Reward> getLvlRewardList()
	{
		return _lvlRewardList;
	}
	
	/**
	 * [Reward ID, Reward object]
	 * @param lvlRewardList
	 */
	public void setLvlRewardList(Map<Integer, Reward> lvlRewardList)
	{
		_lvlRewardList = lvlRewardList;
	}
	
	/**
	 * Returns the list of rewards by player rank.
	 * @param player
	 * @return
	 */
	public Map<Integer, Reward> getRankPvpRewardList(L2PcInstance player)
	{
		// reward_id, Reward
		Map<Integer, Reward> list = new HashMap<>();
		
		int rankId = PvpTable.getInstance().getRankId(player.getObjectId());
		
		if(rankId < 0)
			return list;
		
		for(Map.Entry<Integer, Reward> e : _pvpRewardList.entrySet())
		{
			Reward reward = e.getValue();
			
			if(reward != null && reward.getRankId() == rankId)
			{
				list.put(reward.getId(), reward);
			}
		}

		return list;
	}
	
	/**
	 * Returns the list of rewards by player rank.
	 * @param rankId
	 * @return
	 */
	public Map<Integer, Reward> getRankPvpRewardList(int rankId)
	{
		// reward_id, Reward
		Map<Integer, Reward> list = new HashMap<>();

		if(rankId < 0)
			return list;
		
		for(Map.Entry<Integer, Reward> e : _pvpRewardList.entrySet())
		{
			Reward reward = e.getValue();
			
			if(reward != null && reward.getRankId() == rankId)
			{
				list.put(reward.getId(), reward);
			}
		}

		return list;
	}
	
	/**
	 * Gives the all new Rank Rewards for player.
	 * @param player
	 * @param rankId 
	 */
	public void giveRankLvlRewards(L2PcInstance player, int rankId)
	{
		if (player == null)
			return;

		// overloads inventory, there is no repeat method for this action. Reward is given only in rank level time.

		// add items into player's inventory:
		for(Map.Entry<Integer, Reward> e : _lvlRewardList.entrySet())
		{
			Reward reward = e.getValue();
			
			// Rank should never decreased.	//TODO
			if(reward != null && reward.getRankId() == rankId)
			{
    			player.addItem("Reward", reward.getItemId(), (int) reward.getItemAmount(), player, true);
			}
		}
	}
	
	/**
	 * Gives the all new Rank Rewards for player.
	 * @param player
	 * @param victimRankId 
	 */
	public void giveRankPvpRewards(L2PcInstance player, int victimRankId)
	{
		if (player == null)
			return;

		// overloads inventory, there is no repeat method for this action. Reward is given only in pvp time.

		// add items into player's inventory:
		for(Map.Entry<Integer, Reward> e : _pvpRewardList.entrySet())
		{
			Reward reward = e.getValue();

			if(reward != null && reward.getRankId() == victimRankId)
			{
    			player.addItem("Reward", reward.getItemId(), (int) reward.getItemAmount(), player, true);
			}
		}
	}
	
	/**
	 * Give the item for the Player.
	 * @param player
	 */
	public void giveReward(L2PcInstance player)
	{
		if (player == null || RankPvpSystemConfig.PVP_REWARD_ID <= 0 || RankPvpSystemConfig.PVP_REWARD_AMOUNT <= 0)
			return;

		// overloads inventory, there is no repeat method for this action. Reward is given only in rank level time.
		player.addItem("Reward", RankPvpSystemConfig.PVP_REWARD_ID, (int) RankPvpSystemConfig.PVP_REWARD_AMOUNT, player, true);
	}
	
	private void load()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			// PvP + Level Rewards
			PreparedStatement statement = con.prepareStatement("SELECT * FROM rank_pvp_system_rank_reward ORDER BY id ASC");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
			    Reward r = new Reward();
				
				r.setId(rset.getInt("id"));
				r.setItemId(rset.getInt("item_id"));
				r.setItemAmount(rset.getLong("item_amount"));
				r.setRankId(rset.getInt("rank_id"));
				
				if(RankPvpSystemConfig.RANK_PVP_REWARD_ENABLED && rset.getBoolean("is_pvp"))
					_pvpRewardList.put(r.getId(), r);
				
				if(RankPvpSystemConfig.RANK_LEVEL_REWARD_ENABLED && rset.getBoolean("is_level"))
					_lvlRewardList.put(r.getId(), r);
			}
			
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			log.info(e.getMessage());
		}
		finally
		{
			try
			{
				if (con != null)
				{
					con.close();
				}
			}
			catch (Exception e)
			{
				log.info(e.getMessage());
			}
		}
	}
}
