package ais.action.master.payroll;

public class IzinAction extends CutiDanIzinAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	public IzinAction() {
		super(false, "Izin");
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Izin Pegawai";
	}
}
