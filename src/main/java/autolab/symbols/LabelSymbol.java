/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.symbols;

import com.jogamp.opengl.util.awt.TextRenderer;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import ika.proj.ProjectionFactors;
import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author tsamsonov
 */
public class LabelSymbol extends Symbol{
    
    public enum LabelPlacement{
        CENTROID,
        CENTROID_AROUND,
        CONTOUR,
        STRETCHED_HORIZONTAL,
        STRETCHED_CURVED,
        BORDER_CURVED;
    }
    
    private String text;
    
    private Color color;
    
    private Color shadowColor;
    
    private Font font;
    
    private int size = 28;
    
    private boolean shadow = true;
    
    LabelPlacement placement;
    
    TextRenderer renderer;
    
    // The name of the parameter to extract from Geometry user data
    String parameter;
    
    public LabelSymbol(String s){
        placement = LabelPlacement.CENTROID;
        color = Color.ORANGE;
        shadowColor = new Color(140, 50, 10);
//        shadowColor = Color.BLACK;
//        color = new Color(236,90,25);
        font = new Font("SansSerif", Font.BOLD, size);
        text = s;
        
        // Create antialiased text renderer
        renderer = new TextRenderer(font, true, false);
    }
    
    public void setLabelParameter(String p){
        parameter = p;
    }
    
    public void scaleLabel(float scale){
        font = new Font("SansSerif", Font.BOLD, (int)(size * scale));
        
        renderer = new TextRenderer(font, true, false);
    }
    
    @Override
    public void draw(Geometry g, GLAutoDrawable drawable) {
        
        GL2 gl = drawable.getGL().getGL2();
        
        /*
        current projection matrix
        */
        double[] matrix = new double[16];

        /*
        current viewport
        */
        int[] viewPort = new int[4];
        
        
        // get current projection and viewport
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
        int width = viewPort[2];
        int height = viewPort[3];
        
        gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, matrix, 0);
        float scaleX  = (float) (matrix[0]);
        float scaleY  = (float) (matrix[5]);
        
        float deltaX = (float) (matrix[12]);
        float deltaY = (float) (matrix[13]);
        
        switch(placement){
            case CENTROID:
            default:
                
                renderer.beginRendering(width, height);
//                renderer.setColor(color);
                
                if(g instanceof GeometryCollection){
                    GeometryCollection features = (GeometryCollection)g;
                    
                    DecimalFormat numFormat= new DecimalFormat("##.##");
                    
                    for (int i = 0; i < features.getNumGeometries(); i++) {

                        Point p = features.getGeometryN(i).getCentroid();
                        ProjectionFactors pf = (ProjectionFactors)features.getGeometryN(i).getUserData();
                        
                        switch(parameter){
                            case "conv":
                                text = numFormat.format(pf.conv);
                                break;
                            case "conv2":
                                text = numFormat.format(pf.conv2);
                                break;
                            case "a":
                                text = numFormat.format(pf.a);
                                break;
                            case "b":
                                text = numFormat.format(pf.b);
                                break;
                            case "h":
                                text = numFormat.format(pf.h);
                                break;
                            case "k":
                                text = numFormat.format(pf.k);
                                break;
                            case "omega":
                                text = numFormat.format(pf.omega);
                                break;
                            case "thetap":
                                text = numFormat.format(pf.thetap);
                                break;
                            case "s":
                                text = numFormat.format(pf.s);
                                break; 
                            default:
                                text = numFormat.format(pf.conv);
                                break;
                        }
                        
                
                        // viewport coordinates [-1, 1]
                        double xvp = p.getX() * scaleX + deltaX;
                        double yvp = p.getY() * scaleY + deltaY;

                        int col = (int)(0.5 * width  * (xvp + 1));
                        int row = (int)(0.5 * height * (yvp + 1));
                        
                        renderer.setColor(shadowColor);
                        
                        renderer.draw(text, col+1, row);
                        renderer.draw(text, col+2, row-2);
                        renderer.draw(text, col, row-2);
                        renderer.draw(text, col-1, row-2);
                        
                        renderer.setColor(color);
                        
                        renderer.draw(text, col, row);
                    }
                }

                renderer.endRendering();
                break;
        }
    }
    
    public void setText(String s){
        text = s;
    }
    
    public void setColor(Color c){
        color = c;
    }
    
    public void setSize(byte s){
        font.deriveFont(font.getStyle(), s);
    }
    
    public void setFont(Font f){
        font = f;
    }
    
}
