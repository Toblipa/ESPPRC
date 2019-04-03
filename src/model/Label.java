package model;

import java.util.BitSet;

public class Label {	
	/**
	 * The last node the label path has visited
	 */
	private Customer current;
	
	/**
	 * The previous label used to build the current label
	 */
	private Label previousLabel;
	
	/**
	 * The resources used until now
	 */
	// TODO create a Resource class
	double[] resources;
	
	/**
	 * The total cost of the route
	 */
	private double cost;
	
	/**
	 * If it is dominated by another route
	 */
	private boolean isDominated;
	
	/**
	 * If the label has already been extended to every successor of the curent node
	 */
	private boolean isExtended;
	
	/**
	 * Number of unreachable nodes
	 */
	private int nbUnreachableNodes;
	
	/**
	 * List of unreachable nodes from the current Label
	 */
	private boolean[] unreachableNodes;
	
	/**
	 * Number of visited nodes
	 */
	private int nbVisitedNodes;
	
	/**
	 * The nodes visited from the origin the current node
	 */
	private boolean[] visitationVector;
		
	public Label() {
		this.resources = new double[2];
	}
	
	public Label(Customer node) {
		this.current = node;
		this.resources = new double[2];
	}
	
	// ===== GETTERS & SETTERS =====
	
	public Customer getCurrent() {
		return current;
	}

	public void setCurrent(Customer current) {
		this.current = current;
	}

	public Label getPreviousLabel() {
		return previousLabel;
	}

	public void setPreviousLabel(Label previousLabel) {
		this.previousLabel = previousLabel;
	}

	public double[] getResources() {
		return resources;
	}

	public void setResources(double[] resource) {
		this.resources = resource;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public boolean isDominated() {
		return isDominated;
	}

	public void setDominated(boolean isDominated) {
		this.isDominated = isDominated;
	}

	public boolean[] getVisitationVector() {
		return visitationVector;
	}

	public void setVisitationVector(boolean[] visitationVector) {
		this.visitationVector = visitationVector;
	}

	public boolean[] getUnreachableNodes() {
		return unreachableNodes;
	}

	public void setUnreachableNodes(boolean[] unreachableNodes) {
		this.unreachableNodes = unreachableNodes;
	}

	public int getNbUnreachableNodes() {
		return nbUnreachableNodes;
	}

	public void setNbUnreachableNodes(int nbNodes) {
		this.nbUnreachableNodes = nbNodes;
	}

	public int getNbVisitedNodes() {
		return nbVisitedNodes;
	}

	public void setNbVisitedNodes(int nbVisitedNodes) {
		this.nbVisitedNodes = nbVisitedNodes;
	}
	
	public void addToResource(int index, double value) {
		this.resources[index] += value;
	}
	
	public void setResource(int index, double value) {
		this.resources[index] = value;
	}
	
	public Double getResource(int index) {
		return this.resources[index];
	}
	
	public boolean isReachable(Customer currentSuccessor) {
		return !this.unreachableNodes[ currentSuccessor.getId() ];
	}
	
	public boolean isExtended() {
		return isExtended;
	}

	public void setExtended(boolean isExtended) {
		this.isExtended = isExtended;
	}
	
	/**
	 * Returns true if labels are equal, false if not.
	 * 
	 * @param label
	 * @return
	 */
	public boolean checkDominance(Label label) {
		
		// We check if the dominance rules can be used
		if(this.current.getId() != label.getCurrent().getId()) {
			return false;
		}
		
		if( this.previousLabel == label.getPreviousLabel() ){
			return true;
		}
		
		boolean thisDominance = false;
		boolean anotherDominance = false;
		
		double costDiff = this.cost - label.getCost();
		
		// We compare every resource
		double resourceDiff = 0;
		boolean resourceThisDominance = true;
		boolean resourceAnotherDominance = true;
		
		for (int r = 0; r < this.resources.length; r++) {
			resourceDiff = this.resources[r] - label.getResource(r);
			
			if(resourceThisDominance && resourceDiff > 0) {
				resourceThisDominance = false;
			}
			else if(resourceAnotherDominance && resourceDiff < 0) {
				resourceAnotherDominance = false;
			}
		}
		
		int nodesDiff = this.nbUnreachableNodes - label.getNbUnreachableNodes();
		
		if( costDiff <= 0 && resourceThisDominance && nodesDiff <= 0) {
			thisDominance = true;
		}
		
		if( costDiff >= 0 && resourceAnotherDominance && nodesDiff >= 0) {
			anotherDominance = true;
		}
		
		if(thisDominance && anotherDominance) {
			return false;
		}
		
		if( thisDominance ) {
			boolean[] externalUnreachableNodes = label.getUnreachableNodes();
			
			for(int k = 0; k < this.unreachableNodes.length; k++) {
				if(this.unreachableNodes[k] && !externalUnreachableNodes[k]) {
					thisDominance = false;
					break;
				}
			}
			
			label.setDominated( thisDominance );
		}
		else if( anotherDominance ) {
			boolean[] externalUnreachableNodes = label.getUnreachableNodes();
			
			for(int k = 0; k < this.unreachableNodes.length; k++) {
				if( !this.unreachableNodes[k] && externalUnreachableNodes[k] ) {
					anotherDominance = false;
					break;
				}
			}
			
			this.isDominated = anotherDominance;
		}
				
		return false;
	}
	
	/**
	 * 
	 * @param label
	 * @return
	 */
	public boolean dominates(Label label) {
		// Check if labels are comparable
		if(this.current.getId() != label.getCurrent().getId()) {
			return false;
		}
		
		// Check cost & number of unreachable nodes
		if(this.cost > label.getCost() || this.nbUnreachableNodes > label.getNbUnreachableNodes()) {
			return false;
		}
		
		// Check resources
		for (int r = 0; r < this.resources.length; r++) {
			if(this.resources[r] > label.getResource(r)) {
				return false;
			}
		}
		
		// Check unreachable nodes one by one
		boolean[] externalUnreachableNodes = label.getUnreachableNodes();
		for(int k = 0; k < this.unreachableNodes.length; k++) {
			if( this.unreachableNodes[k] && !externalUnreachableNodes[k] ) {
				return false;
			}
		}
		
		// If we have not broke, the current label dominates the other
		label.setDominated(true);
		return true;
	}
	
	/**
	 * Extend the current label to an adjacent node
	 * 
	 * @param node
	 * @param instance
	 * @return
	 */
	public Label extendLabel(Customer node, EspprcInstance instance) {
		
		double arcCost = instance.getCost()[current.getId()][node.getId()];
		double arcDistance = instance.getDistance()[current.getId()][node.getId()];
		
		// We create a new label on the node successor
		Label extendedLabel = new Label( node );
		
		// We stock the previous label
		extendedLabel.setPreviousLabel( this );
		
		// We add the cost
		extendedLabel.setCost( this.cost + arcCost );
		
		// We add the resources of the label, considering we cannot visit the customer before the start time
		if( node.getStart() > this.getResource(0) + current.getServiceTime() + arcDistance ) {
			extendedLabel.setResource( 0, node.getStart() );
		}
		else {
			extendedLabel.setResource( 0, this.getResource(0) + current.getServiceTime() + arcDistance );
		}
		// Add demand resource
		extendedLabel.setResource( 1, this.getResource(1) + node.getDemand());
		
		// We update the visitation vector
		boolean[] extendedVisitationVector = this.visitationVector.clone();
		extendedVisitationVector[node.getId()] = true;
		extendedLabel.setVisitationVector( extendedVisitationVector );
		extendedLabel.setNbVisitedNodes( this.nbVisitedNodes + 1 );
		
		// We update unreachable nodes
		boolean[] currentUnreachableNodes = this.unreachableNodes;
		boolean[] extendedUnreachableNodes = new boolean[currentUnreachableNodes.length];
		int extendedNbUnreachableNodes = 0;
		
		for(int i = 0; i < instance.getNodes().length; i++ ) {
			if( node.isDepot() || i == node.getId()) {
				extendedUnreachableNodes[i] = true;
				extendedNbUnreachableNodes++;
				continue;
			}
			
			double timeToReach = extendedLabel.getResource(0) + node.getServiceTime() + instance.getDistance()[node.getId()][i];
			if ( currentUnreachableNodes[i] ||
					( instance.getNode(i).getEnd() < timeToReach ||
							instance.getCapacity() < extendedLabel.getResource(1) + instance.getNode(i).getDemand()) ) {
				extendedUnreachableNodes[i] = true;
				extendedNbUnreachableNodes++;
			}
		}
		
		extendedLabel.setUnreachableNodes( extendedUnreachableNodes );
		extendedLabel.setNbUnreachableNodes( extendedNbUnreachableNodes );
				
		return extendedLabel;
	}

	@Override
	public String toString() {
		if(this.current==null) {
			return "Empty Label";
		}
		String dominated = this.isDominated?"Dominated ":"Non Dominated ";
		return dominated + "Label [at " + current.getId() + "," +
				" cost: " + this.cost + "]";
	}
	
	/**
	 * Returns the path in form of a string
	 * @return
	 */
	public String getRoute() {
		if (this.current == null) return "Empty Label";
		
		if (previousLabel == null) return current.getId()+"";
		
		String id = current.isDepot() ? "Depot" : current.getId()+"";
		
		return this.previousLabel.getRoute()+", " + id;
	}
	
	/**
	 * Returns the visitation vector in form of a string
	 * @return
	 */
	public String stringifyVisitationVector() {
		String out = "[ ";
		for(int index = 0; index < this.visitationVector.length; index++) {
			out += this.visitationVector[index]+" ";
		}
		out += "]";
		return out;
	}
	
	/**
	 * Returns the unreachable nodes vector in form of a string
	 * @return
	 */
	public String stringifyUnreachableNodes() {
		String out = "[ ";
		for(int index = 0; index < this.unreachableNodes.length; index++) {
			out += this.unreachableNodes[index]+" ";
		}
		out += "]";
		return out;
	}
	
	/**
	 * Returns the resources utilization in form of a string
	 * @return
	 */
	public String stringifyResource() {
		String out = "[ ";
		for(int index = 0; index < this.resources.length; index++) {
			out += this.resources[index]+", ";
		}
		out += "]";
		return out;
	}
	
	/**
	 * Efficency test
	 */
	public static void main(String[] args) {
		int nbTests = 50;
		int nbNodes = 25;
		int nbLists = 1000;
		
		long totalIntegerSetTime = 0;
		long totalBooleanSetTime = 0;
		long totalBitSetSetTime = 0;
		
		System.out.println("Start tests...");
		for(int t = 0; t < nbTests; t++) {
						
			int[][] integerTestSet = new int[nbLists][nbNodes];
			boolean[][] booleanTestSet = new boolean[nbLists][nbNodes];;
			BitSet[] bitSetTestSet = new BitSet[nbLists];
			for(int i = 0; i < nbLists; i++) {
				BitSet bitSet = new BitSet();
				for(int j = 0; j < nbNodes; j++) {
					if(Math.random() > 0.5) {
						integerTestSet[i][j] = 1;
						booleanTestSet[i][j] = true;
						bitSet.set(j);
					}else {
						integerTestSet[i][j] = 0;
						booleanTestSet[i][j] = false;
					}
				}
				bitSetTestSet[i] = bitSet;
			}

			for(int i = 0; i < nbLists; i++) {
				for(int j = 0; j < nbLists; j ++) {
					boolean integerResult;
					boolean booleanResult;
					boolean bitSetResult;
					
					integerResult = true;
					long startIntegerTime = System.nanoTime();
					for(int k = 0; k < nbNodes; k++) {
						if(integerTestSet[i][k] > integerTestSet[j][k]) {
							integerResult = false;
							break;
						}
					}
					long endIntegerTime = System.nanoTime();

					totalIntegerSetTime += (endIntegerTime - startIntegerTime);
					
					booleanResult = true;
					long startBooleanTime = System.nanoTime();
					for(int k = 0; k < nbNodes; k++) {
						if(booleanTestSet[i][k] && !booleanTestSet[j][k]) {
							booleanResult = false;
							break;
						}
					}
					long endBooleanTime = System.nanoTime();

					totalBooleanSetTime += (endBooleanTime - startBooleanTime);

					long startBitSetTime = System.nanoTime();
					BitSet compareSet = (BitSet) bitSetTestSet[i].clone();
					compareSet.and(bitSetTestSet[j]);
					bitSetResult = compareSet.cardinality() == bitSetTestSet[i].cardinality();
					long endBitSetTime = System.nanoTime();

					totalBitSetSetTime += (endBitSetTime - startBitSetTime);
					
					if(integerResult != booleanResult || integerResult != bitSetResult || booleanResult != bitSetResult) {
						System.out.println("ERROR");
					}
				}
			}
		}
		System.out.println("Finished tests");
		
		System.out.println("Results:");
		System.out.println("Integer Comparison: "+(totalIntegerSetTime/1000000)/nbTests);
		System.out.println("Boolean Comparison: "+(totalBooleanSetTime/1000000)/nbTests);
		System.out.println("BitSet Comparison: "+(totalBitSetSetTime/1000000)/nbTests);
	}
}
