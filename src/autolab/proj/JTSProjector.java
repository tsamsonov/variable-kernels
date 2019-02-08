/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.proj;

import com.jhlabs.map.proj.Projection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import java.awt.geom.Point2D;

/**
 *
 * @author tsamsonov, Lomonosov MSU
 */
public class JTSProjector {
    
    /**
     * Transforms JTS GeometryCollection from geographic coordinates (degrees)
     * to projected CS. Currently implemented for polygons and points
     * @param projection
     * @param gc
     * @return 
     */
    public static GeometryCollection transform(Projection projection,
                                               GeometryCollection gc){
        GeometryCollection gc2 = (GeometryCollection) gc.clone();
        
        for (int i = 0; i < gc2.getNumGeometries(); i++) {
            Geometry g = gc2.getGeometryN(i);
            if(g instanceof Polygon){
                Polygon p = (Polygon)g;
                LineString ls = p.getExteriorRing();
                for (int j = 0; j < ls.getNumPoints(); j++) {
                    Point point = ls.getPointN(j);
                    Point2D.Double source = new Point2D.Double(point.getX(), point.getY());
                    Point2D.Double result = new Point2D.Double();
                    projection.transform(source, result);
                    ls.getCoordinateSequence().setOrdinate(j, 0, result.x);
                    ls.getCoordinateSequence().setOrdinate(j, 1, result.y);
                }
            } else if (g instanceof Point){
                Point p = (Point)g;
                Point2D.Double source = new Point2D.Double(p.getX(), p.getY());
                Point2D.Double result = new Point2D.Double();
                projection.transform(source, result);
                p.getCoordinateSequence().setOrdinate(0, 0, result.x);
                p.getCoordinateSequence().setOrdinate(0, 1, result.y);
            }
            
        }
        return gc2;
    }
    
    /**
     * Transforms JTS GeometryCollection from projected coordinate
     * to GCS (in degrees). Curretly implemented for polygons and points.
     * @param projection
     * @param gc
     * @return 
     */
    public static GeometryCollection inverseTransform(Projection projection,
                                               GeometryCollection gc){
        GeometryCollection gc2 = (GeometryCollection) gc.clone();
        
        for (int i = 0; i < gc2.getNumGeometries(); i++) {
            Geometry g = gc2.getGeometryN(i);
            if(g instanceof Polygon){
                Polygon p = (Polygon)g;
                LineString ls = p.getExteriorRing();
                for (int j = 0; j < ls.getNumPoints(); j++) {
                    Point point = ls.getPointN(j);
                    Point2D.Double source = new Point2D.Double(point.getX(), point.getY());
                    Point2D.Double result = new Point2D.Double();
                    projection.inverseTransform(source, result);
                    ls.getCoordinateSequence().setOrdinate(j, 0, result.x);
                    ls.getCoordinateSequence().setOrdinate(j, 1, result.y);
                }
            } else if (g instanceof Point){
                Point p = (Point)g;
                Point2D.Double source = new Point2D.Double(p.getX(), p.getY());
                Point2D.Double result = new Point2D.Double();
                projection.inverseTransform(source, result);
                p.getCoordinateSequence().setOrdinate(0, 0, result.x);
                p.getCoordinateSequence().setOrdinate(0, 1, result.y);
            }
        }
        return gc2;
    }
            
}
