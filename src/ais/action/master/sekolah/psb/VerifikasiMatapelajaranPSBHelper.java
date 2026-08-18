package ais.action.master.sekolah.psb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.MatapelajaranSekolah;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.CalonSiswaPunyaVerifikasiMatapelajaran;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.GelombangPendaftaranPsbPunyaMatapelajaran;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyRowStyled;

public class VerifikasiMatapelajaranPSBHelper {

	public static Rows tampilkanVerifikasi(final CalonSiswa calonSiswa, Rows rows, final Combobox gelombang)
			throws Exception {
		GelombangPendaftaranPsb gel = (GelombangPendaftaranPsb) (calonSiswa.getGelombangPendaftaranPsb() != null
				? calonSiswa.getGelombangPendaftaranPsb()
				: (gelombang.getSelectedItem() == null ? null : gelombang.getSelectedItem().getValue()));

		return tampilkanVerifikasi(calonSiswa, rows, gel, gelombang);
	}

	@SuppressWarnings("deprecation")
	public static Rows tampilkanVerifikasi(final CalonSiswa calonSiswa, Rows rows, final GelombangPendaftaranPsb gel,
			final Combobox gelombang) throws Exception {
		final Rows subRowsMatapelajaranSekolah = new Rows();

		if (gel == null) {
			return subRowsMatapelajaranSekolah;
		}

		final Row rowBerkas = new MyRowStyled();
		rowBerkas.setVisible(false);
		rowBerkas.setParent(rows);
		rowBerkas.appendChild(new ais.ui.util.MyLabelBold("- Verifikasi Nilai Rapor"));

		final Row rowVerifikasi = new MyRowStyled();
		ais.ui.util.ZkCompat.setSpans(rowVerifikasi, "2");
		rowVerifikasi.setParent(rows);
		final MyGrid subGrid = new MyGrid();
		subGrid.setSclass("dgrid");
		rowVerifikasi.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);

		Column c = new Column("Verifikasi Matapelajaran");
		subColumns.appendChild(c);
		c.setWidth("20%");
		subColumns.appendChild(c);

		c = new Column("Keterangan Nilai Rapor");
		subColumns.appendChild(c);

		if (gel != null) {
			for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
				if (!nilaikelas.trim().isEmpty()) {

					c = new Column();
					c.appendChild(new Label(ais.common.Common.getBahasaConfig("KKM")));
					subColumns.appendChild(c);
					c.setWidth("5%");

					String[] ca = StringUtils.split(nilaikelas, ":");
					String kel = ca.length > 0 ? ca[0] : "";
					String sem = ca.length > 1 ? ca[1] : "";
					Label label = new Label("Kls:" + kel + (sem.isEmpty() ? "" : ", Smt:" + sem));
					label.setMultiline(true);
					c = new Column();
					c.appendChild(label);
					subColumns.appendChild(c);
					c.setWidth("10%");
				}
			}

			c = new Column();
			c.appendChild(new Label(ais.common.Common.getBahasaConfig("Rata2")));
			subColumns.appendChild(c);
			c.setWidth("5%");
		}

		subRowsMatapelajaranSekolah.setParent(subGrid);

		EventListener eventListenerBerkas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(subRowsMatapelajaranSekolah);

				if (gel != null) {

					Session session = HibernateUtil.currentSession();
					Tbmuser tbmuser = Common.getCurrentUser();

					@SuppressWarnings("unchecked")
					final List<MatapelajaranSekolah> matapelajaranSekolahs = ConstantValues
							.simpleList(
									session.createCriteria(GelombangPendaftaranPsbPunyaMatapelajaran.class)
											.setProjection(Projections.property("matapelajaranSekolah.id"))
											.createAlias("matapelajaranSekolah", "matapelajaranSekolah")
											.add(Restrictions.eq("gelombangPendaftaranPsb", gel))
											.add(Restrictions.eq("matapelajaranSekolah.aktif", true))
											.addOrder(Order.asc("matapelajaranSekolah.nama")),
									MatapelajaranSekolah.class, false);
					rowVerifikasi.setVisible(!matapelajaranSekolahs.isEmpty());
					rowBerkas.setVisible(!matapelajaranSekolahs.isEmpty());

					final MyLabelBoldAja rataMatapelajaran = new MyLabelBoldAja();
					final Map<String, MyLabelBoldAja> nilaiSemuaTotal = new HashMap<String, MyLabelBoldAja>();
					final Map<String, MyLabelBoldAja> kkmSemuaTotal = new HashMap<String, MyLabelBoldAja>();
					final Map<String, MyDoublebox> nilaiSemuaTotalVerikal = new HashMap<String, MyDoublebox>();
					final Map<String, MyDoublebox> kkmSemuaTotalVerikal = new HashMap<String, MyDoublebox>();

					for (final MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {

						CalonSiswaPunyaVerifikasiMatapelajaran calonSiswaPunyaVerifikasiMatapelajaranTemp = (CalonSiswaPunyaVerifikasiMatapelajaran) session
								.createCriteria(CalonSiswaPunyaVerifikasiMatapelajaran.class)
								.add(Restrictions.eq("matapelajaranSekolah", matapelajaranSekolah))
								.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

						if (calonSiswaPunyaVerifikasiMatapelajaranTemp == null) {
							calonSiswaPunyaVerifikasiMatapelajaranTemp = new CalonSiswaPunyaVerifikasiMatapelajaran();
							calonSiswaPunyaVerifikasiMatapelajaranTemp.setCalonSiswa(calonSiswa);
							calonSiswaPunyaVerifikasiMatapelajaranTemp.setMatapelajaranSekolah(matapelajaranSekolah);
							Common.refreshSaveOrUpdate(session, calonSiswaPunyaVerifikasiMatapelajaranTemp);
						}

						final CalonSiswaPunyaVerifikasiMatapelajaran calonSiswaPunyaVerifikasiMatapelajaran = calonSiswaPunyaVerifikasiMatapelajaranTemp;

						final Row subRow = new MyRowStyled();
						subRow.setAttribute("matapelajaranSekolah", matapelajaranSekolah);
						subRow.setAttribute("calonSiswaPunyaVerifikasiMatapelajaran",
								calonSiswaPunyaVerifikasiMatapelajaran);
						subRow.setParent(subRowsMatapelajaranSekolah);
						subRow.setValign("top");

						new Label(matapelajaranSekolah.getNama()).setParent(subRow);

						if (tbmuser != null && tbmuser.getCalonSiswa() == null) {

							final Textbox keterangan = new Textbox(calonSiswaPunyaVerifikasiMatapelajaran == null ? ""
									: calonSiswaPunyaVerifikasiMatapelajaran.getKeterangan());
							keterangan.setWidth("90%");
							keterangan.setRows(2);
							keterangan.setParent(subRow);
							keterangan.addEventListener("onChange", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									calonSiswaPunyaVerifikasiMatapelajaran.setKeterangan(keterangan.getValue());
									Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiMatapelajaran);
								}
							});
						} else {

							new MyLabelAgakKecil(calonSiswaPunyaVerifikasiMatapelajaran.getKeterangan())
									.setParent(subRow);

						}

						final MyLabelBoldAja rata = new MyLabelBoldAja(Common.numberFormat.get()
								.format(calonSiswaPunyaVerifikasiMatapelajaran.ambilNilai("RataRata")));

						final List<MyDoublebox> nilaiSemua = new ArrayList<MyDoublebox>();
						final List<MyDoublebox> kkmSemua = new ArrayList<MyDoublebox>();

						for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
							if (!nilaikelas.trim().isEmpty()) {
								final MyDoublebox nilai = new MyDoublebox(
										calonSiswaPunyaVerifikasiMatapelajaran.ambilNilai(nilaikelas.trim()));
								final MyDoublebox kkm = new MyDoublebox(
										calonSiswaPunyaVerifikasiMatapelajaran.ambilKkm(nilaikelas.trim()));

								final EventListener eventListenerRata = new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Double total = 0.0;
										Double jumlah = 0.0;
										for (MyDoublebox doublebox : nilaiSemua) {
											Double d = doublebox.getValue() == null ? 0.0 : doublebox.getValue();
											if (d > 0.1) {
												total += d;
												jumlah++;
											}
										}
										Double r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;
										rata.setValue(Common.numberFormat.get().format(r));

										Double totalKkm = 0.0;
										Double jumlahKkm = 0.0;
										for (MyDoublebox doublebox : kkmSemua) {
											Double d = doublebox.getValue() == null ? 0.0 : doublebox.getValue();
											if (d > 0.1) {
												totalKkm += d;
												jumlahKkm++;
											}
										}
										Double rKkm = jumlahKkm.intValue() == 0 ? 0.0 : totalKkm / jumlahKkm;

										String labeldata = (String) nilai.getAttribute("labeldata");
										Checkbox checkbox = (Checkbox) nilai.getAttribute("checkbox");
										calonSiswaPunyaVerifikasiMatapelajaran.masukkanNilai(labeldata,
												checkbox.isChecked(), nilai.getValue(), kkm.getValue());
										calonSiswaPunyaVerifikasiMatapelajaran.masukkanNilai("RataRata", true, r, rKkm);
										Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiMatapelajaran);

										MyLabelBoldAja nilaiRataRataBawah = nilaiSemuaTotal.get(labeldata);
										if (nilaiRataRataBawah != null) {
											CalonSiswaPunyaVerifikasiMatapelajaran calonSiswaPunyaVerifikasiMatapelajaranRata = (CalonSiswaPunyaVerifikasiMatapelajaran) nilaiRataRataBawah
													.getAttribute("calonSiswaPunyaVerifikasiMatapelajaran");
											if (calonSiswaPunyaVerifikasiMatapelajaranRata != null) {
												total = 0.0;
												jumlah = 0.0;
												for (final MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {
													MyDoublebox nilaiLagi = nilaiSemuaTotalVerikal.get(
															matapelajaranSekolah.getId() + "--" + labeldata.trim());

													Double d = nilaiLagi.getValue() == null ? 0.0
															: nilaiLagi.getValue();
													if (d > 0.1) {
														total += d;
														jumlah++;
													}
												}

												r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;
												nilaiRataRataBawah.setValue(Common.numberFormat.get().format(r));

												totalKkm = 0.0;
												jumlahKkm = 0.0;
												for (final MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {
													MyDoublebox nilaiLagi = kkmSemuaTotalVerikal.get(
															matapelajaranSekolah.getId() + "--" + labeldata.trim());

													Double d = nilaiLagi.getValue() == null ? 0.0
															: nilaiLagi.getValue();
													if (d > 0.1) {
														totalKkm += d;
														jumlahKkm++;
													}
												}

												rKkm = jumlahKkm.intValue() == 0 ? 0.0 : totalKkm / jumlahKkm;

												calonSiswaPunyaVerifikasiMatapelajaranRata.masukkanNilai(labeldata,
														true, r, rKkm);

												total = 0.0;
												jumlah = 0.0;
												for (MyLabelBoldAja data : nilaiSemuaTotal.values()) {
													try {
														Double d = data.getValue() == null ? 0.0
																: Common.numberFormat.get().parse(data.getValue())
																		.doubleValue();
														if (d > 0.1) {
															total += d;
															jumlah++;
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/VerifikasiMatapelajaranPSBHelper.java:291");
														// TODO: handle exception
													}
												}
												r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;
												rataMatapelajaran.setValue(Common.numberFormat.get().format(r));

												totalKkm = 0.0;
												jumlahKkm = 0.0;
												for (MyLabelBoldAja data : kkmSemuaTotal.values()) {
													try {
														Double d = data.getValue() == null ? 0.0
																: Common.numberFormat.get().parse(data.getValue())
																		.doubleValue();
														if (d > 0.1) {
															totalKkm += d;
															jumlahKkm++;
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/VerifikasiMatapelajaranPSBHelper.java:309");
														// TODO: handle exception
													}
												}
												rKkm = jumlahKkm.intValue() == 0 ? 0.0 : totalKkm / jumlahKkm;

												calonSiswaPunyaVerifikasiMatapelajaranRata.masukkanNilai("RataRata",
														true, r, rKkm);
												Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiMatapelajaranRata);
											}
										}
									}
								};

								kkm.setWidth("90%");
								kkm.setParent(subRow);
								kkm.addEventListener("onChange", eventListenerRata);

								Hbox hbox = new Hbox();
								hbox.setParent(subRow);

								nilai.setAttribute("labeldata", nilaikelas.trim());
								nilai.setWidth("90%");
								nilai.setParent(hbox);
								nilai.setDisabled(
										calonSiswaPunyaVerifikasiMatapelajaran.ambilVerifikasi(nilaikelas.trim()));

								nilai.addEventListener("onChange", eventListenerRata);

								kkmSemua.add(kkm);
								nilaiSemua.add(nilai);
								nilaiSemuaTotalVerikal.put(matapelajaranSekolah.getId() + "--" + nilaikelas.trim(),
										nilai);
								kkmSemuaTotalVerikal.put(matapelajaranSekolah.getId() + "--" + nilaikelas.trim(), kkm);

								final Checkbox checkbox = new Checkbox();
								nilai.setAttribute("checkbox", checkbox);
								if (tbmuser != null && tbmuser.getCalonSiswa() == null) {

									checkbox.setChecked(
											calonSiswaPunyaVerifikasiMatapelajaran.ambilVerifikasi(nilaikelas.trim()));
									checkbox.setParent(hbox);
									checkbox.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											nilai.setDisabled(checkbox.isChecked());
											eventListenerRata.onEvent(new Event("", nilai));
										}
									});
								} else {
									new Image(calonSiswaPunyaVerifikasiMatapelajaran.ambilVerifikasi(nilaikelas.trim())
											? "/img/svg/check2-circle.svg"
											: "/img/svg/warning-outline.svg").setParent(hbox);
								}
							}
						}
						subRow.setAttribute("nilaiSemua", nilaiSemua);
						subRow.setAttribute("kkmSemua", kkmSemua);
						rata.setParent(subRow);

					}

					CalonSiswaPunyaVerifikasiMatapelajaran calonSiswaPunyaVerifikasiMatapelajaranTemp = (CalonSiswaPunyaVerifikasiMatapelajaran) session
							.createCriteria(CalonSiswaPunyaVerifikasiMatapelajaran.class)
							.add(Restrictions.isNull("matapelajaranSekolah"))
							.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

					if (calonSiswaPunyaVerifikasiMatapelajaranTemp == null) {
						calonSiswaPunyaVerifikasiMatapelajaranTemp = new CalonSiswaPunyaVerifikasiMatapelajaran();
						calonSiswaPunyaVerifikasiMatapelajaranTemp.setCalonSiswa(calonSiswa);
						calonSiswaPunyaVerifikasiMatapelajaranTemp.setMatapelajaranSekolah(null);
						Common.refreshSaveOrUpdate(session, calonSiswaPunyaVerifikasiMatapelajaranTemp);
					}
					final CalonSiswaPunyaVerifikasiMatapelajaran calonSiswaPunyaVerifikasiMatapelajaran = calonSiswaPunyaVerifikasiMatapelajaranTemp;

					final Row subRow = new MyRowStyled();
					subRow.setAttribute("calonSiswaPunyaVerifikasiMatapelajaran",
							calonSiswaPunyaVerifikasiMatapelajaran);
					subRow.setParent(subRowsMatapelajaranSekolah);
					subRow.setValign("top");

					subRow.appendChild(new MyLabelBoldAja("Rata-Rata"));

					if (tbmuser != null) {

						final Textbox keterangan = new Textbox(calonSiswaPunyaVerifikasiMatapelajaran == null ? ""
								: calonSiswaPunyaVerifikasiMatapelajaran.getKeterangan());
						keterangan.setWidth("90%");
						keterangan.setRows(2);
						keterangan.setParent(subRow);
						keterangan.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								calonSiswaPunyaVerifikasiMatapelajaran.setKeterangan(keterangan.getValue());
								Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiMatapelajaran);
							}
						});
					} else {

						new MyLabelAgakKecil(calonSiswaPunyaVerifikasiMatapelajaran.getKeterangan()).setParent(subRow);

					}

					rataMatapelajaran.setValue(
							Common.numberFormat.get().format(calonSiswaPunyaVerifikasiMatapelajaran.ambilNilai("RataRata")));

					for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
						if (!nilaikelas.trim().isEmpty()) {

							final MyLabelBoldAja kkm = new MyLabelBoldAja(Common.numberFormat.get()
									.format(calonSiswaPunyaVerifikasiMatapelajaran.ambilKkm(nilaikelas.trim())));
							kkm.setAttribute("labeldata", nilaikelas.trim());
							kkm.setAttribute("calonSiswaPunyaVerifikasiMatapelajaran",
									calonSiswaPunyaVerifikasiMatapelajaran);
							kkm.setWidth("90%");
							kkm.setParent(subRow);

							final MyLabelBoldAja nilai = new MyLabelBoldAja(Common.numberFormat.get()
									.format(calonSiswaPunyaVerifikasiMatapelajaran.ambilNilai(nilaikelas.trim())));
							nilai.setAttribute("labeldata", nilaikelas.trim());
							nilai.setAttribute("calonSiswaPunyaVerifikasiMatapelajaran",
									calonSiswaPunyaVerifikasiMatapelajaran);
							nilai.setWidth("90%");
							nilai.setParent(subRow);

							nilaiSemuaTotal.put(nilaikelas.trim(), nilai);
							kkmSemuaTotal.put(nilaikelas.trim(), kkm);
						}
					}
					rataMatapelajaran.setParent(subRow);

				}
			}
		};

		eventListenerBerkas.onEvent(null);
		if (gelombang != null) {
			gelombang.addEventListener("onChange", eventListenerBerkas);
		}

		return subRowsMatapelajaranSekolah;

	}

	@SuppressWarnings("unchecked")
	public static void simpanVerifikasi(CalonSiswa calonSiswa, Rows subRowsMatapelajaranSekolah) {
		if (subRowsMatapelajaranSekolah != null) {
			Tbmuser tbmuser = Common.getCurrentUser();
			GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
			List<Row> rowsVerifikasi = subRowsMatapelajaranSekolah.getChildren();
			for (Row row : rowsVerifikasi) {
				MatapelajaranSekolah matapelajaranSekolah = (MatapelajaranSekolah) row
						.getAttribute("matapelajaranSekolah");

				// FIX akar masalah ClassCastException (KE-5, Label->Doublebox) & NullPointerException
				// (KE-4, iterasi null "nilaiSemua"/"kkmSemua"): Rows ini juga berisi SATU baris ringkasan
				// "Rata-Rata" (murni tampilan, dibangun dari MyLabelBoldAja -- lihat baris ~385-440 di
				// atas) yang TIDAK pernah diberi attribute "matapelajaranSekolah" krn memang bukan baris
				// data mata pelajaran yang perlu disimpan (nilai keteranganya sudah disimpan sendiri via
				// listener onChange terpisah). Loop lama tetap memprosesnya seolah baris data biasa --
				// mengandalkan try/catch di bawah utk menelan exception yg pasti terjadi. Lewati baris
				// ini di sumbernya supaya tidak ada exception yang tercipta sama sekali.
				if (matapelajaranSekolah == null) {
					continue;
				}

				CalonSiswaPunyaVerifikasiMatapelajaran calonSiswaPunyaVerifikasiMatapelajaran = (CalonSiswaPunyaVerifikasiMatapelajaran) row
						.getAttribute("calonSiswaPunyaVerifikasiMatapelajaran");

				if (calonSiswaPunyaVerifikasiMatapelajaran == null) {
					calonSiswaPunyaVerifikasiMatapelajaran = new CalonSiswaPunyaVerifikasiMatapelajaran();
					calonSiswaPunyaVerifikasiMatapelajaran.setCalonSiswa(calonSiswa);
					calonSiswaPunyaVerifikasiMatapelajaran.setMatapelajaranSekolah(matapelajaranSekolah);
				}
				if (row.getChildren().get(1) instanceof Textbox) {
					Textbox keterangan = (Textbox) row.getChildren().get(1);
					calonSiswaPunyaVerifikasiMatapelajaran.setKeterangan(keterangan.getValue());

				}
				int index = 2;
				for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
					if (!nilaikelas.trim().isEmpty()) {

						try {

							Doublebox kkm = (Doublebox) row.getChildren().get(index);
							index++;

							Hbox hbox = (Hbox) row.getChildren().get(index);
							Doublebox nilai = (Doublebox) hbox.getChildren().get(0);
							if (tbmuser != null) {
								Checkbox checkbox = (Checkbox) hbox.getChildren().get(1);
								calonSiswaPunyaVerifikasiMatapelajaran.masukkanNilai(nilaikelas.trim(),
										checkbox.isChecked(), nilai.getValue(), kkm.getValue());
							} else {
								calonSiswaPunyaVerifikasiMatapelajaran.masukkanNilai(nilaikelas.trim(),
										calonSiswaPunyaVerifikasiMatapelajaran.ambilVerifikasi(nilaikelas.trim()),
										nilai.getValue(), kkm.getValue());
							}
							index++;
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/VerifikasiMatapelajaranPSBHelper.java:498");
							// TODO: handle exception
						}

					}
				}
				try {
					List<MyDoublebox> nilaiSemua = (List<MyDoublebox>) row.getAttribute("nilaiSemua");
					Double total = 0.0;
					Double jumlah = 0.0;
					for (MyDoublebox doublebox : nilaiSemua) {
						Double d = doublebox.getValue() == null ? 0.0 : doublebox.getValue();
						if (d > 0.1) {
							total += d;
							jumlah++;
						}
					}
					Double r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;

					List<MyDoublebox> kkmSemua = (List<MyDoublebox>) row.getAttribute("kkmSemua");
					Double totalKkm = 0.0;
					Double jumlahKkm = 0.0;
					for (MyDoublebox doublebox : kkmSemua) {
						Double d = doublebox.getValue() == null ? 0.0 : doublebox.getValue();
						if (d > 0.1) {
							totalKkm += d;
							jumlahKkm++;
						}
					}
					Double rKkm = jumlahKkm.intValue() == 0 ? 0.0 : totalKkm / jumlahKkm;

					calonSiswaPunyaVerifikasiMatapelajaran.masukkanNilai("RataRata", true, r, rKkm);
					Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiMatapelajaran);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/VerifikasiMatapelajaranPSBHelper.java:531");
					// TODO: handle exception
				}
			}
		}
	}

}
