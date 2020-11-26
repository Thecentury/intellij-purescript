package net.kenro.ji.jin.purescript.psi.scope;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;

public class PSCoreLibrary {
    public static final String BASICS_MODULE = "Basics";

    private static final Set<String> BUILT_IN_SYMBOLS = new HashSet<String>() {{
//        add("Bool");
//        add("True");
//        add("False");
        add("String");
//        add("Char");
//        add("Int");
//        add("Float");
//        add("List");
    }};

    private static final Set<String> IMPLICIT_IMPORTS = new HashSet<String>() {{
        add(BASICS_MODULE);
//        add("List");
//        add("Maybe");
//        add("Result");
        add("String");
//        add("Tuple");
//        add("Debug");
//        add("Platform");
//        add("Cmd");
//        add("Sub");
    }};

    public static boolean isBuiltIn(final String typeName) {
        return BUILT_IN_SYMBOLS.contains(typeName);
    }

    public static boolean isImplicitImport(final String moduleName) {
        return IMPLICIT_IMPORTS.contains(moduleName);
    }

    public static Stack<String> getImplicitImportsCopy() {
        final Stack<String> result = new Stack<>();
        IMPLICIT_IMPORTS.forEach(result::push);
        return result;
    }

    public static Stream<String> getBuiltInSymbols(){
        return BUILT_IN_SYMBOLS.stream();
    }
}
