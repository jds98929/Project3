/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;


import static java.lang.System.out;

/************************************************************************************
 * This class provides Tree maps.  Trees are used as multi-level index structures
 * that provide efficient access for both point queries and range queries.
 * All keys will be at the leaf level with leaf nodes linked by references.
 * Internal nodes will contain divider keys such that divKey corresponds to the
 * largest key in its left subtree.
 */
public class RegTreeMap <K extends Comparable <K>, V>
extends AbstractMap <K, V>
implements Serializable, Cloneable, SortedMap <K, V>
{
	/** The maximum fanout (number of children) for a Tree node.
	 *  May wish to increase for better performance for Program 3.
	 */
	private static final int ORDER = 5;

	/** The floor of half the ORDER.
	 */
	private static final int MID = ORDER / 2;

	/** The debug flag
	 */
	private static final boolean DEBUG = true;

	/** The class for type K.
	 */
	private final Class <K> classK;

	/** The class for type V.
	 */
	private final Class <V> classV;


	/********************************************************************************
	 * This inner class defines nodes that are stored in the tree map.
	 */
	private class Node
	{
		boolean   isLeaf;
		int       nKeys;
		K []      key;
		Object [] ref;

		@SuppressWarnings("unchecked")
		Node (boolean _isLeaf)
		{
			isLeaf = _isLeaf;
			nKeys  = 0;
			key    = (K []) Array.newInstance (classK, ORDER - 1);
			if (isLeaf) {
				//ref = (V []) Array.newInstance (classV, ORDER);
				ref = new Object [ORDER];
			} else {
				ref = (Node []) Array.newInstance (Node.class, ORDER);
			} // if
		} // constructor
	} // Node inner class

	/** The root of the Tree
	 */
	private Node root;

	/** The first (leftmost) leaf in the Tree
	 */
	private final Node firstLeaf;

	/** The counter for the number nodes accessed (for performance testing).
	 */
	private int count = 0;

	/** The global Node to split the new parent value whenever there is a split
	 */
	//private Node present;   // Bad idea, didnt work out
	/** Boolean value to let you the program know whether or not there has been a split with
	 *  a present Node to handle.
	 */
	//private boolean presentThere = false;  // part of the bad idea
	/** The global variable that increments whenever a key is added at the leaf level
	 */
	private int size = 0;
	/********************************************************************************
	 * Construct an empty Tree map.
	 * @param _classK  the class for keys (K)
	 * @param _classV  the class for values (V)
	 */
	public RegTreeMap (Class <K> _classK, Class <V> _classV)
	{
		classK    = _classK;
		classV    = _classV;
		root      = new Node (true);
		firstLeaf = root;
	} // constructor

	/********************************************************************************
	 * Return null to use the natural order based on the key type.  This requires the
	 * key type to implement Comparable.
	 */
	public Comparator <? super K> comparator () 
	{
		return null;
	} // comparator

	/********************************************************************************
	 * Return a set containing all the entries as pairs of keys and values.
	 * @return  the set view of the map
	 */
	public Set <Map.Entry <K, V>> entrySet ()
	{
		//  DONE
		Set <Map.Entry <K, V>> enSet = new HashSet <> ();
		Node pos = firstLeaf;
		while(pos != null){
			for(int i = 0; i < pos.nKeys; i++){
				enSet.add(new AbstractMap.SimpleEntry<K,V>(pos.key[i], (V) pos.ref[i]));
			}//for
			pos = (Node) pos.ref[ORDER - 1];
		}//while
		return enSet;
	} // entrySet

	/********************************************************************************
	 * Given the key, look up the value in the Tree map.
	 * @param key  the key used for look up
	 * @return  the value associated with the key or null if not found
	 */
	@SuppressWarnings("unchecked")
	public V get (Object key)
	{
		return find ((K) key, root);
	} // get

	/********************************************************************************
	 * Put the key-value pair in the Tree map.
	 * @param key    the key to insert
	 * @param value  the value to insert
	 * @return  null, not the previous value for this key
	 */
	public V put (K key, V value)
	{
		insert (key, value, root);
		return null;
	} // put

	/********************************************************************************
	 * Return the first (smallest) key in the Tree map.
	 * @return  the first key in the Tree map.
	 */
	public K firstKey () 
	{
		//  DONE
		return firstLeaf.key[0];
	} // firstKey

	/********************************************************************************
	 * Return the last (largest) key in the Tree map.
	 * @return  the last key in the Tree map.
	 */
	public K lastKey () 
	{
		//  DONE
		Node pos = firstLeaf;
		while(pos.ref[ORDER - 1] != null){
			pos = (Node) pos.ref[ORDER-1];
		}//while
		K lastKey = pos.key[0];
		for(int i = 1; i < pos.nKeys; i++){
			lastKey = pos.key[i];
		}//for
		return lastKey;
	} // lastKey

	/********************************************************************************
	 * Return the portion of the Tree map where key < toKey.
	 * @return  the submap with keys in the range [firstKey, toKey)
	 */
	public SortedMap <K,V> headMap (K toKey)
	{
		//DONE
		SortedMap<K,V> map = new TreeMap<K,V>();
		map = subMap(firstKey(), toKey);
		return map;
	} // headMap

	/********************************************************************************
	 * Return the portion of the Tree map where fromKey <= key.
	 * @return  the submap with keys in the range [fromKey, lastKey]
	 */
	public SortedMap <K,V> tailMap (K fromKey)
	{
		//  DONE
		SortedMap<K,V> map = new TreeMap<K,V>();
		Node pos = firstLeaf;
		K toKey = lastKey();
		boolean mapDone = false;
		while(!mapDone){
			for(int i = 0; i < pos.nKeys; i++){
				K temp = pos.key[i];
				if(temp.compareTo(fromKey) >= 0 && temp.compareTo(toKey) <= 0){
					map.put(temp, (V) pos.ref[i]);
				}//if
				if(pos.key[i].compareTo(toKey) >= 0){
					mapDone = true;
				}//if
			}//for
			if((Node) pos.ref[ORDER - 1] != null){
				pos = (Node) pos.ref[ORDER - 1];
			}//if
		}//while
		return map;
	} // tailMap

	/********************************************************************************
	 * Return the portion of the Tree map whose keys are between fromKey and toKey,
	 * i.e., fromKey <= key < toKey.
	 * @return  the submap with keys in the range [fromKey, toKey)
	 */
	public SortedMap <K,V> subMap (K fromKey, K toKey)
	{
		//  DONE
		TreeMap<K,V> map = new TreeMap<K,V>();
		Node pos = firstLeaf;
		boolean mapDone = false;
		while(!mapDone){
			for (int i = 0; i < pos.nKeys; i++){
				K temp = pos.key[i];

				if (temp.compareTo(fromKey) >= 0 && temp.compareTo(toKey) < 0 ){
					map.put(temp, (V) pos.ref[i]);
				}//if
				if (pos.key[i].compareTo(toKey) >= 0){
					mapDone = true;
				}//if
			}//for
			if ((Node) pos.ref[ORDER - 1] != null){
				pos = (Node) pos.ref[ORDER - 1];
			}//if
		}//while
		return map;
	} // subMap

	/********************************************************************************
	 * Return the size (number of keys) in the Tree.
	 * @return  the size of the Tree
	 */
	public int size ()
	{
		//  DONE
		return  size;
	} // size

	/********************************************************************************
	 * Print the Tree using a pre-order traveral and indenting each level.
	 * @param n      the current node to print
	 * @param level  the current level of the Tree
	 */
	@SuppressWarnings("unchecked")
	private void print (Node n, int level)
	{
		out.println ("BpTreeMap");
		out.println ("-------------------------------------------");

		for (int j = 0; j < level; j++) out.print ("\t");
		out.print ("[ . ");
		for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
		out.println ("]");
		if ( ! n.isLeaf) {
			for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
		} // if

		out.println ("-------------------------------------------");
	} // print

	/********************************************************************************
	 * Recursive helper function for finding a key in trees.
	 * @param key  the key to find
	 * @param ney  the current node
	 */
	@SuppressWarnings("unchecked")
	private V find (K key, Node n)
	{
		count++;
		for (int i = 0; i < n.nKeys; i++) {
			K k_i = n.key [i];
			if (key.compareTo (k_i) <= 0) {
				if (n.isLeaf) {
					return (V) n.ref [i];
				} else {
					return find (key, (Node) n.ref [i]);
				} // if
			} // if
		} // for
		return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]);
	} // find

	/********************************************************************************
	 * Recursive helper function for inserting a key in trees.
	 * @param key  the key to insert
	 * @param ref  the value/node to insert
	 * @param n    the current node
	 * @return  the node inserted into (may wish to return more information)
	 */
	private Node insert (K key, V ref, Node n)
	{
		boolean inserted = false;
		if (n.isLeaf) {                                  // handle leaf node
			if (n.nKeys < ORDER - 1) {
				for (int i = 0; i < n.nKeys; i++) {
					K k_i = n.key [i];
					if (key.compareTo (k_i) < 0) {
						wedgeL (key, ref, n, i);
						inserted = true;
						size++;
						break;
					} else if (key.equals (k_i)) {
						out.println ("BpTreeMap.insert: attempt to insert duplicate key = " + key);
						inserted = true;
						break;
					} // if
				} // for
				if (! inserted) wedgeL (key, ref, n, n.nKeys);
			} else {
				for (int i = 0; i < n.nKeys; i++) {
					if (key.equals (n.key[i])) {	
						out.println ("BpTreeMap.insert: attempt to insert duplicate key = " + key);
						return null;				
					} // if
				} // for
				if(!inserted){
					Node sib = splitL (key, ref, n);	
					//					present = new Node(false);    // part of the old code that didnt work out
					//					present.key[0] = n.key[n.nKeys-1];
					//					present.ref[0] = n;
					//					present.ref[1] = sib;
					//					presentThere = true;
					//					present.nKeys++;
					if(n == root) // The base Case
					{
						//						out.println("If n = root"); // more of the old method code
						//						root = present;
						//						presentThere = false;
						Node newRoot = new Node (false);
						root = newRoot;
						root.key[0] = n.key[n.nKeys - 1];	
						root.ref[0] = n;						
						root.ref[1] = sib;
						root.nKeys++;
						sib.ref[ORDER - 1] = null;
					}
					else{
						return sib;
					}//if
				}//if
			} // if
		}
		//Attempt at implementing the further methods past the initial root creation. Didn't work, had to alter logic 
		//		 else {                                         // handle internal node
		//
		//			//  T O   B E   I M P L E M E N T E D
		//
		//			out.println("Else not a leaf");
		//			if(!presentThere){
		//
		//				int insertRef = 0;
		//				if (n.nKeys < ORDER - 1) {
		//					for (int i = 0; i < n.nKeys; i++) {
		//						K k_i = n.key [i];
		//						if (key.compareTo (k_i) > 0) {
		//							//return insert(key, ref, (Node) n.ref[i]);
		//							insertRef++;
		//							//inserted = true;
		//						} else if (key.equals (k_i)) {
		//							out.println ("BpTreeMap.insert: attempt to insert duplicate key = " + key);
		//							//inserted = true;
		//							break;
		//						} // if
		//					} // for
		//
		//				}
		//				return insert(key,ref, (Node) n.ref[insertRef]);
		//			}
		//			else{
		//				inserted = false;
		//				Node right = null;
		//				for(int i = 0; i < n.nKeys; i++){				
		//					if(key.compareTo (n.key[i]) < 0){
		//						right = insert(key, ref, (Node)n.ref[i]);		
		//						inserted = true;
		//						break;
		//					}
		//				}
		//				if(!inserted){
		//					right = insert(key, ref, (Node) n.ref[n.nKeys]);
		//				}
		//				if(right != null){
		//					Node left = (Node)right.ref[ORDER-1];
		//					right.ref[ORDER - 1] = null;	
		//					Node sib = right.isLeaf ? splitI(left.key[left.nKeys - 1], (BpTreeMap<K, V>.Node)right, n) : splitI(left.key[left.nKeys], (BpTreeMap<K, V>.Node)right, n);
		//					
		//					if(n == root){
		//						Node newRoot = new Node(false);
		//						root = newRoot;
		//						
		//						root.key[0] = n.key[n.nKeys];
		//						root.ref[0] = n;
		//						root.ref[1] = sib;
		//						
		//						newRoot.nKeys++;
		//					}
		//					else{
		//						return sib;
		//					}
		//				
		//				}
		//			}
		//
		//		} // if
		else {                                         // handle internal node
			Node right = null;
			int i;
			if (n.nKeys < ORDER - 1) {					
				for(i = 0; i < n.nKeys; i++){			
					if(key.compareTo (n.key[i]) < 0){
						right = insert(key, ref, (Node)n.ref[i]);
						inserted = true;
						break;
					}//if
				}//for
				if(!inserted){						
					right = insert(key, ref, (Node)n.ref[n.nKeys]);
				}//if
				if(right != null){					
					Node left = (Node)right.ref[ORDER - 1];	
					right.ref[ORDER - 1] = null;	
					i = inserted ? i : n.nKeys;	
					K tempKey = right.isLeaf ? left.key[left.nKeys - 1] : left.key[left.nKeys];																					//If the returned sibling is a leaf, the key will be in the nKeys-1 slot of left. If it's not a leaf, the returned key will be in the nKeys slot of left.
					wedgeI(tempKey, (V)right, n, i); //Wedge in based on the final entry past nKeys that does NOT have a reference after it.
				}//if
			}
			else{
				for(i = 0; i < n.nKeys; i++){				//Find where we would like to insert it.
					if(key.compareTo (n.key[i]) < 0){
						right = insert(key, ref, (Node)n.ref[i]);		//If we find it, insert it. Right will store the returned right sibling if the insert overflows.
						inserted = true;
						break;
					}//if
				}//for
				if(!inserted){
					right = insert(key, ref, (Node)n.ref[n.nKeys]);		//We did not find an insertion point earlier and know we want to throw it into the final reference slot. Right will store the returned right sibling if the insert overflows.
				}//if
				if(right != null){
					Node left = (Node)right.ref[ORDER-1];
					right.ref[ORDER - 1] = null;	//Clean up, clean up, everybody it's time for clean up.
					Node sib = right.isLeaf ? splitI(left.key[left.nKeys - 1], right, n) : splitI(left.key[left.nKeys], right, n);
					if(n == root){
						Node newRoot = new Node(false);
						root = newRoot;
						root.key[0] = n.key[n.nKeys];
						root.ref[0] = n;
						root.ref[1] = sib;
						newRoot.nKeys++;
					}
					else{
						return sib;
					}//if
				}//if
			}//if
		} // if	
		if (DEBUG) print (root, 0);
		return null;                                     // FIX: return useful information
	} // insert

	/********************************************************************************
	 * Wedge the key-ref pair into leaf node n.
	 * @param key  the key to insert
	 * @param ref  the value/node to insert
	 * @param n    the current node
	 * @param i    the insertion position within node n
	 */
	private void wedgeL (K key, V ref, Node n, int i)
	{
		for (int j = n.nKeys; j > i; j--) {
			n.key [j] = n.key [j-1];
			n.ref [j] = n.ref [j-1];
		} // for
		n.key [i] = key;
		n.ref [i] = ref;
		n.nKeys++;
	} // wedgeL

	/********************************************************************************
	 * Wedge the key-ref pair into internal node n.
	 * @param key  the key to insert
	 * @param ref  the value/node to insert
	 * @param n    the current node
	 * @param i    the insertion position within node n
	 */
	private void wedgeI (K key, V ref, Node n, int i)
	{
		//  DONE
		//out.println ("wedgeI not implemented yet");
		if(((Node) ref).key[((Node)ref).nKeys - 1].compareTo(key) <= 0){
			n.ref[n.nKeys+1] = n.ref[n.nKeys];
			for (int j = n.nKeys; j > i; j--) {
				n.key [j] = n.key [j-1];
				n.ref [j] = n.ref [j-1];
			} // for
			n.key[i] = key;
			n.ref[i] = ref;
		}
		else{
			for (int j = n.nKeys; j > i; j--) {
				n.key [j] = n.key [j-1];
				n.ref [j+1] = n.ref [j-1];
			} // for
			n.key[i] = key;
			n.ref[i+1] = ref;
		}
		n.nKeys++;
	} // wedgeI

	/********************************************************************************
	 * Split leaf node n and return the newly created right sibling node rt.
	 * Split first (MID keys for both node n and node rt), then add the new key and ref.
	 * @param key  the new key to insert
	 * @param ref  the new value/node to insert
	 * @param n    the current node
	 * @return  the right sibling node (may wish to provide more information)
	 */
	private Node splitL (K key, V ref, Node n)
	{
		// DONE
		//out.println ("splitL not implemented yet");
		Node rt = new Node (true);
		for(int i = 0; i < n.nKeys - MID; i++)
		{
			rt.ref[i] = n.ref[MID+i];
			rt.key[i] = n.key[MID+i];
			n.key[MID + i] = null;
			n.ref[MID + i] = null;
			rt.nKeys++;
		}//for
		rt.ref[ORDER - 1 - MID] = n.ref[ORDER - 1];	
		n.nKeys -= MID;
		if(n.ref[ORDER - 1] == null){
			rt.ref[ORDER - 1] = n;
		}//if
		insert(key, ref, key.compareTo(n.key[n.nKeys - 1]) <= 0 ? n : rt);
		n.ref[n.nKeys] = rt;
		return rt;
	} // splitL

	/********************************************************************************
	 * Split internal node n and return the newly created right sibling node rt.
	 * Split first (MID keys for node n and MID-1 for node rt), then add the new key and ref.
	 * @param key  the new key to insert
	 * @param ref  the new value/node to insert
	 * @param n    the current node
	 * @return  the right sibling node (may wish to provide more information)
	 */
	private Node splitI (K key, Node ref, Node n)
	{
		//out.println ("splitI not implemented yet");
		Node rt = new Node (false);
		for(int i = 0; i < n.nKeys - MID; i++){		//Split the node into 2.
			rt.key[i] = n.key[i + MID];
			rt.ref[i] = n.ref[i + MID];
			n.key[i + MID] = null;
			n.ref[i + MID] = null;
			rt.nKeys++;
		}//for
		n.nKeys = n.nKeys - (MID + 1);
		rt.ref[ORDER - 1 - MID] = n.ref[ORDER -1];
		boolean inserted = false;
		if(key.compareTo(n.key[n.nKeys]) <= 0){
			rt.ref[ORDER -1] = n;
			K temp = n.key[n.nKeys];
			for(int i = 0; i < n.nKeys; i++){
				if(key.compareTo(n.key[i]) <= 0){
					wedgeI(key, (V) ref, n, i);
					inserted = true;
					break;
				}//if
			}//for
			if(!inserted){
				wedgeI(key, (V) ref, n, n.nKeys);
			}//if
			n.key[n.nKeys] = temp;
		}
		else{
			for(int i = 0; i < rt.nKeys; i++){
				if(key.compareTo (rt.key[i]) <= 0){
					wedgeI(key, (V) ref, rt, i);
					inserted = true;
					break;
				}//if
			}//for
			if(!inserted){
				wedgeI(key, (V) ref, rt, rt.nKeys);
			}//if
			rt.ref[ORDER - 1] = n;		
		}//if
		return rt;
	} // splitI

	/********************************************************************************
	 * The main method used for testing.
	 * @param  the command-line arguments (args [0] gives number of keys to insert)
	 */
	public static void main (String [] args)
	{
		int totalKeys    = 25;
		boolean RANDOMLY = false;
		RegTreeMap <Integer, Integer> bpt = new RegTreeMap <> (Integer.class, Integer.class);
		if (args.length == 1) totalKeys = Integer.valueOf (args [0]);
		if (RANDOMLY) {
			Random rng = new Random ();
			for (int i = 1; i <= totalKeys; i += 2) bpt.put (rng.nextInt (2 * totalKeys), i * i);
		} else {
			for (int i = 1; i <= totalKeys; i += 2) bpt.put (i, i * i);
		} // if
		bpt.print (bpt.root, 0);
		for (int i = 0; i <= totalKeys; i++) {
			out.println ("key = " + i + " value = " + bpt.get (i));
		} // for
		out.println ("-------------------------------------------");
		out.println ("Average number of nodes accessed = " + bpt.count / (double) totalKeys);
	} // main

} // BpTreeMap class


