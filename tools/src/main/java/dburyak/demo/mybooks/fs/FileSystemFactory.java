package dburyak.demo.mybooks.fs;

import io.micronaut.context.annotation.Factory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

import javax.inject.Singleton;

@Factory
public class FileSystemFactory {

    @Singleton
    public FileSystem fileSystem(Vertx vertx) {
        return vertx.fileSystem();
    }
}
