package me.kptmusztarda.lights;

import java.io.StringWriter;

public class Bulbs {

    private int status[];
    private int WAIT = 75;

    Bulbs(int bulbsCount) {
        status = new int[bulbsCount];
    }

    protected void setStatus(String str) {
        for(int i=0; i<status.length; i++) {
            status[i] = Integer.parseInt(Character.toString(str.charAt(i*2+1)));
        }
    }

    protected String getSwitchOneString(int ind) {
        return String.format("S%d%d", ind, Math.abs(status[ind] - 1));
    }

    protected String getSwitchAllString() {
        boolean at_least_one_on = true;
        for (int stat : status) {
            if (stat == 0) at_least_one_on = false;
        }
        int val = at_least_one_on ? 0 : 1;

        StringWriter writer = new StringWriter();
        for(int i=0; i<status.length; i++) {
            writer.write(String.format("S%d%d", i, val));
            if(i < status.length - 1) writer.write(String.format(",W%d,", WAIT));
        }

        return writer.toString();
    }

}
