package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PatternAlgorithms {

    private PatternAlgorithms() {}

    public static Result search(String algorithm, String text, String pattern) {
        String normalized = algorithm == null ? "kmp" : algorithm.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("bm")) {
            return boyerMoore(text, pattern);
        }
        return kmp(text, pattern);
    }

    public static Result kmp(String text, String pattern) {
        String source = text == null ? "" : text;
        String target = pattern == null ? "" : pattern;
        if (target.isEmpty() || source.isEmpty() || target.length() > source.length()) {
            return new Result("KMP", Collections.emptyList(), 0, source.length(), target.length());
        }

        int[] prefix = buildPrefixTable(target);
        List<Integer> matches = new ArrayList<>();
        int comparisons = 0;

        int i = 0;
        int j = 0;
        while (i < source.length()) {
            comparisons++;
            if (source.charAt(i) == target.charAt(j)) {
                i++;
                j++;
                if (j == target.length()) {
                    matches.add(i - j);
                    j = prefix[j - 1];
                }
            } else if (j > 0) {
                j = prefix[j - 1];
            } else {
                i++;
            }
        }

        return new Result("KMP", matches, comparisons, source.length(), target.length());
    }

    public static Result boyerMoore(String text, String pattern) {
        String source = text == null ? "" : text;
        String target = pattern == null ? "" : pattern;
        if (target.isEmpty() || source.isEmpty() || target.length() > source.length()) {
            return new Result("BM", Collections.emptyList(), 0, source.length(), target.length());
        }

        int[] badChar = new int[Character.MAX_VALUE + 1];
        Arrays.fill(badChar, -1);
        for (int i = 0; i < target.length(); i++) {
            badChar[target.charAt(i)] = i;
        }

        List<Integer> matches = new ArrayList<>();
        int comparisons = 0;
        int shift = 0;

        while (shift <= source.length() - target.length()) {
            int j = target.length() - 1;
            while (j >= 0) {
                comparisons++;
                if (target.charAt(j) != source.charAt(shift + j)) {
                    break;
                }
                j--;
            }

            if (j < 0) {
                matches.add(shift);
                shift += (shift + target.length() < source.length())
                        ? target.length() - badChar[source.charAt(shift + target.length())]
                        : 1;
            } else {
                int badIndex = badChar[source.charAt(shift + j)];
                shift += Math.max(1, j - badIndex);
            }
        }

        return new Result("BM", matches, comparisons, source.length(), target.length());
    }

    private static int[] buildPrefixTable(String pattern) {
        int[] prefix = new int[pattern.length()];
        int len = 0;
        for (int i = 1; i < pattern.length(); ) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                prefix[i++] = ++len;
            } else if (len > 0) {
                len = prefix[len - 1];
            } else {
                prefix[i++] = 0;
            }
        }
        return prefix;
    }

    public static final class Result {
        public final String algorithm;
        public final List<Integer> matches;
        public final int comparisons;
        public final int textLength;
        public final int patternLength;

        public Result(String algorithm, List<Integer> matches, int comparisons, int textLength, int patternLength) {
            this.algorithm = algorithm;
            this.matches = Collections.unmodifiableList(new ArrayList<>(matches));
            this.comparisons = comparisons;
            this.textLength = textLength;
            this.patternLength = patternLength;
        }

        public boolean found() {
            return !matches.isEmpty();
        }
    }
}