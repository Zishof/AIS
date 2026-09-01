package ais.action.master.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.MahasiswaAction;
import ais.action.master.MonitorKRSMahasiswaAction;
import ais.action.master.PerkuliahanAction;
import ais.action.master.SyaratUjianAction;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.util.PenilaianUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanKurikulumMahasiswa;
import ais.common.Common;
import ais.common.CommonPenilaian;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Komentar;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper layar inti "Rencana Studi Mahasiswa" (KRS — Kartu Rencana Studi): menampilkan dan
 * mengelola seluruh mata kuliah yang diambil satu {@link Mahasiswa} pada satu kombinasi
 * semester/tahap/tahun akademik (termasuk mode khusus semester {@code 0} untuk konversi nilai
 * dari transfer/pindahan, dan mode semester pendek/remedial). Ini adalah salah satu layar
 * terpadat dan paling sering dipakai di AIS — satu baris grid mewakili satu
 * {@link Detailperkuliahan} (mata kuliah yang diambil), dengan kolom yang berubah tampilan
 * tergantung konteks (semester biasa vs. konversi vs. tahap khusus): nilai per komponen
 * ({@link FormatNilai}), nilai huruf (diwarnai lulus/tidak lulus via
 * {@code WarnaStatusLulusUtil}), status persetujuan KRS, jadwal, dosen, dan aksi (pindah data,
 * hapus, kirim ke Feeder/PDDikti).
 *
 * <p>
 * <b>Alur persetujuan KRS</b> — inti bisnis layar ini. Sebelum persetujuan diberikan (per baris
 * lewat {@link #lakukanSatuPersetujuan} atau seluruhnya lewat {@link #lakukanSemuaPersetujuan}),
 * sistem memvalidasi berturut-turut: status pembayaran semester berjalan (bila
 * {@code mahasiswa_harus_bayar_sebelum_persetujuan_krs} aktif), status pembayaran semester
 * sebelumnya (ambang batas persentase dari konfigurasi), dan batas SKS berdasarkan IP
 * ({@link #apakahMelebihiSks}, memakai {@link Common#checkPembatasanSKSBerdasarkanIP}) —
 * pelanggaran apa pun membatalkan centang dan menampilkan pesan penjelasan. Begitu SEMUA baris
 * disetujui, jendela {@link CatatanHelper} otomatis terbuka untuk meminta catatan dosen PA.
 * Pembatalan massal ({@link #lakukanPembatalanSemuaPersetujuan}) menolak baris yang sudah
 * memiliki nilai (>1.0) — nilai yang sudah diinput mengunci status disetujui.
 * </p>
 *
 * <p>
 * <b>Toolbar</b> {@link #display} menyediakan sangat banyak aksi kontekstual (sebagian besar
 * hanya untuk staf ber-hak edit, sebagian bergantung konfigurasi/fitur Feeder aktif): Ambilkan
 * Perkuliahan (dosen PA mengambilkan KRS mahasiswa), Komentar, Catatan (dosen PA), Setujui/
 * Batalkan massal, Tambah Konversi (nilai transfer), Paket (KRS paket), cetak KRS/Nilai/UTS/UAS/
 * Keterangan Aktif, unduh/unggah Excel kurikulum, kirim data ke Neo Feeder (PDDikti, termasuk
 * AKM), ambil nilai dari sumber eksternal, dan Refresh. Di bawah toolbar tampil grid detail
 * (dengan info jam bentrok jadwal via {@link Common#generateInformasiJamBentrok}) serta grid
 * komentar diskusi terpisah ({@link #loadDataKomentar}).
 * </p>
 *
 * <p>
 * <b>Integrasi Feeder</b> — beberapa tombol mengirim data mata kuliah/nilai/AKM ke server Neo
 * Feeder (integrasi PDDikti) memakai kredensial yang diambil dari konfigurasi lewat
 * {@code EksporFromFeederAction.koneksi()} (bukan tertanam di kode), dijalankan di thread
 * terpisah dengan log error yang dapat diunduh bila gagal.
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} agar {@link #loadData(Object)} dapat dipakai sebagai
 * callback penyegaran dari helper lain (mis. {@link TransferDataMahasiswaHelper}).
 * </p>
 */
public class StudiMahasiswaHelper implements DataLoader {

	private MyGrid grid;
	private MyGrid gridKomentar;
	private Mahasiswa mahasiswa;
	private MyDiv jamBentrok = new MyDiv();
	private String tahunAjaran;
	private Integer semester;

	private List<Long> detailperkuliahansData;
	private Component component;

	private Html keterangan;
	private Html komentarshtml;
	private Label statusPersetujuan;
	private Label jumlahKRS;
	private Label ipIpk;

	private Integer semesterPendek = null;

	private Boolean tampilHapus = false;
	private Boolean tampilKonversi = false;

	private boolean delete = true;
	private boolean update = true;
	private boolean approve = true;
	private boolean reject = true;
	private Label catatan;
	private Label catatanKhs;

	private Integer tahapan;
	private A sksSksk;
	private boolean remedial;

	private Tbmuser tbmuser = Common.getCurrentUser();
	private Boolean edit = true;

	private Konfigurasi konfigurasiPersetujuanKrsDosen = null;
	private KrsMahasiswa krsMahasiswa;

	/**
	 * Konstruktor ringkas: hak hapus baris ({@code tampilHapus}) otomatis ditentukan dari
	 * konfigurasi {@code admin_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs}
	 * dikombinasikan dengan status admin pengguna saat ini.
	 *
	 * @param semesterPendek konteks semester pendek (nomor SP), {@code null} untuk semester reguler
	 * @param remedial       {@code true} untuk konteks KRS remedial
	 * @param tampilKonversi tampilkan fitur konversi nilai transfer
	 * @param edit           izinkan pengeditan (toolbar & kolom edit tampil)
	 */
	public StudiMahasiswaHelper(Integer semesterPendek, boolean remedial, Boolean tampilKonversi, Boolean edit) {
		this(semesterPendek, remedial,
				(Common.bolehKonfigurasi("admin_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", Konfigurasi.TIDAK_AKTIF) && Common.getApakahAdmin()),
				true, edit);
		this.tampilKonversi = tampilKonversi;
		this.edit = edit;
	}

	/**
	 * Konstruktor lengkap; menentukan hak update/hapus pengguna saat ini lewat
	 * {@link CommonPrivilages}.
	 *
	 * @param semesterPendek konteks semester pendek (nomor SP), {@code null} untuk semester reguler
	 * @param remedial       {@code true} untuk konteks KRS remedial
	 * @param tampilHapus    tampilkan tombol hapus baris secara eksplisit
	 * @param tampilKonversi tampilkan fitur konversi nilai transfer
	 * @param edit           izinkan pengeditan (toolbar & kolom edit tampil)
	 */
	public StudiMahasiswaHelper(Integer semesterPendek, boolean remedial, Boolean tampilHapus, Boolean tampilKonversi, Boolean edit) {
		this.edit = edit;
		this.delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.update = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		this.approve = true;
		this.reject = true;

		this.semesterPendek = semesterPendek;
		this.tampilHapus = tampilHapus;
		this.tampilKonversi = tampilKonversi;
		this.remedial = remedial;
	}

	/** Menambahkan satu baris nama-nilai pada panel rincian nilai per komponen; baris {@code utama} (biasanya "Total") diberi gaya lebih tebal/menonjol. @return label nilai (untuk diperbarui lebih lanjut oleh pemanggil) */
	private Label tambahBarisNilai(Vbox parent, String nama, String nilai, boolean utama) {
		Hbox baris = new Hbox();
		baris.setWidth("100%");
		baris.setStyle("display:flex;align-items:center;justify-content:space-between;gap:6px;"
				+ "line-height:1.45;padding:" + (utama ? "3px 5px" : "1px 2px") + ";"
				+ (utama ? "margin-top:3px;border-top:1px solid #dbe5f1;background:#f8fafc;border-radius:4px;" : ""));
		baris.setParent(parent);

		Label labelNama = new Label(nama == null ? "" : nama);
		labelNama.setStyle("font-size:10px;color:#64748b;width:auto;white-space:nowrap;");
		labelNama.setParent(baris);

		Label labelNilai = new Label(nilai == null ? "" : nilai);
		labelNilai.setStyle("font-size:11px;font-weight:" + (utama ? "bold" : "normal")
				+ ";color:#0f172a;text-align:right;width:auto;white-space:nowrap;");
		labelNilai.setParent(baris);
		return labelNilai;
	}

	/** Menambahkan baris "Huruf" berisi {@code labelNilaiHuruf} (nilai huruf yang sudah ada, mis. dari {@link #rapikanLabelNilaiHuruf}) ke panel rincian nilai; tidak melakukan apa pun bila label {@code null}. */
	private void tambahBarisNilaiHuruf(Vbox parent, Label labelNilaiHuruf) {
		if (labelNilaiHuruf == null) {
			return;
		}
		Hbox baris = new Hbox();
		baris.setWidth("100%");
		baris.setStyle("display:flex;align-items:center;justify-content:space-between;gap:6px;"
				+ "line-height:1.45;padding:3px 5px;margin-top:3px;background:#eef6ff;border:1px solid #dbeafe;border-radius:4px;");
		baris.setParent(parent);

		Label labelNama = new Label("Huruf");
		labelNama.setStyle("font-size:10px;color:#64748b;width:auto;white-space:nowrap;");
		labelNama.setParent(baris);

		rapikanLabelNilaiHuruf(labelNilaiHuruf);
		labelNilaiHuruf.setParent(baris);
	}

	/** Menerapkan gaya visual standar (ukuran, tebal, warna biru tua, rata kanan) pada label nilai huruf; tidak melakukan apa pun bila {@code null}. */
	private void rapikanLabelNilaiHuruf(Label labelNilaiHuruf) {
		if (labelNilaiHuruf == null) {
			return;
		}
		labelNilaiHuruf.setStyle("font-size:11px;font-weight:bold;color:#1e3a8a;text-align:right;width:auto;white-space:nowrap;");
	}

	/**
	 * Merender satu baris {@link Detailperkuliahan} pada grid KRS. Menangani mata kuliah
	 * ekivalen (menampilkan kode/nama asli dalam kurung bila berbeda dari yang tersimpan),
	 * rincian nilai per komponen, nilai huruf berwarna, checkbox persetujuan (auto-save memicu
	 * {@link #lakukanSatuPersetujuan}), field mode konversi (kode/nama/SKS/nilai huruf asal —
	 * hanya pada semester {@code 0}), info jadwal/dosen, dan berbagai tombol aksi (pindah data
	 * KRS, hapus, kirim ke Feeder) yang visibilitasnya bergantung pada konteks (semester
	 * konversi vs. reguler), peran pengguna (dosen/mahasiswa/staf), dan konfigurasi fitur
	 * (integrasi Feeder aktif, hak edit/hapus).
	 */
	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private KrsMahasiswa krsMahasiswa;
		private boolean refresh;
		private Tbmuser user = Common.getCurrentUser();

		/** @param krsMahasiswa KRS induk konteks render; @param refresh bila {@code true}, paksa hitung ulang mata kuliah ekivalen (bukan dari cache) */
		public DetailMahasiswaRenderer(KrsMahasiswa krsMahasiswa, boolean refresh) {
			this.krsMahasiswa = krsMahasiswa;
			this.refresh = refresh;
		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");

			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, data.toString());
			if (detailperkuliahan == null) {
				row.setVisible(false);
				return;
			}

			Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null ? detailperkuliahan.getMatakuliahKonversi() : detailperkuliahan.getPerkuliahan().getMatakuliah();
			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah, mahasiswa == null ? null : mahasiswa.getNim(), refresh);
			
			matakuliah = matakuliahs[0];
			Matakuliah matakuliahAsli = matakuliahs[1];
			
			if (matakuliah == null) {
				row.setVisible(false);
				return;
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			final Vbox totalNilai = new Vbox();
			List<FormatNilai> formatNilais = null;
			
			if (detailperkuliahan.getPerkuliahan() != null) {
				Session session = HibernateUtil.currentSession();
				formatNilais = Common.getFormatNilais(session, detailperkuliahan.getPerkuliahan());
				for (FormatNilai formatNilai : formatNilais) {
					Double nilai = 0.0;
					try {
						nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					Double n = nilai == null ? 0.0 : nilai.doubleValue();
					tambahBarisNilai(totalNilai, formatNilai.getNama(), Common.numberFormat.get().format(n), false);
				}
			}

			totalNilai.setStyle("width:100%;min-width:110px;max-width:150px;");
			final Label totalNilaiMhs = tambahBarisNilai(totalNilai, "Total", Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()), true);
			if (formatNilais == null || formatNilais.isEmpty() || formatNilais.size() > 1) {
				totalNilaiMhs.getParent().setVisible(true);
			} else {
				totalNilaiMhs.getParent().setVisible(false);
			}

			final Label labelNilaiHuruf = new Label(detailperkuliahan.getNilaiHuruf());
			rapikanLabelNilaiHuruf(labelNilaiHuruf);
			ais.action.master.helper.util.WarnaStatusLulusUtil.warnai(labelNilaiHuruf, detailperkuliahan);
			ais.ui.util.NilaiHurufAnalisisPopupHelper.pasangLink(labelNilaiHuruf, detailperkuliahan);
			final MyDoublebox totalNilaiLabel = new MyDoublebox(detailperkuliahan.getTotalNilai());
			totalNilaiLabel.setDisabled(!update);

			if (semester.equals(0)) {
				totalNilaiLabel.setValue(detailperkuliahan.getTotalNilai());

				/**
				 * Event listener lokal milik {@link DetailMahasiswaRenderer}. Kelas ini menangani event untuk komponen induk
				 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
				 *
				 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DetailMahasiswaRenderer} dan dapat mengakses
				 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
				 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
				 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
				 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
				 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
				 * renderer/listener ini.</p>
				 *
				 * @see DetailMahasiswaRenderer
				 */
				class PerubahanNilaiListener implements EventListener {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (totalNilaiLabel.getValue() == null) {
							totalNilaiLabel.setValue(0.0);
						}

						Session session = HibernateUtil.currentSession();
						Common.updateNilaiKonversi(detailperkuliahan, totalNilaiLabel.getValue(), session);

						labelNilaiHuruf.setValue(detailperkuliahan.getNilaiHuruf());
						rapikanLabelNilaiHuruf(labelNilaiHuruf);
						ais.action.master.helper.util.WarnaStatusLulusUtil.warnai(labelNilaiHuruf, detailperkuliahan);
						totalNilaiMhs.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()));

						checkbox.setDisabled(totalNilaiLabel.getValue() > 1.0);
						if (!checkbox.isChecked()) {
							checkbox.setChecked(totalNilaiLabel.getValue() > 1.0);
						}

						// Perubahan nilai masih berada dalam transaksi request saat ini. Jangan
						// membuka transaksi sinkronisasi kedua yang menulis KRS yang sama karena
						// transaksi tersebut akan menunggu kunci miliknya sendiri sampai timeout.
						krsMahasiswa = Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan,
								semesterPendek);

						Double ipmhs = krsMahasiswa.getIps();
						Double ipkmhs = krsMahasiswa.getIpk();

						Integer sksmhss = krsMahasiswa.getSksYangDiambil();
						Integer sksmhs = krsMahasiswa.getSksk();
						String krsStr = mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek, krsMahasiswa, remedial);
						
						keterangan.setContent(krsStr);
						ipIpk.setValue(Common.numberFormat.get().format(ipmhs) + " / " + Common.numberFormat.get().format(ipkmhs));
						sksSksk.setLabel(Common.numberFormat.get().format(sksmhss) + " / " + Common.numberFormat.get().format(sksmhs));
					}
				}
				totalNilaiLabel.addEventListener("onChange", new PerubahanNilaiListener());
			}

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setVisible(detailperkuliahan.getMatakuliahKonversi() != null && tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);

			row.setAttribute("value", detailperkuliahan);

			checkbox.setChecked(Detailperkuliahan.DISETUJUI.equals(detailperkuliahan.getPersetujuan()));
			checkbox.setDisabled(detailperkuliahan.getTotalNilai() > 1.0 || !approve);

			if (!konfigurasiPersetujuanKrsDosen.checkAktif()) {
				new Label(Detailperkuliahan.DISETUJUI.equals(detailperkuliahan.getPersetujuan()) ? "Ya" : "Belum").setParent(row);
			} else {
				checkbox.setParent(row);
				row.setAttribute("checkbox", checkbox);
			}

			checkbox.addEventListener(Events.ON_CHECK, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					lakukanSatuPersetujuan(checkbox, detailperkuliahan, krsMahasiswa, semester);
				}
			});

			String tambahan = "";
			try {
				if (detailperkuliahan.getPerkuliahan() != null && !tahunAjaran.equals(detailperkuliahan.getPerkuliahan().getTahunAjaran())) {
					tambahan = "(ikut kuliah di TA " + detailperkuliahan.getPerkuliahan().getTahunAjaran() + ")";
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			final Textbox kodeMatakuliahAsal = new Textbox(detailperkuliahan.getKodeMatakuliahAsal());
			final Textbox namaMatakuliahAsal = new Textbox(detailperkuliahan.getNamaMatakuliahAsal());
			final Intbox sksAsal = new Intbox(detailperkuliahan.getSksAsal());
			final Textbox nilaiHurufAsal = new Textbox(detailperkuliahan.getNilaiHurufAsal());

			EventListener eventListenerUpdateAsal = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					session.refresh(detailperkuliahan);
					detailperkuliahan.setKodeMatakuliahAsal(kodeMatakuliahAsal.getValue());
					detailperkuliahan.setNamaMatakuliahAsal(namaMatakuliahAsal.getValue());
					detailperkuliahan.setSksAsal(sksAsal.getValue());
					detailperkuliahan.setNilaiHurufAsal(nilaiHurufAsal.getValue());
					session.update(detailperkuliahan);
				}
			};

			kodeMatakuliahAsal.addEventListener("onChange", eventListenerUpdateAsal);
			namaMatakuliahAsal.addEventListener("onChange", eventListenerUpdateAsal);
			sksAsal.addEventListener("onChange", eventListenerUpdateAsal);
			nilaiHurufAsal.addEventListener("onChange", eventListenerUpdateAsal);

			kodeMatakuliahAsal.setWidth("90%"); kodeMatakuliahAsal.setParent(row);
			namaMatakuliahAsal.setWidth("90%"); namaMatakuliahAsal.setParent(row);
			sksAsal.setWidth("90%"); sksAsal.setParent(row);
			nilaiHurufAsal.setWidth("90%"); nilaiHurufAsal.setParent(row);

			Vbox vboxMK = new Vbox();
			vboxMK.setParent(row);
			String revisiLabel = matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode() : (matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")");
			RevisiHelper.createNewRevisi(Detailperkuliahan.class, detailperkuliahan, revisiLabel + " " + tambahan).setParent(vboxMK);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder() && Konfigurasi.AKTIF.equals(Common.getKonfigurasi("aktifkan_terhubung_langsung_ke_feeder", Konfigurasi.AKTIF).getNilai())) {
				if (detailperkuliahan.getFeeder() != null && !detailperkuliahan.getFeeder().trim().isEmpty()) {
					vboxMK.appendChild(new Image("/img/svg/check2-circle.svg"));
					vboxMK.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					vboxMK.appendChild(new Image("/img/svg/warning-outline.svg"));
					vboxMK.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}

			Vbox vboxNama = new Vbox();
			vboxNama.setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama() : (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")")).setParent(vboxNama);

			if (detailperkuliahan.getPerkuliahan() != null && detailperkuliahan.getPerkuliahan().getKurikulum() != null) {
				new Label("Kurikulum : " + detailperkuliahan.getPerkuliahan().getKurikulum().getNama()).setParent(vboxNama);
			}

			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "") : (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")).setParent(row);

			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, true);
			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(row, perkuliahan, detailperkuliahan);

			if ((user.ambilDosen() != null || user.getMahasiswa() != null) && edit) {
				try {
					if (detailperkuliahan.getSemester().equals(perkuliahan.getSemester())) {
						new Label(detailperkuliahan.getSemester() + "").setParent(row);
					} else {
						new MyLabelKecil((detailperkuliahan.getSemester() + " / " + perkuliahan.getSemester() + (detailperkuliahan.getSemester() > perkuliahan.getSemester() ? " (Mengulang)" : " (Menabung)"))).setParent(row);
					}
				} catch (Exception e) {
					new Label(detailperkuliahan.getSemester() + "").setParent(row);
				}
				new Label(detailperkuliahan.getTahap() == null ? "" : detailperkuliahan.getTahap().toString()).setParent(row);
			} else {
				final Intbox smt = new Intbox(detailperkuliahan.getSemester());
				smt.setParent(row);
				smt.setWidth("90%");
				smt.setDisabled(!update);
				smt.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (smt.getValue() != null) {
							Session session = HibernateUtil.currentSession();
							session.refresh(detailperkuliahan);
							detailperkuliahan.setSemester(smt.getValue());
							session.update(detailperkuliahan);
						}
					}
				});

				final Intbox thp = new Intbox(detailperkuliahan.getTahap() == null ? 0 : detailperkuliahan.getTahap());
				thp.setParent(row);
				thp.setWidth("90%");
				thp.setDisabled(!update);
				thp.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (thp.getValue() != null) {
							Session session = HibernateUtil.currentSession();
							session.refresh(detailperkuliahan);
							detailperkuliahan.setTahap(thp.getValue());
							session.update(detailperkuliahan);
						}
					}
				});
			}

			final MyCheckboxConfig internal = new MyCheckboxConfig();
			internal.setChecked(detailperkuliahan.getInternal());
			internal.setParent(row);
			internal.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					session.refresh(detailperkuliahan);
					detailperkuliahan.setInternal(internal.isChecked());
					session.update(detailperkuliahan);
				}
			});

			new Label(detailperkuliahan.getPerkuliahan() == null ? "" : detailperkuliahan.getPerkuliahan().getKelas()).setParent(row);

			Label labelPersetujuan = new Label(Detailperkuliahan.BELUM_DISETUJUI.equals(detailperkuliahan.getPersetujuan()) || detailperkuliahan.getPersetujuan() == null ? "Belum" : "Ya");
			labelPersetujuan.setParent(row);
			labelPersetujuan.setStyle(labelPersetujuan.getValue().equals("Ya") ? "color:blue;" : "color:red;");

			new Label(detailperkuliahan.getPerkuliahan() == null ? "" : detailperkuliahan.getPerkuliahan().getTahunAjaran()).setParent(row);

			if (semester.equals(0)) {
				Hbox totalNilaiKonversi = new Hbox();
				totalNilaiKonversi.setParent(row);
				totalNilaiLabel.setParent(totalNilaiKonversi);
				totalNilaiLabel.setWidth("90%");
				totalNilaiLabel.setReadonly(!checkbox.isChecked());
				totalNilaiKonversi.appendChild(labelNilaiHuruf);
			} else {
				tambahBarisNilaiHuruf(totalNilai, labelNilaiHuruf);
				totalNilai.setParent(row);
			}

			if (user.getMahasiswa() != null) {
				totalNilaiLabel.setDisabled(true);
			}

			Vbox vboxAction = new Vbox();
			vboxAction.setParent(row);

			Hbox toolbar = new Hbox();
			toolbar.setParent(vboxAction);
			toolbar.setVisible(edit);

			MyToolbarbuttonConfig buttonPindah = new MyToolbarbuttonConfig("", "/img/stock_data_edit_table.png");
			buttonPindah.setTooltiptext("Pindah Data");
			buttonPindah.setVisible(detailperkuliahan.getPerkuliahan() != null && Common.getCurrentUser().getDosen() == null && edit);
			buttonPindah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.showFormatCb("Apakah Bapak/Ibu yakin ingin memindahkan KRS mahasiswa {V1} untuk mata kuliah {V2}? Data perkuliahan mahasiswa tersebut akan dipindahkan sesuai tujuan yang dipilih.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											TransferDataMahasiswaHelper transferDataMahasiswaHelper = new TransferDataMahasiswaHelper(detailperkuliahan.getPerkuliahan(), detailperkuliahan.getMahasiswa());
											MyWindow window = new MyWindow();
											window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											transferDataMahasiswaHelper.display(StudiMahasiswaHelper.this, window);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"membuka jendela pindah data KRS mahasiswa",
													e, new String[] {
															"Muat ulang (refresh) halaman ini lalu coba kembali.",
															"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
													});
										}
									}
								}
							}, detailperkuliahan.getMahasiswa().getNama(), detailperkuliahan.getPerkuliahan().getMatakuliah().getNama());
				}
			});
			buttonPindah.setParent(toolbar);

			if (tampilHapus) {
				MyToolbarbuttonConfig buttonHapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				buttonHapus.setTooltiptext("Hapus Data");
				buttonHapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Session session = HibernateUtil.currentSession();
												List<Komentar> komentars = session.createCriteria(Komentar.class).add(Restrictions.eq("detailperkuliahan", detailperkuliahan.getId())).list();

												for (Komentar komentar : komentars) {
													Common.refreshDelete(komentar);
												}

												Common.refreshDelete(session, detailperkuliahan);

												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(true);
													}
												});
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.showFormat("Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang berelasi dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon menghubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, e.getMessage());
											}
										}
									}
								});
					}
				});
				buttonHapus.setParent(toolbar);

				if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder() && Konfigurasi.AKTIF.equals(Common.getKonfigurasi("aktifkan_terhubung_langsung_ke_feeder", Konfigurasi.AKTIF).getNilai())
						&& (mahasiswa != null && mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().isEmpty())) {

					Hbox feederToolbar = new Hbox();
					feederToolbar.setParent(vboxAction);

					MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder", "/img/Finance-Invoice-icon.png");
					buttonTagihan.setOrient("vertical");
					buttonTagihan.setStyle("font-size:8px;");
					buttonTagihan.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin mengirim data ini ke Feeder? Proses pengiriman akan dilakukan sesuai data yang tersedia.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												String[] kon = EksporFromFeederAction.koneksi();
												final String ip = kon[0];
												final String port = kon[1];
												final String username = kon[2];
												final String password = kon[3];
												final String url = kon[4];

												if (!EksporFromFeederAction.exists(url)) {
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons).");
													return;
												}

												final List<String> errorLog = new ArrayList<String>();
												final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {
															@Override
															public void onEvent(Event arg0) throws Exception {
																if (arg0 != null && !arg0.getName().isEmpty()) {
																	EksporFromFeederAction.display();
																	MyMessageboxConfig.show(arg0.getName(), "Info", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
																}

																if (!errorLog.isEmpty()) {
																	StringBuilder err = new StringBuilder();
																	for (String s : errorLog) {
																		if (err.length() > 0) err.append("\n----------------------------------------------------------------------------------------------------------\n");
																		err.append(s);
																	}

																	MyMessageboxConfig.show(err.toString(), "Error Terjadi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

																	File file = new File(Common.REAL_PATH + "/tmp/error_" + Common.randLong() + ".txt");
																	if (!file.getParentFile().exists()) {
																		file.getParentFile().mkdirs();
																	}
																	FileUtils.writeStringToFile(file, err.toString());
																	Filedownload.save(file, "text/plain");
																}
																loadData(true);
															}
														});

												new Thread(new Runnable() {
													@Override
													public void run() {
														try {
															FeederConnector feederConnector = new FeederConnector(ip, Integer.parseInt(port), myLabelProsesDetail);
															String token = feederConnector.getToken(username, password);

															if (token == null || token.trim().isEmpty() || token.trim().toLowerCase().startsWith("error")) {
																myLabelProsesDetail.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
																return;
															}

															FeederExporter feederImporter = new FeederExporter(feederConnector, token, null, null, null);
															myLabelProsesDetail.setValue("Mengirim data " + detailperkuliahan);
															Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
															if (perkuliahan != null) {
																PerkuliahanAction.kirimKeFeeder(feederImporter, detailperkuliahan, feederConnector, token, mahasiswa, errorLog, true);
															} else if (detailperkuliahan.getMatakuliahKonversi() != null) {
																feederImporter.nilaiTransfer(detailperkuliahan, errorLog);
															}
															// FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses.
															myLabelProsesDetail.setValue("");
														} catch (Exception e) {
															ais.common.Common.tampilErrorJikaAdmin(e);
															myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengiriman data studi/nilai perkuliahan mahasiswa ke Neo Feeder",
																	null, e,
																	new String[] {
																			"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																			"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																			"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																	.replace("\n", " "));
														}
													}
												}).start();
											}
										}
									});
						}
					});
					buttonTagihan.setParent(feederToolbar);
				}

				if (semester.equals(0)) {
					if (Common.getCurrentUser().getMahasiswa() != null) {
						buttonPindah.setVisible(false);
					}
				}

			} else if (semester.equals(0) || (Detailperkuliahan.BELUM_DISETUJUI.equals(detailperkuliahan.getPersetujuan()) || detailperkuliahan.getPersetujuan() == null) && detailperkuliahan.getMatakuliahKonversi() != null) {
				MyToolbarbuttonConfig buttonHapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				buttonHapus.setVisible(delete);
				buttonHapus.setTooltiptext("Hapus Data");
				buttonHapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Session session = HibernateUtil.currentSession();
												if (Konfigurasi.AKTIF.equals(Common.getKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol", Konfigurasi.AKTIF).getNilai())) {
													if (Detailperkuliahan.DISETUJUI.equals(detailperkuliahan.getPersetujuan()) && detailperkuliahan.getTotalNilai() > 1.0) {
														MyMessageboxConfig.show("Apabila nilai tidak bernilai nol, mata kuliah ini tidak dapat dihapus. Langkah yang dapat dilakukan: (1) pastikan nilai mata kuliah ini bernilai nol terlebih dahulu; (2) kosongkan atau batalkan nilai yang telah diinputkan; (3) apabila kendala berlanjut, mohon menghubungi bagian Akademik.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
														return;
													}
												}

												List<Komentar> komentars = session.createCriteria(Komentar.class).add(Restrictions.eq("detailperkuliahan", detailperkuliahan.getId())).list();
												for (Komentar komentar : komentars) {
													Common.refreshDelete(komentar);
												}

												Common.refreshDelete(session, detailperkuliahan);
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(true);
													}
												});
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.showFormat("Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang berelasi dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon menghubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, e.getMessage());
											}
										}
									}
								});
					}
				});
				buttonHapus.setParent(toolbar);

				if (semester.equals(0) && Common.getCurrentUser().getMahasiswa() != null) {
					buttonHapus.setVisible(false);
				}
			}
		}
	}

	/**
	 * Memuat/menyegarkan grid KRS: mengambil seluruh {@link Detailperkuliahan} milik
	 * {@link #mahasiswa} pada semester/tahap/semester-pendek/remedial konteks saat ini lewat
	 * {@link Common#getDetailperkuliahans}, memasang ulang renderer, menyegarkan status
	 * persetujuan ({@link #loadStatus}), dan menghitung ulang informasi jam bentrok jadwal
	 * (langsung, paralel, dan paralel-dari-paralel) yang ditampilkan di {@link #jamBentrok}.
	 * Bila konfigurasi {@code saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok_dosen}
	 * aktif dan bukan mode konversi (tahap {@code -1}), turut memvalidasi bentrok jadwal dosen.
	 *
	 * @param value bila berupa {@link Boolean} {@code true}, memaksa hitung ulang mata kuliah
	 *              ekivalen dari sumber (bukan cache) — dipakai setelah operasi yang mengubah
	 *              data mata kuliah/ekivalensi
	 */
	public void loadData(Object value) {
		boolean refresh = (value != null && value instanceof Boolean) ? (Boolean) value : false;
		detailperkuliahansData = Common.getDetailperkuliahans(mahasiswa, semester, tahapan, null, semesterPendek, remedial, false, false, refresh);

		List<Long> idPerkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahanid : detailperkuliahansData) {
			Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (dp != null && dp.getPerkuliahan() != null) {
				idPerkuliahans.add(dp.getPerkuliahan().getId());
			}
		}

		ListModel strset = new SimpleListModel(detailperkuliahansData);
		grid.setRowRenderer(new DetailMahasiswaRenderer(krsMahasiswa, refresh));
		grid.setModelCheckMobile(strset);

		loadStatus();

		Common.clear(jamBentrok);
		jamBentrok.appendChild(new MyCaptionStyled("Informasi Jam Bentrok"));
		List<Perkuliahan> jadwalPerkuliahanParalels = new ArrayList<Perkuliahan>();
		List<Detailperkuliahan> detailperkuliahansbaru = new ArrayList<Detailperkuliahan>();
		
		for (Long detailperkuliahanid : detailperkuliahansData) {
			Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (d != null && d.getPerkuliahan() != null) {
				detailperkuliahansbaru.add(d);
				List<Perkuliahan> jadwalparalels = d.getPerkuliahan().ambilParalelPerkuliahan();
				jadwalPerkuliahanParalels.addAll(jadwalparalels);
			}
		}

		jamBentrok.appendChild(Common.generateInformasiJamBentrok(detailperkuliahansbaru));
		jamBentrok.appendChild(Common.generateInformasiJamBentrokParalel(detailperkuliahansbaru, jadwalPerkuliahanParalels));
		jamBentrok.appendChild(Common.generateInformasiJamBentrokParalelParalel(jadwalPerkuliahanParalels));

		if (tahapan == null || !tahapan.equals(-1)) {
			if (Konfigurasi.AKTIF.equals(Common.getKonfigurasi("saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok_dosen", Konfigurasi.TIDAK_AKTIF).getNilai())) {
				try {
					Common.checkJamBentrok(detailperkuliahansbaru);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	/** Menghitung ulang total SKS yang diambil dan status persetujuan gabungan (semua/sebagian/belum disetujui) dari {@link #detailperkuliahansData}, lalu memperbarui label {@link #jumlahKRS} dan {@link #statusPersetujuan} (dengan warna sesuai status). */
	private void loadStatus() {
		boolean adaPersetujuan = false;
		boolean adaBelumPersetujuan = false;
		Integer jmlKrs = 0;
		for (Long detailperkuliahanid : detailperkuliahansData) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				try {
					adaPersetujuan |= Detailperkuliahan.DISETUJUI.equals(detailperkuliahan.getPersetujuan());
					adaBelumPersetujuan |= Detailperkuliahan.BELUM_DISETUJUI.equals(detailperkuliahan.getPersetujuan());
					
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null ? detailperkuliahan.getMatakuliahKonversi() : detailperkuliahan.getPerkuliahan().getMatakuliah();
					matakuliah = Common.getMatakuliahApakahEkivalen(matakuliah, mahasiswa == null ? null : mahasiswa.getNim(), false)[0];
					jmlKrs += matakuliah.getSks();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		jumlahKRS.setValue("Jumlah SKS : " + jmlKrs + " SKS");

		if (adaPersetujuan && adaBelumPersetujuan) {
			statusPersetujuan.setValue("Status: Sebagian sudah disetujui");
			statusPersetujuan.setStyle("font-size:11px;font-weight:bold;color:green;");
		} else if (adaPersetujuan) {
			statusPersetujuan.setValue("Status: Sudah disetujui semua");
			statusPersetujuan.setStyle("font-size:11px;font-weight:bold;color:blue;");
		} else if (adaBelumPersetujuan) {
			statusPersetujuan.setValue("Status: Belum disetujui semua");
			statusPersetujuan.setStyle("font-size:11px;font-weight:bold;color:red;");
		}
	}

	/**
	 * Menghitung total SKS dari seluruh baris grid yang checkbox persetujuannya tercentang
	 * (mengabaikan baris konversi bila konfigurasi {@code konversi_masuk_akumulasi_jumlah_sks_pengambilan_krs}
	 * tidak aktif), lalu memvalidasinya terhadap batas SKS berdasarkan IP mahasiswa lewat
	 * {@link Common#checkPembatasanSKSBerdasarkanIP}.
	 *
	 * @return {@code true} bila melebihi batas (pemanggil harus membatalkan aksi persetujuan)
	 */
	@SuppressWarnings("unchecked")
	private boolean apakahMelebihiSks() throws Exception {
		Integer jumlahSks = 0;
		Boolean termasukKonversi = Konfigurasi.AKTIF.equals(Common.getKonfigurasi("konversi_masuk_akumulasi_jumlah_sks_pengambilan_krs", Konfigurasi.TIDAK_AKTIF).getNilai());

		List<Row> rows = grid.getRows().getChildren();
		for (Row row : rows) {
			try {
				MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
				if (myCheckbox != null && myCheckbox.isChecked()) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) row.getAttribute("value");
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null ? detailperkuliahan.getMatakuliahKonversi() : detailperkuliahan.getPerkuliahan().getMatakuliah();
					matakuliah = Common.getMatakuliahApakahEkivalen(matakuliah, mahasiswa == null ? null : mahasiswa.getNim(), false)[0];
					
					if (matakuliah == null || (!termasukKonversi && detailperkuliahan.getMatakuliahKonversi() != null)) {
						continue;
					}
					jumlahSks += matakuliah.getSks();
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, semester, jumlahSks, semesterPendek);
	}

	/**
	 * Memproses perubahan centang persetujuan pada satu baris {@link Detailperkuliahan}. Urutan
	 * validasi (lihat juga javadoc kelas): status pembayaran semester berjalan → status
	 * pembayaran semester sebelumnya (ambang persentase dari konfigurasi) → batas SKS
	 * berdasarkan IP ({@link #apakahMelebihiSks}) — kegagalan pada validasi mana pun membatalkan
	 * centang dan menampilkan pesan. Bila lolos, memuat ulang entitas dari database (menghindari
	 * data stale) dan menyimpan status persetujuan baru. Setelah disimpan, mengecek apakah
	 * SEMUA baris pada grid kini tercentang — bila ya, membuka {@link CatatanHelper} untuk
	 * meminta catatan dosen PA sebelum menyegarkan tampilan; bila tidak, langsung menyegarkan.
	 */
	@SuppressWarnings("unchecked")
	private void lakukanSatuPersetujuan(MyCheckboxConfig checkbox, Detailperkuliahan seledtedDetailperkuliahan, KrsMahasiswa krsMahasiswa, final int semester) throws Exception {
		Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_persetujuan_krs", Konfigurasi.AKTIF);

		if (Konfigurasi.AKTIF.equals(konfigurasi.getNilai())) {
			if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null || tahapan.equals(0)) {
				if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, true, semesterPendek != null)) {
					MyMessageboxConfig.showFormat("Mahasiswa \"{V1}\" belum membayar biaya perkuliahan pada semester {V2}. Mohon mahasiswa tersebut untuk segera menghubungi bagian Keuangan. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah menyelesaikan pembayaran biaya perkuliahan; (2) lakukan konfirmasi pembayaran ke bagian Keuangan; (3) apabila pembayaran telah dilakukan, mohon memperbarui status pembayaran mahasiswa.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNama(), semester);
					return;
				}
			}

			if (!UtsDanUasCheckerHelper.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahapan, true)) {
				return;
			}
		}

		if (semesterPendek == null) {
			if (!Common.checkStatusPembayaranMahasiswaSebelumnya(semester, tahapan, mahasiswa, true)) {
				Double harusLunas = 90.0;
				try {
					harusLunas = Double.parseDouble(Common.getKonfigurasi("batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_disetujui_krs", "90").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/StudiMahasiswaHelper.java:775");}
				
				MyMessageboxConfig.showFormat("Mahasiswa \"{V1}\" belum melunasi {V2}% biaya perkuliahan pada{V3}. Mohon mahasiswa tersebut untuk segera menghubungi bagian Keuangan. Langkah yang dapat dilakukan: (1) pastikan pelunasan minimal sesuai ketentuan telah dipenuhi; (2) lakukan konfirmasi pembayaran ke bagian Keuangan; (3) apabila pembayaran telah dilakukan, mohon memperbarui status pembayaran mahasiswa.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNama(), harusLunas, ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan != null && tahapan > 0) ? " tahap " + (tahapan - 1) : " semester " + (semester - 1)));
				return;
			}
		}

		if (semester > 0 && apakahMelebihiSks()) {
			checkbox.setChecked(false);
		} else {
			Session session = HibernateUtil.currentSession();
			if (seledtedDetailperkuliahan == null || seledtedDetailperkuliahan.getId() == null) {
				checkbox.setChecked(false);
				return;
			}
			seledtedDetailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class).add(Restrictions.idEq(seledtedDetailperkuliahan.getId())).uniqueResult();
			if (seledtedDetailperkuliahan == null) {
				checkbox.setChecked(false);
				return;
			}
			seledtedDetailperkuliahan.setPersetujuan(checkbox.isChecked() ? Detailperkuliahan.DISETUJUI : Detailperkuliahan.BELUM_DISETUJUI);
			Common.refreshUpdate(session, seledtedDetailperkuliahan);

			List<Row> rows = grid.getRows().getChildren();
			boolean semua = true;
			for (Row row : rows) {
				try {
					MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
					if (myCheckbox != null && !myCheckbox.isChecked()) {
						semua = false;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (semua) {
				Dosen dosenPembimbingAkademik = krsMahasiswa.getDosenPa();
				CatatanHelper komentarHelper = new CatatanHelper(mahasiswa, semester, tahapan, dosenPembimbingAkademik, tahunAjaran, semesterPendek, remedial);

				komentarHelper.display(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						KrsMahasiswa krsMhs = (KrsMahasiswa) arg0.getData();
						catatan.setValue(krsMhs.getCatatan());
						catatanKhs.setValue(krsMhs.getCatatanKhs());
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								display(mahasiswa, tahunAjaran, semester, tahapan, component, keterangan, komentarshtml, ipIpk, sksSksk, catatan, catatanKhs);
							}
						});
					}
				});
			} else {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						display(mahasiswa, tahunAjaran, semester, tahapan, component, keterangan, komentarshtml, ipIpk, sksSksk, catatan, catatanKhs);
					}
				});
			}
		}
		if (krsMahasiswa != null) krsMahasiswa.masukkanData(checkbox.isChecked() ? "setujui" : "batalkan");
	}

	/**
	 * Menyetujui SEMUA baris {@link Detailperkuliahan} sekaligus, hanya bila total SKS
	 * (mengabaikan konversi bila dikonfigurasikan demikian) TIDAK melebihi batas SKS berdasarkan
	 * IP mahasiswa. Setelah tersimpan, menyinkronkan ulang KRS mahasiswa dan membuka
	 * {@link CatatanHelper} untuk mencatat catatan dosen PA sebelum menyegarkan tampilan.
	 */
	private void lakukanSemuaPersetujuan() throws Exception {
		if (detailperkuliahansData != null && !detailperkuliahansData.isEmpty()) {
			Session session = HibernateUtil.currentSession();
			Integer jumlahSks = 0;
			Boolean termasukKonversi = Konfigurasi.AKTIF.equals(Common.getKonfigurasi("konversi_masuk_akumulasi_jumlah_sks_pengambilan_krs", Konfigurasi.TIDAK_AKTIF).getNilai());

			for (Long detailperkuliahanid : detailperkuliahansData) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null ? detailperkuliahan.getMatakuliahKonversi() : detailperkuliahan.getPerkuliahan().getMatakuliah();
					matakuliah = Common.getMatakuliahApakahEkivalen(matakuliah, mahasiswa == null ? null : mahasiswa.getNim(), false)[0];
					if (matakuliah == null || (!termasukKonversi && detailperkuliahan.getMatakuliahKonversi() != null)) {
						continue;
					}
					jumlahSks += matakuliah.getSks();
				}
			}

			if (!Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, semester, jumlahSks, semesterPendek)) {
				for (Long detailperkuliahanid : detailperkuliahansData) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
					if (detailperkuliahan != null) {
						detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);
						Common.refreshUpdate(session, detailperkuliahan);
					}
				}

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						KrsMahasiswa krsMhs = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, false, false);
						Dosen dosenPembimbingAkademik = krsMhs.getDosenPa();
						CatatanHelper komentarHelper = new CatatanHelper(mahasiswa, semester, tahapan, dosenPembimbingAkademik, tahunAjaran, semesterPendek, remedial);

						komentarHelper.display(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								KrsMahasiswa krs = (KrsMahasiswa) arg0.getData();
								catatan.setValue(krs.getCatatan());
								catatanKhs.setValue(krs.getCatatanKhs());
								display(mahasiswa, tahunAjaran, semester, tahapan, component, keterangan, komentarshtml, ipIpk, sksSksk, catatan, catatanKhs);
							}
						});
					}
				});
			}
		}
	}

	/**
	 * Membatalkan persetujuan SEMUA baris {@link Detailperkuliahan} yang BELUM memiliki nilai
	 * (total nilai ≤ 1.0); baris yang sudah dinilai dilewati dan memicu pesan peringatan bahwa
	 * mata kuliah bernilai tidak dapat dibatalkan persetujuannya. Menyegarkan tampilan di akhir.
	 */
	private void lakukanPembatalanSemuaPersetujuan() throws Exception {
		Session session = HibernateUtil.currentSession();
		boolean ada = false;
		
		for (Long detailperkuliahanid : detailperkuliahansData) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getTotalNilai() != null && detailperkuliahan.getTotalNilai() > 1.0) {
					ada = true;
				} else {
					detailperkuliahan.setPersetujuan(Detailperkuliahan.BELUM_DISETUJUI);
					Common.refreshUpdate(session, detailperkuliahan);
				}
			}
		}

		if (ada) {
			MyMessageboxConfig.show("Perkuliahan yang sudah dinilai tidak dapat dibatalkan persetujuannya. Langkah yang dapat dilakukan: (1) pastikan nilai perkuliahan telah dikosongkan terlebih dahulu; (2) batalkan penilaian yang telah dilakukan; (3) apabila kendala berlanjut, mohon menghubungi bagian Akademik.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				display(mahasiswa, tahunAjaran, semester, tahapan, component, keterangan, komentarshtml, ipIpk, sksSksk, catatan, catatanKhs);
			}
		});
	}

	/**
	 * Titik masuk utama: merakit seluruh layar Rencana Studi Mahasiswa ke dalam
	 * {@code component} untuk kombinasi mahasiswa/semester/tahap yang diberikan. Lihat javadoc
	 * kelas untuk gambaran lengkap toolbar dan alur persetujuan. Ringkasan alur method ini:
	 * <ol>
	 * <li>Menyinkronkan KRS TANPA memicu sinkronisasi penuh ({@link Common#ambilKrsMahasiswaTanpaSinkronisasi}) —
	 * disengaja, karena method ini juga dipanggil sebagai callback pasca-perubahan sehingga
	 * harus murni baca agar tidak bertabrakan dengan transaksi yang baru selesai.</li>
	 * <li>Mengisi label ringkasan (IP/IPK, SKS diambil/lulus dengan rincian SKS konversi,
	 * keterangan KRS, jumlah komentar).</li>
	 * <li>Bila {@code component} bukan {@link Tabpanel}, membangun {@link Tabbox} dua tab:
	 * "Rencana Studi Mahasiswa" (isi utama) dan "Agenda Konsultasi Mahasiswa" (dimuat lazy,
	 * mendelegasikan ke {@link AktifitasKrsMahasiswaHelper}), lalu memindahkan konten Tab-1 ke
	 * atas di akhir method ({@code MyButtonTabbox.gantiTabboxNative}) agar tampil langsung.</li>
	 * <li>Membangun toolbar aksi kontekstual (lihat javadoc kelas) dan grid utama dengan kolom
	 * yang visibilitas/labelnya menyesuaikan konteks (semester {@code 0}/konversi vs. reguler,
	 * tahap {@code -1}, {@code aktifkanTahapan}).</li>
	 * <li>Memuat grid komentar diskusi ({@link #loadDataKomentar()}) dan grid detail
	 * ({@link #loadData(Object)}).</li>
	 * <li>Bila konfigurasi {@code selain_admin_tidak_boleh_merubah_konversi} aktif dan konteks
	 * semester {@code 0}, mengunci (freeze) seluruh panel untuk pengguna non-administrator.</li>
	 * </ol>
	 *
	 * @param mahasiswa    mahasiswa yang KRS-nya ditampilkan
	 * @param tahunAjaran  tahun akademik konteks
	 * @param semester     semester konteks ({@code 0} = mode konversi nilai transfer)
	 * @param tahapan      tahap konteks (bila jenjang memakai tahapan; {@code -1} = mode khusus)
	 * @param component    kontainer ZK yang akan diisi (isi sebelumnya dibersihkan, kecuali sudah berupa {@link Tabpanel})
	 * @param keterangan   komponen HTML tempat keterangan KRS naratif ditulis
	 * @param komentarshtml komponen HTML tempat ringkasan jumlah komentar ditulis
	 * @param ipIpk        label tempat IP/IPK ditulis
	 * @param sksSksk      tautan tempat ringkasan SKS diambil/lulus ditulis
	 * @param catatan      label catatan dosen PA (diperbarui setelah persetujuan penuh)
	 * @param catatanKhs   label catatan KHS (diperbarui setelah persetujuan penuh)
	 */
	public void display(final Mahasiswa mahasiswa, final String tahunAjaran, final Integer semester,
			final Integer tahapan, final Component component, final Html keterangan, final Html komentarshtml,
			final Label ipIpk, final A sksSksk, final Label catatan, final Label catatanKhs) {

		this.catatan = catatan;
		this.catatanKhs = catatanKhs;
		this.mahasiswa = mahasiswa;
		this.tahunAjaran = tahunAjaran;
		this.semester = semester;
		this.tahapan = tahapan;
		this.component = component;
		this.keterangan = keterangan;
		this.komentarshtml = komentarshtml;
		this.ipIpk = ipIpk;
		this.sksSksk = sksSksk;

		final String jenisSemester = semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;

		konfigurasiPersetujuanKrsDosen = CommonPenilaian.getKonfigurasiPersetujuanKrsOlehDosen(tahunAjaran, jenisSemester, semesterPendek);
		// display() dipanggil juga dari callback penyimpanan catatan/persetujuan.
		// Jalur tampilan harus murni baca; sinkronisasi penuh hanya dilakukan oleh
		// aksi eksplisit setelah transaksi perubahan sebelumnya selesai.
		krsMahasiswa = Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan,
				semesterPendek);

		Double ipmhs = krsMahasiswa.getIps();
		Double ipkmhs = krsMahasiswa.getIpk();
		ipIpk.setValue(Common.numberFormat.get().format(ipmhs) + " / " + Common.numberFormat.get().format(ipkmhs));

		Integer sksmhss = krsMahasiswa.getSksYangDiambil();
		Integer sksmhs = krsMahasiswa.getSksk();
		Integer skskonversi = krsMahasiswa.getSksKonversi();
		Integer sksBukanKonversi = krsMahasiswa.getSksBukanKonversi();
		
		String labelSksk = Common.numberFormat.get().format(sksmhss) + " / " + Common.numberFormat.get().format(sksmhs);
		if (skskonversi > 0) {
			labelSksk += " (Bukan Konversi : " + Common.numberFormat.get().format(sksBukanKonversi) + " SKS, Konversi " + Common.numberFormat.get().format(skskonversi) + " SKS)";
		}
		sksSksk.setLabel(labelSksk);

		String krs = mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek, krsMahasiswa, remedial);
		keterangan.setContent(krs);
		
		Integer komentars = krsMahasiswa.getKomentars();
		komentarshtml.setContent(komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar");

		Common.clear(component);
		Component tabpanelUtama;
		// Dideklarasikan di luar if-else agar gantiTabboxNative bisa dipanggil setelah
		// semua konten Tab-1 selesai dibangun (lihat akhir method).
		Tabbox tabbox = null;

		if (component instanceof Tabpanel) {
			tabpanelUtama = component;
		} else {
			tabbox = new Tabbox();
			tabbox.setParent(component);
			tabbox.setHeight("5500px");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tabSoal = new MyTabConfig("Rencana Studi Mahasiswa");
			tabSoal.setParent(tabs);

			MyTabConfig tabAgendaKonsultasi = new MyTabConfig();
			tabAgendaKonsultasi.setVisible(edit);
			tabAgendaKonsultasi.setParent(tabs);
			tabAgendaKonsultasi.setLabel("Agenda Konsultasi Mahasiswa");

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			tabpanelUtama = new ais.ui.util.MyTabpanel();
			((Tabpanel) tabpanelUtama).setStyle("min-height: 500px;");
			tabpanelUtama.setParent(tabpanels);

			final AktifitasKrsMahasiswaHelper aktifitasKrsMahasiswaHelper = new AktifitasKrsMahasiswaHelper();

			final Tabpanel detailAgendaKonsultasi = new ais.ui.util.MyTabpanel();
			detailAgendaKonsultasi.setParent(tabpanels);
			detailAgendaKonsultasi.setHeight("500px");
			tabAgendaKonsultasi.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detailAgendaKonsultasi.getChildren().isEmpty()) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 500px;");
						aktifitasKrsMahasiswaHelper.initDetail(krsMahasiswa, groupbox);
						detailAgendaKonsultasi.appendChild(groupbox);
					}
				}
			});

			aktifitasKrsMahasiswaHelper.initCetak(tabbox, krsMahasiswa);
		}

		final MyGroupboxStyled groupbox = new MyGroupboxStyled();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("96%");
		groupbox.setParent(tabpanelUtama);
		groupbox.appendChild(new MyCaptionStyled("Rencana Studi Mahasiswa " + mahasiswa.getNim() + " " + mahasiswa.getNama() + " semester " + semester));

		statusPersetujuan = new Label(ais.common.Common.getBahasaConfig("Status: Belum disetujui"));
		statusPersetujuan.setStyle("font-size:11px;font-weight:bold;color:red;");
		statusPersetujuan.setParent(groupbox);
		groupbox.appendChild(new Space());
		
		jumlahKRS = new Label(ais.common.Common.getBahasaConfig("Jumlah SKS : 0"));
		jumlahKRS.setStyle("font-size:11px;font-weight:bold;color:blue;");
		jumlahKRS.setParent(groupbox);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("40px");

		if (tahapan != null && tahapan.equals(-1) || !edit) {
			toolbar.setVisible(false);
		}

		Konfigurasi konfigurasi;
		final Konfigurasi konfigurasiPerbaikan;
		
		if (Konfigurasi.AKTIF.equals(Common.getKonfigurasi("input_krs_harus_berdasarkan_kalender_akademik", Konfigurasi.AKTIF).getNilai())) {
			konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(HibernateUtil.currentSession(),
					remedial ? Konfigurasi.KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
					tahunAjaran, jenisSemester, mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(), mahasiswa.getProgram());
			
			konfigurasiPerbaikan = Common.checkKonfigurasiDenganKalenderAkademik(HibernateUtil.currentSession(),
					remedial ? Konfigurasi.PERBAIKAN_KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.PERBAIKAN_KRS : Konfigurasi.PERBAIKAN_KRS_SP,
					tahunAjaran, jenisSemester, mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(), mahasiswa.getProgram());
		} else {
			konfigurasi = Common.getKonfigurasi(
					remedial ? Konfigurasi.KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
					tahunAjaran, jenisSemester);
			
			konfigurasiPerbaikan = Common.getKonfigurasi(
					remedial ? Konfigurasi.PERBAIKAN_KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.PERBAIKAN_KRS : Konfigurasi.PERBAIKAN_KRS_SP,
					tahunAjaran, jenisSemester);
		}

		if (semester != null && semester > 0 && Konfigurasi.AKTIF.equals(Common.getKonfigurasi("dosen_pa_boleh_mengambilkan_krs_mahasiswa", Konfigurasi.AKTIF).getNilai())) {
			final MyToolbarbuttonConfig buttonPerkuliahan = new MyToolbarbuttonConfig("Ambilkan Perkuliahan", "/img/svg/edit-box-line.svg");
			
			boolean condition1 = (konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai()));
			boolean condition2 = (konfigurasiPerbaikan != null && Konfigurasi.AKTIF.equals(konfigurasiPerbaikan.getNilai()));
			boolean condition3 = Konfigurasi.AKTIF.equals(Common.getKonfigurasi("dosen_pa_boleh_mengambilkan_krs_mahasiswa_walaupn_diluar_jadwal", Konfigurasi.TIDAK_AKTIF).getNilai());
			
			buttonPerkuliahan.setVisible(condition1 || condition2 || condition3);
			buttonPerkuliahan.setOrient("vertical");
			
			if (ConstantValues.aktifkanTahapan && tahapan != null && !tahapan.equals(0)) {
				Integer t = mahasiswa.currentTahapan();
				if (t != null && !t.equals(0) && tahapan >= t) {
					buttonPerkuliahan.setDisabled(false);
				} else {
					buttonPerkuliahan.setDisabled(true);
				}
			}

			final Konfigurasi k = konfigurasi;
			buttonPerkuliahan.addEventListener("onClick", new EventListener() {
				private AmbilDataPerkuliahanHelper ambilDataPerkuliahanHelper = new AmbilDataPerkuliahanHelper(semesterPendek, remedial);

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						List<String> warnings = new ArrayList<String>();
						if (mahasiswa != null) {
							List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
									HibernateUtil.currentSession().createCriteria(SyaratUjian.class)
											.add(Restrictions.eq("krs", true))
											.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), SyaratUjian.class);
							
							for (SyaratUjian syaratUjian : syaratUjians) {
								SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester, "Ambil KRS", warnings);
							}
						}

						if (!warnings.isEmpty()) {
							StringBuilder w = new StringBuilder();
							for (String wa : warnings) {
								if (w.length() > 0) w.append("\n\n");
								w.append(wa);
							}
							MyMessageboxConfig.show(w.toString(), "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}

						if (!(k != null && Konfigurasi.AKTIF.equals(k.getNilai()))) {
							if (konfigurasiPerbaikan != null && Konfigurasi.AKTIF.equals(konfigurasiPerbaikan.getNilai())) {
								if (detailperkuliahansData.isEmpty()) {
									MyMessageboxConfig.show("Mahasiswa ini belum pernah mengambil KRS sehingga KRS tidak dapat diperbaiki. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah melakukan pengisian KRS terlebih dahulu; (2) mohon menghubungi bagian Akademik atau Admin Fakultas atau Program Studi untuk informasi lebih lanjut.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									return;
								}
							}
						}

						if (Konfigurasi.AKTIF.equals(Common.getKonfigurasi("status_mahasiswa_harus_aktif_sebelum_isi_krs", Konfigurasi.AKTIF).getNilai())) {
							HistoryStatusMahasiswa hsm = Common.getHistoryStatusMahasiswa(krsMahasiswa);
							StatusMahasiswa statusMahasiswa = hsm.ambilStatusMahasiswa(semester);
							if (statusMahasiswa == null || !statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId())) {
								MyMessageboxConfig.showFormat("Status mahasiswa \"{V1}\" saat ini {V2}, sehingga KRS pada semester {V3} tidak dapat diambil. Mohon mahasiswa tersebut untuk segera menghubungi bagian Keuangan. Langkah yang dapat dilakukan: (1) pastikan status mahasiswa dalam keadaan aktif; (2) lakukan konfirmasi status ke bagian Akademik atau Keuangan; (3) apabila kendala berlanjut, mohon menghubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNama(), (statusMahasiswa == null ? "" : statusMahasiswa.getNama()), semester);
								return;
							}
						}

						Konfigurasi konfigDPA = Common.getKonfigurasi("dosen_pa_harus_ada_sebelum_isi_krs", Konfigurasi.AKTIF);
						if (krsMahasiswa.getDosenPa() == null && Konfigurasi.AKTIF.equals(konfigDPA.getNilai())) {
							MyMessageboxConfig.show("Mahasiswa ini belum memiliki Dosen Pembimbing Akademik sehingga KRS tidak dapat diambil. Langkah yang dapat dilakukan: (1) daftarkan Dosen Pembimbing Akademik untuk mahasiswa terlebih dahulu; (2) mohon menghubungi bagian Akademik atau Admin Fakultas atau Program Studi untuk pendaftaran Dosen Pembimbing Akademik.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}

						if (Konfigurasi.AKTIF.equals(Common.getKonfigurasi("kelas_harus_ada_sebelum_isi_krs", Konfigurasi.TIDAK_AKTIF).getNilai())) {
							String kelas = krsMahasiswa.getKelas();
							if (kelas == null || kelas.trim().isEmpty()) {
								MyMessageboxConfig.show("Mahasiswa ini belum memiliki kelas. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah ditempatkan pada kelas; (2) mohon menghubungi bagian Akademik untuk penetapan kelas mahasiswa.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}
						}

						if (semesterPendek == null) {
							Konfigurasi konfigBayar = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs", Konfigurasi.AKTIF);
							if (Konfigurasi.AKTIF.equals(konfigBayar.getNilai())) {
								if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null || tahapan.equals(0)) {
									if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false, semesterPendek != null)) {
										if (semester != null && semester.intValue() >= 1) {
											MyMessageboxConfig.showFormat("Bapak/Ibu belum membayar biaya perkuliahan pada semester {V1}. Mohon mengambil KRS sesuai pembayaran yang baru saja Bapak/Ibu lakukan. Langkah yang dapat dilakukan: (1) selesaikan pembayaran biaya perkuliahan terlebih dahulu; (2) mohon menghubungi bagian Keuangan untuk informasi lebih lanjut.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, semester);
											return;
										}
									}
								}
								if (!UtsDanUasCheckerHelper.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahapan)) {
									return;
								}
							}
						} else {
							Konfigurasi konfigBayarSp = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_sp", Konfigurasi.AKTIF);
							if (Konfigurasi.AKTIF.equals(konfigBayarSp.getNilai())) {
								if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null || tahapan.equals(0)) {
									if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false, semesterPendek != null)) {
										MyMessageboxConfig.showFormat("Bapak/Ibu belum membayar biaya perkuliahan semester pendek pada semester {V1}. Mohon mengambil KRS semester pendek sesuai pembayaran yang baru saja Bapak/Ibu lakukan. Langkah yang dapat dilakukan: (1) selesaikan pembayaran biaya perkuliahan semester pendek terlebih dahulu; (2) mohon menghubungi bagian Keuangan untuk informasi lebih lanjut.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, semester);
										return;
									}
								}
							}
						}

						if (semesterPendek == null) {
							if (!Common.checkStatusPembayaranMahasiswaSebelumnya(semester, tahapan, mahasiswa)) {
								Double harusLunas = 90.0;
								try {
									harusLunas = Double.parseDouble(Common.getKonfigurasi("batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs", "90").getNilai().trim());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/StudiMahasiswaHelper.java:1157");}
								
								MyMessageboxConfig.showFormat("Mahasiswa \"{V1}\" belum melunasi {V2}% biaya perkuliahan pada{V3}. Mohon mahasiswa tersebut untuk segera menghubungi bagian Keuangan. Langkah yang dapat dilakukan: (1) pastikan pelunasan minimal sesuai ketentuan telah dipenuhi; (2) lakukan konfirmasi pembayaran ke bagian Keuangan; (3) apabila pembayaran telah dilakukan, mohon memperbarui status pembayaran mahasiswa.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNama(), harusLunas, ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan != null && tahapan > 0) ? " tahap " + (tahapan - 1) : " semester " + (semester - 1)));
								return;
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					Session session = HibernateUtil.currentSession();
					List<String> alasans = session.createCriteria(BlokirMahasiswa.class)
							.add(Restrictions.isNotNull("keterangan"))
							.add(Restrictions.ne("keterangan", ""))
							.setProjection(Projections.property("keterangan"))
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("krs", true))
							.list();

					if (!alasans.isEmpty()) {
						StringBuilder alas = new StringBuilder();
						for (String s : alasans) {
							if (alas.length() > 0) alas.append("\n\n");
							alas.append(s);
						}

						try {
							MyMessageboxConfig.show(alas.toString(), "Informasi KRS", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/StudiMahasiswaHelper.java:1187");
						}
						return;
					}

					ambilDataPerkuliahanHelper.display(mahasiswa, tahunAjaran, semester, tahapan, StudiMahasiswaHelper.this, detailperkuliahansData);
				}
			});
			buttonPerkuliahan.setParent(toolbar);

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAjaran, jenisSemester)) {
						buttonPerkuliahan.setVisible(true);
					}
				}
			});
		}

		MyToolbarbuttonConfig buttonKomentar = new MyToolbarbuttonConfig("Komentar", "/img/m3.gif");
		buttonKomentar.setOrient("vertical");
		buttonKomentar.setVisible(semester > 0);

		buttonKomentar.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				KrsMahasiswa krsMhs = Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan,
						semesterPendek);
				if (krsMhs != null) krsMhs.masukkanData("komentar");
				
				Dosen dosenPembimbingAkademik = krsMhs.getDosenPa();
				KomentarHelper komentarHelper = new KomentarHelper(mahasiswa, tahunAjaran, semester, tahapan, semesterPendek, remedial, dosenPembimbingAkademik);

				komentarHelper.display(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						display(mahasiswa, tahunAjaran, semester, tahapan, component, keterangan, komentarshtml, ipIpk, sksSksk, catatan, catatanKhs);
					}
				});
			}
		});
		buttonKomentar.setParent(toolbar);

		MyToolbarbuttonConfig buttonCatatan = new MyToolbarbuttonConfig("Catatan", "/img/m3.gif");
		buttonCatatan.setOrient("vertical");
		buttonCatatan.setVisible(semester > 0);

		buttonCatatan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				KrsMahasiswa krsMhs = Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan,
						semesterPendek);
				if (krsMhs != null) krsMhs.masukkanData("catatan");
				
				Dosen dosenPembimbingAkademik = krsMhs.getDosenPa();
				CatatanHelper catHelper = new CatatanHelper(mahasiswa, semester, tahapan, dosenPembimbingAkademik, tahunAjaran, semesterPendek, remedial);

				catHelper.display(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						KrsMahasiswa krs = (KrsMahasiswa) arg0.getData();
						catatan.setValue(krs.getCatatan());
						catatanKhs.setValue(krs.getCatatanKhs());
					}
				});
			}
		});
		buttonCatatan.setParent(toolbar);

		final MyToolbarbuttonConfig tolakbutton = new MyToolbarbuttonConfig("Batalkan", "/img/shutdown.PNG");
		tolakbutton.setOrient("vertical");
		tolakbutton.setDisabled(!reject);
		tolakbutton.setVisible(konfigurasiPersetujuanKrsDosen.checkAktif() && semester > 0);

		final MyToolbarbuttonConfig setujubutton = new MyToolbarbuttonConfig("Setujui", "/img/m2.gif");
		setujubutton.setOrient("vertical");
		setujubutton.setVisible(konfigurasiPersetujuanKrsDosen.checkAktif() && semester > 0);
		setujubutton.setDisabled(!approve);
		
		setujubutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_persetujuan_krs", Konfigurasi.AKTIF);

				if (Konfigurasi.AKTIF.equals(konfigurasi.getNilai())) {
					if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null || tahapan.equals(0)) {
						if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, true, semesterPendek != null)) {
							MyMessageboxConfig.showFormat("Mahasiswa \"{V1}\" belum membayar biaya perkuliahan pada semester {V2}. Mohon mahasiswa tersebut untuk segera menghubungi bagian Keuangan. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah menyelesaikan pembayaran biaya perkuliahan; (2) lakukan konfirmasi pembayaran ke bagian Keuangan; (3) apabila pembayaran telah dilakukan, mohon memperbarui status pembayaran mahasiswa.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNama(), semester);
							return;
						}
					}

					if (!UtsDanUasCheckerHelper.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahapan, true)) {
						return;
					}
				}

				if (semesterPendek == null) {
					if (!Common.checkStatusPembayaranMahasiswaSebelumnya(semester, tahapan, mahasiswa, true)) {
						Double harusLunas = 90.0;
						try {
							harusLunas = Double.parseDouble(Common.getKonfigurasi("batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_disetujui_krs", "90").getNilai().trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/StudiMahasiswaHelper.java:1288");}
						
						MyMessageboxConfig.showFormat("Mahasiswa \"{V1}\" belum melunasi {V2}% biaya perkuliahan pada{V3}. Mohon mahasiswa tersebut untuk segera menghubungi bagian Keuangan. Langkah yang dapat dilakukan: (1) pastikan pelunasan minimal sesuai ketentuan telah dipenuhi; (2) lakukan konfirmasi pembayaran ke bagian Keuangan; (3) apabila pembayaran telah dilakukan, mohon memperbarui status pembayaran mahasiswa.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNama(), harusLunas, ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan != null && tahapan > 0) ? " tahap " + (tahapan - 1) : " semester " + (semester - 1)));
						return;
					}
				}

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menyetujui KRS ini? Setelah disetujui, KRS mahasiswa akan berstatus disetujui.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							if (krsMahasiswa != null) krsMahasiswa.masukkanData("setujui");
							lakukanSemuaPersetujuan();
						}
					}
				});
			}
		});
		setujubutton.setParent(toolbar);

		tolakbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan persetujuan KRS ini? Status persetujuan KRS mahasiswa akan dikembalikan menjadi belum disetujui.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							if (krsMahasiswa != null) krsMahasiswa.masukkanData("batalkan");
							if (detailperkuliahansData != null && !detailperkuliahansData.isEmpty()) {
								lakukanPembatalanSemuaPersetujuan();
							}
						}
					}
				});
			}
		});
		tolakbutton.setParent(toolbar);

		MyToolbarbuttonConfig buttonKonversi = new MyToolbarbuttonConfig("Tambah Konversi", "/img/upload.gif");
		buttonKonversi.setOrient("vertical");
		buttonKonversi.setDisabled(!update);
		buttonKonversi.setVisible(semester.equals(0) && Common.getCurrentUser().getMahasiswa() == null);

		buttonKonversi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMatakuliahKonversiHelper ambilDataKonv = new AmbilDataMatakuliahKonversiHelper();
				MyWindow window = new MyWindow();
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				ambilDataKonv.display(mahasiswa, semester, tahunAjaran, new DataLoader() {
					@Override
					public void loadData(Object value) {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								StudiMahasiswaHelper.this.loadData(true);
							}
						});
					}
				}, window);
			}
		});
		buttonKonversi.setParent(toolbar);

		MyToolbarbuttonConfig buttonPaket = new MyToolbarbuttonConfig("Paket", "/img/svg/edit-box-line.svg");
		buttonPaket.setOrient("vertical");
		
		boolean isPaketVisible = update && semesterPendek != null && Common.getCurrentUser().getDosen() == null && Common.getCurrentUser().getMahasiswa() == null && tampilKonversi;
		buttonPaket.setVisible(isPaketVisible);
		
		buttonPaket.addEventListener("onClick", new EventListener() {
			private AmbilDataPaketPerkuliahanHelper ambilDataPaket = new AmbilDataPaketPerkuliahanHelper(semesterPendek);
			@Override
			public void onEvent(Event event) throws Exception {
				Konfigurasi konfigBayar = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_paket", Konfigurasi.AKTIF);
				if (Konfigurasi.AKTIF.equals(konfigBayar.getNilai())) {
					if (!(Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false, semesterPendek != null))) {
						MyMessageboxConfig.showFormat("Mahasiswa dengan NIM \"{V1}\" dan nama \"{V2}\" belum membayar biaya perkuliahan pada semester {V3}{V4}. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah menyelesaikan pembayaran biaya perkuliahan; (2) lakukan konfirmasi pembayaran ke bagian Keuangan; (3) apabila pembayaran telah dilakukan, mohon memperbarui status pembayaran mahasiswa.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNim(), mahasiswa.getNama(), semester, (tahapan != null && tahapan > 0 ? " tahap " + tahapan : ""));
						return;
					}
				}

				Konfigurasi konfigAktif = Common.getKonfigurasi("status_mahasiswa_harus_aktif_sebelum_isi_krs_paket", Konfigurasi.AKTIF);
				if (Konfigurasi.AKTIF.equals(konfigAktif.getNilai())) {
					StatusMahasiswa statusMhs = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahunAjaran, semester).getStatusMahasiswa();
					if (statusMhs == null || !statusMhs.getId().equals(ConstantValues.AKTIF.getId())) {
						MyMessageboxConfig.showFormat("Status Bapak/Ibu saat ini {V1}, sehingga KRS tidak dapat diambil. Langkah yang dapat dilakukan: (1) pastikan status Bapak/Ibu dalam keadaan aktif; (2) mohon menghubungi Administrator untuk informasi lebih lanjut.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, (statusMhs == null ? "" : statusMhs.getNama()));
						return;
					}
				}
				ambilDataPaket.display(mahasiswa, tahunAjaran, semester, StudiMahasiswaHelper.this);
			}
		});
		buttonPaket.setParent(toolbar);

		MyToolbarbuttonConfig buttonCetakKRS = new MyToolbarbuttonConfig("" + Common.getBahasa("label_krs"), "/img/print.png");
		buttonCetakKRS.setOrient("vertical");
		buttonCetakKRS.setVisible(semester > 0);
		buttonCetakKRS.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakKRS(mahasiswa, semester, tahapan, semesterPendek, remedial);
			}
		});
		buttonCetakKRS.setParent(toolbar);

		MyToolbarbuttonConfig buttonCetakNilai = new MyToolbarbuttonConfig("Nilai", "/img/print.png");
		buttonCetakNilai.setOrient("vertical");
		buttonCetakNilai.setVisible(semester > 0);
		buttonCetakNilai.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				CommonReportHelper.cetakNilai(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAjaran);
			}
		});
		buttonCetakNilai.setParent(toolbar);

		final Tbmuser currentUser = Common.getCurrentUser();

		final MyToolbarbuttonConfig buttonHapusSemua = new MyToolbarbuttonConfig("Semua Hapus", "/img/svg/trash.svg");
		buttonHapusSemua.setOrient("vertical");
		
		boolean isHapusSemuaVisible = Konfigurasi.AKTIF.equals(Common.getKonfigurasi("tampilkan_tombol_hapus_semua_di_krs", Konfigurasi.TIDAK_AKTIF).getNilai())
				&& semester != null && semester > 0 && currentUser != null && currentUser.ambilDosen() == null && currentUser.getMahasiswa() == null;
		buttonHapusSemua.setVisible(isHapusSemuaVisible);
		buttonHapusSemua.setDisabled(!delete);
		
		buttonHapusSemua.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus KRS ini? Data KRS yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							if (krsMahasiswa != null) krsMahasiswa.masukkanData("hapus_semua_krs");
							
							Session session = HibernateUtil.currentSession();
							for (Long detailperkuliahanid : detailperkuliahansData) {
								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
								if (detailperkuliahan != null) {
									session.refresh(detailperkuliahan);
									List<Komentar> komentars = session.createCriteria(Komentar.class).add(Restrictions.eq("detailperkuliahan", detailperkuliahan.getId())).list();
									for (Komentar komentar : komentars) {
										Common.refreshDelete(session, komentar);
									}
									Common.refreshDelete(session, detailperkuliahan);
								}
							}
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							});
						}
					}
				});
			}
		});
		buttonHapusSemua.setParent(toolbar);

		MyToolbarbuttonConfig buttonUTS = new MyToolbarbuttonConfig("UTS", "/img/print.png");
		buttonUTS.setOrient("vertical");
		
		boolean isUTSVisible = false;
		if (semester > 0 && currentUser != null && currentUser.ambilDosen() == null && currentUser.getMahasiswa() == null) {
			if (semesterPendek == null) {
				isUTSVisible = Konfigurasi.AKTIF.equals(Common.getKonfigurasi("tampilkan_tombol_cetak_kartu_uts", Konfigurasi.AKTIF).getNilai());
			} else {
				isUTSVisible = Konfigurasi.AKTIF.equals(Common.getKonfigurasi("tampilkan_tombol_cetak_kartu_uts_sp", Konfigurasi.AKTIF).getNilai());
			}
		}
		buttonUTS.setVisible(isUTSVisible);
		
		buttonUTS.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				boolean tanya = Konfigurasi.TIDAK_AKTIF.equals(Common.getKonfigurasi("tanya_tombol_cetak_kartu", Konfigurasi.AKTIF).getNilai());
				CommonReportHelper.cetakUTS(mahasiswa, semester, tahapan, tahunAjaran, semesterPendek, remedial, tanya);
			}
		});
		buttonUTS.setParent(toolbar);

		MyToolbarbuttonConfig buttonUAS = new MyToolbarbuttonConfig("UAS", "/img/print.png");
		buttonUAS.setOrient("vertical");
		
		boolean isUASVisible = false;
		if (semester > 0 && currentUser != null && currentUser.ambilDosen() == null && currentUser.getMahasiswa() == null) {
			if (semesterPendek == null) {
				isUASVisible = Konfigurasi.AKTIF.equals(Common.getKonfigurasi("tampilkan_tombol_cetak_kartu_uas", Konfigurasi.AKTIF).getNilai());
			} else {
				isUASVisible = Konfigurasi.AKTIF.equals(Common.getKonfigurasi("tampilkan_tombol_cetak_kartu_uas_sp", Konfigurasi.AKTIF).getNilai());
			}
		}
		buttonUAS.setVisible(isUASVisible);
		
		buttonUAS.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				boolean tanya = Konfigurasi.TIDAK_AKTIF.equals(Common.getKonfigurasi("tanya_tombol_cetak_kartu", Konfigurasi.AKTIF).getNilai());
				CommonReportHelper.cetakUAS(mahasiswa, semester, tahapan, tahunAjaran, semesterPendek, remedial, tanya);
			}
		});
		buttonUAS.setParent(toolbar);

		MyToolbarbuttonConfig buttonAktif = new MyToolbarbuttonConfig("Ket.Aktif", "/img/print.png");
		buttonAktif.setOrient("vertical");
		buttonAktif.setVisible(semester > 0);
		buttonAktif.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.prosesCetakKetAktif(mahasiswa, tahunAjaran, jenisSemester);
			}
		});
		toolbar.appendChild(buttonAktif);

		final MyToolbarbuttonConfig buttonKurikulum = new MyToolbarbuttonConfig("Kurikulum", "/img/excel.png");
		buttonKurikulum.setOrient("vertical");
		buttonKurikulum.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanKurikulumMahasiswa laporanKurikulum = new LaporanKurikulumMahasiswa(mahasiswa, semester);
				laporanKurikulum.setTitle("Riwayat Kurikulum");
				laporanKurikulum.setHeight("95%");
				laporanKurikulum.setWidth("90%");
				laporanKurikulum.setClosable(true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanKurikulum);
				laporanKurikulum.onModal();
			}
		});
		toolbar.appendChild(buttonKurikulum);

		final MyToolbarbuttonConfig buttonDownload = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		buttonDownload.setOrient("vertical");
		buttonDownload.setDisabled(!update);
		buttonDownload.setVisible(semester.equals(0) && Common.getCurrentUser().getMahasiswa() == null);
		buttonDownload.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PenilaianUtil.downloadPenilaianKonversi(detailperkuliahansData);
			}
		});
		buttonDownload.setParent(toolbar);

		final MyToolbarbuttonConfig buttonUpload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		buttonUpload.setOrient("vertical");
		buttonUpload.setDisabled(!update);
		buttonUpload.setVisible(semester.equals(0) && Common.getCurrentUser().getMahasiswa() == null);
		buttonUpload.setUpload(Common.ukuranFileUpload());
		
		buttonUpload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) return;
				
				if (media.getName().toLowerCase().endsWith("xlsx")) {
					InputStream inputStream = null;
					FileOutputStream fileOutputStream = null;
					try {
						inputStream = media.getStreamData();
						File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						file.getParentFile().mkdirs();
						fileOutputStream = new FileOutputStream(file);
						
						int c;
						while ((c = inputStream.read()) != -1) {
							fileOutputStream.write(c);
						}
					} finally {
						if (fileOutputStream != null) { try { fileOutputStream.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/StudiMahasiswaHelper.java:1563");} }
						if (inputStream != null) { try { inputStream.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/StudiMahasiswaHelper.java:1564");} }
					}

					File fileForPenilaian = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					PenilaianUtil.uploadPenilaianKonversi(mahasiswa, fileForPenilaian, tbmuser);

					MyMessageboxConfig.showFormatCb("Proses unggah nilai konversi telah berhasil dilakukan. Berkas: {V1}.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							display(mahasiswa, tahunAjaran, semester, tahapan, component, keterangan, komentarshtml, ipIpk, sksSksk, catatan, catatanKhs);
						}
					}, media);
				} else {
					MyMessageboxConfig.showFormat("Berkas yang Bapak/Ibu unggah harus berformat Excel Open XML Spreadsheet (xlsx). Berkas: {V1}. Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut; (2) pilih menu Save As dan simpan dalam format Excel Open XML Spreadsheet (xlsx); (3) unggah kembali berkas dengan format yang sesuai.", "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
				}
			}
		});
		buttonUpload.setParent(toolbar);

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder() && Konfigurasi.AKTIF.equals(Common.getKonfigurasi("aktifkan_terhubung_langsung_ke_feeder", Konfigurasi.AKTIF).getNilai())
				&& (mahasiswa != null && mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().isEmpty())) {

			MyToolbarbuttonConfig buttonFeeder = new MyToolbarbuttonConfig("Kirim ke Feeder", "/img/Finance-Invoice-icon.png");
			buttonFeeder.setOrient("vertical");
			buttonFeeder.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin mengirim data ini ke Feeder? Proses pengiriman akan dilakukan sesuai data yang tersedia.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								String[] kon = EksporFromFeederAction.koneksi();
								final String ip = kon[0];
								final String port = kon[1];
								final String username = kon[2];
								final String password = kon[3];
								final String url = kon[4];

								if (!EksporFromFeederAction.exists(url)) {
									ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons).");
									return;
								}

								final List<String> errorLog = new ArrayList<String>();
								final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										if (arg0 != null && !arg0.getName().isEmpty()) {
											EksporFromFeederAction.display();
											MyMessageboxConfig.show(arg0.getName(), "Info", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
										}

										if (!errorLog.isEmpty()) {
											StringBuilder err = new StringBuilder();
											for (String s : errorLog) {
												if (err.length() > 0) err.append("\n----------------------------------------------------------------------------------------------------------\n");
												err.append(s);
											}

											MyMessageboxConfig.show("Terjadi kesalahan pada proses ini. Catatan kesalahan akan otomatis terunduh. Langkah yang dapat dilakukan: (1) periksa catatan kesalahan yang terunduh; (2) perbaiki data sesuai catatan kesalahan; (3) apabila kendala berlanjut, mohon menghubungi Administrator sistem.", "Error Terjadi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											
											File file = new File("/opt/ecampus/error_" + Common.randLong() + ".txt");
											if (!file.getParentFile().exists()) {
												file.getParentFile().mkdirs();
											}
											FileUtils.writeStringToFile(file, err.toString());
											Filedownload.save(file, "text/plain");
										}
										loadData(true);
									}
								});

								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											FeederConnector feederConnector = new FeederConnector(ip, Integer.parseInt(port), null);
											String token = feederConnector.getToken(username, password);

											if (token == null || token.trim().isEmpty() || token.trim().toLowerCase().startsWith("error")) {
												myLabelProsesDetail.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
												return;
											}

											FeederExporter feederImporter = new FeederExporter(feederConnector, token, null, null, myLabelProsesDetail);
											int size = detailperkuliahansData.size();
											int index = 1;
											
											for (Long detailId : detailperkuliahansData) {
												Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailId.toString());
												if (detailperkuliahan != null) {
													Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
													if (perkuliahan != null) {
														myLabelProsesDetail.setValue("Memproses " + perkuliahan.info() + " (" + Common.numberFormat.get().format((index * 100.0) / size) + "%");
														index++;
														PerkuliahanAction.kirimKeFeeder(feederImporter, detailperkuliahan, feederConnector, token, mahasiswa, errorLog, true);
													} else if (detailperkuliahan.getMatakuliahKonversi() != null) {
														myLabelProsesDetail.setValue("Memproses " + detailperkuliahan.getMatakuliahKonversi().getNama() + " (" + Common.numberFormat.get().format((index * 100.0) / size) + "%");
														index++;
														feederImporter.nilaiTransfer(detailperkuliahan, errorLog);
													}
												}
											}
										// FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses.
										myLabelProsesDetail.setValue("");
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
										myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
												"pengiriman data KRS/perkuliahan mahasiswa ke Neo Feeder",
												null, e,
												new String[] {
														"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
														"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
														"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
												.replace("\n", " "));
									}
								}
							}).start();
							}
						}
					});
				}
			});
			toolbar.appendChild(buttonFeeder);

			MyToolbarbuttonConfig buttonKirimAKM = new MyToolbarbuttonConfig("Kirim AKM ke Feeder", "/img/Finance-Invoice-icon.png");
			buttonKirimAKM.setOrient("vertical");
			buttonKirimAKM.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin mengirim data ini ke Feeder? Proses pengiriman akan dilakukan sesuai data yang tersedia.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								String[] kon = EksporFromFeederAction.koneksi();
								final String ip = kon[0];
								final String port = kon[1];
								final String username = kon[2];
								final String password = kon[3];
								final String url = kon[4];

								if (!EksporFromFeederAction.exists(url)) {
									ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons).");
									return;
								}

								final List<String> errorLog = new ArrayList<String>();
								final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										if (arg0 != null && !arg0.getName().isEmpty()) {
											EksporFromFeederAction.display();
											MyMessageboxConfig.show(arg0.getName(), "Info", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
										}

										if (!errorLog.isEmpty()) {
											StringBuilder err = new StringBuilder();
											for (String s : errorLog) {
												if (err.length() > 0) err.append("\n----------------------------------------------------------------------------------------------------------\n");
												err.append(s);
											}

											MyMessageboxConfig.show(err.toString(), "Error Terjadi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											
											File file = new File(Common.REAL_PATH + "/tmp/error_" + Common.randLong() + ".txt");
											if (!file.getParentFile().exists()) {
												file.getParentFile().mkdirs();
											}
											FileUtils.writeStringToFile(file, err.toString());
											Filedownload.save(file, "text/plain");
										}
										loadData(true);
									}
								});

								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											FeederConnector feederConnector = new FeederConnector(ip, Integer.parseInt(port), myLabelProsesDetail);
											String token = feederConnector.getToken(username, password);

											if (token == null || token.trim().isEmpty() || token.trim().toLowerCase().startsWith("error")) {
												myLabelProsesDetail.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
												return;
											}

											FeederExporter feederImporter = new FeederExporter(feederConnector, token, null, null, null);
											myLabelProsesDetail.setValue("Mengirim data " + krsMahasiswa);
											MonitorKRSMahasiswaAction.kirimKeFeeder(feederImporter, feederConnector, token, krsMahasiswa, errorLog);
											// FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses.
											myLabelProsesDetail.setValue("");
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
													"pengiriman data AKM mahasiswa ke Neo Feeder",
													null, e,
													new String[] {
															"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
															"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
															"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
													.replace("\n", " "));
										}
									}
								}).start();
							}
						}
					});
				}
			});
			buttonKirimAKM.setParent(toolbar);
		}

		if (semester != null && semester > 0) {
			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder() && Konfigurasi.AKTIF.equals(Common.getKonfigurasi("aktifkan_terhubung_langsung_ke_feeder", Konfigurasi.AKTIF).getNilai())) {
				MyToolbarbuttonConfig buttonAmbilNilai = new MyToolbarbuttonConfig("Ambil Nilai", "/img/Finance-Invoice-icon.png");
				buttonAmbilNilai.setOrient("vertical");
				buttonAmbilNilai.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.show("Data nilai yang sudah diinputkan di sistem atau nilai mahasiswa lebih dari 0 tidak dapat diambil dari Feeder. Hanya perkuliahan yang belum dinilai yang dapat diambil dari Feeder.\nApakah Bapak/Ibu yakin ingin melanjutkan?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											String[] kon = EksporFromFeederAction.koneksi();
											final String ip = kon[0];
											final String port = kon[1];
											final String username = kon[2];
											final String password = kon[3];
											final String url = kon[4];

											if (!EksporFromFeederAction.exists(url)) {
												ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons).");
												return;
											}

											final List<String> errorLog = new ArrayList<String>();
											final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													if (arg0 != null && !arg0.getName().isEmpty()) {
														EksporFromFeederAction.display();
														MyMessageboxConfig.show(arg0.getName(), "Info", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
													}

													if (!errorLog.isEmpty()) {
														StringBuilder err = new StringBuilder();
														for (String s : errorLog) {
															if (err.length() > 0) err.append("\n----------------------------------------------------------------------------------------------------------\n");
															err.append(s);
														}

														MyMessageboxConfig.show("Terjadi kesalahan pada proses ini. Catatan kesalahan akan otomatis terunduh. Langkah yang dapat dilakukan: (1) periksa catatan kesalahan yang terunduh; (2) perbaiki data sesuai catatan kesalahan; (3) apabila kendala berlanjut, mohon menghubungi Administrator sistem.", "Error Terjadi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
														
														File file = new File("/opt/ecampus/error_" + Common.randLong() + ".txt");
														if (!file.getParentFile().exists()) {
															file.getParentFile().mkdirs();
														}
														FileUtils.writeStringToFile(file, err.toString());
														Filedownload.save(file, "text/plain");
													}
													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															loadData(true);
														}
													});
												}
											});

											new Thread(new Runnable() {
												@Override
												public void run() {
													try {
														FeederConnector feederConnector = new FeederConnector(ip, Integer.parseInt(port), null);
														String token = feederConnector.getToken(username, password);

														if (token == null || token.trim().isEmpty() || token.trim().toLowerCase().startsWith("error")) {
															myLabelProsesDetail.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}
														String idSmt = null;
														try {
															idSmt = StudiMahasiswaHelper.this.tahunAjaran.split("/")[0] +
																	(StudiMahasiswaHelper.this.semesterPendek != null && StudiMahasiswaHelper.this.semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK) ? "3" : (StudiMahasiswaHelper.this.semester % 2 == 0 ? "2" : "1"));
														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/StudiMahasiswaHelper.java:1837");
														}
														MahasiswaAction.ambilNilaiDariFeeder(feederConnector, token, 0, mahasiswa, tbmuser, idSmt);
														// FIX "gagal diam-diam": penanda sukses (setValue("")) dipindah ke akhir try agar exception di bawah tidak dianggap sukses; catch tengah yang menelan exception ambilNilaiDariFeeder dihapus.
														myLabelProsesDetail.setValue("");
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
														myLabelProsesDetail.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																"pengambilan data nilai mahasiswa dari Neo Feeder",
																null, e,
																new String[] {
																		"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																		"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																		"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																.replace("\n", " "));
													}
												}
											}).start();
										}
									}
								});
					}
				});
				toolbar.appendChild(buttonAmbilNilai);
			}
		}

		MyToolbarbuttonConfig buttonRefresh = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		buttonRefresh.setOrient("vertical");
		buttonRefresh.setTooltiptext("Refresh");
		buttonRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);
			}
		});
		buttonRefresh.setParent(toolbar);

		toolbar.setParent(groupbox);

		Vbox myvbox = new Vbox();
		myvbox.setParent(groupbox);
		myvbox.setVisible(edit);

		Hbox hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, krsMahasiswa.getId(), "UPLOAD_KRS_DISETUJUI", "KRS disetujui / di-tanda-tangani", false, null, null, false, false, false, true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);
		grid.setSclass("dgrid");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig columnHidden = new MyColumnConfig();
		columnHidden.setParent(columns);
		columnHidden.setWidth("45px");
		columnHidden.setVisible(false);

		MyColumnConfig columnSetujui = new MyColumnConfig();
		columnSetujui.setParent(columns);
		columnSetujui.setWidth("50px");
		columnSetujui.setLabel("Setujui");
		columnSetujui.setVisible(semester > 0);

		MyColumnConfig columnKodeAsal = new MyColumnConfig();
		columnKodeAsal.setParent(columns);
		columnKodeAsal.setLabel("Kode Asal");
		columnKodeAsal.setWidth("7%");
		columnKodeAsal.setVisible(semester == 0 && (tahapan == null || !tahapan.equals(-1)));

		MyColumnConfig columnNamaAsal = new MyColumnConfig();
		columnNamaAsal.setParent(columns);
		columnNamaAsal.setLabel("Nama Asal");
		columnNamaAsal.setWidth("10%");
		columnNamaAsal.setVisible(semester == 0 && (tahapan == null || !tahapan.equals(-1)));

		MyColumnConfig columnSksAsal = new MyColumnConfig();
		columnSksAsal.setParent(columns);
		columnSksAsal.setLabel("SKS Asal");
		columnSksAsal.setWidth("5%");
		columnSksAsal.setVisible(semester == 0 && (tahapan == null || !tahapan.equals(-1)));

		MyColumnConfig columnNilaiAsal = new MyColumnConfig();
		columnNilaiAsal.setParent(columns);
		columnNilaiAsal.setLabel("Nilai Asal");
		columnNilaiAsal.setWidth("5%");
		columnNilaiAsal.setVisible(semester == 0 && (tahapan == null || !tahapan.equals(-1)));

		MyColumnConfig columnKode = new MyColumnConfig();
		columnKode.setParent(columns);
		columnKode.setLabel("Kode");
		columnKode.setWidth("10%");

		MyColumnConfig columnNama = new MyColumnConfig();
		columnNama.setParent(columns);
		columnNama.setLabel("Nama");

		MyColumnConfig columnSks = new MyColumnConfig();
		columnSks.setParent(columns);
		columnSks.setLabel("SKS");
		columnSks.setWidth(semester.equals(0) ? "8%" : "4%");

		MyColumnConfig columnDosen = new MyColumnConfig();
		columnDosen.setParent(columns);
		columnDosen.setLabel(Common.getBahasa("label_dosen"));
		columnDosen.setWidth("15%");
		columnDosen.setVisible(semester > 0 || (tahapan != null && tahapan.equals(-1)));

		MyColumnConfig columnJadwal = new MyColumnConfig();
		columnJadwal.setParent(columns);
		columnJadwal.setLabel("Hari/Jam/Ruang");
		columnJadwal.setWidth("16%");
		columnJadwal.setVisible(semester > 0 || (tahapan != null && tahapan.equals(-1)));

		MyColumnConfig columnSmt = new MyColumnConfig();
		columnSmt.setParent(columns);
		columnSmt.setLabel("Smt");
		columnSmt.setWidth(semester.equals(0) ? "8%" : "4%");

		MyColumnConfig columnTahap = new MyColumnConfig();
		columnTahap.setParent(columns);
		columnTahap.setLabel("Tahap");
		columnTahap.setWidth("4%");
		columnTahap.setVisible(ConstantValues.aktifkanTahapan);

		MyColumnConfig columnInternal = new MyColumnConfig();
		columnInternal.setParent(columns);
		columnInternal.setLabel("Internal");
		columnInternal.setWidth(semester.equals(0) ? "8%" : "0px");

		MyColumnConfig columnKelas = new MyColumnConfig();
		columnKelas.setParent(columns);
		columnKelas.setLabel("Kelas");
		columnKelas.setWidth("8%");
		columnKelas.setVisible(semester > 0 || (tahapan != null && tahapan.equals(-1)));

		MyColumnConfig columnSetuju = new MyColumnConfig();
		columnSetuju.setParent(columns);
		columnSetuju.setLabel("Setuju");
		columnSetuju.setWidth("5%");
		columnSetuju.setVisible(semester > 0 || (tahapan != null && tahapan.equals(-1)));

		MyColumnConfig columnTA = new MyColumnConfig();
		columnTA.setParent(columns);
		columnTA.setLabel("T/A");
		columnTA.setWidth("0%");
		columnTA.setVisible(semester > 0 || (tahapan != null && tahapan.equals(-1)));

		MyColumnConfig columnNilai = new MyColumnConfig();
		columnNilai.setParent(columns);
		columnNilai.setLabel("Nilai");
		columnNilai.setWidth("130px");

		MyColumnConfig columnAction = new MyColumnConfig();
		columnAction.setParent(columns);
		columnAction.setLabel("");
		columnAction.setWidth("8%");

		gridKomentar = new MyGrid();
		gridKomentar.setMold("paging");
		gridKomentar.setPageSize(20);
		gridKomentar.setParent(groupbox);

		jamBentrok.setVisible(semester > 0);
		gridKomentar.setVisible(semester > 0);

		Columns columnsKomentar = new Columns();
		columnsKomentar.setMenupopup("auto");
		columnsKomentar.setParent(gridKomentar);

		MyColumnConfig colKomentar = new MyColumnConfig();
		colKomentar.setParent(columnsKomentar);
		colKomentar.setLabel("Komentar");
		colKomentar.setWidth("70%");

		MyColumnConfig colOleh = new MyColumnConfig();
		colOleh.setParent(columnsKomentar);
		colOleh.setLabel("Oleh");

		MyColumnConfig colTanggal = new MyColumnConfig();
		colTanggal.setParent(columnsKomentar);
		colTanggal.setLabel("Tanggal");

		MyColumnConfig colActionKomentar = new MyColumnConfig();
		colActionKomentar.setParent(columnsKomentar);
		colActionKomentar.setLabel("");
		colActionKomentar.setWidth("10%");

		loadDataKomentar();

		jamBentrok.setParent(groupbox);
		loadData(null);

		if (semester != null && semester.equals(0) && Konfigurasi.AKTIF.equals(Common.getKonfigurasi("selain_admin_tidak_boleh_merubah_konversi", Konfigurasi.TIDAK_AKTIF).getNilai())) {
			try {
				if (!tbmuser.hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)) {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.freeze(groupbox, true);
						}
					});
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		// Dipanggil DI SINI (bukan di AktifitasKrsMahasiswaHelper.initCetak) agar
		// pindahkanIsiPanel memindahkan konten Tab-1 yang sudah terisi ke atas,
		// sehingga Tab pertama langsung tampil tanpa harus diklik dulu.
		if (tabbox != null) {
			ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, null);
		}
	}

	/** Memuat/menyegarkan grid komentar diskusi KRS ({@link #gridKomentar}) untuk konteks mahasiswa/semester/tahap/tahun-akademik/semester-pendek saat ini, memakai {@link Common.KomentarRenderer} bersama dengan callback penyegaran diri sendiri. */
	public void loadDataKomentar() {
		List<Komentar> komentars = Common.loadKomentarData(mahasiswa, semester, tahapan, tahunAjaran, semesterPendek);
		ListModel strset = new SimpleListModel(komentars);

		gridKomentar.setRowRenderer(new Common.KomentarRenderer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataKomentar();
			}
		}));
		gridKomentar.setModelCheckMobile(strset);
		gridKomentar.renderAll();
		gridKomentar.setOddRowSclass("non-odd");
	}
}
