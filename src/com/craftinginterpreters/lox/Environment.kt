package com.craftinginterpreters.lox

class Environment(private val enclosing: Environment? = null) {
    private val values = HashMap<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    private fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 1..distance)
            environment = environment.enclosing!!

        return environment
    }

    fun getAt(distance: Int, lexeme: String): Any? {
        return ancestor(distance).values[lexeme]
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values[name.lexeme] = value
    }

    fun get(name: Token): Any? {
        return when {
            values.containsKey(name.lexeme) -> values[name.lexeme]!!
            enclosing != null -> enclosing.get(name)
            else -> throw Interpreter.RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun assign(name: Token, value: Any?) {
        when {
            values.containsKey(name.lexeme) -> values[name.lexeme] = value
            enclosing != null -> enclosing.assign(name, value)
            else -> throw Interpreter.RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }
}