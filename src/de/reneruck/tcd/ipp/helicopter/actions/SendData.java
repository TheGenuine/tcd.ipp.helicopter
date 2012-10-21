package de.reneruck.tcd.ipp.helicopter.actions;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import de.reneruck.tcd.ipp.datamodel.Callback;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.TemporalTransitionsStore;
import de.reneruck.tcd.ipp.datamodel.Transition;
import de.reneruck.tcd.ipp.datamodel.TransitionExchangeBean;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;

public class SendData implements Action, Callback {

	private ObjectOutputStream out;
	private DataSender sender;
	private Map<Long, Transition> dataset = new HashMap<Long, Transition>();
	private TemporalTransitionsStore transitionsStore;
	private TransitionExchangeBean bean;

	public SendData(TransitionExchangeBean transitionExchangeBean, TemporalTransitionsStore transitionsStore ) {
		this.bean = transitionExchangeBean;
		this.transitionsStore = transitionsStore;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		System.out.println("Sending data so Server");
		if(this.out == null) {
			this.out = this.bean.getOut();
		}
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
		if(this.out == null) {
			this.out = this.bean.getOut();
		}
		try {
			this.bean.getFsm().handleEvent(new TransitionEvent(Statics.FINISH_RX_SERVER));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
