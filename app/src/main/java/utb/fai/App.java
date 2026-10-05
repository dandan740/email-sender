package utb.fai;

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class App {
    public static void main(String[] args)
    {        
        try
        {
            Socket socket = new Socket("mtb.server.utb.cz", 25);

            System.out.println("Klient navazel spojeni");

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            String message = "EHLO_Localhost\r\n";

            out.write(message.getBytes());
            out.flush();
            Thread.sleep(100);

            final byte[] buffer = new byte[256];
            int len = in.read(buffer, 0, 256);
            
            if(in.available() > 0) {
                len = in.read(buffer, 0, 256);
                System.out.write(buffer, 0, len);
            }

            message = "PCPT_IO:d_matusu@utb.cz\r\n";
            out.write(message.getBytes());
            out.flush();
            Thread.sleep(100);

            EmailSender sender = new EmailSender("smtp.utb.cz", 25);
            sender.send("d_matusu@utb.cz", "d_matusu@utb.cz", "Email from Java", "Funguje to ofc.\n");
            sender.close();
            socket.close();
        }
        
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
