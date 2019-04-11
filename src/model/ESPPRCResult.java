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
	
	// Number of visited nodes during the path
	private int nbVisitedNodes;
	
	// Number of feasible routes found at the depot
	private int nbFeasibleRoutes;
	
	// Number of routes generated during the whole algorithm
	private int nbTotalRoutes;
		
	public ESPPRCResult(String route, double cost, long timeElapsed, int nbVisitedNodes, int nbFeasibleRoutes, int nbTotalRoutes) {
		this.route = route;
		this.cost = cost;
		this.timeElapsed = timeElapsed;
		this.nbVisitedNodes = nbVisitedNodes;
		this.nbFeasibleRoutes = nbFeasibleRoutes;
		this.nbTotalRoutes = nbTotalRoutes;
	}
	
	public ESPPRCResult(String route, double cost, long timeElapsed, int nbVisitedNodes) {
		this.route = route;
		this.cost = cost;
		this.timeElapsed = timeElapsed;
		this.nbVisitedNodes = nbVisitedNodes;
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

	public int getNbVisitedNodes() {
		return nbVisitedNodes;
	}

	public void setNbVisitedNodes(int nbVisitedNodes) {
		this.nbVisitedNodes = nbVisitedNodes;
	}

	public int getNbFeasibleRoutes() {
		return nbFeasibleRoutes;
	}

	public void setNbFeasibleRoutes(int nbFeasibleRoutes) {
		this.nbFeasibleRoutes = nbFeasibleRoutes;
	}

	public int getNbTotalRoutes() {
		return nbTotalRoutes;
	}

	public void setNbTotalRoutes(int nbTotalRoutes) {
		this.nbTotalRoutes = nbTotalRoutes;
	}
}
