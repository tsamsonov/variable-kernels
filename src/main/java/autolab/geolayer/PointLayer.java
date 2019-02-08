/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geolayer;


import autolab.symbols.LineSimple;
import autolab.symbols.PointSimple;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import java.util.ArrayList;
import com.jogamp.opengl.GLAutoDrawable;
import org.locationtech.jts.geom.Point;

/**
 *
 * @author tsamsonov
 */
public class PointLayer extends Layer {

    ArrayList<Point> points;
    
    public PointLayer(){
        symbol = new PointSimple();
        points = new ArrayList<>();
    }
    
    @Override
    public void drawLayer(GLAutoDrawable drawable) {
        for(Point p: points){
            symbol.draw(p, drawable);
        }
    }



    @Override
    public void add(Geometry g) {
        if(g instanceof Point){
            points.add((Point)g);
        }
    }

    @Override
    public void remove(int n) {
        if (n >=0 && n < points.size()){
            points.remove(n);
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
