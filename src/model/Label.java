package model;

import java.util.BitSet;

public class Label implements Comparable<Label>{	
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
	private Resources resources;
	
	/**
	 * If it is dominated by another route
	 */
	private boolean isDominated;
	
	/**
	 * If the label has already been extended to every successor of the curent node
	 */
	private boolean isExtended;
		
	/**
	 * Given an instance it creates the origin label
	 * @param instance
	 */
	public Label(EspprcInstance instance) {
		current = instance.getNode(0);		
		resources = new Resources(instance);
	}
	
	/**
	 * Instanciates a new label on the given node
	 * @param node
	 */
	public Label(Customer node) {
		this.current = node;
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
		
		int comparison = this.resources.compareTo(label.getResources());
		
		if( comparison != 0 ) {
			this.setDominated( comparison > 0 );
			label.setDominated( comparison < 0 );
		}
				
		return (comparison < 0 );
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
		
		if( !this.resources.lessThan( label.getResources() ) ) {
			return false;
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
		
		// We create a new label on the node successor
		Label extendedLabel = new Label( node );
		
		// We stock the previous label
		extendedLabel.setPreviousLabel( this );
		
		// We update the resources
		Resources extendedResources = new Resources( this.resources );
		extendedResources.extendResources( instance, this.current, node );
		
		extendedLabel.setResources( extendedResources );
				
		return extendedLabel;
	}
	
	/**
	 * Returns the path in form of a string
	 * @return
	 */
	public String getRoute() {
		if (current == null) return "Empty Label";

		if (previousLabel == null) return "Start";
		
		String id = current.isDepot() ? ", Depot": ", " + current.getId();
		
		return previousLabel.getRoute() + id;
		
//		Customer prevNode = previousLabel.getCurrent();
//		
//		if (prevNode.getId() == 0) return "";
//
//		String id = current.isDepot() ? prevNode.getId()+"" : prevNode.getId()+", ";
//
//		return previousLabel.getRoute() + id;
	}
	
	/**
	 * Returns the total distance of the path
	 * @param instance
	 * @return
	 */
	public double getRouteDistance(EspprcInstance instance) {
		if (current == null) return 0;

		if (previousLabel == null) return 0;
		
		int curNode = current.getId();
		int prevNode = previousLabel.getCurrent().getId();

		return previousLabel.getRouteDistance(instance) + instance.getDistance(prevNode, curNode);
	}
	
	/**
	 * Check if both paths are equal
	 * @param route
	 * @return
	 */
	public boolean isEqual(Label route) {
		if( current.getId() == route.getCurrent().getId() ) {
			if(previousLabel == route.getPreviousLabel()) {
				return true;
			}
			else if( previousLabel == null && route.getPreviousLabel() == null) {
				return true;
			}
			else if( previousLabel != null && route.getPreviousLabel() != null) {
				return previousLabel.isEqual(route.getPreviousLabel());
			}
			return false;
		}
		return false;
	}

	@Override
	public String toString() {
		if(this.current == null) {
			return "Empty Label";
		}
		String dominated = this.isDominated?"Dominated ":"Non Dominated ";
		return dominated + "Label [at " + current.getId() + "," +
				" cost: " + this.resources.getCost() + "]";
	}
	
	@Override
	public int compareTo(Label that) {
		double comparison = this.getCost() - that.getCost();
		
		return (int) Math.signum(comparison);
	}
	
	// ===== GETTERS & SETTERS =====

	public Customer getCurrent() {
		return current;
	}

	public Label getPreviousLabel() {
		return previousLabel;
	}

	public void setPreviousLabel(Label previousLabel) {
		this.previousLabel = previousLabel;
	}

	public boolean isDominated() {
		return isDominated;
	}

	public void setDominated(boolean isDominated) {
		this.isDominated = isDominated;
	}

	public Resources getResources(){
		return resources;
	}

	public void setResources(Resources resource) {
		this.resources = resource;
	}

	public double getCost() {
		return resources.getCost();
	}
	
	public int getNbVisitedNodes() {
		return resources.getNbVisitedNodes();
	}
	
	public int getNbUnreachableNodes() {
		return resources.getNbUnreachableNodes();
	}

	public boolean isVisited(Customer node) {
		return resources.isVisited( node.getId() );
	}
	
	public boolean isVisited(int nodeId) {
		return resources.isVisited( nodeId );
	}
	
	public boolean isReachable(Customer node) {
		return resources.isReachable( node.getId() );
	}

	public boolean isExtended() {
		return isExtended;
	}

	public void setExtended(boolean isExtended) {
		this.isExtended = isExtended;
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
