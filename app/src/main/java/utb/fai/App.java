package utb.fai;

public class App {
    public static void main(String[] args) {
        if (args.length < 6) {
            return;
        }

        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String from = args[2];
            String to = args[3];
            String subject = args[4];
            String text = args[5];

            EmailSender sender = new EmailSender(host, port);
            try {
                sender.send(from, to, subject, text);
            } finally {
                sender.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
