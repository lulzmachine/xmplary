package se.lolcalhost.xmplary.common;

import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.json.JSONException;

import se.lolcalhost.xmplary.common.commands.Command;
import se.lolcalhost.xmplary.common.exceptions.AuthorizationFailureException;

/**
 * This command runner is a consumer that serves two purposes: to serve as an executor for the commands,
 * and to gather the running commands into the same thread. Since sqlite doesn't like multithreading,
 * and since race conditions will appear, gathering all commands to a single thread feels like a good idea.
 * 
 * Like a really awesome idea, actually.
 * 
 * @author sx00042
 *
 */
public class XMPCommandRunner extends Thread {
	protected static Logger logger = Logger.getLogger(XMPCommandRunner.class);
	long commandsRun = 0;
	
	public XMPCommandRunner() {
		super("XMPCommandRunner");
	}

	static BlockingQueue<Command> queue = new PriorityBlockingQueue<Command>();
	/**
	 * Schedule a command to be run.
	 * 
	 * @param c
	 */
	public synchronized static void scheduleCommand(Command c) {
		try {
			queue.put(c);
		} catch (InterruptedException e) {
			logger.error("Command runner PUT interrupted: ", e);
		}
	}
	
	public void run() {
		try {
			while(true) {
				Command taken = queue.take();
				logger.info("Running command: [Q/P]["+queue.size()+"/"+taken.getPriority().ordinal()+"][n."+commandsRun+"]: " + taken.toString());
				long before = System.currentTimeMillis();
				consume(taken);
				commandsRun++;
				long after = System.currentTimeMillis();
				long diff = after - before;
				
				if (taken.hasLogMessage()) {
					logger.info(taken.toString() + " took " + diff + " millis. Message: " + taken.logMessage());
				} else {
					logger.info(taken.toString() + " took " + diff + " millis.");
				}
			}
		} catch (InterruptedException e) {
			logger.error("Command runner TAKE interrupted: ", e);
		}
	}

	private void consume(Command take) {
		try {
			take.execute();
		} catch (JSONException e) {
			logger.error("Error in command execution: ", e);
		} catch (SQLException e) {
			logger.error("Error in command execution: ", e);
		} catch (AuthorizationFailureException e) {
			logger.error("Error in command execution: ", e);
		} catch (Exception e) {
			logger.error("Error in command execution: ", e);
		}
	}

	public static int getQueueSize() {
		return queue.size();
	}
	
}
