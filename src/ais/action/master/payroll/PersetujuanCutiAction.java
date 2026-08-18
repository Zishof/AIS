package ais.action.master.payroll;

public class PersetujuanCutiAction extends CutiDanIzinAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	public PersetujuanCutiAction() {
		super(true, "Cuti");
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Persetujuan Cuti Pegawai";
	}
}
