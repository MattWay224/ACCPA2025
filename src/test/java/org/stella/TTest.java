package org.stella;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

class TTest {

	private static final Path TEST_DIR = Paths.get("tests", "t");

	// ----------- helpers -----------

	private static Stream<String> stellaFiles(String prefix) throws IOException {
		Path cwd = Paths.get("").toAbsolutePath();
		Path dir = TEST_DIR.toAbsolutePath();

		System.out.println("CWD = " + cwd);
		System.out.println("Looking for tests in = " + dir);

		if (!Files.exists(TEST_DIR) || !Files.isDirectory(TEST_DIR)) {
			throw new IllegalStateException("Test directory not found: " + dir + " (CWD=" + cwd + ")");
		}

		List<String> files = Files.list(TEST_DIR)
				.filter(Files::isRegularFile)
				.map(p -> p.toString().replace('\\', '/'))
				.filter(p -> p.endsWith(".stella"))
				.filter(p -> Paths.get(p).getFileName().toString().startsWith(prefix))
				.sorted()
				.toList();

		if (files.isEmpty()) {
			// Чтобы JUnit не падал PreconditionViolationException, а дал нормальную ошибку
			throw new IllegalStateException("No test files with prefix '" + prefix + "' in " + dir);
		}

		System.out.println("Found " + files.size() + " files for prefix '" + prefix + "'");
		return files.stream();
	}

	static Stream<String> wellTypedFiles() throws IOException {
		return stellaFiles("ok_");
	}

	static Stream<String> illTypedFiles() throws IOException {
		return stellaFiles("err");
	}

	static Stream<String> illmoreTypedFiles() throws IOException {
		return stellaFiles("ERROR");
	}

	// ----------- tests -----------

	@ParameterizedTest(name = "Well-typed: {0}")
	@MethodSource("wellTypedFiles")
	void testWellTyped(String filepath) throws Exception {
		System.out.println("Checking (OK): " + filepath);

		String[] args = new String[0];
		InputStream original = System.in;

		try (FileInputStream fips = new FileInputStream(filepath)) {
			System.setIn(fips);
			assertDoesNotThrow(() -> Main.main(args));
		} finally {
			System.setIn(original);
		}

		System.out.println("PASSED: " + filepath);
		System.out.println("----------------------------------");
	}

	@ParameterizedTest(name = "Ill-typed: {0}")
	@MethodSource("illTypedFiles")
	void testIllTyped(String filepath) throws Exception {
		System.out.println("Checking (ERROR expected): " + filepath);

		String[] args = new String[0];
		InputStream original = System.in;

		try (FileInputStream fips = new FileInputStream(filepath)) {
			System.setIn(fips);

			Exception exception = assertThrows(Exception.class, () -> Main.main(args));
			System.out.println("Type Error Message:");
			System.out.println(exception.getMessage());
		} finally {
			System.setIn(original);
		}

		System.out.println("ERROR TEST PASSED: " + filepath);
		System.out.println("----------------------------------");
	}

	@ParameterizedTest(name = "Ill-typed: {0}")
	@MethodSource("illmoreTypedFiles")
	void testIllTypedd(String filepath) throws Exception {
		System.out.println("Checking (ERROR expected): " + filepath);

		String[] args = new String[0];
		InputStream original = System.in;

		try (FileInputStream fips = new FileInputStream(filepath)) {
			System.setIn(fips);

			Exception exception = assertThrows(Exception.class, () -> Main.main(args));
			System.out.println("Type Error Message:");
			System.out.println(exception.getMessage());
		} finally {
			System.setIn(original);
		}

		System.out.println("ERROR TEST PASSED: " + filepath);
		System.out.println("----------------------------------");
	}
}
