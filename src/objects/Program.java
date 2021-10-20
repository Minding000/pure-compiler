package objects;

public class Program {
	public final File[] files;

	public Program(String sourceCode) {
		files = new File[]{new File(this, sourceCode)};
	}
}
