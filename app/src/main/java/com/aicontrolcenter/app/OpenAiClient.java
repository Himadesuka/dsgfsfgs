package com.aicontrolcenter.app;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OpenAiClient {
    public interface StreamCallback{void delta(String d);void done();void fail(String m);}
    public static List<String[]> listModels(Account a)throws Exception{
        JSONObject j=HttpUtil.getJson("https://api.openai.com/v1/models",a.accessToken); JSONArray arr=j.optJSONArray("models"); List<String[]> out=new ArrayList<>(); if(arr!=null)for(int i=0;i<arr.length();i++){JSONObject x=arr.getJSONObject(i);if("list".equals(x.optString("visibility")))out.add(new String[]{x.optString("slug"),x.optString("display_name",x.optString("slug"))});} return out;
    }
    public static void stream(Account a,String model,String instruction,StreamCallback cb){new Thread(()->{try{
        HttpURLConnection c=(HttpURLConnection)new URL("https://api.openai.com/v1/responses").openConnection(); c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+a.accessToken);c.setRequestProperty("Content-Type","application/json");
        JSONObject body=new JSONObject();body.put("model",model);JSONArray input=new JSONArray();JSONObject msg=new JSONObject();msg.put("role","user");msg.put("content",instruction);input.put(msg);body.put("input",input);body.put("store",false);body.put("stream",true);
        try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();if(code>=400)throw new IOException("HTTP "+code+": "+HttpUtil.readAll(c.getErrorStream()));
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));String line;boolean completed=false;while((line=br.readLine())!=null){if(!line.startsWith("data: "))continue;String d=line.substring(6);if("[DONE]".equals(d))break;try{JSONObject ev=new JSONObject(d);String type=ev.optString("type");if("response.output_text.delta".equals(type))cb.delta(ev.optString("delta"));else if("response.completed".equals(type)){completed=true;cb.done();break;}else if("response.failed".equals(type))throw new IOException(ev.toString());}catch(JSONException ignored){}}
        if(!completed)cb.fail("ストリームが完了イベントなしで終了しました");
    }catch(Exception e){cb.fail(e.getMessage()==null?e.toString():e.getMessage());}}).start();}
}
