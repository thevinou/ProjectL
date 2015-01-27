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
package net.sf.l2j.gameserver.model.entity;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class VoteRewardHopzone
{
       // Configurations.
       private static String hopzoneUrl = Config.HOPZONE_SERVER_LINK;
       private static int voteRewardVotesDifference = Config.HOPZONE_VOTES_DIFFERENCE;
       private static int checkTime = 60*1000*Config.HOPZONE_REWARD_CHECK_TIME;
      
       // Don't-touch variables.
       private static int lastVotes = 0;
       private static HashMap<String, Integer> playerIps = new HashMap<>();
      
       public static void updateConfigurations()
       {
               hopzoneUrl = Config.HOPZONE_SERVER_LINK;
               voteRewardVotesDifference = Config.HOPZONE_VOTES_DIFFERENCE;
               checkTime = 60*1000*Config.HOPZONE_REWARD_CHECK_TIME;
       }
      
       public static void getInstance()
       {
    	   Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
               System.out.println("Hopzone - Vote reward system initialized.");
               ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
               {
                       @Override
                       public void run()
                       {
                               if (Config.ALLOW_HOPZONE_VOTE_REWARD)
                               {
                                       reward();
                               }
                               else
                               {
                                       return;
                               }
                       }
               }, checkTime/2, checkTime);
       }
      
       static void reward()
       {
    	   Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
               int currentVotes = getVotes();
              
               if (currentVotes == -1)
               {
                       if (currentVotes == -1)
                       {
                               System.out.println("There was a problem on getting Hopzone server votes.");
                       }
                      
                       return;
               }
              
               if (lastVotes == 0)
               {
                       lastVotes = currentVotes;
                       Announcements.announceToAll("Hopzone: Vote count is "+currentVotes+".");
                       Announcements.announceToAll("Hopzone: We need "+((lastVotes+voteRewardVotesDifference)-currentVotes)+" vote(s) for reward.");
                       if (Config.ALLOW_HOPZONE_GAME_SERVER_REPORT)
                       {
                               System.out.println("Server votes on hopzone: "+currentVotes);
                               System.out.println("Votes needed for reward: "+((lastVotes+voteRewardVotesDifference)-currentVotes));
                       }
                       return;
               }
              
               if (currentVotes >= lastVotes+voteRewardVotesDifference)
               {
                       Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
                       
                       if (Config.ALLOW_HOPZONE_GAME_SERVER_REPORT)
                       {
                               System.out.println("Server votes on hopzone: "+currentVotes);
                               System.out.println("Votes needed for next reward: "+((currentVotes+voteRewardVotesDifference)-currentVotes));
                       }
                       Announcements.announceToAll("Hopzone: Everyone has been rewarded.");
                       Announcements.announceToAll("Hopzone: Current vote count is "+currentVotes+".");
                       for (L2PcInstance p : pls)
                       {
                               boolean canReward = false;
                               String pIp = p.getClient().getConnection().getInetAddress().getHostAddress();
                               if (playerIps.containsKey(pIp))
                               {
                                       int count = playerIps.get(pIp);
                                       if (count < Config.HOPZONE_DUALBOXES_ALLOWED)
                                       {
                                               playerIps.remove(pIp);
                                               playerIps.put(pIp, count+1);
                                               canReward = true;
                                       }
                               }
                               else
                               {
                                       canReward = true;
                                       playerIps.put(pIp, 1);
                               }
                               if (canReward)
                               {
                                       for (int i = 0; i < Config.HOPZONE_REWARD.length; i++)
                                       {
                                               p.addItem("Vote reward.", Config.HOPZONE_REWARD[i][0], Config.HOPZONE_REWARD[i][1], p, true);
                                       }
                               }
                               else
                               {
                                       p.sendMessage("Already "+Config.HOPZONE_DUALBOXES_ALLOWED+" character(s) of your ip have been rewarded, so this character won't be rewarded.");
                               }
                       }
                       playerIps.clear();
               }
               lastVotes = currentVotes;
       }
      
       private static int getVotes()
       {
    	   int votes = -1;
    	   
    	   try
  			{
  				final WebClient webClient = new WebClient(BrowserVersion.CHROME);
  				webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
  				webClient.getOptions().setThrowExceptionOnScriptError(false);
  				webClient.getOptions().setPrintContentOnFailingStatusCode(false);
  				webClient.setJavaScriptEngine(new JavaScriptEngine(webClient));
  				final HtmlPage page = webClient.getPage(hopzoneUrl);
  				
  				String fullPage = page.asXml();
  				int constrainA = fullPage.indexOf("rank anonymous tooltip") + 24;
  				String voteSection = fullPage.substring(constrainA);
  				int constrainB = voteSection.indexOf("span") - 2;
  				voteSection = voteSection.substring(0, constrainB).trim();
  				votes = Integer.parseInt(voteSection);
  				
  				page.cleanUp();
  				webClient.getJavaScriptEngine().shutdown();
  				webClient.closeAllWindows();
  			}
  			catch (IOException e)
  			{
  				System.out.println("[VoteRewardManager]: Problem occured while getting Hopzone votes. Error Trace: " + e.getMessage());
  			}
   			return votes;
       }
}