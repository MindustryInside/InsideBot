package inside.util;

import arc.util.io.ReusableByteInStream;

import java.util.Objects;

public class StringInputStream extends ReusableByteInStream{

    public void writeString(String data){
        Objects.requireNonNull(data, "Data must not be null");
        setBytes(data.getBytes());
    }
}
