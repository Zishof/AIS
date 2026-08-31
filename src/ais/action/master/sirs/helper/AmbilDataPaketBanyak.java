package ais.action.master.sirs.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.JenisTindakan;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data paket banyak. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code EventListener
 * eventListener}, {@code List tindakans}, {@code List tindakansHanyaDitampilkan}, {@code Set ids}, {@code
 * Boolean tindakanLab}, {@code Boolean tindakanOperasi}, {@code Boolean tindakanRadiologi}; pembacaan/pencarian
 * ({@code getTindakanLab()}, {@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()});
 * mutasi data ({@code setTindakanLab()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class AmbilDataPaketBanyak extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;
	private EventListener eventListener;
	private List<Tindakan> tindakans;
	private List<Tindakan> tindakansHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public Boolean getTindakanLab() {
		return tindakanLab;
	}

	public void setTindakanLab(Boolean tindakanLab) {
		this.tindakanLab = tindakanLab;
	}

	private Boolean tindakanLab = null;
	private Boolean tindakanOperasi = null;
	private Boolean tindakanRadiologi = null;
	private Boolean tindakanVk = null;
	private Boolean tindakanRenalUnit = null;
	private Boolean tindakanGizi = null;

	public AmbilDataPaketBanyak(List<Tindakan> tindakans, Boolean tindakanLab, Boolean tindakanOperasi,
			Boolean tindakanRadiologi, Boolean tindakanVk, Boolean tindakanRenalUnit, Boolean tindakanGizi) {
		super();
		this.tindakans = tindakans;
		this.tindakanLab = tindakanLab;
		this.tindakanOperasi = tindakanOperasi;
		this.tindakanRadiologi = tindakanRadiologi;
		this.tindakanVk = tindakanVk;
		this.tindakanRenalUnit = tindakanRenalUnit;
		this.tindakanGizi = tindakanGizi;
		display();

		onSearchDefault(null);
	}

	public AmbilDataPaketBanyak(List<Tindakan> tindakans) {
		super();
		this.tindakans = tindakans;
		display();

		onSearchDefault(null);
	}

	public AmbilDataPaketBanyak(List<Tindakan> tindakans, List<Tindakan> tindakansHanyaDitampilkan) {
		super();
		this.tindakans = tindakans;
		this.tindakansHanyaDitampilkan = tindakansHanyaDitampilkan;
		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeTindakanan;
	private MyTextbox nama;
	private Combobox jenisTindakan;

	private Checkbox tindakanLabCheck = null;
	private Checkbox tindakanOperasiCheck = null;
	private Checkbox tindakanRadiologiCheck = null;
	private Checkbox tindakanVkCheck = null;
	private Checkbox tindakanRenalUnitCheck = null;
	private Checkbox tindakanGiziCheck = null;

	class TindakanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Tindakan tindakan = (Tindakan) arg1;
			arg0.setAttribute("tindakan", tindakan);
			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(arg0);
			for (Tindakan myTindakan : tindakans) {
				if (myTindakan != null && myTindakan.getId() != null && myTindakan.getId().equals(tindakan.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(tindakan.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(tindakan.getId());
					} else {
						ids.remove(tindakan.getId());
					}
				}
			});

			new Label(tindakan.getKode()).setParent(arg0);
			new Label(tindakan.getNama()).setParent(arg0);
			new Label(tindakan.getJenisTindakan() == null ? "" : tindakan.getJenisTindakan().getNama()).setParent(arg0);
			new Html(tindakan.getKeteranganLayanan()).setParent(arg0);
		}

	}

	@SuppressWarnings("deprecation")
	public void display() {

		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Tindakan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
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

		Grid searchgrid = new Grid();
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Tindakan")));
		row.appendChild(kodeTindakanan = new MyTextbox());
		kodeTindakanan.setWidth("90%");
		kodeTindakanan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Tindakan")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Tindakan")));
		row.appendChild(jenisTindakan = new Combobox());
		Common.insertCombo(jenisTindakan, "nama", JenisTindakan.class);
		jenisTindakan.setWidth("90%");
		jenisTindakan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "6");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.setVisible(tindakanLab == null && tindakanOperasi == null && tindakanRadiologi == null && tindakanVk == null
				&& tindakanRenalUnit == null && tindakanGizi == null);
		row.appendChild(new Hbox(new Component[] { tindakanLabCheck = new Checkbox("Lab."),
				tindakanOperasiCheck = new Checkbox("Operasi"), tindakanRadiologiCheck = new Checkbox("Radiologi"),
				tindakanVkCheck = new Checkbox("VK"), tindakanRenalUnitCheck = new Checkbox("Renal Unit"),
				tindakanGiziCheck = new Checkbox("Gizi") }));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Layanan");

		// onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(south);

		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataPaketBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Tindakan> tindakans = new ArrayList<Tindakan>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						Checkbox checkbox = (Checkbox) row.getChildren().get(0);
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							Tindakan myTindakan = (Tindakan) row.getAttribute("tindakan");
							tindakans.add(myTindakan);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), tindakans);
					eventListener.onEvent(myEvent);
				}
				AmbilDataPaketBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Semua Data", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();

				Criterion criterion = Restrictions.sqlRestriction("1!=1");
				boolean adaOr = false;
				if (tindakanLab != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanLab", tindakanLab));
					adaOr = true;
				}
				if (tindakanGizi != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanGizi", tindakanGizi));
					adaOr = true;
				}
				if (tindakanOperasi != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanOperasi", tindakanOperasi));
					adaOr = true;
				}
				if (tindakanRadiologi != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRadiologi", tindakanRadiologi));
					adaOr = true;
				}
				if (tindakanRenalUnit != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRenalUnit", tindakanRenalUnit));
					adaOr = true;
				}
				if (tindakanVk != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanVk", tindakanVk));
					adaOr = true;
				}

				if (tindakanLabCheck.isChecked()) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanLab", true));
					adaOr = true;
				}
				if (tindakanGiziCheck.isChecked()) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanGizi", true));
					adaOr = true;
				}
				if (tindakanOperasiCheck.isChecked()) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanOperasi", true));
					adaOr = true;
				}
				if (tindakanRadiologiCheck.isChecked()) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRadiologi", true));
					adaOr = true;
				}
				if (tindakanRenalUnitCheck.isChecked()) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRenalUnit", true));
					adaOr = true;
				}
				if (tindakanVkCheck.isChecked()) {
					criterion = Restrictions.or(criterion, Restrictions.eq("tindakanVk", true));
					adaOr = true;
				}

				JenisTindakan jenisTindakan = (JenisTindakan) (AmbilDataPaketBanyak.this.jenisTindakan
						.getSelectedItem() == null ? null
								: AmbilDataPaketBanyak.this.jenisTindakan.getSelectedItem().getValue());
				List<Tindakan> myTindakan = session.createCriteria(Tindakan.class)
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.not(Restrictions.in("id", ids)))
						.add(adaOr ? criterion : Restrictions.sqlRestriction("1=1")).addOrder(Order.asc("nama"))
						.add(jenisTindakan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisTindakan", jenisTindakan))
						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kode", kodeTindakanan.getValue().trim(), MatchMode.ANYWHERE)).list();
				Event myEvent = new Event("myEvent", event.getTarget(), myTindakan);
				eventListener.onEvent(myEvent);

				AmbilDataPaketBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criterion criterion = Restrictions.sqlRestriction("1!=1");
		boolean adaOr = false;
		if (tindakanLab != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanLab", tindakanLab));
			adaOr = true;
		}
		if (tindakanGizi != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanGizi", tindakanGizi));
			adaOr = true;
		}
		if (tindakanOperasi != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanOperasi", tindakanOperasi));
			adaOr = true;
		}
		if (tindakanRadiologi != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRadiologi", tindakanRadiologi));
			adaOr = true;
		}
		if (tindakanRenalUnit != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRenalUnit", tindakanRenalUnit));
			adaOr = true;
		}
		if (tindakanVk != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanVk", tindakanVk));
			adaOr = true;
		}

		if (tindakanLabCheck.isChecked()) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanLab", true));
			adaOr = true;
		}
		if (tindakanGiziCheck.isChecked()) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanGizi", true));
			adaOr = true;
		}
		if (tindakanOperasiCheck.isChecked()) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanOperasi", true));
			adaOr = true;
		}
		if (tindakanRadiologiCheck.isChecked()) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRadiologi", true));
			adaOr = true;
		}
		if (tindakanRenalUnitCheck.isChecked()) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRenalUnit", true));
			adaOr = true;
		}
		if (tindakanVkCheck.isChecked()) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanVk", true));
			adaOr = true;
		}

		JenisTindakan jenisTindakan = (JenisTindakan) (this.jenisTindakan.getSelectedItem() == null ? null
				: this.jenisTindakan.getSelectedItem().getValue());

		List<Long> values = new ArrayList<Long>();
		if (tindakansHanyaDitampilkan != null) {
			for (Tindakan tindakan : tindakansHanyaDitampilkan) {
				values.add(tindakan.getId());
			}
		}

		List<Tindakan> tindakan = ConstantValues.simpleList(
				session.createCriteria(Tindakan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(adaOr ? criterion : Restrictions.sqlRestriction("1=1"))
						.add(Restrictions.eq("jenisPaket", Tindakan.JENIS_PERAWATAN_PAKET))

						.addOrder(Order.asc("nama"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Tindakan.class);

		List<Tindakan> myTindakan = ConstantValues.simpleList(session.createCriteria(Tindakan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(adaOr ? criterion : Restrictions.sqlRestriction("1=1"))
				.add(Restrictions.eq("jenisPaket", Tindakan.JENIS_PERAWATAN_PAKET))

				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(tindakansHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(jenisTindakan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisTindakan", jenisTindakan))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeTindakanan.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT), Tindakan.class);

		tindakan.addAll(myTindakan);

		ListModel strset = new SimpleListModel(tindakan);
		grid.setRowRenderer(new TindakanRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
