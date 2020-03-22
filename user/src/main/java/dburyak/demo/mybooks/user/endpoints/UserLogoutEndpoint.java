package dburyak.demo.mybooks.user.endpoints;

import dburyak.demo.mybooks.web.Endpoint;
import io.vertx.core.http.HttpMethod;

import javax.inject.Singleton;

import static io.vertx.core.http.HttpMethod.POST;

@Singleton
public class UserLogoutEndpoint implements Endpoint {

    @Override
    public String getPath() {
        return "/user/logout";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return POST;
    }
}
