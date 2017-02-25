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
import java.util.regex.Pattern;

public class NanoGrammar {

    public static final String ACTION_NOOP = "";
    
    /**
     * Pseudo action applied at top level
     * of expression evaluation if available.
     * <br/>
     * May be exploited for additional top level 
     * conversion rules.
     */
    public static final String ACTION_EVAL = "#EVAL#";

    /**
     * This is special action for end-of-expression
     * token used for multiple expression parsing.
     */
    public static final String ACTION_EOE = "#EOE#";
    
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
//            if (((LazyScope)scope).scope != null) {
//                throw new IllegalStateException("Scope is already in use, cannot extend");
//            }
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

        public TermBuilder<T> term(String opID, TokenMatcher[] pattern);

        public TermBuilder<T> separator(String pattern);

        public OpInfixBuilder<T> infixOp(String opID, String pattern);

        public OpInfixBuilder<T> infixOp(String opIDandPattern);

        public OpInfixBuilder<T> infixOrPrefixOp(String opID, String pattern);

        public OpInfixBuilder<T> infixOrPrefixOp(String opIDandPattern);

        public OpPrefixBuilder<T> prefixOp(String opID, String pattern);

        public OpPrefixBuilder<T> prefixOp(String opIDandPattern);

        public OpPostfixBuilder<T> postfixOp(String opID, String pattern);
        
        public OpPostfixBuilder<T> postfixOp(String opIDandPattern);

        /**
         * Brackets, parenthesis, braces, function calls, etc 
         * @return
         */
        public OpEnclosureBuilder<T> enclosure(String opID, String openPattern, String closePattern);

        public OpEnclosureBuilder<T> enclosure(String opID, TokenMatcher[] openPattern, String closePattern);

        public OpEnclosureBuilder<T> enclosure(String opID, TokenMatcher[] openPattern, TokenMatcher[] closePattern);

        public OpEnclosureBuilder<T> enclosure(String openPattern, String closePattern);

        public OpEnclosureBuilder<T> enclosure(TokenMatcher[] openPattern, String closePattern);

        public OpEnclosureBuilder<T> enclosure(TokenMatcher[] openPattern, TokenMatcher[] closePattern);
        
        /**
         * This operator would be applied implicitly if stream
         * contains two consecutive terms. 
         */
        public OpInfixBuilder<T> glueOp(String opID);

        /**
         * Tokens would be ignored
         */
        public ParserBuilder<T> skip(String pattern);

        public ParserBuilder<T> skip(String from, String to);

        public ParserBuilder<T> skip(TokenMatcher pattern);

        public ParserBuilder<T> skipSpace();
        
        public SyntaticScope toLazyScope();
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

    public interface OpPostfixBuilder<T extends ParserBuilder<?>> extends ParserBuilderTop<T> {
        
        public ParserBuilderTop<T> rank(int id);
    }

    public interface TermBuilder<T extends ParserBuilder<?>> extends ParserBuilderTop<T> {
        
        public TermBuilder<T> implicitPrefixOp(String opID, int rank);

        public TermBuilder<T> implicitPostfixOp(String opID, int rank);
    
    }

    public interface OpEnclosureBuilder<T extends ParserBuilder<?>> extends ParserBuilderTop<T> {
        
        public ParserBuilderTop<T> scope(SyntaticScope scope);

        public ParserBuilderTop<T> scope(SyntaticScope prefixedScope, SyntaticScope normalScope);
        
        public OpEnclosureBuilder<T> implicitPrefixOp(String opID);

        public OpEnclosureBuilder<T> implicitPrefixOp(String opID, boolean optional);

        public OpEnclosureBuilder<T> implicitOpRank(int rank);

        public OpEnclosureWithRankBuilder<T> nestedPrefixOp(String opIDandPattern);

        public OpEnclosureWithRankBuilder<T> nestedPrefixOp(String opID, String pattern);

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
        
        public void addToken(TokenMatcher[] pattern, OperatorInfo op, OperatorInfo implicitPerfix, OperatorInfo implicitPostfix);

        public void addOperator(TokenMatcher[] pattern, OperatorInfo op);

        public void addGlueOperator(OperatorInfo op);

        public void addEnclosing(TokenMatcher[] pattern, OperatorInfo op, OperatorInfo prefixOp, boolean optionalPrefix, SyntaticScope prefixedNestedScope, SyntaticScope normalNestedScope);

        public void addScopeEscapeToken(TokenMatcher[] pattern);

        public void addSkipToken(TokenMatcher pattern);
        
    }
        
    public enum OpType {
        INFIX,
        INFIX_OR_PREFIX,
        PREFIX,
        POSTFIX,
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

        public boolean isPostfix() {
            return type == OpType.POSTFIX;
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
    private static class Builder implements ParserBuilder, OpEnclosureWithRankBuilder, OpInfixBuilder, OpPrefixBuilder, OpPostfixBuilder, TermBuilder {
        
        List<OpHolder> holders = new ArrayList<OpHolder>();
        Class<?> holderType;
        
        String id;
        TokenMatcher[] pattern;
        TokenMatcher[] pattern2;
        TokenMatcher skipPattern;
        String prefixOp;
        boolean optionalPrefix;
        int prefixOpRank = 1000;
        String postfixOp;
        @SuppressWarnings("unused")
        boolean optionalPostfix;
        int postfixOpRank = 1000;
        OpType type;
        int rank = 1;
        boolean rightAssoc;
        SyntaticScope pscope;
        SyntaticScope nscope;

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
                    OperatorInfo pre = prefixOp == null ? null : new OperatorInfo(prefixOp, OpType.INFIX, prefixOpRank, true);
                    OperatorInfo post = postfixOp == null ? null : new OperatorInfo(postfixOp, OpType.INFIX, postfixOpRank, true);
                    holders.add(new TokenHolder(pattern, opi, pre, post));                    
                }
                else if (holderType == GlueHolder.class) {
                    OperatorInfo opi = new OperatorInfo(id, type, rank, !rightAssoc);
                    holders.add(new GlueHolder(opi));                    
                }
                else if (holderType == EnclosingHolder.class) {
                    OperatorInfo opi = new OperatorInfo(id, type, rank, !rightAssoc);
                    OperatorInfo pop = prefixOp == null ? null : new OperatorInfo(prefixOp, OpType.INFIX, prefixOpRank, true); 
                    holders.add(new EnclosingHolder(opi, pattern, pattern2, pscope, nscope, pop, optionalPrefix));                    
                }
                else if (holderType == SkipHolder.class) {
                    holders.add(new SkipHolder(skipPattern));                    
                }
                
                holderType = null;
                id = null;
                pattern = null; 
                pattern2 = null;
                skipPattern = null;
                prefixOp = null;
                prefixOpRank = 1000;
                optionalPrefix = false;
                postfixOp = null;
                postfixOpRank = 1000;
                optionalPostfix = false;
                type = null;
                rank = 1;
                rightAssoc = false;
                nscope = null;
                pscope = null;
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
            validatePattern(pattern);
            push();
            this.id = "";
            this.type = OpType.UNARY;
            this.pattern = new TokenMatcher[]{simpleMatcher(pattern)};
            this.holderType = TokenHolder.class;
            return this;
        }

        @Override
        public TermBuilder term(String opID, String pattern) {
            return term(opID, new TokenMatcher[]{simpleMatcher(pattern)});
        }

        @Override
        public TermBuilder term(String opID, TokenMatcher[] pattern) {
        	push();
        	this.id = opID;
        	this.type = OpType.UNARY;
        	this.pattern = pattern;
        	this.holderType = TokenHolder.class;
        	return this;
        }

        @Override
        public TermBuilder implicitPrefixOp(String opID, int rank) {
            prefixOp = opID;
            prefixOpRank = rank;
            return this;
        }

        @Override
        public TermBuilder implicitPostfixOp(String opID, int rank) {
            postfixOp = opID;
            postfixOpRank = rank;
            return this;
        }

        @Override
        public TermBuilder separator(String pattern) {
            validatePattern(pattern);
            push();
            this.id = ACTION_EOE;
            this.type = OpType.UNARY;
            this.pattern = new TokenMatcher[]{simpleMatcher(pattern)};
            this.holderType = TokenHolder.class;
            return this;
        }

        @Override
        public OpInfixBuilder infixOp(String opIDandPattern) {
            return infixOp(opIDandPattern, opIDandPattern);
        }

        @Override
        public OpInfixBuilder infixOp(String opID, String pattern) {
            validatePattern(pattern);
            push();
            this.id = opID;
            this.type = OpType.INFIX;
            this.pattern = new TokenMatcher[]{simpleMatcher(pattern)};
            this.holderType = OperatorHolder.class;
            return this;
        }

        @Override
        public OpInfixBuilder infixOrPrefixOp(String opIDandPattern) {
            return infixOrPrefixOp(opIDandPattern, opIDandPattern);
        }

        @Override
        public OpInfixBuilder infixOrPrefixOp(String opID, String pattern) {
            validatePattern(pattern);
            push();
            this.id = opID;
            this.type = OpType.INFIX_OR_PREFIX;
            this.pattern = new TokenMatcher[]{simpleMatcher(pattern)};
            this.holderType = OperatorHolder.class;
            return this;
        }

        @Override
        public OpPrefixBuilder prefixOp(String opIDandPattern) {
            return prefixOp(opIDandPattern, opIDandPattern);
        }
        
        @Override
        public OpPrefixBuilder prefixOp(String opID, String pattern) {
            validatePattern(pattern);
            push();
            this.id = opID;
            this.type = OpType.PREFIX;
            this.pattern = new TokenMatcher[]{simpleMatcher(pattern)};
            this.holderType = OperatorHolder.class;
            return this;
        }

        @Override
        public OpPostfixBuilder postfixOp(String opIDandPattern) {
            return postfixOp(opIDandPattern, opIDandPattern);
        }
        
        @Override
        public OpPostfixBuilder postfixOp(String opID, String pattern) {
            validatePattern(pattern);
            push();
            this.id = opID;
            this.type = OpType.POSTFIX;
            this.pattern = new TokenMatcher[]{simpleMatcher(pattern)};
            this.holderType = OperatorHolder.class;
            return this;
        }

        @Override
        public OpEnclosureBuilder enclosure(String opID, String openPattern, String closePattern) {
            return enclosure(opID, new TokenMatcher[]{simpleMatcher(openPattern)}, new TokenMatcher[]{simpleMatcher(closePattern)});
        }

        @Override
        public OpEnclosureBuilder enclosure(String opID, TokenMatcher[] openPattern, String closePattern) {
        	return enclosure(opID, openPattern, new TokenMatcher[]{simpleMatcher(closePattern)});
        }

        @Override
        public OpEnclosureBuilder enclosure(String opID, TokenMatcher[] openPattern, TokenMatcher[] closePattern) {
        	push();
        	this.id = opID;
        	this.type = OpType.UNARY;
        	this.pattern = openPattern;
        	this.pattern2 = closePattern;
        	this.holderType = EnclosingHolder.class;
        	this.pscope = new LazyScope(this);
        	this.nscope = pscope;
        	return this;
        }

        @Override
        public OpEnclosureBuilder enclosure(String openPattern, String closePattern) {
        	return enclosure("", new TokenMatcher[]{simpleMatcher(openPattern)}, new TokenMatcher[]{simpleMatcher(closePattern)});
        }

        @Override
        public OpEnclosureBuilder enclosure(TokenMatcher[] openPattern, String closePattern) {
        	return enclosure("", openPattern, new TokenMatcher[]{simpleMatcher(closePattern)});
        }

        @Override
        public OpEnclosureBuilder enclosure(TokenMatcher[] openPattern, TokenMatcher[] closePattern) {
        	return enclosure("", openPattern, closePattern);
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
        public ParserBuilder skip(String pattern) {
            validatePattern(pattern);
            push();
            this.skipPattern = simpleMatcher(pattern); 
            this.holderType = SkipHolder.class;
            return this;
        }

        @Override
        public ParserBuilder skip(String start, String end) {
        	return skip(Tokens.comment(simpleMatcher(start), simpleMatcher(end)));
        }

        @Override
		public ParserBuilder skip(TokenMatcher pattern) {
			push();
            this.skipPattern = pattern; 
            this.holderType = SkipHolder.class;
			return this;
		}

		@Override
        public ParserBuilder skipSpace() {
            return skip(Tokens.whitespace());
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
            this.pscope = scope;
            this.nscope = scope;
            return this;
        }

        @Override
        public OpInfixBuilder scope(SyntaticScope pscope, SyntaticScope nscope) {
            this.pscope = pscope;
            this.nscope = nscope;
            return this;
        }

        @Override
        public OpEnclosureWithRankBuilder nestedPrefixOp(String opIDandPattern) {
            return nestedPrefixOp(opIDandPattern, opIDandPattern);
        }

        @Override
        public OpEnclosureWithRankBuilder nestedPrefixOp(String opID, String pattern) {
            validatePattern(pattern);
            if (nested == null) {
                nested = new Builder();
                nested.include(toLazyScope());
                this.pscope = nested.toLazyScope();
                this.nscope = pscope;
            }
            nested.prefixOp(opID, pattern);
            return this;
        }
        
        @Override
        public OpEnclosureWithRankBuilder nestedInfixOp(String opIDandPattern) {
            return nestedInfixOp(opIDandPattern, opIDandPattern);
        }
        
        @Override
        public OpEnclosureWithRankBuilder nestedInfixOp(String opID, String pattern) {
            validatePattern(pattern);
            if (nested == null) {
                nested = new Builder();
                nested.include(toLazyScope());
                this.pscope = nested.toLazyScope();
                this.nscope = pscope;
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
            validatePattern(pattern);
            if (nested == null) {
                nested = new Builder();
                nested.include(toLazyScope());
                this.pscope = nested.toLazyScope();
                this.nscope = pscope;
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
        public OpEnclosureBuilder implicitPrefixOp(String opID, boolean optional) {
            if (prefixOp != null) {
                throw new IllegalStateException("At most on implicit operator is allowed");
            }
            prefixOp = opID;
            optionalPrefix = optional;
            return this;
        }

        @Override
        public OpEnclosureBuilder implicitOpRank(int rank) {
            if (rank < 0) {
                throw new IllegalStateException("Rank should be non-negative (" + rank + ")");
            }
            if (prefixOp == null) {
                throw new IllegalStateException("No implicit prefix operator");
            }
            prefixOpRank = rank;
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
//        private SyntaticScope scope;

        public LazyScope(Builder builder) {
            this.builder = builder;
        }

        @Override
        public void apply(ScopeBuilder builder) {
            this.builder.toScope().apply(builder);
        }
    }
    
    private static abstract class OpHolder {
        public abstract void apply(ScopeBuilder builder);
    }

    private static class OperatorHolder extends OpHolder {
        
        TokenMatcher[] pattern;
        OperatorInfo opInfo;
        
        public OperatorHolder(TokenMatcher[] pattern, OperatorInfo opInfo) {
            this.pattern = pattern;
            this.opInfo = opInfo;
        }

        public void apply(ScopeBuilder builder) {
            builder.addOperator(pattern, opInfo);
        }
    }

    private static class TokenHolder extends OpHolder  {
        
    	TokenMatcher[] pattern;
        OperatorInfo opInfo;
        OperatorInfo implicitPrefix;
        OperatorInfo implicitPostfix;

        public TokenHolder(TokenMatcher[] pattern, OperatorInfo opInfo, OperatorInfo implicitPrefix, OperatorInfo implicitPostfix) {
            this.pattern = pattern;
            this.opInfo = opInfo;
            this.implicitPrefix = implicitPrefix;
            this.implicitPostfix = implicitPostfix;
        }

        public void apply(ScopeBuilder builder) {
            builder.addToken(pattern, opInfo, implicitPrefix, implicitPostfix);
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
        TokenMatcher[] openPattern;
        TokenMatcher[] closePattern;
        SyntaticScope pscope;
        SyntaticScope nscope;
        OperatorInfo prefixOp;
        boolean optionalPrefix;

        public EnclosingHolder(OperatorInfo opInfo, TokenMatcher[] openPattern, TokenMatcher[] closePattern, SyntaticScope pscope, SyntaticScope nscope, OperatorInfo prefixOp, boolean optionalPrefix) {
            this.opInfo = opInfo;
            this.openPattern = openPattern;
            this.closePattern = closePattern;
            this.pscope = pscope;
            this.nscope = nscope;
            this.prefixOp = prefixOp;
            this.optionalPrefix = optionalPrefix;
        }

        public void apply(ScopeBuilder builder) {
            builder.addEnclosing(openPattern, opInfo, prefixOp, optionalPrefix, new NestedScope(pscope, closePattern), new NestedScope(nscope, closePattern));
        }
    }

    private static class SkipHolder extends OpHolder  {

    	TokenMatcher pattern;

        public SkipHolder(TokenMatcher pattern) {
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
        private final TokenMatcher[] escapePattern;
        
        public NestedScope(SyntaticScope scope, TokenMatcher[] escapePattern) {
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
    
    private static TokenMatcher simpleMatcher(String pattern) {
    	if (pattern.startsWith("~")) {
    		return new RegExMatcher(pattern.substring(1));
    	}
    	else {
    		return new StringMatcher(pattern);
    	}
    }
    
    /** Fail fast on invalid regex */
    private static void validatePattern(String text) {
        if (text.startsWith("~")) {
            Pattern.compile(text.substring(1));
        }
    }
}
