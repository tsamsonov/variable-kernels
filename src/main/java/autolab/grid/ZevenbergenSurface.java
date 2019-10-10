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
public class ZevenbergenSurface extends AbstractSurface{
    
    double A, B, C, D, E, F, G, H, I;
        
    float z1, z2, z3, z4, z5, z6, z7, z8, z9;
    
    public ZevenbergenSurface(Geogrid grid, int i, int j){
        
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
                A = ((z1 + z3 + z7 + z9)/4.f - (z2 + z4 + z6 + z8)/2.f + z5) / Math.pow(h.res,4);
                B = ((z1 + z3 - z7 - z9)/4.f - (z2 - z8)/2.f) / Math.pow(h.res,3);
                C = ((-z1 + z3 - z7 + z9)/4.f + (z2 - z6)/2.f) / Math.pow(h.res,3);
                D = ((z4 + z6)/2.f - z5) / Math.pow(h.res, 2);
                E = ((z2 + z8)/2.f - z5) / Math.pow(h.res, 2);
                F = (-z1 + z3 + z7 - z9) / (4.f * Math.pow(h.res, 2));
                G = (-z4 + z6) / (2.f * h.res);
                H = (z2 - z8) / (2.f * h.res);
                I = z5;

                initialized = true;
            }
        }
    }
    
    public ZevenbergenSurface(Geogrid grid, int i, int j, Coordinate[] kernel, double scale){
        
        GridHeader h = grid.getHeader();
        
        double[] coords = grid.getXYfromIJ(i, j);
        
        if(i>1 && i<h.nrow-1 && j>1 && j<h.ncol-1 && h.ncol>2 && h.nrow>2){
            // upper row
            z1 = grid.getZxy(coords[0] + kernel[0].x, coords[1] + kernel[0].y);
            z2 = grid.getZxy(coords[0] + kernel[1].x, coords[1] + kernel[1].y);
            z3 = grid.getZxy(coords[0] + kernel[2].x, coords[1] + kernel[2].y);

            // central row
            z4 = grid.getZxy(coords[0] + kernel[3].x, coords[1] + kernel[3].y);
            z5 = grid.getZxy(coords[0] + kernel[4].x, coords[1] + kernel[4].y);
            z6 = grid.getZxy(coords[0] + kernel[5].x, coords[1] + kernel[5].y);

            // lower row
            z7 = grid.getZxy(coords[0] + kernel[6].x, coords[1] + kernel[6].y);
            z8 = grid.getZxy(coords[0] + kernel[7].x, coords[1] + kernel[7].y);
            z9 = grid.getZxy(coords[0] + kernel[8].x, coords[1] + kernel[8].y);
            
            if(Float.isNaN(z1) || Float.isNaN(z2) || Float.isNaN(z3) || 
               Float.isNaN(z4) || Float.isNaN(z5) || Float.isNaN(z6) || 
               Float.isNaN(z7) || Float.isNaN(z8) || Float.isNaN(z9))
            {
                initialized = false;
            } else {
                A = ((z1 + z3 + z7 + z9)/4.f - (z2 + z4 + z6 + z8)/2.f + z5) / Math.pow(h.res,4);
                B = ((z1 + z3 - z7 - z9)/4.f - (z2 - z8)/2.f) / Math.pow(h.res,3);
                C = ((-z1 + z3 - z7 + z9)/4.f + (z2 - z6)/2.f) / Math.pow(h.res,3);
                D = ((z4 + z6)/2.f - z5) / Math.pow(h.res, 2);
                E = ((z2 + z8)/2.f - z5) / Math.pow(h.res, 2);
                F = (-z1 + z3 + z7 - z9) / (4 * Math.pow(h.res, 2));
                G = (double)(-z4 + z6) * scale / (2.f * h.res);
                H = (double)(z2 - z8) * scale / (2.f * h.res);
                I = z5;

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
        return (float)Math.atan(zFactor * Math.sqrt(G*G + H*H));
    }

    /**
     * Returns hillshade value for constructed surface
     * @param azimuth
     * @param height
     * @return 
     */
    @Override
    public float getHillshade(double azimuth, double height) {
        double Lx = Math.cos(height)*Math.cos(azimuth);
        double Ly = Math.cos(height)*Math.sin(azimuth);
        double Lz = Math.sin(height);
        
        double Nx = G * zFactor;
        double Ny = H * zFactor;
        double Nz = 1;
        
        double L = Math.sqrt(Lx*Lx + Ly*Ly + Lz*Lz);
        double N = Math.sqrt(Nx*Nx + Ny*Ny + Nz*Nz);
        
        float intensity = (float) ((Lx*Nx + Ly*Ny + Lz*Nz) / (L*N));
        
        return intensity;
    }

    @Override
    public float getAspect() {
        float aspect = (float) (Math.atan2(G, H));
        if(aspect>0){
            if(aspect <= 0.5*Math.PI){
                aspect = (float) (0.5*Math.PI - aspect);
            } else {
                aspect = (float) (2.5 * Math.PI - aspect);
            }
        } else {
            aspect = (float) (0.5*Math.PI - aspect);
        }
        return aspect;
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
