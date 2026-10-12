package dev.msmr;

/** Byte-weighted token bucket: normal edits burst freely; repeated large packets are bounded. */
final class RequestBudget {
    private double tokens=64;
    private long updated;
    private long warned;
    RequestBudget(long now) { updated=now; }
    boolean allow(int bytes,long now) {
        tokens=Math.min(64,tokens+Math.max(0,now-updated)/1_000_000_000.0*16); updated=now;
        int cost=1+Math.max(0,bytes)/4096;
        if(tokens<cost) return false;
        tokens-=cost; return true;
    }
    boolean warn(long now) { if(now-warned<10_000_000_000L) return false; warned=now; return true; }
}
