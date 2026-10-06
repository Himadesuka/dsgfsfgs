package com.aicontrolcenter.app;

import org.json.JSONObject;

public class Account {
    public String id, provider, email, displayName, subject, clientId, idToken, accessToken, refreshToken, scope;
    public long expiresAt;
    public boolean sharingEnabled;

    public JSONObject toJson() throws Exception {
        JSONObject o=new JSONObject();
        o.put("id",id); o.put("provider",provider); o.put("email",email); o.put("displayName",displayName);
        o.put("subject",subject); o.put("clientId",clientId); o.put("idToken",idToken); o.put("accessToken",accessToken);
        o.put("refreshToken",refreshToken); o.put("scope",scope); o.put("expiresAt",expiresAt); o.put("sharingEnabled",sharingEnabled);
        return o;
    }
    public static Account fromJson(JSONObject o){
        Account a=new Account();
        a.id=o.optString("id"); a.provider=o.optString("provider"); a.email=o.optString("email"); a.displayName=o.optString("displayName");
        a.subject=o.optString("subject"); a.clientId=o.optString("clientId"); a.idToken=o.optString("idToken"); a.accessToken=o.optString("accessToken");
        a.refreshToken=o.optString("refreshToken"); a.scope=o.optString("scope"); a.expiresAt=o.optLong("expiresAt"); a.sharingEnabled=o.optBoolean("sharingEnabled");
        return a;
    }
}
