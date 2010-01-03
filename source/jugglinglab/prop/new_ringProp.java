// new_ringProp.java
//
// Copyright 2004 by Jack Boyce (jboyce@users.sourceforge.net) and others

/*
 This file is part of Juggling Lab.
 
 Juggling Lab is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.
 
 Juggling Lab is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with Juggling Lab; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package jugglinglab.prop;

import java.util.*;
import java.awt.*;
import java.awt.image.*;

import idx3d.*;
import jugglinglab.util.*;

public class new_ringProp extends ringProp {
	private final int segments = 12;
	private idx3d_Vertex[] inside_points;
	private idx3d_Vertex[] outside_points;
	
	protected void init(String st) throws JuggleExceptionUser {
		super.init(st);
		
		double angle = (2 * JLMath.pi) / segments;
		inside_points = new idx3d_Vertex[segments];
		outside_points = new idx3d_Vertex[segments];
		for (int i = 0; i < segments; i++) {
			inside_points[i] = new idx3d_Vertex((float)(this.inside_diam/2 * Math.cos(i * angle)), 
												(float)(this.inside_diam/2 * Math.sin(i * angle)), 0f);
			outside_points[i] = new idx3d_Vertex((float)(this.outside_diam/2 * Math.cos(i * angle)), 
												 (float)(this.outside_diam/2 * Math.sin(i * angle)), 0f);
		}
	}
	
	public Image getProp2DImage(Component comp, double zoom, double[] camangle) {
		return null;
	}
	
	public String getName() {
		return "New_Ring";
	}
	
	public Object getPropIDX3D() {
		// We have to build up a throw-away object because of the current state of the renderer.
		// TODO: This should eventually be incorporated back into the superclass.
		idx3d_Object ring = new idx3d_Object();
		
		// Copy the cached list of vertices so that the originals don't get modified by the renderer.
		idx3d_Vertex[] inside_points = new idx3d_Vertex[segments];
		idx3d_Vertex[] outside_points = new idx3d_Vertex[segments];
		for (int i = 0; i < segments; i++) {
			//idx3d_Vector in = this.inside_points[i].pos;
			//idx3d_Vector out = this.outside_points[i].pos;
			//inside_points[i] = new idx3d_Vertex(in.x, in.y, in.z);
			//outside_points[i] = new idx3d_Vertex(out.x, out.y, out.z);
			
			inside_points[i] = this.inside_points[i].getClone();
			outside_points[i] = this.outside_points[i].getClone();
		}
		
		// Now make new triangles that point to the copied vertices and add both to the object.
		for (int i = 0; i < segments; i++) {
			int next_index = (i + 1) % segments;
			
			idx3d_Vertex a = inside_points[i];
			idx3d_Vertex b = outside_points[i];
			idx3d_Vertex c = inside_points[next_index];
			idx3d_Vertex d = outside_points[next_index];
			
			// One side of the ring
			ring.addTriangle(new idx3d_Triangle(a, b, c));
			ring.addTriangle(new idx3d_Triangle(b, d, c));
			// The other side of the ring
			ring.addTriangle(new idx3d_Triangle(b, a, c));
			ring.addTriangle(new idx3d_Triangle(d, b, c));
			
			ring.addVertex(a);
			ring.addVertex(b);
		}
		
		ring.setMaterial(new idx3d_Material(color.getRGB()));
		ring.rotate(0f, (float)JLMath.toRad(90.0), 0f);  // probably temporary
		
		return ring;
	}
}
