package io.openems.edge.solaredge.common;

public class PidController {
    private double kp;   // Proportional Gain
    private double ki;   // Integral Gain
    private double kd;   // Derivative Gain
    
    private int setpoint; // Sollwert
    private double integral; // Integral term
    private double previousError; // Speichert den Fehler aus dem vorherigen Schritt
    private double lastUpdateTime; // Zeitpunkt des letzten Updates
    
    public PidController(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.setpoint = 0;
        this.integral = 0;
        this.previousError = 0;
        this.lastUpdateTime = System.currentTimeMillis() / 1000.0; // Initialisieren mit aktueller Zeit in Sekunden
    }
    
    public void setSetpoint(int setpoint) {
        this.setpoint = setpoint;
    }
    
    public int update(int measuredValue, double currentTime) {
        double deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;
        
        double error = setpoint - measuredValue;
        integral += error * deltaTime; // Integral mit Delta-Zeit skalieren
        double derivative = (error - previousError) / deltaTime; // Derivative mit Delta-Zeit skalieren
        previousError = error;
        
        return (int) (kp * error + ki * integral + kd * derivative);
    }
}
