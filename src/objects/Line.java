package objects;

public class Line {
	public final File file;
	public final int number;
	public final String content;

	public Line(File file, int number, String content) {
		this.file = file;
		this.number = number;
		this.content = content;
	}
}
