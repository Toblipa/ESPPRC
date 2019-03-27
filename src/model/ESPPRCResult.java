package model;

/*
 * Class to stock the result of the each algorithm
 */
public class ESPPRCResult{
	
	// String of the route
	private String route;
	
	// Cost of doing the route
	private double cost;
	
	// Time to find the solution in milliseconds
	private long timeElapsed;
	
	public ESPPRCResult(String route, double cost, long timeElapsed) {
		this.route = route;
		this.cost = cost;
		this.timeElapsed = timeElapsed;
	}
	
	public String getRoute() {
		return route;
	}
	public void setRoute(String route) {
		this.route = route;
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	public long getTimeElapsed() {
		return timeElapsed;
	}
	public void setTimeElapsed(long timeElapsed) {
		this.timeElapsed = timeElapsed;
	}
}
