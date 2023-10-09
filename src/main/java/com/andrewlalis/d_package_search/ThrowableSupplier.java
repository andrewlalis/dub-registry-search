package com.andrewlalis.d_package_search;

@FunctionalInterface
public interface ThrowableSupplier<T> {
    T get() throws Exception;
}
