/*******************************************************************************
 * (C) Copyright  2016 Jérôme Comte and others.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *    - Jérôme Comte
 *******************************************************************************/

package io.djigger.ui.threadselection;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JPanel;

public class ThreadSelectionTimeAxis extends JPanel {

	private boolean timeBasedAxis;

	private final ThreadSelectionPane threadSelectionGraphPane;
	
	private AggregateDefinition rangeDefinition;

	public ThreadSelectionTimeAxis(ThreadSelectionPane threadSelectionGraphPane) {
		super();
		this.threadSelectionGraphPane = threadSelectionGraphPane;
		setPreferredSize(new Dimension(0, 20));
	}

	public void setTimeBasedAxis(boolean timeBasedAxis) {
		this.timeBasedAxis = timeBasedAxis;
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (threadSelectionGraphPane.getCurrentRange() != null) {
			Graphics2D g2 = (Graphics2D) g.create();
			setBackground(Color.WHITE);
			g2.setFont(new Font("Arial", Font.PLAIN, 10));

			if(timeBasedAxis) {
				drawTimeAxis(g2, threadSelectionGraphPane.getxOffset());
			} else {
				drawIdAxis(g2, threadSelectionGraphPane.getxOffset(), threadSelectionGraphPane.getWidth());
			}

			g2.dispose();
		}
	}

	public void setRangeDefinition(AggregateDefinition rangeDefinition) {
		this.rangeDefinition = rangeDefinition;
	}

	private void drawTimeAxis(Graphics2D graph, int xOffset) {
		long interval = threadSelectionGraphPane.getCurrentRange().end - threadSelectionGraphPane.getCurrentRange().start;
		SimpleDateFormat format;
		if (interval < 10000) {
			format = new SimpleDateFormat("HH:mm:ss.S");
		} else if (interval < 3600000) {
			format = new SimpleDateFormat("HH:mm:ss");
		} else if (interval < 432000000) {
			format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		} else {
			format = new SimpleDateFormat("yyyy.MM.dd");
		}

		String startDateStr = format.format(new Date(threadSelectionGraphPane.getCurrentRange().start));
		double labelWidth = graph.getFont()
				.getStringBounds(startDateStr, graph.getFontRenderContext())
				.getWidth();

		int wWidth = getSize().width - xOffset;
		int margin = 20;
		int numberOfLabels = (int) (wWidth / (labelWidth + margin));
		int xIncrement = wWidth / numberOfLabels;

		int x = xOffset;
		for (int i = 0; i < numberOfLabels; i++) {
			Date date = new Date(this.threadSelectionGraphPane.xToRange(x));
			String dateStr = format.format(date);
			graph.drawChars(dateStr.toCharArray(), 0, dateStr.length(), x + 5,
					15);
			graph.drawLine(x, 20, x, 15);
			x += xIncrement;
		}

	}

	private void drawIdAxis(Graphics2D graph, int xOffset, int width) {
		String id2Str = Long.toString(rangeDefinition.getEnd());
		double labelWidth = graph.getFont()
				.getStringBounds(id2Str, graph.getFontRenderContext())
				.getWidth();

		int wWidth = width - xOffset;
		int margin = 20;
		
		int maxNumberOfLabel = (int) (wWidth / (labelWidth + margin));
		
		if(rangeDefinition.getRangeNumber()<maxNumberOfLabel) {
			for(int i=0;i<rangeDefinition.getRangeNumber();i++) {
				String str = Long.toString(rangeDefinition.getCursor(i));
				int x = xOffset + (int) (i*1.0/rangeDefinition.getRangeNumber()*wWidth);
				graph.drawChars(str.toCharArray(), 0, str.length(), x, 15);
				graph.drawLine(x, 20, x, 15);
			}
		} else {
			int factor = rangeDefinition.getRangeNumber()/maxNumberOfLabel;
			for(int i=0;i<rangeDefinition.getRangeNumber();i++) {
				if(i%factor==0) {
				String str = Long.toString(rangeDefinition.getCursor(i));
				int x = xOffset + (int) (i*1.0/rangeDefinition.getRangeNumber()*wWidth);
				graph.drawChars(str.toCharArray(), 0, str.length(), x, 15);
				graph.drawLine(x, 20, x, 15);
				}
			}
			System.err.println("number of label to high");
		}
	}

}
