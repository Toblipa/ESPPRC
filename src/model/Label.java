package model;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class Label {
	
	// The current node
	private Customer current;
	
	// The previous label used to build the current label
	private Label previousLabel;
	
	// The resources used until now
//	Map<String, Double> resources;
	double[] resources;
	
	// The total cost of the route
	private double cost;
	
	// If it is dominated by another route
	private boolean isDominated;
	
	// Number of unreachable nodes
	private int nbUnreachableNodes;
	
	// List of unreachable nodes from the current Label
	private BitSet unreachableNodes;
	
	// Number of visited nodes
	private int nbVisitedNodes;
	
	// The nodes visited from the origin the current node
	private int[] visitationVector;
		
	public Label() {
		this.resources = new double[2];
	}
	
	public Label(Customer node) {
		this.current = node;
		this.resources = new double[2];
	}

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

	public int[] getVisitationVector() {
		return visitationVector;
	}

	public void setVisitationVector(int[] visitationVector) {
		this.visitationVector = visitationVector;
	}

	public BitSet getUnreachableNodes() {
		return unreachableNodes;
	}

	public void setUnreachableNodes(BitSet unreachableNodes) {
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
	
//	public void addResource(String name) {
//		this.resources.put(name, 0.0);
//	}
	
	public void addToResource(int index, double value) {
		this.resources[index] += value;
	}
	
	public void setResource(int index, double value) {
		this.resources[index] = value;
	}
	
	public Double getResource(int index) {
		return this.resources[index];
	}
	/*
	 * For time measure purposes
	 */
	class DominanceResult{
		private boolean equals;
		private long timeElapsed;
		
		public DominanceResult (boolean result, long timeElapsed) {
			this.equals = result;
			this.timeElapsed = timeElapsed;
		}

		public boolean getEquals() {
			return equals;
		}

		public long getTimeElapsed() {
			return timeElapsed;
		}
	}
	/**
	 * Returns true if the labels are equal, false if not.
	 * 
	 * @param label
	 * @return
	 */
	public DominanceResult checkDominance(Label label) {
		
		// We check if the dominance rules can be used
		if(this.current.getCustomerId() != label.getCurrent().getCustomerId()) {
			return new DominanceResult(false, 0);
		}
		
		if( this.previousLabel == label.getPreviousLabel() ){
			return new DominanceResult (true, 0);
		}
		
		boolean thisDominance = false;
		boolean anotherDominance = false;
		
		double costDiff = this.cost - label.getCost();
		
		// We compare every resource
		double resourceDiff = 0;
		boolean resourceThisDominance = true;
		boolean resourceAnotherDominance = true;
		
//		for (Map.Entry<String, Double> l : this.resources.entrySet()) {
//			resourceDiff = l.getValue().compareTo( label.getResource(l.getKey()) );
//			
//			if(resourceThisDominance && resourceDiff > 0) {
//				resourceThisDominance = false;
//			}
//			else if(resourceAnotherDominance && resourceDiff < 0) {
//				resourceAnotherDominance = false;
//			}
//		}
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
		
		// Only comparing time resource
//		resourceThisDominance = this.getResource("Time").compareTo( label.getResource("Time") ) <= 0;
//		resourceAnotherDominance =  this.getResource("Time").compareTo( label.getResource("Time") ) >= 0;
		
		if( costDiff <= 0 && resourceThisDominance && nodesDiff <= 0) {
			thisDominance = true;
		}
		
		if( costDiff >= 0 && resourceAnotherDominance && nodesDiff >= 0) {
			anotherDominance = true;
		}
		
		if(thisDominance && anotherDominance) {
			return new DominanceResult(false, 0);
		}
		
		long startReadingThrough = System.nanoTime();		
		if( thisDominance ) {
			BitSet compareSet = (BitSet) label.getUnreachableNodes().clone();
			compareSet.and(this.unreachableNodes);
			
			thisDominance = thisDominance && compareSet.cardinality() == this.nbUnreachableNodes;
			
			label.setDominated( thisDominance );
		}
		else if( anotherDominance ) {
			BitSet compareSet = (BitSet) this.unreachableNodes.clone();
			compareSet.and(label.getUnreachableNodes());
			
			anotherDominance = anotherDominance && compareSet.cardinality() == label.getNbUnreachableNodes();
			
			this.isDominated = anotherDominance;
		}
		long endReadingThrough = System.nanoTime();
		
//		if( thisDominance || anotherDominance ) {
//			for(int n = 0; n < this.unreachableNodes.length; n++) {
//				if(this.unreachableNodes[n] != label.getUnreachableNodes()[n] ) {
//					thisDominance = thisDominance && (this.unreachableNodes[n] <= label.getUnreachableNodes()[n]);
//					anotherDominance = anotherDominance && (this.unreachableNodes[n] >= label.getUnreachableNodes()[n]);
//					
//					if(!thisDominance && !anotherDominance) {
//						break;
//					}
//				}
//			}
//		}
				
		return new DominanceResult(false, endReadingThrough - startReadingThrough);
//		return false;
	}
	
	public boolean dominates(Label label) {
		if(this.current.getCustomerId() != label.getCurrent().getCustomerId()) {
			return false;
		}
		
		if(this.cost > label.getCost() || this.nbUnreachableNodes > label.getNbUnreachableNodes()) {
			return false;
		}
		
		for (int r = 0; r < this.resources.length; r++) {
			if(this.resources[r] > label.getResource(r)) {
				return false;
			}
		}
		
		long startReadingThrough = System.nanoTime();
		
		BitSet compareSet = (BitSet) label.getUnreachableNodes().clone();
		compareSet.and(this.unreachableNodes);
		
		boolean result = compareSet.cardinality() == this.nbUnreachableNodes;
		
		long endReadingThrough = System.nanoTime();

		label.setDominated(result);
		
		return result;
	}

	@Override
	public String toString() {
		if(this.current==null) {
			return "Empty Label";
		}
		String dominated = this.isDominated?"Dominated ":"Non Dominated ";
		return dominated + "Label [at " + current.getCustomerId() + "," +
				" cost: " + this.cost + "]";
	}
	
	public String getRoute() {
		if (this.current == null) return "Empty Label";
		
		if (previousLabel == null) return current.getCustomerId()+"";
		
		String id = current.isDepot() ? "Depot" : current.getCustomerId()+"";
		
		return this.previousLabel.getRoute()+" => " + id;
	}
	
	public String stringifyVisitationVector() {
		String out = "[ ";
		for(int index = 0; index < this.visitationVector.length; index++) {
			out += this.visitationVector[index]+" ";
		}
		out += "]";
		return out;
	}
	
//	public String stringifyUnreachableNodes() {
//		String out = "[ ";
//		for(int index = 0; index < this.unreachableNodes.; index++) {
//			out += this.unreachableNodes[index]+" ";
//		}
//		out += "]";
//		return out;
//	}
	
//	public String stringifyResources() {
//		String out = "{";
//		for (String name: this.resources.keySet()){
//			String key = name.toString();
//			String value = this.resources.get(name).toString();
//			out += (key + ": " + value+", ");
//		}
//		out += "}";
//		return out;
//	}
}
