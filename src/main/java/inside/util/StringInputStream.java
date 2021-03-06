package inside.util;

import arc.util.Strings;
import arc.util.io.ReusableByteInStream;

import java.util.Objects;

public class StringInputStream extends ReusableByteInStream{

    public StringInputStream(){
        super();
    }

    public StringInputStream writeString(String data){
        Objects.requireNonNull(data, "data");
        setBytes(data.getBytes(Strings.utf8));
        return this;
    }
}
