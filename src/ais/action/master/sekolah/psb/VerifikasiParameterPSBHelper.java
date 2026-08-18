package ais.action.master.sekolah.psb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.CalonSiswaPunyaVerifikasiParameter;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa;
import ais.database.model.sekolah.ParameterVerifikasiCalonSiswa;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class VerifikasiParameterPSBHelper {

	@SuppressWarnings("unchecked")
	private static void reloadData(final Rows subRowsParameterVerifikasi, final GelombangPendaftaranPsb gel,
			final CalonSiswa calonSiswa,
			final GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa) {
		Common.clear(subRowsParameterVerifikasi);
		if (gel != null) {

			Session session = HibernateUtil.currentSession();
			Tbmuser tbmuser = Common.getCurrentUser();

			List<CalonSiswaPunyaVerifikasiParameter> calonSiswaPunyaVerifikasiParameters = session
					.createCriteria(CalonSiswaPunyaVerifikasiParameter.class)
					.add(Restrictions.eq("calonSiswa", calonSiswa))
					.add(Restrictions.eq("gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa",
							gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa))
					.addOrder(Order.asc("nama")).list();
			for (final CalonSiswaPunyaVerifikasiParameter calonSiswaPunyaVerifikasiParameter : calonSiswaPunyaVerifikasiParameters) {

				final MyFormRow subRow = new MyFormRow();
				subRow.setAttribute("calonSiswaPunyaVerifikasiParameter", calonSiswaPunyaVerifikasiParameter);
				subRow.setStyle("border:1px;solid;background:transparent;");
				subRow.setParent(subRowsParameterVerifikasi);
				subRow.setValign("top");

				final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setVisible(!calonSiswaPunyaVerifikasiParameter.getVerified());

				final MyDetail myvbox = new MyDetail();
				myvbox.setParent(subRow);

				Vbox a = new Vbox();
				a.setParent(subRow);
				a.setWidth("99%");
				if (tbmuser != null) {

					final Checkbox checkbox = new Checkbox(calonSiswaPunyaVerifikasiParameter.getNama());
					checkbox.setParent(a);
					checkbox.setChecked(calonSiswaPunyaVerifikasiParameter != null
							&& calonSiswaPunyaVerifikasiParameter.getVerified());

					subRow.setAttribute("checkbox", checkbox);

					new Label(calonSiswaPunyaVerifikasiParameter.getParameterVerifikasiCalonSiswa().getNama())
							.setParent(subRow);

					final Textbox keterangan = new Textbox(calonSiswaPunyaVerifikasiParameter == null ? ""
							: calonSiswaPunyaVerifikasiParameter.getKeterangan());
					keterangan.setWidth("90%");
					keterangan.setRows(2);
					keterangan.setParent(subRow);
					keterangan.setDisabled(checkbox.isChecked());

					EventListener listener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							calonSiswaPunyaVerifikasiParameter.setKeterangan(keterangan.getValue());
							calonSiswaPunyaVerifikasiParameter.setVerified(checkbox.isChecked());
							Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiParameter);
							button.setVisible(!checkbox.isChecked());
							keterangan.setDisabled(checkbox.isChecked());

							Common.clear(myvbox);
							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							LampiranLain.createDownloadUploadFileLain(hbox, calonSiswaPunyaVerifikasiParameter.getId(),
									CalonSiswaPunyaVerifikasiParameter.class.getName(), "Bukti", false, null, null,
									false, false, false, !checkbox.isChecked());
							new ais.ui.util.MyHtml("<hr>").setParent(myvbox);
						}
					};

					checkbox.addEventListener("onClick", listener);
					keterangan.addEventListener("onChange", listener);

				} else {

					new Label(calonSiswaPunyaVerifikasiParameter.getNama()).setParent(a);

					new Label(calonSiswaPunyaVerifikasiParameter.getParameterVerifikasiCalonSiswa().getNama())
							.setParent(subRow);

					new Label(calonSiswaPunyaVerifikasiParameter.getKeterangan()).setParent(subRow);

				}

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);

				LampiranLain.createDownloadUploadFileLain(hbox, calonSiswaPunyaVerifikasiParameter.getId(),
						CalonSiswaPunyaVerifikasiParameter.class.getName(), "Bukti", false, null, null, false, false,
						false, !calonSiswaPunyaVerifikasiParameter.getVerified());

				new ais.ui.util.MyHtml("<hr>").setParent(myvbox);
				myvbox.setOpen(true);

				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Common.refreshDelete(calonSiswaPunyaVerifikasiParameter);
												reloadData(subRowsParameterVerifikasi, gel, calonSiswa,
														gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});
				button.setParent(subRow);
			}

		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public static List<Rows> tampilkanVerifikasi(final CalonSiswa calonSiswa, Rows rows,
			final Combobox gelombangPendaftaranPsb, final GelombangPendaftaranPsb p) throws Exception {

		List<Rows> rowsVerifikasi = new ArrayList<Rows>();

		final GelombangPendaftaranPsb gel = p != null ? p
				: (GelombangPendaftaranPsb) (gelombangPendaftaranPsb == null
						|| gelombangPendaftaranPsb.getSelectedItem() == null ? null
								: gelombangPendaftaranPsb.getSelectedItem().getValue());
		if (gel != null) {
			List<GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa> punyaParameterVerifikasiCalonSiswas = HibernateUtil
					.currentSession().createCriteria(GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.class)
					.add(Restrictions.eq("gelombangPendaftaranPsb", gel)).list();

			for (final GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa : punyaParameterVerifikasiCalonSiswas) {

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				row.appendChild(new ais.ui.util.MyHtml("<hr>"));

				final MyFormRow rowBerkas = new MyFormRow();
				rowBerkas.setVisible(false);
				rowBerkas.setStyle("border:0px;background: transparent;");
				rowBerkas.setParent(rows);
				rowBerkas.appendChild(new ais.ui.util.MyLabelBold(
						gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getJudul()));

				final MyFormRow rowTambah = new MyFormRow();
				rowTambah.setStyle("border:0px;background: transparent;");
				ais.ui.util.ZkCompat.setSpans(rowTambah, "2");
				rowTambah.setParent(rows);
				Button tambah = new Button(
						"Tambah " + gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getNama(),
						"/img/new.gif");
				rowTambah.appendChild(tambah);

				final MyFormRow rowVerifikasi = new MyFormRow();
				ais.ui.util.ZkCompat.setSpans(rowVerifikasi, "2");
				rowVerifikasi.setStyle("border:0px;background: transparent;");
				rowVerifikasi.setParent(rows);
				final MyGrid subGrid = new MyGrid();
				rowVerifikasi.appendChild(subGrid);

				Columns subColumns = new Columns();
				subColumns.setParent(subGrid);

				Column c = new Column("");
				c.setWidth("0%");
				subColumns.appendChild(c);
				c = new Column(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getNama());
				subColumns.appendChild(c);
				c.setWidth("50%");

				c = new Column("Tingkat");
				subColumns.appendChild(c);
				c.setWidth("20%");

				subColumns.appendChild(c);
				c = new Column("Keterangan");
				subColumns.appendChild(c);

				c = new Column("");
				subColumns.appendChild(c);
				c.setWidth("5%");

				final Rows subRowsParameterVerifikasi = new Rows();
				subRowsParameterVerifikasi.setParent(subGrid);

				final EventListener eventListenerBerkas = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						reloadData(subRowsParameterVerifikasi, gel, calonSiswa,
								gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);

					}
				};

				eventListenerBerkas.onEvent(null);

				rowsVerifikasi.add(subRowsParameterVerifikasi);

				// if (gelombangPendaftaranPsb != null) {
				// gelombangPendaftaranPsb.addEventListener("onChange",
				// eventListenerBerkas);
				// }

				tambah.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						final MyWindow addWindow = new MyWindow();
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
						addWindow.setHeight("250px");
						addWindow.setWidth("600px");
						addWindow.setTitle(
								"Tambah data " + gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getNama());
						Common.clear(addWindow);
						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(center);
						grid.setWidth("100%");
						grid.setHeight("100%");

						Rows rows = new Rows();
						rows.setParent(grid);

						MyFormRow row = new MyFormRow();row.setValign("top");
						row.setParent(rows);
						row.appendChild(new ais.ui.util.MyLabelConfig(
								gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getNama() + " *"));
						final Textbox nama;
						row.appendChild(nama = new Textbox());
						nama.setWidth("90%");
						nama.setRows(5);

						HibernateUtil.currentSession()
								.refresh(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);
						List<ParameterVerifikasiCalonSiswa> selectedParameterVerifikasiCalonSiswa = new ArrayList<ParameterVerifikasiCalonSiswa>(
								gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa
										.getParameterVerifikasiCalonSiswas());

						Collections.sort(selectedParameterVerifikasiCalonSiswa);

						row = new MyFormRow();
						row.setParent(rows);
						row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat *"));
						final Combobox tingkat = new Combobox();
						Common.insertComboItems(tingkat, "nama", selectedParameterVerifikasiCalonSiswa);
						tingkat.setWidth("90%");
						row.appendChild(tingkat);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						// toolbar.setHeight("25px");
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								addWindow.detach();
							}
						});
						cancel.setParent(toolbar);
						MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
						save.setTooltiptext("Simpan");
						save.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {

								if (nama.getValue().trim().isEmpty()) {
									MyMessageboxConfig.show(
											gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getNama()
													+ " harus diisi",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									return;
								}
								if (tingkat.getSelectedItem() == null) {
									MyMessageboxConfig.show("Tingkat harus dipilih", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									return;
								}

								CalonSiswaPunyaVerifikasiParameter calonSiswaPunyaVerifikasiParameter = new CalonSiswaPunyaVerifikasiParameter();
								calonSiswaPunyaVerifikasiParameter.setCalonSiswa(calonSiswa);
								calonSiswaPunyaVerifikasiParameter.setNama(nama.getValue().trim());
								calonSiswaPunyaVerifikasiParameter
										.setGelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa(
												gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);
								calonSiswaPunyaVerifikasiParameter.setParameterVerifikasiCalonSiswa(
										(ParameterVerifikasiCalonSiswa) tingkat.getSelectedItem().getValue());

								Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiParameter);

								eventListenerBerkas.onEvent(event);
								addWindow.detach();
							}
						});
						save.setParent(toolbar);
						borderlayout.setParent(addWindow);
						addWindow.onModal();
					}
				});

			}
		}

		return rowsVerifikasi;

	}

	@SuppressWarnings("unchecked")
	public static void simpanVerifikasi(CalonSiswa calonSiswa, List<Rows> rows) {
		if (rows != null) {
			for (Rows subRowsMatapelajaranSekolah : rows) {
				if (subRowsMatapelajaranSekolah != null) {

					List<Row> rowsVerifikasi = subRowsMatapelajaranSekolah.getChildren();
					for (Row row : rowsVerifikasi) {
						CalonSiswaPunyaVerifikasiParameter calonSiswaPunyaVerifikasiParameter = (CalonSiswaPunyaVerifikasiParameter) row
								.getAttribute("calonSiswaPunyaVerifikasiParameter");

						if ((row.getChildren().get(2) instanceof Textbox)) {
							Checkbox verified = (Checkbox) row.getAttribute("checkbox");
							Textbox keterangan = (Textbox) row.getChildren().get(2);
							calonSiswaPunyaVerifikasiParameter.setVerified(verified.isChecked());
							calonSiswaPunyaVerifikasiParameter.setKeterangan(keterangan.getValue());
							Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiParameter);

						}

					}
				}
			}
		}
	}

}
