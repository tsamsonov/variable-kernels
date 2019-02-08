/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.grid;

/**
 *
 * @author tsamsonov
 */
public class BilinearSurface extends AbstractSurface{
    GridHeader h;
    
    float a00, a01, a10, a11;
        
    float z00, z01, z10, z11;
    
    public BilinearSurface(Geogrid grid, int i, int j){
        h = grid.getHeader();
        
        if(i>=0 && i<h.nrow-1 && j>=0 && j<h.ncol-1){
            z00 = grid.getZ(i, j);
            z10 = grid.getZ(i+1, j);
            z01 = grid.getZ(i, j+1);
            z11 = grid.getZ(i+1, j+1);
        } else if (i==h.nrow-1 && j>=0 && j<h.ncol-1){
            z00 = grid.getZ(i-1, j);
            z10 = grid.getZ(i, j);
            z01 = grid.getZ(i-1, j+1);
            z11 = grid.getZ(i, j+1);
        } else if (i>=0 && i<h.nrow-1 && j==h.ncol-1){
            z00 = grid.getZ(i, j-1);
            z10 = grid.getZ(i+1, j-1);
            z01 = grid.getZ(i, j);
            z11 = grid.getZ(i+1, j);
        } else if (i==h.nrow-1 && j==h.ncol-1){
            z00 = grid.getZ(i-1, j-1);
            z10 = grid.getZ(i, j-1);
            z01 = grid.getZ(i-1, j);
            z11 = grid.getZ(i, j);
        } else {
            return;
        }
        
        a00 = z00;
        a01 = z10 - z00;
        a10 = z01 - z00;
        a11 = z00 - z10 - z01 + z11;
        
        initialized = true;
    }
    
    public float getSlope(){
        double Fx = a10 + 0.5f * a11;
        double Fy = a01 + 0.5f * a11;
        
        float slope = (float)Math.acos(1/Math.sqrt(Fx*Fx + Fy*Fy +1));
//        float slope = (float)Math.atan(Math.sqrt(Fx*Fx + Fy*Fy));
        return slope;
    }

    @Override
    public float getHillshade(double azimuth, double height) {
        double Lx = Math.cos(height)*Math.cos(azimuth);
        double Ly = Math.cos(height)*Math.sin(azimuth);
        double Lz = Math.sin(height);
        
        double Nx = -(a10 + 0.5f * h.res * a11);
        double Ny = -(a01 + 0.5f * h.res * a11);
        double Nz = 1;
        
        double L = Math.sqrt(Lx*Lx + Ly*Ly + Lz*Lz);
        double N = Math.sqrt(Nx*Nx + Ny*Ny + Nz*Nz);
        
        float intensity = (float) ((Lx*Nx + Ly*Ny + Lz*Nz) / (L*N));
        
        return intensity;
    }

    @Override
    public float getAspect() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
