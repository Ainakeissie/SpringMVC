package aina.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import aina.annotation.Json;
import aina.annotation.Auth;
import aina.util.Mapfile;
import aina.util.Mapping;
import aina.util.ModAndView;
import aina.util.UrlMethod;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Part;
import aina.util.AppSession;
public class FrontControllerServlet extends HttpServlet {

    Map<UrlMethod, Mapping> routesWithMethod;

    String viewPrefix;

    String viewSuffix;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {
        super.init();
        routesWithMethod = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("routesWithMethod");
        viewPrefix = (String) getServletContext().getAttribute("prefix");
        viewSuffix = (String) getServletContext().getAttribute("suffix");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    public Object cast(String value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (type == String.class) {
            return value;
        }

        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        }

        if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        }

        if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        }

        if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        }

        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }

        if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        }

        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        }

        if (type == char.class || type == Character.class) {
            return value.charAt(0);
        }

        if (type == java.util.Date.class) {
            try {
                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd");

                return format.parse(value);

            } catch (java.text.ParseException e) {
                throw new IllegalArgumentException(
                        "Date invalide : " + value, e);
            }
        }

        throw new IllegalArgumentException(
                "Type non supporté : " + type.getName());
    }

    // private Object[] getMethodArguments(Method method, HttpServletRequest
    // request) {
    // Parameter[] parameters = method.getParameters();
    // Object[] arguments = new Object[parameters.length];
    // for (int i = 0; i < parameters.length; i++) {
    // arguments[i] = request.getParameter(parameters[i].getName());
    // if(arguments[i] instanceof String) {
    // arguments[i] = cast((String) arguments[i], parameters[i].getType());
    // }
    // }

    // return arguments;
    // }

    private Object bindObject(Class<?> clazz, HttpServletRequest request, String prefix) {
        try {
            Object object = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                    bindField(object, field, request, prefix);
                }
            }

            return object;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Impossible de créer l'objet paramètre : " + clazz.getName(), e);
        }
    }

    private void bindField(Object object, Field field, HttpServletRequest request, String prefix)
            throws IllegalAccessException {
        String fieldPath = prefix.isBlank() ? field.getName() : prefix + "." + field.getName();
        field.setAccessible(true);

        if (isSimpleType(field.getType())) {
            String value = request.getParameter(fieldPath);
            if (value != null) {
                field.set(object, cast(value, field.getType()));
            }
            return;
        }

        if (List.class.isAssignableFrom(field.getType())) {
            field.set(object, bindList(field.getGenericType(), request, fieldPath, field.getName()));
            return;
        }

        field.set(object, bindObject(field.getType(), request, fieldPath));
    }

    private List<Object> bindList(java.lang.reflect.Type genericType,
            HttpServletRequest request, String parameterName, String name) {
        if (!(genericType instanceof ParameterizedType typeListe)
                || !(typeListe.getActualTypeArguments()[0] instanceof Class<?> typeElement)) {
            throw new IllegalArgumentException("La liste doit préciser un type, par exemple List<String> : " + name);
        }

        if (!isSimpleType(typeElement)) {
            throw new IllegalArgumentException("Seules les listes de types simples sont prises en charge : " + name);
        }

        List<Object> liste = new ArrayList<>();
        String[] valeurs = request.getParameterValues(parameterName);
        if (valeurs != null) {
            for (String valeur : valeurs) {
                liste.add(cast(valeur, typeElement));
            }
        }
        return liste;
    }

    private Object[] getMethodArgumentsObject(Method method, HttpServletRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> parameterType = parameters[i].getType();

            if (parameterType.equals(ApplicationContext.class)) {
                continue;
            }

            if (isSimpleType(parameterType)) {
                arguments[i] = cast(request.getParameter(parameters[i].getName()), parameterType);
                System.out.println(parameters[i].getName());
                continue;
            }

            if (List.class.isAssignableFrom(parameterType)) {
                arguments[i] = bindList(parameters[i].getParameterizedType(), request,
                        parameters[i].getName(), parameters[i].getName());
                continue;
            }
            else{
                if(parameterType.equals(AppSession.class)){
                    arguments[i] = getAppCurrentSession(request);
                    continue;
                }
                arguments[i] = bindObject(parameterType, request, "");
            }

        }

        return arguments;
    }

    private boolean isSimpleType(Class<?> type) {
        return type == String.class || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == boolean.class || type == Boolean.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class
                || type == char.class || type == Character.class
                || type == java.util.Date.class;
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String pathInfo = request.getRequestURI().substring(request.getContextPath().length());
        if (pathInfo.startsWith("/WEB-INF/") || pathInfo.startsWith("/META-INF/")) {
            return;
        }

        UrlMethod urlMethod = new UrlMethod(pathInfo, request.getMethod());

        if (aina.util.LoadingClass.isARouteInsideMappingWithMethod(urlMethod, routesWithMethod)) {
            Mapping mapping = routesWithMethod.get(urlMethod);
            System.out.println("Route trouvée : " + urlMethod + " -> " + mapping);

            try {
                Object controller = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                Method controllerMethod = mapping.getMethod();

                ServletContext servletContext = getServletContext();

                ApplicationContext context = WebApplicationContextUtils
                        .getWebApplicationContext(servletContext);
               
                // Sprint 10 bis 
                if (controllerMethod.isAnnotationPresent(Auth.class)) {
                    Auth auth = controllerMethod.getAnnotation(Auth.class);
                    AppSession session = getAppCurrentSession(request);

                    String[] values = auth.value();
                    String key = auth.key();
                    boolean isAuthorized = false;

                    if (values.length == 0) 
                    {
                        isAuthorized = true;
                    } 
                    else 
                    {
                        String sessionValue = (String) session.getSessionData(key);
                        for (String value : values) 
                        {
                            if (sessionValue != null && sessionValue.toLowerCase().equals(value.toLowerCase())) 
                            {
                                isAuthorized = true;
                                break;
                            }
                        }
                    }
                    if (!isAuthorized) 
                    {
                        response.setContentType("text/plain;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        try (PrintWriter out = response.getWriter()) 
                        {
                            out.println("Accès refusé : l'accès n'est pas autorisé.");
                        }
                        return;
                    }
                }

                Parameter[] parameters = controllerMethod.getParameters();
                Object[] arguments = new Object[parameters.length];

                arguments = getMethodArgumentsObject(controllerMethod, request);

                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];

                    if (parameter.getType().equals(ApplicationContext.class)) {
                        arguments[i] = context;
                    }
                    if (parameter.getType().equals(byte[].class)) {
                        Part tab_byte = request.getPart("fichier");
                        byte[] tableau_byte = null;
                        String nom = null;
                        String extension = null;
                        if (tab_byte != null) {
                            tableau_byte = tab_byte.getInputStream().readAllBytes();
                            nom = tab_byte.getSubmittedFileName();
                            if (nom != null && nom.contains(".")) {
                                extension = nom.substring(nom.lastIndexOf(".") + 1);
                            }
                        }
                        Mapfile mapfile = new Mapfile();
                        mapfile.setTableau_byte(tableau_byte);
                        mapfile.setNom(nom);
                        mapfile.setExtension(extension);
                    }
                }

                Object result = controllerMethod.invoke(controller, arguments);

                if (controllerMethod.isAnnotationPresent(Json.class)) {
                    if (result instanceof String) {
                        response.setContentType("text/plain;charset=UTF-8");
                        response.getWriter().print(result);
                    } else {
                        response.setContentType("application/json;charset=UTF-8");
                        objectMapper.writeValue(response.getWriter(), result);
                    }
                    return;
                }

                if (result instanceof ModAndView mav) {
                    for (Map.Entry<String, Object> en : mav.getValues().entrySet()) {
                        request.setAttribute(en.getKey(), en.getValue());
                    }

                    if (mav.getView() != null && !mav.getView().isBlank()) {
                        String viewPath = viewPrefix + mav.getView() + viewSuffix;
                        RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                        dispatcher.forward(request, response);
                        return;
                    }

                    throw new ServletException("Aucune vue définie pour " + urlMethod);
                }

                if (result instanceof String text) {
                    response.setContentType("text/plain;charset=UTF-8");
                    try (PrintWriter out = response.getWriter()) {
                        out.println("Resultat de la methode:\n");
                        out.println(text);
                    }
                    return;
                }

                throw new ServletException(
                        "Type de retour non supporté pour " + urlMethod + " : " + result.getClass().getName());

            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                throw new RuntimeException("Impossible d'exécuter la méthode liée à " + urlMethod, e);
            }
        } else {
            response.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("Aucune route trouvée pour l'URL : " + pathInfo);
                routesWithMethod.forEach((urlMethodKey, mapping) -> {
                    out.println(urlMethodKey + " -> " + mapping.getClassName() + "->" + mapping.getMethod().getName()
                            + "()");
                });
            }
        }
    }

    private AppSession getAppCurrentSession(HttpServletRequest request) {
        AppSession session = null ;
       session = (AppSession) getServletContext().getAttribute(request.getSession().getId());
        if(session == null) {
            session = new AppSession(request.getSession().getId());
            getServletContext().setAttribute(request.getSession().getId(), session);
        }
        return session;

    }
}
