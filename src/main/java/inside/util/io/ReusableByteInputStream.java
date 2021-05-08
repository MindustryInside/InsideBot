package inside.util.io;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ReusableByteInputStream extends ByteArrayInputStream{

    public ReusableByteInputStream(byte[] buf){
        super(buf);
    }

    public static ReusableByteInputStream ofString(String data){
        Objects.requireNonNull(data, "data");
        return new ReusableByteInputStream(data.getBytes(StandardCharsets.UTF_8));
    }

    public int position(){
        return pos;
    }

    public void setBytes(byte[] bytes){
        setBytes(bytes, 0, bytes.length);
    }

    public void setBytes(byte[] bytes, int offset, int length){
        Objects.requireNonNull(bytes, "bytes");

        buf = bytes;
        pos = offset;
        count = Math.min(offset + length, bytes.length);
        mark = offset;
    }
}
