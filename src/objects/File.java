package objects;

public class File {
	public final Program program;
	public final Line[] lines;

	public File(Program program, String content) {
		this.program = program;
		final String[] rawLines = content.split("\n");
		lines = new Line[rawLines.length];
		for(int i = 0; i < lines.length; i++) {
			lines[i] = new Line(this, i + 1, rawLines[i]);
		}
	}
}
