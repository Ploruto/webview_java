package dev.webview.bridge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as accessible/observable from JavaScript.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JavascriptValue {
    /**
     * Custom name for the property in JS. Defaults to field name.
     */
    String value() default "";

    /**
     * Allow reading this property from JS.
     */
    boolean allowGet() default true;

    /**
     * Allow writing this property from JS.
     */
    boolean allowSet() default true;
}
