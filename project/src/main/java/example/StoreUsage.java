package example;

import service.NormalStore;

import java.io.File;

public class StoreUsage {
    public static void main(String[] args) {
        String dataDir="data"+ File.separator;
        NormalStore store = new NormalStore(dataDir);
        store.set("xjc1","1");
        store.set("xjc2","2");
        store.set("xjc3","3");
        store.set("xjc4","你好");
        System.out.println(store.get("xjc1"));
        store.rm("xjc1");
        System.out.println(store.get("xjc1"));
    }
}
