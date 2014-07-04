package util;

import java.util.ArrayList;
import java.util.HashSet;
import java.awt.Color;
import java.awt.Graphics;
import java.util.StringTokenizer;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import structure.ConstVar;
import structure.Token;

import compiler.CMMLexer;

public class CodeStyle {
	// 每个字符的宽度
	private int charWidth;
	// 每个字符的高度
	private int charHeight;
	// 划定界限
	private static final String DELIM = "[] (); {}. \n\t";
	// 字体
	private static final String FAMILY = "Courier New";
	// 字体大小
	private static final int SIZE = 14;
	// 注释颜色
	private static final Color COMMENT_FOREGROUND = new Color(57, 150, 48);
	// 关键字颜色
	private static final Color KEYWORDS_FOREGROUND = new Color(127, 0, 85);
	// 变量颜色
	private static final Color VARIABLE_FOREGROUND = new Color(0, 0, 255);
	// 变量颜色
	private static final Color STRING_FOREGROUND = new Color(255, 0, 0);
	// 默认字体颜色
	private static final Color DEFAULT_FORGROUND = new Color(0, 0, 0);
	// 样式上下文
	private static final StyleContext styleContext = new StyleContext();
	// 第一行字符的Y值
	private static final int FIRST_POSITION_Y = 20;
	// 第一行以后每行字符的长度
	private static final int LINE_LENGTH_Y = 17;
	// 关键字集合
	private static final HashSet<String> keywords = new HashSet<String>();
	private ArrayList<Token> displayTokens = new ArrayList<Token>();

	private CMMLexer lexer = new CMMLexer();

	public CodeStyle() {
		// 添加默认样式
		addStyle("none");
		// 添加关键字样式
		addStyle("keywords", KEYWORDS_FOREGROUND);
		// 添加变量样式
		addStyle("variable", VARIABLE_FOREGROUND);
		// 添加字符串样式
		addStyle("string", STRING_FOREGROUND);
		// 添加注释样式
		addStyle("comment", COMMENT_FOREGROUND);
		// 添加关键字
		keywords.add("int");
		keywords.add("real");
		keywords.add("bool");
		keywords.add("string");
		keywords.add("if");
		keywords.add("else");
		keywords.add("while");
		keywords.add("read");
		keywords.add("write");
		keywords.add("true");
		keywords.add("false");
		keywords.add("for");
	}

	public int getCharWidth() {
		return charWidth;
	}

	public void setCharWidth(int charWidth) {
		this.charWidth = charWidth;
	}

	public int getCharHeight() {
		return charHeight;
	}

	public void setCharHeight(int charHeight) {
		this.charHeight = charHeight;
	}

	/**
	 * 将text填充文本到doc中.
	 * 
	 * @param text
	 *            - 要填充到StyledDocument中的文本
	 * @param doc
	 *            - 填充的StyledDocument对象
	 */
	public void markStyle(String text, StyledDocument doc, boolean cmmCompiler) {
		if (!cmmCompiler) {
			try {
				StringTokenizer tokenize = new StringTokenizer(text, DELIM,
						true);
				while (tokenize.hasMoreTokens()) {
					String str = tokenize.nextToken();
					Style s = null;
					if (keywords.contains(str.trim())) {
						s = styleContext.getStyle("keywords");
					} else if (str.trim().matches(
							"^[a-zA-Z](\\w*[a-zA-Z0-9]$)?")) {
						s = styleContext.getStyle("variable");
					} else {
						s = styleContext.getStyle("none");
					}
					doc.insertString(doc.getLength(), str, s);
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		} else {
			lexer.execute(text);
			try {
				displayTokens = lexer.getDisplayTokens();
				for (Token token : displayTokens) {
					Style s = null;
					if (keywords.contains(token.getContent())&&token.getKind().equals("关键字")) {
						s = styleContext.getStyle("keywords");
						StyleConstants.setBold(s, true);
					} else if (token.getKind().equals("标识符")) {
						s = styleContext.getStyle("variable");
						StyleConstants.setBold(s, true);
					} else if (token.getContent().equals("//")) {
						s = styleContext.getStyle("comment");
					} else if (token.getKind().equals("注释")) {
						s = styleContext.getStyle("comment");
					} else if (token.getContent().equals("/*")) {
						s = styleContext.getStyle("comment");
					} else if (token.getContent().equals("*/")) {
						s = styleContext.getStyle("comment");
					} else if (token.getContent().equals(ConstVar.DQ)) {
						s = styleContext.getStyle("string");
					} else if (token.getKind().equals("字符串")) {
						s = styleContext.getStyle("string");
					} else {
						s = styleContext.getStyle("none");
					}
					doc.insertString(doc.getLength(), token.getContent(), s);
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	protected void addStyle(String key) {
		addStyle(key, DEFAULT_FORGROUND, SIZE, FAMILY);
	}

	protected void addStyle(String key, Color color) {
		addStyle(key, color, SIZE, FAMILY);
	}

	protected void addStyle(String key, Color color, int size, String fam) {
		Style s = styleContext.addStyle(key, null);
		if (color != null)
			StyleConstants.setForeground(s, color);
		if (size > 0)
			StyleConstants.setFontSize(s, size);
		if (fam != null)
			StyleConstants.setFontFamily(s, fam);
	}

	public void drawWaveLine(Graphics g) {
		int sectionX = charWidth / 4;
		for (Token token : displayTokens) {
			if (token.getKind().equals("错误")) {
				if (token.getLine() != 1) {
					drawRedWaveLine(g, token.getCulomn() * charWidth, (token
							.getLine() - 1)
							* LINE_LENGTH_Y + FIRST_POSITION_Y, sectionX,
							sectionX, token.getContent().length());
				} else {
					drawRedWaveLine(g, token.getCulomn() * charWidth,
							FIRST_POSITION_Y, sectionX, sectionX, token
									.getContent().length());
				}
			}
		}
	}

	private void drawRedWaveLine(Graphics g, int x, int y, int sectionX,
			int sectionY, int length) {
		Color c = g.getColor();
		g.setColor(Color.red);
		boolean up = true;
		int startPositionX = x;
		int startX = 0;
		int startY = 0;
		int endX = 0;
		int endY = 0;

		while (endX < (startPositionX + length * charWidth)) {
			startX = x;
			startY = y;
			x += sectionX;
			if (up) {
				endY = y - sectionY;
				up = false;
			} else {
				endY = y + sectionY;
				up = true;
			}
			endX = x;
			y = endY;
			g.drawLine(startX, startY, endX, endY);
		}
		g.setColor(c);
	}

}