/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pvjava;

import GUI.PVviewer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferStrategy;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sasa
 */
public class PVjava {

    private static final Color AMBER = new Color(255, 126, 0);
    private static final Color VERY_DARK_GRAY = new Color(24, 24, 24);
    private static final Color BB_GREEN = new Color(0, 160, 0);
    private static final Color BB_RED = new Color(160, 16, 32);
    private static final Color BB_UPTOWN_GREEN = new Color(1, 198, 5);
    private static final Color BB_DOWNTOWN_RED = new Color(219, 51, 51);

    //private static final List<Point>
    public static double distance2D(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static Color relativeColor(Color sat, Color gray, double scale) {
        double s = clamp(scale);
        return new Color((int) (sat.getRed() * s + gray.getRed() * (1 - s)), (int) (sat.getGreen() * s + gray.getGreen() * (1 - s)), (int) (sat.getBlue() * s + gray.getBlue() * (1 - s)), (int) (sat.getAlpha() * s + gray.getAlpha() * (1 - s)));
    }

    public static double clamp(double d) {
        return Math.min(1, Math.max(0, d));
    }

    private static int clampRange(int oldVal, int newVal, int maxChange) {
        return Math.max(oldVal - maxChange, Math.min(oldVal + maxChange, newVal));
    }

    private static class PixelCoordinate {

        public int x, y, z;
    }

    private static class NewUpdateStatus {

        public NewUpdateStatus(long timestamp, int index) {
            this.timestamp = timestamp;
            this.index = index;
        }

        public long timestamp;
        public int index;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Configurations.readCongfigs("settings.cfg");
        } catch (IOException ex) {
            System.out.println("Could not read configs.");
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] dvs = ge.getScreenDevices();
        final List<PVviewer> pvs = new ArrayList<>();
        final List<BufferStrategy> bstrats = new ArrayList<>();

        for (int i = 0; i < dvs.length; i++) {
            GraphicsDevice graphicsDevice = dvs[Configurations.getMonitorPosition(i)];
            try {
                java.awt.EventQueue.invokeAndWait(() -> {
                    PVviewer pi = new PVviewer();
                    pvs.add(pi);
                    pi.setVisible(true);
                    graphicsDevice.setFullScreenWindow(pi);
                    pi.createBufferStrategy(2);
                });
            } catch (InterruptedException ex) {
                //a
            } catch (InvocationTargetException ex) {
                //b
            }
        }

        try {
            BufferedReader stockReader = new BufferedReader(new FileReader("stocks.csv"));
            String line;
            int index = 0;
            while ((line = stockReader.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                stocks.add(new Stock(index++, st.nextToken().split(" ")[0], st.nextToken(), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken())));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        new Thread() {
            @Override
            public void run() {
                while (true) {
                    stocks.stream().filter((stock) -> !(Math.random() > .005)).forEach((stock) -> {
                        synchronized (stock) {
                            stock.update();
                            if (stock.getChange() > 1.5) {
                                newlyChangedStocks.add(new NewUpdateStatus(System.currentTimeMillis(), stock.getIndex()));
                            }
                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }.start();

        final PixelCoordinate kinectCoordinate = new PixelCoordinate();
        final int SCREEN_WIDTH = pvs.get(0).getWidth();
        final int SCREEN_HEIGHT = pvs.get(0).getHeight();

        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Socket sock = new Socket(Configurations.getHost(), Configurations.getPort());
                        BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        String line;
                        System.out.println("about to read");
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                            StringTokenizer st = new StringTokenizer(line);
                            st.nextToken();
                            int kid = Integer.parseInt(st.nextToken());
                            double rx = Double.parseDouble(st.nextToken()) * Math.PI / 180;
                            double ry = Double.parseDouble(st.nextToken()) * Math.PI / 180;
                            double rz = Double.parseDouble(st.nextToken()) * Math.PI / 180;
                            double tx = Double.parseDouble(st.nextToken());
                            double ty = Double.parseDouble(st.nextToken());
                            double tz = Double.parseDouble(st.nextToken());
                            kinectCoordinate.x = clampRange(kinectCoordinate.x, (int) ((1920 + 1920 * 2 * kid) + 5000 * (tx + Math.sin(-ry) * tz)), 100);
                            kinectCoordinate.y = clampRange(SCREEN_HEIGHT / 2, (int) (SCREEN_HEIGHT / 2 + 2000 * (ty + Math.sin(-rx) * tz)), SCREEN_HEIGHT / 2);//clampRange(kinectCoordinate.y, (int) (540 + 5000 * (ty + Math.sin(-rx) * tz)), 100);
                            kinectCoordinate.z = clampRange(kinectCoordinate.z, (int) Math.max(1, 2 * Math.pow(tz / Math.cos(rx) / Math.cos(ry), 1.5)), 1);
                        }
                        Thread.sleep(1000);
                    } catch (ConnectException ex) {
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }.start();

        priceFormat = new DecimalFormat();
        priceFormat.setRoundingMode(RoundingMode.HALF_UP);
        priceFormat.setMinimumFractionDigits(1);
        priceFormat.setMaximumFractionDigits(1);

        while (true) {
            int mouseX = 0, mouseY = 0;
            try {
                for (int didx = 0; didx < pvs.size(); didx++) {
                    PVviewer pv = pvs.get(didx);

                    if (pv.getMousePosition() != null) {
                        mouseX = pv.getMousePosition().x + pv.getWidth() * didx;
                        mouseY = pv.getMousePosition().y;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                for (int didx = 0; didx < pvs.size(); didx++) {
                    PVviewer pv = pvs.get(didx);
                    Graphics2D g2d = (Graphics2D) pv.getBufferStrategy().getDrawGraphics();
                    g2d.setColor(Color.black);
                    g2d.fillRect(0, 0, pv.getWidth(), pv.getHeight());
                    int TILE_WIDTH = pv.getWidth() / TILES_ACROSS;
                    int TILE_HEIGHT = pv.getHeight() / TILES_HIGH;

                    if (PVviewer.mouseMode) {
                        paintLevel(4, didx, 0, 0, 1, 1, g2d, pv, mouseX, mouseY, PVviewer.mouseZ);
                    } else {
                        paintLevel(4, didx, 0, 0, 1, 1, g2d, pv, kinectCoordinate.x, kinectCoordinate.y, kinectCoordinate.z);
                    }
                    paintNewlyChangedStocks(g2d, didx, SCREEN_WIDTH, SCREEN_HEIGHT);

                    Point mpos = pv.getMousePosition();
                    if (pv.getMousePosition() != null && PVviewer.indicatorOn) {
                        g2d.setColor(new Color(0, 255, 255, 100));
                        g2d.fillOval(mpos.x - 50, mpos.y - 50, 100, 100);
                        g2d.setColor(Color.ORANGE);
                        g2d.fillOval(mpos.x - 5, mpos.y - 5, 10, 10);
                    }

                    if (PVviewer.debugMode) {
                        g2d.setColor(Color.MAGENTA);
                        g2d.setFont(new Font("Monospaced", Font.BOLD, 48));
                        g2d.drawString(mouseX + " " + mouseY + " " + PVviewer.mouseZ, 0, 100);
                        g2d.drawString(kinectCoordinate.x + " " + kinectCoordinate.y + " " + kinectCoordinate.z, 0, 200);
                    }
                    g2d.dispose();
                    pv.getBufferStrategy().show();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    final static List<Stock> stocks = new ArrayList<>();
    final static List<NewUpdateStatus> newlyChangedStocks = new LinkedList<>();
    final static int TILES_ACROSS = 16, TILES_HIGH = 16;
    final static int PADDING = 2;

    private static DecimalFormat priceFormat;

    public static void paintNewlyChangedStocks(Graphics2D g2d, int didx, int screenWidth, int screenHeight) {
        long nowTimestamp = System.currentTimeMillis();
        for (Iterator<NewUpdateStatus> it = newlyChangedStocks.iterator(); it.hasNext();) {
            NewUpdateStatus stockUpdate = it.next();
            if (nowTimestamp - stockUpdate.timestamp > 5000) {
                it.remove();
                continue;
            }
            int sidx = (stockUpdate.index / (TILES_ACROSS * TILES_HIGH));
            int xpos = (stockUpdate.index % (TILES_ACROSS * TILES_HIGH)) % TILES_ACROSS;
            int ypos = (stockUpdate.index % (TILES_ACROSS * TILES_HIGH)) / TILES_HIGH;
            System.out.println(stockUpdate.index + " " + sidx + " " + xpos + " " + ypos );
            int tileWidth = screenWidth / TILES_ACROSS;
            int tileHeight = screenHeight / TILES_HIGH;
            g2d.setColor(new Color(AMBER.getRed(), AMBER.getGreen(), AMBER.getBlue(), 100));
            g2d.setStroke(new BasicStroke(30));
            int radius = (int) (5000 - (nowTimestamp - stockUpdate.timestamp));
            g2d.drawOval((sidx - didx) * screenWidth + tileWidth * xpos + tileWidth / 2 - radius,
                    tileHeight * ypos + tileHeight / 2 - radius, 2 * radius, 2 * radius);
        }
    }

    public static void paintStock(int row, int col, Graphics2D g2d, Stock stock, PVviewer pv, double distance, int mouseX, int mouseY, int mouseZ) {
        int TILE_WIDTH = pv.getWidth() / TILES_ACROSS;
        int TILE_HEIGHT = pv.getHeight() / TILES_HIGH;
        int x = col * TILE_WIDTH + PADDING, y = row * TILE_HEIGHT + PADDING;

        g2d.setColor(relativeColor(stock.getChange() > 0 ? BB_UPTOWN_GREEN : BB_DOWNTOWN_RED, Color.DARK_GRAY, 1 - Math.pow(distance / 2000, .5)));

        //if (Math.abs(stock.getChange()) > 2.5 && ((System.currentTimeMillis() / 350) % 2) == 0) g2d.setColor(AMBER);
        if (stock.getChange() > 1.5 && System.currentTimeMillis() - stock.getChangeTimestamp() < 3000) {
            g2d.setColor(relativeColor(g2d.getColor(), AMBER, (System.currentTimeMillis() - stock.getChangeTimestamp()) / 3500.0));
        }
        g2d.fillRect(x, y, TILE_WIDTH - 2 * PADDING, TILE_HEIGHT - 2 * PADDING);

        g2d.setClip(new Rectangle(x, y, TILE_WIDTH - 2 * PADDING, TILE_HEIGHT - 2 * PADDING));
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.drawString(stock.getTicker(), x + 2, y + 22);

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g2d.drawString(String.format("%5s", priceFormat.format(stock.getPrice())), x + 60, y + 22);

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString(stock.getName(), x + 2, y + 40);
        g2d.setClip(null);
    }

    public static void paintLevel(int intensity, int didx, int ri, int ci, int rc, int cc, Graphics2D g2d, PVviewer pv, int mouseX, int mouseY, int mouseZ) {
        int intensityIndex = (int) Math.pow(2, intensity);
        int TILE_WIDTH = pv.getWidth() / TILES_ACROSS * intensityIndex;
        int TILE_HEIGHT = pv.getHeight() / TILES_HIGH * intensityIndex;
        for (int row = ri; row < rc + ri; row++) {
            for (int col = ci; col < cc + ci; col++) {
                try {
                    double distance = distance2D(mouseX, mouseY, col * TILE_WIDTH + TILE_WIDTH / 2 + didx * pv.getWidth(), row * TILE_HEIGHT + TILE_HEIGHT / 2);
                    if (distance < 2000 * (intensity) / Math.pow(mouseZ, .7)) {
                        paintLevel(intensity - 1, didx, row * 2, col * 2, 2, 2, g2d, pv, mouseX, mouseY, mouseZ);
                    } else {
                        if (intensity == 0) {
                            Stock stock = stocks.get(didx * TILES_ACROSS * TILES_HIGH + row * TILES_HIGH + col);
                            synchronized (stock) {
                                paintStock(row, col, g2d, stock, pv, distance, mouseX, mouseY, mouseZ);
                            }
                        } else {
                            List<Point> interests = new ArrayList<>(0);
                            double changeTotal = 0;
                            for (int rp = 0; rp < intensityIndex; rp++) {
                                for (int cp = 0; cp < intensityIndex; cp++) {
                                    Point p = new Point(rp + intensityIndex * row, cp + intensityIndex * col);
                                    Stock stock = stocks.get(didx * TILES_ACROSS * TILES_HIGH + (p.y) * TILES_HIGH + (p.x));
                                    synchronized (stock) {
                                        changeTotal += stock.getChange();
//                                        if (Math.abs(stock.getChange()) > 2.5) {
//                                            interests.add(new Point(cp, rp));
//                                        }
                                    }

                                }
                            }
                            g2d.setColor(relativeColor(changeTotal > 0 ? BB_UPTOWN_GREEN : BB_DOWNTOWN_RED, Color.DARK_GRAY, 1 - Math.pow(distance / 2000, .5)));
                            g2d.fillRect(col * TILE_WIDTH + PADDING, row * TILE_HEIGHT + PADDING, TILE_WIDTH - 2 * PADDING, TILE_HEIGHT - 2 * PADDING);
                            for (Point ip : interests) {
                                int xi = ip.x + col * intensityIndex;
                                int yi = ip.y + row * intensityIndex;
                                Stock stock = stocks.get(didx * TILES_ACROSS * TILES_HIGH + xi * TILES_HIGH + yi);
                                paintStock(xi, yi, g2d, stock, pv, distance, mouseX, mouseY, mouseZ);
                            }
                        }

                    }
                } catch (Exception ex) {
                    System.out.println(didx + " " + row + " " + col);
                    ex.printStackTrace();
                }
            }
        }
    }
}
