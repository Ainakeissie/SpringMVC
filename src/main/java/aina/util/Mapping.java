package aina.util;
import java.lang.reflect.Method;

public class Mapping {
    private final Class<?> controllerClass;
    private final Method method;

    public Mapping(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public String getClassName() { return controllerClass.getName(); }
    public Method getMethod() { return method; }
    public Class<?> getControllerClass() { return controllerClass; }

    @Override
    public String toString() {
        return controllerClass.getName() + "." + method.getName() + "()";
    }
}