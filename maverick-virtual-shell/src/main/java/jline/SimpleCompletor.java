/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package jline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 *  <p>
 *  A simple {@link Completor} implementation that handles a pre-defined
 *  list of completion words.
 *  </p>
 *
 *  <p>
 *  Example usage:
 *  </p>
 *  <pre>
 *  myConsoleReader.addCompletor (new SimpleCompletor (new String [] { "now", "yesterday", "tomorrow" }));
 *  </pre>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class SimpleCompletor implements Completor, Cloneable {
    /**
     *  The list of candidates that will be completed.
     */
    SortedSet<String> candidates;

    /**
     *  A delimiter to use to qualify completions.
     */
    String delimiter;
    final SimpleCompletorFilter filter;

    /**
     *  Create a new SimpleCompletor with a single possible completion
     *  values.
     */
    public SimpleCompletor(final String candidateString) {
        this(new String[] {
                 candidateString
             });
    }

    /**
     *  Create a new SimpleCompletor with a list of possible completion
     *  values.
     */
    public SimpleCompletor(final String[] candidateStrings) {
        this(candidateStrings, null);
    }

    public SimpleCompletor(final String[] strings,
                           final SimpleCompletorFilter filter) {
        this.filter = filter;
        setCandidateStrings(strings);
    }

    /**
     *  Complete candidates using the contents of the specified Reader.
     */
    public SimpleCompletor(final Reader reader) throws IOException {
        this(getStrings(reader));
    }

    /**
     *  Complete candidates using the whitespearated values in
     *  read from the specified Reader.
     */
    public SimpleCompletor(final InputStream in) throws IOException {
        this(getStrings(new InputStreamReader(in)));
    }

    private static String[] getStrings(final Reader in)
                                throws IOException {
        Reader reader =
            (in instanceof BufferedReader) ? in : new BufferedReader(in);

        try {
	        List<String> words = new LinkedList<String>();
	        String line;
	
	        while ((line = ((BufferedReader) reader).readLine()) != null) {
	            for (StringTokenizer tok = new StringTokenizer(line);
	                     tok.hasMoreTokens(); words.add(tok.nextToken())) {
	                ;
	            }
	        }
	
	        return (String[]) words.toArray(new String[words.size()]);
        } finally {
        	reader.close();
        }
    }

	public int complete(final String buffer, final int cursor, final List<String> clist) {
        String start = (buffer == null) ? "" : buffer;

        SortedSet<String> matches = candidates.tailSet(start);

        for (Iterator<String> i = matches.iterator(); i.hasNext();) {
            String can = i.next();

            if (!(can.startsWith(start))) {
                break;
            }

            if (delimiter != null) {
                int index = can.indexOf(delimiter, cursor);

                if (index != -1) {
                    can = can.substring(0, index + 1);
                }
            }

            clist.add(can);
        }

        if (clist.size() == 1) {
            clist.set(0, ((String) clist.get(0)) + " ");
        }

        // the index of the completion is always from the beginning of
        // the buffer.
        return (clist.size() == 0) ? (-1) : 0;
    }

    public void setDelimiter(final String delimiter) {
        this.delimiter = delimiter;
    }

    public String getDelimiter() {
        return this.delimiter;
    }

    public void setCandidates(final SortedSet<String> candidates) {
        if (filter != null) {
            TreeSet<String> filtered = new TreeSet<String>();

            for (Iterator<String> i = candidates.iterator(); i.hasNext();) {
                String element = (String) i.next();
                element = filter.filter(element);

                if (element != null) {
                    filtered.add(element);
                }
            }

            this.candidates = filtered;
        } else {
            this.candidates = candidates;
        }
    }

    public SortedSet<String> getCandidates() {
        return Collections.unmodifiableSortedSet(this.candidates);
    }

    public void setCandidateStrings(final String[] strings) {
        setCandidates(new TreeSet<String>(Arrays.asList(strings)));
    }

    public void addCandidateString(final String candidateString) {
        final String string =
            (filter == null) ? candidateString : filter.filter(candidateString);

        if (string != null) {
            candidates.add(string);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     *  Filter for elements in the completor.
     *
     *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
     */
    public static interface SimpleCompletorFilter {
        /**
         *  Filter the specified String. To not filter it, return the
         *  same String as the parameter. To exclude it, return null.
         */
        public String filter(String element);
    }

    public static class NoOpFilter implements SimpleCompletorFilter {
        public String filter(final String element) {
            return element;
        }
    }
}