/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.grid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringTokenizer;
import javax.swing.BoundedRangeModel;

/**
 * A simple reader for Esri ASCII grid files.
 * Adapted from Bernhard Jenny's (jenny@oregonstate.edu) code 
 * for GetTools Coverage2D format instead of GeoGrid.
 * @author jenny, tsamsonov
 */
public class EsriASCIIGridReader {
    
    /**
     * Returns whether a scanner references valid data that can be read.
     * @param scanner
     * @return
     * @throws IOException
     */
    public static boolean canRead(Scanner scanner) {
        try {
            ESRIASCIIGridHeader header = new ESRIASCIIGridHeader();
            header.readHeader(scanner);
            return header.isValid();
        } catch (Exception exc) {
            return false;
        }
    }

    public static boolean canRead(String filePath) {
        Scanner scanner = null;
        try {
            scanner = createUSScanner(new FileInputStream(filePath));
            return EsriASCIIGridReader.canRead(scanner);
        } catch (Exception exc) {
            return false;
        } finally {
            if (scanner != null) {
                try {
                    scanner.close();
                } catch (Throwable exc) {
                }
            }
        }
    }
    
    /** Read a Grid from a file in ESRI ASCII format.
     * @param fileName The path to the file to be read.
     * @param progress A WorkerProgress to inform about the progress.
     * @return The read grid.
     */
    public static Geogrid read(File file, BoundedRangeModel progressModel) throws java.io.IOException {
        FileInputStream fis = new FileInputStream(file.getAbsolutePath());
        Geogrid grid = EsriASCIIGridReader.read(fis, progressModel);
        String name = file.getName();
        return grid;
    }
    
    /** Read a Grid from a stream in ESRI ASCII format to float[][] array
     * @param is The stream to read from. The stream is closed at the end.
     * @param progress A WorkerProgress to inform about the progress.
     * @return The read grid.
     */
    public static Geogrid read(InputStream input, BoundedRangeModel progressModel)
            throws IOException {


        Scanner scanner = createUSScanner(new BufferedInputStream(input));       
        try {
            ESRIASCIIGridHeader header = new ESRIASCIIGridHeader();
            header.readHeader(scanner);

            float[][] values = new float[header.rows][header.cols];
            
            // Defaults to read min and max
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            
            // use legacy StringTokenizer, which is considerably faster than
            // the Scanner class, which uses regular expressions.
            StringTokenizer tokenizer = new StringTokenizer(scanner.nextLine(), " ");
            
            int nCells = header.cols*header.rows;
            
            int n = 0;
            
            // read grid values. Rows are ordered bottom to top.
            for (int row = 0; row < header.rows ; row++) {
                // read one row
                for (int col = 0; col < header.cols; col++) {
                    
                    n = row * header.cols + col;
                            
                    // a logical row in the grid does not necesseraly correspond
                    // to a line in the file!
                    if (!tokenizer.hasMoreTokens()) {
                        tokenizer = new StringTokenizer(scanner.nextLine(), " ");
                    }
                    final float v = Float.parseFloat(tokenizer.nextToken());
                    if (v == header.noDataValue || Float.isNaN(v)) {
                        values[header.rows-row-1][col] = Float.NaN;
                    } else {
                        values[header.rows-row-1][col] = v;
                        if (v > max)
                            max = v;
                        if (v < min)
                            min = v;
                    }
                    
                    progressModel.setValue((int) (100 * (float)n / (float)nCells));
                }
            }
            
            // Create envelope
            GridHeader gh = new GridHeader(
                    header.west, header.south, header.rows, header.cols, min, max, header.cellSize
            );
            
            // Create output grid
            Geogrid grid = new Geogrid(values, gh);
            progressModel.setValue(100);
            
            return grid;
            
        } finally {
            try {
                // this closes the input stream
                scanner.close();
                
            } catch (Exception exc) {
            }
        }

    }

    /**
     * Creates a scanner for ASCII text with a period as decimal separator.
     * @param is
     * @return
     * @throws FileNotFoundException
     */
    private static Scanner createUSScanner(InputStream is) throws FileNotFoundException {
        Scanner scanner = new Scanner(is, "US-ASCII");
        scanner.useLocale(Locale.US);
        return scanner;
    }
}
