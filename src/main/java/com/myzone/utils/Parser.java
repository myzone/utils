package com.myzone.utils;

/**
 * @author: myzone
 * @date: 05.03.13 19:30
 */
public interface Parser<S, T> {

    T parse(S source);

}
