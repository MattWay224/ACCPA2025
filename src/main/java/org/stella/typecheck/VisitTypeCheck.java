package org.stella.typecheck;

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

			if (p.listdecl_ != null) {
				for (Decl decl : p.listdecl_) {
					if (!(decl instanceof DeclFun fun)) continue;
					TypeVisitor tv = new TypeVisitor();

					if (fun.returntype_ instanceof SomeReturnType srt) {
						srt.type_.accept(tv, null);
					}

					if (fun.listparamdecl_ != null) {
						for (ParamDecl pd : fun.listparamdecl_) {
							AParamDecl ap = (AParamDecl) pd;
							ap.type_.accept(tv, null);
						}
					}
					Type ret = null;
					if (fun.returntype_ instanceof SomeReturnType srt) {
						ret = srt.type_;
					}

					ListType paramTypes = new ListType();
					if (fun.listparamdecl_ != null) {
						for (ParamDecl pd : fun.listparamdecl_) {
							AParamDecl ap = (AParamDecl) pd;
							paramTypes.add(ap.type_);
						}
					}

					context.variables.put(fun.stellaident_, new TypeFun(paramTypes, ret));
				}
			}

			if (!context.variables.containsKey("main")) {
				throw new TypeCheckError("ERROR_MISSING_MAIN");
			}

			if (p.listdecl_ != null) {
				DeclVisitor dv = new DeclVisitor();
				for (Decl decl : p.listdecl_) {
					decl.accept(dv, context);
				}
			}

			return null;
		}
	}


	public class DeclVisitor implements Decl.Visitor<Void, TypeCheckContext> {

		@Override
		public Void visit(DeclFun p, TypeCheckContext arg) {
			SomeReturnType someRet = (SomeReturnType) p.returntype_;
			Type declaredReturn = someRet.type_;

			TypeCheckContext funCtx = arg.copy();
			funCtx.expectedType = declaredReturn;

			if (p.listparamdecl_ != null) {
				for (ParamDecl pd : p.listparamdecl_) {
					AParamDecl a = (AParamDecl) pd;
					funCtx.variables.put(a.stellaident_, a.type_);
				}
			}

			if (p.listdecl_ != null) {
				for (Decl d : p.listdecl_) {
					if (d instanceof DeclFun f) {
						Type ret = null;
						if (f.returntype_ instanceof SomeReturnType srt) {
							ret = srt.type_;
						}

						ListType paramTypes = new ListType();
						if (f.listparamdecl_ != null) {
							for (ParamDecl pd : f.listparamdecl_) {
								AParamDecl ap = (AParamDecl) pd;
								paramTypes.add(ap.type_);
							}
						}
						arg.variables.put(f.stellaident_, new TypeFun(paramTypes, ret));
					}
				}
			}

			if (p.listdecl_ != null) {
				for (Decl d : p.listdecl_) {
					d.accept(this, funCtx);
				}
			}
			Type bodyType = p.expr_.accept(new ExprVisitor(), funCtx);
			typeCheck(declaredReturn, bodyType, p.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
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
			checkType(p.expr_1, new TypeBool(), arg);

			if (arg.expectedType != null) {
				checkType(p.expr_2, arg.expectedType, arg);
				checkType(p.expr_3, arg.expectedType, arg);
				return arg.expectedType;
			}

			boolean thenPanic = (p.expr_2 instanceof org.syntax.stella.Absyn.Panic);
			boolean elsePanic = (p.expr_3 instanceof org.syntax.stella.Absyn.Panic);

			if (thenPanic && !elsePanic) {
				Type t = inferType(p.expr_3, arg);

				if (t instanceof TypeFun) {
					throw new TypeCheckError("ERROR_AMBIGUOUS_PANIC_TYPE", p.expr_2);
				}

				checkType(p.expr_2, t, arg);
				return t;
			}

			if (!thenPanic && elsePanic) {
				Type t = inferType(p.expr_2, arg);

				if (t instanceof TypeFun) {
					throw new TypeCheckError("ERROR_AMBIGUOUS_PANIC_TYPE", p.expr_3);
				}

				checkType(p.expr_3, t, arg);
				return t;
			}

			Type tThen = inferType(p.expr_2, arg);
			checkType(p.expr_3, tThen, arg);
			return tThen;
		}


		@Override
		public Type visit(Let p, TypeCheckContext arg) {
			TypeCheckContext saved = arg.copy();
			for (PatternBinding pb : p.listpatternbinding_) {
				APatternBinding binding = (APatternBinding) pb;
				if (!(binding.pattern_ instanceof PatternVar patVar)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", binding.pattern_);
				}
				arg.variables.put(patVar.stellaident_, inferType(binding.expr_, arg));
			}

			Type result = inferType(p.expr_, arg);

			arg.variables.clear();
			arg.variables.putAll(saved.variables);
			arg.expectedType = saved.expectedType;
			arg.exceptionType = saved.exceptionType;

			return result;
		}

		@Override
		public Type visit(LetRec p, TypeCheckContext arg) {
			TypeCheckContext saved = arg.copy();
			for (PatternBinding pb : p.listpatternbinding_) {
				APatternBinding binding = (APatternBinding) pb;
				if (!(binding.pattern_ instanceof PatternVar patVar)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", binding.pattern_);
				}

				Type declared;
				if (binding.expr_ instanceof TypeAsc ta) {
					ta.type_.accept(new TypeVisitor(), null);
					declared = ta.type_;
				} else {
					declared = inferType(binding.expr_, arg);
				}
				arg.variables.put(patVar.stellaident_, declared);
			}

			for (PatternBinding pb : p.listpatternbinding_) {
				APatternBinding binding = (APatternBinding) pb;
				PatternVar patVar = (PatternVar) binding.pattern_;
				Type expected = arg.variables.get(patVar.stellaident_);

				if (binding.expr_ instanceof TypeAsc ta) {
					checkType(ta.expr_, expected, arg);
				} else {
					checkType(binding.expr_, expected, arg);
				}
			}

			Type result = inferType(p.expr_, arg);

			arg.variables.clear();
			arg.variables.putAll(saved.variables);
			arg.expectedType = saved.expectedType;
			arg.exceptionType = saved.exceptionType;

			return result;
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
			p.type_.accept(new TypeVisitor(), null);
			arg.expectedType = p.type_;
			Type actualType = p.expr_.accept(this, arg);
			typeCheck(p.type_, actualType, p.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			return p.type_;
		}

		@Override
		public Type visit(TypeCast p, TypeCheckContext arg) {
			p.type_.accept(new TypeVisitor(), null);
			Type fromT = p.expr_.accept(this, arg.copy());
			Type toT = p.type_;
			if (arg.expectedType != null) {
				typeCheck(arg.expectedType, toT, p, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
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
					if (!typesEqual(declared, expectedParam)) {
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
			if (arg.expectedType == null) {
				throw new TypeCheckError("ERROR_AMBIGUOUS_VARIANT_TYPE", p);
			}

			if (!(arg.expectedType instanceof TypeVariant expected)) {
				throw new TypeCheckError("ERROR_UNEXPECTED_VARIANT", p);
			}

			OptionalTyping slot = null;
			for (VariantFieldType vft : expected.listvariantfieldtype_) {
				AVariantFieldType a = (AVariantFieldType) vft;
				if (a.stellaident_.equals(p.stellaident_)) {
					slot = a.optionaltyping_;
					break;
				}
			}
			if (slot == null) {
				throw new TypeCheckError("ERROR_UNEXPECTED_VARIANT_LABEL", p);
			}

			boolean hasData = p.exprdata_ instanceof SomeExprData;
			boolean expectsData = slot instanceof SomeTyping;

			if (hasData && !expectsData) {
				throw new TypeCheckError("ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL", p);
			}
			if (!hasData && expectsData) {
				throw new TypeCheckError("ERROR_MISSING_DATA_FOR_LABEL", p);
			}

			if (hasData) {
				SomeExprData sed = (SomeExprData) p.exprdata_;
				SomeTyping st = (SomeTyping) slot;

				// checkType(payloadExpr, expectedPayloadType)
				Type inner = sed.expr_.accept(this, withExpected(arg, st.type_));
				typeCheck(st.type_, inner, sed.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
			}

			return expected;
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
				checkPattern(cs.pattern_, exprType, branchCtx);

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
					if (!typesEqual(arg.expectedType, branchType)) {
						throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", cs.expr_);
					}
				} else if (resultType == null) {
					resultType = branchType;
				} else if (!typesEqual(resultType, branchType)) {
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

		private void checkPattern(Pattern ptn, Type expected, TypeCheckContext arg) {
			if (ptn instanceof PatternVar pv) {
				arg.variables.put(pv.stellaident_, expected);
				return;
			}

			if (ptn instanceof PatternInl pil) {
				if (!(expected instanceof TypeSum sum)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
				}
				if (pil.pattern_ instanceof PatternVar pv) {
					arg.variables.put(pv.stellaident_, sum.type_1);
				} else if (!(pil.pattern_ instanceof PatternUnit)) {
					checkPattern(pil.pattern_, sum.type_1, arg);
				}
				return;
			}

			if (ptn instanceof PatternInr pir) {
				if (!(expected instanceof TypeSum sum)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
				}
				if (pir.pattern_ instanceof PatternVar pv) {
					arg.variables.put(pv.stellaident_, sum.type_2);
				} else if (!(pir.pattern_ instanceof PatternUnit)) {
					checkPattern(pir.pattern_, sum.type_2, arg);
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
					arg.variables.put(pv1.stellaident_, listT.type_);
				} else {
					checkPattern(pc.pattern_1, listT.type_, arg);
				}
				// tail
				if (pc.pattern_2 instanceof PatternVar pv2) {
					arg.variables.put(pv2.stellaident_, new TypeList(listT.type_));
				} else {
					checkPattern(pc.pattern_2, new TypeList(listT.type_), arg);
				}
				return;
			}

			// variants
			if (ptn instanceof PatternVariant pv) {
				if (!(expected instanceof TypeVariant tv)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
				}

				String label = pv.stellaident_;
				AVariantFieldType field = null;
				for (VariantFieldType vft : tv.listvariantfieldtype_) {
					AVariantFieldType a = (AVariantFieldType) vft;
					if (a.stellaident_.equals(label)) {
						field = a;
						break;
					}
				}
				if (field == null) {
					throw new TypeCheckError("ERROR_UNEXPECTED_VARIANT_LABEL", ptn);
				}

				boolean hasPayloadType = field.optionaltyping_ instanceof SomeTyping;
				boolean hasData = pv.patterndata_ instanceof SomePatternData;

				if (hasData && !hasPayloadType) {
					throw new TypeCheckError("ERROR_UNEXPECTED_NON_NULLARY_VARIANT_PATTERN", ptn);
				}
				if (!hasData && hasPayloadType) {
					throw new TypeCheckError("ERROR_UNEXPECTED_NULLARY_VARIANT_PATTERN", ptn);
				}

				if (hasData) {
					Type payloadT = ((SomeTyping) field.optionaltyping_).type_;
					Pattern inner = ((SomePatternData) pv.patterndata_).pattern_;
					checkPattern(inner, payloadT, arg);
				}
				return;
			}

			// tuples
			if (ptn instanceof PatternTuple pt) {
				if (!(expected instanceof TypeTuple tt)) {
					throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
				}
				if (pt.listpattern_.size() != tt.listtype_.size()) {
					throw new TypeCheckError("ERROR_UNEXPECTED_TUPLE_LENGTH", ptn);
				}
				for (int i = 0; i < pt.listpattern_.size(); i++) {
					checkPattern(pt.listpattern_.get(i), tt.listtype_.get(i), arg);
				}
				return;
			}

			throw new TypeCheckError("ERROR_UNEXPECTED_PATTERN_FOR_TYPE", ptn);
		}

		@Override
		public Type visit(List p, TypeCheckContext arg) {
			if (arg.expectedType != null && !(arg.expectedType instanceof TypeList)) {
				throw new TypeCheckError("ERROR_UNEXPECTED_LIST", p);
			}

			if (arg.expectedType instanceof TypeList expectedList) {
				for (Expr e : p.listexpr_) {
					checkType(e, expectedList.type_, arg);
				}
				return expectedList;
			}

			if (p.listexpr_.isEmpty()) {
				throw new TypeCheckError("ERROR_AMBIGUOUS_LIST_TYPE", p);
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
				typeCheck(typeFun.listtype_.get(i), actualType, p.listexpr_.get(i),
						"ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
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
			int index = p.integer_ - 1;

			if (index < 0 || index >= tupleType.listtype_.size()) {
				throw new TypeCheckError("ERROR_TUPLE_INDEX_OUT_OF_BOUNDS", p);
			}

			return tupleType.listtype_.get(index);
		}

		@Override
		public Type visit(Tuple p, TypeCheckContext arg) {
			if (arg.expectedType instanceof TypeTuple expected) {
				if (p.listexpr_.size() != expected.listtype_.size()) {
					throw new TypeCheckError("ERROR_UNEXPECTED_TUPLE_LENGTH", p);
				}
				ListType out = new ListType();
				for (int i = 0; i < p.listexpr_.size(); i++) {
					Expr e = p.listexpr_.get(i);
					TypeCheckContext ec = arg.copy();
					ec.expectedType = expected.listtype_.get(i);
					Type et = e.accept(this, ec);
					typeCheck(expected.listtype_.get(i), et, e, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
					out.add(expected.listtype_.get(i));
				}
				return new TypeTuple(out);
			}

			ListType types = new ListType();
			for (Expr e : p.listexpr_) {
				types.add(e.accept(this, new TypeCheckContext(arg.variables, arg.exceptionType)));
			}
			return new TypeTuple(types);
		}

		@Override
		public Type visit(Record p, TypeCheckContext arg) {
			if (arg.expectedType != null && !(arg.expectedType instanceof TypeRecord)) {
				throw new TypeCheckError("ERROR_UNEXPECTED_RECORD", p);
			}

			Set<String> seenFields = new HashSet<>();
			for (Binding b : p.listbinding_) {
				ABinding ab = (ABinding) b;
				if (!seenFields.add(ab.stellaident_)) {
					throw new TypeCheckError("ERROR_DUPLICATE_RECORD_FIELDS", ab);
				}
			}

			TypeRecord expectedRec = (arg.expectedType instanceof TypeRecord tr) ? tr : null;

			if (expectedRec != null) {
				// unexpected fields
				for (String fieldName : seenFields) {
					boolean found = false;
					for (RecordFieldType rft : expectedRec.listrecordfieldtype_) {
						ARecordFieldType rf = (ARecordFieldType) rft;
						if (rf.stellaident_.equals(fieldName)) {
							found = true;
							break;
						}
					}
					if (!found) {
						throw new TypeCheckError("ERROR_UNEXPECTED_RECORD_FIELDS", p);
					}
				}

				// missing fields
				for (RecordFieldType rft : expectedRec.listrecordfieldtype_) {
					ARecordFieldType rf = (ARecordFieldType) rft;
					if (!seenFields.contains(rf.stellaident_)) {
						throw new TypeCheckError("ERROR_MISSING_RECORD_FIELDS", p);
					}
				}

				ListRecordFieldType out = new ListRecordFieldType();
				for (RecordFieldType rft : expectedRec.listrecordfieldtype_) {
					ARecordFieldType rf = (ARecordFieldType) rft;
					for (Binding b : p.listbinding_) {
						ABinding ab = (ABinding) b;
						if (rf.stellaident_.equals(ab.stellaident_)) {
							Type fieldType = ab.expr_.accept(this, withExpected(arg, rf.type_));
							typeCheck(rf.type_, fieldType, ab.expr_, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
							out.add(new ARecordFieldType(ab.stellaident_, rf.type_));
							break;
						}
					}
				}
				return new TypeRecord(out);
			}

			ListRecordFieldType lr = new ListRecordFieldType();
			for (Binding b : p.listbinding_) {
				ABinding ab = (ABinding) b;
				Type t = ab.expr_.accept(this, arg.copy());
				lr.add(new ARecordFieldType(ab.stellaident_, t));
			}
			return new TypeRecord(lr);
		}


		@Override
		public Type visit(ConsList p, TypeCheckContext arg) {
			if (arg.expectedType != null && !(arg.expectedType instanceof TypeList)) {
				throw new TypeCheckError("ERROR_UNEXPECTED_LIST", p);
			}

			if (arg.expectedType instanceof TypeList expectedList) {
				checkType(p.expr_1, expectedList.type_, arg);
				checkType(p.expr_2, expectedList, arg);
				return expectedList;
			}

			Type headT = inferType(p.expr_1, arg);
			Type listT = new TypeList(headT);
			checkType(p.expr_2, listT, arg);
			return listT;
		}

		private TypeCheckContext withExpected(TypeCheckContext base, Type expected) {
			TypeCheckContext c = base.copy();
			c.expectedType = expected;
			return c;
		}

		private Type inferType(Expr expr, TypeCheckContext arg) {
			Type savedExpected = arg.expectedType;
			arg.expectedType = null;
			Type result = expr.accept(this, arg);
			arg.expectedType = savedExpected;
			return result;
		}

		private void checkType(Expr expr, Type expected, TypeCheckContext arg) {
			Type actual = expr.accept(this, withExpected(arg, expected));
			typeCheck(expected, actual, expr, "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
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
			if (arg.expectedType != null) {
				return arg.expectedType;
			}
			throw new TypeCheckError("ERROR_AMBIGUOUS_PANIC_TYPE", p);
		}

		public Type visit(Throw p, TypeCheckContext arg) {
			if (arg.exceptionType == null) {
				throw new TypeCheckError("ERROR_EXCEPTION_TYPE_NOT_DECLARED", p);
			}

			checkType(p.expr_, arg.exceptionType, arg);

			if (arg.expectedType != null) {
				return arg.expectedType;
			}
			throw new TypeCheckError("ERROR_AMBIGUOUS_THROW_TYPE", p);
		}


		@Override
		public Type visit(TryCatch p, TypeCheckContext arg) {
			TypeCheckContext saved = arg.copy();

			if (p.pattern_ instanceof PatternVar pv) {
				if (arg.exceptionType == null) {
					throw new TypeCheckError("ERROR_EXCEPTION_TYPE_NOT_DECLARED", p);
				}
				arg.variables.put(pv.stellaident_, arg.exceptionType);
			}

			try {
				if (arg.expectedType != null) {
					checkType(p.expr_1, arg.expectedType, saved);
					checkType(p.expr_2, arg.expectedType, arg);
					return arg.expectedType;
				}

				Type catchType = inferType(p.expr_2, arg);
				checkType(p.expr_1, catchType, saved);
				return catchType;
			} finally {
				arg.variables.clear();
				arg.variables.putAll(saved.variables);
				arg.expectedType = saved.expectedType;
				arg.exceptionType = saved.exceptionType;
			}
		}

		@Override
		public Type visit(TryWith p, TypeCheckContext arg) {
			TypeCheckContext saved = arg.copy();

			if (arg.expectedType != null) {
				checkType(p.expr_1, arg.expectedType, saved);
				checkType(p.expr_2, arg.expectedType, saved);
				return arg.expectedType;
			}

			Type withType = inferType(p.expr_2, saved);
			checkType(p.expr_1, withType, saved);
			return withType;
		}

		@Override
		public Type visit(TryCastAs p, TypeCheckContext arg) {
			return null;
		}

		@Override
		public Type visit(Inl p, TypeCheckContext arg) {
			if (arg.expectedType == null) throw new TypeCheckError("ERROR_AMBIGUOUS_SUM_TYPE", p);
			if (!(arg.expectedType instanceof TypeSum sum)) throw new TypeCheckError("ERROR_UNEXPECTED_INJECTION", p);
			checkType(p.expr_, sum.type_1, arg);
			return sum;
		}

		@Override
		public Type visit(Inr p, TypeCheckContext arg) {
			if (arg.expectedType == null) throw new TypeCheckError("ERROR_AMBIGUOUS_SUM_TYPE", p);
			if (!(arg.expectedType instanceof TypeSum sum)) throw new TypeCheckError("ERROR_UNEXPECTED_INJECTION", p);
			checkType(p.expr_, sum.type_2, arg);
			return sum;
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
			Type tf = inferType(p.expr_, arg);

			if (!(tf instanceof TypeFun fun) || fun.listtype_.size() != 1) {
				throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", p.expr_);
			}

			Type t1 = fun.listtype_.get(0);
			Type t2 = fun.type_;
			if (!typesEqual(t1, t2)) {
				throw new TypeCheckError("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION", p.expr_);
			}

			return t1;
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

	private boolean typesEqual(Type a, Type b) {
		if (a == b) return true;
		if (a == null || b == null) return false;

		if (a.getClass() != b.getClass()) return false;
		if (a instanceof TypeBool) return true;
		if (a instanceof TypeNat) return true;
		if (a instanceof TypeUnit) return true;
		if (a instanceof TypeTop) return true;
		if (a instanceof TypeBottom) return true;
		if (a instanceof TypeRef ar && b instanceof TypeRef br) {
			return typesEqual(ar.type_, br.type_);
		}
		if (a instanceof TypeList al && b instanceof TypeList bl) {
			return typesEqual(al.type_, bl.type_);
		}
		if (a instanceof TypeSum as && b instanceof TypeSum bs) {
			return typesEqual(as.type_1, bs.type_1) && typesEqual(as.type_2, bs.type_2);
		}
		if (a instanceof TypeTuple at && b instanceof TypeTuple bt) {
			if (at.listtype_.size() != bt.listtype_.size()) return false;
			for (int i = 0; i < at.listtype_.size(); i++) {
				if (!typesEqual(at.listtype_.get(i), bt.listtype_.get(i))) return false;
			}
			return true;
		}
		if (a instanceof TypeFun af && b instanceof TypeFun bf) {
			if (af.listtype_.size() != bf.listtype_.size()) return false;
			for (int i = 0; i < af.listtype_.size(); i++) {
				if (!typesEqual(af.listtype_.get(i), bf.listtype_.get(i))) return false;
			}
			return typesEqual(af.type_, bf.type_);
		}
		if (a instanceof TypeRecord ar && b instanceof TypeRecord br) {
			Map<String, Type> am = new LinkedHashMap<>();
			for (RecordFieldType rft : ar.listrecordfieldtype_) {
				ARecordFieldType x = (ARecordFieldType) rft;
				am.put(x.stellaident_, x.type_);
			}
			Map<String, Type> bm = new LinkedHashMap<>();
			for (RecordFieldType rft : br.listrecordfieldtype_) {
				ARecordFieldType x = (ARecordFieldType) rft;
				bm.put(x.stellaident_, x.type_);
			}
			if (!am.keySet().equals(bm.keySet())) return false;
			for (String k : am.keySet()) {
				if (!typesEqual(am.get(k), bm.get(k))) return false;
			}
			return true;
		}
		if (a instanceof TypeVariant av && b instanceof TypeVariant bv) {
			Map<String, Type> am = new LinkedHashMap<>();
			for (VariantFieldType vft : av.listvariantfieldtype_) {
				AVariantFieldType x = (AVariantFieldType) vft;
				Type t = ((SomeTyping) x.optionaltyping_).type_;
				am.put(x.stellaident_, t);
			}
			Map<String, Type> bm = new LinkedHashMap<>();
			for (VariantFieldType vft : bv.listvariantfieldtype_) {
				AVariantFieldType x = (AVariantFieldType) vft;
				Type t = ((SomeTyping) x.optionaltyping_).type_;
				bm.put(x.stellaident_, t);
			}
			if (!am.keySet().equals(bm.keySet())) return false;
			for (String k : am.keySet()) {
				if (!typesEqual(am.get(k), bm.get(k))) return false;
			}
			return true;
		}
		return a.equals(b);
	}

	public class TypeVisitor implements Type.Visitor<Void, Void> {

		@Override
		public Void visit(TypeBool p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeNat p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeUnit p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeTop p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeBottom p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeRef p, Void arg) {
			if (p.type_ != null) p.type_.accept(this, null);
			return null;
		}

		@Override
		public Void visit(TypeVar p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeList p, Void arg) {
			if (p.type_ != null) p.type_.accept(this, null);
			return null;
		}

		@Override
		public Void visit(TypeSum p, Void arg) {
			if (p.type_1 != null) p.type_1.accept(this, null);
			if (p.type_2 != null) p.type_2.accept(this, null);
			return null;
		}

		@Override
		public Void visit(TypeTuple p, Void arg) {
			for (Type t : p.listtype_) t.accept(this, null);
			return null;
		}

		@Override
		public Void visit(TypeAuto p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeFun p, Void arg) {
			for (Type t : p.listtype_) t.accept(this, null);
			if (p.type_ != null) p.type_.accept(this, null);
			return null;
		}

		@Override
		public Void visit(TypeForAll p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeRec p, Void arg) {
			return null;
		}

		@Override
		public Void visit(TypeRecord p, Void arg) {
			Set<String> seen = new HashSet<>();
			for (RecordFieldType rft : p.listrecordfieldtype_) {
				ARecordFieldType rf = (ARecordFieldType) rft;
				if (!seen.add(rf.stellaident_)) {
					throw new TypeCheckError("ERROR_DUPLICATE_RECORD_TYPE_FIELDS", p);
				}
				rf.type_.accept(this, null);
			}
			return null;
		}

		@Override
		public Void visit(TypeVariant p, Void arg) {
			Set<String> seen = new HashSet<>();
			for (VariantFieldType vft : p.listvariantfieldtype_) {
				AVariantFieldType vf = (AVariantFieldType) vft;
				if (!seen.add(vf.stellaident_)) {
					throw new TypeCheckError("ERROR_DUPLICATE_VARIANT_TYPE_FIELDS", p);
				}
				if (vf.optionaltyping_ instanceof SomeTyping st) {
					st.type_.accept(this, null);
				}
			}
			return null;
		}
	}


	private void typeCheck(Type expected, Type actual, Expr blame, String tag) {

		if (expected instanceof TypeBottom) {
			if (!(actual instanceof TypeBottom)) {
				throw new TypeCheckError(tag, blame);
			}
			return;
		}

		if (expected instanceof TypeVariant ev && actual instanceof TypeVariant av) {
			Set<String> expLabels = new LinkedHashSet<>();
			for (VariantFieldType vft : ev.listvariantfieldtype_) {
				AVariantFieldType x = (AVariantFieldType) vft;
				expLabels.add(x.stellaident_);
			}
			Set<String> actLabels = new LinkedHashSet<>();
			for (VariantFieldType vft : av.listvariantfieldtype_) {
				AVariantFieldType x = (AVariantFieldType) vft;
				actLabels.add(x.stellaident_);
			}
			for (String lab : expLabels) {
				if (!actLabels.contains(lab)) {
					throw new TypeCheckError("ERROR_MISSING_VARIANT_LABELS | Missing label: " + lab, blame);
				}
			}
		}

		if (!typesEqual(expected, actual)) {
			throw new TypeCheckError(tag + " | Expected: " + PrettyPrinter.print(expected)
					+ ", got: " + PrettyPrinter.print(actual), blame);
		}
	}
}