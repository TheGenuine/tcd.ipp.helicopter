package de.reneruck.tcd.ipp.helicopter.actions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.reneruck.tcd.ipp.datamodel.Callback;
import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.TemporalTransitionsStore;
import de.reneruck.tcd.ipp.datamodel.Transition;
import de.reneruck.tcd.ipp.datamodel.TransitionState;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;

public class SendData implements Action, Callback {

	private OutputStream out;
	private DataSender sender;
	private Map<Long, Transition> dataset = new HashMap<Long, Transition>();
	private TemporalTransitionsStore transitionsStore;

	public SendData(OutputStream out, TemporalTransitionsStore transitionsStore ) {
		this.out = out;
		this.transitionsStore = transitionsStore;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		if(this.sender == null) {
			initializeDataSender();
		}
		
		if(Statics.ACK.equals(event.getIdentifier())) {
			Object parameter = event.getParameter(Statics.TRAMSITION_ID);
			if(parameter != null && parameter instanceof Long) {
				this.transitionsStore.removeTransitionById((Long)parameter);
			}
		}
	}

	private void initializeDataSender() {
		this.sender = new DataSender(this.out, this.transitionsStore, this);
		this.sender.start();
	}

	@Override
	public void handleCallback() {
		this.sender = null;
		try {
			this.out.write(new Datagram(Statics.FIN).toString().getBytes());
			this.out.flush();
			try {
				// FIXME: let the fsm handle Statics.FINISH_RX_HELI
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
