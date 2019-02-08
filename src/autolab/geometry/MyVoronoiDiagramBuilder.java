/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geometry;

import java.util.*;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;
import org.locationtech.jts.triangulate.IncrementalDelaunayTriangulator;
import org.locationtech.jts.triangulate.quadedge.*;

/**
 * A utility class which creates Voronoi Diagrams
 * from collections of points.
 * The diagram is returned as a {@link GeometryCollection} of {@link Polygon}s,
 * clipped to the larger of a supplied envelope or to an envelope determined
 * by the input sites.
 * 
 * @author Martin Davis
 *
 */

/**
 * Removed clipping procedure
 * @author tsamsonov
 */
public class MyVoronoiDiagramBuilder {
    private Collection siteCoords;
	private double tolerance = 0.0;
	private QuadEdgeSubdivision subdiv = null;
	private Envelope clipEnv = null;
	private Envelope diagramEnv = null; 
	
	/**
	 * Creates a new Voronoi diagram builder.
	 *
	 */
	public MyVoronoiDiagramBuilder()
	{
	}
	
	/**
	 * Sets the sites (point or vertices) which will be diagrammed.
	 * All vertices of the given geometry will be used as sites.
	 * 
	 * @param geom the geometry from which the sites will be extracted.
	 */
	public void setSites(Geometry geom)
	{
		// remove any duplicate points (they will cause the triangulation to fail)
		siteCoords = DelaunayTriangulationBuilder.extractUniqueCoordinates(geom);
	}
	
	/**
	 * Sets the sites (point or vertices) which will be diagrammed
	 * from a collection of {@link Coordinate}s.
	 * 
	 * @param coords a collection of Coordinates.
	 */
	public void setSites(Collection coords)
	{
		// remove any duplicate points (they will cause the triangulation to fail)
		siteCoords = DelaunayTriangulationBuilder.unique(CoordinateArrays.toCoordinateArray(coords));
	}
	
	/**
	 * Sets the envelope to clip the diagram to.
	 * The diagram will be clipped to the larger
	 * of this envelope or an envelope surrounding the sites.
	 * 
	 * @param clipEnv the clip envelope.
	 */
	public void setClipEnvelope(Envelope clipEnv)
	{
		this.clipEnv = clipEnv;
	}
	/**
	 * Sets the snapping tolerance which will be used
	 * to improved the robustness of the triangulation computation.
	 * A tolerance of 0.0 specifies that no snapping will take place.
	 * 
	 * @param tolerance the tolerance distance to use
	 */
	public void setTolerance(double tolerance)
	{
		this.tolerance = tolerance;
	}
	
	private void create()
	{
		if (subdiv != null) return;
		
		Envelope siteEnv = DelaunayTriangulationBuilder.envelope(siteCoords);
		diagramEnv = siteEnv;
		// add a buffer around the final envelope
		double expandBy = Math.max(diagramEnv.getWidth()/10, diagramEnv.getHeight()/10);
		diagramEnv.expandBy(expandBy);
		if (clipEnv != null)
			diagramEnv.expandToInclude(clipEnv);
		
		List vertices = DelaunayTriangulationBuilder.toVertices(siteCoords);
		subdiv = new QuadEdgeSubdivision(siteEnv, tolerance);
		IncrementalDelaunayTriangulator triangulator = new IncrementalDelaunayTriangulator(subdiv);
		triangulator.insertSites(vertices);
	}
	
	/**
	 * Gets the {@link QuadEdgeSubdivision} which models the computed diagram.
	 * 
	 * @return the subdivision containing the triangulation
	 */
	public QuadEdgeSubdivision getSubdivision()
	{
		create();
		return subdiv;
	}
	
	/**
	 * Gets the faces of the computed diagram as a {@link GeometryCollection} 
	 * of {@link Polygon}s, clipped as specified.
	 * 
	 * @param geomFact the geometry factory to use to create the output
	 * @return the faces of the diagram
	 */
	public Geometry getDiagram(GeometryFactory geomFact)
	{
		create();
		Geometry polys = subdiv.getVoronoiDiagram(geomFact);
		
		// clip polys to diagramEnv
//		return clipGeometryCollection(polys, diagramEnv);
                return polys;
	}
	
	private static Geometry clipGeometryCollection(Geometry geom, Envelope clipEnv)
	{
		Geometry clipPoly = geom.getFactory().toGeometry(clipEnv);
		List clipped = new ArrayList();
		for (int i = 0; i < geom.getNumGeometries(); i++) {
			Geometry g = geom.getGeometryN(i);
			Geometry result = null;
			// don't clip unless necessary
			if (clipEnv.contains(g.getEnvelopeInternal()))
					result = g;
			else if (clipEnv.intersects(g.getEnvelopeInternal())) {
				try{
                                    result = clipPoly.intersection(g);
                                } catch (TopologyException texp) {
                                    result = g;
                                }
				// keep vertex key info
				result.setUserData(g.getUserData());
			}

			if (result != null && ! result.isEmpty()) {
				clipped.add(result);
			}
		}
		return geom.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(clipped));
	}
}
