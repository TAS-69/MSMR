package dev.msmr;

import java.util.*;

/** Requests identify an open menu, never an arbitrary world position or supplied item stack. */
public final class StorageProtocol {
    private StorageProtocol() {}
    public record Get(int menu) {}
    public record State(int menu,long token,List<Long> versions,List<ChestProfile> profiles,boolean editable,String message) {
        public State { versions=List.copyOf(versions); profiles=List.copyOf(profiles); if(profiles.size()>2 || versions.size()!=profiles.size()) throw new IllegalArgumentException("Invalid chest state"); }
    }
    public record Edit(int menu,long token,List<Long> versions,List<ChestProfile> profiles) {
        public Edit { versions=List.copyOf(versions); profiles=List.copyOf(profiles); if(profiles.isEmpty() || profiles.size()>2 || versions.size()!=profiles.size()) throw new IllegalArgumentException("Invalid chest edit"); }
    }
    public record Action(int menu,int state,long rules,long chestToken,String operation,int source) {
        public Action { if(!Set.of("sort","to_chest","from_chest","reserved","quick_move").contains(operation)) throw new IllegalArgumentException("Invalid operation"); }
    }
    public record Result(boolean success,String message) {}
}
