/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app;

import java.util.List;

/**
 * Common object comparision helpers that are used all the time.
 */
public class EqHelper
{
    /**
     * Check equals of two objects.
     *
     * @param o1 the object to check o2 against
     * @param o2 the object to check o1 against
     * @return true, if both objects are equal (both null is considered equal)
     */
    public static boolean checkEquals(Object o1, Object o2)
    {
        if (o1 == null && o2 != null)
            return false;
        if (o2 == null && o1 != null)
            return false;

        if (o1 == null)
            return true;

        return o1.equals(o2);
    }
    
    /**
     * Compare two byte arrays for equality
     *
     * @param a1 the a1
     * @param a2 the a2
     * @return true, if they are equal (both null is considered equal)
     */
    public static boolean byteArrayEquals(byte[] a1, byte[] a2)
    {
        return byteArrayEquals(a1, 0, a2, 0, a1 == null ? 0 : a1.length);
    }
    
    /**
     * Compare two byte arrays for equality
     *
     * @param a1 the a1
     * @param a1Pos starting compare source index
     * @param a2 the a2
     * @param a2Pos starting compare dest index
     * @param length the length
     * @return true, if successful
     */
    @SuppressWarnings("null")
    public static boolean byteArrayEquals(byte[] a1, int a1Pos, byte[] a2, int a2Pos, int length)
    {
        if (a1 == null && a2 != null)
            return false;
        if (a2 == null && a1 != null)
            return false;
        if (a1 == null)
            return true;
        if(a1.length < a1Pos + length)
            return false;
        if(a2.length < a2Pos + length)
            return false;
        for(int i=a1Pos, j=a2Pos; i < length; i++, j++)
        {
            if(a1[i] != a2[j])
                return false;
        }
        return true;
    }
    /**
     * Compare two int arrays for equality
     *
     * @param a1 the a1
     * @param a2 the a2
     * @return true, if they are equal (both null is considered equal)
     */
    public static boolean intArrayEquals(int[] a1, int[] a2)
    {
        return intArrayEquals(a1, 0, a2, 0, a1 == null ? 0 : a1.length);
    }
    
    /**
     * Compare two int arrays for equality
     *
     * @param a1 the a1
     * @param a1Pos starting compare source index
     * @param a2 the a2
     * @param a2Pos starting compare dest index
     * @param length the length
     * @return true, if successful
     */
    @SuppressWarnings("null")
    public static boolean intArrayEquals(int[] a1, int a1Pos, int[] a2, int a2Pos, int length)
    {
        if (a1 == null && a2 != null)
            return false;
        if (a2 == null && a1 != null)
            return false;
        if (a1 == null)
            return true;
        if(a1.length < a1Pos + length)
            return false;
        if(a2.length < a2Pos + length)
            return false;
        for(int i=a1Pos, j=a2Pos; i < length; i++, j++)
        {
            if(a1[i] != a2[j])
                return false;
        }
        return true;
    }
    
    /**
     * Checks if all the key/values found in l1 are also found in l2.
     * Not order specific.
     *
     * @param l1 the l1
     * @param l2 the l2
     * @return true, if successful
     */
    @SuppressWarnings("null")
    public static boolean checkListEqualsUnordered(List<?> l1, List<?> l2)
    {
        if (l1 == null && l2 != null)
            return false;
        if (l2 == null && l1 != null)
            return false;
        if (l1 == null)
            return true;
        int size = l1.size();
        if(size != l2.size())
            return false;
        
        // first try an ordered compare
        boolean match = true;
        int lastFound = -1;
        for(int i=0; i < size; i++)
        {
            if(l1.get(i).equals(l2.get(i)))
                ++lastFound;
            else
            {
                match = false;
                break;
            }
        }
        if(match)
            return true;
    
        // otherwise we only need to look from ++lastFound on up
        ++lastFound;
        boolean[] alreadyFound = new boolean[size];
        for(int i=lastFound; i < size; i++)
        {
            Object l1Obj = l1.get(i);
            boolean found = false;
            for(int j=lastFound; j < size; j++)
            {
                if(alreadyFound[j])
                    continue;
                found = l1Obj.equals(l2.get(j));
                if(found)
                {
                    alreadyFound[j] = true;
                    break;
                }
            }
            if(!found)
                return false;
        }
        return true;
    }
}
