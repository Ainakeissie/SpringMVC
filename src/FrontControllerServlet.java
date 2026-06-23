package src;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import src.annotation.Controller;
import src.annotation.Mapping;

public class FrontControllerServlet extends HttpServlet {
    private final List<String> listClassAnnoted = new ArrayList<>();
    private String packageName;

    @Override
    public void init() throws ServletException {
        ServletConfig config = getServletConfig();
        String configuredPackage = config != null ? config.getInitParameter("base-package") : null;
        packageName = configuredPackage != null && !configuredPackage.isBlank()
                ? configuredPackage
                : "aina.main";

        try {
            scanPackage(packageName);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void scanPackage(String packageName) throws IOException, ClassNotFoundException, URISyntaxException {
        String path = packageName.replace('.', '/');
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);

        if (url == null) {
            throw new IOException("Package introuvable : " + packageName);
        }

        if ("jar".equals(url.getProtocol())) {
            scanJar(path, url);
        } else if ("file".equals(url.getProtocol())) {
            scanDirectory(packageName, new File(url.toURI()));
        }
    }

    private void scanJar(String path, URL url) throws IOException, ClassNotFoundException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        JarFile jarFile = connection.getJarFile();

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.startsWith(path) && name.endsWith(".class") && !entry.isDirectory()) {
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                registerIfController(className);
            }
        }
    }

    private void scanDirectory(String packageName, File folder) throws ClassNotFoundException {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(packageName + "." + file.getName(), file);
                continue;
            }

            if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                registerIfController(className);
            }
        }
    }

    private void registerIfController(String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);

        if (clazz.isAnnotationPresent(Controller.class)) {
            listClassAnnoted.add(clazz.getSimpleName());
        }
    }

    private Map<String, Method> listMethodsAnnotatedWithMapping(String url) {
        if (listClassAnnoted.isEmpty()) {
            return Map.of();
        }
        // on affiche les methodes annotées avec l'url spécifié, si l'url n'existe pas ou est vide, on affiche toutes les methodes annotées avec ses urls
        return listClassAnnoted.stream()
                .flatMap(className -> {
                    try {
                        Class<?> clazz = Class.forName(packageName + "." + className);
                        return Arrays.stream(clazz.getDeclaredMethods())
                                .filter(method -> method.isAnnotationPresent(Mapping.class))
                                .filter(method -> {
                                    Mapping mapping = method.getAnnotation(Mapping.class);
                                    return url == null || url.isBlank() || mapping.value().equals(url);
                                })
                                .map(method -> Map.entry(className + "." + method.getName(), method));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    public void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();
        out.println(req.getRequestURI());
        out.println("Liste des classes annotees :");
        for (String s : listClassAnnoted) {
            out.println(s);
        }
        Map<String, Method> methods = listMethodsAnnotatedWithMapping(req.getRequestURI());
        if (methods.isEmpty()) {
            out.println("Aucune methode trouvee pour l'url : " + req.getRequestURI());
        } else {
            out.println("Liste des methodes annotees avec l'url : " + req.getRequestURI());
            methods.forEach((name, method) -> out.println(name));
        }
    }
}