/*
 * Copyright (C) 2016 Alexey Ragozin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.nanoparser;

import java.util.ArrayList;
import java.util.List;

public class NanoGrammar {

    public static String ACTION_NOOP = "";
    
    /**
     * Pseudo action applied at top level
     * of expression evaluation if available.
     * <br/>
     * May be exploited for additional top level 
     * conversion rules.
     */
    public static String ACTION_EVAL = "#EVAL#";
    
    /**
     * @return Builder for new {@link SyntaticScope}
     */
    @SuppressWarnings("unchecked")
    public static ParserBuilder<ParserBuilderTop<?>> newParseTable() {
        return new Builder();
    }

    /**
     * Accepts {@link SyntaticScope} created by {@link ParserBuilderTop#toLazyScope()} method.
     * Allow adding additional parse rules, modifying previously created table instance.
     */
    @SuppressWarnings("unchecked")
    public static ParserBuilder<ParserBuilderTop<?>> extendTable(SyntaticScope scope) {
        if (scope instanceof LazyScope) {
            if (((LazyScope)scope).scope != null) {
                throw new IllegalStateException("Scope is already in use, cannot extend");
            }
            return ((LazyScope)scope).builder;
        }
        else {
            throw new IllegalArgumentException("Only lazy scope is accepted");
        }
    }
    
    public interface ParserBuilder<T extends ParserBuilder<?>> {
        
        public T include(SyntaticScope scope);

        public TermBuilder<T> term(String pattern);

        public TermBuilder<T> term(String opID, String pattern);

        public OpInfixBuilder<T> infixOp(String opID, String pattern);

        public OpInfixBuilder<T> infixOp(String opIDandPattern);

        public OpInfixBuilder<T> infixOrPrefixOp(String opID, String pattern);

        public OpInfixBuilder<T> infixOrPrefixOp(String opIDandPattern);

        public OpPrefixBuilder<T> perfixOp(String opID, String pattern);

        public OpPrefixBuilder<T> perfixOp(String opIDandPattern);

        /**
         * Brackets, parenthesis, braces, function calls, etc 
         * @return
         */
        public OpEnclosureBuilder<T> enclosure(String opID, String openPattern, String closePattern);

        public OpEnclosureBuilder<T> enclosure(String openPattern, String closePattern);
        
        /**
         * This operator would be applied implicitly if stream
         * contains two consecutive terms. 
         */
        public OpInfixBuilder<T> glueOp(String opID);

        /**
         * Tokens would be ignored
         */
        public OpInfixBuilder<T> skip(String pattern);
        
    }
    
    public interface ParserBuilderTop<T extends ParserBuilder<?>> extends ParserBuilder<T> {
        
        public SyntaticScope toScope();

        public SyntaticScope toLazyScope();
    }
    
    public interface OpInfixBuilder<T extends ParserBuilder<?>> extends ParserBuilderTop<T> {
        
        public OpInfixBuilder<T> rank(int id);

        /**
         * Not implemented
         */
        @Deprecated
        public OpInfixBuilder<T> rightAssoc();

        public OpInfixBuilder<T> leftAssoc();
    }

    public interface OpPrefixBuilder<T extends ParserBuilder<?>> extends ParserBuilderTop<T> {
        
        public ParserBuilderTop<T> rank(int id);
    }

    public interface TermBuilder<T extends ParserBuilder<?>> extends ParserBuilderTop<T> {
    }

    public interface OpEnclosureBuilder<T extends ParserBuilder<?>> extends ParserBuilderTop<T> {
        
        public ParserBuilderTop<T> scope(SyntaticScope scope);
        
        public OpEnclosureBuilder<T> implicitPrefixOp(String opID);

        public OpEnclosureWithRankBuilder<T> nestedInfixOp(String opIDandPattern);

        public OpEnclosureWithRankBuilder<T> nestedInfixOp(String opID, String pattern);

        public OpEnclosureWithRankBuilder<T> nestedInfixOrPrefixOp(String opIDandPattern);
        
        public OpEnclosureWithRankBuilder<T> nestedInfixOrPrefixOp(String opID, String pattern);

    }

    public interface OpEnclosureWithRankBuilder<T extends ParserBuilder<?>> extends OpEnclosureBuilder<T> {
        
        public OpEnclosureBuilder<T> rank(int rank);
    }
    
    public static interface SyntaticScope {
        
        public void apply(ScopeBuilder builder);
    }
    
    public interface ScopeBuilder {
        
        public void addToken(String pattern, OperatorInfo op);

        public void addOperator(String pattern, OperatorInfo op);

        public void addGlueOperator(OperatorInfo op);

        public void addEnclosing(String pattern, OperatorInfo op, OperatorInfo prefixOp, SyntaticScope nestedScope);

        public void addScopeEscapeToken(String pattern);

        public void addSkipToken(String pattern);
        
    }
        
    public enum OpType {
        INFIX,
        INFIX_OR_PREFIX,
        PREFIX,
        UNARY
    }
    
    public final static class OperatorInfo {
        
        private final String id;
        private final OpType type;
        private final int rank;
        private final boolean leftAssoc;
        
        public OperatorInfo(String id, OpType type, int rank, boolean leftAssoc) {
            this.id = id;
            this.type = type;
            this.rank = rank;
            this.leftAssoc = leftAssoc;
        }

        public String id() {
            return id;
        }
        
        public boolean isPrefix() {
            return type == OpType.PREFIX || type == OpType.INFIX_OR_PREFIX;
        }
        
        public OpType type() {
            return type;
        }

        public int rank() {
            return rank;
        }
        
        public boolean isLeftAssociative() {
            return leftAssoc;
        }
    }
    
    @SuppressWarnings("rawtypes")
    private static class Builder implements ParserBuilder, OpEnclosureWithRankBuilder, OpInfixBuilder, OpPrefixBuilder, TermBuilder {
        
        List<OpHolder> holders = new ArrayList<OpHolder>();
        Class<?> holderType;
        
        String id;
        String pattern;
        String pattern2;
        String prefixOp;
        OpType type;
        int rank = 1;
        boolean rightAssoc;
        SyntaticScope scope;

        Builder nested;
        
        private void push() {
            if (nested != null) {
                nested.toScope(); // finalize
                nested = null;                
            }
            if (holderType != null) {
                if (holderType == OperatorHolder.class) {
                    OperatorInfo opi = new OperatorInfo(id, type, rank, !rightAssoc);
                    holders.add(new OperatorHolder(pattern, opi));
                }
                else if (holderType == TokenHolder.class) {
                    OperatorInfo opi = new OperatorInfo(id, type, rank, !rightAssoc);
                    holders.add(new TokenHolder(pattern, opi));                    
                }
                else if (holderType == GlueHolder.class) {
                    OperatorInfo opi = new OperatorInfo(id, type, rank, !rightAssoc);
                    holders.add(new GlueHolder(opi));                    
                }
                else if (holderType == EnclosingHolder.class) {
                    OperatorInfo opi = new OperatorInfo(id, type, rank, !rightAssoc);
                    OperatorInfo pop = prefixOp == null ? null : new OperatorInfo(prefixOp, OpType.INFIX, 1000, true); 
                    holders.add(new EnclosingHolder(opi, pattern, pattern2, scope, pop));                    
                }
                else if (holderType == SkipHolder.class) {
                    holders.add(new SkipHolder(pattern));                    
                }
                
                holderType = null;
                id = null;
                pattern = null; 
                pattern2 = null;
                prefixOp = null;
                type = null;
                rank = 1;
                rightAssoc = false;
                scope = null;
            }
        }

        @Override
        public ParserBuilderTop include(SyntaticScope scope) {
            push();
            holders.add(new CloneHolder(scope));
            return this;
        }

        @Override
        public TermBuilder term(String pattern) {
            push();
            this.id = "";
            this.type = OpType.UNARY;
            this.pattern = pattern;
            this.holderType = TokenHolder.class;
            return this;
        }

        @Override
        public TermBuilder term(String opID, String pattern) {
            push();
            this.id = opID;
            this.type = OpType.UNARY;
            this.pattern = pattern;
            this.holderType = TokenHolder.class;
            return this;
        }

        @Override
        public OpInfixBuilder infixOp(String opIDandPattern) {
            return infixOp(opIDandPattern, opIDandPattern);
        }

        @Override
        public OpInfixBuilder infixOp(String opID, String pattern) {
            push();
            this.id = opID;
            this.type = OpType.INFIX;
            this.pattern = pattern;
            this.holderType = OperatorHolder.class;
            return this;
        }

        @Override
        public OpInfixBuilder infixOrPrefixOp(String opIDandPattern) {
            return infixOrPrefixOp(opIDandPattern, opIDandPattern);
        }

        @Override
        public OpInfixBuilder infixOrPrefixOp(String opID, String pattern) {
            push();
            this.id = opID;
            this.type = OpType.INFIX_OR_PREFIX;
            this.pattern = pattern;
            this.holderType = OperatorHolder.class;
            return this;
        }

        @Override
        public OpPrefixBuilder perfixOp(String opIDandPattern) {
            return perfixOp(opIDandPattern, opIDandPattern);
        }
        
        @Override
        public OpPrefixBuilder perfixOp(String opID, String pattern) {
            push();
            this.id = opID;
            this.type = OpType.PREFIX;
            this.pattern = pattern;
            this.holderType = OperatorHolder.class;
            return this;
        }

        @Override
        public OpEnclosureBuilder enclosure(String opID, String openPattern, String closePattern) {
            push();
            this.id = opID;
            this.type = OpType.UNARY;
            this.pattern = openPattern;
            this.pattern2 = closePattern;
            this.holderType = EnclosingHolder.class;
            this.scope = new LazyScope(this);
            return this;
        }

        @Override
        public OpEnclosureBuilder enclosure(String openPattern, String closePattern) {
            push();
            this.id = "";
            this.type = OpType.UNARY;
            this.pattern = openPattern;
            this.pattern2 = closePattern;
            this.holderType = EnclosingHolder.class;
            this.scope = new LazyScope(this);
            return this;
        }
        
        @Override
        public OpInfixBuilder glueOp(String opID) {
            push();
            this.id = opID;
            this.type = OpType.INFIX;
            this.holderType = GlueHolder.class;
            return this;
        }
        
        @Override
        public OpInfixBuilder skip(String pattern) {
            push();
            this.pattern = pattern; 
            this.holderType = SkipHolder.class;
            return this;
        }

        @Override
        public OpInfixBuilder leftAssoc() {
            rightAssoc = false;
            return this;
        }
        
        @Override
        public OpInfixBuilder rightAssoc() {
            rightAssoc = true;
            return this;
        }
        
        public Builder rank(int rank) {
            if (nested != null) {
                nested.rank(rank);
            }
            else {
                this.rank = rank;
            }
            return this;
        };
        
        @Override
        public OpInfixBuilder scope(SyntaticScope scope) {
            this.scope = scope;
            return this;
        }

        @Override
        public OpEnclosureWithRankBuilder nestedInfixOp(String opIDandPattern) {
            return nestedInfixOp(opIDandPattern, opIDandPattern);
        }
        
        @Override
        public OpEnclosureWithRankBuilder nestedInfixOp(String opID, String pattern) {
            if (nested == null) {
                nested = new Builder();
                nested.include(toLazyScope());
                this.scope = nested.toLazyScope();
            }
            nested.infixOp(opID, pattern);
            return this;
        }
        
        @Override
        public OpEnclosureWithRankBuilder nestedInfixOrPrefixOp(String opIDandPattern) {
            return nestedInfixOrPrefixOp(opIDandPattern, opIDandPattern);
        }
        
        @Override
        public OpEnclosureWithRankBuilder nestedInfixOrPrefixOp(String opID, String pattern) {
            if (nested == null) {
                nested = new Builder();
                nested.include(toLazyScope());
                this.scope = nested.toLazyScope();
            }
            nested.infixOrPrefixOp(opID, pattern);
            return this;
        }
        
        @Override
        public OpEnclosureBuilder implicitPrefixOp(String opID) {
            if (prefixOp != null) {
                throw new IllegalStateException("At most on implicit operator is allowed");
            }
            prefixOp = opID;
            return this;
        }
        
        @Override
        public SyntaticScope toScope() {
            push();
            return new SimpleScope(holders.toArray(new OpHolder[holders.size()]));
        }

        @Override
        public SyntaticScope toLazyScope() {
            return new LazyScope(this);
        }
    }
    
    private static class LazyScope implements SyntaticScope {
        
        private final Builder builder;
        private SyntaticScope scope;

        public LazyScope(Builder builder) {
            this.builder = builder;
        }

        @Override
        public void apply(ScopeBuilder builder) {
            if (scope == null) {
                scope = this.builder.toScope();
            }
            scope.apply(builder);
        }
    }
    
    private static abstract class OpHolder {
        public abstract void apply(ScopeBuilder builder);
    }

    private static class OperatorHolder extends OpHolder {
        
        String pattern;
        OperatorInfo opInfo;
        
        public OperatorHolder(String pattern, OperatorInfo opInfo) {
            this.pattern = pattern;
            this.opInfo = opInfo;
        }

        public void apply(ScopeBuilder builder) {
            builder.addOperator(pattern, opInfo);
        }
    }

    private static class TokenHolder extends OpHolder  {
        
        String pattern;
        OperatorInfo opInfo;

        public TokenHolder(String pattern, OperatorInfo opInfo) {
            this.pattern = pattern;
            this.opInfo = opInfo;
        }

        public void apply(ScopeBuilder builder) {
            builder.addToken(pattern, opInfo);
        }
    }

    private static class GlueHolder extends OpHolder  {
        
        OperatorInfo opInfo;

        public GlueHolder(OperatorInfo opInfo) {
            this.opInfo = opInfo;
        }

        public void apply(ScopeBuilder builder) {
            builder.addGlueOperator(opInfo);
        }
    }

    private static class EnclosingHolder extends OpHolder  {

        OperatorInfo opInfo;
        String openPattern;
        String closePattern;
        SyntaticScope nested;
        OperatorInfo prefixOp;

        public EnclosingHolder(OperatorInfo opInfo, String openPattern, String closePattern, SyntaticScope nested, OperatorInfo prefixOp) {
            this.opInfo = opInfo;
            this.openPattern = openPattern;
            this.closePattern = closePattern;
            this.nested = nested;
            this.prefixOp = prefixOp;
        }

        public void apply(ScopeBuilder builder) {
            builder.addEnclosing(openPattern, opInfo, prefixOp, new NestedScope(nested, closePattern));
        }
    }

    private static class SkipHolder extends OpHolder  {

        String pattern;

        public SkipHolder(String pattern) {
            this.pattern = pattern;
        }

        public void apply(ScopeBuilder builder) {
            builder.addSkipToken(pattern);
        }
    }
    
    private static class CloneHolder extends OpHolder {
        
        private SyntaticScope scope;

        public CloneHolder(SyntaticScope scope) {
            this.scope = scope;
        }

        @Override
        public void apply(ScopeBuilder builder) {
            scope.apply(builder);
        }
    }

    
    private static class NestedScope implements SyntaticScope {
        
        private final SyntaticScope scope;
        private final String escapePattern;
        
        public NestedScope(SyntaticScope scope, String escapePattern) {
            this.scope = scope;
            this.escapePattern = escapePattern;
        }

        @Override
        public void apply(ScopeBuilder builder) {
            builder.addScopeEscapeToken(escapePattern);
            scope.apply(builder);
        }
    }
    
    private static class SimpleScope implements SyntaticScope {
        
        private final OpHolder[] holders;

        public SimpleScope(OpHolder[] holders) {
            this.holders = holders;
        }

        @Override
        public void apply(ScopeBuilder builder) {
            for(OpHolder holder: holders) {
                holder.apply(builder);
            }
        }        
    }
}
