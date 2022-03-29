package inside.interaction.chatinput.common;

import discord4j.common.util.Snowflake;

import java.util.List;
import java.util.Objects;

public class TicTacToeGame {
    // В соответствии с константами ниже
    private static final List<String> SIGNS = List.of("\uD83D\uDFE6", "\uD83C\uDDFD", "\uD83C\uDDF4");

    private static final byte ABSENT = 0;
    private static final byte X_MASK = 1;
    private static final byte O_MASK = 2;

    private final byte[][] grid;
    private final Snowflake xUserId;
    private final Snowflake oUserId;

    private boolean lastX;

    public TicTacToeGame(int size, Snowflake xUserId, Snowflake oUserId, boolean firstX) {
        this.grid = new byte[size][size];
        this.xUserId = Objects.requireNonNull(xUserId, "xUserId");
        this.oUserId = Objects.requireNonNull(oUserId, "oUserId");

        lastX = !firstX;
    }

    public boolean has(int x, int y) {
        if (x > grid.length || y > grid[x].length) {
            return false;
        }

        byte s = grid[x][y];
        return s == ABSENT;
    }

    public boolean play(int x, int y, boolean xSign) {
        if (x > grid.length || y > grid[x].length) {
            return false;
        }

        byte s = grid[x][y];
        if (s != ABSENT) {
            return false;
        }

        grid[x][y] = xSign ? X_MASK : O_MASK;
        lastX = xSign;

        return true;
    }

    public State state() {
        Counters ct = new Counters();

        // x
        //  x
        //   x
        for (int x = 0; x < grid.length; x++) {
            ct.apply(grid[x][x]);
        }

        if (ct.xseq == grid.length || ct.oseq == grid.length) {
            return ct.xseq == grid.length ? State.X_WIN : State.O_WIN;
        }

        ct.reset();

        //   x
        //  x
        // x
        for (int x = 0; x < grid.length; x++) {
            ct.apply(grid[x][grid.length - 1 - x]);
        }

        if (ct.xseq == grid.length || ct.oseq == grid.length) {
            return ct.xseq == grid.length ? State.X_WIN : State.O_WIN;
        }

        for (int x = 0; x < grid.length; x++) {
            ct.reset();

            for (int y = 0; y < grid[x].length; y++) {
                ct.apply(grid[x][y]);

                if (ct.xseq == grid.length || ct.oseq == grid.length) {
                    return ct.xseq == grid.length ? State.X_WIN : State.O_WIN;
                }
            }

            ct.reset();

            for (int y = 0; y < grid[x].length; y++) {
                ct.apply(grid[y][x]);

                if (ct.xseq == grid.length || ct.oseq == grid.length) {
                    return ct.xseq == grid.length ? State.X_WIN : State.O_WIN;
                }
            }
        }

        return ct.allpresent ? State.NEUTRAL : State.PLAYING;
    }

    public String asText() {
        StringBuilder builder = new StringBuilder();
        for (byte[] bytes : grid) {
            for (int v : bytes) {
                String s = SIGNS.get(v);

                builder.append(s).append("  ");
            }

            builder.append('\n');
        }

        return builder.toString();
    }

    public boolean isLastX() {
        return lastX;
    }

    public Snowflake getXUserId() {
        return xUserId;
    }

    public Snowflake getOUserId() {
        return oUserId;
    }

    static class Counters {
        int xseq = 0;
        int oseq = 0;
        boolean allpresent = true;

        void reset() {
            xseq = oseq = 0;
        }

        void apply(byte s) {
            switch (s) {
                case X_MASK -> {
                    xseq++;
                    oseq = 0;
                }
                case O_MASK -> {
                    oseq++;
                    xseq = 0;
                }
                case ABSENT -> {
                    reset();
                    allpresent = false;
                }
            }
        }
    }

    public enum State {
        X_WIN,
        O_WIN,
        NEUTRAL,
        PLAYING
    }
}
