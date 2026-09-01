package ais.action.master.helper;

import java.text.ParseException;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;

/**
 * Panel ZK ringkasan biaya satu {@link Kegiatan} (satu transaksi/kejadian pembayaran) milik
 * seorang {@link Mahasiswa}, dipakai saat staf keuangan memvalidasi/mengecek sebuah pembayaran
 * (mis. dari layar validasi kasir) sebelum diposting. Menampilkan biodata ringkas mahasiswa
 * (NIM, nama, kewarganegaraan dari {@link BiodataMahasiswa} terbaru — fallback ke {@code WNI}
 * bila belum ada biodata —, program, prodi, semester, angkatan, tahun akademik, dan tanggal
 * pembayaran Kegiatan), lalu daftar {@link DetailKegiatan} aktif ({@code aktif} null atau
 * {@code true}) milik Kegiatan tersebut dalam grid ber-footer tiga baris: label "Jumlah Biaya",
 * total nilai tagihan seharusnya (dijumlah ulang dari kolom "Nilai" lewat parsing string
 * {@link Common#numberFormat}), dan total nominal yang benar-benar harus dibayar (dijumlah dari
 * kolom {@link MyDoublebox} read-only "Yang harus dibayar").
 *
 * <p><b>Kuirk non-obvious:</b> {@link #hitungJumlahBiaya()} dan {@link #hitungJumlahBiayaSeharusnya()}
 * tidak membaca nilai numerik {@link DetailKegiatan} secara langsung dari model, melainkan
 * mem-parse ULANG teks yang sudah dirender ke {@link Label}/{@link MyDoublebox} di grid
 * ({@code rows.getChildren()} lalu indeks kolom tetap 2 dan 3) — pola "hitung dari tampilan"
 * yang rawan pecah bila urutan kolom grid diubah. Constructor memuat data begitu instance
 * dibuat ({@code init()} dipanggil langsung dari constructor), jadi objek ini tidak boleh
 * dibuat sebelum {@link Kegiatan} sumber datanya siap.</p>
 *
 * @see Panel
 */
public class CheckMahasiswaPanel extends Panel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4681108185695239730L;

	private Label kewarganegaraan = new Label();
	private Label jenisKuliah = new Label();
	private Label prodi = new Label();
	private Label semester = new Label();
	private Label labelNimMahasiswa = new Label();
	private Label labelNamaMahasiswa = new Label();
	private Label labelTahunMasuk = new Label();
	private Label labelTahunAkademik = new Label();
	private MyGrid gridss;

	Label labelFooter1;
	Label labelFooter2;
	Label labelFooter3;

	private Kegiatan kegiatan;

	private Label tanggalValidasi = new Label();

	/**
	 * Simpan {@link Kegiatan} sumber data dan langsung bangun seluruh panel ({@link #init()})
	 * — biodata ringkas mahasiswa serta grid detail biaya sudah terisi begitu constructor
	 * selesai.
	 *
	 * @param kegiatan Kegiatan (transaksi/kejadian pembayaran) yang ingin dicek/divalidasi.
	 */
	public CheckMahasiswaPanel(Kegiatan kegiatan) throws Exception {
		this.kegiatan = kegiatan;
		init();
	}

	/**
	 * Bangun layout panel (Borderlayout: Center berisi grid biodata, South berisi grid detail
	 * biaya via {@link #listBiaya}). Membaca {@link Mahasiswa} dari {@link #kegiatan} dan
	 * {@link BiodataMahasiswa} terbaru miliknya (query {@code addOrder(Order.desc("id"))
	 * setMaxResults(1)}) untuk menampilkan kewarganegaraan; bila belum ada biodata, kewarganegaraan
	 * ditampilkan sebagai {@link Mahasiswa#WNI}. Dipanggil dari constructor, jadi seluruh komponen
	 * ZK panel ini baru ada setelah method ini selesai.
	 */
	public void init() throws Exception {
		setHeight("300px");
		setWidth("100%");
		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(this);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Mahasiswa mahasiswa = kegiatan.getMahasiswa();

		BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) HibernateUtil.currentSession()
				.createCriteria(BiodataMahasiswa.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		labelNimMahasiswa.setValue(mahasiswa.getNim());
		row.appendChild(labelNimMahasiswa);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		labelNamaMahasiswa.setValue(mahasiswa.getNama());
		row.appendChild(labelNamaMahasiswa);

		if (biodataMahasiswa != null)
			kewarganegaraan.setValue(biodataMahasiswa.getKewarganegaraan());
		else
			kewarganegaraan.setValue(ais.database.model.Mahasiswa.WNI);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
		row.appendChild(kewarganegaraan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		jenisKuliah.setValue(mahasiswa.getProgram());
		row.appendChild(jenisKuliah);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		prodi.setValue(mahasiswa.getJurusan().getNama());
		row.appendChild(prodi);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		semester.setValue(kegiatan.getSemster() + "");
		row.appendChild(semester);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		labelTahunMasuk.setValue(mahasiswa.getTahunangkatan().toString());
		row.appendChild(labelTahunMasuk);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		labelTahunAkademik.setValue(kegiatan.getTahunAkademik());
		row.appendChild(labelTahunAkademik);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembayaran"));
		tanggalValidasi.setValue(Common.dateFormat3.get().format(kegiatan.getTanggal()));
		row.appendChild(tanggalValidasi);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);
		listBiaya(south, mahasiswa, kegiatan);

	}

	/**
	 * Bangun grid detail biaya (kolom Status/Item/Nilai/Yang harus dibayar) di dalam {@code comp},
	 * diisi dari {@link DetailKegiatan} aktif milik {@code kegiatan} (query Hibernate:
	 * {@code aktif} null atau {@code true}, dibatasi {@link Common#MAX_RESULT}), dirender lewat
	 * {@link DetailBiayaMahasiswaRenderer}. Menyiapkan tiga baris footer grid (label, kosong,
	 * total seharusnya, total harus dibayar) lalu memicu penjumlahan awal via
	 * {@link #hitungJumlahBiayaSeharusnya()} dan {@link #hitungJumlahBiaya()}.
	 *
	 * @param comp      komponen South tempat grid dipasang.
	 * @param mahasiswa mahasiswa pemilik Kegiatan (tidak dipakai langsung untuk query di sini,
	 *                  disediakan untuk kebutuhan renderer/pemanggil lanjutan).
	 * @param kegiatan  Kegiatan sumber daftar DetailKegiatan yang ditampilkan.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void listBiaya(final South comp, final Mahasiswa mahasiswa, final Kegiatan kegiatan) throws Exception {

		gridss = new MyGrid();
		gridss.setMold("paging");
		gridss.setPageSize(1000);
		gridss.setParent(comp);
		gridss.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(gridss);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Yang harus dibayar");
		column.setWidth("50%");

		Double sumBiaya = 0.0;

		// --- create footer 1
		Foot foot = new Foot();
		foot.setParent(gridss);

		Footer footer = new Footer();
		footer.setParent(foot);
		labelFooter1 = new Label();
		labelFooter1.setParent(footer);
		labelFooter1.setValue("Jumlah Biaya:");

		footer = new Footer();
		footer.setParent(foot);
		Label label = new Label();
		label.setParent(footer);
		label.setValue("");

		footer = new Footer();
		footer.setParent(foot);
		labelFooter2 = new Label();
		labelFooter2.setParent(footer);
		// labelFooter2.setValue(sumBiayaString);

		footer = new Footer();
		footer.setParent(foot);
		labelFooter3 = new Label();
		labelFooter3.setParent(footer);
		labelFooter3.setValue(sumBiaya.toString());

		Session session = HibernateUtil.currentSession();

		List<DetailKegiatan> detailBiayaListPerSemester = session.createCriteria(DetailKegiatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("kegiatan", kegiatan)).setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = null;
		strset = new SimpleListModel(detailBiayaListPerSemester);
		gridss.setRowRenderer(new DetailBiayaMahasiswaRenderer());
		gridss.setModelCheckMobile(strset);
		ais.ui.util.ZkCompat.setFixedLayout(gridss, true);
		gridss.renderAll();
		gridss.setOddRowSclass("non-odd");

		hitungJumlahBiayaSeharusnya();
		hitungJumlahBiaya();

	}

	/**
	 * Renderer satu baris grid detail biaya: menampilkan nama {@code ItemBiaya} (dari
	 * {@link DetailKegiatan#getDetailBiaya()}), nilai biaya terformat, dan nominal "Yang harus
	 * dibayar" dalam {@link MyDoublebox} read-only. Checkbox "cekBiayaOlehKeuangan" hanya
	 * menandai visual apakah baris ini punya {@link DetailKegiatan} (selalu true di sini karena
	 * data sumbernya memang list DetailKegiatan) — checkbox ini dinonaktifkan (disabled), murni
	 * indikator, tidak mengubah status apa pun.
	 *
	 * @see CheckMahasiswaPanel
	 */
	class DetailBiayaMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Render satu baris: checkbox indikator (kolom 0), label nama item biaya (kolom 1),
		 * label nilai biaya terformat (kolom 2), dan doublebox read-only nominal yang harus
		 * dibayar (kolom 3) — indeks kolom ini dipakai ulang oleh
		 * {@link CheckMahasiswaPanel#hitungJumlahBiaya()} dan
		 * {@link CheckMahasiswaPanel#hitungJumlahBiayaSeharusnya()} untuk menjumlahkan ulang
		 * dari komponen yang sudah dirender di sini.
		 *
		 * @param arg0 baris grid tujuan render.
		 * @param arg1 data baris, di-cast ke {@link DetailKegiatan}.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DetailKegiatan detailKegiatan = (DetailKegiatan) arg1;

			Double nilaiBiaya = 0.0;

			final MyDoublebox harusDiBayar = new MyDoublebox(detailKegiatan == null ? 0.0 : detailKegiatan.getBiaya());
			harusDiBayar.setWidth("90%");
			// harusDiBayar.setReadonly(detailKegiatan == null ? true : false);

			final MyCheckboxConfig cekBiayaOlehKeuangan = new MyCheckboxConfig();
			cekBiayaOlehKeuangan.setChecked(detailKegiatan != null);
			harusDiBayar.setStyle("text-align: right;");
			harusDiBayar.setFormat("#,##0.##");
			harusDiBayar.setReadonly(true);

			cekBiayaOlehKeuangan.setDisabled(true);
			cekBiayaOlehKeuangan.setParent(arg0);

			nilaiBiaya = detailKegiatan.getBiaya();
			String frmNilaiBiaya = Common.numberFormat.get().format(nilaiBiaya);

			new Label(detailKegiatan.getDetailBiaya().getItemBiaya() == null ? ""
					: detailKegiatan.getDetailBiaya().getItemBiaya().getNama()).setParent(arg0);
			new Label(frmNilaiBiaya).setParent(arg0);
			harusDiBayar.setParent(arg0);

		}

	}

	/**
	 * Jumlahkan ulang nominal "Yang harus dibayar" (kolom {@link MyDoublebox} indeks 3 tiap
	 * baris grid {@link #gridss}) dan tampilkan hasilnya di {@link #labelFooter3}. Nilai
	 * {@code null} pada doublebox dihitung sebagai 0.0.
	 */
	public void hitungJumlahBiaya() {
		Rows rows = (Rows) gridss.getRows();
		Double nilaiBiayaHarusDiBayars = 0.0;
		if (rows != null && rows.getChildren() != null) {
			for (int i = 0; i < rows.getChildren().size(); i++) {
				Row myRow = (Row) rows.getChildren().get(i);
				MyDoublebox myMyDoublebox = (MyDoublebox) myRow.getChildren().get(3);
				nilaiBiayaHarusDiBayars += (myMyDoublebox.getValue() == null ? 0.0 : myMyDoublebox.getValue());
			}
			labelFooter3.setStyle("text-align: right;");
			labelFooter3.setValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars));
		}
	}

	/**
	 * Jumlahkan ulang nilai biaya "seharusnya" dengan mem-parse teks kolom "Nilai" (indeks 2,
	 * {@link Label}) tiap baris grid {@link #gridss} lewat {@link Common#numberFormat}, lalu
	 * tampilkan totalnya di {@link #labelFooter2}. Karena sumbernya teks hasil format angka
	 * (bukan nilai numerik model), format lokal {@link Common#numberFormat} harus konsisten
	 * dengan format yang dipakai saat label tersebut dirender di
	 * {@link DetailBiayaMahasiswaRenderer}.
	 *
	 * @throws ParseException bila teks label tidak sesuai format angka yang diharapkan.
	 */
	public void hitungJumlahBiayaSeharusnya() throws ParseException {

		// String sumBiayaString = "";

		Rows rows = (Rows) gridss.getRows();
		Double nilaiBiayaHarusDiBayars = 0.0;
		if (rows != null && rows.getChildren() != null) {
			for (int i = 0; i < rows.getChildren().size(); i++) {
				Row myRow = (Row) rows.getChildren().get(i);
				Label myLabel = (Label) myRow.getChildren().get(2);
				// System.out.println("myLabel = " + myLabel.getValue());
				Double nilaiBiayas = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
				System.out.println("nilaiBiayas = " + nilaiBiayas);
				nilaiBiayaHarusDiBayars += (myLabel.getValue() == null ? 0.0 : nilaiBiayas);
				System.out.println("nilaiBiayaHarusDiBayars = " + nilaiBiayaHarusDiBayars);
			}
			labelFooter2.setStyle("text-align: right;");
			labelFooter2.setValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars));
		}
	}

}
