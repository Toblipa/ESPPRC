package model;

public class ResourcesRoute implements Comparable<ResourcesRoute>{
	
	/**
	 * Total cost of the route
	 */
	private double cost;
	
	/**
	 * Fixed reduced cost of the route
	 */
	private double fixedCost;

	/**
	 * Time consumtion resource
	 */
	private double time;
	
	/**
	 * Time when the route starts = s_0
	 */
	private double startTime;
	
	/**
	 * Negative sum of the dual variables of visited nodes  
	 * \sum_{i \in Route}{- \delta_i - \epsilon_i}
	 */
	private double dualDiff;
	
	private int breakingNode;
	
	/**
	 * Number of visited nodes
	 */
	private int nbVisitedNodes;
	
	/**
	 * Nodes visited from the origin the current node
	 */
	private boolean[] visitationVector;

	/**
	 * Number of unreachable nodes
	 */
	private int nbUnreachableNodes;
	
	/**
	 * Vector showing which nodes are unreachable (true) and those who are not (false)
	 */
	private boolean[] unreachableVector;

	/**
	 * Initialize the origin resources
	 * @param instance
	 */
	public ResourcesRoute(EspprcInstance instance) {
		unreachableVector = new boolean[instance.getNbNodes()];
		unreachableVector[0] = instance.isDuplicateOrigin();
		nbUnreachableNodes = 1;
		
		visitationVector = new boolean[instance.getNbNodes()];
		visitationVector[0] = instance.isDuplicateOrigin();
		nbVisitedNodes = 1;
	}
	
	/**
	 * Create a copy of the given resources
	 * @param resources
	 */
	public ResourcesRoute(ResourcesRoute resources) {
		fixedCost = resources.getCost();
		time = resources.getTime();
		startTime = resources.getStartTime();
		dualDiff = resources.getDualDiff();
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
		fixedCost += amount;
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
	public boolean lessThan(ResourcesRoute resources) {
		
		if( this.fixedCost > resources.getCost() ||
			this.nbUnreachableNodes > resources.getNbUnreachableNodes() || 
			this.time > resources.getTime() )/*|| 
			this.demand > resources.getDemand() )*/
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
		
		extendTime(arcDistance, previousNode, currentNode);
		
		// We add the time of the label, considering we cannot visit the customer before the start time
		if( currentNode.getStart() > this.time + previousNode.getServiceTime() + arcDistance ) {
			this.setTime( currentNode.getStart() );
		}
		else {
			this.addTime( previousNode.getServiceTime() + arcDistance );
		}
		
		// Check starting time
		if(previousNode.getId() == 0) {
			this.startTime = this.time - arcDistance;
		}
		
		// We add the cost
		if(instance.getType().equals("Scheduling")) {
			
			// c`_l = c_l - beta_i + (w_{il} + st_i)*delta_i - w_{il}*epsilon_i
			double finishTime = this.time + currentNode.getServiceTime();
			double stabilityFactor = finishTime + currentNode.getStabilityTime();
			this.addCost( arcCost + stabilityFactor*currentNode.getStabilityDual() - finishTime*currentNode.getPrecedenceDual());
		}
		else if(instance.getType().equals("Routing")) {
			// c`_k = c_k - alpha_i - u_{ik}*delta_i - omega_i*epsilon_i
			this.addCost( arcCost - this.time*currentNode.getStabilityDual() - this.startTime*currentNode.getPrecedenceDual());
		}
		else {
			this.addCost( arcCost );
		}
		
		// Update visited nodes
		if(currentNode.getId() != 0) {
			this.updateVisitationVector(currentNode);
		}
		
		// Update unreachable nodes
		this.updateUnreachableNodes(instance, currentNode);		
	}
	
	private void extendTime(double arcDistance, Customer previousNode, Customer currentNode) {
		
		// We add the time of the label, considering we cannot visit the customer before the start time
		if( currentNode.getStart() > this.time + previousNode.getServiceTime() + arcDistance ) {
			this.setTime( currentNode.getStart() );
		}
		else {
			this.addTime( previousNode.getServiceTime() + arcDistance );
		}
		
		// Check starting time
		if(previousNode.getId() == 0) {
			this.startTime = this.time - arcDistance;
		}		
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
			
			boolean unreachable = ( instance.getNode(i).getEnd() < timeToReach );
			if ( visitationVector[i] || unreachable ) {
				unreachableVector[i] = true;
				nbUnreachableNodes++;
			}
		}
	}
	
	/**
	 * Extend visitation vector to given node
	 * @param currentNode
	 */
	private void updateVisitationVector(Customer currentNode) {
		visitationVector[currentNode.getId()] = true;
		nbVisitedNodes++;
	}
	
	@Override
	public int compareTo(ResourcesRoute that) {
		
		double costDiff = this.getCost() - that.getCost();
		
		int unreachableDiff = this.getNbUnreachableNodes() - that.getNbUnreachableNodes();
		
		double timeDiff = this.time - that.getTime();
		
		boolean thisDominance = (costDiff <= 0 && timeDiff <= 0 && unreachableDiff <= 0);
		boolean thatDominance = (costDiff >= 0 && timeDiff >= 0 && unreachableDiff >= 0);
		
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
		
		return (int) Math.signum(costDiff);
	}
	
	// ============ GETTERS & SETTERS =================
	
	public double getCost() {
		return fixedCost;
	}

	public double getTime() {
		return time;
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
	
	public boolean isVisited(int id) {
		return visitationVector[id];
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

	public double getStartTime() {
		return startTime;
	}

	public double getDualDiff() {
		return dualDiff;
	}

	public void addDualDiff(double dualDiff) {
		this.dualDiff += dualDiff;
	}

	@Override
	public String toString() {
		return "("+fixedCost+", "+time+", "+nbUnreachableNodes+")";
	}
}