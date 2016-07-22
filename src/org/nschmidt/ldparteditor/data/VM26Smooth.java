/* MIT - License

Copyright (c) 2012 - this year, Nils Schmidt

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. */
package org.nschmidt.ldparteditor.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.nschmidt.ldparteditor.dialogs.smooth.SmoothDialog;
import org.nschmidt.ldparteditor.enums.Threshold;

public class VM26Smooth extends VM25RectangleSnap {

    protected VM26Smooth(DatFile linkedDatFile) {
        super(linkedDatFile);
    }    
    
    public void smooth(boolean x, boolean y, boolean z, BigDecimal factor, int iterations) {
        
        SmoothDialog.setX(x);
        SmoothDialog.setY(y);
        SmoothDialog.setZ(z);
        
        SmoothDialog.setFactor(factor);
        SmoothDialog.setIterations(iterations);
        
        Object[] obj = getSmoothedVertices(selectedVertices);
        clearSelection();
        
        @SuppressWarnings("unchecked")
        ArrayList<Vertex> newVerts = (ArrayList<Vertex>) obj[0];
        
        @SuppressWarnings("unchecked")
        ArrayList<Vertex> oldVerts = (ArrayList<Vertex>) obj[3];
        
        int size = newVerts.size();
        for (int i = 0; i < size; i++) {
            Vertex v1 = oldVerts.get(i);
            Vertex v2 = newVerts.get(i);
            
            if (!v1.equals(v2)) {
                changeVertexDirectFast(v1, v2, true);
                selectedVertices.add(v2);
            }
        }
        
        if (!selectedVertices.isEmpty()) {
            setModified_NoSync();
            linkedDatFile.getVertexManager().restoreHideShowState();
            syncWithTextEditors(true);
            updateUnsavedStatus();
        }
    }
    
    
    public Vertex[] getNeighbourVertices(Vertex old) {
        
        TreeSet<Vertex> tverts1 = new TreeSet<Vertex>();
        TreeSet<Vertex> tverts2 = new TreeSet<Vertex>();
        HashSet<GData> surfs = getLinkedSurfaces(old);

        for (GData g : surfs) {
            Vertex[] v;
            if (g.type() == 3) {
                v = triangles.get(g);
            } else if (g.type() == 4) {
                v = quads.get(g);
            } else {
                continue;
            }
            for (Vertex v2 : v) {
                if (tverts1.contains(v2)) {
                    tverts2.add(v2);
                }
                tverts1.add(v2);
            }
        }
        tverts2.remove(old);
        
        if (tverts2.size() != surfs.size()) {
            return new Vertex[0];
        }
        
        Vertex[] result = new Vertex[tverts2.size()];
        Iterator<Vertex> it = tverts2.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }        
        return result;
    }
    
    public Object[] getSmoothedVertices(Set<Vertex> verts) {  
        
        final boolean isX = SmoothDialog.isX();
        final boolean isY = SmoothDialog.isY();
        final boolean isZ = SmoothDialog.isZ();
        
        ArrayList<Vertex> vertsToProcess = new ArrayList<Vertex>();        
        ArrayList<Vertex> originalVerts = new ArrayList<Vertex>();
        ArrayList<Vertex> newPos = new ArrayList<Vertex>();
        
        TreeSet<Vertex> origVerts = new TreeSet<Vertex>();
        origVerts.addAll(verts);
        
        {
            TreeSet<Vertex> allVerts = new TreeSet<Vertex>();
            for (Vertex vertex : verts) {
                allVerts.add(vertex);
                for (Vertex vertex2 : getNeighbourVertices(vertex)) {
                    allVerts.add(vertex2);
                }
            }
            vertsToProcess.addAll(allVerts);
            originalVerts.addAll(vertsToProcess);
        }
                
        
        TreeMap<Integer, ArrayList<Integer>> adjacency = new TreeMap<Integer, ArrayList<Integer>>();
        TreeMap<Vertex, Integer> indmap = new TreeMap<Vertex, Integer>();
        
        int i = 0;
        for (Vertex vertex : vertsToProcess) {
            indmap.put(vertex, i);
            i++;
        }
        
        for (Vertex vertex : origVerts) {
            Integer key = indmap.get(vertex);
            ArrayList<Integer> ad;
            if (adjacency.containsKey(key)) {
                ad = adjacency.get(key);
            } else {
                ad = new ArrayList<Integer>();
                adjacency.put(key, ad);
            }
            for (Vertex vertex2 : getNeighbourVertices(vertex)) {
                ad.add(indmap.get(vertex2));
            }
        }
        
        final BigDecimal FACTOR = SmoothDialog.getFactor();
        final BigDecimal ONE_MINUS_FACTOR = BigDecimal.ONE.subtract(FACTOR);
        final int iterations = SmoothDialog.getIterations();
        final int size = vertsToProcess.size();
        for (int j = 0; j < iterations; j++) {
            i = 0;
            newPos.clear();
            for (Vertex vertex : vertsToProcess) {                
                if (origVerts.contains(vertex)) {
                    
                    ArrayList<Integer> il = adjacency.get(indmap.get(vertex));                    
                    
                    if (il.size() > 0) {                        
                        final BigDecimal ad = new BigDecimal(il.size());
                        
                        BigDecimal vx = BigDecimal.ZERO;
                        BigDecimal vy = BigDecimal.ZERO;
                        BigDecimal vz = BigDecimal.ZERO;
                        
                        for (Integer k : il) {
                            if (isX) vx = vx.add(vertsToProcess.get(k).X);
                            if (isY) vy = vy.add(vertsToProcess.get(k).Y);
                            if (isZ) vz = vz.add(vertsToProcess.get(k).Z);
                        }
                        
                        if (isX) {
                            vx = vx.divide(ad, Threshold.mc).multiply(FACTOR).add(vertex.X.multiply(ONE_MINUS_FACTOR));
                        } else {
                            vx = vertex.X;
                        }
                        if (isY) {
                            vy = vy.divide(ad, Threshold.mc).multiply(FACTOR).add(vertex.Y.multiply(ONE_MINUS_FACTOR));
                        } else {
                            vy = vertex.Y;
                        }
                        if (isZ) {
                            vz = vz.divide(ad, Threshold.mc).multiply(FACTOR).add(vertex.Z.multiply(ONE_MINUS_FACTOR));
                        } else {
                            vz = vertex.Z;
                        }                                                 
                            
                        newPos.add(new Vertex(vx, vy, vz));
                        
                    } else {
                        newPos.add(vertex);
                    }
                    
                } else {
                    newPos.add(null);
                }
                i++;
            }
            origVerts.clear();
            indmap.clear();
            i = 0;
            
            while (i < size) {
                Vertex nv = newPos.get(i);                
                if (nv != null) {
                    origVerts.add(nv);
                    vertsToProcess.set(i, nv);
                    indmap.put(nv, i);
                } else {
                    indmap.put(vertsToProcess.get(i), i);
                }
                i++;
            }
            
        }                               
        
        return new Object[]{vertsToProcess, indmap, adjacency, originalVerts};
    }
}
