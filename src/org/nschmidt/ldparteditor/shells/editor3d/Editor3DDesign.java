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
package org.nschmidt.ldparteditor.shells.editor3d;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.lwjgl.util.vector.Matrix4f;
import org.nschmidt.ldparteditor.composites.Composite3D;
import org.nschmidt.ldparteditor.composites.CompositeContainer;
import org.nschmidt.ldparteditor.composites.ToolItem;
import org.nschmidt.ldparteditor.composites.ToolItemDrawLocation;
import org.nschmidt.ldparteditor.composites.ToolItemDrawMode;
import org.nschmidt.ldparteditor.composites.ToolItemState;
import org.nschmidt.ldparteditor.composites.ToolSeparator;
import org.nschmidt.ldparteditor.composites.primitive.CompositePrimitive;
import org.nschmidt.ldparteditor.data.GColour;
import org.nschmidt.ldparteditor.dialogs.colour.ColourDialog;
import org.nschmidt.ldparteditor.enums.MyLanguage;
import org.nschmidt.ldparteditor.enums.Task;
import org.nschmidt.ldparteditor.enums.Threshold;
import org.nschmidt.ldparteditor.enums.View;
import org.nschmidt.ldparteditor.helpers.ShellHelper;
import org.nschmidt.ldparteditor.helpers.math.MathHelper;
import org.nschmidt.ldparteditor.i18n.I18n;
import org.nschmidt.ldparteditor.logger.NLogger;
import org.nschmidt.ldparteditor.project.Project;
import org.nschmidt.ldparteditor.resources.ResourceManager;
import org.nschmidt.ldparteditor.shells.editortext.EditorTextWindow;
import org.nschmidt.ldparteditor.state.KeyStateManager;
import org.nschmidt.ldparteditor.text.LDParsingException;
import org.nschmidt.ldparteditor.text.StringHelper;
import org.nschmidt.ldparteditor.text.UTF8BufferedReader;
import org.nschmidt.ldparteditor.widgets.BigDecimalSpinner;
import org.nschmidt.ldparteditor.widgets.Tree;
import org.nschmidt.ldparteditor.widgets.TreeItem;
import org.nschmidt.ldparteditor.widgets.ValueChangeAdapter;
import org.nschmidt.ldparteditor.workbench.Composite3DState;
import org.nschmidt.ldparteditor.workbench.Editor3DWindowState;
import org.nschmidt.ldparteditor.workbench.UserSettingState;
import org.nschmidt.ldparteditor.workbench.WorkbenchManager;

import swing2swt.layout.BorderLayout;

/**
 * The 3D editor window
 * <p>
 * Note: This class should not be instantiated, it defines the gui layout and no
 * business logic.
 *
 * @author nils
 *
 */
class Editor3DDesign extends ApplicationWindow {

    /** The menu of the manipulator */
    private Menu mnu_Manipulator;
    /** The menu of the tools */
    private Menu mnu_Tools;
    /** The menu of merge, split and other functions */
    private Menu mnu_Merge;
    /** The menu of the select features */
    private Menu mnu_Select;

    final Menu[] mnu_treeMenu = new Menu[1];

    final Menu[] mnu_coarseMenu = new Menu[1];
    final Menu[] mnu_mediumMenu = new Menu[1];
    final Menu[] mnu_fineMenu = new Menu[1];

    final CTabFolder[] tabFolder_OpenDatFiles = new CTabFolder[1];
    final Button[] btn_SyncTabs = new Button[1];

    Action menuItem_Open = ShellHelper.DUMMY_ACTION;
    Action menuItem_Exit = ShellHelper.DUMMY_ACTION;
    Action toolItem_Save = ShellHelper.DUMMY_ACTION;
    final MenuManager[] mnu_File = new MenuManager[1];
    private Composite cmpNorth;
    private Composite cmpEast;
    private Composite cmpWest;
    private static Composite status;
    private static final int iconSize = WorkbenchManager.getUserSettingState().getIconSize();
    final Button[] btn_Sync = new Button[1];
    final Button[] btn_LastOpen = new Button[1];
    final Button[] btn_New = new Button[1];
    final Button[] btn_Open = new Button[1];
    final Button[] btn_Save = new Button[1];
    final Button[] btn_SaveAll = new Button[1];
    final Button[] btn_Select = new Button[1];
    final Button[] btn_Move = new Button[1];
    final Button[] btn_Rotate = new Button[1];
    final Button[] btn_Scale = new Button[1];
    final Button[] btn_Combined = new Button[1];

    final Button[] btn_Local = new Button[1];
    final Button[] btn_Global = new Button[1];

    final Button[] btn_Undo = new Button[1];
    final Button[] btn_AddHistory = new Button[1];
    final Button[] btn_Redo = new Button[1];

    final Button[] btn_Manipulator_0_toOrigin = new Button[1];
    final Button[] btn_Manipulator_X_XReverse = new Button[1];
    final Button[] btn_Manipulator_XI_YReverse = new Button[1];
    final Button[] btn_Manipulator_XII_ZReverse = new Button[1];
    final Button[] btn_Manipulator_XIII_toWorld = new Button[1];
    final Button[] btn_Manipulator_1_cameraToPos = new Button[1];
    final Button[] btn_Manipulator_2_toAverage = new Button[1];
    final Button[] btn_Manipulator_3_toSubfile = new Button[1];
    final Button[] btn_Manipulator_32_subfileTo = new Button[1];
    final Button[] btn_Manipulator_4_toVertex = new Button[1];
    final Button[] btn_Manipulator_5_toEdge = new Button[1];
    final Button[] btn_Manipulator_6_toSurface = new Button[1];
    final Button[] btn_Manipulator_7_toVertexNormal = new Button[1];
    final Button[] btn_Manipulator_8_toEdgeNormal = new Button[1];
    final Button[] btn_Manipulator_9_toSurfaceNormal = new Button[1];
    final Button[] btn_Manipulator_XIV_adjustRotationCenter = new Button[1];
    final Button[] btn_Manipulator_XV_toVertexPosition = new Button[1];
    final Button[] btn_Manipulator_SwitchXY = new Button[1];
    final Button[] btn_Manipulator_SwitchXZ = new Button[1];
    final Button[] btn_Manipulator_SwitchYZ = new Button[1];

    final MenuItem[] mntm_Manipulator_0_toOrigin = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_X_XReverse = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_XI_YReverse = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_XII_ZReverse = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_XIII_toWorld = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_1_cameraToPos = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_2_toAverage = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_3_toSubfile = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_32_subfileTo = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_4_toVertex = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_5_toEdge = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_6_toSurface = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_7_toVertexNormal = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_8_toEdgeNormal = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_9_toSurfaceNormal = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_XIV_adjustRotationCenter = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_XV_toVertexPosition = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_SwitchXY = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_SwitchXZ = new MenuItem[1];
    final MenuItem[] mntm_Manipulator_SwitchYZ = new MenuItem[1];

    final Button[] btn_LastUsedColour = new Button[1];
    final Button[] btn_Pipette = new Button[1];
    final Button[] btn_Decolour = new Button[1];
    final Button[] btn_Palette = new Button[1];

    final Button[] btn_MoveAdjacentData = new Button[1];
    final Button[] btn_CompileSubfile = new Button[1];
    final Button[] btn_SplitQuad = new Button[1];
    final Button[] btn_MergeQuad = new Button[1];
    final Button[] btn_CondlineToLine = new Button[1];
    final Button[] btn_LineToCondline = new Button[1];
    final Button[] btn_MoveOnLine = new Button[1];
    final Button[] btn_RoundSelection = new Button[1];
    final Button[] btn_ShowSelectionInTextEditor = new Button[1];
    final Button[] btn_BFCswap = new Button[1];
    final Button[] btn_Vertices = new Button[1];
    final Button[] btn_TrisNQuads = new Button[1];
    final Button[] btn_Lines = new Button[1];
    final Button[] btn_Subfiles = new Button[1];
    final Button[] btn_InsertAtCursorPosition = new Button[1];
    final Button[] btn_AddComment = new Button[1];
    final Button[] btn_AddVertex = new Button[1];
    final Button[] btn_AddPrimitive = new Button[1];
    final Button[] btn_AddLine = new Button[1];
    final Button[] btn_AddTriangle = new Button[1];
    final Button[] btn_AddQuad = new Button[1];
    final Button[] btn_AddCondline = new Button[1];
    final Button[] btn_AddDistance = new Button[1];
    final Button[] btn_AddProtractor = new Button[1];

    final MenuItem[] mntm_gridCoarseDefault = new MenuItem[1];
    final MenuItem[] mntm_gridMediumDefault = new MenuItem[1];
    final MenuItem[] mntm_gridFineDefault = new MenuItem[1];

    final MenuItem[] mntm_OpenIn3DEditor = new MenuItem[1];
    final MenuItem[] mntm_OpenInTextEditor = new MenuItem[1];
    final MenuItem[] mntm_Rename = new MenuItem[1];
    final MenuItem[] mntm_Revert = new MenuItem[1];
    final MenuItem[] mntm_Close = new MenuItem[1];
    final MenuItem[] mntm_CopyToUnofficial = new MenuItem[1];

    final Button[] btn_Select2 = new Button[1];

    final MenuItem[] mntm_SelectAll = new MenuItem[1];
    final MenuItem[] mntm_SelectAllWithColours = new MenuItem[1];
    final MenuItem[] mntm_SelectAllVisible = new MenuItem[1];
    final MenuItem[] mntm_SelectAllVisibleWithColours = new MenuItem[1];
    final MenuItem[] mntm_SelectNone = new MenuItem[1];
    final MenuItem[] mntm_SelectInverse = new MenuItem[1];
    final MenuItem[] mntm_SelectEverything = new MenuItem[1];
    final MenuItem[] mntm_SelectConnected = new MenuItem[1];
    final MenuItem[] mntm_SelectTouching = new MenuItem[1];
    final MenuItem[] mntm_WithSameOrientation = new MenuItem[1];
    final MenuItem[] mntm_WithAccuracy = new MenuItem[1];
    final MenuItem[] mntm_WithAdjacency = new MenuItem[1];
    final MenuItem[] mntm_WithWholeSubfiles = new MenuItem[1];
    final MenuItem[] mntm_WithSameColour = new MenuItem[1];
    final MenuItem[] mntm_WithHiddenData = new MenuItem[1];
    final MenuItem[] mntm_ExceptSubfiles = new MenuItem[1];
    final MenuItem[] mntm_StopAtEdges = new MenuItem[1];
    final MenuItem[] mntm_SelectIsolatedVertices = new MenuItem[1];
    final MenuItem[] mntm_SelectSingleVertex = new MenuItem[1];

    final MenuItem[] mntm_PartReview = new MenuItem[1];
    final MenuItem[] mntm_Edger2 = new MenuItem[1];
    final MenuItem[] mntm_Txt2Dat = new MenuItem[1];
    final MenuItem[] mntm_Rectifier = new MenuItem[1];
    final MenuItem[] mntm_Isecalc = new MenuItem[1];
    final MenuItem[] mntm_SlicerPro = new MenuItem[1];
    final MenuItem[] mntm_Intersector = new MenuItem[1];
    final MenuItem[] mntm_Lines2Pattern = new MenuItem[1];
    final MenuItem[] mntm_PathTruder = new MenuItem[1];
    final MenuItem[] mntm_SymSplitter = new MenuItem[1];
    final MenuItem[] mntm_Unificator = new MenuItem[1];
    final MenuItem[] mntm_RingsAndCones = new MenuItem[1];
    final MenuItem[] mntm_TJunctionFinder = new MenuItem[1];
    final MenuItem[] mntm_MeshReducer = new MenuItem[1];

    final MenuItem[] mntm_Options = new MenuItem[1];
    final MenuItem[] mntm_ResetSettingsOnRestart = new MenuItem[1];
    final MenuItem[] mntm_SelectAnotherLDConfig = new MenuItem[1];

    final MenuItem[] mntm_LoadPalette = new MenuItem[1];
    final MenuItem[] mntm_ResetPalette = new MenuItem[1];
    final MenuItem[] mntm_SavePalette = new MenuItem[1];
    final MenuItem[] mntm_SetPaletteSize = new MenuItem[1];

    final MenuItem[] mntm_UploadLogs = new MenuItem[1];
    final MenuItem[] mntm_AntiAliasing = new MenuItem[1];
    final MenuItem[] mntm_SyncLpeInline = new MenuItem[1];

    final MenuItem[] mntm_Flip = new MenuItem[1];
    final MenuItem[] mntm_Smooth = new MenuItem[1];
    final MenuItem[] mntm_SubdivideCatmullClark = new MenuItem[1];
    final MenuItem[] mntm_SubdivideLoop = new MenuItem[1];
    final MenuItem[] mntm_Split = new MenuItem[1];
    final MenuItem[] mntm_SplitNTimes = new MenuItem[1];

    final MenuItem[] mntm_MergeToAverage = new MenuItem[1];
    final MenuItem[] mntm_MergeToLastSelected = new MenuItem[1];
    final MenuItem[] mntm_MergeToNearestVertex = new MenuItem[1];
    final MenuItem[] mntm_MergeToNearestEdge = new MenuItem[1];
    final MenuItem[] mntm_MergeToNearestFace = new MenuItem[1];

    final MenuItem[] mntm_setXYZ = new MenuItem[1];
    final MenuItem[] mntm_Translate = new MenuItem[1];
    final MenuItem[] mntm_Rotate = new MenuItem[1];
    final MenuItem[] mntm_Scale = new MenuItem[1];

    final Text[] txt_Search = new Text[1];
    final Button[] btn_ResetSearch = new Button[1];
    final CTabFolder[] tabFolder_Settings = new CTabFolder[1];

    final Button[] btn_Hide = new Button[1];
    final Button[] btn_ShowAll = new Button[1];
    final Button[] btn_NoTransparentSelection = new Button[1];
    final Button[] btn_BFCToggle = new Button[1];

    final Button[] btn_Delete = new Button[1];
    final Button[] btn_Copy = new Button[1];
    final Button[] btn_Cut = new Button[1];
    final Button[] btn_Paste = new Button[1];

    final Button[] btn_Coarse = new Button[1];
    final Button[] btn_Medium = new Button[1];
    final Button[] btn_Fine = new Button[1];

    final BigDecimalSpinner[] spn_Move = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_Rotate = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_Scale = new BigDecimalSpinner[1];

    final TreeItem[] treeItem_Project = new TreeItem[1];
    final TreeItem[] treeItem_Unsaved = new TreeItem[1];
    final TreeItem[] treeItem_Unofficial = new TreeItem[1];
    final TreeItem[] treeItem_Official = new TreeItem[1];

    final TreeItem[] treeItem_ProjectParts = new TreeItem[1];
    final TreeItem[] treeItem_ProjectSubparts = new TreeItem[1];
    final TreeItem[] treeItem_ProjectPrimitives = new TreeItem[1];
    final TreeItem[] treeItem_ProjectPrimitives8 = new TreeItem[1];
    final TreeItem[] treeItem_ProjectPrimitives48 = new TreeItem[1];
    final TreeItem[] treeItem_UnofficialParts = new TreeItem[1];
    final TreeItem[] treeItem_UnofficialSubparts = new TreeItem[1];
    final TreeItem[] treeItem_UnofficialPrimitives = new TreeItem[1];
    final TreeItem[] treeItem_UnofficialPrimitives8 = new TreeItem[1];
    final TreeItem[] treeItem_UnofficialPrimitives48 = new TreeItem[1];
    final TreeItem[] treeItem_OfficialParts = new TreeItem[1];
    final TreeItem[] treeItem_OfficialSubparts = new TreeItem[1];
    final TreeItem[] treeItem_OfficialPrimitives = new TreeItem[1];
    final TreeItem[] treeItem_OfficialPrimitives8 = new TreeItem[1];
    final TreeItem[] treeItem_OfficialPrimitives48 = new TreeItem[1];
    final Tree[] treeParts = new Tree[1];

    final Button[] btn_PngPrevious = new Button[1];
    final Button[] btn_PngFocus = new Button[1];
    final Button[] btn_PngNext = new Button[1];
    final Button[] btn_PngImage = new Button[1];
    final Text[] txt_PngPath = new Text[1];
    final BigDecimalSpinner[] spn_PngX = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_PngY = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_PngZ = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_PngA1 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_PngA2 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_PngA3 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_PngSX = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_PngSY = new BigDecimalSpinner[1];

    final Button[] btn_ToggleLinesOpenGL = new Button[1];
    final Button[] btn_lineSize1 = new Button[1];
    final Button[] btn_lineSize2 = new Button[1];
    final Button[] btn_lineSize3 = new Button[1];
    final Button[] btn_lineSize4 = new Button[1];

    final Button[] btn_NewDat = new Button[1];
    final Button[] btn_OpenDat = new Button[1];
    final Button[] btn_SaveDat = new Button[1];
    final Button[] btn_SaveAsDat = new Button[1];

    final Button[] btn_PreviousSelection = new Button[1];
    final Button[] btn_NextSelection = new Button[1];
    final Text[] txt_Line = new Text[1];
    final Button[] btn_MoveAdjacentData2 = new Button[1];
    final BigDecimalSpinner[] spn_SelectionX1 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionY1 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionZ1 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionX2 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionY2 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionZ2 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionX3 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionY3 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionZ3 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionX4 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionY4 = new BigDecimalSpinner[1];
    final BigDecimalSpinner[] spn_SelectionZ4 = new BigDecimalSpinner[1];
    final Label[] lbl_SelectionX1 = new Label[1];
    final Label[] lbl_SelectionY1 = new Label[1];
    final Label[] lbl_SelectionZ1 = new Label[1];
    final Label[] lbl_SelectionX2 = new Label[1];
    final Label[] lbl_SelectionY2 = new Label[1];
    final Label[] lbl_SelectionZ2 = new Label[1];
    final Label[] lbl_SelectionX3 = new Label[1];
    final Label[] lbl_SelectionY3 = new Label[1];
    final Label[] lbl_SelectionZ3 = new Label[1];
    final Label[] lbl_SelectionX4 = new Label[1];
    final Label[] lbl_SelectionY4 = new Label[1];
    final Label[] lbl_SelectionZ4 = new Label[1];
    final Label[] lbl_selectedPrimitiveItem = new Label[1];

    final CompositePrimitive[] cmp_Primitives = new CompositePrimitive[1];
    final Text[] txt_primitiveSearch = new Text[1];
    final Button[] btn_resetPrimitiveSearch = new Button[1];

    final Button[] btn_zoomOutPrimitives = new Button[1];
    final Button[] btn_zoomInPrimitives = new Button[1];

    final MenuItem[] mntm_STriangles = new MenuItem[1];
    final MenuItem[] mntm_SQuads = new MenuItem[1];
    final MenuItem[] mntm_SCLines = new MenuItem[1];
    final MenuItem[] mntm_SVertices = new MenuItem[1];
    final MenuItem[] mntm_SLines = new MenuItem[1];


    final MenuItem[] mntm_IconSize1 = new MenuItem[1];
    final MenuItem[] mntm_IconSize2 = new MenuItem[1];
    final MenuItem[] mntm_IconSize3 = new MenuItem[1];
    final MenuItem[] mntm_IconSize4 = new MenuItem[1];
    final MenuItem[] mntm_IconSize5 = new MenuItem[1];
    final MenuItem[] mntm_IconSize6 = new MenuItem[1];

    private ToolItem toolItem_ColourBar;

    private static SashForm sashForm;

    static final int TEXT_3D_SEPARATE = 0;
    static final int TEXT_LEFT_3D_RIGHT = 1;
    static final int TEXT_RIGHT_3D_LEFT = 2;

    /**
     * Create the application window.
     */
    public Editor3DDesign() {
        super(null);
        addToolBar(SWT.FLAT | SWT.WRAP);
    }

    /**
     * Create contents of the application window.
     *
     * @param parent
     */
    @Override
    protected Control createContents(Composite parent) {
        final Editor3DWindowState windowState = WorkbenchManager.getEditor3DWindowState();
        final UserSettingState userSettings = WorkbenchManager.getUserSettingState();

        setStatus(I18n.E3D_ReadyStatus);
        Composite containerTop = new Composite(parent, SWT.NONE);
        containerTop.setLayout(new BorderLayout(0, 0));

        if (userSettings.getTextWinArr() != TEXT_3D_SEPARATE) {
            containerTop = new SashForm(containerTop, SWT.BORDER);
            if (userSettings.getTextWinArr() == TEXT_LEFT_3D_RIGHT) {
                Composite containerTextEditor = new Composite(containerTop, SWT.NONE);
                containerTextEditor.setLayout(new BorderLayout(0, 0));
                EditorTextWindow tWin = new EditorTextWindow(containerTextEditor, this);
                tWin.build();
                tWin.getTabFolder().setWindow(this);
                tWin.registerEvents();
                Project.getOpenTextWindows().add(tWin);
            }
        }

        Composite container = new Composite(containerTop, SWT.NONE);
        container.setLayout(new BorderLayout(0, 0));
        {
            cmpNorth = new Composite(container, SWT.NONE);
            cmpNorth.setLayoutData(BorderLayout.NORTH);
            RowLayout rl_toolBar = new RowLayout(SWT.HORIZONTAL);
            rl_toolBar.center = true;
            cmpNorth.setLayout(rl_toolBar);
        }
        {
            cmpEast = new Composite(container, SWT.NONE);
            cmpEast.setLayoutData(BorderLayout.EAST);
            RowLayout rl_toolBar = new RowLayout(SWT.VERTICAL);
            rl_toolBar.center = true;
            cmpEast.setLayout(rl_toolBar);
        }
        {
            cmpWest = new Composite(container, SWT.NONE);
            cmpWest.setLayoutData(BorderLayout.WEST);
            RowLayout rl_toolBar = new RowLayout(SWT.VERTICAL);
            rl_toolBar.center = true;
            cmpWest.setLayout(rl_toolBar);
        }

        HashSet<String> missingItemsToCreate = new HashSet<String>();
        missingItemsToCreate.add("SYNC_AND_RECENT_FILES"); //$NON-NLS-1$
        missingItemsToCreate.add("PROJECT_MANAGEMENT"); //$NON-NLS-1$
        missingItemsToCreate.add("OPEN_SAVE_DATFILE"); //$NON-NLS-1$
        missingItemsToCreate.add("SHOW_AND_HIDE"); //$NON-NLS-1$
        missingItemsToCreate.add("MISC_TOGGLE"); //$NON-NLS-1$
        missingItemsToCreate.add("UNDO_REDO"); //$NON-NLS-1$
        missingItemsToCreate.add("TRANSFORMATION_MODE"); //$NON-NLS-1$
        missingItemsToCreate.add("MANIPULATOR_MODE"); //$NON-NLS-1$
        missingItemsToCreate.add("MANIPULATOR_ACTIONS"); //$NON-NLS-1$
        missingItemsToCreate.add("MISC_CLICK"); //$NON-NLS-1$
        missingItemsToCreate.add("CUT_COPY_PASTE_DELETE"); //$NON-NLS-1$
        missingItemsToCreate.add("OBJECT_MODE"); //$NON-NLS-1$
        missingItemsToCreate.add("INSERT_AT_CURSOR"); //$NON-NLS-1$
        missingItemsToCreate.add("ADD_SOMETHING"); //$NON-NLS-1$
        missingItemsToCreate.add("COLOUR_BAR"); //$NON-NLS-1$
        missingItemsToCreate.add("LINE_THICKNESS"); //$NON-NLS-1$

        ToolItem lastToolItem = null;
        for (ToolItemState s : userSettings.getToolItemConfig3D()) {
            String obj = s.getKey();
            if (obj.equals("SYNC_AND_RECENT_FILES")) { //$NON-NLS-1$
                lastToolItem = createToolItemSync(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // SYNC_AND_RECENT_FILES
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("PROJECT_MANAGEMENT")) { //$NON-NLS-1$
                lastToolItem = createToolItemNewOpenSave(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // PROJECT_MANAGEMENT
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("OPEN_SAVE_DATFILE")) { //$NON-NLS-1$
                lastToolItem = createToolItemNewOpenDat(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // OPEN_SAVE_DATFILE
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("SHOW_AND_HIDE")) { //$NON-NLS-1$
                lastToolItem = createToolItemHideUnhide(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // SHOW_AND_HIDE
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("MISC_TOGGLE")) { //$NON-NLS-1$
                lastToolItem = createToolItemMiscToggle(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // MISC_TOGGLE
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("UNDO_REDO")) { //$NON-NLS-1$
                lastToolItem = createToolItemUndoRedo(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // UNDO_REDO
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("TRANSFORMATION_MODE")) { //$NON-NLS-1$
                lastToolItem = createToolItemTransformationMode(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // TRANSFORMATION_MODE
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("MANIPULATOR_MODE")) { //$NON-NLS-1$
                lastToolItem = createToolItemManipulatorMode(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // MANIPULATOR_MODE
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("MANIPULATOR_ACTIONS")) { //$NON-NLS-1$
                lastToolItem = createToolItemManipulatorActions(s.getDrawLocation(), s.getDrawMode(), s.getLabel(), lastToolItem); // MANIPULATOR_ACTIONS
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("MISC_CLICK")) { //$NON-NLS-1$
                lastToolItem = createToolItemMiscClick(s.getLabel()); // MISC_CLICK
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("CUT_COPY_PASTE_DELETE")) { //$NON-NLS-1$
                lastToolItem = createToolItemCCPD(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // CUT_COPY_PASTE_DELETE
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("OBJECT_MODE")) { //$NON-NLS-1$
                lastToolItem = createToolItemMode(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // OBJECT_MODE
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("INSERT_AT_CURSOR")) { //$NON-NLS-1$
                lastToolItem = createToolItemInsertAtCursorPosition(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // INSERT_AT_CURSOR
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("ADD_SOMETHING")) { //$NON-NLS-1$
                lastToolItem = createToolItemAdd(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // ADD_SOMETHING
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("COLOUR_BAR")) { //$NON-NLS-1$
                lastToolItem = createToolItemColours(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // COLOUR_BAR
                missingItemsToCreate.remove(obj);
            }
            if (obj.equals("LINE_THICKNESS")) { //$NON-NLS-1$
                lastToolItem = createToolItemLineThickness(s.getDrawLocation(), s.getDrawMode(), s.getLabel()); // LINE_THICKNESS
                missingItemsToCreate.remove(obj);
            }
        }

        if (missingItemsToCreate.contains("SYNC_AND_RECENT_FILES")) lastToolItem = createToolItemSync(ToolItemDrawLocation.WEST, ToolItemDrawMode.VERTICAL, ""); // SYNC_AND_RECENT_FILES //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("PROJECT_MANAGEMENT")) lastToolItem = createToolItemNewOpenSave(ToolItemDrawLocation.WEST, ToolItemDrawMode.VERTICAL, ""); // PROJECT_MANAGEMENT //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("MANIPULATOR_MODE")) lastToolItem = createToolItemManipulatorMode(ToolItemDrawLocation.WEST, ToolItemDrawMode.VERTICAL, ""); // MANIPULATOR_MODE //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("MANIPULATOR_ACTIONS")) lastToolItem = createToolItemManipulatorActions(ToolItemDrawLocation.WEST, ToolItemDrawMode.VERTICAL, "", lastToolItem); // MANIPULATOR_ACTIONS //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("MISC_TOGGLE")) lastToolItem = createToolItemMiscToggle(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // MISC_TOGGLE //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("OPEN_SAVE_DATFILE")) lastToolItem = createToolItemNewOpenDat(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // OPEN_SAVE_DATFILE //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("SHOW_AND_HIDE")) lastToolItem = createToolItemHideUnhide(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // SHOW_AND_HIDE //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("UNDO_REDO")) lastToolItem = createToolItemUndoRedo(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // UNDO_REDO //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("TRANSFORMATION_MODE")) lastToolItem = createToolItemTransformationMode(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // TRANSFORMATION_MODE //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("OBJECT_MODE")) lastToolItem = createToolItemMode(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // OBJECT_MODE //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("CUT_COPY_PASTE_DELETE")) lastToolItem = createToolItemCCPD(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // CUT_COPY_PASTE_DELETE //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("MISC_CLICK")) lastToolItem = createToolItemMiscClick(""); // MISC_CLICK //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("INSERT_AT_CURSOR")) lastToolItem = createToolItemInsertAtCursorPosition(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // INSERT_AT_CURSOR //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("ADD_SOMETHING")) lastToolItem = createToolItemAdd(ToolItemDrawLocation.NORTH, ToolItemDrawMode.HORIZONTAL, ""); // ADD_SOMETHING //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("COLOUR_BAR")) lastToolItem = createToolItemColours(ToolItemDrawLocation.EAST, ToolItemDrawMode.VERTICAL, ""); // COLOUR_BAR //$NON-NLS-1$ //$NON-NLS-2$
        if (missingItemsToCreate.contains("LINE_THICKNESS")) lastToolItem = createToolItemLineThickness(ToolItemDrawLocation.EAST, ToolItemDrawMode.VERTICAL, ""); // LINE_THICKNESS //$NON-NLS-1$ //$NON-NLS-2$

        {
            Composite cmp_main_editor = new Composite(container, SWT.BORDER);
            cmp_main_editor.setLayoutData(BorderLayout.CENTER);
            cmp_main_editor.setLayout(new GridLayout(2, false));
            {
                Button btn_SyncTabs = new Button(cmp_main_editor, SWT.TOGGLE);
                this.btn_SyncTabs[0] = btn_SyncTabs;
                btn_SyncTabs.setToolTipText(I18n.E3D_Sync3DEditor);
                btn_SyncTabs.setImage(ResourceManager.getImage("icon16_sync3D.png")); //$NON-NLS-1$

                btn_SyncTabs.setSelection(userSettings.isSyncingTabs());

                {
                    GridData gridDataX = new GridData();
                    gridDataX.horizontalIndent = 10;
                    btn_SyncTabs.setLayoutData(gridDataX);
                }

                CTabFolder tabFolder_OpenDatFiles = new CTabFolder(cmp_main_editor, SWT.CLOSE);
                this.tabFolder_OpenDatFiles[0] = tabFolder_OpenDatFiles;
                tabFolder_OpenDatFiles.setMRUVisible(true);
                tabFolder_OpenDatFiles.setSelectionBackground(new Color[]{Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND), Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)}, new int[]{100}, true);

                {
                    GridData gridDataX = new GridData();

                    gridDataX.horizontalAlignment = SWT.FILL;
                    gridDataX.minimumHeight = 10;
                    gridDataX.minimumWidth = 160;
                    gridDataX.heightHint = tabFolder_OpenDatFiles.getTabHeight() / 3;

                    gridDataX.verticalAlignment = SWT.FILL;

                    gridDataX.grabExcessHorizontalSpace = true;
                    tabFolder_OpenDatFiles.setLayoutData(gridDataX);
                }

                {
                    CTabItem tItem = new CTabItem(tabFolder_OpenDatFiles, SWT.NONE);
                    tItem.setText(I18n.E3D_NoFileSelected);
                }
                {
                    CTabItem tItem = new CTabItem(tabFolder_OpenDatFiles, SWT.NONE);
                    tItem.setText("new.dat*"); //$NON-NLS-1$
                }

                tabFolder_OpenDatFiles.setSelection(1);

                SashForm sashForm = new SashForm(cmp_main_editor, SWT.NONE);

                {
                    GridData gridDataX = new GridData();

                    gridDataX.horizontalSpan = 2;

                    gridDataX.horizontalAlignment = SWT.FILL;
                    gridDataX.minimumHeight = 200;
                    gridDataX.minimumWidth = 160;

                    gridDataX.verticalAlignment = SWT.FILL;
                    gridDataX.grabExcessVerticalSpace = true;

                    gridDataX.grabExcessHorizontalSpace = true;
                    sashForm.setLayoutData(gridDataX);
                }

                Editor3DDesign.setSashForm(sashForm);
                sashForm.setToolTipText(I18n.E3D_DragHint);
                {
                    SashForm sashForm2 = new SashForm(sashForm, SWT.VERTICAL);
                    Composite cmp_Container1 = new Composite(sashForm2, SWT.BORDER);
                    GridLayout gridLayout = new GridLayout(1, true);
                    cmp_Container1.setLayout(gridLayout);
                    {
                        CTabFolder tabFolder_Settings = new CTabFolder(cmp_Container1, SWT.BORDER);
                        this.tabFolder_Settings[0] = tabFolder_Settings;
                        tabFolder_Settings.setMRUVisible(true);
                        tabFolder_Settings.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
                        GridData gridData = new GridData();
                        gridData.horizontalAlignment = SWT.FILL;
                        gridData.minimumHeight = 200;
                        gridData.minimumWidth = 160;
                        gridData.heightHint = 200;

                        gridData.verticalAlignment = SWT.FILL;
                        gridData.grabExcessVerticalSpace = true;

                        gridData.grabExcessHorizontalSpace = true;
                        tabFolder_Settings.setLayoutData(gridData);
                        tabFolder_Settings.setSize(700, 500);

                        CTabItem tItem = new CTabItem(tabFolder_Settings, SWT.NONE);
                        tItem.setText(I18n.E3D_Snapping);
                        {
                            final ScrolledComposite cmp_scroll = new ScrolledComposite(tabFolder_Settings, SWT.V_SCROLL | SWT.H_SCROLL);
                            Composite cmp_snappingArea = new Composite(cmp_scroll, SWT.NONE);
                            tItem.setControl(cmp_scroll);
                            cmp_scroll.setContent(cmp_snappingArea);
                            cmp_scroll.setExpandHorizontal(true);
                            cmp_scroll.setExpandVertical(true);

                            cmp_snappingArea.setLayout(new GridLayout(3, false));
                            ((GridLayout) cmp_snappingArea.getLayout()).verticalSpacing = 0;
                            ((GridLayout) cmp_snappingArea.getLayout()).marginHeight = 0;
                            ((GridLayout) cmp_snappingArea.getLayout()).marginWidth = 0;

                            {
                                Composite cmp_dummy = new Composite(cmp_snappingArea, SWT.NONE);
                                cmp_dummy.setLayout(new FillLayout(SWT.HORIZONTAL));

                                Button btnCoarse = new Button(cmp_dummy, SWT.RADIO);
                                this.btn_Coarse[0] = btnCoarse;
                                btnCoarse.setImage(ResourceManager.getImage("icon8_coarse.png")); //$NON-NLS-1$
                                btnCoarse.setToolTipText(I18n.E3D_Coarse);

                                Button btnMedium = new Button(cmp_dummy, SWT.RADIO);
                                this.btn_Medium[0] = btnMedium;
                                btnMedium.setSelection(true);
                                btnMedium.setImage(ResourceManager.getImage("icon8_medium.png")); //$NON-NLS-1$
                                btnMedium.setToolTipText(I18n.E3D_Medium);

                                Button btnFine = new Button(cmp_dummy, SWT.RADIO);
                                this.btn_Fine[0] = btnFine;
                                btnFine.setImage(ResourceManager.getImage("icon8_fine.png")); //$NON-NLS-1$
                                btnFine.setToolTipText(I18n.E3D_Fine);

                                cmp_dummy.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 3, 1));
                            }

                            {
                                Object[] messageArguments = {I18n.UNIT_CurrentUnit()};
                                MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
                                formatter.setLocale(MyLanguage.LOCALE);
                                formatter.applyPattern(I18n.E3D_MoveSnap);

                                Label lblNewLabel = new Label(cmp_snappingArea, SWT.NONE);
                                lblNewLabel.setText(formatter.format(messageArguments));
                                lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

                            }

                            BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_snappingArea, SWT.NONE, View.NUMBER_FORMAT8F);
                            this.spn_Move[0] = spinner;
                            spinner.setMaximum(new BigDecimal("100")); //$NON-NLS-1$
                            spinner.setMinimum(new BigDecimal("0.0001")); //$NON-NLS-1$
                            spinner.setValue(userSettings.getMedium_move_snap());
                            spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

                            Label lblNewLabel2 = new Label(cmp_snappingArea, SWT.NONE);
                            lblNewLabel2.setText(I18n.E3D_RotateSnap);
                            lblNewLabel2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

                            BigDecimalSpinner spinner2 = new BigDecimalSpinner(cmp_snappingArea, SWT.NONE, View.NUMBER_FORMAT8F);
                            this.spn_Rotate[0] = spinner2;
                            spinner2.setMaximum(new BigDecimal("360.0")); //$NON-NLS-1$
                            spinner2.setMinimum(new BigDecimal("0.0001")); //$NON-NLS-1$
                            spinner2.setValue(userSettings.getMedium_rotate_snap());
                            spinner2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

                            Label lblNewLabel3 = new Label(cmp_snappingArea, SWT.NONE);
                            lblNewLabel3.setText(I18n.E3D_ScaleSnap);
                            lblNewLabel3.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

                            BigDecimalSpinner spinner3 = new BigDecimalSpinner(cmp_snappingArea, SWT.NONE, View.NUMBER_FORMAT8F);
                            this.spn_Scale[0] = spinner3;
                            spinner3.setMaximum(new BigDecimal("100.0")); //$NON-NLS-1$
                            spinner3.setMinimum(new BigDecimal("0.01")); //$NON-NLS-1$
                            spinner3.setValue(userSettings.getMedium_scale_snap());
                            spinner3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

                            Label separator = new Label(cmp_snappingArea, SWT.SEPARATOR | SWT.HORIZONTAL);
                            separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

                            Label lblNewLabel4 = new Label(cmp_snappingArea, SWT.NONE);
                            lblNewLabel4.setText(I18n.UNITS_Name_LDU + " [" + I18n.UNITS_LDU + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                            lblNewLabel4.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

                            final BigDecimalSpinner spinnerLDU = new BigDecimalSpinner(cmp_snappingArea, SWT.NONE, View.NUMBER_FORMAT8F);
                            spinnerLDU.setMaximum(new BigDecimal("9999.99999999")); //$NON-NLS-1$
                            spinnerLDU.setMinimum(new BigDecimal("-9999.99999999")); //$NON-NLS-1$
                            spinnerLDU.setValue(BigDecimal.ZERO);
                            spinnerLDU.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

                            Label lblNewLabel5 = new Label(cmp_snappingArea, SWT.NONE);
                            lblNewLabel5.setText(I18n.UNITS_Name_secondary + " [" + I18n.UNITS_secondary + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                            lblNewLabel5.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

                            final BigDecimalSpinner spinnerMM = new BigDecimalSpinner(cmp_snappingArea, SWT.NONE, View.NUMBER_FORMAT8F);
                            spinnerMM.setMaximum(new BigDecimal("9999.99999999")); //$NON-NLS-1$
                            spinnerMM.setMinimum(new BigDecimal("-9999.99999999")); //$NON-NLS-1$
                            spinnerMM.setValue(BigDecimal.ZERO);
                            spinnerMM.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

                            Label lblNewLabel6 = new Label(cmp_snappingArea, SWT.NONE);
                            lblNewLabel6.setText(I18n.UNITS_Name_primary + " [" + I18n.UNITS_primary + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                            lblNewLabel6.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

                            final BigDecimalSpinner spinnerInch = new BigDecimalSpinner(cmp_snappingArea, SWT.NONE, View.NUMBER_FORMAT8F);
                            spinnerInch.setMaximum(new BigDecimal("9999.99999999")); //$NON-NLS-1$
                            spinnerInch.setMinimum(new BigDecimal("-9999.99999999")); //$NON-NLS-1$
                            spinnerInch.setValue(BigDecimal.ZERO);
                            spinnerInch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

                            final AtomicBoolean change = new AtomicBoolean();

                            spinnerLDU.addValueChangeListener(new ValueChangeAdapter() {
                                @Override
                                public void valueChanged(BigDecimalSpinner spn) {
                                    if (change.get()) return;
                                    change.set(true);
                                    spinnerInch.setValue(spn.getValue().multiply(new BigDecimal(I18n.UNITS_Factor_primary), Threshold.mc));
                                    spinnerMM.setValue(spn.getValue().multiply(new BigDecimal(I18n.UNITS_Factor_secondary), Threshold.mc));
                                    change.set(false);
                                }
                            });

                            spinnerInch.addValueChangeListener(new ValueChangeAdapter() {
                                @Override
                                public void valueChanged(BigDecimalSpinner spn) {
                                    if (change.get()) return;
                                    change.set(true);
                                    spinnerLDU.setValue(spn.getValue().divide(new BigDecimal(I18n.UNITS_Factor_primary), Threshold.mc));
                                    spinnerMM.setValue(spinnerLDU.getValue().multiply(new BigDecimal(I18n.UNITS_Factor_secondary), Threshold.mc));
                                    change.set(false);
                                }
                            });

                            spinnerMM.addValueChangeListener(new ValueChangeAdapter() {
                                @Override
                                public void valueChanged(BigDecimalSpinner spn) {
                                    if (change.get()) return;
                                    change.set(true);
                                    spinnerLDU.setValue(spn.getValue().divide(new BigDecimal(I18n.UNITS_Factor_secondary), Threshold.mc));
                                    spinnerInch.setValue(spinnerLDU.getValue().multiply(new BigDecimal(I18n.UNITS_Factor_primary), Threshold.mc));
                                    change.set(false);
                                }
                            });

                            cmp_scroll.setMinSize(cmp_snappingArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                        }

                        CTabItem tItem2 = new CTabItem(tabFolder_Settings, SWT.NONE);
                        tItem2.setText(I18n.E3D_Selection);

                        {
                            final ScrolledComposite cmp_scroll = new ScrolledComposite(tabFolder_Settings, SWT.V_SCROLL | SWT.H_SCROLL);
                            Composite cmp_bgArea = new Composite(cmp_scroll, SWT.NONE);
                            tItem2.setControl(cmp_scroll);
                            cmp_scroll.setContent(cmp_bgArea);
                            cmp_scroll.setExpandHorizontal(true);
                            cmp_scroll.setExpandVertical(true);

                            cmp_bgArea.setLayout(new GridLayout(3, false));
                            ((GridLayout) cmp_bgArea.getLayout()).verticalSpacing = 0;
                            ((GridLayout) cmp_bgArea.getLayout()).marginHeight = 0;
                            ((GridLayout) cmp_bgArea.getLayout()).marginWidth = 0;

                            {
                                Composite cmp_Dummy = new Composite(cmp_bgArea, SWT.NONE);
                                cmp_Dummy.setLayout(new FillLayout(SWT.HORIZONTAL));

                                Button btn_PreviousSelection = new Button(cmp_Dummy, SWT.NONE);
                                this.btn_PreviousSelection[0] = btn_PreviousSelection;
                                btn_PreviousSelection.setImage(ResourceManager.getImage("icon8_previous.png")); //$NON-NLS-1$
                                btn_PreviousSelection.setToolTipText(I18n.E3D_PreviousItem);

                                Button btn_NextSelection = new Button(cmp_Dummy, SWT.NONE);
                                this.btn_NextSelection[0] = btn_NextSelection;
                                btn_NextSelection.setImage(ResourceManager.getImage("icon8_next.png")); //$NON-NLS-1$
                                btn_NextSelection.setToolTipText(I18n.E3D_NextItem);

                                cmp_Dummy.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 3, 1));
                            }

                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_TextLine);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Composite cmp_LineSetup = new Composite(cmp_bgArea, SWT.NONE);
                                cmp_LineSetup.setLayout(new GridLayout(1, false));

                                Text txt_Line = new Text(cmp_LineSetup, SWT.BORDER);
                                this.txt_Line[0] = txt_Line;
                                txt_Line.setEnabled(false);
                                txt_Line.setEditable(false);
                                txt_Line.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

                                Button btn_moveAdjacentData2 = new Button(cmp_LineSetup, SWT.TOGGLE);
                                this.btn_MoveAdjacentData2[0] = btn_moveAdjacentData2;
                                btn_moveAdjacentData2.setImage(ResourceManager.getImage("icon16_adjacentmove.png")); //$NON-NLS-1$
                                btn_moveAdjacentData2.setText(I18n.E3D_MoveAdjacentData);

                                cmp_LineSetup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.SEPARATOR | SWT.HORIZONTAL);
                                lbl_Label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionX1[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionX1);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionX1[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionY1[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionY1);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionY1[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionZ1[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionZ1);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionZ1[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionX2[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionX2);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionX2[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionY2[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionY2);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionY2[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionZ2[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionZ2);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionZ2[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionX3[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionX3);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionX3[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionY3[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionY3);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionY3[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionZ3[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionZ3);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionZ3[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionX4[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionX4);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionX4[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionY4[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionY4);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionY4[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                this.lbl_SelectionZ4[0] = lbl_Label;
                                lbl_Label.setText(I18n.E3D_PositionZ4);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                this.spn_SelectionZ4[0] = spinner;
                                spinner.setEnabled(false);
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            cmp_scroll.setMinSize(cmp_bgArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                        }

                        CTabItem tItem3 = new CTabItem(tabFolder_Settings, SWT.NONE);
                        tItem3.setText(I18n.E3D_BackgroundImage);

                        {
                            final ScrolledComposite cmp_scroll = new ScrolledComposite(tabFolder_Settings, SWT.V_SCROLL | SWT.H_SCROLL);
                            Composite cmp_bgArea = new Composite(cmp_scroll, SWT.NONE);
                            tItem3.setControl(cmp_scroll);
                            cmp_scroll.setContent(cmp_bgArea);
                            cmp_scroll.setExpandHorizontal(true);
                            cmp_scroll.setExpandVertical(true);

                            cmp_bgArea.setLayout(new GridLayout(3, false));
                            ((GridLayout) cmp_bgArea.getLayout()).verticalSpacing = 0;
                            ((GridLayout) cmp_bgArea.getLayout()).marginHeight = 0;
                            ((GridLayout) cmp_bgArea.getLayout()).marginWidth = 0;

                            {
                                Composite cmp_dummy = new Composite(cmp_bgArea, SWT.NONE);
                                cmp_dummy.setLayout(new FillLayout(SWT.HORIZONTAL));

                                Button btnPrevious = new Button(cmp_dummy, SWT.NONE);
                                btn_PngPrevious[0] = btnPrevious;
                                btnPrevious.setImage(ResourceManager.getImage("icon8_previous.png")); //$NON-NLS-1$
                                btnPrevious.setToolTipText(I18n.E3D_Previous);

                                Button btnFocusBG = new Button(cmp_dummy, SWT.NONE);
                                btn_PngFocus[0] = btnFocusBG;
                                btnFocusBG.setImage(ResourceManager.getImage("icon8_focus.png")); //$NON-NLS-1$
                                btnFocusBG.setToolTipText(I18n.E3D_Focus);

                                Button btnNext = new Button(cmp_dummy, SWT.NONE);
                                btn_PngNext[0] = btnNext;
                                btnNext.setImage(ResourceManager.getImage("icon8_next.png")); //$NON-NLS-1$
                                btnNext.setToolTipText(I18n.E3D_Next);

                                cmp_dummy.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 3, 1));
                            }

                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_Image);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Composite cmp_pathChooser1 = new Composite(cmp_bgArea, SWT.NONE);
                                cmp_pathChooser1.setLayout(new GridLayout(2, false));

                                Text txt_pngPath = new Text(cmp_pathChooser1, SWT.BORDER);
                                this.txt_PngPath[0] = txt_pngPath;
                                txt_pngPath.setEditable(false);
                                txt_pngPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

                                Button btn_BrowsePngPath = new Button(cmp_pathChooser1, SWT.NONE);
                                btn_PngImage[0] = btn_BrowsePngPath;
                                btn_BrowsePngPath.setText(I18n.DIALOG_Browse);

                                cmp_pathChooser1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_PositionX);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                spn_PngX[0] = spinner;
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_PositionY);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                spn_PngY[0] = spinner;
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_PositionZ);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                spn_PngZ[0] = spinner;
                                spinner.setMaximum(new BigDecimal("1E10")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E10")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_AngleY);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                spn_PngA1[0] = spinner;
                                spinner.setMaximum(new BigDecimal("360")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-360")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_AngleX);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                spn_PngA2[0] = spinner;
                                spinner.setMaximum(new BigDecimal("360")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-360")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_AngleZ);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                spn_PngA3[0] = spinner;
                                spinner.setMaximum(new BigDecimal("360")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-360")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_ScaleX);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                spn_PngSX[0] = spinner;
                                spinner.setMaximum(new BigDecimal("1E6")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E6")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            {
                                Label lbl_Label = new Label(cmp_bgArea, SWT.NONE);
                                lbl_Label.setText(I18n.E3D_ScaleY);
                                lbl_Label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
                                BigDecimalSpinner spinner = new BigDecimalSpinner(cmp_bgArea, SWT.NONE);
                                spn_PngSY[0] = spinner;
                                spinner.setMaximum(new BigDecimal("1E6")); //$NON-NLS-1$
                                spinner.setMinimum(new BigDecimal("-1E6")); //$NON-NLS-1$
                                spinner.setValue(new BigDecimal(0));
                                spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
                            }
                            cmp_scroll.setMinSize(cmp_bgArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                        }

                        tabFolder_Settings.setSelection(tItem);
                    }
                    Composite cmp_Container2 = new Composite(sashForm2, SWT.BORDER);
                    GridLayout gridLayout3 = new GridLayout(1, true);
                    cmp_Container2.setLayout(gridLayout3);
                    {
                        Tree treeAllParts = new Tree(cmp_Container2, SWT.BORDER, 1); // 4096);
                        this.treeParts[0] = treeAllParts;
                        TreeItem treeItemProjectName = new TreeItem(treeAllParts, SWT.NONE);
                        this.treeItem_Project[0] = treeItemProjectName;
                        treeItemProjectName.setText(I18n.PROJECT_NewProject);
                        TreeItem treeItemProjectParts = new TreeItem(treeItemProjectName, SWT.NONE);
                        this.treeItem_ProjectParts[0] = treeItemProjectParts;
                        treeItemProjectParts.setText(I18n.PARTS_Parts);

                        TreeItem treeItemNewPart = new TreeItem(treeItemProjectParts, SWT.NONE);
                        treeItemNewPart.setData(Project.getFileToEdit());
                        Project.addUnsavedFile(Project.getFileToEdit());


                        if (NLogger.DEBUG) {
                            UTF8BufferedReader reader = null;
                            try {
                                reader = new UTF8BufferedReader("testsource.txt"); //$NON-NLS-1$
                                StringBuilder sb = new StringBuilder();
                                String s;
                                while ((s = reader.readLine()) != null) {
                                    sb.append(s);
                                    sb.append(StringHelper.getLineDelimiter());
                                }
                                Project.getFileToEdit().setText(sb.toString());
                            } catch (LDParsingException e) {
                            } catch (FileNotFoundException e) {
                            } catch (UnsupportedEncodingException e) {
                            } finally {
                                try {
                                    if (reader != null)
                                        reader.close();
                                } catch (LDParsingException e1) {
                                }
                            }
                        } else {
                            Project.getFileToEdit().setText(WorkbenchManager.getDefaultFileHeader());
                        }

                        TreeItem treeItemProjectSubparts = new TreeItem(treeItemProjectName, SWT.NONE);
                        this.treeItem_ProjectSubparts[0] = treeItemProjectSubparts;
                        treeItemProjectSubparts.setText(I18n.PARTS_Subparts);
                        TreeItem treeItemProjectPrimitives = new TreeItem(treeItemProjectName, SWT.NONE);
                        this.treeItem_ProjectPrimitives[0] = treeItemProjectPrimitives;
                        treeItemProjectPrimitives.setText(I18n.PARTS_Primitives);
                        TreeItem treeItemProjectHiResPrimitives = new TreeItem(treeItemProjectName, SWT.NONE);
                        this.treeItem_ProjectPrimitives48[0] = treeItemProjectHiResPrimitives;
                        treeItemProjectHiResPrimitives.setText(I18n.PARTS_HiResPrimitives);
                        TreeItem treeItemProjectLowResPrimitives = new TreeItem(treeItemProjectName, SWT.NONE);
                        this.treeItem_ProjectPrimitives8[0] = treeItemProjectLowResPrimitives;
                        treeItemProjectLowResPrimitives.setText(I18n.PARTS_LowResPrimitives);

                        TreeItem treeItemUnsaved = new TreeItem(treeAllParts, SWT.NONE);
                        this.treeItem_Unsaved[0] = treeItemUnsaved;
                        treeItemUnsaved.setText(I18n.E3D_UnsavedFiles);
                        treeItemUnsaved.setVisible(false);
                        TreeItem treeItemNewPart2 = new TreeItem(treeItem_Unsaved[0], SWT.NONE);
                        treeItemNewPart2.setData(Project.getFileToEdit());
                        treeItemNewPart2.setText(Project.getFileToEdit().getShortName());

                        TreeItem treeItemUnofficial = new TreeItem(treeAllParts, SWT.NONE);
                        this.treeItem_Unofficial[0] = treeItemUnofficial;
                        treeItemUnofficial.setText(I18n.PROJECT_UnofficialLibReadWrite);
                        treeItemUnofficial.setVisible(false);
                        TreeItem treeItemUnofficialParts = new TreeItem(treeItemUnofficial, SWT.NONE);
                        this.treeItem_UnofficialParts[0] = treeItemUnofficialParts;
                        treeItemUnofficialParts.setText(I18n.PARTS_Parts);
                        treeItemUnofficialParts.setVisible(false);
                        TreeItem treeItemUnofficialSubparts = new TreeItem(treeItemUnofficial, SWT.NONE);
                        this.treeItem_UnofficialSubparts[0] = treeItemUnofficialSubparts;
                        treeItemUnofficialSubparts.setText(I18n.PARTS_Subparts);
                        treeItemUnofficialSubparts.setVisible(false);
                        TreeItem treeItemUnofficialPrimitives = new TreeItem(treeItemUnofficial, SWT.NONE);
                        this.treeItem_UnofficialPrimitives[0] = treeItemUnofficialPrimitives;
                        treeItemUnofficialPrimitives.setText(I18n.PARTS_Primitives);
                        treeItemUnofficialPrimitives.setVisible(false);
                        TreeItem treeItemUnofficialHiResPrimitives = new TreeItem(treeItemUnofficial, SWT.NONE);
                        this.treeItem_UnofficialPrimitives48[0] = treeItemUnofficialHiResPrimitives;
                        treeItemUnofficialHiResPrimitives.setText(I18n.PARTS_HiResPrimitives);
                        treeItemUnofficialHiResPrimitives.setVisible(false);
                        TreeItem treeItemUnofficialLowResPrimitives = new TreeItem(treeItemUnofficial, SWT.NONE);
                        this.treeItem_UnofficialPrimitives8[0] = treeItemUnofficialLowResPrimitives;
                        treeItemUnofficialLowResPrimitives.setText(I18n.PARTS_LowResPrimitives);
                        treeItemUnofficialLowResPrimitives.setVisible(false);

                        TreeItem treeItemOfficial = new TreeItem(treeAllParts, SWT.NONE);
                        this.treeItem_Official[0] = treeItemOfficial;
                        treeItemOfficial.setText(I18n.PROJECT_OfficialLibRead);
                        treeItemOfficial.setVisible(false);
                        TreeItem treeItemOfficialParts = new TreeItem(treeItemOfficial, SWT.NONE);
                        this.treeItem_OfficialParts[0] = treeItemOfficialParts;
                        treeItemOfficialParts.setText(I18n.PARTS_Parts);
                        treeItemOfficialParts.setVisible(false);
                        TreeItem treeItemOfficialSubparts = new TreeItem(treeItemOfficial, SWT.NONE);
                        this.treeItem_OfficialSubparts[0] = treeItemOfficialSubparts;
                        treeItemOfficialSubparts.setText(I18n.PARTS_Subparts);
                        treeItemOfficialSubparts.setVisible(false);
                        TreeItem treeItemOfficialPrimitives = new TreeItem(treeItemOfficial, SWT.NONE);
                        this.treeItem_OfficialPrimitives[0] = treeItemOfficialPrimitives;
                        treeItemOfficialPrimitives.setText(I18n.PARTS_Primitives);
                        treeItemOfficialPrimitives.setVisible(false);
                        TreeItem treeItemOfficialHiResPrimitives = new TreeItem(treeItemOfficial, SWT.NONE);
                        this.treeItem_OfficialPrimitives48[0] = treeItemOfficialHiResPrimitives;
                        treeItemOfficialHiResPrimitives.setText(I18n.PARTS_HiResPrimitives);
                        treeItemOfficialHiResPrimitives.setVisible(false);
                        TreeItem treeItemOfficialLowResPrimitives = new TreeItem(treeItemOfficial, SWT.NONE);
                        this.treeItem_OfficialPrimitives8[0] = treeItemOfficialLowResPrimitives;
                        treeItemOfficialLowResPrimitives.setText(I18n.PARTS_LowResPrimitives);
                        treeItemOfficialLowResPrimitives.setVisible(false);

                        GridData gridData = new GridData();
                        gridData.horizontalAlignment = SWT.FILL;
                        gridData.verticalAlignment = SWT.FILL;
                        gridData.grabExcessVerticalSpace = true;
                        gridData.grabExcessHorizontalSpace = true;
                        treeAllParts.setLayoutData(gridData);
                    }
                    {
                        Composite cmp_Search = new Composite(cmp_Container2, SWT.NONE);
                        GridData gridData = new GridData();
                        gridData.horizontalAlignment = SWT.FILL;
                        gridData.grabExcessHorizontalSpace = true;
                        cmp_Search.setLayoutData(gridData);
                        GridLayout gridLayout2 = new GridLayout(2, false);
                        cmp_Search.setLayout(gridLayout2);
                        Text txt_Search = new Text(cmp_Search, SWT.BORDER);
                        this.txt_Search[0] = txt_Search;
                        txt_Search.setMessage(I18n.E3D_Search);
                        GridData gridData2 = new GridData();
                        gridData2.horizontalAlignment = SWT.FILL;
                        gridData2.grabExcessHorizontalSpace = true;
                        txt_Search.setLayoutData(gridData2);
                        Button btn_ResetSearch = new Button(cmp_Search, SWT.NONE);
                        this.btn_ResetSearch[0] = btn_ResetSearch;
                        btn_ResetSearch.setText(I18n.E3D_Reset);
                    }

                    Composite cmp_Container4 = new Composite(sashForm2, SWT.BORDER);
                    GridLayout gridLayout4 = new GridLayout(1, true);
                    cmp_Container4.setLayout(gridLayout4);

                    CompositePrimitive cmp_Primitives = new CompositePrimitive(cmp_Container4);
                    this.cmp_Primitives[0] = cmp_Primitives;

                    Matrix4f[] primitiveViewport = windowState.getPrimitiveViewport();
                    if (primitiveViewport != null) {
                        cmp_Primitives.setViewport2(primitiveViewport);
                    }

                    cmp_Primitives.setLayout(new FillLayout());

                    GridData gd = new GridData();
                    gd.grabExcessHorizontalSpace = true;
                    gd.grabExcessVerticalSpace = true;
                    gd.horizontalAlignment = SWT.FILL;
                    gd.verticalAlignment = SWT.FILL;
                    cmp_Primitives.setLayoutData(gd);

                    cmp_Primitives.loadPrimitives();

                    Label lbl_selectedPrimitiveItem = new Label(cmp_Container4, SWT.NONE);
                    this.lbl_selectedPrimitiveItem[0] = lbl_selectedPrimitiveItem;
                    {
                        GridData gd2 = new GridData();
                        gd2.grabExcessHorizontalSpace = true;
                        gd2.horizontalAlignment = SWT.FILL;
                        lbl_selectedPrimitiveItem.setLayoutData(gd2);
                    }
                    lbl_selectedPrimitiveItem.setText(I18n.E3D_NoPrimitiveSelected);

                    {
                        Composite cmp_Search = new Composite(cmp_Container4, SWT.NONE);
                        GridData gridData = new GridData();
                        gridData.horizontalAlignment = SWT.FILL;
                        gridData.grabExcessHorizontalSpace = true;
                        cmp_Search.setLayoutData(gridData);
                        GridLayout gridLayout2 = new GridLayout(4, false);
                        cmp_Search.setLayout(gridLayout2);
                        Text txt_Search = new Text(cmp_Search, SWT.BORDER);
                        this.txt_primitiveSearch[0] = txt_Search;
                        txt_Search.setMessage(I18n.E3D_SearchPrimitives);
                        GridData gridData2 = new GridData();
                        gridData2.horizontalAlignment = SWT.FILL;
                        gridData2.grabExcessHorizontalSpace = true;
                        txt_Search.setLayoutData(gridData2);
                        Button btn_ResetSearch = new Button(cmp_Search, SWT.NONE);
                        this.btn_resetPrimitiveSearch[0] = btn_ResetSearch;
                        btn_ResetSearch.setText(I18n.E3D_Reset);
                        Button btn_ZoomOutPrimitives = new Button(cmp_Search, SWT.NONE);
                        this.btn_zoomOutPrimitives[0] = btn_ZoomOutPrimitives;
                        btn_ZoomOutPrimitives.setText("-"); //$NON-NLS-1$
                        Button btn_ZoomInPrimitives = new Button(cmp_Search, SWT.NONE);
                        this.btn_zoomInPrimitives[0] = btn_ZoomInPrimitives;
                        btn_ZoomInPrimitives.setText("+"); //$NON-NLS-1$
                    }

                    int[] weights = windowState.getLeftSashWeights();
                    if (weights != null) {
                        sashForm2.setWeights(weights);
                    }
                }

                ArrayList<Composite3DState> threeDconfig = windowState.getThreeDwindowConfig();
                if (threeDconfig == null) {
                    @SuppressWarnings("unused")
                    CompositeContainer cmp_Container = new CompositeContainer(sashForm, false);
                    // cmp_Container.getComposite3D().getMntmBottom().setSelection(true);
                    // cmp_Container.getComposite3D().getPerspectiveCalculator().setPerspective(Perspective.BACK);
                    // cmp_Container.getComposite3D().getModifier().splitViewHorizontally();
                } else {
                    final int configSize = threeDconfig.size();
                    if (configSize < 2) {
                        if (configSize == 1) {
                            Composite3DState state = threeDconfig.get(0);
                            createComposite3D(sashForm, null, state);
                        } else {
                            @SuppressWarnings("unused")
                            CompositeContainer cmp_Container = new CompositeContainer(sashForm, false);
                        }
                    } else {
                        // MARK Load the configuration of multiple 3D windows
                        Collections.sort(threeDconfig, new Comparator<Composite3DState>(
                                ) {
                            @Override
                            public int compare(Composite3DState o1, Composite3DState o2) {
                                final int cmp = Integer.compare(o1.getPath().length(), o2.getPath().length());
                                if (cmp == 0) {
                                    return o1.getPath().compareTo(o2.getPath());
                                }
                                return cmp;
                            }
                        });
                        CompositeContainer cmp_Container = new CompositeContainer(sashForm, false);

                        HashSet<String> splitCandidate = new HashSet<String>();
                        HashSet<String> splitAlready = new HashSet<String>();
                        HashMap<String, CompositeContainer> cmpMap = new HashMap<String, CompositeContainer>();
                        HashMap<String, Composite3DState> sMap = new HashMap<String, Composite3DState>();

                        splitCandidate.add("|"); //$NON-NLS-1$
                        splitAlready.add("|"); //$NON-NLS-1$
                        {
                            SashForm sf;
                            if (!threeDconfig.get(0).isVertical()) {
                                sf = cmp_Container.getComposite3D().getModifier().splitViewVertically();
                            } else {
                                sf = cmp_Container.getComposite3D().getModifier().splitViewHorizontally();
                            }
                            sf.setWeights(threeDconfig.get(0).getWeights());
                            cmpMap.put("|s1|", (CompositeContainer) sf.getChildren()[0]); //$NON-NLS-1$
                            cmpMap.put("|s2|", (CompositeContainer) sf.getChildren()[1]); //$NON-NLS-1$
                        }
                        for (Composite3DState state : threeDconfig) {
                            String path = state.getPath();
                            String parentPath = state.getParentPath();
                            if (state.isSash()) {
                                sMap.put(path, state);
                            }
                            if (!splitAlready.contains(path) && !splitCandidate.contains(path)) {
                                splitCandidate.add(path);
                            }
                            if (splitCandidate.contains(parentPath) && !splitAlready.contains(parentPath) && cmpMap.containsKey(parentPath) && sMap.containsKey(parentPath)) {
                                {
                                    Composite3DState state2 = sMap.get(parentPath);
                                    CompositeContainer c = cmpMap.get(parentPath);
                                    SashForm sf;
                                    if (!state2.isVertical()) {
                                        sf = c.getComposite3D().getModifier().splitViewVertically();
                                    } else {
                                        sf = c.getComposite3D().getModifier().splitViewHorizontally();
                                    }
                                    sf.setWeights(state2.getWeights());
                                    cmpMap.remove(parentPath);
                                    cmpMap.put(parentPath + "s1|", (CompositeContainer) sf.getChildren()[0]); //$NON-NLS-1$
                                    cmpMap.put(parentPath + "s2|", (CompositeContainer) sf.getChildren()[1]); //$NON-NLS-1$
                                }
                                splitAlready.add(parentPath);
                            }
                        }

                        for (Composite3DState state : threeDconfig) {
                            String path = state.getPath();
                            if (cmpMap.containsKey(path)) {
                                createComposite3D(null, cmpMap.get(path), state);
                            }
                        }
                    }
                }

                int width = windowState.getWindowState().getSizeAndPosition().width;
                int[] sashSize = windowState.getLeftSashWidth();
                if (sashSize == null) {
                    sashForm.setWeights(new int[] { width / 3, width - width / 3 });
                } else {
                    try {
                        sashForm.setWeights(sashSize);
                    } catch (IllegalArgumentException iae) {
                        sashForm.setWeights(new int[] { width / 3, width - width / 3 });
                    }
                }
            }
        }
        status = new Composite(container, SWT.NONE);
        status.setLayoutData(BorderLayout.SOUTH);
        RowLayout rl_statusBar = new RowLayout(SWT.HORIZONTAL);
        rl_statusBar.center = true;
        status.setLayout(rl_statusBar);
        {
            Label lbl_Status = new Label(status, SWT.NONE);
            lbl_Status.setText(""); //$NON-NLS-1$
        }

        if (userSettings.getTextWinArr() == TEXT_RIGHT_3D_LEFT) {
            Composite containerTextEditor = new Composite(containerTop, SWT.NONE);
            containerTextEditor.setLayout(new BorderLayout(0, 0));
            EditorTextWindow tWin = new EditorTextWindow(containerTextEditor, this);
            tWin.build();
            tWin.getTabFolder().setWindow(this);
            tWin.registerEvents();
            Project.getOpenTextWindows().add(tWin);
        }
        return container;
    }

    private ToolItem createToolItemLineThickness(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_LineThickness = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_lineSize1 = new Button(toolItem_LineThickness, SWT.TOGGLE);
            this.btn_lineSize1[0] = btn_lineSize1;
            btn_lineSize1.setToolTipText(I18n.E3D_LineSize1);
            btn_lineSize1.setImage(ResourceManager.getImage("icon16_linesize1.png")); //$NON-NLS-1$
        }
        {
            Button btn_lineSize2 = new Button(toolItem_LineThickness, SWT.TOGGLE);
            this.btn_lineSize2[0] = btn_lineSize2;
            btn_lineSize2.setToolTipText(I18n.E3D_LineSize2);
            btn_lineSize2.setImage(ResourceManager.getImage("icon16_linesize2.png")); //$NON-NLS-1$
        }
        {
            Button btn_lineSize3 = new Button(toolItem_LineThickness, SWT.TOGGLE);
            btn_lineSize3.setSelection(true);
            this.btn_lineSize3[0] = btn_lineSize3;
            btn_lineSize3.setToolTipText(I18n.E3D_LineSize3);
            btn_lineSize3.setImage(ResourceManager.getImage("icon16_linesize3.png")); //$NON-NLS-1$
        }
        {
            Button btn_lineSize4 = new Button(toolItem_LineThickness, SWT.TOGGLE);
            this.btn_lineSize4[0] = btn_lineSize4;
            btn_lineSize4.setToolTipText(I18n.E3D_LineSize4);
            btn_lineSize4.setImage(ResourceManager.getImage("icon16_linesize4.png")); //$NON-NLS-1$
        }
        {
            Button btn_toggleLinesOpenGL = new Button(target, SWT.TOGGLE);
            this.btn_ToggleLinesOpenGL[0] = btn_toggleLinesOpenGL;
            btn_toggleLinesOpenGL.setToolTipText(I18n.E3D_LineOpenGL);
            btn_toggleLinesOpenGL.setImage(ResourceManager.getImage("icon16_gllines.png")); //$NON-NLS-1$
        }
        return toolItem_LineThickness;
    }

    private ToolItem createToolItemColours(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_Colours = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        toolItem_ColourBar = toolItem_Colours;
        List<GColour> colours = WorkbenchManager.getUserSettingState().getUserPalette();

        {
            final int size = colours.size();
            for (int i = 0; i < size; i++) {
                addColorButton(toolItem_Colours, colours.get(i), i);
            }
        }

        {
            Button btn_Palette = new Button(toolItem_Colours, SWT.NONE);
            this.btn_Palette[0] = btn_Palette;
            btn_Palette.setToolTipText(I18n.E3D_More);
            btn_Palette.setImage(ResourceManager.getImage("icon16_colours.png")); //$NON-NLS-1$
        }

        ToolItem toolItem_ColourFunctions = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_LastUsedColour = new Button(toolItem_ColourFunctions, SWT.NONE);
            this.btn_LastUsedColour[0] = btn_LastUsedColour;
            btn_LastUsedColour.setToolTipText(I18n.E3D_Colour16);
            btn_LastUsedColour.setImage(ResourceManager.getImage("icon16_fullTransparent.png")); //$NON-NLS-1$
            final Color col = SWTResourceManager.getColor(128, 128, 128);
            final Point size = btn_LastUsedColour.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            final int x = Math.round(size.x / 5f);
            final int y = Math.round(size.y / 5f);
            final int w = Math.round(size.x * (3f / 5f));
            final int h = Math.round(size.y * (3f / 5f));
            btn_LastUsedColour.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e) {
                    e.gc.setBackground(col);
                    e.gc.fillRectangle(x, y, w, h);
                }
            });
            btn_LastUsedColour.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (Project.getFileToEdit() != null) {
                        Project.getFileToEdit().getVertexManager().addSnapshot();
                        Project.getFileToEdit().getVertexManager().colourChangeSelection(16, .5f, .5f, .5f, 1f, true);
                    }
                    Editor3DWindow.getWindow().regainFocus();
                }
            });
        }
        {
            Button btn_Pipette = new Button(toolItem_ColourFunctions, SWT.NONE);
            this.btn_Pipette[0] = btn_Pipette;
            btn_Pipette.setToolTipText(I18n.E3D_Pipette);
            btn_Pipette.setImage(ResourceManager.getImage("icon16_pipette.png")); //$NON-NLS-1$
        }
        {
            Button btn_Decolour = new Button(toolItem_ColourFunctions, SWT.NONE);
            this.btn_Decolour[0] = btn_Decolour;
            btn_Decolour.setToolTipText(I18n.E3D_Decolour);
            btn_Decolour.setImage(ResourceManager.getImage("icon16_uncolour.png")); //$NON-NLS-1$
        }
        return toolItem_ColourFunctions;
    }

    private ToolItem createToolItemAdd(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_Add = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);

        Button btn_AddComment = new Button(toolItem_Add, SWT.NONE);
        this.btn_AddComment[0] = btn_AddComment;
        KeyStateManager.addTooltipText(btn_AddComment, I18n.E3D_AddComment, Task.ADD_COMMENTS);
        btn_AddComment.setImage(ResourceManager.getImage("icon16_addcomment.png")); //$NON-NLS-1$

        Button btn_AddVertex = new Button(toolItem_Add, SWT.TOGGLE);
        this.btn_AddVertex[0] = btn_AddVertex;
        KeyStateManager.addTooltipText(btn_AddVertex, I18n.E3D_AddVertex, Task.ADD_VERTEX);
        btn_AddVertex.setImage(ResourceManager.getImage("icon16_addvertex.png")); //$NON-NLS-1$

        Button btn_AddPrimitive = new Button(toolItem_Add, SWT.TOGGLE);
        this.btn_AddPrimitive[0] = btn_AddPrimitive;
        btn_AddPrimitive.setToolTipText(I18n.E3D_AddSubpart);
        btn_AddPrimitive.setImage(ResourceManager.getImage("icon16_addprimitive.png")); //$NON-NLS-1$

        Button btn_AddLine = new Button(toolItem_Add, SWT.TOGGLE);
        this.btn_AddLine[0] = btn_AddLine;
        KeyStateManager.addTooltipText(btn_AddLine, I18n.E3D_AddLine, Task.ADD_LINE);
        btn_AddLine.setImage(ResourceManager.getImage("icon16_addline.png")); //$NON-NLS-1$

        Button btn_AddTriangle = new Button(toolItem_Add, SWT.TOGGLE);
        this.btn_AddTriangle[0] = btn_AddTriangle;
        KeyStateManager.addTooltipText(btn_AddTriangle, I18n.E3D_AddTriangle, Task.ADD_TRIANGLE);
        btn_AddTriangle.setImage(ResourceManager.getImage("icon16_addtriangle.png")); //$NON-NLS-1$

        Button btn_AddQuad = new Button(toolItem_Add, SWT.TOGGLE);
        this.btn_AddQuad[0] = btn_AddQuad;
        KeyStateManager.addTooltipText(btn_AddQuad, I18n.E3D_AddQuad, Task.ADD_QUAD);
        btn_AddQuad.setImage(ResourceManager.getImage("icon16_addquad.png")); //$NON-NLS-1$

        Button btn_AddCondline = new Button(toolItem_Add, SWT.TOGGLE);
        this.btn_AddCondline[0] = btn_AddCondline;
        KeyStateManager.addTooltipText(btn_AddCondline, I18n.E3D_AddCondline, Task.ADD_CONDLINE);
        btn_AddCondline.setImage(ResourceManager.getImage("icon16_addcondline.png")); //$NON-NLS-1$

        Button btn_AddDistance = new Button(toolItem_Add, SWT.TOGGLE);
        this.btn_AddDistance[0] = btn_AddDistance;
        KeyStateManager.addTooltipText(btn_AddDistance, I18n.E3D_AddDistance, Task.ADD_DISTANCE);
        btn_AddDistance.setImage(ResourceManager.getImage("icon16_adddistance.png")); //$NON-NLS-1$

        Button btn_AddProtractor = new Button(toolItem_Add, SWT.TOGGLE);
        this.btn_AddProtractor[0] = btn_AddProtractor;
        KeyStateManager.addTooltipText(btn_AddProtractor, I18n.E3D_AddProtractor, Task.ADD_PROTRACTOR);
        btn_AddProtractor.setImage(ResourceManager.getImage("icon16_addprotractor.png")); //$NON-NLS-1$

        return toolItem_Add;
    }

    private ToolItem createToolItemInsertAtCursorPosition(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_InsertAtCursorPosition = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        Button btn_InsertAtCursorPosition = new Button(toolItem_InsertAtCursorPosition, SWT.TOGGLE);
        this.btn_InsertAtCursorPosition[0] = btn_InsertAtCursorPosition;
        KeyStateManager.addTooltipText(btn_InsertAtCursorPosition, I18n.E3D_InsertAtCursorPosition, Task.INSERT_AT_CURSOR);
        btn_InsertAtCursorPosition.setImage(ResourceManager.getImage("icon16_insertAtCursor.png")); //$NON-NLS-1$
        return toolItem_InsertAtCursorPosition;
    }

    private ToolItem createToolItemMode(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_Mode = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_Vertices = new Button(toolItem_Mode, SWT.TOGGLE);
            this.btn_Vertices[0] = btn_Vertices;
            KeyStateManager.addTooltipText(btn_Vertices, I18n.E3D_ModeVertex, Task.OBJ_VERTEX);
            btn_Vertices.setSelection(true);
            btn_Vertices.setImage(ResourceManager.getImage("icon16_vertices.png")); //$NON-NLS-1$
        }
        {
            Button btn_TrisNQuads = new Button(toolItem_Mode, SWT.TOGGLE);
            this.btn_TrisNQuads[0] = btn_TrisNQuads;
            KeyStateManager.addTooltipText(btn_TrisNQuads, I18n.E3D_ModeSurface, Task.OBJ_FACE);
            btn_TrisNQuads.setImage(ResourceManager.getImage("icon16_trisNquads.png")); //$NON-NLS-1$
        }
        {
            Button btn_Lines = new Button(toolItem_Mode, SWT.TOGGLE);
            this.btn_Lines[0] = btn_Lines;
            KeyStateManager.addTooltipText(btn_Lines, I18n.E3D_ModeLine, Task.OBJ_LINE);
            btn_Lines.setImage(ResourceManager.getImage("icon16_lines.png")); //$NON-NLS-1$
        }
        {
            Button btn_Subfiles = new Button(toolItem_Mode, SWT.TOGGLE);
            this.btn_Subfiles[0] = btn_Subfiles;
            KeyStateManager.addTooltipText(btn_Subfiles, I18n.E3D_ModeSubpart, Task.OBJ_PRIMITIVE);
            btn_Subfiles.setImage(ResourceManager.getImage("icon16_primitives.png")); //$NON-NLS-1$
        }
        return toolItem_Mode;
    }

    private ToolItem createToolItemCCPD(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_CCPD = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_Cut = new Button(toolItem_CCPD, SWT.NONE);
            this.btn_Cut[0] = btn_Cut;
            btn_Cut.setImage(ResourceManager.getImage("icon16_edit-cut.png")); //$NON-NLS-1$
            KeyStateManager.addTooltipText(btn_Cut, I18n.COPYNPASTE_Cut, Task.CUT);
        }
        {
            Button btn_Copy = new Button(toolItem_CCPD, SWT.NONE);
            this.btn_Copy[0] = btn_Copy;
            btn_Copy.setImage(ResourceManager.getImage("icon16_edit-copy.png")); //$NON-NLS-1$
            KeyStateManager.addTooltipText(btn_Copy, I18n.COPYNPASTE_Copy, Task.COPY);
        }
        {
            Button btn_Paste = new Button(toolItem_CCPD, SWT.NONE);
            this.btn_Paste[0] = btn_Paste;
            btn_Paste.setImage(ResourceManager.getImage("icon16_edit-paste.png")); //$NON-NLS-1$
            KeyStateManager.addTooltipText(btn_Paste, I18n.COPYNPASTE_Paste, Task.PASTE);
        }
        {
            Button btn_Delete = new Button(toolItem_CCPD, SWT.NONE);
            this.btn_Delete[0] = btn_Delete;
            btn_Delete.setImage(ResourceManager.getImage("icon16_delete.png")); //$NON-NLS-1$
            KeyStateManager.addTooltipText(btn_Delete, I18n.COPYNPASTE_Delete, Task.DELETE);
        }
        return toolItem_CCPD;
    }

    private ToolItem createToolItemMiscClick(String label) {
        ToolItem toolItem_MiscClick = new ToolItem(cmpNorth, SWT.NONE, true);
        {
            Button btn_ShowSelectionInTextEditor = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_ShowSelectionInTextEditor[0] = btn_ShowSelectionInTextEditor;
            btn_ShowSelectionInTextEditor.setToolTipText(I18n.C3D_ShowInText);
            btn_ShowSelectionInTextEditor.setImage(ResourceManager.getImage("icon16_selection2text.png")); //$NON-NLS-1$
        }
        {
            Button btn_BFCswap = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_BFCswap[0] = btn_BFCswap;
            btn_BFCswap.setToolTipText(I18n.E3D_SwapWinding);
            btn_BFCswap.setImage(ResourceManager.getImage("icon16_bfcSwap.png")); //$NON-NLS-1$
        }
        {
            Button btn_CompileSubfile = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_CompileSubfile[0] = btn_CompileSubfile;
            btn_CompileSubfile.setToolTipText(I18n.E3D_CompileSubfileData);
            btn_CompileSubfile.setImage(ResourceManager.getImage("icon16_subcompile.png")); //$NON-NLS-1$
        }
        {
            Button btn_SplitQuad = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_SplitQuad[0] = btn_SplitQuad;
            btn_SplitQuad.setImage(ResourceManager.getImage("icon16_quadToTri.png")); //$NON-NLS-1$
            btn_SplitQuad.setToolTipText(I18n.E3D_SplitQuad);
        }
        {
            Button btn_MergeQuad = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_MergeQuad[0] = btn_MergeQuad;
            btn_MergeQuad.setImage(ResourceManager.getImage("icon16_triToquad.png")); //$NON-NLS-1$
            btn_MergeQuad.setToolTipText(I18n.EDITORTEXT_MergeQuad);
        }
        {
            Button btn_CondlineToLine = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_CondlineToLine[0] = btn_CondlineToLine;
            // FIXME Needs icon!
            btn_CondlineToLine.setText("C2L"); //$NON-NLS-1$
            btn_CondlineToLine.setToolTipText(I18n.E3D_CondlineToLine);
        }
        {
            Button btn_LineToCondline = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_LineToCondline[0] = btn_LineToCondline;
            // FIXME Needs icon!
            btn_LineToCondline.setText("L2C"); //$NON-NLS-1$
            btn_LineToCondline.setToolTipText(I18n.E3D_LineToCondline);
        }
        {
            Button btn_MoveOnLine = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_MoveOnLine[0] = btn_MoveOnLine;
            // FIXME Needs icon!
            btn_MoveOnLine.setText("ML"); //$NON-NLS-1$
            btn_MoveOnLine.setToolTipText(I18n.E3D_MoveOnLine);
        }
        {
            Button btn_RoundSelection = new Button(toolItem_MiscClick, SWT.NONE);
            this.btn_RoundSelection[0] = btn_RoundSelection;
            btn_RoundSelection.setToolTipText(I18n.E3D_Round + I18n.E3D_ControlClickModify);
            btn_RoundSelection.setImage(ResourceManager.getImage("icon16_round.png")); //$NON-NLS-1$
        }
        {
            final Button btn_Select = new Button(toolItem_MiscClick, SWT.PUSH);
            this.btn_Select2[0] = btn_Select;
            btn_Select.setToolTipText(I18n.E3D_AdvancedSelect);
            btn_Select.setText(I18n.E3D_AdvancedSelect);
            this.mnu_Select = new Menu(this.getShell(), SWT.POP_UP);
            btn_Select.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showSelectMenu();
                    Editor3DWindow.getWindow().regainFocus();
                }
            });
            {
                {
                    MenuItem mntm_SelectAll = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectAll[0] = mntm_SelectAll;
                    KeyStateManager.addKeyText(mntm_SelectAll, I18n.E3D_All, Task.SELECT_ALL);
                }
                {
                    MenuItem mntm_SelectNone = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectNone[0] = mntm_SelectNone;
                    KeyStateManager.addKeyText(mntm_SelectNone, I18n.E3D_None, Task.SELECT_NONE);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator0 = new MenuItem(mnu_Select, SWT.SEPARATOR);
                {
                    MenuItem mntm_SelectInverse = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectInverse[0] = mntm_SelectInverse;
                    mntm_SelectInverse.setText(I18n.E3D_Inverse);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator1 = new MenuItem(mnu_Select, SWT.SEPARATOR);
                {
                    MenuItem mntm_SelectAllVisible = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectAllVisible[0] = mntm_SelectAllVisible;
                    mntm_SelectAllVisible.setText(I18n.E3D_AllShown);
                }
                {
                    MenuItem mntm_SelectAllWithColours = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectAllWithColours[0] = mntm_SelectAllWithColours;
                    KeyStateManager.addKeyText(mntm_SelectAllWithColours, I18n.E3D_AllSameColours, Task.SELECT_ALL_WITH_SAME_COLOURS);
                }
                {
                    MenuItem mntm_SelectAllVisibleWithColours = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectAllVisibleWithColours[0] = mntm_SelectAllVisibleWithColours;
                    mntm_SelectAllVisibleWithColours.setText(I18n.E3D_AllSameColoursShown);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator2 = new MenuItem(mnu_Select, SWT.SEPARATOR);
                {
                    MenuItem mntm_SelectEverything = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectEverything[0] = mntm_SelectEverything;
                    mntm_SelectEverything.setText(I18n.E3D_Everything);
                    mntm_SelectEverything.setEnabled(false);
                }
                {
                    MenuItem mntm_SelectConnected = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectConnected[0] = mntm_SelectConnected;
                    KeyStateManager.addKeyText(mntm_SelectConnected, I18n.E3D_Connected, Task.SELECT_CONNECTED);
                }
                {
                    MenuItem mntm_SelectTouching = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectTouching[0] = mntm_SelectTouching;
                    KeyStateManager.addKeyText(mntm_SelectTouching, I18n.E3D_Touching, Task.SELECT_TOUCHING);
                }
                {
                    MenuItem mntm_WithSameColour = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_WithSameColour[0] = mntm_WithSameColour;
                    KeyStateManager.addKeyText(mntm_WithSameColour, I18n.E3D_WithSameColour, Task.SELECT_OPTION_WITH_SAME_COLOURS);
                }
                {
                    MenuItem mntm_WithSameOrientation = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_WithSameOrientation[0] = mntm_WithSameOrientation;
                    mntm_WithSameOrientation.setText(I18n.E3D_WithSameOrientation);
                }
                {
                    MenuItem mntm_WithAccuracy = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_WithAccuracy[0] = mntm_WithAccuracy;
                    mntm_WithAccuracy.setText(I18n.E3D_WithAccuracy);
                }
                {
                    MenuItem mntm_WithAdjacency = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_WithAdjacency[0] = mntm_WithAdjacency;
                    mntm_WithAdjacency.setText(I18n.E3D_WithAdjacency);
                }
                {
                    MenuItem mntm_WithHiddenData = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_WithHiddenData[0] = mntm_WithHiddenData;
                    mntm_WithHiddenData.setText(I18n.E3D_WhatIsHidden);
                    mntm_WithHiddenData.setImage(ResourceManager.getImage("icon16_hide.png")); //$NON-NLS-1$
                }
                {
                    MenuItem mntm_ExceptSubfiles = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_ExceptSubfiles[0] = mntm_ExceptSubfiles;
                    mntm_ExceptSubfiles.setText(I18n.E3D_ExceptSubfile);
                }
                {
                    MenuItem mntm_WithWholeSubfiles = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_WithWholeSubfiles[0] = mntm_WithWholeSubfiles;
                    mntm_WithWholeSubfiles.setText(I18n.E3D_WithWholeSubfileSelection);
                }
                {
                    MenuItem mntm_StopAtEdges = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_StopAtEdges[0] = mntm_StopAtEdges;
                    mntm_StopAtEdges.setText(I18n.E3D_StopSelectionAtEdges);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator3 = new MenuItem(mnu_Select, SWT.SEPARATOR);
                {
                    MenuItem mntm_SelectSingleVertex = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectSingleVertex[0] = mntm_SelectSingleVertex;
                    mntm_SelectSingleVertex.setText(I18n.E3D_SelectVertex);
                }
                {
                    MenuItem mntm_SelectIsolatedVertices = new MenuItem(mnu_Select, SWT.PUSH);
                    this.mntm_SelectIsolatedVertices[0] = mntm_SelectIsolatedVertices;
                    mntm_SelectIsolatedVertices.setText(I18n.E3D_IsolatedVertices);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator4 = new MenuItem(mnu_Select, SWT.SEPARATOR);
                {
                    MenuItem mntm_SVertices = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_SVertices[0] = mntm_SVertices;
                    mntm_SVertices.setText(I18n.E3D_Vertices);
                    mntm_SVertices.setSelection(true);
                }
                {
                    MenuItem mntm_SLines = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_SLines[0] = mntm_SLines;
                    mntm_SLines.setText(I18n.E3D_Lines);
                    mntm_SLines.setSelection(true);
                }
                {
                    MenuItem mntm_STriangles = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_STriangles[0] = mntm_STriangles;
                    mntm_STriangles.setText(I18n.E3D_Triangles);
                    mntm_STriangles.setSelection(true);
                }
                {
                    MenuItem mntm_SQuads = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_SQuads[0] = mntm_SQuads;
                    mntm_SQuads.setText(I18n.E3D_Quads);
                    mntm_SQuads.setSelection(true);
                }
                {
                    MenuItem mntm_SCLines = new MenuItem(mnu_Select, SWT.CHECK);
                    this.mntm_SCLines[0] = mntm_SCLines;
                    mntm_SCLines.setText(I18n.E3D_Condlines);
                    mntm_SCLines.setSelection(true);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator5 = new MenuItem(mnu_Select, SWT.SEPARATOR);
                {
                    MenuItem mntm_needsThreshold = new MenuItem(mnu_Select, SWT.PUSH);
                    mntm_needsThreshold.setText(I18n.E3D_NeedsAThreshold);
                    mntm_needsThreshold.setEnabled(false);
                }
                {
                    MenuItem mntm_noEffect = new MenuItem(mnu_Select, SWT.PUSH);
                    mntm_noEffect.setText(I18n.E3D_NoEffectSelectEverything);
                    mntm_noEffect.setEnabled(false);
                }
            }
        }
        {
            final Button btn_MergeNSplit = new Button(toolItem_MiscClick, SWT.PUSH);
            btn_MergeNSplit.setToolTipText(I18n.E3D_MergeSplit);
            btn_MergeNSplit.setText(I18n.E3D_MergeSplit);
            this.mnu_Merge = new Menu(this.getShell(), SWT.POP_UP);
            btn_MergeNSplit.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Point loc = btn_MergeNSplit.getLocation();
                    Rectangle rect = btn_MergeNSplit.getBounds();
                    Point mLoc = new Point(loc.x - 1, loc.y + rect.height);
                    mnu_Merge.setLocation(getShell().getDisplay().map(btn_MergeNSplit.getParent(), null, mLoc));
                    mnu_Merge.setVisible(true);
                    Editor3DWindow.getWindow().regainFocus();
                }
            });
            {
                MenuItem mntm_Flip = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_Flip[0] = mntm_Flip;
                mntm_Flip.setText(I18n.E3D_FlipRotate);
            }
            {
                MenuItem mntm_Smooth = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_Smooth[0] = mntm_Smooth;
                mntm_Smooth.setText(I18n.E3D_Smooth);
            }
            @SuppressWarnings("unused")
            final MenuItem mntmSeparator2 = new MenuItem(mnu_Merge, SWT.SEPARATOR);
            {
                MenuItem mntm_Split = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_Split[0] = mntm_Split;
                KeyStateManager.addKeyText(mntm_Split, I18n.E3D_Split, Task.SPLIT);
            }
            {
                MenuItem mntm_SplitNTimes = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_SplitNTimes[0] = mntm_SplitNTimes;
                mntm_SplitNTimes.setText(I18n.E3D_SplitNTimes);
            }
            @SuppressWarnings("unused")
            final MenuItem mntmSeparator3 = new MenuItem(mnu_Merge, SWT.SEPARATOR);
            {
                MenuItem mntm_mergeTo = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_MergeToAverage[0] = mntm_mergeTo;
                KeyStateManager.addKeyText(mntm_mergeTo, I18n.E3D_MergeToAvg, Task.MERGE_TO_AVERAGE);
            }
            {
                MenuItem mntm_mergeTo = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_MergeToLastSelected[0] = mntm_mergeTo;
                KeyStateManager.addKeyText(mntm_mergeTo, I18n.E3D_MergeToLastSelected, Task.MERGE_TO_LAST);
            }
            {
                MenuItem mntm_mergeTo = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_MergeToNearestVertex[0] = mntm_mergeTo;
                mntm_mergeTo.setText(I18n.E3D_MergeToNearestVertex);
            }
            {
                MenuItem mntm_mergeTo = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_MergeToNearestEdge[0] = mntm_mergeTo;
                mntm_mergeTo.setText(I18n.E3D_MergeToNearestEdge);
            }
            {
                MenuItem mntm_mergeTo = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_MergeToNearestFace[0] = mntm_mergeTo;
                mntm_mergeTo.setText(I18n.E3D_MergeToNearestFace);
            }
            @SuppressWarnings("unused")
            final MenuItem mntmSeparator4 = new MenuItem(mnu_Merge, SWT.SEPARATOR);
            {
                MenuItem mntm_setXYZ = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_setXYZ[0] = mntm_setXYZ;
                mntm_setXYZ.setText(I18n.E3D_SetXYZ);
            }
            {
                MenuItem mntm_Translate = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_Translate[0] = mntm_Translate;
                mntm_Translate.setText(I18n.E3D_TranslateSelection);
            }
            {
                MenuItem mntm_Rotate = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_Rotate[0] = mntm_Rotate;
                mntm_Rotate.setText(I18n.E3D_RotateSelection);
            }
            {
                MenuItem mntm_Scale = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_Scale[0] = mntm_Scale;
                mntm_Scale.setText(I18n.E3D_ScaleSelection);
            }
            @SuppressWarnings("unused")
            final MenuItem mntmSeparator1 = new MenuItem(mnu_Merge, SWT.SEPARATOR);
            {
                MenuItem mntm_SubdivideCatmullClark = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_SubdivideCatmullClark[0] = mntm_SubdivideCatmullClark;
                mntm_SubdivideCatmullClark.setText(I18n.E3D_SubdivideCatmullClark);
            }
            {
                MenuItem mntm_SubdivideLoop = new MenuItem(mnu_Merge, SWT.PUSH);
                this.mntm_SubdivideLoop[0] = mntm_SubdivideLoop;
                mntm_SubdivideLoop.setText(I18n.E3D_SubdivideLoop);
            }
        }
        {
            final Button btn_ToolsActions = new Button(toolItem_MiscClick, SWT.ARROW | SWT.DOWN);
            btn_ToolsActions.setToolTipText(I18n.E3D_ToolsOptions);
            this.mnu_Tools = new Menu(this.getShell(), SWT.POP_UP);
            btn_ToolsActions.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Point loc = btn_ToolsActions.getLocation();
                    Rectangle rect = btn_ToolsActions.getBounds();
                    Point mLoc = new Point(loc.x - 1, loc.y + rect.height);
                    mnu_Tools.setLocation(getShell().getDisplay().map(btn_ToolsActions.getParent(), null, mLoc));
                    mnu_Tools.setVisible(true);
                    Editor3DWindow.getWindow().regainFocus();
                }
            });
            {
                {
                    MenuItem mntm_PartReview = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_PartReview[0] = mntm_PartReview;
                    mntm_PartReview.setText(I18n.E3D_PartReview);
                }
                {
                    MenuItem mntm_Edger2 = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_Edger2[0] = mntm_Edger2;
                    mntm_Edger2.setText(I18n.E3D_Edger2);
                }
                {
                    MenuItem mntm_Edger2 = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_Txt2Dat[0] = mntm_Edger2;
                    mntm_Edger2.setText(I18n.E3D_Txt2Dat);
                }
                {
                    MenuItem mntm_Rectifier = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_Rectifier[0] = mntm_Rectifier;
                    mntm_Rectifier.setText(I18n.E3D_Rectifier);
                }
                {
                    MenuItem mntm_Isecalc = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_Isecalc[0] = mntm_Isecalc;
                    mntm_Isecalc.setText(I18n.E3D_Isecalc);
                }
                {
                    MenuItem mntm_SlicerPro = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_SlicerPro[0] = mntm_SlicerPro;
                    mntm_SlicerPro.setText(I18n.E3D_SlicerPro);
                }
                {
                    MenuItem mntm_Intersector = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_Intersector[0] = mntm_Intersector;
                    mntm_Intersector.setText(I18n.E3D_Intersector);
                }
                {
                    MenuItem mntm_Lines2Pattern = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_Lines2Pattern[0] = mntm_Lines2Pattern;
                    mntm_Lines2Pattern.setText(I18n.E3D_Lines2Pattern);
                }
                {
                    MenuItem mntm_PathTruder = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_PathTruder[0] = mntm_PathTruder;
                    mntm_PathTruder.setText(I18n.E3D_PathTruder);
                }
                {
                    MenuItem mntm_SymSplitter = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_SymSplitter[0] = mntm_SymSplitter;
                    mntm_SymSplitter.setText(I18n.E3D_SymSplitter);
                }
                {
                    MenuItem mntm_Unificator = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_Unificator[0] = mntm_Unificator;
                    mntm_Unificator.setText(I18n.E3D_Unificator);
                }
                {
                    MenuItem mntm_RingsAndCones = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_RingsAndCones[0] = mntm_RingsAndCones;
                    mntm_RingsAndCones.setText(I18n.E3D_RingsAndCones);
                }
                {
                    MenuItem mntm_TJunctionFinder = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_TJunctionFinder[0] = mntm_TJunctionFinder;
                    mntm_TJunctionFinder.setText(I18n.E3D_Tjunction);
                }
                {
                    MenuItem mntm_MeshReducer = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_MeshReducer[0] = mntm_MeshReducer;
                    mntm_MeshReducer.setText(I18n.E3D_MeshReduce);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator1 = new MenuItem(mnu_Tools, SWT.SEPARATOR);
                {
                    MenuItem mntm_Options = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_Options[0] = mntm_Options;
                    mntm_Options.setText(I18n.E3D_Options);
                }
                {
                    MenuItem mntm_ResetSettingsOnRestart = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_ResetSettingsOnRestart[0] = mntm_ResetSettingsOnRestart;
                    mntm_ResetSettingsOnRestart.setText(I18n.E3D_ResetAll);
                }
                {
                    MenuItem mntm_SelectAnotherLDConfig = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_SelectAnotherLDConfig[0] = mntm_SelectAnotherLDConfig;
                    mntm_SelectAnotherLDConfig.setText(I18n.E3D_SelectLDConfig);
                }
                {
                    final MenuItem mntm_Palette = new MenuItem(mnu_Tools, SWT.CASCADE);
                    mntm_Palette.setText(I18n.E3D_Palette);
                    final Menu mnu_Palette = new Menu(mntm_Palette);
                    mntm_Palette.setMenu(mnu_Palette);
                    {
                        MenuItem mntm_SavePalette = new MenuItem(mnu_Palette, SWT.PUSH);
                        this.mntm_SavePalette[0] = mntm_SavePalette;
                        mntm_SavePalette.setText(I18n.E3D_PaletteSave);
                    }
                    {
                        MenuItem mntm_LoadPalette = new MenuItem(mnu_Palette, SWT.PUSH);
                        this.mntm_LoadPalette[0] = mntm_LoadPalette;
                        mntm_LoadPalette.setText(I18n.E3D_PaletteLoad);
                    }
                    {
                        MenuItem mntm_SetPaletteSize = new MenuItem(mnu_Palette, SWT.PUSH);
                        this.mntm_SetPaletteSize[0] = mntm_SetPaletteSize;
                        mntm_SetPaletteSize.setText(I18n.E3D_PaletteSetSize);
                    }
                    {
                        MenuItem mntm_ResetPalette = new MenuItem(mnu_Palette, SWT.PUSH);
                        this.mntm_ResetPalette[0] = mntm_ResetPalette;
                        mntm_ResetPalette.setText(I18n.E3D_PaletteReset);
                    }
                }
                {
                    final MenuItem mntm_SetIconSize = new MenuItem(mnu_Tools, SWT.CASCADE);
                    mntm_SetIconSize.setText(I18n.E3D_SetIconSize);
                    final Menu mnu_IconSize = new Menu(mntm_SetIconSize);
                    mntm_SetIconSize.setMenu(mnu_IconSize);
                    final int iconSize = Editor3DWindow.getIconsize();
                    {
                        MenuItem mntm_IconSize = new MenuItem(mnu_IconSize, SWT.RADIO);
                        this.mntm_IconSize1[0] = mntm_IconSize;
                        mntm_IconSize.setText(I18n.E3D_IconSize1);
                        mntm_IconSize.setSelection(iconSize == 0);
                    }
                    {
                        MenuItem mntm_IconSize = new MenuItem(mnu_IconSize, SWT.RADIO);
                        this.mntm_IconSize2[0] = mntm_IconSize;
                        mntm_IconSize.setText(I18n.E3D_IconSize2);
                        mntm_IconSize.setSelection(iconSize == 1);
                    }
                    {
                        MenuItem mntm_IconSize = new MenuItem(mnu_IconSize, SWT.RADIO);
                        this.mntm_IconSize3[0] = mntm_IconSize;
                        mntm_IconSize.setText(I18n.E3D_IconSize3);
                        mntm_IconSize.setSelection(iconSize == 2);
                    }
                    {
                        MenuItem mntm_IconSize = new MenuItem(mnu_IconSize, SWT.RADIO);
                        this.mntm_IconSize4[0] = mntm_IconSize;
                        mntm_IconSize.setText(I18n.E3D_IconSize4);
                        mntm_IconSize.setSelection(iconSize == 3);
                    }
                    {
                        MenuItem mntm_IconSize = new MenuItem(mnu_IconSize, SWT.RADIO);
                        this.mntm_IconSize5[0] = mntm_IconSize;
                        mntm_IconSize.setText(I18n.E3D_IconSize5);
                        mntm_IconSize.setSelection(iconSize == 4);
                    }
                    {
                        MenuItem mntm_IconSize = new MenuItem(mnu_IconSize, SWT.RADIO);
                        this.mntm_IconSize6[0] = mntm_IconSize;
                        mntm_IconSize.setText(I18n.E3D_IconSize6);
                        mntm_IconSize.setSelection(iconSize == 5);
                    }
                    @SuppressWarnings("unused")
                    final MenuItem mntmSeparator2 = new MenuItem(mnu_IconSize, SWT.SEPARATOR);
                    {
                        MenuItem mntm_IconSize = new MenuItem(mnu_IconSize, SWT.PUSH);
                        mntm_IconSize.setText(I18n.E3D_RequiresRestart);
                        mntm_IconSize.setEnabled(false);
                    }
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator2 = new MenuItem(mnu_Tools, SWT.SEPARATOR);
                {
                    MenuItem mntm_UploadErrorLog = new MenuItem(mnu_Tools, SWT.PUSH);
                    this.mntm_UploadLogs[0] = mntm_UploadErrorLog;
                    mntm_UploadErrorLog.setText(I18n.E3D_UploadErrorLogs);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator3 = new MenuItem(mnu_Tools, SWT.SEPARATOR);
                {
                    MenuItem mntm_AntiAliasing = new MenuItem(mnu_Tools, SWT.CHECK);
                    mntm_AntiAliasing.setSelection(WorkbenchManager.getUserSettingState().isAntiAliasing());
                    this.mntm_AntiAliasing[0] = mntm_AntiAliasing;
                    mntm_AntiAliasing.setText(I18n.E3D_AntiAliasing);
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator4 = new MenuItem(mnu_Tools, SWT.SEPARATOR);
                //                {
                //                    MenuItem mntm_SyncWithTextEditor = new MenuItem(mnu_Tools, SWT.CHECK);
                //                    mntm_SyncWithTextEditor.setSelection(WorkbenchManager.getUserSettingState().getSyncWithTextEditor().get());
                //                    this.mntm_SyncWithTextEditor[0] = mntm_SyncWithTextEditor;
                //                    mntm_SyncWithTextEditor.setText(I18n.E3D_Sync3DEditor);
                //                }
                {
                    MenuItem mntm_SyncLpeInline = new MenuItem(mnu_Tools, SWT.CHECK);
                    mntm_SyncLpeInline.setSelection(WorkbenchManager.getUserSettingState().getSyncWithLpeInline().get());
                    // mntm_SyncLpeInline.setEnabled(WorkbenchManager.getUserSettingState().getSyncWithTextEditor().get());
                    this.mntm_SyncLpeInline[0] = mntm_SyncLpeInline;
                    mntm_SyncLpeInline.setText(I18n.E3D_ParseInline);
                }
            }
        }
        return toolItem_MiscClick;
    }

    private ToolItem createToolItemManipulatorActions(ToolItemDrawLocation location, ToolItemDrawMode mode, String label, ToolItem toolItem) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        if (mode == ToolItemDrawMode.DROP_DOWN) {
            if (toolItem == null) {
                toolItem = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);;
            }
            final Button btn_ManipulatorActions = new Button(toolItem, SWT.ARROW | SWT.DOWN);
            btn_ManipulatorActions.setToolTipText(I18n.E3D_ModifyManipulator);
            this.mnu_Manipulator = new Menu(this.getShell(), SWT.POP_UP);
            btn_ManipulatorActions.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Point loc = btn_ManipulatorActions.getLocation();
                    Rectangle rect = btn_ManipulatorActions.getBounds();
                    Point mLoc = new Point(loc.x - 1, loc.y + rect.height);
                    mnu_Manipulator.setLocation(getShell().getDisplay().map(btn_ManipulatorActions.getParent(), null, mLoc));
                    mnu_Manipulator.setVisible(true);
                    Editor3DWindow.getWindow().regainFocus();
                }
            });
            {
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_0_toOrigin[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToOrigin);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_toOrigin.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_XIII_toWorld[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToWorld);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_toWorld.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_XIV_adjustRotationCenter[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_AdjustRotationCenter);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_adjustrotationcenter.png")); //$NON-NLS-1$
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator1 = new MenuItem(mnu_Manipulator, SWT.SEPARATOR);
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_X_XReverse[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ReverseX);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_Xinv.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_XI_YReverse[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ReverseY);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_Yinv.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_XII_ZReverse[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ReverseZ);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_Zinv.png")); //$NON-NLS-1$
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator2 = new MenuItem(mnu_Manipulator, SWT.SEPARATOR);
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_SwitchXY[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_SwapXY);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_XswapY.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_SwitchXZ[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_SwapXZ);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_XswapZ.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_SwitchYZ[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_SwapYZ);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_YswapZ.png")); //$NON-NLS-1$
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator3 = new MenuItem(mnu_Manipulator, SWT.SEPARATOR);
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_1_cameraToPos[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_CamToManipulator);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_cameratomanipulator.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_2_toAverage[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToAvg);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_toavg.png")); //$NON-NLS-1$
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator31 = new MenuItem(mnu_Manipulator, SWT.SEPARATOR);
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_3_toSubfile[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToSubfile);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tosubfile.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_32_subfileTo[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_SubfileToManipulator);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tosubfile.png")); //$NON-NLS-1$
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator32 = new MenuItem(mnu_Manipulator, SWT.SEPARATOR);
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_4_toVertex[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToVertex);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestvertex.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_5_toEdge[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToEdge);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestedge.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_6_toSurface[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToFace);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestface.png")); //$NON-NLS-1$
                }
                @SuppressWarnings("unused")
                final MenuItem mntmSeparator4 = new MenuItem(mnu_Manipulator, SWT.SEPARATOR);
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_7_toVertexNormal[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToVertexN);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestvertexN.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_XV_toVertexPosition[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToVertexP);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestvertexN2.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_8_toEdgeNormal[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToEdgeN);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestedgeN.png")); //$NON-NLS-1$
                }
                {
                    MenuItem btn_Mani = new MenuItem(mnu_Manipulator, SWT.PUSH);
                    this.mntm_Manipulator_9_toSurfaceNormal[0] = btn_Mani;
                    btn_Mani.setText(I18n.E3D_ManipulatorToFaceN);
                    btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestfaceN.png")); //$NON-NLS-1$
                }
            }
            return null;
        } else {
            ToolItem toolItem_ManipulatorActions = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_0_toOrigin[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToOrigin);
                btn_Mani.setImage(ResourceManager.getImage("icon16_toOrigin.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_XIII_toWorld[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToWorld);
                btn_Mani.setImage(ResourceManager.getImage("icon16_toWorld.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_XIV_adjustRotationCenter[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_AdjustRotationCenter);
                btn_Mani.setImage(ResourceManager.getImage("icon16_adjustrotationcenter.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_X_XReverse[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ReverseX);
                btn_Mani.setImage(ResourceManager.getImage("icon16_Xinv.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_XI_YReverse[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ReverseY);
                btn_Mani.setImage(ResourceManager.getImage("icon16_Yinv.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_XII_ZReverse[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ReverseZ);
                btn_Mani.setImage(ResourceManager.getImage("icon16_Zinv.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_SwitchXY[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_SwapXY);
                btn_Mani.setImage(ResourceManager.getImage("icon16_XswapY.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_SwitchXZ[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_SwapXZ);
                btn_Mani.setImage(ResourceManager.getImage("icon16_XswapZ.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_SwitchYZ[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_SwapYZ);
                btn_Mani.setImage(ResourceManager.getImage("icon16_YswapZ.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_1_cameraToPos[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_CamToManipulator);
                btn_Mani.setImage(ResourceManager.getImage("icon16_cameratomanipulator.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_2_toAverage[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToAvg);
                btn_Mani.setImage(ResourceManager.getImage("icon16_toavg.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_3_toSubfile[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToSubfile);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tosubfile.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_32_subfileTo[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_SubfileToManipulator);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tosubfile2.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_4_toVertex[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToVertex);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestvertex.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_5_toEdge[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToEdge);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestedge.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_6_toSurface[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToFace);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestface.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_7_toVertexNormal[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToVertexN);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestvertexN.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_XV_toVertexPosition[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToVertexP);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestvertexN2.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_8_toEdgeNormal[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToEdgeN);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestedgeN.png")); //$NON-NLS-1$
            }
            {
                Button btn_Mani = new Button(toolItem_ManipulatorActions, SWT.NONE);
                this.btn_Manipulator_9_toSurfaceNormal[0] = btn_Mani;
                btn_Mani.setToolTipText(I18n.E3D_ManipulatorToFaceN);
                btn_Mani.setImage(ResourceManager.getImage("icon16_tonearestfaceN.png")); //$NON-NLS-1$
            }

            return null;
        }
    }

    private ToolItem createToolItemManipulatorMode(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_TransformationModes = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_Local = new Button(toolItem_TransformationModes, SWT.TOGGLE);
            this.btn_Local[0] = btn_Local;
            btn_Local.setToolTipText(I18n.E3D_Local);
            btn_Local.setSelection(true);
            btn_Local.setImage(ResourceManager.getImage("icon16_local.png")); //$NON-NLS-1$
        }
        {
            Button btn_Global = new Button(toolItem_TransformationModes, SWT.TOGGLE);
            this.btn_Global[0] = btn_Global;
            btn_Global.setToolTipText(I18n.E3D_Global);
            btn_Global.setImage(ResourceManager.getImage("icon16_global.png")); //$NON-NLS-1$
        }
        return toolItem_TransformationModes;
    }

    private ToolItem createToolItemTransformationMode(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_TransformationMode = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_Select = new Button(toolItem_TransformationMode, SWT.TOGGLE);
            this.btn_Select[0] = btn_Select;
            KeyStateManager.addTooltipText(btn_Select, I18n.E3D_Select, Task.MODE_SELECT);
            btn_Select.setSelection(true);
            btn_Select.setImage(ResourceManager.getImage("icon16_select.png")); //$NON-NLS-1$
        }
        {
            Button btn_Move = new Button(toolItem_TransformationMode, SWT.TOGGLE);
            this.btn_Move[0] = btn_Move;
            KeyStateManager.addTooltipText(btn_Move, I18n.E3D_Move, Task.MODE_MOVE);
            btn_Move.setImage(ResourceManager.getImage("icon16_move.png")); //$NON-NLS-1$
        }
        {
            Button btn_Rotate = new Button(toolItem_TransformationMode, SWT.TOGGLE);
            this.btn_Rotate[0] = btn_Rotate;
            KeyStateManager.addTooltipText(btn_Rotate, I18n.E3D_Rotate, Task.MODE_ROTATE);
            btn_Rotate.setImage(ResourceManager.getImage("icon16_rotate.png")); //$NON-NLS-1$
        }
        {
            Button btn_Scale = new Button(toolItem_TransformationMode, SWT.TOGGLE);
            this.btn_Scale[0] = btn_Scale;
            KeyStateManager.addTooltipText(btn_Scale, I18n.E3D_Scale, Task.MODE_SCALE);
            btn_Scale.setImage(ResourceManager.getImage("icon16_scale.png")); //$NON-NLS-1$
        }
        {
            Button btn_Combined = new Button(toolItem_TransformationMode, SWT.TOGGLE);
            this.btn_Combined[0] = btn_Combined;
            KeyStateManager.addTooltipText(btn_Combined, I18n.E3D_Combined, Task.MODE_COMBINED);
            btn_Combined.setImage(ResourceManager.getImage("icon16_combined.png")); //$NON-NLS-1$
        }
        return toolItem_TransformationMode;
    }

    private ToolItem createToolItemUndoRedo(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_UndoRedo = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_Undo = new Button(toolItem_UndoRedo, SWT.NONE);
            this.btn_Undo[0] = btn_Undo;
            btn_Undo.setImage(ResourceManager.getImage("icon16_undo.png")); //$NON-NLS-1$
            KeyStateManager.addTooltipText(btn_Undo, I18n.E3D_Undo, Task.UNDO);
        }
        if (NLogger.DEBUG) {
            Button btn_Snapshot = new Button(toolItem_UndoRedo, SWT.NONE);
            this.btn_AddHistory[0] = btn_Snapshot;
            btn_Snapshot.setImage(ResourceManager.getImage("icon16_snapshot.png")); //$NON-NLS-1$
            btn_Snapshot.setToolTipText(I18n.E3D_Snapshot);
        }
        {
            Button btn_Redo = new Button(toolItem_UndoRedo, SWT.NONE);
            this.btn_Redo[0] = btn_Redo;
            btn_Redo.setImage(ResourceManager.getImage("icon16_redo.png")); //$NON-NLS-1$
            KeyStateManager.addTooltipText(btn_Redo, I18n.E3D_Redo, Task.REDO);
        }
        return toolItem_UndoRedo;
    }

    private ToolItem createToolItemMiscToggle(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_MiscToggle = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_AdjacentMove = new Button(toolItem_MiscToggle, SWT.TOGGLE);
            this.btn_MoveAdjacentData[0] = btn_AdjacentMove;
            btn_AdjacentMove.setToolTipText(I18n.E3D_MoveAdjacentData);
            btn_AdjacentMove.setImage(ResourceManager.getImage("icon16_adjacentmove.png")); //$NON-NLS-1$
            btn_AdjacentMove.setSelection(false);
        }
        {
            Button btn_TransSelection = new Button(toolItem_MiscToggle, SWT.TOGGLE);
            this.btn_NoTransparentSelection[0] = btn_TransSelection;
            btn_TransSelection.setToolTipText(I18n.E3D_ToggleTransparent);
            btn_TransSelection.setImage(ResourceManager.getImage("icon16_notrans.png")); //$NON-NLS-1$
        }
        {
            Button btn_BFCToggle = new Button(toolItem_MiscToggle, SWT.TOGGLE);
            this.btn_BFCToggle[0] = btn_BFCToggle;
            btn_BFCToggle.setToolTipText(I18n.E3D_ToggleBFC);
            btn_BFCToggle.setImage(ResourceManager.getImage("icon16_bfc.png")); //$NON-NLS-1$
        }
        return toolItem_MiscToggle;
    }

    private ToolItem createToolItemHideUnhide(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_HideUnhide = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_Hide = new Button(toolItem_HideUnhide, SWT.NONE);
            this.btn_Hide[0] = btn_Hide;
            btn_Hide.setToolTipText(I18n.E3D_Hide);
            btn_Hide.setImage(ResourceManager.getImage("icon16_hide.png")); //$NON-NLS-1$
        }
        {
            Button btn_Unhide = new Button(toolItem_HideUnhide, SWT.NONE);
            this.btn_ShowAll[0] = btn_Unhide;
            btn_Unhide.setToolTipText(I18n.E3D_ShowAll);
            btn_Unhide.setImage(ResourceManager.getImage("icon16_unhide.png")); //$NON-NLS-1$
        }
        return toolItem_HideUnhide;
    }

    private ToolItem createToolItemNewOpenDat(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_NewOpenDAT = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_NewDat = new Button(toolItem_NewOpenDAT, SWT.NONE);
            this.btn_NewDat[0] = btn_NewDat;
            btn_NewDat.setToolTipText(I18n.E3D_NewDat);
            btn_NewDat.setImage(ResourceManager.getImage("icon16_document-newdat.png")); //$NON-NLS-1$
        }
        {
            Button btn_OpenDAT = new Button(toolItem_NewOpenDAT, SWT.NONE);
            this.btn_OpenDat[0] = btn_OpenDAT;
            btn_OpenDAT.setToolTipText(I18n.E3D_OpenDat);
            btn_OpenDAT.setImage(ResourceManager.getImage("icon16_document-opendat.png")); //$NON-NLS-1$
        }
        {
            Button btn_Snapshot = new Button(toolItem_NewOpenDAT, SWT.NONE);
            this.btn_LastOpen[0] = btn_Snapshot;
            btn_Snapshot.setToolTipText(I18n.E3D_LastOpened);
            btn_Snapshot.setImage(ResourceManager.getImage("icon16_snapshot.png")); //$NON-NLS-1$
        }
        {
            Button btn_SaveDAT = new Button(toolItem_NewOpenDAT, SWT.NONE);
            this.btn_SaveDat[0] = btn_SaveDAT;
            KeyStateManager.addTooltipText(btn_SaveDAT, I18n.E3D_Save, Task.SAVE);
            btn_SaveDAT.setImage(ResourceManager.getImage("icon16_document-savedat.png")); //$NON-NLS-1$
        }
        {
            Button btn_SaveAsDAT = new Button(toolItem_NewOpenDAT, SWT.NONE);
            this.btn_SaveAsDat[0] = btn_SaveAsDAT;
            btn_SaveAsDAT.setToolTipText(I18n.E3D_SaveAs);
            btn_SaveAsDAT.setImage(ResourceManager.getImage("icon16_document-savedat.png")); //$NON-NLS-1$
            btn_SaveAsDAT.setText("..."); //$NON-NLS-1$
        }
        return toolItem_NewOpenDAT;
    }

    private ToolItem createToolItemNewOpenSave(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_NewOpenSave = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_New = new Button(toolItem_NewOpenSave, SWT.NONE);
            this.btn_New[0] = btn_New;
            btn_New.setToolTipText(I18n.E3D_New);
            btn_New.setImage(ResourceManager.getImage("icon16_document-new.png")); //$NON-NLS-1$
        }
        {
            Button btn_Open = new Button(toolItem_NewOpenSave, SWT.NONE);
            this.btn_Open[0] = btn_Open;
            btn_Open.setToolTipText(I18n.E3D_Open);
            btn_Open.setImage(ResourceManager.getImage("icon16_document-open.png")); //$NON-NLS-1$
        }
        {
            Button btn_Save = new Button(toolItem_NewOpenSave, SWT.NONE);
            this.btn_Save[0] = btn_Save;
            KeyStateManager.addTooltipText(btn_Save, I18n.E3D_Save, Task.SAVE);
            btn_Save.setImage(ResourceManager.getImage("icon16_document-save.png")); //$NON-NLS-1$
        }
        {
            Button btn_SaveAll = new Button(toolItem_NewOpenSave, SWT.NONE);
            this.btn_SaveAll[0] = btn_SaveAll;
            btn_SaveAll.setToolTipText(I18n.E3D_SaveAll);
            btn_SaveAll.setImage(ResourceManager.getImage("icon16_document-saveall.png")); //$NON-NLS-1$
        }
        return toolItem_NewOpenSave;
    }

    private ToolItem createToolItemSync(ToolItemDrawLocation location, ToolItemDrawMode mode, String label) {
        final Composite target;
        switch (location) {
        case NORTH:
        default:
            target = cmpNorth;
            break;
        case EAST:
            target = cmpEast;
            break;
        case WEST:
            target = cmpWest;
            break;
        }
        ToolItem toolItem_Sync = new ToolItem(target, SWT.NONE, mode == ToolItemDrawMode.HORIZONTAL);
        {
            Button btn_Sync = new Button(toolItem_Sync, SWT.NONE);
            this.btn_Sync[0] = btn_Sync;
            btn_Sync.setToolTipText(I18n.E3D_SyncFolders);
            btn_Sync.setImage(ResourceManager.getImage("icon16_sync.png")); //$NON-NLS-1$
        }
        return toolItem_Sync;
    }

    private void createComposite3D(SashForm sashForm, CompositeContainer c, Composite3DState state) {
        // Load the configuration of one 3D window
        final Composite3D c3d;
        if (c == null) {
            final CompositeContainer cmp_Container = new CompositeContainer(sashForm, state.hasScales());
            c3d = cmp_Container.getComposite3D();
        } else {
            state.setScales(false);
            c3d = c.getComposite3D();
        }
        c3d.loadState(state);
    }

    private void addColorButton(ToolItem toolItem_Colours, GColour gColour, final int index) {
        int cn = gColour.getColourNumber();
        if (cn != -1 && View.hasLDConfigColour(cn)) {
            gColour = View.getLDConfigColour(cn);
        }

        final int imgSize;
        switch (Editor3DWindow.getIconsize()) {
        case 0:
            imgSize = 16;
            break;
        case 1:
            imgSize = 24;
            break;
        case 2:
            imgSize = 32;
            break;
        case 3:
            imgSize = 48;
            break;
        case 4:
            imgSize = 64;
            break;
        case 5:
            imgSize = 72;
            break;
        default:
            imgSize = 16;
            break;
        }

        final GColour[] gColour2 = new GColour[] { gColour };
        final Color[] col = new Color[1];
        col[0] = SWTResourceManager.getColor((int) (gColour2[0].getR() * 255f), (int) (gColour2[0].getG() * 255f), (int) (gColour2[0].getB() * 255f));

        final Button btn_Col = new Button(toolItem_Colours, SWT.NONE);
        btn_Col.setData(gColour2);
        int num = gColour2[0].getColourNumber();
        if (!View.hasLDConfigColour(num)) {
            num = -1;
        }
        if (num != -1) {

            Object[] messageArguments = {num, View.getLDConfigColourName(num)};
            MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
            formatter.setLocale(MyLanguage.LOCALE);
            formatter.applyPattern(I18n.EDITORTEXT_Colour1 + I18n.E3D_ControlClickModify);

            btn_Col.setToolTipText(formatter.format(messageArguments));
        } else {
            StringBuilder colourBuilder = new StringBuilder();
            colourBuilder.append("0x2"); //$NON-NLS-1$
            colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getR())).toUpperCase());
            colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getG())).toUpperCase());
            colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getB())).toUpperCase());

            Object[] messageArguments = {colourBuilder.toString()};
            MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
            formatter.setLocale(MyLanguage.LOCALE);
            formatter.applyPattern(I18n.EDITORTEXT_Colour2 + I18n.E3D_ControlClickModify);

            btn_Col.setToolTipText(formatter.format(messageArguments));
        }

        btn_Col.setImage(ResourceManager.getImage("icon16_fullTransparent.png")); //$NON-NLS-1$

        btn_Col.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if ((e.stateMask & SWT.CTRL) == SWT.CTRL) {
                    // Choose new colour
                    new ColourDialog(getShell(), gColour2, false).open();
                    WorkbenchManager.getUserSettingState().getUserPalette().set(index, gColour2[0]);
                    col[0] = SWTResourceManager.getColor((int) (gColour2[0].getR() * 255f), (int) (gColour2[0].getG() * 255f), (int) (gColour2[0].getB() * 255f));
                    int num = gColour2[0].getColourNumber();
                    if (View.hasLDConfigColour(num)) {
                        gColour2[0] = View.getLDConfigColour(num);
                    } else {
                        num = -1;
                    }
                    if (num != -1) {

                        Object[] messageArguments = {num, View.getLDConfigColourName(num)};
                        MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
                        formatter.setLocale(MyLanguage.LOCALE);
                        formatter.applyPattern(I18n.EDITORTEXT_Colour1 + I18n.E3D_ControlClickModify);

                        btn_Col.setToolTipText(formatter.format(messageArguments));
                    } else {
                        StringBuilder colourBuilder = new StringBuilder();
                        colourBuilder.append("0x2"); //$NON-NLS-1$
                        colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getR())).toUpperCase());
                        colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getG())).toUpperCase());
                        colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getB())).toUpperCase());

                        Object[] messageArguments = {colourBuilder.toString()};
                        MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
                        formatter.setLocale(MyLanguage.LOCALE);
                        formatter.applyPattern(I18n.EDITORTEXT_Colour2 + I18n.E3D_ControlClickModify);

                        btn_Col.setToolTipText(formatter.format(messageArguments));
                    }
                    reloadAllColours();
                } else {
                    int num = gColour2[0].getColourNumber();
                    if (View.hasLDConfigColour(num)) {
                        gColour2[0] = View.getLDConfigColour(num);
                    } else {
                        num = -1;
                    }
                    if (Project.getFileToEdit() != null) {
                        Project.getFileToEdit().getVertexManager().addSnapshot();
                        Project.getFileToEdit().getVertexManager().colourChangeSelection(num, gColour2[0].getR(), gColour2[0].getG(), gColour2[0].getB(), gColour2[0].getA(), true);
                    }
                    Editor3DWindow.getWindow().setLastUsedColour(gColour2[0]);
                    btn_LastUsedColour[0].removeListener(SWT.Paint, btn_LastUsedColour[0].getListeners(SWT.Paint)[0]);
                    btn_LastUsedColour[0].removeListener(SWT.Selection, btn_LastUsedColour[0].getListeners(SWT.Selection)[0]);
                    final Color col = SWTResourceManager.getColor((int) (gColour2[0].getR() * 255f), (int) (gColour2[0].getG() * 255f), (int) (gColour2[0].getB() * 255f));
                    final Point size = btn_LastUsedColour[0].computeSize(SWT.DEFAULT, SWT.DEFAULT);
                    final int x = Math.round(size.x / 5f);
                    final int y = Math.round(size.y / 5f);
                    final int w = Math.round(size.x * (3f / 5f));
                    final int h = Math.round(size.y * (3f / 5f));
                    btn_LastUsedColour[0].addPaintListener(new PaintListener() {
                        @Override
                        public void paintControl(PaintEvent e) {
                            e.gc.setBackground(col);
                            e.gc.fillRectangle(x, y, w, h);
                            if (gColour2[0].getA() == 1f) {
                                e.gc.drawImage(ResourceManager.getImage("icon16_transparent.png"), 0, 0, imgSize, imgSize, x, y, w, h); //$NON-NLS-1$
                            } else {
                                e.gc.drawImage(ResourceManager.getImage("icon16_halftrans.png"), 0, 0, imgSize, imgSize, x, y, w, h); //$NON-NLS-1$
                            }
                        }
                    });
                    btn_LastUsedColour[0].addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            if (Project.getFileToEdit() != null) {
                                Editor3DWindow.getWindow().setLastUsedColour(gColour2[0]);
                                int num = gColour2[0].getColourNumber();
                                if (View.hasLDConfigColour(num)) {
                                    gColour2[0] = View.getLDConfigColour(num);
                                } else {
                                    num = -1;
                                }
                                Project.getFileToEdit().getVertexManager().addSnapshot();
                                Project.getFileToEdit().getVertexManager().colourChangeSelection(num, gColour2[0].getR(), gColour2[0].getG(), gColour2[0].getB(), gColour2[0].getA(), true);
                            }
                            Editor3DWindow.getWindow().regainFocus();
                        }
                    });
                    if (num != -1) {
                        Object[] messageArguments = {num, View.getLDConfigColourName(num)};
                        MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
                        formatter.setLocale(MyLanguage.LOCALE);
                        formatter.applyPattern(I18n.EDITORTEXT_Colour1);

                        btn_LastUsedColour[0].setToolTipText(formatter.format(messageArguments));
                    } else {
                        StringBuilder colourBuilder = new StringBuilder();
                        colourBuilder.append("0x2"); //$NON-NLS-1$
                        colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getR())).toUpperCase());
                        colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getG())).toUpperCase());
                        colourBuilder.append(MathHelper.toHex((int) (255f * gColour2[0].getB())).toUpperCase());

                        Object[] messageArguments = {colourBuilder.toString()};
                        MessageFormat formatter = new MessageFormat(""); //$NON-NLS-1$
                        formatter.setLocale(MyLanguage.LOCALE);
                        formatter.applyPattern(I18n.EDITORTEXT_Colour2);

                        btn_LastUsedColour[0].setToolTipText(formatter.format(messageArguments));
                    }
                    btn_LastUsedColour[0].redraw();
                }
                Editor3DWindow.getWindow().regainFocus();
            }
        });
        final Point size = btn_Col.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final int x = Math.round(size.x / 5f);
        final int y = Math.round(size.y / 5f);
        final int w = Math.round(size.x * (3f / 5f));
        final int h = Math.round(size.y * (3f / 5f));
        btn_Col.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                e.gc.setBackground(col[0]);
                e.gc.fillRectangle(x, y, w, h);
                if (gColour2[0].getA() == 1f) {
                    e.gc.drawImage(ResourceManager.getImage("icon16_transparent.png"), 0, 0, imgSize, imgSize, x, y, w, h); //$NON-NLS-1$
                } else {
                    e.gc.drawImage(ResourceManager.getImage("icon16_halftrans.png"), 0, 0, imgSize, imgSize, x, y, w, h); //$NON-NLS-1$
                }
            }
        });
    }

    /**
     * Create the status line manager.
     *
     * @return the status line manager
     */
    @Override
    protected StatusLineManager createStatusLineManager() {
        StatusLineManager status = new StatusLineManager();
        return status;
    }

    /**
     * Configure the shell.
     *
     * @param newShell
     */
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
    }

    /**
     * Return the initial size of the window.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(916, 578);
    }

    public static Label getStatusLabel() {
        return (Label) Editor3DDesign.status.getChildren()[0];
    }

    public static SashForm getSashForm() {
        return sashForm;
    }

    public static void setSashForm(SashForm sashForm) {
        Editor3DDesign.sashForm = sashForm;
    }

    void showSelectMenu() {
        Point loc = btn_Select2[0].getLocation();
        Rectangle rect = btn_Select2[0].getBounds();
        Point mLoc = new Point(loc.x - 1, loc.y + rect.height);
        mnu_Select.setLocation(getShell().getDisplay().map(btn_Select2[0].getParent(), null, mLoc));
        mnu_Select.setVisible(true);
    }

    public static int getIconsize() {
        return iconSize;
    }

    void reloadColours() {
        for (Control ctrl : toolItem_ColourBar.getChildren()) {
            if (!(ctrl instanceof ToolSeparator)) ctrl.dispose();
        }

        List<GColour> colours = WorkbenchManager.getUserSettingState().getUserPalette();

        final int size = colours.size();
        for (int i = 0; i < size; i++) {
            addColorButton(toolItem_ColourBar, colours.get(i), i);
        }

        {
            Button btn_Palette = new Button(toolItem_ColourBar, SWT.NONE);
            this.btn_Palette[0] = btn_Palette;
            btn_Palette.setToolTipText(I18n.E3D_More);
            btn_Palette.setImage(ResourceManager.getImage("icon16_colours.png")); //$NON-NLS-1$
        }

        toolItem_ColourBar.getParent().layout();
        toolItem_ColourBar.layout();
        toolItem_ColourBar.redraw();
    }

    public static void reloadAllColours() {
        for (EditorTextWindow w : Project.getOpenTextWindows()) {
            w.reloadColours();
        }
        Editor3DWindow.getWindow().reloadColours();
    }
}