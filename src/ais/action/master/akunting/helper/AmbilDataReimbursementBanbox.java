package ais.action.master.akunting.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.ReimbursementPegawai;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Banbox pemilih Reimbursement Pegawai untuk BAST (PenerimaanPengadaanMasterAsset)
 * — klon pola {@link AmbilDataUangMukaBanbox}: hanya menampilkan reimbursement yang
 * sudah DISETUJUI (via alur SOP), masih aktif, dan BELUM pernah diterima
 * (penerimaanPengadaanMasterAsset masih null) sehingga satu reimbursement hanya
 * dapat di-BAST-kan sekali. Objek terpilih disimpan pada attribute
 * "reimbursementPegawai".
 */
public class AmbilDataReimbursementBanbox extends Bandbox implements GetEventListener {

	private static final long serialVersionUID = 1L;

	private MyGrid grid;
	private Textbox kodeCari;
	private Textbox namaCari;
	private EventListener eventListener;

	public AmbilDataReimbursementBanbox() {
		super();
		setReadonly(true);
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

	private void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("760px");
		bandpopup.setHeight("520px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Reimbursement Disetujui (belum diterima)");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(panelchildren);
		Rows rowsCari = new Rows();
		rowsCari.setParent(searchgrid);
		MyFormRow row = new MyFormRow();
		row.setParent(rowsCari);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		kodeCari = new Textbox();
		kodeCari.setWidth("90%");
		row.appendChild(kodeCari);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		namaCari = new Textbox();
		namaCari.setWidth("90%");
		row.appendChild(namaCari);
		EventListener cari = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		};
		kodeCari.addEventListener("onOK", cari);
		namaCari.addEventListener("onOK", cari);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("32px");
		toolbar.setParent(panelchildren);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", cari);
		button.setParent(toolbar);
		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		grid.setParent(panelchildren);

		Columns columns = new Columns();
		columns.setParent(grid);
		String[] judul = new String[] { "", "Kode", "Judul", "Pegawai", "Nominal", "Tgl Pengeluaran" };
		String[] lebar = new String[] { "5%", "17%", "30%", "20%", "14%", "14%" };
		for (int i = 0; i < judul.length; i++) {
			MyColumnConfig column = new MyColumnConfig();
			column.setLabel(judul[i]);
			column.setWidth(lebar[i]);
			column.setParent(columns);
		}

		onSearchDefault(null);
	}

	public void onSearchDefault(Event event) {
		try {
			List semua = HibernateUtil.currentSession().createCriteria(ReimbursementPegawai.class)
					.addOrder(Order.desc("id")).setMaxResults(500).list();

			String kode = kodeCari == null ? "" : kodeCari.getValue().trim().toLowerCase();
			String nama = namaCari == null ? "" : namaCari.getValue().trim().toLowerCase();

			// Filter turunan (status DISETUJUI dihitung dari DisposisiSop, bukan kolom
			// murni) + belum pernah diterima lewat BAST + masih aktif.
			List hasil = new ArrayList();
			for (int i = 0; i < semua.size(); i++) {
				ReimbursementPegawai d = (ReimbursementPegawai) semua.get(i);
				try {
					if (!ReimbursementPegawai.DISETUJUI.equals(d.getStatus())) {
						continue;
					}
					if (!Boolean.TRUE.equals(d.getAktif())) {
						continue;
					}
					if (d.getPenerimaanPengadaanMasterAsset() != null) {
						continue;
					}
					if (!kode.isEmpty() && (d.getKode() == null || d.getKode().toLowerCase().indexOf(kode) < 0)) {
						continue;
					}
					String judul = d.getNama() == null ? (d.getDeskripsi() == null ? "" : d.getDeskripsi()) : d.getNama();
					if (!nama.isEmpty() && judul.toLowerCase().indexOf(nama) < 0) {
						continue;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) AmbilDataReimbursementBanbox.filter");
					continue;
				}
				hasil.add(d);
			}

			ListModel model = new SimpleListModel(hasil);
			grid.setRowRenderer(new ReimbursementRenderer());
			grid.setModelCheckMobile(model);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit AmbilDataReimbursementBanbox.onSearchDefault");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private class ReimbursementRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final ReimbursementPegawai d = (ReimbursementPegawai) arg1;

			Radio radio = new Radio("");
			radio.setParent(arg0);
			radio.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataReimbursementBanbox.this.setOpen(false);
					AmbilDataReimbursementBanbox.this.setAttribute("reimbursementPegawai", d);
					AmbilDataReimbursementBanbox.this.setValue(d.getKode());
					if (eventListener != null) {
						eventListener.onEvent(new Event("", AmbilDataReimbursementBanbox.this, d));
					}
				}
			});

			new Label(d.getKode()).setParent(arg0);
			new Label(d.getNama() == null ? d.getDeskripsi() : d.getNama()).setParent(arg0);
			new Label(d.getPegawai() == null ? "-" : d.getPegawai().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(d.getNominal())).setParent(arg0);
			new Label(d.getTanggalPengeluaran() == null ? "-"
					: Common.dateFormat4.get().format(d.getTanggalPengeluaran())).setParent(arg0);
		}
	}

	@Override
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	@Override
	public EventListener getEventListener() {
		return eventListener;
	}
}
