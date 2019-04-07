package com.craftinginterpreters.lox

class LoxFunction(private val declaration: Stmt.Function) : LoxCallable {
    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(interpreter.globals)
        declaration.params.forEachIndexed { index, token ->
            environment.define(token.lexeme, arguments[index])
        }

        interpreter.executeBlock(declaration.body, environment)
        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }

}