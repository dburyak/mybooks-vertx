package dburyak.demo.mybooks.util;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.regex.Pattern;

@Singleton
public class RegexUtil {
    private static final String UUID_PATTERN_STR =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private Pattern uuidPattern;

    public String getUuidPatternString() {
        return UUID_PATTERN_STR;
    }

    public Pattern getUuidPattern() {
        return uuidPattern;
    }

    @PostConstruct
    private void init() {
        uuidPattern = Pattern.compile(UUID_PATTERN_STR);
    }
}
