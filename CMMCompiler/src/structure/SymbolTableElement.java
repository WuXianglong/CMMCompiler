package structure;

public class SymbolTableElement {
	/* 元素名字 */
	private String name;
	/* 元素类型 */
	private String kind;
	/* 元素所在行号 */
	private int lineNum;
	/* 元素作用域 */
	private int level;
	/* 元素的整形数值 */
	private String intValue;
	/* 元素的浮点型数值 */
	private String realValue;
	/* 元素的字符串值 */
	private String stringValue;
	/* 表明元素是否为数组,0表示不是,否则表示数组的大小 */
	private int arrayElementsNum;

	public SymbolTableElement(String name,String kind,int lineNum,int level) {
		this.name = name;
		this.kind = kind;
		this.lineNum = lineNum;
		this.level = level;
		this.intValue = "";
		this.realValue = "";
		this.stringValue = "";
		this.arrayElementsNum = 0;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public int getLineNum() {
		return lineNum;
	}

	public void setLineNum(int lineNum) {
		this.lineNum = lineNum;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getIntValue() {
		return intValue;
	}

	public void setIntValue(String intValue) {
		this.intValue = intValue;
	}

	public String getRealValue() {
		return realValue;
	}

	public void setRealValue(String realValue) {
		this.realValue = realValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public int getArrayElementsNum() {
		return arrayElementsNum;
	}

	public void setArrayElementsNum(int arrayElementsNum) {
		this.arrayElementsNum = arrayElementsNum;
	}
	
	public String toString() {
		return name + "_" + kind + "_" + level + "_" + arrayElementsNum;
	}
	
	public boolean equals(Object object) {
		SymbolTableElement element = (SymbolTableElement)object;
		return this.toString().equals(element.toString());
	}

}
