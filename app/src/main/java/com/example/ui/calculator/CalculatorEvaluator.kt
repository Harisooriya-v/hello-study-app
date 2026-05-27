package com.example.ui.calculator

import kotlin.math.*

object CalculatorEvaluator {
    fun evaluate(expression: String): Double {
        if (expression.trim().isEmpty()) return 0.0

        // Replace constants with their numeric string representations
        var sanitized = expression
            .replace("π", Math.PI.toString())
            .replace("e", java.lang.Math.E.toString())
            .replace("×", "*")
            .replace("÷", "/")

        // Rephrase functions for easy token matches
        sanitized = sanitized
            .replace("sin", "s")
            .replace("cos", "c")
            .replace("tan", "t")
            .replace("log", "g")
            .replace("ln", "n")
            .replace("√", "r")

        return ExpressionParser(sanitized).parse()
    }

    private class ExpressionParser(val sanitized: String) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < sanitized.length) sanitized[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val result = parseExpression()
            if (pos < sanitized.length) throw RuntimeException("Extra characters")
            return result
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else break
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Divide by zero")
                    x /= divisor
                } else break
            }
            return x
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                val numStr = sanitized.substring(startPos, pos)
                x = numStr.toDoubleOrNull() ?: 0.0
            } else if (eat('s'.code)) { // sin
                val hasParen = eat('('.code)
                val arg = if (hasParen) parseExpression() else parseFactor()
                if (hasParen) eat(')'.code)
                x = sin(Math.toRadians(arg))
            } else if (eat('c'.code)) { // cos
                val hasParen = eat('('.code)
                val arg = if (hasParen) parseExpression() else parseFactor()
                if (hasParen) eat(')'.code)
                x = cos(Math.toRadians(arg))
            } else if (eat('t'.code)) { // tan
                val hasParen = eat('('.code)
                val arg = if (hasParen) parseExpression() else parseFactor()
                if (hasParen) eat(')'.code)
                x = tan(Math.toRadians(arg))
            } else if (eat('g'.code)) { // log10
                val hasParen = eat('('.code)
                val arg = if (hasParen) parseExpression() else parseFactor()
                if (hasParen) eat(')'.code)
                if (arg <= 0.0) throw ArithmeticException("Log of non-positive number")
                x = log10(arg)
            } else if (eat('n'.code)) { // ln
                val hasParen = eat('('.code)
                val arg = if (hasParen) parseExpression() else parseFactor()
                if (hasParen) eat(')'.code)
                if (arg <= 0.0) throw ArithmeticException("Ln of non-positive number")
                x = ln(arg)
            } else if (eat('r'.code)) { // square root
                val hasParen = eat('('.code)
                val arg = if (hasParen) parseExpression() else parseFactor()
                if (hasParen) eat(')'.code)
                if (arg < 0.0) throw ArithmeticException("Sqrt of negative number")
                x = sqrt(arg)
            } else {
                throw RuntimeException("N: " + ch.toChar())
            }

            if (eat('^'.code)) {
                val exponent = parseFactor()
                x = x.pow(exponent)
            }

            return x
        }
    }
}
