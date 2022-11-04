package ilo;

public interface Labels {

    static String from(String value) {
        return value.toLowerCase().replace(" ", "_");
    }

}
