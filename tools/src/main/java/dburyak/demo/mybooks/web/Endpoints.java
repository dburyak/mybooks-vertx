package dburyak.demo.mybooks.web;

import javax.inject.Singleton;

@Singleton
public class Endpoints {
    private static final String ENDPOINT_HEALTH = "/health";
    private static final String ENDPOINT_READY = "/ready";
    private static final String ENDPOINT_ABOUT = "/about";

    public String getHealth() {
        return ENDPOINT_HEALTH;
    }

    public String getReady() {
        return ENDPOINT_READY;
    }

    public String getAbout() {
        return ENDPOINT_ABOUT;
    }
}
