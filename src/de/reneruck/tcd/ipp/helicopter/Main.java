package de.reneruck.tcd.ipp.helicopter;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Helicopter helicopter;
		if(args.length > 0)
		{
			helicopter = Helicopter.createNewHelicopter(args[0]);
		} else {
			helicopter = new Helicopter();
		}
		helicopter.start();
	}
}
