package me.eggdev.eggbot.memory;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;

public class FIFOList<T> extends LinkedList<T> {

    private final int capacity;

    public FIFOList(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    public FIFOList(int capacity, @NotNull Collection<? extends T> c) {
        super(c);
        this.capacity = capacity;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @param t the element to add
     */
    @Override
    public void addLast(T t) {
        super.addLast(t);
        if (size() > capacity)
            removeFirst();
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>This method is equivalent to {@link #addLast}.
     *
     * @param t element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    @Override
    public boolean add(T t) {
        boolean added = super.add(t);
        if (size() > capacity)
            removeFirst();
        return added;
    }

    public int getCapacity() {
        return capacity;
    }

}
