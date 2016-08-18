/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pvjava;

/**
 *
 * @author Sasa
 */
public class Stock {

    private final String ticker;
    private final String name;
    private final int index;
    private volatile double price;
    private volatile double volume;
    private volatile double change;
    private volatile long changeTimestamp;

    public Stock(int index, String ticker, String name, double price, double volume) {
        this.index = index;
        this.ticker = ticker;
        this.name = name;
        this.price = price;
        this.volume = volume;
        change = Math.random() * 2 - 1;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public double getVolume() {
        return volume;
    }

    public double getChange() {
        return change;
    }

    public long getChangeTimestamp() {
        return changeTimestamp;
    }

    public int getIndex() {
        return index;
    }

    public void update() {
        changeTimestamp = System.currentTimeMillis();
        change += Math.random() * 2 - 1;
        change *= .9;
        price *= 1 + change * .001;
        volume *= 1 + .001 * Math.random();
    }

}
