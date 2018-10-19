package Search;

import Classes.Path;
import Classes.Query;
import Classes.Stemmer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ExtractQuery implements Iterator<Query> {

    private Pattern matchNumberRegex = Pattern.compile("[0-9]+");
    private BufferedReader reader;
    private HashSet<String> stopWords = new HashSet<>();

    /**
     * you should extract the 4 queries from the Path.TopicDir
     * the query content of each topic should be 1) tokenized, 2) to lowercase, 3) remove stop words, 4) stemming
     * you can simply pick up title only for query, or you can also use title + description + narrative for the query content.
     */
    public ExtractQuery() {
        try {
            this.reader = Files.newBufferedReader(Paths.get(Path.TopicDir));
            // Init stop words set
            try (Stream<String> lines = Files.lines(Paths.get(Path.StopwordDir))) {
                lines.forEach(s -> this.stopWords.add(s.trim().toLowerCase(Locale.US)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.reader = null;
        }
    }

    @Override
    public boolean hasNext() {
        if (this.reader == null) return false;
        boolean res;
        try {
            res = moveToTopStart();
        } catch (Exception e) {
            e.printStackTrace();
            res = false;
        }
        // File end, close reader
        if (!res) this.closeHandler();
        return res;
    }

    @Override
    public Query next() {
        Query q = new Query();
        if (this.reader == null) return q;
        q.SetTopicId(populateTopicId());
        String originalContent = processOneTopic();
        q.SetQueryContent(normalizeString(originalContent));
        return q;
    }

    /**
     * 1) tokenized, 2) to lowercase, 3) remove stop words, 4) stemming
     */
    private String normalizeString(String original) {
        String wekaDelimiters = "\r\n\t.,;:\"()?! ";
        StringTokenizer st = new StringTokenizer(original, wekaDelimiters);
        StringBuilder sb = new StringBuilder();
        while (st.hasMoreTokens()) {
            String lcnss = lcNoStopStem(st.nextToken());
            if (lcnss.isEmpty()) continue;
            sb.append(lcnss);
            sb.append(' ');
        }
        return sb.toString();
    }

    private String lcNoStopStem(String token) {
        token = token.toLowerCase(Locale.US);
        if (this.stopWords.contains(token)) return "";
        char[] tokenLc = token.toCharArray();
        Stemmer stemmer = new Stemmer();
        stemmer.add(tokenLc, tokenLc.length);
        stemmer.stem();
        return stemmer.toString();
    }

    /**
     * Found a topic, current pointer at end of <num>
     */
    private String populateTopicId() {
        try {
            String data = this.reader.readLine();
            Matcher m = this.matchNumberRegex.matcher(data);
            if (m.find()) {
                return m.group(0).trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Move reader pointer to start position
     *
     * @return True if found start, False if hit EOF
     */
    private boolean moveToTopStart() throws IOException {
        String data;
        while ((data = this.reader.readLine()) != null) {
            if (data.trim().startsWith("<top>")) return true;
        }
        return false;
    }

    /**
     * Line now at end of <num>, populate the whole query
     */
    private String processOneTopic() {
        StringBuilder sb = new StringBuilder();
        // Main logic for reading
        String line;
        try {
            // Read all the way to end
            while (!((line = this.reader.readLine()).startsWith("</top>"))) {
                String processed = line.trim();
                sb.append(' ');
                if (processed.startsWith("<title>")) {
                    // Parse title
                    processed = processed.substring(8);
                } else if (processed.startsWith("<desc>")) {
                    continue;
                } else if (processed.startsWith("<narr>")) {
                    continue;
                }
                sb.append(processed);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void closeHandler() {
        if (this.reader == null) return;
        try {
            this.reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
