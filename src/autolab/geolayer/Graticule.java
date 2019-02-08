/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geolayer;

import autolab.symbols.LabelSymbol;
import autolab.symbols.LineSimple;
import autolab.symbols.PolySimple;
import com.jhlabs.map.Ellipsoid;
import com.jhlabs.map.MapMath;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionException;
import com.jhlabs.map.proj.ProjectionFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import ika.geo.FlexProjectorModel;
import ika.proj.ProjectionFactors;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import com.jogamp.opengl.GLAutoDrawable;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Class for generating and visualization of graticule coordinate grid (in degrees)
 * @author tsamsonov
 */
public class Graticule {
    
    private static final double PRECISION = 1e-5;
    
    public static final float STEP = 1.0f;
    
    /*
    Symbol used to draw the grid
    */
    private LineSimple gridSymbol;
    
    /*
    Symbol used to draw the frame
    */
    private PolySimple frameSymbol;
    
    /*
    Symbol used to draw tissot indicatrixes
    */
    private LineSimple tissotSymbol;
    
    /*
    Symbol used to draw voronoyDiagram
    */
    private PolySimple zoneSymbol;
    
    /*
    Symbol used to render distortion coefficients
    */
    private LabelSymbol labelSymbol;
    
    /*
    Projection of the grid
    */
    Projection projection;
    
    // envelope in Coordinates
    Envelope env;
    
    /*
    Grid is visible
    */
    private boolean gridVisible = false;
    
    /*
    Frame is visible
    */
    private boolean frameVisible = false;
    
    /*
    Tissot indicatrices are visible
    */
    private boolean tissotVisible = false;
    
    /*
    Tissot indicatrices are visible
    */
    private boolean zoneVisible = false;
    
    /*
    Text above ellipses is visible
    */
    private boolean textVisible = false;
    
    /*
    Tissot indicatrix scale for transformation
    */
    private double tissotScale = 0.25f;
    
    /*
    Labels scale for transformation
    */
    private double labelsScale = 0.25f;
    
    /*
    Steps in x and y direction (constant)
    */
    private double[] lonValues;
    private double[] latValues;
    
    /*
    Steps for zones
    */
    private double[] lonZoneValues;
    private double[] latZoneValues;
    
    /*
    Parallels and meridians geometry
    */
    GeometryCollection parallels;
    GeometryCollection meridians;
    
    /*
    Zone lines
    */
    GeometryCollection zoneLines;
    
    /*
    Polygons for zones
    */
    GeometryCollection zonePolygons;
    
    /*
    Polygons for zones
    */
    GeometryCollection clippedZonePolygons;
    
    /*
    Tissot ellipses
    */
    GeometryCollection tissotEllipses;
    
    /*
    Tissot ellipses
    */
    GeometryCollection clippedTissotEllipses;
    
    /*
    Centers of tissot ellipses
    */
    GeometryCollection tissotPoints;
    
    /*
    Clipped centers of tissot ellipses
    */
    GeometryCollection clippedTissotPoints;
    
    /*
    The frame of the grid
    */
    Polygon frame;
    
    // whether the grid and tissot indicatrixes should be clipped by frame
    private boolean isClipped = false;
    
    /*
    Parallels and meridians geometry clipped by frame
    */
    GeometryCollection clippedMeridians;
    GeometryCollection clippedParallels;
    
    String labelParameter = "conv";
    
    /**
     * Default graticule constructor
     */
    public Graticule(){
        
        projection = ProjectionFactory.getNamedProjection("Mercator");
        projection.setEllipsoid(Ellipsoid.SPHERE);
        projection.initialize();
        
        frameSymbol = new PolySimple();
        
        frameSymbol.setStrokeColor(Color.GRAY);
        frameSymbol.setStrokeWidth(6.0f);
        
        setGraticule(0, 0, 5, 10);
        
        gridSymbol = new LineSimple();
        
        gridSymbol.setStrokeColor(Color.BLUE);
        gridSymbol.setStrokeWidth(1.0f);
        
        tissotEllipses = FlexProjectorModel.constructTissotIndicatrices(projection, latValues, lonValues, tissotScale);
        tissotSymbol = new LineSimple();
        tissotSymbol.setStrokeColor(Color.RED);
        tissotSymbol.setStrokeWidth(4.0f);
        
        zoneSymbol = new PolySimple();
        zoneSymbol.setStrokeColor(Color.YELLOW);
        zoneSymbol.setStrokeWidth(1.0f);
        
        labelSymbol = new LabelSymbol("Null");
    
    }
    
    /**
     * Sets the envelope by which the grid would be cut
     * @param env envelope
     */
    public void setEnvelope(Envelope env){
        this.env = new Envelope(env);
        generateFrame();
        clipGraticule();
    }
    
    /**
     * Sets graticule parameters and constructs it
     * @param phi0 prime parallel
     * @param lam0 prime meridian
     * @param phiStep parallel step
     * @param lamStep meridian step
     */
    public void setGraticule(double phi0, double lam0, double phiStep, double lamStep){
        /*
        GENERATE GRATICULE  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        */
        ArrayList<Double> latitudes = new ArrayList<>();
        ArrayList<Double> longitudes = new ArrayList<>();
        
        latitudes.add(phi0);
        longitudes.add(lam0);
        
        // Generate northern part
        double phi = phi0 + phiStep;
        while(phi < 90){
            latitudes.add(phi);
            phi += phiStep;
        }
        
        // Generate eastern part
        double lam = lam0 + lamStep;
        while(lam < 180){
            longitudes.add(lam);
            lam += lamStep;
        }
        
        // Generate southern part
        phi = phi0 - phiStep;
        while(phi > -90){
            latitudes.add(phi);
            phi -= phiStep;
        }
        
        // Generate western part
        lam = lam0 - lamStep;
        while(lam > -180){
            longitudes.add(lam);
            lam -= lamStep;
        }
        
        Double[] latResult = new Double[latitudes.size()];
        
        Collections.sort(latitudes);
                
        latValues = ArrayUtils.toPrimitive(latitudes.toArray(latResult));
        
        Double[] lonResult = new Double[longitudes.size()];
        
        Collections.sort(longitudes);
        lonValues = ArrayUtils.toPrimitive(longitudes.toArray(lonResult));
                
        generateGraticule();
        
        
        /*
        GENERATE ZONES >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        */
        
        latitudes = new ArrayList<>();
        longitudes = new ArrayList<>();
        
        phi0 -= 0.5 * phiStep;
        lam0 -= 0.5 * lamStep;
        
        latitudes.add(phi0);
        longitudes.add(lam0);
        
        // Generate northern part
        phi = phi0 + phiStep;
        while(phi < 90){
            latitudes.add(phi);
            phi += phiStep;
        }
        latitudes.add(90.d);
        
        // Generate eastern part
        lam = lam0 + lamStep;
        while(lam < 180){
            longitudes.add(lam);
            lam += lamStep;
        }
        longitudes.add(180.d);
        
        // Generate southern part
        phi = phi0 - phiStep;
        while(phi > -90){
            latitudes.add(phi);
            phi -= phiStep;
        }
        latitudes.add(-90.d);
        
        // Generate western part
        lam = lam0 - lamStep;
        while(lam > -180){
            longitudes.add(lam);
            lam -= lamStep;
        }
        longitudes.add(-180.d);
        
        latResult = new Double[latitudes.size()];
        
        Collections.sort(latitudes);
                
        latZoneValues = ArrayUtils.toPrimitive(latitudes.toArray(latResult));
        
        lonResult = new Double[longitudes.size()];
        
        Collections.sort(longitudes);
        lonZoneValues = ArrayUtils.toPrimitive(longitudes.toArray(lonResult));
                
        generateZones();
        clipGraticule();
    }
    
    public void setLabelsScale(float scale){
        labelSymbol.scaleLabel(scale);
    }
    /**
     * Sets scaling of tissot indicatrix
     * @param scale 
     */
    public void setTissotScale(double scale){
        AffineTransformation at = new AffineTransformation();
        for (int i = 0; i < tissotEllipses.getNumGeometries(); i++) {
            Geometry tissotEllipse = tissotEllipses.getGeometryN(i);
            Point p = tissotEllipse.getCentroid();
            
            // Scale around ellipse centroid
            at.setToTranslation(-p.getX(), -p.getY());
            at.scale(scale/tissotScale, scale/tissotScale);
            tissotEllipse.apply(at);
            
            // Move back
            at.setToTranslation(p.getX(), p.getY());
            tissotEllipse.apply(at);
        }
        
        for (int i = 0; i < clippedTissotEllipses.getNumGeometries(); i++) {
            Geometry tissotEllipse = clippedTissotEllipses.getGeometryN(i);
            Point p = tissotEllipse.getCentroid();
            
            // Scale around ellipse centroid
            at.setToTranslation(-p.getX(), -p.getY());
            at.scale(scale/tissotScale, scale/tissotScale);
            tissotEllipse.apply(at);
            
            // Move back
            at.setToTranslation(p.getX(), p.getY());
            tissotEllipse.apply(at);
        }
        
        tissotScale = scale;
    }
    
    /**
     * Generates frame geometry from the envelope
     */
//    private void generateFrame(){
//        
//        Coordinate[] framePts = new Coordinate[5];
//        
//        // container for the coordinates of a projected point
//        java.awt.geom.Point2D.Double projPt = new java.awt.geom.Point2D.Double();
//        
//        projection.transformRadians(env.getMinX(), env.getMinY(), projPt);
//        framePts[0] = new Coordinate(projPt.x, projPt.y);
//        framePts[4] = new Coordinate(projPt.x, projPt.y);
//        
//        projection.transformRadians(env.getMinX(), env.getMaxY(), projPt);
//        framePts[1] = new Coordinate(projPt.x, projPt.y);
//        
//        projection.transformRadians(env.getMaxX(), env.getMaxY(), projPt);
//        framePts[2] = new Coordinate(projPt.x, projPt.y);
//        
//        projection.transformRadians(env.getMaxX(), env.getMinY(), projPt);
//        framePts[3] = new Coordinate(projPt.x, projPt.y);
//        
//        GeometryFactory gfact = new GeometryFactory();
//        
//        frame = gfact.createPolygon(framePts);
//        
//    }
    
    private void generateFrame(){
        if(env != null){
            Coordinate[] framePts = new Coordinate[5];
        
            framePts[0] = new Coordinate(env.getMinX(), env.getMinY());
            framePts[1] = new Coordinate(env.getMinX(), env.getMaxY());
            framePts[2] = new Coordinate(env.getMaxX(), env.getMaxY());
            framePts[3] = new Coordinate(env.getMaxX(), env.getMinY());
            framePts[4] = new Coordinate(env.getMinX(), env.getMinY());

            GeometryFactory gfact = new GeometryFactory();

            frame = gfact.createPolygon(framePts);
            
            frame.setUserData(false); // no fill
        }
    }
    
    public void setClipped(boolean clipped){
        isClipped = clipped;
        if(clippedParallels == null){
            clipGraticule();
        }
    }
    /**
     * Clips the graticule by frame
     */
    public void clipGraticule(){
        if(frame != null){
            clippedParallels = (GeometryCollection) parallels.intersection(frame);
            clippedMeridians = (GeometryCollection) meridians.intersection(frame);
            clippedTissotPoints = (GeometryCollection) tissotPoints.intersection(frame);
            
            for (int i = 0; i < clippedTissotPoints.getNumGeometries(); i++) {
                Point p = (Point)clippedTissotPoints.getGeometryN(i);
                
                Point2D.Double geoPt = new java.awt.geom.Point2D.Double();
                projection.inverseTransformRadians(new Point2D.Double(p.getX(), p.getY()), geoPt);
                
                if(geoPt.y > MapMath.HALFPI){
                    geoPt.y = MapMath.HALFPI;
                }

                if(geoPt.y < -MapMath.HALFPI){
                    geoPt.y = -MapMath.HALFPI;
                }
            
                ProjectionFactors pf = new ProjectionFactors();
                pf.compute(projection, geoPt.x, geoPt.y, PRECISION);
                p.setUserData(pf); // set scale factor as user data
            }
                    
            
            clippedTissotEllipses = FlexProjectorModel.constructTissotIndicatrices(projection, clippedTissotPoints, tissotScale);
            
            // Clip zones
            Geometry nodedLineStrings = (LineString) zoneLines.getGeometryN(0);
            for(int i = 1; i < zoneLines.getNumGeometries(); i++){
                nodedLineStrings = nodedLineStrings.union(
                        (LineString)zoneLines.getGeometryN(i)
                );
            }
            
            nodedLineStrings = nodedLineStrings.intersection(frame);
            nodedLineStrings = nodedLineStrings.union(frame.getExteriorRing());
            
            Polygonizer polygonizer = new Polygonizer();
            polygonizer.add(nodedLineStrings);
            ArrayList<Geometry> polygons = (ArrayList<Geometry>) polygonizer.getPolygons();
            
            for(Geometry g: polygons){
                g.setUserData(false); // visibility of fill
            }
            
            GeometryFactory gfact = new GeometryFactory();
            Geometry g = gfact.buildGeometry(polygons);
            if(g instanceof GeometryCollection){
                clippedZonePolygons = (GeometryCollection)g;
            } else {
                Geometry[] gs = new Geometry[1];
                gs[0] = g;
                clippedZonePolygons = gfact.createGeometryCollection(gs);
            }
        }
    }
    /**
     * Generates graticule from arrays of lat and lon values.
     * @param latValues
     * @param lonValues 
     */
    private void generateGraticule(){
        
        GeometryFactory gfact = new GeometryFactory();
        
        int nlat = latValues.length;
        int nlon = lonValues.length;
        
        LineString[] lats = new LineString[nlat];
        LineString[] lons = new LineString[nlon];
        
        // container for the coordinates of a projected point
        java.awt.geom.Point2D.Double projPt = new java.awt.geom.Point2D.Double();
        
        // generate parallels
        double minlon = lonValues[0];
        double maxlon = lonValues[nlon-1];
        int npoints = ((int)Math.ceil((maxlon-minlon)/STEP)) + 1;
        
        for (int i = 0; i < nlat; i++) {
            Coordinate coords[] = new Coordinate[npoints];
            double lon = minlon;
            int j = 0;
            
            while(lon < maxlon){
                projection.transform(lon, latValues[i], projPt);
                coords[j++] = new Coordinate(projPt.x,projPt.y);
                lon += STEP;
            }
            // last point
            projection.transform(maxlon, latValues[i], projPt);
            coords[npoints-1] = new Coordinate(projPt.x,projPt.y);
            lats[i] = gfact.createLineString(coords);
        }
        
        parallels = gfact.createGeometryCollection(lats);
        
        // generate meridians
        double minlat = latValues[0];
        double maxlat = latValues[nlat-1];
        npoints = ((int)Math.ceil((maxlat - minlat)/STEP)) + 1;
        
        for (int j = 0; j < nlon; j++) {
            Coordinate coords[] = new Coordinate[npoints];
            double lat = minlat;
            int i = 0;
            
            while(lat < maxlat){
                projection.transform(lonValues[j], lat, projPt);
                coords[i++] = new Coordinate(projPt.x,projPt.y);
                lat += STEP;
            }
            // last point
            projection.transform(lonValues[j], maxlat, projPt);
            coords[npoints-1] = new Coordinate(projPt.x,projPt.y);
            
            lons[j] = gfact.createLineString(coords);
        }
        meridians = gfact.createGeometryCollection(lons);
        
        // Generate tissot points (parallel-meridian intersections)
        Point[] points = new Point[nlat*nlon];
        
        for (int i = 0; i < nlat; i++) {
            for (int j = 0; j < nlon; j++) {
                projection.transform(lonValues[j], latValues[i], projPt);
                
                Point p = gfact.createPoint(new Coordinate(projPt.x, projPt.y));
                
                ProjectionFactors pf = new ProjectionFactors();
                try{
                    pf.compute(projection, Math.toRadians(lonValues[j]), Math.toRadians(latValues[i]), PRECISION);
                    p.setUserData(pf); // set scale factor as user data
                } catch (ProjectionException e){
                    p.setUserData("Null"); // set scale factor as user data
                }
                
                points[(i * nlon) + j] = p;
            }
        }
        tissotPoints = gfact.createGeometryCollection(points);
        
        tissotEllipses = FlexProjectorModel.constructTissotIndicatrices(projection, latValues, lonValues, tissotScale);
    }
    
    private void generateZones() {
        GeometryFactory gfact = new GeometryFactory();
        
        int nlat = latZoneValues.length;
        int nlon = lonZoneValues.length;
        
        LineString[] lines = new LineString[nlat + nlon];
        
        // container for the coordinates of a projected point
        java.awt.geom.Point2D.Double projPt = new java.awt.geom.Point2D.Double();
        
        // generate parallels
        for (int i = 0; i < nlat; i++) {
            Coordinate coords[] = new Coordinate[nlon];
            for (int j = 0; j < nlon; j++) {
                projection.transform(new Point2D.Double(lonZoneValues[j], latZoneValues[i]), projPt);
                coords[j] = new Coordinate(projPt.x,projPt.y);
            }
            lines[i] = gfact.createLineString(coords);
        }
        
        // generate meridians
        
        for (int j = 0; j < nlon; j++) {
            Coordinate coords[] = new Coordinate[nlat];
            for (int i = 0; i < nlat; i++) {
                projection.transform(new Point2D.Double(lonZoneValues[j], latZoneValues[i]), projPt);
                coords[i] = new Coordinate(projPt.x,projPt.y);
            }
            lines[j + nlat] = gfact.createLineString(coords);
        }
        
        zoneLines = gfact.createGeometryCollection(lines);
        
        Geometry nodedLineStrings = (LineString) zoneLines.getGeometryN(0);
        for(int i = 1; i < zoneLines.getNumGeometries(); i++){
            nodedLineStrings = nodedLineStrings.union(
                    (LineString)zoneLines.getGeometryN(i)
            );
        }
        
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(nodedLineStrings);
        ArrayList<Geometry> polygons = (ArrayList<Geometry>) polygonizer.getPolygons();
        
        for(Geometry g: polygons){
            g.setUserData(false); // visibility of fill
        }
        
        zonePolygons = (GeometryCollection) gfact.buildGeometry(polygons);
    }
    
    // Draws all components of graticule: 
    // parallels, meridians, frame, distortion visualizations
    public void draw(GLAutoDrawable drawable) {
        
        if(zoneVisible){
            if(!isClipped){
                for (int i = 0; i < zonePolygons.getNumGeometries(); i++) {
                    zoneSymbol.draw(zonePolygons.getGeometryN(i), drawable);
                }
            } else {
                for (int i = 0; i < clippedZonePolygons.getNumGeometries(); i++) {
                    zoneSymbol.draw(clippedZonePolygons.getGeometryN(i), drawable);
                }
            }
        }
        
        if(gridVisible){
            
           if(!isClipped){
               
                for (int i = 0; i < parallels.getNumGeometries(); i++){
                    gridSymbol.draw(parallels.getGeometryN(i), drawable);
                }

                for (int j = 0; j < meridians.getNumGeometries(); j++){
                    gridSymbol.draw(meridians.getGeometryN(j), drawable);
                }
                
           } else {
               
                for (int i = 0; i < clippedParallels.getNumGeometries(); i++){
                    gridSymbol.draw(clippedParallels.getGeometryN(i), drawable);
                }

                for (int j = 0; j < clippedMeridians.getNumGeometries(); j++){
                    gridSymbol.draw(clippedMeridians.getGeometryN(j), drawable);
                }
           
           }
            
        }
        
        if(frameVisible){
            frameSymbol.draw(frame, drawable);
        }
               
        
        if(tissotVisible){
            if(!isClipped){
                for (int k = 0; k < tissotEllipses.getNumGeometries(); k++) {
                    tissotSymbol.draw(tissotEllipses.getGeometryN(k), drawable);
                }
            } else {
                for (int k = 0; k < clippedTissotEllipses.getNumGeometries(); k++) {
                    tissotSymbol.draw(clippedTissotEllipses.getGeometryN(k), drawable);
                }
            }
            
        }
        
        if(textVisible){
            labelSymbol.setLabelParameter(labelParameter);
            if(!isClipped){
                labelSymbol.draw(tissotPoints, drawable);
            } else {
                labelSymbol.draw(clippedTissotPoints, drawable);
            }
        }
    }
    
    public void setLabelParameter(String s){
        labelParameter = s;
    }
    
    /**
     * Toggles grid visibility. 
     * For now grid and frame are displayed simultaneously
     * @param visible 
     */
    public void setGraticuleVisible(boolean visible){
        this.gridVisible = visible;
        this.frameVisible = visible;
       
    }
    
    /**
     * Toggles tissot indicatrix visibility
     * @param visible 
     */
    public void setTissotVisible(boolean visible){
        this.tissotVisible = visible;
    }
    
    /**
     * Toggles voronoy diagram visibility
     * @param visible 
     */
    public void setVoronoyVisible(boolean visible) {
        this.zoneVisible = visible;
    }
    
    /**
     * Toggles distortions text visibility
     * @param visible 
     */
    public void setTextVisible(boolean visible){
        this.textVisible = visible;
    }
    
    public GeometryCollection getControlPoints(){
        return clippedTissotPoints;
    }
    
    public GeometryCollection getControlZones(){
        return clippedZonePolygons;
    }
    
    public void setProjection(Projection projection){
        if(!projection.equals(this.projection)){
            this.projection = projection;
            generateFrame();
            generateGraticule();
            generateZones();
            clipGraticule();
        }
    }

}
