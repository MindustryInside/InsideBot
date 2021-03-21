package inside.util;

import java.io.ByteArrayInputStream;
import java.util.Objects;

public class ReusableByteInputStream extends ByteArrayInputStream{

    public ReusableByteInputStream(){
        super(new byte[0]);
    }

    public ReusableByteInputStream writeString(String data){
        Objects.requireNonNull(data, "data");
        setBytes(data.getBytes(Strings.utf8));
        return this;
    }

    public int position(){
        return pos;
    }

    public void setBytes(byte[] bytes){
        Objects.requireNonNull(bytes, "bytes");

        pos = 0;
        count = bytes.length;
        mark = 0;
        buf = bytes;
    }

    public void setBytes(byte[] bytes, int offset, int length){
        Objects.requireNonNull(bytes, "bytes");

        buf = bytes;
        pos = offset;
        count = Math.min(offset + length, bytes.length);
        mark = offset;
    }
}
