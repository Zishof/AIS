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
    private int tanpaPrivilageCount;
    private final List<String> warnings = new ArrayList<String>();

    public void duplicate(Long id) { duplicateCount++; warn("Duplicate menu id: " + id); }
    public void duplicateChildGroup(Long child) { duplicateChildGroupCount++; warn("Duplicate parent child group: " + child); }
    public void orphan(Long id, Long root) { orphanCount++; warn("Orphan assignment menu=" + id + ", root=" + root); }
    public void cycle(Long id) { cycleCount++; warn("Cycle menu id: " + id); }
    public void depthLimit(Long id) { depthLimitCount++; warn("Depth limit menu id: " + id); }

    /**
     * Menu yang ditugaskan ke role tetapi tidak punya baris RolePrivilage.
     *
     * <p>Keadaan ini tidak merusak hierarki, sehingga tidak tertangkap
     * pemeriksaan lain — tetapi akibatnya nyata: menunya muncul pada daftar hak
     * akses, lalu ditolak New UI dengan {@code FORBIDDEN} tanpa keterangan,
     * karena izinnya jatuh ke {@code NewUiPermission.none()}. Tanpa penghitung
     * ini, satu-satunya gejala yang terlihat adalah menu yang "kadang tidak bisa
     * dibuka", dan tidak ada tempat untuk menelusurinya.</p>
     *
     * <p>Ini hanya catatan. Tidak ada izin yang diberikan dan tidak ada jawaban
     * yang berubah: memberi akses karena data terlihat tidak lengkap justru
     * membalik arah fail-closed.</p>
     */
    public void tanpaPrivilage(Long id) {
        tanpaPrivilageCount++;
        warn("Menu ditugaskan tanpa RolePrivilage: " + id);
    }
    public void warn(String value) { if (value != null && warnings.size() < 100) warnings.add(value); }

    public int getDuplicateCount() { return duplicateCount; }
    public int getDuplicateChildGroupCount() { return duplicateChildGroupCount; }
    public int getOrphanCount() { return orphanCount; }
    public int getCycleCount() { return cycleCount; }
    public int getDepthLimitCount() { return depthLimitCount; }
    public int getTanpaPrivilageCount() { return tanpaPrivilageCount; }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    public boolean hasCriticalWarnings() { return duplicateCount > 0 || cycleCount > 0 || depthLimitCount > 0; }

    public String summary() {
        return "duplicate=" + duplicateCount + ", duplicateChildGroup=" + duplicateChildGroupCount
                + ", orphan=" + orphanCount + ", cycle=" + cycleCount + ", depth=" + depthLimitCount
                + ", tanpaPrivilage=" + tanpaPrivilageCount;
    }
}
