package com.craftinginterpreters.lox

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    when {
        args.size > 1 -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }
        args.size == 1 -> Lox.runFile(args[0])
        else -> Lox.runPrompt()
    }
}

object Lox {
    var hadError: Boolean = false

    fun runFile(path: String) {
        val code = File(path).readText()
        run(code)
        if (hadError)
            exitProcess(65)
    }

    fun runPrompt() {
        while (true) {
            run(readLine()!!)
            hadError = false
        }
    }

    fun run(source: String) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()
/*
        tokens.forEach {
            println(it)
        }
*/
        val parser = Parser(tokens)
        val expression = parser.parse()

        if(hadError)
            return

        println(AstPrinter().print(expression!!))

    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        hadError = true
    }

    fun error(token: Token, message: String) {
        when {
            token.type == TokenType.EOF -> report(token.line, " at end", message)
            else -> report(token.line, " at '${token.lexeme}'", message)
        }
    }
}

