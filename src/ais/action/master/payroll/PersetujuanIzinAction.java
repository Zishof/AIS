package ais.action.master.payroll;

public class PersetujuanIzinAction extends CutiDanIzinAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	public PersetujuanIzinAction() {
		super(true, "Izin");
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Persetujuan Izin Pegawai";
	}
}
