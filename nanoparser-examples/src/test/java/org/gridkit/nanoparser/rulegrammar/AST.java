package org.gridkit.nanoparser.rulegrammar;

public class AST {

    public static final Lit PLUS = new Lit("plus");
    public static final Lit MINUS = new Lit("minus");
    public static final Lit MULT = new Lit("mult");
    public static final Lit DIV = new Lit("div");
    public static final Lit SEQ = new Lit("seq");

    public static class Var {

        public final String body;

        public Var(String body) {
            this.body = body;
        }
    }

    public static class Lit {

        public final String body;

        public Lit(String body) {
            this.body = body;
        }
    }

    public static class Functor {

        public final Lit symbol;
        public final Expr[] params;

        public Functor(Lit functor, Expr[] params) {
            this.symbol = functor;
            this.params = params;
        }
    }

    public static class Expr {

        public final Literal literal;
        public final Var variable;
        public final Functor functor;

        public Expr(Literal literal) {
            this.literal = literal;
            this.variable = null;
            this.functor = null;
        }

        public Expr(Var variable) {
            this.literal = null;
            this.variable = variable;
            this.functor = null;
        }

        public Expr(Functor functor) {
            this.literal = null;
            this.variable = null;
            this.functor = functor;
        }
    }

    public static class Literal {

        public final String string;
        public final Number number;
        public final Lit atom;

        public Literal(String string) {
            this.string = string;
            this.number = null;
            this.atom = null;
        }

        public Literal(Number number) {
            this.string = null;
            this.number = number;
            this.atom = null;
        }

        public Literal(Lit atom) {
            this.string = null;
            this.number = null;
            this.atom = atom;
        }
    }

    public static abstract class Statement {

        public final Functor lhs;
        public final Clause rhs;

        public Statement(Functor lhs, Clause rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public static class ImplicationRule extends Statement {

        public final boolean implication = true;

        public ImplicationRule(Functor lhs, Clause rhs) {
            super(lhs, rhs);
        }
    }

    public static class InvariantRule extends Statement {

        public final boolean invariant = true;

        public InvariantRule(Functor lhs, Clause rhs) {
            super(lhs, rhs);
        }
    }

    public static class Clause {

        public final Functor functor;
        public final Inversion negation;
        public final BoolClause binary;

        public Clause(Functor functor) {
            this.functor = functor;
            this.negation = null;
            this.binary = null;
        }

        public Clause(Inversion negation) {
            this.functor = null;
            this.negation = negation;
            this.binary = null;
        }

        public Clause(BoolClause binary) {
            this.functor = null;
            this.negation = null;
            this.binary = binary;
        }
    }

    public static class Inversion {

        public final Clause a;

        public Inversion(Clause a) {
            this.a = a;
        }
    }

    public static class BoolClause {

        public final Clause a;
        public final Clause b;
        public final boolean disjunction;

        public BoolClause(Clause a, Clause b, boolean disjunction) {
            this.a = a;
            this.b = b;
            this.disjunction = disjunction;
        }
    }
}
