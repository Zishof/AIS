package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Dosen} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). Varian {@link AmbilDataDosenBanbox}
 * yang disederhanakan khusus untuk pemilihan dosen pembimbing/penguji skripsi: tanpa auto-default
 * dosen milik user login, tanpa parameter {@code hanyaDosenTetap}/{@code tanpaLihatPt}/
 * {@code perguruanTinggi}.
 * <p>
 * Popup menampilkan grid pilih-tunggal (via {@link Radiogroup}) dengan filter "NIP" (cocok kolom
 * {@code code}), "Nama", "Fakultas", dan "Prodi" (combobox berjenjang, diinisialisasi lewat
 * {@code Common.initFakultasDanJurusanDanSemua} pada listener {@code onOpen} — beda dari
 * {@link AmbilDataDosenBanbox} yang menginisialisasinya di {@link #display()}). Hasil selalu
 * dibatasi ke dosen aktif; filter jurusan dan fakultas masing-masing dilewati (OR) bila dosen
 * {@code milikUniversitas=true}, sehingga dosen lintas-prodi selalu muncul di hasil apa pun
 * filter fakultas/prodi yang dipilih.
 *
 * @see Bandbox
 */
public class AmbilDataDosenSkripsiBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;

	/**
	 * Membangun komponen: memasang mode read-only standar dan listener {@code onOpen} yang, pada
	 * pembukaan pertama, menginisialisasi combobox Fakultas/Jurusan lewat
	 * {@code Common.initFakultasDanJurusanDanSemua} lalu membangun popup ({@link #display()}) —
	 * mengikuti kerangka umum di {@link ais.ui.util.GetEventListener}.
	 */
	public AmbilDataDosenSkripsiBanbox() {
		super();
		setReadonly(true);
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

	private Textbox kode;
	private Textbox nama;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	/**
	 * Merender satu baris grid: radio pilih, kode, NIDN, nama, alamat, dan kepemilikan
	 * ("Milik Universitas" bila {@code milikUniversitas=true}, atau nama
	 * jurusan/fakultas pemilik). Memilih baris menutup popup, menyimpan entity {@link Dosen}
	 * terpilih ke attribute {@code "dosen"}/{@code "myValue"} pada Bandbox, mengisi teks
	 * tampilan dengan namanya, lalu memicu {@link #eventListener} bila terpasang — mengikuti
	 * kerangka callback standar di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataDosenSkripsiBanbox
	 */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Dosen dosen = (Dosen) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(dosen.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataDosenSkripsiBanbox.this.setOpen(false);
					AmbilDataDosenSkripsiBanbox.this.setAttribute("dosen", dosen);
					AmbilDataDosenSkripsiBanbox.this.setAttribute("myValue", dosen);
					AmbilDataDosenSkripsiBanbox.this.setValue(dosen.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			// if (tbmuser.getFakultas() != null) {
			//// System.out.println(tbmuser.getFakultas().getNama() + " - "
			//// + dosen.getFakultas().getNama());
			//// System.out.println(!tbmuser.getFakultas().equals(
			//// dosen.getFakultas()));
			// if (!tbmuser.getFakultas().equals(dosen.getFakultas())
			// && (dosen.getMilikUniversitas() == null || dosen
			// .getMilikUniversitas() == false)) {
			// checkbox.setDisabled(true);
			// }
			// }

			new Label(dosen.getCode()).setParent(arg0);

			new Label(dosen.getNidn()).setParent(arg0);
			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getAlamat()).setParent(arg0);
			String milik = "";
			if (dosen.getFakultas() != null) {
				milik = dosen.getFakultas().getNama();
			}
			if (dosen.getJurusan() != null) {
				milik = dosen.getJurusan().getNama();
			}
			new Label(dosen.getMilikUniversitas() != null && dosen.getMilikUniversitas() == true ? "Milik Universitas"
					: milik).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form filter NIP/Nama/
	 * Fakultas/Prodi, grid hasil bermold "paging", lalu memuat data awal lewat
	 * {@link #onSearchDefault(Event)}. Setelah render, filter fakultas/prodi dipastikan aktif
	 * (tidak terkunci) lewat timer default.
	 */
	public void display() {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("850px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Dosen");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIP"));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");

		// row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		// row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(div);

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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIP");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIDN/NUPN");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alamat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");
		column.setLabel("Kepemilikan");

		onSearchDefault(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

			}
		});
	}

	/**
	 * Menyusun dan menjalankan kriteria pencarian {@link Dosen}: aktif, cocok nama dan kode
	 * (ILIKE ANYWHERE), cocok jurusan ATAU {@code milikUniversitas=true}, cocok fakultas ATAU
	 * {@code milikUniversitas=true} — dosen lintas-prodi selalu lolos filter fakultas/jurusan.
	 * Dibatasi {@link Common#MAX_RESULT} baris. Mengisi ulang grid dengan hasilnya beserta
	 * {@link DosenRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Dosen.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		criteria.addOrder(Order.asc("nama")).add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("code", kode.getText().trim(), MatchMode.ANYWHERE))

				.add(Restrictions.or(
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
						Restrictions.eq("milikUniversitas", true)))

				.add(Restrictions.or(Restrictions.eq("milikUniversitas", true),
						searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));

		List<Dosen> dosen = criteria.setMaxResults(Common.MAX_RESULT).list();

		// System.out.println(dosen);
		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @param eventListener dipanggil setiap kali user memilih satu dosen */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan dosen yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}
