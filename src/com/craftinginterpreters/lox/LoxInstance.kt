package com.craftinginterpreters.lox

class LoxInstance(private val klass: LoxClass) {
    override fun toString(): String {
        return "${klass.name} instance"
    }
}
