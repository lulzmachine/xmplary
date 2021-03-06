package se.lolcalhost.xmplary.common.commands;

import java.io.IOException;
import java.sql.SQLException;

import org.jivesoftware.smack.packet.Message;
import org.json.JSONException;

import se.lolcalhost.xmplary.common.XMPMain;
import se.lolcalhost.xmplary.common.exceptions.AuthorizationFailureException;
import se.lolcalhost.xmplary.common.models.XMPMessage;
import se.lolcalhost.xmplary.common.models.XMPMessage.MessageType;

public class UnpackAndReceiveMessage extends Command {

	private Message message;

	/**
	 * 
	 * Read a message into an XMPMessage, try to verify and then send it on to the receivers.
	 * @param xmpMain
	 * @param msg
	 * @param message
	 */
	public UnpackAndReceiveMessage(XMPMain xmpMain, XMPMessage msg,
			Message message) {
		super(xmpMain, msg);
		setPriority(CommandPriority.INCOMING);
		this.message = message;
	}

	@Override
	public void execute() throws JSONException, SQLException,
			AuthorizationFailureException, IOException {
		logger.info(String.format("Attempting to parse message %s ...",
				message.getBody()));
		msg = XMPMessage.unpack(message);
		
		if (msg != null) {
			if (msg.shoudDecrypt()) {
				msg.decrypt();
			}
			boolean verified = false;
			if (msg.shouldVerify()) {
				verified = msg.verify();
			}
			logger.info("Message parsed! It's of type " + msg.getType() + ". Verified: " + (msg.shouldVerify() ? msg.isVerified() : "(shouldn't be verified)"));
			msg.save();
			
			// TODO: I just added this line: is it good? TEST IT.
			if (verified || !msg.shouldVerify()) {
				main.runReceiveHandlers(msg);
			}
			if (msg.shouldVerify() && !msg.isVerified()) {
				RequestRegistrationCommand rrc = new RequestRegistrationCommand(main, msg);
				rrc.schedule();
			}
		}
	}

}
