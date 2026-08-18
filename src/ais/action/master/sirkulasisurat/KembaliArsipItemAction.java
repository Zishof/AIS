package ais.action.master.sirkulasisurat;

public class KembaliArsipItemAction extends KembaliSuratItemAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	private static String tipe = "arsip";

	public KembaliArsipItemAction() {
		super(tipe);
	}

	@Override
	public String istilah() throws Exception {
		return "Pengembalian Arsip";
	}
}
