package ais.action.master.koperasi;

public class PersetujuanTransaksiKoperasiAction extends TransaksiKoperasiAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	public PersetujuanTransaksiKoperasiAction() {
		super(true);
	}

	@Override
	public String istilah() throws Exception {
		return "Persetujuan Transaksi Koperasi";
	}
}
