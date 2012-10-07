package de.reneruck.tcd.ipp.helicopter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import de.reneruck.tcd.datamodel.Statics;

public class DatabaseDiscoverer extends Thread {

	private List<InetAddress> dbServers;
	private DatagramSocket listeningSocket;
	private boolean running;

	public DatabaseDiscoverer(List<InetAddress> dbServers) {
		this.dbServers = dbServers;
	}

	private InetAddress getBroadcastAdress() {
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			byte[] address = localhost.getAddress();
			address[3] = -1;
			return InetAddress.getByAddress(address);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void run() {
		setupDatagramSocket();
		sendBroadcast();
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
			this.listeningSocket = new DatagramSocket(null);
			this.listeningSocket.setReuseAddress(true);
			this.listeningSocket.bind(new InetSocketAddress(Statics.DISCOVERY_PORT+1));
		} catch (SocketException e) {
			e.printStackTrace();
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
					if (/*!address.equals(InetAddress.getLocalHost()) && */!this.dbServers.contains(address)) {
						this.dbServers.add(address);
					}
				} catch (IOException e) {
					System.err.println("Failed to read socket " + e.getMessage());
				} finally {
					this.listeningSocket.close();
				}
			}
			System.err.println("Finished searching, found " + this.dbServers.size() + " Server");
		} else {
			System.err.println("No listening socket available");
		}

	}

	private void sendBroadcast() {
		InetAddress broadcastAdress = getBroadcastAdress();
		if (broadcastAdress != null) {
			try {
				System.out.println("Sending broadcast to " + broadcastAdress);
				DatagramSocket socket = new DatagramSocket();
				byte[] buf = Statics.SYN.getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAdress, Statics.DISCOVERY_PORT);

				for (int i = 0; i < 3; i++) {
					socket.send(packet);
					Thread.sleep(500);
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
}
