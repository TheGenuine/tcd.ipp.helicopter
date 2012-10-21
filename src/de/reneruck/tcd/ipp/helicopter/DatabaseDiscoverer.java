package de.reneruck.tcd.ipp.helicopter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.List;

import de.reneruck.tcd.ipp.datamodel.Airport;
import de.reneruck.tcd.ipp.datamodel.Statics;

/**
 * The {@link DatabaseDiscoverer} constitutes listening part of discovery service.<br>
 * It is joining the specified multicast group and listens for all incoming packages. 
 * Every incoming package from a new sender will be added to list of available servers.
 * This list is also available to the caller of this thread, in that way the outside 
 * world gets the discovered servers.
 * 
 * @author Rene
 *
 */
public class DatabaseDiscoverer extends Thread {

	private List<InetAddress> dbServers;
	private MulticastSocket listeningSocket;
	private boolean running;
	private Airport targetAirport;

	public DatabaseDiscoverer(List<InetAddress> dbServers, Airport target) {
		this.dbServers = dbServers;
		this.targetAirport = target;
	}

	@Override
	public void run() {
		System.err.println("DatabaseDiscoverer startup");
		setupDatagramSocket();
		listenForDatabaseServers();
		shutdown();
		System.err.println("DatabaseDiscoverer shutdown");
	}

	private void shutdown() {
		if(this.listeningSocket != null)
		{
			this.listeningSocket.disconnect();
			this.listeningSocket.close();
		} 
	}

	private void setupDatagramSocket() {
		try {
			InetAddress group = InetAddress.getByName("230.0.0.1");
			this.listeningSocket = new MulticastSocket(null);
			this.listeningSocket.joinGroup(group);
			this.listeningSocket.setReuseAddress(true);
			this.listeningSocket.bind(getsocketAdress());
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private InetSocketAddress getsocketAdress() {
		switch (this.targetAirport) {
		case camp:
			return new InetSocketAddress(Statics.CLIENT_DISCOVERY_PORT);
		case city:
			return new InetSocketAddress(Statics.DISCOVERY_PORT);
		default:
			return new InetSocketAddress(Statics.DISCOVERY_PORT);
		}
	}

	private void listenForDatabaseServers() {
		if (this.listeningSocket != null) {
			byte[] buffer = new byte[100];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			while (this.running && this.dbServers.isEmpty()) {
				try {
					this.listeningSocket.receive(packet);
					InetAddress address = packet.getAddress();
					System.out.println("Discovered server at " + address);
					if (!this.dbServers.contains(address)) {
						this.dbServers.add(address);
					}
					Thread.sleep(500);
				} catch (IOException e) {
					System.err.println("Failed to read socket " + e.getMessage());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					this.listeningSocket.close();
				}
			}
			System.err.println("Finished searching, found " + this.dbServers.size() + " Server");
		} else {
			System.err.println("No listening socket available");
		}

	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
}
