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

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.nschmidt.ldparteditor.composites.Composite3D;
import org.nschmidt.ldparteditor.enums.MyLanguage;
import org.nschmidt.ldparteditor.enums.Threshold;
import org.nschmidt.ldparteditor.enums.View;
import org.nschmidt.ldparteditor.helpers.composite3d.IntersectorSettings;
import org.nschmidt.ldparteditor.helpers.composite3d.IsecalcSettings;
import org.nschmidt.ldparteditor.helpers.composite3d.SelectorSettings;
import org.nschmidt.ldparteditor.helpers.math.MathHelper;
import org.nschmidt.ldparteditor.helpers.math.ThreadsafeHashMap;
import org.nschmidt.ldparteditor.helpers.math.Vector3d;
import org.nschmidt.ldparteditor.helpers.math.Vector3dd;
import org.nschmidt.ldparteditor.helpers.math.Vector3dh;
import org.nschmidt.ldparteditor.i18n.I18n;
import org.nschmidt.ldparteditor.logger.NLogger;
import org.nschmidt.ldparteditor.shells.editor3d.Editor3DWindow;

public class VM12IntersectorAndIsecalc extends VM11HideShow {

    private final BigDecimal TOLERANCE = BigDecimal.ZERO; // new BigDecimal("0.00001"); //.00001
    private final BigDecimal ZEROT = BigDecimal.ZERO; //  = new BigDecimal("-0.00001");
    private final BigDecimal ONET = BigDecimal.ONE; //  = new BigDecimal("1.00001");

    private final BigDecimal TOLERANCER = new BigDecimal("0.00001"); //$NON-NLS-1$ .00001
    private final BigDecimal ZEROTR = new BigDecimal("-0.00001"); //$NON-NLS-1$
    private final BigDecimal ONETR = new BigDecimal("1.00001"); //$NON-NLS-1$

    protected VM12IntersectorAndIsecalc(DatFile linkedDatFile) {
        super(linkedDatFile);
    }

    public void isecalc(IsecalcSettings is) {

        if (linkedDatFile.isReadOnly()) return;

        final ArrayList<GData2> newLines = new ArrayList<GData2>();

        final Set<GData2> linesToDelete = new HashSet<GData2>();
        final Set<GData5> clinesToDelete = new HashSet<GData5>();

        final ArrayList<GData> surfsToParse;

        if (is.getScope() == 0) {
            surfsToParse = new ArrayList<GData>(triangles.size() + quads.size());
            surfsToParse.addAll(triangles.keySet());
            surfsToParse.addAll(quads.keySet());
        } else {
            surfsToParse = new ArrayList<GData>(selectedTriangles.size() + selectedQuads.size());
            surfsToParse.addAll(selectedTriangles);
            surfsToParse.addAll(selectedQuads);
        }

        clearSelection();

        final int surfsSize = surfsToParse.size();

        try
        {
            new ProgressMonitorDialog(Editor3DWindow.getWindow().getShell()).run(true, true, new IRunnableWithProgress()
            {
                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        monitor.beginTask(I18n.VM_SearchIntersection, IProgressMonitor.UNKNOWN);
                        for(int i = 0; i < surfsSize; i++) {
                            /* Check if the monitor has been canceled */
                            if (monitor.isCanceled()) break;
                            NLogger.debug(getClass(), "Checked {0}  of {1} surfaces.", i + 1, surfsSize); //$NON-NLS-1$
                            for(int j = i + 1; j < surfsSize; j++) {
                                GData s1 = surfsToParse.get(i);
                                GData s2 = surfsToParse.get(j);
                                if (isConnected2(s1, s2)) continue;
                                newLines.addAll(intersectionLines(clinesToDelete, linesToDelete, s1, s2));
                            }
                        }
                    }
                    finally
                    {
                        monitor.done();
                    }
                }
            });
        }
        catch (InvocationTargetException consumed) {
        } catch (InterruptedException consumed) {
        }

        if (!newLines.isEmpty()) {

            // Remove zero length lines
            BigDecimal EPSILON = new BigDecimal(".0001"); //$NON-NLS-1$
            for (Iterator<GData2> li = newLines.iterator(); li.hasNext();) {
                GData2 l = li.next();
                BigDecimal dx = l.X1.subtract(l.X2);
                BigDecimal dy = l.Y1.subtract(l.Y2);
                BigDecimal dz = l.Z1.subtract(l.Z2);
                BigDecimal len = dx.multiply(dx).add(dy.multiply(dy)).add(dz.multiply(dz));
                if (len.compareTo(EPSILON) <= 0) {
                    remove(l);
                    li.remove();
                }
            }

            final int lineCount = newLines.size();
            final BigDecimal SMALL = new BigDecimal("0.001"); //$NON-NLS-1$
            final BigDecimal SMALLANGLE = new BigDecimal("0.00001"); //$NON-NLS-1$
            final Vector3d zero = new Vector3d();

            // Merge lines with same directions
            int[] colin = new int[lineCount];
            int distline = 1;
            int flag = 0;
            for (int i=0; i < lineCount; i++)
            {
                if(colin[i] == 0)
                {
                    for (int j= i + 1; j < lineCount; j++)
                    {
                        flag=0;
                        Vector3d p11 = new Vector3d(newLines.get(i).X1, newLines.get(i).Y1, newLines.get(i).Z1);
                        Vector3d p12 = new Vector3d(newLines.get(i).X2, newLines.get(i).Y2, newLines.get(i).Z2);
                        Vector3d p21 = new Vector3d(newLines.get(j).X1, newLines.get(j).Y1, newLines.get(j).Z1);
                        Vector3d p22 = new Vector3d(newLines.get(j).X2, newLines.get(j).Y2, newLines.get(j).Z2);
                        Vector3d line1 = Vector3d.sub(p12, p11);
                        Vector3d line2 = Vector3d.sub(p22, p21);
                        Vector3d temp = Vector3d.cross(line1, line2);
                        BigDecimal angle = Vector3d.manhattan(temp, zero).divide(Vector3d.manhattan(p12, p11), Threshold.mc).divide(Vector3d.manhattan(p22, p21), Threshold.mc);
                        if (angle.compareTo(SMALLANGLE) < 0)
                        {
                            colin[i] = distline;
                            colin[j] = distline;
                            flag=1;
                        }
                    }
                    if((flag = 1) == 1) distline++;
                }
            }
            // printf("%d distinct direction(s)\n", distline-1);

            for (int i=0; i<lineCount-1; i++)
            {
                if(colin[i] > 0)
                {
                    flag=1;
                    while (flag==1)
                    {
                        flag=0;
                        for (int j=i+1; j<lineCount; j++)
                        {
                            if(colin[i]==colin[j])
                            {
                                Vector3d p11 = new Vector3d(newLines.get(i).X1, newLines.get(i).Y1, newLines.get(i).Z1);
                                Vector3d p12 = new Vector3d(newLines.get(i).X2, newLines.get(i).Y2, newLines.get(i).Z2);
                                Vector3d p21 = new Vector3d(newLines.get(j).X1, newLines.get(j).Y1, newLines.get(j).Z1);
                                Vector3d p22 = new Vector3d(newLines.get(j).X2, newLines.get(j).Y2, newLines.get(j).Z2);
                                if(Vector3d.manhattan(p11, p21).compareTo(SMALL) < 0 ||
                                        Vector3d.manhattan(p11, p22).compareTo(SMALL) < 0 ||
                                        Vector3d.manhattan(p12, p22).compareTo(SMALL) < 0 ||
                                        Vector3d.manhattan(p12, p21).compareTo(SMALL) < 0)
                                {
                                    int a=1,b=0;
                                    BigDecimal max, cur;
                                    max = Vector3d.manhattan(p11, p21);
                                    if ((cur = Vector3d.manhattan(p11, p22)).compareTo(max) > 0)
                                    {
                                        a=1; b=1;
                                        max = cur;
                                    }
                                    if ((cur = Vector3d.manhattan(p12, p21)).compareTo(max) > 0)
                                    {
                                        a=0; b=0;
                                        max = cur;
                                    }
                                    if ((cur = Vector3d.manhattan(p12, p22)).compareTo(max) > 0)
                                    {
                                        a=0; b=1;
                                    }
                                    GData2 l1 = newLines.get(i);
                                    GData2 l2 = newLines.get(j);
                                    GColour c = new GColour(24, View.line_Colour_r[0], View.line_Colour_g[0], View.line_Colour_b[0], 1f);
                                    GData2 nl;
                                    // SET(OutLine[i][a], OutLine[j][b]);
                                    if (a == 1) {
                                        if (b == 1) {
                                            nl = new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), l1.X1, l1.Y1, l1.Z1, l2.X2, l2.Y2, l2.Z2, View.DUMMY_REFERENCE, linkedDatFile, true);
                                        } else {
                                            nl = new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), l1.X1, l1.Y1, l1.Z1, l2.X1, l2.Y1, l2.Z1, View.DUMMY_REFERENCE, linkedDatFile, true);
                                        }
                                    } else {
                                        if (b == 1) {
                                            nl = new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), l2.X2, l2.Y2, l2.Z2, l1.X2, l1.Y2, l1.Z2, View.DUMMY_REFERENCE, linkedDatFile, true);
                                        } else {
                                            nl = new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), l2.X1, l2.Y1, l2.Z1, l1.X2, l1.Y2, l1.Z2, View.DUMMY_REFERENCE, linkedDatFile, true);
                                        }
                                    }
                                    remove(l1);
                                    newLines.remove(i);
                                    newLines.add(i, nl);
                                    colin[j]=-1;
                                    flag = 1;
                                }
                            }
                        }
                    }
                }
            }

            // Remove invalid collinear lines
            {
                int counter = 0;
                for (Iterator<GData2> li = newLines.iterator(); li.hasNext();) {
                    GData2 l = li.next();
                    if (colin[counter] < 0) {
                        remove(l);
                        li.remove();
                    }
                    counter++;
                }
            }

            // Append the lines
            for (GData2 line : newLines) {
                linkedDatFile.addToTailOrInsertAfterCursor(line);
            }

            // Round to 6 decimal places

            selectedLines.addAll(newLines);
            selectedData.addAll(selectedLines);

            roundSelection(6, 10, true, false, true, true, true);

            clearSelection();
            setModified_NoSync();
        }

        selectedLines.addAll(linesToDelete);
        selectedCondlines.addAll(clinesToDelete);
        selectedData.addAll(selectedLines);
        selectedData.addAll(selectedCondlines);
        delete(false, false);

        if (isModified()) {
            setModified(true, true);
        }

        validateState();
    }

    public void intersector(final IntersectorSettings ins, boolean syncWithTextEditor) {
        Composite3D c3d =  linkedDatFile.getLastSelectedComposite();
        NLogger.debug(getClass(), "Intersector - (C) Nils Schmidt 2015"); //$NON-NLS-1$
        NLogger.debug(getClass(), "======================"); //$NON-NLS-1$
        if (c3d != null) {

            final int[] isCancelled = new int[]{0};


            final Set<GData3> trisToHide = new HashSet<GData3>();
            final Set<GData4> quadsToHide = new HashSet<GData4>();

            final Set<GData2> linesToDelete = new HashSet<GData2>();
            final Set<GData3> trisToDelete = new HashSet<GData3>();
            final Set<GData4> quadsToDelete = new HashSet<GData4>();
            final Set<GData5> condlinesToDelete = new HashSet<GData5>();

            NLogger.debug(getClass(), "Get target surfaces to parse."); //$NON-NLS-1$

            final HashSet<GData> targetSurfs = new HashSet<GData>();
            {
                Set<GData3> tris = triangles.keySet();
                for (GData3 tri : tris) {
                    if (!hiddenData.contains(tri)) {
                        targetSurfs.add(tri);
                    }
                }
            }
            {
                Set<GData4> qs = quads.keySet();
                for (GData4 quad : qs) {
                    if (!hiddenData.contains(quad)) {
                        targetSurfs.add(quad);
                    }
                }
            }

            NLogger.debug(getClass(), "Cleanup the selection."); //$NON-NLS-1$

            for(Iterator<GData3> ti = selectedTriangles.iterator(); ti.hasNext();) {
                GData3 tri = ti.next();
                if (!lineLinkedToVertices.containsKey(tri)) {
                    ti.remove();
                }
            }
            for(Iterator<GData4> qi = selectedQuads.iterator(); qi.hasNext();) {
                GData4 quad = qi.next();
                if (!lineLinkedToVertices.containsKey(quad)) {
                    qi.remove();
                }
            }
            for(Iterator<GData2> li = selectedLines.iterator(); li.hasNext();) {
                GData2 line = li.next();
                if (!lineLinkedToVertices.containsKey(line)) {
                    li.remove();
                }
            }
            for(Iterator<GData5> ci = selectedCondlines.iterator(); ci.hasNext();) {
                GData5 condline = ci.next();
                if (!lineLinkedToVertices.containsKey(condline)) {
                    ci.remove();
                }
            }

            final ArrayList<GData> originObjects = new ArrayList<GData>();
            originObjects.addAll(selectedTriangles);
            originObjects.addAll(selectedQuads);
            originObjects.addAll(selectedLines);
            originObjects.addAll(selectedCondlines);

            // Remove adjacent non-selected surfaces from targetSurfs!
            {
                TreeSet<Vertex> verts = new TreeSet<Vertex>();
                for (GData g3 : selectedTriangles) {
                    Vertex[] verts2 = triangles.get(g3);
                    for (Vertex vertex : verts2) {
                        verts.add(vertex);
                    }
                }
                for (GData g4 : selectedQuads) {
                    Vertex[] verts2 = quads.get(g4);
                    for (Vertex vertex : verts2) {
                        verts.add(vertex);
                    }
                }
                for (Vertex vertex : verts) {
                    Collection<GData> surfs = getLinkedSurfaces(vertex);
                    for (GData g : surfs) {
                        switch (g.type()) {
                        case 3:
                            trisToHide.add((GData3) g);
                            break;
                        case 4:
                            quadsToHide.add((GData4) g);
                            break;
                        default:
                            break;
                        }
                    }
                    targetSurfs.removeAll(surfs);
                }
            }

            clearSelection();

            final ArrayList<IntersectionInfoWithColour> intersections = new ArrayList<IntersectionInfoWithColour>();
            final Set<GData2> newLines =  Collections.newSetFromMap(new ThreadsafeHashMap<GData2, Boolean>());
            final Set<GData3> newTriangles = Collections.newSetFromMap(new ThreadsafeHashMap<GData3, Boolean>());
            final Set<GData5> newCondlines =  Collections.newSetFromMap(new ThreadsafeHashMap<GData5, Boolean>());
            try
            {
                new ProgressMonitorDialog(Editor3DWindow.getWindow().getShell()).run(true, true, new IRunnableWithProgress()
                {
                    @Override
                    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try
                        {
                            monitor.beginTask(I18n.VM_Intersector, IProgressMonitor.UNKNOWN);

                            {

                                final Set<IntersectionInfoWithColour> intersectionSet = Collections.newSetFromMap(new ThreadsafeHashMap<IntersectionInfoWithColour, Boolean>());

                                final int iterations = originObjects.size();
                                final int chunks = View.NUM_CORES;
                                final Thread[] threads = new Thread[chunks];

                                final String surfCount = "/" + iterations;//$NON-NLS-1$
                                final AtomicInteger counter2 = new AtomicInteger(0);

                                int lastend = 0;
                                for (int j = 0; j < chunks; ++j) {
                                    final int[] start = new int[] { lastend };
                                    lastend = Math.round(iterations / chunks * (j + 1));
                                    final int[] end = new int[] { lastend };
                                    if (j == chunks - 1) {
                                        end[0] = iterations;
                                    }
                                    threads[j] = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            for (int k = start[0]; k < end[0]; k++) {
                                                monitor.subTask(counter2.toString() + surfCount);
                                                GData o = originObjects.get(k);
                                                /* Check if the monitor has been canceled */
                                                if (monitor.isCanceled()) {
                                                    isCancelled[0] = 1;
                                                    return;
                                                }
                                                counter2.incrementAndGet();
                                                IntersectionInfoWithColour ii = getIntersectionInfo(o, targetSurfs, ins);
                                                if (ii != null) {
                                                    intersectionSet.add(ii);
                                                    switch (o.type()) {
                                                    case 2:
                                                        linesToDelete.add((GData2) o);
                                                        break;
                                                    case 3:
                                                        trisToDelete.add((GData3) o);
                                                        break;
                                                    case 4:
                                                        quadsToDelete.add((GData4) o);
                                                        break;
                                                    case 5:
                                                        condlinesToDelete.add((GData5) o);
                                                        break;
                                                    default:
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    });
                                    threads[j].start();
                                }
                                boolean isRunning = true;
                                while (isRunning) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                    }
                                    isRunning = false;
                                    for (Thread thread : threads) {
                                        if (thread.isAlive())
                                            isRunning = true;
                                    }
                                }
                                intersections.addAll(intersectionSet);
                            }

                            if (isCancelled[0] > 0) return;

                            for (GData t : targetSurfs) {
                                switch (t.type()) {
                                case 3:
                                    trisToHide.add((GData3) t);
                                    break;
                                case 4:
                                    quadsToHide.add((GData4) t);
                                    break;
                                default:
                                    break;
                                }
                            }

                            NLogger.debug(getClass(), "Create new faces."); //$NON-NLS-1$

                            {
                                final int iterations = intersections.size();
                                final int chunks = View.NUM_CORES;
                                final Thread[] threads = new Thread[chunks];


                                final String maxIterations = "/" + iterations;//$NON-NLS-1$
                                final AtomicInteger counter2 = new AtomicInteger(0);

                                int lastend = 0;
                                for (int j = 0; j < chunks; ++j) {
                                    final int[] start = new int[] { lastend };
                                    lastend = Math.round(iterations / chunks * (j + 1));
                                    final int[] end = new int[] { lastend };
                                    if (j == chunks - 1) {
                                        end[0] = iterations;
                                    }
                                    threads[j] = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            for (int k = start[0]; k < end[0]; k++) {
                                                monitor.subTask(counter2.toString() + maxIterations);
                                                IntersectionInfoWithColour info = intersections.get(k);
                                                if (monitor.isCanceled()) {
                                                    isCancelled[0] = 2;
                                                    return;
                                                }
                                                counter2.incrementAndGet();

                                                final ArrayList<Vector3dd> av = info.getAllVertices();
                                                final ArrayList<GColour> cols = info.getColours();
                                                final ArrayList<Integer> ts = info.getIsLine();

                                                newTriangles.addAll(MathHelper.triangulatePointGroups(cols, av, ts, View.DUMMY_REFERENCE, linkedDatFile));
                                                newLines.addAll(MathHelper.triangulatePointGroups2(cols, av, ts, View.DUMMY_REFERENCE, linkedDatFile));
                                                newCondlines.addAll(MathHelper.triangulatePointGroups5(cols, av, ts, View.DUMMY_REFERENCE, linkedDatFile));
                                            }
                                        }
                                    });
                                    threads[j].start();
                                }
                                boolean isRunning = true;
                                while (isRunning) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                    }
                                    isRunning = false;
                                    for (Thread thread : threads) {
                                        if (thread.isAlive())
                                            isRunning = true;
                                    }
                                }
                            }

                        }
                        finally
                        {
                            monitor.done();
                        }
                    }
                });
            }
            catch (InvocationTargetException consumed) {
            } catch (InterruptedException consumed) {
            }


            NLogger.debug(getClass(), "Check for identical vertices and collinearity."); //$NON-NLS-1$
            final Set<GData3> trisToDelete2 = new HashSet<GData3>();
            {
                for (GData3 g3 : newTriangles) {
                    Vertex[] verts = triangles.get(g3);
                    Set<Vertex> verts2 = new TreeSet<Vertex>();
                    for (Vertex vert : verts) {
                        verts2.add(vert);
                    }
                    if (verts2.size() < 3 || g3.isCollinear()) {
                        trisToDelete2.add(g3);
                    }
                }
            }


            if (isCancelled[0] == 0) {
                NLogger.debug(getClass(), "Hide intersecting faces."); //$NON-NLS-1$

                if (ins.isHidingOther()) {
                    selectedTriangles.addAll(trisToHide);
                    selectedQuads.addAll(quadsToHide);
                    selectedData.addAll(selectedTriangles);
                    selectedData.addAll(selectedQuads);
                    selectedSubfiles.clear();
                    selectedSubfiles.addAll(vertexCountInSubfile.keySet());
                    selectedData.addAll(selectedSubfiles);
                    hideSelection();
                }

                clearSelection();

                NLogger.debug(getClass(), "Delete old selected objects."); //$NON-NLS-1$

                selectedLines.addAll(linesToDelete);
                selectedTriangles.addAll(trisToDelete);
                selectedQuads.addAll(quadsToDelete);
                selectedCondlines.addAll(condlinesToDelete);
                selectedData.addAll(selectedLines);
                selectedData.addAll(selectedTriangles);
                selectedData.addAll(selectedQuads);
                selectedData.addAll(selectedCondlines);
                delete(false, false);
            } else {
                clearSelection();
            }

            // Append the new data
            for (GData3 tri : newTriangles) {
                linkedDatFile.addToTailOrInsertAfterCursor(tri);
            }

            for (GData2 lin : newLines) {
                linkedDatFile.addToTailOrInsertAfterCursor(lin);
            }

            for (GData5 clin : newCondlines) {
                linkedDatFile.addToTailOrInsertAfterCursor(clin);
            }

            NLogger.debug(getClass(), "Delete new, but invalid objects."); //$NON-NLS-1$

            newTriangles.removeAll(trisToDelete2);
            selectedTriangles.addAll(trisToDelete2);
            selectedData.addAll(selectedTriangles);
            delete(false, false);

            // Round to 6 decimal places

            selectedLines.addAll(newLines);
            selectedTriangles.addAll(newTriangles);
            selectedCondlines.addAll(newCondlines);
            selectedData.addAll(selectedLines);
            selectedData.addAll(selectedTriangles);
            selectedData.addAll(selectedCondlines);

            NLogger.debug(getClass(), "Round."); //$NON-NLS-1$
            roundSelection(6, 10, true, false, true, true, true);

            clearSelection();
            if (syncWithTextEditor) {
                setModified(true, true);
            } else {
                setModified_NoSync();
            }

            NLogger.debug(getClass(), "Done."); //$NON-NLS-1$

            validateState();

        } else {
            NLogger.debug(getClass(), "No 3D view selected. Cancel process."); //$NON-NLS-1$
        }
    }

    private HashSet<GData2> intersectionLines(final Set<GData5> clinesToDelete, final Set<GData2> linesToDelete, GData g1, GData g2) {

        GColour c = new GColour(24, View.line_Colour_r[0], View.line_Colour_g[0], View.line_Colour_b[0], 1f);

        HashSet<GData2> result = new HashSet<GData2>();
        HashSet<Vector3d> points = new HashSet<Vector3d>();

        int t1 = g1.type();
        int t2 = g2.type();

        if (t1 == 3 && t2 == 3) {
            Vertex[] v1 = triangles.get(g1);
            Vertex[] v2 = triangles.get(g2);
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[0], v1[1], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[1], v1[2], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[2], v1[0], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[0], v2[1], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[1], v2[2], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[2], v2[0], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
        } else if (t1 == 4 && t2 == 4) {
            Vertex[] v1 = quads.get(g1);
            Vertex[] v2 = quads.get(g2);
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[0], v1[1], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[1], v1[2], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[2], v1[3], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[3], v1[0], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[0], v1[1], v2[2], v2[3], v2[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[1], v1[2], v2[2], v2[3], v2[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[2], v1[3], v2[2], v2[3], v2[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[3], v1[0], v2[2], v2[3], v2[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[0], v2[1], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[1], v2[2], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[2], v2[3], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[3], v2[0], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[0], v2[1], v1[2], v1[3], v1[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[1], v2[2], v1[2], v1[3], v1[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[2], v2[3], v1[2], v1[3], v1[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[3], v2[0], v1[2], v1[3], v1[0], p)) {
                    points.add(p);
                }
            }
        }

        if (t1 == 4 && t2 == 3) {
            GData g3 = g1;
            g1 = g2;
            g2 = g3;
            t1 = 3;
            t2 = 4;
        }

        if (t1 == 3 && t2 == 4) {
            Vertex[] v1 = triangles.get(g1);
            Vertex[] v2 = quads.get(g2);
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[0], v1[1], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[1], v1[2], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[2], v1[0], v2[0], v2[1], v2[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[0], v1[1], v2[2], v2[3], v2[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[1], v1[2], v2[2], v2[3], v2[0], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v1[2], v1[0], v2[2], v2[3], v2[0], p)) {
                    points.add(p);
                }
            }

            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[0], v2[1], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[1], v2[2], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[2], v2[3], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
            {
                Vector3d p = new Vector3d();
                if (intersectLineTriangle(v2[3], v2[0], v1[0], v1[1], v1[2], p)) {
                    points.add(p);
                }
            }
        }

        final BigDecimal EPSILON = new BigDecimal(".0001"); //$NON-NLS-1$
        for(Iterator<Vector3d> i = points.iterator(); i.hasNext(); ) {
            Vector3d p1 = i.next();
            for (Vector3d p2 : points) {
                if (!p1.equals(p2)) {
                    Vector3d p3 = Vector3d.sub(p1, p2);
                    BigDecimal md = p3.X.multiply(p3.X).add(p3.Y.multiply(p3.Y)).add(p3.Z.multiply(p3.Z));
                    if (md.compareTo(EPSILON) <= 0) {
                        i.remove();
                        break;
                    }
                }
            }
        }
        if (points.size() == 4) {
            ArrayList<Vector3d> points2 = new ArrayList<Vector3d>();
            points2.addAll(points);
            result.add(new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), points2.get(0).X, points2.get(0).Y, points2.get(0).Z, points2.get(1).X, points2.get(1).Y, points2.get(1).Z, View.DUMMY_REFERENCE, linkedDatFile, true));
            result.add(new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), points2.get(1).X, points2.get(1).Y, points2.get(1).Z, points2.get(2).X, points2.get(2).Y, points2.get(2).Z, View.DUMMY_REFERENCE, linkedDatFile, true));
            result.add(new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), points2.get(2).X, points2.get(2).Y, points2.get(2).Z, points2.get(3).X, points2.get(3).Y, points2.get(3).Z, View.DUMMY_REFERENCE, linkedDatFile, true));
            result.add(new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), points2.get(0).X, points2.get(0).Y, points2.get(0).Z, points2.get(3).X, points2.get(3).Y, points2.get(3).Z, View.DUMMY_REFERENCE, linkedDatFile, true));
        } else if (points.size() == 3) {
            ArrayList<Vector3d> points2 = new ArrayList<Vector3d>();
            points2.addAll(points);
            result.add(new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), points2.get(0).X, points2.get(0).Y, points2.get(0).Z, points2.get(1).X, points2.get(1).Y, points2.get(1).Z, View.DUMMY_REFERENCE, linkedDatFile, true));
            result.add(new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), points2.get(2).X, points2.get(2).Y, points2.get(2).Z, points2.get(1).X, points2.get(1).Y, points2.get(1).Z, View.DUMMY_REFERENCE, linkedDatFile, true));
            result.add(new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), points2.get(0).X, points2.get(0).Y, points2.get(0).Z, points2.get(2).X, points2.get(2).Y, points2.get(2).Z, View.DUMMY_REFERENCE, linkedDatFile, true));
        } else if (points.size() == 2) {
            ArrayList<Vector3d> points2 = new ArrayList<Vector3d>();
            points2.addAll(points);
            result.add(new GData2(c.getColourNumber(), c.getR(), c.getG(), c.getB(), c.getA(), points2.get(0).X, points2.get(0).Y, points2.get(0).Z, points2.get(1).X, points2.get(1).Y, points2.get(1).Z, View.DUMMY_REFERENCE, linkedDatFile, true));
        }

        return result;
    }

    private IntersectionInfoWithColour getIntersectionInfo(GData origin, HashSet<GData> targetSurfs, IntersectorSettings ins) {

        final BigDecimal MIN_DIST = new BigDecimal(".0001"); //$NON-NLS-1$

        final int ot = origin.type();

        final ArrayList<ArrayList<Vector3dd>> fixedIntersectionLines = new ArrayList<ArrayList<Vector3dd>>();
        final ArrayList<ArrayList<Vector3dd>> allLines = new ArrayList<ArrayList<Vector3dd>>();
        final ArrayList<Vector3dd> fixedVertices = new ArrayList<Vector3dd>();

        final HashMap<GData, ArrayList<Vector3dd>> intersections = new HashMap<GData, ArrayList<Vector3dd>>();

        Vertex[] ov = null;
        switch (ot) {
        case 2:
            ov = lines.get(origin);
            break;
        case 3:
            ov = triangles.get(origin);
            break;
        case 4:
            ov = quads.get(origin);
            break;
        case 5:
            ov = condlines.get(origin);
            break;
        default:
            return null;
        }

        if (ot == 2 || ot == 5) {

            if (getLineFaceIntersection(fixedVertices, targetSurfs, ov)) {

                final ArrayList<Vector3dd> resultVertices = new ArrayList<Vector3dd>();
                final ArrayList<GColour> resultColours = new ArrayList<GColour>();
                final ArrayList<Integer> resultIsLine = new ArrayList<Integer>();

                Vector3dd start = fixedVertices.get(0);

                Vector3d normal = new Vector3d(new BigDecimal(1.34), new BigDecimal(-.77), new BigDecimal(2));
                normal.normalise(normal);
                for (int i = 1; i < fixedVertices.size(); i++) {
                    Vector3dd end = fixedVertices.get(i);


                    if (ins.isColourise()) {

                        // Calculate pseudo mid-point
                        Vector3dd mid = new Vector3dd();
                        mid.setX(start.X.multiply(MathHelper.R1).add(end.X.multiply(MathHelper.R2.add(MathHelper.R3))));
                        mid.setY(start.Y.multiply(MathHelper.R1).add(end.Y.multiply(MathHelper.R2.add(MathHelper.R3))));
                        mid.setZ(start.Z.multiply(MathHelper.R1).add(end.Z.multiply(MathHelper.R2.add(MathHelper.R3))));

                        int intersectionCount = 0;


                        for (GData3 g3 : triangles.keySet()) {
                            Vertex[] v = triangles.get(g3);
                            if (intersectRayTriangle(mid, normal, new Vector3dd(v[0]), new Vector3dd(v[1]), new Vector3dd(v[2]))) {
                                intersectionCount += 1;
                            }
                        }
                        for (GData4 g4 : quads.keySet()) {
                            Vertex[] v = quads.get(g4);
                            if (
                                    intersectRayTriangle(mid, normal, new Vector3dd(v[0]), new Vector3dd(v[1]), new Vector3dd(v[2])) ||
                                    intersectRayTriangle(mid, normal, new Vector3dd(v[2]), new Vector3dd(v[3]), new Vector3dd(v[0]))) {
                                intersectionCount += 1;
                            }
                        }
                        resultVertices.add(start);
                        resultVertices.add(end);
                        if (ot == 2) {
                            resultIsLine.add(1);
                        } else {
                            resultVertices.add(new Vector3dd(ov[2]));
                            resultVertices.add(new Vector3dd(ov[3]));
                            resultIsLine.add(2);
                        }
                        if (intersectionCount == 1) {
                            resultColours.add(View.getLDConfigColour(0));
                        } else if (intersectionCount % 2 == 0) {
                            resultColours.add(View.getLDConfigColour(28));
                        } else {
                            resultColours.add(View.getLDConfigColour(1));
                        }

                    } else {
                        final float R, G, B, A;
                        final int CN;
                        resultVertices.add(start);
                        resultVertices.add(end);
                        if (ot == 2) {
                            GData2 origin2 = (GData2) origin;
                            CN = origin2.colourNumber;
                            R = origin2.r;
                            G = origin2.g;
                            B = origin2.b;
                            A = origin2.a;
                            resultIsLine.add(1);
                        } else {
                            GData5 origin2 = (GData5) origin;
                            CN = origin2.colourNumber;
                            R = origin2.r;
                            G = origin2.g;
                            B = origin2.b;
                            A = origin2.a;
                            resultVertices.add(new Vector3dd(ov[2]));
                            resultVertices.add(new Vector3dd(ov[3]));
                            resultIsLine.add(2);
                        }
                        resultColours.add(new GColour(CN, R, G, B, A));
                    }
                    start = end;
                }


                return new IntersectionInfoWithColour(resultColours, resultVertices, resultIsLine);
            } else {
                return null;
            }

        } else {
            for (GData targetSurf : targetSurfs) {
                final int tt = targetSurf.type();

                if (ot == 3 && tt == 3) {

                    Vertex[] tv = triangles.get(targetSurf);

                    getTriangleTriangleIntersection(intersections, targetSurf, ov, tv, ins, false, false);

                } else if (ot == 4 && tt == 4) {

                    Vertex[] tv = quads.get(targetSurf);

                    Vertex[] ov1 = new Vertex[]{ov[0], ov[1], ov[2]};
                    Vertex[] ov2 = new Vertex[]{ov[2], ov[3], ov[0]};
                    Vertex[] tv1 = new Vertex[]{tv[0], tv[1], tv[2]};
                    Vertex[] tv2 = new Vertex[]{tv[2], tv[3], tv[0]};

                    getTriangleTriangleIntersection(intersections, targetSurf, ov1, tv1, ins, true, true);
                    getTriangleTriangleIntersection(intersections, targetSurf, ov1, tv2, ins, true, true);
                    getTriangleTriangleIntersection(intersections, targetSurf, ov2, tv1, ins, true, true);
                    getTriangleTriangleIntersection(intersections, targetSurf, ov2, tv2, ins, true, true);

                } else if (ot == 4 && tt == 3) {
                    Vertex[] tv = triangles.get(targetSurf);

                    Vertex[] tv1 = new Vertex[]{tv[0], tv[1], tv[2]};
                    Vertex[] ov1 = new Vertex[]{ov[0], ov[1], ov[2]};
                    Vertex[] ov2 = new Vertex[]{ov[2], ov[3], ov[0]};

                    getTriangleTriangleIntersection(intersections, targetSurf, ov1, tv1, ins, true, false);
                    getTriangleTriangleIntersection(intersections, targetSurf, ov2, tv1, ins, true, false);

                } else if (ot == 3 && tt == 4) {

                    Vertex[] tv = quads.get(targetSurf);

                    Vertex[] ov1 = new Vertex[]{ov[0], ov[1], ov[2]};
                    Vertex[] tv1 = new Vertex[]{tv[0], tv[1], tv[2]};
                    Vertex[] tv2 = new Vertex[]{tv[2], tv[3], tv[0]};

                    getTriangleTriangleIntersection(intersections, targetSurf, ov1, tv1, ins, false, true);
                    getTriangleTriangleIntersection(intersections, targetSurf, ov1, tv2, ins, false, true);

                }
            }

            for (GData key : intersections.keySet()) {
                ArrayList<Vector3dd> line = intersections.get(key);
                if (line.size() > 1) {
                    fixedIntersectionLines.add(line);
                }
            }

            // Check intersections within the fixed intersection lines
            {
                ArrayList<ArrayList<Vector3dd>> linesToRemove = new ArrayList<ArrayList<Vector3dd>>();
                ArrayList<ArrayList<Vector3dd>> newLines = new ArrayList<ArrayList<Vector3dd>>();
                for (Iterator<ArrayList<Vector3dd>> iterator = fixedIntersectionLines.iterator(); iterator.hasNext();) {
                    ArrayList<Vector3dd> line = iterator.next();
                    ArrayList<Vector3d> intersect = new ArrayList<Vector3d>();
                    for (ArrayList<Vector3dd> line2 : fixedIntersectionLines) {
                        if (line2 != line) {
                            TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                            for(int l = 0; l < 2; l++) {
                                allVertices.add(line.get(l));
                                allVertices.add(line2.get(l));
                            }
                            if (allVertices.size() == 4) {
                                Vector3d ip = intersectLineLineSegmentUnidirectional2(line.get(0), line.get(1), line2.get(0), line2.get(1));
                                if (ip != null) {
                                    intersect.add(ip);
                                }
                            }
                        }
                    }
                    if (!intersect.isEmpty()) {
                        TreeMap<BigDecimal, Vector3d> linePoints = new TreeMap<BigDecimal, Vector3d>();
                        Vector3d start = line.get(0);
                        Vector3d end = line.get(1);
                        for (Vector3d v : intersect) {
                            BigDecimal dist = Vector3d.manhattan(v, start);
                            linePoints.put(dist, v);
                        }
                        BigDecimal dist = Vector3d.manhattan(end, start);
                        linePoints.put(dist, end);

                        for (BigDecimal d : linePoints.keySet()) {
                            end = linePoints.get(d);
                            ArrayList<Vector3dd> newLine = new ArrayList<Vector3dd>();
                            newLine.add(new Vector3dd(start));
                            newLine.add(new Vector3dd(end));
                            newLines.add(newLine);
                            start = end;
                        }
                        linesToRemove.add(line);
                    }
                }
                fixedIntersectionLines.removeAll(linesToRemove);
                fixedIntersectionLines.addAll(newLines);
            }

            final ArrayList<Vector3dd> resultVertices = new ArrayList<Vector3dd>();
            final ArrayList<GColour> resultColours = new ArrayList<GColour>();
            final ArrayList<Integer> resultIsLine = new ArrayList<Integer>();

            Vector3d originalNormal = null;

            switch (ot) {
            case 3:
            {
                fixedVertices.add(new Vector3dd(ov[0]).round());
                fixedVertices.add(new Vector3dd(ov[1]).round());
                fixedVertices.add(new Vector3dd(ov[2]).round());
                GData3 g3 = (GData3) origin;
                originalNormal = new Vector3d(new Vertex(g3.xn, g3.yn, g3.zn));
            }
            break;
            case 4:
            {
                fixedVertices.add(new Vector3dd(ov[0]).round());
                fixedVertices.add(new Vector3dd(ov[1]).round());
                fixedVertices.add(new Vector3dd(ov[2]).round());
                fixedVertices.add(new Vector3dd(ov[3]).round());
                GData4 g4 = (GData4) origin;
                originalNormal = new Vector3d(new Vertex(g4.xn, g4.yn, g4.zn));
            }
            break;
            default:
                return null;
            }

            {
                final TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                for (ArrayList<Vector3dd> l : fixedIntersectionLines) {
                    allVertices.add(l.get(0).round());
                    allVertices.add(l.get(1).round());
                    //                    resultVertices.add(l.get(0).round());
                    //                    resultVertices.add(l.get(1).round());
                    //                    resultColours.add(new GColour(-1, 0f, 1f, 0f, 1f));
                    //                    resultIsLine.add(1);
                }
                allVertices.removeAll(fixedVertices);
                fixedVertices.addAll(allVertices);
            }

            allLines.addAll(fixedIntersectionLines);
            if (!allLines.isEmpty()) {
                final int vc = fixedVertices.size();
                for (int i = 0; i < vc; i++) {
                    for (int j = 0; j < vc; j++) {
                        if (i == j) continue;
                        boolean intersect = false;
                        Vector3dd v1 = fixedVertices.get(i);
                        Vector3dd v2 = fixedVertices.get(j);
                        int lc = allLines.size();
                        for (int k = 0; k < lc; k++) {
                            ArrayList<Vector3dd> l = allLines.get(k);
                            Vector3dd v3 = l.get(0);
                            Vector3dd v4 = l.get(1);
                            if (!v1.equals(v3) && !v1.equals(v4) && !v2.equals(v3) && !v2.equals(v4) && intersectLineLineSegmentUnidirectional(v1, v2, v3, v4)) {
                                intersect = true;
                                break;
                            }
                            if (Vector3dd.manhattan(v1, v3).compareTo(MIN_DIST) < 0 && Vector3dd.manhattan(v2, v4).compareTo(MIN_DIST) < 0 ||
                                    Vector3dd.manhattan(v2, v3).compareTo(MIN_DIST) < 0 && Vector3dd.manhattan(v1, v4).compareTo(MIN_DIST) < 0) {
                                intersect = true;
                                break;
                            }
                        }
                        if (intersect) {
                            continue;
                        } else {
                            BigDecimal dist = Vector3dd.manhattan(v1, v2);
                            if (dist.compareTo(MIN_DIST) > 0) {
                                ArrayList<Vector3dd> nl = new ArrayList<Vector3dd>();
                                nl.add(v1);
                                nl.add(v2);
                                allLines.add(nl);
                            }
                        }
                    }
                }

                int lc = allLines.size();
                {
                    int removed = 0;
                    for (int i = 0; i + removed < lc; i++) {
                        for (int j = i + 1; j + removed < lc; j++) {
                            TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                            for(int l = 0; l < 2; l++) {
                                allVertices.add(allLines.get(i).get(l));
                                allVertices.add(allLines.get(j).get(l));
                            }
                            if (allVertices.size() == 2) {
                                removed += 1;
                                allLines.remove(j);
                            }
                        }
                    }

                    lc = allLines.size();

                    removed = 0;
                    for (int i = 0; i + removed < lc; i++) {
                        TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                        allVertices.add(allLines.get(i).get(0));
                        allVertices.add(allLines.get(i).get(1));
                        if (allVertices.size() == 1) {
                            removed += 1;
                            allLines.remove(i);
                        }
                    }
                }

                lc = allLines.size();

                for (int i = 0; i < lc; i++) {
                    for (int j = i + 1; j < lc; j++) {
                        for (int k = j + 1; k < lc; k++) {
                            TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                            for(int l = 0; l < 2; l++) {
                                allVertices.add(allLines.get(i).get(l).round());
                                allVertices.add(allLines.get(j).get(l).round());
                                allVertices.add(allLines.get(k).get(l).round());
                            }
                            if (allVertices.size() == 3) {
                                Vector3dd[] triVerts = new Vector3dd[3];
                                int l = 0;
                                for (Vector3dd v : allVertices) {
                                    triVerts[l] = v;
                                    l++;
                                }
                                boolean isInsideTriangle = false;
                                Vector3d normal = Vector3d.cross(Vector3d.sub(triVerts[2], triVerts[0]), Vector3d.sub(triVerts[1], triVerts[0]));
                                normal.normalise(normal);
                                for (Vector3dd fixed : fixedVertices) {
                                    if (fixed.equals(triVerts[0])) continue;
                                    if (fixed.equals(triVerts[1])) continue;
                                    if (fixed.equals(triVerts[2])) continue;
                                    if (intersectRayTriangle(fixed, normal, triVerts[0], triVerts[1], triVerts[2])) {
                                        isInsideTriangle = true;
                                        break;
                                    }
                                }
                                if (isInsideTriangle) continue;

                                // Check collinearity
                                {
                                    double angle;
                                    Vector3d vertexA = new Vector3d(triVerts[0]);
                                    Vector3d vertexB = new Vector3d(triVerts[1]);
                                    Vector3d vertexC = new Vector3d(triVerts[2]);
                                    Vector3d A = new Vector3d();
                                    Vector3d B = new Vector3d();
                                    Vector3d C = new Vector3d();
                                    Vector3d.sub(vertexB, vertexA, A);
                                    Vector3d.sub(vertexC, vertexB, B);
                                    Vector3d.sub(vertexC, vertexA, C);

                                    angle = Vector3d.angle(A, C);
                                    double sumAngle = angle;
                                    if (angle < Threshold.collinear_angle_minimum || angle > Threshold.collinear_angle_maximum) {
                                        continue;
                                    }

                                    A.negate();
                                    angle = Vector3d.angle(A, B);
                                    sumAngle = sumAngle + angle;
                                    if (angle < Threshold.collinear_angle_minimum || angle > Threshold.collinear_angle_maximum) {
                                        continue;
                                    }

                                    angle = 180.0 - sumAngle;
                                    if (angle < Threshold.collinear_angle_minimum || angle > Threshold.collinear_angle_maximum) {
                                        continue;
                                    }
                                }

                                if (MathHelper.directionOfVectors(normal, originalNormal) == 1) {
                                    resultVertices.add(triVerts[0]);
                                    resultVertices.add(triVerts[1]);
                                    resultVertices.add(triVerts[2]);
                                } else {
                                    resultVertices.add(triVerts[0]);
                                    resultVertices.add(triVerts[2]);
                                    resultVertices.add(triVerts[1]);
                                }

                                if (ins.isColourise()) {

                                    // Calculate pseudo mid-point
                                    Vector3dd mid = new Vector3dd();

                                    mid.setX(triVerts[0].X.multiply(MathHelper.R1).add(triVerts[1].X.multiply(MathHelper.R2)).add(triVerts[2].X.multiply(MathHelper.R3)));
                                    mid.setY(triVerts[0].Y.multiply(MathHelper.R1).add(triVerts[1].Y.multiply(MathHelper.R2)).add(triVerts[2].Y.multiply(MathHelper.R3)));
                                    mid.setZ(triVerts[0].Z.multiply(MathHelper.R1).add(triVerts[1].Z.multiply(MathHelper.R2)).add(triVerts[2].Z.multiply(MathHelper.R3)));

                                    int intersectionCount = 0;

                                    for (GData3 g3 : triangles.keySet()) {
                                        Vertex[] v = triangles.get(g3);
                                        if (intersectRayTriangle(mid, normal, new Vector3dd(v[0]), new Vector3dd(v[1]), new Vector3dd(v[2]))) {
                                            intersectionCount += 1;
                                        }
                                    }
                                    for (GData4 g4 : quads.keySet()) {
                                        Vertex[] v = quads.get(g4);
                                        if (
                                                intersectRayTriangle(mid, normal, new Vector3dd(v[0]), new Vector3dd(v[1]), new Vector3dd(v[2])) ||
                                                intersectRayTriangle(mid, normal, new Vector3dd(v[2]), new Vector3dd(v[3]), new Vector3dd(v[0]))) {
                                            intersectionCount += 1;
                                        }
                                    }

                                    if (intersectionCount == 1) {
                                        resultColours.add(View.getLDConfigColour(7));
                                    } else if (intersectionCount % 2 == 0) {
                                        resultColours.add(View.getLDConfigColour(14));
                                    } else {
                                        resultColours.add(View.getLDConfigColour(11));
                                    }

                                } else {
                                    final float R, G, B, A;
                                    final int CN;
                                    if (ot == 3) {
                                        GData3 origin2 = (GData3) origin;
                                        CN = origin2.colourNumber;
                                        R = origin2.r;
                                        G = origin2.g;
                                        B = origin2.b;
                                        A = origin2.a;
                                    } else {
                                        GData4 origin2 = (GData4) origin;
                                        CN = origin2.colourNumber;
                                        R = origin2.r;
                                        G = origin2.g;
                                        B = origin2.b;
                                        A = origin2.a;
                                    }
                                    resultColours.add(new GColour(CN, R, G, B, A));
                                }
                                resultIsLine.add(0);
                            }
                        }

                    }
                }

                if (resultVertices.isEmpty()) return null;
                return new IntersectionInfoWithColour(resultColours, resultVertices, resultIsLine);
            } else {
                return null;
            }
        }
    }

    private boolean getLineFaceIntersection(ArrayList<Vector3dd> fixedVertices, HashSet<GData> targetSurfs, Vertex[] ov) {

        TreeMap<BigDecimal, Vector3d> linePoints = new TreeMap<BigDecimal, Vector3d>();
        Vector3d start = new Vector3d(ov[0]);
        Vector3d end = new Vector3d(ov[1]);

        for (GData g : targetSurfs) {
            Vector3d intersection = new Vector3d();
            switch (g.type()) {
            case 3:
            {
                Vertex[] verts = triangles.get(g);
                if (intersectLineTriangle(ov[0], ov[1], verts[0], verts[1], verts[2], intersection)) {
                    fixedVertices.add(new Vector3dd(intersection));
                    BigDecimal dist = Vector3d.manhattan(intersection, start);
                    linePoints.put(dist, intersection);
                }
            }
            break;
            case 4:
            {
                Vertex[] verts = quads.get(g);
                if (
                        intersectLineTriangle(ov[0], ov[1], verts[0], verts[1], verts[2], intersection) ||
                        intersectLineTriangle(ov[0], ov[1], verts[2], verts[3], verts[0], intersection)) {
                    fixedVertices.add(new Vector3dd(intersection));
                    BigDecimal dist = Vector3d.manhattan(intersection, start);
                    linePoints.put(dist, intersection);
                }
            }
            break;
            default:
                break;
            }
        }

        if (fixedVertices.isEmpty()) {
            return false;
        } else {
            fixedVertices.clear();
            BigDecimal dist = Vector3d.manhattan(end, start);
            linePoints.put(BigDecimal.ZERO, start);
            linePoints.put(dist, end);
            for (BigDecimal d : linePoints.keySet()) {
                fixedVertices.add(new Vector3dd(linePoints.get(d)));
            }
            return true;
        }

    }

    private void getTriangleTriangleIntersection(HashMap<GData, ArrayList<Vector3dd>> intersections, GData target, Vertex[] ov, Vertex[] tv, IntersectorSettings ins, boolean originIsQuad, boolean targetIsQuad) {
        ArrayList<Vector3dd> result2 = null;
        if (intersections.containsKey(target)) {
            result2 = intersections.get(target);
        } else {
            result2 = new ArrayList<Vector3dd>();
            intersections.put(target, result2);
        }

        final TreeSet<Vector3dd> result = new TreeSet<Vector3dd>();

        {
            Vector3dd r = new Vector3dd();
            if (intersectLineTriangle(tv[0], tv[1], ov[0], ov[1], ov[2], r)) {
                result.add(r.round());
            }
        }
        {
            Vector3dd r = new Vector3dd();
            if (intersectLineTriangle(tv[1], tv[2], ov[0], ov[1], ov[2], r)) {
                result.add(r.round());
            }
        }
        if (!targetIsQuad) {
            Vector3dd r = new Vector3dd();
            if (intersectLineTriangle(tv[2], tv[0], ov[0], ov[1], ov[2], r)) {
                result.add(r.round());
            }
        }
        {
            Vector3dd r = new Vector3dd();
            if (intersectLineTriangle(ov[0], ov[1], tv[0], tv[1], tv[2], r)) {
                result.add(r.round());
            }
        }
        {
            Vector3dd r = new Vector3dd();
            if (intersectLineTriangle(ov[1], ov[2], tv[0], tv[1], tv[2], r)) {
                result.add(r.round());
            }
        }
        if (!originIsQuad) {
            Vector3dd r = new Vector3dd();
            if (intersectLineTriangle(ov[2], ov[0], tv[0], tv[1], tv[2], r)) {
                result.add(r.round());
            }
        }
        result.removeAll(result2);
        result2.addAll(result);
    }

    private boolean intersectLineLineSegmentUnidirectional(Vector3dd p, Vector3dd p2, Vector3dd q, Vector3dd q2) {


        Vector3d sp = Vector3d.sub(p2, p);
        Vector3d sq = Vector3d.sub(q2, q);
        Vector3d c = Vector3d.add(Vector3d.cross(sp, sq), p);
        Vector3d d = Vector3d.sub(p, Vector3d.cross(sp, sq));

        return intersectLineTriangle(new Vertex(q), new Vertex(q2), new Vertex(d), new Vertex(p2), new Vertex(c), c);

    }

    /**
     * FOR ISECALC/INTERSECTOR ONLY
     * @param p
     * @param q
     * @param a
     * @param b
     * @param c
     * @param r
     * @return
     */
    private boolean intersectLineTriangle(Vertex p, Vertex q, Vertex a, Vertex b, Vertex c, Vector3d r) {
        final BigDecimal TOLERANCE = new BigDecimal("0.00001"); //$NON-NLS-1$ .00001
        final BigDecimal ZEROT = new BigDecimal("-0.00001"); //$NON-NLS-1$
        final BigDecimal ONET = new BigDecimal("1.00001"); //$NON-NLS-1$
        BigDecimal diskr = BigDecimal.ZERO;
        BigDecimal inv_diskr = BigDecimal.ZERO;
        Vector3d vert0 = new Vector3d(a);
        Vector3d vert1 = new Vector3d(b);
        Vector3d vert2 = new Vector3d(c);
        Vector3d corner1 = Vector3d.sub(vert1, vert0);
        Vector3d corner2 = Vector3d.sub(vert2, vert0);
        Vector3d orig = new Vector3d(p);
        Vector3d dir = Vector3d.sub(new Vector3d(q), orig);
        BigDecimal len = dir.normalise(dir);
        Vector3d pvec = Vector3d.cross(dir, corner2);
        diskr = Vector3d.dotP(corner1, pvec);
        if (diskr.abs().compareTo(TOLERANCE) < 0)
            return false;
        inv_diskr = BigDecimal.ONE.divide(diskr, Threshold.mc);
        Vector3d tvec = Vector3d.sub(orig, vert0);
        BigDecimal u = Vector3d.dotP(tvec, pvec).multiply(inv_diskr);
        if (u.compareTo(ZEROT) < 0 || u.compareTo(ONET) > 0)
            return false;
        Vector3d qvec = Vector3d.cross(tvec, corner1);
        BigDecimal v = Vector3d.dotP(dir, qvec).multiply(inv_diskr);
        if (v.compareTo(ZEROT) < 0 || u.add(v).compareTo(ONET) > 0)
            return false;
        BigDecimal t = Vector3d.dotP(corner2, qvec).multiply(inv_diskr);
        if (t.compareTo(ZEROT) < 0 || t.compareTo(len.add(TOLERANCE)) > 0)
            return false;
        r.setX(orig.X.add(dir.X.multiply(t)));
        r.setY(orig.Y.add(dir.Y.multiply(t)));
        r.setZ(orig.Z.add(dir.Z.multiply(t)));
        return true;
    }

    protected Vector3d intersectLineLineSegmentUnidirectional2(Vector3dd p, Vector3dd p2, Vector3dd q, Vector3dd q2) {


        Vector3d sp = Vector3d.sub(p2, p);
        Vector3d sq = Vector3d.sub(q2, q);
        Vector3d c = Vector3d.add(Vector3d.cross(sp, sq), p);
        Vector3d d = Vector3d.sub(p, Vector3d.cross(sp, sq));

        return intersectLineTriangle(new Vertex(q), new Vertex(q2), new Vertex(d), new Vertex(p2), new Vertex(c), c) ? c : null;

    }

    public void lines2pattern() {

        if (linkedDatFile.isReadOnly()) return;

        final BigDecimal MIN_DIST = new BigDecimal(".0001"); //$NON-NLS-1$

        final Set<GData2> originalSelectionLines = new HashSet<GData2>();
        final Set<GData3> originalSelectionTriangles = new HashSet<GData3>();
        final Set<GData4> originalSelectionQuads = new HashSet<GData4>();
        final Set<GData3> newTriangles = new HashSet<GData3>();
        final Set<GData3> colouredTriangles = new HashSet<GData3>();

        final ArrayList<ArrayList<Vector3dd>> linesToParse = new ArrayList<ArrayList<Vector3dd>>();
        final ArrayList<ArrayList<Vector3dd>> colourLines = new ArrayList<ArrayList<Vector3dd>>();
        final ArrayList<ArrayList<Vector3dh>> linesToParseHashed = new ArrayList<ArrayList<Vector3dh>>();

        final HashMap<ArrayList<Vector3dd>, GColour> colours = new HashMap<ArrayList<Vector3dd>, GColour>();

        final int chunks = View.NUM_CORES;

        originalSelectionLines.addAll(selectedLines);
        originalSelectionTriangles.addAll(selectedTriangles);
        originalSelectionQuads.addAll(selectedQuads);

        final Vector3d originalNormal = new Vector3d(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE);

        // Verify
        try
        {
            new ProgressMonitorDialog(Editor3DWindow.getWindow().getShell()).run(true, true, new IRunnableWithProgress()
            {
                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        monitor.beginTask(I18n.VM_Lines2Pattern, IProgressMonitor.UNKNOWN);

                        for (GData3 g3 : selectedTriangles) {
                            TreeSet<Vertex> vs = new TreeSet<Vertex>();
                            Vertex[] verts = triangles.get(g3);
                            for (Vertex v : verts) {
                                vs.add(v);
                            }
                            if (vs.size() != 3) return;
                            {
                                ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                                l.add(new Vector3dd(verts[0]));
                                l.add(new Vector3dd(verts[1]));
                                linesToParse.add(l);
                            }
                            {
                                ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                                l.add(new Vector3dd(verts[1]));
                                l.add(new Vector3dd(verts[2]));
                                linesToParse.add(l);
                            }
                            {
                                ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                                l.add(new Vector3dd(verts[2]));
                                l.add(new Vector3dd(verts[0]));
                                linesToParse.add(l);
                            }
                        }

                        for (GData4 g4 : selectedQuads) {
                            TreeSet<Vertex> vs = new TreeSet<Vertex>();
                            Vertex[] verts = quads.get(g4);
                            for (Vertex v : verts) {
                                vs.add(v);
                            }
                            if (vs.size() != 4) return;
                            {
                                ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                                l.add(new Vector3dd(verts[0]));
                                l.add(new Vector3dd(verts[1]));
                                linesToParse.add(l);
                            }
                            {
                                ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                                l.add(new Vector3dd(verts[1]));
                                l.add(new Vector3dd(verts[2]));
                                linesToParse.add(l);
                            }
                            {
                                ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                                l.add(new Vector3dd(verts[2]));
                                l.add(new Vector3dd(verts[3]));
                                linesToParse.add(l);
                            }
                            {
                                ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                                l.add(new Vector3dd(verts[3]));
                                l.add(new Vector3dd(verts[0]));
                                linesToParse.add(l);
                            }
                            {
                                ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                                l.add(new Vector3dd(verts[1]));
                                l.add(new Vector3dd(verts[3]));
                                linesToParse.add(l);
                            }
                        }

                        TreeSet<Vertex> m1 = new TreeSet<Vertex>();
                        TreeSet<Vertex> m2 = new TreeSet<Vertex>();
                        for (GData2 g2 : selectedLines) {
                            Vertex[] verts = lines.get(g2);
                            for (Vertex v : verts) {
                                if (g2.colourNumber == 24) {
                                    if (m1.contains(v)) {
                                        m2.add(v);
                                    } else {
                                        m1.add(v);
                                    }
                                }
                            }
                            ArrayList<Vector3dd> l = new ArrayList<Vector3dd>();
                            l.add(new Vector3dd(verts[0]));
                            l.add(new Vector3dd(verts[1]));
                            if (g2.colourNumber == 24) {
                                linesToParse.add(l);
                            } else {
                                colourLines.add(l);
                                colours.put(l, new GColour(g2.colourNumber, g2.r, g2.g, g2.b, g2.a));
                            }
                        }
                        if (m1.size() != m2.size()) return;

                        BigDecimal seed = new BigDecimal("1.23456789"); //$NON-NLS-1$
                        BigDecimal seed2 = new BigDecimal("-1.832647382"); //$NON-NLS-1$
                        BigDecimal seed3 = new BigDecimal("1.427637292"); //$NON-NLS-1$
                        Vertex s = new Vertex(seed, seed2, seed3);
                        Vertex p1 = null;
                        Vertex p2 = null;
                        Vertex p3 = null;
                        for (Vertex vertex : m2) {
                            p1 = vertex;
                            break;
                        }
                        if (p1 == null) return;
                        for (Vertex vertex : m2) {
                            if (!vertex.equals(p1)) {
                                p2 = vertex;
                                break;
                            }
                        }
                        if (p2 == null) return;
                        for (Vertex vertex : m2) {
                            if (!vertex.equals(p1) && !vertex.equals(p2)) {
                                p3 = vertex;
                                break;
                            }
                        }
                        if (p3 == null) return;
                        Vector3d a = new Vector3d(p1.X.add(s.X), p1.Y.add(s.Y),p1.Z.add(s.Z));
                        Vector3d b = new Vector3d(p2.X.add(s.X), p2.Y.add(s.Y),p2.Z.add(s.Z));
                        Vector3d c = new Vector3d(p3.X.add(s.X), p3.Y.add(s.Y),p3.Z.add(s.Z));

                        Vector3d pOrigin = new Vector3d(p1);
                        Vector3d n = Vector3d.cross(Vector3d.sub(a, c), Vector3d.sub(b, c));
                        n.normalise(n);
                        originalNormal.setX(n.X);
                        originalNormal.setY(n.Y);
                        originalNormal.setZ(n.Z);
                        BigDecimal EPSILON = new BigDecimal("0.001"); //$NON-NLS-1$
                        for (Vertex vertex : m2) {
                            Vector3d vp = new Vector3d(vertex);
                            if (Vector3d.dotP(Vector3d.sub(pOrigin, vp), n).abs().compareTo(EPSILON) > 0) return;
                        }

                        if (monitor.isCanceled()) {
                            originalSelectionLines.clear();
                        }
                    }
                    finally
                    {
                        monitor.done();
                    }
                }
            });
        }
        catch (InvocationTargetException consumed) {
        } catch (InterruptedException consumed) {
        }

        if (originalSelectionLines.isEmpty()) return;
        clearSelection();

        // Calculate intersecting lines, if needed.
        {
            ArrayList<ArrayList<Vector3dd>> linesToRemove = new ArrayList<ArrayList<Vector3dd>>();
            ArrayList<ArrayList<Vector3dd>> newLines = new ArrayList<ArrayList<Vector3dd>>();
            for (Iterator<ArrayList<Vector3dd>> iterator = linesToParse.iterator(); iterator.hasNext();) {
                ArrayList<Vector3dd> line = iterator.next();
                ArrayList<Vector3d> intersect = new ArrayList<Vector3d>();
                for (ArrayList<Vector3dd> line2 : linesToParse) {
                    if (line2 != line) {
                        TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                        for(int l = 0; l < 2; l++) {
                            allVertices.add(line.get(l));
                            allVertices.add(line2.get(l));
                        }
                        if (allVertices.size() == 4) {
                            Vector3d ip = intersectLineLineSegmentUnidirectional2(line.get(0), line.get(1), line2.get(0), line2.get(1));
                            if (ip != null) {
                                intersect.add(ip);
                            }
                        }
                    }
                }
                if (!intersect.isEmpty()) {
                    TreeMap<BigDecimal, Vector3d> linePoints = new TreeMap<BigDecimal, Vector3d>();
                    Vector3d start = line.get(0);
                    Vector3d end = line.get(1);
                    for (Vector3d v : intersect) {
                        BigDecimal dist = Vector3d.manhattan(v, start);
                        linePoints.put(dist, v);
                    }
                    BigDecimal dist = Vector3d.manhattan(end, start);
                    linePoints.put(dist, end);

                    for (BigDecimal d : linePoints.keySet()) {
                        end = linePoints.get(d);
                        ArrayList<Vector3dd> newLine = new ArrayList<Vector3dd>();
                        newLine.add(new Vector3dd(start));
                        newLine.add(new Vector3dd(end));
                        newLines.add(newLine);
                        start = end;
                    }
                    linesToRemove.add(line);
                }
            }
            linesToParse.removeAll(linesToRemove);
            linesToParse.addAll(newLines);
        }

        final ArrayList<Vector3dd> resultVertices = new ArrayList<Vector3dd>();
        final ArrayList<GColour> resultColours = new ArrayList<GColour>();
        final ArrayList<Integer> resultIsLine = new ArrayList<Integer>();

        final Set<ArrayList<Vector3dd>> colourLines2 = Collections.newSetFromMap(new ThreadsafeHashMap<ArrayList<Vector3dd>, Boolean>());
        final ThreadsafeHashMap<ArrayList<Vector3dd> , GColour> colours2 = new ThreadsafeHashMap<ArrayList<Vector3dd>, GColour>();
        final Thread[] colourThreads = new Thread[chunks];

        // Spread coloured lines
        {

            try
            {
                new ProgressMonitorDialog(Editor3DWindow.getWindow().getShell()).run(true, true, new IRunnableWithProgress()
                {
                    @Override
                    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try
                        {

                            if (!colourLines.isEmpty()) {

                                final ArrayList<Vector3dd> fixedVertices = new ArrayList<Vector3dd>();
                                final ArrayList<Vector3dd> colourVertices = new ArrayList<Vector3dd>();
                                final TreeMap<Vector3dd, GColour> vertexColour = new TreeMap<Vector3dd, GColour>();
                                {
                                    final TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                                    for (ArrayList<Vector3dd> l : linesToParse) {
                                        allVertices.add(l.get(0).round());
                                        allVertices.add(l.get(1).round());
                                    }
                                    for (ArrayList<Vector3dd> l : colourLines) {
                                        Vector3dd vc1 = l.get(0).round();
                                        Vector3dd vc2 = l.get(1).round();
                                        if (!vertexColour.containsKey(vc1)) {
                                            vertexColour.put(vc1, colours.get(l));
                                        } else {
                                            GColour gc = vertexColour.get(vc1);
                                            GColour gc2 = colours.get(l);
                                            if (gc.getColourNumber() != gc2.getColourNumber()) vertexColour.remove(vc1);
                                        }
                                        if (!vertexColour.containsKey(vc2)) {
                                            vertexColour.put(vc2, colours.get(l));
                                        } else {
                                            GColour gc = vertexColour.get(vc2);
                                            GColour gc2 = colours.get(l);
                                            if (gc.getColourNumber() != gc2.getColourNumber()) vertexColour.remove(vc2);
                                        }
                                        colourVertices.add(vc1);
                                        colourVertices.add(vc2);
                                    }
                                    fixedVertices.addAll(allVertices);
                                }

                                final ArrayList<ArrayList<Vector3dd>> fixedLinesToParse = new ArrayList<ArrayList<Vector3dd>>();
                                fixedLinesToParse.addAll(linesToParse);

                                final int vc = colourVertices.size();
                                final int vc2 = fixedVertices.size();

                                final AtomicInteger counter2 = new AtomicInteger(0);

                                for (int j = 0; j < chunks; ++j) {
                                    final int[] start = new int[] { j };
                                    colourThreads[j] = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int counter = start[0];
                                            for (int i = 0; i < vc; i++) {
                                                if (counter == 0) {
                                                    counter = chunks;
                                                    counter2.incrementAndGet();
                                                    if (monitor.isCanceled()) {
                                                        return;
                                                    }
                                                    Vector3dd v1 = colourVertices.get(i);
                                                    for (int j = 0; j < vc2; j++) {
                                                        boolean intersect = false;
                                                        Vector3dd v2 = fixedVertices.get(j);
                                                        Vector3d sp = Vector3dd.sub(v2, v1);
                                                        Vector3d dir = new Vector3d();
                                                        BigDecimal len = sp.normalise(dir);
                                                        int lc = fixedLinesToParse.size();
                                                        for (int k = 0; k < lc; k++) {
                                                            ArrayList<Vector3dd> l = fixedLinesToParse.get(k);
                                                            Vector3dd v3 = l.get(0);
                                                            Vector3dd v4 = l.get(1);
                                                            if (!v1.equals(v3) && !v1.equals(v4) && !v2.equals(v3) && !v2.equals(v4) && intersectLineLineSegmentUnidirectionalFast(v1, v2, sp, dir, len, v3, v4)) {
                                                                intersect = true;
                                                                break;
                                                            }
                                                        }
                                                        if (intersect) {
                                                            continue;
                                                        } else {
                                                            BigDecimal dist = Vector3dd.manhattan(v1, v2);
                                                            if (dist.compareTo(MIN_DIST) > 0) {
                                                                if (vertexColour.containsKey(v1) && vertexColour.get(v1) != null) {
                                                                    ArrayList<Vector3dd> nl = new ArrayList<Vector3dd>();
                                                                    nl.add(v1);
                                                                    nl.add(v2);
                                                                    colours2.put(nl, vertexColour.get(v1));
                                                                    colourLines2.add(nl);
                                                                } else if (vertexColour.containsKey(v2) && vertexColour.get(v2) != null) {
                                                                    ArrayList<Vector3dd> nl = new ArrayList<Vector3dd>();
                                                                    nl.add(v1);
                                                                    nl.add(v2);
                                                                    colours2.put(nl, vertexColour.get(v2));
                                                                    colourLines2.add(nl);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                counter -= 1;
                                            }
                                        }
                                    });
                                    colourThreads[j].start();
                                }
                            }
                        } finally {
                            monitor.done();
                        }
                    }
                });
            }
            catch (InvocationTargetException consumed) {
            } catch (InterruptedException consumed) {
            }
        }

        final ArrayList<Vector3dd> fixedVertices = new ArrayList<Vector3dd>();
        final ArrayList<Vector3dh> fixedVertices2 = new ArrayList<Vector3dh>();

        {
            final TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
            for (ArrayList<Vector3dd> l : linesToParse) {
                allVertices.add(l.get(0).round());
                allVertices.add(l.get(1).round());
            }
            fixedVertices.addAll(allVertices);
        }

        if (!linesToParse.isEmpty()) {

            final ThreadsafeHashMap<Vector3dh, HashSet<Vector3dh>> neighbours = new ThreadsafeHashMap<Vector3dh, HashSet<Vector3dh>>();
            try
            {
                new ProgressMonitorDialog(Editor3DWindow.getWindow().getShell()).run(true, true, new IRunnableWithProgress()
                {
                    @Override
                    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try
                        {
                            monitor.beginTask(I18n.VM_Lines2Pattern, IProgressMonitor.UNKNOWN);

                            final Thread[] threads = new Thread[1];

                            {
                                TreeMap<Vector3dd, Vector3dh> hashedRelation = new TreeMap<Vector3dd, Vector3dh>();
                                for (Vector3dd v : fixedVertices) {
                                    Vector3dh vh;
                                    if (hashedRelation.containsKey(v)) {
                                        vh = hashedRelation.get(v);
                                    } else {
                                        vh = new Vector3dh(v);
                                        hashedRelation.put(v, vh);
                                    }

                                    fixedVertices2.add(vh);
                                }
                                for (ArrayList<Vector3dd> l : linesToParse) {

                                    Vector3dd v1nh = l.get(0).round();
                                    Vector3dd v2nh = l.get(1).round();

                                    Vector3dh v1;
                                    Vector3dh v2;

                                    if (hashedRelation.containsKey(v1nh)) {
                                        v1 = hashedRelation.get(v1nh);
                                    } else {
                                        v1 = new Vector3dh(v1nh);
                                        hashedRelation.put(v1nh, v1);
                                    }

                                    if (hashedRelation.containsKey(v2nh)) {
                                        v2 = hashedRelation.get(v2nh);
                                    } else {
                                        v2 = new Vector3dh(v2nh);
                                        hashedRelation.put(v2nh, v2);
                                    }

                                    ArrayList<Vector3dh> newline = new ArrayList<Vector3dh>();
                                    newline.add(v1);
                                    newline.add(v2);
                                    linesToParseHashed.add(newline);

                                }
                            }

                            final int vc = fixedVertices2.size();
                            final String vertCount = "/" + vc + ")"; //$NON-NLS-1$ //$NON-NLS-2$

                            threads[0] = new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    for (int i = 0; i < vc; i++) {

                                        Object[] messageArguments = {i, vertCount};
                                        MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
                                        formatter.setLocale(MyLanguage.LOCALE);
                                        formatter.applyPattern(I18n.VM_DetectNewEdges);

                                        monitor.subTask(formatter.format(messageArguments));

                                        if (monitor.isCanceled()) {
                                            break;
                                        }

                                        Vector3dh v1 = fixedVertices2.get(i);
                                        for (int j = i + 1; j < vc; j++) {
                                            boolean intersect = false;
                                            Vector3dh v2 = fixedVertices2.get(j);

                                            Vector3d sp = Vector3dd.sub(v2, v1);
                                            Vector3d dir = new Vector3d();
                                            BigDecimal len = sp.normalise(dir);
                                            Iterator<ArrayList<Vector3dh>> li = linesToParseHashed.iterator();
                                            while (li.hasNext()) {
                                                ArrayList<Vector3dh> l = li.next();
                                                Vector3dh v3 = l.get(0);
                                                Vector3dh v4 = l.get(1);
                                                if (!v1.equals(v3) && !v1.equals(v4) && !v2.equals(v3) && !v2.equals(v4)) {
                                                    if (intersectLineLineSegmentUnidirectionalFast(v1, v2, sp, dir, len,  v3, v4)) {
                                                        intersect = true;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (!intersect) {
                                                BigDecimal dist = Vector3dd.manhattan(v1, v2);
                                                if (dist.compareTo(MIN_DIST) > 0) {
                                                    ArrayList<Vector3dh> nl = new ArrayList<Vector3dh>();
                                                    nl.add(v1);
                                                    nl.add(v2);
                                                    linesToParseHashed.add(nl);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                            threads[0].start();
                            boolean isRunning = true;
                            while (isRunning) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                }
                                isRunning = false;
                                if (threads[0].isAlive())
                                    isRunning = true;
                            }
                            if (!colourLines.isEmpty()) {
                                isRunning = true;
                                while (isRunning) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                    }
                                    isRunning = false;
                                    for (Thread thread : colourThreads) {
                                        if (thread.isAlive())
                                            isRunning = true;
                                    }
                                }
                            }
                            if (monitor.isCanceled()) {
                                selectedLines.addAll(originalSelectionLines);
                                selectedTriangles.addAll(originalSelectionTriangles);
                                selectedQuads.addAll(originalSelectionQuads);
                                selectedData.addAll(originalSelectionTriangles);
                                selectedData.addAll(originalSelectionQuads);
                                selectedData.addAll(originalSelectionLines);
                                originalSelectionLines.clear();
                                return;
                            } else {
                                colourLines.addAll(colourLines2);
                                colours.putAll(colours2);
                                linesToParse.clear();
                                fixedVertices2.clear();
                                for (ArrayList<Vector3dh> l : linesToParseHashed) {
                                    ArrayList<Vector3dd> nl = new ArrayList<Vector3dd>();
                                    nl.add(new Vector3dd(l.get(0)));
                                    nl.add(new Vector3dd(l.get(1)));
                                    linesToParse.add(nl);
                                }
                                linesToParseHashed.clear();
                            }
                        } finally {
                            monitor.done();
                        }
                    }
                });
            }
            catch (InvocationTargetException consumed) {
            } catch (InterruptedException consumed) {
            }

            if (originalSelectionLines.isEmpty()) return;

            int lc = linesToParse.size();
            {
                int removed = 0;
                for (int i = 0; i + removed < lc; i++) {
                    for (int j = i + 1; j + removed < lc; j++) {
                        TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                        for(int l = 0; l < 2; l++) {
                            allVertices.add(linesToParse.get(i).get(l));
                            allVertices.add(linesToParse.get(j).get(l));
                        }
                        if (allVertices.size() == 2) {
                            removed += 1;
                            linesToParse.remove(j);
                        }
                    }
                }

                lc = linesToParse.size();

                removed = 0;
                for (int i = 0; i + removed < lc; i++) {
                    TreeSet<Vector3dd> allVertices = new TreeSet<Vector3dd>();
                    allVertices.add(linesToParse.get(i).get(0));
                    allVertices.add(linesToParse.get(i).get(1));
                    if (allVertices.size() == 1) {
                        removed += 1;
                        linesToParse.remove(i);
                    }
                }

                lc = linesToParse.size();

                HashSet<Vector3dh> m1 = new HashSet<Vector3dh>();
                HashSet<Vector3dh> m2 = new HashSet<Vector3dh>();
                HashSet<Vector3dh> m3 = new HashSet<Vector3dh>();
                TreeMap<Vector3dd, Vector3dh> hashedRelation = new TreeMap<Vector3dd, Vector3dh>();
                for (int i = 0; i < lc; i++) {
                    Vector3dd v1nh = linesToParse.get(i).get(0).round();
                    Vector3dd v2nh = linesToParse.get(i).get(1).round();

                    Vector3dh v1;
                    Vector3dh v2;

                    if (hashedRelation.containsKey(v1nh)) {
                        v1 = hashedRelation.get(v1nh);
                    } else {
                        v1 = new Vector3dh(v1nh);
                        hashedRelation.put(v1nh, v1);
                    }

                    if (hashedRelation.containsKey(v2nh)) {
                        v2 = hashedRelation.get(v2nh);
                    } else {
                        v2 = new Vector3dh(v2nh);
                        hashedRelation.put(v2nh, v2);
                    }

                    ArrayList<Vector3dh> newline = new ArrayList<Vector3dh>();
                    newline.add(v1);
                    newline.add(v2);
                    linesToParseHashed.add(newline);

                    if (neighbours.containsKey(v1)) {
                        neighbours.get(v1).add(v2);
                    } else {
                        neighbours.put(v1, new HashSet<Vector3dh>());
                        neighbours.get(v1).add(v2);
                    }
                    if (neighbours.containsKey(v2)) {
                        neighbours.get(v2).add(v1);
                    } else {
                        neighbours.put(v2, new HashSet<Vector3dh>());
                        neighbours.get(v2).add(v1);
                    }
                    if (m1.contains(v1)) {
                        if (m2.contains(v1)) {
                            if (!m3.contains(v1)) {
                                m3.add(v1);
                            }
                        } else {
                            m2.add(v1);
                        }
                    } else {
                        m1.add(v1);
                    }
                    if (m1.contains(v2)) {
                        if (m2.contains(v2)) {
                            if (!m3.contains(v2)) {
                                m3.add(v2);
                            }
                        } else {
                            m2.add(v2);
                        }
                    } else {
                        m1.add(v2);
                    }
                }
                for (Vector3dd v : fixedVertices) {
                    fixedVertices2.add(hashedRelation.get(v));
                }
                m2.removeAll(m3);
                fixedVertices2.removeAll(m2);
            }

            try
            {
                new ProgressMonitorDialog(Editor3DWindow.getWindow().getShell()).run(true, true, new IRunnableWithProgress()
                {
                    @Override
                    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try
                        {
                            monitor.beginTask(I18n.VM_Lines2Pattern, IProgressMonitor.UNKNOWN);

                            final int lc = linesToParseHashed.size();

                            final Thread[] threads = new Thread[chunks];

                            final String vertCount = "/" + lc + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                            final AtomicInteger counter2 = new AtomicInteger(0);

                            final Lock rlock = new ReentrantLock(true);

                            for (int t = 0; t < chunks; ++t) {
                                final int[] start = new int[] { t };
                                threads[t] = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        int counter = start[0];
                                        HashSet<Vector3dh> allVertices = new HashSet<Vector3dh>();
                                        Vector3d normal = null;
                                        for (int i = 0; i < lc; i++) {
                                            if (counter == 0) {
                                                counter = chunks;

                                                Object[] messageArguments = {counter2.toString(), vertCount};
                                                MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
                                                formatter.setLocale(MyLanguage.LOCALE);
                                                formatter.applyPattern(I18n.VM_Triangulate);

                                                monitor.subTask(formatter.format(messageArguments));
                                                counter2.incrementAndGet();
                                                if (monitor.isCanceled()) {
                                                    return;
                                                }
                                                for (int j = i + 1; j < lc; j++) {
                                                    for (int k = j + 1; k < lc; k++) {
                                                        for(int l = 0; l < 2; l++) {
                                                            allVertices.add(linesToParseHashed.get(i).get(l));
                                                            allVertices.add(linesToParseHashed.get(j).get(l));
                                                            allVertices.add(linesToParseHashed.get(k).get(l));
                                                        }
                                                        if (allVertices.size() == 3) {
                                                            Vector3dh[] triVerts = new Vector3dh[3];
                                                            int l = 0;
                                                            for (Vector3dh v : allVertices) {
                                                                triVerts[l] = v;
                                                                l++;
                                                            }
                                                            allVertices.clear();
                                                            boolean isInsideTriangle = false;
                                                            if (normal == null) {
                                                                normal = Vector3d.cross(Vector3d.sub(triVerts[2], triVerts[0]), Vector3d.sub(triVerts[1], triVerts[0]));
                                                                normal.normalise(normal);
                                                            }
                                                            for (Vector3dh fixed : fixedVertices2) {
                                                                if (fixed.equals(triVerts[0])) continue;
                                                                if (fixed.equals(triVerts[1])) continue;
                                                                if (fixed.equals(triVerts[2])) continue;
                                                                Set<Vector3dh> n1 = neighbours.get(triVerts[0]);
                                                                Set<Vector3dh> n2 = neighbours.get(triVerts[1]);
                                                                Set<Vector3dh> n3 = neighbours.get(triVerts[2]);
                                                                int nc = 0;
                                                                if (n1.contains(fixed)) nc += 1;
                                                                if (n2.contains(fixed)) nc += 1;
                                                                if (n3.contains(fixed)) nc += 1;
                                                                if (nc > 1) {
                                                                    if (intersectRayTriangle(fixed, normal, triVerts[0], triVerts[1], triVerts[2])) {
                                                                        isInsideTriangle = true;
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            if (isInsideTriangle) continue;

                                                            // Check collinearity
                                                            {
                                                                double angle;
                                                                Vector3d vertexA = new Vector3d(triVerts[0]);
                                                                Vector3d vertexB = new Vector3d(triVerts[1]);
                                                                Vector3d vertexC = new Vector3d(triVerts[2]);
                                                                Vector3d A = new Vector3d();
                                                                Vector3d B = new Vector3d();
                                                                Vector3d C = new Vector3d();
                                                                Vector3d.sub(vertexB, vertexA, A);
                                                                Vector3d.sub(vertexC, vertexB, B);
                                                                Vector3d.sub(vertexC, vertexA, C);

                                                                angle = Vector3d.angle(A, C);
                                                                double sumAngle = angle;
                                                                if (angle < Threshold.collinear_angle_minimum || angle > Threshold.collinear_angle_maximum) {
                                                                    continue;
                                                                }

                                                                A.negate();
                                                                angle = Vector3d.angle(A, B);
                                                                sumAngle = sumAngle + angle;
                                                                if (angle < Threshold.collinear_angle_minimum || angle > Threshold.collinear_angle_maximum) {
                                                                    continue;
                                                                }

                                                                angle = 180.0 - sumAngle;
                                                                if (angle < Threshold.collinear_angle_minimum || angle > Threshold.collinear_angle_maximum) {
                                                                    continue;
                                                                }
                                                            }

                                                            {
                                                                HashSet<ArrayList<Vector3dd>> threeLines = new HashSet<ArrayList<Vector3dd>>();
                                                                threeLines.add(linesToParse.get(i));
                                                                threeLines.add(linesToParse.get(j));
                                                                threeLines.add(linesToParse.get(k));
                                                                ArrayList<Vector3dd> intersected = null;
                                                                for (Iterator<ArrayList<Vector3dd>> iterator = threeLines.iterator(); iterator.hasNext();) {
                                                                    ArrayList<Vector3dd> line = iterator.next();
                                                                    Vector3dd v1 = line.get(0);
                                                                    Vector3dd v2 = line.get(1);
                                                                    Vector3d sp = Vector3dd.sub(v2, v1);
                                                                    Vector3d dir = new Vector3d();
                                                                    BigDecimal len = sp.normalise(dir);
                                                                    for (ArrayList<Vector3dd> line2 : colourLines) {
                                                                        if (line2 != line) {
                                                                            TreeSet<Vector3dd> allVertices1 = new TreeSet<Vector3dd>();
                                                                            for(int l1 = 0; l1 < 2; l1++) {
                                                                                allVertices1.add(line.get(l1));
                                                                                allVertices1.add(line2.get(l1));
                                                                            }
                                                                            if (allVertices1.size() == 4) {
                                                                                if (intersectLineLineSegmentUnidirectionalFast(v1, v2, sp, dir, len, line2.get(0), line2.get(1))) {
                                                                                    intersected = line2;
                                                                                    break;
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    if (intersected != null) {
                                                                        break;
                                                                    }
                                                                }

                                                                rlock.lock();
                                                                if (MathHelper.directionOfVectors(Vector3d.cross(Vector3d.sub(triVerts[2], triVerts[0]), Vector3d.sub(triVerts[1], triVerts[0])), originalNormal) == 1) {
                                                                    resultVertices.add(triVerts[0]);
                                                                    resultVertices.add(triVerts[1]);
                                                                    resultVertices.add(triVerts[2]);
                                                                } else {
                                                                    resultVertices.add(triVerts[0]);
                                                                    resultVertices.add(triVerts[2]);
                                                                    resultVertices.add(triVerts[1]);
                                                                }

                                                                if (intersected != null) {
                                                                    resultColours.add(colours.get(intersected) != null ? colours.get(intersected) : View.getLDConfigColour(16));
                                                                } else {
                                                                    resultColours.add(View.getLDConfigColour(16));
                                                                }
                                                                resultIsLine.add(0);
                                                                rlock.unlock();
                                                            }
                                                        } else {
                                                            allVertices.clear();
                                                        }
                                                    }
                                                }
                                            }
                                            counter -= 1;
                                        }
                                    }
                                });
                                threads[t].start();
                            }
                            boolean isRunning = true;
                            while (isRunning) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                }
                                isRunning = false;
                                for (Thread thread : threads) {
                                    if (thread.isAlive())
                                        isRunning = true;
                                }
                            }
                            if (monitor.isCanceled()) {
                                selectedLines.addAll(originalSelectionLines);
                                selectedTriangles.addAll(originalSelectionTriangles);
                                selectedQuads.addAll(originalSelectionQuads);
                                selectedData.addAll(originalSelectionTriangles);
                                selectedData.addAll(originalSelectionQuads);
                                selectedData.addAll(originalSelectionLines);
                                originalSelectionLines.clear();
                                return;
                            }
                        } finally {
                            monitor.done();
                        }
                    }
                });
            }
            catch (InvocationTargetException consumed) {
            } catch (InterruptedException consumed) {
            }

            if (originalSelectionLines.isEmpty()) return;

            newTriangles.addAll(MathHelper.triangulatePointGroups(resultColours, resultVertices, resultIsLine, View.DUMMY_REFERENCE, linkedDatFile));

            NLogger.debug(getClass(), "Check for identical vertices and collinearity."); //$NON-NLS-1$
            final Set<GData3> trisToDelete2 = new HashSet<GData3>();
            {
                for (GData3 g3 : newTriangles) {
                    Vertex[] verts = triangles.get(g3);
                    Set<Vertex> verts2 = new TreeSet<Vertex>();
                    for (Vertex vert : verts) {
                        verts2.add(vert);
                    }
                    if (verts2.size() < 3 || g3.isCollinear()) {
                        trisToDelete2.add(g3);
                    }
                }
            }

            // Append the new data
            for (GData3 tri : newTriangles) {
                linkedDatFile.addToTailOrInsertAfterCursor(tri);
            }

            NLogger.debug(getClass(), "Delete new, but invalid objects."); //$NON-NLS-1$

            newTriangles.removeAll(trisToDelete2);
            selectedTriangles.addAll(trisToDelete2);
            selectedData.addAll(selectedTriangles);
            delete(false, false);

            // Round to 6 decimal places

            selectedTriangles.addAll(newTriangles);
            selectedData.addAll(selectedTriangles);

            NLogger.debug(getClass(), "Round."); //$NON-NLS-1$
            roundSelection(6, 10, true, false, true, true, true);

            // Fill surfaces
            NLogger.debug(getClass(), "Colour fill."); //$NON-NLS-1$

            newTriangles.clear();
            newTriangles.addAll(selectedTriangles);
            clearSelection();

            final VertexManager vm = linkedDatFile.getVertexManager();
            final SelectorSettings ss = new SelectorSettings();
            ss.setScope(SelectorSettings.CONNECTED);
            ss.setEdgeStop(true);
            ss.setCondlines(false);
            ss.setLines(false);
            ss.setVertices(false);

            colouredTriangles.addAll(newTriangles);

            for (Iterator<GData3> it = newTriangles.iterator(); it.hasNext();) {
                final GData3 tri = it.next();

                // Skip uncoloured or subfile triangles
                if (tri.colourNumber == 16 || !lineLinkedToVertices.containsKey(tri)) {
                    continue;
                }

                clearSelection();
                selectedTriangles.add(tri);
                selectorSilent(ss);

                // Remove the old selected triangles from the set of new triangles
                if (newTriangles.removeAll(selectedTriangles)) {
                    // Reset the iterator
                    it = newTriangles.iterator();
                }

                // Don't want to colour already coloured triangles
                selectedTriangles.removeIf((g) -> g.colourNumber != 16);

                // Change the colour
                vm.colourChangeSelection(tri.colourNumber, tri.r, tri.g, tri.b, tri.a, false);
                // Add the new coloured triangles to the final selection
                colouredTriangles.addAll(selectedTriangles);
            }

            // Cleanup coloured triangle selection, remove subfile/deleted content
            colouredTriangles.removeIf((g) -> !lineLinkedToVertices.containsKey(g));

            // FIXME Needs triangle angle optimisation

            // Restore selection
            clearSelection();
            selectedTriangles.addAll(colouredTriangles);
            selectedData.addAll(selectedTriangles);

            setModified(true, true);

            NLogger.debug(getClass(), "Done."); //$NON-NLS-1$

            validateState();
        }
    }

    private boolean intersectLineLineSegmentUnidirectionalFast(Vector3dd p, Vector3dd p2, Vector3d sp, Vector3d dir, BigDecimal len, Vector3dd q, Vector3dd q2) {

        Vector3d sq = Vector3d.sub(q2, q);

        Vector3d cross = Vector3d.cross(sq, sp);
        Vector3d c = Vector3d.add(cross, q);
        Vector3d d = Vector3d.sub(q, cross);

        return intersectLineTriangleSuperFast(p, q2, d, q2, c, dir, len);

    }

    private boolean intersectRayTriangle(Vector3dd orig, Vector3d dir, Vector3dd vert0, Vector3dd vert1, Vector3dd vert2) {
        BigDecimal diskr = BigDecimal.ZERO;
        BigDecimal inv_diskr = BigDecimal.ZERO;
        Vector3d corner1 = Vector3d.sub(vert1, vert0);
        Vector3d corner2 = Vector3d.sub(vert2, vert0);
        Vector3d pvec = Vector3d.cross(dir, corner2);
        diskr = Vector3d.dotP(corner1, pvec);
        if (diskr.abs().compareTo(TOLERANCER) < 0)
            return false;
        inv_diskr = BigDecimal.ONE.divide(diskr, Threshold.mc);
        Vector3d tvec = Vector3d.sub(orig, vert0);
        BigDecimal u = Vector3d.dotP(tvec, pvec).multiply(inv_diskr);
        if (u.compareTo(ZEROTR) < 0 || u.compareTo(ONETR) > 0)
            return false;
        Vector3d qvec = Vector3d.cross(tvec, corner1);
        BigDecimal v = Vector3d.dotP(dir, qvec).multiply(inv_diskr);
        if (v.compareTo(ZEROTR) < 0 || u.add(v).compareTo(ONETR) > 0)
            return false;
        return true;
    }

    private boolean intersectLineTriangleSuperFast(Vector3dd q, Vector3dd q2, Vector3d d, Vector3dd p2, Vector3d c, Vector3d dir, BigDecimal len) {
        BigDecimal diskr = BigDecimal.ZERO;
        BigDecimal inv_diskr = BigDecimal.ZERO;
        Vector3d vert0 = d;
        Vector3d vert1 = p2;
        Vector3d vert2 = c;
        Vector3d corner1 = Vector3d.sub(vert1, vert0);
        Vector3d corner2 = Vector3d.sub(vert2, vert0);
        Vector3d orig = q;
        Vector3d pvec = Vector3d.cross(dir, corner2);
        diskr = Vector3d.dotP(corner1, pvec);
        if (diskr.abs().compareTo(TOLERANCE) <= 0)
            return false;
        inv_diskr = BigDecimal.ONE.divide(diskr, Threshold.mc);
        Vector3d tvec = Vector3d.sub(orig, vert0);
        BigDecimal u = Vector3d.dotP(tvec, pvec).multiply(inv_diskr);
        if (u.compareTo(ZEROT) < 0 || u.compareTo(ONET) > 0)
            return false;
        Vector3d qvec = Vector3d.cross(tvec, corner1);
        BigDecimal v = Vector3d.dotP(dir, qvec).multiply(inv_diskr);
        if (v.compareTo(ZEROT) < 0 || u.add(v).compareTo(ONET) > 0)
            return false;
        BigDecimal t = Vector3d.dotP(corner2, qvec).multiply(inv_diskr);
        if (t.compareTo(ZEROT) < 0 || t.compareTo(len.add(TOLERANCE)) > 0)
            return false;
        return true;
    }
}
