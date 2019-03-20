package model;

import java.util.HashMap;
import java.util.Map;

public class Label {
	
	// The current node
	private Customer current;
	
	// The previous label used to build the current label
	private Label previousLabel;
	
	// The resources used until now
	Map<String, Double> resource;
	
	// one resource
	private double resources;
	
	// The total cost of the route
	private double cost;
	
	// If it is dominated by another route
	private boolean isDominated;
	
	// Number of unreachable nodes
	private int nbUnreachableNodes;
	
	// List of unreachable nodes from the current Label
	private int[] unreachableNodes;
	
	// Number of visited nodes
	private int nbVisitedNodes;
	
	// The nodes visited from the origin the current node
	private int[] visitationVector;
	
	public Label() {
		this.resource = new HashMap<String, Double>();
	}
	
	public Label(Customer node) {
		this.current = node;
		this.resource = new HashMap<String, Double>();
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

	public Map<String, Double> getResource() {
		return resource;
	}

	public void setResource(Map<String, Double> resource) {
		this.resource = resource;
	}

	public double getResources() {
		return resources;
	}

	public void setResources(double resources) {
		this.resources = resources;
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

	public int[] getUnreachableNodes() {
		return unreachableNodes;
	}

	public void setUnreachableNodes(int[] unreachableNodes) {
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
	
	public void addResource(String name) {
		this.resource.put(name, 0.0);
	}
	
	public void setResource(String name, Double value) {
		this.resource.put(name, value);
	}
	
	public Double getResource(String name) {
		return this.resource.get(name);
	}
	
	@Deprecated
	public boolean dominates(Label label) {
		boolean dominance = true;
		
		if(	this.cost > label.getCost() ||
			this.resources > label.getResources() ||
			this.nbUnreachableNodes > label.getNbUnreachableNodes() ) {
			dominance = false;
		}
		
		if( dominance ) {
			for(int n = 0; n < this.unreachableNodes.length; n++) {
				if(this.unreachableNodes[n] > label.getUnreachableNodes()[n] ) {
					dominance = false;
					break;
				}
			}
		}
		
		label.setDominated(dominance);
		
		return dominance;
	}
	
	/**
	 * 
	 * @param label
	 * @return
	 */
	public boolean checkDominance(Label label) {
//		System.out.println("Comparing labels:");
//		System.out.println(this);
//		System.out.println(this.stringifyVisitationVector());
//		System.out.println(label);
//		System.out.println(label.stringifyVisitationVector());
		
		boolean thisDominance = false;
		boolean anotherDominance = false;

		
		int costDiff = new Double(this.cost).compareTo(label.getCost());
		
		// We compare every resource
		int resourceDiff = 0;
		boolean resourceThisDominance = true;
		boolean resourceAnotherDominance = true;
		for (Map.Entry<String, Double> l : this.resource.entrySet()) {
			resourceDiff = l.getValue().compareTo( label.getResource().get(l.getKey()) );
			
			if(resourceThisDominance && resourceDiff > 0) {
				resourceThisDominance = false;
			}
			else if(resourceAnotherDominance && resourceDiff < 0) {
				resourceAnotherDominance = false;
			}
		}
		
//		int resourcesDiff = new Double(this.resources).compareTo(label.getResources());
//		resourceThisDominance = resourcesDiff <= 0;
//		resourceAnotherDominance = resourcesDiff >= 0;
		
		int nodesDiff = new Integer(this.nbUnreachableNodes).compareTo(label.getNbUnreachableNodes());
		
		if( costDiff <= 0 && resourceThisDominance && nodesDiff <= 0) {
			thisDominance = true;
		}
		
		if( costDiff >= 0 && resourceAnotherDominance && nodesDiff >= 0) {
			anotherDominance = true;
		}
		
		if(thisDominance && anotherDominance) {
			thisDominance = false;
			anotherDominance = false;
		}
		
		if( thisDominance || anotherDominance ) {
			for(int n = 0; n < this.unreachableNodes.length; n++) {
				if(this.unreachableNodes[n] != label.getUnreachableNodes()[n] ) {
					thisDominance = thisDominance && this.unreachableNodes[n] <= label.getUnreachableNodes()[n];
					anotherDominance = anotherDominance && this.unreachableNodes[n] >= label.getUnreachableNodes()[n];
					
					if(!thisDominance && !anotherDominance) {
						break;
					}
				}
			}
		}
		
		label.setDominated( thisDominance );
		this.isDominated = anotherDominance;
		
//		if(thisDominance) {
//			System.out.println("Result: "+this+" dominates "+label);
//		}
//		if(anotherDominance) {
//			System.out.println("Result: "+label+" dominates "+this);
//		}
//		System.out.println("");
		
		return thisDominance;
	}

	@Override
	public String toString() {
		if(this.current==null) {
			return "Empty Label";
		}
		String dominated = this.isDominated?"Dominated":"Non Dominated";
		return dominated + " Label [at " + current.getCustomerId() + "," +
				" resorces: " + this.resources + "," +
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
	
	public String stringifyUnreachableNodes() {
		String out = "[ ";
		for(int index = 0; index < this.unreachableNodes.length; index++) {
			out += this.unreachableNodes[index]+" ";
		}
		out += "]";
		return out;
	}
	
}
