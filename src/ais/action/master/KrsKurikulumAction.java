package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.KrsKurikulumHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk krs kurikulum. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Label searchnim}, {@code Label searchjurusan}, {@code Label searchnama}, {@code Label
 * searchfakultas}, {@code Label searchangkatan}, {@code Label searchprogram}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * onSearchDefaultKeDatabase()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
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
public class KrsKurikulumAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220301468178L;
	protected MyWindow addWindow;
	protected MyGrid grid;
	protected Label searchnim;
	protected Label searchjurusan;
	protected Label searchnama;
	protected Label searchfakultas;
	protected Label searchangkatan;
	protected Label searchprogram;
	protected Mahasiswa mahasiswa;
	protected Integer semesterPendek;

	protected MyColumnConfig colSemester;
	protected MyColumnConfig colTahapan;

	protected Combobox semesterMulai;
	protected Combobox semesterSampai;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getMahasiswa() == null) {
			alert("Anda harus login sebagai mahasiswa");
			return;
		}

		mahasiswa = tbmuser.getMahasiswa();
		if (searchnim != null) { searchnim.setValue(mahasiswa.getNim()); }
		if (searchjurusan != null) { searchjurusan.setValue(mahasiswa.getJurusan().getNama()); }
		if (searchnama != null) { searchnama.setValue(mahasiswa.getNama()); }
		if (searchfakultas != null) { searchfakultas.setValue(mahasiswa.getJurusan().getFakultas().getNama()); }
		if (searchangkatan != null) { searchangkatan.setValue(mahasiswa.getTahunangkatan() + " (" + mahasiswa.getSemesterMulai() + ")"); }
		if (searchprogram != null) { searchprogram.setValue(mahasiswa.getProgram() == null ? "" : mahasiswa.getProgram()); }

		if (ConstantValues.jumlahTahapan.isEmpty()) {
			ConstantValues.initJumlahTahapan();
		}
		if (ConstantValues.aktifkanTahapan
				&& ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan()) > 2) {
			colTahapan.setWidth("10%");
		}

		for (Integer i = 1; i <= (mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus()>0? mahasiswa.getSemesterLulus() : 40); i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterMulai.appendChild(comboitem);
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterSampai.appendChild(comboitem);
		}

		int defaultPemilihanSemesterMulai = Common
				.getKonfigurasi("default_pemilihan_semester_mulai", mahasiswa.currentSemester() + "").niliaInteger();
		int defaultPemilihanSemesterSampai = Common
				.getKonfigurasi("default_pemilihan_semester_sampai", mahasiswa.currentSemester() + "").niliaInteger();

		if (mahasiswa.getSemesterLulus() != null && defaultPemilihanSemesterMulai > mahasiswa.getSemesterLulus()) {
			defaultPemilihanSemesterMulai = mahasiswa.getSemesterLulus();
		}
		if (mahasiswa.getSemesterLulus() != null && defaultPemilihanSemesterSampai > mahasiswa.getSemesterLulus()) {
			defaultPemilihanSemesterSampai = mahasiswa.getSemesterLulus();
		}

		if (defaultPemilihanSemesterMulai != defaultPemilihanSemesterSampai) {
			Common.selectComboItem(semesterMulai, defaultPemilihanSemesterMulai);
			Common.selectComboItem(semesterSampai, defaultPemilihanSemesterSampai);
		} else {
			Common.selectComboItem(semesterMulai,
					mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester() > mahasiswa.getSemesterLulus()
							? mahasiswa.getSemesterLulus()
							: mahasiswa.currentSemester());
			Common.selectComboItem(semesterSampai,
					mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester() > mahasiswa.getSemesterLulus()
							? mahasiswa.getSemesterLulus()
							: mahasiswa.currentSemester());
		}

		if (semesterMulai != null) { semesterMulai.setReadonly(true); }
		if (semesterSampai != null) { semesterSampai.setReadonly(true); }

		onSearchDefault(null);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link KrsKurikulumAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KrsKurikulumAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean keDatabase}; operasi lokal:
	 * {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KrsKurikulumAction
	 */
	class DataRenderer extends ais.ui.util.MyRowRenderer {

		private boolean keDatabase;

		public DataRenderer(boolean keDatabase) {
			this.keDatabase = keDatabase;
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final String[] data = (String[]) arg1;
			final Boolean editable = true;
			final MyDetail detail = new MyDetail();
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
			detail.setVisible(!semester.equals(1000));
			final String tahunAjaran = data[0];

			final Html html = new ais.ui.util.MyHtml("");
			final Html komentarshtml = new ais.ui.util.MyHtml("");
			detail.setParent(arg0);
			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						KrsKurikulumHelper krsHelper = new KrsKurikulumHelper(semesterPendek);
						krsHelper.display(editable, mahasiswa, tahunAjaran, semester, tahapan, detail, addWindow, html,
								komentarshtml);
					}
				}
			};
			detail.addEventListener("onOpen", eventListener);

			new Label(data[0]).setParent(arg0);
			new Label(tahapan != null && tahapan.equals(-1) ? "" : semester.equals(1000) ? "Lulus" : data[1])
					.setParent(arg0);
			new Label(tahapan != null && tahapan.equals(-1) ? "" : tahapan + "").setParent(arg0);
			try {
				new Label(data[2]).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}
			html.setParent(arg0);
			komentarshtml.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			final Label catatan = new Label();
			final Label catatanKhs = new Label();
			catatan.setParent(vbox);
			catatanKhs.setParent(vbox);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {

					if (!ConstantValues.aktifkanTahapan) {
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
						Integer t = mahasiswa.currentTahapan();
						if (t != null && !t.equals(0) && tahapan != null && !tahapan.equals(0) && tahapan.equals(t)) {
							arg0.setStyle("border:0px;background: #C2FFA3;");
							detail.setOpen(true);
							eventListener.onEvent(null);
						}
					}

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
							semesterPendek, keDatabase);
					catatan.setValue(krsMahasiswa.getCatatan());
					catatanKhs.setValue(krsMahasiswa.getCatatanKhs());
					String krs = mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek,
							krsMahasiswa, false);
					html.setContent(krs);
					Integer komentars = krsMahasiswa.getKomentars();
					String kom = komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar";
					komentarshtml.setContent(kom);
				}
			});
		}
	}

	public void onSearchDefault(Event event) {

		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());
		
		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			System.out.println("smt -> " + smt);
			Common.selectComboItem(true, semesterSampai, smt);
		}
		
		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());
		ListModel strset = new SimpleListModel(
				Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek));
		grid.setRowRenderer(new DataRenderer(false));
		grid.setModelCheckMobile(strset);

	}

	public void onSearchDefaultKeDatabase(Event event) {

		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());
		
		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			System.out.println("smt -> " + smt);
			Common.selectComboItem(true, semesterSampai, smt);
		}
		
		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());
		ListModel strset = new SimpleListModel(
				Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek));
		grid.setRowRenderer(new DataRenderer(false));
		grid.setModelCheckMobile(strset);

	}
}
