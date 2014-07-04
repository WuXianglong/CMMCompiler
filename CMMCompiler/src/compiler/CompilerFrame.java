package compiler;

/**
 * 编译器主窗体
 * 
 * @author 吴文苑
 * @version 1.4
 */
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.undo.UndoManager;

import structure.TreeNode;
import util.*;

public class CompilerFrame extends JFrame {
	private static final long serialVersionUID = 14L;
	/* 状态条 */
	private final static JStatusBar STATUSBAR = new JStatusBar();
	/* 窗体菜单栏 */
	private final static JMenuBar MENUBAR = new JMenuBar();
	/* 窗体工具条 */
	private final static JToolBar TOOLBAR = new JToolBar();
	/* 文件浏览树 */
	private final static JFileTree FILETREE = new JFileTree(
			new JFileTree.ExtensionFilter("lnk"));
	/* 默认字体 */
	private final static Font LABELFONT = new Font("幼圆", Font.BOLD, 13);
	/* 编辑区字体 */
	private Font font = new Font("Courier New", Font.PLAIN, 15);
	/* 控制台和错误列表字体 */
	private Font conAndErrFont = new Font("微软雅黑", Font.PLAIN, 14);
	/* 语法分析结果显示区字体 */
	private Font treeFont = new Font("微软雅黑", Font.PLAIN, 12);
	/* 文件菜单 */
	private static JMenu fileMenu;
	/* 编辑菜单 */
	private static JMenu editMenu;
	/* 运行菜单 */
	private static JMenu runMenu;
	/* 格式菜单 */
	private static JMenu setMenu;
	/* 窗口菜单 */
	private static JMenu windowMenu;
	/* 帮助菜单 */
	private static JMenu helpMenu;
	/* 控制台和错误信息 */
	public static JTabbedPane proAndConPanel;
	/* CMM程序文本编辑区 */
	private static JCloseableTabbedPane editTabbedPane;
	private static HashMap<JScrollPane, StyleEditor> map = new HashMap<JScrollPane, StyleEditor>();
	/* 控制台(输入与输出) */
	public static JTextPane consoleArea = new JTextPane();
	/* 错误显示区 */
	public static JTextArea problemArea = new JTextArea();
	/* 保存和打开对话框 */
	private FileDialog filedialog_save, filedialog_load;
	/* Undo管理器 */
	private final UndoManager undo = new UndoManager();
	private UndoableEditListener undoHandler = new UndoHandler();
	/* 编辑区右键菜单 */
	private JPopupMenu popupMenu = new JPopupMenu();
	private JMenuItem item1;
	private JMenuItem item2;
	private JMenuItem item3;
	private JMenuItem item4;
	/* 菜单子项 */
	private JMenuItem newItem;
	private JMenuItem openItem;
	private JMenuItem saveItem;
	private JMenuItem exitItem;
	private JMenuItem undoItem;
	private JMenuItem redoItem;
	private JMenuItem copyItem;
	private JMenuItem cutItem;
	private JMenuItem pasteItem;
	private JMenuItem allItem;
	private JMenuItem searchItem;
	private JMenuItem deleteItem;
	private JMenuItem lexItem;
	private JMenuItem parseItem;
	private JMenuItem runItem;
	private JMenuItem fontItem;
	private JMenuItem startPageItem;
	private JMenuItem newWindowItem;
	private JMenuItem helpItem;
	private JMenuItem aboutItem;
	/* 工具条按钮 */
	private JButton newButton;
	private JButton openButton;
	private JButton saveButton;
	private JButton runButton;
	private JButton lexButton;
	private JButton parseButton;
	private JButton undoButton;
	private JButton redoButton;
	private JButton copyButton;
	private JButton cutButton;
	private JButton pasteButton;
	private JButton searchButton;
	private JButton fontButton;
	private JButton helpButton;
	private JButton aboutButton;
	/* 文件过滤器 */
	FileFilter filter = new FileFilter() {
		public String getDescription() {
			return "CMM程序文件(*.cmm)";
		}

		public boolean accept(File file) {
			String tmp = file.getName().toLowerCase();
			if (tmp.endsWith(".cmm") || tmp.endsWith(".CMM")) {
				return true;
			}
			return false;
		}
	};
	/* 保存要查找的字符串 */
	private static String findStr = null;
	/* 当前文本编辑区字符串 */
	private static String text = null;
	/* 当前选择的文本的位置 */
	private static int position;
	/* 查找次数 */
	private static int time = 0;
	/* CMMLexer词法分析 */
	private CMMLexer lexer = new CMMLexer();
	/* CMMParser语法分析 */
	private CMMParser parser;
	/* CMMParser语义分析 */
	private CMMSemanticAnalysis semanticAnalysis;
	/* 词法分析语法分析结果显示面板 */
	private JTabbedPane tabbedPanel;
	/* 用户输入 */
	private String userInput;
	/* 控制台列数 */
	private static int columnNum;
	/* 控制台行数 */
	private static int rowNum;
	/* 控制台最大行数 */
	private static int presentMaxRow;
	private static int[] index = new int[] { 0, 0 };
	private static StyledDocument doc = null;

	/**
	 * 构造函数
	 * 
	 * @param title
	 */
	public CompilerFrame(String title) {
		super();
		setLayout(null);
		setTitle(title);
		setJMenuBar(MENUBAR);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI(FILETREE);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 初始化菜单项
		fileMenu = new JMenu("文件(F)");
		editMenu = new JMenu("编辑(E)");
		runMenu = new JMenu("运行(R)");
		setMenu = new JMenu("设置(S)");
		windowMenu = new JMenu("窗口(W)");
		helpMenu = new JMenu("帮助(H)");

		// 设置快捷方式
		fileMenu.setMnemonic(KeyEvent.VK_F);
		editMenu.setMnemonic(KeyEvent.VK_E);
		runMenu.setMnemonic(KeyEvent.VK_R);
		setMenu.setMnemonic(KeyEvent.VK_S);
		windowMenu.setMnemonic(KeyEvent.VK_W);
		helpMenu.setMnemonic(KeyEvent.VK_H);

		// 将菜单添加到菜单栏
		MENUBAR.add(fileMenu);
		MENUBAR.add(editMenu);
		MENUBAR.add(runMenu);
		MENUBAR.add(setMenu);
		MENUBAR.add(windowMenu);
		MENUBAR.add(helpMenu);

		// 为文件菜单添加子项
		newItem = new JMenuItem("新 建", new ImageIcon(getClass().getResource(
				"/images/new.png")));
		newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
				ActionEvent.CTRL_MASK));
		openItem = new JMenuItem("打 开", new ImageIcon(getClass().getResource(
				"/images/open.png")));
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				ActionEvent.CTRL_MASK));
		saveItem = new JMenuItem("保 存", new ImageIcon(getClass().getResource(
				"/images/save.png")));
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.CTRL_MASK));
		exitItem = new JMenuItem("退 出", new ImageIcon(getClass().getResource(
				"/images/exit.png")));
		exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
				ActionEvent.CTRL_MASK));
		fileMenu.add(newItem);
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);

		// 为编辑菜单添加子项
		undoItem = new JMenuItem("撤  销", new ImageIcon(getClass().getResource(
				"/images/undo.png")));
		undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
				ActionEvent.CTRL_MASK));
		redoItem = new JMenuItem("重  做", new ImageIcon(getClass().getResource(
				"/images/redo.png")));
		redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
				ActionEvent.CTRL_MASK));
		copyItem = new JMenuItem("复  制", new ImageIcon(getClass().getResource(
				"/images/copy.png")));
		copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				ActionEvent.CTRL_MASK));
		cutItem = new JMenuItem("剪  切", new ImageIcon(getClass().getResource(
				"/images/cut.png")));
		cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
				ActionEvent.CTRL_MASK));
		pasteItem = new JMenuItem("粘  贴", new ImageIcon(getClass().getResource(
				"/images/paste.png")));
		pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
				ActionEvent.CTRL_MASK));
		allItem = new JMenuItem("全  选", new ImageIcon(getClass().getResource(
				"/images/all.png")));
		allItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
				ActionEvent.CTRL_MASK));
		searchItem = new JMenuItem("查  找", new ImageIcon(getClass()
				.getResource("/images/search.png")));
		searchItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
				ActionEvent.CTRL_MASK));
		deleteItem = new JMenuItem("删  除", new ImageIcon(getClass()
				.getResource("/images/delete.png")));
		deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
				ActionEvent.CTRL_MASK));
		editMenu.add(undoItem);
		editMenu.add(redoItem);
		editMenu.addSeparator();
		editMenu.add(copyItem);
		editMenu.add(cutItem);
		editMenu.add(pasteItem);
		editMenu.add(deleteItem);
		editMenu.add(allItem);
		editMenu.addSeparator();
		editMenu.add(searchItem);

		// 为运行菜单添加子项
		lexItem = new JMenuItem("词法分析", new ImageIcon(getClass().getResource(
				"/images/lex.png")));
		lexItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		parseItem = new JMenuItem("语法分析", new ImageIcon(getClass().getResource(
				"/images/parse.png")));
		parseItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
		runItem = new JMenuItem("运    行", new ImageIcon(getClass().getResource(
				"/images/run.png")));
		runItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		runMenu.add(lexItem);
		runMenu.add(parseItem);
		runMenu.addSeparator();
		runMenu.add(runItem);

		// 为设置菜单添加子项
		fontItem = new JMenuItem("字 体", new ImageIcon(getClass().getResource(
				"/images/font.png")));
		fontItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
				ActionEvent.CTRL_MASK));
		setMenu.add(fontItem);

		// 为窗口菜单添加子项
		startPageItem = new JMenuItem("开始页", new ImageIcon(getClass()
				.getResource("/images/startpage.png")));
		startPageItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
				ActionEvent.ALT_MASK));
		newWindowItem = new JMenuItem("新建窗口", new ImageIcon(getClass()
				.getResource("/images/window.png")));
		newWindowItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
				ActionEvent.CTRL_MASK));
		windowMenu.add(startPageItem);
		windowMenu.add(newWindowItem);

		// 为帮助菜单添加子项
		helpItem = new JMenuItem("帮 助", new ImageIcon(getClass().getResource(
				"/images/help.png")));
		helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		aboutItem = new JMenuItem("关 于", new ImageIcon(getClass().getResource(
				"/images/about.png")));
		aboutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
				ActionEvent.CTRL_MASK));
		helpMenu.add(helpItem);
		helpMenu.add(aboutItem);

		// 设置右键菜单
		item1 = new JMenuItem("复 制    ", new ImageIcon(getClass().getResource(
				"/images/copy.png")));
		item2 = new JMenuItem("剪 切    ", new ImageIcon(getClass().getResource(
				"/images/cut.png")));
		item3 = new JMenuItem("粘 贴    ", new ImageIcon(getClass().getResource(
				"/images/paste.png")));
		item4 = new JMenuItem("全 选    ", new ImageIcon(getClass().getResource(
				"/images/all.png")));
		popupMenu.add(item1);
		popupMenu.add(item2);
		popupMenu.add(item3);
		popupMenu.addSeparator();
		popupMenu.add(item4);

		// 工具条
		newButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/new.png")));
		newButton.setToolTipText("新建");
		openButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/open.png")));
		openButton.setToolTipText("打开");
		saveButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/save.png")));
		saveButton.setToolTipText("保存");
		lexButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/lex.png")));
		lexButton.setToolTipText("词法分析");
		parseButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/parse.png")));
		parseButton.setToolTipText("语法分析");
		runButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/run.png")));
		runButton.setToolTipText("运行");
		undoButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/undo.png")));
		undoButton.setToolTipText("撤销");
		redoButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/redo.png")));
		redoButton.setToolTipText("重做");
		copyButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/copy.png")));
		copyButton.setToolTipText("复制");
		cutButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/cut.png")));
		cutButton.setToolTipText("剪切");
		pasteButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/paste.png")));
		pasteButton.setToolTipText("粘贴");
		searchButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/search.png")));
		searchButton.setToolTipText("查找");
		fontButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/font.png")));
		fontButton.setToolTipText("字体设置");
		helpButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/help.png")));
		helpButton.setToolTipText("帮助");
		aboutButton = new JButton(new ImageIcon(getClass().getResource(
				"/images/about.png")));
		aboutButton.setToolTipText("关于");
		TOOLBAR.setFloatable(false);
		TOOLBAR.add(newButton);
		TOOLBAR.add(openButton);
		TOOLBAR.add(saveButton);
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.add(lexButton);
		TOOLBAR.add(parseButton);
		TOOLBAR.add(runButton);
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.add(undoButton);
		TOOLBAR.add(redoButton);
		TOOLBAR.add(copyButton);
		TOOLBAR.add(cutButton);
		TOOLBAR.add(pasteButton);
		TOOLBAR.add(searchButton);
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.addSeparator();
		TOOLBAR.add(fontButton);
		TOOLBAR.add(helpButton);
		TOOLBAR.add(aboutButton);
		add(TOOLBAR);
		TOOLBAR.setBounds(0, 0, 1240, 33);
		TOOLBAR.setPreferredSize(getPreferredSize());

		// 文件保存和打开对话框
		filedialog_save = new FileDialog(this, "保存文件", FileDialog.SAVE);
		filedialog_save.setVisible(false);
		filedialog_load = new FileDialog(this, "打开文件", FileDialog.LOAD);
		filedialog_load.setVisible(false);
		filedialog_save.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				filedialog_save.setVisible(false);
			}
		});
		filedialog_load.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				filedialog_load.setVisible(false);
			}
		});

		// 资源管理区(文件目录浏览区)
		JPanel fileScanPanel = new JPanel(new BorderLayout());
		JLabel fileLabel = new JLabel("资源管理器");
		JPanel fileLabelPanel = new JPanel(new BorderLayout());
		JTextArea introductionArea = new JTextArea("\nCMM编译器简介：\n"
				+ "作 者: 2008级10班  吴文苑\n" + "学 号: 2008302580282\n"
				+ "课 程: 编译原理与实践\n" + "时 间: 2010年10月\n" + "版 本: V1.4\n");
		introductionArea.setFont(new Font("幼圆", Font.BOLD, 14));
		introductionArea.setBackground(getBackground());
		introductionArea.setEditable(false);
		fileLabel.setFont(LABELFONT);
		fileLabelPanel.add(fileLabel, BorderLayout.WEST);
		fileLabelPanel.setBackground(Color.LIGHT_GRAY);
		fileScanPanel.add(fileLabelPanel, BorderLayout.NORTH);
		fileScanPanel.add(new JScrollPane(FILETREE), BorderLayout.CENTER);
		fileScanPanel.add(introductionArea, BorderLayout.SOUTH);
		add(fileScanPanel);
		fileScanPanel.setBounds(0, TOOLBAR.getHeight(), 195, 768
				- TOOLBAR.getHeight() - STATUSBAR.getHeight() - 98);

		// CMM文本编辑区
		editTabbedPane = new JCloseableTabbedPane();
		editTabbedPane.setFont(treeFont);

		final StyleEditor editor = new StyleEditor();
		editor.setFont(font);
		JScrollPane scrollPane = new JScrollPane(editor);
		TextLineNumber tln = new TextLineNumber(editor);
		scrollPane.setRowHeaderView(tln);

		editor.addMouseListener(new DefaultMouseAdapter());
		editor.addCaretListener(new StatusListener());
		editor.getDocument().addUndoableEditListener(undoHandler);
		// 获得默认焦点
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent evt) {
				editor.requestFocus();
				STATUSBAR.setStatus(0, "当前行号: " + 1 + ", 当前列号: " + 1);
			}
		});
		map.put(scrollPane, editor);
		editTabbedPane.add(scrollPane, "CMMTest" + ".cmm");
		JPanel editPanel = new JPanel(null);
		editPanel.setBackground(getBackground());
		editPanel.setForeground(new Color(238, 238, 238));
		JLabel editLabel = new JLabel("|CMM程序文本编辑区");
		JPanel editLabelPanel = new JPanel(new BorderLayout());
		editLabel.setFont(LABELFONT);
		editLabelPanel.add(editLabel, BorderLayout.WEST);
		editLabelPanel.setBackground(Color.LIGHT_GRAY);

		// 控制条和错误列表区
		consoleArea.setEditable(false);
		problemArea.setRows(6);
		problemArea.setEditable(false);
		consoleArea.setFont(font);
		problemArea.setFont(conAndErrFont);
		proAndConPanel = new JTabbedPane();
		proAndConPanel.setFont(treeFont);
		proAndConPanel.add(new JScrollPane(consoleArea), "控制台");
		proAndConPanel.add(new JScrollPane(problemArea), "错误列表");

		editPanel.add(editLabelPanel);
		editPanel.add(editTabbedPane);
		editPanel.add(proAndConPanel);
		editLabelPanel.setBounds(0, 0, 815, 15);
		editTabbedPane.setBounds(0, 15, 815, 462);
		proAndConPanel.setBounds(0, 475, 815, 160);
		add(editPanel);
		editPanel.setBounds(fileScanPanel.getWidth(), TOOLBAR.getHeight(), 815,
				768 - TOOLBAR.getHeight() - STATUSBAR.getHeight() - 98);

		// 词法分析结果显示区
		JScrollPane lexerPanel = new JScrollPane(null);
		JScrollPane parserPanel = new JScrollPane(null);
		tabbedPanel = new JTabbedPane(JTabbedPane.TOP,
				JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPanel.setFont(treeFont);
		tabbedPanel.add(lexerPanel, "词法分析");
		tabbedPanel.add(parserPanel, "语法分析");
		JPanel resultPanel = new JPanel(new BorderLayout());
		JLabel resultLabel = new JLabel("|分析结果显示区");
		JPanel resultLabelPanel = new JPanel(new BorderLayout());
		resultLabel.setFont(LABELFONT);
		resultLabelPanel.add(resultLabel, BorderLayout.WEST);
		resultLabelPanel.setBackground(Color.LIGHT_GRAY);
		resultPanel.add(resultLabelPanel, BorderLayout.NORTH);
		resultPanel.add(tabbedPanel, BorderLayout.CENTER);
		add(resultPanel);
		resultPanel.setBounds(fileScanPanel.getWidth() + editPanel.getWidth(),
				TOOLBAR.getHeight(), 1200 - fileScanPanel.getWidth()
						- editPanel.getWidth() + 38, 768 - TOOLBAR.getHeight()
						- STATUSBAR.getHeight() - 98);

		// 设置状态条
		STATUSBAR.addStatusCell(6666);
		add(STATUSBAR);
		STATUSBAR.setBounds(0, TOOLBAR.getHeight() + editPanel.getHeight(),
				1240, 20);

		// 为FILETREE添加双击监听器，使其在双击一个文件时打开该文件
		FILETREE.setFont(treeFont);
		FILETREE.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					String str = "", fileName = "";
					StringBuilder text = new StringBuilder();
					File file = FILETREE.getSelectFile();
					fileName = file.getName();
					if (file.isFile()) {
						if (fileName.endsWith(".cmm")
								|| fileName.endsWith(".CMM")
								|| fileName.endsWith(".txt")
								|| fileName.endsWith(".TXT")
								|| fileName.endsWith(".java")) {
							try {
								FileReader file_reader = new FileReader(file);
								BufferedReader in = new BufferedReader(
										file_reader);
								while ((str = in.readLine()) != null)
									text.append(str + '\n');
								in.close();
								file_reader.close();
							} catch (IOException e2) {
							}
							create(fileName);
							editTabbedPane.setTitleAt(editTabbedPane
									.getComponentCount() - 1, fileName);
							map.get(editTabbedPane.getSelectedComponent())
									.setText(text.toString());
						}
					}
					setSize(getWidth(), getHeight());
				}
			}
		});

		doc = consoleArea.getStyledDocument();
		consoleArea.addKeyListener(new KeyAdapter() {

			// 按下某键
			public void keyPressed(KeyEvent e) {
				// 获得当前的行和列位置
				getCurrenRowAndCol();
				if (rowNum > presentMaxRow) {
					presentMaxRow = rowNum;
				}
				if (rowNum < presentMaxRow) {
					consoleArea.setCaretPosition(doc.getLength());
					getCurrenRowAndCol();
				}
				if (e.getKeyChar() == KeyEvent.VK_ENTER) {
					consoleArea.setCaretPosition(doc.getLength());
				}
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					if (columnNum == 1) {
						setControlArea(Color.BLACK, false);
					}
				}
			}

			// 释放某键
			public void keyReleased(KeyEvent e) {
				// 获得当前的行和列位置
				getCurrenRowAndCol();
				if (rowNum > presentMaxRow) {
					presentMaxRow = rowNum;
				}
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					// 获得光标相对0行0列的位置
					int pos = consoleArea.getCaretPosition();
					index[0] = index[1];
					index[1] = pos;
					try {
						userInput = doc.getText(index[0], index[1] - 1
								- index[0]);
						semanticAnalysis.setUserInput(userInput);
					} catch (BadLocationException e1) {
						e1.printStackTrace();
					}
					setControlArea(Color.BLACK, false);
				}
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					if (rowNum <= presentMaxRow) {
						consoleArea.setEditable(true);
					}
				}
			}
		});
		// 为菜单项添加事件监听器
		newItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent paramActionEvent) {
				create(null);
			}
		});
		openItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				open();
			}
		});
		saveItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				save();
			}
		});
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		undoItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				undo();
			}
		});
		redoItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				redo();
			}
		});
		copyItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				copy();
			}
		});
		cutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cut();
			}
		});
		pasteItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				paste();
			}
		});
		allItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selectAll();
			}
		});
		searchItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				search();
			}
		});
		deleteItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				delete();
			}
		});
		lexItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				lex();
			}
		});
		parseItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parse();
			}
		});
		runItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				run();
			}
		});
		fontItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent paramActionEvent) {
				setFont();
			}
		});
		helpItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(new JOptionPane(),
						"这是一个简单的CMM语言编译器，可以对CMM\n程序文件进行编辑、"
								+ "词法分析、语法分析，并可以\n进行编译、运行和输出程序结果， "
								+ "同时还实现了\n对程序进行出错检查的功能.", "帮助",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(new JOptionPane(),
						"|　作　　者: 国际软件学院软件工程2008级10班　吴文苑　\n"
								+ "|　学　　号: 2008302580282　\n"
								+ "|　课　　程:《编译原理与实践》　  \n"
								+ "|　设计时间: 2010年10月　　　        \n"
								+ "|　邮箱地址: wuxianglong098@163.com　　 \n",
						"关于CMM编译器", JOptionPane.INFORMATION_MESSAGE);
			}
		});

		// 为右键菜单添加事件监听器
		item1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				copy();
			}
		});
		item2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cut();
			}
		});
		item3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				paste();
			}
		});
		item4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selectAll();
			}
		});

		// 为工具条按钮添加事件监听器
		newButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent paramActionEvent) {
				create(null);
			}
		});
		openButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				open();
			}
		});
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				save();
			}
		});
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				run();
			}
		});
		lexButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				lex();
			}
		});
		parseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parse();
			}
		});
		undoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				undo();
			}
		});
		redoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				redo();
			}
		});
		copyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				copy();
			}
		});
		cutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cut();
			}
		});
		pasteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				paste();
			}
		});
		searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				search();
			}
		});
		fontButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setFont();
			}
		});
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(new JOptionPane(),
						"这是一个简单的CMM语言编译器，可以对CMM\n程序文件进行编辑、"
								+ "词法分析、语法分析，并可以\n进行编译、运行和输出程序结果， "
								+ "同时还实现了\n对程序进行出错检查的功能.", "帮助",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		aboutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(new JOptionPane(),
						"|　作　　者: 国际软件学院软件工程2008级10班　吴文苑　\n"
								+ "|　学　　号: 2008302580282　\n"
								+ "|　课　　程:《编译原理与实践》　　 \n"
								+ "|　设计时间: 2010年10月　　　　　　\n"
								+ "|　邮箱地址: wuxianglong098@163.com　　\n",
						"关于CMM编译器", JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}

	// 内部类：监听鼠标右键
	class DefaultMouseAdapter extends MouseAdapter {
		public void mouseReleased(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e)) {
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	// 内部类：Undo管理
	class UndoHandler implements UndoableEditListener {
		public void undoableEditHappened(UndoableEditEvent e) {
			undo.addEdit(e.getEdit());
		}
	}

	// 内部类：控制状态条的显示
	class StatusListener implements CaretListener {
		public void caretUpdate(CaretEvent e) {
			StyleEditor temp = map.get(editTabbedPane.getSelectedComponent());
			try {
				int row = temp.getLineOfOffset(e.getDot());
				int column = e.getDot() - temp.getLineStartOffset(row);
				STATUSBAR.setStatus(0, "当前行号: " + (row + 1) + ", 当前列号: "
						+ (column + 1));
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	// 词法分析：对程序的词法进行分析，输出分析结果
	public void lex() {
		StyleEditor textArea = map.get(editTabbedPane.getSelectedComponent());
		String text = textArea.getText();

		if (text.equals("")) {
			JOptionPane.showMessageDialog(new JPanel(), "请确认输入CMM程序不为空！");
		} else {
			TreeNode root = lexer.execute(text);
			DefaultTreeModel model = new DefaultTreeModel(root);
			JTree lexerTree = new JTree(model);
			// 设置该JTree使用自定义的节点绘制器
			lexerTree.setCellRenderer(new JTreeRenderer());
			// 设置是否显示根节点的“展开/折叠”图标,默认是false
			lexerTree.setShowsRootHandles(true);
			// 设置节点是否可见,默认是true
			lexerTree.setRootVisible(true);
			// 设置字体
			lexerTree.setFont(treeFont);

			tabbedPanel.setComponentAt(0, new JScrollPane(lexerTree));
			tabbedPanel.setSelectedIndex(0);
			problemArea.setText("**********词法分析结果**********\n");
			problemArea.append(lexer.getErrorInfo());
			problemArea.append("该程序中共有" + lexer.getErrorNum() + "个词法错误！\n");
			proAndConPanel.setSelectedIndex(1);
		}
	}

	// 语法分析：对程序的语法进行分析，并显示语法树
	public TreeNode parse() {
		lex();
		if (lexer.getErrorNum() != 0) {
			JOptionPane.showMessageDialog(new JPanel(),
					"词法分析出现错误！请先修改程序再进行语法分析！", "语法分析",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
		parser = new CMMParser(lexer.getTokens());
		parser.setIndex(0);
		parser.setErrorInfo("");
		parser.setErrorNum(0);
		TreeNode root = parser.execute();
		DefaultTreeModel model = new DefaultTreeModel(root);
		JTree parserTree = new JTree(model);
		// 设置该JTree使用自定义的节点绘制器
		parserTree.setCellRenderer(new JTreeRenderer());

		// 设置是否显示根节点的“展开/折叠”图标,默认是false
		parserTree.setShowsRootHandles(true);
		// 设置节点是否可见,默认是true
		parserTree.setRootVisible(true);
		// 设置字体
		parserTree.setFont(treeFont);
		problemArea.append("\n");
		problemArea.append("**********语法分析结果**********\n");
		if (parser.getErrorNum() != 0) {
			problemArea.append(parser.getErrorInfo());
			problemArea.append("该程序中共有" + parser.getErrorNum() + "个语法错误！\n");
			JOptionPane.showMessageDialog(new JPanel(), "程序进行语法分析时发现错误，请修改！",
					"语法分析", JOptionPane.ERROR_MESSAGE);
		} else {
			problemArea.append("该程序中共有" + parser.getErrorNum() + "个语法错误！\n");
		}
		tabbedPanel.setComponentAt(1, new JScrollPane(parserTree));
		tabbedPanel.setSelectedIndex(1);
		proAndConPanel.setSelectedIndex(1);
		return root;
	}

	// 运行：分析并运行CMM程序，显示运行结果
	public void run() {
		consoleArea.setText(null);
		columnNum = 0;
		rowNum = 0;
		presentMaxRow = 0;
		index = new int[] { 0, 0 };
		TreeNode node = parse();
		if (lexer.getErrorNum() != 0) {
			return;
		} else if (parser.getErrorNum() != 0 || node == null) {
			return;
		} else {
			semanticAnalysis = new CMMSemanticAnalysis(node);
			semanticAnalysis.start();
		}
	}

	// 新建
	private void create(String filename) {
		if (filename == null) {
			filename = JOptionPane.showInputDialog("请输入新建文件的名字.(后缀名为.cmm)");
			if (filename == null || filename.equals("")) {
				JOptionPane.showMessageDialog(null, "文件名不能为空!");
				return;
			}
		}
		filename += ".cmm";
		StyleEditor editor = new StyleEditor();
		editor.setFont(font);
		JScrollPane scrollPane = new JScrollPane(editor);
		TextLineNumber tln = new TextLineNumber(editor);
		scrollPane.setRowHeaderView(tln);

		editor.addMouseListener(new DefaultMouseAdapter());
		editor.addCaretListener(new StatusListener());
		editor.getDocument().addUndoableEditListener(undoHandler);
		map.put(scrollPane, editor);
		editTabbedPane.add(scrollPane, filename);
		editTabbedPane.setSelectedIndex(editTabbedPane.getTabCount() - 1);
	}

	// 打开
	private void open() {
		boolean isOpened = false;
		String str = "", fileName = "";
		File file = null;
		StringBuilder text = new StringBuilder();
		filedialog_load.setVisible(true);
		if (filedialog_load.getFile() != null) {
			try {
				file = new File(filedialog_load.getDirectory(), filedialog_load
						.getFile());
				fileName = file.getName();
				FileReader file_reader = new FileReader(file);
				BufferedReader in = new BufferedReader(file_reader);
				while ((str = in.readLine()) != null)
					text.append(str + '\n');
				in.close();
				file_reader.close();
			} catch (IOException e2) {
			}
			for (int i = 0; i < editTabbedPane.getComponentCount(); i++) {
				if (editTabbedPane.getTitleAt(i).equals(fileName)) {
					isOpened = true;
					editTabbedPane.setSelectedIndex(i);
				}
			}
			if (!isOpened) {
				create(fileName);
				editTabbedPane.setTitleAt(
						editTabbedPane.getComponentCount() - 1, fileName);
				map.get(editTabbedPane.getSelectedComponent()).setText(
						text.toString());
			}

		}
	}

	// 保存
	private void save() {
		StyleEditor temp = map.get(editTabbedPane.getSelectedComponent());
		if (temp.getText() != null) {
			filedialog_save.setVisible(true);
			if (filedialog_save.getFile() != null) {
				try {
					File file = new File(filedialog_save.getDirectory(),
							filedialog_save.getFile());
					FileWriter fw = new FileWriter(file);
					fw.write(map.get(editTabbedPane.getSelectedComponent())
							.getText());
					fw.close();
				} catch (IOException e2) {
				}
			}
		}
	}

	// 撤销
	private void undo() {
		if (undo.canUndo()) {
			try {
				undo.undo();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 重做
	private void redo() {
		if (undo.canRedo()) {
			try {
				undo.redo();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 复制
	private void copy() {
		StyleEditor temp = map.get(editTabbedPane.getSelectedComponent());
		temp.copy();
	}

	// 剪切
	private void cut() {
		StyleEditor temp = map.get(editTabbedPane.getSelectedComponent());
		temp.cut();
	}

	// 粘贴
	private void paste() {
		StyleEditor temp = map.get(editTabbedPane.getSelectedComponent());
		temp.paste();
	}

	// 查找
	private void search() {
		StyleEditor temp = map.get(editTabbedPane.getSelectedComponent());
		if (text == null)
			text = temp.getText();
		if (findStr == null)
			findStr = JOptionPane.showInputDialog(this, "请输入要找的字符串!");
		if (findStr != null) {
			position = text.indexOf(findStr);
			if (text.equals("")) {
				JOptionPane.showMessageDialog(this, "没有你要查找的字符串！");
				findStr = null;
			} else {
				if (position != -1) {
					temp.select(position + findStr.length() * time, position
							+ findStr.length() * (time + 1));
					temp.setSelectedTextColor(Color.RED);
					text = new String(text.substring(position
							+ findStr.length()));
					time += 1;
				} else {
					JOptionPane.showMessageDialog(this, "没有你要查找的字符串！");
					time = 0;
					text = null;
					findStr = null;
				}
			}
		}
	}

	// 全选
	private void selectAll() {
		StyleEditor temp = map.get(editTabbedPane.getSelectedComponent());
		temp.selectAll();
	}

	// 删除
	private void delete() {
		StyleEditor temp = map.get(editTabbedPane.getSelectedComponent());
		temp.replaceSelection("");
	}

	// 设置字体
	private void setFont() {
		font = JFontDialog
				.showDialog(getContentPane(), "字体设置", true, getFont());
		for (int i = 0; i < editTabbedPane.getComponentCount(); i++)
			map.get(editTabbedPane.getComponent(i)).setFont(font);
	}

	private void getCurrenRowAndCol() {
		int row = 0;
		int col = 0;
		// 获得光标相对0行0列的位置
		int pos = consoleArea.getCaretPosition();
		Element root = consoleArea.getDocument().getDefaultRootElement();
		int index = root.getElementIndex(doc.getParagraphElement(pos)
				.getStartOffset());
		// 列!!!
		try {
			col = pos
					- doc.getText(0, doc.getLength()).substring(0, pos)
							.lastIndexOf("\n");
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		// 行!!!
		try {
			// 返回行是从0算起的,所以+1
			row = Integer.parseInt(String.valueOf(index + 1));
		} catch (Exception e) {
			e.printStackTrace();
		}
		rowNum = row;
		columnNum = col;
		presentMaxRow = root.getElementIndex(doc.getParagraphElement(
				doc.getLength()).getStartOffset()) + 1;
	}

	// 改变controlArea的颜色与编辑属性
	public static void setControlArea(Color c, boolean edit) {
		proAndConPanel.setSelectedIndex(0);
		consoleArea.setFocusable(true);
		consoleArea.setForeground(c);
		consoleArea.setEditable(edit);
	}

	// 主函数
	public static void main(String[] args) {
		CompilerFrame frame = new CompilerFrame("CMM解释器");
		frame.setBounds(60, 0, 1240, 742);
		frame.setResizable(false);
		frame.setVisible(true);
	}

}
