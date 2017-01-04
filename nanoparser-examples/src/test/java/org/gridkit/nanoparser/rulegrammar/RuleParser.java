package org.gridkit.nanoparser.rulegrammar;

import java.util.Arrays;

import org.gridkit.nanoparser.NanoGrammar;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.ReflectionActionSource;
import org.gridkit.nanoparser.rulegrammar.AST.BoolClause;
import org.gridkit.nanoparser.rulegrammar.AST.Clause;
import org.gridkit.nanoparser.rulegrammar.AST.Expr;
import org.gridkit.nanoparser.rulegrammar.AST.Functor;
import org.gridkit.nanoparser.rulegrammar.AST.ImplicationRule;
import org.gridkit.nanoparser.rulegrammar.AST.InvariantRule;
import org.gridkit.nanoparser.rulegrammar.AST.Lit;
import org.gridkit.nanoparser.rulegrammar.AST.Literal;
import org.gridkit.nanoparser.rulegrammar.AST.Var;

public class RuleParser extends ReflectionActionSource<Void> {

    public static final SyntaticScope QUOTED_STRING = NanoGrammar.newParseTable()
            .term("~[^\\\\\'\"]+")
            .term("ESCAPE", "~\\\\x[a-fA-F0-9][a-fA-F0-9]")
            .term("ESCAPE", "~\\\\.")
            .glueOp("CONCAT")
            .toScope();
            
    
    public static final SyntaticScope MAIN_GRAMMAR = NanoGrammar.newParseTable()
            // leading tilde (~) in token use for RegEx
            .skip("~\\s") // ignore white spaces
            .enclosure("STRING", "\"", "\"").scope(QUOTED_STRING)
            .term("NUM", "~\\d+([.]\\d+)?") // simple decimal token
            .term("VAR", "~[A-Z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*") // upper case - var
            .term("LIT", "~[a-z_][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*") // lower case - lit
            .enclosure("(", ")")
                .implicitPrefixOp("()", true)
                .nestedInfixOp("+").rank(4)
                .nestedInfixOrPrefixOp("-").rank(4)
                .nestedInfixOp("*").rank(5)
                .nestedInfixOp("/").rank(5)
                            
            .infixOp(":-").rank(1) // implication
            .infixOp(":=").rank(1) // invariant

            .infixOp("|").rank(2)
            .infixOp(",").rank(3)
            
            .prefixOp("!").rank(6)
            .separator(".")
            .toScope();

    @Term("NUM")
    public Number num(String param) {
        if (param.indexOf('.') >= 0) {
            return Double.valueOf(param);
        }
        else {
            return Long.valueOf(param);
        }
    }

    @Term("VAR")
    public Var var(String param) {
        return new Var(param);
    }

    @Term("LIT")
    public Lit lit(String param) {
        return new Lit(param);
    }

    @Unary("STRING")
    public String string(String param) {
        return param;
    }
    
    @Term("ESCAPE")
    public String escapeChar(String escape) {
        if (escape.startsWith("\\x")) {
            int ch = Integer.parseInt(escape.substring(2), 16);
            return String.valueOf((char)ch);
        }
        else {
            return escape.substring(1);
        }
    }
    
    @Binary("CONCAT")
    public String concat(String a, String b) {
        return a + b;
    }
    
    @Convertion
    public Expr var2expr(Var var) {
        return new Expr(var);
    }

    @Convertion
    public Expr num2expr(Number num) {
        return new Expr(new Literal(num));
    }

    @Convertion
    public Expr string2expr(String num) {
        return new Expr(new Literal(num));
    }

    @Convertion
    public Expr functor2expr(Functor functor) {
        return new Expr(functor);
    }
    
    @Convertion
    public Clause simpleClause(Functor functor) {
        return new Clause(functor);
    }

    @Convertion
    public Clause simpleClause(Lit literal) {
        return new Clause(new Functor(literal, new Expr[0]));
    }

    @Binary("()")
    public Functor functor(Lit name, @Convertible Expr params) {
        return new Functor(name, new Expr[]{params});
    }

    @Binary("()")
    public Functor functor(Lit name, @Convertible Expr[] params) {
        return new Functor(name, params);
    }

    @Binary(":-")
    public ImplicationRule implication(Functor lhs, @Convertible Clause rhs) {
        return new ImplicationRule(lhs, rhs);
    }

    @Binary(":=")
    public InvariantRule invariant(Functor lhs, @Convertible Clause rhs) {
        return new InvariantRule(lhs, rhs);
    }
    
    @Binary(",")
    public Clause clauseList(@Convertible Clause a, @Convertible Clause b) {
        return new Clause(new BoolClause(a, b, true));
    }

    @Binary(",")
    public Expr[] exprList(@Convertible Expr a, @Convertible Expr b) {
        return new Expr[]{a, b};        
    }

    @Binary(",")
    public Expr[] exprList(Expr[] a, @Convertible Expr b) {
        Expr[] r = Arrays.copyOf(a, a.length + 1);
        r[a.length] = b;
        return r;        
    }    
}
