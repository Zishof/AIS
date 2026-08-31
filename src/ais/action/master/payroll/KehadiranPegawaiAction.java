package ais.action.master.payroll;


import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.LocalDateTime;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.AbsensiKehadiranPegawaiPerHariHelper;
import ais.action.master.helper.ProsesKehadiranDosen;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.helper.DashboardKehadiranExpert;
import ais.action.master.payroll.helper.ProsesAbsensiPegawai;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.format1.akademik.LaporanAbsensiPegawai;
import ais.action.report.format1.akademik.LaporanAbsensiPegawaiPerHari;
import ais.action.report.format1.akademik.LaporanAbsensiPegawaiPerOrang;
import ais.action.report.format1.akademik.LaporanAbsensiPegawaiPerOrangHorizontal;
import ais.action.report.format1.akademik.LaporanKehadiranDosen;
import ais.action.report.format1.akademik.LaporanKehadiranMahasiswa;
import ais.action.report.format1.payroll.LaporanCutiPegawai;
import ais.action.report.format1.payroll.LaporanLembur;
import ais.action.report.format1.payroll.LaporanRekapitulasiAbsen;
import ais.action.report.format1.sekolah.LaporanRekapAbsenGuruPiketHarian;
import ais.action.report.format1.sekolah.LaporanRekapAbsenPiketHarian;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk kehadiran pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String
 * EVENT_RENDER_DASHBOARD_KEHADIRAN}, {@code String EVENT_SEARCH_DEFAULT_KEHADIRAN}, {@code Paging paging},
 * {@code MyGrid grid}, {@code Textbox kodePegawai}, {@code West absenLangsung}, {@code Label label_universitas},
 * {@code Label labelCariPegawai}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()},
 * {@code initDashboardKehadiran()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * tampilkanLoadingDashboardKehadiran()}, {@code onSearchDefault()}); mutasi data ({@code onProsesKehadiran()},
 * {@code onProsesKehadiranPegawai()}); pelaporan/ekspor ({@code renderDashboardKehadiran()}); operasi domain
 * lain ({@code onCuti()}, {@code onLaporanRekapPerPegawai()}, {@code onLaporanTidakHadir()}, {@code onGuru()},
 * {@code onSiswa()}, {@code onMahasiswa()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class KehadiranPegawaiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private static final String EVENT_RENDER_DASHBOARD_KEHADIRAN = "onRenderDashboardKehadiran";
	private static final String EVENT_SEARCH_DEFAULT_KEHADIRAN = "onSearchDefaultKehadiranPegawai";

	private Paging paging;
	private MyGrid grid;

	private Textbox kodePegawai;
	private West absenLangsung;

	private Label label_universitas;
	private Label labelCariPegawai;

	private AmbilDataSatuanKerjaBanbox searchparent;
	private Tbmuser tbmuser;

	protected Tabpanel laporanAbsensiPegawai;
	protected Tabpanel laporanAbsensiPerPegawai;
	protected Tabpanel laporanRekapPerPegawai;
	protected Tabpanel harianAbsensiPegawai;
	protected Tabpanel laporanAbsensiPerTanggal;
	protected Tabpanel laporanAbsensiPerUnit;
	protected Tabpanel laporanAbsensiPerRinci;
	protected Tabpanel prosesKehadiranTab; 
	protected Tabpanel laporanCuti;
	protected Tabpanel laporanTidakHadir;

	private Textbox cariPegawai;

	/** Saringan jenis pengguna presensi (Semua/Dosen/Guru/Pegawai/Siswa/Mahasiswa) — lihat presensi.zul. */
	private org.zkoss.zul.Combobox jenisPengguna;

	private MyToolbarbuttonConfig find;

	public void onCuti(Event event) {

		if (laporanCuti.getChildren().size() == 0) {
			LaporanCutiPegawai laporan = new LaporanCutiPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanCuti);
		}
	}

	public void onLaporanRekapPerPegawai(Event event) {

		if (laporanRekapPerPegawai.getChildren().size() == 0) {
			LaporanAbsensiPegawaiPerOrangHorizontal laporan = new LaporanAbsensiPegawaiPerOrangHorizontal();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanRekapPerPegawai);
		}
	}

	public void onLaporanTidakHadir(Event event) {

		if (laporanTidakHadir.getChildren().size() == 0) {
			LaporanRekapitulasiAbsen laporan = new LaporanRekapitulasiAbsen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanTidakHadir);
		}
	}

	protected Tabpanel laporanGuru;

	public void onGuru(Event event) throws Exception {

		if (laporanGuru.getChildren().size() == 0) {
			LaporanRekapAbsenGuruPiketHarian laporan = new LaporanRekapAbsenGuruPiketHarian();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanGuru);
		}
	}

	protected Tabpanel laporanSiswa;

	public void onSiswa(Event event) throws Exception {

		if (laporanSiswa.getChildren().size() == 0) {
			LaporanRekapAbsenPiketHarian laporan = new LaporanRekapAbsenPiketHarian();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanSiswa);
		}
	}

	protected Tabpanel laporanMahasiswa;

	public void onMahasiswa(Event event) throws Exception {

		if (laporanMahasiswa.getChildren().size() == 0) {
			LaporanKehadiranMahasiswa laporan = new LaporanKehadiranMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanMahasiswa);
		}
	}

	protected Tabpanel laporanDosen;

	public void onDosen(Event event) throws Exception {

		if (laporanDosen.getChildren().size() == 0) {
			LaporanKehadiranDosen laporan = new LaporanKehadiranDosen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanDosen);
		}
	}

	protected Tabpanel laporanLembur;

	public void onLaporanLembur(Event event) {

		if (laporanLembur.getChildren().size() == 0) {
			LaporanLembur laporan = new LaporanLembur();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanLembur);
		}
	}

	public void onProsesKehadiran(Event event) {

		if (prosesKehadiranTab.getChildren().size() == 0) {
			ProsesKehadiranDosen laporan = new ProsesKehadiranDosen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.prosesKehadiranTab);
		}
	}

	protected Tabpanel prosesKehadiranPegawaiTab;

	public void onProsesKehadiranPegawai(Event event) {

		if (prosesKehadiranPegawaiTab.getChildren().size() == 0) {
			ProsesAbsensiPegawai laporan = new ProsesAbsensiPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.prosesKehadiranPegawaiTab);
		}
	}

	protected Tabpanel laporanQrPerTanggal;

	public void onPerHari(Event event) {

		if (harianAbsensiPegawai.getChildren().size() == 0) {
			AbsensiKehadiranPegawaiPerHariHelper laporan = new AbsensiKehadiranPegawaiPerHariHelper();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.harianAbsensiPegawai);
		}
	}

	public void onLaporanAbsensiPegawai(Event event) {

		if (laporanAbsensiPegawai.getChildren().size() == 0) {
			LaporanAbsensiPegawai laporan = new LaporanAbsensiPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPegawai);
		}
	}

	public void onLaporanAbsensiPerPegawai(Event event) {

		if (laporanAbsensiPerPegawai.getChildren().size() == 0) {
			LaporanAbsensiPegawaiPerOrang laporan = new LaporanAbsensiPegawaiPerOrang();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPerPegawai);
		}
	}

	public void onLaporanAbsensiPerTanggal(Event event) {

		if (laporanAbsensiPerTanggal.getChildren().size() == 0) {
			LaporanAbsensiPegawaiPerHari laporan = new LaporanAbsensiPegawaiPerHari();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPerTanggal);
		}
	}

	public void onLaporanAbsensiPerUnit(Event event) {

		if (laporanAbsensiPerUnit.getChildren().size() == 0) {
			ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerOrang laporan = new ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerOrang();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPerUnit);
		}
	}

	public void onLaporanAbsensiPerRinci(Event event) {

		if (laporanAbsensiPerRinci.getChildren().size() == 0) {
			ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerPegawai laporan = new ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPerRinci);
		}
	}

	private MyDatebox mulai;
	private MyDatebox sampai;

	private LocalDateTime startWaktu = new LocalDateTime(0L);
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	protected Tabpanel tabDashboardPanel;
	private Component dashboardKehadiranHost;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initBahasaParameter(execution.getParameter("lang"));

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];
		tbmuser = Common.getCurrentUser();

		if (laporanSiswa != null) { laporanSiswa.setVisible(ya); }
		if (laporanGuru != null) { laporanGuru.setVisible(ya); }

		laporanSiswa.getLinkedTab().setVisible(ya);
		laporanGuru.getLinkedTab().setVisible(ya);

		if (laporanMahasiswa != null) { laporanMahasiswa.setVisible(pt); }
		if (laporanDosen != null) { laporanDosen.setVisible(pt); }

		laporanMahasiswa.getLinkedTab().setVisible(pt);
		laporanDosen.getLinkedTab().setVisible(pt);

		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();

		int loginTimeout = 3000;
		request.getSession().setMaxInactiveInterval(loginTimeout * 60);

		if (mulai != null) { mulai.setReadonly(true); }
		if (sampai != null) { sampai.setReadonly(true); }

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);

		if (mulai != null) { mulai.setValue(calendar.getTime()); }
		if (sampai != null) { sampai.setValue(WaktuUtil.getDate()); }

		tbmuser = Common.getCurrentUser();
		absenLangsung
				.setVisible(tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getAbsenLangsung());

		if (label_universitas != null) {
			label_universitas.setValue(Common.getKonfigurasi("label_universitas", "Universitas").getNilai());
		}
		Common.ROOT = execution.getContextPath();

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		kodePegawai.focus();

		if (find != null && (tbmuser.getPegawai() != null || tbmuser.getDosen() != null || tbmuser.getGuru() != null)) {
			boolean mobileAndroid = Common.isMobile();
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Absen Online", "/img/svg/fingerprint.svg");
			DashboardTimelinePertemuan.createScanAbsenPegawai(tbmuser, mobileAndroid, toolbarbutton);
			Common.appendKeToolbar(toolbarbutton, find, comp);

			toolbarbutton = new MyToolbarbuttonConfig("Scan Qrcode", "/img/svg/qrcode-scan.svg");
			DashboardTimelinePertemuan.createScanQrCode(tbmuser, true, false, toolbarbutton);
			Common.appendKeToolbar(toolbarbutton, find, comp);
		}

		// ---> PEMANGGILAN DASBOR DENGAN LOADING INLINE, BUKAN POPUP <---
		if (tabDashboardPanel != null) {
			dashboardKehadiranHost = ais.ui.util.PortalUiHelper.panelInParent(tabDashboardPanel, "100%",
					"Dasbor Kehadiran Pegawai",
					"Ringkasan pola kehadiran, lembur, keterlambatan, dan tren absensi pegawai dalam periode terpilih.",
					null);
		}
		registerDashboardKehadiranEvents();
		// Saat halaman dibuka: JANGAN auto-hitung dashboard (berat & memblokir tab).
		// Cukup tampilkan filter + ajakan; klik "Tampilkan Analisis" untuk memuat.
		initDashboardKehadiran(null, null, null, null, "waktu_desc", false);
		if (tabDashboardPanel != null) {
			org.zkoss.zk.ui.event.Events.echoEvent(EVENT_SEARCH_DEFAULT_KEHADIRAN, tabDashboardPanel, null);
		} else {
			onSearchDefault(null);
		}

	}

	private void registerDashboardKehadiranEvents() {
		if (tabDashboardPanel == null) {
			return;
		}

		tabDashboardPanel.addEventListener(EVENT_RENDER_DASHBOARD_KEHADIRAN, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				DashboardKehadiranRequest request = (DashboardKehadiranRequest) event.getData();
				if (request != null) {
					renderDashboardKehadiran(request.filterMulai, request.filterSampai, request.filterHariAktif,
							request.filterSatker, request.sortBy, request.muatData);
				}
			}
		});

		tabDashboardPanel.addEventListener(EVENT_SEARCH_DEFAULT_KEHADIRAN, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	private void initDashboardKehadiran(final Date filterMulai, final Date filterSampai,
			final List<Integer> filterHariAktif, final ais.database.model.rab.SatuanKerja filterSatker,
			final String sortBy, final boolean muatData) throws Exception {

		if (tabDashboardPanel == null) {
			return;
		}

		// Hanya tampilkan spinner bila memang akan menghitung data (klik "Tampilkan
		// Analisis"). Saat halaman dibuka (muatData=false) cukup tampil filter + ajakan,
		// sehingga tab lain langsung bisa diklik tanpa menunggu.
		if (muatData) {
			tampilkanLoadingDashboardKehadiran();
		}
		DashboardKehadiranRequest request = new DashboardKehadiranRequest(filterMulai, filterSampai, filterHariAktif,
				filterSatker, sortBy, muatData);
		org.zkoss.zk.ui.event.Events.echoEvent(EVENT_RENDER_DASHBOARD_KEHADIRAN, tabDashboardPanel, request);
	}

	private void tampilkanLoadingDashboardKehadiran() {
		if (tabDashboardPanel == null) {
			return;
		}

		Component renderTarget = dashboardKehadiranHost != null ? dashboardKehadiranHost : tabDashboardPanel;
		Common.clear(renderTarget);
		final Vbox containerDasborGrid = new Vbox();
		containerDasborGrid.setWidth("100%");
		Html htmlLoading = new Html(
				"<div style=\"padding:15px; text-align:center; color:#666;\"><i class=\"fa fa-spinner fa-spin\"></i> Mengambil Ringkasan Dasbor...</div>");
		containerDasborGrid.appendChild(htmlLoading);
		renderTarget.appendChild(containerDasborGrid);
	}

	private void renderDashboardKehadiran(final Date filterMulai, final Date filterSampai,
			final List<Integer> filterHariAktif, final ais.database.model.rab.SatuanKerja filterSatker,
			final String sortBy, final boolean muatData) throws Exception {

		Component renderTarget = dashboardKehadiranHost != null ? dashboardKehadiranHost : tabDashboardPanel;
		new DashboardKehadiranExpert(renderTarget, tbmuser, new DashboardKehadiranExpert.ReloadHandler() {
			public void reload(Date mulai, Date sampai, List<Integer> hariAktif,
					ais.database.model.rab.SatuanKerja satker, String sortBy) throws Exception {
				// Tombol "Tampilkan Analisis" -> muat data sungguhan.
				initDashboardKehadiran(mulai, sampai, hariAktif, satker, sortBy, true);
			}
		}).render(filterMulai, filterSampai, filterHariAktif, filterSatker, sortBy, muatData);
	}

	private static class DashboardKehadiranRequest {
		private final Date filterMulai;
		private final Date filterSampai;
		private final List<Integer> filterHariAktif;
		private final ais.database.model.rab.SatuanKerja filterSatker;
		private final String sortBy;
		private final boolean muatData;

		DashboardKehadiranRequest(Date filterMulai, Date filterSampai, List<Integer> filterHariAktif,
				ais.database.model.rab.SatuanKerja filterSatker, String sortBy, boolean muatData) {
			this.filterMulai = filterMulai;
			this.filterSampai = filterSampai;
			this.filterHariAktif = filterHariAktif == null ? null : new java.util.ArrayList<Integer>(filterHariAktif);
			this.filterSatker = filterSatker;
			this.sortBy = sortBy;
			this.muatData = muatData;
		}
	}

	class StatuskehadiranKaryawanHarianRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("rawtypes")
		private Collection user = ConstantValues.ambilBerdasarClass(Tbmuser.class).values();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = (StatuskehadiranKaryawanHarian) arg1;

			Pegawai pegawai = statuskehadiranKaryawanHarian.getPegawai();
			Dosen dosen = statuskehadiranKaryawanHarian.getDosen();
			Guru guru = statuskehadiranKaryawanHarian.getGuru();

			Mahasiswa mahasiswa = statuskehadiranKaryawanHarian.getMahasiswa();
			Siswa siswa = statuskehadiranKaryawanHarian.getSiswa();

			if (mahasiswa != null) {
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(mahasiswa.getNim()).setParent(vbox);

				RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNama()).setParent(vbox);

				vbox = new Vbox();
				vbox.setParent(arg0);
				new MyLabelAgakKecil("Mahasiswa").setParent(vbox);

				new MyLabelAgakKecil(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama())
						.setParent(vbox);
				new MyLabelAgakKecil(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getNama()).setParent(vbox);
			} else if (siswa != null) {
				CommonMedia.tampilkanGambarKecil(siswa).setParent(arg0);

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(siswa.getNim()).setParent(vbox);

				RevisiHelper.createNewRevisi(Siswa.class, siswa, siswa.getNama()).setParent(vbox);

				vbox = new Vbox();
				vbox.setParent(arg0);
				new MyLabelAgakKecil("Siswa").setParent(vbox);

				new MyLabelAgakKecil(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()).setParent(vbox);
				KelasSiswa kelasSiswa = siswa.getKelas();
				new MyLabelAgakKecil(kelasSiswa == null ? "" : kelasSiswa.getNama()).setParent(vbox);
			} else if (pegawai != null) {
				CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(vbox);
				new Label(pegawai.getCode() == null ? "" : pegawai.getCode()).setParent(vbox);

				RevisiHelper
						.createNewRevisi(Pegawai.class, pegawai,
								pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama())
						.setParent(vbox);

				vbox = new Vbox();
				vbox.setParent(arg0);
				if (pegawai.getTipePegawai() != null) {
					new MyLabelAgakKecil(pegawai.getTipePegawai().getNama()).setParent(vbox);
				} else if (pegawai.getGuru() != null) {
					new MyLabelAgakKecil("Guru").setParent(vbox);
				} else if (pegawai.getDosen() != null) {
					new MyLabelAgakKecil("Dosen").setParent(vbox);
				} else if (pegawai != null) {

					for (Object o : user) {
						Tbmuser tbmuser = (Tbmuser) o;
						if (tbmuser.getAktif() && tbmuser.hakAkses() != null && tbmuser.getPegawai() != null
								&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
							if (tbmuser.getAktif()) {
								new MyLabelAgakKecil(tbmuser.hakAkses().getRoleName()).setParent(vbox);
							}
						}
					}

				}

				if (pegawai.getStatusKepegawaian() != null) {
					new MyLabelAgakKecil(pegawai.getStatusKepegawaian().getNama()).setParent(vbox);
				}
			} else if (dosen != null) {
				CommonMedia.tampilkanGambarKecil(dosen).setParent(arg0);

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(dosen.getMycode() == null ? "" : dosen.getMycode()).setParent(vbox);
				new Label(dosen.getCode() == null ? "" : dosen.getCode()).setParent(vbox);

				RevisiHelper.createNewRevisi(Dosen.class, dosen, dosen.getNama()).setParent(vbox);

				vbox = new Vbox();
				vbox.setParent(arg0);
				new MyLabelAgakKecil("Dosen").setParent(vbox);

				if (dosen.getStatusKepegawaian() != null) {
					new MyLabelAgakKecil(dosen.getStatusKepegawaian().getNama()).setParent(vbox);
				}
			}

			else if (guru != null) {
				CommonMedia.tampilkanGambarKecil(guru).setParent(arg0);

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(guru.getNuptk() == null ? "" : guru.getNuptk()).setParent(vbox);
				new Label(guru.getKode() == null ? "" : guru.getKode()).setParent(vbox);

				RevisiHelper.createNewRevisi(Dosen.class, guru, guru.getNama()).setParent(vbox);

				vbox = new Vbox();
				vbox.setParent(arg0);
				new MyLabelAgakKecil("Guru").setParent(vbox);

				if (guru.getStatusKepegawaian() != null) {
					new MyLabelAgakKecil(guru.getStatusKepegawaian().getNama()).setParent(vbox);
				}
			}

			else {
				new MyLabelAgakKecil("-").setParent(arg0);
				new MyLabelAgakKecil("-").setParent(arg0);
				new MyLabelAgakKecil("-").setParent(arg0);
			}

			RevisiHelper
					.createNewRevisi(StatuskehadiranKaryawanHarian.class, statuskehadiranKaryawanHarian,
							statuskehadiranKaryawanHarian.getTanggal() == null ? ""
									: Common.dateFormat6.get().format(statuskehadiranKaryawanHarian.getTanggal()))
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(statuskehadiranKaryawanHarian.ambilMasukjam() == null ? ""
					: Common.timeFormat.get().format(statuskehadiranKaryawanHarian.ambilMasukjam())).setParent(vbox);
			CutiDanIzin cutiDanIzin = statuskehadiranKaryawanHarian.getCutiDanIzin();
			if (statuskehadiranKaryawanHarian.getDatangTerlambat()
					&& (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {

				double secondTerlambat = statuskehadiranKaryawanHarian.getJumlahTerlambat() * 3600;

				long millis = (long) (secondTerlambat * 1000);

				String jam = SmartDateTimeUtil.getDayStringJamMenit(startWaktu, new Date(millis), null);

				new MyLabelAgakKecilBoldMerah("terlambat " + jam).setParent(vbox);

			} else if (statuskehadiranKaryawanHarian.getDatangCepat()) {

				double secondTerlambat = statuskehadiranKaryawanHarian.getJumlahMasukSebelumWaktunya() * 3600;

				long millis = (long) (secondTerlambat * 1000);

				String jam = SmartDateTimeUtil.getDayStringJamMenit(startWaktu, new Date(millis), null);

				new MyLabelAgakKecilBoldBiru("lebih cepat " + jam).setParent(vbox);
			} else {
				new MyLabelAgakKecilBoldBiru("tepat waktu").setParent(vbox);
			}

			if (statuskehadiranKaryawanHarian.getFotoAbsenDatang() != null
					&& !statuskehadiranKaryawanHarian.getFotoAbsenDatang().isEmpty()) {
				Image image = new Image(statuskehadiranKaryawanHarian.getFotoAbsenDatang());
				image.setWidth("99%");
				image.setParent(vbox);
			}

			if (statuskehadiranKaryawanHarian.getLokasiAbsenDatang() != null
					&& !statuskehadiranKaryawanHarian.getLokasiAbsenDatang().isEmpty()) {
				MyHtml myHtml = new MyHtml(
						"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
								+ statuskehadiranKaryawanHarian.getLokasiAbsenDatang()
								+ "&amp;output=embed\"></iframe>");
				vbox.appendChild(myHtml);
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(statuskehadiranKaryawanHarian.ambilPulangjam() == null ? ""
					: Common.timeFormat.get().format(statuskehadiranKaryawanHarian.ambilPulangjam())).setParent(vbox);
			if (statuskehadiranKaryawanHarian.ambilPulangjam() != null) {
				if (statuskehadiranKaryawanHarian.getPulangTerlambat()) {

					double secondTerlambat = statuskehadiranKaryawanHarian.getJumlahPulangSetelahWaktunya() * 3600;

					long millis = (long) (secondTerlambat * 1000);

					String jam = SmartDateTimeUtil.getDayStringJamMenit(startWaktu, new Date(millis), null);

					new MyLabelAgakKecilBoldBiru("terlambat " + jam).setParent(vbox);
				} else if (statuskehadiranKaryawanHarian.getPulangCepat()
						&& (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {

					double secondTerlambat = statuskehadiranKaryawanHarian.getJumlahCepatKeluar() * 3600;

					long millis = (long) (secondTerlambat * 1000);

					String jam = SmartDateTimeUtil.getDayStringJamMenit(startWaktu, new Date(millis), null);

					new MyLabelAgakKecilBoldMerah("lebih cepat " + jam).setParent(vbox);
				} else {
					new MyLabelAgakKecilBoldBiru("tepat waktu").setParent(vbox);
				}
			}

			if (statuskehadiranKaryawanHarian.getFotoAbsenPulang() != null
					&& !statuskehadiranKaryawanHarian.getFotoAbsenPulang().isEmpty()) {
				Image image = new Image(statuskehadiranKaryawanHarian.getFotoAbsenPulang());
				image.setWidth("99%");
				image.setParent(vbox);
			}

			if (statuskehadiranKaryawanHarian.getLokasiAbsenPulang() != null
					&& !statuskehadiranKaryawanHarian.getLokasiAbsenPulang().isEmpty()) {
				MyHtml myHtml = new MyHtml(
						"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
								+ statuskehadiranKaryawanHarian.getLokasiAbsenPulang()
								+ "&amp;output=embed\"></iframe>");
				vbox.appendChild(myHtml);
			}

			statuskehadiranKaryawanHarian.renderKeteranganLink(arg0);

		}

	}

	public void onKodePegawai(Event event) throws Exception {

		String kode = ais.action.master.helper.KehadiranPresensiUtil.trimToEmpty(kodePegawai == null ? null : kodePegawai.getValue());
		if (kode.length() == 0) {
			MyMessageboxConfig.show(
					"Mohon maaf, RFID/Kode belum diisi sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) tempelkan kartu RFID atau ketikkan Kode pada kolom yang tersedia; (2) pastikan Kode tidak dikosongkan; (3) ulangi kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			if (kodePegawai != null) {
				kodePegawai.setValue("");
				kodePegawai.select();
				kodePegawai.focus();
			}
			return;
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Pegawai pegawai = (Pegawai) ConstantValues.simpleObject(session.createCriteria(Pegawai.class)
					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN).createAlias("guru", "guru", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.eq("idfinger", kode),
							Restrictions.or(Restrictions.eq("guru.idfinger", kode), Restrictions.eq("dosen.idfinger", kode))))
					.setMaxResults(1), Pegawai.class);

			Mahasiswa mahasiswa = null;
			Siswa siswa = null;
			if (pegawai == null) {
				mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
						session.createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
								.add(Restrictions.eq("nim", kode)).setMaxResults(1),
						Mahasiswa.class);
			}
			if (mahasiswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(
						session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
								.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
								.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
								.add(Restrictions.eq("nomorIndukNasional", kode)).setMaxResults(1),
						Siswa.class);
			}

			if (siswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(
						session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
								.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
								.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
								.add(Restrictions.eq("nomorInduk", kode)).setMaxResults(1),
						Siswa.class);
			}

			if (pegawai == null) {
				MyMessageboxConfig.show(
						"Mohon maaf, RFID/Kode yang Bapak/Ibu masukkan tidak ditemukan di dalam basis data. Langkah yang dapat dilakukan: (1) periksa kembali kartu RFID atau Kode yang digunakan; (2) pastikan data pegawai telah terdaftar beserta RFID/Kode-nya; (3) ulangi kembali proses ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Date tanggal = WaktuUtil.getDate();
			StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = CommonPayroll
					.getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai, mahasiswa, siswa, "", "", session, true);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal);
			String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
			DetailJenisShiftPegawai jenis = CommonPayroll.getDetailJenisShiftPegawai(pegawai, mahasiswa, siswa, tanggal,
					tanggal, hari, statuskehadiranKaryawanHarian.getLiburNasional() != null);

			if (jenis == null) {
				MyMessageboxConfig.showFormat(
						"Hai Bapak/Ibu {V1}. Mohon maaf, proses absensi GAGAL dilakukan karena data shift Anda tidak ditemukan. Langkah yang dapat dilakukan: (1) hubungi bagian Kepegawaian atau HRD; (2) pastikan jadwal shift Anda telah terdaftar untuk hari ini; (3) ulangi kembali proses absensi.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, pegawai.getNama());
				return;
			}

			boolean datang = true;
			CutiDanIzin cutiDanIzin = statuskehadiranKaryawanHarian.getCutiDanIzin();
			if (statuskehadiranKaryawanHarian.ambilMasukjam() == null
					&& (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
				statuskehadiranKaryawanHarian.setMasukjam(tanggal);
			} else {
				datang = false;
				statuskehadiranKaryawanHarian.setPulangJam(tanggal);
			}

			statuskehadiranKaryawanHarian.setDetailJenisShiftPegawai(jenis);
			statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);

			String pesanAbsensi = "ABSEN RFID " + (datang ? "DATANG " : "PULANG ")
					+ Common.dateFormat5.get().format(tanggal);
			statuskehadiranKaryawanHarian.setKeterangan(pesanAbsensi);

			tx = session.getTransaction();
			if (tx == null || !tx.isActive()) {
				tx = session.beginTransaction();
			}
			if (statuskehadiranKaryawanHarian.getId() == null) {
				session.save(statuskehadiranKaryawanHarian);
			} else {
				session.update(statuskehadiranKaryawanHarian);
			}
			session.flush();
			tx.commit();

			CommonPayroll.simpanDetail(session, statuskehadiranKaryawanHarian, true);
			System.out.println("statuskehadiranKaryawanHarian -> " + statuskehadiranKaryawanHarian + ", jenis -> " + jenis);

			onSearchDefault(null);
		} catch (Exception e) {
			ais.action.master.helper.KehadiranPresensiUtil.rollbackQuietly(tx);
			throw e;
		} finally {
			ais.action.master.helper.KehadiranPresensiUtil.closeNativeSession(session);
			HibernateUtil.closeSession();
			if (kodePegawai != null) {
				kodePegawai.setValue("");
				kodePegawai.select();
				kodePegawai.focus();
			}
		}
	}

	/**
	 * Terapkan saringan JENIS PENGGUNA pada daftar presensi.
	 *
	 * <p>Satu baris presensi menunjuk tepat satu subjek lewat kolom {@code mahasiswa}, {@code siswa},
	 * {@code pegawai}, {@code dosen}, atau {@code guru}. Perlu diperhatikan: seorang <b>pegawai bisa
	 * sekaligus terdaftar sebagai dosen atau guru</b> ({@code pegawai.dosen}/{@code pegawai.guru}),
	 * dan renderer daftar ini memang menampilkannya sebagai "Dosen"/"Guru". Karena itu:</p>
	 * <ul>
	 *   <li><b>Dosen</b> = kolom dosen terisi, ATAU pegawainya tertaut ke data dosen.</li>
	 *   <li><b>Guru</b> = kolom guru terisi, ATAU pegawainya tertaut ke data guru.</li>
	 *   <li><b>Pegawai</b> = pegawai MURNI, yaitu bukan dosen dan bukan guru (sesuai permintaan).</li>
	 *   <li><b>Siswa</b> / <b>Mahasiswa</b> = kolom terkait terisi.</li>
	 *   <li><b>Semua</b> (nilai kosong) = tanpa saringan, perilaku lama dipertahankan.</li>
	 * </ul>
	 *
	 * <p>Sengaja memakai {@code sqlRestriction} dan bukan {@code createAlias}: alias untuk
	 * pegawai/dosen/guru/siswa/mahasiswa sudah dibuat secara kondisional oleh pencarian nama di
	 * atas, sehingga menambah alias lagi berisiko "duplicate association path".</p>
	 */
	private void terapkanSaringanJenisPengguna(Criteria criteria) {
		if (jenisPengguna == null || jenisPengguna.getSelectedItem() == null) {
			return;
		}
		Object nilai = jenisPengguna.getSelectedItem().getValue();
		String jenis = nilai == null ? "" : nilai.toString().trim().toLowerCase();
		if (jenis.length() == 0) {
			return; // "Semua" -> tanpa saringan
		}

		if (jenis.equals("siswa")) {
			criteria.add(Restrictions.isNotNull("siswa"));
		} else if (jenis.equals("mahasiswa")) {
			criteria.add(Restrictions.isNotNull("mahasiswa"));
		} else if (jenis.equals("dosen")) {
			criteria.add(Restrictions.sqlRestriction(
					"({alias}.dosen is not null or {alias}.pegawai in (select p.id from public.pegawai p where p.dosen is not null))"));
		} else if (jenis.equals("guru")) {
			criteria.add(Restrictions.sqlRestriction(
					"({alias}.guru is not null or {alias}.pegawai in (select p.id from public.pegawai p where p.guru is not null))"));
		} else if (jenis.equals("pegawai")) {
			criteria.add(Restrictions.sqlRestriction(
					"({alias}.pegawai is not null and {alias}.dosen is null and {alias}.guru is null"
							+ " and {alias}.pegawai in (select p.id from public.pegawai p where p.dosen is null and p.guru is null))"));
		}
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Date[] rangeTanggal = ais.action.master.helper.KehadiranPresensiUtil.normalisasiRentangTanggal(mulai.getValue(), sampai.getValue());
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(StatuskehadiranKaryawanHarian.class)

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(Restrictions.between("tanggal", rangeTanggal[0], rangeTanggal[1]))
				.add(Restrictions.isNotNull("masukjam"));

		if (!cariPegawai.getValue().trim().isEmpty()) {
			criteria.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)

					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)

					.createAlias("dosen", "dosen",
							Criteria.LEFT_JOIN)
					.createAlias("guru", "guru",
							Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							Restrictions.ilike("siswa.nama", cariPegawai.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("mahasiswa.nim", cariPegawai.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("mahasiswa.nama", cariPegawai.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(
													Restrictions.ilike("pegawai.nama", cariPegawai.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("guru.nama",
																	cariPegawai.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("dosen.nama",
																	cariPegawai.getValue().trim(),
																	MatchMode.ANYWHERE)))))));
		}

		terapkanSaringanJenisPengguna(criteria);

		if (order)
			criteria.addOrder(Order.desc("tanggal")).addOrder(Order.desc("tanggal_dirubah"));

		Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();

		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

		if (pegawai != null || dosen != null || guru != null || mahasiswa != null || siswa != null) {
			labelCariPegawai.setVisible(false);
			cariPegawai.setVisible(false);
		}

		criteria.add(
				siswa != null ? Restrictions.eq("siswa", siswa)
						: mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa)
								: pegawai == null && dosen == null && guru == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.or(Restrictions.eq("pegawai", pegawai),
														Restrictions.eq("dosen", dosen)),
												Restrictions.eq("guru", guru)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarian = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(statuskehadiranKaryawanHarian);
		grid.setRowRenderer(new StatuskehadiranKaryawanHarianRenderer());
		grid.setModelCheckMobile(strset);

	}

}
