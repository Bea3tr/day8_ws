package task01.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientApp {

    public static void main(String[] args) throws UnknownHostException, IOException {
        String host;
        int port;

        if (args.length <= 0 || args.length > 1) {
            System.err.println("Invalid number of arguments expected");
            System.exit(-1);
        }

        String[] inputs = args[0].split(":");
        host = inputs[0];
        port = Integer.parseInt(inputs[1]);

        Socket sock = new Socket(host, port);
        Console cons = System.console();

        // Get input & output streams
        InputStream is = sock.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        DataInputStream dis = new DataInputStream(bis);

        OutputStream os = sock.getOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(os);
        DataOutputStream dos = new DataOutputStream(bos);

        String fromServer = "";
        while (fromServer != null) {
            String cmd = cons.readLine("Command: ");
            dos.writeUTF(cmd);
            dos.flush();

            fromServer = dis.readUTF();
            System.out.println(fromServer);
            if (fromServer.startsWith("Terminating")) {
                break;
            }
        }
        dos.close();
        bos.close();
        os.close();

        dis.close();
        bis.close();
        is.close();
        sock.close();

    }

}
