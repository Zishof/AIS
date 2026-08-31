package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
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
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BerkasHasilAkreditasi;
import ais.database.model.BerkasHasilAkreditasiPunyaNama;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk mengelola daftar referensi/dokumen pustaka ({@link BerkasHasilAkreditasiPunyaNama})
 * yang menjadi bukti pendukung satu {@link BerkasHasilAkreditasi}. Setiap baris memuat metadata
 * bibliografi (nama, penulis/editor, kata kunci, penerbit, tanggal terbit, abstrak, keterangan) serta
 * unggahan lampiran file lewat {@link ais.database.model.file.LampiranLain}, dapat dicari berdasarkan
 * nama, diedit lewat dialog form ({@link #init}), dihapus (hanya untuk pengguna dengan hak
 * {@link CommonPrivilages#DELETE}), dan diekspor ke Excel dengan kolom hyperlink lampiran.
 */
public class BerkasHasilAkreditasiPunyaNamaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private BerkasHasilAkreditasi berkasHasilAkreditasi;

	private Textbox nama;

	private Paging paging;

	/** Menyiapkan komponen paging beserta listener yang memuat ulang grid saat halaman berpindah. */
	public BerkasHasilAkreditasiPunyaNamaHelper() {

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/** Row renderer grid: nama+lampiran, penulis/editor, kata kunci, penerbit+tanggal, keterangan (editable), dan tombol ubah/hapus. */
	class DetailBerkasHasilAkreditasiPunyaNamaRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		public DetailBerkasHasilAkreditasiPunyaNamaRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final BerkasHasilAkreditasiPunyaNama berkasHasilAkreditasiPunyaNama = (BerkasHasilAkreditasiPunyaNama) data;

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			Hbox hbox = new Hbox();
			RevisiHelper.createNewRevisi(BerkasHasilAkreditasiPunyaNama.class, berkasHasilAkreditasiPunyaNama,
					berkasHasilAkreditasiPunyaNama.getNama()).setParent(vbox);
			LampiranLain.createDownloadUploadFileLain(hbox, berkasHasilAkreditasiPunyaNama.getId(),
					BerkasHasilAkreditasiPunyaNama.class.getName(), "Lampiran", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
			hbox.setParent(vbox);

			new Label(berkasHasilAkreditasiPunyaNama.getPenulis() + " / " + berkasHasilAkreditasiPunyaNama.getEditor())
					.setParent(row);
			new Label(berkasHasilAkreditasiPunyaNama.getKeyword()).setParent(row);
			new Label(
					berkasHasilAkreditasiPunyaNama.getDiterbitkanoleh() + " / "
							+ (berkasHasilAkreditasiPunyaNama.getTanggal() == null ? ""
									: Common.dateFormat1.get().format(berkasHasilAkreditasiPunyaNama.getTanggal())))
					.setParent(row);

			final MyTextbox keterangan = new MyTextbox(berkasHasilAkreditasiPunyaNama.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					berkasHasilAkreditasiPunyaNama.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(berkasHasilAkreditasiPunyaNama);
				}
			};

			keterangan.addEventListener("onChange", eventListener);

			keterangan.setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(berkasHasilAkreditasiPunyaNama);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
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

											Common.refreshDelete(berkasHasilAkreditasiPunyaNama);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);

		}

	}

	/**
	 * Membangun kriteria Hibernate {@link BerkasHasilAkreditasiPunyaNama} milik
	 * {@code berkasHasilAkreditasi} saat ini, difilter pencarian nama.
	 *
	 * @param order bila {@code true}, menambahkan pengurutan nama menaik
	 * @return kriteria siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BerkasHasilAkreditasiPunyaNama.class)

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("berkasHasilAkreditasi", berkasHasilAkreditasi));

		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	/** Memuat ulang halaman referensi saat ini dan me-render ulang grid. Parameter {@code value} tidak dipakai. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<BerkasHasilAkreditasiPunyaNama> myBerkasHasilAkreditasiPunyaNamas = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myBerkasHasilAkreditasiPunyaNamas);
				grid.setRowRenderer(new DetailBerkasHasilAkreditasiPunyaNamaRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private Textbox namaBerkas;
	private Textbox kode;
	private Textbox penulis;
	private Textbox editor;
	private Textbox abstrak;
	private Textbox keyword;
	private Textbox diterbitkanoleh;
	private MyDatebox tanggal;

	private MyWindow addWindow;
	private Textbox keterangan;
	private BerkasHasilAkreditasiPunyaNama berkasHasilAkreditasiPunyaNama;

	/** Membuka dialog form untuk menambah referensi baru pada {@code berkasHasilAkreditasi} saat ini. */
	public void onAdd(Event event) throws Exception {

		init(new BerkasHasilAkreditasiPunyaNama());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final BerkasHasilAkreditasiPunyaNama berkasHasilAkreditasiPunyaNama) {
		this.berkasHasilAkreditasiPunyaNama = berkasHasilAkreditasiPunyaNama;
		addWindow = new MyWindow("", "none", false);
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		addWindow.setHeight("95%");
		addWindow.setWidth("600px");
		addWindow.setTitle(berkasHasilAkreditasiPunyaNama.getId() == null ? "Tambah Berkas Akreditasi" : "Ubah Berkas Akreditasi");
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

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Berkas"));
		row.appendChild(kode = new Textbox(berkasHasilAkreditasiPunyaNama.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Berkas"));
		row.appendChild(namaBerkas = new Textbox(berkasHasilAkreditasiPunyaNama.getNama()));
		namaBerkas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penulis Berkas"));
		row.appendChild(penulis = new Textbox(berkasHasilAkreditasiPunyaNama.getPenulis()));
		penulis.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Editor Berkas"));
		row.appendChild(editor = new Textbox(berkasHasilAkreditasiPunyaNama.getEditor()));
		editor.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diterbitkan oleh"));
		row.appendChild(diterbitkanoleh = new Textbox(berkasHasilAkreditasiPunyaNama.getDiterbitkanoleh()));
		diterbitkanoleh.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diterbitkan tanggal"));
		row.appendChild(tanggal = new MyDatebox(berkasHasilAkreditasiPunyaNama.getTanggal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Abstrak"));
		row.appendChild(abstrak = new Textbox(berkasHasilAkreditasiPunyaNama.getAbstrak()));
		abstrak.setWidth("90%");
		abstrak.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kata Kunci"));
		row.appendChild(keyword = new Textbox(berkasHasilAkreditasiPunyaNama.getKeyword()));
		keyword.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(berkasHasilAkreditasiPunyaNama.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (berkasHasilAkreditasiPunyaNama.getId() == null) {
					BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama = new BerkasHasilAkreditasiPunyaNama();
				}

				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama
						.setBerkasHasilAkreditasi(berkasHasilAkreditasi);
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama.setNama(namaBerkas.getValue());
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama
						.setKeterangan(keterangan.getValue());
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama.setKode(kode.getValue());
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama.setPenulis(penulis.getValue());
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama.setEditor(editor.getValue());
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama.setAbstrak(abstrak.getValue());
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama.setKeyword(keyword.getValue());
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama
						.setDiterbitkanoleh(diterbitkanoleh.getValue());
				BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama.setTanggal(tanggal.getValue());

				Common.refreshUpdate(BerkasHasilAkreditasiPunyaNamaHelper.this.berkasHasilAkreditasiPunyaNama);

				loadData(null);
				addWindow.detach();
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * Membangun UI grid referensi (toolbar cari/tambah/unduh, kolom grid) di dalam {@code component}
	 * untuk {@code berkasHasilAkreditasi} yang diberikan dan memuat data awal.
	 *
	 * @param berkasHasilAkreditasi berkas hasil akreditasi induk
	 * @param component             container ZK yang akan diisi
	 * @param window                tidak dipakai langsung di badan method
	 */
	public void display(final BerkasHasilAkreditasi berkasHasilAkreditasi, final Component component,
			final MyWindow window) {
		this.berkasHasilAkreditasi = berkasHasilAkreditasi;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Lampiran Baru", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onAdd(null);
			}

		});
		button.setParent(toolbar);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Lampiran");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				BerkasHasilAkreditasiPunyaNama berkasHasilAkreditasiPunyaNama = (BerkasHasilAkreditasiPunyaNama) objects[0];

				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				class DataAddingHelper {
					public void process(XSSFRow row, int index,
							BerkasHasilAkreditasiPunyaNama berkasHasilAkreditasiPunyaNama, String jenis)
							throws Exception {

						LampiranLain lam = LampiranLain.ambil(berkasHasilAkreditasiPunyaNama.getId(), jenis);

						XSSFCell cell = row.createCell(index);

						if (lam != null) {

							String nama = lam.getNama();

							cell.setCellStyle(hlink_style);
							cell.setCellValue(nama);
							String url = lam.createLinkUri();
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
									.createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cell.setHyperlink(link);
						}

					}
				}

				DataAddingHelper dataAddingHelper = new DataAddingHelper();

				dataAddingHelper.process(row, 5, berkasHasilAkreditasiPunyaNama,
						BerkasHasilAkreditasiPunyaNama.class.getName());

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(BerkasHasilAkreditasiPunyaNama.class,
				this, "Download", "/img/print.png", columnHeadersAdding, dataAdding, "id", "berkasHasilAkreditasi",
				"nama", "keterangan");

		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penulis/Editor");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kata Kunci");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Terbit");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		loadData(null);

	}

}
