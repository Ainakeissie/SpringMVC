package aina.util;

import java.util.Objects;

public class UrlMethod {
    private final String url;
    private final String methodType;

    public UrlMethod(String url, String methodType) {
        this.url = url;
        this.methodType = methodType;
    }

    public String getUrl() {
        return url;
    }

    public String getMethodType() {
        return methodType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, methodType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UrlMethod other = (UrlMethod) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (methodType == null) {
            if (other.methodType != null)
                return false;
        } else if (!methodType.equals(other.methodType))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[url: " + url + ", method : " + methodType + "]";
    }

}
