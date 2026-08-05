package ais.action.master.surat;

public class ArsipMasukAction extends SuratMasukAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	public ArsipMasukAction() {
		super("arsip");
	}

	@Override
	public String istilah() throws Exception {
		return "Arsip Masuk";
	}
}
