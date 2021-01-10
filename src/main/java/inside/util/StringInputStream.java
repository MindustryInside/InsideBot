package inside.util;

import arc.util.io.ReusableByteInStream;

import java.util.Objects;

public class StringInputStream extends ReusableByteInStream{

    public StringInputStream(){
        super();
    }

    public StringInputStream(String data){
        super();
        writeString(data);
    }

    public void writeString(String data){
        Objects.requireNonNull(data, "data");
        setBytes(data.getBytes());
    }
}
