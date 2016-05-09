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

import org.gridkit.nanoparser.NanoGrammar.OperatorInfo;
import org.gridkit.nanoparser.NanoGrammar.ScopeBuilder;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.SemanticActionHandler.ActionHandler;

public class NanoParser<C> {

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
        Object v = convertTree(parserContext, type, node);
        if (v instanceof ConversionError) {
            error(((ConversionError) v).token, "Cannot convert to " + ((ConversionError) v).targetType);
        }
        else if (v instanceof OperationError) {
            error(((OperationError) v).token, "Operator inapplicable");
        }
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
                Token tkn = stream.matchToken(table.escapeToken);
                if (tkn != null) {
                    return parser.collapse(tkn);
                }
            }
            Token prev = stream.emptyToken();
            for(ParseTableElement pat: table.table) {
                Token tkn = stream.matchToken(pat.matcher);
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
            error(prev, "Unparsable");
        }
        return parser.collapse(stream.emptyToken());
    }

    private <T> Object convertTree(C parserContext, Class<T> type, ParseNode node) {
        if ("".equals(node.op.id())) {
            // Bypass operator
            
            if (node.leftNode != null || node.rightNode != null) {
                if (node.rightNode == null) {
                    return convertAdaptingTree(parserContext, type, node.leftNode);
                }
                else {
                    error(node.token, "Bypass operator cannot accept two arguments");
                }
            }
            if (type != String.class) {
                ActionHandler<C, T, String, Void> convertor = actionDispatcher.lookupConvertor(String.class, type);
                if (convertor == null) {
                    return new ConversionError(node.token, type, String.class);
                }
                else {
                    return convertor.apply(parserContext, null, node.token.body, null);
                }
            }
            else {
                return node.token.body;
            }
        }
        
        if (node.leftNode == null && node.rightNode == null) {
            return convertToken(parserContext, type, node.op.id(), node.token);
        }
        else if (node.rightNode == null) {
            return convertUnary(parserContext, type, node.op.id(), node.token, node.leftNode);
        }
        else {
            return convertBinary(parserContext, type, node.op.id(), node.token, node.leftNode, node.rightNode);
        }
    }

    private <T> Object convertToken(C parserContext, Class<T> type, String id, Token token) {
        ActionHandler<C, T, Object, Void> h = actionDispatcher.lookupUnary(id, type);
        if (h == null) {
            return new ConversionError(token, type, String.class); 
        }
        else {
            return h.apply(parserContext, token.body, token.body, null);
        }
    }

    private <T> Object convertUnary(C parserContext, Class<T> type, String id, Token token, ParseNode node) {
        ActionHandler<C, T, Object, Void> h = actionDispatcher.lookupUnary(id, type);
        if (h == null) {
            return new OperationError(token, type, id); 
        }
        else {
            Object v = convertAdaptingTree(parserContext, h.leftType(), node);
            if (v instanceof Error) {
                return v;
            }
            else {
                return h.apply(parserContext, token.body, v, null);
            }
        }
    }

    private <T> Object convertBinary(C parserContext, Class<T> type, String id, Token token, ParseNode node1, ParseNode node2) {
        ActionHandler<C, T, Object, Object> h = actionDispatcher.lookupBinary(id, type);
        if (h == null) {
            return new OperationError(token, type, id); 
        }
        else {
            Object v1 = convertAdaptingTree(parserContext, h.leftType(), node1);
            if (v1 instanceof Error) {
                return v1;
            }

            Object v2 = convertAdaptingTree(parserContext, h.rightType(), node2);
            if (v2 instanceof Error) {
                return v2;
            }
            
            return h.apply(parserContext, token.body, v1, v2);
        }
    }

    private <T> Object convertAdaptingTree(C parserContext, Class<T> type, ParseNode node) {
        Object v = convertTree(parserContext, type, node);
        if (v instanceof Error) {
            for(Class<?> altType: actionDispatcher.lookupConvertions(type)) {
                Object v1 = convertTree(parserContext, altType, node);
                if (v1 instanceof Error) {
                    continue;
                }
                
                Object cv1 = actionDispatcher.lookupConvertor(altType, type).apply(parserContext, null, v1, null);
                return cv1;
            }
        }
        return v;
    }

    private static abstract class Error {
        
    }
    
    private static class ConversionError extends Error {
        
        Token token;
        Class<?> targetType;
        @SuppressWarnings("unused")
        Class<?> sourceType;
        
        public ConversionError(Token token, Class<?> targetType, Class<?> sourceType) {
            this.token = token;
            this.targetType = targetType;
            this.sourceType = sourceType;
        }
    }

    private static class OperationError extends Error {
        
        Token token;
        @SuppressWarnings("unused")
        Class<?> targetType;
        @SuppressWarnings("unused")
        String opId;

        public OperationError(Token token, Class<?> targetType, String opId) {
            this.token = token;
            this.targetType = targetType;
            this.opId = opId;
        }
    }
    
    protected static void error(Token token, String message) {
        error(token, message, null);
    }

    protected static void error(Token token, String message, Exception e) {
        if (e != null) {
            throw new ParserException(token.text, token.offset, token.line, token.pos, message, e);
        }
        else {
            throw new ParserException(token.text, token.offset, token.line, token.pos, message);
        }
    }
    
    static class StreamState {
        
        CharSequence text;
        int offset;
        int line;
        int pos;
        
        public Token matchToken(Matcher matcher) {
            matcher.reset(text);
            matcher.region(offset, text.length());
            if (matcher.lookingAt()) {
                Token t = new Token();
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

        public Token emptyToken() {
            Token t = new Token();
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
        public void addEnclosing(String pattern, OperatorInfo op, SyntaticScope nestedScope) {
            ParseTableElement e = new ParseTableElement();
            e.matcher = Pattern.compile(pattern).matcher("");
            e.operatorInfo = op;
            e.enclosing = true;
            e.subscope = nestedScope;
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
            ParseTableElement e = new ParseTableElement();
            e.matcher = Pattern.compile(pattern).matcher("");
            e.operatorInfo = op;
            table.add(e);
        }
        
        @Override
        public void addScopeEscapeToken(String pattern) {
            if (escapeToken != null) {
                throw new IllegalArgumentException("Cannot define second escape token");
            }
            escapeToken = Pattern.compile(pattern).matcher("");
        }
        
        @Override
        public void addToken(String pattern, OperatorInfo op) {
            ParseTableElement e = new ParseTableElement();
            e.matcher = Pattern.compile(pattern).matcher("");
            e.operatorInfo = op;
            e.term = true;
            table.add(e);
        }
        
        @Override
        public void addSkipToken(String pattern) {
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

        private ParseNode collapse(Token mark) {
            
            if (stack.isEmpty()) {
                error(mark, "Empty expression");
            }
            if (last().rank > 0) {
                error(last().token, "Missing right argument");
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
                        error(op.token, " operator expected");
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
                        error(op.token, "Missing left argument for '" + op.token.body + "'");
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
        SyntaticScope subscope;
        ParseTable subtable;
        
        public synchronized ParseTable subtable() {
            if (subtable == null) {
                subtable = new ParseTable(subscope);
            }
            
            return subtable;
        }
    }
    
    private static class Token {
        CharSequence text;
        String body;
        int offset;
        int line;
        int pos;
        
        @Override
        public String toString() {
            return body;
        }
    }
    
    private static class ParseNode {
        
        Token token;
        int rank; // -1 is term rank
        OperatorInfo op;
        ParseNode leftNode;
        ParseNode rightNode;
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
