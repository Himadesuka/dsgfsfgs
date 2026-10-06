package com.aicontrolcenter.app;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(11,15,23),SURFACE=Color.rgb(18,24,35),SURFACE2=Color.rgb(25,32,46),TEXT=Color.rgb(242,245,252),MUTED=Color.rgb(150,160,180),ACCENT=Color.rgb(124,140,255),GREEN=Color.rgb(72,205,148),RED=Color.rgb(190,70,82),AMBER=Color.rgb(245,183,72);
    CryptoStore store;OpenAiAuth openAi;GoogleCloudAuth google;LinearLayout content,nav;TextView title,subtitle;String page="home";
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,int sp,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    GradientDrawable bg(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView button(String s,int c){TextView v=tv(s,14,Color.WHITE);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(14),dp(10),dp(14),dp(10));v.setBackground(bg(c,14));return v;}
    TextView pill(String s,int c){TextView v=tv(s,11,c);v.setPadding(dp(9),dp(5),dp(9),dp(5));GradientDrawable g=bg((c&0x00ffffff)|0x22000000,99);g.setStroke(dp(1),(c&0x00ffffff)|0x66000000);v.setBackground(g);return v;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        store=new CryptoStore(this);openAi=new OpenAiAuth(this,store);google=new GoogleCloudAuth(this,store);shell();home();refreshExpiring();
    }
    void shell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(8),dp(16),dp(6));root.setBackgroundColor(BG);setContentView(root);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=tv("◈",25,ACCENT);head.addView(logo,new LinearLayout.LayoutParams(dp(38),dp(52)));
        LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);title=tv("AI Control Center",20,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);subtitle=tv("すべてのAIを、ひとつに",11,MUTED);ht.addView(title);ht.addView(subtitle);head.addView(ht,new LinearLayout.LayoutParams(0,dp(52),1));head.addView(pill("● ONLINE",GREEN));root.addView(head);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);String[][] ns={{"⌂","ホーム","home"},{"◎","アカウント","accounts"},{"▶","実行","run"},{"◇","リモート","remote"},{"⚙","設定","settings"}};
        for(String[] n:ns){TextView x=tv(n[0]+"\n"+n[1],11,MUTED);x.setGravity(Gravity.CENTER);x.setTag(n[2]);x.setOnClickListener(v->go((String)v.getTag()));nav.addView(x,new LinearLayout.LayoutParams(0,dp(58),1));}root.addView(nav);
    }
    void go(String p){if("home".equals(p))home();else if("accounts".equals(p))accounts();else if("run".equals(p))run();else if("remote".equals(p))remote();else settings();}
    void clear(String p,String t,String st){page=p;content.removeAllViews();title.setText(t);subtitle.setText(st);for(int i=0;i<nav.getChildCount();i++){TextView v=(TextView)nav.getChildAt(i);v.setTextColor(p.equals(v.getTag())?ACCENT:MUTED);}}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(bg(SURFACE,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(6));content.addView(c,lp);return c;}
    void section(String s){TextView v=tv(s,12,MUTED);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(2),dp(15),0,dp(4));content.addView(v);}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(105,114,134));e.setTextColor(TEXT);e.setPadding(dp(12),0,dp(12),0);e.setBackground(bg(SURFACE2,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.setMargins(0,dp(8),0,dp(8));e.setLayoutParams(lp);return e;}
    LinearLayout stat(String label,String value,int c){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setGravity(Gravity.CENTER);TextView v=tv(value,23,c);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER);TextView l=tv(label,11,MUTED);l.setGravity(Gravity.CENTER);b.addView(v);b.addView(l);return b;}

    void home(){
        clear("home","AI Control Center","AI・アカウント・実行を統合管理");
        List<Account> xs=store.accounts();LinearLayout h=card();TextView big=tv("コントロールセンター",24,TEXT);big.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.addView(big);h.addView(tv("複数のAIアカウント、エージェント、Remote / Cloud実行をスマホから管理。",13,MUTED));
        LinearLayout stats=new LinearLayout(this);stats.setPadding(0,dp(12),0,dp(12));stats.addView(stat("接続済み",""+xs.size(),GREEN),new LinearLayout.LayoutParams(0,dp(76),1));stats.addView(stat("実行中","0",ACCENT),new LinearLayout.LayoutParams(0,dp(76),1));stats.addView(stat("待機","0",AMBER),new LinearLayout.LayoutParams(0,dp(76),1));h.addView(stats);
        TextView add=button("＋ AIアカウントを接続",ACCENT);add.setOnClickListener(v->addAccount());h.addView(add,new LinearLayout.LayoutParams(-1,dp(48)));
        section("接続アカウント");if(xs.isEmpty()){LinearLayout c=card();c.addView(tv("まだアカウントがありません",17,TEXT));c.addView(tv("OpenAI / ChatGPT または Google Cloud を追加してください。",13,MUTED));}else for(Account a:xs)miniAccount(a);
        section("実行基盤");LinearLayout r=card();r.addView(tv("Agent Runtime",18,TEXT));r.addView(tv("Responses APIのストリーミング指示送信とRemote Runner設定を搭載。",13,MUTED));TextView b=button("指示を送る",Color.rgb(80,96,190));b.setOnClickListener(v->run());r.addView(b,new LinearLayout.LayoutParams(-1,dp(46)));
    }
    void miniAccount(Account a){LinearLayout c=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView ic=tv(a.provider.startsWith("OpenAI")?"◎":"G",20,a.provider.startsWith("OpenAI")?ACCENT:GREEN);ic.setGravity(Gravity.CENTER);ic.setBackground(bg(SURFACE2,14));row.addView(ic,new LinearLayout.LayoutParams(dp(46),dp(46)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(12),0,0,0);TextView n=tv(a.displayName==null||a.displayName.isEmpty()?a.email:a.displayName,15,TEXT);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);tx.addView(n);tx.addView(tv(a.provider+" • "+a.email,12,MUTED));row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));row.addView(pill(a.expiresAt>System.currentTimeMillis()?"接続中":"更新必要",a.expiresAt>System.currentTimeMillis()?GREEN:AMBER));c.addView(row);}

    void addAccount(){
        clear("accounts","アカウント接続","複数アカウント対応");
        LinearLayout o=card();o.addView(tv("OpenAI / ChatGPT",19,TEXT));o.addView(tv("PKCE + state + nonceで認証し、refresh tokenをAndroid Keystoreで暗号化保存します。",13,MUTED));TextView ob=button("Continue with ChatGPT",ACCENT);ob.setOnClickListener(v->{toast("ブラウザを開きます");openAi.signIn(null,openCb());});o.addView(ob,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout g=card();g.addView(tv("Google Cloud",19,TEXT));g.addView(tv("Google Cloud / Cloud Code用。Google Cloud Consoleで作成したDesktop OAuth Client IDを入力します。",13,MUTED));EditText id=input("Google OAuth Client ID");id.setText(store.get("google_client_id",""));g.addView(id);TextView gb=button("Google Cloud と接続",Color.rgb(62,125,80));gb.setOnClickListener(v->{String cid=id.getText().toString().trim();try{store.put("google_client_id",cid);}catch(Exception ignored){}google.signIn(cid,new GoogleCloudAuth.Callback(){public void ok(Account a){runOnUiThread(()->{toast("接続しました");accounts();});}public void fail(String m){runOnUiThread(()->error(m));}});});g.addView(gb,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    OpenAiAuth.Callback openCb(){return new OpenAiAuth.Callback(){public void ok(Account a){runOnUiThread(()->{toast("接続しました: "+a.email);accounts();});}public void fail(String m){runOnUiThread(()->error(m));}};}

    void accounts(){
        clear("accounts","アカウント","OAuth・更新・個別ログアウト");
        TextView add=button("＋ 新しいアカウントを接続",ACCENT);add.setOnClickListener(v->addAccount());content.addView(add,new LinearLayout.LayoutParams(-1,dp(50)));
        for(Account a:store.accounts()){LinearLayout c=card();TextView n=tv(a.displayName,18,TEXT);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(n);c.addView(tv(a.email+"\n"+a.provider,13,MUTED));LinearLayout flags=new LinearLayout(this);flags.setPadding(0,dp(8),0,dp(8));flags.addView(pill(a.expiresAt>System.currentTimeMillis()?"● セッション有効":"● 更新必要",a.expiresAt>System.currentTimeMillis()?GREEN:AMBER));c.addView(flags);LinearLayout btn=new LinearLayout(this);TextView re=button("再認証",Color.rgb(64,76,115));re.setTag(a);re.setOnClickListener(v->{Account x=(Account)v.getTag();if(x.provider.startsWith("OpenAI"))openAi.signIn(x,openCb());else toast("Googleは新規接続から再認証してください");});btn.addView(re,new LinearLayout.LayoutParams(0,dp(44),1));TextView lo=button("ログアウト",RED);lo.setTag(a);lo.setOnClickListener(v->logout((Account)v.getTag()));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);lp.setMargins(dp(8),0,0,0);btn.addView(lo,lp);c.addView(btn);}
    }
    void logout(Account a){new AlertDialog.Builder(this).setTitle("アカウントをログアウト").setMessage(a.email+"\nこの端末の保存トークンを削除します。").setNegativeButton("キャンセル",null).setPositiveButton("ログアウト",(d,w)->{try{store.remove(a.id);accounts();}catch(Exception e){error(e.getMessage());}}).show();}

    void run(){
        clear("run","実行・エージェント","選択したアカウントへ指示");
        List<Account> ai=new ArrayList<>();for(Account a:store.accounts())if(a.provider.startsWith("OpenAI")&&a.sharingEnabled)ai.add(a);
        if(ai.isEmpty()){LinearLayout c=card();c.addView(tv("実行可能なOpenAIアカウントがありません",17,TEXT));TextView b=button("アカウントを接続",ACCENT);b.setOnClickListener(v->addAccount());c.addView(b);return;}
        LinearLayout c=card();Spinner who=new Spinner(this);ArrayList<String> names=new ArrayList<>();for(Account a:ai)names.add(a.email);ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names);who.setAdapter(ad);c.addView(who,new LinearLayout.LayoutParams(-1,dp(50)));
        EditText model=input("モデルslug（空欄なら利用可能モデルを自動選択）");c.addView(model);EditText prompt=input("指示を入力");prompt.setSingleLine(false);prompt.setGravity(Gravity.TOP);prompt.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(130)));c.addView(prompt);
        TextView go=button("▶ 実行",ACCENT);TextView out=tv("",14,TEXT);out.setTextIsSelectable(true);go.setOnClickListener(v->{Account a=ai.get(who.getSelectedItemPosition());String p=prompt.getText().toString().trim();if(p.isEmpty()){toast("指示を入力してください");return;}go.setText("実行中…");out.setText("");new Thread(()->{try{String m=model.getText().toString().trim();if(m.isEmpty()){List<String[]> ms=OpenAiClient.listModels(a);if(ms.isEmpty())throw new Exception("利用可能モデルがありません");m=ms.get(0)[0];String mm=m;runOnUiThread(()->model.setText(mm));}OpenAiClient.stream(a,m,p,new OpenAiClient.StreamCallback(){public void delta(String d){runOnUiThread(()->out.append(d));}public void done(){runOnUiThread(()->{go.setText("▶ 実行");toast("完了");});}public void fail(String x){runOnUiThread(()->{go.setText("▶ 実行");error(x);});}});}catch(Exception e){runOnUiThread(()->{go.setText("▶ 実行");error(e.getMessage());});}}).start();});c.addView(go,new LinearLayout.LayoutParams(-1,dp(50)));section("実行出力");LinearLayout oc=card();oc.addView(out);
    }

    void remote(){
        clear("remote","Remote / Cloud","外部ランナー接続");
        LinearLayout c=card();c.addView(tv("Remote Runner",19,TEXT));c.addView(tv("Codex app-serverや自前VMなどを接続するための管理設定です。",13,MUTED));EditText url=input("Runner HTTPS URL");url.setText(store.get("runner_url",""));c.addView(url);EditText tok=input("Runner Bearer Token");tok.setText(store.get("runner_token",""));c.addView(tok);TextView save=button("設定を暗号化保存",Color.rgb(70,88,140));save.setOnClickListener(v->{try{store.put("runner_url",url.getText().toString().trim());store.put("runner_token",tok.getText().toString().trim());toast("保存しました");}catch(Exception e){error(e.getMessage());}});c.addView(save,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout s=card();s.addView(tv("接続状態",18,TEXT));s.addView(tv("● Android管理層: 稼働中",13,GREEN));s.addView(tv((store.get("runner_url","").isEmpty()?"○":"●")+" Remote Runner: "+(store.get("runner_url","").isEmpty()?"未設定":"設定済み"),13,MUTED));
    }
    void settings(){
        clear("settings","設定","セキュリティ・接続");
        LinearLayout c=card();c.addView(tv("セキュリティ",18,TEXT));c.addView(tv("• OAuth PKCE S256 + state + nonce\n• OpenAI ID token RS256署名検証\n• Android Keystore AES-GCM\n• アカウント単位セッション\n• OpenAI refresh token自動更新",13,MUTED));
        LinearLayout d=card();d.addView(tv("AI Control Center 0.1.0-alpha",17,TEXT));d.addView(tv("Multi-account / OAuth / Responses / Remote設定の初期実装",13,MUTED));
    }
    void refreshExpiring(){for(Account a:store.accounts())if(a.provider.startsWith("OpenAI")&&a.refreshToken!=null&&!a.refreshToken.isEmpty()&&a.expiresAt<System.currentTimeMillis()+600000)openAi.refresh(a,new OpenAiAuth.Callback(){public void ok(Account x){}public void fail(String m){}});}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    void error(String s){new AlertDialog.Builder(this).setTitle("エラー").setMessage(s==null?"不明なエラー":s).setPositiveButton("OK",null).show();}
    @Override protected void onDestroy(){openAi.cancel();super.onDestroy();}
}
