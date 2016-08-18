/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pvjava;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author Sasa
 */
public class Configurations {

    private static int port = 14739;
    private static String host = "127.0.0.1";
    private static List<Integer> monitorOrder = new ArrayList<Integer>();

    private static void init() {
        for (int i = 0; i < 32; i++) {
            monitorOrder.add(i);
        }
        //DEMO DATAF
    }

    public static int getPort() {
        return port;
    }

    public static String getHost() {
        return host;
    }

    public static int getMonitorPosition(int position) {
        return monitorOrder.get(position);
    }

    public static void readCongfigs(String file) throws IOException {
        init();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                switch (st.nextToken()) {
                    case "port":
                        port = Integer.parseInt(st.nextToken());
                        break;
                    case "host":
                        host = st.nextToken();
                        break;
                    case "monitors":
                        monitorOrder.clear();
                        while (st.hasMoreElements()) {
                            monitorOrder.add(Integer.parseInt(st.nextToken()));
                        }
                        break;
                    default:
                        if (line.charAt(0) != '#') {
                            System.out.println("Oops no such setting " + line);
                        }
                        break;
                }
            }
        }
    }

    private Configurations() {

    }
}
