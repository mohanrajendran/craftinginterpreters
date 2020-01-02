package com.craftinginterpreters.lox


class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    class RuntimeError(val token: Token, message: String) : RuntimeException(message)

    private val globals = Environment()
    private var environment = globals
    private val locals: MutableMap<Expr, Int> = HashMap()

    init {
        globals.define("clock", object : LoxCallable {
            override fun arity(): Int {
                return 0
            }

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                return System.currentTimeMillis() / 1000.0
            }

            override fun toString(): String {
                return "<native fn>"
            }
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach {
                execute(it)
            }
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        when (val distance = locals[expr]) {
            null -> globals.assign(expr.name, value)
            else -> environment.assignAt(distance, expr.name, value)
        }

        return value
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

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)

        val arguments = expr.arguments.map(this::evaluate)

        if (callee !is LoxCallable)
            throw RuntimeError(expr.paren, "Can only call functions and classes")

        if (arguments.size != callee.arity())
            throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")

        return callee.call(this, arguments)
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val instance = evaluate(expr.instance)
        if (instance is LoxInstance)
            return instance.get(expr.name)

        throw RuntimeError(expr.name, "Only instances have properties")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        when (expr.operator.type) {
            TokenType.OR -> if (isTruthy(left)) return left
            else -> if (!isTruthy(left)) return left
        }

        return evaluate(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val instance = evaluate(expr.instance)

        if (instance !is LoxInstance)
            throw RuntimeError(expr.name, "Only instances have fields.")

        val value = evaluate(expr.value)
        instance.set(expr.name, value)
        return value
    }

    override fun visitSuperExpr(expr: Expr.Super): Any? {
        val distance = locals[expr]!!
        val superclass = environment.getAt(distance, "super") as LoxClass
        val superinstance = environment.getAt(distance-1, "this") as LoxInstance

        val method = superclass.findMethod(expr.method.lexeme)

        if (method == null) {
            throw RuntimeError(expr.method, "Undefined property '${expr.method.lexeme}'.")
        }

        return method.bind(superinstance)
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookUpVariable(expr.keyword, expr)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> -checkNumberOperand(expr.operator, right)
            TokenType.BANG -> !isTruthy(right)
            else -> null
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        return when (val dist = locals[expr]) {
            null -> globals.get(name)
            else -> environment.getAt(dist, name.lexeme)
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

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment

            statements.forEach {
                execute(it)
            }
        } finally {
            this.environment = previous
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        var superclass: LoxClass? = null
        if (stmt.superclass != null) {
            val superklass = evaluate(stmt.superclass)
            if (superklass !is LoxClass)
                throw RuntimeError(stmt.superclass.name, "Superclass must be a class.")
            superclass = superklass
        }
        environment.define(stmt.name.lexeme, null)

        if (stmt.superclass != null) {
            environment = Environment(environment)
            environment.define("super", superclass)
        }

        val methods = HashMap<String, LoxFunction>()
        stmt.methods.forEach {
            val function = LoxFunction(it, environment, it.name.lexeme == "init")
            methods[it.name.lexeme] = function
        }

        val klass = LoxClass(stmt.name.lexeme, superclass, methods)

        if(stmt.superclass != null) environment = environment.enclosing!!

        environment.assign(stmt.name, klass)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment, false)
        environment.define(stmt.name.lexeme, function)
        return
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        when {
            isTruthy(evaluate(stmt.condition)) -> execute(stmt.thenBranch)
            stmt.elseBranch != null -> execute(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        var value: Any? = null
        if (stmt.value != null)
            value = evaluate(stmt.value)

        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = if (stmt.initializer != null) evaluate(stmt.initializer) else null

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }


}