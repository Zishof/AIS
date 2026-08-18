package ais.action.master.helper;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyButtonConfig;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.MetaReport;
import ais.action.master.helper.FilterLanjutHelper;

public class CekMetaReportHelper extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6947829244115144706L;

	private Row rowNama;
	private Row rowNim;
	private Row rowFakultas;
	private Row rowProdi;
	private Row rowIpk;
	private Row rowYudisium;
	private Row rowJumlahMk;
	private Row rowPenandaTangan;
	private Row rowTglCetak;
	private Row rowJenisReport;
	// private Row rowButton;
	private Label labelNama;
	private Label labelNim;
	private Label labelFakultas;
	private Label labelProdi;
	private Label labelJumlahMk;
	private Label labelIpk;
	private Label labelYudisium;
	private Label labelPenandaTangan;
	private Label labelTglCetak;
	private Label labelJenisReport;
	private Textbox barcode;
	// MyButtonConfig reset;
	MyButtonConfig cari;
	private MetaReport metaReport;

	// 

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		// reset.setDisabled(true);

		// reset.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event event) throws Exception {
		// // TODO Auto-generated method stub
		//
		// if (onReset()) {
		// String pesan;
		// if (tbmuser != null) {
		// pesan = tbmuser.getUserId();
		// } else {
		// pesan = mahasiswa.getNim();
		// }
		// MyMessageboxConfig.show("Password untuk User ID : "
		// + barcode.getValue() + " telah diset menjadi : "
		// + pesan);
		// System.out.println("Password untuk User ID : "
		// + barcode.getValue() + " telah diset menjadi : "
		// + pesan);
		//
		// reset.setDisabled(true);
		//
		// }
		//
		// }
		// });

	        FilterLanjutHelper.setup(comp);
}

	public void onCari() throws Exception {
		if (barcode.getValue().equals("")) {
			MyMessageboxConfig.show("Masukkan Barcode", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		metaReport = (MetaReport) session
				.createCriteria(MetaReport.class)
				.add(Restrictions.ilike("barcode", barcode.getValue().trim(),
						MatchMode.EXACT)).uniqueResult();

		if (metaReport == null) {
			MyMessageboxConfig.show("Barcode tidak ditemukan / tidak valid");
			return;

		} else {
			rowNama.setVisible(true);
			labelNama.setValue(metaReport.getNama());
			rowNim.setVisible(true);
			labelNim.setValue(metaReport.getNim());
			rowFakultas.setVisible(true);
			labelFakultas.setValue(metaReport.getFakultas());
			rowProdi.setVisible(true);
			labelProdi.setValue(metaReport.getProdi());
			rowIpk.setVisible(true);
			labelIpk.setValue(metaReport.getIpk());
			rowYudisium.setVisible(true);
			labelYudisium.setValue(metaReport.getYudisium());
			rowJumlahMk.setVisible(true);
			labelJumlahMk.setValue(metaReport.getJumlahMk());
			rowPenandaTangan.setVisible(true);
			labelPenandaTangan.setValue(metaReport.getPenandaTangan());
			rowTglCetak.setVisible(true);
			labelTglCetak.setValue(metaReport.getTanggalCetak());
			rowJenisReport.setVisible(true);
			labelJenisReport.setValue(metaReport.getJenis_report());
		}

	}

}
