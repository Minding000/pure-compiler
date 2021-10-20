package code;

import objects.Program;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Main {

	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Nothing to compile.");
			return;
		}
		if(args.length > 1) {
			System.out.println("To many arguments (only 1 file required).");
			return;
		}
		final File sourceFile = new File(args[0]);
		final String sourceFileName = sourceFile.getName();
		if(!sourceFileName.endsWith(".pure")) {
			System.err.println("Wrong extension: The provided file is not PURE source code.");
			return;
		}
		final String sourceCode;
		try {
			sourceCode = Files.readString(sourceFile.toPath());
		} catch(IOException e) {
			System.err.println("Failed to read source file.");
			e.printStackTrace();
			return;
		}
		final Program program = new Program(sourceCode);
	}
}
