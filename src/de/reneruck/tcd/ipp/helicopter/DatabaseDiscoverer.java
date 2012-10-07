package de.reneruck.tcd.ipp.helicopter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
	}

	private void setupDatagramSocket() {
		try {
			this.listeningSocket = new DatagramSocket(Statics.DB_SERVER_PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void listenForDatabaseServers() {
		if (this.listeningSocket != null) {
			byte[] buffer = new byte[2048];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			while (this.running && this.dbServers.isEmpty()) {
				try {
					this.listeningSocket.receive(packet);
					InetAddress address = packet.getAddress();
					if (!address.equals(InetAddress.getLocalHost()) && !this.dbServers.contains(address)) {
						this.dbServers.add(address);
					}
				} catch (IOException e) {
					System.err.println("Failed to read socket " + e.getMessage());
				}
			}
		} else {
			System.err.println("No listening socket available");
		}

	}

	private void sendBroadcast() {
		InetAddress broadcastAdress = getBroadcastAdress();
		if (broadcastAdress != null) {
			try {
				DatagramSocket socket = new DatagramSocket();
				byte[] buf = new byte[] { 83, 89, 78 };
				DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAdress, Statics.DB_SERVER_PORT);

				for (int i = 0; i < 3; i++) {
					socket.send(packet);
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void shutdown() {
		this.running = false;
		this.listeningSocket.close();
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
}
