package model;

public class Resources {
	private double cost;

	private double time;

	private double demand;

	private int nbUnreachableNodes;

	private boolean[] unreachableVector;

	/**
	 * Initialize the origin resources
	 * @param instance
	 */
	public Resources(EspprcInstance instance) {
		nbUnreachableNodes = 0;
		unreachableVector = new boolean[instance.getNbNodes()];
	}
	/**
	 * Creates a copy of the given resources
	 * @param resources
	 */
	public Resources(Resources resources) {
		cost = resources.getCost();
		time = resources.getTime();
		demand = resources.getDemand();
		nbUnreachableNodes = resources.getNbUnreachableNodes();
		unreachableVector = resources.getUnreachableVector();
//		unreachableVector = new boolean[ resources.getUnreachableVector().length ];
	}

	public void addCost(double amount){
		cost += amount;
	}

	public void addDemand(double amount) {
		demand += amount;
	}
	
	public void addTime(double amount) {
		time += amount;		
	}
	
	public void setTime(double time) {
		this.time = time;		
	}

	public boolean lesserThan(Resources resources) {
		// Check cost & number of unreachable nodes
		if(this.cost > resources.getCost() || this.nbUnreachableNodes > resources.getNbUnreachableNodes()) {
			return false;
		}

		// Check resources
		if( this.time > resources.getTime() ) {
			return false;
		}			
		if( this.demand > resources.getDemand() ) {
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
	
	public Resources extendResources(EspprcInstance instance, Customer previousNode, Customer currentNode) {
		double arcCost = instance.getCost()[previousNode.getId()][currentNode.getId()];
		double arcDistance = instance.getDistance()[previousNode.getId()][currentNode.getId()];
		
		Resources extendedResources = new Resources(this);
		
		// We add the cost
		extendedResources.addCost( arcCost );
		
		// We add the time of the label, considering we cannot visit the customer before the start time
		if( currentNode.getStart() > this.time + previousNode.getServiceTime() + arcDistance ) {
			extendedResources.setTime( currentNode.getStart() );
		}
		else {
			extendedResources.addTime( previousNode.getServiceTime() + arcDistance );
		}
		
		// Add demand resource
		extendedResources.addDemand( currentNode.getDemand() );
		
		// Update unreachable nodes
		extendedResources.updateUnreachableNodes(instance, currentNode);
		
		return extendedResources;
	}
	
	public void updateUnreachableNodes(EspprcInstance instance, Customer currentNode) {
		// We update unreachable nodes
		this.nbUnreachableNodes = 0;
		for(int i = 0; i < instance.getNodes().length; i++ ) {
			if( currentNode.isDepot() || i == currentNode.getId()) {
				this.unreachableVector[i] = true;
				this.nbUnreachableNodes++;
				continue;
			}
			
			double timeToReach = this.time + currentNode.getServiceTime() + instance.getDistance()[currentNode.getId()][i];
			if ( this.unreachableVector[i] ||
					( instance.getNode(i).getEnd() < timeToReach ||
							instance.getCapacity() < this.getTime() + instance.getNode(i).getDemand()) ) {
				this.unreachableVector[i] = true;
				this.nbUnreachableNodes++;
			}
		}
		
//		extendedLabel.setUnreachableNodes( extendedUnreachableNodes );
//		extendedLabel.setNbUnreachableNodes( extendedNbUnreachableNodes );
		
	}
	public void updateResources(double cost, double distance) {
		
	}

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
}