/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.symbols;

import org.locationtech.jts.geom.Geometry;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author timofey
 */
public abstract class Symbol {
    public abstract void draw(Geometry g, GLAutoDrawable drawable);
}