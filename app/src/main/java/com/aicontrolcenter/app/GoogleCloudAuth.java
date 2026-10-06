package com.aicontrolcenter.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.Executors;

public class GoogleCloudAuth {
    public interface Callback{void ok(Account a);void fail(String m);}
    private final Activity activity;private final CryptoStore store;
    public GoogleCloudAuth(Activity a,CryptoStore s){activity=a;store=s;}
    private static String b64(byte[] b){return Base64.encodeToString(b,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
    private static String q(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}

    public void signIn(String clientId,Callback cb){
        Executors.newSingleThreadExecutor().submit(()->{
            ServerSocket ss=null;
            try{
                if(clientId==null||clientId.trim().isEmpty())throw new IllegalArgumentException("Google OAuth Client IDを設定してください");
                String verifier=b64(randomBytes(48)),state=b64(randomBytes(24));
                String challenge=b64(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                ss=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));ss.setSoTimeout(300000);
                String redirect="http://127.0.0.1:"+ss.getLocalPort()+"/oauth2callback";
                String url="https://accounts.google.com/o/oauth2/v2/auth?client_id="+q(clientId)+"&redirect_uri="+q(redirect)+"&response_type=code&scope="+q("openid email profile https://www.googleapis.com/auth/cloud-platform")+"&access_type=offline&prompt=select_account&state="+q(state)+"&code_challenge="+q(challenge)+"&code_challenge_method=S256";
                activity.runOnUiThread(()->activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))));
                try(Socket s=ss.accept()){
                    s.setSoTimeout(10000);BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8));String line=br.readLine();
                    if(line==null||!line.startsWith("GET /oauth2callback?"))throw new SecurityException("Unexpected OAuth callback");
                    Uri u=Uri.parse("http://127.0.0.1"+line.split(" ")[1]);if(!state.equals(u.getQueryParameter("state")))throw new SecurityException("OAuth state mismatch");
                    String err=u.getQueryParameter("error");if(err!=null)throw new SecurityException("OAuth: "+err);
                    String code=u.getQueryParameter("code");if(code==null)throw new SecurityException("Authorization code missing");
                    String html="HTTP/1.1 200 OK\r\nContent-Type:text/html; charset=utf-8\r\nConnection:close\r\n\r\n<html><body style='background:#0b0f17;color:#fff;font-family:sans-serif;padding:32px'><h2>AI Control Center</h2><p>Google認証を受信しました。アプリへ戻ってください。</p></body></html>";
                    s.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
                    Map<String,String> f=new LinkedHashMap<>();f.put("client_id",clientId);f.put("code",code);f.put("code_verifier",verifier);f.put("redirect_uri",redirect);f.put("grant_type","authorization_code");
                    JSONObject t=HttpUtil.postForm("https://oauth2.googleapis.com/token",f);
                    String access=t.optString("access_token");if(access.isEmpty())throw new SecurityException("Access token missing");
                    JSONObject user=HttpUtil.getJson("https://openidconnect.googleapis.com/v1/userinfo",access);
                    Account a=new Account();a.provider="Google Cloud";a.clientId=clientId;a.idToken=t.optString("id_token");a.accessToken=access;a.refreshToken=t.optString("refresh_token");
                    a.scope=t.optString("scope");a.expiresAt=System.currentTimeMillis()+t.optLong("expires_in",3600)*1000L;a.subject=user.getString("sub");a.email=user.optString("email","Google account");a.displayName=user.optString("name",a.email);
                    a.id="google:"+a.subject+":"+clientId;a.sharingEnabled=true;store.upsert(a);cb.ok(a);
                }
            }catch(Exception e){cb.fail(e.getMessage()==null?e.toString():e.getMessage());}
            finally{try{if(ss!=null)ss.close();}catch(Exception ignored){}}
        });
    }
    private static byte[] randomBytes(int n){byte[] b=new byte[n];new SecureRandom().nextBytes(b);return b;}
}
