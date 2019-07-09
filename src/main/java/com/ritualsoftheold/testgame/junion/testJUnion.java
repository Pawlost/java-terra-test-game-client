package com.ritualsoftheold.testgame.junion;

import theleo.jstruct.ArrayType;
import theleo.jstruct.DirectBuffer;
import theleo.jstruct.Heap;
import theleo.jstruct.Mem;

public class testJUnion {
    public static void main(String[] args) {
        byte[] big = new @DirectBuffer byte[100];

        System.out.println("Size: "+big.length);
    }
}
