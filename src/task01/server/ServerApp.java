package task01.server;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.util.stream.*;
import java.net.*;

public class ServerApp {

    public static void main(String[] args) throws IOException, FileNotFoundException {

        int port;
        int num_decks;

        if (args.length <= 0 || args.length > 2) {
            System.out.println("Invalid number of arguments expected");
            System.exit(-1);
        }

        port = Integer.parseInt(args[0]);
        num_decks = Integer.parseInt(args[1]);

        // Create deck
        List<String> deck = new ArrayList<>();
        for (int num = 1; num <= 13; num++) {
            for (int suit = 1; suit <= 4; suit++) {
                String card = num + "." + suit;
                deck.add(card);
            }
        }

        List<String> totalDecks = deck.stream()
                .flatMap(card -> Collections.nCopies(num_decks, card).stream())
                .collect(Collectors.toList());
        // Shuffle list
        Collections.shuffle(totalDecks);

        // Create data file to store deck
        Writer write = new FileWriter(new File("cards.db"));
        BufferedWriter bw = new BufferedWriter(write);
        totalDecks.forEach(card -> {
            try {
                bw.write(card + "\n");
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        });
        bw.flush();
        write.flush();
        bw.close();
        write.close();

        // Main operation
        ServerSocket server = new ServerSocket(port);
        ExecutorService thrPool = Executors.newFixedThreadPool(3);

        while (true) {
            Socket sock = server.accept();
            BaccaratEngine be = new BaccaratEngine(sock);
            thrPool.submit(be);
        }

    }
}