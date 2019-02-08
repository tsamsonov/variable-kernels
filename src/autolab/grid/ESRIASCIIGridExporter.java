/*
 * ESRIASCIIGridExporter.java
 *
 * Created on August 14, 2005, 4:17 PM
 *
 */
package autolab.grid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.BoundedRangeModel;

/**
 * 
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */

/**
 * Modified to export my geogrids
 * @author tsamsonov
 */
public class ESRIASCIIGridExporter {

    private ESRIASCIIGridExporter() {
    }

    public static void export(Geogrid g, File file, BoundedRangeModel progressModel) throws IOException {

        try (PrintWriter writer = 
                new PrintWriter(new BufferedWriter(
                        new FileWriter(file)))) {
            
            String voidValueStr = Float.toString(-9999);
            String lineSeparator = System.getProperty("line.separator");
            writer.write("ncols " + g.getHeader().ncol+ lineSeparator);
            writer.write("nrows " + g.getHeader().nrow + lineSeparator);
            writer.write("xllcorner " + g.getHeader().xmin + lineSeparator);
            writer.write("yllcorner " + g.getHeader().ymin + lineSeparator);
            writer.write("cellsize " + g.getHeader().res + lineSeparator);
            writer.write("nodata_value " + voidValueStr + lineSeparator);
            
            int nrows = g.getHeader().nrow;
            int ncols = g.getHeader().ncol;
            int ncells = nrows*ncols;
            int n = 0;
            
            for (int i = 0; i < nrows; ++i) {
                for (int j = 0; j < ncols; ++j) {
                    float v = g.getZ(nrows-i-1, j);
                    if (Float.isNaN(v)) {
                        writer.write(voidValueStr);
                    } else {
                        writer.write(Float.toString(v));
                    }
                    writer.write(" ");
                    
                    progressModel.setValue(100 * n / ncells);
                    
                    n++;
                }
                writer.write(lineSeparator);
            }
        }
    }
//    private static float findVoidValue(GeoGrid grid) {
//        float min = grid.getStatistics().min;
//        String voidValue = "-9999";
//        while (Float.parseFloat(voidValue) >= min) {
//            voidValue += "9";
//        }
//        return Float.parseFloat(voidValue);
//    }
}
