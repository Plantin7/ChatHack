package fr.uge.nonblocking.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {

  private static void sendBroadcastMsg(SocketChannel sc, String message, String login)
      throws IOException {
    var bb = ByteBuffer.allocate(1024);
    bb.put((byte) 5);
    var loginbb = StandardCharsets.UTF_8.encode(login);
    var msgbb = StandardCharsets.UTF_8.encode(message);
    bb.putInt(loginbb.limit());
    bb.put(loginbb);
    bb.putInt(msgbb.limit());
    bb.put(msgbb);
    bb.flip();
    sc.write(bb);

    System.out.println("readdd");
    bb.clear();
    sc.read(bb);

    bb.flip();
    var size = bb.getInt();
    System.out.println("login size: " + size);
  }

  private static void sendAnonymousConnection(SocketChannel sc, String login) throws IOException {
    var bb = ByteBuffer.allocate(1024);
    bb.put((byte) 0);
    var loginbb = StandardCharsets.UTF_8.encode(login);
    bb.putInt(loginbb.limit());
    bb.put(loginbb);
    bb.flip();
    System.out.println(bb);
    sc.write(bb);
    var scan = new Scanner(System.in);
    while (scan.hasNextLine()) {

    }
  }


  public static void main(String[] args) throws IOException {
    var addr = new InetSocketAddress("localhost", 7777);
    var sc = SocketChannel.open(addr);

    
     sendBroadcastMsg(sc, "hello", "peach");
  }
}