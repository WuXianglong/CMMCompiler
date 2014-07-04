package util;

import java.io.*;
import java.util.*;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.tree.*;

/**
 * <p>
 * Title: JFileTree
 * </p>
 * <p>
 * Description: JFileTree 文件树
 * </p>
 * 
 * @author 吴
 * @version 1.0
 */

@SuppressWarnings("serial")
public class JFileTree extends JTree implements Serializable {
	private static final FileSystemView fileSystemView = FileSystemView
			.getFileSystemView();
	private DefaultTreeModel treeModel;

	/**
	 * 指定过滤器的文件树
	 * 
	 * @param filter
	 *            FileFilter 指定过滤器
	 */
	public JFileTree(java.io.FileFilter filter) {
		FileNode root = new FileNode(fileSystemView.getRoots()[0], filter);
		treeModel = new DefaultTreeModel(root);
		root.explore();
		treeModel.nodeStructureChanged(root);
		this.setModel(treeModel);

		addTreeExpansionListener(new JFileTreeExpandsionListener());
		setCellRenderer(new JFileTreeCellRenderer());
	}

	/**
	 * 取得当前选择的节点
	 * 
	 * @return FileNode
	 */
	public FileNode getSelectFileNode() {
		TreePath path = getSelectionPath();
		if (path == null || path.getLastPathComponent() == null) {
			return null;
		}
		return (FileNode) path.getLastPathComponent();
	}

	/**
	 * 设置当前选择的节点
	 * 
	 * @param f
	 *            FileNode
	 * @throws Exception
	 */
	public void setSelectFileNode(FileNode f) throws Exception {
		this.setSelectFile(f.getFile());
	}

	/**
	 * 取得当前选择的文件或目录
	 * 
	 * @return File
	 */
	public File getSelectFile() {
		FileNode node = getSelectFileNode();
		return node == null ? null : node.getFile();
	}

	/**
	 * 设置当前选择的文件或目录
	 * 
	 * @param f
	 *            File
	 * @throws Exception
	 */
	public void setSelectFile(File f) throws Exception {
		FileNode node = this.expandFile(f);
		TreePath path = new TreePath(node.getPath());
		this.scrollPathToVisible(path);
		this.setSelectionPath(path);
		this.repaint();
	}

	/**
	 * 展开指定的文件或目录
	 * 
	 * @param f
	 *            File
	 * @return FileNode
	 * @throws Exception
	 */
	public FileNode expandFile(File f) throws Exception {
		if (!f.exists()) {
			throw new java.io.FileNotFoundException(f.getAbsolutePath());
		}
		Vector<File> vTemp = new Vector<File>();
		File fTemp = f;
		while (fTemp != null) {
			vTemp.add(fTemp);
			fTemp = fileSystemView.getParentDirectory(fTemp);
		}

		FileNode nParent = (FileNode) treeModel.getRoot();
		for (int i = vTemp.size() - 1; i >= 0; i--) {
			fTemp = (File) vTemp.get(i);
			nParent.explore();
			for (int j = 0; j < nParent.getChildCount(); j++) {
				FileNode nChild = (FileNode) nParent.getChildAt(j);
				if (nChild.getFile().equals(fTemp)) {
					nParent = nChild;
				}
			}
		}
		return nParent;
	}

	/**
	 * 
	 * <p>
	 * Description: 文件树修饰器
	 * </p>
	 * 
	 * @author 吴
	 * @version 1.0
	 */
	class JFileTreeCellRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded,
					leaf, row, hasFocus);
			try {
				closedIcon = fileSystemView.getSystemIcon(((FileNode) value)
						.getFile());
				openIcon = closedIcon;
				setIcon(closedIcon);
			} catch (Exception ex) {
			}
			return this;
		}
	}

	/**
	 * 
	 * <p>
	 * Description: 文件树展开事件监听器
	 * </p>
	 * 
	 * @author 吴
	 * @version 1.0
	 */
	class JFileTreeExpandsionListener implements TreeExpansionListener {
		public JFileTreeExpandsionListener() {
		}

		public void treeExpanded(TreeExpansionEvent event) {
			TreePath path = event.getPath();
			if (path == null || path.getLastPathComponent() == null)
				return;
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			FileNode node = (FileNode) path.getLastPathComponent();
			node.explore();
			JTree tree = (JTree) event.getSource();
			DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
			treeModel.nodeStructureChanged(node);
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}

		public void treeCollapsed(TreeExpansionEvent event) {
		}
	}

	/**
	 * 
	 * <p>
	 * Description:文件节点
	 * </p>
	 * 
	 * @author 吴
	 * @version 1.0
	 */
	public static class FileNode extends DefaultMutableTreeNode {
		private boolean explored = false;
		private java.io.FileFilter filter = null;

		public FileNode(File file, java.io.FileFilter filter) {
			if (filter == null) {
				this.filter = new AllFileFilter();
			} else {
				this.filter = filter;
			}
			setUserObject(file);
		}

		public boolean getAllowsChildren() {
			return isDirectory();
		}

		public boolean isDirectory() {
			return !isLeaf();
		}

		public boolean isLeaf() {
			return getFile().isFile();
		}

		public File getFile() {
			return (File) getUserObject();
		}

		public boolean isExplored() {
			return explored;
		}

		public void setExplored(boolean b) {
			explored = b;
		}

		public String toString() {
			if (getFile() instanceof File)
				return fileSystemView.getSystemDisplayName((File) getFile());
			else
				return getFile().toString();
		}

		/**
		 * 展开节点
		 */
		public void explore() {
			if (!explored) {
				explored = true;
				File file = getFile();
				// 如果这里使用 file.listFiles(filter) 有BUG
				File[] children = file.listFiles();
				if (children == null || children.length == 0) {
					return;
				}
				// 过滤后排序,选加入排序后的目录, 再加入排序后的文件
				ArrayList<File> listDir = new ArrayList<File>();
				ArrayList<File> listFile = new ArrayList<File>();
				for (int i = 0; i < children.length; ++i) {
					File f = children[i];
					if (filter.accept(f)) {
						if (f.isDirectory()) {
							listDir.add(f);
						} else {
							listFile.add(f);
						}
					}
				}
				Collections.sort(listDir);
				Collections.sort(listFile);
				for (int i = 0; i < listDir.size(); i++) {
					add(new FileNode((File) listDir.get(i), filter));
				}
				for (int i = 0; i < listFile.size(); i++) {
					add(new FileNode((File) listFile.get(i), filter));
				}
			}
		}
	}

	/**
	 * 
	 * <p>
	 * Description: 所有文件过滤器
	 * </p>
	 * 
	 * @author 吴
	 * @version 1.0
	 */
	public static class AllFileFilter implements java.io.FileFilter {
		public boolean accept(File pathname) {
			return true;
		}
	}

	/**
	 * 
	 * <p>
	 * Description:扩展名过滤器
	 * </p>
	 * 
	 * @author 吴
	 * @version 1.0
	 */
	public static class ExtensionFilter implements java.io.FileFilter {
		String extension;

		public ExtensionFilter(String extension) {
			this.extension = extension.toLowerCase();
		}

		public boolean accept(File pathname) {
			if (pathname.isDirectory()) {
				return true;
			}
			String name = pathname.getName().toLowerCase();
			if (!name.endsWith(extension)) {
				return true;
			}
			return false;
		}
	}

}
