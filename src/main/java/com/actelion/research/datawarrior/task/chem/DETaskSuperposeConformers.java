package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.alignment3d.PheSAAlignmentOptimizer;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.phesaflex.FlexibleShapeAlignment;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.task.chem.elib.ConformerViewController;
import com.actelion.research.gui.form.JFXConformerPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.V3DScene;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Properties;

public class DETaskSuperposeConformers extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Superpose Conformers";

	private static final String PROPERTY_CONFORMERS = "conformers";
	private static final String PROPERTY_FLEXIBLE = "flexible";
	private static final int COLUMNS_PER_CONFORMER = 3;

	private final DETable mTable;
	protected JFXConformerPanel mConformerPanel;
	private JCheckBox mCheckBoxFlexible;
	private String[] mConformerIDCode;
	private StereoMolecule[] mConformer;
	private boolean mIsFlexible;

	public DETaskSuperposeConformers(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
		mTable = parent.getMainFrame().getMainPane().getTable();
	}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
	}

	@Override
	public JPanel getExtendedDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };

		EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
		settings.add(V3DScene.ViewerSettings.EDITING);
		mConformerPanel = new JFXConformerPanel(false, settings);
		mConformerPanel.adaptToLookAndFeelChanges();
//		mConformerPanel.setBackground(new java.awt.Color(24, 24, 96));
		mConformerPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(400), HiDPIHelper.scale(200)));
		mConformerPanel.setPopupMenuController(new ConformerViewController(getParentFrame(), mConformerPanel));

		mCheckBoxFlexible = new JCheckBox("Use Flexible Alignment");

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Use right mouse click for adding 3D-molecule(s) to be superposed:", JLabel.CENTER), "0,1");
		ep.add(mConformerPanel, "0,3");
		ep.add(mCheckBoxFlexible, "0,5");
		return ep;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/conformers.html#Superpose";
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		StringBuilder sb = new StringBuilder();
		ArrayList<StereoMolecule> moleculeList = mConformerPanel.getMolecules(null);
		for (StereoMolecule mol:moleculeList) {
			mol.center();
			Canonizer canonizer = new Canonizer(mol);
			if (!sb.isEmpty())
				sb.append('\t');
			sb.append(canonizer.getIDCode());
			sb.append(' ');
			sb.append(canonizer.getEncodedCoordinates());
			}

		configuration.setProperty(PROPERTY_CONFORMERS, sb.toString());
		configuration.setProperty(PROPERTY_FLEXIBLE, mCheckBoxFlexible.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		StereoMolecule[] mols = getConformers(configuration);
		if (mols != null) {
			for (StereoMolecule mol:mols)
				mConformerPanel.addMolecule(mol, null, null);
			}

		mCheckBoxFlexible.setSelected("true".equals(configuration.getProperty(PROPERTY_FLEXIBLE)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		}

	private StereoMolecule[] getConformers(Properties configuration) {
		String conformers = configuration.getProperty(PROPERTY_CONFORMERS);
		if (conformers == null)
			return null;

		mConformerIDCode = conformers.split("\\t");
		StereoMolecule[] mol = new StereoMolecule[mConformerIDCode.length];
		for (int i=0; i<mConformerIDCode.length; i++) {
			mol[i] = new IDCodeParser(false).getCompactMolecule(mConformerIDCode[i]);
			if (mol[i] == null)
				return null;
			mol[i].ensureHelperArrays(Molecule.cHelperParities);
			}

		mCheckBoxFlexible.setSelected(false);

		return mol;
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		int conformerColumn = firstNewColumn;
		for (int i = 0; i<mConformer.length; i++) {
			String columnNameEnd = (mConformer[i].getName() != null && !mConformer[i].getName().isEmpty()) ?
					" "+ mConformer[i].getName() : (mConformer.length == 1) ? "" : " "+(i+1);
			getTableModel().setColumnName("Best Match" + columnNameEnd, conformerColumn);
			getTableModel().setColumnProperty(conformerColumn, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);

			int coords3DColumn = conformerColumn + 1;
			getTableModel().setColumnName("Superposition" + columnNameEnd, coords3DColumn);
			getTableModel().setColumnProperty(coords3DColumn, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
			getTableModel().setColumnProperty(coords3DColumn, CompoundTableConstants.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(conformerColumn));
			getTableModel().setColumnProperty(coords3DColumn, CompoundTableConstants.cColumnPropertySuperposeMolecule, mConformerIDCode[i]);

			int matchColumn = conformerColumn + 2;
			getTableModel().setColumnName("PheSA Score" + columnNameEnd, matchColumn);

			conformerColumn += COLUMNS_PER_CONFORMER;
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String conformers = configuration.getProperty(PROPERTY_CONFORMERS, "");
		if (conformers.isEmpty()) {
			showErrorMessage("No query conformers defined.");
			return false;
			}

		if (isLive) {
			if (!"true".equals(configuration.getProperty(PROPERTY_FLEXIBLE))
			 && getTableModel().getChildColumn(getChemistryColumn(), CompoundTableConstants.cColumnType3DCoordinates) == -1) {
				showErrorMessage("The selected structure column does not contain 3D-coordinates.\n"
							   + "To align these structures you either need to generate conformers\n"
							   + "before aligning them, or you need to select 'flexible' alignment.");
				return false;
			}
		}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	protected int getNewColumnCount() {
		return mConformer == null ? 0 : mConformer.length * COLUMNS_PER_CONFORMER;
	}

	@Override
	protected String getNewColumnName(int column) {
		// names are given by setNewColumnProperties();
		return "";
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mConformer = getConformers(configuration);
		mIsFlexible = "true".equals(configuration.getProperty(PROPERTY_FLEXIBLE));
		return super.preprocessRows(configuration);
	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		CompoundRecord record = getTableModel().getTotalRecord(row);
		byte[] idcode = (byte[])record.getData(getChemistryColumn());
		if (idcode != null) {
			byte[] coords = (byte[])record.getData(getCoordinates3DColumn());
			if (mIsFlexible || coords != null) {
				int targetColumn = firstNewColumn;
				for (StereoMolecule conformer : mConformer) {
					Conformer bestConformer = null;
					double bestFit = 0.0f;

					int coordinateIndex = 0;
					while (mIsFlexible || (coords != null && coordinateIndex<coords.length)) {
						new IDCodeParser(false).parse(containerMol, idcode, coords, 0, coordinateIndex);
						if (coords == null
						 && new ConformerGenerator().getOneConformerAsMolecule(containerMol) == null)
							break;

						double fit = PheSAAlignmentOptimizer.alignTwoMolsInPlace(conformer, containerMol, 0.5);
						if (mIsFlexible) {
							FlexibleShapeAlignment fsa = new FlexibleShapeAlignment(conformer, containerMol);
							fit = fsa.align()[0];
						}
						if (bestFit<fit) {
							bestFit = fit;
							bestConformer = new Conformer(containerMol);
						}

						if (bestConformer != null) {
							bestConformer.toMolecule(containerMol);
							Canonizer canonizer = new Canonizer(containerMol);
							getTableModel().setTotalValueAt(canonizer.getIDCode(), row, targetColumn);
							getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(true), row, targetColumn + 1);
							getTableModel().setTotalValueAt(DoubleFormat.toString(bestFit), row, targetColumn + 2);
						}

						if (mIsFlexible)
							break;

						while (coordinateIndex<coords.length) {
							coordinateIndex++;
							if (coords[coordinateIndex - 1] == ' ')
								break;
						}
					}

					targetColumn += COLUMNS_PER_CONFORMER;
				}
				}
			}
		}

	protected void postprocess(int firstNewColumn) {
		while (firstNewColumn < getTableModel().getTotalColumnCount()) {
			mTable.setColumnVisibility(firstNewColumn, false);
			firstNewColumn += COLUMNS_PER_CONFORMER;
			}
		}
	}
