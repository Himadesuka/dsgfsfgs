package com.aicontrolcenter.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CryptoStore {
    private static final String ALIAS="ai_control_center_master_v1";
    private final SharedPreferences prefs;
    public CryptoStore(Context c){ prefs=c.getSharedPreferences("secure_store",Context.MODE_PRIVATE); }
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if(!ks.containsAlias(ALIAS)){
            KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            kg.generateKey();
        }
        return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
    }
    public void put(String name,String value) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key());
        byte[] ct=c.doFinal(value.getBytes(StandardCharsets.UTF_8));
        JSONObject wrap=new JSONObject(); wrap.put("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP)); wrap.put("ct",Base64.encodeToString(ct,Base64.NO_WRAP));
        prefs.edit().putString(name,wrap.toString()).apply();
    }
    public String get(String name,String def){
        try{
            String raw=prefs.getString(name,null); if(raw==null)return def;
            JSONObject w=new JSONObject(raw); byte[] iv=Base64.decode(w.getString("iv"),Base64.NO_WRAP); byte[] ct=Base64.decode(w.getString("ct"),Base64.NO_WRAP);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
            return new String(c.doFinal(ct),StandardCharsets.UTF_8);
        }catch(Exception e){ return def; }
    }
    public synchronized List<Account> accounts(){
        List<Account> out=new ArrayList<>();
        try{ JSONArray a=new JSONArray(get("accounts","[]")); for(int i=0;i<a.length();i++) out.add(Account.fromJson(a.getJSONObject(i))); }catch(Exception ignored){}
        return out;
    }
    public synchronized void saveAccounts(List<Account> accounts) throws Exception {
        JSONArray a=new JSONArray(); for(Account x:accounts)a.put(x.toJson()); put("accounts",a.toString());
    }
    public synchronized void upsert(Account a) throws Exception {
        List<Account> xs=accounts(); boolean found=false;
        for(int i=0;i<xs.size();i++) if(xs.get(i).id.equals(a.id)){xs.set(i,a);found=true;break;}
        if(!found)xs.add(a); saveAccounts(xs);
    }
    public synchronized void remove(String id) throws Exception {
        List<Account> xs=accounts();
        for(int i=xs.size()-1;i>=0;i--) if(xs.get(i).id.equals(id)) xs.remove(i);
        saveAccounts(xs);
    }
    public String hostId(){
        String h=get("host_id",""); if(!h.isEmpty())return h;
        h="urn:uuid:"+java.util.UUID.randomUUID(); try{put("host_id",h);}catch(Exception ignored){} return h;
    }
}
