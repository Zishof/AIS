package ais.action.master.sekolah.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
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
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
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
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.HasilUjianMahasiswaHelper;
import ais.action.master.helper.PertemuanHelper;
import ais.action.master.helper.PertemuanPunyaDiskusiHelper;
import ais.action.master.helper.TugasMandiriHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PengajuanIzinTidakMasukPerkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Ruang;
import ais.database.model.StatusPertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.TugasPertemuan;
import ais.database.model.asset.Lokasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.VoKelasPunyaSiswa;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.action.master.helper.AbsensiUiHelper;
import ais.ui.util.MyHtml;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI ZK terbesar dan terpadat di modul sekolah: membangun seluruh panel manajemen
 * kehadiran ("presensi") satu {@link Pertemuan} (sesi kelas/mata pelajaran) — mencakup form info
 * pertemuan (topik, metode, waktu, ruang, dan konfigurasi kelas daring: Zoom/BBB/Skype/WhatsApp/
 * Google Meet/link lain, beserta batas toleransi waktu absen), daftar kehadiran per siswa
 * (dengan kolom nilai/keterlambatan/catatan), daftar pengajuan izin/sakit yang menunggu
 * persetujuan, riwayat aktivitas daring, serta widget status kehadiran guru/asisten yang dipakai
 * ulang oleh layar-layar sekolah lain (empat overload statis {@code createStatusKehadiran}, satu
 * untuk asisten kelas, dua untuk satu/banyak guru, satu ringkasan dari koleksi {@link CommonVO}).
 *
 * <p>
 * Titik masuk utama adalah {@link #mainInit(Pertemuan, Component, boolean)}: membangun tata
 * letak berbeda untuk mode mobile (tumpuk vertikal) vs desktop (border layout tiga panel — info
 * di barat, daftar kehadiran di tengah, daftar izin di timur), sekaligus menentukan status
 * kehadiran default dari konfigurasi {@code default_status_kehadiran} dan mengunci field
 * tanggal/waktu/ruang bila guru tidak diizinkan mengubah jadwal ({@code
 * JadwalPelajaran#getGuruBisaMerubahTanggalJadwalPelajaran}). Setiap perubahan data memicu
 * {@link #reload(Pertemuan)}, yang membangun ulang seluruh panel dari nol lewat timer default
 * (bukan memperbarui komponen secara parsial) — pola yang berulang di banyak method lain di
 * kelas ini — dan menandai cache tren absensi ({@code AbsensiTrenCache}) kotor agar grafik
 * terkait ikut segar. {@link #sesuaikan(Pertemuan, boolean)} adalah satu-satunya titik yang
 * menuliskan kembali seluruh field form ke entitas {@link Pertemuan} dan (opsional) menyimpannya.
 * </p>
 *
 * <p>
 * Kelas ini memegang cukup banyak state instance (field-field form info pertemuan) sehingga satu
 * instance {@link AbsensiSiswaHelper} idealnya dipakai untuk satu tampilan pertemuan pada satu
 * waktu; konstruktor {@link #AbsensiSiswaHelper(Siswa, CalonSiswa)} membatasi tampilan ke
 * konteks satu siswa/calon siswa (mis. saat siswa melihat kehadirannya sendiri, field menjadi
 * read-only) dan memuat daftar status kehadiran yang dapat dipilih (mengecualikan status
 * "belajar"/"cuti"/"dinas" yang khusus untuk pegawai). Widget status kehadiran guru
 * ({@code createStatusKehadiran}) memiliki aturan berlapis: bila jadwal pelajaran mensyaratkan
 * input sesuai jadwal dan waktu saat ini di luar rentang jadwal, guru hanya melihat status
 * read-only (tidak dapat mengubah); selain itu, {@link #boleh} menentukan komponen input yang
 * ditampilkan berdasarkan status kehadiran yang dipilih.
 * </p>
 */
public class AbsensiSiswaHelper {

	// private Textbox topik;
	private Textbox metode;
	private Combobox ujian;
	private Textbox bukuRujukan1;
	private Textbox bukuRujukan2;
	private Textbox guruTamu;
	private Textbox guruTamu2;
	private MyDatebox tanggal;
	private MyDatebox tanggalRealisasi;
	private Timebox waktuMulai;
	private Timebox waktuSelesai;
	private AmbilDataRuangBanbox ruang;

	private List<Siswa> siswas;
	private JadwalPelajaran jadwalPelajaran;

	private Siswa siswa;
	private CalonSiswa calonSiswa;
	private Component tabpanelUtama;
	private MyGrid siswaIzinGrid;
//	private Center center;
	private Textbox topik;
	private List<Statusabsensi> statusabsensis;
	private Collection<Guru> listGuru = null;
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
	private Row rowUtamaAbsensiOnline;
	private MyIntbox bolehAbsenSebelumWaktuMulaiDalamMenit;
	private MyIntbox bolehAbsenSetelahWaktuMulaiDalamMenit;
	private Statusabsensi status = null;
	private Tbmuser tbmuser = null;
	private Combobox lokasi;
	private MyDoublebox jarak;

	/**
	 * Membangun tombol "Kehadiran" (membuka {@link PertemuanHelper} untuk {@code pertemuan})
	 * yang disertai label kecil ringkasan status kehadiran (mis. {@code "Hadir=20, Alpa=3"})
	 * beserta jumlah pengajuan izin ({@code "P=n"}) bila ada; hanya tombol polos bila belum ada
	 * data kehadiran maupun pengajuan izin sama sekali.
	 *
	 * @param pertemuan  pertemuan yang tombolnya dibangun
	 * @param dataLoader callback pemuatan ulang data pada layar pemanggil setelah dialog kehadiran ditutup
	 * @return komponen tombol (atau {@link Label} kosong bila terjadi galat) siap disisipkan ke tampilan
	 */
	public static Component createTombolAbsen(final Pertemuan pertemuan, final DataLoader dataLoader) {

		try {

			MyToolbarbutton a = new MyToolbarbutton("fa-address-book", "Kehadiran");

			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					new PertemuanHelper().display(pertemuan, dataLoader, 0);
				}
			});
			Map<String, Integer> statuses = pertemuan.hitungStatus();
			String abs = statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

			int p = ((Number) HibernateUtil.currentSession().createCriteria(PengajuanIzinTidakMasukPerkuliahan.class)
					.add(Restrictions.eq("pertemuan", pertemuan)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

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

	/**
	 * Membangun helper untuk konteks siswa {@code siswa}/{@code calonSiswa} tertentu (mis.
	 * saat siswa melihat kehadirannya sendiri, {@code null} keduanya untuk tampilan staf), sambil
	 * memuat daftar status kehadiran yang dapat dipilih (mengecualikan status yang namanya
	 * mengandung "belajar"/"cuti"/"dinas", karena status-status itu khusus untuk absensi pegawai).
	 */
	@SuppressWarnings("unchecked")
	public AbsensiSiswaHelper(final Siswa siswa, final CalonSiswa calonSiswa) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
		Session session = HibernateUtil.currentSession();
		statusabsensis = ConstantValues.simpleList(
				session.createCriteria(Statusabsensi.class)
						.add(Restrictions.not(Restrictions.or(Restrictions.ilike("nama", "belajar", MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", "cuti", MatchMode.ANYWHERE),
										Restrictions.ilike("nama", "dinas", MatchMode.ANYWHERE))))),
				Statusabsensi.class);
	}

	/**
	 * Mengambil daftar siswa aktif yang seharusnya hadir pada {@code pertemuan}, diambil dari
	 * kelas pada {@link JadwalPelajaran} terkait dan disaring ulang berdasarkan mata pelajaran
	 * jadwal tersebut ({@code KelasSiswaPunyaSiswa#filterMk}), terurut menurut NIM atau nama
	 * sesuai konfigurasi {@code absensi_urut_berdasarkan_nim}.
	 *
	 * @param pertemuan pertemuan yang daftar siswanya diambil
	 * @return daftar siswa aktif yang termasuk kelas/mata pelajaran pertemuan tersebut, daftar
	 *         kosong bila pertemuan tidak memiliki jadwal pelajaran atau kelas
	 */
	@SuppressWarnings({ "unchecked" })
	public static List<Siswa> populateSiswaDariPertemuan(Pertemuan pertemuan) {

		JadwalPelajaran jadwalPelajaran = pertemuan.getJadwalPelajaran();

		List<Siswa> siswas = new ArrayList<Siswa>();
		Session session = HibernateUtil.currentSession();
		if (jadwalPelajaran != null && jadwalPelajaran.getKelas() != null) {
			List<? extends VoKelasPunyaSiswa> siswasa = ConstantValues.simpleList(session
					.createCriteria(KelasSiswaPunyaSiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("siswa")).add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas()))
					.createAlias("siswa", "siswa")
					.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim") ? Order.asc("siswa.nim") : Order.asc("siswa.nama")),
					KelasSiswaPunyaSiswa.class);

			List<? extends VoKelasPunyaSiswa> kelasSiswaPunyaSiswas = KelasSiswaPunyaSiswa.filterMk(siswasa,
					jadwalPelajaran.getMatapelajaran());
			for (VoKelasPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
				if (kelasSiswaPunyaSiswa.getAktif()) {
					siswas.add(kelasSiswaPunyaSiswa.getSiswa());
				}
			}
			siswasa = null;
			kelasSiswaPunyaSiswas = null;
		} else if (jadwalPelajaran != null && jadwalPelajaran.getKelasLesSiswa() != null) {
			List<KelasLesSiswaPunyaSiswa> siswasa = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
					.add(Restrictions.isNotNull("siswa")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("kelasLesSiswa", jadwalPelajaran.getKelasLesSiswa()))
					.createAlias("siswa", "siswa")
					.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim") ? Order.asc("siswa.nim") : Order.asc("siswa.nama"))
					.list();

			for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : siswasa) {
				if (kelasLesSiswaPunyaSiswa.getAktif()) {
					siswas.add(kelasLesSiswaPunyaSiswa.getSiswa());
				}
			}
			siswasa = null;
		} else if (pertemuan.getFormulirKegiatan() != null) {
			siswas = ConstantValues.simpleList(session.createCriteria(FormulirKegiatanPeserta.class)
					.setProjection(Projections.property("siswa.id")).add(Restrictions.isNotNull("siswa"))
					.add(Restrictions.eq("formulirKegiatan", pertemuan.getFormulirKegiatan()))
					.createAlias("siswa", "siswa").addOrder(Order.asc("siswa.nama")), Siswa.class, false);

		}

		return siswas;
	}

	@SuppressWarnings({ "deprecation" })
	/**
	 * Membangun panel form informasi pertemuan: (opsional) ringkasan info pertemuan
	 * ({@code DashboardTimelinePertemuan#displayInfoPertemuan}), hari/waktu/ruang dari jadwal
	 * pelajaran, serta seluruh field yang dapat diubah (topik, metode, waktu mulai/selesai,
	 * ruang, buku rujukan, guru tamu, status pertemuan/ujian, konfigurasi kelas daring dan batas
	 * toleransi absen daring). Setiap perubahan field memicu {@link #sesuaikan(Pertemuan,
	 * boolean)} untuk menyimpan langsung ke database.
	 *
	 * @param pertemuan pertemuan yang informasinya ditampilkan/diedit
	 * @return komponen panel info siap disisipkan ke tata letak utama
	 */
	private Component bagianInfo(final Pertemuan pertemuan) throws Exception {

		final EventListener sesuaikan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pertemuan a = (Pertemuan) arg0.getData();
				sesuaikan(a, false);
			}
		};

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

		if (pertemuan.getJadwalPelajaran() != null) {

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Waktu"));

			Vbox vbox = new Vbox();
			Common.displayHariJamRuanganJadwalPelajaranUmum(vbox, jadwalPelajaran);
			row.appendChild(vbox);
		}

		EventListener updateLocal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				sesuaikan(pertemuan, true);
			}
		};

		row = new MyFormRow();
		row.setValign("top");

		topik = new Textbox(pertemuan.getTopik());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan akhir pembelajaran"));
		if (siswa != null || calonSiswa != null) {
			row.appendChild(new Label(pertemuan.getTopik()));
		} else {
			row.appendChild(topik);
		}
		topik.setWidth("90%");
		topik.setRows(4);
		topik.addEventListener("onChange", updateLocal);

		bukuRujukan1 = new Textbox(pertemuan.getBukuRujukan1());
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahan Kajian"));

		if (siswa != null || calonSiswa != null) {
			row.appendChild(new Label(pertemuan.getBukuRujukan1()));
		} else {
			row.appendChild(bukuRujukan1);
		}

		bukuRujukan1.setWidth("90%");
		bukuRujukan1.setRows(2);
		bukuRujukan1.addEventListener("onChange", updateLocal);

		bukuRujukan2 = new Textbox(pertemuan.getBukuRujukan2());
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Daftar Pustaka"));
		if (siswa != null || calonSiswa != null) {
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

		if (siswa != null || calonSiswa != null) {
			row.appendChild(
					new Label(pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama()));
		} else {
			row.appendChild(ujian);
		}
		ujian.setWidth("90%");
		ujian.setReadonly(true);
		ujian.addEventListener("onChange", updateLocal);

		if (siswa != null || calonSiswa != null) {

		} else {
			Common.initKeterangan(rows, "Untuk pertemuan Online, harap ubah jenis pertemuanya menjadi Daring.");
		}

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
		if (siswa != null || calonSiswa != null) {
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

		// rowMeet.appendChild(AktifitasJadwalPelajaranHelper.createCalendarButton(pertemuan,
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

				rowMeetKeterangan.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.GOOGLE_MEET));
				rowMeet.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.GOOGLE_MEET));
				rowLinkMeetLink.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.GOOGLE_MEET));
				rowLinkMeetButton.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.GOOGLE_MEET));

				rowLinkZoomKeterangan.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.ZOOM));
				rowLinkZoom.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.ZOOM));
				rowLinkZoomButton.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.ZOOM));
				rowLinkZoomLink.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.ZOOM));

				rowLinkBbbKeterangan.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.BBB));
				rowLinkBbb.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.BBB));
				rowLinkBbbButton.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.BBB));
				rowLinkBbbLink.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.BBB));

				rowLinkSkypeKeterangan.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.SKYPE));
				rowLinkSkype.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.SKYPE));
				rowLinkSkypeButton.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.SKYPE));
				rowLinkSkypeLink.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.SKYPE));

				rowLinkWa.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.WA));
				rowLinkWaButton.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.WA));
				waLink.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.WA));
				rowLinkWaKeterangan.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.WA));

				rowLinkLain.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.LAIN));
				linkLain.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.LAIN));
				rowLinkLainKeterangan.setVisible(siswa == null && calonSiswa == null && ol.equals(Pertemuan.LAIN));

				testButton.setVisible(siswa == null && calonSiswa == null && true);
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

		row = new MyFormRow();
		row.setVisible(siswa == null && calonSiswa == null);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(perkulaiahnOnlineHarusSesuaiJadwal = new MyCheckboxConfig(
				"Pertemuan online dan absensi online harus sesuai dengan jadwal yang telah ditentukan"));
		perkulaiahnOnlineHarusSesuaiJadwal.setChecked(pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal());
		perkulaiahnOnlineHarusSesuaiJadwal.addEventListener("onClick", updateLocal);

		guruTamu = new Textbox(pertemuan.getGuruTamu() == null ? "" : pertemuan.getGuruTamu());
		guruTamu2 = new Textbox(pertemuan.getGuruTamu2() == null ? "" : pertemuan.getGuruTamu2());
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Tamu"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		if (siswa != null || calonSiswa != null) {
			hbox.appendChild(new Label(pertemuan.getGuruTamu() == null ? "" : pertemuan.getGuruTamu()));
		} else {
			hbox.appendChild(guruTamu);
		}
		guruTamu.setWidth("90%");
		if (siswa != null || calonSiswa != null) {
			hbox.appendChild(new Label(pertemuan.getGuruTamu2() == null ? "" : pertemuan.getGuruTamu2()));
		} else {
			hbox.appendChild(guruTamu2);
		}
		guruTamu2.setWidth("90%");
		guruTamu.addEventListener("onChange", updateLocal);
		guruTamu2.addEventListener("onChange", updateLocal);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		tanggal = new MyDatebox(pertemuan.getTanggal());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Rencana (*)"));
		if (siswa != null || calonSiswa != null) {
			row.appendChild(
					new Label(pertemuan.getTanggal() == null ? "" : Common.timeFormat.get().format(pertemuan.getTanggal())));
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:830");

		}
		try {
			waktuSelesai
					.setValue(pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:837");

		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Rencana"));
		hbox = new Hbox();
		row.appendChild(hbox);
		if (siswa != null || calonSiswa != null) {
			hbox.appendChild(
					new Label(waktuMulai.getValue() == null ? "" : Common.timeFormat.get().format(waktuMulai.getValue())));
		} else {
			hbox.appendChild(waktuMulai);
		}
		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());

		hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
		if (siswa != null || calonSiswa != null) {
			hbox.appendChild(new Label(
					waktuSelesai.getValue() == null ? "" : Common.timeFormat.get().format(waktuSelesai.getValue())));
		} else {
			hbox.appendChild(waktuSelesai);
		}
		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Menit toleransi absensi online sebelum"));
		bolehAbsenSebelumWaktuMulaiDalamMenit = new MyIntbox(pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit());
		if (siswa != null || calonSiswa != null) {
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
		if (siswa != null || calonSiswa != null) {
			row.appendChild(new Label(
					Common.numberFormat.get().format(pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit()) + " menit"));
		} else {
			row.appendChild(bolehAbsenSetelahWaktuMulaiDalamMenit);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kehadiran Guru"));
		Vbox vbox = new Vbox();
		vbox.setParent(row);

		Component guruUtama = null;

		if (jadwalPelajaran != null) {
			listGuru = jadwalPelajaran.populateGuruBuNama();
		}

		if (listGuru != null) {
			for (Guru guru : listGuru) {
				vbox.appendChild(CommonMedia.tampilkanGambarKecil(guru));
				vbox.appendChild(new Label(guru.getNama()));
				vbox.appendChild(guruUtama = AbsensiSiswaHelper.createStatusKehadiran(guru, pertemuan, siswa,
						calonSiswa, tanggalRealisasi, sesuaikan));
			}
		}

		Guru dsnPengganti = (Guru) (pertemuan.getGuruPengganti() == null ? null
				: ConstantValues.ambil(Guru.class.getName(), pertemuan.getGuruPengganti()));

		row = new MyFormRow();
		row.setValign("top");
		row.setVisible(siswa == null && calonSiswa == null);
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig guruPenggantiAda;
		row.appendChild(guruPenggantiAda = new ais.ui.util.MyCheckboxConfig("Ada guru pengganti"));
		guruPenggantiAda.setChecked(dsnPengganti != null);

		final MyFormRow rowGuruPengganti = new MyFormRow();
		rowGuruPengganti.setVisible(guruPenggantiAda.isChecked());
		rowGuruPengganti.setStyle("border:0px;background: transparent;");
		rowGuruPengganti.setParent(rows);
		rowGuruPengganti.setValign("top");
		rowGuruPengganti.appendChild(new ais.ui.util.MyLabelConfig("Guru Pengganti"));

		final Component utmGuru = guruUtama;
		final AmbilDataGuruBanbox guruPengganti = new AmbilDataGuruBanbox(true);
		guruPenggantiAda.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowGuruPengganti.setVisible(guruPenggantiAda.isChecked());
				if (!guruPenggantiAda.isChecked()) {
					if (pertemuan.getId() != null) {
						HibernateUtil.currentSession().refresh(pertemuan);
					}
					pertemuan.setGuruPengganti(null);
					sesuaikan(pertemuan, false);
					Common.refreshUpdate(pertemuan);
					guruPengganti.setValue("");
					guruPengganti.setAttribute("guru", null);
				}

				if (utmGuru != null) {
					utmGuru.setVisible(!guruPenggantiAda.isChecked());
				}
			}
		});

		vbox = new Vbox();
		vbox.setParent(rowGuruPengganti);

		final Hbox guruPenggantiHb = new Hbox();
		vbox.appendChild(guruPenggantiHb);

		final Hbox guruPenggantiHbWkt = new Hbox();

		if (dsnPengganti != null) {
			Common.clear(guruPenggantiHb);
			Common.clear(guruPenggantiHbWkt);
			guruPenggantiHb.appendChild(CommonMedia.tampilkanGambarKecil(dsnPengganti));
			guruPenggantiHbWkt.appendChild(AbsensiSiswaHelper.createStatusKehadiran(dsnPengganti, pertemuan, siswa,
					calonSiswa, tanggalRealisasi, sesuaikan));

		}

		if (guruUtama != null) {
			guruUtama.setVisible(!guruPenggantiAda.isChecked());
		}

		if (siswa == null && calonSiswa == null) {

			vbox.appendChild(guruPengganti);
			guruPengganti.setAttribute("guru", dsnPengganti);
			guruPengganti.setValue(dsnPengganti == null ? "" : dsnPengganti.getNama());
			guruPengganti.setReadonly(true);
			guruPengganti.setWidth("90%");

			guruPengganti.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (pertemuan.getId() != null) {
						HibernateUtil.currentSession().refresh(pertemuan);
					}
					Guru d = (Guru) guruPengganti.getAttribute("guru");
					pertemuan.setGuruPengganti(d == null ? null : d.getId());
					sesuaikan(pertemuan, false);
					Common.refreshUpdate(pertemuan);

					if (d != null) {
						Common.clear(guruPenggantiHb);
						Common.clear(guruPenggantiHbWkt);
						guruPenggantiHb.appendChild(CommonMedia.tampilkanGambarKecil(d));
						guruPenggantiHbWkt.appendChild(AbsensiSiswaHelper.createStatusKehadiran(d, pertemuan, siswa,
								calonSiswa, tanggalRealisasi, sesuaikan));
					}
				}
			});
		} else {
			Statusabsensi statusabsensi = dsnPengganti == null ? null
					: (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(dsnPengganti.getId()));
			new Label(dsnPengganti == null ? "" : dsnPengganti.getNama()).setParent(vbox);

			new Label(statusabsensi == null ? "" : statusabsensi.getNama()).setParent(guruPenggantiHbWkt);
			String wkt = dsnPengganti == null ? ""
					: pertemuan.retreiveAbsensiMulai(dsnPengganti.getId()) + " s.d "
							+ pertemuan.retreiveAbsensiSampai(dsnPengganti.getId());
			new Label(wkt.trim().equals("s.d") ? "" : wkt).setParent(guruPenggantiHbWkt);

			waktuMulai.setDisabled(true);
			waktuSelesai.setDisabled(true);
		}

		vbox.appendChild(guruPenggantiHbWkt);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Realisasi"));
		if (siswa != null || calonSiswa != null) {
			row.appendChild(new Label(
					tanggalRealisasi.getValue() == null ? "" : Common.dateFormat6.get().format(tanggalRealisasi.getValue())));
		} else {
			row.appendChild(tanggalRealisasi);
			Common.initKeterangan(rows,
					"(*) \"Tanggal Realisasi\" adalah tanggal terjadi-nya proses belajar mengajar, tanggal realisasi bisa diisi jika guru telah melakukan absensi atau terdapat guru pengganti");
		}
		tanggalRealisasi.addEventListener("onChange", updateLocal);

		row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		ruang = new AmbilDataRuangBanbox();
		if (siswa != null || calonSiswa != null) {
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
		if (siswa != null || calonSiswa != null) {
			row.appendChild(new Label(pertemuan.getMetodePembelajaran()));
		} else {
			row.appendChild(metode);
		}
		metode.setWidth("90%");
		metode.setRows(2);
		metode.addEventListener("onChange", updateLocal);

		if (siswa != null || calonSiswa != null) {
			Common.freeze(grid, true);
		}

		return grid;
	}

	@SuppressWarnings("unchecked")
	/**
	 * Titik masuk utama: membangun seluruh panel manajemen kehadiran untuk {@code pertemuan} ke
	 * dalam {@code tabpanelUtama}. Menentukan daftar siswa yang seharusnya hadir
	 * ({@link #populateSiswaDariPertemuan}), status kehadiran default dari konfigurasi
	 * {@code default_status_kehadiran}, dan tata letak berbeda untuk mobile (panel info, daftar
	 * kehadiran, daftar izin ditumpuk vertikal) vs desktop (border layout tiga panel: info di
	 * barat, daftar kehadiran di tengah, daftar izin di timur, dimuat lewat timer). Panel dikunci
	 * (read-only) bila helper dibangun untuk konteks satu siswa/calon siswa tertentu, dan field
	 * tanggal/waktu/ruang dikunci bila pengguna adalah guru yang tidak diizinkan mengubah jadwal.
	 * Daftar izin/sakit tidak ditampilkan untuk pertemuan yang merupakan sesi ujian PMB/PSB.
	 *
	 * @param pertemuan     pertemuan yang kehadirannya dikelola
	 * @param tabpanelUtama komponen ZK induk tempat seluruh panel disisipkan (dibersihkan lebih dulu)
	 * @param tampilInfo    tampilkan ringkasan info pertemuan di puncak panel info
	 */
	public void mainInit(final Pertemuan pertemuan, Component tabpanelUtama, boolean tampilInfo) throws Exception {
		this.tabpanelUtama = tabpanelUtama;
		this.tampilInfo = tampilInfo;
		siswas = AbsensiSiswaHelper.populateSiswaDariPertemuan(pertemuan);
		if (pertemuan != null) {
			pertemuan.masukkanData("melihat_absensi");
		}
		jadwalPelajaran = pertemuan.getJadwalPelajaran();

		Konfigurasi konfigurasi = Common.getKonfigurasi("default_status_kehadiran",
				ConstantValues.BELUM_ABSEN.getKode());

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
			createListSiswaAbsensi(row, pertemuan);
//			d.setStyle("min-height: 400px;height:" + (150 + (75 * siswas.size())) + "px");

			if (pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null) {
				row = new MyFormRow();
				row.setParent(rows);
				row.setValign("top");
				createListSiswaIzin(row, pertemuan).setHeight("500px");
			}
		} else {
			center.setTitle("Presensi kehadiran siswa");

			West west = new West();
			west.setTitle("Informasi");
			ais.ui.util.ZkCompat.setFlex(west, true);
			west.setWidth("28%");
			west.setParent(borderlayout);
			west.appendChild(bagianInfo(pertemuan));

			createListSiswaAbsensi(center, pertemuan);

			if (siswa != null || calonSiswa != null) {
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
						createListSiswaIzin(east, pertemuan);
					}
				});
			}

		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilGuru() != null && jadwalPelajaran != null
				&& !jadwalPelajaran.getGuruBisaMerubahTanggalJadwalPelajaran()) {
			tanggal.setDisabled(true);
			waktuMulai.setDisabled(true);
			waktuSelesai.setDisabled(true);
			ruang.setDisabled(true);
		}
	}

	/** Menandai cache tren absensi kotor untuk perkuliahan terkait, lalu membangun ulang seluruh panel dari nol lewat {@link #mainInit(Pertemuan, Component, boolean)} (dijadwalkan lewat timer default). */
	private void reload(final Pertemuan pertemuan) {
		// Absensi berubah → segarkan cache tren agar grafik yang dirender ulang memakai data baru.
		try {
			if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
				ais.common.AbsensiTrenCache.invalidasi(pertemuan.getPerkuliahan().getId());
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1216");
		}
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tabpanelUtama);
				mainInit(pertemuan, tabpanelUtama, tampilInfo);
			}
		});
	}

	/**
	 * Membangun panel daftar kehadiran siswa untuk {@code pertemuan}: statistik ringkas (gaya
	 * kartu donat/gauge), tren kehadiran, toolbar aksi massal, dan baris kehadiran per siswa
	 * (didelegasikan ke {@link #tampilRowAbsensi} lewat {@link #reloadAbsensiBaru}). Dibangun
	 * sebagai satu wadah vertikal penuh-lebar (bukan sel-sel baris ZK berdampingan) agar kartu
	 * statistik dan tren tersusun rapi di atas grid data.
	 *
	 * @param parentrow komponen ZK induk tempat panel disisipkan
	 * @param pertemuan pertemuan yang daftar kehadirannya ditampilkan
	 */
	private void createListSiswaAbsensi(Component parentrow, final Pertemuan pertemuan) {

		Row utamaBanget = Common.tampilanScroll1(parentrow);

		/*
		 * Wadah VERTIKAL full-width. Sebelumnya gaya, donut, tren, dan toolbar ditempel langsung
		 * sebagai anak ZK Row → dirender sel-sel <td> berdampingan (horizontal) sehingga
		 * berantakan. Menumpuknya dalam satu Vlayout full-width membuatnya rapi & melebar penuh;
		 * donut+tren tetap responsif (berdampingan di desktop, menumpuk di mobile).
		 */
		org.zkoss.zul.Vlayout kolomAtas = AbsensiUiHelper.wadahRingkasanAtas(utamaBanget);

		// Tampilan modern (reuse AbsensiUiHelper, HTML/CSS): gaya kartu + donut komposisi kehadiran
		// pertemuan ini + tren kehadiran antar-pertemuan (ber-cache L1/L2/L3) di atas daftar presensi.
		kolomAtas.appendChild(new MyHtml(AbsensiUiHelper.gayaKartuPresensi()));
		try {
			String komposisi = AbsensiUiHelper.htmlKomposisiKehadiran(pertemuan, siswas);
			Long pkId = (pertemuan != null && pertemuan.getPerkuliahan() != null)
					? pertemuan.getPerkuliahan().getId()
					: null;
			String tren = AbsensiUiHelper.htmlTrenKehadiran(pkId);
			String ringkas = AbsensiUiHelper.htmlRingkasanGabung(komposisi, tren);
			if (ringkas != null && ringkas.length() > 0) {
				kolomAtas.appendChild(new MyHtml(ringkas));
			}
		} catch (Throwable abaikanRingkasan) { ais.common.ErrorAuditUtil.record(abaikanRingkasan, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1253");
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Toolbar toolbar = new Toolbar();
		toolbar.setSclass("ais-absn-toolbar");
		toolbar.setParent(kolomAtas);

		MyFormRow utamalagi = new MyFormRow();
		utamalagi.setParent(utamaBanget.getParent());

		Row rowUtama = Common.tampilanScroll1(utamalagi);
		Rows rowsUtama = (Rows) rowUtama.getParent();

		if ((tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null)) {

			MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Semua hadir", "/img/svg/check2.svg");
			masuk.setParent(toolbar);
			masuk.setTooltiptext("Tutup");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin semua siswa masuk kelas ini ?", "Pertanyaan",
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

												for (GeneralValueObject generalValueObject : siswas) {
													Statusabsensi statusabsensi = ConstantValues.MASUK;

													pertemuan.populate(generalValueObject.getId(), statusabsensi,
															pertemuan.getWaktuMulai(), pertemuan.getWaktuSelesai(),
															"Siswa");

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
													HibernateUtil.currentSession().refresh(pertemuan);
												}

												for (GeneralValueObject generalValueObject : siswas) {
													Statusabsensi statusabsensi = ConstantValues.BELUM_ABSEN;
													pertemuan.populate(generalValueObject.getId(), statusabsensi, "",
															null, pertemuan.getWaktuMulai(),
															pertemuan.getWaktuSelesai(), "Siswa");
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
							.getRealPath("/tmp/data_absen_" + URLEncoder
									.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
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

							rowhead.createCell(0).setCellValue("Siswa");
							rowhead.createCell(1).setCellValue("Status");
							rowhead.createCell(2).setCellValue("Mulai");
							rowhead.createCell(3).setCellValue("Sampai");
							rowhead.createCell(4).setCellValue("Keterangan");

							rowIndex = 1;
							for (GeneralValueObject siswa : siswas) {

								label.setValue("Sedang memproses data " + siswa.toString() + " ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / siswas.size()) + " %)");

								XSSFRow row = sheet.createRow(rowIndex);
								row.createCell(0).setCellValue(siswa.toString());

								Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
										Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(siswa.getId()));

								row.createCell(1).setCellValue(statusabsensi == null ? "" : statusabsensi.toString());

								row.createCell(2).setCellValue(pertemuan.retreiveAbsensiMulai(siswa.getId()));
								row.createCell(3).setCellValue(pertemuan.retreiveAbsensiSampai(siswa.getId()));

								row.createCell(4).setCellValue(pertemuan.retreiveAbsensiKeterangan(siswa.getId()));

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
					Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
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
											MyMessageboxConfig.show("Upload data absen berhasil dilakukan.",
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
										Session session = null;
										try {

											XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
											XSSFSheet sheet = workbook.getSheetAt(0);

											/*
											 * WAJIB openSession(), BUKAN currentNativeSession(). Common.getSheetContentAsObject()
											 * dan getSheetContentAsString() di dalam loop menutup native session ThreadLocal
											 * (HibernateUtil.closeSession()), sehingga session hasil currentNativeSession()
											 * sudah TERTUTUP saat dipakai -> "Session is closed!" di SETIAP baris.
											 */
											session = HibernateUtil.openSession();
											int rowCount = (sheet.getLastRowNum() + 1);
											for (int i = 1; i < rowCount; i++) {
												@SuppressWarnings("rawtypes")
												Map datum = null;
												try {

													Siswa siswa = (Siswa) Common.getSheetContentAsObject(sheet, 0, i,
															Siswa.class);

													if (siswa == null) {
														continue;
													}

													String waktuMulai = Common.getSheetContentAsString(sheet, 2, i);
													String waktuSelesai = Common.getSheetContentAsString(sheet, 3, i);
													String keterangan = Common.getSheetContentAsString(sheet, 4, i);

													label.setValue("Upload data \"" + siswa.getNim() + " - "
															+ siswa.getNama() + "\" ("
															+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

													Statusabsensi statusabsensi = (Statusabsensi) Common
															.getSheetContentAsObject(sheet, 1, i, Statusabsensi.class);

													// Reload ke session khusus thread ini agar entitas managed (bukan
													// detached dari cache/session lain) -> update pasti ter-flush.
													Pertemuan pertemuanSafe = (Pertemuan) session.get(Pertemuan.class,
															pertemuan.getId());
													if (pertemuanSafe == null) {
														continue;
													}
													pertemuanSafe.populate(siswa.getId(), statusabsensi, keterangan, null,
															waktuMulai, waktuSelesai, "Siswa");
													session.getTransaction().begin();
													try {
														Common.refreshUpdate(session, pertemuanSafe);
														session.getTransaction().commit();
													} catch (Exception eSimpan) {
														try {
															session.getTransaction().rollback();
														} catch (Exception eRoll) { ais.common.ErrorAuditUtil.record(eRoll,
																"rollback-gagal-upload src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java");
														}
														throw eSimpan;
													}

												} catch (Exception e) {
													System.out.println("error --> datum=>" + datum);
													Common.tampilErrorJikaAdmin(e);
												}
											}
										} catch (Exception e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1554");
										} finally {
											HibernateUtil.closeSessionQuietly(session);
											HibernateUtil.closeSession();
										}

										label.setValue("");
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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		button.setVisible(jadwalPelajaran != null);
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanBeritaAcara(pertemuan, null);

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
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
	/** Menyusun riwayat sesi absensi daring (online) {@code pertemuan}, dikelompokkan per timestamp/sesi ke peta detail (dosen/mahasiswa yang tercatat online pada sesi tersebut). */
	private TreeMap<String, Map<String, String>> reloadSejarahAbsensiOnline(Pertemuan pertemuan) {
		String sebelumnya = pertemuan.retreive("sejarah");
		JSONObject jsonObject = new JSONObject();
		try {
			if (!sebelumnya.isEmpty()) {
				jsonObject = new JSONObject(sebelumnya);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1631");
			// TODO: handle exception
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
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1650");
			}

		}
		return maps;
	}

	private TreeMap<String, Map<String, String>> maps;
	private String namaPencarianOnline = "";

	/**
	 * Menampilkan dialog modal riwayat absensi daring {@code pertemuan}, memuat data lewat
	 * {@link #reloadSejarahAbsensiOnline(Pertemuan)} dan menyajikannya sebagai daftar sesi yang
	 * dapat diperluas untuk melihat detail peserta yang tercatat online pada tiap sesi.
	 */
	private void tampilkanAbsensiOnline(final Pertemuan pertemuan) {
		Common.clear(rowUtamaAbsensiOnline);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getCalonSiswa() == null) {
			maps = reloadSejarahAbsensiOnline(pertemuan);

			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.setParent(rowUtamaAbsensiOnline);

			groupboxStyled.appendChild(new MyCaptionStyled("Sejarah Absensi Online"));

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

			if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
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
															Siswa siswa = (Siswa) (ss[1].startsWith("mhs")
																	? ConstantValues.ambil(Siswa.class.getName(),
																			Long.parseLong(ss[1].replaceAll("mhs", "")))
																	: null);
															Guru guru = (Guru) (ss[1].startsWith("dsn")
																	? ConstantValues.ambil(Guru.class.getName(),
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
															if (siswa != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(siswa.getId()));
															} else if (guru != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(guru.getId()));
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
																} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1775");

																}

																String keterangan = "Absensi online "
																		+ (tglJam == null ? ""
																				: Common.dateFormat5.get().format(tglJam))
																		+ " "
																		+ (video == null || video.isEmpty() ? "foto"
																				: "video")
																		+ " " + foto + " lokasi " + lokasi;

																if (siswa != null) {
																	pertemuan.populate(siswa.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get().format(tglJam),
																			pertemuan.getWaktuSelesai(), "Siswa");
																} else if (guru != null) {
																	pertemuan.populate(guru.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get().format(tglJam),
																			pertemuan.getWaktuSelesai(), "Guru");
																}
															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1803");
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
															Siswa siswa = (Siswa) (ss[1].startsWith("mhs")
																	? ConstantValues.ambil(Siswa.class.getName(),
																			Long.parseLong(ss[1].replaceAll("mhs", "")))
																	: null);
															Guru guru = (Guru) (ss[1].startsWith("dsn")
																	? ConstantValues.ambil(Guru.class.getName(),
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
															if (siswa != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(siswa.getId()));
															} else if (guru != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(guru.getId()));
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
																} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1898");

																}

																String keterangan = "Absensi online "
																		+ (tglJam == null ? ""
																				: Common.dateFormat5.get().format(tglJam))
																		+ " "
																		+ (video == null || video.isEmpty() ? "foto"
																				: "video")
																		+ " " + foto + " lokasi " + lokasi;

																if (siswa != null) {
																	pertemuan.populate(siswa.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get().format(tglJam),
																			pertemuan.getWaktuSelesai(), "Siswa");
																} else if (guru != null) {
																	pertemuan.populate(guru.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get().format(tglJam),
																			pertemuan.getWaktuSelesai(), "Guru");
																}
															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:1926");
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
															Siswa siswa = (Siswa) (ss[1].startsWith("mhs")
																	? ConstantValues.ambil(Siswa.class.getName(),
																			Long.parseLong(ss[1].replaceAll("mhs", "")))
																	: null);
															Guru guru = (Guru) (ss[1].startsWith("dsn")
																	? ConstantValues.ambil(Guru.class.getName(),
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
															if (siswa != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(siswa.getId()));
															} else if (guru != null) {
																statusabsensi = (Statusabsensi) ConstantValues.ambil(
																		Statusabsensi.class.getName(),
																		pertemuan.retreiveAbsensiId(guru.getId()));
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
																} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:2019");

																}

																String keterangan = "Absensi online "
																		+ (tglJam == null ? ""
																				: Common.dateFormat5.get().format(tglJam))
																		+ " foto " + foto + " lokasi " + lokasi;

																if (siswa != null) {
																	pertemuan.populate(siswa.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get().format(tglJam),
																			pertemuan.getWaktuSelesai(), "Siswa");
																} else if (guru != null) {
																	pertemuan.populate(guru.getId(),
																			ConstantValues.MASUK, keterangan, null,
																			tglJam == null ? pertemuan.getWaktuMulai()
																					: Common.timeFormat2.get().format(tglJam),
																			pertemuan.getWaktuSelesai(), "Guru");
																}
															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:2044");
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
			column.setWidth(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null ? "12%" : "0%");

			Rows rowsData = new Rows();
			rowsData.setParent(grid);

			Siswa selectedMhs = tbmuser == null ? null : tbmuser.getSiswa();

			for (final String key : maps.keySet()) {
				try {
					String[] ss = key.split(":");
					String tgl = ss[0];
					Siswa siswa = (Siswa) (ss[1].startsWith("mhs")
							? ConstantValues.ambil(Siswa.class.getName(), Long.parseLong(ss[1].replaceAll("mhs", "")))
							: null);

					if (selectedMhs != null && selectedMhs.getId() != null) {
						if (siswa == null || siswa.getId() == null || !siswa.getId().equals(selectedMhs.getId())) {
							continue;
						}
					}

					Guru guru = (Guru) (ss[1].startsWith("dsn")
							? ConstantValues.ambil(Guru.class.getName(), Long.parseLong(ss[1].replaceAll("dsn", "")))
							: null);

					if (namaPencarianOnline.trim().isEmpty() || ((siswa != null && ((siswa.getNim() != null
							&& siswa.getNim().toLowerCase().contains(namaPencarianOnline.trim().toLowerCase())) ||

							(siswa.getNama() != null && siswa.getNama().toLowerCase()
									.contains(namaPencarianOnline.trim().toLowerCase())))

					))

							|| ((guru != null && ((guru.getKode() != null
									&& guru.getKode().toLowerCase().contains(namaPencarianOnline.trim().toLowerCase()))
									||

									(guru.getNama() != null && guru.getNama().toLowerCase()
											.contains(namaPencarianOnline.trim().toLowerCase())))

							))

					) {

						MyFormRow rowData = new MyFormRow();
						rowData.setValign("top");
						rowData.setValign("top");
						rowData.setParent(rowsData);
						try {
							Label a;
							rowData.appendChild(
									a = new Label(Common.dateFormat5.get().format(Common.dateFormat9.get().parse(tgl))));
							a.setStyle("font-size:9px;");
						} catch (Exception e) {
							rowData.appendChild(new Label());
						}

						Label aaa;
						rowData.appendChild(aaa = new Label(siswa != null ? siswa.getNim() + " " + siswa.getNama()
								: guru != null ? guru.getNama() : ""));
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
						if (siswa != null) {
							statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
									pertemuan.retreiveAbsensiId(siswa.getId()));
						} else if (guru != null) {
							statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
									pertemuan.retreiveAbsensiId(guru.getId()));
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
									MyMessageboxConfig.show("Apakah yakin siswa ini dianggap hadir di kelas ini ?",
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
																	Siswa siswa = (Siswa) (ss[1].startsWith("mhs")
																			? ConstantValues.ambil(
																					Siswa.class.getName(),
																					Long.parseLong(ss[1]
																							.replaceAll("mhs", "")))
																			: null);
																	Guru guru = (Guru) (ss[1].startsWith("dsn")
																			? ConstantValues.ambil(Guru.class.getName(),
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
																	if (siswa != null) {
																		statusabsensi = (Statusabsensi) ConstantValues
																				.ambil(Statusabsensi.class.getName(),
																						pertemuan.retreiveAbsensiId(
																								siswa.getId()));
																	} else if (guru != null) {
																		statusabsensi = (Statusabsensi) ConstantValues
																				.ambil(Statusabsensi.class.getName(),
																						pertemuan.retreiveAbsensiId(
																								guru.getId()));
																	}
																	if (statusabsensi == null) {
																		statusabsensi = ConstantValues.BELUM_ABSEN;
																	}

																	try {
																		tglJam = Common.dateFormat9.get().parse(tgl);
																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:2281");

																	}

																	String keterangan = "Absensi online "
																			+ (tglJam == null ? ""
																					: Common.dateFormat5.get().format(tglJam))
																			+ " "
																			+ (video == null || video.isEmpty() ? "foto"
																					: "video")
																			+ " " + foto + " lokasi " + lokasi;

																	if (siswa != null) {
																		pertemuan.populate(siswa.getId(),
																				ConstantValues.MASUK, keterangan, null,
																				tglJam == null
																						? pertemuan.getWaktuMulai()
																						: Common.timeFormat2.get()
																								.format(tglJam),
																				pertemuan.getWaktuSelesai(), "Siswa");
																	} else if (guru != null) {
																		pertemuan.populate(guru.getId(),
																				ConstantValues.MASUK, keterangan, null,
																				tglJam == null
																						? pertemuan.getWaktuMulai()
																						: Common.timeFormat2.get()
																								.format(tglJam),
																				pertemuan.getWaktuSelesai(), "Guru");
																	}

																} catch (Exception e) {
																	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:2312");
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
							rowData.appendChild(new MyHtml(AbsensiUiHelper.badgeStatus(statusabsensi)));
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:2341");
				}
			}
		}

	}

	/**
	 * Membangun toolbar aksi massal di atas grid kehadiran (khusus tampak bagi staf, bukan siswa
	 * sendiri): "Ikut Diskusi" (menandai seluruh peserta diskusi hadir lewat
	 * {@link PertemuanPunyaDiskusiHelper#diskusiDianggapHadir}) dan "Ikut Ujian" (aksi serupa
	 * untuk peserta ujian), masing-masing hanya tampil bila pertemuan memiliki diskusi/ujian
	 * terkait; menyimpan perubahan lewat {@link #sesuaikan(Pertemuan, boolean)} dan membangun
	 * ulang panel setelah aksi selesai.
	 */
	private void tampilBawah(final Pertemuan pertemuan, Row vlayout) {
		final Tbmuser tbmuser = Common.getCurrentUser();

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
		toolbar.setParent(vlayout);

		int qtyDiskusi = pertemuan.ambilJumlahPertemuanPunyaDiskusi();
		Toolbarbutton masuk = new MyToolbarbuttonConfig("Ikut Diskusi (" + qtyDiskusi + " diskusi)",
				"/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
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
		masuk.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
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
		masuk.setVisible(pert > 0 && tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
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
			masuk.setVisible(pert > 0 && tbmuser != null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
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
		if (siswas != null)
			for (GeneralValueObject generalValueObject : siswas) {
				TreeMap<String, String> d = pertemuan.ambilData("tugas", generalValueObject.getId().toString(),
						pertemuan.getMulai(), pertemuan.getSelesai());
				if (!d.isEmpty()) {
					pert += d.size();
				}

			}
		if (listGuru != null)
			for (Guru guru : listGuru) {
				TreeMap<String, String> d = pertemuan.ambilData("tugas", guru.getId().toString(), pertemuan.getMulai(),
						pertemuan.getSelesai());
				if (!d.isEmpty()) {
					pert += d.size();
				}
			}

		masuk = new MyToolbarbuttonConfig("Akses \"" + pertemuan.getJudultugas() + "\" (" + pert + " akses)",
				"/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(pert > 0 && tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
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
			if (siswas != null)
				for (GeneralValueObject generalValueObject : siswas) {
					TreeMap<String, String> d = tugasPertemuan.ambilData("tugas", generalValueObject.getId().toString(),
							tugasPertemuan.getMulai(), tugasPertemuan.getSelesai());
					if (!d.isEmpty()) {
						pert += d.size();
					}

				}
			if (listGuru != null)
				for (Guru guru : listGuru) {
					TreeMap<String, String> d = tugasPertemuan.ambilData("tugas", guru.getId().toString(),
							tugasPertemuan.getMulai(), tugasPertemuan.getSelesai());
					if (!d.isEmpty()) {
						pert += d.size();
					}
				}

			masuk = new MyToolbarbuttonConfig("Akses \"" + tugasPertemuan.getJudultugas() + "\" (" + pert + " akses)",
					"/img/svg/check2.svg");
			masuk.setStyle("font-size:9px;");
			masuk.setVisible(pert > 0 && tbmuser != null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
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
		if (siswas != null)
			for (GeneralValueObject generalValueObject : siswas) {
				TreeMap<String, String> d = pertemuan.ambilData("online", generalValueObject.getId().toString());
				if (!d.isEmpty()) {
					pert += d.size();
				}

			}
		if (listGuru != null)
			for (Guru guru : listGuru) {
				TreeMap<String, String> d = pertemuan.ambilData("online", guru.getId().toString());
				if (!d.isEmpty()) {
					pert += d.size();
				}
			}

		masuk = new MyToolbarbuttonConfig("Video Conf.(" + pert + " akses)", "/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
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
		if (siswas != null)
			for (GeneralValueObject generalValueObject : siswas) {
				TreeMap<String, String> d = pertemuan.ambilData("akses", generalValueObject.getId().toString());
				if (!d.isEmpty()) {
					pert += d.size();
				}

			}
		if (listGuru != null)
			for (Guru guru : listGuru) {
				TreeMap<String, String> d = pertemuan.ambilData("akses", guru.getId().toString());
				if (!d.isEmpty()) {
					pert += d.size();
				}
			}

		masuk = new MyToolbarbuttonConfig("Login & Akses (" + pert + " akses)", "/img/svg/check2.svg");
		masuk.setStyle("font-size:9px;");
		masuk.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && pert > 0);
		masuk.setTooltiptext("Siswa dan guru yang login dan akses (" + pert + " org)");
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
		for (GeneralValueObject generalValueObject : siswas) {
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
		masuk.setTooltiptext("Semua siswa yang belum absen dianggap Alpa");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin semua siswa yang belum absen dianggap Alpa ?", "Pertanyaan",
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

											for (GeneralValueObject generalValueObject : siswas) {
												Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
														Statusabsensi.class.getName(),
														pertemuan.retreiveAbsensiId(generalValueObject.getId()));
												if (statusabsensi == null || statusabsensi.getId()
														.equals(ConstantValues.BELUM_ABSEN.getId())) {
													statusabsensi = ConstantValues.TIDAK_ADA_ALASAN;
													pertemuan.populate(generalValueObject.getId(), statusabsensi,
															"Otomatis dijadikan alpa karena tidak ada keterangan", null,
															pertemuan.getWaktuMulai(), pertemuan.getWaktuSelesai(),
															"Siswa");
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

	/**
	 * Membangun panel daftar pengajuan izin/sakit untuk {@code pertemuan}: toolbar "Ajukan Izin
	 * atau Sakit" (membuka dialog form pengajuan baru, dengan lampiran bukti opsional) dan grid
	 * daftar pengajuan yang sudah ada, dirender lewat {@link SiswaIzinRenderer}.
	 *
	 * @param parentrow komponen ZK induk tempat panel disisipkan
	 * @param pertemuan pertemuan yang daftar izinnya ditampilkan/dikelola
	 * @return border layout siap disisipkan ke tata letak utama
	 */
	private Borderlayout createListSiswaIzin(Component parentrow, final Pertemuan pertemuan) {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(parentrow);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);
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

				List<Long> indsMhsJadwalPelajaran = new ArrayList<Long>();
				for (GeneralValueObject mhs : AbsensiSiswaHelper.populateSiswaDariPertemuan(pertemuan)) {
					indsMhsJadwalPelajaran.add(mhs.getId());
				}
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setValign("top");

				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
				final AmbilDataSiswaBanbox siswa;
				row.appendChild(siswa = new AmbilDataSiswaBanbox(indsMhsJadwalPelajaran));
				siswa.setWidth("90%");

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Siswa mhs = (Siswa) siswa.getAttribute("myValue");

						Radio s = status.getSelectedItem();

						if (mhs == null) {
							MyMessageboxConfig.show("Siswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						if (s == null) {
							MyMessageboxConfig.show("Status harus dipilih", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						if (katerangan.getValue().trim().equals("")) {
							MyMessageboxConfig.show("Alasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Session session = HibernateUtil.currentSession();
						PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) session
								.createCriteria(PengajuanIzinTidakMasukPerkuliahan.class)
								.add(Restrictions.eq("siswa", mhs)).add(Restrictions.eq("pertemuan", pertemuan))
								.setMaxResults(1).uniqueResult();
						if (pengajuanIzinTidakMasukPerkuliahan == null) {
							pengajuanIzinTidakMasukPerkuliahan = new PengajuanIzinTidakMasukPerkuliahan();
						} else if (pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
							MyMessageboxConfig.show(
									"Pengajian siswa " + mhs.getNama() + " telah disetujui, sehingga tidak bisa diubah",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						pengajuanIzinTidakMasukPerkuliahan.setPertemuan(pertemuan);
						pengajuanIzinTidakMasukPerkuliahan.setSiswa(mhs);
						pengajuanIzinTidakMasukPerkuliahan.setStatusabsensi((Statusabsensi) s.getAttribute("nilai"));
						pengajuanIzinTidakMasukPerkuliahan.setKeterangan(katerangan.getValue().trim());
						Common.refreshSaveOrUpdate(session, pengajuanIzinTidakMasukPerkuliahan);

						// pertemuan.retreiveAbsensiId(mhs.getId());

						// pertemuan.populate(mhs.getId(),
						// pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi(),
						// pengajuanIzinTidakMasukPerkuliahan.getKeterangan(),
						// pengajuanIzinTidakMasukPerkuliahan);
						//
						// Common.refreshSaveOrUpdate(session, pertemuan);

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

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		siswaIzinGrid = new MyGrid();
		siswaIzinGrid.setMold("paging");
		siswaIzinGrid.setPageSize(10000);
		siswaIzinGrid.setParent(center);
		siswaIzinGrid.setWidth("100%");
		siswaIzinGrid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(siswaIzinGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("OK");
		column.setWidth("16%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Siswa");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		reloadIzinAbsensi(pertemuan);

		return borderlayout;
	}

	/**
	 * Menuliskan kembali seluruh field form info pertemuan (topik, metode, guru tamu, buku
	 * rujukan, waktu mulai/selesai, status pertemuan/ujian, ruang, tanggal dan tanggal realisasi,
	 * konfigurasi kelas daring beserta seluruh tautan Zoom/BBB/Skype/WhatsApp/Meet/lain, batas
	 * toleransi waktu absen daring, lokasi dan jarak) ke entitas {@code pertemuan} di memori;
	 * bila {@code update} bernilai {@code true}, entitas di-refresh dari database lebih dulu
	 * (bila sudah tersimpan) dan disimpan setelah diperbarui.
	 *
	 * @param pertemuan pertemuan yang diperbarui dari isian form
	 * @param update    simpan perubahan ke database bila {@code true}; hanya perbarui objek di memori bila {@code false}
	 * @return selalu {@code true} (tidak ada jalur kegagalan validasi pada method ini)
	 */
	public boolean sesuaikan(Pertemuan pertemuan, boolean update) {

		if (update) {
			if (pertemuan.getId() != null) {
				HibernateUtil.currentSession().refresh(pertemuan);
			}
		}

		pertemuan
				.setWaktuMulai(waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
		pertemuan.setWaktuSelesai(
				waktuSelesai.getValue() == null ? null : Common.timeFormat2.get().format(waktuSelesai.getValue()));

		pertemuan.setGuruTamu2(guruTamu2.getValue());
		pertemuan.setMetodePembelajaran(metode.getValue());
		pertemuan.setTopik(topik.getValue().trim());
		// pertemuan.setBukuRujukan1(bukuRujukan1.getText());
		// pertemuan.setBukuRujukan2(bukuRujukan2.getText());
		pertemuan.setGuruTamu(guruTamu.getText());

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
				(Integer) (onlineMenggunakan == null || onlineMenggunakan.getValue() == null ? null
						: onlineMenggunakan.getSelectedItem().getValue()));
		pertemuan.setZoomLink(zoomLink == null ? "" : zoomLink.getValue().trim());
		pertemuan.setBbbLink(bbbLink == null ? "" : bbbLink.getValue().trim());
		pertemuan.setSkypeLink(skypeLink == null ? "" : skypeLink.getValue().trim());
		pertemuan.setWaLink(waLink == null ? "" : waLink.getValue().trim());
		pertemuan.setMeetLink(meetLink == null ? "" : meetLink.getValue().trim());
		pertemuan.setPerkulaiahnOnlineHarusSesuaiJadwal(perkulaiahnOnlineHarusSesuaiJadwal.isChecked());

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

	/** Membangun toolbar aksi massal ({@link #tampilBawah}) diikuti satu baris kehadiran per siswa ({@link #tampilRowAbsensi}) untuk seluruh {@link #siswas} pada {@code pertemuan}. */
	private void reloadAbsensiBaru(final Pertemuan pertemuan, final Rows rowsUtama) {

		jadwalPelajaran = pertemuan.getJadwalPelajaran();
		MyFormRow rowUtama = new MyFormRow();
		rowUtama.setParent(rowsUtama);
		tampilBawah(pertemuan, rowUtama);

		Integer tahap = null;
		Integer semester = jadwalPelajaran == null ? 1 : jadwalPelajaran.getSemester();

		int index = 1;
		for (GeneralValueObject mhs : this.siswas) {
			MyFormRow arg0 = new MyFormRow();
			arg0.setParent(rowsUtama);
			try {
				tampilRowAbsensi(arg0, index++, (Siswa) mhs, statusabsensis, status, semester, tbmuser, tahap,
						pertemuan);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	/** Memuat ulang daftar pengajuan izin/sakit {@code pertemuan} ke {@link #siswaIzinGrid}, dirender lewat {@link SiswaIzinRenderer}. */
	private void reloadIzinAbsensi(Pertemuan pertemuan) {

		List<PengajuanIzinTidakMasukPerkuliahan> pengajuanIzinTidakMasukPerkuliahans = pertemuan
				.ambilPengajuanIzinTidakMasukPerkuliahanTotal();
		ListModel strset = new SimpleListModel(pengajuanIzinTidakMasukPerkuliahans);
		siswaIzinGrid.setRowRenderer(new SiswaIzinRenderer(pertemuan));
		siswaIzinGrid.setModelCheckMobile(strset);
		siswaIzinGrid.setOddRowSclass("non-odd");

	}

	/**
	 * Mengisi {@code rowDataAbsen} dengan satu baris kehadiran {@code siswa} pada
	 * {@code pertemuan}: foto, identitas, kolom status kehadiran (combobox pilihan dari
	 * {@code statusabsensis}, disimpan langsung ke database saat berubah), waktu, dan catatan.
	 * Baris ini adalah unit terkecil yang membentuk grid kehadiran dibangun berulang oleh
	 * {@link #reloadAbsensiBaru}.
	 *
	 * @param rowDataAbsen  baris ZK yang diisi
	 * @param index         nomor urut baris (untuk kolom nomor)
	 * @param siswa         siswa yang kehadirannya ditampilkan/diedit pada baris ini
	 * @param statusabsensis daftar pilihan status kehadiran yang dapat dipilih
	 * @param status        status kehadiran default bila belum ada data tersimpan
	 * @param semester      semester konteks (dari jadwal pelajaran), memengaruhi tampilan tertentu
	 * @param tbmuser       pengguna yang sedang login, menentukan hak edit baris
	 * @param tahap         tahap penilaian terkait, boleh {@code null}
	 * @param pertemuan     pertemuan yang menjadi konteks kehadiran
	 */
	private void tampilRowAbsensi(Row rowDataAbsen, Integer index, final Siswa siswa,
			List<Statusabsensi> statusabsensis, Statusabsensi status, Integer semester, Tbmuser tbmuser, Integer tahap,
			final Pertemuan pertemuan) throws Exception {
		// TODO Auto-generated method stub

		MyGroupboxStyled group = new MyGroupboxStyled();
		group.setParent(rowDataAbsen);

		rowDataAbsen.setAttribute("siswa", siswa);
		String ket = pertemuan.retreiveAbsensiKeterangan(siswa.getId());
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

		final Textbox keterangan = new Textbox(ket);
		final Timebox waktuMulai = new ais.ui.util.MyTimebox();
		final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
		waktuMulai.setVisible(Common.bolehKonfigurasi("tampilkan_jam_masuk_absen_untuk_siswa", Konfigurasi.TIDAK_AKTIF));
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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3098");

						}
					}
					if (waktuSelesai.getValue() == null) {
						try {
							waktuSelesai.setValue(
									pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty()
											? null
											: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3108");

						}
					}
				} else {
					waktuMulai.setValue(null);
					waktuSelesai.setValue(null);
				}

				pertemuan.populate(siswa.getId(), statusabsensi, keterangan.getValue(), null,
						waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
						waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
						"Siswa");
				sesuaikan(pertemuan, false);
				Common.refreshUpdate(pertemuan);
			}
		};

		PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = pertemuan
				.ambilPengajuanIzinTidakMasukPerkuliahan(siswa);

		Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
				pertemuan.retreiveAbsensiId(siswa.getId()));
		if (statusabsensi == null) {
			statusabsensi = ConstantValues.BELUM_ABSEN;
		}

		Hbox hboxStatus = new Hbox();

		if ((tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getSiswa() == null)) {

			if (statusabsensi != null && pengajuanIzinTidakMasukPerkuliahan != null
					&& pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi() != null
					&& pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {

				if (!statusabsensi.getId().equals(pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi().getId())) {
					if (pertemuan.getId() != null) {
						HibernateUtil.currentSession().refresh(pertemuan);
					}
					pertemuan.populate(siswa.getId(), pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi(),
							keterangan.getValue(), null,
							waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
							waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
							"Siswa");
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
					String _mulai = pertemuan.retreiveAbsensiMulai(siswa.getId());
					if (_mulai != null && !_mulai.trim().isEmpty()) {
						waktuMulai.setValue(Common.timeFormat2.get().parse(_mulai));
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3184"); }

				hbox.appendChild(new ais.ui.util.MyLabelConfig("s.d"));
				hbox.appendChild(waktuSelesai);
				waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
				try {
					String _sampai = pertemuan.retreiveAbsensiSampai(siswa.getId());
					if (_sampai != null && !_sampai.trim().isEmpty()) {
						waktuSelesai.setValue(Common.timeFormat2.get().parse(_sampai));
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3193"); }
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
				new Label(statusabsensi == null ? "" : statusabsensi.getNama()).setParent(hbox);
				String wkt = pertemuan.retreiveAbsensiMulai(siswa.getId()) + " s.d "
						+ pertemuan.retreiveAbsensiSampai(siswa.getId());
				new Label(wkt.trim().equals("s.d") ? "" : wkt).setParent(hbox);

			}
		}

		group.appendChild(new MyCaptionStyled(siswa.getNim() + " / " + siswa.getNama()));

		Hbox hbox = new Hbox();
		hbox.appendChild(new Label(index + ". "));
		hbox.setWidth("90%");
		hbox.setParent(group);

		if (Common.bolehKonfigurasi("tampilkan_foto_di_absensi_kehadiran")) {
			CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox);
		}

		Vbox vboxStats = new Vbox();
		vboxStats.setParent(hbox);
		vboxStats.appendChild(hboxStatus);

		if (pengajuanIzinTidakMasukPerkuliahan != null && pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
			new MyLabelKecil(pengajuanIzinTidakMasukPerkuliahan.getKeterangan()).setParent(vboxStats);
		} else if ((tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null)) {

			Vbox hbox1 = new Vbox();
			hbox1.setWidth("90%");
			hbox1.setParent(vboxStats);

			keterangan.setVisible(false);
			keterangan.setCols(50);
			keterangan.setRows(4);
			keterangan.setParent(hbox1);
			keterangan.addEventListener("onChange", eventListener);

			List<String> urls = Common.getUrls(ket);
			String catat = ket.toString();
			catat = catat.replaceAll("\n", "<br>");
			for (String url : urls) {
				String u;
				if (url.contains("download")) {
					u = org.apache.commons.lang3.StringUtils.replace(url, "_", ",");
				} else {
					u = url;
				}
				catat = org.apache.commons.lang3.StringUtils.replace(catat, u,
						"<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>");
			}
			urls = null;
			if(catat != null && !catat.trim().isEmpty()) {
				catat = catat.replaceAll("(https?://\\S+)", "<a target='_blank' href=\"$1\">"+Common.getBahasaConfig("Klik di sini")+"</a>");
    		}
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

					String catat = keterangan.getValue().trim();

					List<String> urls = Common.getUrls(catat);
					catat = catat.replaceAll("\n", "<br>");
					for (String url : urls) {
						String u;
						if (url.contains("download")) {
							u = org.apache.commons.lang3.StringUtils.replace(url, "_", ",");
						} else {
							u = url;
						}
						catat = org.apache.commons.lang3.StringUtils.replace(catat, u,
								"<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>");
					}
					urls = null;

					buttonSelesai.setVisible(false);
					buttonUbah.setVisible(true);
					if(catat != null && !catat.trim().isEmpty()) {
						catat = catat.replaceAll("(https?://\\S+)", "<a target='_blank' href=\"$1\">"+Common.getBahasaConfig("Klik di sini")+"</a>");
		    		}
					keterangan.setVisible(false);
					ketComp.setContent(
							"<div style='font-size:11px;'><u>Keterangan</u>:</div><div style='font-size:10px;'>"
									+ (catat == null || catat.trim().isEmpty() ? "Tidak/belum ada keterangan" : catat)
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

			List<String> urls = Common.getUrls(ket);
			String catat = ket.toString();
			catat = catat.replaceAll("\n", "<br>");
			for (String url : urls) {
				String u;
				if (url.contains("download")) {
					u = org.apache.commons.lang3.StringUtils.replace(url, "_", ",");
				} else {
					u = url;
				}
				catat = org.apache.commons.lang3.StringUtils.replace(catat, u,
						"<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>");
			}
			urls = null;
			if(catat != null && !catat.trim().isEmpty()) {
				catat = catat.replaceAll("(https?://\\S+)", "<a target='_blank' href=\"$1\">"+Common.getBahasaConfig("Klik di sini")+"</a>");
    		}
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
				contentVideo = StringUtils.split(contentVideo, "&")[0];
				contentVideo = StringUtils.split(contentVideo, "/")[0];
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

	}

	class SiswaIzinRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser;
		private Pertemuan pertemuan;

		/** Membangun renderer untuk daftar pengajuan izin/sakit {@code pertemuan}, menangkap pengguna yang sedang login untuk keperluan pemeriksaan hak akses per baris. */
		public SiswaIzinRenderer(Pertemuan pertemuan) {
			tbmuser = Common.getCurrentUser();
			this.pertemuan = pertemuan;
		}

		/**
		 * Perenderan satu baris pengajuan izin/sakit: checkbox persetujuan (staf saja; menyetujui
		 * langsung memperbarui status kehadiran siswa pada pertemuan lewat
		 * {@link Pertemuan#populate}), foto dan identitas siswa, status dan keterangan pengajuan,
		 * lampiran bukti, dan tombol hapus (hanya bila belum disetujui, dan hanya bagi staf atau
		 * pemilik pengajuan).
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) arg1;
			arg0.setValign("top");
			if (tbmuser.getSiswa() == null) {
				final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Setujui");
				checkboxConfig.setChecked(pengajuanIzinTidakMasukPerkuliahan.getDiizinkan());
				checkboxConfig.setParent(arg0);
				checkboxConfig.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pengajuanIzinTidakMasukPerkuliahan.setDiizinkan(checkboxConfig.isChecked());
						Common.refreshSaveOrUpdate(pengajuanIzinTidakMasukPerkuliahan);

						pertemuan.populate(pengajuanIzinTidakMasukPerkuliahan.getSiswa().getId(),
								pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi(),
								pengajuanIzinTidakMasukPerkuliahan.getKeterangan(), null, pertemuan.getWaktuMulai(),
								pertemuan.getWaktuSelesai(), "Siswa");
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
			CommonMedia.tampilkanGambarKecil(pengajuanIzinTidakMasukPerkuliahan.getSiswa()).setParent(hbox);

			Vbox vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(hbox);
			new Label(pengajuanIzinTidakMasukPerkuliahan.getSiswa().getNim() + " - "
					+ pengajuanIzinTidakMasukPerkuliahan.getSiswa().getNama()).setParent(vbox);

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
			button.setVisible(!pengajuanIzinTidakMasukPerkuliahan.getDiizinkan() && ((tbmuser.getSiswa() != null
					&& tbmuser.getSiswa().getId().equals(pengajuanIzinTidakMasukPerkuliahan.getSiswa().getId()))
					|| tbmuser.getSiswa() == null));
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
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});
			button.setParent(arg0);
		}
	}

	/**
	 * Membangun widget status kehadiran asisten kelas ({@code asisten}) pada {@code pertemuan}:
	 * bagi staf (bukan konteks satu siswa tertentu), berupa combobox status kehadiran (dari
	 * daftar status yang bukan "belajar"/"cuti"/"dinas") ditambah rentang waktu dan catatan,
	 * yang tersimpan langsung ke {@code pertemuan} saat berubah dan mengisi otomatis waktu
	 * mulai/selesai dari jam pertemuan bila status berkode {@code "M"} (masuk); bagi siswa/calon
	 * siswa (tampilan read-only), hanya label nama status.
	 *
	 * @param asisten   asisten kelas yang statusnya ditampilkan; {@link Label} kosong bila {@code null}
	 * @param pertemuan pertemuan yang menjadi konteks
	 * @param siswa     bila diisi, tampilan read-only untuk siswa ini
	 * @param calonSiswa bila diisi, tampilan read-only untuk calon siswa ini
	 * @param sesuaikan callback yang dipanggil setelah status kehadiran berubah, untuk menyinkronkan tampilan induk
	 * @return komponen widget siap disisipkan
	 */
	public static Component createStatusKehadiran(final Siswa asisten, final Pertemuan pertemuan, Siswa siswa,
			CalonSiswa calonSiswa, final EventListener sesuaikan) {
		if (asisten == null) {
			return new Label();
		}

		Statusabsensi statusabsensi = null;

		if (pertemuan.getId() != null) {

			statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					pertemuan.retreiveAbsensiId(asisten.getId()));
		}

		if (siswa == null && calonSiswa == null) {

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
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3553");

							}
						}
						if (waktuSelesai.getValue() == null) {
							try {
								waktuSelesai.setValue(pertemuan.getWaktuSelesai() == null
										|| pertemuan.getWaktuSelesai().trim().isEmpty() ? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3562");

							}
						}
					} else {
						waktuMulai.setValue(null);
						waktuSelesai.setValue(null);
					}
					pertemuan.populate(asisten.getId(), statusabsensi,
							waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
							waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3588");

			}

			hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
			hbox.appendChild(waktuSelesai);
			waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				waktuSelesai.setValue(Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(asisten.getId())));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3597");

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

	/**
	 * Membangun widget input status kehadiran satu {@code guru} pada {@code pertemuan} untuk
	 * konteks staf (editable): combobox status kehadiran beserta komponen tambahan yang
	 * menyesuaikan status terpilih (mis. rentang waktu untuk status masuk, kolom catatan/alasan
	 * untuk status lain), tersimpan langsung ke {@code pertemuan} saat berubah dan memicu
	 * {@code sesuaikan} untuk menyinkronkan tampilan induk.
	 *
	 * @param statusabsensi     status kehadiran guru saat ini (nilai awal komponen)
	 * @param pertemuan         pertemuan yang menjadi konteks
	 * @param guru              guru yang statusnya diedit
	 * @param tanggalRealisasi  komponen tanggal realisasi terkait, ikut disinkronkan bila relevan
	 * @param sesuaikan         callback yang dipanggil setelah status berubah
	 * @return komponen widget siap disisipkan
	 */
	public static Component boleh(Statusabsensi statusabsensi, final Pertemuan pertemuan, final Guru guru,
			final MyDatebox tanggalRealisasi, final EventListener sesuaikan) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		String ket = pertemuan.retreiveAbsensiKeterangan(guru.getId());
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
		final EventListener ubahRealisasi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggalRealisasi != null) {

					if (Common.bolehKonfigurasi("tanggal_realisasi_jadwalPelajaran_harus_diisi_sesuai_pertemuan_jadwalPelajaran")) {
						tanggalRealisasi.setDisabled(!pertemuan.apakahAdaGuruYangMasuk());

						if (tbmuser != null && tbmuser.ambilGuru() != null && (pertemuan.getJadwalPelajaran() != null
								&& !pertemuan.getJadwalPelajaran().getGuruBisaMerubahTanggalJadwalPelajaran())) {
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

		if ((tbmuser != null && tbmuser.ambilGuru() != null
				&& (pertemuan.getJadwalPelajaran() != null
						&& !pertemuan.getJadwalPelajaran().getGuruBisaMerubahTanggalJadwalPelajaran()))
				|| (tbmuser != null && tbmuser.ambilGuru() != null && Common.bolehKonfigurasi("guru_wajib_menggunakan_tombol_start_stop_di_absensi", Konfigurasi.TIDAK_AKTIF))) {
			Vbox vbox = new Vbox();

			final Textbox catatan = new Textbox(ket);
			catatan.setWidth("120px");
			catatan.setRows(2);

			Date m = null;
			try {
				m = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(guru.getId()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3661");
			}

			Date s = null;
			try {
				s = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(guru.getId()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3667");
			}

			final Button masuk = new Button("Klik Tombol ini jika Anda mulai mengajar", "/img/Start-icon.png");
			if (m != null) {
				masuk.setLabel("Mulai mengajar " + Common.timeFormat2.get().format(m));
				masuk.setDisabled(true);
			}

			final MyCheckboxConfig selesaikanOtomatis = new MyCheckboxConfig(
					"Selesaikan jam mengajar otomatis sesuai rencana jadwalPelajaran, yaitu pukul "
							+ pertemuan.getJadwalPelajaran().getWaktuSelesai());

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
										pertemuan.populate(guru.getId(), ConstantValues.MASUK, catatan.getValue(), null,
												Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()), "", "Guru");
										sesuaikan.onEvent(new Event("", null, pertemuan));
										Common.refreshUpdate(pertemuan);
										masuk.setDisabled(true);
										keluar.setVisible(true);

										Date m = null;
										try {
											m = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(guru.getId()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3714");
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
										pertemuan.populate(guru.getId(), ConstantValues.MASUK, catatan.getValue(), null,
												mm == null ? Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
														: Common.timeFormat2.get().format(mm),
												Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()), "Guru");
										sesuaikan.onEvent(new Event("", null, pertemuan));
										Common.refreshUpdate(pertemuan);
										keluar.setDisabled(true);
										masuk.setDisabled(true);

										Date s = null;
										try {
											s = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(guru.getId()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3757");
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
							"Apakah Anda ingin menyelesaikan jam mengajar otomatis sesuai rencana jadwalPelajaran ?",
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
										try {
											m = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(guru.getId()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3791");
										}

										String mulai = m == null
												? Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
												: Common.timeFormat2.get().format(m);
										String selesai = pertemuan.getJadwalPelajaran().getWaktuSelesai().toString()
												+ "000000";

										try {
											selesai = selesai.substring(0, 5);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3802");
											// TODO: handle exception
										}

										System.out.println("mulai => " + mulai + ", selesai => " + selesai);

										pertemuan.populate(guru.getId(), ConstantValues.MASUK, catatan.getValue(), null,
												mulai, selesai, "Guru");
										sesuaikan.onEvent(new Event("", null, pertemuan));
										Common.refreshUpdate(pertemuan);
										keluar.setDisabled(true);
										masuk.setDisabled(true);
										keluar.setVisible(true);
										masuk.setVisible(true);
										selesaikanOtomatis.setDisabled(true);

										Date s = null;
										try {
											s = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(guru.getId()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3821");
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
						m = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(guru.getId()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3850");
					}

					Date s = null;
					try {
						s = Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(guru.getId()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3856");
					}

					pertemuan.populate(guru.getId(), ConstantValues.MASUK, catatan.getValue(), null,
							m == null ? "" : Common.timeFormat2.get().format(m),
							s == null ? "" : Common.timeFormat2.get().format(s), "Guru");
					sesuaikan.onEvent(new Event("", null, pertemuan));
					Common.refreshUpdate(pertemuan);

					ubahRealisasi.onEvent(arg0);
				}
			});

			if (guru != null && !guru.getId().equals(tbmuser.getGuru().getId())) {
				Common.freeze(vbox, true);
			}

			try {
				ubahRealisasi.onEvent(null);
			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3876");
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

			final Textbox catatan = new Textbox(pertemuan.retreiveAbsensiKeterangan(guru.getId()));
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
						HibernateUtil.currentSession().refresh(pertemuan);
					}

					Statusabsensi statusabsensi = (Statusabsensi) kehadiran.getSelectedItem().getValue();

					if (statusabsensi.getKode() != null && statusabsensi.getKode().trim().equals("M")) {

						if (waktuMulai.getValue() == null) {
							try {
								waktuMulai.setValue(
										pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
												? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3939");

							}
						}
						if (waktuSelesai.getValue() == null) {
							try {
								waktuSelesai.setValue(pertemuan.getWaktuSelesai() == null
										|| pertemuan.getWaktuSelesai().trim().isEmpty() ? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3948");

							}
						}
					} else {
						waktuMulai.setValue(null);
						waktuSelesai.setValue(null);
					}

					pertemuan.populate(guru.getId(), statusabsensi, catatan.getValue(), null,
							waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
							waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
							"Guru");
					sesuaikan.onEvent(new Event("", null, pertemuan));
					Common.refreshUpdate(pertemuan);

					ubahRealisasi.onEvent(arg0);
				}
			};

			kehadiran.addEventListener("onChange", eventListener);
			kehadiran.setParent(vbox);
			new Label(ais.common.Common.getBahasaConfig("Catatan:")).setParent(vbox);
			catatan.setParent(vbox);

			hbox.appendChild(new ais.ui.util.MyLabelConfig("Wkt :"));
			hbox.appendChild(waktuMulai);
			waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				String absensiMulai = pertemuan.retreiveAbsensiMulai(guru.getId());
				waktuMulai.setValue(absensiMulai == null || absensiMulai.trim().length() == 0 ? null
						: Common.timeFormat2.get().parse(absensiMulai.trim()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3978");

			}

			hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
			hbox.appendChild(waktuSelesai);
			waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				String absensiSelesai = pertemuan.retreiveAbsensiSampai(guru.getId());
				waktuSelesai.setValue(absensiSelesai == null || absensiSelesai.trim().length() == 0 ? null
						: Common.timeFormat2.get().parse(absensiSelesai.trim()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:3987");

			}

			waktuMulai.addEventListener("onChange", eventListener);
			waktuSelesai.addEventListener("onChange", eventListener);
			catatan.addEventListener("onChange", eventListener);

			hbox.setParent(vbox);

			try {
				ubahRealisasi.onEvent(null);

			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:4001");
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

	/** Adaptor: mengubah koleksi {@link CommonVO} (pembungkus generik) berisi {@link Guru} menjadi {@link Collection}{@code <Guru>} lalu mendelegasikan ke {@link #createStatusKehadiran(Collection, Pertemuan)}. */
	public static Component createStatusKehadiranData(Collection<CommonVO> dataGuru, final Pertemuan pertemuan)
			throws Exception {
		Collection<Guru> collection = new ArrayList<Guru>();
		for (CommonVO commonVO : dataGuru) {
			Guru guru = (Guru) commonVO.getValueObject();
			collection.add(guru);
		}
		Component d = createStatusKehadiran(collection, pertemuan);
		collection = null;
		return d;
	}

	/**
	 * Membangun widget ringkasan status kehadiran (read-only) untuk sekelompok {@code gurus}
	 * pada {@code pertemuan}: satu kartu kecil (foto, nama, status kehadiran, jam) per guru,
	 * disusun dalam grid maksimal 3 kolom bila jumlah guru lebih dari 3 agar tetap ringkas.
	 *
	 * @param gurus     kumpulan guru yang statusnya ditampilkan
	 * @param pertemuan pertemuan yang menjadi konteks
	 * @return komponen ringkasan siap disisipkan, {@link Label} kosong bila {@code gurus} kosong
	 */
	public static Component createStatusKehadiran(Collection<Guru> gurus, final Pertemuan pertemuan) throws Exception {
		if (gurus.isEmpty()) {
			return new Label();
		}

		Hbox hbox = new Hbox();

		if (gurus.size() > 3) {

			Vbox vboxBaru = new Vbox();
			vboxBaru.setParent(hbox);

			Hbox hboxBaru = new Hbox();
			hboxBaru.setParent(vboxBaru);
			int size = 0;

			for (Guru guru : gurus) {
				if (guru != null && guru.getId() != null) {

					if (size % 3 == 0) {
						hboxBaru = new Hbox();
						hboxBaru.setParent(vboxBaru);
					}
					size++;

					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(guru.getId()));

					Vbox vbox1 = new Vbox();
					vbox1.setParent(hboxBaru);
					vbox1.appendChild(CommonMedia.tampilkanGambarKecil(guru));
					vbox1.appendChild(new MyLabelAgakKecil(guru.getNama()));
					vbox1.appendChild(new MyLabelAgakKecil(
							"Kehadiran : " + (statusabsensi == null || statusabsensi.getNama() == null ? "-"
									: Common.getBahasaConfig(statusabsensi.getNama()))));
					String wkt = pertemuan.retreiveAbsensiMulai(guru.getId()) + " s.d "
							+ pertemuan.retreiveAbsensiSampai(guru.getId());
					new MyLabelKecil(wkt.trim().equals("s.d") ? "" : "Pukul : " + wkt).setParent(vbox1);
				}
			}

		} else {
			for (Guru guru : gurus) {
				if (guru != null && guru.getId() != null) {

					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(guru.getId()));

					Vbox vbox1 = new Vbox();
					vbox1.setParent(hbox);
					vbox1.appendChild(CommonMedia.tampilkanGambarKecil(guru));
					vbox1.appendChild(new MyLabelAgakKecil(guru.getNama()));
					vbox1.appendChild(new MyLabelAgakKecil(
							"Kehadiran : " + (statusabsensi == null || statusabsensi.getNama() == null ? "-"
									: Common.getBahasaConfig(statusabsensi.getNama()))));
					String wkt = pertemuan.retreiveAbsensiMulai(guru.getId()) + " s.d "
							+ pertemuan.retreiveAbsensiSampai(guru.getId());
					new MyLabelKecil(wkt.trim().equals("s.d") ? "" : "Pukul : " + wkt).setParent(vbox1);
				}
			}
		}

		if (pertemuan.getGuruPengganti() != null) {
			Guru dsnPengganti = (Guru) (pertemuan.getGuruPengganti() == null ? null
					: ConstantValues.ambil(Guru.class.getName(), pertemuan.getGuruPengganti()));
			if (dsnPengganti != null) {
				Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
						pertemuan.retreiveAbsensiId(dsnPengganti.getId()));

				Vbox vbox1 = new Vbox();
				vbox1.setParent(hbox);
				vbox1.appendChild(CommonMedia.tampilkanGambarKecil(dsnPengganti));
				vbox1.appendChild(new MyLabelAgakKecil("Guru Pengganti :"));
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
		if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
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

			class PertemuanChangeListener implements EventListener {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Pegawai petugas = (Pegawai) pegawai.getAttribute("pegawai");
					Pegawai petugas2 = (Pegawai) pegawai2.getAttribute("pegawai");
					Pegawai petugas3 = (Pegawai) pegawai3.getAttribute("pegawai");
					Pegawai petugas4 = (Pegawai) pegawai4.getAttribute("pegawai");
					System.out.println("========= Ganti Waktu ujian =========");

					pertemuan.setPetugas(petugas == null ? null : petugas.getId());
					pertemuan.setPetugas2(petugas2 == null ? null : petugas2.getId());
					pertemuan.setPetugas3(petugas3 == null ? null : petugas3.getId());
					pertemuan.setPetugas4(petugas4 == null ? null : petugas4.getId());
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

	/**
	 * Membangun widget status kehadiran satu {@code guru} pada {@code pertemuan}, varian paling
	 * lengkap yang menggabungkan seluruh aturan: bila jadwal pelajaran mensyaratkan input sesuai
	 * jadwal dan waktu saat ini berada di luar rentang jam pertemuan, guru pengampu hanya
	 * melihat status kehadirannya sebagai label read-only (tidak dapat mengubah di luar jendela
	 * waktu tersebut). Selain itu: bagi staf (bukan konteks siswa tertentu), didelegasikan ke
	 * {@link #boleh} untuk widget editable; bagi siswa/calon siswa, ditampilkan sebagai
	 * ringkasan read-only (status dan keterangan).
	 *
	 * @param guru             guru yang statusnya ditampilkan/diedit; {@link Label} kosong bila {@code null}
	 * @param pertemuan        pertemuan yang menjadi konteks
	 * @param siswa            bila diisi, tampilan read-only untuk siswa ini
	 * @param calonSiswa       bila diisi, tampilan read-only untuk calon siswa ini
	 * @param tanggalRealisasi komponen tanggal realisasi terkait, diteruskan ke {@link #boleh} bila relevan
	 * @param sesuaikan        callback yang dipanggil setelah status berubah
	 * @return komponen widget siap disisipkan
	 */
	public static Component createStatusKehadiran(final Guru guru, final Pertemuan pertemuan, Siswa siswa,
			CalonSiswa calonSiswa, final MyDatebox tanggalRealisasi, final EventListener sesuaikan) {
		if (guru == null) {
			return new Label();
		}

		Statusabsensi statusabsensi = null;
		if (pertemuan.getId() != null) {

			statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					pertemuan.retreiveAbsensiId(guru.getId()));

		}

		if (statusabsensi == null) {
			statusabsensi = ConstantValues.BELUM_ABSEN;
		}

		Date curreDate = ais.ui.util.WaktuUtil.getDate();
		Date mulai = null;
		Date selesai = null;
		try {
			mulai = Common.timeFormat2.get().parse(pertemuan.getWaktuMulai());
			selesai = Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AbsensiSiswaHelper.java:4226");

		}
		if (pertemuan != null && pertemuan.getJadwalPelajaran() != null) {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.ambilGuru() != null
					&& pertemuan.getJadwalPelajaran().getKehadiranGuruHarusDiinputSesuaiJadwal()
					&& ((mulai != null && curreDate.before(mulai) || (selesai != null && curreDate.after(selesai))))) {
				Vbox vbox = new Vbox();
				vbox.appendChild(
						new Label(statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama())));

				String wkt = guru == null ? ""
						: pertemuan.retreiveAbsensiMulai(guru.getId()) + " s.d "
								+ pertemuan.retreiveAbsensiSampai(guru.getId());
				vbox.appendChild(new Label(wkt.trim().equals("s.d") ? "" : wkt));
				return vbox;
			}
		}

		if (siswa == null && calonSiswa == null) {
			return boleh(statusabsensi, pertemuan, guru, tanggalRealisasi, sesuaikan);
		} else {
			String ket = pertemuan.retreiveAbsensiKeterangan(guru.getId());
			ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
			Vbox vbox = new Vbox();
			vbox.appendChild(new Label(statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama())));

			String wkt = guru == null ? ""
					: pertemuan.retreiveAbsensiMulai(guru.getId()) + " s.d "
							+ pertemuan.retreiveAbsensiSampai(guru.getId());
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
}
