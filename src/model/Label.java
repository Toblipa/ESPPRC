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
	
	// The nodes visited from the origin the current node
	private boolean[] path;
	
	public boolean compare(Label label) {
		
		if(	this.cost <= label.getCost() &&
			this.resources <= label.getResources() &&
			this.path != label.getPath()	)
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

	public boolean[] getPath() {
		return path;
	}

	public void setPath(boolean[] path) {
		this.path = path;
	}
}
