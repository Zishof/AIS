package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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

import ais.action.master.koperasi.helper.SimpanPinjamUiUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.koperasi.AnggaranKasKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>AnggaranKasKoperasiAction — Perencanaan Kas (RAPB) &amp; Analisis Rencana vs Realisasi</h2>
 *
 * <p>
 * Halaman ini adalah tempat pengurus koperasi menyusun <b>rencana anggaran kas</b> tahunan (Rencana
 * Anggaran Pendapatan dan Belanja/RAPB) sebagaimana diamanatkan SOM USPK, lalu <b>membandingkannya
 * dengan realisasi</b> yang dihitung otomatis dari transaksi nyata. Dengan begitu koperasi tahu
 * apakah penerimaan dan pengeluaran kas berjalan sesuai target, dan dapat memperkirakan saldo kas
 * akhir tahun agar likuiditas tetap terjaga.
 * </p>
 *
 * <h3>Dua fungsi utama</h3>
 * <ol>
 * <li><b>CRUD Anggaran</b> — daftar rencana kas per tahun (tambah/ubah/hapus) memakai pola baku modul
 * (MyBorderlayout + MyGrid + addWindow popup). Untuk tiap tahun pengurus mengisi saldo kas awal serta
 * perkiraan tiap pos penerimaan (simpanan, angsuran pokok, jasa pinjaman, lain-lain) dan pengeluaran
 * (penyaluran pinjaman, biaya operasional, lain-lain).</li>
 * <li><b>Analisis Rencana vs Realisasi</b> — tombol "Analisis" pada tiap baris membuka jendela berisi
 * kartu ringkas dan tabel perbandingan rencana terhadap realisasi per pos, lengkap dengan selisih,
 * persentase capaian, serta perkiraan saldo kas akhir. Tabel dapat diunduh ke Excel.</li>
 * </ol>
 *
 * <h3>Sumber realisasi</h3>
 * <p>
 * Realisasi dihitung dari data yang sudah ada, difilter menurut tahun: penerimaan simpanan dari
 * setoran {@link TransaksiKoperasi} bertipe simpanan; angsuran pokok dan jasa dari
 * {@link TransaksiKoperasiDetail} yang sudah dibayar; penyaluran pinjaman dari {@link TransaksiKoperasi}
 * bertipe pinjaman. Pos biaya operasional dan penerimaan/pengeluaran lain-lain belum terekam di modul
 * simpan pinjam (berada di akunting), sehingga realisasinya ditampilkan nol dan diberi keterangan —
 * bersifat <b>managerial</b>, bukan laporan keuangan yang diaudit.
 * </p>
 *
 * <h3>Kaidah teknis</h3>
 * <p>
 * Seluruh operasi baca/tulis memakai {@link HibernateUtil#currentSession()} yang ditutup otomatis oleh
 * kerangka (tidak ditutup manual). Perhitungan realisasi dilakukan di memori secara aman-null. Kode
 * kompatibel Java 1.7, memaksimalkan pemakaian ulang ({@link SimpanPinjamUiUtil} untuk grid+Excel,
 * {@link DashboardUiKit} untuk kartu), dan tidak mengubah perilaku modul lain.
 * </p>
 *
 * @see AnggaranKasKoperasi
 */
public class AnggaranKasKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = 6620170014412771010L;

	private MyWindow addWindow;
	private MyWindow analisisWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchtahun;
	private Checkbox searchaktif;

	private Combobox koperasi;
	private MyIntbox tahun;
	private MyDoublebox saldoAwalKas;
	private MyDoublebox rencanaSimpanan;
	private MyDoublebox rencanaAngsuranPokok;
	private MyDoublebox rencanaJasaPinjaman;
	private MyDoublebox rencanaPenerimaanLain;
	private MyDoublebox rencanaPenyaluran;
	private MyDoublebox rencanaBiayaOperasional;
	private MyDoublebox rencanaPengeluaranLain;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AnggaranKasKoperasi anggaranKasKoperasi;
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

	/**
	 * Renderer lokal untuk layar/komponen {@link AnggaranKasKoperasiAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AnggaranKasKoperasiAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AnggaranKasKoperasiAction
	 */
	class AnggaranKasKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final AnggaranKasKoperasi a = (AnggaranKasKoperasi) arg1;

			new Label(String.valueOf(a.getTahun())).setParent(arg0);
			new Label("Rp " + DashboardUiKit.money(a.getTotalPenerimaanRencana())).setParent(arg0);
			new Label("Rp " + DashboardUiKit.money(a.getTotalPengeluaranRencana())).setParent(arg0);
			new Label("Rp " + DashboardUiKit.money(a.getSaldoAkhirRencana())).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(a.getAktif());
			checkbox.setParent(arg0);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0b) throws Exception {
					a.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(a);
				}
			});

			MyToolbarbuttonConfig analisis = new MyToolbarbuttonConfig("Analisis", "/img/chart-pie-icon.png");
			analisis.setTooltiptext("Bandingkan rencana dengan realisasi tahun ini");
			analisis.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					tampilAnalisis(a);
				}
			});
			analisis.setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, a, AnggaranKasKoperasiAction.this).setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		AnggaranKasKoperasi baru = new AnggaranKasKoperasi();
		baru.setTahun(Calendar.getInstance().get(Calendar.YEAR));
		init(baru);
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		buildForm((AnggaranKasKoperasi) obj);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void buildForm(AnggaranKasKoperasi a) {
		this.anggaranKasKoperasi = a;
		addWindow.setTitle(a.getId() == null ? "Tambah Anggaran Kas" : "Ubah Anggaran Kas " + a.getTahun());
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
		if (a.getKoperasi() != null) {
			Common.selectComboItem(true, koperasi, a.getKoperasi());
		} else if (myKoperasi != null) {
			Common.selectComboItem(true, koperasi, myKoperasi);
		}
		koperasi.setWidth("90%");
		koperasi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Anggaran *"));
		row.appendChild(tahun = new MyIntbox(a.getTahun()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saldo Kas Awal Tahun"));
		row.appendChild(saldoAwalKas = new MyDoublebox(a.getSaldoAwalKas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("— Rencana Penerimaan —"));
		row.appendChild(new Label(""));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Setoran Simpanan Anggota"));
		row.appendChild(rencanaSimpanan = new MyDoublebox(a.getRencanaSimpanan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengembalian Angsuran Pokok"));
		row.appendChild(rencanaAngsuranPokok = new MyDoublebox(a.getRencanaAngsuranPokok()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jasa / Bunga Pinjaman"));
		row.appendChild(rencanaJasaPinjaman = new MyDoublebox(a.getRencanaJasaPinjaman()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerimaan Lain-lain"));
		row.appendChild(rencanaPenerimaanLain = new MyDoublebox(a.getRencanaPenerimaanLain()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("— Rencana Pengeluaran —"));
		row.appendChild(new Label(""));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyaluran Pinjaman"));
		row.appendChild(rencanaPenyaluran = new MyDoublebox(a.getRencanaPenyaluran()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Operasional"));
		row.appendChild(rencanaBiayaOperasional = new MyDoublebox(a.getRencanaBiayaOperasional()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengeluaran Lain-lain"));
		row.appendChild(rencanaPengeluaranLain = new MyDoublebox(a.getRencanaPengeluaranLain()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(a.getKeterangan()));
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
		Integer th = tahun.getValue();
		if (th == null || th.intValue() < 1900) {
			MyMessageboxConfig.show("Mohon maaf, tahun anggaran belum diisi dengan benar. Langkah yang dapat dilakukan: (1) isi kolom Tahun Anggaran dengan angka tahun yang valid (contoh: 2025); (2) pastikan tahun yang diisi belum memiliki anggaran kas; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Koperasi kop = (Koperasi) (koperasi.getSelectedItem() == null ? null
				: koperasi.getSelectedItem().getValue());

		if (sudahAdaTahun(th, kop)) {
			MyMessageboxConfig.show("Anggaran kas untuk tahun " + th + " sudah ada. Langkah yang dapat dilakukan: (1) ubah tahun ke yang belum memiliki anggaran kas; (2) atau buka anggaran yang sudah ada untuk memperbarui datanya; (3) hubungi Administrator jika perlu mengatur ulang.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (anggaranKasKoperasi.getId() != null) {
			anggaranKasKoperasi = (AnggaranKasKoperasi) session.load(AnggaranKasKoperasi.class,
					anggaranKasKoperasi.getId());
		}

		anggaranKasKoperasi.setKoperasi(kop);
		anggaranKasKoperasi.setTahun(th);
		anggaranKasKoperasi.setSaldoAwalKas(saldoAwalKas.getValue());
		anggaranKasKoperasi.setRencanaSimpanan(rencanaSimpanan.getValue());
		anggaranKasKoperasi.setRencanaAngsuranPokok(rencanaAngsuranPokok.getValue());
		anggaranKasKoperasi.setRencanaJasaPinjaman(rencanaJasaPinjaman.getValue());
		anggaranKasKoperasi.setRencanaPenerimaanLain(rencanaPenerimaanLain.getValue());
		anggaranKasKoperasi.setRencanaPenyaluran(rencanaPenyaluran.getValue());
		anggaranKasKoperasi.setRencanaBiayaOperasional(rencanaBiayaOperasional.getValue());
		anggaranKasKoperasi.setRencanaPengeluaranLain(rencanaPengeluaranLain.getValue());
		anggaranKasKoperasi.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, anggaranKasKoperasi);
		return true;
	}

	private boolean sudahAdaTahun(Integer th, Koperasi kop) {
		try {
			Session session = HibernateUtil.currentSession();
			Criteria c = session.createCriteria(AnggaranKasKoperasi.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahun", th))
					.add(anggaranKasKoperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", anggaranKasKoperasi.getId()));
			if (kop != null && kop.getId() != null) {
				c.add(Restrictions.eq("koperasi.id", kop.getId()));
			}
			Number n = (Number) c.uniqueResult();
			return n != null && n.intValue() > 0;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AnggaranKasKoperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (searchtahun != null && !searchtahun.getValue().trim().isEmpty()) {
			try {
				criteria.add(Restrictions.eq("tahun", Integer.valueOf(searchtahun.getValue().trim())));
			} catch (NumberFormatException nfe) { ais.common.ErrorAuditUtil.record(nfe, "auto-audit(empty-catch) src/ais/action/master/koperasi/AnggaranKasKoperasiAction.java:404");
				// abaikan input tahun tak valid
			}
		}
		if (order) {
			criteria.addOrder(Order.desc("tahun"));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<AnggaranKasKoperasi> list = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(list);
		grid.setRowRenderer(new AnggaranKasKoperasiRenderer());
		grid.setModelCheckMobile(strset);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Analisis Rencana vs Realisasi
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Tampilkan perbandingan rencana anggaran kas dengan realisasinya untuk satu tahun pada jendela
	 * popup: kartu ringkas (total penerimaan, total pengeluaran, saldo akhir — rencana vs realisasi)
	 * dan tabel rinci per pos beserta selisih, capaian, serta perkiraan saldo kas akhir. Tabel bisa
	 * diunduh ke Excel. Read-only, memakai {@code currentSession()}.
	 */
	private void tampilAnalisis(AnggaranKasKoperasi a) throws Exception {
		if (analisisWindow == null || a == null) {
			return;
		}
		int th = a.getTahun();
		double[] real = hitungRealisasi(th);
		double realSimpanan = real[0], realAngsuran = real[1], realJasa = real[2], realPnerLain = real[3];
		double realPenyaluran = real[4], realBiayaOp = real[5], realPengLain = real[6];
		double totRencanaP = a.getTotalPenerimaanRencana();
		double totRealP = realSimpanan + realAngsuran + realJasa + realPnerLain;
		double totRencanaK = a.getTotalPengeluaranRencana();
		double totRealK = realPenyaluran + realBiayaOp + realPengLain;
		double saldoAkhirRencana = a.getSaldoAkhirRencana();
		double saldoAkhirReal = a.getSaldoAwalKas() + (totRealP - totRealK);

		Common.clear(analisisWindow);
		analisisWindow.setTitle("Analisis Anggaran Kas — Tahun " + th);
		Div host = new Div();
		host.setStyle("overflow:auto;height:100%;width:100%;padding:8px 12px;background:#f8fafc;");
		host.setParent(analisisWindow);

		List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
		kartu.add(new DashboardUiKit.Stat("Penerimaan (Realisasi)", "Rp " + DashboardUiKit.money(totRealP),
				"rencana Rp " + DashboardUiKit.money(totRencanaP), DashboardUiKit.GOOD));
		kartu.add(new DashboardUiKit.Stat("Pengeluaran (Realisasi)", "Rp " + DashboardUiKit.money(totRealK),
				"rencana Rp " + DashboardUiKit.money(totRencanaK), DashboardUiKit.WARN));
		kartu.add(new DashboardUiKit.Stat("Saldo Kas Akhir (Realisasi)", "Rp " + DashboardUiKit.money(saldoAkhirReal),
				"rencana Rp " + DashboardUiKit.money(saldoAkhirRencana), DashboardUiKit.PRIMARY));
		host.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
				"Perbandingan rencana anggaran kas dengan realisasi dari transaksi nyata tahun " + th
						+ ". Bersifat managerial (pos biaya operasional & lain-lain belum terekam di modul ini).")));
		host.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));

		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(barisAnalisis("Penerimaan — Setoran Simpanan", a.getRencanaSimpanan(), realSimpanan));
		rows.add(barisAnalisis("Penerimaan — Angsuran Pokok", a.getRencanaAngsuranPokok(), realAngsuran));
		rows.add(barisAnalisis("Penerimaan — Jasa/Bunga Pinjaman", a.getRencanaJasaPinjaman(), realJasa));
		rows.add(barisAnalisis("Penerimaan — Lain-lain", a.getRencanaPenerimaanLain(), realPnerLain));
		rows.add(barisAnalisis("TOTAL PENERIMAAN", totRencanaP, totRealP));
		rows.add(barisAnalisis("Pengeluaran — Penyaluran Pinjaman", a.getRencanaPenyaluran(), realPenyaluran));
		rows.add(barisAnalisis("Pengeluaran — Biaya Operasional", a.getRencanaBiayaOperasional(), realBiayaOp));
		rows.add(barisAnalisis("Pengeluaran — Lain-lain", a.getRencanaPengeluaranLain(), realPengLain));
		rows.add(barisAnalisis("TOTAL PENGELUARAN", totRencanaK, totRealK));
		rows.add(barisAnalisis("SURPLUS / (DEFISIT)", totRencanaP - totRencanaK, totRealP - totRealK));
		rows.add(barisAnalisis("Saldo Kas Awal", a.getSaldoAwalKas(), a.getSaldoAwalKas()));
		rows.add(barisAnalisis("PERKIRAAN SALDO KAS AKHIR", saldoAkhirRencana, saldoAkhirReal));

		SimpanPinjamUiUtil.appendRekapGrid(host, "Rencana vs Realisasi Anggaran Kas " + th,
				"Selisih = realisasi − rencana. Capaian = realisasi ÷ rencana.", "Anggaran Kas " + th,
				"anggaran_kas_" + th,
				new String[] { "Pos", "Rencana", "Realisasi", "Selisih", "Capaian" },
				new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH,
						SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.TEKS },
				rows);

		analisisWindow.setVisible(true);
		analisisWindow.onModal();
	}

	/** Susun satu baris analisis: pos, rencana, realisasi, selisih, dan persentase capaian. */
	private Object[] barisAnalisis(String pos, double rencana, double realisasi) {
		String capaian = rencana == 0 ? "-" : (Math.round(realisasi / rencana * 100.0) + "%");
		return new Object[] { pos, Double.valueOf(rencana), Double.valueOf(realisasi),
				Double.valueOf(realisasi - rencana), capaian };
	}

	/**
	 * Hitung realisasi kas satu tahun dari transaksi nyata. Indeks hasil: [0] setoran simpanan,
	 * [1] angsuran pokok terbayar, [2] jasa/bunga terbayar, [3] penerimaan lain (0), [4] penyaluran
	 * pinjaman, [5] biaya operasional (0), [6] pengeluaran lain (0).
	 */
	@SuppressWarnings("unchecked")
	private double[] hitungRealisasi(int th) {
		double[] r = new double[7];
		try {
			Session session = HibernateUtil.currentSession();
			Long tipeSimpanan = ConstantValues.SIMPANAN != null ? ConstantValues.SIMPANAN.getId() : null;
			Long tipePinjaman = ConstantValues.PINJAMAN != null ? ConstantValues.PINJAMAN.getId() : null;

			if (tipeSimpanan != null) {
				List<TransaksiKoperasi> simp = session.createQuery(
						"select t from TransaksiKoperasi t where t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipeSimpanan).list();
				for (TransaksiKoperasi t : simp) {
					try {
						if (tahunDari(t.getTanggalTransaksi() != null ? t.getTanggalTransaksi() : t.getTanggal()) == th) {
							r[0] += t.getNilai() == null ? 0.0 : t.getNilai();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/AnggaranKasKoperasiAction.java:522");
					}
				}
			}

			if (tipePinjaman != null) {
				List<TransaksiKoperasi> pinj = session.createQuery(
						"select t from TransaksiKoperasi t where t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasi t : pinj) {
					try {
						if (tahunDari(t.getTanggalTransaksi() != null ? t.getTanggalTransaksi() : t.getTanggal()) == th) {
							r[4] += t.getNilai() == null ? 0.0 : t.getNilai();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/AnggaranKasKoperasiAction.java:536");
					}
				}

				List<TransaksiKoperasiDetail> bayar = session.createQuery(
						"select d from TransaksiKoperasiDetail d where d.pembayaranAnggotaKoperasiDetail is not null "
								+ "and d.transaksiKoperasi.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasiDetail d : bayar) {
					try {
						if (tahunDari(d.getTanggal()) == th) {
							r[1] += d.getPokok();
							r[2] += d.getMargin();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/AnggaranKasKoperasiAction.java:550");
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return r;
	}

	/** Tahun kalender dari sebuah tanggal, atau -1 bila tanggal null. */
	private int tahunDari(java.util.Date d) {
		if (d == null) {
			return -1;
		}
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.YEAR);
	}
}
