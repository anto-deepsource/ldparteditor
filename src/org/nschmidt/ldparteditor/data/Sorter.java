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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.nschmidt.ldparteditor.logger.NLogger;
import org.nschmidt.ldparteditor.text.StringHelper;

/**
 * Sorts selected lines by a given criteria
 *
 * @author nils
 *
 */
public enum Sorter {
    INSTANCE;

    /*
     * 0 = "By colour, ascending.",
     * 1 = "By colour, descending.",
     * 2 = "By type, ascending.",
     * 3 = "By type, descending.",
     * 4 = "By type and then colour, ascending.",
     * 5 = "By type and then colour, descending."
     */
    public static void sort(StyledText st, int fromLine, int toLine, DatFile fileNameObj, int scope, int sortCriteria, boolean destructiveSort) {

        // Backup selection range
        final int x = st.getSelectionRange().x;
        final int y = st.getSelectionRange().y;

        // Basic Data Structure
        List<ArrayList<GData>> subLists = new ArrayList<ArrayList<GData>>();
        List<Boolean> shouldListBeSorted = new ArrayList<Boolean>();

        {
            if (scope == 1 && fromLine < toLine) {
                NLogger.debug(Sorter.class, "Sorting selection.."); //$NON-NLS-1$

            } else {
                NLogger.debug(Sorter.class, "Sorting file.."); //$NON-NLS-1$
            }
            GData data2draw = fileNameObj.getDrawChainStart();
            int state = 0;
            ArrayList<GData> currentList = new ArrayList<GData>();
            int lineCount = 0;
            while ((data2draw = data2draw.getNext()) != null) {
                boolean validType = false;
                lineCount += 1;
                switch (data2draw.type()) {
                case 0:
                    if (!data2draw.toString().trim().isEmpty() && !destructiveSort) break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    validType = true;
                default:
                    if (!validType && !destructiveSort) break;
                    if (scope == 1 && fromLine < toLine) {
                        if (lineCount == fromLine - 1) {
                            if (data2draw.type() == 6) {
                                GDataBFC bfc = (GDataBFC) data2draw;
                                if (!(bfc.getType() == BFC.INVERTNEXT && data2draw.getNext() != null && data2draw.getNext().type() == 1 && destructiveSort)) {
                                    break;
                                }
                            }
                        } else if (lineCount < fromLine || lineCount > toLine) {
                            break;
                        }
                    }
                    // CHECK FOR BFC INVERTNEXT
                    if (data2draw.type() == 6) {
                        GDataBFC bfc = (GDataBFC) data2draw;
                        if (!(bfc.getType() == BFC.INVERTNEXT && data2draw.getNext() != null && data2draw.getNext().type() == 1 && destructiveSort)) {
                            break;
                        }
                    }
                    if (state != 1) {
                        currentList = new ArrayList<GData>();
                        state = 1;
                        shouldListBeSorted.add(true);
                        subLists.add(currentList);
                    }
                    currentList.add(data2draw);
                    continue;
                }
                if (state != 2) {
                    currentList = new ArrayList<GData>();
                    state = 2;
                    shouldListBeSorted.add(false);
                    subLists.add(currentList);
                }
                currentList.add(data2draw);
            }
        }

        final int listCount = shouldListBeSorted.size();
        for (int i = 0; i < listCount; i++) {
            boolean  sortIt = shouldListBeSorted.get(i);
            if (sortIt) {
                List<GData> listToSort = subLists.get(i);
                switch (sortCriteria) {
                case 0:
                    Collections.sort(listToSort, new Comparator<GData>() {
                        @Override
                        public int compare(GData g1, GData g2) {
                            float c1 = -2f;
                            float c2 = -2f;
                            switch (g1.type()) {
                            case 1:
                            {GData1 g = (GData1) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 2:
                            {GData2 g = (GData2) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 3:
                            {GData3 g = (GData3) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 4:
                            {GData4 g = (GData4) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 5:
                            {GData5 g = (GData5) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            }
                            switch (g2.type()) {
                            case 1:
                            {GData1 g = (GData1) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 2:
                            {GData2 g = (GData2) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 3:
                            {GData3 g = (GData3) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 4:
                            {GData4 g = (GData4) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 5:
                            {GData5 g = (GData5) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            }
                            if (g1.type() == 6 && g1.getNext() != null) {
                                switch (g1.getNext().type()) {
                                case 1:
                                {GData1 g = (GData1) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 2:
                                {GData2 g = (GData2) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 3:
                                {GData3 g = (GData3) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 4:
                                {GData4 g = (GData4) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 5:
                                {GData5 g = (GData5) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                }
                            }
                            if (g2.type() == 6 && g2.getNext() != null) {
                                switch (g2.getNext().type()) {
                                case 1:
                                {GData1 g = (GData1) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 2:
                                {GData2 g = (GData2) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 3:
                                {GData3 g = (GData3) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 4:
                                {GData4 g = (GData4) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 5:
                                {GData5 g = (GData5) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                }
                            }
                            return Float.compare(c1, c2);
                        }
                    });
                    break;
                case 1:
                    Collections.sort(listToSort, new Comparator<GData>() {
                        @Override
                        public int compare(GData g1, GData g2) {
                            float c1 = -2f;
                            float c2 = -2f;
                            switch (g1.type()) {
                            case 1:
                            {GData1 g = (GData1) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 2:
                            {GData2 g = (GData2) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 3:
                            {GData3 g = (GData3) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 4:
                            {GData4 g = (GData4) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 5:
                            {GData5 g = (GData5) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            }
                            switch (g2.type()) {
                            case 1:
                            {GData1 g = (GData1) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 2:
                            {GData2 g = (GData2) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 3:
                            {GData3 g = (GData3) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 4:
                            {GData4 g = (GData4) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            case 5:
                            {GData5 g = (GData5) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                            break;
                            }
                            if (g1.type() == 6 && g1.getNext() != null) {
                                switch (g1.getNext().type()) {
                                case 1:
                                {GData1 g = (GData1) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 2:
                                {GData2 g = (GData2) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 3:
                                {GData3 g = (GData3) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 4:
                                {GData4 g = (GData4) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 5:
                                {GData5 g = (GData5) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                }
                            }
                            if (g2.type() == 6 && g2.getNext() != null) {
                                switch (g2.getNext().type()) {
                                case 1:
                                {GData1 g = (GData1) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 2:
                                {GData2 g = (GData2) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 3:
                                {GData3 g = (GData3) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 4:
                                {GData4 g = (GData4) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 5:
                                {GData5 g = (GData5) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                }
                            }
                            return Float.compare(c2, c1);
                        }
                    });
                    break;
                case 2:
                    Collections.sort(listToSort, new Comparator<GData>() {
                        @Override
                        public int compare(GData g1, GData g2) {
                            int t1 = g1.type();
                            int t2 = g2.type();
                            if (t1 == 6 && g1.getNext() != null) {
                                t1 = g1.getNext().type();
                            }
                            if (t2 == 6 && g2.getNext() != null) {
                                t2 = g2.getNext().type();
                            }
                            return Integer.compare(t1, t2);
                        }
                    });
                    break;
                case 3:
                    Collections.sort(listToSort, new Comparator<GData>() {
                        @Override
                        public int compare(GData g1, GData g2) {
                            int t1 = g1.type();
                            int t2 = g2.type();
                            if (t1 == 6 && g1.getNext() != null) {
                                t1 = g1.getNext().type();
                            }
                            if (t2 == 6 && g2.getNext() != null) {
                                t2 = g2.getNext().type();
                            }
                            return Integer.compare(t2, t1);
                        }
                    });
                    break;
                case 4:
                    Collections.sort(listToSort, new Comparator<GData>() {
                        @Override
                        public int compare(GData g1, GData g2) {
                            int t1 = g1.type();
                            int t2 = g2.type();
                            if (t1 == 6 && g1.getNext() != null) {
                                t1 = g1.getNext().type();
                            }
                            if (t2 == 6 && g2.getNext() != null) {
                                t2 = g2.getNext().type();
                            }
                            int cmp1 = Integer.compare(t1, t2);
                            if (cmp1 == 0) {
                                float c1 = 0f;
                                float c2 = 0f;
                                switch (g1.type()) {
                                case 1:
                                {GData1 g = (GData1) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 2:
                                {GData2 g = (GData2) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 3:
                                {GData3 g = (GData3) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 4:
                                {GData4 g = (GData4) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 5:
                                {GData5 g = (GData5) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                }
                                switch (g2.type()) {
                                case 1:
                                {GData1 g = (GData1) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 2:
                                {GData2 g = (GData2) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 3:
                                {GData3 g = (GData3) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 4:
                                {GData4 g = (GData4) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 5:
                                {GData5 g = (GData5) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                }
                                if (g1.type() == 6 && g1.getNext() != null) {
                                    switch (g1.getNext().type()) {
                                    case 1:
                                    {GData1 g = (GData1) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 2:
                                    {GData2 g = (GData2) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 3:
                                    {GData3 g = (GData3) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 4:
                                    {GData4 g = (GData4) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 5:
                                    {GData5 g = (GData5) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    }
                                }
                                if (g2.type() == 6 && g2.getNext() != null) {
                                    switch (g2.getNext().type()) {
                                    case 1:
                                    {GData1 g = (GData1) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 2:
                                    {GData2 g = (GData2) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 3:
                                    {GData3 g = (GData3) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 4:
                                    {GData4 g = (GData4) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 5:
                                    {GData5 g = (GData5) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    }
                                }
                                return Float.compare(c1, c2);
                            } else {
                                return cmp1;
                            }
                        }
                    });
                    break;
                case 5:
                    Collections.sort(listToSort, new Comparator<GData>() {
                        @Override
                        public int compare(GData g1, GData g2) {
                            int t1 = g1.type();
                            int t2 = g2.type();
                            if (t1 == 6) {
                                t1 = g1.getNext().type();
                            }
                            if (t2 == 6) {
                                t2 = g2.getNext().type();
                            }
                            int cmp1 = Integer.compare(t2, t1);
                            if (cmp1 == 0) {
                                float c1 = 0f;
                                float c2 = 0f;
                                switch (g1.type()) {
                                case 1:
                                {GData1 g = (GData1) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 2:
                                {GData2 g = (GData2) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 3:
                                {GData3 g = (GData3) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 4:
                                {GData4 g = (GData4) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 5:
                                {GData5 g = (GData5) g1; if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                }
                                switch (g2.type()) {
                                case 1:
                                {GData1 g = (GData1) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 2:
                                {GData2 g = (GData2) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 3:
                                {GData3 g = (GData3) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 4:
                                {GData4 g = (GData4) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                case 5:
                                {GData5 g = (GData5) g2; if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                break;
                                }
                                if (g1.type() == 6 && g1.getNext() != null) {
                                    switch (g1.getNext().type()) {
                                    case 1:
                                    {GData1 g = (GData1) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 2:
                                    {GData2 g = (GData2) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 3:
                                    {GData3 g = (GData3) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 4:
                                    {GData4 g = (GData4) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 5:
                                    {GData5 g = (GData5) g1.getNext(); if (g.colourNumber != -1) {c1 = g.colourNumber;} else {c1 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    }
                                }
                                if (g2.type() == 6 && g2.getNext() != null) {
                                    switch (g2.getNext().type()) {
                                    case 1:
                                    {GData1 g = (GData1) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 2:
                                    {GData2 g = (GData2) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 3:
                                    {GData3 g = (GData3) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 4:
                                    {GData4 g = (GData4) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    case 5:
                                    {GData5 g = (GData5) g2.getNext(); if (g.colourNumber != -1) {c2 = g.colourNumber;} else {c2 = g.b + 10f * g.g + 100f * g.r;}}
                                    break;
                                    }
                                }
                                return Float.compare(c2, c1);
                            } else {
                                return cmp1;
                            }
                        }
                    });
                    break;
                default:
                    break;
                }

            }
        }


        StringBuilder newDatText = new StringBuilder();
        final String ld = StringHelper.getLineDelimiter();
        for (int i = 0; i < listCount; i++) {
            List<GData> listToSort = subLists.get(i);
            int listCount2 = listToSort.size();
            for (int j = 0; j < listCount2; j++) {
                GData g = listToSort.get(j);
                newDatText.append(g.toString());
                if (!(i == listCount - 1 && j == listCount2 - 1)) newDatText.append(ld);
            }

        }
        st.setText(newDatText.toString());

        // Restore selection range

        try {
            st.setSelectionRange(x, y);
        } catch (IllegalArgumentException iae1) {
            try {
                st.setSelectionRange(x - 1, y);
            } catch (IllegalArgumentException iae2) {
                try {
                    st.setSelectionRange(x - 2, y);
                } catch (IllegalArgumentException consumed) {}
            }
        }

        st.showSelection();
    }
}
