package de.reneruck.tcd.ipp.helicopter.actions;

import java.io.OutputStream;

import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;

public class SendControlSignal implements Action {

	private String signal;
	private OutputStream out;

	public SendControlSignal(OutputStream out, String signal) {
		this.out = out;
		this.signal = signal;
	}

	@Override
	public void execute(TransitionEvent event) throws Exception {
		if(this.out != null) {
			try {
				this.out.write(new Datagram(this.signal).toString().getBytes());
				this.out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} else {
			System.err.println("OutputStream was null, could not send signal " + this.signal);
		}
	}

}
