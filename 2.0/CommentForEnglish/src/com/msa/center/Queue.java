package com.msa.center;

import java.util.ArrayList;

/**
 * The queue type, used to store the visited node (address) when traversing hierarchically.
 * @author ShixiangWan, Quan Zou
 */
public class Queue {

    ArrayList<Object> Q = new ArrayList<Object>();

    /**
     * Insert the queue method: that is inserted at the end of a tree node
     *
     * @param node - tree node
     */
    public void insert(NewRoot node) {
        Q.add(node);
    }

    /**
     * Queue insertion method: that is inserted in the tail of a record AC algorithm processing information nodes
     *
     * @param I - Record types of nodes
     */
    public void insert(Record I) {
        Q.add(I);
    }

    /**
     * Queue pop-up method, that is read from the first team and delete a tree node
     *
     * @return The tree node of the first team
     */
    public NewRoot popfront() {
        NewRoot node;
        if (Q.size() == 0) 
        {
            node = null;
        }
        else {
            node = (NewRoot) Q.get(0);
            Q.remove(0);
        }
        return node;
    }

    /**
     * Queue pops up: Reads and deletes a Record type node from the head of the queue.
     *
     * @return Record the first class object
     */
    public Record popI() {
        Record I;
        I = (Record) Q.get(0);
        Q.remove(0);
        return I;
    }

    /**
     * Queue length method, the use of ArrayList provided by the size () method returns the queue length
     *
     * @return Queue length
     */
    public int size() {
        return Q.size();
    }

    /**
     * Clear the queue method, the use of ArrayList provides clear () method to clear the queue
     */
    public void clear() {
        Q.clear();
    }
}
