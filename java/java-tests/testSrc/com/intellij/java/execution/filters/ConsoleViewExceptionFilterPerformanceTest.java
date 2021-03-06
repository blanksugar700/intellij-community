// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.execution.filters;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.impl.ConsoleViewImplTest;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

public class ConsoleViewExceptionFilterPerformanceTest extends LightJavaCodeInsightFixtureTestCase {
  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return JAVA_11;
  }

  public void testManyExceptions() {
    String trace = "java.lang.RuntimeException\n" +
                   "\tat ExceptionTest.main(ExceptionTest.java:5)\n" +
                   "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                   "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:64)\n" +
                   "\tat java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                   "\tat java.base/java.lang.reflect.Method.invoke(Method.java:564)\n" +
                   "\tat com.intellij.rt.execution.application.AppMainV2.main(AppMainV2.java:114)\n";
    myFixture.addClass("public class ExceptionTest {\n" +
                       "    public static void main(String[] args) {\n" +
                       "        //noinspection InfiniteLoopStatement\n" +
                       "        while (true) {\n" +
                       "            new RuntimeException().printStackTrace();\n" +
                       "        }\n" +
                       "    }\n" +
                       "}");
    PlatformTestUtil.startPerformanceTest("Many exceptions", 16_000, () -> {
      ConsoleViewImpl console = ConsoleViewImplTest.createConsole(true, getProject());
      try {
        console.setUpdateFoldingsEnabled(false); // avoid spending time on foldings
        console.print("start\n", ConsoleViewContentType.NORMAL_OUTPUT);
        console.flushDeferredText();
        console.getEditor().getCaretModel().moveToOffset(0); // avoid stick-to-end
        console.getEditor().getDocument().setInBulkUpdate(true); // avoid editor size validation
        for (int i = 0; i < 25; i++) {
          for (int j = 0; j < 1_000; j++) {
            console.print(trace, ConsoleViewContentType.ERROR_OUTPUT);
          }
          WriteAction.run(console::flushDeferredText);
        }
        console.getEditor().getDocument().setInBulkUpdate(false);
        console.waitAllRequests();
      }
      finally {
        Disposer.dispose(console);
      }
    }).assertTiming();
  }
}
