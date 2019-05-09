package model;

import java.util.ArrayList;

public class VRPTWResult {

	/**
	 * The list of the routes used in the solution set
	 */
	private ArrayList<Label> routes;

	/**
	 * Last objective value
	 */
	private double objective;
	

	/**
	 * Time elpased during resolution
	 */
	private long timeElapsed;
	
	/**
	 * Sum of the x variables
	 */
	private double xSum;
	
	/**
	 * Number of routes at the begining of the algorithm
	 */
	private int initialRoutes;
	
	/**
	 * Number of generated routes during the algorithm
	 */
	private int generatedRoutes;
	
	/**
	 * Number of iterations done until the end of the algorithm 
	 */
	private int iterations;
	
	/**
	 * Last reduced cost given by the subproblem
	 */
	private double reducedCost;
	
	/**
	 * 
	 * @param routes
	 * @param objective
	 * @param xSum
	 * @param iterations
	 * @param reducedCost
	 */
	public VRPTWResult(ArrayList<Label> routes, double objective, double xSum, int initialRoutes, int generatedRoutes, int iterations, double reducedCost) {
		this.setRoutes(routes);
		this.setObjective(objective);
		this.setxSum(xSum);
		this.setInitialRoutes(initialRoutes);
		this.setGeneratedRoutes(generatedRoutes);
		this.setIterations(iterations);
		this.setReducedCost(reducedCost);
	}
	
	// ============== GETTERS & SETTERS ============
	
	public ArrayList<Label> getRoutes() { 
		return routes;
	}

	public void setRoutes(ArrayList<Label> routes) {
		this.routes = routes;
	}

	public double getObjective() {
		return objective;
	}

	public void setObjective(double objective) {
		this.objective = objective;
	}

	public long getTimeElapsed() {
		return timeElapsed;
	}

	public void setTimeElapsed(long timeElapsed) {
		this.timeElapsed = timeElapsed;
	}

	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public double getReducedCost() {
		return reducedCost;
	}

	public void setReducedCost(double reducedCost) {
		this.reducedCost = reducedCost;
	}

	public double getxSum() {
		return xSum;
	}

	public void setxSum(double xSum) {
		this.xSum = xSum;
	}

	public int getInitialRoutes() {
		return initialRoutes;
	}

	public void setInitialRoutes(int initialRoutes) {
		this.initialRoutes = initialRoutes;
	}

	public int getGeneratedRoutes() {
		return generatedRoutes;
	}

	public void setGeneratedRoutes(int generatedRoutes) {
		this.generatedRoutes = generatedRoutes;
	}
}
