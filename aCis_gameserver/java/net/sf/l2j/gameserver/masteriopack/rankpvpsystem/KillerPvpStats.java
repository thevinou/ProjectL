package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The class contains summary of killer PvP's, and list of all PvP's with his victims.
 * @author Masterio
 */
public class KillerPvpStats
{
	private int _killerId = 0;				// killer id
	
	private int _totalKills = 0;			// sum of kill's 
	//private int _totalKillsToday = 0;		// sum of kill's in this day (unused)
	private int _totalKillsLegal = 0;		// sum of legal kill's
	//private int _totalKillsLegalToday = 0;	// sum of legal kill's in this day. (unused)
	private long _totalRankPoints = 0;		// sum of rank point's
	private long _totalRankPointsToday = 0;	// sum of rank point's in this day
	private int _totalWarKills = 0;			// sum of kill's on victims from war clan
	private int _totalWarKillsLegal = 0;	// sum of legal kill's on victims from war clan

	private int _rankId = 0;				// id of current rank
	
	/** 
	 * [victim_id, PvP] contains killer pvp's data (victim, kills on victim, last kill time, etc.) 
	 */
	private Map<Integer, Pvp> _victimPvpTable = new ConcurrentHashMap<>();	// used by many threads

	private long _lastKillTime = 0; // store last <legal> kill time
	
	public void addTotalKills(int kills)
	{
		_totalKills += kills;
	}
	/*
	public void addTotalKillsToday(int killsToday)
	{
		_totalKillsToday += killsToday;
	}*/
	
	public void addTotalKillsLegal(int killsLegal)
	{
		_totalKillsLegal += killsLegal;
	}
	/*
	public void addTotalKillsLegalToday(int killsLegalToday)
	{
		_totalKillsLegalToday += killsLegalToday;
	}
	*/
	/** Add Rank Points to Total Rank Points and update Rank. <br>
	 * Is NOT recommended use this method in LOOP, because this method use: <b>onUpdateRankPoints()</b>.
	 * @param rankPoints */
	public void addTotalRankPoints(long rankPoints)
	{
		_totalRankPoints += rankPoints;
		onUpdateRankPoints();
	}
	
	/** Add Rank Points to Total Rank Points and <b>NOT</b> update Rank. <br>
	 * After finish adding points, you should execute this method: <b>onUpdateRankPoints();</b> 
	 * @param rankPoints */
	public void addTotalRankPointsOnly(long rankPoints)
	{
		_totalRankPoints += rankPoints;
	}
	
	public void addTotalRankPointsToday(long rankPointsToday)
	{
		_totalRankPointsToday += rankPointsToday;
	}
	
	public void addTotalWarKills(int warKills)
	{
		_totalWarKills += warKills;
	}
	
	public void addTotalWarKillsLegal(int warKillsLegal)
	{
		_totalWarKillsLegal += warKillsLegal;
	}

	/**
	 * @return the _killerId
	 */
	public int getKillerId()
	{
		return _killerId;
	}
	/**
	 * @param killerId the _killerId to set
	 */
	public void setKillerId(int killerId)
	{
		_killerId = killerId;
	}
	/**
	 * @return the _totalKills
	 */
	public int getTotalKills()
	{
		return _totalKills;
	}
	/**
	 * @param totalKills the _totalKills to set
	 */
	public void setTotalKills(int totalKills)
	{
		_totalKills = totalKills;
	}
	/**
	 * @return the _totalKillsToday
	 *//*
	public int getTotalKillsToday()
	{
		return _totalKillsToday;
	}*/
	/**
	 * @param totalKillsToday the _totalKillsToday to set
	 *//*
	public void setTotalKillsToday(int totalKillsToday)
	{
		_totalKillsToday = totalKillsToday;
	}*/
	/**
	 * @return the _totalKillsLegal
	 */
	public int getTotalKillsLegal()
	{
		return _totalKillsLegal;
	}
	/**
	 * @param totalKillsLegal the _totalKillsLegal to set
	 */
	public void setTotalKillsLegal(int totalKillsLegal)
	{
		_totalKillsLegal = totalKillsLegal;
	}
	/**
	 * @return the _totalKillsLegalToday
	 *//*
	public int getTotalKillsLegalToday()
	{
		return _totalKillsLegalToday;
	}*/
	/**
	 * @param totalKillsLegalToday the _totalKillsLegalToday to set
	 *//*
	public void setTotalKillsLegalToday(int totalKillsLegalToday)
	{
		_totalKillsLegalToday = totalKillsLegalToday;
	}*/
	/**
	 * @return the _totalRankPoints
	 */
	public long getTotalRankPoints()
	{
		return _totalRankPoints;
	}
	/**
	 * Set Total Rank Points and update Rank.<br>
	 * Is NOT recommended use this method in LOOP, because this method use: <b>onUpdateRankPoints()</b>.
	 * @param totalRankPoints the _totalRankPoints to set
	 */
	public void setTotalRankPoints(long totalRankPoints)
	{
		this._totalRankPoints = totalRankPoints;
		onUpdateRankPoints();
	}
	/**
	 * Set Total Rank Points and update Rank.<br>
	 * The Rank is <b>NOT</b> updated here!
	 * @param totalRankPoints
	 */
	public void setTotalRankPointsOnly(long totalRankPoints)
	{
		_totalRankPoints = totalRankPoints;
	}
	/**
	 * @return the _totalRankPointsToday
	 */
	public long getTotalRankPointsToday()
	{
		return _totalRankPointsToday;
	}
	/**
	 * @param totalRankPointsToday the _totalRankPointsToday to set
	 */
	public void setTotalRankPointsToday(long totalRankPointsToday)
	{
		_totalRankPointsToday = totalRankPointsToday;
	}
	/**
	 * @return the _totalWarKills
	 */
	public int getTotalWarKills()
	{
		return _totalWarKills;
	}
	/**
	 * @param totalWarKills the _totalWarKills to set
	 */
	public void setTotalWarKills(int totalWarKills)
	{
		_totalWarKills = totalWarKills;
	}
	/**
	 * @return the _totalWarKillsLegal
	 */
	public int getTotalWarKillsLegal()
	{
		return _totalWarKillsLegal;
	}
	/**
	 * @param totalWarKillsLegal the _totalWarKillsLegal to set
	 */
	public void setTotalWarKillsLegal(int totalWarKillsLegal)
	{
		_totalWarKillsLegal = totalWarKillsLegal;
	}

	/**
	 * @return the _rank
	 */
	public int getRankId()
	{
		return _rankId;
	}
	
	public Rank getRank()
	{
		return RankTable.getInstance().getRankById(getRankId());
	}

	/**
	 * @param rankId the _rank to set
	 */
	public void setRankId(int rankId)
	{
		_rankId = rankId;
	}
	
	/** Update current Rank values for this character,<br>should be executed always when total rank points was updated. */
	public void onUpdateRankPoints()
	{
		Map<Integer, Rank> list = RankTable.getInstance().getRankList();

		// if total points equals 0 return minimum rank:
		if(_totalRankPoints == 0)
		{
			if(!list.isEmpty() && list.containsKey(1) && list.get(1) != null)
			{
				_rankId = list.get(1).getId();
				return;
			}
		}

		int rankId = 1; // ranks starts from id = 1.
		for (Map.Entry<Integer, Rank> e : list.entrySet()) 
		{
			Rank rank = e.getValue();

			if(rank != null)
			{
    			// set up rank for current rank points:
    			// MinPoints checked from 0!
    			if(_totalRankPoints < rank.getMinPoints())
    			{
    				_rankId = rankId;
    				return;
    			}
    			
    			rankId = rank.getId();
			}
	    }

		// set maximum rankId:
		_rankId = rankId; 
	}

	/** 
	 * Updates daily fields like: total kills today, etc.<br> 
	 * Other fields are actual (updated on each PvP).
	 * @param systemDay 
	 * */
	public void updateDailyStats(long systemDay)
	{
		//int totalKillsToday = 0;
		//int totalKillsLegalToday = 0;
		long totalRankPointsToday = 0;
		
		for (Map.Entry<Integer, Pvp> e : _victimPvpTable.entrySet()) 
		{
			Pvp pvp = e.getValue();
			
			if(pvp != null)
			{
    			if(pvp.getKillDay() == systemDay)
    			{
    				//totalKillsToday += pvp.getKillsToday();
    				//totalKillsLegalToday += pvp.getKillsLegalToday();
    				totalRankPointsToday += pvp.getRankPointsToday();
    			}
    			else
    			{
    				pvp.resetDailyFields();
    			}
			}
	    }
		
		//_totalKillsToday = totalKillsToday;
		//_totalKillsLegalToday = totalKillsLegalToday;
		_totalRankPointsToday = totalRankPointsToday;
	}
	
	public Map<Integer, Pvp> getVictimPvpTable()
	{
		return _victimPvpTable;
	}

	public void setVictimPvpTable(Map<Integer, Pvp> victimPvpTable)
	{
		_victimPvpTable = victimPvpTable;
	}

	/**
	 * Add pvp into victimPvpTable and updates killerPvpStats.<br>
	 * Used when load from database ONLY.
	 * @param pvp
	 * @return 
	 */
	public boolean addVictimPvp(Pvp pvp)
	{
		// add PvP:
		_victimPvpTable.put(pvp.getVictimId(), pvp);
		
		// update killer pvp stats:
		addTotalKills(pvp.getKills());
		//addTotalKillsToday(pvp.getKillsToday());
		addTotalKillsLegal(pvp.getKillsLegal());
		//addTotalKillsLegalToday(pvp.getKillsLegalToday());
		
		addTotalRankPoints(pvp.getRankPoints());
		addTotalRankPointsToday(pvp.getRankPointsToday());
		addTotalWarKills(pvp.getWarKills());
		addTotalWarKillsLegal(pvp.getWarKillsLegal());
		
		// set last kill time:
		if(pvp.getKillTime() > getLastKillTime())
		{
			_lastKillTime = pvp.getKillTime();
		}

		return true;
	}

	public long getLastKillTime()
	{
		return _lastKillTime;
	}

	public void setLastKillTime(long lastKillTime) 
	{
		_lastKillTime = lastKillTime;
	}
}
