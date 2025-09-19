package org.stella.typecheck;

import org.syntax.stella.Absyn.DeclFun;
import org.syntax.stella.Absyn.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeCheckContext {

	public final Map<String, Type> variables;
	public Type expectedType;
	public Type exceptionType;

	public TypeCheckContext(Map<String, Type> variables, Type exceptionType) {
		this.variables = variables;
		this.expectedType = null;
		this.exceptionType = exceptionType;
	}

	public TypeCheckContext copy() {
		return new TypeCheckContext(new HashMap<>(this.variables), this.exceptionType);
	}
}
