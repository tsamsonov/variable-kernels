/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.grid;

import autolab.math.Stats.FilterMethod;
import autolab.math.Stats.KernelShape;
import com.jhlabs.map.MapMath;
import com.jhlabs.map.proj.Projection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.AffineTransformation;
import ika.proj.ProjectionFactors;
import java.awt.geom.Point2D;
import javax.swing.BoundedRangeModel;

/**
 *
 * @author tsamsonov
 */
public class GridProcessor {
    
    public enum ProcessingType{
        SIMPLE,
        AFFINE,
        DIRECT
    }
    
    ProcessingType processingType;
    
    private static final double EPS = 1e-4;
    
    /*
    Method used for filtering
    */
    FilterMethod filterMethod;
    
    /*
    Kernel shape used for filtering
    */
    KernelShape kernelShape;
    
    /*
    Kernel size (in meters) used for filtering
    */
    double kernelSize;
      
    /*
    Source grid
    */
    Geogrid grid;
    
    /*
    Result grid
    */
    Geogrid grid2;
    
    /*
    Grid projection
    */
    Projection projection;
    
    /*
    Projection factors that are calculated at each point
    */
    ProjectionFactors[] controlFactors;
    
    /*
    Zones that will be used for applying kernel properties
    */
    GeometryCollection controlZones;
    
    /*
    Kernel focalKernels that are used for focal statistics
    */
    Coordinate[][] focalKernels;
    
    /*
    Kernel focalKernels that are used for surface derivatives (affine transform)
    */
    Coordinate[][] surfaceKernelsAffine;
    
    /*
    Kernel focalKernels that are used for surface derivatives (direct transform)
    */
    Coordinate[][] surfaceKernelsDirect;
    
    /*
    Kernel used for fixed filtering
    */
    Coordinate[] fixedKernel;
    
    /*
    if variable kernel shape should be applied
    */
    boolean isVariable;
    
    /*
    Mean and MRSQ values
    */
    double statistics[];
    
    /*
    Mean and MRSQ values corrected
    */
    double correctedStatistics[];
    
    /*
    Hillshade azimuth and height
    */
    private float hillAzimuth = (float) (7. * Math.PI / 4.);
    private float hillHeight = (float) (Math.PI / 4.);
    
    /*
    Z-factor used for calculations. Every Z value is multiplied by zFactor
    */
    private float zFactor = 1.0f;
    
    /*
    D-factor used for calculations. Every distance is multiplied by dFactor
    */
    private float dFactor = 1.0f;
            
    public GridProcessor(Geogrid g, Projection p){
        projection = p;
        grid = g;
        filterMethod = FilterMethod.MEAN;
        kernelShape = KernelShape.SQUARE;
        processingType = ProcessingType.SIMPLE;
        kernelSize = 10000;
        isVariable = false;
        calculateFixedKernel();
    }
    
    public boolean getVariable(){
        return isVariable;
    }
    
    public void execute(BoundedRangeModel progress, Point ptIndicator){
        switch(filterMethod){
            case SLOPE:
            case HILLSHADE:
            case ASPECT:
            case PLANCURV:
            case PROFCURV:
            case CURV:
                surfaceFilter(progress, ptIndicator);
                break;
            case MEAN:
                meanFilter(progress, ptIndicator);
                break;
            case DISTAREAL:
                distArealFilter(progress, ptIndicator);
                break;
            case VECTOR: return;
            case SCALAR: return;
            case MIN: return;
            case MAX: return;
            case MEDIAN: return;
            case MODE: return;
            case STDEV: return;
            case RANGE: return;
            case GAUSSIAN: return;
            case ROBERTS: return;
            case SOBEL: return;
            case PREWITT: return;
            case CANNY: return ;
            case MANUAL: return;
            default: throw new IllegalArgumentException(); 
        }
        resetZonesIndication();
    }
    
    /**
     * Surface filtering based on Zevenbergen and Thorne (1987) equation.
     * Z = A(x^2)(y^2) + B(x^2)y + Cx(y^2) + Dx^2 + Ey^2 + Fxy + Gx + Hy + I
     * 
     * Bilinear equation is used for cells along the border of the grid:
     * Z = a00 + (a10)x + (a01)y + (a11)xy
     * 
     * @param progress
     * @param ptIndicator 
     */
    private void surfaceFilter(BoundedRangeModel progress, Point ptIndicator){
        
        GeometryFactory gfact = new GeometryFactory();
        
        GridHeader h = grid.getHeader();
        
        AbstractSurface surface = null;
        
        float[][] matrix = new float[h.nrow][h.ncol];
        
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        
        int nzone = 0;
        for (int i = 0; i < h.nrow; i++) {
//            System.out.println(String.valueOf(i)+" "+String.valueOf(nzone));
            for (int j = 0; j < h.ncol; j++) {
                float value = Float.NaN;
                
                if(Float.isNaN(grid.getZ(i, j))){
                    matrix[i][j] = Float.NaN;
                    continue;
                }
                
                double[] coords = grid.getXYfromIJ(i, j);

                ptIndicator.getCoordinateSequence().setOrdinate(0, 0, coords[0]);
                ptIndicator.getCoordinateSequence().setOrdinate(0, 1, coords[1]);

                if(coords == null){
                    matrix[i][j] = Float.NaN;
                }
                
                if(processingType == ProcessingType.SIMPLE){
                    surface = new ZevenbergenSurface(grid, i, j);
                } else{
                    
                    Point p = gfact.createPoint(new Coordinate(coords[0], coords[1]));

                    // Find kernel
                    nzone = 0;
                    while (nzone < controlZones.getNumGeometries()) {
                        Geometry zone = controlZones.getGeometryN(nzone);
                        if (p.intersects(zone)){
                            // select kernel for current zone

                            Coordinate[] kernel;
                            if(processingType == ProcessingType.AFFINE)
                                kernel = surfaceKernelsAffine[nzone];
                            else 
                                kernel = surfaceKernelsDirect[nzone];

                            if (kernel == null){
                                matrix[i][j] = Float.NaN;
                                surface = null;
                            } else {
                                surface = new ZevenbergenSurface(grid, i, j, kernel);
                                zone.setUserData(true);
                            }
                            break;
                        }
                        nzone++;
                    }

                    if(nzone == controlZones.getNumGeometries()){
                        matrix[i][j] = Float.NaN;
                        surface = null;
                    }
                }
                
                if(surface != null && surface.initialized){
                    surface.setZfactor(zFactor);
                    switch(filterMethod){
                        case SLOPE:
                            value = (float)Math.toDegrees(surface.getSlope());
                            break;
                        case ASPECT:
                            value = (float)Math.toDegrees(surface.getAspect());
                            break;
                        case CURV:
                            value = (float)Math.toDegrees(surface.getCurvature());
                            break;
                        case PLANCURV:
                            value = (float)Math.toDegrees(surface.getPlanCurvature());
                            break;
                        case PROFCURV:
                            value = (float)Math.toDegrees(surface.getProfileCurvature());
                            break;
                        case HILLSHADE:
                            value = (float)Math.toDegrees(surface.getHillshade(hillAzimuth, hillHeight));
                            break;
                    }
                }
                
                matrix[i][j] = value;
                
                if(!Float.isNaN(value)){
                    if(value > max){
                        max = value;
                    }

                    if(value < min){
                        min = value;
                    }
                }
                
                progress.setValue(i*100/h.nrow);
            }
        }
        
        GridHeader h2 = new GridHeader(h);
        
        h2.zmax = max;
        h2.zmin = min;
        
        grid2 = new Geogrid(matrix, h2);
        
    }
    
    private void resetZonesIndication(){
        if(controlZones != null){
            for(int i = 0; i < controlZones.getNumGeometries(); i++){
                controlZones.getGeometryN(i).setUserData(false);
            }
        }
    }
    
    private void distArealFilter(BoundedRangeModel progress, Point ptIndicator){
        GridHeader h = grid.getHeader();
        
        float[][] matrix = new float[h.nrow][h.ncol];
        
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int i = 0; i < h.nrow; i++) {
            for (int j = 0; j < h.ncol; j++) {

                float z = grid.getZ(i, j);
                if(z == h.noData || Float.isNaN(z)){
                    matrix[i][j] = z;
                    continue;
                }
                
                double[] coords = grid.getXYfromIJ(i, j);
                
                ptIndicator.getCoordinateSequence().setOrdinate(0, 0, coords[0]);
                ptIndicator.getCoordinateSequence().setOrdinate(0, 1, coords[1]);
                
                Point2D.Double geoPt = new java.awt.geom.Point2D.Double();
                projection.inverseTransformRadians(new Point2D.Double(coords[0], coords[1]), geoPt);
                
                if(geoPt.y > MapMath.HALFPI){
                    geoPt.y = MapMath.HALFPI;
                }

                if(geoPt.y < -MapMath.HALFPI){
                    geoPt.y = -MapMath.HALFPI;
                }
            
                ProjectionFactors pf = new ProjectionFactors();
                pf.compute(projection, geoPt.x, geoPt.y, EPS);
                
                matrix[i][j] = (float) pf.s;
                
                if(z != h.noData && !Float.isNaN(z)){
                    if (pf.s > max)
                        max = (float)pf.s;
                    if (pf.s < min)
                        min = (float)pf.s;
                }
                
                progress.setValue(i*100/h.nrow);
            }
        }
        
        GridHeader h2 = new GridHeader(h);
        
        h2.zmax = max;
        h2.zmin = min;
        
        grid2 = new Geogrid(matrix, h2);
    }
    
    private void meanFilter(BoundedRangeModel progress, Point ptIndicator){
        GridHeader h = grid.getHeader();
        
        float[][] matrix = new float[h.nrow][h.ncol];
        
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int i = 0; i < h.nrow; i++) {
            for (int j = 0; j < h.ncol; j++) {

                float z0 = grid.getZ(i, j);
                if(z0 == h.noData || Float.isNaN(z0)){
                    matrix[i][j] = z0;
                    continue;
                }
                
                double[] coords = grid.getXYfromIJ(i, j);
                
                ptIndicator.getCoordinateSequence().setOrdinate(0, 0, coords[0]);
                ptIndicator.getCoordinateSequence().setOrdinate(0, 1, coords[1]);
                
                float z = meanFilter(i,j);

                matrix[i][j] = z;

                if(z != h.noData && !Float.isNaN(z)){
                    if (z > max)
                        max = z;
                    if (z < min)
                        min = z;
                }

            }
            progress.setValue(i*100/h.nrow);
        }
        GridHeader h2 = new GridHeader(h);
        grid2 = new Geogrid(matrix, h2);
        
    }
    
    // mean filtering at current grid cell
    private float meanFilter(int i, int j){
        
        GeometryFactory gfact = new GeometryFactory();
        GridHeader h = grid.getHeader();
        
        Coordinate[] kernel = fixedKernel;
        
        if(isVariable){
            // find kernel
            
            double[] coords = grid.getXYfromIJ(i, j);

            if(coords == null){
                return Float.NaN;
            }

            Point p = gfact.createPoint(new Coordinate(coords[0], coords[1]));

            // Find kernel
            int nzone = 0;
            int nkernel = 0;
            while (nzone < controlZones.getNumGeometries()) {
                Geometry zone = controlZones.getGeometryN(nzone);
                if (p.intersects(zone)){
                    // select kernel for current zone
                    kernel = focalKernels[nzone];
                    zone.setUserData(true);
                    break;
                }
                nzone++;
            }
            
            if(nzone == controlZones.getNumGeometries()){
                return grid.getZ(i, j);
            }
        }
        
        float sum = 0.f;
        
        int count = 0;
        
        if(kernel == null){
            return grid.getZ(i, j);
        }
        
        for (Coordinate shift : kernel) {
            if(shift == null){
                return grid.getZ(i, j);
            }
            int ik = i + (int)Math.round(shift.x);
            int jk = j + (int)Math.round(shift.y);
            
            if(ik < 0 || jk < 0 || ik >= h.nrow || jk >= h.ncol)
                continue;
                
            float z = grid.getZ(ik, jk);

            if(z == h.noData || Float.isNaN(z))
                continue;
                
            sum += z;
            
            count++;
        }
        
        return sum/count;
    }

    /**
     * Sets control zones to manage variable kernel shapes
     * @param zones 
     */
    public void setControlZones(GeometryCollection zones){
        controlZones = zones;
        
        controlFactors = new ProjectionFactors[controlZones.getNumGeometries()];
        
        for (int i = 0; i < controlFactors.length; i++) {
            
            Point centre = controlZones.getGeometryN(i).getCentroid();
            
            Point2D.Double geoPt = new java.awt.geom.Point2D.Double();
            projection.inverseTransformRadians(new Point2D.Double(centre.getX(), centre.getY()), geoPt);
            
            controlFactors[i] = new ProjectionFactors();
            
            if(geoPt.y > projection.getMaxLatitude() || geoPt.y >= MapMath.HALFPI){
                controlFactors[i] = null;
            } else {
                controlFactors[i].compute(projection, geoPt.x, geoPt.y, EPS);
            }
        }
        
        focalKernels = new Coordinate[controlZones.getNumGeometries()][];
        surfaceKernelsAffine = new Coordinate[controlZones.getNumGeometries()][];
        surfaceKernelsDirect = new Coordinate[controlZones.getNumGeometries()][];
        
        for (int i = 0; i < focalKernels.length; i++) {
            if (controlFactors[i] != null){
                focalKernels[i] = calculateVariableStatKernel(controlFactors[i]);
                surfaceKernelsAffine[i] = calculateVariableSurfaceKernelAffine(controlFactors[i]);
                surfaceKernelsDirect[i] = calculateVariableSurfaceKernelDirect(controlFactors[i]);
            } else {
                focalKernels[i] = null;
                surfaceKernelsAffine[i] = null;
                surfaceKernelsDirect[i] = null;
            }
            
        }
    }
    
    /**
     * Calculates kernel for fixed-kernel filterings
     * @return 
     */
    private void calculateFixedKernel(){
        int width = (int)(kernelSize / grid.getHeader().res);
        if(width % 2 == 0){
            width++;
        }
        
        int length = (int)(kernelSize / grid.getHeader().res);
        if(length%2 == 0){
            length++;
        }
        
        fixedKernel = new Coordinate[width*length];
        int k = 0;
        for (int i = - width/2; i <= width/2; i++) {
            for (int j = -length/2; j <= length/2; j++) {
                fixedKernel[k++] = new Coordinate(i,j);
            }
        }
    }
    
    /**
     * Calculates transformed kernel for focal statistics filtering
     * @param factors
     * @return 
     */
    public Coordinate[] calculateVariableStatKernel(ProjectionFactors factors){
        
        GeometryFactory gfact = new GeometryFactory();
        
        double lambdaScale = dFactor * factors.h;
        
        double phiScale = dFactor * factors.k;
        
        int phiPixels = (int)(phiScale * kernelSize / (grid.getHeader().res * projection.getScaleFactor()));
        if(phiPixels % 2 == 0){
            phiPixels++;
        }
        
        int lambdaPixels = (int)(lambdaScale * kernelSize / (grid.getHeader().res * projection.getScaleFactor()));
        if(lambdaPixels%2 == 0){
            lambdaPixels++;
        }
        
        Coordinate[] coords = new Coordinate[phiPixels*lambdaPixels];
       
        int k = 0;
        for (int i = - phiPixels/2; i <= phiPixels/2; i++) {
            for (int j = -lambdaPixels/2; j <= lambdaPixels/2; j++) {
                coords[k++] = new Coordinate(i,j);
            }
        }
        
        MultiPoint kernel = gfact.createMultiPoint(coords);
        
        Coordinate c11 = new Coordinate(0,0);
        Coordinate c12 = new Coordinate(0,0);
        
        Coordinate c21 = new Coordinate(0,lambdaScale);
        Coordinate c22 = new Coordinate(- lambdaScale * Math.sin(factors.conv),
                                          lambdaScale * Math.cos(factors.conv));
        Coordinate c31 = new Coordinate(phiScale,0);
        Coordinate c32 = new Coordinate(phiScale * Math.cos(factors.conv2),
                                        phiScale * Math.sin(factors.conv2));
        
        AffineTransformation at = new AffineTransformation(c11, c12, c21, c22, c31, c32);
        
        kernel = (MultiPoint)at.transform(kernel);
        
        return kernel.getCoordinates();
        
    }
    
    /**
     * Directly constructs 3x3 kernel for surface derivatives filtering
     * @param factors
     * @return 
     */
    public Coordinate[] calculateVariableSurfaceKernelDirect(ProjectionFactors factors){
        GeometryFactory gfact = new GeometryFactory();
        GridHeader h = grid.getHeader();
        Coordinate[] coords = new Coordinate[9];
        
        double lambdaScale = dFactor * factors.h;
        
        double phiScale = dFactor * factors.k;
        
//      NW---N---NE   0----1----2
//      |    |    |   |    |    |
//      W----0----E   3----4----5
//      |    |    |   |    |    |
//      SW---S---SE   6----7----8
        
        // Central point
        coords[4] = new Coordinate(0,0);
        
        // N S E W points
        if(factors.conv < EPS){
            coords[1] = new Coordinate(0,h.res * lambdaScale);
            coords[7] = new Coordinate(0,-h.res * lambdaScale);
        } else {
            coords[1] = new Coordinate(- h.res * lambdaScale * Math.sin(factors.conv),
                                     h.res * lambdaScale * Math.cos(factors.conv));
            coords[7] = new Coordinate(  h.res * lambdaScale * Math.sin(factors.conv),
                                   - h.res * lambdaScale * Math.cos(factors.conv));
        }
        
        if(factors.conv2 < EPS){
            coords[3] = new Coordinate(-h.res * phiScale, 0);
            coords[5] = new Coordinate(h.res * phiScale, 0);
        } else {
            coords[3] = new Coordinate(- h.res * phiScale * Math.cos(factors.conv2),
                                   - h.res * phiScale * Math.sin(factors.conv2));
            coords[5] = new Coordinate(h.res * phiScale * Math.cos(factors.conv2),
                                       h.res * phiScale * Math.sin(factors.conv2));
        }
        
        // NW NE SW SE points
        double alpha, mu, dist;
        double sintheta = Math.sin(factors.thetap);
        double costheta = Math.cos(factors.thetap);
        
        /*
        North-East
        */
        alpha = Math.atan(
                factors.k * sintheta/
               (factors.h + factors.k * costheta)
        );
        
        mu = Math.sqrt(
                (Math.pow(factors.h, 2) + Math.pow(factors.k, 2))*0.5 +
                 factors.h * factors.k * costheta
        );
        
        dist = h.res * mu * Math.sqrt(2.d);
        
        coords[2] = new Coordinate(dist * Math.sin(alpha - factors.conv),
                                   dist * Math.cos(alpha - factors.conv));
        
        /*
        South-East
        */
        alpha = Math.atan(
                -factors.k * sintheta/
               (factors.h - factors.k * costheta)
        ) + Math.PI;
        
        mu = Math.sqrt(
                (Math.pow(factors.h, 2) + Math.pow(factors.k, 2))*0.5 -
                 factors.h * factors.k * costheta
        );
        
        dist = h.res * mu * Math.sqrt(2.d);
        
        coords[8] = new Coordinate(dist * Math.sin(alpha - factors.conv),
                                   dist * Math.cos(alpha - factors.conv));
        
        /*
        South-West
        */
        alpha = Math.atan(
                factors.k * sintheta/
               (factors.h + factors.k * costheta)
        ) + Math.PI;
        
        mu = Math.sqrt(
                (Math.pow(factors.h, 2) + Math.pow(factors.k, 2))*0.5 +
                 factors.h * factors.k * costheta
        );
        
        dist = h.res * mu * Math.sqrt(2.d);
        
        coords[6] = new Coordinate(dist * Math.sin(alpha - factors.conv),
                                   dist * Math.cos(alpha - factors.conv));
        
        /*
        North-West
        */
        alpha = Math.atan(
                -factors.k * sintheta/
               (factors.h - factors.k * costheta)
        );
        
        mu = Math.sqrt(
                (Math.pow(factors.h, 2) + Math.pow(factors.k, 2))*0.5 -
                 factors.h * factors.k * costheta
        );
        
        dist = h.res * mu * Math.sqrt(2.d);
        
        coords[0] = new Coordinate(dist * Math.sin(alpha - factors.conv),
                                   dist * Math.cos(alpha - factors.conv));
        
        return coords;
    }
    /**
     * Applies affine transform to 3x3 kernel for surface derivatives filtering
     * @param factors
     * @return 
     */
    public Coordinate[] calculateVariableSurfaceKernelAffine(ProjectionFactors factors){
        GeometryFactory gfact = new GeometryFactory();
        GridHeader h = grid.getHeader();
        
        // Standard 3x3 kernel
        Coordinate[] coords = new Coordinate[9];
        int k = 0;
        for (double y = h.res; y >= -h.res; y-=h.res){
            for (double x = -h.res; x <= h.res; x+=h.res) {
                coords[k++] = new Coordinate(x,y);
            }
        }
        
        double lambdaScale = dFactor * factors.h;
        
        double phiScale = dFactor * factors.k;
        
        MultiPoint kernel = gfact.createMultiPoint(coords);
        
        Coordinate c11 = new Coordinate(0,0);
        Coordinate c12 = new Coordinate(0,0);
        
        Coordinate c21 = new Coordinate(0,1);
        Coordinate c22 = new Coordinate(- lambdaScale * Math.sin(factors.conv),
                                          lambdaScale * Math.cos(factors.conv));
        Coordinate c31 = new Coordinate(1,0);
        Coordinate c32 = new Coordinate(phiScale * Math.cos(factors.conv2),
                                        phiScale * Math.sin(factors.conv2));
        
        AffineTransformation at = new AffineTransformation(c11, c12, c21, c22, c31, c32);
        
        kernel = (MultiPoint)at.transform(kernel);
        return kernel.getCoordinates();
    }
    
    public void setFilterMethod(FilterMethod fm){
        filterMethod = fm;
    }
    
    public void setHillShade(float azimuth, float height){
        hillAzimuth = azimuth;
        hillHeight = height;
    }
    
    public void setKernelSize(double size){
        kernelSize = size;
        calculateFixedKernel();
    }
    
    public void setKernelShape(KernelShape shape){
        kernelShape = shape;
    }
    
    public Geogrid getResult(){
        return grid2;
    }
    
    /**
     * Returns grid envelope in projected coordinates
     * @return 
     */
    public Envelope getProjectedEnvelope(){
        GridHeader h = grid.getHeader();
        Envelope env = new Envelope(h.xmin, h.xmax, h.ymin, h.ymax);
        return env;
    }
    
    /**
     * Returns grid envelope in geographic coordinates
     * @return 
     */
    public Envelope getGeographicEnvelope(){
        GridHeader h = grid.getHeader();
        
        // lower left point
        java.awt.geom.Point2D.Double llPoint = new java.awt.geom.Point2D.Double();
        
        projection.inverseTransformRadians(new java.awt.geom.Point2D.Double(h.xmin, h.ymin), llPoint);
        
        // upper right point
        java.awt.geom.Point2D.Double urPoint = new java.awt.geom.Point2D.Double();
        projection.inverseTransformRadians(new java.awt.geom.Point2D.Double(h.xmax, h.ymax), urPoint);
        
        Envelope env = new Envelope(llPoint.x, urPoint.x, llPoint.y, urPoint.y);
        
        return env;
    }
    
    public Envelope getEnvelope(){
        GridHeader h = grid.getHeader();
        return new Envelope(h.xmin, h.xmax, h.ymin, h.ymax);
    }
    
    public void setType(ProcessingType pt){
        processingType = pt;
        isVariable = (pt != ProcessingType.SIMPLE); 
    }

    public void setGrid(Geogrid grid) {
        this.grid = grid;
    }
    
    public void setZfactor(float zf){
        zFactor = zf;
    }

    public void setDfactor(float df) {
        dFactor = df;
    }
}
