package inside.util;

import java.io.ByteArrayOutputStream;

public class ReusableOutputStream extends ByteArrayOutputStream{

    public ReusableOutputStream(int capacity){
        super(capacity);
    }

    public ReusableOutputStream(){}

    @Override
    public synchronized void reset(){
        super.reset();
        buf = new byte[0];
    }

    public byte[] getBytes(){
        return buf;
    }
}
