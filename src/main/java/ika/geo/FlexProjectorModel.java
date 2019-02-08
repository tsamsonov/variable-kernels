/*
 * FlexProjectorModel.java
 *
 * Created on May 16, 2007, 12:04 PM
 *
 */
package ika.geo;

import java.util.ArrayList;
import com.jhlabs.map.proj.*;
import com.jhlabs.map.proj.Projection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;


/**
 * The model object for the Flex Projector application. Holds all model data.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 * 
 * Adapted by Tim Samsonov, Lomonosov MSU for RasterProcessing Application
 */

/**
 * Adapted for RasterProcessing Application. Uses JTS library for storing
 * resulting geometry and direct sequence of latitudes and longitudes
 * @editor Tim Samsonov, Lomonosov MSU.
 * 
 */
public class FlexProjectorModel {

    private static final long serialVersionUID = -56297773451900288L;
    /**
     * Number of points in a Tissot ellipse.
     */
    private static final int TISSOT_CIRCLE_POINT_COUNT = 40;
    /**
     * The radius of a Tissot circle on the sphere.
     */
    private static final double TISSOT_CIRCLE_RADIUS = 0.00000025;
    
    /**
     * Scale factor applied to Tissot indicatrices to approach an infinitesimal
     * size.
     */
    private static final double TISSOT_SCALE = 500000;
    

    /**
     * Constructs an array of Tissot indicatrices for the passed projection.
     * Very small circles are constructed from straight lines. The circles are
     * projected and then enlarged for display. This approach gives the
     * indicatrices a more regular ellipse-like shape. It requires that all
     * computations are done with doubles (and not floats). Also, the clipping
     * along the outlines of the projection of GeoPath.project() cannot be used,
     * since the ellipses are enlarged after projecting them.
     *
     * @param projection The projection for which indicatrices are constructed.
     * @param lats array of lats for which the ellipses should be constructed
     * @param lons array of lons for which the ellipses should be constructed
     * @param tissotScale the scale of tissot indicatrices
     * @return A GeoSet containing the indicatrices as GeoPath objects.
     */
    public static GeometryCollection constructTissotIndicatrices(Projection projection, double lats[], double lons[], double tissotScale) {

        // remember the central meridian and set it to 0.
        final double lon0 = projection.getProjectionLongitude();
        projection.setProjectionLongitude(0);

        try {
            // geometry factory for constructing polygons
            GeometryFactory gfact = new GeometryFactory();
            ArrayList<LineString> ellipses = new ArrayList<>();
            
            // construct the ellipses per columns, from left to right
            for (int i = 0; i < lons.length; i++) {

                // the longitude of the current column of ellipses
                final double lon = Math.toRadians(lons[i]);

                // make sure the longitude is in the range -pi..+pi
                if (lon < -Math.PI - 0.0000001 || lon > Math.PI + 0.0000001) {
                    continue;
                }

                // construct a column of ellipses from bottom to top
                for (int j = 0; j < lats.length; j++) {

                    // the latitude of the current ellipse
                    final double lat = Math.toRadians(lats[j]);

                    LineString ellipse = constructTissotIndicatrix(projection, lon, lat, tissotScale);
                    
                    ellipses.add(ellipse);
                }
            }
            
            LineString[] exportEllipses = new LineString[ellipses.size()];
            return gfact.createGeometryCollection(ellipses.toArray(exportEllipses));
            
        } finally {
            // reset to the initial central meridian
            projection.setProjectionLongitude(lon0);
        }
    }
    
    /**
     * The same as previous but for point collection
     * @param projection
     * @param points
     * @param tissotScale
     * @return 
     */
    public static GeometryCollection constructTissotIndicatrices(Projection projection, GeometryCollection points, double tissotScale) {

        // remember the central meridian and set it to 0.
//        final double lon0 = projection.getProjectionLongitude();
//        projection.setProjectionLongitude(0);
        
        try {
            // geometry factory for constructing polygons
            GeometryFactory gfact = new GeometryFactory();
            ArrayList<LineString> ellipses = new ArrayList<>();
            
            // container for the coordinates of a projected point
            java.awt.geom.Point2D.Double geoPt = new java.awt.geom.Point2D.Double();
        
            for(int i = 0; i < points.getNumGeometries(); i++){
                Point p = (Point)points.getGeometryN(i);
                // the longitude of the current column of ellipses
                projection.inverseTransformRadians(new java.awt.geom.Point2D.Double(p.getX(), p.getY()), geoPt);
                
                final double lon = geoPt.x;
                
                // make sure the longitude is in the range -pi..+pi
                if (lon < -Math.PI - 0.0000001 || lon > Math.PI + 0.0000001) {
                    continue;
                }
                
                // the latitude of the current ellipse
                final double lat = geoPt.y;
                
                // make sure the longitude is in the range -pi..+pi
                if (lat < -Math.PI/2 + 0.0000001 || lat > Math.PI/2 - 0.0000001) {
                    continue;
                }

                LineString ellipse = constructTissotIndicatrix(projection, lon, lat, tissotScale);

                ellipses.add(ellipse);
            }
            
            LineString[] exportEllipses = new LineString[ellipses.size()];
            return gfact.createGeometryCollection(ellipses.toArray(exportEllipses));
        } finally {
            // reset to the initial central meridian
//            projection.setProjectionLongitude(lon0);
        }
    }
    
    /**
     * Constructs single Tissot ellipse
     * @param projection
     * @param lon
     * @param lat
     * @param tissotScale
     * @return 
     */
    private static LineString constructTissotIndicatrix(Projection projection, double lon, double lat, double tissotScale){
        GeometryFactory gfact = new GeometryFactory();
        
        // container for the coordinates of a projected point
        java.awt.geom.Point2D.Double projPt = new java.awt.geom.Point2D.Double();

        // array holding the coordinates of an ellipse. This array will be
        // reused for each ellipse. x1, y1, x2, y2, ...
        double[] circlePts = new double[TISSOT_CIRCLE_POINT_COUNT * 2];

        // scale factor to convert from the unary sphere to earth coordinates
        // final double scale = projection.getEquatorRadius();

        final double scale = 1.d;

        // scale factor to enlarge the small ellipses
        final double indicatrixScale = TISSOT_SCALE * tissotScale;
        // construct the circle in radians around lon/lat
        constructPlateCarreeCircle(lon, lat, circlePts);

        // compute the center of the indicatrix in earth coordinates
        try {
            projection.transformRadians(lon, lat, projPt);
        } catch (ProjectionException exc) {
            return null;
        }
        final double cx = scale * projPt.x;
        final double cy = scale * projPt.y;
        if (Double.isNaN(cx) || Double.isNaN(cy)
                || Double.isInfinite(cx) || Double.isInfinite(cy)) {
            return null;
        }

        // project the small circle to an ellipse, enlarge it and
        // add each point to a new GeoPath.

        Coordinate[] coords = new Coordinate[circlePts.length/2 + 1];

        for (int k = 0; k < circlePts.length / 2; k++) {
            // project the point
            try {
                projection.transformRadians(circlePts[k * 2], circlePts[k * 2 + 1], projPt);
            } catch (ProjectionException exc) {
                continue;
            }
            // convert from the unary sphere to earth coordinates
            double x = scale * projPt.x;
            double y = scale * projPt.y;

            if (!Double.isNaN(x) && !Double.isNaN(y)
                    && !Double.isInfinite(x) && !Double.isInfinite(y)) {
                // scale the point relative to the ellipse center
                coords[k] = new Coordinate(
                        (x - cx) * indicatrixScale + cx,
                        (y - cy) * indicatrixScale + cy
                );
            }
        }

        // close line
        coords[circlePts.length/2] = new Coordinate(coords[0]);
        
        return gfact.createLineString(coords);
    }
    /**
     * Constructs a circle around lon/lat in Plate Carree projection. The 
     * outline consists of straight line segments. 
     * The number of points is TISSOT_CIRCLE_POINT_COUNT and the radius is
     * TISSOT_CIRCLE_RADIUS.
     * @param lon The horizontal coordinate of the center.
     * @param lat The vertical coordinate of the center.
     * @param circle An array that will receive the coordinates of the outline
     * of the circle.
     */
    private static void constructPlateCarreeCircle(double lon, double lat, double[] circle) {

        final double angleIncrement = Math.PI * 2. / TISSOT_CIRCLE_POINT_COUNT;
        for (int i = 0; i < TISSOT_CIRCLE_POINT_COUNT; i++) {
            final double a = i * angleIncrement;
            circle[i * 2] = lon + Math.cos(a) * TISSOT_CIRCLE_RADIUS / Math.cos(lat);
            circle[i * 2 + 1] = lat + Math.sin(a) * TISSOT_CIRCLE_RADIUS;
        }
    }

    
}
