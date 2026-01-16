package dev.webview.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Base class for objects exposed to JavaScript.
 * Subclass and use @JavascriptFunction/@JavascriptValue annotations.
 */
public abstract class JavascriptObject {
    private final String id = UUID.randomUUID().toString();
    private final Map<String, FieldMapping> properties = new HashMap<>();
    private final Map<String, MethodMapping> functions = new HashMap<>();
    private final Map<String, Field> subObjects = new HashMap<>();
    private WebviewBridge bridge = null;

    public JavascriptObject() {
        scanAnnotations();
    }

    private void scanAnnotations() {
        // Scan fields
        for (Field field : this.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            if (JavascriptObject.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                this.subObjects.put(field.getName(), field);
            } else if (field.isAnnotationPresent(JavascriptValue.class)) {
                field.setAccessible(true);
                JavascriptValue annotation = field.getAnnotation(JavascriptValue.class);
                String name = annotation.value().isEmpty() ? field.getName() : annotation.value();
                this.properties.put(name, new FieldMapping(this, field, annotation));
            }
        }

        // Scan methods
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;

            if (method.isAnnotationPresent(JavascriptFunction.class)) {
                method.setAccessible(true);
                JavascriptFunction annotation = method.getAnnotation(JavascriptFunction.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();
                this.functions.put(name, new MethodMapping(this, method));
            }
        }
    }

    public String getId() {
        return id;
    }

    String getInitScript(String name, WebviewBridge bridge) {
        this.bridge = bridge;
        bridge.registerObject(name, this);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("window.Bridge.__internal.defineObject('%s', '%s');\n", name, id));

        for (String funcName : functions.keySet()) {
            sb.append(String.format("window.%s.__internal.defineFunction('%s');\n", name, funcName));
        }

        for (String propName : properties.keySet()) {
            sb.append(String.format("window.%s.__internal.defineProperty('%s');\n", name, propName));
        }

        for (Map.Entry<String, Field> sub : subObjects.entrySet()) {
            try {
                JavascriptObject subObj = (JavascriptObject) sub.getValue().get(this);
                if (subObj != null) {
                    sb.append(subObj.getInitScript(name + "." + sub.getKey(), bridge));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    Object get(String property) throws Throwable {
        FieldMapping mapping = properties.get(property);
        if (mapping == null) throw new NoSuchFieldException(property);
        return mapping.get();
    }

    void set(String property, Object value) throws Throwable {
        FieldMapping mapping = properties.get(property);
        if (mapping == null) throw new NoSuchFieldException(property);
        mapping.set(value);
    }

    Object invoke(String function, Object[] args) throws Throwable {
        MethodMapping mapping = functions.get(function);
        if (mapping == null) throw new NoSuchMethodException(function);
        return mapping.invoke(args);
    }

    private static class FieldMapping {
        private final Object instance;
        private final Field field;
        private final JavascriptValue annotation;

        FieldMapping(Object instance, Field field, JavascriptValue annotation) {
            this.instance = instance;
            this.field = field;
            this.annotation = annotation;
        }

        Object get() throws IllegalAccessException {
            if (!annotation.allowGet()) throw new UnsupportedOperationException("GET not allowed");
            return field.get(instance);
        }

        void set(Object value) throws IllegalAccessException {
            if (!annotation.allowSet()) throw new UnsupportedOperationException("SET not allowed");
            
            // Convert value to the correct type if needed
            Class<?> fieldType = field.getType();
            Object convertedValue = convertType(value, fieldType);
            
            field.set(instance, convertedValue);
        }
        
        private Object convertType(Object value, Class<?> targetType) {
            if (value == null) return null;
            if (targetType.isAssignableFrom(value.getClass())) return value;
            
            // Handle primitive type conversions
            String strValue = value.toString();
            
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(strValue);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(strValue);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(strValue);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(strValue);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(strValue);
            } else if (targetType == String.class) {
                return strValue;
            }
            
            return value;
        }
    }

    private static class MethodMapping {
        private final Object instance;
        private final Method method;

        MethodMapping(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
            return method.invoke(instance, args);
        }
    }
}
