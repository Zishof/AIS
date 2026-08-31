package ais.action.master.sirs.helper;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.TempatTidur;
import ais.ui.util.MyTextbox;

/**
 * Helper terfokus untuk monitor data tempat tidur. Tipe ini membungkus satu variasi kecil dari
 * alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code EventListener
 * eventListener}, {@code MyTextbox nama}, {@code KelasPerawatan myKelasPerawatan}, {@code Ruang myRuang}, {@code
 * Kamar myKamar}, {@code Combobox kelasPerawatan}, {@code Combobox ruangPerawatan}; pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}, {@code getMyKelasPerawatan()},
 * {@code getMyRuang()}, {@code getMyKamar()}); mutasi data ({@code setMyKelasPerawatan()}, {@code setMyRuang()},
 * {@code setMyKamar()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class MonitorDataTempatTidurHelper extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;

	private EventListener eventListener;

	public MonitorDataTempatTidurHelper() {
		super();
		display();
	}

	private MyTextbox nama;
	private KelasPerawatan myKelasPerawatan;
	private Ruang myRuang;
	private Kamar myKamar;
	private Combobox kelasPerawatan;
	private Combobox ruangPerawatan;
	private Combobox kamarPerawatan;

	/**
	 * Renderer lokal untuk layar/komponen {@link MonitorDataTempatTidurHelper}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MonitorDataTempatTidurHelper} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see MonitorDataTempatTidurHelper
	 */
	class TempatTidurRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TempatTidur tempatTidur = (TempatTidur) arg1;

			tempatTidur.updateTerisi();

			if (tempatTidur.getTerisi() != null && tempatTidur.getTerisi()) {
				arg0.setStyle("background-color:red;");
			} else if (tempatTidur.getStatusTempatTidur() != null
					&& !tempatTidur.getStatusTempatTidur().getId().equals(ConstantValues.TERSEDIA.getId())) {
				arg0.setStyle("background-color:yellow;");
			}
			// } else {
			// arg0.setStyle("background-color:green;");
			// }

			new Label(tempatTidur.getNama()).setParent(arg0);
			new Label(tempatTidur.getTerisi() == null || !tempatTidur.getTerisi() ? "Tidak" : "Iya").setParent(arg0);

			if (tempatTidur.getTerisi() != null && tempatTidur.getTerisi()) {
				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.property("pasien"));
				projectionList.add(Projections.property("kode"));
				projectionList.add(Projections.property("tanggalPendaftaran"));

				Object[] pendaftaran = (Object[]) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
						.setProjection(projectionList).add(Restrictions.eq("tempatTidur", tempatTidur))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

				if (pendaftaran != null) {
					Pasien pasien = (Pasien) (pendaftaran.length < 1 ? null : pendaftaran[0]);
					String kode = (String) (pendaftaran.length < 2 ? null : pendaftaran[1]);
					Date tanggalPendaftaran = (Date) (pendaftaran.length < 3 ? null : pendaftaran[2]);
					new Html(pasien == null ? ""
							: pasien.getKode() + " - " + pasien.getNama() + "<br><b>No. Reg </b>: " + kode
									+ "<br><b>Wkt. Reg </b>: "
									+ (tanggalPendaftaran == null ? "" : Common.dateFormat3.get().format(tanggalPendaftaran)))
							.setParent(arg0);
				} else {
					new Label(ais.common.Common.getBahasaConfig("Tidak ada keterangan")).setParent(arg0);
				}
			} else {
				new Label("").setParent(arg0);
			}

			new Label(tempatTidur.getKelasPerawatan() == null ? "" : tempatTidur.getKelasPerawatan().getNama())
					.setParent(arg0);
			new Label(tempatTidur.getRuang() == null ? "" : tempatTidur.getRuang().getNama()).setParent(arg0);
			new Label(tempatTidur.getKamar() == null ? "" : tempatTidur.getKamar().getNama()).setParent(arg0);
			new Label(tempatTidur.getStatusTempatTidur() == null ? "" : tempatTidur.getStatusTempatTidur().getNama())
					.setParent(arg0);
			new Label(tempatTidur.getKeterangan()).setParent(arg0);
		}

	}

	public void display() {
		Common.clear(this);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		Grid searchgrid = new Grid();
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Tempat Tidur")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas Perawatan")));
		row.appendChild(kelasPerawatan = new Combobox());
		Common.insertCombo(kelasPerawatan, "nama", KelasPerawatan.class,
				Restrictions.ne("id", ConstantValues.kelasNormalId()));
		Common.selectComboItem(kelasPerawatan, myKelasPerawatan);
		kelasPerawatan.setDisabled(myKelasPerawatan != null);
		kelasPerawatan.setWidth("90%");
		kelasPerawatan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ruang")));
		row.appendChild(ruangPerawatan = new Combobox());
		Common.insertCombo(ruangPerawatan, "nama", Ruang.class);
		Common.selectComboItem(ruangPerawatan, myRuang);
		ruangPerawatan.setDisabled(myRuang != null);
		ruangPerawatan.setWidth("90%");
		ruangPerawatan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kamar")));
		row.appendChild(kamarPerawatan = new Combobox());
		kamarPerawatan.setWidth("90%");
		kamarPerawatan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kamarPerawatan);

				Common.insertCombo(kamarPerawatan, "nama", "keterangan", Kamar.class, Restrictions.and(
						ruangPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ruang", ruangPerawatan.getSelectedItem().getValue()),
						kelasPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kelasPerawatan", kelasPerawatan.getSelectedItem().getValue())));

				Common.selectComboItem(kamarPerawatan, myKamar);
				kamarPerawatan.setDisabled(myKamar != null);

			}
		};

		kelasPerawatan.addEventListener("onChange", myEventListener);
		ruangPerawatan.addEventListener("onChange", myEventListener);
		try {
			myEventListener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/MonitorDataTempatTidurHelper.java:224");
		}

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(div);

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
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Bed");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Terisi");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pasien");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ruang");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kamar");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ket.");
		column.setWidth("10%");

		onSearchDefault(null);

		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(borderlayout);
		//
		// toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setParent(south);
		//
		// button = new ais.ui.util.MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		// button.setTooltiptext("Tutup");
		// button.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// detach();
		// }
		// });
		// button.setParent(toolbar);toolbar.appendChild(Common.createCleanButton(this,
		// new EventListener() {@Override public void onEvent(Event event)
		// throws Exception {if(eventListener != null){try
		// {eventListener.onEvent(null);} catch (Exception e)
		// {e.printStackTrace();}}onSearchDefault(event);}}));

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<TempatTidur> tempatTidur = session.createCriteria(TempatTidur.class).addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(ruangPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", ruangPerawatan.getSelectedItem().getValue()))
				.add(kamarPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kamar", kamarPerawatan.getSelectedItem().getValue()))
				.add(kelasPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kelasPerawatan", kelasPerawatan.getSelectedItem().getValue()))
				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(tempatTidur);
		grid.setRowRenderer(new TempatTidurRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setMyKelasPerawatan(KelasPerawatan myKelasPerawatan) {
		this.myKelasPerawatan = myKelasPerawatan;
		display();
	}

	public KelasPerawatan getMyKelasPerawatan() {
		return myKelasPerawatan;
	}

	public void setMyRuang(Ruang myRuang) {
		this.myRuang = myRuang;
		display();
	}

	public Ruang getMyRuang() {
		return myRuang;
	}

	public void setMyKamar(Kamar myKamar) {
		this.myKamar = myKamar;
		display();
	}

	public Kamar getMyKamar() {
		return myKamar;
	}

}
