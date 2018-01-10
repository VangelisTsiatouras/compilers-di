import syntaxtree.*;
import visitor.GJDepthFirst;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("Duplicates") // Remove IntelliJ warning about duplicate code

public class LLVMGenerateVisitor extends GJDepthFirst<String, String> {
    public VTables vTables;
    public SymbolTable symbolTable;
    public String fileName;
    public File fileptr;
    public int register;
    public int loopLabel;
    public int ifLabel;
    public String currentClass;
    public String currentMethod;

    LLVMGenerateVisitor(String fileName, VTables vTables, SymbolTable symbolTable) {
        this.vTables = vTables;
        this.symbolTable = symbolTable;
        this.fileName = fileName;
        // Create "out" directory to store generated LLVM code
        File dir = new File("LLVM");
        // If the directory does not exist, create it
        if (!dir.exists()) {
            dir.mkdir();
        }
        try {
            // Create file to store the V-Table
            this.fileptr = new File("LLVM/" + fileName + ".ll");
            if (!this.fileptr.exists()) {
                this.fileptr.createNewFile();
            }
            // If file with same name already exists delete its contents
            else {
                PrintWriter writer = new PrintWriter(this.fileptr);
                writer.print("");
                writer.close();
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    void emit(String buffer) {
        try {
            FileWriter fw = new FileWriter(this.fileptr, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            pw.print(buffer);
            pw.close();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    String get_register() {
        int retVal = this.register;
        this.register++;
        return "%_" + retVal;
    }

    String get_loop_label() {
        int retVal = this.loopLabel;
        this.loopLabel++;
        return "loop" + retVal;
    }

    String get_if_label() {
        int retVal = this.ifLabel;
        this.ifLabel++;
        return "if" + retVal;
    }

    void llvm_create_v_tables() {
        for (Map.Entry entry : this.vTables.classesTables.entrySet()) {
            Object key = entry.getKey();
            String className = entry.getKey().toString();
            VTables.ClassVTable classVTable = this.vTables.classesTables.get(key);
            if (classVTable.isMainClass) {
                emit("@." + className + "_vtable = global [0 x i8*] []\n");
                continue;
            }
            int numberOfFuncs = classVTable.methodsTable.size();
//            System.err.println("num of funcs" + numberOfFuncs);
            String buffer = "@." + className + "_vtable = global [" + numberOfFuncs + " x i8*] [";
            // Retrieve data from symbol table
            SymbolTable.ClassSymTable classSymTable = this.symbolTable.classes.get(className);
//            for (Map.Entry classVTableEntryFields : classVTable.fieldsTable.entrySet()) {
//                String fieldName = classVTableEntryFields.getKey().toString();
//                Integer offset = Integer.parseInt(classVTableEntryFields.getValue().toString());
//            }
            boolean printComa = false;
            for (Map.Entry classVTableEntryMethods : classVTable.methodsTable.entrySet()) {
                String methodName = classVTableEntryMethods.getKey().toString();
                Integer offset = Integer.parseInt(classVTableEntryMethods.getValue().toString());
                SymbolTable.MethodSymTable methodSymTable = classSymTable.methods.get(methodName);
                String methodRetType = methodSymTable.returnType;
                if (printComa) {
                    buffer += ", ";
                }
                // Return type
                if (methodRetType.equals("int")) {
                    buffer += "i8* bitcast (i32 (i8*";
                } else if (methodRetType.equals("boolean")) {
                    buffer += "i8* bitcast (i1 (i8*";
                } else {
                    buffer += "i8* bitcast (i8* (i8*";
                }
                // Set up parameters
                for (Map.Entry methodParams : methodSymTable.parameters.entrySet()) {
                    String paramType = methodParams.getValue().toString();
                    if (paramType.equals("int")) {
                        buffer += ",i32";
                    } else if (paramType.equals("boolean")) {
                        buffer += ",i1";
                    } else {
                        buffer += ",i8*";
                    }
                }
                buffer += ")* @" + className + "." + methodName + " to i8*)";

                printComa = true;
                emit(buffer);
                buffer = "";
            }
            emit("]\n");
        }
    }

    void llvm_helper_methods() {
        String buffer = "\n" +
                "declare i8* @calloc(i32, i32)\n" +
                "declare i32 @printf(i8*, ...)\n" +
                "declare void @exit(i32)\n" +
                "\n" +
                "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                "define void @print_int(i32 %i) {\n" +
                "    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "    ret void\n" +
                "}\n" +
                "\n" +
                "define void @throw_oob() {\n" +
                "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str)\n" +
                "    call void @exit(i32 1)\n" +
                "    ret void\n" +
                "}\n";
        emit(buffer);
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, String str) throws Exception {
        String _ret = null;
        llvm_create_v_tables();
        llvm_helper_methods();
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        n.f2.accept(this, null);
        return _ret;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public String visit(MainClass n) throws Exception {
        emit("\ndefine i32 @main() {\n");
        emit("\n");
        emit("}\n");
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, String str) throws Exception {
        this.currentClass = n.f1.accept(this, null);
        n.f3.accept(this, null);
        n.f4.accept(this, null);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, String str) throws Exception {
        String type = n.f0.accept(this, null);
        String identifier = n.f1.accept(this, null);
        String buffer = "\t%"+identifier + " = alloca ";
        if (type.equals("int")) {
            buffer += "i32\n";
        } else if (type.equals("boolean")) {
            buffer += "i1\n";
        } else {
            buffer += "i8*\n";
        }
        emit(buffer);
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, String str) throws Exception {
        String buffer = "\ndefine ";

        String methodType = n.f1.accept(this, null);
        String llvmMethType;
        if (methodType.equals("int")) {
            buffer += "i32";
            llvmMethType = "i32";
        } else if (methodType.equals("boolean")) {
            buffer += "i1";
            llvmMethType = "i1";
        } else {
            buffer += "i8*";
            llvmMethType = "i8*";
        }

        this.currentMethod = n.f2.accept(this, null);
        buffer += " @" + this.currentClass + "." + this.currentMethod + "(i8* %this";

        SymbolTable.ClassSymTable classSymTable = this.symbolTable.classes.get(this.currentClass);
        SymbolTable.MethodSymTable methodSymTable = classSymTable.methods.get(this.currentMethod);
        // Set up parameters
        for (Map.Entry methodParams : methodSymTable.parameters.entrySet()) {
            String paramType = methodParams.getValue().toString();
            String paramName = methodParams.getKey().toString();
            if (paramType.equals("int")) {
                buffer += ", i32";
            } else if (paramType.equals("boolean")) {
                buffer += ", i1";
            } else {
                buffer += ", i8*";
            }
            buffer += " %." + paramName;
        }
        buffer += ") {\n";
        emit(buffer);
        // Allocate parameters

        for (Map.Entry methodParams : methodSymTable.parameters.entrySet()) {
            String paramType = methodParams.getValue().toString();
            String paramName = methodParams.getKey().toString();
            buffer = "\t%" + paramName + " = alloca ";
            if (paramType.equals("int")) {
                buffer += "i32\n";
                buffer += "\tstore i32 %." + paramName + ", i32* %" + paramName;
            } else if (paramType.equals("boolean")) {
                buffer += "i1\n";
                buffer += "\tstore i1 %." + paramName + ", i1* %" + paramName;
            } else {
                buffer += "i8*\n";
                buffer += "\tstore i8* %." + paramName + ", i8** %" + paramName;
            }
            buffer += "\n";
            emit(buffer);
        }

        n.f7.accept(this, null);

        // Return
        String retExpr = n.f10.accept(this, null);
        String retRegister = get_register();
        buffer = "\t" + retRegister + " = " + retExpr + '\n';
        buffer += "\tret " + llvmMethType + " " + retRegister + "\n}\n";
        emit(buffer);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public String visit(IfStatement n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, String str) throws Exception {

        return "boolean";
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, String str) throws Exception {

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, String str) throws Exception {

        return "boolean";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, String str) throws Exception {

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, String str) throws Exception {

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, String str) throws Exception {

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, String str) throws Exception {

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, String str) throws Exception {

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     */
    public String visit(PrimaryExpression n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n, String str) throws Exception {

        return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, String str) throws Exception {

        return null;
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n, String str) throws Exception {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, String str) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, String str) throws Exception {
        return "int";
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, String str) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, String str) throws Exception {
        return "true";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, String str) throws Exception {
        return "false";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, String str) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, String str) throws Exception {
        return "this";
    }
}
