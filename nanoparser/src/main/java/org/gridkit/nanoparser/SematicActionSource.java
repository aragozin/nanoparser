package org.gridkit.nanoparser;

import java.util.Collection;

import org.gridkit.nanoparser.SemanticActionHandler.BinaryActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.TermActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.UnaryActionHandler;

public interface SematicActionSource<C> {

    public Collection<TermAction<C>> enumTerms();

    public Collection<UnaryAction<C>> enumUnaries();

    public Collection<BinaryAction<C>> enumBinaries();

    public Collection<Converter<C>> enumConverters();


    public interface TermAction<C> {

        public String opId();

        public Class<?> returnType();

        public <R> TermActionHandler<C, R> handler();
    }

    public interface Converter<C> {

        public Class<?> returnType();

        public Class<?> inputType();

        public <R, A> UnaryActionHandler<C, R, A> handler();
    }

    public interface UnaryAction<C> {

        public String opId();

        public Class<?> returnType();

        public Class<?> argType();

        public Collection<Class<?>> convertibleArgTypes();

        public <R, A> UnaryActionHandler<C, R, A> handler();
    }

    public interface BinaryAction<C> {

        public String opId();

        public Class<?> returnType();

        public Class<?> leftType();

        public Class<?> rightType();

        public Collection<Class<?>> convertibleLeftTypes();

        public Collection<Class<?>> convertibleRightTypes();

        public <R, A, B> BinaryActionHandler<C, R, A, B> handler();
    }
}
