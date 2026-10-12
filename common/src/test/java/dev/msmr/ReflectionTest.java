package dev.msmr;

import dev.msmr.compat.Reflect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionTest {
    public static class Api {
        public String query(Object value) { return "generic"; }
        public String query(String value) { return "specific"; }
        public boolean permission(boolean value,int count) { return value && count==4; }
        public String ambiguous(String value,Integer other) { return "a"; }
        public String ambiguous(Integer value,String other) { return "b"; }
    }
    @Test void usesMostSpecificPublicOverloadAndCachesIt() throws Exception {
        var api=new Api();
        assertEquals("specific",Reflect.call(api,"query","example"));
        assertEquals("specific",Reflect.call(api,"query","again"));
        assertEquals("specific",Reflect.call(api,"query",(Object)null));
    }
    @Test void primitiveArgumentsAreMatched() throws Exception { assertTrue(Reflect.bool(new Api(),"permission",true,4)); }
    @Test void missingOrAmbiguousApiCannotSilentlyGrantAccess() {
        assertThrows(NoSuchMethodException.class,() -> Reflect.call(new Api(),"missing"));
        assertThrows(NoSuchMethodException.class,() -> Reflect.call(new Api(),"ambiguous",null,null));
    }
}
