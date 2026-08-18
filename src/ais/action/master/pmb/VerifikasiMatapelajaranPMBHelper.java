package ais.action.master.pmb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
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
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran;
import ais.database.model.Konfigurasi;
import ais.database.model.MatapelajaranSekolah;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaMatapelajaran;
import ais.database.model.Tbmuser;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyRowStyled;

public class VerifikasiMatapelajaranPMBHelper {

	@SuppressWarnings("deprecation")
	public static Rows tampilkanVerifikasi(final BiodataCalonMahasiswa biodataCalonMahasiswa, final Rows rows,
			final Combobox paket, final Paket p, final Combobox gelombangPendaftaran) throws Exception {
		final Rows[] holder = new Rows[1];
		final List<Component> renderedComponents = new ArrayList<Component>();

		final EventListener reloadAll = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				for (Component component : new ArrayList<Component>(renderedComponents)) {
					try {
						component.detach();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiMatapelajaranPMBHelper.java:58");
					}
				}
				renderedComponents.clear();
				if (rows == null) {
					holder[0] = new Rows();
					return;
				}
				int jumlahSebelum = rows.getChildren() == null ? 0 : rows.getChildren().size();
				holder[0] = VerifikasiMatapelajaranPMBHelper.tampilkanVerifikasi(biodataCalonMahasiswa, rows, paket, p);
				List children = rows.getChildren();
				if (children != null) {
					for (int i = jumlahSebelum; i < children.size(); i++) {
						Object child = children.get(i);
						if (child instanceof Component) {
							renderedComponents.add((Component) child);
						}
					}
				}
			}
		};

		reloadAll.onEvent(null);
		if (gelombangPendaftaran != null) {
			gelombangPendaftaran.addEventListener("onChange", reloadAll);
		}
		return holder[0] == null ? new Rows() : holder[0];
	}

	@SuppressWarnings("deprecation")
	public static Rows tampilkanVerifikasi(final BiodataCalonMahasiswa biodataCalonMahasiswa, Rows rows,
			final Combobox paket, final Paket p) throws Exception {
		final Rows subRowsMatapelajaranSekolah = new Rows();
		final Paket gel = p != null ? p
				: (Paket) (paket == null || paket.getSelectedItem() == null
						? (biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getPaket())
						: paket.getSelectedItem().getValue());

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

		c = new Column();
		c.appendChild(new Label(ais.common.Common.getBahasaConfig("KKM")));
		subColumns.appendChild(c);
		c.setWidth(Common.bolehKonfigurasi("kkm_tampil_di_input_nilai_raport") ? "5%" : "0px");

		subColumns.appendChild(c);
		c = new Column("Keterangan Nilai Rapor");
		subColumns.appendChild(c);

		if (gel != null) {
			for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
				if (!nilaikelas.trim().isEmpty()) {
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
					final List<MatapelajaranSekolah> matapelajaranSekolahs = ConstantValues.simpleList(session
							.createCriteria(PaketPunyaMatapelajaran.class)
							.setProjection(Projections.property("matapelajaranSekolah.id"))
							.createAlias("matapelajaranSekolah", "matapelajaranSekolah")
							.add(Restrictions.eq("paket", gel)).add(Restrictions.eq("matapelajaranSekolah.aktif", true))
							.addOrder(Order.asc("matapelajaranSekolah.nama")), MatapelajaranSekolah.class, false);
					rowVerifikasi.setVisible(!matapelajaranSekolahs.isEmpty());
					rowBerkas.setVisible(!matapelajaranSekolahs.isEmpty());

					final MyLabelBoldAja rataMatapelajaran = new MyLabelBoldAja();
					final Map<String, MyLabelBoldAja> nilaiSemuaTotal = new HashMap<String, MyLabelBoldAja>();
					final Map<String, MyDoublebox> nilaiSemuaTotalVerikal = new HashMap<String, MyDoublebox>();
					for (final MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {

						BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp = (BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran) session
								.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran.class)
								.add(Restrictions.eq("matapelajaranSekolah", matapelajaranSekolah))
								.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa)).setMaxResults(1)
								.uniqueResult();

						if (biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp == null) {
							biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp = new BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran();
							biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp
									.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
							biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp
									.setMatapelajaranSekolah(matapelajaranSekolah);
							Common.refreshSaveOrUpdate(session, biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp);
						}

						final BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran biodataCalonMahasiswaPunyaVerifikasiMatapelajaran = biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp;

						final Row subRow = new MyRowStyled();
						subRow.setAttribute("matapelajaranSekolah", matapelajaranSekolah);
						subRow.setAttribute("biodataCalonMahasiswaPunyaVerifikasiMatapelajaran",
								biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
						subRow.setParent(subRowsMatapelajaranSekolah);
						subRow.setValign("top");

						new Label(matapelajaranSekolah.getNama()).setParent(subRow);

						if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null) {

							final MyDoublebox kkm = new MyDoublebox(
									biodataCalonMahasiswaPunyaVerifikasiMatapelajaran == null ? 0.0
											: biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.getKkm());
							kkm.setWidth("90%");
							kkm.setParent(subRow);
							kkm.addEventListener("onChange", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.setKkm(kkm.getValue());
									Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
								}
							});

							final Textbox keterangan = new Textbox(
									biodataCalonMahasiswaPunyaVerifikasiMatapelajaran == null ? ""
											: biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.getKeterangan());
							keterangan.setWidth("90%");
							keterangan.setRows(2);
							keterangan.setParent(subRow);
							keterangan.addEventListener("onChange", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									biodataCalonMahasiswaPunyaVerifikasiMatapelajaran
											.setKeterangan(keterangan.getValue());
									Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
								}
							});
						} else {
							new MyLabelAgakKecil(Common.numberFormat.get()
									.format(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.getKkm()))
									.setParent(subRow);
							new MyLabelAgakKecil(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.getKeterangan())
									.setParent(subRow);

						}

						final MyLabelBoldAja rata = new MyLabelBoldAja(Common.numberFormat.get()
								.format(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.ambilNilai("RataRata")));

						final List<MyDoublebox> nilaiSemua = new ArrayList<MyDoublebox>();

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

								MyDoublebox nilai = (MyDoublebox) arg0.getTarget();
								String labeldata = (String) nilai.getAttribute("labeldata");
								Checkbox checkbox = (Checkbox) nilai.getAttribute("checkbox");
								biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.masukkanNilai(labeldata,
										checkbox.isChecked(), nilai.getValue());
								biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.masukkanNilai("RataRata", true, r);
								Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);

								MyLabelBoldAja nilaiRataRataBawah = nilaiSemuaTotal.get(labeldata);
								if (nilaiRataRataBawah != null) {
									BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran biodataCalonMahasiswaPunyaVerifikasiMatapelajaranRata = (BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran) nilaiRataRataBawah
											.getAttribute("biodataCalonMahasiswaPunyaVerifikasiMatapelajaran");
									if (biodataCalonMahasiswaPunyaVerifikasiMatapelajaranRata != null) {
										total = 0.0;
										jumlah = 0.0;
										for (final MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {
											MyDoublebox nilaiLagi = nilaiSemuaTotalVerikal
													.get(matapelajaranSekolah.getId() + "--" + labeldata.trim());

											Double d = nilaiLagi.getValue() == null ? 0.0 : nilaiLagi.getValue();
											if (d > 0.1) {
												total += d;
												jumlah++;
											}
										}

										r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;
										nilaiRataRataBawah.setValue(Common.numberFormat.get().format(r));

										biodataCalonMahasiswaPunyaVerifikasiMatapelajaranRata.masukkanNilai(labeldata,
												true, r);

										total = 0.0;
										jumlah = 0.0;
										for (MyLabelBoldAja data : nilaiSemuaTotal.values()) {
											try {
												Double d = data.getValue() == null ? 0.0
														: Common.numberFormat.get().parse(data.getValue()).doubleValue();
												if (d > 0.1) {
													total += d;
													jumlah++;
												}
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiMatapelajaranPMBHelper.java:307");
												// TODO: handle exception
											}
										}
										r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;
										rataMatapelajaran.setValue(Common.numberFormat.get().format(r));
										biodataCalonMahasiswaPunyaVerifikasiMatapelajaranRata.masukkanNilai("RataRata",
												true, r);
										Common.refreshSaveOrUpdate(
												biodataCalonMahasiswaPunyaVerifikasiMatapelajaranRata);
									}
								}
							}
						};

						for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
							if (!nilaikelas.trim().isEmpty()) {
								Hbox hbox = new Hbox();
								hbox.setParent(subRow);

								final MyDoublebox nilai = new MyDoublebox(
										biodataCalonMahasiswaPunyaVerifikasiMatapelajaran
												.ambilNilai(nilaikelas.trim()));
								nilai.setAttribute("labeldata", nilaikelas.trim());
								nilai.setWidth("90%");
								nilai.setParent(hbox);
								nilai.setDisabled(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran
										.ambilVerifikasi(nilaikelas.trim()));

								nilai.addEventListener("onChange", eventListenerRata);

								nilaiSemua.add(nilai);
								nilaiSemuaTotalVerikal.put(matapelajaranSekolah.getId() + "--" + nilaikelas.trim(),
										nilai);

								final Checkbox checkbox = new Checkbox();
								nilai.setAttribute("checkbox", checkbox);
								if (tbmuser != null) {

									checkbox.setChecked(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran
											.ambilVerifikasi(nilaikelas.trim()));
									checkbox.setParent(hbox);
									checkbox.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											nilai.setDisabled(checkbox.isChecked());
											eventListenerRata.onEvent(new Event("", nilai));
										}
									});
								} else {
									new Image(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran
											.ambilVerifikasi(nilaikelas.trim()) ? "/img/svg/check2-circle.svg"
													: "/img/svg/warning-outline.svg")
											.setParent(hbox);
								}
							}
						}
						subRow.setAttribute("nilaiSemua", nilaiSemua);
						rata.setParent(subRow);

					}

					BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp = (BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran) session
							.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran.class)
							.add(Restrictions.isNull("matapelajaranSekolah"))
							.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa)).setMaxResults(1)
							.uniqueResult();

					if (biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp == null) {
						biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp = new BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran();
						biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp
								.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
						biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp.setMatapelajaranSekolah(null);
						Common.refreshSaveOrUpdate(session, biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp);
					}
					final BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran biodataCalonMahasiswaPunyaVerifikasiMatapelajaran = biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp;

					final Row subRow = new MyRowStyled();
					subRow.setAttribute("biodataCalonMahasiswaPunyaVerifikasiMatapelajaran",
							biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
					subRow.setParent(subRowsMatapelajaranSekolah);
					subRow.setValign("top");

					subRow.appendChild(new MyLabelBoldAja("Rata-Rata"));

					if (tbmuser != null) {

						final MyDoublebox kkm = new MyDoublebox(
								biodataCalonMahasiswaPunyaVerifikasiMatapelajaran == null ? 0.0
										: biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.getKkm());
						kkm.setWidth("90%");
						kkm.setParent(subRow);
						kkm.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.setKkm(kkm.getValue());
								Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
							}
						});

						final Textbox keterangan = new Textbox(
								biodataCalonMahasiswaPunyaVerifikasiMatapelajaran == null ? ""
										: biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.getKeterangan());
						keterangan.setWidth("90%");
						keterangan.setRows(2);
						keterangan.setParent(subRow);
						keterangan.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.setKeterangan(keterangan.getValue());
								Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
							}
						});
					} else {
						new MyLabelAgakKecil(
								Common.numberFormat.get().format(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.getKkm()))
								.setParent(subRow);
						new MyLabelAgakKecil(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.getKeterangan())
								.setParent(subRow);

					}

					rataMatapelajaran.setValue(Common.numberFormat.get()
							.format(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.ambilNilai("RataRata")));

					for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
						if (!nilaikelas.trim().isEmpty()) {

							final MyLabelBoldAja nilai = new MyLabelBoldAja(Common.numberFormat.get().format(
									biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.ambilNilai(nilaikelas.trim())));
							nilai.setAttribute("labeldata", nilaikelas.trim());
							nilai.setAttribute("biodataCalonMahasiswaPunyaVerifikasiMatapelajaran",
									biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
							nilai.setWidth("90%");
							nilai.setParent(subRow);

							nilaiSemuaTotal.put(nilaikelas.trim(), nilai);

						}
					}
					rataMatapelajaran.setParent(subRow);

				}
			}
		};

		eventListenerBerkas.onEvent(null);
		if (paket != null) {
			paket.addEventListener("onChange", eventListenerBerkas);
		}

		return subRowsMatapelajaranSekolah;

	}

	@SuppressWarnings("unchecked")
	public static void simpanVerifikasi(BiodataCalonMahasiswa biodataCalonMahasiswa, Rows subRowsMatapelajaranSekolah) {
		if (subRowsMatapelajaranSekolah != null) {
			Tbmuser tbmuser = Common.getCurrentUser();
			Paket gel = biodataCalonMahasiswa.getPaket();
			List<Row> rowsVerifikasi = subRowsMatapelajaranSekolah.getChildren();
			for (Row row : rowsVerifikasi) {
				MatapelajaranSekolah matapelajaranSekolah = (MatapelajaranSekolah) row
						.getAttribute("matapelajaranSekolah");
				BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran biodataCalonMahasiswaPunyaVerifikasiMatapelajaran = (BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran) row
						.getAttribute("biodataCalonMahasiswaPunyaVerifikasiMatapelajaran");

				if (biodataCalonMahasiswaPunyaVerifikasiMatapelajaran == null) {
					biodataCalonMahasiswaPunyaVerifikasiMatapelajaran = new BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran();
					biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
					biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.setMatapelajaranSekolah(matapelajaranSekolah);
				}

				// BUG FIX (ClassCastException Textbox->Hbox @get(1)/get(index) & NPE
				// nilaiSemua null): struktur child subRow yg dibangun tampilkanVerifikasi()
				// SEBENARNYA: [0]=Label nama (khusus row matapelajaran; row "Rata-Rata"
				// pakai MyLabelBoldAja di posisi sama), [1]=komponen KKM (MyDoublebox
				// kalau bisa-edit / MyLabelAgakKecil kalau read-only), [2]=komponen
				// keterangan (Textbox kalau bisa-edit / MyLabelAgakKecil kalau read-only),
				// [3..]=komponen nilai per kelas, lalu child terakhir=label rata-rata.
				// Kode lama memakai index 1 utk keterangan (selalu meleset, krn index 1
				// itu KKM) & mulai loop nilai-per-kelas dari index 2 (meleset 1 posisi,
				// krn index 2 itu keterangan) -> cast (Hbox) ke Textbox gagal.
				// Selain itu row "Rata-Rata" (matapelajaranSekolah == null) TIDAK PERNAH
				// membungkus nilai per-kelas dlm Hbox (pakai MyLabelBoldAja read-only
				// langsung, lihat baris ~438-444) & TIDAK SET attribute "nilaiSemua" sama
				// sekali -- row itu murni ringkasan hasil hitung (nilainya sudah
				// disinkron via eventListenerRata tiap row matapelajaran berubah), bukan
				// input yg perlu diproses ulang di sini. Maka: (a) ambil keterangan dari
				// index 2 yg benar; (b) proses nilai-per-kelas & rata-rata HANYA utk row
				// matapelajaran biasa (matapelajaranSekolah != null); (c) tetap
				// instanceof-check sebelum cast Hbox agar aman kalau struktur berubah lagi.
				if (row.getChildren().size() > 2 && row.getChildren().get(2) instanceof Textbox) {
					Textbox keterangan = (Textbox) row.getChildren().get(2);
					biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.setKeterangan(keterangan.getValue());
				}

				if (matapelajaranSekolah != null) {
					int index = 3;
					for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
						if (!nilaikelas.trim().isEmpty()) {
							try {
								if (index >= row.getChildren().size()
										|| !(row.getChildren().get(index) instanceof Hbox)) {
									// Bukan Hbox (struktur tak sesuai utk row ini) -- skip aman,
									// jangan paksa cast yg bisa gagal.
									index++;
									continue;
								}
								Hbox hbox = (Hbox) row.getChildren().get(index);
								Doublebox nilai = (Doublebox) hbox.getChildren().get(0);
								if (tbmuser != null) {
									Checkbox checkbox = (Checkbox) hbox.getChildren().get(1);
									biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.masukkanNilai(nilaikelas.trim(),
											checkbox.isChecked(), nilai.getValue());
								} else {
									biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.masukkanNilai(nilaikelas.trim(),
											biodataCalonMahasiswaPunyaVerifikasiMatapelajaran
													.ambilVerifikasi(nilaikelas.trim()),
											nilai.getValue());
								}
								index++;
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiMatapelajaranPMBHelper.java:504");
								// TODO: handle exception
							}

						}
					}
					try {
						@SuppressWarnings("unchecked")
						List<MyDoublebox> nilaiSemua = (List<MyDoublebox>) row.getAttribute("nilaiSemua");
						if (nilaiSemua != null) {
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
							biodataCalonMahasiswaPunyaVerifikasiMatapelajaran.masukkanNilai("RataRata", true, r);
						}
						Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiMatapelajaranPMBHelper.java:524");
						// TODO: handle exception
					}
				} else {
					// Row "Rata-Rata" (ringkasan) -- cukup simpan keterangan yg sudah
					// diambil di atas, nilai per-kelasnya murni tampilan hasil hitung.
					try {
						Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiMatapelajaran);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiMatapelajaranPMBHelper.java:524b");
					}
				}
			}
		}
	}

}
