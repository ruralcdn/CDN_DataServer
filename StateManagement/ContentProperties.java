package StateManagement;

import java.util.BitSet;

public class ContentProperties {
	public int curSeg ;
	public int totalSeg ;
	public BitSet bitmap ;
	
	public ContentProperties(int cur, int size){
		curSeg = cur ;
		totalSeg = size ;
		bitmap = new BitSet(size);
	}

}
