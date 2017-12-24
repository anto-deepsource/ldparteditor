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
import java.util.Set;

import org.nschmidt.ldparteditor.enums.SlantingMatrixStatus;
import org.nschmidt.ldparteditor.enums.View;
import org.nschmidt.ldparteditor.helpers.math.MathHelper;
import org.nschmidt.ldparteditor.helpers.math.Vector3d;
import org.nschmidt.ldparteditor.i18n.I18n;
import org.nschmidt.ldparteditor.logger.NLogger;
import org.nschmidt.ldparteditor.shells.editor3d.Editor3DWindow;
import org.nschmidt.ldparteditor.text.DatParser;

public class VM28SlantingMatrixProjector extends VM27YTruder {

    protected VM28SlantingMatrixProjector(DatFile linkedDatFile) {
        super(linkedDatFile);
    }

    // These variables are static because the same transformation matrix should be calculated for many DatFiles
    private static Vertex[] axis1 = null;
    private static Vertex[] axis2 = null;
    private static Vertex[] axis3 = null;
    private static int axisSelectionMode = -1;
    private static boolean movingOriginToAxisCenter = false;

    private static Matrix transformation = View.ACCURATE_ID;

    public void resetSlantingMatrixProjector() {
        axis1 = null;
        axis2 = null;
        axis3 = null;
        transformation = View.ACCURATE_ID;
    }

    public void storeAxisForSlantingMatrixProjector() {
        final int selectedLinesSize = selectedLines.size();
        if (selectedLinesSize > 1 && selectedLinesSize < 4 && axis1 == null && axis2 == null) {

            boolean hasXaxis = false;
            boolean hasYaxis = false;
            boolean hasZaxis = false;
            Vertex[] tmpAxisX = null;
            Vertex[] tmpAxisY = null;
            Vertex[] tmpAxisZ = null;

            for (GData2 line : selectedLines) {
                if (line.colourNumber == 4) {
                    tmpAxisX = lines.get(line);
                    hasXaxis = true;
                } else if (line.colourNumber == 2) {
                    tmpAxisY = lines.get(line);
                    hasYaxis = true;
                } else if (line.colourNumber == 1) {
                    tmpAxisZ = lines.get(line);
                    hasZaxis = true;
                }
            }

            if (hasXaxis && hasYaxis && hasZaxis) {
                axis1 = tmpAxisX;
                axis2 = tmpAxisY;
                axis3 = tmpAxisZ;
                axisSelectionMode = 0;
                NLogger.debug(getClass(), "Stored selected X,Y,Z axis"); //$NON-NLS-1$
            } else if (hasXaxis && hasYaxis) {
                axis1 = tmpAxisX;
                axis2 = tmpAxisY;
                axisSelectionMode = 1;
                NLogger.debug(getClass(), "Stored selected X,Y axis"); //$NON-NLS-1$
            } else if (hasXaxis && hasZaxis) {
                axis1 = tmpAxisX;
                axis2 = tmpAxisZ;
                axisSelectionMode = 2;
                NLogger.debug(getClass(), "Stored selected X,Z axis"); //$NON-NLS-1$
            } else if (hasYaxis && hasZaxis) {
                axis1 = tmpAxisY;
                axis2 = tmpAxisZ;
                axisSelectionMode = 3;
                NLogger.debug(getClass(), "Stored selected Y,Z axis"); //$NON-NLS-1$
            }
        }
    }

    public String getSlantingMatrixProjectorStatusString() {
        if (axis1 == null) {
            return ""; //$NON-NLS-1$
        } else {
            switch (axisSelectionMode) {
            case 0: // X,Y,Z
                return I18n.SLANT_MatrixForXYZ;
            case 1: // X,Y
                return I18n.SLANT_MatrixForXY;
            case 2: // X,Z
                return I18n.SLANT_MatrixForXZ;
            case 3: // Y,Z
                return I18n.SLANT_MatrixForYZ;
            default:
                return ""; //$NON-NLS-1$
            }
        }
    }

    public SlantingMatrixStatus getSlantingMatrixStatus() {
        if (axis1 == null) {
            return ""; //$NON-NLS-1$
        } else {
            switch (axisSelectionMode) {
            case 0: // X,Y,Z
                return I18n.SLANT_MatrixForXYZ;
            case 1: // X,Y
                return I18n.SLANT_MatrixForXY;
            case 2: // X,Z
                return I18n.SLANT_MatrixForXZ;
            case 3: // Y,Z
                return I18n.SLANT_MatrixForYZ;
            default:
                return ""; //$NON-NLS-1$
            }
        }
    }

    public Matrix getSlantingMatrix(boolean originToAxisCenter) {
        final Matrix result;
        if (axis1 == null || axis2 == null) {
            return View.ACCURATE_ID;
        }

        Vector3d mx = new Vector3d();
        Vector3d my = new Vector3d();
        Vector3d nmx = new Vector3d();
        Vector3d nmy = new Vector3d();
        Vector3d mz = new Vector3d();
        Vector3d zref;

        // Calculate first and second axis
        {
            BigDecimal dX0Y0 = Vector3d.distSquare(new Vector3d(axis1[0]), new Vector3d(axis2[0]));
            BigDecimal dX0Y1 = Vector3d.distSquare(new Vector3d(axis1[0]), new Vector3d(axis2[1]));
            BigDecimal dX1Y0 = Vector3d.distSquare(new Vector3d(axis1[1]), new Vector3d(axis2[0]));
            BigDecimal dX1Y1 = Vector3d.distSquare(new Vector3d(axis1[1]), new Vector3d(axis2[1]));

            if (dX0Y0.compareTo(dX0Y1) <= 0 && dX0Y0.compareTo(dX1Y0) <= 0 && dX0Y0.compareTo(dX1Y1) <= 0) {
                Vector3d.sub(new Vector3d(axis1[1]), new Vector3d(axis1[0]), mx);
                Vector3d.sub(new Vector3d(axis2[1]), new Vector3d(axis2[0]), my);
                zref = new Vector3d(axis1[0]);
            } else if (dX0Y1.compareTo(dX0Y0) <= 0 && dX0Y1.compareTo(dX1Y0) <= 0 && dX0Y1.compareTo(dX1Y1) <= 0) {
                Vector3d.sub(new Vector3d(axis1[1]), new Vector3d(axis1[0]), mx);
                Vector3d.sub(new Vector3d(axis2[0]), new Vector3d(axis2[1]), my);
                zref = new Vector3d(axis1[0]);
            } else if (dX1Y0.compareTo(dX0Y0) <= 0 && dX1Y0.compareTo(dX0Y1) <= 0 && dX1Y0.compareTo(dX1Y1) <= 0) {
                Vector3d.sub(new Vector3d(axis1[0]), new Vector3d(axis1[1]), mx);
                Vector3d.sub(new Vector3d(axis2[1]), new Vector3d(axis2[0]), my);
                zref = new Vector3d(axis1[1]);
            } else {
                Vector3d.sub(new Vector3d(axis1[0]), new Vector3d(axis1[1]), mx);
                Vector3d.sub(new Vector3d(axis2[0]), new Vector3d(axis2[1]), my);
                zref = new Vector3d(axis1[1]);
            }

            if (Vector3d.distSquare(new Vector3d(), mx).compareTo(BigDecimal.ZERO) > 0) {
                mx.normalise(nmx);
            } else {
                nmx = new Vector3d(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            if (Vector3d.distSquare(new Vector3d(), my).compareTo(BigDecimal.ZERO) > 0) {
                my.normalise(nmy);
            } else {
                nmy = new Vector3d(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO);
            }
        }

        // Calculate third axis

        if (axis3 == null) {
            mz = Vector3d.cross(nmx, nmy);
        } else {
            BigDecimal dXZ0 = Vector3d.distSquare(zref, new Vector3d(axis3[0]));
            BigDecimal dXZ1 = Vector3d.distSquare(zref, new Vector3d(axis3[1]));
            if (dXZ0.compareTo(dXZ1) <= 0) {
                Vector3d.sub(new Vector3d(axis3[1]), new Vector3d(axis3[0]), mz);
            } else {
                Vector3d.sub(new Vector3d(axis3[0]), new Vector3d(axis3[1]), mz);
            }
        }

        // Adjust axes to match LDraw coordinate system

        Vector3d tmp;
        Vector3d origin = new Vector3d();

        if (originToAxisCenter) {
            mx = mx.scaledByHalf();
            my = my.scaledByHalf();
            Vector3d.add(origin, zref, origin);
            Vector3d.add(origin, mx, origin);
            Vector3d.add(origin, my, origin);
        }

        // FIXME Modes needs to be tested...

        switch (axisSelectionMode) {
        case 0: // X,Y,Z (no adjustment)
        case 1: // X,Y (no adjustment)
            break;
        case 2: // X,Z (swap YZ)
            tmp = mz;
            mz = my;
            my = tmp.scale(BigDecimal.ONE.negate());
            break;
        case 3: // Y,Z (swap XY+YZ)
            tmp = mx;
            mx = my;
            my = tmp;
            tmp = mz;
            mz = my;
            my = tmp;
            break;
        default:
            break;
        }

        // Calculate matrix
        result = new Matrix(
                mx.X, mx.Y, mx.Z, BigDecimal.ZERO,
                my.X, my.Y, my.Z, BigDecimal.ZERO,
                mz.X, mz.Y, mz.Z, BigDecimal.ZERO,
                origin.X, origin.Y, origin.Z, BigDecimal.ONE);

        transformation = result;
        return result;
    }

    public void projectWithSlantingMatrix(boolean moveSubfilesToOrigin) {
        if (axis1 == null || axis2 == null) {
            return;
        }
        if (moveSubfilesToOrigin) {
            Set<GData1> newSubfileSelection = new HashSet<GData1>();
            for (GData1 s : selectedSubfiles) {
                StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append(1);
                lineBuilder.append(" "); //$NON-NLS-1$
                if (s.colourNumber == -1) {
                    lineBuilder.append("0x2"); //$NON-NLS-1$
                    lineBuilder.append(MathHelper.toHex((int) (255f * s.r)).toUpperCase());
                    lineBuilder.append(MathHelper.toHex((int) (255f * s.g)).toUpperCase());
                    lineBuilder.append(MathHelper.toHex((int) (255f * s.b)).toUpperCase());
                } else {
                    lineBuilder.append(s.colourNumber);
                }
                lineBuilder.append(" 0 0 0 1 0 0 0 1 0 0 0 1 "); //$NON-NLS-1$
                lineBuilder.append(s.shortName);
                final GColour col16 = View.getLDConfigColour(16);
                Set<String> alreadyParsed = new HashSet<String>();
                alreadyParsed.add(linkedDatFile.getShortName());
                ArrayList<ParsingResult> subfileLine = DatParser
                        .parseLine(lineBuilder.toString(), -1, 0, col16.getR(), col16.getG(), col16.getB(), 1.1f, View.DUMMY_REFERENCE, View.ID, View.ACCURATE_ID, linkedDatFile, false, alreadyParsed, false);
                GData1 subfile = (GData1) subfileLine.get(0).getGraphicalData();
                if (subfile != null) {
                    linker(s, subfile);
                    newSubfileSelection.add(subfile);
                }
            }
            selectedSubfiles.clear();
            selectedSubfiles.addAll(newSubfileSelection);
            reSelectSubFiles();
        }
        transformSelection(transformation, null, Editor3DWindow.getWindow().isMovingAdjacentData());
    }

    public static boolean isMovingOriginToAxisCenter() {
        return movingOriginToAxisCenter;
    }

    public static void setMovingOriginToAxisCenter(boolean movingOriginToAxisCenter) {
        VM28SlantingMatrixProjector.movingOriginToAxisCenter = movingOriginToAxisCenter;
    }
}
