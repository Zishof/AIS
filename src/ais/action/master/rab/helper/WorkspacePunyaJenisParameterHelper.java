package ais.action.master.rab.helper;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.JenisParameter;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaJenisParameter;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDiv;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK untuk mengelola parameter tambahan bertipe-dinamis ({@link WorkspacePunyaJenisParameter})
 * milik satu {@link Workspace} pada modul RAB (relasi "punya banyak"). Setiap baris memilih satu
 * {@link JenisParameter} (yang menentukan tipe data nilainya: String/Integer/Double/Date/Time) lalu
 * menampilkan HANYA satu input yang sesuai tipe tersebut (input lain disembunyikan, bukan dihapus,
 * lewat toggle visibilitas di {@link #initRow}) — pola "polymorphic value" satu baris menyimpan
 * nilai ke salah satu dari lima kolom berbeda tergantung tipe data parameter yang dipilih. Baris
 * baru langsung ditambahkan ke grid tanpa disimpan ke database (baru tersimpan saat workspace induk
 * disimpan); tombol tambah/hapus disembunyikan sesuai hak akses
 * {@link CommonPrivilages#CREATE}/{@code DELETE}, dan seluruh input dinonaktifkan bila tidak punya
 * hak {@link CommonPrivilages#UPDATE}.
 */
public class WorkspacePunyaJenisParameterHelper {

	private MyGrid gridParameter;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	/**
	 * Membuat helper terikat pada satu komponen grid target, sekaligus mengevaluasi hak akses
	 * tambah, ubah, dan hapus pengguna saat ini.
	 *
	 * @param gridParameter komponen grid ZK tempat baris parameter dirender
	 */
	public WorkspacePunyaJenisParameterHelper(MyGrid gridParameter) {
		this.gridParameter = gridParameter;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun kerangka layout detail (toolbar tambah + kolom grid) dan langsung memuat data
	 * parameter untuk workspace yang diberikan. Tombol tambah langsung menyisipkan baris kosong baru
	 * ke grid (belum tersimpan ke database).
	 *
	 * @param workspace workspace yang daftar parameternya ditampilkan/dikelola
	 * @return komponen {@link Borderlayout} berisi toolbar dan grid parameter yang siap dipasang ke layar pemanggil
	 */
	public Borderlayout initDetail(final Workspace workspace) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Parameter",
				"/img/new.gif");
		add.setVisible(WorkspacePunyaJenisParameterHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				WorkspacePunyaJenisParameter workspacePunyaJenisParameter = new WorkspacePunyaJenisParameter();
				workspacePunyaJenisParameter.setWorkspace(workspace);

				Rows rows = gridParameter.getRows() == null ? new Rows()
						: gridParameter.getRows();
				rows.setParent(gridParameter);
				Row row = new Row();row.setValign("top");
				row.setParent(rows);
				initRow(row, workspacePunyaJenisParameter);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridParameter);
		gridParameter.setParent(center);
		gridParameter.setWidth("100%");
		gridParameter.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridParameter);

		MyColumnConfig column = new MyColumnConfig("Parameter");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Nilai");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(workspace);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Workspace workspace) {

		List<WorkspacePunyaJenisParameter> workspacePunyaJenisParameters = workspace == null
				|| workspace.getId() == null ? new ArrayList<WorkspacePunyaJenisParameter>()
				: HibernateUtil.currentSession()
						.createCriteria(WorkspacePunyaJenisParameter.class)
						.add(Restrictions.eq("workspace", workspace)).list();

		Rows rows = gridParameter.getRows() == null ? new Rows()
				: gridParameter.getRows();
		rows.setParent(gridParameter);

		for (WorkspacePunyaJenisParameter workspacePunyaJenisParameter : workspacePunyaJenisParameters) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, workspacePunyaJenisParameter);
		}
	}

	/**
	 * Mengisi satu baris grid dengan kombo pemilihan jenis parameter, lima input nilai bertipe
	 * berbeda (String/Integer/Double/Date/Time) yang saling eksklusif — hanya satu yang tampil sesuai
	 * tipe data jenis parameter terpilih, ditentukan ulang setiap kali pilihan jenis parameter
	 * berubah — kolom keterangan, dan tombol hapus beserta event handler-nya (dialog konfirmasi,
	 * hapus dari database dan dari grid bila dikonfirmasi).
	 *
	 * @param row                          baris grid yang diisi
	 * @param workspacePunyaJenisParameter baris penghubung workspace-jenis parameter yang direpresentasikan baris ini
	 */
	public void initRow(final Row row,
			final WorkspacePunyaJenisParameter workspacePunyaJenisParameter) {
		row.setValign("top");row.setAttribute("workspacePunyaJenisParameter",
				workspacePunyaJenisParameter);

		// final RealisasiWorkspacePunyaJenisParameterHelper
		// workspacePunyaJenisParameterHelper = new
		// RealisasiWorkspacePunyaJenisParameterHelper(
		// workspacePunyaJenisParameter);
		// workspacePunyaJenisParameterHelper.setParent(row);

		final Combobox jenisParameter = new Combobox();
		jenisParameter.setDisabled(!edit);
		jenisParameter.setWidth("90%");
		jenisParameter.setParent(row);

		Common.insertCombo(jenisParameter, "nama", "typedata",
				JenisParameter.class);
		Common.selectComboItem(jenisParameter,
				workspacePunyaJenisParameter.getJenisParameter());

		final Textbox jenisParameterValue = new Textbox(
				workspacePunyaJenisParameter.getJenisParameterValue());

		final MyIntbox jenisParameterValueInteger = new MyIntbox(
				workspacePunyaJenisParameter.getJenisParameterValueInteger() == null ? 0
						: workspacePunyaJenisParameter
								.getJenisParameterValueInteger());

		final MyDoublebox jenisParameterValueDouble = new MyDoublebox(
				workspacePunyaJenisParameter.getJenisParameterValueDouble() == null ? 0.0
						: workspacePunyaJenisParameter
								.getJenisParameterValueDouble());

		final MyDatebox jenisParameterValueDate = new MyDatebox(
				workspacePunyaJenisParameter.getJenisParameterValueDate());

		final Timebox jenisParameterValueTime = new ais.ui.util.MyTimebox(
				workspacePunyaJenisParameter.getJenisParameterValueTime());

		jenisParameterValue.setVisible(false);
		jenisParameterValue.setDisabled(!edit);
		jenisParameterValue.setWidth("90%");
		jenisParameterValue.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				workspacePunyaJenisParameter
						.setJenisParameterValue(jenisParameterValue.getValue());
				row.setValign("top");row.setAttribute("workspacePunyaJenisParameter",
						workspacePunyaJenisParameter);
			}
		});

		jenisParameterValueInteger.setVisible(false);
		jenisParameterValueInteger.setDisabled(!edit);
		jenisParameterValueInteger.setWidth("90%");
		jenisParameterValueInteger.addEventListener("onChange",
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						workspacePunyaJenisParameter
								.setJenisParameterValueInteger(jenisParameterValueInteger
										.getValue());
						row.setValign("top");row.setAttribute("workspacePunyaJenisParameter",
								workspacePunyaJenisParameter);
					}
				});

		jenisParameterValueDouble.setVisible(false);
		jenisParameterValueDouble.setDisabled(!edit);
		jenisParameterValueDouble.setWidth("90%");
		jenisParameterValueDouble.addEventListener("onChange",
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						workspacePunyaJenisParameter
								.setJenisParameterValueDouble(jenisParameterValueDouble
										.getValue());
						row.setValign("top");row.setAttribute("workspacePunyaJenisParameter",
								workspacePunyaJenisParameter);
					}
				});

		jenisParameterValueDate.setVisible(false);
		jenisParameterValueDate.setDisabled(!edit);
		jenisParameterValueDate.addEventListener("onChange",
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						workspacePunyaJenisParameter
								.setJenisParameterValueDate(jenisParameterValueDate
										.getValue());
						row.setValign("top");row.setAttribute("workspacePunyaJenisParameter",
								workspacePunyaJenisParameter);
					}
				});

		jenisParameterValueTime.setVisible(false);
		jenisParameterValueTime.setDisabled(!edit);
		jenisParameterValueTime.addEventListener("onChange",
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						workspacePunyaJenisParameter
								.setJenisParameterValueTime(jenisParameterValueTime
										.getValue());
						row.setValign("top");row.setAttribute("workspacePunyaJenisParameter",
								workspacePunyaJenisParameter);
					}
				});

		// Vbox vbox = new Vbox(new Component[] { jenisParameterValue,
		// jenisParameterValueInteger, jenisParameterValueDouble,
		// jenisParameterValueDate, jenisParameterValueTime });
		MyDiv vbox = new MyDiv();
		vbox.setStyle("border:none;");
		vbox.appendChild(jenisParameterValue);
		vbox.appendChild(jenisParameterValueInteger);
		vbox.appendChild(jenisParameterValueDouble);
		vbox.appendChild(jenisParameterValueDate);
		vbox.appendChild(jenisParameterValueTime);

		vbox.setWidth("90%");
		vbox.setParent(row);

		final Textbox keterangan = new Textbox(
				workspacePunyaJenisParameter.getKeterangan());
		keterangan.setDisabled(!edit);
		keterangan.setWidth("90%");
		keterangan.setParent(row);
		keterangan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				workspacePunyaJenisParameter.setKeterangan(keterangan
						.getValue());
				row.setValign("top");row.setAttribute("workspacePunyaJenisParameter",
						workspacePunyaJenisParameter);
			}
		});

		jenisParameter.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jenisParameter.getSelectedItem() == null) {
					MyMessageboxConfig.show("Parameter harus dipilih", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				JenisParameter myJenisParameter = (JenisParameter) (jenisParameter
						.getSelectedItem() == null ? null : jenisParameter
						.getSelectedItem().getValue());

				if (myJenisParameter.getTypedata() == null) {
					MyMessageboxConfig
							.show("Parameter type data harus dipilih",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
					return;
				}

				workspacePunyaJenisParameter
						.setJenisParameter(myJenisParameter);
				row.setValign("top");row.setAttribute("workspacePunyaJenisParameter",
						workspacePunyaJenisParameter);

				jenisParameterValue.setVisible(myJenisParameter.getTypedata()
						.equals(String.class.getName()));
				jenisParameterValueInteger.setVisible(myJenisParameter
						.getTypedata().equals(Integer.class.getName()));
				jenisParameterValueDouble.setVisible(myJenisParameter
						.getTypedata().equals(Double.class.getName()));
				jenisParameterValueDate.setVisible(myJenisParameter
						.getTypedata().equals(Date.class.getName()));
				jenisParameterValueTime.setVisible(myJenisParameter
						.getTypedata().equals(Time.class.getName()));

			}
		});

		JenisParameter myJenisParameter = (JenisParameter) (jenisParameter
				.getSelectedItem() == null ? null : jenisParameter
				.getSelectedItem().getValue());
		if (myJenisParameter != null) {
			jenisParameterValue.setVisible(myJenisParameter.getTypedata()
					.equals(String.class.getName()));
			jenisParameterValueInteger.setVisible(myJenisParameter
					.getTypedata().equals(Integer.class.getName()));
			jenisParameterValueDouble.setVisible(myJenisParameter.getTypedata()
					.equals(Double.class.getName()));
			jenisParameterValueDate.setVisible(myJenisParameter.getTypedata()
					.equals(Date.class.getName()));
			jenisParameterValueTime.setVisible(myJenisParameter.getTypedata()
					.equals(Time.class.getName()));
		}

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (workspacePunyaJenisParameter.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(workspacePunyaJenisParameter);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
