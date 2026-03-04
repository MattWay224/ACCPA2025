package org.stella.typecheck;

import org.syntax.stella.Absyn.*;
import org.syntax.stella.PrettyPrinter;

public class TypeCheckError extends RuntimeException {
	public TypeCheckError(String message, Expr node) {
		super(message + " | at " + PrettyPrinter.print(node));
	}

	public TypeCheckError(String message) {
		super(message);
	}

	public TypeCheckError(String message, Pattern pattern) {
		super(message + " | at " + PrettyPrinter.print(pattern));
	}

	public TypeCheckError(String message, ListMatchCase cases) {
		super(message + " | at " + PrettyPrinter.print(cases.element()));
	}

	public TypeCheckError(String message, AParamDecl pattern) {
		super(message + " | at " + PrettyPrinter.print(pattern));
	}

	public TypeCheckError(String message, TypeVariant var) {
		super(message + " | at " + PrettyPrinter.print(var));
	}

	public TypeCheckError(String message, ABinding ab) {
		super(message + " | at " + PrettyPrinter.print(ab));
	}

	public TypeCheckError(String message, TypeRecord r) {
		super(message + " | at " + PrettyPrinter.print(r));
	}
}