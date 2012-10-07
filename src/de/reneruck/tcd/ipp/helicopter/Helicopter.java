package de.reneruck.tcd.ipp.helicopter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import de.reneruck.tcd.datamodel.Airport;
import de.reneruck.tcd.datamodel.TransportContainer;
import de.reneruck.tcd.ipp.serializer.Serializer;

public class Helicopter extends Thread {

	private static final byte[] RADAR_CONTACT = new byte[]{61,63,63,67};
	private static final int RADAR_PORT = 8765;
	private static final byte[] ACC_CONTENT = new byte[]{97,99,99};
	private static final String CAMP_RADAR = "localhost";
	private static final String CITY_RADAR = "localhost";
	private static final int FLIGHT_TIME_IN_MS = 20000;
	private static final int MAX_RETRIES = 3; 
	
	private boolean getsLost = false;
	private boolean inFlight = false;
	private boolean radarContacted = false;
	private int flightTimeElapsedInMs = 0;
	private Airport target;
	private TransportContainer container;

	public Helicopter() {
		this.container = new TransportContainer();
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
			
//			if(this.flightTimeElapsedInMs >= FLIGHT_TIME_IN_MS/2 && !this.radarContacted)
//			{
//				handoverToRadar();
//			}
			
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
			System.out.println("waiting for clearance to take off");
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
		System.out.println("exchanging tranisions");
		
		TransitionExchange transitionExchange = new TransitionExchange(this.container);
		transitionExchange.startExchange();
		
		System.out.println("[DONE]");
	}

	private void handoverToRadar() {
		System.out.println("handing over to RADAR");
		if(contactRadar())
		{
			System.out.println("Handover succesfully");
			System.out.println("Shuting down");
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
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();
			outputStream.write(RADAR_CONTACT);
			outputStream.flush();
			waitForAcc(inputStream);
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
	
	private void waitForAcc(InputStream inputStream) throws IOException, InterruptedException, TimeoutException {
		boolean acc = false;
		int retries = 0;
		byte[] buffer = new byte[1000];
		do {
			System.out.println("Waiting for response");
			inputStream.read(buffer);
			if(verifyResponse(buffer)){
				acc = true;
				System.out.println("received proper acc");
			}
			this.sleep(1000);
			retries ++;
		} while (acc | (retries > MAX_RETRIES));
		if(!acc && retries > MAX_RETRIES) {
			System.err.println("Waiting for ACC timed out");
			throw new TimeoutException();
		}
	}

	private boolean verifyResponse(byte[] bytes) {
		byte[] copyOfRange = Arrays.copyOfRange(bytes, 0, 3);
		if(ACC_CONTENT.equals(copyOfRange))
		{
			return true;
		}
		return false;
	}

	private byte[] serialize() {
		byte[] serialized = null;
		return serialized;
	}
	
	private void calcGetsLost()	{
		double rand = Math.random() * 10;
		if(rand == 0.0 | rand == 10.0)
		{
			this.getsLost = true;
		}
	}
}