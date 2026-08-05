package ais.action.master.koperasi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.ModalPenyertaanKoperasi;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>ModalPenyertaanKoperasiAction — Pencatatan Modal Penyertaan &amp; Ringkasannya</h2>
 *
 * <p>
 * Halaman ini adalah tempat pengurus koperasi mencatat <b>modal penyertaan</b>: dana penguat modal
 * yang ditanam anggota atau pihak lain (investor) di luar simpanan pokok/wajib, sebagaimana diatur UU
 * Perkoperasian dan SOM USPK. Selain daftar CRUD (tambah/ubah/hapus) memakai pola baku modul, halaman
 * menampilkan <b>ringkasan</b> berupa kartu (total modal penyertaan aktif, jumlah penyerta, perkiraan
 * imbal hasil per tahun) dan grafik komposisi anggota vs non-anggota (donut HTML/CSS), sehingga
 * pengurus cepat memahami kekuatan permodalan tambahan koperasi.
 * </p>
 *
 * <h3>Kaidah teknis</h3>
 * <p>
 * Seluruh operasi baca/tulis memakai {@link HibernateUtil#currentSession()} yang ditutup otomatis oleh
 * kerangka (tidak ditutup manual). Ringkasan dihitung di memori secara aman-null dan digambar ulang
 * setiap daftar dimuat. Kode kompatibel Java 1.7, memaksimalkan pemakaian ulang ({@link DashboardUiKit}
 * untuk kartu/donut) dengan penjelasan sederhana bagi pengguna awam, serta tidak mengubah perilaku
 * modul lain.
 * </p>
 *
 * @see ModalPenyertaanKoperasi
 */
public class ModalPenyertaanKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = 6620270014412991010L;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd-MM-yyyy");

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Div ringkasanHost;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Combobox koperasi;
	private Combobox jenisPenyerta;
	private Textbox namaPenyerta;
	private Textbox nomorPerjanjian;
	private MyDoublebox nominal;
	private MyDatebox tanggalMasuk;
	private MyIntbox jangkaWaktuBulan;
	private MyDoublebox imbalHasilPersen;
	private Combobox status;
	private MyDatebox tanggalKembali;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ModalPenyertaanKoperasi modalPenyertaanKoperasi;
	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class ModalPenyertaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final ModalPenyertaanKoperasi m = (ModalPenyertaanKoperasi) arg1;

			String jenis = ModalPenyertaanKoperasi.JENIS_NON_ANGGOTA.equals(m.getJenisPenyerta()) ? "Non-Anggota"
					: "Anggota";
			new Label((m.getNamaPenyerta() == null ? "-" : m.getNamaPenyerta()) + " (" + jenis + ")").setParent(arg0);
			new Label("Rp " + DashboardUiKit.money(m.getNominal())).setParent(arg0);
			new Label(m.getTanggalMasuk() == null ? "-" : SDF.format(m.getTanggalMasuk())).setParent(arg0);
			new Label(m.getTanggalJatuhTempo() == null ? "-" : SDF.format(m.getTanggalJatuhTempo())).setParent(arg0);
			new Label(DashboardUiKit.money(m.getImbalHasilPersen()) + " %").setParent(arg0);
			new Label(labelStatus(m.getStatus())).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(m.getAktif());
			checkbox.setParent(arg0);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					m.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(m);
					onSearchDefault(null);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, m, ModalPenyertaanKoperasiAction.this).setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		ModalPenyertaanKoperasi baru = new ModalPenyertaanKoperasi();
		baru.setTanggalMasuk(new Date());
		init(baru);
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		buildForm((ModalPenyertaanKoperasi) obj);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void buildForm(ModalPenyertaanKoperasi m) {
		this.modalPenyertaanKoperasi = m;
		addWindow.setTitle(m.getId() == null ? "Tambah Modal Penyertaan" : "Ubah Modal Penyertaan");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koperasi"));
		row.appendChild(koperasi = new Combobox());
		Common.insertCombo(koperasi, "nama", Koperasi.class, Restrictions.eq("aktif", true));
		Koperasi myKoperasi = Common.getCurrentKoperasi();
		if (m.getKoperasi() != null) {
			Common.selectComboItem(true, koperasi, m.getKoperasi());
		} else if (myKoperasi != null) {
			Common.selectComboItem(true, koperasi, myKoperasi);
		}
		koperasi.setWidth("90%");
		koperasi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penyerta *"));
		row.appendChild(jenisPenyerta = new Combobox());
		Comboitem ci = new Comboitem("Anggota");
		ci.setValue(ModalPenyertaanKoperasi.JENIS_ANGGOTA);
		jenisPenyerta.appendChild(ci);
		ci = new Comboitem("Non-Anggota / Investor");
		ci.setValue(ModalPenyertaanKoperasi.JENIS_NON_ANGGOTA);
		jenisPenyerta.appendChild(ci);
		Common.selectComboItem(jenisPenyerta, m.getJenisPenyerta());
		jenisPenyerta.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Penyerta *"));
		row.appendChild(namaPenyerta = new Textbox(m.getNamaPenyerta()));
		namaPenyerta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Perjanjian"));
		row.appendChild(nomorPerjanjian = new Textbox(m.getNomorPerjanjian()));
		nomorPerjanjian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nominal Penyertaan *"));
		row.appendChild(nominal = new MyDoublebox(m.getNominal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Masuk"));
		try {
			row.appendChild(tanggalMasuk = new MyDatebox(m.getTanggalMasuk() == null ? new Date() : m.getTanggalMasuk()));
		} catch (Exception e) {
			row.appendChild(tanggalMasuk = new MyDatebox());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jangka Waktu (bulan, 0 = tanpa batas)"));
		row.appendChild(jangkaWaktuBulan = new MyIntbox(m.getJangkaWaktuBulan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Imbal Hasil (Persen/Tahun)"));
		row.appendChild(imbalHasilPersen = new MyDoublebox(m.getImbalHasilPersen()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(status = new Combobox());
		ci = new Comboitem("Aktif");
		ci.setValue(ModalPenyertaanKoperasi.STATUS_AKTIF);
		status.appendChild(ci);
		ci = new Comboitem("Jatuh Tempo");
		ci.setValue(ModalPenyertaanKoperasi.STATUS_JATUH_TEMPO);
		status.appendChild(ci);
		ci = new Comboitem("Ditarik / Dikembalikan");
		ci.setValue(ModalPenyertaanKoperasi.STATUS_DITARIK);
		status.appendChild(ci);
		Common.selectComboItem(status, m.getStatus());
		status.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kembali (bila ditarik)"));
		try {
			row.appendChild(tanggalKembali = new MyDatebox(m.getTanggalKembali()));
		} catch (Exception e) {
			row.appendChild(tanggalKembali = new MyDatebox());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(m.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
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
		if (namaPenyerta.getValue() == null || namaPenyerta.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, nama penyerta modal belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Penyerta dengan nama lengkap; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nominal.getValue() == null || nominal.getValue() <= 0) {
			MyMessageboxConfig.show("Mohon maaf, nominal penyertaan modal harus lebih dari nol. Langkah yang dapat dilakukan: (1) isi kolom Nominal Penyertaan dengan angka lebih dari 0; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (modalPenyertaanKoperasi.getId() != null) {
			modalPenyertaanKoperasi = (ModalPenyertaanKoperasi) session.load(ModalPenyertaanKoperasi.class,
					modalPenyertaanKoperasi.getId());
		}

		modalPenyertaanKoperasi.setKoperasi((Koperasi) (koperasi.getSelectedItem() == null ? null
				: koperasi.getSelectedItem().getValue()));
		modalPenyertaanKoperasi.setJenisPenyerta((String) (jenisPenyerta.getSelectedItem() == null
				? ModalPenyertaanKoperasi.JENIS_ANGGOTA
				: jenisPenyerta.getSelectedItem().getValue()));
		modalPenyertaanKoperasi.setNamaPenyerta(namaPenyerta.getValue());
		modalPenyertaanKoperasi.setNomorPerjanjian(nomorPerjanjian.getValue());
		modalPenyertaanKoperasi.setNominal(nominal.getValue());
		modalPenyertaanKoperasi.setTanggalMasuk(tanggalMasuk.getValue());
		modalPenyertaanKoperasi.setJangkaWaktuBulan(jangkaWaktuBulan.getValue());
		modalPenyertaanKoperasi.setImbalHasilPersen(imbalHasilPersen.getValue());
		modalPenyertaanKoperasi.setStatus((String) (status.getSelectedItem() == null
				? ModalPenyertaanKoperasi.STATUS_AKTIF
				: status.getSelectedItem().getValue()));
		modalPenyertaanKoperasi.setTanggalKembali(tanggalKembali.getValue());
		modalPenyertaanKoperasi.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, modalPenyertaanKoperasi);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ModalPenyertaanKoperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (searchnama != null && !searchnama.getValue().trim().isEmpty()) {
			criteria.add(Restrictions.ilike("namaPenyerta", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		}
		if (order) {
			criteria.addOrder(Order.desc("tanggalMasuk"));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<ModalPenyertaanKoperasi> list = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(list);
		grid.setRowRenderer(new ModalPenyertaanRenderer());
		grid.setModelCheckMobile(strset);
		buildRingkasan();
	}

	/**
	 * Gambar ulang ringkasan modal penyertaan (kartu total/jumlah/imbal hasil + donut komposisi
	 * anggota vs non-anggota) pada {@code ringkasanHost}. Hanya menghitung penyertaan yang berstatus
	 * aktif. Aman bila host tidak ada (kompat ZUL lama).
	 */
	@SuppressWarnings("unchecked")
	private void buildRingkasan() {
		if (ringkasanHost == null) {
			return;
		}
		ringkasanHost.getChildren().clear();
		double totalAktif = 0.0;
		double totalImbalHasil = 0.0;
		int jumlah = 0;
		double anggota = 0.0;
		double nonAnggota = 0.0;
		try {
			Session session = HibernateUtil.currentSession();
			List<ModalPenyertaanKoperasi> aktifList = session.createCriteria(ModalPenyertaanKoperasi.class)
					.add(Restrictions.eq("status", ModalPenyertaanKoperasi.STATUS_AKTIF))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
			for (ModalPenyertaanKoperasi m : aktifList) {
				try {
					totalAktif += m.getNominal();
					totalImbalHasil += m.getEstimasiImbalHasilTahunan();
					jumlah++;
					if (ModalPenyertaanKoperasi.JENIS_NON_ANGGOTA.equals(m.getJenisPenyerta())) {
						nonAnggota += m.getNominal();
					} else {
						anggota += m.getNominal();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/ModalPenyertaanKoperasiAction.java:431");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
		kartu.add(new DashboardUiKit.Stat("Total Modal Penyertaan Aktif", "Rp " + DashboardUiKit.money(totalAktif),
				"memperkuat modal sendiri", DashboardUiKit.PRIMARY));
		kartu.add(new DashboardUiKit.Stat("Jumlah Penyerta Aktif", DashboardUiKit.money(jumlah), "penyerta",
				DashboardUiKit.ACCENT));
		kartu.add(new DashboardUiKit.Stat("Perkiraan Imbal Hasil / Tahun", "Rp " + DashboardUiKit.money(totalImbalHasil),
				"kewajiban imbal hasil", DashboardUiKit.WARN));
		ringkasanHost.appendChild(DashboardUiKit.html(DashboardUiKit
				.descChip("Kekuatan modal tambahan koperasi dari penyertaan yang masih aktif.")));
		ringkasanHost.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));

		LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
		komposisi.put("Anggota", anggota);
		komposisi.put("Non-Anggota / Investor", nonAnggota);
		ringkasanHost.appendChild(DashboardUiKit.html(DashboardUiKit.donut("Komposisi Modal Penyertaan",
				"Perbandingan modal penyertaan dari anggota dan dari pihak luar.", komposisi, true,
				"Belum ada modal penyertaan aktif.")));
	}

	/** Label ramah-baca untuk status penyertaan. */
	private String labelStatus(String s) {
		if (ModalPenyertaanKoperasi.STATUS_JATUH_TEMPO.equals(s)) {
			return "Jatuh Tempo";
		}
		if (ModalPenyertaanKoperasi.STATUS_DITARIK.equals(s)) {
			return "Ditarik";
		}
		return "Aktif";
	}
}
