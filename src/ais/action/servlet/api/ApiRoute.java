package ais.action.servlet.api;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import ais.database.model.PerguruanTinggi;

/**
 * Kontrak ringan untuk handler action API.
 * Dibuat sebagai interface terpisah agar route dapat direuse/test secara mandiri.
 */
public interface ApiRoute {
    JSONObject execute(HttpServletRequest request, JSONObject json, PerguruanTinggi perguruanTinggi) throws Exception;
}
