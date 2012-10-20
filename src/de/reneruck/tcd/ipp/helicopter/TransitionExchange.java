package de.reneruck.tcd.ipp.helicopter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeoutException;

import de.reneruck.tcd.ipp.datamodel.Callback;
import de.reneruck.tcd.ipp.datamodel.Datagram;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.TemporalTransitionsStore;
import de.reneruck.tcd.ipp.fsm.Action;
import de.reneruck.tcd.ipp.fsm.FiniteStateMachine;
import de.reneruck.tcd.ipp.fsm.SimpleState;
import de.reneruck.tcd.ipp.fsm.Transition;
import de.reneruck.tcd.ipp.fsm.TransitionEvent;
import de.reneruck.tcd.ipp.helicopter.actions.ReceiveData;
import de.reneruck.tcd.ipp.helicopter.actions.SendControlSignal;
import de.reneruck.tcd.ipp.helicopter.actions.SendData;
import de.reneruck.tcd.ipp.helicopter.actions.ShutdownConnection;

public class TransitionExchange implements Callback{

	private static int MAX_TRIES = 10;

	private TemporalTransitionsStore transitionStore;
	private Socket socket;
	private boolean listen = true;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private List<InetAddress> dbServers;

	private FiniteStateMachine fsm;

	public TransitionExchange(TemporalTransitionsStore transitionStore, List<InetAddress> dbServers) {
		this.transitionStore = transitionStore;
		this.dbServers = dbServers;
		setupFSM();
	}

	private void setupFSM() {
		this.fsm = new FiniteStateMachine();
		
		SimpleState state_start = new SimpleState("start");
		SimpleState state_syn = new SimpleState("syn");
		SimpleState state_acked = new SimpleState("acked");
		SimpleState state_waitRxModeAck = new SimpleState("waitRxModeAck");
		SimpleState state_ReceiveData = new SimpleState("ReceiveData");
		SimpleState state_SendData = new SimpleState("SendData");
		SimpleState state_finished = new SimpleState("finished");

		Action sendSYN = new SendControlSignal(this.out, Statics.SYN);
		Action sendSYNACK = new SendControlSignal(this.out, Statics.SYNACK);
		Action sendRxServer = new SendControlSignal(this.out, Statics.RX_SERVER);
		Action sendRxHeli = new SendControlSignal(this.out, Statics.RX_HELI);
		Action receiveData = new ReceiveData(this.out, this.transitionStore);
		Action sendData = new SendData(this.out, this.transitionStore);
		Action sendFIN = new SendControlSignal(this.out, Statics.FIN);
		Action sendFIN_ACK = new SendControlSignal(this.out, Statics.FINACK);
		Action shutdownConnection = new ShutdownConnection(this);

		Transition txSyn = new Transition(new TransitionEvent("sendSyn"), state_syn, sendSYN);
		Transition rxAck = new Transition(new TransitionEvent(Statics.ACK), state_acked, sendSYNACK);
		
		Transition txMode_send = new Transition(new TransitionEvent("sendMode_send"), state_waitRxModeAck, sendRxServer);
		Transition txMode_receive = new Transition(new TransitionEvent("sendMode_receive"), state_waitRxModeAck, sendRxHeli);
		
		Transition rxMode_Heli_ack = new Transition(new TransitionEvent(Statics.RX_HELI_ACK), state_ReceiveData, receiveData);
		Transition rxMode_Server_ack = new Transition(new TransitionEvent(Statics.RX_SERVER_ACK), state_SendData, sendData);

		Transition rxDataAck = new Transition(new TransitionEvent(Statics.ACK), state_SendData, sendData);
		Transition rxData = new Transition(new TransitionEvent(Statics.DATA), state_ReceiveData, receiveData);

		Transition finishedSending = new Transition(new TransitionEvent(Statics.FINISH_RX_SERVER), state_finished, sendFIN);
		Transition rxFin = new Transition(new TransitionEvent(Statics.FIN), state_finished, sendFIN_ACK);
		Transition rxFinACK = new Transition(new TransitionEvent(Statics.FINACK), null, shutdownConnection);
		Transition shutdown = new Transition(new TransitionEvent(Statics.SHUTDOWN), null, shutdownConnection);

		state_start.addTranstion(txSyn);
		state_syn.addTranstion(rxAck);
		state_syn.addTranstion(txSyn);
		state_acked.addTranstion(txMode_send);
		state_acked.addTranstion(txMode_receive);
		
		state_waitRxModeAck.addTranstion(rxMode_Heli_ack);
		state_waitRxModeAck.addTranstion(rxMode_Server_ack);
		
		state_SendData.addTranstion(rxDataAck);
		state_SendData.addTranstion(finishedSending);
		
		state_ReceiveData.addTranstion(rxData);
		state_ReceiveData.addTranstion(rxFin);
		
		state_finished.addTranstion(rxFinACK);
		state_finished.addTranstion(shutdown);

		this.fsm.setStartState(state_start);

	}

	
	public void startExchange() {
		try {
			waitForServer();
			establishConnection();
			send(Statics.SYN);
			waitForAnswer();
			shutdown();
		} catch (TimeoutException e) {
			System.out.println("No Server found");
		}

	}

	private void waitForServer() throws TimeoutException {
		System.out.print("Looking for available DB servers ");
		int tries = 0;
		while (this.dbServers.isEmpty()) {
			if (tries > MAX_TRIES)
				break;
			tries++;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.print(". ");
		}
		if (tries < MAX_TRIES) {
			System.out.println(" ");
			System.out.println("Found a server ");
		} else {
			throw new TimeoutException("No DB server found in reasonable time");
		}
	}

	private void establishConnection() {
		try {
			System.out.println("Establishing connection to " + this.dbServers.get(0));
			this.socket = new Socket(this.dbServers.get(0), Statics.DB_SERVER_PORT);
			this.in = new ObjectInputStream(this.socket.getInputStream());
			this.out = new ObjectOutputStream(this.socket.getOutputStream());
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void waitForAnswer() {
		try {
			do {
				Thread.sleep(500);
				handle(this.in.readObject());
			} while (this.listen);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			shutdown();
		}
	}

	private void handle(Object input) {
		if (input instanceof Datagram) {
			TransitionEvent event = getTransitionEventFromDatagram((Datagram) input);
			try {
				this.fsm.handleEvent(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Unknown type " + input.getClass() + " discarding package");
		}
	}

	private TransitionEvent getTransitionEventFromDatagram(Datagram input) {
		TransitionEvent event = new TransitionEvent(input.getType());
		for (String key : input.getKeys()) {
			event.addParameter(key, input.getPayload(key));
		}
		return event;
	}
	
	private void shutdown() {
		this.listen = false;
		try {
			this.out.close();
			this.in.close();
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void send(String message) {
		try {
			System.out.println("Sending> " + message);
			this.out.writeObject(message);
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleCallback() {
		shutdown();
	}
}
