package cnuphys.ced.cedview.alldc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import cnuphys.ced.cedview.CedView;
import cnuphys.ced.component.ControlPanel;
import cnuphys.ced.component.DisplayBits;
import cnuphys.ced.event.data.DCDataContainer;
import cnuphys.ced.event.data.DataDrawSupport;
import cnuphys.ced.geometry.GeoConstants;
import cnuphys.ced.item.AllDCSuperLayer;
import cnuphys.bCNU.drawable.DrawableAdapter;
import cnuphys.bCNU.drawable.IDrawable;
import cnuphys.bCNU.graphics.GraphicsUtilities;
import cnuphys.bCNU.graphics.container.IContainer;
import cnuphys.bCNU.graphics.style.Styled;
import cnuphys.bCNU.graphics.toolbar.BaseToolBar;
import cnuphys.bCNU.graphics.world.WorldGraphicsUtilities;
import cnuphys.bCNU.layer.LogicalLayer;
import cnuphys.bCNU.util.Fonts;
import cnuphys.bCNU.util.PropertySupport;
import cnuphys.bCNU.util.X11Colors;

/**
 * The AllDC view is a non-faithful representation of all six sectors of
 * driftchambers. It is very useful for occupancy plots.
 * 
 * @author heddle
 * 
 */
@SuppressWarnings("serial")
public class AllDCView extends CedView {

	/**
	 * A sector rectangle for each sector
	 */
	private Rectangle2D.Double _sectorWorldRects[];

	// font for label text
	private static final Font labelFont = Fonts.commonFont(Font.PLAIN, 11);

	/**
	 * Used for drawing the sector rects.
	 */
	private Styled _sectorStyle;

	// The optional "before" drawer for this view
	private IDrawable _beforeDraw;

	// transparent color
	private static final Color TRANS = new Color(255, 255, 0, 80);

	/**
	 * The all dc view is rendered on 2x3 grid. Each grid is 1x1 in world
	 * coordinates. Thus the whole view has width = 3 and height = 2. These
	 * offesets move the sector to the right spot on the grid.
	 */
	private static double _xoffset[] = { 0.0, 1.0, 2.0, 0.0, 1.0, 2.0 };

	/**
	 * The all dc view is rendered on 2x3 grid. Each grid is 1x1 in world
	 * coordinates. Thus the whole view has width = 3 and height = 2. These
	 * offesets move the sector to the right spot on the grid.
	 */
	private static double _yoffset[] = { 1.0, 1.0, 1.0, 0.0, 0.0, 0.0 };

	// all the superlayer items indexed by sector (0..5) and superlayer (0..5)
	private AllDCSuperLayer _superLayerItems[][];

	private static Rectangle2D.Double _defaultWorldRectangle = new Rectangle2D.Double(
			0.0, 0.0, 3.0, 2.0);

	/**
	 * Create an allDCView
	 * 
	 * @param keyVals
	 *            variable set of arguments.
	 */
	private AllDCView(Object... keyVals) {
		super(keyVals);

		setSectorWorldRects();
		setBeforeDraw();
		setAfterDraw();
		addItems();
	}

	/**
	 * Convenience method for creating an AllDC View.
	 * 
	 * @return a new AllDCView.
	 */
	public static AllDCView createAllDCView() {
		AllDCView view = null;

		// set to a fraction of screen
		Dimension d = GraphicsUtilities.screenFraction(0.5);

		// create the view
		view = new AllDCView(
				PropertySupport.WORLDSYSTEM,
				_defaultWorldRectangle,
				PropertySupport.WIDTH,
				d.width, // container width, not total view width
				PropertySupport.HEIGHT,
				d.height, // container height, not total view width
				PropertySupport.TOOLBAR, true, PropertySupport.TOOLBARBITS,
				BaseToolBar.NODRAWING & ~BaseToolBar.RANGEBUTTON
						& ~BaseToolBar.TEXTFIELD
						& ~BaseToolBar.CONTROLPANELBUTTON
						& ~BaseToolBar.TEXTBUTTON & ~BaseToolBar.DELETEBUTTON,
				PropertySupport.VISIBLE, true, PropertySupport.HEADSUP, false,
				PropertySupport.TITLE, "All Drift Chambers",
				PropertySupport.STANDARDVIEWDECORATIONS, true);

		view._controlPanel = new ControlPanel(view, ControlPanel.NOISECONTROL
				+ ControlPanel.DISPLAYARRAY + ControlPanel.FEEDBACK
				+ ControlPanel.ACCUMULATIONLEGEND + ControlPanel.RECONSARRAY,
				DisplayBits.ACCUMULATION + DisplayBits.DC_HB_RECONS_HITS
						+ DisplayBits.DC_TB_RECONS_HITS + DisplayBits.MCTRUTH,
				2, 10);

		view.add(view._controlPanel, BorderLayout.EAST);
		view.pack();
		return view;
	}

	/**
	 * Create the before drawer to draw the sector outlines.
	 */
	private void setBeforeDraw() {
		// style for sector rects
		_sectorStyle = new Styled(X11Colors.getX11Color("dark slate gray"));
		_sectorStyle.setLineColor(Color.lightGray);

		// use a before-drawer to sector dividers and labels
		_beforeDraw = new DrawableAdapter() {

			@Override
			public void draw(Graphics g, IContainer container) {
				g.setFont(labelFont);
				for (int sector = 0; sector < GeoConstants.NUM_SECTOR; sector++) {
					WorldGraphicsUtilities.drawWorldRectangle(g, container,
							_sectorWorldRects[sector], _sectorStyle);
					double left = _sectorWorldRects[sector].x;
					double top = _sectorWorldRects[sector].y
							+ _sectorWorldRects[sector].height;
					g.setColor(Color.cyan);
					WorldGraphicsUtilities.drawWorldText(g, container, left,
							top, "Sector " + (sector + 1), 8, 12);
				}
			}

		};

		getContainer().setBeforeDraw(_beforeDraw);
	}

	private void drawDiag(Graphics g, Rectangle rect, Color color, int opt) {
		int l = rect.x + 1;
		int t = rect.y + 1;
		int r = l + rect.width - 2;
		int b = t + rect.height - 2;

		g.setColor(color);

		if (opt == 0) {
			g.drawLine(l, b, r, t);
		} else {
			g.drawLine(l, t, r, b);
		}
	}

	/**
	 * Set the views before draw
	 */
	private void setAfterDraw() {
		IDrawable _afterDraw = new DrawableAdapter() {

			@Override
			public void draw(Graphics g, IContainer container) {
				DCDataContainer dcData = _eventManager.getDCData();

				// reconstructed drawing
				if (showDChbHits() && !_eventManager.isAccumulating()) {
					int hbHitCount = dcData.getHitBasedHitCount();
					if (hbHitCount > 0) {
						int sector[] = dcData.hitbasedtrkg_hbhits_sector;
						int wire[] = dcData.hitbasedtrkg_hbhits_wire;
						int layer[] = dcData.hitbasedtrkg_hbhits_layer;
						;
						int superLayer[] = dcData.hitbasedtrkg_hbhits_superlayer;
						Rectangle2D.Double wr = new Rectangle2D.Double();
						Rectangle rr = new Rectangle();
						for (int i = 0; i < hbHitCount; i++) {
							getCell(sector[i], superLayer[i], layer[i],
									wire[i], wr);

							container.worldToLocal(rr, wr);
							drawDiag(g, rr, DataDrawSupport.DC_TB_COLOR, 0);
						} // end for
					} // end hbhitcount > 0
				} // show hb recons hits

				if (showDCtbHits() && !_eventManager.isAccumulating()) {
					int tbHitCount = dcData.getTimeBasedHitCount();
					if (tbHitCount > 0) {
						int sector[] = dcData.timebasedtrkg_tbhits_sector;
						int wire[] = dcData.timebasedtrkg_tbhits_wire;
						int layer[] = dcData.timebasedtrkg_tbhits_layer;
						;
						int superLayer[] = dcData.timebasedtrkg_tbhits_superlayer;
						Rectangle2D.Double wr = new Rectangle2D.Double();
						Rectangle rr = new Rectangle();
						for (int i = 0; i < tbHitCount; i++) {
							getCell(sector[i], superLayer[i], layer[i],
									wire[i], wr);

							container.worldToLocal(rr, wr);
							drawDiag(g, rr, DataDrawSupport.DC_TB_COLOR, 1);
						} // end for
					} // end hbhitcount > 0
				} // end tb hits
			}

		};
		getContainer().setAfterDraw(_afterDraw);
	}

	/**
	 * Setup the sector world rects
	 */
	private void setSectorWorldRects() {

		_sectorWorldRects = new Rectangle2D.Double[6];

		Rectangle2D.Double defaultWorld = _defaultWorldRectangle;
		double left = defaultWorld.getMinX();
		double right = defaultWorld.getMaxX();
		double top = defaultWorld.getMaxY();
		double bottom = defaultWorld.getMinY();
		double ymid = defaultWorld.getCenterY();
		double x13 = left + defaultWorld.width / 3.0;
		double x23 = right - defaultWorld.width / 3.0;

		_sectorWorldRects[0] = new Rectangle2D.Double(left, ymid, x13 - left,
				top - ymid);
		_sectorWorldRects[1] = new Rectangle2D.Double(x13, ymid, x23 - x13, top
				- ymid);
		_sectorWorldRects[2] = new Rectangle2D.Double(x23, ymid, right - x13,
				top - ymid);

		_sectorWorldRects[3] = new Rectangle2D.Double(left, bottom, x13 - left,
				ymid - bottom);
		_sectorWorldRects[4] = new Rectangle2D.Double(x13, bottom, x23 - x13,
				ymid - bottom);
		_sectorWorldRects[5] = new Rectangle2D.Double(x23, bottom, right - x23,
				ymid - bottom);
	}

	/**
	 * This adds the detector items. The AllDC view is not faithful to geometry.
	 * All we really uses in the number of superlayers, number of layers, and
	 * number of wires.
	 */
	private void addItems() {
		// use sector 0 all the same
		LogicalLayer detectorLayer = getContainer().getLogicalLayer(
				_detectorLayerName);

		double width = 0.92; // full width of each sector is 1.0;
		double xo = (1.0 - width) / 2.0;

		// sizing the height is more difficult. Total height is 1.0.
		double bottomMargin = 0.03; // from bottom to superlayer 1 in world
		// coords
		double topMargin = 0.06; // will get bigger if TOF added
		double superLayerGap = 0.02; // between superlayers
		double regionGap = 0.04; // between regions
		double whiteSpace = bottomMargin + topMargin + 3 * superLayerGap + 2
				* regionGap;
		double height = (1.0 - whiteSpace) / GeoConstants.NUM_SUPERLAYER;

		// cache all the superlayer items we are about to create
		_superLayerItems = new AllDCSuperLayer[GeoConstants.NUM_SECTOR][GeoConstants.NUM_SUPERLAYER];

		// loop over the sectors and add 6 superlayer items for each sector
		for (int sector = 0; sector < GeoConstants.NUM_SECTOR; sector++) {
			double yo = bottomMargin;
			for (int superLayer = 0; superLayer < GeoConstants.NUM_SUPERLAYER; superLayer++) {
				Rectangle2D.Double wr = new Rectangle2D.Double(_xoffset[sector]
						+ xo, _yoffset[sector] + yo, width, height);

				// note we add superlayer items with 0-based sector and
				// superLayer
				// note we flip for lower sectors

				_superLayerItems[sector][superLayer] = null;
				if (sector < 3) {
					_superLayerItems[sector][superLayer] = new AllDCSuperLayer(
							detectorLayer, this, wr, sector, superLayer,
							GeoConstants.NUM_WIRE);
				} else {
					_superLayerItems[sector][superLayer] = new AllDCSuperLayer(
							detectorLayer, this, wr, sector, 5 - superLayer,
							GeoConstants.NUM_WIRE);
				}

				if ((superLayer % 2) == 0) {
					yo += superLayerGap + height;
				} else {
					yo += regionGap + height;
				}
			}

		}
	}

	/**
	 * Get the AllDCSuperLayer item for the given sector and superlayer.
	 * 
	 * @param sector
	 *            the zero-based sector [0..5]
	 * @param superLayer
	 *            the zero based super layer [0..5]
	 * @return the AllDCSuperLayer item for the given sector and superlayer (or
	 *         <code>null</code>).
	 */
	public AllDCSuperLayer getAllDCSuperLayer(int sector, int superLayer) {
		if ((sector < 0) || (sector >= GeoConstants.NUM_SECTOR)) {
			return null;
		}
		if ((superLayer < 0) || (superLayer >= GeoConstants.NUM_SUPERLAYER)) {
			return null;
		}
		return _superLayerItems[sector][superLayer];
	}

	/**
	 * Some view specific feedback. Should always call super.getFeedbackStrings
	 * first.
	 * 
	 * @param container
	 *            the base container for the view.
	 * @param screenPoint
	 *            the pixel point
	 * @param worldPoint
	 *            the corresponding world location.
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedbackStrings) {

		// get the common information
		super.getFeedbackStrings(container, screenPoint, worldPoint,
				feedbackStrings);
		// feedbackStrings.add("#DC hits: " + _numHits);
	}

	/**
	 * Get the sector corresponding to the current pointer location..
	 * 
	 * @param container
	 *            the base container for the view.
	 * @param screenPoint
	 *            the pixel point
	 * @param worldPoint
	 *            the corresponding world location.
	 * @return the sector [1..6] or -1 for none.
	 */
	@Override
	public int getSector(IContainer container, Point screenPoint,
			Point2D.Double worldPoint) {
		for (int sector = 0; sector < GeoConstants.NUM_SECTOR; sector++) {
			if (_sectorWorldRects[sector].contains(worldPoint)) {
				return sector + 1; // convert to 1-based index
			}
		}
		return -1;
	}

	/**
	 * Get the world rectangle for a given cell (the wire is in the center)
	 * 
	 * @param sector
	 *            the 1-based sector
	 * @param superLayer
	 *            the 1-based super layer
	 * @param layer
	 *            the 1-based layer [1..6]
	 * @param wire
	 *            the 1-based wire [1..] return the world rectangle cell for
	 *            this layer, wire
	 */
	public void getCell(int sector, int superLayer, int layer, int wire,
			Rectangle2D.Double wr) {
		_superLayerItems[sector - 1][superLayer - 1].getCell(layer, wire, wr);
	}

}
