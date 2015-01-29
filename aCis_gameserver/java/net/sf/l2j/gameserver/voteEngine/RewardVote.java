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
package net.sf.l2j.gameserver.voteEngine;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
/**
 * @author Vianney
 *
 */
public class RewardVote implements IUserCommandHandler
{
		private static enum ValueType
		{
			ACCOUNT_NAME,
			IP_ADRESS,
			HWID
		}
		
		private static final Logger _log = Logger.getLogger(RewardVote.class.getName());
		
		private static final int[] COMMAND_IDS =
		{
			1204
		};
		
		private static int WAIT_TIME = 5;
		private static int MIN_LEVEL_TO_ALLOW_VOTE = 40;
		private static final long INTERVAL = WAIT_TIME * 60 * 1000; // in minutes.
		private static int rewardId = 57;
		private static int rewardAmount = 10000;
		
		public List<L2PcInstance> _safePeople = new ArrayList<>();
		
		@Override
		public boolean useUserCommand(int id, L2PcInstance activeChar)
		{
				if (_safePeople.contains(activeChar))
				{
					activeChar.sendMessage("You can use this command only once at " + WAIT_TIME + " minutes.");
					return false;
				}
				
				_safePeople.add(activeChar);
				ThreadPoolManager.getInstance().scheduleGeneral(new PopSafePlayer(activeChar), INTERVAL);
				
				// getting IP of client, here we will have to check for HWID when we have LAMEGUARD
				InetAddress IPClient = activeChar.getClient().getConnection().getInetAddress();
				// String IPIntern= activeChar.getClient().getConnectionAddress().get
				// sending IP to client for debug purpose
				// ??
				// Return 0 if he didnt voted. Date when he voted on website
				long dateHeVotedOnWebsite = VoteRead.checkVotedIP(IPClient);
				if (dateHeVotedOnWebsite > 0)
				{
					if (activeChar.getLevel() < MIN_LEVEL_TO_ALLOW_VOTE)
					{
						activeChar.sendMessage("You need to be at least " + String.valueOf(MIN_LEVEL_TO_ALLOW_VOTE) + " level in order to use this command.");
						return false;
					}
					
					String uniqueID = "";
					// uniqueID = activeChar.getClient().getHWID();
					// if (uniqueID == null)
					// {
					// uniqueID = "";
					// }
					
					// Calculate if he can take reward
					if (canTakeReward(dateHeVotedOnWebsite, IPClient, activeChar, uniqueID))
					{
						_log.info(activeChar.getName() + " got rewarded");
						insertInDataBase(dateHeVotedOnWebsite, IPClient, activeChar, uniqueID);
						
						activeChar.getInventory().addItem("VoteReward", rewardId, rewardAmount, activeChar, null);
						activeChar.sendMessage("Successfully rewarded.");
					}
				}
				else
				// He didnt voted.
				{
					activeChar.sendMessage("You didnt voted. Try again later!");
					return false;
				}
				return true;
		}
		
		/**
		 * @param dateHeVotedOnWebsite
		 * @param iPClient
		 * @param activeChar
		 * @param HwID
		 **/
		private static void insertInDataBase(long dateHeVotedOnWebsite, InetAddress iPClient, L2PcInstance activeChar, String HwID)
		{
			insertInDataBase(dateHeVotedOnWebsite, activeChar.getAccountName(), ValueType.ACCOUNT_NAME);
			insertInDataBase(dateHeVotedOnWebsite, iPClient.toString(), ValueType.IP_ADRESS);
			// insertInDataBase(dateHeVotedOnWebsite, HwID, ValueType.HWID);
		}
		
		private static void insertInDataBase(long dateHeVotedOnWebsite, String string, ValueType type)
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT * FROM votes WHERE value=? AND value_type=?");
				statement.setString(1, string.toString());
				statement.setInt(2, type.ordinal());
				rset = statement.executeQuery();
				
				if (rset.next()) // He already exit in database because he voted before.
				{
					int count = rset.getInt("vote_count");
					PreparedStatement statement2 = null;
					try
					{
						statement2 = con.prepareStatement("UPDATE votes SET date_voted_website=?, date_take_reward_in_game=?, vote_count=? WHERE value=? AND value_type=?");
						statement2.setLong(1, dateHeVotedOnWebsite);
						statement2.setLong(2, (System.currentTimeMillis() / 1000L));
						statement2.setInt(3, (count + 1));
						statement2.setString(4, string);
						statement2.setInt(5, type.ordinal());
						statement2.executeUpdate();
					}
					catch (SQLException e)
					{
						_log.warning("RewardVote:insertInDataBase(long,String,ValueType): " + e);
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
							_log.info(e.getMessage());
						}
					}
				}
				else
				{
					PreparedStatement statement2 = null;
					try
					{
						statement2 = con.prepareStatement("INSERT INTO votes(value, value_type, date_voted_website, date_take_reward_in_game, vote_count) VALUES (?, ?, ?, ?, ?)");
						statement2.setString(1, string);
						statement2.setInt(2, type.ordinal());
						statement2.setLong(3, dateHeVotedOnWebsite);
						statement2.setLong(4, (System.currentTimeMillis() / 1000L));
						statement2.setInt(5, 1);
						statement2.execute();
					}
					catch (SQLException e)
					{
						_log.warning("RewardVote:insertInDataBase(long,String,ValueType): " + e);
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
							_log.info(e.getMessage());
						}
					}
				}
			}
			catch (SQLException e)
			{
				_log.warning("RewardVote:insertInDataBase(long,String,ValueType): " + e);
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
					_log.info(e.getMessage());
				}
			}
		}
		
		private static boolean canTakeReward(long dateHeVotedOnWebsite, InetAddress iPClient, L2PcInstance activeChar, String HwID)
		{
			int whenCanVote = canTakeReward(dateHeVotedOnWebsite, activeChar.getAccountName(), ValueType.ACCOUNT_NAME);
			int whenCanVoteIP = canTakeReward(dateHeVotedOnWebsite, iPClient.toString(), ValueType.IP_ADRESS);
			int whenCanVoteHWID = canTakeReward(dateHeVotedOnWebsite, HwID, ValueType.HWID);
			
			whenCanVote = Math.max(whenCanVote, Math.max(whenCanVoteIP, whenCanVoteHWID));
			
			if (whenCanVote > 0)
			{
				if (whenCanVote > 60)
				{
					activeChar.sendMessage("You can vote only once at 12 hours and 5 minutes. You still have to wait " + (whenCanVote / 60) + " hours " + (whenCanVote % 60) + " minutes.");
				}
				else
				{
					activeChar.sendMessage("You can vote only once at 12 hours and 5 minutes. You still have to wait " + whenCanVote + " minutes.");
				}
				return false;
			}
			return true;
		}
		
		private static int canTakeReward(long dateHeVotedOnWebsite, String value, ValueType type)
		{
			int dateLastVote = 0; // Date When he last voted on server
			int whenCanVote = 0; // The number of minutes when he can vote
			
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT date_take_reward_in_game FROM votes WHERE value=? AND value_type=?");
				statement.setString(1, value);
				statement.setInt(2, type.ordinal());
				rset = statement.executeQuery();
				
				if (rset.next())
				{
					dateLastVote = rset.getInt("date_take_reward_in_game");
				}
			}
			catch (SQLException e)
			{
				_log.warning("RewardVote:canTakeReward(long,String,String): " + e);
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
					_log.info(e.getMessage());
				}
			}
			
			// The number of minutes when he can vote
			if (dateLastVote == 0)
			{
				whenCanVote = (int) ((dateHeVotedOnWebsite - (System.currentTimeMillis() / 1000L)) / 60);
			}
			else
			{
				whenCanVote = (int) (((dateLastVote + (12 * 60 * 60) + 300) - (System.currentTimeMillis() / 1000L)) / 60);
			}
			
			return whenCanVote;
		}
		
		// this is the class to remove safe players to be reported again after the given time
		private class PopSafePlayer implements Runnable
		{
			private final L2PcInstance _safeplayer;
			
			public PopSafePlayer(L2PcInstance safeplayer)
			{
				_safeplayer = safeplayer;
			}
			
			@Override
			public void run()
			{
				if (_safePeople.contains(_safeplayer))
				{
					_safePeople.remove(_safeplayer);
				}
			}
		}
		
		@Override
		public int[] getUserCommandList()
		{
			return COMMAND_IDS;
		}
}
