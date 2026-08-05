package ais.action.master.spi;

/**
 * Companion class of {@link PenugasanAuditSPIAction} instantiated by the SOP/Disposisi engine
 * (via reflection, {@code Class.forName(alurSop.getFormInputan()).newInstance()}) at the approval
 * step of the workflow — sets {@code persetujuan=true} so the shared {@link PenugasanAuditSPIAction#form}
 * renders in approver mode (read-only fields plus the Setuju/Tolak radiogroup). Requires the
 * no-arg constructor for reflective instantiation.
 *
 * @author e-Campus SPI Team
 */
public class PersetujuanPenugasanAuditSPIAction extends PenugasanAuditSPIAction {

    private static final long serialVersionUID = 1L;

    public PersetujuanPenugasanAuditSPIAction() {
        super(true);
    }

    @Override
    public String istilah() throws Exception {
        return "Persetujuan Audit SPI";
    }
}
