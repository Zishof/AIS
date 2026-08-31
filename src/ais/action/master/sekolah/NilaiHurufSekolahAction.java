package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.JenisNilaiHuruf;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk nilai huruf sekolah. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}, {@code Textbox
 * searchnilaiHuruf}, {@code Decimalbox searchtahunAngkatan}, {@code Decimalbox mulai}; inisialisasi/lifecycle
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
public class NilaiHurufSekolahAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 261036075526361529L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Textbox searchnilaiHuruf;
	private Decimalbox searchtahunAngkatan;

	private Decimalbox mulai;
	private Decimalbox sampai;
	private Textbox nilaiHurufSekolah;
	private Decimalbox tahunAngkatan;
	private Textbox keterangan;

	private Combobox tahunAkademik;
	private Combobox semester;

	private NilaiHurufSekolah nilai;
	private boolean edit;
	private boolean delete;

	private Combobox yayasan;

	private Combobox sekolah;

	private Combobox jenisNilaiHuruf;

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

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link NilaiHurufSekolahAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link NilaiHurufSekolahAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see NilaiHurufSekolahAction
	 */
	class NilaiHurufSekolahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final NilaiHurufSekolah nilai = (NilaiHurufSekolah) arg1;

			RevisiHelper.createNewRevisi(NilaiHurufSekolah.class, nilai, nilai.getMulai().toString()).setParent(arg0);

			new Label(nilai.getSampai().toString()).setParent(arg0);

			new Label(nilai.getNilaiHuruf()).setParent(arg0);
			new Label(nilai.getTahunAngkatan() == null ? "" : nilai.getTahunAngkatan().toString()).setParent(arg0);

			new Label(nilai.getJenisNilaiHuruf() == null ? "" : nilai.getJenisNilaiHuruf().getNama()).setParent(arg0);
			new Label((nilai.getTahunAkademik() == null || nilai.getTahunAkademik().trim().isEmpty() ? " Semua "
					: nilai.getTahunAkademik()) + " / "
					+ (nilai.getSemester() == null || nilai.getSemester().trim().isEmpty() ? " Semua "
							: nilai.getSemester())
					+ " / " + nilai.getTa()).setParent(arg0);
			new Label(nilai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Lulus");
			checkbox.setChecked(nilai.getLulus());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					nilai.setLulus(checkbox.isChecked());
					Common.refreshSaveOrUpdate(nilai);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(nilai);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

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

											Common.refreshDelete(nilai);

											onSearchDefault(event);
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
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new NilaiHurufSekolah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final NilaiHurufSekolah nilai) {
		this.nilai = nilai;
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new Decimalbox(new BigDecimal(nilai.getMulai() == null ? 0 : nilai.getMulai())));
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new Decimalbox(new BigDecimal(nilai.getSampai() == null ? 0 : nilai.getSampai())));
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Huruf"));
		row.appendChild(nilaiHurufSekolah = new Textbox(nilai.getNilaiHuruf()));
		// nilaiHurufSekolah.setDisabled(true);
		nilaiHurufSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Minimal"));
		row.appendChild(tahunAngkatan = new Decimalbox(
				new BigDecimal(nilai.getTahunAngkatan() == null ? 0 : nilai.getTahunAngkatan())));
		tahunAngkatan.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, nilai.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, nilai.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		tahunAkademik = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		tahunAkademik.appendChild(comboitem);
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);

		semester = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Tahun Ajaran"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, nilai.getTahunAkademik());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Semester"));
		row.appendChild(semester);
		semester.setWidth("90%");
		Common.selectComboItem(semester, nilai.getSemester());
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Nilai Huruf"));
		row.appendChild(jenisNilaiHuruf = new Combobox());
		jenisNilaiHuruf.setWidth("90%");
		jenisNilaiHuruf.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(jenisNilaiHuruf, new String[] { "nama", "kode" }, "keterangan",
						JenisNilaiHuruf.class, "=Tanpa Jenis Nilai Huruf=",
						Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));
				Common.selectComboItem(true, jenisNilaiHuruf, nilai.getJenisNilaiHuruf());

			}

		};

		sekolah.addEventListener("onChange", eventListener);
		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(nilai.getKeterangan()));
		// nilaiHurufSekolah.setDisabled(true);
		nilaiHurufSekolah.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
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

		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Yayasan belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Yayasan dan pilih yayasan yang sesuai; (2) pastikan yayasan terpilih sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Sekolah dan pilih sekolah yang sesuai; (2) pastikan yayasan sudah dipilih agar daftar sekolah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (nilai.getId() != null) {
			nilai = (NilaiHurufSekolah) session.load(NilaiHurufSekolah.class, nilai.getId());
		}
		nilai.setMulai(mulai.getValue().doubleValue());
		nilai.setSampai(sampai.getValue().doubleValue());
		nilai.setTahunAngkatan(tahunAngkatan.getValue().intValue());
		nilai.setNilaiHuruf(nilaiHurufSekolah.getValue());
		nilai.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		nilai.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());

		nilai.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? null
						: tahunAkademik.getSelectedItem().getValue()));
		nilai.setSemester((String) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		nilai.setJenisNilaiHuruf((JenisNilaiHuruf) (jenisNilaiHuruf.getSelectedItem() == null ? null
				: jenisNilaiHuruf.getSelectedItem().getValue()));

		nilai.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, nilai);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ConstantValues.realoadNilaiHurufSekolah(HibernateUtil.currentSession());
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(NilaiHurufSekolah.class);
		if (order)
			criteria.addOrder(Order.asc("nilaiHuruf"));
		criteria.add(searchnilaiHuruf.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nilaiHuruf", searchnilaiHuruf.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchtahunAngkatan.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAngkatan", searchtahunAngkatan.getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<NilaiHurufSekolah> nilai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nilai);
		grid.setRowRenderer(new NilaiHurufSekolahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
