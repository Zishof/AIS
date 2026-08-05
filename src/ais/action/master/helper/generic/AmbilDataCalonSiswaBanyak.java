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
import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.CalonSiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataCalonSiswaBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<CalonSiswa> calonSiswas;
	private List<CalonSiswa> calonSiswasHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataCalonSiswaBanyak(List<CalonSiswa> calonSiswas) {
		super();
		this.calonSiswas = calonSiswas;
		display();
		onSearchDefault(null);
	}

	public AmbilDataCalonSiswaBanyak(List<CalonSiswa> calonSiswas, List<CalonSiswa> calonSiswasHanyaDitampilkan) {
		super();
		this.calonSiswas = calonSiswas;
		this.calonSiswasHanyaDitampilkan = calonSiswasHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox nama;
	private MyTextbox searchnoreg;
	private MyTextbox searchujian;
	private Combobox searchTahunAjaran;
	private boolean tampilSederhana;

	class CalonSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CalonSiswa calonSiswa = (CalonSiswa) arg1;
			arg0.setAttribute("calonSiswa", calonSiswa);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (CalonSiswa myCalonSiswa : calonSiswas) {
				if (myCalonSiswa.getId().equals(calonSiswa.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(calonSiswa.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(calonSiswa.getId());
					} else {
						ids.remove(calonSiswa.getId());
					}
				}
			});

			CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(arg0);

			RevisiHelper.createNewRevisi(CalonSiswa.class, calonSiswa, calonSiswa.getNama()).setParent(arg0);

			RevisiHelper.createNewRevisi(CalonSiswa.class, calonSiswa,
					calonSiswa.getTanggalLahir() == null ? Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
							: Common.dateFormat2.get().format(calonSiswa.getTanggalLahir()))
					.setParent(arg0);

			new Label(calonSiswa.getSekolahAsal()).setParent(arg0);
			
			Vbox a = new Vbox(); 
			a.setParent(arg0);
			new Label(calonSiswa.getNoRegistrasi()).setParent(a);
			
			new Label(calonSiswa.getSiswa() == null ? "" : calonSiswa.getSiswa().getNomorInduk()).setParent(a);
			new Label(calonSiswa.getSiswa() == null ? "" : calonSiswa.getSiswa().getNomorIndukNasional()).setParent(a);
			
			new Label(calonSiswa.getNamaAyah()).setParent(arg0);
			new Label(calonSiswa.getNamaIbu()).setParent(arg0);

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
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
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
		column.setWidth("70px");

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
		column.setLabel("No. Registrasi, Ujian, NIS");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ayah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ibu");
		column.setWidth("10%");

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
				AmbilDataCalonSiswaBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<CalonSiswa> calonSiswas = new ArrayList<CalonSiswa>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								CalonSiswa myCalonSiswa = (CalonSiswa) row.getAttribute("calonSiswa");
								calonSiswas.add(myCalonSiswa);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataCalonSiswaBanyak.java:335");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), calonSiswas);
					eventListener.onEvent(myEvent);
				}
				AmbilDataCalonSiswaBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (calonSiswasHanyaDitampilkan != null) {
			for (CalonSiswa calonSiswa : calonSiswasHanyaDitampilkan) {
				values.add(calonSiswa.getId());
			}
		}

		List<Long> notin = new ArrayList<Long>();
		if (calonSiswas != null) {
			for (CalonSiswa calonSiswa : calonSiswas) {
				notin.add(calonSiswa.getId());
			}
		}

		List<CalonSiswa> calonSiswa = session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb")).addOrder(Order.asc("nama"))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<CalonSiswa> myCalonSiswa = session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))

				.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb")

				.add(notin.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", notin)))

				.addOrder(Order.asc("nama"))
				.add(searchTahunAjaran == null || searchTahunAjaran.getSelectedItem() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("gelombangPendaftaranPsb.tahunAjaran",
								searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchnoreg == null || searchnoreg.getValue().trim().isEmpty()
						? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noRegistrasi", searchnoreg.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchujian == null || searchujian.getValue().trim().isEmpty()
						? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noUjian", searchujian.getValue().trim(), MatchMode.ANYWHERE))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(calonSiswasHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		calonSiswa.addAll(myCalonSiswa);

		ListModel strset = new SimpleListModel(calonSiswa);
		grid.setRowRenderer(new CalonSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
