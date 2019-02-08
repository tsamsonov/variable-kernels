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
public class Geogrid {
    
    /*
    Matrix of values
    */
    private float Z[][];
    
    /*
    Grid header
    */
    private GridHeader h;
    
    /*
    Mean value
    */
    private double mean;
    
    /*
    Mean root square value
    */
    private double mrsq;
    
    /*
    Mean value corrected according to projection distortions
    */
    private double mean2;
    
    /*
    Mean root square value corrected according to projection distortions
    */
    private double mrsq2;
    
    
    public Geogrid(float[][] inputGrid, GridHeader inputHeader){
        h = inputHeader;
        Z = inputGrid;
    }
    
    public Geogrid(){
        
    }
    
    public GridHeader getHeader(){
        return h;
    }
    
    /**
     * Calculates grid mean value, corrected by simple average 
     */
    public void calculateStatistics(){
        /*
        Calculate mean from the first point
        */
        double numerator = 0;
        double numerator2 = 0; // numerator for squared value
        double denumerator = 0;

        int n = 1;
        
        for (int i = 0; i < h.nrow; i++) {
            for (int j = 0; j < h.ncol; j++) {
                double z = getZ(i, j);

                if (Double.isNaN(z)){
                    continue;
                }

                numerator += z;
                
                numerator2 += z*z;

                denumerator++;

            }
        }
        
        mean = numerator / denumerator;
        double var = (numerator2 / denumerator) - (mean * mean);
        mrsq = Math.sqrt(var);

    }
    
    /**
     * Calculates grid mean value, as average of pixels weighted by 
     * areal scale factor
     * @param weights the matrix of the same size as this grid 
     */
    public void calculateWeightedStatistics(Geogrid weights){
        
        /*
        Calculate mean from the first point
        */
        double numerator = 0;
        double numerator2 = 0;
        double denumerator = 0;

        int n = 1;

        for (int i = 0; i < h.nrow; i++) {
            for (int j = 0; j < h.ncol; j++) {
                double z = getZ(i, j);

                if (Double.isNaN(z)){
                    continue;
                }
                
                double w = weights.getZ(i, j);

                numerator += z / w;

                numerator2 += z * z / w;

                denumerator += 1 / w;

            }
        } 
            
        mean2 = numerator / denumerator;
        double var = (numerator2 / denumerator) - (mean2 * mean2);
        mrsq2 = Math.sqrt(var);

    }
    
    public float getZ(int i, int j){
        if(i<0 || j<0 || i>=h.nrow || j>=h.ncol)
            return Float.NaN;
        else
            return Z[i][j];
    }
    
    /**
     * Function returns XY coordinates of the (i,j) cell
     * @param i the row
     * @param j the column
     * @return 
     */
    public double[] getXYfromIJ(int i, int j){
        double coords[] = new double[2];
        
        if(i<0 || i > h.nrow || j<0 || j > h.ncol){
            return null;
        }
        
        coords[0] = j*h.res + h.xmin;
        coords[1] = i*h.res + h.ymin;
        
        return coords;
    }
    
    /**
     * Returns (i,j) of the cell in which (x,y) coordinates fall to
     * @param x
     * @param y
     * @return 
     */
    public int[] getIJfromXY(double x, double y){
        int coords[] = new int[2];
        
        if(x<=h.xmax && x >= h.xmin && y <= h.ymax && y >= h.ymin){
            double dx = x-h.xmin;
            double dy = y-h.ymin;
            
            if (x==h.xmax){
                coords[1] = h.ncol-1;
            } else {
                coords[1] = (int) Math.floor(dx/h.res);
            }
            
            if (y==h.ymax){
                coords[0] = h.nrow-1;
            } else {
                coords[0] = (int) Math.floor(dy/h.res);
            }
            return coords;
        }
        else return null;
    }
    
    /**
     * Function returns Z value interpolated at (x,y) using bilinear function
     * @param x coordinate
     * @param y coordinate
     * @return 
     */
    public float getZxy(double x, double y){
        int i,j;
        int[] ij = getIJfromXY(x,y);
        if(ij == null)
            return Float.NaN;
        i=ij[0];
        j=ij[1];
        if(i >= h.nrow-1 || j >= h.ncol-1)
            return Float.NaN;
        if (ij != null){
            double[] xy1 = getXYfromIJ(i,j);
            if(xy1 == null){
                return Float.NaN;
            }
            double dx = (x-xy1[0])/h.res;
            double dy = (y-xy1[1])/h.res;
            double Ax = (Z[i][j+1] - Z[i][j]);
            double Ay = (Z[i+1][j] - Z[i][j]);
            double Axy = Z[i][j] + Z[i+1][j+1] - Z[i+1][j] - Z[i][j+1];
            double Z0 = Z[i][j] + Ax*dx + Ay*dy + Axy*dx*dy;
            return (float)Z0;
        } else {
            return Float.NaN;
        }
        
//        int[] ij = getIJfromXY(x,y);
//        
//        if(ij == null)
//            return Float.NaN;
//            
//        int i = ij[0];
//        int j = ij[1];
//        if(i>1 && i<h.nrow-1 && j>1 && j<h.ncol-1 && h.ncol>2 && h.nrow>2){
//            // upper row
//            float z1 = Z[i-1][j-1];
//            float z2 = Z[i][j-1];
//            float z3 = Z[i+1][j-1];
//
//            // central row
//            float z4 = Z[i-1][j];
//            float z5 = Z[i][j];
//            float z6 = Z[i+1][j];
//
//            // lower row
//            float z7 = Z[i-1][j+1];
//            float z8 = Z[i][j+1];
//            float z9 = Z[i+1][j+1];
//
//            if(Float.isNaN(z1) || Float.isNaN(z2) || Float.isNaN(z3) || 
//               Float.isNaN(z4) || Float.isNaN(z5) || Float.isNaN(z6) || 
//               Float.isNaN(z7) || Float.isNaN(z8) || Float.isNaN(z9))
//            {
//                return Float.NaN;
//            } else {
//                double A = ((z1 + z3 + z7 + z9)/4.f - (z2 + z4 + z6 + z8)/2.f + z5) / Math.pow(h.res,4);
//                double B = ((z1 + z3 - z7 - z9)/4.f - (z2 - z8)/2.f) / Math.pow(h.res,3);
//                double C = ((-z1 + z3 - z7 + z9)/4.f + (z2 - z6)/2.f) / Math.pow(h.res,3);
//                double D = ((z4 + z6)/2.f - z5) / Math.pow(h.res, 2);
//                double E = ((z2 + z8)/2.f - z5) / Math.pow(h.res, 2);
//                double F = (-z1 + z3 + z7 - z9) / (4.f * Math.pow(h.res, 2));
//                double G = (-z4 + z6) / (2.f * h.res);
//                double H = (z2 - z8) / (2.f * h.res);
//                double I = z5;
//                
//                double[] xy1 = getXYfromIJ(i,j);
//                if(xy1 == null){
//                    return Float.NaN;
//                }
//                double dx = (x-xy1[0])/h.res;
//                double dy = (y-xy1[1])/h.res;
//                
//                double z = A*dx*dx*dy*dy + B*dx*dx*dy + C*dx*dy*dy + D*dx*dx +
//                           E*dy*dy + F*dx*dy + G*dx + H*dy + I;
//                
//                return (float)z;
//            }
//        } else return Float.NaN;
    }
    
    /**
     * Returns 3D normal to the surface at (i,j) cell
     * @param i coordinate
     * @param j coordinate
     * @return 
     */
    public double[] getNorm(int i, int j){
        float k = 1.0f;
        float l = 1.0f;
        double[] N = new double[3];
        
        if(i>0 && i<h.ncol-1){
            k = 2.0f;
            N[0] = -2*h.zk*(Z[i+1][j] - Z[i-1][j])*h.res;
        } else if(i==0){
            N[0] = -h.zk*(Z[i+1][j] - Z[i][j])*h.res;
        } else {
            N[0] = -h.zk*(Z[i][j] - Z[i-1][j])*h.res;
        }
        
        if(j>0 && j < h.nrow-1){
            l = 2.0f;
            N[1] = -2*h.zk*(Z[i][j+1] - Z[i][j-1])*h.res;
        } else if(j==0){
            N[1] = -h.zk*(Z[i][j+1] - Z[i][j])*h.res;
        } else {
            N[1] = -h.zk*(Z[i][j] - Z[i][j-1])*h.res;
        }
        
        N[2] = k*l*h.res*h.res;
        return N;
    }
    
    /**
     * Returns 2D gradient of the surface at (i,j) cell
     * @param i
     * @param j
     * @return 
     */
    public double[] getGrad(int i, int j){
        double[] G = new double[2];
        
        if(i>0 && i<h.ncol-1){
            G[0] = -0.5f*h.zk*(Z[i+1][j] - Z[i-1][j])/h.res;
        } else if(i==0){
            G[0] = -h.zk*(Z[i+1][j] - Z[i][j])/h.res;
        } else {
            G[0] = -h.zk*(Z[i][j] - Z[i-1][j])/h.res;
        }
        
        if(j>0 && j < h.nrow-1){
            G[1] = -0.5f*h.zk*(Z[i][j+1] - Z[i][j-1])/h.res;
        } else if(j==0){
            G[1] = -h.zk*(Z[i][j+1] - Z[i][j])/h.res;
        } else {
            G[1] = -h.zk*(Z[i][j] - Z[i][j-1])/h.res;
        }
        
        return G;
    }
    
    /**
     * Calculates gradient from the point (x,y)
     * @param x
     * @param y
     * @return 
     */
    public double[] getGradXY(float x, float y){
        int i,j;
        float x1,y1,x2,y2;
        int[] ij = getIJfromXY(x,y);
        if(ij == null)
            return null;
        i=ij[0];
        j=ij[1];
        
        if(i == h.ncol-1 || j == h.nrow-1)
            return null;
        
        if (ij != null){
            double[] xy1 = getXYfromIJ(i,j);
            if(xy1 == null){
                return null;
            }
            double dx = (x-xy1[0])/h.res;
            double dy = (y-xy1[1])/h.res;
            float Ax = Z[i+1][j] - Z[i][j];
            float Ay = Z[i][j+1] - Z[i][j];
            float Axy = Z[i][j] + Z[i+1][j+1] - Z[i+1][j] - Z[i][j+1];
            double[] G = new double[2];
            G[0] = h.zk*(Ax + dy*Axy);
            G[1] = h.zk*(Ay + dx*Axy);
            return G;
        } else {
            return null;
        }
    }
    
    /**
     * Returns surface slope in (i,j) cell
     * @param i
     * @param j
     * @return 
     */
    public double getSlope(int i, int j){
        double[] N = getNorm(i,j);
        if (N != null){
            double len = Math.pow(N[0]*N[0] + N[1]*N[1] + N[2]*N[2], 0.5);
            double slope = Math.acos(N[2]/len);
            return slope;
        } else {
            return Double.NaN;
        }
    } 
    
    public double getMean(){
        return mean;
    }
    
    public double getMrsq(){
        return mrsq;
    }
    
    public double getWeightedMean(){
        return mean2;
    }
    
    public double getWeightedMrsq(){
        return mrsq2;
    }
}
