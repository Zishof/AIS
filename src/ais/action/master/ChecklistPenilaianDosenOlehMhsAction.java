package ais.action.master;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;

import ais.action.master.helper.generic.AngketDosenWindow;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JadwalChecklistPenilaianUmum;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk checklist penilaian dosen oleh mhs. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code South south}, {@code Mahasiswa mahasiswa}, {@code Konfigurasi
 * konfigurasiBerlangsung}, {@code String taAngketAktif}, {@code String smtAngketAktif}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code onRefresh()}, {@code
 * onSearchDefault()}); operasi domain lain ({@code onIsiAngketDosenSelesai()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class ChecklistPenilaianDosenOlehMhsAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	// private MyToolbarbuttonConfig add;
	private South south;
	private Mahasiswa mahasiswa;
	private Konfigurasi konfigurasiBerlangsung;
	// TA + Semester (GANJIL/GENAP) dari JADWAL ANGKET aktif (JadwalChecklistPenilaianUmum yg periode-
	// nya mencakup hari ini & punya grup angket dosen). Dipakai memilih grup default yang dibuka.
	private String taAngketAktif;
	private String smtAngketAktif;
	private boolean adaJadwalAngketAktif;

	public void onIsiAngketDosenSelesai(Event event) {
		execution.sendRedirect(Common.getRequestHostWithProtocol() + "/main");

	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		Tbmuser tbmuser = Common.getCurrentUser();

		mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		if (execution.getParameter("mahasiswa") != null && Common.isNumber(execution.getParameter("mahasiswa"))) {
			mahasiswa = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa").trim()))).uniqueResult();
			if (mahasiswa != null) {
				if (south != null) {
				south.setVisible(false);
			}
			}
		}

		if (mahasiswa == null) {
			alert("Anda harus login sebagai mahasiswa");
			execution.sendRedirect(Common.getRequestHostWithProtocol() + "/main");
			return;
		}

		final Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

		final int currentSmt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		if (Common.bolehKonfigurasi("input_angket_penilaian_dosen_harus_berdasarkan_kalender_akademik", Konfigurasi.TIDAK_AKTIF)) {
			konfigurasiBerlangsung = Common.checkKonfigurasiDenganKalenderAkademik(HibernateUtil.currentSession(),
					"checklist_penilaian_dosen_semester_berlangsung", Common.getCurrentTahunAkademik(),
					currentSmt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, mahasiswa.getSemesterMulai(),
					mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(), mahasiswa.getProgram());
		} else {
			konfigurasiBerlangsung = Common.getKonfigurasi("checklist_penilaian_dosen_semester_berlangsung",
					Common.getCurrentTahunAkademik(), currentSmt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
					mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(),
					mahasiswa.getProgram(), Konfigurasi.TIDAK_AKTIF);
		}

		// TA + Semester yang MENGIKUTI JADWAL ANGKET: cari JadwalChecklistPenilaianUmum yang periodenya
		// (mulai..sampai) mencakup HARI INI & punya Grup Angket dosen. Bila ada, grup TA+semester itulah
		// yang dibuka default (menggantikan pemilihan berbasis semester berjalan mahasiswa).
		try {
			java.util.Date now = ais.ui.util.WaktuUtil.getDate();
			JadwalChecklistPenilaianUmum jadwalAktif = (JadwalChecklistPenilaianUmum) HibernateUtil.currentSession()
				.createCriteria(JadwalChecklistPenilaianUmum.class)
				.add(Restrictions.isNotNull("grupChecklistPenilaianDosen"))
				.add(Restrictions.or(Restrictions.isNull("mulai"), Restrictions.le("mulai", now)))
				.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", now)))
				.addOrder(org.hibernate.criterion.Order.desc("mulai"))
				.addOrder(org.hibernate.criterion.Order.desc("id")).setMaxResults(1).uniqueResult();
			if (jadwalAktif != null) {
				taAngketAktif = jadwalAktif.getTahunAkademik();
				smtAngketAktif = jadwalAktif.getSemester();
				adaJadwalAngketAktif = taAngketAktif != null && !taAngketAktif.trim().isEmpty()
					&& smtAngketAktif != null && !smtAngketAktif.trim().isEmpty();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/ChecklistPenilaianDosenOlehMhsAction.java:123");
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	public void onRefresh(Event event) {
		onSearchDefault(null);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link ChecklistPenilaianDosenOlehMhsAction}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ChecklistPenilaianDosenOlehMhsAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ChecklistPenilaianDosenOlehMhsAction
	 */
	class DataRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final String[] data = (String[]) arg1;
			final MyDetail detail = new MyDetail();
			Integer smt;
			try {
				smt = Integer.parseInt(data[1]);
			} catch (Exception e) {
				smt = 0;
			}
			final Integer semester = smt;
			arg0.setVisible(semester > 0);
			detail.setVisible(!semester.equals(1000));
			detail.setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						List<Long> perkuliahans = mahasiswa.ambilPerkuliahanDanParalel(semester, null);
						AngketDosenWindow angketDosenWindow = new AngketDosenWindow(data[0],
								semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, perkuliahans, mahasiswa,
								addWindow, false);
						detail.appendChild(angketDosenWindow);
					}
				}
			};

			detail.addEventListener("onOpen", eventListener);

			new Label(data[0]).setParent(arg0);
			new Label(semester.equals(1000) ? "Lulus" : data[1]).setParent(arg0);

			if (data != null && data.length > 2) {
				new Label(data[2]).setParent(arg0);
			} else {
				new Label("").setParent(arg0);
			}

			if (adaJadwalAngketAktif) {
				// MENGIKUTI JADWAL ANGKET (JadwalChecklistPenilaianUmum aktif mencakup hari ini): buka grup yang
				// TA + semester-nya SAMA dengan jadwal tsb (bukan sekadar semester berjalan mahasiswa).
				String jenisSmt = semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
				if (taAngketAktif.equalsIgnoreCase(data[0]) && smtAngketAktif.equalsIgnoreCase(jenisSmt)) {
					arg0.setStyle("border:0px;background: #C2FFA3;");
					detail.setOpen(true);
					eventListener.onEvent(null);
				}
			} else if (konfigurasiBerlangsung != null && konfigurasiBerlangsung.getNilai().equals(Konfigurasi.AKTIF)) {

				String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
				if (Common
						.getSemester(mahasiswa.getTahunangkatan(), semesterMulai,
								mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai())
						.equals(semester)) {
					arg0.setStyle("border:0px;background: #C2FFA3;");
					detail.setOpen(true);
					eventListener.onEvent(null);
				}

			} else {

				String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
				if (Common
						.getSemester(mahasiswa.getTahunangkatan(), semesterMulai,
								mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai())
						.equals(semester + 1)) {
					arg0.setStyle("border:0px;background: #C2FFA3;");
					detail.setOpen(true);
					eventListener.onEvent(null);
				}
			}
		}
	}

	public void onSearchDefault(Event event) {
		if (mahasiswa == null) {
			grid.setModelCheckMobile(new SimpleListModel(new java.util.ArrayList()));
			return;
		}
		ListModel strset = new SimpleListModel(
				Common.generateSemestersForGrid(mahasiswa, 1, mahasiswa.currentSemester() + 1, null));
		grid.setRowRenderer(new DataRenderer());
		grid.setModelCheckMobile(strset);

	}

}
