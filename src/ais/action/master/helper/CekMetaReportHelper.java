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

/**
 * Helper terfokus untuk cek meta report. Tipe ini membungkus satu variasi kecil dari alur yang
 * lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Row rowNama}, {@code Row rowNim},
 * {@code Row rowFakultas}, {@code Row rowProdi}, {@code Row rowIpk}, {@code Row rowYudisium}, {@code Row
 * rowJumlahMk}, {@code Row rowPenandaTangan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}); pembacaan/pencarian ({@code onCari()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
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
