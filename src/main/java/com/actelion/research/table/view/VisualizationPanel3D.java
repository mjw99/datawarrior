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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundListSelectionModel;
import com.actelion.research.table.model.CompoundTableModel;

public class VisualizationPanel3D extends VisualizationPanel implements RotationListener {
    private static final long serialVersionUID = 0x20060904;

    public VisualizationPanel3D(DEFrame owner,
                                CompoundTableModel tableModel,
                                CompoundListSelectionModel selectionModel) {
		super(owner, tableModel);
		mVisualization = new JVisualization3D(owner, tableModel, selectionModel);
		((JVisualization3D)mVisualization).addRotationListener(this);
		mDimensions = 3;
		initialize();
		}

	@Override
	public void rotationChanged(JVisualization3D source, float[][] rotation) {
		if (getSynchronizationMaster() != null)
			((VisualizationPanel3D)getSynchronizationMaster()).rotationChanged(source, rotation);
		else
			for (VisualizationPanel child:getSynchronizationChildList())
				if (child.getDimensionCount() == 3 && child.getVisualization() != source)
					((JVisualization3D)child.getVisualization()).rotationChanged(source, rotation);

		fireVisualizationChanged(VisualizationEvent.TYPE.ROTATION);
		}

	@Override
	protected void synchronizeViewWithMaster(VisualizationPanel newMaster) {
		super.synchronizeViewWithMaster(newMaster);
		((JVisualization3D)mVisualization).setRotationMatrix(
				((JVisualization3D)newMaster.getVisualization()).getRotationMatrix());
		}

	@Override
    public void zoom(int sx, int sy, int steps) {
		final double MIN_ZOOM = 0.0001;
		float[] c = ((JVisualization3D)mVisualization).getMetaFromScreenCoordinates(sx, sy, 0);
		double f = Math.exp(steps / 20.0);
		double[] low = new double[3];
		double[] high = new double[3];
		boolean zoom = false;
		for (int i=0; i<3; i++) {
			low[i] = getActingPruningBar(i).getLowValue();
			high[i] = getActingPruningBar(i).getHighValue();
			if ((steps < 0 && high[i]-low[i] > MIN_ZOOM) || (steps > 0 && high[i]-low[i] < 1.0)) {
	    		double p = low[i] + (1.0+c[i]) * (high[i]-low[i]) / 2.0;
	    		low[i] = Math.max(0, p-f*(p-low[i]));
	    		high[i] = Math.min(1.0, p+f*(high[i]-p));
	    		zoom = true;
				}
			}
		if (zoom)
			setZoom(low, high, false, false);
		}
	}
