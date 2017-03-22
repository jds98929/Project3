package Relation;
/************************************************************************************
 * @file TreeMap.java
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
public class TreeMap <K extends Comparable <K>, V>
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
					ref = new Object [ORDER];
				} else {
					ref = (Node []) Array.newInstance (Node.class, ORDER);
				}
			}
	}

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
	public TreeMap (Class <K> _classK, Class <V> _classV)
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
		Set <Map.Entry <K, V>> enSet = new HashSet <> ();
		Node pos = firstLeaf;
		while(pos != null){
			for(int i = 0; i < pos.nKeys; i++){
				enSet.add(new AbstractMap.SimpleEntry<K,V>(pos.key[i], (V) pos.ref[i]));
			}
			pos = (Node) pos.ref[ORDER - 1];
		}
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
		return firstLeaf.key[0];
	} // firstKey

	/********************************************************************************
	 * Return the last (largest) key in the Tree map.
	 * @return  the last key in the Tree map.
	 */
	public K lastKey () 
	{
		Node pos = firstLeaf;
		while(pos.ref[ORDER - 1] != null){
			pos = (Node) pos.ref[ORDER-1];
		}
		K lastKey = pos.key[0];
		for(int i = 1; i < pos.nKeys; i++){
			lastKey = pos.key[i];
		}
		return lastKey;
	} // lastKey

	/********************************************************************************
	 * Return the portion of the Tree map where key < toKey.
	 * @return  the submap with keys in the range [firstKey, toKey)
	 */
	public SortedMap <K,V> headMap (K toKey)
	{
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
		SortedMap<K,V> map = new TreeMap<K,V>();
		Node pos = firstLeaf;
		K toKey = lastKey();
		boolean mapDone = false;
		while(!mapDone){
			for(int i = 0; i < pos.nKeys; i++){
				K temp = pos.key[i];
				if(temp.compareTo(fromKey) >= 0 && temp.compareTo(toKey) <= 0){
					map.put(temp, (V) pos.ref[i]);
				}
				if(pos.key[i].compareTo(toKey) >= 0){
					mapDone = true;
				}
			}
			if((Node) pos.ref[ORDER - 1] != null){
				pos = (Node) pos.ref[ORDER - 1];
			}
		}
		return map;
	} // tailMap

	/********************************************************************************
	 * Return the portion of the Tree map whose keys are between fromKey and toKey,
	 * i.e., fromKey <= key < toKey.
	 * @return  the submap with keys in the range [fromKey, toKey)
	 */
	public SortedMap <K,V> subMap (K fromKey, K toKey)
	{
		SortedMap<K,V> map = new TreeMap<K,V>();
		Node pos = firstLeaf;
		boolean mapDone = false;
		while(!mapDone){
			for (int i = 0; i < pos.nKeys; i++){
				K temp = pos.key[i];

				if (temp.compareTo(fromKey) >= 0 && temp.compareTo(toKey) < 0 ){
					map.put(temp, (V) pos.ref[i]);
				}
				if (pos.key[i].compareTo(toKey) >= 0){
					mapDone = true;
				}
			}
			if ((Node) pos.ref[ORDER - 1] != null){
				pos = (Node) pos.ref[ORDER - 1];
			}
		}
		return map;
	} // subMap

	/********************************************************************************
	 * Return the size (number of keys) in the Tree.
	 * @return  the size of the Tree
	 */
	public int size ()
	{
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
			out.println ("TreeMap");
			out.println ("-------------------------------------------");

			for (int j = 0; j < level; j++) out.print ("\t");
			out.print ("[ . ");
			for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
			out.println ("]");
			if ( ! n.isLeaf) {
				for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
			}

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
					}
				}
			} 
			return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]);
		} 

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
		if (n.isLeaf) {
			if (n.nKeys < ORDER - 1) {
				for (int i = 0; i < n.nKeys; i++) {
					K k_i = n.key [i];
					if (key.compareTo (k_i) < 0) {
						wedgeL (key, ref, n, i);
						inserted = true;
						size++;
						break;
					} else if (key.equals (k_i)) {
						out.println ("TreeMap.insert: attempt to insert duplicate key = " + key);
						inserted = true;
						break;
					}
				}
				if (! inserted) wedgeL (key, ref, n, n.nKeys);
			} else {
				for (int i = 0; i < n.nKeys; i++) {
					if (key.equals (n.key[i])) {	
						out.println ("TreeMap.insert: attempt to insert duplicate key = " + key);
						return null;				
					}
				}
				if(!inserted){
					Node sib = splitL (key, ref, n);	
					present.nKeys++;
					if(n == root)
					{
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
					}
				}
			}
		}

		Node right = null;
		int i;
		if (n.nKeys < ORDER - 1) {					
			for(i = 0; i < n.nKeys; i++){			
				if(key.compareTo (n.key[i]) < 0){
					right = insert(key, ref, (Node)n.ref[i]);
					inserted = true;
					break;
				}
			}
			if(!inserted){						
				right = insert(key, ref, (Node)n.ref[n.nKeys]);
			}
			if(right != null){					
				Node left = (Node)right.ref[ORDER - 1];	
				right.ref[ORDER - 1] = null;	
				i = inserted ? i : n.nKeys;	
				K tempKey = right.isLeaf ? left.key[left.nKeys - 1] : left.key[left.nKeys];
				wedgeI(tempKey, (V)right, n, i);
			}
		}
		else{
			for(i = 0; i < n.nKeys; i++){									if(key.compareTo (n.key[i]) < 0){
				right = insert(key, ref, (Node)n.ref[i]);
				inserted = true;
				break;
			}
			}
			if(!inserted){
				right = insert(key, ref, (Node)n.ref[n.nKeys]);
			}
			if(right != null){
				Node left = (Node)right.ref[ORDER-1];
				right.ref[ORDER - 1] = null;
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
				}
			}
		}
	
	if (DEBUG) print (root, 0);
	return null;                                   	
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
 * Split then add the new key and ref.
 * @param key  the new key to insert
 * @param ref  the new value/node to insert
 * @param n    the current node
 * @return  the right sibling node (may wish to provide more information)
 */
private Node splitL (K key, V ref, Node n)
{
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
	Node rt = new Node (false);
		for(int i = 0; i < n.nKeys - MID; i++){
			//split node
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
		TreeMap <Integer, Integer> tree = new TreeMap <> (Integer.class, Integer.class);
		if (args.length == 1) totalKeys = Integer.valueOf (args [0]);
		if (RANDOMLY) {
			Random rng = new Random ();
			for (int i = 1; i <= totalKeys; i += 2) tree.put (rng.nextInt (2 * totalKeys), i * i);
		} else {
			for (int i = 1; i <= totalKeys; i += 2) tree.put (i, i * i);
		} // if
		tree.print (tree.root, 0);
		for (int i = 0; i <= totalKeys; i++) {
			out.println ("key = " + i + " value = " + tree.get (i));
		} // for
		out.println ("-------------------------------------------");
		out.println ("Average number of nodes accessed = " + tree.count / (double) totalKeys);
	} // main

} // TreeMap class



