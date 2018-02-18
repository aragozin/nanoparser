package org.gridkit.nanoparser;

import java.util.HashSet;
import java.util.Set;

import org.gridkit.nanoparser.NanoGrammar.OpType;
import org.gridkit.nanoparser.NanoGrammar.OperatorInfo;
import org.gridkit.nanoparser.NanoGrammar.ScopeBuilder;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.SemanticActionHandler.BinaryActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.UnaryActionHandler;

public class SemanticValidator<C> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <C> String validate(SyntaticScope grammar, SematicActionSource<?>... actions) {
        return new SemanticValidator<Object>(new MultiSourceSemanticHandler(actions)).verify(grammar);
    }

    private final SemanticActionHandler<C> handler;

    public SemanticValidator(SemanticActionHandler<C> handler) {
        this.handler = handler;
    }

    public String verify(SyntaticScope scope) {
        OpSet opset = new OpSet();
        scope.apply(opset);
        StringBuilder sb = new StringBuilder();
        for(Op op: opset.ops) {
            if (!op.equals(term(NanoGrammar.ACTION_EOE)))
            checkConsistentcy(sb, op, opset.ops);
        }

        Set<Object> actions = new HashSet<Object>();
        collectAvilableActions(actions, opset.ops);

        verifyActions(actions, opset.ops);

        for(Object aref: actions) {
            sb.append("Unreachable action: " + aref);
        }

        return sb.toString();
    }

    private void collectAvilableActions(Set<Object> actions, Set<Op> ops) {
        for(Op op: ops) {
            if (op.term) {
                // ignore terms
            }
            else if (op.binary) {
                for(BinaryActionHandler<?, ?, ?, ?> ba: handler.enumBinaries(op.tkn, null, null, null)) {
                    actions.add(ba.implemetationReference());
                }
            }
            else {
                // unary
                for(UnaryActionHandler<?, ?, ?> ua: handler.enumUnaries(op.tkn, null, null)) {
                    actions.add(ua.implemetationReference());
                }
            }
        }
    }

    private void verifyActions(Set<Object> actions, Set<Op> ops) {
        for(Op op: ops) {
            if (op.term) {
                // ignore terms
            }
            else if (op.binary) {
                for(BinaryActionHandler<?, ?, ?, ?> ba: handler.enumBinaries(op.tkn, null, null, null)) {
                    if (hasToken(ba.rightType(), new HashSet<Object>(), ops) && hasToken(ba.leftType(), new HashSet<Object>(), ops)) {
                        // resolved
                        actions.remove(ba.implemetationReference());
                    }
                }
            }
            else {
                // unary
                for(UnaryActionHandler<?, ?, ?> ua: handler.enumUnaries(op.tkn, null, null)) {
                    if (hasToken(ua.argType(), new HashSet<Object>(), ops)) {
                        // resolved
                        actions.remove(ua.implemetationReference());
                    }
                }
            }
        }
    }

    private void checkConsistentcy(StringBuilder sb, Op op, Set<Op> ops) {
        if (op.term) {
            if (handler.enumTerm(op.tkn, null).length == 0) {
                sb.append("Missing token action for '" + op.tkn + "'\n");
            }
        }
        else if (op.binary) {
            for(BinaryActionHandler<?, ?, ?, ?> ba: handler.enumBinaries(op.tkn, null, null, null)) {
                if (hasToken(ba.rightType(), new HashSet<Object>(), ops) && hasToken(ba.leftType(), new HashSet<Object>(), ops)) {
                    // resolved
                    return;
                }
            }
            sb.append("Unresolavble binary action '" + op.tkn + "'\n");
        }
        else {
            // unary
            for(UnaryActionHandler<?, ?, ?> ua: handler.enumUnaries(op.tkn, null, null)) {
                if (hasToken(ua.argType(), new HashSet<Object>(), ops)) {
                    // resolved
                    return;
                }
            }
            sb.append("Unresolavble unary action '" + op.tkn + "'\n");
        }
    }

    private boolean hasToken(Class<?> type, Set<Object> stack, Set<Op> tokens) {
        if (type == String.class || tokens.contains(term(NanoGrammar.ACTION_NOOP))) {
            return true;
        }
        for(Op op: tokens) {
            if (op.term && handler.enumTerm(op.tkn, type).length > 0) {
                return true;
            }
        }
        for(Op op: tokens) {
            if (!op.binary && !op.term) {
                for(UnaryActionHandler<?, ?, ?> uah: handler.enumUnaries(op.tkn, type, null)) {
                    if (stack.add(uah)) {
                        try {
                            if (hasToken(uah.argType(), stack, tokens)) {
                                return true;
                            }
                        }
                        finally {
                            stack.remove(uah);
                        }
                    }
                }
            }
        }
        for(Op op: tokens) {
            if (op.binary) {
                for(BinaryActionHandler<?, ?, ?, ?> bah: handler.enumBinaries(op.tkn, type, null, null)) {
                    if (stack.add(bah)) {
                        try {
                            if (hasToken(bah.leftType(), stack, tokens) && hasToken(bah.rightType(), stack, tokens)) {
                                return true;
                            }
                        }
                        finally {
                            stack.remove(bah);
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Op term(String tkn) {
        return new Op(tkn, false, true);
    }

    private static Op unary(String tkn) {
        return new Op(tkn, false, false);
    }

    private static Op binary(String tkn) {
        return new Op(tkn, true, false);
    }

    private static class Op {

        final String tkn;
        final boolean binary;
        final boolean term;

        public Op(String tkn, boolean binary, boolean term) {
            this.tkn = tkn;
            this.binary = binary;
            this.term = term;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (binary ? 1231 : 1237);
            result = prime * result + (term ? 1231 : 1237);
            result = prime * result + ((tkn == null) ? 0 : tkn.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Op other = (Op) obj;
            if (binary != other.binary)
                return false;
            if (term != other.term)
                return false;
            if (tkn == null) {
                if (other.tkn != null)
                    return false;
            } else if (!tkn.equals(other.tkn))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return tkn;
        }
    }

    private class OpSet implements ScopeBuilder {

        Set<Op> ops = new HashSet<Op>();
        int depth = 0;

        @Override
        public void addToken(TokenMatcher[] pattern, OperatorInfo op, OperatorInfo implicitPerfix,  OperatorInfo implicitPostfix) {
            if (op.id().length() > 0) {
                ops.add(term(op.id()));
            }
            if (implicitPerfix != null) {
                ops.add(term(implicitPerfix.id()));
            }
            if (implicitPostfix != null) {
                ops.add(term(implicitPostfix.id()));
            }
        }

        @Override
        public void addOperator(TokenMatcher[] pattern, OperatorInfo op) {
            if (op.type() == OpType.INFIX || op.type() == OpType.INFIX_OR_PREFIX) {
                ops.add(binary(op.id()));
            }
            if (op.type() != OpType.INFIX) {
                ops.add(unary(op.id()));
            }
        }

        @Override
        public void addGlueOperator(OperatorInfo op) {
            ops.add(binary(op.id()));
        }

        @Override
        public void addEnclosing(TokenMatcher[] pattern, OperatorInfo op, OperatorInfo prefixOp, boolean optionalPrefix, SyntaticScope prefixedNestedScope, SyntaticScope normalNestedScope) {
            if (op != null && op.id().length() > 0) {
                ops.add(unary(op.id()));
            }
            if (prefixOp != null) {
                ops.add(binary(prefixOp.id()));
            }
            if (depth < 6) {
                ++depth;
                if (prefixedNestedScope != null) {
                    prefixedNestedScope.apply(this);
                }
                if (normalNestedScope != null) {
                    normalNestedScope.apply(this);
                }
                --depth;
            }
        }

        @Override
        public void addScopeEscapeToken(TokenMatcher[] pattern) {
            // ignore
        }

        @Override
        public void addSkipToken(TokenMatcher pattern) {
            // ignore
        }
    }
}
