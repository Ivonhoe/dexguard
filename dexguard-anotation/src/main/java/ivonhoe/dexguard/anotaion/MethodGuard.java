package ivonhoe.dexguard.anotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface MethodGuard {
    String name = null;
}
