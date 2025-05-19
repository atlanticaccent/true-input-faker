package com.crimes_collection.true_input_faker.input_impls

import com.crimes_collection.true_input_faker.ReflectionUtils
import org.lwjgl.opengl.InputImplementation

abstract class FakeInputImpl<T>(val inner: InputImplementation) : InputImplementation by inner {
    var disabled: Boolean = false

    abstract fun addEvent(event: T)
}

open class InputImplFactory<T : FakeInputImpl<*>>(
    val attach: InputImplFactory<T>.() -> T,
    val disconnect: InputImplFactory<T>.() -> Boolean,
) {
    inline fun <reified U> into(crossinline build: (InputImplementation) -> T) = InputImplFactory({
        build(getInputImplementation<U>()).also {
            setInputImplementation<U>(it)
            impl = it
        }
    }, {
        when (val currImpl = impl) {
            null -> false
            else -> {
                impl = null
                setInputImplementation<U>(currImpl.inner)
                true
            }
        }
    })

    constructor(other: InputImplFactory<T>) : this(other.attach, other.disconnect)

    var impl: T? = null

    fun attach() = (attach)()

    fun getOrAttach(): T {
        return when (val impl = impl) {
            null -> (attach)()
            else -> impl
        }
    }

    fun disconnect() = (disconnect)()
}

inline fun <reified T> getInputImplementation() = ReflectionUtils.getStatic(
    T::class.java, "implementation"
) as InputImplementation

inline fun <reified T> setInputImplementation(inputImplWrapper: InputImplementation) {
    ReflectionUtils.setStatic(T::class.java, "implementation", inputImplWrapper)
}