package com.msa.center;

import java.util.ArrayList;

public class NewRoot {
    /*If the two partitions have the same prefix, because the symbols in the prefix have a unique position in the tree, 
     * the position of the same symbol in P is recorded with an ArrayList's positions*/
    ArrayList<Position> positions = new ArrayList<Position>();
    char v;    	//Node represents the symbol
    NewRoot nv;  //The corresponding node (address) of the node in the dead link
    ArrayList<NewRoot> sons = new ArrayList<NewRoot>();	/*Used to record the son of all the node nodes (address)*/
    /*In order to facilitate the establishment of the key tree to determine whether the correct, 
    for each node to introduce an ID to help determine the relationship between nodes. 
    Note: you can delete later, no effect on the results*/
    int ID;	  
    /*The layer where the nodes are located. In order to get the start position of Pi in T in the AC algorithm, we set up. 
     * This position is obtained by subtracting the current node level from the position number in the current T.*/
    int level;	

    /**
     * Constructor: Initializes a root node, nv points to the root node itself
     */
    NewRoot() {
        v = 'r';
        nv = this;
        level = 0;
    }

    /**
     * Constructor: Initializes a node (usually a son) based on the current symbol.
     */
    NewRoot(char c) 
    {
        v = c;
    }

    /**
     * This method saves the new location where the symbols in the same prefix appear in P
     *
     * @param posit - The position of the symbol in P
     */
    public void setPos(int posit) {
        Position posrec = new Position();
        posrec.position = posit;
        positions.add(posrec);
    }

    /**
     * This method adds a new son to the node
     *
     * @param newSon - new son
     */
    public void addSon(NewRoot newSon) {
        sons.add(newSon);
    }

    /**
     * The method queries all the sons of the node based on the entered notation for whether or not 
     * a son already exists for the symbol. There is to return to the son (address), and then return to the empty
     * @param c - The symbol to query
     * @return son - Query results, found to return to the son, or null
     */
    public NewRoot searchSon(char c) {
        NewRoot son = null;
        for (int i = 0; i < sons.size(); i++) 
        {
            son = (NewRoot) sons.get(i);
            if (son.v == c)
            {
                break;
            }
            else
            {
                son = null;
            }
        }
        return son;
    }

}
