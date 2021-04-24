package inside;

import inside.util.Try;
import org.junit.jupiter.api.*;

import java.util.function.*;

public class TryTest{

    @Test
    public void jvmErrors(){
        Assertions.assertThrows(VirtualMachineError.class, () -> Try.run(() -> {throw new InternalError();}));
        Assertions.assertThrows(ThreadDeath.class, () -> Try.run(() -> {throw new ThreadDeath();}));
        Assertions.assertThrows(LinkageError.class, () -> Try.run(() -> {throw new LinkageError();}));
        Assertions.assertDoesNotThrow(() -> Try.run(() -> {throw new RuntimeException();}));
    }

    @Test
    public void andThen(){
        Assertions.assertTrue(Try.success("ok").andThen(() -> {throw new RuntimeException();}).isFailure());
        Assertions.assertTrue(Try.failure(new RuntimeException())
                .andThen(() -> {throw new RuntimeException();})
                .isFailure());
    }

    @Test
    public void filters(){
        Try<String> try0 = Try.success("ok");
        Assertions.assertTrue(try0.filter("ok"::equals).isSuccess());
        Assertions.assertTrue(try0.filter("no"::equals).isFailure());
        Assertions.assertTrue(try0.filter("no"::equals, (Supplier<Throwable>)RuntimeException::new)
                .getCause() instanceof RuntimeException);
        Assertions.assertTrue(try0.filter("no"::equals, (Function<String, Throwable>)RuntimeException::new)
                .getCause() instanceof RuntimeException);
    }

    @Test
    public void flatMap(){
        Try<String> try0 = Try.success("ok");
        Assertions.assertNotEquals(try0, try0.flatMap(s -> Try.ofSupplier(() -> s + "!")));

        Try<String> try1 = Try.failure(new RuntimeException());
        Assertions.assertEquals(try1, try1.flatMap(s -> Try.ofSupplier(() -> s + "!")));
    }

    @Test
    public void mappings(){
        Try<String> try0 = Try.success("ok")
                .map(s -> s + "!");
        Assertions.assertEquals(Try.success("ok!"), try0);

        Try<String> try1 = Try.success("ok")
                .map(s -> {throw new RuntimeException();});
        Assertions.assertTrue(try1.isFailure());
    }

    @Test
    public void andFinallyActions(){
        Try<String> try0 = Try.success("ok");
        Assertions.assertTrue(try0.andFinally(() -> {throw new RuntimeException();}).isFailure());
        Assertions.assertTrue(try0.andFinally(() -> {}).isSuccess());
    }

    @Test
    public void recovers(){
        Try<String> try0 = Try.failure(new RuntimeException());
        Assertions.assertTrue(try0.recover(t -> "ok").isSuccess());
        Assertions.assertTrue(try0.recoverWith(t -> Try.success("ok")).isSuccess());
        Assertions.assertTrue(try0.recoverWith(t -> Try.failure(new AssertionError())).isFailure());
        Assertions.assertTrue(try0.recover(Throwable.class, t -> "no").isSuccess());
        Assertions.assertTrue(try0.recoverWith(Throwable.class, t -> Try.success("no")).isSuccess());
        Assertions.assertTrue(try0.recoverWith(Throwable.class, t -> Try.failure(new AssertionError())).isFailure());
    }

    @Test
    public void transforms(){
        Try<String> try0 = Try.success("ok");
        Assertions.assertEquals("ok", try0.transform(Try::get));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> try0.transform(Try::getCause));
    }

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
