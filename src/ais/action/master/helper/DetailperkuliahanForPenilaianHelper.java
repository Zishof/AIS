package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.MahasiswaAction;
import ais.action.master.dashboard.admin.RekapHasilTugasKelompokPerVoPertemuan;
import ais.action.master.dashboard.admin.RekapHasilTugasPerVoPertemuan;
import ais.action.master.dashboard.admin.RekapHasilUjianPerVoPertemuan;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.helper.util.NilaiLoader;
import ais.action.master.helper.util.PenilaianUtil;
import ais.action.master.helper.util.PerubahanNilaiListener;
import ais.action.report.Report;
import ais.action.report.helper.nilai.LaporanDaftarPrestasiBelajarWindow;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPenilaian;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisEvaluasi;
import ais.database.model.Jurusan;
import ais.database.model.KomentarPerkuliahan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper terfokus untuk detailperkuliahan for penilaian. Tipe ini membungkus satu variasi kecil
 * dari alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link DataLoader}. Implementasi konkret
 * bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil sebaiknya bergantung
 * pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code MyGrid
 * gridKomentar}, {@code Perkuliahan perkuliahan}, {@code List formatNilais}, {@code Konfigurasi konfigurasi},
 * {@code List statusPertemuan}, {@code EventListener onPerubahanNilai}, {@code Textbox nama};
 * pembacaan/pencarian ({@code loadData()}, {@code loadDataDetailAsisten()}, {@code loadDataKomentar()}); mutasi
 * data ({@code prosesDisplay()}); operasi domain lain ({@code tanamkanRekapKeTabpanel()}, {@code
 * displayAsistenMahasiswa()}, {@code display()}, {@code onLaporan()}, {@code onLaporan()}, {@code onLaporan()});
 * konfigurasi constructor: {@code editDisable}, {@code nilai0MasukPenghitungan}, {@code tbmuser}. Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class DetailperkuliahanForPenilaianHelper implements DataLoader {

	private MyGrid grid;
	private MyGrid gridKomentar;

	private Perkuliahan perkuliahan;
	private List<FormatNilai> formatNilais;
	private Konfigurasi konfigurasi;

	private List<String> statusPertemuan;

	private EventListener onPerubahanNilai;
	private Textbox nama;

	private Dosen dosen;
	private Boolean aktifPenilaian = false;
	private boolean edit = false;
	private MyCheckboxConfig nilai0masukNilaiAkhir;
	private MyCheckboxConfig jikaNilai0masukNilaiAkhir;
	private boolean nilai0MasukPenghitungan;
	private Tbmuser tbmuser;
	private boolean adaProsesVerifikasiNilai = false;

	private boolean mahasiswaBolehUbahNilai = false;

	private MyCheckboxConfig hanyaInputNilaiHuruf;
	private MyCheckboxConfig sembunyikanNilaiJikaBelumDiverifikasi;

	// private boolean delete = false;

	private boolean editDisable = false;

	public DetailperkuliahanForPenilaianHelper(boolean edit) {

		this.edit = edit;

		if (Common.bolehKonfigurasi("hanya_dosen_yg_boleh_entry_nilai", Konfigurasi.TIDAK_AKTIF)) {
			tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.ambilDosen() == null) {
				editDisable = true;
			}
		}

		System.out.println("editDisable -> " + editDisable);

		nilai0MasukPenghitungan = Common.bolehKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir");

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link DetailperkuliahanForPenilaianHelper}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DetailperkuliahanForPenilaianHelper} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean aturanUts}, {@code boolean
	 * aturanUas}, {@code String statusPertemuanUts}, {@code String statusPertemuanUas}; operasi lokal: {@code
	 * checkWarningUts()}, {@code checkWarningUas()}, {@code render}(). Aturan bisnis bersama tetap berada pada
	 * kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DetailperkuliahanForPenilaianHelper
	 */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		private boolean aturanUts = Common.bolehKonfigurasi("aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uts", Konfigurasi.TIDAK_AKTIF);
		private boolean aturanUas = Common.bolehKonfigurasi("aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uas", Konfigurasi.TIDAK_AKTIF);

		private String statusPertemuanUts = Common.getKonfigurasi(
				"status_pertemuan_aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uts",
				"").getNilai();
		private String statusPertemuanUas = Common.getKonfigurasi(
				"status_pertemuan_aturan_batas_maksimal_tidak_masuk_kuliah_ini_juga_berlaku_saat_proses_penilaian_uas",
				"").getNilai();

		private String checkWarningUts(Detailperkuliahan detailperkuliahan, Map<String, Integer> statuses) {
			String warning = "";
			Integer semester = detailperkuliahan.getSemester();
			Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			if (aturanUts) {
				String ujian = "uts";
				int maxAlpa = 34;
				try {
					maxAlpa = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_alpa_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:174");

				}
				int maxSakit = 34;
				try {
					maxSakit = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_sakit_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:186");

				}
				int maxIzin = 34;
				try {
					maxIzin = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_izin_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:198");

				}

				int maxSemua = 34;
				try {
					maxSemua = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_semua_tidak_masuk_kuliah_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:211");

				}

				int maxPersen = 0;
				try {
					maxPersen = Integer.parseInt(Common.getKonfigurasi(
							"batas_maksimal_persen_tidak_masuk_kuliah_untuk_mengikuti_" + ujian.toLowerCase(), "0",
							semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
							mahasiswa.getStatusAwalMahasiswa()).getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:221");

				}

				if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {

					int semua = 0;

					int qtyAlpa = statuses.containsKey("A") ? statuses.get("A") : 0;
					semua += qtyAlpa;
					if (qtyAlpa >= maxAlpa) {
						warning += "Status Kehadiran A (Alpa) = " + qtyAlpa + " kali";
					}

					int qtySakit = statuses.containsKey("S") ? statuses.get("S") : 0;
					semua += qtySakit;
					if (qtySakit >= maxSakit) {
						warning += "Status Kehadiran S (Sakit) = " + qtySakit + " kali";
					}

					int qtyIzin = statuses.containsKey("I") ? statuses.get("I") : 0;
					semua += qtyIzin;
					if (qtyIzin >= maxIzin) {
						warning += "Status Kehadiran I (Izin) = " + qtyIzin + " kali";
					}

					if (semua >= maxSemua) {
						warning += "Tidak hadir kuliah = " + semua + " kali";
					}

					double persen = (semua * 100.0) / detailperkuliahan.getPerkuliahan().getJumlahMaksimalPertemuan();

					if (persen > maxPersen) {
						warning += "\n\nPerkuliahan " + detailperkuliahan.getPerkuliahan().toString()
								+ " => Persen tidak hadir kuliah = " + Common.numberFormat.get().format(persen) + "%";
					}
				}
			}

			return warning;
		}

		private String checkWarningUas(Detailperkuliahan detailperkuliahan, Map<String, Integer> statuses) {
			String warning = "";
			Integer semester = detailperkuliahan.getSemester();
			Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			if (aturanUas) {
				String ujian = "uas";
				int maxAlpa = 34;
				try {
					maxAlpa = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_alpa_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:278");

				}
				int maxSakit = 34;
				try {
					maxSakit = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_sakit_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:290");

				}
				int maxIzin = 34;
				try {
					maxIzin = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_tidak_masuk_kuliah_karena_izin_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:302");

				}

				int maxSemua = 34;
				try {
					maxSemua = Integer.parseInt(Common
							.getKonfigurasi(
									"batas_maksimal_jumlah_semua_tidak_masuk_kuliah_untuk_mengikuti_"
											+ ujian.toLowerCase(),
									"34", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
									mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa())
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:315");

				}

				int maxPersen = 0;
				try {
					maxPersen = Integer.parseInt(Common.getKonfigurasi(
							"batas_maksimal_persen_tidak_masuk_kuliah_untuk_mengikuti_" + ujian.toLowerCase(), "0",
							semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
							mahasiswa.getStatusAwalMahasiswa()).getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:325");

				}

				if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {

					int semua = 0;

					int qtyAlpa = statuses.containsKey("A") ? statuses.get("A") : 0;
					semua += qtyAlpa;
					if (qtyAlpa >= maxAlpa) {
						warning += "Status Kehadiran A (Alpa) = " + qtyAlpa + " kali";
					}

					int qtySakit = statuses.containsKey("S") ? statuses.get("S") : 0;
					semua += qtySakit;
					if (qtySakit >= maxSakit) {
						warning += "Status Kehadiran S (Sakit) = " + qtySakit + " kali";
					}

					int qtyIzin = statuses.containsKey("I") ? statuses.get("I") : 0;
					semua += qtyIzin;
					if (qtyIzin >= maxIzin) {
						warning += "Status Kehadiran I (Izin) = " + qtyIzin + " kali";
					}

					if (semua >= maxSemua) {
						warning += "Tidak hadir kuliah = " + semua + " kali";
					}

					double persen = (semua * 100.0) / detailperkuliahan.getPerkuliahan().getJumlahMaksimalPertemuan();

					if (persen > maxPersen) {
						warning += "\n\nPerkuliahan " + detailperkuliahan.getPerkuliahan().toString()
								+ " => Persen tidak hadir kuliah = " + Common.numberFormat.get().format(persen) + "%";
					}
				}
			}
			return warning;
		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");

			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, data.toString());

			detailperkuliahan.getPerkuliahan();
			Map<String, Integer> statuses = Perkuliahan.hitungStatus(statusPertemuan,
					detailperkuliahan.getMahasiswa().getId());

			CommonMedia.tampilkanGambarKecil(detailperkuliahan.getMahasiswa()).setParent(row);

			Vbox vbox = RevisiHelper.createNewRevisi(Detailperkuliahan.class, detailperkuliahan,
					detailperkuliahan.getMahasiswa().getNim());
			new Label(detailperkuliahan.getMahasiswa().getNama()).setParent(vbox);
			vbox.setParent(row);

			String warningUts = checkWarningUts(detailperkuliahan, statuses);
			if (!warningUts.trim().isEmpty()) {
				new ais.ui.util.MyHtml("<font style=\"font-weight:bold;color:red;font-size: 9px;\">"
						+ warningUts.replaceAll("\n", "<br>") + "</font>").setParent(vbox);
			}
			String warningUas = checkWarningUas(detailperkuliahan, statuses);
			if (!warningUas.trim().isEmpty()) {
				new ais.ui.util.MyHtml("<font style=\"font-weight:bold;color:red;font-size: 9px;\">"
						+ warningUas.replaceAll("\n", "<br>") + "</font>").setParent(vbox);
			}

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);
			if (Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
				if (detailperkuliahan.getFeeder() != null && !detailperkuliahan.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}

			tbmuser = Common.getCurrentUser();
			DetailperkuliahanHelper.kirimKeFeeder(tbmuser, detailperkuliahan, DetailperkuliahanForPenilaianHelper.this,
					myHbox, false);

			new Label(detailperkuliahan.getSemester() + "").setParent(row);

			vbox = new Vbox();
			vbox.setParent(row);

			int semua = statuses.get("T") == null ? 0 : statuses.get("T");

			Hbox hbox = new Hbox();
			vbox.appendChild(hbox);
			for (String key : statuses.keySet()) {
				if (!key.equals("T")) {
					int v = statuses.get(key);
					hbox.appendChild(new MyLabelAgakKecil(key + "=" + v + ","));
				}
			}

			hbox.appendChild(new MyLabelAgakKecil("T=" + semua));

			double persen = detailperkuliahan.hitungPersenKehadiran();

			vbox.appendChild(new MyLabelAgakKecil("Presensi = " + Common.numberFormat.get().format(persen) + "%"));

			final Label label = new Label(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
					+ detailperkuliahan.getNilaiHuruf() + ")");
			label.setStyle("cursor:pointer;text-decoration:underline;color:#0b63ce;font-weight:bold;");
			label.setTooltiptext("Klik untuk melihat analisis nilai huruf");
			label.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tampilkanAnalisisNilaiHuruf(detailperkuliahan);
				}
			});
			final MyCheckboxConfig verifyAll = new MyCheckboxConfig();
			final List<PerubahanNilaiListener> checkboxs = new ArrayList<PerubahanNilaiListener>();
			for (FormatNilai formatNilai : formatNilais) {
				MyCheckboxConfig verify = new MyCheckboxConfig();
				if (editDisable || (perkuliahan.getDikunci() != null || !edit || formatNilai.getKunci() != null
						|| (formatNilai.getStatusPertemuan() != null && formatNilai.getStatusPertemuan().getKunci())
						|| (!warningUts.trim().isEmpty() && statusPertemuanUts.trim().isEmpty())

						|| (!warningUas.trim().isEmpty() && statusPertemuanUas.trim().isEmpty())

						|| (tbmuser.getMahasiswa() != null && !mahasiswaBolehUbahNilai) ||

						(mhsYgBelumBayarBelumBisaDiEntryNilai
								&& !PenilaianMahasiswaHelper.checkBolehLihatNilai(detailperkuliahan.getMahasiswa(),
										detailperkuliahan.getSemester()))

						|| (!aktifPenilaian && (konfigurasi.getNilai() == null
								|| !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))))) {

					MyDoublebox doublebox = new MyDoublebox();
					NilaiLoader.startLoad(detailperkuliahan, formatNilai, doublebox);
					final PerubahanNilaiListener perubahanNilaiListener = new PerubahanNilaiListener(detailperkuliahan,
							formatNilai, formatNilais, onPerubahanNilai, label, doublebox, verify);

					Label myLabel = new Label(ais.common.Common.getBahasaConfig("Load.."));
					myLabel.setStyle("text-align: right;");
					NilaiLoader.startLoad(detailperkuliahan, formatNilai, myLabel);
					if (!adaProsesVerifikasiNilai) {
						myLabel.setParent(row);
					} else {
						verify.setChecked(detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai));
						checkboxs.add(perubahanNilaiListener);
						hbox = new Hbox();
						hbox.setWidth("95%");
						hbox.setParent(row);
						myLabel.setParent(hbox);
						verify.setParent(hbox);
						if (perkuliahan.getDikunci() != null
								|| !perkuliahan.getDosenBolehVerifikasiNilaiSendiri() && dosen != null) {
							verify.setDisabled(true);
						} else {
							verify.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									try {

										Session session = HibernateUtil.currentNativeSession();
										session.refresh(detailperkuliahan);
										Boolean checkSemua = true;
										for (PerubahanNilaiListener perubahanNilaiListener : checkboxs) {
											if (!perubahanNilaiListener.getVerify().isChecked()) {
												checkSemua = false;
												break;
											}

											if (perubahanNilaiListener.getDoublebox() == null) {
												checkSemua = false;
												break;
											}
										}
										verifyAll.setChecked(checkSemua);
										detailperkuliahan.setVerify(verifyAll.isChecked() ? Detailperkuliahan.VERIFIED
												: Detailperkuliahan.NOT_VERIFIED);
										Tbmuser tbmuser = Common.getCurrentUser();
										detailperkuliahan
												.setVerifikator(tbmuser.getUserId() + " " + tbmuser.getUserNama());
										detailperkuliahan.setWaktuVerifikasi(ais.ui.util.WaktuUtil.getDate());
										session.getTransaction().begin();
										Common.refreshUpdate(session, detailperkuliahan);
										session.getTransaction().commit();
										// session.disconnect();
										if (session.isOpen()) {
											session.disconnect();
											session.close();
										}

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:515");
									}
									HibernateUtil.closeSession();

									Common.createDefaultTimer(perubahanNilaiListener);
								}
							});
						}
					}

				} else {

					MyDoublebox doublebox = new MyDoublebox();
					final PerubahanNilaiListener perubahanNilaiListener = new PerubahanNilaiListener(detailperkuliahan,
							formatNilai, formatNilais, onPerubahanNilai, label, doublebox, verify);

					/* width:85% !important agar kotak nilai mengikuti lebar kolom (lihat MyDoublebox). */
					doublebox.setStyle("text-align: right; width:85% !important;");
					doublebox.setWidth("95%");
					if (!adaProsesVerifikasiNilai) {

						if ((!warningUts.trim().isEmpty() && statusPertemuanUts.trim()
								.equalsIgnoreCase(formatNilai.getStatusPertemuan().getNama()))
								|| (!warningUas.trim().isEmpty() && statusPertemuanUas.trim()
										.equalsIgnoreCase(formatNilai.getStatusPertemuan().getNama()))) {
							Label myLabel = new Label(ais.common.Common.getBahasaConfig("Load.."));
							myLabel.setStyle("text-align: right;");
							NilaiLoader.startLoad(detailperkuliahan, formatNilai, myLabel);
							myLabel.setParent(row);
						} else {
							doublebox.setParent(row);
						}

					} else {
						verify.setChecked(detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai));
						checkboxs.add(perubahanNilaiListener);
						hbox = new Hbox();
						hbox.setWidth("95%");
						hbox.setParent(row);

						if ((!warningUts.trim().isEmpty() && statusPertemuanUts.trim()
								.equalsIgnoreCase(formatNilai.getStatusPertemuan().getNama()))
								|| (!warningUas.trim().isEmpty() && statusPertemuanUas.trim()
										.equalsIgnoreCase(formatNilai.getStatusPertemuan().getNama()))) {
							Label myLabel = new Label(ais.common.Common.getBahasaConfig("Load.."));
							myLabel.setStyle("text-align: right;");
							NilaiLoader.startLoad(detailperkuliahan, formatNilai, myLabel);
							myLabel.setParent(hbox);
						} else {
							doublebox.setParent(hbox);
						}
						verify.setParent(hbox);
						if (perkuliahan.getDikunci() != null
								|| !perkuliahan.getDosenBolehVerifikasiNilaiSendiri() && dosen != null) {
							verify.setDisabled(true);
						} else {
							verify.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									try {
										Session session = HibernateUtil.currentNativeSession();
										session.refresh(detailperkuliahan);
										Boolean checkSemua = true;
										for (PerubahanNilaiListener perubahanNilaiListener : checkboxs) {
											if (!perubahanNilaiListener.getVerify().isChecked()) {
												checkSemua = false;
												break;
											}

											if (perubahanNilaiListener.getDoublebox() == null) {
												checkSemua = false;
												break;
											}
										}
										verifyAll.setChecked(checkSemua);
										detailperkuliahan.setVerify(verifyAll.isChecked() ? Detailperkuliahan.VERIFIED
												: Detailperkuliahan.NOT_VERIFIED);
										Tbmuser tbmuser = Common.getCurrentUser();
										detailperkuliahan
												.setVerifikator(tbmuser.getUserId() + " " + tbmuser.getUserNama());
										detailperkuliahan.setWaktuVerifikasi(ais.ui.util.WaktuUtil.getDate());
										session.getTransaction().begin();
										Common.refreshUpdate(session, detailperkuliahan);
										session.getTransaction().commit();
										// session.disconnect();
										if (session.isOpen()) {
											session.disconnect();
											session.close();
										}

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:607");
									}
									HibernateUtil.closeSession();

									Common.createDefaultTimer(perubahanNilaiListener);
								}
							});
						}
					}

					doublebox.setDisabled((!edit || !aktifPenilaian)
							&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)));
					doublebox.addEventListener("onChange", perubahanNilaiListener);
					NilaiLoader.startLoad(detailperkuliahan, formatNilai, doublebox);
				}
			}

			for (Column column : columns) {
				column.setVisible(!perkuliahan.getHanyaInputNilaiHuruf());
			}

			if (perkuliahan.getHanyaInputNilaiHuruf()) {
				columnMahasiswa.setWidth("85%");

				if (perkuliahan.getDikunci() != null || !edit
						|| (!warningUts.trim().isEmpty() && statusPertemuanUts.trim().isEmpty())

						|| (!warningUas.trim().isEmpty() && statusPertemuanUas.trim().isEmpty())

						|| (tbmuser.getMahasiswa() != null && !mahasiswaBolehUbahNilai) ||

						(!aktifPenilaian && (konfigurasi.getNilai() == null
								|| !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)))) {

					new Label(detailperkuliahan.getNilaiHuruf()).setParent(row);

				} else {

					final Textbox nilaiHurufText = new Textbox(detailperkuliahan.getNilaiHuruf());
					nilaiHurufText.setWidth("95%");
					nilaiHurufText.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Double nilai = 0.0;
							// GATE SP (semester pendek): tolak entry nilai bila pembayaran SP mahasiswa belum lunas.
							String alasanSpNilaiHuruf = ais.action.master.helper.util.GateBayarSpUtil.alasanBlokir(detailperkuliahan);
							if (alasanSpNilaiHuruf != null) {
								try {
									ais.ui.util.MyMessageboxConfig.show(alasanSpNilaiHuruf, "Peringatan", ais.ui.util.MyMessageboxConfig.OK,
											ais.ui.util.MyMessageboxConfig.EXCLAMATION);
								} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:658");
								}
								return;
							}
							Session session = HibernateUtil.currentNativeSession();
							session.refresh(detailperkuliahan);
							NilaiHuruf nilaiHuruf = null;

							for (NilaiHuruf huruf : ConstantValues.nilaiHurufs) {
								if (huruf != null && huruf.getNilaiHuruf() != null
										&& huruf.getNilaiHuruf().equalsIgnoreCase(nilaiHurufText.getValue().trim())
										&& huruf.getJurusan() != null && huruf.getJurusan().getId() != null
										&& huruf.getJurusan().getId()
												.equals(detailperkuliahan.getMahasiswa().getJurusan().getId())) {
									nilaiHuruf = huruf;
									break;
								}
							}

							if (nilaiHuruf == null) {
								for (NilaiHuruf huruf : ConstantValues.nilaiHurufs) {
									if (huruf != null && huruf.getNilaiHuruf() != null
											&& huruf.getNilaiHuruf().equalsIgnoreCase(nilaiHurufText.getValue().trim())
											&& huruf.getFakultas() != null && huruf.getFakultas().getId() != null
											&& huruf.getFakultas().getId().equals(detailperkuliahan.getMahasiswa()
													.getJurusan().getFakultas().getId())) {
										nilaiHuruf = huruf;
										break;
									}
								}
							}

							if (nilaiHuruf == null) {
								for (NilaiHuruf huruf : ConstantValues.nilaiHurufs) {
									if (huruf != null && huruf.getNilaiHuruf() != null && huruf.getNilaiHuruf()
											.equalsIgnoreCase(nilaiHurufText.getValue().trim())) {
										nilaiHuruf = huruf;
										break;
									}
								}
							}

							if (nilaiHuruf != null) {
								nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
							}
							for (FormatNilai formatNilai : formatNilais) {
								detailperkuliahan.populateDetailNilai(formatNilai, null, nilai,
										detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai),
										perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(), tbmuser);
							}
							detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
							detailperkuliahan.setTotalNilai(nilai);
							detailperkuliahan.setNilaiHuruf(nilaiHurufText.getValue().trim());

							Matakuliah matakuliah = detailperkuliahan == null ? null
									: detailperkuliahan.getPerkuliahan() != null
											? detailperkuliahan.getPerkuliahan().getMatakuliah()
											: detailperkuliahan.getMatakuliahKonversi();

							Double totalSementara = nilai;
							nilaiHuruf = Common.getNilaiHuruf(totalSementara,
									detailperkuliahan.getMahasiswa().getTahunangkatan(),
									detailperkuliahan.getMahasiswa().getJurusan(),
									detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
									detailperkuliahan.getTahunAkademik(),
									detailperkuliahan.getPerkuliahan() == null ? null
											: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
									matakuliah == null ? "" : matakuliah.getKode(),
									matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

							detailperkuliahan.setTotalNilaiSementara(totalSementara);
							detailperkuliahan
									.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
							detailperkuliahan
									.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

							session.getTransaction().begin();
							Common.refreshUpdate(session, detailperkuliahan);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
							HibernateUtil.closeSession();
						}
					});
					nilaiHurufText.setParent(row);
				}
			} else {
				columnMahasiswa.setWidth((75 - (formatNilais.size() * 5)) + "%");
				if (perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					label.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilaiSementara()) + " ("
							+ detailperkuliahan.getNilaiHurufSementara() + ")");
				} else {
					label.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
							+ detailperkuliahan.getNilaiHuruf() + ")");
				}

				// SEL TOTAL: label nilai + (bila perlu) peringatan merah digabung dalam SATU sel.
				// Sebelumnya peringatan di-setParent(row) sebagai SEL TAMBAHAN di ujung baris —
				// jatuh ke kolom sempit sehingga teks berdesakan tak terbaca (laporan dosen 19-08).
				org.zkoss.zul.Vbox selTotal = new org.zkoss.zul.Vbox();
				selTotal.setStyle("width:100%;");
				label.setParent(selTotal);

				// PERINGATAN MERAH: bila Nilai Total = 0 PADAHAL komponen sudah di-entry, jelaskan
				// penyebabnya (komponen terkunci ber-snapshot 0, bobot persen 0/kosong, kehadiran
				// di bawah minimal, atau aturan "nilai 0 tak dihitung"). Membantu dosen paham
				// kenapa nilai akhir "tidak sesuai" dan APA tindakannya.
				try {
					double totalTampil = (perkuliahan != null
							&& perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
							&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED))
									? detailperkuliahan.getTotalNilaiSementara()
									: detailperkuliahan.getTotalNilai();
					if (totalTampil < 0.01) {
						String alasan = detailperkuliahan.alasanNilaiJadiNol(true, formatNilais);
						if (alasan != null && !alasan.trim().isEmpty()) {
							org.zkoss.zul.Label peringatan = new org.zkoss.zul.Label(alasan);
							peringatan.setMultiline(true);
							peringatan.setStyle(
									"color:#c62828;font-weight:bold;font-size:10px;line-height:1.35;display:block;"
											+ "margin-top:3px;white-space:normal;word-wrap:break-word;max-width:230px;");
							peringatan.setParent(selTotal);
						}
					}
				} catch (Exception eWarn) {
					Common.tampilErrorJikaAdmin(eWarn);
				}
				selTotal.setParent(row);
			}

			if (perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir() == null) {
				perkuliahan.setNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir(nilai0MasukPenghitungan);
			}

			nilai0masukNilaiAkhir.setChecked(perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir());
			jikaNilai0masukNilaiAkhir.setChecked(perkuliahan.getJikaAdaNilai0TidakMenghitungNilaiAkhir());
			hanyaInputNilaiHuruf.setChecked(perkuliahan.getHanyaInputNilaiHuruf());

			if (dosen == null || (dosen != null && perkuliahan.getDosenBolehVerifikasiNilaiSendiri())) {

				verifyAll.setChecked(detailperkuliahan.getVerify().equals(Detailperkuliahan.VERIFIED));
				verifyAll.setParent(row);
				verifyAll.setAttribute("janganDisabled", true);
				verifyAll.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {

							for (PerubahanNilaiListener perubahanNilaiListener : checkboxs) {
								perubahanNilaiListener.getVerify().setChecked(verifyAll.isChecked());
								perubahanNilaiListener.process();
							}

							Session session = HibernateUtil.currentNativeSession();
							session.refresh(detailperkuliahan);
							detailperkuliahan.setVerify(verifyAll.isChecked() ? Detailperkuliahan.VERIFIED
									: Detailperkuliahan.NOT_VERIFIED);
							Tbmuser tbmuser = Common.getCurrentUser();
							detailperkuliahan.setVerifikator(tbmuser.getUserId() + " " + tbmuser.getUserNama());
							detailperkuliahan.setWaktuVerifikasi(ais.ui.util.WaktuUtil.getDate());
							session.getTransaction().begin();
							Common.refreshUpdate(session, detailperkuliahan);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:825");
						}
						HibernateUtil.closeSession();
					}
				});
			} else {

				Label labelVerifikasi;
				(labelVerifikasi = new Label(
						detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED) ? "Belum" : "Ya"))
						.setParent(row);
				labelVerifikasi.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
			}
		}

	}

	private Collection<Long> detailperkuliahans;

	public void loadData(Object value) {
		boolean refresh = (value != null && value.equals(true));
		detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, nama.getValue().trim(),
				urutkanBerdasarkanNama.isChecked(), refresh);

		List<Long> baru = new ArrayList<Long>();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
					baru.add(detailperkuliahan.getId());
				}
			}
		}
		ListModel strset = new SimpleListModel(baru);
		grid.setSclass("fgrid");
		grid.setRowRenderer(new DetailPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

		Common.freeze(grid, perkuliahan.getDikunci() != null);
	}

	public static void loadDataDetailAsisten(Object value, final Perkuliahan perkuliahan,
			final MyGrid gridDetailAsisten, boolean refresh) {

		if (refresh) {
			perkuliahan.belum("mahasiswaJadiAsisten");
		}

		List<MahasiswaJadiAsisten> mahasiswaJadiAsistens = perkuliahan.ambilMahasiswaJadiAsisten();

		ListModel strset = new SimpleListModel(mahasiswaJadiAsistens);
		gridDetailAsisten.setRowRenderer(new ais.ui.util.MyRowRenderer() {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				final MahasiswaJadiAsisten mahasiswaJadiAsisten = (MahasiswaJadiAsisten) arg1;

				RevisiHelper.createNewRevisi(MahasiswaJadiAsisten.class, mahasiswaJadiAsisten,
						mahasiswaJadiAsisten.getMahasiswa().getNim()).setParent(arg0);
				new Label(mahasiswaJadiAsisten.getMahasiswa().getNama()).setParent(arg0);

				final MyCheckboxConfig inputNilai = new MyCheckboxConfig("Nilai");
				inputNilai.setChecked(mahasiswaJadiAsisten.getInputNilai());
				inputNilai.setParent(arg0);
				inputNilai.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaJadiAsisten.setInputNilai(inputNilai.isChecked());
						Common.refreshSaveOrUpdate(mahasiswaJadiAsisten);
					}
				});

				final MyCheckboxConfig inputAbsen = new MyCheckboxConfig("Absen");
				inputAbsen.setChecked(mahasiswaJadiAsisten.getInputAbsen());
				inputAbsen.setParent(arg0);
				inputAbsen.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaJadiAsisten.setInputAbsen(inputAbsen.isChecked());
						Common.refreshSaveOrUpdate(mahasiswaJadiAsisten);
					}
				});

				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(mahasiswaJadiAsisten.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaJadiAsisten.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(mahasiswaJadiAsisten);
					}
				});

				final Textbox keterangan = new Textbox(mahasiswaJadiAsisten.getKeterangan());
				keterangan.setParent(arg0);
				keterangan.setWidth("90%");
				keterangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaJadiAsisten.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(mahasiswaJadiAsisten);
					}
				});

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setOrient("vertical");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Common.refreshDelete(mahasiswaJadiAsisten);
												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadDataDetailAsisten(null, perkuliahan, gridDetailAsisten,
																true);
													}
												});
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
											}

										}

									}
								});

					}

				});
				button.setParent(arg0);
			}
		});
		gridDetailAsisten.setModelCheckMobile(strset);

		gridDetailAsisten.renderAll();

	}

	private List<Column> columns = new ArrayList<Column>();
	private MyColumnConfig columnMahasiswa;
	private MyCheckboxConfig urutkanBerdasarkanNama;
	private boolean adminBoleh = false;
	private Integer semester;
	private boolean mhsYgBelumBayarBelumBisaDiEntryNilai = false;

	/**
	 * Tanamkan isi rekap (toolbar + grafik + tabel) yang dibangun kelas
	 * {@code RekapHasil*PerVoPertemuan} LANGSUNG ke {@code target} (tabpanel),
	 * meniru pola Rekap Total Nilai (GradingHelper) yang BERHASIL tampil: borderlayout
	 * di-set parent ke tabpanel dengan TINGGI PASTI (520px), bukan dibiarkan di dalam
	 * MyWindow. Saat berada di tabpanel, borderlayout di dalam window collapse 0px
	 * sehingga konten tidak tampil; dengan ditanam langsung + tinggi pasti, konten
	 * ter-render seperti tab Rekap Total Nilai.
	 *
	 * @param windowRekap instance RekapHasil*PerVoPertemuan yang sudah membangun
	 *                    borderlayout di dalamnya (belum dilampirkan ke halaman)
	 * @param target      tabpanel tujuan tampilan
	 */
	private static void tanamkanRekapKeTabpanel(org.zkoss.zk.ui.Component windowRekap,
			org.zkoss.zk.ui.Component target) {
		try {
			for (Object anak : new java.util.ArrayList<Object>(windowRekap.getChildren())) {
				if (anak instanceof org.zkoss.zul.Borderlayout) {
					org.zkoss.zul.Borderlayout bl = (org.zkoss.zul.Borderlayout) anak;
					bl.setParent(target);
					bl.setWidth("100%");
					bl.setHeight("2000px");
				}
			}
		} catch (Throwable t) {
			Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
		}
	}

	public static void displayAsistenMahasiswa(org.zkoss.zk.ui.Component detailPenilaian, final Perkuliahan perkuliahan) {
		Common.clear(detailPenilaian);

		final MyGrid gridDetailAsisten = new MyGrid();
		Tbmuser tbmuser = Common.getCurrentUser();
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 300px;");
		groupbox.setParent(detailPenilaian);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.setVisible(tbmuser != null);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaForAsistenHelper dataMahasiswaHelper = new AmbilDataMahasiswaForAsistenHelper(
						perkuliahan);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetailAsisten(null, perkuliahan, gridDetailAsisten, true);
							}
						});
					}
				});
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.getFormatNilais(perkuliahan, true);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadDataDetailAsisten(null, perkuliahan, gridDetailAsisten, true);
					}
				});
			}

		});
		button.setParent(toolbar);

		gridDetailAsisten.setMold("paging");
		gridDetailAsisten.setPageSize(1000);
		gridDetailAsisten.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(gridDetailAsisten);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Input Nilai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Input Absen");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(tbmuser == null ? "0%" : "5%");

		loadDataDetailAsisten(null, perkuliahan, gridDetailAsisten, false);
	}

	public void display(final Perkuliahan kuliyah, final Component component, final EventListener onPerubahanNilai,
			final MyToolbarbuttonConfig buttonFormatNilai, boolean aktifPenilaianData) throws Exception {

		this.onPerubahanNilai = onPerubahanNilai == null ? this.onPerubahanNilai : onPerubahanNilai;
		this.perkuliahan = kuliyah.getMerupakan_paralel() && kuliyah.getPerkuliahan_paralel() != null
				? kuliyah.getPerkuliahan_paralel()
				: kuliyah;

		String jenisSemester = perkuliahan.getGanjilGenap();
		String tahunAkademik = perkuliahan.getTahunAjaran();

		tbmuser = Common.getCurrentUser() == null ? null : Common.getCurrentUser();

		dosen = tbmuser == null ? null : tbmuser.ambilDosen();

		semester = perkuliahan.getSemester();
		jenisSemester = perkuliahan.getGanjilGenap();

		aktifPenilaian = aktifPenilaianData;
		System.out.println("aktifPenilaian = " + aktifPenilaian);
		Konfigurasi konfigurasi = CommonPenilaian.getKonfigurasi(tahunAkademik, jenisSemester,
				perkuliahan.getStatusSemesterPendek());

		this.konfigurasi = konfigurasi;

		Collection<Long> temp = this.perkuliahan.ambilDetailperkuliahan();

		int adaygkosong = 0;
		for (Long detailperkuliahanid : temp) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getDetailNilai() == null || detailperkuliahan.getDetailNilai().trim().isEmpty()) {
					adaygkosong++;
				}
			}
		}
		temp = null;

		if (adaygkosong > 0) {
			Common.realoadNilaiLangsung(perkuliahan, perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, true);
						}
					}, detailperkuliahans);
		} else {
			prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, null);
		}
	}

	public void prosesDisplay(final Perkuliahan kuliyah, final Component component,
			final EventListener onPerubahanNilai, final MyToolbarbuttonConfig buttonFormatNilai, Boolean refresh) {

		mhsYgBelumBayarBelumBisaDiEntryNilai = Common.bolehKonfigurasi("mhs_yg_belum_bayar_belum_bisa_di_ntry_nilai", Konfigurasi.TIDAK_AKTIF);
		statusPertemuan = new ArrayList<String>();

		TreeMap<String, Long> pertemuans = perkuliahan.ambilPertemuan();
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				if (!pertemuan.getAbsensi().trim().isEmpty()) {
					statusPertemuan.add(pertemuan.getAbsensi());
				}
			}
		}

		Common.clear(component);
		final Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);

		// FIX ZK5: konten sub-tab Nilai yang dibangun LAZY (Rekap Tugas/Ujian/
		// Tugas Kelompok/Total Nilai/Prestasi) kadang tidak keluar — pada konteks
		// nested (Aktifitas Perkuliahan) event onClick tab tidak selalu memicu
		// pembangunan + render. Solusi andal lewat onSelect tabbox:
		//   1) bila panel terpilih masih KOSONG, picu onClick tab tsb untuk
		//      membangun kontennya (tidak rebuild bila sudah terisi → Input Nilai
		//      yang eager tetap aman),
		//   2) invalidate panel via timer agar konten yang baru dibangun
		//      benar-benar ter-render setelah event seleksi tuntas.
		tabbox.addEventListener("onSelect", new EventListener() {

			@Override
			public void onEvent(Event evtSelTab) throws Exception {
				try {
					org.zkoss.zul.Tab tabTerpilih = tabbox.getSelectedTab();
					org.zkoss.zul.Tabpanel panelTerpilih = tabbox.getSelectedPanel();
					if (panelTerpilih != null && panelTerpilih.getFirstChild() == null
							&& tabTerpilih != null) {
						org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", tabTerpilih, null));
					}
				} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1216");
				}
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event evtTimerTab) throws Exception {
						try {
							org.zkoss.zul.Tabpanel panelTerpilih = tabbox.getSelectedPanel();
							if (panelTerpilih != null) {
								panelTerpilih.invalidate();
							}
						} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1227");
						}
					}
				});
			}
		});

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig();
		tab1.setParent(tabs);
		tab1.setLabel("Input Nilai");

		MyTabConfig tab1AsistenMahasiswa = new MyTabConfig();
		tab1AsistenMahasiswa.setParent(tabs);
		tab1AsistenMahasiswa.setLabel("Asisten Dosen");
		tab1AsistenMahasiswa.setVisible(tbmuser.getMahasiswa() == null);
//
//		MyTabConfig tab1LihatRekapKehadiran = new MyTabConfig();
//		tab1LihatRekapKehadiran.setParent(tabs);
//		tab1LihatRekapKehadiran.setLabel("Rekap Kehadiran");

		MyTabConfig tab1LihatRekapTugas = new MyTabConfig();
		tab1LihatRekapTugas.setParent(tabs);
		tab1LihatRekapTugas.setLabel("Rekap Tugas");

		MyTabConfig tab1LihatRekapUjian = new MyTabConfig();
		tab1LihatRekapUjian.setParent(tabs);
		tab1LihatRekapUjian.setLabel("Rekap Ujian");

		MyTabConfig tab1LihatRekapTugasKelompok = new MyTabConfig();
		tab1LihatRekapTugasKelompok.setParent(tabs);
		tab1LihatRekapTugasKelompok.setLabel("Rekap Tugas Kelompok");

		MyTabConfig tab1LihatRekapNilai = new MyTabConfig();
		tab1LihatRekapNilai.setParent(tabs);
		tab1LihatRekapNilai.setLabel("Rekap Total Nilai");

		MyTabConfig tab1Prestasi = new MyTabConfig();
		tab1Prestasi.setParent(tabs);
		tab1Prestasi.setLabel("Prestasi Belajar");

//		MyTabConfig tab1PrestasiSemua = new MyTabConfig();
//		tab1PrestasiSemua.setParent(tabs);
//		tab1PrestasiSemua.setLabel("Prestasi Belajar Semua");

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel managemenPenilaian = new ais.ui.util.MyTabpanel();
		managemenPenilaian.setParent(tabpanels);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(managemenPenilaian);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang mengikuti perkuliahan " + perkuliahan.toString()));

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);

		nilai0masukNilaiAkhir = new MyCheckboxConfig("Nilai 0 tidak masuk pembagi nilai akhir");
		nilai0masukNilaiAkhir.setStyle("font-size:9px");
		jikaNilai0masukNilaiAkhir = new MyCheckboxConfig("Jika ada nilai 0 tidak menghitung nilai akhir");
		jikaNilai0masukNilaiAkhir.setStyle("font-size:9px");

		hanyaInputNilaiHuruf = new MyCheckboxConfig("Hanya input nilai huruf");
		hanyaInputNilaiHuruf.setStyle("font-size:9px");

		sembunyikanNilaiJikaBelumDiverifikasi = new MyCheckboxConfig(
				"Sembunyikan nilai ke mhs, jika blm di-verifikasi");
		sembunyikanNilaiJikaBelumDiverifikasi.setStyle("font-size:9px");
		sembunyikanNilaiJikaBelumDiverifikasi.setChecked(perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi());

		sembunyikanNilaiJikaBelumDiverifikasi.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				perkuliahan.setSembunyikanNilaiJikaBelumDiverifikasi(sembunyikanNilaiJikaBelumDiverifikasi.isChecked());
				Common.refreshUpdate(perkuliahan);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, true);
									}
								}, detailperkuliahans);
					}
				});
			}
		});

		urutkanBerdasarkanNama = new MyCheckboxConfig("Urutkan berdasar nama");
		urutkanBerdasarkanNama.setStyle("font-size:9px");
		urutkanBerdasarkanNama.setChecked(true);
		urutkanBerdasarkanNama.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(groupbox);
		hbox.appendChild(nilai0masukNilaiAkhir);
		hbox.appendChild(jikaNilai0masukNilaiAkhir);
		hbox.appendChild(hanyaInputNilaiHuruf);
		hbox.appendChild(sembunyikanNilaiJikaBelumDiverifikasi);
		hbox.appendChild(urutkanBerdasarkanNama);

		final Html warning = new ais.ui.util.MyHtml(
				"<font style='font-size:12px;color:red;'>Demi menjaga integritas data penilaian, harap segera mengunci data nilai mahasiswa anda setelah semua nilai dimasukkan.</font>");
		warning.setParent(hbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(8);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);
		button.setOrient("vertical");

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onLaporan(perkuliahan, null);
			}
		});
		print.setParent(toolbar);
		print.setOrient("vertical");

		mahasiswaBolehUbahNilai = false;
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			mahasiswaBolehUbahNilai = perkuliahan.merupakanAsistenNilai(tbmuser.getMahasiswa());
		}

		boolean editDisableTemp = !edit
				|| (!aktifPenilaian
						&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)))
				|| (tbmuser.getMahasiswa() != null && !mahasiswaBolehUbahNilai);

		if (Common.bolehKonfigurasi("hanya_dosen_yg_boleh_entry_nilai", Konfigurasi.TIDAK_AKTIF)) {
			if (tbmuser != null && tbmuser.ambilDosen() == null) {
				editDisableTemp = true;
			}
		}

		final boolean editDisable = editDisableTemp;

		final MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Format Nilai", "/img/svg/edit-box-line.svg");
		if (component instanceof Tabpanel) {
			if (perkuliahan != null && !perkuliahan.getSembunyikanFormatPenilaian()) {
				btn.setOrient("vertical");
				if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
						.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
					// OBE: tombol TETAP TAMPIL. Bobot penilaian OBE ditentukan CPMK/Sub-CPMK, jadi klik
					// tombol ini akan membuka popup RPS OBE (tab CPMK & Sub-CPMK) lewat FormatPenilaianHelper,
					// bukan lagi disembunyikan. Beri tooltip agar maksudnya jelas.
					btn.setTooltiptext(Common.getBahasaConfig("Atur bobot penilaian OBE (CPMK & Sub-CPMK)"));
				}
				btn.addEventListener("onClick", new EventListener() {

					FormatPenilaianHelper formatPenilaianHelper = new FormatPenilaianHelper();

					@Override
					public void onEvent(Event event) throws Exception {
						MyWindow addWindow = new MyWindow();
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
						formatPenilaianHelper.display(perkuliahan, addWindow, new TampilDetailNilaiInterface() {

							@Override
							public void realoadNilai(final Perkuliahan perkuliahan) {

								Common.realoadNilai(perkuliahan, perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												display(perkuliahan, component, onPerubahanNilai, btn, aktifPenilaian);
											}
										}, detailperkuliahans);

							}
						});
					}

				});
				btn.setParent(toolbar);
			}
		}

		final MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		download.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PenilaianUtil.downloadPenilaian(perkuliahan, formatNilais);
			}
		});

		download.setDisabled(editDisable);
		download.setOrient("vertical");
		download.setParent(toolbar);

		final MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setDisabled(editDisable);
		upload.setVisible(Common.bolehKonfigurasi("tampilkan_upload_nilai_di_modul_penilaian"));
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// // System.out.println("media = " + media);
					File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// // System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					PenilaianUtil.uploadPenilaian(perkuliahan, file, formatNilais, onPerubahanNilai,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									display(perkuliahan, component, onPerubahanNilai, buttonFormatNilai,
											aktifPenilaian);
								}
							});

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		upload.setParent(toolbar);
		upload.setOrient("vertical");
		final MyToolbarbuttonConfig bukaKunci = new MyToolbarbuttonConfig("Buka", "/img/svg/unlock.svg");
		final MyToolbarbuttonConfig kunci = new MyToolbarbuttonConfig("Kunci", "/img/Lock-Lock-icon.png");

		bukaKunci.setStyle("font-size:11px;");
		kunci.setStyle("font-size:11px;");

		final MyToolbarbuttonConfig buttonMasukkanNilaiAbsen = new MyToolbarbuttonConfig("Masukkan Nilai Absen",
				"/img/excel.png");

		adminBoleh = false;

		if (tbmuser.getMahasiswa() == null) {

			kunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin mengunci nilai ini ?\n\nCatatan : Nilai akan terkunci dan tidak bisa dirubah oleh orang lain kecuali jika anda membuka kunci penilain kembali.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										if (Common.bolehKonfigurasi("sebelum_dikunci_harus_diverifikasi_dulu", Konfigurasi.TIDAK_AKTIF)) {

											Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(
													null, null, "", urutkanBerdasarkanNama.isChecked(), true);

											for (Long detailperkuliahanid : detailperkuliahans) {
												Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
														.ambilData(Detailperkuliahan.class,
																detailperkuliahanid.toString());
												if (detailperkuliahan != null && detailperkuliahan.getVerify()
														.equals(Detailperkuliahan.NOT_VERIFIED)) {
													MyMessageboxConfig.show(
															"Semua nilai harus diverifikasi dulu sebelum bisa di kunci",
															"Peringatan", MyMessageboxConfig.OK,
															MyMessageboxConfig.INFORMATION);
													return;
												}
											}

										}

										// PENTING (cegah nilai berubah saat dikunci, mis. "Hasil Proyek" 85 -> 0):
										// Saat terkunci, getDetailNilai() mengembalikan snapshot detailNilaiKunci,
										// BUKAN detailNilai yang sedang tampil. Snapshot itu hanya ter-mirror
										// otomatis (getDetailNilaiKunci) ketika entitas di-flush SELAGI perkuliahan
										// masih terbuka DAN asosiasi perkuliahan termuat. Bila nilai sempat di-update
										// lewat jalur yang tidak memenuhi syarat itu (mis. sinkronisasi feeder /
										// hasil proyek dengan perkuliahan tak termuat), detailNilaiKunci jadi TERTINGGAL
										// (desync) dari detailNilai -> begitu dikunci, nilai yang tampil "berubah" ke
										// snapshot lama. Maka sebelum mengunci, salin ulang detailNilai (nilai LIVE
										// yang sedang ditampilkan) -> detailNilaiKunci untuk SEMUA mahasiswa, selagi
										// perkuliahan masih terbuka, agar penguncian membekukan persis nilai saat ini.
										try {
											Collection<Long> idsSnapshotKunci = perkuliahan.ambilDetailperkuliahan(
													null, null, "", urutkanBerdasarkanNama.isChecked(), true);
											for (Long idSnapshotKunci : idsSnapshotKunci) {
												Detailperkuliahan dpkKunci = (Detailperkuliahan) GeneralValueObject
														.ambilData(Detailperkuliahan.class, idSnapshotKunci.toString());
												if (dpkKunci != null) {
													// Perkuliahan masih terbuka: bekukan detail komponen sekaligus
													// total, huruf, IP, kelulusan, dan nilai sementara ke kolom
													// snapshot masing-masing sebelum status global dipasang.
													dpkKunci.bekukanSemuaNilai();
													Common.refreshUpdate(dpkKunci);
												}
											}
										} catch (Exception exSnapshotKunci) {
											Common.tampilErrorJikaAdmin(exSnapshotKunci);
											MyMessageboxConfig.show(
													"Penguncian dibatalkan karena snapshot permanen nilai belum berhasil disimpan seluruhnya.",
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										perkuliahan.setDikunci(tbmuser);
										Common.refreshUpdate(perkuliahan);

										loadData(null);

										kunci.setVisible(perkuliahan.getDikunci() == null);
										bukaKunci.setVisible(perkuliahan.getDikunci() != null);
										if (perkuliahan.getDikunci() != null) {
											bukaKunci.setLabel(
													"Buka Kunci (" + perkuliahan.getDikunci().getUserNama() + ")");
										}
										Common.freeze(grid, perkuliahan.getDikunci() != null);
										upload.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										download.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										btn.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										warning.setVisible(perkuliahan.getDikunci() == null && !editDisable);

										if (buttonFormatNilai != null)
											buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null);

										onLaporan(perkuliahan, null);

										buttonMasukkanNilaiAbsen.setVisible(perkuliahan.getDikunci() == null);
										nilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_pilihan_nilai_0_tidak_masuk_penghitungan_nilai_akhir")
												&& tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null);
										jikaNilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_jika_ada_nilai_0_tidak_masuk_penghitungan_nilai_akhir")
												&& tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null);

										hanyaInputNilaiHuruf.setVisible(Common.bolehKonfigurasi("tampilkan_hanya_input_nilai_huruf") && tbmuser.getMahasiswa() == null
												&& perkuliahan.getDikunci() == null);

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai,
														true);
											}
										});
									}

								}
							});
				}
			});

			kunci.setVisible(perkuliahan.getDikunci() == null);

			kunci.setParent(toolbar);
			kunci.setOrient("vertical");

			bukaKunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin membuka kunci nilai ini ?\n\nCatatan : Nilai akan terbuka dan bisa dirubah oleh orang lain yang berhak mengakses penilaian anda (misalnya: admin).",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										perkuliahan.setDikunci(null);
										Common.refreshUpdate(perkuliahan);

										loadData(null);

										kunci.setVisible(perkuliahan.getDikunci() == null);
										bukaKunci.setVisible(perkuliahan.getDikunci() != null);

										Common.freeze(grid, perkuliahan.getDikunci() != null);
										upload.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										download.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										btn.setDisabled(perkuliahan.getDikunci() != null || editDisable);
										warning.setVisible(perkuliahan.getDikunci() == null && !editDisable);

										if (buttonFormatNilai != null)
											buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null);
										buttonMasukkanNilaiAbsen.setVisible(perkuliahan.getDikunci() == null);
										nilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_pilihan_nilai_0_tidak_masuk_penghitungan_nilai_akhir")
												&& tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null);
										jikaNilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_jika_ada_nilai_0_tidak_masuk_penghitungan_nilai_akhir")
												&& tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null);

										hanyaInputNilaiHuruf.setVisible(Common.bolehKonfigurasi("tampilkan_hanya_input_nilai_huruf") && tbmuser.getMahasiswa() == null
												&& perkuliahan.getDikunci() == null);

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai,
														true);
											}
										});
									}

								}
							});
				}
			});
			bukaKunci.setVisible(perkuliahan.getDikunci() != null);
			if (perkuliahan.getDikunci() != null) {
				bukaKunci.setLabel("Buka Kunci (" + perkuliahan.getDikunci().getUserNama() + ")");
			}
			bukaKunci.setDisabled((perkuliahan.getDikunci() != null && tbmuser.getUserId() != null
					&& !perkuliahan.getDikunci().getUserId().equals(tbmuser.getUserId())) || !edit
					|| (!aktifPenilaian
							&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))));

			bukaKunci.setParent(toolbar);
			bukaKunci.setOrient("vertical");

			Konfigurasi konfigurasiKunci = Common.getKonfigurasi("kunci_nilai_untuk_admin", Konfigurasi.TIDAK_AKTIF);

			if (konfigurasiKunci.getNilai().equals(Konfigurasi.AKTIF)) {
				if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses() != null && tbmuser != null
						&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR)) {
					bukaKunci.setDisabled(false);
					adminBoleh = true;
				}
			}

			if (aktifPenilaian) {
				if ((perkuliahan.getDikunci() != null && tbmuser.getUserId() != null
						&& perkuliahan.getDikunci().getUserId().equals(tbmuser.getUserId()))) {
					bukaKunci.setDisabled(false);
				}

				if (perkuliahan.getJumlahDosen().intValue() == 0) {
					if (Common.bolehKonfigurasi("buka_kunci_nilai_untuk_jadwal_tanpa_dosen", Konfigurasi.TIDAK_AKTIF)) {
						bukaKunci.setDisabled(false);
					}
				}

				kunci.setDisabled(false);
			}

		}

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Detailperkuliahan.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI));

				criteria.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
						.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.ilike("mahasiswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(),
												MatchMode.ANYWHERE)))
						.add(Restrictions.eq("perkuliahan", perkuliahan));

				if (order)
					criteria.addOrder(Order.asc("mahasiswa.nim"));

				return criteria;
			}
		}, "perkuliahan", "mahasiswa", "semester", "tahunAkademik", "totalNilai", "nilaiHuruf", "totalIP");
		cetakToolbarbutton.setOrient("vertical");
		toolbar.appendChild(cetakToolbarbutton);

		button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/options.png");
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session nilaiHurufSession = null;
						try {
							nilaiHurufSession = HibernateUtil.openSession();
							ConstantValues.realoadNilaiHuruf(nilaiHurufSession);
						} finally {
							if (nilaiHurufSession != null) {
								try {
									nilaiHurufSession.close();
								} catch (Exception eClose) {
									ais.common.ErrorAuditUtil.record(eClose,
											"DetailperkuliahanForPenilaianHelper:reloadNilaiHuruf");
								}
							}
						}

						// MUAT ULANG DARI DATABASE lebih dulu agar cache = DB. Ini memperbaiki kelas bug
						// "status kunci / nilai BASI di cache" (mis. perkuliahan sudah dibuka kuncinya di DB
						// tetapi objek Perkuliahan di cache masih 'terkunci' -> getDetailNilai menimpa nilai
						// ketikan dengan snapshot lama). Reload dilakukan IN-PLACE (Common.refresh) ke objek
						// yang SAMA, lalu di-masukkan ulang ke cache (DataUtil.masukkanData) sehingga tetap
						// SATU objek per (kelas,id) di JVM dan seluruh pemegang melihat data terbaru DB.
						try {
							Common.refresh(perkuliahan);
							ais.common.DataUtil.masukkanData(Perkuliahan.class, perkuliahan);

						} catch (Exception eReload) {
							Common.tampilErrorJikaAdmin(eReload);
						}

						// HITUNG ULANG PARALEL: tiap mahasiswa dihitung di thread & session sendiri, sebanyak
						// jumlah mahasiswa TAPI maksimal 50 thread sekali jalan (dipatok DbThreadPool.safe).
						Common.realoadNilaiLangsungParalel(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, true);
									}
								}, detailperkuliahans, 50);
					}
				});

			}

		});
		toolbar.appendChild(button);

		if (tbmuser.getMahasiswa() == null) {

			boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);

			adaProsesVerifikasiNilai = Common.bolehKonfigurasi("ada_proses_verifikasi_penilaian_kepada_dosen", Konfigurasi.TIDAK_AKTIF) || nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk;
			if (!adaProsesVerifikasiNilai) {
				perkuliahan.setSembunyikanNilaiJikaBelumDiverifikasi(false);
			}
			sembunyikanNilaiJikaBelumDiverifikasi.setVisible(adaProsesVerifikasiNilai);
			button = new MyToolbarbuttonConfig("Verifikasi", "/img/svg/check2.svg");
			button.setOrient("vertical");
			button.setVisible((dosen == null || (dosen != null && perkuliahan.getDosenBolehVerifikasiNilaiSendiri()))
					&& adaProsesVerifikasiNilai);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan verifikasi nilai ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												for (Long detailperkuliahanid : perkuliahan.ambilDetailperkuliahan()) {
													Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
															.ambilData(Detailperkuliahan.class,
																	detailperkuliahanid.toString());
													if (detailperkuliahan != null) {

														try {
															Session session = HibernateUtil.currentNativeSession();
															session.refresh(detailperkuliahan);

															boolean adayangBelumVerified = false;
															for (FormatNilai formatNilai : formatNilais) {
																Double jumlah = detailperkuliahan
																		.retreiveDetailNilaiBelumVerify(formatNilai);
																if (jumlah < 0.01) {
																	adayangBelumVerified = true;
																} else {
																	detailperkuliahan.populateDetailNilai(formatNilai,
																			null, jumlah, true,
																			perkuliahan
																					.getSembunyikanNilaiJikaBelumDiverifikasi(),
																			tbmuser);
																}
															}

															detailperkuliahan.setVerify(adayangBelumVerified
																	? Detailperkuliahan.NOT_VERIFIED
																	: Detailperkuliahan.VERIFIED);
															detailperkuliahan.setVerifikator(
																	tbmuser.getUserId() + " " + tbmuser.getUserNama());
															detailperkuliahan.setWaktuVerifikasi(
																	ais.ui.util.WaktuUtil.getDate());
															session.getTransaction().begin();
															Common.refreshUpdate(session, detailperkuliahan);
															session.getTransaction().commit();
															// session.disconnect();
															if (session.isOpen()) {
																session.disconnect();
																session.close();
															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1863");
														}
														HibernateUtil.closeSession();
													}
												}

												KomentarPerkuliahanHelper komentarHelper = new KomentarPerkuliahanHelper(
														perkuliahan);

												komentarHelper.display(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadDataKomentar();

														Common.realoadNilai(perkuliahan,
																sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
																new EventListener() {

																	@Override
																	public void onEvent(Event arg0) throws Exception {
																		loadData(null);
																	}
																}, detailperkuliahans);
													}
												});

											}
										});

									}

								}
							});

				}

			});
			toolbar.appendChild(button);

			MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
			cetakSksDosen.setOrient("vertical");
			cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_sinkronkan_semua"));
			toolbar.appendChild(cetakSksDosen);
			cetakSksDosen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi perkuliahan"));

							new Thread(new Runnable() {

								@Override
								public void run() {
									// Thread latar: openSession DEDIKASI (bukan currentNativeSession). Method
									// reInit* yang dipanggil singkronkan (mis. pengumpulan email pertemuan)
									// memanggil HibernateUtil.closeSession() untuk session ThreadLocal-nya
									// sendiri; bila kita ikut memakai session ThreadLocal, session kita ikut
									// TERTUTUP di tengah proses → "Session is closed!". Session dedikasi tidak
									// tersimpan di ThreadLocal sehingga kebal. Ditutup di finally.
									Session session = null;
									try {
										session = HibernateUtil.openSession();
										perkuliahan.singkronkan(session);
									} finally {
										if (session != null) {
											try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1934");}
											try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1935");}
											try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:1936");}
										}
										HibernateUtil.closeSession();
										label.setValue("");
									}

								}
							}).start();

							final Timer timer = new Timer(500);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									// // System.out.println("process = " +
									// label.getValue());
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {

										DetailperkuliahanForPenilaianHelper.this.loadData(true);
										Clients.clearBusy();
										MyMessageboxConfig.show("Singkronisasi perkuliahan berhasil dilakukan",
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
										timer.detach();

									}

								}
							});
							timer.start();

						}
					});
				}
			});

		} else {
			sembunyikanNilaiJikaBelumDiverifikasi.setVisible(false);
		}

		button = new MyToolbarbuttonConfig("Komentar", "/img/m3.gif");
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				KomentarPerkuliahanHelper komentarHelper = new KomentarPerkuliahanHelper(perkuliahan);

				komentarHelper.display(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadDataKomentar();
					}
				});

			}

		});
		button.setParent(toolbar);

		buttonMasukkanNilaiAbsen.setOrient("vertical");
		buttonMasukkanNilaiAbsen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						for (Long detailperkuliahanid : detailperkuliahans) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {

								FormatNilai formatNilaiAbsen = null;
								for (FormatNilai formatNilai : formatNilais) {
									if (formatNilai != null && formatNilai.getNama() != null
											&& (formatNilai.getNama().toLowerCase().trim().contains("absen")
													|| formatNilai.getNama().toLowerCase().trim().contains("hadir")
													|| formatNilai.getNama().toLowerCase().trim()
															.contains("presensi"))) {
										formatNilaiAbsen = formatNilai;
										break;
									}
								}

								if (formatNilaiAbsen != null) {
									Map<String, Integer> absensi = Perkuliahan.hitungStatus(statusPertemuan,
											detailperkuliahan.getMahasiswa().getId());

									int semua = absensi.get("T") == null ? 0 : absensi.get("T");
									int masuk = absensi.get("M") == null ? 0 : absensi.get("M");
									int sakit = absensi.get("S") == null ? 0 : absensi.get("S");
									int izin = absensi.get("I") == null ? 0 : absensi.get("I");

									double nilaiAbsensi = semua == 0 ? 0.0
											: ((masuk * 100.0) + (sakit * 0.5 * 100.0) + (izin * 0.5 * 100.0)) / semua;

									detailperkuliahan.populateDetailNilai(formatNilaiAbsen, null, nilaiAbsensi, true,
											perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(), tbmuser);
									Matakuliah matakuliah = detailperkuliahan == null ? null
											: detailperkuliahan.getPerkuliahan() != null
													? detailperkuliahan.getPerkuliahan().getMatakuliah()
													: detailperkuliahan.getMatakuliahKonversi();
									Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);
									NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
											detailperkuliahan.getMahasiswa().getTahunangkatan(),
											detailperkuliahan.getMahasiswa().getJurusan(),
											detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
											detailperkuliahan.getTahunAkademik(),
											detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
													: Perkuliahan.GANJIL,
											matakuliah == null ? "" : matakuliah.getKode(),
											matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

									detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
									detailperkuliahan
											.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
									detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

									detailperkuliahan.setTotalNilai(total);

									Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true,
											formatNilais);

									nilaiHuruf = Common.getNilaiHuruf(totalSementara,
											detailperkuliahan.getMahasiswa().getTahunangkatan(),
											detailperkuliahan.getMahasiswa().getJurusan(),
											detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
											detailperkuliahan.getTahunAkademik(),
											detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
													: Perkuliahan.GANJIL,
											matakuliah == null ? "" : matakuliah.getKode(),
											matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

									detailperkuliahan.setTotalNilaiSementara(totalSementara);
									detailperkuliahan.setNilaiHurufSementara(
											nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
									detailperkuliahan
											.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

									// Timer callback: currentNativeSession() bisa stale → openSession eksplisit
									Session session = HibernateUtil.openSession();
									try {
										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, detailperkuliahan);
										session.getTransaction().commit();
									} catch (Exception eSess) {
										try { if (session.getTransaction() != null && session.getTransaction().isActive()) { session.getTransaction().rollback(); } } catch (Exception eRb) {}
										throw eSess;
									} finally {
										try { session.clear(); session.disconnect(); session.close(); } catch (Exception eClose) {}
									}

								} else {
									MyMessageboxConfig.show("Format nilai absen tidak ditemukan", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}
							}
						}
						loadData(true);
					}
				});

			}
		});
		toolbar.appendChild(buttonMasukkanNilaiAbsen);
		buttonMasukkanNilaiAbsen.setVisible(
				tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null && !download.isDisabled());

		/*
		 * Tombol "Reset": mengosongkan SELURUH nilai mahasiswa pada kelas ini menjadi semula
		 * (kosong/0). Hanya tampil selama belum dikunci dan masih boleh edit (syarat sama dengan
		 * "Masukkan Nilai Absen": bukan mahasiswa + getDikunci()==null + !download.isDisabled()).
		 * Selalu meminta konfirmasi peringatan dulu karena bersifat destruktif & tak bisa dibatalkan.
		 */
		final MyToolbarbuttonConfig buttonReset = new MyToolbarbuttonConfig("Reset", "/img/svg/arrow-go-back-line.svg");
		buttonReset.setOrient("vertical");
		buttonReset.setTooltiptext("Reset / kosongkan seluruh nilai mahasiswa menjadi semula");
		buttonReset.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"PERINGATAN: Anda akan MERESET SELURUH nilai mahasiswa pada kelas ini menjadi SEMULA (kosong/0).\n\n"
								+ "Seluruh nilai per komponen (Sub-CPMK) dan nilai tambahan untuk SEMUA mahasiswa akan "
								+ "dikosongkan, lalu total nilai dihitung ulang menjadi 0 (E). Tindakan ini TIDAK DAPAT "
								+ "dibatalkan.\n\nApakah Anda yakin ingin melanjutkan?",
						"Peringatan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.EXCLAMATION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										for (Long detailperkuliahanid : detailperkuliahans) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan == null) {
												continue;
											}

											/*
											 * Kembalikan nilai yang masih terbuka ke kondisi semula. Nilai komponen
											 * terkunci dan snapshot permanennya harus tetap utuh. Total dinolkan
											 * sebelum dihitung ulang supaya refreshNilaiKeDefault() tidak membangun
											 * kembali nilai terbuka dari total lama.
											 */
											// Reset tidak boleh menghapus komponen yang sudah dikunci. Model akan
											// mengosongkan nilai terbuka dan memulihkan setiap entri terkunci dari
											// kolom snapshot permanen. Snapshot sengaja tidak pernah dikosongkan.
											detailperkuliahan.resetDetailNilaiYangTidakDikunci(formatNilais);
											detailperkuliahan.setTotalNilai(0.0);
											detailperkuliahan.setTotalNilaiSementara(0.0);

											Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
													? detailperkuliahan.getPerkuliahan().getMatakuliah()
													: detailperkuliahan.getMatakuliahKonversi();

											Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);
											NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
													detailperkuliahan.getMahasiswa().getTahunangkatan(),
													detailperkuliahan.getMahasiswa().getJurusan(),
													detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
													detailperkuliahan.getTahunAkademik(),
													detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
															: Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

											detailperkuliahan
													.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
											detailperkuliahan
													.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
											detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
											detailperkuliahan.setTotalNilai(total);

											Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true,
													formatNilais);
											nilaiHuruf = Common.getNilaiHuruf(totalSementara,
													detailperkuliahan.getMahasiswa().getTahunangkatan(),
													detailperkuliahan.getMahasiswa().getJurusan(),
													detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
													detailperkuliahan.getTahunAkademik(),
													detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
															: Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
											detailperkuliahan.setTotalNilaiSementara(totalSementara);
											detailperkuliahan.setNilaiHurufSementara(
													nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
											detailperkuliahan.setTotalIPSementara(
													nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

											Session session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, detailperkuliahan);
											session.getTransaction().commit();
											HibernateUtil.closeSession();
										}

										loadData(true);
									}
								});
							}
						});
			}
		});
		toolbar.appendChild(buttonReset);
		buttonReset.setVisible(
				tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null && !download.isDisabled());

		nilai0masukNilaiAkhir.setDisabled(editDisable);
		jikaNilai0masukNilaiAkhir.setDisabled(editDisable);
		hanyaInputNilaiHuruf.setDisabled(editDisable);

		nilai0masukNilaiAkhir.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				perkuliahan.setNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir(nilai0masukNilaiAkhir.isChecked());
				Common.refreshUpdate(perkuliahan);
				Common.realoadNilai(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						}, detailperkuliahans);
			}
		});
		nilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_pilihan_nilai_0_tidak_masuk_penghitungan_nilai_akhir") && tbmuser.getMahasiswa() == null
				&& perkuliahan.getDikunci() == null && !download.isDisabled());

		jikaNilai0masukNilaiAkhir.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				perkuliahan.setJikaAdaNilai0TidakMenghitungNilaiAkhir(jikaNilai0masukNilaiAkhir.isChecked());
				Common.refreshUpdate(perkuliahan);
				Common.realoadNilai(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						}, detailperkuliahans);
			}
		});
		jikaNilai0masukNilaiAkhir.setVisible(Common.bolehKonfigurasi("tampilkan_jika_ada_nilai_0_tidak_masuk_penghitungan_nilai_akhir") && tbmuser.getMahasiswa() == null
				&& perkuliahan.getDikunci() == null && !download.isDisabled());

		hanyaInputNilaiHuruf.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				perkuliahan.setHanyaInputNilaiHuruf(hanyaInputNilaiHuruf.isChecked());
				Common.refreshUpdate(perkuliahan);
				Common.realoadNilai(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						}, detailperkuliahans);
			}
		});
		hanyaInputNilaiHuruf.setVisible(Common.bolehKonfigurasi("tampilkan_hanya_input_nilai_huruf") && tbmuser.getMahasiswa() == null
				&& perkuliahan.getDikunci() == null && !download.isDisabled());

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.getFormatNilais(perkuliahan, true);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, true);
					}
				});
			}

		});
		button.setParent(toolbar);

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Ambil Nilai dri Feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setOrient("vertical");
			buttonTagihan.setStyle("font-size:8px;");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Data nilai yang sudah dinputkan di sistem atau nilai mahasiswa lebih dari nilai 0, tidak bisa diambil dari Feeder. Hanya perkuliahan yg belum dinilai saja yg bisa diambil dari feeder.\nApakah Anda yakin ingin melanjutkan ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

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

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final List<String> errorLog = new ArrayList<String>();
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												loadData(true);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													int size = detailperkuliahans.size();
													int index = 1;
													for (Long detailperkuliahanid : detailperkuliahans) {
														Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
																.ambilData(Detailperkuliahan.class,
																		detailperkuliahanid.toString());
														if (detailperkuliahan != null) {
															Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
															myLabelProsesDetail
																	.setValue(
																			"Memproses " + mahasiswa.getNim() + " "
																					+ mahasiswa.getNama() + " ("
																					+ Common.numberFormat.get().format(
																							(index * 100.0) / size)
																					+ "%");
															index++;
															try {
																MahasiswaAction.ambilNilaiDariFeeder(feederConnector,
																		token, 0, mahasiswa, tbmuser, null,
																		perkuliahan);
															} catch (Exception e) {
																errorLog.add("[" + mahasiswa.getNim() + " " + mahasiswa.getNama() + "] " + e.getMessage());
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:2413");
															}
														}
													}

													// FIX "gagal diam-diam": sebelumnya penanda SUKSES (setValue(""))
													// berada DI LUAR try, sehingga tetap dijalankan walau blok try di
													// atas melempar exception (mis. gagal konek/parse port) - popup
													// menutup dengan status "berhasil" padahal proses ambil nilai
													// dari Feeder sebenarnya gagal total. Sekarang penanda sukses
													// adalah pernyataan TERAKHIR di dalam try, sehingga hanya
													// tercapai bila tidak ada exception yang lolos.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengambilan data nilai perkuliahan \"" + perkuliahan.info()
																			+ "\" dari Neo Feeder",
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
			toolbar.appendChild(buttonTagihan);
		}

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setDisabled(editDisable);
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiDetailPerkuliahanHelper revisiHelper = new RevisiDetailPerkuliahanHelper(perkuliahan,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								perkuliahan.belum("detailperkulaiahan");
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										DetailperkuliahanForPenilaianHelper.this.loadData(true);
									}
								});
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		// ── Restore: kembalikan bobot + nilai ke revisi terakhir per tanggal terpilih ──
		// Aktif hanya bila BELUM dikunci & masih waktu entry (!editDisable). KECUALI admin (konfigurasi
		// "aktifkan_restore_untuk_admin_walau_terkunci" default AKTIF) → tetap aktif walau terkunci.
		{
			MyToolbarbuttonConfig buttonRestore = new MyToolbarbuttonConfig("Restore", "/img/svg/clock-history.svg");
			buttonRestore.setOrient("vertical");
			boolean adminOverride = Common.getApakahAdmin()
					&& Common.bolehKonfigurasi("aktifkan_restore_untuk_admin_walau_terkunci");
			boolean bolehRestore = adminOverride || (perkuliahan.getDikunci() == null && !editDisable);
			buttonRestore.setDisabled(!bolehRestore);
			buttonRestore.setTooltiptext(adminOverride
					? "Restore nilai/bobot ke revisi tanggal tertentu (admin: aktif walau terkunci)"
					: "Restore nilai/bobot ke revisi tanggal tertentu (aktif saat nilai belum dikunci)");
			buttonRestore.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					RestoreNilaiPerkuliahanHelper.bukaDialog(perkuliahan, detailperkuliahans, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							perkuliahan.belum("detailperkulaiahan");
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									DetailperkuliahanForPenilaianHelper.this.loadData(true);
								}
							});
						}
					});
				}
			});
			buttonRestore.setParent(toolbar);
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("95%");
		grid.setMold("paging");
		grid.setPageSize(10000);
		grid.setParent(groupbox);

		formatNilais = Common.getFormatNilais(perkuliahan);

		// Hitung lebar kolom adaptif agar total % ~ 95 tanpa meluap saat komponen banyak
		int _n = formatNilais.size();
		int _verPct = adaProsesVerifikasiNilai ? 5 : 0;
		// Kolom tetap: Foto(70px) + Smt(5%) + Minimal(10%) + Total(8%) + Ver(0-5%)
		// Minimal dipersempit 15%->10% & cap per-komponen dinaikkan 10->14% agar nama
		// ranah/jenis evaluasi (mis. "Kognitif/ Pengetahuan") muat di header kolom.
		int _budget = 95 - (28 + _verPct); // sisa untuk Mahasiswa + N kolom FormatNilai
		int perColPct = _n > 0 ? Math.max(4, Math.min(14, (_budget - 15) / _n)) : 9;
		int mhsColPct = Math.max(15, _budget - _n * perColPct);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		columnMahasiswa = new MyColumnConfig();
		columnMahasiswa.setParent(columns);
		columnMahasiswa.setLabel("Mahasiswa");
		columnMahasiswa.setWidth(mhsColPct + "%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		Vbox k = new Vbox();
		k.setParent(column);
		// RAPIKAN kolom "Minimal nilai kehadiran": label panjang & baris "Min: [box] %" tampak berdesakan.
		// Kini label ringkas + penjelasan lengkap dipindah ke tooltip, dan baris input ditata rapi (flex,
		// rata, tak wrap acak). Fungsinya sama: persen kehadiran minimal agar Nilai Total dihitung; di
		// bawah nilai ini total menjadi 0 (itulah sebab sebagian mahasiswa "0 (E)" walau komponen terisi).
		MyLabelKecilBold aa;
		k.appendChild(aa = new MyLabelKecilBold("Min. Kehadiran"));
		aa.setMultiline(true);
		aa.setStyle("display:block;text-align:center;line-height:1.15;");
		aa.setTooltiptext(
				"Minimal persen kehadiran mahasiswa agar Nilai Total dihitung. Bila kehadiran mahasiswa DI BAWAH nilai ini, Nilai Total-nya menjadi 0 (E) meskipun komponen nilainya terisi. Kosongkan / isi 0 untuk menonaktifkan.");

		boolean nggakBolehUbah = perkuliahan.getDikunci() != null || !edit

				|| (tbmuser.getMahasiswa() != null && !mahasiswaBolehUbahNilai)

				|| (!aktifPenilaian
						&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)));

		if (nggakBolehUbah) {
			org.zkoss.zul.Div dMin = new org.zkoss.zul.Div();
			dMin.setStyle("display:flex;align-items:center;justify-content:center;gap:3px;white-space:nowrap;");
			dMin.appendChild(new MyLabelAgakKecil("Min:"));
			dMin.appendChild(new MyLabelAgakKecil(
					Common.numberFormat.get().format(perkuliahan.getPersenKehadiranDinilai0())));
			dMin.appendChild(new MyLabelAgakKecil("%"));
			k.appendChild(dMin);
		} else {

			final MyDoublebox min;
			org.zkoss.zul.Div dMin = new org.zkoss.zul.Div();
			dMin.setStyle("display:flex;align-items:center;justify-content:center;gap:3px;white-space:nowrap;");
			dMin.appendChild(new MyLabelAgakKecil("Min:"));
			dMin.appendChild(min = new MyDoublebox(perkuliahan.getPersenKehadiranDinilai0()));
			dMin.appendChild(new MyLabelAgakKecil("%"));
			k.appendChild(dMin);
			min.setCols(1);
			min.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					perkuliahan.setPersenKehadiranDinilai0(min.getValue());
					Common.refreshUpdate(perkuliahan);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi.isChecked(),
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai,
													true);
										}
									}, detailperkuliahans);
						}
					});
				}
			});

		}

		this.columns = new ArrayList<Column>();

		int index = 1;
		for (final FormatNilai formatNilai : formatNilais) {
			column = new MyColumnConfig();
			this.columns.add(column);
			column.setParent(columns);
			column.setVisible(formatNilai.getPersen() > 0.01);
			column.setWidth(perColPct + "%");

			column.setAlign("right");
			Vbox hb = new Vbox();
			hb.setParent(column);

			Vbox lbl;
			try {
				(lbl = RevisiHelper.createNewRevisi(FormatNilai.class, formatNilai,
						formatNilai == null ? "" : formatNilai.getNama() + " " + formatNilai.getPersen() + "%"))
						.setParent(hb);
				lbl.setStyle("font-size: xx-small;text-align: center;");
				lbl.setParent(hb);
				lbl.setWidth("100%");
				lbl.setHeight("100%");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:2629");
			}

			// Vbox (bukan Hbox) agar nomor urut & combobox ranah/jenis evaluasi menumpuk
			// vertikal -> masing-masing dapat lebar penuh kolom (combobox "Kognitif" tak terpotong).
			// sclass "ranah-cell" dipakai CSS utk mengecilkan font combobox ranah (lihat css_utama.css).
			Vbox hboxD = new Vbox();
			hboxD.setWidth("100%");
			hboxD.setSclass("ranah-cell");
			hboxD.setParent(hb);

			try {
				Integer nomorUrutData = formatNilai.getNomorUrut();
				Long n = null;
				try {
					JSONObject jsonData = new JSONObject(perkuliahan.getPembombotanNilai().getNomorUrutFormat());
					n = jsonData.isNull(formatNilai.getStatusPertemuan().getId().toString()) ? null
							: ais.common.CommonJSONUtil.ambilLong(jsonData,
									formatNilai.getStatusPertemuan().getId().toString());
					if (n != null) {
						nomorUrutData = n.intValue();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:2652");
				}

				if (nggakBolehUbah || n != null) {
					new Label(Common.numberFormat.get().format(nomorUrutData == null ? index : nomorUrutData))
							.setParent(hboxD);
				} else {
					final Intbox nomorUrut = new Intbox(
							formatNilai.getNomorUrut() == null ? index : formatNilai.getNomorUrut());
					nomorUrut.setCols(1);
					nomorUrut.setParent(hboxD);
					nomorUrut.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							formatNilai.setNomorUrut(nomorUrut.getValue());
							Common.refreshUpdate(formatNilai);
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									prosesDisplay(kuliyah, component, onPerubahanNilai, buttonFormatNilai, null);
								}
							});
						}
					});
				}
				index++;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java:2680");
				// TODO: handle exception
			}

			if (nggakBolehUbah) {
				new Label(formatNilai.getJenisEvaluasi() == null ? "" : formatNilai.getJenisEvaluasi().getNama())
						.setParent(hboxD);
			} else {
				final Combobox jenisEvaluasi = new Combobox();
				jenisEvaluasi.setReadonly(true);
				Common.insertCombo(jenisEvaluasi, new String[] { "nama" }, "keterangan", JenisEvaluasi.class,
						Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisEvaluasi, formatNilai.getJenisEvaluasi());
				// Isi penuh lebar kolom (jangan dipaksa 2 kolom karakter). Font dikecilkan lewat CSS
				// (.ranah-cell .z-combobox-inp) agar nama ranah panjang muat & tidak terpotong.
				jenisEvaluasi.setWidth("95%");
				jenisEvaluasi.setTooltiptext(
						formatNilai.getJenisEvaluasi() == null ? "" : formatNilai.getJenisEvaluasi().getNama());
				jenisEvaluasi.setParent(hboxD);
				jenisEvaluasi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						formatNilai.setJenisEvaluasi((JenisEvaluasi) (jenisEvaluasi.getSelectedItem() == null ? null
								: jenisEvaluasi.getSelectedItem().getValue()));
						Common.refreshUpdate(formatNilai);
					}
				});
			}

			final MyToolbarbuttonConfig bukaKunciDetail = new MyToolbarbuttonConfig(
					formatNilai.getKunci() == null ? "" : formatNilai.getKunci().getUserNama(), "/img/svg/unlock.svg");
			final MyToolbarbuttonConfig kunciDetail = new MyToolbarbuttonConfig(
					formatNilai.getKunci() == null ? "" : formatNilai.getKunci().getUserNama(),
					"/img/Lock-Lock-icon.png");

			bukaKunciDetail.setStyle("font-size:8px;");
			kunciDetail.setStyle("font-size:8px;");

			if (formatNilai.getKunci() != null) {
				bukaKunciDetail.setTooltiptext("Dikunci oleh " + formatNilai.getKunci().getUserId());
			}
			if (tbmuser.getMahasiswa() == null && perkuliahan.getDikunci() == null
					&& (formatNilai.getStatusPertemuan() == null || !formatNilai.getStatusPertemuan().getKunci())) {

				kunciDetail.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show(
								"Apakah yakin ingin mengunci nilai ini ?\n\nCatatan : Nilai akan terkunci dan tidak bisa dirubah oleh orang lain kecuali jika anda membuka kunci penilain kembali.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											if (Common.bolehKonfigurasi("sebelum_dikunci_harus_diverifikasi_dulu", Konfigurasi.TIDAK_AKTIF)) {

												Collection<Long> detailperkuliahans = perkuliahan
														.ambilDetailperkuliahan(null, null, "",
																urutkanBerdasarkanNama.isChecked(), true);

												for (Long detailperkuliahanid : detailperkuliahans) {
													Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
															.ambilData(Detailperkuliahan.class,
																	detailperkuliahanid.toString());
													if (detailperkuliahan != null) {

														boolean verif = detailperkuliahan
																.retreiveDetailVerifikasiNilai(formatNilai);
														if (!verif) {
															MyMessageboxConfig.show("Semua nilai \""
																	+ formatNilai.getNama()
																	+ "\" harus diverifikasi dulu sebelum bisa di kunci",
																	"Peringatan", MyMessageboxConfig.OK,
																	MyMessageboxConfig.INFORMATION);
															return;
														}
													}
												}

											}

											// Bekukan nilai kolom ini untuk seluruh mahasiswa sebelum status
											// kunci dipasang. Nilai disimpan di detail nilai utama sekaligus
											// snapshot, sehingga sinkronisasi eksternal tidak dapat menimpanya.
											Collection<Long> idsSnapshotKolom = perkuliahan.ambilDetailperkuliahan(
													null, null, "", urutkanBerdasarkanNama.isChecked(), true);
											for (Long idSnapshotKolom : idsSnapshotKolom) {
												Detailperkuliahan dpkKolom = (Detailperkuliahan) GeneralValueObject
														.ambilData(Detailperkuliahan.class, idSnapshotKolom.toString());
												if (dpkKolom != null) {
													dpkKolom.bekukanDetailNilai(formatNilai);
													Common.refreshUpdate(dpkKolom);
												}
											}

											formatNilai.setKunci(Common.getCurrentUser());
											Common.refreshUpdate(formatNilai);

											loadData(null);

											bukaKunciDetail.setLabel(formatNilai.getKunci() == null ? ""
													: formatNilai.getKunci().getUserNama());

											kunciDetail.setVisible(formatNilai.getKunci() == null);
											bukaKunciDetail.setVisible(formatNilai.getKunci() != null);
											if (formatNilai.getKunci() != null) {
												bukaKunciDetail.setTooltiptext(
														"Dikunci oleh " + formatNilai.getKunci().getUserId());
											}

											bukaKunciDetail.setDisabled((formatNilai.getKunci() != null
													&& Common.getCurrentUser().getUserId() != null
													&& !formatNilai.getKunci().getUserId()
															.equals(Common.getCurrentUser().getUserId()))
													|| !edit || (!aktifPenilaian && (konfigurasi.getNilai() == null
															|| !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))));

										}

									}
								});
					}
				});
				kunciDetail.setVisible(formatNilai.getKunci() == null);
				kunciDetail.setDisabled(!edit || (!aktifPenilaian
						&& (konfigurasi.getNilai() == null || !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))));

				kunciDetail.setParent(toolbar);
				kunciDetail.setOrient("vertical");

				bukaKunciDetail.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show(
								"Apakah yakin ingin membuka kunci nilai ini ?\n\nCatatan : Nilai akan terbuka dan bisa dirubah oleh orang lain yang berhak mengakses penilaian anda (misalnya: admin).",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											formatNilai.setKunci(null);
											Common.refreshUpdate(formatNilai);

											loadData(null);

											kunciDetail.setVisible(formatNilai.getKunci() == null);
											bukaKunciDetail.setVisible(formatNilai.getKunci() != null);

										}

									}
								});
					}
				});
				bukaKunciDetail.setVisible(formatNilai.getKunci() != null);
				if (formatNilai.getKunci() != null) {
					bukaKunciDetail.setTooltiptext("Dikunci oleh " + formatNilai.getKunci().getUserId());
				}
				bukaKunciDetail
						.setDisabled((formatNilai.getKunci() != null && Common.getCurrentUser().getUserId() != null
								&& !formatNilai.getKunci().getUserId().equals(Common.getCurrentUser().getUserId()))
								|| !edit || (!aktifPenilaian && (konfigurasi.getNilai() == null
										|| !konfigurasi.getNilai().equals(Konfigurasi.AKTIF))));

				bukaKunciDetail.setOrient("vertical");
				kunciDetail.setOrient("vertical");

				bukaKunciDetail.setVisible(formatNilai.getKunci() != null);
				bukaKunciDetail.setDisabled(tbmuser == null || formatNilai.getKunci() == null
						|| !formatNilai.getKunci().getUserId().equals(tbmuser.getUserId()));
				kunciDetail.setVisible(formatNilai.getKunci() == null);

				Hbox hboxK = new Hbox();
				hb.appendChild(hboxK);

				hboxK.appendChild(bukaKunciDetail);
				hboxK.appendChild(kunciDetail);

				if (adminBoleh) {
					bukaKunciDetail.setDisabled(false);
				}

				if (adaProsesVerifikasiNilai && tbmuser.ambilDosen() == null) {

					final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig();

					hboxK.appendChild(checkboxConfig);

					Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
							urutkanBerdasarkanNama.isChecked(), false);
					boolean checkData = true;
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {

							boolean verif = detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai);
							checkData = checkData && verif;

						}
					}

					checkboxConfig.setChecked(checkData);
					checkboxConfig.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
									urutkanBerdasarkanNama.isChecked(), false);

							for (Long detailperkuliahanid : detailperkuliahans) {
								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
										.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
								if (detailperkuliahan != null) {

									Session session = HibernateUtil.currentNativeSession();
									session.refresh(detailperkuliahan);
									Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
									detailperkuliahan.populateDetailNilai(formatNilai, null, nilai,
											checkboxConfig.isChecked(), tbmuser);
									session.getTransaction().begin();
									session.update(detailperkuliahan);
									session.getTransaction().commit();

									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										session.close();
									}
									HibernateUtil.closeSession();

								}
							}

							loadData(null);
						}
					});
				}
			}

		}
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setWidth("8%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth(adaProsesVerifikasiNilai ? "5%" : "0%");

		Vbox hboxK = new Vbox();
		column.appendChild(hboxK);

		hboxK.appendChild(new Label(ais.common.Common.getBahasaConfig("Verify")));

		if (adaProsesVerifikasiNilai && tbmuser.ambilDosen() == null) {

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig();

			Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
					urutkanBerdasarkanNama.isChecked(), false);
			boolean checkData = true;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					for (FormatNilai formatNilai : formatNilais) {
						boolean verif = detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai);
						checkData = checkData && verif;
					}
				}
			}

			hboxK.appendChild(checkboxConfig);
			checkboxConfig.setChecked(checkData);
			checkboxConfig.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
							urutkanBerdasarkanNama.isChecked(), false);

					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {

							Session session = HibernateUtil.currentNativeSession();
							session.refresh(detailperkuliahan);
							for (FormatNilai formatNilai : formatNilais) {
								Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
								detailperkuliahan.populateDetailNilai(formatNilai, null, nilai,
										checkboxConfig.isChecked(), tbmuser);
							}
							detailperkuliahan.setVerify(checkboxConfig.isChecked() ? Detailperkuliahan.VERIFIED
									: Detailperkuliahan.NOT_VERIFIED);
							session.getTransaction().begin();
							session.update(detailperkuliahan);
							session.getTransaction().commit();

							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
							HibernateUtil.closeSession();

						}
					}

					loadData(null);
				}
			});
		}

		loadData(refresh);
		Common.freeze(grid, perkuliahan.getDikunci() != null);
		upload.setDisabled(perkuliahan.getDikunci() != null || editDisable);
		download.setDisabled(perkuliahan.getDikunci() != null || editDisable);
		btn.setDisabled(perkuliahan.getDikunci() != null || editDisable);
		warning.setVisible(perkuliahan.getDikunci() == null && !editDisable);

		final Tabpanel detailAsistenMahasiswa = new ais.ui.util.MyTabpanel();
		detailAsistenMahasiswa.setParent(tabpanels);

		tab1AsistenMahasiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (detailAsistenMahasiswa.getChildren().isEmpty()) {
					DetailperkuliahanForPenilaianHelper.displayAsistenMahasiswa(detailAsistenMahasiswa, kuliyah);

				}
			}
		});

//		final Tabpanel detailRekapKehadiran = new ais.ui.util.MyTabpanel();
//		detailRekapKehadiran.setParent(tabpanels);
//
//		tab1LihatRekapKehadiran.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				Common.clear(detailRekapKehadiran);
//				DashboardRekapAbsensiPerMahasiswa dashboardRekapAbsensiMahasiswa = new DashboardRekapAbsensiPerMahasiswa(
//						kuliyah);
//				dashboardRekapAbsensiMahasiswa.setHeight("500px");
//				detailRekapKehadiran.appendChild(dashboardRekapAbsensiMahasiswa);
//				detailRekapKehadiran.setStyle("min-height: 500px;");
//
//			}
//		});

		final Tabpanel detailRekapTugas = new ais.ui.util.MyTabpanel();
		detailRekapTugas.setParent(tabpanels);

		tab1LihatRekapTugas.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapTugas);
				// Tiru pola Rekap Total Nilai yang BERHASIL tampil (GradingHelper): isi rekap
				// (borderlayout) ditanam LANGSUNG ke tabpanel dengan tinggi PASTI, BUKAN
				// dibungkus MyWindow (di dalam tabpanel window membuat borderlayout collapse
				// 0px sehingga konten tidak tampil).
				tanamkanRekapKeTabpanel(new RekapHasilTugasPerVoPertemuan(true, kuliyah), detailRekapTugas);
				detailRekapTugas.setStyle("min-height: 2000px;");

			}
		});

		final Tabpanel detailRekapUjian = new ais.ui.util.MyTabpanel();
		detailRekapUjian.setParent(tabpanels);

		tab1LihatRekapUjian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapUjian);
				// Sama dengan Rekap Tugas: tanam isi langsung ke tabpanel (lihat GradingHelper).
				tanamkanRekapKeTabpanel(new RekapHasilUjianPerVoPertemuan(true, kuliyah), detailRekapUjian);
				detailRekapUjian.setStyle("min-height: 2000px;");

			}
		});

		final Tabpanel detailRekapTugasKelompok = new ais.ui.util.MyTabpanel();
		detailRekapTugasKelompok.setParent(tabpanels);

		tab1LihatRekapTugasKelompok.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapTugasKelompok);
				// Sama dengan Rekap Tugas: tanam isi langsung ke tabpanel (lihat GradingHelper).
				tanamkanRekapKeTabpanel(new RekapHasilTugasKelompokPerVoPertemuan(true, kuliyah),
						detailRekapTugasKelompok);
				detailRekapTugasKelompok.setStyle("min-height: 2000px;");

			}
		});

		final Tabpanel detailRekapNilai = new ais.ui.util.MyTabpanel();
		detailRekapNilai.setParent(tabpanels);

		tab1LihatRekapNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapNilai);
				ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(detailRekapNilai, kuliyah,
						kuliyah.ambilFormatNilai(HibernateUtil.currentSession()).toArray(new FormatNilai[] {}));
				detailRekapNilai.setStyle("min-height: 500px;");
				detailRekapNilai.invalidate();  // paksa render konten tabpanel (fix ZK5: konten tak tampil saat dibangun lazy)

			}
		});

		final Tabpanel detailPrestasi = new ais.ui.util.MyTabpanel();
		detailPrestasi.setParent(tabpanels);
		detailPrestasi.setHeight("500px");
		tab1Prestasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailPrestasi);
				LaporanDaftarPrestasiBelajarWindow daftarPrestasiBelajarWindow = new LaporanDaftarPrestasiBelajarWindow(
						kuliyah);
				daftarPrestasiBelajarWindow.setHeight("100%");
				daftarPrestasiBelajarWindow.setWidth("100%");
				daftarPrestasiBelajarWindow.setTitle("");
				detailPrestasi.appendChild(daftarPrestasiBelajarWindow);
				detailPrestasi.setStyle("min-height: 500px;");
				detailPrestasi.invalidate();  // paksa render konten tabpanel (fix ZK5: konten tak tampil saat dibangun lazy)

			}
		});

//		final Tabpanel detailPrestasiSemua = new ais.ui.util.MyTabpanel();
//		detailPrestasiSemua.setParent(tabpanels);
//		detailPrestasiSemua.setHeight("500px");
//		tab1PrestasiSemua.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				Common.clear(detailPrestasiSemua);
//				LaporanDaftarPrestasiBelajarWindow daftarPrestasiBelajarWindow = new LaporanDaftarPrestasiBelajarWindow(
//						kuliyah, null);
//				daftarPrestasiBelajarWindow.setHeight("100%");
//				daftarPrestasiBelajarWindow.setWidth("100%");
//				daftarPrestasiBelajarWindow.setTitle("");
//				detailPrestasiSemua.appendChild(daftarPrestasiBelajarWindow);
//				detailPrestasiSemua.setStyle("min-height: 500px;");
//
//			}
//		});

		gridKomentar = new MyGrid();
		gridKomentar.setMold("paging");
		gridKomentar.setPageSize(20);
		gridKomentar.setParent(groupbox);

		gridKomentar.setVisible(semester > 0);

		Columns columns2 = new Columns();
		columns2.setMenupopup("auto");
		columns2.setParent(gridKomentar);

		MyColumnConfig column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Komentar");
		column2.setWidth("50%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Oleh");
		column2.setWidth("20%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Tanggal");
		column2.setWidth("20%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("");
		column2.setWidth("10%");

		loadDataKomentar();

	}

	@SuppressWarnings("unchecked")
	public void loadDataKomentar() {
		Session session = HibernateUtil.currentSession();
		List<KomentarPerkuliahan> komentarPerkuliahanPerkuliahans = session.createCriteria(KomentarPerkuliahan.class)
				.addOrder(Order.asc("tanggal_dirubah")).add(Restrictions.eq("perkuliahan", perkuliahan)).list();
		ListModel strset = new SimpleListModel(komentarPerkuliahanPerkuliahans);

		// grid = new MyGrid();grid.setWidth("100%");
		gridKomentar.setRowRenderer(new KomentarPerkuliahanRenderer());
		gridKomentar.setModelCheckMobile(strset);

		gridKomentar.renderAll();
		gridKomentar.setOddRowSclass("non-odd");

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link DetailperkuliahanForPenilaianHelper}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DetailperkuliahanForPenilaianHelper} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DetailperkuliahanForPenilaianHelper
	 */
	class KomentarPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final KomentarPerkuliahan komentarPerkuliahanBeans = (KomentarPerkuliahan) data;

			new ais.ui.util.MyHtml(komentarPerkuliahanBeans.getKeterangan()).setParent(row);
			new Label(komentarPerkuliahanBeans.getNama()).setParent(row);
			new Label(Common.dateFormat.get().format(komentarPerkuliahanBeans.getTanggal_dirubah())).setParent(row);

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setParent(toolbar);
			button.setVisible(komentarPerkuliahanBeans.getNama().equals(tbmuser == null ? "" : tbmuser.getUserId()));
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Common.refreshDelete(komentarPerkuliahanBeans);
										loadDataKomentar();
									}

								}
							});
				}
			});

		}

	}

	public static void onLaporan(Perkuliahan perkuliahan) throws Exception {
		onLaporan(perkuliahan, null);
	}

	public static void onLaporan(Perkuliahan perkuliahan, Component component) throws Exception {
		onLaporan(perkuliahan, component, false);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporan(Perkuliahan perkuliahan, Component component, boolean refresh) throws Exception {
		if (perkuliahan == null) {
			// data pertemuan/perkuliahan belum lengkap/tidak ditemukan saat dosen
			// klik tombol laporan -> jangan lanjut (baris2 di bawah memakai
			// perkuliahan tanpa null-check lagi), beri tahu user drpd NPE diam.
			MyMessageboxConfig.show("Data perkuliahan tidak ditemukan/belum lengkap, laporan tidak dapat dibuat.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan, refresh);
		Map parameters = ais.common.HashMapGenerator.getRand();
		if (perkuliahan != null) {
			Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");

			if (perkuliahan.getJurusan() != null) {
				Common.insertProperty(Jurusan.class, perkuliahan.getJurusan(), parameters, "jur");
			}
			if (perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getFakultas() != null) {
				Common.insertProperty(Fakultas.class, perkuliahan.getJurusan().getFakultas(), parameters, "fak");
			}
			if (perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getFakultas() != null
					&& perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() != null) {
				Common.insertProperty(PerguruanTinggi.class,
						perkuliahan.getJurusan().getFakultas().getPerguruanTinggi(), parameters, "pt");
			}

		}

		parameters.put("judul_laporan_nilai",
				(perkuliahan.getStatusSemesterPendek() != null
						&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK))
								? "Daftar Nilai Ujian Semester Pendek"
								: "Daftar Nilai Ujian");

		parameters.put("perkuliahan", perkuliahan.getId());
		parameters.put("kelas",
				perkuliahan.getSemester() + " " + (perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas()));

		parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
		parameters.put("tampil_nilai", 1);
		parameters.put("fakultas",
				perkuliahan.getJurusan() == null || perkuliahan.getJurusan().getFakultas() == null ? ""
						: perkuliahan.getJurusan().getFakultas().getNama());
		parameters.put("jenis_semester",
				((Integer) perkuliahan.getSemester()) % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
		parameters.put("kode_matakuliah",
				perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());
		parameters.put("nama_matakuliah",
				perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

		Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");

		String dosen = "";
		int indexDosen = 1;
		for (Dosen d : perkuliahan.populateDosenBuNama()) {
			Common.insertProperty(Dosen.class, d, parameters, "dosen_" + indexDosen);
			String namaDosen = d == null || d.getNama() == null ? "" : d.getNama().toUpperCase();
			dosen += dosen.isEmpty() ? namaDosen : " / " + namaDosen;
		}
		parameters.put("dosen", dosen);
		parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
		parameters.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
		parameters.put("program", perkuliahan.getProgram());

		if (perkuliahan.getKurikulum() != null && perkuliahan.getJurusan() != null
				&& perkuliahan.getJurusan().getGrupJurusan() != null
				&& perkuliahan.getJurusan().getGrupJurusan().getKajur() != null) {
			parameters.put("nama_kajur", perkuliahan.getJurusan().getGrupJurusan().getKajur().getNama());
			parameters.put("nip_kajur", perkuliahan.getJurusan().getGrupJurusan().getKajur().getCode());
		}

		int i = 1;
		for (FormatNilai formatNilai : formatNilais) {
			parameters.put("col" + i, formatNilai.getNama() + "\n" + formatNilai.getPersen() + "%");
			parameters.put("col_nama_" + i, formatNilai.getNama());
			parameters.put("col_persen_" + i, Common.numberFormat.get().format(formatNilai.getPersen()) + "%");
			parameters.put("persen_" + i, formatNilai.getPersen());
			i++;
		}

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Collection<Long> terdaftar = perkuliahan.ambilDetailperkuliahan();
		for (Long detailperkuliahanid : terdaftar) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null && detailperkuliahan.getMahasiswa() != null
					&& Detailperkuliahan.DISETUJUI.equals(detailperkuliahan.getPersetujuan())) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
				map.put("nim", mahasiswa.getNim());
				map.put("nama", mahasiswa.getNama() == null ? "" : mahasiswa.getNama().toUpperCase());
				map.put("kode_matakuliah",
						perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

				Common.insertProperty(Detailperkuliahan.class, detailperkuliahan, map, "detailperkuliahan");

				i = 1;
				for (FormatNilai formatNilai : formatNilais) {
					if (perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
							&& Detailperkuliahan.NOT_VERIFIED.equals(detailperkuliahan.getVerify())) {
						Double nilai = detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai);
						map.put("nilai_" + i, (nilai));
					} else {
						Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
						map.put("nilai_" + i, (nilai));
					}
					i++;
				}

				if (perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
						&& Detailperkuliahan.NOT_VERIFIED.equals(detailperkuliahan.getVerify())) {
					map.put("nilai", detailperkuliahan.getTotalNilaiSementara());
					map.put("nilai_huruf", detailperkuliahan.getNilaiHurufSementara());
				} else {
					map.put("nilai", detailperkuliahan.getTotalNilai());
					map.put("nilai_huruf", detailperkuliahan.getNilaiHuruf());
				}

				// Data pejabat penanda-tangan (kajur/dekan, kaprodi, pudek1-3). Null-safe: resolusi
				// jurusan/fakultas/pejabat sekali, set nilai HANYA bila objek tersedia — agar tidak
				// membanjiri log dengan NullPointerException saat pejabat belum diisi.
				try {
					Jurusan jurusanTtd = mahasiswa.getJurusan();
					Fakultas fakultasTtd = jurusanTtd == null ? null : jurusanTtd.getFakultas();

					if (jurusanTtd != null) {
						Dosen kaprodiTtd = jurusanTtd.getKaprodi();
						if (kaprodiTtd != null) {
							map.put("nip_kaprodi", kaprodiTtd.getCode());
							map.put("nama_kaprodi", kaprodiTtd.getNama());
						}
					}

					if (fakultasTtd != null) {
						map.put("id_fakultas", fakultasTtd.getId());

						Dosen dekanTtd = fakultasTtd.getDekan();
						if (dekanTtd != null) {
							map.put("nip_kajur", dekanTtd.getCode());
							map.put("nama_kajur", dekanTtd.getNama());
						}

						Dosen pudek1Ttd = fakultasTtd.getPudek1();
						if (pudek1Ttd != null) {
							map.put("nama_pudek1", pudek1Ttd.getNama());
							map.put("nip_pudek1", pudek1Ttd.getCode());
							map.put("nidn_pudek1", pudek1Ttd.getNidn());
						}

						Dosen pudek2Ttd = fakultasTtd.getPudek2();
						if (pudek2Ttd != null) {
							map.put("nama_pudek2", pudek2Ttd.getNama());
							map.put("nip_pudek2", pudek2Ttd.getCode());
							map.put("nidn_pudek2", pudek2Ttd.getNidn());
						}

						Dosen pudek3Ttd = fakultasTtd.getPudek3();
						if (pudek3Ttd != null) {
							map.put("nama_pudek3", pudek3Ttd.getNama());
							map.put("nip_pudek3", pudek3Ttd.getCode());
							map.put("nidn_pudek3", pudek3Ttd.getNidn());
						}
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit DetailperkuliahanForPenilaianHelper pejabat-ttd");
				}

				maps.add(map);
			}
		}

		parameters.put("terdaftar", terdaftar.size());

		String tahunAkademik = perkuliahan.getTahunAjaran();

		parameters.put("bar", "3-" + tahunAkademik + "-" + perkuliahan.getSemester() + "-" + perkuliahan.getId());

		String ttd = null;
		Dosen kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
				: perkuliahan.getJurusan().getKaprodi();
		if (kaprodi != null) {
			LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					ttd = lam.ambilFile().getAbsolutePath();

					parameters.put("ttd_kaprodi", ttd);
				}
			}
		}
		// System.out.println("ttd_kaprodi => " + ttd);

		if (perkuliahan != null) {
			int d = 1;
			for (Dosen dosena : perkuliahan.populateDosenBuNama()) {
				LampiranLain lam = LampiranLain.ambil(dosena.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						ttd = lam.ambilFile().getAbsolutePath();
						parameters.put("ttd_dosen_" + d, ttd);
						// System.out.println("ttd_dosen_" + d + " => " + ttd);
					}
				}
				d++;
			}

			if (kaprodi != null) {
				LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						ttd = lam.ambilFile().getAbsolutePath();

						parameters.put("ttd_dosen_" + d, ttd);
					}
				}
			}
		}

		try {
			// Pilih template: size 0 atau >9 → Daftar_Nilai_1 (catch-all), size 3 → Daftar_Nilai,
			// selain itu → Daftar_Nilai_<size>. Size 0 WAJIB dijabarkan eksplisit karena
			// Daftar_Nilai_0 tidak pernah ada sebagai berkas template.
			String namaTemplateDaftarNilai = (formatNilais.size() == 0 || formatNilais.size() > 9
					|| perkuliahan.getHanyaInputNilaiHuruf()) ? "Daftar_Nilai_1"
							: formatNilais.size() == 3 ? "Daftar_Nilai"
									: "Daftar_Nilai_" + formatNilais.size();
			Report.generatePDFReport(Report.PDF, parameters, namaTemplateDaftarNilai,
					ais.ui.util.WaktuUtil.getDate(), maps, Common.locale, component);
		} catch (Exception eLaporan) {
			// Berkas template .jasper laporan belum ter-deploy / gagal dibuka. Tampilkan pesan RAMAH ke
			// pengguna dan JANGAN biarkan menjadi UiException yang memenuhi log error sistem (masalah
			// deploy template, bukan bug aplikasi). Fungsi laporan tetap berjalan bila template tersedia.
			String pesanLaporan = eLaporan.getMessage();
			if (pesanLaporan == null || pesanLaporan.trim().isEmpty()) {
				pesanLaporan = "Laporan belum dapat dibuat. Silakan hubungi administrator "
						+ "untuk menyediakan berkas template laporan. "
						+ "Mohon sertakan tangkapan layar (screenshot) pesan ini saat menghubungi administrator.";
			}
			ais.ui.util.MyMessageboxConfig.show(pesanLaporan,
					"Informasi", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
		}
		terdaftar = null;
	}

	private void tampilkanAnalisisNilaiHuruf(Detailperkuliahan detailperkuliahan) throws Exception {
		if (detailperkuliahan == null) {
			return;
		}
		MyWindow window = new MyWindow("Analisis Nilai Huruf", "normal", true);
		window.setWidth(Common.isMobile() ? "100%" : "680px");
		window.setHeight(Common.isMobile() ? "90%" : "620px");
		window.setContentStyle("overflow:auto;background:#f8fafc;padding:0;");
		window.appendChild(new Html(buatHtmlAnalisisNilaiHuruf(detailperkuliahan)));
		if (window.getPage() == null && org.zkoss.zk.ui.Executions.getCurrent() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage() != null) {
			window.setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());
		}
		window.doModal();
	}

	private String buatHtmlAnalisisNilaiHuruf(Detailperkuliahan detailperkuliahan) {
		StringBuilder html = new StringBuilder();
		boolean tampilSementara = perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
				&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED);
		double total = tampilSementara ? nilaiAman(detailperkuliahan.getTotalNilaiSementara())
				: nilaiAman(detailperkuliahan.getTotalNilai());
		String huruf = tampilSementara ? detailperkuliahan.getNilaiHurufSementara() : detailperkuliahan.getNilaiHuruf();
		NilaiHuruf aturanHuruf = ambilAturanNilaiHuruf(detailperkuliahan, total);
		NilaiHuruf targetBerikut = ambilAturanNilaiHurufBerikut(detailperkuliahan, total);

		html.append("<div style='font-family:Arial,sans-serif;color:#172033;font-size:13px;line-height:1.45;'>");
		html.append("<div style='background:#0b63ce;color:white;padding:14px 18px;'>");
		html.append("<div style='font-size:18px;font-weight:bold;'>Analisis Nilai Huruf</div>");
		html.append("<div style='font-size:12px;opacity:.92;'>Rincian ini membaca komponen nilai, bobot, verifikasi, kehadiran, dan tabel Nilai Huruf yang sama dengan perhitungan sistem.</div>");
		html.append("</div>");
		html.append("<div style='padding:16px 18px;'>");

		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;margin-bottom:12px;'>");
		html.append("<div style='font-weight:bold;font-size:15px;margin-bottom:6px;'>")
				.append(teksAmanHtml(mahasiswa == null ? "Mahasiswa" : mahasiswa.getNim() + " - " + mahasiswa.getNama()))
				.append("</div>");
		html.append("<div>Nilai akhir: <b>").append(Common.numberFormat.get().format(total)).append("</b></div>");
		html.append("<div>Nilai huruf: <b>").append(teksAmanHtml(huruf == null || huruf.trim().isEmpty() ? "-" : huruf))
				.append("</b></div>");
		if (aturanHuruf != null) {
			html.append("<div>Rentang huruf ini: <b>")
					.append(Common.numberFormat.get().format(aturanHuruf.getMulai())).append(" s.d ")
					.append(Common.numberFormat.get().format(aturanHuruf.getSampai())).append("</b>");
			if (aturanHuruf.getNilaiDiIPK() != null) {
				html.append(", IP: <b>").append(Common.numberFormat.get().format(aturanHuruf.getNilaiDiIPK()))
						.append("</b>");
			}
			html.append("</div>");
		}
		if (tampilSementara) {
			html.append("<div style='margin-top:6px;color:#a16207;'>Nilai yang dianalisis adalah nilai sementara karena nilai belum diverifikasi dan setting sembunyikan nilai belum verifikasi sedang aktif.</div>");
		}
		html.append("</div>");

		html.append(buatHtmlAnalisisPintar(detailperkuliahan, total, huruf, aturanHuruf, targetBerikut,
				tampilSementara));
		html.append(buatHtmlKomponenNilai(detailperkuliahan, tampilSementara));

		String alasanNol = "";
		try {
			if (total < 0.01) {
				alasanNol = detailperkuliahan.alasanNilaiJadiNol(true, formatNilais);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;margin-top:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:6px;'>Kesimpulan</div>");
		if (alasanNol != null && !alasanNol.trim().isEmpty()) {
			html.append("<div style='color:#b91c1c;font-weight:bold;'>").append(teksAmanHtml(alasanNol)).append("</div>");
		} else if (aturanHuruf != null) {
			html.append("<div>Total <b>").append(Common.numberFormat.get().format(total)).append("</b> masuk rentang <b>")
					.append(teksAmanHtml(aturanHuruf.getNilaiHuruf())).append("</b>, sehingga sistem menampilkan nilai huruf tersebut.</div>");
		} else {
			html.append("<div>Nilai huruf belum ditemukan dari tabel konfigurasi Nilai Huruf. Periksa setting rentang nilai huruf untuk prodi/fakultas/tahun akademik ini.</div>");
		}
		if (targetBerikut != null && targetBerikut.getMulai() != null && targetBerikut.getNilaiHuruf() != null) {
			double kurang = targetBerikut.getMulai().doubleValue() - total;
			if (kurang > 0.0) {
				html.append("<div style='margin-top:6px;'>Untuk mencapai <b>")
						.append(teksAmanHtml(targetBerikut.getNilaiHuruf())).append("</b>, kurang sekitar <b>")
						.append(Common.numberFormat.get().format(kurang)).append("</b> poin dari batas bawah ")
						.append(Common.numberFormat.get().format(targetBerikut.getMulai())).append(".</div>");
			}
		}
		html.append("</div>");

		html.append("</div></div>");
		return html.toString();
	}

	private String buatHtmlAnalisisPintar(Detailperkuliahan detailperkuliahan, double total, String hurufTampil,
			NilaiHuruf aturanHuruf, NilaiHuruf targetBerikut, boolean tampilSementara) {
		StringBuilder html = new StringBuilder();
		String hurufSeharusnya = aturanHuruf == null ? "" : aturanHuruf.getNilaiHuruf();
		double persenHadir = 0.0;
		try {
			persenHadir = detailperkuliahan.hitungPersenKehadiran();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		double totalBobot = hitungTotalBobotEfektif(detailperkuliahan, tampilSementara);
		FormatNilai bobotTerbesar = ambilFormatNilaiBobotTerbesar(detailperkuliahan, tampilSementara);

		html.append("<div style='background:#f0f7ff;border:1px solid #bfdbfe;border-radius:8px;padding:12px;margin-bottom:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;color:#0b3b78;'>Analisis Pintar</div>");
		html.append("<ol style='margin:0;padding-left:20px;'>");

		if (aturanHuruf == null) {
			html.append("<li><b>Rentang nilai huruf belum cocok.</b> Sistem tidak menemukan konfigurasi Nilai Huruf untuk total ")
					.append(Common.numberFormat.get().format(total))
					.append(". Ini biasanya karena setting Nilai Huruf prodi/fakultas/tahun akademik/jenis nilai belum lengkap.</li>");
		} else if (hurufTampil == null || !hurufTampil.trim().equalsIgnoreCase(hurufSeharusnya)) {
			html.append("<li><b>Ada indikasi huruf tersimpan tidak sinkron.</b> Berdasarkan total ")
					.append(Common.numberFormat.get().format(total)).append(", sistem membaca rentang <b>")
					.append(teksAmanHtml(hurufSeharusnya)).append("</b>, tetapi yang tampil <b>")
					.append(teksAmanHtml(hurufTampil)).append("</b>. Klik Hitung Ulang/Singkronkan Nilai agar nilai huruf tersimpan mengikuti rentang terbaru.</li>");
		} else {
			html.append("<li><b>Huruf sudah konsisten.</b> Total ")
					.append(Common.numberFormat.get().format(total)).append(" berada pada rentang <b>")
					.append(teksAmanHtml(hurufSeharusnya)).append("</b> yaitu ")
					.append(Common.numberFormat.get().format(aturanHuruf.getMulai())).append(" s.d ")
					.append(Common.numberFormat.get().format(aturanHuruf.getSampai())).append(".</li>");
		}

		if (tampilSementara) {
			html.append("<li><b>Nilai belum diverifikasi.</b> Analisis memakai nilai sementara, sehingga hasil akhir dapat berubah setelah verifikasi selesai.</li>");
		}
		if (perkuliahan != null && perkuliahan.getPersenKehadiranDinilai0() > 0.1) {
			html.append("<li>Kehadiran mahasiswa <b>").append(Common.numberFormat.get().format(persenHadir))
					.append("%</b>; batas minimal agar nilai tidak menjadi 0 adalah <b>")
					.append(Common.numberFormat.get().format(perkuliahan.getPersenKehadiranDinilai0()))
					.append("%</b>.</li>");
		}
		if (perkuliahan != null && perkuliahan.getJikaAdaNilai0TidakMenghitungNilaiAkhir()) {
			html.append("<li>Aturan <b>jika ada nilai 0 maka nilai akhir tidak dihitung</b> sedang aktif. Komponen bernilai 0 wajib diperiksa.</li>");
		} else if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()) {
			html.append("<li>Aturan <b>nilai 0 tidak masuk pembagi</b> sedang aktif. Bobot komponen bernilai 0 tidak ikut membentuk rata-rata akhir.</li>");
		}
		if (totalBobot < 99.9 || totalBobot > 100.1) {
			html.append("<li><b>Total bobot efektif ").append(Common.numberFormat.get().format(totalBobot))
					.append("%</b>. Jika tidak sesuai harapan, cek bobot Format Nilai karena perhitungan memakai bobot efektif ini.</li>");
		}
		if (targetBerikut != null && targetBerikut.getMulai() != null && bobotTerbesar != null && totalBobot > 0.0) {
			double kurangTotal = targetBerikut.getMulai().doubleValue() - total;
			double bobot = nilaiAman(bobotTerbesar.getPersen());
			double perluNaikKomponen = bobot <= 0.0 ? 0.0 : kurangTotal / (bobot / totalBobot);
			if (kurangTotal > 0.0 && perluNaikKomponen > 0.0) {
				html.append("<li>Jalur tercepat untuk naik ke <b>").append(teksAmanHtml(targetBerikut.getNilaiHuruf()))
						.append("</b>: komponen berbobot terbesar adalah <b>")
						.append(teksAmanHtml(bobotTerbesar.getNama())).append("</b> (")
						.append(Common.numberFormat.get().format(bobot)).append("%). Secara kasar perlu tambahan sekitar <b>")
						.append(Common.numberFormat.get().format(perluNaikKomponen))
						.append("</b> poin pada komponen itu, selama nilai maksimal komponen masih memungkinkan.</li>");
			}
		}
		html.append("</ol>");
		html.append("</div>");
		return html.toString();
	}

	private String buatHtmlKomponenNilai(Detailperkuliahan detailperkuliahan, boolean tampilSementara) {
		StringBuilder html = new StringBuilder();
		double totalBobot = 0.0;
		List<Object[]> baris = new ArrayList<Object[]>();
		double kontribusiMax = -1.0;
		double kontribusiMin = 999999.0;
		String namaMax = "";
		String namaMin = "";

		if (formatNilais != null) {
			for (FormatNilai formatNilai : formatNilais) {
				if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
					continue;
				}
				double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
						: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
				double bobot = nilaiAman(formatNilai.getPersen());
				boolean bobotMasuk = !(perkuliahan != null
						&& perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir() && nilai < 0.01);
				if (bobotMasuk) {
					totalBobot += bobot;
				}
				baris.add(new Object[] { formatNilai.getNama(), Double.valueOf(nilai), Double.valueOf(bobot),
						Boolean.valueOf(bobotMasuk), Boolean.valueOf(detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai)) });
			}
		}

		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;'>Komponen Pembentuk Nilai</div>");
		html.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
		html.append("<tr style='background:#eef4fb;'><th style='text-align:left;padding:6px;border:1px solid #dbe5f0;'>Komponen</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Nilai</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Bobot</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Kontribusi</th><th style='text-align:center;padding:6px;border:1px solid #dbe5f0;'>Ver.</th></tr>");
		for (Object[] data : baris) {
			String nama = data[0] == null ? "" : data[0].toString();
			double nilai = nilaiAman((Double) data[1]);
			double bobot = nilaiAman((Double) data[2]);
			boolean bobotMasuk = ((Boolean) data[3]).booleanValue();
			boolean verifikasi = ((Boolean) data[4]).booleanValue();
			double kontribusi = totalBobot > 0.0 && bobotMasuk ? nilai * (bobot / totalBobot) : 0.0;
			if (kontribusi > kontribusiMax) {
				kontribusiMax = kontribusi;
				namaMax = nama;
			}
			if (kontribusi < kontribusiMin) {
				kontribusiMin = kontribusi;
				namaMin = nama;
			}
			html.append("<tr>");
			html.append("<td style='padding:6px;border:1px solid #dbe5f0;'>").append(teksAmanHtml(nama));
			if (!bobotMasuk) {
				html.append("<div style='color:#a16207;font-size:11px;'>Bobot tidak masuk pembagi karena nilai 0.</div>");
			}
			html.append("</td>");
			html.append("<td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>").append(Common.numberFormat.get().format(nilai)).append("</td>");
			html.append("<td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>").append(Common.numberFormat.get().format(bobot)).append("%</td>");
			html.append("<td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>").append(Common.numberFormat.get().format(kontribusi)).append("</td>");
			html.append("<td style='text-align:center;padding:6px;border:1px solid #dbe5f0;'>").append(verifikasi ? "Ya" : "Belum").append("</td>");
			html.append("</tr>");
		}
		html.append("</table>");
		if (baris.isEmpty()) {
			html.append("<div style='color:#64748b;margin-top:8px;'>Belum ada komponen format nilai aktif yang dapat dianalisis.</div>");
		} else {
			html.append("<div style='margin-top:8px;color:#334155;'>Total bobot pembagi: <b>")
					.append(Common.numberFormat.get().format(totalBobot)).append("%</b>.</div>");
			html.append("<div style='margin-top:4px;color:#334155;'>Kontribusi terbesar berasal dari <b>")
					.append(teksAmanHtml(namaMax)).append("</b>");
			if (namaMin != null && !namaMin.trim().isEmpty()) {
				html.append(", sedangkan kontribusi terkecil dari <b>").append(teksAmanHtml(namaMin)).append("</b>");
			}
			html.append(".</div>");
		}
		html.append("</div>");
		return html.toString();
	}

	private NilaiHuruf ambilAturanNilaiHuruf(Detailperkuliahan detailperkuliahan, double total) {
		try {
			Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null ? detailperkuliahan.getMatakuliahKonversi()
					: detailperkuliahan.getPerkuliahan().getMatakuliah();
			Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
			Fakultas fakultas = jurusan == null ? null : jurusan.getFakultas();
			return Common.getNilaiHuruf(Double.valueOf(total), mahasiswa == null ? null : mahasiswa.getTahunangkatan(),
					jurusan, fakultas, detailperkuliahan.getTahunAkademik(),
					detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
					matakuliah == null ? "" : matakuliah.getKode(),
					matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private NilaiHuruf ambilAturanNilaiHurufBerikut(Detailperkuliahan detailperkuliahan, double total) {
		NilaiHuruf kandidat = null;
		try {
			for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
				if (nilaiHuruf == null || nilaiHuruf.getMulai() == null || nilaiHuruf.getNilaiHuruf() == null
						|| nilaiHuruf.getMulai().doubleValue() <= total) {
					continue;
				}
				NilaiHuruf cocok = ambilAturanNilaiHuruf(detailperkuliahan, nilaiHuruf.getMulai().doubleValue());
				if (cocok == null || cocok.getNilaiHuruf() == null
						|| !cocok.getNilaiHuruf().equalsIgnoreCase(nilaiHuruf.getNilaiHuruf())) {
					continue;
				}
				if (kandidat == null || nilaiHuruf.getMulai().doubleValue() < kandidat.getMulai().doubleValue()) {
					kandidat = nilaiHuruf;
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return kandidat;
	}

	private double hitungTotalBobotEfektif(Detailperkuliahan detailperkuliahan, boolean tampilSementara) {
		double totalBobot = 0.0;
		if (formatNilais == null) {
			return totalBobot;
		}
		for (FormatNilai formatNilai : formatNilais) {
			if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
				continue;
			}
			double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
					: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
			if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()
					&& nilai < 0.01) {
				continue;
			}
			totalBobot += nilaiAman(formatNilai.getPersen());
		}
		return totalBobot;
	}

	private FormatNilai ambilFormatNilaiBobotTerbesar(Detailperkuliahan detailperkuliahan, boolean tampilSementara) {
		FormatNilai kandidat = null;
		double bobotTerbesar = -1.0;
		if (formatNilais == null) {
			return null;
		}
		for (FormatNilai formatNilai : formatNilais) {
			if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
				continue;
			}
			double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
					: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
			if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()
					&& nilai < 0.01) {
				continue;
			}
			if (formatNilai.getPersen().doubleValue() > bobotTerbesar) {
				bobotTerbesar = formatNilai.getPersen().doubleValue();
				kandidat = formatNilai;
			}
		}
		return kandidat;
	}

	private double nilaiAman(Double nilai) {
		if (nilai == null || nilai.isNaN() || nilai.isInfinite()) {
			return 0.0;
		}
		return nilai.doubleValue();
	}

	private String teksAmanHtml(String teks) {
		if (teks == null) {
			return "";
		}
		return teks.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

}
