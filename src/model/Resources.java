package model;

public class Resources implements Comparable<Resources>{
	
	private double cost;

	private double time;
	
	private double demand;
	
	/**
	 * Number of visited nodes
	 */
	private int nbVisitedNodes;
	
	/**
	 * The nodes visited from the origin the current node
	 */
	private boolean[] visitationVector;

	/**
	 * The number of unreachable nodes
	 */
	private int nbUnreachableNodes;
	
	/**
	 * A vector showing which nodes are unreachable (true) and those who are not (false)
	 */
	private boolean[] unreachableVector;

	/**
	 * Initialize the origin resources
	 * @param instance
	 */
	public Resources(EspprcInstance instance) {
		unreachableVector = new boolean[instance.getNbNodes()];
		unreachableVector[0] = true;
		nbUnreachableNodes = 1;
		
		visitationVector = new boolean[instance.getNbNodes()];
		visitationVector[0] = true;
		nbVisitedNodes = 1;
	}
	
	/**
	 * Creates a copy of the given resources
	 * @param resources
	 */
	public Resources(Resources resources) {
		cost = resources.getCost();
		time = resources.getTime();
		demand = resources.getDemand();
		nbVisitedNodes = resources.getNbVisitedNodes();
		nbUnreachableNodes = resources.getNbUnreachableNodes();
		visitationVector = resources.getVisitationVector().clone();
		unreachableVector = new boolean[visitationVector.length];
	}
	
	/**
	 * Add the given amount to the cost resource
	 * @param amount
	 */
	public void addCost(double amount){
		cost += amount;
	}
	
	/**
	 * Add the given amount to the demand resource
	 * @param amount
	 */
	public void addDemand(double amount) {
		demand += amount;
	}
	
	/**
	 * Add the given amount to the time resource
	 * @param amount
	 */
	public void addTime(double amount) {
		time += amount;		
	}
	
	/**
	 * Set the time resource
	 * @param time
	 */
	public void setTime(double time) {
		this.time = time;		
	}
	
	/**
	 * If each resource is less than a the given Resources object
	 * @param resources
	 * @return
	 */
	public boolean lessThan(Resources resources) {
		
		if( this.cost > resources.getCost() ||
			this.nbUnreachableNodes > resources.getNbUnreachableNodes() || 
			this.time > resources.getTime() || 
			this.demand > resources.getDemand() )
		{
			return false;
		}

		// Check unreachable nodes one by one
		boolean[] externalUnreachableVector = resources.getUnreachableVector();
		for(int k = 0; k < this.unreachableVector.length; k++) {
			if( this.unreachableVector[k] && !externalUnreachableVector[k] ) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Extend the resources from the previousNode to the currentNode
	 * @param instance
	 * @param previousNode
	 * @param currentNode
	 * @return
	 */
	public void extendResources(EspprcInstance instance, Customer previousNode, Customer currentNode) {
		double arcCost = instance.getCost(previousNode.getId(), currentNode.getId());
		double arcDistance = instance.getDistance( previousNode.getId(), currentNode.getId() );
		
		// We add the cost
		this.addCost( arcCost );
		
		// We add the time of the label, considering we cannot visit the customer before the start time
		if( currentNode.getStart() > this.time + previousNode.getServiceTime() + arcDistance ) {
			this.setTime( currentNode.getStart() );
		}
		else {
			this.addTime( previousNode.getServiceTime() + arcDistance );
		}
		
		// Add demand resource
		this.addDemand( currentNode.getDemand() );
		
		// Update visited nodes
		this.updateVisitationVector(currentNode);
		
		// Update unreachable nodes
		this.updateUnreachableNodes(instance, currentNode);		
	}
	
	/**
	 * Update the unreachable nodes from the currentNode
	 * taking into account current reasources
	 * @param instance
	 * @param currentNode
	 */
	public void updateUnreachableNodes(EspprcInstance instance, Customer currentNode) {
		// We update unreachable nodes
		nbUnreachableNodes = 0;
		for(int i = 0; i < instance.getNbNodes(); i++) {
			if( currentNode.isDepot() ) {
				unreachableVector[i] = true;
				nbUnreachableNodes++;
				continue;
			}
			
			double timeToReach = this.time + currentNode.getServiceTime() + instance.getDistance(currentNode.getId(), i);
			double neededDemand = this.demand + instance.getNode(i).getDemand();
			
			boolean unreachable = ( instance.getNode(i).getEnd() < timeToReach || instance.getCapacity() < neededDemand );
			if ( visitationVector[i] || unreachable ) {
				unreachableVector[i] = true;
				nbUnreachableNodes++;
			}
		}
	}
	
	private void updateVisitationVector(Customer currentNode) {
		visitationVector[currentNode.getId()] = true;
		nbVisitedNodes++;
	}
	
	@Override
	public int compareTo(Resources that) {
		
		double costDiff = this.cost - that.getCost();		
		double timeDiff = this.time - that.getTime();
		double demandDiff = this.demand - that.getDemand();
		int unreachableNodesDiff = this.nbUnreachableNodes - that.getNbUnreachableNodes();
		
		boolean thisDominance = (costDiff <= 0 && timeDiff <= 0 && demandDiff <= 0 && unreachableNodesDiff <= 0);
		boolean thatDominance = (costDiff >= 0 && timeDiff >= 0 && demandDiff >= 0 && unreachableNodesDiff >= 0);
		
		if(thisDominance == thatDominance) {
			return 0;
		}
		
		boolean[] thatUnreachableVector = that.getUnreachableVector();
		for(int k = 0; k < this.unreachableVector.length; k++) {
			if( (thisDominance && this.unreachableVector[k] && !thatUnreachableVector[k]) || 
				(thatDominance && !this.unreachableVector[k] && thatUnreachableVector[k])	) {
				return 0;
			}
		}
		// -1 if costDiff is negative, 1 if it is positive
		return (int) Math.signum(costDiff);
	}
	
	// ============ GETTERS =================
	
	public double getCost() {
		return cost;
	}

	public double getTime() {
		return time;
	}

	public double getDemand() {
		return demand;
	}

	public int getNbUnreachableNodes() {
		return nbUnreachableNodes;
	}

	public boolean[] getUnreachableVector() {
		return unreachableVector;
	}
	
	public boolean isReachable(int id) {
		return !unreachableVector[id];
	}
	
	public boolean[] getVisitationVector() {
		return visitationVector;
	}

	public void setVisitationVector(boolean[] visitationVector) {
		this.visitationVector = visitationVector;
	}

	public int getNbVisitedNodes() {
		return nbVisitedNodes;
	}

	public void setNbVisitedNodes(int nbVisitedNodes) {
		this.nbVisitedNodes = nbVisitedNodes;
	}

	@Override
	public String toString() {
		return "("+cost+", "+time+", "+demand+", "+nbUnreachableNodes+")";
	}
}