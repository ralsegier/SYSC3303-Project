
public class BlockNumber {
	private byte[] numbers;
	public final static byte MAX = 127;
	
	public BlockNumber (byte[] start) {
		if (start.length > 2 || start.length <= 0) System.exit(1);
		numbers = new byte[2];
		if (start.length == 1) numbers[1] = 0;
		else numbers[1] = start[1];
		
		numbers[0] = start[0];
	}
	
	public BlockNumber () {
		numbers = new byte[2];
		numbers[0] = 0;
		numbers[1] = 0;
	}
	
	public byte[] getNext() {
		if (numbers[0] == MAX) {
			numbers[0] = 0;
			if (numbers[1] == MAX) {
				numbers[1] = 0;
			} else {
				numbers[1]++;
			}
		} else {
			numbers[0]++;
		}
		return numbers;
	}
	
	public void increment() {
		this.numbers = this.getNext();
	}
	
	public boolean compare(byte n[]) {
		if(n.length != numbers.length) return false;
		if(n[0] != numbers[0]) return false;
		if(n[1] != numbers[0]) return false;
		return true;
	}
}
