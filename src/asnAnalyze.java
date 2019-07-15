import com.sun.istack.internal.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class asnAnalyze {
    private BufferedWriter bw;                //创建.c文件
    private BufferedWriter tw;                //创建.h文件
    private BufferedWriter cw;                //创建编码文件
    private BufferedReader br;
    private RandomAccessFile addNotDefined ;  //先使用后定义补充定义
    private String addDefinition = "";        //储存处理单行定义多个类型的语句
    private String constraintString = "";     //约束函数的字符串
    private boolean isPrimType = false;
    private int skip ;                        //先使用后定义现象，向文件中添加定义时光标需要移动的位置


    private String type ,name,value;
    private String primTypeStr[] = {"typedef struct ASN_PRIMITIVE_TYPE_S {","\tuint8_t *buf;","\tint size;","}ASN_PRIMITIVE_TYPE_t;"};
    private graNodeList asnAnalyzeList = new graNodeList();    //语法项节点总链表
    private graNodeList userDefineList[] = new graNodeList[52];    //用户自定义标识符集,以大小写字母进行区别
    private String[] randomName = new String[52];                  //嵌套定义时的随机name
    private int index;                                          //随机name数组的索引

    private boolean isCycle = false;                          //判断是否需要循环处理，即结构体类型定义
    private int numCycle = 0;                                 //储存循环次数，即判断是否结构体中有嵌套定义
    //private String structName = null,structType = null;       //储存结构体类型的名字及类型
    private int emptyLines = 0;
    private ArrayList<Integer> notDefinedLines = new ArrayList();
    private ArrayList<String> notDefinnedTypes = new ArrayList();
    private ArrayList<String> structName = new ArrayList();       //用于在出现结构体嵌套定义时区别是哪一个结构体
    private ArrayList<String> structType = new ArrayList();
    private boolean isCycle() {
        return isCycle;
    }


    private void setCycle(boolean cycle) {
        isCycle = cycle;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private static String letters[] = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w",
            "x","y","z","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T",
            "U","V","W","X","Y","Z"};
    private static String letters2[] = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w",
            "x","y","z","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T",
            "U","V","W","X","Y","Z","{","["};
    private static  String[] asnType = {"INTEGER","BOOLEAN","REAL","OCTET_STRING","BIT_STRING","NULL","SEQUENCE","SEQUENCE OF",
            "SET","SET OF","CHOICE","ENUMERATED","IA5String"};                     //ASN类型数组
    //private static  String[] cType = {"int","char","float"};

    private static int line;

    asnAnalyze(){
        index = 0;
        for(int i =0;i<52;i++){
            userDefineList[i] = new graNodeList();
            randomName[i] = "Temp" + i;
        }
        try {
            FileReader fr = new FileReader("D:\\ideaProjects\\ASN\\asn.txt");
            FileReader define = new FileReader("D:\\ideaProjects\\ASN\\asn.txt");
            br = new BufferedReader(fr);
            BufferedReader de = new BufferedReader(define);
            bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/asn.c"));
            tw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/asn.h"));
            cw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/code.txt"));
            String defineSequOfStrs[] = {"#define\tA_SEQUENCE_OF(type)\tA_SET_OF(type)","#define\tA_SET_OF(type)","\tstruct {",
                    "\t\ttype **array;","\t\tint count;","\t\tint size;","\t\tvoid (*free)(type *);","\t}"};
            String defineSetOfStrs[] = {"#define\tA_SET_OF(type)","\tstruct {",
                    "\t\ttype **array;","\t\tint count;","\t\tint size;","\t\tvoid (*free)(type *);","\t}"};
            String str;
            int temp = 0;
            line = 0;
            for(String line = de.readLine();line != null;line = de.readLine()){
                if(line.contains("SEQUENCE OF")){
                    temp = 1;
                }
                else if(line.contains("SET OF")){
                    temp = 2;
                }
            }
            if(temp == 1)
                for(int i =0;i<defineSequOfStrs.length;i++)
                    writeHline(defineSequOfStrs[i]);
            else if(temp == 2)
                for(int i=0;i<defineSetOfStrs.length;i++)
                    writeHline(defineSetOfStrs[i]);
            de.close();
            define.close();
            str = br.readLine();
            line ++;
            if(str.contains("DEFINITIONS")){
                String t[] = str.split("\\s+");
                writeLine("/*\n" +
                        " * Generated by ASN.1 resolver\n" +
                        " * From ASN.1 module \"" + t[0] + "\"\n" +
                        " * \tfound in \"" + t[0] + ".txt\"\n" +
                        " */");
                str = br.readLine();line++;
                while(str.contains("BEGIN") || str.contains("EXPORT") || str.contains("IMPORT")){
                    str = br.readLine();line++;
                }
                writeLine("#inlcude\"asn.h\"");
                skip = ("/*\n" +
                        " * Generated by ASN.1 resolver\n" +
                        " * From ASN.1 module \"" + t[0] + "\"\n" +
                        " * \tfound in \"" + t[0] + "\".txt\n" +
                        " */\n" + "#inlcude\"asn.h\"\n").length();
                while (str != null && !str.contains("END")){
                    while(str.equals("") && str != null){
                        str = br.readLine();
                        line++;
                        emptyLines++;
                    }
                    if(emptyLines!=0){
                        for(int i=0;i<emptyLines;i++){
                            writeLine("");
                        }
                        emptyLines = 0;
                    }
                    if(str.trim().equals("{")){
                        str = br.readLine();
                        line++;
                    }
                    boolean isError = isDefineError(str);
                    if(isError){
                        if(isUpLow(str) == 0){            //类型定义处理
                            isCycle = typeDefineManage(str);
                            while (isCycle){
                                str = br.readLine();
                                if(str.trim().equals("{")){
                                    str = br.readLine();
                                    line++;
                                }
                                if(str != null){
                                    line++;
                                    String temp2 = str.trim();
                                    if(isDefineError(temp2)){
                                        if(temp2.indexOf(",") == -1 || temp2.indexOf(",") == temp2.length()-1)
                                            isCycle = typeDefineManage(temp2);
                                        else {
                                            String xxx[] = temp2.split(",");
                                            for(int i=0;i<xxx.length;i++){
                                                isCycle = typeDefineManage(xxx[i].trim());
                                            }
                                        }
                                    }
                                }
                                else{
                                    System.out.println("第" + line + "行结构体类型定义有误");
                                }
                            }
                        }
                        else if(isUpLow(str)==1){         //值定义处理
                            valueDefineManage(str);
                        }
                    }
                    str = br.readLine();line++;
                }
            }
            else
                System.out.println("模块定义有误");
            if(str == null || str.equals(""))
                System.out.println("模块定义没有以END结尾");
            if(notDefinnedTypes!=null){
                for(int i =0;i<notDefinnedTypes.size();i++){
                    System.out.println("第" + notDefinedLines.get(i) + "行" + notDefinnedTypes.get(i) + "类型未定义");
                }
            }
            if(!constraintString.equals(""))
                writeLine(constraintString);
            br.close();
            fr.close();
            bw.flush();
            bw.close();
            tw.flush();
            tw.close();
            cw.flush();
            cw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param typeDefine
     * @return
     */
    //类型定义处理
    private boolean typeDefineManage (String typeDefine){
        //String type = null,name = null;
        if(typeDefine.contains("OPTIONAL") || typeDefine.contains("DEFAULT") || typeDefine.contains("COMPONENTS OF") || typeDefine.contains("...")){
            keyWordManage(typeDefine);
        }
        else if(typeDefine.contains("(") && typeDefine.contains(")")){
            constraintManage(typeDefine);
        }
        else{
            spiltTypeDefine(typeDefine.trim());
            if((type.equals("SEQUENCE")&&!isCycle()) || !isCycle()&&(type.equals("SET") )){   //结构体类型定义处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId(type);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    structName.add(name);
                    structType.add(type);
                    if(notDefinnedTypes != null && notDefinnedTypes.indexOf(name) !=-1){
                        useButNotDefinedManage(typeDefine);
                    }

                    else{
                        setCycle(true);
                        numCycle++;
                        String firstLine = "typedef struct " + name + "{";
                        writeLine(firstLine);
                    }
                }

            }
            else if(type.equals("CHOICE")&&!isCycle()){//UNION类型定义处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId(type);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    structName.add(name);
                    structType.add(type);
                    if(notDefinnedTypes != null && notDefinnedTypes.indexOf(name) !=-1){
                        useButNotDefinedManage(typeDefine);
                    }

                    else{
                        setCycle(true);
                        numCycle++;
                        String firstLine = "typedef union " + name + "{";
                        writeLine(firstLine);
                    }
                }
            }
            else if(typeDefine.contains("SEQUENCE OF")){                     //数组定义处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    String typeStr[] = type.split("\\s+");
                    type = typeStr[typeStr.length-1];
                    graNode node = new graNode();
                    if(!isDefined(type)){
                        notDefinedLines.add(line);              //记录未定义出现的行数
                        notDefinnedTypes.add(type);             //记录未定义类型
                    }
                    node.setId("SEQUENCE OF");
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    if(notDefinnedTypes != null && notDefinnedTypes.indexOf(name) !=-1){
                        useButNotDefinedManage(typeDefine);
                    }
                    else{
                        String str = "";
                        if(isUpLow(name) == 0)
                            str = "typedef struct "+ name + "{\n" + "\tA_SEQUENCE_OF(" + typeStr[typeStr.length-1] +"_t) list;\n" + "} " + name + "_t" + ";";
                        else{
                            for(int i=0;i<numCycle;i++)
                                str+="\t";
                            str += typeStr[typeStr.length-1] +"[]" + " " + name + ";";
                        }
                        writeLine(str);
                    }
                }
            }
            else if(typeDefine.contains("SET OF")){                       //SET OF 类型定义处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    String typeStr[] = type.split("\\s+");
                    type = typeStr[typeStr.length-1];
                    if(!isDefined(type)){
                        notDefinedLines.add(line);              //记录未定义出现的行数
                        notDefinnedTypes.add(type);             //记录未定义类型
                    }
                    graNode node = new graNode();
                    node.setId("SET OF");
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    if(notDefinnedTypes != null && notDefinnedTypes.indexOf(name) !=-1){
                        useButNotDefinedManage(typeDefine);
                    }
                    else{
                        String str = "";
                        if(isUpLow(name) == 0)
                            str = "typedef struct "+ name + "{\n" + "\tA_SET_OF(" + typeStr[typeStr.length-1] +"_t) list;\n" + "} " + name + "_t" + ";";
                        else{
                            for(int i=0;i<numCycle;i++)
                                str+="\t";
                            str += typeStr[typeStr.length-1] +"[]" + " " + name + ";";
                        }
                        writeLine(str);
                    }
                }

            }
            else if(isCycle() && (typeDefine.contains("SEQUENCE")||typeDefine.contains("SET")||typeDefine.contains("CHOICE")) ){  //结构体内的嵌套定义
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    setCycle(true);
                    //numCycle++;
                    graNode node = new graNode();
                    node.setId(randomName[index]);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    writeLine("\t"  + randomName[index] + "_t " + name + ";");
                    notDefinnedTypes.add(randomName[index] );
                    notDefinedLines.add(line);
                    structName.add(randomName[index++]);
                    structType.add(type);
                    useButNotDefinedManage(typeDefine);
                }

            }
            else if(typeDefine.contains("{") && typeDefine.contains("}")){  //**单行嵌套定义
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    String nestStr[] = typeDefine.split("::=");
                    String nestBeh = nestStr[1].trim();
                    graNode node = new graNode();
                    if(nestBeh.indexOf("{") == 0){
                        if(!isDefined(type)){
                            node.setId(randomName[index]);
                            notDefinnedTypes.add(type);
                            notDefinedLines.add(line);
                            writeLine(name + "_t ::=" + " " + randomName[index] + "_t;");
                            useButNotDefinedManage(typeDefine);
                        }
                        else{
                            node.setId(randomName[index]);
                            writeLine(name + "_t ::=" + " " + randomName[index] + "_t;");
                            useButNotDefinedManage(typeDefine);
                        }
                        node.setName(name);
                        node.setLine(line);
                        node.setType(0);
                        addAsnList(node,name);
                    }
                }
            }
            else if(typeDefine.indexOf("}")==0 || typeDefine.indexOf("}")==type.length()-1){
                if(--numCycle==0){
                    setCycle(false);
                    String endLine = null;
                    if(structType.get(structType.size()-1).equals("SET"))
                        endLine="\tunsigned int _presence_map\n" +
                                "\t\t[((4+(8*sizeof(unsigned int))-1)/(8*sizeof(unsigned int)))];" + "}" + structName.get(structName.size()-1) +"_t" + ";";
                    else
                        endLine= "}" + structName.get(structName.size()-1) +"_t" + ";";
                    structName.remove(structName.size()-1);
                    structType.remove(structType.size()-1);
                    writeLine(endLine);
                }
            }
            else if(type.contains("INTEGER")){                                 //整数类型定义
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId(type);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    try {
                        br.mark(100);
                        String strT = br.readLine();
                        if(typeDefine.contains("{") ){
                            if(!isCycle())
                                writeLine("typedef enum " + name + " {");
                            else{
                                writeLine("\te_" + randomName[index] + " " + name +";");
                                structName.add(randomName[index++]);
                                structType.add("enumIntegerCycle");
                            }
                            node.setId("enumINTEGER");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().equals("{")){
                            line++;
                            if(!isCycle())
                                strT = "typedef enum " + name + " {";
                            else{
                                strT = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index]);
                                index++;
                                structType.add("enumIntegerCycle");
                            }
                            writeLine(strT);
                            strT = br.readLine();
                            line++;
                            node.setId("enumINTEGER");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().indexOf("{") == 0 ){
                            line++;
                            String tep ;
                            if (!isCycle())
                                tep = "typedef enum " + name + " {";
                            else {
                                tep = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index++]);
                                structType.add("enumIntegerCycle");
                            }
                            writeLine(tep);
                            node.setId("enumINTEGER");
                            enumDefineManage(strT);
                        }
                        else {
                            br.reset();
                            if (notDefinnedTypes != null && notDefinnedTypes.indexOf(name) != -1) {
                                useButNotDefinedManage(typeDefine);
                            } else {
                                String str = "";
                                String temp = "typedef ASN_PRIMITIVE_TYPE_t INTEGER_t;";
                                if (isUpLow(name) == 0)
                                    str = "typedef " + "INTEGER_t" + " " + name + "_t" + ";";
                                else {
                                    for (int i = 0; i < numCycle; i++)
                                        str += "\t";
                                    str += "INTEGER_t" + " " + name + ";";
                                }
                                if (!isPrimType) {
                                    for (int i = 0; i < primTypeStr.length; i++) {
                                        writeHline(primTypeStr[i]);
                                    }
                                    isPrimType = true;
                                }
                                if (!isHeadDefined(temp))
                                    writeHline(temp);
                                writeLine(str);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    addAsnList(node,name);
                }
            }
            else if(type.equals("REAL")){                                  //浮点数类型定义
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId(type);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    try {
                        br.mark(100);
                        String strT = br.readLine();
                        if(typeDefine.contains("{") ){
                            if(!isCycle())
                                writeLine("typedef enum " + name + " {");
                            else{
                                writeLine("\te_" + randomName[index] + " " + name +";");
                                structName.add(randomName[index++]);
                                structType.add("enumRealCycle");
                            }
                            node.setId("enumReal");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().equals("{")){
                            line++;
                            if(!isCycle())
                                strT = "typedef enum " + name + " {";
                            else{
                                strT = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index]);
                                index++;
                                structType.add("enumRealCycle");
                            }
                            writeLine(strT);
                            strT = br.readLine();
                            line++;
                            node.setId("enumReal");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().indexOf("{") == 0 ){
                            line++;
                            String tep ;
                            if (!isCycle())
                                tep = "typedef enum " + name + " {";
                            else {
                                tep = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index++]);
                                structType.add("enumRealCycle");
                            }
                            writeLine(tep);
                            node.setId("enumReal");
                            enumDefineManage(strT);
                        }
                        else{
                            br.reset();
                            if(notDefinnedTypes!=null && notDefinnedTypes.indexOf(name) != -1){
                                useButNotDefinedManage(typeDefine);
                            }
                            else{
                                String str = "";
                                String temp = "typedef ASN_PRIMITIVE_TYPE_t REAL_t;";
                                if(isUpLow(name) == 0)
                                    str = "typedef " + "REAL_t" + " " + name + "_t" + ";";
                                else{
                                    for(int i=0;i<numCycle;i++)
                                        str+="\t";
                                    str += "REAL_t" + " " + name + ";";
                                }
                                if(!isPrimType){
                                    for(int i=0;i<primTypeStr.length;i++){
                                        writeHline(primTypeStr[i]);
                                    }
                                    isPrimType = true;
                                }
                                if(!isHeadDefined(temp))
                                    writeHline(temp);
                                writeLine(str);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    addAsnList(node,name);
                }
            }
            else if(type.equals("NULL")){                                  //NULL型处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId("NULL");
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    if(notDefinnedTypes!=null && notDefinnedTypes.indexOf(name) != -1){
                        useButNotDefinedManage(typeDefine);
                    }
                    else {
                        String str = "";
                        String temp = "typedef int NULL_t;";
                        if(isUpLow(name) == 0)
                            str = "typedef " + "NULL_t" + " " + name + "_t" + ";";
                        else{
                            for(int i=0;i<numCycle;i++)
                                str+="\t";
                            str += "NULL_t" + " " + name + ";";
                        }
                        if(!isHeadDefined(temp))
                            writeHline(temp);
                        writeLine(str);
                    }
                }
            }
            else if(type.equals("BOOLEAN")){                           //BOOLEAN类型处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId("BOOLEAN");
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    if(notDefinnedTypes!=null && notDefinnedTypes.indexOf(name) != -1){
                        useButNotDefinedManage(typeDefine);
                    }
                    else {
                        String str = "";
                        String temp = "typedef int BOOLEAN_t;";
                        if(isUpLow(name) == 0)
                            str = "typedef " + "BOOLEAN_t" + " " + name + "_t" + ";";
                        else{
                            for(int i=0;i<numCycle;i++)
                                str+="\t";
                            str += "BOOLEAN_t" + " " + name + ";";
                        }
                        if(!isHeadDefined(temp))
                            writeHline(temp);
                        writeLine(str);
                    }
                }
            }
            else if(type.equals("ENUMERATED")){
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId("ENUMERATED");
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    try {
                        br.mark(100);
                        String strT = br.readLine();
                        if(typeDefine.contains("{") ){
                            if(!isCycle())
                                writeLine("typedef enum " + name + " {");
                            else{
                                writeLine("\te_" + randomName[index] + " " + name +";");
                                structName.add(randomName[index++]);
                                structType.add("enumEnumeRatedCycle");
                            }
                            node.setId("enumENUMERATED");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().equals("{")){
                            line++;
                            if(!isCycle())
                                strT = "typedef enum " + name + " {";
                            else{
                                strT = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index]);
                                index++;
                                structType.add("enumENUMERATEDCycle");
                            }
                            writeLine(strT);
                            strT = br.readLine();
                            line++;
                            node.setId("enumENUMERATED");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().indexOf("{") == 0 ){
                            line++;
                            String tep ;
                            if (!isCycle())
                                tep = "typedef enum " + name + " {";
                            else {
                                tep = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index++]);
                                structType.add("enumENUMERATEDCycle");
                            }
                            writeLine(tep);
                            node.setId("enumENUMERATED");
                            enumDefineManage(strT);
                        }
                        else{
                            br.reset();
                            if(notDefinnedTypes!=null && notDefinnedTypes.indexOf(name) != -1){
                                useButNotDefinedManage(typeDefine);
                            }
                            else {
                                String str = "";
                                String temp = "typedef INTEGER_t ENUMERATED_t;";
                                if(isUpLow(name) == 0)
                                    str = "typedef " + "ENUMERATED_t" + " " + name + "_t" + ";";
                                else{
                                    for(int i=0;i<numCycle;i++)
                                        str+="\t";
                                    str += "ENUMERATED_t" + " " + name + ";";
                                }
                                if(!isHeadDefined(temp))
                                    writeHline(temp);
                                writeLine(str);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    addAsnList(node,name);
                }
            }
            else if(typeDefine.contains("BIT STRING")){                     //BIT STRING型处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId("BIT STRING");
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    try {
                        br.mark(100);
                        String strT = br.readLine();
                        if(typeDefine.contains("{") ){
                            if(!isCycle())
                                writeLine("typedef enum " + name + " {");
                            else{
                                writeLine("\te_" + randomName[index] + " " + name +";");
                                structName.add(randomName[index++]);
                                structType.add("enumBit_String");
                            }
                            node.setId("enumBIT_STRING");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().equals("{")){
                            line++;
                            if(!isCycle())
                                strT = "typedef enum " + name + " {";
                            else{
                                strT = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index]);
                                index++;
                                structType.add("enumBIT_STRINGCycle");
                            }
                            writeLine(strT);
                            strT = br.readLine();
                            line++;
                            node.setId("enumBIT_STRING");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().indexOf("{") == 0 ){
                            line++;
                            String tep ;
                            if (!isCycle())
                                tep = "typedef enum " + name + " {";
                            else {
                                tep = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index++]);
                                structType.add("enumBsCycle");
                            }
                            writeLine(tep);
                            node.setId("enumBIT_STRING");
                            enumDefineManage(strT);
                        }
                        else{
                            br.reset();
                            if(notDefinnedTypes!=null && notDefinnedTypes.indexOf(name) != -1){
                                useButNotDefinedManage(typeDefine);
                            }
                            else {
                                String str = "";
                                String temp[] = {"typedef struct BIT_STRING_s{","\tuint8_t *buf;","\tint size;","\tint bits_unused;","}BIT_STRING_t;"};
                                if(isUpLow(name) == 0)
                                    str = "typedef " + "BIT_STRING_t" + " " + name + "_t" + ";";
                                else{
                                    for(int i=0;i<numCycle;i++)
                                        str+="\t";
                                    str += "BIT_STRING_t" + " " + name + ";";
                                }
                                if(!isHeadDefined(temp[0]))
                                    for(int i =0;i<temp.length;i++)
                                        writeHline(temp[i]);
                                writeLine(str);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    addAsnList(node,name);
                }
            }
            else if(typeDefine.contains("OCTET STRING")){                   //OCTET STRING型处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId("OCTET STRING");
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    try {
                        br.mark(100);
                        String strT = br.readLine();
                        if(typeDefine.contains("{") ){
                            if(!isCycle())
                                writeLine("typedef enum " + name + " {");
                            else{
                                writeLine("\te_" + randomName[index] + " " + name +";");
                                structName.add(randomName[index++]);
                                structType.add("enumOS");
                            }
                            node.setId("enumOCTET_STRING");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().equals("{")){
                            line++;
                            if(!isCycle())
                                strT = "typedef enum " + name + " {";
                            else{
                                strT = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index]);
                                index++;
                                structType.add("enumOSCycle");
                            }
                            writeLine(strT);
                            strT = br.readLine();
                            line++;
                            node.setId("enumOCTET_STRING");
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().indexOf("{") == 0 ){
                            line++;
                            String tep ;
                            if (!isCycle())
                                tep = "typedef enum " + name + " {";
                            else {
                                tep = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index++]);
                                structType.add("enumOSCycle");
                            }
                            writeLine(tep);
                            node.setId("enumOCTET_STRING");
                            enumDefineManage(strT);
                        }
                        else{
                            br.reset();
                            if(notDefinnedTypes!=null && notDefinnedTypes.indexOf(name) != -1){
                                useButNotDefinedManage(typeDefine);
                            }
                            else {
                                String str = "";
                                String typeStr[] = {"typedef struct OCTET_STRING {","\tuint8_t *buf;","\tint size;","}OCTET_STRING_t;"};
                                if(isUpLow(name) == 0)
                                    str = "typedef " + "OCTET_STRING_t" + " " + name + "_t" + ";";
                                else{
                                    for(int i=0;i<numCycle;i++)
                                        str+="\t";
                                    str += "OCTET_STRING_t" + " " + name + ";";
                                }
                                if(!isHeadDefined("typedef struct OCTET_STRING {"))
                                    for(int i=0;i<typeStr.length;i++)
                                        writeHline(typeStr[i]);
                                writeLine(str);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    addAsnList(node,name);
                }
            }
            else if(type.contains("String")||type.contains("Time")){             //乱七八糟String和Time型处理
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId(type);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    try {
                        br.mark(100);
                        String strT = br.readLine();
                        if(typeDefine.contains("{") ){
                            if(!isCycle())
                                writeLine("typedef enum " + name + " {");
                            else{
                                writeLine("\te_" + randomName[index] + " " + name +";");
                                structName.add(randomName[index++]);
                                structType.add("enumS&TCycle");
                            }
                            node.setId("enum" + type);
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().equals("{")){
                            line++;
                            if(!isCycle())
                                strT = "typedef enum " + name + " {";
                            else{
                                strT = "\te_" + randomName[index] + " " + name +";";
                                structName.add(randomName[index]);
                                index++;
                                structType.add("enumS&TCycle");
                            }
                            writeLine(strT);
                            strT = br.readLine();
                            line++;
                            node.setId("enum" + type);
                            enumDefineManage(strT);
                        }
                        else if(strT.trim().indexOf("{") == 0 ){
                            line++;
                            String tep ;
                            if (!isCycle())
                                tep = "typedef enum " + name + " {";
                            else {
                                tep = "\te_" + randomName[index] + " " + name +";" ;
                                structName.add(randomName[index++]);
                                structType.add("enumS&TCycle");
                            }
                            writeLine(tep);
                            node.setId("enum" + type);
                            enumDefineManage(strT);
                        }
                        else{
                            br.reset();
                            if(notDefinnedTypes!=null && notDefinnedTypes.indexOf(name) != -1){
                                useButNotDefinedManage(typeDefine);
                            }
                            else {
                                String str = "";
                                String typeStr = "typedef OCTET_STRING_t " + type + "_t";
                                if(isUpLow(name) == 0)
                                    str = "typedef " + type + "_t" + " " + name + "_t" + ";";
                                else{
                                    for(int i=0;i<numCycle;i++)
                                        str+="\t";
                                    str += type + "_t" + " " + name + ";";
                                }
                                if(!isHeadDefined(typeStr))
                                    writeHline(typeStr);
                                writeLine(str);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    addAsnList(node,name);
                }
            }
            else{
                if(isDefined(name)){
                    System.out.println("第" + line + "行重复定义");
                }
                else{
                    graNode node = new graNode();
                    node.setId(type);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    try {
                        if(!isDefined(type)){
                            notDefinedLines.add(line);              //记录未定义出现的行数
                            notDefinnedTypes.add(type);             //记录未定义类型
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    addAsnList(node,name);
                    String str = "";
                    if(isUpLow(name) == 0)
                        str = "typedef " + type + "_t" + " " + name + "_t" + ";";
                    else{
                        for(int i=0;i<numCycle;i++)
                            str+="\t";
                        str += type + "_t" + " " + name + ";";
                    }
                    writeLine(str);
                }
            }
        }

        return isCycle();
    }

    //值定义处理
    private void valueDefineManage (String valueDefine){
        spiltValueDefine(valueDefine);
        boolean isASNType = false;
        String valueStr = "";
        for (int i = 0;i < asnType.length;i++){
            if(asnType[i].equals(type)){
                isASNType=true;
                break;
            }
        }
        if (isASNType) {
            if (isDefined(name)){
                System.out.println("第" + line + "行重复定义");
            }
            else {
                graNode node = new graNode();
                node.setType(1);
                node.setLine(line);
                String temp = "";
                if (!value.contains("{") && !value.contains("\"")){
                    if (!type.equals("INTEGER") && !type.equals("REAL")) {
                        System.out.println("第" + line + "行value与type不匹配，不能将" + value + "赋给" + type + "类型");
                    }
                    else{
                        valueStr = type + "_t " + name + " = " + value + ";";
                        node.setId("INTEGER");
                        node.setValue(value);
                        node.setName(name);
                        String contents = "";
                        if (Integer.parseInt(value) > 0)
                            contents = Integer.toBinaryString(Integer.parseInt(value));
                        else {
                            contents = Integer.toBinaryString((Integer.parseInt(value) & 0x7f) + 0x80);
                        }
                        String length = Integer.toBinaryString(contents.length());
                        writeCodeLine(length + " " + contents);
                    }

                }
                else if (value.contains("\"") && value.indexOf("\"") == 0){
                    if (type.contains("String") || type.contains("STRING")) {
                        valueStr = type + "_t " + name + " = " + value + ";";
                        node.setName(name);
                        node.setId("String");
                        node.setValue(value.replace("\"","").trim());
                        if (type.contains("BIT")) {
                            String contents = value.replace("\"","").trim();
                            String length = Integer.toBinaryString(contents.length());
                            writeCodeLine(length + " " + contents);
                        }
                        else {
                            String contents = "";
                            char[] contentsChar = value.toCharArray();
                            for (int i = 0; i < contentsChar.length; i++) {
                                contents += Integer.toBinaryString(contentsChar[i]);
                            }
                            String length = Integer.toBinaryString(contents.length());
                            writeCodeLine(length + " " + contents);
                        }
                    }
                    else
                        System.out.println("第" + line + "行不能将字符型值赋给非String类型");
                }
                else if (value.contains("{")){    //ASN基本类型里应该是不会出现这种类型的，ASN对于数组一定是先定义，之后才能进行赋值。
//                    valueStr = type + "_t[] " + name + " = { ";
//                    String values[] = value.substring(value.indexOf("{") + 1,value.indexOf("}")).split(",");
//                    for (int i = 0;i < values.length;i++) {
//                        valueStr += values[i] + ", ";
//                    }
//                    valueStr += "};";  //而且这句写错了，最后字符串会多出一个逗号
                    System.out.println("不能将值数组赋给非数组类型");
                }
                else {
                    String valueSpilt[] = value.split(":");
                    if (isDefined(valueSpilt[0])){
                        if (isDefined(valueSpilt[1].substring(1,valueSpilt[1].length()-1))){
                            graNode tempNode = userDefineList[getCharid(valueSpilt[1].substring(1,valueSpilt[1].length()-1))].head;
                            while (tempNode != null){
                                if(tempNode.getName().equals(valueSpilt[1].substring(1,valueSpilt[1].length()-1))){
                                    valueStr = type + "_t" + name + " = " + tempNode.getValue() + ";";
                                    node.setId(valueSpilt[0]);
                                    node.setName(name);
                                    node.setValue(tempNode.getValue());
                                    String contents = "";
                                    if (Integer.parseInt(tempNode.getValue()) > 0)
                                        contents = Integer.toBinaryString(Integer.parseInt(tempNode.getValue()));
                                    else {
                                        contents = Integer.toBinaryString(((Integer.parseInt(tempNode.getValue()) & 0x7f) + 0x80));
                                    }
                                    String length = Integer.toBinaryString(contents.length());
                                    writeCodeLine(length + " " + contents);
                                    break;
                                }
                                tempNode = tempNode.next;
                            }
                        }
                        else {
                            System.out.println(valueSpilt[1].substring(1,valueSpilt[1].length()-1) + "未定义");
                        }
                    }
                    else {
                        try {
                            boolean isFind = false;
                            br.mark(8192);
                            String tempStr = br.readLine();
                            while (tempStr != null) {
                                if (tempStr.contains(valueSpilt[1].replace("\"",""))) {
                                    if (tempStr.indexOf(",") == tempStr.length()-1 || !tempStr.contains(",")){
                                        valueStr = type + "_t " + name + " = " + tempStr.substring(tempStr.indexOf("(") + 1,tempStr.indexOf(")")) + ";";
                                        node.setName(name);
                                        node.setId("enum_value_define");
                                        node.setValue(tempStr.substring(tempStr.indexOf("(") + 1,tempStr.indexOf(")")));
                                        String contents = Integer.toBinaryString(Integer.parseInt(node.getValue()));
                                        String length = Integer.toBinaryString(contents.length());
                                        writeCodeLine(length + " " + contents);
                                    }

                                    else {
                                        String valuesSpilt[] = tempStr.split(",");
                                        for (int i = 0;i < valuesSpilt.length;i++) {
                                            if (valuesSpilt[i].contains(valueSpilt[1].replace("\"",""))) {
                                                valueStr = type + "_t " + name + " = " + valuesSpilt[i].substring(valuesSpilt[i].indexOf("(") + 1,valuesSpilt[i].indexOf(")")) + ";";
                                                node.setName(name);
                                                node.setId("enum_value_define");
                                                node.setValue(valuesSpilt[i].substring(valuesSpilt[i].indexOf("(") + 1,valuesSpilt[i].indexOf(")")));
                                                //System.out.println("fuyi:" + node.getValue());
                                                String contents = "";
                                                if (Integer.parseInt(node.getValue()) > 0)
                                                    contents = Integer.toBinaryString(Integer.parseInt(node.getValue()));
                                                else
                                                    contents = Integer.toBinaryString((Integer.parseInt(node.getValue()) & 0x7f) + 0x80);
                                                String length = Integer.toBinaryString((contents.length()));
                                                writeCodeLine(length + " " + contents);
                                                break;
                                            }
                                        }
                                    }
                                    isFind = true;
                                    break;
                                }
                                tempStr = br.readLine();
                            }
                            if (!isFind)
                                System.out.println(valueSpilt[1].substring(1,valueSpilt[1].length()-1) + "未定义");
                            br.reset();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (type.equals("INTEGER") || type.equals("REAL")) {
                     temp = "typedef ASN_PRIMITIVE_TYPE_t " + type + "_t;";
                }
                else if (type.equals("NULL"))
                    temp = "typedef int NULL_t";
                else if (type.equals("BOOLEAN"))
                    temp = "typedef int BOOLEAN_t";
                else if (type.equals("ENUMERATED"))
                    temp = "typedef INTEGER_t ENUMERATED_t;";
                else if (type.equals("OCTET_STRING")){
                    String typeStr[] = {"typedef struct OCTET_STRING {","\tuint8_t *buf;","\tint size;","}OCTET_STRING_t;"};
                    if(!isHeadDefined(typeStr[0]))
                        for(int i =0;i<typeStr.length;i++)
                            writeHline(typeStr[i]);
                }
                else if (type.equals("BIT_STRING")) {
                    String temps[] = {"typedef struct BIT_STRING_s{","\tuint8_t *buf;","\tint size;","\tint bits_unused;","}BIT_STRING_t;"};
                    if(!isHeadDefined(temps[0]))
                        for(int i =0;i<temps.length;i++)
                            writeHline(temps[i]);
                }
                else if (type.contains("String") || type.contains("Time")) {
                    temp = "typedef OCTET_STRING_t " + type + "_t";
                }
                if (!isPrimType) {
                    for (int i = 0; i < primTypeStr.length; i++) {
                        writeHline(primTypeStr[i]);
                    }
                    isPrimType = true;
                }
                if ( !temp.equals("") && !isHeadDefined(temp))
                    writeHline(temp);
                if ( valueStr != null || !valueStr.equals(""))
                    writeLine(valueStr);
                if (!node.getValue().equals("-1"))
                    addAsnList(node,node.getName());
            }
        }
        else {
            if (isDefined(type)){
                if (isDefined(name)) {
                    System.out.println("第" + line + "行重复定义");
                }
                else {
                    graNode node = new graNode();
                    node.setLine(line);
                    node.setType(1);

                    if (!value.contains("{") && !value.contains("\"")) {
                        graNode tempNode = userDefineList[getCharid(type)].head;
                        while (tempNode != null) {
                            if (tempNode.getName().equals(type)) {
                                if (tempNode.getId().equals("INTEGER") || tempNode.getId().equals("REAL")) {
                                    valueStr += type + "_t " + name + " = " + value + ";";
                                    node.setId(type);
                                    node.setName(name);
                                    node.setValue(value);
                                    String contents = "";
                                    if (Integer.parseInt(value) > 0)
                                        contents = Integer.toBinaryString(Integer.parseInt(value));
                                    else
                                        contents = (Integer.toBinaryString((Integer.parseInt(value) & 0x7f) + 0x80));
                                    String length = Integer.toBinaryString(contents.length());
                                    writeCodeLine(length + " " + contents);
                                    break;
                                }
                                else{
                                    System.out.println("不能将值" + value + "赋值给类型" + type);
                                    break;
                                }
                            }
                            tempNode = tempNode.next;
                        }
                    }
                    else if (value.contains("\"") && value.indexOf("\"") == 0) {
                        graNode tempNode = userDefineList[getCharid(type)].head;
                        while (tempNode != null) {
                            if (tempNode.getName().equals(type)) {
                                if (tempNode.getId().contains("String") || tempNode.getId().contains("STRING")) {
                                    valueStr += type + "_t " + name + " = " + value + ";";
                                    node.setId(type);
                                    node.setName(name);
                                    node.setValue(value);
                                    String contents = "";
                                    char[] contentsChar = value.toCharArray();
                                    for (int i=0;i<contentsChar.length;i++) {
                                        contents += Integer.toBinaryString(contentsChar[i]);
                                    }
                                    String length = Integer.toBinaryString(contents.length());
                                    writeCodeLine(length + " " + contents);
                                }
                                else
                                    System.out.println("不能将" + value + "赋值给非String类型");
                                break;
                            }
                            tempNode = tempNode.next;
                        }
                    }
                    else if (value.contains("{")) {
                        graNode tempNode = userDefineList[getCharid(type)].head;
                        while (tempNode != null) {
                            if (tempNode.getName().equals(type)) {
                                if (tempNode.getId().equals("SEQUENCE OF") || tempNode.getId().equals("SET OF")) {
                                    valueStr += type + "_t[] " + name + " = {";
                                    String contents = "";
                                    String values[] = value.substring(value.indexOf("{") + 1,value.indexOf("}")).split(",");
                                    for (int i = 0;i < values.length;i++){
                                        valueStr += values[i] + ", ";
                                        contents += Integer.toBinaryString(Integer.parseInt(values[i]));
                                    }
                                    valueStr = valueStr.substring(0,valueStr.length()-2) + "};";
                                    node.setId(type);
                                    node.setName(name);
                                    node.setValue(value);
                                    String length = Integer.toBinaryString(values.length);
                                    writeCodeLine(length + " " + contents);
                                }
                                break;
                            }
                            tempNode = tempNode.next;
                        }
                    }
                    else {
                        String valueSpilt[] = value.split(":");
                        if (isDefined(valueSpilt[0])) {
                            if (isDefined(valueSpilt[1].substring(1,valueSpilt[1].length()-1))) {
                                graNode tempNode = userDefineList[getCharid(valueSpilt[1].substring(1,valueSpilt[1].length()-1))].head;
                                while (tempNode != null) {
                                    if (tempNode.getName().equals(valueSpilt[1].substring(1,valueSpilt[1].length()-1))){
                                        valueStr += type + "_t " + name + " = " + tempNode.getValue() + ";";
                                        node.setId(type);
                                        node.setValue(tempNode.getValue());
                                        node.setName(name);
                                        String contents = "";
                                        if (Integer.parseInt(tempNode.getValue()) < 0 )
                                            contents = Integer.toBinaryString((Integer.parseInt(tempNode.getValue()) & 0x7f) + 0x80);
                                        else
                                            contents = Integer.toBinaryString(Integer.parseInt(tempNode.getValue()));
                                        String length = Integer.toBinaryString(contents.length());
                                        writeCodeLine(length + " " + contents);
                                        break;
                                    }
                                    tempNode = tempNode.next;
                                }
                            }
                            else {
                                System.out.println("第" + line + "行" + valueSpilt[1].substring(1,valueSpilt[1].length()-1) + "未找到定义");
                            }
                        }
                        else {
                            try {
                                br.mark(8192);
                                String tempStr = br.readLine();
                                boolean isFind = false;
                                while (tempStr != null) {
                                    if (tempStr.contains(valueSpilt[1].replace("\"",""))) {
                                        node.setName(name);
                                        node.setId(type);
                                        node.setValue(tempStr.substring(tempStr.indexOf("(") + 1,tempStr.indexOf(")")));
                                        if (tempStr.indexOf(",") == tempStr.length()-1 || !tempStr.contains(",")){
                                            valueStr = type + "_t " + name + " = " + tempStr.substring(tempStr.indexOf("(") + 1,tempStr.indexOf(")")) + ";";
                                            String contents = "";
                                            if (Integer.parseInt(tempStr.substring(tempStr.indexOf("(") + 1,tempStr.indexOf(")"))) < 0)
                                                contents = Integer.toBinaryString((Integer.parseInt(tempStr.substring(tempStr.indexOf("(") + 1,tempStr.indexOf(")"))) & 0x7f) + 0x80);
                                            else
                                                contents = Integer.toBinaryString(Integer.parseInt(tempStr.substring(tempStr.indexOf("(") + 1,tempStr.indexOf(")"))));
                                            String length = Integer.toBinaryString(contents.length());
                                            writeCodeLine(length + " " + contents);
                                        }

                                        else {
                                            String valuesSpilt[] = tempStr.split(",");
                                            for (int i = 0;i < valuesSpilt.length;i++) {
                                                if (valuesSpilt[i].contains(valueSpilt[1].replace("\"",""))) {
                                                    valueStr = type + "_t " + name + " = " + valuesSpilt[i].substring(valuesSpilt[i].indexOf("(") + 1,valuesSpilt[i].indexOf(")")) + ";";
                                                    String contents = "";
                                                    if (Integer.parseInt(valuesSpilt[i].substring(valuesSpilt[i].indexOf("(") + 1,valuesSpilt[i].indexOf(")"))) < 0)
                                                        contents = Integer.toBinaryString((Integer.parseInt(valuesSpilt[i].substring(valuesSpilt[i].indexOf("(") + 1,valuesSpilt[i].indexOf(")"))) & 0x7f) + 0x80);
                                                    else
                                                        contents = Integer.toBinaryString(Integer.parseInt(valuesSpilt[i].substring(valuesSpilt[i].indexOf("(") + 1,valuesSpilt[i].indexOf(")"))));
                                                    String length = Integer.toBinaryString(contents.length());
                                                    writeCodeLine(length + " " + contents);
                                                    break;
                                                }
                                            }
                                        }
                                        isFind = true;
                                        break;
                                    }
                                    tempStr = br.readLine();
                                }
                                if (!isFind)
                                    System.out.println("第" + line + "行" + valueSpilt[1].substring(1,valueSpilt[1].length()-1) + "未找到定义");
                                br.reset();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (valueStr != null || !valueStr.equals(""))
                        writeLine(valueStr);
                    if (!node.getValue().equals("-1"))
                        addAsnList(node,node.getName());
                }
            }
            else {
                if (isDefined(name))
                    System.out.println("第" + line + "行出现重复定义");
                else {
                    notDefinnedTypes.add(type);
                    notDefinedLines.add(line);
                    graNode node = new graNode();
                    node.setLine(line);
                    node.setType(1);
                    try {
                        br.mark(8192);
                        String tempStr = br.readLine();
                        boolean isFind = false;
                        while (tempStr!=null) {
                            if (tempStr.contains(type)) {
                                if (!value.contains("{") && !value.contains("\"")){
                                    if (!tempStr.contains("INTEGER") && !tempStr.contains("REAL")) {
                                        System.out.println("第" + line + "行value与type不匹配，不能将" + value + "赋给" + type + "类型");
                                    }
                                    else{
                                        valueStr = type + "_t " + name + " = " + value + ";";
                                        node.setName(name);
                                        node.setValue(value);
                                        node.setId(type);
                                        String contents = "";
                                        if (Integer.parseInt(value) > 0)
                                            contents = Integer.toBinaryString(Integer.parseInt(value));
                                        else
                                            contents = (Integer.toBinaryString((Integer.parseInt(value) & 0x7f) + 0x80));
                                        String length = Integer.toBinaryString(contents.length());
                                        writeCodeLine(length + " " + contents);
                                    }

                                }
                                else if (value.contains("\"") && value.indexOf("\"") == 0){
                                    if (tempStr.contains("String") || tempStr.contains("STRING")) {
                                        valueStr = type + "_t " + name + " = " + value + ";";
                                        node.setId(type);
                                        node.setName(name);
                                        node.setValue(value);
                                        String contents = "";
                                        char[] contentsChar = value.toCharArray();
                                        for (int i=0;i<contentsChar.length;i++) {
                                            contents += Integer.toBinaryString(contentsChar[i]);
                                        }
                                        String length = Integer.toBinaryString(contents.length());
                                        writeCodeLine(length + " " + contents);
                                    }
                                    else
                                        System.out.println("第" + line + "行不能将字符型值赋给非String类型");
                                }
                                else if (value.contains("{")){
                                    if (tempStr.contains("SEQUENCE OF") || tempStr.contains("SET OF")) {
                                        valueStr += type + "_t[] " + name + " = {";
                                        String contents = "";
                                        String values[] = value.substring(value.indexOf("{") + 1,value.indexOf("}")).split(",");
                                        for (int i = 0;i < values.length;i++){
                                            valueStr += values[i] + ", ";
                                            contents += Integer.toBinaryString(Integer.parseInt(values[i]));
                                        }
                                        valueStr = valueStr.substring(0,valueStr.length()-2) + "};";
                                        node.setId(type);
                                        node.setName(name);
                                        node.setValue(value);
                                        String length = Integer.toBinaryString(values.length);
                                        writeCodeLine(length + " " + contents);
                                    }
                                }
                                else {
                                    String valueSpilt[] = value.split(":");
                                    if (isDefined(valueSpilt[0])) {
                                        if (isDefined(valueSpilt[1].substring(1,valueSpilt[1].length()-1))) {
                                            graNode tempNode = userDefineList[getCharid(valueSpilt[1].substring(1,valueSpilt[1].length()-1))].head;
                                            while (tempNode != null) {
                                                if (tempNode.getName().equals(valueSpilt[1].substring(1,valueSpilt[1].length()-1))){
                                                    valueStr += type + "_t " + name + " = " + tempNode.getValue() + ";";
                                                    node.setId(type);
                                                    node.setValue(tempNode.getValue());
                                                    node.setName(name);
                                                    String contents = "";
                                                    if (Integer.parseInt(tempNode.getValue()) < 0 )
                                                        contents = Integer.toBinaryString((Integer.parseInt(tempNode.getValue()) & 0x7f) + 0x80);
                                                    else
                                                        contents = Integer.toBinaryString(Integer.parseInt(tempNode.getValue()));
                                                    String length = Integer.toBinaryString(contents.length());
                                                    writeCodeLine(length + " " + contents);
                                                    break;
                                                }
                                                tempNode = tempNode.next;
                                            }
                                        }
                                        else {
                                            System.out.println("第" + line + "行" + valueSpilt[1].substring(1,valueSpilt[1].length()-1) + "未找到定义");
                                        }
                                    }
                                    else {
                                        try {
                                            br.reset();
                                            br.mark(8192);
                                            String tempStr1 = br.readLine();
                                            boolean isFound = false;
                                            while (tempStr1 != null) {
                                                if (tempStr1.contains(valueSpilt[1].replace("\"",""))) {
                                                    if (tempStr1.indexOf(",") == tempStr1.length()-1 || !tempStr1.contains(",")){
                                                        valueStr = type + "_t " + name + " = " + tempStr1.substring(tempStr1.indexOf("(") + 1,tempStr1.indexOf(")")) + ";";
                                                        String contents = "";
                                                        if (Integer.parseInt(tempStr1.substring(tempStr1.indexOf("(") + 1,tempStr1.indexOf(")"))) < 0)
                                                            contents = Integer.toBinaryString((Integer.parseInt(tempStr1.substring(tempStr1.indexOf("(") + 1,tempStr1.indexOf(")"))) & 0x7f) + 0x80);
                                                        else
                                                            contents = Integer.toBinaryString(Integer.parseInt(tempStr1.substring(tempStr1.indexOf("(") + 1,tempStr1.indexOf(")"))));
                                                        String length = Integer.toBinaryString(contents.length());
                                                        writeCodeLine(length + " " + contents);
                                                    }
                                                    else {
                                                        String valuesSpilt[] = tempStr1.split(",");
                                                        for (int i = 0;i < valuesSpilt.length;i++) {
                                                            if (valuesSpilt[i].contains(valueSpilt[1].replace("\"",""))) {
                                                                valueStr = type + "_t " + name + " = " + valuesSpilt[i].substring(valuesSpilt[i].indexOf("(") + 1,valuesSpilt[i].indexOf(")")) + ";";
                                                                String contents = "";
                                                                if (Integer.parseInt(valueSpilt[1].substring(valueSpilt[1].indexOf("(") + 1,valueSpilt[1].indexOf(")"))) < 0)
                                                                    contents = Integer.toBinaryString((Integer.parseInt(valueSpilt[1].substring(valueSpilt[1].indexOf("(") + 1,valueSpilt[1].indexOf(")"))) & 0x7f) + 0x80);
                                                                else
                                                                    contents = Integer.toBinaryString(Integer.parseInt(valueSpilt[1].substring(valueSpilt[1].indexOf("(") + 1,valueSpilt[1].indexOf(")"))));
                                                                String length = Integer.toBinaryString(contents.length());
                                                                writeCodeLine(length + " " + contents);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    isFound = true;
                                                    break;
                                                }
                                                tempStr1 = br.readLine();
                                            }
                                            if (!isFound)
                                                System.out.println("第" + line + "行" + valueSpilt[1].substring(1,valueSpilt[1].length()-1) + "未找到定义");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                isFind = true;
                                if (valueStr != null || !valueStr.equals(""))
                                    writeLine(valueStr);
                                if (!node.getValue().equals("-1"))
                                    addAsnList(node,node.getName());
                                break;
                            }
                            tempStr = br.readLine();
                        }
                        if (!isFind)
                            System.out.println("第" + line + "行" + type + "未找到定义");
                        br.reset();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //处理使用未定义现象时向.c文件头添加定义
    private void writeAddDefine(String addStr){
        try {
            bw.flush();
            bw.close();
            addNotDefined = new RandomAccessFile(System.getProperty("user.dir") + "/asn.c","rw");
            byte[] b = addStr.getBytes();
            addNotDefined.setLength(addNotDefined.length() + b.length);
            //把后面的内容往后面挪
            for (long i = addNotDefined.length() - 1; i > b.length + skip - 1; i--) {
                addNotDefined.seek(i - b.length);
                byte temp = addNotDefined.readByte();
                addNotDefined.seek(i);
                addNotDefined.writeByte(temp);
            }
            addNotDefined.seek(skip);
            addNotDefined.write(b);
            addNotDefined.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //对于枚举定义的处理
    private void enumDefineManage(String typeDefine){
        String temp = typeDefine.replace("{","").trim();
        graNode node = new graNode();
        if(!isCycle()){
            try {
                if(temp.indexOf(",") == -1 || temp.indexOf(",") == temp.length()-1){
                    while(!temp.contains("}")){
                        node.setId("enum");
                        node.setName((temp.substring(0,temp.indexOf("("))));
                        node.setType(1);
                        node.setLine(line);
                        node.setValue(temp.substring(temp.indexOf("(")+1,temp.indexOf(")")));
                        addAsnList(node,(temp.substring(0,temp.indexOf("("))));
                        writeLine("\t" + name + "_" + (temp.substring(0,temp.indexOf("("))).replace("-","_") + " = " + temp.substring(temp.indexOf("(")+1,temp.indexOf(")")) + ";");
                        temp = br.readLine();
                        line++;
                    }
                    if(temp.trim().indexOf("}") != 0){
                        temp = temp.replace("}","").trim();
                        node.setId("enum");
                        node.setName((temp.substring(0,temp.indexOf("("))));
                        node.setType(1);
                        node.setLine(line);
                        node.setValue(temp.substring(temp.indexOf("(")+1,temp.indexOf(")")));
                        addAsnList(node,(temp.substring(0,temp.indexOf("("))));
                        writeLine("\t" + name + "_" + (temp.substring(0,temp.indexOf("("))).replace("-","_") + " = " + temp.substring(temp.indexOf("(")+1,temp.indexOf(")")) + ";");
                    }
                    else
                        writeLine("}e_" + name + ";");
                }
                else{
                    String strs[] = temp.split(",");
                    for(int i=0;i<strs.length;i++){
                        node = new graNode();
                        temp = strs[i].trim();
                        node.setId("enum");
                        node.setName((strs[i].substring(0,temp.indexOf("("))));
                        node.setType(1);
                        node.setLine(line);
                        node.setValue(strs[i].substring(temp.indexOf("(")+1,temp.indexOf(")")));
                        addAsnList(node,(strs[i].substring(0,strs[i].indexOf("("))));
                        writeLine("\t" + name + "_" + (temp.substring(0,temp.indexOf("("))).replace("-","_") + " = " + temp.substring(temp.indexOf("(")+1,temp.indexOf(")")) + ";");
                    }
                    temp = br.readLine();
                    line++;
                    enumDefineManage(temp);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            if(addDefinition.equals("")||addDefinition==null)
                addDefinition += "typedef enum " + structName.get(structName.size()-1) + "{\n";
            try {
                if(temp.indexOf(",") == -1 || temp.indexOf(",") == temp.length()-1){
                    while (!temp.contains("}")){
                        node = new graNode();
                        addDefinition += "\t" + structName.get(structName.size()-1) + "_" + (temp.substring(0,temp.indexOf("("))).replace("-","_") + " = " + temp.substring(temp.indexOf("(")+1,temp.indexOf(")")) + ";\n";
                        node.setId("enum");
                        node.setName((temp.substring(0,temp.indexOf("("))));
                        node.setType(1);
                        node.setLine(line);
                        node.setValue(temp.substring(temp.indexOf("(")+1,temp.indexOf(")")));
                        addAsnList(node,(temp.substring(0,temp.indexOf("("))));
                        temp = br.readLine();
                        line++;
                    }
                    if(temp.trim().indexOf("}")!=0){
                        temp = temp.replace("}","").trim();
                        addDefinition += "\t" + structName.get(structName.size()-1) + "_" + (temp.substring(0,temp.indexOf("("))).replace("-","_") + " = " + temp.substring(temp.indexOf("(")+1,temp.indexOf(")")) + ";\n";
                        node.setId("enum");
                        node.setName((temp.substring(0,temp.indexOf("("))));
                        node.setType(1);
                        node.setLine(line);
                        node.setValue(temp.substring(temp.indexOf("(")+1,temp.indexOf(")")));
                        addAsnList(node,(temp.substring(0,temp.indexOf("("))));
                    }
                    else{
                        addDefinition += "} e_" + structName.get(structName.size()-1) + ";\n";
                        structName.remove(structName.size()-1);
                        structType.remove(structType.size()-1);
                        writeAddDefine(addDefinition);
                        addDefinition = "";
                        try {
                            addNotDefined.close();
                            bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/asn.c",true));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    String douSpilt[] = temp.split(",");
                    for(int i = 0;i<douSpilt.length;i++){
                        temp = douSpilt[i].trim();
                        node = new graNode();
                        addDefinition += "\t" + structName.get(structName.size()-1) + "_" + (temp.substring(0,temp.indexOf("("))).replace("-","_") + " = " + temp.substring(temp.indexOf("(")+1,temp.indexOf(")")) + ";\n";
                        node.setId("enum");
                        node.setName((temp.substring(0,temp.indexOf("("))));
                        node.setType(1);
                        node.setLine(line);
                        node.setValue(temp.substring(temp.indexOf("(")+1,temp.indexOf(")")));
                        addAsnList(node,(temp.substring(0,temp.indexOf("("))));
                    }
                    temp = br.readLine();
                    line++;
                    enumDefineManage(temp);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    //使用而未定义处理
    private void useButNotDefinedManage(String typeDefine){  //使用未定义类型处理
        spiltTypeDefine(typeDefine);
        graNode node = new graNode();
        String writeLine = "";
        try {
            //FileWriter addNotDefined = new FileWriter(System.getProperty("user.dir") + "/asn.c",true);

            if(type.equals("SEQUENCE")){
                node.setId(type);
                node.setName(name);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,name);
                if(!isCycle())
                    writeLine = "typedef struct " + name + "{\n";
                else
                    writeLine = "typedef struct " + structName.get(structName.size()-1) + "{\n";
                writeLine += writeLineStr();
                writeLine += "}" + structName.get(structName.size()-1) + "_t;\n";
                writeAddDefine(writeLine);
                delete();

            }
            else if(typeDefine.contains("{") && typeDefine.contains("}")){
                node.setId(type);
                node.setName(randomName[index]);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,randomName[index]);
                writeLine = "typedef " + type.replace(" ","_") + "_t " +randomName[index] + "_t;\n";
                writeAddDefine(writeLine);
                deleteWithoutStruct(randomName[index++]);
            }
            else if(typeDefine.contains("OCTET STRING")){
                node.setId("OCTET STRING");
                node.setName(name);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,name);
                writeLine = "\ntypedef OCTET_STRING_t " + name +"_t\n";
                writeAddDefine(writeLine);
                deleteWithoutStruct(name);
            }
            else if(typeDefine.contains("BIT STRING")){
                node.setId("BIT STRING");
                node.setName(name);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,name);
                writeLine = "\ntypedef BIT_STRING_t " + name +"_t\n";
                writeAddDefine(writeLine);
                deleteWithoutStruct(name);
            }
            else if(typeDefine.contains("SEQUENCE OF")){
                node.setId("SEQUENCE OF");
                node.setName(name);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,name);
                writeLine = "\ntypedef A_SEQUENCE_OF " + name +"_t\n";
                writeAddDefine(writeLine);
                deleteWithoutStruct(name);
            }
            else if(typeDefine.contains("SET OF")){
                node.setId("SET OF");
                node.setName(name);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,name);
                writeLine = "\ntypedef A_SET_OF " + name +"_t\n";
                writeAddDefine(writeLine);
                deleteWithoutStruct(name);
            }
            else if(type.equals("SET")){
                node.setId(type);
                node.setName(name);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,name);
                if(!isCycle())
                    writeLine = "typedef struct " + name + "{\n";
                else
                    writeLine = "typedef struct " + structName.get(structName.size()-1) + "{\n";
                writeLine += writeLineStr();
                writeLine +="\tunsigned int _presence_map\n" +
                        "\t\t[((4+(8*sizeof(unsigned int))-1)/(8*sizeof(unsigned int)))];\n" + "}" + structName.get(structName.size()-1) + "_t;\n";
                writeAddDefine(writeLine);
                delete();
            }
            else if(type.equals("CHOICE")){
                node.setId(type);
                node.setName(name);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,name);
                if(!isCycle())
                    writeLine = "typedef union " + name + "{\n";
                else
                    writeLine = "typedef union " + structName.get(structName.size()-1) + "{\n";
                writeLine += writeLineStr();
                writeLine += "}" + structName.get(structName.size()-1) + "_t;\n";
                writeAddDefine(writeLine);
                delete();
            }

            else{
                node.setId(type);
                node.setName(name);
                node.setLine(line);
                node.setType(0);
                addAsnList(node,name);
                writeLine = "\ntypedef " + type + "_t " + name + "_t;\n";
                writeAddDefine(writeLine);
                deleteWithoutStruct(name);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            addNotDefined.close();
            bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/asn.c",true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //关键字处理
    private void keyWordManage(String typeDefine){           //对于关键字的处理
        graNode node = new graNode();
        try {
            if(typeDefine.contains("DEFAULT")){
                String[] strs = typeDefine.replace("{","").split("\\s+");
                if(strs.length == 4){
                    try {
                        if(!isDefined(strs[1])){
                            notDefinedLines.add(line);              //记录未定义出现的行数
                            notDefinnedTypes.add(strs[1]);             //记录未定义类型
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String id;
                    if(typeDefine.contains("["))
                        id = strs[1].substring(strs[1].indexOf("]")+1,strs[1].length());
                    else
                        id = strs[1] ;
                    node.setId(id + "_DEFAULT");
                    node.setType(0);
                    node.setLine(line);
                    node.setName(strs[0]);
                    addAsnList(node,strs[0]);
                    writeLine("\t" + id + "_t *" + strs[0] + " /* DEFAULT " + strs[3].replace(",","") + " */;");
                    //typeDefine = br.readLine();
                }
                else
                    System.out.println("第" + line + "行关键字使用错误");
            }
            else if(typeDefine.contains("OPTIONAL")){
                String[] strs = typeDefine.replace("{","").split("\\s+");
                if(strs.length == 3){
                    try {
                        if(!isDefined(strs[1])){
                            notDefinedLines.add(line);              //记录未定义出现的行数
                            notDefinnedTypes.add(strs[1]);             //记录未定义类型
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String id;
                    if(typeDefine.contains("["))
                        id = strs[1].substring(strs[1].indexOf("]")+1,strs[1].length());
                    else
                        id = strs[1];
                    node.setId(id);
                    node.setType(0);
                    node.setLine(line);
                    node.setName(strs[0]);
                    addAsnList(node,strs[0]);
                    writeLine("\t" + id + "_t *" + strs[0] + " /* OPTIONAL */;");
                    //typeDefine = br.readLine();
                }
                else
                    System.out.println("第" + line + "行关键字使用错误");
            }
            else if(typeDefine.contains("COMPONENTS OF")){
                String temp[] = typeDefine.replace("{","").split("\\s+");
                if(temp.length == 3){
                    BufferedReader skim = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/asn.txt"));
                    String content = skim.readLine();
                    boolean trag = false;
                    while(content != null){
                        if(content.indexOf(temp[2].replace(",","")) == 0){
                            trag = true;
                            content = skim.readLine();
                            if (content.trim().equals("{"))
                                content = skim.readLine();
                            String defineContnet = content.replace("{","");
                            while(!defineContnet.contains("}")){
                                spiltTypeDefine(defineContnet.trim());
                                writeLine("\t" + type + "_t " + name + ";");
                                defineContnet = skim.readLine();
                            }
                            if(defineContnet.indexOf("}") !=0){
                                defineContnet = content.replace("}","");
                                spiltTypeDefine(defineContnet);
                                writeLine("\t" + type + "_t " + name + ";");
                            }
                            //typeDefine = br.readLine();
                            break;
                        }
                        content = skim.readLine();
                    }
                    if(!trag){
                        System.out.println("第" + line + "行COMPONENTS OF 后的类型未定义");
                    }
                    skim.close();
                }
            }
            else if(typeDefine.contains("...")){
                writeLine("\t/*\n" +
                        "\t * This type is extensible,\n" +
                        "\t * possible extensions are below.\n" +
                        "\t */");
                //typeDefine = br.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        //return typeDefine.trim();
    }

    //约束处理
    private void constraintManage(String typeDefine){
        graNode node = new graNode();
        node.setConstraint(true);
        node.setLine(line);
        node.setType(0);
        if (typeDefine.contains("SEQUENCE OF") || typeDefine.contains("SET OF")){
            spiltTypeDefine(typeDefine.trim());
            String temp;
            node.setId("OF");
            node.setName(name);
            boolean trag = false;    //OF后是否为ASN基本类型
            if(type.contains("SEQUENCE OF"))
                temp = "A_SEQUENCE_OF";
            else
                temp = "A_SET_OF";
            String typeName[] = type.split("\\s+");
            writeLine("typedef struct " + name + " {\n\t" + temp + "(" + typeName[2].substring(0,typeName[2].indexOf("(") ) + "_t) list;\n}" + name + "_t") ;
            for(int i = 0;i<asnType.length;i++){
                if (asnType[i].equals(typeName[2].substring(0,typeName[2].indexOf("(") ))){
                    trag = true;
                    break;
                }
            }
            if(!isDefined(typeName[2].substring(0,typeName[2].indexOf("(") )) && !trag){
                notDefinnedTypes.add(typeName[2].substring(0,typeName[2].indexOf("(") -1 ));
                notDefinedLines.add(line);
            }
            constraintString += getConstraintString(typeDefine);
        }
        else if (typeDefine.contains("String") || typeDefine.contains("STRING")){
            spiltTypeDefine(typeDefine.trim());
            node.setId("String");
            node.setName(name);
            writeLine("typedef " + type.substring(0,type.indexOf("(")).replace(" ","_").trim() + "_t " + name + "_t" );
            constraintString += getConstraintString(typeDefine);
        }
        else if (typeDefine.contains("REAL")){
            //暂时这里没有处理，因为文档中给的约束定义对于asn1c工具而言，有语法错误，故我也不知道应该翻译成什么样子
        }
        else if (typeDefine.contains("BOOLEAN")){
            spiltTypeDefine(typeDefine.trim());
            node.setId("BOOLEAN");
            node.setName(name);
            writeLine("typedef BOOLEAN_t " + name + "_t");
            constraintString += getConstraintString(typeDefine);
        }
        else if (typeDefine.contains("INTEGER")){
            spiltTypeDefine(typeDefine.trim());
            node.setId("INTEGER");
            node.setName(name);
            if (notDefinnedTypes != null && notDefinnedTypes.indexOf(name) != -1) {
                writeAddDefine("typedef long " + name + "_t;");
                try {
                    addNotDefined.close();
                    bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/asn.c",true));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for(int i=0,len = notDefinnedTypes.size();i<len;i++){
                    if(notDefinnedTypes.get(i).equals(name)){
                        notDefinnedTypes.remove(i);
                        notDefinedLines.remove(i);
                        i--;
                        len--;
                    }
                }
            }
            else
                writeLine("typedef long " + name + "_t;");
            constraintString += getConstraintString(typeDefine);
        }
        else{                                     //自定义类型约束
            spiltTypeDefine(typeDefine.trim());
            node.setId("User Defined Type");
            node.setName(name);
            if (notDefinnedTypes != null && notDefinnedTypes.indexOf(name) != -1) {
                writeAddDefine("typedef " + type.substring(0,type.indexOf("(")) + "_t " + name + "_t");
                try {
                    addNotDefined.close();
                    bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/asn.c",true));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for(int i=0,len = notDefinnedTypes.size();i<len;i++){
                    if(notDefinnedTypes.get(i).equals(name)){
                        notDefinnedTypes.remove(i);
                        notDefinedLines.remove(i);
                        i--;
                        len--;
                    }
                }
            }
            else
                writeLine("typedef " + type.substring(0,type.indexOf("(")) + "_t " + name + "_t");
            constraintString += getConstraintString(typeDefine);
        }
        addAsnList(node,name);
    }

    //返回约束函数的字符串
    private String getConstraintString(String typeDefine){
        graNode node = new graNode();
        node.setType(0);
        node.setName(name);
        node.setLine(line);
        String returnedString = "";
        returnedString += "int " + name + "_constraint(const void* sptr){\n\tlong value;\n\tif(!sptr){\n\t\tprintf(\"value not given\");\n\t\treturn -1;\n\t}\n";
        if(type.contains("INTEGER")){
            node.setId("INTEGER");
            String temp = typeDefine.substring(typeDefine.indexOf("(")+1,typeDefine.indexOf(")")).replace("\"","");
            if(temp.contains("|")){
                node.setValue(temp);
                String values[] = temp.replace("|"," ").split(" ");
                temp = getJudgeString(values);
                returnedString += "\tvalue = *(const long *)sptr;\n\t" + temp + "\n\t\treturn 0;  //constraint success\n\t}\n";
            }
            else if (temp.contains("..")){
                String values[] = temp.replace(".."," ").split(" ");
                node.setValue(temp);
                if(values[0].contains("<") && values[1].contains(">")){
                    temp = "if((value > " + values[0].replace("<","") + " && value < " + values[values.length-1].replace(">","") + ")) {";
                }
                else if(values[0].contains("<")){
                    temp = "if((value > " + values[0].replace("<","") + " && value <= " + values[values.length-1] + ")) {";
                }
                else if(values[1].contains(">")){
                    temp = "if((value >= " + values[0] + " && value < " + values[values.length-1].replace(">","") + ")) {";
                }
                else
                    temp = "if((value >= " + values[0] + " && value <= " + values[values.length-1] + ")) {";
                returnedString += "\tvalue = *(const long *)sptr;\n\t" + temp + "\n\t\treturn 0;  //constraint success\n\t}\n";
            }
            else{
                node.setValue(temp);
                returnedString += "\tvalue = *(const long *)sptr;\n\tif((value == " + temp + ")) {\n\t\treturn 0;  //constraint success\n\t}\n";
            }
            addAsnList(node,name);
            returnedString += "\telse {\n\t\tprintf(\"constraint failed\");\n\t\treturn -1;\n\t}\n}\n";
        }
        else if(type.contains("BOOLEAN")){
            node.setId("BOOLEAN");
            node.setValue("0|1");
            //String temp = typeDefine.substring(typeDefine.indexOf("(")+1,typeDefine.indexOf(")")).replace("\"","");
            returnedString += "\tvalue = *(const long *)sptr;\n\tif((value == 0 || value == 1)) {\n\t\treturn 0;  //constraint success\n\t}\n";
        }
        else if (type.contains("OF")){
            node.setId("constraint_OF");
            String temps[] = type.split("\\s+");
            boolean trag = false;
            for(int i = 0;i<asnType.length;i++){
                if (asnType[i].equals(temps[2].substring(0,temps[2].indexOf("(")))){
                    trag = true;
                    break;
                }
            }
            String value = temps[2].substring(temps[2].indexOf("(") +1,temps[2].indexOf(")"));
            if(!trag){
                returnedString += "\tsize_t size"+
                        "\t/* Determine the number of elements */\n" +
                        "\tsize = _A_CSEQUENCE_FROM_VOID(sptr)->count;\n" +
                        "\t\n" +
                        "\tif((size == " + value.substring(value.indexOf("(")+1,value.length()) + ")) {\n" +
                        "\t\t/* Perform validation of the inner elements */\n" +
                        "\t\treturn td->check_constraints(td, sptr, ctfailcb, app_key);\n" +
                        "\t} else {\n" +
                        "\t\tprintf(\"constraint failed\")\n"+
                        "\t\treturn -1;\n" +
                        "\t}\n" +
                        "}\n";
            }
            else{
                returnedString += "const " + temps[2].substring(0,temps[2].indexOf("(")-1) + "_t *st = (const " + temps[2].substring(0,temps[2].indexOf("(")-1) + "_t *)sptr;\n" +
                        "\tsize_t size;\n" +
                        "\t\n" +
                        "\tif(!sptr) {\n" +
                        "\t\t_ASN_CTFAIL(app_key, td, sptr,\n" +
                        "\t\t\t\"%s: value not given (%s:%d)\",\n" +
                        "\t\t\ttd->name, __FILE__, __LINE__);\n" +
                        "\t\treturn -1;\n" +
                        "\t}\n" +
                        "\t\n" +
                        "\tsize = st->size;\n" +
                        "\t\n" +
                        "\tif((size == "+ value.substring(value.indexOf("(")+1,value.length()) +")\n" +
                        "\t\t && !check_permitted_alphabet_2(st)) {\n" +
                        "\t\t/* Constraint check succeeded */\n" +
                        "\t\treturn 0;\n" +
                        "\t} else {\n" +
                        "\t\tprintf (\"constraint failed\");\n"+
                        "\t\treturn -1;\n" +
                        "\t}\n" +
                        "}\n";
                returnedString += "int check_permitted_alphabet_2(const void *sptr) {\n" +
                        "\tconst " + temps[2].substring(0,temps[2].indexOf("(")-1) + "_t *st = (const " + temps[2].substring(0,temps[2].indexOf("(")-1) + "_t *)sptr;\n" +
                        "\tconst uint8_t *ch = st->buf;\n" +
                        "\tconst uint8_t *end = ch + st->size;\n" +
                        "\t\n" +
                        "\tfor(; ch < end; ch++) {\n" +
                        "\t\tuint8_t cv = *ch;\n" +
                        "\t\tif(!(cv <= 127)) return -1;\n" +
                        "\t}\n" +
                        "\treturn 0;\n" +
                        "}\n";
            }
        }
        else if(type.contains("STRING") || type.contains("String")){
            node.setId(type.substring(0,type.indexOf("(")).replace(" ","_"));
            String temp = typeDefine.substring(typeDefine.indexOf("(")+1,typeDefine.indexOf(")")).replace("\"","");
            node.setValue(temp);
            addAsnList(node,name);
            if(temp.contains("FROM")){
                temp = temp.substring(temp.indexOf("(")+1,temp.length());
                if(temp.contains("|")){
                    boolean isNeed = false;
                    String judgeString = "\t\tif( ";
                    String values[] = temp.replace("|"," ").split(" ");
                    for(int i = 0;i<values.length;i++){
                        if(!values[i].contains("..")){
                            if(!isNeed){
                                judgeString += "cv != " + getASCII(values[i]);
                                isNeed = true;
                            }
                            else{
                                judgeString += " || cv != " + getASCII(values[i]);
                            }
                        }
                        else{
                            String valueTemp[] = values[i].replace(".."," ").split(" ");
                            if(!isNeed){
                                judgeString += "!( cv >= " + getASCII(valueTemp[0]) + " && cv <= " + getASCII(valueTemp[1]) + ")";
                                isNeed = true;
                            }
                            else{
                                judgeString += " || !( cv >= " + getASCII(valueTemp[0]) + " && cv <= " + getASCII(valueTemp[1]) + ")";
                            }
                        }
                    }
                    returnedString += "\tconst "+ type.substring(0,type.indexOf("(")).replace(" ","_").trim() + "_t *st = (const IA5String_t *)sptr;\n" +
                            "\tconst uint8_t *ch = st->buf;\n" +
                            "\tconst uint8_t *end = ch + st->size;\n" +
                            "\t\n" +
                            "\tfor(; ch < end; ch++) {\n" +
                            "\t\tuint8_t cv = *ch;\n" +
                            judgeString +") \n\t\t\treturn -1;\n" +
                            "\t}\n" +
                            "\treturn 0;\n}\n";
                }
                else{
                    String values[] = temp.replace(".."," ").split(" ");
                    returnedString += "\tconst "+ type.substring(0,type.indexOf("(")).replace(" ","_").trim() + "_t *st = (const IA5String_t *)sptr;\n" +
                            "\tconst uint8_t *ch = st->buf;\n" +
                            "\tconst uint8_t *end = ch + st->size;\n" +
                            "\t\n" +
                            "\tfor(; ch < end; ch++) {\n" +
                            "\t\tuint8_t cv = *ch;\n" +
                            "\t\tif(!(cv >= " + getASCII(values[0]) + " && cv <= " + getASCII(values[1]) + ")) return -1;\n" +
                            "\t}\n" +
                            "\treturn 0;\n}\n";
                }
            }
            else if (temp.contains("SIZE")){
                temp = temp.substring(temp.indexOf("(")+1,temp.length());
                String judgeString = "\tif(";
                if(isInteger(temp)){
                    judgeString += " size == " + temp + ") {\n";
                }
                else if (temp.contains("..")){
                    String values[] = temp.replace(".."," ").split(" ");
                    judgeString += "( size >= " + values[0] + " && size <= " + values[1] + ")) {\n";
                }
                else {                          //SIZE括号里为自定义的且包含限制的INTEGER类型
                    int index_temp = getCharid(temp);
                    graNode gdx = userDefineList[index_temp].head;
                    boolean isdefined = false;
                    while(gdx != null){
                        if(gdx.getName().equals(temp)){        //类型定义过
                            String value_temp = gdx.getValue();
                            isdefined = true;
                            if(value_temp.contains("|")){
                                boolean isNeed = false;
                                String values_temp[] = value_temp.replace("|"," ").split(" ");
                                for(int i = 0;i<values_temp.length;i++){
                                    if(!isNeed){
                                        judgeString += "(size == " + values_temp[i] + ")";
                                        isNeed = true;
                                    }
                                    else
                                        judgeString += " || (size == " + values_temp[i] + ")";
                                }
                                judgeString += ") {\n";
                            }
                            else{
                                judgeString += " size == " + value_temp + ") {\n";
                            }
                        }
                        gdx = gdx.next;
                    }
                    if(!isdefined){                 //类型未定义过
                        notDefinnedTypes.add(temp);
                        notDefinedLines.add(line);
                        try {
                            br.mark(8192);
                            String readString = br.readLine();
                            while(readString != null){
                                if (readString.contains(temp + " ::=")){   //找到文件下面该类型的定义
                                    String value_temp = readString.substring(readString.indexOf("(")+1,readString.indexOf(")")) ;
                                    boolean isNeed = false;
                                    if(value_temp.contains("|")){
                                        String values_temp[] = value_temp.replace("|"," ").split(" ");
                                        for(int i = 0;i<values_temp.length;i++){
                                            if(!isNeed){
                                                judgeString += "(size == " + values_temp[i] + ")";
                                                isNeed = true;
                                            }
                                            else
                                                judgeString += " || (size == " + values_temp[i] + ")";
                                        }
                                        judgeString += ") {\n";
                                    }
                                    else
                                        judgeString += " size == " + value_temp + ") {\n";
                                }
                                readString = br.readLine();
                            }
                            br.reset();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (IndexOutOfBoundsException a) {
                            System.out.println(temp + "未被限制值域");
                            a.printStackTrace();
                        }
                    }
                }
                returnedString += "\tconst "+ type.substring(0,type.indexOf("(")).replace(" ","_").trim() + "_t *st = (const " + type.substring(0,type.indexOf("(")).replace(" ","_").trim() + "_t *)sptr;\n" +
                        "\tsize_t size;\n" +
                        "\t\n" +
                        "\tif(!sptr) {\n" +
                        "\tprintf(\"value not given \")\n"+
                        "\t\treturn -1;\n" +
                        "\t}\n" +
                        "\t\n" +
                        "\tif(st->size > 0) {\n" +
                        "\t\t/* Size in bits */\n" +
                        "\t\tsize = 8 * st->size - (st->bits_unused & 0x07);\n" +
                        "\t} else {\n" +
                        "\t\tsize = 0;\n" +
                        "\t}\n" +
                        "\t\n" +
                        judgeString +
                        "\t\t/* Constraint check succeeded */\n" +
                        "\t\treturn 0;\n" +
                        "\t} else {\n" +
                        "\tprintf(\" constraint failed \")\n"+
                        "\t\treturn -1;\n" +
                        "\t}\n}\n";
            }
            else {
                returnedString += "\tconst " + type.substring(0,type.indexOf("(")).replace(" ","_").trim() + "_t *st = (const " +type.substring(0,type.indexOf("(")).replace(" ","_").trim() + "_t *)sptr;\n" +
                        "\tconst uint8_t *ch = st->buf;\n" +
                        "\tconst uint8_t *end = ch + st->size;\n" +
                        "\t\n" +
                        "\tfor(; ch < end; ch++) {\n" +
                        "\t\tuint8_t cv = *ch;\n" +
                        "\t\tif(!(cv <= 127)) return -1;\n" +
                        "\t}\n" +
                        "\treturn 0;\n}\n";
            }

        }
        else{                        //自定义类型的包含约束
            node.setId(type.substring(0,type.indexOf("(")));
            String temp = type.substring(type.indexOf("(") + 1,type.indexOf(")"));
            node.setValue(temp);
            addAsnList(node,name);
            String judegString = "\tif (";
            boolean isDefined = false;         //类型是否定义过
            int index_temp = getCharid(type.substring(0,type.indexOf("(")));
            graNode node_temp = userDefineList[index_temp].head;
            while (node_temp != null){
                if(node_temp.getName().equals(type.substring(0,type.indexOf("(")))){
                    isDefined = true;
                }
                node_temp = node_temp.next;
            }
            if(isDefined){                    //类型定义过
                if(temp.contains("|")){
                    boolean isNeed = false;
                    String values[] = temp.replace("|"," ").split(" ");
                    for(int i = 0;i<values.length;i++){
                        if (isUpLow(values[i]) == 1){
                            node_temp = userDefineList[getCharid(values[i])].head;
                            while(node_temp != null){
                                if(node_temp.getName().equals(values[i])){
                                    String value = node_temp.getValue();
                                    if(!isNeed){
                                        judegString += " value == " + value ;
                                        isNeed = true;
                                        break;
                                    }
                                    else{
                                        judegString += " || value == " + value;
                                        break;
                                    }
                            }
                                node_temp = node_temp.next;
                            }
                        }
                        else{
                            judegString += upUserDefineTypeCons(values[i]);
                        }
                    }
                    judegString += " ) {\n";
                }
                else if (temp.contains(":")){
                    temp = temp.substring(temp.indexOf(":") +1,temp.length());
                    node_temp = userDefineList[getCharid(temp)].head;
                    while (node_temp != null){
                        if(node_temp.getName().equals(temp)){
                            String value = node_temp.getValue();
                            judegString += " value == " + value + " ) {\n";
                            break;
                        }
                        node_temp = node_temp.next;
                    }
                }
                else{
                    if (isUpLow(temp) == 1){
                        node_temp = userDefineList[getCharid(temp)].head;
                        while(node_temp != null){
                            if(node_temp.getName().equals(temp)){
                                String value = node_temp.getValue();
                                judegString += " value == " + value + " ) {\n";
                            }
                            node_temp = node_temp.next;
                        }
                    }
                    else {
                        judegString += upUserDefineTypeCons(temp);
                        judegString += " ) {\n";
                    }
                }
            }
            else {
                notDefinnedTypes.add(type.substring(0,type.indexOf("(")));
                notDefinedLines.add(line);
                try {
                    br.mark(8192);
                    String readlineString = br.readLine();
                    while(readlineString != null){
                        if(readlineString.contains(type.substring(0,type.indexOf("(")) + " ::=")){
                            if (temp.contains("|")){
                                String values[] = temp.replace("|"," ").split(" ");
                                for(int i = 0;i<values.length;i++){
                                    if (isUpLow(values[i]) == 0)
                                        judegString += upUserDefineTypeCons(values[i]);
                                    else {
                                        while (!readlineString.contains(values[i])){
                                            readlineString = br.readLine();
                                        }
                                        if(readlineString.indexOf(",") == -1 || readlineString.indexOf(",") == readlineString.length()-1){
                                            judegString += " value == " + readlineString.substring(readlineString.indexOf("(") + 1,readlineString.indexOf(")")) ;
                                        }
                                        else{                //单行定义了多个变量，需要拆分开而后找到需要的值
                                            readlineString = readlineString.replace("{","");
                                            String temps[] = readlineString.replace(","," ").split(" ");
                                            for(int j = 0;j<temps.length;j++){
                                                if(temps[j].contains(values[i])){
                                                    judegString += "value == " + temps[j].substring(temps[j].indexOf("(") +1,temps[j].indexOf(")")) ;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                judegString += " ) {\n";
                            }
                            else if (temp.contains(":")){  //此种情况直接取：后面的值处理即可
                                temp = temp.substring(temp.indexOf(":") + 1,temp.length());
                                while (!readlineString.contains(temp)){
                                    readlineString = br.readLine();
                                }
                                if(readlineString.indexOf(",") == -1 || readlineString.indexOf(",") == readlineString.length()-1){
                                    judegString += " value == " + readlineString.substring(readlineString.indexOf("(") + 1,readlineString.indexOf(")")) + " ) {\n";
                                }
                                else{                //单行定义了多个变量，需要拆分开而后找到需要的值
                                    String temps[] = readlineString.replace("{","").replace(","," ").split(" ");
                                    for(int i = 0;i<temps.length;i++){
                                        if(temps[i].contains(temp)){
                                            judegString += "value == " + temps[i].substring(temps[i].indexOf("(") +1,temps[i].indexOf(")")) + " ) {\n";
                                            break;
                                        }
                                    }
                                }
                            }
                            else {           //单值情况，需要对大小写进行判断
                                if (isUpLow(temp) == 0){
                                    judegString += upUserDefineTypeCons(temp);
                                    judegString += " ) {\n";
                                }
                                else {
                                    while (!readlineString.contains(temp)){
                                        readlineString = br.readLine();
                                    }
                                    if(readlineString.indexOf(",") == -1 || readlineString.indexOf(",") == readlineString.length()-1){
                                        judegString += " value == " + readlineString.substring(readlineString.indexOf("(") + 1,readlineString.indexOf(")")) + " ) {\n";
                                    }
                                    else{                //单行定义了多个变量，需要拆分开而后找到需要的值
                                        String temps[] = readlineString.replace(","," ").split(" ");
                                        for(int i = 0;i<temps.length;i++){
                                            if(temps[i].contains(temp)){
                                                judegString += "value == " + temps[i].substring(temps[i].indexOf("(") +1,temps[i].indexOf(")")) + " ) {\n";
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        readlineString = br.readLine();
                    }
                    br.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IndexOutOfBoundsException a){
                    a.printStackTrace();
                    System.out.println("类型包含约束失败，其中有未限制的类型");
                }
            }
            returnedString += "\tconst Day_t *st = (const Day_t *)sptr;\n" +
                    "\tlong value;\n" +
                    "\t\n" +
                    "\tif(!sptr) {\n" +
                    "\t\t_ASN_CTFAIL(app_key, td, sptr,\n" +
                    "\t\t\t\"%s: value not given (%s:%d)\",\n" +
                    "\t\t\ttd->name, __FILE__, __LINE__);\n" +
                    "\t\treturn -1;\n" +
                    "\t}" +
                    judegString +
                    "\t\t/* Constraint check succeeded */\n" +
                    "\t\treturn 0;\n" +
                    "\t} else {\n" +
                    "\t\tprintf (\"constraint failed\");\n" +
                    "\t\treturn -1;\n" +
                    "\t}\n" +
                    "}\n";

        }
        return returnedString;
    }

    //处理类型包含约束
    private  String upUserDefineTypeCons (String value){
        String resultString = "";
        boolean isDefined = false;
        String price = "";
        graNode gdx = userDefineList[getCharid(value)].head;
        while(gdx != null){
            if(gdx.getName().equals(value)){
                isDefined = true;
                price = gdx.getValue();
                break;
            }
            gdx = gdx.next;
        }
        if(isDefined){
            if(price.contains("|")){
                boolean isNeed = false;
                String values[] = price.replace("|"," ").split(" ");
                for(int i = 0;i<values.length;i++){
                    gdx = userDefineList[getCharid(values[i])].head;
                    if (isUpLow(values[i]) == 0){
                        resultString += upUserDefineTypeCons(values[i]);
                    }
                    else {
                        while(gdx != null){
                            if(gdx.getName().equals(values[i])){
                                price = gdx.getValue();
                                if(!isNeed){
                                   resultString += " value == " + price;
                                   isNeed = true;
                                   break;
                                }
                                else{
                                    resultString += " || value == " + price;
                                    break;
                                }
                            }
                            gdx = gdx.next;
                        }
                    }
                }
            }
            else if (price.contains(":")){
                price = price.substring(price.indexOf(":") + 1,price.length());
                gdx = userDefineList[getCharid(price)].head;
                while(gdx != null){
                    if (gdx.getName().equals(price)){
                        price = gdx.getValue();
                        resultString += " value == " + price ;
                        break;
                    }
                    gdx = gdx.next;
                }
            }
            else {
                if (isUpLow(price) == 0)
                    resultString += upUserDefineTypeCons(price);
                else {
                    gdx = userDefineList[getCharid(price)].head;
                    while(gdx != null){
                        if (gdx.getName().equals(price)){
                            price = gdx.getValue();
                            resultString += " value == " + price ;
                            break;
                        }
                        gdx = gdx.next;
                    }
                }
            }
        }
        else{
            notDefinnedTypes.add(value);
            notDefinedLines.add(line);
            try {
                br.mark(8192);
                String readlineString = br.readLine();
                while(readlineString != null){
                    if(readlineString.contains(value + " ::=")){
                        price = readlineString.substring(readlineString.indexOf("(") + 1,readlineString.indexOf(")"));
                        if(price.contains("|")){
                            String values[] = price.replace("|"," ").split(" ");
                            for (int i = 0;i<values.length;i++){
                                if(isUpLow(values[i]) == 0)
                                    resultString += upUserDefineTypeCons(values[i]);
                                else {
                                    while(!readlineString.contains(values[i])){
                                        readlineString = br.readLine();
                                    }
                                    if (readlineString.indexOf(",")==-1 || readlineString.indexOf(",") == readlineString.length()-1){
                                        resultString += " value == " + readlineString.substring(readlineString.indexOf("(") +1,readlineString.indexOf(")"));
                                    }
                                    else {
                                        String values1[] = readlineString.replace(","," ").split(" ");
                                        for(int j = 0; j<values1.length;j++){
                                            if(values1[j].contains(values[i])){
                                                resultString += " value == " + values1[j].substring(values1[j].indexOf("(") + 1,values1[j].indexOf(")"));
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else if (price.contains(":")){
                            price = price.substring(price.indexOf(":"),price.length());
                            while(!readlineString.contains(price)){
                                readlineString = br.readLine();
                            }
                            if (!readlineString.contains(",") || readlineString.indexOf(",") == readlineString.length()-1){
                                resultString += " value == " + readlineString.substring(readlineString.indexOf("(") +1,readlineString.indexOf(")"));
                            }
                            else {
                                String values[] = readlineString.replace(","," ").split(" ");
                                for(int j = 0; j<values.length;j++){
                                    if(values[j].contains(price)){
                                        resultString += " value == " + values[j].substring(values[j].indexOf("(") + 1,values[j].indexOf(")"));
                                        break;
                                    }
                                }
                            }
                        }
                        else {
                            if(isUpLow(price) == 0)
                                resultString += upUserDefineTypeCons(price);
                            else {
                                while(!readlineString.contains(price)){
                                    readlineString = br.readLine();
                                }
                                if (!readlineString.contains(",") || readlineString.indexOf(",") == readlineString.length()-1){
                                    resultString += " value == " + readlineString.substring(readlineString.indexOf("(") +1,readlineString.indexOf(")"));
                                }
                                else {
                                    String values[] = readlineString.replace(","," ").split(" ");
                                    for(int j = 0; j<values.length;j++){
                                        if(values[j].contains(price)){
                                            resultString += " value == " + values[j].substring(values[j].indexOf("(") + 1,values[j].indexOf(")"));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    readlineString = br.readLine();
                }
                br.reset();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IndexOutOfBoundsException a){
                a.printStackTrace();
                System.out.println("类型包含约束失败，存在未限制的类型");
            }
        }
        return resultString;
    }

    //返回由于多个值需要排序的判断条件的字符串
    private String getJudgeString(String[] values){
        String judgeString = "if (";
        ArrayList<Integer> preorder = new ArrayList();
        int[] behOrder;
        behOrder = new int[values.length];
        for(int i = 0;i<values.length;i++ ){
            preorder.add(Integer.parseInt(values[i]));
        }
        for(int i = 0;i<values.length;i++){
            int temp = preorder.get(0);
            for(int j=0;j<preorder.size();j++){
                if(temp>preorder.get(j)){
                    temp = preorder.get(j);
                }
            }
            behOrder[i] = temp;
            preorder.remove(preorder.indexOf(temp));
        }
        int min,max = 0,judge;
        min = judge = behOrder[0] ;
        boolean isNeed = false;   //判断是否需要添加||
        int k = 0;                //判断循环退出，即是否遍历一遍了数组
        while(k<behOrder.length-1){
            if(behOrder[++k]-judge != 1){
                max = behOrder[k-1];
                if(min == max){                  //min==max 即出现了单值，反之则是一个范围
                    if(!isNeed){
                        judgeString += " value == " + max ;
                        isNeed = true;
                    }
                    else
                        judgeString += " || value == " + max ;
                }
                else{
                    if(!isNeed){
                        judgeString += "( value >= " + min + " && value <= " + max + ")";
                        isNeed = true;
                    }
                    else
                        judgeString += " || ( value >= " + min + " && value <= " + max + ")";
                }
                judge = min = behOrder[k];
            }
            else{
                judge = behOrder[k];
            }
        }
        max = behOrder[behOrder.length-1];
        if(min == max)
            judgeString += " || value == " + max + ") {";
        else{
            if(isNeed)
                judgeString += " || ( value >= " + min + " && value <= " + max + ")) {";
            else
                judgeString += " ( value >= " + min + " && value <= " + max + ")) {";
        }

        return judgeString;
    }

    //返回String类型约束判断条件的ASCII码
    private String getASCII(String value){        //返回字符的ASCII码
        return Integer.toString(Integer.valueOf(value.charAt(0)));
    }

    //以::=或空格分隔文件读入的类型定义的字符串
    private void spiltTypeDefine(String typeDefine){

        if(typeDefine.contains("::=")){
            String spiltStr[] = typeDefine.split("::=");
            type = spiltStr[1].replace("{","").replace("}","").trim().replace(",","");
            if(type.contains("[")){
                type = type.substring(type.indexOf("]")+1,type.length());
            }
            name = spiltStr[0].trim();
        }
        else{
            String spiltStr[] = typeDefine.split("\\s+");
            if(spiltStr.length !=1){
                type = spiltStr[1].replace(",","");
                if(type.contains("["))
                    type = type.substring(type.indexOf("]")+1,type.length());
                name = spiltStr[0].replace("{","");
            }
        }
    }

    //以::=分隔文件读入的值定义的字符串
    private void spiltValueDefine (String valueDefine){
        String spiltStr[] = valueDefine.split("::=");
        String nametype [] = spiltStr[0].split("\\s+");
        name = nametype[0].trim();
        if(!valueDefine.contains("OCTET") && !valueDefine.contains("BIT"))
            type = nametype[1].trim();
        else {
            type = nametype[1].trim() + "_" + nametype[2].trim();
        }
        if (!spiltStr[1].contains("{"))
            value = spiltStr[1].trim().replace(",","");
        else
            value = spiltStr[1].trim();
    }

    //向.c文件中写入str
    private void writeLine(String str){
        try {
            bw.write(str);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(str);
    }

    //向.h文件中写入str
    private void writeHline(String str){
        try {
            tw.write(str);
            tw.newLine();
            tw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(str);
    }

    private void writeCodeLine (String str) {
        try {
            cw.write(str);
            cw.newLine();
            //cw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //添加语法项节点至链表中
    private void addAsnList(graNode node , String type){
        int charId;
        if(asnAnalyzeList.head == null){
            asnAnalyzeList.head = node;
            asnAnalyzeList.tail = node;
        }
        else{
            asnAnalyzeList.tail.next = node;
            node.pre = asnAnalyzeList.tail;
            asnAnalyzeList.tail = node;
        }
        charId = getCharid(type);
        if(userDefineList[charId].head == null){
            userDefineList[charId].head = node;
            userDefineList[charId].tail = node;
        }
        else{
            userDefineList[charId].tail.next = node;
            node.pre = userDefineList[charId].tail;
            userDefineList[charId].tail = node;
        }
    }

    //判断字符串首字母大小写
    private static int isUpLow(String str){  //判断首字母大小写
        if(Character.isUpperCase(str.charAt(0)))
            return 0;
        else
            return 1;
    }

    //判断ASN基本类型头文件中是否定义过
    private static boolean isHeadDefined(String str){
        boolean trag = false;
        try {
            BufferedReader bf = new BufferedReader(new FileReader("D:\\ideaProjects\\ASN\\asn.h"));
            for(String line = bf.readLine();line != null;line = bf.readLine()){
                if(line.contains(str))
                    trag = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trag;
    }

    //判断字符串首字符是否为字母
    private static boolean isChar (String firstChar){
        for(int i = 0;i<letters.length;i++){
            if(firstChar.equals(letters[i]))
                return true;
        }
        return false;
    }

    //判断字符串是否为字母或者"{""["
    private static boolean isChar2 (String firstChar){
        for(int i = 0;i<letters2.length;i++){
            if(firstChar.equals(letters2[i]))
                return true;
        }
        return false;
    }

    //判断字符串是否含有特殊字符
    private static boolean isSpecialChar(String str) {
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }

    //判断字符串是否含有除括号以外的特殊字符
    private static boolean isSpecialCharWithout(String str){
        String regEx = " _`~!@#$%^&*()+=|':;',\\[\\].<>/?~！@#￥%……&*（）——+|【】‘；：”“’。，、？|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }

    //返回标识符首字母下标
    private static int getCharid (String firstString){

        int i = 0;
        try {
            String firstChar = firstString.substring(0,1);

            for(; i<letters.length;i++){
                if(firstChar.equals(letters[i]))
                    break;
            }
        } catch (Exception e) {
            System.out.println("字符串不能为空");
            e.printStackTrace();
        }

        return i;
    }

    //判断::=前字符串是否存在不符合ASN语法的情况，1表示首字母非字母，2表示含有特殊字符
    //3表示含有两个连续的连字符，4表示以连字符结尾，666表示无错误
    private static int isPreDefineError(String st){
        if(!asnAnalyze.isChar(st.substring(0,1))){
            System.out.println("第" + line + "行存在首字母非字母");
            return 1;
        }
        else if(asnAnalyze.isSpecialChar(st)){
            System.out.println("第" + line + "行含有特殊字符");
            return 2;
        }
        else if(st.contains("--")){
            System.out.println("第" + line + "行含有两个连续的连字符");
            return 3;
        }
        else if((st.substring(st.length()-1)).equals("-")){
            System.out.println("第" + line + "行存在以连字符结尾");
            return 4;
        }
        else
            return 666;
    }

    //判断::=后字符串是否存在不符合ASN语法的情况，1表示首字母非字母，2表示含有除中括号和花括号及逗号特殊字符
    //3表示含有两个连续的连字符，4表示以连字符结尾，666表示无错误
    private int isBehDefineError(String temp){
        if(!asnAnalyze.isChar2(temp.substring(0,1)) && isCycle == false){
            System.out.println("第" + line + "行存在首字母非字母");
            return 1;
        }
        else if(asnAnalyze.isSpecialCharWithout(temp)){
            System.out.println("第" + line + "行含有特殊字符");
            return 2;
        }
        else if(temp.contains("--")){
            System.out.println("第" + line + "行含有两个连续的连字符");
            return 5;
        }
        else if((temp.substring(temp.length()-1)).equals("-")){
            System.out.println("第" + line + "行存在以连字符结尾");
            return 4;
        }
        else
            return 666;
    }

    //判断括号是否正确处理
    private boolean isBracketError (@NotNull String[] strs, String str){
        int isHuaBracketError ,isZhoBracketError;
        boolean trag = true;
        for(int i = 0;i<strs.length;i++){           //循环检测{和[的使用是否符合语法规则
            isHuaBracketError = strs[i].indexOf("{");
            isZhoBracketError = strs[i].indexOf("[");
            if(isHuaBracketError!=-1&&isHuaBracketError!=0&&isHuaBracketError!=strs[i].length()-1 && isZhoBracketError != 0){
                System.out.println("第"+ line + "行“{”的使用不符合ASN语法规则");
                trag = false;
                break;
            }
            else if(!str.contains("{") && str.contains("}") && isCycle == false){
                System.out.println("第"+ line + "行“}”的使用不符合ASN语法规则");
                trag = false;
                break;
            }
            else if(isHuaBracketError == 0){
                if(str.substring(str.length()-1).equals(",") && !isCycle()) {
                    if(!str.substring(str.length()-2,str.length()-1).equals("}")){
                        System.out.println("第"+ line + "行“{”的使用不符合ASN语法规则");
                        trag = false;
                        break;
                    }
                }
            }
            else if(!str.contains("[") && str.contains("]")){
                System.out.println("第"+ line + "行“[”的使用不符合ASN语法规则");
                trag = false;
                break;
            }
            else if(isZhoBracketError != -1 && isZhoBracketError != 0){
                System.out.println("第"+ line + "行“[”的使用不符合ASN语法规则");
                trag = false;
                break;
            }
            else if(isZhoBracketError == 0 ){
                if(!str.contains("]")){
                    System.out.println("第"+ line + "行“[”的使用不符合ASN语法规则");
                    trag =false;
                    break;
                }
                else if(isHuaBracketError != -1 && isHuaBracketError<strs[i].indexOf("]")){
                    System.out.println("第"+ line + "行“{”的使用不符合ASN语法规则");
                    trag =false;
                    break;

                }
            }
        }
        return trag;
    }

    //判断是否含有约束
    private boolean isConstraint(String type) {
        boolean b = false;
        graNode temp = userDefineList[getCharid(type)].head;
        while (temp != null) {
            if (temp.getConstraint()) {
                b = true;
                return b;
            }
            temp = temp.next;
        }
        return b;
    }

    //判断是否被定义过,定义过返回true
    private boolean isDefined(String type){
        boolean trag = false;
        for(int i =0;i<asnType.length;i++){
            if(type.equals(asnType[i]))
                trag =true;
        }
        if(type.contains("String") || type.contains("Time"))
            trag = true;
        else {
            int index = getCharid(type);
            graNode temp = userDefineList[index].head;
            while(temp!=null){
                if(temp.getName().equals(type)){
                    trag = true;
                    break;
                }
                else
                    temp = temp.next;
            }
        }

        return trag;
    }

    //把整个关于语法鉴别的函数独立了出来，返回true表示无错误，什么鬼逻辑，地方太多，懒得改了。。
    private boolean isDefineError(String str){
        String[] strs,strs1;
        boolean trag = true;
        if(str.contains("::=")){
            String splitEqual[] = str.split("::="); //含有::=的进行分割处理
            String temp1 = splitEqual[0].trim();
            String temp2 = splitEqual[1].trim();          //去除分割后多余的空格
            strs = temp2.split("\\s+");
            strs1 = temp1.split("\\s+");
            int isDefineError = 666;

            for(int i =0;i<strs1.length;i++){
                isDefineError = isPreDefineError(strs1[i]);
            }
            if(isDefineError == 666 && isUpLow(strs1[0]) == 0){
                for(int i=0;i<strs.length;i++){
                    isDefineError = isBehDefineError(strs[i]);
                }
                if(isDefineError == 666){
                    trag = isBracketError(strs,str);
                }
                else
                    trag = false;
            }
            else if (isDefineError != 666)
                trag = false;

        }
        else{
            strs = str.split("\\s+");
            int isDefinesError = 666;
            for(int i=0;i<strs.length;i++){
                isDefinesError = isBehDefineError(strs[i]);
            }
            if(isDefinesError == 666){
                boolean bracketsError = isBracketError(strs,str);
                if(!bracketsError)
                    trag = false;
            }
            else
                trag = false;
        }
        return trag;
    }

    //先使用后定义 应该向文件写入的内容
    private String writeLineStr(){
        String writeLine = "";
        try {
            String str = br.readLine();
            while(!str.contains("}")){
                if(str.trim().equals("{")){
                    str = br.readLine();
                    line++;
                }
                String temp = str.replace("}","").trim();
                if(isDefineError(temp)){
                    spiltTypeDefine(temp);
                    graNode node = new graNode();
                    node.setId(type);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    if(!isDefined(type)){
                        notDefinnedTypes.add(type);
                        notDefinedLines.add(line);
                    }
                    writeLine +="\t" + type + "_t " + name + ";\n";
                    str = br.readLine();
                }
            }
            if(str.trim().indexOf("}") != 0){
                String temp = str.replace("}","").trim();
                if(isDefineError(temp)){
                    spiltTypeDefine(temp);
                    if(!isDefined(type)){
                        notDefinnedTypes.add(type);
                        notDefinedLines.add(line);
                    }
                    graNode node = new graNode();
                    node.setId(type);
                    node.setName(name);
                    node.setLine(line);
                    node.setType(0);
                    addAsnList(node,name);
                    writeLine +="\t" + type + "_t " + name + "_t;\n";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return writeLine;
    }

    //删除已处理过的使用未定义
    private void delete (){
        for(int i=0,len = notDefinnedTypes.size();i<len;i++)
            if(notDefinnedTypes.get(i).equals(structName.get(structName.size()-1))){
                notDefinnedTypes.remove(i);
                notDefinedLines.remove(i);
                i--;
                len--;
            }
        structType.remove(structType.size()-1);
        structName.remove(structName.size()-1);
    }

    //删除已处理过的未使用结构体的使用未定义
    private void deleteWithoutStruct(String nameType){
        for(int i=0,len = notDefinnedTypes.size();i<len;i++)
            if(notDefinnedTypes.get(i).equals(nameType)){
                notDefinnedTypes.remove(i);
                notDefinedLines.remove(i);
                i--;
                len--;
            }
    }

    //判断字符串是否为整数
    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    public static void main(String[] args) {
//        System.out.println("sadfs::=df".contains("::="));
//        System.out.println(asnAnalyze.isSpecialCharWithout("sadfasdf[{"));
//        System.out.println(asnAnalyze.getCharid("abcd"));
//        String[] tt = "employee Employee  ::=  String,".split("::=");
//        String[] s1 = tt[0].split("\\s+");
//        String t2 = tt[1].trim();
//        String[] s2 = t2.split("\\s+");
//        for(int i=0;i<tt.length;i++){
//            System.out.println(tt[i]);
//        }
//        for(int i=0;i<s1.length;i++){
//            System.out.println(s1[i]);
//        }
//        for(int i=0;i<s2.length;i++){
//            System.out.println(s2.length);
//            System.out.println(s2[i]);
//        }
        //System.out.println("asdf".substring("asdf".length()-1));
        asnAnalyze a = new asnAnalyze();
    }
}
