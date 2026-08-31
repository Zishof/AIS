package ais.action.master.helper.generic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data biodata calon mahasiswa banyak. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * biodataCalonMahasiswas}, {@code List biodataCalonMahasiswasHanyaDitampilkan}, {@code Set ids}, {@code
 * MyTextbox nama}, {@code MyTextbox searchnoreg}; pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataBiodataCalonMahasiswaBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<BiodataCalonMahasiswa> biodataCalonMahasiswas;
	private List<BiodataCalonMahasiswa> biodataCalonMahasiswasHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataBiodataCalonMahasiswaBanyak(List<BiodataCalonMahasiswa> biodataCalonMahasiswas) {
		super();
		this.biodataCalonMahasiswas = biodataCalonMahasiswas;
		display();
		onSearchDefault(null);
	}

	public AmbilDataBiodataCalonMahasiswaBanyak(List<BiodataCalonMahasiswa> biodataCalonMahasiswas,
			List<BiodataCalonMahasiswa> biodataCalonMahasiswasHanyaDitampilkan) {
		super();
		this.biodataCalonMahasiswas = biodataCalonMahasiswas;
		this.biodataCalonMahasiswasHanyaDitampilkan = biodataCalonMahasiswasHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox nama;
	private MyTextbox searchnoreg;
	private MyTextbox searchujian;
	private Combobox searchTahunAjaran;
	private boolean tampilSederhana;

	class BiodataCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;
			arg0.setAttribute("biodataCalonMahasiswa", biodataCalonMahasiswa);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (BiodataCalonMahasiswa myBiodataCalonMahasiswa : biodataCalonMahasiswas) {
				if (myBiodataCalonMahasiswa.getId().equals(biodataCalonMahasiswa.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(biodataCalonMahasiswa.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(biodataCalonMahasiswa.getId());
					} else {
						ids.remove(biodataCalonMahasiswa.getId());
					}
				}
			});

			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) arg1;

			CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(arg0);

			RevisiHelper.createNewRevisi(BiodataCalonMahasiswa.class, calonMahasiswa, calonMahasiswa.getNama())
					.setParent(arg0);

			new Label(calonMahasiswa.getTanggalLahir() == null
					? Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
					: Common.dateFormat2.get().format(calonMahasiswa.getTanggalLahir())).setParent(arg0);
			new Label(calonMahasiswa.getAsalSma() == null ? "" : calonMahasiswa.getAsalSma()).setParent(arg0);

			Vbox vbox = new Vbox();
			if (calonMahasiswa.getJenisSeleksi() != null)
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getJenisSeleksi().toString())));
			if (calonMahasiswa.getNoRegistrasi() != null && !calonMahasiswa.getNoRegistrasi().trim().isEmpty())
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getNoRegistrasi())));
			if (calonMahasiswa.getNoUjian() != null && !calonMahasiswa.getNoUjian().trim().isEmpty())
				vbox.appendChild(new Label("No. Ujian:" + (calonMahasiswa.getNoUjian())));
			if (calonMahasiswa.getNim() != null && !calonMahasiswa.getNim().trim().isEmpty())
				vbox.appendChild(new Label("NIM :" + (calonMahasiswa.getNim())));

			if (calonMahasiswa.getMerupakanPindahan()) {
				vbox.appendChild(new Label("Pindahan dari :" + (calonMahasiswa.getPindahanDariKampus())));
				vbox.appendChild(new Label("Prodi :" + (calonMahasiswa.getPindahanDariProdi())));
				vbox.appendChild(
						new Label("Pindah di semester :" + (calonMahasiswa.getPindahDariKampusLamaDiSemester())));
				vbox.appendChild(new Label("NIM lama :" + (calonMahasiswa.getNimLamaSebelumPindah())));
				vbox.appendChild(new Label("Alasan pindah:" + (calonMahasiswa.getKeteranganPindah())));
			}

			vbox.setParent(arg0);

			new Label(calonMahasiswa.getGelombangPendaftaran() == null ? ""
					: calonMahasiswa.getGelombangPendaftaran().toString()).setParent(arg0);

			vbox = new Vbox();
			if (calonMahasiswa.getProdi1() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi1())));
			}
			if (calonMahasiswa.getProdi2() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi2())));
			}
			if (calonMahasiswa.getProdi3() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi3())));
			}
			if (calonMahasiswa.getProdi4() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi4())));
			}
			if (calonMahasiswa.getProdi5() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi5())));
			}
			if (calonMahasiswa.getProdiLulus() != null) {
				vbox.appendChild(new Label("Lulus di prodi : " + (calonMahasiswa.getProdiLulus())));
			} else {
				vbox.appendChild(new Label("Belum / tidak lulus"));
			}
			vbox.setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Calon Mahasiswa");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		tampilSederhana = Common.bolehKonfigurasi("tampil_formulir_sederhana", Konfigurasi.TIDAK_AKTIF);

		if (!tampilSederhana) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
			row.appendChild(searchTahunAjaran = new Combobox());
			searchTahunAjaran.setWidth("90%");
			searchTahunAjaran.addEventListener(Events.ON_CHANGE, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});
			searchTahunAjaran.setReadonly(true);

			String tahunAkademikPenerimaanMahasiswaBaru = Common
					.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
					.getNilai();
			Common.generateTahunAjaranDanSemua(searchTahunAjaran);
			Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

			Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Reg"));
		row.appendChild(searchnoreg = new MyTextbox());
		searchnoreg.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		if (!tampilSederhana) {
			row.appendChild(new ais.ui.util.MyLabelConfig("No. Ujian"));
			row.appendChild(searchujian = new MyTextbox());
			searchujian.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});
		}

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal Lahir");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Asal Sekolah/Kampus");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No. Registrasi");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Gelombang");
		if (tampilSederhana) {
			column.setWidth("0%");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan Prodi");
		if (tampilSederhana) {
			column.setWidth("0%");
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataBiodataCalonMahasiswaBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<BiodataCalonMahasiswa> biodataCalonMahasiswas = new ArrayList<BiodataCalonMahasiswa>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								BiodataCalonMahasiswa myBiodataCalonMahasiswa = (BiodataCalonMahasiswa) row
										.getAttribute("biodataCalonMahasiswa");
								biodataCalonMahasiswas.add(myBiodataCalonMahasiswa);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataBiodataCalonMahasiswaBanyak.java:379");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), biodataCalonMahasiswas);
					eventListener.onEvent(myEvent);
				}
				AmbilDataBiodataCalonMahasiswaBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (biodataCalonMahasiswasHanyaDitampilkan != null) {
			for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswasHanyaDitampilkan) {
				values.add(biodataCalonMahasiswa.getId());
			}
		}

		List<Long> notin = new ArrayList<Long>();
		if (biodataCalonMahasiswas != null) {
			for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {
				notin.add(biodataCalonMahasiswa.getId());
			}
		}

		List<BiodataCalonMahasiswa> biodataCalonMahasiswa = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama"))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<BiodataCalonMahasiswa> myBiodataCalonMahasiswa = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(notin.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", notin)))

				.addOrder(Order.asc("nama"))
				.add(searchTahunAjaran == null || searchTahunAjaran.getSelectedItem() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchnoreg == null || searchnoreg.getValue().trim().isEmpty()
						? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noRegistrasi", searchnoreg.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchujian == null || searchujian.getValue().trim().isEmpty()
						? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noUjian", searchujian.getValue().trim(), MatchMode.ANYWHERE))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(biodataCalonMahasiswasHanyaDitampilkan == null || values.size() == 0
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		biodataCalonMahasiswa.addAll(myBiodataCalonMahasiswa);

		ListModel strset = new SimpleListModel(biodataCalonMahasiswa);
		grid.setRowRenderer(new BiodataCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
