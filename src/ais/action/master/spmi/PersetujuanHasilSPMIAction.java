package ais.action.master.spmi;

/**
 * Spesialisasi {@link HasilSPMIAction} untuk layar persetujuan hasil SPMI (Sistem Penjaminan Mutu
 * Internal), dengan mode "persetujuan" aktif diteruskan ke superclass ({@code approve=true}).
 */
public class PersetujuanHasilSPMIAction extends HasilSPMIAction {

    private static final long serialVersionUID = 2948747832889865543L;

    /** Membuat layar dalam mode persetujuan (delegasi ke {@link HasilSPMIAction#HasilSPMIAction(boolean)} dengan {@code true}). */
    public PersetujuanHasilSPMIAction() {
        super(true);
    }

    /** @return label tampilan layar ini, {@code "Persetujuan SPMI"}. */
    @Override
    public String istilah() throws Exception {
        return "Persetujuan SPMI";
    }
}
