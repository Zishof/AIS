package ais.action.master.sirs.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
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
import ais.database.model.sirs.Pasien;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data pasien banyak. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code EventListener
 * eventListener}, {@code List pasiens}, {@code List pasiensHanyaDitampilkan}, {@code Set ids}, {@code MyTextbox
 * kodePasienan}, {@code MyTextbox nama}, {@code MyTextbox telp}; pembacaan/pencarian ({@code onSearchDefault()},
 * {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class AmbilDataPasienBanyak extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;
	private EventListener eventListener;
	private List<Pasien> pasiens;
	private List<Pasien> pasiensHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataPasienBanyak(List<Pasien> pasiens) {
		super();
		this.pasiens = pasiens;

		display();

		onSearchDefault(null);
	}

	public AmbilDataPasienBanyak(List<Pasien> pasiens, List<Pasien> pasiensHanyaDitampilkan) {
		super();
		this.pasiens = pasiens;
		this.pasiensHanyaDitampilkan = pasiensHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox kodePasienan;
	private MyTextbox nama;
	private MyTextbox telp;
	private MyTextbox alamat;

	class PasienRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Pasien pasien = (Pasien) arg1;
			arg0.setAttribute("pasien", pasien);
			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(arg0);
			for (Pasien myPasien : pasiens) {
				if (myPasien.getId().equals(pasien.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(pasien.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(pasien.getId());
					} else {
						ids.remove(pasien.getId());
					}
				}
			});

			new Label(pasien.getKode()).setParent(arg0);
			new Label(pasien.getNama()).setParent(arg0);
			new Label(pasien.getNoTelp()).setParent(arg0);
			new Label(pasien.getNoHp()).setParent(arg0);
			new Label(pasien.getAsuransi() == null ? "" : pasien.getAsuransi().getNama()).setParent(arg0);
			new Label(pasien.getAlamatLengkap()).setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pasien");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR")));
		row.appendChild(kodePasienan = new MyTextbox());
		kodePasienan.setWidth("90%");
		kodePasienan.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Telp")));
		row.appendChild(telp = new MyTextbox());
		telp.setWidth("90%");
		telp.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new MyTextbox());
		alamat.setWidth("90%");
		alamat.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

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
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("MR");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("No. Telp.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("No. Hp");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Asuransi");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alamat");

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
				AmbilDataPasienBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Pasien> pasiens = new ArrayList<Pasien>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						Checkbox checkbox = (Checkbox) row.getChildren().get(0);
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							Pasien myPasien = (Pasien) row.getAttribute("pasien");
							pasiens.add(myPasien);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), pasiens);
					eventListener.onEvent(myEvent);
				}
				AmbilDataPasienBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Criterion criterion = Restrictions.ilike("alamat", alamat.getValue(), MatchMode.ANYWHERE);

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("propinsi.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("kota.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("kecamatan.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("kelurahan.nama", alamat.getValue(), MatchMode.ANYWHERE));

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (pasiensHanyaDitampilkan != null) {
			for (Pasien pasien : pasiensHanyaDitampilkan) {
				values.add(pasien.getId());
			}
		}

		List<Pasien> pasien = ConstantValues.simpleList(
				session.createCriteria(Pasien.class).addOrder(Order.asc("nama"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Pasien.class);

		List<Pasien> myPasien = ConstantValues.simpleList(session.createCriteria(Pasien.class)

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(pasiensHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(Restrictions.not(Restrictions.ilike("kode", "SS", MatchMode.ANYWHERE)))
				.add(Restrictions.not(Restrictions.ilike("kode", "DD", MatchMode.ANYWHERE)))
				.add(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.ANYWHERE))).addOrder(Order.desc("id"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(kodePasienan.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kodePasienan.getValue().trim(), MatchMode.ANYWHERE))

				.createAlias("propinsi", "propinsi", Criteria.LEFT_JOIN).createAlias("kota", "kota", Criteria.LEFT_JOIN)
				.createAlias("kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				.createAlias("kelurahan", "kelurahan", Criteria.LEFT_JOIN)

				.add(criterion)

				.add(Restrictions.or(Restrictions.ilike("noTelp", telp.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("noHp", telp.getValue(), MatchMode.ANYWHERE)))

				.setMaxResults(Common.MAX_RESULT), Pasien.class);

		pasien.addAll(myPasien);

		ListModel strset = new SimpleListModel(pasien);
		grid.setRowRenderer(new PasienRenderer());
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
