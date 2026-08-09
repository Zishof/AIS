package ais.common.newui.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ringkasan data hierarchy yang tidak sehat; hanya boleh dirender untuk admin. */
public class NewUiHybridMenuDiagnostics implements Serializable {

    private static final long serialVersionUID = 1L;

    private int duplicateCount;
    private int duplicateChildGroupCount;
    private int orphanCount;
    private int cycleCount;
    private int depthLimitCount;
    private final List<String> warnings = new ArrayList<String>();

    public void duplicate(Long id) { duplicateCount++; warn("Duplicate menu id: " + id); }
    public void duplicateChildGroup(Long child) { duplicateChildGroupCount++; warn("Duplicate parent child group: " + child); }
    public void orphan(Long id, Long root) { orphanCount++; warn("Orphan assignment menu=" + id + ", root=" + root); }
    public void cycle(Long id) { cycleCount++; warn("Cycle menu id: " + id); }
    public void depthLimit(Long id) { depthLimitCount++; warn("Depth limit menu id: " + id); }
    public void warn(String value) { if (value != null && warnings.size() < 100) warnings.add(value); }

    public int getDuplicateCount() { return duplicateCount; }
    public int getDuplicateChildGroupCount() { return duplicateChildGroupCount; }
    public int getOrphanCount() { return orphanCount; }
    public int getCycleCount() { return cycleCount; }
    public int getDepthLimitCount() { return depthLimitCount; }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    public boolean hasCriticalWarnings() { return duplicateCount > 0 || cycleCount > 0 || depthLimitCount > 0; }

    public String summary() {
        return "duplicate=" + duplicateCount + ", duplicateChildGroup=" + duplicateChildGroupCount
                + ", orphan=" + orphanCount + ", cycle=" + cycleCount + ", depth=" + depthLimitCount;
    }
}
