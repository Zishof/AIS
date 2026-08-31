package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.MatakuliahBerbayarDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.MatakuliahBerbayar;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk matakuliah berbayar. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchFakultas}, {@code Combobox searchJurusan}, {@code Combobox
 * searchJenjang}, {@code Combobox searchSemester}, {@code Combobox searchProgram}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class MatakuliahBerbayarAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4733551737383264330L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchFakultas;
	private Combobox searchJurusan;
	private Combobox searchJenjang;
	private Combobox searchSemester;
	private Combobox searchProgram;
	private Combobox searchTahunAjaran;
	private Combobox searchGanjilGenap;

	private Textbox nama;
	private Textbox deskripsi;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox jenjang;
	private Combobox semester;
	private Combobox program;
	private boolean edit;
	private boolean delete;
	private Combobox tahunAjaran;
	private Combobox ganjilGenap;

	private MatakuliahBerbayar matakuliahBerbayar;

	private MyToolbarbuttonConfig add;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.insertCombo(searchFakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		Common.insertComboDanSemua(searchJenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(fakultas = new Combobox(), new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		MyComboitemConfig comboitem1 = new MyComboitemConfig();
		if (comboitem1 != null) { comboitem1.setLabel("Semua"); }
		if (comboitem1 != null) { comboitem1.setValue(null); }
		fakultas.appendChild(comboitem1);
		Common.insertCombo(jurusan = new Combobox(), "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		if (searchTahunAjaran != null) { searchTahunAjaran.setSelectedItem(null); }

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		for (int i = 1; i < maxSemesterPilihan; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			searchSemester.appendChild(comboitem);
		}

		semester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(0); }
		semester.appendChild(comboitem);

		for (int i = 1; i < maxSemesterPilihan; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		program = Common.initPrograms(program);

		Common.initPrograms(searchProgram);

		ganjilGenap = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		ganjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchGanjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchGanjilGenap.appendChild(comboitem);

		/**
		 * Event listener lokal milik {@link MatakuliahBerbayarAction}. Kelas ini menangani event untuk komponen induk
		 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MatakuliahBerbayarAction} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see MatakuliahBerbayarAction
		 */
		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchJurusan);
				searchJurusan.setSelectedItem(null);
				if (searchFakultas.getSelectedItem() == null) {
					return;
				}
				Common.insertCombo(searchJurusan, "nama", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchFakultas, false));

			}

		}

		/**
		 * Event listener lokal milik {@link MatakuliahBerbayarAction}. Kelas ini menangani event untuk komponen induk
		 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MatakuliahBerbayarAction} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see MatakuliahBerbayarAction
		 */
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				} else {
					Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
				}

			}

		}

		searchFakultas.addEventListener("onChange", new SearchFakultasEventListener());

		fakultas.addEventListener("onChange", new FakultasEventListener());

		Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		MyComboitemConfig comboitem2 = new MyComboitemConfig();
		if (comboitem2 != null) { comboitem2.setValue(null); }
		if (comboitem2 != null) { comboitem2.setLabel("Semua"); }
		jenjang.appendChild(comboitem2);

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
			Common.selectComboItem(searchFakultas, tbmuser.ambilFakultas());
			Common.clear(jurusan);
			Common.clear(searchJurusan);
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			Common.insertCombo(searchJurusan, "nama", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			fakultas.setDisabled(true);
			searchFakultas.setDisabled(true);
		} else {
			fakultas.setDisabled(false);
			searchFakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());
			Common.selectComboItem(searchJurusan, tbmuser.ambilJurusan());
			jurusan.setDisabled(true);
			searchJurusan.setDisabled(true);
		} else {
			jurusan.setDisabled(false);
			searchJurusan.setDisabled(false);
		}

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link MatakuliahBerbayarAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MatakuliahBerbayarAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see MatakuliahBerbayarAction
	 */
	class MatakuliahBerbayarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MatakuliahBerbayar matakuliahBerbayar = (MatakuliahBerbayar) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen())
						;

				}

			});

			RevisiHelper.createNewRevisi(MatakuliahBerbayar.class, matakuliahBerbayar, matakuliahBerbayar.getNama())
					.setParent(arg0);

			new Label(matakuliahBerbayar.getDeskripsi()).setParent(arg0);
			new Label(
					matakuliahBerbayar.getTahunAjaran() == null || matakuliahBerbayar.getTahunAjaran().trim().equals("")
							? "Semua" : matakuliahBerbayar.getTahunAjaran()).setParent(arg0);

			new Label(matakuliahBerbayar.getFakultas() == null ? "Semua" : matakuliahBerbayar.getFakultas().getNama())
					.setParent(arg0);
			new Label(matakuliahBerbayar.getJurusan() == null ? "Semua" : matakuliahBerbayar.getJurusan().getNama())
					.setParent(arg0);
			new Label(matakuliahBerbayar.getSemester() == null || matakuliahBerbayar.getSemester().equals(0) ? "Semua"
					: matakuliahBerbayar.getSemester() + "").setParent(arg0);
			new Label(matakuliahBerbayar.getJenjang() == null ? "Semua" : matakuliahBerbayar.getJenjang().getNama())
					.setParent(arg0);
			new Label(matakuliahBerbayar.getProgram() == null || matakuliahBerbayar.getProgram().trim().equals("")
					? "Semua" : matakuliahBerbayar.getProgram()).setParent(arg0);
			new Label(matakuliahBerbayar.getGanjilGenap() == null ? "Semua" : matakuliahBerbayar.getGanjilGenap())
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(matakuliahBerbayar);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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

											Common.refreshDelete(matakuliahBerbayar);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new MatakuliahBerbayar());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(MatakuliahBerbayar matakuliahBerbayar) {
		this.matakuliahBerbayar = matakuliahBerbayar;
		addWindow.setTitle(matakuliahBerbayar.getId() == null ? "Tambah Kalender Akademik" : "Ubah Kalender Akademik");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan"));
		row.appendChild(nama = new Textbox(matakuliahBerbayar.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Kegiatan"));
		row.appendChild(deskripsi = new Textbox(matakuliahBerbayar.getNama()));
		deskripsi.setWidth("90%");
		deskripsi.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAjaran = Common.generateTahunAjaran(tahunAjaran);
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");
		Common.sisipkanSemuaDiCombo(tahunAjaran, null);

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				matakuliahBerbayar.getFakultas() == null ? tbmuser.ambilFakultas() : matakuliahBerbayar.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		Common.sisipkanSemuaDiCombo(fakultas, null);

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				matakuliahBerbayar.getJurusan() == null ? tbmuser.ambilJurusan() : matakuliahBerbayar.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.sisipkanSemuaDiCombo(jurusan, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang, matakuliahBerbayar.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");
		Common.sisipkanSemuaDiCombo(jenjang, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		Common.selectComboItem(semester, matakuliahBerbayar.getSemester());
		row.appendChild(semester);
		semester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, matakuliahBerbayar.getProgram());
		row.appendChild(program);
		program.setWidth("90%");
		Common.sisipkanSemuaDiCombo(program, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ganjil/Genap"));
		Common.selectComboItem(ganjilGenap, matakuliahBerbayar.getGanjilGenap());
		row.appendChild(ganjilGenap);
		ganjilGenap.setWidth("90%");
		Common.sisipkanSemuaDiCombo(ganjilGenap, null);

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data kegiatan akademik",
					"Kolom Nama kegiatan akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama kegiatan akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		// if (program.getValue().trim().equals("")) {
		// MyMessageboxConfig.show("Program Oleh harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		MatakuliahBerbayarDao matakuliahBerbayarDao = DaoFactory.getInstance().getMatakuliahBerbayarDao();
		if (matakuliahBerbayar.getId() != null) {
			matakuliahBerbayar = matakuliahBerbayarDao.load(matakuliahBerbayar.getId());
		}
		matakuliahBerbayar.setDeskripsi(deskripsi.getValue());
		matakuliahBerbayar.setNama(nama.getValue());
		matakuliahBerbayar.setTahunAjaran(
				tahunAjaran.getSelectedItem() == null ? null : (String) tahunAjaran.getSelectedItem().getValue());
		matakuliahBerbayar.setGanjilGenap(
				ganjilGenap.getSelectedItem() == null ? null : (String) ganjilGenap.getSelectedItem().getValue());

		matakuliahBerbayar.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		matakuliahBerbayar
				.setJurusan((Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? null : jurusan.getSelectedItem().getValue()));
		matakuliahBerbayar.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));

		matakuliahBerbayar.setSemester(
				semester.getSelectedItem() == null ? null : (Integer) semester.getSelectedItem().getValue());
		matakuliahBerbayar.setProgram(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
				|| program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue().toString());

		// matakuliahBerbayarDao.beginTransaction();
		if (matakuliahBerbayar.getId() != null) {
			matakuliahBerbayarDao.update(matakuliahBerbayar);
		} else {
			matakuliahBerbayarDao.save(matakuliahBerbayar);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MatakuliahBerbayar.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(
				searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))
				.add(searchGanjilGenap.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ganjilGenap", searchGanjilGenap.getSelectedItem().getValue()))

				.add(searchSemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("semester", searchSemester.getSelectedItem().getValue()))
				.add(searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchProgram.getSelectedItem() == null ? "Reguler"
								: searchProgram.getSelectedItem().getValue()))

				.add(searchFakultas.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchFakultas, false))
				.add(searchJurusan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchJurusan, false))
				.add(searchJenjang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchJenjang.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<MatakuliahBerbayar> matakuliahBerbayar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(matakuliahBerbayar);
		grid.setRowRenderer(new MatakuliahBerbayarRenderer());
		grid.setModelCheckMobile(strset);

	}

}
