package model;

import java.util.ArrayList;

public class Schedule {
	String path;
	int nbVisitedNodes;
	ArrayList<Customer> jobs;
	double[] serviceTime;
	double cost;
	
	public Schedule(int nbNodes) {
		this.jobs = new ArrayList<Customer>();
		this.serviceTime = new double[nbNodes];
	}
	
	public String getPath() {
		return this.path;
	}
	
	public int getNbVisitedNodes() {
		return this.nbVisitedNodes;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setNbVisitedNodes(int visitedNodes) {
		this.nbVisitedNodes = visitedNodes;
	}
	
	public void setCost(double cost) {
		this.cost = cost;
	}

	public double getCost() {
		return cost;
	}
	
	public void addJob(Customer job) {
		this.jobs.add(job);
	}
	
	public void setServiceTime(int nodeId, double value) {
		this.serviceTime[nodeId] = value;
	}
	
	public double getServiceTime(int nodeId) {
		return this.serviceTime[nodeId];
	}
	
	public boolean isVisited(int nodeId) {
		return this.serviceTime[nodeId] > 0;
	}
	
	public double getDuration() {
		double maxTime = 0;
		double minTime = 10000;
		for(int i=0; i < this.serviceTime.length; i++) {
			if(this.serviceTime[i] > maxTime) {
				maxTime = this.serviceTime[i];
			}
			if(this.serviceTime[i] < minTime) {
				minTime = this.serviceTime[i];
			}
		}
		
		return maxTime - minTime;
	}
}
