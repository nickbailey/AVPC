/**
   A convenience class to pass hints about the behavior of a
   scroll bar (JScrollBar component).
**/

public class ScrollHints {
	public int min;
	public int max;
	public int value;
	public int extent;

	public ScrollHints() { this(0, 0, 0, 0); }

	public ScrollHints(int mn, int mx, int val, int ext) {
		min = mn; max = mx; value = val; extent = ext;
	}
}
