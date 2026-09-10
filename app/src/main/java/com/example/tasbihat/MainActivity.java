package com.example.tasbihat;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    TasbihatView view;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();
        view = new TasbihatView();
        setContentView(view);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean f) {
        super.onWindowFocusChanged(f);
        if (f) hideSystemUi();
    }

    @Override protected void onPause() { super.onPause(); view.saveState(); }
    @Override protected void onDestroy() { view.releaseAudio(); super.onDestroy(); }

    class TasbihatView extends View {
        static final int INTRO=0, S1=1, S2=2, S3=3, FINAL=4;
        final SharedPreferences prefs;
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint t=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Rect src=new Rect();
        final RectF dst=new RectF();

        Bitmap current, next;
        int stage=INTRO, index=0, direction=1;
        boolean musicOn, dhikrOn, settingsOpen=false;
        float downX, downY;
        long introStart=System.currentTimeMillis(), animStart=0;
        static final long INTRO_MS=2000, ANIM_MS=320;

        TasbihatView() {
            super(MainActivity.this);
            prefs=getSharedPreferences("settings",MODE_PRIVATE);
            musicOn=prefs.getBoolean("music_on",true);
            dhikrOn=prefs.getBoolean("dhikr_on",true);
            current=load("intro");
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            postDelayed(() -> {
                if(stage==INTRO) {
                    stage=S1; index=0; current=load("section1_01");
                    startMusic(false); playDhikr(); invalidate();
                }
            },INTRO_MS);
        }

        float dp(float x){ return x*getResources().getDisplayMetrics().density; }

        Bitmap load(String name){
            int id=getResources().getIdentifier(name,"drawable",getPackageName());
            return id==0?null:BitmapFactory.decodeResource(getResources(),id);
        }

        int total(){
            return stage==S1?34:(stage==S2||stage==S3?33:0);
        }

        String dhikr(){
            return stage==S1?"الله اکبر":stage==S2?"الحمدلله":stage==S3?"سبحان الله":"";
        }

        void startMusic(boolean restart){
            if(!musicOn){ stopMusic(); return; }
            if(musicPlayer==null){
                musicPlayer=MediaPlayer.create(MainActivity.this,R.raw.audio4);
                if(musicPlayer!=null){ musicPlayer.setLooping(true); musicPlayer.setVolume(1,1); }
            }
            if(musicPlayer!=null){
                if(restart) musicPlayer.seekTo(0);
                if(!musicPlayer.isPlaying()) musicPlayer.start();
            }
        }

        void stopMusic(){
            if(musicPlayer!=null && musicPlayer.isPlaying()) musicPlayer.pause();
        }

        void playDhikr(){
            stopDhikr();
            if(!dhikrOn || (stage!=S1&&stage!=S2&&stage!=S3)) return;
            int id=stage==S1?R.raw.audio1:(stage==S2?R.raw.audio2:R.raw.audio3);
            dhikrPlayer=MediaPlayer.create(MainActivity.this,id);
            if(dhikrPlayer!=null){ dhikrPlayer.setVolume(1,1); dhikrPlayer.start(); }
        }

        void stopDhikr(){
            if(dhikrPlayer!=null){
                try{dhikrPlayer.stop();}catch(Exception ignored){}
                dhikrPlayer.release(); dhikrPlayer=null;
            }
        }

        void go(int s,int i,int dir){
            String name=s==FINAL?"final_image":
                s==S1?String.format(Locale.US,"section1_%02d",i+1):
                s==S2?String.format(Locale.US,"section2_%02d",i+1):
                String.format(Locale.US,"section3_%02d",i+1);
            Bitmap b=load(name);
            if(b==null)return;
            next=b; direction=dir; animStart=System.currentTimeMillis();
            stage=s; index=i; playDhikr(); invalidate();
        }

        void advance(){
            if(stage==S1){ if(index<33)go(S1,index+1,1); else go(S2,0,1); }
            else if(stage==S2){ if(index<32)go(S2,index+1,1); else go(S3,0,1); }
            else if(stage==S3){ if(index<32)go(S3,index+1,1); else go(FINAL,0,1); }
        }

        void restart(){
            stage=S1; index=0; settingsOpen=false; stopDhikr();
            current=load("section1_01"); next=null;
            startMusic(true); playDhikr(); invalidate();
        }

        void feedback(){
            Intent i=new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            i.putExtra(Intent.EXTRA_SUBJECT,"بازخورد درباره تسبیحات حضرت زهرا");
            i.putExtra(Intent.EXTRA_TEXT,"سلام،\n\nنظر یا پیشنهاد من:\n");
            try{startActivity(Intent.createChooser(i,"ارسال بازخورد"));}catch(Exception ignored){}
        }

        void toggleMusic(){
            musicOn=!musicOn; prefs.edit().putBoolean("music_on",musicOn).apply();
            if(musicOn)startMusic(false);else stopMusic(); invalidate();
        }

        void toggleDhikr(){
            dhikrOn=!dhikrOn; prefs.edit().putBoolean("dhikr_on",dhikrOn).apply();
            if(dhikrOn)playDhikr();else stopDhikr(); invalidate();
        }

        void saveState(){ prefs.edit().putBoolean("music_on",musicOn).putBoolean("dhikr_on",dhikrOn).apply(); }

        void releaseAudio(){
            if(musicPlayer!=null){musicPlayer.release();musicPlayer=null;}
            stopDhikr();
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            c.drawColor(Color.rgb(232,223,202));
            if(current!=null) drawCover(c,current,0);

            if(next!=null){
                float q=Math.min(1f,(System.currentTimeMillis()-animStart)/(float)ANIM_MS);
                q=1-(1-q)*(1-q);
                float w=getWidth(), shift=(1-q)*w*direction;
                drawCover(c,current,-shift);
                drawCover(c,next,w*direction-shift);
                if(q>=1){current=next;next=null;} else postInvalidateDelayed(16);
            }

            if(stage==INTRO){
                if(System.currentTimeMillis()-introStart>=INTRO_MS){
                    stage=S1; index=0; current=load("section1_01");
                    startMusic(false); playDhikr();
                } else postInvalidateDelayed(16);
                return;
            }

            if(stage==FINAL) drawFinal(c); else drawBar(c);
            if(settingsOpen) drawSettings(c);
        }

        void drawCover(Canvas c,Bitmap b,float dx){
            float vw=getWidth(),vh=getHeight(), bw=b.getWidth(),bh=b.getHeight();
            float scale=Math.max(vw/bw,vh/bh), dw=bw*scale,dh=bh*scale;
            float left=(vw-dw)/2+dx,top=(vh-dh)/2;
            src.set(0,0,b.getWidth(),b.getHeight()); dst.set(left,top,left+dw,top+dh);
            c.drawBitmap(b,src,dst,p);
        }

        void drawBar(Canvas c){
            float h=Math.max(dp(70),getHeight()*.105f), top=getHeight()-h;
            p.setColor(Color.argb(192,245,243,238)); c.drawRect(0,top,getWidth(),getHeight(),p);
            p.setColor(Color.argb(80,125,105,72)); c.drawRect(0,top,getWidth(),top+dp(1),p);

            int n=total(); float left=dp(30),right=getWidth()-dp(78);
            float gap=(right-left)/Math.max(1,n-1), cy=top+dp(18);
            for(int i=0;i<n;i++){
                float x=left+i*gap;
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.4f));
                p.setColor(i==index?Color.rgb(207,160,45):Color.argb(105,112,101,85));
                c.drawCircle(x,cy,i==index?dp(3.5f):dp(2.2f),p);
                if(i==index){p.setStyle(Paint.Style.FILL);c.drawCircle(x,cy,dp(3.5f),p);}
            }
            p.setStyle(Paint.Style.FILL);
            t.setTextAlign(Paint.Align.CENTER);t.setTextSize(dp(19));t.setColor(Color.rgb(60,49,39));
            c.drawText(dhikr(),getWidth()/2,top+dp(50),t);
            t.setTextAlign(Paint.Align.LEFT);t.setTextSize(dp(13));t.setColor(Color.rgb(91,80,67));
            c.drawText((index+1)+" از "+n,dp(14),top+dp(50),t);
            drawGear(c,getWidth()-dp(30),top+dp(46));
        }

        void drawGear(Canvas c,float x,float y){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.8f));p.setColor(Color.rgb(101,91,78));
            c.drawCircle(x,y,dp(7),p);c.drawCircle(x,y,dp(2),p);
            for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(
                x+(float)Math.cos(a)*dp(8),y+(float)Math.sin(a)*dp(8),
                x+(float)Math.cos(a)*dp(10),y+(float)Math.sin(a)*dp(10),p);}
            p.setStyle(Paint.Style.FILL);
        }

        void drawFinal(Canvas c){
            float h=Math.max(dp(82),getHeight()*.115f),top=getHeight()-h;
            p.setColor(Color.argb(205,245,243,238));c.drawRect(0,top,getWidth(),getHeight(),p);
            p.setColor(Color.argb(80,125,105,72));c.drawRect(0,top,getWidth(),top+dp(1),p);
            pill(c,getWidth()*.19f,top+dp(39),getWidth()*.34f,"ارسال عکس یا نظر");
            pill(c,getWidth()*.70f,top+dp(39),getWidth()*.24f,"شروع مجدد");
        }

        void pill(Canvas c,float cx,float cy,float w,String s){
            p.setColor(Color.argb(235,255,252,245));c.drawRoundRect(
                new RectF(cx-w/2,cy-dp(20),cx+w/2,cy+dp(20)),dp(20),dp(20),p);
            t.setTextAlign(Paint.Align.CENTER);t.setTextSize(dp(13));t.setColor(Color.rgb(94,75,50));
            c.drawText(s,cx,cy+dp(5),t);
        }

        void drawSettings(Canvas c){
            p.setColor(Color.argb(35,0,0,0));c.drawRect(0,0,getWidth(),getHeight(),p);
            float w=Math.min(getWidth()-dp(28),dp(330)),h=dp(205),left=(getWidth()-w)/2,top=getHeight()-h-dp(74);
            p.setShadowLayer(dp(18),0,dp(8),Color.argb(90,0,0,0));p.setColor(Color.argb(248,250,247,240));
            c.drawRoundRect(new RectF(left,top,left+w,top+h),dp(22),dp(22),p);p.clearShadowLayer();
            t.setTextAlign(Paint.Align.RIGHT);t.setTextSize(dp(18));t.setColor(Color.rgb(89,69,45));
            c.drawText("تنظیمات",left+w-dp(20),top+dp(32),t);
            row(c,left,top+dp(65),"موسیقی",musicOn,0);
            row(c,left,top+dp(110),"ذکر",dhikrOn,1);
            row(c,left,top+dp(155),"شروع مجدد",false,2);
        }

        void row(Canvas c,float left,float y,String label,boolean on,int type){
            t.setTextAlign(Paint.Align.RIGHT);t.setTextSize(dp(15));t.setColor(Color.rgb(78,68,57));
            c.drawText(label,left+dp(250),y,t);
            if(type<2){
                float x=left+dp(28),yy=y-dp(6);p.setColor(on?Color.rgb(196,154,66):Color.rgb(190,186,177));
                c.drawRoundRect(new RectF(x,yy,x+dp(44),yy+dp(24)),dp(12),dp(12),p);
                p.setColor(Color.WHITE);c.drawCircle(x+(on?dp(32):dp(12)),yy+dp(12),dp(9),p);
            }else{
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.2f));p.setColor(Color.rgb(185,144,65));
                c.drawRoundRect(new RectF(left+dp(22),y-dp(20),left+dp(120),y+dp(8)),dp(14),dp(14),p);
                p.setStyle(Paint.Style.FILL);t.setTextAlign(Paint.Align.CENTER);t.setTextSize(dp(12));t.setColor(Color.rgb(135,104,61));
                c.drawText("بازنشانی",left+dp(71),y-dp(1),t);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=e.getX(),y=e.getY();

            if(settingsOpen){
                float w=Math.min(getWidth()-dp(28),dp(330)),h=dp(205),left=(getWidth()-w)/2,top=getHeight()-h-dp(74);
                if(y>=top+dp(45)&&y<top+dp(100)){toggleMusic();return true;}
                if(y>=top+dp(100)&&y<top+dp(145)){toggleDhikr();return true;}
                if(y>=top+dp(145)&&y<top+dp(195)){restart();return true;}
                settingsOpen=false;invalidate();return true;
            }

            if(stage!=FINAL && y>getHeight()-dp(90) && x>getWidth()-dp(70)){
                settingsOpen=true;invalidate();return true;
            }

            if(stage==FINAL){
                float top=getHeight()-Math.max(dp(82),getHeight()*.115f);
                if(y>top && x<getWidth()*.48f){feedback();return true;}
                if(y>top){restart();return true;}
                return true;
            }

            advance();
            return true;
        }
    }
}