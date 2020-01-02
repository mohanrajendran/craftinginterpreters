package com.craftinginterpreters.lox

class LoxClass(val name: String, private val methods: HashMap<String, LoxFunction>) : LoxCallable {
    override fun arity(): Int {
        val initializer = findMethod("init")
        return initializer?.arity() ?: 0
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)

        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)

        return instance
    }

    override fun toString(): String {
        return name
    }

    fun findMethod(name: String): LoxFunction? {
        return methods[name]
    }
}