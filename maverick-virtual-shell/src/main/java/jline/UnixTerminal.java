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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package jline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 *  <p>
 *  Terminal that is used for unix platforms. Terminal initialization
 *  is handled by issuing the <em>stty</em> command against the
 *  <em>/dev/tty</em> file to disable character echoing and enable
 *  character input. All known unix systems (including
 *  Linux and Macintosh OS X) support the <em>stty</em>), so this
 *  implementation should work for an reasonable POSIX system.
 *        </p>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 *  @author  Updates <a href="mailto:dwkemp@gmail.com">Dale Kemp</a> 2005-12-03
 */
public class UnixTerminal extends Terminal {
    public static final short ARROW_START = 27;
    public static final short ARROW_PREFIX = 91;
    public static final short ARROW_LEFT = 68;
    public static final short ARROW_RIGHT = 67;
    public static final short ARROW_UP = 65;
    public static final short ARROW_DOWN = 66;
    public static final short O_PREFIX = 79;
    public static final short HOME_CODE = 72;
    public static final short END_CODE = 70;

    public static final short DEL_THIRD = 51;
    public static final short DEL_SECOND = 126;

    private boolean echoEnabled;
    private String ttyConfig;
    private boolean backspaceDeleteSwitched = false;
    private static String sttyCommand =
        System.getProperty("jline.sttyCommand", "stty");

    
    String encoding = System.getProperty("input.encoding", "UTF-8");
    ReplayPrefixOneCharInputStream replayStream = new ReplayPrefixOneCharInputStream(encoding);
    InputStreamReader replayReader;
	String tty;


    public UnixTerminal() {
    	this("/dev/tty");
    }

    public UnixTerminal(String tty) {
        try {
        	this.tty = tty;
            replayReader = new InputStreamReader(replayStream, encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
   
    protected void checkBackspace(){
        String[] ttyConfigSplit = ttyConfig.split(":|=");

        if (ttyConfigSplit.length < 7)
            return;
        
        if (ttyConfigSplit[6] == null)
            return;
	
        backspaceDeleteSwitched = ttyConfigSplit[6].equals("7f");
    }
    
    /**
     *  Remove line-buffered input by invoking "stty -icanon min 1"
     *  against the current terminal.
     */
    public void initializeTerminal() throws IOException, InterruptedException {
        // save the initial tty configuration
        ttyConfig = stty("-g", tty);

        // sanity check
        if ((ttyConfig.length() == 0)
                || ((ttyConfig.indexOf("=") == -1)
                       && (ttyConfig.indexOf(":") == -1))) {
            throw new IOException("Unrecognized stty code: " + ttyConfig);
        }

        checkBackspace();

        // set the console to be character-buffered instead of line-buffered
        stty("-icanon min 1", tty);

        // disable character echoing
        stty("-echo", tty);
        echoEnabled = false;

        // at exit, restore the original tty configuration (for JDK 1.3+)
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void start() {
                        try {
                            restoreTerminal();
                        } catch (Exception e) {
                            consumeException(e);
                        }
                    }
                });
        } catch (AbstractMethodError ame) {
            // JDK 1.3+ only method. Bummer.
            consumeException(ame);
        }
    }

    /** 
     * Restore the original terminal configuration, which can be used when
     * shutting down the console reader. The ConsoleReader cannot be
     * used after calling this method.
     */
    public void restoreTerminal() throws Exception {
        if (ttyConfig != null) {
            stty(ttyConfig, tty);
            ttyConfig = null;
        }
        resetTerminal();
    }

    
    
    public int readVirtualKey(InputStream in) throws IOException {
        int c = readCharacter(in);

        if (backspaceDeleteSwitched)
            if (c == DELETE)
                c = '\b';
            else if (c == '\b')
                c = DELETE;

        // in Unix terminals, arrow keys are represented by
        // a sequence of 3 characters. E.g., the up arrow
        // key yields 27, 91, 68
        if (c == ARROW_START) {
		//also the escape key is 27
		//thats why we read until we
		//have something different than 27
		//this is a bugfix, because otherwise
		//pressing escape and than an arrow key
		//was an undefined state
		while (c == ARROW_START)
            		c = readCharacter(in);
            if (c == ARROW_PREFIX || c == O_PREFIX) {
                c = readCharacter(in);
                if (c == ARROW_UP) {
                    return CTRL_P;
                } else if (c == ARROW_DOWN) {
                    return CTRL_N;
                } else if (c == ARROW_LEFT) {
                    return CTRL_B;
                } else if (c == ARROW_RIGHT) {
                    return CTRL_F;
                } else if (c == HOME_CODE) {
                    return CTRL_A;
                } else if (c == END_CODE) {
                    return CTRL_E;
                } else if (c == DEL_THIRD) {
                    c = readCharacter(in); // read 4th
                    return DELETE;
                }
            } 
        } 
        // handle unicode characters, thanks for a patch from amyi@inf.ed.ac.uk
        if (c > 128) {
          // handle unicode characters longer than 2 bytes,
          // thanks to Marc.Herbert@continuent.com
            replayStream.setInput(c, in);
//            replayReader = new InputStreamReader(replayStream, encoding);
            c = replayReader.read();
            
        }

        return c;
    }

    /**
     *  No-op for exceptions we want to silently consume.
     */
    private void consumeException(Throwable e) {
    }

    public boolean isSupported() {
        return true;
    }

    public boolean getEcho() {
        return false;
    }

    /**
     *  Returns the value of "stty size" width param.
     *
     *  <strong>Note</strong>: this method caches the value from the
     *  first time it is called in order to increase speed, which means
     *  that changing to size of the terminal will not be reflected
     *  in the console.
     */
    public int getTerminalWidth() {
        int val = -1;

        try {
            val = getTerminalProperty("columns", tty);
        } catch (Exception e) {
        }

        if (val == -1) {
            val = 80;
        }

        return val;
    }

    /**
     *  Returns the value of "stty size" height param.
     *
     *  <strong>Note</strong>: this method caches the value from the
     *  first time it is called in order to increase speed, which means
     *  that changing to size of the terminal will not be reflected
     *  in the console.
     */
    public int getTerminalHeight() {
        int val = -1;

        try {
            val = getTerminalProperty("rows", tty);
        } catch (Exception e) {
        }

        if (val == -1) {
            val = 24;
        }

        return val;
    }

    private static int getTerminalProperty(String prop, String tty)
                                    throws IOException, InterruptedException {
        // need to be able handle both output formats:
        // speed 9600 baud; 24 rows; 140 columns;
        // and:
        // speed 38400 baud; rows = 49; columns = 111; ypixels = 0; xpixels = 0;
        String props = stty("-a", tty);

        for (StringTokenizer tok = new StringTokenizer(props, ";\n");
                 tok.hasMoreTokens();) {
            String str = tok.nextToken().trim();

            if (str.startsWith(prop)) {
                int index = str.lastIndexOf(" ");

                return Integer.parseInt(str.substring(index).trim());
            } else if (str.endsWith(prop)) {
                int index = str.indexOf(" ");

                return Integer.parseInt(str.substring(0, index).trim());
            }
        }

        return -1;
    }

    /**
     *  Execute the stty command with the specified arguments
     *  against the current active terminal.
     */
    private static String stty(final String args, String tty)
                        throws IOException, InterruptedException {
        return exec("stty " + args + " < " + tty).trim();
    }

    /**
     *  Execute the specified command and return the output
     *  (both stdout and stderr).
     */
    private static String exec(final String cmd)
                        throws IOException, InterruptedException {
        return exec(new String[] {
                        "sh",
                        "-c",
                        cmd
                    });
    }

    /**
     *  Execute the specified command and return the output
     *  (both stdout and stderr).
     */
    private static String exec(final String[] cmd)
                        throws IOException, InterruptedException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        Process p = Runtime.getRuntime().exec(cmd);
        int c;
        InputStream in;

        in = p.getInputStream();

        while ((c = in.read()) != -1) {
            bout.write(c);
        }

        in = p.getErrorStream();

        while ((c = in.read()) != -1) {
            bout.write(c);
        }

        p.waitFor();

        String result = new String(bout.toByteArray());

        return result;
    }

    /**
     *  The command to use to set the terminal options. Defaults
     *  to "stty", or the value of the system property "jline.sttyCommand".
     */
    public static void setSttyCommand(String cmd) {
        sttyCommand = cmd;
    }

    /**
     *  The command to use to set the terminal options. Defaults
     *  to "stty", or the value of the system property "jline.sttyCommand".
     */
    public static String getSttyCommand() {
        return sttyCommand;
    }

    public synchronized boolean isEchoEnabled() {
        return echoEnabled;
    }


    public synchronized void enableEcho() {
    	try {
			stty("echo", tty);
            echoEnabled = true;
		} catch (Exception e) {
			consumeException(e);
		}
    }

    public synchronized void disableEcho() {
    	try {
			stty("-echo", tty);
            echoEnabled = false;
		} catch (Exception e) {
			consumeException(e);
		}
    }

    /**
     * This is awkward and inefficient, but probably the minimal way to add
     * UTF-8 support to JLine
     *
     * @author <a href="mailto:Marc.Herbert@continuent.com">Marc Herbert</a>
     */
    static class ReplayPrefixOneCharInputStream extends InputStream {
        byte firstByte;
        int byteLength;
        InputStream wrappedStream;
        int byteRead;

        final String encoding;
        
        public ReplayPrefixOneCharInputStream(String encoding) {
            this.encoding = encoding;
        }
        
        public void setInput(int recorded, InputStream wrapped) throws IOException {
            this.byteRead = 0;
            this.firstByte = (byte) recorded;
            this.wrappedStream = wrapped;

            byteLength = 1;
            if (encoding.equalsIgnoreCase("UTF-8"))
                setInputUTF8(recorded, wrapped);
            else if (encoding.equalsIgnoreCase("UTF-16"))
                byteLength = 2;
            else if (encoding.equalsIgnoreCase("UTF-32"))
                byteLength = 4;
        }
            
            
        public void setInputUTF8(int recorded, InputStream wrapped) throws IOException {
            // 110yyyyy 10zzzzzz
            if ((firstByte & (byte) 0xE0) == (byte) 0xC0)
                this.byteLength = 2;
            // 1110xxxx 10yyyyyy 10zzzzzz
            else if ((firstByte & (byte) 0xF0) == (byte) 0xE0)
                this.byteLength = 3;
            // 11110www 10xxxxxx 10yyyyyy 10zzzzzz
            else if ((firstByte & (byte) 0xF8) == (byte) 0xF0)
                this.byteLength = 4;
            else
                throw new IOException("invalid UTF-8 first byte: " + firstByte);
        }

        public int read() throws IOException {
            if (available() == 0)
                return -1;

            byteRead++;

            if (byteRead == 1)
                return firstByte;

            return wrappedStream.read();
        }

        /**
        * InputStreamReader is greedy and will try to read bytes in advance. We
        * do NOT want this to happen since we use a temporary/"losing bytes"
        * InputStreamReader above, that's why we hide the real
        * wrappedStream.available() here.
        */
        public int available() {
            return byteLength - byteRead;
        }
    }
}
