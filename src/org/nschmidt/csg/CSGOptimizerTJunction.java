package org.nschmidt.csg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.nschmidt.ldparteditor.data.GColour;
import org.nschmidt.ldparteditor.data.GData1;
import org.nschmidt.ldparteditor.data.GData3;
import org.nschmidt.ldparteditor.data.Vertex;
import org.nschmidt.ldparteditor.helpers.math.MathHelper;

public enum CSGOptimizerTJunction {
    INSTANCE;

    // TODO This epsilon should be accessible by the user!
    public static volatile double epsilon = .1;

    static boolean optimize(Random rnd, Map<Plane, List<GData3>> trianglesPerPlane, Map<GData3, IdAndPlane> optimization) {
        boolean result = false;

        for (List<GData3> triangles : trianglesPerPlane.values()) {

            final Set<VectorCSGd> vertices = new TreeSet<>();
            final Map<GData3, VectorCSGd[]> trimap = new HashMap<>();

            for (GData3 tri : triangles) {
                final VectorCSGd[] triverts = new VectorCSGd[]{
                        new VectorCSGd(tri.x1, tri.y1, tri.z1),
                        new VectorCSGd(tri.x2, tri.y2, tri.z2),
                        new VectorCSGd(tri.x3, tri.y3, tri.z3)};

                vertices.add(triverts[0]);
                vertices.add(triverts[1]);
                vertices.add(triverts[2]);
                trimap.put(tri, triverts);
            }

            for (GData3 tri : triangles) {
                final VectorCSGd[] triverts = trimap.get(tri);
                for (VectorCSGd v : vertices) {
                    if (triverts[0].compareTo(v) == 0) continue;
                    if (triverts[1].compareTo(v) == 0) continue;
                    if (triverts[2].compareTo(v) == 0) continue;
                    {
                        double d =  MathHelper.getNearestPointDistanceToLineSegmentCSG(triverts[0], triverts[1], v, epsilon);
                        if (d < epsilon) {
                            // Make sure that the new edge generates no T-Junction!
                            if (isValidEdge(v, triverts[2], vertices)) {
                                final IdAndPlane oldId = optimization.get(tri);
                                optimization.remove(tri);
                                optimization.put(createTriangle(tri, triverts[0], v, triverts[2]), oldId);
                                optimization.put(createTriangle(tri, v, triverts[1], triverts[2]), oldId);
                                result = true;
                                break;
                            }
                        }
                    }

                    {
                        double d = MathHelper.getNearestPointDistanceToLineSegmentCSG(triverts[1], triverts[2], v, epsilon);
                        if (d < epsilon) {
                            if (isValidEdge(v, triverts[0], vertices)) {
                                final IdAndPlane oldId = optimization.get(tri);
                                optimization.remove(tri);
                                optimization.put(createTriangle(tri, triverts[1], v, triverts[0]), oldId);
                                optimization.put(createTriangle(tri, v, triverts[2], triverts[0]), oldId);
                                result = true;
                                break;
                            }
                        }
                    }

                    {
                        double d = MathHelper.getNearestPointDistanceToLineSegmentCSG(triverts[2], triverts[0], v, epsilon);
                        if (d < epsilon) {
                            if (isValidEdge(v, triverts[1], vertices)) {
                                final IdAndPlane oldId = optimization.get(tri);
                                optimization.remove(tri);
                                optimization.put(createTriangle(tri, triverts[1], v, triverts[0]), oldId);
                                optimization.put(createTriangle(tri, v, triverts[1], triverts[2]), oldId);
                                result = true;
                                break;
                            }
                        }
                    }
                }
                if (result) break;
            }
        }
        return result;
    }

    private static boolean isValidEdge(VectorCSGd a, VectorCSGd b, Set<VectorCSGd> vertices) {
        for (VectorCSGd v : vertices) {
            if (a.compareTo(v) == 0) continue;
            if (b.compareTo(v) == 0) continue;
            double d = MathHelper.getNearestPointDistanceToLineSegmentCSG(a, b, v, epsilon);
            if (d < epsilon) {
                return false;
            }
        }
        return true;
    }

    private static GData3 createTriangle(GData3 idol, VectorCSGd a, VectorCSGd b, VectorCSGd c) {
        Vertex v1 = new Vertex((float) a.x, (float) a.y, (float) a.z);
        Vertex v2 = new Vertex((float) b.x, (float) b.y, (float) b.z);
        Vertex v3 = new Vertex((float) c.x, (float) c.y, (float) c.z);
        GData1 parent = idol.parent;
        GColour colour = new GColour(idol.colourNumber, idol.r, idol.g, idol.b, idol.a);
        return new GData3(v1, v2, v3, parent, colour, true);
    }
}
