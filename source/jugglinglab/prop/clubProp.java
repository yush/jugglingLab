// clubProp.java
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

import java.awt.*;
import java.util.*;
import jugglinglab.util.*;
import jugglinglab.renderer.*;
import idx3d.*;

public class clubProp extends Prop {
	protected Color color = null;
	
	public String getName() { return "Club"; }
	
    public Color getEditorColor() { return color; }
	
    public ParameterDescriptor[] getParameterDescriptors() { return null; }
	
    protected void init(String st) throws JuggleExceptionUser {
		color = Color.red;
	}
	
    public Coordinate getMax() { return null; }
    public Coordinate getMin() { return null; }
    public Image getProp2DImage(Component comp, double zoom, double[] camangle) { return null; }
    public Dimension getProp2DSize(Component comp, double zoom) { return null; }
    public Dimension getProp2DCenter(Component comp, double zoom) { return null; }
    public Dimension getProp2DGrip(Component comp, double zoom) { return null; }
    public Coordinate getPropIDX3DGrip() {
		return new Coordinate(0.0, 0.0, 0.0);
	}
	
	public Object getPropIDX3D() {
		// Calibrated to the specs of my 95mm Renegades.
		idx3d_Object club = new idx3d_Object();
		
		// The number of sides of each circle
		int segments = 8;
		double angle = (2 * JLMath.pi) / segments;
		
		// The fat end cap
		float fat_cap_z = 20f;
		float fat_cap_size = 2f;
		idx3d_Vertex[] fat_cap = new idx3d_Vertex[segments];
		idx3d_Vertex fat_end_center = new idx3d_Vertex(0f, 0f, fat_cap_z);
		club.addVertex(fat_end_center);
		
		// The middle of the fat part
		float fat_middle_z = 10f;
		float fat_middle_size = 5f;
		idx3d_Vertex[] fat_middle = new idx3d_Vertex[segments];
		
		// The bridge part
		float bridge_z = -7f;
		float handle_size = 1f;
		idx3d_Vertex[] bridge = new idx3d_Vertex[segments];
		
		// The small end cap
		float small_cap_z = -30f;
		idx3d_Vertex[] small_cap = new idx3d_Vertex[segments];
		idx3d_Vertex small_end_center = new idx3d_Vertex(0f, 0f, small_cap_z);
		club.addVertex(small_end_center);
		
		// Build up the vertex lists
		for (int i = 0; i < segments; i++) {
			// The fat end cap
			fat_cap[i] = new idx3d_Vertex((float)(fat_cap_size * Math.cos(i * angle)),
										  (float)(fat_cap_size * Math.sin(i * angle)),
										  fat_cap_z);
			
			// The middle of the fat part
			fat_middle[i] = new idx3d_Vertex((float)(fat_middle_size * Math.cos(i * angle)),
											 (float)(fat_middle_size * Math.sin(i * angle)),
											 fat_middle_z);
			
			// The bridge part
			bridge[i] = new idx3d_Vertex((float)(handle_size * Math.cos(i * angle)),
										 (float)(handle_size * Math.sin(i * angle)),
										 bridge_z);
			
			// The small end cap
			small_cap[i] = new idx3d_Vertex((float)(handle_size * Math.cos(i * angle)),
											(float)(handle_size * Math.sin(i * angle)),
											small_cap_z);
		}
		
		// Connect vertices as triangles and add to the object
		for (int i = 0; i < segments; i++) {
			int next_index = (i + 1) % segments;
			
			// The fat end cap
			idx3d_Vertex fc1 = fat_cap[i];
			idx3d_Vertex fc2 = fat_cap[next_index];
			
			club.addVertex(fc1);
			club.addTriangle(fc2, fc1, fat_end_center);
			
			// The middle of the fat part
			idx3d_Vertex fm1 = fat_middle[i];
			idx3d_Vertex fm2 = fat_middle[next_index];
			
			club.addVertex(fm1);
			club.addTriangle(fc1, fm1, fm2);
			club.addTriangle(fc1, fm2, fc2);
			
			// The bridge part
			idx3d_Vertex b1 = bridge[i];
			idx3d_Vertex b2 = bridge[next_index];
			
			club.addVertex(b1);
			club.addTriangle(fm1, b1, b2);
			club.addTriangle(fm1, b2, fm2);
			
			// The small end cap
			idx3d_Vertex sc1 = small_cap[i];
			idx3d_Vertex sc2 = small_cap[next_index];
			
			club.addVertex(sc1);
			club.addTriangle(sc2, sc1, small_end_center);
			
			// The handle
			club.addTriangle(b1, sc1, sc2);
			club.addTriangle(b1, sc2, b2);
		}
		
		club.setMaterial(new idx3d_Material(color.getRGB()));
		
		return club;
	}
}
