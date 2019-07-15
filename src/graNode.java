import com.sun.istack.internal.localization.NullLocalizable;

public class graNode {


    private String id;    //父类型
    private String name;  //标识符名字
    private int type;     //0，类型定义；1，值定义
    private String value;//值定义的值，类型定义默认为-1
    private int line;     //行数
    private Boolean isConstraint; //是否存在约束
    public graNode next;
    public graNode pre;

    graNode(){
        id = null;
        name = null;
        type = -1;
        value = "-1";
        line = 0;
        isConstraint = false;
        next = null;
        pre = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Boolean getConstraint() {
        return isConstraint;
    }

    public void setConstraint(Boolean constraint) {
        isConstraint = constraint;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }
}
class graNodeList{
    public graNode head;
    public graNode tail;

    graNodeList(){
        head = null;
        tail = null;
    }
}
