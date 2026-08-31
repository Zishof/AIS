package ais.action.master.sekolah;

import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.South;

import ais.action.master.helper.generic.AngketGuruWindow;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk checklist penilaian guru oleh mhs. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code South south}, {@code Siswa siswa}, {@code Konfigurasi
 * konfigurasiBerlangsung}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()});
 * pembacaan/pencarian ({@code onRefresh()}, {@code onSearchDefault()}); operasi domain lain ({@code
 * onIsiAngketGuruSelesai()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
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
public class ChecklistPenilaianGuruOlehMhsAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	// private MyToolbarbuttonConfig add;
	private South south;
	private Siswa siswa;
	@SuppressWarnings("unused")
	private Konfigurasi konfigurasiBerlangsung;

	public void onIsiAngketGuruSelesai(Event event) {
		execution.sendRedirect(execution.getContextPath() + "/main");

	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unused")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		Tbmuser tbmuser = Common.getCurrentUser();

		siswa = tbmuser == null ? null : tbmuser.getSiswa();

		if (execution.getParameter("siswa") != null && Common.isNumber(execution.getParameter("siswa"))) {
			siswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""))
					.add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("siswa").trim()))).uniqueResult();
			if (siswa != null) {
				south.setVisible(false);
			}
		}

		if (siswa == null) {
			alert("Anda harus login sebagai siswa");
			execution.sendRedirect(execution.getContextPath() + "/main");
			return;
		}

		final Integer tahunAngkatanMhs = siswa.getTahunMasuk();
		String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

		final int currentSmt = 1;

		if (Common.bolehKonfigurasi("input_angket_penilaian_guru_harus_berdasarkan_kalender_akademik", Konfigurasi.TIDAK_AKTIF)) {
			konfigurasiBerlangsung = Common.checkKonfigurasiDenganKalenderAkademik(HibernateUtil.currentSession(),
					"checklist_penilaian_guru_semester_berlangsung", Common.getCurrentTahunAkademik(),
					currentSmt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, Perkuliahan.GANJIL,
					siswa.getSekolah().getYayasan(), siswa.getSekolah(), siswa.getProgram());
		} else {
			konfigurasiBerlangsung = Common.getKonfigurasi("checklist_penilaian_guru_semester_berlangsung",
					Common.getCurrentTahunAkademik(), currentSmt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
					Perkuliahan.GANJIL, siswa.getSekolah().getYayasan(), siswa.getSekolah(), siswa.getProgram(),
					Konfigurasi.TIDAK_AKTIF);
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

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						String sql = "this_.kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id="
								+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
						Criterion criterionKls = Restrictions.sqlRestriction(sql);

						sql = "this_.kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
								+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
						Criterion criterionLes = Restrictions.sqlRestriction(sql);

						List<Long> jadwalPelajarans = HibernateUtil.currentSession()
								.createCriteria(JadwalPelajaran.class).add(Restrictions.or(criterionKls, criterionLes))
								.add(Restrictions.eq("semester", semester)).add(Restrictions.eq("tahunAjaran", data[0]))
								.setProjection(Projections.property("id")).list();

						AngketGuruWindow angketGuruWindow = new AngketGuruWindow(data[0],
								semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, jadwalPelajarans, siswa,
								addWindow, false);
						detail.appendChild(angketGuruWindow);
					}
				}
			};

			detail.addEventListener("onOpen", eventListener);

			new Label(data[0]).setParent(arg0);
			new Label(semester.equals(1000) ? "Lulus" : data[1]).setParent(arg0);

			try {
				new Label(data[2]).setParent(arg0);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

//			if (konfigurasiBerlangsung != null && konfigurasiBerlangsung.getNilai().equals(Konfigurasi.AKTIF)) {
//
//				String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
//				if (Common
//						.getSemester(siswa.getTahunangkatan(), semesterMulai,
//								siswa.getPindahKeKampusIniMasukSemester(), siswa.getSemesterMulai())
//						.equals(semester)) {
//					arg0.setStyle("border:0px;background: #C2FFA3;");
//					detail.setOpen(true);
//					eventListener.onEvent(null);
//				}
//
//			} else {
//
//				String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
//				if (Common
//						.getSemester(siswa.getTahunangkatan(), semesterMulai,
//								siswa.getPindahKeKampusIniMasukSemester(), siswa.getSemesterMulai())
//						.equals(semester + 1)) {
//					arg0.setStyle("border:0px;background: #C2FFA3;");
//					detail.setOpen(true);
//					eventListener.onEvent(null);
//				}
//			}
		}
	}

	public void onSearchDefault(Event event) {
//		ListModel strset = new SimpleListModel(
//				Common.generateSemestersForGrid(siswa, 1, siswa.currentSemester() + 1, null));
		grid.setRowRenderer(new DataRenderer());
//		grid.setModelCheckMobile(strset);

	}

}
