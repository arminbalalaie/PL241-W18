package me.arminb.hws.pl241.symbol;

import java.util.HashMap;

public class MemoryAllocator {
    private static final int WORD_SIZE = 4;
    private static MemoryAllocator instance;

    private Integer globalWordCounter;
    private HashMap<String, Integer> localWordCounter;
    private HashMap<String, Integer> localParamWordCounter;

    public MemoryAllocator() {
        this.globalWordCounter = 0;
        this.localWordCounter = new HashMap<>();
        localParamWordCounter = new HashMap<>();
    }

    public static MemoryAllocator getInstance() {
        if (instance == null) {
            instance = new MemoryAllocator();
        }
        return instance;
    }

    public void allocate(Symbol symbol) {
        if (symbol.isGlobal()) {
            symbol.setRelativeBaseAddress(globalWordCounter * WORD_SIZE);
            globalWordCounter++;
        } else {
            // TODO think about the stack frame and you want to manage this
            HashMap<String, Integer> hashMapToUse;
            if (symbol.isParam()) {
                hashMapToUse = localParamWordCounter;
            } else {
                hashMapToUse = localWordCounter;
            }
            if (!hashMapToUse.containsKey(symbol.getScope())) {
                hashMapToUse.put(symbol.getScope(), 0);
            }
            Integer currentLocalCounter = hashMapToUse.get(symbol.getScope());
            symbol.setRelativeBaseAddress(currentLocalCounter);
            hashMapToUse.put(symbol.getScope(), currentLocalCounter + 1);
        }
    }
}
