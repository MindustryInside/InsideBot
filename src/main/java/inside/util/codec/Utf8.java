package inside.util.codec;

import java.nio.*;
import java.nio.charset.*;

public final class Utf8{

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private Utf8(){
    }

    public static byte[] encode(CharSequence string){
        try{
            ByteBuffer bytes = CHARSET.newEncoder().encode(CharBuffer.wrap(string));
            byte[] bytesCopy = new byte[bytes.limit()];
            System.arraycopy(bytes.array(), 0, bytesCopy, 0, bytes.limit());
            return bytesCopy;
        }catch(CharacterCodingException ex){
            throw new IllegalArgumentException("Encoding failed", ex);
        }
    }

    public static String decode(byte[] bytes){
        try{
            return CHARSET.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        }catch(CharacterCodingException ex){
            throw new IllegalArgumentException("Decoding failed", ex);
        }
    }
}
