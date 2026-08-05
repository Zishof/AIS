package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;

import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Paket;
import ais.database.model.PaketJurusanPmb;

public class DetailPaketJurusanPmbHelper implements DataLoader {

	private MyGrid grid;
	private Paket paket;

	class DetailPaketRenderer extends ais.ui.util.MyRowRenderer {

		public DetailPaketRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PaketJurusanPmb paketJurusanPmb = (PaketJurusanPmb) data;

			new Label(paketJurusanPmb.getJurusan().getFakultas().getNama()).setParent(row);
			new Label(paketJurusanPmb.getJurusan().getNama()).setParent(row);

			final MyCheckboxConfig pilihan1 = new MyCheckboxConfig();
			pilihan1.setChecked(paketJurusanPmb.getPilihan1());
			pilihan1.setParent(row);

			final MyCheckboxConfig pilihan2 = new MyCheckboxConfig();
			pilihan2.setChecked(paketJurusanPmb.getPilihan2());
			pilihan2.setParent(row);

			final MyCheckboxConfig pilihan3 = new MyCheckboxConfig();
			pilihan3.setChecked(paketJurusanPmb.getPilihan3());
			pilihan3.setParent(row);

			final MyCheckboxConfig pilihan4 = new MyCheckboxConfig();
			pilihan4.setChecked(paketJurusanPmb.getPilihan4());
			pilihan4.setParent(row);

			final MyCheckboxConfig pilihan5 = new MyCheckboxConfig();
			pilihan5.setChecked(paketJurusanPmb.getPilihan5());
			pilihan5.setParent(row);

			final Combobox kelamin = new Combobox();
			kelamin.setReadonly(true);
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Semua");
			comboitem.setValue("Semua");
			kelamin.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Laki-laki");
			comboitem.setValue("Laki-laki");
			kelamin.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Perempuan");
			comboitem.setValue("Perempuan");
			kelamin.appendChild(comboitem);
			kelamin.setParent(row);
			kelamin.setWidth("90%");
			Common.selectComboItem(kelamin, paketJurusanPmb.getKelamin());

			final Intbox kuota = new Intbox();
			kuota.setValue(paketJurusanPmb.getKuota());
			kuota.setWidth("80px");
			kuota.setParent(row);
			kuota.setTooltiptext("Kuota maksimal untuk kombinasi paket dan prodi ini. Kosong/0 berarti tidak dibatasi.");

			final MyCheckboxConfig kuotaBerlakuPerGelombang = new MyCheckboxConfig();
			kuotaBerlakuPerGelombang.setChecked(paketJurusanPmb.getKuotaBerlakuPerGelombang());
			kuotaBerlakuPerGelombang.setParent(row);
			kuotaBerlakuPerGelombang.setTooltiptext(
					"Jika dicentang, kuota dihitung per gelombang. Jika tidak, kuota dihitung untuk semua gelombang pada tahun akademik yang sama.");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					session.refresh(paketJurusanPmb);
					paketJurusanPmb.setPilihan1(pilihan1.isChecked());
					paketJurusanPmb.setPilihan2(pilihan2.isChecked());
					paketJurusanPmb.setPilihan3(pilihan3.isChecked());
					paketJurusanPmb.setPilihan4(pilihan4.isChecked());
					paketJurusanPmb.setPilihan5(pilihan5.isChecked());
					paketJurusanPmb.setKelamin(kelamin.getSelectedItem() == null ? "Semua"
							: (String) kelamin.getSelectedItem().getValue());
					paketJurusanPmb.setKuota(kuota.getValue() == null ? 0 : kuota.getValue());
					paketJurusanPmb.setKuotaBerlakuPerGelombang(kuotaBerlakuPerGelombang.isChecked());
					Common.refreshSaveOrUpdate(session, paketJurusanPmb);
				}
			};

			pilihan1.addEventListener("onClick", eventListener);
			pilihan2.addEventListener("onClick", eventListener);
			pilihan3.addEventListener("onClick", eventListener);
			pilihan4.addEventListener("onClick", eventListener);
			pilihan5.addEventListener("onClick", eventListener);
			kelamin.addEventListener("onChange", eventListener);
			kuota.addEventListener("onChange", eventListener);
			kuota.addEventListener("onOK", eventListener);
			kuotaBerlakuPerGelombang.addEventListener("onClick", eventListener);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(paketJurusanPmb);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			toolbar.setParent(row);

		}

	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<PaketJurusanPmb> paketJurusanPmb = session.createCriteria(PaketJurusanPmb.class)
				.add(Restrictions.eq("paket", paket)).list();

		ListModel strset = new SimpleListModel(paketJurusanPmb);
		grid.setRowRenderer(new DetailPaketRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void displayDetailPaketJurusan(final Paket paket, final Component component, final MyWindow window) {
		this.paket = paket;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();groupbox.setStyle("min-height: 200px;");
		groupbox.appendChild(new MyCaptionStyled("Daftar " + Common.getBahasaConfig("Jurusan")));
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.setVisible(Common.getCurrentUser().ambilDosen() == null);
		button.addEventListener("onClick", new EventListener() {

			private AmbilPaketJurusanPmbHelper ambilPaketJurusanPmbHelper = new AmbilPaketJurusanPmbHelper();

			@Override
			public void onEvent(Event event) throws Exception {

				ambilPaketJurusanPmbHelper.display(paket, getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan 1");
		column.setWidth(paket.getJumlahProdiYgBolehDiambil() > 0 ? "5%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan 2");
		column.setWidth(paket.getJumlahProdiYgBolehDiambil() > 1 ? "5%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan 3");
		column.setWidth(paket.getJumlahProdiYgBolehDiambil() > 2 ? "5%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan 4");
		column.setWidth(paket.getJumlahProdiYgBolehDiambil() > 3 ? "5%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan 5");
		column.setWidth(paket.getJumlahProdiYgBolehDiambil() > 4 ? "5%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelamin");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kuota");
		column.setWidth("90px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Per Gelombang");
		column.setWidth("110px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
		// borderlayout.setParent(component);

	}

}
