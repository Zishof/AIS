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

	public CheckMahasiswaPanel(Kegiatan kegiatan) throws Exception {
		this.kegiatan = kegiatan;
		init();
	}

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

	class DetailBiayaMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

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
