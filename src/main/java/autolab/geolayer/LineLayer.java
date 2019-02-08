/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geolayer;

import autolab.symbols.LineSimple;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import java.util.ArrayList;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author tsamsonov
 */
public class LineLayer extends Layer {
    
    ArrayList<LineString> lines;
    
    public LineLayer(){
        symbol = new LineSimple();
        lines = new ArrayList<>();
    }
    
    @Override
    public void drawLayer(GLAutoDrawable drawable) {
        for(LineString line: lines){
            symbol.draw(line, drawable);
        }
    }


    @Override
    public void add(Geometry g) {
        if(g instanceof LineString){
            lines.add((LineString)g);
        }
    }

    @Override
    public void remove(int n) {
        if(n > 0 && n < lines.size()){
            lines.remove(n);
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
