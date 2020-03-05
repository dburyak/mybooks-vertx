package dburyak.demo.mybooks.util;

import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;
import java.util.Base64;

@Factory
public class Base64Factory {

    @Singleton
    public Base64.Decoder base64Decoder() {
        return Base64.getUrlDecoder();
    }

    @Singleton
    public Base64.Encoder base64Encoder() {
        return Base64.getUrlEncoder();
    }
}
