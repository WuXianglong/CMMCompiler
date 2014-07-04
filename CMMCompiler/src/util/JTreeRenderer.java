package util;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class JTreeRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 1L;
	/* 没有子结点的结点的显示图标 */
	private ImageIcon hasNoChildIcon = new ImageIcon(getClass().getResource("/images/file.png"));
	/* 有子结点的节点的显示图标 */
	private ImageIcon hasChildIcon = new ImageIcon(getClass().getResource("/images/folder.png"));

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		// 执行父类默认的节点绘制操作
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node.getUserObject().toString().trim().startsWith("错误")) {
			setForeground(Color.RED);
		}

		// 改变图标
		if (leaf)
			this.setIcon(hasNoChildIcon);
		else
			this.setIcon(hasChildIcon);
		return this;
	}

}
