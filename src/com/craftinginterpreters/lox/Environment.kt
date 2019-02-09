package com.craftinginterpreters.lox

class Environment {
    private val values = HashMap<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        when {
            values.containsKey(name.lexeme) -> return values[name.lexeme]!!
            else -> throw Interpreter.RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun assign(name: Token, value: Any?) {
        when {
            values.containsKey(name.lexeme) -> values[name.lexeme] = value
            else -> throw Interpreter.RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }
}