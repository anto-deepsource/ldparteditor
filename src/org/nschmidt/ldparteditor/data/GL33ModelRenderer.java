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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;
import org.nschmidt.ldparteditor.composites.Composite3D;
import org.nschmidt.ldparteditor.enums.View;
import org.nschmidt.ldparteditor.helpers.math.MathHelper;
import org.nschmidt.ldparteditor.helpers.math.ThreadsafeHashMap;
import org.nschmidt.ldparteditor.logger.NLogger;
import org.nschmidt.ldparteditor.opengl.GLMatrixStack;
import org.nschmidt.ldparteditor.opengl.GLShader;
import org.nschmidt.ldparteditor.opengl.OpenGLRenderer33;

/**
 * New OpenGL 3.3 high performance render function for the model (VAO accelerated)
 * @author nils
 *
 */
public class GL33ModelRenderer {

    boolean isPaused = false;

    private final Composite3D c3d;
    private final OpenGLRenderer33 renderer;

    public GL33ModelRenderer(Composite3D c3d) {
        this.c3d = c3d;
        this.renderer = (OpenGLRenderer33) c3d.getRenderer();
    }

    // FIXME needs concept implementation!
    // |
    // --v Here I try to use only one(!) VAO for the price of letting an asynchronous thread doing the buffer data generation! 

    // This is super-fast!
    // However, TEXMAP/!LPE PNG will require a multi-VAO solution (all non-TEXMAP/PNG stuff can still be rendered with one VAO). 

    private int vao;
    private int vbo;
    
    private int vaoGlyphs;
    private int vboGlyphs;
    
    private int vaoLines;
    private int vboLines;
    
    private int vaoTempLines;
    private int vboTempLines;
    
    private int vaoVertices;
    private int vboVertices;
    
    private int vaoSelectionLines;
    private int vboSelectionLines;
    
    private int vaoCondlines;
    private int vboCondlines;

    private volatile Lock lock = new ReentrantLock();
    private static volatile Lock static_lock = new ReentrantLock();
    private static volatile AtomicInteger idGen = new AtomicInteger(0);
    private static volatile AtomicInteger idCount = new AtomicInteger(0);

    private static volatile CopyOnWriteArrayList<Integer> idList = new CopyOnWriteArrayList<>();

    private volatile AtomicBoolean calculateCondlineControlPoints = new AtomicBoolean(true);
    private volatile TreeSet<Vertex> pureCondlineControlPoints = new TreeSet<>();
    private volatile float[] dataTriangles = null;
    private volatile float[] dataLines = new float[]{0f};
    private volatile float[] dataTempLines = new float[]{0f};
    private volatile float[] dataGlyphs = new float[]{0f};
    private volatile float[] dataVertices = null;
    private volatile float[] dataCondlines = new float[]{0f};
    private volatile float[] dataSelectionLines = new float[]{0f};
    private volatile int solidTriangleSize = 0;
    private volatile int transparentTriangleOffset = 0;
    private volatile int transparentTriangleSize = 0;
    private volatile int lineSize = 0;
    private volatile int tempLineSize = 0;
    private volatile int glyphSize = 0;
    private volatile int vertexSize = 0;
    private volatile int condlineSize = 0;
    private volatile int selectionSize = 0;
    
    private volatile boolean usesTEXMAP = false;
    private volatile boolean usesPNG = false;

    private volatile AtomicBoolean isRunning = new AtomicBoolean(true);

    public void init() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, 0);

        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, 3 * 4);

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, (3 + 3) * 4);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        
        vaoGlyphs = GL30.glGenVertexArrays();
        vboGlyphs = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoGlyphs);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboGlyphs);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, 0);

        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, 3 * 4);

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, (3 + 3) * 4);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        
        vaoLines = GL30.glGenVertexArrays();
        vboLines = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoLines);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboLines);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 4) * 4, 0);

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 4) * 4, 3 * 4);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        
        vaoTempLines = GL30.glGenVertexArrays();
        vboTempLines = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoTempLines);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTempLines);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 4) * 4, 0);

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 4) * 4, 3 * 4);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        
        vaoVertices = GL30.glGenVertexArrays();
        vboVertices = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoVertices);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboVertices);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 4) * 4, 0);

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 4) * 4, 3 * 4);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        
        vaoSelectionLines = GL30.glGenVertexArrays();
        vboSelectionLines = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoSelectionLines);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboSelectionLines);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 4) * 4, 0);

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 4) * 4, 3 * 4);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        
        vaoCondlines = GL30.glGenVertexArrays();
        vboCondlines = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoCondlines);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboCondlines);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 15 * 4, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 15 * 4, 3 * 4);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, 15 * 4, 6 * 4);
        GL20.glEnableVertexAttribArray(3);
        GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, 15 * 4, 9 * 4);
        GL20.glEnableVertexAttribArray(4);
        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, 15 * 4, 12 * 4);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        new Thread(new Runnable() {
            @Override
            public void run() {
                
                float[] normal;
                
                final Vector4f Nv = new Vector4f(0, 0, 0, 1f);
                final Matrix4f Mm = new Matrix4f();                
                Matrix4f.setIdentity(Mm);
                
                final Set<GData> selectionSet = new HashSet<GData>();
                final ArrayList<GDataAndWinding> dataInOrder = new ArrayList<>();
                final HashMap<GData, Vertex[]> vertexMap = new HashMap<>();
                final HashMap<GData, float[]> normalMap = new HashMap<>();
                final HashMap<GData, GData> transformMap = new HashMap<>();
                final ThreadsafeHashMap<GData1, Matrix4f> CACHE_viewByProjection = new ThreadsafeHashMap<GData1, Matrix4f>(1000);
                final HashMap<GData1, Matrix4f> matrixMap = new HashMap<>();
                final Integer myID = idGen.getAndIncrement();
                matrixMap.put(View.DUMMY_REFERENCE, View.ID);
                idList.add(myID);
                while (isRunning.get()) {

                    boolean myTurn;
                    try {
                        myTurn = myID == idList.get(idCount.get()); 
                    } catch (IndexOutOfBoundsException iob) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {}
                        continue;
                    }

                    if (!myTurn) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {}
                        continue;
                    }

                    try {                        
                        static_lock.lock();
                        final long start = System.currentTimeMillis();

                        // First we have to get links to the sets from the model
                        final DatFile df = c3d.getLockableDatFileReference();
                        // Just to speed up things in some cases...                           
                        if (!df.isDrawSelection()) {
                            continue; // static_lock.unlock(); on finally
                        }
                        final VertexManager vm = df.getVertexManager();
                        final Lock maniLock = vm.getManifestationLock();
                        // For the declared vertices, we have to use shallow copy
                        maniLock.lock();
                        final List<Vertex> vertices = new ArrayList<>(vm.vertexLinkedToPositionInFile.size());
                        vertices.addAll(vm.vertexLinkedToPositionInFile.keySet());
                        maniLock.unlock();
                        
                        if (calculateCondlineControlPoints.compareAndSet(true, false)) {
                            CompletableFuture.runAsync( () -> {
                                final TreeSet<Vertex> tmpPureCondlineControlPoints = new TreeSet<>();
                                for (Vertex v : vertices) {
                                    Set<VertexManifestation> manis = vm.vertexLinkedToPositionInFile.get(v);
                                    if (manis != null) {
                                        boolean pureControlPoint = true;
                                        maniLock.lock();
                                        for (VertexManifestation m : manis) {
                                            if (m.getPosition() < 2 || m.getGdata().type() != 5) {
                                                pureControlPoint = false;
                                                break;
                                            }
                                        }
                                        maniLock.unlock();
                                        if (pureControlPoint) {
                                            tmpPureCondlineControlPoints.add(v);
                                        }
                                    }
                                }
                                pureCondlineControlPoints = tmpPureCondlineControlPoints;
                                calculateCondlineControlPoints.set(true);
                            });
                        }
                        
                        // The links are sufficient
                        final Set<GData> selectedData = vm.selectedData;                        
                        final Set<Vertex> selectedVertices = vm.selectedVertices;
                        final Set<Vertex> hiddenVertices = vm.hiddenVertices;
                        final ThreadsafeHashMap<GData2, Vertex[]> lines = vm.lines;
                        final ThreadsafeHashMap<GData3, Vertex[]> triangles = vm.triangles;
                        final ThreadsafeHashMap<GData4, Vertex[]> quads = vm.quads;
                        final ThreadsafeHashMap<GData5, Vertex[]> condlines = vm.condlines;                            

                        // Build the list of the data from the datfile
                        dataInOrder.clear();
                        vertexMap.clear();
                        normalMap.clear();
                        transformMap.clear();
                        selectionSet.clear();
                        CACHE_viewByProjection.clear();
                        
                        {
                            boolean[] special = loadBFCinfo(dataInOrder, vertexMap, matrixMap, df,
                                    lines, triangles, quads, condlines);
                            usesPNG = special[0];
                            usesTEXMAP = special[1];                            
                        }

                        final int renderMode = c3d.getRenderMode();
                        final int lineMode = c3d.getLineMode();
                        final boolean condlineMode = renderMode == 6;
                        final boolean hideCondlines = !condlineMode && lineMode > 1;
                        final boolean hideLines = !condlineMode && lineMode > 2;
                        final float zoom = c3d.getZoom();
                        final Matrix4f viewport = c3d.getViewport();

                        int local_triangleSize = 0;
                        int local_lineSize = 0;
                        int local_condlineSize = 0;
                        int local_tempLineSize = 0;
                        int local_verticesSize = vertices.size();
                        int local_glyphSize = 0;
                        int local_selectionLineSize = 0;
                        
                        int triangleVertexCount = 0;
                        int lineVertexCount = 0;
                        int condlineVertexCount = 0;
                        int tempLineVertexCount = 0;
                        int glyphVertexCount = 0;
                        int selectionLineVertexCount = 0;
                        
                        
                        // Only do "heavy" CPU condline computing with the special condline mode
                        // (if the condline was not shown before)
                        if (condlineMode) {
                            dataInOrder.parallelStream().forEach((GDataAndWinding gw) -> {
                                GData gd = gw.data;
                                if (gd.type() == 5) {
                                    ((GData5) gd).isShown(viewport, CACHE_viewByProjection, zoom);
                                }
                            });
                        }

                        // Calculate the buffer sizes
                        // Lines are never transparent!
                        for (GDataAndWinding gw : dataInOrder) {
                            
                            GData tgd = gw.data;
                            if (!tgd.visible) {
                                continue;
                            }
                            
                            final boolean selected = selectedData.contains(tgd);
                            
                            // FIXME If anything is transformed, transform it here (transformMap)
                            // and update the vertex positions (vertexMap) and normals for it (normalMap)
                            
                            final GData gd = tgd;
                            if (selected) {
                                selectionSet.add(gd);
                            }
                            switch (gd.type()) {
                            case 2:
                                if (hideLines) {                                    
                                    continue;
                                }
                                final GData2 gd2 = (GData2) gd;
                                if (gd2.isLine) {
                                    local_lineSize += 14;
                                    lineVertexCount += 2;
                                    if (selected) {
                                        local_selectionLineSize += 14;
                                        selectionLineVertexCount += 2;
                                    }
                                } else {
                                    int[] distanceMeterSize = gd2.getDistanceMeterDataSize();
                                    local_glyphSize += distanceMeterSize[0];
                                    glyphVertexCount += distanceMeterSize[1];
                                    local_tempLineSize += distanceMeterSize[2];
                                    tempLineVertexCount += distanceMeterSize[3];
                                }
                                continue;
                            case 3:
                                final GData3 gd3 = (GData3) gd;
                                if (gd3.isTriangle) {
                                    switch (renderMode) {
                                    case 0:                                   
                                    case 1:
                                        local_triangleSize += 60;
                                        triangleVertexCount += 6;
                                        continue;
                                    default:
                                        continue;
                                    }
                                } else {
                                    int[] protractorSize = gd3.getProtractorDataSize();
                                    local_glyphSize += protractorSize[0];
                                    glyphVertexCount += protractorSize[1];
                                    local_tempLineSize += protractorSize[2];
                                    tempLineVertexCount += protractorSize[3];
                                }
                                continue;
                            case 4:
                                switch (renderMode) {
                                case 0:
                                case 1:
                                    local_triangleSize += 120;
                                    triangleVertexCount += 12;
                                    continue;
                                default:
                                    continue;
                                }
                            case 5:
                                if (hideCondlines) {
                                    continue;
                                }
                                // Condlines are tricky, since I have to calculate their visibility
                                local_condlineSize += 30;
                                condlineVertexCount += 2;
                                if (selected) {
                                    local_selectionLineSize += 42;
                                    selectionLineVertexCount += 6;
                                }
                                continue;
                            default:
                                continue;
                            }
                        }

                        // for GL_TRIANGLES
                        float[] triangleData = new float[local_triangleSize];
                        float[] glyphData = new float[local_glyphSize];
                        // for GL_LINES
                        float[] lineData = new float[local_lineSize];
                        float[] condlineData = new float[local_condlineSize];
                        float[] tempLineData = new float[local_tempLineSize];
                        float[] selectionLineData = new float[local_selectionLineSize];
                        
                        // for GL_POINTS
                        float[] vertexData = new float[local_verticesSize * 7];
                        
                        // Build the vertex array
                        {
                            final float r = View.vertex_Colour_r[0]; 
                            final float g = View.vertex_Colour_g[0];
                            final float b = View.vertex_Colour_b[0];
                            final float r2 = View.vertex_selected_Colour_r[0]; 
                            final float g2 = View.vertex_selected_Colour_g[0];
                            final float b2 = View.vertex_selected_Colour_b[0];
                            int i = 0;
                            for(Vertex v : vertices) {
                                vertexData[i] = v.x;
                                vertexData[i + 1] = v.y;
                                vertexData[i + 2] = v.z;
                                
                                if (selectedVertices.contains(v)) {
                                    vertexData[i + 3] = r2;
                                    vertexData[i + 4] = g2;
                                    vertexData[i + 5] = b2;
                                    vertexData[i + 6] = 7f;
                                } else {
                                    vertexData[i + 3] = r;
                                    vertexData[i + 4] = g;
                                    vertexData[i + 5] = b;
                                    
                                    if (c3d.isShowingCondlineControlPoints()) {
                                        vertexData[i + 6] = hiddenVertices.contains(v) ? 0f : 7f;
                                    } else {
                                        vertexData[i + 6] = hiddenVertices.contains(v) || pureCondlineControlPoints.contains(v) ? 0f : 7f;
                                    }
                                }
                                i += 7;
                            }
                        }
                        
                        
                        Vertex[] v;
                        int triangleIndex = 0;                        
                        int lineIndex = 0;
                        int condlineIndex = 0;
                        int tempLineIndex = 0;
                        int selectionLineIndex = 0;
                        int glyphIndex = 0;

                        // Iterate the objects and generate the buffer data
                        // TEXMAP and Real Backface Culling are quite "the same", but they need different vertex normals / materials
                        for (GDataAndWinding gw : dataInOrder) {                                
                            final GData gd = transformMap.getOrDefault(gw.data, gw.data);
                            if (!gd.visible) {
                                continue;
                            }
                            final boolean transformed = gd != gw.data;
                            final boolean selected = selectionSet.contains(gd);
                            switch (gd.type()) {
                            case 2:
                                if (hideLines) {
                                    continue;
                                }
                                GData2 gd2 = (GData2) gd;
                                v = vertexMap.get(gd);
                                if (gd2.isLine) {
                                    if (selected) {
                                        pointAt7(0, v[0].x, v[0].y, v[0].z, selectionLineData, selectionLineIndex);
                                        pointAt7(1, v[1].x, v[1].y, v[1].z, selectionLineData, selectionLineIndex);
                                        colourise7(0, 2, View.vertex_selected_Colour_r[0], View.vertex_selected_Colour_g[0], View.vertex_selected_Colour_b[0], 7f, selectionLineData, selectionLineIndex);
                                        selectionLineIndex += 2;
                                    }
                                    pointAt7(0, v[0].x, v[0].y, v[0].z, lineData, lineIndex);
                                    pointAt7(1, v[1].x, v[1].y, v[1].z, lineData, lineIndex);
                                    if (renderMode != 1) {
                                        colourise7(0, 2, gd2.r, gd2.g, gd2.b, 7f, lineData, lineIndex);
                                    } else {
                                        final float r = MathHelper.randomFloat(gd2.ID, 0);
                                        final float g = MathHelper.randomFloat(gd2.ID, 1);
                                        final float b = MathHelper.randomFloat(gd2.ID, 2);
                                        colourise7(0, 2, r, g, b, 7f, lineData, lineIndex);
                                    }
                                    lineIndex += 2;
                                } else {
                                    int[] inc = gd2.insertDistanceMeter(v, glyphData, tempLineData, glyphIndex, tempLineIndex);
                                    tempLineIndex += inc[0];
                                    glyphIndex += inc[1];
                                }
                                continue;
                            case 3:
                                GData3 gd3 = (GData3) gd;
                                v = vertexMap.get(gd);
                                if (gd3.isTriangle) {
                                    float xn, yn, zn;
                                    
                                    if ((normal = normalMap.get(gd)) != null) {
                                        xn = normal[0];
                                        yn = normal[1];
                                        zn = normal[2];
                                    } else {
                                        Nv.x = gd3.xn;
                                        Nv.y = gd3.yn;
                                        Nv.z = gd3.zn;
                                        Nv.w = 1f;
                                        Matrix4f loc = matrixMap.get(gd3.parent);
                                        Matrix4f.transform(loc, Nv, Nv);
                                        xn = Nv.x - loc.m30; 
                                        yn = Nv.y - loc.m31;
                                        zn = Nv.z - loc.m32;
                                    }                                    
                                    
                                    switch (renderMode) {
                                    case 0:
                                    {
                                        pointAt(0, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);
                                        pointAt(1, v[1].x, v[1].y, v[1].z, triangleData, triangleIndex);
                                        pointAt(2, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                        pointAt(3, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);
                                        pointAt(4, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                        pointAt(5, v[1].x, v[1].y, v[1].z, triangleData, triangleIndex);
                                        colourise(0, 6, gd3.r, gd3.g, gd3.b, gd3.visible ? gd3.a : 0f, triangleData, triangleIndex);
                                        if (gw.negativeDeterminant) {                                            
                                            normal(0, 3, xn, yn, zn, triangleData, triangleIndex);
                                            normal(3, 3, -xn, -yn, -zn, triangleData, triangleIndex);
                                        } else {
                                            normal(0, 3, -xn, -yn, -zn, triangleData, triangleIndex);
                                            normal(3, 3, xn, yn, zn, triangleData, triangleIndex);
                                        }
                                        triangleIndex += 6;
                                        continue;
                                    }                                    
                                    case 1:
                                    {
                                        final float r = MathHelper.randomFloat(gd3.ID, 0);
                                        final float g = MathHelper.randomFloat(gd3.ID, 1);
                                        final float b = MathHelper.randomFloat(gd3.ID, 2);
                                        pointAt(0, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);
                                        pointAt(1, v[1].x, v[1].y, v[1].z, triangleData, triangleIndex);
                                        pointAt(2, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                        pointAt(3, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);
                                        pointAt(4, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                        pointAt(5, v[1].x, v[1].y, v[1].z, triangleData, triangleIndex);
                                        colourise(0, 6, r, g, b, gd3.visible ? gd3.a : 0f, triangleData, triangleIndex);
                                        if (gw.negativeDeterminant) {                                            
                                            normal(0, 3, xn, yn, zn, triangleData, triangleIndex);
                                            normal(3, 3, -xn, -yn, -zn, triangleData, triangleIndex);
                                        } else {
                                            normal(0, 3, -xn, -yn, -zn, triangleData, triangleIndex);
                                            normal(3, 3, xn, yn, zn, triangleData, triangleIndex);
                                        }
                                        triangleIndex += 6;
                                        continue;
                                    }                                    
                                    default:
                                        continue;
                                    }
                                } else {
                                    int[] inc = gd3.insertProtractor(v, glyphData, tempLineData, glyphIndex, tempLineIndex);
                                    triangleIndex += inc[0];
                                    tempLineIndex += inc[1];
                                }
                                continue;
                            case 4:
                                float xn, yn, zn;
                                v = vertexMap.get(gd);
                                GData4 gd4 = (GData4) gd;
                                
                                if ((normal = normalMap.get(gd)) != null) {
                                    xn = normal[0];
                                    yn = normal[1];
                                    zn = normal[2];
                                } else {
                                    Nv.x = gd4.xn;
                                    Nv.y = gd4.yn;
                                    Nv.z = gd4.zn;
                                    Nv.w = 1f;
                                    Matrix4f loc = matrixMap.get(gd4.parent);
                                    Matrix4f.transform(loc, Nv, Nv);
                                    Nv.x = Nv.x - loc.m30; 
                                    Nv.y = Nv.y - loc.m31;
                                    Nv.z = Nv.z - loc.m32;
                                    xn = Nv.x;
                                    yn = Nv.y;
                                    zn = Nv.z;
                                }
                                
                                switch (renderMode) {
                                case 0:
                                {
                                    pointAt(0, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);
                                    pointAt(1, v[1].x, v[1].y, v[1].z, triangleData, triangleIndex);
                                    pointAt(2, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                    pointAt(3, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                    pointAt(4, v[3].x, v[3].y, v[3].z, triangleData, triangleIndex);
                                    pointAt(5, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);

                                    pointAt(6, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);
                                    pointAt(7, v[3].x, v[3].y, v[3].z, triangleData, triangleIndex);
                                    pointAt(8, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                    pointAt(9, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                    pointAt(10, v[1].x, v[1].y, v[1].z, triangleData, triangleIndex);
                                    pointAt(11, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);

                                    colourise(0, 12, gd4.r, gd4.g, gd4.b, gd4.visible ? gd4.a : 0f, triangleData, triangleIndex);
                                    if (gw.negativeDeterminant) {                                            
                                        normal(0, 6, xn, yn, zn, triangleData, triangleIndex);
                                        normal(6, 6, -xn, -yn, -zn, triangleData, triangleIndex);
                                    } else {
                                        normal(0, 6, -xn, -yn, -zn, triangleData, triangleIndex);
                                        normal(6, 6, xn, yn, zn, triangleData, triangleIndex);
                                    }
                                    triangleIndex += 12;
                                    continue;
                                }                                
                                case 1:
                                {
                                    final float r = MathHelper.randomFloat(gd4.ID, 0);
                                    final float g = MathHelper.randomFloat(gd4.ID, 1);
                                    final float b = MathHelper.randomFloat(gd4.ID, 2);
                                    pointAt(0, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);
                                    pointAt(1, v[1].x, v[1].y, v[1].z, triangleData, triangleIndex);
                                    pointAt(2, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                    pointAt(3, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                    pointAt(4, v[3].x, v[3].y, v[3].z, triangleData, triangleIndex);
                                    pointAt(5, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);

                                    pointAt(6, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);
                                    pointAt(7, v[3].x, v[3].y, v[3].z, triangleData, triangleIndex);
                                    pointAt(8, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                    pointAt(9, v[2].x, v[2].y, v[2].z, triangleData, triangleIndex);
                                    pointAt(10, v[1].x, v[1].y, v[1].z, triangleData, triangleIndex);
                                    pointAt(11, v[0].x, v[0].y, v[0].z, triangleData, triangleIndex);

                                    colourise(0, 12, r, g, b, gd4.visible ? gd4.a : 0f, triangleData, triangleIndex);
                                    if (gw.negativeDeterminant) {                                            
                                        normal(0, 6, xn, yn, zn, triangleData, triangleIndex);
                                        normal(6, 6, -xn, -yn, -zn, triangleData, triangleIndex);
                                    } else {
                                        normal(0, 6, -xn, -yn, -zn, triangleData, triangleIndex);
                                        normal(6, 6, xn, yn, zn, triangleData, triangleIndex);
                                    }
                                    triangleIndex += 12;
                                    continue;
                                }
                                default:
                                    continue;
                                }
                            case 5:
                                if (hideCondlines) {
                                    continue;
                                }
                                GData5 gd5 = (GData5) gd;
                                v = vertexMap.get(gd);
                                if (selected) {
                                    pointAt7(0, v[0].x, v[0].y, v[0].z, selectionLineData, selectionLineIndex);
                                    pointAt7(1, v[1].x, v[1].y, v[1].z, selectionLineData, selectionLineIndex);
                                    pointAt7(2, v[0].x, v[0].y, v[0].z, selectionLineData, selectionLineIndex);
                                    pointAt7(3, v[2].x, v[2].y, v[2].z, selectionLineData, selectionLineIndex);
                                    pointAt7(4, v[0].x, v[0].y, v[0].z, selectionLineData, selectionLineIndex);
                                    pointAt7(5, v[3].x, v[3].y, v[3].z, selectionLineData, selectionLineIndex);
                                    colourise7(0, 2, View.vertex_selected_Colour_r[0], View.vertex_selected_Colour_g[0], View.vertex_selected_Colour_b[0], 7f, selectionLineData, selectionLineIndex);
                                    colourise7(2, 2, View.condline_selected_Colour_r[0], View.condline_selected_Colour_g[0], View.condline_selected_Colour_b[0], 7f, selectionLineData, selectionLineIndex);
                                    colourise7(4, 2, View.condline_selected_Colour_r[0] / 2f, View.condline_selected_Colour_g[0] / 2f, View.condline_selected_Colour_b[0] / 2f, 7f, selectionLineData, selectionLineIndex);
                                    selectionLineIndex += 6;
                                }
                                pointAt15(0, v[0].x, v[0].y, v[0].z, condlineData, condlineIndex);
                                pointAt15(1, v[1].x, v[1].y, v[1].z, condlineData, condlineIndex);                                    
                                controlPointAt15(0, 0, v[1].x, v[1].y, v[1].z, condlineData, condlineIndex);
                                controlPointAt15(0, 1, v[2].x, v[2].y, v[2].z, condlineData, condlineIndex);
                                controlPointAt15(0, 2, v[3].x, v[3].y, v[3].z, condlineData, condlineIndex);
                                controlPointAt15(1, 0, v[0].x, v[0].y, v[0].z, condlineData, condlineIndex);
                                controlPointAt15(1, 1, v[2].x, v[2].y, v[2].z, condlineData, condlineIndex);
                                controlPointAt15(1, 2, v[3].x, v[3].y, v[3].z, condlineData, condlineIndex);
                                if (condlineMode) {
                                    if (gd5.wasShown()) {
                                        colourise15(0, 2, View.condline_shown_Colour_r[0], View.condline_shown_Colour_g[0], View.condline_shown_Colour_b[0], condlineData, condlineIndex);
                                    } else {
                                        colourise15(0, 2, View.condline_hidden_Colour_r[0], View.condline_hidden_Colour_g[0], View.condline_hidden_Colour_b[0], condlineData, condlineIndex);
                                    }
                                } else {
                                    if (renderMode != 1) {
                                        colourise15(0, 2, gd5.r, gd5.g, gd5.b, condlineData, condlineIndex);   
                                    } else {
                                        final float r = MathHelper.randomFloat(gd5.ID, 0);
                                        final float g = MathHelper.randomFloat(gd5.ID, 1);
                                        final float b = MathHelper.randomFloat(gd5.ID, 2);
                                        colourise15(0, 2, r, g, b, condlineData, condlineIndex);
                                    }                                         
                                } 
                                condlineIndex += 2;
                                continue;
                            default:
                                continue;
                            }
                        }

                        lock.lock();
                        dataTriangles = triangleData;
                        solidTriangleSize = triangleVertexCount;
                        transparentTriangleSize = 0;
                        transparentTriangleOffset = 0;
                        vertexSize = local_verticesSize;
                        dataVertices = vertexData;
                        lineSize = lineVertexCount;
                        dataLines = lineData;
                        condlineSize = condlineVertexCount;
                        dataCondlines = condlineData;
                        tempLineSize = tempLineVertexCount;
                        dataTempLines = tempLineData;
                        selectionSize = selectionLineVertexCount;
                        dataSelectionLines = selectionLineData;
                        lock.unlock();

                        if (NLogger.DEBUG) {
                            System.out.println("Processing time: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
                        }
                    } catch (Exception ex) {
                        if (NLogger.DEBUG) {
                            System.out.println("Exception: " + ex.getMessage()); //$NON-NLS-1$
                        }
                    } finally {
                        static_lock.unlock();
                    }
                    if (idCount.incrementAndGet() >= idList.size()) {
                        idCount.set(0);
                    }
                }
                idCount.set(0);
                idList.remove(myID);
                idCount.set(0);
            }
        }).start();
    }

    public void dispose() {
        isRunning.set(false);
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vaoVertices);
        GL15.glDeleteBuffers(vboVertices);
        GL30.glDeleteVertexArrays(vaoLines);
        GL15.glDeleteBuffers(vboLines);
        GL30.glDeleteVertexArrays(vaoTempLines);
        GL15.glDeleteBuffers(vboTempLines);
        GL30.glDeleteVertexArrays(vaoGlyphs);
        GL15.glDeleteBuffers(vboGlyphs);
        GL30.glDeleteVertexArrays(vaoSelectionLines);
        GL15.glDeleteBuffers(vboSelectionLines);
        GL30.glDeleteVertexArrays(vaoCondlines);
        GL15.glDeleteBuffers(vboCondlines);
    }

    private int ts, ss, to, vs, ls, tls, gs, sls, cls;
    public void draw(GLMatrixStack stack, GLShader mainShader, GLShader condlineShader, boolean drawSolidMaterials, DatFile df) {

        Matrix4f vm = c3d.getViewport();
        Matrix4f ivm = c3d.getViewport_Inverse();
        
        if (dataTriangles == null || dataLines == null || dataVertices == null) {
            return;
        }
        
        final float zoom = c3d.getZoom();
        
        
        // TODO Draw !LPE PNG VAOs here
        if (usesPNG) {
            
        }

        // TODO Draw !TEXMAP VAOs here
        if (usesTEXMAP) {
            
        }

        
        if (drawSolidMaterials) {
            
            GL30.glBindVertexArray(vaoGlyphs);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboGlyphs);
            lock.lock();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataGlyphs, GL15.GL_STATIC_DRAW);
            gs = glyphSize;
            lock.unlock();

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, 0);

            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, 3 * 4);

            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, (3 + 3) * 4);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            lock.lock();
            // I can't use glBufferSubData() it creates a memory leak!!!
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataTriangles, GL15.GL_STATIC_DRAW);
            ss = solidTriangleSize;
            to = transparentTriangleOffset;
            ts = transparentTriangleSize;
            ls = lineSize;
            tls = tempLineSize;
            sls = selectionSize;
            lock.unlock();

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, 0);

            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, 3 * 4);

            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 3 + 4) * 4, (3 + 3) * 4);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }

        GL30.glBindVertexArray(vao);
        
        if (c3d.isLightOn()) {
            mainShader.setFactor(.9f);
        } else {
            mainShader.setFactor(1f);
        }

        // Transparent and solid parts are at a different location in the buffer
        if (drawSolidMaterials) {
            
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, ss);           
                        
            if (ls > 0) {
                GL30.glBindVertexArray(vaoLines);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboLines);
                lock.lock();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataLines, GL15.GL_STATIC_DRAW);
                ls = lineSize;
                lock.unlock();

                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 4) * 4, 0);

                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 4) * 4, 3 * 4);

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                GL11.glLineWidth(View.lineWidthGL[0]);                    
                
                Vector4f tr = new Vector4f(vm.m30, vm.m31, vm.m32 + 330f * zoom, 1f);
                Matrix4f.transform(ivm, tr, tr);
                stack.glPushMatrix();
                stack.glTranslatef(tr.x, tr.y, tr.z);
                GL11.glDrawArrays(GL11.GL_LINES, 0, ls);
                stack.glPopMatrix();
            }
            
            if (tls > 0) {
                GL30.glBindVertexArray(vaoTempLines);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTempLines);
                lock.lock();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataTempLines, GL15.GL_STATIC_DRAW);
                tls = tempLineSize;
                lock.unlock();

                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 4) * 4, 0);

                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 4) * 4, 3 * 4);

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                GL11.glLineWidth(View.lineWidthGL[0]);                    
                
                Vector4f tr = new Vector4f(vm.m30, vm.m31, vm.m32 + 330f * zoom, 1f);
                Matrix4f.transform(ivm, tr, tr);
                stack.glPushMatrix();
                stack.glTranslatef(tr.x, tr.y, tr.z);
                GL11.glDrawArrays(GL11.GL_LINES, 0, tls);
                stack.glPopMatrix();
            }
            
            mainShader.setFactor(1f);
            
        } else {

            GL11.glDrawArrays(GL11.GL_TRIANGLES, to, ts);
            mainShader.setFactor(1f);
            
            if (c3d.isShowingVertices()) {
                GL30.glBindVertexArray(vaoVertices);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboVertices);
                lock.lock();
                // I can't use glBufferSubData() it creates a memory leak!!!
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataVertices, GL15.GL_STATIC_DRAW);
                vs = vertexSize;
                lock.unlock();

                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 4) * 4, 0);

                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 4) * 4, 3 * 4);

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                
                if (c3d.isShowingHiddenVertices()) {
                    mainShader.setFactor(.5f);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    GL11.glDrawArrays(GL11.GL_POINTS, 0, vs);
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                    mainShader.setFactor(1f);
                }
                
                Vector4f tr = new Vector4f(vm.m30, vm.m31, vm.m32 + 330f * zoom, 1f);
                Matrix4f.transform(ivm, tr, tr);
                stack.glPushMatrix();
                stack.glTranslatef(tr.x, tr.y, tr.z);
                GL11.glDrawArrays(GL11.GL_POINTS, 0, vs);
                stack.glPopMatrix();
            }
        }
        
        // Draw condlines here
        if (drawSolidMaterials) {
            condlineShader.use();
            stack.setShader(condlineShader);
            
            GL20.glUniform1f(condlineShader.getUniformLocation("showAll"), c3d.getLineMode() == 1 ? 1f : 0f); //$NON-NLS-1$
            GL20.glUniform1f(condlineShader.getUniformLocation("condlineMode"), c3d.getRenderMode() == 6 ? 1f : 0f); //$NON-NLS-1$
            
            GL30.glBindVertexArray(vaoCondlines);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboCondlines);
            lock.lock();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataCondlines, GL15.GL_STATIC_DRAW);
            cls = condlineSize;
            lock.unlock();

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 15 * 4, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 15 * 4, 3 * 4);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, 15 * 4, 6 * 4);
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, 15 * 4, 9 * 4);
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, 15 * 4, 12 * 4);            

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            
            Vector4f tr = new Vector4f(vm.m30, vm.m31, vm.m32 + 330f * zoom, 1f);
            Matrix4f.transform(ivm, tr, tr);
            stack.glPushMatrix();
            stack.glTranslatef(tr.x, tr.y, tr.z);
            GL11.glLineWidth(View.lineWidthGL[0]);
            GL11.glDrawArrays(GL11.GL_LINES, 0, cls);
            stack.glPopMatrix();
            mainShader.use();
            stack.setShader(mainShader);
            
        } else {
            
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            
            // Draw lines from the selection
            if (sls > 0) {
                GL30.glBindVertexArray(vaoSelectionLines);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboSelectionLines);
                lock.lock();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataSelectionLines, GL15.GL_STATIC_DRAW);
                sls = selectionSize;
                lock.unlock();

                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (3 + 4) * 4, 0);

                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, (3 + 4) * 4, 3 * 4);

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                
                
                GL11.glDrawArrays(GL11.GL_LINES, 0, sls);            
            }
            
            // TODO Draw glyphs here 
            if (gs > 0) {
                GL30.glBindVertexArray(vaoGlyphs);
                stack.glPushMatrix();
                GL11.glDisable(GL11.GL_CULL_FACE);
                stack.glMultMatrixf(renderer.getRotationInverse());
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, gs);
                GL11.glEnable(GL11.GL_CULL_FACE);
                stack.glPopMatrix();
            }
            
            GL11.glEnable(GL11.GL_DEPTH_TEST);            
        }
        
        GL30.glBindVertexArray(0);
    }
    
    private boolean[] loadBFCinfo(
            final ArrayList<GDataAndWinding> dataInOrder,
            final HashMap<GData, Vertex[]> vertexMap,
            final HashMap<GData1, Matrix4f> matrixMap, final DatFile df,
            final ThreadsafeHashMap<GData2, Vertex[]> lines,
            final ThreadsafeHashMap<GData3, Vertex[]> triangles,
            final ThreadsafeHashMap<GData4, Vertex[]> quads,
            final ThreadsafeHashMap<GData5, Vertex[]> condlines) {

        final boolean[] result = new boolean[2];
        boolean hasTEXMAP = false;
        boolean hasPNG = false;
        Stack<GData> stack = new Stack<>();
        Stack<Byte> tempWinding = new Stack<>();
        Stack<Boolean> tempInvertNext = new Stack<>();
        Stack<Boolean> tempInvertNextFound = new Stack<>();
        Stack<Boolean> tempNegativeDeterminant = new Stack<>();
        boolean isCertified = true; 

        GData gd = df.getDrawChainStart();

        byte localWinding = BFC.NOCERTIFY;
        int accumClip = 0;
        boolean globalInvertNext = false;
        boolean globalInvertNextFound = false;
        boolean globalNegativeDeterminant = false;

        // The BFC logic/state machine is not correct yet? (for BFC no-certify).
        while ((gd = gd.next) != null || !stack.isEmpty()) {                                
            if (gd == null) {
                if (accumClip > 0) {
                    accumClip--;
                }
                gd = stack.pop();
                localWinding = tempWinding.pop();
                isCertified = localWinding != BFC.NOCERTIFY;
                globalInvertNext = tempInvertNext.pop();
                globalInvertNextFound = tempInvertNextFound.pop();
                globalNegativeDeterminant = tempNegativeDeterminant.pop();
                continue;
            }
            Vertex[] verts = null;
            switch (gd.type()) {
            case 1:
                final GData1 gd1 = ((GData1) gd);
                matrixMap.put(gd1, gd1.productMatrix);
                stack.push(gd);
                isCertified = localWinding != BFC.NOCERTIFY;
                tempWinding.push(localWinding);
                tempInvertNext.push(globalInvertNext);
                tempInvertNextFound.push(globalInvertNextFound);
                tempNegativeDeterminant.push(globalNegativeDeterminant);
                if (accumClip > 0) {
                    accumClip++;
                }
                globalInvertNextFound = false;
                localWinding = BFC.NOCERTIFY;
                globalNegativeDeterminant = globalNegativeDeterminant ^ gd1.negativeDeterminant;
                gd = gd1.myGData;
                continue;                
            case 6:
                if (!isCertified) {
                    continue;
                }
                if (accumClip > 0) {
                    switch (((GDataBFC) gd).type) {
                    case BFC.CCW_CLIP:
                        if (accumClip == 1)
                            accumClip = 0;
                        localWinding = BFC.CCW;
                        continue;
                    case BFC.CLIP:
                        if (accumClip == 1)
                            accumClip = 0;
                        continue;
                    case BFC.CW_CLIP:
                        if (accumClip == 1)
                            accumClip = 0;
                        localWinding = BFC.CW;
                        continue;
                    default:
                        continue;
                    }
                } else {
                    switch (((GDataBFC) gd).type) {
                    case BFC.CCW:
                        localWinding = BFC.CCW;
                        continue;
                    case BFC.CCW_CLIP:
                        localWinding = BFC.CCW;
                        continue;
                    case BFC.CW:
                        localWinding = BFC.CW;
                        continue;
                    case BFC.CW_CLIP:
                        localWinding = BFC.CW;
                        continue;
                    case BFC.INVERTNEXT:
                        boolean validState = false;
                        GData g = gd.next;
                        while (g != null && g.type() < 2) {
                            if (g.type() == 1) {
                                if (g.visible) validState = true;
                                break;
                            } else if (!g.toString().trim().isEmpty()) {
                                break;
                            }
                            g = g.next;
                        }
                        if (validState) {
                            globalInvertNext = !globalInvertNext;
                            globalInvertNextFound = true;
                        }
                        continue;
                    case BFC.NOCERTIFY:
                        localWinding = BFC.NOCERTIFY;
                        continue;
                    case BFC.NOCLIP:
                        if (accumClip == 0)
                            accumClip = 1;
                        continue;
                    default:
                        continue;
                    }
                }
            case 2:
                verts = lines.get(gd);
                if (verts != null) {
                    vertexMap.put(gd, verts);
                    dataInOrder.add(new GDataAndWinding(gd, localWinding, globalNegativeDeterminant, globalInvertNext));
                }
                continue;
            case 3:
                verts = triangles.get(gd);
                if (verts != null) {
                    vertexMap.put(gd, verts);
                    dataInOrder.add(new GDataAndWinding(gd, localWinding, globalNegativeDeterminant, globalInvertNext));
                }
                continue;
            case 4:
                verts = quads.get(gd);
                if (verts != null) {
                    vertexMap.put(gd, verts);
                    dataInOrder.add(new GDataAndWinding(gd, localWinding, globalNegativeDeterminant, globalInvertNext));
                }
                continue;
            case 5:
                verts = condlines.get(gd);
                if (verts != null) {
                    vertexMap.put(gd, verts);
                    dataInOrder.add(new GDataAndWinding(gd, localWinding, globalNegativeDeterminant, globalInvertNext));
                }
                continue;
            case 9:
                hasTEXMAP = true;
                continue;
            case 10:
                hasPNG = true;
                continue;
            default:
                continue;
            }
        }
        result[0] = hasPNG;
        result[1] = hasTEXMAP;
        return result;
    }
    
    private void normal(int offset, int times, float xn, float yn, float zn,
            float[] vertexData, int i) {
        for (int j = 0; j < times; j++) {
            int pos = (offset + i + j) * 10;
            vertexData[pos + 3] = xn;
            vertexData[pos + 4] = yn;
            vertexData[pos + 5] = zn;
        }
    }

    private void colourise(int offset, int times, float r, float g, float b,
            float a, float[] vertexData, int i) {
        for (int j = 0; j < times; j++) {
            int pos = (offset + i + j) * 10;
            vertexData[pos + 6] = r;
            vertexData[pos + 7] = g;
            vertexData[pos + 8] = b;
            vertexData[pos + 9] = a;
        }
    }
    
    private void colourise7(int offset, int times, float r, float g, float b,
            float a, float[] vertexData, int i) {
        for (int j = 0; j < times; j++) {
            int pos = (offset + i + j) * 7;
            vertexData[pos + 3] = r;
            vertexData[pos + 4] = g;
            vertexData[pos + 5] = b;
            vertexData[pos + 6] = a;
        }
    }
    
    private void colourise15(int offset, int times, float r, float g, float b,
            float[] vertexData, int i) {
        for (int j = 0; j < times; j++) {
            int pos = (offset + i + j) * 15;
            vertexData[pos + 12] = r;
            vertexData[pos + 13] = g;
            vertexData[pos + 14] = b;            
        }
    }

    private void pointAt(int offset, float x, float y, float z,
            float[] vertexData, int i) {
        int pos = (offset + i) * 10;
        vertexData[pos] = x;
        vertexData[pos + 1] = y;
        vertexData[pos + 2] = z;
    }
    
    private void pointAt7(int offset, float x, float y, float z,
            float[] vertexData, int i) {
        int pos = (offset + i) * 7;
        vertexData[pos] = x;
        vertexData[pos + 1] = y;
        vertexData[pos + 2] = z;
    }
    
    private void pointAt15(int offset, float x, float y, float z,
            float[] vertexData, int i) {
        int pos = (offset + i) * 15;
        vertexData[pos] = x;
        vertexData[pos + 1] = y;
        vertexData[pos + 2] = z;
    }
    
    private void controlPointAt15(int offset, int offset2, float x, float y, float z,
            float[] vertexData, int i) {
        int pos = (offset + i) * 15 + 3 * offset2;
        vertexData[pos + 3] = x;
        vertexData[pos + 4] = y;
        vertexData[pos + 5] = z;
    }

    class GDataAndWinding {
        final GData data;
        final byte winding;
        final boolean negativeDeterminant;
        final boolean invertNext;
        public GDataAndWinding(GData gd, byte bfc, boolean negDet, boolean iNext) {
            data = gd;
            winding = bfc;
            negativeDeterminant = negDet;
            invertNext = iNext;
        }
    }
}