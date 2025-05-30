/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.table.view;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.generic.GenericRectangle;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.chart.AbstractChart;
import com.actelion.research.table.view.chart.AbstractDistributionPlot;
import com.actelion.research.table.view.chart.ChartType;
import com.actelion.research.table.view.graph.RadialVisualizationNode;
import com.actelion.research.table.view.graph.TreeVisualizationNode;
import com.actelion.research.table.view.graph.VisualizationNode;
import com.actelion.research.util.ByteArrayComparator;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.DoubleFormat;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.*;

import static com.actelion.research.chem.io.CompoundTableConstants.*;

public abstract class JVisualization extends JComponent
		implements CompoundTableListener,HighlightListener,MarkerLabelDisplayer,ListSelectionListener,MouseListener,
			MouseMotionListener,Printable,CompoundTableListListener,VisualizationColorListener {
	private static final long serialVersionUID = 0x20100610;

	// taken from class com.actelion.research.gui.form.FormObjectBorder
	public static final Color DEFAULT_TITLE_BACKGROUND = new Color(224, 224, 255);
	public static final Color DEFAULT_LABEL_BACKGROUND = new Color(224, 224, 224);
	public static final float DEFAULT_LABEL_TRANSPARENCY = 0.25f;
	protected static final GenericRectangle DEPICTOR_RECT = new GenericRectangle(0, 0, 32000, 32000);

	// For a view width of 460 pixels (no hi-dpi scaling) we assign a default font size of 8 points
	// multiplied by the actor taken from the font size slider (mReativeFontSize).
	// In case of absolute font size mode, the font size doesn't change when the view is resized.
	// In case of relative font size mode, the font scaled proportionally.
	// In case of adaptive font size mode, the font scaled sub-proportionally.
	protected static final int cAbsoluteDefaultFontSize = 8; // in case of absolute font sizes, this is the reference
	private static final float cFontRefenceViewSize = 420f;  // View size that independent of font size mode should have ABSOLUTE_FONT_SIZE * mReativeFontSize
	private static final float cFontHeightFactor = cAbsoluteDefaultFontSize / cFontRefenceViewSize;

	public static final String[] FONT_SIZE_MODE_TEXT = { "Relative font size:", "Adaptive font size:", "Absolute font size:" };
	public static final String[] FONT_SIZE_MODE_CODE = { "relative", "adaptive", "absolute" };
	public static final int cFontSizeModeRelative = 0;
	public static final int cFontSizeModeAdaptive = 1;
	public static final int cFontSizeModeAbsolute = 2;

	public static final int cColumnUnassigned = -1;
	public static final int cConnectionColumnConnectAll = -2;
	public static final int cConnectionColumnMeanAndMedian = -3;
	public static final int cConnectionColumnConnectCases = -4;

	public static final String[] SCALE_MODE_CODE = { "false", "true", "y", "x" };
	public static final int cScaleModeShown = 0;
	public static final int cScaleModeHidden = 1;
	public static final int cScaleModeHideY = 2;
	public static final int cScaleModeHideX = 3;

	public static final String[] SCALE_STYLE_CODE = { "arrows", "frame" };
	public static final int cScaleStyleArrows = 0;
	public static final int cScaleStyleFrame = 1;

	public static final String[] GRID_MODE_CODE = { "false", "true", "vertical", "horizontal" };
	public static final int cGridModeShown = 0;
	public static final int cGridModeHidden = 1;
	public static final int cGridModeShowVertical = 2;
	public static final int cGridModeShowHorizontal = 3;

	public static final String[] cConnectionListModeText = { "Don't suppress", "Only show", "Suppress" };
	public static final String[] cConnectionListModeCode = { "none", "show", "hide" };
	public static final int cConnectionListModeNone = 0;
	public static final int cConnectionListModeShow = 1;
	public static final int cConnectionListModeHide = 2;

	public static final int cTreeViewModeNone = 0;
	public static final int cTreeViewModeTopRoot = 1;
	public static final int cTreeViewModeBottomRoot = 2;
	public static final int cTreeViewModeLeftRoot = 3;
	public static final int cTreeViewModeRightRoot = 4;
	public static final int cTreeViewModeRadial = 5;

	public static final String[] TREE_VIEW_MODE_NAME = { "<none>", "Layered Graph with root at top", "Layered Graph with root at bottom", "Layered Graph with root at left side", "Layered Graph with root at right side", "Radial Graph with root in center" };
	public static final String[] TREE_VIEW_MODE_CODE = { "none", "hTree", "hTree2", "vTree", "vTree2", "radial" };

	public static final int cMaxChartCategoryCount = 10000;			// this is for one axis
	public static final int cMaxTotalChartCategoryCount = 100000;	// this is the product of all axis

	public static final int cMaxCaseSeparationCategoryCount = 128;	// this is for one axis
	public static final int cMaxSplitViewCount = 10000;

	private static final float MAX_SCATTERPLOT_MARGIN = 0.05f;

	private static final float MARKER_ZOOM_ADAPTION_FACTOR = 0.75f;   // value < 1f to keep marker zoom adaption sub-proportional
	private static final float LABEL_ZOOM_ADAPTION_FACTOR = 0.50f;    // label zoom adaption is smaller than marker zoom adaption

	protected static final String[] CHART_MODE_AXIS_TEXT = ChartType.MODE_CODE;

	public static final String[] BOXPLOT_MEAN_MODE_TEXT = { "No Indicator", "Median Line", "Mean Line", "Mean & Median Lines", "Mean & Median Triangles" };
	public static final String[] BOXPLOT_MEAN_MODE_CODE = { "none", "median", "mean", "both", "triangles" };
	public static final int BOXPLOT_DEFAULT_MEAN_MODE = 3;

/*	protected static final int AXIS_TYPE_UNASSIGNED = 0;
	protected static final int AXIS_TYPE_TEXT_CATEGORY = 1;
	protected static final int AXIS_TYPE_DOUBLE_CATEGORY = 2;
	protected static final int AXIS_TYPE_DOUBLE_VALUE = 3;
*/
	public static final float SCALE_LIGHT = 0.4f;
	public static final float SCALE_MEDIUM = 0.7f;
	public static final float SCALE_STRONG = 1.0f;

	protected static final boolean USE_FULL_RANGE_CATEGORY_SCALES = false;

	private static final int DRAG_MODE_NONE = 0;
	private static final int DRAG_MODE_LASSO_SELECT = 1;
	private static final int DRAG_MODE_RECT_SELECT = 2;
	private static final int DRAG_MODE_TRANSLATE = 3;
	private static final int DRAG_MODE_MOVE_LABEL = 4;

	private static final byte EXCLUSION_FLAG_ZOOM_0 = 0x01;
	private static final byte EXCLUSION_FLAG_NAN_0 = 0x08;
	private static final byte EXCLUSION_FLAG_DETAIL_GRAPH = 0x40;
	private static final byte EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN = (byte)0x80;	// used if a column's NAN values affect visibility, but column not assigned to axis
	private static final byte EXCLUSION_FLAGS_NAN = (byte)(7 * EXCLUSION_FLAG_NAN_0) | EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN;

	private static final long RIGHT_MOUSE_POPUP_DELAY = 800;
	private static final int MAX_TOOLTIP_LENGTH = 24;

	protected CompoundTableModel	mTableModel;
	protected double[]				mAxisVisMin,mAxisVisMax,mPruningBarLow,mPruningBarHigh;
	protected float[][]				mAxisSimilarity;
	protected int[]					mAxisIndex,mLabelColumn,mSplittingColumn;
	protected boolean[]				mIsCategoryAxis;
	protected ChartType             mChartType;
	protected AbstractChart mChartInfo;
	protected VisualizationPoint[]	mPoint;
	protected VisualizationPoint 	mHighlightedPoint,mActivePoint;
	protected LabelPosition2D       mHighlightedLabelPosition;
	protected VisualizationColor	mMarkerColor,mLabelBackgroundColor;
	protected VisualizationSplitter	mSplitter;
	protected VisualizationPoint[]  mConnectionLinePoint;
	protected TreeMap<byte[],VisualizationPoint>mConnectionLineMap;
	private TreeMap<VisualizationPoint,LineConnection[]> mReverseConnectionMap;
	protected VisualizationNode[][]	mTreeNodeList;
	protected ArrayList<VisualizationLegend> mLegendList;
	protected float					mAbsoluteMarkerSize,mRelativeMarkerSize,mMarkerLabelSize,mMarkerJittering,mRelativeFontSize,
									mAbsoluteConnectionLineWidth,mRelativeConnectionLineWidth,mCaseSeparationValue,mMarkerSizeZoomAdaption,
									mSplittingAspectRatio,mLabelBackgroundTransparency,mScatterPlotMargin,mZoomState,
									mMarkerSizeMin,mMarkerSizeMax;
	protected volatile boolean		mOffImageValid;
	protected boolean               mCoordinatesValid,mShowNaNValues,mLabelsInTreeViewOnly,mMouseIsDown,
									mIsMarkerLabelsBlackAndWhite,mIsConnectionLineInverted,mShowLabelBackground,mShowEmptyInSplitView,
									mIsFastRendering,mOptimizeLabelPositions,mSuppressLegend,mTreeViewShowAll;
	private boolean                 mShowStandardDeviation,mShowConfidenceInterval,mShowValueCount,mShowBarOrPieSizeValues,
									mShowMeanAndMedianValues;
	protected boolean[]				mAxisVisRangeIsLogarithmic;
	protected final int             mDimensions;
	protected int					mDataPoints,mMarkerSizeColumn,mMarkerShapeColumn,mFontHeight,mBoxplotMeanMode,mLabelList,
									mMouseX1,mMouseY1,mMouseX2,mMouseY2,mConnectionColumn,mConnectionOrderColumn,
									mPValueColumn,mTreeViewRadius,mPreferredChartType,
									mFocusList,mCaseSeparationColumn,mCaseSeparationCategoryCount,mScaleMode,mScaleStyle,
									mUseAsFilterFlagNo,mTreeViewMode,mActiveExclusionFlags,mHVCount,mHVExclusionTag,mFontSizeMode,
									mVisibleCategoryExclusionTag,mMarkerJitteringAxes,mGridMode, mLocalExclusionList,
									mConnectionLineListMode,mConnectionLineList1,mConnectionLineList2,mPopupX,mPopupY,
									mOnePerCategoryLabelCategoryColumn;
	private int						mOnePerCategoryLabelValueColumn,mOnePerCategoryLabelMode;
	protected String				mPValueRefCategory,mWarningMessage;
	protected Random				mRandom;
	protected StereoMolecule		mLabelMolecule;
	protected int[][]				mVisibleCategoryFromCategory;

	private Thread					mPopupThread;
	private final CompoundListSelectionModel mSelectionModel;
	private int						mDragMode,mLocalExclusionFlagNo,mPreviousLocalExclusionFlagNo;
	private float[]					mSimilarityMarkerSize;
	private int[]					mCombinedCategoryCount,mFullCombinedCategoryCount;
	private Color					mViewBackground,mTitleBackground;
	private volatile boolean		mApplyLocalExclusionScheduled;
	private boolean				    mMouseIsControlDown,mTouchFunctionActive,mLocalAffectsGlobalExclusion,
									mAddingToSelection,mMarkerSizeInversion,mSuspendGlobalExclusion,
									mBoxplotShowPValue,mBoxplotShowFoldChange,mSplitViewCountExceeded,
									mTreeViewIsDynamic,mTreeViewIsInverted,mMarkerSizeProportional,
									mIsGloballyHidingRows,mIsIgnoreGlobalExclusion,mIsDynamicScale;
	private Polygon			 		mLassoRegion;
	private DetailPopupProvider		mDetailPopupProvider;
	private ViewSelectionHelper		mViewSelectionHelper;

	public JVisualization(CompoundTableModel tableModel,
						  CompoundListSelectionModel selectionModel,
						  int dimensions) {
		mTableModel = tableModel;
		mSelectionModel = selectionModel;
		mDimensions = dimensions;
		mChartType = new ChartType(dimensions);
		mPreferredChartType = ChartType.cTypeScatterPlot;

		addMouseListener(this);
		addMouseMotionListener(this);

		tableModel.addHighlightListener(this);
		selectionModel.addListSelectionListener(this);

		mPruningBarLow = new double[mDimensions];
		mPruningBarHigh = new double[mDimensions];

		mAxisVisMin = new double[mDimensions];
		mAxisVisMax = new double[mDimensions];
		mAxisVisRangeIsLogarithmic = new boolean[mDimensions];
		mAxisIndex = new int[mDimensions];
		mIsCategoryAxis = new boolean[mDimensions];
		mSplittingColumn = new int[mDimensions];
		mPreviousLocalExclusionFlagNo = -1;
		mLocalExclusionFlagNo = -1;
		mLocalAffectsGlobalExclusion = true;	// default
		mSuspendGlobalExclusion = true;
		mZoomState = 1.0f;
		mMarkerSizeZoomAdaption = 1.0f;
		mIsDynamicScale = true; // default

		mMarkerColor = new VisualizationColor(mTableModel, this);
		mLabelBackgroundColor = new VisualizationColor(mTableModel, this);

		mLabelColumn = new int[MarkerLabelDisplayer.cPositionCode.length];

		mAxisSimilarity = new float[mDimensions][];

		mRandom = new Random();
		setToolTipText("");	// to switch on tool-tips
		}

	protected void initialize() {
		mFontSizeMode = cFontSizeModeRelative;
		mRelativeFontSize = 1.0f;
		mRelativeMarkerSize = 1.0f;
		mMarkerSizeInversion = false;
		mMarkerSizeProportional = false;
		mMarkerSizeColumn = cColumnUnassigned;
		mMarkerShapeColumn = cColumnUnassigned;
		mMarkerJittering = 0.0f;
		mMarkerJitteringAxes = (mDimensions == 3) ? 7 : 3;
		mConnectionColumn = cColumnUnassigned;
		mConnectionOrderColumn = cColumnUnassigned;
		mRelativeConnectionLineWidth = 1.0f;
		mSplittingColumn[0] = cColumnUnassigned;
		mSplittingColumn[1] = cColumnUnassigned;
		mSplittingAspectRatio = 1.0f;
		mCaseSeparationColumn = cColumnUnassigned;
		mCaseSeparationValue = 0.5f;
		mOnePerCategoryLabelCategoryColumn = cColumnUnassigned;
		mPValueColumn = cColumnUnassigned;
		mBoxplotMeanMode = BOXPLOT_DEFAULT_MEAN_MODE;
		mSplitter = null;
		mShowEmptyInSplitView = true;
		mVisibleCategoryExclusionTag = -1;
		mHVExclusionTag = -1;
		mHighlightedPoint = null;
		mActivePoint = null;
		mCoordinatesValid = false;
		mLegendList = new ArrayList<>();
		mSuppressLegend = false;
		mScaleMode = cScaleModeShown;
		mGridMode = cGridModeShown;
		mFocusList = FocusableView.cFocusNone;
		mLabelList = cLabelsOnAllRows;
		mShowLabelBackground = false;
		mLabelsInTreeViewOnly = false;
		for (int i=0; i<MarkerLabelDisplayer.cPositionCode.length; i++)
			mLabelColumn[i] = cColumnUnassigned;

		mMarkerLabelSize = 1.0f;
		mLabelBackgroundTransparency = DEFAULT_LABEL_TRANSPARENCY;
		for (int axis=0; axis<mDimensions; axis++) {
			mAxisIndex[axis] = cColumnUnassigned;
			initializeAxis(axis);
			}
		mShowNaNValues = true;
		mTitleBackground = DEFAULT_TITLE_BACKGROUND;
		mLabelBackgroundColor.setDefaultDataColor(DEFAULT_LABEL_BACKGROUND);
		mTreeViewMode = cTreeViewModeNone;
		mTreeViewRadius = 5;
		mTreeViewShowAll = true;
		mUseAsFilterFlagNo = -1;
		mLocalExclusionList = -1;
		mScatterPlotMargin = getDefaultScatterplotMargin();

		mConnectionLineListMode = 0;
		mConnectionLineList1 = -1;
		mConnectionLineList2 = -1;

		determineChartType();
		}

	protected void setViewSelectionHelper(ViewSelectionHelper l) {
		mViewSelectionHelper = l;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	protected boolean copyViewContent() {
		// don't do anything currently
		return false;
		}

	public String getAxisTitle(int column) {
		return mTableModel.getColumnTitleExtended(column);
		}

	public void cleanup() {
		mTableModel.removeHighlightListener(this);
		mSelectionModel.removeListSelectionListener(this);
		if (mLocalExclusionFlagNo != -1)
			mTableModel.freeRowFlag(mLocalExclusionFlagNo);
		mLocalExclusionFlagNo = -1;
		}

	public void pixelScalingUpdated(float pixelScaling) {
	}

	public int getDimensionCount() {
		return mDimensions;
		}

	/**
	 * Build up the VisualizationPoint array mPoint of all rows.
	 * Datapoint initialization needs to be done when a new view is created
	 * or when rows are removed or added.
	 * @param recycleExisting true if some rows stay the same
	 * @param afterDeletion true if rows where deleted
	 */
	public void initializeDataPoints(boolean recycleExisting, boolean afterDeletion) {
		mDataPoints = mTableModel.getTotalRowCount();

		VisualizationPoint[] existing = mPoint;
		mPoint = new VisualizationPoint[mDataPoints];

		if (!recycleExisting) {
			for (int i=0; i<mDataPoints; i++) {
				CompoundRecord record = mTableModel.getTotalRecord(i);
				mPoint[record.getID()] = createVisualizationPoint(record);
				}
			}
		else if (afterDeletion) {
			int index = 0;
			for (int i=0; i<existing.length; i++)
				if (existing[i].record.getID() != -1)
					mPoint[index++] = existing[i];
			}
		else {
			for (int i=0; i<existing.length; i++)
				mPoint[i] = existing[i];
			for (int i=existing.length; i<mDataPoints; i++)
				mPoint[i] = createVisualizationPoint(mTableModel.getTotalRecord(i));
			}

		updateActiveRow();
		}

	public VisualizationPoint[] getDataPoints() {
		return mPoint;
	}

	protected Point getLabelConnectionPoint(float px, float py, int lx1, int ly1, int lx2, int ly2) {
		if (px >= lx1 && px <= lx2 && py >= ly1 && py <= ly2)
			return null;
		Point p = new Point();
		p.x = (px <= lx1) ? lx1 : (px >= lx2) ? lx2 : (lx1+lx2)/2;
		p.y = (py <= ly1) ? ly1 : (py >= ly2) ? ly2 : (ly1+ly2)/2;
		return p;
		}

	protected void drawSelectionOutline(Graphics g) {
		if (mDragMode == DRAG_MODE_RECT_SELECT
		 || mDragMode == DRAG_MODE_LASSO_SELECT) {
			g.setColor(VisualizationColor.cSelectedColor);
			if (mDragMode == DRAG_MODE_RECT_SELECT)
				g.drawRect( Math.min(mMouseX1, mMouseX2),
							Math.min(mMouseY1, mMouseY2),
							Math.abs(mMouseX2 - mMouseX1),
							Math.abs(mMouseY2 - mMouseY1) );
			else
				g.drawPolygon(mLassoRegion);
			}
		}

	protected abstract VisualizationPoint createVisualizationPoint(CompoundRecord record);
	protected abstract void updateHighlightedLabelPosition();
	public abstract int getAvailableShapeCount();
	public abstract int print(Graphics g, PageFormat f, int pageIndex);
	public abstract void paintHighResolution(Graphics2D g, Rectangle bounds, float fontScaling, boolean transparentBG, boolean isPrinting);

	// methods needed by VisualizationLegend
	public abstract int getStringWidth(String s);
	public abstract void setFontHeight(int h);
	protected abstract void setColor(Color c);
	protected abstract void drawLine(int x1, int y1, int x2, int y2);
	protected abstract void drawRect(int x, int y, int w, int h);
	protected abstract void fillRect(int x, int y, int w, int h);
	protected abstract void drawMarker(Color color, int shape, int size, int x, int y);
	protected abstract void drawString(String s, int x, int y);
	protected abstract void drawMolecule(StereoMolecule mol, Color color, GenericRectangle rect, int mode, int maxAVBL);

	protected void setActivePoint(VisualizationPoint vp) {
		mActivePoint = vp;

		if (mTreeViewMode != cTreeViewModeNone)
			updateTreeViewGraph();

		for (int axis=0; axis<mDimensions; axis++)
			if (mAxisIndex[axis] != cColumnUnassigned && mTableModel.isDescriptorColumn(mAxisIndex[axis]))
				setSimilarityValues(axis, -1);

		updateSimilarityMarkerSizes(-1);

		repaint();
		}

	private void setHighlightedPoint(VisualizationPoint vp) {
		mHighlightedPoint = vp;
		repaint();
		}

	/**
	 * Determines whether radial chart view is selected and if conditions are such
	 * that a radial chart is shown when a current record is defined, i.e. we have
	 * connection lines defined by both a referencing and a referenced column.
	 * @return true radial chart view can be displayed
	 */
	public int getTreeViewMode() {
		if (mTreeViewMode != cTreeViewModeNone
		 && mConnectionColumn != cColumnUnassigned
		 && mConnectionColumn != cConnectionColumnConnectAll
		 && mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn)) != -1)
			return mTreeViewMode;
		return cTreeViewModeNone;
		}

	public int getTreeViewRadius() {
		return mTreeViewRadius;
		}

	public void setTreeViewMode(int mode, int radius, boolean showAll, boolean isDynamic, boolean isInverted) {
		if (mTreeViewMode != mode
		 || (mTreeViewMode != cTreeViewModeNone
		  && (mTreeViewRadius != radius
		   || mTreeViewShowAll != showAll
		   || mTreeViewIsDynamic != isDynamic
		   || mTreeViewIsInverted != isInverted))) {
			mTreeViewMode = mode;
			mTreeViewRadius = radius;
			mTreeViewShowAll = showAll;
			mTreeViewIsDynamic = isDynamic;
			mTreeViewIsInverted = isInverted;
			updateTreeViewGraph();
			}
		}

	public boolean isTreeViewDynamic() {
		return mTreeViewIsDynamic;
		}

	public boolean isTreeViewInverted() {
		return mTreeViewIsInverted;
	}

	public boolean isTreeViewShowAll() {
		return mTreeViewShowAll;
		}

	public boolean isTreeViewModeEnabled() {
		return getTreeViewMode() != cTreeViewModeNone;
		}

	public boolean isMarkerLabelsInTreeViewOnly() {
		return mLabelsInTreeViewOnly;
		}

	/**
	 * Determines whether currently a tree view is displayed, which includes
	 * empty tree views, if no root is chosen and not all rows are shown.
	 * @return
	 */
	protected boolean isTreeViewGraph() {
		return mTreeViewMode != cTreeViewModeNone
			&& (mActivePoint != null || !mTreeViewShowAll)
		   	&& mConnectionColumn != cColumnUnassigned
		   	&& mConnectionColumn != cConnectionColumnConnectAll
		   	&& mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn)) != -1;
		}

	protected int compareConnectionLinePoints(VisualizationPoint p1, VisualizationPoint p2) {
		if (p1.hvIndex != p2.hvIndex)
			return (p1.hvIndex < p2.hvIndex) ? -1 : 1;
		if (isCaseSeparationDone()) {
			float v1 = p1.record.getDouble(mCaseSeparationColumn);
			float v2 = p2.record.getDouble(mCaseSeparationColumn);
			if (Float.isNaN(v1) ^ Float.isNaN(v2))
				return Float.isNaN(v2) ? -1 : 1;
			if (!Float.isNaN(v1) && !Float.isNaN(v2) && v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		if (mConnectionColumn > cColumnUnassigned) {
			float v1 = p1.record.getDouble(mConnectionColumn);
			float v2 = p2.record.getDouble(mConnectionColumn);
			if (Float.isNaN(v1) ^ Float.isNaN(v2))
				return Float.isNaN(v2) ? -1 : 1;
			if (!Float.isNaN(v1) && !Float.isNaN(v2) && v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		int connectionOrderColumn = (mConnectionOrderColumn == cColumnUnassigned) ? mAxisIndex[0] : mConnectionOrderColumn;
		if (connectionOrderColumn != cColumnUnassigned) {
			float v1 = p1.record.getDouble(connectionOrderColumn);
			float v2 = p2.record.getDouble(connectionOrderColumn);
			if (Float.isNaN(v1) ^ Float.isNaN(v2))
				return Float.isNaN(v2) ? -1 : 1;
			if (!Float.isNaN(v1) && !Float.isNaN(v2) && v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		return 0;
		}

	protected boolean isConnectionLineSuppressed(VisualizationPoint p1, VisualizationPoint p2) {
		if (mConnectionLineListMode == cConnectionListModeNone)
			return false;

		long flag1 = mTableModel.getListHandler().getListMask(mConnectionLineList1);

		boolean found = false;
		if (mConnectionLineList2 == -1) {
			found = ((p1.record.getFlags() & flag1) != 0L
				  || (p2.record.getFlags() & flag1) != 0L);
			}
		else {
			long flag2 = mTableModel.getListHandler().getListMask(mConnectionLineList2);
			found = (((p1.record.getFlags() & flag1) != 0L && (p2.record.getFlags() & flag2) != 0L)
				  || ((p1.record.getFlags() & flag2) != 0L && (p2.record.getFlags() & flag1) != 0L));
			}

		return found ^ (mConnectionLineListMode != cConnectionListModeHide);
		}

	protected boolean isConnectionLinePossible(VisualizationPoint p1, VisualizationPoint p2) {
		if (p1.hvIndex != p2.hvIndex
		 || (isCaseSeparationDone()
		  && p1.record.getDouble(mCaseSeparationColumn) != p2.record.getDouble(mCaseSeparationColumn))
		 || (mConnectionColumn != JVisualization.cConnectionColumnConnectAll
		  && p1.record.getDouble(mConnectionColumn) != p2.record.getDouble(mConnectionColumn)))
		  	return false;
		return true;
		}

	protected int getNextChangedConnectionLinePointIndex(int index1) {
		int index2 = index1+1;

		while (index2<mConnectionLinePoint.length
			&& compareConnectionLinePoints(mConnectionLinePoint[index1], mConnectionLinePoint[index2]) == 0)
			index2++;

		return index2;
		}

	private void updateTreeViewGraph() {
		boolean oldWasNull = (mTreeNodeList == null);
		mTreeNodeList = null;

		if (isTreeViewGraph()) {
			int referencedColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn));
			int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
			float min = 0;
			float max = 0;
			float dif = 0;
			if (strengthColumn != -1) {
				min = mTableModel.getMinimumValue(strengthColumn);
				max = mTableModel.getMaximumValue(strengthColumn);
				if (max == min) {
					strengthColumn = -1;
					}
				else {
					min -= 0.2f * (max - min);
					dif = max - min;
					}
				}

			if (mConnectionLineMap == null)
				mConnectionLineMap = createReferenceMap(referencedColumn);

	   		for (int i=0; i<mDataPoints; i++)
	   			mPoint[i].exclusionFlags |= EXCLUSION_FLAG_DETAIL_GRAPH;

			if (mActivePoint == null || (mTreeViewIsDynamic && !isVisibleInModel(mActivePoint))) {
				mTreeNodeList = new VisualizationNode[0][];
				}
			else {
		   		mActivePoint.exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;

				VisualizationNode[] rootShell = new VisualizationNode[1];
				rootShell[0] = createVisualizationNode(mActivePoint, null, 1.0f);
	
				ArrayList<VisualizationNode[]> shellList = new ArrayList<VisualizationNode[]>();
				shellList.add(rootShell);

				boolean isReverseTree = mTreeViewIsInverted
						&& CompoundTableConstants.cColumnPropertyReferenceTypeTopDown.equals(
						mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType));
				if (mReverseConnectionMap == null && isReverseTree)
					mReverseConnectionMap = createReverseConnectionMap(mConnectionColumn);

				// create array lists for every shell
				for (int shell=1; shell<=mTreeViewRadius; shell++) {
					ArrayList<VisualizationNode> vpList = new ArrayList<VisualizationNode>();
					for (VisualizationNode parent:shellList.get(shell-1)) {
						if (isReverseTree) {
							LineConnection[] connection = mReverseConnectionMap.get(parent.getVisualizationPoint());
							if (connection != null) {
								int firstChildIndex = vpList.size();
								for (int i=0; i<connection.length; i++) {
									VisualizationPoint vp = connection[i].target;
									if (vp != null && (!mTreeViewIsDynamic || isVisibleInModel(vp))) {
										float strength = connection[i].strength;
										VisualizationNode childNode = null;

										// if we have a strength value and the child is already connected compare strength values
										if ((vp.exclusionFlags & EXCLUSION_FLAG_DETAIL_GRAPH) == 0) {
											for (int j=0; j<firstChildIndex; j++) {
												if (vpList.get(j).getVisualizationPoint() == vp) {
													if (vpList.get(j).getStrength() < strength) {
														childNode = vpList.get(j);
														vpList.remove(childNode);
														firstChildIndex--;
														childNode.setParentNode(parent);
														childNode.setStrength(strength);
														}
													break;
													}
												}
											if (childNode == null)
												continue;
											}
										else {
											vp.exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
											childNode = createVisualizationNode(vp, parent, strength);
											}

										int insertIndex = firstChildIndex;
										while (insertIndex < vpList.size() && childNode.getStrength() <= vpList.get(insertIndex).getStrength())
											insertIndex++;

										vpList.add(insertIndex, childNode);
										}
									}
								}
							}
						else {
							byte[] data = (byte[])parent.getVisualizationPoint().record.getData(mConnectionColumn);
							if (data != null) {
								String[] entry = mTableModel.separateEntries(new String(data));
								String[] strength = null;
								if (strengthColumn != cColumnUnassigned) {
									byte[] strengthData = (byte[])parent.getVisualizationPoint().record.getData(strengthColumn);
									if (strengthData != null) {
										strength = mTableModel.separateEntries(new String(strengthData));
										if (strength.length != entry.length)
											strength = null;
										}
									}
								int firstChildIndex = vpList.size();
								for (int i=0; i<entry.length; i++) {
									String ref = entry[i];
									VisualizationPoint vp = mConnectionLineMap.get(ref.getBytes());
									if (vp != null && (!mTreeViewIsDynamic || isVisibleInModel(vp))) {
										// if we don't have connection strength information and the child is already connected to another parent
										if (strengthColumn == cColumnUnassigned && (vp.exclusionFlags & EXCLUSION_FLAG_DETAIL_GRAPH) == 0)
											continue;

										float strengthValue = 1.0f;
										if (strength != null) {
											try {
												float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strength[i], strengthColumn)));
												strengthValue = Float.isNaN(value) ? 0.0f : (float) ((value - min) / dif);
												}
											catch (NumberFormatException nfe) {}
											}

										VisualizationNode childNode = null;

										// if we have a strength value and the child is already connected compare strength values
										if ((vp.exclusionFlags & EXCLUSION_FLAG_DETAIL_GRAPH) == 0) {
											for (int j = 0; j < firstChildIndex; j++) {
												if (vpList.get(j).getVisualizationPoint() == vp) {
													if (vpList.get(j).getStrength() < strengthValue) {
														childNode = vpList.get(j);
														vpList.remove(childNode);
														firstChildIndex--;
														childNode.setParentNode(parent);
														childNode.setStrength(strengthValue);
														}
													break;
													}
												}
											if (childNode == null)
												continue;
											}
										else {
											vp.exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
											childNode = createVisualizationNode(vp, parent, strengthValue);
											}

										int insertIndex = firstChildIndex;
										while (insertIndex < vpList.size() && childNode.getStrength() <= vpList.get(insertIndex).getStrength())
											insertIndex++;

										vpList.add(insertIndex, childNode);
										}
									}
								}
							}
						}
	
					if (vpList.size() == 0)
						break;
	
					shellList.add(vpList.toArray(new VisualizationNode[0]));
					}
	
				mTreeNodeList = shellList.toArray(new VisualizationNode[0][]);
				}
			}

		if (!oldWasNull && mTreeNodeList == null) {
	   		for (int i=0; i<mDataPoints; i++)
	   			mPoint[i].exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
			}

		if (!oldWasNull || mTreeNodeList != null) {
			// if we have a tree without any nodes (no chosen root) we don't want to hide invisible rows from other views
			mActiveExclusionFlags = (mTreeNodeList != null && mTreeNodeList.length == 0) ? 0 : EXCLUSION_FLAG_DETAIL_GRAPH;
			applyLocalExclusion(false);
			invalidateOffImage(true);
			}
		}

	private VisualizationNode createVisualizationNode(VisualizationPoint vp, VisualizationNode parent, float strength) {
		if (mTreeViewMode == cTreeViewModeLeftRoot || mTreeViewMode == cTreeViewModeRightRoot
		 || mTreeViewMode == cTreeViewModeTopRoot || mTreeViewMode == cTreeViewModeBottomRoot)
			return new TreeVisualizationNode(vp, (TreeVisualizationNode)parent, strength);
		if (mTreeViewMode == cTreeViewModeRadial)
			return new RadialVisualizationNode(vp, (RadialVisualizationNode)parent, strength);
		return null;
		}

	protected void invalidateOffImage(boolean invalidateCoordinates) {
		if (invalidateCoordinates)
			mCoordinatesValid = false;
		mOffImageValid = false;
		repaint();
		}

	protected void calculateLegend(Rectangle bounds, int fontHeight) {
		mLegendList.clear();
		if (!mSuppressLegend) {
			int initialHeight = bounds.height;
			addLegends(bounds, fontHeight);

			// if we have less than 20% of the height then stepwise remove large legends to remedy
			int removalCount = 0;
			while (bounds.height < initialHeight / 5) {
				int largestLegendIndex = -1;
				int largestLegendHeight = 0;
				for (int i=0; i<mLegendList.size(); i++) {
					VisualizationLegend legend = mLegendList.get(i);
					if (largestLegendHeight < legend.getHeight()) {
						largestLegendHeight = legend.getHeight();
						largestLegendIndex = i;
						}
					}
				bounds.height += largestLegendHeight;
				mLegendList.remove(largestLegendIndex);
				for (int i=largestLegendIndex; i<mLegendList.size(); i++)
					mLegendList.get(i).moveVertically(-largestLegendHeight);
				removalCount++;
				}
			if (removalCount != 0)
				mWarningMessage = "Reduce font size to show "+removalCount+" suppressed legend(s)!";
			}
		}

	protected void addLegends(Rectangle bounds, int fontHeight) {
		if (mMarkerSizeColumn != cColumnUnassigned
		 && mChartType.displaysMarkers()) {
			VisualizationLegend sizeLegend = new VisualizationLegend(this, mTableModel,
													mMarkerSizeColumn,
													null,
													VisualizationLegend.cLegendTypeSize);
			sizeLegend.calculate(bounds, fontHeight);
			bounds.height -= sizeLegend.getHeight();
			mLegendList.add(sizeLegend);
			}

		if (mMarkerShapeColumn != cColumnUnassigned
		 && mChartType.displaysMarkers()
			// if the marker color and marker shape encode the same categories, then use only one legend
		 && (mMarkerColor.getColorColumn() != mMarkerShapeColumn
		  || mMarkerColor.getColorListMode() != VisualizationColor.cColorListModeCategories)) {
			VisualizationLegend shapeLegend = new VisualizationLegend(this, mTableModel,
													mMarkerShapeColumn,
													null,
													VisualizationLegend.cLegendTypeShapeCategory);
			shapeLegend.calculate(bounds, fontHeight);
			bounds.height -= shapeLegend.getHeight();
			mLegendList.add(shapeLegend);
			}

		if (mMarkerColor.getColorColumn() != cColumnUnassigned) {
			VisualizationLegend colorLegend = new VisualizationLegend(this, mTableModel,
													mMarkerColor.getColorColumn(),
													mMarkerColor,
													mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories ?
													  VisualizationLegend.cLegendTypeColorCategory
													: VisualizationLegend.cLegendTypeColorDouble);
			colorLegend.calculate(bounds, fontHeight);
			bounds.height -= colorLegend.getHeight();
			mLegendList.add(colorLegend);
			}

/* Don't add a legend for label background colors!
   If we want that later, we should adapt the legend painter to apply the proper label transparency!
		if (mLabelBackgroundColor.getColorColumn() != cColumnUnassigned) {
			VisualizationLegend colorLegend = new VisualizationLegend(this, mTableModel,
					mLabelBackgroundColor.getColorColumn(),
					mLabelBackgroundColor,
					mLabelBackgroundColor.getColorListMode() == VisualizationColor.cColorListModeCategories ?
							VisualizationLegend.cLegendTypeLabelBackgroundCategory
							: VisualizationLegend.cLegendTypeColorDouble);
			colorLegend.calculate(bounds, fontHeight);
			bounds.height -= colorLegend.getHeight();
			mLegendList.add(colorLegend);
			}
 */
		}

	protected void paintLegend(Rectangle bounds, boolean transparentBG) {
		for (VisualizationLegend legend:mLegendList)
			legend.paint(bounds, transparentBG);
		}

	protected void paintTouchIcon(Graphics g) {
		if (mTouchFunctionActive) {
			g.setColor(Color.red);
			g.drawString("touch", 10, 20);
			}
		}

	protected void paintMessages(Graphics g, int boundsX, int boundsWidth) {
		String msg = "";

		if (mIsIgnoreGlobalExclusion)
			msg = msg.concat("  Filters off!");

		if (mLocalExclusionList != -1)
			msg = msg.concat("  List '"+mTableModel.getListHandler().getListName(mLocalExclusionList)+"' only!");

		if (msg.length() != 0) {
			int fontSize = HiDPIHelper.scale(10);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
			g.setColor(ColorHelper.getContrastColor(Color.red, getViewBackground()));
			g.drawString(msg, boundsX + boundsWidth - fontSize/2 - g.getFontMetrics().stringWidth(msg), fontSize*3/2);
			}
		}

	public void setDetailPopupProvider(DetailPopupProvider p) {
		mDetailPopupProvider = p;
		}

	/**
	 * This returns the <b>indended</b> case separation column that was defined with
	 * setCaseSeparation(). Whether the view really separates cases by applying a case
	 * specific shift to markers, bars or boxes, depends on other view settings.
	 * This shift is not applied, if<br>
	 * - the categories are already separated, because the same column is also assigned to an axis.<br>
	 * - none of the axes is unassigned or assigned to another category column with a low number of categories.<br>
	 * Call isCaseSeparationDone() to find out, whether the view applies a case specific shift.
	 * @return
	 */
	public int getCaseSeparationColumn() {
		return mCaseSeparationColumn;
		}

	public float getCaseSeparationValue() {
		return mCaseSeparationValue;
		}

	public int getCaseSeparationCategoryCount() {
		return mCaseSeparationCategoryCount;
		}

	/**
	 * Checks, whether a case separation column was defined and the view applies
	 * a case specific shift to all markers, bars or boxes.
	 * Case separation is not done, if the case separation column is also assigned to an axis.
	 * Case separation is not possible if we have no category axis or unassigned axis.
	 * @return true, if the view applies a case specific shift to markers, bars or boxes
	 */
	public boolean isCaseSeparationDone() {
		if (mCaseSeparationColumn == cColumnUnassigned)
			return false;

		for (int axis=0; axis<mDimensions; axis++)
			if (mAxisIndex[axis] == mCaseSeparationColumn)
				return false;	// don't separate cases, if we have them separated on one axis anyway

		boolean categoryCountExceeded = false;
		for (int axis=0; axis<mDimensions; axis++) {
			if (mAxisIndex[axis] == cColumnUnassigned)
				return true;
			if (mTableModel.isColumnTypeCategory(mAxisIndex[axis])) {
				if (mTableModel.getCategoryCount(mAxisIndex[axis]) <= cMaxCaseSeparationCategoryCount)
					return true;
				else
					categoryCountExceeded = true;
				}
			}

		if (categoryCountExceeded)
			mWarningMessage = "Too many categories on axis for case separation!";

		return false;
		}

	protected int getCaseSeparationAxis() {
		if (!isCaseSeparationDone())
			return -1;

		int preferredAxis = -1;
		int preferredCategoryCount = Integer.MAX_VALUE;
		for (int axis=0; axis<mDimensions; axis++) {
			if (!mChartType.isDistributionPlot()
			 || mChartInfo.getDoubleAxis() != axis) {
				int column = mAxisIndex[axis];
				if (column == cColumnUnassigned) {
					if (preferredCategoryCount > 1) {
						preferredAxis = axis;
						preferredCategoryCount = 1;
						}
					}
				else if (mTableModel.isColumnTypeCategory(column)
					  && mTableModel.getCategoryCount(column) <= cMaxCaseSeparationCategoryCount
					  && preferredCategoryCount > mTableModel.getCategoryCount(column)) {
					preferredAxis = axis;
					preferredCategoryCount = mTableModel.getCategoryCount(column);
					}
				}
			}
		return preferredAxis;
		}

	protected int getFocusList() {
		return mFocusList;
		}

	public int getFocusFlag() {
		return (mFocusList == FocusableView.cFocusNone) ? -1
			 : (mFocusList == FocusableView.cFocusOnSelection) ? CompoundRecord.cFlagSelected
			 : mTableModel.getListHandler().getListFlagNo(mFocusList);
		}

	protected int getLabelFlag() {
		return (mLabelList == cLabelsOnAllRows) ? getFocusFlag()
				: (mLabelList == FocusableView.cFocusOnSelection) ? CompoundRecord.cFlagSelected
				: mTableModel.getListHandler().getListFlagNo(mLabelList);
		}

	public float getFontSize() {
		return mRelativeFontSize;
		}

	public int getMarkerShapeColumn() {
		return mMarkerShapeColumn;
		}

	public float getJittering() {
		return mMarkerJittering;
		}

	public int getJitterAxes() {
		return mMarkerJitteringAxes;
	}

	public float getMarkerSize() {
		return mRelativeMarkerSize;
		}

	public int getMarkerSizeColumn() {
		return mMarkerSizeColumn;
		}

	public float getMarkerSizeMin() {
		return (mMarkerSizeColumn > 0
			 && !mMarkerSizeProportional
			 && (mTableModel.isColumnTypeDouble(mMarkerSizeColumn)
			  || mTableModel.isDescriptorColumn(mMarkerSizeColumn))) ? mMarkerSizeMin : Float.NaN;
		}

	public float getMarkerSizeMax() {
		return (mMarkerSizeColumn > 0
			 && !mMarkerSizeProportional
			 && (mTableModel.isColumnTypeDouble(mMarkerSizeColumn)
			  || mTableModel.isDescriptorColumn(mMarkerSizeColumn))) ? mMarkerSizeMax : Float.NaN;
		}

	public boolean getMarkerSizeInversion() {
		return mMarkerSizeInversion;
		}

	public boolean getMarkerSizeProportional() {
		return mMarkerSizeProportional;
		}

	public boolean isMarkerSizeZoomAdapted() {
		return !Float.isNaN(mMarkerSizeZoomAdaption);
		}

	public float getMarkerLabelSize() {
		return mMarkerLabelSize;
		}

	/**
	 * Calculates the font size for drawing a marker label. Usually this depends on
	 * default font size, general font size factor, marker label size factor.
	 * However, if we have a central label instead of a marker, and if the marker size column
	 * is set, then the central label's font size is defined by the general marker label size setting.
	 * @param vp
	 * @param position cMidCenter or other
	 * @return
	 */
	protected float getLabelFontSize(VisualizationPoint vp, int position, boolean isTreeView) {
		if (mMarkerSizeColumn != cColumnUnassigned
		 && position == cMidCenter
		 && !isTreeView) {
			float fontsize = getMarkerSize(vp) / 2.0f;  // includes marker zoom adaption factor
			float factor = 0.5f*(mMarkerLabelSize+1.0f); // reduce effect by factor 2.0
			return Math.max(3f, mRelativeFontSize * factor * fontsize);
			}

		// if we use marker zoom adaption, we slightly adapt decorating labels as well
		float factor = Float.isNaN(mMarkerSizeZoomAdaption) ? 1f : 1f + (mZoomState * LABEL_ZOOM_ADAPTION_FACTOR) - LABEL_ZOOM_ADAPTION_FACTOR;
		return factor * mMarkerLabelSize * mFontHeight;
		}

	/**
	 * Calculates the average bond length used for molecule scaling, if a label
	 * displays the chemical structure. This method is based on getLabelFontSize()
	 * and, thus, used the same logic.
	 * @param vp
	 * @param position cMidCenter or other
	 * @return
	 */
	protected float getLabelAVBL(VisualizationPoint vp, int position, boolean isTreeView) {
		return getLabelFontSize(vp, position, isTreeView) / 2f;
		}

	/**
	 * Calculates the absolute marker size of the visualization point, which depends
	 * on a view size specific base value (mAbsoluteMarkerSize), a user changeable factor
	 * (mRelativeMarkerSize), possibly on the zoom state (mMarkerSizeZoomAdaption), and optionally
	 * another factor derived from a column value defined to influence the marker size.
	 * It also includes the retina factor, but not the anti-aliasing factor.
	 * @param vp
	 * @return
	 */
	protected float getMarkerSize(VisualizationPoint vp) {
		if (mMarkerSizeColumn == cColumnUnassigned) {
			float size = Float.isNaN(mMarkerSizeZoomAdaption) ? mAbsoluteMarkerSize : mAbsoluteMarkerSize * mMarkerSizeZoomAdaption;
			return validateSizeWithConnections(size);
			}

		if (mTableModel.isDescriptorColumn(mMarkerSizeColumn)) {
			return getMarkerSizeFromDescriptorSimilarity(mSimilarityMarkerSize == null ? 0.2f : mSimilarityMarkerSize[vp.record.getID()]);
			}

		if (CompoundTableListHandler.isListColumn(mMarkerSizeColumn))
			return getMarkerSizeFromHitlistMembership(vp.record.isFlagSet(
				mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.convertToListIndex(mMarkerSizeColumn))));

		return getMarkerSizeFromValue(vp.record.getDouble(mMarkerSizeColumn));
		}

	protected float getMarkerSizeFromHitlistMembership(boolean isMember) {
		float size = (isMember ^ mMarkerSizeInversion) ?
				mAbsoluteMarkerSize * 1.2f : mAbsoluteMarkerSize * 0.6f;
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			size *= mMarkerSizeZoomAdaption;
		return validateSizeWithConnections(size);
		}

	private float getMarkerSizeFromDescriptorSimilarity(float similarity) {
		float size = 2f * mAbsoluteMarkerSize * similarity;
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			size *= mMarkerSizeZoomAdaption;
		return validateSizeWithConnections(size);
		}

	protected float getMarkerSizeFromValue(float value) {
		float factor = getMarkerSizeVPFactor(value, mMarkerSizeColumn);
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			factor *= mMarkerSizeZoomAdaption;
		float size = (int)(factor*mAbsoluteMarkerSize);
		return validateSizeWithConnections(size);
		}

	/**
	 * Calculates the marker size factor from the absolute(!) row value normalized
	 * by the maximum of all absolute values. Returned is 2*sqrt(value/max)) assuming that
	 * the factor is used on two marker dimensions to make the marker area proportional
	 * to the value.
	 * @param value
	 * @param valueColumn
	 * @return 0.0 -> 2.0
	 */
	protected float getMarkerSizeVPFactor(float value, int valueColumn) {
		if (mMarkerSizeProportional) {
			if (Float.isNaN(value))
				return 0;
			float min = mTableModel.getMinimumValue(valueColumn);
			float max = mTableModel.getMaximumValue(valueColumn);
			if (mTableModel.isLogarithmicViewMode(valueColumn)) {
				max = (float)Math.pow(10, max);
				value = (float)Math.pow(10, value);
				}
			else {
				max = Math.max(Math.abs(min), Math.abs(max));
				value = Math.abs(value);
				}
			return 2f*(float)Math.sqrt((0.000001+(mMarkerSizeInversion?max-value:value) / max) / 1.000001f);
			}
		else {
			float min = mTableModel.getMinimumValue(valueColumn);
			if (!Float.isNaN(mMarkerSizeMin)) {
				float msmin = mTableModel.isLogarithmicViewMode(valueColumn) ?
						(float)Math.log10(mMarkerSizeMin) : mMarkerSizeMin;
				if (min < msmin)
					min = msmin;
				if (!Float.isNaN(value) && value < min)
					value = min;
				}
			if (Float.isNaN(value))
				value = min;
			float max = mTableModel.getMaximumValue(valueColumn);
			if (!Float.isNaN(mMarkerSizeMax)) {
				float msmax = mTableModel.isLogarithmicViewMode(valueColumn) ?
						(float)Math.log10(mMarkerSizeMax) : mMarkerSizeMax;
				if (max < msmax)
					max = msmax;
				if (value > max)
					value = max;
				}
			float minValue = (mLabelColumn[cMidCenter] == -1) ? 0.04f : 0.1f;
			return 2f*(float)Math.sqrt((minValue+(mMarkerSizeInversion?max-value:value-min) / (max-min)) / (1.0f+minValue));
			}
		}

	/**
	 * Make marker size not smaller than 1.5 unless<br>
	 * - marker sizes are not modulated by a column value<br>
	 * - and connection lines are shown that are thicker than the marker<br>
	 * This allows to reduce marker size to 0, if connection lines are shown.
	 * @param size updated size
	 * @return
	 */
	private float validateSizeWithConnections(float size) {
		if (size < 1.5) {
			if (mMarkerSizeColumn != cColumnUnassigned
			 || mConnectionColumn == cColumnUnassigned
			 || mAbsoluteConnectionLineWidth < size)
				size = 1.5f;
			}

		return size;
		}

	public void colorChanged(VisualizationColor source) {
		if (source == mMarkerColor) {
			updateColorIndices(mMarkerColor, VisualizationPoint.COLOR_TYPE_MARKER_FG);
			}
		else if (source == mLabelBackgroundColor) {
			updateColorIndices(mLabelBackgroundColor, VisualizationPoint.COLOR_TYPE_LABEL_BG);
			}
		}

	protected boolean isLegendLayoutValid(VisualizationLegend legend, VisualizationColor color) {
		if (legend == null || color.getColorColumn() == cColumnUnassigned)
			return legend == null && color.getColorColumn() == cColumnUnassigned;

		return legend.layoutIsValid(color.getColorListMode() == VisualizationColor.cColorListModeCategories, color.getColorListSizeWithoutDefaults());
		}

	public VisualizationColor getMarkerColor() {
		return mMarkerColor;
		}

	public VisualizationColor getLabelBackgroundColor() {
		return mLabelBackgroundColor;
		}

	protected void updateColorIndices(VisualizationColor visualizationColor, int colorType) {
		if (visualizationColor.getColorColumn() == cColumnUnassigned) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].setColorIndex(colorType, VisualizationColor.cDefaultDataColorIndex);
			}
		else if (CompoundTableListHandler.isListColumn(visualizationColor.getColorColumn())) {
			int listIndex = CompoundTableListHandler.convertToListIndex(visualizationColor.getColorColumn());
			int flagNo = mTableModel.getListHandler().getListFlagNo(listIndex);
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].setColorIndex(colorType, (short)(mPoint[i].record.isFlagSet(flagNo) ?
						VisualizationColor.cSpecialColorCount : VisualizationColor.cSpecialColorCount + 1));
			}
		else if (mTableModel.isDescriptorColumn(visualizationColor.getColorColumn())) {
			setSimilarityColors(mMarkerColor, VisualizationPoint.COLOR_TYPE_MARKER_FG, -1);
			}
		else if (visualizationColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
			float[] thresholds = visualizationColor.getColorThresholds();
			if (thresholds != null) {
				for (int i=0; i<mDataPoints; i++) {
					float value = mPoint[i].record.getDouble(visualizationColor.getColorColumn());
					if (Float.isNaN(value)) {
						mPoint[i].setColorIndex(colorType, VisualizationColor.cMissingDataColorIndex);
						}
					else {
						if (mTableModel.isLogarithmicViewMode(visualizationColor.getColorColumn()))
							value = (float)Math.pow(10, value);
						mPoint[i].setColorIndex(colorType, (short)(VisualizationColor.cSpecialColorCount+thresholds.length));
						for (int j=0; j<thresholds.length; j++) {
							if (value<thresholds[j]) {
								mPoint[i].setColorIndex(colorType, (short)(VisualizationColor.cSpecialColorCount + j));
								break;
								}
							}
						}
					}
				}
			else {
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].setColorIndex(colorType, (short)(VisualizationColor.cSpecialColorCount
							+ mTableModel.getCategoryIndex(visualizationColor.getColorColumn(), mPoint[i].record)));
				}
			}
		else if (mTableModel.isColumnTypeDouble(visualizationColor.getColorColumn())) {
			float min = Float.isNaN(visualizationColor.getColorMin()) ?
									mTableModel.getMinimumValue(visualizationColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(visualizationColor.getColorColumn())) ?
							   (float)Math.log10(visualizationColor.getColorMin()) : visualizationColor.getColorMin();
			float max = Float.isNaN(visualizationColor.getColorMax()) ?
									mTableModel.getMaximumValue(visualizationColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(visualizationColor.getColorColumn())) ?
							   (float)Math.log10(visualizationColor.getColorMax()) : visualizationColor.getColorMax();

			//	1. colorMin is explicitly set; max is real max, but lower than min
			// or 2. colorMax is explicitly set; min is real min, but larger than max
			// first case is OK, second needs adaption below to be handled as indented
			if (min >= max)  
				if (!Float.isNaN(visualizationColor.getColorMax()))
					min = Float.MIN_VALUE;

			for (int i=0; i<mDataPoints; i++) {
				float value = mPoint[i].record.getDouble(visualizationColor.getColorColumn());
				if (Float.isNaN(value))
					mPoint[i].setColorIndex(colorType, VisualizationColor.cMissingDataColorIndex);
				else if (value <= min)
					mPoint[i].setColorIndex(colorType, (short)VisualizationColor.cSpecialColorCount);
				else if (value >= max)
					mPoint[i].setColorIndex(colorType, (short)(visualizationColor.getColorList().length-1));
				else
					mPoint[i].setColorIndex(colorType, (short)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(visualizationColor.getColorList().length-VisualizationColor.cSpecialColorCount-1)
						* (value - min) / (max - min)));
				}
			}

		invalidateOffImage(true);
		}

	/**
	 * @param axis
	 * @param rowID if != -1 then update this row only
	 */
	private void setSimilarityValues(int axis, int rowID) {
		int column = mAxisIndex[axis];
		if (mActivePoint == null) {
			mAxisSimilarity[axis] = null;
			}
		else if (rowID != -1) {
			for (int i=0; i<mDataPoints; i++) {
				if (mPoint[i].record.getID() == rowID) {
					mAxisSimilarity[axis][rowID] = mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, column);
					break;
					}
				}
			}
		else {
			if (DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(column))) {
				// if we have the slow 3DPPMM2 then use a progress dialog and multi-threading
				mAxisSimilarity[axis] = null;
				Object descriptor = mActivePoint.record.getData(column);
				if (descriptor != null) {
					Component c = this;
					while (!(c instanceof Frame))
						c = c.getParent();

					String idcode = new String((byte[])mActivePoint.record.getData(mTableModel.getParentColumn(column)));
					mAxisSimilarity[axis] = createSimilarityListSMP(idcode, descriptor, column);
					}
				}
			else {
				mAxisSimilarity[axis] = new float[mDataPoints];
				for (int i=0; i<mDataPoints; i++)
					mAxisSimilarity[axis][mPoint[i].record.getID()] =
								mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, column);
				}
			}
		updateLocalZoomExclusion(axis);
		invalidateOffImage(true);
		}

	/**
	 * @param rowID if != -1 then update this row only
	 */
	private void updateSimilarityMarkerSizes(int rowID) {
		if (mActivePoint == null
		 || !mTableModel.isDescriptorColumn(mMarkerSizeColumn)) {
			mSimilarityMarkerSize = null;
			}
		else if (rowID != -1) {
			for (int i=0; i<mDataPoints; i++) {
				if (mPoint[i].record.getID() == rowID) {
					mSimilarityMarkerSize[rowID] =	mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, mMarkerSizeColumn);
					break;
					}
				}
			}
		else {
			if (DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(mMarkerSizeColumn))) {
				// if we have the slow 3DPPMM2 then use a progress dialog and multi-threading
				mSimilarityMarkerSize = null;
				Object descriptor = mActivePoint.record.getData(mMarkerSizeColumn);
				if (descriptor != null) {
					Component c = this;
					while (!(c instanceof Frame))
						c = c.getParent();

					String idcode = new String((byte[])mActivePoint.record.getData(mTableModel.getParentColumn(mMarkerSizeColumn)));
					mSimilarityMarkerSize = createSimilarityListSMP(idcode, descriptor, mMarkerSizeColumn);
					}
				}
			else {
				mSimilarityMarkerSize = new float[mDataPoints];
				for (int i=0; i<mDataPoints; i++)
					mSimilarityMarkerSize[mPoint[i].record.getID()] =
						mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, mMarkerSizeColumn);
				}
			}
		}

	/**
	 * @param rowID if != -1 then update this row only
	 */
	protected void setSimilarityColors(VisualizationColor visualizationColor, int colorType, int rowID) {
		if (mActivePoint == null) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].setColorIndex(colorType, VisualizationColor.cDefaultDataColorIndex);
			}
		else {
			float min = Float.isNaN(visualizationColor.getColorMin()) ? 0.0f : visualizationColor.getColorMin();
			float max = Float.isNaN(visualizationColor.getColorMax()) ? 1.0f : visualizationColor.getColorMax();
			if (min >= max) {
				min = 0.0f;
				max = 1.0f;
				}

			int column = visualizationColor.getColorColumn();
			float[] flexophoreSimilarity = null;
			if (rowID == -1
			 && DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(column))) {
				// if we have the slow 3DPPMM2 then use a progress dialog and multi-threading
				Object descriptor = mActivePoint.record.getData(column);
				if (descriptor != null) {
					Component c = this;
					while (!(c instanceof Frame))
						c = c.getParent();

					String idcode = new String((byte[])mActivePoint.record.getData(mTableModel.getParentColumn(column)));
					flexophoreSimilarity = createSimilarityListSMP(idcode, descriptor, column);
					if (flexophoreSimilarity == null) {	// cancelled
						visualizationColor.setColor(cColumnUnassigned);
						return;
						}
					}
				}

			for (int i=0; i<mDataPoints; i++) {
				if (rowID == -1 || mActivePoint.record.getID() == rowID) {
					float similarity = (flexophoreSimilarity != null) ? flexophoreSimilarity[i]
									  : mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, column);
					if (Float.isNaN(similarity))
						mPoint[i].setColorIndex(colorType, VisualizationColor.cMissingDataColorIndex);
					else if (visualizationColor.getColorThresholds() != null) {
						float[] thresholds = visualizationColor.getColorThresholds();
						mPoint[i].setColorIndex(colorType, (short)(VisualizationColor.cSpecialColorCount + thresholds.length));
						for (int j=0; j<thresholds.length; j++) {
							if (similarity<thresholds[j]) {
								mPoint[i].setColorIndex(colorType, (short)(VisualizationColor.cSpecialColorCount + j));
								break;
								}
							}
						}

					else if (similarity <= min)
						mPoint[i].setColorIndex(colorType, (short)VisualizationColor.cSpecialColorCount);
					else if (similarity >= max)
						mPoint[i].setColorIndex(colorType, (short)(visualizationColor.getColorList().length-1));
					else
						mPoint[i].setColorIndex(colorType, (short)(0.5 + VisualizationColor.cSpecialColorCount
							+ (float)(visualizationColor.getColorList().length - VisualizationColor.cSpecialColorCount - 1)
							* (similarity - min) / (max - min)));
					}
				}
			}
		}

	private float[] createSimilarityListSMP(String idcode, Object descriptor, int descriptorColumn) {
		float[] similarity = mTableModel.getStructureSimilarityListFromCache(idcode, descriptorColumn);
		if (similarity != null)
			return similarity;

		Component c = this;
		while (!(c instanceof Frame))
			c = c.getParent();

		JProgressDialog progressDialog = new JProgressDialog((Frame)c) {
			private static final long serialVersionUID = 0x20110325;

			public void stopProgress() {
				super.stopProgress();
				close();
				}
			};

	   	mTableModel.createSimilarityListSMP(null, descriptor, idcode, descriptorColumn, progressDialog, false);

   		progressDialog.setVisible(true);
   		similarity = mTableModel.getSimilarityListSMP(false);

		return similarity;
 		}

	/**
	 * Calculates for the given category column, which of its categories have at least one visible
	 * member in this view. To do so, this method updates mVisibleCategoryFromCategory[column].
	 * @param column valid category column
	 * @return
	 */
	protected int calculateVisibleCategoryCount(int column) {
		if (CompoundTableListHandler.isListOrSelectionColumn(column))
			return 2;	// don't handle lists here

		if (mVisibleCategoryFromCategory == null)
			mVisibleCategoryFromCategory = new int[mTableModel.getTotalColumnCount()][];

		// If the tableModel's row visibility was changed since the last generation of visible categories
		// then we have to freshly generate.
		if (mVisibleCategoryExclusionTag != mTableModel.getExclusionTag()) {
			mVisibleCategoryExclusionTag = mTableModel.getExclusionTag();
			mVisibleCategoryFromCategory[column] = null;
			}

		if (mVisibleCategoryFromCategory[column] == null) {
			int categoryCount = mTableModel.getCategoryCount(column);
			int count = 0;
			boolean[] isVisibleCategory = new boolean[categoryCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisible(mPoint[i])) {
					int category = mTableModel.getCategoryIndex(column, mPoint[i].record);
					if (!isVisibleCategory[category]) {
						isVisibleCategory[category] = true;
						if (++count == categoryCount)
							break;
						}
					}
				}

			mVisibleCategoryFromCategory[column] = new int[categoryCount];
			int visibleCategory = 0;
			for (int i=0; i<categoryCount; i++)
				mVisibleCategoryFromCategory[column][i] = isVisibleCategory[i] ? visibleCategory++ : -1;

			return count;
			}

		int count = 0;
		for (int i=0; i<mVisibleCategoryFromCategory[column].length; i++)
			if (mVisibleCategoryFromCategory[column][i] != -1)
				count++;
		return count;
		}

	/**
	 * Creates a list of those categories within the given column,
	 * which have at least one visible member in this view.
	 * @param column
	 * @return
	 */
	protected String[] getVisibleCategoryList(int column) {
		String[] category = mTableModel.getCategoryList(column);
		if (CompoundTableListHandler.isListOrSelectionColumn(column))
			return category;
		String[] visibleCategory = new String[calculateVisibleCategoryCount(column)];
		for (int i=0; i<mVisibleCategoryFromCategory[column].length; i++)
			if (mVisibleCategoryFromCategory[column][i] != -1)
				visibleCategory[mVisibleCategoryFromCategory[column][i]] = category[i];
		return visibleCategory;
		}

	private void invalidateSplittingIndices() {
		mHVExclusionTag = -1;
		invalidateOffImage(true);
		}

	/**
	 * Updates all rows assignments (hv-indexes) to split views, if they are invalid.
	 * @return true, if these assignments were updated.
	 */
	protected boolean validateSplittingIndices() {
		if (mHVExclusionTag == mTableModel.getExclusionTag())
			return false;

		mHVExclusionTag = mTableModel.getExclusionTag();

		int count1 = (mSplittingColumn[0] < cColumnUnassigned) ? 2
				   : (mSplittingColumn[0] == cColumnUnassigned) ? 1 : mShowEmptyInSplitView ?
				mTableModel.getCategoryCount(mSplittingColumn[0]) : calculateVisibleCategoryCount(mSplittingColumn[0]);
		int count2 = (mSplittingColumn[1] < cColumnUnassigned) ? 2
				   : (mSplittingColumn[1] == cColumnUnassigned) ? 1 : mShowEmptyInSplitView ?
				mTableModel.getCategoryCount(mSplittingColumn[1]) : calculateVisibleCategoryCount(mSplittingColumn[1]);
		mHVCount = Math.max(0, count1 * count2);

		mSplitViewCountExceeded = (mHVCount > cMaxSplitViewCount);
		if (mSplitViewCountExceeded) {
			mHVCount = 1;
			mWarningMessage = "The view is not split into individual views, because there would be too many of them!";
			}

		if (mHVCount == 1) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].hvIndex = 0;
			}
		else if (mSplittingColumn[1] == cColumnUnassigned) {
			if (CompoundTableListHandler.isListOrSelectionColumn(mSplittingColumn[0])) {
				int flagNo = mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.convertToListIndex(mSplittingColumn[0]));
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].hvIndex = mPoint[i].record.isFlagSet(flagNo) ? 0 : 1;
				}
			else if (mShowEmptyInSplitView) {
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].hvIndex = mTableModel.getCategoryIndex(mSplittingColumn[0], mPoint[i].record);
				}
			else {
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].hvIndex = mVisibleCategoryFromCategory[mSplittingColumn[0]][mTableModel.getCategoryIndex(mSplittingColumn[0], mPoint[i].record)];
				}
			}
		else {
			int flagNo1 = -1;
			if (CompoundTableListHandler.isListOrSelectionColumn(mSplittingColumn[0]))
				flagNo1 = mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.convertToListIndex(mSplittingColumn[0]));

			int flagNo2 = -1;
			if (CompoundTableListHandler.isListOrSelectionColumn(mSplittingColumn[1]))
				flagNo2 = mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.convertToListIndex(mSplittingColumn[1]));

			for (int i=0; i<mDataPoints; i++) {
				CompoundRecord record = mPoint[i].record;
				int index1 = (flagNo1 != -1) ? (record.isFlagSet(flagNo1) ? 0 : 1)
							: mShowEmptyInSplitView ? mTableModel.getCategoryIndex(mSplittingColumn[0], record)
							: mVisibleCategoryFromCategory[mSplittingColumn[0]][mTableModel.getCategoryIndex(mSplittingColumn[0], record)];
				if (index1 == -1) {
					mPoint[i].hvIndex = -1;
					continue;
					}
				int index2 = (flagNo2 != -1) ? (record.isFlagSet(flagNo2) ? 0 : 1)
							: mShowEmptyInSplitView ? mTableModel.getCategoryIndex(mSplittingColumn[1], record)
							: mVisibleCategoryFromCategory[mSplittingColumn[1]][mTableModel.getCategoryIndex(mSplittingColumn[1], record)];
				if (index2 == -1) {
					mPoint[i].hvIndex = -1;
					continue;
				}
				mPoint[i].hvIndex = index1 + index2 * count1;
				}
			}

		return true;
		}

	public Color getTitleBackground() {
		return mTitleBackground;
		}

	public void setTitleBackground(Color c) {
		if (!mTitleBackground.equals(c)) {
			mTitleBackground = c;
			invalidateOffImage(false);
			}
		}

	public Color getViewBackground() {
		return (mViewBackground == null) ? Color.WHITE : mViewBackground;
		}

	/**
	 * Defines the background color
	 * @param c (null for WHITE)
	 * @return whether there was a change
	 */
	public boolean setViewBackground(Color c) {
		if (Color.WHITE.equals(c))
			c = null;
		if ((c != null && !c.equals(mViewBackground))
		 || (c == null && mViewBackground != null)) {
			mViewBackground = c;
			invalidateOffImage(false);
			return true;
			}
		return false;
		}

	public Color getDefaultLabelBackground() {
		return mLabelBackgroundColor.getDefaultDataColor();
		}

	/**
	 * Defines whether to show background area and which color of the rectangular areas behind labels
	 * @param c color of background area
	 * @return whether there was a change
	 */
	public boolean setDefaultLabelBackground(Color c) {
		if (!c.equals(mLabelBackgroundColor.getDefaultDataColor())) {
			mLabelBackgroundColor.setDefaultDataColor(c);
			if (showAnyLabels())
				invalidateOffImage(false);
			return true;
			}
		return false;
		}

	@Override
	public boolean isShowLabelBackground() {
		return mShowLabelBackground;
		}

	/**
	 * Defines whether to show a rectangular background area behind any labels
	 * @param b whether to show a rectangular background
	 */
	public void setShowLabelBackground(boolean b) {
		if (mShowLabelBackground != b) {
			mShowLabelBackground = b;
			if (showAnyLabels())
				invalidateOffImage(false);
			}
		}

	@Override
	public float getLabelTransparency() {
		return mLabelBackgroundTransparency;
		}

	@Override
	public void setLabelTransparency(float transparency, boolean isAdjusting) {
		if (mLabelBackgroundTransparency != transparency) {
			mLabelBackgroundTransparency = transparency;
			if (showAnyLabels())
				invalidateOffImage(false);
			}
		}

	/**
	 * Generates a neutral grey with given contrast to the current background.
	 * @param contrast 0.0 (not visible) to 1.0 (full contrast)
	 * @return
	 */
	public Color getContrastGrey(float contrast) {
		return getContrastGrey(contrast, mViewBackground);
		}

	/**
	 * Generates a neutral grey with given contrast to given color.
	 * @param contrast 0.0 (not visible) to 1.0 (full contrast)
	 * @return
	 */
	protected Color getContrastGrey(float contrast, Color color) {
		float brightness = ColorHelper.perceivedBrightness(color);

		if (contrast == 1f)
			return (brightness > 0.5) ? Color.BLACK : Color.WHITE;

		float range = (brightness > 0.5) ? brightness : 1f-brightness;

		// enhance contrast for middle bright backgrounds
		contrast = (float)Math.pow(contrast, range);

		return (brightness > 0.5) ?
				  Color.getHSBColor(0.0f, 0.0f, brightness - range*contrast)
				: Color.getHSBColor(0.0f, 0.0f, brightness + range*contrast);
		}

	public void setFocusList(int listIndex) {
		if (mFocusList != listIndex) {
			mFocusList = listIndex;
				invalidateOffImage(mChartType.updateCoordsOnFocusOrSelectionChange());    // Color count in mChartInfo needs to be updated
			}
		}

	public int getFontSizeMode() {
		return mFontSizeMode;
		}

	/**
	 * This is the user defined relative font size factor applied to marker and scale labels.
	 * Note: Marker labels are also affected by setMarkerLabelSize().
	 * @param size
	 * @param isAdjusting
	 */
	public void setFontSize(float size, int mode, boolean isAdjusting) {
		if (mRelativeFontSize != size || mFontSizeMode != mode) {
			mRelativeFontSize = size;
			mFontSizeMode = mode;
			invalidateOffImage(true);
			}
		}

	/**
	 *
	 * @param width
	 * @param height
	 * @param dpiFactor for printing in higher resolution (dpi/75)
	 * @param retinaFactor
	 * @return
	 */
	protected int calculateFontSize(int width, int height, float dpiFactor, float retinaFactor, boolean isScreen) {
		float fontSize = mRelativeFontSize * cAbsoluteDefaultFontSize;

		if (mFontSizeMode == cFontSizeModeAbsolute) {
			fontSize *= dpiFactor * retinaFactor;
			}
		else {
			// retinaFactor is already in width and height!
			float relativeViewSize = (float)Math.sqrt(width * height)
					/ (dpiFactor * (isScreen ? HiDPIHelper.scale(cFontRefenceViewSize) : cFontRefenceViewSize));

			if (mFontSizeMode == cFontSizeModeRelative)
				fontSize *= dpiFactor * relativeViewSize;
			else    // adaptive
				fontSize *= dpiFactor * (float)Math.sqrt(relativeViewSize);
			}

		return isScreen ? HiDPIHelper.scale(fontSize) : Math.round(fontSize);
		}

	/**
	 * This defines the amount of random displacement on one,two or three axes
	 * @param jittering relative displacement amount
	 * @param axes combination of 1,2,4 for x,y,z axes
	 * @param isAdjusting
	 */
	public void setJittering(float jittering, int axes, boolean isAdjusting) {
		if (axes == 0)
			axes = (mDimensions == 3) ? 7 : 3;
		if (mMarkerJittering != jittering || mMarkerJitteringAxes != axes) {
			mMarkerJittering = jittering;
			mMarkerJitteringAxes = axes;
			invalidateOffImage(true);
			}
		}

	public void setCaseSeparation(int column, float value, boolean isAdjusting) {
		if (column >= 0
		 && (!mTableModel.isColumnTypeCategory(column)
		  || mTableModel.getCategoryCount(column) > cMaxCaseSeparationCategoryCount))
			column = cColumnUnassigned;

		if (mCaseSeparationColumn != column
		 || (mCaseSeparationColumn != cColumnUnassigned && mCaseSeparationValue != value)) {
			mCaseSeparationColumn = column;
			mCaseSeparationValue = value;
		   	validateExclusion(determineChartType());
			invalidateOffImage(true);
			}
		}

	/**
	 * Returns whether the user selected one or two category columns for view splitting.
	 * If columns are selected, but the vast number of categories is preventing splitting,
	 * then this returns true!
	 * @return whether the view is configured for view splitting
	 */
	public boolean isSplitViewConfigured() {
		return mSplittingColumn[0] != cColumnUnassigned;
		}

	/**
	 * Returns whether the user selected one or two category columns for view splitting
	 * and if the number of view categories don't exceed the maximum for rendering.
	 * @return whether split views are rendered
	 */
	public boolean isSplitView() {
		return mSplittingColumn[0] != cColumnUnassigned && !mSplitViewCountExceeded;
		}

	public boolean isShowEmptyInSplitView() {
		return mShowEmptyInSplitView;
		}

	public VisualizationSplitter getSplitter() {
		return mSplitter;
		}

	public int[] getSplittingColumns() {
		return mSplittingColumn;
		}

	public float getSplittingAspectRatio() {
		return mSplittingAspectRatio;
		}

	public void setSplittingColumns(int column1, int column2, float aspectRatio, boolean showEmptyViews) {
		if (column1 == cColumnUnassigned
		 && column2 != cColumnUnassigned) {
			column1 = column2;
			column2 = cColumnUnassigned;
			}

		if (mSplittingColumn[0] != column1
		 || mSplittingColumn[1] != column2
		 || (mSplittingColumn[0] != cColumnUnassigned && mSplittingAspectRatio != aspectRatio)
		 || (mSplittingColumn[0] != cColumnUnassigned && mShowEmptyInSplitView != showEmptyViews)) {
			if ((column1 == cColumnUnassigned
			  || mTableModel.isColumnTypeCategory(column1)
			  || mTableModel.getListHandler().getListIndex(column1) != CompoundTableListHandler.LISTINDEX_NONE)
			 && (column2 == cColumnUnassigned
			  || mTableModel.isColumnTypeCategory(column2)
			  || mTableModel.getListHandler().getListIndex(column2) != CompoundTableListHandler.LISTINDEX_NONE)) {
				mSplittingColumn[0] = column1;
				mSplittingColumn[1] = column2;
				mSplittingAspectRatio = aspectRatio;
				mShowEmptyInSplitView = showEmptyViews;
				invalidateSplittingIndices();
				}
			}
		}

		/** Determine current mean/median setting for box and whisker plots
		 * @return mode or BOXPLOT_DEFAULT_MEAN_MODE if not box/whisker plot
		 */
	public int getBoxplotMeanMode() {
		return mChartType.isDistributionPlot() ? mBoxplotMeanMode : BOXPLOT_DEFAULT_MEAN_MODE;
		}

	/**
	 * Defines whether mean and/or median are have indicators in box/whisker plots.
	 * @param mode
	 */
	public void setBoxplotMeanMode(int mode) {
		if (mBoxplotMeanMode != mode) {
			mBoxplotMeanMode = mode;
			invalidateOffImage(false);
			}
		}

	/** Determines whether bar/pie size values are shown in a current bar/pie chart.
	 *  Returns false if the current plot is neither bar nor pie chart.
	 * @return
	 */
	public boolean isShowBarOrPieSizeValue() {
		return mChartType.supportsShowMeanAndMedian()
			&& mShowBarOrPieSizeValues;
	}

	/**
	 * Defines whether mean and/or median are shown in box/whisker plots.
	 * @param b
	 */
	public void setShowBarOrPieSizeValue(boolean b) {
		if (mShowBarOrPieSizeValues != b) {
			mShowBarOrPieSizeValues = b;
			if (mChartType.supportsShowBarOrPieSizeValues())
				invalidateOffImage(true);
		}
	}

	/** Determines whether mean and/or median values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot. 
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowMeanAndMedianValues() {
		return mChartType.supportsShowMeanAndMedian()
			&& mShowMeanAndMedianValues;
		}

	/**
	 * Defines whether mean and/or median are shown in box/whisker plots.
	 * @param b
	 */
	public void setShowMeanAndMedianValues(boolean b) {
		if (mShowMeanAndMedianValues != b) {
			mShowMeanAndMedianValues = b;
			if (mChartType.supportsShowMeanAndMedian())
				invalidateOffImage(true);
			}
		}

	/**
	 * Determines whether the standard deviation is shown in a current box or whisker plot.
	 * @return true if current graph box or whisker plot and standard deviation is shown
	 */
	public boolean isShowStandardDeviation() {
		return mChartType.supportsShowStdDevAndErrorMergin()
			  && mShowStandardDeviation;
		}

	/**
	 * Sets whether the standard deviation is shown in box/whisker plots.
	 * @param b
	 */
	public void setShowStandardDeviation(boolean b) {
		if (mShowStandardDeviation != b) {
			mShowStandardDeviation = b;
			if (mChartType.supportsShowStdDevAndErrorMergin())
				invalidateOffImage(true);
			}
		}

	/**
	 * Determines whether the 95% confidence interval is shown in a current box or whisker plot.
	 * @return true if current graph box or whisker plot and standard deviation is shown
	 */
	public boolean isShowConfidenceInterval() {
		return mChartType.supportsShowStdDevAndErrorMergin()
			  && mShowConfidenceInterval;
		}

	/**
	 * Sets whether the 95% confidence interval is shown in box/whisker plots.
	 * @param b
	 */
	public void setShowConfidenceInterval(boolean b) {
		if (mShowConfidenceInterval != b) {
			mShowConfidenceInterval = b;
			if (mChartType.supportsShowStdDevAndErrorMergin())
				invalidateOffImage(true);
			}
		}

	/**
	 * Determines whether the value count N is shown in a current box or whisker plot.
	 * @return true if current graph box or whisker plot and standard deviation is shown
	 */
	public boolean isShowValueCount() {
		return mChartType.supportsShowValueCount()
			&& mShowValueCount;
		}

	/**
	 * Sets whether the value count N is shown in box/whisker plots.
	 * @param b
	 */
	public void setShowValueCount(boolean b) {
		if (mShowValueCount != b) {
			mShowValueCount = b;
			if (mChartType.supportsShowValueCount())
				invalidateOffImage(true);
			}
		}

	/** Determines whether p-values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot
	 *  or if no proper p-value column is assigned.
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowPValue() {
		return mChartType.supportsPValues()
	   		&& mBoxplotShowPValue
   			&& isValidPValueColumn(mPValueColumn);
   		}

	/**
	 * Defines whether p-values are shown in box/whisker plots.
	 * For showing p-values one also needs to call setPValueColumn()
	 * @param b
	 */
	public void setShowPValue(boolean b) {
		if (mBoxplotShowPValue != b) {
			mBoxplotShowPValue = b;
			if (isValidPValueColumn(mPValueColumn))
				invalidateOffImage(true);	// to recalculate p-values
			}
		}

	/** Determines whether fold-change values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot
	 *  or if no proper p-value column is assigned.
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowFoldChange() {
		return mChartType.supportsPValues()
	   		&& mBoxplotShowFoldChange
   			&& isValidPValueColumn(mPValueColumn);
   		}

	/**
	 * Defines whether fold-change values are shown in box/whisker plots.
	 * For showing fold-change values one also needs to call setPValueColumn()
	 * @param b
	 */
	public void setShowFoldChange(boolean b) {
		if (mBoxplotShowFoldChange != b) {
			mBoxplotShowFoldChange = b;
			if (isValidPValueColumn(mPValueColumn))
				invalidateOffImage(true);	// to recalculate fold change
			}
		}

	/**
	 * Returns the currently applied p-value column.
	 * @return
	 */
	public int getPValueColumn() {
		return isValidPValueColumn(mPValueColumn) ? mPValueColumn : cColumnUnassigned;
		}

	/**
	 * Returns the currently applied reference category for p-value calculation.
	 * @return
	 */
	public String getPValueRefCategory() {
		return (getPValueColumn() == cColumnUnassigned) ? null : mPValueRefCategory;
		}

	public boolean isValidPValueColumn(int column) {
		if (column == cColumnUnassigned)
			return false;

		if (mChartType.supportsPValues()) {
			if (column == getCaseSeparationColumn())
				return true;

			if (isSplitView()
			 && (column == mSplittingColumn[0]
			  || column == mSplittingColumn[1]))
				return true;

			for (int axis=0; axis<mDimensions; axis++)
				if (column == mAxisIndex[axis]
				 && getCategoryVisCount(axis) >= 2
				 && (mChartInfo == null || axis != mChartInfo.getDoubleAxis()))
					return true;
			}

		return false;
		}

	public void setPValueColumn(int column, String refCategory) {
		if (column == cColumnUnassigned)
			refCategory = null;
		else if (refCategory == null)
			column = cColumnUnassigned;

		if (column != mPValueColumn
		 || (refCategory != null && !refCategory.equals(mPValueRefCategory))) {
			mPValueColumn = column;
			mPValueRefCategory = refCategory;
			invalidateOffImage(true);
			}
		}

	/**
	 * In fast rendering mode antialiasing is switched off
	 * @return whether we are in fast render mode
	 */
	public boolean isFastRendering() {
		return mIsFastRendering;
		}

	/**
	 * In fast rendering mode antialiasing is switched off
	 * @param v
	 */
	public void setFastRendering(boolean v) {
		if (mIsFastRendering != v) {
			mIsFastRendering = v;
			invalidateOffImage(false);
			}
		}

	/**
	 * Determines the type of the drawn chart and visible ranges on all axes.
	 * This depends on the preferred chart type, on the columns being assigned
	 * to the axes, the number of existing categories within these columns,
	 * and the current pruning bar settings.
	 * If any of these change, then this method needs to be called.
	 * @return local exclusion update needs (EXCLUSION_FLAG_ZOOM_0 left shifted according to axis)
	 */
	private int determineChartType() {
		boolean[] wasCategoryAxis = new boolean[mDimensions];
		for (int axis=0; axis<mDimensions; axis++)
			wasCategoryAxis[axis] = mIsCategoryAxis[axis];
		int oldChartColumn = (mChartInfo == null || !mChartInfo.useProportionalFractions()) ? -1 : mChartType.getColumn();

		int chartType = ChartType.cTypeScatterPlot;	// scatter plot is the default that is always possible
		for (int axis=0; axis<mDimensions; axis++)
			mIsCategoryAxis[axis] = (mAxisIndex[axis] != cColumnUnassigned
								  && mTableModel.isColumnTypeCategory(mAxisIndex[axis])
								  && !mTableModel.isColumnTypeDouble(mAxisIndex[axis]));

		if (mPreferredChartType == ChartType.cTypeScatterPlot) {
			int csAxis = getCaseSeparationAxis();
			if (csAxis != -1)
				mIsCategoryAxis[csAxis] = true;
			}
		else if (ChartType.isDistributionPlot(mPreferredChartType)) {
			int boxPlotDoubleAxis = determineBoxPlotDoubleAxis();
			if (boxPlotDoubleAxis != -1) {
				chartType = mPreferredChartType;
				for (int axis=0; axis<mDimensions; axis++)
					mIsCategoryAxis[axis] = (axis != boxPlotDoubleAxis);
				}
			}
		else if (ChartType.isBarOrPieChart(mPreferredChartType)) {
			boolean allQualify = true;
			for (int axis=0; axis<mDimensions; axis++)
				if (!qualifiesAsChartCategory(axis))
					allQualify = false;

			if (allQualify) {
				int categoryCount = 1;
				for (int axis=0; axis<mDimensions; axis++)
					if (mAxisIndex[axis] != cColumnUnassigned)
						categoryCount *= mTableModel.getCategoryCount(mAxisIndex[axis]);

				if (categoryCount <= cMaxTotalChartCategoryCount) {
					chartType = mPreferredChartType;
					for (int axis=0; axis<mDimensions; axis++)
						mIsCategoryAxis[axis] = (mAxisIndex[axis] != cColumnUnassigned);
					}
				}
			}
		mChartType.setType(chartType);

		if (mTreeNodeList == null) {			// no tree view
			mActiveExclusionFlags = 0xFF;	// masks out NaN flags for axes that show categories
			for (int axis=0; axis<mDimensions; axis++)
				if (mIsCategoryAxis[axis])
					mActiveExclusionFlags &= ~(byte)(EXCLUSION_FLAG_NAN_0 << axis);

			if (!showProportionalBarOrPieFractions())
				mActiveExclusionFlags &= ~EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN;
			}
		else if (mTreeNodeList.length == 0) {	// we have an empty tree view
			mActiveExclusionFlags = 0;
			}
		else {									// tree view with at least one node
			mActiveExclusionFlags = EXCLUSION_FLAG_DETAIL_GRAPH;
			}

		int localExclusionNeeds = 0;
		for (int axis=0; axis<mDimensions; axis++) {
			if (mIsCategoryAxis[axis] != wasCategoryAxis[axis])
				localExclusionNeeds |= (EXCLUSION_FLAG_ZOOM_0 << axis);

			calculateVisibleDataRange(axis);
			}

		int newChartColumn = (mChartInfo == null || !mChartInfo.useProportionalFractions()) ? -1 : mChartType.getColumn();
		if (oldChartColumn != newChartColumn)
			localExclusionNeeds |= EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN;

		return localExclusionNeeds;
		}

	protected void determineWarningMessage() {
		if (mChartType.getType() != mPreferredChartType) {
			String preferred = ChartType.getName(mPreferredChartType);

			if (ChartType.isBarOrPieChart(mPreferredChartType)) {
				for (int axis=0; axis<mDimensions; axis++) {
					if (!qualifiesAsChartCategory(axis)) {
						if (mTableModel.isColumnTypeCategory(mAxisIndex[axis]))
							mWarningMessage = "A " + preferred + " is not shown, because an axis is assigned to a column containing too many categories!";
						else
							mWarningMessage = "A " + preferred + " is not shown, because an axis is assigned to a column that does not contain categories!";
						return;
						}
					}
				mWarningMessage = "A " + preferred + " is not shown, because the total number of displayed categories would be too high!";
				}
			else {
				mWarningMessage = "A " + preferred + " is not shown, because the current column-to-axes assignment is not compatible!";
				}
			}
		}

	/**
	 * Updates local pie/bar NaN exclusion in case of proportional pie/bar fractions
	 * @return true, if pie/bar NaN exclusion should be considered
	 *
	private boolean updateBarAndPieNaNExclusion() {
		boolean excludeNaN = mChartInfo.useProportionalFractions();

		for (int i=0; i<mDataPoints; i++) {
			if (excludeNaN && Float.isNaN(mPoint[i].record.getDouble(mChartColumn)))
				mPoint[i].exclusionFlags |= EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN;
			else
				mPoint[i].exclusionFlags &= ~EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN;
			}

		return excludeNaN;
		}*/

	protected boolean showProportionalBarOrPieFractions() {
		return ChartType.isBarOrPieChart(mPreferredChartType)
			&& mChartType.getMode() != ChartType.cModeCount
			&& mChartType.getMode() != ChartType.cModePercent
			&& mChartType.getMode() != ChartType.cModeCountLog
			&& mChartType.getColumn() != cColumnUnassigned
			&& !mTableModel.isDescriptorColumn(mChartType.getColumn());
		}

	/**
	 * Updates the NaN and zoom flags of local exclusion for all axes where needed
	 * and applies the local to the global exclusion, if local NaN or zoom flags were updated.
	 * @param localExclusionNeeds EXCLUSION_FLAG_NAN_0 and EXCLUSION_FLAG_ZOOM_0 left shifted according to axis
	 */
	private void validateExclusion(int localExclusionNeeds) {
		boolean found = false;
		for (int axis=0; axis<mDimensions; axis++) {
			if ((localExclusionNeeds & (EXCLUSION_FLAG_NAN_0 << axis)) != 0) {
				found = true;
				initializeLocalExclusion(axis);
				}
			if (mAxisIndex[axis] != cColumnUnassigned
			 && (localExclusionNeeds & (EXCLUSION_FLAG_ZOOM_0 << axis)) != 0) {
				found = true;
				updateLocalZoomExclusion(axis);
				}
			}
		if ((localExclusionNeeds & EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN) != 0) {
			found = true;
			initializeProportionalFractionExclusion();
			}
		if (found)
		   	applyLocalExclusion(false);
		}

	/**
	 * Determines based on table model information and current axis assignment,
	 * which axis is the preferred double value axis.
	 * @return 0,1,-1 for x-axis, y-axis, none
	 */
	public int determineBoxPlotDoubleAxis() {
		for (int i=0; i<mDimensions; i++)
			if (mAxisIndex[i] != cColumnUnassigned
			 && (mTableModel.isDescriptorColumn(mAxisIndex[i]) || mTableModel.isColumnTypeDouble(mAxisIndex[i]))
			 && !mTableModel.isColumnTypeCategory(mAxisIndex[i])
			 && qualifyOtherAsChartCategory(i))
				return i;

		int minCategoryCount = Integer.MAX_VALUE;
		int minCountAxis = -1;
		for (int i=0; i<mDimensions; i++) {
			if (mAxisIndex[i] != cColumnUnassigned
			 && mTableModel.isColumnTypeDouble(mAxisIndex[i])
			 && qualifyOtherAsChartCategory(i)) {
				int categoryCount = 1;
				for (int axis=0; axis<mDimensions; axis++)
					if (axis != i && mAxisIndex[axis] != cColumnUnassigned)
						categoryCount = mTableModel.getCategoryCount(mAxisIndex[axis]);

				if (minCategoryCount > categoryCount) {
					minCategoryCount = categoryCount;
					minCountAxis = i;
					}
				}
			}

		return minCountAxis;
		}

	private boolean qualifyOtherAsChartCategory(int axis) {
		for (int i=0; i<mDimensions; i++)
			if (axis != i && !qualifiesAsChartCategory(i))
				return false;
		return true;
		}

	protected boolean qualifiesAsChartCategory(int axis) {
		return (mAxisIndex[axis] == cColumnUnassigned
			 || (mTableModel.isColumnTypeCategory(mAxisIndex[axis])
			  && mTableModel.getCategoryCount(mAxisIndex[axis]) <= cMaxChartCategoryCount));
		}

	/**
	 * Calculates for numerical or date columns the pruning bar low and high values
	 * needed to exactly represent the given non-logarithmic data range.
	 * This method applies the margin concept used in calcDataMinAndMax(),
	 * where absolute margins shrink when zooming to keep relative margin sizes unchanged.
	 * For logarithmic columns passed data low and high values must be logarithmic.
	 * @param dataLow
	 * @param dataHigh
	 * @return
	 */
	protected double[] calculatePruningBarLowAndHigh(int axis, double dataLow, double dataHigh) {
		return calculateDataMinAndMax(axis).calculatePruningBarLowAndHigh(dataLow, dataHigh);
		}

	/**
	 * Calculates visual min and max values for numerical or date columns.
	 * These are the lowest and highest data values extended by some percent
	 * of the total range as added margin. For cyclic data or if explicit data low and high
	 * values exist, then no margin is added. In case of logarithmic columns returned
	 * values are also on a logarithmic scale.
	 * Note: The total data range depends on the zoom state, because the added margin
	 * shrinks with increasing zoom state to compensate and keep same size in view space!
	 * @param axis
	 * @return min and max values of total data range incl. margin or data range definition
	 */
	protected AxisDataRange calculateDataMinAndMax(int axis) {
		double margin = (mChartType.getMargin() == -1f) ? mScatterPlotMargin : mChartType.getMargin();
		return new AxisDataRange(mTableModel, mAxisIndex[axis], margin);
		}

	/**
	 * Calculates the visible range of the axis based on pruning bar settings
	 * and the full data range, which considers current graph type, margins,
	 * data range definitions, and logarithmic view mode.
	 * @param axis
	 * @return whether the visible range has been changed
	 */
	private boolean calculateVisibleDataRange(int axis) {
		double visMin = -0.5;
		double visMax =  0.5;
		boolean visRangeIsLog = false;
		int column = mAxisIndex[axis];
		if (column != cColumnUnassigned) {
			if (mIsCategoryAxis[axis]) {
				int categoryCount = mTableModel.getCategoryCount(column);
				visMin = Math.round(mPruningBarLow[axis] * categoryCount) - 0.5f;
				visMax = Math.round(mPruningBarHigh[axis] * categoryCount - 1f) + 0.5f;
				}
			else if (mTableModel.isDescriptorColumn(column)) {
				visMin = mPruningBarLow[axis];
				visMax = mPruningBarHigh[axis];
				}
			else {
				AxisDataRange adr = calculateDataMinAndMax(axis);
				double[] dataMinAndMax = adr.calculateScaledMinAndMax(mPruningBarLow[axis], mPruningBarHigh[axis]);
				double dataMin = dataMinAndMax[0];
				double dataMax = dataMinAndMax[1];
				double dataRange = dataMax - dataMin;
				visMin = (mPruningBarLow[axis] == 0.0f) ? dataMin : dataMin + mPruningBarLow[axis] * dataRange;
				visMax = (mPruningBarHigh[axis] == 1.0f) ? dataMax : dataMin + mPruningBarHigh[axis] * dataRange;
				visRangeIsLog = mTableModel.isLogarithmicViewMode(column);
				}
			}

		if (visMin != mAxisVisMin[axis]
		 || visMax != mAxisVisMax[axis]) {
			mAxisVisMin[axis] = visMin;
			mAxisVisMax[axis] = visMax;
			mAxisVisRangeIsLogarithmic[axis] = visRangeIsLog;
			return true;
			}

		return false;
		}

	/**
	 * @param axis
	 * @return percentage of visible data range on that axis between 0.0 and 1.0
	 */
	public double getRelativeVisibleRange(int axis) {
		return mPruningBarHigh[axis] - mPruningBarLow[axis];
		}

	/**
	 * @param axis
	 * @return value at the left/bottom end of visible range; logarithmic in case of logarithmic columns
	 */
	public double getVisibleMin(int axis) {
		return mAxisVisMin[axis];
		}

	/**
	 * @param axis
	 * @return value at the right/upper end of visible range; logarithmic in case of logarithmic columns
	 */
	public double getVisibleMax(int axis) {
		return mAxisVisMax[axis];
		}

	public boolean isLogarithmicAxis(int axis) {
		return mAxisVisRangeIsLogarithmic[axis];
		}

	/**
	 * For the given axis this method maps the axis column's full data range to the pruning bar
	 * range (0.0 to 1.0). It then determines the data value of the given record and axis column
	 * and translates it to the pruning bar range. If the axis is unused, then it returns 0.5.
	 * If the data value is NaN, then NaN is returned.
	 * @param axis
	 * @param record
	 * @return pruning bar value in range 0.0 to 1.0 reflecting the record's data
	 */
	public double calcPruningBarMappedValue(CompoundRecord record, int axis) {
		int column = mAxisIndex[axis];
		if (column == cColumnUnassigned)
			return 0.5;

		double value = getAxisValue(record, axis);
		if (Double.isNaN(value))
			return Double.NaN;

		if (mTableModel.isDescriptorColumn(column))
			return value;
		if (mIsCategoryAxis[axis])
			return Math.round(value + 0.5) / (double)mTableModel.getCategoryCount(column);

		if (mTableModel.isLogarithmicViewMode(column) != mAxisVisRangeIsLogarithmic[axis]) {
			if (mAxisVisRangeIsLogarithmic[axis])
				value = Math.pow(10, value);
			else
				value = Math.log10(value);
			}

		AxisDataRange adr = calculateDataMinAndMax(axis);
		double[] scaledMaxAndMin = adr.calculateScaledMinAndMax(mPruningBarLow[axis], mPruningBarHigh[axis]);
		return Math.min(1f, Math.max(0.0, (value - scaledMaxAndMin[0]) / (scaledMaxAndMin[1] - scaledMaxAndMin[0])));
		}

	private void updateZoomState() {
		double zoomFactor = 1.0f;
		int axisCount = 0;
		for (int axis=0; axis<mDimensions; axis++) {
			if (mAxisIndex[axis] != cColumnUnassigned) {
				if (mIsCategoryAxis[axis]) {
					zoomFactor *= (Math.max(1.0, mAxisVisMax[axis] - mAxisVisMin[axis]) / mTableModel.getCategoryCount(mAxisIndex[axis]));
					axisCount++;
					}
				else {
					zoomFactor *= Math.max(0.001, mPruningBarHigh[axis] - mPruningBarLow[axis]);
					axisCount++;
					}
				}
			}
		zoomFactor = Math.pow(zoomFactor, 1.0 / (float)axisCount);
		mZoomState = Math.min(100f, 1f / (float)zoomFactor);
		}

	public void calculateCategoryCounts(int boxPlotDoubleAxis) {
		if (isCaseSeparationDone())
			mCaseSeparationCategoryCount = mTableModel.getCategoryCount(mCaseSeparationColumn);
		else
			mCaseSeparationCategoryCount = 1;

		mCombinedCategoryCount = new int[1+mDimensions];
		mCombinedCategoryCount[0] = mCaseSeparationCategoryCount;
		for (int axis=0; axis<mDimensions; axis++) {
			mCombinedCategoryCount[axis + 1] = mCombinedCategoryCount[axis];
			if (axis != boxPlotDoubleAxis)
				mCombinedCategoryCount[axis + 1] *= getCategoryVisCount(axis);
			}

		mFullCombinedCategoryCount = new int[1+mDimensions];
		mFullCombinedCategoryCount[0] = mCaseSeparationCategoryCount;
		for (int axis=0; axis<mDimensions; axis++) {
			int column = mAxisIndex[axis];
			mFullCombinedCategoryCount[axis + 1] = mFullCombinedCategoryCount[axis];
			if (column != cColumnUnassigned && axis != boxPlotDoubleAxis)
				mFullCombinedCategoryCount[axis + 1] *= mTableModel.getCategoryCount(column);
			}
		}

	public int getColorIndex(VisualizationPoint vp, int colorListLength, int focusFlagNo) {
		int colorIndex = (mUseAsFilterFlagNo != -1 && !mTableModel.isRowFlagSuspended(mUseAsFilterFlagNo)
						&& !vp.record.isFlagSet(mUseAsFilterFlagNo)) ? colorListLength+1
					   : (vp.record.isSelected() && mFocusList != FocusableView.cFocusOnSelection) ?
						 colorListLength : vp.markerColorIndex;

		if (focusFlagNo != -1 && !vp.record.isFlagSet(focusFlagNo))
			colorIndex += colorListLength+2;

		return colorIndex;
		}

	public boolean isNaNInBarsOrPies(VisualizationPoint vp) {
		if (isNaNOnAxis(vp))
			return true;

		if (!mChartType.isSimpleMode()
		 && Float.isNaN(vp.record.getDouble(mChartType.getColumn())))
			return true;

		if (mChartType.getType() == ChartType.cTypeBars
		 && mMarkerSizeColumn != cColumnUnassigned
		 && Float.isNaN(vp.record.getDouble(mMarkerSizeColumn)))
			return true;

		return false;
		}

	public boolean isVisibleInBarsOrPies(VisualizationPoint vp) {
		if (!isVisibleExcludeNaN(vp))
			return false;

		if (!mChartType.isSimpleMode()
		 && Float.isNaN(vp.record.getDouble(mChartType.getColumn())))
			return false;

		if (mChartType.getType() == ChartType.cTypeBars
		 && mMarkerSizeColumn != cColumnUnassigned
		 && Float.isNaN(vp.record.getDouble(mMarkerSizeColumn)))
			return false;

		return true;
		}

	public String getStatisticalValues() {
		if (mChartType.isScatterPlot())
			return "Incompatible chart type.";

		String[][] categoryList = new String[6][];
		int[] categoryColumn = new int[6];
		int categoryColumnCount = 0;

		if (isSplitView()) {
			for (int i=0; i<2; i++) {
				if (mSplittingColumn[i] != cColumnUnassigned) {
					categoryColumn[categoryColumnCount] = mSplittingColumn[i];
					categoryList[categoryColumnCount] = mTableModel.getCategoryList(mSplittingColumn[i]);
					categoryColumnCount++;
					}
				}
			}
		for (int axis=0; axis<mDimensions; axis++) {
			if (mIsCategoryAxis[axis]) {
				int categoryCount = getCategoryVisCount(axis);
				categoryColumn[categoryColumnCount] = mAxisIndex[axis];
				categoryList[categoryColumnCount] = new String[categoryCount];
				if (mAxisIndex[axis] == cColumnUnassigned) { // box/whisker plot with unassigned category axis
					categoryList[categoryColumnCount][0] = "<All Rows>";
					}
				else {
					String[] list = mTableModel.getCategoryList(categoryColumn[categoryColumnCount]);
					for (int j=0; j<categoryCount; j++)
						categoryList[categoryColumnCount][j] = list[getCategoryVisMin(axis)+j];
					}
				categoryColumnCount++;
				}
			}
		if (isCaseSeparationDone()) {
			categoryColumn[categoryColumnCount] = mCaseSeparationColumn;
			categoryList[categoryColumnCount] = mTableModel.getCategoryList(mCaseSeparationColumn);
			categoryColumnCount++;
			}

		boolean includePValue = false;
		boolean includeFoldChange = false;
		int pValueColumn = getPValueColumn();
		int referenceCategoryIndex = -1;
		if (pValueColumn != cColumnUnassigned) {
			referenceCategoryIndex = getCategoryIndex(pValueColumn, mPValueRefCategory);
			if (referenceCategoryIndex != -1) {
				includePValue = mBoxplotShowPValue;
				includeFoldChange = mBoxplotShowFoldChange;
				}
			}

		StringWriter stringWriter = new StringWriter(1024);
		BufferedWriter writer = new BufferedWriter(stringWriter);

		try {
			// construct the title line
			for (int i=0; i<categoryColumnCount; i++) {
				String columnTitle = (categoryColumn[i] == -1) ? "Category" : mTableModel.getColumnTitleWithSpecialType(categoryColumn[i]);
				writer.append(columnTitle+"\t");
				}

			if (!mChartType.supportsOutliers())
				writer.append("Rows in Category");

			if (mChartType.isBarOrPieChart()
			 && mChartType.getMode() != ChartType.cModeCount) {
				String name = mTableModel.getColumnTitleWithSpecialType(mChartType.getColumn());
				writer.append(mChartType.getModeText(name));
				if (mChartType.getMode() != ChartType.cModePercent && mChartType.getMode() != ChartType.cModeCountLog) {
					boolean isLogarithmic = mTableModel.isLogarithmicViewMode(mChartType.getColumn());
					writer.append(isLogarithmic ? "\tStandard Deviation (geom.)" : "\tStandard Deviation");
					writer.append("\tConfidence Interval (95%)");
					}
				}

			if (mChartType.isDistributionPlot()) {
				boolean isLogarithmic = mTableModel.isLogarithmicViewMode(mAxisIndex[mChartInfo.getDoubleAxis()]);
				if (mChartType.supportsOutliers()) {
					writer.append("Total Count");
					writer.append("\tOutlier Count");
					}
				writer.append(isLogarithmic ? "\tMean (geom.)" : "\tMean Value");
				writer.append("\t1st Quartile");
				writer.append("\tMedian");
				writer.append("\t3rd Quartile");
				writer.append("\tLower Adjacent Limit");
				writer.append("\tUpper Adjacent Limit");
				writer.append(isLogarithmic ? "\tStandard Deviation (geom.)" : "\tStandard Deviation");
				writer.append("\tConfidence Interval (95%)");
/*				if (includeFoldChange || includePValue)		don't use additional column
					writer.append("\tIs Reference Group");	*/
				if (includeFoldChange)
					writer.append(isLogarithmic ? "\tlog2(Fold Change)" : "\tFold Change");
				if (includePValue)
					writer.append("\tp-Value");
				}
			writer.newLine();
		
			int[] categoryIndex = new int[6];
			while (categoryIndex[0] < categoryList[0].length) {
				int columnIndex = 0;
				int hv = 0;
				if (isSplitView()) {
					if (mSplittingColumn[0] != cColumnUnassigned)
						hv += categoryIndex[columnIndex++];
					if (mSplittingColumn[1] != cColumnUnassigned)
						hv += categoryIndex[columnIndex++] * categoryList[0].length;
					}

				int cat = 0;
				for (int axis=0; axis<mDimensions; axis++)
					if (mIsCategoryAxis[axis])
						cat += categoryIndex[columnIndex++] * mCombinedCategoryCount[axis];

				if (isCaseSeparationDone())
					cat += categoryIndex[columnIndex++];

				if (mChartInfo.getPointsInCategory(hv, cat) != 0) {
					for (int i=0; i<categoryColumnCount; i++) {
						writer.append(categoryList[i][categoryIndex[i]]);
						if ((includeFoldChange || includePValue)
						 && pValueColumn==categoryColumn[i]
						 && mPValueRefCategory.equals(categoryList[i][categoryIndex[i]]))
							writer.append(" (ref)");
						writer.append("\t");
						}
			
					if (!mChartType.supportsOutliers())
						writer.append(""+mChartInfo.getPointsInCategory(hv, cat));

					if (mChartType.isBarOrPieChart()
					 && mChartType.getMode() != ChartType.cModeCount) {
						writer.append("\t" + formatValue(mChartInfo.getBarValue(hv, cat), mChartType.getColumn()));
						if (mChartType.getMode() != ChartType.cModePercent && mChartType.getMode() != ChartType.cModeCountLog) {
							writer.append("\t"+formatValue(mChartInfo.getStdDev(hv, cat), mChartType.getColumn()));
							writer.append("\t"+formatValue(mChartInfo.getMean(hv, cat)-mChartInfo.getErrorMargin(hv, cat), mChartType.getColumn())
										  +"-"+formatValue(mChartInfo.getMean(hv, cat)+mChartInfo.getErrorMargin(hv, cat), mChartType.getColumn()));
							}
						}

					if (mChartType.isDistributionPlot()) {
						AbstractDistributionPlot vi = (AbstractDistributionPlot)mChartInfo;
						if (mChartType.supportsOutliers()) {
							writer.append(Integer.toString(mChartInfo.getPointsInCategory(hv, cat)+vi.getOutlierCount(hv, cat)));
							writer.append("\t").append(String.valueOf(vi.getOutlierCount(hv, cat)));
							}
						int column = mAxisIndex[mChartInfo.getDoubleAxis()];
						writer.append("\t").append(formatValue(vi.getMean(hv, cat), column));
						writer.append("\t").append(formatValue(vi.getBoxQ1(hv, cat), column));
						writer.append("\t").append(formatValue(vi.getMedian(hv, cat), column));
						writer.append("\t").append(formatValue(vi.getBoxQ3(hv, cat), column));
						writer.append("\t").append(formatValue(vi.getBoxLAV(hv, cat), column));
						writer.append("\t").append(formatValue(vi.getBoxUAV(hv, cat), column));
						writer.append("\t").append(formatValue(vi.getStdDev(hv, cat), column));
						writer.append("\t").append(formatValue(vi.getMean(hv, cat) - vi.getErrorMargin(hv, cat), column)).append("-").append(formatValue(vi.getMean(hv, cat) + vi.getErrorMargin(hv, cat), column));
/*						if (includeFoldChange || includePValue) {		don't use additional column
							int refHV = getReferenceHV(hv, pValueColumn, referenceCategoryIndex);
							int refCat = getReferenceCat(cat, pValueColumn, referenceCategoryIndex, new int[1+mDimensions]);
							writer.append("\t"+((hv==refHV && cat==refCat) ? "yes" : "no"));
							}	*/
						if (includeFoldChange) {
							writer.append("\t");
							if (!Float.isNaN(vi.getFoldChange(hv, cat)))
								writer.append(new DecimalFormat("#.#####").format(vi.getFoldChange(hv, cat)));
							}
						if (includePValue) {
							writer.append("\t");
							if (!Float.isNaN(vi.getPValue(hv, cat)))
								writer.append(new DecimalFormat("#.#####").format(vi.getPValue(hv, cat)));
							}
						}
					writer.newLine();
					}
	
				// update category indices for next row
				for (int i=categoryColumnCount-1; i>=0; i--) {
					if (++categoryIndex[i] < categoryList[i].length || i == 0)
						break;

					categoryIndex[i] = 0;
					}
				}
			writer.close();
			}
		catch (IOException ioe) {}

		return stringWriter.toString();
		}

	/**
	 * Formats a numerical value for displaying it.
	 * This includes proper rounding and potential de-logarithmization of the original value.
	 * @param value is the logarithm, if the column is in logarithmic view mode
	 * @param column the column this value refers to
	 * @return
	 */
	protected String formatValue(float value, int column) {
		if (mTableModel.isLogarithmicViewMode(column) && !Float.isNaN(value) && !Float.isInfinite(value))
			value = (float)Math.pow(10, value);
		return DoubleFormat.toString(value);
		}

	/**
	 * If axis != -1, then this method returns getAxisValue(record, axis), which is
	 * the correct value to apply, when positioning a VisualizationPoint on an axis.
	 * Otherwise, this method returns record.getDouble(column).
	 * @param record
	 * @param axis -1 if column is given
	 * @param column -1 if axis is given
	 * @return
	 */
	public float getValue(CompoundRecord record, int axis, int column) {
		return (column != -1) ? record.getDouble(column) : getAxisValue(record, axis);
		}

	/**
	 * Returns the correct value to apply, when positioning a VisualizationPoint
	 * on an axis. This method resolves whether we have a dynamic value (e.g. from
	 * a descriptor similarity calculation) or a static value from the CompoundRecord.
	 * With ambiguous column types (category and double) it also considers, whether
	 * to use the category index or the double value.
	 * It does not, however, consider case separation on this axis!!!
	 * @param record
	 * @param axis existing axis assigned to existing column
	 * @return
	 */
	public float getAxisValue(CompoundRecord record, int axis) {
		int column = mAxisIndex[axis];
		return mTableModel.isDescriptorColumn(column) ?
				(mAxisSimilarity[axis] == null ? 0.5f : mAxisSimilarity[axis][record.getID()])
			  : (mIsCategoryAxis[axis]) ? mTableModel.getCategoryIndex(column, record) : record.getDouble(column);
		}

	protected TreeMap<byte[],VisualizationPoint> createReferenceMap(int referencedColumn) {
		// create map of existing and referenced VisualizationPoints
		TreeMap<byte[],VisualizationPoint> map = new TreeMap<>(new ByteArrayComparator());
		for (VisualizationPoint vp:mPoint) {
			byte[] key = (byte[])vp.record.getData(referencedColumn);
			if (key != null)
				map.put(key, vp);
			}

		return map;
		}

	private TreeMap<VisualizationPoint,LineConnection[]> createReverseConnectionMap(int referencingColumn) {
		TreeMap<VisualizationPoint,LineConnection[]> map = new TreeMap<VisualizationPoint,LineConnection[]>(new VisualizationPointComparator());

		int keyColumn = mTableModel.findColumn(mTableModel.getColumnProperty(referencingColumn, CompoundTableConstants.cColumnPropertyReferencedColumn));
		int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(referencingColumn, CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
		float min = 0;
		float max = 0;
		float dif = 0;
		if (strengthColumn != -1) {
			min = mTableModel.getMinimumValue(strengthColumn);
			max = mTableModel.getMaximumValue(strengthColumn);
			if (max == min) {
				strengthColumn = -1;
				}
			else {
				min -= 0.2f * (max - min);
				dif = max - min;
				}
			}

		for (VisualizationPoint vp:mPoint) {
			byte[] key = (byte[])vp.record.getData(keyColumn);
			if (key == null)
				continue;

			byte[] data = (byte[])vp.record.getData(referencingColumn);
			if (data != null) {
				String[] keyEntry = mTableModel.separateEntries(new String(data));
				float[] strength = null;
				data = (byte[])vp.record.getData(strengthColumn);
				if (data != null) {
					String[] strengthEntry = mTableModel.separateEntries(new String(data));
					if (strengthEntry.length == keyEntry.length) {
						strength = new float[strengthEntry.length];
						for (int i=0; i<strength.length; i++) {
							try {
								float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strengthEntry[i], strengthColumn)));
								strength[i] = Float.isNaN(value) ? 0.0f : (value - min) / dif;
								}
							catch (NumberFormatException nfe) {}
							}
						}
					}

				for (int i=0; i<keyEntry.length; i++) {
					VisualizationPoint vp1 = mConnectionLineMap.get(key);
					VisualizationPoint vp2 = mConnectionLineMap.get(keyEntry[i].getBytes());
					if (vp2 != null) {
						LineConnection[] connection = map.get(vp2);
						if (connection == null) {
							connection = new LineConnection[1];
							connection[0] = new LineConnection(vp1, strength == null ? 1.0f : strength[i]);
							}
						else {
							LineConnection[] old = connection;
							connection = new LineConnection[old.length+1];
							for (int j=0; j<old.length; j++)
								connection[j] = old[j];
							connection[old.length] = new LineConnection(vp1, strength == null ? 1.0f : strength[i]);
							}
						map.put(vp2, connection);
						}
					}
				}
			}
		return map;
		}

	public boolean isUsedAsFilter() {
		return mUseAsFilterFlagNo != -1;
	}

	public void setUseAsFilter(boolean b) {
		if (b == (mUseAsFilterFlagNo == -1)) {
			if (!b) {
				mTableModel.setRowFlagToDirty(mUseAsFilterFlagNo);
				mTableModel.freeRowFlag(mUseAsFilterFlagNo);
				mUseAsFilterFlagNo = -1;
				}
			else {
				mUseAsFilterFlagNo = mTableModel.getUnusedRowFlag(true);
				mTableModel.setRowFlagSuspension(mUseAsFilterFlagNo, true); // works only after selection
				mTableModel.updateExternalExclusion(mUseAsFilterFlagNo, false, true);
				}
			}
		}

	public int getLocalExclusionList() {
		return mLocalExclusionList;
	}

	public void setLocalExclusionList(int listNo) {
		if (listNo != mLocalExclusionList) {
			mLocalExclusionList = listNo;
			invalidateOffImage(false);
		}
	}

	public boolean isIgnoreGlobalExclusion() {
		return mIsIgnoreGlobalExclusion;
		}

	public void setIgnoreGlobalExclusion(boolean b) {
		if (b != mIsIgnoreGlobalExclusion) {
			mIsIgnoreGlobalExclusion = b;
			invalidateOffImage(false);
			}
		}

	public void resetUseAsFilter() {
		if (mUseAsFilterFlagNo != -1) {
			mTableModel.setRowFlagToDirty(mUseAsFilterFlagNo);
			mTableModel.clearRowFlag(mUseAsFilterFlagNo);
			}
		}

	public boolean getShowNaNValues() {
		return mShowNaNValues;
		}

	public void setShowNaNValues(boolean b) {
		if (mShowNaNValues != b) {
			mShowNaNValues = b;
			applyLocalExclusion(false);
			invalidateOffImage(true);
			}
		}

	public boolean isDynamicScale() {
		return mIsDynamicScale;
		}

	public void setDynamicScale(boolean b) {
		if (mIsDynamicScale != b) {
			mIsDynamicScale = b;
			invalidateOffImage(true);
			}
		}

	@Override
	public int[] getMarkerLabelOnePerCategory() {
		if (mOnePerCategoryLabelCategoryColumn == cColumnUnassigned
		 || mOnePerCategoryLabelValueColumn == cColumnUnassigned)
			return null;

		int[] opc = new int[3];
		opc[0] = mOnePerCategoryLabelCategoryColumn;
		opc[1] = mOnePerCategoryLabelValueColumn;
		opc[2] = mOnePerCategoryLabelMode;
		return opc;
		}

	@Override
	public void setMarkerLabelOnePerCategory(int categoryColumn, int valueColumn, int mode) {
		if (mOnePerCategoryLabelCategoryColumn != categoryColumn
		 || mOnePerCategoryLabelValueColumn != valueColumn
		 || mOnePerCategoryLabelMode != mode) {
			if ((categoryColumn == cColumnUnassigned || mTableModel.isColumnTypeCategory(categoryColumn))
			 && (valueColumn == cColumnUnassigned || mTableModel.isColumnTypeDouble(valueColumn))) {
				mOnePerCategoryLabelCategoryColumn = categoryColumn;
				mOnePerCategoryLabelValueColumn = valueColumn;
				mOnePerCategoryLabelMode = mode;
				invalidateOffImage(false);
				}
			}
		}

	protected TreeMap<byte[],VisualizationPoint> buildOnePerCategoryMap() {
		if (mOnePerCategoryLabelCategoryColumn == cColumnUnassigned
		 || mOnePerCategoryLabelValueColumn == cColumnUnassigned)
			return null;

		// TODO use better 'magic' algorithm to distribute labels
		// if (mOnePerCategoryLabelMode == cOPCModeMagic)
		//     return buildMagicOnePerCategoryMap();

		TreeMap<byte[],VisualizationPoint> oneLabelPerCategoryMap = new TreeMap<>(new ByteArrayComparator());
		double min = mTableModel.getMinimumValue(mOnePerCategoryLabelValueColumn);
		double max = mTableModel.getMaximumValue(mOnePerCategoryLabelValueColumn);
		for (int i=0; i<mDimensions; i++) {
			if (mOnePerCategoryLabelValueColumn == mAxisIndex[i]) {
				min = mAxisVisMin[i];
				max = mAxisVisMax[i];
				break;
				}
			}

		// magicValue contains for every category a different value mapping
		// the categories in natural order onto the visible value span
		double[] magicValue = null;
		if (mOnePerCategoryLabelMode == cOPCModeMagic) {
			int visCatCount = calculateVisibleCategoryCount(mOnePerCategoryLabelCategoryColumn);
			if (visCatCount == 0)
				return null;
			magicValue = new double[visCatCount];
			for (int cat=0; cat<mTableModel.getCategoryCount(mOnePerCategoryLabelCategoryColumn); cat++) {
				int visCat = mVisibleCategoryFromCategory[mOnePerCategoryLabelCategoryColumn][cat];
				if (visCat != -1) {
					double catSpan = (max - min) / visCatCount;
					magicValue[visCat] = min + (0.5 + visCat) * catSpan;
					}
				}
			}

		double targetValue = (mOnePerCategoryLabelMode == cOPCModeHighest) ? max
						   : (mOnePerCategoryLabelMode == cOPCModeAverage) ? (max + min) / 2.0 : min;
		for (VisualizationPoint vp:mPoint) {
			if (isVisible(vp)) {
				// don't consider VPs that have no reasonable label
				boolean hasLabel = false;
				for (int i=0; i<mLabelColumn.length; i++) {
					if (mLabelColumn[i] != cColumnUnassigned
					 && vp.record.getData(mLabelColumn[i]) != null
					 && (!mTableModel.isColumnTypeDouble(mLabelColumn[i]) || !Float.isNaN(vp.record.getDouble(mLabelColumn[i])))) {
						hasLabel = true;
						break;
						}
					}

				if (hasLabel) {
					float value = vp.record.getDouble(mOnePerCategoryLabelValueColumn);
					if (mOnePerCategoryLabelMode == cOPCModeMagic) {
						int cat = mTableModel.getCategoryIndex(mOnePerCategoryLabelCategoryColumn, vp.record);
						int visCat = mVisibleCategoryFromCategory[mOnePerCategoryLabelCategoryColumn][cat];
						targetValue = magicValue[visCat];
						}

					byte[] category = (byte[])vp.record.getData(mOnePerCategoryLabelCategoryColumn);
					VisualizationPoint ovp = oneLabelPerCategoryMap.get(category);
					if (ovp == null
					 || Math.abs(targetValue - ovp.record.getDouble(mOnePerCategoryLabelValueColumn)) > Math.abs(targetValue - value))
						oneLabelPerCategoryMap.put(category, vp);
					}
				}
			}

		return oneLabelPerCategoryMap;
		}

	public int getGridMode() {
		return mGridMode;
		}

	public void setGridMode(int gridMode) {
		if (mGridMode != gridMode) {
			mGridMode = gridMode;
			invalidateOffImage(false);
			}
		}

	public boolean isLegendSuppressed() {
		return mSuppressLegend;
	}

	public void setSuppressLegend(boolean hideLegend) {
		if (mSuppressLegend != hideLegend) {
			mSuppressLegend = hideLegend;
			invalidateOffImage(true);
		}
	}

	public int getScaleMode() {
		return mScaleMode;
		}

	public void setScaleMode(int scaleMode) {
		if (mScaleMode != scaleMode) {
			mScaleMode = scaleMode;
			invalidateOffImage(true);
			}
		}

	public int getScaleStyle() {
		return mScaleStyle;
	}

	public void setScaleStyle(int scaleStyle) {
		if (mScaleStyle != scaleStyle) {
			mScaleStyle = scaleStyle;
			invalidateOffImage(true);
		}
	}

	public static float getDefaultScatterplotMargin() {
		return 0.5f * MAX_SCATTERPLOT_MARGIN;
		}

	public float getScatterPlotMargin() {
		return mScatterPlotMargin;
		}

	public void setScatterPlotMargin(float margin) {
		if (mScatterPlotMargin != margin) {
			mScatterPlotMargin = margin;
			if (mChartType.isScatterPlot()) {
				for (int i = 0; i<mDimensions; i++)
					updateVisibleRange(i, mPruningBarLow[i], mPruningBarHigh[i], false);
				invalidateOffImage(true);
				}
			}
		}

	public int getConnectionColumn() {
		// filter out connection types being incompatible with chart type
		if (mChartType.isDistributionPlot()) {
			if (mConnectionColumn == cConnectionColumnConnectCases
			 || (mCaseSeparationCategoryCount != 1 && mConnectionColumn == mCaseSeparationColumn))
				return mConnectionColumn;
			for (int i=0; i<mDimensions; i++)
				if (mConnectionColumn == mAxisIndex[i])
					return mConnectionColumn;
			return cColumnUnassigned;
			}
		else {
			return mConnectionColumn != cConnectionColumnConnectCases ? mConnectionColumn : cColumnUnassigned;
			}
		}

	public int getConnectionOrderColumn() {
		return (mConnectionColumn == cColumnUnassigned
			 || mChartType.isDistributionPlot()) ?
					 cColumnUnassigned : mConnectionOrderColumn;
		}

	public void setConnectionColumns(int column, int orderColumn) {
		if (column != cColumnUnassigned
		 && column != cConnectionColumnConnectAll
		 && column != cConnectionColumnConnectCases
		 && !mTableModel.isColumnTypeCategory(column)
		 && mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn) == null)
			column = cColumnUnassigned;

		if (column == cColumnUnassigned
		 || column == cConnectionColumnConnectCases
		 ||	(column >= 0 && mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn) != null))
			orderColumn = cColumnUnassigned;

		if (mConnectionColumn != column || mConnectionOrderColumn != orderColumn) {
			invalidateConnectionLines();
			mConnectionColumn = column;
			mConnectionOrderColumn = orderColumn;
			invalidateOffImage(false);
			updateTreeViewGraph();
			}
		}

	public void setConnectionLineListMode(int mode, int list1, int list2) {
		if (mode == 0) {
			list1 = -1;
			list2 = -1;
			}
		if (mConnectionLineListMode != mode
		 || mConnectionLineList1 != list1
		 || mConnectionLineList2 != list2) {
			mConnectionLineListMode = mode;
			mConnectionLineList1 = list1;
			mConnectionLineList2 = list2;
			invalidateOffImage(false);
			}
		}

	public int getConnectionLineListMode() {
		return mConnectionLineListMode;
		}

	public int getConnectionLineList1() {
		return mConnectionLineList1;
		}

	public int getConnectionLineList2() {
		return mConnectionLineList2;
		}

	public void setConnectionLineWidth(float width, boolean isAdjusting) {
		width = Math.min(4f, width);
		if (mRelativeConnectionLineWidth != width) {
			mRelativeConnectionLineWidth = width;
			invalidateOffImage(false);
			}
		}

	public float getConnectionLineWidth() {
		return mRelativeConnectionLineWidth;
		}

	public boolean isConnectionLineInverted() {
		return (mIsConnectionLineInverted
			 && mConnectionColumn != cColumnUnassigned
			 && mConnectionColumn != cConnectionColumnConnectAll
			 && mConnectionColumn != cConnectionColumnConnectCases
			 && mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn) != null
			 && CompoundTableConstants.cColumnPropertyReferenceTypeTopDown.equals(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType)));
		}

	public void setConnectionLineInversion(boolean isInverted) {
		if (mIsConnectionLineInverted != isInverted) {
			mIsConnectionLineInverted = isInverted;
			if (mConnectionColumn != cColumnUnassigned
				&& mConnectionColumn != cConnectionColumnConnectAll
				&& mConnectionColumn != cConnectionColumnConnectCases
				&& mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn) != null
				&& CompoundTableConstants.cColumnPropertyReferenceTypeTopDown.equals(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType)))
				invalidateOffImage(false);
			}
		}

	private void invalidateConnectionLines() {
		mConnectionLinePoint = null;
		mConnectionLineMap = null;
		mReverseConnectionMap = null;
		}

	public void setMarkerSize(float size, boolean isAdjusting) {
		if (mRelativeMarkerSize != size) {
			mRelativeMarkerSize = size;

			// if no connection lines are drawn then keep line width synchronized with marker size for potential use
			if (mConnectionColumn == cColumnUnassigned)
				mRelativeConnectionLineWidth = mRelativeMarkerSize;

			invalidateOffImage(true);
			}
		}

	public void setMarkerSizeColumn(int column, float min, float max) {
		if (mMarkerSizeColumn == cColumnUnassigned) {
			min = Float.NaN;
			max = Float.NaN;
			}
		else if ((!mTableModel.isColumnTypeDouble(column) && !mTableModel.isDescriptorColumn(column))
			  || (!Float.isNaN(min) && !Float.isNaN(max) && min >= max)
			  || (!Float.isNaN(min) && mTableModel.isLogarithmicViewMode(column) && min <= 0.0)
			  || (!Float.isNaN(max) && mTableModel.isLogarithmicViewMode(column) && max <= 0.0)) {
			min = Float.NaN;
			max = Float.NaN;
			}
		if (mMarkerSizeColumn != column || mMarkerSizeMin != min || mMarkerSizeMax != max) {
			mMarkerSizeColumn = column;
			mMarkerSizeMin = min;
			mMarkerSizeMax = max;
			updateSimilarityMarkerSizes(-1);
			invalidateOffImage(true);
			}
		}

	public void setMarkerSizeInversion(boolean inversion) {
		if (mMarkerSizeInversion != inversion) {
			mMarkerSizeInversion = inversion;
			invalidateOffImage(false);
			}
		}

	public void setMarkerSizeProportional(boolean proportional) {
		if (mMarkerSizeProportional != proportional) {
			mMarkerSizeProportional = proportional;
			invalidateOffImage(false);
			}
		}

	public void setMarkerSizeZoomAdaption(boolean adapt) {
		if (Float.isNaN(mMarkerSizeZoomAdaption) == adapt) {
			if (adapt)
				mMarkerSizeZoomAdaption = 1f + (mZoomState * MARKER_ZOOM_ADAPTION_FACTOR) - MARKER_ZOOM_ADAPTION_FACTOR;
			else
				mMarkerSizeZoomAdaption = Float.NaN;
			invalidateOffImage(false);
			}
		}

	public void setMarkerLabelSize(float size, boolean isAdjusting) {
		if (mMarkerLabelSize != size) {
			mMarkerLabelSize = size;
			if (showAnyLabels()) {
				invalidateOffImage(false);
				}
			}
		}

	@Override
	public void setMarkerLabelsBlackOrWhite(boolean blackAndWhite) {
		if (mIsMarkerLabelsBlackAndWhite != blackAndWhite) {
			mIsMarkerLabelsBlackAndWhite = blackAndWhite;
			if (showAnyLabels()) {
				invalidateOffImage(false);
				}
			}
		}

	@Override
	public boolean isMarkerLabelBlackOrWhite() {
		return mIsMarkerLabelsBlackAndWhite;
		}

	@Override
	public void setOptimizeLabelPositions(boolean optimize) {
		if (mOptimizeLabelPositions != optimize) {
			mOptimizeLabelPositions = optimize;
			if (showAnyLabels()) {
				invalidateOffImage(false);
				}
			}
		}

	@Override
	public boolean isOptimizeLabelPositions() {
		return mOptimizeLabelPositions;
		}

	public void setMarkerShapeColumn(int column) {
		if (column >= 0
		 && (!mTableModel.isColumnTypeCategory(column)
		  || mTableModel.getCategoryCount(column) > getAvailableShapeCount()))
			column = cColumnUnassigned;

		if (mMarkerShapeColumn != column) {
			mMarkerShapeColumn = column;
			updateShapeIndices();
			}
		}
	
	private void updateShapeIndices() {
		if (mMarkerShapeColumn == cColumnUnassigned)
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = 0;
		else if (CompoundTableListHandler.isListColumn(mMarkerShapeColumn)) {
			int flagNo = mTableModel.getListHandler().getListFlagNo(CompoundTableListHandler.convertToListIndex(mMarkerShapeColumn));
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = (byte)(mPoint[i].record.isFlagSet(flagNo) ? 0 : 1);
			}
		else {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = (byte)mTableModel.getCategoryIndex(mMarkerShapeColumn, mPoint[i].record);
			}

		invalidateOffImage(true);
		}

	public void setMarkerLabelsInTreeViewOnly(boolean inTreeViewOnly) {
		mLabelsInTreeViewOnly = inTreeViewOnly;
		invalidateOffImage(false);
		}

	public void addMarkerLabel(int position, int column) {
		if (mLabelColumn[position] != column) {
			if (mLabelColumn[position] != cColumnUnassigned)
				clearNonCustomLabelPositions(mLabelColumn[position]);

			mLabelColumn[position] = column;

			invalidateOffImage(false);
			}
		}

	public void setMarkerLabels(int[] columnAtPosition) {
		boolean[] columnToClear = new boolean[mTableModel.getTotalColumnCount()];
		for (int i=0; i<mLabelColumn.length; i++)
			if (mLabelColumn[i] != cColumnUnassigned)
				columnToClear[mLabelColumn[i]] = true;

		mLabelColumn = columnAtPosition;

		for (int i=0; i<mLabelColumn.length; i++)
			if (mLabelColumn[i] != cColumnUnassigned)
				columnToClear[mLabelColumn[i]] = false;

		for (int column=0; column<columnToClear.length; column++)
			if (columnToClear[column])
				clearNonCustomLabelPositions(column);

		invalidateOffImage(false);
		}

	public int getMarkerLabelList() {
		return mLabelList;
		}

	public void setMarkerLabelList(int listNo) {
		if (mLabelList != listNo) {
			mLabelList = listNo;
			invalidateOffImage(false);
			}
		}

	public boolean showAnyLabels() {
		if (!mLabelsInTreeViewOnly || isTreeViewGraph())
			for (int i=0; i<mLabelColumn.length; i++)
				if (mLabelColumn[i] != cColumnUnassigned)
					return true;

		return false;
		}

	public int getMarkerLabelColumn(int position) {
		return mLabelColumn[position];
		}

	public int getColumnIndex(int axis) {
		return mAxisIndex[axis];
		}

	/**
	 * Assigns the axis to the specified column or cColumnUnassigned.
	 * The chart type is updated and the visible range set to the maximum.
	 * Local record hiding of this axis is initialized and applied to the global
	 * exclusion.
	 * @param axis
	 * @param column
	 */
	public void setColumnIndex(int axis, int column) {
		if (mAxisIndex[axis] != column) {
			mAxisIndex[axis] = column;
			clearAllLabelPositions();
			initializeAxis(axis);
			int exclusionNeeeds = (EXCLUSION_FLAG_NAN_0 << axis) | determineChartType();
			validateExclusion(exclusionNeeeds);
			}
		}

	public abstract int[] getSupportedChartTypes();

	public ChartType getChartType() {
		return mChartType;
		}

	/**
	 * @return defined chart type, which may be different from the actually shown one
	 */
	public int getPreferredChartType() {
		return mPreferredChartType;
		}

	/**
	 * @return defined chart mode even if we don't show bar/pie chart
	 */
	public int getPreferredChartMode() {
		return mChartType.getMode();
		}

	/**
	 * @return defined chart column even if we don't show bar/pie chart or if they are in count/percent mode
	 */
	public int getPreferredChartColumn() {
		return mChartType.getColumn();
		}

	public void setPreferredChartType(int type, int mode, int column) {
		if (mode == -1)
			mode = ChartType.cModeCount;
		if (mode != ChartType.cModeCount && mode != ChartType.cModePercent && mode != ChartType.cModeCountLog && column == cColumnUnassigned)
			mode = ChartType.cModeCount;
		if (mPreferredChartType != type
		 || mChartType.getColumn() != column
		 || mChartType.getMode() != mode) {
			mChartType.setColumn(column);
			mChartType.setMode(mode);
			mPreferredChartType = type;
			int exclusionNeeeds = determineChartType();
			validateExclusion(exclusionNeeeds);
			updateTreeViewGraph();
			invalidateOffImage(true);
			}
		}

	public int getReferenceHV(int hv, int categoryColumn, int categoryIndex) {
		if (!isSplitView())
			return 0;

		if (mSplittingColumn[1] == cColumnUnassigned)
			return (mSplittingColumn[0] == categoryColumn) ? categoryIndex : hv;

		int categoryCount = mTableModel.getCategoryCount(mSplittingColumn[0]);
		if (mSplittingColumn[0] == categoryColumn) {
			int index2 = hv / categoryCount;
			return categoryIndex + index2 * categoryCount;
			}
		else {
			int index1 = hv % categoryCount;
			return index1 + categoryIndex * categoryCount;
			}
		}

	public int getReferenceCat(int cat, int categoryColumn, int categoryIndex, int[] individualIndex) {
		for (int i=mDimensions; i>0; i--) {
			individualIndex[i] = cat / mCombinedCategoryCount[i-1];
			cat -= individualIndex[i] * mCombinedCategoryCount[i-1];
			}
		individualIndex[0] = cat;
		
		if (mCaseSeparationCategoryCount != 1 && categoryColumn == mCaseSeparationColumn) {
			individualIndex[0] = categoryIndex;
			}
		else {
			for (int axis=0; axis<mDimensions; axis++) {
				if (categoryColumn == mAxisIndex[axis]) {
					individualIndex[axis+1] = categoryIndex;
					break;
					}
				}
			}

		int index = individualIndex[0];
		for (int axis=0; axis<mDimensions; axis++)
			index += individualIndex[axis+1] * mCombinedCategoryCount[axis];

		return index;
		}

	private int getCategoryVisMin(int axis) {
		return (int)Math.round(mAxisVisMin[axis] + 0.5f);
		}

	private int getCategoryVisMax(int axis) {
		return (int)Math.round(mAxisVisMax[axis] - 0.5f);
		}

	public int getCategoryVisCount(int axis) {
		return (int)Math.round(mAxisVisMax[axis] - mAxisVisMin[axis]);
		}

	/**
	 * Returns the category index on the axis, i.e. the visible(!) category list
	 * index of the visualization point. If the row belong to a category being
	 * zoomed out of the view, then -1 is returned.
	 * @param axis
	 * @param vp
	 * @return visible category index or -1
	 */
	protected int getCategoryIndex(int axis, VisualizationPoint vp) {
		if (mAxisIndex[axis] == cColumnUnassigned)
			return 0;

		int index = mTableModel.getCategoryIndex(mAxisIndex[axis], vp.record);

		return (index >= getCategoryVisMin(axis) && index <= getCategoryVisMax(axis)) ?
			index - getCategoryVisMin(axis) : -1;
		}

	/**
	 * Returns the index of value in the column's visible(!!!) category list.
	 * If the column is shown on an axis and if this axis is zoomed in, then
	 * this category index differs from the one of the CompoundTableModel.
	 * @param column
	 * @param value
	 * @return category index or -1 if the category is scrolled out of view
	 */
	public int getCategoryIndex(int column, String value) {
		if (column == mCaseSeparationColumn)
			return mTableModel.getCategoryIndex(column, value);

		if (column == mSplittingColumn[0]
		 || column == mSplittingColumn[1])
			return mSplitViewCountExceeded ? 0 : mTableModel.getCategoryIndex(column, value);

		int axis = -1;
		for (int i=0; i<mDimensions; i++) {
			if (mAxisIndex[i] == column) {
				axis = i;
				break;
				}
			}

		int index = mTableModel.getCategoryIndex(column, value);

		return (index >= getCategoryVisMin(axis) && index <= getCategoryVisMax(axis)) ?
			index - getCategoryVisMin(axis) : -1;
		}

	/**
	 * This method requires a valid chart type, which may be achieved by calling determineChartBasics()
	 * @param axis
	 * @return whether the data of the column assigned to this axis is considered category data
	 */
	public boolean isCategoryAxis(int axis) {
		return mIsCategoryAxis[axis];
		}

	/**
	 * Calculates one combined category index for a VisualizationPoint that
	 * includes all categories from case separation and visible(!) axis
	 * categories. If the vp is zoomed out of the view, then -1 is returned.
	 * @param vp
	 * @return
	 */
	public int getChartCategoryIndex(VisualizationPoint vp) {
		int index = (mCombinedCategoryCount[0] == 1) ?
				0 : mTableModel.getCategoryIndex(mCaseSeparationColumn, vp.record);

		for (int axis=0; axis<mDimensions; axis++) {
			if (!mChartType.isDistributionPlot()
		   	 || axis != mChartInfo.getDoubleAxis()) {
				int axisIndex = getCategoryIndex(axis, vp);
				if (axisIndex == -1)
					return -1;

				index += axisIndex * mCombinedCategoryCount[axis];
				}
			}

		return index;
		}

	/**
	 * Calculates one combined category index for a VisualizationPoint that
	 * includes all categories from case separation and all(!) axis
	 * categories.
	 * @param vp
	 * @return
	 */
	public int getFullCategoryIndex(VisualizationPoint vp) {
		int index = (mCombinedCategoryCount[0] == 1) ?
				0 : mTableModel.getCategoryIndex(mCaseSeparationColumn, vp.record);

		for (int axis=0; axis<mDimensions; axis++) {
			if (!mChartType.isDistributionPlot()
			  || axis != mChartInfo.getDoubleAxis()) {
				int column = mAxisIndex[axis];
				int axisIndex = (column == cColumnUnassigned) ? 0 : mTableModel.getCategoryIndex(column, vp.record);
				index += axisIndex * mFullCombinedCategoryCount[axis];
				}
			}

		return index;
		}

	public void mouseClicked(MouseEvent e) {
		if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			VisualizationPoint marker = findMarker(e.getX(), e.getY());
			if (mActivePoint != marker) {
				// don't allow root de-selection if we are in a dedicated tree view
				boolean isPureTreeView = isTreeViewGraph() && !mTreeViewShowAll;
				if (marker != null || !isPureTreeView)
					mTableModel.setActiveRow(marker==null? null : marker.record);
				}
			}
		}

	/**
	 * This is the default implementation of locating a marker from screen coordinates.
	 * It assumes a rectangular marker shape and relies on getDistanceToMarker().
	 * For complex marker shapes overwrite this method or getDistanceToMarker().
	 * @param x
	 * @param y
	 * @return
	 */
	public VisualizationPoint findMarker(int x, int y) {
		// inverted order to prefer markers that are in the front
		boolean searchLabels = showAnyLabels();
		mHighlightedLabelPosition = null;
		VisualizationPoint p = null;
		float minDistance = Float.MAX_VALUE;
		for (int i=mDataPoints-1; i>=0; i--) {
			if (isVisible(mPoint[i])) {
				float dvp = Float.MAX_VALUE;
				if (mLabelColumn[cMidCenter] == -1) {
					dvp = getDistanceToMarker(mPoint[i], x, y);
					if (dvp == 0)
						return mPoint[i];
					}
				if (searchLabels) {
					mHighlightedLabelPosition = mPoint[i].findLabel(x, y);
					if (mHighlightedLabelPosition != null)
						return mPoint[i];
					}
				if (dvp < 4 && minDistance > dvp) {
					p = mPoint[i];
					minDistance = dvp;
					}
				}
			}

		return p;
		}

	/**
	 * This method assumes a rectangular marker shape and uses the
	 * VisualizationPoint's width and height values.
	 * May be overwritten to support complex marker shapes.
	 * @param vp
	 * @param x
	 * @param y
	 * @return
	 */
	public float getDistanceToMarker(VisualizationPoint vp, int x, int y) {
		float dx = Math.abs(vp.screenX - x) - vp.widthOrAngle1 / 2f;
		float dy = Math.abs(vp.screenY - y) - vp.heightOrAngle2 / 2f;
		return Math.max(0, Math.max(dx, dy));
		}

	public void mousePressed(MouseEvent e) {
		mViewSelectionHelper.setSelectedView((CompoundTableView)getParent());

		mMouseX2 = e.getX();
		mMouseY2 = e.getY();
		mMouseIsDown = true;
		mMouseIsControlDown = e.isControlDown();

		if (System.getProperty("touch") != null) {
			new Thread(() -> {
					try {
						Thread.sleep(1000);
						}
					catch (InterruptedException ie) {}

					if (Math.abs(mMouseX2 - mMouseX1) < 5
					 && Math.abs(mMouseY2 - mMouseY1) < 5
					 && mMouseIsDown)
						SwingUtilities.invokeLater(() -> activateTouchFunction() );
				} ).start();
			}

		mDragMode = DRAG_MODE_NONE;
		if (e.isPopupTrigger()) {
			if (delayPopupMenu())
				startPopupTimer();
			else
				showPopupMenu();
			}
		if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == 0) {
			mAddingToSelection = e.isShiftDown();
			if (e.isControlDown()) {
				mDragMode = DRAG_MODE_TRANSLATE;
				}
			else if (e.isAltDown()) {
				mDragMode = DRAG_MODE_RECT_SELECT;
				}
			else if (mHighlightedLabelPosition != null) {
				mDragMode = DRAG_MODE_MOVE_LABEL;
				}
			else {
				mDragMode = DRAG_MODE_LASSO_SELECT;
				mLassoRegion = new Polygon();
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				}
			}
		}

	public void mouseReleased(MouseEvent e) {
		mMouseIsDown = false;
		if (e.isPopupTrigger()) {
			if (showDelayedPopupMenu())
				showPopupMenu();
			}
		else if (mPopupThread != null) {
			mPopupThread = null;
			if (showDelayedPopupMenu())
				showPopupMenu();
			}

		if (mDragMode == DRAG_MODE_RECT_SELECT) {
			int mouseX1,mouseX2,mouseY1,mouseY2;

			if (mMouseX1 < mMouseX2) {
				mouseX1 = mMouseX1;
				mouseX2 = mMouseX2;
				}
			else {
				mouseX1 = mMouseX2;
				mouseX2 = mMouseX1;
				}

			if (mMouseY1 < mMouseY2) {
				mouseY1 = mMouseY1;
				mouseY2 = mMouseY2;
				}
			else {
				mouseY1 = mMouseY2;
				mouseY2 = mMouseY1;
				}

			boolean isCustomFilter = (mUseAsFilterFlagNo != -1);
			boolean customSelectionFound = false;
			for (int i=0; i<mDataPoints; i++) {
				boolean isSelected = mPoint[i].screenX>=mouseX1
								  && mPoint[i].screenX<=mouseX2
								  && mPoint[i].screenY>=mouseY1
								  && mPoint[i].screenY<=mouseY2
								  && isVisible(mPoint[i]);
				if (isCustomFilter) {
					if (isSelected) {
						mPoint[i].record.clearFlag(mUseAsFilterFlagNo);
						customSelectionFound = true;
						}
					else if (!mAddingToSelection) {
						mPoint[i].record.setFlag(mUseAsFilterFlagNo);
						}
					}
				else {
					if (isSelected)
						mPoint[i].record.setSelection(true);
					else if (!mAddingToSelection)
						mPoint[i].record.setSelection(false);
					}
				}

			if (isCustomFilter) {
				mTableModel.setRowFlagSuspension(mUseAsFilterFlagNo, !customSelectionFound);
				mTableModel.updateExternalExclusion(mUseAsFilterFlagNo, false, customSelectionFound);
				}
			else
				mSelectionModel.invalidate();
			}
		else if (mDragMode == DRAG_MODE_LASSO_SELECT) {
			boolean isCustomFilter = (mUseAsFilterFlagNo != -1);
			boolean customSelectionFound = false;
			for (int i=0; i<mDataPoints; i++) {
				boolean isSelected = mLassoRegion.contains(mPoint[i].screenX, mPoint[i].screenY)
						&& isVisible(mPoint[i]);
				if (isCustomFilter) {
					if (isSelected) {
						mPoint[i].record.clearFlag(mUseAsFilterFlagNo);
						customSelectionFound = true;
						}
					else if (!mAddingToSelection) {
						mPoint[i].record.setFlag(mUseAsFilterFlagNo);
						}
					}
				else {
					if (isSelected)
						mPoint[i].record.setSelection(true);
					else if (!mAddingToSelection)
						mPoint[i].record.setSelection(false);
					}
				}

			if (isCustomFilter) {
				mTableModel.setRowFlagSuspension(mUseAsFilterFlagNo, !customSelectionFound);
				mTableModel.updateExternalExclusion(mUseAsFilterFlagNo, false, customSelectionFound);
				}
			else
				mSelectionModel.invalidate();
			}
		else if (mDragMode == DRAG_MODE_MOVE_LABEL) {
			updateHighlightedLabelPosition();
			invalidateOffImage(false);
			}

		if (mTouchFunctionActive) {
			mTouchFunctionActive = false;
			repaint();
			}

		mDragMode = DRAG_MODE_NONE;
		}

	private void activateTouchFunction() {
		if (!showPopupMenu()) {
			mTouchFunctionActive = true;
			repaint();
			}
		}

	protected boolean isTouchFunctionActive() {
		return mTouchFunctionActive;
		}

	protected boolean showPopupMenu() {
		if (mDetailPopupProvider != null) {
			CompoundRecord record = (mHighlightedPoint == null) ? null : mHighlightedPoint.record;
			if (record != null || showCrossHair()) {
				JPopupMenu popup = mDetailPopupProvider.createPopupMenu(record, (VisualizationPanel)getParent(), -1, mMouseIsControlDown);
				if (popup != null) {
					mPopupX = mMouseX1;
					mPopupY = mMouseY1;
					popup.show(this, mMouseX1, mMouseY1);
					return true;
					}
				}
			}

		return false;
		}

	/**
	 * May be overridden to delay popup menus in case a preferred alternative action
	 * may happen, e.g. dragging to rotate the view. After a short delay
	 * showDelayedPopupMenu() is called.
	 * @return true if popup menu shall be shown
	 */
	public boolean delayPopupMenu() {
		return false;
		}

	private void startPopupTimer() {
		mPopupThread = new Thread(() -> {
			try {
				Thread.sleep(RIGHT_MOUSE_POPUP_DELAY);
				if (showDelayedPopupMenu())
					SwingUtilities.invokeLater(() -> {
						if (mPopupThread != null)
							showPopupMenu();
					});
				} catch (InterruptedException ie) {}
			} );
		mPopupThread.start();
		}

	/**
	 * If delayPopupMenu() is overridden, this should also be overridden to decide,
	 * whether the delayed popup shall be shown or whether the alternative action took place.
	 * @return
	 */
	public boolean showDelayedPopupMenu() {
		return true;
		}

	public void mouseEntered(MouseEvent e) {
		mMouseIsDown = false;
		}

	public void mouseExited(MouseEvent e) {
		mMouseIsDown = false;
		}

	public void mouseMoved(MouseEvent e) {
		mMouseX1 = e.getX();
		mMouseY1 = e.getY();
		LabelPosition2D oldLabel = mHighlightedLabelPosition;
		VisualizationPoint marker = findMarker(e.getX(), e.getY());
		if (oldLabel != mHighlightedLabelPosition || showCrossHair())
			repaint();
		if (mHighlightedPoint != marker)
			mTableModel.setHighlightedRow((marker == null) ? null : marker.record);
		}

	public void mouseDragged(MouseEvent e) {
		if (mDragMode == DRAG_MODE_MOVE_LABEL) {
			mHighlightedLabelPosition.translate(e.getX() - mMouseX2, e.getY() - mMouseY2);
			repaint();
			}

		mMouseX2 = e.getX();
		mMouseY2 = e.getY();

		if (mDragMode == DRAG_MODE_RECT_SELECT) {
			repaint();
			}
		else if (mDragMode == DRAG_MODE_LASSO_SELECT) {
			if ((Math.abs(mMouseX2 - mLassoRegion.xpoints[mLassoRegion.npoints-1]) > 3)
			 || (Math.abs(mMouseY2 - mLassoRegion.ypoints[mLassoRegion.npoints-1]) > 3)) {
				mLassoRegion.npoints--;
				mLassoRegion.addPoint(mMouseX2, mMouseY2);
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				}

			repaint();
			}
		}

	public boolean showCrossHair() {
		return false;
		}

	@Override
	public Point getToolTipLocation(MouseEvent e) {
		VisualizationPoint vp = findMarker(e.getX(), e.getY());
		return (vp != null) ? new Point((int)vp.screenX, (int)vp.screenY) : null;
		}

	@Override
	public String getToolTipText(MouseEvent e) {
		VisualizationPoint vp = findMarker(e.getX(), e.getY());
		if (vp == null)
			return null;
		TreeSet<Integer> columnSet = new TreeSet<>();
		StringBuilder sb = new StringBuilder();
		for (int axis=0; axis<mDimensions; axis++)
			addTooltipRow(vp.record, mAxisIndex[axis], mAxisSimilarity[axis], columnSet, sb);

		addMarkerTooltips(vp, columnSet, sb);

		addLabelTooltips(vp, columnSet, sb);

		if (sb.length() == 0)
			return null;

		sb.append("</html>");
		return sb.toString();
		}

	protected void addMarkerTooltips(VisualizationPoint vp, TreeSet<Integer> columnSet, StringBuilder sb) {
		addTooltipRow(vp.record, mMarkerColor.getColorColumn(), null, columnSet, sb);
		addTooltipRow(vp.record, mMarkerSizeColumn, null, columnSet, sb);
		addTooltipRow(vp.record, mMarkerShapeColumn, null, columnSet, sb);
		addTooltipRow(vp.record, mChartType.getColumn(), null, columnSet, sb);
		}

	protected void addLabelTooltips(VisualizationPoint vp, TreeSet<Integer> columnSet, StringBuilder sb) {
		for (int column:mLabelColumn)
			if (column != cColumnUnassigned)
				addTooltipRow(vp.record, column, null, columnSet, sb);
		}

	protected void addTooltipRow(CompoundRecord record, int column, float[] similarity, TreeSet<Integer> columnSet, StringBuilder sb) {
		if (column != cColumnUnassigned && !columnSet.contains(column)) {
			columnSet.add(column);
			String title = null;
			String value = null;
			if (CompoundTableListHandler.isListColumn(column)) {
				int listIndex = CompoundTableListHandler.convertToListIndex(column);
				int flagNo = mTableModel.getListHandler().getListFlagNo(listIndex);
				title = record.isFlagSet(flagNo) ? "Member of '" : "Not member of '";
				value = mTableModel.getListHandler().getListName(listIndex);
				}
			else {
				title = getAxisTitle(column)+": ";
				if (mTableModel.isDescriptorColumn(column)) {
					if (similarity != null)
						value = DoubleFormat.toString(similarity[record.getID()]);
					else
						value = DoubleFormat.toString((mActivePoint == null) ? Double.NaN
								: mTableModel.getDescriptorSimilarity(mActivePoint.record, record, column));
					}
				else if (mTableModel.getColumnSpecialType(column) != null) {
					int idColumn = mTableModel.findColumn(mTableModel.getColumnProperty(column,
							CompoundTableConstants.cColumnPropertyRelatedIdentifierColumn));
					if (idColumn != -1)
						value = mTableModel.encodeData(record, idColumn);
					}
			   	else {
			   		value = mTableModel.getValue(record, column);
			   		}
		   		}
			if (value != null) {
				sb.append((sb.length() == 0) ? "<html>" : "<br>");
		   		sb.append(title);
		   		sb.append(value.length() > MAX_TOOLTIP_LENGTH ? value.substring(0, MAX_TOOLTIP_LENGTH)+"..." : value);
				}
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		boolean needsUpdate = false;
		int exclusionNeeds = 0;

		if (e.getType() == CompoundTableEvent.cChangeExcluded) {
			mVisibleCategoryFromCategory = null;
			if ((mSplittingColumn[0] >=0 || mSplittingColumn[1] >=0) && !mShowEmptyInSplitView)
				invalidateSplittingIndices();
			if (mTreeViewIsDynamic && mTreeNodeList != null) {
				updateTreeViewGraph();
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			int column = e.getColumn();
			if (mVisibleCategoryFromCategory != null)
				mVisibleCategoryFromCategory[column] = null;

			for (int axis=0; axis<mDimensions; axis++) {
				if (column == mAxisIndex[axis]) {
					if (mTableModel.isDescriptorColumn(column))
			   			setSimilarityValues(axis, e.getSpecifier());

				   	exclusionNeeds = ((EXCLUSION_FLAG_NAN_0 | EXCLUSION_FLAG_ZOOM_0) << axis);
					needsUpdate = true;
					}
				}
			if (mMarkerSizeColumn == column) {
		   		updateSimilarityMarkerSizes(e.getSpecifier());
				needsUpdate = true;
				}
			if (mMarkerShapeColumn == column) {
				if (!mTableModel.isColumnTypeCategory(column)
				 || mTableModel.getCategoryCount(column) > getAvailableShapeCount())
				 	mMarkerShapeColumn = cColumnUnassigned;
	   			updateShapeIndices();
		   		needsUpdate = true;
				}
			if (mCaseSeparationColumn == column) {
				if (!mTableModel.isColumnTypeCategory(column)
				 || mTableModel.getCategoryCount(column) > cMaxCaseSeparationCategoryCount)
					mCaseSeparationColumn = cColumnUnassigned;
				needsUpdate = true;
				}
			if (mOnePerCategoryLabelCategoryColumn == column) {
				if (!mTableModel.isColumnTypeCategory(column))
					mOnePerCategoryLabelCategoryColumn = cColumnUnassigned;
				needsUpdate = true;
				}
			if (mOnePerCategoryLabelValueColumn == column) {
				needsUpdate = true;
				}
			if (mSplittingColumn[0] == column
			 || mSplittingColumn[1] == column) {
				if (!mTableModel.isColumnTypeCategory(column)) {
					if (mSplittingColumn[0] == column) {
						mSplittingColumn[0] = mSplittingColumn[1];
						mSplittingColumn[1] = cColumnUnassigned;
						}
					else {
						mSplittingColumn[1] = cColumnUnassigned;
						}
					}
				
				invalidateSplittingIndices();
				}
			for (int i=0; i<mLabelColumn.length; i++) {
				if (mLabelColumn[i] == column)
					needsUpdate = true;
				}
			if (mConnectionColumn == column || mConnectionOrderColumn == column) {
				invalidateConnectionLines();
				needsUpdate = true;
				}
			}
		else if (e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cDeleteRows) {
			boolean recycle = (e.getType() != CompoundTableEvent.cAddRows || e.getSpecifier() != 0);	// otherwise we have inserted rows from row splitting
			initializeDataPoints(recycle, e.getType() == CompoundTableEvent.cDeleteRows);
			mVisibleCategoryFromCategory = null;
			for (int axis=0; axis<mDimensions; axis++) {
				int column = mAxisIndex[axis];
				if (column != cColumnUnassigned) {
				   	if (mTableModel.isDescriptorColumn(column))
				   		setSimilarityValues(axis, -1);

				   	exclusionNeeds |= ((EXCLUSION_FLAG_NAN_0 | EXCLUSION_FLAG_ZOOM_0) << axis);
					}
				}
			if (e.getType() == CompoundTableEvent.cAddRows && showProportionalBarOrPieFractions())
				exclusionNeeds |= EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN;

	   		updateSimilarityMarkerSizes(-1);
		   	if (mMarkerShapeColumn >= 0) {	// if not is unassigned or list
				if (!mTableModel.isColumnTypeCategory(mMarkerShapeColumn))
					mMarkerShapeColumn = cColumnUnassigned;
				updateShapeIndices();
				}
			if (mCaseSeparationColumn >= 0) {
				if (!mTableModel.isColumnTypeCategory(mCaseSeparationColumn)
				 || mTableModel.getCategoryCount(mCaseSeparationColumn) > cMaxCaseSeparationCategoryCount)
					mCaseSeparationColumn = cColumnUnassigned;
				}
			if (mOnePerCategoryLabelCategoryColumn >= 0) {
				if (!mTableModel.isColumnTypeCategory(mOnePerCategoryLabelCategoryColumn))
					mOnePerCategoryLabelCategoryColumn = cColumnUnassigned;
			}
			if (mSplittingColumn[0] >= 0
			 || mSplittingColumn[1] >= 0) {  // if not is unassigned or list
				if (mSplittingColumn[0] >= 0 && !mTableModel.isColumnTypeCategory(mSplittingColumn[0])) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				if (mSplittingColumn[1] >= 0 && !mTableModel.isColumnTypeCategory(mSplittingColumn[1])) {
					mSplittingColumn[1] = cColumnUnassigned;
					}
				invalidateSplittingIndices();
				}
			invalidateConnectionLines();
			needsUpdate = true;
			}
		else if (e.getType() == CompoundTableEvent.cAddColumns) {
			if (mVisibleCategoryFromCategory != null) {
				int[][] oldVisibleCategoryFromCategory = mVisibleCategoryFromCategory;
				mVisibleCategoryFromCategory = new int[mTableModel.getTotalColumnCount()][];
				for (int i=0; i<oldVisibleCategoryFromCategory.length; i++)
					mVisibleCategoryFromCategory[i] = oldVisibleCategoryFromCategory[i];
				}
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			if (mVisibleCategoryFromCategory != null) {
				int[][] oldVisibleCategoryFromCategory = mVisibleCategoryFromCategory;
				mVisibleCategoryFromCategory = new int[mTableModel.getTotalColumnCount()][];
				for (int i=0; i<columnMapping.length; i++)
					if (columnMapping[i] != -1)
						mVisibleCategoryFromCategory[columnMapping[i]] = oldVisibleCategoryFromCategory[i];
				}
			for (int i=mLegendList.size()-1; i>=0; i--) {
				int column = mLegendList.get(i).getColumn();
				if (column >= 0 && columnMapping[column] == cColumnUnassigned)
					needsUpdate = true;
				}
			if (mMarkerSizeColumn >= 0) {
				mMarkerSizeColumn = columnMapping[mMarkerSizeColumn];
				}
			if (mMarkerShapeColumn >= 0) {
				mMarkerShapeColumn = columnMapping[mMarkerShapeColumn];
				}
			if (mCaseSeparationColumn >= 0) {
				mCaseSeparationColumn = columnMapping[mCaseSeparationColumn];
				if (mCaseSeparationColumn == cColumnUnassigned)
					needsUpdate = true;
				}
			if (mSplittingColumn[0] >= 0) {
				mSplittingColumn[0] = columnMapping[mSplittingColumn[0]];
				boolean updateSplitting = (mSplittingColumn[0] == cColumnUnassigned);
				if (mSplittingColumn[1] >= 0) {
					mSplittingColumn[1] = columnMapping[mSplittingColumn[1]];
					if (mSplittingColumn[1] == cColumnUnassigned)
						updateSplitting = true;
					}
				if (mSplittingColumn[0] == cColumnUnassigned && mSplittingColumn[1] != -1) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				if (updateSplitting) {
					invalidateSplittingIndices();
					needsUpdate = true;
					}
				}
			if (mOnePerCategoryLabelCategoryColumn >= 0) {
				mOnePerCategoryLabelCategoryColumn = columnMapping[mOnePerCategoryLabelCategoryColumn];
				if (mOnePerCategoryLabelCategoryColumn == cColumnUnassigned)
					needsUpdate = true;
				}
			if (mOnePerCategoryLabelValueColumn >= 0) {
				mOnePerCategoryLabelValueColumn = columnMapping[mOnePerCategoryLabelValueColumn];
				if (mOnePerCategoryLabelValueColumn == cColumnUnassigned)
					needsUpdate = true;
				}
			if (mChartType.getColumn() >= 0) {
				mChartType.setColumn(columnMapping[mChartType.getColumn()]);
				if (mChartType.getColumn() == cColumnUnassigned) {
					mChartType.setMode(ChartType.cModeCount);
					needsUpdate = true;
					}
				}
			if (mConnectionColumn >= 0) {
				mConnectionColumn = columnMapping[mConnectionColumn];
				if (mConnectionColumn == cColumnUnassigned) {
					invalidateConnectionLines();
					needsUpdate = true;
					}
				}
			if (mConnectionOrderColumn != cColumnUnassigned) {
				mConnectionOrderColumn = columnMapping[mConnectionOrderColumn];
				if (mConnectionOrderColumn == cColumnUnassigned) {
					needsUpdate = true;
					}
				}
			for (int i=0; i<mLabelColumn.length; i++) {
				if (mLabelColumn[i] >= 0) {
					mLabelColumn[i] = columnMapping[mLabelColumn[i]];
					if (mLabelColumn[i] == cColumnUnassigned)
						needsUpdate = true;
					}
				}
			for (int i=0; i<mDataPoints; i++) {
				mPoint[i].remapLabelPositionColumns(columnMapping);
				}
			for (int axis=0; axis<mDimensions; axis++) {
				if (mAxisIndex[axis] != cColumnUnassigned) {
					mAxisIndex[axis] = columnMapping[mAxisIndex[axis]];
					if (mAxisIndex[axis] == cColumnUnassigned) {
						mAxisIndex[axis] = cColumnUnassigned;
						needsUpdate = true;
						initializeAxis(axis);
						exclusionNeeds = (EXCLUSION_FLAG_NAN_0 << axis);
						}
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			updateActiveRow();
			needsUpdate = true;
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			invalidateOffImage(false);
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnReference) {
			if (mConnectionColumn == e.getColumn()) {
				invalidateConnectionLines();
				updateTreeViewGraph();
				invalidateOffImage(false);
				}
			}

		for (VisualizationLegend legend:mLegendList)
			legend.compoundTableChanged(e);

		mMarkerColor.compoundTableChanged(e);

	   	validateExclusion(exclusionNeeds | determineChartType());

		if (needsUpdate)
			invalidateOffImage(true);
		}

	/**
	 * Removes all cached label positions for the given column.
	 * This does not include manually assigned label positions.
	 * @param column
	 */
	public void clearNonCustomLabelPositions(int column) {
		for (int i=0; i<mDataPoints; i++)
			mPoint[i].removeNonCustomLabelPosition(column);
		}

	/**
	 * Removes all cached label positions with no exception.
	 */
	public void clearAllLabelPositions() {
		for (int i=0; i<mDataPoints; i++)
			mPoint[i].labelPosition = null;
		}

	public void listChanged(CompoundTableListEvent e) {
		if (e.getType() == CompoundTableListEvent.cDelete) {
			if (mLocalExclusionList != CompoundTableListHandler.LISTINDEX_NONE) {
				if (mLocalExclusionList == e.getListIndex())
					setLocalExclusionList(CompoundTableListHandler.LISTINDEX_NONE);
				else if (mLocalExclusionList > e.getListIndex())
					mLocalExclusionList--;
				}
			if (mFocusList != FocusableView.cFocusNone) {
				if (mFocusList == e.getListIndex())
					setFocusList(FocusableView.cFocusNone);
				else if (mFocusList > e.getListIndex())
					mFocusList--;
				}
			if (mConnectionLineListMode != 0) {
				if (mConnectionLineList1 == e.getListIndex())
					setConnectionLineListMode(0, -1, -1);
				else if (mConnectionLineList1 > e.getListIndex())
					mConnectionLineList1--;
				if (mConnectionLineList2 == e.getListIndex())
					setConnectionLineListMode(0, -1, -1);
				else if (mConnectionLineList1 > e.getListIndex())
					mConnectionLineList1--;
				}
			if (mLabelList != cLabelsOnAllRows) {
				if (mLabelList == e.getListIndex())
					setMarkerLabelList(cLabelsOnAllRows);
				else if (mLabelList > e.getListIndex())
					mLabelList--;
				}
			if (CompoundTableListHandler.isListColumn(mMarkerSizeColumn)) {
				int listIndex = CompoundTableListHandler.convertToListIndex(mMarkerSizeColumn);
				if (e.getListIndex() == listIndex) {
					mMarkerSizeColumn = cColumnUnassigned;
					invalidateOffImage(false);
					}
				else if (listIndex > e.getListIndex()) {
					mMarkerSizeColumn = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			if (CompoundTableListHandler.isListColumn(mMarkerShapeColumn)) {
				int listIndex = CompoundTableListHandler.convertToListIndex(mMarkerShapeColumn);
				if (e.getListIndex() == listIndex) {
					mMarkerShapeColumn = cColumnUnassigned;
					for (int i=0; i<mDataPoints; i++)
						mPoint[i].shape = 0;
					invalidateOffImage(true);
					}
				else if (listIndex > e.getListIndex()) {
					mMarkerShapeColumn = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			if (CompoundTableListHandler.isListColumn(mCaseSeparationColumn)) {
				int listIndex = CompoundTableListHandler.convertToListIndex(mCaseSeparationColumn);
				if (e.getListIndex() == listIndex) {
					mCaseSeparationColumn = cColumnUnassigned;
					invalidateOffImage(true);
					}
				else if (listIndex > e.getListIndex()) {
					mCaseSeparationColumn = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			boolean splittingChanged = false;
			if (CompoundTableListHandler.isListColumn(mSplittingColumn[0])) {
				int listIndex = CompoundTableListHandler.convertToListIndex(mSplittingColumn[0]);
				if (e.getListIndex() == listIndex) {
					mSplittingColumn[0] = cColumnUnassigned;
					splittingChanged = true;
					}
				else if (listIndex > e.getListIndex()) {
					mSplittingColumn[0] = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			if (CompoundTableListHandler.isListColumn(mSplittingColumn[1])) {
				int listIndex = CompoundTableListHandler.convertToListIndex(mSplittingColumn[1]);
				if (e.getListIndex() == listIndex) {
					mSplittingColumn[1] = cColumnUnassigned;
					splittingChanged = true;
					}
				else if (listIndex > e.getListIndex()) {
					mSplittingColumn[1] = CompoundTableListHandler.getColumnFromList(listIndex-1);
					}
				}
			if (splittingChanged) {
				if (mSplittingColumn[0] == cColumnUnassigned
				 || mSplittingColumn[1] != cColumnUnassigned) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				invalidateSplittingIndices();
				}
			}
		else if (e.getType() == CompoundTableListEvent.cChange) {
			if (mLocalExclusionList == e.getListIndex()) {
				invalidateOffImage(false);
				}
			if (mFocusList == e.getListIndex()) {
				invalidateOffImage(false);
				}
			if (mLabelList == e.getListIndex()) {
				invalidateOffImage(false);
				}
			if (mConnectionLineListMode != 0
			 && (mConnectionLineList1 == e.getListIndex()
			  || mConnectionLineList2 == e.getListIndex())) {
				invalidateOffImage(false);
				}
			if (CompoundTableListHandler.isListColumn(mMarkerSizeColumn)) {
				int listIndex = CompoundTableListHandler.convertToListIndex(mMarkerSizeColumn);
				if (e.getListIndex() == listIndex) {
					invalidateOffImage(false);
					}
				}
			if (CompoundTableListHandler.isListColumn(mMarkerShapeColumn)) {
				int listIndex = CompoundTableListHandler.convertToListIndex(mMarkerShapeColumn);
				if (e.getListIndex() == listIndex) {
					int flagNo = mTableModel.getListHandler().getListFlagNo(listIndex);
					for (int i=0; i<mDataPoints; i++)
						mPoint[i].shape = (byte)(mPoint[i].record.isFlagSet(flagNo) ? 0 : 1);
					invalidateOffImage(false);
					}
				}
			if (CompoundTableListHandler.isListColumn(mCaseSeparationColumn)) {
				int listIndex = CompoundTableListHandler.convertToListIndex(mCaseSeparationColumn);
				if (e.getListIndex() == listIndex)
					invalidateOffImage(true);
				}
			if ((CompoundTableListHandler.isListColumn(mSplittingColumn[0])
			  && e.getListIndex() == CompoundTableListHandler.convertToListIndex(mSplittingColumn[0]))
			 || (CompoundTableListHandler.isListColumn(mSplittingColumn[1])
			  && e.getListIndex() == CompoundTableListHandler.convertToListIndex(mSplittingColumn[1]))) {
				invalidateSplittingIndices();
				}
			}

		mMarkerColor.listChanged(e);
		}

	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			if (mSplittingColumn[0] == CompoundTableListHandler.PSEUDO_COLUMN_SELECTION
			 || mSplittingColumn[1] == CompoundTableListHandler.PSEUDO_COLUMN_SELECTION)
				invalidateSplittingIndices();

			invalidateOffImage(mChartType.updateCoordsOnFocusOrSelectionChange());
			}
		}

	@Override
	public void highlightChanged(CompoundRecord record) {
		if (record != null
		 && (mHighlightedPoint == null || mHighlightedPoint.record != record)) {
			for (int i=0; i<mPoint.length; i++) {
				if (mPoint[i].record == record) {
					setHighlightedPoint(mPoint[i]);
					break;
					}
				}
			}
		else if (record == null && mHighlightedPoint != null) {
			setHighlightedPoint(null);
			}
		}

	public void updateVisibleRange(int axis, double low, double high, boolean isAdjusting) {
		if (axis < mDimensions && mAxisIndex[axis] != cColumnUnassigned) {
			mPruningBarLow[axis] = low;
			mPruningBarHigh[axis] = high;
			if (calculateVisibleDataRange(axis)) {
				updateLocalZoomExclusion(axis);
				applyLocalExclusion(isAdjusting);
				updateZoomState();
				if (!Float.isNaN(mMarkerSizeZoomAdaption))
					mMarkerSizeZoomAdaption = 1f + (mZoomState * MARKER_ZOOM_ADAPTION_FACTOR) - MARKER_ZOOM_ADAPTION_FACTOR;
				invalidateOffImage(true);
				}
			}
		}

	/**
	 * This is used by find marker, assuming that a marker covers a
	 * rectangular area. If the marker's area is not rectangular,
	 * then findMarker() should be overridden for a smooth handling.
	 * @param p
	 * @return
	 */
	protected float getMarkerWidth(VisualizationPoint p) {
		return mAbsoluteMarkerSize;
		}

	/**
	 * This is used by find marker, assuming that a marker covers a
	 * rectangular area. If the marker's area is not rectangular,
	 * then findMarker() should be overridden for a smooth handling.
	 * @param p
	 * @return
	 */
	protected float getMarkerHeight(VisualizationPoint p) {
		return mAbsoluteMarkerSize;
		}

	/**
	 * Resets the visible range of the axis to the tablemodel's
	 * min and max values and repaints.
	 * @param axis
	 */
	public void initializeAxis(int axis) {
		mPruningBarLow[axis] = 0.0f;
		mPruningBarHigh[axis] = 1.0f;

		mAxisSimilarity[axis] = null;

		int column = mAxisIndex[axis];

		if (column != cColumnUnassigned
		 && mTableModel.isDescriptorColumn(column))
			setSimilarityValues(axis, -1);

		invalidateOffImage(true);
		}

	/**
	 * @return whether this is a non excluded point in the table model neglecting this views filter flag
	 */
	protected boolean isVisibleInModel(VisualizationPoint point) {
		if (mLocalExclusionList != CompoundTableListHandler.LISTINDEX_NONE
				&& !point.record.isFlagSet(mTableModel.getListHandler().getListFlagNo(mLocalExclusionList)))
			return false;
		return mIsIgnoreGlobalExclusion || ((mUseAsFilterFlagNo == -1) ? mTableModel.isVisible(point.record)
				: mTableModel.isVisibleNeglecting(point.record, mUseAsFilterFlagNo));
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered visible, if the showNaNValue option is on.
	 * @param point
	 * @return
	 */
	public boolean isVisible(VisualizationPoint point) {
		return (point.exclusionFlags & (mShowNaNValues ? ~EXCLUSION_FLAGS_NAN : mActiveExclusionFlags)) == 0
			&& isVisibleInModel(point);
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered invisible, even if the showNaNValue option is on.
	 * @param point
	 * @return
	 */
	public boolean isVisibleExcludeNaN(VisualizationPoint point) {
		return (point.exclusionFlags & mActiveExclusionFlags) == 0
				&& isVisibleInModel(point);
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered visible, even if the showNaNValue option is off.
	 * @param point
	 * @return
	 */
	protected boolean isVisibleIncludeNaN(VisualizationPoint point) {
		return (point.exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0
				&& isVisibleInModel(point);
		}

	/**
	 * Checks, whether this visualization point is not zoomed out of this view
	 * and not invisible because of another view, but contains a NaN value
	 * on at least one axis that shows floating point values.
	 * @param point
	 * @return
	 */
	protected boolean isVisibleAndNaN(VisualizationPoint point) {
		return (point.exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0
			&& (point.exclusionFlags & EXCLUSION_FLAGS_NAN & mActiveExclusionFlags) != 0
			&& isVisibleInModel(point);
		}

	/**
	 * Checks, whether there is at least one axis showing floating point values
	 * (not as categories) where this point has a NaN value in the associated column.
	 * @param point
	 * @return
	 */
	public boolean isNaNOnAxis(VisualizationPoint point) {
		return (point.exclusionFlags & EXCLUSION_FLAGS_NAN & mActiveExclusionFlags) != 0;
		}

	/**
	 * Checks whether the current column to axis assigment, the current zoom state
	 * and the current show NaN state, and the current localAffectsGlobalExclusion
	 * cause rows to be hidden globally. Suspension of global row hiding (because
	 * a view is currently not visible) is not taken into acount.
	 * @return whether this view currently causes rows to be hidden globally
	 */
	public boolean isGloballyHidingRows() {
		return mIsGloballyHidingRows;
		}

	/**
	 * Sets axis related zoom flags to false and sets NaN flags depending on
	 * whether the value is NaN regardless whether the axis is showing floats or categories.
	 * @param axis
	 */
	private void initializeLocalExclusion(int axis) {
			// flags 0-2: set if invisible due to view zooming
			// flags 3-5: set if invisible because of empty data (applies only for non-category )
			// flag  6  : set if invisible because point is not part of currently shown detail graph
		int column = mAxisIndex[axis];
		byte nanFlag = (byte)(EXCLUSION_FLAG_NAN_0 << axis);
		byte bothFlags = (byte)((EXCLUSION_FLAG_ZOOM_0 | EXCLUSION_FLAG_NAN_0) << axis);

			// reset all flags and then
			// flag all records with empty data
		for (int i=0; i<mDataPoints; i++) {
			mPoint[i].exclusionFlags &= ~bothFlags;
			if (column != -1
			 && !mTableModel.isDescriptorColumn(column)
			 && Float.isNaN(mPoint[i].record.getDouble(column)))
				mPoint[i].exclusionFlags |= nanFlag;
			}
		}

	/**
	 * Sets proportional bar/pie fraction related NaN flags depending on whether the respective
	 * chart-column value is NaN regardless whether bars/pies show count or value proportional fractions.
	 */
	private void initializeProportionalFractionExclusion() {
		// flags 0-2: set if invisible due to view zooming
		// flags 3-5: set if invisible because of empty data (applies only for non-category )
		// flag  6  : set if invisible because point is not part of currently shown detail graph
		byte nanFlag = EXCLUSION_FLAG_PROPORTIONAL_FRACTION_NAN;
		boolean isProportional = showProportionalBarOrPieFractions()
				&& !mTableModel.isDescriptorColumn(mChartType.getColumn());

		// reset all flags and then
		// flag all records with empty data
		for (int i=0; i<mDataPoints; i++) {
			if (isProportional && Float.isNaN(mPoint[i].record.getDouble(mChartType.getColumn())))
				mPoint[i].exclusionFlags |= nanFlag;
			else
				mPoint[i].exclusionFlags &= (byte)~nanFlag;
		}
	}

	/**
	 * Updates the local exclusion flags of non-NAN row values to
	 * reflect whether the value lies between the visible range of the axis.
	 * Needs to be called after determineChartType().
	 * @param axis
	 */
	private void updateLocalZoomExclusion(int axis) {
		byte zoomFlag = (byte)(EXCLUSION_FLAG_ZOOM_0 << axis);
		byte nanFlag = (byte)(EXCLUSION_FLAG_NAN_0 << axis);
		for (int i=0; i<mDataPoints; i++) {
			if (mIsCategoryAxis[axis] || (mPoint[i].exclusionFlags & nanFlag) == 0) {
				float theDouble = getAxisValue(mPoint[i].record, axis);
				if (theDouble < mAxisVisMin[axis]
				 || theDouble > mAxisVisMax[axis])
					mPoint[i].exclusionFlags |= zoomFlag;
				else
					mPoint[i].exclusionFlags &= (byte)~zoomFlag;
				}
			else {
				mPoint[i].exclusionFlags &= (byte)~zoomFlag;
				}
			}
		}

	private void updateGlobalExclusion() {
		if (mLocalAffectsGlobalExclusion && !mSuspendGlobalExclusion) {
			if (mLocalExclusionFlagNo == -1)
				mLocalExclusionFlagNo = mTableModel.getUnusedRowFlag(true);
			}
		else {
			mLocalExclusionFlagNo = -1;
			}
		applyLocalExclusion(false);
		}

	/**
	 * Returns whether rows zoomed out of view or invisible rows because of NaN values
	 * are also excluded from other views. The default is true.
	 * @return whether this view's local exclusion also affects the global exclusion
	 */
	public boolean getAffectGlobalExclusion() {
		return mLocalAffectsGlobalExclusion;
		}

	/**
	 * Defines whether local exclusion is affecting global exclusion,
	 * i.e. whether rows zoomed out of view or invisible rows because of NaN values
	 * are also excluded from other views. The default is true.
	 * @param v
	 */
	public void setAffectGlobalExclusion(boolean v) {
		if (mLocalAffectsGlobalExclusion != v) {
	   		mLocalAffectsGlobalExclusion = v;
			updateGlobalExclusion();
			}
		}

	/**
	 * Used to temporarily suspend the global record exclusion from the local one.
	 * This is called when the view gets hidden or shown again.
	 * @param suspend
	 */
	public void setSuspendGlobalExclusion(boolean suspend) {
		if (mSuspendGlobalExclusion != suspend) {
			mSuspendGlobalExclusion = suspend;
			updateGlobalExclusion();
			}
		}

	/**
	 * Set table model row exclusion flags according to local zooming/NAN-values
	 * and trigger a TableModelEvent in case the global record visibility changes.
	 * @param isAdjusting
	 */
	private void applyLocalExclusion(final boolean isAdjusting) {
		if (!mApplyLocalExclusionScheduled) {
			mApplyLocalExclusionScheduled = true;

			// in case applyLocalExclusion() is called in the cascade caused by a compoundTableChanged()
			// (e.g. with delete columns), then we must wait until all views have updated accordingly,
			// before interfering by spawning another compoundTableChanged() cascade...
			SwingUtilities.invokeLater(() -> {
					mApplyLocalExclusionScheduled = false;

					if (mLocalExclusionFlagNo != -1) {
						boolean excludedRecordsFound = false;
						long mask = mTableModel.convertRowFlagToMask(mLocalExclusionFlagNo);
						long listMask = mTableModel.getListHandler().getListMask(mLocalExclusionList);
						for (int i=0; i<mDataPoints; i++) {
							if ((mPoint[i].exclusionFlags & mActiveExclusionFlags) == 0
							 || (mShowNaNValues && (mPoint[i].exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0)
							 || (mLocalExclusionList != CompoundTableListHandler.LISTINDEX_NONE
									&& (mPoint[i].record.getFlags() & listMask) == 0)) {
								mPoint[i].record.clearFlags(mask);
								}
							else {
								mPoint[i].record.setFlags(mask);
								excludedRecordsFound = true;
								}
							}

						if (!mSuspendGlobalExclusion)
							mIsGloballyHidingRows = excludedRecordsFound;

						mTableModel.updateExternalExclusion(mLocalExclusionFlagNo, isAdjusting, excludedRecordsFound);
						}
					else if (mPreviousLocalExclusionFlagNo != -1) {
						if (!mSuspendGlobalExclusion)
							mIsGloballyHidingRows = false;

						mTableModel.freeRowFlag(mPreviousLocalExclusionFlagNo);
						}

					mPreviousLocalExclusionFlagNo = mLocalExclusionFlagNo;
				});
			}
		}

	protected String createDateLabel(int theMarker, int exponent) {
		long time = theMarker;
		while (exponent < 0) {
			if (time % 10 != 0)
				return null;
			time /= 10;
			exponent++;
			}
		while (exponent > 0) {
			time *= 10;
			exponent--;
			}
		return DateFormat.getDateInstance().format(new Date(86400000*time+43200000));
		}

	protected void updateActiveRow() {
		CompoundRecord newActiveRow = mTableModel.getActiveRow();
		if (newActiveRow != null
		 && (mActivePoint == null || mActivePoint.record != newActiveRow)) {
			for (int i=0; i<mPoint.length; i++) {
				if (mPoint[i].record == newActiveRow) {
					setActivePoint(mPoint[i]);
					break;
					}
				}
			}
		else if (newActiveRow == null && mActivePoint != null) {
			setActivePoint(null);
			}
		}

	@Override
	public boolean supportsMarkerLabelTable() {
		return false;
		}

	@Override
	public boolean isShowColumnNameInTable() {
		return false;
		}

	@Override
	public void setShowColumnNameInTable(boolean b) {}

	@Override
	public boolean supportsMidPositionLabels() {
		return true;
		}

	@Override
	public boolean supportsLabelsByList() {
		return true;
		}

	@Override
	public boolean supportsLabelBackground() {
		return true;
		}

	@Override
	public boolean supportsLabelBackgroundTransparency() {
		return mDimensions == 2;
		}

	public boolean hasCustomPositionLabels() {
		for (int i=0; i<mDataPoints; i++) {
			LabelPosition2D lp = mPoint[i].labelPosition;
			while (lp != null) {
				if (lp.isCustom())
					return true;
				lp = lp.getNext();
				}
			}
		return false;
		}

	public void readCustomLabelPositions(ArrayList<String> lineList) {
		float[] value = new float[mDimensions];
		for (String line:lineList) {
			String[] entry = line.split("\\t");
			if (entry.length == mDimensions+2) {
				int column = mTableModel.findColumn(entry[1]);
				try {
					int row = Integer.parseInt(entry[0]);
					for (int i=0; i<mDimensions; i++)
						value[i] = Float.parseFloat(entry[i+2]);
					LabelPosition2D lp = mPoint[row].getOrCreateLabelPosition(column, value.length == 3);
					lp.setCustom(true);
					lp.setXY(value[0], value[1]);
					if (value.length == 3)
						((LabelPosition3D)lp).setZ(value[2]);
					}
				catch (NumberFormatException nfe) {}
				}
			}
		}

	public void writeCustomLabelPositions(BufferedWriter writer) throws IOException {
		int[] idToRow = new int[mTableModel.getTotalRowCount()];
		for (int row=0; row<mTableModel.getTotalRowCount(); row++)
			idToRow[mTableModel.getTotalRecord(row).getID()] = row;

		for (int i=0; i<mDataPoints; i++) {
			LabelPosition2D lp = mPoint[i].labelPosition;
			while (lp != null) {
				if (lp.isCustom()) {
					lp.writePosition(writer, Integer.toString(idToRow[mPoint[i].record.getID()]), mTableModel.getColumnTitleNoAlias(lp.getColumn()));
					writer.newLine();
					}

				lp = lp.getNext();
				}
			}
		}

	public static class DoubleDimension {
		double width,height;
		}

	public static class LineConnection {
		VisualizationPoint target;
		float strength;

		public LineConnection(VisualizationPoint target, float strength) {
			this.target = target;
			this.strength = strength;
			}
		}
	}

class AxisDataRange {
	private final double min,max,relMargin;
	private double leftFullMargin,rightFullMargin;

//	public double scaledMin() {
//		return min - leftScaledMargin;
//	}
//
//	public double scaledMax() {
//		return max + rightScaledMargin;
//	}

	public double fullMin() {
		return min - leftFullMargin;
	}

	public double fullMax() {
		return max + rightFullMargin;
	}

	public double fullRange() {
		return max - min + leftFullMargin + rightFullMargin;
	}

	public AxisDataRange(CompoundTableModel tableModel, int column, double margin) {
		// NOTE: If you change the margin logic here, then also adapt the margin logic
		//       in calculatePruningBarLowAndHigh() accordingly!!!
		relMargin = margin;
		if (tableModel.isDescriptorColumn(column)) {
			min = 0.0;
			max = 1.0;
		}
		else {
			min = tableModel.getMinimumValue(column);
			max = tableModel.getMaximumValue(column);
			if (margin != 0.0
			 && min != max
			 && tableModel.getColumnProperty(column, cColumnPropertyCyclicDataMax) == null) {
				if (tableModel.getColumnProperty(column, cColumnPropertyDataMin) == null)
					leftFullMargin = margin * (max - min);
				if (tableModel.getColumnProperty(column, cColumnPropertyDataMax) == null)
					rightFullMargin = margin * (max - min);
			}
		}
	}

	public double[] calculateScaledMinAndMax(double pruningBarLow, double pruningBarHigh) {
		// NOTE: If you change the margin logic here, then also adapt the margin logic
		//       in AxisDataRange() and calculatePruningBarLowAndHigh() accordingly!!!
		double[] minAndMax = new double[2];
		minAndMax[0] = min;
		minAndMax[1] = max;
		if (leftFullMargin != 0.0 || rightFullMargin != 0.0) {
			double relWithMargin0 = (leftFullMargin == 0.0) ? 0.0 : 0.0 - relMargin * (pruningBarHigh - pruningBarLow);
			double relWithMargin1 = (rightFullMargin == 0.0) ? 1.0 : 1.0 + relMargin * (pruningBarHigh - pruningBarLow);
			double pos0 = relWithMargin0 + pruningBarLow * (relWithMargin1 - relWithMargin0);
			double pos1 = relWithMargin0 + pruningBarHigh * (relWithMargin1 - relWithMargin0);
			double visDataFraction = Math.min(1.0, pos1) - Math.max(0.0, pos0);
			minAndMax[0] = min - leftFullMargin * visDataFraction;
			minAndMax[1] = max + rightFullMargin * visDataFraction;
		}
		return minAndMax;
	}

	/**
	 * Calculates the relative end point positions of the pruning bar as 0.0 to 1.0
	 * from the input range defined in data space. Data range is defined by low and
	 * high data value, which should be between min-leftMargin and max+rightMargin.
	 * @param dataLow NaN or low data value mapped to left pruning bar position
	 * @param dataHigh NaN or high data value mapped to right pruning bar position
	 * @return array of left and right pruning bar positions (0.0 to 1.0 each)
	 */
	public double[] calculatePruningBarLowAndHigh(double dataLow, double dataHigh) {
		// NOTE: If you change the margin logic here, then also adapt the margin logic
		//       in AxisDataRange() accordingly!!!
		double[] lowAndHigh = new double[2];
		lowAndHigh[0] = 0f;
		lowAndHigh[1] = 1f;
		if (min < max) {
			if (!Double.isNaN(dataLow) && dataLow <= fullMin())
				dataLow = Double.NaN;
			if (!Double.isNaN(dataHigh) && dataHigh >= fullMax())
				dataHigh = Double.NaN;
			boolean maxOutLeft = Double.isNaN(dataLow);
			boolean maxOutRight = Double.isNaN(dataHigh);
			if (!maxOutLeft || !maxOutRight) {
				double data0 = maxOutLeft ? min : Math.min(max, Math.max(min, dataLow));
				double data1 = maxOutRight ? max : Math.max(min, Math.min(max, dataHigh));
				double relRange = (data1 - data0) / (max - min);

				double margin0 = leftFullMargin * relRange;
				double margin1 = rightFullMargin * relRange;
				if (!maxOutLeft) {
					if (dataLow < min)  // if in margin area we have to apapt for smaller margin
						dataLow = min - margin0 * (min - dataLow) / leftFullMargin;
					lowAndHigh[0] = (dataLow - min + margin0) / (margin0 + margin1 + max - min);
					}
				if (!maxOutRight) {
					if (dataHigh > max)  // if in margin area we have to apapt for smaller margin
						dataHigh = max + margin1 * (dataHigh - max) / rightFullMargin;
					lowAndHigh[1] = (dataHigh - min + margin0) / (margin0 + margin1 + max - min);
				}
			}
		}
		return lowAndHigh;
	}
}
class VisualizationPointComparator implements Comparator<VisualizationPoint> {
	public int compare(VisualizationPoint o1, VisualizationPoint o2) {
		int id1 = o1.record.getID();
		int id2 = o2.record.getID();
		return Integer.compare(id1, id2);
		}
	}
