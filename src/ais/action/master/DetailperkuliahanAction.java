package ais.action.master;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.TransferDataMahasiswaHelper;
import ais.action.master.helper.UtsDanUasCheckerHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPenilaian;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.Komentar;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk detailperkuliahan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchnama}, {@code Textbox searchmk}, {@code MyCheckboxConfig searchBelumMasukFeeder}, {@code
 * MyCheckboxConfig searchMasukFeeder}, {@code Combobox searchTahunAjaran}, {@code Combobox searchsemester};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code lakukanSatuPersetujuan()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class DetailperkuliahanAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchmk;
	private MyCheckboxConfig searchBelumMasukFeeder;
	private MyCheckboxConfig searchMasukFeeder;

	protected Combobox searchTahunAjaran;
	protected Combobox searchsemester;
	protected Combobox searchJenisSemester;
	protected Combobox searchprogram;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;

	protected AmbilDataDosenBanbox searchdosen;

	private MyCheckboxConfig searchaktif;
	private MyCheckboxConfig searchtdkaktif;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig find;
	private Tbmuser tbmuser;
	private boolean approve;
	private boolean tampiHapus;

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
		tbmuser = Common.getCurrentUser();

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);

				if (searchJenisSemester.getSelectedItem() == null) {
					return;
				}
				if (searchJenisSemester.getSelectedItem().getValue() == null) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					for (int i = 1; i < 30; i++) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					Boolean genap = searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					if (genap) {
						for (int i : Common.genap) {
							if (i == 0)
								continue;
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					} else {
						for (int i : Common.ganjil) {
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					}
				}

				searchsemester.setSelectedIndex(0);
				searchsemester.setReadonly(true);
			}
		};

		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchJenisSemester.appendChild(comboitem);

		// Default Jenis Semester = SEMESTER SAAT INI (abaikan konfigurasi
			// 'pilihan_semester_di_perkuliahan_dibuat_default_semua_aja'; selalu pakai isNowSemensterGanjil()).
			Common.selectComboItem(searchJenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		Common.initPrograms(searchprogram);

		searchJenisSemester.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);
		if ((tbmuser != null && tbmuser.getMahasiswa() != null)) {
			searchfakultas.setDisabled(false);
			searchjurusan.setDisabled(false);

			searchfakultas.setSelectedIndex(-1);
			searchjurusan.setSelectedIndex(-1);
		}

		boolean adminLainBoleh = false;
		String admLain = Common
				.getKonfigurasi("admin_lain_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", "").getNilai();
		String[] aa = admLain.split(";");
		for (String a : aa) {
			try {
				adminLainBoleh = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
				if (adminLainBoleh) {
					break;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);

			}
		}
		tampiHapus = adminLainBoleh || (Common.bolehKonfigurasi("admin_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", Konfigurasi.TIDAK_AKTIF) && Common.getApakahAdmin());

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama", "perkuliahan.matakuliah.kode",
				"perkuliahan.matakuliah.nama", "matakuliahKonversi.kode", "matakuliahKonversi.nama",
				"perkuliahan.kelas", "semester", "tahunAkademik", "persetujuan", "totalNilai", "nilaiHuruf", "totalIP",
				"kodeMatakuliahAsal", "namaMatakuliahAsal", "nilaiHurufAsal", "sksAsal", "feeder" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Detailperkuliahan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link DetailperkuliahanAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DetailperkuliahanAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DetailperkuliahanAction
	 */
	class DetailperkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			row.setValign("top");

			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) arg1;
			Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
					? detailperkuliahan.getMatakuliahKonversi()
					: detailperkuliahan.getPerkuliahan().getMatakuliah();
			final Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
					mahasiswa == null ? null : mahasiswa.getNim(), false);
			final Integer semester = detailperkuliahan.getSemester();
			final Integer tahapan = detailperkuliahan.getTahap();
			final Integer semesterPendek = detailperkuliahan.getPerkuliahan() == null ? null
					: detailperkuliahan.getPerkuliahan().getStatusSemesterPendek();

			matakuliah = matakuliahs[0];
			Matakuliah matakuliahAsli = matakuliahs[1];
			if (detailperkuliahan == null || matakuliah == null) {
				row.setVisible(false);

				return;
			}

			String jenisSemester = detailperkuliahan.getPerkuliahan() == null ? null: detailperkuliahan.getPerkuliahan().getGanjilGenap();
			String tahunAjaran = detailperkuliahan.getTahunAkademik();
			Konfigurasi konfigurasiPersetujuanKrsDosen = CommonPenilaian.getKonfigurasiPersetujuanKrsOlehDosen(
					detailperkuliahan.getTahunAkademik(), jenisSemester, semesterPendek);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			final Vbox totalNilai = new Vbox();
			List<FormatNilai> formatNilais = null;
			if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
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
					totalNilai.appendChild(
							new MyLabelAgakKecil(formatNilai.getNama() + ":" + Common.numberFormat.get().format(n)));
				}
			}

			final MyLabelAgakKecil totalNilaiMhs = new MyLabelAgakKecil(
					"Total:" + Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()));
			if (formatNilais == null || formatNilais.isEmpty() || formatNilais.size() > 1) {
				totalNilai.appendChild(totalNilaiMhs);
			}

			checkbox.setChecked(detailperkuliahan.getPersetujuan() != null
					&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI));
			checkbox.setDisabled(detailperkuliahan.getTotalNilai() > 1.0);
			if (!approve) {
				checkbox.setDisabled(true);
			}
			if (!konfigurasiPersetujuanKrsDosen.checkAktif()) {
				new Label(detailperkuliahan.getPersetujuan() != null
						&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI) ? "Ya" : "Belum")
						.setParent(row);
			} else {
				checkbox.setParent(row);
				row.setValign("top");row.setAttribute("checkbox", checkbox);
			}
			checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
							semesterPendek);

					lakukanSatuPersetujuan(checkbox, detailperkuliahan, krsMahasiswa, semester);
				}
			});

			String tambahan = "";
			try {
				if (detailperkuliahan.getPerkuliahan() != null
						&& !tahunAjaran.equals(detailperkuliahan.getPerkuliahan().getTahunAjaran())) {
					tambahan = "(ikut kuliah di TA " + detailperkuliahan.getPerkuliahan().getTahunAjaran() + ")";
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			RevisiHelper
					.createNewRevisi(Detailperkuliahan.class, detailperkuliahan,
							(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode()
									: (matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")")) + " " + tambahan)
					.setParent(vbox);
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
				if (detailperkuliahan.getFeeder() != null && !detailperkuliahan.getFeeder().trim().isEmpty()) {
					vbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					vbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					vbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					vbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}

			vbox = new Vbox();
			vbox.setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama()
					: (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")")).setParent(vbox);

			if (detailperkuliahan.getPerkuliahan() != null
					&& detailperkuliahan.getPerkuliahan().getKurikulum() != null) {
				new Label("Kurikulum : " + detailperkuliahan.getPerkuliahan().getKurikulum().getNama()).setParent(vbox);
			}

			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "")
					: (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")).setParent(row);

			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, true);

			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(row, perkuliahan, detailperkuliahan);

			try {
				if (detailperkuliahan.getSemester().equals(perkuliahan.getSemester())) {
					new Label(detailperkuliahan.getSemester() + "").setParent(row);
				} else {
					new MyLabelKecil((detailperkuliahan.getSemester() + " / " + perkuliahan.getSemester()
							+ (detailperkuliahan.getSemester() > perkuliahan.getSemester() ? " (Mengulang)"
									: " (Menabung)")))
							.setParent(row);
				}
			} catch (Exception e) {
				new Label(detailperkuliahan.getSemester() + "").setParent(row);
			}

			new Label(detailperkuliahan.getPerkuliahan() == null || detailperkuliahan.getPerkuliahan() == null ? ""
					: detailperkuliahan.getPerkuliahan().getKelas()).setParent(row);

			new Label(detailperkuliahan.getPerkuliahan() == null ? ""
					: detailperkuliahan.getPerkuliahan().getTahunAjaran()).setParent(row);

			if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {

				Map<Long, List<String>> dataAbsensi = new HashMap<Long, List<String>>();
				List<Pertemuan> statusPertemuan = perkuliahan.ambilPertemuanList();
				for (Pertemuan pertemuan : statusPertemuan) {
					if (pertemuan.getPerkuliahan() != null) {
						if (!dataAbsensi.containsKey(pertemuan.getPerkuliahan().getId())) {
							dataAbsensi.put(pertemuan.getPerkuliahan().getId(), new ArrayList<String>());
							dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
						} else {
							dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
						}
					}
				}
				statusPertemuan.clear();
				statusPertemuan = null;

				vbox = new Vbox();
				vbox.setParent(row);
				detailperkuliahan.getPerkuliahan();
				Map<String, Integer> statuses = Perkuliahan.hitungStatus(
						dataAbsensi.get(detailperkuliahan.getPerkuliahan().getId()),
						detailperkuliahan.getMahasiswa().getId());

				int semua = statuses.get("T") == null ? 0 : statuses.get("T");
				int masuk = statuses.get("M") == null ? 0 : statuses.get("M");

				for (String key : statuses.keySet()) {
					if (!key.equals("T")) {
						int v = statuses.get(key);
						vbox.appendChild(new MyLabelAgakKecil(key + "=" + v + ","));
					}
				}
				double persen = semua == 0 ? 0.0 : (masuk * 100.0) / semua;
				vbox.appendChild(new MyLabelAgakKecil("T=" + semua + "(" + Common.numberFormat.get().format(persen) + "%)"));
			} else {
				new Label().setParent(row);
			}

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/stock_data_edit_table.png");
			button.setTooltiptext("Pindah Data");
			button.setVisible(
					detailperkuliahan.getPerkuliahan() != null && Common.getCurrentUser().getDosen() == null && edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah yakin ingin memindahkan krs mahasiswa " + detailperkuliahan.getMahasiswa().getNama()
									+ " matakuliah " + detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
									+ " ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											TransferDataMahasiswaHelper transferDataMahasiswaHelper = new TransferDataMahasiswaHelper(
													detailperkuliahan.getPerkuliahan(),
													detailperkuliahan.getMahasiswa());
											MyWindow window = new MyWindow();
											window.setParent(
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											transferDataMahasiswaHelper.display(new DataLoader() {

												@Override
												public void loadData(Object value) {
													onSearchDefault(null);
												}
											}, window);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);

										}

									}

								}
							});

				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(edit && detailperkuliahan.getPerkuliahan() != null && Common.getCurrentUser().getDosen() == null
					&& delete && tampiHapus);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											List<Komentar> komentars = session.createCriteria(Komentar.class).add(
													Restrictions.eq("detailperkuliahan", detailperkuliahan.getId()))
													.list();

											for (Komentar komentar : komentars) {
												Common.refreshDelete((komentar));
											}

											Common.refreshDelete(session, detailperkuliahan);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});

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
			aksiButtons.add(button);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")
					&& (mahasiswa != null && mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().isEmpty())) {

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.setOrient("vertical");
				buttonTagihan.setStyle("font-size:8px;");
				buttonTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

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

											final Label myLabelProsesDetail = Common
													.displayLoadBar(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															if (arg0 != null && !arg0.getName().isEmpty()) {
																EksporFromFeederAction.display();
																MyMessageboxConfig.show(arg0.getName(), "Info",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);
															}

															if (!errorLog.isEmpty()) {
																String err = "";
																for (String s : errorLog) {
																	err += err.isEmpty() ? s
																			: "\n----------------------------------------------------------------------------------------------------------\n"
																					+ s;
																}

																MyMessageboxConfig.show(err, "Error Terjadi",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);

																File file = new File(Common.REAL_PATH + "/tmp/error_"
																		+ Common.randLong() + ".txt");

																if (!file.getParentFile().exists()) {
																	file.getParentFile().mkdirs();
																}
																FileUtils.writeStringToFile(file, err);
																Filedownload.save(file, "text/plain");
															}

															onSearchDefault(arg0);
														}
													});

											new Thread(new Runnable() {

												@Override
												public void run() {
													try {
														FeederConnector feederConnector = new FeederConnector(ip,
																Integer.parseInt(port), myLabelProsesDetail);

														String token = feederConnector.getToken(username, password);
														System.out.println("TOKEN => " + token);

														if (token == null || token.trim().isEmpty()
																|| token.trim().toLowerCase().startsWith("error")) {
															myLabelProsesDetail
																	.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null, null);
														myLabelProsesDetail
																.setValue("Mengirim data " + detailperkuliahan);
														Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
														if (perkuliahan != null) {
															PerkuliahanAction.kirimKeFeeder(feederImporter,
																	detailperkuliahan, feederConnector, token,
																	mahasiswa, errorLog, true);
														} else if (detailperkuliahan.getMatakuliahKonversi() != null) {

															feederImporter.nilaiTransfer(detailperkuliahan, errorLog);
														}
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}

													myLabelProsesDetail.setValue("");
												}
											}).start();

										}

									}
								});

					}
				});
				aksiButtons.add(buttonTagihan);

			}

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);

		}

	}

	private void lakukanSatuPersetujuan(MyCheckboxConfig checkbox, Detailperkuliahan seledtedDetailperkuliahan,
			KrsMahasiswa krsMahasiswa, final int semester) throws Exception {

		Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_persetujuan_krs",
				Konfigurasi.AKTIF);
		Mahasiswa mahasiswa = seledtedDetailperkuliahan.getMahasiswa();
		Integer tahapan = seledtedDetailperkuliahan.getTahap();
		Integer semesterPendek = seledtedDetailperkuliahan.getPerkuliahan() == null ? null
				: seledtedDetailperkuliahan.getPerkuliahan().getStatusSemesterPendek();
		if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
			if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null || tahapan.equals(0)) {
				if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, true,
						semesterPendek != null)) {
					MyMessageboxConfig.show(
							"\"" + mahasiswa.getNama() + "\" belum membayar biaya perkuliahan di semester " + semester
									+ ". Harap mahasiswa tsb untuk segera menghubungi bagian keuangan",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
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
					harusLunas = Double.parseDouble(Common
							.getKonfigurasi("batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_disetujui_krs",
									"90")
							.getNilai().trim());
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				MyMessageboxConfig.show(
						"\"" + mahasiswa.getNama() + "\" belum melunasi " + harusLunas + "% biaya perkuliahan di "
								+ ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan != null && tahapan > 0)
										? " tahap " + (tahapan - 1)
										: " semester " + (semester - 1))
								+ ". Harap mahasiswa tsb untuk segera menghubungi bagian keuangan",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
		}

		Session session = HibernateUtil.currentSession();
		seledtedDetailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.idEq(seledtedDetailperkuliahan.getId())).uniqueResult();
		seledtedDetailperkuliahan
				.setPersetujuan(checkbox.isChecked() ? Detailperkuliahan.DISETUJUI : Detailperkuliahan.BELUM_DISETUJUI);
		Common.refreshUpdate(session, (seledtedDetailperkuliahan));

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (krsMahasiswa != null)
			krsMahasiswa.masukkanData(checkbox.isChecked() ? "setujui" : "batalkan");
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criterion criterion = searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("mahasiswa.dosenPa", searchdosen.getAttribute("myValue"));

		Criteria criteria = session.createCriteria(Detailperkuliahan.class)

				.add(searchBelumMasukFeeder != null && searchBelumMasukFeeder.isChecked()
						? Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", ""))

						: Restrictions.sqlRestriction("true"))

				.add(searchMasukFeeder != null && searchMasukFeeder.isChecked()
						? Restrictions.or(Restrictions.isNotNull("feeder"), Restrictions.ne("feeder", ""))

						: Restrictions.sqlRestriction("true"))

				.createAlias("mahasiswa", "mahasiswa").createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
				.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
				.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN).add(criterion)
				.add(searchaktif == null || searchaktif.isChecked() ? Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI)
						: Restrictions.sqlRestriction("true"))

				.add(searchtdkaktif.isChecked() ? Restrictions.eq("persetujuan", Detailperkuliahan.BELUM_DISETUJUI)
						: Restrictions.sqlRestriction("true"))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.program", searchprogram.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions
										.in("semester",
												searchJenisSemester.getSelectedItem().getValue()
														.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))

				.add(searchsemester.getSelectedItem() == null || searchsemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))

				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

		;

		if (order)
			criteria.addOrder(Order.desc("id"));
		
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("mahasiswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchmk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matakuliah.kode", searchmk.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("matakuliah.nama", searchmk.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("matakuliahKonversi.kode",
														searchmk.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.ilike("matakuliahKonversi.nama",
														searchmk.getValue().trim(), MatchMode.ANYWHERE)))))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Detailperkuliahan> detailperkuliahan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(detailperkuliahan);
		grid.setRowRenderer(new DetailperkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
