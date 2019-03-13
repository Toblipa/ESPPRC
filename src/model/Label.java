package model;

public class Label {
	
	// The current node
	private Customer current;
	
	// The previous label used to build the current label
	private Label previousLabel;
	
	// The resources used until now
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
			
	public boolean compare(Label label) {
		
		if(	this.cost <= label.getCost() &&
			this.resources <= label.getResources()	)
		{
			return true;
		}
		
		return false;
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

	public boolean dominates(Label label) {
		boolean dominance = true;
		
		if(	this.cost > label.getCost() || 
			this.nbVisitedNodes > label.getNbVisitedNodes() ||
			this.resources > label.getResources() ) {
			dominance = false;
		}
		
		if(dominance) {
			for(int n = 0; n < this.visitationVector.length; n++) {
				if(this.visitationVector[n] > label.getVisitationVector()[n] ) {
					dominance = false;
					break;
				}
			}
		}
		
		label.setDominated(dominance);
		
		return dominance;
	}
}
