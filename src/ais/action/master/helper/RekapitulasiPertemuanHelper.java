package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Helper composer ZK yang menampilkan rekapitulasi/timeline {@link Pertemuan} (pertemuan
 * perkuliahan, jadwal pelajaran sekolah, KKN, PKL, bimbingan, dsb.) milik satu user
 * ({@link Tbmuser}), baik untuk mahasiswa, dosen, guru, siswa, maupun admin. Dipakai antara lain
 * sebagai widget rekap pertemuan pada dashboard/portal user.
 *
 * <p>
 * Sumber data pertemuan bergantung konteks pemanggilan:
 * </p>
 * <ul>
 * <li>Bila {@code perkuliahan} (berupa {@link VOPembelajaran}: {@link Perkuliahan} atau
 * {@link JadwalPelajaran}) diberikan, hanya pertemuan aktif milik objek tersebut yang diambil.</li>
 * <li>Bila user adalah mahasiswa, dipakai {@link Mahasiswa#ambilPertemuan(Session)} (dengan
 * {@link Mahasiswa#reInitPertemuan} dipanggil lebih dulu saat refresh, memakai rentang 6 bulan
 * sebelum/sesudah tanggal filter).</li>
 * <li>Bila user adalah dosen, dipakai {@link Dosen#ambilPertemuan(Session)} dengan pola serupa.</li>
 * <li>Selain itu (guru/siswa/admin sekolah), kriteria dibangun lewat
 * {@link DashboardTimelinePertemuan#initStaticCriteria} dengan seluruh filter jenis jadwal
 * diaktifkan sesuai apakah user terkait sekolah ({@link SekolahUtil#getSekolah()}) atau
 * perkuliahan.</li>
 * </ul>
 *
 * <p>
 * UI berupa grid berpaging dengan satu kolom "Pertemuan" yang dirender lewat
 * {@link DetailPertemuanRenderer} (mendelegasikan tampilan baris ke
 * {@link DashboardTimelinePertemuan#displayRow}), serta filter rentang tanggal (disembunyikan bila
 * {@code perkuliahan} diberikan) dan timer refresh otomatis lewat
 * {@link Common#createDefaultTimer(EventListener)}.
 * </p>
 */
public class RekapitulasiPertemuanHelper {

	/** Seperti {@link #display(Component, Tbmuser, VOPembelajaran)} dengan {@code perkuliahan=null} (memakai sumber pertemuan berbasis user). */
	public static void display(Component parent, final Tbmuser tbmuser) {
		display(parent, tbmuser, null);
	}

	/**
	 * Membangun UI rekap pertemuan (filter tanggal + tombol refresh + grid berpaging) di dalam
	 * {@code parent}, lalu memuat data awal lewat {@link #reload}. Rentang tanggal default diambil
	 * dari {@link RencanaTahunAkademik} yang sedang berlaku (±7 hari), dan grid disegarkan otomatis
	 * lewat timer berkala.
	 *
	 * @param parent      komponen induk ZK tempat UI dibangun
	 * @param tbmuser     user yang rekap pertemuannya ditampilkan
	 * @param perkuliahan bila diberikan, rekap dibatasi hanya pada pertemuan milik objek
	 *                    pembelajaran ini ({@link Perkuliahan} atau {@link JadwalPelajaran}); bila
	 *                    {@code null}, sumber pertemuan ditentukan dari peran {@code tbmuser}
	 */
	public static void display(Component parent, final Tbmuser tbmuser, final VOPembelajaran perkuliahan) {

		Borderlayout subBorderlayoutUtama = new Borderlayout();
		subBorderlayoutUtama.setParent(parent);

		final Center center = new Center();
		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(subBorderlayoutUtama);
		north.setHeight("38px");
		Toolbar hbox = new Toolbar();
		hbox.setParent(north);

		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
				.getCurrentRencanaTahunAkademik(WaktuUtil.getDate());

		Calendar calendarMulai = Calendar.getInstance();
		calendarMulai
				.setTime(rencanaTahunAkademik == null ? WaktuUtil.getDate() : rencanaTahunAkademik.getTanggalMulai());
		calendarMulai.set(Calendar.DATE, calendarMulai.get(Calendar.DATE) - 7);

		Calendar calendarSampai = Calendar.getInstance();
		calendarSampai
				.setTime(rencanaTahunAkademik == null ? WaktuUtil.getDate() : rencanaTahunAkademik.getTanggalSampai());
		calendarSampai.set(Calendar.DATE, calendarMulai.get(Calendar.DATE) + 7);

		final MyDatebox mulai = new MyDatebox(calendarMulai.getTime());
		mulai.setReadonly(true);

		final MyDatebox sampai = new MyDatebox(calendarSampai.getTime());
		sampai.setReadonly(true);

		if (perkuliahan == null) {
			hbox.appendChild(new MyLabelConfig("Tanggal"));
			hbox.appendChild(mulai);
			hbox.appendChild(new MyLabelConfig("sd"));
			hbox.appendChild(sampai);
		}

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refresh.setTooltiptext("Refresh");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload(tbmuser, center, mulai.getValue(), sampai.getValue(), true, true, perkuliahan);
			}
		});
		refresh.setParent(hbox);

		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(subBorderlayoutUtama);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(tbmuser, center, mulai.getValue(), sampai.getValue(), false, true, perkuliahan);
			}
		};

		Common.createDefaultTimer(eventListener);
		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);
	}

	/**
	 * Menentukan daftar id {@link Pertemuan} yang relevan (sesuai {@code perkuliahan}/peran
	 * {@code tbmuser}, lihat javadoc kelas) dan membangun ulang grid rekap di dalam
	 * {@code center}. Bila {@code refresh=true}, memicu {@code reInitPertemuan} pada mahasiswa
	 * atau dosen terlebih dahulu (menyinkronkan ulang cache pertemuan mereka) sebelum mengambil
	 * datanya.
	 *
	 * @param tbmuser     user pemilik rekap
	 * @param center      panel tempat grid dibangun ulang; isinya dibersihkan lebih dulu
	 * @param mulai       batas awal filter tanggal
	 * @param sampai      batas akhir filter tanggal
	 * @param refreh      bila {@code true}, sinkronkan ulang cache pertemuan mahasiswa/dosen lewat
	 *                    {@code reInitPertemuan} sebelum memuat data
	 * @param awal        tidak dipakai dalam implementasi saat ini
	 * @param perkuliahan bila diberikan, batasi pertemuan hanya milik objek ini
	 */
	@SuppressWarnings("unchecked")
	private static void reload(final Tbmuser tbmuser, final Center center, final Date mulai, final Date sampai,
			boolean refreh, boolean awal, final VOPembelajaran perkuliahan) {
		Common.clear(center);

		final Mahasiswa mahasiswa = tbmuser.getMahasiswa();
		Dosen dosen = tbmuser.ambilDosen();

		final List<Long> pertemuans = new ArrayList<Long>();
		Session session = HibernateUtil.currentSession();
		if (perkuliahan != null) {
			Criteria criteria = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(perkuliahan instanceof Perkuliahan ? Restrictions.eq("perkuliahan", perkuliahan)
							: perkuliahan instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", perkuliahan)
									: Restrictions.sqlRestriction("false"));
			pertemuans.addAll(criteria.setProjection(Projections.property("id")).list());
		}

		else if (mahasiswa != null) {

			if (refreh) {

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.setTime(mulai);
				calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.setTime(sampai);
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

				mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
			}

			pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

		} else if (dosen != null) {

			if (refreh) {

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.setTime(mulai);
				calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.setTime(sampai);
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

				dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
			}

			pertemuans.addAll(dosen.ambilPertemuan(session).values());
		} else if (!Common.getApakahAdmin()) {

			Sekolah sk = SekolahUtil.getSekolah();
			Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
			Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

			if (guru != null) {
				sk = guru.getSekolah();
			}
			if (siswa != null) {
				sk = siswa.getSekolah();
			}

			Calendar calendarMulai = Calendar.getInstance();
			calendarMulai.setTime(mulai);
			calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

			Calendar calendarSampai = Calendar.getInstance();
			calendarSampai.setTime(sampai);
			calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

			String mul = null;
			String sam = null;

			Integer bln = 1;
			Integer ke = null;

			boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

			boolean jadwalPelajaran = (sk != null && sk.getId() != null);

			boolean jadwalKkn = jadwalPerkuliahan;

			boolean jadwalPkl = jadwalPerkuliahan;

			boolean jadwalKegiatan = jadwalPerkuliahan;

			boolean jadwalRevisi = jadwalPerkuliahan;
			boolean jadwalKonsultasi = jadwalPerkuliahan;

			boolean jadwalBimbingan = jadwalPerkuliahan;

			boolean jadwalKonsultasiLain = jadwalPerkuliahan;

			boolean tdpDiskusi = false;

			boolean tdpUjian = false;

			boolean tdpMateri = false;
			boolean tdpTugas = false;
			boolean tdpCatatan = false;
			boolean tdpAudio = false;
			boolean tdpVideo = false;
			boolean tdpDosenPengganti = false;

			String cariMk = "";
			String cariDosen = "";
			String cariTopik = "";
			String cariCatatan = "";
			String cariMahasiswa = "";
			String cariKelas = "";
			String cariRuang = "";
			String cari = "";
			Integer ekstra = null;

			boolean remedial = false;
			boolean paralel = false;
			boolean pra = false;
			StatusPertemuan statusPertemuan = null;
			boolean ujian = false;

			String day = null;

			Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
					calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
					tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
					jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian, tdpDiskusi,
					tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra, remedial, cariMk,
					ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang, cariDosen, cariMahasiswa,
					ujian, sk, session);

			pertemuans.addAll(criteria.setProjection(Projections.property("id")).list());

		}

		final MyGrid grid = new MyGrid();

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(center);

		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pertemuan");

		ListModel strset = new SimpleListModel(pertemuans);
		grid.setModelCheckMobile(strset);

		grid.setMold("paging");
		grid.setPageSize(10);

		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");

		grid.setRowRenderer(new DetailPertemuanRenderer());

	}

	/**
	 * Perender baris grid yang memuat ulang satu {@link Pertemuan} berdasarkan id (data model
	 * grid hanya menyimpan id, bukan entitas penuh) lalu mendelegasikan tampilan baris ke
	 * {@link DashboardTimelinePertemuan#displayRow}. Baris disembunyikan bila pertemuan sudah
	 * tidak ditemukan (mis. terhapus setelah id dimuat ke grid).
	 */
	public static class DetailPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		/** Menandai apakah tampilan sedang diakses dari perangkat mobile; diteruskan ke {@link DashboardTimelinePertemuan#displayRow} untuk menyesuaikan tata letak baris. */
		private boolean mobile = false;
		/** Pengguna yang sedang login saat renderer dibuat; diteruskan ke {@link DashboardTimelinePertemuan#displayRow} untuk menentukan hak/tampilan aksi per baris. */
		private Tbmuser tbmuser;

		/** Menyimpan status mobile dan pengguna yang sedang login sekali di awal (dipakai berulang untuk setiap baris yang dirender). */
		public DetailPertemuanRenderer() {
			mobile = Common.isMobile();
			tbmuser = Common.getCurrentUser();
		}

		/**
		 * Memuat ulang satu {@link Pertemuan} berdasarkan id yang tersimpan sebagai {@code data} baris
		 * grid, lalu mendelegasikan tampilannya ke {@link DashboardTimelinePertemuan#displayRow}. Baris
		 * disembunyikan bila pertemuan sudah tidak ditemukan (mis. terhapus setelah id dimuat ke grid).
		 *
		 * @param arg0 baris grid yang akan diisi
		 * @param data id {@link Pertemuan} (bukan entitas penuh) yang harus dimuat ulang
		 */
		@Override
		public void render(final Row arg0, Object data) throws Exception {

			Session session = HibernateUtil.currentSession();
			Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class).add(Restrictions.idEq(data))
					.uniqueResult();
			if (pertemuan == null) {
				arg0.setVisible(false);
				return;
			}
			Long selectedDiskusi = null;

			DashboardTimelinePertemuan.displayRow(arg0, mobile, pertemuan, selectedDiskusi, tbmuser);

		}

	}

}
