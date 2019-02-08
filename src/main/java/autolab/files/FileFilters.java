/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.files;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author tsamsonov
 */
public class FileFilters {
    /*
     * Filter for gridded data
     */
    
    public static class GridFilesFilter extends FileFilter {
        
        String extensions[] = {".grd", ".dat", ".asc"};

        @Override
        public boolean accept(File file) {
            if(file.isDirectory()){
                return true;
            }
            String name = file.getName();
            for(String ext: extensions){
                if (name.endsWith(ext)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Grid files (*.grd, *.dat, *.asc)";
        }
        
    }
    
    /*
     * Filter for vector data in shapefile format
     */
    public static class ShapeFilesFilter extends FileFilter {
        
        String extensions[] = {".shp"};

        @Override
        public boolean accept(File file) {
            if(file.isDirectory()){
                return true;
            }
            String name = file.getName();
            for(String ext: extensions){
                if (name.endsWith(ext)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Shapefiles (*.shp)";
        }
        
    }
}
