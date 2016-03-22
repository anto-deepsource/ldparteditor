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

import java.io.Serializable;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.nschmidt.ldparteditor.composites.primitive.CompositePrimitive;
import org.nschmidt.ldparteditor.helpers.composite3d.ViewIdleManager;

/**
 * @author nils
 *
 */
public final class PGData1 extends PGData implements Serializable {

    private static final long serialVersionUID = 1L;

    final transient FloatBuffer matrix;
    final transient FloatBuffer matrix2;
    final Matrix4f productMatrix;
    final Matrix4f localMatrix;

    final String name;
    final String shortName;

    final boolean negativeDeterminant;

    final PGData myGData = new PGDataInit();

    final int depth;

    /**
     * SLOWER, FOR PRIMITIVE CHOOSER ONLY, uses no cache, uses no bounding box!
     *
     * @param tMatrix
     * @param lines
     * @param name
     * @param shortName
     * @param depth
     * @param det
     * @param pMatrix
     * @param hotMap
     * @param firstRef
     */
    public PGData1(Matrix4f tMatrix, LinkedList<String> lines, String name, String shortName, int depth, boolean det, Matrix4f pMatrix,
            Set<String> alreadyParsed, HashMap<PGTimestamp, PGTimestamp> hotMap) {

        depth++;
        if (depth < 16) {
            this.depth = depth;
            negativeDeterminant = det;
            this.name = name;
            this.shortName = shortName;
            this.productMatrix = new Matrix4f(pMatrix);
            this.localMatrix = new Matrix4f(tMatrix);
            matrix = BufferUtils.createFloatBuffer(16);
            tMatrix.store(matrix);

            matrix.position(0);
            matrix2 = BufferUtils.createFloatBuffer(16);
            Matrix4f tMatrix2 = new Matrix4f(tMatrix);
            tMatrix2.m30 = -tMatrix2.m30;
            tMatrix2.store(matrix2);
            matrix2.position(0);

            PGData anchorData = myGData;

            for (String line : lines) {
                if (isNotBlank(line)) {
                    PGData gdata = CompositePrimitive.parseLine(line, depth, pMatrix, alreadyParsed, hotMap);
                    if (gdata != null) {
                        anchorData.setNext(gdata);
                        anchorData = gdata;
                    }
                }
            }
        } else {
            this.depth = depth;
            this.name = null;
            this.shortName = null;
            this.matrix = null;
            this.matrix2 = null;
            this.productMatrix = null;
            this.localMatrix = null;
            this.negativeDeterminant = false;
        }
    }

    private boolean isNotBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return false;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(str.charAt(i)) == false) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void drawBFCprimitive(int drawOnlyMode) {
        if (matrix != null) {
            byte tempWinding = PGData.localWinding;
            boolean tempInvertNext = PGData.globalInvertNext;
            boolean tempInvertNextFound = PGData.globalInvertNextFound;
            boolean tempNegativeDeterminant = PGData.globalNegativeDeterminant;
            PGData.globalInvertNextFound = false;
            PGData.localWinding = BFC.NOCERTIFY;
            PGData.globalNegativeDeterminant = PGData.globalNegativeDeterminant ^ negativeDeterminant;
            GL11.glPushMatrix();
            GL11.glMultMatrix(matrix);
            PGData data2draw = myGData;
            if (PGData.accumClip > 0) {
                PGData.accumClip++;
                while ((data2draw = data2draw.getNext()) != null && !ViewIdleManager.pause[0].get())
                    data2draw.drawBFCprimitive(drawOnlyMode);
                PGData.accumClip--;
            } else {
                while ((data2draw = data2draw.getNext()) != null && !ViewIdleManager.pause[0].get()) {
                    data2draw.drawBFCprimitive(drawOnlyMode);
                }
                if (PGData.accumClip > 0)
                    PGData.accumClip = 0;
            }
            GL11.glPopMatrix();
            PGData.localWinding = tempWinding;
            if (tempInvertNextFound)
                PGData.globalInvertNext = !tempInvertNext;
            PGData.globalNegativeDeterminant = tempNegativeDeterminant;
        }
    }
    @Override
    public int type() {
        return 1;
    }
    @Override
    public PGData data() {
        return this;
    }
}
