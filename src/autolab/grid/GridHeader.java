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
public class GridHeader {
     
    public double xmin, xmax;
    public double ymin, ymax;
    public float zmin, zmax;
    public double res;
    public int nrow, ncol;
    public float noData;
    public float zk;
    
    public GridHeader(){
        
    }
    /**
     * Grid header short constructor. Other values are calculated from parameters
     * @param x
     * @param y
     * @param rows
     * @param cols
     * @param cellsize 
     */
    public GridHeader(double x, double y, int rows, int cols, 
                        float z1, float z2, double cellsize){
        // Source parameters
        xmin = x;
        ymin = y;
        nrow = rows;
        ncol = cols;
        res = cellsize;
        zmin = z1;
        zmax = z2;
        
        // Calculated parameters
        xmax = xmin + cols*cellsize;
        ymax = ymin + rows*cellsize;
        
        // Default parameters
        noData = Float.NaN;
        zk = 1.0f;
    }
    
    public GridHeader(GridHeader h){
        // Source parameters
        xmin = h.xmin;
        ymin = h.ymin;
        nrow = h.nrow;
        ncol = h.ncol;
        res = h.res;
        zmin = h.zmin;
        zmax = h.zmax;
        
        // Calculated parameters
        xmax = xmin + h.ncol * res;
        ymax = ymin + h.nrow * res;
        
        // Default parameters
        noData = Float.NaN;
        zk = 1.0f;
    }
}
