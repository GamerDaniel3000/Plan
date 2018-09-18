/*
 * License is provided in the jar as LICENSE also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/LICENSE
 */
package com.djrapitops.plan.data.store.mutators.health;

import com.djrapitops.plan.data.store.Key;
import com.djrapitops.plan.data.store.containers.AnalysisContainer;
import com.djrapitops.plan.data.store.keys.AnalysisKeys;
import com.djrapitops.plan.data.store.mutators.PlayersMutator;
import com.djrapitops.plan.data.store.mutators.PlayersOnlineResolver;
import com.djrapitops.plan.data.store.mutators.TPSMutator;
import com.djrapitops.plan.utilities.html.icon.Icons;
import com.djrapitops.plugin.api.TimeAmount;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Server Health analysis mutator.
 *
 * @author Rsl1122
 */
public class HealthInformation extends AbstractHealthInfo {

    private final AnalysisContainer analysisContainer;

    public HealthInformation(AnalysisContainer analysisContainer) {
        super(
                analysisContainer.getUnsafe(AnalysisKeys.ANALYSIS_TIME),
                analysisContainer.getUnsafe(AnalysisKeys.ANALYSIS_TIME_MONTH_AGO)
        );
        this.analysisContainer = analysisContainer;
        calculate();
    }

    @Override
    public String toHtml() {
        StringBuilder healthNoteBuilder = new StringBuilder();
        for (String healthNote : notes) {
            healthNoteBuilder.append(healthNote);
        }
        return healthNoteBuilder.toString();
    }

    @Override
    protected void calculate() {
        activityChangeNote(analysisContainer.getUnsafe(AnalysisKeys.ACTIVITY_DATA));
        newPlayerNote();
        activePlayerPlaytimeChange(analysisContainer.getUnsafe(AnalysisKeys.PLAYERS_MUTATOR));
        lowPerformance();
    }

    private void newPlayerNote() {
        Key<PlayersMutator> newMonth = new Key<>(PlayersMutator.class, "NEW_MONTH");
        PlayersMutator newPlayersMonth = analysisContainer.getValue(newMonth).orElse(new PlayersMutator(new ArrayList<>()));
        PlayersOnlineResolver onlineResolver = analysisContainer.getUnsafe(AnalysisKeys.PLAYERS_ONLINE_RESOLVER);

        double avgOnlineOnRegister = newPlayersMonth.registerDates().stream()
                .map(onlineResolver::getOnlineOn)
                .filter(Optional::isPresent)
                .mapToInt(Optional::get)
                .average().orElse(0);
        if (avgOnlineOnRegister >= 1) {
            addNote(Icons.GREEN_THUMB + " New Players have players to play with when they join ("
                    + decimalFormatter.apply(avgOnlineOnRegister) + " on average)");
        } else {
            addNote(Icons.YELLOW_FLAG + " New Players may not have players to play with when they join ("
                    + decimalFormatter.apply(avgOnlineOnRegister) + " on average)");
            serverHealth -= 5;
        }

        long playersNewMonth = analysisContainer.getValue(AnalysisKeys.PLAYERS_NEW_MONTH).orElse(0);
        long playersRetainedMonth = analysisContainer.getValue(AnalysisKeys.PLAYERS_RETAINED_MONTH).orElse(0);

        if (playersNewMonth != 0) {
            double retainPercentage = playersRetainedMonth * 1.0 / playersNewMonth;
            if (retainPercentage >= 0.25) {
                addNote(Icons.GREEN_THUMB + " " + percentageFormatter.apply(retainPercentage)
                        + " of new players have stuck around (" + playersRetainedMonth + "/" + playersNewMonth + ")");
            } else {
                addNote(Icons.YELLOW_FLAG + " " + percentageFormatter.apply(retainPercentage)
                        + " of new players have stuck around (" + playersRetainedMonth + "/" + playersNewMonth + ")");
            }
        }
    }

    private void lowPerformance() {
        Key<TPSMutator> tpsMonth = new Key<>(TPSMutator.class, "TPS_MONTH");
        TPSMutator tpsMutator = analysisContainer.getUnsafe(tpsMonth);
        long serverDownTime = tpsMutator.serverDownTime();

        int threshold = 5; // TODO TPS THRESHOLD from settings

        double aboveThreshold = tpsMutator.percentageTPSAboveThreshold(threshold);
        long tpsSpikeMonth = analysisContainer.getValue(AnalysisKeys.TPS_SPIKE_MONTH).orElse(0);

        String avgLowThresholdString = "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
        if (aboveThreshold >= 0.96) {
            avgLowThresholdString += Icons.GREEN_THUMB;
        } else if (aboveThreshold >= 0.9) {
            avgLowThresholdString += Icons.YELLOW_FLAG;
            serverHealth *= 0.9;
        } else {
            avgLowThresholdString += Icons.RED_WARN;
            serverHealth *= 0.6;
        }
        avgLowThresholdString += " Average TPS was above Low Threshold "
                + decimalFormatter.apply(aboveThreshold * 100.0) + "% of the time";

        if (tpsSpikeMonth <= 5) {
            addNote(Icons.GREEN_THUMB + " Average TPS dropped below Low Threshold (" + threshold + ")" +
                    " " + tpsSpikeMonth + " times" +
                    avgLowThresholdString);
        } else if (tpsSpikeMonth <= 25) {
            addNote(Icons.YELLOW_FLAG + " Average TPS dropped below Low Threshold (" + threshold + ")" +
                    " " + tpsSpikeMonth + " times" +
                    avgLowThresholdString);
            serverHealth *= 0.95;
        } else {
            addNote(Icons.RED_WARN + " Average TPS dropped below Low Threshold (" + threshold + ")" +
                    " " + tpsSpikeMonth + " times" +
                    avgLowThresholdString);
            serverHealth *= 0.8;
        }

        if (serverDownTime <= TimeUnit.DAYS.toMillis(1L)) {
            addNote(Icons.GREEN_THUMB + " Total Server downtime (No Data) was " + timeAmountFormatter.apply(serverDownTime));
        } else {
            long weekMs = TimeAmount.WEEK.toMillis(1L);
            if (serverDownTime <= weekMs) {
                addNote(Icons.YELLOW_FLAG + " Total Server downtime (No Data) was " + timeAmountFormatter.apply(serverDownTime));
                serverHealth *= (weekMs - serverDownTime) * 1.0 / weekMs;
            } else {
                addNote(Icons.RED_WARN + " Total Server downtime (No Data) was " + timeAmountFormatter.apply(serverDownTime));
                long monthMs = TimeAmount.MONTH.toMillis(1L);
                serverHealth *= (monthMs - serverDownTime) * 1.0 / monthMs;
            }
        }
    }

}