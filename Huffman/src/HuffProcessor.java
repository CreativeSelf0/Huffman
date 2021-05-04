import java.util.*;
/**
 *	Interface that all compression suites must implement. That is they must be
 *	able to compress a file and also reverse/decompress that process.
 */
public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); // or 256
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;
	public static final int HUFF_COUNTS = HUFF_NUMBER | 2;

	public enum Header{TREE_HEADER, COUNT_HEADER};
	public Header myHeader = Header.TREE_HEADER;
	
	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
	    int[] counts = readForCounts(in);
	    //System.out.println(counts.length);
	    HuffNode root = makeTreeFromCounts(counts);
	    String[] codings = makeCodingsFromTree(root);
	    out.writeBits(BITS_PER_INT, HUFF_NUMBER);
	    writeHeader(root,out);	 
	    in.reset();
	    writeCompressedBits(in,codings,out);
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		int id = in.readBits(BITS_PER_INT);
		if(id!=HUFF_NUMBER&&id!=HUFF_TREE&&id!=HUFF_COUNTS){
			throw new HuffException("The end is nigh!");
		}
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root,in,out);
	}
	
	public void setHeader(Header header) {
        myHeader = header;
        System.out.println("header set to "+myHeader);
    }
	public HuffNode readTreeHeader(BitInputStream in){
		int bit = in.readBits(1);
		if (bit == 0){
			HuffNode left = readTreeHeader(in);
			HuffNode right = readTreeHeader(in);
			HuffNode root = new HuffNode(0,0,left,right);
			return root;
		}
		if(bit == 1){
			int val = in.readBits(9);
			HuffNode leaf = new HuffNode(val,1,null,null);
			return leaf;
		} else 
			return null;
	

	}
	
	public void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out){
		HuffNode temp = root;
		while(true){
			int bit = in.readBits(1);
			if (bit == 0){
				temp = temp.left();
			}
			if (bit == 1) {
				temp = temp.right();
			}
			if (temp.left() == null && temp.right() == null){
				if (temp.value() == PSEUDO_EOF){
					break;
				}else{
					out.writeBits(BITS_PER_WORD, temp.value());
					temp = root;
				}
			}
		}
	}

	public int[] readForCounts(BitInputStream in){
		int[] ret = new int[256];
		for(int i=0; i<256; i++){
			ret[i] = 0;
		}
		while (true){
            int val = in.readBits(BITS_PER_WORD);
            if (val == -1) break;
            ret[val]++;          
        }
		return ret;
	}
	public HuffNode makeTreeFromCounts(int[] Counts){
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for(int i=0; i<Counts.length; i++){
			if(Counts[i]>0){
				pq.add(new HuffNode(i,Counts[i]));
			}
		}
		pq.add(new HuffNode(PSEUDO_EOF,1));
		while (pq.size() > 1) {
		    HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    HuffNode t = new HuffNode(-1,
		                 left.weight() + right.weight(),
		                 left,right);
		    pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}
	public String[] makeCodingsFromTree(HuffNode root){
		String[] codings = new String[257];
		makeCodingsHelper(root,"",codings);
	    return codings;		
	}
	public void makeCodingsHelper(HuffNode root, String s, String[] codings){
        if(root.left()==null&&root.right()==null){
			codings[root.value()]=s;
			return;
		}
        makeCodingsHelper(root.left(), s+"0", codings);
        makeCodingsHelper(root.right(), s+"1", codings);        
	}
	
	public void writeHeader(HuffNode root, BitOutputStream out){		
		if(root.left()==null&&root.right()==null){
			out.writeBits(1,1);
			out.writeBits(9,root.value());
		}
		else{
			out.writeBits(1,0);
			writeHeader(root.left(),out);
			writeHeader(root.right(),out);
		}
	}

	public void writeCompressedBits(BitInputStream in, String[] codings, BitOutputStream out){
		while(true){
			int val = in.readBits(BITS_PER_WORD);
			if (val==-1) break;
			out.writeBits(codings[val].length(),Integer.parseInt(codings[val], 2));			
		}
		out.writeBits(codings[PSEUDO_EOF].length(), Integer.parseInt(codings[PSEUDO_EOF], 2));
	}
	
}