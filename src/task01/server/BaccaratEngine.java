package task01.server;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class BaccaratEngine implements Runnable {

    private final Socket sock;
    private List<String> deck;

    public BaccaratEngine(Socket s) {
        this.sock = s;
        this.deck = loadCard();
    }

    public int getBalance(String name) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File(name + ".db")));
        int bal = Integer.parseInt(br.readLine());
        br.close();
        return bal;
    }

    public void setBalance(String name, int amt) throws FileNotFoundException, IOException {
        int newBal = getBalance(name) + amt;
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(name + ".db")));
        bw.write(Integer.toString(newBal));
        bw.flush();
        bw.close();
    }
    public int sumValue(List<String> cards) {
        int sum = 0;
        for (String card : cards) {
            int num = Integer.parseInt(card);
            sum += num;
        }
        return sum;
    }

    public String cardVal(String card) {
        System.out.println("Calculating value...");
        System.out.println(card);
        String[] value = card.split("\\.");
        System.out.println(value[0]);
        int num = Integer.parseInt(value[0]);
        if (num > 10)
            num = 10;
        return Integer.toString(num);
    }

    public List<String> loadCard() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("cards.db")));
            List<String> deck = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                deck.add(line);
            }
            br.close();
            return deck;

        } catch (FileNotFoundException ex) {
            System.err.println("Missing card deck");
            return deck;

        } catch (IOException ex) {
            System.err.println("Error processing card deck");
            return deck;
        }

    }

    public int lessThanFifteen(List<String> cards) {
        cards.add(cardVal(deck.remove(0)));
        return sumValue(cards);
    }

    public String outcome(List<String> banker, List<String> player) {
        String result = "P";
        for (String card : player) {
            int value = Integer.parseInt(card);
            if (value > 10)
                value = 10;
            result += "|" + value;
        }
        result += ", B";
        for (String card : banker) {
            int value = Integer.parseInt(card);
            if (value > 10)
                value = 10;
            result += "|" + value;
        }
        System.out.println(result);
        return result;
    }

    public BufferedWriter writeFile(String player) throws IOException {
        Writer createFile = new FileWriter(new File(player + ".db"));
        BufferedWriter bFile = new BufferedWriter(createFile);
        return bFile;
    }

    public String dealCards(List<String> player, List<String> banker) {
        System.out.println("Checking remaining deck...");
        if(deck.size() < 4) {
            System.out.println("Insuffient cards in deck");
            System.exit(0);
        }
        int playerHand = 0;
        int bankerHand = 0;
        System.out.println("Distributing cards...");
        for (int i = 0; i < 2; i++) {
            System.out.println("Getting cards...");
            player.add(cardVal(deck.remove(0)));
            System.out.println("Getting cards...");
            banker.add(cardVal(deck.remove(0)));
        }
        System.out.println("Cards distributed");
        bankerHand = sumValue(banker);
        playerHand = sumValue(player);

        if (bankerHand <= 15) {
            bankerHand = lessThanFifteen(banker);
        }
        if (playerHand <= 15) {
            playerHand = lessThanFifteen(player);
        }

        System.out.println("Calculating points...");
        // Points of each side
        bankerHand %= 10;
        playerHand %= 10;
        String result = "";

        if (bankerHand == playerHand) {
            result = "D";
        } else if (bankerHand > playerHand) {
            result = "B";
        } else {
            result = "P";
        }
        System.out.println("Result is " + result);
        return result;
    }

    private void game_history(List<String> games) throws IOException {
        File history = new File("game_history.csv");
        Writer write = new FileWriter(history);
        BufferedWriter bw = new BufferedWriter(write);
        System.out.println(games.size());
        for (int i = 0; i < games.size(); i+=6) {
            if(i+6 > games.size()) {
                bw.write(String.join(",", games.subList(i, games.size())) + "\n");
                bw.flush();
            } else {
                bw.write(String.join(",", games.subList(i, i+6)) + "\n");
                bw.flush();
            }
            
        }
        bw.close();
        write.close();
    }

    @Override
    public void run() {

        try {
            // Get input & output streams
            InputStream is = sock.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);

            OutputStream os = sock.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);

            String name = "";
            String input;
            String bet = "";
            String deal;
            List<String> games = new ArrayList<>();
            BufferedWriter bFile;
            while ((input = dis.readUTF()) != null) {
                System.out.println(input);
                // Parse command
                String[] cmd = input.split("\\|");
                System.out.println(cmd[0]);
                switch (cmd[0]) {
                    // Can use BigInteger to use bigger values
                    case "login":
                        dos.writeUTF("User logged in");
                        dos.flush();
                        name = cmd[1];
                        bFile = writeFile(name);
                        bFile.write(cmd[2]);
                        bFile.flush();
                        bFile.close();
                        break;

                    case "bet":
                        bet = cmd[1];
                        if(getBalance(name) < Integer.parseInt(bet)) {
                            dos.writeUTF("Insufficient amount");
                            dos.flush();
                            System.exit(0);
                        }
                        dos.writeUTF("Betting $%s\tCurrent balance %d".formatted(bet, getBalance(name)));
                        dos.flush();
                        break;

                    case "deal":
                        deal = cmd[1];
                        List<String> player = new ArrayList<>();
                        List<String> banker = new ArrayList<>();
                        String winner = dealCards(player, banker);
                        games.add(winner);

                        if (winner.equals("D")) {
                            dos.writeUTF(outcome(banker, player) + "\n" + "It's a draw\tReturning bet amount to player\tCurrent balance: $%d".formatted(getBalance(name)));
                            dos.flush();

                        } else if(winner.equals(deal)) {
                            setBalance(name, Integer.parseInt(bet));
                            if(winner.equals("B")) {
                                dos.writeUTF(outcome(banker, player) + "\n" + "Banker wins\tCurrent balance: $%d".formatted(getBalance(name)));
                                dos.flush();
                            }
                            if(winner.equals("P")) {
                                dos.writeUTF(outcome(banker, player) + "\n" + "Player wins\tCurrent balance: $%d".formatted(getBalance(name)));
                                dos.flush();
                            }
                        } else {
                            setBalance(name, -1 * Integer.parseInt(bet));
                            if(winner.equals("B")) {
                                dos.writeUTF(outcome(banker, player) + "\n" + "Banker wins\tCurrent balance: $%d".formatted(getBalance(name)));
                                dos.flush();
                            }
                            if(winner.equals("P")) {
                                dos.writeUTF(outcome(banker, player) + "\n" + "Player wins\tCurrent balance: $%d".formatted(getBalance(name)));
                                dos.flush();
                            }
                        }
                        break;
                    
                    case "quit":
                        game_history(games);
                        dos.writeUTF("Terminating game. Thank you for playing!");
                        dos.flush();
                        dos.close();
                        bos.close();
                        os.close();

                        dis.close();
                        bis.close();
                        is.close();
                        sock.close();
                        break;

                    default:
                }
            }
        } catch (IOException ex) {
            System.err.println(ex.toString());
        }
    }

}
