package com.msa.center;

import java.util.ArrayList;

/**
 * Record type, used to save the Aho-Corasick algorithm to find matching information.
 *
 * @author ShixiangWan, Quan Zou
 */
public class Record {

    int site;
    ArrayList<Position> name = new ArrayList<Position>();
    /**
     * The constructor of the record: creates a new record based on the starting position 
     * of the matching string in T and all the corresponding patterns.
     *
     * @param st  - The starting position of this matching pattern in T.
     * @param pos - The number of all the patterns in this match.
     */
    Record(int st, ArrayList<?> pos) {
        site = st;
        Position p;
        for (int i = 0; i < pos.size(); i++) { //For each of the Position class objects in pos, add the object to name
            p = (Position) pos.get(i);
            name.add(p);
        }
    }
}
