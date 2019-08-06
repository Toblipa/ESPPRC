package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Representation of a routing instance
 * @author pablo
 *
 */
public class EspprcInstance {
	
	/** The list of nodes beginning with the origin */
	private Customer[] nodes;
	
	/** An array with the list of all the successors of the node
	 * in the respective position */
	private ArrayList<Customer>[] successors;
	
	/** The cost to go from node i to node j */
	private double[][] cost;
	
	/** The time to go from node i to node j */
	private double[][] distance;
	
	/** The quantity of vehicles */
	private int nbVehicles;
	
	/** The capacity of the uniform float of vehicles */
	private double capacity;
	
	/** If we duplicate de origin node */
	private boolean duplicateOrigin;
	
	/** To identify the instance */
	private String name;

	/** Default constructor */
	public EspprcInstance() {
	}
	
	/** Constructor by copy
	 * @param instance
	 */
	public EspprcInstance(EspprcInstance instance) {
		this.capacity = instance.getCapacity();
		this.cost = instance.getCostMatrix().clone();
		this.distance = instance.getDistanceMatrix().clone();
		this.duplicateOrigin = instance.isDuplicateOrigin();
		this.name = instance.getName();
		this.nbVehicles = instance.getVehicles();
		this.nodes = instance.getNodes().clone();
		this.successors = instance.getSuccessors().clone();
	}

	/**
	 * Count all the possible sucessor nodes for each node
	 * @return Number of edges in the current graph
	 */
	public int getNbEdges() {
		int result = 0;
		for(ArrayList<Customer> succesorList :  successors) {
			result += succesorList.size();
		}
		return result;
	}
	
	/**
	 * Count all the successor nodes from wich the current node the cost is negative
	 * @return Number of edges with negative cost
	 */
	public double getNbNegativeEdges() {
		int result = 0;
		for(int id = 0; id < successors.length ; id++) {
			for(Customer successorNode : successors[id]) {
				if(cost[id][successorNode.getId()] < 0) {
					result++;
				}
			}
		}
		return result;
	}
	
	/**
	 * Calculate the density for a directed simple graph with the following formula
	 * |E|/[|V|*(|V|-1)] where |E| is the number of edges and |V| the number of nodes
	 * @return Density of the graph
	 */
	public double getDensity() {
		double cardE = this.getNbEdges();
		double  cardV = this.nodes.length-1;
		return ( cardE / (cardV * (cardV - 1)) );
	}
	
	// ===== PREPROCESSING NODES =====
	
	/**
	 * To stock the edge costs and distance in a matrix
	 * It ramdomly generates negative costs for the edges if simulate is active
	 *
	 * @param simulate Set to "true" to randomly add negative costs to the edges
	 */
	public void buildEdges(boolean simulate) {
		// For simulation purposes
		int max = 20;
		int min = 0;
		Random rand = new Random(0);

		// We introduce a cost factor to model time costs
		double costFactor = 1;
		double timeFactor = 1;

		int nbNodes = this.getNodes().length;
		this.cost = new double[nbNodes][nbNodes];
		this.distance = new double[nbNodes][nbNodes];
		for(int i=0; i < nbNodes; i++) {
			int randomInt = rand.nextInt(max - min + 1) + min;
			this.getNode(i).setDemand(0);

			for(int j=0; j < nbNodes; j++) {
				if( i != j ) {
					double euclidianDistance = this.getNodes()[i].distance(this.getNodes()[j]);
					// Truncate
					euclidianDistance =  Math.floor(euclidianDistance * 10) / 10;
										
					if( !simulate || i == 0) { randomInt = 0; }

					this.cost[j][i] = ( euclidianDistance * costFactor) - randomInt;
					this.distance[i][j] = euclidianDistance * timeFactor;
				}
			}
		}
		
		if( this.duplicateOrigin ) {
			// The origin & the depot are the same
			this.cost[0][nbNodes-1] = 0;
		}
	}
	
	/**
	 * To see which edges are allowed or forbidden
	 */
	@SuppressWarnings("unchecked")
	public void buildSuccessors() {
		this.successors = new ArrayList[this.nodes.length];
		
		for(int i = 0; i < this.nodes.length; i++) {
			Customer node = this.nodes[i];

			this.successors[i] = new ArrayList<Customer>();
			
			// We check every node to see if it is a valid successor
			int startNode = this.duplicateOrigin ? 1 : 0;
			for(int n = startNode; n < this.nodes.length; n++) {
				Customer nextNode = this.nodes[n];
				
				// We compute the time needed to reach the node which corresponds to
				// the minimal time to complete the service in the current node + the time needed to get to the next node
				double timeToReach = node.getStart() + node.getServiceTime() + this.distance[node.getId()][nextNode.getId()];
				
				// Check if it is possible
				if( i != n && nextNode.getEnd() >= timeToReach ) {
					this.successors[i].add( nextNode );
				}
			}
		}
		
		// The depot has no successors
		if( this.duplicateOrigin ) {
			this.successors[this.successors.length-1] = new ArrayList<Customer>();
			this.successors[0].remove( this.successors[0].size()-1 );
		}
	}
	
	/**
	 * Given the dual values, it updates the cost of an edge
	 * for the VRPTW sub-problem
	 * @param pi The dual variables
	 */
	public void updateDualValues(double[] pi, double pc) {
		int duplicated = duplicateOrigin ? 1 : 0;
        for (int i = 1; i < this.getNbNodes() - duplicated; i++) {
            for (int j = 0; j < this.getNbNodes(); j++) {
          	  this.cost[i][j] = this.distance[i][j] - pi[i-1] - pc;
            }
        }
	}
	
	/**
	 * 
	 * @param foundRoute
	 */
	public void deleteRouteNodes(Label foundRoute) {
		ArrayList<Customer> routeNodes = getRouteNodes(foundRoute);
		updateSuccessors(routeNodes);
	}
	
	/**
	 * 
	 * @param route
	 * @return
	 */
	private ArrayList<Customer> getRouteNodes(Label route){
		ArrayList<Customer> nodes = new ArrayList<Customer>();
		for(int i = 1; i < this.getNbNodes()-1;i++) {
			if(route.isVisited(i)) {
				nodes.add(this.getNode(i));
			}
		}
		return nodes;
	}
	
	/**
	 * 
	 * @param deleteNodes
	 */
	public void updateSuccessors(ArrayList<Customer> deleteNodes) {
		for(int i = 0; i < this.nodes.length; i++) {
			ArrayList<Customer> successorList = this.successors[i];
			Iterator<Customer> iterator = successorList.iterator();
			while(iterator.hasNext()) {
				Customer successor = iterator.next();
				if( deleteNodes.contains(successor) ) {
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Print the successors of every node
	 */
	public void printSuccessors() {
		for(int i = 0; i < successors.length; i++) {
			System.out.print("Node "+i+": ");
			for(int s = 0; s < successors[i].size(); s++) {
				System.out.print(successors[i].get(s).getId()+", ");
			}
			System.out.println("");
		}
	}
	
	/**
	 * Print the edge cost matrix
	 */
	public void printCostMatrix() {
		for( int i = 0; i < cost.length; i++ ) {
			for( int j= 0; j < cost[i].length; j++ ) {
				System.out.print( Math.floor(cost[i][j]*10)/10+" ");
			}
			System.out.println("");
		}
	}
	
	// ===== GETTERS & SETTERS =====
	
	public Customer[] getNodes() {
		return nodes;
	}
	
	public Customer getNode(int pos) {
		return nodes[pos];
	}

	public void setNodes(Customer[] nodes) {
		this.nodes = nodes;
	}
	
	public int getVehicles() {
		return nbVehicles;
	}

	public void setVehicles(int vehicles) {
		this.nbVehicles = vehicles;
	}

	public double[][] getCostMatrix() {
		return cost;
	}
	
	public double getCost(int i, int j) {
		return cost[i][j];
	}

	public double[][] getDistanceMatrix() {
		return distance;
	}
	
	public double getDistance(int i, int j) {
		return distance[i][j];
	}
	
	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	
	public ArrayList<Customer>[] getSuccessors() {
		return successors;
	}
	
	public boolean isDuplicateOrigin() {
		return duplicateOrigin;
	}

	public void setDuplicateOrigin(boolean duplicateOrigin) {
		this.duplicateOrigin = duplicateOrigin;
	}
	
	public int getNbNodes() {
		return nodes.length;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
