package de.reneruck.tcd.ipp.helicopter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

import de.reneruck.tcd.datamodel.Airport;
import de.reneruck.tcd.datamodel.TransportContainer;

public class Helicopter extends Thread {

	private static final int RADAR_PORT = 8765;
	private static final byte[] ACC_CONTENT = new byte[]{97,97,99};
	private static final String CAMP_RADAR = null;
	private static final String CITY_RADAR = null;
	private static final int FLIGHT_TIME_IN_MS = 90000;
	private static final int MAX_RETRIES = 3; 
	
	private boolean getsLost = false;
	private int flightTimeElapsedInMs = 0;
	private Airport target;
	private boolean inFlight = false;
	private boolean radarContacted = false;
	private TransportContainer container;
	
	public static Helicopter createNewHelicopter(String args) {
		return handleArgs(args);
	}
	
	private static Helicopter handleArgs(String args) {
		Gson gson = new Gson();
		return gson.fromJson(args, Helicopter.class);
	}

	@Override
	public void run() {
		if(!this.inFlight)
		{
			calcGetsLost();
			exchangeTransitions();
			waitForTakeOff();
			depart();
		} else {
			getLost();
		}
		
		while(this.inFlight)
		{
			long timeMillis = System.currentTimeMillis();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.flightTimeElapsedInMs += System.currentTimeMillis() - timeMillis;
			printElapsedTime();
			
			if(this.flightTimeElapsedInMs >= FLIGHT_TIME_IN_MS/2 && !this.radarContacted)
			{
				handoverToRadar();
			}
			
			if(this.flightTimeElapsedInMs >= FLIGHT_TIME_IN_MS)
			{
				land();
				exchangeTransitions();
				waitForTakeOff();
				depart();				
			}
		}
	}

	private void getLost() {
		if(this.getsLost)
		{
			System.err.println("Helicopter got lost, all passengers died");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.exit(1);
		}
	}

	private void waitForTakeOff() {
		try {
			System.out.println("waiting for clearance for take off");
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void printElapsedTime() {
		int seconds = (int) (this.flightTimeElapsedInMs / 1000) % 60 ;
		int minutes = (int) ((this.flightTimeElapsedInMs / (1000*60)) % 60);	
		String secoundsString = seconds < 10 ? "0" + seconds : String.valueOf(seconds);
		String durationText = minutes + ":" + secoundsString;
		System.out.println("Fying since " + durationText);
	}

	private void depart() {
		this.inFlight = true;
		this.flightTimeElapsedInMs = 0;
		this.radarContacted = false;
		setNextTarget();
		calcGetsLost();
		System.out.println("ready for take off");
	}

	private void setNextTarget() {
		if(Airport.camp.equals(this.target))
		{
			this.target = Airport.city;
		} else {
			this.target = Airport.camp;
		}
	}

	private void land() {
		System.out.println("landing");
		this.inFlight = false;
	}

	private void exchangeTransitions() {
		System.out.print("exchanging tranisions");
		// TODO Auto-generated method stub
		System.out.println(" [DONE]");
	}

	private void handoverToRadar() {
		System.out.println("handing over to RADAR");
		if(contactRadar())
		{
			this.radarContacted = true;
			System.exit(1);
		}
	}
	
	private boolean contactRadar() {
		try {
			
			Socket socket = null;
			if(Airport.camp.equals(this.target)){
				socket = new Socket(InetAddress.getByName(CAMP_RADAR), RADAR_PORT);
			} else if(Airport.city.equals(this.target)){
				socket = new Socket(InetAddress.getByName(CITY_RADAR), RADAR_PORT);
			}
			SocketChannel channel = socket.getChannel();
			channel.write(ByteBuffer.wrap(toString().getBytes()));
			waitForAcc(channel);
			return true;
			
		} catch (UnknownHostException e) {
			System.err.println("Cannot reach RADAR service " + e.getMessage());
			return false;
		} catch (IOException e) {
			System.err.println("Cannot reach RADAR service " + e.getMessage());
			return false;
		} catch (InterruptedException e) {
			System.err.println("Cannot reach RADAR service " + e.getMessage());
			return false;
		} catch (TimeoutException e) {
			System.err.println("Cannot reach RADAR service");
			return false;
		}
	}
	
	private void waitForAcc(SocketChannel channel) throws IOException, InterruptedException, TimeoutException {
		boolean acc = false;
		int retries = 0;
		ByteBuffer buffer = ByteBuffer.allocate(1000);
		do {
			int readBytes = channel.read(buffer);
			
			if(readBytes > 0)
			{
				if(verifyResponse(buffer)){
					acc = true;
				}
			}
			this.sleep(1000);
			retries ++;
		} while (acc | retries > MAX_RETRIES);
		if(!acc && retries > MAX_RETRIES) {
			throw new TimeoutException();
		}
	}

	private boolean verifyResponse(ByteBuffer buffer) {
		if(ACC_CONTENT.equals(buffer.array()))
		{
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		String json = gson.toJson(this);
		return json;
	}
	
	private void calcGetsLost()	{
		double rand = Math.random() * 10;
		if(rand == 0.0 | rand == 10.0)
		{
			this.getsLost = true;
		}
	}
}