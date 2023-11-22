package net.minecraft.src;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class StatBase {
    private static final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);
    private static final DecimalFormat decimalFormat = new DecimalFormat("########0.00");
    public static IStatType simpleStatType = new StatTypeSimple();
    public static IStatType timeStatType = new StatTypeTime();
    public static IStatType distanceStatType = new StatTypeDistance();
    public final int statId;
    private final String statName;
    private final IStatType type;
    public boolean isIndependent = false;
    public String statGuid;

    public StatBase(int i, String string, IStatType iStatType) {
        this.statId = i;
        this.statName = string;
        this.type = iStatType;
    }

    public StatBase(int i, String string) {
        this(i, string, simpleStatType);
    }

    public StatBase initIndependentStat() {
        this.isIndependent = true;
        return this;
    }

    public StatBase registerStat() {
        if (StatList.oneShotStats.containsKey(this.statId)) {
            throw new RuntimeException(
                "Duplicate stat id: \"" + ((StatBase) StatList.oneShotStats.get(this.statId)).statName + "\" and \"" + this.statName + "\" at id " + this.statId
            );
        } else {
            StatList.allStats.add(this);
            StatList.oneShotStats.put(this.statId, this);
            this.statGuid = AchievementMap.getGuid(this.statId);
            return this;
        }
    }

    @Override
    public String toString() {
        return StatCollector.translateToLocal(this.statName);
    }

    public String getName() {
        return statName;
    }
}
