package com.myzone.utils;

/**
 * @author: myzone
 * @date: 05.03.13 19:30
 */
public interface Converter<S, T> extends Parser<S, T>, Renderer<S, T> {

    @Override
    T parse(S source);

    @Override
    S render(T target);

}
