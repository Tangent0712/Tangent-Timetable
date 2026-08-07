package com.timetable.service.impl;

import com.timetable.exception.BusinessException;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户自定义循环规则的脚本执行器（沙箱）。
 *
 * 用户用 JavaScript 写一个 shouldTrigger(ctx) 函数返回布尔值，决定当前时刻是否触发。
 *
 * 安全措施（执行不可信代码，必须全部具备）：
 * 1. ClassShutter 阻断所有 Java 类访问 —— 杜绝 java.lang.Runtime 等反射逃逸
 * 2. 指令计数中断 —— 防死循环 / 长时间占用 CPU
 * 3. 独立线程 + 超时强制终止 —— 兜住极端情况
 * 4. 密封的顶层作用域，禁用 Rhino 的 JavaAdapter / importClass 等桥接能力
 * 5. 脚本长度上限，避免存入超大文本
 */
@Component
public class RecurringScriptEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RecurringScriptEvaluator.class);

    /** 指令计数上限，超出即中断（约相当于百万级基础操作） */
    private static final int MAX_INSTRUCTIONS = 2_000_000;
    /** 单次执行墙钟超时 */
    private static final long TIMEOUT_MS = 1500L;
    public static final int MAX_SCRIPT_LENGTH = 4000;
    /** 脚本运行期允许新增的堆内存上限，防「字符串炸弹」等内存耗尽攻击 */
    private static final long MAX_HEAP_GROWTH_BYTES = 64L * 1024 * 1024;
    /**
     * 指令观察间隔。设得足够小，才能在「字符串炸弹」这类循环次数极少、
     * 但每轮内存翻倍的脚本造成 OOM 之前介入。
     */
    private static final int OBSERVER_THRESHOLD = 200;
    /** 解释器调用栈深度上限，防无限递归耗尽堆内存 */
    private static final int MAX_STACK_DEPTH = 256;

    private static final String FUNCTION_NAME = "shouldTrigger";

    /**
     * 沙箱 ContextFactory：开启指令观察以支持中断。
     */
    private static final class SandboxFactory extends ContextFactory {
        @Override
        protected void onContextCreated(Context cx) {
            cx.setInstructionObserverThreshold(OBSERVER_THRESHOLD);
            super.onContextCreated(cx);
        }

        @Override
        protected void observeInstructionCount(Context cx, int instructionCount) {
            Counter counter = (Counter) cx.getThreadLocal(Counter.KEY);
            if (counter != null) {
                counter.count += OBSERVER_THRESHOLD;
                if (counter.count > MAX_INSTRUCTIONS) {
                    throw new ScriptAbortError("脚本执行步数过多，可能存在死循环");
                }
                // 内存哨兵：字符串/数组炸弹的循环次数很少，指令计数抓不到，需单独看堆增长
                if (counter.baselineHeap > 0) {
                    long used = usedHeap();
                    if (used - counter.baselineHeap > MAX_HEAP_GROWTH_BYTES) {
                        throw new ScriptAbortError("脚本占用内存过多，已终止");
                    }
                }
            }
            if (System.currentTimeMillis() - startOf(cx) > TIMEOUT_MS) {
                throw new ScriptAbortError("脚本执行超时");
            }
            super.observeInstructionCount(cx, instructionCount);
        }

        private long startOf(Context cx) {
            Object v = cx.getThreadLocal("startAt");
            return v instanceof Long l ? l : System.currentTimeMillis();
        }
    }

    private static final class Counter {
        static final String KEY = "instructionCounter";
        long count;
        long baselineHeap;
    }

    private static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    /** 用 Error 而非 Exception，确保用户脚本无法用 try/catch 吞掉中断 */
    static final class ScriptAbortError extends Error {
        ScriptAbortError(String message) {
            super(message);
        }
    }

    private static final SandboxFactory FACTORY = new SandboxFactory();

    /** 只允许脚本引擎自身必需的类，其余 Java 类全部拒绝 */
    private static final ClassShutter SHUTTER = fullClassName -> false;

    /**
     * 执行用户脚本，判断此刻是否应触发。
     *
     * @param script 用户脚本，需定义 shouldTrigger(ctx) 函数
     * @param now    当前时间
     * @param lastTriggeredAt 上次触发时间，可为 null
     */
    public boolean evaluate(String script, LocalDateTime now, LocalDateTime lastTriggeredAt) {
        ScriptResult result = run(script, now, lastTriggeredAt);
        if (!result.ok()) {
            throw new BusinessException(400, result.error());
        }
        return result.triggered();
    }

    /**
     * 试运行（用于前端「测试脚本」与保存前校验），不抛异常，返回结构化结果。
     */
    public ScriptResult test(String script, LocalDateTime now, LocalDateTime lastTriggeredAt) {
        return run(script, now, lastTriggeredAt);
    }

    public record ScriptResult(boolean ok, boolean triggered, String error) {}

    private ScriptResult run(String script, LocalDateTime now, LocalDateTime lastTriggeredAt) {
        if (script == null || script.isBlank()) {
            return new ScriptResult(false, false, "脚本内容不能为空");
        }
        if (script.length() > MAX_SCRIPT_LENGTH) {
            return new ScriptResult(false, false,
                    "脚本过长（上限 " + MAX_SCRIPT_LENGTH + " 字符）");
        }

        // 独立线程执行，超时可强制放弃等待
        final ScriptResult[] holder = new ScriptResult[1];
        Thread worker = new Thread(() -> holder[0] = doRun(script, now, lastTriggeredAt),
                "recurring-script");
        worker.setDaemon(true);
        worker.start();
        try {
            worker.join(TIMEOUT_MS + 500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ScriptResult(false, false, "脚本执行被中断");
        }
        if (worker.isAlive()) {
            log.warn("Recurring script did not finish in time, abandoning thread");
            return new ScriptResult(false, false, "脚本执行超时（上限 " + TIMEOUT_MS + "ms）");
        }
        return holder[0] != null ? holder[0]
                : new ScriptResult(false, false, "脚本执行失败");
    }

    private ScriptResult doRun(String script, LocalDateTime now, LocalDateTime lastTriggeredAt) {
        Context cx = FACTORY.enterContext();
        try {
            cx.setClassShutter(SHUTTER);
            cx.setOptimizationLevel(-1);           // 解释模式，指令计数才生效
            cx.setLanguageVersion(Context.VERSION_ES6);
            // 限制调用栈深度，让无限递归在耗尽堆之前就被拦下
            cx.setMaximumInterpreterStackDepth(MAX_STACK_DEPTH);
            Counter counter = new Counter();
            counter.baselineHeap = usedHeap();
            cx.putThreadLocal(Counter.KEY, counter);
            cx.putThreadLocal("startAt", System.currentTimeMillis());

            // initSafeStandardObjects(sealed=true)：不含 JavaAdapter/Packages，且顶层对象密封
            ScriptableObject scope = cx.initSafeStandardObjects(null, true);

            Scriptable ctx = toScriptable(cx, scope, buildContext(now, lastTriggeredAt));
            scope.putConst("ctx", scope, ctx);

            cx.evaluateString(scope, script, "userScript", 1, null);

            Object fn = scope.get(FUNCTION_NAME, scope);
            if (!(fn instanceof org.mozilla.javascript.Function function)) {
                return new ScriptResult(false, false,
                        "脚本中必须定义 function " + FUNCTION_NAME + "(ctx) 函数");
            }
            Object out = function.call(cx, scope, scope, new Object[]{ctx});
            if (out == null || out == Context.getUndefinedValue()) {
                return new ScriptResult(false, false,
                        FUNCTION_NAME + " 必须返回 true 或 false，当前返回了空值");
            }
            if (!(out instanceof Boolean b)) {
                // 明确要求布尔，避免 "0"/"" 之类的隐式转换造成误触发
                return new ScriptResult(false, false,
                        FUNCTION_NAME + " 必须返回布尔值，当前返回类型为 " + describeType(out));
            }
            return new ScriptResult(true, b, null);
        } catch (ScriptAbortError e) {
            return new ScriptResult(false, false, e.getMessage());
        } catch (EvaluatorException e) {
            return new ScriptResult(false, false, "脚本语法错误：" + e.details()
                    + "（第 " + e.lineNumber() + " 行）");
        } catch (StackOverflowError e) {
            return new ScriptResult(false, false, "脚本递归层数过深（可能存在无限递归）");
        } catch (OutOfMemoryError e) {
            // 无限递归 / 超大分配可能耗尽堆，必须在工作线程内消化掉，
            // 否则会波及主线程导致整个服务不可用
            return new ScriptResult(false, false, "脚本占用内存过多，已终止");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ScriptResult(false, false, "脚本运行出错：" + msg);
        } finally {
            Context.exit();
        }
    }

    private String describeType(Object out) {
        if (out instanceof Number) return "数字";
        if (out instanceof CharSequence) return "字符串";
        return out.getClass().getSimpleName();
    }

    /**
     * 注入给脚本的上下文变量。只暴露纯数据（数字/字符串/布尔），不暴露任何 Java 对象。
     */
    public static Map<String, Object> buildContext(LocalDateTime now, LocalDateTime lastTriggeredAt) {
        LocalDate date = now.toLocalDate();
        Map<String, Object> map = new HashMap<>();
        map.put("year", date.getYear());
        map.put("month", date.getMonthValue());
        map.put("day", date.getDayOfMonth());
        map.put("dayOfWeek", date.getDayOfWeek().getValue());   // 1=周一 ... 7=周日
        map.put("dayOfYear", date.getDayOfYear());
        map.put("weekOfYear", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        map.put("hour", now.getHour());
        map.put("minute", now.getMinute());
        map.put("daysInMonth", date.lengthOfMonth());
        map.put("isLastDayOfMonth", date.getDayOfMonth() == date.lengthOfMonth());
        map.put("isWeekend", date.getDayOfWeek().getValue() >= 6);
        map.put("dateStr", date.toString());
        if (lastTriggeredAt == null) {
            map.put("daysSinceLastTrigger", -1);
            map.put("hoursSinceLastTrigger", -1);
            map.put("neverTriggered", true);
        } else {
            map.put("daysSinceLastTrigger",
                    (int) ChronoUnit.DAYS.between(lastTriggeredAt.toLocalDate(), date));
            map.put("hoursSinceLastTrigger",
                    (int) ChronoUnit.HOURS.between(lastTriggeredAt, now));
            map.put("neverTriggered", false);
        }
        return map;
    }

    private Scriptable toScriptable(Context cx, Scriptable scope, Map<String, Object> data) {
        Scriptable obj = cx.newObject(scope);
        data.forEach((k, v) -> ScriptableObject.putProperty(obj, k, v));
        return obj;
    }

    /** 给前端的函数骨架模板 */
    public static final String SCRIPT_TEMPLATE = """
            // 返回 true 表示此刻应该触发，生成一条待办
            // 系统每分钟检查一次；同一分钟内只会触发一次
            function shouldTrigger(ctx) {
              // 可用字段：
              //   ctx.year / ctx.month / ctx.day
              //   ctx.dayOfWeek        1=周一 ... 7=周日
              //   ctx.hour / ctx.minute
              //   ctx.weekOfYear       ISO 周序号
              //   ctx.dayOfYear / ctx.daysInMonth
              //   ctx.isLastDayOfMonth / ctx.isWeekend
              //   ctx.daysSinceLastTrigger   距上次触发天数（从未触发为 -1）
              //   ctx.neverTriggered

              // 示例：每逢双周的周五 12:00
              return ctx.dayOfWeek === 5
                  && ctx.hour === 12
                  && ctx.minute === 0
                  && ctx.weekOfYear % 2 === 0;
            }
            """;
}
