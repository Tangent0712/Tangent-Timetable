package com.timetable.service.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 沙箱脚本执行器的安全与行为测试。
 * 重点验证：正常脚本可用、非布尔返回值被拒、以及危险脚本被安全终止而非拖垮进程。
 */
class RecurringScriptEvaluatorTest {

    private final RecurringScriptEvaluator evaluator = new RecurringScriptEvaluator();

    /** 2026-09-28 是周一 */
    private final LocalDateTime monday = LocalDateTime.of(2026, 9, 28, 10, 0);

    private RecurringScriptEvaluator.ScriptResult run(String script) {
        return evaluator.test(script, monday, null);
    }

    @Test
    void validScriptCanTrigger() {
        var result = run("function shouldTrigger(ctx){ return ctx.dayOfWeek === 1; }");
        assertTrue(result.ok());
        assertTrue(result.triggered());
    }

    @Test
    void validScriptCanDecline() {
        var result = run("function shouldTrigger(ctx){ return ctx.dayOfWeek === 5; }");
        assertTrue(result.ok());
        assertFalse(result.triggered());
    }

    @Test
    void nonBooleanReturnIsRejected() {
        var result = run("function shouldTrigger(ctx){ return 1; }");
        assertFalse(result.ok());
        assertFalse(result.triggered());
    }

    @Test
    void missingFunctionIsRejected() {
        var result = run("var x = 1;");
        assertFalse(result.ok());
    }

    @Test
    void syntaxErrorIsRejected() {
        var result = run("function shouldTrigger(ctx) { return true; ");
        assertFalse(result.ok());
    }

    @Test
    void oversizedScriptIsRejected() {
        String script = "function shouldTrigger(ctx){ return true; }" + "0".repeat(5000);
        var result = run(script);
        assertFalse(result.ok());
    }

    @Test
    void javaAccessIsBlocked() {
        var result = run("function shouldTrigger(ctx){ return java.lang.Runtime !== null; }");
        assertFalse(result.ok());
    }

    @Test
    void infiniteLoopIsAborted() {
        var result = run("function shouldTrigger(ctx){ while(true){} }");
        assertFalse(result.ok());
    }

    @Test
    void infiniteRecursionIsAborted() {
        var result = run("function shouldTrigger(ctx){ return shouldTrigger(ctx); }");
        assertFalse(result.ok());
    }
}
