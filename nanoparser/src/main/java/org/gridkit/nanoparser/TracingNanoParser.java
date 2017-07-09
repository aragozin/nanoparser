package org.gridkit.nanoparser;

import java.io.PrintStream;

import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;

public class TracingNanoParser<C> extends NanoParser<C> {

	private PrintStream trace;
	private int deepth;

	public TracingNanoParser(SemanticActionHandler<C> actionDispatcher, SyntaticScope scope) {
		super(actionDispatcher, scope);
	}

	public TracingNanoParser(SyntaticScope scope, SematicActionSource<C> actionSource1, SematicActionSource<C> actionSource2) {
		super(scope, actionSource1, actionSource2);
	}

	public TracingNanoParser(SyntaticScope scope, SematicActionSource<C>... actionSources) {
		super(scope, actionSources);
	}

	public TracingNanoParser(SyntaticScope scope, SematicActionSource<C> actionSource) {
		super(scope, actionSource);
	}

	public void setTraceOut(PrintStream trace) {
		this.trace = trace;
	}
	
	@Override
	 protected <T> T evalNode(C parserContext, Class<T> type, SourceReader source,	org.gridkit.nanoparser.NanoParser.ParseNode node) {
		trace("evalNode to " + type.getName());
		dump("  ", node);
		return super.evalNode(parserContext, type, source, node);
	}
	
    private void dump(String indent, org.gridkit.nanoparser.NanoParser.ParseNode node) {
		trace(indent + "-> " + node.op.id() + " [" + node.op.rank() + "] '" + node.token.tokenBody() + "'");
		if (node.leftNode != null) {
			dump(indent + "  ", node.leftNode);
		}
		if (node.rightNode != null) {
			dump(indent + "  ", node.rightNode);
		}
	}

	@Override
    protected Error mapTermAction(Class<?> type, ParseNode node, int bestParsed) {
        trace("mapTermAction: " + type.getSimpleName() + " | " + node.toString());
        ++deepth;
        Error error = super.mapTermAction(type, node, bestParsed);
        --deepth;
        trace(error == null ? "-> OK" : "-> ERROR: " + error);
        return error;
    }

    @Override
    protected Error mapUnaryAction(Class<?> type, ParseNode node, int bestParsed) {
        trace("mapUnaryAction: " + type.getSimpleName() + " | " + node.toString());
        ++deepth;
        Error error = super.mapUnaryAction(type, node, bestParsed);
        --deepth;
        trace(error == null ? "-> OK" : "-> ERROR: " + error);
        return error;
    }

    @Override
    protected Error mapBinaryAction(Class<?> type, ParseNode node, int bestParsed) {
        trace("mapBinaryAction: " + type.getSimpleName() + " | " + node.toString());
        ++deepth;
        Error error = super.mapBinaryAction(type, node, bestParsed);
        --deepth;
        trace(error == null ? "-> OK" : "-> ERROR: " + error);
        return error;
    }
    
    protected void trace(String text) {
        if (trace != null) {
            indent();
            trace.println(text);
        }
    }

    protected void indent() {
        if (trace != null) {
            for(int i = 0; i != deepth; ++i) {
                trace.print("  ");
            }
        }
    }        
}
