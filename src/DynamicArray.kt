package mpp.dynamicarray

import kotlinx.atomicfu.*

interface DynamicArray<E> {
    /**
     * Returns the element located in the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun get(index: Int): E

    /**
     * Puts the specified [element] into the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun put(index: Int, element: E)

    /**
     * Adds the specified [element] to this array
     * increasing its [size].
     */
    fun pushBack(element: E)

    /**
     * Returns the current size of this array,
     * it increases with [pushBack] invocations.
     */
    val size: Int
}

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(INITIAL_CAPACITY))

    private val RESIZE_FACTOR = 2

    override fun get(index: Int): E = core.value.get(index)

    override fun put(index: Int, element: E) {
        var curCore: Core<E>? = core.value
        if (curCore!!._size.value <= index) throw IllegalArgumentException()
        while (curCore != null) {
            curCore.array[index].getAndSet(element)
            curCore = curCore.next.value
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val curCore = this.core.value
            val curSize = curCore._size.value
            val curCapacity = curCore._capacity.value
            if (curSize < curCapacity) {
                if (tryInsert(element, curCore, curSize)) {
                    break
                }
            } else {
                tryResizeAndContinue(curCore, curSize, curCapacity)
            }
        }
    }

    override val size: Int get() = core.value._size.value

    private fun tryInsert(element: E, coreVal: Core<E>, curSize: Int): Boolean {
        val successfullyUpdated = coreVal.array[curSize].compareAndSet(null, element)
        coreVal._size.compareAndSet(curSize, curSize + 1)
        return successfullyUpdated
    }

    private fun tryResizeAndContinue(coreVal: Core<E>, curSize: Int, curCapacity: Int) {
        val nextCore = Core<E>(curCapacity * RESIZE_FACTOR)
        nextCore._size.compareAndSet(0, curSize)
        coreVal.next.compareAndSet(null, nextCore)
        (0 until curCapacity).forEach { i ->
            coreVal.next.value!!.array[i].compareAndSet(null, coreVal.array[i].value)
        }
        this.core.compareAndSet(coreVal, coreVal.next.value!!)
    }
}

private class Core<E>(
    capacity: Int,
) {
    val array = atomicArrayOfNulls<E>(capacity)
    val _size = atomic(0)
    val next = atomic<Core<E>?>(null)
    val _capacity = atomic(capacity)

    val size: Int = _size.value

    @Suppress("UNCHECKED_CAST")
    fun get(index: Int): E {
        require(index < _size.value)
        return array[index].value as E
    }
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME