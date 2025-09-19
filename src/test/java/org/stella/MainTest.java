package org.stella;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.stella.typecheck.TypeCheckError;

class MainTest {

	@ParameterizedTest(name = "{index} Typechecking well-typed program {0}")
	@ValueSource(strings = {
			"tests/well-typed/factorial.stella",
			"tests/well-typed/squares.stella",
			"tests/well-typed/higher-order-1.stella",
			"tests/well-typed/increment_twice.stella",
			"tests/well-typed/logical-operators.stella"})
	void testWellTyped(String filepath) throws Exception {
		String[] args = new String[0];
		final InputStream original = System.in;
		final FileInputStream fips = new FileInputStream(filepath);
		System.setIn(fips);
		Assertions.assertDoesNotThrow(() -> Main.main(args));
		System.setIn(original);
	}

	@ParameterizedTest(name = "{index} Typechecking ill-typed program {0}")
	@ValueSource(strings = {
			"tests/ill-typed/applying-non-function-1.stella",
			"tests/ill-typed/applying-non-function-2.stella",
			"tests/ill-typed/applying-non-function-3.stella",
			"tests/ill-typed/argument-type-mismatch-1.stella",
			"tests/ill-typed/argument-type-mismatch-2.stella",
			"tests/ill-typed/argument-type-mismatch-3.stella",
			"tests/ill-typed/bad-if-1.stella",
			"tests/ill-typed/bad-if-2.stella",
			"tests/ill-typed/bad-succ-1.stella",
			"tests/ill-typed/bad-succ-2.stella",
			"tests/ill-typed/bad-succ-3.stella",
			"tests/ill-typed/shadowed-variable-1.stella",
			"tests/ill-typed/undefined-variable-1.stella",
			"tests/ill-typed/undefined-variable-2.stella",
			"tests/ill-typed/bad-squares-1.stella",
			"tests/ill-typed/bad-squares-2.stella",
			"tests/public/4.stella",
			"tests/public/5.stella",
			"tests/public/6.stella",
			"tests/public/7.stella",
			"tests/public/8.stella",
			"tests/public/9.stella",
			"tests/public/11.stella",
			"tests/public/15.stella",
			"tests/public/16.stella",
			"tests/public/17.stella",
			"tests/public/18.stella",
			"tests/public/19.stella",
			"tests/public/20.stella",
			"tests/public/22.stella",
			"tests/public/24.stella",
			"tests/public/25.stella",
			"tests/public/26.stella",
			"tests/public/27.stella",
			"tests/public/28.stella",
			"tests/public/29.stella",
			"tests/public/30.stella",
			"tests/public/31.stella",
			"tests/public/32.stella",
			"tests/public/34.stella",
			"tests/public/58.stella",
			"tests/public/60.stella",
			"tests/public/61.stella",
			"tests/public/62.stella",
			"tests/public/64.stella",
			"tests/public/65.stella",
			"tests/public/66.stella",
			"tests/public/67.stella",
			"tests/public/68.stella",
			"tests/public/70.stella",
			"tests/public/72.stella",
			"tests/public/73.stella",
			"tests/public/74.stella",
			"tests/public/75.stella",
			"tests/public/76.stella",
			"tests/public/77.stella",
			"tests/public/78.stella",
			"tests/public/79.stella",
			"tests/public/80.stella",
			"tests/public/81.stella",
			"tests/public/82.stella",
			"tests/public/84.stella",
			"tests/public/85.stella",
			"tests/public/86.stella",
			"tests/public/87.stella",
			"tests/public/88.stella",
			"tests/public/89.stella",
			"tests/public/90.stella",
			"tests/public/91.stella",
			"tests/public/92.stella",
			"tests/public/93.stella",
			"tests/public/94.stella",
			"tests/public/95.stella",
			"tests/public/96.stella",
			"tests/public/97.stella",
			"tests/public/98.stella",
			"tests/public/99.stella",
			"tests/public/100.stella",
			"tests/public/101.stella",
			"tests/public/115.stella",
			"tests/public/116.stella",
			"tests/public/117.stella",
			"tests/public/118.stella",
			"tests/public/53.stella",
			"tests/public/57.stella",
			"tests/public/69.stella",
			"tests/public/102.stella",
			"tests/public/103.stella",
			"tests/public/106.stella",
			"tests/public/12.stella",
			"tests/public/21.stella",
			"tests/public/33.stella",
			"tests/public/36.stella",
			"tests/public/37.stella",
			"tests/public/42.stella",
			"tests/testPupupup/1.stella",
			"tests/testPupupup/7.stella",
			"tests/testPupupup/9.stella",
			"tests/testPupupup/15.stella",
			"tests/testPupupup/17.stella",
			"tests/testPupupup/19.stella",
			"tests/testPupupup/21.stella",
			"tests/testPupupup/23.stella",
			"tests/testPupupup/25.stella",
			"tests/testPupupup/27.stella",
			"tests/testPupupup/29.stella",
			"tests/testPupupup/31.stella",
			"tests/testPupupup/33.stella",
			"tests/testPupupup/35.stella",
			"tests/testPupupup/37.stella",
			"tests/testPupupup/39.stella",
			"tests/testPupupup/43.stella",
			"tests/testPupupup/51.stella",
			"tests/testPupupup/53.stella",
			"tests/testPupupup/57.stella",
			"tests/testPupupup/59.stella",
			"tests/testPupupup/67.stella",
			"tests/testPupupup/69.stella",
			"tests/testPupupup/71.stella",
			"tests/testPupupup/47.stella",

			"tests/public-tests-main-week-3-main-public/1.stella",
			"tests/public-tests-main-week-3-main-public/7.stella",
			"tests/public-tests-main-week-3-main-public/9.stella",
			"tests/public-tests-main-week-3-main-public/17.stella",
			"tests/public-tests-main-week-3-main-public/19.stella",
			"tests/public-tests-main-week-3-main-public/23.stella",
			"tests/public-tests-main-week-3-main-public/21.stella",
			"tests/public-tests-main-week-3-main-public/25.stella",
			"tests/public-tests-main-week-3-main-public/27.stella",
			"tests/public-tests-main-week-3-main-public/29.stella",
			"tests/public-tests-main-week-3-main-public/31.stella",
			"tests/public-tests-main-week-3-main-public/33.stella",
			"tests/public-tests-main-week-3-main-public/35.stella",
			"tests/public-tests-main-week-3-main-public/37.stella",
			"tests/public-tests-main-week-3-main-public/39.stella",

			"tests/public-tests-main-week-3-main-public/43.stella",
			"tests/public-tests-main-week-3-main-public/47.stella",
			"tests/public-tests-main-week-3-main-public/51.stella",
			"tests/public-tests-main-week-3-main-public/53.stella",
			"tests/public-tests-main-week-3-main-public/57.stella",
			"tests/public-tests-main-week-3-main-public/59.stella",
			"tests/public-tests-main-week-3-main-public/63.stella",
			"tests/public-tests-main-week-3-main-public/67.stella",
			"tests/public-tests-main-week-3-main-public/69.stella",
			"tests/public-tests-main-week-3-main-public/71.stella",
			"tests/testPupupup/63.stella",
			"tests/public/63.stella",


			"tests/public-tests-main-week-3-main-secret/4.stella",
			"tests/public-tests-main-week-3-main-secret/6.stella",
			"tests/public-tests-main-week-3-main-secret/8.stella",


			"tests/public-tests-main-week-3-main-secret/12.stella",
			"tests/public-tests-main-week-3-main-secret/14.stella",
			"tests/public-tests-main-week-3-main-secret/16.stella",
			"tests/public-tests-main-week-3-main-secret/18.stella",
			"tests/public-tests-main-week-3-main-secret/20.stella",
			"tests/public-tests-main-week-3-main-secret/24.stella",
			"tests/public-tests-main-week-3-main-secret/26.stella",
			"tests/public-tests-main-week-3-main-secret/28.stella",
			"tests/public-tests-main-week-3-main-secret/30.stella",

			"tests/public-tests-main-week-3-main-secret/32.stella",
			"tests/public-tests-main-week-3-main-secret/34.stella",
			"tests/public-tests-main-week-3-main-secret/36.stella",
			"tests/public-tests-main-week-3-main-secret/38.stella",
			"tests/public-tests-main-week-3-main-secret/40.stella",
			"tests/public-tests-main-week-3-main-secret/42.stella",
			"tests/public-tests-main-week-3-main-secret/44.stella",
			"tests/public-tests-main-week-3-main-secret/46.stella",
			"tests/public-tests-main-week-3-main-secret/48.stella",
			"tests/public-tests-main-week-3-main-secret/50.stella",
			"tests/public-tests-main-week-3-main-secret/52.stella",
			"tests/public-tests-main-week-3-main-secret/54.stella",
			"tests/public-tests-main-week-3-main-secret/58.stella",
			"tests/public-tests-main-week-3-main-secret/60.stella",
			"tests/public-tests-main-week-3-main-secret/62.stella",
			"tests/public-tests-main-week-3-main-secret/64.stella",
			"tests/public-tests-main-week-3-main-secret/66.stella",
			"tests/public-tests-main-week-3-main-secret/68.stella",
			"tests/public-tests-main-week-3-main-secret/70.stella",

			"tests/public-tests-main-week-3-main-secret/74.stella",
			"tests/public-tests-main-week-3-main-secret/76.stella",
	})
	void testIllTyped(String filepath) throws Exception {
		String[] args = new String[0];
		final FileInputStream fips = new FileInputStream(filepath);
		System.setIn(fips);

		// Change Exception class to your specific
		Exception exception = assertThrows(Exception.class, () -> Main.main(args), "Expected the type checker to fail!");
		System.out.println("Type Error: " + exception.getMessage());
	}

	@ParameterizedTest(name = "{index} Typechecking well-typed program {0}")
	@ValueSource(strings = {
			"tests/public-tests-main-week-3-main-public/41.stella",
			"tests/public-tests-main-week-3-main-public/45.stella",
			"tests/public-tests-main-week-3-main-public/49.stella",
			"tests/public-tests-main-week-3-main-public/55.stella",
			"tests/public-tests-main-week-3-main-public/61.stella",

			"tests/public-tests-main-week-3-main-public/65.stella",

			"tests/public-tests-main-week-3-main-public/3.stella",
			"tests/public-tests-main-week-3-main-public/5.stella",
			"tests/public-tests-main-week-3-main-public/13.stella",
			"tests/public-tests-main-week-3-main-public/11.stella",
			"tests/public-tests-main-week-3-main-public/73.stella",

			"tests/public/1.stella",
			"tests/public/104.stella",
			"tests/public/83.stella",
			"tests/public/71.stella",
			"tests/public/3.stella",
			"tests/public/14.stella",
			"tests/public/38.stella",
			"tests/public/39.stella",
			"tests/public/23.stella",
			"tests/public/35.stella",
			"tests/public/40.stella",
			"tests/public/41.stella",
			"tests/public/43.stella",
			"tests/public/44.stella",
			"tests/public/45.stella",
			"tests/public/46.stella",
			"tests/public/47.stella",
			"tests/public/48.stella",
			"tests/public/49.stella",
			"tests/public/50.stella",
			"tests/public/51.stella",
			"tests/public/55.stella",
			"tests/public/56.stella",
			"tests/public/52.stella",
			"tests/public/54.stella",
			"tests/public/59.stella",
			"tests/public/105.stella",
			"tests/public/107.stella",
			"tests/public/108.stella",
			"tests/public/109.stella",
			"tests/public/110.stella",
			"tests/public/111.stella",
			"tests/public/112.stella",
			"tests/public/113.stella",
			"tests/public/114.stella",
			"tests/public/2.stella",
			"tests/public/10.stella",
			"tests/public/13.stella",
			"tests/testPupupup/61.stella",
			"tests/testPupupup/3.stella",
			"tests/testPupupup/5.stella",
			"tests/testPupupup/11.stella",
			"tests/testPupupup/13.stella",
			"tests/testPupupup/41.stella",
			"tests/testPupupup/45.stella",
			"tests/testPupupup/49.stella",
			"tests/testPupupup/55.stella",
			"tests/testPupupup/65.stella",
			"tests/testPupupup/73.stella",

			"tests/public-tests-main-week-3-main-secret/2.stella",
			"tests/public-tests-main-week-3-main-secret/10.stella",
			"tests/public-tests-main-week-3-main-secret/22.stella",
			"tests/public-tests-main-week-3-main-secret/56.stella",
			"tests/public-tests-main-week-3-main-secret/72.stella",

	})
	void testPublicTyped(String filepath) throws Exception {
		String[] args = new String[0];
		final InputStream original = System.in;
		final FileInputStream fips = new FileInputStream(filepath);
		System.setIn(fips);
		Assertions.assertDoesNotThrow(() -> Main.main(args));
		System.setIn(original);
	}

	@ParameterizedTest(name = "{index} Typechecking well-typed program {0}")
	@ValueSource(strings = {

			"tests/week5/10.stella",
			"tests/week5/12.stella",
			"tests/week5/37.stella",
			"tests/week5/39.stella",
			"tests/week5/44.stella",


			"tests/week5/47.stella",
			"tests/week5/48.stella",
			"tests/week5/49.stella",
			"tests/week5/50.stella",
			"tests/week5/53.stella",
			"tests/week5/55.stella",
			"tests/week5/56.stella",
			"tests/week5/57.stella",
			"tests/week5/59.stella",
			"tests/week5/60.stella",
			"tests/week5/61.stella",
			"tests/week5/62.stella",
			"tests/week5/71.stella",
			"tests/week5/72.stella",
			"tests/week5/73.stella",
			"tests/week5/74.stella",
			"tests/week5/75.stella",
			"tests/week5/76.stella",
			"tests/week5/78.stella",
			"tests/week5/79.stella",
			"tests/week5/81.stella",
			"tests/week5/82.stella",
			"tests/week5/83.stella",
			"tests/week5/85.stella",
			"tests/week5/87.stella",
			"tests/week5/88.stella",
			"tests/week5/89.stella",
			"tests/week5/90.stella",
			"tests/week5/91.stella",
			"tests/week5/92.stella",
			"tests/week5/93.stella",
			"tests/week5/94.stella",
			"tests/week5/95.stella",
			"tests/week5/96.stella",
			"tests/week5/97.stella",
			"tests/week5/98.stella",
			"tests/week5/99.stella",
			"tests/week5/101.stella",
			"tests/week5/106.stella",
			"tests/week5/107.stella",
			"tests/week5/110.stella",
			"tests/week5/111.stella",
			"tests/week5/112.stella",
			"tests/week5/113.stella",
			"tests/week5/114.stella",
			"tests/week5/115.stella",
			"tests/week5/116.stella",
			"tests/week5/117.stella",
			"tests/week5/118.stella",
	})
	void testWeek5well(String filepath) throws Exception {
		String[] args = new String[0];
		final InputStream original = System.in;
		final FileInputStream fips = new FileInputStream(filepath);
		System.setIn(fips);
		Assertions.assertDoesNotThrow(() -> Main.main(args));
		System.setIn(original);
	}

	@ParameterizedTest(name = "{index} Typechecking ill-typed program {0}")
	@ValueSource(strings = {
			"tests/week5/1.stella",
			"tests/week5/2.stella",
			"tests/week5/3.stella",
			"tests/week5/4.stella",
			"tests/week5/5.stella",
			"tests/week5/6.stella",
			"tests/week5/7.stella",
			"tests/week5/8.stella",
			"tests/week5/9.stella",

			"tests/week5/11.stella",
			"tests/week5/13.stella",
			"tests/week5/14.stella",
			"tests/week5/15.stella",
			"tests/week5/16.stella",
			"tests/week5/17.stella",
			"tests/week5/18.stella",
			"tests/week5/19.stella",
			"tests/week5/20.stella",
			"tests/week5/21.stella",
			"tests/week5/22.stella",
			"tests/week5/23.stella",
			"tests/week5/24.stella",
			"tests/week5/25.stella",
			"tests/week5/26.stella",
			"tests/week5/27.stella",
			"tests/week5/28.stella",
			"tests/week5/29.stella",
			"tests/week5/30.stella",
			"tests/week5/31.stella",
			"tests/week5/32.stella",
			"tests/week5/33.stella",
			"tests/week5/34.stella",
			"tests/week5/35.stella",
			"tests/week5/36.stella",

			"tests/week5/38.stella",
			"tests/week5/40.stella",
			"tests/week5/41.stella",
			"tests/week5/42.stella",
			"tests/week5/43.stella",
			"tests/week5/45.stella",
			"tests/week5/46.stella",

			"tests/week5/51.stella",
			"tests/week5/52.stella",
			"tests/week5/54.stella",
			"tests/week5/58.stella",
			"tests/week5/63.stella",
			"tests/week5/64.stella",
			"tests/week5/65.stella",
			"tests/week5/66.stella",
			"tests/week5/67.stella",
			"tests/week5/68.stella",
			"tests/week5/69.stella",
			"tests/week5/70.stella",
			"tests/week5/77.stella",
			"tests/week5/80.stella",

			"tests/week5/84.stella",
			"tests/week5/86.stella",
			"tests/week5/86.stella",
			"tests/week5/100.stella",
			"tests/week5/102.stella",
			"tests/week5/103.stella",
			"tests/week5/104.stella",
			"tests/week5/105.stella",
			"tests/week5/108.stella",
			"tests/week5/109.stella",
			"tests/week5/119.stella",
			"tests/week5/120.stella",
			"tests/week5/121.stella",
			"tests/week5/122.stella",
			"tests/week5/123.stella",
			"tests/week5/124.stella",
			"tests/week5/125.stella",
			"tests/week5/126.stella",
			"tests/week5/127.stella",
			"tests/week5/128.stella",
			"tests/week5/129.stella",
			"tests/week5/130.stella",
			"tests/week5/131.stella",
			"tests/week5/132.stella",
			"tests/week5/133.stella",
			"tests/week5/134.stella",
	})
	void testWeek5ill(String filepath) throws Exception {
		String[] args = new String[0];
		final FileInputStream fips = new FileInputStream(filepath);
		System.setIn(fips);

		// Change Exception class to your specific
		Exception exception = assertThrows(Exception.class, () -> Main.main(args), "Expected the type checker to fail!");
		System.out.println("Type Error: " + exception.getMessage());
	}

	@ParameterizedTest(name = "{index} Typechecking well-typed program {0}")
	@ValueSource(strings = {
			"tests/week6/161.stella",
			"tests/week6/173.stella",
			"tests/week6/167.stella",
			"tests/week6/171.stella",
	})
	void testWeek6well(String filepath) throws Exception {
		String[] args = new String[0];
		final InputStream original = System.in;
		final FileInputStream fips = new FileInputStream(filepath);
		System.setIn(fips);
		Assertions.assertDoesNotThrow(() -> Main.main(args));
		System.setIn(original);
	}

	@ParameterizedTest(name = "{index} Typechecking ill-typed program {0}")
	@ValueSource(strings = {

			"tests/week6/141.stella",
			"tests/week6/143.stella",
			"tests/week6/145.stella",
			"tests/week6/163.stella",
			"tests/week6/165.stella",
			"tests/week6/169.stella",
	})
	void testWeek6ill(String filepath) throws Exception {
		String[] args = new String[0];
		final FileInputStream fips = new FileInputStream(filepath);
		System.setIn(fips);

		// Change Exception class to your specific
		Exception exception = assertThrows(Exception.class, () -> Main.main(args), "Expected the type checker to fail!");
		System.out.println("Type Error: " + exception.getMessage());
	}

}
