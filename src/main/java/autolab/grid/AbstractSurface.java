/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.grid;

/**
 * Abstract class for various surfaces reconstructed on the grid
 * @author tsamsonov
 */
public abstract class AbstractSurface {
    public boolean initialized = false;
    double zFactor = 1.0d;
    
    public abstract float getSlope();
    public abstract float getAspect();
    public abstract float getCurvature();
    public abstract float getPlanCurvature();
    public abstract float getProfileCurvature();
    public abstract float getHillshade(double azimuth, double height);
    
    public void setZfactor(float zf){
        zFactor = zf;
    }
    
}
