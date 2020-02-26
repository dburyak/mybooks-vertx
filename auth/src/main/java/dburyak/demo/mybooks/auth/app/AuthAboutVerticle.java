package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.AboutVerticle;
import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;

import javax.inject.Singleton;

@Singleton
public class AuthAboutVerticle extends AboutVerticle {
    public static final String ADDR_BRIEF_INFO = AuthAboutVerticle.class.getCanonicalName() + ".briefInfo";
    public static final String ADDR_DETAILED_INFO = AuthAboutVerticle.class.getCanonicalName() + ".detailedInfo";

    @Override
    protected String getBriefInfoAddr() {
        return ADDR_BRIEF_INFO;
    }

    @Override
    protected String getDetailedInfoAddr() {
        return ADDR_DETAILED_INFO;
    }

    public static class Producer extends MicronautVerticleProducer {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new AuthAboutVerticle();
        }
    }
}
