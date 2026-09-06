package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelompokParameterTambahanPertemuan;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPertemuan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;

/**
 * Pengelola baris-baris parameter tambahan dinamis pada form {@link Pertemuan} (satu agenda
 * pertemuan/kegiatan — dapat berupa perkuliahan, ujian PMB/PSB, bimbingan tugas akhir, KKN, PKL,
 * skripsi, KRS, jadwal pelajaran sekolah, grup pertemuan, atau formulir kegiatan). Ini adalah
 * varian parameter tambahan yang paling kompleks di antara keluarga
 * {@code ParameterTambahan*Listener} lainnya, karena {@link Pertemuan} bisa menjadi konteks
 * beberapa jenis kegiatan sekaligus dan parameter dapat diisi dengan beberapa tata letak
 * berbeda tergantung konfigurasi kelompoknya:
 * <ul>
 * <li><b>Jenis konteks pertemuan</b> — jenis {@code criterion} pencarian parameter ditentukan
 * dari field {@link Pertemuan} mana yang terisi (perkuliahan, jadwalUjianPMB,
 * mahasiswaRequestTugasAkhir, kelompokKkn, kelompokPkl, skripsi, krsMahasiswa, jadwalUjianPSB,
 * jadwalPelajaran, pertemuanPunyaGrupPertemuan, formulirKegiatan) — hanya satu yang berlaku per
 * pertemuan, dicek berurutan (yang pertama cocok dipakai).</li>
 * <li><b>{@code getUntukDosenDanAdmin()}</b> — bila kelompok parameter khusus dosen/admin,
 * ditampilkan sebagai grid dengan hingga 2 kolom parameter ({@code kolomKe} 1 dan 2) per baris,
 * bukan satu kolom per baris seperti listener lain.</li>
 * <li><b>{@code getDiisiPerPeserta()}</b> — bila parameter diisi terpisah untuk tiap peserta
 * (mahasiswa) pertemuan: untuk staf (bukan mahasiswa/siswa login), ditampilkan sebagai grid satu
 * baris per mahasiswa peserta ({@link Pertemuan#ambilMahasiswa()}) dengan kolom per parameter,
 * nilainya disimpan sebagai objek JSON berkunci id mahasiswa; untuk mahasiswa/siswa yang login
 * sendiri, hanya nilai miliknya yang ditampilkan (read-only, diambil dari JSON berkunci id-nya).</li>
 * <li><b>Kasus umum</b> — satu baris per parameter seperti listener lain, dengan nilai tersimpan
 * langsung (bukan per-peserta) kecuali bila {@code getDiisiPerPeserta()} aktif untuk mahasiswa/
 * siswa yang login (nilai diekstrak dari JSON berkunci id pengguna).</li>
 * </ul>
 * Format penyimpanan nilai per baris tetap mengikuti konvensi
 * {@code "kelompokId->parameterId[->parameterId2]<=>nilai<=>keterangan"} dipisah newline pada
 * {@link Pertemuan#getParameterTambahanInds()}, dengan nilai/keterangan itu sendiri bisa berupa
 * string JSON bersarang (berkunci id pertemuan atau id peserta) untuk mode per-dosen-admin atau
 * per-peserta.
 */
public class ParameterTambahanPertemuanListener implements EventListener {

	/** Daftar baris form yang dikelola bersama; dibersihkan/dibangun ulang oleh {@link #onEvent(Event)} dan dibaca kembali oleh {@link #onSave(Pertemuan)}. */
	private List<Row> parameterRows;
	/** Kontainer ZK tempat baris parameter ditambahkan. */
	private Rows rows;
	/** Pertemuan (agenda kegiatan) yang parameter tambahannya dikelola oleh listener ini -- lihat javadoc kelas untuk daftar konteks yang mungkin. */
	private Pertemuan pertemuan;
	/** Lampiran yang sudah diunggah, dikunci per jenis parameter (lihat {@link LampiranLain#resolveJenisParameterTambahan}), diteruskan ke {@link ParameterTambahan#initComponent}. */
	private Map<String, LampiranLain> lampiranLains;

	/**
	 * @param pertemuan     pertemuan yang parameter tambahannya dikelola
	 * @param parameterRows daftar baris form yang dikelola bersama
	 * @param lampiranLains lampiran yang sudah diunggah, dikunci per jenis parameter
	 * @param rows          kontainer ZK tempat baris parameter ditambahkan
	 */
	public ParameterTambahanPertemuanListener(Pertemuan pertemuan, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.pertemuan = pertemuan;
		this.lampiranLains = lampiranLains;
	}

	/** Menulis nilai isian dari {@code parameterRows} saat ini ke entitas {@code pertemuan} yang diberikan. */
	public void onSave(Pertemuan pertemuan) {
		pertemuan.populateParameterTambahan(parameterRows);
	}

	/**
	 * Membangun ulang seluruh baris parameter tambahan untuk {@link #pertemuan}: menentukan
	 * jenis konteks pertemuan (lihat javadoc kelas) untuk menyaring
	 * {@link KelompokParameterTambahanPertemuan} yang relevan, lalu untuk tiap kelompok aktif
	 * memilih salah satu dari tiga tata letak (grid dosen/admin 2 kolom, grid per-peserta, atau
	 * satu baris per parameter) sesuai flag {@code getUntukDosenDanAdmin()}/
	 * {@code getDiisiPerPeserta()} kelompok tersebut, lalu mengurai nilai tersimpan sebelumnya
	 * dari {@link Pertemuan#getParameterTambahanInds()} (termasuk membongkar lapisan JSON
	 * bersarang untuk mode per-dosen-admin/per-peserta) untuk memulihkan isian form. Bila tidak
	 * ada kelompok parameter yang berlaku sama sekali, menampilkan satu baris pesan "Tidak ada
	 * parameter hasil yang bisa di isi".
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		Criterion criterion = Restrictions.sqlRestriction("false");

		if (pertemuan.getPerkuliahan() != null) {
			criterion = Restrictions.eq("perkuliahan", true);
		} else if (pertemuan.getJadwalUjianPMB() != null) {
			criterion = Restrictions.eq("jadwalUjianPMB", true);
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			criterion = Restrictions.eq("mahasiswaRequestTugasAkhir", true);
		} else if (pertemuan.getKelompokKkn() != null) {
			criterion = Restrictions.eq("kelompokKkn", true);
		} else if (pertemuan.getKelompokPkl() != null) {
			criterion = Restrictions.eq("kelompokPkl", true);
		} else if (pertemuan.getSkripsi() != null) {
			criterion = Restrictions.eq("skripsi", true);
		} else if (pertemuan.getKrsMahasiswa() != null) {
			criterion = Restrictions.eq("krsMahasiswa", true);
		} else if (pertemuan.getJadwalUjianPSB() != null) {
			criterion = Restrictions.eq("jadwalUjianPSB", true);
		} else if (pertemuan.getJadwalPelajaran() != null) {
			criterion = Restrictions.eq("jadwalPelajaran", true);
		} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null) {
			criterion = Restrictions.eq("pertemuanPunyaGrupPertemuan", true);
		} else if (pertemuan.getFormulirKegiatan() != null) {
			criterion = Restrictions.eq("formulirKegiatan", true);
		}

		Session session = HibernateUtil.currentSession();
		List<KelompokParameterTambahanPertemuan> kelompokParameterTambahanPertemuans = ConstantValues.simpleList(
				session.createCriteria(ParameterTambahanPertemuan.class).add(criterion)
						.createAlias("parameterTambahan", "parameterTambahan")
						.createAlias("kelompokParameterTambahanPertemuan", "kelompokParameterTambahanPertemuan")
						.add(Restrictions.eq("parameterTambahan.aktif", true))
						.add(Restrictions.eq("kelompokParameterTambahanPertemuan.aktif", true))
						.setProjection(Projections.groupProperty("kelompokParameterTambahanPertemuan.id")),
				KelompokParameterTambahanPertemuan.class, false);

		List<Mahasiswa> mahasiswas = pertemuan.ambilMahasiswa();

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				session.refresh(pertemuan);
				pertemuan.populateParameterTambahan(parameterRows);
				Common.refreshUpdate(session, pertemuan);
				session.flush();
			}
		};

		if (!kelompokParameterTambahanPertemuans.isEmpty()) {
			Collections.sort(kelompokParameterTambahanPertemuans);

			for (KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan : kelompokParameterTambahanPertemuans) {
				pertemuan.populateKelompokParameterTambahanPertemuan(kelompokParameterTambahanPertemuan, true);
				MyFormRow rowParameterTambahan = new MyFormRow();
				rowParameterTambahan.setVisible(false);
				rowParameterTambahan.setStyle("border:0px;background: transparent;");
				rowParameterTambahan.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
				rowParameterTambahan.appendChild(new MyLabelBold(kelompokParameterTambahanPertemuan.getNama() + ""));
				parameterRows.add(rowParameterTambahan);

				List<ParameterTambahan> parameterTambahans = ConstantValues
						.simpleList(
								session.createCriteria(ParameterTambahanPertemuan.class)
										.add(Restrictions
												.or(Restrictions.isNull("kolomKe"), Restrictions.eq("kolomKe", 1)))
										.add(criterion)
										.add(Restrictions.eq("kelompokParameterTambahanPertemuan",
												kelompokParameterTambahanPertemuan))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanPertemuan",
												"kelompokParameterTambahanPertemuan")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanPertemuan.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
				if (!parameterTambahans.isEmpty()) {

					if (kelompokParameterTambahanPertemuan.getUntukDosenDanAdmin()) {

						MyFormRow rowParameterGrid = new MyFormRow();
						rowParameterGrid.setStyle("border:0px;background: transparent;");
						rowParameterGrid.setParent(rows);
						ais.ui.util.ZkCompat.setSpans(rowParameterGrid, "2");

						Grid grid = new Grid();
						grid.setSclass("dgrid");
						grid.setParent(rowParameterGrid);

						Columns columns = new Columns();
						columns.setParent(grid);

						for (int index = 0; index < kelompokParameterTambahanPertemuan.getKolomKe(); index++) {

							Column column = new Column();
							column.setParent(columns);
							column = new Column();
							column.setParent(columns);
							column.setWidth("10%");
						}

						List<Row> rowsData = new ArrayList<Row>();

						Rows rows = new Rows();
						rows.setParent(grid);
						for (ParameterTambahan parameterTambahan : parameterTambahans) {
							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							rowsData.add(row);

							row.setAttribute("parameterTambahan", parameterTambahan);
							row.setAttribute("kelompokParameterTambahanPertemuan", kelompokParameterTambahanPertemuan);

							String jenis = LampiranLain.resolveJenisParameterTambahan(Pertemuan.class,
									pertemuan.getId(), kelompokParameterTambahanPertemuan.getId() + "->"
											+ parameterTambahan.getId());
							row.appendChild(new Label(parameterTambahan.getLabelInputan()
									+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));

							String valAsli = "";
							String ketAsli = "";

							String val = "";
							String ket = "";

							String[] spl = pertemuan.getParameterTambahanInds().split("\n");
							for (String d : spl) {
								String[] value = d.split("<=>");
								if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
									valAsli = value.length > 1 ? value[1].trim() : "";

									try {
										ketAsli = value.length > 0 ? value[value.length - 1] : "";
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:212");

									}

									try {
										JSONObject jsonObject = new JSONObject(valAsli);
										val = jsonObject.getString(pertemuan.getId().toString());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:219");

									}

									try {
										JSONObject jsonObject = new JSONObject(ketAsli);
										ket = jsonObject.getString(pertemuan.getId().toString());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:226");

									}

									try {
										JSONObject jsonObject = new JSONObject(val);
										val = jsonObject.getString(pertemuan.getId().toString());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:233");

									}

									try {
										JSONObject jsonObject = new JSONObject(ket);
										ket = jsonObject.getString(pertemuan.getId().toString());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:240");

									}

									System.out.println("ketAsli 1 -> " + ketAsli + " val -> " + val
											+ " parameterTambahan -> " + parameterTambahan);

								}
							}

							if (mahasiswa == null && siswa == null) {
								ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
										pertemuan.getId(), val, ket, parameterTambahan, isi);
							} else {
								Vbox vbox = new Vbox();
								vbox.setParent(row);
								vbox.appendChild(new MyLabelAgakKecilBold(val));
								vbox.appendChild(new MyLabelAgakKecil(ket));
							}
						}

						List<ParameterTambahan> parameterTambahansKe2 = ConstantValues.simpleList(
								session.createCriteria(ParameterTambahanPertemuan.class)
										.add(Restrictions.eq("kolomKe", 2)).add(criterion)
										.add(Restrictions.eq("kelompokParameterTambahanPertemuan",
												kelompokParameterTambahanPertemuan))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanPertemuan",
												"kelompokParameterTambahanPertemuan")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanPertemuan.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);
						Collections.sort(parameterTambahansKe2);

						for (int i = 0; i < rowsData.size(); i++) {
							try {
								Row row = rowsData.get(i);
								ParameterTambahan parameterTambahan = parameterTambahansKe2.size() > (i + 1)
										? parameterTambahansKe2.get(i)
										: parameterTambahansKe2.get(parameterTambahansKe2.size() - 1);
								if (parameterTambahan != null) {
									ParameterTambahan parameterTambahan1 = (ParameterTambahan) row
											.getAttribute("parameterTambahan");
									row.setAttribute("parameterTambahan_2", parameterTambahan);
									row.setAttribute("kelompokParameterTambahanPertemuan",
											kelompokParameterTambahanPertemuan);

									String jenis = LampiranLain.resolveJenisParameterTambahan(Pertemuan.class,
											pertemuan.getId(), kelompokParameterTambahanPertemuan.getId() + "->"
													+ parameterTambahan.getId() + "->" + parameterTambahan1.getId());

									row.appendChild(new Label(parameterTambahan.getLabelInputan()
											+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));

									String valAsli = "";
									String ketAsli = "";

									String val = "";
									String ket = "";

									String[] spl = pertemuan.getParameterTambahanInds().split("\n");
									for (String d : spl) {
										String[] value = d.split("<=>");
										if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
											valAsli = value.length > 1 ? value[1].trim() : "";

											try {
												ketAsli = value.length > 0 ? value[value.length - 1] : "";
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:308");

											}

											try {
												JSONObject jsonObject = new JSONObject(valAsli);
												val = jsonObject.getString(pertemuan.getId().toString());
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:315");

											}

											try {
												JSONObject jsonObject = new JSONObject(ketAsli);
												ket = jsonObject.getString(pertemuan.getId().toString());
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:322");

											}
											

											try {
												JSONObject jsonObject = new JSONObject(val);
												val = jsonObject.getString(pertemuan.getId().toString());
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:330");

											}

											try {
												JSONObject jsonObject = new JSONObject(ket);
												ket = jsonObject.getString(pertemuan.getId().toString());
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:337");

											}

											System.out.println("ketAsli 2 -> " + ketAsli + " parameterTambahan -> "
													+ parameterTambahan);

										}
									}

									if (mahasiswa == null && siswa == null) {
										ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
												pertemuan.getId(), val, ket, parameterTambahan, isi,
												lampiranLains == null, "component_2");
									} else {
										Vbox vbox = new Vbox();
										vbox.setParent(row);
										vbox.appendChild(new MyLabelAgakKecilBold(val));
										vbox.appendChild(new MyLabelAgakKecil(ket));
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:359");
							}
						}

					}

					else if (mahasiswa == null && siswa == null
							&& kelompokParameterTambahanPertemuan.getDiisiPerPeserta()) {

						MyFormRow rowParameterGrid = new MyFormRow();
						rowParameterGrid.setStyle("border:0px;background: transparent;");
						rowParameterGrid.setParent(rows);
						ais.ui.util.ZkCompat.setSpans(rowParameterGrid, "2");

						Grid grid = new Grid();
						grid.setSclass("dgrid");
						grid.setParent(rowParameterGrid);

						Columns columns = new Columns();
						columns.setParent(grid);
						Column column = new Column("Peserta");
						column.setParent(columns);
						column.setWidth("15%");
						for (ParameterTambahan parameterTambahan : parameterTambahans) {
							column = new Column(parameterTambahan.getLabelInputan());
							column.setParent(columns);
						}

						Rows rows = new Rows();
						rows.setParent(grid);
						for (Mahasiswa mhs : mahasiswas) {

							Vbox a = new Vbox();
							Hbox ahbox = new Hbox();
							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							ahbox.setParent(row);
							CommonMedia.tampilkanGambarKecil(mhs).setParent(ahbox);

							a.setParent(ahbox);
							new Label(mhs.getNim() + " / " + mhs.getNama()).setParent(a);

							mhs.tampilkanHp(a);
							mhs.tampilkanEmail(a);

							for (ParameterTambahan parameterTambahan : parameterTambahans) {
								String jenis = kelompokParameterTambahanPertemuan.getId() + "->"
										+ parameterTambahan.getId();
								String valAsli = "";
								String ketAsli = "";
								String val = "";
								String ket = "";
								String[] spl = pertemuan.getParameterTambahanInds().split("\n");
								for (String d : spl) {
									String[] value = d.split("<=>");
									if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
										valAsli = value.length > 1 ? value[1].trim() : "";

										try {
											ketAsli = value.length > 0 ? value[value.length - 1] : "";
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:421");

										}

										try {
											JSONObject jsonObject = new JSONObject(valAsli);
											val = jsonObject.getString(mhs.getId().toString());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:428");

										}

										try {
											JSONObject jsonObject = new JSONObject(ketAsli);
											ket = jsonObject.getString(mhs.getId().toString());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:435");

										}
									}
								}

								Vbox vbox = new Vbox();
								vbox.setParent(row);
								vbox.appendChild(new MyLabelAgakKecilBold(val));
								vbox.appendChild(new MyLabelAgakKecil(ket));

								if (parameterTambahan.getHarusMenyertakanLampiran()) {

									Hbox hbox = new Hbox();
									hbox.setWidth("100%");
									hbox.setStyle("border:0px;background: transparent;");

									LampiranLain.createDownloadUploadFileLain(hbox,
											pertemuan.getId() == null ? -Common.randLong() : pertemuan.getId(),
											LampiranLain.resolveJenisParameterTambahan(Pertemuan.class,
													pertemuan.getId(), jenis + "_" + mhs.getId().toString()),
											parameterTambahan.getLabelInputan()
													+ (parameterTambahan.getLampiranWajibDiisi() ? " (*)" : " "),
											false, null, null, false, false, false, false, null);
									hbox.setParent(vbox);
								}
							}
						}

					} else {

						boolean tampil = false;
						for (ParameterTambahan parameterTambahan : parameterTambahans) {
							String jenis = LampiranLain.resolveJenisParameterTambahan(Pertemuan.class,
									pertemuan.getId(), kelompokParameterTambahanPertemuan.getId() + "->"
											+ parameterTambahan.getId());

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setAttribute("parameterTambahan", parameterTambahan);
							row.setAttribute("kelompokParameterTambahanPertemuan", kelompokParameterTambahanPertemuan);
							row.setParent(rows);
							row.appendChild(new Label(parameterTambahan.getLabelInputan()
									+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
							if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
								parameterRows
										.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
							}
							String valAsli = "";
							String ketAsli = "";

							String val = "";
							String ket = "";

							String[] spl = pertemuan.getParameterTambahanInds().split("\n");
							for (String d : spl) {
								String[] value = d.split("<=>");
								if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
									valAsli = value.length > 1 ? value[1].trim() : "";

									try {
										ketAsli = value.length > 0 ? value[value.length - 1] : "";
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:495");

									}

									if (mahasiswa != null && kelompokParameterTambahanPertemuan.getDiisiPerPeserta()) {
										try {
											JSONObject jsonObject = new JSONObject(valAsli);
											val = jsonObject.getString(mahasiswa.getId().toString());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:503");

										}

										try {
											JSONObject jsonObject = new JSONObject(ketAsli);
											ket = jsonObject.getString(mahasiswa.getId().toString());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:510");

										}
									} else if (siswa != null
											&& kelompokParameterTambahanPertemuan.getDiisiPerPeserta()) {
										try {
											JSONObject jsonObject = new JSONObject(valAsli);
											val = jsonObject.getString(siswa.getId().toString());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:518");

										}

										try {
											JSONObject jsonObject = new JSONObject(ketAsli);
											ket = jsonObject.getString(siswa.getId().toString());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPertemuanListener.java:525");

										}
									} else {
										val = valAsli;
										ket = ketAsli;
									}

								}
							}

							tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
									pertemuan.getId(), val, ket, parameterTambahan, isi);

						}

						rowParameterTambahan.setVisible(tampil);
					}
				}
			}
		} else {
			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(true);
			rowParameterTambahan.setStyle("border:0px;background: transparent;");
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelBold("Tidak ada parameter hasil yang bisa di isi"));
			parameterRows.clear();
		}
	}
}
