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

/**
 * @author Vianney
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.logging.Logger;

public class VoteRead
{
		private static final Logger _log = Logger.getLogger(VoteRead.class.getName());
		// Enter your http://l2network.eu/ server id
		// Example:
		// If your server link is http://l2network.eu/?a=details&u=Anius then your id is Anius
		private static String L2NetworkServerId = "Anius";
		
		public static long checkVotedIP(InetAddress iPClient)
		{
			long voteDate = 0;
			if (checkIfVoted("Network", iPClient))
			{
				voteDate = System.currentTimeMillis() / 1000L;
			}
			return voteDate;
		}
		
		private static boolean checkIfVoted(String Topsite, InetAddress iPClient)
		{
			String WordToCheck = "";
			boolean voted = false;
			URL url = null;
			InputStreamReader isr = null;
			try
			{
				switch (Topsite)
				{
					case "Network":
						String ip =iPClient.toString().substring(1);
						url = new URL("http://l2network.eu/index.php?a=in&u=" + L2NetworkServerId + "&ipc=" + ip);
						WordToCheck = "0"; // This is the word that API returns if you haven't voted for Network
						break;
				}
				
				if (url != null)
				{
					isr = new InputStreamReader(url.openStream());
					BufferedReader br = new BufferedReader(isr);
					String strLine;
					while ((strLine = br.readLine()) != null) // Read File Line By Line
					{
						if (!strLine.equals(WordToCheck))
						{
							voted = true;
						}
					}
					isr.close(); // Close the input stream
				}
			}
			catch (Exception e) // Catch exception if any
			{
				_log.warning("VoteRead: ERROR: " + e);
			}
			return voted;
		}
}
