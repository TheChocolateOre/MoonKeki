package util.collections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A list with unique elements that provides O(1) access time to the node of a
 * given element, in the average case.
 * @param <E> The type of the elements of this data structure.
 * @author Infinite Machine
 */
public class AccessList<E> implements Iterable<E> {

    /**
     * An Iterator for this AccessList.
     * @author Infinity Machine
     */
    public class MyIterator implements Iterator<E> {

        /**
         * An Iterator over the LinkedList of this AccessList.
         */
        private Iterator<E> it = AccessList.this.list.iterator();

        /**
         * The element returned by the last call of next().
         */
        private E lastElement;

        /**
         * Indicates if the Node this Iterator is pointing at has a next
         * Node.
         * @return True if the Node this Iterator is pointing at has a next
         * Node.
         */
        @Override
        public boolean hasNext() {
            return this.it.hasNext();
        }

        /**
         * Gets the element pointing by this Iterator and goes to the next
         * Node.
         * @return The element pointing by this Iterator.
         * @throws NoSuchElementException If the iteration has no more
         * elements.
         */
        @Override
        public E next() {
            //Gets the next element
            this.lastElement = this.it.next();
            return this.lastElement;
        }

        /**
         * Removes the element returned by the previous call to next().
         * @throws IllegalStateException If next() has not yet been called, or
         * remove() has already been called after the last call to next().
         */
        @Override
        public void remove() {
            //Removes the element from the LinkedList
            this.it.remove();
            //Removes the last element from the HashMap
            AccessList.this.nodeMap.remove(this.lastElement);
        }

    }//end inner class MyIterator

    /**
     * A simple double linked list. It does not permit null values.
     * @param <E> The type of the elements of this data structure.
     * @author Infinity Machine
     */
    private static class LinkedList<E> implements Iterable<E> {

        /**
         * This class represents a node in a double linked list.
         * @param <T> The type of the element this Node stores.
         */
        private static class Node<T> {

            /**
             * The data this Node stores.
             */
            private T data;

            /**
             * A reference to the previous Node of this Node.
             */
            private Node<T> previous;

            /**
             * A reference to the next Node of this Node.
             */
            private Node<T> next;

            /**
             * Swaps the data between 2 given Node's'.
             * @param n1 The 1st Node.
             * @param n2 The 2nd Node.
             * @deprecated The Node's' must contain their initial data through
             * their life span, in order to keep the HashMap properly updated.
             */
            @Deprecated
            private static <T> void swapData(Node<T> n1,
                    Node<T> n2) {
                T temp = n1.getData();
                n1.setData(n2.getData());
                n2.setData(temp);
            }

            /**
             * Constructs a Node given its data.
             * @param data The data of this Node.
             */
            Node(T data) {
                this.data = data;
            }

            /**
             * Gets the data of this Node.
             * @return The data of this Node.
             */
            T getData() {
                return this.data;
            }

            /**
             * Sets the data of this Node.
             * @param data The new data of this Node.
             */
            void setData(T data) {
                this.data = data;
            }

            /**
             * Gets the previous Node of this Node.
             * @return The previous Node of this Node.
             */
            Node<T> getPrevious() {
                return this.previous;
            }

            /**
             * Sets the previous Node of this Node.
             * @param previous The new previous Node of this Node.
             */
            void setPrevious(Node<T> previous) {
                this.previous = previous;
            }

            /**
             * Indicates if this Node has a previous Node.
             * @return True if this Node has a previous Node, otherwise false.
             */
            boolean hasPrevious() {
                return this.getPrevious() != null;
            }

            /**
             * Gets the next Node of this Node.
             * @return The next Node of this Node.
             */
            Node<T> getNext() {
                return this.next;
            }

            /**
             * Sets the next Node of this Node.
             * @param next The new next Node of this Node.
             */
            void setNext(Node<T> next) {
                this.next = next;
            }

            /**
             * Indicates if this Node has a next Node.
             * @return True if this Node has a next Node, otherwise false.
             */
            boolean hasNext() {
                return this.getNext() != null;
            }

        }//end inner class Node

        /**
         * An Iterator for this LinkedList.
         * @author Infinity Machine
         */
        private class MyIterator implements Iterator<E> {

            /**
             * The Node this Iterator is currently pointing at.
             */
            private Node<E> cursor = LinkedList.this.getHead();

            /**
             * Indicates if this Iterator can call remove(). True if it can
             * remove, otherwise false.
             */
            private boolean remove;

            /**
             * Indicates if the Node this Iterator is pointing at has a next
             * Node.
             * @return True if the Node this Iterator is pointing at has a next
             * Node.
             */
            @Override
            public boolean hasNext() {
                return this.cursor != null;
            }

            /**
             * Gets the element pointing by this Iterator and goes to the next
             * Node.
             * @return The element pointing by this Iterator.
             * @throws NoSuchElementException If the iteration has no more
             * elements.
             */
            @Override
            public E next() {
                //Validates that this Iterator has a next element
                if (!this.hasNext()) {
                    throw new NoSuchElementException("There are no more " +
                            "elements to iterate.");
                }//end if

                //Gets the element of the cursor
                E element = this.cursor.getData();
                //Gets the next Node of the Node this cursor currently is
                //pointing at
                this.cursor = this.cursor.getNext();

                //Indicates that the remove() method can be called
                this.remove = true;
                return element;
            }

            /**
             * Removes the element returned by the previous call to next().
             * @throws IllegalStateException If next() has not yet been called,
             * or remove() has already been called after the last call to
             * next().
             */
            @Override
            public void remove() {
                //Validates that this Iterator can remove an element
                if (!this.canRemove()) {
                    throw new IllegalStateException("For every remove() call " +
                            "there must precede a next() call.");
                }//end if

                //Checks if this Iterator came to its end
                if (this.cursor != null) {
                    //Removes the previous Node of the Node pointed by
                    //this.cursor
                    LinkedList.this.remove(this.cursor.getPrevious());
                } else {
                    //Removes the last element of this LinkedList
                    LinkedList.this.pop();
                }//end if
                //Indicates that the remove() method can not be called
                this.remove = false;
            }

            /**
             * Indicates if this Iterator can call remove(). True if it can
             * remove, otherwise false.
             */
            private boolean canRemove() {
                return this.remove;
            }

        }//end inner class MyIterator

        /**
         * The head of this LinkedList.
         */
        private Node<E> head;

        /**
         * The tail of this LinkedList.
         */
        private Node<E> tail;

        /**
         * How many elements this LinkedList has.
         */
        private int size;

        /**
         * Gets an iterator for this LinkedList pointed at its head.
         * @return An iterator for this LinkedList pointed at its head.
         */
        public MyIterator iterator() {
            return new MyIterator();
        }

        /**
         * How many elements this LinkedList has.
         * @return The number of elements of this LinkedList.
         */
        private int size() {
            return this.size;
        }

        /**
         * Indicates if this LinkedList has no elements.
         * @return True if this LinkedList has no elements, otherwise false.
         */
        private boolean isEmpty() {
            return this.size() == 0;
        }

        /**
         * Adds an element after the tail of this LinkedList. Runs in O(1) time
         * for the worst case.
         * @param element An element to be added in this LinkedList.
         * @return The new Node with the new element.
         */
        private Node<E> add(E element) {
            //Checks if this LinkedList has no elements
            if (this.isEmpty()) {
                //Creates a new Node to store the element
                Node<E> newNode = new Node<>(element);
                this.head = newNode;
                this.tail = this.head;

                //Increments the size of this LinkedList by 1
                ++size;
                return newNode;
            } else {
                //Adds the new element after the tail of this LinkedList
                return this.addAfter(this.getTail(), element);
            }//end if
        }

        /**
         * Removes the given Node from this LinkedList. The given Node must
         * belong in this LinkedList.
         * @param node The Node to remove from this LinkedList.
         */
        private void remove(Node<E> node) {
            //Checks if node has a previous Node
            if (node.hasPrevious()) {
                Node<E> prev = node.getPrevious();
                prev.setNext(node.getNext());
            } else {
                this.head = node.getNext();
            }//end if

            //Checks if node has a next Node
            if (node.hasNext()) {
                Node<E> next = node.getNext();
                next.setPrevious(node.getPrevious());
            } else {
                this.tail = node.getPrevious();
            }//end if

            //Deletes the link to the previous Node of node
            node.setPrevious(null);
            //Deletes the link to the next Node of node
            node.setNext(null);
            //Decrements the size of this LinkedList by 1
            --size;
        }

        /**
         * Removes the last (tail) element of this LinkedList.
         * @return The last (tail) element of this LinkedList. Returns null if
         * this LinkedList has no elements.
         */
        private E pop() {
            //Checks if this LinkedList has no elements
            if (this.isEmpty()) {
                return null;
            }//end if

            //The data of the tail of this LinkedList
            E element = this.getTail().getData();
            //Removes the tail of this LinkedList
            this.remove(this.getTail());
            return element;
        }

        /**
         * Removes all the elements of this LinkedList.
         */
        private void clear() {
            this.head = null;
            this.tail = null;
            this.size = 0;
        }

        /**
         * Adds the given element after a given Node. Runs in O(1) time for the
         * worst case.
         * @param pivot The Node to add element after it.
         * @param element The element to add, after Node.
         * @return The new Node with the new element.
         */
        private Node<E> addAfter(Node<E> pivot,
                                          E element) {
            return this.addAfter(pivot, new Node<>(element));
        }

        /**
         * Adds the given Node after a given pivot Node. The given Node node
         * must not belong in this LinkedList.
         * @param newNode The Node to be added after pivot.
         * @param pivot The Node to add element after it.
         * @return The final Node that will hold the element stored in the given
         * one.
         */
        private Node<E> addAfter(Node<E> pivot,
                                          Node<E> newNode) {
            //Checks if pivot does not have a next Node
            if (!pivot.hasNext()) {
                pivot.setNext(newNode);
                newNode.setPrevious(pivot);
                this.tail = newNode;
            } else {//pivot has a next Node
                //Gets the next Node of pivot
                Node<E> nodeNext = pivot.getNext();
                pivot.setNext(newNode);
                newNode.setPrevious(pivot);
                newNode.setNext(nodeNext);
                nodeNext.setPrevious(newNode);
            }//end if

            //Increments the size of this LinkedList by 1
            ++size;
            return newNode;
        }

        /**
         * Adds the given element before a given Node. Runs in O(1) time for the
         * worst case.
         * @param pivot The Node to add element before it.
         * @param element The element to add, before Node.
         * @return The new Node with the new element.
         */
        private Node<E> addBefore(Node<E> pivot,
                                           E element) {
            return this.addBefore(pivot, new Node<>(element));
        }

        /**
         * Adds the given Node before a given pivot Node. The given Node node
         * must not belong in this LinkedList.
         * @param newNode The Node to be added before pivot.
         * @param pivot The Node to add element before it.
         * @return The final Node that will hold the element stored in the given
         * one.
         */
        private Node<E> addBefore(Node<E> pivot,
                                           Node<E> newNode) {
            //Checks if pivot does not have a previous Node
            if (!pivot.hasPrevious()) {
                pivot.setPrevious(newNode);
                newNode.setNext(pivot);
                this.head = newNode;
            } else {//pivot has a previous Node
                //Gets the previous Node of pivot
                Node<E> nodePrev = pivot.getPrevious();
                pivot.setPrevious(newNode);
                newNode.setNext(pivot);
                newNode.setPrevious(nodePrev);
                nodePrev.setNext(newNode);
            }//end if

            //Increments the size of this LinkedList by 1
            ++size;
            return newNode;
        }

        /**
         * Moves the given Node 1 Node at the tail direction (as indicated by
         * the <>Node.next()</> method). If the given node is the tail of this
         * LinkedList, this method does nothing. The given Node must belong in
         * this LinkedList.
         * @param node The Node to move.
         * @return The final Node that will hold the element stored in the given
         * one.
         */
        private Node<E> moveForward(Node<E> node) {
            //Checks if node has no next node
            if (!node.hasNext()) {
                return node;
            }//end if

            //Makes node the next of its next Node
            return this.moveAfter(node.getNext(), node);
        }

        /**
         * Moves the given Node 1 Node at the head direction (as indicated by
         * the <>Node.previous()</> method). If the given node is the head of
         * this LinkedList, this method does nothing. The given Node must belong
         * in this LinkedList.
         * @param node The Node to move.
         * @return The final Node that will hold the element stored in the given
         * one.
         */
        private Node<E> moveBackward(Node<E> node) {
            //Checks if node has no previous node
            if (!node.hasPrevious()) {
                return node;
            }//end if

            //Makes node the previous of its previous Node
            return this.moveBefore(node.getPrevious(), node);
        }

        /**
         * Makes node the head of this LinkedList.
         * @param node The Node to make it the head of this LinkedList.
         * @return The final Node that will hold the element stored in node.
         */
        private Node<E> moveToHead(Node<E> node) {
            return this.moveBefore(this.getHead(), node);
        }

        /**
         * Makes node the tail of this LinkedList.
         * @param node The Node to make it the tail of this LinkedList.
         * @return The final Node that will hold the element stored in node.
         */
        private Node<E> moveToTail(Node<E> node) {
            return this.moveAfter(this.getTail(), node);
        }

        /**
         * Moves node exactly before pivot. Both Node's' must belong in this
         * LinkedList.
         * @param pivot The Node to move node before it.
         * @param node The Node to move before pivot.
         * @return The final Node that will hold the element stored in node.
         */
        private Node<E> moveBefore(Node<E> pivot,
                                            Node<E> node) {
            //Checks if pivot and node are the same Node
            if (pivot == node) {
                //Nothing to do
                return node;
            }//end if

            //Removes node from its current position
            this.remove(node);
            return this.addBefore(pivot, node);
        }

        /**
         * Moves node exactly after pivot. Both Node's' must belong in this
         * LinkedList.
         * @param pivot The Node to move node after it.
         * @param node The Node to move after pivot.
         * @return The final Node that will hold the element stored in node.
         */
        private Node<E> moveAfter(Node<E> pivot,
                                           Node<E> node) {
            //Checks if pivot and node are the same Node
            if (pivot == node) {
                //Nothing to do
                return node;
            }//end if

            //Removes node from its current position
            this.remove(node);
            return this.addAfter(pivot, node);
        }

        /**
         * Gets the head of this LinkedList.
         * @return The head of this LinkedList.
         */
        private Node<E> getHead() {
            return this.head;
        }

        /**
         * Gets the tail of this LinkedList.
         * @return The tail of this LinkedList.
         */
        private Node<E> getTail() {
            return this.tail;
        }

    }//end innerclass LinkedList

    private LinkedList<E> list = new LinkedList<>();

    /**
     * A HashMap with the elements of this AccessList as keys and their Node's'
     * as values.
     */
    private HashMap<E, LinkedList.Node<E>> nodeMap;

    /**
     * Constructs an AccessList with the default initial capacity.
     */
    public AccessList() {
        this.nodeMap = new HashMap<>();
    }

    /**
     * Constructs an AccessList given how many elements it will store, as a
     * starting point. It will grown if size is needed.
     * @param capacity For how many elements to reserve space.
     */
    public AccessList(int capacity) {
        this.nodeMap = new HashMap<>(capacity);
    }

    /**
     * Adds an element at the tail of this AccessList. If element already exists
     * in this AccessList, this method does nothing.
     * @param element The new element to add.
     */
    public void add(E element) {
        //Checks if element is contained in this AccessList
        if (this.contains(element)) {
            return;
        }//end if

        //Adds element at the LinkedList
        LinkedList.Node<E> node = this.list.add(element);
        //Updates the HashMap with the element
        this.nodeMap.put(element, node);
    }

    /**
     * Adds an element before a given pivot, towards the head of this
     * {@code AccessList}. If element already exists in this {@code AccessList},
     * this method will move it before pivot. The pivot element must belong in
     * this {@code AccessList}.
     * @param pivot The element in this {@code AccessList} to add element
     * parameter before it.
     * @param element The new element to add before pivot.
     */
    public void addBefore(E pivot, E element) {
        this.add(element);
        this.moveBefore(pivot, element);
    }

    /**
     * Adds an element after a given pivot, towards the tail of this
     * {@code AccessList}. If element already exists in this {@code AccessList},
     * this method will move it after pivot. The pivot element must belong in
     * this {@code AccessList}.
     * @param pivot The element in this {@code AccessList} to add element
     * parameter after it.
     * @param element The new element to add after pivot.
     */
    public void addAfter(E pivot, E element) {
        this.add(element);
        this.moveAfter(pivot, element);
    }

    /**
     * Removes the given element from this AccessList. The element must already
     * belong in this AccessList.
     * @param element The element to remove from this AccessList.
     * @throws IllegalArgumentException If element does not belong in this
     * AccessList prior to this method invocation.
     */
    public void remove(E element) {
        //Validates that element belongs in this AccessList
        if (!this.nodeMap.containsKey(element)) {
            throw new IllegalArgumentException("Argument element does not " +
                    "belong in this AccessList.");
        }//end if

        //Removes the element from the LinkedList
        this.list.remove(this.nodeMap.get(element));
        //Removes the element from the HashMap
        this.nodeMap.remove(element);
    }

    /**
     * Indicates if this AccessList contains the given element.
     * @param element The element to check if it belongs in this AccessList.
     * @return True if element belongs in this AccessList, otherwise false.
     */
    public boolean contains(E element) {
        return this.nodeMap.containsKey(element);
    }

    /**
     * Removes all the elements of this AccessList.
     */
    public void clear() {
        //Removes all the elements of the LinkedList
        this.list.clear();
        //Removes all the elements of the HashMap
        this.nodeMap.clear();
    }

    /**
     * Moves the given element 1 Node at the tail direction. If the given node
     * is the tail of this list, this method does nothing. The given element
     * must belong in this list.
     * @param element The element to move.
     */
    public void moveForward(E element) {
        //Gets the node of the element
        LinkedList.Node<E> node = this.nodeMap.get(element);
        //Moves node forward in the list
        LinkedList.Node<E> endNode = this.list.moveForward(node);
        //Refreshes the node in the HashMap
        this.nodeMap.put(element, endNode);
    }

    /**
     * Moves the given element 1 Node at the head direction. If the given node
     * is the head of this list, this method does nothing. The given element
     * must belong in this list.
     * @param element The element to move.
     */
    public void moveBackward(E element) {
        //Gets the node of the element
        LinkedList.Node<E> node = this.nodeMap.get(element);
        //Moves node backward in the list
        LinkedList.Node<E> endNode = this.list.moveBackward(node);
        //Refreshes the node in the HashMap
        this.nodeMap.put(element, endNode);
    }

    /**
     * Makes the given element the head of this list. The element must belong in
     * this list.
     * @param element The element to make it the head of this list.
     */
    public void moveToHead(E element) {
        //Gets the node of the element
        LinkedList.Node<E> node = this.nodeMap.get(element);
        //Moves node at the head of the list
        LinkedList.Node<E> endNode = this.list.moveToHead(node);
        //Refreshes the node in the HashMap
        this.nodeMap.put(element, endNode);
    }

    /**
     * Makes the given element the tail of this list. The element must belong in
     * this list.
     * @param element The element to make it the tail of this list.
     */
    public void moveToTail(E element) {
        //Gets the node of the element
        LinkedList.Node<E> node = this.nodeMap.get(element);
        //Moves node at the tail of the list
        LinkedList.Node<E> endNode = this.list.moveToTail(node);
        //Refreshes the node in the HashMap
        this.nodeMap.put(element, endNode);
    }

    /**
     * Moves the element before a given pivot element in the list. Both the
     * pivot and element must belong in this list.
     * @param pivot The element to move element before it.
     * @param element The element to make it the previous of pivot.
     */
    public void moveBefore(E pivot, E element) {
        //Gets the Node of the pivot
        LinkedList.Node<E> pNode = this.nodeMap.get(pivot);
        //Gets the Node of the element
        LinkedList.Node<E> eNode = this.nodeMap.get(element);
        //Makes eNode the previous Node of pNode
        LinkedList.Node<E> endNode = this.list.moveBefore(pNode, eNode);
        //Refreshes the eNode in the HashMap
        this.nodeMap.put(element, endNode);
    }

    /**
     * Moves the element after a given pivot element in the list. Both the pivot
     * and element must belong in this list.
     * @param pivot The element to move element after it.
     * @param element The element to make it the next of pivot.
     */
    public void moveAfter(E pivot, E element) {
        //Gets the Node of the pivot
        LinkedList.Node<E> pNode = this.nodeMap.get(pivot);
        //Gets the Node of the element
        LinkedList.Node<E> eNode = this.nodeMap.get(element);
        //Makes eNode the next Node of pNode
        LinkedList.Node<E> endNode = this.list.moveAfter(pNode, eNode);
        //Refreshes the eNode in the HashMap
        this.nodeMap.put(element, endNode);
    }

    /**
     * Gets the previous element of a given element or null if it has no
     * previous element.
     * @param element An element of this {@code AccessList}, to find its
     * previous element.
     * @return The previous element of a given element or null if it has no
     * previous element.
     * @throws IllegalArgumentException If this {@code AccessList} does not
     * contain the given element.
     */
    public E getPrevious(E element) {
        LinkedList.Node<E> pNode = this.nodeMap.get(element);
        if (null == pNode) {
            throw new IllegalArgumentException("Argument element is not " +
                    "contained in this AccessList.");
        }//end if

        return pNode.hasPrevious() ? pNode.getPrevious().getData() : null;
    }

    /**
     * Gets the next element of a given element or null if it has no next
     * element.
     * @param element An element of this {@code AccessList}, to find its next
     * element.
     * @return The next element of a given element or null if it has no next
     * element.
     * @throws IllegalArgumentException If this {@code AccessList} does not
     * contain the given element.
     */
    public E getNext(E element) {
        LinkedList.Node<E> pNode = this.nodeMap.get(element);
        if (null == pNode) {
            throw new IllegalArgumentException("Argument element is not " +
                    "contained in this AccessList.");
        }//end if

        return pNode.hasNext() ? pNode.getNext().getData() : null;
    }

    /**
     * Indicates if the given element lies somewhere before a given pivot.
     * @param pivot The pivot to test if parameter element lies somewhere before
     * it.
     * @param element An element to test if it lies somewhere before pivot.
     * @implNote The time complexity of this operation is: <li>Best: O(1)</li>
     * <li>Average: O(n)</li> <li>Worst: O(n)</li>
     * @return True if element lies somewhere before pivot, otherwise false.
     * @throws IllegalArgumentException If pivot is not contained in this
     * {@code AccessList}.
     * @throws IllegalArgumentException If element is not contained in this
     * {@code AccessList}.
     */
    public boolean precedes(E pivot, E element) {
        LinkedList.Node<E> node = this.nodeMap.get(pivot);
        if (null == node) {
            throw new IllegalArgumentException("Argument pivot must be " +
                    "contained int this AccessList.");
        }//end if

        if (!this.contains(element)) {
            throw new IllegalArgumentException("Argument element must be " +
                    "contained int this AccessList.");
        }//end if

        while (node.hasPrevious()) {
            node = node.getPrevious();
            if (node.getData().equals(element)) {
                return true;
            }//end if
        }//end while

        return false;
    }

    /**
     * Indicates if the given element lies somewhere after a given pivot.
     * @param pivot The pivot to test if parameter element lies somewhere after
     * it.
     * @param element An element to test if it lies somewhere after pivot.
     * @implNote The time complexity of this operation is: <li>Best: O(1)</li>
     * <li>Average: O(n)</li> <li>Worst: O(n)</li>
     * @return True if element lies somewhere after pivot, otherwise false.
     * @throws IllegalArgumentException If pivot is not contained in this
     * {@code AccessList}.
     * @throws IllegalArgumentException If element is not contained in this
     * {@code AccessList}.
     */
    public boolean succeeds(E pivot, E element) {
        LinkedList.Node<E> node = this.nodeMap.get(pivot);
        if (null == node) {
            throw new IllegalArgumentException("Argument pivot must be " +
                    "contained int this AccessList.");
        }//end if

        if (!this.contains(element)) {
            throw new IllegalArgumentException("Argument element must be " +
                    "contained int this AccessList.");
        }//end if

        while (node.hasNext()) {
            node = node.getNext();
            if (node.getData().equals(element)) {
                return true;
            }//end if
        }//end while

        return false;
    }

    /**
     * Gets an iterator for this AccessList pointed at its head.
     * @return An iterator for this AccessList pointed at its head.
     */
    public Iterator<E> iterator() {
        return new MyIterator();
    }

    /**
     * Gets the number of elements of this AccessList.
     * @return How many elements this AccessList contains.
     */
    public int size() {
        return this.list.size();
    }

    /**
     * Indicates if this AccessList has no elements.
     * @return True if this AccessList has no elements, otherwise false.
     */
    public boolean isEmpty() {
        return this.size() == 0;
    }

    /**
     * Constructs a String representation of this AccessList.
     * @return A String representation of this AccessList.
     */
    @Override
    public String toString() {
        //Checks if this AccessList has no elements
        if (this.isEmpty()) {
            return "[]";
        }//end if

        //Creates a StringBuilder to construct the String
        StringBuilder sb = new StringBuilder(2 + 3 * this.size());
        //Gets an Iterator over the elements of this AccessList
        Iterator<E> itr = this.iterator();
        //Gets the first element of itr
        E next = itr.next();

        //Constructs the 1st segment of the String representation of this
        //AccessList
        sb.append('[').append(next != this ? next : "this");
        //Iterates the remaining elements of AccessList
        while (itr.hasNext()) {
            //Gets to the next element
            next = itr.next();
            //Appends the current element in the StringBuilder
            sb.append(',').append(' ').append(next != this ? next : "this");
        }//end while
        //Closes the String representation of this AccessList
        sb.append(']');

        return sb.toString();
    }

}//end class AccessList
