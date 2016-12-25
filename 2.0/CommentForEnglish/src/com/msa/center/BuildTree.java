package com.msa.center;

/**
 * System class, including methods, variations and so on.
 * @author Quan Zou, Shixiang Wan
 */
public class BuildTree {

    Queue Q = new Queue();	//Commonly used variables: Queue Q
    int idcounter = 1;		//Commonly used variables: ID value of the node Adder

    /**
      * Output method: used to output Aho-Corasick algorithm results
      * Deal with this category in the Q on the column, which preserved the record Aho-Corasick processing results information
      * Record class object I (see this class method ACsearch ()).
     */
    public int[][] out() 
    {
        Position p;          //A variable of the Position class that is used to read the saved location
        Record I;            //Record class variables, used to read the recorded information
        int i = 0;           //The integer i is used for loop counting
        int[][] all = new int[2][Q.size() + 1];    //#############To be amended############################33

        while (Q.size() != 0) 
        {	//When the queue is not empty, loop
            I = Q.popI();                   //Read the record of the team leader
            p = (Position) I.name.get(0);	//Read the mode number.
            all[0][i] = p.position;
            all[1][i] = I.site;
            i++;
        }
        all[0][i] = 0;
        all[1][i] = 0;
        return (all);
    }

    /**
     * Construction method: According to the beginning of each paragraph in the string P and length of the division in the keyword tree trie
     * (Possibly a subtree) to recursively establish the corresponding branch.
     *
     * @param P - A string read from the keyboard
	 * @param i - the starting position of the substring to be read in (in a segment)
	 * @param r - the length of the unread sub-string
     * @param trie - Partially created keyword tree (subtree root address)
     * @ Param pid - that this is being processed in which the P, the number from 1 onwards. This parameter
     * Purely for output convenience, in fact, as long as the end of the division, at any time can be
     * To calculate it.
     */
    public void build(String P, int i, int r, int pid, NewRoot trie) {
        if (r > 0) 
        {	//If there are no characters to read, execute the following statement
            NewRoot son = trie.searchSon(P.charAt(i)); /*In the trie's son to find whether there is a position i in the P character appears*/
            if (son == null) 
            {
                /*Create a new son node based on position and character
                Note that the following two sentences and ID related words can be deleted.*/
                son = new NewRoot(P.charAt(i));
                son.ID = idcounter;
                idcounter++;
                trie.addSon(son);
            }
            //If the last character of the segment is processed, the segment number of the segment is saved in the node.
            if (r == 1) 
            {
                son.setPos(pid);
            }
            /*Will son as a new sub-tree roots, and in accordance with the P position in the next character to continue to build, unread string length -1*/
            build(P, i + 1, r - 1, pid, son);
        }
    }

    /**
     * The method of establishing a dead link: According to the contents of the queue Q 
     * and the root trie level to establish a failure of each node link
     * 
     * @param trie - Keyword tree (root address)
     */
    public void failLink(NewRoot trie) {
        int i;                      //The integer i is used for subsequent loop counting
        int level = 1;              //The integer level is used to calculate the node level. 
        							//At the same time, it will always equal to the level of the traversal. Here is the first layer.
        Q.clear();                  //The system queue is cleared to zero

        NewRoot son;                //Create a tree node type variable son
        for (i = 0; i < trie.sons.size(); i++) 
        {   //Initializes the first level node
            son = (NewRoot) trie.sons.get(i);  //Each node, the son of the root
            son.nv = trie;           //The broken link points to the root of the tree
            son.level = level;       //Its level is 1 (because it is the first layer)
            Q.insert(son);           //Insert the son into the queue.
        }
        level++;                     //Level level plus 1, standby.

        NewRoot NewRoot= new NewRoot();         //Create a new tree node
        NewRoot.v = 'L';                /*The character of the node is 'L', that is, "layer symbol" represents a layer of the tree.
                        The symbol is used to determine whether each layer traversal ends to calculate the value of level.*/
        Q.insert(NewRoot);              //Insert the node into the queue.
        NewRoot w;                      //Create a tree node type variable w

        while (Q.size() != 0) 
        {	//When the queue is not empty
            NewRoot= Q.popfront();      //Reads the queue head node of the queue
            if (NewRoot.v != 'L') {     //If the node is not a "delimiter"
                for (i = 0; i < NewRoot.sons.size(); i++) 
                {
                    son = (NewRoot) NewRoot.sons.get(i);   //Perform the following operation for each of the sons of the node
                    w = NewRoot.nv;     //Look at the son of the father (ie, the first node) of the invalid link
                    //(This is the comment to the following sentence) If the link is not a root and does not have a son
                    while (w != trie && w.searchSon(son.v) == null) 
                    {
                    	w = w.nv;      //Next, look at the broken links for broken links
                    }
                    /*It can be seen that the loop stop condition is that the dead link is pointing to the root of the tree, 
                     * or there is a son that is calibrating the son's character*/
                    if (w.searchSon(son.v) != null) 
                    {
                    	/*If w exists, the son of the son character calibration*/
                        son.nv = w.searchSon(son.v); /*The son's broken link points to the son of w that is calibrating the son's character*/
                    } else 
                    {	//Otherwise, there is no such calibration in the son of w
                    	//The child's broken link points to the root of the tree
                    	son.nv = trie; 
                    }
                        
                    /* (This is the comment for the following if statement) If the son is not the end node of each segment, but its dead link ends up
                    Will point to the end of a Pi node (not necessarily direct), then the son of the positions inserted -1,
                    Said through the son node to find a broken link, then eventually able to find a representative of a model of the node, regardless of
                    Whether this node or pattern's tail node is a leaf. */
                    if (son.positions.size() == 0 && son.nv.positions.size() != 0)
                    {
                        son.setPos(-1);
                    }

                    son.level = level; //Save the level value to son.
                    Q.insert(son);	   //The son is stored in the queue, so that the next layer measurement layer traversal
                }
            } else 
            {	//Otherwise, if the first node of the queue is "delimiter" L,
                if (Q.size() != 0) 
                { /*See if the queue is empty, that is, just pop the first team is the last node, if not*/
                    level++;
                    /*Will have this "layer" of the node and then inserted into the team tail, 
                    so the team before the end of the tree with the layer nodes, layer number is just after 1 level*/
                    Q.insert(NewRoot);
                }
            }
        }
    }

    /**
      * Aho-Corasick search method: according to the keyword tree trie to deal with the string T
      *
      * @param T - The string to read in
      * @param trie - Creates a tree of keywords
      */
    public void ACsearch(String T, NewRoot trie) {
        int i;							//The integer i is used for loop counting
        Position p;						//The p of the Position class is used for the staging location
        NewRoot NewRoot= trie;			//The tree node Nodea is equal to the root trie
        NewRoot son;                    //Tree node variable son later.
        Record I;						//The Record type variable is used to hold the result
        Q.clear();						//The queue is cleared to zero
        for (i = 0; i < T.length(); i++) 
        {	//For each character T (i) in T,
            son = NewRoot.searchSon(T.charAt(i)); //Take a look at Nodea's son there is no calibration T (i)
            while (son == null) 
            {	////If Nodea is the root,
                if (NewRoot == trie)		
                {
                	break;
                }
                NewRoot= NewRoot.nv;        //Otherwise Nodea is equal to its dead link
                son = NewRoot.searchSon(T.charAt(i)); //Continue to look for T (i) among its sons
            }
            
            //If Nodea is the root, and just jumped out (so that son will be empty)
            if (NewRoot == trie && son == null) 
            {
                continue;					//The end of the outer loop, continue to see the next character in T
            }
            NewRoot= son;					//Nodea is equal to the son of its calibration T (i)
            if (NewRoot.positions.size() != 0) 
            {	//If the node may represent a pattern Pi
                p = (Position) NewRoot.positions.get(0); //Check the first bit of positions to determine whether the node directly represents a pattern
                if (p.position != -1) 
                {	//If the position of the first position is not -1, which means that the node directly on behalf of a model Pi
					//(This is the comment to the following sentence) the starting point of the pattern represented by this node in T is computed and recorded in I,
					//While the pattern represented by the node number is also recorded.
                    I = new Record(i + 2 - NewRoot.level, NewRoot.positions);
                    Q.insert(I);             //Save I in the queue.
                }
                son = NewRoot.nv;            //Continue to see Nodea's broken links
                /*If the link is not empty and does not point to the root node, and it may represent a pattern, 
                 * loop through the loop to find all the possible modes to which the node is connected.*/
                while (son != null && son != trie && son.positions.size() != 0) 
                {	//Look at the link in the top position
                    p = (Position) son.positions.get(0);
                    if (p.position != -1) 
                    { 	//If the first bit is not -1, that is, the failed link directly represents a pattern, then
						/*(This is the comment to the following sentence) the starting point of the pattern represented by 
                    	* this node in T is computed and recorded in I,While the pattern represented by the node number is also recorded.*/
                        I = new Record(i + 2 - son.level, son.positions);
                        Q.insert(I);		//Save I in the queue.
                        son = son.nv;
                    }
                }
            }
        }
    }
}
