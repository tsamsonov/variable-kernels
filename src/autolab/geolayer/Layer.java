/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geolayer;

import autolab.symbols.Symbol;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author tsamsonov
 */
public abstract class Layer {
    
    Symbol symbol;
    
    String name;
    
    Envelope envelope;
   
    
    /*
    Image fit flags
    */
    boolean fit = false;
    boolean fitWidth = false;
    boolean fitHeight = false;
    
    public void setName(String name){
        this.name = name;
    }
    
    boolean isVisible = true;
    
    /**
     * Sets fit mode
     * @param f 
     */
//    public void setFit(int f){
//        
//        if(f == 0){
//            fit = true;
//            fitWidth = false;
//            fitHeight = false;
//        } else if (f == 1){
//            fit = false;
//            fitWidth = true;
//            fitHeight = false;
//        } else if (f == 2){
//            fit = false;
//            fitWidth = false;
//            fitHeight = true;
//        } else {
//            fit = false;
//            fitWidth = false;
//            fitHeight = false;
//        }
//    }
    
    public void setVisible(boolean visible){
        isVisible = visible;
    }
    
    public boolean isVisible(){
        return isVisible;
    }
    
    /**
     * Adds object to layer
     * @param g 
     */
    public abstract void add(Geometry g);
    
    /** 
     * Removes object from layer
     * @param n
     */
    public abstract void remove(int n);
    
    /**
     * Clears layer from all objects
     */
    public abstract void clear();
       
    /**
     * Draws the map
     * @param drawable 
     */
    public void draw(GLAutoDrawable drawable){
        if(isVisible){
            drawLayer(drawable);
        }
    };
    
    public abstract void drawLayer(GLAutoDrawable drawable);
    
    /** Fits image into the viewport
     * @param drawable 
     */
//    abstract void fit(GLAutoDrawable drawable);
//    
//    /**
//     * Fits image into height of the viewport
//     * @param drawable 
//     */
//    abstract void fitHeight(GLAutoDrawable drawable);
//    
//    /**
//     * Fits image into width of the viewport
//     * @param drawable 
//     */
//    abstract void fitWidth(GLAutoDrawable drawable);
    
    public Envelope getEnvelope(){
        return envelope;
    }

}
