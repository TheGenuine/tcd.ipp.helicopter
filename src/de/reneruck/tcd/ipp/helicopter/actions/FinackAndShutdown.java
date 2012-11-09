package de.reneruck.tcd.ipp.helicopter.actions;

import java.io.ObjectOutputStream;

import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.transition.TransitionExchangeBean;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;
import de.reneruck.tcd.ipp.helicopter.TransitionExchange;

/**
 * On successful data exchange this class acknowledges the request to shutdown
 * the connection and finally calls for a shutdown of the
 * {@link TransitionExchange}.
 * 
 * @author Rene
 * 
 */
public class FinackAndShutdown implements Action {

	private TransitionExchangeBean bean;
	private TransitionExchange connection;

	public FinackAndShutdown(TransitionExchangeBean transitionExchangeBean, TransitionExchange connection) {
		this.bean = transitionExchangeBean;
		this.connection = connection;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		ObjectOutputStream out = this.bean.getOut();
		out.writeObject(new Datagram(Statics.FINACK));
		out.flush();
		
		// call for TransitionExchange shutdown
		this.connection.shutdown();
	}

}
