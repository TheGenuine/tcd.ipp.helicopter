package de.reneruck.tcd.ipp.helicopter;

import java.net.ConnectException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.reneruck.tcd.ipp.datamodel.Airport;
import de.reneruck.tcd.ipp.datamodel.Booking;
import de.reneruck.tcd.ipp.datamodel.Statics;
import de.reneruck.tcd.ipp.datamodel.exceptions.DatabaseException;
import de.reneruck.tcd.ipp.datamodel.transition.NewBookingTransition;
import de.reneruck.tcd.ipp.datamodel.transition.TemporalTransitionsStore;

/**
 * The {@link Helicopter} class is the base class and backbone of the whole
 * helicopter.<br>
 * As soon as it gets started it starts to travel between the city and the camp
 * and tries to deliver transactions.
 * 
 * @author Rene
 * 
 */
public class Helicopter extends Thread {

	private static final int FLIGHT_TIME_IN_MS = 10000;
	
	private List<InetAddress> dbServers = new ArrayList<InetAddress>();
	private boolean getsLost = false;
	private boolean inFlight = false;
	private boolean radarContacted = false;
	private int flightTimeElapsedInMs = 0;
	private Airport target = Airport.city;
	private TemporalTransitionsStore transitionStore;
	private DatabaseDiscoverer dbDiscoverer;
	
	/**
	 * Creates a new Helicopter and initializes it.
	 */
	public Helicopter() {
		try {
			this.transitionStore = new TemporalTransitionsStore("");
			//fillTransitionStore();
		} catch (ConnectException e) {
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
		startDbDiscoverer();
	}
	

	/**
	 * For testing purposes only!!
	 */
	private void fillTransitionStore() {
		this.transitionStore.addTransition(new NewBookingTransition(new Booking("Harry Horse", new Date(1353520800000L), Airport.city))); 
		this.transitionStore.addTransition(new NewBookingTransition(new Booking("Harry Horse", new Date(1353920800000L), Airport.camp))); 
		this.transitionStore.addTransition(new NewBookingTransition(new Booking("Harry Horse", new Date(1353620800000L), Airport.city))); 
	}


	@Override
	public void run() {
		if(!this.inFlight) // initial 
		{
			calcGetsLost();
			exchangeTransitions(Statics.RX_HELI);
			stopDbDiscoverer();
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
			
			// when on half way to the target
			if(this.flightTimeElapsedInMs >= FLIGHT_TIME_IN_MS/2 && !this.radarContacted)
			{
				// start discovering for connection partners
				if(this.dbDiscoverer == null)
				{
					startDbDiscoverer();
				}
			}
			
			// when arrived
			if(this.flightTimeElapsedInMs >= FLIGHT_TIME_IN_MS)
			{
				land();
				exchangeTransitions(Statics.RX_SERVER);
				waitForTakeOff();
				exchangeTransitions(Statics.RX_HELI);
				depart();				
			}
		}
	}

	private void startDbDiscoverer() {
		this.dbServers.clear();
		this.dbDiscoverer = new DatabaseDiscoverer(this.dbServers, this.target);
		this.dbDiscoverer.setRunning(true);
		this.dbDiscoverer.start();
	}
	
	private void stopDbDiscoverer() {
		this.dbDiscoverer.setRunning(false);
		this.dbDiscoverer = null;
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
			Thread.sleep(2000);
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
		stopDbDiscoverer();
	}

	private void exchangeTransitions(String mode) {
		System.out.println("exchanging tranisions");
		
		createTranistionExchange(mode, this.dbServers);
	}


	private void createTranistionExchange(String mode, List<InetAddress> servers) {
		TransitionExchange transitionExchange = new TransitionExchange(this.transitionStore, servers, mode, this.target);
		transitionExchange.startExchange();
	}

	private void calcGetsLost()	{
		double rand = Math.random() * 10;
		if(rand == 0.0 | rand == 10.0)
		{
			this.getsLost = true;
		}
	}
}