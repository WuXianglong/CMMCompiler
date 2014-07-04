package util;

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;

/**
 *模拟Windows的状态条
 * 
 * @version 1.0
 */
@SuppressWarnings("serial")
public class JStatusBar extends JComponent implements Serializable {
	@SuppressWarnings("unchecked")
	private Vector vecCellWidth = new Vector();

	public JStatusBar() {
		this.setPreferredSize(new Dimension(10, 20));
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		setBorder(BorderFactory.createLoweredBevelBorder());
	}

	/**
	 * 添加一个状态栏
	 * 
	 * @param width
	 *            int
	 */
	@SuppressWarnings("unchecked")
	public void addStatusCell(int width) {
		JLabel lb = new JLabel() {
			public void paint(Graphics g) {
				super.paint(g);
				int w = getWidth();
				int h = getHeight();
				g.setColor(Color.white);
				g.drawLine(w - 4, 0, w - 4, h - 2);
				g.setColor(new Color(128, 128, 128));
				g.drawLine(w - 1, 0, w - 1, h - 5);
			}

			public Insets getInsets() {
				return new Insets(0, 0, 0, 5);
			}
		};

		lb.setPreferredSize(new Dimension(width, getPreferredSize().height));
		add(lb);
		vecCellWidth.add("" + width);
	}

	/**
	 * 取得状态条的栏数
	 * 
	 * @return int
	 */
	public int getCellCount() {
		return getComponentCount();
	}

	/**
	 * 设置状态信息
	 * 
	 * @param cellIndex
	 *            int 栏号
	 * @param status
	 *            String 状态信息
	 */
	public void setStatus(int cellIndex, String status) {
		if (getLabel(cellIndex) != null)
			getLabel(cellIndex).setText(status);
	}

	/**
	 * 取得状态栏
	 * 
	 * @param cellIndex
	 *            int 栏号
	 * @return JLabel
	 */
	private JLabel getLabel(int cellIndex) {
		if (cellIndex >= getCellCount())
			return null;
		JLabel lb = (JLabel) getComponent(cellIndex);
		return lb;
	}

	/**
	 * 取得状态信息
	 * 
	 * @param cellIndex
	 *            int 栏号
	 * @return String
	 */
	public String getStatusText(int cellIndex) {
		if (getLabel(cellIndex) != null)
			return getLabel(cellIndex).getText();
		return null;
	}

	public void paintChildren(Graphics g) {
		super.paintChildren(g);
		int w = getWidth();
		int h = getHeight();
		Color oldColor = g.getColor();
		// draw ///
		g.setColor(Color.white);
		g.drawLine(w, h - 12, w - 12, h);
		g.drawLine(w, h - 8, w - 8, h);
		g.drawLine(w, h - 4, w - 4, h);
		g.setColor(new Color(128, 128, 128));
		g.drawLine(w, h - 11, w - 11, h);
		g.drawLine(w, h - 10, w - 10, h);
		g.drawLine(w, h - 7, w - 7, h);
		g.drawLine(w, h - 6, w - 6, h);
		g.drawLine(w, h - 3, w - 3, h);
		g.drawLine(w, h - 2, w - 2, h);
		g.setColor(UIManager.getColor("Panel.background"));
		int cellW = 0;
		for (int i = 0; i < vecCellWidth.size(); i++) {
			cellW += Integer.parseInt(vecCellWidth.get(i).toString());
			g.drawLine(cellW - 2, 0, cellW, 0);
			g.drawLine(cellW - 2, 1, cellW, 1);
		}
		g.setColor(oldColor);
	}

}
