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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.nanoparser.NanoGrammar.OpType;
import org.gridkit.nanoparser.NanoGrammar.OperatorInfo;
import org.gridkit.nanoparser.NanoGrammar.ScopeBuilder;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.SemanticActionHandler.BinaryActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.TermActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.UnariActionHandler;

public class NanoParser<C> {

    private final static OperatorInfo EVAL_OP = new OperatorInfo(NanoGrammar.ACTION_EVAL, OpType.UNARY, 0, true);
    
    private final SemanticActionHandler<C> actionDispatcher;
    private final ParseTable parseTable;
    
    public NanoParser(SemanticActionHandler<C> actionDispatcher, SyntaticScope scope) {
        this.actionDispatcher = actionDispatcher;
        this.parseTable = new ParseTable(scope);
    }
    
    public <T> T parse(C parserContext, Class<T> type, CharSequence source) {
        StreamState state = new StreamState();
        state.text = source;
        ParseNode node =  parse(state, parseTable);
        if (actionDispatcher.enumUnaries(NanoGrammar.ACTION_EVAL, type, null).length > 0) {
            ParseNode evalNode = new ParseNode();
            evalNode.op = EVAL_OP;
            evalNode.token = state.emptyToken();
            evalNode.leftNode = node;
            node = evalNode;
        }
        Object v = convertTree(parserContext, type, node);
        return type.cast(v);
    }

    protected <T> ParseNode parse(StreamState stream, ParseTable table) {
        ParserState parser = new ParserState();
        
        tokenLoop:
        while(!stream.endOfStream()) {
            
            if (table.skipPattern != null && stream.matchToken(table.skipPattern) != null) {
                continue;
            }
            if (table.escapeToken != null) {
                PToken tkn = stream.matchToken(table.escapeToken);
                if (tkn != null) {
                    return parser.collapse(tkn);
                }
            }
            PToken prev = stream.emptyToken();
            for(ParseTableElement pat: table.table) {
                PToken tkn = stream.matchToken(pat.matcher);
                if (tkn != null) {
                    if (pat.term) {
                        if (!parser.isEmpty() && parser.last().rank < 0 && table.glueToken != null) {
                            ParseNode node = new ParseNode();
                            node.op = table.glueToken;
                            node.token = prev;
                            node.rank = table.glueToken.rank();
                            parser.pushToken(node);                            
                        }
                        
                        ParseNode node = new ParseNode();
                        node.op = pat.operatorInfo;
                        node.token = tkn;
                        node.rank = -1;
                        parser.pushToken(node);
                    }
                    else if (pat.enclosing) {
                        if (pat.prefixOp != null) {
                            ParseNode prefOp = new ParseNode();
                            prefOp.op = pat.prefixOp;
                            prefOp.token = tkn;
                            prefOp.rank = prefOp.op.rank();
                            parser.pushToken(prefOp);
                        }
                        ParseNode node = new ParseNode();
                        node.op = pat.operatorInfo;
                        node.token = tkn;
                        node.rank = -1;
                 
                        node.leftNode = parse(stream, pat.subtable());
                        
                        parser.pushToken(node);                        
                    }
                    else {
                        // regular operator
                        ParseNode node = new ParseNode();
                        node.op = pat.operatorInfo;
                        node.token = tkn;
                        node.rank = pat.operatorInfo.rank();
                        
                        parser.pushToken(node);
                    }
                    
                    // Token processed
                    continue tokenLoop;
                }                
            }
            // No token matched
            error(prev, "Cannot parse next token");
        }
        return parser.collapse(stream.emptyToken());
    }

    private <T> Object convertTree(C parserContext, Class<T> type, ParseNode node) {
        Error error = mapActions(type, node);
        if (error == null) {
            return applyActions(parserContext, type, node);
        }
        else {
            throw new ParserException(error.token, error.message);
        }
    }
    
    private Error mapActions(Class<?> type, ParseNode node) {
        if (isTerm(node)) {
            return mapTermAction(type, node);
        }
        else if (isUnary(node)) {
            return mapUnaryAction(type, node);
        }
        else {
            return mapBinaryAction(type, node);
        }
    }

    private Object applyActions(C parserContext, Class<?> type, ParseNode node) {
        if (isTerm(node)) {
            return applyTermAction(parserContext, type, node);
        }
        else if (isUnary(node)) {
            return applyUnaryAction(parserContext, type, node);
        }
        else {
            return applyBinaryAction(parserContext, type, node);
        }
    }

    private Error mapTermAction(Class<?> type, ParseNode node) {
        if (NanoGrammar.ACTION_NOOP.equals(node.op.id())) {
            if (type != String.class) {
                return new ConversionError(node.token, type, String.class);
            }
            else {
                return null;
            }
        }
        else {
            TermActionHandler<?, ?>[] hh = actionDispatcher.enumTerm(node.op.id(), type);
            if (hh.length == 0) {
                return new OperationError(node.token, type, node.op.id());
            }
            else {
                node.inferedHandler = hh[0];
                return null;
            }
        }
    }

    private Error mapUnaryAction(Class<?> type, ParseNode node) {
        if (NanoGrammar.ACTION_NOOP.equals(node.op.id())) {
            return mapActions(type, node.leftNode);
        }
        else {
            UnariActionHandler<?, ?, ?>[] hh = actionDispatcher.enumUnaries(node.op.id(), type, null);
            Error fe = null;
            for(UnariActionHandler<?, ?, ?> h: hh) {
                Class<?> at = h.argType();
                Error e = mapActions(at, node.leftNode);
                if (e != null) {
                    if (fe != null) {
                        if (e.token.offset > fe.token.offset) {
                            fe = e;
                        }
                    }
                    else {
                        fe = e;
                    }
                }
                else {
                    // solution found
                    node.inferedHandler = h;
                    return null;
                }
            }
            if (fe != null) {
                return fe;
            }
            else {
                Class<?> dt = defaultUnaryType(node.op.id(), defaultType(node.leftNode));
                if (dt == null) {
                    return new OperationError(node.token, type, node.op.id());
                }
                else {
                    return new ConversionError(node.token, type, dt);
                }
            }
        }
    }

    private Error mapBinaryAction(Class<?> type, ParseNode node) {
        
        BinaryActionHandler<?, ?, ?, ?>[] hh = actionDispatcher.enumBinaries(node.op.id(), type, null, null);
        Error fe = null; // right most error
        for(BinaryActionHandler<?, ?, ?, ?> h: hh) {
            Class<?> lt = h.leftType();
            Class<?> rt = h.rightType();
            Error e = mapActions(lt, node.leftNode);
            if (e == null) {
                e = mapActions(rt, node.rightNode);
            }
            if (e != null) {
                if (fe != null) {
                    if (e.token.offset > fe.token.offset) {
                        fe = e;
                    }
                    else if (node.token.offset > fe.token.offset) {
                        if (fe instanceof OperationError) {
                            Class<?> dt = defaultType(node.leftNode);
                            if (dt != null && dt != h.leftType()) {
                                fe = new ConversionError(node.token, h.leftType(), dt);
                            }
                        }
                    }
                }
                else {
                    fe = e;
                }
            }
            else {
                // solution found
                node.inferedHandler = h;
                return null;
            }
        }
        if (fe != null) {
            return fe;
        }
        else {
            Class<?> dt = defaultUnaryType(node.op.id(), defaultType(node.leftNode));
            if (dt == null) {
                return new OperationError(node.token, type, node.op.id());
            }
            else {
                return new ConversionError(node.token, type, dt);
            }
        }
    }

    private Class<?> defaultType(ParseNode node) {
        if (isTerm(node)) {
            return defaultTermType(node.op.id());
        }
        else if (isUnary(node)) {
            return defaultUnaryType(node.op.id(), defaultType(node.leftNode));
        }
        else {
            return defaultBinaryType(node.op.id(), defaultType(node.leftNode), defaultType(node.rightNode));
        }        
    }
    
    private Class<?> defaultTermType(String id) {
        if (NanoGrammar.ACTION_NOOP.equals(id)) {
            return String.class;
        }
        TermActionHandler<?, ?>[] hh = actionDispatcher.enumTerm(id, null);
        return hh.length > 0 ? hh[0].returnType() : null;
    }

    private Class<?> defaultUnaryType(String id, Class<?> argType) {
        if (NanoGrammar.ACTION_NOOP.equals(id)) {
            return argType;
        }
        UnariActionHandler<?, ?, ?>[] hh = actionDispatcher.enumUnaries(id, null, null);
        return hh.length > 0 ? hh[0].returnType() : null;
    }

    private Class<?> defaultBinaryType(String id, Class<?> leftType, Class<?> rightType) {
        BinaryActionHandler<?, ?, ?, ?>[] hh = actionDispatcher.enumBinaries(id, null, leftType, rightType);
        if (hh.length == 0) {
            hh = actionDispatcher.enumBinaries(id, null, leftType, null);
        }
        if (hh.length == 0) {
            hh = actionDispatcher.enumBinaries(id, null, null, rightType);
        }
        return hh.length > 0 ? hh[0].returnType() : null;
    }

    @SuppressWarnings("unchecked")
    private Object applyTermAction(C parserContext, Class<?> type, ParseNode node) {
        if (node.inferedHandler == null) {
            return node.token.body;
        }
        else {
            try {
                TermActionHandler<C, ?> h = (TermActionHandler<C, ?>) node.inferedHandler;
                return h.apply(parserContext, node.token);
            }
            catch(SemanticExpection e) {
                Token tkn = e.getToken();
                tkn = tkn == null ? node.token : tkn;
                throw new ParserException(tkn, e.getMessage(), e);
            }
        }
    }

    private Object applyUnaryAction(C parserContext, Class<?> type, ParseNode node) {
        if (node.inferedHandler == null) {
            return applyActions(parserContext, type, node.leftNode);
        }
        else {
            try {
                @SuppressWarnings("unchecked")
                UnariActionHandler<C, ?, Object> h = (UnariActionHandler<C, ?, Object>) node.inferedHandler;
                return h.apply(parserContext, node.token, applyActions(parserContext, type, node.leftNode));
            }
            catch(SemanticExpection e) {
                Token tkn = e.getToken();
                tkn = tkn == null ? node.token : tkn;
                throw new ParserException(tkn, e.getMessage(), e);
            }
        }
    }

    private Object applyBinaryAction(C parserContext, Class<?> type, ParseNode node) {
        try {
            @SuppressWarnings("unchecked")
            BinaryActionHandler<C, ?, Object, Object> h = (BinaryActionHandler<C, ?, Object, Object>) node.inferedHandler;
            return h.apply(parserContext, node.token, applyActions(parserContext, type, node.leftNode), applyActions(parserContext, type, node.rightNode));
        }
        catch(SemanticExpection e) {
            Token tkn = e.getToken();
            tkn = tkn == null ? node.token : tkn;
            throw new ParserException(tkn, e.getMessage(), e);
        }
    }
    
    private boolean isTerm(ParseNode node) {        
        return node.leftNode == null;
    }
    
    private boolean isUnary(ParseNode node) {
        return node.rightNode == null;
    }

    private static abstract class Error {
        PToken token;
        String message;
    }
    
    private static class ConversionError extends Error {
        
        public ConversionError(PToken token, Class<?> targetType, Class<?> sourceType) {
            this.token = token;
            this.message = "Required type '" + targetType.getSimpleName() + "' but found '" + sourceType.getSimpleName() + "'";
        }
    }

    private static class OperationError extends Error {

        public OperationError(PToken token, Class<?> targetType, String opId) {
            this.token = token;
            this.message = "No action for '" + opId + "' producing '" + targetType.getSimpleName() + "'";
        }
    }
    
    protected static void error(PToken token, String message) {
        error(token, message, null);
    }

    protected static void error(PToken token, String message, Exception e) {
        if (e != null) {
            throw new ParserException(token, message, e);
        }
        else {
            throw new ParserException(token, message);
        }
    }
    
    static class StreamState {
        
        CharSequence text;
        int offset;
        int line;
        int pos;
        
        public PToken matchToken(Matcher matcher) {
            matcher.reset(text);
            matcher.region(offset, text.length());
            if (matcher.lookingAt()) {
                PToken t = new PToken();
                t.text = text;
                t.body = matcher.group(0);
                t.offset = offset;
                t.line = line;
                t.pos = pos;
                offset += t.body.length();
                for(int i = 0; i != t.body.length(); ++i) {
                    if (t.body.charAt(0) == '\n') {
                        ++line;
                        pos = 0;
                    }
                    else {
                        ++pos;
                    }
                }
                return t;
            }
            else {
                return null;
            }
        }        
        
        public boolean endOfStream() {
            return text.length() <= offset;
        }

        public PToken emptyToken() {
            PToken t = new PToken();
            t.text = text;
            t.body = "";
            t.offset = offset;
            t.line = line;
            t.pos = pos;
            return t;
        }
    }
    
    private static class ParseTable implements ScopeBuilder {

        private List<ParseTableElement> table = new ArrayList<NanoParser.ParseTableElement>();
        private OperatorInfo glueToken;
        private Matcher escapeToken;
        private Matcher skipPattern;

        public ParseTable(SyntaticScope scope) {
            scope.apply(this);
        }
        
        @Override
        public void addEnclosing(String pattern, OperatorInfo op, OperatorInfo prefixOp, SyntaticScope nestedScope) {
            ParseTableElement e = new ParseTableElement(pattern);
            e.operatorInfo = op;
            e.enclosing = true;
            e.subscope = nestedScope;
            e.prefixOp = prefixOp;
            table.add(e);
        }
        @Override
        public void addGlueOperator(OperatorInfo op) {
            if (glueToken != null) {
                throw new IllegalArgumentException("Cannot define second glue token");
            }
            glueToken = op;
        }
        
        @Override
        public void addOperator(String pattern, OperatorInfo op) {
            ParseTableElement e = new ParseTableElement(pattern);
            e.operatorInfo = op;
            table.add(e);
        }
        
        @Override
        public void addScopeEscapeToken(String pattern) {
            if (escapeToken != null) {
                throw new IllegalArgumentException("Cannot define second escape token");
            }
            if (pattern.startsWith("~")) {
                escapeToken = Pattern.compile(pattern.substring(1)).matcher("");
            }
            else {
                escapeToken = Pattern.compile(Pattern.quote(pattern)).matcher("");
            }
        }
        
        @Override
        public void addToken(String pattern, OperatorInfo op) {
            ParseTableElement e = new ParseTableElement(pattern);
            e.operatorInfo = op;
            e.term = true;
            table.add(e);
        }
        
        @Override
        public void addSkipToken(String pattern) {
            if (pattern.startsWith("~")) {
                pattern = pattern.substring(1);
            }
            else {
                pattern = Pattern.quote(pattern);
            }
            if (skipPattern != null) {
                pattern = "(" + pattern + ")|(" + skipPattern.pattern().pattern() + ")";
                skipPattern = null;
            }
            skipPattern = Pattern.compile(pattern).matcher("");
        }
    }
    
    private static class ParserState {

        List<ParseNode> stack = new ArrayList<ParseNode>();

        public boolean isEmpty() {
            return stack.isEmpty();
        }

        private ParseNode collapse(PToken mark) {
            
            if (stack.isEmpty()) {
                error(mark, "Empty expression");
            }
            if (last().rank > 0) {
                error(last().token, "Missing right hand side '" + last().token.body + "'");
            }
            
            while(stack.size() > 1) {
                mergeLastOp();
            }
            return stack.get(0);
        }
        
        private ParseNode last() {
            return stack.get(stack.size() - 1);
        }
        
        private void pushToken(ParseNode op) {
            if (op.rank < 0) {
                if (stack.isEmpty()) {
                    stack.add(op);
                }
                else {
                    if (last().rank < 0) {
                        error(op.token, "Operator expected");
                    }
                    else {
                        stack.add(op);
                    }
                }
            }
            else if (op.rank >= 0) {
                if (stack.isEmpty()) {
                    if (op.op.isPrefix()) {
                        stack.add(op);
                        return;
                    }
                    else {
                        error(op.token, "Missing left hand side '" + op.token.body + "'");
                    }
                }
                while(true) {
                    int lor = lastOpRank();
                    if (lor < 0 || lor < op.rank) {
                        stack.add(op);
                        break;
                    }
                    else {
                        if (last().rank >= 0) {
                            if (op.op.isPrefix()) {
                                stack.add(op);
                                break;
                            }
                            else {
                                error(op.token, "Two consequive operators");
                            }
                        }
                        else {
                            mergeLastOp();
                        }
                        continue;
                    }
                }
            }
        }
        
        private int lastOpRank() {
            if (stack.size() == 0) {
                return -1;
            }
            else {
                int s = stack.size();
                ParseNode op = stack.get(s - 1);
                if (op.rank >= 0) {
                    return op.rank;
                }
                else {
                    if (stack.size() < 2) {
                        return -1;
                    }
                    return stack.get(s - 2).rank;
                }
            }
        }
        
        private void mergeLastOp() {
            int s = stack.size();
            ParseNode b = stack.remove(s - 1);
            ParseNode o = stack.remove(s - 2);
            if (o.rank < 0) {
                throw new RuntimeException("Op already collapsed");
            }

            if (stack.isEmpty() || last().rank >= 0) {
                if (o.op.isPrefix()) {
                    // process prefix operator
                    o.leftNode = b;
                    o.rightNode = null;
                    o.rank = -1;
                    stack.add(o);
                }                
            }
            else {
                ParseNode a = stack.remove(s - 3);
                // TODO process right associativity
                o.leftNode = a;
                o.rightNode = b;
                o.rank = -1;
                stack.add(o);
            }
        }
    }
    
    private static class ParseTableElement {
        
        Matcher matcher;
        boolean term;
        boolean enclosing;
        OperatorInfo operatorInfo;
        OperatorInfo prefixOp; // optional enclosure prefix operator
        SyntaticScope subscope;
        ParseTable subtable;
        
        public ParseTableElement(String pattern) {
            if (pattern.startsWith("~")) {
                matcher = Pattern.compile(pattern.substring(1)).matcher("");
            }
            else {
                matcher = Pattern.compile(Pattern.quote(pattern)).matcher("");
            }
        }

        public synchronized ParseTable subtable() {
            if (subtable == null) {
                subtable = new ParseTable(subscope);
            }
            
            return subtable;
        }
    }
    
    private static class PToken implements Token {
        
        CharSequence text;
        String body;
        int offset;
        int line;
        int pos;
        
        @Override
        public String tokenBody() {
            return body;
        }
        
        @Override
        public CharSequence source() {
            return text;
        }
        
        @Override
        public int line() {
            return line + 1;
        }
        
        @Override
        public int pos() {
            return pos;
        }
        
        @Override
        public int offset() {
            return offset;
        }
        
        @Override
        public String excerpt() {
            return excerpt(60);
        }
        
        @Override
        public String excerpt(int excerptLengthLimit) {
            return ParserException.formatTokenExcertp(this, excerptLengthLimit);
        }
        
        @Override
        public String toString() {
            return body;
        }
    }
    
    private static class ParseNode {
        
        PToken token;
        int rank; // -1 is term rank
        OperatorInfo op;
        ParseNode leftNode;
        ParseNode rightNode;

        // used to cache handler chosen by type inference
        Object inferedHandler;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(op.id()).append("[").append(token);
            if (leftNode != null) {
                sb.append(",").append(leftNode);
            }
            if (rightNode != null) {
                sb.append(",").append(rightNode);
            }
            sb.append("]");
            return sb.toString();
        }
        
        
    }
}
