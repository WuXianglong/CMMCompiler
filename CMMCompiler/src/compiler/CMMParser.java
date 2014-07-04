package compiler;

import java.util.ArrayList;

import structure.ConstVar;
import structure.Token;
import structure.TreeNode;

/**
 * CMM语法分析器
 * 
 * @author 吴文苑
 * @version 1.4
 */
public class CMMParser {

	// 词法分析得到的tokens向量
	private ArrayList<Token> tokens;
	// 标记当前token的游标
	private int index = 0;
	// 存放当前token的值
	private Token currentToken = null;
	// 错误个数
	private int errorNum = 0;
	// 错误信息
	private String errorInfo = "";
	// 语法分析根结点
	private static TreeNode root;

	public CMMParser(ArrayList<Token> tokens) {
		this.tokens = tokens;
		if (tokens.size() != 0)
			currentToken = tokens.get(0);
	}

	public int getErrorNum() {
		return errorNum;
	}

	public void setErrorNum(int errorNum) {
		this.errorNum = errorNum;
	}

	public String getErrorInfo() {
		return errorInfo;
	}

	public void setErrorInfo(String errorInfo) {
		this.errorInfo = errorInfo;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * 语法分析主方法
	 * 
	 * @return TreeNode
	 */
	public TreeNode execute() {
		root = new TreeNode("PROGRAM");
		for (; index < tokens.size();) {
			root.add(statement());
		}
		return root;
	}

	/**
	 * 取出tokens中的下一个token
	 * 
	 */
	private void nextToken() {
		index++;
		if (index > tokens.size() - 1) {
			currentToken = null;
			if (index > tokens.size())
				index--;
			return;
		}
		currentToken = tokens.get(index);
	}

	/**
	 * 出错处理函数
	 * 
	 * @param error
	 *            出错信息
	 */
	private void error(String error) {
		String line = "    ERROR:第 ";
		Token previous = tokens.get(index - 1);
		if (currentToken != null
				&& currentToken.getLine() == previous.getLine()) {
			line += currentToken.getLine() + " 行,第 " + currentToken.getCulomn()
					+ " 列：";
		} else
			line += previous.getLine() + " 行,第 " + previous.getCulomn() + " 列：";
		errorInfo += line + error;
		errorNum++;
	}

	/**
	 * statement: if_stm | while_stm | read_stm | write_stm | assign_stm |
	 * declare_stm | for_stm;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode statement() {
		// 保存要返回的结点
		TreeNode tempNode = null;
		// 赋值语句
		if (currentToken != null && currentToken.getKind().equals("标识符")) {
			tempNode = assign_stm(false);
		}
		// 声明语句
		else if (currentToken != null
				&& (currentToken.getContent().equals(ConstVar.INT)
						|| currentToken.getContent().equals(ConstVar.REAL) || currentToken
						.getContent().equals(ConstVar.BOOL))
				|| currentToken.getContent().equals(ConstVar.STRING)) {
			tempNode = declare_stm();
		}
		// For循环语句
		else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.FOR)) {
			tempNode = for_stm();
		}
		// If条件语句
		else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.IF)) {
			tempNode = if_stm();
		}
		// While循环语句
		else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.WHILE)) {
			tempNode = while_stm();
		}
		// read语句
		else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.READ)) {
			TreeNode readNode = new TreeNode("关键字", ConstVar.READ, currentToken
					.getLine());
			readNode.add(read_stm());
			tempNode = readNode;
		}
		// write语句
		else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.WRITE)) {
			TreeNode writeNode = new TreeNode("关键字", ConstVar.WRITE,
					currentToken.getLine());
			writeNode.add(write_stm());
			tempNode = writeNode;
		}
		// 出错处理
		else {
			String error = " 语句以错误的token开始" + "\n";
			error(error);
			tempNode = new TreeNode(ConstVar.ERROR + "语句以错误的token开始");
			nextToken();
		}
		return tempNode;
	}

	/**
	 * for_stm :FOR LPAREN (assign_stm) SEMICOLON condition SEMICOLON assign_stm
	 * RPAREN LBRACE statement RBRACE;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode for_stm() {
		// 是否有大括号,默认为true
		boolean hasBrace = true;
		// if函数返回结点的根结点
		TreeNode forNode = new TreeNode("关键字", "for", currentToken.getLine());
		nextToken();
		// 匹配左括号(
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LPAREN)) {
			nextToken();
		} else { // 报错
			String error = " for循环语句缺少左括号\"(\"" + "\n";
			error(error);
			forNode.add(new TreeNode(ConstVar.ERROR + "for循环语句缺少左括号\"(\""));
		}
		// initialization
		TreeNode initializationNode = new TreeNode("initialization",
				"Initialization", currentToken.getLine());
		initializationNode.add(assign_stm(true));
		forNode.add(initializationNode);
		// 匹配分号;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.SEMICOLON)) {
			nextToken();
		} else {
			String error = " for循环语句缺少分号\";\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "for循环语句缺少分号\";\"");
		}
		// condition
		TreeNode conditionNode = new TreeNode("condition", "Condition",
				currentToken.getLine());
		conditionNode.add(condition());
		forNode.add(conditionNode);
		// 匹配分号;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.SEMICOLON)) {
			nextToken();
		} else {
			String error = " for循环语句缺少分号\";\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "for循环语句缺少分号\";\"");
		}
		// change
		TreeNode changeNode = new TreeNode("change", "Change", currentToken
				.getLine());
		changeNode.add(assign_stm(true));
		forNode.add(changeNode);
		// 匹配右括号)
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.RPAREN)) {
			nextToken();
		} else { // 报错
			String error = " if条件语句缺少右括号\")\"" + "\n";
			error(error);
			forNode.add(new TreeNode(ConstVar.ERROR + "if条件语句缺少右括号\")\""));
		}
		// 匹配左大括号{
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LBRACE)) {
			nextToken();
		} else {
			hasBrace = false;
		}
		// statement
		TreeNode statementNode = new TreeNode("statement", "Statements",
				currentToken.getLine());
		forNode.add(statementNode);
		if(hasBrace) {
		while (currentToken != null) {
			if (!currentToken.getContent().equals(ConstVar.RBRACE))
				statementNode.add(statement());
			else if (statementNode.getChildCount() == 0) {
				forNode.remove(forNode.getChildCount() - 1);
				statementNode.setContent("EmptyStm");
				forNode.add(statementNode);
				break;
			} else {
				break;
			}
		}
		// 匹配右大括号}
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.RBRACE)) {
			nextToken();
		} else { // 报错
			String error = " if条件语句缺少右大括号\"}\"" + "\n";
			error(error);
			forNode.add(new TreeNode(ConstVar.ERROR + "if条件语句缺少右大括号\"}\""));
		}
		} else {
			statementNode.add(statement());
		}
		return forNode;
	}

	/**
	 * if_stm: IF LPAREN condition RPAREN LBRACE statement RBRACE (ELSE LBRACE
	 * statement RBRACE)?;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode if_stm() {
		// if语句是否有大括号,默认为true
		boolean hasIfBrace = true;
		// else语句是否有大括号,默认为true
		boolean hasElseBrace = true;
		// if函数返回结点的根结点
		TreeNode ifNode = new TreeNode("关键字", "if", currentToken.getLine());
		nextToken();
		// 匹配左括号(
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LPAREN)) {
			nextToken();
		} else { // 报错
			String error = " if条件语句缺少左括号\"(\"" + "\n";
			error(error);
			ifNode.add(new TreeNode(ConstVar.ERROR + "if条件语句缺少左括号\"(\""));
		}
		// condition
		TreeNode conditionNode = new TreeNode("condition", "Condition",
				currentToken.getLine());
		ifNode.add(conditionNode);
		conditionNode.add(condition());
		// 匹配右括号)
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.RPAREN)) {
			nextToken();
		} else { // 报错
			String error = " if条件语句缺少右括号\")\"" + "\n";
			error(error);
			ifNode.add(new TreeNode(ConstVar.ERROR + "if条件语句缺少右括号\")\""));
		}
		// 匹配左大括号{
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LBRACE)) {
			nextToken();
		} else {
			hasIfBrace = false;
		}
		// statement
		TreeNode statementNode = new TreeNode("statement", "Statements",
				currentToken.getLine());
		ifNode.add(statementNode);
		if (hasIfBrace) {
			while (currentToken != null) {
				if (!currentToken.getContent().equals(ConstVar.RBRACE))
					statementNode.add(statement());
				else if (statementNode.getChildCount() == 0) {
					ifNode.remove(ifNode.getChildCount() - 1);
					statementNode.setContent("EmptyStm");
					ifNode.add(statementNode);
					break;
				} else {
					break;
				}
			}
			// 匹配右大括号}
			if (currentToken != null
					&& currentToken.getContent().equals(ConstVar.RBRACE)) {
				nextToken();
			} else { // 报错
				String error = " if条件语句缺少右大括号\"}\"" + "\n";
				error(error);
				ifNode.add(new TreeNode(ConstVar.ERROR + "if条件语句缺少右大括号\"}\""));
			}
		} else {
			if (currentToken != null)
				statementNode.add(statement());
		}
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.ELSE)) {
			TreeNode elseNode = new TreeNode("关键字", ConstVar.ELSE, currentToken
					.getLine());
			ifNode.add(elseNode);
			nextToken();
			// 匹配左大括号{
			if (currentToken.getContent().equals(ConstVar.LBRACE)) {
				nextToken();
			} else {
				hasElseBrace = false;
			}
			if (hasElseBrace) {
				// statement
				while (currentToken != null
						&& !currentToken.getContent().equals(ConstVar.RBRACE)) {
					elseNode.add(statement());
				}
				// 匹配右大括号}
				if (currentToken != null
						&& currentToken.getContent().equals(ConstVar.RBRACE)) {
					nextToken();
				} else { // 报错
					String error = " else语句缺少右大括号\"}\"" + "\n";
					error(error);
					elseNode.add(new TreeNode(ConstVar.ERROR
							+ "else语句缺少右大括号\"}\""));
				}
			} else {
				if (currentToken != null)
				elseNode.add(statement());
			}
		}
		return ifNode;
	}

	/**
	 * while_stm: WHILE LPAREN condition RPAREN LBRACE statement RBRACE;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode while_stm() {
		// 是否有大括号,默认为true
		boolean hasBrace = true;
		// while函数返回结点的根结点
		TreeNode whileNode = new TreeNode("关键字", ConstVar.WHILE, currentToken
				.getLine());
		nextToken();
		// 匹配左括号(
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LPAREN)) {
			nextToken();
		} else { // 报错
			String error = " while循环缺少左括号\"(\"" + "\n";
			error(error);
			whileNode.add(new TreeNode(ConstVar.ERROR + "while循环缺少左括号\"(\""));
		}
		// condition
		TreeNode conditionNode = new TreeNode("condition", "Condition",
				currentToken.getLine());
		whileNode.add(conditionNode);
		conditionNode.add(condition());
		// 匹配右括号)
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.RPAREN)) {
			nextToken();
		} else { // 报错
			String error = " while循环缺少右括号\")\"" + "\n";
			error(error);
			whileNode.add(new TreeNode(ConstVar.ERROR + "while循环缺少右括号\")\""));
		}
		// 匹配左大括号{
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LBRACE)) {
			nextToken();
		} else {
			hasBrace = false;
		}
		// statement
		TreeNode statementNode = new TreeNode("statement", "Statements",
				currentToken.getLine());
		whileNode.add(statementNode);
		if(hasBrace) {
		while (currentToken != null
				&& !currentToken.getContent().equals(ConstVar.RBRACE)) {
			if (!currentToken.getContent().equals(ConstVar.RBRACE))
				statementNode.add(statement());
			else if (statementNode.getChildCount() == 0) {
				whileNode.remove(whileNode.getChildCount() - 1);
				statementNode.setContent("EmptyStm");
				whileNode.add(statementNode);
				break;
			} else {
				break;
			}
		}
		// 匹配右大括号}
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.RBRACE)) {
			nextToken();
		} else { // 报错
			String error = " while循环缺少右大括号\"}\"" + "\n";
			error(error);
			whileNode.add(new TreeNode(ConstVar.ERROR + "while循环缺少右大括号\"}\""));
		}
		} else {
			if(currentToken != null)
				statementNode.add(statement());
		}
		return whileNode;
	}

	/**
	 * read_stm: READ LPAREN ID RPAREN SEMICOLON;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode read_stm() {
		// 保存要返回的结点
		TreeNode tempNode = null;
		nextToken();
		// 匹配左括号(
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LPAREN)) {
			nextToken();
		} else {
			String error = " read语句缺少左括号\"(\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "read语句缺少左括号\"(\"");
		}
		// 匹配标识符
		if (currentToken != null && currentToken.getKind().equals("标识符")) {
			tempNode = new TreeNode("标识符", currentToken.getContent(),
					currentToken.getLine());
			nextToken();
			// 判断是否是为数组赋值
			if (currentToken != null
					&& currentToken.getContent().equals(ConstVar.LBRACKET)) {
				tempNode.add(array());
			}
		} else {
			String error = " read语句左括号后不是标识符" + "\n";
			error(error);
			nextToken();
			return new TreeNode(ConstVar.ERROR + "read语句左括号后不是标识符");
		}
		// 匹配右括号)
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.RPAREN)) {
			nextToken();
		} else {
			String error = " read语句缺少右括号\")\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "read语句缺少右括号\")\"");
		}
		// 匹配分号;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.SEMICOLON)) {
			nextToken();
		} else {
			String error = " read语句缺少分号\";\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "read语句缺少分号\";\"");
		}
		return tempNode;
	}

	/**
	 * write_stm: WRITE LPAREN expression RPAREN SEMICOLON;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode write_stm() {
		// 保存要返回的结点
		TreeNode tempNode = null;
		nextToken();
		// 匹配左括号(
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LPAREN)) {
			nextToken();
		} else {
			String error = " write语句缺少左括号\"(\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "write语句缺少左括号\"(\"");
		}
		// 调用expression函数匹配表达式
		tempNode = expression();
		// 匹配右括号)
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.RPAREN)) {
			nextToken();
		} else {
			String error = " write语句缺少右括号\")\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "write语句缺少右括号\")\"");
		}
		// 匹配分号;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.SEMICOLON)) {
			nextToken();
		} else {
			String error = " write语句缺少分号\";\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "write语句缺少分号\";\"");
		}
		return tempNode;
	}

	/**
	 * assign_stm: (ID | ID array) ASSIGN expression SEMICOLON;
	 * 
	 * @param isFor
	 *            是否是在for循环中调用
	 * @return TreeNode
	 */
	private final TreeNode assign_stm(boolean isFor) {
		// assign函数返回结点的根结点
		TreeNode assignNode = new TreeNode("运算符", ConstVar.ASSIGN, currentToken
				.getLine());
		TreeNode idNode = new TreeNode("标识符", currentToken.getContent(),
				currentToken.getLine());
		assignNode.add(idNode);
		nextToken();
		// 判断是否是为数组赋值
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LBRACKET)) {
			idNode.add(array());
		}
		// 匹配赋值符号=
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.ASSIGN)) {
			nextToken();
		} else { // 报错
			String error = " 赋值语句缺少\"=\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "赋值语句缺少\"=\"");
		}
		// expression
		assignNode.add(condition());
		// 如果不是在for循环语句中调用声明语句,则匹配分号
		if (!isFor) {
			// 匹配分号;
			if (currentToken != null
					&& currentToken.getContent().equals(ConstVar.SEMICOLON)) {
				nextToken();
			} else { // 报错
				String error = " 赋值语句缺少分号\";\"" + "\n";
				error(error);
				assignNode.add(new TreeNode(ConstVar.ERROR + "赋值语句缺少分号\";\""));
			}
		}
		return assignNode;
	}

	/**
	 * declare_stm: (INT | REAL | BOOL | STRING) declare_aid(COMMA declare_aid)*
	 * SEMICOLON;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode declare_stm() {
		TreeNode declareNode = new TreeNode("关键字", currentToken.getContent(),
				currentToken.getLine());
		nextToken();
		// declare_aid
		declareNode = declare_aid(declareNode);
		// 处理同时声明多个变量的情况
		String next = null;
		while (currentToken != null) {
			next = currentToken.getContent();
			if (next.equals(ConstVar.COMMA)) {
				nextToken();
				declareNode = declare_aid(declareNode);
			} else {
				break;
			}
			if (currentToken != null)
				next = currentToken.getContent();
		}
		// 匹配分号;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.SEMICOLON)) {
			nextToken();
		} else { // 报错
			String error = " 声明语句缺少分号\";\"" + "\n";
			error(error);
			declareNode.add(new TreeNode(ConstVar.ERROR + "声明语句缺少分号\";\""));
		}
		return declareNode;
	}

	/**
	 * declare_aid: (ID|ID array)(ASSIGN expression)?;
	 * 
	 * @param root
	 *            根结点
	 * @return TreeNode
	 */
	private final TreeNode declare_aid(TreeNode root) {
		if (currentToken != null && currentToken.getKind().equals("标识符")) {
			TreeNode idNode = new TreeNode("标识符", currentToken.getContent(),
					currentToken.getLine());
			root.add(idNode);
			nextToken();
			// 处理array的情况
			if (currentToken != null
					&& currentToken.getContent().equals(ConstVar.LBRACKET)) {
				idNode.add(array());
			} else if (currentToken != null
					&& !currentToken.getContent().equals(ConstVar.ASSIGN)
					&& !currentToken.getContent().equals(ConstVar.SEMICOLON)
					&& !currentToken.getContent().equals(ConstVar.COMMA)) {
				String error = " 声明语句出错,标识符后出现不正确的token" + "\n";
				error(error);
				root
						.add(new TreeNode(ConstVar.ERROR
								+ "声明语句出错,标识符后出现不正确的token"));
				nextToken();
			}
		} else { // 报错
			String error = " 声明语句中标识符出错" + "\n";
			error(error);
			root.add(new TreeNode(ConstVar.ERROR + "声明语句中标识符出错"));
			nextToken();
		}
		// 匹配赋值符号=
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.ASSIGN)) {
			TreeNode assignNode = new TreeNode("分隔符", ConstVar.ASSIGN,
					currentToken.getLine());
			root.add(assignNode);
			nextToken();
			assignNode.add(condition());
		}
		return root;
	}

	/**
	 * condition: expression (comparison_op expression)? | ID;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode condition() {
		// 记录expression生成的结点
		TreeNode tempNode = expression();
		// 如果条件判断为比较表达式
		if (currentToken != null
				&& (currentToken.getContent().equals(ConstVar.EQUAL)
						|| currentToken.getContent().equals(ConstVar.NEQUAL)
						|| currentToken.getContent().equals(ConstVar.LT) || currentToken
						.getContent().equals(ConstVar.GT))) {
			TreeNode comparisonNode = comparison_op();
			comparisonNode.add(tempNode);
			comparisonNode.add(expression());
			return comparisonNode;
		}
		// 如果条件判断为bool变量
		return tempNode;
	}

	/**
	 * expression: term (add_op term)?;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode expression() {
		// 记录term生成的结点
		TreeNode tempNode = term();

		// 如果下一个token为加号或减号
		while (currentToken != null
				&& (currentToken.getContent().equals(ConstVar.PLUS) || currentToken
						.getContent().equals(ConstVar.MINUS))) {
			// add_op
			TreeNode addNode = add_op();
			addNode.add(tempNode);
			tempNode = addNode;
			tempNode.add(term());
		}
		return tempNode;
	}

	/**
	 * term : factor (mul_op factor)?;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode term() {
		// 记录factor生成的结点
		TreeNode tempNode = factor();

		// 如果下一个token为乘号或除号
		while (currentToken != null
				&& (currentToken.getContent().equals(ConstVar.TIMES) || currentToken
						.getContent().equals(ConstVar.DIVIDE))) {
			// mul_op
			TreeNode mulNode = mul_op();
			mulNode.add(tempNode);
			tempNode = mulNode;
			tempNode.add(factor());
		}
		return tempNode;
	}

	/**
	 * factor : TRUE | FALSE | REAL_LITERAL | INTEGER_LITERAL | ID | LPAREN
	 * expression RPAREN | DQ string DQ | ID array;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode factor() {
		// 保存要返回的结点
		TreeNode tempNode = null;
		if (currentToken != null && currentToken.getKind().equals("整数")) {
			tempNode = new TreeNode("整数", currentToken.getContent(),
					currentToken.getLine());
			nextToken();
		} else if (currentToken != null && currentToken.getKind().equals("实数")) {
			tempNode = new TreeNode("实数", currentToken.getContent(),
					currentToken.getLine());
			nextToken();
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.TRUE)) {
			tempNode = new TreeNode("布尔值", currentToken.getContent(),
					currentToken.getLine());
			nextToken();
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.FALSE)) {
			tempNode = new TreeNode("布尔值", currentToken.getContent(),
					currentToken.getLine());
			nextToken();
		} else if (currentToken != null && currentToken.getKind().equals("标识符")) {
			tempNode = new TreeNode("标识符", currentToken.getContent(),
					currentToken.getLine());
			nextToken();
			// array
			if (currentToken != null
					&& currentToken.getContent().equals(ConstVar.LBRACKET)) {
				tempNode.add(array());
			}
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LPAREN)) { // 匹配左括号(
			nextToken();
			tempNode = expression();
			// 匹配右括号)
			if (currentToken != null
					&& currentToken.getContent().equals(ConstVar.RPAREN)) {
				nextToken();
			} else { // 报错
				String error = " 算式因子缺少右括号\")\"" + "\n";
				error(error);
				return new TreeNode(ConstVar.ERROR + "算式因子缺少右括号\")\"");
			}
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.DQ)) { // 匹配双引号
			nextToken();
			tempNode = new TreeNode("字符串", currentToken.getContent(),
					currentToken.getLine());
			nextToken();
			// 匹配另外一个双引号
			nextToken();
		} else { // 报错
			String error = " 算式因子存在错误" + "\n";
			error(error);
			if (currentToken != null
					&& !currentToken.getContent().equals(ConstVar.SEMICOLON)) {
				nextToken();
			}
			return new TreeNode(ConstVar.ERROR + "算式因子存在错误");
		}
		return tempNode;
	}

	/**
	 * array : LBRACKET (expression) RBRACKET;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode array() {
		// 保存要返回的结点
		TreeNode tempNode = null;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LBRACKET)) {
			nextToken();
		} else {
			String error = " 缺少左中括号\"[\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "缺少左中括号\"[\"");
		}
		// 调用expression函数匹配表达式
		tempNode = expression();
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.RBRACKET)) {
			nextToken();
		} else { // 报错
			String error = " 缺少右中括号\"]\"" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "缺少右中括号\"]\"");
		}
		return tempNode;
	}

	/**
	 * add_op : PLUS | MINUS;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode add_op() {
		// 保存要返回的结点
		TreeNode tempNode = null;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.PLUS)) {
			tempNode = new TreeNode("运算符", ConstVar.PLUS, currentToken
					.getLine());
			nextToken();
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.MINUS)) {
			tempNode = new TreeNode("运算符", ConstVar.MINUS, currentToken
					.getLine());
			nextToken();
		} else { // 报错
			String error = " 加减符号出错" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "加减符号出错");
		}
		return tempNode;
	}

	/**
	 * mul_op : TIMES | DIVIDE;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode mul_op() {
		// 保存要返回的结点
		TreeNode tempNode = null;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.TIMES)) {
			tempNode = new TreeNode("运算符", ConstVar.TIMES, currentToken
					.getLine());
			nextToken();
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.DIVIDE)) {
			tempNode = new TreeNode("运算符", ConstVar.DIVIDE, currentToken
					.getLine());
			nextToken();
		} else { // 报错
			String error = " 乘除符号出错" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "乘除符号出错");
		}
		return tempNode;
	}

	/**
	 * comparison_op: LT | GT | EQUAL | NEQUAL;
	 * 
	 * @return TreeNode
	 */
	private final TreeNode comparison_op() {
		// 保存要返回的结点
		TreeNode tempNode = null;
		if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.LT)) {
			tempNode = new TreeNode("运算符", ConstVar.LT, currentToken.getLine());
			nextToken();
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.GT)) {
			tempNode = new TreeNode("运算符", ConstVar.GT, currentToken.getLine());
			nextToken();
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.EQUAL)) {
			tempNode = new TreeNode("运算符", ConstVar.EQUAL, currentToken
					.getLine());
			nextToken();
		} else if (currentToken != null
				&& currentToken.getContent().equals(ConstVar.NEQUAL)) {
			tempNode = new TreeNode("运算符", ConstVar.NEQUAL, currentToken
					.getLine());
			nextToken();
		} else { // 报错
			String error = " 比较运算符出错" + "\n";
			error(error);
			return new TreeNode(ConstVar.ERROR + "比较运算符出错");
		}
		return tempNode;
	}

}