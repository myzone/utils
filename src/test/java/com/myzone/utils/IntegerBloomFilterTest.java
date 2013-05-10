package com.myzone.utils;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author : myzone
 * @date: 10.05.13 Time: 3:58
 */
public class IntegerBloomFilterTest {

    private Set<String> dataSet;
    private IntegerBloomFilter<String> bloomFilter;

    @Before
    public void setUp() {
        dataSet = spy(new HashSet<>());
        bloomFilter = new IntegerBloomFilter<>(dataSet);
    }

    @Test
    public void testContains() {
        bloomFilter.add("Hello");
        bloomFilter.add("World");
        bloomFilter.add("!!!");

        assertFalse(bloomFilter.contains("Java"));
        verify(dataSet, never()).contains(any());
    }

    @Test
    public void testAdd() {
        bloomFilter.add("Java");
        bloomFilter.add("World");
        bloomFilter.add("!!!");

        assertTrue(bloomFilter.contains("Java"));
        verify(dataSet, atLeastOnce()).contains(any());
    }

    @Test
    public void testRemove() {
        bloomFilter.add("Java");
        bloomFilter.add("World");
        bloomFilter.add("!!!");
        bloomFilter.remove("Java");

        assertFalse(bloomFilter.contains("Java"));
        verify(dataSet, never()).contains(any());
    }

    @Test
    public void testClear() {
        bloomFilter.add("Java");
        bloomFilter.add("World");
        bloomFilter.add("!!!");
        bloomFilter.clear();

        assertFalse(bloomFilter.contains("Java"));
        verify(dataSet, never()).contains(any());
    }

}
