package io.openems.edge.solaredge.common;

public class PidController {
	private double kp; // Proportional Gain
	private double ki; // Integral Gain
	private double kd; // Derivative Gain
	private double deltaMax; // Maximum step size for PID output

	private int setpoint; // Desired target value
	private double integral; // Integral term
	private double previousError; // Last error recorded
	private double lastUpdateTime; // Time of last update
	private double previousOutput;
	private double tolerance; // Tolerance for not exceeding the limit

	public PidController(double kp, double ki, double kd, double deltaMax, double tolerance) {
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
		this.deltaMax = deltaMax;
		this.tolerance = tolerance;
		this.setpoint = 0;
		this.integral = 0;
		this.previousError = 0;
		this.lastUpdateTime = System.currentTimeMillis() / 1000.0; // Initialize with current time in seconds
		this.previousOutput = 0;
	}

	public void setSetpoint(int setpoint) {
		this.setpoint = setpoint;
	}

	public int update(int measuredValue, double currentTime) {
		double deltaTime = currentTime - lastUpdateTime;
		lastUpdateTime = currentTime;

		double error = setpoint - measuredValue;
		integral += error * deltaTime;
		double derivative = (error - previousError) / deltaTime;
		previousError = error;

		double output = kp * error + ki * integral + kd * derivative;
		// Apply tolerance only if output is attempting to exceed the setpoint
		if (output > setpoint) {
			output = setpoint - tolerance; // Adjust to stay below the setpoint by the tolerance amount
		}

		// Limit the output change by the step size
		double outputChange = output - this.previousOutput;
		output = this.previousOutput + Math.max(-deltaMax, Math.min(deltaMax, outputChange));
		this.previousOutput = output;

		return (int) output;
	}
}
