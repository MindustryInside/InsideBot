package inside.util;

import reactor.util.annotation.Nullable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

// copied from https://github.com/google/diff-match-patch
public class Differ{
    /** Cost of an empty edit operation in terms of edit characters. */
    public short diffEditCost = 4;
    /** At what point is no match declared (0.0 = perfection, 1.0 = very loose). */
    public float matchThreshold = 0.5f;
    /**
     * How far to search for a match (0 = exact location, 1000+ = broad match).
     * A match this many characters away from the expected location will add
     * 1.0 to the score (0.0 is a perfect match).
     */
    public int matchDistance = 1000;
    /**
     * When deleting a large block of text (over ~64 characters), how close do
     * the contents have to be to match the expected contents. (0.0 = perfection,
     * 1.0 = very loose).  Note that Match_Threshold controls how closely the
     * end points of a delete need to match.
     */
    public float patchDeleteThreshold = 0.5f;
    /** Chunk size for context length. */
    public short patchMargin = 4;
    /** The number of bits in an int. */
    private final short matchMaxBits = 32;

    /**
     * Internal class for returning results from diff_linesToChars().
     * Other less paranoid languages just use a three-element array.
     */
    protected static class LinesToCharsResult{
        protected String chars1;
        protected String chars2;
        protected List<String> lineArray;

        protected LinesToCharsResult(String chars1, String chars2, List<String> lineArray){
            this.chars1 = chars1;
            this.chars2 = chars2;
            this.lineArray = lineArray;
        }
    }

    /**
     * The data structure representing a diff is a Linked list of Diff objects:
     * {Diff(Operation.DELETE, "Hello"), Diff(Operation.INSERT, "Goodbye"),
     * Diff(Operation.EQUAL, " world.")}
     * which means: delete "Hello", add "Goodbye" and keep " world."
     */
    public enum Operation{
        DELETE, INSERT, EQUAL
    }

    /**
     * Find the differences between two texts.
     * Run a faster, slightly less optimal diff.
     * This method allows the 'checklines' of diff_main() to be optional.
     * Most of the time checklines is wanted, so default to true.
     *
     * @param text1 Old string to be diffed.
     * @param text2 New string to be diffed.
     * @return Linked List of Diff objects.
     */
    public LinkedList<Diff> getDifference(String text1, String text2){
        return getDifference(text1, text2, true);
    }

    /**
     * Find the differences between two texts.  Simplifies the problem by
     * stripping any common prefix or suffix off the texts before diffing.
     *
     * @param text1 Old string to be diffed.
     * @param text2 New string to be diffed.
     * @param checklines Speedup flag.  If false, then don't run a
     * line-level diff first to identify the changed areas.
     * If true, then run a faster slightly less optimal diff.
     * @return Linked List of Diff objects.
     */
    private LinkedList<Diff> getDifference(String text1, String text2, boolean checklines){
        Objects.requireNonNull(text1, "text1");
        Objects.requireNonNull(text2, "text2");

        // Check for equality (speedup).
        LinkedList<Diff> diffs;
        if(text1.equals(text2)){
            diffs = new LinkedList<>();
            if(text1.length() != 0){
                diffs.add(new Diff(Operation.EQUAL, text1));
            }
            return diffs;
        }

        // Trim off common prefix (speedup).
        int commonlength = diffCommonPrefix(text1, text2);
        String commonprefix = text1.substring(0, commonlength);
        text1 = text1.substring(commonlength);
        text2 = text2.substring(commonlength);

        // Trim off common suffix (speedup).
        commonlength = diffCommonSuffix(text1, text2);
        String commonsuffix = text1.substring(text1.length() - commonlength);
        text1 = text1.substring(0, text1.length() - commonlength);
        text2 = text2.substring(0, text2.length() - commonlength);

        // Compute the diff on the middle block.
        diffs = diffCompute(text1, text2, checklines);

        // Restore the prefix and suffix.
        if(commonprefix.length() != 0){
            diffs.addFirst(new Diff(Operation.EQUAL, commonprefix));
        }
        if(commonsuffix.length() != 0){
            diffs.addLast(new Diff(Operation.EQUAL, commonsuffix));
        }

        diffCleanupAndMerge(diffs);
        return diffs;
    }

    /**
     * Find the differences between two texts.  Assumes that the texts do not
     * have any common prefix or suffix.
     *
     * @param text1 Old string to be diffed.
     * @param text2 New string to be diffed.
     * @param checkLines Speedup flag.  If false, then don't run a
     * line-level diff first to identify the changed areas.
     * If true, then run a faster slightly less optimal diff.
     * @return Linked List of Diff objects.
     */
    private LinkedList<Diff> diffCompute(String text1, String text2, boolean checkLines){
        LinkedList<Diff> diffs = new LinkedList<>();

        if(text1.length() == 0){
            // Just add some text (speedup).
            diffs.add(new Diff(Operation.INSERT, text2));
            return diffs;
        }

        if(text2.length() == 0){
            // Just delete some text (speedup).
            diffs.add(new Diff(Operation.DELETE, text1));
            return diffs;
        }

        String longText = text1.length() > text2.length() ? text1 : text2;
        String shortText = text1.length() > text2.length() ? text2 : text1;
        int i = longText.indexOf(shortText);
        if(i != -1){
            // Shorter text is inside the longer text (speedup).
            Operation op = text1.length() > text2.length() ?
                    Operation.DELETE : Operation.INSERT;
            diffs.add(new Diff(op, longText.substring(0, i)));
            diffs.add(new Diff(Operation.EQUAL, shortText));
            diffs.add(new Diff(op, longText.substring(i + shortText.length())));
            return diffs;
        }

        if(shortText.length() == 1){
            // Single character string.
            // After the previous speedup, the character can't be an equality.
            diffs.add(new Diff(Operation.DELETE, text1));
            diffs.add(new Diff(Operation.INSERT, text2));
            return diffs;
        }

        // Check to see if the problem can be split in two.
        String[] halfMatch = diffHalfMatch(text1, text2);
        if(halfMatch != null){
            // A half-match was found, sort out the return data.
            String text1A = halfMatch[0];
            String text1B = halfMatch[1];
            String text2A = halfMatch[2];
            String text2B = halfMatch[3];
            String midCommon = halfMatch[4];
            // Send both pairs off for separate processing.
            LinkedList<Diff> diffsA = getDifference(text1A, text2A, checkLines);
            LinkedList<Diff> diffsB = getDifference(text1B, text2B, checkLines);
            // Merge the results.
            diffs = diffsA;
            diffs.add(new Diff(Operation.EQUAL, midCommon));
            diffs.addAll(diffsB);
            return diffs;
        }

        if(checkLines && text1.length() > 100 && text2.length() > 100){
            return diffLineMode(text1, text2);
        }

        return diffBisect(text1, text2);
    }

    /**
     * Do a quick line-level diff on both strings, then rediff the parts for
     * greater accuracy.
     * This speedup can produce non-minimal diffs.
     *
     * @param text1 Old string to be diffed.
     * @param text2 New string to be diffed.
     * @return Linked List of Diff objects.
     */
    private LinkedList<Diff> diffLineMode(String text1, String text2){
        // Scan the text on a line-by-line basis first.
        LinesToCharsResult a = diffLinesToChars(text1, text2);
        text1 = a.chars1;
        text2 = a.chars2;
        List<String> lines = a.lineArray;

        LinkedList<Diff> diffs = getDifference(text1, text2, false);

        // Convert the diff back to original text.
        charsToLines(diffs, lines);
        // Eliminate freak matches (e.g. blank lines)
        cleanupDiffSemantic(diffs);

        // Rediff any replacement blocks, this time character-by-character.
        // Add a dummy entry at the end.
        diffs.add(new Diff(Operation.EQUAL, ""));
        int countDelete = 0;
        int countInsert = 0;
        StringBuilder textDelete = new StringBuilder();
        StringBuilder textInsert = new StringBuilder();
        ListIterator<Diff> pointer = diffs.listIterator();
        Diff thisDiff = pointer.next();
        while(thisDiff != null){
            switch(thisDiff.operation){
                case INSERT:
                    countInsert++;
                    textInsert.append(thisDiff.text);
                    break;
                case DELETE:
                    countDelete++;
                    textDelete.append(thisDiff.text);
                    break;
                case EQUAL:
                    // Upon reaching an equality, check for prior redundancies.
                    if(countDelete >= 1 && countInsert >= 1){
                        // Delete the offending records and add the merged ones.
                        pointer.previous();
                        for(int j = 0; j < countDelete + countInsert; j++){
                            pointer.previous();
                            pointer.remove();
                        }
                        for(Diff subDiff : getDifference(textDelete.toString(), textInsert.toString(), false)){
                            pointer.add(subDiff);
                        }
                    }
                    countInsert = 0;
                    countDelete = 0;
                    textDelete = new StringBuilder();
                    textInsert = new StringBuilder();
                    break;
            }
            thisDiff = pointer.hasNext() ? pointer.next() : null;
        }
        diffs.removeLast();  // Remove the dummy entry at the end.

        return diffs;
    }

    /**
     * Find the 'middle snake' of a diff, split the problem in two
     * and return the recursively constructed diff.
     * See Myers 1986 paper: An O(ND) Difference Algorithm and Its Variations.
     *
     * @param text1 Old string to be diffed.
     * @param text2 New string to be diffed.
     * @return LinkedList of Diff objects.
     */
    protected LinkedList<Diff> diffBisect(String text1, String text2){
        // Cache the text lengths to prevent multiple calls.
        int text1Length = text1.length();
        int text2Length = text2.length();
        int maxD = (text1Length + text2Length + 1) / 2;
        int vLength = 2 * maxD;
        int[] v1 = new int[vLength];
        int[] v2 = new int[vLength];
        for(int x = 0; x < vLength; x++){
            v1[x] = -1;
            v2[x] = -1;
        }
        v1[maxD + 1] = 0;
        v2[maxD + 1] = 0;
        int delta = text1Length - text2Length;
        // If the total number of characters is odd, then the front path will
        // collide with the reverse path.
        boolean front = delta % 2 != 0;
        // Offsets for start and end of k loop.
        // Prevents mapping of space beyond the grid.
        int k1start = 0;
        int k1end = 0;
        int k2start = 0;
        int k2end = 0;
        for(int d = 0; d < maxD; d++){
            // Walk the front path one step.
            for(int k1 = -d + k1start; k1 <= d - k1end; k1 += 2){
                int k1_offset = maxD + k1;
                int x1;
                if(k1 == -d || k1 != d && v1[k1_offset - 1] < v1[k1_offset + 1]){
                    x1 = v1[k1_offset + 1];
                }else{
                    x1 = v1[k1_offset - 1] + 1;
                }
                int y1 = x1 - k1;
                while(x1 < text1Length && y1 < text2Length
                        && text1.charAt(x1) == text2.charAt(y1)){
                    x1++;
                    y1++;
                }
                v1[k1_offset] = x1;
                if(x1 > text1Length){
                    // Ran off the right of the graph.
                    k1end += 2;
                }else if(y1 > text2Length){
                    // Ran off the bottom of the graph.
                    k1start += 2;
                }else if(front){
                    int k2_offset = maxD + delta - k1;
                    if(k2_offset >= 0 && k2_offset < vLength && v2[k2_offset] != -1){
                        // Mirror x2 onto top-left coordinate system.
                        int x2 = text1Length - v2[k2_offset];
                        if(x1 >= x2){
                            // Overlap detected.
                            return diffBisectSplit(text1, text2, x1, y1);
                        }
                    }
                }
            }

            // Walk the reverse path one step.
            for(int k2 = -d + k2start; k2 <= d - k2end; k2 += 2){
                int k2_offset = maxD + k2;
                int x2;
                if(k2 == -d || k2 != d && v2[k2_offset - 1] < v2[k2_offset + 1]){
                    x2 = v2[k2_offset + 1];
                }else{
                    x2 = v2[k2_offset - 1] + 1;
                }
                int y2 = x2 - k2;
                while(x2 < text1Length && y2 < text2Length &&
                        text1.charAt(text1Length - x2 - 1) == text2.charAt(text2Length - y2 - 1)){
                    x2++;
                    y2++;
                }
                v2[k2_offset] = x2;
                if(x2 > text1Length){
                    // Ran off the left of the graph.
                    k2end += 2;
                }else if(y2 > text2Length){
                    // Ran off the top of the graph.
                    k2start += 2;
                }else if(!front){
                    int k1_offset = maxD + delta - k2;
                    if(k1_offset >= 0 && k1_offset < vLength && v1[k1_offset] != -1){
                        int x1 = v1[k1_offset];
                        int y1 = maxD + x1 - k1_offset;
                        // Mirror x2 onto top-left coordinate system.
                        x2 = text1Length - x2;
                        if(x1 >= x2){
                            // Overlap detected.
                            return diffBisectSplit(text1, text2, x1, y1);
                        }
                    }
                }
            }
        }
        // Diff took too long and hit the deadline or
        // number of diffs equals number of characters, no commonality at all.
        LinkedList<Diff> diffs = new LinkedList<>();
        diffs.add(new Diff(Operation.DELETE, text1));
        diffs.add(new Diff(Operation.INSERT, text2));
        return diffs;
    }

    /**
     * Given the location of the 'middle snake', split the diff in two parts
     * and recurse.
     *
     * @param text1 Old string to be diffed.
     * @param text2 New string to be diffed.
     * @param x Index of split point in text1.
     * @param y Index of split point in text2.
     * @return LinkedList of Diff objects.
     */
    private LinkedList<Diff> diffBisectSplit(String text1, String text2, int x, int y){
        String text1a = text1.substring(0, x);
        String text2a = text2.substring(0, y);
        String text1b = text1.substring(x);
        String text2b = text2.substring(y);

        // Compute both diffs serially.
        LinkedList<Diff> diffsA = getDifference(text1a, text2a, false);
        LinkedList<Diff> diffsB = getDifference(text1b, text2b, false);

        diffsA.addAll(diffsB);
        return diffsA;
    }

    /**
     * Split two texts into a list of strings.  Reduce the texts to a string of
     * hashes where each Unicode character represents one line.
     *
     * @param text1 First string.
     * @param text2 Second string.
     * @return An object containing the encoded text1, the encoded text2 and
     * the List of unique strings.  The zeroth element of the List of
     * unique strings is intentionally blank.
     */
    protected LinesToCharsResult diffLinesToChars(String text1, String text2){
        List<String> lineArray = new ArrayList<>();
        Map<String, Integer> lineHash = new HashMap<>();
        // e.g. linearray[4] == "Hello\n"
        // e.g. linehash.get("Hello\n") == 4

        // "\x00" is a valid character, but various debuggers don't like it.
        // So we'll insert a junk entry to avoid generating a null character.
        lineArray.add("");

        // Allocate 2/3rds of the space for text1, the rest for text2.
        String chars1 = diffLinesToCharsMunge(text1, lineArray, lineHash, 40000);
        String chars2 = diffLinesToCharsMunge(text2, lineArray, lineHash, 65535);
        return new LinesToCharsResult(chars1, chars2, lineArray);
    }

    /**
     * Split a text into a list of strings.  Reduce the texts to a string of
     * hashes where each Unicode character represents one line.
     *
     * @param text String to encode.
     * @param lineArray List of unique strings.
     * @param lineHash Map of strings to indices.
     * @param maxLines Maximum length of lineArray.
     * @return Encoded string.
     */
    private String diffLinesToCharsMunge(String text, List<String> lineArray,
                                         Map<String, Integer> lineHash, int maxLines){
        int lineStart = 0;
        int lineEnd = -1;
        String line;
        StringBuilder chars = new StringBuilder();
        // Walk the text, pulling out a substring for each line.
        // text.split('\n') would would temporarily double our memory footprint.
        // Modifying text would create many large strings to garbage collect.
        while(lineEnd < text.length() - 1){
            lineEnd = text.indexOf('\n', lineStart);
            if(lineEnd == -1){
                lineEnd = text.length() - 1;
            }
            line = text.substring(lineStart, lineEnd + 1);

            if(lineHash.containsKey(line)){
                chars.append((char)(int)lineHash.get(line));
            }else{
                if(lineArray.size() == maxLines){
                    // Bail out at 65535 because
                    // String.valueOf((char)65536).equals(String.valueOf(((char)0)))
                    line = text.substring(lineStart);
                    lineEnd = text.length();
                }
                lineArray.add(line);
                lineHash.put(line, lineArray.size() - 1);
                chars.append((char)(lineArray.size() - 1));
            }
            lineStart = lineEnd + 1;
        }
        return chars.toString();
    }

    /**
     * Rehydrate the text in a diff from a string of line hashes to real lines of
     * text.
     *
     * @param diffs List of Diff objects.
     * @param lineArray List of unique strings.
     */
    protected void charsToLines(List<Diff> diffs, List<String> lineArray){
        StringBuilder text;
        for(Diff diff : diffs){
            text = new StringBuilder();
            for(int j = 0; j < diff.text.length(); j++){
                text.append(lineArray.get(diff.text.charAt(j)));
            }
            diff.text = text.toString();
        }
    }

    /**
     * Determine the common prefix of two strings
     *
     * @param text1 First string.
     * @param text2 Second string.
     * @return The number of characters common to the start of each string.
     */
    public int diffCommonPrefix(String text1, String text2){
        // Performance analysis: https://neil.fraser.name/news/2007/10/09/
        int n = Math.min(text1.length(), text2.length());
        for(int i = 0; i < n; i++){
            if(text1.charAt(i) != text2.charAt(i)){
                return i;
            }
        }
        return n;
    }

    /**
     * Determine the common suffix of two strings
     *
     * @param text1 First string.
     * @param text2 Second string.
     * @return The number of characters common to the end of each string.
     */
    public int diffCommonSuffix(String text1, String text2){
        // Performance analysis: https://neil.fraser.name/news/2007/10/09/
        int text1Length = text1.length();
        int text2Length = text2.length();
        int n = Math.min(text1Length, text2Length);
        for(int i = 1; i <= n; i++){
            if(text1.charAt(text1Length - i) != text2.charAt(text2Length - i)){
                return i - 1;
            }
        }
        return n;
    }

    /**
     * Determine if the suffix of one string is the prefix of another.
     *
     * @param text1 First string.
     * @param text2 Second string.
     * @return The number of characters common to the end of the first
     * string and the start of the second string.
     */
    protected int diffCommonOverlap(String text1, String text2){
        // Cache the text lengths to prevent multiple calls.
        int text1_length = text1.length();
        int text2_length = text2.length();
        // Eliminate the null case.
        if(text1_length == 0 || text2_length == 0){
            return 0;
        }
        // Truncate the longer string.
        if(text1_length > text2_length){
            text1 = text1.substring(text1_length - text2_length);
        }else if(text1_length < text2_length){
            text2 = text2.substring(0, text1_length);
        }
        int textLength = Math.min(text1_length, text2_length);
        // Quick check for the worst case.
        if(text1.equals(text2)){
            return textLength;
        }

        // Start by looking for a single character match
        // and increase length until no match is found.
        // Performance analysis: https://neil.fraser.name/news/2010/11/04/
        int best = 0;
        int length = 1;
        while(true){
            String pattern = text1.substring(textLength - length);
            int found = text2.indexOf(pattern);
            if(found == -1){
                return best;
            }
            length += found;
            if(found == 0 || text1.substring(textLength - length).equals(text2.substring(0, length))){
                best = length;
                length++;
            }
        }
    }

    /**
     * Do the two texts share a substring which is at least half the length of
     * the longer text?
     * This speedup can produce non-minimal diffs.
     *
     * @param text1 First string.
     * @param text2 Second string.
     * @return Five element String array, containing the prefix of text1, the
     * suffix of text1, the prefix of text2, the suffix of text2 and the
     * common middle.  Or null if there was no match.
     */
    @Nullable
    protected String[] diffHalfMatch(String text1, String text2){
        String longText = text1.length() > text2.length() ? text1 : text2;
        String shortText = text1.length() > text2.length() ? text2 : text1;
        if(longText.length() < 4 || shortText.length() * 2 < longText.length()){
            return null;  // Pointless.
        }

        // First check if the second quarter is the seed for a half-match.
        String[] hm1 = diffHalfMatchI(longText, shortText, (longText.length() + 3) / 4);
        // Check again based on the third quarter.
        String[] hm2 = diffHalfMatchI(longText, shortText, (longText.length() + 1) / 2);
        String[] hm;
        if(hm1 == null && hm2 == null){
            return null;
        }else if(hm2 == null){
            hm = hm1;
        }else if(hm1 == null){
            hm = hm2;
        }else{
            // Both matched.  Select the longest.
            hm = hm1[4].length() > hm2[4].length() ? hm1 : hm2;
        }

        // A half-match was found, sort out the return data.
        if(text1.length() > text2.length()){
            return hm;
            //return new String[]{hm[0], hm[1], hm[2], hm[3], hm[4]};
        }else{
            return new String[]{hm[2], hm[3], hm[0], hm[1], hm[4]};
        }
    }

    /**
     * Does a substring of shortText exist within longtext such that the
     * substring is at least half the length of longtext?
     *
     * @param longtext Longer string.
     * @param shortText Shorter string.
     * @param i Start index of quarter length substring within longtext.
     * @return Five element String array, containing the prefix of longtext, the
     * suffix of longtext, the prefix of shortText, the suffix of shortText
     * and the common middle.  Or null if there was no match.
     */
    @Nullable
    private String[] diffHalfMatchI(String longtext, String shortText, int i){
        // Start with a 1/4 length substring at position i as a seed.
        String seed = longtext.substring(i, i + longtext.length() / 4);
        int j = -1;
        String best_common = "";
        String bestLongtextA = "", bestLongtextB = "";
        String bestShortTextA = "", bestShortTextB = "";
        while((j = shortText.indexOf(seed, j + 1)) != -1){
            int prefixLength = diffCommonPrefix(longtext.substring(i),
                    shortText.substring(j));
            int suffixLength = diffCommonSuffix(longtext.substring(0, i),
                    shortText.substring(0, j));
            if(best_common.length() < suffixLength + prefixLength){
                best_common = shortText.substring(j - suffixLength, j)
                        + shortText.substring(j, j + prefixLength);
                bestLongtextA = longtext.substring(0, i - suffixLength);
                bestLongtextB = longtext.substring(i + prefixLength);
                bestShortTextA = shortText.substring(0, j - suffixLength);
                bestShortTextB = shortText.substring(j + prefixLength);
            }
        }
        if(best_common.length() * 2 >= longtext.length()){
            return new String[]{bestLongtextA, bestLongtextB, bestShortTextA, bestShortTextB, best_common};
        }else{
            return null;
        }
    }

    /**
     * Reduce the number of edits by eliminating semantically trivial equalities.
     * @param diffs LinkedList of Diff objects.
     */
    public void cleanupDiffSemantic(LinkedList<Diff> diffs){
        if(diffs.isEmpty()){
            return;
        }
        boolean changes = false;
        Deque<Diff> equalities = new ArrayDeque<>();  // Double-ended queue of qualities.
        String lastEquality = null; // Always equal to equalities.peek().text
        ListIterator<Diff> pointer = diffs.listIterator();
        // Number of characters that changed prior to the equality.
        int lengthInsertions1 = 0;
        int lengthDeletions1 = 0;
        // Number of characters that changed after the equality.
        int lengthInsertions2 = 0;
        int lengthDeletions2 = 0;
        Diff thisDiff = pointer.next();
        while(thisDiff != null){
            if(thisDiff.operation == Operation.EQUAL){
                // Equality found.
                equalities.push(thisDiff);
                lengthInsertions1 = lengthInsertions2;
                lengthDeletions1 = lengthDeletions2;
                lengthInsertions2 = 0;
                lengthDeletions2 = 0;
                lastEquality = thisDiff.text;
            }else{
                // An insertion or deletion.
                if(thisDiff.operation == Operation.INSERT){
                    lengthInsertions2 += thisDiff.text.length();
                }else{
                    lengthDeletions2 += thisDiff.text.length();
                }
                // Eliminate an equality that is smaller or equal to the edits on both
                // sides of it.
                if(lastEquality != null && lastEquality.length() <= Math.max(lengthInsertions1, lengthDeletions1)
                        && lastEquality.length() <= Math.max(lengthInsertions2, lengthDeletions2)){
                    //System.out.println("Splitting: '" + lastEquality + "'");
                    // Walk back to offending equality.
                    while(thisDiff != equalities.peek()){
                        thisDiff = pointer.previous();
                    }
                    pointer.next();

                    // Replace equality with a delete.
                    pointer.set(new Diff(Operation.DELETE, lastEquality));
                    // Insert a corresponding an insert.
                    pointer.add(new Diff(Operation.INSERT, lastEquality));

                    equalities.pop();  // Throw away the equality we just deleted.
                    if(!equalities.isEmpty()){
                        // Throw away the previous equality (it needs to be reevaluated).
                        equalities.pop();
                    }
                    if(equalities.isEmpty()){
                        // There are no previous equalities, walk back to the start.
                        while(pointer.hasPrevious()){
                            pointer.previous();
                        }
                    }else{
                        // There is a safe equality we can fall back to.
                        thisDiff = equalities.peek();
                        while(thisDiff != pointer.previous()){
                            // Intentionally empty loop.
                        }
                    }

                    lengthInsertions1 = 0;  // Reset the counters.
                    lengthInsertions2 = 0;
                    lengthDeletions1 = 0;
                    lengthDeletions2 = 0;
                    lastEquality = null;
                    changes = true;
                }
            }
            thisDiff = pointer.hasNext() ? pointer.next() : null;
        }

        // Normalize the diff.
        if(changes){
            diffCleanupAndMerge(diffs);
        }
        diffCleanupSemanticLossless(diffs);

        // Find any overlaps between deletions and insertions.
        // e.g: <del>abcxxx</del><ins>xxxdef</ins>
        //   -> <del>abc</del>xxx<ins>def</ins>
        // e.g: <del>xxxabc</del><ins>defxxx</ins>
        //   -> <ins>def</ins>xxx<del>abc</del>
        // Only extract an overlap if it is as big as the edit ahead or behind it.
        pointer = diffs.listIterator();
        Diff prevDiff = null;
        thisDiff = null;
        if(pointer.hasNext()){
            prevDiff = pointer.next();
            if(pointer.hasNext()){
                thisDiff = pointer.next();
            }
        }
        while(thisDiff != null){
            if(prevDiff.operation == Operation.DELETE && thisDiff.operation == Operation.INSERT){
                String deletion = prevDiff.text;
                String insertion = thisDiff.text;
                int overlapLength1 = diffCommonOverlap(deletion, insertion);
                int overlapLength2 = diffCommonOverlap(insertion, deletion);
                if(overlapLength1 >= overlapLength2){
                    if(overlapLength1 >= deletion.length() / 2.0 || overlapLength1 >= insertion.length() / 2.0){
                        // Overlap found. Insert an equality and trim the surrounding edits.
                        pointer.previous();
                        pointer.add(new Diff(Operation.EQUAL, insertion.substring(0, overlapLength1)));
                        prevDiff.text = deletion.substring(0, deletion.length() - overlapLength1);
                        thisDiff.text = insertion.substring(overlapLength1);
                        // pointer.add inserts the element before the cursor, so there is
                        // no need to step past the new element.
                    }
                }else{
                    if(overlapLength2 >= deletion.length() / 2.0 || overlapLength2 >= insertion.length() / 2.0){
                        // Reverse overlap found.
                        // Insert an equality and swap and trim the surrounding edits.
                        pointer.previous();
                        pointer.add(new Diff(Operation.EQUAL, deletion.substring(0, overlapLength2)));
                        prevDiff.operation = Operation.INSERT;
                        prevDiff.text = insertion.substring(0, insertion.length() - overlapLength2);
                        thisDiff.operation = Operation.DELETE;
                        thisDiff.text = deletion.substring(overlapLength2);
                        // pointer.add inserts the element before the cursor, so there is
                        // no need to step past the new element.
                    }
                }
                thisDiff = pointer.hasNext() ? pointer.next() : null;
            }
            prevDiff = thisDiff;
            thisDiff = pointer.hasNext() ? pointer.next() : null;
        }
    }

    /**
     * Look for single edits surrounded on both sides by equalities
     * which can be shifted sideways to align the edit to a word boundary.
     * e.g: The c<ins>at c</ins>ame. -> The <ins>cat </ins>came.
     *
     * @param diffs LinkedList of Diff objects.
     */
    public void diffCleanupSemanticLossless(LinkedList<Diff> diffs){
        String equality1, edit, equality2;
        String commonString;
        int commonOffset;
        int score, bestScore;
        String bestEquality1, bestEdit, bestEquality2;
        // Create a new iterator at the start.
        ListIterator<Diff> pointer = diffs.listIterator();
        Diff prevDiff = pointer.hasNext() ? pointer.next() : null;
        Diff thisDiff = pointer.hasNext() ? pointer.next() : null;
        Diff nextDiff = pointer.hasNext() ? pointer.next() : null;
        // Intentionally ignore the first and last element (don't need checking).
        while(nextDiff != null){
            if(prevDiff.operation == Operation.EQUAL &&
                    nextDiff.operation == Operation.EQUAL){
                // This is a single edit surrounded by equalities.
                equality1 = prevDiff.text;
                edit = thisDiff.text;
                equality2 = nextDiff.text;

                // First, shift the edit as far left as possible.
                commonOffset = diffCommonSuffix(equality1, edit);
                if(commonOffset != 0){
                    commonString = edit.substring(edit.length() - commonOffset);
                    equality1 = equality1.substring(0, equality1.length() - commonOffset);
                    edit = commonString + edit.substring(0, edit.length() - commonOffset);
                    equality2 = commonString + equality2;
                }

                // Second, step character by character right, looking for the best fit.
                bestEquality1 = equality1;
                bestEdit = edit;
                bestEquality2 = equality2;
                bestScore = getDiffCleanupSemanticScore(equality1, edit)
                        + getDiffCleanupSemanticScore(edit, equality2);
                while(edit.length() != 0 && equality2.length() != 0
                        && edit.charAt(0) == equality2.charAt(0)){
                    equality1 += edit.charAt(0);
                    edit = edit.substring(1) + equality2.charAt(0);
                    equality2 = equality2.substring(1);
                    score = getDiffCleanupSemanticScore(equality1, edit)
                            + getDiffCleanupSemanticScore(edit, equality2);
                    // The >= encourages trailing rather than leading whitespace on edits.
                    if(score >= bestScore){
                        bestScore = score;
                        bestEquality1 = equality1;
                        bestEdit = edit;
                        bestEquality2 = equality2;
                    }
                }

                if(!prevDiff.text.equals(bestEquality1)){
                    // We have an improvement, save it back to the diff.
                    if(bestEquality1.length() != 0){
                        prevDiff.text = bestEquality1;
                    }else{
                        pointer.previous(); // Walk past nextDiff.
                        pointer.previous(); // Walk past thisDiff.
                        pointer.previous(); // Walk past prevDiff.
                        pointer.remove(); // Delete prevDiff.
                        pointer.next(); // Walk past thisDiff.
                        pointer.next(); // Walk past nextDiff.
                    }
                    thisDiff.text = bestEdit;
                    if(bestEquality2.length() != 0){
                        nextDiff.text = bestEquality2;
                    }else{
                        pointer.remove(); // Delete nextDiff.
                        nextDiff = thisDiff;
                        thisDiff = prevDiff;
                    }
                }
            }
            prevDiff = thisDiff;
            thisDiff = nextDiff;
            nextDiff = pointer.hasNext() ? pointer.next() : null;
        }
    }

    /**
     * Given two strings, compute a score representing whether the internal
     * boundary falls on logical boundaries.
     * Scores range from 6 (best) to 0 (worst).
     *
     * @param one First string.
     * @param two Second string.
     * @return The score.
     */
    private int getDiffCleanupSemanticScore(String one, String two){
        if(one.length() == 0 || two.length() == 0){
            // Edges are the best.
            return 6;
        }

        // Each port of this function behaves slightly differently due to
        // subtle differences in each language's definition of things like
        // 'whitespace'.  Since this function's purpose is largely cosmetic,
        // the choice has been made to use each language's native features
        // rather than force total conformity.
        char char1 = one.charAt(one.length() - 1);
        char char2 = two.charAt(0);
        boolean nonAlphaNumeric1 = !Character.isLetterOrDigit(char1);
        boolean nonAlphaNumeric2 = !Character.isLetterOrDigit(char2);
        boolean whitespace1 = nonAlphaNumeric1 && Character.isWhitespace(char1);
        boolean whitespace2 = nonAlphaNumeric2 && Character.isWhitespace(char2);
        boolean lineBreak1 = whitespace1 && Character.getType(char1) == Character.CONTROL;
        boolean lineBreak2 = whitespace2 && Character.getType(char2) == Character.CONTROL;
        boolean blankLine1 = lineBreak1 && BLANK_LINE_END.matcher(one).find();
        boolean blankLine2 = lineBreak2 && BLANK_LINE_START.matcher(two).find();

        if(blankLine1 || blankLine2){
            // Five points for blank lines.
            return 5;
        }else if(lineBreak1 || lineBreak2){
            // Four points for line breaks.
            return 4;
        }else if(nonAlphaNumeric1 && !whitespace1 && whitespace2){
            // Three points for end of sentences.
            return 3;
        }else if(whitespace1 || whitespace2){
            // Two points for whitespace.
            return 2;
        }else if(nonAlphaNumeric1 || nonAlphaNumeric2){
            // One point for non-alphanumeric.
            return 1;
        }
        return 0;
    }

    // Define some regex patterns for matching boundaries.
    private final Pattern BLANK_LINE_END = Pattern.compile("\\n\\r?\\n\\Z", Pattern.DOTALL);
    private final Pattern BLANK_LINE_START = Pattern.compile("\\A\\r?\\n\\r?\\n", Pattern.DOTALL);

    /**
     * Reduce the number of edits by eliminating operationally trivial equalities.
     * @param diffs LinkedList of Diff objects.
     */
    public void cleanupDiffEfficiency(LinkedList<Diff> diffs){
        if(diffs.isEmpty()){
            return;
        }
        boolean changes = false;
        Deque<Diff> equalities = new ArrayDeque<>();  // Double-ended queue of equalities.
        String lastEquality = null; // Always equal to equalities.peek().text
        ListIterator<Diff> pointer = diffs.listIterator();
        // Is there an insertion operation before the last equality.
        boolean preIns = false;
        // Is there a deletion operation before the last equality.
        boolean preDel = false;
        // Is there an insertion operation after the last equality.
        boolean postIns = false;
        // Is there a deletion operation after the last equality.
        boolean postDel = false;
        Diff thisDiff = pointer.next();
        Diff safeDiff = thisDiff;  // The last Diff that is known to be unsplittable.
        while(thisDiff != null){
            if(thisDiff.operation == Operation.EQUAL){
                // Equality found.
                if(thisDiff.text.length() < diffEditCost && (postIns || postDel)){
                    // Candidate found.
                    equalities.push(thisDiff);
                    preIns = postIns;
                    preDel = postDel;
                    lastEquality = thisDiff.text;
                }else{
                    // Not a candidate, and can never become one.
                    equalities.clear();
                    lastEquality = null;
                    safeDiff = thisDiff;
                }
                postIns = postDel = false;
            }else{
                // An insertion or deletion.
                if(thisDiff.operation == Operation.DELETE){
                    postDel = true;
                }else{
                    postIns = true;
                }
                /*
                 * Five types to be split:
                 * <ins>A</ins><del>B</del>XY<ins>C</ins><del>D</del>
                 * <ins>A</ins>X<ins>C</ins><del>D</del>
                 * <ins>A</ins><del>B</del>X<ins>C</ins>
                 * <ins>A</del>X<ins>C</ins><del>D</del>
                 * <ins>A</ins><del>B</del>X<del>C</del>
                 */
                if(lastEquality != null &&
                        (preIns && preDel && postIns && postDel || lastEquality.length() < diffEditCost / 2
                        && (preIns ? 1 : 0) + (preDel ? 1 : 0) + (postIns ? 1 : 0) + (postDel ? 1 : 0) == 3)){
                    //System.out.println("Splitting: '" + lastEquality + "'");
                    // Walk back to offending equality.
                    while(thisDiff != equalities.peek()){
                        thisDiff = pointer.previous();
                    }
                    pointer.next();

                    // Replace equality with a delete.
                    pointer.set(new Diff(Operation.DELETE, lastEquality));
                    // Insert a corresponding an insert.
                    pointer.add(thisDiff = new Diff(Operation.INSERT, lastEquality));

                    equalities.pop();  // Throw away the equality we just deleted.
                    lastEquality = null;
                    if(preIns && preDel){
                        // No changes made which could affect previous entry, keep going.
                        postIns = postDel = true;
                        equalities.clear();
                        safeDiff = thisDiff;
                    }else{
                        if(!equalities.isEmpty()){
                            // Throw away the previous equality (it needs to be reevaluated).
                            equalities.pop();
                        }
                        if(equalities.isEmpty()){
                            // There are no previous questionable equalities,
                            // walk back to the last known safe diff.
                            thisDiff = safeDiff;
                        }else{
                            // There is an equality we can fall back to.
                            thisDiff = equalities.peek();
                        }
                        while(thisDiff != pointer.previous()){
                            // Intentionally empty loop.
                        }
                        postIns = postDel = false;
                    }

                    changes = true;
                }
            }
            thisDiff = pointer.hasNext() ? pointer.next() : null;
        }

        if(changes){
            diffCleanupAndMerge(diffs);
        }
    }

    /**
     * Reorder and merge like edit sections.  Merge equalities.
     * Any edit section can move as long as it doesn't cross an equality.
     * @param diffs LinkedList of Diff objects.
     */
    public void diffCleanupAndMerge(LinkedList<Diff> diffs){
        diffs.add(new Diff(Operation.EQUAL, ""));  // Add a dummy entry at the end.
        ListIterator<Diff> pointer = diffs.listIterator();
        int countDelete = 0;
        int countInsert = 0;
        String textDelete = "";
        String textInsert = "";
        Diff thisDiff = pointer.next();
        Diff prevEqual = null;
        int commonlength;
        while(thisDiff != null){
            switch(thisDiff.operation){
                case INSERT:
                    countInsert++;
                    textInsert += thisDiff.text;
                    prevEqual = null;
                    break;
                case DELETE:
                    countDelete++;
                    textDelete += thisDiff.text;
                    prevEqual = null;
                    break;
                case EQUAL:
                    if(countDelete + countInsert > 1){
                        boolean both_types = countDelete != 0 && countInsert != 0;
                        // Delete the offending records.
                        pointer.previous();  // Reverse direction.
                        while(countDelete-- > 0){
                            pointer.previous();
                            pointer.remove();
                        }
                        while(countInsert-- > 0){
                            pointer.previous();
                            pointer.remove();
                        }
                        if(both_types){
                            // Factor out any common prefixies.
                            commonlength = diffCommonPrefix(textInsert, textDelete);
                            if(commonlength != 0){
                                if(pointer.hasPrevious()){
                                    thisDiff = pointer.previous();
                                    Preconditions.requireState(thisDiff.operation == Operation.EQUAL,
                                            "Previous diff should have been an equality.");
                                    thisDiff.text += textInsert.substring(0, commonlength);
                                    pointer.next();
                                }else{
                                    pointer.add(new Diff(Operation.EQUAL,
                                            textInsert.substring(0, commonlength)));
                                }
                                textInsert = textInsert.substring(commonlength);
                                textDelete = textDelete.substring(commonlength);
                            }
                            // Factor out any common suffixies.
                            commonlength = diffCommonSuffix(textInsert, textDelete);
                            if(commonlength != 0){
                                thisDiff = pointer.next();
                                thisDiff.text = textInsert.substring(textInsert.length()
                                        - commonlength) + thisDiff.text;
                                textInsert = textInsert.substring(0, textInsert.length()
                                        - commonlength);
                                textDelete = textDelete.substring(0, textDelete.length()
                                        - commonlength);
                                pointer.previous();
                            }
                        }
                        // Insert the merged records.
                        if(textDelete.length() != 0){
                            pointer.add(new Diff(Operation.DELETE, textDelete));
                        }
                        if(textInsert.length() != 0){
                            pointer.add(new Diff(Operation.INSERT, textInsert));
                        }
                        // Step forward to the equality.
                        thisDiff = pointer.hasNext() ? pointer.next() : null;
                    }else if(prevEqual != null){
                        // Merge this equality with the previous one.
                        prevEqual.text += thisDiff.text;
                        pointer.remove();
                        thisDiff = pointer.previous();
                        pointer.next();  // Forward direction
                    }
                    countInsert = 0;
                    countDelete = 0;
                    textDelete = "";
                    textInsert = "";
                    prevEqual = thisDiff;
                    break;
            }
            thisDiff = pointer.hasNext() ? pointer.next() : null;
        }
        if(diffs.getLast().text.length() == 0){
            diffs.removeLast();  // Remove the dummy entry at the end.
        }

        /*
         * Second pass: look for single edits surrounded on both sides by equalities
         * which can be shifted sideways to eliminate an equality.
         * e.g: A<ins>BA</ins>C -> <ins>AB</ins>AC
         */
        boolean changes = false;
        // Create a new iterator at the start.
        // (As opposed to walking the current one back.)
        pointer = diffs.listIterator();
        Diff prevDiff = pointer.hasNext() ? pointer.next() : null;
        thisDiff = pointer.hasNext() ? pointer.next() : null;
        Diff nextDiff = pointer.hasNext() ? pointer.next() : null;
        // Intentionally ignore the first and last element (don't need checking).
        while(nextDiff != null){
            if(prevDiff.operation == Operation.EQUAL && nextDiff.operation == Operation.EQUAL){
                // This is a single edit surrounded by equalities.
                if(thisDiff.text.endsWith(prevDiff.text)){
                    // Shift the edit over the previous equality.
                    thisDiff.text = prevDiff.text
                            + thisDiff.text.substring(0, thisDiff.text.length()
                            - prevDiff.text.length());
                    nextDiff.text = prevDiff.text + nextDiff.text;
                    pointer.previous(); // Walk past nextDiff.
                    pointer.previous(); // Walk past thisDiff.
                    pointer.previous(); // Walk past prevDiff.
                    pointer.remove(); // Delete prevDiff.
                    pointer.next(); // Walk past thisDiff.
                    thisDiff = pointer.next(); // Walk past nextDiff.
                    nextDiff = pointer.hasNext() ? pointer.next() : null;
                    changes = true;
                }else if(thisDiff.text.startsWith(nextDiff.text)){
                    // Shift the edit over the next equality.
                    prevDiff.text += nextDiff.text;
                    thisDiff.text = thisDiff.text.substring(nextDiff.text.length())
                            + nextDiff.text;
                    pointer.remove(); // Delete nextDiff.
                    nextDiff = pointer.hasNext() ? pointer.next() : null;
                    changes = true;
                }
            }
            prevDiff = thisDiff;
            thisDiff = nextDiff;
            nextDiff = pointer.hasNext() ? pointer.next() : null;
        }
        // If shifts were made, the diff needs reordering and another shift sweep.
        if(changes){
            diffCleanupAndMerge(diffs);
        }
    }

    /**
     * loc is a location in text1, compute and return the equivalent location in
     * text2.
     * e.g. "The cat" vs "The big cat", 1->1, 5->8
     *
     * @param diffs List of Diff objects.
     * @param loc Location within text1.
     * @return Location within text2.
     */
    public int getXIndex(List<Diff> diffs, int loc){
        int chars1 = 0;
        int chars2 = 0;
        int lastChars1 = 0;
        int lastChars2 = 0;
        Diff lastDiff = null;
        for(Diff diff : diffs){
            if(diff.operation != Operation.INSERT){
                // Equality or deletion.
                chars1 += diff.text.length();
            }
            if(diff.operation != Operation.DELETE){
                // Equality or insertion.
                chars2 += diff.text.length();
            }
            if(chars1 > loc){
                // Overshot the location.
                lastDiff = diff;
                break;
            }
            lastChars1 = chars1;
            lastChars2 = chars2;
        }
        if(lastDiff != null && lastDiff.operation == Operation.DELETE){
            // The location was deleted.
            return lastChars2;
        }
        // Add the remaining character length.
        return lastChars2 + loc - lastChars1;
    }

    /**
     * Compute and return the source text (all equalities and deletions).
     *
     * @param diffs List of Diff objects.
     * @return Source text.
     */
    public String getText1FromDiff(List<Diff> diffs){
        StringBuilder text = new StringBuilder();
        for(Diff diff : diffs){
            if(diff.operation != Operation.INSERT){
                text.append(diff.text);
            }
        }
        return text.toString();
    }

    /**
     * Compute and return the destination text (all equalities and insertions).
     *
     * @param diffs List of Diff objects.
     * @return Destination text.
     */
    public String getText2FromDiff(List<Diff> diffs){
        StringBuilder text = new StringBuilder();
        for(Diff diff : diffs){
            if(diff.operation != Operation.DELETE){
                text.append(diff.text);
            }
        }
        return text.toString();
    }

    /**
     * Compute the Levenshtein distance; the number of inserted, deleted or
     * substituted characters.
     *
     * @param diffs List of Diff objects.
     * @return Number of changes.
     */
    public int getDiffLevenshtein(List<Diff> diffs){
        int levenshtein = 0;
        int insertions = 0;
        int deletions = 0;
        for(Diff diff : diffs){
            switch(diff.operation){
                case INSERT:
                    insertions += diff.text.length();
                    break;
                case DELETE:
                    deletions += diff.text.length();
                    break;
                case EQUAL:
                    // A deletion and an insertion is one substitution.
                    levenshtein += Math.max(insertions, deletions);
                    insertions = 0;
                    deletions = 0;
                    break;
            }
        }
        levenshtein += Math.max(insertions, deletions);
        return levenshtein;
    }

    /**
     * Crush the diff into an encoded string which describes the operations
     * required to transform text1 into text2.
     * E.g. =3\t-2\t+ing  -> Keep 3 chars, delete 2 chars, insert 'ing'.
     * Operations are tab-separated.  Inserted text is escaped using %xx notation.
     *
     * @param diffs List of Diff objects.
     * @return Delta text.
     */
    public String getDeltaFromDiff(List<Diff> diffs){
        StringBuilder text = new StringBuilder();
        for(Diff aDiff : diffs){
            switch(aDiff.operation){
                case INSERT:
                    text.append("+").append(aDiff.text.replace('+', ' ')).append("\t");
                    break;
                case DELETE:
                    text.append("-").append(aDiff.text.length()).append("\t");
                    break;
                case EQUAL:
                    text.append("=").append(aDiff.text.length()).append("\t");
                    break;
            }
        }
        String delta = text.toString();
        if(delta.length() != 0){
            // Strip off trailing tab character.
            delta = delta.substring(0, delta.length() - 1);;
        }
        return delta;
    }

    /**
     * Given the original text1, and an encoded string which describes the
     * operations required to transform text1 into text2, compute the full diff.
     *
     * @param text1 Source string for the diff.
     * @param delta Delta text.
     * @return Array of Diff objects or null if invalid.
     * @throws IllegalArgumentException If invalid input.
     */
    public LinkedList<Diff> getDiffFromDelta(String text1, String delta){
        LinkedList<Diff> diffs = new LinkedList<>();
        int pointer = 0;  // Cursor in text1
        String[] tokens = delta.split("\t");
        for(String token : tokens){
            if(token.length() == 0){
                // Blank tokens are ok (from a trailing \t).
                continue;
            }
            // Each token begins with a one character parameter which specifies the
            // operation of this token (delete, insert, equality).
            String param = token.substring(1);
            switch(token.charAt(0)){
                case '+':
                    // decode would change all "+" to " "
                    param = param.replace("+", "%2B");
                    try{
                        param = URLDecoder.decode(param, StandardCharsets.UTF_8);
                    }catch(IllegalArgumentException e){
                        // Malformed URI sequence.
                        throw new IllegalArgumentException(
                                "Illegal escape in diff_fromDelta: " + param, e);
                    }
                    diffs.add(new Diff(Operation.INSERT, param));
                    break;
                case '-':
                    // Fall through.
                case '=':
                    int n;
                    try{
                        n = Integer.parseInt(param);
                    }catch(NumberFormatException e){
                        throw new IllegalArgumentException(
                                "Invalid number in diff_fromDelta: " + param, e);
                    }
                    Preconditions.requireState(n > 0, "Negative number in diff_fromDelta: " + param);
                    String text;
                    try{
                        text = text1.substring(pointer, pointer += n);
                    }catch(StringIndexOutOfBoundsException e){
                        throw new IllegalArgumentException("Delta length (" + pointer
                                + ") larger than source text length (" + text1.length()
                                + ").", e);
                    }
                    if(token.charAt(0) == '='){
                        diffs.add(new Diff(Operation.EQUAL, text));
                    }else{
                        diffs.add(new Diff(Operation.DELETE, text));
                    }
                    break;
                default:
                    // Anything else is an error.
                    throw new IllegalArgumentException(
                            "Invalid diff operation in diff_fromDelta: " + token.charAt(0));
            }
        }
        Preconditions.requireState(pointer == text1.length(), "Delta length (" + pointer
                + ") smaller than source text length (" + text1.length() + ").");
        return diffs;
    }

    /**
     * Locate the best instance of 'pattern' in 'text' near 'loc'.
     * Returns -1 if no match found.
     *
     * @param text The text to search.
     * @param pattern The pattern to search for.
     * @param loc The location to search around.
     * @return Best match index or -1.
     */
    public int findMatch(String text, String pattern, int loc){
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");

        loc = Math.max(0, Math.min(loc, text.length()));
        if(text.equals(pattern)){
            // Shortcut (potentially not guaranteed by the algorithm)
            return 0;
        }else if(text.length() == 0){
            // Nothing to match.
            return -1;
        }else if(loc + pattern.length() <= text.length() && text.startsWith(pattern, loc)){
            // Perfect match at the perfect spot!  (Includes case of null pattern)
            return loc;
        }else{
            // Do a fuzzy compare.
            return matchBitap(text, pattern, loc);
        }
    }

    /**
     * Locate the best instance of 'pattern' in 'text' near 'loc' using the
     * Bitap algorithm.  Returns -1 if no match found.
     *
     * @param text The text to search.
     * @param pattern The pattern to search for.
     * @param loc The location to search around.
     * @return Best match index or -1.
     */
    protected int matchBitap(String text, String pattern, int loc){
        Preconditions.requireState(pattern.length() <= matchMaxBits, "Pattern too long for this application.");

        // Initialise the alphabet.
        Map<Character, Integer> alphabet = matchAlphabet(pattern);

        // Highest score beyond which we give up.
        double score_threshold = matchThreshold;
        // Is there a nearby exact match? (speedup)
        int bestLoc = text.indexOf(pattern, loc);
        if(bestLoc != -1){
            score_threshold = Math.min(matchBitapScore(0, bestLoc, loc, pattern),
                    score_threshold);
            // What about in the other direction? (speedup)
            bestLoc = text.lastIndexOf(pattern, loc + pattern.length());
            if(bestLoc != -1){
                score_threshold = Math.min(matchBitapScore(0, bestLoc, loc, pattern),
                        score_threshold);
            }
        }

        // Initialise the bit arrays.
        int matchMask = 1 << pattern.length() - 1;
        bestLoc = -1;

        int binMin, binMid;
        int binMax = pattern.length() + text.length();
        // Empty initialization added to appease Java compiler.
        int[] lastRd = new int[0];
        for(int d = 0; d < pattern.length(); d++){
            // Scan for the best match; each iteration allows for one more error.
            // Run a binary search to determine how far from 'loc' we can stray at
            // this error level.
            binMin = 0;
            binMid = binMax;
            while(binMin < binMid){
                if(matchBitapScore(d, loc + binMid, loc, pattern) <= score_threshold){
                    binMin = binMid;
                }else{
                    binMax = binMid;
                }
                binMid = (binMax - binMin) / 2 + binMin;
            }
            // Use the result from this iteration as the maximum for the next.
            binMax = binMid;
            int start = Math.max(1, loc - binMid + 1);
            int finish = Math.min(loc + binMid, text.length()) + pattern.length();

            int[] rd = new int[finish + 2];
            rd[finish + 1] = (1 << d) - 1;
            for(int j = finish; j >= start; j--){
                int charMatch;
                if(text.length() <= j - 1 || !alphabet.containsKey(text.charAt(j - 1))){
                    // Out of range.
                    charMatch = 0;
                }else{
                    charMatch = alphabet.get(text.charAt(j - 1));
                }
                if(d == 0){
                    // First pass: exact match.
                    rd[j] = (rd[j + 1] << 1 | 1) & charMatch;
                }else{
                    // Subsequent passes: fuzzy match.
                    rd[j] = (rd[j + 1] << 1 | 1) & charMatch
                            | (lastRd[j + 1] | lastRd[j]) << 1 | 1 | lastRd[j + 1];
                }
                if((rd[j] & matchMask) != 0){
                    double score = matchBitapScore(d, j - 1, loc, pattern);
                    // This match will almost certainly be better than any existing
                    // match.  But check anyway.
                    if(score <= score_threshold){
                        // Told you so.
                        score_threshold = score;
                        bestLoc = j - 1;
                        if(bestLoc > loc){
                            // When passing loc, don't exceed our current distance from loc.
                            start = Math.max(1, 2 * loc - bestLoc);
                        }else{
                            // Already passed loc, downhill from here on in.
                            break;
                        }
                    }
                }
            }
            if(matchBitapScore(d + 1, loc, loc, pattern) > score_threshold){
                // No hope for a (better) match at greater error levels.
                break;
            }
            lastRd = rd;
        }
        return bestLoc;
    }

    /**
     * Compute and return the score for a match with e errors and x location.
     *
     * @param e Number of errors in match.
     * @param x Location of match.
     * @param loc Expected location of match.
     * @param pattern Pattern being sought.
     * @return Overall score for match (0.0 = good, 1.0 = bad).
     */
    private double matchBitapScore(int e, int x, int loc, String pattern){
        float accuracy = (float)e / pattern.length();
        int proximity = Math.abs(loc - x);
        if(matchDistance == 0){
            // Dodge divide by zero error.
            return proximity == 0 ? accuracy : 1.0;
        }
        return accuracy + proximity / (float)matchDistance;
    }

    /**
     * Initialise the alphabet for the Bitap algorithm.
     *
     * @param pattern The text to encode.
     * @return Hash of character locations.
     */
    protected Map<Character, Integer> matchAlphabet(String pattern){
        Map<Character, Integer> s = new HashMap<>();
        char[] charPattern = pattern.toCharArray();
        for(char c : charPattern){
            s.put(c, 0);
        }
        int i = 0;
        for(char c : charPattern){
            s.put(c, s.get(c) | 1 << pattern.length() - i - 1);
            i++;
        }
        return s;
    }

    /**
     * Increase the context until it is unique,
     * but don't let the pattern expand beyond Match_MaxBits.
     *
     * @param patch The patch to grow.
     * @param text Source text.
     */
    protected void patchAddContext(Patch patch, String text){
        if(text.length() == 0){
            return;
        }
        String pattern = text.substring(patch.start2, patch.start2 + patch.length1);
        int padding = 0;

        // Look for the first and last matches of pattern in text.  If two different
        // matches are found, increase the pattern length.
        while(text.indexOf(pattern) != text.lastIndexOf(pattern)
                && pattern.length() < matchMaxBits - patchMargin - patchMargin){
            padding += patchMargin;
            pattern = text.substring(Math.max(0, patch.start2 - padding),
                    Math.min(text.length(), patch.start2 + patch.length1 + padding));
        }
        // Add one chunk for good luck.
        padding += patchMargin;

        // Add the prefix.
        String prefix = text.substring(Math.max(0, patch.start2 - padding), patch.start2);
        if(prefix.length() != 0){
            patch.diffs.addFirst(new Diff(Operation.EQUAL, prefix));
        }
        // Add the suffix.
        String suffix = text.substring(patch.start2 + patch.length1,
                Math.min(text.length(), patch.start2 + patch.length1 + padding));
        if(suffix.length() != 0){
            patch.diffs.addLast(new Diff(Operation.EQUAL, suffix));
        }

        // Roll back the start points.
        patch.start1 -= prefix.length();
        patch.start2 -= prefix.length();
        // Extend the lengths.
        patch.length1 += prefix.length() + suffix.length();
        patch.length2 += prefix.length() + suffix.length();
    }

    /**
     * Compute a list of patches to turn text1 into text2.
     * A set of diffs will be computed.
     *
     * @param text1 Old text.
     * @param text2 New text.
     * @return LinkedList of Patch objects.
     */
    public LinkedList<Patch> getPatch(String text1, String text2){
        Objects.requireNonNull(text1, "text1");
        Objects.requireNonNull(text2, "text2");
        // No diffs provided, compute our own.
        LinkedList<Diff> diffs = getDifference(text1, text2, true);
        if(diffs.size() > 2){
            cleanupDiffSemantic(diffs);
            cleanupDiffEfficiency(diffs);
        }
        return getPatch(text1, diffs);
    }

    /**
     * Compute a list of patches to turn text1 into text2.
     * text1 will be derived from the provided diffs.
     *
     * @param diffs Array of Diff objects for text1 to text2.
     * @return LinkedList of Patch objects.
     */
    public LinkedList<Patch> getPatch(LinkedList<Diff> diffs){
        Objects.requireNonNull(diffs, "diffs");
        // No origin string provided, compute our own.
        String text1 = getText1FromDiff(diffs);
        return getPatch(text1, diffs);
    }

    /**
     * Compute a list of patches to turn text1 into text2.
     * text2 is not provided, diffs are the delta between text1 and text2.
     *
     * @param text1 Old text.
     * @param diffs Array of Diff objects for text1 to text2.
     * @return LinkedList of Patch objects.
     */
    public LinkedList<Patch> getPatch(String text1, LinkedList<Diff> diffs){
        Objects.requireNonNull(text1, "text1");
        Objects.requireNonNull(diffs, "diffs");

        LinkedList<Patch> patches = new LinkedList<>();
        if(diffs.isEmpty()){
            return patches;  // Get rid of the null case.
        }
        Patch patch = new Patch();
        int char_count1 = 0;  // Number of characters into the text1 string.
        int char_count2 = 0;  // Number of characters into the text2 string.
        // Start with text1 (prePatchText) and apply the diffs until we arrive at
        // text2 (postPatchText). We recreate the patches one by one to determine
        // context info.
        String prePatchText = text1;
        String postPatchText = text1;
        for(Diff aDiff : diffs){
            if(patch.diffs.isEmpty() && aDiff.operation != Operation.EQUAL){
                // A new patch starts here.
                patch.start1 = char_count1;
                patch.start2 = char_count2;
            }

            switch(aDiff.operation){
                case INSERT:
                    patch.diffs.add(aDiff);
                    patch.length2 += aDiff.text.length();
                    postPatchText = postPatchText.substring(0, char_count2)
                            + aDiff.text + postPatchText.substring(char_count2);
                    break;
                case DELETE:
                    patch.length1 += aDiff.text.length();
                    patch.diffs.add(aDiff);
                    postPatchText = postPatchText.substring(0, char_count2)
                            + postPatchText.substring(char_count2 + aDiff.text.length());
                    break;
                case EQUAL:
                    if(aDiff.text.length() <= 2 * patchMargin
                            && !patch.diffs.isEmpty() && aDiff != diffs.getLast()){
                        // Small equality inside a patch.
                        patch.diffs.add(aDiff);
                        patch.length1 += aDiff.text.length();
                        patch.length2 += aDiff.text.length();
                    }

                    if(aDiff.text.length() >= 2 * patchMargin && !patch.diffs.isEmpty()){
                        // Time for a new patch.
                        patchAddContext(patch, prePatchText);
                        patches.add(patch);
                        patch = new Patch();
                        // Unlike Unidiff, our patch lists have a rolling context.
                        // https://github.com/google/diff-match-patch/wiki/Unidiff
                        // Update prepatch text & pos to reflect the application of the
                        // just completed patch.
                        prePatchText = postPatchText;
                        char_count1 = char_count2;
                    }
                    break;
            }

            // Update the current character count.
            if(aDiff.operation != Operation.INSERT){
                char_count1 += aDiff.text.length();
            }
            if(aDiff.operation != Operation.DELETE){
                char_count2 += aDiff.text.length();
            }
        }
        // Pick up the leftover patch if not empty.
        if(!patch.diffs.isEmpty()){
            patchAddContext(patch, prePatchText);
            patches.add(patch);
        }

        return patches;
    }

    /**
     * Given an array of patches, return another array that is identical.
     *
     * @param patches Array of Patch objects.
     * @return Array of Patch objects.
     */
    public LinkedList<Patch> patchDeepCopy(LinkedList<Patch> patches){
        LinkedList<Patch> patchesCopy = new LinkedList<>();
        for(Patch patch : patches){
            Patch patchCopy = new Patch();
            for(Diff diff : patch.diffs){
                Diff diffCopy = new Diff(diff.operation, diff.text);
                patchCopy.diffs.add(diffCopy);
            }
            patchCopy.start1 = patch.start1;
            patchCopy.start2 = patch.start2;
            patchCopy.length1 = patch.length1;
            patchCopy.length2 = patch.length2;
            patchesCopy.add(patchCopy);
        }
        return patchesCopy;
    }

    /**
     * Merge a set of patches onto the text.  Return a patched text, as well
     * as an array of true/false values indicating which patches were applied.
     *
     * @param patches Array of Patch objects
     * @param text Old text.
     * @return Two element Object array, containing the new text and an array of
     * boolean values.
     */
    public Object[] patchApply(LinkedList<Patch> patches, String text){
        if(patches.isEmpty()){
            return new Object[]{text, new boolean[0]};
        }

        // Deep copy the patches so that no changes are made to originals.
        patches = patchDeepCopy(patches);

        String nullPadding = addPadding(patches);
        text = nullPadding + text + nullPadding;
        patchSplitMax(patches);

        int x = 0;
        // delta keeps track of the offset between the expected and actual location
        // of the previous patch.  If there are patches expected at positions 10 and
        // 20, but the first patch was found at 12, delta is 2 and the second patch
        // has an effective expected position of 22.
        int delta = 0;
        boolean[] results = new boolean[patches.size()];
        for(Patch patch : patches){
            int expectedLoc = patch.start2 + delta;
            String text1 = getText1FromDiff(patch.diffs);
            int startLoc;
            int endLoc = -1;
            if(text1.length() > matchMaxBits){
                // patch_splitMax will only provide an oversized pattern in the case of
                // a monster delete.
                startLoc = findMatch(text, text1.substring(0, matchMaxBits), expectedLoc);
                if(startLoc != -1){
                    endLoc = findMatch(text,
                            text1.substring(text1.length() - matchMaxBits),
                            expectedLoc + text1.length() - matchMaxBits);
                    if(endLoc == -1 || startLoc >= endLoc){
                        // Can't find valid trailing context.  Drop this patch.
                        startLoc = -1;
                    }
                }
            }else{
                startLoc = findMatch(text, text1, expectedLoc);
            }
            if(startLoc == -1){
                // No match found. :(
                results[x] = false;
                // Subtract the delta for this failed patch from subsequent patches.
                delta -= patch.length2 - patch.length1;
            }else{
                // Found a match. :)
                results[x] = true;
                delta = startLoc - expectedLoc;
                String text2;
                if(endLoc == -1){
                    text2 = text.substring(startLoc,
                            Math.min(startLoc + text1.length(), text.length()));
                }else{
                    text2 = text.substring(startLoc,
                            Math.min(endLoc + matchMaxBits, text.length()));
                }
                if(text1.equals(text2)){
                    // Perfect match, just shove the replacement text in.
                    text = text.substring(0, startLoc) + getText2FromDiff(patch.diffs)
                            + text.substring(startLoc + text1.length());
                }else{
                    // Imperfect match.  Run a diff to get a framework of equivalent
                    // indices.
                    LinkedList<Diff> diffs = getDifference(text1, text2, false);
                    if(text1.length() > matchMaxBits
                            && getDiffLevenshtein(diffs) / (float)text1.length()
                            > patchDeleteThreshold){
                        // The end points match, but the content is unacceptably bad.
                        results[x] = false;
                    }else{
                        diffCleanupSemanticLossless(diffs);
                        int index1 = 0;
                        for(Diff diff : patch.diffs){
                            if(diff.operation != Operation.EQUAL){
                                int index2 = getXIndex(diffs, index1);
                                if(diff.operation == Operation.INSERT){
                                    // Insertion
                                    text = text.substring(0, startLoc + index2) + diff.text
                                            + text.substring(startLoc + index2);
                                }else if(diff.operation == Operation.DELETE){
                                    // Deletion
                                    text = text.substring(0, startLoc + index2)
                                            + text.substring(startLoc + getXIndex(diffs,
                                            index1 + diff.text.length()));
                                }
                            }
                            if(diff.operation != Operation.DELETE){
                                index1 += diff.text.length();
                            }
                        }
                    }
                }
            }
            x++;
        }
        // Strip the padding off.
        text = text.substring(nullPadding.length(), text.length() - nullPadding.length());
        return new Object[]{text, results};
    }

    /**
     * Add some padding on text start and end so that edges can match something.
     * Intended to be called only from within patch_apply.
     *
     * @param patches Array of Patch objects.
     * @return The padding string added to each side.
     */
    public String addPadding(LinkedList<Patch> patches){
        short paddingLength = patchMargin;
        StringBuilder nullPadding = new StringBuilder();
        for(short x = 1; x <= paddingLength; x++){
            nullPadding.append((char)x);
        }

        // Bump all the patches forward.
        for(Patch patch : patches){
            patch.start1 += paddingLength;
            patch.start2 += paddingLength;
        }

        // Add some padding on start of first diff.
        Patch patch = patches.getFirst();
        LinkedList<Diff> diffs = patch.diffs;
        if(diffs.isEmpty() || diffs.getFirst().operation != Operation.EQUAL){
            // Add nullPadding equality.
            diffs.addFirst(new Diff(Operation.EQUAL, nullPadding.toString()));
            patch.start1 -= paddingLength;  // Should be 0.
            patch.start2 -= paddingLength;  // Should be 0.
            patch.length1 += paddingLength;
            patch.length2 += paddingLength;
        }else if(paddingLength > diffs.getFirst().text.length()){
            // Grow first equality.
            Diff firstDiff = diffs.getFirst();
            int extraLength = paddingLength - firstDiff.text.length();
            firstDiff.text = nullPadding.substring(firstDiff.text.length()) + firstDiff.text;
            patch.start1 -= extraLength;
            patch.start2 -= extraLength;
            patch.length1 += extraLength;
            patch.length2 += extraLength;
        }

        // Add some padding on end of last diff.
        patch = patches.getLast();
        diffs = patch.diffs;
        if(diffs.isEmpty() || diffs.getLast().operation != Operation.EQUAL){
            // Add nullPadding equality.
            diffs.addLast(new Diff(Operation.EQUAL, nullPadding.toString()));
            patch.length1 += paddingLength;
            patch.length2 += paddingLength;
        }else if(paddingLength > diffs.getLast().text.length()){
            // Grow last equality.
            Diff lastDiff = diffs.getLast();
            int extraLength = paddingLength - lastDiff.text.length();
            lastDiff.text += nullPadding.substring(0, extraLength);
            patch.length1 += extraLength;
            patch.length2 += extraLength;
        }

        return nullPadding.toString();
    }

    /**
     * Look through the patches and break up any which are longer than the
     * maximum limit of the match algorithm.
     * Intended to be called only from within patch_apply.
     *
     * @param patches LinkedList of Patch objects.
     */
    public void patchSplitMax(LinkedList<Patch> patches){
        short patchSize = matchMaxBits;
        String precontext, postcontext;
        Patch patch;
        int start1, start2;
        boolean empty;
        Operation diffType;
        String diffText;
        ListIterator<Patch> pointer = patches.listIterator();
        Patch bigpatch = pointer.hasNext() ? pointer.next() : null;
        while(bigpatch != null){
            if(bigpatch.length1 <= matchMaxBits){
                bigpatch = pointer.hasNext() ? pointer.next() : null;
                continue;
            }
            // Remove the big old patch.
            pointer.remove();
            start1 = bigpatch.start1;
            start2 = bigpatch.start2;
            precontext = "";
            while(!bigpatch.diffs.isEmpty()){
                // Create one of several smaller patches.
                patch = new Patch();
                empty = true;
                patch.start1 = start1 - precontext.length();
                patch.start2 = start2 - precontext.length();
                if(precontext.length() != 0){
                    patch.length1 = patch.length2 = precontext.length();
                    patch.diffs.add(new Diff(Operation.EQUAL, precontext));
                }
                while(!bigpatch.diffs.isEmpty()
                        && patch.length1 < patchSize - patchMargin){
                    diffType = bigpatch.diffs.getFirst().operation;
                    diffText = bigpatch.diffs.getFirst().text;
                    if(diffType == Operation.INSERT){
                        // Insertions are harmless.
                        patch.length2 += diffText.length();
                        start2 += diffText.length();
                        patch.diffs.addLast(bigpatch.diffs.removeFirst());
                        empty = false;
                    }else if(diffType == Operation.DELETE && patch.diffs.size() == 1
                            && patch.diffs.getFirst().operation == Operation.EQUAL
                            && diffText.length() > 2 * patchSize){
                        // This is a large deletion.  Let it pass in one chunk.
                        patch.length1 += diffText.length();
                        start1 += diffText.length();
                        empty = false;
                        patch.diffs.add(new Diff(diffType, diffText));
                        bigpatch.diffs.removeFirst();
                    }else{
                        // Deletion or equality.  Only take as much as we can stomach.
                        diffText = diffText.substring(0, Math.min(diffText.length(),
                                patchSize - patch.length1 - patchMargin));
                        patch.length1 += diffText.length();
                        start1 += diffText.length();
                        if(diffType == Operation.EQUAL){
                            patch.length2 += diffText.length();
                            start2 += diffText.length();
                        }else{
                            empty = false;
                        }
                        patch.diffs.add(new Diff(diffType, diffText));
                        if(diffText.equals(bigpatch.diffs.getFirst().text)){
                            bigpatch.diffs.removeFirst();
                        }else{
                            bigpatch.diffs.getFirst().text = bigpatch.diffs.getFirst().text
                                    .substring(diffText.length());
                        }
                    }
                }
                // Compute the head context for the next patch.
                precontext = getText2FromDiff(patch.diffs);
                precontext = precontext.substring(Math.max(0, precontext.length()
                        - patchMargin));
                // Append the end context for this patch.
                if(getText1FromDiff(bigpatch.diffs).length() > patchMargin){
                    postcontext = getText1FromDiff(bigpatch.diffs).substring(0, patchMargin);
                }else{
                    postcontext = getText1FromDiff(bigpatch.diffs);
                }
                if(postcontext.length() != 0){
                    patch.length1 += postcontext.length();
                    patch.length2 += postcontext.length();
                    if(!patch.diffs.isEmpty()
                            && patch.diffs.getLast().operation == Operation.EQUAL){
                        patch.diffs.getLast().text += postcontext;
                    }else{
                        patch.diffs.add(new Diff(Operation.EQUAL, postcontext));
                    }
                }
                if(!empty){
                    pointer.add(patch);
                }
            }
            bigpatch = pointer.hasNext() ? pointer.next() : null;
        }
    }

    /**
     * Parse a textual representation of patches and return a List of Patch
     * objects.
     *
     * @param textLine Text representation of patches.
     * @return List of Patch objects.
     * @throws IllegalArgumentException If invalid input.
     */
    public List<Patch> getPatchFromText(String textLine){
        List<Patch> patches = new LinkedList<>();
        if(textLine.length() == 0){
            return patches;
        }
        LinkedList<String> text = new LinkedList<>(Arrays.asList(textLine.split("\n")));
        Patch patch;
        Pattern patchHeader = Pattern.compile("^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@$");

        Matcher matcher;
        char sign;
        String line;
        while(!text.isEmpty()){
            matcher = patchHeader.matcher(text.getFirst());
            Preconditions.requireState(matcher.matches(), "Invalid patch string: " + text.getFirst());
            patch = new Patch();
            patches.add(patch);
            patch.start1 = Integer.parseInt(matcher.group(1));
            if(matcher.group(2).length() == 0){
                patch.start1--;
                patch.length1 = 1;
            }else if(matcher.group(2).equals("0")){
                patch.length1 = 0;
            }else{
                patch.start1--;
                patch.length1 = Integer.parseInt(matcher.group(2));
            }

            patch.start2 = Integer.parseInt(matcher.group(3));
            if(matcher.group(4).length() == 0){
                patch.start2--;
                patch.length2 = 1;
            }else if(matcher.group(4).equals("0")){
                patch.length2 = 0;
            }else{
                patch.start2--;
                patch.length2 = Integer.parseInt(matcher.group(4));
            }
            text.removeFirst();

            while(!text.isEmpty()){
                try{
                    sign = text.getFirst().charAt(0);
                }catch(IndexOutOfBoundsException e){
                    // Blank line?  Whatever.
                    text.removeFirst();
                    continue;
                }
                line = text.getFirst().substring(1);
                line = line.replace("+", "%2B");  // decode would change all "+" to " "
                try{
                    line = URLDecoder.decode(line, StandardCharsets.UTF_8);
                }catch(IllegalArgumentException e){
                    // Malformed URI sequence.
                    throw new IllegalArgumentException(
                            "Illegal escape in patch_fromText: " + line, e);
                }
                if(sign == '-'){
                    // Deletion.
                    patch.diffs.add(new Diff(Operation.DELETE, line));
                }else if(sign == '+'){
                    // Insertion.
                    patch.diffs.add(new Diff(Operation.INSERT, line));
                }else if(sign == ' '){
                    // Minor equality.
                    patch.diffs.add(new Diff(Operation.EQUAL, line));
                }else if(sign == '@'){
                    // Start of next patch.
                    break;
                }else{
                    // WTF?
                    throw new IllegalArgumentException(
                            "Invalid patch mode '" + sign + "' in: " + line);
                }
                text.removeFirst();
            }
        }
        return patches;
    }

    /**
     * Class representing one diff operation.
     */
    public static class Diff{
        /**
         * One of: INSERT, DELETE or EQUAL.
         */
        public Operation operation;
        /**
         * The text associated with this diff operation.
         */
        public String text;

        /**
         * Constructor.  Initializes the diff with the provided values.
         *
         * @param operation One of INSERT, DELETE or EQUAL.
         * @param text The text being applied.
         */
        protected Diff(Operation operation, String text){
            this.operation = operation;
            this.text = text;
        }

        public Operation getOperation(){
            return operation;
        }

        public String getText(){
            return text;
        }

        @Override
        public String toString(){
            String prettyText = text.replace('\n', '\u00b6');
            return "Diff(" + operation + ",\"" + prettyText + "\")";
        }

        @Override
        public int hashCode(){
            return Objects.hash(operation, text);
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            Diff diff = (Diff)o;
            return operation == diff.operation && text.equals(diff.text);
        }
    }

    public static class Patch{
        public LinkedList<Diff> diffs = new LinkedList<>();
        public int start1;
        public int start2;
        public int length1;
        public int length2;

        @Override
        public String toString(){
            String coords1, coords2;
            if(length1 == 0){
                coords1 = start1 + ",0";
            }else if(length1 == 1){
                coords1 = Integer.toString(start1 + 1);
            }else{
                coords1 = (start1 + 1) + "," + length1;
            }
            if(length2 == 0){
                coords2 = start2 + ",0";
            }else if(length2 == 1){
                coords2 = Integer.toString(start2 + 1);
            }else{
                coords2 = (start2 + 1) + "," + length2;
            }
            StringBuilder text = new StringBuilder();
            text.append("@@ -").append(coords1).append(" +").append(coords2).append(" @@\n");
            // Escape the body of the patch with %xx notation.
            for(Diff aDiff : diffs){
                switch(aDiff.operation){
                    case INSERT:
                        text.append('+');
                        break;
                    case DELETE:
                        text.append('-');
                        break;
                    case EQUAL:
                        text.append(' ');
                        break;
                }
                text.append(aDiff.text).append("\n");
            }
            return text.toString();
        }
    }
}
