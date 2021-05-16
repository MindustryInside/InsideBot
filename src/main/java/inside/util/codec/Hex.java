package inside.util.codec;

public final class Hex{

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Hex(){
    }

    public static char[] encode(byte[] bytes){
        int length = bytes.length;
        char[] result = new char[2 * length];
        int j = 0;
        for(byte b : bytes){
            // Char for top 4 bits
            result[j++] = HEX[(0xF0 & b) >>> 4];
            // Bottom 4
            result[j++] = HEX[0x0F & b];
        }
        return result;
    }

    public static byte[] decode(CharSequence s){
        int length = s.length();
        if(length % 2 != 0){
            throw new IllegalArgumentException("Hex-encoded string must have an even number of characters");
        }
        byte[] result = new byte[length / 2];
        for(int i = 0; i < length; i += 2){
            int msb = Character.digit(s.charAt(i), 16);
            int lsb = Character.digit(s.charAt(i + 1), 16);
            if(msb < 0 || lsb < 0){
                throw new IllegalArgumentException(
                        "Detected a Non-hex character at " + (i + 1) + " or " + (i + 2) + " position");
            }
            result[i / 2] = (byte)(msb << 4 | lsb);
        }
        return result;
    }

}
