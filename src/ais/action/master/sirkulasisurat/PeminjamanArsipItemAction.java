package ais.action.master.sirkulasisurat;

public class PeminjamanArsipItemAction extends PeminjamanSuratItemAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	private static String tipe = "arsip";

	public PeminjamanArsipItemAction() {
		super(tipe);
	}

	@Override
	public String istilah() throws Exception {
		return "Peminjaman Arsip";
	}
}
