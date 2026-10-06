package com.aicontrolcenter.app;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpUtil {
    public static String readAll(InputStream in) throws Exception { if(in==null)return ""; ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>=0)b.write(buf,0,n); return b.toString("UTF-8"); }
    public static JSONObject postForm(String url, Map<String,String> form) throws Exception {
        StringBuilder body=new StringBuilder(); for(Map.Entry<String,String> e:form.entrySet()){ if(body.length()>0)body.append('&'); body.append(URLEncoder.encode(e.getKey(),"UTF-8")).append('=').append(URLEncoder.encode(e.getValue(),"UTF-8")); }
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(30000); c.setRequestMethod("POST"); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode(); String raw=readAll(code>=400?c.getErrorStream():c.getInputStream()); if(code>=400)throw new IOException("HTTP "+code+": "+raw); return new JSONObject(raw);
    }
    public static JSONObject getJson(String url,String bearer) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(20000); if(bearer!=null)c.setRequestProperty("Authorization","Bearer "+bearer);
        int code=c.getResponseCode(); String raw=readAll(code>=400?c.getErrorStream():c.getInputStream()); if(code>=400)throw new IOException("HTTP "+code+": "+raw); return new JSONObject(raw);
    }
}
