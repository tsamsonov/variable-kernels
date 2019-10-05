/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.grid;

import org.locationtech.jts.geom.Coordinate;

/**
 * Class constructing Zevenbergen and Thorne's surface
 * @author tsamsonov
 */
public class TrapezoidalSurface extends AbstractSurface{
    
    double A = 2.0;
    double B = 1.0;
    
    double a, b, c, d, e; // distances on ellipsoid
    
    double p, q; // partial derivatives on ellipsoid
        
    float z1, z2, z3, z4, z5, z6, z7, z8, z9;
    
    private double GeodeticDistance(double lat1, double lon1, double lat2, double lon2) {
        return 1.0d;
    }
    
    public TrapezoidalSurface(Geogrid grid, int i, int j){
        
        GridHeader h = grid.getHeader();
        
        if(i>1 && i<h.nrow-1 && j>1 && j<h.ncol-1 && h.ncol>2 && h.nrow>2){
            // upper row
            z1 = grid.getZ(i+1, j-1);
            z2 = grid.getZ(i+1, j);
            z3 = grid.getZ(i+1, j+1);

            // central row
            z4 = grid.getZ(i, j-1);
            z5 = grid.getZ(i, j);
            z6 = grid.getZ(i, j+1);

            // lower row
            z7 = grid.getZ(i-1, j-1);
            z8 = grid.getZ(i-1, j);
            z9 = grid.getZ(i-1, j+1);

            if(Float.isNaN(z1) || Float.isNaN(z2) || Float.isNaN(z3) || 
               Float.isNaN(z4) || Float.isNaN(z5) || Float.isNaN(z6) || 
               Float.isNaN(z7) || Float.isNaN(z8) || Float.isNaN(z9))
            {
                initialized = false;
            } else {
                // Calculate a
                double[] latlon1 = grid.getXYfromIJ(i-1, j-1);
                double[] latlon2 = grid.getXYfromIJ(i-1, j);
                a = GeodeticDistance(latlon1[0], latlon1[1], latlon2[0], latlon2[1]);
                
                // Calculate b
                latlon1 = grid.getXYfromIJ(i, j-1);
                latlon2 = grid.getXYfromIJ(i, j);
                b = GeodeticDistance(latlon1[0], latlon1[1], latlon2[0], latlon2[1]);
                
                // Calculate c
                latlon1 = grid.getXYfromIJ(i+1, j-1);
                latlon2 = grid.getXYfromIJ(i+1, j);
                c = GeodeticDistance(latlon1[0], latlon1[1], latlon2[0], latlon2[1]);
                
                // Calculate d
                latlon1 = grid.getXYfromIJ(i-1, j-1);
                latlon2 = grid.getXYfromIJ(i, j-1);
                d = GeodeticDistance(latlon1[0], latlon1[1], latlon2[0], latlon2[1]);
                
                // Calculate e
                latlon1 = grid.getXYfromIJ(i, j-1);
                latlon2 = grid.getXYfromIJ(i+1, j-1);
                e = GeodeticDistance(latlon1[0], latlon1[1], latlon2[0], latlon2[1]);
                
                
                initialized = true;
            }
        }
    }
    
    /**
     * Returns slope value for constructed surface
     * @return 
     */
    @Override
    public float getSlope(){
//        return (float)Math.atan(zFactor * Math.sqrt(G*G + H*H));

        return 0.0f;
    }

    /**
     * Returns hillshade value for constructed surface
     * @param azimuth
     * @param height
     * @return 
     */
    @Override
    public float getHillshade(double azimuth, double height) {
//        double Lx = Math.cos(height)*Math.cos(azimuth);
//        double Ly = Math.cos(height)*Math.sin(azimuth);
//        double Lz = Math.sin(height);
//        
//        double Nx = G * zFactor;
//        double Ny = H * zFactor;
//        double Nz = 1;
//        
//        double L = Math.sqrt(Lx*Lx + Ly*Ly + Lz*Lz);
//        double N = Math.sqrt(Nx*Nx + Ny*Ny + Nz*Nz);
//        
//        float intensity = (float) ((Lx*Nx + Ly*Ny + Lz*Nz) / (L*N));
//        
//        return intensity;
        return 0.0f;
    }

    @Override
    public float getAspect() {
//        float aspect = (float) (Math.atan2(G, H));
//        if(aspect>0){
//            if(aspect <= 0.5*Math.PI){
//                aspect = (float) (0.5*Math.PI - aspect);
//            } else {
//                aspect = (float) (2.5 * Math.PI - aspect);
//            }
//        } else {
//            aspect = (float) (0.5*Math.PI - aspect);
//        }
//        return aspect;
        return 0.0f;
    }

    @Override
    public float getCurvature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public float getPlanCurvature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public float getProfileCurvature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
