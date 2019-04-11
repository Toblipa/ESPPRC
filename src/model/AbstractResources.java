package model;

public abstract class AbstractResources implements ResourcesInterface{
	
	protected double cost;
	
	/**
	 * Number of visited nodes
	 */
	protected int nbVisitedNodes;
	
	/**
	 * The nodes visited from the origin the current node
	 */
	protected boolean[] visitationVector;

	/**
	 * The number of unreachable nodes
	 */
	protected int nbUnreachableNodes;
	
	/**
	 * A vector showing which nodes are unreachable (true) and those who are not (false)
	 */
	protected boolean[] unreachableVector;

	/**
	 * Initialize the origin resources
	 * @param instance
	 */
	public AbstractResources(EspprcInstance instance) {
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
	public AbstractResources(AbstractResources resources) {
		cost = resources.getCost();
		nbVisitedNodes = resources.getNbVisitedNodes();
		nbUnreachableNodes = resources.getNbUnreachableNodes();
		visitationVector = resources.getVisitationVector().clone();
		unreachableVector = new boolean[visitationVector.length];
	}
	
	/**
	 * If each resource is less than a the given Resources object
	 * @param resources
	 * @return
	 */
//	public abstract boolean lessThan(AbstractResources resources);
	
	/**
	 * Extend the resources from the previousNode to the currentNode
	 * @param instance
	 * @param previousNode
	 * @param currentNode
	 * @return
	 */
	protected abstract void extendResources(EspprcInstance instance, Customer previousNode, Customer currentNode);
	
	protected void updateVisitationVector(Customer currentNode) {
		visitationVector[currentNode.getId()] = true;
		nbVisitedNodes++;
	}
		
	/**
	 * Add the given amount to the cost resource
	 * @param amount
	 */
	public void addCost(double amount){
		cost += amount;
	}
	
	// ============ GETTERS =================
	
	public double getCost() {
		return cost;
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

	public int getNbVisitedNodes() {
		return nbVisitedNodes;
	}
}