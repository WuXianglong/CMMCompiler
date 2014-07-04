package compiler;

import java.awt.Color;
import java.math.BigDecimal;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import structure.ConstVar;
import structure.SymbolTable;
import structure.SymbolTableElement;
import structure.TreeNode;

/**
 * CMM语义分析器
 * 
 * @author 吴文苑
 * @version 1.4
 */
public class CMMSemanticAnalysis extends Thread {
	/* 语义分析时的符号表 */
	private SymbolTable table = new SymbolTable();
	/* 语法分析得到的抽象语法树 */
	private TreeNode root;
	/* 语义分析错误信息 */
	private String errorInfo = "";
	/* 语义分析错误个数 */
	private int errorNum = 0;
	/* 语义分析标识符作用域 */
	private int level = 0;
	/* 用户输入 */
	private String userInput;

	public CMMSemanticAnalysis(TreeNode root) {
		this.root = root;
	}

	public void error(String error, int line) {
		errorNum++;
		String s = ConstVar.ERROR + "第 " + line + " 行：" + error + "\n";
		errorInfo += s;
	}

	/**
	 * 识别正确的整数：排除多个零的情况
	 * 
	 * @param input
	 *            要识别的字符串
	 * @return 布尔值
	 */
	private static boolean matchInteger(String input) {
		if (input.matches("^-?\\d+$") && !input.matches("^-?0{1,}\\d+$"))
			return true;
		else
			return false;
	}

	/**
	 * 识别正确的浮点数：排除00.000的情况
	 * 
	 * @param input
	 *            要识别的字符串
	 * @return 布尔值
	 */
	private static boolean matchReal(String input) {
		if (input.matches("^(-?\\d+)(\\.\\d+)+$")
				&& !input.matches("^(-?0{2,}+)(\\.\\d+)+$"))
			return true;
		else
			return false;
	}

	/**
	 * 设置用户输入
	 * 
	 * @param userInput
	 *            输入的内容
	 */
	public synchronized void setUserInput(String userInput) {
		this.userInput = userInput;
		notify();
	}

	/**
	 * 读取用户输入
	 * 
	 * @return 返回用户输入内容的字符串形式
	 */
	public synchronized String readInput() {
		String result = null;
		try {
			while (userInput == null) {
				wait();
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		result = userInput;
		userInput = null;
		return result;
	}

	/**
	 * 进程运行时执行的方法
	 */
	public void run() {
		table.removeAll();
		statement(root);
		CompilerFrame.problemArea.append("\n");
		CompilerFrame.problemArea.append("**********语义分析结果**********\n");
		if (errorNum != 0) {
			CompilerFrame.problemArea.append(errorInfo);
			CompilerFrame.problemArea.append("该程序中共有" + errorNum + "个语义错误！\n");
			CompilerFrame.proAndConPanel.setSelectedIndex(1);
			JOptionPane.showMessageDialog(new JPanel(), "程序进行语义分析时发现错误，请修改！",
					"语义分析", JOptionPane.ERROR_MESSAGE);
		} else {
			CompilerFrame.problemArea.append("该程序中共有" + errorNum + "个语义错误！\n");
			CompilerFrame.proAndConPanel.setSelectedIndex(0);
		}
	}

	/**
	 * 语义分析主方法
	 * 
	 * @param root
	 *            根结点
	 */
	private void statement(TreeNode root) {
		for (int i = 0; i < root.getChildCount(); i++) {
			TreeNode currentNode = root.getChildAt(i);
			String content = currentNode.getContent();
			if (content.equals(ConstVar.INT) || content.equals(ConstVar.REAL)
					|| content.equals(ConstVar.BOOL)
					|| content.equals(ConstVar.STRING)) {
				forDeclare(currentNode);
			} else if (content.equals(ConstVar.ASSIGN)) {
				forAssign(currentNode);
			} else if (content.equals(ConstVar.FOR)) {
				// 进入for循环语句，改变作用域
				level++;
				forFor(currentNode);
				// 退出for循环语句，改变作用域并更新符号表
				level--;
				table.update(level);
			}  else if (content.equals(ConstVar.IF)) {
				// 进入if语句，改变作用域
				level++;
				forIf(currentNode);
				// 退出if语句，改变作用域并更新符号表
				level--;
				table.update(level);
			} else if (content.equals(ConstVar.WHILE)) {
				// 进入while语句，改变作用域
				level++;
				forWhile(currentNode);
				// 退出while语句，改变作用域并更新符号表
				level--;
				table.update(level);
			} else if (content.equals(ConstVar.READ)) {
				forRead(currentNode.getChildAt(0));
			} else if (content.equals(ConstVar.WRITE)) {
				forWrite(currentNode.getChildAt(0));
			}
		}
	}

	/**
	 * 分析declare语句
	 * 
	 * @param root
	 *            根结点
	 */
	private void forDeclare(TreeNode root) {
		// 结点显示的内容,即声明变量的类型int real bool string
		String content = root.getContent();
		int index = 0;
		while (index < root.getChildCount()) {
			TreeNode temp = root.getChildAt(index);
			// 变量名
			String name = temp.getContent();
			// 判断变量是否已经被声明
			if (table.getCurrentLevel(name, level) == null) {
				// 声明普通变量
				if (temp.getChildCount() == 0) {
					SymbolTableElement element = new SymbolTableElement(temp
							.getContent(), content, temp.getLineNum(), level);
					index++;
					// 判断变量是否在声明时被初始化
					if (index < root.getChildCount()
							&& root.getChildAt(index).getContent().equals(
									ConstVar.ASSIGN)) {
						// 获得变量的初始值结点
						TreeNode valueNode = root.getChildAt(index).getChildAt(
								0);
						String value = valueNode.getContent();
						if (content.equals(ConstVar.INT)) { // 声明int型变量
							if (matchInteger(value)) {
								element.setIntValue(value);
								element.setRealValue(String.valueOf(Double
										.parseDouble(value)));
							} else if (matchReal(value)) {
								String error = "不能将浮点数赋值给整型变量";
								error(error, valueNode.getLineNum());
							} else if (value.equals("true")
									|| value.equals("false")) {
								String error = "不能将" + value + "赋值给整型变量";
								error(error, valueNode.getLineNum());
							} else if (valueNode.getNodeKind().equals("字符串")) {
								String error = "不能将字符串赋值给整型变量";
								error(error, valueNode.getLineNum());
							} else if (valueNode.getNodeKind().equals("标识符")) {
								if (checkID(valueNode, level)) {
									if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.INT)) {
										element.setIntValue(table.getAllLevel(
												valueNode.getContent(), level)
												.getIntValue());
										element.setRealValue(table.getAllLevel(
												valueNode.getContent(), level)
												.getRealValue());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.REAL)) {
										String error = "不能将浮点型变量赋值给整型变量";
										error(error, valueNode.getLineNum());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.BOOL)) {
										String error = "不能将布尔型变量赋值给整型变量";
										error(error, valueNode.getLineNum());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.STRING)) {
										String error = "不能将字符串变量赋值给整型变量";
										error(error, valueNode.getLineNum());
									}
								} else {
									return;
								}
							} else if (value.equals(ConstVar.PLUS)
									|| value.equals(ConstVar.MINUS)
									|| value.equals(ConstVar.TIMES)
									|| value.equals(ConstVar.DIVIDE)) {
								String result = forExpression(valueNode);
								if (result != null) {
									if (matchInteger(result)) {
										element.setIntValue(result);
										element.setRealValue(String
												.valueOf(Double
														.parseDouble(result)));
									} else if (matchReal(result)) {
										String error = "不能将浮点数赋值给整型变量";
										error(error, valueNode.getLineNum());
										return;
									} else {
										return;
									}
								} else {
									return;
								}
							}
						} else if (content.equals(ConstVar.REAL)) { // 声明real型变量
							if (matchInteger(value)) {
								element.setRealValue(String.valueOf(Double
										.parseDouble(value)));
							} else if (matchReal(value)) {
								element.setRealValue(value);
							} else if (value.equals("true")
									|| value.equals("false")) {
								String error = "不能将" + value + "赋值给浮点型变量";
								error(error, valueNode.getLineNum());
							} else if (valueNode.getNodeKind().equals("字符串")) {
								String error = "不能将字符串给浮点型变量";
								error(error, valueNode.getLineNum());
							} else if (valueNode.getNodeKind().equals("标识符")) {
								if (checkID(valueNode, level)) {
									if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.INT)
											|| table.getAllLevel(
													valueNode.getContent(),
													level).getKind().equals(
													ConstVar.REAL)) {
										element.setRealValue(table.getAllLevel(
												valueNode.getContent(), level)
												.getRealValue());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.BOOL)) {
										String error = "不能将布尔型变量赋值给浮点型变量";
										error(error, valueNode.getLineNum());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.STRING)) {
										String error = "不能将字符串变量赋值给浮点型变量";
										error(error, valueNode.getLineNum());
									}
								} else {
									return;
								}
							} else if (value.equals(ConstVar.PLUS)
									|| value.equals(ConstVar.MINUS)
									|| value.equals(ConstVar.TIMES)
									|| value.equals(ConstVar.DIVIDE)) {
								String result = forExpression(valueNode);
								if (result != null) {
									if (matchInteger(result)) {
										element.setRealValue(String
												.valueOf(Double
														.parseDouble(result)));
									} else if (matchReal(result)) {
										element.setRealValue(result);
									}
								} else {
									return;
								}
							}
						} else if (content.equals(ConstVar.STRING)) { // 声明string型变量
							if (matchInteger(value)) {
								String error = "不能将整数赋值给字符串型变量";
								error(error, valueNode.getLineNum());
							} else if (matchReal(value)) {
								String error = "不能将浮点数赋值给字符串型变量";
								error(error, valueNode.getLineNum());
							} else if (value.equals("true")
									|| value.equals("false")) {
								String error = "不能将" + value + "赋值给字符串型变量";
								error(error, valueNode.getLineNum());
							} else if (valueNode.getNodeKind().equals("字符串")) {
								element.setStringValue(value);
							} else if (valueNode.getNodeKind().equals("标识符")) {
								if (checkID(valueNode, level)) {
									if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.INT)) {
										String error = "不能将整数赋值给字符串型变量";
										error(error, valueNode.getLineNum());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.REAL)) {
										String error = "不能将浮点数赋值给字符串型变量";
										error(error, valueNode.getLineNum());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.BOOL)) {
										String error = "不能将布尔型变量赋值给字符串型变量";
										error(error, valueNode.getLineNum());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.STRING)) {
										element.setStringValue(value);
									}
								} else {
									return;
								}
							} else if (value.equals(ConstVar.PLUS)
									|| value.equals(ConstVar.MINUS)
									|| value.equals(ConstVar.TIMES)
									|| value.equals(ConstVar.DIVIDE)) {
								String error = "不能将算术表达式赋值给字符串型变量";
								error(error, valueNode.getLineNum());
							}
						} else { // 声明bool型变量
							if (matchInteger(value)) {
								// 如果是0或负数则记为false,其他记为true
								int i = Integer.parseInt(value);
								if (i <= 0)
									element.setStringValue("false");
								else
									element.setStringValue("true");
							} else if (matchReal(value)) {
								String error = "不能将浮点数赋值给布尔型变量";
								error(error, valueNode.getLineNum());
							} else if (value.equals("true")
									|| value.equals("false")) {
								element.setStringValue(value);
							} else if (valueNode.getNodeKind().equals("字符串")) {
								String error = "不能将字符串给布尔型变量";
								error(error, valueNode.getLineNum());
							} else if (valueNode.getNodeKind().equals("标识符")) {
								if (checkID(valueNode, level)) {
									if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.INT)) {
										int i = Integer.parseInt(table
												.getAllLevel(
														valueNode.getContent(),
														level).getIntValue());
										if (i <= 0)
											element.setStringValue("false");
										else
											element.setStringValue("true");
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.REAL)) {
										String error = "不能将浮点型变量赋值给布尔型变量";
										error(error, valueNode.getLineNum());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.BOOL)) {
										element
												.setStringValue(table
														.getAllLevel(
																valueNode
																		.getContent(),
																level)
														.getStringValue());
									} else if (table.getAllLevel(
											valueNode.getContent(), level)
											.getKind().equals(ConstVar.STRING)) {
										String error = "不能将字符串变量赋值给布尔型变量";
										error(error, valueNode.getLineNum());
									}
								} else {
									return;
								}
							} else if (value.equals(ConstVar.EQUAL)
									|| value.equals(ConstVar.NEQUAL)
									|| value.equals(ConstVar.LT)
									|| value.equals(ConstVar.GT)) {
								boolean result = forCondition(valueNode);
								if (result) {
									element.setStringValue("true");
								} else {
									element.setStringValue("false");
								}
							}
						}
						index++;
					}
					table.add(element);
				} else { // 声明数组
					SymbolTableElement element = new SymbolTableElement(temp
							.getContent(), content, temp.getLineNum(), level);
					String sizeValue = temp.getChildAt(0).getContent();
					if (matchInteger(sizeValue)) {
						int i = Integer.parseInt(sizeValue);
						if (i < 1) {
							String error = "数组大小必须大于零";
							error(error, root.getLineNum());
							return;
						}
					} else if (temp.getChildAt(0).getNodeKind().equals("标识符")) {
						if (checkID(root, level)) {
							SymbolTableElement tempElement = table.getAllLevel(
									root.getContent(), level);
							if (tempElement.getKind().equals(ConstVar.INT)) {
								int i = Integer.parseInt(tempElement
										.getIntValue());
								if (i < 1) {
									String error = "数组大小必须大于零";
									error(error, root.getLineNum());
									return;
								} else {
									sizeValue = tempElement.getIntValue();
								}
							} else {
								String error = "类型不匹配,数组大小必须为整数类型";
								error(error, root.getLineNum());
								return;
							}
						} else {
							return;
						}
					} else if (sizeValue.equals(ConstVar.PLUS)
							|| sizeValue.equals(ConstVar.MINUS)
							|| sizeValue.equals(ConstVar.TIMES)
							|| sizeValue.equals(ConstVar.DIVIDE)) {
						sizeValue = forExpression(temp.getChildAt(0));
						if (sizeValue != null) {
							if (matchInteger(sizeValue)) {
								int i = Integer.parseInt(sizeValue);
								if (i < 1) {
									String error = "数组大小必须大于零";
									error(error, root.getLineNum());
									return;
								}
							} else {
								String error = "类型不匹配,数组大小必须为整数类型";
								error(error, root.getLineNum());
								return;
							}
						} else {
							return;
						}
					}
					element.setArrayElementsNum(Integer.parseInt(sizeValue));
					table.add(element);
					index++;
					for (int j = 0; j < Integer.parseInt(sizeValue); j++) {
						String s = temp.getContent() + "@" + j;
						SymbolTableElement ste = new SymbolTableElement(s,
								content, temp.getLineNum(), level);
						table.add(ste);
					}
				}
			} else { // 报错
				String error = "变量" + name + "已被声明,请重命名该变量";
				error(error, temp.getLineNum());
				return;
			}
		}
	}

	/**
	 * 分析assign语句
	 * 
	 * @param root
	 *            语法树中assign语句结点
	 */
	private void forAssign(TreeNode root) {
		// 赋值语句左半部分
		TreeNode node1 = root.getChildAt(0);
		// 赋值语句左半部分标识符
		String node1Value = node1.getContent();
		if (table.getAllLevel(node1Value, level) != null) {
			if (node1.getChildCount() != 0) {
				String s = forArray(node1.getChildAt(0), table.getAllLevel(
						node1Value, level).getArrayElementsNum());
				if (s != null)
					node1Value += "@" + s;
				else
					return;
			}
		} else {
			String error = "变量" + node1Value + "在使用前未声明";
			error(error, node1.getLineNum());
			return;
		}
		// 赋值语句左半部分标识符类型
		String node1Kind = table.getAllLevel(node1Value, level).getKind();
		// 赋值语句右半部分
		TreeNode node2 = root.getChildAt(1);
		String node2Kind = node2.getNodeKind();
		String node2Value = node2.getContent();
		// 赋值语句右半部分的值
		String value = "";
		if (node2Kind.equals("整数")) { // 整数
			value = node2Value;
			node2Kind = "int";
		} else if (node2Kind.equals("实数")) { // 实数
			value = node2Value;
			node2Kind = "real";
		} else if (node2Kind.equals("字符串")) { // 字符串
			value = node2Value;
			node2Kind = "string";
		} else if (node2Kind.equals("布尔值")) { // true和false
			value = node2Value;
			node2Kind = "bool";
		} else if (node2Kind.equals("标识符")) { // 标识符
			if (checkID(node2, level)) {
				if (node2.getChildCount() != 0) {
					String s = forArray(node2.getChildAt(0), table.getAllLevel(
							node2Value, level).getArrayElementsNum());
					if (s != null)
						node2Value += "@" + s;
					else
						return;
				}
				SymbolTableElement temp = table.getAllLevel(node2Value, level);
				if (temp.getKind().equals(ConstVar.INT)) {
					value = temp.getIntValue();
				} else if (temp.getKind().equals(ConstVar.REAL)) {
					value = temp.getRealValue();
				} else if (temp.getKind().equals(ConstVar.BOOL)
						|| temp.getKind().equals(ConstVar.STRING)) {
					value = temp.getStringValue();
				}
				node2Kind = table.getAllLevel(node2Value, level).getKind();
			} else {
				return;
			}
		} else if (node2Value.equals(ConstVar.PLUS)
				|| node2Value.equals(ConstVar.MINUS)
				|| node2Value.equals(ConstVar.TIMES)
				|| node2Value.equals(ConstVar.DIVIDE)) { // 表达式
			String result = forExpression(node2);
			if (result != null) {
				if (matchInteger(result))
					node2Kind = "int";
				else if (matchReal(result))
					node2Kind = "real";
				value = result;
			} else {
				return;
			}
		} else if (node2Value.equals(ConstVar.EQUAL)
				|| node2Value.equals(ConstVar.NEQUAL)
				|| node2Value.equals(ConstVar.LT)
				|| node2Value.equals(ConstVar.GT)) { // 逻辑表达式
			boolean result = forCondition(node2);
			node2Kind = "bool";
			value = String.valueOf(result);
		}
		if (node1Kind.equals(ConstVar.INT)) {
			if (node2Kind.equals(ConstVar.INT)) {
				table.getAllLevel(node1Value, level).setIntValue(value);
				table.getAllLevel(node1Value, level).setRealValue(
						String.valueOf(Double.parseDouble(value)));
			} else if (node2Kind.equals(ConstVar.REAL)) {
				String error = "不能将浮点数赋值给整型变量";
				error(error, node1.getLineNum());
				return;
			} else if (node2Kind.equals(ConstVar.BOOL)) {
				String error = "不能将布尔值赋值给整型变量";
				error(error, node1.getLineNum());
				return;
			} else if (node2Kind.equals(ConstVar.STRING)) {
				String error = "不能将字符串给整型变量";
				error(error, node1.getLineNum());
				return;
			}
		} else if (node1Kind.equals(ConstVar.REAL)) {
			if (node2Kind.equals(ConstVar.INT)) {
				table.getAllLevel(node1Value, level).setRealValue(
						String.valueOf(Double.parseDouble(value)));
			} else if (node2Kind.equals(ConstVar.REAL)) {
				table.getAllLevel(node1Value, level).setRealValue(value);
			} else if (node2Kind.equals(ConstVar.BOOL)) {
				String error = "不能将布尔值赋值给浮点型变量";
				error(error, node1.getLineNum());
				return;
			} else if (node2Kind.equals(ConstVar.STRING)) {
				String error = "不能将字符串给浮点型变量";
				error(error, node1.getLineNum());
				return;
			}
		} else if (node1Kind.equals(ConstVar.BOOL)) {
			if (node2Kind.equals(ConstVar.INT)) {
				int i = Integer.parseInt(node2Value);
				if (i <= 0)
					table.getAllLevel(node1Value, level).setStringValue("false");
				else
					table.getAllLevel(node1Value, level).setStringValue("true");
			} else if (node2Kind.equals(ConstVar.REAL)) {
				String error = "不能将浮点数赋值给布尔型变量";
				error(error, node1.getLineNum());
				return;
			} else if (node2Kind.equals(ConstVar.BOOL)) {
				table.getAllLevel(node1Value, level).setStringValue(value);
			} else if (node2Kind.equals(ConstVar.STRING)) {
				String error = "不能将字符串赋值给布尔型变量";
				error(error, node1.getLineNum());
				return;
			}
		} else if (node1Kind.equals(ConstVar.STRING)) {
			if (node2Kind.equals(ConstVar.INT)) {
				String error = "不能将整数赋值给字符串变量";
				error(error, node1.getLineNum());
				return;
			} else if (node2Kind.equals(ConstVar.REAL)) {
				String error = "不能将浮点数赋值给字符串变量";
				error(error, node1.getLineNum());
				return;
			} else if (node2Kind.equals(ConstVar.BOOL)) {
				String error = "不能将布尔变量赋值给字符串变量";
				error(error, node1.getLineNum());
				return;
			} else if (node2Kind.equals(ConstVar.STRING)) {
				table.getAllLevel(node1Value, level).setStringValue(value);
			}
		}
	}

	/**
	 * 分析for语句
	 * 
	 * @param root
	 *            语法树中for语句结点
	 */
	private void forFor(TreeNode root) {
		// 根结点Initialization
		TreeNode initializationNode = root.getChildAt(0);
		// 根结点Condition
		TreeNode conditionNode = root.getChildAt(1);
		// 根结点Change
		TreeNode changeNode = root.getChildAt(2);
		// 根结点Statements
		TreeNode statementNode = root.getChildAt(3);
		// for循环语句初始化
		forAssign(initializationNode.getChildAt(0));
		// 条件为真
		while (forCondition(conditionNode.getChildAt(0))) {
			statement(statementNode);
			level--;
			table.update(level);
			level++;
			// for循环执行一次后改变循环条件中的变量
			forAssign(changeNode.getChildAt(0));
		}
	}
	
	/**
	 * 分析if语句
	 * 
	 * @param root
	 *            语法树中if语句结点
	 */
	private void forIf(TreeNode root) {
		int count = root.getChildCount();
		// 根结点Condition
		TreeNode conditionNode = root.getChildAt(0);
		// 根结点Statements
		TreeNode statementNode = root.getChildAt(1);
		// 条件为真
		if (forCondition(conditionNode.getChildAt(0))) {
			statement(statementNode);
		} else if (count == 3) { // 条件为假且有else语句
			TreeNode elseNode = root.getChildAt(2);
			level++;
			statement(elseNode);
			level--;
			table.update(level);
		} else { // 条件为假同时没有else语句
			return;
		}
	}

	/**
	 * 分析while语句
	 * 
	 * @param root
	 *            语法树中while语句结点
	 */
	private void forWhile(TreeNode root) {
		// 根结点Condition
		TreeNode conditionNode = root.getChildAt(0);
		// 根结点Statements
		TreeNode statementNode = root.getChildAt(1);
		while (forCondition(conditionNode.getChildAt(0))) {
			statement(statementNode);
			level--;
			table.update(level);
			level++;
		}
	}

	/**
	 * 分析read语句
	 * 
	 * @param root
	 *            语法树中read语句结点
	 */
	private void forRead(TreeNode root) {
//		CompilerFrame.consoleArea.setText("");
		CompilerFrame.setControlArea(Color.GREEN, true);
		// 要读取的变量的名字
		String idName = root.getContent();
		// 查找变量
		SymbolTableElement element = table.getAllLevel(idName, level);
		// 判断变量是否已经声明
		if (element != null) {
			if (root.getChildCount() != 0) {
				String s = forArray(root.getChildAt(0), element
						.getArrayElementsNum());
				if (s != null) {
					idName += "@" + s;
				} else {
					return;
				}
			}
			String value = readInput();
			if (element.getKind().equals(ConstVar.INT)) {
				if (matchInteger(value)) {
					table.getAllLevel(idName, level).setIntValue(value);
					table.getAllLevel(idName, level).setRealValue(
							String.valueOf(Double.parseDouble(value)));
				} else { // 报错
					String error = "不能将\"" + value + "\"赋值给变量" + idName;
					JOptionPane.showMessageDialog(new JPanel(), error, "输入错误",
							JOptionPane.ERROR_MESSAGE);
				}
			} else if (element.getKind().equals(ConstVar.REAL)) {
				if (matchReal(value)) {
					table.getAllLevel(idName, level).setRealValue(value);
				} else if (matchInteger(value)) {
					table.getAllLevel(idName, level).setRealValue(
							String.valueOf(Double.parseDouble(value)));
				} else { // 报错
					String error = "不能将\"" + value + "\"赋值给变量" + idName;
					JOptionPane.showMessageDialog(new JPanel(), error, "输入错误",
							JOptionPane.ERROR_MESSAGE);
				}
			} else if (element.getKind().equals(ConstVar.BOOL)) {
				if (value.equals("true")) {
					table.getAllLevel(idName, level).setStringValue("true");
				} else if (value.equals("false")) {
					table.getAllLevel(idName, level).setStringValue("false");
				} else { // 报错
					String error = "不能将\"" + value + "\"赋值给变量" + idName;
					JOptionPane.showMessageDialog(new JPanel(), error, "输入错误",
							JOptionPane.ERROR_MESSAGE);
				}
			} else if (element.getKind().equals(ConstVar.STRING)) {
				table.getAllLevel(idName, level).setStringValue(value);
			}
		} else { // 报错
			String error = "变量" + idName + "在使用前未声明";
			error(error, root.getLineNum());
		}
	}

	/**
	 * 分析write语句
	 * 
	 * @param root
	 *            语法树中write语句结点
	 */
	private void forWrite(TreeNode root) {
		CompilerFrame.setControlArea(Color.BLACK, false);
		// 结点显示的内容
		String content = root.getContent();
		// 结点的类型
		String kind = root.getNodeKind();
		if (kind.equals("整数") || kind.equals("实数")) { // 常量
			CompilerFrame.consoleArea.setText(CompilerFrame.consoleArea
					.getText()
					+ content + "\n");
		} else if (kind.equals("字符串")) { // 字符串
			CompilerFrame.consoleArea.setText(CompilerFrame.consoleArea
					.getText()
					+ content + "\n");
		} else if (kind.equals("标识符")) { // 标识符
			if (checkID(root, level)) {
				if (root.getChildCount() != 0) {
					String s = forArray(root.getChildAt(0), table.getAllLevel(
							content, level).getArrayElementsNum());
					if (s != null)
						content += "@" + s;
					else
						return;
				}
				SymbolTableElement temp = table.getAllLevel(content, level);
				if (temp.getKind().equals(ConstVar.INT)) {
					CompilerFrame.consoleArea.setText(CompilerFrame.consoleArea
							.getText()
							+ temp.getIntValue() + "\n");
				} else if (temp.getKind().equals(ConstVar.REAL)) {
					CompilerFrame.consoleArea.setText(CompilerFrame.consoleArea
							.getText()
							+ temp.getRealValue() + "\n");
				} else {
					CompilerFrame.consoleArea.setText(CompilerFrame.consoleArea
							.getText()
							+ temp.getStringValue() + "\n");
				}
			} else {
				return;
			}
		} else if (content.equals(ConstVar.PLUS)
				|| content.equals(ConstVar.MINUS)
				|| content.equals(ConstVar.TIMES)
				|| content.equals(ConstVar.DIVIDE)) { // 表达式
			String value = forExpression(root);
			if (value != null) {
				CompilerFrame.consoleArea.setText(CompilerFrame.consoleArea
						.getText()
						+ value + "\n");
			}
		}
	}

	/**
	 * 分析if和while语句的条件
	 * 
	 * @param root
	 *            根结点
	 * @return 返回计算结果
	 */
	private boolean forCondition(TreeNode root) {
		// > < <> == true false 布尔变量
		String content = root.getContent();
		if (content.equals(ConstVar.TRUE)) {
			return true;
		} else if (content.equals(ConstVar.FALSE)) {
			return false;
		} else if (root.getNodeKind().equals("标识符")) {
			if (checkID(root, level)) {
				if (root.getChildCount() != 0) {
					String s = forArray(root.getChildAt(0), table.getAllLevel(
							content, level).getArrayElementsNum());
					if (s != null)
						content += "@" + s;
					else
						return false;
				}
				SymbolTableElement temp = table.getAllLevel(content, level);
				if (temp.getKind().equals(ConstVar.BOOL)) {
					if(temp.getStringValue().equals(ConstVar.TRUE))
						return true;
					else
						return false;
				} else { // 报错
					String error = "不能将变量" + content + "作为判断条件";
					error(error, root.getLineNum());
				}
			} else {
				return false;
			}
		} else if (content.equals(ConstVar.EQUAL)
				|| content.equals(ConstVar.NEQUAL)
				|| content.equals(ConstVar.LT) || content.equals(ConstVar.GT)) {
			// 存放两个待比较对象的值
			String[] results = new String[2];
			for (int i = 0; i < root.getChildCount(); i++) {
				String kind = root.getChildAt(i).getNodeKind();
				String tempContent = root.getChildAt(i).getContent();
				if (kind.equals("整数") || kind.equals("实数")) { // 常量
					results[i] = tempContent;
				} else if (kind.equals("标识符")) { // 标识符
					if (checkID(root.getChildAt(i), level)) {
						if (root.getChildAt(i).getChildCount() != 0) {
							String s = forArray(root.getChildAt(i)
									.getChildAt(0), table.getAllLevel(
									tempContent, level).getArrayElementsNum());
							if (s != null)
								tempContent += "@" + s;
							else
								return false;
						}
						SymbolTableElement temp = table.getAllLevel(
								tempContent, level);
						if (temp.getKind().equals(ConstVar.INT)) {
							results[i] = temp.getIntValue();
						} else {
							results[i] = temp.getRealValue();
						}
					} else {
						return false;
					}
				} else if (tempContent.equals(ConstVar.PLUS)
						|| tempContent.equals(ConstVar.MINUS)
						|| tempContent.equals(ConstVar.TIMES)
						|| tempContent.equals(ConstVar.DIVIDE)) { // 表达式
					String result = forExpression(root.getChildAt(i));
					if (result != null)
						results[i] = result;
					else
						return false;
				}
			}
			if (!results[0].equals("") && !results[1].equals("")) {
				double element1 = Double.parseDouble(results[0]);
				double element2 = Double.parseDouble(results[1]);
				if (content.equals(ConstVar.GT)) { // >
					if (element1 > element2)
						return true;
				} else if (content.equals(ConstVar.LT)) { // <
					if (element1 < element2)
						return true;
				} else if (content.equals(ConstVar.EQUAL)) { // ==
					if (element1 == element2)
						return true;
				} else { // <>
					if (element1 != element2)
						return true;
				}
			}
		}
		// 语义分析出错或者分析条件结果为假返回false
		return false;
	}

	/**
	 * 分析表达式
	 * 
	 * @param root
	 *            根结点
	 * @return 返回计算结果
	 */
	private String forExpression(TreeNode root) {
		boolean isInt = true;
		// + -
		String content = root.getContent();
		// 存放两个运算对象的值
		String[] results = new String[2];
		for (int i = 0; i < root.getChildCount(); i++) {
			TreeNode tempNode = root.getChildAt(i);
			String kind = tempNode.getNodeKind();
			String tempContent = tempNode.getContent();
			if (kind.equals("整数")) { // 整数
				results[i] = tempContent;
			} else if (kind.equals("实数")) { // 实数
				results[i] = tempContent;
				isInt = false;
			} else if (kind.equals("标识符")) { // 标识符
				if (checkID(tempNode, level)) {
					if (tempNode.getChildCount() != 0) {
						String s = forArray(tempNode.getChildAt(0), table
								.getAllLevel(tempContent, level)
								.getArrayElementsNum());
						if (s != null)
							tempContent += "@" + s;
						else
							return null;
					}
					SymbolTableElement temp = table.getAllLevel(tempNode
							.getContent(), level);
					if (temp.getKind().equals(ConstVar.INT)) {
						results[i] = temp.getIntValue();
					} else if (temp.getKind().equals(ConstVar.REAL)) {
						results[i] = temp.getRealValue();
						isInt = false;
					}
				} else {
					return null;
				}
			} else if (tempContent.equals(ConstVar.PLUS)
					|| tempContent.equals(ConstVar.MINUS)
					|| tempContent.equals(ConstVar.TIMES)
					|| tempContent.equals(ConstVar.DIVIDE)) { // 表达式
				String result = forExpression(root.getChildAt(i));
				if (result != null) {
					results[i] = result;
					if (matchReal(result))
						isInt = false;
				} else
					return null;
			}
		}
		if (isInt) {
			int e1 = Integer.parseInt(results[0]);
			int e2 = Integer.parseInt(results[1]);
			if (content.equals(ConstVar.PLUS))
				return String.valueOf(e1 + e2);
			else if (content.equals(ConstVar.MINUS))
				return String.valueOf(e1 - e2);
			else if (content.equals(ConstVar.TIMES))
				return String.valueOf(e1 * e2);
			else
				return String.valueOf(e1 / e2);
		} else {
			double e1 = Double.parseDouble(results[0]);
			double e2 = Double.parseDouble(results[1]);
			BigDecimal bd1 = new BigDecimal(e1);
			BigDecimal bd2 = new BigDecimal(e2);
			if (content.equals(ConstVar.PLUS))
				return String.valueOf(bd1.add(bd2).floatValue());
			else if (content.equals(ConstVar.MINUS))
				return String.valueOf(bd1.subtract(bd2).floatValue());
			else if (content.equals(ConstVar.TIMES))
				return String.valueOf(bd1.multiply(bd2).floatValue());
			else
				return String.valueOf(bd1.divide(bd2, 3,
						BigDecimal.ROUND_HALF_UP).floatValue());
		}
	}

	/**
	 * array
	 * 
	 * @param root
	 *            根结点
	 * @param arraySize
	 *            数组大小
	 * @return 出错返回null
	 */
	private String forArray(TreeNode root, int arraySize) {
		if (root.getNodeKind().equals("整数")) {
			int i = Integer.parseInt(root.getContent());
			if (i > -1 && i < arraySize) {
				return root.getContent();
			} else if (i < 0) {
				String error = "数组下标不能为负数";
				error(error, root.getLineNum());
				return null;
			} else {
				String error = "数组下标越界";
				error(error, root.getLineNum());
				return null;
			}
		} else if (root.getNodeKind().equals("标识符")) {
			// 检查标识符
			if (checkID(root, level)) {
				SymbolTableElement temp = table.getAllLevel(root.getContent(),
						level);
				if (temp.getKind().equals(ConstVar.INT)) {
					int i = Integer.parseInt(temp.getIntValue());
					if (i > -1 && i < arraySize) {
						return temp.getIntValue();
					} else if (i < 0) {
						String error = "数组下标不能为负数";
						error(error, root.getLineNum());
						return null;
					} else {
						String error = "数组下标越界";
						error(error, root.getLineNum());
						return null;
					}
				} else {
					String error = "类型不匹配,数组索引号必须为整数类型";
					error(error, root.getLineNum());
					return null;
				}
			} else {
				return null;
			}
		} else if (root.getContent().equals(ConstVar.PLUS)
				|| root.getContent().equals(ConstVar.MINUS)
				|| root.getContent().equals(ConstVar.TIMES)
				|| root.getContent().equals(ConstVar.DIVIDE)) { // 表达式
			String result = forExpression(root);
			if (result != null) {
				if (matchInteger(result)) {
					int i = Integer.parseInt(result);
					if (i > -1 && i < arraySize) {
						return result;
					} else if (i < 0) {
						String error = "数组下标不能为负数";
						error(error, root.getLineNum());
						return null;
					} else {
						String error = "数组下标越界";
						error(error, root.getLineNum());
						return null;
					}
				} else {
					String error = "类型不匹配,数组索引号必须为整数类型";
					error(error, root.getLineNum());
					return null;
				}
			} else
				return null;
		}
		return null;
	}

	/**
	 * 检查字符串是否声明和初始化
	 * 
	 * @param root
	 *            字符串结点
	 * @param level
	 *            字符串作用域
	 * @return 如果声明且初始化则返回true,否则返回false
	 */
	private boolean checkID(TreeNode root, int level) {
		// 标识符名字
		String idName = root.getContent();
		// 标识符未声明
		if (table.getAllLevel(idName, level) == null) {
			String error = "变量" + idName + "在使用前未声明";
			error(error, root.getLineNum());
			return false;
		} else {
			if (root.getChildCount() != 0) {
				String tempString = forArray(root.getChildAt(0), table
						.getAllLevel(idName, level).getArrayElementsNum());
				if (tempString != null)
					idName += "@" + tempString;
				else
					return false;
			}
			SymbolTableElement temp = table.getAllLevel(idName, level);
			// 变量未初始化
			if (temp.getIntValue().equals("") && temp.getRealValue().equals("")
					&& temp.getStringValue().equals("")) {
				String error = "变量" + idName + "在使用前未初始化";
				error(error, root.getLineNum());
				return false;
			} else {
				return true;
			}
		}
	}

	public String getErrorInfo() {
		return errorInfo;
	}

	public void setErrorInfo(String errorInfo) {
		this.errorInfo = errorInfo;
	}

	public int getErrorNum() {
		return errorNum;
	}

	public void setErrorNum(int errorNum) {
		this.errorNum = errorNum;
	}

}