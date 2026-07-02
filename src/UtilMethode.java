package src;

import java.util.Objects;

public class UtilMethode {
    String url;
    String method;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UtilMethode other = (UtilMethode) obj;
        return url.equals(other.url) && method.equals(other.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }

}
