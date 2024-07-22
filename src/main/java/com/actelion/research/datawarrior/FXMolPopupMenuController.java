package com.actelion.research.datawarrior;

import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.alignment3d.PheSAAlignmentOptimizer;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.table.DETaskSetColumnProperties;
import com.actelion.research.gui.form.JFXConformerPanel;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ArrayUtils;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import org.openmolecules.fx.viewer3d.V3DPopupMenuController;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class FXMolPopupMenuController implements V3DPopupMenuController {
	private final JFXConformerPanel mConformerPanel;
	private final CompoundTableModel mTableModel;
	private CompoundRecord mParentRecord;
	private final int mIDCodeColumn,mCoordsColumn;
	private final boolean mAllowSuperposeReference;

	public FXMolPopupMenuController(JFXConformerPanel conformerPanel, CompoundTableModel tableModel, int coordsColumn, boolean allowSuperposeReference) {
		mConformerPanel = conformerPanel;
		mTableModel = tableModel;
		mIDCodeColumn = tableModel.getParentColumn(coordsColumn);
		mCoordsColumn = coordsColumn;
		mAllowSuperposeReference = allowSuperposeReference;
	}

	/**
	 * Must be called to keep underlying record up-to-date only if mAllowSuperposeReference is true
	 * @param record
	 */
	public void setParentRecord(CompoundRecord record) {
		mParentRecord = record;
	}

	@Override
	public void addExternalMenuItems(ContextMenu popup, int type) {
		if (type == V3DPopupMenuController.TYPE_VIEW) {
			boolean hasCavity = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyProteinCavity) != null;
			boolean hasLigand = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand) != null;
			boolean isShowLigand = !"false".equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyShowNaturalLigand));
			boolean isSuperpose = CompoundTableConstants.cSuperposeValueReferenceRow.equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperpose));
			boolean isShapeAlign = CompoundTableConstants.cSuperposeAlignValueShape.equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperposeAlign));

			if (hasLigand) {
				javafx.scene.control.CheckMenuItem itemShowLigand = new CheckMenuItem("Show Natural Ligand");
				itemShowLigand.setSelected(isShowLigand);
				itemShowLigand.setOnAction(e -> setShowNaturalLigand(!isShowLigand));
				popup.getItems().add(itemShowLigand);
			}

			if (mAllowSuperposeReference) {
				javafx.scene.control.CheckMenuItem itemSuperpose = new CheckMenuItem("Show Reference Row Structure");
				itemSuperpose.setSelected(isSuperpose);
				itemSuperpose.setOnAction(e -> setSuperposeMode(!isSuperpose, isShapeAlign));
				popup.getItems().add(itemSuperpose);
			}

			if (mAllowSuperposeReference && !hasCavity && isSuperpose) {
				// don't allow shape alignment if we show a protein cavity
				javafx.scene.control.CheckMenuItem itemAlignShape = new CheckMenuItem("Align Shown Structures");
				itemAlignShape.setSelected(isShapeAlign);
				itemAlignShape.setDisable(false);
				itemAlignShape.setOnAction(e -> setSuperposeMode(isSuperpose, !isShapeAlign));
				popup.getItems().add(itemAlignShape);
			}

			if (hasCavity) {
				boolean isShowInteractions = (hasLigand && mConformerPanel.getV3DScene().isShowInteractions());
				javafx.scene.control.CheckMenuItem itemShowInteractions = new CheckMenuItem("Show Interactions");
				itemShowInteractions.setSelected(isShowInteractions);
				itemShowInteractions.setDisable(!hasLigand);
				itemShowInteractions.setOnAction(e -> setShowInteractions(!isShowInteractions));
				popup.getItems().add(itemShowInteractions);
			}

			popup.getItems().add(new SeparatorMenuItem());
		}
	}

	private void setSuperposeMode(boolean isSuperpose, boolean isShapeAlign) {
		SwingUtilities.invokeLater(() -> {
			HashMap<String, String> map = new HashMap<>();
			map.put(CompoundTableConstants.cColumnPropertySuperpose, isSuperpose ? CompoundTableConstants.cSuperposeValueReferenceRow : null);
			map.put(CompoundTableConstants.cColumnPropertySuperposeAlign, isShapeAlign ? CompoundTableConstants.cSuperposeAlignValueShape : null);
			new DETaskSetColumnProperties(getFrame(), mCoordsColumn, map, false).defineAndRun();
			update3DView(isSuperpose, isShapeAlign);
		});
	}

	private void setShowNaturalLigand(boolean isShowNaturalLigand) {
		String ligand = isShowNaturalLigand ? mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand) : null;
		StereoMolecule ligandMol = (ligand == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(ligand);
		mConformerPanel.setOverlayMolecule(ligandMol);
		SwingUtilities.invokeLater(() -> {
			HashMap<String, String> map = new HashMap<>();
			map.put(CompoundTableConstants.cColumnPropertyShowNaturalLigand, isShowNaturalLigand ? null : "false");
			new DETaskSetColumnProperties(getFrame(), mCoordsColumn, map, false).defineAndRun();
		});
	}

	private DEFrame getFrame() {
		Component c = mConformerPanel;
		while (!(c instanceof DEFrame))
			c = c.getParent();

		return (DEFrame)c;
	}

	private void setShowInteractions(boolean showInteractions) {
		mConformerPanel.getV3DScene().setShowInteractions(showInteractions);
	}

	public void update3DView(boolean isSuperpose, boolean isAlign) {
		new Thread(() -> {
			StereoMolecule[] rowMol = null;
			if (mParentRecord != null)
				rowMol = getConformers(mParentRecord, true);

			StereoMolecule[] refMol = null;
			if (isSuperpose && mTableModel.getActiveRow() != null && mTableModel.getActiveRow() != mParentRecord)
				refMol = getConformers(mTableModel.getActiveRow(), false);

			if (rowMol != null) {
				StereoMolecule best = null;
				if (isAlign && refMol != null) {
					double maxFit = 0;
					for (StereoMolecule stereoMolecule : rowMol) {
						double fit = PheSAAlignmentOptimizer.alignTwoMolsInPlace(refMol[0], stereoMolecule);
						if (fit>maxFit) {
							maxFit = fit;
							best = stereoMolecule;
						}
					}

					rowMol = new StereoMolecule[1];
					rowMol[0] = best;
				}
			}

			int rowID = (mParentRecord == null || isSuperpose || mConformerPanel.getOverlayMolecule() != null) ? -1 : mParentRecord.getID();
			mConformerPanel.updateConformers(rowMol, rowID, refMol == null ? null : refMol[0]);
		}).start();
	}

	private StereoMolecule[] getConformers(CompoundRecord record, boolean allowMultiple) {
		byte[] idcode = (byte[]) record.getData(mIDCodeColumn);
		byte[] coords = (byte[]) record.getData(mCoordsColumn);
		if (idcode != null && coords != null) {
			boolean split3DFragments = "true".equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableModel.cColumnProperty3DFragmentSplit));
			if (split3DFragments) {
				return new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, coords).getFragments();
			}
			else {
				int count = 1;
				int index = ArrayUtils.indexOf(coords, (byte) 32);
				if (index != -1 && allowMultiple) {
					count++;
					for (int i = index + 1; i < coords.length; i++)
						if (coords[i] == (byte) 32)
							count++;
					index = -1;
				}
				StereoMolecule[] mol = new StereoMolecule[count];
				for (int i=0; i<count; i++) {
					mol[i] = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, coords, 0, index + 1);
					index = ArrayUtils.indexOf(coords, (byte) 32, index + 1);
				}
				return mol;
			}
		}
		return null;
	}
}