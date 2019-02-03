package com.craftinginterpreters.lox

class Interpreter : Expr.Visitor<Any?> {
    class RuntimeError(val token: Token, message: String) : RuntimeException(message)

    fun interpret(expression: Expr) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.GREATER -> {
                val (dLeft, dRight) = checkNumberOperands(expr.operator, left, right)
                dLeft > dRight
            }
            TokenType.GREATER_EQUAL -> {
                val (dLeft, dRight) = checkNumberOperands(expr.operator, left, right)
                dLeft >= dRight
            }
            TokenType.LESS -> {
                val (dLeft, dRight) = checkNumberOperands(expr.operator, left, right)
                dLeft < dRight
            }
            TokenType.LESS_EQUAL -> {
                val (dLeft, dRight) = checkNumberOperands(expr.operator, left, right)
                dLeft <= dRight
            }

            TokenType.MINUS -> {
                val (dLeft, dRight) = checkNumberOperands(expr.operator, left, right)
                dLeft - dRight
            }
            TokenType.PLUS -> {
                return if (left is Double && right is Double) {
                    left + right
                } else if (left is String || right is String) {
                    stringify(left) + stringify(right)
                } else {
                    null
                }
            }

            TokenType.SLASH -> {
                val (dLeft, dRight) = checkNumberOperands(expr.operator, left, right)
                dLeft / dRight
            }
            TokenType.STAR -> {
                val (dLeft, dRight) = checkNumberOperands(expr.operator, left, right)
                dLeft * dRight
            }

            TokenType.BANG_EQUAL -> !isEqual(left, right)
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            else -> null
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> -checkNumberOperand(expr.operator, right)
            TokenType.BANG -> !isTruthy(right)
            else -> null
        }
    }

    private fun checkNumberOperand(operator: Token, operand: Any?): Double {
        when (operand) {
            is Double -> return operand
            else -> throw RuntimeError(operator, "Operand must be a number.")
        }
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?): Pair<Double, Double> {
        when {
            left is Double && right is Double -> return Pair(left, right)
            else -> throw RuntimeError(operator, "Operands must be numbers.")
        }
    }

    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            else -> true
        }
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        return when {
            a == null && b == null -> true
            a == null -> false
            else -> a == b
        }
    }

    private fun stringify(obj: Any?): String {
        return when (obj) {
            null -> "nil"
            is Double -> {
                var text = obj.toString()
                if (text.endsWith(".0")) {
                    text = text.substring(0, text.length - 2)
                }
                text
            }
            else -> obj.toString()
        }
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }
}