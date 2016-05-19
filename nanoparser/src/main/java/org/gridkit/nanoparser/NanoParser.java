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
        if (v instanceof Error) {
            error(((Error) v).token, ((Error) v).message);
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

    private <T> Object convertToken(C parserContext, Class<T> type, String id, PToken token) {
        if (id.length() == 0 && type != String.class) {
            return new ConversionError(token, type, String.class); 
        }
        ActionHandler<C, T, Object, Void> h = actionDispatcher.lookupTerm(id, type);
        if (h == null) {
            return new OperationError(token, type, id); 
        }
        else {
            return callUnary(parserContext, h, token, token.body);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Object convertUnary(C parserContext, Class<T> type, String id, PToken token, ParseNode node) {
        Class<?>[][] nops = actionDispatcher.enumUnaries(id);
        if (nops == null) {
            return new OperationError(token, type, id); 
        }

        Error fisrtError = null;
        
        // Phase 1: no conversion
        for(Class<?>[] sig: nops) {
            Class<T> rtype = (Class<T>)sig[0];
            Class<Object> atype = (Class<Object>) sig[1];
            if (type.isAssignableFrom(rtype)) {
                Object v = convertTree(parserContext, atype, node);
                if (v instanceof SematicError) {
                    return v;
                }
                else if (v instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) v;
                    }
                    continue;
                }
                else {
                    ActionHandler<C, T, Object, Void> h = actionDispatcher.lookupUnary(id, rtype, atype);
                    return callUnary(parserContext, h, token, v);
                }                
            }
        }

        // Phase 2: conversion allowed
        for(Class<?>[] sig: nops) {
            Class<T> rtype = (Class<T>)sig[0];
            Class<Object> atype = (Class<Object>) sig[1];
            if (type.isAssignableFrom(rtype)) {
                Object v = convertAdaptingTree(parserContext, atype, node);
                if (v instanceof SematicError) {
                    return v;
                }
                else if (v instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) v;
                    }
                    continue;
                }
                else {
                    ActionHandler<C, T, Object, Void> h = actionDispatcher.lookupUnary(id, rtype, atype);
                    return callUnary(parserContext, h, token, v);
                }                
            }
        }
        
        // unable to dispatch
        if (fisrtError != null) {
            return fisrtError;
        }
        else {
            return new OperationError(token, type, id);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Object convertBinary(C parserContext, Class<T> type, String id, PToken token, ParseNode node1, ParseNode node2) {
        Class<?>[][] nops = actionDispatcher.enumBinaries(id);
        if (nops == null) {
            return new OperationError(token, type, id); 
        }

        Error fisrtError = null;
        
        // Phase 1: no conversion
        for(Class<?>[] sig: nops) {
            Class<T> rtype = (Class<T>)sig[0];
            Class<Object> atype = (Class<Object>) sig[1];
            Class<Object> btype = (Class<Object>) sig[2];
            if (type.isAssignableFrom(rtype)) {
                Object a = convertTree(parserContext, atype, node1);
                if (a instanceof SematicError) {
                    return a;
                }
                else if (a instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) a;
                    }
                    continue;
                }

                Object b = convertTree(parserContext, btype, node2);
                if (b instanceof SematicError) {
                    return b;
                }
                else if (b instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) b;
                    }
                    continue;
                }

                ActionHandler<C, T, Object, Object> h = actionDispatcher.lookupBinary(id, rtype, atype, btype);
                if (h == null) {
                    throw new RuntimeException("No action [" + id + "] " + atype.getSimpleName() + ", " + btype.getSimpleName());
                }
                return callBinary(parserContext, h, token, a, b);
            }
        }

        // Phase 2: left argument conversion is allowed
        for(Class<?>[] sig: nops) {
            Class<T> rtype = (Class<T>)sig[0];
            Class<Object> atype = (Class<Object>) sig[1];
            Class<Object> btype = (Class<Object>) sig[2];
            if (type.isAssignableFrom(rtype)) {
                Object a = convertAdaptingTree(parserContext, atype, node1);
                if (a instanceof SematicError) {
                    return a;
                }
                else if (a instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) a;
                    }
                    continue;
                }

                Object b = convertTree(parserContext, btype, node2);
                if (b instanceof SematicError) {
                    return b;
                }
                else if (b instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) b;
                    }
                    continue;
                }

                ActionHandler<C, T, Object, Object> h = actionDispatcher.lookupBinary(id, rtype, atype, btype);
                return callBinary(parserContext, h, token, a, b);
            }
        }

        // Phase 3: right argument conversion is allowed
        for(Class<?>[] sig: nops) {
            Class<T> rtype = (Class<T>)sig[0];
            Class<Object> atype = (Class<Object>) sig[1];
            Class<Object> btype = (Class<Object>) sig[2];
            if (type.isAssignableFrom(rtype)) {
                Object a = convertTree(parserContext, atype, node1);
                if (a instanceof SematicError) {
                    return a;
                }
                else if (a instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) a;
                    }
                    continue;
                }

                Object b = convertAdaptingTree(parserContext, btype, node2);
                if (b instanceof SematicError) {
                    return b;
                }
                else if (b instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) b;
                    }
                    continue;
                }

                ActionHandler<C, T, Object, Object> h = actionDispatcher.lookupBinary(id, rtype, atype, btype);
                return callBinary(parserContext, h, token, a, b);
            }
        }

        
        // Phase 4: both arguments conversion is allowed
        for(Class<?>[] sig: nops) {
            Class<T> rtype = (Class<T>)sig[0];
            Class<Object> atype = (Class<Object>) sig[1];
            Class<Object> btype = (Class<Object>) sig[2];
            if (type.isAssignableFrom(rtype)) {
                Object a = convertAdaptingTree(parserContext, atype, node1);
                if (a instanceof SematicError) {
                    return a;
                }
                else if (a instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) a;
                    }
                    continue;
                }

                Object b = convertAdaptingTree(parserContext, btype, node2);
                if (b instanceof SematicError) {
                    return b;
                }
                else if (b instanceof TypeError) {
                    if (fisrtError == null) {
                        fisrtError = (Error) b;
                    }
                    continue;
                }

                ActionHandler<C, T, Object, Object> h = actionDispatcher.lookupBinary(id, rtype, atype, btype);
                return callBinary(parserContext, h, token, a, b);
            }
        }
        
        // unable to dispatch
        if (fisrtError != null) {
            return fisrtError;
        }
        else {
            return new OperationError(token, type, id);
        }
    }

    private <T> Object convertAdaptingTree(C parserContext, Class<T> type, ParseNode node) {
        Object v = convertTree(parserContext, type, node);
        if (v instanceof Error) {
            for(Class<?> altType: actionDispatcher.enumConvertions(type)) {
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

    @SuppressWarnings("unchecked")
    protected <T> T callUnary(C parserContext, ActionHandler<C, T, Object, Void> h, PToken token, Object param) {
        try {
            return h.apply(parserContext, token, param, null);
        }
        catch(SemanticExpection e) {
            return (T)new SematicError(token, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T callBinary(C parserContext, ActionHandler<C, T, Object, Object> h, PToken token, Object v1, Object v2) {
        try {
            return h.apply(parserContext, token, v1, v2);
        }
        catch(SemanticExpection e) {
            return (T)new SematicError(token, e.getMessage());
        }
    }

    private static abstract class Error {
        PToken token;
        String message;
    }
    
    private static abstract class TypeError extends Error {
        
    }

    private static class SematicError extends Error {
        
        public SematicError(PToken token, String message) {
            this.token = token;
            this.message = message;
                    
        }
    }
    
    private static class ConversionError extends TypeError {
        
        public ConversionError(PToken token, Class<?> targetType, Class<?> sourceType) {
            this.token = token;
            this.message = "Requed type '" + targetType.getSimpleName() + "' but found '" + sourceType.getSimpleName() + "'";
        }
    }

    private static class OperationError extends TypeError {
        
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
