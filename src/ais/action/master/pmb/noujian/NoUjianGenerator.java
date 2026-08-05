package ais.action.master.pmb.noujian;

import java.util.List;

import ais.database.model.BiodataCalonMahasiswa;

public interface NoUjianGenerator {

	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa)
			throws Exception;

	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa,
			List<String> noRegPengecualian) throws Exception;

}
