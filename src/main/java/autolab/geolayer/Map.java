/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geolayer;

import autolab.symbols.PointSimple;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import java.util.ArrayList;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author tsamsonov
 */
public class Map {
     /*
    Layers contained in the map
    */
    ArrayList<Layer> layers;
    
//    Point cursor;
    
//    PointSimple cursorSymbol;
    
//    boolean showCursor = false;
    
    public Map(){
        layers = new ArrayList<>();
        
        GeometryFactory gfact = new GeometryFactory();
//        cursor = gfact.createPoint(new Coordinate(0, 0));
        
//        cursorSymbol = new PointSimple();
    }
    
    public void addLayer(Layer lyr){
        layers.add(lyr);
    }
    
    // draws the map
    public void draw(GLAutoDrawable drawable){
        for(Layer l: layers){
            if(l !=  null)
                l.draw(drawable);
        }
        
//        if(showCursor){
//            cursorSymbol.draw(cursor, drawable);
//        }
    }
    
//    public Point getCursor(){
//        return cursor;
//    }
//    
//    public void showCursor(boolean show){
//        showCursor = show;
//    }
    
    public void setLayer(Layer lyr, int n){
        if(n > 0 && n < layers.size()){
            layers.remove(n);
            layers.add(n, lyr);
        }
    }
    
    public Layer getLayer(int n){
        if(n<0 || n >= layers.size())
            return null;
        return layers.get(n);
    }
    
}
