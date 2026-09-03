package ais.action.master.helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.KelompokParameterTambahanAlumni;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanAlumni;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * {@link EventListener} yang membangun dan mengelola blok "parameter tambahan" pada formulir
 * biodata alumni ({@link BiodataMahasiswa} yang sudah lulus) — field kustom dinamis (per {@link
 * ParameterTambahan}, dikelompokkan oleh {@link KelompokParameterTambahanAlumni}) yang dapat
 * dikonfigurasi admin, mis. kuesioner tracer study/data kealumnian (status pekerjaan, instansi
 * tempat bekerja, dsb) beserta lampiran pendukungnya. Dibanding sepupunya {@link
 * ParameterTambahanMahasiswaListener} untuk biodata mahasiswa aktif, kelas ini menambahkan dua
 * hal: (1) parameter dapat berjenjang/nested — satu {@link ParameterTambahan} bisa punya
 * {@code parent}, dan child-nya dirender sebagai sub-grid berindentasi lewat rekursi {@link
 * #displayRinci}; (2) tampilnya sebuah field bisa BERSYARAT terhadap jawaban field lain lewat
 * JSON {@code syaratTampil} pada {@link ParameterTambahan}, dievaluasi oleh {@link
 * #lolosSyaratTampil} (lihat javadoc method tsb untuk format JSON-nya).
 *
 * <p>
 * Field yang ditampilkan disaring berdasarkan tahun angkatan alumni ({@code
 * tampilDiSemuaTahunAngkatan} atau {@code tahunAngkatans} memuat angkatan ybs — dicek dengan
 * {@code ILIKE '%;<gel>;%'}), status aktif kelompok/parameter, dan flag {@link
 * #digunakanUntukPenggunaAlumni} yang membedakan konteks pengisian oleh admin/operator vs oleh
 * alumni sendiri (mis. lewat portal alumni) — dikendalikan lewat kolom {@code
 * kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni} pada tiap query Hibernate
 * Criteria di kelas ini.
 * </p>
 *
 * <p>
 * Nilai tersimpan sebagai teks terserialisasi baris-per-baris pada {@code
 * biodataMahasiswa.getParameterTambahanIndsAlumni()} (perhatikan: field alumni terpisah dari
 * {@code getParameterTambahanInds()} milik mahasiswa aktif), dengan format
 * {@code "<idKelompok>-><idParameter><=>nilai<=>...<=>keterangan"} per baris, di-parse oleh
 * {@link #parseParameterMap} sekali di awal ({@link #onEvent}/{@link #validate}) lalu dipakai
 * sebagai lookup O(1) — dulunya diparse berulang di dalam loop, komentar {@code OPTIMASI} di
 * badan method menandai bekas optimasi tersebut.
 * </p>
 *
 * <p>
 * {@link #onEvent} (dipasang sebagai listener perubahan konteks, mis. saat tahun angkatan
 * berubah) membangun ulang seluruh baris field dari awal, termasuk sub-grid nested-nya; {@link
 * #onSave} menyerahkan proses pengumpulan nilai balik ke {@link
 * BiodataMahasiswa#populateParameterTambahanAlumni}; {@link #validate} adalah method statis
 * independen (tidak memerlukan instance dibangun via {@link #onEvent} lebih dulu) yang memeriksa
 * parameter wajib diisi/wajib lampiran dan menampilkan pesan peringatan bila ada yang belum
 * lengkap; {@link #check} hanya memeriksa apakah ada parameter tambahan relevan sama sekali
 * untuk alumni ini (dipakai untuk menyembunyikan seluruh blok bila tidak ada konfigurasi yang
 * berlaku).
 * </p>
 */
public class ParameterTambahanAlumniListener implements EventListener {

	/** Baris {@link Row} ZK yang sudah dibangun oleh {@link #onEvent}/{@link #displayRinci}; dibaca ulang oleh {@link #onSave} untuk memanen nilai isian. */
	private List<Row> parameterRows;
	/** Container {@link Rows} ZK tempat baris-baris field parameter tambahan disisipkan (parent dari {@link #parameterRows}). */
	private Rows rows;
	/** Biodata alumni yang parameter tambahannya sedang dikelola/ditampilkan/divalidasi oleh instance ini. */
	private BiodataMahasiswa biodataMahasiswa;
	/** Peta lampiran yang sudah diunggah per kunci parameter ({@code idKelompok->idParameter}), diteruskan ke {@link ParameterTambahan#initComponent}. */
	private Map<String, LampiranLain> lampiranLains;
	/** Membedakan konteks pengisian: {@code true} bila form ini diakses alumni sendiri (portal alumni), {@code false}/null bila diisi admin/operator — menentukan filter {@code digunakanUntukPenggunaAlumni} pada tiap query di kelas ini. */
	private Boolean digunakanUntukPenggunaAlumni;

	/**
	 * @param biodataMahasiswa              biodata alumni yang parameter tambahannya dikelola
	 * @param parameterRows                 daftar baris ZK yang dibangun/dikelola listener ini (dibaca ulang oleh {@link #onSave})
	 * @param lampiranLains                 peta lampiran yang sudah diunggah per kunci parameter, diteruskan ke {@link ParameterTambahan#initComponent}
	 * @param rows                          container ZK tempat baris field disisipkan
	 * @param digunakanUntukPenggunaAlumni  {@code true} bila konteks pengisian oleh alumni sendiri, {@code false}/null bila oleh admin/operator
	 */
	public ParameterTambahanAlumniListener(BiodataMahasiswa biodataMahasiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, Boolean digunakanUntukPenggunaAlumni) {
		this.digunakanUntukPenggunaAlumni = digunakanUntukPenggunaAlumni;
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.biodataMahasiswa = biodataMahasiswa;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Mem-parsing teks jawaban parameter tambahan alumni ({@code
	 * biodataMahasiswa.getParameterTambahanIndsAlumni()}, format baris-per-baris {@code
	 * "<idKelompok>-><idParameter><=>nilai<=>...<=>keterangan"}) menjadi peta lookup O(1) berkunci
	 * {@code idKelompok->idParameter} (huruf kecil semua). Dipanggil sekali di awal {@link
	 * #onEvent}/{@link #validate} agar {@code split("\n")}/{@code split("<=>")} tidak diulang di
	 * dalam loop bersarang (komentar {@code OPTIMASI} menandai perbaikan performa ini).
	 *
	 * @param rawParam teks mentah jawaban parameter tambahan alumni; boleh {@code null}/kosong
	 * @return peta {@code jenisKey -> array hasil split("<=>")} baris tsb; peta kosong bila {@code rawParam} kosong
	 */
	private static Map<String, String[]> parseParameterMap(String rawParam) {
		Map<String, String[]> mapParam = new HashMap<String, String[]>();
		if (rawParam != null && !rawParam.trim().isEmpty()) {
			String[] spl = rawParam.split("\n");
			for (String d : spl) {
				if (!d.trim().isEmpty()) {
					String[] value = d.split("<=>");
					if (value.length > 0) {
						mapParam.put(value[0].trim().toLowerCase(), value);
					}
				}
			}
		}
		return mapParam;
	}

	/**
	 * Memvalidasi bahwa seluruh parameter tambahan alumni yang wajib (aktif, relevan dengan tahun
	 * angkatan alumni, dan sesuai konteks {@code digunakanUntukPenggunaAlumni}) sudah diisi —
	 * termasuk lampiran wajib bila parameter mensyaratkan {@code lampiranWajibDiisi &&
	 * harusMenyertakanLampiran}. Method statis independen, membuka {@link Session} Hibernate
	 * sendiri (query {@link ParameterTambahanAlumni} via Criteria, alias ke {@code
	 * parameterTambahan} dan {@code kelompokParameterTambahanAlumni}) dan menutupnya sebelum
	 * mengevaluasi jawaban. Berhenti pada kegagalan validasi PERTAMA yang ditemukan (tidak
	 * mengumpulkan semua kegagalan sekaligus). Catatan: parameter yang punya {@code syaratTampil}
	 * (field bersyarat) TIDAK dikecualikan di sini — validasi wajib-isi berjalan lepas dari
	 * status tampil/sembunyi di UI.
	 *
	 * @param biodataMahasiswa              biodata alumni yang divalidasi; {@code null} langsung mengembalikan {@code false}
	 * @param eventListener                 listener yang di-passing sebagai target fokus pada messagebox ({@code tampilMessage=true}), atau dipanggil langsung ({@code onEvent(null)}) bila {@code tampilMessage=false}
	 * @param tampilMessage                 {@code true} untuk menampilkan {@link MyMessageboxConfig} peringatan; {@code false} untuk memanggil {@code eventListener.onEvent(null)} secara diam-diam
	 * @param digunakanUntukPenggunaAlumni  filter konteks pengisian, sama seperti field instance dengan nama sama
	 * @return {@code true} bila semua parameter wajib (dan lampirannya) sudah lengkap; {@code false} pada kegagalan validasi pertama
	 * @throws Exception diteruskan dari {@code eventListener.onEvent(null)}
	 */
	@SuppressWarnings("unchecked")
	public static boolean validate(BiodataMahasiswa biodataMahasiswa, EventListener eventListener,
			final Boolean tampilMessage, Boolean digunakanUntukPenggunaAlumni) throws Exception {

		if (biodataMahasiswa == null) return false;

		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null : biodataMahasiswa.getMahasiswa().getTahunangkatan();
		
		Session session = null;
		List<ParameterTambahanAlumni> parameterTambahanAlumnis = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			parameterTambahanAlumnis = session.createCriteria(ParameterTambahanAlumni.class)
					.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
							gel == null ? Restrictions.sqlRestriction("false")
									: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
					.createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanAlumni", "kelompokParameterTambahanAlumni")
					.add(digunakanUntukPenggunaAlumni
							? Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", true)
							: Restrictions.or(
									Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", false),
									Restrictions.isNull("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni")))
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanAlumni.aktif", true)).list();

		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanAlumniListener.java:97");}
			}
		}

		if (parameterTambahanAlumnis == null || parameterTambahanAlumnis.isEmpty()) {
			return true;
		}

		// OPTIMASI: Parsing parameter tambahan 1x saja O(1) Lookup
		Map<String, String[]> mapValParam = parseParameterMap(biodataMahasiswa.getParameterTambahanIndsAlumni());

		for (ParameterTambahanAlumni parameterTambahanAlumni : parameterTambahanAlumnis) {
			ParameterTambahan parameterTambahan = parameterTambahanAlumni.getParameterTambahan();
			KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni = parameterTambahanAlumni.getKelompokParameterTambahanAlumni();

			if (parameterTambahan != null && kelompokParameterTambahanAlumni != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(BiodataMahasiswa.class,
						biodataMahasiswa.getId(),
						kelompokParameterTambahanAlumni.getId() + "->" + parameterTambahan.getId());
				String jenisKey = jenis.toLowerCase();

				String val = "";
				if (mapValParam.containsKey(jenisKey)) {
					String[] value = mapValParam.get(jenisKey);
					val = value.length > 1 ? value[1].trim() : "";
				}

				boolean isTipeInputanValid = parameterTambahanAlumni.getParameterTambahan() != null && !parameterTambahanAlumni.getParameterTambahan().getTipeDataInputan().equals(ParameterTambahan.TIDAK_ADA);
				boolean isEmptyValue = (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"));
				boolean wajib = parameterTambahanAlumni.getWajibDiisi() && isTipeInputanValid && isEmptyValue;

				if (wajib) {
					if (tampilMessage) {
						MyMessageboxConfig.show(
								"\"" + kelompokParameterTambahanAlumni.getNama()
										+ "\" harus Anda lengkapi !\n\nPilihan \"" + parameterTambahan.getLabelInputan()
										+ "\" harus dipilih",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, eventListener);
					} else {
						if (eventListener != null) {
							eventListener.onEvent(null);
						}
					}
					return false;
				}

				if (parameterTambahan.getLampiranWajibDiisi() && parameterTambahan.getHarusMenyertakanLampiran()) {
					LampiranLain lam = LampiranLain.ambil(biodataMahasiswa.getId(), jenis);
					if (lam == null) {
						if (tampilMessage) {
							MyMessageboxConfig.show(
									"\"" + kelompokParameterTambahanAlumni.getNama()
											+ "\" harus Anda lengkapi !\n\nUntuk pilihan \""
											+ parameterTambahan.getLabelInputan() + "\", lampiran harus diunggah",
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
									eventListener);
						} else {
							if (eventListener != null) {
								eventListener.onEvent(null);
							}
						}
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Mengumpulkan nilai seluruh field parameter tambahan alumni yang sedang ditampilkan
	 * ({@link #parameterRows}) kembali ke {@code biodataMahasiswa}, lewat {@link
	 * BiodataMahasiswa#populateParameterTambahanAlumni}. Tidak melakukan apa-apa bila {@code
	 * biodataMahasiswa} atau {@link #parameterRows} {@code null}.
	 *
	 * @param biodataMahasiswa objek biodata alumni yang akan diisi nilai parameter tambahannya (state-nya dimutasi in-place)
	 */
	public void onSave(BiodataMahasiswa biodataMahasiswa) {
		if (biodataMahasiswa != null && parameterRows != null) {
			biodataMahasiswa.populateParameterTambahanAlumni(parameterRows);
		}
	}

	/**
	 * Memeriksa apakah ada minimal satu {@link ParameterTambahanAlumni} aktif yang relevan untuk
	 * {@link #biodataMahasiswa} (sesuai tahun angkatan dan konteks {@link
	 * #digunakanUntukPenggunaAlumni}), tanpa memuat datanya — hanya {@code COUNT(*)} via {@link
	 * Projections#rowCount()}. Dipakai untuk menyembunyikan seluruh blok UI parameter tambahan
	 * bila tidak ada konfigurasi yang berlaku untuk alumni ybs. Error Hibernate ditangkap dan
	 * dicatat ke {@code ErrorAuditUtil} (bukan dilempar ulang) — pada kegagalan query, {@code
	 * count} tetap 0 sehingga method mengembalikan {@code false}.
	 *
	 * @return {@code true} bila ada minimal satu parameter tambahan alumni relevan; {@code false} bila tidak ada atau {@link #biodataMahasiswa} {@code null}
	 */
	public boolean check() {
		if (biodataMahasiswa == null) return false;

		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null : biodataMahasiswa.getMahasiswa().getTahunangkatan();
		int count = 0;
		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			count = ((Number) session.createCriteria(ParameterTambahanAlumni.class)
					.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
							gel == null ? Restrictions.sqlRestriction("false")
									: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
					.createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanAlumni", "kelompokParameterTambahanAlumni")
					.add(digunakanUntukPenggunaAlumni
							? Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", true)
							: Restrictions.or(
									Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", false),
									Restrictions.isNull("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni")))
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanAlumni.aktif", true))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ParameterTambahanAlumniListener.java:194");
		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanAlumniListener.java:197");}
			}
		}

		return count != 0;
	}

	/**
	 * Membangun ulang dari awal seluruh baris field parameter tambahan alumni: menyembunyikan dan
	 * mengosongkan baris lama ({@link #parameterRows}), lalu untuk tiap {@link
	 * KelompokParameterTambahanAlumni} aktif & relevan (diquery via Hibernate Criteria, di-{@code
	 * groupProperty} pada id kelompok lalu diurutkan dengan {@link Collections#sort}) membuat satu
	 * baris judul kelompok ({@link MyLabelStyled}) diikuti seluruh field-nya lewat {@link
	 * #displayRinci} (termasuk field bersarang/nested-nya). Dipasang sebagai listener perubahan
	 * konteks pada UI (mis. saat tahun angkatan alumni berubah, form parameter tambahan perlu
	 * dibangun ulang karena filter tahun angkatan berubah). Membuka dan menutup {@link Session}
	 * Hibernate sendiri di dalam satu pemanggilan.
	 *
	 * @param event event ZK pemicu; TIDAK dipakai isinya oleh implementasi ini (parameter kontrak {@link EventListener} semata)
	 * @throws Exception diteruskan dari operasi Hibernate/ZK di dalamnya
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		if (parameterRows != null) {
			for (Row row : parameterRows) {
				row.setVisible(false);
			}
			parameterRows.clear();
		}

		if (biodataMahasiswa == null) return;

		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null : biodataMahasiswa.getMahasiswa().getTahunangkatan();
		
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			
			List<KelompokParameterTambahanAlumni> kelompokParameterTambahanAlumnis = ConstantValues.simpleList(
					session.createCriteria(ParameterTambahanAlumni.class)
							.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
									gel == null ? Restrictions.sqlRestriction("false")
											: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
							.createAlias("parameterTambahan", "parameterTambahan")
							.createAlias("kelompokParameterTambahanAlumni", "kelompokParameterTambahanAlumni")
							.add(digunakanUntukPenggunaAlumni
									? Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", true)
									: Restrictions.or(
											Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", false),
											Restrictions.isNull("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni")))
							.add(Restrictions.eq("parameterTambahan.aktif", true))
							.add(Restrictions.eq("kelompokParameterTambahanAlumni.aktif", true))
							.setProjection(Projections.groupProperty("kelompokParameterTambahanAlumni.id")),
					KelompokParameterTambahanAlumni.class, false);
					
			Collections.sort(kelompokParameterTambahanAlumnis);

			// OPTIMASI: Parsing data string ke dalam Map sekali saja sebelum looping bersarang
			Map<String, String[]> mapValParam = parseParameterMap(biodataMahasiswa.getParameterTambahanIndsAlumni());

			for (KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni : kelompokParameterTambahanAlumnis) {
				MyFormRow rowParameterTambahan = new MyFormRow();
				rowParameterTambahan.setStyle("border:0px;background: transparent;");
				rowParameterTambahan.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
				rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanAlumni.getNama() + ""));
				
				parameterRows.add(rowParameterTambahan);

				displayRinci(rowParameterTambahan, rows, session, kelompokParameterTambahanAlumni, gel, null, 0, mapValParam);
			}

		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanAlumniListener.java:259");}
			}
		}
	}

	/**
	 * Merender satu level field {@link ParameterTambahan} milik {@code
	 * kelompokParameterTambahanAlumni} yang parent-nya adalah {@code parent} (atau parameter
	 * tingkat-atas bila {@code parent == null}) ke dalam {@code rowsUtama}, lalu memanggil
	 * dirinya sendiri secara REKURSIF untuk merender field CHILD dari tiap parameter yang punya
	 * anak (dideteksi lewat {@code ConstantValues.ambilBerdasarClass(ParameterTambahan.class)},
	 * dicari parameter lain yang {@code getParent().getId()} sama dengan id parameter ini).
	 * Field child dirender di dalam {@link Grid} tersendiri (kelas CSS {@code "dgrid fgrid"},
	 * indentasi {@code padding-left: 50px}) yang disisipkan sebagai baris di bawah field induk.
	 *
	 * <p>Untuk tiap parameter: nilai jawaban tersimpan (dan keterangannya bila ada) diambil dari
	 * {@code mapValParam} (hasil {@link #parseParameterMap}), lalu komponen input dibangun lewat
	 * {@link ParameterTambahan#initComponent} (tipe komponen mengikuti {@code
	 * tipeDataInputan} parameter — text/pilihan/lampiran/dsb). Visibilitas baris ditentukan oleh
	 * {@link #lolosSyaratTampil} — field dengan {@code syaratTampil} yang belum terpenuhi TETAP
	 * dibangun komponennya (agar nilainya tetap ikut ter-submit/tersimpan bila syaratnya nanti
	 * terpenuhi lewat interaksi lain) tetapi barisnya disembunyikan ({@code row.setVisible(false)}).
	 * Baris judul kelompok ({@code rowParameterTambahan}) ikut disembunyikan bila TIDAK ADA field
	 * di dalamnya yang benar-benar tampil ({@code tampil} diakumulasi dari {@code adaKomponen &&
	 * lolosSyarat} tiap field).</p>
	 *
	 * @param rowParameterTambahan               baris judul kelompok ({@link #onEvent}); disembunyikan di akhir bila tak ada field yang tampil di dalamnya
	 * @param rowsUtama                          container {@link Rows} tempat baris field level ini disisipkan (berbeda dari {@link #rows} bila dipanggil rekursif untuk child, karena child punya {@link Grid} tersendiri)
	 * @param session                            sesi Hibernate aktif yang dipakai untuk query (dibuka/ditutup oleh {@link #onEvent} pemanggil, TIDAK oleh method ini)
	 * @param kelompokParameterTambahanAlumni    kelompok parameter yang sedang dirender
	 * @param gel                                tahun angkatan alumni, dipakai untuk filter {@code tahunAngkatans}; {@code null} bila mahasiswa tak diketahui angkatannya
	 * @param parent                             parameter induk untuk level rekursi ini; {@code null}/id {@code null} berarti level tingkat-atas
	 * @param indexParent                        kedalaman rekursi (0 = tingkat atas); diteruskan bertambah tiap level tapi TIDAK dipakai untuk logika apa pun di method ini selain diteruskan ke rekursi berikutnya
	 * @param mapValParam                        peta jawaban tersimpan hasil {@link #parseParameterMap}, dipakai untuk mengisi nilai awal komponen dan mengevaluasi {@link #lolosSyaratTampil}
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void displayRinci(Row rowParameterTambahan, Rows rowsUtama, Session session,
			KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni, Integer gel, ParameterTambahan parent,
			int indexParent, Map<String, String[]> mapValParam) {

		List<ParameterTambahanAlumni> parameterTambahanAlumnis = ConstantValues.simpleList(
				session.createCriteria(ParameterTambahanAlumni.class)
						.add(Restrictions.eq("kelompokParameterTambahanAlumni", kelompokParameterTambahanAlumni))
						.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
								gel == null ? Restrictions.sqlRestriction("false")
										: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
						.createAlias("parameterTambahan", "parameterTambahan")
						.add(parent == null || parent.getId() == null ? Restrictions.isNull("parameterTambahan.parent") : Restrictions.eq("parameterTambahan.parent", parent))
						.createAlias("kelompokParameterTambahanAlumni", "kelompokParameterTambahanAlumni")
						.add(digunakanUntukPenggunaAlumni
								? Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", true)
								: Restrictions.or(
										Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", false),
										Restrictions.isNull("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni")))
						.add(Restrictions.eq("parameterTambahan.aktif", true))
						.add(Restrictions.eq("kelompokParameterTambahanAlumni.aktif", true)),
				ParameterTambahanAlumni.class);
				
		Collections.sort(parameterTambahanAlumnis);

		EventListener isi = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (biodataMahasiswa != null && parameterRows != null) {
					biodataMahasiswa.populateParameterTambahanAlumni(parameterRows);
				}
			}
		};

		boolean tampil = false;
		if (!parameterTambahanAlumnis.isEmpty()) {
			Map<Long, ParameterTambahan> mapdata = ConstantValues.ambilBerdasarClass(ParameterTambahan.class);
			
			for (ParameterTambahanAlumni parameterTambahanAlumni : parameterTambahanAlumnis) {
				ParameterTambahan parameterTambahan = parameterTambahanAlumni.getParameterTambahan();

				// SYARAT TAMPIL (conditional display): tampil HANYA bila jawaban parameter acuan == syaratNilai.
				// Dievaluasi dari jawaban tersimpan (mapValParam).
				final boolean lolosSyarat = lolosSyaratTampil(parameterTambahan, mapValParam);

				boolean adaChild = false;
				for (ParameterTambahan p : mapdata.values()) {
					if (p.getParent() != null && p.getParent().getId().equals(parameterTambahan.getId())) {
						adaChild = true;
						break;
					}
				}

				String jenis = LampiranLain.resolveJenisParameterTambahan(BiodataMahasiswa.class,
						biodataMahasiswa.getId(),
						kelompokParameterTambahanAlumni.getId() + "->" + parameterTambahan.getId());
				String jenisKey = jenis.toLowerCase();

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setAttribute("parameterTambahan", parameterTambahan);
				row.setAttribute("kelompokParameterTambahanAlumni", kelompokParameterTambahanAlumni);
				row.setParent(rowsUtama);
				
				row.appendChild(new Label(parameterTambahan.getLabelInputan() + (parameterTambahanAlumni.getWajibDiisi() ? " (*)" : " ")));
				
				if (parameterTambahan.getKeterangan() != null && !parameterTambahan.getKeterangan().trim().isEmpty()) {
					parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
				}

				// PENGAMBILAN NILAI MELALUI MAP (Sangat Cepat O(1))
				String val = "";
				String ket = "";
				if (mapValParam.containsKey(jenisKey)) {
					String[] value = mapValParam.get(jenisKey);
					val = value.length > 1 ? value[1].trim() : "";
					try {
						ket = value.length > 2 ? value[value.length - 1] : "";
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanAlumniListener.java:340");}
				}

				boolean adaKomponen = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
						biodataMahasiswa.getId(), val, ket, parameterTambahan, isi);
				// SYARAT TAMPIL: sembunyikan baris parameter bila syarat tak terpenuhi.
				row.setVisible(lolosSyarat);
				tampil |= (adaKomponen && lolosSyarat);

				Rows childRows = rowsUtama;
				if (adaChild) {
					MyFormRow row1 = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(row1, "2");
					row1.setParent(rowsUtama);
					row1.setVisible(lolosSyarat); // ikut sembunyi bila induk parameter tak lolos syarat

					Grid grid = new Grid();
					grid.setSclass("dgrid fgrid");
					grid.setStyle("padding-left: 50px; background:transparent;");
					grid.setParent(row1);

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);
					column.setWidth("30%");

					column = new MyColumnConfig();
					column.setParent(columns);

					childRows = new Rows();
					childRows.setParent(grid);

					displayRinci(rowParameterTambahan, childRows, session, kelompokParameterTambahanAlumni, gel,
							parameterTambahan, indexParent + 1, mapValParam);
				}
			}
			if (rowParameterTambahan != null) {
				rowParameterTambahan.setVisible(tampil);
			}
		}
	}

	/**
	 * Mengevaluasi apakah field {@code parameterTambahan} berhak TAMPIL berdasarkan kolom JSON
	 * {@link ParameterTambahan#getSyaratTampil()}. Format JSON yang didukung: {@code {"logika":
	 * "AND"|"OR", "syarat": [{"parameterId": <id>, "nilai": "<nilai pembanding>"}, ...]}} — tiap
	 * entri {@code syarat} dicocokkan terhadap jawaban TERSIMPAN dari parameter acuan ({@code
	 * parameterId}, dicari lewat {@link #cariNilaiParam}) memakai pencocokan label-aware {@link
	 * ais.common.ParameterTambahanHtmlHelper#nilaiCocok} (mis. jawaban tersimpan {@code
	 * "Bekerja:1"} cocok dengan syarat {@code "Bekerja"}) — konsisten dengan evaluasi syarat
	 * tampil versi JSP. Hasil akhir digabung AND atau OR sesuai {@code logika} (default AND bila
	 * tak diset). Selalu mengembalikan {@code true} (fail-open, field tetap tampil) bila: JSON
	 * kosong/null, array {@code syarat} kosong, tak ada entri valid yang bisa dievaluasi, atau
	 * JSON gagal di-parse (exception ditelan) — desain ini sengaja agar konfigurasi syarat yang
	 * cacat tidak sampai menyembunyikan field secara tak sengaja.
	 *
	 * @param parameterTambahan parameter yang visibilitasnya dievaluasi
	 * @param mapValParam        peta jawaban tersimpan (hasil {@link #parseParameterMap}) tempat nilai parameter acuan dicari
	 * @return {@code true} bila tak bersyarat atau syarat terpenuhi; {@code false} bila ada syarat yang dievaluasi dan tidak terpenuhi
	 */
	private boolean lolosSyaratTampil(ParameterTambahan parameterTambahan, Map<String, String[]> mapValParam) {
		try {
			String json = parameterTambahan.getSyaratTampil();
			if (json == null || json.trim().isEmpty()) {
				return true; // tak bersyarat -> selalu tampil
			}
			org.json.JSONObject obj = new org.json.JSONObject(json.trim());
			org.json.JSONArray arr = obj.optJSONArray("syarat");
			if (arr == null || arr.length() == 0) {
				return true;
			}
			boolean or = "OR".equalsIgnoreCase(obj.optString("logika", "AND"));
			boolean hasilAnd = true;
			boolean hasilOr = false;
			int dievaluasi = 0;
			for (int i = 0; i < arr.length(); i++) {
				org.json.JSONObject c = arr.optJSONObject(i);
				if (c == null) {
					continue;
				}
				long pid = c.optLong("parameterId", 0L);
				if (pid == 0L) {
					continue;
				}
				String nilaiSyarat = c.optString("nilai", "");
				String nilaiAcuan = cariNilaiParam(mapValParam, Long.valueOf(pid));
				// Pencocokan label-aware (mis. "Bekerja" cocok dengan tersimpan "Bekerja:1") — konsisten dgn JSP.
				boolean cocok = ais.common.ParameterTambahanHtmlHelper.nilaiCocok(nilaiAcuan, nilaiSyarat);
				hasilAnd = hasilAnd && cocok;
				hasilOr = hasilOr || cocok;
				dievaluasi++;
			}
			if (dievaluasi == 0) {
				return true; // tak ada syarat valid -> tampil
			}
			return or ? hasilOr : hasilAnd;
		} catch (Exception e) {
			return true; // JSON invalid -> jangan sembunyikan (aman)
		}
	}

	/**
	 * Mencari jawaban tersimpan untuk parameter tertentu (berdasar id-nya saja, tanpa tahu id
	 * kelompoknya) di dalam {@code mapValParam} yang berkunci {@code idKelompok->idParameter}
	 * (lower-case) — dicari dengan mencocokkan AKHIRAN kunci ({@code endsWith("->" + parameterId)})
	 * karena {@link #lolosSyaratTampil} hanya tahu id parameter acuan, bukan kelompoknya.
	 *
	 * @param mapValParam peta jawaban tersimpan hasil {@link #parseParameterMap}; {@code null} langsung mengembalikan {@code null}
	 * @param parameterId id {@link ParameterTambahan} yang jawabannya dicari; {@code null} langsung mengembalikan {@code null}
	 * @return nilai jawaban (bagian ke-2 dari array hasil split {@code "<=>"}), string kosong bila entri ditemukan tapi tak punya bagian nilai, atau {@code null} bila tak ada entri yang cocok
	 */
	private String cariNilaiParam(Map<String, String[]> mapValParam, Long parameterId) {
		if (mapValParam == null || parameterId == null) {
			return null;
		}
		String suffix = ("->" + parameterId).toLowerCase();
		for (Map.Entry<String, String[]> e : mapValParam.entrySet()) {
			if (e.getKey() != null && e.getKey().endsWith(suffix)) {
				String[] v = e.getValue();
				return (v != null && v.length > 1) ? v[1] : "";
			}
		}
		return null;
	}
}