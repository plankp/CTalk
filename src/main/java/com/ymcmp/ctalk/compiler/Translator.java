/**
 * MIT License
 *
 * Copyright (c) 2017 Paul T.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ymcmp.ctalk.compiler;

import com.ymcmp.ctalk.compiler.NsInfo.Visibility;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.TokenStream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 *
 * @author YTENG
 */
public class Translator extends GrammarBaseVisitor<String> {

    private enum MangleScheme {
        INTERNAL, HIERACHY
    }

    private enum ProcState {
        GEN_SYM, GEN_CODE
    }

    private final Map<String, NsInfo> nsInfo = new HashMap<>();
    private final Set<String> importSet = new HashSet<>();
    private final Deque<GrammarParser.NamespaceContext> currentNs = new ArrayDeque<>();
    private final Deque<URI> currentFile = new ArrayDeque<>();
    private final Deque<Deque<LocalVar>> locals = new ArrayDeque<>();
    private final StringBuilder textBuf = new StringBuilder();
    private final StringBuilder pasteInclude = new StringBuilder();
    private final StringBuilder pasteTypedef = new StringBuilder();
    private final StringBuilder pasteMacro = new StringBuilder();
    private final StringBuilder head = new StringBuilder();
    private final StringBuilder tail = new StringBuilder();

    private Visibility visibility = Visibility.HIDDEN;
    private MangleScheme mangleScheme = MangleScheme.INTERNAL;
    private ProcState procState = ProcState.GEN_SYM;
    private String paramSeparator = ",";
    private LocalVar currentVar = null;

    public Translator(final URI uri) {
        currentFile.add(uri);
    }

    public String generate(GrammarParser.ProgramContext ctx, final String entryFuncId) {
        // This has to be processed before head and tail
        procState = ProcState.GEN_SYM;
        visitProgram(ctx); // ignore output
        importSet.clear();

        procState = ProcState.GEN_CODE;
        final String body = visitProgram(ctx);

        // demo::main:argc:argv => demo main:argc:argv
        final String[] nsPart = entryFuncId.split("::");
        final StringBuilder ent = new StringBuilder();
        ent.append("_C");
        for (int i = 0; i < nsPart.length - 1; ++i) {
            final String fragment = nsPart[i];
            ent.append(fragment.length()).append(fragment);
        }
        final String[] nameSel = nsPart[nsPart.length - 1].split(":");
        ent.append(nameSel[0].length()).append(nameSel[0]);
        for (int i = 1; i < nameSel.length; ++i) {
            final String fragment = nameSel[i];
            ent.append('_').append(fragment.length()).append(fragment);
        }

        final String entry = "int main (int argc, char **argv) { return " + ent.toString() + "(argc, argv); }";
        return Arrays.stream(("#include <stdbool.h>\n#include <stddef.h>\n"
                + pasteInclude.toString() + "/* END OF INCLUDES */\n"
                + pasteTypedef.toString() + "/* END OF TYPEDEFS */\n"
                + pasteMacro.toString() + "/* END OF MACROS */\n"
                + head.toString() + "/* END OF PROTOTYPES */\n"
                + body + "\n" + tail.toString() + "\n" + entry).split("\n"))
                .filter(e -> !e.trim().isEmpty())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String visitProgram(GrammarParser.ProgramContext ctx) {
        // This has to be processed before head and tail
        return ctx.p.stream().map(this::visit).collect(Collectors.joining("\n"));
    }

    @Override
    public String visitProgramLevel(GrammarParser.ProgramLevelContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public String visitTopLevel(GrammarParser.TopLevelContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public String visitNamespace(GrammarParser.NamespaceContext ctx) {
        if (ctx == null) {
            return "_C";
        }
        switch (mangleScheme) {
        case HIERACHY:
            return ctx.getText().replaceAll("::", "/");
        case INTERNAL: {
            String[] sec = ctx.getText().split("::");
            if (sec[0].equals("_")) {
                sec = (currentNs.peek().getText() + ctx.getText().substring(1)).split("::");
            }
            final StringBuilder sb = new StringBuilder("_C");
            for (final String part : sec) {
                sb.append(part.length()).append(part);
            }
            return sb.toString();
        }
        default:
            throw new RuntimeException("Unhandled namespace mangle scheme of " + mangleScheme);
        }
    }

    @Override
    public String visitDefFunction(GrammarParser.DefFunctionContext ctx) {
        final String retType = ctx.r == null ? "void %s" : visit(ctx.r);
        final String tmp = textBuf.toString();
        textBuf.setLength(0);
        locals.add(new ArrayDeque<>());
        paramSeparator = ",";
        final String params = visit(ctx.p);
        // Parameters *MUST* be processed before name
        mangleScheme = MangleScheme.INTERNAL;
        final String rawName = ctx.n.getText();
        final String name = visitNamespace(currentNs.peek()) + rawName.length() + rawName + textBuf.toString();
        mangleScheme = MangleScheme.HIERACHY;
        {
            final String prior = currentNs.peek() == null ? "" : (currentNs.peek().getText() + "::");
            nsInfo.put(name, new NsInfo(visibility, prior + rawName.length() + rawName, visitNamespace(currentNs.peek())));
        }
        textBuf.setLength(0);
        textBuf.append(tmp);
        final String proto = String.format(retType, name + " " + params);
        String ret = "";
        switch (procState) {
        case GEN_SYM:
            switch (visibility) {
            case EXPORT:
            case INTERNAL:
                head.append("extern");
                break;
            case HIDDEN:
                head.append("static");
                break;
            default:
                throw new AssertionError("Visibility of " + visibility + " not handled!");
            }
            head.append(' ').append(proto).append(";\n");
            break;
        case GEN_CODE:
            final String body = ctx.s.stream().map(this::visit).collect(Collectors.joining("\n"));
            ret = proto + "\n{\n" + body + "\n}";
            break;
        default:
            throw new RuntimeException("Unhandled process state of " + procState);
        }
        locals.removeLast();
        return ret;
    }

    @Override
    public String visitDefParams(GrammarParser.DefParamsContext ctx) {
        if (ctx.getChild(0).getText().equals("(")) {
            textBuf.append(VOID_FUNC_ID);
            return "()";
        }
        final StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < ctx.getChildCount(); i += 2) {
            sb.append(visit(ctx.getChild(i))).append(paramSeparator);
        }
        if (ctx.v == null) {
            sb.deleteCharAt(sb.length() - 1);
        } else {
            sb.append("...");
        }
        return sb.append(')').toString();
    }

    @Override
    public String visitDefParam(GrammarParser.DefParamContext ctx) {
        final String ts = visit(ctx.getChild(ctx.getChildCount() - 1));
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount() - 2; i += 2) {
            final String pname = ctx.getChild(i).getText();
            final String iname = "_C" + pname.length() + pname;
            locals.peek().add(new LocalVar(iname, String.format(ts, "")));
            textBuf.append('_').append(pname.length()).append(pname);
            sb.append(String.format(ts, iname)).append(paramSeparator);
        }
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    @Override
    public String visitDefModuleVar(GrammarParser.DefModuleVarContext ctx) {
        final String ts = visit(ctx.getChild(ctx.getChildCount() - 1));
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount() - 2; i += 2) {
            textBuf.setLength(0);
            final String pname = visit(ctx.getChild(i));
            mangleScheme = MangleScheme.HIERACHY;
            final String hname = visitNamespace(currentNs.peek()) + "/" + pname;
            mangleScheme = MangleScheme.INTERNAL;
            final String iname = visitNamespace(currentNs.peek()) + pname.length() + pname;
            nsInfo.put(iname, new NsInfo(visibility, iname, hname));
            sb.append(String.format(ts, iname));
            if (procState == ProcState.GEN_CODE) {
                sb.append(textBuf);
            }
            sb.append(';');
        }
        switch (procState) {
        case GEN_SYM:
            switch (visibility) {
            case EXPORT:
            case INTERNAL:
                head.append("extern ");
                break;
            case HIDDEN:
                head.append("static ");
                break;
            default:
                throw new RuntimeException("Unhandled visibility module variable of " + visibility);
            }
            head.append(sb).append('\n');
            break;
        case GEN_CODE:
            head.append(sb).append('\n');
            break;
        default:
            throw new RuntimeException("Unhandled process phase of " + procState + " when define module variables");
        }
        return "";
    }

    @Override
    public String visitDefLocal(GrammarParser.DefLocalContext ctx) {
        final String ts = visit(ctx.getChild(ctx.getChildCount() - 1));
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount() - 2; i += 2) {
            textBuf.setLength(0);
            final String pname = visit(ctx.getChild(i));
            final String iname = "_C" + pname.length() + pname;
            locals.peek().add(new LocalVar(iname, String.format(ts, "")));
            sb.append(String.format(ts, iname)).append(textBuf).append(';');
        }
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    @Override
    public String visitLocalInit(GrammarParser.LocalInitContext ctx) {
        if (ctx.v != null) {
            textBuf.append('=').append(visit(ctx.v));
        }
        return ctx.n.getText();
    }

    @Override
    public String visitValueRetType(GrammarParser.ValueRetTypeContext ctx) {
        return visit(ctx.getChild(1));
    }

    @Override
    public String visitVoidRetType(GrammarParser.VoidRetTypeContext ctx) {
        return "void %s";
    }

    @Override
    public String visitLesserFloatPoint(GrammarParser.LesserFloatPointContext ctx) {
        return ctx.children.stream().map(ParseTree::getText).collect(Collectors.joining(" "));
    }

    @Override
    public String visitFloatPoint(GrammarParser.FloatPointContext ctx) {
        final String base = visit(ctx.f);
        if (ctx.getChildCount() > 1) {
            final String mod;
            switch (ctx.getChild(0).getText()) {
            case "complex":
                mod = "_Complex";
                break;
            case "imaginary":
                mod = "_Imaginary";
                break;
            default:
                throw new RuntimeException("Unhandled float point modifier of " + ctx.getChild(0).getText());
            }
            return base + " " + mod + " %s";
        }
        return base + " %s";
    }

    @Override
    public String visitLesserIntegral(GrammarParser.LesserIntegralContext ctx) {
        return ctx.children.stream().map(ParseTree::getText).collect(Collectors.joining(" "));
    }

    @Override
    public String visitIntegral(GrammarParser.IntegralContext ctx) {
        final String base = visit(ctx.i);
        if (ctx.getChildCount() > 1) {
            return ctx.getChild(0).getText() + " " + base + " %s";
        }
        return base + " %s";
    }

    @Override
    public String visitPrimTypeId(GrammarParser.PrimTypeIdContext ctx) {
        String type = ctx.getText();
        if (type.equals("any_t")) {
            type = "void *";
        }
        return type + " %s";
    }

    @Override
    public String visitNsTypeId(GrammarParser.NsTypeIdContext ctx) {
        mangleScheme = MangleScheme.INTERNAL;
        final String t = visit(ctx.n);
        checkCallVisibility(t);
        return t + " %s";
    }

    @Override
    public String visitPtrTypeId(GrammarParser.PtrTypeIdContext ctx) {
        if (ctx.c != null) {
            final String size = visit(ctx.c);
            return String.format(visit(ctx.t), "(%s)[" + size + "]");
        }
        return String.format(visit(ctx.t), "* %s");
    }

    @Override
    public String visitFuncTypeId(GrammarParser.FuncTypeIdContext ctx) {
        final String old = textBuf.toString();
        textBuf.setLength(0);
        paramSeparator = ",";
        // Create dummy scope
        locals.add(new ArrayDeque<>());
        final String p = visit(ctx.p);
        final String sel = textBuf.toString();
        locals.removeFirst();
        textBuf.setLength(0);
        textBuf.append(old);
        final String retType = visit(ctx.r);
        return String.format(retType, "(* %s" + sel + ")" + p);
    }

    @Override
    public String visitArrayBounds(GrammarParser.ArrayBoundsContext ctx) {
        final String part = visit(ctx.getChild(0));
        if (ctx.getChildCount() > 1) {
            return part + "][" + visit(ctx.getChild(2));
        }
        return part;
    }

    @Override
    public String visitBasicTypeId(GrammarParser.BasicTypeIdContext ctx) {
        final StringBuilder mod = new StringBuilder();
        if (ctx.c != null) {
            mod.append(" const");
        }
        if (ctx.v != null) {
            mod.append(" volatile");
        }
        if (mod.length() == 0) {
            return visit(ctx.t);
        }
        return String.format(visit(ctx.t), mod.append(" %s").toString());
    }

    @Override
    public String visitVconstTypeId(GrammarParser.VconstTypeIdContext ctx) {
        return String.format(visit(ctx.t), " const volatile %s");
    }

    @Override
    public String visitStatement(GrammarParser.StatementContext ctx) {
        return visit(ctx.getChild(0)) + ";";
    }

    @Override
    public String visitLvalExpression(GrammarParser.LvalExpressionContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public String visitAssignVar(GrammarParser.AssignVarContext ctx) {
        return visit(ctx.d) + ctx.getChild(1).getText() + visit(ctx.s);
    }

    @Override
    public String visitRetVal(GrammarParser.RetValContext ctx) {
        if (ctx.e == null) {
            return "return";
        }
        return "return " + visit(ctx.e);
    }

    @Override
    public String visitUnitFuncCall(GrammarParser.UnitFuncCallContext ctx) {
        mangleScheme = MangleScheme.INTERNAL;
        final String qualId = visit(ctx.n) + VOID_FUNC_ID;
        checkCallVisibility(qualId);
        return qualId + "()";
    }

    @Override
    public String visitExtUnitCall(GrammarParser.ExtUnitCallContext ctx) {
        final String rawName = ctx.n.getText();
        final String varName = "_C" + rawName.length() + rawName;
        checkCallVisibility(varName);
        if (!currentVar.type.matches("\\w+")) {
            throw new RuntimeException("Extension function calls only support non-pointer types");
        }
        final String rawFName = ctx.s.getText();
        final String synthName = currentVar.type + rawFName.length() + rawFName + EXT_FUNC_ID;
        checkCallVisibility(synthName);
        return synthName + "(&" + varName + ")";
    }
    public static final String VOID_FUNC_ID = "_v";

    @Override
    public String visitExtFuncCall(GrammarParser.ExtFuncCallContext ctx) {
        final String rawName = ctx.n.getText();
        final String varName = "_C" + rawName.length() + rawName;
        checkCallVisibility(varName);
        if (!currentVar.type.matches("\\w+")) {
            throw new RuntimeException("Extension function calls only support non-pointer types");
        }

        final String old = textBuf.toString();
        textBuf.setLength(0);
        final String param = ctx.p.stream().map(this::visit).collect(Collectors.joining(","));
        final String vparam = ctx.v.stream().map(this::visit).collect(Collectors.joining());
        final String rawFName = ctx.s.getText();
        final String synthName = currentVar.type + rawFName.length() + rawFName + EXT_FUNC_ID + textBuf.toString();
        checkCallVisibility(synthName);
        final StringBuilder ret = new StringBuilder()
                .append(synthName)
                .append('(')
                .append('&')
                .append(varName);
        if (!param.trim().isEmpty()) {
            ret.append(',').append(param);
        }
        ret.append(vparam).append(')');
        textBuf.setLength(0);
        textBuf.append(old);
        return ret.toString();
    }
    public static final String EXT_FUNC_ID = "_2of";

    private boolean isNameVisible(final String qualId) {
        for (final Iterator<Deque<LocalVar>> rit = locals.descendingIterator();
                rit.hasNext();) {
            for (final Iterator<LocalVar> vars = rit.next().descendingIterator();
                    vars.hasNext();) {
                final LocalVar cvar = vars.next();
                if (cvar.sameName(qualId)) {
                    currentVar = cvar;
                    return true;
                }
            }
        }

        final NsInfo info = nsInfo.get(qualId);
        if (info == null) {
            throw new RuntimeException(NsInfo.toExternalName(qualId) + " does not exist! Maybe you forgot to import?");
        }

        switch (info.visibility) {
        case EXPORT:
            return true;
        case INTERNAL: {
            // Internal is current and child
            // namespaces.
            mangleScheme = MangleScheme.HIERACHY;
            final String h = visitNamespace(currentNs.peek());
            return h.startsWith(info.hierachy.replaceFirst("/.*$", ""));
        }
        case HIDDEN: {
            // Hidden implies same namespace
            mangleScheme = MangleScheme.HIERACHY;
            final String h = visitNamespace(currentNs.peek());
            return h.equals(info.hierachy);
        }
        default:
            throw new RuntimeException("Unhandled visibility of " + info);
        }
    }

    private void checkCallVisibility(final String qualId) {
        if (!isNameVisible(qualId)) {
            throw new RuntimeException("Illegal referencing to "
                    + NsInfo.toExternalName(qualId) + " from "
                    + (currentNs.peek() == null
                    ? "nameless module"
                    : ("module " + currentNs.peek().getText()))
                    + ": not visible");
        }
    }

    @Override
    public String visitParamFuncCall(GrammarParser.ParamFuncCallContext ctx) {
        final String old = textBuf.toString();
        textBuf.setLength(0);
        mangleScheme = MangleScheme.INTERNAL;
        final String nsPortion = visit(ctx.n);
        final String param = ctx.p.stream().map(this::visit).collect(Collectors.joining(","));
        final String vparam = ctx.v.stream().map(this::visit).collect(Collectors.joining());
        final String qualId = nsPortion + textBuf.toString();
        checkCallVisibility(qualId);
        final String ret = qualId + "(" + param + vparam + ")";
        textBuf.setLength(0);
        textBuf.append(old);
        return ret;
    }

    @Override
    public String visitParameter(GrammarParser.ParameterContext ctx) {
        final String rawPName = ctx.getChild(0).getText();
        textBuf.append('_').append(rawPName.length()).append(rawPName);
        return visit(ctx.getChild(2));
    }

    @Override
    public String visitVariadicParam(GrammarParser.VariadicParamContext ctx) {
        return "," + visit(ctx.e);
    }

    @Override
    public String visitLogOrExpr(GrammarParser.LogOrExprContext ctx) {
        return visit(ctx.e1) + " || " + visit(ctx.e2);
    }

    @Override
    public String visitLogAndExpr(GrammarParser.LogAndExprContext ctx) {
        return visit(ctx.e1) + " && " + visit(ctx.e2);
    }

    @Override
    public String visitBitOrExpr(GrammarParser.BitOrExprContext ctx) {
        return visit(ctx.e1) + ctx.getChild(1).getText() + visit(ctx.e2);
    }

    @Override
    public String visitBitXorExpr(GrammarParser.BitXorExprContext ctx) {
        return visit(ctx.e1) + ctx.getChild(1).getText() + visit(ctx.e2);
    }

    @Override
    public String visitBitAndExpr(GrammarParser.BitAndExprContext ctx) {
        return visit(ctx.e1) + ctx.getChild(1).getText() + visit(ctx.e2);
    }

    @Override
    public String visitEqlLikeExpr(GrammarParser.EqlLikeExprContext ctx) {
        return visit(ctx.e1) + ctx.getChild(1).getText() + visit(ctx.e2);
    }

    @Override
    public String visitRelLikeExpr(GrammarParser.RelLikeExprContext ctx) {
        return visit(ctx.e1) + ctx.getChild(1).getText() + visit(ctx.e2);
    }

    @Override
    public String visitShiftLikeExpr(GrammarParser.ShiftLikeExprContext ctx) {
        return visit(ctx.e1) + ctx.getChild(1).getText() + visit(ctx.e2);
    }

    @Override
    public String visitAddLikeExpr(GrammarParser.AddLikeExprContext ctx) {
        return visit(ctx.e1) + ctx.getChild(1).getText() + visit(ctx.e2);
    }

    @Override
    public String visitMulLikeExpr(GrammarParser.MulLikeExprContext ctx) {
        return visit(ctx.e1) + ctx.getChild(1).getText() + visit(ctx.e2);
    }

    @Override
    public String visitUnaryPrefixExpr(GrammarParser.UnaryPrefixExprContext ctx) {
        String op = ctx.getChild(0).getText();
        switch (op) {
        case "@":
            op = "&";
            break;
        case "!":
            op = "~";
            break;
        case "not":
            op = "!";
            break;
        default:
            break;
        }
        return "(" + op + "(" + visit(ctx.e) + "))";
    }

    @Override
    public String visitCastExpr(GrammarParser.CastExprContext ctx) {
        return "((" + String.format(visit(ctx.t), "") + ")" + visit(ctx.e) + ")";
    }

    @Override
    public String visitBasicExpr(GrammarParser.BasicExprContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public String visitPrimExpr(GrammarParser.PrimExprContext ctx) {
        final String txt = ctx.getText();
        if (txt.equals("null")) {
            return "NULL";
        }
        return txt.replace("?", "\\?");
    }

    @Override
    public String visitTypeSizeExpr(GrammarParser.TypeSizeExprContext ctx) {
        return "(sizeof(" + String.format(visit(ctx.t), "") + "))";
    }

    @Override
    public String visitRefExpr(GrammarParser.RefExprContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public String visitBraceExpr(GrammarParser.BraceExprContext ctx) {
        return "(" + visit(ctx.e) + ")";
    }

    @Override
    public String visitDereference(GrammarParser.DereferenceContext ctx) {
        final String offset = ctx.off == null ? "0" : visit(ctx.off);
        return "(" + visit(ctx.e) + "[" + offset + "])";
    }

    @Override
    public String visitFuncRef(GrammarParser.FuncRefContext ctx) {
        mangleScheme = MangleScheme.INTERNAL;
        final StringBuilder sb = new StringBuilder(visit(ctx.n));
        final String sel = ctx.s.isEmpty() ? "" : ctx.s.stream()
                .map(this::visit)
                .collect(Collectors.joining("_", "_", ""));
        if (ctx.t.isEmpty()) {
            sb.append(sel);
            final String qualId = sb.toString();
            checkCallVisibility(qualId);
            return qualId;
        }
        checkCallVisibility(sb.toString());
        return sb.append(ctx.t.stream()
                .map(e -> {
                    final String rawName = e.n.getText();
                    return e.getChild(0).getText() + "_C" + rawName.length() + rawName;
                })
                .collect(Collectors.joining()))
                .append(sel).toString();
    }

    @Override
    public String visitFuncSel(GrammarParser.FuncSelContext ctx) {
        return ctx.s.getText();
    }

    @Override
    public String visitDefModule(GrammarParser.DefModuleContext ctx) {
        currentNs.push(ctx.ns);
        final String ret = ctx.b.stream().map(this::visit).collect(Collectors.joining("\n"));
        currentNs.pop();
        return ret;
    }

    @Override
    public String visitExportEntity(GrammarParser.ExportEntityContext ctx) {
        visibility = Visibility.EXPORT;
        return visit(ctx.b);
    }

    @Override
    public String visitInternalEntity(GrammarParser.InternalEntityContext ctx) {
        visibility = Visibility.INTERNAL;
        return visit(ctx.b);
    }

    @Override
    public String visitHiddenEntity(GrammarParser.HiddenEntityContext ctx) {
        visibility = Visibility.HIDDEN;
        return visit(ctx.b);
    }

    @Override
    public String visitImportModule(GrammarParser.ImportModuleContext ctx) {
        mangleScheme = MangleScheme.INTERNAL;
        final String qualId = visit(ctx.n);
        if (importSet.contains(qualId)) {
            return "";
        }
        // The standard library has priority in
        // searching. The current directory follows
        // and special environment varibles are
        // searched last(?).
        //
        // Namespace like std::io::println:str
        // looks up the following:
        // println:str in 'std/io.ct'
        mangleScheme = MangleScheme.HIERACHY;
        final String path = "/" + visit(ctx.n) + ".ct";

        final URL iu = Translator.class.getResource(path);
        try {
            final URI f;
            if (iu == null) {
                f = currentFile.peek().resolve(path.substring(1));
            } else {
                try {
                    f = iu.toURI();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            if (currentFile.contains(f)) {
                // Special case where the initial file
                // tries to import itself!
                importSet.add(qualId);
                return "";
            }

            currentFile.push(f);
            importSet.add(qualId);
            final String body = processCharStream(CharStreams.fromStream(f.toURL().openStream()));
            currentFile.pop();
            return body;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to import module " + qualId + ": " + ex.getMessage());
        }
    }

    @Override
    public String visitIncludeLocal(GrammarParser.IncludeLocalContext ctx) {
        if (procState == ProcState.GEN_SYM) {
            pasteInclude.append("#include ").append(ctx.n.getText()).append('\n');
        }
        return "";
    }

    @Override
    public String visitIncludeSys(GrammarParser.IncludeSysContext ctx) {
        if (procState == ProcState.GEN_SYM) {
            pasteInclude.append("#include ").append(ctx.n.getText()).append('\n');
        }
        return "";
    }

    public String processCharStream(CharStream st) throws RecognitionException {
        final GrammarLexer lex = new GrammarLexer(st);
        final TokenStream toks = new CommonTokenStream(lex);
        final GrammarParser parser = new GrammarParser(toks);
        final String body = this.visitProgram(parser.program());
        return body;
    }

    @Override
    public String visitDefExternFunction(GrammarParser.DefExternFunctionContext ctx) {
        final String retType = ctx.r == null ? "void %s" : visit(ctx.r);
        final String tmp = textBuf.toString();
        textBuf.setLength(0);
        // Provide dummy scope
        locals.add(new ArrayDeque<>());
        paramSeparator = ",";
        final String params = visit(ctx.p);
        locals.removeLast();
        // Parameters *MUST* be processed before name
        mangleScheme = MangleScheme.INTERNAL;
        final String rawName = ctx.n.getText();
        final String name = visitNamespace(currentNs.peek()) + rawName.length() + rawName + textBuf.toString();
        mangleScheme = MangleScheme.HIERACHY;
        {
            final String prior = currentNs.peek() == null ? "" : (currentNs.peek().getText() + "::");
            nsInfo.put(name, new NsInfo(visibility, prior + rawName.length() + rawName, visitNamespace(currentNs.peek())));
        }
        textBuf.setLength(0);
        textBuf.append(tmp);
        final StringBuilder body = new StringBuilder();
        if (!retType.equals("void %s")) {
            body.append("return ");
        }
        {
            final String e = ctx.e.getText();
            body.append(e.substring(1, e.length() - 1));
        }
        final String proto = String.format(retType, name + " " + params);
        switch (procState) {
        case GEN_SYM:
            switch (visibility) {
            case EXPORT:
            case INTERNAL:
                head.append("extern");
                break;
            case HIDDEN:
                head.append("static");
                break;
            default:
                throw new AssertionError("Visibility of " + visibility + " not handled!");
            }
            head.append(' ').append(proto).append(";\n");
            return "";
        case GEN_CODE:
            body.append('(').append(Arrays.stream(params.split(","))
                    .map(e -> e.split("_C"))
                    .map(e -> e[e.length - 1])
                    .collect(Collectors.joining(",_C", "_C", ""))).append(';'); // No need to append )!
            return proto + "\n{\n" + body + "\n}";
        default:
            throw new RuntimeException("Unhandled process state of " + procState);
        }
    }

    @Override
    public String visitDefExternMacro(GrammarParser.DefExternMacroContext ctx) {
        if (procState == ProcState.GEN_SYM) {
            final String tmp = textBuf.toString();
            textBuf.setLength(0);
            final String params = ctx.p == null ? "" : visit(ctx.p);
            // Parameters *MUST* be processed before name
            mangleScheme = MangleScheme.INTERNAL;
            final String rawName = ctx.n.getText();
            final String name = visitNamespace(currentNs.peek()) + rawName.length() + rawName + textBuf.toString();
            mangleScheme = MangleScheme.HIERACHY;
            {
                final String prior = currentNs.peek() == null ? "" : (currentNs.peek().getText() + "::");
                nsInfo.put(name, new NsInfo(visibility, prior + rawName.length() + rawName, visitNamespace(currentNs.peek())));
            }
            textBuf.setLength(0);
            textBuf.append(tmp);
            pasteMacro.append("#define ").append(name).append(params).append(' ');
            {
                final String e = ctx.e.getText();
                pasteMacro.append(e.substring(1, e.length() - 1));
            }
            // This line is very GNU specific (the ## syntax)
            pasteMacro.append(params.replace("...", "##__VA_ARGS__")).append("\n");
        }
        return "";
    }

    @Override
    public String visitDefMParams(GrammarParser.DefMParamsContext ctx) {

        if (ctx.getChild(0).getText().equals("(")) {
            textBuf.append(VOID_FUNC_ID);
            return "()";
        }
        final StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < ctx.getChildCount(); i += 2) {
            final String name = ctx.getChild(i).getText();
            textBuf.append('_').append(name.length()).append(name);
            sb.append(name).append(',');
        }
        if (ctx.v == null) {
            sb.deleteCharAt(sb.length() - 1);
        } else {
            sb.append("...");
        }
        return sb.append(')').toString();
    }

    @Override
    public String visitDefExternType(GrammarParser.DefExternTypeContext ctx) {
        if (procState == ProcState.GEN_SYM) {
            String externTypeName = ctx.e.getText();
            externTypeName = externTypeName.substring(1, externTypeName.length() - 1);

            mangleScheme = MangleScheme.INTERNAL;
            final String tname = ctx.n.getText();
            final String name = visitNamespace(currentNs.peek()) + tname.length() + tname;
            mangleScheme = MangleScheme.HIERACHY;
            {
                final String prior = currentNs.peek() == null ? "" : (currentNs.peek().getText() + "::");
                nsInfo.put(name, new NsInfo(visibility, prior + tname.length() + tname, visitNamespace(currentNs.peek())));
            }
            pasteTypedef.append("typedef ").append(externTypeName).append(' ').append(name).append(";\n");
        }
        return "";
    }

    @Override
    public String visitDefStruct(GrammarParser.DefStructContext ctx) {
        if (procState == ProcState.GEN_SYM) {
            mangleScheme = MangleScheme.INTERNAL;
            final String tname = ctx.n.getText();
            final String name = visitNamespace(currentNs.peek()) + tname.length() + tname;
            mangleScheme = MangleScheme.HIERACHY;
            {
                final String prior = currentNs.peek() == null ? "" : (currentNs.peek().getText() + "::");
                nsInfo.put(name, new NsInfo(visibility, prior + tname.length() + tname, visitNamespace(currentNs.peek())));
            }
            // Provide dummy scope
            locals.add(new ArrayDeque<>());
            paramSeparator = ";";
            final String typedefLine = new StringBuilder()
                    .append("typedef struct ").append(name)
                    .append(' ').append(name).append(";\n").toString();
            pasteTypedef.insert(0, typedefLine)
                    .append("struct ").append(name).append("\n{\n");
            for (int i = 2; i < ctx.getChildCount() - 1; i += 2) {
                pasteTypedef.append(visit(ctx.getChild(i))).append(";\n");
            }
            locals.removeLast();
            pasteTypedef.append("};\n");
        }
        return "";
    }

    @Override
    public String visitDefUnion(GrammarParser.DefUnionContext ctx) {
        if (procState == ProcState.GEN_SYM) {
            mangleScheme = MangleScheme.INTERNAL;
            final String tname = ctx.n.getText();
            final String name = visitNamespace(currentNs.peek()) + tname.length() + tname;
            mangleScheme = MangleScheme.HIERACHY;
            {
                final String prior = currentNs.peek() == null ? "" : (currentNs.peek().getText() + "::");
                nsInfo.put(name, new NsInfo(visibility, prior + tname.length() + tname, visitNamespace(currentNs.peek())));
            }
            // Provide dummy scope
            locals.add(new ArrayDeque<>());
            paramSeparator = ";";
            final String typedefLine = new StringBuilder()
                    .append("typedef union ").append(name)
                    .append(' ').append(name).append(";\n").toString();
            pasteTypedef.insert(0, typedefLine)
                    .append("union ").append(name).append("\n{\n");
            for (int i = 2; i < ctx.getChildCount() - 1; i += 2) {
                pasteTypedef.append(visit(ctx.getChild(i))).append(";\n");
            }
            locals.removeLast();
            pasteTypedef.append("};\n");
        }
        return "";
    }

    @Override
    public String visitBlockScope(GrammarParser.BlockScopeContext ctx) {
        if (ctx.s.isEmpty()) {
            return "";
        }
        locals.add(new ArrayDeque<>());
        final String body = ctx.s.stream()
                .map(this::visit)
                .collect(Collectors.joining("\n", "{\n", "\n}//"));
        locals.removeLast();
        return body;
    }

    @Override
    public String visitAlterFlow(GrammarParser.AlterFlowContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitSwitchFlow(GrammarParser.SwitchFlowContext ctx) {
        final String expr = visit(ctx.e);
        final String casePart1 = ctx.c1.stream()
                .map(this::visit)
                .collect(Collectors.joining("\n"));
        final String defaultCase = ctx.d == null ? "" : visit(ctx.d);
        final String casePart2 = ctx.c2.stream()
                .map(this::visit)
                .collect(Collectors.joining("\n"));
        return "switch (" + expr + ")\n{\n"
                + casePart1 + "\n"
                + defaultCase + "\n"
                + casePart2
                + "\n}//";
    }

    @Override
    public String visitCaseFlow(GrammarParser.CaseFlowContext ctx) {
        locals.add(new ArrayDeque<>());
        final String ret = ctx.s.stream()
                .map(this::visit)
                .collect(Collectors.joining("\n",
                                            "case " + visit(ctx.e) + ":\n{\n",
                                            "\n}"));
        locals.removeLast();
        return ret;
    }

    @Override
    public String visitDefaultFlow(GrammarParser.DefaultFlowContext ctx) {
        locals.add(new ArrayDeque<>());
        final String ret = ctx.s.stream()
                .map(this::visit)
                .collect(Collectors.joining("\n", "default:\n{\n", "\n}"));
        locals.removeLast();
        return ret;
    }

    @Override
    public String visitForFlow(GrammarParser.ForFlowContext ctx) {
        locals.add(new ArrayDeque<>());
        final String stmts = ctx.s.isEmpty() ? "{\n}//" : ctx.s.stream()
                .map(this::visit)
                .collect(Collectors.joining("\n", "{\n", "\n}//"));
        locals.removeLast();
        return "for (" + visit(ctx.c) + ")\n" + stmts;
    }

    @Override
    public String visitForCondition(GrammarParser.ForConditionContext ctx) {
        final StringBuilder sb = new StringBuilder();
        if (ctx.i != null) {
            sb.append(visit(ctx.i));
        }
        sb.append(';');
        if (ctx.c != null) {
            sb.append(visit(ctx.c));
        }
        sb.append(';');
        if (ctx.f != null) {
            sb.append(visit(ctx.f));
        }
        return sb.toString();
    }

    @Override
    public String visitIfFlow(GrammarParser.IfFlowContext ctx) {
        locals.add(new ArrayDeque<>());
        final StringBuilder tmp = new StringBuilder()
                .append(ctx.s.stream()
                        .map(this::visit)
                        .collect(Collectors.joining("\n",
                                                    "if (" + visit(ctx.c) + ")\n{\n",
                                                    "\n}//")));
        locals.removeLast();
        if (!ctx.a.isEmpty()) {
            tmp.append(ctx.a.stream()
                    .map(this::visit)
                    .collect(Collectors.joining("\n", "\n", "")));
        }
        if (ctx.e != null) {
            tmp.append('\n').append(visit(ctx.e));
        }
        return tmp.toString();
    }

    @Override
    public String visitElseIfFlow(GrammarParser.ElseIfFlowContext ctx) {
        locals.add(new ArrayDeque<>());
        final String ret = ctx.s.stream()
                .map(this::visit)
                .collect(Collectors.joining("\n",
                                            "else if (" + visit(ctx.c) + ")\n{\n",
                                            "\n}//"));
        locals.removeLast();
        return ret;
    }

    @Override
    public String visitElseFlow(GrammarParser.ElseFlowContext ctx) {
        locals.add(new ArrayDeque<>());
        final String ret = ctx.s.stream()
                .map(this::visit)
                .collect(Collectors.joining("\n", "else\n{\n", "\n}//"));
        locals.removeLast();
        return ret;
    }

    @Override
    public String visitLabelFlow(GrammarParser.LabelFlowContext ctx) {
        return "_T" + ctx.n.getText() + ":\n" + visit(ctx.s) + "//";
    }

    @Override
    public String visitGotoFlow(GrammarParser.GotoFlowContext ctx) {
        return "goto _T" + ctx.n.getText();
    }
}
