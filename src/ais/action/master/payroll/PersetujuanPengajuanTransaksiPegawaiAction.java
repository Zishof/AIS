package ais.action.master.payroll;

public class PersetujuanPengajuanTransaksiPegawaiAction extends PengajuanTransaksiPegawaiAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	public PersetujuanPengajuanTransaksiPegawaiAction() {
		super(true);
	}

	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pengajuan Transaksi Pegawai";
	}
}
