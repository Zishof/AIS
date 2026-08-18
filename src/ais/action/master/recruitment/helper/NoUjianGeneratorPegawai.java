package ais.action.master.recruitment.helper;

import java.util.List;

import ais.database.model.recruitment.CalonPegawai;

public interface NoUjianGeneratorPegawai {

	public String generateNoUjian(CalonPegawai calonPegawai) throws Exception;

	public String generateNoUjian(CalonPegawai calonPegawai, List<String> noRegPengecualian) throws Exception;

}
