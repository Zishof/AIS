package ais.action.master.surat;

public class ArsipKeluarAction extends SuratKeluarAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	public ArsipKeluarAction() {
		super("arsip");
	}

	@Override
	public String istilah() throws Exception {
		return "Arsip Keluar";
	}
}
