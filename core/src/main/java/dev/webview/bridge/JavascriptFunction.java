package dev.webview.bridge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as callable from JavaScript.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JavascriptFunction {
    /**
     * Custom name for the function in JS. Defaults to method name.
     */
    String value() default "";
}
