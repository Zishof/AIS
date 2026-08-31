package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.pkl.KelompokPklAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.Tbmuser;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data kelompok pkl banbox. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code EventListener
 * eventListener}, {@code Textbox alamat}, {@code Textbox nama}; pembacaan/pencarian ({@code onSearchDefault()},
 * {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataKelompokPklBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;

	// private Mahasiswa mahasiswa = null;

	public AmbilDataKelompokPklBanbox() {
		this(true);
	}

	public AmbilDataKelompokPklBanbox(Boolean notDeafault) {
		super();
		setReadonly(true);

		// mahasiswa = Common.getCurrentUser().getMahasiswa();

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

	}

	private Textbox alamat;
	private Textbox nama;

	class KelompokPklRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokPkl kelompokPkl = (KelompokPkl) arg1;
			Radio checkbox = new Radio(kelompokPkl.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKelompokPklBanbox.this.setOpen(false);
					AmbilDataKelompokPklBanbox.this.setAttribute("kelompokPkl", kelompokPkl);
					AmbilDataKelompokPklBanbox.this.setAttribute("myValue", kelompokPkl);
					AmbilDataKelompokPklBanbox.this.setValue(kelompokPkl.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			KelompokPklAction.tampilkanInfoDosen(kelompokPkl, false, true).setParent(arg0);

			new Label(kelompokPkl.getAlamat()).setParent(arg0);

			int count = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("kelompokPkl", kelompokPkl)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();

			new Label(kelompokPkl.getKuota() + " / " + count).setParent(arg0);

			checkbox.setDisabled(count >= kelompokPkl.getKuota());
		}

	}

	public void display() {

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("750px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
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
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox());
		alamat.setWidth("90%");

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

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
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
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pembimbing");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");
		column.setLabel("Alamat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kuota");
		column.setWidth("10%");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Fakultas fakultas = null;
		Jurusan jurusan = null;
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			fakultas = tbmuser.getMahasiswa().getJurusan().getFakultas();
			jurusan = tbmuser.getMahasiswa().getJurusan();
		} else if (tbmuser != null) {
			fakultas = tbmuser.ambilFakultas();
			jurusan = tbmuser.ambilJurusan();
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(KelompokPkl.class)

				.createAlias("pkl", "pkl")

				.add(fakultas == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("pkl.fakultas", fakultas),
								Restrictions.isNull("pkl.fakultas")))
				.add(jurusan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("pkl.jurusan", jurusan), Restrictions.isNull("pkl.jurusan")))

				.add(Restrictions.eq("mahasiswaBisaMemilih", true));

		criteria.addOrder(Order.asc("nama"))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(alamat.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("alamat", alamat.getText().trim(), MatchMode.ANYWHERE));

		List<KelompokPkl> kelompokPkl = criteria.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(kelompokPkl);
		grid.setRowRenderer(new KelompokPklRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
