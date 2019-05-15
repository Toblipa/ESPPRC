package model;

import java.util.ArrayList;

public class VRPTWResult {

	/**
	 * The list of the routes used in the solution set of the relaxed problem
	 */
	private ArrayList<Label> relaxedSolution;
	
	/**
	 * The list of the routes used in the solution set of the MIP
	 */
	private ArrayList<Label> integerSolution;

	/**
	 * Last relaxed objective value
	 */
	private double lowerBound;
	
	/**
	 * Integer problem objective value
	 */
	private double upperBound;
	

	/**
	 * Time elpased during resolution
	 */
	private long timeElapsed;
	
	/**
	 * Cplex resolution gap of the MIP
	 */
	private double mipGap;
	
	/**
	 * Gap between the upper and lower bounds as a percentage of the lower bound
	 */
	private double gap;
	
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
		this.setRelaxedSolution(routes);
		this.setLowerBound(objective);
		this.setxSum(xSum);
		this.setInitialRoutes(initialRoutes);
		this.setGeneratedRoutes(generatedRoutes);
		this.setIterations(iterations);
		this.setReducedCost(reducedCost);
	}
	
	// ============== GETTERS & SETTERS ============
	
	public ArrayList<Label> getRelaxedSolution() { 
		return relaxedSolution;
	}

	public void setRelaxedSolution(ArrayList<Label> routes) {
		this.relaxedSolution = routes;
	}

	public double getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(double objective) {
		this.lowerBound = objective;
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

	public double getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(double upperBound) {
		this.upperBound = upperBound;
	}

	public double getMipGap() {
		return mipGap;
	}

	public void setMipGap(double mipGap) {
		this.mipGap = mipGap;
	}

	public double getGap() {
		return gap;
	}

	public void setGap(double gap) {
		this.gap = gap;
	}

	public ArrayList<Label> getIntegerSolution() {
		return integerSolution;
	}

	public void setIntegerSolution(ArrayList<Label> integerSolution) {
		this.integerSolution = integerSolution;
	}
}
