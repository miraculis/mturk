package com.mturktest;

public class Tuple<X, Y> {
    private final X left;
    private final Y right;

    public static <X, Y> Tuple<X,Y> of(X left, Y right) {
        return new Tuple<>(left, right);
    }

    private Tuple(X left, Y right) {
        this.left = left;
        this.right = right;
    }

    public X getLeft() {
        return left;
    }

    public Y getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}
