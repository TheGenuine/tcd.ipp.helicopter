package de.reneruck.tcd.ipp.helicopter;

/**
 * This class does not much, it only creates a new {@link Helicopter} and starts
 * it.
 * 
 * @author Rene
 * 
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Helicopter helicopter = new Helicopter();
		helicopter.start();
	}
}
