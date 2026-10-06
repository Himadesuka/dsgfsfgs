package com.aicontrolcenter.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenAiAuth {
    public interface Callback { void ok(Account a); void fail(String message); }
    private final Activity activity;
    private final CryptoStore store;
    private final ExecutorService exec=Executors.newCachedThreadPool();
    private volatile ServerSocket server;

    public OpenAiAuth(Activity activity,CryptoStore store){this.activity=activity;this.store=store;}
    private static String b64(byte[] b){return Base64.encodeToString(b,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
    private static String random(int n){byte[] b=new byte[n];new SecureRandom().nextBytes(b);return b64(b);}
    private static String q(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}

    public void signIn(Account existing,Callback cb){
        exec.submit(()->{
            ServerSocket ss=null;
            try{
                ss=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
                ss.setSoTimeout(300000); server=ss;
                String redirect="http://127.0.0.1:"+ss.getLocalPort()+"/auth/callback";
                String verifier=random(48),state=random(32),nonce=random(32);
                String challenge=b64(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                String client=existing==null?"dynamic_agent_client":existing.clientId;
                StringBuilder u=new StringBuilder("https://auth.openai.com/api/accounts/authorize?response_type=code")
                    .append("&client_id=").append(q(client))
                    .append("&redirect_uri=").append(q(redirect))
                    .append("&scope=").append(q("openid profile email offline_access resource.invoke chatgpt.tokens.use.direct"))
                    .append("&resource=").append(q("https://api.openai.com/v1"))
                    .append("&state=").append(q(state))
                    .append("&nonce=").append(q(nonce))
                    .append("&code_challenge_method=S256&code_challenge=").append(q(challenge))
                    .append("&ext_agent_host_id=").append(q(store.hostId()));
                if(existing==null)u.append("&agent_name_hint=").append(q("AI Control Center"));
                else{
                    if(existing.idToken!=null&&!existing.idToken.isEmpty())u.append("&id_token_hint=").append(q(existing.idToken));
                    if(existing.email!=null&&!existing.email.isEmpty())u.append("&login_hint=").append(q(existing.email));
                }
                String authUrl=u.toString();
                activity.runOnUiThread(()->activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(authUrl))));
                try(Socket s=ss.accept()){
                    s.setSoTimeout(10000);
                    BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8));
                    String line=br.readLine();
                    if(line==null||!line.startsWith("GET /auth/callback?"))throw new SecurityException("Unexpected OAuth callback");
                    String target=line.split(" ")[1];
                    Uri uri=Uri.parse("http://127.0.0.1"+target);
                    String returned=uri.getQueryParameter("state");
                    if(returned==null||!MessageDigest.isEqual(returned.getBytes(StandardCharsets.UTF_8),state.getBytes(StandardCharsets.UTF_8)))throw new SecurityException("OAuth state validation failed");
                    String err=uri.getQueryParameter("error"); if(err!=null)throw new SecurityException("OAuth: "+err);
                    String code=uri.getQueryParameter("code"); if(code==null||code.isEmpty())throw new SecurityException("Authorization code missing");
                    String issued=existing==null?uri.getQueryParameter("client_id"):existing.clientId;
                    if(issued==null||issued.isEmpty())throw new SecurityException("Client ID missing");
                    if(existing!=null&&uri.getQueryParameter("client_id")!=null&&!issued.equals(uri.getQueryParameter("client_id")))throw new SecurityException("Client ID mismatch");
                    String html="HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n<html><body style='background:#0b0f17;color:#fff;font-family:sans-serif;padding:32px'><h2>AI Control Center</h2><p>認証応答を受信しました。アプリへ戻ってください。</p></body></html>";
                    s.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
                    Map<String,String> f=new LinkedHashMap<>();
                    f.put("grant_type","authorization_code");f.put("client_id",issued);f.put("code",code);f.put("code_verifier",verifier);f.put("redirect_uri",redirect);f.put("resource","https://api.openai.com/v1");
                    JSONObject tok=HttpUtil.postForm("https://auth.openai.com/api/accounts/oauth/token",f);
                    String idToken=tok.optString("id_token"); if(idToken.isEmpty())throw new SecurityException("ID token missing");
                    JSONObject claims=verifyIdToken(idToken,issued,nonce);
                    Account a=new Account();a.provider="OpenAI / ChatGPT";a.clientId=issued;a.idToken=idToken;a.accessToken=tok.optString("access_token");
                    a.refreshToken=tok.optString("refresh_token");a.scope=tok.optString("scope");a.expiresAt=System.currentTimeMillis()+tok.optLong("expires_in",3600)*1000L;
                    a.sharingEnabled=a.scope.contains("chatgpt.tokens.use.direct");a.subject=claims.getString("sub");a.email=claims.optString("email","ChatGPT account");a.displayName=claims.optString("name",a.email);
                    a.id="openai:"+a.subject+":"+a.clientId;
                    if(existing!=null&&!existing.subject.equals(a.subject))throw new SecurityException("Signed-in identity does not match selected account");
                    store.upsert(a);cb.ok(a);
                }
            }catch(Exception e){cb.fail(e.getMessage()==null?e.toString():e.getMessage());}
            finally{try{if(ss!=null)ss.close();}catch(Exception ignored){}server=null;}
        });
    }

    public void refresh(Account a,Callback cb){
        exec.submit(()->{try{
            Map<String,String> f=new LinkedHashMap<>();f.put("grant_type","refresh_token");f.put("client_id",a.clientId);f.put("refresh_token",a.refreshToken);f.put("resource","https://api.openai.com/v1");
            JSONObject t=HttpUtil.postForm("https://auth.openai.com/api/accounts/oauth/token",f);
            a.accessToken=t.optString("access_token",a.accessToken);a.refreshToken=t.optString("refresh_token",a.refreshToken);a.idToken=t.optString("id_token",a.idToken);
            a.scope=t.optString("scope",a.scope);a.sharingEnabled=a.scope.contains("chatgpt.tokens.use.direct");a.expiresAt=System.currentTimeMillis()+t.optLong("expires_in",3600)*1000L;
            store.upsert(a);cb.ok(a);
        }catch(Exception e){cb.fail(e.getMessage()==null?e.toString():e.getMessage());}});
    }

    private JSONObject verifyIdToken(String jwt,String clientId,String nonce)throws Exception{
        String[] p=jwt.split("\\.");if(p.length!=3)throw new SecurityException("Invalid ID token");
        JSONObject header=new JSONObject(new String(Base64.decode(p[0],Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP),StandardCharsets.UTF_8));
        JSONObject claims=new JSONObject(new String(Base64.decode(p[1],Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP),StandardCharsets.UTF_8));
        if(!"RS256".equals(header.optString("alg")))throw new SecurityException("Unsupported ID token algorithm");
        JSONObject jwks=HttpUtil.getJson("https://auth.openai.com/.well-known/jwks.json",null);JSONArray keys=jwks.getJSONArray("keys");JSONObject chosen=null;String kid=header.optString("kid");
        for(int i=0;i<keys.length();i++){JSONObject k=keys.getJSONObject(i);if(kid.equals(k.optString("kid"))){chosen=k;break;}}
        if(chosen==null)throw new SecurityException("Signing key not found");
        BigInteger n=new BigInteger(1,Base64.decode(chosen.getString("n"),Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP));
        BigInteger e=new BigInteger(1,Base64.decode(chosen.getString("e"),Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP));
        PublicKey pk=KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n,e));
        Signature sig=Signature.getInstance("SHA256withRSA");sig.initVerify(pk);sig.update((p[0]+"."+p[1]).getBytes(StandardCharsets.US_ASCII));
        if(!sig.verify(Base64.decode(p[2],Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP)))throw new SecurityException("ID token signature invalid");
        if(!"https://auth.openai.com".equals(claims.optString("iss")))throw new SecurityException("Issuer mismatch");
        boolean audOk=false;Object aud=claims.opt("aud");
        if(aud instanceof JSONArray){JSONArray ar=(JSONArray)aud;for(int i=0;i<ar.length();i++)if(clientId.equals(ar.optString(i)))audOk=true;} else audOk=clientId.equals(String.valueOf(aud));
        if(!audOk)throw new SecurityException("Audience mismatch");
        if(claims.optLong("exp",0)*1000L<System.currentTimeMillis()-30000)throw new SecurityException("ID token expired");
        if(!nonce.equals(claims.optString("nonce")))throw new SecurityException("Nonce mismatch");
        return claims;
    }
    public void cancel(){try{if(server!=null)server.close();}catch(Exception ignored){}}
}
