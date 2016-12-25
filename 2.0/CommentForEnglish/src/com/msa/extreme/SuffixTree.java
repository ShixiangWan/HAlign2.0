package com.msa.extreme;

import java.util.*;
/**
 * Suffix tree implementation: Ukkonen construction method,
 * Translated Articles:<code>http://www.oschina.net/translate/ukkonens-suffix-tree-algorithm-in-plain-english</code>
 * In this paper, we use the edge to save the character information. Here we use the node to save the character information, 
 * and there is no difference. At the same time, the structure of the tree is preserved by child nodes and sibling nodes, 
 * that is, traversing a child node of a node, it needs to find its direct child node first, and then access its sibling node
 * to find all child nodes.
 * 
 * @author Quan Zou
 */
public class SuffixTree {
	private  Node root = new Node(new char[0]);// root node, which is no chars
	// active point is triple：(active_node, active_edge, active_length)
	// active_node is current active point, active_edge is active edge，active_length is active length
	private ActivePoint activePoint = new ActivePoint(root, null, 0);
	private int reminder = 0;// remainder, indicating how many suffixes need to be inserted

	public int minMatchLen = 15;

	/**
	 * Build the suffix tree
	 *
	 * @param word
	 */
	public void build(String word) {
		int index = 0;
		char[] chars = word.toCharArray();
		while (index < chars.length) { // The loop creates the suffix
			int currenctIndex = index++; // save current location
			char w = chars[currenctIndex]; // The current suffix character

			if (find(w)) {// Finds whether there is a node that holds the current suffix character
				reminder++;//Exists, then reminder + 1, activePoint.length + 1, and then return to it
				continue;
			}

			/*If not, if reminder == 0 before the character before there is no remaining with the inserted 
			 * suffix characters, so you can directly insert the suffix character*/
			if (reminder == 0) {
				/*Directly in the current active node to insert a node can be inserted here, the node contains 
				 * the character from the current character to start the string of all the remaining characters, 
				 * here is an optimization.*/
				Node node = new Node(Arrays.copyOfRange(chars, currenctIndex, chars.length));
				//node.position.add(currenctIndex);
				node.label=currenctIndex;
				/*If the current active node has no child nodes, it will be the new node as its child nodes. 
				 * Otherwise, it will loop through the child nodes (save through the sibling node)*/				
				Node child = activePoint.point.child;
				if (null == child) {
					activePoint.point.child = node;
				} else {
					while (null != child.brother) {
						child = child.brother;
					}
					child.brother = node;
				}
				node.father=activePoint.point;
				/*********
				 Node fath = node.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(currenctIndex);
				 fath = fath.father;
				 }
				 *********/
			} else if(activePoint.index==null){

				/*Just before the one side of the reminder that those who insert a new character, 
				 * the new side of the deposit directly to the character, and to the previous owe supplement*/
				Node node = new Node(Arrays.copyOfRange(chars, currenctIndex, chars.length));
				//node.position.add(currenctIndex-reminder);
				node.label=currenctIndex-reminder;


				/*If the current active node has no child nodes, it will be the new node as its child nodes. 
				 * Otherwise, it will loop through the child nodes (save through the sibling node)*/
				Node child = activePoint.point.child;
				if (null == child) {
					activePoint.point.child = node;
				} else {
					while (null != child.brother) {
						child = child.brother;
					}
					child.brother = node;
				}
				node.father = activePoint.point;
				/************
				 Node fath = node.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(currenctIndex-reminder);
				 fath = fath.father;
				 }
				 *************/

				// According to rule 1 for processing
				if (root == activePoint.point) {// active node is root node
					// activePoint.point == root
					// According to rule 3 for processing
				} else if (null == activePoint.point.suffixNode) {// No-suffix node, the active node becomes root
					activePoint.point = root;
				} else {// Otherwise, the active node becomes the suffix node of the current active node
					activePoint.point = activePoint.point.suffixNode;
				}
				// Both active and active edge lengths are reset
				activePoint.index = null;
				activePoint.length = 0;
				// The remainder of the suffix to be inserted is processed recursively
				innerSplit(chars, currenctIndex, activePoint.point);
			}	else if(reminder-getNodeString(activePoint.point).length()<activePoint.index.chars.length){//Not reminder, is distance
				// If reminder> 0, it indicates that there are remaining characters before the character, 
				// need to be split, and then insert the new suffix character
				Node splitNode = activePoint.index;// The node to be split is the active edge(active_edge)
				// Create a segmented node and drop it on the child node of the current node
				// This node inherits the information of the child node and suffix node of the current node
				// Create a new node as the son of index, index into internal nodes
				// The remaining characters are taken as child nodes starting from the active edge length
				Node node = new Node(Arrays.copyOfRange(splitNode.chars, activePoint.length, splitNode.chars.length));
				node.child = splitNode.child;

				Node child = splitNode.child;
				while(null!=child){
					child.father=node;
					child=child.brother;
				}


				node.suffixNode = splitNode.suffixNode;
				splitNode.child = node;
				node.father = splitNode;
				splitNode.suffixNode = null;
				//node.position = (ArrayList<Integer>) splitNode.position.clone();
				if(splitNode.chars[splitNode.chars.length-1]=='$')
				{
					node.label=splitNode.label;
				}
				// Insert the new suffix character
				Node newNode = new Node(Arrays.copyOfRange(chars, currenctIndex, chars.length));
				splitNode.child.brother = newNode;
				newNode.father=splitNode;
				// Modifies the character of the current node
				splitNode.chars = Arrays.copyOfRange(splitNode.chars, 0, activePoint.length);
				//newNode.position.add(currenctIndex-reminder);
				newNode.label=currenctIndex-reminder;

				//Node fath = newNode.father;
				/***************
				 while(null!=fath && fath!=root){
				 fath.position.add(currenctIndex-reminder);
				 fath = fath.father;
				 }
				 ****************/

				// According to rule 1 for processing
				if (root == activePoint.point) {// active node is root node
					// activePoint.point == root
					// According to rule 3 for processing
				} else if (null == activePoint.point.suffixNode) {// No-suffix node, the active node becomes root
					activePoint.point = root;
				} else {// Otherwise, the active node becomes the suffix node of the current active node
					activePoint.point = activePoint.point.suffixNode;
				}
				// Both active and active edge lengths are reset
				activePoint.index = null;
				activePoint.length = 0;
				// The remainder of the suffix to be inserted is processed recursively
				innerSplit(chars, currenctIndex, splitNode);
			}
			else if(reminder-getNodeString(activePoint.point).length()==activePoint.index.chars.length){
				Node node = new Node(Arrays.copyOfRange(chars, currenctIndex, chars.length));
				//node.position.add(currenctIndex-reminder);
				node.label=currenctIndex-reminder;

				Node child = activePoint.index.child;
				if (null == child) {
					activePoint.index.child = node;
				} else {
					while (null != child.brother) {
						child = child.brother;
					}
					child.brother = node;
				}
				node.father = activePoint.index;
				/***********
				 Node fath = node.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(currenctIndex-reminder);
				 fath = fath.father;
				 }
				 ************/

				Node ttmp=activePoint.index;
				if (root == activePoint.point) {// active node is root node
					// activePoint.point == root
					// According to rule 3 for processing
				} else if (null == activePoint.point.suffixNode) {
					// No-suffix node, the active node becomes root
					activePoint.point = root;
				} else {// Otherwise, the active node becomes the suffix node of the current active node
					activePoint.point = activePoint.point.suffixNode;
				}
				// Both active and active edge lengths are reset
				activePoint.index = null;
				activePoint.length = 0;
				// The remainder of the suffix to be inserted is processed recursively
				innerSplit(chars, currenctIndex, ttmp);
			}
			//***********
			if(index==chars.length&&reminder > 0){
				System.out.println("This should not appear!");
				index-=reminder;
				reminder=0;
				activePoint.point = root;
				activePoint.index = null;
				activePoint.length=0;
			}
			//***********
		}

	}

	/**
	 * Process the remaining pending insert suffixes
	 * @param chars	Constructs all the characters of the suffix tree
	 * @param currenctIndex	The character position that is currently being processed
	 * @param prefixNode The successor node, the node that has been segmented, is used to identify the suffix node
	 */
	private void innerSplit(char[] chars, int currenctIndex, Node prefixNode) {
		/*For example, we need to insert the three suffixes (abx, bx, x), has handled the abx, 
		 * there are still bx and x, then the following calculation of the position is b position*/
		int start = currenctIndex - reminder + 1;
		//To be modified later, each node to the root of the number of characters should be stored
		int tmpp=0;
		Node fathh= activePoint.point;
		while(fathh!=root){
			tmpp+=fathh.chars.length;
			fathh=fathh.father;
		}

		int dealStart = start + tmpp + activePoint.length;
		//*********************************************************
		// Starting from the dealStart find all the suffix characters are there (relative to the active point)
		for (int index = dealStart; index <= currenctIndex; index++) {
			char w = chars[index];
			if (find(w)) {
				continue;
			}
			Node splitNode = null;// The segmented node

			if(null==activePoint.index){
				// If activePoint.index == null, indicating that no active edge, 
				// then only need to insert a node in the active node can be
				splitNode=activePoint.point;
				Node node = new Node(Arrays.copyOfRange(chars, index, chars.length));
				
				//node.position.add(start);
				node.label=start;
				Node child = activePoint.point.child;
				if(null==child){
					activePoint.point.child = node;
				}else{
					while (null != child.brother) {
						child = child.brother;
					}
					child.brother = node;
				}
				node.father = activePoint.point;
				// modify root node
				// After the completion of the division according to rule 1 and rule 3 to be treated differently
				// According to rule 1 for processing
				if (root == activePoint.point) {// active node is root node
					// activePoint.point == root
					// According to rule 3 for processing
				} else if (null == activePoint.point.suffixNode) {// No-suffix node, the active node becomes root
					activePoint.point = root;
				} else {// Otherwise, the active node becomes the suffix node of the current active node
					activePoint.point = activePoint.point.suffixNode;
				}
				// Both active and active edge lengths are reset
				activePoint.index = null;
				activePoint.length = 0;
				//*******************************
				/**************
				 Node fath = node.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(start);
				 fath = fath.father;
				 }
				 ***************/
			}else{
				// Start the division, the division of the same part of the above segmentation
				splitNode = activePoint.index;//(The active node's son node is not the parent node)
				// Create a segmented node and drop it on the child node of the current node
				// This node inherits the information of the child node and suffix node of the current node
				Node node = new Node(Arrays.copyOfRange(splitNode.chars, activePoint.length, splitNode.chars.length));
				node.child = splitNode.child;

				Node child = splitNode.child;
				while(null!=child){
					child.father=node;
					child=child.brother;
				}

				node.suffixNode = splitNode.suffixNode;
				splitNode.child = node;
				node.father = splitNode;
				splitNode.suffixNode = null;
				//node.position = (ArrayList<Integer>) splitNode.position.clone();
				if(splitNode.chars[splitNode.chars.length-1]=='$')
					node.label=splitNode.label;
				/*Creates a newly inserted node and places it in the child node of 
				 * the current node (saved by the sibling node of the child node)*/
				Node newNode = new Node(Arrays.copyOfRange(chars, index, chars.length));
				splitNode.child.brother = newNode;
				newNode.father = splitNode;
				// Modifies the number of characters for the current node
				splitNode.chars = Arrays.copyOfRange(splitNode.chars, 0, activePoint.length);
				// Rule 2, connect the suffix node
				prefixNode.child.suffixNode = splitNode;

				//****Computes the length of the string from the splitNode to the root
				int k=0;
				Node tmp = splitNode;
				while(tmp!=root){
					k+=tmp.chars.length;
					tmp=tmp.father;
				}
				//***************************************
				//newNode.position.add(index-k);
				newNode.label=index-k;
				/******************
				 if(index-k<0){
				 System.out.println("+++++++++++++++++++++++++++");
				 System.out.println(index);
				 System.out.println(k);
				 }

				 /*****************
				 Node fath = newNode.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(index-k);
				 fath = fath.father;
				 }
				 *****************/
			}

			reminder--;

			// According to rule 1 for processing
			if (root == activePoint.point) {// active point is root point
				// activePoint.point == root
				// According to rule 3 for processing
			} else if (null == activePoint.point.suffixNode) {
				activePoint.point = root;
			} else {
				activePoint.point = activePoint.point.suffixNode;
			}

			activePoint.index = null;
			activePoint.length = 0;
			if(reminder > 0){// If reminder == 0 you do not need to continue recursively inserting the suffix
				innerSplit(chars, currenctIndex, splitNode);
			}
		}

	}

	/**
	 * Finds whether there is a node (edge) containing the suffix character in the child node of the current active point.
	 *
	 * @param w
	 * @return
	 */
	private boolean find(char w) {
		final Node start = activePoint.point;
		final Node current = activePoint.index;
		boolean exist = false;
		if (null == current) {
			Node child = start.child;
			while (null != child) {
				if (child.chars[0] == w) {
					exist = true;
					if(child.chars.length>1){
						activePoint.index = child;
						activePoint.length++;
					}
					else if(child.chars.length==1){
						activePoint.point=child;
						activePoint.index = null;
						activePoint.length = 0;
					}
					break;
				} else {
					child = child.brother;
				}
			}
		}
		else if (current.chars.length>activePoint.length&&current.chars[activePoint.length] == w) {
			activePoint.length++;
			exist = true;
			if (current.chars.length == activePoint.length) {
				activePoint.point = current;
				activePoint.index = null;
				activePoint.length = 0;
			}
		}
		else {
			exist = false;
		}
		return exist;
	}

	/**
	 * Finds if the given string is a substring
	 *
	 * @param word
	 * @return
	 */
	public boolean select(String word) {
		char[] chars = word.toCharArray();
		int index = 0;
		Node start = root;
		for (int i = 0; i < chars.length; i++) {
			if (start.chars.length < index + 1) {
				index = 0;
				start = start.child;
				while (null != start) {
					// Compares whether the character at the specified index of the current node matches the character to be searched
					// Since it is traversing the child nodes, if not match another child node, then continue
					if (start.chars[index] == chars[i]) {
						index++;
						break;
					} else {
						start = start.brother;
					}
				}
				if (null == start) {// Returns false if the child nodes have no matching traversal
					return false;
				}
			} else if (start.chars[index] == chars[i]) {
				// If the current node to find there are comparable characters; if not, return false
				index++;
			} else {
				return false;
			}
		}
		return true;
	}
	/**
	 * Returns the label (position) of all descendant leaf nodes of the node node.
	 * @param node
	 * @return
	 */

	public static ArrayList <Integer> getNodeAllLeafSonLabel(Node node){
		ArrayList <Integer> result = new ArrayList<>();

		if(node.chars[node.chars.length-1]=='$'){
			result.add(node.label);
		}
		else{
			Node child = node.child;
			while(null!=child){
				result.addAll(getNodeAllLeafSonLabel(child));
				child=child.brother;
			}
		}
		return result;
	}

	/**
	 * Finds the longest prefix of a given string and returns the best start position 
	 * and length for its alignment effect. Returns (pos, len) For wordstartpos, 
	 * enter the position where word begins in the original sequence
	 * @param word
	 * @return
	 */
	public int[] selectPrefixForAlignment(String word, int wordstartpos) {
		int[] back = new int[2];
		back[0]=-1;
		back[1]=0;
		word=word.substring(wordstartpos);

		char[] chars = word.toCharArray();
		int index = 0;	// Find the matching location of the node
		Node start = root; // Lookup starts at the root node and traverses the child nodes

		for (int i = 0; i < chars.length; i++) {
			// If the current node has been matched, it starts at the child node and resets index == 0 at the same time
			if (start.chars.length < index + 1) {
				index = 0;
				start = start.child;

				while (null != start) {
					// Compares whether the character at the specified index of the current node matches the character to be searched
					// Since it is traversing the child nodes, if you do not match another child node to continue

					if (start.chars.length>index&&start.chars[index] == chars[i]) {
						index++;
						break;
					} else if(null != start.brother){
						start = start.brother;
					}
					else if (null == start.brother&&i>=minMatchLen){ //Broken in a node, each son did not continue to match

						Integer[] startpos= (Integer[]) getNodeAllLeafSonLabel(start.father).toArray(new Integer[getNodeAllLeafSonLabel(start.father).size()]);
						int mindis=Integer.MAX_VALUE;
						int pos_j=-1;


						for(int j=0;j<startpos.length;j++){
							int tmp=Math.abs(startpos[j].intValue()-wordstartpos)-i;
							if(tmp<mindis){
								mindis=tmp;
								pos_j=startpos[j].intValue();
							}
						}

						if(mindis<-minMatchLen){
							back[0]=pos_j;
							back[1]=i;
						}
						else{
							back[0]=-1;
							back[1]=0;
						}

						return back;
					}else if(i<minMatchLen){
						back[0]=-1;
						back[1]=0;

						return back;

					}
				}

			} else if (start.chars[index] == chars[i]) {
				// If the current node to find there are comparable characters, the comparison is, if not directly return false
				index++;
			} else if (i>=minMatchLen){
				//Broken in the middle of an edge, or leaf node, because the leaf node is $, definitely different, so at this end
				Integer[] startpos= (Integer[]) getNodeAllLeafSonLabel(start).toArray(new Integer[getNodeAllLeafSonLabel(start).size()]);
				int mindis=Integer.MAX_VALUE;
				int pos_j=-1;

				for(int j=0;j<startpos.length;j++){
					int tmp=Math.abs(startpos[j].intValue()-wordstartpos)-i;
					if(tmp<mindis){
						mindis=tmp;
						pos_j=startpos[j].intValue();

					}
				}

				if(mindis<-minMatchLen){
					back[0]=pos_j;
					back[1]=i;
				}
				else{
					back[0]=-1;
					back[1]=0;
				}

				return back;
			} else if(i<minMatchLen){
				back[0]=-1;
				back[1]=0;

				return back;
			}
		}


		Integer[] startpos= (Integer[]) getNodeAllLeafSonLabel(start).toArray(new Integer[getNodeAllLeafSonLabel(start).size()]);
		int mindis=Integer.MAX_VALUE;
		int pos_j=-1;
		for(int j=0;j<startpos.length;j++){
			int tmp=Math.abs(startpos[j].intValue()-wordstartpos);
			if(tmp<mindis){
				mindis=tmp;
				pos_j=startpos[j].intValue();
			}
		}

		back[0]=pos_j;
		back[1]=chars.length;
		return back;
	}

	/**
	 * Formatting prints the entire suffix tree
	 */
	public void print() {
		Node child = root.child;
		System.out.println("[root] [activePoint:(" + activePoint.point + "," + activePoint.index + ","
				+ activePoint.length + ")], [reminder:" + reminder + "]");
		while (child != null) {
			System.out.print("|——");
			child.print("    ");
			child = child.brother;
		}
	}

	/**
	 * The node of the suffix tree, the edge
	 * Each node chars is the father of the point to the side of the string, 
	 * the root node no father, so chars = ""
	 */
	private class Node {
		public char[] chars;
		public Node child;
		public Node brother;
		public Node father;
		public Node suffixNode;
		/*It is used to record the descendants of all descendants of the leaf node on behalf 
		* of the suffix of the starting position, if it is a leaf node is only an integer value, 
		* it represents the beginning of the suffix position */
		//public ArrayList <Integer> position;  
		/*Record leaf node represents the starting position of the suffix, 
		 *the internal node may also have a value, through the last chars is $ to determine */
		public int label;
		public Node(char[] chars) {
			this.chars = chars;
			//position = new ArrayList();
		}

		@Override
		public String toString() {
			return "Node [chars=" + String.valueOf(chars) + "]";
		}

		public void print(String prefix) {
			System.out.print(String.valueOf(chars));
			if(chars[chars.length-1]=='$'){
				System.out.print(label);
			}
			if (null != this.suffixNode) {
				System.out.println("--" + String.valueOf(this.suffixNode.chars));
			} else {
				System.out.println();
			}
			Node child = this.child;
			while (null != child) {
				System.out.print(prefix + "|——");
				child.print(prefix + prefix);
				child = child.brother;
			}
		}
	}

	/**
	 * active point, a triple: (active_node,active_edge,active_length)
	 * Active side of the father is the point, the son is the index; 
	 * the activity point should be point, extract the stored string 
	 * from the son of the chars to find (0, length)
	 */
	private class ActivePoint {
		public Node point;
		public Node index;
		public int length;

		public ActivePoint(Node point, Node index, int length) {
			this.point = point;
			this.index = index;
			this.length = length;
		}

		@Override
		public String toString() {
			return "ActivePoint [point=" + point + ", index=" + index + ", length=" + length + "]";
		}
	}
	/**
	 * String formatting, delete illegal characters (only retain agctn, all the rest 
	 * of the characters replaced by n), all converted to lowercase, u all replaced by t
	 * @param s
	 * @return
	 */
	private static String format(String s){
		s=s.toLowerCase();
		s=s.replace('u', 't');
		StringBuffer sb = new StringBuffer(s);

		for(int i=0;i<sb.length();i++){
			switch(sb.charAt(i)){
				case 'a': break;
				case 'c': break;
				case 'g': break;
				case 't': break;
				case 'n':break;
				default: sb=sb.replace(i, i+1, "n");
			}
		}


		return(sb.toString());
	}

	/**
	 * Enter a node to output a string from the edge to the node
	 * @param start
	 * @return
	 */

	public static String getNodeString(Node start){
		String s="";

		while(start.chars.length>0){
			s=String.valueOf(start.chars)+s;
			start=start.father;
		}
		s=String.valueOf(start.chars)+s;
		return s;
	}

	public static void main(String[] args) {
		SuffixTree center = new SuffixTree();
		String s1 ="ACACCGATGAGTCTGTCACGCGATAGCTCGACGCTGCACCCTATGCTCGATAGCATTGCGACC";
		s1=format(s1)+"$";
		center.build(s1);
		String word = "ACACCGATGAGTCTGTCACGCGATAGCTCGACGCTGCACCCTATGCTCGATAGCATTGCGACC";
		word = word.toLowerCase();
		System.out.println(Arrays.toString(center.selectPrefixForAlignment(word, 0)));
	}
}
