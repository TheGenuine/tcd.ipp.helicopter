package de.reneruck.tcd.ipp.helicopter.actions;

import de.reneruck.tcd.ipp.datamodel.Callback;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;
import de.reneruck.tcd.ipp.helicopter.TransitionExchange;

/**
 * Calls for a shutdown of the {@link TransitionExchange}
 * 
 * @author Rene
 * 
 */
public class ShutdownConnection implements Action {

	private Callback callback;

	public ShutdownConnection(Callback callback) {
		this.callback = callback;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		this.callback.handleCallback();
	}

}
