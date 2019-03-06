package model;

public class Vehicle {
	
	private int number;
	private int capacity;
	
	public Vehicle(){
		this.setNumber(0);
		this.setCapacity(0);
	}
	
	public Vehicle(int number, int capacity) {
		this.setNumber(number);
		this.setCapacity(capacity);
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
}
