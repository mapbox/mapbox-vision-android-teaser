package com.mapbox.vision.examples.utils.classification


class Tracker<T>(private val maxCapacity: Int) {

    private var items = LinkedHashMap<Int, Item<T>>()

    data class Item<T>(val payload: T) {
        var seenCounter = 0
        var expirationCounter = -1

        fun resetTimer() {
            expirationCounter = -1
            seenCounter += 1
        }

        fun updateTimer() {
            expirationCounter += 1
        }
    }

    fun update(batch: List<T>) {

        // add new elements
        batch.forEach {
            val hash = it!!.hashCode()
            if (items[hash] == null) {
                items[hash] = Item(it)
            } else {
                if (items[hash]!!.expirationCounter > OLD_PAYLOAD_BORDER) {
                    val oldItemHash = hash + OLD_PAYLOAD_HASH_SHIFT
                    items[oldItemHash] = items[hash]!!
                    items[hash] = Item(it)
                }
            }
            items[hash]?.resetTimer()
        }

        // update counters
        items.keys.forEach { items[it]?.updateTimer() }

        // remove expired
        items = LinkedHashMap(items.filter {
            it.value.expirationCounter < EXPIRY_DEADLINE
        })
    }

    fun getCurrent(): List<T> {
        // filter out ones that aren't seen enough
        val current = items.values.filter {
            it.seenCounter >= SEEN_FILTER
        }

        // trim out old items to fit maxCapacity
        if (current.size > maxCapacity) {
            current.drop(current.size - maxCapacity)
        }

        return current.map { it.payload }
    }

    companion object {
        private const val EXPIRY_DEADLINE = 10 * 30
        private const val SEEN_FILTER = 5

        private const val OLD_PAYLOAD_HASH_SHIFT = 100
        private const val OLD_PAYLOAD_BORDER = 90
    }
}