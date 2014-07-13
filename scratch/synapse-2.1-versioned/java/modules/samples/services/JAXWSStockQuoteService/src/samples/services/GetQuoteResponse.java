/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package samples.services;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="GetQuoteResponse")
public class GetQuoteResponse {
    private double change;
    private double earnings;
    private double high;
    private double last;
    private String lastTradeTimestamp;
    private double low;
    private double marketCap;
    private String name;
    private double open;
    private double peRatio;
    private double percentageChange;
    private double prevClose;
    private String symbol;
    private int volume;

    public double getChange() {
        return change;
    }

    public void setChange(double value) {
        this.change = value;
    }

    public double getEarnings() {
        return earnings;
    }

    public void setEarnings(double value) {
        this.earnings = value;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double value) {
        this.high = value;
    }

    public double getLast() {
        return last;
    }

    public void setLast(double value) {
        this.last = value;
    }

    @XmlElement(required=true)
    public String getLastTradeTimestamp() {
        return lastTradeTimestamp;
    }

    public void setLastTradeTimestamp(String value) {
        this.lastTradeTimestamp = value;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double value) {
        this.low = value;
    }

    public double getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(double value) {
        this.marketCap = value;
    }

    @XmlElement(required=true)
    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double value) {
        this.open = value;
    }

    public double getPeRatio() {
        return peRatio;
    }

    public void setPeRatio(double value) {
        this.peRatio = value;
    }

    public double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(double value) {
        this.percentageChange = value;
    }

    public double getPrevClose() {
        return prevClose;
    }

    public void setPrevClose(double value) {
        this.prevClose = value;
    }

    @XmlElement(required=true)
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String value) {
        this.symbol = value;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int value) {
        this.volume = value;
    }
}
