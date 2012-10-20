package de.reneruck.tcd.ipp.helicopter.actions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.TemporalTransitionsStore;
import de.reneruck.tcd.ipp.datamodel.Transition;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;

public class ReceiveData implements Action {

	private OutputStream out;
	private TemporalTransitionsStore transitionStorage;

	public ReceiveData(OutputStream out, TemporalTransitionsStore transitionsQueue) {
		this.out = out;
		this.transitionStorage = transitionsQueue;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		Object content = event.getParameter(Statics.CONTENT_TRANSITION);
		if(content != null && content instanceof Transition) {
			this.transitionStorage.addTransition((Transition)content);
			sendAck(content);
		} else {
			System.err.println("Invalid event content");
		}
	}

	private void sendAck(Object content) throws IOException {
		Map<String, Object> datagramPayload = new HashMap<String, Object>();
		datagramPayload.put(Statics.TRAMSITION_ID, ((Transition)content).getTransitionId());
		this.out.write(new Datagram(Statics.ACK, datagramPayload).toString().getBytes());
		this.out.flush();
	}
}
