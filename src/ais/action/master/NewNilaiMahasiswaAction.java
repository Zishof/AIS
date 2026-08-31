package ais.action.master;

import java.util.List;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabpanel;

import ais.action.master.helper.PenilaianMahasiswaHelper;
import ais.action.master.helper.TampilStudiMahasiswaHelper;
import ais.action.report.format1.akademik.LaporanKHS;
import ais.action.report.format1.akademik.LaporanRekamanNilai;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk new nilai mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Mahasiswa mahasiswa}, {@code Tabpanel kartuHasilStudi}, {@code Tabpanel transkripAkademik},
 * {@code Tabpanel rekamanNilai}, {@code Tabpanel asisten}, {@code Integer semesterPendek};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code
 * onTampilKHS()}, {@code onTampilTranskripAkademik()}, {@code onTampilRekamanNilai()}, {@code
 * onSearchDefaultKeDatabase()}, {@code onSearchDefault()}, {@code load()}); operasi domain lain ({@code
 * onNilaiSp()}, {@code onAsisten()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class NewNilaiMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220401468178L;
	protected MyWindow addWindow;
	protected MyGrid grid;
	protected Mahasiswa mahasiswa;

	protected Tabpanel kartuHasilStudi;
	protected Tabpanel transkripAkademik;
	protected Tabpanel rekamanNilai;
	protected Tabpanel asisten;

	protected Integer semesterPendek = null;
	protected Tabpanel nilaiSp;
	protected Tabpanel dasborPanel;
	protected Combobox semesterMulai;
	protected Combobox semesterSampai;
	protected MyCheckboxConfig cariSemesterPendek;

	protected boolean remedial = false;

	public void onNilaiSp(Event event) {
		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa)) {
			return;
		}
		if (nilaiSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(nilaiSp);
			include.setSrc("/pages/master/nilai_mahasiswa_sp.zul");
		}
	}

	public void onAsisten(Event event) {
		if (asisten.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(asisten);
			include.setSrc("/pages/master/penilaian.zul");
		}
	}

	public void onTampilKHS(Event event) {

		if (kartuHasilStudi.getChildren().size() == 0) {
			LaporanKHS laporanKHS = new LaporanKHS();
			laporanKHS.setHeight("100%");
			laporanKHS.setWidth("100%");
			laporanKHS.setParent(kartuHasilStudi);
		}
	}

	public void onTampilTranskripAkademik(Event event) {
		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa)) {
			return;
		}
		if (transkripAkademik.getChildren().size() == 0) {
			LaporanTranskipAkademik laporanTranskipAkademik = new LaporanTranskipAkademik();
			laporanTranskipAkademik.setHeight("100%");
			laporanTranskipAkademik.setWidth("100%");
			laporanTranskipAkademik.setParent(transkripAkademik);
		}
	}

	public void onTampilRekamanNilai(Event event) {
		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa)) {
			return;
		}
		if (rekamanNilai.getChildren().size() == 0) {
			LaporanRekamanNilai laporanRekamanNilai = new LaporanRekamanNilai();
			laporanRekamanNilai.setHeight("100%");
			laporanRekamanNilai.setWidth("100%");
			laporanRekamanNilai.setParent(rekamanNilai);
		}
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
		boolean cek = execution.getParameter("pass") == null;
		if (cek) {
			if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				session.removeAttribute("usersTemp");
				Common.goLogoff();
				return;
			}
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getMahasiswa() == null) {
			alert("Anda harus login sebagai mahasiswa");
			return;
		}

		mahasiswa = tbmuser.getMahasiswa();

		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa)) {
			return;
		}
		
		if (dasborPanel != null && dasborPanel.getChildren().isEmpty()) {
			TampilStudiMahasiswaHelper.initDashboard(dasborPanel, mahasiswa, null, null, false);
		}
		

		if (asisten != null) {
			boolean mahasiswaBolehLihatNilai = ((Number) HibernateUtil.currentSession()
					.createCriteria(MahasiswaJadiAsisten.class)
					.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue() > 0;
			asisten.setVisible(mahasiswaBolehLihatNilai);
			asisten.getLinkedTab().setVisible(mahasiswaBolehLihatNilai);
		}

		for (Integer i = 1; i <= (mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() > 0
				? mahasiswa.getSemesterLulus()
				: 40); i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterMulai.appendChild(comboitem);
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterSampai.appendChild(comboitem);
		}

		Common.selectComboItem(semesterMulai,
				mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester() > mahasiswa.getSemesterLulus()
						? mahasiswa.getSemesterLulus()
						: mahasiswa.currentSemester());
		Common.selectComboItem(semesterSampai,
				mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester() > mahasiswa.getSemesterLulus()
						? mahasiswa.getSemesterLulus()
						: mahasiswa.currentSemester());

		if (semesterMulai != null) { semesterMulai.setReadonly(true); }
		if (semesterSampai != null) { semesterSampai.setReadonly(true); }

		if (cariSemesterPendek != null) {
			cariSemesterPendek.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});
		}

		onSearchDefault(null);
	}

	public void onSearchDefaultKeDatabase(Event event) {
		load(true);
	}

	private Center rowData;

	public void onSearchDefault(Event event) {
		load(false);
	}

	private void load(final boolean keDatabase) {

		// KE-FIX (NPE mahasiswa.currentSemester()): onSearchDefault/onSearchDefaultKeDatabase adalah
		// event handler publik yang bisa terpicu dari komponen ZUL yang sudah terlanjur ter-render
		// walau doAfterCompose berhenti lebih awal (mis. tbmuser bukan mahasiswa -> alert & return
		// tanpa pernah mengisi field mahasiswa). Tanpa jaga-jaga ini, klik pada komponen tsb
		// melempar NullPointerException di baris mahasiswa.currentSemester() di bawah.
		if (mahasiswa == null) {
			return;
		}

		final Integer semesterPendekEfektif = (cariSemesterPendek != null && cariSemesterPendek.isChecked())
				? Perkuliahan.SEMESTER_PENDEK
				: semesterPendek;

		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());
		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			System.out.println("smt -> " + smt);
			Common.selectComboItem(true, semesterSampai, smt);
		}
		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());

		Common.clear(rowData);

		if (mulai.equals(sampai)) {
			List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendekEfektif);
			for (String[] data : datas) {
				Integer smt;
				try {
					smt = Integer.parseInt(data[1].split(",")[0]);
				} catch (Exception e) {
					smt = 0;
				}
				Integer semester = smt;
				if (semester > 0) {
					Integer tahap;
					try {
						tahap = Integer.parseInt(data[3]);
					} catch (Exception e) {
						tahap = 0;
					}
					Integer tahapan = tahap;
					String tahunAjaran = data[0];

					Row rowUtama = Common.tampilanScroll1(rowData);

					PenilaianMahasiswaHelper penilaianMahasiswaHelper = new PenilaianMahasiswaHelper(semesterPendekEfektif,
							remedial);
					penilaianMahasiswaHelper.display(mahasiswa, tahunAjaran, semester, tahapan, rowUtama, addWindow,
							keDatabase);
				}
			}
		} else {

			// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
			// per tahun ajaran/semester ini data-driven, sama seperti pola "Ke-1".."Ke-N"
			// di SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
			// Tab/Tabpanel bawaan ZK.
			ais.ui.util.MyButtonTabbox tabboxKrs = ais.ui.util.MyButtonTabbox.buat(rowData, "100%", null);

			List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendekEfektif);
			int i = 0;
			boolean ada = false;
			for (String[] data : datas) {
				Integer smt;
				try {
					smt = Integer.parseInt(data[1].split(",")[0]);
				} catch (Exception e) {
					smt = 0;
				}
				if (mahasiswa.currentSemester() == smt) {
					ada = true;
				}
			}

			int indexTerpilih = 1;
			for (String[] data : datas) {
				Integer smt;
				try {
					smt = Integer.parseInt(data[1].split(",")[0]);
				} catch (Exception e) {
					smt = 0;
				}
				final Integer semester = smt;

				Integer tahap;
				try {
					tahap = Integer.parseInt(data[3]);
				} catch (Exception e) {
					tahap = 0;
				}
				final Integer tahapan = tahap;

				final String tahunAjaran = data[0];
				final int index = i + 1;

				tabboxKrs.tambahTabLazy(index, tahunAjaran + "/" + semester,
						new ais.ui.util.MyButtonTabbox.PemuatTab() {
							@Override
							public void muat(org.zkoss.zul.Div panel) throws Exception {
								PenilaianMahasiswaHelper penilaianMahasiswaHelper = new PenilaianMahasiswaHelper(
										semesterPendekEfektif, remedial);
								penilaianMahasiswaHelper.display(mahasiswa, tahunAjaran, semester, tahapan, panel,
										addWindow, keDatabase);
							}
						});
				if (ada) {
					if (mahasiswa.currentSemester() == semester) {
						indexTerpilih = index;
					}
				} else if (i == 0) {
					indexTerpilih = index;
				}
				i++;
			}
			tabboxKrs.pilih(indexTerpilih);
		}
	}

}
