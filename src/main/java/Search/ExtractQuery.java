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
    // Adjust as needed, title only is much faster
    private boolean isTitleOnly;

    /**
     * you should extract the 4 queries from the Path.TopicDir
     * the query content of each topic should be 1) tokenized, 2) to lowercase, 3) remove stop words, 4) stemming
     * you can simply pick up title only for query, or you can also use title + description + narrative for the query content.
     */
    public ExtractQuery() {
        try {
            this.reader = Files.newBufferedReader(Paths.get(Path.TopicDir));
            this.isTitleOnly = true;
            // Init stop words set
            try (Stream<String> lines = Files.lines(Paths.get(Path.StopwordDir))) {
                lines.forEach(s -> this.stopWords.add(s.trim().toLowerCase(Locale.US)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.reader = null;
        }
    }

    public boolean isTitleOnly() {
        return isTitleOnly;
    }

    public void setTitleOnly(boolean titleOnly) {
        isTitleOnly = titleOnly;
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
        String originalContent = processOneTopic(this.isTitleOnly);
        q.SetQueryContent(irPreProcess(originalContent));
        return q;
    }

    /**
     * 1) tokenized, 2) to lowercase, 3) remove stop words, 4) stemming
     */
    private String irPreProcess(String original) {
        String wekaDelimiters = "\r\n\t.,;:\"()?! ";
        StringTokenizer st = new StringTokenizer(original, wekaDelimiters);
        StringBuilder sb = new StringBuilder();
        while (st.hasMoreTokens()) {
            String lcnss = normalizeString(st.nextToken());
            if (lcnss.isEmpty()) continue;
            sb.append(lcnss);
            sb.append(' ');
        }
        return sb.toString();
    }

    private String normalizeString(String token) {
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
            if (data.trim().toLowerCase(Locale.US).startsWith("<top>")) return true;
        }
        return false;
    }

    /**
     * Line now at end of <num>, populate the whole query
     */
    private String processOneTopic(boolean titleOnly) {
        StringBuilder sb = new StringBuilder();
        boolean hasTitleSaved = false;
        // Main logic for reading
        try {
            String line;
            // Read all the way to end of this document
            while (!((line = this.reader.readLine().trim()).startsWith("</top>"))) {
                // No need to process this line with these conditions
                if ((titleOnly && hasTitleSaved) || line.isEmpty()) continue;
                String lcLine = line.toLowerCase(Locale.US).trim();
                sb.append(' ');
                if (lcLine.startsWith("<title>")) {
                    // Parse title
                    line = line.substring(8);
                    if (titleOnly) {
                        sb.append(line);
                        hasTitleSaved = true;
                        continue;
                    }
                } else if (lcLine.startsWith("<desc>")
                        || lcLine.startsWith("<narr>")) {
                    continue;
                }
                sb.append(line);
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
