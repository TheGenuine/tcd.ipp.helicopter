package de.reneruck.tcd.ipp.helicopter.actions;

import java.io.ObjectOutputStream;

import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.datamodel.transition.TransitionExchangeBean;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;

/**
 * Helper class to send control signals required for the correct protocol flow.
 * 
 * @author Rene
 * 
 */
public class SendControlSignal implements Action {

	private String signal;
	private ObjectOutputStream out;
	private TransitionExchangeBean bean;

	public SendControlSignal(TransitionExchangeBean transitionExchangeBean, String signal) {
		this.bean = transitionExchangeBean;
		this.signal = signal;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		if (this.out == null) {
			this.out = this.bean.getOut();
		}
		try {
			System.out.println("Sending " + this.signal);
			this.out.writeObject(new Datagram(this.signal));
			this.out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
