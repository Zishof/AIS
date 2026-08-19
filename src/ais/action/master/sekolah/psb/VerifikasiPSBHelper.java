package ais.action.master.sekolah.psb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.CalonSiswaPunyaVerifikasiBerkas;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyRowStyled;

public class VerifikasiPSBHelper {

	public static EventListener tampilkanGrid(final Component rowVerifikasi,
			final Rows subRowsVerifikasiKelengkapanCalonSiswa, final GelombangPendaftaranPsb gel,
			final Long calonSiswaId) {
		return tampilkanGrid(rowVerifikasi, subRowsVerifikasiKelengkapanCalonSiswa, gel, calonSiswaId, null, false);
	}

	public static EventListener tampilkanGrid(final Component rowVerifikasi,
			final Rows subRowsVerifikasiKelengkapanCalonSiswa, final GelombangPendaftaranPsb gel,
			final Long calonSiswaId, final Set<VerifikasiKelengkapanCalonSiswa> verif, final boolean readonly) {
		MyGrid subGrid = new MyGrid();
		subGrid.setSclass("fgrid");
		rowVerifikasi.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);

		Column c = new Column("");
		c.setWidth("0%");
		subColumns.appendChild(c);
		c = new Column("Formulir Verifikasi Kelengkapan Berkas");
		c.setWidth("60%");
		subColumns.appendChild(c);
		c = new Column("Keterangan Kelengkapan Berkas");
		subColumns.appendChild(c);

		subRowsVerifikasiKelengkapanCalonSiswa.setParent(subGrid);

		EventListener eventListenerBerkas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(subRowsVerifikasiKelengkapanCalonSiswa);

				if (gel != null) {

					try {
						Tbmuser tbmuser = Common.getCurrentUser();
						Session session1 = HibernateUtil.currentNativeSession();
						GelombangPendaftaranPsb gel2 = (GelombangPendaftaranPsb) session1
								.createCriteria(GelombangPendaftaranPsb.class).add(Restrictions.idEq(gel.getId()))
								.uniqueResult();

						Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = new TreeSet<VerifikasiKelengkapanCalonSiswa>(
								gel2.getVerifikasiKelengkapanCalonSiswas());

						session1.disconnect();
						session1.close();

						HibernateUtil.closeSession();

						rowVerifikasi.setVisible(!verifikasiKelengkapanCalonSiswas.isEmpty());
						for (final VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa : verifikasiKelengkapanCalonSiswas) {
							if (verifikasiKelengkapanCalonSiswa.getAktif()) {
								session1 = HibernateUtil.currentNativeSession();
								CalonSiswaPunyaVerifikasiBerkas calonSiswaPunyaVerifikasiBerkas = (CalonSiswaPunyaVerifikasiBerkas) session1
										.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
										.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa",
												verifikasiKelengkapanCalonSiswa))
										.add(Restrictions.eq("calonSiswa.id", calonSiswaId)).setMaxResults(1)
										.uniqueResult();
								session1.disconnect();
								session1.close();
								HibernateUtil.closeSession();

								if (calonSiswaPunyaVerifikasiBerkas == null) {
									calonSiswaPunyaVerifikasiBerkas = new CalonSiswaPunyaVerifikasiBerkas();
									calonSiswaPunyaVerifikasiBerkas.setCalonSiswa(new CalonSiswa(calonSiswaId));
									calonSiswaPunyaVerifikasiBerkas
											.setVerifikasiKelengkapanCalonSiswa(verifikasiKelengkapanCalonSiswa);

									Session session = HibernateUtil.currentNativeSession();
									session.getTransaction().begin();
									session.save(calonSiswaPunyaVerifikasiBerkas);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
									HibernateUtil.closeSession();
								}

								final CalonSiswaPunyaVerifikasiBerkas fix = calonSiswaPunyaVerifikasiBerkas;

								final Row subRow = new MyRowStyled();
								subRow.setAttribute("verifikasiKelengkapanCalonSiswa", verifikasiKelengkapanCalonSiswa);
								subRow.setAttribute("calonSiswaPunyaVerifikasiBerkas", calonSiswaPunyaVerifikasiBerkas);
								subRow.setParent(subRowsVerifikasiKelengkapanCalonSiswa);
								subRow.setValign("top");

								final MyDetail myvbox1 = new MyDetail();
								myvbox1.setParent(subRow);

								if (tbmuser != null && tbmuser.getCalonSiswa() == null && !readonly) {

									Vbox vboxBerkas = new Vbox();
									vboxBerkas.setParent(subRow);

									final Checkbox checkbox = new Checkbox("Sesuai", "/img/Check-icon.png");
									checkbox.setStyle("font-size:11px;font-weight: bolder;");
									checkbox.setParent(vboxBerkas);
									checkbox.setChecked(calonSiswaPunyaVerifikasiBerkas != null
											&& calonSiswaPunyaVerifikasiBerkas.getVerified());

									new ais.ui.util.MyHtml("<div style=\"font-size:12px;\">"
											+ verifikasiKelengkapanCalonSiswa.getNama() + "</div>")
											.setParent(vboxBerkas);

									Hbox hbox = new Hbox();
									hbox.setParent(vboxBerkas);
									LampiranLain.createDownloadUploadFileLain(hbox,
											verifikasiKelengkapanCalonSiswa.getId(),
											VerifikasiKelengkapanCalonSiswa.class.getName(), "Lampiran", false, null,
											null, false, false, false, false);

									subRow.setAttribute("checkbox", checkbox);

									final Textbox keterangan = new Textbox(calonSiswaPunyaVerifikasiBerkas == null ? ""
											: calonSiswaPunyaVerifikasiBerkas.getKeterangan());
									keterangan.setWidth("90%");
									keterangan.setRows(3);
									keterangan.setParent(subRow);
									keterangan.setDisabled(checkbox.isChecked());

									subRow.setAttribute("keterangan", keterangan);

									final CalonSiswaPunyaVerifikasiBerkas bio = calonSiswaPunyaVerifikasiBerkas;
									checkbox.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											keterangan.setDisabled(checkbox.isChecked());

											Common.clear(myvbox1);
											Hbox hbox = new Hbox();
											hbox.setStyle(subRow.getStyle());
											hbox.setParent(myvbox1);
											hbox.setWidth("100%");
											LampiranLain.createDownloadUploadFileLain(hbox, bio.getId(),
													CalonSiswaPunyaVerifikasiBerkas.class.getName(), "Berkas", false,

													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															LampiranLain lampiranLain = (LampiranLain) arg0.getData();
															if (lampiranLain != null) {
																Common.refreshUpdate(fix);
															}
														}

													}, null, false, false, false, false, null, false, false);
											new ais.ui.util.MyHtml("<hr style='border: 1px solid #bdbbbb;'>")
													.setParent(myvbox1);

										}
									});

									final CalonSiswaPunyaVerifikasiBerkas calonSiswaPunyaVerifikasiBerkasData = calonSiswaPunyaVerifikasiBerkas;
									EventListener eventListener = new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											calonSiswaPunyaVerifikasiBerkasData.setVerified(checkbox.isChecked());
											calonSiswaPunyaVerifikasiBerkasData.setKeterangan(keterangan.getValue());
											Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiBerkasData);

										}
									};

									checkbox.addEventListener("onClick", eventListener);
									keterangan.addEventListener("onChange", eventListener);
								} else {

									Vbox vboxBerkas = new Vbox();
									vboxBerkas.setParent(subRow);
									new ais.ui.util.MyHtml(calonSiswaPunyaVerifikasiBerkas.getVerified()
											? "<font style='font-weight:bold;color:blue;font-size:xx-small'>"
													+ verifikasiKelengkapanCalonSiswa.getNama()
													+ " ==> Telah Sesuai</font>"
											: "<font style='font-weight:bold;color:red;font-size:xx-small'>"
													+ verifikasiKelengkapanCalonSiswa.getNama()
													+ " ==> Belum Diverifikasi</font>")
											.setParent(vboxBerkas);

									Hbox hbox = new Hbox();
									hbox.setParent(vboxBerkas);

									FileFotoLain fileFotoLain = LampiranLain.ambil(
											verifikasiKelengkapanCalonSiswa.getId(),
											VerifikasiKelengkapanCalonSiswa.class.getName());
									if (readonly && fileFotoLain == null) {

									} else {

										LampiranLain.createDownloadUploadFileLain(hbox,
												verifikasiKelengkapanCalonSiswa.getId(),
												VerifikasiKelengkapanCalonSiswa.class.getName(), "Lampiran", false,
												null, null, false, false, false, false);
									}

									if (!calonSiswaPunyaVerifikasiBerkas.getKeterangan().isEmpty()) {
										new ais.ui.util.MyHtml(
												"Keterangan:<br>" + calonSiswaPunyaVerifikasiBerkas.getKeterangan())
												.setParent(subRow);
									} else {
										new ais.ui.util.MyHtml("").setParent(subRow);
									}

								}

								Hbox hbox = new Hbox();
								hbox.setParent(myvbox1);
								hbox.setStyle(subRow.getStyle());
								hbox.setWidth("100%");

								FileFotoLain fileFotoLain = LampiranLain.ambil(calonSiswaPunyaVerifikasiBerkas.getId(),
										CalonSiswaPunyaVerifikasiBerkas.class.getName());
								if (readonly && fileFotoLain == null) {
									hbox.appendChild(new MyLabelAgakKecilBoldMerah("Tidak/belum diupload"));
								} else {
									LampiranLain.createDownloadUploadFileLain(hbox,
											calonSiswaPunyaVerifikasiBerkas.getId(),
											CalonSiswaPunyaVerifikasiBerkas.class.getName(), "Berkas", false,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													LampiranLain lampiranLain = (LampiranLain) arg0.getData();
													if (lampiranLain != null) {

														Common.refreshUpdate(fix);
													}
												}

											}, null, false, false, false, false, null, false, false);
								}
								new ais.ui.util.MyHtml("<hr style='border: 1px solid #bdbbbb;'>").setParent(myvbox1);
								myvbox1.setOpen(true);

							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/VerifikasiPSBHelper.java:281");
					}

				}
			}
		};

		return eventListenerBerkas;
	}

	public static Rows tampilkanVerifikasi(CalonSiswa calonSiswa, Rows rows, Combobox gelombangPendaftaranPsb,
			GelombangPendaftaranPsb gelombang) throws Exception {
		return tampilkanVerifikasi(calonSiswa, rows, gelombangPendaftaranPsb, gelombang, null);
	}

	@SuppressWarnings("deprecation")
	public static Rows tampilkanVerifikasi(final CalonSiswa calonSiswa, Rows rows,
			final Combobox gelombangPendaftaranPsb, final GelombangPendaftaranPsb gelombang,
			final List<VerifikasiKelengkapanCalonSiswa> verif) throws Exception {

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));

		final MyFormRow rowBerkas = new MyFormRow();
		rowBerkas.setVisible(false);
		rowBerkas.setStyle("border:0px;background: transparent;");
		rowBerkas.setParent(rows);
		rowBerkas.appendChild(new ais.ui.util.MyLabelBold("Verifikasi Kelengkapan Berkas"));

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
		c = new Column("Formulir Verifikasi Kelengkapan Berkas");
		c.setWidth("60%");
		subColumns.appendChild(c);
		c = new Column("Keterangan Kelengkapan Berkas");
		subColumns.appendChild(c);

		final Rows subRowsVerifikasiKelengkapanCalonSiswa = new Rows();
		subRowsVerifikasiKelengkapanCalonSiswa.setParent(subGrid);

		/*
		 * PENAUTAN TERTUNDA (bagian 4 dari 4) - bertahan saat gelombang diganti.
		 *
		 * Mengganti gelombang membuat daftar berkas dirender ULANG (Common.clear di bawah),
		 * dan tanpa peta ini setiap render menghasilkan acuan sementara acak yang baru
		 * (lihat FileFotoLain.createDownloadUpload: ref = -randLong() bila id masih null).
		 * Akibatnya berkas yang sudah diunggah tidak ditemukan lagi: tombolnya kembali
		 * kosong sehingga orang tua mengunggah ulang, dan berkas lamanya jadi yatim.
		 *
		 * Peta ini menyimpan satu acuan sementara PER JENIS BERKAS dan dipakai ulang di
		 * setiap render, sehingga berkas yang sudah diunggah tetap tampil dan tetap bisa
		 * ditautkan simpanVerifikasi() setelah calon siswa tersimpan.
		 */
		final java.util.Map<Long, Long> refSementaraBerkas = new java.util.HashMap<Long, Long>();

		EventListener eventListenerBerkas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(subRowsVerifikasiKelengkapanCalonSiswa);
				GelombangPendaftaranPsb gel = gelombang != null ? gelombang
						: (GelombangPendaftaranPsb) (gelombangPendaftaranPsb.getSelectedItem() == null ? null
								: gelombangPendaftaranPsb.getSelectedItem().getValue());

				if (gel != null && gel.getId() != null) {

					Session session = HibernateUtil.currentSession();
					Tbmuser tbmuser = Common.getCurrentUser();

					GelombangPendaftaranPsb gel2 = (GelombangPendaftaranPsb) session
							.createCriteria(GelombangPendaftaranPsb.class).add(Restrictions.idEq(gel.getId()))
							.uniqueResult();

					Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswasTemp = null;

					if (verif == null) {
						verifikasiKelengkapanCalonSiswasTemp = new TreeSet<VerifikasiKelengkapanCalonSiswa>(
								gel2.getVerifikasiKelengkapanCalonSiswas());
					}

					List<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = new ArrayList<VerifikasiKelengkapanCalonSiswa>(
							verifikasiKelengkapanCalonSiswasTemp == null ? verif
									: verifikasiKelengkapanCalonSiswasTemp);
					try {
						Collections.sort(verifikasiKelengkapanCalonSiswas);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/VerifikasiPSBHelper.java:365");
						// TODO: handle exception
					}

					rowBerkas.setVisible(!verifikasiKelengkapanCalonSiswas.isEmpty());
					rowVerifikasi.setVisible(!verifikasiKelengkapanCalonSiswas.isEmpty());
					for (final VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa : verifikasiKelengkapanCalonSiswas) {
						if (verifikasiKelengkapanCalonSiswa.getAktif()) {
							/*
							 * Pada PENDAFTARAN BARU calon siswa masih transien (id null), sehingga
							 * tidak ada baris penghubung yang bisa dicari - pencariannya dilewati
							 * agar Hibernate tidak diberi acuan entitas yang belum tersimpan.
							 */
							CalonSiswaPunyaVerifikasiBerkas calonSiswaPunyaVerifikasiBerkas = (calonSiswa == null
									|| calonSiswa.getId() == null) ? null
											: (CalonSiswaPunyaVerifikasiBerkas) session
													.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
													.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa",
															verifikasiKelengkapanCalonSiswa))
													.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1)
													.uniqueResult();

							if (calonSiswaPunyaVerifikasiBerkas == null) {
								calonSiswaPunyaVerifikasiBerkas = new CalonSiswaPunyaVerifikasiBerkas();
								calonSiswaPunyaVerifikasiBerkas.setCalonSiswa(calonSiswa);
								calonSiswaPunyaVerifikasiBerkas
										.setVerifikasiKelengkapanCalonSiswa(verifikasiKelengkapanCalonSiswa);
								/*
								 * PENAUTAN TERTUNDA (bagian 1 dari 4).
								 * Pada PENDAFTARAN BARU calon siswa belum punya id, sehingga baris
								 * penghubung ini belum boleh disimpan (kolom calon_siswa masih null).
								 * Penyimpanan dilakukan simpanVerifikasi() SETELAH calon siswa tersimpan.
								 */
								if (calonSiswa != null && calonSiswa.getId() != null) {
									Common.refreshSaveOrUpdate(session, calonSiswaPunyaVerifikasiBerkas);
								}
							}

							final MyFormRow subRow = new MyFormRow();
							subRow.setAttribute("verifikasiKelengkapanCalonSiswa", verifikasiKelengkapanCalonSiswa);
							subRow.setAttribute("calonSiswaPunyaVerifikasiBerkas", calonSiswaPunyaVerifikasiBerkas);
							subRow.setStyle("border:1px;solid;background:transparent;");
							subRow.setParent(subRowsVerifikasiKelengkapanCalonSiswa);
							subRow.setValign("top");

							final MyDetail myvbox = new MyDetail();
							myvbox.setParent(subRow);

							if (tbmuser != null && tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null) {

								Vbox vboxBerkas = new Vbox();
								vboxBerkas.setParent(subRow);

								final Checkbox checkbox = new Checkbox("Sesuai", "/img/Check-icon.png");
								checkbox.setStyle("font-size:11px;font-weight: bolder;");
								checkbox.setParent(vboxBerkas);
								checkbox.setChecked(calonSiswaPunyaVerifikasiBerkas != null
										&& calonSiswaPunyaVerifikasiBerkas.getVerified());

								new ais.ui.util.MyHtml("<div style=\"font-size:12px;\">"
										+ verifikasiKelengkapanCalonSiswa.getNama() + "</div>").setParent(vboxBerkas);

								Hbox hbox = new Hbox();
								hbox.setParent(vboxBerkas);
								LampiranLain.createDownloadUploadFileLain(hbox, verifikasiKelengkapanCalonSiswa.getId(),
										VerifikasiKelengkapanCalonSiswa.class.getName(), "Lampiran", false, null, null,
										false, false, false, false);

								final Textbox keterangan = new Textbox(calonSiswaPunyaVerifikasiBerkas == null ? ""
										: calonSiswaPunyaVerifikasiBerkas.getKeterangan());
								keterangan.setWidth("90%");
								keterangan.setRows(2);
								keterangan.setParent(subRow);
								keterangan.setDisabled(checkbox.isChecked());

								subRow.setAttribute("checkbox", checkbox);
								subRow.setAttribute("keterangan", keterangan);

								final CalonSiswaPunyaVerifikasiBerkas cal = calonSiswaPunyaVerifikasiBerkas;

								EventListener eventListenerSimpan = new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										cal.setVerified(checkbox.isChecked());
										cal.setKeterangan(keterangan.getValue());
										Common.refreshUpdate(cal);

										keterangan.setDisabled(checkbox.isChecked());
										Common.clear(myvbox);
										Hbox hbox = new Hbox();
										hbox.setParent(myvbox);
										LampiranLain.createDownloadUploadFileLain(hbox, cal.getId(),
												CalonSiswaPunyaVerifikasiBerkas.class.getName(), "Berkas", false, null,
												null, false, false, false, !checkbox.isChecked());
										new ais.ui.util.MyHtml("<hr>").setParent(myvbox);
									}
								};

								checkbox.addEventListener("onClick", eventListenerSimpan);
								keterangan.addEventListener("onChange", eventListenerSimpan);
							} else {

								new ais.ui.util.MyHtml(calonSiswaPunyaVerifikasiBerkas.getVerified()
										? "<font style='font-weight:bold;color:blue;font-size:xx-small'>"
												+ verifikasiKelengkapanCalonSiswa.getNama() + " ==> Telah Sesuai</font>"
										: "<font style='font-weight:bold;color:red;font-size:xx-small'>"
												+ verifikasiKelengkapanCalonSiswa.getNama()
												+ " ==> Belum Diverifikasi</font>")
										.setParent(subRow);

								if (!calonSiswaPunyaVerifikasiBerkas.getKeterangan().isEmpty()) {
									new ais.ui.util.MyHtml(
											"<hr>Keterangan:<br>" + calonSiswaPunyaVerifikasiBerkas.getKeterangan())
											.setParent(subRow);
								} else {
									new ais.ui.util.MyHtml("").setParent(subRow);
								}
							}

							Hbox hbox = new Hbox();
							hbox.setParent(myvbox);
							/*
							 * PENAUTAN TERTUNDA (bagian 2 dari 4).
							 * Saat pendaftaran baru, id baris penghubung masih null sehingga berkas
							 * terunggah belum punya acuan. Berkasnya ditangkap ke atribut baris
							 * "lampiranBaru" agar simpanVerifikasi() bisa menautkannya setelah id
							 * terbit. Untuk data lama listener ini tidak mengubah apa pun.
							 */
							/*
							 * Acuan sementara yang STABIL untuk pendaftaran baru: dipakai ulang
							 * setiap kali daftar berkas dirender (mis. gelombang diganti), supaya
							 * berkas yang sudah diunggah tetap ditemukan dan tetap tampil.
							 */
							Long refBerkas = calonSiswaPunyaVerifikasiBerkas.getId();
							if (refBerkas == null && verifikasiKelengkapanCalonSiswa.getId() != null) {
								refBerkas = refSementaraBerkas.get(verifikasiKelengkapanCalonSiswa.getId());
								if (refBerkas == null) {
									/*
									 * Acuan sementara SELALU negatif supaya tidak mungkin bentrok
									 * dengan id baris penghubung yang asli.
									 *
									 * Dua undian randLong() digabung (ruang ~1e16, bukan ~1e8): kolom
									 * acuan ini dipakai bersama SEMUA pendaftar yang sedang mengisi
									 * formulir, dan berkas dicari lewat acuan itu. Ruang yang sempit
									 * berarti dua pendaftar bisa mendapat acuan yang sama untuk jenis
									 * berkas yang sama, lalu saling melihat berkas satu sama lain.
									 */
									refBerkas = Long
											.valueOf(-((Common.randLong() + 1L) * 100000000L + Common.randLong()));
									refSementaraBerkas.put(verifikasiKelengkapanCalonSiswa.getId(), refBerkas);
								}
								subRow.setAttribute("refSementaraBerkas", refBerkas);

								// Pulihkan berkas yang diunggah SEBELUM render ulang ini.
								FileFotoLain berkasTerunggah = FileFotoLain.ambil(Boolean.FALSE, refBerkas,
										CalonSiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class, true);
								if (berkasTerunggah instanceof LampiranLain) {
									subRow.setAttribute("lampiranBaru", berkasTerunggah);
								}
							}

							final MyFormRow subRowLampiran = subRow;
							EventListener penangkapLampiran = new EventListener() {
								@Override
								public void onEvent(Event argLampiran) throws Exception {
									try {
										if (argLampiran != null && argLampiran.getData() instanceof LampiranLain) {
											subRowLampiran.setAttribute("lampiranBaru", argLampiran.getData());
										}
									} catch (Exception e) {
										ais.common.ErrorAuditUtil.record(e, "VerifikasiPSBHelper.penangkapLampiran");
									}
								}
							};
							LampiranLain.createDownloadUploadFileLain(hbox, refBerkas,
									CalonSiswaPunyaVerifikasiBerkas.class.getName(), "Berkas", false,
									penangkapLampiran, null, false,
									false, false, !calonSiswaPunyaVerifikasiBerkas.getVerified(), null, false, true);

							new ais.ui.util.MyHtml("<hr>").setParent(myvbox);
							myvbox.setOpen(true);
						}
					}
				}
			}
		};

		eventListenerBerkas.onEvent(null);
		if (gelombangPendaftaranPsb != null) {
			gelombangPendaftaranPsb.addEventListener("onChange", eventListenerBerkas);
		}

		return subRowsVerifikasiKelengkapanCalonSiswa;

	}

	@SuppressWarnings("unchecked")
	public static void simpanVerifikasi(CalonSiswa calonSiswa, Rows subRowsVerifikasiKelengkapanCalonSiswa) {
		if (subRowsVerifikasiKelengkapanCalonSiswa != null) {
			List<Row> rowsVerifikasi = subRowsVerifikasiKelengkapanCalonSiswa.getChildren();
			for (Row row : rowsVerifikasi) {
				/*
				 * PENAUTAN TERTUNDA (bagian 3 dari 4).
				 *
				 * Sebelumnya baris HANYA diproses bila punya atribut "checkbox", padahal
				 * checkbox itu hanya dibuat pada cabang ADMIN. Akibatnya berkas yang diunggah
				 * calon siswa sendiri (jalur pendaftaran) tidak pernah tersimpan penghubungnya.
				 * Sekarang setiap baris yang mengenal jenis berkasnya ikut diproses; nilai
				 * verified/keterangan tetap HANYA disentuh bila checkbox-nya memang ada,
				 * sehingga perilaku layar admin tidak berubah.
				 */
				VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa = (VerifikasiKelengkapanCalonSiswa) row
						.getAttribute("verifikasiKelengkapanCalonSiswa");
				if (verifikasiKelengkapanCalonSiswa == null) {
					continue;
				}
				Checkbox checkbox = (Checkbox) ((row.getAttribute("checkbox") instanceof Checkbox)
						? row.getAttribute("checkbox") : null);
				Object lampiranBaru = row.getAttribute("lampiranBaru");
				/*
				 * Jalur cadangan: atribut "lampiranBaru" bisa hilang bila baris sempat
				 * dirender ulang (gelombang diganti) setelah berkasnya diunggah. Acuan
				 * sementaranya stabil, jadi berkasnya masih bisa ditemukan lewat acuan itu.
				 */
				Object refSementara = row.getAttribute("refSementaraBerkas");
				if (!(lampiranBaru instanceof LampiranLain) && refSementara instanceof Long) {
					FileFotoLain berkasSementara = FileFotoLain.ambil(Boolean.FALSE, (Long) refSementara,
							CalonSiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class, true);
					if (berkasSementara instanceof LampiranLain) {
						lampiranBaru = berkasSementara;
					}
				}
				if (checkbox == null && !(lampiranBaru instanceof LampiranLain)) {
					continue;
				}

				CalonSiswaPunyaVerifikasiBerkas calonSiswaPunyaVerifikasiBerkas = (CalonSiswaPunyaVerifikasiBerkas) row
						.getAttribute("calonSiswaPunyaVerifikasiBerkas");

				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
						&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
								: null);

				if (calonSiswaPunyaVerifikasiBerkas == null) {
					calonSiswaPunyaVerifikasiBerkas = new CalonSiswaPunyaVerifikasiBerkas();
					calonSiswaPunyaVerifikasiBerkas
							.setVerifikasiKelengkapanCalonSiswa(verifikasiKelengkapanCalonSiswa);
				}
				// Calon siswa sudah punya id di titik ini (dipanggil SETELAH calon siswa disimpan).
				calonSiswaPunyaVerifikasiBerkas.setCalonSiswa(calonSiswa);
				if (checkbox != null) {
					calonSiswaPunyaVerifikasiBerkas.setVerified(checkbox.isChecked());
					if (keterangan != null) {
						calonSiswaPunyaVerifikasiBerkas.setKeterangan(keterangan.getValue());
					}
				}
				Common.refreshSaveOrUpdate(calonSiswaPunyaVerifikasiBerkas);

				if (lampiranBaru instanceof LampiranLain && calonSiswaPunyaVerifikasiBerkas.getId() != null) {
					tautkanLampiranTertunda((LampiranLain) lampiranBaru,
							calonSiswaPunyaVerifikasiBerkas.getId());
					row.removeAttribute("lampiranBaru");
					row.removeAttribute("refSementaraBerkas");
				}
			}
		}
	}

	/**
	 * Tautkan berkas yang diunggah SEBELUM baris penghubungnya punya id.
	 *
	 * <p>Berkas lampiran disimpan pada basis data STREAMING (terpisah dari basis data utama),
	 * sehingga pembaruan acuannya memakai sesi streaming - pola yang sama dipakai
	 * {@code CalonSiswaAction} saat menautkan foto dan lampiran Form Tambahan setelah simpan.</p>
	 *
	 * <p>Kegagalan di sini TIDAK boleh menggagalkan pendaftaran yang sudah tersimpan: berkas
	 * masih bisa diunggah ulang, sedangkan data pendaftarannya tidak boleh hilang.</p>
	 */
	private static void tautkanLampiranTertunda(LampiranLain lampiran, Long idPenghubung) {
		if (lampiran == null || idPenghubung == null) {
			return;
		}
		if (idPenghubung.equals(lampiran.getRef())) {
			return;
		}
		Session streamingSession = null;
		try {
			streamingSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			streamingSession.refresh(lampiran);
			lampiran.setRef(idPenghubung);
			streamingSession.update(lampiran);
			streamingSession.getTransaction().commit();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "VerifikasiPSBHelper.tautkanLampiranTertunda");
			try {
				if (streamingSession != null && streamingSession.getTransaction() != null
						&& streamingSession.getTransaction().isActive()) {
					streamingSession.getTransaction().rollback();
				}
			} catch (Exception abaikan) {
			}
		} finally {
			try {
				ais.database.hibernate.StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception abaikan) {
			}
		}
	}

}
