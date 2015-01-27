package net.sf.l2j.gameserver.masteriopack.rankpvpsystem;

/**
 * @author Masterio
 */
public class RPC 
{
	private int _playerId = 0;					// player id
	private long _rpcTotal = 0;					// total RPC
	private long _rpcCurrent = 0;				// current RPC

	private byte _dbStatus = DBStatus.NONE;		// if NONE it is mean, the values are not changed and is not require the database update.
		
	public RPC()
	{
		
	}
	
	public RPC(int playerId)
	{
		_playerId = playerId;
	}
	
	public int getPlayerId()
	{
		return _playerId;
	}
	
	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}
	
	public long getRpcTotal()
	{
		return _rpcTotal;
	}
	
	public void setRpcTotal(long rpcTotal) 
	{
		_rpcTotal = rpcTotal;
	}
	
	public long getRpcCurrent()
	{
		return _rpcCurrent;
	}
	
	public void setRpcCurrent(long rpcCurrent) 
	{
		_rpcCurrent = rpcCurrent;
	}
	
	/**
	 * Decrease RPC, and update DBStatus into updated, or leave inserted and return RpcCurrent.
	 * @param decreaseValue
	 * @return 
	 */
	public long decreaseRpcCurrentBy(long decreaseValue)
	{
		_rpcCurrent -= decreaseValue;
		
		if(_dbStatus != DBStatus.INSERTED)
		{
			_dbStatus = DBStatus.UPDATED;
		}
		
		return _rpcCurrent;
	}
	
	public void increaseRpcTotalAndCurrentBy(long increaseValue)
	{
		_rpcCurrent += increaseValue;
		_rpcTotal += increaseValue;
		
		if(_dbStatus != DBStatus.INSERTED)
		{
			_dbStatus = DBStatus.UPDATED;
		}
	}

	public byte getDbStatus()
	{
		return _dbStatus;
	}

	/**
	 * DBStatus class as parameter.
	 * @param dbStatus
	 */
	public void setDbStatus(byte dbStatus)
	{
		_dbStatus = dbStatus;
	}
	
}
