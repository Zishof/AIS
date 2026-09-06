package ais.action.master.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.IsiAngketParameterUmum;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanAngketUmum;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;

/**
 * Renderer dan validator parameter tambahan angket untuk pengisian
 * {@link IsiAngketParameterUmum}, dipakai bersama halaman input checklist penilaian umum,
 * dosen, maupun guru.
 *
 * <p>Listener lama hanya mendukung {@link GrupChecklistPenilaianUmum}. Versi ini tetap
 * kompatibel dengan constructor lama, dan menambahkan constructor untuk grup
 * angket dosen serta guru.</p>
 *
 * <p><b>Catatan keamanan — jalur RENTAN task_484d4bd0:</b> {@link #onSave(IsiAngketParameterUmum)}
 * memanggil {@code managed.populateParameterTambahan(parameterRows)}, yaitu method pada entity
 * {@link IsiAngketParameterUmum} yang (per Javadoc kelas entity tsb) masih memakai
 * {@code buildJenis(...)} miliknya SENDIRI — implementasi yang TERPISAH dan tidak sinkron dengan
 * {@link #buildJenis(Row, ParameterTambahan)} di kelas ini — untuk memanggil
 * {@code LampiranLain.ambil(getId(), jenis)} secara langsung tanpa
 * {@code LampiranLain.resolveJenisParameterTambahan(...)}. Listener ini sendiri SUDAH diperbaiki
 * pada r83937 (task_b82b25d2): {@link #buildJenis(Object, ParameterTambahan)} memakai
 * {@code resolveJenisParameterTambahan(GrupChecklistPenilaianUmum.class, ...)} untuk cabang grup
 * checklist Umum, sehingga jalur render ({@link #onEvent(Event)}) dan validasi
 * ({@link #validate()}) sudah aman dari tabrakan namespace lintas-entitas. Perbaikan itu TIDAK
 * menutup entity-nya sendiri — akibatnya {@code jenis} yang dipakai untuk mengaitkan lampiran saat
 * render (ber-namespace) bisa berbeda dari {@code jenis} yang dipakai entity saat menyusun ulang
 * string jawaban tersimpan (tanpa namespace) pada {@link #onSave(IsiAngketParameterUmum)} —
 * berpotensi membuat lampiran yang baru diunggah tidak terhubung ke jawaban yang tersimpan
 * (gagal-aman ke arah "tidak ditemukan", bukan tertukar), di samping risiko tabrakan
 * lintas-entitas yang sudah dilaporkan di task_484d4bd0 pada entity itu sendiri. Perbaikan
 * lengkap mengikuti kelas entity {@link IsiAngketParameterUmum}, bukan kelas listener ini.</p>
 */
public class IsiAngketParameterUmumListener implements EventListener {

	/** Baris ZK komponen parameter tambahan yang sedang dirender, dipakai ulang oleh {@link #validate()} dan {@link #onSave(IsiAngketParameterUmum)}. */
	private List<Row> parameterRows;
	/** Kontainer baris grid tempat baris parameter tambahan disisipkan/dibersihkan. */
	private Rows rows;
	/** Data pengisian angket yang sedang diedit; sumber nilai jawaban tersimpan dan target populasi saat simpan. */
	private IsiAngketParameterUmum isiAngketParameterUmum;
	/** Peta lampiran yang sudah diunggah, dikunci berdasarkan {@code jenis}; dipakai memeriksa kewajiban unggah lampiran pada {@link #validate()}. */
	private Map<String, LampiranLain> lampiranLains;
	/** Grup checklist penilaian umum target, bila listener dipakai untuk konteks Umum (konstruktor lama); boleh {@code null}. */
	private GrupChecklistPenilaianUmum grupChecklistPenilaianUmum;
	/** Grup checklist penilaian dosen target, bila listener dipakai untuk konteks angket Dosen; boleh {@code null}. */
	private GrupChecklistPenilaianDosen grupChecklistPenilaianDosen;
	/** Grup checklist penilaian guru target, bila listener dipakai untuk konteks angket Guru; boleh {@code null}. */
	private GrupChecklistPenilaianGuru grupChecklistPenilaianGuru;

	/**
	 * Konstruktor lama (kompatibilitas mundur) untuk konteks grup checklist penilaian Umum.
	 * Target grup akan diresolusi otomatis dari {@code isiAngketParameterUmum} lewat
	 * {@link #resolveGrupTarget()} bila tidak ditentukan eksplisit.
	 *
	 * @param isiAngketParameterUmum data pengisian angket yang sedang diedit
	 * @param parameterRows          daftar baris ZK parameter tambahan yang akan dikelola
	 * @param lampiranLains          peta lampiran yang sudah diunggah, dikunci per {@code jenis}
	 * @param rows                   kontainer baris grid tempat komponen dirender
	 */
	public IsiAngketParameterUmumListener(IsiAngketParameterUmum isiAngketParameterUmum, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this(isiAngketParameterUmum, parameterRows, lampiranLains, rows, null, null, null);
	}

	/**
	 * Konstruktor untuk konteks angket Dosen.
	 *
	 * @param isiAngketParameterUmum      data pengisian angket yang sedang diedit
	 * @param parameterRows               daftar baris ZK parameter tambahan yang akan dikelola
	 * @param lampiranLains               peta lampiran yang sudah diunggah, dikunci per {@code jenis}
	 * @param rows                        kontainer baris grid tempat komponen dirender
	 * @param grupChecklistPenilaianDosen grup checklist penilaian dosen target
	 */
	public IsiAngketParameterUmumListener(IsiAngketParameterUmum isiAngketParameterUmum, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, GrupChecklistPenilaianDosen grupChecklistPenilaianDosen) {
		this(isiAngketParameterUmum, parameterRows, lampiranLains, rows, null, grupChecklistPenilaianDosen, null);
	}

	/**
	 * Konstruktor untuk konteks angket Guru.
	 *
	 * @param isiAngketParameterUmum      data pengisian angket yang sedang diedit
	 * @param parameterRows               daftar baris ZK parameter tambahan yang akan dikelola
	 * @param lampiranLains               peta lampiran yang sudah diunggah, dikunci per {@code jenis}
	 * @param rows                        kontainer baris grid tempat komponen dirender
	 * @param grupChecklistPenilaianGuru  grup checklist penilaian guru target
	 */
	public IsiAngketParameterUmumListener(IsiAngketParameterUmum isiAngketParameterUmum, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, GrupChecklistPenilaianGuru grupChecklistPenilaianGuru) {
		this(isiAngketParameterUmum, parameterRows, lampiranLains, rows, null, null, grupChecklistPenilaianGuru);
	}

	/**
	 * Konstruktor induk privat yang menyatukan ketiga varian konteks grup checklist (Umum/Dosen/Guru).
	 *
	 * @param isiAngketParameterUmum      data pengisian angket yang sedang diedit
	 * @param parameterRows               daftar baris ZK parameter tambahan yang akan dikelola
	 * @param lampiranLains               peta lampiran yang sudah diunggah, dikunci per {@code jenis}
	 * @param rows                        kontainer baris grid tempat komponen dirender
	 * @param grupChecklistPenilaianUmum  grup checklist penilaian umum target, atau {@code null}
	 * @param grupChecklistPenilaianDosen grup checklist penilaian dosen target, atau {@code null}
	 * @param grupChecklistPenilaianGuru  grup checklist penilaian guru target, atau {@code null}
	 */
	private IsiAngketParameterUmumListener(IsiAngketParameterUmum isiAngketParameterUmum, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, GrupChecklistPenilaianUmum grupChecklistPenilaianUmum,
			GrupChecklistPenilaianDosen grupChecklistPenilaianDosen,
			GrupChecklistPenilaianGuru grupChecklistPenilaianGuru) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.isiAngketParameterUmum = isiAngketParameterUmum;
		this.lampiranLains = lampiranLains;
		this.grupChecklistPenilaianUmum = grupChecklistPenilaianUmum;
		this.grupChecklistPenilaianDosen = grupChecklistPenilaianDosen;
		this.grupChecklistPenilaianGuru = grupChecklistPenilaianGuru;
	}

	/**
	 * Memvalidasi seluruh parameter tambahan yang sedang dirender: field wajib diisi
	 * dan lampiran wajib diunggah.
	 *
	 * <p>Untuk tiap baris pada {@link #parameterRows} yang memiliki atribut {@code parameterTambahan},
	 * dibangun {@code jenis} lewat {@link #buildJenis(Row, ParameterTambahan)} (jalur yang sudah
	 * memakai namespace ber-{@code resolveJenisParameterTambahan} untuk cabang Umum — lihat catatan
	 * keamanan pada Javadoc kelas), lalu diperiksa nilai isian ({@link ParameterTambahan#ambilVal})
	 * dan (bila dikonfigurasi wajib) keberadaan lampiran pada {@link #lampiranLains}. Pesan
	 * peringatan ditampilkan pada pelanggaran pertama yang ditemukan dan validasi langsung
	 * dihentikan.</p>
	 *
	 * @return {@code true} bila semua parameter tambahan valid (atau tidak ada yang perlu
	 *         divalidasi); {@code false} pada pelanggaran pertama yang ditemukan
	 * @throws Exception diteruskan dari operasi ZK/akses atribut baris
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			if (parameterTambahan == null) {
				continue;
			}
			String jenis = buildJenis(row, parameterTambahan);
			if (jenis == null || jenis.trim().isEmpty()) {
				continue;
			}

			String val = ParameterTambahan.ambilVal(row, parameterTambahan);

			if (parameterTambahan.getWajibDiisi()
					&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
				MyMessageboxConfig.show("Pilihan \"" + parameterTambahan.getLabelInputan() + "\" harus dipilih",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (parameterTambahan.getLampiranWajibDiisi()) {
				if (parameterTambahan.getHarusMenyertakanLampiran()
						&& (lampiranLains == null || !lampiranLains.keySet().contains(jenis))) {
					MyMessageboxConfig.show(
							"Untuk pilihan \"" + parameterTambahan.getLabelInputan() + "\", lampiran harus di-upload",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Menyimpan pengisian angket dalam transaksi Hibernate tersendiri (sesi baru, terlepas
	 * dari sesi ambien lain), setelah memopulasikan string jawaban parameter tambahan.
	 *
	 * <p>Memuat ulang entity dari database bila sudah memiliki id (agar tidak menimpa
	 * perubahan lain di luar sesi ini), lalu memanggil
	 * {@code managed.populateParameterTambahan(parameterRows)} — lihat catatan keamanan
	 * pada Javadoc kelas mengenai jalur {@code LampiranLain.ambil(...)} tanpa namespace
	 * pada method entity tsb — dan {@link #pastikanTidakSetTbmuserUntukPeserta(IsiAngketParameterUmum)}
	 * sebelum {@code saveOrUpdate}. Transaksi di-rollback dan kegagalan dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)} bila terjadi exception; sesi selalu
	 * ditutup pada blok {@code finally}.</p>
	 *
	 * @param isiAngketParameterUmum data pengisian angket yang akan disimpan; bila sudah
	 *                               memiliki id, baris terkelola (managed) yang dimuat ulang
	 *                               dari database yang dipakai, bukan instance ini
	 */
	public void onSave(IsiAngketParameterUmum isiAngketParameterUmum) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			IsiAngketParameterUmum managed = isiAngketParameterUmum;
			if (managed != null && managed.getId() != null) {
				managed = (IsiAngketParameterUmum) session.get(IsiAngketParameterUmum.class, managed.getId());
			}
			if (managed == null) {
				managed = isiAngketParameterUmum;
			}
			managed.populateParameterTambahan(parameterRows);
			pastikanTidakSetTbmuserUntukPeserta(managed);
			session.saveOrUpdate(managed);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:130");
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:138");
				}
			}
		}
	}

	/**
	 * Menjaga agar baris pengisian angket milik peserta bertipe entity spesifik
	 * (mahasiswa/siswa/dosen/guru) tidak juga membawa referensi {@code tbmuser}.
	 *
	 * <p>Bila salah satu dari {@code getMahasiswa()}, {@code getSiswa()}, {@code getDosen()},
	 * atau {@code getGuru()} tidak {@code null}, {@code tbmuser} dipaksa {@code null} — mencegah
	 * baris yang seharusnya diatribusikan ke entity spesifik tsb malah ikut tertaut ke akun
	 * {@code Tbmuser} generik (mis. akibat sisa state dari pengisian sebelumnya). Kegagalan
	 * tak terduga saat memeriksa relasi diabaikan diam-diam (dicatat via
	 * {@code ais.common.ErrorAuditUtil}) agar tidak menghentikan proses simpan.</p>
	 *
	 * @param isi data pengisian angket yang akan diperiksa; tidak melakukan apa pun bila {@code null}
	 */
	private void pastikanTidakSetTbmuserUntukPeserta(IsiAngketParameterUmum isi) {
		if (isi == null) {
			return;
		}
		try {
			if (isi.getMahasiswa() != null || isi.getSiswa() != null || isi.getDosen() != null || isi.getGuru() != null) {
				isi.setTbmuser(null);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:152");
		}
	}

	/**
	 * Merender ulang seluruh baris komponen parameter tambahan untuk grup checklist target
	 * (Umum/Dosen/Guru), dipanggil ulang tiap kali konteks yang mempengaruhi daftar parameter
	 * berubah (mis. event ZK pemicu terkait).
	 *
	 * <p>Membersihkan baris lama ({@link #clearParameterRows()}), menentukan grup target lewat
	 * {@link #resolveGrupTarget()}, lalu mengambil daftar {@link ParameterTambahan} yang berlaku
	 * untuk grup tsb (query berbeda tergantung tipe grup) dan mengurutkannya. Untuk tiap parameter,
	 * dibangun {@code jenis} lewat {@link #buildJenis(Object, ParameterTambahan)}, nilai jawaban
	 * tersimpan diambil dari {@code isiAngketParameterUmum.getParameterTambahanInds()} (format teks
	 * baris demi baris dipisah {@code "<=>"}), lalu komponen input dirender lewat
	 * {@link ParameterTambahan#initComponent}. Baris label grup disembunyikan bila tidak ada satu
	 * pun parameter yang akhirnya tampil.</p>
	 *
	 * @param event event ZK pemicu (isinya tidak dipakai secara langsung)
	 * @throws Exception diteruskan dari operasi ZK/Hibernate di bawahnya
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {
		clearParameterRows();

		Session session = HibernateUtil.currentSession();
		Object grup = resolveGrupTarget();
		if (grup == null || rows == null) {
			return;
		}

		MyFormRow rowParameterTambahan = new MyFormRow();
		rowParameterTambahan.setVisible(false);
		rowParameterTambahan.setStyle("border:0px;background: transparent;");
		rowParameterTambahan.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
		rowParameterTambahan.appendChild(new MyLabelBold(getGrupLabel(grup)));
		parameterRows.add(rowParameterTambahan);

		List<ParameterTambahan> parameterTambahans;
		if (grup instanceof GrupChecklistPenilaianDosen) {
			parameterTambahans = session.createCriteria(ParameterTambahanAngketUmum.class)
					.add(Restrictions.eq("grupChecklistPenilaianDosen", grup))
					.setProjection(Projections.groupProperty("parameterTambahan")).list();
		} else if (grup instanceof GrupChecklistPenilaianGuru) {
			parameterTambahans = session.createCriteria(ParameterTambahanAngketUmum.class)
					.add(Restrictions.eq("grupChecklistPenilaianGuru", grup))
					.setProjection(Projections.groupProperty("parameterTambahan")).list();
		} else {
			parameterTambahans = session.createCriteria(ParameterTambahanAngketUmum.class)
					.add(Restrictions.eq("grupChecklistPenilaianUmum", grup))
					.setProjection(Projections.groupProperty("parameterTambahan")).list();
		}

		if (parameterTambahans == null) {
			return;
		}
		Collections.sort(parameterTambahans);

		EventListener isi = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				isiAngketParameterUmum.populateParameterTambahan(parameterRows);
			}
		};

		boolean tampil = false;
		rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
		for (ParameterTambahan parameterTambahan : parameterTambahans) {
			String jenis = buildJenis(grup, parameterTambahan);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setAttribute("parameterTambahan", parameterTambahan);
			row.setAttribute("grupChecklistPenilaianUmum", grup instanceof GrupChecklistPenilaianUmum ? grup : null);
			row.setAttribute("grupChecklistPenilaianDosen", grup instanceof GrupChecklistPenilaianDosen ? grup : null);
			row.setAttribute("grupChecklistPenilaianGuru", grup instanceof GrupChecklistPenilaianGuru ? grup : null);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
			if (parameterTambahan.getKeterangan() != null && !parameterTambahan.getKeterangan().trim().isEmpty()) {
				parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
			}

			String val = "";
			String ket = "";
			String[] spl = isiAngketParameterUmum.getParameterTambahanInds().split("\\n");
			for (String d : spl) {
				String[] value = d.split("<=>");
				if (value.length > 0 && value[0].trim().equalsIgnoreCase(jenis)) {
					val = value.length > 1 ? value[1].trim() : "";
					try {
						ket = value.length > 0 ? value[value.length - 1] : "";
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:228");
					}
				}
			}

			tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
					isiAngketParameterUmum.getId(), val, ket, parameterTambahan, isi);
		}
		rowParameterTambahan.setVisible(tampil);
	}

	/**
	 * Menyembunyikan dan mengosongkan daftar baris komponen parameter tambahan hasil render
	 * sebelumnya, sebagai persiapan sebelum {@link #onEvent(Event)} merender ulang.
	 *
	 * <p>Aman dipanggil meski {@link #parameterRows} {@code null}; kegagalan tak terduga saat
	 * menyembunyikan satu baris diabaikan diam-diam (dicatat via {@code ais.common.ErrorAuditUtil})
	 * agar baris lain tetap diproses.</p>
	 */
	private void clearParameterRows() {
		if (parameterRows == null) {
			return;
		}
		for (Row row : parameterRows) {
			try {
				row.setVisible(false);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:246");
			}
		}
		parameterRows.clear();
	}

	/**
	 * Menentukan grup checklist penilaian target yang akan dipakai untuk mengambil daftar
	 * parameter tambahan.
	 *
	 * <p>Prioritas: field yang di-set eksplisit di konstruktor ({@link #grupChecklistPenilaianDosen},
	 * lalu {@link #grupChecklistPenilaianGuru}, lalu {@link #grupChecklistPenilaianUmum}); bila
	 * ketiganya {@code null} (konstruktor lama tanpa grup eksplisit), diresolusi dari
	 * {@code isiAngketParameterUmum.getJadwalChecklistPenilaianUmum().getGrupChecklistPenilaianUmum()}.
	 * Kegagalan tak terduga pada jalur fallback ini diabaikan diam-diam (dicatat via
	 * {@code ais.common.ErrorAuditUtil}).</p>
	 *
	 * @return instance {@link GrupChecklistPenilaianDosen}, {@link GrupChecklistPenilaianGuru},
	 *         atau {@link GrupChecklistPenilaianUmum} yang berlaku; {@code null} bila tidak ada
	 *         satu pun yang dapat ditentukan
	 */
	private Object resolveGrupTarget() {
		if (grupChecklistPenilaianDosen != null) {
			return grupChecklistPenilaianDosen;
		}
		if (grupChecklistPenilaianGuru != null) {
			return grupChecklistPenilaianGuru;
		}
		if (grupChecklistPenilaianUmum != null) {
			return grupChecklistPenilaianUmum;
		}
		try {
			if (isiAngketParameterUmum != null && isiAngketParameterUmum.getJadwalChecklistPenilaianUmum() != null) {
				return isiAngketParameterUmum.getJadwalChecklistPenilaianUmum().getGrupChecklistPenilaianUmum();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:266");
		}
		return null;
	}

	/**
	 * Membangun label judul yang ditampilkan di atas daftar parameter tambahan, sesuai tipe grup.
	 *
	 * @param grup instance {@link GrupChecklistPenilaianDosen}, {@link GrupChecklistPenilaianGuru},
	 *             {@link GrupChecklistPenilaianUmum}, atau tipe lain/{@code null}
	 * @return label deskriptif untuk tipe grup yang dikenali; label generik
	 *         {@code "Parameter Tambahan Angket"} untuk tipe lain/{@code null}
	 */
	private String getGrupLabel(Object grup) {
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return "Parameter Tambahan Angket Dosen - " + ((GrupChecklistPenilaianDosen) grup).getIsi();
		}
		if (grup instanceof GrupChecklistPenilaianGuru) {
			return "Parameter Tambahan Angket Guru - " + ((GrupChecklistPenilaianGuru) grup).getIsi();
		}
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return "Parameter Tambahan Angket Umum - " + ((GrupChecklistPenilaianUmum) grup).getIsi();
		}
		return "Parameter Tambahan Angket";
	}

	/**
	 * Overload yang membaca tipe grup dari atribut yang sudah disimpan pada {@code row} saat
	 * render ({@link #onEvent(Event)}), lalu mendelegasikan ke {@link #buildJenis(Object, ParameterTambahan)}.
	 *
	 * <p>Dipakai oleh {@link #validate()}, yang hanya memiliki akses ke baris ZK (bukan objek
	 * grup itu sendiri).</p>
	 *
	 * @param row              baris ZK yang membawa atribut {@code grupChecklistPenilaianDosen}/
	 *                         {@code grupChecklistPenilaianGuru}/{@code grupChecklistPenilaianUmum}
	 * @param parameterTambahan parameter tambahan terkait baris ini
	 * @return {@code jenis} efektif untuk pasangan grup-parameter ini, atau {@code null} bila
	 *         tidak ada atribut grup yang dikenali pada {@code row}
	 */
	private String buildJenis(Row row, ParameterTambahan parameterTambahan) {
		Object grup = row.getAttribute("grupChecklistPenilaianDosen");
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return buildJenis(grup, parameterTambahan);
		}
		grup = row.getAttribute("grupChecklistPenilaianGuru");
		if (grup instanceof GrupChecklistPenilaianGuru) {
			return buildJenis(grup, parameterTambahan);
		}
		grup = row.getAttribute("grupChecklistPenilaianUmum");
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return buildJenis(grup, parameterTambahan);
		}
		return null;
	}

	/**
	 * Membangun penanda {@code jenis} lampiran/jawaban untuk pasangan grup-parameter, dipakai
	 * sebagai kunci pencocokan pada tabel {@code lampiran_lain} dan pada string jawaban tersimpan.
	 *
	 * <p>Cabang Dosen dan Guru memakai format lama dengan penanda literal ({@code "DOSEN:"}/
	 * {@code "GURU:"}) yang sudah unik lintas-entitas. Cabang Umum memakai
	 * {@link LampiranLain#resolveJenisParameterTambahan(Class, Long, String)} dengan
	 * {@code ownerClass = GrupChecklistPenilaianUmum.class} — perbaikan r83937/task_b82b25d2 yang
	 * menutup tabrakan namespace lintas-entitas untuk jalur render/validasi. <b>Catatan:</b>
	 * entity {@link IsiAngketParameterUmum} memiliki implementasi {@code buildJenis} miliknya
	 * SENDIRI (dipakai pada {@code populateParameterTambahan}) yang TIDAK memanggil
	 * {@code resolveJenisParameterTambahan} — lihat catatan keamanan pada Javadoc kelas ini.</p>
	 *
	 * @param grup              instance {@link GrupChecklistPenilaianDosen},
	 *                          {@link GrupChecklistPenilaianGuru}, atau {@link GrupChecklistPenilaianUmum}
	 * @param parameterTambahan parameter tambahan terkait; harus memiliki id
	 * @return {@code jenis} efektif; string kosong bila {@code grup}/{@code parameterTambahan}/id-nya {@code null}
	 *         atau tipe {@code grup} tidak dikenali
	 */
	private String buildJenis(Object grup, ParameterTambahan parameterTambahan) {
		if (grup == null || parameterTambahan == null || parameterTambahan.getId() == null) {
			return "";
		}
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return "DOSEN:" + ((GrupChecklistPenilaianDosen) grup).getId() + "->" + parameterTambahan.getId();
		}
		if (grup instanceof GrupChecklistPenilaianGuru) {
			return "GURU:" + ((GrupChecklistPenilaianGuru) grup).getId() + "->" + parameterTambahan.getId();
		}
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return LampiranLain.resolveJenisParameterTambahan(GrupChecklistPenilaianUmum.class,
					isiAngketParameterUmum == null ? null : isiAngketParameterUmum.getId(),
					((GrupChecklistPenilaianUmum) grup).getId() + "->" + parameterTambahan.getId());
		}
		return "";
	}
}
