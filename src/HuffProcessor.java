import java.util.PriorityQueue;

public class HuffProcessor implements Processor {
	String[] pathArray;

	public void compress(BitInputStream in, BitOutputStream out) {
		/*
		 * Count the character frequencies Build a Huffman tree using HuffNodes
		 * and a PriorityQueue Extract the codes from the tree Write the
		 * HUFF_NUMBER Write the header using a pre-order traversal of the tree
		 * Write the body of the file using the extracted codes Write the
		 * PSEUDO_EOF
		 */
		int[] FrequencyArray = new int[ALPH_SIZE]; // store the frequencies
		pathArray = new String[ALPH_SIZE + 1]; // store the paths
		// count characters in file
		int RB = in.readBits(BITS_PER_WORD);
		while (RB != -1) {
			FrequencyArray[RB] += 1;
			RB = in.readBits(BITS_PER_WORD);
		}
		in.reset();
		// create the Huffman tree
		PriorityQueue<HuffNode> myPQ = new PriorityQueue<HuffNode>();
		for (int i = 0; i < ALPH_SIZE; i++) {
			if (FrequencyArray[i] != 0) {
				myPQ.add(new HuffNode(i, FrequencyArray[i]));
			}
		}
		// add PSEUDO_EOF node
		myPQ.add(new HuffNode(PSEUDO_EOF, 0));
		// Combine the two smallest nodes into a new node and add to priority
		// queue
		while (myPQ.size() > 1) {
			HuffNode left = myPQ.poll();
			int leftWeight = left.weight();
			HuffNode right = myPQ.poll();
			int rightWeight = right.weight();
			myPQ.add(new HuffNode(-1, leftWeight + rightWeight, left, right));
		}

		String path = "";
		out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		// traverse tree and extract codes
		extractCodes(myPQ.peek(), path);
		// write header
		writeHeader(myPQ.peek(), out);
		// compress/ write the body
		RB = in.readBits(BITS_PER_WORD);
		String code = "";
		while (RB != -1) {
			code = pathArray[RB];
			out.writeBits(code.length(), Integer.parseInt(code, 2));
			RB = in.readBits(BITS_PER_WORD);
		}
		in.reset();

		code = pathArray[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code, 2));

	}

	private void extractCodes(HuffNode current, String path) {
		// traverse tree and extract codes
		if (current.left() == null && current.right() == null) {
			pathArray[current.value()] = path;
		}

		if (current.left() != null) {
			extractCodes(current.left(), path + 0);
			extractCodes(current.right(), path + 1);
		}

	}

	private void writeHeader(HuffNode current, BitOutputStream out) {
		// write the header
		if (current.left() == null && current.right() == null) {
			// if we're at a leaf node
			out.writeBits(1, 1);
			out.writeBits(9, current.value());

		} else {
			out.writeBits(1, 0);
			writeHeader(current.left(), out);
			writeHeader(current.right(), out);
		}

	}

	public void decompress(BitInputStream in, BitOutputStream out) {
		// check for HUFF_NUMBER
		int RB = in.readBits(BITS_PER_INT);
		if (RB != HUFF_NUMBER) {
			throw new HuffException("The first 32 bits aren't HUFF_NUMBER!");
		}
		// recreate tree from header
		HuffNode root = readHeader(in);

		// parse body of compressed file
		HuffNode current = root;
		int ReadBits = in.readBits(1);
		while (ReadBits != -1) {
			if (ReadBits == 1) {
				current = current.right();
			} else {
				current = current.left();
			}

			if (current.left() == null && current.right() == null) {
				if (current.value() == PSEUDO_EOF) {
					return;
				} else {
					out.writeBits(8, current.value());
					current = root;
				}

			}

			ReadBits = in.readBits(1);

		}

		throw new HuffException("PSEUDO_EOF error at end of file");

	}

	private HuffNode readHeader(BitInputStream in) {
		// recreate tree from header
		if (in.readBits(1) == 0) {
			HuffNode left = readHeader(in);
			HuffNode right = readHeader(in);
			return new HuffNode(-1, -1, left, right);
		}

		else {
			return new HuffNode(in.readBits(9), -99);
		}

	}

}
