package inside.util.io;

import java.io.ByteArrayOutputStream;

public class ReusableByteOutputStream extends ByteArrayOutputStream{

    public ReusableByteOutputStream(int capacity){
        super(capacity);
    }

    public ReusableByteOutputStream(){}

    @Override
    public synchronized void reset(){
        super.reset();
        buf = new byte[0];
    }

    public byte[] getBytes(){
        return buf;
    }
}
