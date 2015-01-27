package net.sf.l2j.gameserver.masteriopack.imageconverter;

import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * Packet used as PledgeCrest packet.
 * @author Masterio
 */
public class ImageServerPacket extends L2GameServerPacket
{
	private final int _crestId;
	private final byte[] _data;
	
	public ImageServerPacket(int crestId, byte[] data)
	{
		_crestId = crestId;
		_data = data;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x6c); // IL client: 0x6c, H5 client: 0x6a
		writeD(_crestId);
		
		if (_data != null)
		{
			writeD(_data.length);
			writeB(_data);
		}
		else
		{
			writeD(0);
		}
	}

	@Override
	public String getType()
	{
		return null;
	}
}
