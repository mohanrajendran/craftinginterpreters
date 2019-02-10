package com.craftinginterpreters.tool

import java.io.PrintWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(1)
    }

    val outputDir = args[0]

    defineAst(outputDir, "Expr", listOf(
            "Assign   : Token name, Expr value",
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Any? value",
            "Logical  : Expr left, Token operator, Expr right",
            "Unary    : Token operator, Expr right",
            "Variable : Token name"
    ))

    defineAst(outputDir, "Stmt", listOf(
            "Block      : List<Stmt> statements",
            "Expression : Expr expression",
            "If         : Expr condition, Stmt thenBranch, Stmt? elseBranch",
            "Print      : Expr expression",
            "Var        : Token name, Expr? initializer",
            "While      : Expr condition, Stmt body"
    ))
}

fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path)

    writer.println("package com.craftinginterpreters.lox")
    writer.println()

    writer.println("abstract class $baseName {")

    writer.println("\tabstract fun <R>accept(visitor: Visitor<R>): R")
    writer.println()

    defineVisitor(writer, baseName, types)

    types.forEach {
        val className = it.split(":")[0].trim()
        val fields = it.split(":")[1].trim()

        defineType(writer, baseName, className, fields)
    }

    writer.println("}")
    writer.close()
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    writer.println("\tinterface Visitor<R> {")
    types.forEach {
        val typeName = it.split(":")[0].trim()
        writer.println("\t\tfun visit$typeName$baseName(${baseName.toLowerCase()}: $typeName): R")
    }
    writer.println("\t}")
    writer.println()
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fields: String) {
    val params = fields.split(",").joinToString(", ") {
        val type = it.trim().split(" ")[0].trim()
        val name = it.trim().split(" ")[1].trim()

        "val $name: $type"
    }

    writer.println("\tclass $className($params) : $baseName(){")
    writer.println("\t\toverride fun <R> accept(visitor: Visitor<R>): R {")
    writer.println("\t\t\treturn visitor.visit$className$baseName(this)")
    writer.println("\t\t}")
    writer.println("\t}")
}


