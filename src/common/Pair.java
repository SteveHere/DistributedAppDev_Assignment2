package common;

public class Pair<L,R> {

	private final L left;
	private final R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public L getLeft() { return left; }
	public R getRight() { return right; }

	@Override
	public int hashCode() { return left.hashCode() ^ right.hashCode(); }

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Pair)) return false;
		Pair<?, ?> pairToCompare = (Pair<?, ?>) object;
		return this.left.equals(pairToCompare.getLeft()) &&
				this.right.equals(pairToCompare.getRight());
	}
	
	@Override
	public String toString() { return left.toString() + "~" + right.toString(); }
	
	public boolean isEmpty() { return left == null && right == null; }
	
	public boolean isFull() { return left != null && right != null; }
	
	public boolean contains(Object object) { return left.equals(object) || right.equals(object); }

}