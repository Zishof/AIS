package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisKegiatanDetail;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk memilih sekumpulan {@link DetailBiaya} (rincian item biaya per Fakultas/Jurusan/
 * Tahun Akademik) yang akan dilekatkan ke satu {@link JenisKegiatanDetail} (baris detail dalam satu
 * {@link JenisKegiatan}, mis. paket kegiatan pembayaran). Jendela pencarian menyaring
 * {@link DetailBiaya} yang bukan merupakan pembayaran itu sendiri ({@code merupakanPembayaran=false})
 * berdasarkan Jurusan; baris yang sudah terpasang pada {@code jenisKegiatanDetail} sebelum jendela
 * dibuka tampil tercentang.
 *
 * <p>
 * {@link #save()} mengganti (bukan menambah) seluruh set {@code detailBiayas} milik
 * {@link #jenisKegiatanDetail} dengan baris-baris yang tercentang pada grid saat tombol Simpan
 * ditekan — checkbox yang tidak tercentang berarti item tersebut dilepas dari relasi.
 * </p>
 */
public class AmbilDetailBiayaHelper {

	/** Grid hasil pencarian {@link DetailBiaya}, dirender ulang oleh {@link #onSearchDefault(Event)}. */
	private MyGrid grid;

	/** Filter tahun akademik pada bagian pencarian (belum dipakai sebagai kriteria query aktif). */
	private Decimalbox tahunAkademik;
	/** Combobox Fakultas hasil {@link Common#initFakultasDanJurusanDanSemua}; belum dipakai sebagai filter query aktif. */
	private Combobox searchfakultas = new Combobox();
	/** Combobox Jurusan hasil {@link Common#initFakultasDanJurusanDanSemua}; filter aktif pada {@link #onSearchDefault(Event)}. */
	private Combobox searchjurusan = new Combobox();

	/** Baris {@link JenisKegiatanDetail} target yang set {@code detailBiayas}-nya sedang dipilih ulang. */
	private JenisKegiatanDetail jenisKegiatanDetail;

	/**
	 * Snapshot {@link DetailBiaya} yang sudah terpasang pada {@link #jenisKegiatanDetail} saat
	 * konstruktor dipanggil — dipakai {@link DetailBiayaRenderer} untuk menentukan status centang
	 * awal tiap baris grid. Tidak diperbarui lagi setelah konstruksi (lihat {@link #save()} untuk
	 * sumber kebenaran final saat disimpan).
	 */
	private DetailBiaya[] detailBiayas;

	/** @param jenisKegiatanDetail baris detail kegiatan yang set {@code detailBiayas}-nya akan dipilih ulang. */
	public AmbilDetailBiayaHelper(JenisKegiatanDetail jenisKegiatanDetail) {

		Session session = HibernateUtil.currentSession();
		this.jenisKegiatanDetail = (JenisKegiatanDetail) session.load(JenisKegiatanDetail.class,
				jenisKegiatanDetail.getId());
		detailBiayas = this.jenisKegiatanDetail.getDetailBiayas().toArray(new DetailBiaya[] {});

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDetailBiayaHelper}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDetailBiayaHelper} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDetailBiayaHelper
	 */
	class DetailBiayaRenderer extends ais.ui.util.MyRowRenderer {

		// private DetailJenisKegiatanDao detailJenisKegiatanDao = DaoFactory
		// .getInstance().getDetailJenisKegiatanDao();

		// private Session session = detailJenisKegiatanDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DetailBiaya detailBiaya = (DetailBiaya) arg1;
			MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("myValue", detailBiaya);
			// checkbox.setId(detailBiaya.getId() + "");

			// Integer jml = ((Number) session.createCriteria(
			// JenisKegiatanDetail.class).setProjection(
			// Projections.rowCount()).add(
			// Restrictions.eq("jurusan",
			// detailBiaya.getJurusan() == null ? "" : detailBiaya
			// .getJurusan())).uniqueResult()).intValue();

			for (DetailBiaya myBiaya : detailBiayas) {
				if (myBiaya.getId().equals(detailBiaya.getId())) {
					checkbox.setChecked(true);
					break;
				}
			}

			// checkbox.setChecked(jenisKegiatanDetail.getDetailBiayas().contains(
			// detailBiaya));

			new Label(detailBiaya.getItemBiaya().getNama()).setParent(arg0);
			new Label(detailBiaya.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);
			new Label(detailBiaya.getFakultas().getNama()).setParent(arg0);
			new Label(detailBiaya.getJurusan().getNama()).setParent(arg0);
			new Label(detailBiaya.getTahunAkademik() + "").setParent(arg0);
			new Label((detailBiaya.getNilaiBiaya() == null ? new Double(0.0) : detailBiaya.getNilaiBiaya()) + "")
					.setParent(arg0);

		}

	}

	/**
	 * Mengganti set {@code detailBiayas} milik {@link #jenisKegiatanDetail} dengan seluruh baris
	 * yang tercentang pada grid saat ini (baris yang tidak tercentang berarti dilepas dari relasi).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {
		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		Session session = HibernateUtil.currentSession();
		this.jenisKegiatanDetail = (JenisKegiatanDetail) HibernateUtil.currentSession().load(JenisKegiatanDetail.class,
				jenisKegiatanDetail.getId());
		Set<DetailBiaya> detailBiayas = new HashSet<DetailBiaya>();

		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				DetailBiaya detailBiaya = (DetailBiaya) checkbox.getAttribute("myValue");
				if (checkbox.isChecked()) {
					detailBiayas.add(detailBiaya);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDetailBiayaHelper.java:132");
				// TODO: handle exception
			}
		}
		jenisKegiatanDetail.setDetailBiayas(detailBiayas);
		Common.refreshUpdate(session, (jenisKegiatanDetail));

	}

	/**
	 * Membangun jendela pencarian dan pemilihan {@link DetailBiaya} untuk {@link #jenisKegiatanDetail}.
	 * Tombol Simpan memanggil {@link #save()} lalu menyegarkan tampilan pemanggil lewat {@code dataLoader}.
	 *
	 * @param jenisKegiatan tidak dipakai langsung oleh method ini (disimpan untuk konteks pemanggil)
	 * @param dataLoader    dipanggil setelah simpan untuk menyegarkan tampilan pemanggil
	 * @param window        jendela ({@link MyWindow}) yang dipakai ulang untuk menampilkan layar ini
	 */
	public void display(final JenisKegiatan jenisKegiatan, final DataLoader dataLoader, final MyWindow window) {
		Common.clear(window);
		window.setTitle("Ambil Data Detail Biaya");
		window.setWidth("750px");
		window.setHeight("540px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Detail Biaya");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("320px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Decimalbox());
		tahunAkademik.setWidth("90%");

		/*
		 * row = new MyFormRow(); row.setSclass("ais-form-row"); row.setParent(rows);
		 * row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		 * row.appendChild(nama = new Textbox()); nama.setWidth("90%");
		 * 
		 * row = new MyFormRow(); row.setSclass("ais-form-row"); row.setParent(rows);
		 * row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		 * row.appendChild(tahunangkatan = new Decimalbox());
		 * tahunangkatan.setWidth("90%");
		 */

		/*
		 * row = new MyFormRow(); row.setSclass("ais-form-row"); row.setParent(rows);
		 * row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		 * Common.insertCombo(fakultas=new Combobox(), new String[]{"nama", "kode"},
		 * Fakultas.class, Restrictions.eq("aktif", true)); row.appendChild(fakultas);
		 * fakultas.setWidth("90%");
		 * 
		 * row = new MyFormRow(); row.setSclass("ais-form-row"); row.setParent(rows);
		 * row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		 * //row.appendChild(jurusan=new Combobox()); Common.insertCombo(jurusan=new
		 * Combobox(), "nama", Jurusan.class,
		 * Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif",
		 * true))); row.appendChild(jurusan); jurusan.setWidth("90%");
		 */

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(div);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getChildren().get(0);
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDetailBiayaHelper.java:277");

					}
				}
			}
		});
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Biaya");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun angkatan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai biaya");
		column.setWidth("25%");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mengisi ulang grid hasil pencarian {@link DetailBiaya} (bukan pembayaran) sesuai filter Jurusan
	 * saat ini. Hasil dibatasi {@code Common#MAX_RESULT} baris.
	 *
	 * @param event tidak dipakai isinya
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<DetailBiaya> detailBiaya = session.createCriteria(DetailBiaya.class)
				.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false),
						Restrictions.isNull("merupakanPembayaran")))
				.addOrder(Order.desc("id")).addOrder(Order.asc("id"))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(detailBiaya);
		grid.setRowRenderer(new DetailBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
