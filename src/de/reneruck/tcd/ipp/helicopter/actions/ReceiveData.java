package de.reneruck.tcd.ipp.helicopter.actions;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.TemporalTransitionsStore;
import de.reneruck.tcd.ipp.datamodel.Transition;
import de.reneruck.tcd.ipp.datamodel.TransitionExchangeBean;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;

public class ReceiveData implements Action {

	private ObjectOutputStream out;
	private TemporalTransitionsStore transitionStorage;
	private TransitionExchangeBean bean;

	public ReceiveData(TransitionExchangeBean transitionExchangeBean, TemporalTransitionsStore transitionsQueue) {
		this.bean = transitionExchangeBean;
		this.transitionStorage = transitionsQueue;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		Object content = event.getParameter(Statics.CONTENT_TRANSITION);
		if(content != null && content instanceof Transition) {
			this.transitionStorage.addTransition((Transition)content);
			System.out.println("Received " + ((Transition)content).getTransitionId());
			sendAck(content);
		} else {
			System.err.println("Invalid event content");
		}
	}

	private void sendAck(Object content) throws IOException {
		if(this.out == null) {
			this.out = this.bean.getOut();
		}
		Map<String, Object> datagramPayload = new HashMap<String, Object>();
		datagramPayload.put(Statics.TRAMSITION_ID, ((Transition)content).getTransitionId());
		this.out.writeObject(new Datagram(Statics.ACK, datagramPayload));
		this.out.flush();
	}
}
