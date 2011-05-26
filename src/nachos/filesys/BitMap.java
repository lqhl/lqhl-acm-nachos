package nachos.filesys;

import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.machine.OpenFile;

/**
 * @author Kang Zhang
 * 
 *         This file is derived from the code by 1998 Rice University and
 *         1992-1993 The Regents of the University of California.
 */

// BitMap.java
// Class to manage a bitmap -- an array of bits each of which
// can be either on or off.
//
// Represented as an array of unsigned integers, on which we do
// modulo arithmetic to find the bit we are interested in.
//
// The bitmap can be parameterized with with the number of bits being
// managed.
//
// The following class defines a "bitmap" -- an array of bits,
// each of which can be independently set, cleared, and tested.
//
// Most useful for managing the allocation of the elements of an array --
// for instance, disk sectors, or main memory pages.
// Each bit represents whether the corresponding sector or page is
// in use or free.
class BitMap {
	// Definitions helpful for representing a bitmap as an array of integers
	public static final int BitsInByte = 8;

	public static final int BitsInWord = 32;

	private int numBits; // number of bits in the bitmap

	private int numWords; // number of words of bitmap storage

	// (rounded up if numBits is not a
	// multiple of the number of bits in
	// a word)
	private int map[]; // bit storage

	// ----------------------------------------------------------------------
	// BitMap::BitMap
	// Initialize a bitmap with "nitems" bits, so that every bit is clear.
	// it can be added somewhere on a list.
	//
	// "nitems" is the number of bits in the bitmap.
	// ----------------------------------------------------------------------

	public BitMap(int nitems) {
		numBits = nitems;
		numWords = numBits / BitsInWord;
		if (numBits % BitsInWord != 0)
			numWords++;

		map = new int[numWords];
		for (int i = 0; i < numBits; i++)
			clear(i);
	}

	// ----------------------------------------------------------------------
	// BitMap::Mark
	// Set the "nth" bit in a bitmap.
	//
	// "which" is the number of the bit to be set.
	// ----------------------------------------------------------------------

	public void mark(int which) {
		Lib.assertTrue((which >= 0 && which < numBits));
		map[which / BitsInWord] |= 1 << (which % BitsInWord);
	}

	// ----------------------------------------------------------------------
	// BitMap::Clear
	// Clear the "nth" bit in a bitmap.
	//
	// "which" is the number of the bit to be cleared.
	// ----------------------------------------------------------------------

	public void clear(int which) {
		Lib.assertTrue(which >= 0 && which < numBits);
		map[which / BitsInWord] &= ~(1 << (which % BitsInWord));
	}

	// ----------------------------------------------------------------------
	// BitMap::test
	// Return true if the "nth" bit is set.
	//
	// "which" is the number of the bit to be tested.
	// ----------------------------------------------------------------------

	public boolean test(int which) {
		Lib.assertTrue(which >= 0 && which < numBits);

		if ((map[which / BitsInWord] & (1 << (which % BitsInWord))) != 0)
			return true;
		else
			return false;
	}

	// ----------------------------------------------------------------------
	// BitMap::find
	// Return the number of the first bit which is clear.
	// As a side effect, set the bit (mark it as in use).
	// (In other words, find and allocate a bit.)
	//
	// If no bits are clear, return -1.
	// ----------------------------------------------------------------------

	public int find() {
		for (int i = 0; i < numBits; i++)
			if (!test(i)) {
				mark(i);
				return i;
			}
		return -1;
	}

	// ----------------------------------------------------------------------
	// BitMap::NumClear
	// Return the number of clear bits in the bitmap.
	// (In other words, how many bits are unallocated?)
	// ----------------------------------------------------------------------

	public int numClear() {
		int count = 0;

		for (int i = 0; i < numBits; i++)
			if (!test(i))
				count++;
		return count;
	}

	// ----------------------------------------------------------------------
	// BitMap::print
	// Print the contents of the bitmap, for debugging.
	//
	// Could be done in a number of ways, but we just print the #'s of
	// all the bits that are set in the bitmap.
	// ----------------------------------------------------------------------

	public void print() {
		Lib.debug('+', "Bitmap set:\n");
		for (int i = 0; i < numBits; i++)
			if (test(i))
				Lib.debug('+', i + ", ");
		Lib.debug('+', "");
	}

	// These aren't needed until the FILESYS assignment

	// ----------------------------------------------------------------------
	// BitMap::fetchFromFile
	// Initialize the contents of a bitmap from a Nachos file.
	//
	// "file" is the place to read the bitmap from
	// ----------------------------------------------------------------------

	public void fetchFrom(OpenFile file) {
		byte buffer[] = new byte[numWords * 4];
		// read bitmap
		file.read(0, buffer, 0, numWords * 4);
		// unmarshall
		for (int i = 0; i < numWords; i++)
			map[i] = Disk.intInt(buffer, i * 4);
	}

	// ----------------------------------------------------------------------
	// BitMap::writeBack
	// Store the contents of a bitmap to a Nachos file.
	//
	// "file" is the place to write the bitmap to
	// ----------------------------------------------------------------------

	public void writeBack(OpenFile file) {
		byte buffer[] = new byte[numWords * 4];
		// marshall
		for (int i = 0; i < numWords; i++)
			Disk.extInt(map[i], buffer, i * 4);
		// write bitmap
		file.write(0, buffer, 0, numWords * 4);
	}

}
