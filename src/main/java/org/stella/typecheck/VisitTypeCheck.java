package org.stella.typecheck;

import org.stella.smth.TypePanic;
import org.stella.smth.TypeThrow;
import org.syntax.stella.Absyn.*;
import org.syntax.stella.Absyn.List;
import org.syntax.stella.Absyn.Record;
import org.syntax.stella.PrettyPrinter;

import java.util.*;

public class VisitTypeCheck {

	public class ProgramVisitor implements Program.Visitor<Void, Void> {

		@Override
		public Void visit(AProgram p, Void arg) {
			TypeCheckContext context = new TypeCheckContext(new HashMap<>(), null);

			for (Decl decl : p.listdecl_) {
				decl.accept(new ContextDeclVisitor(), context);
			}

			if (!context.variables.containsKey("main")) {
				throw new TypeCheckError("ERROR_MISSING_MAIN");
			}

			for (Decl decl : p.listdecl_) {
				decl.accept(new DeclVisitor(), context);
			}

			return null;
		}
	}

	public class ContextDeclVisitor implements Decl.Visitor<Void, TypeCheckContext> {

		@Override
		public Void visit(DeclFun p, TypeCheckContext arg) {
			ListType listType = new ListType();
			for (ParamDecl paramDecl : p.listparamdecl_) {
				listType.add(((AParamDecl) paramDecl).type_);
			}
			Type returnType = ((SomeReturnType) p.returntype_).type_;
			arg.variables.put(p.stellaident_, new TypeFun(listType, returnType));
			return null;
		}

		@Override
		public Void visit(DeclFunGeneric p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Void visit(DeclTypeAlias p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Void visit(DeclExceptionType p, TypeCheckContext arg) {
			arg.exceptionType = p.type_;
			return null;
		}

		@Override
		public Void visit(DeclExceptionVariant p, TypeCheckContext arg) {
			return null;
		}
	}

	public class DeclVisitor implements Decl.Visitor<Void, TypeCheckContext> {

		@Override
		public Void visit(DeclFun p, TypeCheckContext arg) {
			SomeReturnType someRet = (SomeReturnType) p.returntype_;
			Type declaredReturn = someRet.type_;

			TypeCheckContext bodyCtx = arg.copy();
			for (ParamDecl pd : p.listparamdecl_) {
				AParamDecl a = (AParamDecl) pd;
				bodyCtx.variables.put(a.stellaident_, a.type_);
			}
			bodyCtx.expectedType = declaredReturn;
			for (Decl d : p.listdecl_) {
				d.accept(this, bodyCtx);
			}

			Type bodyType = p.expr_.accept(new ExprVisitor(), bodyCtx);
			if (!declaredReturn.equals(bodyType)) {
				throw new TypeCheckError(
						"ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION | Expected: " +
								PrettyPrinter.print(declaredReturn) + ", got: " + PrettyPrinter.print(bodyType),
						p.expr_);
			}
			return null;
		}


		@Override
		public Void visit(DeclFunGeneric p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Void visit(DeclTypeAlias p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Void visit(DeclExceptionType p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Void visit(DeclExceptionVariant p, TypeCheckContext arg) {
			return null;
		}
	}

	public class ExprVisitor implements Expr.Visitor<Type, TypeCheckContext> {

		@Override
		public Type visit(Sequence p, TypeCheckContext arg) {
			TypeCheckContext firstCtx = arg.copy();
			firstCtx.expectedType = new TypeUnit();
			p.expr_1.accept(this, firstCtx);
			return p.expr_2.accept(this, arg);
		}

		@Override
		public Type visit(Assign p, TypeCheckContext arg) {
			Type rightType = p.expr_2.accept(this, arg.copy());

			TypeCheckContext leftCtx = arg.copy();
			leftCtx.expectedType = new TypeRef(rightType);
			Type leftType = p.expr_1.accept(this, leftCtx);

			if (leftType instanceof TypeRef leftRef) {
				typeCheck(leftRef.type_, rightType, p.expr_2, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
				return new TypeUnit();
			}
			throw new TypeCheckError("ERROR_NOT_A_REFERENCE", p.expr_1);
		}

		@Override
		public Type visit(If p, TypeCheckContext arg) {
			TypeCheckContext condCtx = arg.copy();
			condCtx.expectedType = new TypeBool();
			Type condType = p.expr_1.accept(this, condCtx);
			typeCheck(new TypeBool(), condType, p.expr_1, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");

			if (arg.expectedType != null) {
				TypeCheckContext br = arg.copy();
				br.expectedType = arg.expectedType;
				Type thenType = p.expr_2.accept(this, br);
				Type elseType = p.expr_3.accept(this, br);
				typeCheck(arg.expectedType, thenType, p.expr_2, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
				typeCheck(arg.expectedType, elseType, p.expr_3, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
				return arg.expectedType;
			}

			Type thenType = p.expr_2.accept(this, arg.copy());
			Type elseType = p.expr_3.accept(this, arg.copy());
			typeCheck(thenType, elseType, p.expr_3, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			return thenType;
		}


		@Override
		public Type visit(Let p, TypeCheckContext arg) {
			TypeCheckContext scope = arg.copy();

			for (PatternBinding pb : p.listpatternbinding_) {
				APatternBinding b = (APatternBinding) pb;

				Type exprType = b.expr_.accept(this, new TypeCheckContext(scope.variables, scope.exceptionType));

				if (b.pattern_ instanceof PatternVar pv) {
					scope.variables.put(pv.stellaident_, exprType);
				} else {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", b.pattern_);
				}
			}

			TypeCheckContext bodyArg = scope.copy();
			bodyArg.expectedType = arg.expectedType;
			return p.expr_.accept(this, bodyArg);
		}

		@Override
		public Type visit(LetRec p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(TypeAbstraction p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(LessThan p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(LessThanOrEqual p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(GreaterThan p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(GreaterThanOrEqual p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Equal p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(NotEqual p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(TypeAsc p, TypeCheckContext arg) {
			arg.expectedType = p.type_;
			Type actualType = p.expr_.accept(this, arg);
			typeCheck(p.type_, actualType, p.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			return p.type_;
		}

		@Override
		public Type visit(TypeCast p, TypeCheckContext arg) {
			Type fromT = p.expr_.accept(this, arg.copy());
			Type toT = p.type_;
			if (arg.expectedType != null) {
				Subtype.expectSubtype(toT, arg.expectedType, p, "ERROR_UNEXPECTED_SUBTYPE");
				return arg.expectedType;
			}
			return toT;
		}

		@Override
		public Type visit(Abstraction p, TypeCheckContext arg) {
			TypeCheckContext lambdaContext = arg.copy();


			ListType paramTypes = new ListType();
			for (ParamDecl paramDecl : p.listparamdecl_) {
				AParamDecl param = (AParamDecl) paramDecl;
				lambdaContext.variables.put(param.stellaident_, param.type_);
				paramTypes.add(param.type_);
			}

			if (arg.expectedType instanceof TypeFun expectedFun) {
				if (expectedFun.listtype_.size() != p.listparamdecl_.size()) {
					throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_PARAMETER", p);
				}
				for (int i = 0; i < p.listparamdecl_.size(); i++) {
					Type declared = ((AParamDecl) p.listparamdecl_.get(i)).type_;
					Type expectedParam = expectedFun.listtype_.get(i);
					if (!declared.equals(expectedParam)) {
						throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_PARAMETER", (AParamDecl) p.listparamdecl_.get(i));
					}
				}
				lambdaContext.expectedType = expectedFun.type_;
			}

			Type returnType = p.expr_.accept(this, lambdaContext);
			return new TypeFun(paramTypes, returnType);
		}

		@Override
		public Type visit(Variant p, TypeCheckContext arg) {
			if (arg.expectedType != null) {
				if (!(arg.expectedType instanceof TypeVariant expected)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_VARIANT", p);
				}
				java.util.LinkedHashMap<String, OptionalTyping> map = new java.util.LinkedHashMap<>();
				for (VariantFieldType vft : expected.listvariantfieldtype_) {
					AVariantFieldType a = (AVariantFieldType) vft;
					map.put(a.stellaident_, a.optionaltyping_);
				}
				OptionalTyping slot = map.get(p.stellaident_);
				if (slot == null) {
					throw new TypeCheckError("ERROR_UNEXPECTED_VARIANT_LABEL", p);
				}

				if (p.exprdata_ instanceof SomeExprData sed) {
					if (slot instanceof SomeTyping st) {
						Type inner = sed.expr_.accept(this, withExpected(arg, st.type_));
						Subtype.expectSubtype(inner, st.type_, sed.expr_, "ERROR_UNEXPECTED_SUBTYPE");
					} else {
						throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", p);
					}
				} else { // NoExprData
					if (slot instanceof SomeTyping) {
						throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", p);
					}
				}
				return expected;
			}

			if (p.exprdata_ instanceof SomeExprData sed) {
				try {
					Type inner = sed.expr_.accept(this, arg.copy());
					ListVariantFieldType lst = new ListVariantFieldType();
					lst.add(new AVariantFieldType(p.stellaident_, new SomeTyping(inner)));
					return new TypeVariant(lst);
				} catch (TypeCheckError e) {
					String m = e.getMessage();
					if (m.startsWith("ERROR_AMBIGUOUS_PANIC_TYPE")
							|| m.startsWith("ERROR_AMBIGUOUS_THROW_TYPE")
							|| m.startsWith("ERROR_AMBIGUOUS_REFERENCE_TYPE")) {
						throw new TypeCheckError("ERROR_AMBIGUOUS_VARIANT_TYPE", p);
					}
					throw e;
				}
			} else {
				ListVariantFieldType lst = new ListVariantFieldType();
				lst.add(new AVariantFieldType(p.stellaident_, new NoTyping()));
				return new TypeVariant(lst);
			}
		}


		@Override
		public Type visit(Match p, TypeCheckContext arg) {
			Type scrutineeType = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			boolean checkExhaustiveness = true;
			return visitMatch(scrutineeType, p.listmatchcase_, arg, checkExhaustiveness);
		}

		private Type visitMatch(Type exprType, ListMatchCase cases, TypeCheckContext arg, boolean exhaustiveness) {
			if (cases.isEmpty()) {
				throw new TypeCheckError("ERROR_ILLEGAL_EMPTY_MATCHING", cases);
			}

			boolean sawInl = false, sawInr = false;
			boolean sawNil = false, sawCons = false;

			Type resultType = null;

			for (MatchCase cse : cases) {
				AMatchCase cs = (AMatchCase) cse;

				TypeCheckContext branchCtx = arg.copy();
				checkAndBindPattern(cs.pattern_, exprType, branchCtx);

				if (exprType instanceof TypeSum) {
					if (cs.pattern_ instanceof PatternInl) sawInl = true;
					if (cs.pattern_ instanceof PatternInr) sawInr = true;
					if (cs.pattern_ instanceof PatternVar) {
						sawInl = true;
						sawInr = true;
					}
				} else if (exprType instanceof TypeList) {
					if (cs.pattern_ instanceof PatternList) sawNil = true;
					if (cs.pattern_ instanceof PatternCons) sawCons = true;
					if (cs.pattern_ instanceof PatternVar) {
						sawNil = true;
						sawCons = true;
					}
				}

				TypeCheckContext bodyCtx = branchCtx.copy();
				bodyCtx.expectedType = arg.expectedType;

				Type branchType = cs.expr_.accept(this, bodyCtx);
				if (arg.expectedType != null) {
					if (!arg.expectedType.equals(branchType)) {
						throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", cs.expr_);
					}
				} else if (resultType == null) {
					resultType = branchType;
				} else if (!resultType.equals(branchType)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", cs.expr_);
				}
			}

			if (exhaustiveness) {
				if (exprType instanceof TypeSum) {
					if (!(sawInl && sawInr)) {
						throw new TypeCheckError("ERROR_NONEXHAUSTIVE_MATCH_PATTERNS", cases);
					}
				} else if (exprType instanceof TypeList) {
					if (!(sawNil && sawCons)) {
						throw new TypeCheckError("ERROR_NONEXHAUSTIVE_MATCH_PATTERNS", cases);
					}
				}
			}

			return (arg.expectedType != null) ? arg.expectedType : resultType;
		}

		private void checkAndBindPattern(Pattern ptn, Type expected, TypeCheckContext ctx) {
			if (ptn instanceof PatternVar pv) {
				// переменная всегда подходит и получает тип expected
				ctx.variables.put(pv.stellaident_, expected);
				return;
			}

			if (ptn instanceof PatternInl pil) {
				if (!(expected instanceof TypeSum sum)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
				}
				if (pil.pattern_ instanceof PatternVar pv) {
					ctx.variables.put(pv.stellaident_, sum.type_1);
				} else if (!(pil.pattern_ instanceof PatternUnit)) {
					checkAndBindPattern(pil.pattern_, sum.type_1, ctx);
				}
				return;
			}

			if (ptn instanceof PatternInr pir) {
				if (!(expected instanceof TypeSum sum)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
				}
				if (pir.pattern_ instanceof PatternVar pv) {
					ctx.variables.put(pv.stellaident_, sum.type_2);
				} else if (!(pir.pattern_ instanceof PatternUnit)) {
					checkAndBindPattern(pir.pattern_, sum.type_2, ctx);
				}
				return;
			}

			if (ptn instanceof PatternList) {
				if (!(expected instanceof TypeList)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
				}
				return;
			}

			if (ptn instanceof PatternCons pc) {
				if (!(expected instanceof TypeList listT)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
				}
				// head
				if (pc.pattern_1 instanceof PatternVar pv1) {
					ctx.variables.put(pv1.stellaident_, listT.type_);
				} else {
					checkAndBindPattern(pc.pattern_1, listT.type_, ctx);
				}
				// tail
				if (pc.pattern_2 instanceof PatternVar pv2) {
					ctx.variables.put(pv2.stellaident_, new TypeList(listT.type_));
				} else {
					checkAndBindPattern(pc.pattern_2, new TypeList(listT.type_), ctx);
				}
				return;
			}

			throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
		}

		@Override
		public Type visit(List p, TypeCheckContext arg) {
			if (arg.expectedType instanceof TypeList expected) {
				for (Expr e : p.listexpr_) {
					Type et = e.accept(this, withExpected(arg, expected.type_));
					typeCheck(expected.type_, et, e, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
				}
				return expected;
			}

			if (p.listexpr_.isEmpty()) {
				throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", p);
			}

			Iterator<Expr> it = p.listexpr_.iterator();
			Type firstT = it.next().accept(this, arg.copy());
			while (it.hasNext()) {
				Expr e = it.next();
				Type et = e.accept(this, withExpected(arg, firstT));
				typeCheck(firstT, et, e, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			}
			return new TypeList(firstT);
		}

		@Override
		public Type visit(Add p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Subtract p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(LogicOr p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Multiply p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Divide p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(LogicAnd p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Ref p, TypeCheckContext arg) {
			if (arg.expectedType instanceof TypeRef expectedRef) {
				TypeCheckContext inner = arg.copy();
				inner.expectedType = expectedRef.type_;
				Type innerType = p.expr_.accept(this, inner);
				typeCheck(expectedRef.type_, innerType, p.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
				return expectedRef;
			}
			return new TypeRef(p.expr_.accept(this, arg));
		}

		@Override
		public Type visit(Deref p, TypeCheckContext arg) {
			TypeCheckContext sub = arg.copy();
			if (arg.expectedType != null) {
				sub.expectedType = new TypeRef(arg.expectedType);
			}
			Type type = p.expr_.accept(this, sub);
			if (type instanceof TypeRef typeRef) {
				return typeRef.type_;
			}
			throw new TypeCheckError("ERROR_NOT_A_REFERENCE", p.expr_);
		}


		@Override
		public Type visit(Application p, TypeCheckContext arg) {
			Type funType = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			if (!(funType instanceof TypeFun typeFun)) {
				throw new TypeCheckError("ERROR_NOT_A_FUNCTION", p.expr_);
			}
			if (p.listexpr_.size() != typeFun.listtype_.size()) {
				throw new TypeCheckError("ERROR_INCORRECT_NUMBER_OF_ARGUMENTS", p);
			}

			for (int i = 0; i < p.listexpr_.size(); ++i) {
				TypeCheckContext argContext = arg.copy();
				argContext.expectedType = typeFun.listtype_.get(i);
				Type actualType = p.listexpr_.get(i).accept(this, argContext);
				typeCheck(typeFun.listtype_.get(i), actualType, p.listexpr_.get(i), "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			}

			return typeFun.type_;
		}

		@Override
		public Type visit(TypeApplication p, TypeCheckContext arg) {
			return null;
		}


		@Override
		public Type visit(DotRecord p, TypeCheckContext arg) {
			Type recType = p.expr_.accept(this, arg.copy());

			if (!(recType instanceof TypeRecord tr)) {
				throw new TypeCheckError("ERROR_NOT_A_RECORD", p.expr_);
			}

			for (RecordFieldType rft : tr.listrecordfieldtype_) {
				ARecordFieldType a = (ARecordFieldType) rft;
				if (a.stellaident_.equals(p.stellaident_)) {
					return a.type_;
				}
			}
			throw new TypeCheckError("ERROR_UNEXPECTED_FIELD_ACCESS", p);
		}


		@Override
		public Type visit(DotTuple p, TypeCheckContext arg) {
			Type exprType = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			if (!(exprType instanceof TypeTuple tupleType)) {
				throw new TypeCheckError("ERROR_NOT_A_TUPLE", p.expr_);
			}

			if (tupleType.listtype_.size() != 2) {
				throw new TypeCheckError("ERROR_UNEXPECTED_TUPLE_LENGTH", p.expr_);
			}
			int index = p.integer_ - 1;

			if (index < 0 || index >= tupleType.listtype_.size()) {
				throw new TypeCheckError("ERROR_TUPLE_INDEX_OUT_OF_BOUNDS", p);
			}

			return tupleType.listtype_.get(index);
		}

		@Override
		public Type visit(Tuple p, TypeCheckContext arg) {
			if (p.listexpr_.size() != 2)
				throw new TypeCheckError("ERROR_UNEXPECTED_TUPLE_LENGTH", p);

			ListType types = new ListType();
			for (Expr e : p.listexpr_) {
				types.add(e.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType)));
			}
			return new TypeTuple(types);
		}

		@Override
		public Type visit(Record p, TypeCheckContext arg) {
			Map<String, Type> expectedMap = null;
			if (arg.expectedType instanceof TypeRecord exp) {
				expectedMap = new LinkedHashMap<>();
				for (RecordFieldType rft : exp.listrecordfieldtype_) {
					ARecordFieldType a = (ARecordFieldType) rft;
					expectedMap.put(a.stellaident_, a.type_);
				}
			}

			Map<String, Type> actual = new LinkedHashMap<>();
			for (Binding b : p.listbinding_) {
				ABinding ab = (ABinding) b;
				TypeCheckContext fieldCtx = arg.copy();
				if (expectedMap != null && expectedMap.containsKey(ab.stellaident_)) {
					fieldCtx.expectedType = expectedMap.get(ab.stellaident_);
				} else {
					fieldCtx.expectedType = null;
				}
				Type t = ab.expr_.accept(this, fieldCtx);
				actual.put(ab.stellaident_, t);
			}

			if (arg.expectedType == null) {
				ListRecordFieldType lr = new ListRecordFieldType();
				for (var entry : actual.entrySet()) {
					lr.add(new ARecordFieldType(entry.getKey(), entry.getValue()));
				}
				return new TypeRecord(lr);
			}

			if (!(arg.expectedType instanceof TypeRecord expected)) {
				throw new TypeCheckError("ERROR_UNEXPECTED_RECORD", p);
			}

			Map<String, Type> expectedMapFinal = new LinkedHashMap<>();
			for (RecordFieldType rft : expected.listrecordfieldtype_) {
				ARecordFieldType a = (ARecordFieldType) rft;
				expectedMapFinal.put(a.stellaident_, a.type_);
			}

			for (String f : actual.keySet()) {
				if (!expectedMapFinal.containsKey(f)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_RECORD_FIELDS", p);
				}
			}
			for (var entry : expectedMapFinal.entrySet()) {
				String f = entry.getKey();
				Type expT = entry.getValue();
				if (!actual.containsKey(f)) {
					throw new TypeCheckError("ERROR_MISSING_RECORD_FIELDS", p);
				}
				typeCheck(expT, actual.get(f), p, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			}

			return expected;
		}


		@Override
		public Type visit(ConsList p, TypeCheckContext arg) {
			if (arg.expectedType instanceof TypeList expectedList) {
				// head : T
				TypeCheckContext headCtx = arg.copy();
				headCtx.expectedType = expectedList.type_;
				Type headT = p.expr_1.accept(this, headCtx);
				Subtype.expectSubtype(headT, expectedList.type_, p.expr_1, "ERROR_UNEXPECTED_SUBTYPE");

				// tail : [T]
				TypeCheckContext tailCtx = arg.copy();
				tailCtx.expectedType = expectedList;
				Type tailT = p.expr_2.accept(this, tailCtx);
				Subtype.expectSubtype(tailT, expectedList, p.expr_2, "ERROR_UNEXPECTED_SUBTYPE");
				return expectedList;
			}

			try {
				Type tailT = p.expr_2.accept(this, arg.copy());
				if (!(tailT instanceof TypeList tailList)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", p.expr_2);
				}
				Type elem = tailList.type_;

				Type headT = p.expr_1.accept(this, withExpected(arg, elem));
				Subtype.expectSubtype(headT, elem, p.expr_1, "ERROR_UNEXPECTED_SUBTYPE");
				return tailList;
			} catch (TypeCheckError e) {
				if (isEmptyListLiteral(p.expr_2)) {
					Type headT = p.expr_1.accept(this, arg.copy());

					TypeCheckContext tailCtx = arg.copy();
					tailCtx.expectedType = new TypeList(headT);
					p.expr_2.accept(this, tailCtx);

					return new TypeList(headT);
				}
				throw e;
			}
		}

		private boolean isEmptyListLiteral(Expr e) {
			return (e instanceof List l) && l.listexpr_.isEmpty();
		}

		private TypeCheckContext withExpected(TypeCheckContext base, Type expected) {
			TypeCheckContext c = base.copy();
			c.expectedType = expected;
			return c;
		}


		@Override
		public Type visit(Head p, TypeCheckContext arg) {
			Type t = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			if (!(t instanceof TypeList list)) {
				throw new TypeCheckError("ERROR_NOT_A_LIST", p.expr_);
			}
			return list.type_;
		}

		@Override
		public Type visit(IsEmpty p, TypeCheckContext arg) {
			Type t = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			if (!(t instanceof TypeList)) {
				throw new TypeCheckError("ERROR_NOT_A_LIST", p.expr_);
			}
			return new TypeBool();
		}

		@Override
		public Type visit(Tail p, TypeCheckContext arg) {
			Type t = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			if (!(t instanceof TypeList list)) {
				throw new TypeCheckError("ERROR_NOT_A_LIST", p.expr_);
			}
			return new TypeList(list.type_);
		}

		@Override
		public Type visit(Panic p, TypeCheckContext arg) {
			if (arg.expectedType == null) {
				throw new TypeCheckError("ERROR_AMBIGUOUS_PANIC_TYPE", p);
			}
			return arg.expectedType;
		}


		@Override
		public Type visit(Throw p, TypeCheckContext arg) {
			if (arg.expectedType == null) {
				throw new TypeCheckError("ERROR_AMBIGUOUS_THROW_TYPE", p);
			}
			return arg.expectedType;
		}


		@Override
		public Type visit(TryCatch p, TypeCheckContext arg) {
			Type tryType = p.expr_1.accept(this, arg);

			if (arg.exceptionType == null) {
				throw new IllegalArgumentException("ERROR_EXCEPTION_TYPE_NOT_DECLARED");
			}

			ListMatchCase listMatchCase = new ListMatchCase();
			listMatchCase.add(new AMatchCase(p.pattern_, p.expr_2));

			Type catchType = visitMatch(arg.exceptionType, listMatchCase, arg, false);

			return inferType(tryType, catchType);
		}

		@Override
		public Type visit(TryWith p, TypeCheckContext arg) {
			Type tryType = p.expr_1.accept(this, arg);
			Type withType = p.expr_2.accept(this, arg);
			return inferType(tryType, withType);
		}

		@Override
		public Type visit(TryCastAs p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Inl p, TypeCheckContext arg) {
			if (arg.expectedType instanceof TypeSum sum) {
				Type leftExp = sum.type_1;
				Type t = p.expr_.accept(this, withExpected(arg, leftExp));
				Subtype.expectSubtype(t, leftExp, p.expr_, "ERROR_UNEXPECTED_SUBTYPE");
				return sum;
			}
			Type left = p.expr_.accept(this, arg.copy());
			return new TypeSum(left, new TypeBottom());
		}

		@Override
		public Type visit(Inr p, TypeCheckContext arg) {
			if (arg.expectedType instanceof TypeSum sum) {
				Type rightExp = sum.type_2;
				Type t = p.expr_.accept(this, withExpected(arg, rightExp));
				Subtype.expectSubtype(t, rightExp, p.expr_, "ERROR_UNEXPECTED_SUBTYPE");
				return sum;
			}
			Type right = p.expr_.accept(this, arg.copy());
			return new TypeSum(new TypeBottom(), right);
		}


		@Override
		public Type visit(Succ p, TypeCheckContext arg) {
			Type innerType = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			typeCheck(new TypeNat(), innerType, p.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			return new TypeNat();
		}

		@Override
		public Type visit(LogicNot p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Pred p, TypeCheckContext arg) {
			Type t = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			typeCheck(new TypeNat(), t, p.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			return new TypeNat();
		}

		@Override
		public Type visit(IsZero p, TypeCheckContext arg) {
			Type t = p.expr_.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			typeCheck(new TypeNat(), t, p.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			return new TypeBool();
		}


		@Override
		public Type visit(Fix p, TypeCheckContext arg) {
			Type type = p.expr_.accept(this, arg);
			if (type instanceof TypeFun) {
				TypeFun typeFun = (TypeFun) type;

				Type argType = typeFun.listtype_.get(0);
				Type returnType = typeFun.type_;

				return inferType(argType, returnType);
			}

			throw new IllegalArgumentException("ERROR_NOT_A_FUNCTION");
		}

		@Override
		public Type visit(NatRec p, TypeCheckContext arg) {
			Type natArgType = p.expr_1.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));
			typeCheck(new TypeNat(), natArgType, p.expr_1, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");

			Type initialType = p.expr_2.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));

			Type actualStepType = p.expr_3.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType));

			ListType innerParamList = new ListType();
			innerParamList.add(initialType);
			TypeFun innerFunType = new TypeFun(innerParamList, initialType); // fn(T) -> T

			ListType outerParamList = new ListType();
			outerParamList.add(new TypeNat());
			Type expectedStepType = new TypeFun(outerParamList, innerFunType); // fn(Nat) -> (fn(T) -> T)

			typeCheck(expectedStepType, actualStepType, p.expr_3, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			return initialType;
		}

		@Override
		public Type visit(Fold p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Unfold p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(ConstTrue p, TypeCheckContext arg) {
			return new TypeBool();
		}

		@Override
		public Type visit(ConstFalse p, TypeCheckContext arg) {
			return new TypeBool();
		}

		@Override
		public Type visit(ConstUnit p, TypeCheckContext arg) {
			return new TypeUnit();
		}

		@Override
		public Type visit(ConstInt p, TypeCheckContext arg) {
			return new TypeNat();
		}

		@Override
		public Type visit(ConstMemory p, TypeCheckContext arg) {
			if (arg.expectedType == null) {
				throw new TypeCheckError("ERROR_AMBIGUOUS_REFERENCE_TYPE", p);
			}
			if (!(arg.expectedType instanceof TypeRef expectedRef)) {
				throw new TypeCheckError("ERROR_UNEXPECTED_MEMORY_ADDRESS", p);
			}
			return expectedRef;
		}


		@Override
		public Type visit(Var p, TypeCheckContext arg) {
			if (arg.variables.containsKey(p.stellaident_)) {
				return arg.variables.get(p.stellaident_);
			}
			throw new TypeCheckError("ERROR_UNDEFINED_VARIABLE", p);
		}
	}

	public class ReturnTypeVisitor implements ReturnType.Visitor<Type, TypeCheckContext> {

		@Override
		public Type visit(NoReturnType p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(SomeReturnType p, TypeCheckContext arg) {
			return p.type_;
		}
	}

	public void typeCheck(Type expectedType, Type type) {
		if (!expectedType.equals(type)) {
			throw new IllegalArgumentException("ERROR_UNEXPECTED_TYPE_IN_EXPRESSION | Expected: "
					+ PrettyPrinter.print(expectedType) + ", got: " + PrettyPrinter.print(type));
		}
	}

	private TypeCheckContext withExpected(TypeCheckContext base, Type expected) {
		TypeCheckContext c = base.copy();
		c.expectedType = expected;
		return c;
	}


	private void typeCheck(Type expected, Type actual, Expr blame, String tag) {
		Subtype.expectSubtype(actual, expected, blame, "ERROR_UNEXPECTED_SUBTYPE");
	}

	public Type inferType(Type expectedType, Type type) {
		if (expectedType instanceof TypeBottom) {
			return type;
		} else if (type instanceof TypeBottom) {
			return expectedType;
		} else if (expectedType instanceof TypeSum expectedSum && type instanceof TypeSum typeSum) {
			return new TypeSum(
					inferType(expectedSum.type_1, typeSum.type_1),
					inferType(expectedSum.type_2, typeSum.type_2)
			);
		} else if (expectedType instanceof TypeList expectedList && type instanceof TypeList typeList) {
			return new TypeList(
					inferType(expectedList.type_, typeList.type_)
			);
		} else if (expectedType instanceof TypeRecord expectedRecord && type instanceof TypeRecord typeRecord) {
			Map<String, Type> exp = new LinkedHashMap<>();
			for (RecordFieldType rft : expectedRecord.listrecordfieldtype_) {
				ARecordFieldType a = (ARecordFieldType) rft;
				exp.put(a.stellaident_, a.type_);
			}
			Map<String, Type> act = new LinkedHashMap<>();
			for (RecordFieldType rft : typeRecord.listrecordfieldtype_) {
				ARecordFieldType a = (ARecordFieldType) rft;
				act.put(a.stellaident_, a.type_);
			}
			if (!exp.keySet().equals(act.keySet())) {
				throw new IllegalArgumentException("ERROR_UNEXPECTED_TYPE_IN_EXPRESSION | Expected: "
						+ PrettyPrinter.print(expectedType) + ", got: " + PrettyPrinter.print(type));
			}
			ListRecordFieldType merged = new ListRecordFieldType();
			for (RecordFieldType rft : expectedRecord.listrecordfieldtype_) {
				ARecordFieldType a = (ARecordFieldType) rft;
				Type mergedFieldType = inferType(a.type_, act.get(a.stellaident_));
				merged.add(new ARecordFieldType(a.stellaident_, mergedFieldType));
			}
			return new TypeRecord(merged);
		} else if (expectedType instanceof TypeRef expectedRef && type instanceof TypeRef typeRef) {
			return new TypeRef(
					inferType(expectedRef.type_, typeRef.type_)
			);
		} else if (expectedType instanceof TypeFun expectedFun && type instanceof TypeFun typeFun) {
			if (expectedFun.listtype_.size() != typeFun.listtype_.size()) {
				throw new IllegalArgumentException("ERROR_UNEXPECTED_TYPE_IN_EXPRESSION | Expected: "
						+ PrettyPrinter.print(expectedType) + ", got: " + PrettyPrinter.print(type));
			}
			ListType mergedParams = new ListType();
			for (int i = 0; i < expectedFun.listtype_.size(); i++) {
				Type pExp = expectedFun.listtype_.get(i);
				Type pAct = typeFun.listtype_.get(i);
				mergedParams.add(inferType(pExp, pAct));
			}
			Type mergedRet = inferType(expectedFun.type_, typeFun.type_);
			return new TypeFun(mergedParams, mergedRet);
		}

		if (!expectedType.equals(type)) {
			throw new IllegalArgumentException("ERROR_UNEXPECTED_TYPE_IN_EXPRESSION | Expected: "
					+ PrettyPrinter.print(expectedType) + ", got: " + PrettyPrinter.print(type));
		}

		return expectedType;
	}

}
