package utb.fai;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class EmailSender {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private boolean closed;

    /*
     * Constructor opens Socket to host/port. If the Socket throws an exception
     * during opening,
     * the exception is not handled in the constructor.
     */
    public EmailSender(String host, int port) throws UnknownHostException, IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        try {
            expectResponse(220);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    /*
     * Sends email from an email address to an email address with some subject and
     * text.
     * If the Socket throws an exception during sending, the exception is not
     * handled by this method.
     */
    public void send(String from, String to, String subject, String text) throws IOException {
        ensureOpen();
        rejectHeaderBreaks(from);
        rejectHeaderBreaks(to);
        rejectHeaderBreaks(subject);

        sendCommand("HELO localhost", 250);
        sendCommand("MAIL FROM:<" + from + ">", 250);
        sendCommand("RCPT TO:<" + to + ">", 250, 251);
        sendCommand("DATA", 354);

        writeDataLine("From: " + from);
        writeDataLine("To: " + to);
        writeDataLine("Subject: " + subject);
        writeDataLine("");

        String normalizedText = text.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalizedText.split("\n", -1)) {
            writeDataLine(line);
        }

        writer.write(".\r\n");
        writer.flush();
        expectResponse(250);
    }

    /*
     * Sends QUIT and closes the socket
     */
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        IOException failure = null;
        try {
            writer.write("QUIT\r\n");
            writer.flush();
            expectResponse(221);
        } catch (IOException e) {
            failure = e;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }

        if (failure != null) {
            throw new UncheckedIOException(failure);
        }
    }

    private void sendCommand(String command, int... expectedCodes) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        expectResponse(expectedCodes);
    }

    private void expectResponse(int... expectedCodes) throws IOException {
        String line = reader.readLine();
        if (line == null || line.length() < 4 || !isStatusCode(line)) {
            throw new IOException("Invalid or missing SMTP response: " + line);
        }

        int code = Integer.parseInt(line.substring(0, 3));
        char separator = line.charAt(3);
        while (separator == '-') {
            line = reader.readLine();
            if (line == null || line.length() < 4 || !isStatusCode(line)) {
                throw new IOException("Invalid or incomplete SMTP response: " + line);
            }
            if (Integer.parseInt(line.substring(0, 3)) != code) {
                throw new IOException("Inconsistent multiline SMTP response: " + line);
            }
            separator = line.charAt(3);
        }

        for (int expectedCode : expectedCodes) {
            if (code == expectedCode) {
                return;
            }
        }
        throw new IOException("Unexpected SMTP response: " + code);
    }

    private boolean isStatusCode(String line) {
        return Character.isDigit(line.charAt(0))
                && Character.isDigit(line.charAt(1))
                && Character.isDigit(line.charAt(2))
                && (line.charAt(3) == ' ' || line.charAt(3) == '-');
    }

    private void writeDataLine(String line) throws IOException {
        if (line.startsWith(".")) {
            writer.write('.');
        }
        writer.write(line);
        writer.write("\r\n");
    }

    private void ensureOpen() throws IOException {
        if (closed || socket.isClosed()) {
            throw new IOException("SMTP connection is closed");
        }
    }

    private void rejectHeaderBreaks(String value) {
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("SMTP headers must not contain line breaks");
        }
    }
}
