package use;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class EchoClient {
    public static void main(String[] args){
        if(args.length != 2){
            System.err.println("Usage: java EchoClient <hostname> <port>");
            System.exit(1);
        }
        String host = args[0];
        int portNumber = Integer.parseInt(args[1]);
        try(Socket echoSocket = new Socket(host, portNumber);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        ){
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            String userInput ;
            while((userInput = stdIn.readLine()) != null){
                out.println(userInput);
                System.out.println("Echo: " + in.readLine());
                if(userInput.endsWith("bye")) break;
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    host);
            System.exit(1);
        }


    }
}
