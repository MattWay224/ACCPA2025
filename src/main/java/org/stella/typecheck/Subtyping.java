// Subtype.java
package org.stella.typecheck;

import org.syntax.stella.Absyn.*;

import java.util.LinkedHashMap;
import java.util.Map;

final class Subtype {
	private Subtype() {
	}

	static boolean isSubtype(Type s, Type t) {
		if (s == null || t == null) return false;
		if (s.equals(t)) return true;

		// Top / Bottom
		if (t instanceof TypeTop) return true;
		if (s instanceof TypeBottom) return true;

		// Lists: covariant
		if (s instanceof TypeList sl && t instanceof TypeList tl) {
			return isSubtype(sl.type_, tl.type_);
		}

		// Functions: contravariant in domain(s), covariant in codomain
		if (s instanceof TypeFun sf && t instanceof TypeFun tf) {
			if (sf.listtype_.size() != tf.listtype_.size()) return false;
			for (int i = 0; i < sf.listtype_.size(); i++) {
				Type sDom = sf.listtype_.get(i);
				Type tDom = tf.listtype_.get(i);
				if (!isSubtype(tDom, sDom)) return false;
			}
			return isSubtype(sf.type_, tf.type_);
		}


		if (s instanceof TypeRecord sr && t instanceof TypeRecord tr) {
			Map<String, Type> sm = recordToMap(sr);
			Map<String, Type> tm = recordToMap(tr);
			for (Map.Entry<String, Type> e : tm.entrySet()) {
				Type sFld = sm.get(e.getKey());
				if (sFld == null) return false;
				if (!isSubtype(sFld, e.getValue())) return false;
			}
			return true;
		}

		// Binary sums
		if (s instanceof TypeSum ss && t instanceof TypeSum ts) {
			return isSubtype(ss.type_1, ts.type_1) && isSubtype(ss.type_2, ts.type_2);
		}

		if (s instanceof TypeVariant sv && t instanceof TypeVariant tv) {
			Map<String, OptionalTyping> sm = variantToMap(sv);
			Map<String, OptionalTyping> tm = variantToMap(tv);
			for (Map.Entry<String, OptionalTyping> e : sm.entrySet()) {
				String lbl = e.getKey();
				OptionalTyping sOpt = e.getValue();
				OptionalTyping tOpt = tm.get(lbl);
				if (tOpt == null) return false;

				if (sOpt instanceof SomeTyping ss && tOpt instanceof SomeTyping ts) {
					if (!isSubtype(ss.type_, ts.type_)) return false;
				} else if ((sOpt instanceof SomeTyping) != (tOpt instanceof SomeTyping)) {
					return false;
				}
			}
			return true;
		}

		// References: invariant
		if (s instanceof TypeRef sr && t instanceof TypeRef tr) {
			return isSubtype(sr.type_, tr.type_) && isSubtype(tr.type_, sr.type_);
		}

		return false;
	}

	static void expectSubtype(Type actual, Type expected, Expr blame, String tag) {
		if (!isSubtype(actual, expected)) {
			throw new TypeCheckError(tag, blame);
		}
	}

	private static Map<String, Type> recordToMap(TypeRecord r) {
		Map<String, Type> m = new LinkedHashMap<>();
		for (RecordFieldType f : r.listrecordfieldtype_) {
			ARecordFieldType a = (ARecordFieldType) f;
			m.put(a.stellaident_, a.type_);
		}
		return m;
	}

	private static Map<String, OptionalTyping> variantToMap(TypeVariant v) {
		Map<String, OptionalTyping> m = new LinkedHashMap<>();
		for (VariantFieldType f : v.listvariantfieldtype_) {
			AVariantFieldType a = (AVariantFieldType) f;
			m.put(a.stellaident_, a.optionaltyping_);
		}
		return m;
	}
}
