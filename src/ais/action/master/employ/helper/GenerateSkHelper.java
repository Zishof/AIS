package ais.action.master.employ.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.KonfigurasiSK;

/**
 * Helper UI ZK modul kepegawaian untuk menampilkan daftar konfigurasi field SK (Surat
 * Keputusan, mis. SK kenaikan pangkat) dan memicu pembuatan (generate) laporan PDF SK
 * berdasarkan konfigurasi tersebut. Digunakan sebagai panel dalam dialog/window pemicu generate
 * SK, dengan grid daftar {@link KonfigurasiSK} (jenis field, isi, jenis kegiatan employ) dan
 * tombol "Generate".
 *
 * <p>
 * Beberapa bagian kode (checkbox seleksi baris, filter kombo jenis field) tampak belum
 * diselesaikan/dinonaktifkan (dikomentari) dan {@link #onGenerate(Long)} hanya menggunakan
 * parameter {@code id} (id kenaikan pangkat) tanpa memanfaatkan seleksi baris grid — perilaku
 * saat ini dipertahankan apa adanya sesuai batasan tugas dokumentasi (tidak mengubah kode).
 * </p>
 */
public class GenerateSkHelper {

	List myList = new ArrayList();
	private MyGrid grid = new MyGrid();
	private Center center = new Center();
	North north = new North();
	South south = new South();

	Toolbar toolbar = new Toolbar();
	MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
	MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");

	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
	private Combobox jenisField;

	private String idpeg;

	/** Membangun helper dan menyiapkan combobox jenis field (diisi dari {@link KonfigurasiSK}), meski combobox ini belum dipasang ke toolbar (lihat catatan kelas). */
	public GenerateSkHelper() {

		Common.insertCombo(jenisField = new Combobox(), "jenisField",
				KonfigurasiSK.class);

	}

	/** Perenderan satu baris tabel konfigurasi SK: jenis field, isi, nama jenis kegiatan employ, dan checkbox seleksi baris. */
	class RiwayatPendidikanPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final KonfigurasiSK konfigurasiSk = (KonfigurasiSK) arg1;
			// menimbang
			new Label(konfigurasiSk.getJenisField() == null ? ""
					: konfigurasiSk.getJenisField()).setParent(row);
			new Label(konfigurasiSk.getIsi() == null ? ""
					: konfigurasiSk.getIsi()).setParent(row);
			new Label(konfigurasiSk.getJenisKegiatanEmploy() == null ? ""
					: konfigurasiSk.getJenisKegiatanEmploy().getNama())
					.setParent(row);

			Hbox toolbar = new Hbox();

			final MyCheckboxConfig check = new MyCheckboxConfig();
			// check.setTooltiptext("Hapus Data");

			check.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if(check.isChecked() == true){
//						myList.add(row.get,konfigurasiSk.getIsi());
						System.out.println(row.getId());
					}else{
						
					}
				}
			});
			check.setParent(toolbar);
			toolbar.setParent(row);
		}
	}

	/**
	 * Membangun panel (border layout) berisi grid daftar konfigurasi SK dan toolbar
	 * Batal/Generate, lalu memuat data konfigurasi awal.
	 *
	 * @param window jendela ZK induk yang ditutup oleh tombol Batal
	 * @param id     id kenaikan pangkat yang diteruskan ke {@link #onGenerate(Long)} saat tombol Generate ditekan
	 * @return border layout siap disisipkan sebagai konten {@code window}
	 */
	public Borderlayout display(final MyWindow window, final Long id ) {
		// this.konfigurasiSk = konfigurasiSk;

		borderlayout.setWidth("100%");
		Common.clear(center);
		Common.clear(north);
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		// jenisField.setParent(toolbar);
		// jenisField.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// onSearchDefault();
		// }
		// });

		MyGrid searchgrid = new MyGrid();searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Field");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Isi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Kegiatas Employ");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		borderlayout.setParent(window);

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);
		
		button = new MyToolbarbuttonConfig("Generate", "/img/save.gif");
		button.setTooltiptext("Generate");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onGenerate(id);
			}
		});
		button.setParent(toolbar);

		onSearchDefault();

		return borderlayout;
	}

	/** Membuat laporan PDF SK kenaikan pangkat ({@code report_kenaikan_pangkat}) untuk {@code id} kenaikan pangkat yang diberikan (0 bila {@code null}). */
	@SuppressWarnings( { "unchecked" })
	public void onGenerate(Long id) throws Exception {
//		Session session = HibernateUtil.currentSession();
//		BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
//				.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//				.add(
//						Restrictions
//								.idEq(BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa
//										.getId())).uniqueResult();
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("idkenaikan", id == null ? 0
				: id);
		Report.generatePDFReport(Report.PDF, parameters,
				"report_kenaikan_pangkat", ais.ui.util.WaktuUtil.getDate());
		
//		Report.generateDownloadReport(Report.PDF, parameters,
//				"KartuBayarSpmbMandiri", null, ais.ui.util.WaktuUtil.getDate(),
//					);

	}

	/** Memuat seluruh {@link KonfigurasiSK} (terurut jenis field) ke grid tanpa filter pencarian. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault() {
		Session session = HibernateUtil.currentSession();
		System.out.println("masuk osd");
		List<KonfigurasiSK> konfigurasiSks = session
				.createCriteria(KonfigurasiSK.class)
				// .add(jenisField.getSelectedItem() == null ? Restrictions
				// .sqlRestriction("1=1") : Restrictions.ilike(
				// "jenisField", jenisField.getSelectedItem().getLabel(),
				// MatchMode.ANYWHERE))

				.addOrder(Order.asc("jenisField"))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(konfigurasiSks);
		grid.setRowRenderer(new RiwayatPendidikanPegawaiRenderer());
		grid.setModelCheckMobile(strset);
		
	}
}