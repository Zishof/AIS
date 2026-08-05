package ais.action.master.spmi;

public class PersetujuanHasilSPMIAction extends HasilSPMIAction {

    private static final long serialVersionUID = 2948747832889865543L;

    public PersetujuanHasilSPMIAction() {
        super(true);
    }

    @Override
    public String istilah() throws Exception {
        return "Persetujuan SPMI";
    }
}
