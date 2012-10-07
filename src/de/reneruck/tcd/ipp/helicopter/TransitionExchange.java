package de.reneruck.tcd.ipp.helicopter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import de.reneruck.tcd.datamodel.Statics;
import de.reneruck.tcd.datamodel.Transition;
import de.reneruck.tcd.datamodel.TransportContainer;

public class TransitionExchange {

	private static int MAX_TRIES = 3;
	private List<InetAddress> dbServers = new ArrayList<InetAddress>();

	private TransportContainer container;
	private Socket socket;
	private boolean listen = true;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private DatabaseDiscoverer dbDiscoverer;

	public TransitionExchange(TransportContainer container) {
		this.container = container;
		this.dbDiscoverer = new DatabaseDiscoverer(this.dbServers);
		this.dbDiscoverer.setRunning(true);
		this.dbDiscoverer.start();
		try {
			this.dbServers.add(InetAddress.getByName("localhost"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void startExchange() {
		try {
			waitForServer();
			establishConnection();
			send(Statics.SYN);
			waitForAnswer();
		} catch (TimeoutException e) {

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
		}
	}

	private void handle(Object input) {
		if (input instanceof String) {
			String message = (String) input;
			System.out.println("Received: " + message);

			if (message.equals(Statics.ACK)) {
				send(Statics.ACK + Statics.ACK);
				transferTransitions();
				send(Statics.FIN);
			} else if (message.equals(Statics.FINACK)) {
				System.out.println("Transition successful");
				shutdown();
			} else {
				System.out.println("Heli> received " + message);
			}
		} else {
			System.err.println("Unknown type " + input.getClass());
		}
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
		this.dbDiscoverer.shutdown();
		this.dbDiscoverer = null;
	}

	private void transferTransitions() {
		if (this.container != null) {

			List<Transition> transitions = this.container.getTransitions();
			for (Transition transition : transitions) {
				send(transition.toString());
			}
		} else {
			System.err.println("No valid container loaded");
		}
	}

	private void send(String message) {
		try {
			this.out.writeObject(message);
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
