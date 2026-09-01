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
 * Composer ZK untuk halaman verifikasi keaslian dokumen cetak (mis. transkrip/ijazah/surat) lewat
 * kode barcode yang tercetak di dokumen. Pengguna memasukkan/scan {@link #barcode}, lalu
 * {@link #onCari()} mencari record {@link MetaReport} (metadata dokumen yang disimpan saat dokumen
 * tersebut pertama kali dicetak/di-generate — nama, NIM, fakultas, prodi, IPK, yudisium, jumlah
 * matakuliah, penanda tangan, tanggal cetak, dan jenis report) dengan barcode yang cocok persis.
 * Bila ditemukan, seluruh baris info ditampilkan sehingga pemeriksa dapat membandingkan data pada
 * layar dengan data yang tercetak di dokumen fisik untuk mendeteksi pemalsuan/perubahan.
 *
 * <p><b>Kuirk:</b> tombol reset password ({@code reset}, terkait field {@code barcode} yang sama)
 * sepenuhnya dikomentari — sisa kode lama yang tidak aktif, bukan bagian dari alur cek meta report
 * saat ini; jangan diaktifkan ulang tanpa meninjau relevansinya. Composer ini juga memanggil
 * {@link FilterLanjutHelper#setup(Component)} untuk menyiapkan filter lanjutan standar halaman.</p>
 *
 * @see GenericAutowireComposer
 * @see MetaReport
 */
public class CekMetaReportHelper extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6947829244115144706L;

	/** Baris nama pemilik dokumen; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowNama;
	/** Baris NIM pemilik dokumen; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowNim;
	/** Baris fakultas; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowFakultas;
	/** Baris program studi; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowProdi;
	/** Baris IPK yang tercatat pada dokumen; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowIpk;
	/** Baris predikat yudisium; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowYudisium;
	/** Baris jumlah matakuliah yang tercatat pada dokumen; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowJumlahMk;
	/** Baris nama penanda tangan dokumen; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowPenandaTangan;
	/** Baris tanggal cetak dokumen; disembunyikan sampai hasil pencarian ditemukan. */
	private Row rowTglCetak;
	/** Baris jenis report/dokumen (mis. transkrip, ijazah); disembunyikan sampai hasil pencarian ditemukan. */
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
	/** Input barcode yang di-scan/diketik dari dokumen fisik; sumber pencarian di {@link #onCari()}. */
	private Textbox barcode;
	// MyButtonConfig reset;
	/** Tombol pemicu {@link #onCari()} (di-wire otomatis oleh ZK berdasarkan id komponen). */
	MyButtonConfig cari;
	/** Hasil pencarian {@link MetaReport} terakhir; null bila belum dicari atau barcode tidak ditemukan. */
	private MetaReport metaReport;

	//

	/**
	 * Hook keamanan ZK: memaksa {@code Common.doCheckSecurity()} sebelum komponen di-compose,
	 * sehingga halaman ini hanya bisa diakses oleh session yang sudah lolos pemeriksaan keamanan
	 * standar aplikasi.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}
	/**
	 * Inisialisasi setelah komponen ZK selesai dibangun: menyiapkan bahasa UI
	 * ({@code Common.initLaguage()}), memvalidasi session login dan privilese
	 * {@link CommonPrivilages#READ} — bila salah satu tidak terpenuhi, session dipaksa logoff
	 * ({@code Common.goLogoff()}) dan compose dihentikan — lalu menyiapkan filter lanjutan halaman
	 * via {@link FilterLanjutHelper#setup(Component)}.
	 *
	 * @param comp root komponen halaman ini.
	 * @throws Exception diteruskan dari {@code super.doAfterCompose}.
	 */
	public void doAfterCompose(Component comp) throws Exception {
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

	/**
	 * Menangani klik tombol {@link #cari}: memvalidasi bahwa {@link #barcode} tidak kosong, lalu
	 * mencari satu {@link MetaReport} dengan barcode yang cocok persis (case-insensitive,
	 * {@link MatchMode#EXACT}). Bila tidak ditemukan, tampilkan pesan peringatan dan hentikan. Bila
	 * ditemukan, tampilkan (set visible) seluruh baris info dokumen dan isi label-nya dari field
	 * {@link MetaReport} yang bersangkutan (nama, NIM, fakultas, prodi, IPK, yudisium, jumlah
	 * matakuliah, penanda tangan, tanggal cetak, jenis report) — murni operasi baca, tidak mengubah
	 * data di database.
	 *
	 * @throws Exception diteruskan dari operasi Hibernate/ZK di dalamnya.
	 */
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
