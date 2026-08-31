package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.action.master.dashboard.admin.DashboardAkademikHtmlCssHelper;
import ais.common.AbsensiTrenCache;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CommonVO;
import ais.database.model.DetailKelasPertemuan;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.KelasPertemuan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.Pegawai;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.PengajuanIzinTidakMasukPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Ruang;
import ais.database.model.RuangPaketPMB;
import ais.database.model.StatusPertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOMahasiswa;
import ais.database.model.asset.Lokasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.ProdukPeserta;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHtml;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Helper terfokus untuk absensi. Tipe ini membungkus satu variasi kecil dari alur yang lebih umum
 * agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox metode}, {@code Combobox
 * ujian}, {@code Textbox bukuRujukan1}, {@code Textbox bukuRujukan2}, {@code Textbox dosenTamu}, {@code Textbox
 * dosenTamu2}, {@code MyDatebox tanggal}, {@code MyDatebox tanggalRealisasi}; inisialisasi/lifecycle ({@code
 * mainInit()}, {@code initKelasPertemuan()}); pembacaan/pencarian ({@code reload()}, {@code
 * createListMahasiswaAbsensi()}, {@code reloadSejarahAbsensiOnline()}, {@code tampilkanAbsensiOnline()}, {@code
 * tampilBawah()}, {@code createListMahasiswaIzin()}); validasi/perhitungan ({@code boleh()}, {@code
 * bolehKonfirmasi()}, {@code bolehKonfirmasiRps()}, {@code bolehOlehAkademik()}); mutasi data ({@code
 * ubahTerlewat()}); operasi domain lain ({@code createTombolAbsen()}, {@code createTombolAbsen()}, {@code
 * populateMahasiswaDariPertemuan()}, {@code bagianInfo()}, {@code gayaKartuPresensi()}, {@code badgeStatus()});
 * konfigurasi constructor: {@code statusabsensis}. Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class AbsensiHelper {

	// private Textbox topik;
	private Textbox metode;
	private Combobox ujian;
	private Textbox bukuRujukan1;
	private Textbox bukuRujukan2;
	private Textbox dosenTamu;
	private Textbox dosenTamu2;
	private MyDatebox tanggal;
	private MyDatebox tanggalRealisasi;
	private Timebox waktuMulai;
	private Timebox waktuSelesai;
	private AmbilDataRuangBanbox ruang;

	private List<? extends GeneralValueObject> mahasiswas;
	private Perkuliahan perkuliahan;

	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private Component tabpanelUtama;
	private MyGrid mahasiswaIzinGrid;
	private boolean mahasiswaBolehUbahAbsen;
//	private Center center;
	private Textbox topik;
	private List<Statusabsensi> statusabsensis;
	private Collection<Dosen> listDosen = null;
	private boolean tampilInfo;
	private Combobox onlineMenggunakan;
	private Row rowMeetKeterangan;
	private Row rowMeet;
	private Textbox zoomLink;
	private Row rowLinkZoom;
	private Row rowLinkZoomKeterangan;
	private Row rowLinkZoomButton;
	private Row rowLinkBbbKeterangan;
	private Row rowLinkBbb;
	private Textbox bbbLink;
	private Row rowLinkBbbButton;
	private Row rowLinkZoomLink;
	private Row rowLinkBbbLink;
	private Row rowLinkSkypeKeterangan;
	private Row rowLinkSkypeLink;
	private Row rowLinkSkype;
	private Textbox skypeLink;
	private Row rowLinkSkypeButton;
	private Row rowLinkWa;
	private Textbox waLink;
	private Row rowLinkWaButton;
	private Row rowLinkWaKeterangan;
	private Row rowLinkMeetLink;
	private Textbox meetLink;
	private Row rowLinkMeetButton;
	private MyCheckboxConfig perkulaiahnOnlineHarusSesuaiJadwal;
	private Row rowLinkLain;
	private Textbox linkLain;
	private Row rowLinkLainKeterangan;
	private boolean mobile;
	private MyCheckboxConfig dosenBolehAbsenMenggunakanFoto;
	private MyCheckboxConfig mahasiswaBolehAbsenMenggunakanFoto;
	private Row rowUtamaAbsensiOnline;
	private MyIntbox bolehAbsenSebelumWaktuMulaiDalamMenit;
	private MyIntbox bolehAbsenSetelahWaktuMulaiDalamMenit;
	private Statusabsensi status = null;
	private Tbmuser tbmuser = null;
	private Combobox lokasi;
	private MyDoublebox jarak;

	public AbsensiHelper(final Mahasiswa mahasiswa, final BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		Session session = HibernateUtil.currentSession();
		statusabsensis = ConstantValues.simpleList(
				session.createCriteria(Statusabsensi.class)
						.add(Restrictions.not(Restrictions.or(Restrictions.ilike("nama", "belajar", MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", "cuti", MatchMode.ANYWHERE),
										Restrictions.ilike("nama", "dinas", MatchMode.ANYWHERE))))),
				Statusabsensi.class);
	}

	public static Component createTombolAbsen(final Pertemuan pertemuan, final DataLoader dataLoader) {
		return createTombolAbsen(pertemuan, true, dataLoader);
	}

	public static Component createTombolAbsen(final Pertemuan pertemuan, boolean vertical,
			final DataLoader dataLoader) {
		try {
			MyToolbarbutton a = new MyToolbarbutton("fa-address-book", "Kehadiran");

			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null).display(pertemuan, dataLoader, 0);
				}
			});
			Map<String, Integer> statuses = pertemuan.hitungStatus();
			String abs = statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

			int p = pertemuan.ambilJumlahPengajuanIzinTidakMasukPerkuliahan();

			String absen = (p == 0 ? "" : "P=" + p + (abs.isEmpty() ? "" : ", ")) + abs;

			if (!absen.trim().isEmpty()) {
				Vbox vbox = new Vbox();
				vbox.appendChild(a);
				MyLabelKecil labelKecil = new MyLabelKecil(absen);
				labelKecil.setStyle("font-size:8px;color:blue;");
				vbox.appendChild(labelKecil);
				return vbox;
			} else {
				return a;
			}
		} catch (Exception e) {
			return new Label();
		}
	}

	@SuppressWarnings({ })
	public static List<? extends GeneralValueObject> populateMahasiswaDariPertemuan(Pertemuan pertemuan) {

		Perkuliahan perkuliahan = pertemuan.getPerkuliahan();

		List<? extends GeneralValueObject> mahasiswas = new ArrayList<GeneralValueObject>();
		Session session = HibernateUtil.currentSession();

		if (pertemuan.getKomponenDataProdukKursus() != null) {
			mahasiswas = ConstantValues.simpleList(
					session.createCriteria(ProdukPeserta.class).setProjection(Projections.property("pesertaKursus.id"))
							.add(Restrictions.ilike("komponens",
									"," + pertemuan.getKomponenDataProdukKursus().getId() + ",", MatchMode.ANYWHERE))

							.createAlias("pesertaKursus", "pesertaKursus")
							.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim") ? Order.asc("pesertaKursus.kode")
											: Order.asc("pesertaKursus.nama")),
					PesertaKursus.class, false);
		}

		else if (pertemuan.getJadwalUjianPMB() != null) {

			if (pertemuan.getJadwalUjianPMB().getUjianPMB() != null
					&& pertemuan.getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null
					&& pertemuan.getJadwalUjianPMB().getPesertaUjianHarusTelahUjian()) {

				mahasiswas = ConstantValues.simpleList(
						session.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("mulaiPada"))
								.add(Restrictions.isNotNull("keyhasil"))
								.setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"))
								.add(Restrictions.isNotNull("biodataCalonMahasiswa"))
								.createAlias("pertemuanPunyaUjian", "pertemuanPunyaUjian")
								.add(Restrictions.eq("pertemuanPunyaUjian.pertemuan", pertemuan)),
						BiodataCalonMahasiswa.class, false);

			}

			else if (pertemuan.getJadwalUjianPMB().getUjianPMB() != null
					&& pertemuan.getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null
					&& !pertemuan.getJadwalUjianPMB().getRuanganYgIkut().isEmpty()) {

				mahasiswas = ConstantValues.simpleList(
						session.createCriteria(RuangPaketPMB.class)
								.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
								.add(pertemuan.getJadwalUjianPMB().getPesertaUjianHarusPunyaNomorUjian()
										? Restrictions.and(Restrictions.ne("biodataCalonMahasiswa.noUjian", ""),
												Restrictions.isNotNull("biodataCalonMahasiswa.noUjian"))
										: Restrictions.sqlRestriction("true"))

								.setProjection(Projections.property("biodataCalonMahasiswa.id"))
								.add(Restrictions.sqlRestriction(
										"ruang_pmb in (-1" + pertemuan.getJadwalUjianPMB().getRuanganYgIkut() + "-1)")),
						BiodataCalonMahasiswa.class, false);

			}

			else if (pertemuan.getJadwalUjianPMB().getUjianPMB() != null
					&& pertemuan.getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null) {
				mahasiswas = ConstantValues.simpleList(session.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(pertemuan.getJadwalUjianPMB().getPesertaUjianHarusPunyaNomorUjian()
								? Restrictions.and(Restrictions.ne("noUjian", ""), Restrictions.isNotNull("noUjian"))
								: Restrictions.sqlRestriction("true"))
						.add(pertemuan.getJadwalUjianPMB().getPaket() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("paket", pertemuan.getJadwalUjianPMB().getPaket()))
						.add(Restrictions.eq("gelombangPendaftaran",
								pertemuan.getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran()))
						.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim") ? Order.asc("noRegistrasi") : Order.asc("nama")),
						BiodataCalonMahasiswa.class);

			} else {

				mahasiswas = ConstantValues.simpleList(
						session.createCriteria(RuangPaketPMB.class)
								.setProjection(Projections.property("biodataCalonMahasiswa.id"))
								.createAlias("ruangPMB", "ruangPMB")
								.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
								.add(Restrictions.eq("ruangPMB.ujianPMB", pertemuan.getJadwalUjianPMB().getUjianPMB()))
								.add(pertemuan.getJadwalUjianPMB().getPaket() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("biodataCalonMahasiswa.paket",
												pertemuan.getJadwalUjianPMB().getPaket()))
								.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim")
												? Order.asc("biodataCalonMahasiswa.noRegistrasi")
												: Order.asc("biodataCalonMahasiswa.nama")),
						BiodataCalonMahasiswa.class, false);
			}
		}

		else if (perkuliahan != null) {
			Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, "",
					!Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim"),
					false);
			List<Mahasiswa> listMhs = new ArrayList<Mahasiswa>();
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null && detailperkuliahan.getMahasiswa() != null
						&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
					listMhs.add(detailperkuliahan.getMahasiswa());
				}
			}
			mahasiswas = new ArrayList<GeneralValueObject>(listMhs);
		} else if (pertemuan.getFormulirKegiatan() != null) {

			mahasiswas = ConstantValues.simpleList(
					session.createCriteria(FormulirKegiatanPeserta.class)
							.setProjection(Projections.property("mahasiswa.id"))
							.add(Restrictions.eq("formulirKegiatan", pertemuan.getFormulirKegiatan()))
							.createCriteria("mahasiswa")
							.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim") ? Order.asc("nim") : Order.asc("nama")),
					Mahasiswa.class, false);

		} else if (pertemuan.getWisuda() != null) {
			mahasiswas = ConstantValues.simpleList(
					session.createCriteria(PendaftaranWisuda.class).add(Restrictions.eq("persetujuanWisuda", true))
							.setProjection(Projections.property("mahasiswa.id"))
							.add(Restrictions.eq("wisuda", pertemuan.getWisuda())).createCriteria("mahasiswa")
							.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim") ? Order.asc("nim") : Order.asc("nama")),
					Mahasiswa.class, false);
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			List<Mahasiswa> listMhs = new ArrayList<Mahasiswa>();
			listMhs.add(pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa());
			mahasiswas = new ArrayList<GeneralValueObject>(listMhs);
		} else if (pertemuan.getSkripsi() != null) {
			List<Mahasiswa> listMhs = new ArrayList<Mahasiswa>();
			listMhs.add(pertemuan.getSkripsi().getMahasiswa());
			mahasiswas = new ArrayList<GeneralValueObject>(listMhs);
		} else if (pertemuan.getKelompokKkn() != null) {
			mahasiswas = ConstantValues.simpleList(session.createCriteria(MahasiswaDapatKelompokKkn.class)
					.add(Restrictions.eq("diterima", true)).setProjection(Projections.property("mahasiswa.id"))
					.add(Restrictions.eq("kelompokKkn", pertemuan.getKelompokKkn())).createCriteria("mahasiswa")
					.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nama")), Mahasiswa.class, false);
		} else if (pertemuan.getKelompokPkl() != null) {
			mahasiswas = ConstantValues.simpleList(session.createCriteria(MahasiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("diterima", true)).setProjection(Projections.property("mahasiswa.id"))
					.add(Restrictions.eq("kelompokPkl", pertemuan.getKelompokPkl())).createCriteria("mahasiswa")
					.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nama")), Mahasiswa.class, false);
		} else if (pertemuan.getKrsMahasiswa() != null) {
			List<Mahasiswa> listMhs = new ArrayList<Mahasiswa>();
			listMhs.add(pertemuan.getKrsMahasiswa().getMahasiswa());
			mahasiswas = new ArrayList<GeneralValueObject>(listMhs);
		}

		return mahasiswas;
	}

	private void ubahTerlewat(Pertemuan pertemuan) {
		if (Common.bolehKonfigurasi("absen_harus_sesuai_waktu")) {
			Date currentDate = WaktuUtil.getDate();

			Integer selisih = pertemuan.getTanggal() == null ? 0
					: Math.abs(Common.getBetweenTwoDates(currentDate, pertemuan.getTanggal())) - 1;

			Integer toleransiHari = pertemuan.getPerkuliahan() == null ? 1000
					: pertemuan.getPerkuliahan().getBatasWaktuBolehAbsenKehadiran();
			if (Common.bolehKonfigurasi("jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF)) {
				try {
					toleransiHari = Integer.parseInt(
							Common.getKonfigurasi("jumlah_hari_batas_waktu_dalam_hari", "0").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:415");
					// TODO: handle exception
				}
			}

			terlewat = pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() && selisih > toleransiHari;

			terlewatInfo = "Pengisian kehadiran pada tanggal " + Common.dateFormat5.get().format(currentDate)
					+ " tidak dapat dilakukan. Waktu saat ini berada di luar periode yang diizinkan " + "(selisih "
					+ selisih + " hari dari jadwal). " + "Batas toleransi pengisian adalah " + toleransiHari
					+ " hari sebelum/sesudah pertemuan.";

		} else {
			terlewat = false;
		}
	}

	@SuppressWarnings({ "deprecation" })
	private Component bagianInfo(final Pertemuan pertemuan) throws Exception {

		final EventListener sesuaikan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pertemuan a = (Pertemuan) arg0.getData();
				sesuaikan(a, false);

				ubahTerlewat(pertemuan);

			}
		};

		ubahTerlewat(pertemuan);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("120px");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (tampilInfo) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setValign("top");

		if (pertemuan.getPerkuliahan() != null) {

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Waktu Perkuliahan"));

			Vbox vbox = new Vbox();
			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);
			row.appendChild(vbox);
		}

		EventListener updateLocal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				sesuaikan(pertemuan, true);

				ubahTerlewat(pertemuan);

				if (arg0 != null
						&& (arg0.getTarget() == perkulaiahnOnlineHarusSesuaiJadwal || arg0.getTarget() == tanggal)) {
					reload(pertemuan);
				}
			}
		};

		if (terlewat && pertemuan.getPerkuliahan() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.setValign("top");

			row.appendChild(new ais.ui.util.MyLabelConfig("Informasi"));
			row.appendChild(new ais.ui.util.MyLabelAgakKecilBoldMerah(terlewatInfo));
		} else if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null) {

			row = new MyFormRow();
			row.setParent(rows);
			row.setValign("top");

			row.appendChild(new ais.ui.util.MyLabelConfig("Absen"));
			Vbox toolbar = new Vbox();
			row.appendChild(toolbar);

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Scan QR-Code KTM", "/img/QR-Code-icon_.png");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					String host = URLEncoder
							.encode(Common.getRequestHostWithProtocol() + "/Absen?id=" + pertemuan.getId(), "UTF-8");

					String src = Common.getRequestHostWithProtocol() + "/read_qr_code_kartu.jsp?q=" + host;

					final MyWindow window = new MyWindow("Absen via QR-Code KTM", "none", false);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src
							+ "\" style=\"width:100%;height:1500px;border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html.setHeight("1500px");
					Common.tampilanScroll(center).appendChild(html);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
							reload(pertemuan);
						}
					});
					cancel.setParent(toolbar);
					boolean mobile = Common.isMobile();
					window.setVisible(true);
					window.setHeight("97%");
					window.setWidth(mobile ? "97%" : "750px");
					window.onModal();

				}
			});
			cancel.setParent(toolbar);

			cancel = new MyToolbarbuttonConfig("Scan RFID", "/img/QR-Code-icon_.png");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					String host = URLEncoder
							.encode(Common.getRequestHostWithProtocol() + "/Absen?id=" + pertemuan.getId(), "UTF-8");

					String src = Common.getRequestHostWithProtocol() + "/read_rfid_kartu.jsp?q=" + host;

					final MyWindow window = new MyWindow("Absen via RFID", "none", false);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src
							+ "\" style=\"width:100%;height:1500px;border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html.setHeight("1500px");
					Common.tampilanScroll(center).appendChild(html);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
							reload(pertemuan);
						}
					});
					cancel.setParent(toolbar);
					boolean mobile = Common.isMobile();
					window.setVisible(true);
					window.setHeight("97%");
					window.setWidth(mobile ? "97%" : "750px");
					window.onModal();

				}
			});
			cancel.setParent(toolbar);

		}

		row = new MyFormRow();
		row.setValign("top");

		topik = new Textbox(pertemuan.getTopik());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan akhir pembelajaran *"));
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(pertemuan.getTopik()));
		} else {
			row.appendChild(topik);
		}
//		if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getSiswa() == null) {
//			topik.setConstraint("no empty");
//		}
		topik.setWidth("90%");
		topik.setRows(4);
		topik.addEventListener("onChange", updateLocal);

		bukuRujukan1 = new Textbox(pertemuan.getBukuRujukan1());
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahan Kajian *"));

		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(pertemuan.getBukuRujukan1()));
		} else {
			row.appendChild(bukuRujukan1);
		}
//		if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getSiswa() == null) {
//			bukuRujukan1.setConstraint("no empty");
//		}
		bukuRujukan1.setWidth("90%");
		bukuRujukan1.setRows(2);
		bukuRujukan1.addEventListener("onChange", updateLocal);

		bukuRujukan2 = new Textbox(pertemuan.getBukuRujukan2());
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Daftar Pustaka"));
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(pertemuan.getBukuRujukan2()));
		} else {
			row.appendChild(bukuRujukan2);
		}
		bukuRujukan2.setWidth("90%");
		bukuRujukan2.setRows(2);
		bukuRujukan2.addEventListener("onChange", updateLocal);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis (*)"));
		ujian = new Combobox();
		Common.insertCombo(ujian, "nama", StatusPertemuan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(ujian, pertemuan.getStatusPertemuan());

		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(
					new Label(pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama()));
		} else {
			row.appendChild(ujian);
		}
		ujian.setWidth("90%");
		ujian.setReadonly(true);
		ujian.addEventListener("onChange", updateLocal);

		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {

		} else {
			Common.initKeterangan(rows, "Untuk pertemuan Online, harap ubah jenis pertemuanya menjadi Daring.");
		}

		final Dosen dosenUtamaOk = pertemuan.dosenUtama();
		if (dosenUtamaOk != null && !dosenUtamaOk.getOnlineMenggunakan().equals(Dosen.TIDAK_AKTIF)
				&& !dosenUtamaOk.getOnlineLink().trim().isEmpty()) {
			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Media Online (*)"));
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Online menggunakan media online dari dosen")));
		} else {
			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Media Online (*)"));
			onlineMenggunakan = new Combobox();

			Comboitem mediaOnline = new Comboitem("Jitsi", "/img/jitsi.png");
			mediaOnline.setValue(Pertemuan.JITSI);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Google Meet", "/img/meet-google.png");
			mediaOnline.setValue(Pertemuan.GOOGLE_MEET);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Zoom", "/img/zoom.png");
			mediaOnline.setValue(Pertemuan.ZOOM);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Big Blue Button", "/img/bbb.png");
			mediaOnline.setValue(Pertemuan.BBB);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Skype", "/img/Skype-icon.png");
			mediaOnline.setValue(Pertemuan.SKYPE);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Grup Whatsapp", "/img/svg/whats.svg");
			mediaOnline.setValue(Pertemuan.WA);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Lain-Lain", "/img/online-red-icon.png");
			mediaOnline.setValue(Pertemuan.LAIN);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Tidak Ada Pertemuan Online", "/img/svg/trash.svg");
			mediaOnline.setValue(Pertemuan.TIDAK_AKTIF);
			onlineMenggunakan.appendChild(mediaOnline);

			Common.selectComboItem(onlineMenggunakan, pertemuan.getOnlineMenggunakan());
			onlineMenggunakan.setCols(7);

			Hbox myonlineMenggunakan = new Hbox();
			if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
				row.appendChild(new Label(onlineMenggunakan.getValue()));
			} else {
				row.appendChild(myonlineMenggunakan);
			}
			myonlineMenggunakan.appendChild(onlineMenggunakan);

			final MyToolbarbuttonConfig testButton = new MyToolbarbuttonConfig("Tes Online Sekarang");
			myonlineMenggunakan.appendChild(testButton);
			testButton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Integer ol = (Integer) onlineMenggunakan.getSelectedItem().getValue();
					String url = "";
					if (ol.equals(Pertemuan.GOOGLE_MEET)) {
						String l = pertemuan.getMeetLink();
						url = l + "?hs=122&ijlm=1588886137268";
					} else if (ol.equals(Pertemuan.JITSI)) {
						url = pertemuan.generateJitsiLink();
					} else if (ol.equals(Pertemuan.ZOOM)) {
						url = pertemuan.getZoomLink();
					} else if (ol.equals(Pertemuan.BBB)) {
						url = pertemuan.getBbbLink();
					} else if (ol.equals(Pertemuan.SKYPE)) {
						url = pertemuan.getSkypeLink();
					} else if (ol.equals(Pertemuan.WA)) {
						url = pertemuan.getWaLink();
					} else if (ol.equals(Pertemuan.LAIN)) {
						url = pertemuan.getLainLink();
					}
					if (url == null || url.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Untuk pertemuan online menggunakan Gogle Meet, Zoom, Big Blue Button, atau Skype, atau WA, harap masukkan link online secara benar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + url + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			onlineMenggunakan.setReadonly(true);
			onlineMenggunakan.addEventListener("onChange", updateLocal);

			rowMeetKeterangan = Common.initKeterangan(rows,
					"Untuk pertemuan Online menggunakan Google Meet, harap memasukkan link Google Meet di bawah ini..");

			rowLinkMeetLink = new MyFormRow();
			rowLinkMeetLink.setValign("top");
			rowLinkMeetLink.setParent(rows);
			rowLinkMeetLink.appendChild(new ais.ui.util.MyLabelConfig(""));
			A linkMeetSignup;
			rowLinkMeetLink.appendChild(linkMeetSignup = new A(
					"Klik disini dan login untuk mendapatkan link Google Meet yang baru, https://meet.google.com/"));
			linkMeetSignup.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String server = "https://meet.google.com/";

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			rowMeet = new MyFormRow();
			rowMeet.setValign("top");
			rowMeet.setParent(rows);
			rowMeet.appendChild(new ais.ui.util.MyLabelConfig("Link Meet *"));
			rowMeet.appendChild(meetLink = new Textbox(pertemuan.getMeetLink()));
			meetLink.setWidth("90%");
			meetLink.setRows(2);
			meetLink.addEventListener("onChange", updateLocal);

			rowLinkMeetButton = Common.initKeterangan(rows,
					"Secara default, link meet akan menggunakan link meet dari pertemuan sebelumnya..");

			// rowMeet.appendChild(AktifitasPerkuliahanHelper.createCalendarButton(pertemuan,
			// Common.getCurrentUser(),
			// true, new DataLoader() {
			//
			// @Override
			// public void loadData(Object value) {
			//
			// }
			// }));

			rowLinkZoomKeterangan = Common.initKeterangan(rows,
					"Untuk pertemuan Online menggunakan Zoom, harap memasukkan link zoom di bawah ini. Contoh link zoom : https://us04web.zoom.us/j/4445712881?pwd=ZnNReHRJYXVRem8zRkc5OFpPd3I3QT09");

			rowLinkZoomLink = new MyFormRow();
			rowLinkZoomLink.setValign("top");
			rowLinkZoomLink.setParent(rows);
			rowLinkZoomLink.appendChild(new ais.ui.util.MyLabelConfig(""));
			A linkZoomSignup;
			rowLinkZoomLink.appendChild(linkZoomSignup = new A(
					"Klik disini dan login untuk mendapatkan link zoom yang baru, https://zoom.us/signin"));
			linkZoomSignup.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String server = "https://zoom.us/signin";

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			rowLinkZoom = new MyFormRow();
			rowLinkZoom.setValign("top");
			rowLinkZoom.setParent(rows);
			rowLinkZoom.appendChild(new ais.ui.util.MyLabelConfig("Link Zoom *"));
			rowLinkZoom.appendChild(zoomLink = new Textbox(pertemuan.getZoomLink()));
			zoomLink.setWidth("90%");
			zoomLink.setRows(2);
			zoomLink.addEventListener("onChange", updateLocal);

			rowLinkZoomButton = Common.initKeterangan(rows,
					"Secara default, link zoom akan menggunakan link zoom dari pertemuan sebelumnya..");

			rowLinkBbbKeterangan = Common.initKeterangan(rows,
					"Untuk pertemuan Online menggunakan Big Blue Button, harap memasukkan link Big Blue Button di bawah ini. Contoh link bbb : https://demo.bigbluebutton.org/gl/muh-jjn-72p");

			rowLinkBbbLink = new MyFormRow();
			rowLinkBbbLink.setValign("top");
			rowLinkBbbLink.setParent(rows);
			rowLinkBbbLink.appendChild(new ais.ui.util.MyLabelConfig(""));
			A linkBbbSignup;
			rowLinkBbbLink.appendChild(linkBbbSignup = new A(
					"Klik disini dan login untuk mendapatkan link Big Blue Button yang baru, https://demo.bigbluebutton.org/gl/signin"));

			linkBbbSignup.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String server = "https://demo.bigbluebutton.org/gl/signin";

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			rowLinkBbb = new MyFormRow();
			rowLinkBbb.setValign("top");
			rowLinkBbb.setParent(rows);
			rowLinkBbb.appendChild(new ais.ui.util.MyLabelConfig("Link Big Blue Button *"));
			rowLinkBbb.appendChild(bbbLink = new Textbox(pertemuan.getBbbLink()));
			bbbLink.setWidth("90%");
			bbbLink.setRows(2);
			bbbLink.addEventListener("onChange", updateLocal);

			rowLinkBbbButton = Common.initKeterangan(rows,
					"Secara default, link Big Blue Button akan menggunakan link Big Blue Button dari pertemuan sebelumnya..");

			rowLinkSkypeKeterangan = Common.initKeterangan(rows,
					"Untuk pertemuan Online menggunakan Skype, harap memasukkan link Skype di bawah ini. Contoh link skype : https://join.skype.com/Ut2b1onFnJnD");

			rowLinkSkypeLink = new MyFormRow();
			rowLinkSkypeLink.setValign("top");
			rowLinkSkypeLink.setParent(rows);
			rowLinkSkypeLink.appendChild(new ais.ui.util.MyLabelConfig(""));
			A linkSkypeSignup;
			rowLinkSkypeLink.appendChild(linkSkypeSignup = new A(
					"Klik disini dan login untuk mendapatkan link Skype yang baru, https://web.skype.com"));

			linkSkypeSignup.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String server = "https://web.skype.com";

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			rowLinkSkype = new MyFormRow();
			rowLinkSkype.setValign("top");
			rowLinkSkype.setParent(rows);
			rowLinkSkype.appendChild(new ais.ui.util.MyLabelConfig("Link Skype *"));
			rowLinkSkype.appendChild(skypeLink = new Textbox(pertemuan.getSkypeLink()));
			skypeLink.setWidth("90%");
			skypeLink.setRows(2);
			skypeLink.addEventListener("onChange", updateLocal);

			rowLinkSkypeButton = Common.initKeterangan(rows,
					"Secara default, link Skype akan menggunakan link Skype dari pertemuan sebelumnya..");

			rowLinkWa = new MyFormRow();
			rowLinkWa.setValign("top");
			rowLinkWa.setParent(rows);
			rowLinkWa.appendChild(new ais.ui.util.MyLabelConfig("Link Grup Whatsapp *"));
			rowLinkWa.appendChild(waLink = new Textbox(pertemuan.getWaLink()));
			waLink.setWidth("90%");
			waLink.setRows(2);
			waLink.addEventListener("onChange", updateLocal);

			rowLinkWaButton = Common.initKeterangan(rows,
					"Secara default, link Grup Whatsapp akan menggunakan link Grup Whatsapp dari pertemuan sebelumnya..");

			rowLinkWaKeterangan = Common.initKeterangan(rows,
					"Untuk pertemuan Online menggunakan Grup WA, harap memasukkan link WA di atas. Untuk membuat link Grup WA, buka aplikasi WA Grup Anda (harus sebagai admin) atau buat grup WA baru, pilih Grup Info, dan pilih undang via link.. Contoh link : https://chat.whatsapp.com/Djx0r98Z30YTmFmEZGJ3");

			rowLinkLain = new MyFormRow();
			rowLinkLain.setValign("top");
			rowLinkLain.setParent(rows);
			rowLinkLain.appendChild(new ais.ui.util.MyLabelConfig("Link Media Online *"));
			rowLinkLain.appendChild(linkLain = new Textbox(pertemuan.getLainLink()));
			linkLain.setWidth("90%");
			linkLain.setRows(2);
			linkLain.addEventListener("onChange", updateLocal);
			rowLinkLainKeterangan = Common.initKeterangan(rows,
					"Untuk tatap muka online menggunakan media onlien lain, harap memasukkan link media tersebut di bawah ini.");

			EventListener eventListenerOl = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Integer ol = (Integer) onlineMenggunakan.getSelectedItem().getValue();

					rowMeetKeterangan.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.GOOGLE_MEET));
					rowMeet.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.GOOGLE_MEET));
					rowLinkMeetLink.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.GOOGLE_MEET));
					rowLinkMeetButton.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.GOOGLE_MEET));

					rowLinkZoomKeterangan.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.ZOOM));
					rowLinkZoom.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.ZOOM));
					rowLinkZoomButton.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.ZOOM));
					rowLinkZoomLink.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.ZOOM));

					rowLinkBbbKeterangan.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.BBB));
					rowLinkBbb.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.BBB));
					rowLinkBbbButton.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.BBB));
					rowLinkBbbLink.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.BBB));

					rowLinkSkypeKeterangan.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.SKYPE));
					rowLinkSkype.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.SKYPE));
					rowLinkSkypeButton.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.SKYPE));
					rowLinkSkypeLink.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.SKYPE));

					rowLinkWa.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.WA));
					rowLinkWaButton.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.WA));
					waLink.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.WA));
					rowLinkWaKeterangan.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.WA));

					rowLinkLain.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.LAIN));
					linkLain.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.LAIN));
					rowLinkLainKeterangan.setVisible(
							mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
									&& tbmuser.getSiswa() == null && ol.equals(Pertemuan.LAIN));

					testButton.setVisible(mahasiswa == null && biodataCalonMahasiswa == null
							&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && true);
					if (ol.equals(Pertemuan.GOOGLE_MEET)) {
						testButton.setImage("/img/meet-google.png");
					} else if (ol.equals(Pertemuan.JITSI)) {
						testButton.setImage("/img/jitsi.png");
					} else if (ol.equals(Pertemuan.ZOOM)) {
						testButton.setImage("/img/zoom.png");
					} else if (ol.equals(Pertemuan.BBB)) {
						testButton.setImage("/img/bbb.png");
					} else if (ol.equals(Pertemuan.SKYPE)) {
						testButton.setImage("/img/Skype-icon.png");
					} else if (ol.equals(Pertemuan.WA)) {
						testButton.setImage("/img/svg/whats.svg");
					} else if (ol.equals(Pertemuan.LAIN)) {
						testButton.setImage("/img/online-red-icon.png");
					} else {
						testButton.setVisible(false);
					}

				}
			};

			onlineMenggunakan.addEventListener("onChange", eventListenerOl);
			eventListenerOl.onEvent(null);
		}

		row = new MyFormRow();
		row.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(perkulaiahnOnlineHarusSesuaiJadwal = new MyCheckboxConfig(
				"Pertemuan dan absensi harus sesuai dengan jadwal yang telah ditentukan"));
		perkulaiahnOnlineHarusSesuaiJadwal.setChecked(pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal());
		perkulaiahnOnlineHarusSesuaiJadwal.addEventListener("onClick", updateLocal);

		if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getWaktuPerkuliahanOnlineBebas()) {
			perkulaiahnOnlineHarusSesuaiJadwal.setDisabled(true);
		}

		if (tbmuser != null && tbmuser.getDosen() != null
				&& Common.bolehKonfigurasi("absen_tanpa_batas_waktu", Konfigurasi.TIDAK_AKTIF)) {
			row.setVisible(false);
		}

		dosenTamu = new Textbox(pertemuan.getDosenTamu() == null ? "" : pertemuan.getDosenTamu());
		dosenTamu2 = new Textbox(pertemuan.getDosenTamu2() == null ? "" : pertemuan.getDosenTamu2());
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Tamu"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			hbox.appendChild(new Label(pertemuan.getDosenTamu() == null ? "" : pertemuan.getDosenTamu()));
		} else {
			hbox.appendChild(dosenTamu);
		}
		dosenTamu.setWidth("90%");
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			hbox.appendChild(new Label(pertemuan.getDosenTamu2() == null ? "" : pertemuan.getDosenTamu2()));
		} else {
			hbox.appendChild(dosenTamu2);
		}
		dosenTamu2.setWidth("90%");
		dosenTamu.addEventListener("onChange", updateLocal);
		dosenTamu2.addEventListener("onChange", updateLocal);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		tanggal = new MyDatebox(pertemuan.getTanggal());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Rencana (*)"));
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(
					pertemuan.getTanggal() == null ? "" : Common.timeFormat.get().format(pertemuan.getTanggal())));
		} else {
			row.appendChild(tanggal);
		}

		tanggal.addEventListener("onChange", updateLocal);

		tanggalRealisasi = new MyDatebox(pertemuan.getTanggalRealisasi());
		waktuMulai = new ais.ui.util.MyTimebox();
		waktuSelesai = new ais.ui.util.MyTimebox();

		waktuMulai.setCols(2);
		waktuSelesai.setCols(2);

		waktuMulai.addEventListener("onChange", updateLocal);
		waktuSelesai.addEventListener("onChange", updateLocal);

		try {
			waktuMulai.setValue(pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
					: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:1186");

		}
		try {
			waktuSelesai
					.setValue(pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:1193");

		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Rencana"));
		hbox = new Hbox();
		row.appendChild(hbox);
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			hbox.appendChild(new Label(
					waktuMulai.getValue() == null ? "" : Common.timeFormat.get().format(waktuMulai.getValue())));
		} else {
			hbox.appendChild(waktuMulai);
		}
		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());

		hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			hbox.appendChild(new Label(
					waktuSelesai.getValue() == null ? "" : Common.timeFormat.get().format(waktuSelesai.getValue())));
		} else {
			hbox.appendChild(waktuSelesai);
		}
		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dosenBolehAbsenMenggunakanFoto = new MyCheckboxConfig("Dosen Diizinkan / Boleh Absen Online"));
		dosenBolehAbsenMenggunakanFoto.setChecked(pertemuan.getDosenBolehAbsenMenggunakanFoto());

		if (perkuliahan != null && !perkuliahan.getDosenBolehAbsenMenggunakanFoto()) {
			dosenBolehAbsenMenggunakanFoto.setDisabled(true);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				mahasiswaBolehAbsenMenggunakanFoto = new MyCheckboxConfig("Mahasiswa Diizinkan / Boleh Absen Online"));
		mahasiswaBolehAbsenMenggunakanFoto.setChecked(pertemuan.getMahasiswaBolehAbsenMenggunakanFoto());

		if (perkuliahan != null && !perkuliahan.getMahasiswaBolehAbsenMenggunakanFoto()) {
			mahasiswaBolehAbsenMenggunakanFoto.setDisabled(true);
		}

		dosenBolehAbsenMenggunakanFoto.addEventListener("onClick", updateLocal);
		mahasiswaBolehAbsenMenggunakanFoto.addEventListener("onClick", updateLocal);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Menit toleransi absensi online sebelum"));
		bolehAbsenSebelumWaktuMulaiDalamMenit = new MyIntbox(pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit());
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(
					Common.numberFormat.get().format(pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit()) + " menit"));
		} else {
			row.appendChild(bolehAbsenSebelumWaktuMulaiDalamMenit);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Menit toleransi absensi online setelah"));
		bolehAbsenSetelahWaktuMulaiDalamMenit = new MyIntbox(pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit());
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(
					Common.numberFormat.get().format(pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit()) + " menit"));
		} else {
			row.appendChild(bolehAbsenSetelahWaktuMulaiDalamMenit);
		}

		if (perkuliahan != null && perkuliahan.getBolehAbsenWaktuIkutiPerkuliahan()) {
			bolehAbsenSebelumWaktuMulaiDalamMenit.setDisabled(true);
			bolehAbsenSetelahWaktuMulaiDalamMenit.setDisabled(true);
		}

		bolehAbsenSebelumWaktuMulaiDalamMenit.addEventListener("onChange", updateLocal);
		bolehAbsenSetelahWaktuMulaiDalamMenit.addEventListener("onChange", updateLocal);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Pertemuan"));
		row.appendChild(lokasi = new Combobox());
		lokasi.setWidth("90%");
		Common.insertComboDanSemua(lokasi, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Semua Lokasi", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi, pertemuan.getLokasi());
		lokasi.addEventListener("onChange", updateLocal);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Radius posisi kehadiran titik dari lokasi (km)"));
		row.appendChild(jarak = new MyDoublebox(pertemuan.getJarak()));
		jarak.addEventListener("onChange", updateLocal);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Kehadiran Dosen"));
		Vbox vbox = new Vbox();
		vbox.setParent(row);

		Component dosenUtama = null;

		if (perkuliahan != null) {
			listDosen = perkuliahan.populateDosenBuNama();
		} else if (pertemuan.getKelompokKkn() != null) {
			listDosen = pertemuan.getKelompokKkn().populateDosenBuNama();
		} else if (pertemuan.getKelompokPkl() != null) {
			listDosen = pertemuan.getKelompokPkl().populateDosenBuNama();
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			listDosen = pertemuan.getMahasiswaRequestTugasAkhir().populateDosenBuNama();
		} else if (pertemuan.getSkripsi() != null) {
			listDosen = pertemuan.getSkripsi().populateDosenBuNama();
		} else if (pertemuan.getKrsMahasiswa() != null) {
			listDosen = pertemuan.getKrsMahasiswa().populateDosenBuNama();
		} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null) {
			listDosen = pertemuan.getPertemuanPunyaGrupPertemuan().populateDosenBuNama();
		} else if (pertemuan.getFormulirKegiatan() != null) {
			listDosen = pertemuan.getFormulirKegiatan().populateDosenBuNama();
		}

		if (listDosen != null) {
			for (Dosen dosen : listDosen) {
				vbox.appendChild(CommonMedia.tampilkanGambarKecil(dosen));
				vbox.appendChild(new Label(dosen.getNama()));
				vbox.appendChild(dosenUtama = AbsensiHelper.createStatusKehadiran(dosen, pertemuan, mahasiswa,
						biodataCalonMahasiswa, tanggalRealisasi, sesuaikan, terlewat));
			}
		}

		if (perkuliahan != null) {
			row = new MyFormRow();
			row.setValign("top");

			row.setParent(rows);
			row.setValign("top");
			row.appendChild(new ais.ui.util.MyLabelConfig("Kehadiran Asisten"));
			vbox = new Vbox();
			vbox.setParent(row);

			List<Mahasiswa> asistens = perkuliahan.ambilAsisten();
			row.setVisible(!asistens.isEmpty());
			for (Mahasiswa asisten : asistens) {
				vbox.appendChild(CommonMedia.tampilkanGambarKecil(asisten));
				vbox.appendChild(new Label(asisten.getNama()));
				vbox.appendChild(AbsensiHelper.createStatusKehadiran(asisten, pertemuan, mahasiswa,
						biodataCalonMahasiswa, sesuaikan, terlewat));
			}
		}

		Dosen dsnPengganti = (Dosen) (pertemuan.getDosenPengganti() == null ? null
				: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getDosenPengganti()));

		row = new MyFormRow();
		row.setValign("top");
		row.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig dosenPenggantiAda;
		row.appendChild(dosenPenggantiAda = new ais.ui.util.MyCheckboxConfig("Ada dosen pengganti"));
		dosenPenggantiAda.setChecked(dsnPengganti != null);

		final MyFormRow rowDosenPengganti = new MyFormRow();
		rowDosenPengganti.setVisible(dosenPenggantiAda.isChecked());
		rowDosenPengganti.setStyle("border:0px;background: transparent;");
		rowDosenPengganti.setParent(rows);
		rowDosenPengganti.setValign("top");
		rowDosenPengganti.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pengganti"));

		final Component utmDosen = dosenUtama;
		final AmbilDataDosenBanbox dosenPengganti = new AmbilDataDosenBanbox(true);
		dosenPenggantiAda.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowDosenPengganti.setVisible(dosenPenggantiAda.isChecked());
				if (!dosenPenggantiAda.isChecked()) {
					if (pertemuan.getId() != null) {
						HibernateUtil.currentSession().refresh(pertemuan);
					}
					pertemuan.setDosenPengganti(null);
					sesuaikan(pertemuan, false);
					Common.refreshUpdate(pertemuan);
					dosenPengganti.setValue("");
					dosenPengganti.setAttribute("dosen", null);
				}

				if (utmDosen != null) {
					utmDosen.setVisible(!dosenPenggantiAda.isChecked());
				}
			}
		});

		vbox = new Vbox();
		vbox.setParent(rowDosenPengganti);

		final Hbox dosenPenggantiHb = new Hbox();
		vbox.appendChild(dosenPenggantiHb);

		final Hbox dosenPenggantiHbWkt = new Hbox();

		if (dsnPengganti != null) {
			Common.clear(dosenPenggantiHb);
			Common.clear(dosenPenggantiHbWkt);
			dosenPenggantiHb.appendChild(CommonMedia.tampilkanGambarKecil(dsnPengganti));
			dosenPenggantiHbWkt.appendChild(AbsensiHelper.createStatusKehadiran(dsnPengganti, pertemuan, mahasiswa,
					biodataCalonMahasiswa, tanggalRealisasi, sesuaikan, terlewat));

		}

		if (dosenUtama != null) {
			dosenUtama.setVisible(!dosenPenggantiAda.isChecked());
		}

		if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null) {

			vbox.appendChild(dosenPengganti);
			dosenPengganti.setAttribute("dosen", dsnPengganti);
			dosenPengganti.setValue(dsnPengganti == null ? "" : dsnPengganti.getNama());
			dosenPengganti.setReadonly(true);
			dosenPengganti.setWidth("90%");

			dosenPengganti.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (pertemuan.getId() != null) {
						HibernateUtil.currentSession().refresh(pertemuan);
					}
					Dosen d = (Dosen) dosenPengganti.getAttribute("dosen");
					pertemuan.setDosenPengganti(d == null ? null : d.getId());
					sesuaikan(pertemuan, false);
					Common.refreshUpdate(pertemuan);

					if (d != null) {
						Common.clear(dosenPenggantiHb);
						Common.clear(dosenPenggantiHbWkt);
						dosenPenggantiHb.appendChild(CommonMedia.tampilkanGambarKecil(d));
						dosenPenggantiHbWkt.appendChild(AbsensiHelper.createStatusKehadiran(d, pertemuan, mahasiswa,
								biodataCalonMahasiswa, tanggalRealisasi, sesuaikan, terlewat));
					}
				}
			});
		} else {
			Statusabsensi statusabsensi = dsnPengganti == null ? null
					: (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(dsnPengganti.getId()));
			new Label(dsnPengganti == null ? "" : dsnPengganti.getNama()).setParent(vbox);

			new Label(statusabsensi == null ? "" : statusabsensi.getNama()).setParent(dosenPenggantiHbWkt);
			String wkt = dsnPengganti == null ? ""
					: pertemuan.retreiveAbsensiMulai(dsnPengganti.getId()) + " s.d "
							+ pertemuan.retreiveAbsensiSampai(dsnPengganti.getId());
			new Label(wkt.trim().equals("s.d") ? "" : wkt).setParent(dosenPenggantiHbWkt);

			waktuMulai.setDisabled(true);
			waktuSelesai.setDisabled(true);
		}

		vbox.appendChild(dosenPenggantiHbWkt);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Realisasi"));
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(tanggalRealisasi.getValue() == null ? ""
					: Common.dateFormat6.get().format(tanggalRealisasi.getValue())));
		} else {
			row.appendChild(tanggalRealisasi);
			Common.initKeterangan(rows,
					"(*) \"Tanggal Realisasi\" adalah tanggal terjadi-nya proses belajar mengajar, tanggal realisasi bisa diisi jika dosen telah melakukan absensi atau terdapat dosen pengganti");
		}
		tanggalRealisasi.addEventListener("onChange", updateLocal);

		row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		ruang = new AmbilDataRuangBanbox();
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getNama()));
		} else {
			row.appendChild(ruang);
		}
		ruang.setReadonly(true);
		ruang.setValue(pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getNama());
		ruang.setAttribute("ruang", pertemuan.getRuang());
		ruang.setWidth("90%");
		ruang.setEventListener(updateLocal);

		row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode"));
		metode = new Textbox(pertemuan.getMetodePembelajaran() == null ? "" : pertemuan.getMetodePembelajaran());
		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			row.appendChild(new Label(pertemuan.getMetodePembelajaran()));
		} else {
			row.appendChild(metode);
		}
		metode.setWidth("90%");
		metode.setRows(2);
		metode.addEventListener("onChange", updateLocal);

		if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
			Common.freeze(grid, true);
		}

		return grid;
	}

	@SuppressWarnings("unchecked")
	public void mainInit(final Pertemuan pertemuan, Component tabpanelUtama, boolean tampilInfo) throws Exception {

		this.tabpanelUtama = tabpanelUtama;
		this.tampilInfo = tampilInfo;
		mahasiswas = AbsensiHelper.populateMahasiswaDariPertemuan(pertemuan);
		if (pertemuan != null) {
			pertemuan.masukkanData("melihat_absensi");
		}
		perkuliahan = pertemuan.getPerkuliahan();

		Konfigurasi konfigurasi = Common.getKonfigurasi("default_status_kehadiran",
				ConstantValues.BELUM_ABSEN == null ? "-" : ConstantValues.BELUM_ABSEN.getKode());

		Statusabsensi statusabsensi = null;
		Map<Serializable, Statusabsensi> p = ConstantValues.ambilBerdasarClass(Statusabsensi.class);
		for (Statusabsensi pp : p.values()) {
			if (pp != null && pp.getKode().toLowerCase().contains(konfigurasi.getNilai().toLowerCase())) {
				statusabsensi = pp;
				break;
			}
		}

		status = statusabsensi != null ? statusabsensi : ConstantValues.BELUM_ABSEN;
		tbmuser = Common.getCurrentUser();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanelUtama);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		mobile = Common.isMobile();

		if (mobile) {

			MyGrid grid = new MyGrid();
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.setValign("top");
			row.appendChild(bagianInfo(pertemuan));

			row = new MyFormRow();
			row.setParent(rows);
			row.setValign("top");
			createListMahasiswaAbsensi(row, pertemuan);
//			d.setStyle("min-height: 400px;height:" + (150 + (75 * mahasiswas.size())) + "px");

			if (pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null) {
				row = new MyFormRow();
				row.setParent(rows);
				row.setValign("top");
				createListMahasiswaIzin(row, pertemuan);
			}
		} else {
			center.setTitle("Presensi kehadiran");

			West west = new West();
			west.setTitle("Informasi");
			ais.ui.util.ZkCompat.setFlex(west, true);
			west.setWidth("28%");
			west.setParent(borderlayout);
			west.appendChild(bagianInfo(pertemuan));

			createListMahasiswaAbsensi(center, pertemuan);

			if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
				Common.freeze(borderlayout, true);
			}

			if (pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null) {
				final East east = new East();
				east.setTitle("Pengajuan Izin atau Sakit");
				ais.ui.util.ZkCompat.setFlex(east, true);
				east.setWidth("25%");
				east.setParent(borderlayout);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						createListMahasiswaIzin(east, pertemuan);
					}
				});
			}

		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& perkuliahan != null && !perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()) {
			tanggal.setDisabled(true);
			waktuMulai.setDisabled(true);
			waktuSelesai.setDisabled(true);
			ruang.setDisabled(true);
		}
	}

	@SuppressWarnings("unchecked")
	private void initKelasPertemuan(final KelasPertemuan kelasPertemuan, final Pertemuan pertemuan) throws Exception {
		final MyWindow myWindow = new MyWindow("Pendataan Kelas/Ruangan", "none", true);
		myWindow.setHeight("95%");
		myWindow.setWidth("950px");
		myWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(myWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		MyGrid gridMahasiswa = new MyGrid();
		gridMahasiswa.setWidth("100%");
		gridMahasiswa.setParent(east);
		gridMahasiswa.setWidth("100%");
		gridMahasiswa.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(gridMahasiswa);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig("Foto");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("NIM");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Nama");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(gridMahasiswa);

		Session session = HibernateUtil.currentSession();

		final List<Mahasiswa> selectedMahaiswas = kelasPertemuan == null || kelasPertemuan.getId() == null
				? new ArrayList<Mahasiswa>()
				: session.createCriteria(DetailKelasPertemuan.class)
						.setProjection(Projections.groupProperty("mahasiswa"))
						.add(Restrictions.eq("kelasPertemuan", kelasPertemuan)).list();

		@SuppressWarnings("rawtypes")
		List<Long> indsYgSudah = kelasPertemuan != null && kelasPertemuan.getId() != null
				? (List) new ArrayList<Mahasiswa>()
				: session.createCriteria(DetailKelasPertemuan.class)
						.setProjection(Projections.groupProperty("mahasiswa.id"))
						.createAlias("kelasPertemuan", "kelasPertemuan")
						.add(Restrictions.eq("kelasPertemuan.pertemuan", pertemuan)).list();

		for (GeneralValueObject mhs : AbsensiHelper.populateMahasiswaDariPertemuan(pertemuan)) {
			if (mhs instanceof Mahasiswa) {
				final Mahasiswa mahasiswa = (Mahasiswa) mhs;
				if (!indsYgSudah.contains(mahasiswa.getId())) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setValign("top");
					row.setParent(rows);
					final Checkbox pilh = new Checkbox();
					row.appendChild(pilh);
					pilh.setChecked(selectedMahaiswas.contains(mahasiswa));
					pilh.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (pilh.isChecked()) {
								selectedMahaiswas.add(mahasiswa);
							} else {
								selectedMahaiswas.remove(mahasiswa);
							}
						}
					});

					CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(row);
					row.appendChild(new Label(mahasiswa.getNim()));
					row.appendChild(new Label(mahasiswa.getNama()));
				}
			}
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		final MyDatebox mulai;
		final MyDatebox selesai;
		final Timebox waktuMulai;
		final Timebox waktuSelesai;
		final AmbilDataRuangBanbox ruang;

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("70%");

		rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas *"));
		final Textbox nama;
		row.appendChild(nama = new Textbox(kelasPertemuan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(mulai = new MyDatebox(kelasPertemuan.getMulai()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(selesai = new MyDatebox(kelasPertemuan.getSelesai()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu *"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(waktuMulai = new ais.ui.util.MyTimebox());
		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			waktuMulai.setValue(
					kelasPertemuan.getWaktuMulai() == null || kelasPertemuan.getWaktuMulai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(kelasPertemuan.getWaktuMulai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:1787");

		}

		hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
		hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			waktuSelesai.setValue(
					kelasPertemuan.getWaktuSelesai() == null || kelasPertemuan.getWaktuSelesai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(kelasPertemuan.getWaktuSelesai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:1798");

		}

		waktuMulai.setCols(2);
		waktuSelesai.setCols(2);

		row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		row.appendChild(ruang = new AmbilDataRuangBanbox());
		ruang.setReadonly(true);
		ruang.setValue(kelasPertemuan.getRuang() == null ? "" : kelasPertemuan.getRuang().getNama());
		ruang.setAttribute("ruang", kelasPertemuan.getRuang());
		ruang.setWidth("90%");

		Pegawai petugas = (Pegawai) (kelasPertemuan.getPetugas() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas()));

		Pegawai petugas2 = (Pegawai) (kelasPertemuan.getPetugas2() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas2()));

		Pegawai petugas3 = (Pegawai) (kelasPertemuan.getPetugas3() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas3()));

		Dosen pjawabDosen = (Dosen) (kelasPertemuan.getPjDosen() == null ? null
				: ConstantValues.ambil(Dosen.class.getName(), kelasPertemuan.getPjDosen()));

		row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengawas " + pertemuan.getStatusPertemuan().getNama()));
		hbox = new Hbox();
		hbox.setWidth("90%");
		row.appendChild(hbox);
		final AmbilDataPegawaiBanbox pegawai;
		hbox.appendChild(pegawai = new AmbilDataPegawaiBanbox(false));
		pegawai.setWidth("150px");
		pegawai.setAttribute("pegawai", petugas);
		pegawai.setValue(petugas == null ? null : petugas.getNama());
		pegawai.setReadonly(true);

		final AmbilDataPegawaiBanbox pegawai2;
		hbox.appendChild(pegawai2 = new AmbilDataPegawaiBanbox(false));
		pegawai2.setWidth("150px");
		pegawai2.setAttribute("pegawai", petugas2);
		pegawai2.setValue(petugas2 == null ? null : petugas2.getNama());
		pegawai2.setReadonly(true);

		final AmbilDataPegawaiBanbox pegawai3;
		hbox.appendChild(pegawai3 = new AmbilDataPegawaiBanbox(false));
		pegawai3.setWidth("150px");
		pegawai3.setAttribute("pegawai", petugas3);
		pegawai3.setValue(petugas3 == null ? null : petugas3.getNama());
		pegawai3.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penangungjawab Dosen"));
		final AmbilDataDosenBanbox pjDosen;
		row.appendChild(pjDosen = new AmbilDataDosenBanbox(false));
		pjDosen.setWidth("90%");
		pjDosen.setAttribute("dosen", pjawabDosen);
		pjDosen.setValue(pjawabDosen == null ? null : pjawabDosen.getNama());
		pjDosen.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final Textbox keterangan;
		row.appendChild(keterangan = new Textbox(kelasPertemuan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				myWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (nama.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, nama kelas belum diisi. Langkah yang dapat dilakukan: (1) isi nama kelas pada kolom yang tersedia; (2) pastikan nama kelas tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									nama.focus();
								}
							});
					return;
				}
				if (mulai.getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, tanggal mulai belum diisi. Langkah yang dapat dilakukan: (1) pilih tanggal mulai dari kalender; (2) pastikan tanggal mulai tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									mulai.focus();
								}
							});
					return;
				}
				// if (selesai.getValue() == null) {
				// MyMessageboxConfig.show("Tanggal selesai untuk harus diisi",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.INFORMATION, new EventListener() {
				//
				// @Override
				// public void onEvent(Event arg0) throws Exception {
				// selesai.focus();
				// }
				// });
				// return;
				// }
				if (waktuMulai.getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, waktu mulai belum diisi. Langkah yang dapat dilakukan: (1) isi waktu mulai pada kolom yang tersedia; (2) pastikan format waktu sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									waktuMulai.focus();
								}
							});
					return;
				}
				if (waktuSelesai.getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, waktu selesai belum diisi. Langkah yang dapat dilakukan: (1) isi waktu selesai pada kolom yang tersedia; (2) pastikan format waktu sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									waktuMulai.focus();
								}
							});
					return;
				}

				Pegawai petugas = (Pegawai) pegawai.getAttribute("pegawai");
				Pegawai petugas2 = (Pegawai) pegawai2.getAttribute("pegawai");
				Pegawai petugas3 = (Pegawai) pegawai3.getAttribute("pegawai");
				Dosen pjawabDosen = (Dosen) pjDosen.getAttribute("dosen");

				kelasPertemuan.setPetugas(petugas == null ? null : petugas.getId());
				kelasPertemuan.setPetugas2(petugas2 == null ? null : petugas2.getId());
				kelasPertemuan.setPetugas3(petugas3 == null ? null : petugas3.getId());

				kelasPertemuan.setPjDosen(pjawabDosen == null ? null : pjawabDosen.getId());

				kelasPertemuan.setPertemuan(pertemuan);

				kelasPertemuan.setNama(nama.getValue().trim());
				kelasPertemuan.setWaktuMulai(
						waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
				kelasPertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
						: Common.timeFormat2.get().format(waktuSelesai.getValue()));

				kelasPertemuan.setRuang((Ruang) ruang.getAttribute("ruang"));

				Calendar m1 = ais.ui.util.WaktuUtil.getCalendar();
				if (waktuMulai.getValue() != null) {
					m1.setTime(waktuMulai.getValue());
				}

				if (mulai.getValue() != null) {
					Calendar m = ais.ui.util.WaktuUtil.getCalendar();
					m.setTime(mulai.getValue());
					m.set(Calendar.HOUR_OF_DAY, m1.get(Calendar.HOUR_OF_DAY));
					m.set(Calendar.MINUTE, m1.get(Calendar.MINUTE));
					kelasPertemuan.setMulai(m.getTime());
				}

				Calendar s1 = ais.ui.util.WaktuUtil.getCalendar();
				if (waktuSelesai.getValue() != null) {
					s1.setTime(waktuSelesai.getValue());
				}

				if (selesai.getValue() != null) {
					Calendar s = ais.ui.util.WaktuUtil.getCalendar();
					s.setTime(selesai.getValue());
					s.set(Calendar.HOUR_OF_DAY, s1.get(Calendar.HOUR_OF_DAY));
					s.set(Calendar.MINUTE, s1.get(Calendar.MINUTE));
					kelasPertemuan.setSelesai(s.getTime());
				}

				Common.refreshSaveOrUpdate(kelasPertemuan);

				Session session = HibernateUtil.currentSession();
				session.createSQLQuery(
						"delete from detail_kelas_pertemuan where kelas_pertemuan=" + kelasPertemuan.getId())
						.executeUpdate();
				for (Mahasiswa mahasiswa : selectedMahaiswas) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
							.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1).uniqueResult();
					DetailKelasPertemuan detailKelasPertemuan = new DetailKelasPertemuan();
					detailKelasPertemuan.setMahasiswa(mahasiswa);
					detailKelasPertemuan.setKelasPertemuan(kelasPertemuan);
					detailKelasPertemuan.setDetailperkuliahan(detailperkuliahan);
					Common.refreshSaveOrUpdate(session, detailKelasPertemuan);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswas = AbsensiHelper.populateMahasiswaDariPertemuan(pertemuan);
						reload(pertemuan);
					}
				});
				myWindow.detach();
			}
		});
		save.setParent(toolbar);

		myWindow.onModal();
	}

	private void reload(final Pertemuan pertemuan) {
		// Absensi berubah → segarkan cache tren agar grafik yang dirender ulang memakai data baru.
		try {
			if (perkuliahan != null) {
				AbsensiTrenCache.invalidasi(perkuliahan.getId());
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:2045");
		}
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tabpanelUtama);
				mainInit(pertemuan, tabpanelUtama, tampilInfo);
			}
		});
	}

	/** Gaya kartu presensi modern &mdash; delegasi ke {@link AbsensiUiHelper#gayaKartuPresensi()}. */
	private static String gayaKartuPresensi() {
		return AbsensiUiHelper.gayaKartuPresensi();
	}

	/** Lencana status berwarna &mdash; delegasi ke {@link AbsensiUiHelper#badgeStatus(Statusabsensi)}. */
	private static String badgeStatus(Statusabsensi s) {
		return AbsensiUiHelper.badgeStatus(s);
	}

	/**
	 * Menyajikan grafik garis TREN KEHADIRAN antar-pertemuan untuk sebuah perkuliahan, dengan
	 * sumber data ber-cache bertingkat ({@link AbsensiTrenCache}) sehingga sangat cepat dibuka
	 * berulang kali. Tiap titik adalah persentase kehadiran peserta didik pada satu pertemuan,
	 * berurutan dari pertemuan awal hingga akhir; grafik baru ditampilkan bila minimal ada dua
	 * pertemuan yang sudah diabsen. Memakai ulang
	 * {@link DashboardAkademikHtmlCssHelper#trendLineChart} agar tampilan konsisten dan ringan.
	 *
	 * @param pk perkuliahan/kelas yang trennya ditampilkan.
	 * @return potongan HTML grafik; string kosong bila data belum cukup.
	 */
	private String htmlTrenKehadiran(Perkuliahan pk) {
		return AbsensiUiHelper.htmlTrenKehadiran(pk == null ? null : pk.getId());
	}

	/** Donut komposisi kehadiran &mdash; delegasi ke {@link AbsensiUiHelper#htmlKomposisiKehadiran}. */
	private String htmlKomposisiKehadiran(Pertemuan pertemuan, List<? extends GeneralValueObject> peserta) {
		return AbsensiUiHelper.htmlKomposisiKehadiran(pertemuan, peserta);
	}

	/**
	 * Membangun DAFTAR PRESENSI KEHADIRAN per peserta (mahasiswa/asisten) untuk satu
	 * {@link Pertemuan}, yang ditampilkan pada kolom tengah modul Kehadiran &amp; Absensi.
	 *
	 * <p>
	 * <b>Tujuan tampilan.</b> Untuk setiap peserta dirender satu "kartu" yang memuat: nomor urut,
	 * foto (bila diaktifkan lewat konfigurasi {@code tampilkan_foto_di_absensi_kehadiran}),
	 * identitas (NIM / nama / status / program), kontrol/indikator status kehadiran
	 * (Hadir/Izin/Sakit/Alpa beserta jam mulai&ndash;selesai), serta keterangan dan tombol
	 * <i>Ubah Keterangan</i>. Toolbar di atas daftar menyediakan aksi massal seperti
	 * <i>Semua hadir</i>. Komponen dibangun secara terprogram (bukan ZUL) karena perilakunya
	 * sangat dinamis terhadap peran pengguna dan status periode pengisian.
	 * </p>
	 *
	 * <p>
	 * <b>Aturan akses &amp; status.</b> Kontrol input (radiogroup status + timebox jam) hanya
	 * dirender bila pengguna BUKAN mahasiswa/calon/siswa (atau seorang asisten absen yang
	 * berwenang) dan periode pengisian masih dalam batas toleransi jadwal
	 * ({@code !perkulaiahnOnlineHarusSesuaiJadwal || !terlewat}); jika tidak, status ditampilkan
	 * sebagai teks read-only. Bila peserta memiliki pengajuan izin/sakit yang disetujui, status
	 * disinkronkan otomatis ke status pada pengajuan tersebut sebelum dirender, sehingga tampilan
	 * konsisten dengan keputusan izin. Setiap perubahan status/jam/keterangan dipersist melalui
	 * {@link Pertemuan#populate} lalu {@code sesuaikan(...)} dan {@code Common.refreshUpdate(...)}.
	 * </p>
	 *
	 * <p>
	 * <b>Kerapian tata letak (UI/UX).</b> Agar daftar enak dibaca dan SEJAJAR antar kartu di
	 * layar mobile maupun desktop, kontainer kartu disetel rata KIRI dan rata ATAS secara
	 * eksplisit: baris kartu ({@code hbox}) memakai {@code align=top} dengan
	 * {@code align-items:flex-start}; kolom konten ({@code vboxStats}) memakai {@code align=start},
	 * {@code hflex=1}, dan {@code text-align:left}; blok status ({@code hboxStatus}) serta blok
	 * keterangan ({@code hbox1}) juga rata kiri. Pendekatan ini menghindari kesan "ter-tengah dan
	 * berantakan" tanpa mengubah logika absensi apa pun.
	 * </p>
	 *
	 * @param parentrow komponen induk (umumnya region tengah border layout) tempat daftar dipasang.
	 * @param pertemuan pertemuan yang presensinya ditampilkan; tidak boleh {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private void createListMahasiswaAbsensi(Component parentrow, final Pertemuan pertemuan) {

		Row utamaBanget = Common.tampilanScroll1(parentrow);

		/*
		 * Wadah VERTIKAL full-width. Sebelumnya gaya, donut, tren, dan toolbar ditempel
		 * langsung sebagai anak ZK Row sehingga dirender sebagai sel-sel <td> BERDAMPINGAN
		 * (horizontal) → tampilan berdesakan/berantakan. Dengan menumpuknya dalam satu
		 * Vlayout full-width, semuanya melebar penuh & rapi; donut+tren tetap responsif
		 * (berdampingan di desktop, menumpuk di mobile) lewat htmlRingkasanGabung.
		 */
		org.zkoss.zul.Vlayout kolomAtas = AbsensiUiHelper.wadahRingkasanAtas(utamaBanget);

		// Sekali tempel: gaya kartu presensi modern (bayangan halus, sudut membulat, hover,
		// badge status berwarna) + responsif. Reusable lewat helper agar konsisten antar tampilan.
		kolomAtas.appendChild(new MyHtml(gayaKartuPresensi()));

		// Ringkasan komposisi kehadiran (donut) + tren antar-pertemuan dalam satu wadah responsif.
		try {
			String komposisi = htmlKomposisiKehadiran(pertemuan, mahasiswas);
			String tren = htmlTrenKehadiran(perkuliahan);
			String ringkas = AbsensiUiHelper.htmlRingkasanGabung(komposisi, tren);
			if (ringkas != null && ringkas.length() > 0) {
				kolomAtas.appendChild(new MyHtml(ringkas));
			}
		} catch (Throwable abaikanRingkasan) { ais.common.ErrorAuditUtil.record(abaikanRingkasan, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:2151");
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		mahasiswaBolehUbahAbsen = false;
		if (tbmuser != null && tbmuser.getMahasiswa() != null && perkuliahan != null) {
			mahasiswaBolehUbahAbsen = perkuliahan.merupakanAsistenAbsen(tbmuser.getMahasiswa());
		}

		Toolbar toolbar = new Toolbar();
		toolbar.setSclass("ais-absn-toolbar");
		toolbar.setParent(kolomAtas);

		MyFormRow utamalagi = new MyFormRow();
		utamalagi.setParent(utamaBanget.getParent());

		Row rowUtama = Common.tampilanScroll1(utamalagi);
		Rows rowsUtama = (Rows) rowUtama.getParent();

		if ((tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
				&& (!pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() || !terlewat)) || mahasiswaBolehUbahAbsen) {

			MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Semua hadir", "/img/svg/check2.svg");
			masuk.setParent(toolbar);
			masuk.setTooltiptext("Tutup");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin semua mahasiswa masuk kelas ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												if (pertemuan.getId() != null) {
													try {
														HibernateUtil.currentSession().refresh(pertemuan);
													} catch (org.hibernate.UnresolvableObjectException uoe) { ais.common.ErrorAuditUtil.record(uoe, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:2198");
														// Baris Pertemuan sudah dihapus sesi lain. Lanjut pakai data di
														// memori; Common.refreshUpdate menangani baris hilang/stale.
													}
												}

												for (GeneralValueObject generalValueObject : mahasiswas) {
													Statusabsensi statusabsensi = ConstantValues.MASUK;

													pertemuan.populate(generalValueObject.getId(), statusabsensi,
															pertemuan.getWaktuMulai(), pertemuan.getWaktuSelesai(),
															"Mahasiswa");

												}
												sesuaikan(pertemuan, false);
												Common.refreshUpdate(pertemuan);

												reload(pertemuan);
											}
										});

									}

								}
							});

				}
			});

			masuk = new MyToolbarbuttonConfig("Reset", "/img/reply.png");
			masuk.setParent(toolbar);
			masuk.setTooltiptext("Reset");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin me-reset absen di kelas ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (pertemuan.getId() != null) {
													try {
														HibernateUtil.currentSession().refresh(pertemuan);
													} catch (org.hibernate.UnresolvableObjectException uoe) { ais.common.ErrorAuditUtil.record(uoe, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:2248");
														// Baris Pertemuan sudah dihapus sesi lain. Lanjut pakai data di
														// memori; Common.refreshUpdate menangani baris hilang/stale.
													}
												}

												for (GeneralValueObject generalValueObject : mahasiswas) {
													Statusabsensi statusabsensi = ConstantValues.BELUM_ABSEN;
													pertemuan.populate(generalValueObject.getId(), statusabsensi, "",
															null, pertemuan.getWaktuMulai(),
															pertemuan.getWaktuSelesai(), "Mahasiswa");
												}
												sesuaikan(pertemuan, false);
												Common.refreshUpdate(pertemuan);

												reload(pertemuan);
											}
										});

									}

								}
							});

				}
			});

			MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download", "/img/excel.png");
			download.setParent(toolbar);
			download.setVisible(!Common.isMobile());
			download.setTooltiptext("Download");
			download.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					final String filename = Sessions.getCurrent().getWebApp()
							.getRealPath("/tmp/data_absen_" + URLEncoder.encode(
									Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
									+ ".xlsx");

					File file;
					(file = new File(filename)).createNewFile();
					final Intbox sizedata = new Intbox(30);
					final Label label = Common
							.displayLoadBar(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot(), file);

					new Thread(new Runnable() {

						@Override
						public void run() {

							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet("Absensi");
							sheet.setDefaultColumnWidth(20);
							int rowIndex = 0;

							XSSFRow rowhead = sheet.createRow((short) 0);

							rowhead.createCell(0).setCellValue("Mahasiswa");
							rowhead.createCell(1).setCellValue("Status");
							rowhead.createCell(2).setCellValue("Mulai");
							rowhead.createCell(3).setCellValue("Sampai");
							rowhead.createCell(4).setCellValue("Keterangan");

							rowIndex = 1;
							for (GeneralValueObject mahasiswa : mahasiswas) {

								label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / mahasiswas.size())
										+ " %)");

								XSSFRow row = sheet.createRow(rowIndex);
								row.createCell(0).setCellValue(mahasiswa.toString());

								Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
										Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(mahasiswa.getId()));

								row.createCell(1).setCellValue(statusabsensi == null ? "" : statusabsensi.toString());

								row.createCell(2).setCellValue(pertemuan.retreiveAbsensiMulai(mahasiswa.getId()));
								row.createCell(3).setCellValue(pertemuan.retreiveAbsensiSampai(mahasiswa.getId()));

								row.createCell(4).setCellValue(pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId()));

								rowIndex++;
							}

							Common.setStyled(sheet);
							sizedata.setValue(rowIndex + 1);

							try {
								FileOutputStream fileOut = new FileOutputStream(filename);
								workbook.write(fileOut);
								fileOut.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								Common.tampilErrorJikaAdmin(e);
							}

							System.out.println("Your excel file has been generated! ");

							label.setValue("");
						}
					}).start();

				}
			});

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
					"/img/excel.png");
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
						// System.out.println("media = " + media);
						final File file = new File(
								Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						// System.out.println("file = " +
						// file.getAbsolutePath());
						file.getParentFile().mkdirs();
						FileOutputStream fileOutputStream = new FileOutputStream(file);
						int c;
						while ((c = inputStream.read()) != -1) {
							fileOutputStream.write(c);
						}
						fileOutputStream.close();
						inputStream.close();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Absensi Pertemuan");
								final Label downloadPath = new Label("");
								final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
								Clients.showBusy(label.getValue());
								final Timer timer = new Timer(200);
								timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								timer.setRepeats(true);
								timer.addEventListener("onTimer", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Clients.showBusy(label.getValue());
										if (label.getValue().isEmpty()) {
											System.out.println("loading file " + file.getAbsolutePath());
											if (!downloadPath.getValue().isEmpty()) {
												try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
												catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan AbsensiHelper"); }
											}
											MyMessageboxConfig.show(report.getRingkasan(),
													"Pemberitahuan", MyMessageboxConfig.OK,
													MyMessageboxConfig.INFORMATION, new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															reload(pertemuan);
														}
													});

											Clients.clearBusy();
											timer.detach();
										}

									}
								});
								timer.start();

								new Thread(new Runnable() {

									@Override
									public void run() {
										try {

										try {

											XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
											XSSFSheet sheet = workbook.getSheetAt(0);

											Session session = HibernateUtil.currentNativeSession();
											int rowCount = (sheet.getLastRowNum() + 1);
											for (int i = 1; i < rowCount; i++) {
												@SuppressWarnings("rawtypes")
												Map datum = null;
												String identBaris = "Baris-" + i;
												try {
													// FIX "Session is closed!": bila satu baris gagal, blok catch memanggil
													// HibernateUtil.rollbackTransaction() yang MENUTUP + menghapus thread-local
													// session. Referensi 'session' yang diambil sekali SEBELUM loop menjadi
													// basi/tertutup sehingga baris-baris berikutnya gagal semua. Ambil ulang
													// session yang dijamin terbuka di awal tiap iterasi.
													session = HibernateUtil.currentNativeSession();

													Mahasiswa mahasiswa = (Mahasiswa) Common
															.getSheetContentAsObject(sheet, 0, i, Mahasiswa.class);

													if (mahasiswa == null) {
														report.gagal(i, identBaris, "Mahasiswa tidak ditemukan pada baris ini", "Pastikan nama/NIM pada kolom 0 valid.");
														continue;
													}
													identBaris = mahasiswa.getNim() + " - " + mahasiswa.getNama();

													String waktuMulai = Common.getSheetContentAsString(sheet, 2, i);
													String waktuSelesai = Common.getSheetContentAsString(sheet, 3, i);
													String keterangan = Common.getSheetContentAsString(sheet, 4, i);

													label.setValue(
															"Upload data \"" + mahasiswa.getNim() + " - "
																	+ mahasiswa.getNama() + "\" (" + Common.numberFormat
																			.get().format(i * 100.0 / rowCount)
																	+ " %)");

													Statusabsensi statusabsensi = (Statusabsensi) Common
															.getSheetContentAsObject(sheet, 1, i, Statusabsensi.class);
													// 'pertemuan' dimuat di thread REQUEST → DETACHED pada native session
													// (ThreadLocal) thread upload ini, sehingga session.refresh() melempar
													// "not associated with this Session" → SEMUA baris gagal & TIDAK ada
													// absen tersimpan (upload seakan tak berfungsi). Muat ULANG by-id ke
													// session thread ini, lalu populate + simpan entity yang ter-attach.
													Pertemuan pertemuanDb = (Pertemuan) session.get(Pertemuan.class,
															pertemuan.getId());
													if (pertemuanDb == null) {
														report.gagal(i, identBaris, "Pertemuan tidak ditemukan di database", "Data pertemuan tidak valid.");
														continue;
													}
													pertemuanDb.populate(mahasiswa.getId(), statusabsensi, keterangan,
															null, waktuMulai, waktuSelesai, "Mahasiswa");
													session.getTransaction().begin();
													Common.refreshUpdate(session, pertemuanDb);
													session.getTransaction().commit();
													report.sukses(i, identBaris, "Absensi berhasil dicatat");

												} catch (Exception e) {
													System.out.println("error --> datum=>" + datum);
													Common.tampilErrorJikaAdmin(e);
													report.gagal(i, identBaris, e, "Periksa data/format pada baris ini.");
													try {
														HibernateUtil.rollbackTransaction();
													} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:2481");

													}
												}
											}
											HibernateUtil.closeSession();
										} catch (Exception e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:2489");
										}

										try {
											java.io.File rptFile = report.simpanLaporan();
											downloadPath.setValue(rptFile.getAbsolutePath());
										} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) AbsensiHelper laporan"); }
										label.setValue("");
																			} finally {
											ais.database.hibernate.HibernateUtil.closeSession();
										}
									}
								}).start();

							}
						}, "Harap tunggu.. sedang melakukan proses upload data..");

					} else {
						MyMessageboxConfig.show(
								"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
										+ media,
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					}
				}
			});
			toolbar.appendChild(upload);

			upload.setVisible(
					!Common.isMobile() && Common.bolehKonfigurasi("aktifkan_upload_data_absen"));
		}

		Toolbarbutton masuk = new MyToolbarbuttonConfig("Pisahkan", "/img/absensi_pmb.png");
		masuk.setParent(toolbar);
		masuk.setVisible(perkuliahan != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
		masuk.setTooltiptext("Pisahkan Kelas/Ruangan");
		masuk.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				KelasPertemuan kelasPertemuan = new KelasPertemuan();
				kelasPertemuan.setPertemuan(pertemuan);
				initKelasPertemuan(kelasPertemuan, pertemuan);
			}
		});

		final MyCheckboxConfig tampilkanJamAbsensiBagiMahasiswa = new MyCheckboxConfig("Waktu");
		tampilkanJamAbsensiBagiMahasiswa.setChecked(pertemuan.getTampilkanJamAbsensiBagiMahasiswa());
		tampilkanJamAbsensiBagiMahasiswa.setParent(toolbar);
		tampilkanJamAbsensiBagiMahasiswa.setTooltiptext("Tampilkan waktu absensi bagi mahasiswa");
		tampilkanJamAbsensiBagiMahasiswa.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pertemuan.setTampilkanJamAbsensiBagiMahasiswa(tampilkanJamAbsensiBagiMahasiswa.isChecked());
				sesuaikan(pertemuan, false);
				Common.refreshSaveOrUpdate(pertemuan);
				reload(pertemuan);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		button.setVisible(perkuliahan != null);
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanBeritaAcara(pertemuan, null);

			}

		});
		button.setParent(toolbar);

		final AmbilDataPertemuanBerdasarKelasPertemuanBanbox ambilDataKelasPertemuanBanbox = new AmbilDataPertemuanBerdasarKelasPertemuanBanbox();
		ambilDataKelasPertemuanBanbox.setCols(10);
		ambilDataKelasPertemuanBanbox.setVisible(false);
		toolbar.appendChild(ambilDataKelasPertemuanBanbox);
		ambilDataKelasPertemuanBanbox.setReadonly(true);
		ambilDataKelasPertemuanBanbox.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pertemuan pert = (Pertemuan) ambilDataKelasPertemuanBanbox.getAttribute("pertemuan");
				if (pert != null && pert.getId() != null) {
					Session session = HibernateUtil.currentSession();

					session.createSQLQuery(
							"delete from detail_kelas_pertemuan where kelas_pertemuan in (select id from kelas_pertemuan where pertemuan="
									+ pertemuan.getId() + ")")
							.executeUpdate();
					session.createSQLQuery("delete from kelas_pertemuan where pertemuan=" + pertemuan.getId())
							.executeUpdate();

					List<KelasPertemuan> kelasPertemuans = session.createCriteria(KelasPertemuan.class)
							.add(Restrictions.eq("pertemuan", pert)).list();

					for (KelasPertemuan kelasPertemuan : kelasPertemuans) {
						List<Mahasiswa> selectedMahaiswas = session.createCriteria(DetailKelasPertemuan.class)
								.setProjection(Projections.groupProperty("mahasiswa"))
								.add(Restrictions.eq("kelasPertemuan", kelasPertemuan)).list();
						if (!selectedMahaiswas.isEmpty()) {
							KelasPertemuan kelasPertemuanLama = (KelasPertemuan) kelasPertemuan.clone();
							kelasPertemuanLama.setPertemuan(pertemuan);
							session.save(kelasPertemuanLama);

							for (Mahasiswa mahasiswa : selectedMahaiswas) {
								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
										.createCriteria(Detailperkuliahan.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1)
										.uniqueResult();
								if (detailperkuliahan != null) {
									DetailKelasPertemuan detailKelasPertemuan = new DetailKelasPertemuan();
									detailKelasPertemuan.setDetailperkuliahan(detailperkuliahan);
									detailKelasPertemuan.setKelasPertemuan(kelasPertemuanLama);
									detailKelasPertemuan.setMahasiswa(mahasiswa);
									session.save(detailKelasPertemuan);
								}
							}

						}
					}
				}

				reload(pertemuan);
			}
		});

		masuk = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		masuk.setParent(toolbar);
		masuk.setTooltiptext("Refresh");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				HibernateUtil.currentSession().refresh(pertemuan);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.belum("PengajuanIzinTidakMasukPerkuliahan");
						reload(pertemuan);
					}
				});

			}
		});

		reloadAbsensiBaru(pertemuan, rowsUtama);

		utamalagi = new MyFormRow();
		utamalagi.setParent(utamaBanget.getParent());
		rowUtamaAbsensiOnline = Common.tampilanScroll1(utamalagi);
		tampilkanAbsensiOnline(pertemuan);
	}

	@SuppressWarnings("unchecked")
	private TreeMap<String, Map<String, String>> reloadSejarahAbsensiOnline(Pertemuan pertemuan) {
		String sebelumnya = pertemuan.retreive("sejarah");
		JSONObject jsonObject = new JSONObject();
		if (sebelumnya != null && !sebelumnya.trim().isEmpty()) {
			/* PostgreSQL/berkas lama pernah menghasilkan karakter NUL di tengah JSON.
			 * org.json menolaknya sebagai unterminated string. Bersihkan karakter kontrol
			 * yang tidak sah dan, bila ada ekor data terpotong, pertahankan objek JSON
			 * lengkap terakhir yang masih dapat dibaca. */
			String bersih = sebelumnya.replace('\u0000', ' ');
			try {
				jsonObject = new JSONObject(bersih);
			} catch (Exception jsonRusak) {
				int penutup = bersih.lastIndexOf('}');
				while (penutup > 1) {
					try {
						jsonObject = new JSONObject(bersih.substring(0, penutup + 1));
						break;
					} catch (Exception belumLengkap) {
						penutup = bersih.lastIndexOf('}', penutup - 1);
					}
				}
			}
		}

		TreeMap<String, Map<String, String>> maps = new TreeMap<String, Map<String, String>>();
		Iterator<String> keys = jsonObject.keys();
		while (keys.hasNext()) {
			try {

				String key = keys.next();
				String[] s = key.split("_");

				Map<String, String> map = maps.get(s[0]);
				if (map == null) {
					map = new HashMap<String, String>();
					maps.put(s[0], map);
				}
				map.put(key, jsonObject.getString(key));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:2669");
			}

		}
		return maps;
	}

	private TreeMap<String, Map<String, String>> maps;
	private String namaPencarianOnline = "";
	private boolean terlewat = false;
	private String terlewatInfo = "";

	private void tampilkanAbsensiOnline(final Pertemuan pertemuan) {

		Common.clear(rowUtamaAbsensiOnline);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null) {
			maps = reloadSejarahAbsensiOnline(pertemuan);

			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.setParent(rowUtamaAbsensiOnline);

			groupboxStyled.appendChild(ais.ui.util.DashboardUiKit.html(ais.ui.util.DashboardUiKit.headerModul(
					"absensi", "Sejarah Absensi Online",
					"Rekam jejak kehadiran online pada pertemuan ini, lengkap dengan waktu dan statusnya.")));

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(groupboxStyled);

			toolbar.appendChild(new ais.ui.util.MyLabelConfig("Cari"));
			final Textbox cariNama;
			toolbar.appendChild(cariNama = new Textbox(namaPencarianOnline));
			cariNama.setWidth("60px");
			cariNama.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					namaPencarianOnline = cariNama.getValue().trim();
					tampilkanAbsensiOnline(pertemuan);
				}
			});

			MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			masuk.getAttribute("janganDisabled", true);
			masuk.setParent(toolbar);
			masuk.setTooltiptext("Refresh");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					namaPencarianOnline = cariNama.getValue().trim();
					tampilkanAbsensiOnline(pertemuan);
				}
			});

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null) {
				masuk = new MyToolbarbuttonConfig("Hadir yg upld foto/video & lokasi", "/img/svg/check2.svg");
				masuk.setParent(toolbar);
				masuk.setTooltiptext("Tutup");
				masuk.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah yakin semua upload foto/video & lokasi diangap hadir di kelas ini ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													if (pertemuan.getId() != null) {
														HibernateUtil.currentSession().refresh(pertemuan);
													}
													TreeMap<String, Map<String, String>> maps = reloadSejarahAbsensiOnline(
															pertemuan);
													for (String key : maps.keySet()) {
														try {
															String[] ss = key.split(":");
															String tgl = ss[0];
															Mahasiswa mahasiswa = (Mahasiswa) (ss[1].startsWith("mhs")
																	? ConstantValues.ambil(Mahasiswa.class.getName(),
																			Long.parseLong(ss[1].replaceAll("mhs", "")))
																	: null);
															Dosen dosen = (Dosen) (ss[1].startsWith("dsn")
																	? ConstantValues.ambil(Dosen.class.getName(),
																			Long.parseLong(ss[1].replaceAll("dsn", "")))
																	: null);

															Date tglJam = null;
															String foto = maps.get(key).containsKey(key + "_foto")
																	? maps.get(key).get(key + "_foto")
																	: "";

															String video = maps.get(key).containsKey(key + "_img")
																	? maps.get(key).get(key + "_img")
																	: "";

															String lokasi = maps.get(key).containsKey(key + "_lokasi")
																	? maps.get(key).get(key + "_lokasi")
																	: "";

															Statusabsensi statusabsensi = null;
															if (mahasiswa != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(mahasiswa.getId()));
															} else if (dosen != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(dosen.getId()));
															}
															if (statusabsensi == null) {
																statusabsensi = ConstantValues.BELUM_ABSEN;
															}

															if (!foto.trim().isEmpty() && !lokasi.trim().isEmpty()
																	&& (statusabsensi.getId()
																			.equals(ConstantValues.BELUM_ABSEN.getId())
																			|| statusabsensi.getId().equals(
																					ConstantValues.MASUK.getId()))) {

																try {
																	tglJam = Common.dateFormat9.get().parse(tgl);
																} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:2800");

																}

																String keterangan = "Absensi online "
																		+ (tglJam == null ? ""
																				: Common.dateFormat5.get()
																						.format(tglJam))
																		+ " "
																		+ (video == null || video.isEmpty() ? "foto"
																				: "video")
																		+ " " + foto + " lokasi " + lokasi;

																if (mahasiswa != null) {
																	pertemuan.populate(mahasiswa.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get()
																							.format(tglJam),
																			pertemuan.getWaktuSelesai(), "Mahasiswa");
																} else if (dosen != null) {
																	pertemuan.populate(dosen.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get()
																							.format(tglJam),
																			pertemuan.getWaktuSelesai(), "Dosen");
																}
															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:2831");
														}
													}

													sesuaikan(pertemuan, false);
													Common.refreshUpdate(pertemuan);

													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Common.clear(tabpanelUtama);
															mainInit(pertemuan, tabpanelUtama, tampilInfo);
														}
													});
												}
											});

										}

									}
								});

					}
				});

				masuk = new MyToolbarbuttonConfig("Hadir yg upld foto/video", "/img/svg/check2.svg");
				masuk.setParent(toolbar);
				masuk.setTooltiptext("Tutup");
				masuk.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin semua upload foto/video diangap hadir di kelas ini ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													if (pertemuan.getId() != null) {
														HibernateUtil.currentSession().refresh(pertemuan);
													}
													TreeMap<String, Map<String, String>> maps = reloadSejarahAbsensiOnline(
															pertemuan);
													for (String key : maps.keySet()) {
														try {
															String[] ss = key.split(":");
															String tgl = ss[0];
															Mahasiswa mahasiswa = (Mahasiswa) (ss[1].startsWith("mhs")
																	? ConstantValues.ambil(Mahasiswa.class.getName(),
																			Long.parseLong(ss[1].replaceAll("mhs", "")))
																	: null);
															Dosen dosen = (Dosen) (ss[1].startsWith("dsn")
																	? ConstantValues.ambil(Dosen.class.getName(),
																			Long.parseLong(ss[1].replaceAll("dsn", "")))
																	: null);

															Date tglJam = null;
															String foto = maps.get(key).containsKey(key + "_foto")
																	? maps.get(key).get(key + "_foto")
																	: "";
															String video = maps.get(key).containsKey(key + "_img")
																	? maps.get(key).get(key + "_img")
																	: "";
															String lokasi = maps.get(key).containsKey(key + "_lokasi")
																	? maps.get(key).get(key + "_lokasi")
																	: "";

															Statusabsensi statusabsensi = null;
															if (mahasiswa != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(mahasiswa.getId()));
															} else if (dosen != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(dosen.getId()));
															}
															if (statusabsensi == null) {
																statusabsensi = ConstantValues.BELUM_ABSEN;
															}

															if (!foto.trim().isEmpty() && (statusabsensi.getId()
																	.equals(ConstantValues.BELUM_ABSEN.getId())
																	|| statusabsensi.getId()
																			.equals(ConstantValues.MASUK.getId()))) {

																try {
																	tglJam = Common.dateFormat9.get().parse(tgl);
																} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:2926");

																}

																String keterangan = "Absensi online "
																		+ (tglJam == null ? ""
																				: Common.dateFormat5.get()
																						.format(tglJam))
																		+ " "
																		+ (video == null || video.isEmpty() ? "foto"
																				: "video")
																		+ " " + foto + " lokasi " + lokasi;

																if (mahasiswa != null) {
																	pertemuan.populate(mahasiswa.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get()
																							.format(tglJam),
																			pertemuan.getWaktuSelesai(), "Mahasiswa");
																} else if (dosen != null) {
																	pertemuan.populate(dosen.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get()
																							.format(tglJam),
																			pertemuan.getWaktuSelesai(), "Dosen");
																}
															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:2957");
														}
													}

													sesuaikan(pertemuan, false);
													Common.refreshUpdate(pertemuan);

													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Common.clear(tabpanelUtama);
															mainInit(pertemuan, tabpanelUtama, tampilInfo);
														}
													});
												}
											});

										}

									}
								});

					}
				});

				masuk = new MyToolbarbuttonConfig("Hadir yg upld lokasi", "/img/svg/check2.svg");
				masuk.setParent(toolbar);
				masuk.setTooltiptext("Tutup");
				masuk.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin semua upload lokasi diangap hadir di kelas ini ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													if (pertemuan.getId() != null) {
														HibernateUtil.currentSession().refresh(pertemuan);
													}
													TreeMap<String, Map<String, String>> maps = reloadSejarahAbsensiOnline(
															pertemuan);
													for (String key : maps.keySet()) {
														try {
															String[] ss = key.split(":");
															String tgl = ss[0];
															Mahasiswa mahasiswa = (Mahasiswa) (ss[1].startsWith("mhs")
																	? ConstantValues.ambil(Mahasiswa.class.getName(),
																			Long.parseLong(ss[1].replaceAll("mhs", "")))
																	: null);
															Dosen dosen = (Dosen) (ss[1].startsWith("dsn")
																	? ConstantValues.ambil(Dosen.class.getName(),
																			Long.parseLong(ss[1].replaceAll("dsn", "")))
																	: null);

															Date tglJam = null;

															String lokasi = maps.get(key).containsKey(key + "_lokasi")
																	? maps.get(key).get(key + "_lokasi")
																	: "";
															String foto = maps.get(key).containsKey(key + "_foto")
																	? maps.get(key).get(key + "_foto")
																	: "";

															Statusabsensi statusabsensi = null;
															if (mahasiswa != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(mahasiswa.getId()));
															} else if (dosen != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(dosen.getId()));
															}
															if (statusabsensi == null) {
																statusabsensi = ConstantValues.BELUM_ABSEN;
															}

															if (!lokasi.trim().isEmpty() && (statusabsensi.getId()
																	.equals(ConstantValues.BELUM_ABSEN.getId())
																	|| statusabsensi.getId()
																			.equals(ConstantValues.MASUK.getId()))) {

																try {
																	tglJam = Common.dateFormat9.get().parse(tgl);
																} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:3050");

																}

																String keterangan = "Absensi online "
																		+ (tglJam == null ? ""
																				: Common.dateFormat5.get()
																						.format(tglJam))
																		+ " foto " + foto + " lokasi " + lokasi;

																if (mahasiswa != null) {
																	pertemuan.populate(mahasiswa.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get()
																							.format(tglJam),
																			pertemuan.getWaktuSelesai(), "Mahasiswa");
																} else if (dosen != null) {
																	pertemuan.populate(dosen.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get()
																							.format(tglJam),
																			pertemuan.getWaktuSelesai(), "Dosen");
																}
															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:3078");
														}
													}

													sesuaikan(pertemuan, false);
													Common.refreshUpdate(pertemuan);

													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Common.clear(tabpanelUtama);
															mainInit(pertemuan, tabpanelUtama, tampilInfo);
														}
													});
												}
											});

										}

									}
								});

					}
				});
			}

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setMold("paging");
			grid.setPageSize(15);
			grid.setWidth("100%");
			grid.setParent(groupboxStyled);
			grid.setWidth("100%");
			grid.setHeight("100%");
			grid.setSclass("dgrid");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Tanggal");
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig("Peserta");
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig("Info");
			column.setParent(columns);
			column.setWidth("30%");

			column = new MyColumnConfig("Foto/Video");
			column.setParent(columns);

			column = new MyColumnConfig("Lokasi");
			column.setParent(columns);

			column = new MyColumnConfig("Status");
			column.setParent(columns);
			column.setWidth(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null ? "12%" : "0%");

			Rows rowsData = new Rows();
			rowsData.setParent(grid);

			Mahasiswa selectedMhs = tbmuser == null ? null : tbmuser.getMahasiswa();

			for (final String key : maps.keySet()) {
				try {
					String[] ss = key.split(":");
					if (ss == null || ss.length < 2 || ss[1] == null || ss[1].trim().length() == 0) {
						continue;
					}
					String tgl = ss[0];
					Mahasiswa mahasiswa = (Mahasiswa) (ss[1].startsWith("mhs")
							? ConstantValues.ambil(Mahasiswa.class.getName(),
									Long.parseLong(ss[1].replaceAll("mhs", "")))
							: null);

					if (selectedMhs != null && selectedMhs.getId() != null) {
						if (mahasiswa == null || mahasiswa.getId() == null
								|| !mahasiswa.getId().equals(selectedMhs.getId())) {
							continue;
						}
					}

					Dosen dosen = (Dosen) (ss[1].startsWith("dsn")
							? ConstantValues.ambil(Dosen.class.getName(), Long.parseLong(ss[1].replaceAll("dsn", "")))
							: null);

					if (namaPencarianOnline.trim().isEmpty() || ((mahasiswa != null && ((mahasiswa.getNim() != null
							&& mahasiswa.getNim().toLowerCase().contains(namaPencarianOnline.trim().toLowerCase())) ||

							(mahasiswa.getNama() != null && mahasiswa.getNama().toLowerCase()
									.contains(namaPencarianOnline.trim().toLowerCase())))

					))

							|| ((dosen != null && ((dosen.getNidn() != null
									&& dosen.getNidn().toLowerCase().contains(namaPencarianOnline.trim().toLowerCase()))
									||

									(dosen.getNama() != null && dosen.getNama().toLowerCase()
											.contains(namaPencarianOnline.trim().toLowerCase())))

							))

					) {

						MyFormRow rowData = new MyFormRow();
						rowData.setValign("top");
						rowData.setValign("top");
						rowData.setParent(rowsData);
						try {
							Label a;
							rowData.appendChild(a = new Label(
									Common.dateFormat5.get().format(Common.dateFormat9.get().parse(tgl))));
							a.setStyle("font-size:9px;");
						} catch (Exception e) {
							rowData.appendChild(new Label());
						}

						Label aaa;
						rowData.appendChild(
								aaa = new Label(mahasiswa != null ? mahasiswa.getNim() + " " + mahasiswa.getNama()
										: dosen != null ? dosen.getNama() : ""));
						aaa.setStyle("font-size:9px;");

						rowData.appendChild(new MyHtml(maps.get(key).containsKey(key + "_info")
								? "<div style='font-size:9px;'>" + maps.get(key).get(key + "_info") + "</div>"
								: ""));
						A a;
						rowData.appendChild(a = new A(
								maps.get(key).containsKey(key + "_foto") ? maps.get(key).get(key + "_foto") : ""));
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Clients.evalJavaScript("popupCenter({url: '" + ((A) arg0.getTarget()).getLabel()
										+ "', title: 'Data', w: 1200, h: 600});");
							}
						});
						a.setStyle("font-size:9px;");

						rowData.appendChild(a = new A(
								maps.get(key).containsKey(key + "_lokasi") ? maps.get(key).get(key + "_lokasi") : ""));
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Clients.evalJavaScript("popupCenter({url: '" + ((A) arg0.getTarget()).getLabel()
										+ "', title: 'Data', w: 1200, h: 600});");
							}
						});
						a.setStyle("font-size:9px;");

						Statusabsensi statusabsensi = null;
						if (mahasiswa != null) {
							statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
									pertemuan.retreiveAbsensiId(mahasiswa.getId()));
						} else if (dosen != null) {
							statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
									pertemuan.retreiveAbsensiId(dosen.getId()));
						}
						if (statusabsensi == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}

						if (statusabsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId())) {
							masuk = new MyToolbarbuttonConfig("Hadirkan", "/img/svg/check2.svg");
							masuk.setStyle("font-size:9px;");
							masuk.setOrient("vertical");
							masuk.setParent(rowData);
							masuk.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah yakin mahasiswa ini dianggap hadir di kelas ini ?",
											"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														Common.createDefaultTimer(new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {

																if (pertemuan.getId() != null) {
																	HibernateUtil.currentSession().refresh(pertemuan);
																}

																try {
																	String[] ss = key.split(":");
																	String tgl = ss[0];
																	Mahasiswa mahasiswa = (Mahasiswa) (ss[1]
																			.startsWith("mhs")
																					? ConstantValues.ambil(
																							Mahasiswa.class.getName(),
																							Long.parseLong(
																									ss[1].replaceAll(
																											"mhs", "")))
																					: null);
																	Dosen dosen = (Dosen) (ss[1].startsWith("dsn")
																			? ConstantValues.ambil(
																					Dosen.class.getName(),
																					Long.parseLong(ss[1]
																							.replaceAll("dsn", "")))
																			: null);

																	Date tglJam = null;
																	String foto = maps.get(key)
																			.containsKey(key + "_foto")
																					? maps.get(key).get(key + "_foto")
																					: "";

																	String video = maps.get(key)
																			.containsKey(key + "_img")
																					? maps.get(key).get(key + "_img")
																					: "";

																	String lokasi = maps.get(key)
																			.containsKey(key + "_lokasi")
																					? maps.get(key).get(key + "_lokasi")
																					: "";

																	Statusabsensi statusabsensi = null;
																	if (mahasiswa != null) {
																		statusabsensi = (Statusabsensi) ConstantValues
																				.ambil(Statusabsensi.class.getName(),
																						pertemuan.retreiveAbsensiId(
																								mahasiswa.getId()));
																	} else if (dosen != null) {
																		statusabsensi = (Statusabsensi) ConstantValues
																				.ambil(Statusabsensi.class.getName(),
																						pertemuan.retreiveAbsensiId(
																								dosen.getId()));
																	}
																	if (statusabsensi == null) {
																		statusabsensi = ConstantValues.BELUM_ABSEN;
																	}

																	try {
																		tglJam = Common.dateFormat9.get().parse(tgl);
																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:3322");

																	}

																	String keterangan = "Absensi online "
																			+ (tglJam == null ? ""
																					: Common.dateFormat5.get()
																							.format(tglJam))
																			+ " "
																			+ (video == null || video.isEmpty() ? "foto"
																					: "video")
																			+ " " + foto + " lokasi " + lokasi;

																	if (mahasiswa != null) {
																		pertemuan.populate(mahasiswa.getId(),
																				ConstantValues.MASUK, keterangan, null,
																				tglJam == null
																						? pertemuan.getWaktuMulai()
																						: Common.timeFormat2.get()
																								.format(tglJam),
																				pertemuan.getWaktuSelesai(),
																				"Mahasiswa");
																	} else if (dosen != null) {
																		pertemuan.populate(dosen.getId(),
																				ConstantValues.MASUK, keterangan, null,
																				tglJam == null
																						? pertemuan.getWaktuMulai()
																						: Common.timeFormat2.get()
																								.format(tglJam),
																				pertemuan.getWaktuSelesai(), "Dosen");
																	}

																} catch (Exception e) {
																	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:3355");
																}

																sesuaikan(pertemuan, false);
																Common.refreshUpdate(pertemuan);

																Common.createDefaultTimer(new EventListener() {

																	@Override
																	public void onEvent(Event arg0) throws Exception {
																		Common.clear(tabpanelUtama);
																		mainInit(pertemuan, tabpanelUtama, tampilInfo);
																	}
																});
															}
														});

													}

												}
											});

								}
							});
						} else {
							new MyLabelConfig(statusabsensi.getNama()).setParent(rowData);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:3384");
				}
			}
		}

	}

	private void tampilBawah(final Pertemuan pertemuan, Row vlayout) {
		final Tbmuser tbmuser = Common.getCurrentUser();

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
		toolbar.setParent(vlayout);

		int qtyDiskusi = pertemuan.ambilJumlahPertemuanPunyaDiskusi();
		Toolbarbutton masuk = new MyToolbarbuttonConfig("Ikut Diskusi (" + qtyDiskusi + " diskusi)",
				"/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && qtyDiskusi > 0);
		masuk.setTooltiptext("Diskusi dianggap hadir");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PertemuanPunyaDiskusiHelper.diskusiDianggapHadir(pertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (sesuaikan(pertemuan, true)) {
							Common.clear(tabpanelUtama);
							mainInit(pertemuan, tabpanelUtama, tampilInfo);
						}
					}
				});

			}
		});
		masuk.setParent(toolbar);

		int qtyUjian = pertemuan.ambilJumlahPertemuanPunyaUjian();
		masuk = new MyToolbarbuttonConfig("Ikut Ujian (" + qtyUjian + " org)", "/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && qtyUjian > 0);
		masuk.setTooltiptext("Ikut ujian dianggap hadir");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				Collection<PertemuanPunyaUjian> pertemuanPunyaUjians = pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser)
						.values();

				for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
					HasilUjianMahasiswaHelper.ujianDianggapHadir(pertemuanPunyaUjian, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (sesuaikan(pertemuan, true)) {
								Common.clear(tabpanelUtama);
								mainInit(pertemuan, tabpanelUtama, tampilInfo);
							}
						}
					});
				}

			}
		});
		masuk.setParent(toolbar);

		int pert = pertemuan.ambilJumlahTugasFileContent();

		masuk = new MyToolbarbuttonConfig("Upload \"" + pertemuan.getJudultugas() + "\" (" + pert + " org)",
				"/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(pert > 0 && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
				&& !pertemuan.getJudultugas().isEmpty());
		masuk.setTooltiptext("Upload Tugas \"" + pertemuan.getJudultugas() + "\" dianggap hadir");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				TugasMandiriHelper.uploadTugasDiangapHadir(pertemuan, pertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (sesuaikan(pertemuan, true)) {
							Common.clear(tabpanelUtama);
							mainInit(pertemuan, tabpanelUtama, tampilInfo);
						}
					}
				});

			}
		});
		masuk.setParent(toolbar);

		for (final TugasPertemuan tugasPertemuan : pertemuan.ambilTugasPertemuanTotal().values()) {
			pert = tugasPertemuan.ambilJumlahTugasFileContent();
			masuk = new MyToolbarbuttonConfig("Upload \"" + tugasPertemuan.getJudultugas() + "\" (" + pert + " org)",
					"/img/svg/check2.svg");
			masuk.setStyle("font-size:9px;");
			masuk.setVisible(pert > 0 && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
					&& !tugasPertemuan.getJudultugas().isEmpty());
			masuk.setTooltiptext("Upload Tugas \"" + tugasPertemuan.getJudultugas() + "\" dianggap hadir");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					TugasMandiriHelper.uploadTugasDiangapHadir(tugasPertemuan, pertemuan, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (sesuaikan(pertemuan, true)) {
								Common.clear(tabpanelUtama);
								mainInit(pertemuan, tabpanelUtama, tampilInfo);
							}
						}
					});

				}
			});
			masuk.setParent(toolbar);
		}
		pert = 0;
		if (mahasiswas != null)
			for (GeneralValueObject generalValueObject : mahasiswas) {
				TreeMap<String, String> d = pertemuan.ambilData("tugas", generalValueObject.getId().toString(),
						pertemuan.getMulai(), pertemuan.getSelesai());
				if (!d.isEmpty()) {
					pert += d.size();
				}

			}
		if (listDosen != null)
			for (Dosen dosen : listDosen) {
				TreeMap<String, String> d = pertemuan.ambilData("tugas", dosen.getId().toString(), pertemuan.getMulai(),
						pertemuan.getSelesai());
				if (!d.isEmpty()) {
					pert += d.size();
				}
			}

		masuk = new MyToolbarbuttonConfig("Akses \"" + pertemuan.getJudultugas() + "\" (" + pert + " akses)",
				"/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(pert > 0 && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
				&& !pertemuan.getJudultugas().isEmpty());
		masuk.setTooltiptext("Akses Tugas \"" + pertemuan.getJudultugas() + "\"  dianggap hadir");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PertemuanPunyaDiskusiHelper.aksesDianggapHadir(pertemuan, "tugas",
						"Akses Tugas \"" + pertemuan.getJudultugas() + "\"", pertemuan.getMulai(),
						pertemuan.getSelesai(), new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (sesuaikan(pertemuan, true)) {
									Common.clear(tabpanelUtama);
									mainInit(pertemuan, tabpanelUtama, tampilInfo);
								}
							}
						});

			}
		});
		masuk.setParent(toolbar);

		for (final TugasPertemuan tugasPertemuan : pertemuan.ambilTugasPertemuanTotal().values()) {
			pert = 0;
			if (mahasiswas != null)
				for (GeneralValueObject generalValueObject : mahasiswas) {
					TreeMap<String, String> d = tugasPertemuan.ambilData("tugas", generalValueObject.getId().toString(),
							tugasPertemuan.getMulai(), tugasPertemuan.getSelesai());
					if (!d.isEmpty()) {
						pert += d.size();
					}

				}
			if (listDosen != null)
				for (Dosen dosen : listDosen) {
					TreeMap<String, String> d = tugasPertemuan.ambilData("tugas", dosen.getId().toString(),
							tugasPertemuan.getMulai(), tugasPertemuan.getSelesai());
					if (!d.isEmpty()) {
						pert += d.size();
					}
				}

			masuk = new MyToolbarbuttonConfig("Akses \"" + tugasPertemuan.getJudultugas() + "\" (" + pert + " akses)",
					"/img/svg/check2.svg");
			masuk.setStyle("font-size:9px;");
			masuk.setVisible(pert > 0 && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
					&& !tugasPertemuan.getJudultugas().isEmpty());
			masuk.setTooltiptext("Akses Tugas \"" + tugasPertemuan.getJudultugas() + "\"  dianggap hadir");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PertemuanPunyaDiskusiHelper.aksesDianggapHadir(tugasPertemuan, "tugas",
							"Akses Tugas \"" + tugasPertemuan.getJudultugas() + "\"", tugasPertemuan.getMulai(),
							tugasPertemuan.getSelesai(), new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (sesuaikan(pertemuan, true)) {
										Common.clear(tabpanelUtama);
										mainInit(pertemuan, tabpanelUtama, tampilInfo);
									}
								}
							});

				}
			});
			masuk.setParent(toolbar);
		}

		pert = 0;
		if (mahasiswas != null)
			for (GeneralValueObject generalValueObject : mahasiswas) {
				TreeMap<String, String> d = pertemuan.ambilData("online", generalValueObject.getId().toString());
				if (!d.isEmpty()) {
					pert += d.size();
				}

			}
		if (listDosen != null)
			for (Dosen dosen : listDosen) {
				TreeMap<String, String> d = pertemuan.ambilData("online", dosen.getId().toString());
				if (!d.isEmpty()) {
					pert += d.size();
				}
			}

		masuk = new MyToolbarbuttonConfig("Video Conf.(" + pert + " akses)", "/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && pert > 0);
		masuk.setTooltiptext("Ikut. Vidio Conf.");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PertemuanPunyaDiskusiHelper.aksesDianggapHadir(pertemuan, "online", "Video Conference", null, null,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (sesuaikan(pertemuan, true)) {
									Common.clear(tabpanelUtama);
									mainInit(pertemuan, tabpanelUtama, tampilInfo);
								}
							}
						});

			}
		});
		masuk.setParent(toolbar);

		pert = 0;
		if (mahasiswas != null)
			for (GeneralValueObject generalValueObject : mahasiswas) {
				TreeMap<String, String> d = pertemuan.ambilData("akses", generalValueObject.getId().toString());
				if (!d.isEmpty()) {
					pert += d.size();
				}

			}
		if (listDosen != null)
			for (Dosen dosen : listDosen) {
				TreeMap<String, String> d = pertemuan.ambilData("akses", dosen.getId().toString());
				if (!d.isEmpty()) {
					pert += d.size();
				}
			}

		masuk = new MyToolbarbuttonConfig("Login & Akses (" + pert + " akses)", "/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && pert > 0);
		masuk.setTooltiptext("Mahasiswa dan dosen yang login dan akses (" + pert + " org)");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PertemuanPunyaDiskusiHelper.aksesDianggapHadir(pertemuan, "akses", "Akses Pertemuan", null, null,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (sesuaikan(pertemuan, true)) {
									Common.clear(tabpanelUtama);
									mainInit(pertemuan, tabpanelUtama, tampilInfo);
								}
							}
						});

			}
		});
		masuk.setParent(toolbar);

		int jumlahBelumAbsen = 0;
		for (GeneralValueObject generalValueObject : mahasiswas) {
			Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					pertemuan.retreiveAbsensiId(generalValueObject.getId()));
			if (statusabsensi == null || statusabsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId())) {
				jumlahBelumAbsen++;
			}
		}

		masuk = new MyToolbarbuttonConfig("Belum absen jadikan Alpa (" + jumlahBelumAbsen + " org)",
				"/img/Check-icon.png");
		masuk.setStyle("font-size:9px;");
		masuk.setParent(toolbar);
		masuk.setTooltiptext("Semua mahasiswa yang belum absen dianggap Alpa");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin semua mahasiswa yang belum absen dianggap Alpa ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											if (pertemuan.getId() != null) {
												HibernateUtil.currentSession().refresh(pertemuan);
											}

											for (GeneralValueObject generalValueObject : mahasiswas) {
												Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
														Statusabsensi.class.getName(),
														pertemuan.retreiveAbsensiId(generalValueObject.getId()));
												if (statusabsensi == null || statusabsensi.getId()
														.equals(ConstantValues.BELUM_ABSEN.getId())) {
													statusabsensi = ConstantValues.TIDAK_ADA_ALASAN;
													pertemuan.populate(generalValueObject.getId(), statusabsensi,
															"Otomatis dijadikan alpa karena tidak ada keterangan", null,
															pertemuan.getWaktuMulai(), pertemuan.getWaktuSelesai(),
															"Mahasiswa");
												}
											}
											sesuaikan(pertemuan, false);
											Common.refreshUpdate(pertemuan);

											reload(pertemuan);
										}
									});

								}

							}
						});

			}
		});
	}

	private LampiranLain lampiranTizakMasuk;

	private void createListMahasiswaIzin(Component parentrow, final Pertemuan pertemuan) {

		Row rowUtama = Common.tampilanScroll1(parentrow);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(rowUtama);
		MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Ajukan Izin atau Sakit", "/img/add_item.png");
		masuk.setParent(toolbar);
		masuk.setTooltiptext("Ajukan Izin");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				lampiranTizakMasuk = null;

				final Window window = new Window("Pengajuan Izin atau Sakit", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("400px");
				window.setWidth("500px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				List<Long> indsMhsPerkuliahan = new ArrayList<Long>();
				for (GeneralValueObject mhs : AbsensiHelper.populateMahasiswaDariPertemuan(pertemuan)) {
					indsMhsPerkuliahan.add(mhs.getId());
				}
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setValign("top");

				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
				final AmbilDataMahasiswaBanbox mahasiswa;
				row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox(indsMhsPerkuliahan));
				mahasiswa.setWidth("90%");

				final Radiogroup status = new Radiogroup();
				MyRadioConfig radio = new MyRadioConfig();
				radio.setLabel(ConstantValues.IZIN.getNama());
				radio.setAttribute("nilai", ConstantValues.IZIN);
				status.appendChild(radio);

				radio = new MyRadioConfig();
				radio.setLabel(ConstantValues.SAKIT.getNama());
				radio.setAttribute("nilai", ConstantValues.SAKIT);
				status.appendChild(radio);

				row = new MyFormRow();
				row.setValign("top");

				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
				row.appendChild(status);

				row = new MyFormRow();
				row.setValign("top");

				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Alasan"));
				final Textbox katerangan;
				row.appendChild(katerangan = new Textbox());
				katerangan.setWidth("90%");
				katerangan.setRows(5);

				row = new MyFormRow();
				row.setValign("top");

				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));

				Hbox hbox = new Hbox();
				hbox.setParent(row);
				LampiranLain.createDownloadUploadFileLain(hbox, null, LampiranLain.IZIN_TIDAK_MASUK, "Lampiran", false,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								lampiranTizakMasuk = (LampiranLain) arg0.getData();
							}
						}, null, false, false, false, true);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan ajukan izin atau sakit",
						"/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Mahasiswa mhs = (Mahasiswa) mahasiswa.getAttribute("myValue");

						Radio s = status.getSelectedItem();

						if (mhs == null) {
							MyMessageboxConfig.show("Mohon maaf, mahasiswa belum dipilih. Langkah yang dapat dilakukan: (1) pilih mahasiswa dari daftar atau cari menggunakan fitur pencarian; (2) pastikan data mahasiswa sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						if (s == null) {
							MyMessageboxConfig.show("Mohon maaf, status kehadiran belum dipilih. Langkah yang dapat dilakukan: (1) pilih status kehadiran dari pilihan yang tersedia; (2) pastikan salah satu status sudah dipilih; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						if (katerangan.getValue().trim().equals("")) {
							MyMessageboxConfig.show("Mohon maaf, alasan izin belum diisi. Langkah yang dapat dilakukan: (1) isi alasan izin tidak masuk pada kolom yang tersedia; (2) pastikan alasan tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Session session = null;
						Session sessionFallback = null;
						Transaction transaksiFallback = null;
						PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = null;
						try {
							session = HibernateUtil.currentSession();
							if (session == null || !session.isOpen()) {
								sessionFallback = HibernateUtil.openSession();
								transaksiFallback = sessionFallback.beginTransaction();
								session = sessionFallback;
							}
							pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) session
									.createCriteria(PengajuanIzinTidakMasukPerkuliahan.class)
									.add(Restrictions.eq("mahasiswa", mhs))
									.add(Restrictions.eq("pertemuan", pertemuan)).setMaxResults(1).uniqueResult();
							if (pengajuanIzinTidakMasukPerkuliahan == null) {
								pengajuanIzinTidakMasukPerkuliahan = new PengajuanIzinTidakMasukPerkuliahan();
							} else if (pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
								MyMessageboxConfig.show(
										"Pengajian mahasiswa " + mhs.getNama()
												+ " telah disetujui, sehingga tidak bisa diubah",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return;
							}
							pengajuanIzinTidakMasukPerkuliahan.setPertemuan(pertemuan);
							pengajuanIzinTidakMasukPerkuliahan.setMahasiswa(mhs);
							pengajuanIzinTidakMasukPerkuliahan
									.setStatusabsensi((Statusabsensi) s.getAttribute("nilai"));
							pengajuanIzinTidakMasukPerkuliahan.setKeterangan(katerangan.getValue().trim());
							Common.refreshSaveOrUpdate(session, pengajuanIzinTidakMasukPerkuliahan);
							session.flush();
							if (transaksiFallback != null && transaksiFallback.isActive()) {
								transaksiFallback.commit();
							}
						} catch (Exception eSimpanIzin) {
							if (transaksiFallback != null && transaksiFallback.isActive()) {
								try {
									transaksiFallback.rollback();
								} catch (Exception eRollback) {
									System.err.println("Gagal rollback sesi lokal pengajuan izin: "
											+ eRollback.getMessage());
								}
							}
							throw eSimpanIzin;
						} finally {
							if (sessionFallback != null) {
								try {
									sessionFallback.clear();
								} catch (Exception eClear) {
									System.err.println("Gagal clear sesi lokal pengajuan izin: " + eClear.getMessage());
								}
								try {
									sessionFallback.disconnect();
								} catch (Exception eDisconnect) {
									System.err.println("Gagal disconnect sesi lokal pengajuan izin: "
											+ eDisconnect.getMessage());
								}
								try {
									if (sessionFallback.isOpen()) {
										sessionFallback.close();
									}
								} catch (Exception eClose) {
									System.err.println("Gagal menutup sesi lokal pengajuan izin: " + eClose.getMessage());
								}
							}
						}

						pertemuan.belum("PengajuanIzinTidakMasukPerkuliahan");

						System.out
								.println("pengajuanIzinTidakMasukPerkuliahan -> " + pengajuanIzinTidakMasukPerkuliahan);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadIzinAbsensi(pertemuan);
								window.detach();
							}
						});

						try {
							session = StreamingHibernateUtil.getInstance().currentSession();

							if (lampiranTizakMasuk != null && lampiranTizakMasuk.getId() != null) {
								session.refresh(lampiranTizakMasuk);
								lampiranTizakMasuk.setRef(pengajuanIzinTidakMasukPerkuliahan.getId());

								session.getTransaction().begin();
								session.update(lampiranTizakMasuk);
								session.getTransaction().commit();
							}

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

						CommonEmail.infoAdaIzinAbsensi(pengajuanIzinTidakMasukPerkuliahan);

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowUtama.getParent());
		row.setValign("top");

		mahasiswaIzinGrid = new MyGrid();
		mahasiswaIzinGrid.setMold("paging");
		mahasiswaIzinGrid.setPageSize(10000);
		mahasiswaIzinGrid.setParent(row);
		mahasiswaIzinGrid.setWidth("100%");
		mahasiswaIzinGrid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(mahasiswaIzinGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("OK");
		column.setWidth("16%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mahasiswa");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		reloadIzinAbsensi(pertemuan);

		row = new MyFormRow();
		new Space().setParent(row);
		row.setParent(rowUtama.getParent());
		row.setValign("top");

		row = new MyFormRow();
		new Space().setParent(row);
		row.setParent(rowUtama.getParent());
		row.setValign("top");

		Group group = new ais.ui.util.MyGroupConfig("Konfirmasi kehadiran dosen oleh perwakilan kelas");
		group.setVisible(pertemuan.getJadwalPelajaran() == null && pertemuan.getJadwalUjianPMB() == null
				&& pertemuan.getJadwalUjianPSB() == null);
		group.setParent(rowUtama.getParent());

		row = new MyFormRow();
		row.setVisible(pertemuan.getJadwalPelajaran() == null && pertemuan.getJadwalUjianPMB() == null
				&& pertemuan.getJadwalUjianPSB() == null);
		row.setParent(rowUtama.getParent());
		row.setValign("top");
		row.appendChild(AbsensiHelper.createStatusKehadiranKonfirmasi(pertemuan, mahasiswa));

		row = new MyFormRow();
		new Space().setParent(row);
		row.setParent(rowUtama.getParent());
		row.setValign("top");

		row = new MyFormRow();
		new Space().setParent(row);
		row.setParent(rowUtama.getParent());
		row.setValign("top");

		group = new ais.ui.util.MyGroupConfig("Konfirmasi kesesuaian dengan RPS oleh perwakilan kelas");
		group.setVisible(pertemuan.getJadwalPelajaran() == null && pertemuan.getJadwalUjianPMB() == null
				&& pertemuan.getJadwalUjianPSB() == null);
		group.setParent(rowUtama.getParent());

		row = new MyFormRow();
		row.setVisible(pertemuan.getJadwalPelajaran() == null && pertemuan.getJadwalUjianPMB() == null
				&& pertemuan.getJadwalUjianPSB() == null);
		row.setParent(rowUtama.getParent());
		row.setValign("top");
		row.appendChild(AbsensiHelper.createStatusSesuaiDenganRpsKonfirmasi(pertemuan, mahasiswa));

		row = new MyFormRow();
		new Space().setParent(row);
		row.setParent(rowUtama.getParent());
		row.setValign("top");

		row = new MyFormRow();
		new Space().setParent(row);
		row.setParent(rowUtama.getParent());
		row.setValign("top");

		group = new ais.ui.util.MyGroupConfig("Konfirmasi kesesuaian dengan RPS oleh penjamin mutu");
		group.setVisible(pertemuan.getJadwalPelajaran() == null && pertemuan.getJadwalUjianPMB() == null
				&& pertemuan.getJadwalUjianPSB() == null);
		group.setParent(rowUtama.getParent());

		row = new MyFormRow();
		row.setVisible(pertemuan.getJadwalPelajaran() == null && pertemuan.getJadwalUjianPMB() == null
				&& pertemuan.getJadwalUjianPSB() == null);
		row.setParent(rowUtama.getParent());
		row.setValign("top");
		row.appendChild(AbsensiHelper.createStatusSesuaiOlehAkademik(pertemuan, Common.getApakahAdmin() ? tbmuser
				: (tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
						|| tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null || tbmuser.getCalonPegawai() != null) ? null : tbmuser));
	}

	public boolean sesuaikan(Pertemuan pertemuan, boolean update) {

		if (update) {
			if (pertemuan.getId() != null) {
				HibernateUtil.currentSession().refresh(pertemuan);
			}
		}

		pertemuan.setWaktuMulai(
				waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
		pertemuan.setWaktuSelesai(
				waktuSelesai.getValue() == null ? null : Common.timeFormat2.get().format(waktuSelesai.getValue()));

		pertemuan.setDosenTamu2(dosenTamu2.getValue());
		pertemuan.setMetodePembelajaran(metode.getValue());
		pertemuan.setTopik(topik.getValue().trim());
		// pertemuan.setBukuRujukan1(bukuRujukan1.getText());
		// pertemuan.setBukuRujukan2(bukuRujukan2.getText());
		pertemuan.setDosenTamu(dosenTamu.getText());

		// pertemuan.setTanggal(mulai.getValue());pertemuan.setTanggalEdit(mulai.getValue());
		pertemuan.setStatusPertemuan(
				(StatusPertemuan) (ujian.getSelectedItem() == null ? null : ujian.getSelectedItem().getValue()));

		pertemuan.setRuang((Ruang) ruang.getAttribute("ruang"));

		pertemuan.setTanggal(tanggal.getValue());
		pertemuan.setTanggalEdit(tanggal.getValue());
		pertemuan.setTanggalRealisasi(tanggalRealisasi.getValue());
		pertemuan.setBukuRujukan1(bukuRujukan1.getValue());
		pertemuan.setBukuRujukan2(bukuRujukan2.getValue());

		pertemuan.setOnlineMenggunakan(
				(Integer) (onlineMenggunakan == null || onlineMenggunakan.getSelectedItem() == null ? null
						: onlineMenggunakan.getSelectedItem().getValue()));
		pertemuan.setZoomLink(zoomLink == null ? "" : zoomLink.getValue().trim());
		pertemuan.setBbbLink(bbbLink == null ? "" : bbbLink.getValue().trim());
		pertemuan.setSkypeLink(skypeLink == null ? "" : skypeLink.getValue().trim());
		pertemuan.setWaLink(waLink == null ? "" : waLink.getValue().trim());
		pertemuan.setMeetLink(meetLink == null ? "" : meetLink.getValue().trim());
		pertemuan.setPerkulaiahnOnlineHarusSesuaiJadwal(perkulaiahnOnlineHarusSesuaiJadwal.isChecked());

		pertemuan.setMahasiswaBolehAbsenMenggunakanFoto(mahasiswaBolehAbsenMenggunakanFoto.isChecked());
		pertemuan.setDosenBolehAbsenMenggunakanFoto(dosenBolehAbsenMenggunakanFoto.isChecked());

		pertemuan.setBolehAbsenSebelumWaktuMulaiDalamMenit(bolehAbsenSebelumWaktuMulaiDalamMenit.getValue());
		pertemuan.setBolehAbsenSetelahWaktuMulaiDalamMenit(bolehAbsenSetelahWaktuMulaiDalamMenit.getValue());

		pertemuan.setLainLink(linkLain == null ? "" : linkLain.getValue().trim());

		pertemuan.setLokasi((Lokasi) (lokasi == null || lokasi.getSelectedItem() == null ? null
				: lokasi.getSelectedItem().getValue()));
		pertemuan.setJarak(jarak == null ? null : jarak.getValue());

		if (update) {
			Common.refreshUpdate(pertemuan);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private void reloadAbsensiBaru(final Pertemuan pertemuan, final Rows rowsUtama) {

		MyFormRow rowUtama = new MyFormRow();
		rowUtama.setParent(rowsUtama);
		tampilBawah(pertemuan, rowUtama);

		Integer tahap;
		try {
			tahap = perkuliahan == null || perkuliahan.getKurikulumPunyaMatakuliah() == null
					|| !org.hibernate.Hibernate.isInitialized(perkuliahan.getKurikulumPunyaMatakuliah())
					|| perkuliahan.getKurikulumPunyaMatakuliah().getTahap() == null ? 0
							: perkuliahan.getKurikulumPunyaMatakuliah().getTahap();
		} catch (Exception e) {
			tahap = 0;
		}
		Integer semester = perkuliahan == null ? (mahasiswa == null ? 0 : mahasiswa.currentSemester())
				: perkuliahan.getSemester();

		Session session = HibernateUtil.currentSession();
		List<KelasPertemuan> kelasPertemuans = new ArrayList<KelasPertemuan>();
		// Pertemuan baru belum memiliki id. Menjadikannya parameter Criteria membuat
		// Hibernate mencoba menyimpan referensi transient dan melempar
		// TransientObjectException. Pada kondisi ini memang belum mungkin ada kelas
		// pertemuan tersimpan, sehingga daftar kosong adalah hasil yang benar.
		if (pertemuan != null && pertemuan.getId() != null) {
			kelasPertemuans = session.createCriteria(KelasPertemuan.class)
					.add(Restrictions.eq("pertemuan.id", pertemuan.getId())).addOrder(Order.asc("nama")).list();
		}

		if (kelasPertemuans.isEmpty()) {

			int index = 1;
			for (GeneralValueObject mhs : this.mahasiswas) {
				MyFormRow arg0 = new MyFormRow();
				arg0.setParent(rowsUtama);
				try {
					tampilRowAbsensi(arg0, index++, (VOMahasiswa) mhs, statusabsensis, status, semester, tbmuser, tahap,
							pertemuan);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

		} else {

			List<Long> mahasiswasYgAdaDiKelas = new ArrayList<Long>();

			for (final KelasPertemuan kelasPertemuan : kelasPertemuans) {
				Group group = new ais.ui.util.MyGroupConfig();
				group.setParent(rowsUtama);
				Hbox hbox = new Hbox();
				hbox.setParent(group);

				Label label = new Label(kelasPertemuan.getNama()
						+ (", Tgl: "
								+ (kelasPertemuan.getMulai() == null ? ""
										: Common.dateFormat.get().format(kelasPertemuan.getMulai()))
								+ (kelasPertemuan.getSelesai() == null ? ""
										: " s.d " + Common.dateFormat.get().format(kelasPertemuan.getSelesai())))
						+ ", Waktu: " + kelasPertemuan.getWaktuMulai() + " s.d " + kelasPertemuan.getWaktuSelesai()
						+ ", Ruang : " + (kelasPertemuan.getRuang() == null ? "" : kelasPertemuan.getRuang()));
				label.setWidth("80%");
				hbox.appendChild(label);

				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
						new java.util.ArrayList<org.zkoss.zk.ui.Component>();

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
				button.setAttribute("janganDisabled", true);
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						CommonReportHelper.onLaporanBeritaAcara(pertemuan, kelasPertemuan);
					}

				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						initKelasPertemuan(kelasPertemuan, pertemuan);
					}

				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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
												Common.refreshDelete(kelasPertemuan);

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														mahasiswas = AbsensiHelper
																.populateMahasiswaDariPertemuan(pertemuan);
														reload(pertemuan);
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
				aksiButtons.add(button);

				ais.ui.util.UIHelper.buatBarisAksi(hbox, 3, aksiButtons);

				List<GeneralValueObject> mahasiswas = session.createCriteria(DetailKelasPertemuan.class)
						.setProjection(Projections.property("mahasiswa")).createAlias("mahasiswa", "mahasiswa")
						.add(Restrictions.eq("kelasPertemuan", kelasPertemuan)).addOrder(Order.asc("mahasiswa.nim"))
						.list();

				for (GeneralValueObject o : mahasiswas) {
					mahasiswasYgAdaDiKelas.add(o.getId());
				}

				int index = 1;
				for (GeneralValueObject mahasiswa : mahasiswas) {
					MyFormRow arg0 = new MyFormRow();
					arg0.setParent(rowsUtama);
					try {
						tampilRowAbsensi(arg0, index++, (Mahasiswa) mahasiswa, statusabsensis, status, semester,
								tbmuser, tahap, pertemuan);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				mahasiswas = null;
			}

			Group group = new ais.ui.util.MyGroupConfig();
			group.setParent(rowsUtama);
			Hbox hbox = new Hbox();
			hbox.setParent(group);

			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum dimasukkan ke pemisahan kelas")));

			int index = 1;
			for (GeneralValueObject mhs : this.mahasiswas) {
				if (!mahasiswasYgAdaDiKelas.contains(mhs.getId())) {
					MyFormRow arg0 = new MyFormRow();
					arg0.setParent(rowsUtama);
					try {
						tampilRowAbsensi(arg0, index++, (Mahasiswa) mhs, statusabsensis, status, semester, tbmuser,
								tahap, pertemuan);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}

	}

	private void reloadIzinAbsensi(Pertemuan pertemuan) {

		List<PengajuanIzinTidakMasukPerkuliahan> pengajuanIzinTidakMasukPerkuliahans = pertemuan
				.ambilPengajuanIzinTidakMasukPerkuliahanTotal();
		ListModel strset = new SimpleListModel(pengajuanIzinTidakMasukPerkuliahans);
		mahasiswaIzinGrid.setRowRenderer(new MahasiswaIzinRenderer(pertemuan));
		mahasiswaIzinGrid.setModelCheckMobile(strset);
		mahasiswaIzinGrid.setOddRowSclass("non-odd");

	}

	private void tampilRowAbsensi(Row rowDataAbsen, Integer index, final VOMahasiswa mahasiswa,
			List<Statusabsensi> statusabsensis, Statusabsensi status, Integer semester, Tbmuser tbmuser, Integer tahap,
			final Pertemuan pertemuan) throws Exception {

		MyGroupboxStyled group = new MyGroupboxStyled();
		group.setParent(rowDataAbsen);
		// Tampilan kartu modern (lihat gayaKartuPresensi). Append agar sclass bawaan tetap ada.
		group.setSclass(((group.getSclass() == null) ? "" : group.getSclass() + " ") + "ais-absn-card");

		rowDataAbsen.setAttribute("mahasiswa", mahasiswa);
		String ket = pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId());

		// Mengganti _ dengan , untuk parameter
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

		final Textbox keterangan = new Textbox(ket);
		final Timebox waktuMulai = new ais.ui.util.MyTimebox();
		final Timebox waktuSelesai = new ais.ui.util.MyTimebox();

		waktuMulai.setVisible(pertemuan.getTampilkanJamAbsensiBagiMahasiswa()
				&& Common.bolehKonfigurasi("tampilkan_jam_masuk_absen_untuk_mahasiswa"));
		waktuSelesai.setVisible(waktuMulai.isVisible());

		final Radiogroup kehadiran = new Radiogroup();
		kehadiran.setWidth("82px");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kehadiran.getSelectedItem() == null) {
					return;
				}

				if (pertemuan.getId() != null) {
					HibernateUtil.currentSession().refresh(pertemuan);
				}

				Statusabsensi statusabsensi = (Statusabsensi) kehadiran.getSelectedItem().getAttribute("value");
				if (statusabsensi.getKode() != null && statusabsensi.getKode().trim().equals("M")) {
					if (waktuMulai.getValue() == null) {
						try {
							waktuMulai.setValue(
									pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4367");
						}
					}
					if (waktuSelesai.getValue() == null) {
						try {
							waktuSelesai.setValue(
									pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4376");
						}
					}
				} else {
					waktuMulai.setValue(null);
					waktuSelesai.setValue(null);
				}

				// GATE SP (semester pendek): tolak absensi bila pembayaran SP mahasiswa belum lunas.
				String alasanSpAbsen = ais.action.master.helper.util.GateBayarSpUtil.alasanBlokir(pertemuan.getPerkuliahan(), mahasiswa.getId());
				if (alasanSpAbsen != null) {
					try {
						ais.ui.util.MyMessageboxConfig.show(alasanSpAbsen, "Peringatan", ais.ui.util.MyMessageboxConfig.OK,
								ais.ui.util.MyMessageboxConfig.EXCLAMATION);
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4390");
					}
					return;
				}
				pertemuan.populate(mahasiswa.getId(), statusabsensi, keterangan.getValue(), null,
						waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
						waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
						"Mahasiswa");
				sesuaikan(pertemuan, false);
				Common.refreshUpdate(pertemuan);
			}
		};

		PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = pertemuan
				.ambilPengajuanIzinTidakMasukPerkuliahan(mahasiswa);
		Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
				pertemuan.retreiveAbsensiId(mahasiswa.getId()));
		if (statusabsensi == null) {
			statusabsensi = ConstantValues.BELUM_ABSEN;
		}

		Hbox hboxStatus = new Hbox();
		// Status kehadiran (Hadir/Alpa + jam) rapat ke KIRI agar sejajar antar kartu.
		hboxStatus.setAlign("center");
		hboxStatus.setStyle("text-align:left; gap:6px; flex-wrap:wrap;");

		if ((tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
				&& (!pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() || !terlewat)) || mahasiswaBolehUbahAbsen) {

			if (statusabsensi != null && pengajuanIzinTidakMasukPerkuliahan != null
					&& pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi() != null
					&& pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {

				if (!statusabsensi.getId().equals(pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi().getId())) {
					if (pertemuan.getId() != null) {
						HibernateUtil.currentSession().refresh(pertemuan);
					}
					pertemuan.populate(mahasiswa.getId(), pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi(),
							keterangan.getValue(), null,
							waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
							waktuSelesai.getValue() == null ? ""
									: Common.timeFormat2.get().format(waktuSelesai.getValue()),
							"Mahasiswa");
					sesuaikan(pertemuan, false);
					Common.refreshUpdate(pertemuan);
				}

				new Label("Status: "
						+ Common.getBahasaConfig(pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi().getNama()))
						.setParent(hboxStatus);
			} else {
				Vbox vbox = new Vbox();
				vbox.setParent(hboxStatus);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);
				Common.insertRadioItemsMyConfig(kehadiran, "nama", ConstantValues.listAbsenMahasiswa);
				Common.selectRadioItem(kehadiran, statusabsensi);

				kehadiran.addEventListener("onClick", eventListener);
				hbox.appendChild(kehadiran);

				hbox = new Hbox();
				hbox.setParent(vbox);
				hbox.setVisible(waktuMulai.isVisible());

				waktuMulai.setCols(1);
				waktuSelesai.setCols(1);

				hbox.appendChild(waktuMulai);
				waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
				try {
					String absensiMulai = pertemuan.retreiveAbsensiMulai(mahasiswa.getId());
					waktuMulai.setValue(absensiMulai == null || absensiMulai.trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(absensiMulai));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4465");
				}

				hbox.appendChild(new ais.ui.util.MyLabelConfig("s.d"));
				hbox.appendChild(waktuSelesai);
				waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
				try {
					String absensiSampai = pertemuan.retreiveAbsensiSampai(mahasiswa.getId());
					waktuSelesai.setValue(absensiSampai == null || absensiSampai.trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(absensiSampai));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4474");
				}

				waktuMulai.addEventListener("onChange", eventListener);
				waktuSelesai.addEventListener("onChange", eventListener);
			}
		} else {
			if (pengajuanIzinTidakMasukPerkuliahan != null && pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
				new Label("Status: " + pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi().getNama())
						.setParent(hboxStatus);
			} else {
				Hbox hbox = new Hbox();
				hbox.setParent(hboxStatus);
				hbox.appendChild(new MyHtml(badgeStatus(statusabsensi)));
				String wkt = pertemuan.retreiveAbsensiMulai(mahasiswa.getId()) + " s.d "
						+ pertemuan.retreiveAbsensiSampai(mahasiswa.getId());
				new Label(wkt.trim().equals("s.d") ? "" : wkt).setParent(hbox);
			}
		}

		if (mahasiswa instanceof Mahasiswa) {
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa((Mahasiswa) mahasiswa, semester, tahap, null,
					false);
			HistoryStatusMahasiswa tempHistoryStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.getHistoryStatusMahasiswa(krsMahasiswa, false);

			group.appendChild(new MyCaptionStyled(mahasiswa.getNim() + " / " + mahasiswa.getNama() + " / "
					+ tempHistoryStatusMahasiswa.getStatusAwalMahasiswa().getNama() + " / "
					+ tempHistoryStatusMahasiswa.getProgram()));
		} else {
			group.appendChild(new MyCaptionStyled(mahasiswa.getNim() + " / " + mahasiswa.getNama()));
		}

		Hbox hbox = new Hbox();
		hbox.appendChild(new Label(index + ". "));
		hbox.setWidth("100%");
		// Rapikan: foto & info kehadiran rata ATAS dan rata KIRI (sebelumnya terkesan ter-tengah).
		hbox.setAlign("top");
		hbox.setStyle("align-items:flex-start; text-align:left;");
		hbox.setParent(group);

		if (Common.bolehKonfigurasi("tampilkan_foto_di_absensi_kehadiran")) {
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);
		}

		Vbox vboxStats = new Vbox();
		vboxStats.setParent(hbox);
		// Konten status/keterangan rata KIRI & memenuhi sisa lebar agar sejajar antar kartu.
		vboxStats.setAlign("start");
		vboxStats.setHflex("1");
		vboxStats.setStyle("text-align:left; align-items:flex-start;");
		vboxStats.appendChild(hboxStatus);

		if (pengajuanIzinTidakMasukPerkuliahan != null && pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
			new MyLabelKecil(pengajuanIzinTidakMasukPerkuliahan.getKeterangan()).setParent(vboxStats);
		} else if ((tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) || mahasiswaBolehUbahAbsen) {

			Vbox hbox1 = new Vbox();
			hbox1.setWidth("100%");
			// Keterangan & tombol Ubah rata KIRI agar rapi & sejajar.
			hbox1.setAlign("start");
			hbox1.setStyle("text-align:left; align-items:flex-start;");
			hbox1.setParent(vboxStats);

			keterangan.setVisible(false);
			keterangan.setCols(50);
			keterangan.setRows(4);
			keterangan.setParent(hbox1);
			keterangan.addEventListener("onChange", eventListener);

			// =========================================================================
			// PERBAIKAN FORMAT HTML: URL Formatting yang tidak Overlaping (Bertumpuk)
			// =========================================================================
			String catat = formatTextToHtmlSafe(ket);

			final MyHtml ketComp = new MyHtml(
					"<div style='font-size:11px;'><u>Keterangan</u>:</div><div style='font-size:10px;'>"
							+ (catat == null || catat.trim().isEmpty() ? "Tidak/belum ada keterangan" : catat)
							+ "</div>");
			ketComp.setParent(hbox1);

			final MyToolbarbuttonConfig buttonSelesai = new MyToolbarbuttonConfig("Selesai", "/img/save.gif");
			final MyToolbarbuttonConfig buttonUbah = new MyToolbarbuttonConfig("Ubah Keterangan", "/img/edit-icon.png");

			buttonSelesai.setTooltiptext("Simpan Data");
			buttonSelesai.setVisible(false);
			buttonSelesai.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					String catatEvent = formatTextToHtmlSafe(keterangan.getValue().trim());

					buttonSelesai.setVisible(false);
					buttonUbah.setVisible(true);

					keterangan.setVisible(false);
					ketComp.setContent(
							"<div style='font-size:11px;'><u>Keterangan</u>:</div><div style='font-size:10px;'>"
									+ (catatEvent == null || catatEvent.trim().isEmpty() ? "Tidak/belum ada keterangan"
											: catatEvent)
									+ "</div>");
					ketComp.setVisible(true);
				}

			});
			buttonSelesai.setParent(hbox1);

			buttonUbah.setTooltiptext("Ubah Data");
			buttonUbah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					buttonSelesai.setVisible(true);
					buttonUbah.setVisible(false);
					keterangan.setVisible(true);
					ketComp.setVisible(false);
				}

			});
			buttonUbah.setParent(hbox1);

		} else {
			String catat = formatTextToHtmlSafe(ket);

			MyHtml ketComp = new MyHtml(
					"<div style='font-size:11px;'><u>Keterangan</u>:</div><div style='font-size:10px;'>"
							+ (catat == null || catat.trim().isEmpty() ? "Tidak/belum ada keterangan" : catat)
							+ "</div>");
			ketComp.setParent(vboxStats);
		}

		Box box = mobile ? new Vbox() : new Hbox();
		box.setWidth("90%");
		box.setParent(group);
		List<String> urls = Common.getUrls(ket);
		for (String u : urls) {
			if (u.contains("iframe")) {
				MyHtml myHtml = new MyHtml(u);
				box.appendChild(myHtml);
			} else if (u.contains("maps")) {
				MyHtml myHtml = new MyHtml(
						"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
								+ u + "&amp;output=embed\"></iframe>");
				box.appendChild(myHtml);
			} else if (ket.toLowerCase().contains("video") && u.contains("download")) {
				String contentVideo = org.apache.commons.lang3.StringUtils.replace(u,
						"https://drive.google.com/uc?download=view&id=", "");
				contentVideo = org.apache.commons.lang3.StringUtils.split(contentVideo, "&")[0];
				contentVideo = org.apache.commons.lang3.StringUtils.split(contentVideo, "/")[0];
				Html html = new Html("<iframe src=\"https://drive.google.com/file/d/" + contentVideo
						+ "/preview\" style=\"width:100%;height:200px\" frameborder=\"0\" marginheight=\"0\"  marginwidth=\"0\"></iframe>");
				html.setParent(box);

			} else if (u.contains("download") || u.contains("AmbilLampiran")) {
				MyHtml myHtml = new MyHtml("<a onclick=\"popupCenter({url: '" + u
						+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\"" + u
						+ "\"></image></a>");
				box.appendChild(myHtml);
			}
		}

		try {
			if (perkuliahan != null && (mahasiswa instanceof Mahasiswa)
					&& Common.bolehKonfigurasi("mahasiswa_yang_belum_membayar_tidak_bisa_absen_perkuliahan", Konfigurasi.TIDAK_AKTIF)) {

				Long detailperkuliahanid = perkuliahan.ambilDetailperkuliahan((Mahasiswa) mahasiswa);
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) (detailperkuliahanid == null ? null
						: GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString()));

				if (!Common.checkStatusPembayaranMahasiswa(
						detailperkuliahan == null ? semester : detailperkuliahan.getSemester(), tahap,
						(Mahasiswa) mahasiswa, false, false)) {
					kehadiran.setVisible(false);
					MyLabelAgakKecil a = new MyLabelAgakKecil(
							"Mahasiswa tidak bisa di-absen karena belum memenuhi kewajiban pembayaran di semester "
									+ (detailperkuliahan == null ? semester : detailperkuliahan.getSemester()));
					// kehadiran hanya di-attach ke parent pada cabang mahasiswa boleh ubah absen
					// sendiri (lihat hbox.appendChild(kehadiran) di atas); pada tampilan
					// read-only (mis. mahasiswa lihat absensi sendiri tanpa hak ubah) kehadiran
					// TIDAK PERNAH di-attach sehingga getParent() null -> jatuhkan ke vboxStats
					// (kontainer baris ini, selalu sudah ter-attach) agar tidak NPE.
					if (kehadiran.getParent() != null) {
						kehadiran.getParent().appendChild(a);
					} else {
						a.setParent(vboxStats);
					}
					a.setStyle("font-size:10px;color:red;");
				}

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4654");
		}
	}

	/**
	 * HELPER METHOD UNTUK MENANGANI REPLACE HTML DENGAN AMAN
	 */
	private String formatTextToHtmlSafe(String input) {
		if (input == null || input.trim().isEmpty()) {
			return "";
		}

		String result = input;

		// 1. Terapkan spasi agar pemisahan URL lebih jelas
		result = org.apache.commons.lang3.StringUtils.replace(result, "target=\"_blank\">", " ");

		// 2. Terapkan penggantian baris baru
		result = result.replaceAll("\n", "<br>");

		// 3. Kita ubah DULU regex URL HTTP secara general
		// Negative Lookahead agar kita tidak me-replace HTTP yang SUDAH di dalam
		// struktur HTML.
		result = result.replaceAll(
				"(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))",
				"<a target=\"_blank\" href=\"$1\">" + Common.getBahasaConfig("Klik di sini") + "</a>");

		// 4. Setelah digenerate tag A nya, baru kita tangani masalah URL Download yg
		// mengandung _,_
		List<String> urls = Common.getUrls(result);
		for (String url : urls) {
			if (url.contains("download")) {
				String cleanUrl = org.apache.commons.lang3.StringUtils.replace(url, "_", ",");
				result = org.apache.commons.lang3.StringUtils.replace(result, url, cleanUrl);
			}
		}

		return result;
	}

	class MahasiswaIzinRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser;
		private Pertemuan pertemuan;

		public MahasiswaIzinRenderer(Pertemuan pertemuan) {
			tbmuser = Common.getCurrentUser();
			this.pertemuan = pertemuan;
		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) arg1;
			arg0.setValign("top");
			if (tbmuser.getMahasiswa() == null || mahasiswaBolehUbahAbsen) {
				final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Setujui");
				checkboxConfig.setChecked(pengajuanIzinTidakMasukPerkuliahan.getDiizinkan());
				checkboxConfig.setParent(arg0);
				checkboxConfig.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pengajuanIzinTidakMasukPerkuliahan.setDiizinkan(checkboxConfig.isChecked());
						Common.refreshSaveOrUpdate(pengajuanIzinTidakMasukPerkuliahan);

						pertemuan.populate(pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getId(),
								pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi(),
								pengajuanIzinTidakMasukPerkuliahan.getKeterangan(), null, pertemuan.getWaktuMulai(),
								pertemuan.getWaktuSelesai(), "Mahasiswa");
						sesuaikan(pertemuan, false);
						Common.refreshSaveOrUpdate(pertemuan);

						reload(pertemuan);
					}
				});
			} else {
				new MyLabelConfig(pengajuanIzinTidakMasukPerkuliahan.getDiizinkan() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(pengajuanIzinTidakMasukPerkuliahan.getMahasiswa()).setParent(hbox);

			Vbox vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(hbox);
			new Label(pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getNim() + " - "
					+ pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getNama()).setParent(vbox);

			vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(arg0);

			new Label("Status : "
					+ Common.getBahasaConfig(pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi().getNama()))
					.setParent(vbox);

			new Label(pengajuanIzinTidakMasukPerkuliahan.getKeterangan()).setParent(vbox);

			Vbox myVbox = new Vbox();
			myVbox.setParent(vbox);

			hbox = new Hbox();
			hbox.setParent(myVbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pengajuanIzinTidakMasukPerkuliahan.getId(),
					LampiranLain.IZIN_TIDAK_MASUK, "Lampiran", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false);

			Hbox tombol = new Hbox();
			tombol.setParent(vbox);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(!pengajuanIzinTidakMasukPerkuliahan.getDiizinkan() && ((tbmuser.getMahasiswa() != null
					&& tbmuser.getMahasiswa().getId().equals(pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getId()))
					|| tbmuser.getMahasiswa() == null || mahasiswaBolehUbahAbsen));
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
											Common.refreshDelete(pengajuanIzinTidakMasukPerkuliahan);
											reload(pertemuan);
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
	}

	public static Component createStatusKehadiran(final Mahasiswa asisten, final Pertemuan pertemuan,
			Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa, final EventListener sesuaikan,
			boolean terlewat) {
		if (asisten == null) {
			return new Label();
		}

		Statusabsensi statusabsensi = null;

		if (pertemuan.getId() != null) {

			statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					pertemuan.retreiveAbsensiId(asisten.getId()));
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null && (!pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() || !terlewat)) {

			Vbox vbox = new Vbox();

			Hbox hbox = new Hbox();
			final Timebox waktuMulai = new ais.ui.util.MyTimebox();
			final Timebox waktuSelesai = new ais.ui.util.MyTimebox();

			final Textbox catatan = new Textbox(pertemuan.retreiveAbsensiKeterangan(asisten.getId()));
			catatan.setWidth("120px");
			catatan.setRows(2);

			final Combobox kehadiranAsisten = new Combobox();
			kehadiranAsisten.setWidth("120px");
			kehadiranAsisten.setReadonly(true);
			Common.insertComboMyConfig(kehadiranAsisten, "nama", Statusabsensi.class,
					Restrictions.not(Restrictions.or(Restrictions.ilike("nama", "belajar", MatchMode.ANYWHERE),
							Restrictions.or(Restrictions.ilike("nama", "cuti", MatchMode.ANYWHERE),
									Restrictions.ilike("nama", "dinas", MatchMode.ANYWHERE)))));
			Common.selectComboItem(kehadiranAsisten, statusabsensi);
			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (kehadiranAsisten.getSelectedItem() == null) {
						return;
					}
					if (pertemuan.getId() != null) {
						HibernateUtil.currentSession().refresh(pertemuan);
					}
					Statusabsensi statusabsensi = (Statusabsensi) kehadiranAsisten.getSelectedItem().getValue();
					if (statusabsensi.getKode() != null && statusabsensi.getKode().trim().equals("M")) {

						if (waktuMulai.getValue() == null) {
							try {
								waktuMulai.setValue(
										pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
												? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4869");

							}
						}
						if (waktuSelesai.getValue() == null) {
							try {
								waktuSelesai.setValue(pertemuan.getWaktuSelesai() == null
										|| pertemuan.getWaktuSelesai().trim().isEmpty() ? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4878");

							}
						}
					} else {
						waktuMulai.setValue(null);
						waktuSelesai.setValue(null);
					}
					pertemuan.populate(asisten.getId(), statusabsensi,
							waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
							waktuSelesai.getValue() == null ? ""
									: Common.timeFormat2.get().format(waktuSelesai.getValue()),
							"Asisten");
					sesuaikan.onEvent(new Event("", null, pertemuan));
					Common.refreshUpdate(pertemuan);
				}
			};
			kehadiranAsisten.addEventListener("onChange", eventListener);
			kehadiranAsisten.setParent(vbox);

			new Label(ais.common.Common.getBahasaConfig("Catatan:")).setParent(vbox);
			catatan.setParent(vbox);

			hbox.appendChild(waktuMulai);
			waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				waktuMulai.setValue(Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(asisten.getId())));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4905");

			}

			hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
			hbox.appendChild(waktuSelesai);
			waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				waktuSelesai.setValue(Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(asisten.getId())));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4914");

			}

			hbox.setParent(vbox);

			kehadiranAsisten.addEventListener("onChange", eventListener);
			waktuMulai.addEventListener("onChange", eventListener);
			waktuSelesai.addEventListener("onChange", eventListener);
			catatan.addEventListener("onChange", eventListener);
			waktuMulai.setCols(2);
			waktuSelesai.setCols(2);
			return vbox;
		} else {
			return new Label(statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama()));
		}

	}

	public static Component boleh(Statusabsensi statusabsensi, final Pertemuan pertemuan, final Dosen dosen,
			final MyDatebox tanggalRealisasi, final EventListener sesuaikan) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		String ket = pertemuan.retreiveAbsensiKeterangan(dosen.getId());
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
		final EventListener ubahRealisasi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggalRealisasi != null) {

					if (Common.bolehKonfigurasi("tanggal_realisasi_perkuliahan_harus_diisi_sesuai_pertemuan_perkuliahan")) {
						tanggalRealisasi.setDisabled(!pertemuan.apakahAdaDosenYangMasuk());

						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& (pertemuan.getPerkuliahan() != null
										&& !pertemuan.getPerkuliahan().getDosenBisaMerubahTanggalPerkuliahan())) {
							tanggalRealisasi.setDisabled(true);
						}
					}

					if (tanggalRealisasi.isDisabled()) {
						tanggalRealisasi.setValue(null);
					} else {
						if (tanggalRealisasi.getValue() == null) {
							tanggalRealisasi.setValue(pertemuan.getTanggal());
						}
					}
				}

			}
		};

		if ((tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& (pertemuan.getPerkuliahan() != null
						&& !pertemuan.getPerkuliahan().getDosenBisaMerubahTanggalPerkuliahan()))
				|| (tbmuser != null && tbmuser.ambilDosen() != null
						&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
						&& Common.bolehKonfigurasi("dosen_wajib_menggunakan_tombol_start_stop_di_absensi", Konfigurasi.TIDAK_AKTIF))) {
			Vbox vbox = new Vbox();

			final Textbox catatan = new Textbox(ket);
			catatan.setWidth("120px");
			catatan.setRows(2);

			Date m = null;
			try {
				String mulaiStr = pertemuan.retreiveAbsensiMulai(dosen.getId());
				if (mulaiStr != null && !mulaiStr.trim().isEmpty()) {
					m = Common.timeFormat2.get().parse(mulaiStr);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4982");
			}

			Date s = null;
			try {
				String sampaiStr = pertemuan.retreiveAbsensiSampai(dosen.getId());
				if (sampaiStr != null && !sampaiStr.trim().isEmpty()) {
					s = Common.timeFormat2.get().parse(sampaiStr);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:4988");
			}

			final Button masuk = new Button("Klik Tombol ini jika Anda mulai mengajar", "/img/Start-icon.png");
			if (m != null) {
				masuk.setLabel("Mulai mengajar " + Common.timeFormat2.get().format(m));
				masuk.setDisabled(true);
			}

			final MyCheckboxConfig selesaikanOtomatis = new MyCheckboxConfig(
					"Selesaikan jam mengajar otomatis sesuai rencana perkuliahan, yaitu pukul "
							+ pertemuan.getPerkuliahan().getWaktuSelesaiD());

			final Button keluar = new Button("Klik Tombol ini jika Anda selesai mengajar", "/img/Stop-icon.png");
			keluar.setVisible(m != null);
			if (s != null) {
				keluar.setLabel("Selesai mengajar " + Common.timeFormat2.get().format(s));
				keluar.setDisabled(true);
				selesaikanOtomatis.setVisible(false);
			}

			masuk.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah Anda mulai mengajar ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										if (pertemuan.getId() != null) {
											HibernateUtil.currentSession().refresh(pertemuan);
										}
										pertemuan.populate(dosen.getId(), ConstantValues.MASUK, catatan.getValue(),
												null, Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()),
												"", "Dosen");
										sesuaikan.onEvent(new Event("", null, pertemuan));
										Common.refreshUpdate(pertemuan);
										masuk.setDisabled(true);
										keluar.setVisible(true);

										Date m = null;
										try {
											m = Common.timeFormat2.get()
													.parse(pertemuan.retreiveAbsensiMulai(dosen.getId()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5037");
										}
										masuk.setLabel("Mulai mengajar " + Common.timeFormat2.get().format(m));
										selesaikanOtomatis.setVisible(m != null);

										ubahRealisasi.onEvent(event);
									}

								}
							});

				}
			});

			final Date mm = m;
			keluar.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah Anda selesai mengajar ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										if (pertemuan.getId() != null) {
											HibernateUtil.currentSession().refresh(pertemuan);
										}
										pertemuan.populate(dosen.getId(), ConstantValues.MASUK, catatan.getValue(),
												null,
												mm == null
														? Common.timeFormat2.get()
																.format(ais.ui.util.WaktuUtil.getDate())
														: Common.timeFormat2.get().format(mm),
												Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()),
												"Dosen");
										sesuaikan.onEvent(new Event("", null, pertemuan));
										Common.refreshUpdate(pertemuan);
										keluar.setDisabled(true);
										masuk.setDisabled(true);

										Date s = ais.ui.util.WaktuUtil.getDate();
										String waktuSampai = pertemuan.retreiveAbsensiSampai(dosen.getId());
										if (waktuSampai != null && waktuSampai.trim().length() > 0) {
											s = Common.timeFormat2.get().parse(waktuSampai);
										}
										keluar.setLabel("Selesai mengajar " + Common.timeFormat2.get().format(s));

										selesaikanOtomatis.setVisible(false);

										ubahRealisasi.onEvent(event);
									}

								}
							});
				}
			});

			selesaikanOtomatis.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Apakah Anda ingin menyelesaikan jam mengajar otomatis sesuai rencana perkuliahan ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										if (pertemuan.getId() != null) {
											HibernateUtil.currentSession().refresh(pertemuan);
										}
										Date m = ais.ui.util.WaktuUtil.getDate();
										String waktuMulai = pertemuan.retreiveAbsensiMulai(dosen.getId());
										if (waktuMulai != null && waktuMulai.trim().length() > 0) {
											m = Common.timeFormat2.get().parse(waktuMulai);
										}

										String mulai = m == null
												? Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
												: Common.timeFormat2.get().format(m);
										String selesai = Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate());
										if (pertemuan.getPerkuliahan() != null
												&& pertemuan.getPerkuliahan().getWaktuSelesaiD() != null) {
											String waktuSelesaiRencana = pertemuan.getPerkuliahan().getWaktuSelesaiD().toString();
											if (waktuSelesaiRencana.length() >= 5) {
												selesai = waktuSelesaiRencana.substring(0, 5);
											}
										}

										System.out.println("mulai => " + mulai + ", selesai => " + selesai);

										pertemuan.populate(dosen.getId(), ConstantValues.MASUK, catatan.getValue(),
												null, mulai, selesai, "Dosen");
										sesuaikan.onEvent(new Event("", null, pertemuan));
										Common.refreshUpdate(pertemuan);
										keluar.setDisabled(true);
										masuk.setDisabled(true);
										keluar.setVisible(true);
										masuk.setVisible(true);
										selesaikanOtomatis.setDisabled(true);

										Date s = ais.ui.util.WaktuUtil.getDate();
										String waktuSampai = pertemuan.retreiveAbsensiSampai(dosen.getId());
										if (waktuSampai != null && waktuSampai.trim().length() > 0) {
											s = Common.timeFormat2.get().parse(waktuSampai);
										}
										keluar.setLabel("Selesai mengajar " + Common.timeFormat2.get().format(s));
										masuk.setLabel("Mulai mengajar " + Common.timeFormat2.get().format(m));

										ubahRealisasi.onEvent(event);
									}

								}
							});
				}
			});

			vbox.appendChild(masuk);
			vbox.appendChild(keluar);
			vbox.appendChild(selesaikanOtomatis);

			vbox.appendChild(catatan);

			catatan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (pertemuan.getId() != null) {
						HibernateUtil.currentSession().refresh(pertemuan);
					}
					Date m = null;
					try {
						m = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(dosen.getId()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5180");
					}

					Date s = null;
					try {
						s = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(dosen.getId()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5186");
					}

					pertemuan.populate(dosen.getId(), ConstantValues.MASUK, catatan.getValue(), null,
							m == null ? "" : Common.timeFormat2.get().format(m),
							s == null ? "" : Common.timeFormat2.get().format(s), "Dosen");
					sesuaikan.onEvent(new Event("", null, pertemuan));
					Common.refreshUpdate(pertemuan);

					ubahRealisasi.onEvent(arg0);
				}
			});

			if (dosen != null && !dosen.getId().equals(tbmuser.getDosen().getId())) {
				Common.freeze(vbox, true);
			}

			try {
				ubahRealisasi.onEvent(null);
			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:5206");
			}

			List<String> urls = Common.getUrls(ket);
			for (String u : urls) {
				if (u.contains("iframe")) {
					MyHtml myHtml = new MyHtml(u);
					vbox.appendChild(myHtml);
				} else if (u.contains("maps")) {
					MyHtml myHtml = new MyHtml(
							"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
									+ u + "&amp;output=embed\"></iframe>");
					vbox.appendChild(myHtml);
				} else if (u.contains("download") || u.contains("AmbilLampiran")) {
					MyHtml myHtml = new MyHtml("<a onclick=\"popupCenter({url: '" + u
							+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\"" + u
							+ "\"></image></a>");
					vbox.appendChild(myHtml);
				}
			}

			return vbox;
		} else {

			Vbox vbox = new Vbox();

			Hbox hbox = new Hbox();
			final Timebox waktuMulai = new ais.ui.util.MyTimebox();
			final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
			waktuMulai.setCols(2);
			waktuSelesai.setCols(2);

			final Textbox catatan = new Textbox(pertemuan.retreiveAbsensiKeterangan(dosen.getId()));
			catatan.setWidth("220px");
			catatan.setRows(2);

			final Combobox kehadiran = new Combobox();
			kehadiran.setWidth("120px");
			kehadiran.setReadonly(true);
			Common.insertComboMyConfig(kehadiran, "nama", Statusabsensi.class);
			Common.selectComboItem(kehadiran, statusabsensi);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (kehadiran.getSelectedItem() == null) {
						return;
					}
					if (pertemuan.getId() != null) {
						try {
							HibernateUtil.currentSession().refresh(pertemuan);
						} catch (org.hibernate.UnresolvableObjectException uoe) { ais.common.ErrorAuditUtil.record(uoe, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5258");
							// Baris Pertemuan sudah dihapus sesi lain. Lanjut pakai data di memori.
						}
					}

					Statusabsensi statusabsensi = (Statusabsensi) kehadiran.getSelectedItem().getValue();

					if (statusabsensi.getKode() != null && statusabsensi.getKode().trim().equals("M")) {

						if (waktuMulai.getValue() == null) {
							try {
								waktuMulai.setValue(
										pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
												? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5273");

							}
						}
						if (waktuSelesai.getValue() == null) {
							try {
								waktuSelesai.setValue(pertemuan.getWaktuSelesai() == null
										|| pertemuan.getWaktuSelesai().trim().isEmpty() ? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5282");

							}
						}
					} else {
						waktuMulai.setValue(null);
						waktuSelesai.setValue(null);
					}

					pertemuan.populate(dosen.getId(), statusabsensi, catatan.getValue(), null,
							waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
							waktuSelesai.getValue() == null ? ""
									: Common.timeFormat2.get().format(waktuSelesai.getValue()),
							"Dosen");
					sesuaikan.onEvent(new Event("", null, pertemuan));
					Common.refreshUpdate(pertemuan);

					ubahRealisasi.onEvent(arg0);
				}
			};

			kehadiran.setParent(vbox);
			new Label(ais.common.Common.getBahasaConfig("Catatan:")).setParent(vbox);
			catatan.setParent(vbox);

			hbox.appendChild(new ais.ui.util.MyLabelConfig("Wkt :"));
			hbox.appendChild(waktuMulai);
			waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				String absensiMulai = pertemuan.retreiveAbsensiMulai(dosen.getId());
				waktuMulai.setValue(absensiMulai == null || absensiMulai.trim().isEmpty() ? null
						: Common.timeFormat2.get().parse(absensiMulai));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5312");

			}

			hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
			hbox.appendChild(waktuSelesai);
			waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				String absensiSampai = pertemuan.retreiveAbsensiSampai(dosen.getId());
				waktuSelesai.setValue(absensiSampai == null || absensiSampai.trim().isEmpty() ? null
						: Common.timeFormat2.get().parse(absensiSampai));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5321");

			}

			waktuMulai.addEventListener("onChange", eventListener);
			waktuSelesai.addEventListener("onChange", eventListener);
			catatan.addEventListener("onChange", eventListener);
			kehadiran.addEventListener("onChange", eventListener);

			hbox.setParent(vbox);

			try {
				ubahRealisasi.onEvent(null);

			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:5336");
			}

			List<String> urls = Common.getUrls(ket);
			for (String u : urls) {
				if (u.contains("iframe")) {
					MyHtml myHtml = new MyHtml(u);
					vbox.appendChild(myHtml);
				} else if (u.contains("maps")) {
					MyHtml myHtml = new MyHtml(
							"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
									+ u + "&amp;output=embed\"></iframe>");
					vbox.appendChild(myHtml);
				} else if (u.contains("download") || u.contains("AmbilLampiran")) {
					MyHtml myHtml = new MyHtml("<a onclick=\"popupCenter({url: '" + u
							+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\"" + u
							+ "\"></image></a>");
					vbox.appendChild(myHtml);
				}
			}

			return vbox;
		}
	}

	public static Component bolehKonfirmasi(final Dosen dosen, final Pertemuan pertemuan, final Mahasiswa mahasiswa) {

		Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
				pertemuan.retreiveAbsensiIdKonfirmasi(mahasiswa.getId(), dosen));
		if (statusabsensi == null) {
			statusabsensi = ConstantValues.BELUM_ABSEN;
		}

		String ket = pertemuan.retreiveAbsensiKeteranganKonfirmasi(mahasiswa.getId(), dosen);
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

		Vbox vbox = new Vbox();
		Hbox hbox = new Hbox();
		final Timebox waktuMulai = new ais.ui.util.MyTimebox();
		final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
		waktuMulai.setCols(2);
		waktuSelesai.setCols(2);

		final Textbox catatan = new Textbox(pertemuan.retreiveAbsensiKeteranganKonfirmasi(mahasiswa.getId(), dosen));
		catatan.setWidth("220px");
		catatan.setRows(2);

		final Combobox kehadiran = new Combobox();
		kehadiran.setWidth("82px");
		Common.insertComboItemsMyConfig(kehadiran, "nama", ConstantValues.listAbsenMahasiswa);
		Common.selectComboItem(kehadiran, statusabsensi);
		kehadiran.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kehadiran.getSelectedItem() == null) {
					return;
				}

				Session session = HibernateUtil.currentSession();
				if (pertemuan.getId() != null) {
					session.refresh(pertemuan);
				}

				Statusabsensi statusabsensi = (Statusabsensi) kehadiran.getSelectedItem().getValue();

				if (statusabsensi.getKode() != null && statusabsensi.getKode().trim().equals("M")) {

					if (waktuMulai.getValue() == null) {
						try {
							waktuMulai.setValue(
									pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5412");

						}
					}
					if (waktuSelesai.getValue() == null) {
						try {
							waktuSelesai.setValue(
									pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5422");

						}
					}

					waktuMulai.setDisabled(false);
					waktuSelesai.setDisabled(false);
				} else {
					waktuMulai.setValue(null);
					waktuSelesai.setValue(null);

					waktuMulai.setDisabled(true);
					waktuSelesai.setDisabled(true);
				}

				pertemuan.populateKonfirmasi(mahasiswa.getId(), statusabsensi, catatan.getValue(),
						waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
						waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
						"Mahasiswa", dosen);
				Common.refreshUpdate(session, pertemuan);
				session.flush();

			}
		};

		kehadiran.setParent(vbox);
		new Label(ais.common.Common.getBahasaConfig("Catatan:")).setParent(vbox);
		catatan.setParent(vbox);

		hbox.appendChild(new ais.ui.util.MyLabelConfig("Wkt :"));
		hbox.appendChild(waktuMulai);
		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			String waktuMulaiKonfirmasi = pertemuan.retreiveAbsensiMulaiKonfirmasi(mahasiswa.getId(), dosen);
			waktuMulai.setValue(
					waktuMulaiKonfirmasi == null || waktuMulaiKonfirmasi.trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(waktuMulaiKonfirmasi));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5457");

		}

		hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
		hbox.appendChild(waktuSelesai);
		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			String waktuSelesaiKonfirmasi = pertemuan.retreiveAbsensiSampaiKonfirmasi(mahasiswa.getId(), dosen);
			waktuSelesai.setValue(
					waktuSelesaiKonfirmasi == null || waktuSelesaiKonfirmasi.trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(waktuSelesaiKonfirmasi));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5467");

		}

		waktuMulai.addEventListener("onChange", eventListener);
		waktuSelesai.addEventListener("onChange", eventListener);
		catatan.addEventListener("onChange", eventListener);
		kehadiran.addEventListener("onChange", eventListener);

		if (statusabsensi.getKode() != null && statusabsensi.getKode().trim().equals("M")) {
			waktuMulai.setDisabled(false);
			waktuSelesai.setDisabled(false);
		} else {
			waktuMulai.setDisabled(true);
			waktuSelesai.setDisabled(true);
		}
		hbox.setParent(vbox);

		return vbox;

	}

	public static Component bolehKonfirmasiRps(final Dosen dosen, final Pertemuan pertemuan,
			final Mahasiswa mahasiswa) {

		Long status = pertemuan.retreiveAbsensiIdKonfirmasiRps(mahasiswa.getId(), dosen);
		if (status == null) {
			status = 0L;
		}

		String ket = pertemuan.retreiveAbsensiKeteranganSesuaiDenganRps(mahasiswa.getId(), dosen);
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

		Vbox vbox = new Vbox();
		Hbox hbox = new Hbox();
		final Timebox waktuMulai = new ais.ui.util.MyTimebox();
		final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
		waktuMulai.setCols(2);
		waktuSelesai.setCols(2);

		final Textbox catatan = new Textbox(
				pertemuan.retreiveAbsensiKeteranganSesuaiDenganRps(mahasiswa.getId(), dosen));
		catatan.setWidth("220px");
		catatan.setRows(2);

		final Combobox kehadiran = new Combobox();
		kehadiran.setWidth("82px");

		Comboitem comboitem = new Comboitem("Belum Ditentukan");
		comboitem.setValue(0L);
		kehadiran.appendChild(comboitem);

		comboitem = new Comboitem("Sesuai");
		comboitem.setValue(1L);
		kehadiran.appendChild(comboitem);

		comboitem = new Comboitem("Tidak Sesuai");
		comboitem.setValue(2L);
		kehadiran.appendChild(comboitem);

		Common.selectComboItem(kehadiran, status);
		kehadiran.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kehadiran.getSelectedItem() == null) {
					return;
				}

				Session session = HibernateUtil.currentSession();
				if (pertemuan.getId() != null) {
					session.refresh(pertemuan);
				}

				Long status = (Long) kehadiran.getSelectedItem().getValue();

				if (status != null && status.equals(1L)) {

					if (waktuMulai.getValue() == null) {
						try {
							waktuMulai.setValue(
									pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5553");

						}
					}
					if (waktuSelesai.getValue() == null) {
						try {
							waktuSelesai.setValue(
									pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5563");

						}
					}

					waktuMulai.setDisabled(false);
					waktuSelesai.setDisabled(false);
				} else {
					waktuMulai.setValue(null);
					waktuSelesai.setValue(null);

					waktuMulai.setDisabled(true);
					waktuSelesai.setDisabled(true);
				}

				pertemuan.populateKonfirmasiRps(mahasiswa.getId(), status, catatan.getValue(),
						waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
						waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
						"Mahasiswa", dosen);
				Common.refreshUpdate(session, pertemuan);
				session.flush();

			}
		};

		kehadiran.setParent(vbox);
		new Label(ais.common.Common.getBahasaConfig("Catatan:")).setParent(vbox);
		catatan.setParent(vbox);

		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
		// FIX: mahasiswa yang belum mengisi konfirmasi RPS wajar mengembalikan string
		// kosong dari retreiveAbsensiMulaiKonfirmasiRps() -> jangan panggil parse() utk
		// kasus normal ini (dulu selalu melempar+menangkap ParseException tiap render).
		String waktuMulaiStr = pertemuan.retreiveAbsensiMulaiKonfirmasiRps(mahasiswa.getId(), dosen);
		if (waktuMulaiStr != null && waktuMulaiStr.trim().length() > 0) {
			try {
				waktuMulai.setValue(Common.timeFormat2.get().parse(waktuMulaiStr));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5596");

			}
		}

		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
		String waktuSelesaiStr = pertemuan.retreiveAbsensiSampaiKonfirmasiRps(mahasiswa.getId(), dosen);
		if (waktuSelesaiStr != null && waktuSelesaiStr.trim().length() > 0) {
			try {
				waktuSelesai.setValue(Common.timeFormat2.get().parse(waktuSelesaiStr));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5604");

			}
		}

		waktuMulai.addEventListener("onChange", eventListener);
		waktuSelesai.addEventListener("onChange", eventListener);
		catatan.addEventListener("onChange", eventListener);
		kehadiran.addEventListener("onChange", eventListener);

		hbox.setParent(vbox);

		return vbox;

	}

	public static Component bolehOlehAkademik(final Dosen dosen, final Pertemuan pertemuan, final Tbmuser tbmuser) {

		Long status = pertemuan.retreiveAbsensiIdOlehAkademik(tbmuser.getUserId(), dosen);
		if (status == null) {
			status = 0L;
		}

		String ket = pertemuan.retreiveAbsensiKeteranganSesuaiOlehAkademik(tbmuser.getUserId(), dosen);
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

		Vbox vbox = new Vbox();
		Hbox hbox = new Hbox();
		final Timebox waktuMulai = new ais.ui.util.MyTimebox();
		final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
		waktuMulai.setCols(2);
		waktuSelesai.setCols(2);

		final Textbox catatan = new Textbox(
				pertemuan.retreiveAbsensiKeteranganSesuaiOlehAkademik(tbmuser.getUserId(), dosen));
		catatan.setWidth("220px");
		catatan.setRows(2);

		final Combobox kehadiran = new Combobox();
		kehadiran.setWidth("82px");

		Comboitem comboitem = new Comboitem("Belum Ditentukan");
		comboitem.setValue(0L);
		kehadiran.appendChild(comboitem);

		comboitem = new Comboitem("Sesuai");
		comboitem.setValue(1L);
		kehadiran.appendChild(comboitem);

		comboitem = new Comboitem("Tidak Sesuai");
		comboitem.setValue(2L);
		kehadiran.appendChild(comboitem);

		Common.selectComboItem(kehadiran, status);
		kehadiran.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kehadiran.getSelectedItem() == null) {
					return;
				}

				Session session = HibernateUtil.currentSession();
				if (pertemuan.getId() != null) {
					session.refresh(pertemuan);
				}

				Long status = (Long) kehadiran.getSelectedItem().getValue();

				if (status != null && status.equals(1L)) {

					if (waktuMulai.getValue() == null) {
						try {
							waktuMulai.setValue(
									pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5682");

						}
					}
					if (waktuSelesai.getValue() == null) {
						try {
							waktuSelesai.setValue(
									pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5692");

						}
					}

					waktuMulai.setDisabled(false);
					waktuSelesai.setDisabled(false);
				} else {
					waktuMulai.setValue(null);
					waktuSelesai.setValue(null);

					waktuMulai.setDisabled(true);
					waktuSelesai.setDisabled(true);
				}

				pertemuan.populateOlehAkademik(tbmuser.getUserId(), status, catatan.getValue(),
						waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
						waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
						"Admin", dosen);
				Common.refreshUpdate(session, pertemuan);
				session.flush();

			}
		};

		kehadiran.setParent(vbox);
		new Label(ais.common.Common.getBahasaConfig("Catatan:")).setParent(vbox);
		catatan.setParent(vbox);

		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			// FIX ParseException "": string kosong/null (belum diisi akademik) BUKAN error,
			// jangan diteruskan ke DateFormat.parse -- lewati saja, waktuMulai tetap null.
			String waktuMulaiOlehAkademik = pertemuan.retreiveAbsensiMulaiOlehAkademik(tbmuser.getUserId(), dosen);
			if (waktuMulaiOlehAkademik != null && !waktuMulaiOlehAkademik.trim().isEmpty()) {
				waktuMulai.setValue(Common.timeFormat2.get().parse(waktuMulaiOlehAkademik));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5725");

		}

		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			String waktuSelesaiOlehAkademik = pertemuan.retreiveAbsensiSampaiOlehAkademik(tbmuser.getUserId(), dosen);
			if (waktuSelesaiOlehAkademik != null && !waktuSelesaiOlehAkademik.trim().isEmpty()) {
				waktuSelesai.setValue(Common.timeFormat2.get().parse(waktuSelesaiOlehAkademik));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:5733");

		}

		waktuMulai.addEventListener("onChange", eventListener);
		waktuSelesai.addEventListener("onChange", eventListener);
		catatan.addEventListener("onChange", eventListener);
		kehadiran.addEventListener("onChange", eventListener);

		hbox.setParent(vbox);

		return vbox;

	}

	public static Component createStatusKehadiranData(Collection<CommonVO> dataDosen, final Pertemuan pertemuan)
			throws Exception {
		Collection<Dosen> collection = new ArrayList<Dosen>();
		for (CommonVO commonVO : dataDosen) {
			Dosen dosen = (Dosen) commonVO.getValueObject();
			collection.add(dosen);
		}
		Component d = createStatusKehadiran(collection, pertemuan);
		collection = null;
		return d;
	}

	public static Component createStatusKehadiran(Collection<Dosen> dosens, final Pertemuan pertemuan)
			throws Exception {
		if (dosens.isEmpty()) {
			return new Label();
		}

		Hbox hbox = new Hbox();

		if (dosens.size() > 3) {

			Vbox vboxBaru = new Vbox();
			vboxBaru.setParent(hbox);

			Hbox hboxBaru = new Hbox();
			hboxBaru.setParent(vboxBaru);
			int size = 0;

			for (Dosen dosen : dosens) {
				if (dosen != null && dosen.getId() != null) {

					if (size % 3 == 0) {
						hboxBaru = new Hbox();
						hboxBaru.setParent(vboxBaru);
					}
					size++;

					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(dosen.getId()));

					Vbox vbox1 = new Vbox();
					vbox1.setParent(hboxBaru);
					vbox1.appendChild(CommonMedia.tampilkanGambarKecil(dosen));
					vbox1.appendChild(new MyLabelAgakKecil(dosen.getNama()));
					vbox1.appendChild(new MyLabelAgakKecil(
							"Kehadiran : " + (statusabsensi == null || statusabsensi.getNama() == null ? "-"
									: Common.getBahasaConfig(statusabsensi.getNama()))));
					String wkt = pertemuan.retreiveAbsensiMulai(dosen.getId()) + " s.d "
							+ pertemuan.retreiveAbsensiSampai(dosen.getId());
					new MyLabelKecil(wkt.trim().equals("s.d") ? "" : "Pukul : " + wkt).setParent(vbox1);
				}
			}

		} else {
			for (Dosen dosen : dosens) {
				if (dosen != null && dosen.getId() != null) {

					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(dosen.getId()));

					Vbox vbox1 = new Vbox();
					vbox1.setParent(hbox);
					vbox1.appendChild(CommonMedia.tampilkanGambarKecil(dosen));
					vbox1.appendChild(new MyLabelAgakKecil(dosen.getNama()));
					vbox1.appendChild(new MyLabelAgakKecil(
							"Kehadiran : " + (statusabsensi == null || statusabsensi.getNama() == null ? "-"
									: Common.getBahasaConfig(statusabsensi.getNama()))));
					String wkt = pertemuan.retreiveAbsensiMulai(dosen.getId()) + " s.d "
							+ pertemuan.retreiveAbsensiSampai(dosen.getId());
					new MyLabelKecil(wkt.trim().equals("s.d") ? "" : "Pukul : " + wkt).setParent(vbox1);
				}
			}
		}

		if (pertemuan.getDosenPengganti() != null) {
			Dosen dsnPengganti = (Dosen) (pertemuan.getDosenPengganti() == null ? null
					: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getDosenPengganti()));
			if (dsnPengganti != null) {
				Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
						pertemuan.retreiveAbsensiId(dsnPengganti.getId()));

				Vbox vbox1 = new Vbox();
				vbox1.setParent(hbox);
				vbox1.appendChild(CommonMedia.tampilkanGambarKecil(dsnPengganti));
				vbox1.appendChild(new MyLabelAgakKecil("Dosen Pengganti :"));
				vbox1.appendChild(new MyLabelAgakKecil(dsnPengganti.getNama()));
				vbox1.appendChild(new MyLabelAgakKecil(
						"Kehadiran : " + (statusabsensi == null || statusabsensi.getNama() == null ? "-"
								: Common.getBahasaConfig(statusabsensi.getNama()))));
				String wkt = pertemuan.retreiveAbsensiMulai(dsnPengganti.getId()) + " s.d "
						+ pertemuan.retreiveAbsensiSampai(dsnPengganti.getId());
				new MyLabelKecil(wkt.trim().equals("s.d") ? "" : "Pukul : " + wkt).setParent(vbox1);
			}
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
				&& (pertemuan.getStatusPertemuan() != null
						&& (pertemuan.getStatusPertemuan().getNama().equalsIgnoreCase("UTS")
								|| pertemuan.getStatusPertemuan().getNama().equalsIgnoreCase("UAS")))) {
			Pegawai petugas = (Pegawai) (pertemuan.getPetugas() == null ? null
					: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas()));

			Pegawai petugas2 = (Pegawai) (pertemuan.getPetugas2() == null ? null
					: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas2()));

			Pegawai petugas3 = (Pegawai) (pertemuan.getPetugas3() == null ? null
					: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas3()));

			Pegawai petugas4 = (Pegawai) (pertemuan.getPetugas4() == null ? null
					: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas4()));

			Dosen pjawabDosen = (Dosen) (pertemuan.getPjDosen() == null ? null
					: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getPjDosen()));

			Vbox vbox = new Vbox();
			vbox.appendChild(new MyLabelConfig("Pengawas:"));
			vbox.setWidth("90%");
			hbox.appendChild(vbox);
			final AmbilDataPegawaiBanbox pegawai;
			vbox.appendChild(pegawai = new AmbilDataPegawaiBanbox(false));
			pegawai.setWidth("150px");
			pegawai.setAttribute("pegawai", petugas);
			pegawai.setValue(petugas == null ? null : petugas.getNama());
			pegawai.setReadonly(true);

			final AmbilDataPegawaiBanbox pegawai2;
			vbox.appendChild(pegawai2 = new AmbilDataPegawaiBanbox(false));
			pegawai2.setWidth("150px");
			pegawai2.setAttribute("pegawai", petugas2);
			pegawai2.setValue(petugas2 == null ? null : petugas2.getNama());
			pegawai2.setReadonly(true);

			final AmbilDataPegawaiBanbox pegawai3;
			vbox.appendChild(pegawai3 = new AmbilDataPegawaiBanbox(false));
			pegawai3.setWidth("150px");
			pegawai3.setAttribute("pegawai", petugas3);
			pegawai3.setValue(petugas3 == null ? null : petugas3.getNama());
			pegawai3.setReadonly(true);

			final AmbilDataPegawaiBanbox pegawai4;
			vbox.appendChild(pegawai4 = new AmbilDataPegawaiBanbox(false));
			pegawai4.setWidth("150px");
			pegawai4.setAttribute("pegawai", petugas4);
			pegawai4.setValue(petugas4 == null ? null : petugas4.getNama());
			pegawai4.setReadonly(true);

			pegawai.setDisabled(false);
			pegawai2.setDisabled(false);
			pegawai3.setDisabled(false);

			vbox = new Vbox();
			vbox.appendChild(new MyLabelConfig("Penanggungjawab Dosen:"));
			vbox.setWidth("90%");
			hbox.appendChild(vbox);

			final AmbilDataDosenBanbox pjDosen;
			hbox.appendChild(pjDosen = new AmbilDataDosenBanbox(false));
			pjDosen.setWidth("150px");
			pjDosen.setAttribute("dosen", pjawabDosen);
			pjDosen.setValue(pjawabDosen == null ? null : pjawabDosen.getNama());
			pjDosen.setReadonly(true);

			class PertemuanChangeListener implements EventListener {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Pegawai petugas = (Pegawai) pegawai.getAttribute("pegawai");
					Pegawai petugas2 = (Pegawai) pegawai2.getAttribute("pegawai");
					Pegawai petugas3 = (Pegawai) pegawai3.getAttribute("pegawai");
					Pegawai petugas4 = (Pegawai) pegawai4.getAttribute("pegawai");
					Dosen pjawabDosen = (Dosen) pjDosen.getAttribute("dosen");
					System.out.println("========= Ganti Waktu ujian =========");

					pertemuan.setPetugas(petugas == null ? null : petugas.getId());
					pertemuan.setPetugas2(petugas2 == null ? null : petugas2.getId());
					pertemuan.setPetugas3(petugas3 == null ? null : petugas3.getId());
					pertemuan.setPetugas4(petugas4 == null ? null : petugas4.getId());
					pertemuan.setPjDosen(pjawabDosen == null ? null : pjawabDosen.getId());

					HibernateUtil.currentSession().update(pertemuan);

				}

			}

			PertemuanChangeListener changeListener = new PertemuanChangeListener();

			pegawai.setEventListener(changeListener);
			pegawai2.setEventListener(changeListener);
			pegawai3.setEventListener(changeListener);
			pegawai4.setEventListener(changeListener);
		}

		return hbox;
	}

	public static Component createStatusKehadiran(final Dosen dosen, final Pertemuan pertemuan, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, final MyDatebox tanggalRealisasi,
			final EventListener sesuaikan, boolean terlewat) {
		if (dosen == null) {
			return new Label();
		}

		Statusabsensi statusabsensi = null;
		if (pertemuan.getId() != null) {

			statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					pertemuan.retreiveAbsensiId(dosen.getId()));

		}

		if (statusabsensi == null) {
			statusabsensi = ConstantValues.BELUM_ABSEN;
		}

		Date curreDate = ais.ui.util.WaktuUtil.getDate();
		Date mulai = null;
		Date selesai = null;
			try {
				if (pertemuan.getWaktuMulai() != null && pertemuan.getWaktuMulai().trim().length() > 0) {
					mulai = Common.timeFormat2.get().parse(pertemuan.getWaktuMulai());
				}
				if (pertemuan.getWaktuSelesai() != null && pertemuan.getWaktuSelesai().trim().length() > 0) {
					selesai = Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai());
				}
			} catch (Exception e) {
				// Jam pertemuan kosong/tidak valid: jangan blokir tampilan status kehadiran.
			}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (pertemuan != null && pertemuan.getPerkuliahan() != null) {

			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& pertemuan.getPerkuliahan().getKehadiranDosenHarusDiinputSesuaiJadwal()
					&& ((mulai != null && curreDate.before(mulai) || (selesai != null && curreDate.after(selesai))))) {
				Vbox vbox = new Vbox();
				vbox.appendChild(
						new Label(statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama())));

				String wkt = dosen == null ? ""
						: pertemuan.retreiveAbsensiMulai(dosen.getId()) + " s.d "
								+ pertemuan.retreiveAbsensiSampai(dosen.getId());
				vbox.appendChild(new Label(wkt.trim().equals("s.d") ? "" : wkt));
				return vbox;
			}
		}

		if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null && (!pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() || !terlewat)) {
			return boleh(statusabsensi, pertemuan, dosen, tanggalRealisasi, sesuaikan);
		} else {
			String ket = pertemuan.retreiveAbsensiKeterangan(dosen.getId());
			ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
			Vbox vbox = new Vbox();
			vbox.appendChild(new Label(statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama())));

			String wkt = dosen == null ? ""
					: pertemuan.retreiveAbsensiMulai(dosen.getId()) + " s.d "
							+ pertemuan.retreiveAbsensiSampai(dosen.getId());
			vbox.appendChild(new Label(wkt.trim().equals("s.d") ? "" : wkt));

			vbox.appendChild(new MyLabelAgakKecil(ket));

			List<String> urls = Common.getUrls(ket);
			for (String u : urls) {
				if (u.contains("iframe")) {
					MyHtml myHtml = new MyHtml(u);
					vbox.appendChild(myHtml);
				} else if (u.contains("maps")) {
					MyHtml myHtml = new MyHtml(
							"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
									+ u + "&amp;output=embed\"></iframe>");
					vbox.appendChild(myHtml);
				} else if (u.contains("download") || u.contains("AmbilLampiran")) {
					MyHtml myHtml = new MyHtml("<a onclick=\"popupCenter({url: '" + u
							+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\"" + u
							+ "\"></image></a>");
					vbox.appendChild(myHtml);
				}
			}

			return vbox;
		}

	}

	public static Component createStatusKehadiranKonfirmasi(Pertemuan pertemuan, Mahasiswa mahasiswa) {
		if (pertemuan == null) {
			return new Label();
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		String[] nilais = pertemuan.getKeteranganKonfirmasi().split(";");

		List<Dosen> dosens = pertemuan.ambilDosen();
		for (Dosen dosen : dosens) {

			MyFormRow rowAtas = new MyFormRow();
			rowAtas.setParent(rows);

			Groupbox groupbox = new Groupbox();
			groupbox.setParent(rowAtas);
			groupbox.setWidth("95%");
			groupbox.appendChild(new Caption("Dosen : " + dosen.getNama()));

			Grid gridsub = new Grid();
			gridsub.setSclass("fgrid");
			gridsub.setWidth("100%");
			gridsub.setHeight("100%");
			gridsub.setParent(groupbox);

			Columns columnssub = new Columns();
			columnssub.setParent(gridsub);

			MyColumnConfig columnsub = new MyColumnConfig();
			columnsub.setParent(columnssub);

			Rows rowssub = new Rows();
			rowssub.setParent(gridsub);
			boolean ada = false;
			boolean tidakAda = false;

			for (String nn : nilais) {

				if (tidakAda) {
					break;
				}

				try {
					if (nn.toLowerCase().endsWith("mahasiswa") || nn.toLowerCase().endsWith("siswa")) {
						String[] s = nn.split(",");
						Long formatId = null;
						try {
							formatId = Long.parseLong(s[0]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:6096");
						}

						if (formatId != null && mahasiswa != null && formatId.equals(mahasiswa.getId())) {
							Row row;
							row = new MyFormRow();
							row.setParent(rowssub);

							groupbox = new Groupbox();
							groupbox.setWidth("95%");
							groupbox.setParent(row);

							groupbox.appendChild(new Caption("Oleh : " + mahasiswa.getNama()));

							groupbox.appendChild(bolehKonfirmasi(dosen, pertemuan, mahasiswa));
							ada = true;
							tidakAda = true;
						} else if (formatId != null) {

							Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
									Statusabsensi.class.getName(),
									pertemuan.retreiveAbsensiIdKonfirmasi(formatId, dosen));
							if (statusabsensi != null) {
								tidakAda = true;
								Row row;
								row = new MyFormRow();
								row.setParent(rowssub);

								groupbox = new Groupbox();
								groupbox.setWidth("95%");
								groupbox.setParent(row);

								Mahasiswa mhs = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), formatId);

								String ket = pertemuan.retreiveAbsensiKeteranganKonfirmasi(formatId, dosen);
								ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

								groupbox.appendChild(new Caption(mhs == null ? "" : "Oleh : " + mhs.getNama()));

								Vbox vbox = new Vbox();
								groupbox.appendChild(vbox);

								vbox.appendChild(new Label(
										statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama())));

								String wkt = formatId == null ? ""
										: pertemuan.retreiveAbsensiMulaiKonfirmasi(formatId, dosen) + " s.d "
												+ pertemuan.retreiveAbsensiSampaiKonfirmasi(formatId, dosen);
								vbox.appendChild(new Label(wkt.trim().equals("s.d") ? "" : wkt));

								vbox.appendChild(new MyLabelAgakKecil(ket));
							}
						}
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:6152");
				}
			}

			if (!tidakAda && mahasiswa == null) {
				Row row;
				row = new MyFormRow();
				row.setParent(rowssub);
				row.appendChild(new MyLabelAgakKecilBoldBiru(
						"Belum ada konfirmasi dari Ketua/Perwakilan Kelas terkait kehadiran dosen"));
			} else if (!ada && mahasiswa != null) {
				if (!tidakAda) {
					Row row;
					row = new MyFormRow();
					row.setParent(rowssub);
					groupbox = new Groupbox();
					groupbox.setWidth("95%");
					groupbox.setParent(row);

					groupbox.appendChild(new Caption("Oleh : " + mahasiswa.getNama()));
					groupbox.appendChild(bolehKonfirmasi(dosen, pertemuan, mahasiswa));
				}
			}
		}

		return grid;

	}

	public static Component createStatusSesuaiDenganRpsKonfirmasi(Pertemuan pertemuan, Mahasiswa mahasiswa) {
		if (pertemuan == null) {
			return new Label();
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		String[] nilais = pertemuan.getKeteranganSesuaiDenganRps().split(";");

		List<Dosen> dosens = pertemuan.ambilDosen();
		for (Dosen dosen : dosens) {

			MyFormRow rowAtas = new MyFormRow();
			rowAtas.setParent(rows);

			Groupbox groupbox = new Groupbox();
			groupbox.setParent(rowAtas);
			groupbox.setWidth("95%");

			groupbox.appendChild(new Label("Apakah bapak/ibu " + dosen.getNama() + " mengajar sesuai dengan RPS ?"));

			Grid gridsub = new Grid();
			gridsub.setSclass("fgrid");
			gridsub.setWidth("100%");
			gridsub.setHeight("100%");
			gridsub.setParent(groupbox);

			Columns columnssub = new Columns();
			columnssub.setParent(gridsub);

			MyColumnConfig columnsub = new MyColumnConfig();
			columnsub.setParent(columnssub);

			Rows rowssub = new Rows();
			rowssub.setParent(gridsub);
			boolean ada = false;
			boolean tidakAda = false;

			for (String nn : nilais) {

				try {
					if (nn.toLowerCase().endsWith("mahasiswa") || nn.toLowerCase().endsWith("siswa")) {
						String[] s = nn.split(",");
						Long formatId = null;
						try {
							formatId = Long.parseLong(s[0]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:6240");
						}

						if (formatId != null && mahasiswa != null && formatId.equals(mahasiswa.getId())) {
							Row row;
							row = new MyFormRow();
							row.setParent(rowssub);

							groupbox = new Groupbox();
							groupbox.setWidth("95%");
							groupbox.setParent(row);

							groupbox.appendChild(new Caption("Oleh : " + mahasiswa.getNama()));

							groupbox.appendChild(bolehKonfirmasiRps(dosen, pertemuan, mahasiswa));
							ada = true;
							tidakAda = true;
						} else if (formatId != null) {

							Long status = pertemuan.retreiveAbsensiIdKonfirmasiRps(formatId, dosen);
							if (status != null) {
								tidakAda = true;
								Row row;
								row = new MyFormRow();
								row.setParent(rowssub);

								groupbox = new Groupbox();
								groupbox.setWidth("95%");
								groupbox.setParent(row);

								Mahasiswa mhs = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), formatId);

								String ket = pertemuan.retreiveAbsensiKeteranganSesuaiDenganRps(formatId, dosen);
								ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

								groupbox.appendChild(new Caption(mhs == null ? "" : "Oleh : " + mhs.getNama()));

								Vbox vbox = new Vbox();
								groupbox.appendChild(vbox);

								String nama = "Belum Ditentukan";
								if (status.equals(1L)) {
									nama = "Sesuai";
								}
								if (status.equals(2L)) {
									nama = "Tidak Sesuai";
								}

								vbox.appendChild(new Label(Common.getBahasaConfig(nama)));

								vbox.appendChild(new MyLabelAgakKecil(ket));
							}
						}
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:6296");
				}
			}

			if (!tidakAda && mahasiswa == null) {
				Row row;
				row = new MyFormRow();
				row.setParent(rowssub);
				row.appendChild(new MyLabelAgakKecilBoldBiru(
						"Belum ada konfirmasi dari perwakilan kelas apakah pertemuan ini sesuai dengan RPS"));
			} else if (!ada && mahasiswa != null) {
				Row row;
				row = new MyFormRow();
				row.setParent(rowssub);
				groupbox = new Groupbox();
				groupbox.setWidth("95%");
				groupbox.setParent(row);

				groupbox.appendChild(new Caption("Oleh : " + mahasiswa.getNama()));
				groupbox.appendChild(bolehKonfirmasiRps(dosen, pertemuan, mahasiswa));
			}
		}

		return grid;

	}

	public static Component createStatusSesuaiOlehAkademik(Pertemuan pertemuan, Tbmuser tbmuser) {
		if (pertemuan == null) {
			return new Label();
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		String[] nilais = pertemuan.getKeteranganSesuaiOlehAkademik().split(";");

		List<Dosen> dosens = pertemuan.ambilDosen();
		for (Dosen dosen : dosens) {

			MyFormRow rowAtas = new MyFormRow();
			rowAtas.setParent(rows);

			Groupbox groupbox = new Groupbox();
			groupbox.setParent(rowAtas);
			groupbox.setWidth("95%");

			groupbox.appendChild(new Label("Apakah bapak/ibu " + dosen.getNama() + " mengajar sesuai dengan RPS ?"));

			Grid gridsub = new Grid();
			gridsub.setSclass("fgrid");
			gridsub.setWidth("100%");
			gridsub.setHeight("100%");
			gridsub.setParent(groupbox);

			Columns columnssub = new Columns();
			columnssub.setParent(gridsub);

			MyColumnConfig columnsub = new MyColumnConfig();
			columnsub.setParent(columnssub);

			Rows rowssub = new Rows();
			rowssub.setParent(gridsub);

			if (pertemuan != null && pertemuan.getPerkuliahan() != null
					&& pertemuan.getPerkuliahan().getSemuaPertemuanSesuaiRps()) {

				Row row;
				row = new MyFormRow();
				row.setParent(rowssub);

				groupbox = new Groupbox();
				groupbox.setWidth("95%");
				groupbox.setParent(row);

				Long status = pertemuan.getPerkuliahan().getSemuaNilaiSesuaiRps();
				String ket = pertemuan.getPerkuliahan().getCatatanSesuaiRps();
				ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

				groupbox.appendChild(new Caption("Oleh : Penjamin Mutu"));

				Vbox vbox = new Vbox();
				groupbox.appendChild(vbox);

				String nama = "Belum Ditentukan";
				if (status.equals(1L)) {
					nama = "Sesuai";
				}
				if (status.equals(2L)) {
					nama = "Tidak Sesuai";
				}

				vbox.appendChild(new Label(Common.getBahasaConfig(nama)));

				vbox.appendChild(new MyLabelAgakKecil(ket));

			} else {

				boolean ada = false;
				boolean tidakAda = false;

				for (String nn : nilais) {

					try {
						if (nn.toLowerCase().endsWith("admin")) {
							String[] s = nn.split(",");
							String formatId = null;
							try {
								formatId = (s[0]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiHelper.java:6417");
							}

							if (formatId != null && tbmuser != null && formatId.equals(tbmuser.getUserId())) {
								Row row;
								row = new MyFormRow();
								row.setParent(rowssub);

								groupbox = new Groupbox();
								groupbox.setWidth("95%");
								groupbox.setParent(row);

								groupbox.appendChild(new Caption("Oleh : " + tbmuser.getUserNama()));

								groupbox.appendChild(bolehOlehAkademik(dosen, pertemuan, tbmuser));
								ada = true;
								tidakAda = true;
							} else if (formatId != null) {

								Long status = pertemuan.retreiveAbsensiIdOlehAkademik(formatId, dosen);
								if (status != null) {
									tidakAda = true;
									Row row;
									row = new MyFormRow();
									row.setParent(rowssub);

									groupbox = new Groupbox();
									groupbox.setWidth("95%");
									groupbox.setParent(row);

									Tbmuser mhs = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), formatId);

									String ket = pertemuan.retreiveAbsensiKeteranganSesuaiOlehAkademik(formatId, dosen);
									ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

									groupbox.appendChild(new Caption(mhs == null ? "" : "Oleh : " + mhs.getUserNama()));

									Vbox vbox = new Vbox();
									groupbox.appendChild(vbox);

									String nama = "Belum Ditentukan";
									if (status.equals(1L)) {
										nama = "Sesuai";
									}
									if (status.equals(2L)) {
										nama = "Tidak Sesuai";
									}

									vbox.appendChild(new Label(Common.getBahasaConfig(nama)));

									vbox.appendChild(new MyLabelAgakKecil(ket));
								}
							}
						}

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiHelper.java:6473");
					}
				}

				if (!tidakAda && tbmuser == null) {
					Row row;
					row = new MyFormRow();
					row.setParent(rowssub);
					row.appendChild(new MyLabelAgakKecilBoldBiru(
							"Belum ada konfirmasi dari akademik apakah pertemuan ini sesuai dengan RPS"));
				} else if (!ada && tbmuser != null) {
					Row row;
					row = new MyFormRow();
					row.setParent(rowssub);
					groupbox = new Groupbox();
					groupbox.setWidth("95%");
					groupbox.setParent(row);

					groupbox.appendChild(new Caption("Oleh : " + tbmuser.getUserNama()));
					groupbox.appendChild(bolehOlehAkademik(dosen, pertemuan, tbmuser));
				}
			}
		}

		return grid;

	}
}
