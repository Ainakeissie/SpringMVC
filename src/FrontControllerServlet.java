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

    private Map<UtilMethode, UrlMethode> listMethodsAnnotatedWithMapping(String url, String httpMethod) {
        if (listClassAnnoted.isEmpty()) {
            return Map.of();
        }

        Map<UtilMethode, UrlMethode> result = listClassAnnoted.stream()
                .flatMap(className -> {
                    try {
                        Class<?> clazz = Class.forName(packageName + "." + className);

                        return Arrays.stream(clazz.getDeclaredMethods())
                                .filter(m -> m.isAnnotationPresent(Mapping.class))
                                .filter(m -> Objects.equals(m.getAnnotation(Mapping.class).value(), url))
                                .filter(m -> Objects.equals(m.getAnnotation(Mapping.class).method(), httpMethod))
                                .map(m -> {
                                    UtilMethode key = new UtilMethode();
                                    key.url = url;
                                    key.method = httpMethod;

                                    UrlMethode value = new UrlMethode();
                                    value.nomClasse = clazz.getSimpleName();
                                    value.nomMethode = m.getName();

                                    return Map.entry(key, value);
                                });

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return java.util.stream.Stream.<Map.Entry<UtilMethode, UrlMethode>>empty();
                    }
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a));

        // fallback : si aucune méthode ne correspond à l'URL et méthode HTTP → toutes
        // les méthodes @Mapping
        if (result.isEmpty()) {
            return listClassAnnoted.stream()
                    .flatMap(className -> {
                        try {
                            Class<?> clazz = Class.forName(packageName + "." + className);

                            return Arrays.stream(clazz.getDeclaredMethods())
                                    .filter(m -> m.isAnnotationPresent(Mapping.class))
                                    .map(m -> {
                                        UtilMethode key = new UtilMethode();
                                        key.url = m.getAnnotation(Mapping.class).value();
                                        key.method = m.getAnnotation(Mapping.class).method();

                                        UrlMethode value = new UrlMethode();
                                        value.nomClasse = clazz.getSimpleName();
                                        value.nomMethode = m.getName();

                                        return Map.entry(key, value);
                                    });

                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            return java.util.stream.Stream.<Map.Entry<UtilMethode, UrlMethode>>empty();
                        }
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a));
        }

        return result;
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
        String url = req.getServletPath();
        String httpMethod = req.getMethod();

        out.println(url);
        out.println("Méthode HTTP: " + httpMethod);
        out.println("Liste des classes annotees :");
        for (String s : listClassAnnoted) {
            out.println(s);
        }

        Map<UtilMethode, UrlMethode> methods = listMethodsAnnotatedWithMapping(url, httpMethod);
        if (methods.isEmpty()) {
            out.println("Aucune methode trouvee pour l'url : " + url + " avec la methode : " + httpMethod);
        } else {
            out.println("Liste des methodes annotees avec l'url : " + url + " et methode : " + httpMethod);
            methods.forEach((util, urlMethode) -> out.println(
                    urlMethode.nomClasse + "." + urlMethode.nomMethode +
                            " - URL: " + util.url + " METHOD: " + util.method));
            out.println("Invoquer la methode qui correspond à l'url");
            methods.forEach((util, urlMethode) -> {
                try {
                    if(util.url.equals(url) && util.method.equals(httpMethod)) {
                        Class<?> clazz = Class.forName(packageName + "." + urlMethode.nomClasse);
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        Method method = clazz.getDeclaredMethod(urlMethode.nomMethode, HttpServletRequest.class, HttpServletResponse.class);
                        method.invoke(instance, req, res);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}