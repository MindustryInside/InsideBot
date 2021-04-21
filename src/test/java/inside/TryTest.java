package inside;

import inside.util.Try;
import org.junit.jupiter.api.*;

public class TryTest{

    @Test
    public void hashCodes(){
        Try<String> try0 = Try.success("ok");
        Try<String> try1 = Try.success("ok");
        Assertions.assertEquals(try0.hashCode(), try1.hashCode());

        Try<String> try2 = Try.failure(new Throwable());
        Try<String> try3 = Try.failure(new Throwable());
        Assertions.assertNotEquals(try2.hashCode(), try3.hashCode());
    }

    @Test
    public void equalsAndIdentity(){
        Try<String> try0 = Try.success("ok");
        Try<String> try1 = Try.success("ok");
        Assertions.assertEquals(try0, try1);
        Assertions.assertSame(try0, try0);
        Assertions.assertNotSame(try0, try1);

        Try<String> try2 = Try.failure(new Throwable());
        Try<String> try3 = Try.failure(new Throwable());
        Assertions.assertNotEquals(try2, try3);
        Assertions.assertSame(try2, try2);
        Assertions.assertNotSame(try2, try3);
    }
}
