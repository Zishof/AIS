package ais.common;

import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Generator UI dinamis untuk dynamic table generator. Kelas ini menerjemahkan metadata/konfigurasi
 * menjadi form, tabel, tab, atau wizard sehingga action tidak membangun ulang struktur komponen
 * yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi domain lain ({@code generateTable()}, {@code
 * generateTable()}, {@code toJsonArray()}, {@code toJsonArray()}, {@code toJsonArray2D()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> operasi membuat/mengubah komponen UI dan dapat membaca metadata atau data persistence.
 * Jalankan dengan desktop/session aktif dan gunakan generator ini sebagai satu sumber struktur dinamis, bukan
 * menyalin konstruksi widget ke action.</p>
 */
public class DynamicTableGenerator {

	/**
	 * Menghasilkan HTML dan JavaScript lengkap untuk manajemen data (CRUD) dengan
	 * Bootstrap. Kompatibel dengan Java 1.7.
	 *
	 * @param columns      Array nama field di database (misal: {"nama", "negara",
	 *                     "aktif"})
	 * @param labels       Array label untuk header tabel (misal: {"Nama Propinsi",
	 *                     "Negara", "Status"})
	 * @param searchCols   Array nama field yang muncul di panel pencarian
	 * @param formCols     Array nama field yang muncul di modal tambah/edit
	 * @param randomID     String unik untuk suffix ID elemen (misal: "A01", "X99")
	 * @param dataTypeName Nama entitas untuk display (misal: "Propinsi")
	 * @param modelClass   Nama full class Java Model (misal:
	 *                     "ais.database.model.Propinsi")
	 * @return String berisi kode HTML dan Javascript
	 * @throws Exception
	 */
	public static String generateTable(String[] columns, String[] labels, String[] searchCols, String[] formCols,
			String randomID, String dataTypeName, String modelClass, Set<String> paramRequired,
			Set<String> paramTidakBolehSama) throws Exception {
		String sqlWhere = "";
		Map<String, String> hiddenValues = null;
		return generateTable(columns, labels, searchCols, formCols, randomID, dataTypeName, modelClass, paramRequired,
				paramTidakBolehSama, sqlWhere, hiddenValues);
	}

	public static String generateTable(String[] columns, String[] labels, String[] searchCols, String[] formCols,
			String randomID, String dataTypeName, String modelClass, Set<String> paramRequired,
			Set<String> paramTidakBolehSama, String sqlWhere, Map<String, String> hiddenValues) throws Exception {
		JSONArray wizardConfigs = new JSONArray();

		JSONObject wizardConfig = new JSONObject();

		String[] stepTitles = { "" };

		// 2. Masukkan Data Scalar
		wizardConfig.put("dataTypeName", dataTypeName);
		wizardConfig.put("modelClass", modelClass);
		wizardConfig.put("randomID", randomID);
		wizardConfig.put("sqlWhere", sqlWhere); // Opsional
		// wizardConfig.put("hiddenValues", new JSONObject(mapHiddenValues)); //
		// Opsional jika ada map

		// 3. Masukkan Array 1 Dimensi
		wizardConfig.put("searchCols", toJsonArray(searchCols));
		wizardConfig.put("stepTitles", toJsonArray(stepTitles));
		wizardConfig.put("tableColumns", toJsonArray(columns));
		wizardConfig.put("tableLabels", toJsonArray(labels));

		// 4. Masukkan Array 2 Dimensi (Forms & Labels)
		wizardConfig.put("formCols", toJsonArray2D(new String[][] { formCols }));
		wizardConfig.put("formLabels", toJsonArray2D(new String[][] { labels }));

		// 5. Masukkan Set (Required & Unique)
		wizardConfig.put("paramRequired", toJsonArray(paramRequired));
		wizardConfig.put("paramTidakBolehSama", toJsonArray(paramTidakBolehSama));

		// Masukkan ke Array Utama
		wizardConfigs.put(wizardConfig);

		String htmlResult = DynamicJspCrudGenerator.generateWizardTable(wizardConfigs);

		// System.out.println("htmlResult -> \n\n"+htmlResult);

		return htmlResult;
	}

	private static JSONArray toJsonArray(String[] values) {
		JSONArray array = new JSONArray();
		if (values != null) {
			for (String value : values) {
				array.put(value);
			}
		}
		return array;
	}

	private static JSONArray toJsonArray(Set<String> values) {
		JSONArray array = new JSONArray();
		if (values != null) {
			for (String value : values) {
				array.put(value);
			}
		}
		return array;
	}

	private static JSONArray toJsonArray2D(String[][] values) {
		JSONArray array = new JSONArray();
		if (values != null) {
			for (String[] row : values) {
				array.put(toJsonArray(row));
			}
		}
		return array;
	}
}
